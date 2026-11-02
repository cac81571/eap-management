package com.eap.management.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import com.eap.management.process.LogLevel;
import com.eap.management.process.LogListener;
import com.formdev.flatlaf.FlatLaf;

public class LogPanel extends JPanel implements LogListener {
    private static final int MAX_LINES = 4000;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Font LOG_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 13);

    private final ArrayDeque<LogEntry> entries = new ArrayDeque<>();
    private final JTextPane area = new JTextPane();
    private final JScrollPane scroll = new JScrollPane(area);
    private final JTextField filter = new JTextField();
    private final JLabel hits = new JLabel(" ");
    private String filterText = "";

    public LogPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 16));
        area.setEditable(false);
        area.setFont(LOG_FONT);

        filter.putClientProperty("JTextField.placeholderText", "ログを検索");
        filter.putClientProperty("JTextField.showClearButton", true);
        filter.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });
        registerKeyboardAction(new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                filter.requestFocusInWindow();
                filter.selectAll();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);

        JButton clear = new JButton("クリア");
        clear.addActionListener(e -> clear());

        JPanel tools = new JPanel(new BorderLayout(8, 0));
        tools.add(filter, BorderLayout.CENTER);
        JPanel east = new JPanel(new BorderLayout(8, 0));
        east.add(hits, BorderLayout.CENTER);
        east.add(clear, BorderLayout.EAST);
        tools.add(east, BorderLayout.EAST);

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.add(new JLabel("ログ"), BorderLayout.WEST);
        header.add(tools, BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        updateHitLabel(0, 0);
    }

    @Override
    public void append(String source, String line) {
        String text = LocalTime.now().format(TIME) + " [" + source + "] " + line + "\n";
        LogEntry entry = new LogEntry(text, LogLevel.detect(line));
        runOnEdt(() -> addEntry(entry));
    }

    public void clear() {
        entries.clear();
        area.setText("");
        updateHitLabel(0, 0);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (area != null) {
            SwingUtilities.invokeLater(this::rebuild);
        }
    }

    private void addEntry(LogEntry entry) {
        entries.addLast(entry);
        LogEntry removed = entries.size() > MAX_LINES ? entries.removeFirst() : null;
        if (removed != null && !filterText.isEmpty()) {
            rebuild();
            return;
        }
        boolean follow = isFollowTail();
        if (removed != null) {
            removeFirstDisplayedLine();
        }
        if (matches(entry)) {
            insert(entry);
            if (follow) {
                followTail();
            }
        }
        updateHitLabel(visibleCount(), entries.size());
    }

    private void removeFirstDisplayedLine() {
        StyledDocument doc = area.getStyledDocument();
        Element first = doc.getDefaultRootElement().getElement(0);
        if (first == null) {
            return;
        }
        try {
            doc.remove(first.getStartOffset(), first.getEndOffset() - first.getStartOffset());
        } catch (BadLocationException ignored) {
            // keep remaining lines
        }
    }

    private void applyFilter() {
        filterText = filter.getText() == null ? "" : filter.getText().trim();
        rebuild();
    }

    private void rebuild() {
        if (area == null || filter == null || hits == null) {
            return;
        }
        try {
            boolean follow = filterText.isEmpty() && isFollowTail();
            StyledDocument doc = area.getStyledDocument();
            try {
                doc.remove(0, doc.getLength());
            } catch (BadLocationException ignored) {
                area.setText("");
            }
            int visible = 0;
            for (LogEntry entry : entries) {
                if (matches(entry)) {
                    insert(entry);
                    visible++;
                }
            }
            updateHitLabel(visible, entries.size());
            if (follow) {
                followTail();
            } else {
                setCaretSafely(0);
            }
        } catch (RuntimeException ignored) {
            // L&F 更新中の再描画失敗はアプリを落とさない
        }
    }

    private void insert(LogEntry entry) {
        StyledDocument doc = area.getStyledDocument();
        try {
            doc.insertString(doc.getLength(), entry.text, attributes(entry.level));
        } catch (BadLocationException ignored) {
            // skip malformed insert
        }
    }

    private boolean matches(LogEntry entry) {
        if (filterText.isEmpty()) {
            return true;
        }
        return entry.text.toLowerCase(Locale.ROOT).contains(filterText.toLowerCase(Locale.ROOT));
    }

    private int visibleCount() {
        if (filterText.isEmpty()) {
            return entries.size();
        }
        int count = 0;
        for (LogEntry entry : entries) {
            if (matches(entry)) {
                count++;
            }
        }
        return count;
    }

    private void updateHitLabel(int visible, int total) {
        if (filterText.isEmpty()) {
            hits.setText(" ");
            hits.setToolTipText(null);
            return;
        }
        hits.setText(visible + " / " + total);
        hits.setToolTipText("フィルタ一致 " + visible + " 件 / 全 " + total + " 件");
    }

    private boolean isFollowTail() {
        javax.swing.JScrollBar bar = scroll.getVerticalScrollBar();
        return bar.getValue() + bar.getVisibleAmount() >= bar.getMaximum() - 24;
    }

    private void followTail() {
        setCaretSafely(area.getDocument().getLength());
    }

    private void setCaretSafely(int position) {
        try {
            int max = area.getDocument().getLength();
            area.setCaretPosition(Math.max(0, Math.min(position, max)));
        } catch (IllegalArgumentException ignored) {
            // ignore invalid caret during document rebuild
        }
    }

    private SimpleAttributeSet attributes(LogLevel level) {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setFontFamily(attrs, LOG_FONT.getFamily());
        StyleConstants.setFontSize(attrs, LOG_FONT.getSize());
        StyleConstants.setForeground(attrs, colorFor(level));
        return attrs;
    }

    private Color colorFor(LogLevel level) {
        boolean dark = FlatLaf.isLafDark();
        return switch (level) {
            case ERROR -> dark ? new Color(0xFF8A80) : new Color(0xC62828);
            case WARN -> dark ? new Color(0xFFD180) : new Color(0xE65100);
            case INFO -> dark ? new Color(0x90CAF9) : new Color(0x1565C0);
            case DEBUG -> dark ? new Color(0xBDBDBD) : new Color(0x757575);
            case OTHER -> {
                Color fallback = UIManager.getColor("TextPane.foreground");
                if (fallback == null) {
                    fallback = UIManager.getColor("Label.foreground");
                }
                yield fallback != null ? fallback : (dark ? Color.LIGHT_GRAY : Color.DARK_GRAY);
            }
        };
    }

    private void runOnEdt(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    private record LogEntry(String text, LogLevel level) {
    }
}
