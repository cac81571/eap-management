package com.eap.management.process;

@FunctionalInterface
public interface LogListener {
    void append(String source, String line);
}
