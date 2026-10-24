package com.eap.management.process;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.eap.management.config.WarProject;

public class BuildService {
    private final ProcessService processes;
    private final AtomicBoolean running = new AtomicBoolean();

    public BuildService(ProcessService processes) {
        this.processes = processes;
    }

    public boolean isRunning() {
        return running.get();
    }

    public void build(WarProject project, Map<String, String> env, LogListener log)
            throws IOException, InterruptedException {
        if (!running.compareAndSet(false, true)) {
            throw new IOException("別のビルドが実行中です");
        }
        try {
            Path dir = project.projectPath();
            if (dir == null || !Files.isDirectory(dir)) {
                throw new IOException("プロジェクトディレクトリがありません: " + project.getProjectDir());
            }
            log.append("BUILD", project.getName() + " をビルドします");
            log.append("BUILD", "作業ディレクトリ: " + dir.toAbsolutePath());
            log.append("BUILD", "コマンド: " + project.getBuildCommand());
            CommandResult result = processes.runShell(
                    project.getBuildCommand(),
                    dir,
                    env,
                    "BUILD",
                    log,
                    Duration.ofMinutes(20)
            );
            if (!result.succeeded()) {
                throw new IOException(result.timedOut()
                        ? "ビルドがタイムアウトしました"
                        : "ビルドが失敗しました (exit=" + result.exitCode() + ")");
            }
            Path war = project.resolveWarFile().orElseThrow(() ->
                    new IOException("WAR が見つかりません: " + project.getWarPath()));
            log.append("BUILD", "成果物: " + war.toAbsolutePath());
        } finally {
            running.set(false);
        }
    }
}
