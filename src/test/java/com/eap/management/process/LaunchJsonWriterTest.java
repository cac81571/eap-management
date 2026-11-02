package com.eap.management.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class LaunchJsonWriterTest {
    @TempDir
    Path temp;

    @Test
    void writesAttachConfigIntoNewFile() throws Exception {
        Path file = LaunchJsonWriter.write(temp, "shop", "localhost", 8787);
        JsonNode root = new ObjectMapper().readTree(file.toFile());
        JsonNode attach = root.path("configurations").get(0);

        assertEquals(temp.resolve(".vscode").resolve("launch.json"), file);
        assertEquals("0.2.0", root.path("version").asText());
        assertEquals("java", attach.path("type").asText());
        assertEquals(LaunchJsonWriter.CONFIG_NAME, attach.path("name").asText());
        assertEquals("attach", attach.path("request").asText());
        assertEquals("localhost", attach.path("hostName").asText());
        assertEquals(8787, attach.path("port").asInt());
        assertEquals("shop", attach.path("projectName").asText());
    }

    @Test
    void mergesWithoutRemovingOtherConfigurations() throws Exception {
        Path vscode = temp.resolve(".vscode");
        Files.createDirectories(vscode);
        Path file = vscode.resolve("launch.json");
        Files.writeString(file, """
                {
                    "version": "0.2.0",
                    "configurations": [
                        {
                            "type": "java",
                            "name": "Current File",
                            "request": "launch",
                            "mainClass": "${file}"
                        }
                    ]
                }
                """);

        LaunchJsonWriter.write(temp, "shop", "127.0.0.1", 8788);
        JsonNode root = new ObjectMapper().readTree(file.toFile());
        assertEquals(2, root.path("configurations").size());
        assertEquals("Current File", root.path("configurations").get(0).path("name").asText());
        JsonNode attach = root.path("configurations").get(1);
        assertEquals(LaunchJsonWriter.CONFIG_NAME, attach.path("name").asText());
        assertEquals(8788, attach.path("port").asInt());
        assertEquals("127.0.0.1", attach.path("hostName").asText());
    }

    @Test
    void updatesExistingAttachConfig() throws Exception {
        Path vscode = temp.resolve(".vscode");
        Files.createDirectories(vscode);
        Files.writeString(vscode.resolve("launch.json"), """
                {
                    "version": "0.2.0",
                    "configurations": [
                        {
                            "type": "java",
                            "name": "Attach to EAP",
                            "request": "attach",
                            "hostName": "localhost",
                            "port": 5005,
                            "timeout": 30000
                        }
                    ]
                }
                """);

        LaunchJsonWriter.write(temp, "probe", "localhost", 8787);
        JsonNode root = new ObjectMapper().readTree(vscode.resolve("launch.json").toFile());
        assertEquals(1, root.path("configurations").size());
        JsonNode attach = root.path("configurations").get(0);
        assertEquals(8787, attach.path("port").asInt());
        assertEquals(30000, attach.path("timeout").asInt());
        assertEquals("probe", attach.path("projectName").asText());
        assertTrue(Files.readString(vscode.resolve("launch.json")).contains("Attach to EAP"));
    }
}
