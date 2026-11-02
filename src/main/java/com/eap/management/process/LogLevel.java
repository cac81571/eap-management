package com.eap.management.process;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum LogLevel {
    ERROR,
    WARN,
    INFO,
    DEBUG,
    OTHER;

    private static final Pattern LEVEL = Pattern.compile(
            "(?i)(?:\\[|\\b)(FATAL|SEVERE|ERROR|WARN(?:ING)?|INFO|DEBUG|TRACE)(?:\\]|\\b)");
    private static final Pattern JAVA_EXCEPTION = Pattern.compile(
            "(?i)\\b[A-Za-z][\\w$]*(Exception|Error)\\b");

    public static LogLevel detect(String text) {
        if (text == null || text.isBlank()) {
            return OTHER;
        }
        String raw = text;
        String upper = raw.toUpperCase(Locale.ROOT);
        if (raw.contains("失敗") || raw.contains("エラー")
                || upper.contains("BUILD FAILURE") || upper.contains("CAUSED BY")
                || JAVA_EXCEPTION.matcher(raw).find()) {
            return ERROR;
        }
        Matcher matcher = LEVEL.matcher(raw);
        LogLevel found = OTHER;
        while (matcher.find()) {
            found = stronger(found, fromToken(matcher.group(1)));
        }
        return found;
    }

    private static LogLevel fromToken(String token) {
        String value = token.toUpperCase(Locale.ROOT);
        return switch (value) {
            case "FATAL", "SEVERE", "ERROR" -> ERROR;
            case "WARN", "WARNING" -> WARN;
            case "INFO" -> INFO;
            case "DEBUG", "TRACE" -> DEBUG;
            default -> OTHER;
        };
    }

    private static LogLevel stronger(LogLevel current, LogLevel candidate) {
        return rank(candidate) < rank(current) ? candidate : current;
    }

    private static int rank(LogLevel level) {
        return switch (level) {
            case ERROR -> 0;
            case WARN -> 1;
            case INFO -> 2;
            case DEBUG -> 3;
            case OTHER -> 4;
        };
    }
}
