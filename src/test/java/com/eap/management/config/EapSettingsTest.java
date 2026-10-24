package com.eap.management.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class EapSettingsTest {
    @Test
    void configFilePathUsesStandaloneConfiguration() {
        EapSettings settings = new EapSettings();
        settings.setHome("C:/jboss/jboss-eap-8.1");
        settings.setConfigFile("standalone.xml");
        assertEquals(
                Path.of("C:/jboss/jboss-eap-8.1/standalone/configuration/standalone.xml"),
                settings.configFilePath());
    }

    @Test
    void standaloneConfPathUsesBinDirectory() {
        EapSettings settings = new EapSettings();
        settings.setHome("C:/jboss/jboss-eap-8.1");
        String expected = com.eap.management.process.Os.isWindows()
                ? "C:/jboss/jboss-eap-8.1/bin/standalone.conf.bat"
                : "C:/jboss/jboss-eap-8.1/bin/standalone.conf";
        assertEquals(Path.of(expected), settings.standaloneConfPath());
    }

    @Test
    void configFilePathKeepsAbsolutePath() {
        EapSettings settings = new EapSettings();
        settings.setHome("C:/jboss/jboss-eap-8.1");
        Path custom = Path.of("C:/opt/eap/standalone-custom.xml");
        settings.setConfigFile(custom.toString());
        assertEquals(custom, settings.configFilePath());
    }
}
