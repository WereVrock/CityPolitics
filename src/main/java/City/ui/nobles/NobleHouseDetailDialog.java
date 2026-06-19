// NobleHouseDetailDialog.java
package City.ui.nobles;

import City.main.map.Zone;
import City.main.map.ZoneManager;
import City.main.map.ZoneState;
import City.main.nobles.ClaimManager;
import City.main.nobles.NobleCharacter;
import City.main.nobles.NobleHouse;
import City.main.nobles.NobleHouseColors;
import City.main.bank.BankManager;
import City.ui.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only detail popup for a single noble house: active leader profile,
 * other family members, held territory (flagging unlawfully/lawfully
 * acquired zones), and outstanding claims on land it doesn't hold.
 *
 * Pure display — no game-state mutation happens here.
 */
public class NobleHouseDetailDialog extends JDialog {

    private final NobleHouse   house;
    private final ClaimManager claimManager;
    private final ZoneManager  zoneManager;
    private final BankManager  bankManager;

    public NobleHouseDetailDialog(Window owner, NobleHouse house,
                                   ClaimManager claimManager, ZoneManager zoneManager,
                                   BankManager bankManager) {
        super(owner, house.getName(), Dialog.ModalityType.APPLICATION_MODAL);
        this.house        = house;
        this.claimManager = claimManager;
        this.zoneManager  = zoneManager;
        this.bankManager  = bankManager;

        getContentPane().setBackground(UITheme.BG_DARK);
        setLayout(new BorderLayout());
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(16, 16, 16, 16));

        add(buildHeader(), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(UITheme.BG_DARK);
        body.add(buildLeaderSection());
        body.add(Box.createVerticalStrut(12));
        body.add(buildResourcesSection());
        body.add(Box.createVerticalStrut(12));
        body.add(buildBenchSection());
        body.add(Box.createVerticalStrut(12));
        body.add(buildTerritorySection());
        body.add(Box.createVerticalStrut(12));
        body.add(buildClaimsSection());

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UITheme.BG_DARK);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        add(scroll, BorderLayout.CENTER);

        add(buildFooter(), BorderLayout.SOUTH);

        setPreferredSize(new Dimension(460, 560));
        pack();
        setLocationRelativeTo(owner);
    }

    // ─── Header ───────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(UITheme.BG_DARK);
        panel.setBorder(new EmptyBorder(0, 0, 12, 0));

        JPanel swatch = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(NobleHouseColors.getPrimary(house.getId()));
                g.fillRect(0, 0, getWidth(), getHeight() / 2);
                g.setColor(NobleHouseColors.getSecondary(house.getId()));
                g.fillRect(0, getHeight() / 2, getWidth(), getHeight() - getHeight() / 2);
            }
        };
        swatch.setPreferredSize(new Dimension(28, 40));

        JLabel name = new JLabel(house.getName());
        name.setFont(UITheme.FONT_TITLE);
        name.setForeground(UITheme.TEXT_GOLD);

        String capitalName = house.getCapitalZoneId() != null
            ? house.getCapitalZoneId().replace("_", " ") : "None";
        JLabel race = new JLabel(house.getRace().name() + "  ·  Capital: " + capitalName);
        race.setFont(UITheme.FONT_SMALL);
        race.setForeground(UITheme.TEXT_SECONDARY);

        JPanel textCol = new JPanel();
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        textCol.setBackground(UITheme.BG_DARK);
        textCol.add(name);
        textCol.add(race);

        panel.add(swatch,  BorderLayout.WEST);
        panel.add(textCol, BorderLayout.CENTER);
        return panel;
    }

    // ─── Leader ───────────────────────────────────────────────────────────

private JPanel buildLeaderSection() {
        NobleCharacter leader = house.getActiveCharacter();

        JPanel section = sectionPanel("ACTIVE LEADER");
        if (leader == null) {
            section.add(bodyLabel("No active leader."));
            return section;
        }

        section.add(boldLabel(leader.getName()));
        section.add(italicLabel(leader.getPersonality()));
        section.add(Box.createVerticalStrut(4));
        section.add(bodyLabel("Motivation: " + leader.getDominantMotivation().name()
            + " / " + leader.getSecondaryMotivation().name()));
        section.add(bodyLabel("Diplomacy " + leader.getDiplomacy()
            + "   ·   Military " + leader.getMilitary()
            + "   ·   Cunning " + leader.getCunning()));
        return section;
    }

