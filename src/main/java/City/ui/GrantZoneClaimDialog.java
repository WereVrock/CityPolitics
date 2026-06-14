package City.ui;

import City.main.actions.GrantZoneClaimAction;
import City.main.map.Zone;
import City.main.map.ZoneManager;
import City.main.nobles.NobleHouse;
import City.main.nobles.NobleHouseManager;
import City.main.parameters.ActionParams;
 
import City.main.resources.ResourcePool;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Dialog for granting a zone claim to a noble house.
 * Sorts zones and houses alphabetically. Strips "House " prefix from names.
 * Costs GRANT_CLAIM_INFLUENCE_COST influence.
 * Can be pre-seeded with a zone when opened from the map info panel.
 */
public class GrantZoneClaimDialog {

    private GrantZoneClaimDialog() {}

    public static void show(Window parent,
                            GrantZoneClaimAction action,
                            NobleHouseManager nobleHouseManager,
                            ZoneManager zoneManager,
                            ResourcePool resources,
                            City.main.ledger.Ledger ledger,
                            String preselectZoneId,
                            Runnable onGranted) {
        // Collect all non-desolate zones sorted alphabetically
        List<Zone> ownedZones = new ArrayList<>();
        for (Zone z : zoneManager.getZones()) {
            if (z.isDesolate()) continue;
            ownedZones.add(z);
        }
        ownedZones.sort(Comparator.comparing(Zone::getDisplayName));

        if (ownedZones.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "No zones available.");
            return;
        }

        // Houses sorted alphabetically, "House " stripped
        List<NobleHouse> houses = new ArrayList<>();
        for (NobleHouse h : nobleHouseManager.getHouses()) {
            if (!h.isEliminated()) houses.add(h);
        }
        houses.sort(Comparator.comparing(h -> stripHousePrefix(h.getName())));

