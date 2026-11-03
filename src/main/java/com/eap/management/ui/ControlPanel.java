package com.eap.management.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;

import com.eap.management.config.WarProject;
import com.eap.management.process.EapStatus;

public class ControlPanel extends JPanel {
    public interface Listener {
        void onStart();

        void onStartDebug();

        void onStop();

        void onRestart();

        void onBuild();

        void onDeploy();

        void onBuildAndDeploy();

        void onRemoveDeployedWarsChanged(boolean enabled);

        void onOpenProject();

        void onExportLaunchJson();

        void onExportTasksJson();

        void onOpenDeployments();

        void onOpenConfig();

        void onOpenStandaloneConf();

        void onOpenApp();
    }

    private final JLabel statusDot = new JLabel("●");
    private final JLabel statusLabel = new JLabel("停止");
    private final JLabel eapHint = new JLabel("EAP ホームが未設定です");
    private final LinkLabel managementLink = new LinkLabel();
    private final LinkLabel httpLink = new LinkLabel();
    private final JPanel eapInfo = new JPanel(new CardLayout());
    private final JLabel selectedLabel = new JLabel("プロジェクトが選択されていません");
    private final JLabel lastDeployedLabel = new JLabel("前回デプロイ: なし");
    private final JLabel appUrlPrefix = new JLabel("URL:");
    private final LinkLabel appUrlLink = new LinkLabel();
    private final JPanel appUrlRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    private final JButton startButton = new JButton("起動");
    private final JButton startDebugButton = new JButton("デバッグ起動");
    private final JButton stopButton = new JButton("停止");
    private final JButton restartButton = new JButton("再起動");
    private final JButton buildButton = new JButton("ビルド");
    private final JButton deployButton = new JButton("デプロイ");
    private final JButton buildDeployButton = new JButton("ビルド＋デプロイ");
    private final JButton openProjectButton = new JButton("プロジェクトを開く");
    private final JButton exportLaunchJsonButton = new JButton("launch.json を出力");
    private final JButton exportTasksJsonButton = new JButton("tasks.json を出力");
    private final JCheckBox removeDeployedWars = new JCheckBox("既存の WAR を削除する", true);
    private final JButton openDeploymentsButton = new JButton("deployments を開く");
    private final JButton openConfigButton = new JButton("設定ファイルを開く");
    private final JButton openStandaloneConfButton = new JButton("standalone.conf.bat を開く");
    private final JButton openAppButton = new JButton("アプリを開く");
    private final EnvTablePanel globalEnv = new EnvTablePanel();
    private final EnvTablePanel projectEnv = new EnvTablePanel();

    private Listener listener;

    public ControlPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.add(eapCard());
        stack.add(Box.createVerticalStrut(6));
        stack.add(warCard());
        stack.add(Box.createVerticalStrut(6));
        stack.add(envCard());
        add(stack, BorderLayout.NORTH);

        Icons.apply(startButton, Icons.play());
        Icons.apply(startDebugButton, Icons.bug());
        Icons.apply(stopButton, Icons.stop());
        Icons.apply(restartButton, Icons.restart());
        Icons.apply(buildButton, Icons.build());
        Icons.apply(deployButton, Icons.deploy());
        Icons.apply(buildDeployButton, Icons.buildDeploy());
        Icons.apply(openProjectButton, Icons.folder());
        Icons.apply(exportLaunchJsonButton, Icons.file());
        Icons.apply(exportTasksJsonButton, Icons.tasks());
        Icons.apply(openDeploymentsButton, Icons.folder());
        Icons.apply(openConfigButton, Icons.file());
        Icons.apply(openStandaloneConfButton, Icons.settings());
        Icons.apply(openAppButton, Icons.browser());

        startButton.addActionListener(e -> fire(Listener::onStart));
        startDebugButton.addActionListener(e -> fire(Listener::onStartDebug));
        stopButton.addActionListener(e -> fire(Listener::onStop));
        restartButton.addActionListener(e -> fire(Listener::onRestart));
        buildButton.addActionListener(e -> fire(Listener::onBuild));
        deployButton.addActionListener(e -> fire(Listener::onDeploy));
        buildDeployButton.putClientProperty("JButton.buttonType", "roundRect");
        buildDeployButton.addActionListener(e -> fire(Listener::onBuildAndDeploy));
        openProjectButton.addActionListener(e -> fire(Listener::onOpenProject));
        exportLaunchJsonButton.addActionListener(e -> fire(Listener::onExportLaunchJson));
        exportTasksJsonButton.addActionListener(e -> fire(Listener::onExportTasksJson));
        openDeploymentsButton.addActionListener(e -> fire(Listener::onOpenDeployments));
        openConfigButton.addActionListener(e -> fire(Listener::onOpenConfig));
        openStandaloneConfButton.addActionListener(e -> fire(Listener::onOpenStandaloneConf));
        openAppButton.addActionListener(e -> fire(Listener::onOpenApp));
        removeDeployedWars.addActionListener(e -> fire(l -> l.onRemoveDeployedWarsChanged(removeDeployedWars.isSelected())));
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public EnvTablePanel globalEnv() {
        return globalEnv;
    }

