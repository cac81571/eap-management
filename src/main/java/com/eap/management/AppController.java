package com.eap.management;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.eap.management.config.AppConfig;
import com.eap.management.config.ConfigStore;
import com.eap.management.config.EnvVar;
import com.eap.management.config.WarProject;
import com.eap.management.process.BuildService;
import com.eap.management.process.DeployService;
import com.eap.management.process.EapService;
import com.eap.management.process.EapStatus;
import com.eap.management.process.Os;
import com.eap.management.process.ProcessService;
import com.eap.management.ui.EapSettingsDialog;
import com.eap.management.ui.MainFrame;
import com.eap.management.ui.ProjectEditDialog;
import com.eap.management.ui.Theme;
import com.eap.management.ui.UiUtils;

public class AppController {
    private final ConfigStore store;
    private final AppConfig config;
    private final EapService eapService;
    private final BuildService buildService;
    private final DeployService deployService;
    private final ExecutorService workers = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "eap-manager-worker");
        thread.setDaemon(true);
        return thread;
    });
    private final Timer poller;

    private MainFrame frame;
    private boolean jobBusy;

    public AppController(ConfigStore store, AppConfig config) {
        this.store = store;
        this.config = config;
        ProcessService processes = new ProcessService(() -> config.getEap().getConsoleEncoding());
        this.eapService = new EapService(processes);
        this.buildService = new BuildService(processes);
        this.deployService = new DeployService(processes);
        this.poller = new Timer(1500, e -> refresh());
    }

    public void bind(MainFrame frame) {
        this.frame = frame;
        frame.projects().setListener(new com.eap.management.ui.ProjectListPanel.Listener() {
            @Override
            public void onSelectionChanged(WarProject project) {
                if (project != null) {
                    config.setSelectedProjectId(project.getId());
                    persistQuietly();
                }
                refresh();
            }

            @Override
            public void onAdd() {
                addProject();
            }

            @Override
            public void onEdit(WarProject project) {
                editProject(project);
            }

            @Override
            public void onRemove(WarProject project) {
                removeProject(project);
            }
        });
        frame.controls().setListener(new com.eap.management.ui.ControlPanel.Listener() {
            @Override
            public void onStart() {
                eapService.markStarting();
                runJob("EAP 起動", () -> eapService.start(config.getEap(), envForSelected(), frame.log()));
            }

            @Override
            public void onStop() {
                eapService.markStopping();
                runJob("EAP 停止", () -> eapService.stop(config.getEap(), envForSelected(), frame.log()));
            }

            @Override
            public void onRestart() {
                runJob("EAP 再起動", () -> eapService.restart(config.getEap(), envForSelected(), frame.log()));
            }

            @Override
            public void onBuild() {
                WarProject project = requireProject();
                if (project == null) {
                    return;
                }
                runJob("ビルド", () -> buildService.build(project, envFor(project), frame.log()));
            }

            @Override
            public void onDeploy() {
                WarProject project = requireProject();
                if (project == null) {
                    return;
                }
                runJob("デプロイ", () -> deployService.switchAndDeploy(config, project, envFor(project), frame.log()));
            }

            @Override
            public void onBuildAndDeploy() {
                WarProject project = requireProject();
                if (project == null) {
                    return;
                }
                runJob("ビルドしてデプロイ", () -> {
                    buildService.build(project, envFor(project), frame.log());
                    deployService.switchAndDeploy(config, project, envFor(project), frame.log());
                });
            }

            @Override
            public void onOpenProject() {
                openSelectedProject();
            }

            @Override
            public void onOpenDeployments() {
                openDeployments();
            }

            @Override
            public void onOpenConfig() {
                openConfigFile();
            }

            @Override
            public void onOpenStandaloneConf() {
                openStandaloneConf();
            }

            @Override
            public void onOpenApp() {
                openApp();
            }
        });
        frame.controls().globalEnv().setOnChange(this::persistQuietly);
        frame.controls().projectEnv().setOnChange(this::persistQuietly);
        reloadLists();
        refresh();
        poller.start();
        frame.log().append("APP", "設定ファイル: " + store.file());
        if (!config.getEap().looksInstalled()) {
            frame.log().append("APP", "EAP ホームを設定してください。右上の「EAP 設定」から指定できます。");
        }
    }

    public void toggleTheme(boolean dark) {
        config.getUi().setDarkTheme(dark);
        Theme.apply(dark);
        persistQuietly();
    }

    public void editEapSettings() {
        EapSettingsDialog dialog = new EapSettingsDialog(frame, config.getEap());
        dialog.setVisible(true);
        if (dialog.isAccepted()) {
            dialog.applyTo(config.getEap());
            persistQuietly();
            refresh();
            frame.log().append("APP", "EAP 設定を保存しました: " + config.getEap().getHome());
        }
    }

    public void onClose() {
        poller.stop();
        config.getUi().setWindowWidth(frame.getWidth());
        config.getUi().setWindowHeight(frame.getHeight());
        persistQuietly();
        workers.shutdownNow();
    }

    private void addProject() {
        WarProject project = new WarProject();
        ProjectEditDialog dialog = new ProjectEditDialog(frame, project);
        dialog.setVisible(true);
        if (!dialog.isAccepted()) {
            return;
        }
        dialog.applyTo(project);
        config.getProjects().add(project);
        config.setSelectedProjectId(project.getId());
        persistQuietly();
        reloadLists();
        refresh();
    }

    private void editProject(WarProject project) {
        ProjectEditDialog dialog = new ProjectEditDialog(frame, project);
        dialog.setVisible(true);
        if (!dialog.isAccepted()) {
            return;
        }
        dialog.applyTo(project);
        persistQuietly();
        reloadLists();
        refresh();
    }

    private void removeProject(WarProject project) {
        if (!UiUtils.confirm(frame, project.getName() + " を削除しますか？")) {
            return;
        }
        config.getProjects().remove(project);
        if (project.getId().equals(config.getSelectedProjectId())) {
            config.setSelectedProjectId(config.getProjects().isEmpty() ? "" : config.getProjects().getFirst().getId());
        }
        persistQuietly();
        reloadLists();
        refresh();
    }

    private void openSelectedProject() {
        WarProject project = requireProject();
        if (project == null) {
            return;
        }
        try {
            Os.openFolder(project.projectPath());
        } catch (IOException e) {
            UiUtils.error(frame, "フォルダを開けませんでした。", e);
        }
    }

    private void openDeployments() {
        try {
            Path dir = config.getEap().deploymentsDir();
            if (dir == null) {
                throw new IOException("EAP ホームが未設定です");
            }
            Os.openFolder(dir);
        } catch (IOException e) {
            UiUtils.error(frame, "deployments を開けませんでした。", e);
        }
    }

    private void openConfigFile() {
        Path file = config.getEap().configFilePath();
        frame.log().append("APP", "設定ファイルを開きます: " + file);
        try {
            Os.openFile(file);
        } catch (IOException e) {
            UiUtils.error(frame, "設定ファイルを開けませんでした。", e);
        }
    }

    private void openStandaloneConf() {
        Path file = config.getEap().standaloneConfPath();
        frame.log().append("APP", "standalone.conf を開きます: " + file);
        try {
            Os.openFile(file);
        } catch (IOException e) {
            UiUtils.error(frame, "standalone.conf を開けませんでした。", e);
        }
    }

    private void openApp() {
        WarProject project = config.selectedProject().orElse(null);
        String url = project == null
                ? config.getEap().httpBaseUrl()
                : project.resolveAppUrl(config.getEap().httpBaseUrl());
        frame.log().append("APP", "ブラウザを開きます: " + url);
        Thread thread = new Thread(() -> {
            try {
                Os.browse(url);
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> UiUtils.error(frame, "ブラウザを開けませんでした: " + url, e));
            }
        }, "open-browser");
        thread.setDaemon(true);
        thread.start();
    }

    private WarProject requireProject() {
        Optional<WarProject> selected = config.selectedProject();
        if (selected.isEmpty()) {
            UiUtils.error(frame, "先に WAR プロジェクトを追加・選択してください。");
            return null;
        }
        return selected.get();
    }

    private Map<String, String> envForSelected() {
        return envFor(config.selectedProject().orElse(null));
    }

    private Map<String, String> envFor(WarProject project) {
        return EnvVar.merge(config.getGlobalEnv(), project == null ? null : project.getEnv());
    }

    private void runJob(String title, Job job) {
        if (jobBusy) {
            return;
        }
        jobBusy = true;
        refresh();
        frame.log().append("APP", title + " を開始します");
        workers.submit(() -> {
            try {
                job.run();
                SwingUtilities.invokeLater(() -> {
                    frame.log().append("APP", title + " が完了しました");
                    persistQuietly();
                    reloadLists();
                    finishJob();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    frame.log().append("APP", title + " に失敗しました: " + e.getMessage());
                    UiUtils.error(frame, title + " に失敗しました。", e);
                    finishJob();
                });
            }
        });
    }

    private void finishJob() {
        jobBusy = false;
        refresh();
    }

    private void reloadLists() {
        String lastName = config.findProject(config.getLastDeployedProjectId())
                .map(WarProject::getName)
                .orElse("");
        frame.projects().setProjects(config.getProjects(), config.getSelectedProjectId(), config.getLastDeployedProjectId());
        updateSelectedProject(lastName);
        frame.controls().globalEnv().setVars(config.getGlobalEnv());
        frame.controls().projectEnv().setVars(config.selectedProject()
                .map(WarProject::getEnv)
                .orElseGet(java.util.ArrayList::new));
    }

    private void refresh() {
        if (frame == null) {
            return;
        }
        EapStatus status = eapService.status(config.getEap());
        boolean configured = config.getEap().looksInstalled();
        String hint;
        if (!configured) {
            hint = "EAP ホームを設定してください";
        } else if (status == EapStatus.RUNNING) {
            hint = "管理 " + config.getEap().managementHost() + ":" + config.getEap().getManagementPort()
                    + " / HTTP " + config.getEap().httpBaseUrl();
        } else {
            hint = config.getEap().getHome();
        }
        frame.controls().setStatus(status, hint);
        frame.controls().setBusy(status, jobBusy, config.selectedProject().isPresent(), configured);
        String lastName = config.findProject(config.getLastDeployedProjectId())
                .map(WarProject::getName)
                .orElse("");
        updateSelectedProject(lastName);
    }

    private void updateSelectedProject(String lastName) {
        WarProject selected = config.selectedProject().orElse(null);
        String appUrl = selected == null ? "" : selected.resolveAppUrl(config.getEap().httpBaseUrl());
        frame.controls().setSelectedProject(selected, lastName, appUrl);
    }

    private void persistQuietly() {
        try {
            store.save(config);
        } catch (RuntimeException e) {
            if (frame != null) {
                frame.log().append("APP", "設定の保存に失敗しました: " + e.getMessage());
            }
        }
    }

    @FunctionalInterface
    private interface Job {
        void run() throws Exception;
    }
}
