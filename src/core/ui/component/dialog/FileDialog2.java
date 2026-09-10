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
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import util.automaticBindClick;
import util.functions;

/** Path dialog for copy / rename / remote-download. Upload and download use GFileChooser. */
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
        this.srcSelectdFileButton = new JButton("...");
        this.destSelectdFileButton = new JButton("...");
        this.okButton = new JButton("\u786e\u5b9a");
        this.cancelButton = new JButton("\u53d6\u6d88");

        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 0;
        form.add(new JLabel(EasyI18N.getI18nString("\u6e90\u8def\u5f84")), gc);
        gc.gridx = 1;
        gc.weightx = 1;
        form.add(this.srcFileTextField, gc);
        gc.gridx = 2;
        gc.weightx = 0;
        form.add(this.srcSelectdFileButton, gc);

        gc.gridx = 0;
        gc.gridy = 1;
        form.add(new JLabel(EasyI18N.getI18nString("\u76ee\u6807\u8def\u5f84")), gc);
        gc.gridx = 1;
        gc.weightx = 1;
        form.add(this.destFileTextField, gc);
        gc.gridx = 2;
        gc.weightx = 0;
        form.add(this.destSelectdFileButton, gc);

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

        this.setMinimumSize(new Dimension(640, 160));
        functions.setWindowSize(this, 720, 170);
        this.setLocationRelativeTo(owner);
        this.setDefaultCloseOperation(2);
        EasyI18N.installObject(this);
        this.setVisible(true);
    }

    public JComponent $$$getRootComponent$$$() {
        return this.corePanel;
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
        File f = chooser.showOpenDialog(this.corePanel);
        if (f != null) {
            this.srcFileTextField.setText(f.getAbsolutePath());
        }
    }

    private void destSelectdFileButtonClick(ActionEvent e) {
        GFileChooser chooser = new GFileChooser();
        File f = chooser.showSaveDialog(this.corePanel);
        if (f != null) {
            this.destFileTextField.setText(f.getAbsolutePath());
        }
    }

    public static String joinDest(String dest, String fileName) {
        if (fileName == null || fileName.trim().length() == 0) {
            return dest == null ? "" : dest.trim();
        }
        String name = fileName.trim();
        if (dest == null || dest.trim().length() == 0) {
            return name;
        }
        String d = dest.trim();
        if (d.endsWith("/") || d.endsWith("\\")) {
            return d + name;
        }
        return d;
    }

    public static String baseName(String path) {
        if (path == null) {
            return "";
        }
        String p = path.trim().replace('\\', '/');
        int slash = p.lastIndexOf('/');
        return slash >= 0 ? p.substring(slash + 1) : p;
    }

    public static FileOpertionInfo showFileOpertion(Frame owner, String title, String srcFile, String destFile) {
        return showFileOpertion(owner, title, srcFile, destFile, false);
    }

    public static FileOpertionInfo showFileOpertion(Frame owner, String title, String srcFile, String destFile, boolean bigFile) {
        if (SwingUtilities.isEventDispatchThread()) {
            return new FileDialog2(owner, title, srcFile, destFile).getResult();
        }
        AtomicReference<FileOpertionInfo> ref = new AtomicReference<FileOpertionInfo>();
        try {
            SwingUtilities.invokeAndWait(() ->
                    ref.set(new FileDialog2(owner, title, srcFile, destFile).getResult()));
        } catch (Exception e) {
            FileOpertionInfo fail = new FileOpertionInfo();
            fail.setOpertionStatus(Boolean.FALSE);
            return fail;
        }
        return ref.get() != null ? ref.get() : new FileOpertionInfo();
    }
}