    public EnvTablePanel projectEnv() {
        return projectEnv;
    }

    public void setStatus(EapStatus status, String hint, String managementEndpoint, String managementUrl,
                          String httpUrl, boolean debug) {
        statusLabel.setText(status == EapStatus.RUNNING && debug ? "稼働中 (デバッグ)" : status.label());
        Color color = switch (status) {
            case RUNNING -> new Color(46, 160, 67);
            case STARTING, STOPPING, RESTARTING -> new Color(210, 140, 20);
            case UNKNOWN, UNCONFIGURED -> new Color(120, 120, 120);
            case STOPPED -> new Color(180, 70, 70);
        };
        statusDot.setForeground(color);
        statusLabel.setForeground(color);

        CardLayout cards = (CardLayout) eapInfo.getLayout();
        if (managementUrl != null && !managementUrl.isBlank() && httpUrl != null && !httpUrl.isBlank()) {
            String endpoint = managementEndpoint == null || managementEndpoint.isBlank()
                    ? managementUrl
                    : managementEndpoint;
            managementLink.setLink("管理 " + endpoint, managementUrl);
            httpLink.setLink("HTTP " + httpUrl, httpUrl);
            cards.show(eapInfo, "links");
        } else {
            eapHint.setText(hint == null ? "" : hint);
            cards.show(eapInfo, "hint");
        }
    }

    public void setSelectedProject(WarProject project, String lastDeployedName, String appUrl) {
        if (project == null) {
            selectedLabel.setText("プロジェクト未選択");
            appUrlLink.clear();
            appUrlPrefix.setText("URL: なし");
        } else {
            String deploy = project.getDeployName().isBlank() ? project.getWarPath() : project.getDeployName();
            selectedLabel.setText("<html>WAR: <b>" + escape(project.getName()) + "</b> （"
                    + escape(deploy) + "）</html>");
            if (appUrl == null || appUrl.isBlank()) {
                appUrlLink.clear();
                appUrlPrefix.setText("URL: なし");
            } else {
                appUrlPrefix.setText("URL:");
                appUrlLink.setLink(appUrl, appUrl);
            }
        }
        lastDeployedLabel.setText("前回: " + (lastDeployedName == null || lastDeployedName.isBlank()
                ? "なし"
                : lastDeployedName));
    }

    public void setBusy(EapStatus status, boolean jobBusy, boolean hasProject, boolean eapConfigured) {
        boolean eapBusy = status.transitioning();
        startButton.setEnabled(!eapBusy && !jobBusy && eapConfigured);
        startDebugButton.setEnabled(!eapBusy && !jobBusy && eapConfigured);
        stopButton.setEnabled(!jobBusy && eapConfigured && status == EapStatus.RUNNING);
        restartButton.setEnabled(!eapBusy && !jobBusy && eapConfigured);
        buildButton.setEnabled(!jobBusy && hasProject);
        deployButton.setEnabled(!jobBusy && hasProject && eapConfigured);
        buildDeployButton.setEnabled(!jobBusy && hasProject && eapConfigured);
        openProjectButton.setEnabled(hasProject);
        exportLaunchJsonButton.setEnabled(hasProject);
        exportTasksJsonButton.setEnabled(hasProject && eapConfigured);
        openDeploymentsButton.setEnabled(eapConfigured);
        openConfigButton.setEnabled(eapConfigured);
        openStandaloneConfButton.setEnabled(eapConfigured);
    }

    public void setRemoveDeployedWars(boolean enabled) {
        removeDeployedWars.setSelected(enabled);
    }

