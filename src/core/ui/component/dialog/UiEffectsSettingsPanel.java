package core.ui.component.dialog;

import core.Db;
import core.ui.MainActivity;
import core.ui.ModernUi;
import core.ui.WallpaperManager;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * \u5e94\u7528\u8bbe\u7f6e\u5bf9\u8bdd\u6846\u4e2d\u901a\u8fc7 registerPluginSeting \u589e\u52a0\u7684\u6807\u7b7e\u9875\uff1a\u754c\u9762\u6548\u679c\u3002
 */
public class UiEffectsSettingsPanel extends JPanel {

    private final JLabel wallpaperPathLabel;
    private JSlider brightSlider;
    private JSlider graySlider;
    private JCheckBox chkFold;
    private JComboBox<String> styleBox;
    private JSlider foldSlider;
    private JSlider delaySlider;

    public UiEffectsSettingsPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JPanel grid = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(4, 0, 4, 0);
        gc.gridx = 0;

        gc.gridy = 0;
        JCheckBox chkWin = new JCheckBox("\u542f\u7528\u7a97\u53e3\u534a\u900f\u660e");
        chkWin.setSelected(ModernUi.isWindowTranslucentEnabled());
        chkWin.addActionListener(e -> persistTranslucent(chkWin.isSelected()));
        grid.add(chkWin, gc);

        gc.gridy = 1;
        JCheckBox chkAurora = new JCheckBox("\u542f\u7528\u6781\u5149\u70ab\u5f69\u6e10\u53d8\uff08\u72b6\u6001\u680f\u4e0e\u8fd0\u884c\u65e5\u5fd7\u6807\u9898\uff09");
        chkAurora.setSelected(ModernUi.isAuroraGradientEnabled());
        chkAurora.addActionListener(e -> persistAurora(chkAurora.isSelected()));
        grid.add(chkAurora, gc);

        gc.gridy = 2;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        JLabel hint = new JLabel(
                "<html><body style='width:320px'><font color=#64748b>"
                        + "\u52fe\u9009\u4e3a\u5f00\u542f\uff0c\u53d6\u6d88\u52fe\u9009\u4e3a\u5173\u95ed\uff1b\u4fee\u6539\u540e\u7acb\u5373\u751f\u6548\u3002"
                        + "<br/>\u7a97\u53e3\u900f\u660e\u5728\u90e8\u5206\u7cfb\u7edf\u4e0a\u53ef\u80fd\u4e0d\u751f\u6548\u3002"
                        + "</font></body></html>");
        grid.add(hint, gc);

        gc.gridy = 3;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.insets = new Insets(14, 0, 4, 0);
        JLabel toneSec = new JLabel("<html><b>\u4eae\u5ea6 / \u7070\u5ea6</b></html>");
        grid.add(toneSec, gc);

        gc.gridy = 4;
        gc.insets = new Insets(2, 0, 4, 0);
        this.brightSlider = new JSlider(0, 100, MainActivity.readUiToneSetting("ui-brightness", 50));
        grid.add(buildToneRow("\u4eae\u5ea6", this.brightSlider, true), gc);

        gc.gridy = 5;
        this.graySlider = new JSlider(0, 100, MainActivity.readSavedGrayscale());
        grid.add(buildToneRow("\u7070\u5ea6", this.graySlider, false), gc);

        gc.gridy = 6;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0;
        gc.insets = new Insets(6, 0, 4, 0);
        JPanel toneBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton saveToneBtn = new JButton("\u4fdd\u5b58");
        saveToneBtn.addActionListener(e -> saveTone());
        JButton resetToneBtn = new JButton("\u6062\u590d\u9ed8\u8ba4");
        resetToneBtn.addActionListener(e -> resetTone());
        toneBtns.add(saveToneBtn);
        toneBtns.add(resetToneBtn);
        grid.add(toneBtns, gc);

        gc.gridy = 7;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.insets = new Insets(4, 0, 4, 0);
        JLabel toneHint = new JLabel(
                "<html><body style='width:320px'><font color=#64748b>"
                        + "\u62d6\u52a8\u6ed1\u5757\u53ef\u9884\u89c8\uff0c\u70b9\u4fdd\u5b58\u540e\u4e0b\u6b21\u542f\u52a8\u4ecd\u751f\u6548\u3002\u4eae\u5ea6 50 \u4e3a\u9ed8\u8ba4\uff0c\u7070\u5ea6 0 \u4e3a\u5173\u95ed\u3002"
                        + "</font></body></html>");
        grid.add(toneHint, gc);

        gc.gridy = 8;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.insets = new Insets(14, 0, 4, 0);
        JLabel sec = new JLabel("<html><b>\u81ea\u5b9a\u4e49\u58c1\u7eb8\uff08\u4e0e\u5f53\u524d\u4e3b\u9898\u878d\u5408\uff09</b></html>");
        grid.add(sec, gc);

