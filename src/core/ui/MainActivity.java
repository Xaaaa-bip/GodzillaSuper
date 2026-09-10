package core.ui;

import com.formdev.flatlaf.demo.intellijthemes.IJThemeInfo;
import com.formdev.flatlaf.demo.intellijthemes.IJThemesPanel;
import com.formdev.flatlaf.util.SystemInfo;
import core.ApplicationContext;
import core.Db;
import core.EasyI18N;
import core.OperationAuditLog;
import core.OperationLogRuntime;
import core.GodzillaObjectInputFilter;
import core.shell.ShellEntity;
import core.ui.component.C2ProfileManage;
import core.ui.component.DataView;
import core.ui.component.AuroraBarPanel;
import core.ui.component.FoldTransition;
import core.ui.component.WallpaperLayerPanel;
import core.ui.WallpaperTableStyle;
import core.ui.component.OperationLogPanel;
import core.ui.component.dialog.AppSetingDialog;
import core.ui.component.dialog.GOptionPane;
import core.ui.component.dialog.GenerateShellLoder;
import core.ui.component.dialog.OperationAuditLogDialog;
import core.ui.component.dialog.UiEffectsSettingsPanel;
import core.ui.component.dialog.PluginManage;
import core.ui.component.frame.BasicShellSetting;
import core.ui.component.frame.DatabaseShellSetting;
import core.ui.component.frame.LiveScan;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.PopupMenu;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.AWTEvent;
import java.awt.event.AWTEventListener;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import util.Log;
import util.StdoutTee;
import util.automaticBindClick;
import util.ipdb.IpLocationService;

public class MainActivity extends JFrame {
    private static final Pattern LOG_LINE_STD = Pattern.compile(
            "\\[[^]]+] Time:\\d{4}-\\d{2}-\\d{2} (\\d{2}:\\d{2}:\\d{2}) ThreadId:\\d+ Message: (.*)");
    private static final Pattern LOG_LINE_SUPER = Pattern.compile(
            "\\[[^]]+] Time:\\d{4}-\\d{2}-\\d{2} (\\d{2}:\\d{2}:\\d{2}) LastStackTrace:.* ThreadId:\\d+ Message: (.*)");
    private static final String SHELL_TABLE_COL_LOCATION = "\u5f52\u5c5e\u5730";
    private static final String KIND_HTTP = "http";
    private static final String KIND_DB = "db";
    private static final String GSL_EXPORT_PROTO = "gsl5://import?data=";
    private static final String FIELD_SEPARATOR = "";
    private static final String RECORD_SEPARATOR = "";
    private static MainActivity mainActivityFrame;
    private static JMenuBar menuBar;
    private static JMenu menuBarAboutMenu;
    private static final List<JMenu> menusPendingForMenuBar = new ArrayList<>();
    private static boolean generateDialogAutoCloseInstalled;
    private static boolean stdoutTeeInstalled;
    private JMenu targetMenu;
    private JMenu aboutMenu;
    private JMenu attackMenu;
    private JMenu configMenu;
    private static JMenu pluginMenu;
    private DataView shellView;
    private JScrollPane shellViewScrollPane;
    private static JPopupMenu shellViewPopupMenu;
    private Vector<String> columnVector;


    private JSplitPane splitPane;
    private JSplitPane verticalMainSplit;
    private OperationLogPanel operationLogPanel;
    private JList<String> kindList;
    private DefaultListModel<String> kindModel;
    private String currentKind;
    private String currentGroup;
    private JLabel statusLabel;
    private AuroraBarPanel statusAuroraPanel;
    private JMenuItem copyselectItem;
    private JMenuItem shareLinksItem;
    private JMenuItem interactMenuItem;
    private JMenuItem interactCacheMenuItem;
    private JMenuItem removeShell;
    private JMenuItem editShell;
    private JMenuItem refreshShell;
    private JPanel mainRootPanel;
    private JLabel targetIndicatorLabel;
    private WallpaperLayerPanel wallpaperLayer;
    private core.ui.component.UiToneOverlay toneOverlay;
    /** 启动时的开合动效句柄（windowOpened 后 start()） */
    private FoldTransition.Pending pendingFold;
    private JScrollPane shellGroupScrollPane;

    private static void hideShellViewPopupLater() {
        SwingUtilities.invokeLater(() -> {
            if (shellViewPopupMenu != null) {
                shellViewPopupMenu.setVisible(false);
            }

            MenuSelectionManager.defaultManager().clearSelectedPath();
        });
    }

    private static void bindAutoHidePopup(JMenuItem menuItem) {
        if (menuItem != null) {
            menuItem.addActionListener((e) -> {
                hideShellViewPopupLater();
            });
        }
    }

    private static String simplifyLogLine(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.trim();
        Matcher mSuper = LOG_LINE_SUPER.matcher(t);
        if (mSuper.find()) {
            return "[" + mSuper.group(1) + "] " + mSuper.group(2);
        }
        Matcher mStd = LOG_LINE_STD.matcher(t);
        if (mStd.find()) {
            return "[" + mStd.group(1) + "] " + mStd.group(2);
        }
        int msgIdx = t.lastIndexOf(" Message: ");
        if (msgIdx >= 0) {
            return t.substring(msgIdx + " Message: ".length());
        }
        return t;
    }

    private static void insertMenuBeforeAbout(JMenu menu) {
        if (menuBar == null || menu == null) {
            return;
        }
        int idx = menuBar.getMenuCount();
        if (menuBarAboutMenu != null) {
            for (int i = 0; i < menuBar.getMenuCount(); i++) {
                if (menuBar.getMenu(i) == menuBarAboutMenu) {
                    idx = i;
                    break;
                }
            }
        }
        menuBar.add(menu, idx);
        menuBar.revalidate();
    }

    private static void flushPendingMenusForMenuBar() {
        synchronized (menusPendingForMenuBar) {
            for (JMenu m : menusPendingForMenuBar) {
                insertMenuBeforeAbout(m);
            }
            menusPendingForMenuBar.clear();
        }
    }

    private static void initStatic() {
        menuBar = new JMenuBar();
        pluginMenu = new JMenu("\u63d2\u4ef6");
        shellViewPopupMenu = new JPopupMenu();
        AppSetingDialog.registerPluginSeting("\u754c\u9762\u6548\u679c", UiEffectsSettingsPanel.class);
    }

    public MainActivity() {
        this.initVariable();
        EasyI18N.installObject(this);
    }

