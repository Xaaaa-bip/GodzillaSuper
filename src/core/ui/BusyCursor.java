package core.ui;

import java.awt.Component;
import java.awt.Cursor;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;

/**
 * 统一的忙碌反馈：等待光标（可嵌套，最外层恢复），配合 {@link #runAsync} 把耗时操作
 * 挪到后台线程，避免界面在操作期间完全没反应、用户重复点击。
 */
public final class BusyCursor {

    private static final AtomicInteger DEPTH = new AtomicInteger();
    private static Cursor previous;

    private BusyCursor() {
    }

    public static void push(final Component c) {
        if (DEPTH.incrementAndGet() == 1 && c != null) {
            previous = c.getCursor();
            c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }
    }

    public static void pop(final Component c) {
        if (DEPTH.decrementAndGet() <= 0) {
            DEPTH.set(0);
            if (c != null) {
                c.setCursor(previous);
            }
        }
    }

    /** EDT 上的短操作：包一层等待光标。 */
    public static void run(final Component c, final Runnable body) {
        push(c);
        try {
            body.run();
        } finally {
            pop(c);
        }
    }

    /** 后台执行 body，完成后回到 EDT 执行 done（可为 null）。异常记日志，不静默丢。 */
    public static void runAsync(final Component c, final Runnable body, final Runnable done) {
        push(c);
        new Thread(new Runnable() {
            public void run() {
                try {
                    body.run();
                } catch (Throwable t) {
                    util.Log.error(t);
                } finally {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            pop(c);
                            if (done != null) {
                                done.run();
                            }
                        }
                    });
                }
            }
        }, "gsl5-busy").start();
    }
}
