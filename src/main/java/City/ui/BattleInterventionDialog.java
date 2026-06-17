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
    return showDetailed(parent, attackerName, defenderName, zoneId, playerSize, attackerSize,
            attackerAllies, defenderAllies, defenderIsProtected, false, false);
}

public static InterventionResult showDetailed(
            Window parent,
            String attackerName, String defenderName,
            String zoneId, int playerSize, int attackerSize,
            List<String> attackerAllies,
            List<String> defenderAllies,
            boolean defenderIsProtected,
            boolean attackerHasUnlawfulZone,
            boolean zoneIsUnlawful) {

        JDialog dialog = new JDialog(
                parent instanceof Frame ? (Frame) parent : null,
                "Noble Battle at " + zoneId.replace("_", " "), true);
        dialog.setMinimumSize(new Dimension(500, 460));
        dialog.setLocationRelativeTo(parent);
        dialog.setResizable(true);
        dialog.getContentPane().setBackground(UITheme.BG_PANEL);

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(UITheme.BG_PANEL);
        root.setBorder(new EmptyBorder(16, 20, 12, 20));

        // Title
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

        // Justification explanations
        boolean joinAtkJustified = zoneIsUnlawful;
        boolean joinDefenderJustified = defenderIsProtected || attackerHasUnlawfulZone;

        JPanel justPanel = new JPanel();
        justPanel.setLayout(new BoxLayout(justPanel, BoxLayout.Y_AXIS));
        justPanel.setBackground(UITheme.BG_PANEL);

        JLabel generalNote = new JLabel("<html><i>Joining without justification costs 1 Trust and lowers bystander opinions.</i></html>");
        generalNote.setFont(UITheme.FONT_SMALL);
        generalNote.setForeground(new Color(200, 160, 60));
        justPanel.add(generalNote);

        if (joinAtkJustified) {
            JLabel atkJustNote = new JLabel("<html><i>★ Joining the attacker is <b>justified</b> — this zone is marked as unlawfully acquired.</i></html>");
            atkJustNote.setFont(UITheme.FONT_SMALL);
            atkJustNote.setForeground(new Color(120, 200, 120));
            justPanel.add(atkJustNote);
        }

        if (defenderIsProtected) {
            JLabel protNote = new JLabel("<html><i>★ Joining the defender is <b>justified</b> — they are under your protection.</i></html>");
            protNote.setFont(UITheme.FONT_SMALL);
            protNote.setForeground(new Color(120, 200, 120));
            justPanel.add(protNote);
        } else if (attackerHasUnlawfulZone) {
            JLabel unlawfulNote = new JLabel("<html><i>★ Joining the defender is <b>justified</b> — the attacker holds an unlawfully acquired zone.</i></html>");
            unlawfulNote.setFont(UITheme.FONT_SMALL);
            unlawfulNote.setForeground(new Color(120, 200, 120));
            justPanel.add(unlawfulNote);
        }

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(UITheme.BG_PANEL);
        header.add(title);
        header.add(Box.createVerticalStrut(8));
        header.add(info);
        header.add(Box.createVerticalStrut(6));
        header.add(justPanel);

        // Buttons — full descriptions
        JPanel btnPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        btnPanel.setBackground(UITheme.BG_PANEL);

        final InterventionResult[] result = {new InterventionResult(PlayerChoice.IGNORE, false)};

        String atkShort = stripHouse(attackerName);
        String defShort = stripHouse(defenderName);

        String joinAtkJust = zoneIsUnlawful
                ? "Justified — this zone is marked as unlawfully acquired. No Trust penalty."
                : "No special justification. Costs 1 Trust and lowers bystander opinions.";
        String joinDefJust = defenderIsProtected
                ? "Justified — defender is under your protection. No Trust penalty."
                : (attackerHasUnlawfulZone
                    ? "Justified — attacker holds an unlawfully acquired zone. No Trust penalty."
                    : "No special justification. Costs 1 Trust and lowers bystander opinions.");
        String stopJust = "Stopping a fight between nobles. Costs some attacker opinion, gains small defender opinion.";
        String ignoreJust = "You do nothing. No penalties.";

        JButton joinAtkBtn  = makeBtn(
                "<html><b>Join " + atkShort + (joinAtkJustified ? " ★" : "") + "</b>"
                + "<br><font size='-1' color='#aaaaaa'>+opinion attacker, −opinion defender</font>"
                + "<br><font size='-1' color='" + (joinAtkJustified ? "#78C87A" : "#cc8844") + "'>"
                + joinAtkJust + "</font></html>",
                joinAtkJustified ? UITheme.TEXT_GREEN : new Color(200, 80, 60));
        JButton joinDefBtn  = makeBtn(
                "<html><b>Join " + defShort + (joinDefenderJustified ? " ★" : "") + "</b>"
                + "<br><font size='-1' color='#aaaaaa'>+opinion defender, −opinion attacker</font>"
                + "<br><font size='-1' color='" + (joinDefenderJustified ? "#78C87A" : "#cc8844") + "'>"
                + joinDefJust + "</font></html>",
                joinDefenderJustified ? UITheme.TEXT_GREEN : new Color(60, 140, 200));
        JButton stopBtn     = makeBtn(
                "<html><b>Stop the Fight</b>"
                + "<br><font size='-1' color='#aaaaaa'>−½ opinion attacker, +¼ opinion defender</font>"
                + "<br><font size='-1' color='#cc8844'>" + stopJust + "</font></html>",
                new Color(180, 140, 60));
        JButton ignoreBtn   = makeBtn(
                "<html><b>Ignore</b>"
                + "<br><font size='-1' color='#aaaaaa'>No immediate effect</font>"
                + "<br><font size='-1' color='#78C87A'>" + ignoreJust + "</font></html>",
                UITheme.TEXT_SECONDARY);

        joinAtkBtn.addActionListener(e -> {
            result[0] = new InterventionResult(PlayerChoice.JOIN_ATTACKER, false);
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
        dialog.pack();
        dialog.setMinimumSize(new Dimension(500, dialog.getHeight()));
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        return result[0];
    }

private static JButton makeBtn(String text, Color fg) {
        JButton btn = new JButton("<html><body style='text-align:left; padding: 4px'>" + text + "</body></html>");
        btn.setFont(UITheme.FONT_BUTTON);
        btn.setForeground(fg);
        btn.setBackground(UITheme.BUTTON_BG);
        btn.setBorderPainted(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
                new EmptyBorder(8, 10, 8, 10)));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        return btn;
    }

private static String stripHouse(String name) {
        if (name == null) return "";
        return name.startsWith("House ") ? name.substring(6) : name;
    }
}