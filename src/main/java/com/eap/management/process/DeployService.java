package com.eap.management.process;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.eap.management.config.AppConfig;
import com.eap.management.config.DeployMode;
import com.eap.management.config.EapSettings;
import com.eap.management.config.WarProject;

public class DeployService {
    private static final String[] MARKERS = {
            ".deployed", ".failed", ".dodeploy", ".pending", ".isdeploying", ".undeployed"
    };

    private final ProcessService processes;
    private final AtomicBoolean running = new AtomicBoolean();

    public DeployService(ProcessService processes) {
        this.processes = processes;
    }

    public boolean isRunning() {
        return running.get();
    }

    public Path switchAndDeploy(AppConfig config, WarProject selected, Map<String, String> env, LogListener log)
            throws IOException, InterruptedException {
        if (!running.compareAndSet(false, true)) {
            throw new IOException("別のデプロイが実行中です");
        }
        try {
            Path war = selected.resolveWarFile().orElseThrow(() ->
                    new IOException("デプロイする WAR が見つかりません。先にビルドしてください: " + selected.getWarPath()));
            String deployName = selected.resolvedDeployName(war);
            EapSettings eap = config.getEap();
            Set<String> others = managedDeployNames(config, selected.getId());

            log.append("DEPLOY", "デプロイ対象を切り替えます: " + selected.getName() + " -> " + deployName);
            undeployAll(eap, others, env, log);
            deploy(eap, war, deployName, env, log);
            config.setLastDeployedProjectId(selected.getId());
            return war;
        } finally {
            running.set(false);
        }
    }

    private void undeployAll(EapSettings eap, Set<String> names, Map<String, String> env, LogListener log)
            throws IOException, InterruptedException {
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            log.append("DEPLOY", "アンデプロイ: " + name);
            if (eap.getDeployMode() == DeployMode.CLI) {
                runCli(eap, "undeploy " + name, env, log, false);
            } else {
                removeFromDeployments(eap, name, log);
            }
        }
    }

    private void deploy(EapSettings eap, Path war, String deployName, Map<String, String> env, LogListener log)
            throws IOException, InterruptedException {
        if (eap.getDeployMode() == DeployMode.CLI) {
            if (!new EapService(processes).isManagementReachable(eap)) {
                throw new IOException("EAP が起動していないため CLI デプロイできません。先に起動するか、デプロイ方式をディレクトリにしてください。");
            }
            String command = "deploy \"" + war.toAbsolutePath() + "\" --name=" + deployName + " --force";
            CommandResult result = runCli(eap, command, env, log, true);
            if (!result.succeeded()) {
                throw new IOException("CLI デプロイに失敗しました");
            }
            return;
        }
        Path deployments = requireDeployments(eap);
        Path dest = deployments.resolve(deployName);
        removeFromDeployments(eap, deployName, log);
        retryCopy(war, dest, log);
        log.append("DEPLOY", "コピーしました: " + dest.toAbsolutePath());
    }

    private CommandResult runCli(EapSettings eap, String command, Map<String, String> env, LogListener log,
                                 boolean failOnError) throws IOException, InterruptedException {
        Path cli = eap.cliScript();
        if (cli == null || !Files.isRegularFile(cli)) {
            throw new IOException("jboss-cli が見つかりません");
        }
        String controller = eap.managementHost() + ":" + eap.getManagementPort();
        List<String> args = List.of("--connect", "--controller=" + controller, "--command=" + command);
        CommandResult result = processes.run(Os.script(cli, args), eap.homePath(), env, "DEPLOY", log,
                Duration.ofSeconds(60));
        if (!result.succeeded() && failOnError) {
            return result;
        }
        if (!result.succeeded()) {
            log.append("DEPLOY", "CLI コマンドをスキップまたは失敗として扱います: " + command);
        }
        return result;
    }

    private void removeFromDeployments(EapSettings eap, String deployName, LogListener log) throws IOException {
        Path deployments = eap.deploymentsDir();
        if (deployments == null || !Files.isDirectory(deployments)) {
            return;
        }
        Path artifact = deployments.resolve(deployName);
        if (Files.isDirectory(artifact)) {
            deleteRecursive(artifact);
        } else {
            Files.deleteIfExists(artifact);
        }
        for (String marker : MARKERS) {
            Files.deleteIfExists(deployments.resolve(deployName + marker));
        }
        log.append("DEPLOY", "デプロイメントから削除: " + deployName);
    }

    private static Path requireDeployments(EapSettings eap) throws IOException {
        Path deployments = eap.deploymentsDir();
        if (deployments == null) {
            throw new IOException("EAP ホームが未設定です");
        }
        if (!Files.isDirectory(deployments)) {
            throw new IOException("deployments ディレクトリがありません: " + deployments);
        }
        return deployments;
    }

    private static void retryCopy(Path war, Path dest, LogListener log) throws IOException {
        IOException last = null;
        for (int i = 0; i < 4; i++) {
            try {
                Files.copy(war, dest, StandardCopyOption.REPLACE_EXISTING);
                return;
            } catch (IOException e) {
                last = e;
                log.append("DEPLOY", "コピー再試行 (" + (i + 1) + "): " + e.getMessage());
                try {
                    Thread.sleep(400L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
        throw last;
    }

    private static void deleteRecursive(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path child : stream) {
                if (Files.isDirectory(child)) {
                    deleteRecursive(child);
                } else {
                    Files.deleteIfExists(child);
                }
            }
        }
        Files.deleteIfExists(dir);
    }

    static Set<String> managedDeployNames(AppConfig config, String keepId) {
        Set<String> names = new LinkedHashSet<>();
        List<WarProject> projects = new ArrayList<>(config.getProjects());
        config.findProject(config.getLastDeployedProjectId()).ifPresent(projects::add);
        for (WarProject project : projects) {
            if (project.getId().equals(keepId)) {
                continue;
            }
            try {
                project.resolveWarFile().ifPresent(war -> names.add(project.resolvedDeployName(war)));
            } catch (IOException ignored) {
                // skip unresolved
            }
            if (project.getDeployName() != null && !project.getDeployName().isBlank()) {
                String value = project.getDeployName().trim();
                names.add(value.endsWith(".war") ? value : value + ".war");
            }
        }
        return names;
    }
}
