package City.ui;

import City.main.army.PlayerBattleInterventionProcessor.PlayerChoice;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Dialog asking the player whether to intervene in a noble battle.
 */
public class BattleInterventionDialog {

    private BattleInterventionDialog() {}

public static PlayerChoice show(Window parent,
                                     String attackerName, String defenderName,
                                     String zoneId, int playerSize, int attackerSize) {
        JDialog dialog = new JDialog(
                parent instanceof Frame ? (Frame) parent : null,
                "Noble Battle at " + zoneId.replace("_", " "), true);
        dialog.setSize(520, 360);
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

        // Detailed sides breakdown
        String atk  = City.ui.GrantZoneClaimDialog.stripHousePrefix(attackerName);
        String def  = City.ui.GrantZoneClaimDialog.stripHousePrefix(defenderName);
        JLabel info = new JLabel("<html>"
                + "<b>ATTACKER:</b> " + atk + " — ~" + attackerSize + " soldiers<br>"
                + "<b>DEFENDER:</b> " + def + " — defending " + zoneId.replace("_"," ") + "<br><br>"
                + "<b>YOUR ARMY:</b> " + playerSize + " soldiers present<br><br>"
                + "What will you do?"
                + "</html>");
        info.setFont(UITheme.FONT_BODY);
        info.setForeground(UITheme.TEXT_PRIMARY);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(UITheme.BG_PANEL);
        header.add(title);
        header.add(Box.createVerticalStrut(8));
        header.add(info);

        // Buttons with consequences shown
        JPanel btnPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        btnPanel.setBackground(UITheme.BG_PANEL);

        final PlayerChoice[] result = {PlayerChoice.IGNORE};

        JButton joinAtkBtn  = makeBtn("<html>Join " + atk
                + "<br><font size='-1' color='#aaaaaa'>+opinion with attacker, -opinion with defender</font></html>",
                new Color(200, 80, 60));
        JButton joinDefBtn  = makeBtn("<html>Join " + def
                + "<br><font size='-1' color='#aaaaaa'>+opinion with defender, -opinion with attacker</font></html>",
                new Color(60, 140, 200));
        JButton stopBtn     = makeBtn("<html>Stop the Fight"
                + "<br><font size='-1' color='#aaaaaa'>-opinion attacker, +½opinion defender</font></html>",
                new Color(180, 140, 60));
        JButton ignoreBtn   = makeBtn("<html>Ignore"
                + "<br><font size='-1' color='#aaaaaa'>No effect</font></html>",
                UITheme.TEXT_SECONDARY);

        joinAtkBtn.addActionListener(e -> { result[0] = PlayerChoice.JOIN_ATTACKER; dialog.dispose(); });
        joinDefBtn.addActionListener(e -> { result[0] = PlayerChoice.JOIN_DEFENDER; dialog.dispose(); });
        stopBtn.addActionListener(e   -> { result[0] = PlayerChoice.STOP_FIGHT;    dialog.dispose(); });
        ignoreBtn.addActionListener(e -> { result[0] = PlayerChoice.IGNORE;        dialog.dispose(); });

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

}