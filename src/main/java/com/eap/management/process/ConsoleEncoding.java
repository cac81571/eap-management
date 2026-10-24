package com.eap.management.process;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class ConsoleEncoding {
    public static final String AUTO = "AUTO";
    public static final String UTF8 = "UTF-8";
    public static final String MS932 = "MS932";

    private static final Charset SHIFT_JIS = Charset.forName("MS932");

    private ConsoleEncoding() {
    }

    public static List<String> choices() {
        return List.of(AUTO, UTF8, MS932);
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank() || AUTO.equalsIgnoreCase(value)) {
            return AUTO;
        }
        if (MS932.equalsIgnoreCase(value) || "Shift_JIS".equalsIgnoreCase(value) || "SJIS".equalsIgnoreCase(value)) {
            return MS932;
        }
        if (UTF8.equalsIgnoreCase(value) || "UTF8".equalsIgnoreCase(value)) {
            return UTF8;
        }
        return AUTO;
    }

    public static String decode(byte[] bytes, String mode) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        String normalized = normalize(mode);
        if (UTF8.equals(normalized)) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        if (MS932.equals(normalized)) {
            return new String(bytes, SHIFT_JIS);
        }
        return decodeAuto(bytes);
    }

    static String decodeAuto(byte[] bytes) {
        try {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            return decoder.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, SHIFT_JIS);
        }
    }
}
