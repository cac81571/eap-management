package com.eap.management.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.eap.management.process.LogListener;

public class LogPanel extends JPanel implements LogListener {
    private static final int MAX_LINES = 4000;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final JTextArea area = new JTextArea();
    private int lines;

    public LogPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 16));
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);

        JButton clear = new JButton("クリア");
        clear.addActionListener(e -> clear());

        JPanel header = new JPanel(new BorderLayout());
        header.add(new JLabel("ログ"), BorderLayout.WEST);
        header.add(clear, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        add(new JScrollPane(area), BorderLayout.CENTER);
    }

    @Override
    public void append(String source, String line) {
        String text = LocalTime.now().format(TIME) + " [" + source + "] " + line + System.lineSeparator();
        Runnable append = () -> {
            area.append(text);
            lines++;
            if (lines > MAX_LINES) {
                String content = area.getText();
                int cut = content.indexOf('\n');
                if (cut >= 0) {
                    area.replaceRange("", 0, cut + 1);
                    lines--;
                }
            }
            area.setCaretPosition(area.getDocument().getLength());
        };
        if (SwingUtilities.isEventDispatchThread()) {
            append.run();
        } else {
            SwingUtilities.invokeLater(append);
        }
    }

    public void clear() {
        area.setText("");
        lines = 0;
    }
}
