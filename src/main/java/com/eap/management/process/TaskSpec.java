package com.eap.management.process;

import java.util.List;
import java.util.Map;

record TaskSpec(
        String label,
        boolean compound,
        List<String> dependsOn,
        String command,
        List<String> args,
        String cwd,
        boolean background,
        boolean defaultBuild,
        Map<String, String> env
) {
    static TaskSpec command(String label, String command, List<String> args, String cwd, boolean background) {
        return command(label, command, args, cwd, background, Map.of());
    }

    static TaskSpec command(
            String label,
            String command,
            List<String> args,
            String cwd,
            boolean background,
            Map<String, String> env
    ) {
        return new TaskSpec(label, false, List.of(), command, args, cwd, background, false, env);
    }

    static TaskSpec build(String label, String command, String cwd) {
        return new TaskSpec(label, false, List.of(), command, List.of(), cwd, false, true, Map.of());
    }

    static TaskSpec compound(String label, List<String> dependsOn) {
        return new TaskSpec(label, true, dependsOn, "", List.of(), "", false, false, Map.of());
    }
}
