//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package core.shell;

import com.formdev.flatlaf.util.StringUtils;
import core.ApplicationContext;
import core.Db;
import core.EasyI18N;
import core.Encoding;
import core.c2profile.C2Profile;
import core.imp.Cryption;
import core.imp.Payload;
import core.shell.cache.CachePayload;
import core.shell.cache.enums.CachePayloadType;
import core.ui.ShellManage;
import core.ui.component.dialog.GOptionPane;
import core.ui.component.model.DbInfo;
import java.awt.Component;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import org.yaml.snakeyaml.Yaml;
import shells.channel.RequestChannel;
import util.Log;
import util.functions;
import util.http.Http;

public class ShellEntity {
    public static final String ERR_RETRY_NUM_ENV = "ERR_RETRY_NUM";
    private String url;
    private String password;
    private String secretKey;
    private String payload;
    private String cryption;
    private String remark;
    private String encoding;
    private LinkedHashMap<String, LinkedList<String>> headers;
    private String reqLeft;
    private String reqRight;
    private int connTimeout;
    private int readTimeout;
    private String proxyType;
    private String proxyHost;
    private int proxyPort;
    private Cryption cryptionModel;
    private Payload payloadModel;
    private String id;
    private ShellManage frame;
    private Encoding encodingModule;
    private Encoding dbEncodingModule;
    private String dbEncoding;
    private String c2ProfileName;
    private RequestChannel request;
    private boolean isSendLRReqData;
    private int bigFileErrorRetryNum;
    private int onceBigFileDownloadByteNum;
    private int onceBigFileUploadByteNum;
    private boolean useCache;
    private SSLSocketFactory clientCertAuthSSLSocketFactory;
    private C2Profile profile;
    private HashMap<String, String> tmpEnv;
    private Http http;

    public ShellEntity(boolean useCache) {
        this.bigFileErrorRetryNum = Db.getSetingIntValue("bigFileErrorRetryNum", 5);
        this.onceBigFileDownloadByteNum = Db.getSetingIntValue("onceBigFileDownloadByteNumTextField", 1048576);
        this.onceBigFileUploadByteNum = Db.getSetingIntValue("onceBigFileUploadByteNumTextField", 1048576);
        this.tmpEnv = new HashMap();
        this.url = "";
        this.password = "";
        this.secretKey = "";
        this.payload = "";
        this.cryption = "";
        this.remark = "";
        this.encoding = "Auto";
        this.headers = new LinkedHashMap();
        this.reqLeft = "";
        this.reqRight = "";
        this.connTimeout = 60000;
        this.readTimeout = 60000;
        this.proxyType = "";
        this.proxyHost = "";
        this.proxyPort = 8080;
        this.id = "";
        this.useCache = useCache;
    }

    public ShellEntity() {
        this(false);
    }

    /** Last init failure detail, surfaced by MCP instead of UI popups. */
    public String lastInitError = null;

    public boolean initShellOpertion() {
        boolean state = false;
        this.lastInitError = null;

        try {
            if (!StringUtils.isEmpty(this.id)) {
                this.bigFileErrorRetryNum = Integer.parseInt(this.getEnv("bigFileErrorRetryNum", String.valueOf(this.getBigFileErrorRetryNum())));
                this.onceBigFileDownloadByteNum = Integer.parseInt(this.getEnv("onceBigFileDownloadByteNumTextField", String.valueOf(this.getOnceBigFileDownloadByteNum())));
                this.onceBigFileUploadByteNum = Integer.parseInt(this.getEnv("onceBigFileUploadByteNumTextField", String.valueOf(this.getOnceBigFileUploadByteNum())));
            }

            this.payloadModel = this.openProxyPayloadClass(ApplicationContext.getPayload(this.payload).getClass());
            if (this.isUseCache()) {
                if (this.payloadModel != null) {
                    this.payloadModel.init(this);
                    return true;
                } else {
                    return false;
                }
            } else {
                this.cryptionModel = ApplicationContext.getCryption(this.payload, this.cryption);

                assert this.request != null;

                this.cryptionModel.init(this);
                if (this.cryptionModel.check()) {
                    this.payloadModel.init(this);
                    if (this.payloadModel.test()) {
                        state = true;
                    } else {
                        Log.error(EasyI18N.getI18nString("有效载荷初始化失败!"));
                        this.lastInitError = "有效载荷初始化失败";
                        GOptionPane.showMessageDialog((Component)null, "有效载荷初始化失败!");
                    }
                } else {
                    Log.error(EasyI18N.getI18nString("加密器初始化失败!"));
                    this.lastInitError = "加密器初始化失败";
                    GOptionPane.showMessageDialog((Component)null, "加密器初始化失败!");
                }

                return state;
            }
        } catch (Throwable var3) {
            this.lastInitError = var3.getClass().getName() + ": " + var3.getMessage();
            GOptionPane.showThrowableMessageDialog((Component)null, "初始化shell时发生异常", var3);
            return state;
        }
    }

