package core;

/**
 * JVM entry. Must not extend Swing or import AWT: {@code java -jar} loads
 * Main-Class before {@code main()}, which is why {@code MainActivity extends JFrame}
 * crashed Linux hosts with no DISPLAY.
 *
 * <p>{@code java -jar gsl5.jar} → GUI.
 * {@code java -jar gsl5.jar mcp [port] [bindHost]} → MCP server (always headless).
 */
public class Gsl5Main {

    public static void main(String[] args) {
        if (args != null && args.length >= 1 && "mcp".equals(args[0])) {
            System.setProperty("java.awt.headless", "true");
            startMcp(args);
            return;
        }
        // GUI mode needs a display. On headless hosts (no X11/DISPLAY) fall back
        // to the MCP server so `java -jar gsl5.jar` still runs without a display.
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            System.setProperty("java.awt.headless", "true");
            System.err.println("[GSL5] 无显示环境（headless），自动进入 MCP 模式（默认 0.0.0.0:9123）");
            startMcp(args);
            return;
        }
        core.ui.component.dialog.SafeFileSystemView.install();
        core.ui.MainActivity.main(args);
    }

    static void startMcp(String[] args) {
        int p = 9123;
        String bindHost = "0.0.0.0";
        if (args.length >= 2) {
            String a1 = args[1] == null ? "" : args[1].trim();
            if (a1.contains(":") && !a1.matches("^\\d+$")) {
                int idx = a1.lastIndexOf(':');
                bindHost = a1.substring(0, idx);
                try {
                    p = Integer.parseInt(a1.substring(idx + 1));
                } catch (Exception ignored) {
                }
            } else {
                try {
                    p = Integer.parseInt(a1);
                } catch (Exception ignored) {
                    if (!a1.isEmpty()) bindHost = a1;
                }
            }
        }
        if (args.length >= 3) {
            String a2 = args[2] == null ? "" : args[2].trim();
            if (!a2.isEmpty()) bindHost = a2;
        }
        try {
            Class.forName("core.ApplicationContext", true, Thread.currentThread().getContextClassLoader());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
            return;
        }
        shells.plugins.generic.McpService.startHeadless(p, bindHost);
    }
}
