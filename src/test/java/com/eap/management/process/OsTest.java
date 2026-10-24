package com.eap.management.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class OsTest {
    @Test
    void tokenizeQuotedKeepsProgramFilesX86() {
        assertEquals(
                List.of("C:\\Program Files (x86)\\sakura\\sakura.exe", "C:\\eap\\bin\\standalone.conf.bat"),
                Os.tokenizeQuoted(Os.expandCommandTemplate(
                        "\"C:\\Program Files (x86)\\sakura\\sakura.exe\" \"%1\"",
                        "C:\\eap\\bin\\standalone.conf.bat")));
    }

    @Test
    void expandCommandTemplateUsesQuotedFile() {
        assertEquals(
                "\"C:\\App\\Code.exe\" --goto \"C:\\eap\\bin\\standalone.conf.bat\"",
                Os.expandCommandTemplate(
                        "\"C:\\App\\Code.exe\" --goto \"%1\"",
                        "C:\\eap\\bin\\standalone.conf.bat"));
        assertEquals(
                "NOTEPAD.EXE \"C:\\eap\\bin\\standalone.conf.bat\"",
                Os.expandCommandTemplate("NOTEPAD.EXE %1", "C:\\eap\\bin\\standalone.conf.bat"));
    }

    @Test
    void parseRegSzReadsValue() {
        String output = """
                HKEY_CLASSES_ROOT\\.txt
                    (Default)    REG_SZ    txtfile
                """;
        assertEquals("txtfile", Os.parseRegSz(output));
    }

    @Test
    void batOpensAsTextNotExecutableAssociation() {
        assertTrue(Os.opensAsExecutable(Path.of("bin/standalone.conf.bat")));
        assertFalse(Os.opensAsExecutable(Path.of("standalone/configuration/standalone.xml")));
    }
}
