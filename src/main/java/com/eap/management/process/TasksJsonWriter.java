package com.eap.management.process;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.eap.management.config.AppConfig;
import com.eap.management.config.WarProject;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class TasksJsonWriter {
    public static final String VERSION = "2.0.0";

    private static final ObjectMapper MAPPER = mapper();

    private TasksJsonWriter() {
    }

    public static Path write(Path projectDir, AppConfig config, WarProject project) throws IOException {
        if (projectDir == null) {
            throw new IOException("プロジェクトディレクトリが未設定です");
        }
        Path dir = projectDir.toAbsolutePath().normalize();
        if (!Files.isDirectory(dir)) {
            throw new IOException("プロジェクトディレクトリがありません: " + dir);
        }
        if (config == null || config.getEap().homePath() == null) {
            throw new IOException("EAP ホームが未設定です");
        }
        if (config.getEap().standaloneScript() == null || config.getEap().cliScript() == null) {
            throw new IOException("EAP のスクリプトパスを解決できません");
        }
        Path vscode = dir.resolve(".vscode");
        Files.createDirectories(vscode);
        VscodeEapSettings.write(dir, config.getEap(), project);
        Path file = vscode.resolve("tasks.json");
        ObjectNode root = readOrCreate(file);
        if (!root.hasNonNull("version")) {
            root.put("version", VERSION);
        }
        ArrayNode tasks = array(root, "tasks");
        removeLegacy(tasks);
        for (TaskSpec spec : EapGuiTasks.specs(config, project)) {
            ObjectNode node = findByLabel(tasks, spec.label());
            if (node == null) {
                node = MAPPER.createObjectNode();
                tasks.add(node);
            }
            fill(node, spec);
        }
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), root);
        return file;
    }

    static void fill(ObjectNode node, TaskSpec spec) {
        node.removeAll();
        node.put("label", spec.label());
        if (spec.compound()) {
            ArrayNode depends = MAPPER.createArrayNode();
            for (String label : spec.dependsOn()) {
                depends.add(label);
            }
            node.set("dependsOn", depends);
            node.put("dependsOrder", "sequence");
            node.set("problemMatcher", MAPPER.createArrayNode());
            return;
        }
        node.put("type", "shell");
        node.put("command", spec.command());
        ArrayNode args = MAPPER.createArrayNode();
        for (String arg : spec.args()) {
            args.add(arg);
        }
        node.set("args", args);
        ObjectNode options = MAPPER.createObjectNode();
        options.put("cwd", spec.cwd());
        if (!spec.env().isEmpty()) {
            ObjectNode env = MAPPER.createObjectNode();
            spec.env().forEach(env::put);
            options.set("env", env);
        }
        if (Os.isWindows()) {
            ObjectNode shell = MAPPER.createObjectNode();
            shell.put("executable", "cmd.exe");
            ArrayNode shellArgs = MAPPER.createArrayNode();
            shellArgs.add("/d");
            shellArgs.add("/c");
            shellArgs.add("chcp 65001 >nul &&");
            shell.set("args", shellArgs);
            options.set("shell", shell);
        }
        node.set("options", options);
        ObjectNode presentation = MAPPER.createObjectNode();
        presentation.put("reveal", "always");
        presentation.put("panel", "shared");
        node.set("presentation", presentation);
        if (spec.background()) {
            node.put("isBackground", true);
            node.set("problemMatcher", backgroundMatcher());
        } else {
            node.put("isBackground", false);
            node.set("problemMatcher", MAPPER.createArrayNode());
        }
        if (spec.defaultBuild()) {
            ObjectNode group = MAPPER.createObjectNode();
            group.put("kind", "build");
            group.put("isDefault", true);
            node.set("group", group);
        }
    }

    private static ObjectNode backgroundMatcher() {
        ObjectNode matcher = MAPPER.createObjectNode();
        matcher.put("owner", "eap");
        ObjectNode pattern = MAPPER.createObjectNode();
        pattern.put("regexp", ".");
        matcher.set("pattern", pattern);
        ObjectNode background = MAPPER.createObjectNode();
        background.put("activeOnStart", true);
        background.put("beginsPattern", ".");
        background.put("endsPattern", "WFLYSRV0025|started in");
        matcher.set("background", background);
        return matcher;
    }

    static void removeLegacy(ArrayNode tasks) {
        for (int i = tasks.size() - 1; i >= 0; i--) {
            String label = tasks.get(i).path("label").asText();
            if (EapGuiTask.LEGACY_LABELS.contains(label)) {
                tasks.remove(i);
            }
        }
    }

    static ObjectNode readOrCreate(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            ObjectNode root = MAPPER.createObjectNode();
            root.put("version", VERSION);
            root.set("tasks", MAPPER.createArrayNode());
            return root;
        }
        try {
            JsonNode parsed = MAPPER.readTree(file.toFile());
            if (parsed != null && parsed.isObject()) {
                return (ObjectNode) parsed;
            }
        } catch (IOException e) {
            throw new IOException("既存の tasks.json を読めません: " + file, e);
        }
        throw new IOException("既存の tasks.json の形式が不正です: " + file);
    }

    static ArrayNode array(ObjectNode root, String field) {
        JsonNode node = root.get(field);
        if (node instanceof ArrayNode existing) {
            return existing;
        }
        ArrayNode created = MAPPER.createArrayNode();
        root.set(field, created);
        return created;
    }

    static ObjectNode findByLabel(ArrayNode tasks, String label) {
        for (JsonNode node : tasks) {
            if (node instanceof ObjectNode object && label.equals(object.path("label").asText())) {
                return object;
            }
        }
        return null;
    }

    private static ObjectMapper mapper() {
        DefaultPrettyPrinter pretty = new DefaultPrettyPrinter();
        DefaultIndenter indenter = new DefaultIndenter("    ", "\n");
        pretty.indentObjectsWith(indenter);
        pretty.indentArraysWith(indenter);
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setDefaultPrettyPrinter(pretty);
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        mapper.configure(JsonParser.Feature.ALLOW_TRAILING_COMMA, true);
        return mapper;
    }
}
