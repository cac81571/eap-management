package com.eap.management.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigStoreTest {
    @TempDir
    Path temp;

    @Test
    void saveAndLoadRoundTrip() {
        Path file = temp.resolve("config.json");
        ConfigStore store = new ConfigStore(file);

        AppConfig config = new AppConfig();
        config.getEap().setHome("C:/eap");
        config.getEap().setHttpPort(8180);
        config.getEap().setDeployMode(DeployMode.CLI);
        WarProject project = new WarProject();
        project.setName("shop");
        project.setProjectDir("C:/work/shop");
        project.getEnv().add(new EnvVar("PROFILE", "dev"));
        config.getProjects().add(project);
        config.setSelectedProjectId(project.getId());
        config.getGlobalEnv().add(new EnvVar("JAVA_HOME", "C:/jdk"));

        store.save(config);
        AppConfig loaded = store.load();

        assertEquals("C:/eap", loaded.getEap().getHome());
        assertEquals(8180, loaded.getEap().getHttpPort());
        assertEquals(DeployMode.CLI, loaded.getEap().getDeployMode());
        assertEquals(1, loaded.getProjects().size());
        assertEquals("shop", loaded.getProjects().getFirst().getName());
        assertEquals("dev", loaded.getProjects().getFirst().getEnv().getFirst().getValue());
        assertEquals(project.getId(), loaded.getSelectedProjectId());
        assertTrue(file.toFile().exists());
    }
}
