package ui;

import main.actions.GrantZoneClaimAction;
import main.map.Zone;
import main.map.ZoneManager;
import main.nobles.NobleHouse;
import main.nobles.NobleHouseManager;
import main.parameters.GameParameters;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog for granting a zone claim to a noble house.
 */
public class GrantZoneClaimDialog {

    private GrantZoneClaimDialog() {}

    public static void show(Window parent,
                            GrantZoneClaimAction action,
                            NobleHouseManager nobleHouseManager,
                            ZoneManager zoneManager,
                            Runnable onGranted) {
        // Collect all owned zones (by nobles) as claimable targets
        List<Zone> ownedZones = new ArrayList<>();
        for (Zone z : zoneManager.getZones()) {
            if (z.isDesolate()) continue;
            if (nobleHouseManager.getOwnerOfZone(z.getId()) != null) {
                ownedZones.add(z);
            }
        }
        if (ownedZones.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "No noble-owned zones available.");
            return;
        }

        List<NobleHouse> houses = new ArrayList<>();
        for (NobleHouse h : nobleHouseManager.getHouses()) {
            if (!h.isEliminated()) houses.add(h);
        }

        JDialog dialog = new JDialog(
                parent instanceof Frame ? (Frame) parent : null,
                "Grant Zone Claim", true);
        dialog.setSize(500, 440);
        dialog.setLocationRelativeTo(parent);
        dialog.setResizable(false);
        dialog.getContentPane().setBackground(UITheme.BG_PANEL);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(UITheme.BG_PANEL);
        content.setBorder(new EmptyBorder(16, 16, 8, 16));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.weightx = 1.0; gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(5, 0, 5, 0);

