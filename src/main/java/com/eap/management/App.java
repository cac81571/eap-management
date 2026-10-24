package com.eap.management;

import javax.swing.SwingUtilities;

import com.eap.management.config.AppConfig;
import com.eap.management.config.ConfigStore;
import com.eap.management.ui.MainFrame;
import com.eap.management.ui.Theme;
import com.eap.management.ui.UiUtils;

public final class App {
    private App() {
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> {
            try {
                ConfigStore store = new ConfigStore();
                AppConfig config = store.load();
                Theme.apply(config.getUi().isDarkTheme());
                AppController controller = new AppController(store, config);
                MainFrame frame = new MainFrame(controller, config);
                controller.bind(frame);
                frame.setVisible(true);
            } catch (Exception e) {
                UiUtils.error(null, "起動に失敗しました。", e);
            }
        });
    }
}
