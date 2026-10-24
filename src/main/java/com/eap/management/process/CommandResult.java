package com.eap.management.process;

public record CommandResult(int exitCode, boolean timedOut) {
    public boolean succeeded() {
        return !timedOut && exitCode == 0;
    }
}
