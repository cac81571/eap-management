package com.eap.management.process;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class ProcessService {
    private final Supplier<String> encoding;

    public ProcessService() {
        this(() -> ConsoleEncoding.AUTO);
    }

    public ProcessService(Supplier<String> encoding) {
        this.encoding = encoding == null ? () -> ConsoleEncoding.AUTO : encoding;
    }

    public Process start(List<String> command, Path workDir, Map<String, String> extraEnv, String source,
                         LogListener log) throws IOException {
        ProcessBuilder builder = newBuilder(command, workDir, extraEnv);
        Process process = builder.start();
        consume(process, source, log);
        return process;
    }

    public CommandResult run(List<String> command, Path workDir, Map<String, String> extraEnv, String source,
                             LogListener log, Duration timeout) throws IOException, InterruptedException {
        Process process = start(command, workDir, extraEnv, source, log);
        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            log.append(source, "タイムアウトしたためプロセスを停止しました (" + timeout.toSeconds() + " 秒)");
            return new CommandResult(-1, true);
        }
        return new CommandResult(process.exitValue(), false);
    }

    public CommandResult runShell(String command, Path workDir, Map<String, String> extraEnv, String source,
                                  LogListener log, Duration timeout) throws IOException, InterruptedException {
        return run(Os.shell(command), workDir, extraEnv, source, log, timeout);
    }

    private static ProcessBuilder newBuilder(List<String> command, Path workDir, Map<String, String> extraEnv) {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        if (workDir != null) {
            builder.directory(workDir.toFile());
        }
        if (extraEnv != null && !extraEnv.isEmpty()) {
            builder.environment().putAll(extraEnv);
        }
        return builder;
    }

    private void consume(Process process, String source, LogListener log) {
        Thread thread = new Thread(() -> {
            try (InputStream in = process.getInputStream()) {
                ByteArrayOutputStream line = new ByteArrayOutputStream();
                int read;
                while ((read = in.read()) != -1) {
                    if (read == '\n') {
                        emit(source, log, line);
                    } else {
                        line.write(read);
                    }
                }
                if (line.size() > 0) {
                    emit(source, log, line);
                }
            } catch (IOException ignored) {
                // process closed
            }
        }, "eap-log-" + source);
        thread.setDaemon(true);
        thread.start();
    }

    private void emit(String source, LogListener log, ByteArrayOutputStream line) {
        byte[] bytes = stripCr(line.toByteArray());
        line.reset();
        log.append(source, ConsoleEncoding.decode(bytes, encoding.get()));
    }

    private static byte[] stripCr(byte[] bytes) {
        if (bytes.length > 0 && bytes[bytes.length - 1] == '\r') {
            byte[] trimmed = new byte[bytes.length - 1];
            System.arraycopy(bytes, 0, trimmed, 0, trimmed.length);
            return trimmed;
        }
        return bytes;
    }
}
