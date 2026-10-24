package com.eap.management.process;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

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
        if (isWindows() && opensAsExecutable(path)) {
            if (openWithTxtAssociation(absolute, errors)) {
                return;
            }
            if (start(List.of("notepad.exe", absolute), errors)) {
                return;
            }
        } else if (isWindows()) {
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

    static boolean openWithTxtAssociation(String absolute, List<IOException> errors) {
        try {
            String command = windowsTxtOpenCommand(absolute);
            if (command == null || command.isBlank()) {
                return false;
            }
            List<String> tokens = tokenizeQuoted(command);
            if (tokens.isEmpty()) {
                return false;
            }
            return start(tokens, errors);
        } catch (Exception e) {
            errors.add(new IOException(".txt の関連付けを解決できませんでした", e));
            return false;
        }
    }

    static String windowsTxtOpenCommand(String file) {
        String progId = regQuery(
                "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\FileExts\\.txt\\UserChoice",
                "ProgId");
        if (progId == null || progId.isBlank()) {
            progId = regQuery("HKCR\\.txt", null);
        }
        if (progId == null || progId.isBlank()) {
            return null;
        }
        String template = regQuery("HKCR\\" + progId + "\\shell\\open\\command", null);
        if (template == null || template.isBlank()) {
            template = regQuery("HKCU\\Software\\Classes\\" + progId + "\\shell\\open\\command", null);
        }
        if (template == null || template.isBlank()) {
            return null;
        }
        return expandCommandTemplate(template, file);
    }

    static List<String> tokenizeQuoted(String command) {
        List<String> tokens = new ArrayList<>();
        if (command == null || command.isBlank()) {
            return tokens;
        }
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);
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

    static String expandCommandTemplate(String template, String file) {
        String quoted = '"' + file + '"';
        return template
                .replace("%~1", quoted)
                .replace("\"%1\"", quoted)
                .replace("%1", quoted)
                .replace("\"%L\"", quoted)
                .replace("%L", quoted)
                .replace("%*", quoted);
    }

    static String parseRegSz(String output) {
        if (output == null) {
            return null;
        }
        for (String line : output.split("\\R")) {
            int index = line.toUpperCase(Locale.ROOT).indexOf("REG_SZ");
            if (index >= 0) {
                String value = line.substring(index + 6).trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return null;
    }

    private static String regQuery(String key, String valueName) {
        List<String> command = new ArrayList<>();
        command.add("reg");
        command.add("query");
        command.add(key);
        if (valueName == null) {
            command.add("/ve");
        } else {
            command.add("/v");
            command.add(valueName);
        }
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            byte[] bytes = process.getInputStream().readAllBytes();
            if (!process.waitFor(3, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return null;
            }
            Charset charset = isWindows() ? Charset.forName("MS932") : StandardCharsets.UTF_8;
            return parseRegSz(new String(bytes, charset));
        } catch (Exception e) {
            return null;
        }
    }

    static boolean opensAsExecutable(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".bat")
                || name.endsWith(".cmd")
                || name.endsWith(".ps1")
                || name.endsWith(".exe")
                || name.endsWith(".com")
                || name.endsWith(".vbs");
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
