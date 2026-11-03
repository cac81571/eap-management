package com.eap.management.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        assertFalse(args.stream().anyMatch(arg -> arg.startsWith("--user=")));
        assertFalse(args.stream().anyMatch(arg -> arg.startsWith("--password=")));
    }

    @Test
    void appendsAuthWhenManagementUserAndPasswordAreSet() {
        EapSettings eap = new EapSettings();
        eap.setHome("C:/eap");
        eap.setManagementUser("admin");
        eap.setManagementPassword("secret");
        List<String> args = JbossCliCommands.arguments(JbossCliTask.SHUTDOWN, eap, null);
        assertEquals("--connect", args.get(0));
        assertEquals("--controller=127.0.0.1:9990", args.get(1));
        assertEquals("--user=admin", args.get(2));
        assertEquals("--password=secret", args.get(3));
        assertEquals("--command=:shutdown", args.get(4));
        assertTrue(eap.hasManagementCredentials());
    }

    @Test
    void skipsAuthWhenPasswordMissing() {
        EapSettings eap = new EapSettings();
        eap.setHome("C:/eap");
        eap.setManagementUser("admin");
        eap.setManagementPassword("");
        List<String> args = JbossCliCommands.arguments(JbossCliTask.SHUTDOWN, eap, null);
        assertFalse(eap.hasManagementCredentials());
        assertFalse(args.stream().anyMatch(arg -> arg.startsWith("--user=")));
        assertFalse(args.stream().anyMatch(arg -> arg.startsWith("--password=")));
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
