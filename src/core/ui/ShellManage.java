package core.ui;

import core.ApplicationContext;
import core.EasyI18N;
import core.annotation.DisplayName;
import core.annotation.PluginAnnotation;
import core.c2profile.C2Profile;
import core.c2profile.c2annotation.C2ProfilePluginConfig;
import core.imp.Payload;
import core.imp.Plugin;
import core.shell.ShellEntity;
import core.ui.SvgIcons;
import core.ui.component.PostExPluginHub;
import core.ui.component.RTabbedPane;
import core.ui.component.ShellBasicsInfo;
import core.ui.component.ShellCopyTab;
import core.ui.component.ShellDatabasePanel;
import core.ui.component.ShellExecCommandPanel;
import core.ui.component.ShellFileManager;
import core.ui.component.ShellNetstat;
import core.ui.component.ShellNote;
import core.ui.component.dialog.GOptionPane;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import shells.channel.RequestChannel;
import util.Log;
import util.functions;

/**
 * Shell interact window. Loading label must be swapped for tabs on the EDT;
 * wallpaper chrome may wrap contentPane asynchronously \u2014 never use bare
 * {@code remove(loadLabel)/add(tabbedPane)} off the EDT against that race.
 */
public class ShellManage extends JFrame {
    private JTabbedPane tabbedPane;
    private ShellEntity shellEntity;
    private ShellExecCommandPanel shellExecCommandPanel;
    private ShellBasicsInfo shellBasicsInfo;
    private ShellFileManager shellFileManager;
    private ShellDatabasePanel shellDatabasePanel;
    private C2Profile profile;
    private LinkedHashMap<String, Plugin> pluginMap = new LinkedHashMap();
    private LinkedHashMap<String, JPanel> globalComponent = new LinkedHashMap();
    private ArrayList<JPanel> allViews = new ArrayList();
    private Payload payload;
    private ShellCopyTab shellCopyTab;
    private JLabel loadLabel = new JLabel("loading......", 0);
    private C2Profile c2Profile;
    private volatile boolean uiReady = false;
    private static final HashMap<String, String> CN_HASH_MAP = new HashMap();

