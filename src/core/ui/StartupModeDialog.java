package core.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.yaml.snakeyaml.Yaml;

public class StartupModeDialog extends JDialog {

    public static class DbConfig {
        public final String dbPath;
        public final String username;
        public final String password;
        public final String operatorName;
        public final boolean isPg;

        public DbConfig(String dbPath, String op, boolean isPg) {
            this(dbPath, "", "", op, isPg);
        }

        public DbConfig(String dbPath, String user, String pass, String op, boolean isPg) {
            this.dbPath = dbPath;
            this.username = user;
            this.password = pass;
            this.operatorName = op;
            this.isPg = isPg;
        }

        public boolean isRemote() {
            return dbPath != null && !dbPath.isEmpty();
        }
    }

    private DbConfig result;
    private JRadioButton localRb;
    private JRadioButton pgRb;
    private JTextField hostField;
    private JTextField portField;
    private JTextField dbNameField;
    private JTextField userField;
    private JTextField opField;
    private JPasswordField passField;
    private JButton testBtn;
    private JButton saveBtn;
    private JButton loadBtn;

    public static DbConfig showDialog() {
        return showDialog(null);
    }

    public static DbConfig showDialog(Frame owner) {
        StartupModeDialog d = new StartupModeDialog(owner);
        d.setVisible(true);
        return d.result;
    }

    public StartupModeDialog(Frame owner) {
        super(owner, "\u6570\u636e\u6e90", true);
        setSize(480, 440);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel main = new JPanel(new BorderLayout(8, 8));
        main.setBorder(BorderFactory.createEmptyBorder(16, 24, 12, 24));
        JLabel title = new JLabel("\u6570\u636e\u6e90", JLabel.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        main.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.CENTER;

        localRb = new JRadioButton("localdb\uff08\u672c\u5730 data.db\uff09", true);
        pgRb = new JRadioButton("PostgreSQL \u8fdc\u7a0b");
        ButtonGroup bg = new ButtonGroup();
        bg.add(localRb);
        bg.add(pgRb);
        JPanel radios = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 0));
        radios.add(localRb);
        radios.add(pgRb);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        form.add(radios, gbc);
        gbc.gridwidth = 1;

        hostField = sizedField(new JTextField());
        portField = sizedField(new JTextField("5432"));
        dbNameField = sizedField(new JTextField("gsl5"));
        userField = sizedField(new JTextField());
        passField = sizedPassword();
        opField = sizedField(new JTextField(System.getProperty("user.name", "op")));

        addRow(form, gbc, 1, "\u4e3b\u673a", hostField);
        addRow(form, gbc, 2, "\u7aef\u53e3", portField);
        addRow(form, gbc, 3, "\u6570\u636e\u5e93", dbNameField);
        addRow(form, gbc, 4, "\u7528\u6237", userField);
        addRow(form, gbc, 5, "\u5bc6\u7801", passField);
        addRow(form, gbc, 6, "\u64cd\u4f5c\u5458", opField);
        localRb.addActionListener(e -> togglePg(false));
        pgRb.addActionListener(e -> togglePg(true));

        JPanel centerWrap = new JPanel(new GridBagLayout());
        centerWrap.add(form);
        main.add(centerWrap, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        testBtn = new JButton("\u6d4b\u8bd5\u8fde\u63a5");
        testBtn.addActionListener(e -> testConnection());
        saveBtn = new JButton("\u4fdd\u5b58");
        saveBtn.addActionListener(e -> saveConfig());
        loadBtn = new JButton("\u52a0\u8f7d");
        loadBtn.addActionListener(e -> loadAndClose());
        btnPanel.add(testBtn);
        btnPanel.add(saveBtn);
        btnPanel.add(loadBtn);
        main.add(btnPanel, BorderLayout.SOUTH);
        setContentPane(main);
        togglePg(false);
        loadSavedConfig();
    }

    private static final Dimension LABEL_SIZE = new Dimension(72, 24);
    private static final Dimension FIELD_SIZE = new Dimension(260, 26);

    private static JTextField sizedField(JTextField field) {
        field.setPreferredSize(FIELD_SIZE);
        field.setMinimumSize(FIELD_SIZE);
        field.setMaximumSize(FIELD_SIZE);
        return field;
    }

    private static JPasswordField sizedPassword() {
        JPasswordField field = new JPasswordField();
        field.setPreferredSize(FIELD_SIZE);
        field.setMinimumSize(FIELD_SIZE);
        field.setMaximumSize(FIELD_SIZE);
        return field;
    }

