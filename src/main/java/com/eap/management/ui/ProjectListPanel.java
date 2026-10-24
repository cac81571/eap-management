package com.eap.management.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import com.eap.management.config.WarProject;

public class ProjectListPanel extends JPanel {
    public interface Listener {
        void onSelectionChanged(WarProject project);

        void onAdd();

        void onEdit(WarProject project);

        void onRemove(WarProject project);
    }

    private final DefaultListModel<WarProject> model = new DefaultListModel<>();
    private final JList<WarProject> list = new JList<>(model);
    private Listener listener;
    private String lastDeployedId = "";

    public ProjectListPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, UIManager.getColor("Component.borderColor")),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        setPreferredSize(new Dimension(320, 0));

        JLabel title = new JLabel("WAR プロジェクト");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
        JLabel hint = new JLabel("<html>追加したプロジェクトを選ぶと、<br/>それがデプロイ対象になります。</html>");
        hint.setForeground(UIManager.getColor("Label.disabledForeground"));
        JPanel header = new JPanel(new BorderLayout(0, 8));
        header.add(title, BorderLayout.NORTH);
        header.add(hint, BorderLayout.SOUTH);

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new ProjectRenderer());
        list.setFixedCellHeight(72);
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && listener != null) {
                listener.onSelectionChanged(list.getSelectedValue());
            }
        });
        list.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && list.getSelectedValue() != null && listener != null) {
                    listener.onEdit(list.getSelectedValue());
                }
            }
        });

        JButton add = new JButton("追加");
        JButton edit = new JButton("編集");
        JButton remove = new JButton("削除");
        add.addActionListener(e -> {
            if (listener != null) {
                listener.onAdd();
            }
        });
        edit.addActionListener(e -> {
            if (listener != null && list.getSelectedValue() != null) {
                listener.onEdit(list.getSelectedValue());
            }
        });
        remove.addActionListener(e -> {
            if (listener != null && list.getSelectedValue() != null) {
                listener.onRemove(list.getSelectedValue());
            }
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.add(add);
        buttons.add(edit);
        buttons.add(remove);

        add(header, BorderLayout.NORTH);
        add(new JScrollPane(list), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setProjects(List<WarProject> projects, String selectedId, String lastDeployedId) {
        this.lastDeployedId = lastDeployedId == null ? "" : lastDeployedId;
        WarProject previous = list.getSelectedValue();
        model.clear();
        projects.forEach(model::addElement);
        WarProject select = null;
        for (WarProject project : projects) {
            if (project.getId().equals(selectedId)) {
                select = project;
                break;
            }
        }
        if (select == null && previous != null) {
            select = previous;
        }
        if (select == null && !projects.isEmpty()) {
            select = projects.getFirst();
        }
        if (select != null) {
            list.setSelectedValue(select, true);
        } else {
            list.clearSelection();
        }
        list.repaint();
    }

    public WarProject selected() {
        return list.getSelectedValue();
    }

    private final class ProjectRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> jlist, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(jlist, value, index, isSelected, cellHasFocus);
            if (!(value instanceof WarProject project)) {
                return label;
            }
            boolean deployed = project.getId().equals(lastDeployedId);
            String badge = isSelected ? "デプロイ対象" : (deployed ? "前回デプロイ" : "");
            label.setText("<html><b>" + escape(project.getName()) + "</b><br/>"
                    + "<span style='font-size:10px'>" + escape(shortPath(project)) + "</span>"
                    + (badge.isEmpty() ? "" : "<br/><span style='font-size:10px'>" + badge + "</span>")
                    + "</html>");
            label.setVerticalAlignment(SwingConstants.CENTER);
            label.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            if (!isSelected) {
                label.setBackground(index % 2 == 0
                        ? UIManager.getColor("List.background")
                        : mix(UIManager.getColor("List.background"), UIManager.getColor("Component.borderColor")));
            }
            return label;
        }
    }

    private static String shortPath(WarProject project) {
        String deploy = project.getDeployName();
        if (deploy != null && !deploy.isBlank()) {
            return deploy;
        }
        return project.getWarPath();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static Color mix(Color base, Color tint) {
        if (base == null) {
            return Color.WHITE;
        }
        if (tint == null) {
            return base;
        }
        return new Color(
                (base.getRed() * 9 + tint.getRed()) / 10,
                (base.getGreen() * 9 + tint.getGreen()) / 10,
                (base.getBlue() * 9 + tint.getBlue()) / 10
        );
    }
}
