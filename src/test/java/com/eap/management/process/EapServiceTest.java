package com.eap.management.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.eap.management.config.EapSettings;

class EapServiceTest {
    @Test
    void stopDoesNotLookLikeStartingWhenWrapperProcessRemains() {
        assertEquals(EapStatus.STOPPING, EapService.nextStatus(EapStatus.STOPPING, true, true, false));
        assertEquals(EapStatus.STOPPED, EapService.nextStatus(EapStatus.STOPPING, false, true, false));
        assertEquals(EapStatus.STOPPED, EapService.nextStatus(EapStatus.STOPPED, false, true, false));
        assertEquals(EapStatus.STARTING, EapService.nextStatus(EapStatus.STARTING, false, true, false));
        assertEquals(EapStatus.STARTING, EapService.nextStatus(EapStatus.STARTING, false, false, true));
        assertEquals(EapStatus.RUNNING, EapService.nextStatus(EapStatus.STARTING, true, true, true));
    }

    @Test
    void startArgsAddsDebugPort() {
        EapSettings settings = new EapSettings();
        settings.setDebugPort(8787);
        List<String> args = EapService.startArgs(settings, true);
        assertEquals("--debug", args.get(0));
        assertEquals("8787", args.get(1));
        assertTrue(args.contains("-c"));
    }

    @Test
    void tokenizeKeepsQuotedArguments() {
        List<String> tokens = EapService.tokenize("-Dfoo=bar  \"path with space\"   --debug");
        assertEquals(List.of("-Dfoo=bar", "path with space", "--debug"), tokens);
    }
}