        JDialog dialog = new JDialog(
                parent instanceof Frame ? (Frame) parent : null,
                "Grant Zone Claim", true);
        dialog.setSize(520, 460);
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
                + "Grant a claim on a zone to a noble house. Costs "
                + ActionParams.GRANT_CLAIM_INFLUENCE_COST + " influence.<br>"
                + "Owner: " + ActionParams.GRANT_CLAIM_OWNER_OPINION_MALUS + " opinion.  "
                + "Target: +" + ActionParams.GRANT_CLAIM_TARGET_OPINION_BONUS + " opinion.  "
                + "Other claimants: " + ActionParams.GRANT_CLAIM_OTHER_CLAIMANT_MALUS + " opinion."
                + "</html>");
        info.setFont(UITheme.FONT_SMALL);
        info.setForeground(UITheme.TEXT_SECONDARY);
        content.add(info, gc);

        gc.gridy = 2;
        JLabel influenceLabel = new JLabel("Influence available: " + resources.getInfluence()
                + "  (need " + ActionParams.GRANT_CLAIM_INFLUENCE_COST + ")");
        influenceLabel.setFont(UITheme.FONT_SMALL);
        influenceLabel.setForeground(resources.getInfluence() >= ActionParams.GRANT_CLAIM_INFLUENCE_COST
                ? UITheme.TEXT_GREEN : UITheme.TEXT_RED);
        content.add(influenceLabel, gc);

        gc.gridy = 3;
        JLabel zoneLabel = new JLabel("Zone:");
        zoneLabel.setFont(UITheme.FONT_BODY);
        zoneLabel.setForeground(UITheme.TEXT_PRIMARY);
        content.add(zoneLabel, gc);

        gc.gridy = 4;
        String[] zoneNames = ownedZones.stream().map(z -> {
            NobleHouse owner = nobleHouseManager.getOwnerOfZone(z.getId());
            return z.getDisplayName() + (owner != null
                    ? " (owned by " + stripHousePrefix(owner.getName()) + ")" : " (unowned)");
        }).toArray(String[]::new);
        JComboBox<String> zoneBox = new JComboBox<>(zoneNames);
        zoneBox.setFont(UITheme.FONT_BODY);
        zoneBox.setBackground(UITheme.BG_PANEL_LIGHT);
        zoneBox.setForeground(UITheme.TEXT_PRIMARY);
        // Pre-select if zone id provided
        if (preselectZoneId != null) {
            for (int i = 0; i < ownedZones.size(); i++) {
                if (ownedZones.get(i).getId().equals(preselectZoneId)) {
                    zoneBox.setSelectedIndex(i);
                    break;
                }
            }
        }
        content.add(zoneBox, gc);

        gc.gridy = 5;
        JLabel houseLabel = new JLabel("Grant claim to:");
        houseLabel.setFont(UITheme.FONT_BODY);
        houseLabel.setForeground(UITheme.TEXT_PRIMARY);
        content.add(houseLabel, gc);

        gc.gridy = 6;
        String[] houseNames = houses.stream().map(h ->
                stripHousePrefix(h.getName()) + "  (opinion: " + h.getPlayerOpinion() + ")")
                .toArray(String[]::new);
        JComboBox<String> houseBox = new JComboBox<>(houseNames);
        houseBox.setFont(UITheme.FONT_BODY);
        houseBox.setBackground(UITheme.BG_PANEL_LIGHT);
        houseBox.setForeground(UITheme.TEXT_PRIMARY);
        content.add(houseBox, gc);

        gc.gridy = 7;
        JLabel preview = new JLabel();
        preview.setFont(UITheme.FONT_SMALL);
        preview.setForeground(UITheme.TEXT_SECONDARY);
        updatePreview(preview, ownedZones, houses, zoneBox, houseBox, nobleHouseManager);
        content.add(preview, gc);

        zoneBox.addActionListener(e ->
                updatePreview(preview, ownedZones, houses, zoneBox, houseBox, nobleHouseManager));
        houseBox.addActionListener(e ->
                updatePreview(preview, ownedZones, houses, zoneBox, houseBox, nobleHouseManager));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btnRow.setBackground(UITheme.BG_PANEL);

        boolean canAffordInfluence = resources.getInfluence()
                >= ActionParams.GRANT_CLAIM_INFLUENCE_COST;

        JButton grantBtn = new JButton("GRANT CLAIM");
        grantBtn.setFont(UITheme.FONT_BUTTON);
        grantBtn.setForeground(UITheme.TEXT_GOLD);
        grantBtn.setBackground(UITheme.BUTTON_BG);
        grantBtn.setBorderPainted(false);
        grantBtn.setFocusPainted(false);
        grantBtn.setEnabled(canAffordInfluence);
        if (!canAffordInfluence)
            grantBtn.setToolTipText("Not enough influence.");

        final boolean[] granted = {false};
        grantBtn.addActionListener(e -> {
            Zone       selectedZone = ownedZones.get(zoneBox.getSelectedIndex());
            NobleHouse target       = houses.get(houseBox.getSelectedIndex());
            NobleHouse owner        = nobleHouseManager.getOwnerOfZone(selectedZone.getId());
            if (owner == target) {
                JOptionPane.showMessageDialog(dialog,
                        stripHousePrefix(target.getName())
                        + " already owns this zone. Choose a different house or zone.");
                return;
            }
            City.main.actions.ActionResult result =
                    action.grantClaim(selectedZone.getId(), target, resources, ledger);
            if (result.isSuccess()) {
                granted[0] = true;
                JOptionPane.showMessageDialog(dialog, result.getMessage(),
                        "Claim Granted", JOptionPane.INFORMATION_MESSAGE);
                onGranted.run();
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, result.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
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

    public static String stripHousePrefix(String name) {
        if (name == null) return "";
        if (name.startsWith("House ")) return name.substring(6);
        return name;
    }

    private static void updatePreview(JLabel preview, List<Zone> zones, List<NobleHouse> houses,
                                       JComboBox<String> zoneBox, JComboBox<String> houseBox,
                                       NobleHouseManager nhm) {
        if (zones.isEmpty() || houses.isEmpty()) return;
        int zi = Math.max(0, zoneBox.getSelectedIndex());
        int hi = Math.max(0, houseBox.getSelectedIndex());
        if (zi >= zones.size() || hi >= houses.size()) return;
        Zone       z      = zones.get(zi);
        NobleHouse target = houses.get(hi);
        NobleHouse owner  = nhm.getOwnerOfZone(z.getId());
        String ownerText = owner != null
                ? stripHousePrefix(owner.getName()) + " ("
                  + ActionParams.GRANT_CLAIM_OWNER_OPINION_MALUS + " opinion)"
                : "none";
        preview.setText("<html>Owner: " + ownerText
                + "  →  " + stripHousePrefix(target.getName())
                + " (+" + ActionParams.GRANT_CLAIM_TARGET_OPINION_BONUS + " opinion)</html>");
    }
}