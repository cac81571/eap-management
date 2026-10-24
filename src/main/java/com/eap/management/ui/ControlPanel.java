package com.eap.management.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;

import com.eap.management.config.WarProject;
import com.eap.management.process.EapStatus;

public class ControlPanel extends JPanel {
    public interface Listener {
        void onStart();

        void onStop();

        void onRestart();

        void onBuild();

        void onDeploy();

        void onBuildAndDeploy();

        void onOpenProject();

        void onOpenDeployments();

        void onOpenConfig();

        void onOpenStandaloneConf();

        void onOpenApp();
    }

    private final JLabel statusDot = new JLabel("●");
    private final JLabel statusLabel = new JLabel("停止");
    private final JLabel eapHint = new JLabel("EAP ホームが未設定です");
    private final JLabel selectedLabel = new JLabel("プロジェクトが選択されていません");
    private final JLabel lastDeployedLabel = new JLabel("前回デプロイ: なし");
    private final JLabel appUrlLabel = new JLabel("アプリ URL: なし");
    private final JButton startButton = new JButton("起動");
    private final JButton stopButton = new JButton("停止");
    private final JButton restartButton = new JButton("再起動");
    private final JButton buildButton = new JButton("ビルド");
    private final JButton deployButton = new JButton("WAR を切り替えてデプロイ");
    private final JButton buildDeployButton = new JButton("ビルドしてデプロイ");
    private final JButton openProjectButton = new JButton("プロジェクトを開く");
    private final JButton openDeploymentsButton = new JButton("deployments を開く");
    private final JButton openConfigButton = new JButton("設定ファイルを開く");
    private final JButton openStandaloneConfButton = new JButton("standalone.conf.bat を開く");
    private final JButton openAppButton = new JButton("アプリを開く");
    private final EnvTablePanel globalEnv = new EnvTablePanel();
    private final EnvTablePanel projectEnv = new EnvTablePanel();

    private Listener listener;

    public ControlPanel() {
        super(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));

        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.add(eapCard());
        stack.add(Box.createVerticalStrut(12));
        stack.add(warCard());
        stack.add(Box.createVerticalStrut(12));
        stack.add(envCard());

        add(stack, BorderLayout.NORTH);

        startButton.addActionListener(e -> fire(Listener::onStart));
        stopButton.addActionListener(e -> fire(Listener::onStop));
        restartButton.addActionListener(e -> fire(Listener::onRestart));
        buildButton.addActionListener(e -> fire(Listener::onBuild));
        deployButton.addActionListener(e -> fire(Listener::onDeploy));
        buildDeployButton.putClientProperty("JButton.buttonType", "roundRect");
        buildDeployButton.addActionListener(e -> fire(Listener::onBuildAndDeploy));
        openProjectButton.addActionListener(e -> fire(Listener::onOpenProject));
        openDeploymentsButton.addActionListener(e -> fire(Listener::onOpenDeployments));
        openConfigButton.addActionListener(e -> fire(Listener::onOpenConfig));
        openStandaloneConfButton.addActionListener(e -> fire(Listener::onOpenStandaloneConf));
        openAppButton.addActionListener(e -> fire(Listener::onOpenApp));
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

    public void setStatus(EapStatus status, String hint) {
        statusLabel.setText(status.label());
        eapHint.setText(hint == null ? "" : hint);
        Color color = switch (status) {
            case RUNNING -> new Color(46, 160, 67);
            case STARTING, STOPPING, RESTARTING -> new Color(210, 140, 20);
            case UNKNOWN, UNCONFIGURED -> new Color(120, 120, 120);
            case STOPPED -> new Color(180, 70, 70);
        };
        statusDot.setForeground(color);
        statusLabel.setForeground(color);
    }

