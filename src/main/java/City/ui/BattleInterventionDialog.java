package City.ui;

import City.main.army.PlayerBattleInterventionProcessor.PlayerChoice;
import City.main.nobles.NobleArmy;
import City.main.nobles.NobleHouse;
import City.main.nobles.NobleHouseManager;
import City.main.nobles.RelationshipManager;
import City.main.nobles.Relationship;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog asking the player whether to intervene in a noble battle.
 * Shows full detail of all participants including coalition joiners.
 * Tracks whether intervention is justified (coalition / protecting a protected house).
 */
public class BattleInterventionDialog {

    public record InterventionResult(PlayerChoice choice, boolean justified) {}

    private BattleInterventionDialog() {}

    public static PlayerChoice show(Window parent,
                                     String attackerName, String defenderName,
                                     String zoneId, int playerSize, int attackerSize) {
        return showDetailed(parent, attackerName, defenderName, zoneId,
                playerSize, attackerSize, List.of(), List.of(), false).choice();
    }

    public static InterventionResult showDetailed(
            Window parent,
            String attackerName, String defenderName,
            String zoneId, int playerSize, int attackerSize,
            List<String> attackerAllies,
            List<String> defenderAllies,
            boolean defenderIsProtected) {

        JDialog dialog = new JDialog(
                parent instanceof Frame ? (Frame) parent : null,
                "Noble Battle at " + zoneId.replace("_", " "), true);
        dialog.setSize(560, 480);
        dialog.setLocationRelativeTo(parent);
        dialog.setResizable(false);
        dialog.getContentPane().setBackground(UITheme.BG_PANEL);

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(UITheme.BG_PANEL);
        root.setBorder(new EmptyBorder(16, 20, 12, 20));

        // Header
        JLabel title = new JLabel("⚔ Noble Battle — " + zoneId.replace("_", " "));
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_GOLD);

        // Attacker side
        StringBuilder atkSide = new StringBuilder();
        atkSide.append("<b>ATTACKERS:</b><br>");
        atkSide.append("&nbsp;&nbsp;").append(stripHouse(attackerName))
               .append(" — ~").append(attackerSize).append(" soldiers<br>");
        for (String ally : attackerAllies) {
            atkSide.append("&nbsp;&nbsp;+ ").append(stripHouse(ally)).append(" (joining)<br>");
        }

        // Defender side
        StringBuilder defSide = new StringBuilder();
        defSide.append("<b>DEFENDERS:</b><br>");
        defSide.append("&nbsp;&nbsp;").append(stripHouse(defenderName))
               .append(" — defending ").append(zoneId.replace("_", " ")).append("<br>");
        for (String ally : defenderAllies) {
            defSide.append("&nbsp;&nbsp;+ ").append(stripHouse(ally)).append(" (joining)<br>");
        }
        if (defenderIsProtected) {
            defSide.append("&nbsp;&nbsp;<font color='#78C87A'>★ Under your protection</font><br>");
        }

        JLabel info = new JLabel("<html>"
                + atkSide
                + "<br>"
                + defSide
                + "<br>"
                + "<b>YOUR ARMY:</b> " + playerSize + " soldiers present<br>"
                + "</html>");
        info.setFont(UITheme.FONT_BODY);
        info.setForeground(UITheme.TEXT_PRIMARY);

        // Justification note
        boolean joinDefenderJustified = defenderIsProtected;
        boolean joinAttackerJustified = false; // coalition not passed through here yet

        JLabel justNote = new JLabel("<html><i>"
                + (defenderIsProtected
                    ? "Joining the defender is justified — they are under your protection."
                    : "Note: joining either side without justification costs 1 Trust and lowers bystander opinions.")
                + "</i></html>");
        justNote.setFont(UITheme.FONT_SMALL);
        justNote.setForeground(defenderIsProtected ? UITheme.TEXT_GREEN : new Color(200, 160, 60));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(UITheme.BG_PANEL);
        header.add(title);
        header.add(Box.createVerticalStrut(8));
        header.add(info);
        header.add(Box.createVerticalStrut(6));
        header.add(justNote);

        // Buttons
        JPanel btnPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        btnPanel.setBackground(UITheme.BG_PANEL);

        final InterventionResult[] result = {new InterventionResult(PlayerChoice.IGNORE, false)};

        String atkShort = stripHouse(attackerName);
        String defShort = stripHouse(defenderName);

        JButton joinAtkBtn  = makeBtn("<html>Join " + atkShort
                + "<br><font size='-1' color='#aaaaaa'>+opinion attacker, −opinion defender</font></html>",
                new Color(200, 80, 60));
        JButton joinDefBtn  = makeBtn("<html>Join " + defShort
                + (defenderIsProtected ? " ★" : "")
                + "<br><font size='-1' color='#aaaaaa'>+opinion defender, −opinion attacker</font></html>",
                defenderIsProtected ? UITheme.TEXT_GREEN : new Color(60, 140, 200));
        JButton stopBtn     = makeBtn("<html>Stop the Fight"
                + "<br><font size='-1' color='#aaaaaa'>−½opinion attacker, +¼opinion defender</font></html>",
                new Color(180, 140, 60));
        JButton ignoreBtn   = makeBtn("<html>Ignore"
                + "<br><font size='-1' color='#aaaaaa'>No effect</font></html>",
                UITheme.TEXT_SECONDARY);

        joinAtkBtn.addActionListener(e -> {
            result[0] = new InterventionResult(PlayerChoice.JOIN_ATTACKER, joinAttackerJustified);
            dialog.dispose();
        });
        joinDefBtn.addActionListener(e -> {
            result[0] = new InterventionResult(PlayerChoice.JOIN_DEFENDER, joinDefenderJustified);
            dialog.dispose();
        });
        stopBtn.addActionListener(e -> {
            result[0] = new InterventionResult(PlayerChoice.STOP_FIGHT, true);
            dialog.dispose();
        });
        ignoreBtn.addActionListener(e -> {
            result[0] = new InterventionResult(PlayerChoice.IGNORE, true);
            dialog.dispose();
        });

        btnPanel.add(joinAtkBtn);
        btnPanel.add(joinDefBtn);
        btnPanel.add(stopBtn);
        btnPanel.add(ignoreBtn);

        root.add(header,   BorderLayout.CENTER);
        root.add(btnPanel, BorderLayout.SOUTH);
        dialog.add(root);
        dialog.setVisible(true);
        return result[0];
    }

    private static JButton makeBtn(String text, Color fg) {
        JButton btn = new JButton("<html><body style='text-align:center'>" + text + "</body></html>");
        btn.setFont(UITheme.FONT_BUTTON);
        btn.setForeground(fg);
        btn.setBackground(UITheme.BUTTON_BG);
        btn.setBorderPainted(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
                new EmptyBorder(6, 8, 6, 8)));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private static String stripHouse(String name) {
        if (name == null) return "";
        return name.startsWith("House ") ? name.substring(6) : name;
    }
}