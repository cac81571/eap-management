package com.eap.management.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.eap.management.config.EapSettings;
import com.eap.management.config.WarProject;

class JbossCliCommandsTest {
    @Test
    void shutdownUsesControllerAndCommand() {
        EapSettings eap = new EapSettings();
        eap.setHome("C:/jboss/jboss-eap-8.1");
        eap.setManagementPort(9990);
        List<String> args = JbossCliCommands.arguments(JbossCliTask.SHUTDOWN, eap, null);
        assertEquals("--connect", args.get(0));
        assertEquals("--controller=127.0.0.1:9990", args.get(1));
        assertEquals("--command=:shutdown", args.get(2));
    }

    @Test
    void deployUsesWorkspaceWarAndRuntimeName() {
        EapSettings eap = new EapSettings();
        eap.setHome("C:/eap");
        WarProject project = new WarProject();
        project.setProjectDir("C:/work/springmvc-probe");
        project.setWarPath("target/*.war");
        project.setDeployName("shop");
        assertEquals("deploy \"${workspaceFolder}/target/shop.war\" --name=shop.war --force",
                JbossCliCommands.cliCommand(JbossCliTask.DEPLOY, project));
        assertEquals("undeploy shop.war", JbossCliCommands.cliCommand(JbossCliTask.UNDEPLOY, project));
        String relative = JbossCliCommands.workspaceWarRelative(project);
        assertEquals(Os.isWindows() ? "target\\shop.war" : "target/shop.war", relative);
    }

    @Test
    void startUsesStandaloneDebugArgs() {
        EapSettings eap = new EapSettings();
        eap.setDebugPort(8787);
        List<String> args = JbossCliCommands.arguments(JbossCliTask.START_DEBUG, eap, null);
        assertEquals("--debug", args.get(0));
        assertEquals("8787", args.get(1));
        assertTrue(args.contains("-c"));
    }
}
