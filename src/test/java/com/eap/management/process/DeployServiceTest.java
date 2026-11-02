package com.eap.management.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeployServiceTest {
    @TempDir
    Path temp;

    @Test
    void deployedWarNamesIgnoresMarkers() throws Exception {
        Path deployments = temp.resolve("deployments");
        Files.createDirectories(deployments);
        Files.writeString(deployments.resolve("app.war"), "war");
        Files.writeString(deployments.resolve("app.war.deployed"), "ok");
        Files.writeString(deployments.resolve("other.war.failed"), "ng");
        Files.writeString(deployments.resolve("legacy.ear"), "ear");

        assertEquals(Set.of("app.war"), DeployService.deployedWarNames(deployments));
    }

    @Test
    void deployedWarNamesIsEmptyWhenMissing() {
        assertTrue(DeployService.deployedWarNames(temp.resolve("missing")).isEmpty());
    }
}
