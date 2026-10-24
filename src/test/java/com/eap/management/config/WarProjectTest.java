package com.eap.management.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WarProjectTest {
    @TempDir
    Path temp;

    @Test
    void resolveNewestWarFromGlob() throws Exception {
        Path target = temp.resolve("target");
        Files.createDirectories(target);
        Path older = target.resolve("app-1.0.0.war");
        Path newer = target.resolve("app-1.0.1.war");
        Files.writeString(older, "old");
        Thread.sleep(20);
        Files.writeString(newer, "new");

        WarProject project = new WarProject();
        project.setProjectDir(temp.toString());
        project.setWarPath("target/*.war");
        project.setDeployName("shop");

        Path resolved = project.resolveWarFile().orElseThrow();
        assertEquals(newer.toAbsolutePath().normalize(), resolved.toAbsolutePath().normalize());
        assertEquals("shop.war", project.resolvedDeployName(resolved));
        assertEquals("shop", project.contextRoot(resolved));
        assertEquals("shop", project.suggestedContextRoot());
    }

    @Test
    void resolveExactWarFile() throws Exception {
        Path war = temp.resolve("custom.war");
        Files.writeString(war, "war");
        WarProject project = new WarProject();
        project.setProjectDir(temp.toString());
        project.setWarPath(war.toString());
        assertTrue(project.resolveWarFile().isPresent());
        assertEquals("custom.war", project.resolvedDeployName(war));
    }

    @Test
    void resolveAppUrlUsesRegisteredPathOrAbsoluteUrl() {
        WarProject project = new WarProject();
        project.setAppUrl("/portal/login");
        assertEquals("http://localhost:8080/portal/login", project.resolveAppUrl("http://localhost:8080/"));

        project.setAppUrl("https://example.local/app");
        assertEquals("https://example.local/app", project.resolveAppUrl("http://localhost:8080/"));
    }

    @Test
    void suggestedContextRootIgnoresMissingGlob() {
        WarProject project = new WarProject();
        project.setProjectDir(temp.toString());
        project.setWarPath("target/*.war");
        assertEquals("", project.suggestedContextRoot());
    }
}