        gc.gridy = 9;
        gc.insets = new Insets(2, 0, 4, 0);
        wallpaperPathLabel = new JLabel();
        refreshWallpaperPathLabel();
        grid.add(wallpaperPathLabel, gc);

        gc.gridy = 10;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 0;
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton pickBtn = new JButton("\u4e0a\u4f20\u58c1\u7eb8\u2026");
        pickBtn.addActionListener(e -> pickWallpaper());
        JButton clearBtn = new JButton("\u6e05\u9664\u58c1\u7eb8");
        clearBtn.addActionListener(e -> clearWallpaper());
        btnRow.add(pickBtn);
        btnRow.add(clearBtn);
        grid.add(btnRow, gc);

        gc.gridy = 11;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.insets = new Insets(4, 0, 4, 0);
        JLabel whint = new JLabel(
                "<html><body style='width:320px'><font color=#64748b>"
                        + "\u56fe\u7247\u4f1a\u590d\u5236\u5230\u5f53\u524d\u76ee\u5f55\u4e0b <code>wallpapers/</code> \uff0c"
                        + "\u4e3b\u533a\u57df\u4e3a\u6bdb\u73bb\u7483\u906e\u7f69\u4ee5\u4fbf\u9605\u8bfb\u8868\u683c\u3002"
                        + "</font></body></html>");
        grid.add(whint, gc);

        gc.gridy = 12;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.insets = new Insets(14, 0, 4, 0);
        grid.add(new JLabel("<html><b>动效</b></html>"), gc);

        gc.gridy = 13;
        gc.insets = new Insets(2, 0, 4, 0);
        this.chkFold = new JCheckBox("启用窗口开合动画（主窗口打开 / 关闭）");
        this.chkFold.setSelected(ModernUi.isFoldAnimEnabled());
        this.chkFold.addActionListener(e -> persistFoldAnim(this.chkFold.isSelected()));
        grid.add(this.chkFold, gc);

        gc.gridy = 14;
        JPanel styleRow = new JPanel(new BorderLayout(8, 0));
        styleRow.add(new JLabel("样式"), BorderLayout.WEST);
        this.styleBox = new JComboBox<>(new String[]{"旋转（绕竖轴 3D）", "折叠（沿中缝对折）"});
        this.styleBox.setSelectedIndex(ModernUi.isFoldRotate() ? 0 : 1);
        this.styleBox.addActionListener(e -> persistFoldStyle(this.styleBox.getSelectedIndex() == 1));
        styleRow.add(this.styleBox, BorderLayout.CENTER);
        grid.add(styleRow, gc);

        gc.gridy = 15;
        this.foldSlider = new JSlider(60, 900, ModernUi.getFoldDurationMs());
        grid.add(buildSliderRow("时长", this.foldSlider, true), gc);

        gc.gridy = 16;
        this.delaySlider = new JSlider(0, 2000, ModernUi.getFoldDelayMs());
        grid.add(buildSliderRow("延迟", this.delaySlider, false), gc);

        gc.gridy = 17;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.insets = new Insets(4, 0, 4, 0);
        grid.add(new JLabel(
                "<html><body style='width:320px'><font color=#64748b>"
                        + "默认关闭。延迟 = 窗口出现后等多久再开始动（0 表示立即）；"
                        + "点「预览」按当前设置重播一次。"
                        + "</font></body></html>"), gc);

