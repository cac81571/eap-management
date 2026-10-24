package com.eap.management.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;

import com.eap.management.AppController;
import com.eap.management.config.AppConfig;

public class MainFrame extends JFrame {
    private final ProjectListPanel projects = new ProjectListPanel();
    private final ControlPanel controls = new ControlPanel();
    private final LogPanel log = new LogPanel();
    private final JToggleButton themeToggle = new JToggleButton("ダーク");

    public MainFrame(AppController controller, AppConfig config) {
        super("EAP Manager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(config.getUi().getWindowWidth(), config.getUi().getWindowHeight());
        setLocationRelativeTo(null);

        JLabel title = new JLabel("EAP Manager");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        JButton settings = new JButton("EAP 設定");
        themeToggle.setSelected(config.getUi().isDarkTheme());
        themeToggle.setText(config.getUi().isDarkTheme() ? "ライト" : "ダーク");
        themeToggle.addActionListener(e -> {
            controller.toggleTheme(themeToggle.isSelected());
            themeToggle.setText(themeToggle.isSelected() ? "ライト" : "ダーク");
        });
        settings.addActionListener(e -> controller.editEapSettings());

        JPanel toolbarRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        toolbarRight.add(themeToggle);
        toolbarRight.add(settings);
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 16, 10, 16));
        toolbar.add(title, BorderLayout.WEST);
        toolbar.add(toolbarRight, BorderLayout.EAST);

        JScrollPane controlScroll = new JScrollPane(controls);
        controlScroll.setBorder(null);
        controlScroll.getVerticalScrollBar().setUnitIncrement(16);

        JSplitPane horizontal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, projects, controlScroll);
        horizontal.setDividerLocation(320);
        horizontal.setResizeWeight(0);

        JSplitPane vertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT, horizontal, log);
        vertical.setDividerLocation(560);
        vertical.setResizeWeight(0.72);

        add(toolbar, BorderLayout.NORTH);
        add(vertical, BorderLayout.CENTER);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                controller.onClose();
            }
        });
    }

    public ProjectListPanel projects() {
        return projects;
    }

    public ControlPanel controls() {
        return controls;
    }

    public LogPanel log() {
        return log;
    }
}
