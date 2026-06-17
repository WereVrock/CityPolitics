package City.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Reusable themed message dialog matching FrostVeil's UITheme styling.
 * Used in place of plain JOptionPane dialogs so info/result popups
 * stay visually consistent with the rest of the UI.
 */
public final class ThemedDialogs {

    private ThemedDialogs() {}

    public static void showInfo(Component parent, String title, String message) {
        JDialog d = new JDialog(
                SwingUtilities.getWindowAncestor(parent) instanceof Frame
                        ? (Frame) SwingUtilities.getWindowAncestor(parent) : null,
                title, true);
        d.setUndecorated(true);

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(UITheme.BG_PANEL);
        root.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 2));

        JLabel titleLabel = new JLabel("  " + title);
        titleLabel.setFont(UITheme.FONT_TITLE);
        titleLabel.setForeground(UITheme.TEXT_GOLD);
        titleLabel.setBackground(UITheme.BG_PANEL_LIGHT);
        titleLabel.setOpaque(true);
        titleLabel.setBorder(new EmptyBorder(12, 12, 10, 12));

        JLabel body = new JLabel("<html><body style='width:340px; padding:8px'>"
                + message.replace("\n", "<br>") + "</body></html>");
        body.setFont(UITheme.FONT_BODY);
        body.setForeground(UITheme.TEXT_PRIMARY);
        body.setBorder(new EmptyBorder(4, 14, 4, 14));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btns.setBackground(UITheme.BG_PANEL);
        JButton ok = new JButton("OK");
        ok.setFont(UITheme.FONT_BUTTON);
        ok.setForeground(UITheme.TEXT_GOLD);
        ok.setBackground(UITheme.BUTTON_BG);
        ok.setBorderPainted(false);
        ok.setFocusPainted(false);
        ok.addActionListener(e -> d.dispose());
        btns.add(ok);

        root.add(titleLabel, BorderLayout.NORTH);
        root.add(body,       BorderLayout.CENTER);
        root.add(btns,       BorderLayout.SOUTH);

        d.setContentPane(root);
        d.pack();
        d.setMinimumSize(new Dimension(380, 180));
        d.setLocationRelativeTo(parent);
        d.setVisible(true);
    }
}