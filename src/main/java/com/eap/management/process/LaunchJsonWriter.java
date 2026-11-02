package com.eap.management.process;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class LaunchJsonWriter {
    public static final String CONFIG_NAME = "Attach to EAP";
    public static final String VERSION = "0.2.0";

    private static final ObjectMapper MAPPER = mapper();

    private LaunchJsonWriter() {
    }

    public static Path write(Path projectDir, String projectName, String hostName, int port) throws IOException {
        if (projectDir == null) {
            throw new IOException("プロジェクトディレクトリが未設定です");
        }
        Path dir = projectDir.toAbsolutePath().normalize();
        if (!Files.isDirectory(dir)) {
            throw new IOException("プロジェクトディレクトリがありません: " + dir);
        }
        Path vscode = dir.resolve(".vscode");
        Files.createDirectories(vscode);
        Path file = vscode.resolve("launch.json");
        ObjectNode root = readOrCreate(file);
        if (!root.hasNonNull("version")) {
            root.put("version", VERSION);
        }
        ArrayNode configurations = configurationsOf(root);
        ObjectNode attach = findAttach(configurations);
        if (attach == null) {
            attach = MAPPER.createObjectNode();
            configurations.add(attach);
        }
        attach.put("type", "java");
        attach.put("name", CONFIG_NAME);
        attach.put("request", "attach");
        attach.put("hostName", hostName == null || hostName.isBlank() ? "localhost" : hostName);
        attach.put("port", port <= 0 ? 8787 : port);
        if (projectName != null && !projectName.isBlank()) {
            attach.put("projectName", projectName);
        }
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), root);
        return file;
    }

    static ObjectNode readOrCreate(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            ObjectNode root = MAPPER.createObjectNode();
            root.put("version", VERSION);
            root.set("configurations", MAPPER.createArrayNode());
            return root;
        }
        try {
            JsonNode parsed = MAPPER.readTree(file.toFile());
            if (parsed != null && parsed.isObject()) {
                return (ObjectNode) parsed;
            }
        } catch (IOException e) {
            throw new IOException("既存の launch.json を読めません: " + file, e);
        }
        throw new IOException("既存の launch.json の形式が不正です: " + file);
    }

    static ArrayNode configurationsOf(ObjectNode root) {
        JsonNode node = root.get("configurations");
        if (node instanceof ArrayNode array) {
            return array;
        }
        ArrayNode created = MAPPER.createArrayNode();
        root.set("configurations", created);
        return created;
    }

    static ObjectNode findAttach(ArrayNode configurations) {
        for (JsonNode node : configurations) {
            if (node instanceof ObjectNode object && CONFIG_NAME.equals(object.path("name").asText())) {
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
