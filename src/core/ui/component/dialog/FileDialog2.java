package core.ui.component.dialog;

import core.EasyI18N;
import core.ui.component.model.FileOpertionInfo;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import util.automaticBindClick;
import util.functions;

/**
 * Upload / download / copy path dialog. Ctrl+V works in the fields
 * (MainActivity no longer steals paste from text inputs).
 */
public class FileDialog2 extends JDialog {
    public JTextField srcFileTextField;
    public JTextField destFileTextField;
    public JButton srcSelectdFileButton;
    public JButton destSelectdFileButton;
    public JButton okButton;
    public JButton cancelButton;
    public JPanel corePanel;
    private final FileOpertionInfo fileOpertionInfo = new FileOpertionInfo();
    private boolean state;

    private FileDialog2(Frame owner, String title, String srcFile, String destFile) {
        super(owner, title, true);
        this.corePanel = new JPanel(new BorderLayout(8, 8));
        this.corePanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 10, 16));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.fill = GridBagConstraints.HORIZONTAL;

        this.srcFileTextField = new JTextField(srcFile == null ? "" : srcFile, 42);
        this.destFileTextField = new JTextField(destFile == null ? "" : destFile, 42);
        enableEasyPaste(this.srcFileTextField);
        enableEasyPaste(this.destFileTextField);

        this.srcSelectdFileButton = new JButton("...");
        this.destSelectdFileButton = new JButton("...");
        this.okButton = new JButton("OK");
        this.cancelButton = new JButton("CANCEL");

        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 0;
        form.add(new JLabel(EasyI18N.getI18nString("\u6e90\u6587\u4ef6(srcFile):")), gc);
        gc.gridx = 1;
        gc.weightx = 1;
        form.add(this.srcFileTextField, gc);
        gc.gridx = 2;
        gc.weightx = 0;
        form.add(this.srcSelectdFileButton, gc);

        gc.gridx = 0;
        gc.gridy = 1;
        form.add(new JLabel(EasyI18N.getI18nString("\u76ee\u6807\u6587\u4ef6(destFile):")), gc);
        gc.gridx = 1;
        gc.weightx = 1;
        form.add(this.destFileTextField, gc);
        gc.gridx = 2;
        gc.weightx = 0;
        form.add(this.destSelectdFileButton, gc);

        JLabel hint = new JLabel(EasyI18N.getI18nString("\u8def\u5f84\u53ef\u76f4\u63a5\u7c98\u8d34\uff08Ctrl+V\uff09\uff0cEnter \u786e\u8ba4\uff0cEsc \u53d6\u6d88"));
        hint.setEnabled(false);
        gc.gridx = 0;
        gc.gridy = 2;
        gc.gridwidth = 3;
        gc.weightx = 1;
        form.add(hint, gc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(this.okButton);
        buttons.add(this.cancelButton);

        this.corePanel.add(form, BorderLayout.CENTER);
        this.corePanel.add(buttons, BorderLayout.SOUTH);
        this.setContentPane(this.corePanel);

        automaticBindClick.bindJButtonClick(this, this);
        this.getRootPane().setDefaultButton(this.okButton);
        this.getRootPane().registerKeyboardAction(e -> cancelButtonClick(null),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);

        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                FileDialog2.this.cancelButtonClick(null);
            }
        });

        this.setMinimumSize(new Dimension(720, 180));
        functions.setWindowSize(this, 820, 190);
        this.setLocationRelativeTo(owner);
        this.setDefaultCloseOperation(2);
        EasyI18N.installObject(this);
        this.setVisible(true);
    }

    public JComponent $$$getRootComponent$$$() {
        return this.corePanel;
    }

    private static void enableEasyPaste(JTextField field) {
        field.putClientProperty("JTextField.placeholderText", "");
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (field.getText() != null && !field.getText().isEmpty()) {
                    field.selectAll();
                }
            }
        });
    }

    public FileOpertionInfo getResult() {
        return this.fileOpertionInfo;
    }

    private void okButtonClick(ActionEvent e) {
        this.fileOpertionInfo.setOpertionStatus(Boolean.TRUE);
        this.changeFileInfo();
    }

    private void cancelButtonClick(ActionEvent e) {
        this.fileOpertionInfo.setOpertionStatus(Boolean.FALSE);
        this.changeFileInfo();
    }

    private void changeFileInfo() {
        this.fileOpertionInfo.setSrcFileName(this.srcFileTextField.getText());
        this.fileOpertionInfo.setDestFileName(this.destFileTextField.getText());
        this.state = true;
        this.dispose();
    }

    private void srcSelectdFileButtonClick(ActionEvent e) {
        GFileChooser chooser = new GFileChooser();
        boolean save = isDownloadLike();
        File f = save ? chooser.showSaveDialog(this.corePanel) : chooser.showOpenDialog(this.corePanel);
        if (f != null) {
            this.srcFileTextField.setText(f.getAbsolutePath());
            if (this.destFileTextField.getText() == null || this.destFileTextField.getText().trim().isEmpty()
                    || this.destFileTextField.getText().endsWith("/") || this.destFileTextField.getText().endsWith("\\")) {
                String dest = this.destFileTextField.getText() == null ? "" : this.destFileTextField.getText().trim();
                if (dest.endsWith("/") || dest.endsWith("\\")) {
                    this.destFileTextField.setText(dest + f.getName());
                }
            }
        }
    }

    private void destSelectdFileButtonClick(ActionEvent e) {
        GFileChooser chooser = new GFileChooser();
        File f = chooser.showSaveDialog(this.corePanel);
        if (f != null) {
            this.destFileTextField.setText(f.getAbsolutePath());
        }
    }

    private boolean isDownloadLike() {
        String t = this.getTitle();
        return t != null && t.toLowerCase().contains("download");
    }

    public static FileOpertionInfo showFileOpertion(Frame owner, String title, String srcFile, String destFile) {
        return new FileDialog2(owner, title, srcFile, destFile).getResult();
    }
}