    public ShellManage(ShellEntity shellEntity) {
        this.shellEntity = shellEntity;
        this.tabbedPane = new RTabbedPane();
        String titleString = String.format(
                "Url:%s Payload:%s Cryption:%s openCache:%s useCache:%s",
                this.shellEntity.getUrl(),
                this.shellEntity.getPayload(),
                this.shellEntity.getCryption(),
                shellEntity.isUseCache() ? false : ApplicationContext.isOpenCache(),
                shellEntity.isUseCache());
        this.setTitle(titleString);
        this.add(this.loadLabel);
        functions.setWindowSize((Window) this, (int) 1700, (int) 680);
        this.setLocationRelativeTo((Component) MainActivity.getFrame());
        this.setVisible(true);
        this.setDefaultCloseOperation(2);
        new Thread(() -> {
            this.safeSetLoadLabel(EasyI18N.getI18nString((String) "\u6b63\u5728\u8fde\u63a5\u5230\u76ee\u6807....."));
            try {
                boolean state = this.shellEntity.initShellOpertion();
                if (state) {
                    this.safeSetLoadLabel(EasyI18N.getI18nString((String) "\u6b63\u5728\u52a0\u8f7dShellManage\u4e0a\u4e0b\u6587\u5c5e\u6027....."));
                    this.profile = shellEntity.getCurrentProfile();
                    this.shellEntity.setFrame(this);
                    this.payload = shellEntity.getPayloadModule();
                    this.c2Profile = shellEntity.getCurrentProfile();
                    this.safeSetLoadLabel(EasyI18N.getI18nString((String) "\u6b63\u5728\u52a0\u8f7d\u57fa\u7840\u56fe\u5f62\u7ec4\u4ef6....."));
                    this.loadGlobalComponent();
                    if (!shellEntity.isUseCache()) {
                        this.safeSetLoadLabel(EasyI18N.getI18nString((String) "\u6b63\u5728\u52a0\u8f7d\u63d2\u4ef6....."));
                        this.loadPlugins();
                    }
                    this.safeSetLoadLabel(EasyI18N.getI18nString((String) "\u6b63\u5728\u52a0\u8f7d\u53ef\u89c6\u5316\u9875\u9762....."));
                    this.loadView();
                    this.shellCopyTab.scan();
                    this.showMainUiOnEdt();
                    if (this.profile != null && this.profile.coreConfig.enabledHeartbeat && !shellEntity.isUseCache()) {
                        new Thread(() -> {
                            long heartbeatSleepTime = this.profile.coreConfig.heartbeatSleepTime;
                            int heartbeatJitterTime = (int) ((double) heartbeatSleepTime
                                    * Double.parseDouble("0." + this.profile.coreConfig.heartbeatJitter));
                            try {
                                while (this.payload.isAlive()) {
                                    long currentTime = System.currentTimeMillis();
                                    long _time = currentTime - this.payload.lastSendTime();
                                    if (_time > heartbeatSleepTime && !this.payload.test()) {
                                        Log.error((String) "\u5fc3\u8df3\u5305\u53d1\u9001\u5931\u8d25! \u5fc3\u8df3\u5305\u7ebf\u7a0b\u5df2\u9000\u51fa");
                                        return;
                                    }
                                    long realSleepTime = functions.randomInt((int) 0, (int) heartbeatJitterTime);
                                    Thread.sleep(heartbeatSleepTime + realSleepTime);
                                }
                            } catch (Throwable e) {
                                Log.log((String) "\u5fc3\u8df3\u7ebf\u7a0b\u53d1\u9001\u5f02\u5e38\u5df2\u9000\u51fa msg:%s",
                                        (Object[]) new Object[]{e.getMessage()});
                            }
                        }).start();
                    }
                } else if (this.isShowing()) {
                    GOptionPane.showMessageDialog(null, (Object) "\u521d\u59cb\u5316\u5931\u8d25", (String) "\u63d0\u793a", (int) 2);
                    this.dispose();
                }
            } catch (Throwable e) {
                if (this.isShowing()) {
                    GOptionPane.showThrowableMessageDialog(null,
                            (String) "\u521d\u59cb\u5316Shell\u56fe\u5f62\u5316\u754c\u9762\u65f6\u53d1\u751f\u9519\u8bef", (Throwable) e);
                    this.dispose();
                }
            }
        }, "ShellManage-init").start();
    }

