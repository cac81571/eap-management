package com.eap.management.process;

public enum JbossCliTask {
    START("eap:start", false, true),
    START_DEBUG("eap:start (debug)", false, true),
    SHUTDOWN("jboss-cli:shutdown", true, false),
    RELOAD("jboss-cli:reload", true, false),
    RESTART("jboss-cli:restart", true, false),
    DEPLOY("jboss-cli:deploy", true, false),
    UNDEPLOY("jboss-cli:undeploy", true, false),
    SERVER_STATE("jboss-cli:server-state", true, false);

    private final String label;
    private final boolean cli;
    private final boolean background;

    JbossCliTask(String label, boolean cli, boolean background) {
        this.label = label;
        this.cli = cli;
        this.background = background;
    }

    public String label() {
        return label;
    }

    public boolean cli() {
        return cli;
    }

    public boolean background() {
        return background;
    }

    public boolean debug() {
        return this == START_DEBUG;
    }
}
