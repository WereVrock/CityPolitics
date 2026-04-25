package ui;

import main.calendar.GameCalendar;
import main.core.GameState;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.function.Supplier;

/**
 * Displays the current date, Frost Giant countdown, and End Turn button.
 */
public class CalendarPanel extends JPanel {

    private final GameState         gameState;
    private final JLabel            dateLabel;
    private final JLabel            countdownLabel;
    private       JButton           endTurnBtn;
    private       Runnable          onEndTurn;
    private       Supplier<Boolean> isBlocked;

    public CalendarPanel(GameState gameState) {
        this.gameState = gameState;
        this.isBlocked = () -> false;

        setBackground(UITheme.BG_PANEL);
        setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, UITheme.BORDER_COLOR),
            new EmptyBorder(8, 16, 8, 16)
        ));
        setLayout(new BorderLayout(16, 0));

        JLabel titleLabel = new JLabel("❄  FROSTVEIL");
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.ACCENT_FROST);

        dateLabel = new JLabel();
        dateLabel.setFont(UITheme.FONT_BODY);
        dateLabel.setForeground(UITheme.TEXT_PRIMARY);
        dateLabel.setToolTipText(buildCalendarTooltip());

        countdownLabel = new JLabel();
        countdownLabel.setFont(UITheme.FONT_BODY);
        countdownLabel.setForeground(UITheme.TEXT_RED);
        countdownLabel.setToolTipText("<html>The Frost Giants are a primordial force from beyond the northern ice.<br>"
            + "They march on a cycle tied to the ancient calendar — when the year turns past 200 A.S.,<br>"
            + "their vanguard reaches the realm's borders. Prepare or perish.</html>");

        JPanel dateBlock = new JPanel(new GridLayout(2, 1, 0, 2));
        dateBlock.setBackground(UITheme.BG_PANEL);
        dateBlock.add(dateLabel);
        dateBlock.add(countdownLabel);

        endTurnBtn = new JButton("END TURN  ▶");
        endTurnBtn.setFont(new Font("Serif", Font.BOLD, 14));
        endTurnBtn.setForeground(UITheme.ACCENT_FROST);
        endTurnBtn.setBackground(new Color(25, 45, 65));
        endTurnBtn.setBorderPainted(false);
        endTurnBtn.setFocusPainted(false);
        endTurnBtn.setPreferredSize(new Dimension(160, 44));
        endTurnBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        endTurnBtn.addActionListener(e -> { if (onEndTurn != null) onEndTurn.run(); });

        add(titleLabel, BorderLayout.WEST);
        add(dateBlock,  BorderLayout.CENTER);
        add(endTurnBtn, BorderLayout.EAST);

        refresh();
    }

    public void setEndTurnCallback(Runnable onEndTurn)          { this.onEndTurn = onEndTurn; }
    public void setBlockedSupplier(Supplier<Boolean> supplier)  { this.isBlocked = supplier; }

    public void updateEndTurnState(boolean blocked, boolean hasPendingVote) {
        endTurnBtn.setEnabled(!blocked);
        endTurnBtn.setBackground(blocked ? UITheme.BUTTON_DISABLED : new Color(25, 45, 65));
        endTurnBtn.setText(hasPendingVote ? "VOTE PENDING  ⚠" : "END TURN  ▶");
        endTurnBtn.setForeground(hasPendingVote ? UITheme.TEXT_GOLD : UITheme.ACCENT_FROST);
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

    private String buildCalendarTooltip() {
        return "<html>"
            + "<b>The Calendar of the Sundering</b><br><br>"
            + "Time is measured in years <i>After the Sundering</i> (A.S.) — the cataclysmic event<br>"
            + "that shattered the old empire and scattered its peoples across the known world.<br><br>"
            + "Each year is divided into two Periods:<br>"
            + "&nbsp;&nbsp;<b>The Thaw</b> — the warmer half-year; trade opens, harvests begin.<br>"
            + "&nbsp;&nbsp;<b>The Frost</b> — the cold half-year; resources strain, spirits falter.<br><br>"
            + "The campaign begins in Year 184 A.S. The Frost Giants are expected to reach<br>"
            + "the realm's northern borders by Year 200 A.S. You have 32 periods to prepare."
            + "</html>";
    }
}