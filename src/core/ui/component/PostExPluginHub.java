package core.ui.component;

import core.annotation.PluginAnnotation;
import core.imp.Plugin;
import core.ui.SvgIcons;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * Groups post-ex plugins into a left tree so the main shell window
 * does not grow one tab per plugin.
 */
public class PostExPluginHub extends JPanel {

    private static final String[] CATEGORY_ORDER = {
            "\u7ec8\u7aef",
            "\u6743\u9650\u63d0\u5347",
            "\u51ed\u8bc1\u8d26\u53f7",
            "\u5185\u5b58\u6301\u4e45\u5316",
            "\u7f51\u7edc\u4ee3\u7406",
            "\u4ee3\u7801\u6267\u884c",
            "\u5e94\u7528\u5229\u7528",
            "\u4fa6\u5bdf\u8bc6\u522b",
            "\u5176\u5b83"
    };

    private static final class CatNode {
        final String title;
        final String iconKey;

        CatNode(String title, String iconKey) {
            this.title = title;
            this.iconKey = iconKey;
        }

        @Override
        public String toString() {
            return this.title;
        }
    }

    private static final class PluginLeaf {
        final String title;
        final String cardKey;
        final String iconKey;

        PluginLeaf(String title, String cardKey, String iconKey) {
            this.title = title;
            this.cardKey = cardKey;
            this.iconKey = iconKey;
        }

        @Override
        public String toString() {
            return this.title;
        }
    }

    public PostExPluginHub(Map<String, Plugin> plugins) {
        super(new BorderLayout());
        JPanel cards = new JPanel(new CardLayout());
        JLabel empty = new JLabel("\u5de6\u4fa7\u9009\u62e9\u63d2\u4ef6", JLabel.CENTER);
        cards.add(empty, "_empty");

        LinkedHashMap<String, DefaultMutableTreeNode> cats = new LinkedHashMap<String, DefaultMutableTreeNode>();
        for (int i = 0; i < CATEGORY_ORDER.length; i++) {
            String cat = CATEGORY_ORDER[i];
            cats.put(cat, new DefaultMutableTreeNode(new CatNode(cat, SvgIcons.categoryKey(cat))));
        }

        if (plugins != null) {
            for (Map.Entry<String, Plugin> e : plugins.entrySet()) {
                Plugin plugin = e.getValue();
                if (plugin == null) {
                    continue;
                }
                JPanel view = plugin.getView();
                if (view == null) {
                    continue;
                }
                PluginAnnotation ann = plugin.getClass().getAnnotation(PluginAnnotation.class);
                String title = ann != null ? ann.DisplayName() : e.getKey();
                String name = ann != null ? ann.Name() : e.getKey();
                String cat = categoryOf(name, title);
                String cardKey = e.getKey();
                cards.add(view, cardKey);
                cats.get(cat).add(new DefaultMutableTreeNode(
                        new PluginLeaf(title, cardKey, SvgIcons.pluginKey(name, title, cat))));
            }
        }

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("\u540e\u6e17\u900f");
        for (DefaultMutableTreeNode cat : cats.values()) {
            if (cat.getChildCount() > 0) {
                root.add(cat);
            }
        }

        JTree tree = new JTree(new DefaultTreeModel(root));
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setRowHeight(22);
        tree.setCellRenderer(new PluginTreeRenderer());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        expandAll(tree, new TreePath(root));
        tree.addTreeSelectionListener((TreeSelectionEvent ev) -> {
            Object last = tree.getLastSelectedPathComponent();
            if (!(last instanceof DefaultMutableTreeNode)) {
                return;
            }
            Object u = ((DefaultMutableTreeNode) last).getUserObject();
            CardLayout cl = (CardLayout) cards.getLayout();
            if (u instanceof PluginLeaf) {
                String cardKey = ((PluginLeaf) u).cardKey;
                cl.show(cards, cardKey);
                cards.revalidate();
                cards.repaint();
            }
        });

        JScrollPane left = new JScrollPane(tree);
        left.setPreferredSize(new Dimension(168, 200));
        left.setMinimumSize(new Dimension(120, 80));
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, cards);
        split.setDividerLocation(168);
        split.setResizeWeight(0);
        add(split, BorderLayout.CENTER);
        ((CardLayout) cards.getLayout()).show(cards, "_empty");
    }

    public static String categoryOf(String name, String display) {
        String n = ((name == null ? "" : name) + " " + (display == null ? "" : display)).toLowerCase();
        if (match(n, "superterminal", "csuperterminal", "realcmd", "crealcmd",
                "\u7efc\u5408\u63d2\u4ef6", "newcmd", "\u8d85\u7ea7\u7ec8\u7aef", "\u865a\u62df\u7ec8\u7aef")) {
            return "\u7ec8\u7aef";
        }
        if (match(n, "potato", "badpotato", "sweetpotato", "efspotato", "lemon",
                "petitpotam", "th_tools", "th-tools", "bypassdisable", "bypassopen")) {
            return "\u6743\u9650\u63d0\u5347";
        }
        if (match(n, "mimikatz", "sharpweb", "machinekey", "listmachinekey", "useradd")) {
            return "\u51ed\u8bc1\u8d26\u53f7";
        }
        if (match(n, "memoryshell", "filtershell", "meterpreter", "servlet", "\u5185\u5b58\u9a6c")) {
            return "\u5185\u5b58\u6301\u4e45\u5316";
        }
        if (match(n, "httpproxy", "chttpproxy", "socks", "ceasysocks", "portscan", "cportscan",
                "httptoprofile", "\u7aef\u53e3\u626b\u63cf", "\u4ee3\u7406")) {
            return "\u7f51\u7edc\u4ee3\u7406";
        }
        if (match(n, "eval", "evalcode", "shellcode", "executeassembly", "inlineexecute",
                "cexecuteassembly", "classloader", "jarloader", "peloader", "\u4ee3\u7801\u6267\u884c", "\u5185\u5b58\u52a0\u8f7d")) {
            return "\u4ee3\u7801\u6267\u884c";
        }
        if (match(n, "oatools", "enumdatabase", "attackfpm", "webshellscan")) {
            return "\u5e94\u7528\u5229\u7528";
        }
        if (match(n, "avscan", "\u6740\u8f6f", "screen", "ms17010", "netbios", "oxid", "pps")) {
            return "\u4fa6\u5bdf\u8bc6\u522b";
        }
        return "\u5176\u5b83";
    }

    private static boolean match(String hay, String... needles) {
        for (int i = 0; i < needles.length; i++) {
            if (hay.contains(needles[i])) {
                return true;
            }
        }
        return false;
    }

    private static final class PluginTreeRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            Object u = value instanceof DefaultMutableTreeNode
                    ? ((DefaultMutableTreeNode) value).getUserObject() : null;
            Icon icon = null;
            if (u instanceof PluginLeaf) {
                icon = SvgIcons.of(((PluginLeaf) u).iconKey);
            } else if (u instanceof CatNode) {
                icon = SvgIcons.of(((CatNode) u).iconKey);
            }
            if (icon != null) {
                setIcon(icon);
            }
            return this;
        }
    }

    private static void expandAll(JTree tree, TreePath path) {
        tree.expandPath(path);
        Object last = path.getLastPathComponent();
        if (!(last instanceof DefaultMutableTreeNode)) {
            return;
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) last;
        for (int i = 0; i < node.getChildCount(); i++) {
            expandAll(tree, path.pathByAddingChild(node.getChildAt(i)));
        }
    }
}
