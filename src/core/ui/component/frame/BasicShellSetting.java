//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package core.ui.component.frame;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import core.ApplicationContext;
import core.Db;
import core.EasyI18N;
import core.c2profile.cryption.C2Channel;
import core.imp.Cryption;
import core.shell.ShellEntity;
import core.ui.MainActivity;
import core.ui.component.RTextArea;
import core.ui.component.dialog.ChooseGroup;
import core.ui.component.dialog.GOptionPane;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.Border;
import org.fife.ui.rtextarea.RTextScrollPane;
import util.Log;
import util.UiFunction;
import util.automaticBindClick;
import util.functions;

public class BasicShellSetting extends JFrame {
    public JTabbedPane tabbedPane1;
    public JPanel corePanel;
    public JTextField urlTextField;
    public JLabel urlLabel;
    public JTextField passwordTextField;
    public JLabel passwordLabel;
    public JLabel secretKeyLabel;
    public JTextField secretKeyTextField;
    public JTextField connTimeOutTextField;
    public JLabel connTimeOutLabel;
    public JTextField readTimeOutTextField;
    public JLabel readTimeOutLabel;
    public JTextField proxyHostTextField;
    public JLabel proxyHostLabel;
    public JTextField proxyPortTextField;
    public JLabel proxyPortLabel;
    public JTextField remarkTextField;
    public JLabel remarkLabel;
    public JTextField groupIdTextField;
    public JLabel groupIdLabel;
    public JComboBox proxyComboBox;
    public JLabel proxyLabel;
    public JLabel encodingLabel;
    public JComboBox encodingComboBox;
    public JLabel payloadLabel;
    public JComboBox payloadComboBox;
    public JComboBox cryptionComboBox;
    public JLabel cryptionLabel;
    public JLabel c2ProfileLabel;
    public JComboBox c2ProfileComboBox;
    public JButton setButton;
    public JButton testButton;
    public RTextArea headersTextArea;
    public RTextScrollPane headersTextScrollPane;
    public RTextArea rightTextArea;
    public RTextScrollPane rightTextScrollPane;
    public RTextScrollPane leftTextScrollPane;
    public RTextArea leftTextArea;
    public JTextField errRetryNumTextField;
    public JLabel errRetryNumLabel;
    public JTextField bigFileUploadByteNumTextField;
    public JTextField bigFileDownloadByteNumTextField;
    public JTextField clientSSLCertPathTextField;
    public JTextField clientSSLCertPasswordTextField;
    public JCheckBox isMergeResponseCookieCheckBox;
    public JTextField bigFileDownloadThreadNumTextField;
    private ShellEntity shellContext;
    private final String shellId;
    private String error;
    private String currentGroup;
    private String currentC2profile;

    public BasicShellSetting(String id) {
        this(id, "/");
    }