private JPanel buildBenchSection() {
        JPanel section = sectionPanel("OTHER FAMILY MEMBERS");
        List<NobleCharacter> characters = house.getCharacters();
        NobleCharacter active = house.getActiveCharacter();
        boolean any = false;
        for (NobleCharacter c : characters) {
            if (c == active) continue;
            any = true;
            section.add(boldLabel(c.getName()));
            section.add(italicLabel(c.getPersonality()));
            section.add(Box.createVerticalStrut(4));
        }
        if (!any) section.add(bodyLabel("None recorded."));
        return section;
    }

    // ─── Territory ────────────────────────────────────────────────────────

    private JPanel buildTerritorySection() {
        JPanel section = sectionPanel("TERRITORY");
        List<String> zoneIds = house.getZoneIds();
        if (zoneIds.isEmpty()) {
            section.add(bodyLabel("This house holds no land."));
            return section;
        }
        for (String zoneId : zoneIds) {
            Zone zone = zoneManager.getZone(zoneId);
            ZoneState state = zoneManager.getState(zoneId);
            String display = zone != null ? zone.getDisplayName() : zoneId.replace("_", " ");
            boolean isCapital   = house.isCapital(zoneId);
            boolean unlawful    = state != null && state.isUnlawfullyAcquired();
            boolean lawfulCeded = state != null && state.isLawfullyAcquired();

            String text = display + (isCapital ? "  (Capital)" : "");
            JLabel label = bodyLabel(text);
            if (unlawful) {
                label.setText(text + "   ⚠ UNLAWFULLY HELD");
                label.setForeground(new Color(220, 90, 80));
            } else if (lawfulCeded) {
                label.setText(text + "   (Lawfully Ceded)");
                label.setForeground(new Color(120, 200, 140));
            }
            section.add(label);
        }
        return section;
    }

    // ─── Claims ───────────────────────────────────────────────────────────

    private JPanel buildClaimsSection() {
        JPanel section = sectionPanel("OUTSTANDING CLAIMS");
        List<String> heldZones = house.getZoneIds();
        List<String> claimed = new ArrayList<>();
        for (Zone zone : zoneManager.getZones()) {
            if (claimManager.hasClaim(house.getId(), zone.getId())) {
                claimed.add(zone.getId());
            }
        }
        if (claimed.isEmpty()) {
            section.add(bodyLabel("No outstanding claims."));
            return section;
        }
        for (String zoneId : claimed) {
            Zone zone = zoneManager.getZone(zoneId);
            String display = zone != null ? zone.getDisplayName() : zoneId.replace("_", " ");
            boolean owned = heldZones.contains(zoneId);
            section.add(bodyLabel(display + (owned ? "  (already held)" : "  (unclaimed land)")));
        }
        return section;
    }

    // ─── Footer ───────────────────────────────────────────────────────────

    private JPanel buildFooter() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BG_DARK);
        panel.setBorder(new EmptyBorder(12, 0, 0, 0));

        JButton close = new JButton("CLOSE");
        close.setFont(UITheme.FONT_BUTTON);
        close.setForeground(UITheme.TEXT_SECONDARY);
        close.setBackground(UITheme.BUTTON_BG);
        close.setBorderPainted(false);
        close.setFocusPainted(false);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.addActionListener(e -> dispose());

        panel.add(close, BorderLayout.EAST);
        return panel;
    }

    // ─── Small helpers ────────────────────────────────────────────────────

    private JPanel sectionPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UITheme.BG_PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
            new EmptyBorder(8, 10, 8, 10)
        ));
        panel.setAlignmentX(LEFT_ALIGNMENT);

        JLabel header = new JLabel(title);
        header.setFont(UITheme.FONT_HEADER);
        header.setForeground(UITheme.TEXT_GOLD);
        panel.add(header);
        panel.add(Box.createVerticalStrut(6));
        return panel;
    }

    private JLabel bodyLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UITheme.FONT_SMALL);
        label.setForeground(UITheme.TEXT_PRIMARY);
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private JLabel boldLabel(String text) {
        JLabel label = bodyLabel(text);
        label.setFont(UITheme.FONT_BODY);
        label.setForeground(UITheme.TEXT_GOLD);
        return label;
    }

    private JLabel italicLabel(String text) {
        JLabel label = new JLabel("<html><div style='width:380px'>" + escape(text) + "</div></html>");
        label.setFont(new Font("Serif", Font.ITALIC, 12));
        label.setForeground(UITheme.TEXT_SECONDARY);
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private String percent(double weight) {
        return Math.round(weight * 100) + "%";
    }

    private String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

private JPanel buildResourcesSection() {
        JPanel section = sectionPanel("RESOURCES");
        section.add(bodyLabel("Gold: " + house.getGold()));
        section.add(bodyLabel("Food: " + house.getFood()));
        String manpowerLabel = house.isBank() ? "Citizen Manpower" : "Manpower";
        section.add(bodyLabel(manpowerLabel + ": " + house.getNobleManpower()));
        if (house.isBank() && bankManager != null) {
            section.add(bodyLabel("Mercenary Pool: " + bankManager.getMercenaryManpower()));
        }
        return section;
    }

}