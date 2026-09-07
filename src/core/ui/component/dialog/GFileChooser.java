package core.ui.component.dialog;

import core.Db;
import core.EasyI18N;
import java.awt.Component;
import java.awt.HeadlessException;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

/**
 * Local file dialog. Always uses {@link SafeFileSystemView} and Swing (not
 * JavaFX/native), so a missing or disconnected drive letter cannot freeze the UI.
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
    private FileSystemView fsv = SafeFileSystemView.get();
    private String selectedFile;
    private String fileFilterDescription;
    private String[] fileFilterExtensions;

    public GFileChooser() {
    }

    public GFileChooser(FileSystemView fsv) {
        this();
        if (fsv != null) {
            this.fsv = fsv instanceof SafeFileSystemView ? fsv : new SafeFileSystemView(fsv, SafeFileSystemView.DEFAULT_TIMEOUT_MS);
        }
    }

    private static String getDefaultDirectory() {
        String fallback = new File("").getAbsolutePath();
        try {
            String stored = Db.getSetingValue(LAST_DIR_KEY, fallback);
            File dir = new File(stored);
            if (SafeFileSystemView.existsSafe(dir) && dir.isDirectory()) {
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
            if (SafeFileSystemView.existsSafe(candidate) && candidate.isDirectory()) {
                this.initDirectory = candidate.getAbsolutePath();
                startDir = candidate;
            } else {
                this.initDirectory = null;
            }
        }

        File selected = showSwingDialog(parent, type, startDir);
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

    private File showSwingDialog(Component parent, FileChooserType type, File startDir) throws HeadlessException {
        JFileChooser chooser = new JFileChooser();
        SafeFileSystemView.apply(chooser);
        if (this.fsv != null) {
            chooser.setFileSystemView(this.fsv);
        }
        if (startDir != null) {
            chooser.setCurrentDirectory(startDir);
        }
        if (this.title != null) {
            chooser.setDialogTitle(this.title);
        }
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
