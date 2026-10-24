package com.eap.management.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EnvVar {
    private boolean enabled = true;
    private String key = "";
    private String value = "";

    public EnvVar() {
    }

    public EnvVar(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public EnvVar copy() {
        EnvVar copy = new EnvVar();
        copy.enabled = enabled;
        copy.key = key;
        copy.value = value;
        return copy;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key == null ? "" : key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value == null ? "" : value;
    }

    public static Map<String, String> merge(List<EnvVar> global, List<EnvVar> project) {
        Map<String, String> map = new LinkedHashMap<>();
        putEnabled(map, global);
        putEnabled(map, project);
        return map;
    }

    private static void putEnabled(Map<String, String> map, List<EnvVar> vars) {
        if (vars == null) {
            return;
        }
        for (EnvVar var : vars) {
            if (var == null || !var.enabled) {
                continue;
            }
            String name = var.key == null ? "" : var.key.trim();
            if (name.isEmpty()) {
                continue;
            }
            map.put(name, var.value == null ? "" : var.value);
        }
    }
}
