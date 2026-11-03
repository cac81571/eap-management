package com.eap.management.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.eap.management.config.AppConfig;
import com.eap.management.config.DeployMode;
import com.eap.management.config.WarProject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class TasksJsonWriterTest {
    @TempDir
    Path temp;

    @Test
    void writesGuiTasksWithConfigVariables() throws Exception {
        AppConfig config = new AppConfig();
        config.getEap().setHome("C:/jboss/jboss-eap-8.1");
        config.getEap().setDeployMode(DeployMode.DIRECTORY);
        WarProject project = new WarProject();
        project.setProjectDir(temp.toString());
        project.setDeployName("probe.war");
        project.setBuildCommand("mvn -DskipTests package");
        config.getProjects().add(project);

        Path file = TasksJsonWriter.write(temp, config, project);
        JsonNode tasks = new ObjectMapper().readTree(file.toFile()).path("tasks");
        JsonNode settings = new ObjectMapper().readTree(temp.resolve(".vscode").resolve("settings.json").toFile());

        assertEquals(nativeHome("C:/jboss/jboss-eap-8.1"), settings.path("eap.home").asText());
        assertEquals(Os.isWindows() ? "bin\\standalone.bat" : "bin/standalone.sh",
                settings.path("eap.standaloneScript").asText());
        assertEquals(Os.isWindows() ? "bin\\jboss-cli.bat" : "bin/jboss-cli.sh",
                settings.path("eap.cliScript").asText());
        assertEquals(VscodeEapSettings.DEFAULT_JAVA_TOOL_OPTIONS, settings.path("eap.javaToolOptions").asText());
        assertEquals(VscodeEapSettings.DEFAULT_JAVA_SIZING, settings.path("eap.javaSizing").asText());
        assertEquals(VscodeEapSettings.DEFAULT_CLI_JAVA_OPTS, settings.path("eap.cliJavaOpts").asText());
        assertEquals("standalone.xml", settings.path("eap.configFile").asText());
        assertEquals(8080, settings.path("eap.httpPort").asInt());
        assertEquals(9990, settings.path("eap.managementPort").asInt());
        assertEquals(8787, settings.path("eap.debugPort").asInt());
        assertEquals(Os.isWindows() ? "target\\probe.war" : "target/probe.war", settings.path("eap.warFile").asText());
        assertEquals("probe.war", settings.path("eap.deployName").asText());

        assertEquals(temp.resolve(".vscode").resolve("tasks.json"), file);
        assertEquals(EapGuiTask.values().length, tasks.size());

        JsonNode start = find(tasks, "起動");
        assertEquals("${config:eap.standaloneScript}", start.path("command").asText());
        assertEquals("${config:eap.home}", start.path("options").path("cwd").asText());
        assertEquals("${config:eap.javaToolOptions}",
                start.path("options").path("env").path("JAVA_TOOL_OPTIONS").asText());
        assertEquals("${config:eap.javaSizing}",
                start.path("options").path("env").path("JBOSS_JAVA_SIZING").asText());
        assertTrue(start.path("isBackground").asBoolean());
        assertTrue(containsArg(start, "-c"));
        assertTrue(containsArg(start, "${config:eap.configFile}"));
        assertTrue(containsArg(start, "-Djboss.http.port=${config:eap.httpPort}"));
        if (Os.isWindows()) {
            assertTrue(containsShellArg(start, "chcp 65001 >nul &&"));
        }

        JsonNode debug = find(tasks, "デバッグ起動");
        assertTrue(containsArg(debug, "--debug"));
        assertTrue(containsArg(debug, "${config:eap.debugPort}"));
        assertEquals("${config:eap.javaSizing}",
                debug.path("options").path("env").path("JBOSS_JAVA_SIZING").asText());

        JsonNode stop = find(tasks, "停止");
        assertEquals("${config:eap.cliScript}", stop.path("command").asText());
        assertTrue(containsArg(stop, "--command=:shutdown"));
        assertTrue(containsArg(stop, "--controller=${config:eap.managementHost}:${config:eap.managementPort}"));
        assertFalse(containsArg(stop, "--user=${config:eap.managementUser}"));
        assertEquals("${config:eap.cliJavaOpts}", stop.path("options").path("env").path("JAVA_OPTS").asText());
        assertTrue(settings.path("eap.managementUser").isMissingNode());

        assertEquals("mvn -DskipTests package", find(tasks, "ビルド").path("command").asText());
        assertTrue(find(tasks, "ビルド").path("options").path("env").isMissingNode());

        JsonNode deploy = find(tasks, "デプロイ");
        JsonNode undeploy = find(tasks, "アンデプロイ");
        if (Os.isWindows()) {
            assertEquals("copy", deploy.path("command").asText());
            assertTrue(containsArg(deploy, "/Y"));
            assertTrue(containsArg(deploy,
                    "${config:eap.home}\\standalone\\deployments\\${config:eap.deployName}"));
            assertEquals("del", undeploy.path("command").asText());
            assertTrue(containsArg(undeploy, "/Q"));
            assertTrue(containsArg(undeploy,
                    "${config:eap.home}\\standalone\\deployments\\${config:eap.deployName}*"));
        } else {
            assertEquals("cp", deploy.path("command").asText());
            assertTrue(containsArg(deploy,
                    "${config:eap.home}/standalone/deployments/${config:eap.deployName}"));
            assertEquals("rm", undeploy.path("command").asText());
            assertTrue(containsArg(undeploy,
                    "${config:eap.home}/standalone/deployments/${config:eap.deployName}*"));
        }
        assertTrue(containsArg(deploy, "${config:eap.warFile}"));
        JsonNode combo = find(tasks, "ビルド＋デプロイ");
        assertEquals("ビルド", combo.path("dependsOn").get(0).asText());
        assertEquals("デプロイ", combo.path("dependsOn").get(1).asText());
        assertEquals("sequence", combo.path("dependsOrder").asText());
        JsonNode restart = find(tasks, "再起動");
        assertEquals("停止", restart.path("dependsOn").get(0).asText());
        assertEquals("起動", restart.path("dependsOn").get(1).asText());
    }

    @Test
    void deployUsesCliWhenConfigured() throws Exception {
        AppConfig config = new AppConfig();
        config.getEap().setHome("C:/eap");
        config.getEap().setDeployMode(DeployMode.CLI);
        config.getEap().setManagementUser("admin");
        config.getEap().setManagementPassword("secret");
        WarProject project = new WarProject();
        project.setProjectDir(temp.toString());
        project.setDeployName("shop.war");

        JsonNode tasks = new ObjectMapper().readTree(TasksJsonWriter.write(temp, config, project).toFile()).path("tasks");
        JsonNode deploy = find(tasks, "デプロイ");
        assertEquals("${config:eap.cliScript}", deploy.path("command").asText());
        assertTrue(containsArg(deploy, "--user=${config:eap.managementUser}"));
        assertFalse(containsArg(deploy, "--password=secret"));
        assertTrue(containsArg(deploy,
                "--command=deploy \"${workspaceFolder}/${config:eap.warFile}\" --name=${config:eap.deployName} --force"));
        JsonNode undeploy = find(tasks, "アンデプロイ");
        assertEquals("${config:eap.cliScript}", undeploy.path("command").asText());
        assertTrue(containsArg(undeploy, "--command=undeploy ${config:eap.deployName}"));
        assertTrue(containsArg(undeploy, "--user=${config:eap.managementUser}"));
        JsonNode settings = new ObjectMapper().readTree(temp.resolve(".vscode").resolve("settings.json").toFile());
        assertEquals(Os.isWindows() ? "target\\shop.war" : "target/shop.war", settings.path("eap.warFile").asText());
        assertEquals("shop.war", settings.path("eap.deployName").asText());
        assertEquals("admin", settings.path("eap.managementUser").asText());
        assertTrue(settings.path("eap.managementPassword").isMissingNode());
    }

    @Test
    void mergesSettingsWithoutRemovingOtherKeys() throws Exception {
        Path vscode = temp.resolve(".vscode");
        Files.createDirectories(vscode);
        Files.writeString(vscode.resolve("settings.json"), """
                {
                    "java.configuration.updateBuildConfiguration": "automatic",
                    "eap.home": "C:/old",
                    "eap.javaSizing": "-Xmx1g"
                }
                """);

        AppConfig config = new AppConfig();
        config.getEap().setHome("C:/eap");
        TasksJsonWriter.write(temp, config, new WarProject());

        JsonNode settings = new ObjectMapper().readTree(vscode.resolve("settings.json").toFile());
        assertEquals("automatic", settings.path("java.configuration.updateBuildConfiguration").asText());
        assertEquals(nativeHome("C:/eap"), settings.path("eap.home").asText());
        assertEquals("-Xmx1g", settings.path("eap.javaSizing").asText());
        assertEquals(VscodeEapSettings.DEFAULT_JAVA_TOOL_OPTIONS, settings.path("eap.javaToolOptions").asText());
    }

    @Test
    void replacesLegacyLabelsAndKeepsOtherTasks() throws Exception {
        Path vscode = temp.resolve(".vscode");
        Files.createDirectories(vscode);
        Files.writeString(vscode.resolve("tasks.json"), """
                {
                    "version": "2.0.0",
                    "tasks": [
                        {
                            "label": "package",
                            "type": "shell",
                            "command": "mvn -DskipTests package"
                        },
                        {
                            "label": "jboss-cli:shutdown",
                            "type": "shell",
                            "command": "old"
                        }
                    ]
                }
                """);

        AppConfig config = new AppConfig();
        config.getEap().setHome("C:/eap");
        JsonNode tasks = new ObjectMapper().readTree(
                TasksJsonWriter.write(temp, config, new WarProject()).toFile()).path("tasks");
        assertEquals(1 + EapGuiTask.values().length, tasks.size());
        assertEquals("package", tasks.get(0).path("label").asText());
        assertEquals("起動", tasks.get(1).path("label").asText());
        for (JsonNode task : tasks) {
            if ("jboss-cli:shutdown".equals(task.path("label").asText())) {
                throw new AssertionError("legacy task should be removed");
            }
        }
    }

    private static String nativeHome(String path) {
        String value = Path.of(path).toAbsolutePath().normalize().toString();
        return Os.isWindows() ? value.replace('/', '\\') : value.replace('\\', '/');
    }

    private static JsonNode find(JsonNode tasks, String label) {
        for (JsonNode task : tasks) {
            if (label.equals(task.path("label").asText())) {
                return task;
            }
        }
        throw new AssertionError("missing task: " + label);
    }

    private static boolean containsArg(JsonNode task, String expected) {
        for (JsonNode arg : task.path("args")) {
            if (expected.equals(arg.asText())) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsShellArg(JsonNode task, String expected) {
        for (JsonNode arg : task.path("options").path("shell").path("args")) {
            if (expected.equals(arg.asText())) {
                return true;
            }
        }
        return false;
    }
}
