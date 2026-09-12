package core.ui.component;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.Timer;

/**
 * 主窗口的开合动效（默认关闭，可在「设置 → 界面效果 → 动效」里开启）。
 *
 * <p>Swing 没有真正的 3D：把窗口内容抓成快照，切成竖条逐条做透视变换，画在临时玻璃面板上，
 * 由 {@link Timer} 在 EDT 上逐帧驱动。两种样式：
 * <ul>
 *   <li><b>rotate</b>：绕竖直中轴 3D 旋转（卡片翻转 / 推门），近侧放大、远侧缩小变暗</li>
 *   <li><b>fold</b>：左右两半各自沿中缝对折</li>
 * </ul>
 * 动画结束后把原来的玻璃面板（如亮度遮罩 {@code UiToneOverlay}）装回去。
 */
public final class FoldTransition {

    private static final int TICK_MS = 16;              // ~60fps
    private static final double SHADE_ALPHA = 0.45;     // 折到最扁时的阴影强度
    private static final int STRIPS = 48;               // 每半张切成多少竖条做透视
    private static final int MIN_MS = 60;
    private static final int MAX_MS = 900;
    private static final int MAX_DELAY_MS = 2000;

    private FoldTransition() {
    }

    /** 展开动画的启动句柄：窗口 setVisible 之后、布局稳定了再调 {@link #start()}。 */
    public static final class Pending {
        private final Runnable starter;

        Pending(Runnable starter) {
            this.starter = starter;
        }

        public void start() {
            this.starter.run();
        }
    }

    public static boolean isAvailable() {
        return !GraphicsEnvironment.isHeadless();
    }

    public static int clampDuration(int ms) {
        return Math.max(MIN_MS, Math.min(MAX_MS, ms));
    }

    public static int clampDelay(int ms) {
        return Math.max(0, Math.min(MAX_DELAY_MS, ms));
    }

