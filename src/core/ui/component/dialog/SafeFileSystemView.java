package core.ui.component.dialog;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.filechooser.FileSystemView;
import javax.swing.Icon;

/**
 * FileSystemView that never blocks the EDT on dead/disconnected Windows drives.
 * {@code File.listFiles()} / ShellFolder on an unmapped letter or stale UNC can
 * wait indefinitely; every disk call here is bounded.
 */
public class SafeFileSystemView extends FileSystemView {

    public static final long DEFAULT_TIMEOUT_MS = 2500L;
    public static final long ROOT_PROBE_MS = 800L;

    private static final ExecutorService POOL = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "gsl5-fs");
        t.setDaemon(true);
        return t;
    });

    private static volatile SafeFileSystemView INSTANCE;

    private final FileSystemView delegate;
    private final long timeoutMs;

    public SafeFileSystemView(FileSystemView delegate, long timeoutMs) {
        this.delegate = delegate == null ? FileSystemView.getFileSystemView() : delegate;
        this.timeoutMs = timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT_MS;
    }

    public static SafeFileSystemView get() {
        install();
        SafeFileSystemView v = INSTANCE;
        if (v == null) {
            synchronized (SafeFileSystemView.class) {
                v = INSTANCE;
                if (v == null) {
                    INSTANCE = v = new SafeFileSystemView(FileSystemView.getFileSystemView(), DEFAULT_TIMEOUT_MS);
                }
            }
        }
        return v;
    }

    /** Keep Windows ShellFolder so the look-in combo still lists Desktop and drives. */
    public static void install() {
        try {
            UIManager.put("FileChooser.useShellFolder", Boolean.TRUE);
        } catch (Throwable ignored) {
        }
    }

    public static void apply(JFileChooser chooser) {
        if (chooser != null) {
            chooser.setFileSystemView(get());
        }
    }

    public static boolean existsSafe(File file) {
        if (file == null) {
            return false;
        }
        Boolean ok = call(file::exists, Boolean.FALSE, ROOT_PROBE_MS);
        return ok != null && ok.booleanValue();
    }

    public static <T> T call(Callable<T> task, T fallback, long timeoutMs) {
        Future<T> future = POOL.submit(task);
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
            future.cancel(true);
            return fallback;
        } catch (Throwable t) {
            return fallback;
        }
    }

    private <T> T call(Callable<T> task, T fallback) {
        return call(task, fallback, this.timeoutMs);
    }

    @Override
    public File createNewFolder(File containingDir) throws IOException {
        try {
            File created = call(() -> delegate.createNewFolder(containingDir), null);
            if (created == null) {
                throw new IOException("create folder timed out or failed: " + containingDir);
            }
            return created;
        } catch (IOException e) {
            throw e;
        }
    }

    @Override
    public File[] getRoots() {
        File[] raw = call(() -> {
            File[] fromDelegate = delegate.getRoots();
            if (fromDelegate != null && fromDelegate.length > 0) {
                return fromDelegate;
            }
            return File.listRoots();
        }, File.listRoots(), ROOT_PROBE_MS);
        if (raw == null || raw.length == 0) {
            File[] listed = File.listRoots();
            return listed == null ? new File[0] : listed;
        }
        return raw;
    }

    @Override
    public File[] getFiles(File dir, boolean useFileHiding) {
        File[] files = call(() -> {
            File[] result = delegate.getFiles(dir, useFileHiding);
            return result == null ? new File[0] : result;
        }, new File[0]);
        return files == null ? new File[0] : files;
    }

    @Override
    public Boolean isTraversable(File f) {
        if (f == null) {
            return Boolean.FALSE;
        }
        if (delegate.isRoot(f) || delegate.isDrive(f) || delegate.isFileSystemRoot(f)
                || delegate.isComputerNode(f)) {
            return Boolean.TRUE;
        }
        Boolean trav = call(() -> delegate.isTraversable(f), Boolean.TRUE, ROOT_PROBE_MS);
        return trav == null ? Boolean.TRUE : trav;
    }

    @Override
    public File getChild(File parent, String fileName) {
        return call(() -> delegate.getChild(parent, fileName), parent == null ? new File(fileName) : new File(parent, fileName));
    }

    @Override
    public File getParentDirectory(File dir) {
        return call(() -> delegate.getParentDirectory(dir), dir == null ? null : dir.getParentFile());
    }

    @Override
    public File getHomeDirectory() {
        return call(delegate::getHomeDirectory, new File(System.getProperty("user.home", ".")));
    }

    @Override
    public File getDefaultDirectory() {
        return call(delegate::getDefaultDirectory, getHomeDirectory());
    }

    @Override
    public File createFileObject(File dir, String filename) {
        return delegate.createFileObject(dir, filename);
    }

    @Override
    public File createFileObject(String path) {
        return delegate.createFileObject(path);
    }

    @Override
    public boolean isHiddenFile(File f) {
        Boolean h = call(() -> Boolean.valueOf(delegate.isHiddenFile(f)), Boolean.FALSE, ROOT_PROBE_MS);
        return h != null && h.booleanValue();
    }

    @Override
    public boolean isRoot(File f) {
        return delegate.isRoot(f);
    }

    @Override
    public boolean isFileSystem(File f) {
        Boolean v = call(() -> Boolean.valueOf(delegate.isFileSystem(f)), Boolean.TRUE, ROOT_PROBE_MS);
        return v == null || v.booleanValue();
    }

    @Override
    public boolean isFileSystemRoot(File dir) {
        return delegate.isFileSystemRoot(dir);
    }

    @Override
    public boolean isDrive(File dir) {
        return delegate.isDrive(dir);
    }

    @Override
    public boolean isFloppyDrive(File dir) {
        return delegate.isFloppyDrive(dir);
    }

    @Override
    public boolean isComputerNode(File dir) {
        return delegate.isComputerNode(dir);
    }

    @Override
    public String getSystemDisplayName(File f) {
        return call(() -> delegate.getSystemDisplayName(f), f == null ? null : f.getName());
    }

    @Override
    public String getSystemTypeDescription(File f) {
        return call(() -> delegate.getSystemTypeDescription(f), null);
    }

    @Override
    public Icon getSystemIcon(File f) {
        return call(() -> delegate.getSystemIcon(f), null);
    }
}
