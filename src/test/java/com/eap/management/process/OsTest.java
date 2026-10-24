package com.eap.management.process;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class OsTest {
    @Test
    void batOpensAsTextNotExecutableAssociation() {
        assertTrue(Os.opensAsExecutable(Path.of("bin/standalone.conf.bat")));
        assertFalse(Os.opensAsExecutable(Path.of("standalone/configuration/standalone.xml")));
    }
}
