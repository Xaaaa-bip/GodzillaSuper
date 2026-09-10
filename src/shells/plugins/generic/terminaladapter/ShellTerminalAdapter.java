package shells.plugins.generic.terminaladapter;

import core.Encoding;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import util.functions;

public abstract class ShellTerminalAdapter {
    protected Socket socket;
    protected BufferedReader bufferedReader;
    protected BufferedWriter bufferedWriter;
    protected DataInputStream dataInputStream;
    protected DataOutputStream dataOutputStream;
    protected boolean isStarted;

    public ShellTerminalAdapter() {
        this.isStarted = false;
    }

    public void start(Socket socket, String encoding) throws IOException {
        String charset = Encoding.resolveIoCharset(encoding);
        this.socket = socket;
        this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), charset));
        this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), charset));
        this.dataInputStream = new DataInputStream(socket.getInputStream());
        this.dataOutputStream = new DataOutputStream(socket.getOutputStream());
        this.isStarted = true;
    }

    public boolean stop() {
        if (!this.isStarted) {
            return false;
        }
        functions.close(this.bufferedReader);
        functions.close(this.bufferedWriter);
        functions.close(this.dataInputStream);
        functions.close(this.dataOutputStream);
        functions.close(this.socket);
        this.isStarted = false;
        return true;
    }

    public void consoleWriteCharacters(String text) {
    }

    public void consoleNextLine() {
        this.consoleWriteCharacters("\n");
    }

    public abstract javax.swing.JPanel getView();

    public static ShellTerminalAdapter createShellTerminal(boolean pty) {
        if (pty) {
            return new JeditermAdapter();
        }
        return new RawTerminalAdapter();
    }
}
