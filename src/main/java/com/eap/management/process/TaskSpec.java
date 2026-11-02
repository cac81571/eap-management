package com.eap.management.process;

import java.util.List;

record TaskSpec(
        String label,
        boolean compound,
        List<String> dependsOn,
        String command,
        List<String> args,
        String cwd,
        boolean background,
        boolean defaultBuild
) {
    static TaskSpec command(String label, String command, List<String> args, String cwd, boolean background) {
        return new TaskSpec(label, false, List.of(), command, args, cwd, background, false);
    }

    static TaskSpec build(String label, String command, String cwd) {
        return new TaskSpec(label, false, List.of(), command, List.of(), cwd, false, true);
    }

    static TaskSpec compound(String label, List<String> dependsOn) {
        return new TaskSpec(label, true, dependsOn, "", List.of(), "", false, false);
    }
}
