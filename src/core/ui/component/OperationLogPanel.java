package core.ui.component;

import core.OperationLogRuntime;
import core.persist.OperationLogStore;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

/**
 * \u4e3b\u754c\u9762\u5e95\u90e8\u8fd0\u884c\u65e5\u5fd7\uff1a\u4ec5\u6807\u9898 + \u5168\u5bbd\u6587\u672c\uff08\u5206\u7ec4/\u6811\u5728\u83dc\u5355\u5ba1\u8ba1\u5bf9\u8bdd\u6846\u4e2d\uff09\u3002
 */
public class OperationLogPanel extends JPanel {

    private static final Color HEADER_FG = new Color(0x334155);
    private static final Color HEADER_BORDER = new Color(0xd4d4d8);
    private static final Color LOG_BG = new Color(0x111317);
    private static final Color LOG_FG = new Color(0xe4e4e7);
    private static final Color LOG_ACCENT_DIM = new Color(0x64748b);

    private final JTextArea logTextArea = new JTextArea();
    private final JScrollPane logScroll = new JScrollPane(logTextArea);
    private final JPanel headerInner = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
    private final AuroraBarPanel headerBar = new AuroraBarPanel(AuroraBarPanel.Variant.HEADER_LIGHT);
    private final JLabel titleLabel = new JLabel("\u8fd0\u884c\u65e5\u5fd7");
    private OperationLogRuntime.ChunkListener chunkListener;
    private boolean persistenceUi;
    private String boundCurrentSessionId;
    private float lastScale = 1f;

    public OperationLogPanel() {
        super(new BorderLayout(0, 0));
        setOpaque(false);
        headerInner.setOpaque(false);
        titleLabel.setForeground(HEADER_FG);
        headerInner.add(titleLabel);
        headerBar.add(headerInner, BorderLayout.WEST);
        headerBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, HEADER_BORDER),
                new EmptyBorder(0, 0, 0, 0)));
        logTextArea.setEditable(false);
        logTextArea.setLineWrap(true);
        logTextArea.setWrapStyleWord(true);
        logTextArea.setBackground(LOG_BG);
        logTextArea.setForeground(LOG_FG);
        logTextArea.setCaretColor(LOG_ACCENT_DIM);
        logTextArea.setSelectedTextColor(LOG_FG);
        logTextArea.setSelectionColor(new Color(0x8b5cf6));
        logTextArea.setBorder(BorderFactory.createEmptyBorder(6, 10, 8, 10));
        logScroll.setBorder(BorderFactory.createEmptyBorder());
        logScroll.getViewport().setBackground(LOG_BG);
        add(headerBar, BorderLayout.NORTH);
        add(logScroll, BorderLayout.CENTER);
        persistenceUi = false;
        applyUiScale(1f);
    }

    public void onPersistenceUnavailable() {
        Runnable r = () -> {
            if (chunkListener != null) {
                OperationLogRuntime.removeChunkListener(chunkListener);
                chunkListener = null;
            }
            persistenceUi = false;
            revalidate();
            repaint();
        };
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    public void onPersistenceReady(String sessionId, long groupId) {
        Runnable r = () -> {
            this.boundCurrentSessionId = sessionId;
            if (chunkListener != null) {
                OperationLogRuntime.removeChunkListener(chunkListener);
            }
            persistenceUi = true;
            logTextArea.setText(OperationLogStore.loadSessionText(sessionId));
            logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
            chunkListener = (sid, chunk) -> {
                if (sid != null && sid.equals(boundCurrentSessionId)) {
                    SwingUtilities.invokeLater(() -> {
                        logTextArea.append(chunk);
                        logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
                    });
                }
            };
            OperationLogRuntime.addChunkListener(chunkListener);
            applyUiScale(lastScale);
            revalidate();
            repaint();
        };
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    public void setAuroraEnabled(boolean enabled) {
        headerBar.setAuroraEnabled(enabled);
    }

    public void applyUiScale(float scale) {
        lastScale = Math.max(0.90f, Math.min(1.16f, scale));
        float s = lastScale;
        int gapH = Math.round(10 * s);
        int gapV = Math.round(4 * s);
        headerInner.setLayout(new FlowLayout(FlowLayout.LEFT, gapH, gapV));
        int marginH = Math.round(4 * s);
        headerBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 1, 0, HEADER_BORDER),
                new EmptyBorder(1, marginH, 1, marginH)));
        Font base = UIManager.getFont("Label.font");
        int titleSize = base != null
                ? Math.max(12, Math.round(base.getSize() * s * 1.02f))
                : Math.round(13 * s);
        titleLabel.setFont(base != null
                ? base.deriveFont(Font.BOLD, (float) titleSize)
                : new Font(Font.SANS_SERIF, Font.BOLD, titleSize));
        int mono = Math.max(11, Math.round(12 * s));
        Font global = core.ApplicationContext.getFont();
        logTextArea.setFont(global != null ? global.deriveFont(Font.PLAIN, mono) : new Font(Font.MONOSPACED, Font.PLAIN, mono));
        int prefH = Math.max(110, Math.round(148 * s));
        int prefW = Math.round(260 * s);
        logScroll.setPreferredSize(new Dimension(prefW, prefH));
        revalidate();
    }

    public void appendLine(String line) {
        if (line == null || line.isEmpty()) {
            return;
        }
        if (OperationLogStore.isAvailable() && persistenceUi) {
            OperationLogRuntime.appendPanelLine(line);
            return;
        }
        SwingUtilities.invokeLater(() -> {
            logTextArea.append(line);
            if (!line.endsWith("\n")) {
                logTextArea.append("\n");
            }
            logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
        });
    }
}
