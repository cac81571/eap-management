package com.eap.management.process;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.eap.management.config.AppConfig;
import com.eap.management.config.DeployMode;
import com.eap.management.config.EapSettings;
import com.eap.management.config.WarProject;

final class EapGuiTasks {
    private static final Map<String, String> STANDALONE_ENV = Map.of(
            "JAVA_TOOL_OPTIONS", VscodeEapSettings.JAVA_TOOL_OPTIONS,
            "JBOSS_JAVA_SIZING", VscodeEapSettings.JAVA_SIZING
    );
    private static final Map<String, String> CLI_ENV = Map.of(
            "JAVA_TOOL_OPTIONS", VscodeEapSettings.JAVA_TOOL_OPTIONS,
            "JAVA_OPTS", VscodeEapSettings.CLI_JAVA_OPTS
    );

    private EapGuiTasks() {
    }

    static List<TaskSpec> specs(AppConfig config, WarProject project) {
        EapSettings eap = config.getEap();
        List<TaskSpec> specs = new ArrayList<>();
        specs.add(standalone(EapGuiTask.START, eap, false));
        specs.add(standalone(EapGuiTask.START_DEBUG, eap, true));
        specs.add(cli(EapGuiTask.STOP, JbossCliTask.SHUTDOWN, project));
        specs.add(TaskSpec.compound(EapGuiTask.RESTART.label(), List.of(
                EapGuiTask.STOP.label(), EapGuiTask.START.label())));
        specs.add(build(project));
        specs.add(deploy(eap, project));
        specs.add(undeploy(eap, project));
        specs.add(TaskSpec.compound(EapGuiTask.BUILD_AND_DEPLOY.label(), List.of(
                EapGuiTask.BUILD.label(), EapGuiTask.DEPLOY.label())));
        return specs;
    }

    private static TaskSpec standalone(EapGuiTask task, EapSettings eap, boolean debug) {
        return TaskSpec.command(
                task.label(),
                VscodeEapSettings.STANDALONE_SCRIPT,
                startArgs(eap, debug),
                VscodeEapSettings.HOME,
                true,
                STANDALONE_ENV
        );
    }

    private static TaskSpec cli(EapGuiTask task, JbossCliTask cliTask, WarProject project) {
        return TaskSpec.command(
                task.label(),
                VscodeEapSettings.CLI_SCRIPT,
                cliArgs(cliTask, project),
                VscodeEapSettings.HOME,
                false,
                CLI_ENV
        );
    }

    private static TaskSpec build(WarProject project) {
        String command = project == null || project.getBuildCommand() == null || project.getBuildCommand().isBlank()
                ? "mvn -DskipTests package"
                : project.getBuildCommand().trim();
        return TaskSpec.build(EapGuiTask.BUILD.label(), command, "${workspaceFolder}");
    }

    private static TaskSpec deploy(EapSettings eap, WarProject project) {
        if (eap.getDeployMode() == DeployMode.CLI) {
            return cli(EapGuiTask.DEPLOY, JbossCliTask.DEPLOY, project);
        }
        if (Os.isWindows()) {
            return TaskSpec.command(
                    EapGuiTask.DEPLOY.label(),
                    "copy",
                    List.of("/Y", VscodeEapSettings.WAR_FILE, VscodeEapSettings.DEPLOYED_WAR),
                    "${workspaceFolder}",
                    false
            );
        }
        return TaskSpec.command(
                EapGuiTask.DEPLOY.label(),
                "cp",
                List.of(VscodeEapSettings.WAR_FILE, VscodeEapSettings.DEPLOYED_WAR),
                "${workspaceFolder}",
                false
        );
    }

    private static TaskSpec undeploy(EapSettings eap, WarProject project) {
        if (eap.getDeployMode() == DeployMode.CLI) {
            return cli(EapGuiTask.UNDEPLOY, JbossCliTask.UNDEPLOY, project);
        }
        if (Os.isWindows()) {
            return TaskSpec.command(
                    EapGuiTask.UNDEPLOY.label(),
                    "del",
                    List.of("/Q", VscodeEapSettings.DEPLOYED_WAR + "*"),
                    "${workspaceFolder}",
                    false
            );
        }
        return TaskSpec.command(
                EapGuiTask.UNDEPLOY.label(),
                "rm",
                List.of("-f", VscodeEapSettings.DEPLOYED_WAR + "*"),
                "${workspaceFolder}",
                false
        );
    }

    private static List<String> startArgs(EapSettings eap, boolean debug) {
        List<String> args = new ArrayList<>();
        if (debug) {
            args.add("--debug");
            args.add(VscodeEapSettings.DEBUG_PORT);
        }
        args.add("-c");
        args.add(VscodeEapSettings.CONFIG_FILE);
        args.add("-b");
        args.add(VscodeEapSettings.BIND_ADDRESS);
        args.add("-bmanagement");
        args.add(VscodeEapSettings.MANAGEMENT_HOST);
        args.add("-Djboss.http.port=" + VscodeEapSettings.HTTP_PORT);
        args.add("-Djboss.management.http.port=" + VscodeEapSettings.MANAGEMENT_PORT);
        args.addAll(EapService.tokenize(eap.getExtraArgs()));
        return args;
    }

    private static List<String> cliArgs(JbossCliTask cliTask, WarProject project) {
        return List.of(
                "--connect",
                "--controller=" + VscodeEapSettings.MANAGEMENT_HOST + ":" + VscodeEapSettings.MANAGEMENT_PORT,
                "--command=" + cliCommand(cliTask, project)
        );
    }

    private static String cliCommand(JbossCliTask cliTask, WarProject project) {
        if (cliTask == JbossCliTask.DEPLOY) {
            return "deploy \"" + VscodeEapSettings.WORKSPACE_WAR + "\" --name="
                    + VscodeEapSettings.DEPLOY_NAME + " --force";
        }
        if (cliTask == JbossCliTask.UNDEPLOY) {
            return "undeploy " + VscodeEapSettings.DEPLOY_NAME;
        }
        return JbossCliCommands.cliCommand(cliTask, project);
    }
}
