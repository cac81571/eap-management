package com.eap.management.config;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class WarProject {
    private String id = UUID.randomUUID().toString();
    private String name = "";
    private String projectDir = "";
    private String buildCommand = "mvn -DskipTests package";
    private String warPath = "target/*.war";
    private String deployName = "";
    private String appUrl = "";
    private List<EnvVar> env = new ArrayList<>();

    public Optional<Path> resolveWarFile() throws IOException {
        if (projectDir == null || projectDir.isBlank() || warPath == null || warPath.isBlank()) {
            return Optional.empty();
        }
        Path base = Path.of(projectDir.trim()).toAbsolutePath().normalize();
        String raw = warPath.trim().replace('\\', '/');
        if (raw.contains("*") || raw.contains("?")) {
            return newestMatch(base, raw);
        }
        Path candidate = Path.of(warPath.trim());
        if (!candidate.isAbsolute()) {
            candidate = base.resolve(warPath.trim());
        }
        candidate = candidate.normalize();
        if (Files.isRegularFile(candidate)) {
            return Optional.of(candidate);
        }
        if (Files.isDirectory(candidate)) {
            return newestMatch(candidate, "*.war");
        }
        return Optional.empty();
    }

    public String resolvedDeployName(Path warFile) {
        if (deployName != null && !deployName.isBlank()) {
            String value = deployName.trim();
            return value.endsWith(".war") ? value : value + ".war";
        }
        return warFile.getFileName().toString();
    }

    public String contextRoot(Path warFile) {
        String artifact = resolvedDeployName(warFile);
        return artifact.endsWith(".war") ? artifact.substring(0, artifact.length() - 4) : artifact;
    }

    public String suggestedContextRoot() {
        if (deployName != null && !deployName.isBlank()) {
            String value = deployName.trim();
            return value.endsWith(".war") ? value.substring(0, value.length() - 4) : value;
        }
        try {
            return resolveWarFile().map(this::contextRoot).orElse("");
        } catch (IOException e) {
            return "";
        }
    }

    public String resolveAppUrl(String httpBaseUrl) {
        String registered = appUrl == null ? "" : appUrl.trim();
        if (registered.startsWith("http://") || registered.startsWith("https://")) {
            return registered;
        }
        String path = registered.isEmpty() ? suggestedContextRoot() : registered;
        return joinUrl(httpBaseUrl, path);
    }

    static String joinUrl(String httpBaseUrl, String path) {
        String base = httpBaseUrl == null || httpBaseUrl.isBlank() ? "http://localhost:8080/" : httpBaseUrl;
        if (path == null || path.isBlank()) {
            return base.endsWith("/") ? base : base + "/";
        }
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        String relative = path.startsWith("/") ? path.substring(1) : path;
        return base + relative;
    }

    public Path projectPath() {
        return projectDir == null || projectDir.isBlank() ? null : Path.of(projectDir.trim());
    }

    private static Optional<Path> newestMatch(Path base, String relativeGlob) throws IOException {
        String normalized = relativeGlob.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String glob = slash < 0 ? normalized : normalized.substring(slash + 1);
        Path dir = base;
        if (slash >= 0) {
            Path maybe = Path.of(normalized.substring(0, slash));
            dir = maybe.isAbsolute() ? maybe : base.resolve(maybe);
        }
        if (!Files.isDirectory(dir)) {
            return Optional.empty();
        }
        List<Path> matches = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, glob)) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    matches.add(path);
                }
            }
        }
        return matches.stream().max(Comparator.comparingLong(WarProject::lastModified));
    }

    private static long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name;
    }

    public String getProjectDir() {
        return projectDir;
    }

    public void setProjectDir(String projectDir) {
        this.projectDir = projectDir == null ? "" : projectDir;
    }

    public String getBuildCommand() {
        return buildCommand;
    }

    public void setBuildCommand(String buildCommand) {
        this.buildCommand = buildCommand == null || buildCommand.isBlank()
                ? "mvn -DskipTests package"
                : buildCommand;
    }

    public String getWarPath() {
        return warPath;
    }

    public void setWarPath(String warPath) {
        this.warPath = warPath == null || warPath.isBlank() ? "target/*.war" : warPath;
    }

    public String getDeployName() {
        return deployName;
    }

    public void setDeployName(String deployName) {
        this.deployName = deployName == null ? "" : deployName;
    }

    public String getAppUrl() {
        return appUrl;
    }

    public void setAppUrl(String appUrl) {
        this.appUrl = appUrl == null ? "" : appUrl;
    }

    public List<EnvVar> getEnv() {
        if (env == null) {
            env = new ArrayList<>();
        }
        return env;
    }

    public void setEnv(List<EnvVar> env) {
        this.env = env == null ? new ArrayList<>() : env;
    }

    @Override
    public String toString() {
        return name == null || name.isBlank() ? "(無題)" : name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WarProject that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
