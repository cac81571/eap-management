package com.eap.management;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

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
        Thread.setDefaultUncaughtExceptionHandler(App::handleCrash);
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
                handleCrash(Thread.currentThread(), e);
                UiUtils.error(null, "起動に失敗しました。", e);
            }
        });
    }

    private static void handleCrash(Thread thread, Throwable error) {
        error.printStackTrace();
        try {
            Path log = ConfigStore.defaultFile().getParent().resolve("crash.log");
            Files.createDirectories(log.getParent());
            StringWriter body = new StringWriter();
            body.write(LocalDateTime.now() + " [" + thread.getName() + "]\n");
            error.printStackTrace(new PrintWriter(body));
            body.write("\n");
            Files.writeString(log, body.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
            // keep going
        }
    }
}