        gc.gridy = 0;
        JLabel title = new JLabel("Grant Zone Claim");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_GOLD);
        content.add(title, gc);

        gc.gridy = 1;
        JLabel info = new JLabel("<html>"
                + "Grant a claim on a zone to a noble house.<br>"
                + "Owner: " + GameParameters.GRANT_CLAIM_OWNER_OPINION_MALUS + " opinion.  "
                + "Target: +" + GameParameters.GRANT_CLAIM_TARGET_OPINION_BONUS + " opinion.  "
                + "Other claimants: " + GameParameters.GRANT_CLAIM_OTHER_CLAIMANT_MALUS + " opinion."
                + "</html>");
        info.setFont(UITheme.FONT_SMALL);
        info.setForeground(UITheme.TEXT_SECONDARY);
        content.add(info, gc);

        gc.gridy = 2;
        JLabel zoneLabel = new JLabel("Zone:");
        zoneLabel.setFont(UITheme.FONT_BODY);
        zoneLabel.setForeground(UITheme.TEXT_PRIMARY);
        content.add(zoneLabel, gc);

        gc.gridy = 3;
        String[] zoneNames = ownedZones.stream().map(z -> {
            NobleHouse owner = nobleHouseManager.getOwnerOfZone(z.getId());
            return z.getDisplayName() + " (owned by "
                    + (owner != null ? owner.getName() : "none") + ")";
        }).toArray(String[]::new);
        JComboBox<String> zoneBox = new JComboBox<>(zoneNames);
        zoneBox.setFont(UITheme.FONT_BODY);
        zoneBox.setBackground(UITheme.BG_PANEL_LIGHT);
        zoneBox.setForeground(UITheme.TEXT_PRIMARY);
        content.add(zoneBox, gc);

        gc.gridy = 4;
        JLabel houseLabel = new JLabel("Grant claim to:");
        houseLabel.setFont(UITheme.FONT_BODY);
        houseLabel.setForeground(UITheme.TEXT_PRIMARY);
        content.add(houseLabel, gc);

        gc.gridy = 5;
        String[] houseNames = houses.stream().map(h ->
                h.getName() + "  (opinion: " + h.getPlayerOpinion() + ")")
                .toArray(String[]::new);
        JComboBox<String> houseBox = new JComboBox<>(houseNames);
        houseBox.setFont(UITheme.FONT_BODY);
        houseBox.setBackground(UITheme.BG_PANEL_LIGHT);
        houseBox.setForeground(UITheme.TEXT_PRIMARY);
        content.add(houseBox, gc);

        // Preview label
        gc.gridy = 6;
        JLabel preview = buildPreviewLabel(ownedZones, houses, zoneBox, houseBox,
                nobleHouseManager, zoneManager);
        content.add(preview, gc);

        // Update preview on change
        zoneBox.addActionListener(e -> updatePreview(preview, ownedZones, houses,
                zoneBox, houseBox, nobleHouseManager));
        houseBox.addActionListener(e -> updatePreview(preview, ownedZones, houses,
                zoneBox, houseBox, nobleHouseManager));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btnRow.setBackground(UITheme.BG_PANEL);

        JButton grantBtn = new JButton("GRANT CLAIM");
        grantBtn.setFont(UITheme.FONT_BUTTON);
        grantBtn.setForeground(UITheme.TEXT_GOLD);
        grantBtn.setBackground(UITheme.BUTTON_BG);
        grantBtn.setBorderPainted(false);
        grantBtn.setFocusPainted(false);
        grantBtn.addActionListener(e -> {
            Zone selectedZone  = ownedZones.get(zoneBox.getSelectedIndex());
            NobleHouse target  = houses.get(houseBox.getSelectedIndex());
            NobleHouse owner   = nobleHouseManager.getOwnerOfZone(selectedZone.getId());
            if (owner == target) {
                JOptionPane.showMessageDialog(dialog,
                        target.getName() + " already owns this zone. Choose a different house or zone.");
                return;
            }
            main.actions.ActionResult result = action.grantClaim(selectedZone.getId(), target);
            JOptionPane.showMessageDialog(dialog, result.getMessage(),
                    "Claim Granted", JOptionPane.INFORMATION_MESSAGE);
            onGranted.run();
            dialog.dispose();
        });

        JButton cancelBtn = new JButton("CANCEL");
        cancelBtn.setFont(UITheme.FONT_BUTTON);
        cancelBtn.setForeground(UITheme.TEXT_SECONDARY);
        cancelBtn.setBackground(UITheme.BUTTON_BG);
        cancelBtn.setBorderPainted(false);
        cancelBtn.setFocusPainted(false);
        cancelBtn.addActionListener(e -> dialog.dispose());

        btnRow.add(cancelBtn);
        btnRow.add(grantBtn);

        dialog.setLayout(new BorderLayout());
        dialog.add(content, BorderLayout.CENTER);
        dialog.add(btnRow,  BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private static JLabel buildPreviewLabel(List<Zone> zones, List<NobleHouse> houses,
                                             JComboBox<String> zoneBox,
                                             JComboBox<String> houseBox,
                                             NobleHouseManager nhm,
                                             ZoneManager zm) {
        JLabel lbl = new JLabel();
        lbl.setFont(UITheme.FONT_SMALL);
        lbl.setForeground(UITheme.TEXT_SECONDARY);
        updatePreview(lbl, zones, houses, zoneBox, houseBox, nhm);
        return lbl;
    }

    private static void updatePreview(JLabel preview, List<Zone> zones, List<NobleHouse> houses,
                                       JComboBox<String> zoneBox, JComboBox<String> houseBox,
                                       NobleHouseManager nhm) {
        if (zones.isEmpty() || houses.isEmpty()) return;
        int zi = Math.max(0, zoneBox.getSelectedIndex());
        int hi = Math.max(0, houseBox.getSelectedIndex());
        if (zi >= zones.size() || hi >= houses.size()) return;
        Zone z = zones.get(zi);
        NobleHouse target = houses.get(hi);
        NobleHouse owner  = nhm.getOwnerOfZone(z.getId());
        String ownerText = owner != null ? owner.getName() + " ("
                + GameParameters.GRANT_CLAIM_OWNER_OPINION_MALUS + " opinion)" : "none";
        preview.setText("<html>Owner: " + ownerText
                + "  →  " + target.getName()
                + " (+" + GameParameters.GRANT_CLAIM_TARGET_OPINION_BONUS + " opinion)</html>");
    }
}