package core.ui;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * Lets wallpaper show through the shell list table by using non-opaque cell renderers when unselected.
 * Does not call {@link JTable#getDefaultRenderer(Class)} on {@code DataView} — it casts to
 * {@link DefaultTableCellRenderer} and would break after wrapping.
 */
public final class WallpaperTableStyle {

    private WallpaperTableStyle() {
    }

    private static final class TranslucentCellWrapper implements TableCellRenderer {
        private final TableCellRenderer delegate;

        TranslucentCellWrapper(TableCellRenderer delegate) {
            this.delegate = delegate != null ? delegate : new DefaultTableCellRenderer();
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (c instanceof JComponent) {
                JComponent jc = (JComponent) c;
                if (isSelected) {
                    jc.setOpaque(true);
                } else {
                    jc.setOpaque(false);
                }
            }
            return c;
        }
    }

    public static void applyToShellTable(JTable table) {
        if (table == null) {
            return;
        }
        Color tb = UIManager.getColor("Table.background");
        // 没有壁纸时不需要半透明包装：直接用不透明主题色，省掉每格一次 alpha 合成（滚动更顺）
        boolean translucent = true;
        try {
            translucent = !core.ui.WallpaperManager.getStoredPathOrEmpty().isEmpty();
        } catch (Throwable ignored) {
        }
        if (tb != null) {
            table.setBackground(translucent ? new Color(tb.getRed(), tb.getGreen(), tb.getBlue(), 18) : tb);
        } else {
            table.setBackground(translucent ? new Color(255, 255, 255, 18) : Color.WHITE);
        }
        table.setOpaque(!translucent);

        TableColumnModel cm = table.getColumnModel();
        for (int i = 0; i < cm.getColumnCount(); i++) {
            TableColumn col = cm.getColumn(i);
            TableCellRenderer r = col.getCellRenderer();
            TableCellRenderer base;
            if (r instanceof TranslucentCellWrapper) {
                base = ((TranslucentCellWrapper) r).delegate;
            } else if (r != null) {
                base = r;
            } else {
                base = new DefaultTableCellRenderer();
            }
            if (base instanceof DefaultTableCellRenderer) {
                ((DefaultTableCellRenderer) base).setHorizontalAlignment(SwingConstants.CENTER);
            }
            if (translucent && !(r instanceof TranslucentCellWrapper)) {
                col.setCellRenderer(new TranslucentCellWrapper(base));
            } else if (!translucent && r instanceof TranslucentCellWrapper) {
                col.setCellRenderer(base);
            }
        }

        JTableHeader header = table.getTableHeader();
        if (header != null) {
            Color hb = UIManager.getColor("TableHeader.background");
            if (hb != null) {
                header.setBackground(new Color(hb.getRed(), hb.getGreen(), hb.getBlue(), 110));
            } else {
                header.setBackground(new Color(240, 240, 240, 110));
            }
            header.setOpaque(false);
            TableCellRenderer hr = header.getDefaultRenderer();
            if (hr instanceof DefaultTableCellRenderer) {
                ((DefaultTableCellRenderer) hr).setHorizontalAlignment(SwingConstants.CENTER);
            }
        }
    }
}
