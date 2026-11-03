package com.eap.management.process;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.eap.management.config.EapSettings;

public class ManagementUserService {
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private final ProcessService processes;

    public ManagementUserService(ProcessService processes) {
        this.processes = processes;
    }

    public CommandResult create(EapSettings eap, LogListener log) throws IOException, InterruptedException {
        if (eap == null || eap.homePath() == null) {
            throw new IOException("EAP ホームが未設定です");
        }
        if (eap.getManagementUser().isBlank()) {
            throw new IOException("管理ユーザを入力してください");
        }
        if (eap.getManagementPassword().isEmpty()) {
            throw new IOException("管理パスワードを入力してください");
        }
        Path script = eap.addUserScript();
        if (script == null || !Files.isRegularFile(script)) {
            throw new IOException("add-user が見つかりません。EAP ホームを確認してください。");
        }
        List<String> args = createArgs(eap.getManagementUser(), eap.getManagementPassword());
        log.append("APP", "管理ユーザを作成します: " + eap.getManagementUser() + " (ManagementRealm)");
        Map<String, String> env = new LinkedHashMap<>();
        if (Os.isWindows()) {
            env.put("NOPAUSE", "true");
        }
        CommandResult result = processes.run(Os.script(script, args), eap.homePath(), env, "APP", log, TIMEOUT);
        if (result.succeeded()) {
            log.append("APP", "管理ユーザを作成（または更新）しました: " + eap.getManagementUser());
        } else {
            log.append("APP", "管理ユーザの作成に失敗しました (exit=" + result.exitCode() + ")");
        }
        return result;
    }

    static List<String> createArgs(String user, String password) {
        List<String> args = new ArrayList<>();
        args.add("-u");
        args.add(user);
        args.add("-p");
        args.add(password);
        args.add("-r");
        args.add("ManagementRealm");
        args.add("-s");
        args.add("-cw");
        return args;
    }
}
