package core.ui.component;

import core.ApplicationContext;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import javax.swing.UIManager;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.TextEditorPane;

/**
 * Editor used by ShellcodeLoader and other plugins. RSyntaxTextArea freezes the
 * EDT when a multi-megabyte hex blob is pasted as a single line (tokenize +
 * current-line highlight + undo). Large hex pastes are wrapped and expensive
 * features are turned off.
 */
public class RTextArea extends TextEditorPane {
    private static final int LARGE_CHARS = 32 * 1024;
    private static final int HEX_WRAP = 128;
    private static boolean initialized;
    private PrintStream printStream;

    public RTextArea() {
        this.printStream = null;
        this.setBackgroundObject(UIManager.getColor("TextArea.background"));
        this.setForeground(UIManager.getColor("TextArea.foreground"));
        this.setSelectionColor(UIManager.getColor("TextArea.selectionBackground"));
        this.setCurrentLineHighlightColor(UIManager.getColor("TextArea.background"));
        if (initialized) {
            this.setFont(ApplicationContext.getFont());
        }
        this.setMarkAllHighlightColor(UIManager.getColor("TextArea.markAllHighlightColor"));
        this.setMarkOccurrencesColor(UIManager.getColor("TextArea.markOccurrencesColor"));
        this.setMatchedBracketBGColor(UIManager.getColor("TextArea.matchedBracketBackground"));
        this.setMatchedBracketBorderColor(UIManager.getColor("TextArea.matchedBracketBorderColor"));
        applyLiteMode();
    }

    private void applyLiteMode() {
        this.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        this.setCodeFoldingEnabled(false);
        this.setLineWrap(false);
        this.setHighlightCurrentLine(false);
        this.setMarkOccurrences(false);
        this.setBracketMatchingEnabled(false);
        this.setPaintMatchedBracketPair(false);
        this.setAnimateBracketMatching(false);
        this.setAutoIndentEnabled(false);
        this.setHyperlinksEnabled(false);
        this.setPaintTabLines(false);
    }

    public static void initialized() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        if (!initialized && stack.length > 2 && stack[2].getClassName().equals(ApplicationContext.class.getName())) {
            initialized = true;
        }
    }

    public synchronized PrintStream getPrintStream() {
        if (this.printStream == null) {
            this.printStream = new PrintStream(new RTextAreaPrintOutputStream(this));
        }
        return this.printStream;
    }

    public void setText(String t) {
        if (t != null && t.length() >= LARGE_CHARS) {
            applyLiteMode();
        }
        super.setText(t);
        if (t != null && t.length() >= LARGE_CHARS) {
            this.discardAllEdits();
        }
    }

    public void append(String str) {
        if (str != null && str.length() >= LARGE_CHARS) {
            applyLiteMode();
        }
        super.append(str);
        if (str != null && str.length() >= LARGE_CHARS) {
            this.discardAllEdits();
        }
    }

    public void paste() {
        try {
            Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this);
            if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String s = (String) t.getTransferData(DataFlavor.stringFlavor);
                if (s != null) {
                    if (s.length() >= LARGE_CHARS) {
                        applyLiteMode();
                        s = prepareLargeText(s);
                    }
                    super.replaceSelection(s);
                    if (s.length() >= LARGE_CHARS) {
                        this.discardAllEdits();
                    }
                    return;
                }
            }
        } catch (Exception ignored) {
        }
        super.paste();
    }

    private String prepareLargeText(String text) {
        if (text == null || text.length() < LARGE_CHARS) {
            return text;
        }
        applyLiteMode();
        if (looksLikeHexBlob(text) && hasOverlongLine(text, HEX_WRAP * 2)) {
            return wrapHexLines(text, HEX_WRAP);
        }
        return text;
    }

    private static boolean looksLikeHexBlob(String s) {
        int n = Math.min(s.length(), 4096);
        int hex = 0;
        int other = 0;
        for (int i = 0; i < n; ++i) {
            char c = s.charAt(i);
            if (c <= ' ') {
                continue;
            }
            if (isHexChar(c)) {
                ++hex;
            } else {
                ++other;
            }
        }
        return hex > 64 && other * 20 < hex;
    }

    private static boolean hasOverlongLine(String s, int limit) {
        int col = 0;
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (c == '\n' || c == '\r') {
                col = 0;
            } else if (++col > limit) {
                return true;
            }
        }
        return col > limit;
    }

    private static String wrapHexLines(String s, int width) {
        StringBuilder sb = new StringBuilder(s.length() + s.length() / width + 8);
        int col = 0;
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (c == '\r') {
                continue;
            }
            if (c == '\n') {
                sb.append('\n');
                col = 0;
                continue;
            }
            sb.append(c);
            if (!Character.isWhitespace(c) && ++col >= width) {
                sb.append('\n');
                col = 0;
            }
        }
        return sb.toString();
    }

    private static boolean isHexChar(char c) {
        return c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
    }

    static class RTextAreaPrintOutputStream extends OutputStream {
        final RTextArea this$0;

        RTextAreaPrintOutputStream(RTextArea owner) {
            this.this$0 = owner;
        }

        public void write(byte[] b, int off, int len) throws IOException {
            if (b == null) {
                throw new IllegalArgumentException("b");
            }
            this.this$0.append(new String(b, off, len));
        }

        public void write(int b) throws IOException {
            this.write(new byte[]{(byte) b}, 0, 1);
        }
    }
}
