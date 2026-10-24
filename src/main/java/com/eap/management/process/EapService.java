package com.eap.management.process;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.eap.management.config.EapSettings;

public class EapService {
    private static final Duration STOP_WAIT = Duration.ofSeconds(10);
    private static final Duration PROCESS_KILL_WAIT = Duration.ofSeconds(3);

    private final ProcessService processes;
    private final AtomicReference<Process> process = new AtomicReference<>();
    private final AtomicReference<EapStatus> status = new AtomicReference<>(EapStatus.STOPPED);
    private final AtomicBoolean startRequested = new AtomicBoolean();

    public EapService(ProcessService processes) {
        this.processes = processes;
    }

    public EapStatus status(EapSettings settings) {
        if (!settings.looksInstalled()) {
            status.set(EapStatus.UNCONFIGURED);
            return EapStatus.UNCONFIGURED;
        }
        Process running = process.get();
        boolean processAlive = running != null && running.isAlive();
        EapStatus next = nextStatus(status.get(), isManagementReachable(settings), processAlive, startRequested.get());
        status.set(next);
        if (next == EapStatus.RUNNING || next == EapStatus.STOPPED || next == EapStatus.STOPPING) {
            startRequested.set(false);
        }
        if (next == EapStatus.STOPPED && running != null && !processAlive) {
            process.compareAndSet(running, null);
        }
        return next;
    }

    static EapStatus nextStatus(EapStatus current, boolean reachable, boolean processAlive, boolean startRequested) {
        return switch (current) {
            case UNCONFIGURED -> EapStatus.UNCONFIGURED;
            case STOPPING -> reachable ? EapStatus.STOPPING : EapStatus.STOPPED;
            case STARTING -> reachable
                    ? EapStatus.RUNNING
                    : (processAlive || startRequested ? EapStatus.STARTING : EapStatus.STOPPED);
            case RESTARTING -> reachable ? EapStatus.RUNNING : EapStatus.RESTARTING;
            default -> reachable ? EapStatus.RUNNING : EapStatus.STOPPED;
        };
    }

