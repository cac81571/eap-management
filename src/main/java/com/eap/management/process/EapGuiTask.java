package com.eap.management.process;

import java.util.Set;

public enum EapGuiTask {
    START("起動"),
    START_DEBUG("デバッグ起動"),
    STOP("停止"),
    RESTART("再起動"),
    BUILD("ビルド"),
    DEPLOY("デプロイ"),
    UNDEPLOY("アンデプロイ"),
    BUILD_AND_DEPLOY("ビルド＋デプロイ");

    public static final Set<String> LEGACY_LABELS = Set.of(
            "eap:start",
            "eap:start (debug)",
            "jboss-cli:shutdown",
            "jboss-cli:reload",
            "jboss-cli:restart",
            "jboss-cli:deploy",
            "jboss-cli:undeploy",
            "jboss-cli:server-state"
    );

    private final String label;

    EapGuiTask(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
