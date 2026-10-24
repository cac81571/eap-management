package com.eap.management.process;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Os {
    private Os() {
    }

    public static boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return os.contains("win");
    }

    public static List<String> shell(String command) {
        List<String> cmd = new ArrayList<>();
        if (isWindows()) {
            cmd.add("cmd.exe");
            cmd.add("/c");
            cmd.add(command);
        } else {
            cmd.add("bash");
            cmd.add("-lc");
            cmd.add(command);
        }
        return cmd;
    }

    public static List<String> script(Path script, List<String> args) {
        List<String> cmd = new ArrayList<>();
        if (isWindows()) {
            cmd.add("cmd.exe");
            cmd.add("/c");
        }
        cmd.add(script.toAbsolutePath().toString());
        if (args != null) {
            cmd.addAll(args);
        }
        return cmd;
    }

    public static void openFolder(Path path) throws IOException {
        Path target = path;
        if (target == null) {
            throw new IOException("パスが空です");
        }
        if (!java.nio.file.Files.exists(target)) {
            throw new IOException("存在しません: " + target);
        }
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            Desktop.getDesktop().open(target.toFile());
            return;
        }
        if (isWindows()) {
            new ProcessBuilder("explorer.exe", target.toAbsolutePath().toString()).start();
        } else {
            new ProcessBuilder("xdg-open", target.toAbsolutePath().toString()).start();
        }
    }

    public static void openFile(Path path) throws IOException {
        if (path == null) {
            throw new IOException("パスが空です");
        }
        if (!java.nio.file.Files.isRegularFile(path)) {
            throw new IOException("ファイルがありません: " + path.toAbsolutePath());
        }
        String absolute = path.toAbsolutePath().toString();
        List<IOException> errors = new ArrayList<>();
        if (isWindows()) {
            if (start(List.of("cmd.exe", "/c", "start", "", absolute), errors)) {
                return;
            }
            if (start(List.of("explorer.exe", absolute), errors)) {
                return;
            }
            if (start(List.of("notepad.exe", absolute), errors)) {
                return;
            }
        }
        try {
            Desktop desktop = Desktop.getDesktop();
            if (Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.EDIT)) {
                desktop.edit(path.toFile());
                return;
            }
            if (Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.OPEN)) {
                desktop.open(path.toFile());
                return;
            }
        } catch (Exception e) {
            errors.add(new IOException("Desktop で開けませんでした", e));
        }
        if (!isWindows() && start(List.of("xdg-open", absolute), errors)) {
            return;
        }
        IOException failed = new IOException("ファイルを開けませんでした: " + absolute);
        errors.forEach(failed::addSuppressed);
        throw failed;
    }

    public static void browse(String url) throws IOException {
        if (url == null || url.isBlank()) {
            throw new IOException("URL が空です");
        }
        List<IOException> errors = new ArrayList<>();
        if (isWindows()) {
            if (start(List.of("cmd.exe", "/c", "start", "", url), errors)) {
                return;
            }
            if (start(List.of("rundll32.exe", "url.dll,FileProtocolHandler", url), errors)) {
                return;
            }
            if (start(List.of("explorer.exe", url), errors)) {
                return;
            }
        }
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                return;
            }
        } catch (Exception e) {
            errors.add(new IOException("Desktop.browse に失敗しました", e));
        }
        if (!isWindows() && start(List.of("xdg-open", url), errors)) {
            return;
        }
        IOException failed = new IOException("ブラウザを開けませんでした: " + url);
        errors.forEach(failed::addSuppressed);
        throw failed;
    }

    private static boolean start(List<String> command, List<IOException> errors) {
        try {
            new ProcessBuilder(command).start();
            return true;
        } catch (IOException e) {
            errors.add(e);
            return false;
        }
    }
}