    private void safeSetLoadLabel(String text) {
        try {
            SwingUtilities.invokeAndWait(() -> this.loadLabel.setText(text));
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * Swap loading label for tabbed UI on the EDT, under whatever content root
     * wallpaper chrome currently uses (frame contentPane or WallpaperLayerPanel child).
     */
    private void showMainUiOnEdt() {
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    Container host = resolveUiHost();
                    if (this.loadLabel.getParent() != null) {
                        this.loadLabel.getParent().remove(this.loadLabel);
                    }
                    // also strip any leftover loading labels under host (race survivors)
                    stripLoadingLabels(host);
                    if (this.tabbedPane.getParent() != host) {
                        if (this.tabbedPane.getParent() != null) {
                            this.tabbedPane.getParent().remove(this.tabbedPane);
                        }
                        host.add(this.tabbedPane);
                    }
                    this.loadLabel.setVisible(false);
                    this.uiReady = true;
                    host.revalidate();
                    host.repaint();
                    this.invalidate();
                    this.repaint();
                    this.setVisible(true);
                } catch (Throwable t) {
                    Log.error(t);
                }
            });
        } catch (Throwable e) {
            Log.error(e);
        }
    }

    /** Prefer wallpaper content child if chrome already wrapped the frame. */
    private Container resolveUiHost() {
        Container cp = this.getContentPane();
        if (cp instanceof core.ui.component.WallpaperLayerPanel) {
            for (Component c : cp.getComponents()) {
                if (c instanceof Container) {
                    return (Container) c;
                }
            }
        }
        return cp;
    }

    private static void stripLoadingLabels(Container c) {
        if (c == null) {
            return;
        }
        Component[] kids = c.getComponents();
        for (Component ch : kids) {
            if (ch instanceof JLabel) {
                String t = ((JLabel) ch).getText();
                if (t != null && (t.contains("\u52a0\u8f7d\u53ef\u89c6\u5316") || t.startsWith("loading")
                        || t.contains("\u6b63\u5728\u52a0\u8f7d") || t.contains("\u6b63\u5728\u8fde\u63a5"))) {
                    c.remove(ch);
                }
            } else if (ch instanceof Container) {
                stripLoadingLabels((Container) ch);
            }
        }
    }

    /** Used by chrome to know loading finished (avoid hiding real UI). */
    public boolean isUiReady() {
        return this.uiReady;
    }

    private void loadView() {
        this.allViews.addAll(this.globalComponent.values());
        String[] coreOrder = new String[]{
            "BasicsInfo", "ExecCommand", "FileManage", "DatabaseManage", "Netstat", "Note"
        };
        for (int i = 0; i < coreOrder.length; i++) {
            addCoreTab(coreOrder[i]);
        }
        LinkedHashMap<String, Plugin> hubPlugins = new LinkedHashMap<String, Plugin>();
        for (String key : this.pluginMap.keySet()) {
            Plugin plugin = this.pluginMap.get(key);
            if (plugin == null) {
                continue;
            }
            JPanel panel = plugin.getView();
            if (panel == null) {
                continue;
            }
            EasyI18N.installObject((Object) plugin);
            EasyI18N.installObject((Object) panel);
            this.allViews.add(panel);
            if (isSuperTerminalPlugin(plugin)) {
                this.tabbedPane.addTab("\u8d85\u7ea7\u7ec8\u7aef", SvgIcons.of("terminal"), panel);
            } else {
                hubPlugins.put(key, plugin);
            }
        }
        if (!hubPlugins.isEmpty()) {
            PostExPluginHub hub = new PostExPluginHub(hubPlugins);
            this.allViews.add(hub);
            this.tabbedPane.addTab("\u540e\u6e17\u900f", SvgIcons.of("postex"), hub);
        }
        addCoreTab("CopyTab");
    }

    private void addCoreTab(String key) {
        JPanel panel = this.globalComponent.get(key);
        if (panel == null) {
            return;
        }
        EasyI18N.installObject((Object) panel);
        this.tabbedPane.addTab(coreTabTitle(key, panel), SvgIcons.of(SvgIcons.coreTabKey(key)), panel);
    }

    private static String coreTabTitle(String key, JPanel panel) {
        if ("BasicsInfo".equals(key)) {
            return "\u57fa\u7840\u4fe1\u606f";
        }
        if ("ExecCommand".equals(key)) {
            return "\u547d\u4ee4\u6267\u884c";
        }
        if ("FileManage".equals(key)) {
            return "\u6587\u4ef6\u7ba1\u7406";
        }
        if ("DatabaseManage".equals(key)) {
            return "\u6570\u636e\u5e93";
        }
        if ("Netstat".equals(key)) {
            return "\u7f51\u7edc\u8fde\u63a5";
        }
        if ("Note".equals(key)) {
            return "\u7b14\u8bb0";
        }
        if ("CopyTab".equals(key)) {
            return "\u6807\u7b7e";
        }
        DisplayName displayName = panel.getClass().getAnnotation(DisplayName.class);
        if (displayName != null) {
            return EasyI18N.getI18nString((String) displayName.DisplayName());
        }
        return panel.getClass().getSimpleName();
    }

    public static String getCNName(String name) {
        for (String key : CN_HASH_MAP.keySet()) {
            if (!key.toUpperCase().equals(name.toUpperCase())) {
                continue;
            }
            return CN_HASH_MAP.get(key);
        }
        return name;
    }

    private void loadGlobalComponent() {
        this.shellCopyTab = new ShellCopyTab(this.shellEntity);
        this.shellBasicsInfo = new ShellBasicsInfo(this.shellEntity);
        this.globalComponent.put("BasicsInfo", (JPanel) this.shellBasicsInfo);
        this.shellExecCommandPanel = new ShellExecCommandPanel(this.shellEntity);
        this.globalComponent.put("ExecCommand", (JPanel) this.shellExecCommandPanel);
        this.shellFileManager = new ShellFileManager(this.shellEntity);
        this.globalComponent.put("FileManage", (JPanel) this.shellFileManager);
        this.shellDatabasePanel = new ShellDatabasePanel(this.shellEntity);
        this.globalComponent.put("DatabaseManage", (JPanel) this.shellDatabasePanel);
        this.globalComponent.put("Note", (JPanel) new ShellNote(this.shellEntity));
        this.globalComponent.put("Netstat", (JPanel) new ShellNetstat(this.shellEntity));
        this.globalComponent.put("CopyTab", (JPanel) this.shellCopyTab);
    }

    private static boolean isSuperTerminalPlugin(Plugin p) {
        PluginAnnotation ann = p.getClass().getAnnotation(PluginAnnotation.class);
        if (ann != null && "SuperTerminal".equalsIgnoreCase(ann.Name())) {
            return true;
        }
        return p.getClass().getSimpleName().toLowerCase().contains("superterminal");
    }

    private String getPluginName(Plugin p) {
        PluginAnnotation pluginAnnotation = p.getClass().getAnnotation(PluginAnnotation.class);
        if (pluginAnnotation != null && pluginAnnotation.Name() != null && pluginAnnotation.Name().trim().length() > 0) {
            return pluginAnnotation.Name();
        }
        return p.getClass().getSimpleName();
    }

    public Plugin createPlugin(String pluginName) {
        try {
            Plugin plugin = this.pluginMap.get(pluginName);
            if (plugin != null) {
                plugin = (Plugin) plugin.getClass().newInstance();
                plugin.init(this.shellEntity);
                plugin.getView();
                return plugin;
            }
        } catch (Exception e) {
            Log.error((Throwable) e);
        }
        return null;
    }

    public ShellFileManager getShellFileManager() {
        return this.shellFileManager;
    }

    private void loadPlugins() {
        Plugin plugin;
        int i;
        Plugin[] plugins = ApplicationContext.getAllPlugin((String) this.shellEntity.getPayload());
        for (i = 0; i < plugins.length; ++i) {
            try {
                plugin = plugins[i];
                this.loadPluginConfig(plugin);
                this.pluginMap.put(this.getPluginName(plugin), plugin);
            } catch (Exception e) {
                Log.error((Throwable) e);
            }
        }
        for (i = 0; i < plugins.length; ++i) {
            try {
                plugin = plugins[i];
                plugin.init(this.shellEntity);
            } catch (Exception e) {
                Log.error((Throwable) e);
            }
        }
    }

    private void loadPluginConfig(Plugin plugin) {
        Class<?> pluginClass = plugin.getClass();
        PluginAnnotation pluginAnnotation = pluginClass.getAnnotation(PluginAnnotation.class);
        if (pluginAnnotation == null) {
            return;
        }
        try {
            Map configs;
            Map pluginConfigs;
            if (this.c2Profile != null && this.c2Profile.pluginConfigs != null
                    && (pluginConfigs = (Map) this.c2Profile.pluginConfigs.get(this.shellEntity.getPayload())) != null
                    && (configs = (Map) pluginConfigs.get(pluginAnnotation.Name())) != null) {
                for (Object propertyNameObj : configs.keySet()) {
                    String propertyName = String.valueOf(propertyNameObj);
                    Field field = functions.getField(pluginClass, (String) propertyName);
                    if (field != null) {
                        if (field.getAnnotation(C2ProfilePluginConfig.class) != null) {
                            field.setAccessible(true);
                            field.set(plugin, configs.get(propertyNameObj));
                            continue;
                        }
                        throw new IllegalStateException(propertyName);
                    }
                    throw new NoSuchElementException(propertyName);
                }
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public Plugin getPlugin(String pluginName) {
        return this.pluginMap.get(pluginName);
    }

    public Object getBasicComponent(String componentName) {
        return this.globalComponent.get(componentName);
    }

    @Override
    public void dispose() {
        super.dispose();
        try {
            this.tabbedPane.disable();
            for (JPanel jPanel : this.allViews) {
                if (!jPanel.isEnabled()) {
                    continue;
                }
                jPanel.disable();
            }
        } catch (Exception e) {
            Log.error((Throwable) e);
        }
        new Thread(this::close).start();
        super.dispose();
        System.gc();
    }

    public void close() {
        RequestChannel requestChannel;
        this.pluginMap.keySet().forEach(key -> {
            Plugin plugin = this.pluginMap.get(key);
            try {
                plugin.close();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
        if (this.payload != null && ApplicationContext.isOpenC((String) "isAutoCloseShell")) {
            try {
                Log.log((String) String.format("CloseShellState: %s\tShellId: %s\tShellHash: %s",
                        this.shellEntity.getPayloadModule().close(), this.shellEntity.getId(), this.shellEntity.hashCode()));
            } catch (Exception e) {
                Log.error((Throwable) e);
            }
        }
        if ((requestChannel = this.shellEntity.getRequest()) != null) {
            this.shellEntity.getRequest().close();
        }
        this.globalComponent.keySet().forEach(key -> {
            JPanel plugin = this.globalComponent.get(key);
            try {
                Method method = functions.getMethodByClass(plugin.getClass(), (String) "closePlugin", (Class[]) null);
                if (method != null) {
                    method.invoke(plugin, (Object[]) null);
                }
            } catch (Exception e) {
                Log.error((Throwable) e);
            }
        });
        this.pluginMap.clear();
        this.globalComponent.clear();
    }

    public LinkedHashMap<String, Plugin> getPluginMap() {
        return this.pluginMap;
    }

    public LinkedHashMap<String, JPanel> getGlobalComponent() {
        return this.globalComponent;
    }

    public JTabbedPane getTabbedPane() {
        return this.tabbedPane;
    }

    static {
        CN_HASH_MAP.put("payload", "\u6709\u6548\u8f7d\u8377");
        CN_HASH_MAP.put("secretKey", "\u5bc6\u94a5");
        CN_HASH_MAP.put("password", "\u5bc6\u7801");
        CN_HASH_MAP.put("c2profile", "C2Profile");
        CN_HASH_MAP.put("cryption", "\u52a0\u5bc6\u5668");
        CN_HASH_MAP.put("PROXYHOST", "\u4ee3\u7406\u4e3b\u673a");
        CN_HASH_MAP.put("PROXYPORT", "\u4ee3\u7406\u7aef\u53e3");
        CN_HASH_MAP.put("CONNTIMEOUT", "\u8fde\u63a5\u8d85\u65f6");
        CN_HASH_MAP.put("READTIMEOUT", "\u8bfb\u53d6\u8d85\u65f6");
        CN_HASH_MAP.put("PROXY", "\u4ee3\u7406\u7c7b\u578b");
        CN_HASH_MAP.put("REMARK", "\u5907\u6ce8");
        CN_HASH_MAP.put("ENCODING", "\u7f16\u7801");
    }
}