    /**
     * 装上覆盖层（此刻只画背景，避免闪出最终界面），返回句柄；窗口可见后再 start()。
     * start() 时才抓快照，确保拿到的是布局完成后的内容；delayMs > 0 时先等一会儿再动。
     */
    public static Pending foldOpen(final JFrame frame, final JComponent restoreGlass,
                                   final int durationMs, final int delayMs, final boolean rotate) {
        if (frame == null || !isAvailable()) {
            return new Pending(new Runnable() {
                public void run() {
                }
            });
        }
        final Overlay ov = new Overlay(frame, rotate);
        frame.setGlassPane(ov);
        ov.setVisible(true);
        final int total = clampDuration(durationMs);
        final int delay = clampDelay(delayMs);
        return new Pending(new Runnable() {
            public void run() {
                ov.capture();
                if (delay <= 0) {
                    animate(frame, ov, restoreGlass, true, total, null);
                    return;
                }
                Timer d = new Timer(delay, null);
                d.setRepeats(false);
                d.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        animate(frame, ov, restoreGlass, true, total, null);
                    }
                });
                d.start();
            }
        });
    }

    /** 收起（旋转 / 对折），动画结束后执行 onDone（通常是 dispose + 退出）。 */
    public static void foldClose(JFrame frame, JComponent restoreGlass, int durationMs,
                                 boolean rotate, Runnable onDone) {
        if (frame == null || !isAvailable()) {
            if (onDone != null) {
                onDone.run();
            }
            return;
        }
        try {
            Overlay ov = new Overlay(frame, rotate);
            ov.capture();
            frame.setGlassPane(ov);
            ov.setVisible(true);
            animate(frame, ov, restoreGlass, false, clampDuration(durationMs), onDone);
        } catch (Throwable t) {
            if (onDone != null) {
                onDone.run();
            }
        }
    }

    private static void animate(final JFrame frame, final Overlay ov, final JComponent restoreGlass,
                                final boolean opening, final int durationMs, final Runnable onDone) {
        final int total = Math.max(1, durationMs);
        final long start = System.currentTimeMillis();
        final Timer timer = new Timer(TICK_MS, null);
        timer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                double t = Math.min(1.0, (System.currentTimeMillis() - start) / (double) total);
                double eased = opening ? easeOut(t) : easeIn(t);
                ov.setProgress(opening ? eased : 1.0 - eased);
                ov.repaint();
                if (t < 1.0) {
                    return;
                }
                ((Timer) e.getSource()).stop();
                try {
                    if (restoreGlass != null) {
                        frame.setGlassPane(restoreGlass);
                        restoreGlass.setVisible(true);
                    } else {
                        ov.setVisible(false);
                    }
                } catch (Throwable ignored) {
                }
                frame.repaint();
                if (onDone != null) {
                    onDone.run();
                }
            }
        });
        timer.setCoalesce(true);
        timer.start();
    }

    private static double easeOut(double t) {
        return 1.0 - Math.pow(1.0 - t, 3);
    }

    private static double easeIn(double t) {
        return t * t * t;
    }

    /** 临时玻璃面板：把快照按 progress（0=收起, 1=展开）画成旋转 / 对折的样子。 */
    private static final class Overlay extends JComponent {
        private final JFrame frame;
        private final Color background;
        private final boolean rotate;
        private BufferedImage shot;
        private double progress;

        Overlay(JFrame frame, boolean rotate) {
            this.frame = frame;
            this.rotate = rotate;
            this.setOpaque(true);
            Container cp = frame.getContentPane();
            Color bg = cp != null ? cp.getBackground() : null;
            this.background = bg != null ? bg : Color.WHITE;
        }

        /** 抓当前内容面板的位图（不依赖可见性，随时可抓）。 */
        void capture() {
            if (this.shot != null) {
                return;
            }
            Container cp = this.frame.getContentPane();
            int w = Math.max(1, cp.getWidth());
            int h = Math.max(1, cp.getHeight());
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(this.background);
            g.fillRect(0, 0, w, h);
            cp.paint(g);
            g.dispose();
            this.shot = img;
        }

        void setProgress(double p) {
            this.progress = Math.max(0.0, Math.min(1.0, p));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            try {
                g.setColor(this.background);
                g.fillRect(0, 0, getWidth(), getHeight());
                if (this.shot == null) {
                    return; // 还没 start()：先只盖住内容
                }
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                int sw = this.shot.getWidth();
                int sh = this.shot.getHeight();
                double k = this.progress;
                // 快照按当前组件尺寸缩放（窗口被用户/WM 改过尺寸时也不变形）
                g.scale(getWidth() / (double) sw, getHeight() / (double) sh);
                if (this.rotate) {
                    drawRotate(g, k);
                } else {
                    drawFold(g, 0, sw / 2, k);
                    drawFold(g, sw / 2, sw, k);
                }
            } finally {
                g.dispose();
            }
        }

        /**
         * 绕竖直中轴做 3D 旋转（卡片翻转 / 推门）：整张图按竖条切分，每条水平按 cos(θ) 收进、
         * 纵向按 focal/(focal+z) 透视缩放；z 是该条到中轴的有符号距离乘 sin(θ)，
         * 所以一侧转近（放大）、另一侧转远（缩小并变暗）。open=1 正对屏幕，open=0 侧立成一条线。
         */
        private void drawRotate(Graphics2D g, double open) {
            int sw = this.shot.getWidth();
            int h = this.shot.getHeight();
            double cx = sw / 2.0;
            double cy = h / 2.0;
            double theta = (1.0 - open) * (Math.PI / 2.0);
            double cos = Math.max(1.0e-4, Math.cos(theta));
            double sin = Math.sin(theta);
            double focal = Math.max(240.0, sw * 1.25);
            double half = Math.max(1.0, sw / 2.0);
            for (int i = 0; i < STRIPS; i++) {
                int sx0 = sw * i / STRIPS;
                int sx1 = sw * (i + 1) / STRIPS;
                int w = sx1 - sx0;
                if (w <= 0) {
                    continue;
                }
                double d = ((sx0 + sx1) / 2.0) - cx;
                double z = d * sin;
                double pz = focal / (focal + z);
                AffineTransform at = new AffineTransform();
                at.translate(cx, cy);
                at.scale(cos, pz);
                at.translate(-cx, -cy);
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.transform(at);
                    g2.drawImage(this.shot, sx0, 0, sx1, h, sx0, 0, sx1, h, null);
                    double depthN = Math.min(1.0, Math.max(0.0, z) / half);
                    float alpha = (float) (SHADE_ALPHA * (1.0 - open) * (0.12 + 0.88 * depthN));
                    if (alpha > 0.002f) {
                        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                        g2.setColor(Color.BLACK);
                        g2.fillRect(sx0, 0, w, h);
                    }
                } finally {
                    g2.dispose();
                }
            }
        }

        /** 画半张图：这一半的竖条水平按 cos(θ) 压向中缝，z 取到中缝的距离（无符号），两半朝中缝对折。 */
        private void drawFold(Graphics2D g, int x0, int x1, double open) {
            int w = x1 - x0;
            if (w <= 0) {
                return;
            }
            int h = this.shot.getHeight();
            double cx = this.shot.getWidth() / 2.0;
            double cy = h / 2.0;
            double hinge = (x0 == 0) ? x1 : x0;
            double theta = (1.0 - open) * (Math.PI / 2.0);
            double cos = Math.max(1.0e-4, Math.cos(theta));
            double sin = Math.sin(theta);
            double focal = Math.max(240.0, (this.shot.getWidth() / 2.0) * 1.6);
            double half = Math.max(1.0, this.shot.getWidth() / 2.0);
            int strips = STRIPS;
            for (int i = 0; i < strips; i++) {
                int sx0 = x0 + w * i / strips;
                int sx1 = x0 + w * (i + 1) / strips;
                int sw = sx1 - sx0;
                if (sw <= 0) {
                    continue;
                }
                double d = Math.abs(((sx0 + sx1) / 2.0) - hinge);
                double z = d * sin;
                double pz = focal / (focal + z);
                AffineTransform at = new AffineTransform();
                at.translate(hinge, cy);
                at.scale(cos, pz);
                at.translate(-hinge, -cy);
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.transform(at);
                    g2.drawImage(this.shot, sx0, 0, sx1, h, sx0, 0, sx1, h, null);
                    double depthN = Math.min(1.0, d / half);
                    float alpha = (float) (SHADE_ALPHA * (1.0 - open) * (0.35 + 0.65 * depthN));
                    if (alpha > 0.002f) {
                        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                        g2.setColor(Color.BLACK);
                        g2.fillRect(sx0, 0, w, h);
                    }
                } finally {
                    g2.dispose();
                }
            }
        }
    }
}
