package com.eap.management.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class ConsoleEncodingTest {
    private static final Charset SHIFT_JIS = Charset.forName("MS932");

    @Test
    void autoPrefersUtf8UsedByEap8() {
        String original = "サーバーを起動しています";
        byte[] utf8 = original.getBytes(StandardCharsets.UTF_8);
        String garbled = new String(utf8, SHIFT_JIS);

        assertTrue(garbled.contains("繧ｵ"));
        assertEquals(original, ConsoleEncoding.decode(utf8, ConsoleEncoding.AUTO));
        assertEquals(garbled, ConsoleEncoding.decode(utf8, ConsoleEncoding.MS932));
    }

    @Test
    void autoFallsBackToMs932() {
        String original = "HTTP 管理インターフェイス";
        byte[] sjis = original.getBytes(SHIFT_JIS);
        assertEquals(original, ConsoleEncoding.decode(sjis, ConsoleEncoding.AUTO));
    }
}
