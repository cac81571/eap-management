package com.eap.management.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.nio.file.Path;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import com.eap.management.config.DeployMode;
import com.eap.management.config.EapSettings;
import com.eap.management.process.ConsoleEncoding;

public class EapSettingsDialog extends JDialog {
    private final JTextField home = new JTextField();
    private final JTextField configFile = new JTextField();
    private final JTextField bindAddress = new JTextField();
    private final JTextField httpPort = new JTextField();
    private final JTextField managementPort = new JTextField();
    private final JTextField extraArgs = new JTextField();
    private final JComboBox<String> consoleEncoding = new JComboBox<>(
            ConsoleEncoding.choices().toArray(String[]::new));
    private final JRadioButton directoryMode = new JRadioButton("ディレクトリコピー (standalone/deployments)");
    private final JRadioButton cliMode = new JRadioButton("jboss-cli");
    private boolean accepted;

    public EapSettingsDialog(JFrame owner, EapSettings settings) {
        super(owner, "EAP 設定", true);
        setLayout(new BorderLayout(8, 8));

        home.setText(settings.getHome());
        configFile.setText(settings.getConfigFile());
        bindAddress.setText(settings.getBindAddress());
        httpPort.setText(String.valueOf(settings.getHttpPort()));
        managementPort.setText(String.valueOf(settings.getManagementPort()));
        extraArgs.setText(settings.getExtraArgs());
        consoleEncoding.setSelectedItem(ConsoleEncoding.normalize(settings.getConsoleEncoding()));
        directoryMode.setSelected(settings.getDeployMode() == DeployMode.DIRECTORY);
        cliMode.setSelected(settings.getDeployMode() == DeployMode.CLI);
        ButtonGroup group = new ButtonGroup();
        group.add(directoryMode);
        group.add(cliMode);

        JButton browse = new JButton("参照");
        browse.addActionListener(e -> {
            Path selected = UiUtils.chooseDirectory(this, "EAP ホームを選択",
                    home.getText().isBlank() ? null : Path.of(home.getText()));
            if (selected != null) {
                home.setText(selected.toAbsolutePath().toString());
            }
        });
        JPanel homeRow = new JPanel(new BorderLayout(8, 0));
        homeRow.add(home, BorderLayout.CENTER);
        homeRow.add(browse, BorderLayout.EAST);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 8, 16));
        GridBagConstraints gbc = UiUtils.formConstraints();
        UiUtils.addFormRow(form, gbc, 0, "EAP ホーム", homeRow);
        UiUtils.addFormRow(form, gbc, 1, "設定ファイル", configFile);
        UiUtils.addFormRow(form, gbc, 2, "バインドアドレス", bindAddress);
        UiUtils.addFormRow(form, gbc, 3, "HTTP ポート", httpPort);
        UiUtils.addFormRow(form, gbc, 4, "管理ポート", managementPort);
        UiUtils.addFormRow(form, gbc, 5, "追加引数", extraArgs);
        UiUtils.addFormRow(form, gbc, 6, "ログ文字コード", consoleEncoding);
        UiUtils.addFormRow(form, gbc, 7, "デプロイ方式", directoryMode);
        gbc.gridy = 8;
        gbc.gridx = 1;
        form.add(cliMode, gbc);

        JButton ok = new JButton("保存");
        JButton cancel = new JButton("キャンセル");
        ok.addActionListener(e -> {
            if (parsePort(httpPort.getText(), "HTTP ポート") < 0
                    || parsePort(managementPort.getText(), "管理ポート") < 0) {
                return;
            }
            accepted = true;
            dispose();
        });
        cancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancel);
        buttons.add(ok);

        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        setSize(640, 470);
        setLocationRelativeTo(owner);
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void applyTo(EapSettings settings) {
        settings.setHome(home.getText().trim());
        settings.setConfigFile(configFile.getText().trim());
        settings.setBindAddress(bindAddress.getText().trim());
        settings.setHttpPort(parsePort(httpPort.getText(), "HTTP ポート"));
        settings.setManagementPort(parsePort(managementPort.getText(), "管理ポート"));
        settings.setExtraArgs(extraArgs.getText().trim());
        settings.setConsoleEncoding((String) consoleEncoding.getSelectedItem());
        settings.setDeployMode(cliMode.isSelected() ? DeployMode.CLI : DeployMode.DIRECTORY);
    }

    private int parsePort(String value, String label) {
        try {
            int port = Integer.parseInt(value.trim());
            if (port < 1 || port > 65535) {
                UiUtils.error(this, label + " は 1〜65535 で入力してください。");
                return -1;
            }
            return port;
        } catch (NumberFormatException e) {
            UiUtils.error(this, label + " は数値で入力してください。");
            return -1;
        }
    }
}
