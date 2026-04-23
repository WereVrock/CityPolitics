package ui;

import main.calendar.GameCalendar;
import main.core.GameState;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;

/**
 * Displays the current date and the Frost Giant countdown.
 */
public class CalendarPanel extends JPanel {

    private final GameState gameState;

    private final JLabel dateLabel;
    private final JLabel countdownLabel;

    public CalendarPanel(GameState gameState) {
        this.gameState = gameState;

        setBackground(UITheme.BG_PANEL);
        setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, UITheme.BORDER_COLOR),
            new EmptyBorder(10, 16, 10, 16)
        ));
        setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("❄  FROSTVEIL");
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.ACCENT_FROST);

        dateLabel = new JLabel();
        dateLabel.setFont(UITheme.FONT_BODY);
        dateLabel.setForeground(UITheme.TEXT_PRIMARY);

        countdownLabel = new JLabel();
        countdownLabel.setFont(UITheme.FONT_BODY);
        countdownLabel.setForeground(UITheme.TEXT_RED);

        JPanel rightPanel = new JPanel(new GridLayout(2, 1));
        rightPanel.setBackground(UITheme.BG_PANEL);
        rightPanel.add(dateLabel);
        rightPanel.add(countdownLabel);

        add(titleLabel,  BorderLayout.WEST);
        add(rightPanel,  BorderLayout.EAST);

        refresh();
    }

    public void refresh() {
        GameCalendar cal = gameState.getCalendar();
        dateLabel.setText(cal.getDisplayString());

        int turns = cal.getTurnsUntilFrostGiants();
        if (turns <= 0) {
            countdownLabel.setText("⚠  THE FROST GIANTS ARE HERE");
            countdownLabel.setForeground(UITheme.TEXT_RED);
        } else {
            countdownLabel.setText("Frost Giants arrive in " + turns + " period(s)");
            countdownLabel.setForeground(turns <= 4 ? UITheme.TEXT_RED : UITheme.TEXT_SECONDARY);
        }
    }
}