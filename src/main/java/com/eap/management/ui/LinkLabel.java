package com.eap.management.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import com.eap.management.process.Os;

final class LinkLabel extends JLabel {
    private static final Color LINK = new Color(30, 110, 210);
    private static final Color LINK_HOVER = new Color(20, 80, 180);

    private String url = "";

    LinkLabel() {
        setForeground(LINK);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e) || url.isBlank()) {
                    return;
                }
                open();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!url.isBlank()) {
                    setForeground(LINK_HOVER);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setForeground(LINK);
            }
        });
        clear();
    }

    void setLink(String label, String targetUrl) {
        if (targetUrl == null || targetUrl.isBlank()) {
            clear();
            return;
        }
        this.url = targetUrl.trim();
        String text = label == null || label.isBlank() ? this.url : label;
        setText("<html><u>" + escape(text) + "</u></html>");
        setToolTipText(this.url);
        setEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setForeground(LINK);
    }

    void clear() {
        this.url = "";
        setText(" ");
        setToolTipText(null);
        setCursor(Cursor.getDefaultCursor());
        setEnabled(false);
    }

    private void open() {
        try {
            Os.browse(url);
        } catch (Exception e) {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI.create(url));
                    return;
                }
            } catch (Exception ignored) {
                // fall through
            }
            UiUtils.error(this, "URL を開けませんでした: " + url, e);
        }
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
