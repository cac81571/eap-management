package com.eap.management.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.nio.file.Path;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.eap.management.config.WarProject;

public class ProjectEditDialog extends JDialog {
    private final JTextField name = new JTextField();
    private final JTextField projectDir = new JTextField();
    private final JTextField buildCommand = new JTextField();
    private final JTextField warPath = new JTextField();
    private final JTextField deployName = new JTextField();
    private final JTextField appUrl = new JTextField();
    private boolean accepted;

    public ProjectEditDialog(JFrame owner, WarProject project) {
        super(owner, project.getName().isBlank() ? "プロジェクト追加" : "プロジェクト編集", true);
        setLayout(new BorderLayout(8, 8));

        name.setText(project.getName());
        projectDir.setText(project.getProjectDir());
        buildCommand.setText(project.getBuildCommand());
        warPath.setText(project.getWarPath());
        deployName.setText(project.getDeployName());
        appUrl.setText(project.getAppUrl());
        appUrl.putClientProperty("JTextField.placeholderText", "/app または http://localhost:8080/app");

        JButton browse = new JButton("参照");
        browse.addActionListener(e -> {
            Path selected = UiUtils.chooseDirectory(this, "プロジェクトディレクトリを選択",
                    projectDir.getText().isBlank() ? null : Path.of(projectDir.getText()));
            if (selected != null) {
                projectDir.setText(selected.toAbsolutePath().toString());
                if (name.getText().isBlank()) {
                    name.setText(selected.getFileName().toString());
                }
            }
        });
        JPanel dirRow = new JPanel(new BorderLayout(8, 0));
        dirRow.add(projectDir, BorderLayout.CENTER);
        dirRow.add(browse, BorderLayout.EAST);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 8, 16));
        GridBagConstraints gbc = UiUtils.formConstraints();
        UiUtils.addFormRow(form, gbc, 0, "名前", name);
        UiUtils.addFormRow(form, gbc, 1, "プロジェクトディレクトリ", dirRow);
        UiUtils.addFormRow(form, gbc, 2, "ビルドコマンド", buildCommand);
        UiUtils.addFormRow(form, gbc, 3, "WAR パス", warPath);
        UiUtils.addFormRow(form, gbc, 4, "デプロイ名", deployName);
        UiUtils.addFormRow(form, gbc, 5, "アプリ URL", appUrl);

        JButton ok = new JButton("保存");
        JButton cancel = new JButton("キャンセル");
        ok.addActionListener(e -> {
            if (name.getText().isBlank() || projectDir.getText().isBlank()) {
                UiUtils.error(this, "名前とプロジェクトディレクトリは必須です。");
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
        setSize(640, 400);
        setLocationRelativeTo(owner);
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void applyTo(WarProject project) {
        project.setName(name.getText().trim());
        project.setProjectDir(projectDir.getText().trim());
        project.setBuildCommand(buildCommand.getText().trim());
        project.setWarPath(warPath.getText().trim());
        project.setDeployName(deployName.getText().trim());
        project.setAppUrl(appUrl.getText().trim());
    }
}
