package core.ui.component;

import core.ui.ModernUi;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Animated multi-stop gradient strip (aurora / iridescent) for status bar and log header.
 */
public class AuroraBarPanel extends JPanel {

    public enum Variant {
        /** Deep jewel tones, light text on top */
        STATUS_DARK,
        /** Pastel iridescent for light chrome */
        HEADER_LIGHT
    }

    private static final Color HEADER_SOLID = new Color(0xf4f4f5);

    private static final Color[] STATUS_COLORS = {
            new Color(0x1e293b),
            new Color(0x273549),
            new Color(0x1e293b),
            new Color(0x273549),
            new Color(0x1e293b),
            new Color(0x273549)
    };
    private static final Color[] HEADER_COLORS = {
            new Color(0xf4f4f5),
            new Color(0xefefef),
            new Color(0xf4f4f5),
            new Color(0xefefef),
            new Color(0xf4f4f5),
            new Color(0xefefef)
    };

    private final Variant variant;
    private float phase;
    private Timer timer;
    private boolean auroraEnabled = true;
    private boolean windowStopHookInstalled;

    public AuroraBarPanel(Variant variant) {
        super(new BorderLayout(0, 0));
        this.variant = variant;
        setOpaque(false);
    }

    public void setAuroraEnabled(boolean enabled) {
        boolean was = this.auroraEnabled;
        this.auroraEnabled = enabled;
        if (!enabled) {
            stopAuroraAnimation();
            phase = 0f;
        } else if (!was || timer == null) {
            startAuroraAnimation();
        }
        repaint();
    }

    public boolean isAuroraEnabled() {
        return auroraEnabled;
    }

    public void startAuroraAnimation() {
        if (!auroraEnabled) {
            return;
        }
        if (timer != null) {
            return;
        }
        timer = new Timer(48, e -> {
            phase += 0.008f;
            if (phase > 1f) {
                phase -= 1f;
            }
            repaint();
        });
        timer.setRepeats(true);
        ensureWindowStopHook();
        timer.start();
    }

    private void ensureWindowStopHook() {
        if (windowStopHookInstalled) {
            return;
        }
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w == null) {
            return;
        }
        windowStopHookInstalled = true;
        w.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                stopAuroraAnimation();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                stopAuroraAnimation();
            }
        });
    }

    public void stopAuroraAnimation() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        int w = Math.max(1, getWidth());
        int h = Math.max(1, getHeight());
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        if (!auroraEnabled) {
            g2.setColor(variant == Variant.STATUS_DARK ? ModernUi.STATUS_BAR_BG : HEADER_SOLID);
            g2.fillRect(0, 0, w, h);
            g2.dispose();
            super.paintComponent(g);
            return;
        }

        Color[] colors = variant == Variant.STATUS_DARK ? STATUS_COLORS : HEADER_COLORS;
        int n = colors.length;
        float[] fractions = new float[n];
        for (int i = 0; i < n; i++) {
            fractions[i] = i / (float) (n - 1);
        }

        float shift = phase * w * 0.85f;
        LinearGradientPaint paint = new LinearGradientPaint(
                -shift,
                0,
                w * 2f - shift,
                0,
                fractions,
                colors,
                MultipleGradientPaint.CycleMethod.REPEAT);
        g2.setPaint(paint);
        g2.fillRect(0, 0, w, h);
        g2.dispose();
        super.paintComponent(g);
    }
}
