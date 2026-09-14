package core.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import javax.swing.AbstractButton;
import javax.swing.Icon;

/** Classpath SVG icons for shell tabs, menus, and post-ex plugin labels. */
public final class SvgIcons {

    private SvgIcons() {
    }

    public static Icon of(String key) {
        if (key == null || key.trim().length() == 0) {
            return null;
        }
        try {
            FlatSVGIcon icon = new FlatSVGIcon("images/tabs/" + key + ".svg", 16, 16);
            return icon.hasFound() ? icon : null;
        } catch (Throwable t) {
            return null;
        }
    }

    public static void apply(AbstractButton button, String key) {
        if (button == null) {
            return;
        }
        Icon icon = of(key);
        if (icon != null) {
            button.setIcon(icon);
        }
    }

    public static void decorate(AbstractButton button) {
        if (button == null || button.getIcon() != null) {
            return;
        }
        apply(button, menuKey(button.getText()));
    }

    public static String menuKey(String text) {
        if (text == null) {
            return "plugin";
        }
        String t = text.trim();
        if ("\u76ee\u6807".equals(t)) {
            return "target";
        }
        if ("\u653b\u51fb".equals(t)) {
            return "attack";
        }
        if ("\u914d\u7f6e".equals(t) || t.contains("\u8bbe\u7f6e") || t.contains("\u7ba1\u7406")) {
            return "settings";
        }
        if ("\u63d2\u4ef6".equals(t)) {
            return "plugin";
        }
        if ("\u6570\u636e\u6e90".equals(t) || t.contains("\u6570\u636e\u5e93")) {
            return "database";
        }
        if ("\u66f4\u65b0".equals(t) || t.contains("\u68c0\u67e5\u66f4\u65b0")) {
            return "update";
        }
        if ("\u6dfb\u52a0".equals(t)) {
            return "add";
        }
        if (t.contains("\u5bfc\u5165")) {
            return "import";
        }
        if ("\u751f\u6210".equals(t)) {
            return "generate";
        }
        if (t.contains("\u626b\u63cf")) {
            return "scan";
        }
        if (t.contains("\u65e5\u5fd7")) {
            return t.contains("\u56e2\u961f") ? "team" : "log";
        }
        if (t.contains("\u8d5e\u52a9")) {
            return "heart";
        }
        if ("\u6253\u5f00".equals(t)) {
            return "open";
        }
        if (t.contains("\u590d\u5236")) {
            return "copy";
        }
        if ("\u4ea4\u4e92".equals(t)) {
            return "interact";
        }
        if (t.contains("\u7f13\u5b58")) {
            return "cache";
        }
        if ("\u79fb\u9664".equals(t) || t.contains("\u5220\u9664")) {
            return "delete";
        }
        if ("\u7f16\u8f91".equals(t)) {
            return "edit";
        }
        if ("\u5237\u65b0".equals(t)) {
            return "refresh";
        }
        if (t.contains("MCP")) {
            return "mcp";
        }
        if (t.contains("C2")) {
            return "settings";
        }
        return "plugin";
    }

    public static String coreTabKey(String key) {
        if ("BasicsInfo".equals(key)) {
            return "info";
        }
        if ("ExecCommand".equals(key)) {
            return "command";
        }
        if ("FileManage".equals(key)) {
            return "folder";
        }
        if ("DatabaseManage".equals(key)) {
            return "database";
        }
        if ("Netstat".equals(key)) {
            return "network";
        }
        if ("Note".equals(key)) {
            return "note";
        }
        if ("CopyTab".equals(key)) {
            return "tag";
        }
        return "plugin";
    }

    public static String categoryKey(String category) {
        if ("\u7ec8\u7aef".equals(category)) {
            return "terminal";
        }
        if ("\u6743\u9650\u63d0\u5347".equals(category)) {
            return "priv";
        }
        if ("\u51ed\u8bc1\u8d26\u53f7".equals(category)) {
            return "cred";
        }
        if ("\u5185\u5b58\u6301\u4e45\u5316".equals(category)) {
            return "memory";
        }
        if ("\u7f51\u7edc\u4ee3\u7406".equals(category)) {
            return "proxy";
        }
        if ("\u4ee3\u7801\u6267\u884c".equals(category)) {
            return "code";
        }
        if ("\u5e94\u7528\u5229\u7528".equals(category)) {
            return "app";
        }
        if ("\u4fa6\u5bdf\u8bc6\u522b".equals(category)) {
            return "recon";
        }
        return "other";
    }

    public static String pluginKey(String name, String display, String category) {
        String n = ((name == null ? "" : name) + " " + (display == null ? "" : display)).toLowerCase();
        if (contains(n, "superterminal", "realcmd", "newcmd", "\u7ec8\u7aef")) {
            return "terminal";
        }
        if (contains(n, "potato", "lemon", "petitpotam", "bypass", "th_tools", "th-tools")) {
            return "priv";
        }
        if (contains(n, "mimikatz", "sharpweb", "machinekey", "useradd")) {
            return "cred";
        }
        if (contains(n, "memoryshell", "filtershell", "meterpreter", "servlet", "\u5185\u5b58")) {
            return "memory";
        }
        if (contains(n, "proxy", "socks", "portscan", "httptoprofile", "\u4ee3\u7406")) {
            return "proxy";
        }
        if (contains(n, "eval", "shellcode", "executeassembly", "inlineexecute",
                "classloader", "jarloader", "peloader", "\u4ee3\u7801")) {
            return "code";
        }
        if (contains(n, "oatools", "enumdatabase", "attackfpm", "webshellscan")) {
            return "app";
        }
        if (contains(n, "avscan", "screen", "ms17010", "netbios", "oxid", "pps", "\u6740\u8f6f")) {
            return "recon";
        }
        return categoryKey(category);
    }

    private static boolean contains(String hay, String... needles) {
        for (int i = 0; i < needles.length; i++) {
            if (hay.contains(needles[i])) {
                return true;
            }
        }
        return false;
    }
}
