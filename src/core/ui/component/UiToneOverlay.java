package core.ui.component;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;

/** Click-through veil: brightness (black/white) and overall gray wash. */
public class UiToneOverlay extends JComponent {

    private int brightness = 50;
    private int grayscale = 0;

    public UiToneOverlay() {
        setOpaque(false);
    }

    public void setBrightness(int value) {
        this.brightness = clamp(value);
        repaint();
    }

    public void setGrayscale(int value) {
        this.grayscale = clamp(value);
        repaint();
    }

    public int getBrightness() {
        return brightness;
    }

    public int getGrayscale() {
        return grayscale;
    }

    @Override
    public boolean contains(int x, int y) {
        return false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        if (grayscale > 0) {
            int a = Math.round(grayscale / 100f * 180f);
            g2.setColor(new Color(128, 128, 128, a));
            g2.fillRect(0, 0, w, h);
        }
        if (brightness < 50) {
            int a = Math.round((50 - brightness) / 50f * 200f);
            g2.setColor(new Color(0, 0, 0, a));
            g2.fillRect(0, 0, w, h);
        } else if (brightness > 50) {
            int a = Math.round((brightness - 50) / 50f * 90f);
            g2.setColor(new Color(255, 255, 255, a));
            g2.fillRect(0, 0, w, h);
        }
        g2.dispose();
    }

    private static int clamp(int v) {
        if (v < 0) {
            return 0;
        }
        if (v > 100) {
            return 100;
        }
        return v;
    }
}
