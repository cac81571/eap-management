package com.eap.management.ui;

import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

public final class Theme {
    private Theme() {
    }

    public static void apply(boolean dark) {
        if (dark) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        UIManager.put("Button.arc", 10);
        UIManager.put("Component.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("ProgressBar.arc", 10);
        UIManager.put("TitlePane.unifiedBackground", true);
        FlatLaf.updateUI();
    }
}
