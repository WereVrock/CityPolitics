package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;

/**
 * Scrollable text log of game events and action results.
 */
public class EventLogPanel extends JPanel {

    private final JTextArea logArea;

    public EventLogPanel() {
        setBackground(UITheme.LOG_BG);
        setBorder(new MatteBorder(1, 0, 0, 0, UITheme.BORDER_COLOR));
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(0, 140));

        JLabel header = new JLabel("  EVENT LOG");
        header.setFont(UITheme.FONT_HEADER);
        header.setForeground(UITheme.TEXT_GOLD);
        header.setBorder(new EmptyBorder(6, 8, 4, 8));
        header.setBackground(UITheme.BG_PANEL);
        header.setOpaque(true);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(UITheme.LOG_BG);
        logArea.setForeground(UITheme.TEXT_PRIMARY);
        logArea.setFont(UITheme.FONT_SMALL);
        logArea.setBorder(new EmptyBorder(4, 8, 4, 8));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(null);
        scroll.setBackground(UITheme.LOG_BG);
        scroll.getViewport().setBackground(UITheme.LOG_BG);
        scroll.getVerticalScrollBar().setBackground(UITheme.BG_PANEL);

        add(header, BorderLayout.NORTH);
        add(scroll,  BorderLayout.CENTER);
    }

    public void clear() {
        logArea.setText("");
    }

    public void appendLine(String line) {
        logArea.append(line + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public void appendLines(java.util.List<String> lines) {
        for (String line : lines) appendLine(line);
    }
}