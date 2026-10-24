package com.eap.management.config;

import java.nio.file.Files;
import java.nio.file.Path;

import com.eap.management.process.Os;

public class EapSettings {
    private String home = "";
    private String configFile = "standalone.xml";
    private String bindAddress = "0.0.0.0";
    private int httpPort = 8080;
    private int managementPort = 9990;
    private String extraArgs = "";
    private String consoleEncoding = "AUTO";
    private DeployMode deployMode = DeployMode.DIRECTORY;

    public Path homePath() {
        return home == null || home.isBlank() ? null : Path.of(home.trim());
    }

    public Path binDir() {
        Path homePath = homePath();
        return homePath == null ? null : homePath.resolve("bin");
    }

    public Path standaloneScript() {
        Path bin = binDir();
        if (bin == null) {
            return null;
        }
        return bin.resolve(Os.isWindows() ? "standalone.bat" : "standalone.sh");
    }

    public Path cliScript() {
        Path bin = binDir();
        if (bin == null) {
            return null;
        }
        return bin.resolve(Os.isWindows() ? "jboss-cli.bat" : "jboss-cli.sh");
    }

    public Path deploymentsDir() {
        Path homePath = homePath();
        return homePath == null ? null : homePath.resolve("standalone").resolve("deployments");
    }

    public Path configurationDir() {
        Path homePath = homePath();
        return homePath == null ? null : homePath.resolve("standalone").resolve("configuration");
    }

    public Path configFilePath() {
        String name = getConfigFile();
        Path specified = Path.of(name);
        if (specified.isAbsolute()) {
            return specified;
        }
        Path dir = configurationDir();
        return dir == null ? specified : dir.resolve(name);
    }

    public boolean looksInstalled() {
        Path script = standaloneScript();
        return script != null && Files.isRegularFile(script);
    }

    public String managementHost() {
        String bind = bindAddress == null || bindAddress.isBlank() ? "127.0.0.1" : bindAddress.trim();
        if ("0.0.0.0".equals(bind) || "::".equals(bind)) {
            return "127.0.0.1";
        }
        return bind;
    }

    public String httpBaseUrl() {
        return "http://localhost:" + httpPort + "/";
    }

    public String applicationUrl(String contextRoot) {
        String base = httpBaseUrl();
        if (contextRoot == null || contextRoot.isBlank()) {
            return base;
        }
        String ctx = contextRoot.startsWith("/") ? contextRoot.substring(1) : contextRoot;
        return base + ctx;
    }

    public String getHome() {
        return home;
    }

    public void setHome(String home) {
        this.home = home == null ? "" : home;
    }

    public String getConfigFile() {
        return configFile;
    }

    public void setConfigFile(String configFile) {
        this.configFile = configFile == null || configFile.isBlank() ? "standalone.xml" : configFile;
    }

    public String getBindAddress() {
        return bindAddress;
    }

    public void setBindAddress(String bindAddress) {
        this.bindAddress = bindAddress == null || bindAddress.isBlank() ? "0.0.0.0" : bindAddress;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public int getManagementPort() {
        return managementPort;
    }

    public void setManagementPort(int managementPort) {
        this.managementPort = managementPort;
    }

    public String getExtraArgs() {
        return extraArgs;
    }

    public void setExtraArgs(String extraArgs) {
        this.extraArgs = extraArgs == null ? "" : extraArgs;
    }

    public String getConsoleEncoding() {
        return consoleEncoding == null || consoleEncoding.isBlank() ? "AUTO" : consoleEncoding;
    }

    public void setConsoleEncoding(String consoleEncoding) {
        this.consoleEncoding = consoleEncoding == null || consoleEncoding.isBlank() ? "AUTO" : consoleEncoding;
    }

    public DeployMode getDeployMode() {
        return deployMode == null ? DeployMode.DIRECTORY : deployMode;
    }

    public void setDeployMode(DeployMode deployMode) {
        this.deployMode = deployMode == null ? DeployMode.DIRECTORY : deployMode;
    }
}
