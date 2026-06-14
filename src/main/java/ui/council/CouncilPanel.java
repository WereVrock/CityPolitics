package ui.council;

import main.nobles.council.*;
import main.nobles.NobleHouse;
import main.nobles.ProtectionManager;
import main.core.GameState;
import main.parameters.GameParameters;
import ui.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * The Noble Council panel — displays voters, impressions, and deal options.
 * Accessed via a new COUNCIL button in the bottom bar.
 */
public class CouncilPanel extends JPanel {

    private final GameState  gameState;
    private final Runnable   onBack;

    private final JPanel     voterListPanel;
    private final JLabel     totalLabel;
    private final JButton    boostBtn;
    private final JButton    finalizeBtn;

    // For UNLAWFUL_ACQUISITION we need a zone selection
    private String selectedZoneId = null;

    public CouncilPanel(GameState gameState, Runnable onBack) {
        this.gameState = gameState;
        this.onBack    = onBack;

        setBackground(UITheme.BG_DARK);
        setLayout(new BorderLayout(0, 0));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.BG_DARK);
        header.setBorder(new EmptyBorder(0, 0, 10, 0));

        JLabel title = new JLabel("NOBLE COUNCIL");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_GOLD);

        JButton back = new JButton("← BACK");
        back.setFont(UITheme.FONT_BUTTON);
        back.setForeground(UITheme.TEXT_SECONDARY);
        back.setBackground(UITheme.BUTTON_BG);
        back.setBorderPainted(false);
        back.setFocusPainted(false);
        back.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        back.addActionListener(e -> onBack.run());

        header.add(title, BorderLayout.WEST);
        header.add(back,  BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        if (!gameState.hasActiveCouncilSession()) {
            add(buildActionSelector(), BorderLayout.CENTER);
            totalLabel  = new JLabel();
            boostBtn    = new JButton();
            finalizeBtn = new JButton();
            voterListPanel = new JPanel();
            return;
        }

        // Active session
        CouncilSession session = gameState.getActiveCouncilSession();
        voterListPanel = new JPanel();
        voterListPanel.setLayout(new BoxLayout(voterListPanel, BoxLayout.Y_AXIS));
        voterListPanel.setBackground(UITheme.BG_DARK);

        JScrollPane scroll = new JScrollPane(voterListPanel);
        scroll.setBorder(null);
        scroll.setBackground(UITheme.BG_DARK);
        scroll.getViewport().setBackground(UITheme.BG_DARK);
        add(scroll, BorderLayout.CENTER);

        // South bar
        JPanel south = new JPanel(new BorderLayout(0, 6));
        south.setBackground(UITheme.BG_DARK);
        south.setBorder(new EmptyBorder(8, 0, 0, 0));

        totalLabel = new JLabel();
        totalLabel.setFont(UITheme.FONT_BODY);

        boostBtn = new JButton("Spend " + GameParameters.COUNCIL_PLAYER_BOOST_INFLUENCE_COST
                + " Influence for +" + GameParameters.COUNCIL_PLAYER_BOOST_IMPRESSION
                + " Impression");
        boostBtn.setFont(UITheme.FONT_BUTTON);
        boostBtn.setForeground(UITheme.TEXT_GOLD);
        boostBtn.setBackground(UITheme.BUTTON_BG);
        boostBtn.setBorderPainted(false);
        boostBtn.setFocusPainted(false);
        boostBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boostBtn.addActionListener(e -> {
            if (session.isPlayerBoostUsed()) return;
            if (gameState.getResources().getInfluence()
                    < GameParameters.COUNCIL_PLAYER_BOOST_INFLUENCE_COST) {
                JOptionPane.showMessageDialog(this, "Not enough influence.");
                return;
            }
            gameState.getResources().spendInfluence(
                    GameParameters.COUNCIL_PLAYER_BOOST_INFLUENCE_COST);
            CouncilVoter pv = session.getPlayerVoter();
            if (pv != null) {
                pv.setImpression(pv.getImpression()
                        + GameParameters.COUNCIL_PLAYER_BOOST_IMPRESSION);
            }
            session.markPlayerBoostUsed();
            refresh();
        });

        finalizeBtn = new JButton("CALL THE VOTE  ▶");
        finalizeBtn.setFont(new Font("Serif", Font.BOLD, 14));
        finalizeBtn.setForeground(UITheme.ACCENT_FROST);
        finalizeBtn.setBackground(new Color(25, 45, 65));
        finalizeBtn.setBorderPainted(false);
        finalizeBtn.setFocusPainted(false);
        finalizeBtn.setPreferredSize(new Dimension(0, 44));
        finalizeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        finalizeBtn.addActionListener(e -> finalizeVote());

        JPanel btnRow = new JPanel(new BorderLayout(6, 0));
        btnRow.setBackground(UITheme.BG_DARK);
        btnRow.add(boostBtn, BorderLayout.WEST);
        btnRow.add(finalizeBtn, BorderLayout.CENTER);

        south.add(totalLabel, BorderLayout.CENTER);
        south.add(btnRow, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);

        refresh();
    }

    private JPanel buildActionSelector() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UITheme.BG_DARK);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel sub = new JLabel("Convene the noble council to pass a realm decree.");
        sub.setFont(UITheme.FONT_BODY);
        sub.setForeground(UITheme.TEXT_SECONDARY);
        sub.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(sub);
        panel.add(Box.createVerticalStrut(20));

        JLabel prestigeLabel = new JLabel("Your Prestige: "
                + gameState.getPlayerPrestige().getPrestige());
        prestigeLabel.setFont(UITheme.FONT_BODY);
        prestigeLabel.setForeground(new Color(210, 170, 80));
        prestigeLabel.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(prestigeLabel);
        panel.add(Box.createVerticalStrut(16));

        for (CouncilAction action : CouncilAction.values()) {
            panel.add(buildActionCard(action));
            panel.add(Box.createVerticalStrut(10));
        }

        panel.add(Box.createVerticalStrut(20));
        panel.add(buildProtectionSection());

        return panel;
    }

    private JPanel buildActionCard(CouncilAction action) {
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBackground(UITheme.BG_PANEL);
        card.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
                new EmptyBorder(12, 14, 12, 14)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        card.setAlignmentX(LEFT_ALIGNMENT);

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setBackground(UITheme.BG_PANEL);

        JLabel name = new JLabel(action.getDisplayName());
        name.setFont(UITheme.FONT_HEADER);
        name.setForeground(UITheme.TEXT_GOLD);

        JLabel desc = new JLabel("<html><body style='width:380px'>"
                + action.getDescription() + "</body></html>");
        desc.setFont(UITheme.FONT_SMALL);
        desc.setForeground(UITheme.TEXT_SECONDARY);

        text.add(name);
        text.add(Box.createVerticalStrut(4));
        text.add(desc);

        JButton conveneBtn = new JButton("CONVENE");
        conveneBtn.setFont(UITheme.FONT_BUTTON);
        conveneBtn.setForeground(UITheme.TEXT_GOLD);
        conveneBtn.setBackground(UITheme.BUTTON_BG);
        conveneBtn.setBorderPainted(false);
        conveneBtn.setFocusPainted(false);
        conveneBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        conveneBtn.addActionListener(e -> startSession(action));

        card.add(text,       BorderLayout.CENTER);
        card.add(conveneBtn, BorderLayout.EAST);
        return card;
    }

    private JPanel buildProtectionSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBackground(UITheme.BG_DARK);
        section.setAlignmentX(LEFT_ALIGNMENT);

        JLabel header = new JLabel("DECLARE PROTECTION");
        header.setFont(UITheme.FONT_HEADER);
        header.setForeground(UITheme.TEXT_GOLD);
        header.setAlignmentX(LEFT_ALIGNMENT);
        section.add(header);
        section.add(Box.createVerticalStrut(4));

        JLabel desc = new JLabel("<html><body style='width:440px'>"
                + "Declare a noble house under your protection. Costs "
                + GameParameters.PROTECTION_INFLUENCE_COST + " influence. "
                + "Grants +" + GameParameters.PROTECTION_TARGET_OPINION_BONUS
                + " opinion to target, " + GameParameters.PROTECTION_RIVAL_OPINION_MALUS
                + " to rivals. You suffer -" + Math.abs(GameParameters.PLAYER_PRESTIGE_PROTECTED_ZONE_LOST)
                + " prestige if they lose a zone.</body></html>");
        desc.setFont(UITheme.FONT_SMALL);
        desc.setForeground(UITheme.TEXT_SECONDARY);
        desc.setAlignmentX(LEFT_ALIGNMENT);
        section.add(desc);
        section.add(Box.createVerticalStrut(8));

        ProtectionManager pm = gameState.getProtectionManager();
        List<main.nobles.NobleHouse> houses = gameState.getNobleHouseManager().getHouses();
        for (main.nobles.NobleHouse house : houses) {
            if (house.isEliminated()) continue;
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setBackground(UITheme.BG_PANEL);
            row.setBorder(new EmptyBorder(6, 10, 6, 10));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            row.setAlignmentX(LEFT_ALIGNMENT);

            boolean protected_ = pm.isUnderProtection(house.getId());
            JLabel houseName = new JLabel(
                    (protected_ ? "🛡 " : "  ") + house.getName()
                    + "  (opinion: " + house.getPlayerOpinion() + ")");
            houseName.setFont(UITheme.FONT_SMALL);
            houseName.setForeground(protected_ ? new Color(120, 200, 100) : UITheme.TEXT_PRIMARY);

            JButton btn;
            if (protected_) {
                btn = new JButton("PROTECTED");
                btn.setEnabled(false);
                btn.setForeground(UITheme.TEXT_SECONDARY);
                btn.setBackground(UITheme.BUTTON_DISABLED);
            } else {
                btn = new JButton("PROTECT (" + GameParameters.PROTECTION_INFLUENCE_COST + " inf)");
                btn.setForeground(UITheme.TEXT_GOLD);
                btn.setBackground(UITheme.BUTTON_BG);
                btn.addActionListener(ev -> declareProtection(house));
            }
            btn.setFont(UITheme.FONT_SMALL);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            row.add(houseName, BorderLayout.WEST);
            row.add(btn,       BorderLayout.EAST);
            section.add(row);
        }

        return section;
    }

    private void declareProtection(main.nobles.NobleHouse house) {
        if (gameState.getResources().getInfluence() < GameParameters.PROTECTION_INFLUENCE_COST) {
            JOptionPane.showMessageDialog(this,
                    "Not enough influence. Need " + GameParameters.PROTECTION_INFLUENCE_COST + ".");
            return;
        }
        gameState.getResources().spendInfluence(GameParameters.PROTECTION_INFLUENCE_COST);
        gameState.getProtectionManager().declareProtection(house.getId());
        house.adjustPlayerOpinion(GameParameters.PROTECTION_TARGET_OPINION_BONUS);

        // Opinion malus for rivals
        main.nobles.RelationshipManager rm =
                gameState.getNobleHouseManager().getRelationships();
        for (main.nobles.NobleHouse other : gameState.getNobleHouseManager().getHouses()) {
            if (other == house || other.isEliminated()) continue;
            if (rm.get(house.getId(), other.getId()) == main.nobles.Relationship.RIVAL
                    || rm.get(house.getId(), other.getId()) == main.nobles.Relationship.HOSTILE) {
                other.adjustPlayerOpinion(GameParameters.PROTECTION_RIVAL_OPINION_MALUS);
            }
        }
        JOptionPane.showMessageDialog(this,
                house.getName() + " is now under your protection.",
                "Protection Declared", JOptionPane.INFORMATION_MESSAGE);
        // Rebuild
        onBack.run();
    }

    private void startSession(CouncilAction action) {
        if (action == CouncilAction.UNLAWFUL_ACQUISITION) {
            selectedZoneId = pickZoneForUnlawful();
            if (selectedZoneId == null) return;
        }

        int oracleOpinion = 50;
        for (main.politics.PoliticalParty p : gameState.getPartyManager().getParties()) {
            if (p.getName().equals("Oracles")) { oracleOpinion = p.getPlayerOpinion(); break; }
        }

        CouncilSession session = gameState.getCouncilSessionManager().createSession(
                action,
                gameState.getPlayerPrestige().getPrestige(),
                oracleOpinion,
                new java.util.ArrayList<>(gameState.getNobleHouseManager().getHouses()));
        gameState.setActiveCouncilSession(session);
        // Rebuild panel with session active
        onBack.run();
    }

    private String pickZoneForUnlawful() {
        // Show dialog to pick a zone with an owner
        java.util.List<main.map.Zone> ownedZones = new java.util.ArrayList<>();
        for (main.map.Zone z : gameState.getZoneManager().getZones()) {
            if (!z.isDesolate()
                    && gameState.getNobleHouseManager().getOwnerOfZone(z.getId()) != null) {
                ownedZones.add(z);
            }
        }
        if (ownedZones.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No noble-owned zones available.");
            return null;
        }
        ownedZones.sort(java.util.Comparator.comparing(main.map.Zone::getDisplayName));
        String[] options = ownedZones.stream()
                .map(z -> {
                    main.nobles.NobleHouse o =
                            gameState.getNobleHouseManager().getOwnerOfZone(z.getId());
                    return z.getDisplayName() + " (owned by "
                            + (o != null ? o.getName() : "?") + ")";
                })
                .toArray(String[]::new);
        int choice = JOptionPane.showOptionDialog(this,
                "Select the zone to declare as unlawfully acquired:",
                "Unlawful Acquisition — Select Zone",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);
        if (choice < 0) return null;
        return ownedZones.get(choice).getId();
    }

    public void refresh() {
        CouncilSession session = gameState.getActiveCouncilSession();
        if (session == null || voterListPanel == null) return;

        voterListPanel.removeAll();

        // Player row
        CouncilVoter pv = session.getPlayerVoter();
        if (pv != null) voterListPanel.add(buildVoterRow(session, pv, true));
        voterListPanel.add(Box.createVerticalStrut(4));

        // Oracle row
        CouncilVoter ov = session.getOracleVoter();
        if (ov != null) voterListPanel.add(buildVoterRow(session, ov, false));
        voterListPanel.add(Box.createVerticalStrut(4));

        // Noble voters sorted by impression desc
        java.util.List<CouncilVoter> nobles = session.getNoblevoters();
        nobles.sort((a, b) -> Integer.compare(b.getImpression(), a.getImpression()));
        for (CouncilVoter voter : nobles) {
            voterListPanel.add(buildVoterRow(session, voter, false));
            voterListPanel.add(Box.createVerticalStrut(3));
        }

        voterListPanel.revalidate();
        voterListPanel.repaint();

        // Update totals label
        int yes   = session.getTotalYes();
        int no    = session.getTotalNo();
        int total = session.getTotalImpression();
        String status = yes > no ? "PASSING ✓" : yes < no ? "FAILING ✗" : "TIED";
        Color statusColor = yes > no ? UITheme.TEXT_GREEN
                : yes < no ? UITheme.TEXT_RED : UITheme.TEXT_GOLD;
        totalLabel.setText("YES: " + yes + "   NO: " + no
                + "   TOTAL: " + total + "   →  " + status);
        totalLabel.setForeground(statusColor);

        boolean canBoost = !session.isPlayerBoostUsed()
                && gameState.getResources().getInfluence()
                >= GameParameters.COUNCIL_PLAYER_BOOST_INFLUENCE_COST;
        boostBtn.setEnabled(canBoost);
        if (session.isPlayerBoostUsed()) {
            boostBtn.setText("Impression boost used");
            boostBtn.setForeground(UITheme.TEXT_SECONDARY);
        }
    }

    private JPanel buildVoterRow(CouncilSession session, CouncilVoter voter,
                                  boolean isPlayer) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(isPlayer ? new Color(30, 28, 50) : UITheme.BG_PANEL);
        row.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(
                        isPlayer ? UITheme.ACCENT_FROST : UITheme.BORDER_COLOR, 1),
                new EmptyBorder(6, 12, 6, 12)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        row.setAlignmentX(LEFT_ALIGNMENT);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(row.getBackground());

        String typeLabel = switch (voter.getType()) {
            case PLAYER           -> "You";
            case ORACLE           -> "Arch Oracle";
            case PRESTIGIOUS_NOBLE-> "⭐ Prestigious";
            case MINOR_NOBLE      -> "Minor";
        };
        JLabel nameLabel = new JLabel(voter.getDisplayName()
                + "  [" + typeLabel + "]"
                + "  " + voter.getImpression() + " impression");
        nameLabel.setFont(UITheme.FONT_BUTTON);
        nameLabel.setForeground(isPlayer ? UITheme.ACCENT_FROST : UITheme.TEXT_PRIMARY);

        left.add(nameLabel);

        row.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setBackground(row.getBackground());

        if (!isPlayer && !voter.isDealt()
                && voter.getType() != CouncilVoter.VoterType.ORACLE) {
            JButton dealBtn = new JButton("negotiate");
            dealBtn.setFont(UITheme.FONT_SMALL);
            dealBtn.setForeground(UITheme.TEXT_SECONDARY);
            dealBtn.setBackground(UITheme.BUTTON_BG);
            dealBtn.setBorderPainted(false);
            dealBtn.setFocusPainted(false);
            dealBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            dealBtn.addActionListener(e -> openDealDialog(session, voter));
            right.add(dealBtn);
        } else if (voter.isDealt()) {
            JLabel dealt = new JLabel("DEAL ✓");
            dealt.setFont(UITheme.FONT_SMALL);
            dealt.setForeground(UITheme.TEXT_GREEN);
            right.add(dealt);
        }

        // Stance selector for player; fixed label for others
        if (isPlayer) {
            String[] opts = { "YES", "NO" };
            JComboBox<String> stanceBox = new JComboBox<>(opts);
            stanceBox.setSelectedItem(voter.getStance() == CouncilVoter.Stance.NO ? "NO" : "YES");
            stanceBox.setFont(UITheme.FONT_SMALL);
            stanceBox.addActionListener(e -> {
                voter.setStance("NO".equals(stanceBox.getSelectedItem())
                        ? CouncilVoter.Stance.NO : CouncilVoter.Stance.YES);
                refresh();
            });
            right.add(stanceBox);
        } else {
            JLabel stanceLabel = new JLabel(stanceText(voter.getStance()));
            stanceLabel.setFont(UITheme.FONT_BUTTON);
            stanceLabel.setForeground(stanceColor(voter.getStance()));
            right.add(stanceLabel);
        }

        row.add(right, BorderLayout.EAST);
        return row;
    }

    private void openDealDialog(CouncilSession session, CouncilVoter voter) {
        CouncilDealOffer offer = session.getDealOffer(voter,
                gameState.getNobleHouseManager().getClaimManager(),
                gameState.getProtectionManager(),
                new java.util.ArrayList<>(gameState.getNobleHouseManager().getHouses()),
                new java.util.Random());
        if (offer == null) return;

        String message = "\"" + voter.getDisplayName() + "\" asks:\n\n"
                + offer.getDescription() + "\n\n"
                + "Accept this deal? They will vote YES.";
        boolean canAfford = offer.canAfford(gameState.getResources());
        int choice = JOptionPane.showConfirmDialog(this,
                message + (canAfford ? "" : "\n\n⚠ You cannot afford this."),
                "Council Deal", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;
        if (!canAfford) {
            JOptionPane.showMessageDialog(this, "Cannot afford this deal.");
            return;
        }

        // Apply deal
        applyDeal(offer);
        voter.setStance(CouncilVoter.Stance.YES);
        voter.setDealt(true);
        refresh();
    }

    private void applyDeal(CouncilDealOffer offer) {
        main.resources.ResourcePool res = gameState.getResources();
        main.nobles.ClaimManager cm = gameState.getNobleHouseManager().getClaimManager();
        switch (offer.getType()) {
            case GOLD      -> res.spendMoney(offer.getCost());
            case INFLUENCE -> res.spendInfluence(offer.getCost());
            case MANPOWER  -> res.spendManpower(offer.getCost());
            case GRANT_CLAIM -> {
                NobleHouse house = offer.getVoter().getHouse();
                if (house != null && offer.getTargetZoneId() != null) {
                    cm.addClaim(house.getId(), offer.getTargetZoneId());
                }
            }
            case REVOKE_RIVAL_CLAIM -> {
                if (offer.getTargetZoneId() != null && offer.getTargetHouseId() != null) {
                    cm.removeClaim(offer.getTargetHouseId(), offer.getTargetZoneId());
                }
            }
            case DECLARE_PROTECTION -> {
                NobleHouse house = offer.getVoter().getHouse();
                if (house != null) {
                    gameState.getProtectionManager().declareProtection(house.getId());
                    house.adjustPlayerOpinion(GameParameters.PROTECTION_TARGET_OPINION_BONUS);
                }
            }
        }
    }

    private void finalizeVote() {
        CouncilSession session = gameState.getActiveCouncilSession();
        if (session == null) return;

        boolean passed = session.isPassingCurrently();
        String result = passed ? "✓ COUNCIL DECREE PASSED" : "✗ COUNCIL DECREE REJECTED";
        Color  color  = passed ? UITheme.TEXT_GREEN : UITheme.TEXT_RED;

        if (passed) {
            java.util.List<String> effectLog = gameState.getCouncilSessionManager().applyOutcome(
                    session, session.getAction(), selectedZoneId,
                    gameState.getNobleHouseManager(),
                    gameState.getZoneManager(),
                    gameState.getArmyManager(),
                    gameState.getResources(),
                    gameState.getPlayerPrestige(),
                    gameState.getProtectionManager());
            JOptionPane.showMessageDialog(this,
                    result + "\n\n" + String.join("\n", effectLog),
                    "Council Vote Result", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, result,
                    "Council Vote Result", JOptionPane.INFORMATION_MESSAGE);
        }

        gameState.clearActiveCouncilSession();
        selectedZoneId = null;
        onBack.run();
    }

    private String stanceText(CouncilVoter.Stance s) {
        return switch (s) {
            case YES      -> "YES";
            case NO       -> "NO";
            case UNDECIDED-> "?";
        };
    }

    private Color stanceColor(CouncilVoter.Stance s) {
        return switch (s) {
            case YES      -> UITheme.TEXT_GREEN;
            case NO       -> UITheme.TEXT_RED;
            case UNDECIDED-> UITheme.TEXT_GOLD;
        };
    }
}