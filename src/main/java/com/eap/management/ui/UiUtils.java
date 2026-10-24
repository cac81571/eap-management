package com.eap.management.ui;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.Border;

public final class UiUtils {
    private UiUtils() {
    }

    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(javax.swing.UIManager.getColor("Component.borderColor"), 1, true),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        );
    }

    public static void addFormRow(java.awt.Container parent, GridBagConstraints gbc, int row, String label,
                                  Component field) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        parent.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        parent.add(field, gbc);
    }

    public static GridBagConstraints formConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 0, 6, 8);
        return gbc;
    }

    public static Path chooseDirectory(Component parent, String title, Path current) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (current != null) {
            File file = current.toFile();
            chooser.setCurrentDirectory(file.isDirectory() ? file : file.getParentFile());
        }
        return chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION
                ? chooser.getSelectedFile().toPath()
                : null;
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "エラー", JOptionPane.ERROR_MESSAGE);
    }

    public static void error(Component parent, String message, Throwable error) {
        StringWriter writer = new StringWriter();
        writer.write(message);
        writer.write(System.lineSeparator());
        writer.write(System.lineSeparator());
        error.printStackTrace(new PrintWriter(writer));
        JTextArea area = new JTextArea(writer.toString());
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(560, 280));
        JOptionPane.showMessageDialog(parent, scroll, "エラー", JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "情報", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "確認", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
    }

    public static boolean browseSupported() {
        return Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);
    }
}
