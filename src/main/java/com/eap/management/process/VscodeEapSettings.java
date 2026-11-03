package com.eap.management.process;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.eap.management.config.EapSettings;
import com.eap.management.config.WarProject;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

final class VscodeEapSettings {
    static final String KEY_HOME = "eap.home";
    static final String KEY_JAVA_TOOL_OPTIONS = "eap.javaToolOptions";
    static final String KEY_JAVA_SIZING = "eap.javaSizing";
    static final String KEY_CLI_JAVA_OPTS = "eap.cliJavaOpts";
    static final String KEY_STANDALONE_SCRIPT = "eap.standaloneScript";
    static final String KEY_CLI_SCRIPT = "eap.cliScript";
    static final String KEY_CONFIG_FILE = "eap.configFile";
    static final String KEY_BIND_ADDRESS = "eap.bindAddress";
    static final String KEY_MANAGEMENT_HOST = "eap.managementHost";
    static final String KEY_HTTP_PORT = "eap.httpPort";
    static final String KEY_MANAGEMENT_PORT = "eap.managementPort";
    static final String KEY_MANAGEMENT_USER = "eap.managementUser";
    static final String KEY_DEBUG_PORT = "eap.debugPort";
    static final String KEY_WAR_FILE = "eap.warFile";
    static final String KEY_DEPLOY_NAME = "eap.deployName";

    static final String DEFAULT_JAVA_TOOL_OPTIONS = "-Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8";
    static final String DEFAULT_JAVA_SIZING = "-Xms256m -Xmx512m";
    static final String DEFAULT_CLI_JAVA_OPTS = "-Xms32m -Xmx128m";

    static final String HOME = config(KEY_HOME);
    static final String JAVA_TOOL_OPTIONS = config(KEY_JAVA_TOOL_OPTIONS);
    static final String JAVA_SIZING = config(KEY_JAVA_SIZING);
    static final String CLI_JAVA_OPTS = config(KEY_CLI_JAVA_OPTS);
    static final String STANDALONE_SCRIPT = config(KEY_STANDALONE_SCRIPT);
    static final String CLI_SCRIPT = config(KEY_CLI_SCRIPT);
    static final String CONFIG_FILE = config(KEY_CONFIG_FILE);
    static final String BIND_ADDRESS = config(KEY_BIND_ADDRESS);
    static final String MANAGEMENT_HOST = config(KEY_MANAGEMENT_HOST);
    static final String HTTP_PORT = config(KEY_HTTP_PORT);
    static final String MANAGEMENT_PORT = config(KEY_MANAGEMENT_PORT);
    static final String MANAGEMENT_USER = config(KEY_MANAGEMENT_USER);
    static final String DEBUG_PORT = config(KEY_DEBUG_PORT);
    static final String WAR_FILE = config(KEY_WAR_FILE);
    static final String DEPLOY_NAME = config(KEY_DEPLOY_NAME);
    static final String WORKSPACE_WAR = "${workspaceFolder}/" + WAR_FILE;
    static final String DEPLOYED_WAR = Os.isWindows()
            ? HOME + "\\standalone\\deployments\\" + DEPLOY_NAME
            : HOME + "/standalone/deployments/" + DEPLOY_NAME;

    private static final ObjectMapper MAPPER = mapper();

    private VscodeEapSettings() {
    }

    static Path write(Path projectDir, EapSettings eap, WarProject project) throws IOException {
        Path vscode = projectDir.resolve(".vscode");
        Files.createDirectories(vscode);
        Path file = vscode.resolve("settings.json");
        ObjectNode root = readOrCreate(file);
        root.put(KEY_HOME, nativePath(eap.homePath()));
        putIfAbsent(root, KEY_JAVA_TOOL_OPTIONS, DEFAULT_JAVA_TOOL_OPTIONS);
        putIfAbsent(root, KEY_JAVA_SIZING, DEFAULT_JAVA_SIZING);
        putIfAbsent(root, KEY_CLI_JAVA_OPTS, DEFAULT_CLI_JAVA_OPTS);
        root.put(KEY_STANDALONE_SCRIPT, relativeBin(eap.standaloneScript()));
        root.put(KEY_CLI_SCRIPT, relativeBin(eap.cliScript()));
        root.put(KEY_CONFIG_FILE, eap.getConfigFile());
        root.put(KEY_BIND_ADDRESS, eap.getBindAddress());
        root.put(KEY_MANAGEMENT_HOST, eap.managementHost());
        root.put(KEY_HTTP_PORT, eap.getHttpPort());
        root.put(KEY_MANAGEMENT_PORT, eap.getManagementPort());
        root.put(KEY_DEBUG_PORT, eap.getDebugPort());
        if (eap.hasManagementCredentials()) {
            root.put(KEY_MANAGEMENT_USER, eap.getManagementUser());
        } else {
            root.remove(KEY_MANAGEMENT_USER);
        }
        root.put(KEY_WAR_FILE, JbossCliCommands.workspaceWarRelative(project));
        root.put(KEY_DEPLOY_NAME, JbossCliCommands.runtimeName(project));
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), root);
        return file;
    }

    private static void putIfAbsent(ObjectNode root, String key, String value) {
        if (!root.hasNonNull(key) || root.path(key).asText().isBlank()) {
            root.put(key, value);
        }
    }

    private static String relativeBin(Path script) {
        String fileName = script.getFileName() == null ? "standalone.bat" : script.getFileName().toString();
        return Os.isWindows() ? "bin\\" + fileName : "bin/" + fileName;
    }

    private static String nativePath(Path path) {
        String value = path.toAbsolutePath().normalize().toString();
        return Os.isWindows() ? value.replace('/', '\\') : value.replace('\\', '/');
    }

    private static String config(String key) {
        return "${config:" + key + "}";
    }

    private static ObjectNode readOrCreate(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            return MAPPER.createObjectNode();
        }
        try {
            JsonNode parsed = MAPPER.readTree(file.toFile());
            if (parsed != null && parsed.isObject()) {
                return (ObjectNode) parsed;
            }
        } catch (IOException e) {
            throw new IOException("既存の settings.json を読めません: " + file, e);
        }
        throw new IOException("既存の settings.json の形式が不正です: " + file);
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
