package com.eap.management.process;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LogLevelTest {
    @Test
    void detectsJbossAndMavenLevels() {
        assertEquals(LogLevel.ERROR, LogLevel.detect("11:22:33,456 ERROR [org.jboss.as] failed"));
        assertEquals(LogLevel.WARN, LogLevel.detect("11:22:33,456 WARN  [org.jboss.as.server] slow"));
        assertEquals(LogLevel.WARN, LogLevel.detect("[WARNING] something odd"));
        assertEquals(LogLevel.INFO, LogLevel.detect("[INFO] Building shop 1.0.0"));
        assertEquals(LogLevel.DEBUG, LogLevel.detect("TRACE [org.jboss.msc] detail"));
        assertEquals(LogLevel.OTHER, LogLevel.detect("サーバーを起動しています"));
    }

    @Test
    void treatsFailuresAsError() {
        assertEquals(LogLevel.ERROR, LogLevel.detect("EAP 起動 に失敗しました: connection refused"));
        assertEquals(LogLevel.ERROR, LogLevel.detect("[INFO] BUILD FAILURE"));
        assertEquals(LogLevel.ERROR, LogLevel.detect("java.lang.IllegalStateException: boom"));
        assertEquals(LogLevel.ERROR, LogLevel.detect("FATAL [org.jboss.msc] service failed"));
    }

    @Test
    void prefersMoreSevereLevelWhenBothPresent() {
        assertEquals(LogLevel.ERROR, LogLevel.detect("[INFO] deployment ERROR [org.app]"));
    }
}