    public Payload openProxyPayloadClass(Class payloadClass) throws InstantiationException, IllegalAccessException {
        return this.openProxyPayloadClassInternal(payloadClass, (Object)null);
    }

    public Payload openProxyPayloadClassInternal(Class payloadClass, Object internalObj) throws InstantiationException, IllegalAccessException {
        Payload proxyPayload = null;
        Class tmpPayloadClass = null;
        CachePayloadType cachePayloadType = CachePayloadType.NO_CACHE;
        if (this.isUseCache()) {
            proxyPayload = CachePayload.openUseCachePayload(this, payloadClass);
            cachePayloadType = CachePayloadType.USE_CACHE;
        } else if (ApplicationContext.isOpenCache()) {
            tmpPayloadClass = CachePayload.openCachePayload(payloadClass);
            cachePayloadType = CachePayloadType.OPEN_CACHE;
        } else {
            tmpPayloadClass = payloadClass;
        }

        if (tmpPayloadClass != null) {
            Constructor constructor = null;
            Object[] constructorParameter = new Object[0];

            try {
                constructor = tmpPayloadClass.getConstructor();
            } catch (NoSuchMethodException var20) {
                if (internalObj != null) {
                    try {
                        constructor = tmpPayloadClass.getDeclaredConstructor(internalObj.getClass());
                        constructor.setAccessible(true);
                        constructorParameter = new Object[]{internalObj};
                    } catch (NoSuchMethodException var19) {
                    }
                }
            } finally {
                if (constructor != null) {
                    try {
                        proxyPayload = (Payload)constructor.newInstance(constructorParameter);
                    } catch (InvocationTargetException var18) {
                        var18.printStackTrace();
                    }
                }

            }
        }

        CachePayload.setCachePayloadHandle(this, proxyPayload, cachePayloadType);
        return proxyPayload;
    }

    public RequestChannel getRequest() {
        return this.request;
    }

    public Cryption getCryptionModule() {
        return this.cryptionModel;
    }