    public boolean isManagementReachable(EapSettings settings) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(settings.managementHost(), settings.getManagementPort()), 350);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void markStopping() {
        startRequested.set(false);
        status.set(EapStatus.STOPPING);
    }

    public void markStarting() {
        startRequested.set(true);
        status.set(EapStatus.STARTING);
    }

    public void start(EapSettings settings, Map<String, String> env, LogListener log) throws IOException {
        markStarting();
        if (isManagementReachable(settings)) {
            startRequested.set(false);
            status.set(EapStatus.RUNNING);
            log.append("EAP", "すでに起動しています");
            return;
        }
        Process leftover = process.get();
        if (leftover != null && leftover.isAlive()) {
            leftover.destroyForcibly();
            process.set(null);
        }
        Path script = settings.standaloneScript();
        if (script == null || !Files.isRegularFile(script)) {
            startRequested.set(false);
            status.set(EapStatus.STOPPED);
            throw new IOException("EAP の standalone スクリプトが見つかりません。EAP ホームを確認してください。");
        }
        List<String> args = new ArrayList<>();
        args.add("-c");
        args.add(settings.getConfigFile());
        args.add("-b");
        args.add(settings.getBindAddress());
        args.add("-bmanagement");
        args.add(settings.managementHost());
        args.add("-Djboss.http.port=" + settings.getHttpPort());
        args.add("-Djboss.management.http.port=" + settings.getManagementPort());
        args.addAll(tokenize(settings.getExtraArgs()));

        log.append("EAP", "起動します: " + script + " " + String.join(" ", args));
        try {
            Process started = processes.start(Os.script(script, args), settings.homePath(), env, "EAP", (source, line) -> {
                log.append(source, line);
                if (looksStarted(line)) {
                    startRequested.set(false);
                    status.set(EapStatus.RUNNING);
                }
                if (looksFailed(line)) {
                    startRequested.set(false);
                    status.compareAndSet(EapStatus.STARTING, EapStatus.STOPPED);
                }
            });
            process.set(started);
        } catch (IOException e) {
            startRequested.set(false);
            status.set(EapStatus.STOPPED);
            throw e;
        }
    }

    public void stop(EapSettings settings, Map<String, String> env, LogListener log)
            throws IOException, InterruptedException {
        if (!isManagementReachable(settings) && (process.get() == null || !process.get().isAlive())) {
            status.set(EapStatus.STOPPED);
            log.append("EAP", "すでに停止しています");
            return;
        }
        status.set(EapStatus.STOPPING);
        Path cli = settings.cliScript();
        if (cli == null || !Files.isRegularFile(cli)) {
            throw new IOException("jboss-cli が見つかりません。EAP ホームを確認してください。");
        }
        String controller = settings.managementHost() + ":" + settings.getManagementPort();
        List<String> args = List.of(
                "--connect",
                "--controller=" + controller,
                "--command=:shutdown"
        );
        log.append("EAP", "停止します: jboss-cli --controller=" + controller);
        Process cliProcess = processes.start(Os.script(cli, args), settings.homePath(), env, "EAP", log);
        boolean down = waitUntilStopped(settings, log, STOP_WAIT);
        if (!down) {
            log.append("EAP", STOP_WAIT.toSeconds() + " 秒以内に停止しなかったためプロセスを終了します");
            forceStopOwnedProcess();
            if (cliProcess.isAlive()) {
                cliProcess.destroyForcibly();
            }
            down = waitUntilStopped(settings, log, PROCESS_KILL_WAIT);
        }
        Process running = process.get();
        if (running != null && !running.isAlive()) {
            process.set(null);
        }
        if (down || !isManagementReachable(settings)) {
            status.set(EapStatus.STOPPED);
            return;
        }
        throw new IOException("停止がタイムアウトしました");
    }

    private void forceStopOwnedProcess() throws InterruptedException {
        Process running = process.get();
        if (running == null || !running.isAlive()) {
            return;
        }
        running.destroy();
        if (!running.waitFor(PROCESS_KILL_WAIT.toSeconds(), java.util.concurrent.TimeUnit.SECONDS)) {
            running.destroyForcibly();
        }
    }

    public void restart(EapSettings settings, Map<String, String> env, LogListener log)
            throws IOException, InterruptedException {
        status.set(EapStatus.RESTARTING);
        try {
            boolean wasUp = isManagementReachable(settings)
                    || (process.get() != null && process.get().isAlive());
            if (wasUp) {
                log.append("EAP", "再起動します（停止してから起動）");
                stop(settings, env, log);
                waitUntilStopped(settings, log, PROCESS_KILL_WAIT);
                Thread.sleep(300);
            } else {
                log.append("EAP", "停止しているため再起動ではなく起動します");
            }
            status.set(EapStatus.RESTARTING);
            start(settings, env, log);
        } catch (IOException | InterruptedException e) {
            if (status.get() == EapStatus.RESTARTING) {
                status.set(isManagementReachable(settings) ? EapStatus.RUNNING : EapStatus.STOPPED);
            }
            throw e;
        }
    }

    private boolean waitUntilStopped(EapSettings settings, LogListener log, Duration timeout)
            throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (!isManagementReachable(settings)) {
                log.append("EAP", "停止を確認しました");
                return true;
            }
            Thread.sleep(200);
        }
        return false;
    }

    private static boolean looksStarted(String line) {
        return line.contains("WFLYSRV0025")
                || line.contains("JBAS015874")
                || line.contains("started in");
    }

    private static boolean looksFailed(String line) {
        return line.contains("WFLYSRV0056")
                || line.contains("Address already in use")
                || line.contains("サーバーは起動に失敗");
    }

    static List<String> tokenize(String extraArgs) {
        List<String> tokens = new ArrayList<>();
        if (extraArgs == null || extraArgs.isBlank()) {
            return tokens;
        }
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < extraArgs.length(); i++) {
            char c = extraArgs.charAt(i);
            if (c == '"') {
                quoted = !quoted;
                continue;
            }
            if (!quoted && Character.isWhitespace(c)) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            current.append(c);
        }
        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }
        return tokens;
    }
}
