package com.eap.management.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class EnvVarTest {
    @Test
    void mergePrefersProjectAndSkipsDisabled() {
        EnvVar global = new EnvVar("JAVA_HOME", "C:/jdk-17");
        EnvVar disabled = new EnvVar("SKIP", "1");
        disabled.setEnabled(false);
        EnvVar override = new EnvVar("JAVA_HOME", "C:/jdk-21");
        EnvVar blank = new EnvVar("  ", "x");

        Map<String, String> merged = EnvVar.merge(List.of(global, disabled), List.of(override, blank));

        assertEquals("C:/jdk-21", merged.get("JAVA_HOME"));
        assertFalse(merged.containsKey("SKIP"));
        assertEquals(1, merged.size());
    }
}
