package util.ipdb;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import util.Log;
import util.ipdb.IpdbReader.IpdbException;

/**
 * Resolves URL host to location string using qqwry.ipdb (IPIP format).
 * Data file: <a href="https://github.com/nmgliangwei/qqwry.ipdb">nmgliangwei/qqwry.ipdb</a> standard {@code qqwry.ipdb}.
 */
public final class IpLocationService {
    private static final String COL_HINT_NO_DB = "\u672a\u914d\u7f6e qqwry.ipdb";
    private static final String COL_HINT_RESOLVE = "\u57df\u540d\u89e3\u6790\u8d85\u65f6/\u5931\u8d25";
    private static final ExecutorService DNS_POOL = Executors.newFixedThreadPool(6, r -> {
        Thread t = new Thread(r, "ipdb-dns");
        t.setDaemon(true);
        return t;
    });
    private static volatile IpdbReader reader;
    private static final Map<String, String> HOST_CACHE = new ConcurrentHashMap<>();

    private IpLocationService() {
    }

    private static final String CLASSPATH_IPDB = "data/qqwry.ipdb";

    public static synchronized void init() {
        if (reader != null) {
            return;
        }
        Path path = locateDatabaseFile();
        if (path != null && Files.isRegularFile(path)) {
            try (InputStream in = new FileInputStream(path.toFile())) {
                reader = new IpdbReader(in);
                Log.log("qqwry.ipdb loaded: %s", path.toAbsolutePath().normalize());
                return;
            } catch (Exception e) {
                reader = null;
                Log.error(e);
            }
        }
        InputStream bundled = openClasspathIpdb();
        if (bundled != null) {
            try {
                reader = new IpdbReader(bundled);
                Log.log("qqwry.ipdb loaded: classpath %s", CLASSPATH_IPDB);
            } catch (Exception e) {
                reader = null;
                Log.error(e);
            }
        }
    }

    public static boolean isLoaded() {
        return reader != null;
    }

    public static String getDatabasePathHint() {
        Path p = locateDatabaseFile();
        if (p != null && Files.isRegularFile(p)) {
            return p.toAbsolutePath().toString();
        }
        if (classpathIpdbAvailable()) {
            return "classpath:" + CLASSPATH_IPDB;
        }
        return "";
    }

    private static boolean classpathIpdbAvailable() {
        java.net.URL u = IpLocationService.class.getResource("/" + CLASSPATH_IPDB);
        return u != null;
    }