    public void setSelectedProject(WarProject project, String lastDeployedName, String appUrl) {
        if (project == null) {
            selectedLabel.setText("プロジェクトが選択されていません");
            appUrlLabel.setText("アプリ URL: なし");
        } else {
            String deploy = project.getDeployName().isBlank() ? project.getWarPath() : project.getDeployName();
            selectedLabel.setText("<html>デプロイする WAR: <b>" + escape(project.getName()) + "</b> （"
                    + escape(deploy) + "）</html>");
            appUrlLabel.setText("アプリ URL: " + (appUrl == null || appUrl.isBlank() ? "なし" : appUrl));
        }
        lastDeployedLabel.setText("前回デプロイ: " + (lastDeployedName == null || lastDeployedName.isBlank()
                ? "なし"
                : lastDeployedName));
    }

    public void setBusy(EapStatus status, boolean jobBusy, boolean hasProject, boolean eapConfigured) {
        boolean eapBusy = status.transitioning();
        startButton.setEnabled(!eapBusy && !jobBusy && eapConfigured);
        stopButton.setEnabled(!jobBusy && eapConfigured && status == EapStatus.RUNNING);
        restartButton.setEnabled(!eapBusy && !jobBusy && eapConfigured);
        buildButton.setEnabled(!jobBusy && hasProject);
        deployButton.setEnabled(!jobBusy && hasProject && eapConfigured);
        buildDeployButton.setEnabled(!jobBusy && hasProject && eapConfigured);
        openProjectButton.setEnabled(hasProject);
        openDeploymentsButton.setEnabled(eapConfigured);
        openConfigButton.setEnabled(eapConfigured);
        openStandaloneConfButton.setEnabled(eapConfigured);
    }

    private JPanel eapCard() {
        statusDot.setFont(statusDot.getFont().deriveFont(Font.BOLD, 16f));
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 15f));
        JPanel card = titledCard("EAP", statusDot, statusLabel);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setAlignmentX(LEFT_ALIGNMENT);
        buttons.add(startButton);
        buttons.add(stopButton);
        buttons.add(restartButton);
        buttons.add(openDeploymentsButton);
        buttons.add(openConfigButton);
        buttons.add(openStandaloneConfButton);
        buttons.add(openAppButton);

        eapHint.setAlignmentX(LEFT_ALIGNMENT);
        card.add(eapHint);
        card.add(Box.createVerticalStrut(8));
        card.add(buttons);
        return card;
    }

    private JPanel warCard() {
        JPanel card = titledCard("WAR のビルドと切り替え");
        selectedLabel.setFont(selectedLabel.getFont().deriveFont(14f));
        selectedLabel.setAlignmentX(LEFT_ALIGNMENT);
        lastDeployedLabel.setAlignmentX(LEFT_ALIGNMENT);
        appUrlLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(selectedLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(lastDeployedLabel);
        card.add(Box.createVerticalStrut(2));
        card.add(appUrlLabel);
        card.add(Box.createVerticalStrut(8));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setAlignmentX(LEFT_ALIGNMENT);
        buttons.add(buildButton);
        buttons.add(deployButton);
        buttons.add(buildDeployButton);
        buttons.add(openProjectButton);
        card.add(buttons);
        return card;
    }

    private JPanel envCard() {
        JPanel card = titledCard("環境変数");
        JLabel hint = new JLabel("共通は EAP 起動とビルドの両方に、プロジェクトは選択中 WAR のビルドと起動に適用されます。");
        JTabbedPane tabs = new JTabbedPane();
        tabs.setAlignmentX(LEFT_ALIGNMENT);
        tabs.setPreferredSize(new Dimension(720, 250));
        tabs.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        tabs.addTab("共通", globalEnv);
        tabs.addTab("選択プロジェクト", projectEnv);
        hint.setAlignmentX(LEFT_ALIGNMENT);
        card.add(hint);
        card.add(Box.createVerticalStrut(8));
        card.add(tabs);
        return card;
    }

    private static JPanel titledCard(String title, JComponent... trailing) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        card.setBorder(UiUtils.cardBorder());
        JLabel label = new JLabel(title);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 15f));
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        header.setAlignmentX(LEFT_ALIGNMENT);
        header.add(label);
        for (JComponent component : trailing) {
            header.add(component);
        }
        card.add(header);
        card.add(Box.createVerticalStrut(8));
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
            setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        }
        if (statusDot != null) {
            statusDot.setForeground(UIManager.getColor("Label.foreground"));
        }
    }
}
