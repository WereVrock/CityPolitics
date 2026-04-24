package ui;

import main.politics.VoteResult;
import main.politics.VoteScore;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Modal dialog displaying the full assembly vote breakdown.
 */
public class VoteResultPanel extends JDialog {

    public VoteResultPanel(JFrame owner, String actionName, VoteResult result) {
        super(owner, "Assembly Vote — " + actionName, true);

        setBackground(UITheme.BG_DARK);
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(420, 320));
        setLocationRelativeTo(owner);

        add(buildHeader(result), BorderLayout.NORTH);
        add(buildBreakdown(result), BorderLayout.CENTER);
        add(buildCloseButton(), BorderLayout.SOUTH);

        pack();
    }

    private JPanel buildHeader(VoteResult result) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BG_PANEL);
        panel.setBorder(new EmptyBorder(12, 16, 12, 16));

        String outcome = result.isPassed() ? "✓  PASSED" : "✗  REJECTED";
        Color  color   = result.isPassed() ? UITheme.TEXT_GREEN : UITheme.TEXT_RED;

        JLabel outcomeLabel = new JLabel(outcome);
        outcomeLabel.setFont(UITheme.FONT_TITLE);
        outcomeLabel.setForeground(color);

        JLabel totalsLabel = new JLabel(
            "YES: " + result.getTotalYes()
            + "   NO: " + result.getTotalNo()
            + "   ABSTAIN: " + result.getTotalAbstain()
            + "   (needed: " + result.getSeatsNeeded() + ")"
        );
        totalsLabel.setFont(UITheme.FONT_BODY);
        totalsLabel.setForeground(UITheme.TEXT_SECONDARY);

        panel.add(outcomeLabel, BorderLayout.NORTH);
        panel.add(totalsLabel,  BorderLayout.SOUTH);
        return panel;
    }

    private JScrollPane buildBreakdown(VoteResult result) {
        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setBackground(UITheme.BG_DARK);
        rows.setBorder(new EmptyBorder(8, 16, 8, 16));

        for (VoteScore score : result.getPartyScores()) {
            JLabel label = new JLabel(score.getSummary());
            label.setFont(UITheme.FONT_SMALL);
            label.setForeground(UITheme.TEXT_PRIMARY);
            label.setBorder(new EmptyBorder(3, 0, 3, 0));
            rows.add(label);
        }

        JScrollPane scroll = new JScrollPane(rows);
        scroll.setBorder(null);
        scroll.setBackground(UITheme.BG_DARK);
        scroll.getViewport().setBackground(UITheme.BG_DARK);
        return scroll;
    }

    private JPanel buildCloseButton() {
        JButton close = new JButton("CLOSE");
        close.setFont(UITheme.FONT_BUTTON);
        close.setForeground(UITheme.TEXT_GOLD);
        close.setBackground(UITheme.BUTTON_BG);
        close.setBorderPainted(false);
        close.setFocusPainted(false);
        close.addActionListener(e -> dispose());

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setBackground(UITheme.BG_DARK);
        panel.setBorder(new EmptyBorder(4, 0, 8, 0));
        panel.add(close);
        return panel;
    }

    public static void show(JFrame owner, String actionName, VoteResult result) {
        new VoteResultPanel(owner, actionName, result).setVisible(true);
    }
}