    public BasicShellSetting(String id, String defaultGroup) {
        super("Shell Setting");
        this.shellId = id;
        this.currentGroup = defaultGroup;
        this.$$$setupUI$$$();
        this.addToComboBox(this.proxyComboBox, ApplicationContext.getAllProxy());
        this.addToComboBox(this.encodingComboBox, ApplicationContext.getAllEncodingTypes());
        this.addToComboBox(this.payloadComboBox, ApplicationContext.getAllPayload());
        this.payloadComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent paramActionEvent) {
                String seleteItemString = (String)BasicShellSetting.this.payloadComboBox.getSelectedItem();
                BasicShellSetting.this.cryptionComboBox.removeAllItems();
                BasicShellSetting.this.addToComboBox(BasicShellSetting.this.cryptionComboBox, ApplicationContext.getAllCryption(seleteItemString));
            }
        });
        this.cryptionComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent paramActionEvent) {
                String currentPayload = (String)BasicShellSetting.this.payloadComboBox.getSelectedItem();
                String currentCryption = (String)BasicShellSetting.this.cryptionComboBox.getSelectedItem();
                if (currentPayload != null && currentCryption != null) {
                    BasicShellSetting.this.c2ProfileComboBox.removeAllItems();
                    BasicShellSetting.this.c2ProfileComboBox.setEnabled(false);
                    Cryption cryption = ApplicationContext.getCryption(currentPayload, currentCryption);
                    if (cryption instanceof C2Channel) {
                        BasicShellSetting.this.c2ProfileComboBox.setEnabled(true);
                        BasicShellSetting.this.addToComboBox(BasicShellSetting.this.c2ProfileComboBox, ApplicationContext.listC2Profile(currentPayload));
                    }
                }

            }
        });
        this.groupIdTextField.setEditable(false);
        this.groupIdTextField.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                String group = (new ChooseGroup(UiFunction.getParentWindow(BasicShellSetting.this.groupIdTextField), BasicShellSetting.this.groupIdTextField.getText())).getChooseGroup();
                if (group != null) {
                    BasicShellSetting.this.groupIdTextField.setText(group);
                } else {
                    Log.log("\u53d6\u6d88\u9009\u62e9......");
                }

            }
        });
        this.setButton.setText(this.shellId != null && this.shellId.trim().length() > 0 ? "\u4fee\u6539" : "\u6dfb\u52a0");
        functions.fireActionEventByJComboBox(this.payloadComboBox);
        this.initShellContent();
        this.add(this.corePanel);
        automaticBindClick.bindJButtonClick(this, this);
        functions.setWindowSize(this, 490, 600);
        this.clientSSLCertPathTextField.setColumns(150);
        this.setLocationRelativeTo(MainActivity.getFrame());
        this.setDefaultCloseOperation(2);
        EasyI18N.installObject(this);
        this.setVisible(true);
    }

    private void $$$setupUI$$$() {
        this.createUIComponents();
        this.corePanel = new JPanel();
        this.corePanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        this.tabbedPane1 = new JTabbedPane();
        this.corePanel.add(this.tabbedPane1, new GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, (Dimension)null, new Dimension(200, 200), (Dimension)null, 0, false));
        JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        this.tabbedPane1.addTab("\u57fa\u7840\u914d\u7f6e", panel1);
        JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(16, 2, new Insets(0, 80, 0, 80), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 0, 1, 1, 0, 1, 3, 3, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.urlLabel = new JLabel();
        this.urlLabel.setText("URL");
        panel2.add(this.urlLabel, new GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, (Dimension)null, new Dimension(130, -1), (Dimension)null, 0, false));
        this.urlTextField = new JTextField();
        this.urlTextField.setColumns(0);
        this.urlTextField.setText("http://127.0.0.1/shell.jsp");
        panel2.add(this.urlTextField, new GridConstraints(0, 1, 1, 1, 8, 1, 4, 3, (Dimension)null, new Dimension(150, -1), (Dimension)null, 0, false));
        this.passwordLabel = new JLabel();
        this.passwordLabel.setText("\u5bc6\u7801");
        panel2.add(this.passwordLabel, new GridConstraints(1, 0, 1, 1, 8, 0, 0, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.passwordTextField = new JTextField();
        this.passwordTextField.setText("pass");
        panel2.add(this.passwordTextField, new GridConstraints(1, 1, 1, 1, 8, 1, 4, 0, (Dimension)null, new Dimension(150, -1), (Dimension)null, 0, false));
        this.secretKeyLabel = new JLabel();
        this.secretKeyLabel.setText("\u5bc6\u94a5");
        panel2.add(this.secretKeyLabel, new GridConstraints(2, 0, 1, 1, 8, 0, 0, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.secretKeyTextField = new JTextField();
        this.secretKeyTextField.setColumns(100);
        this.secretKeyTextField.setText("key");
        panel2.add(this.secretKeyTextField, new GridConstraints(2, 1, 1, 1, 8, 1, 4, 0, (Dimension)null, new Dimension(150, -1), (Dimension)null, 0, false));
        this.connTimeOutLabel = new JLabel();
        this.connTimeOutLabel.setText("\u8fde\u63a5\u8d85\u65f6");
        panel2.add(this.connTimeOutLabel, new GridConstraints(3, 0, 1, 1, 8, 0, 0, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.connTimeOutTextField = new JTextField();
        this.connTimeOutTextField.setText("10000");
        panel2.add(this.connTimeOutTextField, new GridConstraints(3, 1, 1, 1, 8, 1, 4, 0, (Dimension)null, new Dimension(150, -1), (Dimension)null, 0, false));
        this.readTimeOutLabel = new JLabel();
        this.readTimeOutLabel.setText("\u8bfb\u53d6\u8d85\u65f6");
        panel2.add(this.readTimeOutLabel, new GridConstraints(4, 0, 1, 1, 8, 0, 0, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.readTimeOutTextField = new JTextField();
        this.readTimeOutTextField.setText("60000");
        panel2.add(this.readTimeOutTextField, new GridConstraints(4, 1, 1, 1, 8, 1, 4, 0, (Dimension)null, new Dimension(150, -1), (Dimension)null, 0, false));
        this.proxyHostLabel = new JLabel();
        this.proxyHostLabel.setText("\u4ee3\u7406\u4e3b\u673a");
        panel2.add(this.proxyHostLabel, new GridConstraints(6, 0, 1, 1, 8, 0, 0, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.proxyHostTextField = new JTextField();
        this.proxyHostTextField.setText("127.0.0.1");
        panel2.add(this.proxyHostTextField, new GridConstraints(6, 1, 1, 1, 8, 1, 4, 0, (Dimension)null, new Dimension(150, -1), (Dimension)null, 0, false));
        this.proxyPortLabel = new JLabel();
        this.proxyPortLabel.setText("\u4ee3\u7406\u7aef\u53e3");
        panel2.add(this.proxyPortLabel, new GridConstraints(7, 0, 1, 1, 8, 0, 0, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.proxyPortTextField = new JTextField();
        this.proxyPortTextField.setText("8080");
        panel2.add(this.proxyPortTextField, new GridConstraints(7, 1, 1, 1, 8, 1, 4, 0, (Dimension)null, new Dimension(150, -1), (Dimension)null, 0, false));
        this.remarkLabel = new JLabel();
        this.remarkLabel.setText("\u5907\u6ce8");
        panel2.add(this.remarkLabel, new GridConstraints(8, 0, 1, 1, 8, 0, 0, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.remarkTextField = new JTextField();
        this.remarkTextField.setText("\u5907\u6ce8");
        panel2.add(this.remarkTextField, new GridConstraints(8, 1, 1, 1, 8, 1, 4, 0, (Dimension)null, new Dimension(150, -1), (Dimension)null, 0, false));
        this.groupIdLabel = new JLabel();
        this.groupIdLabel.setText("GROUP");
        panel2.add(this.groupIdLabel, new GridConstraints(9, 0, 1, 1, 8, 0, 0, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.groupIdTextField = new JTextField();
        this.groupIdTextField.setEditable(false);
        this.groupIdTextField.setText("/");
        panel2.add(this.groupIdTextField, new GridConstraints(9, 1, 1, 1, 8, 1, 4, 0, (Dimension)null, new Dimension(150, -1), (Dimension)null, 0, false));
        this.proxyLabel = new JLabel();
        this.proxyLabel.setText("\u4ee3\u7406\u7c7b\u578b");
        panel2.add(this.proxyLabel, new GridConstraints(10, 0, 1, 1, 8, 0, 0, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.proxyComboBox = new JComboBox();
        panel2.add(this.proxyComboBox, new GridConstraints(10, 1, 1, 1, 0, 0, 2, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.encodingLabel = new JLabel();
        this.encodingLabel.setText("\u7f16\u7801");
        this.encodingLabel.setVisible(false);
        this.encodingComboBox = new JComboBox();
        this.encodingComboBox.setEditable(true);
        this.encodingComboBox.setVisible(false);
        this.payloadLabel = new JLabel();
        this.payloadLabel.setText("\u6709\u6548\u8f7d\u8377");
        panel2.add(this.payloadLabel, new GridConstraints(12, 0, 1, 1, 8, 0, 0, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.payloadComboBox = new JComboBox();
        panel2.add(this.payloadComboBox, new GridConstraints(12, 1, 1, 1, 0, 0, 2, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.cryptionLabel = new JLabel();
        this.cryptionLabel.setText("\u52a0\u5bc6\u5668");
        panel2.add(this.cryptionLabel, new GridConstraints(13, 0, 1, 1, 8, 0, 0, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.cryptionComboBox = new JComboBox();
        panel2.add(this.cryptionComboBox, new GridConstraints(13, 1, 1, 1, 0, 0, 2, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.c2ProfileLabel = new JLabel();
        this.c2ProfileLabel.setText("C2Profile");
        panel2.add(this.c2ProfileLabel, new GridConstraints(14, 0, 1, 1, 8, 0, 0, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.c2ProfileComboBox = new JComboBox();
        panel2.add(this.c2ProfileComboBox, new GridConstraints(14, 1, 1, 1, 0, 0, 2, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel3, new GridConstraints(15, 0, 1, 2, 0, 3, 3, 3, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.setButton = new JButton();
        this.setButton.setText("\u6dfb\u52a0");
        this.setButton.setVerticalAlignment(0);
        panel3.add(this.setButton, new GridConstraints(0, 0, 1, 1, 0, 3, 3, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.testButton = new JButton();
        this.testButton.setText("\u6d4b\u8bd5\u8fde\u63a5");
        panel3.add(this.testButton, new GridConstraints(0, 1, 1, 1, 0, 3, 3, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.errRetryNumLabel = new JLabel();
        this.errRetryNumLabel.setText("\u9519\u8bef\u91cd\u8bd5");
        panel2.add(this.errRetryNumLabel, new GridConstraints(5, 0, 1, 1, 8, 0, 0, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.errRetryNumTextField = new JTextField();
        this.errRetryNumTextField.setText("0");
        panel2.add(this.errRetryNumTextField, new GridConstraints(5, 1, 1, 1, 8, 1, 4, 0, (Dimension)null, new Dimension(150, -1), (Dimension)null, 0, false));
        JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        this.tabbedPane1.addTab("\u8bf7\u6c42\u914d\u7f6e", panel4);
        JSplitPane splitPane1 = new JSplitPane();
        splitPane1.setDividerLocation(150);
        splitPane1.setOrientation(0);
        panel4.add(splitPane1, new GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, (Dimension)null, new Dimension(200, 200), (Dimension)null, 0, false));
        JSplitPane splitPane2 = new JSplitPane();
        splitPane2.setDividerLocation(150);
        splitPane2.setDividerSize(10);
        splitPane2.setOrientation(0);
        splitPane1.setRightComponent(splitPane2);
        this.leftTextScrollPane.setIconRowHeaderEnabled(false);
        this.leftTextScrollPane.setLineNumbersEnabled(false);
        splitPane2.setLeftComponent(this.leftTextScrollPane);
        this.leftTextScrollPane.setBorder(BorderFactory.createTitledBorder((Border)null, "\u5de6\u8fb9\u8ffd\u52a0\u6570\u636e", 0, 0, (Font)null, (Color)null));
        this.leftTextArea.setEncoding("UTF-8");
        this.leftTextScrollPane.setViewportView(this.leftTextArea);
        this.rightTextScrollPane.setIconRowHeaderEnabled(false);
        this.rightTextScrollPane.setLineNumbersEnabled(false);
        splitPane2.setRightComponent(this.rightTextScrollPane);
        this.rightTextScrollPane.setBorder(BorderFactory.createTitledBorder((Border)null, "\u53f3\u8fb9\u8ffd\u52a0\u6570\u636e", 0, 0, (Font)null, (Color)null));
        this.rightTextArea.setEncoding("UTF-8");
        this.rightTextScrollPane.setViewportView(this.rightTextArea);
        this.headersTextScrollPane.setIconRowHeaderEnabled(false);
        this.headersTextScrollPane.setLineNumbersEnabled(false);
        splitPane1.setLeftComponent(this.headersTextScrollPane);
        this.headersTextScrollPane.setBorder(BorderFactory.createTitledBorder((Border)null, "\u534f\u8bae\u5934", 0, 0, (Font)null, (Color)null));
        this.headersTextArea.setEncoding("UTF-8");
        this.headersTextArea.setFractionalFontMetricsEnabled(false);
        this.headersTextArea.setLineWrap(false);
        this.headersTextArea.setRoundedSelectionEdges(false);
        this.headersTextArea.setTabsEmulated(false);
        this.headersTextArea.setText("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:84.0) Gecko/20100101 Firefox/84.0\nAccept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\nAccept-Language: zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2\n");
        this.headersTextArea.setUseSelectedTextColor(false);
        this.headersTextScrollPane.setViewportView(this.headersTextArea);
        JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        this.tabbedPane1.addTab("\u5176\u5b83\u914d\u7f6e", panel5);
        JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(6, 2, new Insets(0, 80, 0, 80), -1, -1));
        panel5.add(panel6, new GridConstraints(0, 0, 1, 1, 1, 1, 3, 3, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        JLabel label1 = new JLabel();
        label1.setText("\u5927\u6587\u4ef6\u5355\u6b21\u4e0a\u4f20\u5927\u5c0f");
        label1.setVerticalTextPosition(0);
        panel6.add(label1, new GridConstraints(1, 0, 1, 1, 8, 0, 0, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.bigFileUploadByteNumTextField = new JTextField();
        panel6.add(this.bigFileUploadByteNumTextField, new GridConstraints(1, 1, 1, 1, 8, 1, 4, 0, (Dimension)null, new Dimension(150, -1), (Dimension)null, 0, false));
        JLabel label2 = new JLabel();
        label2.setText("\u5927\u6587\u4ef6\u5355\u6b21\u4e0b\u8f7d\u5927\u5c0f");
        panel6.add(label2, new GridConstraints(2, 0, 1, 1, 8, 0, 0, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.bigFileDownloadByteNumTextField = new JTextField();
        panel6.add(this.bigFileDownloadByteNumTextField, new GridConstraints(2, 1, 1, 1, 8, 1, 4, 0, (Dimension)null, new Dimension(150, -1), (Dimension)null, 0, false));
        JLabel label3 = new JLabel();
        label3.setText("\u5ba2\u6237\u7aefSSL\u8bc1\u4e66\u8def\u5f84");
        panel6.add(label3, new GridConstraints(4, 0, 1, 1, 8, 0, 0, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.clientSSLCertPathTextField = new JTextField();
        panel6.add(this.clientSSLCertPathTextField, new GridConstraints(4, 1, 1, 1, 8, 1, 4, 0, (Dimension)null, new Dimension(150, -1), (Dimension)null, 0, false));
        JLabel label4 = new JLabel();
        label4.setText("\u5ba2\u6237\u7aef\u8bc1\u4e66\u5bc6\u7801");
        panel6.add(label4, new GridConstraints(5, 0, 1, 1, 8, 0, 0, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.clientSSLCertPasswordTextField = new JTextField();
        panel6.add(this.clientSSLCertPasswordTextField, new GridConstraints(5, 1, 1, 1, 8, 1, 4, 0, (Dimension)null, new Dimension(150, -1), (Dimension)null, 0, false));
        JLabel label5 = new JLabel();
        label5.setText("\u662f\u5426\u5408\u5e76\u54cd\u5e94\u5305Cookie");
        panel6.add(label5, new GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.isMergeResponseCookieCheckBox = new JCheckBox();
        this.isMergeResponseCookieCheckBox.setSelected(true);
        this.isMergeResponseCookieCheckBox.setText("\u5408\u5e76");
        panel6.add(this.isMergeResponseCookieCheckBox, new GridConstraints(0, 1, 1, 1, 8, 0, 3, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        JLabel label6 = new JLabel();
        label6.setText("\u5927\u6587\u4ef6\u4e0b\u8f7d\u7ebf\u7a0b\u6570\u91cf");
        panel6.add(label6, new GridConstraints(3, 0, 1, 1, 8, 0, 0, 0, (Dimension)null, (Dimension)null, (Dimension)null, 0, false));
        this.bigFileDownloadThreadNumTextField = new JTextField();
        this.bigFileDownloadThreadNumTextField.setText("10");
        panel6.add(this.bigFileDownloadThreadNumTextField, new GridConstraints(3, 1, 1, 1, 8, 1, 4, 0, (Dimension)null, new Dimension(150, -1), (Dimension)null, 0, false));
        this.connTimeOutLabel.setLabelFor(this.connTimeOutTextField);
    }

    public JComponent $$$getRootComponent$$$() {
        return this.corePanel;
    }

    private void initShellContent() {
        if (this.shellId != null && this.shellId.trim().length() > 0) {
            this.initUpdateShellValue(this.shellId);
        } else {
            this.initAddShellValue();
        }

        this.groupIdTextField.setText(this.currentGroup);
        this.c2ProfileComboBox.setSelectedItem(this.currentC2profile);
    }

    private void addToComboBox(JComboBox<String> comboBox, String[] data) {
        for(int i = 0; i < data.length; ++i) {
            comboBox.addItem(data[i]);
        }

    }

    private void initAddShellValue() {
        this.shellContext = new ShellEntity();
        this.headersTextArea.setText(ApplicationContext.getGloballHttpHeader());
        this.bigFileDownloadByteNumTextField.setText(String.valueOf(Db.getSetingIntValue("onceBigFileDownloadByteNumTextField", 1048576)));
        this.bigFileUploadByteNumTextField.setText(String.valueOf(Db.getSetingIntValue("onceBigFileUploadByteNumTextField", 1048576)));
        this.isMergeResponseCookieCheckBox.setSelected(true);
        if (this.currentGroup == null) {
            this.currentGroup = "/";
        }

        if (this.currentC2profile == null) {
            this.currentC2profile = "";
        }

    }

    private void initUpdateShellValue(String id) {
        this.shellContext = Db.getOneShell(id);
        this.urlTextField.setText(this.shellContext.getUrl());
        this.passwordTextField.setText(this.shellContext.getPassword());
        this.secretKeyTextField.setText(this.shellContext.getSecretKey());
        this.proxyHostTextField.setText(this.shellContext.getProxyHost());
        this.proxyPortTextField.setText(Integer.toString(this.shellContext.getProxyPort()));
        this.connTimeOutTextField.setText(Integer.toString(this.shellContext.getConnTimeout()));
        this.readTimeOutTextField.setText(Integer.toString(this.shellContext.getReadTimeout()));
        this.remarkTextField.setText(this.shellContext.getRemark());
        this.errRetryNumTextField.setText(String.valueOf(this.shellContext.getMaxErrRetry()));
        this.headersTextArea.setText(this.shellContext.getHeaderS());
        this.leftTextArea.setText(this.shellContext.getReqLeft());
        this.rightTextArea.setText(this.shellContext.getReqRight());
        this.proxyComboBox.setSelectedItem(this.shellContext.getProxyType());
        this.encodingComboBox.setSelectedItem(this.shellContext.getEncoding());
        this.payloadComboBox.setSelectedItem(this.shellContext.getPayload());
        this.cryptionComboBox.setSelectedItem(this.shellContext.getCryption());
        this.bigFileDownloadByteNumTextField.setText(String.valueOf(this.shellContext.getOnceBigFileDownloadByteNum()));
        this.bigFileUploadByteNumTextField.setText(String.valueOf(this.shellContext.getOnceBigFileUploadByteNum()));
        this.bigFileDownloadThreadNumTextField.setText(String.valueOf(this.shellContext.getBigFileDownloadThreadNum()));
        this.clientSSLCertPasswordTextField.setText(this.shellContext.getClientCertPassword());
        this.clientSSLCertPathTextField.setText(this.shellContext.getClientCertPath());
        this.isMergeResponseCookieCheckBox.setSelected(this.shellContext.isMergeResponseCookie());
        if (this.shellId != null) {
            if (this.currentGroup == null || this.currentGroup.isEmpty()) {
                this.currentGroup = this.shellContext.getGroup();
            }

            if (this.currentC2profile == null || this.currentC2profile.isEmpty()) {
                this.currentC2profile = this.shellContext.getC2ProfileName();
            }
        }

    }

    private void testButtonClick(ActionEvent actionEvent) {
        int oldErrRetry = this.shellContext.getMaxErrRetry();
        if (this.updateTempShellEntity()) {
            if (this.shellContext.initShellOpertion()) {
                GOptionPane.showMessageDialog(this, "Success!", "\u63d0\u793a", 1);
                Log.log(String.format("CloseShellState: %s\tShellId: %s\tShellHash: %s", this.shellContext.getPayloadModule().close(), this.shellContext.getId(), this.shellContext.hashCode()));
            } else {
                GOptionPane.showMessageDialog(this, "\u6d4b\u8bd5\u5931\u8d25!", "\u63d0\u793a", 2);
            }
        } else {
            GOptionPane.showMessageDialog(this, this.error, "\u63d0\u793a", 2);
            this.error = null;
        }
        this.shellContext.setMaxErrRetry(oldErrRetry);
    }

    private void setButtonClick(ActionEvent actionEvent) {
        this.currentGroup = this.groupIdTextField.getText().trim();
        this.currentC2profile = (String)this.c2ProfileComboBox.getSelectedItem();
        if (this.updateTempShellEntity()) {
            if (this.shellId != null && this.shellId.trim().length() > 0) {
                if (Db.updateShell(this.shellContext) > 0) {
                    this.shellContext.setGroup(this.currentGroup);
                    this.shellContext.setC2ProfileName2(this.currentC2profile);
                    GOptionPane.showMessageDialog(this, "\u4fee\u6539\u6210\u529f", "\u63d0\u793a", 1);
                    this.dispose();
                } else {
                    GOptionPane.showMessageDialog(this, "\u4fee\u6539\u5931\u8d25", "\u63d0\u793a", 2);
                }
            } else if (Db.addShell(this.shellContext) > 0) {
                this.shellContext.setGroup(this.currentGroup);
                this.shellContext.setC2ProfileName2(this.currentC2profile);
                GOptionPane.showMessageDialog(this, "\u6dfb\u52a0\u6210\u529f", "\u63d0\u793a", 1);
                this.dispose();
            } else {
                GOptionPane.showMessageDialog(this, "\u6dfb\u52a0\u5931\u8d25", "\u63d0\u793a", 2);
            }
        } else {
            GOptionPane.showMessageDialog(this, this.error, "\u63d0\u793a", 2);
            this.error = null;
        }
    }

    private boolean updateTempShellEntity() {
        String url = this.urlTextField.getText();
        String password = this.passwordTextField.getText();
        String secretKey = this.secretKeyTextField.getText();
        String payload = (String)this.payloadComboBox.getSelectedItem();
        String cryption = (String)this.cryptionComboBox.getSelectedItem();
        String encoding = (String)this.encodingComboBox.getSelectedItem();
        if (encoding == null || encoding.trim().length() == 0) {
            encoding = "UTF-8";
        }
        String headers = this.headersTextArea.getText();
        String reqLeft = this.leftTextArea.getText();
        String reqRight = this.rightTextArea.getText();
        String proxyType = (String)this.proxyComboBox.getSelectedItem();
        String proxyHost = this.proxyHostTextField.getText();
        String remark = this.remarkTextField.getText();
        String c2ProfileName = (String)this.c2ProfileComboBox.getSelectedItem();
        int proxyPort;
        int connTimeout;
        int readTimeout;
        int errRetry;
        int bigFileDownloadByteNum;
        int bigFileUploadByteNum;
        int bigFileDownloadThreadNum;

        try {
            proxyPort = Integer.parseInt(this.proxyPortTextField.getText());
            connTimeout = Integer.parseInt(this.connTimeOutTextField.getText());
            readTimeout = Integer.parseInt(this.readTimeOutTextField.getText());
            errRetry = Integer.parseInt(this.errRetryNumTextField.getText());
            bigFileDownloadByteNum = Integer.parseInt(this.bigFileDownloadByteNumTextField.getText());
            bigFileUploadByteNum = Integer.parseInt(this.bigFileUploadByteNumTextField.getText());
            bigFileDownloadThreadNum = Integer.parseInt(this.bigFileDownloadThreadNumTextField.getText());
        } catch (Exception var22) {
            Log.error(var22);
            this.error = var22.getMessage();
            return false;
        }

        if (url != null && url.trim().length() > 0 && password != null && password.trim().length() > 0 && secretKey != null && secretKey.trim().length() > 0 && payload != null && payload.trim().length() > 0 && cryption != null && cryption.trim().length() > 0 && encoding != null && encoding.trim().length() > 0) {
            this.shellContext.setUrl(url == null ? "" : url);
            this.shellContext.setPassword(password == null ? "" : password);
            this.shellContext.setSecretKey(secretKey == null ? "" : secretKey);
            this.shellContext.setPayload(payload == null ? "" : payload);
            this.shellContext.setCryption(cryption == null ? "" : cryption);
            this.shellContext.setEncoding(encoding == null ? "" : encoding);
            this.shellContext.setHeader(headers == null ? "" : headers);
            this.shellContext.setReqLeft(reqLeft == null ? "" : reqLeft);
            this.shellContext.setReqRight(reqRight == null ? "" : reqRight);
            this.shellContext.setConnTimeout(connTimeout);
            this.shellContext.setReadTimeout(readTimeout);
            this.shellContext.setProxyType(proxyType == null ? "" : proxyType);
            this.shellContext.setProxyHost(proxyHost == null ? "" : proxyHost);
            this.shellContext.setProxyPort(proxyPort);
            this.shellContext.setRemark(remark == null ? "" : remark);
            this.shellContext.setC2ProfileName(c2ProfileName);
            this.shellContext.setMaxErrRetry(errRetry);
            this.shellContext.setOnceBigFileDownloadByteNum(bigFileDownloadByteNum);
            this.shellContext.setOnceBigFileUploadByteNum(bigFileUploadByteNum);
            this.shellContext.setBigFileDownloadThreadNum(bigFileDownloadThreadNum);
            this.shellContext.setMergeResponseCookie(this.isMergeResponseCookieCheckBox.isSelected());
            this.shellContext.setClientCertPath(this.clientSSLCertPathTextField.getText());
            this.shellContext.setClientCertPassword(this.clientSSLCertPasswordTextField.getText());
            return true;
        } else {
            this.error = "\u8bf7\u68c0\u67e5  url password secretKey payload cryption encoding \u662f\u5426\u586b\u5199\u5b8c\u6574";
            return false;
        }
    }

    public void dispose() {
        super.dispose();
        MainActivity.getMainActivityFrame().refreshShellView();
    }

    private void createUIComponents() {
        this.headersTextArea = new RTextArea();
        this.rightTextArea = new RTextArea();
        this.leftTextArea = new RTextArea();
        this.headersTextScrollPane = new RTextScrollPane(this.headersTextArea, true);
        this.rightTextScrollPane = new RTextScrollPane(this.rightTextArea, true);
        this.leftTextScrollPane = new RTextScrollPane(this.leftTextArea, true);
    }
}
