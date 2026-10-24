package com.eap.management.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AppConfig {
    private EapSettings eap = new EapSettings();
    private String selectedProjectId = "";
    private String lastDeployedProjectId = "";
    private List<WarProject> projects = new ArrayList<>();
    private List<EnvVar> globalEnv = new ArrayList<>();
    private UiSettings ui = new UiSettings();

    public Optional<WarProject> findProject(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return getProjects().stream().filter(project -> id.equals(project.getId())).findFirst();
    }

    public Optional<WarProject> selectedProject() {
        Optional<WarProject> selected = findProject(selectedProjectId);
        if (selected.isPresent()) {
            return selected;
        }
        return getProjects().isEmpty() ? Optional.empty() : Optional.of(getProjects().getFirst());
    }

    public EapSettings getEap() {
        if (eap == null) {
            eap = new EapSettings();
        }
        return eap;
    }

    public void setEap(EapSettings eap) {
        this.eap = eap == null ? new EapSettings() : eap;
    }

    public String getSelectedProjectId() {
        return selectedProjectId;
    }

    public void setSelectedProjectId(String selectedProjectId) {
        this.selectedProjectId = selectedProjectId == null ? "" : selectedProjectId;
    }

    public String getLastDeployedProjectId() {
        return lastDeployedProjectId;
    }

    public void setLastDeployedProjectId(String lastDeployedProjectId) {
        this.lastDeployedProjectId = lastDeployedProjectId == null ? "" : lastDeployedProjectId;
    }

    public List<WarProject> getProjects() {
        if (projects == null) {
            projects = new ArrayList<>();
        }
        return projects;
    }

    public void setProjects(List<WarProject> projects) {
        this.projects = projects == null ? new ArrayList<>() : projects;
    }

    public List<EnvVar> getGlobalEnv() {
        if (globalEnv == null) {
            globalEnv = new ArrayList<>();
        }
        return globalEnv;
    }

    public void setGlobalEnv(List<EnvVar> globalEnv) {
        this.globalEnv = globalEnv == null ? new ArrayList<>() : globalEnv;
    }

    public UiSettings getUi() {
        if (ui == null) {
            ui = new UiSettings();
        }
        return ui;
    }

    public void setUi(UiSettings ui) {
        this.ui = ui == null ? new UiSettings() : ui;
    }

    public static class UiSettings {
        private boolean darkTheme;
        private int windowWidth = 1280;
        private int windowHeight = 840;

        public boolean isDarkTheme() {
            return darkTheme;
        }

        public void setDarkTheme(boolean darkTheme) {
            this.darkTheme = darkTheme;
        }

        public int getWindowWidth() {
            return windowWidth <= 0 ? 1280 : windowWidth;
        }

        public void setWindowWidth(int windowWidth) {
            this.windowWidth = windowWidth;
        }

        public int getWindowHeight() {
            return windowHeight <= 0 ? 840 : windowHeight;
        }

        public void setWindowHeight(int windowHeight) {
            this.windowHeight = windowHeight;
        }
    }
}
