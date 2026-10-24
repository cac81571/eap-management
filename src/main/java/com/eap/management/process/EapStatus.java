package com.eap.management.process;

public enum EapStatus {
    STOPPED("停止"),
    STARTING("起動中"),
    RUNNING("稼働中"),
    RESTARTING("再起動中"),
    STOPPING("停止作業中"),
    UNCONFIGURED("未設定"),
    UNKNOWN("不明");

    private final String label;

    EapStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean transitioning() {
        return this == STARTING || this == STOPPING || this == RESTARTING;
    }
}