    private JPanel eapCard() {
        statusDot.setFont(statusDot.getFont().deriveFont(Font.BOLD, 14f));
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 13f));
        eapHint.setForeground(UIManager.getColor("Label.disabledForeground"));
        eapHint.setHorizontalAlignment(JLabel.RIGHT);

        JPanel links = new JPanel();
        links.setOpaque(false);
        links.setLayout(new BoxLayout(links, BoxLayout.Y_AXIS));
        managementLink.setAlignmentX(RIGHT_ALIGNMENT);
        httpLink.setAlignmentX(RIGHT_ALIGNMENT);
        links.add(managementLink);
        links.add(httpLink);

        eapInfo.setOpaque(false);
        eapInfo.add(eapHint, "hint");
        eapInfo.add(links, "links");

        JPanel body = stacked(
                buttonRow(startButton, startDebugButton, stopButton, restartButton),
                buttonRow(openDeploymentsButton, openConfigButton, openStandaloneConfButton, openAppButton)
        );

        JPanel card = card();
        card.add(headerRow(titleLabel("EAP"), statusRow(), eapInfo), BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return wrapCard(card);
    }

    private JPanel warCard() {
        selectedLabel.setFont(selectedLabel.getFont().deriveFont(13f));
        lastDeployedLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        appUrlPrefix.setForeground(UIManager.getColor("Label.disabledForeground"));
        appUrlRow.setOpaque(false);
        appUrlRow.add(appUrlPrefix);
        appUrlRow.add(appUrlLink);

        JPanel meta = new JPanel(new GridBagLayout());
        meta.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 0, 0, 12);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        meta.add(selectedLabel, gbc);
        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        meta.add(lastDeployedLabel, gbc);
        gbc.gridx = 2;
        meta.add(appUrlRow, gbc);

        removeDeployedWars.setAlignmentX(LEFT_ALIGNMENT);
        JPanel body = stacked(
                meta,
                buttonRow(
                        buildButton, deployButton, buildDeployButton,
                        openProjectButton, exportLaunchJsonButton, exportTasksJsonButton
                ),
                removeDeployedWars
        );

        JPanel card = card();
        card.add(headerRow(titleLabel("WAR のビルドとデプロイ"), null, null), BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return wrapCard(card);
    }

    private JPanel envCard() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setPreferredSize(new Dimension(720, 180));
        tabs.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        tabs.addTab("共通", globalEnv);
        tabs.addTab("選択プロジェクト", projectEnv);
        tabs.setToolTipText("共通は EAP 起動とビルドの両方に、プロジェクトは選択中 WAR のビルドと起動に適用されます。");

        JPanel card = card();
        card.add(headerRow(titleLabel("環境変数"), null, null), BorderLayout.NORTH);
        card.add(tabs, BorderLayout.CENTER);
        return wrapCard(card);
    }

    private JPanel statusRow() {
        JPanel status = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        status.setOpaque(false);
        status.add(statusDot);
        status.add(statusLabel);
        return status;
    }

    private static JPanel headerRow(JLabel title, JPanel status, Component east) {
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(title);
        if (status != null) {
            left.add(status);
        }

        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        header.add(left, BorderLayout.WEST);
        if (east != null) {
            header.add(east, BorderLayout.EAST);
        }
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        return header;
    }

    private static JPanel buttonRow(JButton... buttons) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        for (JButton button : buttons) {
            row.add(button);
        }
        return row;
    }

    private static JPanel stacked(java.awt.Component... components) {
        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setAlignmentX(LEFT_ALIGNMENT);
        for (int i = 0; i < components.length; i++) {
            java.awt.Component component = components[i];
            if (component instanceof JPanel panel) {
                panel.setAlignmentX(LEFT_ALIGNMENT);
            }
            stack.add(component);
            if (i < components.length - 1) {
                stack.add(Box.createVerticalStrut(4));
            }
        }
        return stack;
    }

    private static JLabel titleLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
        return label;
    }

    private static JPanel card() {
        JPanel card = new JPanel(new BorderLayout(0, 0));
        card.setBorder(UiUtils.cardBorder());
        return card;
    }

    private static JPanel wrapCard(JPanel card) {
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return card;
    }

    private void fire(java.util.function.Consumer<Listener> action) {
        if (listener != null) {
            action.accept(listener);
        }
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (getBorder() != null) {
            setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        }
        if (statusDot != null) {
            statusDot.setForeground(UIManager.getColor("Label.foreground"));
        }
        if (eapHint != null) {
            eapHint.setForeground(UIManager.getColor("Label.disabledForeground"));
        }
        if (lastDeployedLabel != null) {
            lastDeployedLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        }
        if (appUrlPrefix != null) {
            appUrlPrefix.setForeground(UIManager.getColor("Label.disabledForeground"));
        }
    }
}