    public Payload getPayloadModule() {
        return this.payloadModel;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPassword() {
        return this.password;
    }

    public String getSecretKey() {
        return this.secretKey;
    }

    public String getSecretKeyX() {
        return functions.md5(this.getSecretKey()).substring(0, 16);
    }

    public String getPayload() {
        return this.payload;
    }

    public String getEncoding() {
        return this.encoding;
    }

    public synchronized Encoding getEncodingModule() {
        if (this.encodingModule == null) {
            this.encodingModule = Encoding.getEncoding(this.getEncoding());
            applyFixedRemoteCharset(this.encodingModule);
        }

        return this.encodingModule;
    }

    /**
     * 部分 payload 的目标端解码字符集是**协议写死的**，与目标机代码页无关，
     * 而「encoding」下拉框（以及 chcp 探测）拿到的是代码页。两者一旦分叉，
     * 所有非 ASCII 的路径/命令都会乱码，所以这里按 payload 类型钉死。
     *
     *   AspDynamicPayload     —— payload.asp 用 ADODB.Stream 且 CharSet="utf-8"
     *   NetCoreDynamicPayload —— payload_core.dll 用 Encoding.UTF8
     *                            (.NET Core 上 Encoding.Default 本身就是 UTF-8)
     *
     * 不在此列的：
     *   JavaDynamicPayload    —— 跟随目标 JVM 的 file.encoding，
     *                            JDK 18+ 起固定 UTF-8，由 JavaAShell 从
     *                            getBasicsInfo() 里读出来动态设置。
     *   CSharpDynamicPayload  —— .NET Framework 的 Encoding.Default = 系统 ANSI
     *                            代码页，与 chcp 一致，无需干预。
     *   PhpDynamicPayload     —— 目标端字节透明，不做解码。
     */
    private void applyFixedRemoteCharset(Encoding encodingModule) {
        String p = this.payload == null ? "" : this.payload;
        if ("AspDynamicPayload".equals(p) || "NetCoreDynamicPayload".equals(p)) {
            encodingModule.setRemoteCharset("UTF-8");
        }
    }

    public synchronized String getDbEncoding() {
        if (this.dbEncoding == null) {
            this.dbEncoding = "UTF-8";
        }

        return this.dbEncoding;
    }

    public synchronized Encoding getDbEncodingModule() {
        if (this.dbEncodingModule == null) {
            this.dbEncodingModule = Encoding.getEncoding(this.getDbEncoding());
        }

        return this.dbEncodingModule;
    }

    public String getProxyType() {
        return this.proxyType;
    }

    public String getProxyHost() {
        return this.proxyHost;
    }

    public int getProxyPort() {
        return this.proxyPort;
    }

    public String getCryption() {
        return this.cryption;
    }

    public void setCryption(String cryption) {
        this.cryption = cryption;
    }

    public void setHeaders(LinkedHashMap<String, LinkedList<String>> headers) {
        this.headers = headers;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void setPayload(String Payload) {
        this.payload = Payload;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
        // Payload caches Encoding at init; update charset in-place so shell/payload stay in sync
        if (this.encodingModule != null) {
            this.encodingModule.setCharsetString(encoding == null ? "UTF-8" : encoding);
        }
    }

    public C2Profile getCurrentProfile() {
        return this.profile;
    }

    public int getMaxErrRetry() {
        C2Profile profile = this.getCurrentProfile();
        if (profile != null) {
            return profile.coreConfig.enabledErrRetry ? profile.coreConfig.errRetryNum : 0;
        } else {
            return Integer.parseInt(this.getEnv("ERR_RETRY_NUM", "0"));
        }
    }

    public void setMaxErrRetry(int errRetryNum) {
        if (this.profile == null) {
            this.setEnv("ERR_RETRY_NUM", String.valueOf(errRetryNum));
        }

    }

    public String getC2Profile() {
        return ApplicationContext.getC2Profile(this.getC2ProfileName());
    }

    public String getC2ProfileName() {
        return this.c2ProfileName;
    }

    public void setC2ProfileName(String c2ProfileName) {
        this.c2ProfileName = c2ProfileName;
    }

    public void setC2ProfileName2(String c2ProfileName) {
        this.setEnv("c2Profile", c2ProfileName);
        this.c2ProfileName = c2ProfileName;
    }

    public void setRequest(RequestChannel request) {
        this.request = request;
    }

    public void setProxyType(String proxyType) {
        this.proxyType = proxyType;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public int getConnTimeout() {
        return this.connTimeout;
    }

    public int getReadTimeout() {
        return this.readTimeout;
    }

    public void setConnTimeout(int connTimeout) {
        this.connTimeout = connTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public LinkedHashMap<String, LinkedList<String>> getHeaders() {
        return this.headers;
    }

    public String putHeader(String name, String value) {
        LinkedList<String> old = (LinkedList)this.getHeaders().get(name);
        if (old == null) {
            old = new LinkedList();
            this.getHeaders().put(name, old);
        } else {
            old.clear();
        }

        old.addFirst(value);
        return value;
    }

    public void setPayloadModel(Payload payloadModel) {
        this.payloadModel = payloadModel;
    }

    public String addHeader(String name, String value) {
        LinkedList<String> old = (LinkedList)this.getHeaders().get(name);
        if (old == null) {
            old = new LinkedList();
            this.getHeaders().put(name, old);
        }

        old.addLast(value);
        return value;
    }

    public String getHeaderS() {
        StringBuilder builder = new StringBuilder();
        Iterator<String> iterator = this.headers.keySet().iterator();

        while(iterator.hasNext()) {
            String key = (String)iterator.next();
            Iterator<String> values = ((LinkedList)this.headers.get(key)).iterator();

            while(values.hasNext()) {
                String value = (String)values.next();
                builder.append(key);
                builder.append(": ");
                builder.append(value);
                builder.append("\r\n");
            }
        }

        return builder.toString();
    }

    public String getRemark() {
        return this.remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public ShellManage getFrame() {
        return this.frame;
    }

    public void setFrame(ShellManage frame) {
        this.frame = frame;
    }

    public void setHeader(String reqString) {
        if (reqString != null) {
            String[] reqLines = reqString.split("\n");
            this.headers = new LinkedHashMap();

            for(int i = 0; i < reqLines.length; ++i) {
                if (!reqLines[i].trim().isEmpty()) {
                    int index = reqLines[i].indexOf(":");
                    if (index > 1) {
                        String keyName = reqLines[i].substring(0, index).trim();
                        String keyValue = reqLines[i].substring(index + 1).trim();
                        LinkedList<String> values = (LinkedList)this.headers.get(keyName);
                        if (values == null) {
                            values = new LinkedList();
                            this.headers.put(keyName, values);
                        }

                        values.add(keyValue);
                    }
                }
            }
        }

    }

    public String getReqLeft() {
        return this.reqLeft;
    }

    public void setReqLeft(String reqLeft) {
        this.reqLeft = reqLeft;
    }

    public String getReqRight() {
        return this.reqRight;
    }

    public void setReqRight(String reqRight) {
        this.reqRight = reqRight;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isSendLRReqData() {
        return this.cryptionModel.isSendRLData();
    }

    public boolean isMergeResponseCookie() {
        return Boolean.parseBoolean(this.getEnv("mergeResponseCookie", "false"));
    }

    public void setMergeResponseCookie(boolean mergeResponseCookie) {
        this.setEnv("mergeResponseCookie", Boolean.toString(mergeResponseCookie));
    }

    public SSLSocketFactory getClientCertAuthSSLSocketFactory() {
        if (this.clientCertAuthSSLSocketFactory == null) {
            String certPath = this.getClientCertPath();
            String certPassword = this.getClientCertPassword();
            if (!StringUtils.isEmpty(certPath)) {
                try {
                    byte[] certBytes = Files.readAllBytes(Paths.get(certPath));
                    char[] passwordChars = certPassword.toCharArray();
                    KeyStore keyStore = KeyStore.getInstance("PKCS12");
                    keyStore.load(new ByteArrayInputStream(certBytes), passwordChars);
                    KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                    kmf.init(keyStore, passwordChars);
                    TrustManager[] tm = new TrustManager[]{new Http.miTM()};
                    SSLContext sslCtx = SSLContext.getInstance("SSL");
                    sslCtx.init(kmf.getKeyManagers(), tm, new SecureRandom());
                    this.clientCertAuthSSLSocketFactory = sslCtx.getSocketFactory();
                } catch (Throwable var9) {
                    var9.printStackTrace();
                    throw new IllegalArgumentException(EasyI18N.getI18nString("尝试加载客户端SSL证书时发生异常! 异常消息:%s", new Object[]{var9.getMessage()}));
                }
            }
        }

        return this.clientCertAuthSSLSocketFactory;
    }

    public String getClientCertPath() {
        return this.getEnv("clientCertPath", "");
    }

    public void setClientCertPath(String clientCertPath) {
        this.setEnv("clientCertPath", clientCertPath);
    }

    public String getClientCertPassword() {
        return this.getEnv("clientCertPassword", "");
    }

    public void setClientCertPassword(String clientCertPassword) {
        this.setEnv("clientCertPassword", clientCertPassword);
    }

    public void setCurrentProfile(C2Profile profile) {
        this.profile = profile;
    }

    public boolean setEnv2(String key, Object value) {
        return this.setEnv(key, (new Yaml()).dump(value));
    }

    public boolean setEnv(String key, String value) {
        if (StringUtils.isEmpty(this.getId())) {
            this.tmpEnv.put(key, value);
            return true;
        } else {
            if (ApplicationContext.isOpenC("isSuperLog")) {
                Log.log(String.format("updateShellEnv id:%s key:%s value:%s", this.getId(), key, value));
            }

            String updateSetingSql;
            PreparedStatement preparedStatement;
            int affectNum;
            if (this.existsSetingKey(key)) {
                updateSetingSql = "UPDATE shellEnv set value=? WHERE shellId=? and key=?";
                preparedStatement = Db.getPreparedStatement(updateSetingSql);

                try {
                    preparedStatement.setString(1, value);
                    preparedStatement.setString(2, this.getId());
                    preparedStatement.setString(3, key);
                    affectNum = preparedStatement.executeUpdate();
                    preparedStatement.close();
                    return affectNum > 0;
                } catch (Exception var6) {
                    var6.printStackTrace();
                    return false;
                }
            } else {
                updateSetingSql = "INSERT INTO shellEnv (shellId,key,value) VALUES (?, ?, ?)";
                preparedStatement = Db.getPreparedStatement(updateSetingSql);

                try {
                    preparedStatement.setString(1, this.getId());
                    preparedStatement.setString(2, key);
                    preparedStatement.setString(3, value);
                    affectNum = preparedStatement.executeUpdate();
                    preparedStatement.close();
                    return affectNum > 0;
                } catch (Exception var7) {
                    var7.printStackTrace();
                    return false;
                }
            }
        }
    }

    public String getEnv(String key, String defaultValue) {
        if (StringUtils.isEmpty(this.getId())) {
            return (String)this.tmpEnv.getOrDefault(key, defaultValue);
        } else {
            String getShellEnvSql = "SELECT value FROM shellEnv WHERE shellId=? and key=?";
            if (this.existsSetingKey(key)) {
                try {
                    PreparedStatement preparedStatement = Db.getPreparedStatement(getShellEnvSql);
                    preparedStatement.setString(1, this.getId());
                    preparedStatement.setString(2, key);
                    ResultSet resultSet = preparedStatement.executeQuery();
                    String value = resultSet.next() ? resultSet.getString("value") : null;
                    resultSet.close();
                    preparedStatement.close();
                    return value;
                } catch (Exception var7) {
                    Log.error(var7);
                    return null;
                }
            } else {
                if (defaultValue != null) {
                    this.setEnv(key, defaultValue);
                }

                return defaultValue;
            }
        }
    }

    public Object getEnv2(String key, Object defaultValue) {
        return this.getEnv(key, defaultValue);
    }

    public Object getEnv(String key, Object defaultValue) {
        String yamlStr = this.getEnv(key, (String)null);
        if (StringUtils.isEmpty(yamlStr)) {
            if (defaultValue != null) {
                this.setEnv2(key, defaultValue);
            }

            return defaultValue;
        } else {
            return (new Yaml()).load(yamlStr);
        }
    }

    public String[] listDatabaseConfigs() {
        return this.listDatabaseConfigs(false);
    }

    public String[] listDatabaseConfigs(boolean isShowGroupDatabases) {
        LinkedList<String> configs = new LinkedList();

        try {
            PreparedStatement preparedStatement = Db.getPreparedStatement("SELECT key FROM shellEnv WHERE shellId=? and key like 'DatabaseConfig_%'");
            preparedStatement.setString(1, this.getId());
            ResultSet resultSet = preparedStatement.executeQuery();

            while(resultSet.next()) {
                String str = resultSet.getString(1);
                configs.add(str.substring("DatabaseConfig_".length()));
            }

            resultSet.close();
            preparedStatement.close();
        } catch (Exception var6) {
            Log.error(var6);
        }

        return (String[])configs.toArray(new String[0]);
    }

    public DbInfo getDbInfo(String configName) {
        DbInfo dbInfo = null;
        if (configName != null) {
            try {
                String serYaml = this.getEnv("DatabaseConfig_" + configName, (String)null);
                if (serYaml != null) {
                    dbInfo = (DbInfo)(new Yaml()).loadAs(serYaml, DbInfo.class);
                }
            } catch (Exception var4) {
                Log.error(var4);
            }
        }

        return dbInfo;
    }

    public boolean addDbIfo(DbInfo dbInfo) {
        String keyName = "DatabaseConfig_" + dbInfo.getConfigName();
        return !this.existsSetingKey(keyName) ? this.setEnv(keyName, (new Yaml()).dump(dbInfo)) : false;
    }

    public boolean updateDbIfo(DbInfo dbInfo) {
        String keyName = "DatabaseConfig_" + dbInfo.getConfigName();
        return this.setEnv(keyName, (new Yaml()).dump(dbInfo));
    }

    public boolean renameDbInfo(String configName, String newName) {
        String keyName = "DatabaseConfig_" + configName;
        String newKeyName = "DatabaseConfig_" + newName;
        if (this.existsSetingKey(keyName) && !this.existsSetingKey(newKeyName)) {
            String updateShellEnvSql = "UPDATE shellEnv set key=? WHERE shellId=? and key=?";
            PreparedStatement preparedStatement = Db.getPreparedStatement(updateShellEnvSql);

            try {
                preparedStatement.setString(1, newKeyName);
                preparedStatement.setString(2, this.getId());
                preparedStatement.setString(3, keyName);
                int affectNum = preparedStatement.executeUpdate();
                preparedStatement.close();
                return affectNum > 0;
            } catch (Exception var8) {
                var8.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean deleteDbInfo(String configName) {
        String keyName = "DatabaseConfig_" + configName;
        if (this.existsSetingKey(keyName)) {
            String updateShellEnvSql = "DELETE FROM shellEnv WHERE shellId=? and key=?";
            PreparedStatement preparedStatement = Db.getPreparedStatement(updateShellEnvSql);

            try {
                preparedStatement.setString(1, this.getId());
                preparedStatement.setString(2, keyName);
                int affectNum = preparedStatement.executeUpdate();
                preparedStatement.close();
                return affectNum > 0;
            } catch (Exception var6) {
                var6.printStackTrace();
                return false;
            }
        } else {
            return true;
        }
    }

    public void setGroup(String groupId) {
        this.setEnv("ENV_GROUP_ID", groupId);
    }

    public String getGroup() {
        return this.getEnv("ENV_GROUP_ID", "/");
    }

    public boolean removeEnv(String key) {
        String updateSetingSql = "DELETE FROM shellEnv WHERE shellId=? and key=?";
        PreparedStatement preparedStatement = Db.getPreparedStatement(updateSetingSql);

        try {
            preparedStatement.setString(1, this.getId());
            preparedStatement.setString(2, key);
            int affectNum = preparedStatement.executeUpdate();
            preparedStatement.close();
            return affectNum > 0;
        } catch (Exception var5) {
            var5.printStackTrace();
            return false;
        }
    }

    public boolean existsSetingKey(String key) {
        String selectKeyNumSql = "SELECT COUNT(1) as c FROM shellEnv WHERE shellId=? and key=?";

        try {
            PreparedStatement preparedStatement = Db.getPreparedStatement(selectKeyNumSql);
            preparedStatement.setString(1, this.getId());
            preparedStatement.setString(2, key);
            java.sql.ResultSet rs = preparedStatement.executeQuery();
            int c = rs.next() ? rs.getInt("c") : 0;
            preparedStatement.close();
            return c > 0;
        } catch (Exception var5) {
            Log.error(var5);
            return false;
        }
    }

    public int getBigFileErrorRetryNum() {
        return this.bigFileErrorRetryNum;
    }

    public void setBigFileErrorRetryNum(int bigFileErrorRetryNum) {
        this.bigFileErrorRetryNum = bigFileErrorRetryNum;
    }

    public int getOnceBigFileDownloadByteNum() {
        return Integer.parseInt(this.getEnv("onceBigFileDownloadByteNum", String.valueOf(Db.getSetingIntValue("onceBigFileDownloadByteNumTextField", 1048576))));
    }

    public void setOnceBigFileDownloadByteNum(int onceBigFileDownloadByteNum) {
        this.setEnv("onceBigFileDownloadByteNum", String.valueOf(onceBigFileDownloadByteNum));
    }

    public int getBigFileDownloadThreadNum() {
        return Integer.parseInt(this.getEnv("bigFileDownloadThreadNum", String.valueOf(5)));
    }

    public void setBigFileDownloadThreadNum(int bigFileDownloadThreadNum) {
        this.setEnv("bigFileDownloadThreadNum", String.valueOf(bigFileDownloadThreadNum));
    }

    public int getOnceBigFileUploadByteNum() {
        return Integer.parseInt(this.getEnv("onceBigFileUploadByteNum", String.valueOf(Db.getSetingIntValue("onceBigFileUploadByteNumTextField", 1048576))));
    }

    public void setOnceBigFileUploadByteNum(int onceBigFileUploadByteNum) {
        this.setEnv("onceBigFileUploadByteNum", String.valueOf(onceBigFileUploadByteNum));
    }

    public void setUseCache(boolean useCache) {
        this.useCache = useCache;
    }

    public boolean isUseCache() {
        return this.useCache;
    }

    public String toString() {
        return "ShellEntity [id=" + this.id + ", url=" + this.url + ", password=" + this.password + ", secretKey=" + this.secretKey + ", payload=" + this.payload + ", cryption=" + this.cryption + ", remark=" + this.remark + ", encoding=" + this.encoding + ", headers=" + this.headers + ", reqLeft=" + this.reqLeft + ", reqRight=" + this.reqRight + ", connTimeout=" + this.connTimeout + ", readTimeout=" + this.readTimeout + ", proxyType=" + this.proxyType + ", proxyHost=" + this.proxyHost + ", proxyPort=" + this.proxyPort + "]";
    }

    public Http getHttp() {
        return this.http;
    }


}
