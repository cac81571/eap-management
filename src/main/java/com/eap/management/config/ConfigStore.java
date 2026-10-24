package com.eap.management.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class ConfigStore {
    private final Path file;
    private final ObjectMapper mapper;

    public ConfigStore() {
        this(defaultFile());
    }

    public ConfigStore(Path file) {
        this.file = file;
        this.mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static Path defaultFile() {
        return Path.of(System.getProperty("user.home"), ".eap-management", "config.json");
    }

    public Path file() {
        return file;
    }

    public AppConfig load() {
        if (!Files.isRegularFile(file)) {
            return new AppConfig();
        }
        try {
            return mapper.readValue(file.toFile(), AppConfig.class);
        } catch (IOException e) {
            throw new IllegalStateException("設定ファイルを読めません: " + file, e);
        }
    }

    public void save(AppConfig config) {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            mapper.writeValue(tmp.toFile(), config);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            try {
                Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
                if (Files.exists(tmp)) {
                    Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
                    return;
                }
            } catch (IOException ignored) {
                // fall through
            }
            throw new IllegalStateException("設定ファイルを保存できません: " + file, e);
        }
    }
}
