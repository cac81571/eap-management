package com.eap.management.process;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.eap.management.config.EapSettings;
import com.eap.management.config.WarProject;

public final class JbossCliCommands {
    private JbossCliCommands() {
    }

    public static Path executable(JbossCliTask task, EapSettings eap) {
        if (eap == null) {
            return null;
        }
        return task.cli() ? eap.cliScript() : eap.standaloneScript();
    }

    public static List<String> arguments(JbossCliTask task, EapSettings eap, WarProject project) {
        if (!task.cli()) {
            return EapService.startArgs(eap, task.debug());
        }
        List<String> args = connectArgs(eap);
        args.add("--command=" + cliCommand(task, project));
        return args;
    }

    public static List<String> connectArgs(EapSettings eap) {
        List<String> args = new ArrayList<>();
        args.add("--connect");
        args.add("--controller=" + eap.managementHost() + ":" + eap.getManagementPort());
        appendAuth(args, eap);
        return args;
    }

    public static void appendAuth(List<String> args, EapSettings eap) {
        if (eap == null || !eap.hasManagementCredentials()) {
            return;
        }
        args.add("--user=" + eap.getManagementUser());
        args.add("--password=" + eap.getManagementPassword());
    }

    static String cliCommand(JbossCliTask task, WarProject project) {
        return switch (task) {
            case SHUTDOWN -> ":shutdown";
            case RELOAD -> ":reload";
            case RESTART -> ":shutdown(restart=true)";
            case SERVER_STATE -> ":read-attribute(name=server-state)";
            case DEPLOY -> {
                String war = workspaceWarPath(project);
                String name = runtimeName(project);
                yield "deploy \"" + war + "\" --name=" + name + " --force";
            }
            case UNDEPLOY -> "undeploy " + runtimeName(project);
            default -> throw new IllegalArgumentException(task.label());
        };
    }

    static String workspaceWarPath(WarProject project) {
        String warPath = project == null || project.getWarPath() == null || project.getWarPath().isBlank()
                ? "target/*.war"
                : project.getWarPath().trim().replace('\\', '/');
        if (!warPath.contains("*") && !warPath.contains("?")) {
            String relative = warPath.startsWith("/") ? warPath.substring(1) : warPath;
            return "${workspaceFolder}/" + relative;
        }
        String dir = "target";
        int slash = warPath.lastIndexOf('/');
        if (slash >= 0) {
            String parent = warPath.substring(0, slash);
            if (!parent.contains("*") && !parent.contains("?")) {
                dir = parent;
            }
        }
        return "${workspaceFolder}/" + dir + "/" + runtimeName(project);
    }

    static String workspaceWarRelative(WarProject project) {
        String path = workspaceWarPath(project);
        String prefix = "${workspaceFolder}/";
        if (path.startsWith(prefix)) {
            path = path.substring(prefix.length());
        }
        return Os.isWindows() ? path.replace('/', '\\') : path.replace('\\', '/');
    }

    static String runtimeName(WarProject project) {
        if (project != null && project.getDeployName() != null && !project.getDeployName().isBlank()) {
            String value = project.getDeployName().trim();
            return value.endsWith(".war") ? value : value + ".war";
        }
        if (project != null && project.getWarPath() != null) {
            String warPath = project.getWarPath().trim().replace('\\', '/');
            if (!warPath.contains("*") && !warPath.contains("?")) {
                int slash = warPath.lastIndexOf('/');
                String name = slash >= 0 ? warPath.substring(slash + 1) : warPath;
                if (!name.isBlank()) {
                    return name.endsWith(".war") ? name : name + ".war";
                }
            }
        }
        Path dir = project == null ? null : project.projectPath();
        String folder = dir != null && dir.getFileName() != null ? dir.getFileName().toString() : "application";
        return folder.endsWith(".war") ? folder : folder + ".war";
    }

    static String slash(Path path) {
        return path.toAbsolutePath().normalize().toString().replace('\\', '/');
    }
}