    private static InputStream openClasspathIpdb() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) {
            InputStream s = cl.getResourceAsStream(CLASSPATH_IPDB);
            if (s != null) {
                return s;
            }
        }
        return IpLocationService.class.getResourceAsStream("/" + CLASSPATH_IPDB);
    }

    private static Path locateDatabaseFile() {
        String prop = System.getProperty("qqwry.ipdb.path");
        if (prop != null && !prop.isEmpty()) {
            Path p = Paths.get(prop);
            if (Files.isRegularFile(p)) {
                return p;
            }
        }
        String userDir = System.getProperty("user.dir", ".");
        Path projectData = Paths.get(userDir, "data", "qqwry.ipdb");
        if (Files.isRegularFile(projectData)) {
            return projectData;
        }
        Path cwdRoot = Paths.get(userDir, "qqwry.ipdb");
        if (Files.isRegularFile(cwdRoot)) {
            return cwdRoot;
        }
        Path dataRelative = Paths.get("data", "qqwry.ipdb");
        if (Files.isRegularFile(dataRelative)) {
            return dataRelative;
        }
        Path cwdBare = Paths.get("qqwry.ipdb");
        if (Files.isRegularFile(cwdBare)) {
            return cwdBare;
        }
        Path home = Paths.get(System.getProperty("user.home"), ".webshell-manager", "qqwry.ipdb");
        if (Files.isRegularFile(home)) {
            return home;
        }
        return null;
    }

    public static String lookupUrl(String url) {
        if (reader == null) {
            init();
        }
        if (HOST_CACHE.size() > 5000) {
            HOST_CACHE.clear();
        }
        String host = hostFromUrl(url);
        if (host.isEmpty()) {
            return "";
        }
        String cached = HOST_CACHE.get(host);
        if (cached != null) {
            return cached;
        }
        if (isLoopbackHost(host)) {
            HOST_CACHE.put(host, "\u56de\u73af");
            return "\u56de\u73af";
        }
        String ip = isLiteralIp(host) ? host : null;
        if (ip != null) {
            String special = classifySpecialIp(ip);
            if (special != null) {
                HOST_CACHE.put(host, special);
                return special;
            }
        }
        if (reader == null) {
            return COL_HINT_NO_DB;
        }
        if (ip == null) {
            ip = resolveHostToIp(host);
        }
        if (ip == null) {
            String v = COL_HINT_RESOLVE;
            HOST_CACHE.put(host, v);
            return v;
        }
        String special = classifySpecialIp(ip);
        if (special != null) {
            HOST_CACHE.put(host, special);
            return special;
        }
        try {
            String[] fields = reader.find(ip, "CN");
            if (fields == null) {
                fields = reader.find(ip, "EN");
            }
            String v = formatFields(fields);
            HOST_CACHE.put(host, v);
            return v;
        } catch (IpdbException e) {
            String v = e.getMessage() != null ? e.getMessage() : "ipdb";
            HOST_CACHE.put(host, v);
            return v;
        }
    }

    public static void clearCache() {
        HOST_CACHE.clear();
    }

    private static String hostFromUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "";
        }
        String u = url.trim();
        if (u.startsWith("jdbc:")) {
            int idx = u.indexOf("://");
            if (idx > 0) {
                u = "http" + u.substring(idx);
            }
        }
        if (!u.contains("://")) {
            u = "http://" + u;
        }
        try {
            URI uri = new URI(u);
            String h = uri.getHost();
            return h != null ? h : "";
        } catch (URISyntaxException e) {
            return "";
        }
    }

    private static String resolveHostToIp(String host) {
        if (isLiteralIp(host)) {
            return host;
        }
        Future<String> f = DNS_POOL.submit(() -> {
            try {
                return InetAddress.getByName(host).getHostAddress();
            } catch (UnknownHostException e) {
                return null;
            }
        });
        try {
            return f.get(2, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            f.cancel(true);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException e) {
            return null;
        }
    }

    private static boolean isLiteralIp(String host) {
        if (host.indexOf(':') >= 0) {
            return true;
        }
        return host.matches("^(\\d{1,3}\\.){3}\\d{1,3}$");
    }

    private static boolean isLoopbackHost(String host) {
        if (host == null) {
            return false;
        }
        String h = host.trim().toLowerCase();
        return "localhost".equals(h) || "localhost.localdomain".equals(h);
    }

    private static String classifySpecialIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return null;
        }
        if (ip.indexOf(':') >= 0) {
            String low = ip.toLowerCase();
            if ("::1".equals(low) || low.endsWith("::1")) {
                return "\u56de\u73af";
            }
            if (low.startsWith("fe80:") || low.startsWith("fc") || low.startsWith("fd")) {
                return "\u5c40\u57df\u7f51";
            }
            return null;
        }
        int[] o = parseIpv4(ip);
        if (o == null) {
            return null;
        }
        if (o[0] == 127) {
            return "\u56de\u73af";
        }
        if (o[0] == 10
                || (o[0] == 172 && o[1] >= 16 && o[1] <= 31)
                || (o[0] == 192 && o[1] == 168)
                || (o[0] == 169 && o[1] == 254)) {
            return "\u5c40\u57df\u7f51";
        }
        if (o[0] == 0 || o[0] >= 224) {
            return "\u4fdd\u7559";
        }
        return null;
    }

    private static int[] parseIpv4(String ip) {
        String[] p = ip.split("\\.");
        if (p.length != 4) {
            return null;
        }
        try {
            int[] o = new int[4];
            for (int i = 0; i < 4; i++) {
                o[i] = Integer.parseInt(p[i]);
                if (o[i] < 0 || o[i] > 255) {
                    return null;
                }
            }
            return o;
        } catch (Exception e) {
            return null;
        }
    }

    private static String formatFields(String[] fields) {
        if (fields == null || fields.length == 0) {
            return "";
        }
        java.util.ArrayList<String> parts = new java.util.ArrayList<String>();
        for (String raw : fields) {
            if (raw == null) {
                continue;
            }
            String f = raw.trim();
            if (f.isEmpty() || "0".equals(f) || skipGeoToken(f)) {
                continue;
            }
            if (!parts.isEmpty() && (f.equals(parts.get(parts.size() - 1)) || f.startsWith(parts.get(parts.size() - 1)))) {
                parts.set(parts.size() - 1, f);
                continue;
            }
            parts.add(f);
        }
        if (parts.isEmpty()) {
            return "";
        }
        String joined = String.join("", parts);
        String special = mapSpecialLabel(joined);
        if (special != null) {
            return special;
        }
        if ("\u4e2d\u56fd".equals(parts.get(0)) || "CN".equalsIgnoreCase(parts.get(0))) {
            parts.remove(0);
        }
        if (parts.isEmpty()) {
            return "\u4e2d\u56fd";
        }
        String a = parts.get(0);
        if (parts.size() == 1) {
            return a;
        }
        String b = parts.get(1);
        if (b.equals(a) || b.startsWith(a) || a.startsWith(b)) {
            return b.length() >= a.length() ? b : a;
        }
        return a + b;
    }

    private static String mapSpecialLabel(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        String t = text.toLowerCase();
        if (t.contains("loopback") || text.contains("\u56de\u73af") || text.contains("\u672c\u673a")) {
            return "\u56de\u73af";
        }
        if (t.contains("lan") || text.contains("\u5c40\u57df") || text.contains("\u5185\u7f51")
                || text.contains("\u79c1\u6709\u7f51\u7edc") || text.contains("\u79c1\u7f51")) {
            return "\u5c40\u57df\u7f51";
        }
        if (t.contains("iana") || t.contains("apnic") || t.contains("ripe")
                || text.contains("\u4fdd\u7559") || text.contains("\u672a\u5206\u914d")) {
            return "\u4fdd\u7559";
        }
        return null;
    }

    private static boolean skipGeoToken(String f) {
        if (f.matches("[-+]?\\d+(\\.\\d+)?")) {
            return true;
        }
        if (f.length() == 2 && f.matches("[A-Za-z]{2}")) {
            return true;
        }
        return "\u7535\u4fe1".equals(f) || "\u8054\u901a".equals(f) || "\u79fb\u52a8".equals(f)
                || "\u94c1\u901a".equals(f) || "\u6559\u80b2\u7f51".equals(f) || "\u79d1\u6280\u7f51".equals(f)
                || "\u5e7f\u7535".equals(f) || "\u957f\u57ce".equals(f) || "\u6c5f\u82cf\u6709\u7ebf".equals(f)
                || "\u963f\u91cc\u4e91".equals(f) || "\u817e\u8baf\u4e91".equals(f) || "\u534e\u4e3a\u4e91".equals(f)
                || "AWS".equalsIgnoreCase(f) || "Google".equalsIgnoreCase(f) || "Cloudflare".equalsIgnoreCase(f)
                || f.contains("ISP") || f.contains("Cloud") || f.endsWith("\u516c\u53f8");
    }
}