    private void initVariable() {
        this.setLayout(new BorderLayout(2, 2));
        this.currentGroup = "/";
        this.currentKind = KIND_HTTP;
        this.operationLogPanel = new OperationLogPanel();
        OperationLogRuntime.bootstrap(this.operationLogPanel);
        if (!stdoutTeeInstalled) {
            stdoutTeeInstalled = true;
            final OperationLogPanel logSink = this.operationLogPanel;
            StdoutTee.setLineConsumer((line) -> logSink.appendLine(simplifyLogLine(line)));
            StdoutTee.install();
        }
        this.statusLabel = new JLabel("status");
        this.statusLabel.setOpaque(false);
        this.statusLabel.setForeground(ModernUi.STATUS_BAR_FG);
        this.statusAuroraPanel = new AuroraBarPanel(AuroraBarPanel.Variant.STATUS_DARK);
        this.statusLabel.setMinimumSize(new Dimension(0, 18));
        this.statusAuroraPanel.add(this.statusLabel, BorderLayout.CENTER);
        this.targetIndicatorLabel = new JLabel("localdb");
        this.targetIndicatorLabel.setForeground(Color.GRAY);
        this.targetIndicatorLabel.setFont(this.targetIndicatorLabel.getFont().deriveFont(Font.BOLD));
        this.targetIndicatorLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 12, 0, 8),
                BorderFactory.createEmptyBorder()));
        this.updateTargetIndicator();
        Vector<Vector<String>> headerRow = Db.getAllShell();
        this.columnVector = (Vector) headerRow.get(0).clone();
        this.columnVector.add(SHELL_TABLE_COL_LOCATION);
        this.shellView = new DataView((Vector) null, this.columnVector, -1, -1);
        this.refreshShellView();
        this.shellView.setSelectionMode(2);
        this.shellView.setShowGrid(false);
        this.shellView.setFillsViewportHeight(true);
        WallpaperTableStyle.applyToShellTable(this.shellView);
        this.splitPane = new JSplitPane(1);
        this.kindModel = new DefaultListModel<String>();
        this.kindList = new JList<String>(this.kindModel);
        this.kindList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.kindList.setFixedCellHeight(24);
        this.refreshKindList();
        this.kindList.setSelectedIndex(0);
        this.kindList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int idx = this.kindList.getSelectedIndex();
            String next = kindFromIndex(idx);
            if (next.equals(this.currentKind)) {
                return;
            }
            this.currentKind = next;
            OperationAuditLog.ui("\u4e3b\u754c\u9762", "\u5207\u6362\u5206\u7ec4", kindLabel(next));
            this.refreshShellView();
        });
        this.splitPane.setRightComponent(this.shellViewScrollPane = new JScrollPane(this.shellView));
        this.shellViewScrollPane.setBorder(BorderFactory.createEmptyBorder());
        this.splitPane.setLeftComponent(this.shellGroupScrollPane = new JScrollPane(this.kindList));
        this.shellGroupScrollPane.setMinimumSize(new Dimension(120, 0));
        this.kindList.setMinimumSize(new Dimension(120, 0));
        this.shellGroupScrollPane.getViewport().setMinimumSize(new Dimension(120, 0));
        this.verticalMainSplit = new JSplitPane(0, this.splitPane, this.operationLogPanel);
        this.verticalMainSplit.setResizeWeight(0.74);
        this.verticalMainSplit.setOneTouchExpandable(true);
        this.verticalMainSplit.setOpaque(false);
        this.splitPane.setOpaque(false);
        this.shellViewScrollPane.setOpaque(false);
        this.shellViewScrollPane.getViewport().setOpaque(false);
        this.shellViewScrollPane.getViewport().setBackground(new Color(255, 255, 255, 0));
        this.shellGroupScrollPane.setOpaque(false);
        this.shellGroupScrollPane.getViewport().setOpaque(false);
        WallpaperLayerPanel.FrostedWorkArea frostedCenter = new WallpaperLayerPanel.FrostedWorkArea(new BorderLayout(0, 0));
        frostedCenter.add(this.verticalMainSplit, BorderLayout.CENTER);
        this.mainRootPanel = new JPanel(new BorderLayout(0, 0));
        this.mainRootPanel.setOpaque(false);
        this.mainRootPanel.add(frostedCenter, BorderLayout.CENTER);
        this.mainRootPanel.add(this.statusAuroraPanel, BorderLayout.SOUTH);
        this.wallpaperLayer = new WallpaperLayerPanel();
        this.wallpaperLayer.setContentRoot(this.mainRootPanel);
        this.wallpaperLayer.reloadFromSettings();
        this.add(this.wallpaperLayer, BorderLayout.CENTER);
        this.splitPane.setResizeWeight(0.0);
        this.splitPane.setOneTouchExpandable(true);
        this.targetMenu = new JMenu("\u76ee\u6807");
        JMenuItem addShellMenuItem = new JMenuItem("\u6dfb\u52a0");
        addShellMenuItem.setActionCommand("addShell");
        JMenuItem addDatabaseShellMenuItem = new JMenuItem("\u6dfb\u52a0\u6570\u636e\u5e93Shell");
        addDatabaseShellMenuItem.setActionCommand("addDatabaseShell");
        JMenuItem exportLinksMenuItem = new JMenuItem("\u5206\u4eab\u94fe\u63a5\uff08\u652f\u6301\u591a\u9009\uff09");
        exportLinksMenuItem.setActionCommand("exportLinks");
        JMenuItem importLinkMenuItem = new JMenuItem("\u5bfc\u5165\u94fe\u63a5");
        importLinkMenuItem.setActionCommand("importLink");
        this.targetMenu.add(addShellMenuItem);
        this.targetMenu.add(addDatabaseShellMenuItem);
        this.targetMenu.addSeparator();
        this.targetMenu.add(exportLinksMenuItem);
        this.targetMenu.add(importLinkMenuItem);
        bindAutoHidePopup(addShellMenuItem);
        bindAutoHidePopup(addDatabaseShellMenuItem);
        bindAutoHidePopup(exportLinksMenuItem);
        bindAutoHidePopup(importLinkMenuItem);
        this.attackMenu = new JMenu("\u653b\u51fb");
        JMenuItem shellLiveScanMenuItem = new JMenuItem("\u5b58\u6d3b\u626b\u63cf");
        shellLiveScanMenuItem.setActionCommand("shellLiveScan");
        JMenuItem generateShellMenuItem = new JMenuItem("\u751f\u6210");
        generateShellMenuItem.setActionCommand("generateShell");
        this.attackMenu.add(generateShellMenuItem);
        this.attackMenu.add(shellLiveScanMenuItem);
        bindAutoHidePopup(generateShellMenuItem);
        bindAutoHidePopup(shellLiveScanMenuItem);
        this.configMenu = new JMenu("\u914d\u7f6e");
        JMenuItem pluginConfigMenuItem = new JMenuItem("\u63d2\u4ef6\u7ba1\u7406");
        pluginConfigMenuItem.setActionCommand("pluginConfig");
        JMenuItem appConfigMenuItem = new JMenuItem("\u5e94\u7528\u8bbe\u7f6e");
        appConfigMenuItem.setActionCommand("appConfig");
        JMenuItem c2ProfileConfigMenuItem = new JMenuItem("C2Profile \u8bbe\u7f6e");
        c2ProfileConfigMenuItem.setActionCommand("c2ProfileConfig");
        this.configMenu.add(c2ProfileConfigMenuItem);
        this.configMenu.add(appConfigMenuItem);
        this.configMenu.add(pluginConfigMenuItem);
        JMenuItem operationAuditLogMenuItem = new JMenuItem("\u64cd\u4f5c\u5ba1\u8ba1\u65e5\u5fd7");
        operationAuditLogMenuItem.setActionCommand("showOperationAuditLog");
        JMenuItem teamOpLogMenuItem = new JMenuItem("\u56e2\u961f\u64cd\u4f5c\u65e5\u5fd7");
        teamOpLogMenuItem.setActionCommand("showTeamOpLog");
        this.configMenu.add(operationAuditLogMenuItem);
        this.configMenu.add(teamOpLogMenuItem);
        bindAutoHidePopup(c2ProfileConfigMenuItem);
        bindAutoHidePopup(appConfigMenuItem);
        bindAutoHidePopup(pluginConfigMenuItem);
        bindAutoHidePopup(operationAuditLogMenuItem);
        this.aboutMenu = new JMenu("\u66f4\u65b0");
        JMenuItem sponsorMenuItem = new JMenuItem("\u8d5e\u52a9\u4f5c\u8005");
        sponsorMenuItem.setActionCommand("about");
        this.aboutMenu.add(sponsorMenuItem);
        bindAutoHidePopup(sponsorMenuItem);
        JMenuItem checkUpdateMenuItem = new JMenuItem("\u68c0\u67e5\u66f4\u65b0");
        checkUpdateMenuItem.setActionCommand("checkUpdate");
        this.aboutMenu.add(checkUpdateMenuItem);
        bindAutoHidePopup(checkUpdateMenuItem);
        JMenu dataSourceMenu = new JMenu("\u6570\u636e\u6e90");
        JMenuItem dataSourceMenuItem = new JMenuItem("\u6253\u5f00");
        dataSourceMenuItem.setActionCommand("dataSource");
        dataSourceMenu.add(dataSourceMenuItem);
        bindAutoHidePopup(dataSourceMenuItem);
        dataSourceMenu.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                MenuSelectionManager.defaultManager().clearSelectedPath();
                SwingUtilities.invokeLater(() -> MainActivity.this.dataSourceMenuItemClick(null));
            }
        });
        SvgIcons.apply(this.targetMenu, "target");
        SvgIcons.apply(addShellMenuItem, "add");
        SvgIcons.apply(addDatabaseShellMenuItem, "database");
        SvgIcons.apply(exportLinksMenuItem, "copy");
        SvgIcons.apply(importLinkMenuItem, "import");
        SvgIcons.apply(this.attackMenu, "attack");
        SvgIcons.apply(generateShellMenuItem, "generate");
        SvgIcons.apply(shellLiveScanMenuItem, "scan");
        SvgIcons.apply(this.configMenu, "settings");
        SvgIcons.apply(c2ProfileConfigMenuItem, "settings");
        SvgIcons.apply(appConfigMenuItem, "settings");
        SvgIcons.apply(pluginConfigMenuItem, "plugin");
        SvgIcons.apply(operationAuditLogMenuItem, "log");
        SvgIcons.apply(teamOpLogMenuItem, "team");
        SvgIcons.apply(this.aboutMenu, "update");
        SvgIcons.apply(sponsorMenuItem, "heart");
        SvgIcons.apply(checkUpdateMenuItem, "update");
        SvgIcons.apply(dataSourceMenu, "database");
        SvgIcons.apply(dataSourceMenuItem, "open");
        SvgIcons.apply(pluginMenu, "plugin");
        menuBar.add(this.targetMenu);
        menuBar.add(this.attackMenu);
        menuBar.add(this.configMenu);
        menuBar.add(pluginMenu);
        menuBar.add(dataSourceMenu);
        menuBar.add(this.aboutMenu);
        menuBarAboutMenu = this.aboutMenu;
        menuBar.add(this.targetIndicatorLabel);
        menuBar.add(Box.createHorizontalGlue());
        flushPendingMenusForMenuBar();
        this.setJMenuBar(menuBar);
        registerMcpMenuItem();
        automaticBindClick.bindMenuItemClick(menuBar, (Map) null, this);
        this.copyselectItem = new JMenuItem("\u590d\u5236\u9009\u4e2d");
        this.copyselectItem.setActionCommand("copyShellViewSelected");
        this.shareLinksItem = new JMenuItem("\u590d\u5236\u5206\u4eab\u94fe\u63a5\uff08\u652f\u6301\u591a\u9009\uff09");
        this.shareLinksItem.setActionCommand("copyShareLinks");
        this.interactMenuItem = new JMenuItem("\u4ea4\u4e92");
        this.interactMenuItem.setActionCommand("interact");
        this.interactCacheMenuItem = new JMenuItem("\u8fdb\u5165\u7f13\u5b58");
        this.interactCacheMenuItem.setActionCommand("interactCache");
        this.removeShell = new JMenuItem("\u79fb\u9664");
        this.removeShell.setActionCommand("removeShell");
        this.editShell = new JMenuItem("\u7f16\u8f91");
        this.editShell.setActionCommand("editShell");
        this.refreshShell = new JMenuItem("\u5237\u65b0");
        this.refreshShell.setActionCommand("refreshShellView");
        SvgIcons.apply(this.interactMenuItem, "interact");
        SvgIcons.apply(this.interactCacheMenuItem, "cache");
        SvgIcons.apply(this.copyselectItem, "copy");
        SvgIcons.apply(this.shareLinksItem, "copy");
        SvgIcons.apply(this.removeShell, "delete");
        SvgIcons.apply(this.editShell, "edit");
        SvgIcons.apply(this.refreshShell, "refresh");
        shellViewPopupMenu.add(this.interactMenuItem);
        shellViewPopupMenu.add(this.interactCacheMenuItem);
        shellViewPopupMenu.add(this.copyselectItem);
        shellViewPopupMenu.add(this.shareLinksItem);
        shellViewPopupMenu.add(this.removeShell);
        shellViewPopupMenu.add(this.editShell);
        shellViewPopupMenu.add(this.refreshShell);
        bindAutoHidePopup(this.interactMenuItem);
        bindAutoHidePopup(this.interactCacheMenuItem);
        bindAutoHidePopup(this.copyselectItem);
        bindAutoHidePopup(this.removeShell);
        bindAutoHidePopup(this.editShell);
        bindAutoHidePopup(this.refreshShell);
        this.shellView.setRightClickMenu(shellViewPopupMenu);
        automaticBindClick.bindMenuItemClick(shellViewPopupMenu, (Map) null, this);
        this.installGenerateDialogAutoCloseListener();
        shellViewPopupMenu.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                MainActivity.this.updateShellViewPopupMenuState();
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
        MouseAdapter shellViewPopupMouseAdapter = new MouseAdapter() {
            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger() && shellViewPopupMenu != null) {
                    if (MainActivity.this.shellView != null) {
                        int row = MainActivity.this.shellView.rowAtPoint(e.getPoint());
                        if (row >= 0) {
                            return;
                        } else {
                            MainActivity.this.shellView.clearSelection();
                        }
                    }

                    if (!shellViewPopupMenu.isVisible()) {
                        shellViewPopupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }

            public void mousePressed(MouseEvent e) {
                this.maybeShowPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                this.maybeShowPopup(e);
            }
        };
        this.shellView.addMouseListener(shellViewPopupMouseAdapter);
        this.shellViewScrollPane.getViewport().addMouseListener(shellViewPopupMouseAdapter);
        this.applyMainWindowLayout();
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                SwingUtilities.invokeLater(() -> {
                    MainActivity.this.applySplitDividerProportions();
                    // 布局稳定后再起展开动画（快照才是最终界面）
                    if (MainActivity.this.pendingFold != null) {
                        FoldTransition.Pending p = MainActivity.this.pendingFold;
                        MainActivity.this.pendingFold = null;
                        p.start();
                    }
                });
            }

            @Override
            public void windowClosing(WindowEvent e) {
                MainActivity.this.exitWithFold();
            }
        });
        this.setLocationRelativeTo((Component) null);
        this.applyUiEffectsFromSettings();
        this.installGlobalKeyboardHandler();
        this.installToneOverlay();
        // 先装覆盖层（不抓快照），窗口可见且布局完成后由 windowOpened 触发 start()
        if (ModernUi.isFoldAnimEnabled() && FoldTransition.isAvailable()) {
            this.pendingFold = FoldTransition.foldOpen(this, this.toneOverlay,
                    ModernUi.getFoldDurationMs(), ModernUi.getFoldDelayMs(), ModernUi.isFoldRotate());
        }
        this.setVisible(true);
        this.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
    }

    /** 关闭主窗口：开了动效就先播收起动画，再真正退出（等价于原来的 EXIT_ON_CLOSE）。 */
    private void exitWithFold() {
        if (ModernUi.isFoldAnimEnabled() && FoldTransition.isAvailable() && isShowing()) {
            FoldTransition.foldClose(this, this.toneOverlay, ModernUi.getFoldDurationMs(),
                    ModernUi.isFoldRotate(), new Runnable() {
                        public void run() {
                            MainActivity.this.dispose();
                            System.exit(0);
                        }
                    });
        } else {
            this.dispose();
            System.exit(0);
        }
    }

    /** 设置面板「预览」按钮：按当前设置重播一次展开动效。 */
    public void replayFoldPreview() {
        if (!isShowing() || !FoldTransition.isAvailable()) {
            return;
        }
        FoldTransition.Pending p = FoldTransition.foldOpen(this, this.toneOverlay,
                ModernUi.getFoldDurationMs(), ModernUi.getFoldDelayMs(), ModernUi.isFoldRotate());
        if (p != null) {
            p.start();
        }
    }

    private void installGlobalKeyboardHandler() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (e.getID() != KeyEvent.KEY_PRESSED) {
                    return false;
                }
                Window activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
                if (activeWindow != MainActivity.this && !isChildWindowOf(activeWindow, MainActivity.this)) {
                    return false;
                }
                if (!e.isControlDown()) {
                    return false;
                }
                if (isTextInputFocus()) {
                    return false;
                }
                if (e.getKeyCode() == KeyEvent.VK_C) {
                    if (shellView.getSelectedRowCount() > 0) {
                        SwingUtilities.invokeLater(() -> exportSelectedShells());
                        return true;
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_V) {
                    SwingUtilities.invokeLater(() -> importShellsFromClipboard());
                    return true;
                }
                return false;
            }
        });
    }

    /** Do not steal Ctrl+C/V from path fields, editors, or combo boxes. */
    private static boolean isTextInputFocus() {
        Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        while (c != null) {
            if (c instanceof javax.swing.text.JTextComponent) {
                javax.swing.text.JTextComponent t = (javax.swing.text.JTextComponent) c;
                return t.isEnabled() && t.isEditable();
            }
            if (c instanceof JComboBox && ((JComboBox<?>) c).isEditable()) {
                return true;
            }
            c = c.getParent();
        }
        return false;
    }

    private static boolean isChildWindowOf(Window child, Window parent) {
        if (child == null || parent == null) return false;
        if (child == parent) return true;
        Window owner = child.getOwner();
        while (owner != null) {
            if (owner == parent) return true;
            owner = owner.getOwner();
        }
        return false;
    }

    public void setUiBrightness(int value) {
        if (this.toneOverlay != null) {
            this.toneOverlay.setBrightness(value);
        }
    }

    public void setUiGrayscale(int value) {
        if (this.toneOverlay != null) {
            this.toneOverlay.setGrayscale(value);
        }
    }

    public static int readUiToneSetting(String key, int fallback) {
        return readUiTone(key, fallback);
    }

    private void installToneOverlay() {
        this.toneOverlay = new core.ui.component.UiToneOverlay();
        this.toneOverlay.setBrightness(readUiTone("ui-brightness", 50));
        this.toneOverlay.setGrayscale(readSavedGrayscale());
        this.setGlassPane(this.toneOverlay);
        this.toneOverlay.setVisible(true);
    }

    public static int readSavedGrayscale() {
        if (!Db.existsSetingKey("ui-tone-saved")) {
            return 0;
        }
        return readUiTone("ui-grayscale", 0);
    }

    private static int readUiTone(String key, int fallback) {
        try {
            String s = Db.tryGetSetingValue(key, String.valueOf(fallback));
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static void switchLocalDb() {
        try {
            java.lang.reflect.Field f = Db.class.getDeclaredField("dbConn");
            f.setAccessible(true);
            java.sql.Connection old = (java.sql.Connection) f.get(null);
            Class.forName("org.sqlite.JDBC");
            java.sql.Connection c = java.sql.DriverManager.getConnection("jdbc:sqlite:data.db");
            try (java.sql.Statement s = c.createStatement()) {
                s.execute("PRAGMA journal_mode=WAL");
            } catch (Exception ignored) {
            }
            f.set(null, c);
            isRemoteDb = false;
            remoteDbUrl = "";
            if (old != null && !old.isClosed()) {
                try {
                    old.close();
                } catch (Exception ex) {
                }
            }
            Log.log("Switched Db to local data.db");
        } catch (Exception e) {
            Log.error(e);
            javax.swing.JOptionPane.showMessageDialog(null, "\u5207\u636e\u672c\u5730\u5e93\u5931\u8d25: " + e.getMessage());
        }
    }

    public void applyUiEffectsFromSettings() {
        this.applyWindowOpacity();
        ShellLinkedWindowChrome.applyOpacityToAllShellWindows();
        boolean aurora = ModernUi.isAuroraGradientEnabled();
        if (this.statusAuroraPanel != null) {
            this.statusAuroraPanel.setAuroraEnabled(aurora);
        }
        if (this.operationLogPanel != null) {
            this.operationLogPanel.setAuroraEnabled(aurora);
        }
    }

    public void reloadWallpaper() {
        if (this.wallpaperLayer != null) {
            this.wallpaperLayer.reloadFromSettings();
        }
        ShellLinkedWindowChrome.reloadWallpaperOnAllShellWindows();
        repaint();
    }

    private void applyWindowOpacity() {
        ModernUi.applyWindowOpacityTo(this);
    }

    private static float computeMainUiScale(Rectangle usable) {
        float w = usable.width / 1366f;
        float h = usable.height / 768f;
        float s = Math.min(w, h);
        if (usable.width <= 1440) {
            s = Math.min(s, 1.02f);
        }
        if (usable.width <= 1280 || usable.height <= 800) {
            s = Math.min(s, 1.0f);
        }
        if (usable.width <= 1152) {
            s = Math.min(s, 0.98f);
        }
        return Math.max(0.92f, Math.min(1.14f, s));
    }

    private void applyMainWindowLayout() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle usable = ge.getMaximumWindowBounds();
        float scale = computeMainUiScale(usable);
        int gw = usable.width;
        int gh = usable.height;
        int targetW = Math.min(Math.round(gw * 0.92f), Math.round(1560 * Math.min(scale, 1.05f)));
        int targetH = Math.min(Math.round(gh * 0.88f), Math.round(920 * Math.min(scale, 1.05f)));
        targetW = Math.max(820, Math.min(targetW, gw - 16));
        targetH = Math.max(520, Math.min(targetH, gh - 16));
        this.setSize(targetW, targetH);
        this.setMinimumSize(new Dimension(880, 520));
        float marginFactor = Math.min(scale, 1.0f);
        int margin = Math.max(3, Math.round(6 * marginFactor));
        if (this.mainRootPanel != null) {
            this.mainRootPanel.setBorder(BorderFactory.createEmptyBorder(margin, margin, margin, margin));
        }
        if (this.operationLogPanel != null) {
            this.operationLogPanel.applyUiScale(scale);
            int logMinH = Math.max(88, Math.round(108 * Math.min(scale, 1.06f)));
            this.operationLogPanel.setMinimumSize(new Dimension(0, logMinH));
        }
        this.configureShellGroupTree(scale);
        this.splitPane.setContinuousLayout(true);
        int div = Math.max(5, Math.round(7 * scale));
        this.splitPane.setDividerSize(div);
        this.verticalMainSplit.setContinuousLayout(true);
        this.verticalMainSplit.setDividerSize(div);
        Font statFont = UIManager.getFont("Label.font");
        if (statFont != null) {
            float statSize = Math.max(11f, Math.min(14f, statFont.getSize2D() * Math.min(scale, 1.06f)));
            this.statusLabel.setFont(statFont.deriveFont(Font.PLAIN, statSize));
        }
        int padV = Math.max(4, Math.round(6 * marginFactor));
        int padH = Math.max(8, Math.round(14 * marginFactor));
        this.statusAuroraPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(255, 255, 255, 55)),
                BorderFactory.createEmptyBorder(padV, padH, padV, padH)));
        this.statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        if (this.shellView != null) {
            int rowH = Math.max(26, Math.round(28 * Math.min(scale, 1.06f)));
            this.shellView.setRowHeight(rowH);
        }
        Font menuFont = UIManager.getFont("Menu.font");
        if (menuFont != null && menuBar != null) {
            float menuSize = Math.max(11f, Math.min(14f, menuFont.getSize2D() * Math.min(scale, 1.04f)));
            menuBar.setFont(menuFont.deriveFont(Font.PLAIN, menuSize));
        }
    }

    private void configureShellGroupTree(float scale) {
        if (this.kindList == null) {
            return;
        }
        float tight = Math.min(scale, 1.03f);
        int rowH = Math.max(22, Math.min(28, Math.round(24 * tight)));
        this.kindList.setFixedCellHeight(rowH);
        this.kindList.setOpaque(false);
        this.kindList.setBackground(new Color(255, 255, 255, 0));
        if (this.shellGroupScrollPane != null) {
            this.shellGroupScrollPane.getViewport().setBackground(new Color(255, 255, 255, 0));
            this.shellGroupScrollPane.setBorder(BorderFactory.createEmptyBorder());
        }
    }

    private void applySplitDividerProportions() {
        if (this.splitPane.getWidth() > 0) {
            this.splitPane.setDividerLocation(168);
        }
        if (this.verticalMainSplit.getHeight() > 0) {
            this.verticalMainSplit.setDividerLocation(0.74);
        }
    }

    private void hideShellViewPopupMenu() {
        hideShellViewPopupLater();
    }

    private void installGenerateDialogAutoCloseListener() {
        if (generateDialogAutoCloseInstalled) {
            return;
        }

        generateDialogAutoCloseInstalled = true;
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            public void eventDispatched(AWTEvent event) {
                if (event instanceof WindowEvent) {
                    WindowEvent we = (WindowEvent) event;
                    if (we.getID() == WindowEvent.WINDOW_OPENED) {
                        Window w = we.getWindow();
                        if (w instanceof JDialog) {
                            JDialog d = (JDialog) w;
                            String title = d.getTitle();
                            if (title != null && title.toLowerCase().contains("generateshell")) {
                                MainActivity.this.hookGenerateButtonInComponent(d.getContentPane(), d);
                            }
                        }
                    }
                }

            }
        }, AWTEvent.WINDOW_EVENT_MASK);
    }

    private void updateShellViewPopupMenuState() {
        boolean hasRowSelected = this.shellView != null && this.shellView.getSelectedRow() >= 0;
        if (this.interactMenuItem != null) {
            this.interactMenuItem.setEnabled(hasRowSelected);
            this.interactMenuItem.setVisible(hasRowSelected);
        }
        if (this.interactCacheMenuItem != null) {
            this.interactCacheMenuItem.setEnabled(hasRowSelected);
            this.interactCacheMenuItem.setVisible(hasRowSelected);
        }
        if (this.copyselectItem != null) {
            this.copyselectItem.setEnabled(hasRowSelected);
            this.copyselectItem.setVisible(hasRowSelected);
        }
        if (this.removeShell != null) {
            this.removeShell.setEnabled(hasRowSelected);
            this.removeShell.setVisible(hasRowSelected);
        }
        if (this.editShell != null) {
            this.editShell.setEnabled(hasRowSelected);
            this.editShell.setVisible(hasRowSelected);
        }
        if (this.refreshShell != null) {
            this.refreshShell.setEnabled(true);
            this.refreshShell.setVisible(true);
        }
    }

    private void hookGenerateShellDialogAutoClose() {
        SwingUtilities.invokeLater(() -> {
            java.awt.Window[] windows = java.awt.Window.getWindows();
            for (int i = 0; i < windows.length; ++i) {
                java.awt.Window w = windows[i];
                if (w instanceof JDialog) {
                    JDialog d = (JDialog) w;
                    String title = d.getTitle();
                    if (title != null && title.toLowerCase().contains("generateshell")) {
                        java.awt.Component[] comps = d.getContentPane().getComponents();
                        for (int j = 0; j < comps.length; ++j) {
                            this.hookGenerateButtonInComponent(comps[j], d);
                        }
                    }
                }
            }
        });
    }

    private void hookGenerateButtonInComponent(java.awt.Component c, JDialog dialog) {
        if (c instanceof javax.swing.JButton) {
            javax.swing.JButton b = (javax.swing.JButton) c;
            Object installed = b.getClientProperty("autoCloseInstalled");
            if (installed instanceof Boolean && (Boolean) installed) {
                return;
            }

            String text = b.getText();
            if (text != null && (text.equals("\u751f\u6210") || text.equalsIgnoreCase("generate"))) {
                b.addActionListener((e) -> {
                    SwingUtilities.invokeLater(dialog::dispose);
                });
                b.putClientProperty("autoCloseInstalled", Boolean.TRUE);
            }
        }

        if (c instanceof java.awt.Container) {
            java.awt.Component[] children = ((java.awt.Container) c).getComponents();
            for (int i = 0; i < children.length; ++i) {
                this.hookGenerateButtonInComponent(children[i], dialog);
            }
        }
    }


    private void addShellMenuItemClick(ActionEvent e) {
        this.hideShellViewPopupMenu();
        OperationAuditLog.ui("\u4e3b\u754c\u9762", "\u6dfb\u52a0 Shell", "\u5206\u7ec4: " + this.currentGroup);
        logOperation("", "\u65b0\u589eShell", "\u65b0\u589eShell");
        new BasicShellSetting((String) null, this.currentGroup);
        this.refreshShellView();
    }

    private void addDatabaseShellMenuItemClick(ActionEvent e) {
        logOperation("", "\u65b0\u589e\u6570\u636e\u5e93Shell", "\u65b0\u589e\u6570\u636e\u5e93Shell");
        this.hideShellViewPopupMenu();
        OperationAuditLog.ui("\u4e3b\u754c\u9762", "\u6dfb\u52a0\u6570\u636e\u5e93 Shell", "\u5206\u7ec4: " + this.currentGroup);
        new DatabaseShellSetting((String) null, this.currentGroup);
        this.refreshShellView();
    }

    private void generateShellMenuItemClick(ActionEvent e) {
        logOperation("", "\u751f\u6210Shell", "\u751f\u6210Shell\u8f7d\u8377");
        this.hideShellViewPopupMenu();
        OperationAuditLog.ui("\u4e3b\u754c\u9762", "\u6253\u5f00\u751f\u6210 Shell", "");
        new GenerateShellLoder();
        this.hookGenerateShellDialogAutoClose();
    }

    private void shellLiveScanMenuItemClick(ActionEvent e) {
        logOperation("", "\u75c5\u6bd2\u626b\u63cf", "\u75c5\u6bd2\u626b\u63cf");
        this.hideShellViewPopupMenu();
        OperationAuditLog.ui("\u4e3b\u754c\u9762", "\u5b58\u6d3b\u626b\u63cf", "\u5206\u7ec4: " + this.currentGroup);
        new LiveScan(this.currentGroup);
    }

    private boolean dataSourceDialogBusy;

    private void dataSourceMenuItemClick(ActionEvent e) {
        if (this.dataSourceDialogBusy) {
            return;
        }
        this.dataSourceDialogBusy = true;
        this.hideShellViewPopupMenu();
        StartupModeDialog.DbConfig cfg;
        try {
            cfg = StartupModeDialog.showDialog(this);
        } catch (Throwable t) {
            Log.error(t);
            GOptionPane.showMessageDialog(this, "\u6253\u5f00\u6570\u636e\u6e90\u5931\u8d25: " + t.getMessage(), "\u6570\u636e\u6e90", 2);
            return;
        } finally {
            this.dataSourceDialogBusy = false;
        }
        if (cfg == null) {
            return;
        }
        operatorName = cfg.operatorName;
        if (cfg.isPg) {
            swiitchDb(cfg);
            if (!isRemoteDb) {
                return;
            }
            ensureAllTables();
        } else {
            switchLocalDb();
        }
        this.currentGroup = "/";
        this.currentKind = KIND_HTTP;
        this.refreshShellView();
        if (this.kindList != null) {
            this.kindList.setSelectedIndex(0);
        }
        this.updateTargetIndicator();
        OperationAuditLog.ui("\u4e3b\u754c\u9762", "\u52a0\u8f7d\u6570\u636e\u6e90", cfg.isPg ? cfg.dbPath : "localdb");
    }

    private void pluginConfigMenuItemClick(ActionEvent e) {
        this.hideShellViewPopupMenu();
        OperationAuditLog.ui("\u4e3b\u754c\u9762", "\u63d2\u4ef6\u7ba1\u7406", "");
        new PluginManage();
    }

    private void appConfigMenuItemClick(ActionEvent e) {
        this.hideShellViewPopupMenu();
        OperationAuditLog.ui("\u4e3b\u754c\u9762", "\u5e94\u7528\u8bbe\u7f6e", "");
        new AppSetingDialog();
    }

    private void c2ProfileConfigMenuItemClick(ActionEvent e) {
        this.hideShellViewPopupMenu();
        OperationAuditLog.ui("\u4e3b\u754c\u9762", "C2Profile \u8bbe\u7f6e", "");
        C2ProfileManage.showC2ProfileManage();
    }

    private void showOperationAuditLogMenuItemClick(ActionEvent e) {
        this.hideShellViewPopupMenu();
        OperationAuditLogDialog.showDialog(this);
    }

        private void showTeamOpLogMenuItemClick(ActionEvent e) {
        this.hideShellViewPopupMenu();
        JFrame frame = new JFrame("Team Operation Log");
        frame.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 4, 12));
        JLabel titleLabel = new JLabel("Team Operation Audit Log");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        JLabel statsLabel = new JLabel("");
        statsLabel.setForeground(Color.GRAY);
        JButton refreshBtn = new JButton("Refresh");
        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        topRight.add(statsLabel);
        topRight.add(refreshBtn);
        topPanel.add(titleLabel, BorderLayout.WEST);
        topPanel.add(topRight, BorderLayout.EAST);

        Vector<String> cols = new Vector<>();
        cols.add("Time"); cols.add("Operator"); cols.add("Action"); cols.add("Shell"); cols.add("Detail");
        Vector<Vector<String>> rows = new Vector<>();

        Runnable loadData = () -> {
            rows.clear();
            try {
                java.lang.reflect.Field f = Db.class.getDeclaredField("dbConn");
                f.setAccessible(true);
                java.sql.Connection c = (java.sql.Connection) f.get(null);
                if (c != null) {
                    java.sql.Statement s = c.createStatement();
                    java.sql.ResultSet rs = s.executeQuery("SELECT username, shell_id, action, detail, create_time FROM operation_log ORDER BY create_time DESC");
                    int addCount = 0, delCount = 0, execCount = 0, otherCount = 0;
                    while (rs.next()) {
                        Vector<String> row = new Vector<>();
                        row.add(rs.getString(5));
                        row.add(rs.getString(1));
                        row.add(rs.getString(3));
                        String sid = rs.getString(2);
                        row.add(sid != null && sid.length() > 8 ? sid.substring(0, 8) : (sid != null ? sid : ""));
                        row.add(rs.getString(4));
                        rows.add(row);
                        String action = rs.getString(3);
                        if (action != null) {
                            if (action.contains("\u65b0\u589e") || action.contains("\u65b0\u589e") || action.contains("\u65b0\u589e")) addCount++;
                            else if (action.contains("\u5220\u9664") || action.contains("\u7f16\u8f91")) delCount++;
                            else if (action.contains("\u6267\u884c")) execCount++;
                            else otherCount++;
                        }
                    }
                    s.close();
                    int total = addCount + delCount + execCount + otherCount;
                    statsLabel.setText("\u603b\u8ba1: " + total + " | \u65b0\u589e: " + addCount + " | \u5220\u9664: " + delCount + " | \u6267\u884c: " + execCount + " | \u5176\u4ed6: " + otherCount);
                    if (total == 0) statsLabel.setText("\u65e0\u64cd\u4f5c\u65e5\u5fd7");
                }
            } catch (Exception ex) { statsLabel.setText("Error: " + ex.getMessage()); }
        };
        loadData.run();

        DefaultTableModel model = new DefaultTableModel(rows, cols);
        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                if (v == null) return comp;
                String val = v.toString();
                if (!sel) {
                    if (val.contains("\u65b0\u589e") || val.contains("\u65b0\u589e") || val.contains("\u65b0\u589e")) comp.setBackground(new Color(230, 255, 230));
                    else if (val.contains("\u5220\u9664") || val.contains("\u7f16\u8f91")) comp.setBackground(new Color(255, 230, 230));
                    else if (val.contains("\u6267\u884c")) comp.setBackground(new Color(230, 240, 255));
                    else if (val.contains("\u4ea4\u4e92") || val.contains("\u5176\u4ed6")) comp.setBackground(new Color(255, 250, 230));
                    else comp.setBackground(Color.WHITE);
                }
                return comp;
            }
        });

        JScrollPane sp = new JScrollPane(table);
        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(sp, BorderLayout.CENTER);

        refreshBtn.addActionListener(ev -> { loadData.run(); model.setRowCount(0); for (Vector<String> rw : rows) model.addRow(rw); });

        frame.setSize(960, 550);
        frame.setLocationRelativeTo(this);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setVisible(true);
    }


        private void aboutMenuItemClick(ActionEvent e) {
        this.hideShellViewPopupMenu();
        GOptionPane.showMessageDialog(getFrame(), "\u611f\u8c22\u60a8\u7684\u652f\u6301\uff01", "\u8d5e\u52a9", 1);
    }
        private void checkUpdateMenuItemClick(ActionEvent e) {
        this.hideShellViewPopupMenu();
        checkForUpdate(false);
    }

    private static void checkForUpdate(boolean silentUpToDate) {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("https://api.github.com/repos/Xaaaa-bip/GodzillaSuper/releases/latest");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/vnd.github+json");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.connect();
                int code = conn.getResponseCode();
                if (code != 200) {
                    SwingUtilities.invokeLater(() ->
                        GOptionPane.showMessageDialog(getFrame(), "\u68c0\u67e5\u66f4\u65b0\u5931\u8d25 (HTTP " + code + ")", "\u68c0\u67e5\u66f4\u65b0", 2));
                    return;
                }
                java.io.InputStream is = conn.getInputStream();
                byte[] buf = new byte[8192];
                java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                int n;
                while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
                is.close();
                conn.disconnect();
                String json = new String(bos.toByteArray(), "UTF-8");
                // extract "tag_name"
                String latestTag = extractJsonString(json, "tag_name");
                if (latestTag == null || latestTag.isEmpty()) {
                    SwingUtilities.invokeLater(() ->
                        GOptionPane.showMessageDialog(getFrame(), "\u89e3\u6790\u7248\u672c\u4fe1\u606f\u5931\u8d25", "\u68c0\u67e5\u66f4\u65b0", 2));
                    return;
                }
                String currentTag = core.ApplicationContext.RELEASE_TAG;
                final String latest = latestTag;
                if (currentTag.equals(latestTag)) {
                    if (!silentUpToDate) {
                        SwingUtilities.invokeLater(() ->
                            GOptionPane.showMessageDialog(getFrame(), "\u5df2\u662f\u6700\u65b0\u7248\u672c " + currentTag, "\u68c0\u67e5\u66f4\u65b0", 1));
                    }
                } else {
                    String htmlUrl = extractJsonString(json, "html_url");
                    final String urlStr = (htmlUrl != null && !htmlUrl.isEmpty()) ? htmlUrl
                            : "https://github.com/Xaaaa-bip/GodzillaSuper/releases/tag/" + latestTag;
                    SwingUtilities.invokeLater(() -> {
                        int choice = GOptionPane.showConfirmDialog(getFrame(),
                            "\u53d1\u73b0\u65b0\u7248\u672c " + latest + "\uff01\n\u5f53\u524d\u7248\u672c: " + currentTag + "\n\n\u662f\u5426\u6253\u5f00\u4e0b\u8f7d\u9875\u9762\uff1f",
                            "\u68c0\u67e5\u66f4\u65b0", GOptionPane.OK_CANCEL_OPTION, 1);
                        if (choice == GOptionPane.OK_OPTION) {
                            try {
                                java.awt.Desktop.getDesktop().browse(new java.net.URI(urlStr));
                            } catch (Exception ex) {
                                Log.error(ex);
                            }
                        }
                    });
                }
            } catch (Exception ex) {
                Log.error(ex);
                SwingUtilities.invokeLater(() ->
                    GOptionPane.showMessageDialog(getFrame(), "\u68c0\u67e5\u66f4\u65b0\u5931\u8d25: " + ex.getMessage(), "\u68c0\u67e5\u66f4\u65b0", 2));
            }
        }).start();
    }

    private static String extractJsonString(String json, String key) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }private void copyShellViewSelectedMenuItemClick(ActionEvent e) {
        this.hideShellViewPopupMenu();
        int columnIndex = this.shellView.getSelectedColumn();
        if (columnIndex != -1) {
            Object o = this.shellView.getValueAt(this.shellView.getSelectedRow(), this.shellView.getSelectedColumn());
            if (o != null) {
                String value = (String) o;
                OperationAuditLog.ui("\u4e3b\u754c\u9762", "\u590d\u5236\u8868\u683c\u5355\u5143\u683c", "");
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(value), (ClipboardOwner) null);
                GOptionPane.showMessageDialog(getMainActivityFrame(), "\u590d\u5236\u6210\u529f", "\u63d0\u793a", 1);
            } else {
                GOptionPane.showMessageDialog(getMainActivityFrame(), "\u9009\u4e2d\u5185\u5bb9\u4e3a\u7a7a", "\u63d0\u793a", 2);
            }
        } else {
            GOptionPane.showMessageDialog(getMainActivityFrame(), "\u672a\u9009\u4e2d\u5185\u5bb9", "\u63d0\u793a", 2);
        }

    }

    private void removeShellMenuItemClick(ActionEvent e) {
        this.hideShellViewPopupMenu();
        String[] shellIds = this.getSlectedShellId();
        if (shellIds.length > 0) {
            int n = GOptionPane.showConfirmDialog(getMainActivityFrame(), String.format(EasyI18N.getI18nString("\u786e\u8ba4\u5220\u9664id\u4e3a %s \u7684shell\u5417?"), Arrays.toString((Object[]) shellIds)), "\u786e\u8ba4", 0);
            if (n == 0) {
                OperationAuditLog.ui("\u4e3b\u754c\u9762", "\u5220\u9664 Shell", Arrays.toString(shellIds));
                for (int i = 0; i < shellIds.length; ++i) {
                    String shellId = shellIds[i];
                    String shshellInfo = Db.getOneShell(shellId).toString();
                    logOperation(shellId, "\u5220\u9664Shell", "\u5220\u9664:" + Db.getOneShell(shellId).getUrl());
                    Log.log("removeShell status:%s  -> %s", new Object[]{Db.removeShell(shellId) > 0, shshellInfo});
                }

                GOptionPane.showMessageDialog(getMainActivityFrame(), "\u5220\u9664\u6210\u529f", "\u63d0\u793a", 1);
                this.refreshShellView();
            } else if (n == 1) {
                GOptionPane.showMessageDialog(getMainActivityFrame(), "\u5df2\u53d6\u6d88");
            }
        }

    }

    private String[] getSlectedShellId() {
        int[] rows = this.shellView.getSelectedRows();
        String[] shellIds = new String[rows.length];

        for (int i = 0; i < shellIds.length; ++i) {
            shellIds[i] = (String) this.shellView.getValueAt(rows[i], 0);
        }

        return shellIds;
    }

    private String serializeShellEntity(ShellEntity shell) {
        StringBuilder sb = new StringBuilder();
        sb.append(encodeField(shell.getUrl())).append(FIELD_SEPARATOR);
        sb.append(encodeField(shell.getPassword())).append(FIELD_SEPARATOR);
        sb.append(encodeField(shell.getSecretKey())).append(FIELD_SEPARATOR);
        sb.append(encodeField(shell.getPayload())).append(FIELD_SEPARATOR);
        sb.append(encodeField(shell.getCryption())).append(FIELD_SEPARATOR);
        sb.append(encodeField(shell.getEncoding())).append(FIELD_SEPARATOR);
        sb.append(encodeField(shell.getHeaderS())).append(FIELD_SEPARATOR);
        sb.append(encodeField(shell.getReqLeft())).append(FIELD_SEPARATOR);
        sb.append(encodeField(shell.getReqRight())).append(FIELD_SEPARATOR);
        sb.append(shell.getConnTimeout()).append(FIELD_SEPARATOR);
        sb.append(shell.getReadTimeout()).append(FIELD_SEPARATOR);
        sb.append(encodeField(shell.getProxyType())).append(FIELD_SEPARATOR);
        sb.append(encodeField(shell.getProxyHost())).append(FIELD_SEPARATOR);
        sb.append(shell.getProxyPort()).append(FIELD_SEPARATOR);
        sb.append(encodeField(shell.getRemark())).append(FIELD_SEPARATOR);
        sb.append(encodeField(shell.getC2ProfileName() != null ? shell.getC2ProfileName() : "")).append(FIELD_SEPARATOR);
        sb.append(shell.getMaxErrRetry()).append(FIELD_SEPARATOR);
        sb.append(shell.getOnceBigFileDownloadByteNum()).append(FIELD_SEPARATOR);
        sb.append(shell.getOnceBigFileUploadByteNum()).append(FIELD_SEPARATOR);
        sb.append(shell.getBigFileDownloadThreadNum()).append(FIELD_SEPARATOR);
        sb.append(shell.isMergeResponseCookie()).append(FIELD_SEPARATOR);
        sb.append(encodeField(shell.getClientCertPath())).append(FIELD_SEPARATOR);
        sb.append(encodeField(shell.getClientCertPassword())).append(FIELD_SEPARATOR);
        sb.append(encodeField(shell.getGroup()));
        return sb.toString();
    }

    private static String encodeField(String value) {
        if (value == null) return "";
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    private static String decodeField(String value) {
        if (value == null || value.isEmpty()) return "";
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    private void exportLinksMenuItemClick(ActionEvent e) {
        this.hideShellViewPopupMenu();
        exportSelectedShells();
    }

    private void copyShareLinksMenuItemClick(ActionEvent e) {
        this.hideShellViewPopupMenu();
        exportSelectedShells();
    }

    /** 把当前选中的所有 Shell 打包成一个 gsl5:// 链接放进剪贴板（多选即多条）。 */
    private void exportSelectedShells() {
        String[] shellIds = this.getSlectedShellId();
        if (shellIds.length == 0) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < shellIds.length; ++i) {
            ShellEntity shell = Db.getOneShell(shellIds[i]);
            if (shell != null) {
                sb.append(serializeShellEntity(shell));
                if (i < shellIds.length - 1) {
                    sb.append(RECORD_SEPARATOR);
                }
            }
        }
        if (sb.length() == 0) {
            GOptionPane.showMessageDialog(getMainActivityFrame(), "\u65e0\u53ef\u5bfc\u51fa\u7684Shell\u6570\u636e", "\u63d0\u793a", 2);
            return;
        }
        try {
            byte[] rawBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            GZIPOutputStream gzos = new GZIPOutputStream(bos);
            gzos.write(rawBytes);
            gzos.close();
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(bos.toByteArray());
            String link = GSL_EXPORT_PROTO + encoded;
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new StringSelection(link), (ClipboardOwner) null);
            OperationAuditLog.ui("\u4e3b\u754c\u9762", "\u5bfc\u51faShell\u94fe\u63a5", "\u5df2\u5bfc\u51fa " + shellIds.length + " \u6761Shell");
            GOptionPane.showMessageDialog(getMainActivityFrame(),
                    "\u5df2\u5bfc\u51fa " + shellIds.length + " \u6761Shell\u5230\u526a\u8d34\u677f\n\u94fe\u63a5\u5df2\u590d\u5236\uff0c\u53ef\u53d1\u9001\u7ed9\u4ed6\u4eba\u901a\u8fc7Ctrl+V\u5bfc\u5165", "\u63d0\u793a", 1);
        } catch (Exception e) {
            Log.error(e);
            GOptionPane.showMessageDialog(getMainActivityFrame(), "\u5bfc\u51fa\u5931\u8d25: " + e.getMessage(), "\u9519\u8bef", 2);
        }
    }

    private void importShellsFromClipboard() {
        String clipboardText = null;
        try {
            Transferable trans = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            if (trans == null || !trans.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                return;
            }
            clipboardText = (String) trans.getTransferData(DataFlavor.stringFlavor);
            if (clipboardText == null || !clipboardText.startsWith(GSL_EXPORT_PROTO)) {
                return;
            }
            String encoded = clipboardText.substring(GSL_EXPORT_PROTO.length()).trim();
            if (encoded.isEmpty()) {
                GOptionPane.showMessageDialog(getMainActivityFrame(), "\u5bfc\u5165\u94fe\u63a5\u6570\u636e\u4e3a\u7a7a", "\u63d0\u793a", 2);
                return;
            }
            byte[] compressed = Base64.getUrlDecoder().decode(encoded);
            ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
            GZIPInputStream gzis = new GZIPInputStream(bis);
            byte[] rawBytes = util.functions.readInputStream(gzis);
            gzis.close();
            String data = new String(rawBytes, StandardCharsets.UTF_8);
            String[] records = data.split(RECORD_SEPARATOR, -1);
            int recordCount = 0;
            for (String r : records) {
                if (r != null && !r.trim().isEmpty()) {
                    recordCount++;
                }
            }

            int q = GOptionPane.showConfirmDialog(getMainActivityFrame(),
                    "\u68c0\u6d4b\u5230\u526a\u8d34\u677f\u4e2d\u7684 Shell \u5bfc\u5165\u94fe\u63a5\uff08\u5171 " + recordCount + " \u6761\uff09\uff0c\u662f\u5426\u7acb\u5373\u5bfc\u5165\uff1f", "\u5bfc\u5165Shell", 0);
            if (q != 0) {
                return;
            }
            java.util.ArrayList<String> importUrls = new java.util.ArrayList<>(); int addedCount = 0;
            int skipCount = 0;
            for (String record : records) {
                if (record == null || record.trim().isEmpty()) {
                    continue;
                }
                String[] fields = record.split(FIELD_SEPARATOR, -1);
                if (fields.length < 24) {
                    skipCount++;
                    continue;
                }
                String shellId = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
                ShellEntity shell = new ShellEntity();
                shell.setId(shellId);
                shell.setUrl(decodeField(fields[0]));
                shell.setPassword(decodeField(fields[1]));
                shell.setSecretKey(decodeField(fields[2]));
                shell.setPayload(decodeField(fields[3]));
                shell.setCryption(decodeField(fields[4]));
                String enc = decodeField(fields[5]);
                if (enc != null && !enc.trim().isEmpty()) {
                    shell.setEncoding(enc);
                }
                shell.setHeader(decodeField(fields[6]));
                shell.setReqLeft(decodeField(fields[7]));
                shell.setReqRight(decodeField(fields[8]));
                try {
                    shell.setConnTimeout(Integer.parseInt(fields[9]));
                } catch (Exception e) {
                }
                try {
                    shell.setReadTimeout(Integer.parseInt(fields[10]));
                } catch (Exception e) {
                }
                shell.setProxyType(decodeField(fields[11]));
                shell.setProxyHost(decodeField(fields[12]));
                try {
                    shell.setProxyPort(Integer.parseInt(fields[13]));
                } catch (Exception e) {
                }
                shell.setRemark(decodeField(fields[14]));
                shell.setC2ProfileName(decodeField(fields[15]));
                if (Db.addShell(shell) > 0) {
                    // After addShell, persist env-based settings
                    try {
                        shell.setMaxErrRetry(Integer.parseInt(fields[16]));
                    } catch (Exception e) {
                    }
                    try {
                        shell.setOnceBigFileDownloadByteNum(Integer.parseInt(fields[17]));
                    } catch (Exception e) {
                    }
                    try {
                        shell.setOnceBigFileUploadByteNum(Integer.parseInt(fields[18]));
                    } catch (Exception e) {
                    }
                    try {
                        shell.setBigFileDownloadThreadNum(Integer.parseInt(fields[19]));
                    } catch (Exception e) {
                    }
                    try {
                        shell.setMergeResponseCookie(Boolean.parseBoolean(fields[20]));
                    } catch (Exception e) {
                    }
                    shell.setClientCertPath(decodeField(fields[21]));
                    shell.setClientCertPassword(decodeField(fields[22]));
                    if (fields.length > 23) {
                        String group = decodeField(fields[23]);
                        if (group != null && !group.isEmpty()) {
                            shell.setGroup(group);
                        }
                    }
                    String c2Profile = shell.getC2ProfileName();
                    if (c2Profile != null && !c2Profile.isEmpty()) {
                        shell.setC2ProfileName2(c2Profile);
                    }
                    importUrls.add(decodeField(fields[0])); addedCount++;
                } else {
                    skipCount++;
                }
            }
            OperationAuditLog.ui("\u4e3b\u754c\u9762", "\u5bfc\u5165Shell", "\u6210\u529f: " + addedCount + " \u8df3\u8fc7: " + skipCount);
            logOperation("", "\u65b0\u589eShell", "\u65b0\u589e " + addedCount + " \u6761Shell" + (importUrls.isEmpty() ? "" : " | " + String.join(", ", importUrls.subList(0, Math.min(importUrls.size(), 10))) + (importUrls.size() > 10 ? " ..." : "")));;
            GOptionPane.showMessageDialog(getMainActivityFrame(),
                    "\u5bfc\u5165\u5b8c\u6210! \u6210\u529f: " + addedCount + " \u6761, \u8df3\u8fc7: " + skipCount + " \u6761", "\u63d0\u793a", 1);
            this.refreshShellView();
        } catch (Exception e) {
            // 剪贴板不是 gsl5:// 链接就静默返回；但确实是链接却失败时，必须让用户看到原因
            if (clipboardText != null && clipboardText.startsWith(GSL_EXPORT_PROTO)) {
                Log.error(e);
                GOptionPane.showMessageDialog(getMainActivityFrame(),
                        "导入失败: " + e, "错误", 0);
            }
        }
    }

    private void editShellMenuItemClick(ActionEvent e) {
        this.hideShellViewPopupMenu();
        String[] shellIds = this.getSlectedShellId();
        if (shellIds.length > 0) {
            OperationAuditLog.ui("\u4e3b\u754c\u9762", "\u7f16\u8f91 Shell", Arrays.toString(shellIds));
            for (int i = 0; i < shellIds.length; ++i) {
                String shellId = shellIds[i];
                String shellUrl = Db.getOneShell(shellId).getUrl();
                logOperation(shellId, "\u7f16\u8f91Shell", "\u7f16\u8f91:" + shellUrl);
                if (shellUrl != null && shellUrl.startsWith("jdbc:")) {
                    new DatabaseShellSetting(shellId, this.currentGroup);
                } else {
                    new BasicShellSetting(shellId, this.currentGroup);
                }
            }
        }

    }

    private void interactMenuItemClick(ActionEvent e) {
        this.hideShellViewPopupMenu();
        if (this.shellView.getSelectedRow() < 0) {
            GOptionPane.showMessageDialog(getMainActivityFrame(), "\u8bf7\u5148\u9009\u4e2d\u4e00\u6761\u8bb0\u5f55", "\u63d0\u793a", 2);
            return;
        }

        try {
            String shellId = (String) this.shellView.getValueAt(this.shellView.getSelectedRow(), 0);
            OperationAuditLog.ui("\u4e3b\u754c\u9762", "\u6253\u5f00 Shell \u4ea4\u4e92", shellId);
            logOperation(shellId, "\u7f13\u5b58\u8fdb\u5165\u4ea4\u4e92", "\u4ea4\u4e92:" + Db.getOneShell(shellId).getUrl());
            ShellManage sm = new ShellManage(Db.getOneShell(shellId));
            ShellLinkedWindowChrome.attach(sm);
        } catch (Throwable var4) {
            GOptionPane.showThrowableMessageDialog(getMainActivityFrame(), "\u4ea4\u4e92Shell\u65f6\u53d1\u751f\u5f02\u5e38", var4);
        }

    }

    private void interactCacheMenuItemClick(ActionEvent e) {
        this.hideShellViewPopupMenu();
        if (this.shellView.getSelectedRow() < 0) {
            GOptionPane.showMessageDialog(getMainActivityFrame(), "\u8bf7\u5148\u9009\u4e2d\u4e00\u6761\u8bb0\u5f55", "\u63d0\u793a", 2);
            return;
        }

        String shellId = (String) this.shellView.getValueAt(this.shellView.getSelectedRow(), 0);

        try {
            if ((new File(String.format("%s/%s/cache.db", "GodzillaCache", shellId))).isFile()) {
                ShellEntity shellEntity = Db.getOneShell(shellId);
                OperationAuditLog.ui("\u4e3b\u754c\u9762", "\u7f13\u5b58\u6a21\u5f0f\u8fdb\u5165\u4ea4\u4e92", shellId);
                shellEntity.setUseCache(true);
                ShellManage sm = new ShellManage(shellEntity);
                ShellLinkedWindowChrome.attach(sm);
            } else {
                GOptionPane.showMessageDialog(getMainActivityFrame(), "\u7f13\u5b58\u6587\u4ef6\u4e0d\u5b58\u5728");
            }
        } catch (Throwable var5) {
            GOptionPane.showThrowableMessageDialog((Component) null, "\u8fdb\u5165\u7f13\u5b58Shell\u65f6\u53d1\u751f\u5f02\u5e38", var5);
        }

    }

    private void updateStatusBarText(int tableRows) {
        int totalShells = Db.getAllShell().size() - 1;
        String filterLabel = kindLabel(this.currentKind);
        File homeDb = new File(new File(System.getProperty("user.home")), ".webshell-manager/data.db");
        String dbPath = homeDb.isFile() ? homeDb.getAbsolutePath() : new File("data.db").getAbsolutePath();
        this.statusLabel.setText(String.format(
                "Shell \u603b\u6570: %d | \u5f53\u524d\u8fc7\u6ee4: %s | \u8868\u683c\u884c\u6570: %d | \u672c\u5730\u5e93: %s",
                totalShells, filterLabel, tableRows, dbPath));
    }

    private void updateTargetIndicator() {
        if (!isRemoteDb || remoteDbUrl == null || remoteDbUrl.isEmpty()) {
            this.targetIndicatorLabel.setText("localdb");
            this.targetIndicatorLabel.setForeground(Color.GRAY);
            return;
        }
        try {
            String hostPort = remoteDbUrl;
            // parse jdbc:postgresql://host:port/dbname
            int start = remoteDbUrl.indexOf("://");
            if (start > 0) {
                String rest = remoteDbUrl.substring(start + 3);
                int slash = rest.indexOf('/');
                if (slash > 0) rest = rest.substring(0, slash);
                hostPort = rest;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(hostPort).append(" \u56e2\u961f\u6a21\u5f0f");
            if (operatorName != null && !operatorName.isEmpty()) {
                sb.append(" | \u64cd\u4f5c\u5458: ").append(operatorName);
            }
            this.targetIndicatorLabel.setText(sb.toString());
            this.targetIndicatorLabel.setForeground(Color.RED);
        } catch (Exception ex) {
            this.targetIndicatorLabel.setText("localdb");
            this.targetIndicatorLabel.setForeground(Color.GRAY);
        }
    }

    private void enrichShellRowsWithIpLocation(Vector<Vector<String>> dataRows) {
        IpLocationService.init();
        for (Vector<String> row : dataRows) {
            String url = row.size() > 1 ? row.get(1) : "";
            row.add(IpLocationService.lookupUrl(url));
        }
    }

    public void refreshShellView() {
        Vector<Vector<String>> rowsVector = Db.getAllShell();
        if (rowsVector == null || rowsVector.isEmpty()) {
            rowsVector = new Vector<Vector<String>>();
        } else {
            rowsVector.remove(0);
        }
        rowsVector = filterRowsByKind(rowsVector, this.currentKind);
        this.enrichShellRowsWithIpLocation(rowsVector);
        this.shellView.AddRows(rowsVector);
        this.shellView.getModel().fireTableDataChanged();
        WallpaperTableStyle.applyToShellTable(this.shellView);
        this.refreshKindList();
        this.updateStatusBarText(rowsVector.size());
        if (this.operationLogPanel != null) {
            String ts = new SimpleDateFormat("HH:mm:ss").format(new Date());
            this.operationLogPanel.appendLine(String.format("[%s] \u5df2\u52a0\u8f7d Shell \u5217\u8868: %d", ts, rowsVector.size()));
        }
    }

    private static boolean isDatabaseShellUrl(String url) {
        return url != null && url.regionMatches(true, 0, "jdbc:", 0, 5);
    }

    private static String rowKind(Vector<String> row) {
        String url = row.size() > 1 ? row.get(1) : "";
        if (isDatabaseShellUrl(url)) {
            return KIND_DB;
        }
        return KIND_HTTP;
    }

    private static String kindFromIndex(int idx) {
        if (idx == 1) {
            return KIND_DB;
        }
        return KIND_HTTP;
    }

    private static String kindLabel(String kind) {
        if (KIND_DB.equals(kind)) {
            return "\u6570\u636e\u5e93";
        }
        return "HTTP(S)";
    }

    private static Vector<Vector<String>> filterRowsByKind(Vector<Vector<String>> rows, String kind) {
        Vector<Vector<String>> out = new Vector<Vector<String>>();
        for (int i = 0; i < rows.size(); i++) {
            Vector<String> row = rows.get(i);
            if (kind.equals(rowKind(row))) {
                out.add(row);
            }
        }
        return out;
    }

    private void refreshKindList() {
        if (this.kindModel == null) {
            return;
        }
        int http = 0;
        int db = 0;
        Vector<Vector<String>> all = Db.getAllShell();
        if (all != null && all.size() > 1) {
            for (int i = 1; i < all.size(); i++) {
                if (KIND_DB.equals(rowKind(all.get(i)))) {
                    db++;
                } else {
                    http++;
                }
            }
        }
        String httpLabel = "HTTP(S) (" + http + ")";
        String dbLabel = "\u6570\u636e\u5e93 (" + db + ")";
        if (this.kindModel.size() != 2) {
            this.kindModel.removeAllElements();
            this.kindModel.addElement(httpLabel);
            this.kindModel.addElement(dbLabel);
        } else {
            this.kindModel.set(0, httpLabel);
            this.kindModel.set(1, dbLabel);
        }
    }

    private void refreshShellViewMenuItemClick(ActionEvent e) {
        this.hideShellViewPopupMenu();
        OperationAuditLog.ui("\u4e3b\u754c\u9762", "\u5237\u65b0 Shell \u5217\u8868", "\u5206\u7ec4: " + this.currentGroup);
        this.refreshShellView();
    }

    public MainActivity getJFrame() {
        return this;
    }

    public static MainActivity getFrame() {
        return mainActivityFrame;
    }

    public static JMenuItem registerPluginJMenuItem(JMenuItem menuItem) {
        SvgIcons.decorate(menuItem);
        return pluginMenu.add(menuItem);
    }

    private void registerMcpMenuItem() {
        try {
            JMenuItem mcpItem = new JMenuItem("MCP \u670d\u52a1");
            SvgIcons.apply(mcpItem, "mcp");
            mcpItem.addActionListener(e -> {
                try {
                    Class<?> mcpClass = Class.forName("shells.plugins.generic.McpService");
                    Object mcp = mcpClass.getDeclaredConstructor().newInstance();
                    java.awt.Container view = (java.awt.Container) mcpClass.getMethod("getView").invoke(mcp);
                    JFrame frame = new JFrame("MCP \u670d\u52a1\u63a7\u5236\u9762\u677f");
                    frame.setContentPane(view);
                    frame.setSize(780, 520);
                    frame.setMinimumSize(new java.awt.Dimension(640, 420));
                    frame.setLocationRelativeTo(MainActivity.this);
                    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    frame.setVisible(true);
                } catch (Exception ex) {
                    Log.error(ex);
                    try {
                        javax.swing.JOptionPane.showMessageDialog(MainActivity.this,
                                "MCP \u9762\u677f\u6253\u5f00\u5931\u8d25: " + ex.getMessage(),
                                "MCP", javax.swing.JOptionPane.ERROR_MESSAGE);
                    } catch (Exception ignored2) {}
                }
            });
            if (this.configMenu != null) {
                this.configMenu.add(mcpItem);
            } else {
                pluginMenu.add(mcpItem);
            }
        } catch (Exception ex) {
            Log.error(ex);
        }
    }
    public static void registerPluginPopMenu(PopupMenu popupMenu) {
        pluginMenu.add(popupMenu);
    }

    public static JMenu registerJMenu(JMenu menu) {
        SvgIcons.decorate(menu);
        if (menuBarAboutMenu != null) {
            insertMenuBeforeAbout(menu);
            return menu;
        }
        synchronized (menusPendingForMenuBar) {
            menusPendingForMenuBar.add(menu);
        }
        return menu;
    }

    public static void setPluginMenuFont(Font font) {
        if (pluginMenu != null) {
            pluginMenu.setFont(font);
            MenuElement[] menuElements = pluginMenu.getSubElements();
            MenuElement[] var2 = menuElements;
            int var3 = menuElements.length;

            for (int var4 = 0; var4 < var3; ++var4) {
                MenuElement menuElement = var2[var4];
                setSubMenuFont(menuElement, font);
            }
        }

    }

    private static void setSubMenuFont(MenuElement menuElement, Font font) {
        MenuElement[] menuElements = menuElement.getSubElements();
        MenuElement[] var3 = menuElements;
        int var4 = menuElements.length;

        for (int var5 = 0; var5 < var4; ++var5) {
            MenuElement element = var3[var5];
            if (element instanceof JComponent) {
                ((JComponent) element).setFont(font);
            }

            setSubMenuFont(element, font);
        }

    }

    public static JMenuItem registerShellViewJMenuItem(JMenuItem menuItem) {
        SvgIcons.decorate(menuItem);
        return shellViewPopupMenu.add(menuItem);
    }

    public static void registerShellViewPopupMenu(PopupMenu popupMenu) {
        shellViewPopupMenu.add(popupMenu);
    }

    public static MainActivity getMainActivityFrame() {
        return mainActivityFrame;
    }

    public static void initUi() {
        core.ui.component.dialog.SafeFileSystemView.install();
        if (SystemInfo.isMacOS && System.getProperty("apple.laf.useScreenMenuBar") == null) {
            System.setProperty("apple.laf.useScreenMenuBar", "false");
        }

        ModernUi.installBeforeTheme();
        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);
        String resourceNameString = Db.getSetingValue("ui-resourceName");
        String lafClassNameString = Db.getSetingValue("ui-lafClassName");
        if (resourceNameString == null && lafClassNameString == null) {
            Db.updateSetingKV("ui-lafClassName", "com.formdev.flatlaf.FlatIntelliJLaf");
        }

        lafClassNameString = Db.getSetingValue("ui-lafClassName");
        IJThemesPanel.setTheme(new IJThemeInfo(resourceNameString, lafClassNameString));
    }

    public static void main(String[] args) {
        if (args.length >= 1 && "mcp".equals(args[0])) {
            // Prefer java -jar (core.Gsl5Main): that class is not a JFrame, so Linux without DISPLAY can start.
            core.Gsl5Main.main(args);
            return;
        }
        try {
            initUi();
            operatorName = System.getProperty("user.name", "operator");
            isRemoteDb = false;
            remoteDbUrl = "";
            initStatic();
            Class.forName("core.ApplicationContext", true, Thread.currentThread().getContextClassLoader());
        } catch (Exception e) { Log.error(e); }
        GodzillaObjectInputFilter.installObjectInputFilter();
        MainActivity activity = new MainActivity();
        mainActivityFrame = activity.getJFrame();
    }


    private static void ensureDefaultData() {
        try {
            java.lang.reflect.Field f = Db.class.getDeclaredField("dbConn");
            f.setAccessible(true);
            java.sql.Connection c = (java.sql.Connection) f.get(null);
            if (c == null) return;
            // Check if shell table is empty (fresh database)
            java.sql.Statement s = c.createStatement();
            java.sql.ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM shell");
            rs.next();
            int count = rs.getInt(1);
            rs.close(); s.close();
            if (count > 0) return;
            // Insert default demo shell
            String id = java.util.UUID.randomUUID().toString();
            String now = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            java.sql.PreparedStatement ps = c.prepareStatement(
                "INSERT INTO shell(id,url,password,secretKey,payload,cryption,encoding,headers,reqLeft,reqRight,connTimeout,readTimeout,proxyType,proxyHost,proxyPort,remark,note,createTime,updateTime) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            ps.setString(1, id);
            ps.setString(2, "http://127.0.0.1:8080/1.jsp");
            ps.setString(3, "pass");
            ps.setString(4, "key");
            ps.setString(5, "JavaDynamicPayload");
            ps.setString(6, "JavaAesBase64");
            ps.setString(7, "UTF-8");
            ps.setString(8, "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:84.0) Gecko/20100101 Firefox/84.0\nAccept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\nAccept-Language: zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
            ps.setString(9, "");
            ps.setString(10, "");
            ps.setInt(11, 60000);
            ps.setInt(12, 60000);
            ps.setString(13, "NO_PROXY");
            ps.setString(14, "");
            ps.setString(15, "");
            ps.setString(16, "Demo Shell");
            ps.setString(17, "");
            ps.setString(18, now);
            ps.setString(19, now);
            ps.executeUpdate(); ps.close();
            Log.log("Default demo shell created: http://127.0.0.1:8080/1.jsp");
        } catch (Exception ignored) {}
    }
    static String operatorName;
    public static boolean isRemoteDb = false;
    static String remoteDbUrl = "";

    private static void swiitchDb(StartupModeDialog.DbConfig cfg) {
        try {
            java.lang.reflect.Field f = Db.class.getDeclaredField("dbConn");
            f.setAccessible(true);
            java.sql.Connection old = (java.sql.Connection) f.get(null);
            java.sql.Connection c;
            if (cfg.isPg) {
                Class.forName("org.postgresql.Driver");
                c = java.sql.DriverManager.getConnection(cfg.dbPath, cfg.username, cfg.password);
            } else {
                c = java.sql.DriverManager.getConnection("jdbc:sqlite:" + cfg.dbPath);
                try (java.sql.Statement s = c.createStatement()) { s.execute("PRAGMA journal_mode=WAL"); } catch (Exception ignored) {}
            }
            isRemoteDb = true;
            remoteDbUrl = cfg.dbPath;
            f.set(null, c);
            if (old != null && !old.isClosed()) { try { old.close(); } catch (Exception ex) {} }
            Log.log("Switched Db to: " + cfg.dbPath);
        } catch (Exception e) {
            Log.error(e);
            javax.swing.JOptionPane.showMessageDialog(null, "DB connect failed: " + e.getMessage());
        }
    }

    private static void ensureAllTables() {
        try {
            java.lang.reflect.Field f = Db.class.getDeclaredField("dbConn");
            f.setAccessible(true);
            java.sql.Connection c = (java.sql.Connection) f.get(null);
            if (c == null) return;
            try (java.sql.Statement s = c.createStatement()) {
                String idType = "TEXT";
                String intType = "INTEGER";
                String autoIncr = isRemoteDb ? "GENERATED BY DEFAULT AS IDENTITY" : "AUTOINCREMENT";
                s.execute("CREATE TABLE IF NOT EXISTS shell (id " + idType + " PRIMARY KEY, url TEXT NOT NULL, password TEXT NOT NULL, secretKey TEXT NOT NULL, payload TEXT NOT NULL, cryption TEXT NOT NULL, encoding TEXT NOT NULL, headers TEXT, reqLeft TEXT, reqRight TEXT, connTimeout " + intType + " DEFAULT 60000, readTimeout " + intType + " DEFAULT 60000, proxyType TEXT, proxyHost TEXT, proxyPort TEXT, remark TEXT, note TEXT, createTime TEXT, updateTime TEXT)");
                s.execute("CREATE TABLE IF NOT EXISTS shellEnv (shellId VARCHAR(64) NOT NULL, key VARCHAR(256) NOT NULL, value TEXT, PRIMARY KEY (shellId, key))");
                s.execute("CREATE TABLE IF NOT EXISTS seting (key VARCHAR(256) PRIMARY KEY, value TEXT)");
                s.execute("CREATE TABLE IF NOT EXISTS plugin (pluginJarFile VARCHAR(512) PRIMARY KEY)");
                s.execute("CREATE TABLE IF NOT EXISTS shellGroup (groupId VARCHAR(256) PRIMARY KEY)");
                s.execute("CREATE TABLE IF NOT EXISTS operation_log (id " + intType + " " + autoIncr + " PRIMARY KEY, username VARCHAR(128) NOT NULL, shell_id VARCHAR(64), action VARCHAR(128) NOT NULL, detail TEXT, create_time TEXT NOT NULL)");
            }
        } catch (Exception ignored) {}
    }

    public static void logOperation(String shellId, String action, String detail) {
        if (!isRemoteDb || operatorName == null) return;
        try {
            java.lang.reflect.Field f = Db.class.getDeclaredField("dbConn");
            f.setAccessible(true);
            java.sql.Connection c = (java.sql.Connection) f.get(null);
            if (c == null) return;
            String timeFn = isRemoteDb ? "NOW()" : "datetime('now')";
            String sql = "INSERT INTO operation_log(username,shell_id,action,detail,create_time) VALUES(?,?,?,?," + timeFn + ")";
            java.sql.PreparedStatement ps = c.prepareStatement(sql);
            ps.setString(1, operatorName);
            ps.setString(2, shellId != null ? shellId : "");
            ps.setString(3, action);
            ps.setString(4, detail != null ? detail : "");
            ps.executeUpdate();
            ps.close();
        } catch (Exception ignored) {}
    }
}