        // 外面套滚动条：对话框尺寸较小时「动效」分区也能滚到
        add(new javax.swing.JScrollPane(grid), BorderLayout.CENTER);
    }

    /** 时长 / 延迟共用的一行：名称 + 滑块 + 数值 + 预览按钮。 */
    private JPanel buildSliderRow(String title, JSlider slider, boolean isDuration) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        JLabel value = new JLabel(String.valueOf(slider.getValue()));
        value.setPreferredSize(new java.awt.Dimension(44, 18));
        slider.addChangeListener(e -> {
            int v = slider.getValue();
            value.setText(String.valueOf(v));
            if (isDuration) {
                persistFoldMs(v);
            } else {
                persistFoldDelay(v);
            }
        });
        JButton previewBtn = new JButton("预览");
        previewBtn.addActionListener(e -> {
            MainActivity f = MainActivity.getMainActivityFrame();
            if (f != null) {
                f.replayFoldPreview();
            }
        });
        JPanel right = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        right.add(value);
        right.add(previewBtn);
        row.add(new JLabel(title), BorderLayout.WEST);
        row.add(slider, BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    private static void persistFoldAnim(boolean on) {
        Db.updateSetingKV(ModernUi.SETTING_FOLD_ANIM, String.valueOf(on));
        if (on) {
            MainActivity f = MainActivity.getMainActivityFrame();
            if (f != null) {
                f.replayFoldPreview();
            }
        }
    }

    private static void persistFoldMs(int ms) {
        Db.updateSetingKV(ModernUi.SETTING_FOLD_MS, String.valueOf(ms));
    }

    private static void persistFoldDelay(int ms) {
        Db.updateSetingKV(ModernUi.SETTING_FOLD_DELAY, String.valueOf(ms));
    }

    private static void persistFoldStyle(boolean foldStyle) {
        Db.updateSetingKV(ModernUi.SETTING_FOLD_STYLE,
                foldStyle ? ModernUi.FOLD_STYLE_FOLD : ModernUi.FOLD_STYLE_ROTATE);
        MainActivity f = MainActivity.getMainActivityFrame();
        if (f != null) {
            f.replayFoldPreview();
        }
    }

    private JPanel buildToneRow(String title, JSlider slider, boolean brightness) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        JLabel name = new JLabel(title);
        JLabel value = new JLabel(String.valueOf(slider.getValue()));
        value.setPreferredSize(new java.awt.Dimension(28, 18));
        slider.addChangeListener(e -> {
            int v = slider.getValue();
            value.setText(String.valueOf(v));
            MainActivity f = MainActivity.getMainActivityFrame();
            if (f != null) {
                if (brightness) {
                    f.setUiBrightness(v);
                } else {
                    f.setUiGrayscale(v);
                }
            }
        });
        row.add(name, BorderLayout.WEST);
        row.add(slider, BorderLayout.CENTER);
        row.add(value, BorderLayout.EAST);
        return row;
    }

    private void applyTonePreview() {
        MainActivity f = MainActivity.getMainActivityFrame();
        if (f != null) {
            f.setUiBrightness(this.brightSlider.getValue());
            f.setUiGrayscale(this.graySlider.getValue());
        }
    }

    private void saveTone() {
        Db.updateSetingKV("ui-brightness", String.valueOf(this.brightSlider.getValue()));
        Db.updateSetingKV("ui-grayscale", String.valueOf(this.graySlider.getValue()));
        Db.updateSetingKV("ui-tone-saved", "1");
        applyTonePreview();
        GOptionPane.showMessageDialog(this, "\u5df2\u4fdd\u5b58", "\u63d0\u793a", 1);
    }

    private void resetTone() {
        this.brightSlider.setValue(50);
        this.graySlider.setValue(0);
        applyTonePreview();
    }

    private void refreshWallpaperPathLabel() {
        String p = WallpaperManager.getStoredPathOrEmpty();
        if (p.isEmpty()) {
            wallpaperPathLabel.setText(
                    "<html><font color=#64748b>\u5f53\u524d\uff1a\u672a\u8bbe\u7f6e\u58c1\u7eb8\uff08\u4f7f\u7528\u4e3b\u9898\u80cc\u666f\u8272\uff09</font></html>");
        } else {
            String shortP = p.length() > 56 ? "\u2026" + p.substring(p.length() - 52) : p;
            wallpaperPathLabel.setText("<html>\u5f53\u524d\uff1a<code>" + shortP + "</code></html>");
        }
    }

    private void pickWallpaper() {
        JFileChooser fc = new JFileChooser();
        SafeFileSystemView.apply(fc);
        fc.setFileFilter(new FileNameExtensionFilter(
                "Images (png,jpg,jpeg,gif,bmp)", "png", "jpg", "jpeg", "gif", "bmp"));
        fc.setDialogTitle("\u9009\u62e9\u58c1\u7eb8\u56fe\u7247");
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File sel = fc.getSelectedFile();
        try {
            File dest = WallpaperManager.importWallpaperFile(sel);
            Db.updateSetingKV(ModernUi.SETTING_WALLPAPER_PATH, dest.getAbsolutePath());
            refreshWallpaperPathLabel();
            applyWallpaperToMainFrame();
            GOptionPane.showMessageDialog(this, "\u58c1\u7eb8\u5df2\u5e94\u7528\u3002", "\u63d0\u793a", 1);
        } catch (Exception ex) {
            GOptionPane.showMessageDialog(
                    this,
                    "\u65e0\u6cd5\u5bfc\u5165\uff1a" + ex.getMessage(),
                    "\u9519\u8bef",
                    0);
        }
    }

    private void clearWallpaper() {
        WallpaperManager.clearWallpaperSetting();
        refreshWallpaperPathLabel();
        applyWallpaperToMainFrame();
    }

    private static void persistTranslucent(boolean on) {
        Db.updateSetingKV(ModernUi.SETTING_WINDOW_TRANSLUCENT, String.valueOf(on));
        applyToMainFrame();
    }

    private static void persistAurora(boolean on) {
        Db.updateSetingKV(ModernUi.SETTING_AURORA_GRADIENT, String.valueOf(on));
        applyToMainFrame();
    }

    private static void applyToMainFrame() {
        MainActivity f = MainActivity.getMainActivityFrame();
        if (f != null) {
            f.applyUiEffectsFromSettings();
        }
    }

    private static void applyWallpaperToMainFrame() {
        MainActivity f = MainActivity.getMainActivityFrame();
        if (f != null) {
            f.reloadWallpaper();
        }
    }
}