    private static void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, java.awt.Component field) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel lb = new JLabel(label, JLabel.RIGHT);
        lb.setPreferredSize(LABEL_SIZE);
        panel.add(lb, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(field, gbc);
    }

    private String jdbcUrl() {
        return "jdbc:postgresql://" + hostField.getText().trim() + ":" + portField.getText().trim()
                + "/" + dbNameField.getText().trim();
    }

    private void togglePg(boolean pg) {
        hostField.setEnabled(pg);
        portField.setEnabled(pg);
        dbNameField.setEnabled(pg);
        userField.setEnabled(pg);
        passField.setEnabled(pg);
        opField.setEnabled(pg);
        if (testBtn != null) {
            testBtn.setEnabled(pg);
        }
    }

    private void testConnection() {
        if (!pgRb.isSelected()) {
            JOptionPane.showMessageDialog(this, "\u672c\u5730\u5e93\u65e0\u9700\u6d4b\u8bd5", "\u6d4b\u8bd5\u8fde\u63a5", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        testBtn.setEnabled(false);
        testBtn.setText("\u6d4b\u8bd5\u4e2d...");
        new Thread(() -> {
            try {
                Class.forName("org.postgresql.Driver");
                Connection c = DriverManager.getConnection(jdbcUrl(), userField.getText().trim(),
                        new String(passField.getPassword()));
                c.close();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "\u8fde\u63a5\u6210\u529f", "\u6d4b\u8bd5\u8fde\u63a5", JOptionPane.INFORMATION_MESSAGE));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "\u5931\u8d25: " + ex.getMessage(), "\u9519\u8bef", JOptionPane.ERROR_MESSAGE));
            } finally {
                SwingUtilities.invokeLater(() -> {
                    testBtn.setEnabled(true);
                    testBtn.setText("\u6d4b\u8bd5\u8fde\u63a5");
                });
            }
        }).start();
    }

    private void saveConfig() {
        try {
            Map<String, Object> root;
            java.io.File f = new java.io.File("config.yaml");
            if (f.exists()) {
                String content = new String(Files.readAllBytes(Paths.get("config.yaml")), StandardCharsets.UTF_8);
                root = new Yaml().load(content);
                if (root == null) {
                    root = new LinkedHashMap<String, Object>();
                }
            } else {
                root = new LinkedHashMap<String, Object>();
            }
            Map<String, Object> db = new LinkedHashMap<String, Object>();
            db.put("mode", pgRb.isSelected() ? "pg" : "local");
            db.put("host", hostField.getText().trim());
            db.put("port", portField.getText().trim());
            db.put("dbName", dbNameField.getText().trim());
            db.put("user", userField.getText().trim());
            db.put("password", new String(passField.getPassword()));
            db.put("operator", opField.getText().trim());
            root.put("database", db);
            Files.write(Paths.get("config.yaml"), new Yaml().dump(root).getBytes(StandardCharsets.UTF_8));
            JOptionPane.showMessageDialog(this, "\u5df2\u4fdd\u5b58", "\u4fdd\u5b58", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "\u4fdd\u5b58\u5931\u8d25: " + ex.getMessage(), "\u9519\u8bef", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadAndClose() {
        String op = opField.getText().trim();
        if (op.isEmpty()) {
            op = System.getProperty("user.name", "operator");
        }
        if (localRb.isSelected()) {
            result = new DbConfig("", op, false);
            dispose();
            return;
        }
        if (hostField.getText().trim().isEmpty() || dbNameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "\u8bf7\u586b\u5199\u4e3b\u673a\u548c\u6570\u636e\u5e93", "\u63d0\u793a", JOptionPane.WARNING_MESSAGE);
            return;
        }
        result = new DbConfig(jdbcUrl(), userField.getText().trim(), new String(passField.getPassword()), op, true);
        dispose();
    }

    private void loadSavedConfig() {
        try {
            java.io.File f = new java.io.File("config.yaml");
            if (!f.exists()) {
                return;
            }
            String content = new String(Files.readAllBytes(Paths.get("config.yaml")), StandardCharsets.UTF_8);
            Map<String, Object> root = new Yaml().load(content);
            if (root == null) {
                return;
            }
            Object dbObj = root.get("database");
            if (!(dbObj instanceof Map)) {
                return;
            }
            Map<String, Object> db = (Map<String, Object>) dbObj;
            String mode = String.valueOf(db.getOrDefault("mode", "local"));
            if ("pg".equals(mode)) {
                pgRb.setSelected(true);
                togglePg(true);
            } else {
                localRb.setSelected(true);
                togglePg(false);
            }
            if (db.get("host") != null) {
                hostField.setText(db.get("host").toString());
            }
            if (db.get("port") != null) {
                portField.setText(db.get("port").toString());
            }
            if (db.get("dbName") != null) {
                dbNameField.setText(db.get("dbName").toString());
            }
            if (db.get("user") != null) {
                userField.setText(db.get("user").toString());
            }
            if (db.get("password") != null) {
                passField.setText(db.get("password").toString());
            }
            if (db.get("operator") != null) {
                opField.setText(db.get("operator").toString());
            }
        } catch (Exception ignored) {
        }
    }
}
