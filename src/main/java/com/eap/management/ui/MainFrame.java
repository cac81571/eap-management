package com.eap.management.ui;

import java.awt.BorderLayout;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import com.eap.management.AppController;
import com.eap.management.config.AppConfig;

public class MainFrame extends JFrame {
    private final ProjectListPanel projects = new ProjectListPanel();
    private final ControlPanel controls = new ControlPanel();
    private final LogPanel log = new LogPanel();

    public MainFrame(AppController controller, AppConfig config) {
        super("EAP Manager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(config.getUi().getWindowWidth(), config.getUi().getWindowHeight());
        setLocationRelativeTo(null);
        setJMenuBar(createMenuBar(controller, config));

        JScrollPane controlScroll = new JScrollPane(controls);
        controlScroll.setBorder(null);
        controlScroll.getVerticalScrollBar().setUnitIncrement(16);

        JSplitPane horizontal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, projects, controlScroll);
        horizontal.setDividerLocation(280);
        horizontal.setResizeWeight(0);

        JSplitPane vertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT, horizontal, log);
        vertical.setDividerLocation(560);
        vertical.setResizeWeight(0.72);

        add(vertical, BorderLayout.CENTER);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                controller.onClose();
            }
        });
    }

    private static JMenuBar createMenuBar(AppController controller, AppConfig config) {
        JMenuBar menuBar = new JMenuBar();

        JMenu settingsMenu = new JMenu("設定");
        JMenuItem eapSettings = new JMenuItem("EAP 設定...");
        Icons.apply(eapSettings, Icons.settings());
        eapSettings.addActionListener(e -> controller.editEapSettings());
        settingsMenu.add(eapSettings);

        JMenu viewMenu = new JMenu("表示");
        JCheckBoxMenuItem darkTheme = new JCheckBoxMenuItem("ダークテーマ");
        Icons.apply(darkTheme, Icons.moon());
        darkTheme.setSelected(config.getUi().isDarkTheme());
        darkTheme.addActionListener(e -> controller.toggleTheme(darkTheme.isSelected()));
        viewMenu.add(darkTheme);

        menuBar.add(settingsMenu);
        menuBar.add(viewMenu);
        return menuBar;
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
