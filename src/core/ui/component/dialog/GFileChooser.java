package core.ui.component.dialog;

import core.Db;
import core.EasyI18N;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Window;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

/**
 * Local file picker. Windows uses the native dialog so the directory
 * combo lists Desktop / This PC / drives. Other platforms use JFileChooser
 * with the default FileSystemView (do not wrap it — that empties the combo).
 */
public class GFileChooser {

    private static final String LAST_DIR_KEY = GFileChooser.class.getName() + "-lastDir";
    private static String StaticLastDirectory = getDefaultDirectory();

    enum FileChooserType {
        OPEN,
        SAVE
    }

    private String initDirectory = StaticLastDirectory;
    private String title;
    private FileSystemView fsv;
    private String selectedFile;
    private String fileFilterDescription;
    private String[] fileFilterExtensions;
    private String approveButtonText;

    public GFileChooser() {
    }

    public GFileChooser(FileSystemView fsv) {
        this.fsv = fsv;
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name", "");
        return os.toLowerCase().contains("win");
    }

    private static String getDefaultDirectory() {
        String fallback = new File("").getAbsolutePath();
        try {
            String stored = Db.getSetingValue(LAST_DIR_KEY, fallback);
            File dir = new File(stored);
            if (dir.isDirectory()) {
                return dir.getAbsolutePath();
            }
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    public void setFileSystemView(FileSystemView fsv) {
        this.fsv = fsv;
    }

    public void setTitle(String title) {
        this.title = EasyI18N.getI18nString(title);
    }

    public void setSelectedFile(String selectedFile) {
        this.selectedFile = selectedFile;
    }

    public void setFileFilter(String description, String... extensions) {
        this.fileFilterDescription = description;
        this.fileFilterExtensions = extensions;
    }

    public void setApproveButtonText(String approveButtonText) {
        this.approveButtonText = approveButtonText;
    }

    public String getTitle() {
        return this.title;
    }

    public FileSystemView getFsv() {
        return this.fsv;
    }

    public String getInitSelectedFile() {
        return this.selectedFile;
    }

    public String getFileFilterDescription() {
        return this.fileFilterDescription;
    }

    public String[] getFileFilterExtensions() {
        return this.fileFilterExtensions;
    }

    public File showOpenDialog(Component parent) throws HeadlessException {
        return showDialog(parent, FileChooserType.OPEN);
    }

    public File showSaveDialog(Component parent) throws HeadlessException {
        return showDialog(parent, FileChooserType.SAVE);
    }

    private File showDialog(Component parent, FileChooserType type) throws HeadlessException {
        if (!SwingUtilities.isEventDispatchThread()) {
            AtomicReference<File> ref = new AtomicReference<File>();
            AtomicReference<HeadlessException> err = new AtomicReference<HeadlessException>();
            try {
                SwingUtilities.invokeAndWait(() -> {
                    try {
                        ref.set(showDialogOnEdt(parent, type));
                    } catch (HeadlessException e) {
                        err.set(e);
                    }
                });
            } catch (Exception e) {
                throw new HeadlessException(String.valueOf(e.getMessage()));
            }
            if (err.get() != null) {
                throw err.get();
            }
            return ref.get();
        }
        return showDialogOnEdt(parent, type);
    }

    private File showDialogOnEdt(Component parent, FileChooserType type) throws HeadlessException {
        File startDir = null;
        if (this.initDirectory != null) {
            File candidate = new File(this.initDirectory);
            if (candidate.isDirectory()) {
                this.initDirectory = candidate.getAbsolutePath();
                startDir = candidate;
            } else {
                this.initDirectory = null;
            }
        }

        File selected;
        if (isWindows()) {
            selected = showNativeDialog(parent, type, startDir);
        } else {
            selected = showSwingDialog(parent, type, startDir);
        }
        if (selected != null && selected.getParentFile() != null) {
            String parentPath = selected.getParentFile().getAbsolutePath();
            if (StaticLastDirectory == null || !StaticLastDirectory.equals(parentPath)) {
                try {
                    Db.updateSetingKV(LAST_DIR_KEY, parentPath);
                } catch (Throwable ignored) {
                }
            }
            StaticLastDirectory = parentPath;
        }
        return selected;
    }

    private File showNativeDialog(Component parent, FileChooserType type, File startDir) {
        Window window = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        if (window == null && parent instanceof Window) {
            window = (Window) parent;
        }
        int mode = type == FileChooserType.SAVE ? FileDialog.SAVE : FileDialog.LOAD;
        FileDialog fd;
        if (window instanceof Frame) {
            fd = new FileDialog((Frame) window, this.title == null ? "" : this.title, mode);
        } else if (window instanceof Dialog) {
            fd = new FileDialog((Dialog) window, this.title == null ? "" : this.title, mode);
        } else {
            fd = new FileDialog((Frame) null, this.title == null ? "" : this.title, mode);
        }
        if (startDir != null) {
            fd.setDirectory(startDir.getAbsolutePath());
        }
        if (this.selectedFile != null && this.selectedFile.trim().length() > 0) {
            File hint = new File(this.selectedFile);
            fd.setFile(hint.getName());
            if (startDir == null && hint.getParentFile() != null && hint.getParentFile().isDirectory()) {
                fd.setDirectory(hint.getParentFile().getAbsolutePath());
            }
        }
        fd.setVisible(true);
        String name = fd.getFile();
        String dir = fd.getDirectory();
        if (name == null || dir == null) {
            return null;
        }
        return new File(dir, name);
    }

    private File showSwingDialog(Component parent, FileChooserType type, File startDir) throws HeadlessException {
        JFileChooser chooser = startDir != null ? new JFileChooser(startDir) : new JFileChooser();
        if (this.fsv != null) {
            chooser.setFileSystemView(this.fsv);
        }
        if (this.title != null) {
            chooser.setDialogTitle(this.title);
        }
        if (this.approveButtonText != null && this.approveButtonText.trim().length() > 0) {
            chooser.setApproveButtonText(this.approveButtonText);
        }
        chooser.setAcceptAllFileFilterUsed(true);
        if (this.selectedFile != null) {
            chooser.setSelectedFile(new File(this.selectedFile));
        }
        if (this.fileFilterDescription != null && this.fileFilterExtensions != null) {
            chooser.setFileFilter(new FileNameExtensionFilter(this.fileFilterDescription, this.fileFilterExtensions));
        }
        int result;
        if (type == FileChooserType.OPEN) {
            result = chooser.showOpenDialog(parent);
        } else if (type == FileChooserType.SAVE) {
            result = chooser.showSaveDialog(parent);
        } else {
            return null;
        }
        return result == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile() : null;
    }
}
