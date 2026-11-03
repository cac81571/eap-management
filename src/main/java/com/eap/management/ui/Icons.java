package com.eap.management.ui;

import java.awt.Color;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.UIManager;

import com.formdev.flatlaf.extras.FlatSVGIcon;

final class Icons {
    private static final int SIZE = 16;
    private static final Map<String, FlatSVGIcon> CACHE = new ConcurrentHashMap<>();

    private Icons() {
    }

    static Icon play() {
        return load("play");
    }

    static Icon bug() {
        return load("bug");
    }

    static Icon stop() {
        return load("stop");
    }

    static Icon restart() {
        return load("restart");
    }

    static Icon build() {
        return load("build");
    }

    static Icon deploy() {
        return load("deploy");
    }

    static Icon buildDeploy() {
        return load("build-deploy");
    }

    static Icon folder() {
        return load("folder");
    }

    static Icon file() {
        return load("file");
    }

    static Icon tasks() {
        return load("tasks");
    }

    static Icon open() {
        return load("open");
    }

    static Icon browser() {
        return load("browser");
    }

    static Icon settings() {
        return load("settings");
    }

    static Icon moon() {
        return load("moon");
    }

    static Icon plus() {
        return load("plus");
    }

    static Icon edit() {
        return load("edit");
    }

    static Icon trash() {
        return load("trash");
    }

    static Icon clear() {
        return load("clear");
    }

    static void apply(AbstractButton button, Icon icon) {
        if (button == null || icon == null) {
            return;
        }
        button.setIcon(icon);
        if (!(button instanceof JMenuItem)) {
            button.setIconTextGap(6);
        }
    }

    private static Icon load(String name) {
        return CACHE.computeIfAbsent(name, Icons::create);
    }

    private static FlatSVGIcon create(String name) {
        FlatSVGIcon icon = new FlatSVGIcon("icons/" + name + ".svg", SIZE, SIZE);
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> {
            Color foreground = UIManager.getColor("Button.foreground");
            return foreground == null ? color : foreground;
        }));
        return icon;
    }
}
