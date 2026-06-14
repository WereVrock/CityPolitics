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
                "Noble Battle at " + zoneId, true);
        dialog.setSize(480, 320);
        dialog.setLocationRelativeTo(parent);
        dialog.setResizable(false);
        dialog.getContentPane().setBackground(UITheme.BG_PANEL);

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(UITheme.BG_PANEL);
        root.setBorder(new EmptyBorder(16, 20, 12, 20));

        JLabel title = new JLabel("⚔ Noble Battle in Progress");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_GOLD);

        JLabel info = new JLabel("<html>"
                + "<b>" + City.ui.GrantZoneClaimDialog.stripHousePrefix(attackerName) + "</b>"
                + " is attacking <b>"
                + City.ui.GrantZoneClaimDialog.stripHousePrefix(defenderName) + "</b>"
                + " at <b>" + zoneId.replace("_", " ") + "</b>.<br><br>"
                + "Your army (" + playerSize + " soldiers) is present.<br>"
                + "Attacker force: ~" + attackerSize + " soldiers.<br><br>"
                + "What do you do?"
                + "</html>");
        info.setFont(UITheme.FONT_BODY);
        info.setForeground(UITheme.TEXT_PRIMARY);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(UITheme.BG_PANEL);
        header.add(title);
        header.add(Box.createVerticalStrut(8));
        header.add(info);

        JPanel btnPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        btnPanel.setBackground(UITheme.BG_PANEL);

        final PlayerChoice[] result = {PlayerChoice.IGNORE};

        JButton joinAtkBtn  = makeBtn("Join " + City.ui.GrantZoneClaimDialog.stripHousePrefix(attackerName),
                new Color(200, 80, 60));
        JButton joinDefBtn  = makeBtn("Join " + City.ui.GrantZoneClaimDialog.stripHousePrefix(defenderName),
                new Color(60, 140, 200));
        JButton stopBtn     = makeBtn("Stop the Fight", new Color(180, 140, 60));
        JButton ignoreBtn   = makeBtn("Ignore", UITheme.TEXT_SECONDARY);

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
        JButton btn = new JButton(text);
        btn.setFont(UITheme.FONT_BUTTON);
        btn.setForeground(fg);
        btn.setBackground(UITheme.BUTTON_BG);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}