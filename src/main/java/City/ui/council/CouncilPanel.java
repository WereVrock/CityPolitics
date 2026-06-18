package City.ui.council;

import City.main.nobles.NobleHouse;
import City.main.nobles.ProtectionManager;
import City.main.core.GameState;
import City.main.nobles.council.CouncilAction;
import City.main.nobles.council.CouncilDealOffer;
import City.main.nobles.council.CouncilSession;
import City.main.nobles.council.CouncilVoter;
import City.main.parameters.NobleCouncilParams;
import City.main.parameters.PrestigeXPParams;
import City.main.parameters.ProtectionParams;
import City.ui.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.Random;

/**
 * Realm Council panel — renamed from Noble Council.
 * Limited to 1 session per turn (tracked via LegislationManager.realmCouncilUsedThisTurn).
 * Player trust bonus is applied to player impression.
 * Unlawful Acquisition opens a zone picker before convening.
 * Clicking Convene immediately shows the voting session.
 */
public class CouncilPanel extends JPanel {

    private final GameState gameState;
    private final Runnable  onBack;
    // Callback so MainWindow can open map zone picker
    private java.util.function.Consumer<java.util.function.Consumer<String>> unlawfulZonePickerCallback;
    // Fired right after a council session is created, so MainWindow can
    // immediately show the voting view instead of falling back to onBack.
    private Runnable onSessionStarted;

    private final JPanel     voterListPanel;
    private final JLabel     totalLabel;
    private final JButton    boostBtn;
    private final JButton    finalizeBtn;

    private String  selectedZoneId     = null;
    private boolean zonePickerActive   = false;

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

        JLabel title = new JLabel("REALM COUNCIL");
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

        // Active session — voter list
        CouncilSession session = gameState.getActiveCouncilSession();
        voterListPanel = new JPanel();
        voterListPanel.setLayout(new BoxLayout(voterListPanel, BoxLayout.Y_AXIS));
        voterListPanel.setBackground(UITheme.BG_DARK);

        JScrollPane scroll = new JScrollPane(voterListPanel);
        scroll.setBorder(null);
        scroll.setBackground(UITheme.BG_DARK);
        scroll.getViewport().setBackground(UITheme.BG_DARK);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        add(scroll, BorderLayout.CENTER);

        // South bar
        JPanel south = new JPanel(new BorderLayout(0, 6));
        south.setBackground(UITheme.BG_DARK);
        south.setBorder(new EmptyBorder(8, 0, 0, 0));

        totalLabel = new JLabel();
        totalLabel.setFont(UITheme.FONT_BODY);

        boostBtn = new JButton("Spend " + NobleCouncilParams.COUNCIL_PLAYER_BOOST_INFLUENCE_COST
                + " Influence for +" + NobleCouncilParams.COUNCIL_PLAYER_BOOST_IMPRESSION + " Impression");
        boostBtn.setFont(UITheme.FONT_BUTTON);
        boostBtn.setForeground(UITheme.TEXT_GOLD);
        boostBtn.setBackground(UITheme.BUTTON_BG);
        boostBtn.setBorderPainted(false);
        boostBtn.setFocusPainted(false);
        boostBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boostBtn.addActionListener(e -> {
            if (session.isPlayerBoostUsed()) return;
            if (gameState.getResources().getInfluence() < NobleCouncilParams.COUNCIL_PLAYER_BOOST_INFLUENCE_COST) {
                JOptionPane.showMessageDialog(this, "Not enough influence.");
                return;
            }
            gameState.getResources().spendInfluence(NobleCouncilParams.COUNCIL_PLAYER_BOOST_INFLUENCE_COST);
            CouncilVoter pv = session.getPlayerVoter();
            if (pv != null) pv.setImpression(pv.getImpression() + NobleCouncilParams.COUNCIL_PLAYER_BOOST_IMPRESSION);
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

    public void setUnlawfulZonePickerCallback(
            java.util.function.Consumer<java.util.function.Consumer<String>> cb) {
        this.unlawfulZonePickerCallback = cb;
    }

public void setSessionStartedCallback(Runnable r) {
        this.onSessionStarted = r;
    }

private JPanel buildActionSelector() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UITheme.BG_DARK);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        boolean usedThisTurn = gameState.getLegislationManager().isRealmCouncilUsedThisTurn();

        JLabel sub = new JLabel(usedThisTurn
                ? "Realm council already convened this turn."
                : "Convene the realm council to pass a decree.");
        sub.setFont(UITheme.FONT_BODY);
        sub.setForeground(usedThisTurn ? UITheme.TEXT_RED : UITheme.TEXT_SECONDARY);
        sub.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(sub);
        panel.add(Box.createVerticalStrut(6));

        int trust = gameState.getPlayerPrestige().getTrust();
        int trustBonus = gameState.getPlayerPrestige().getTrustCouncilBonus();
        JLabel trustLabel = new JLabel("Your Prestige: " + gameState.getPlayerPrestige().getPrestige()
                + "   Trust: " + trust + "/10"
                + "   Council bonus: " + (trustBonus >= 0 ? "+" : "") + trustBonus + " impression");
        trustLabel.setFont(UITheme.FONT_BODY);
        trustLabel.setForeground(trust >= 5 ? new Color(210, 170, 80) : UITheme.TEXT_RED);
        trustLabel.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(trustLabel);
        panel.add(Box.createVerticalStrut(16));

        for (CouncilAction action : CouncilAction.values()) {
            panel.add(buildActionCard(action, usedThisTurn));
            panel.add(Box.createVerticalStrut(10));
        }

        return panel;
    }

    private JPanel buildActionCard(CouncilAction action, boolean disabled) {
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

        // Strip "owner might refuse" from description for Unlawful
        String desc = action.getDescription();
        JLabel descLabel = new JLabel("<html><body style='width:380px'>" + desc + "</body></html>");
        descLabel.setFont(UITheme.FONT_SMALL);
        descLabel.setForeground(UITheme.TEXT_SECONDARY);

        text.add(name);
        text.add(Box.createVerticalStrut(4));
        text.add(descLabel);

        JButton conveneBtn = new JButton("CONVENE");
        conveneBtn.setFont(UITheme.FONT_BUTTON);
        conveneBtn.setForeground(disabled ? UITheme.TEXT_SECONDARY : UITheme.TEXT_GOLD);
        conveneBtn.setBackground(disabled ? UITheme.BUTTON_DISABLED : UITheme.BUTTON_BG);
        conveneBtn.setBorderPainted(false);
        conveneBtn.setFocusPainted(false);
        conveneBtn.setEnabled(!disabled);
        conveneBtn.setCursor(disabled ? Cursor.getDefaultCursor()
                : Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (!disabled) conveneBtn.addActionListener(e -> startSession(action));

        card.add(text,       BorderLayout.CENTER);
        card.add(conveneBtn, BorderLayout.EAST);
        return card;
    }

    private void startSession(CouncilAction action) {
        if (gameState.getLegislationManager().isRealmCouncilUsedThisTurn()) {
            JOptionPane.showMessageDialog(this,
                    "The realm council has already been convened this turn.");
            return;
        }

        if (action == CouncilAction.UNLAWFUL_ACQUISITION) {
            openUnlawfulZonePicker(action);
        } else {
            doStartSession(action);
        }
    }

    private void openUnlawfulZonePicker(CouncilAction action) {
        java.util.List<City.main.map.Zone> validZones = buildUnlawfulValidZones();
        if (validZones.isEmpty()) {
            City.ui.ThemedDialogs.showInfo(this, "Unlawful Acquisition",
                    "No zone qualifies — each must be noble-owned with at least one other claimant.");
            return;
        }

        // Build inline zone picker dialog with "Pick from Map" option
        JDialog picker = new JDialog(
                SwingUtilities.getWindowAncestor(this) instanceof java.awt.Frame
                        ? (java.awt.Frame) SwingUtilities.getWindowAncestor(this) : null,
                "Unlawful Acquisition — Select Zone", true);
        picker.setSize(480, 520);
        picker.setLocationRelativeTo(this);
        picker.setResizable(true);
        picker.getContentPane().setBackground(City.ui.UITheme.BG_PANEL);
        picker.setLayout(new BorderLayout());

        JLabel title = new JLabel("  Select zone to declare unlawfully acquired");
        title.setFont(City.ui.UITheme.FONT_HEADER);
        title.setForeground(City.ui.UITheme.TEXT_GOLD);
        title.setBorder(new EmptyBorder(14, 14, 10, 14));
        title.setBackground(City.ui.UITheme.BG_PANEL_LIGHT);
        title.setOpaque(true);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(City.ui.UITheme.BG_DARK);
        listPanel.setBorder(new EmptyBorder(8, 10, 8, 10));

        ButtonGroup group = new ButtonGroup();
        final City.main.map.Zone[] selected = {validZones.get(0)};
        selectedZoneId = selected[0].getId();

        for (City.main.map.Zone z : validZones) {
            City.main.nobles.NobleHouse owner =
                    gameState.getNobleHouseManager().getOwnerOfZone(z.getId());
            java.util.List<String> claimants = new java.util.ArrayList<>();
            for (City.main.nobles.NobleHouse h : gameState.getNobleHouseManager().getHouses()) {
                if (h != owner && !h.isEliminated()
                        && gameState.getNobleHouseManager().getClaimManager()
                                .hasClaim(h.getId(), z.getId())) {
                    claimants.add(h.getName().replace("House ", ""));
                }
            }

            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setBackground(City.ui.UITheme.BG_PANEL);
            row.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createLineBorder(City.ui.UITheme.BORDER_COLOR, 1),
                    new EmptyBorder(8, 10, 8, 10)));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));

            JRadioButton rb = new JRadioButton();
            rb.setBackground(City.ui.UITheme.BG_PANEL);
            if (z == selected[0]) rb.setSelected(true);
            group.add(rb);

            JPanel info = new JPanel();
            info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
            info.setBackground(City.ui.UITheme.BG_PANEL);

            JLabel nameLabel = new JLabel(z.getDisplayName());
            nameLabel.setFont(City.ui.UITheme.FONT_BUTTON);
            nameLabel.setForeground(City.ui.UITheme.TEXT_GOLD);

            String ownerText = owner != null ? owner.getName().replace("House ", "") : "?";
            JLabel detailLabel = new JLabel("Owner: " + ownerText
                    + "   Claimants: " + String.join(", ", claimants));
            detailLabel.setFont(City.ui.UITheme.FONT_SMALL);
            detailLabel.setForeground(City.ui.UITheme.TEXT_SECONDARY);

            info.add(nameLabel);
            info.add(detailLabel);
            row.add(rb,   BorderLayout.WEST);
            row.add(info, BorderLayout.CENTER);

            final City.main.map.Zone capturedZone = z;
            row.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    rb.setSelected(true);
                    selected[0]    = capturedZone;
                    selectedZoneId = capturedZone.getId();
                }
            });
            listPanel.add(row);
            listPanel.add(Box.createVerticalStrut(6));
        }

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.setBackground(City.ui.UITheme.BG_DARK);
        scroll.getViewport().setBackground(City.ui.UITheme.BG_DARK);
        scroll.getVerticalScrollBar().setUnitIncrement(24);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btnRow.setBackground(City.ui.UITheme.BG_PANEL);

        // "Pick from Map" button — opens map picker overlay
        if (unlawfulZonePickerCallback != null) {
            JButton mapPickBtn = new JButton("🗺 Pick from Map");
            mapPickBtn.setFont(City.ui.UITheme.FONT_BUTTON);
            mapPickBtn.setForeground(City.ui.UITheme.ACCENT_FROST);
            mapPickBtn.setBackground(City.ui.UITheme.BUTTON_BG);
            mapPickBtn.setBorderPainted(false);
            mapPickBtn.setFocusPainted(false);
            mapPickBtn.addActionListener(e -> {
                picker.dispose();
                unlawfulZonePickerCallback.accept(zoneId -> {
                    if (zoneId == null) return; // cancelled
                    // Validate: must be a valid unlawful zone
                    boolean valid = validZones.stream()
                            .anyMatch(z -> z.getId().equals(zoneId));
                    if (!valid) {
                        JOptionPane.showMessageDialog(null,
                                "That zone is not eligible for unlawful acquisition.",
                                "Invalid Selection", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    startSessionWithZone(action, zoneId);
                });
            });
            btnRow.add(mapPickBtn);
        }

        JButton cancelBtn = new JButton("CANCEL");
        cancelBtn.setFont(City.ui.UITheme.FONT_BUTTON);
        cancelBtn.setForeground(City.ui.UITheme.TEXT_SECONDARY);
        cancelBtn.setBackground(City.ui.UITheme.BUTTON_BG);
        cancelBtn.setBorderPainted(false);
        cancelBtn.setFocusPainted(false);
        cancelBtn.addActionListener(e -> {
            selectedZoneId = null;
            picker.dispose();
        });

        JButton confirmBtn = new JButton("SELECT ZONE");
        confirmBtn.setFont(City.ui.UITheme.FONT_BUTTON);
        confirmBtn.setForeground(City.ui.UITheme.TEXT_GOLD);
        confirmBtn.setBackground(City.ui.UITheme.BUTTON_BG);
        confirmBtn.setBorderPainted(false);
        confirmBtn.setFocusPainted(false);
        confirmBtn.addActionListener(e -> {
            String zoneId = selected[0].getId();
            picker.dispose();
            startSessionWithZone(action, zoneId);
        });

        btnRow.add(cancelBtn);
        btnRow.add(confirmBtn);

        picker.add(title,  BorderLayout.NORTH);
        picker.add(scroll, BorderLayout.CENTER);
        picker.add(btnRow, BorderLayout.SOUTH);
        picker.setVisible(true);
    }

private java.util.List<City.main.map.Zone> buildUnlawfulValidZones() {
        java.util.List<City.main.map.Zone> result = new java.util.ArrayList<>();
        for (City.main.map.Zone z : gameState.getZoneManager().getZones()) {
            if (z.isDesolate()) continue;
            City.main.map.ZoneState state = gameState.getZoneManager().getState(z.getId());
            if (state != null && state.isLawfullyAcquired()) continue;
            City.main.nobles.NobleHouse owner =
                    gameState.getNobleHouseManager().getOwnerOfZone(z.getId());
            if (owner == null) continue;
            boolean hasClaimant = false;
            for (City.main.nobles.NobleHouse h : gameState.getNobleHouseManager().getHouses()) {
                if (h != owner && !h.isEliminated()
                        && gameState.getNobleHouseManager().getClaimManager()
                                .hasClaim(h.getId(), z.getId())) {
                    hasClaimant = true;
                    break;
                }
            }
            if (hasClaimant) result.add(z);
        }
        result.sort(java.util.Comparator.comparing(City.main.map.Zone::getDisplayName));
        return result;
    }

private void doStartSession(CouncilAction action) {
        if (gameState.getLegislationManager().isRealmCouncilUsedThisTurn()) {
            JOptionPane.showMessageDialog(this,
                    "The realm council has already been convened this turn.");
            return;
        }

        int oracleOpinion = 50;
        for (City.main.politics.PoliticalParty p : gameState.getPartyManager().getParties()) {
            if (p.getName().equals("Oracles")) { oracleOpinion = p.getPlayerOpinion(); break; }
        }

        int trustBonus = gameState.getPlayerPrestige().getTrustCouncilBonus();

        String unlawfulZoneId = action == CouncilAction.UNLAWFUL_ACQUISITION
                ? gameState.getPendingUnlawfulAcquisitionZoneId()
                : null;

        CouncilSession session = gameState.getCouncilSessionManager().createSession(
                action,
                gameState.getPlayerPrestige().getPrestige(),
                oracleOpinion,
                trustBonus,
                new java.util.ArrayList<>(gameState.getNobleHouseManager().getHouses()),
                unlawfulZoneId,
                gameState.getNobleHouseManager().getClaimManager(),
                gameState.getNobleHouseManager().getRelationships());
        gameState.setActiveCouncilSession(session);
        gameState.getLegislationManager().markRealmCouncilUsedThisTurn();

        // Immediately open the voting view rather than returning to the main screen.
        if (onSessionStarted != null) {
            onSessionStarted.run();
        } else {
            onBack.run();
        }
    }

// pickZoneForUnlawful replaced by openUnlawfulZonePicker — kept empty for safety
    @SuppressWarnings("unused")
    private String pickZoneForUnlawful() { return null; }

    public void refresh() {
        CouncilSession session = gameState.getActiveCouncilSession();
        if (session == null || voterListPanel == null) return;

        voterListPanel.removeAll();

        CouncilVoter pv = session.getPlayerVoter();
        if (pv != null) {
            JPanel row = buildVoterRow(session, pv, true);
            if (row != null) voterListPanel.add(row);
        }
        voterListPanel.add(Box.createVerticalStrut(4));

        CouncilVoter ov = session.getOracleVoter();
        if (ov != null) {
            JPanel row = buildVoterRow(session, ov, false);
            if (row != null) voterListPanel.add(row);
        }
        voterListPanel.add(Box.createVerticalStrut(4));

        java.util.List<CouncilVoter> nobles = session.getNoblevoters();
        nobles.sort((a, b) -> Integer.compare(b.getImpression(), a.getImpression()));
        for (CouncilVoter voter : nobles) {
            City.main.nobles.NobleHouse house = voter.getHouse();
            if (house != null && house.isEliminated()) continue;
            JPanel row = buildVoterRow(session, voter, false);
            if (row != null) voterListPanel.add(row);
            voterListPanel.add(Box.createVerticalStrut(3));
        }

        voterListPanel.revalidate();
        voterListPanel.repaint();

        int yes   = session.getTotalYes();
        int no    = session.getTotalNo();
        String status = yes > no ? "PASSING ✓" : yes < no ? "FAILING ✗" : "TIED";
        Color statusColor = yes > no ? UITheme.TEXT_GREEN
                : yes < no ? UITheme.TEXT_RED : UITheme.TEXT_GOLD;
        totalLabel.setText("YES: " + yes + "   NO: " + no
                + "   TOTAL: " + session.getTotalImpression() + "   →  " + status);
        totalLabel.setForeground(statusColor);

        boolean canBoost = !session.isPlayerBoostUsed()
                && gameState.getResources().getInfluence()
                >= NobleCouncilParams.COUNCIL_PLAYER_BOOST_INFLUENCE_COST;
        boostBtn.setEnabled(canBoost);
        if (session.isPlayerBoostUsed()) {
            boostBtn.setText("Impression boost used");
            boostBtn.setForeground(UITheme.TEXT_SECONDARY);
        }
    }

    private JPanel buildVoterRow(CouncilSession session, CouncilVoter voter, boolean isPlayer) {
        if (!isPlayer && voter.getType() != CouncilVoter.VoterType.ORACLE) {
            City.main.nobles.NobleHouse house = voter.getHouse();
            if (house != null && house.isEliminated()) return null;
        }

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
            dealBtn.addActionListener(e -> openNegotiateDialog(session, voter));
            right.add(dealBtn);
        } else if (voter.isDealt()) {
            JLabel dealt = new JLabel("DEAL ✓");
            dealt.setFont(UITheme.FONT_SMALL);
            dealt.setForeground(UITheme.TEXT_GREEN);
            right.add(dealt);
        }

        if (isPlayer) {
            String[] opts = {"YES", "NO"};
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

    private void openNegotiateDialog(CouncilSession session, CouncilVoter voter) {
        CouncilDealOffer offer = session.getDealOffer(voter,
                gameState.getNobleHouseManager().getClaimManager(),
                gameState.getProtectionManager(),
                new java.util.ArrayList<>(gameState.getNobleHouseManager().getHouses()),
                new Random());
        if (offer == null) return;

        // Build a themed in-character dialog
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this) instanceof Frame
                ? (Frame) SwingUtilities.getWindowAncestor(this) : null,
                "Negotiate — " + voter.getDisplayName(), true);
        dialog.setSize(500, 380);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);
        dialog.getContentPane().setBackground(UITheme.BG_PANEL);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(UITheme.BG_PANEL);
        root.setBorder(new EmptyBorder(18, 20, 14, 20));

        // Portrait placeholder
        JPanel portrait = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.BG_PANEL_LIGHT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(UITheme.BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                int cx = getWidth() / 2;
                g2.setColor(UITheme.TEXT_SECONDARY);
                g2.fillOval(cx - 18, 12, 36, 36);
                g2.fillRoundRect(cx - 22, 52, 44, 38, 8, 8);
            }
        };
        portrait.setPreferredSize(new Dimension(90, 110));
        portrait.setBackground(UITheme.BG_PANEL);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(UITheme.BG_PANEL);

        JLabel nameLabel = new JLabel(voter.getDisplayName());
        nameLabel.setFont(UITheme.FONT_HEADER);
        nameLabel.setForeground(UITheme.TEXT_GOLD);

        // In-character line based on stance
        String line = generateNegotiateDialogue(voter);
        JTextArea dialogue = new JTextArea(line);
        dialogue.setFont(new Font("Serif", Font.ITALIC, 13));
        dialogue.setForeground(UITheme.TEXT_PRIMARY);
        dialogue.setBackground(UITheme.BG_PANEL);
        dialogue.setEditable(false);
        dialogue.setLineWrap(true);
        dialogue.setWrapStyleWord(true);
        dialogue.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
                new EmptyBorder(8, 10, 8, 10)));

        JLabel demandLabel = new JLabel("<html><b>Demands:</b> " + offer.getDescription() + "</html>");
        demandLabel.setFont(UITheme.FONT_BODY);
        demandLabel.setForeground(offer.canAfford(gameState.getResources())
                ? UITheme.TEXT_GOLD : UITheme.TEXT_RED);

        infoPanel.add(nameLabel);
        infoPanel.add(Box.createVerticalStrut(8));
        infoPanel.add(dialogue);
        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(demandLabel);

        JPanel topRow = new JPanel(new BorderLayout(14, 0));
        topRow.setBackground(UITheme.BG_PANEL);
        topRow.add(portrait,  BorderLayout.WEST);
        topRow.add(infoPanel, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setBackground(UITheme.BG_PANEL);

        JButton acceptBtn = new JButton("ACCEPT");
        acceptBtn.setFont(UITheme.FONT_BUTTON);
        acceptBtn.setForeground(UITheme.TEXT_GOLD);
        acceptBtn.setBackground(UITheme.BUTTON_BG);
        acceptBtn.setBorderPainted(false);
        acceptBtn.setFocusPainted(false);
        acceptBtn.setEnabled(offer.canAfford(gameState.getResources()));
        acceptBtn.addActionListener(e -> {
            applyDeal(offer);
            voter.setStance(CouncilVoter.Stance.YES);
            voter.setDealt(true);
            dialog.dispose();
            refresh();
        });

        JButton declineBtn = new JButton("DECLINE");
        declineBtn.setFont(UITheme.FONT_BUTTON);
        declineBtn.setForeground(UITheme.TEXT_SECONDARY);
        declineBtn.setBackground(UITheme.BUTTON_BG);
        declineBtn.setBorderPainted(false);
        declineBtn.setFocusPainted(false);
        declineBtn.addActionListener(e -> dialog.dispose());

        btnRow.add(declineBtn);
        btnRow.add(acceptBtn);

        root.add(topRow, BorderLayout.CENTER);
        root.add(btnRow, BorderLayout.SOUTH);
        dialog.add(root);
        dialog.setVisible(true);
    }

    private String generateNegotiateDialogue(CouncilVoter voter) {
        int impression = voter.getImpression();
        CouncilVoter.Stance stance = voter.getStance();
        if (stance == CouncilVoter.Stance.YES) {
            return "\"I am inclined to support this decree. But nothing comes without a price.\"";
        }
        if (stance == CouncilVoter.Stance.NO) {
            return "\"I have reservations about this. If you want my impression, you will need to offer something substantial.\"";
        }
        if (voter.getType() == CouncilVoter.VoterType.PRESTIGIOUS_NOBLE) {
            return "\"The council watches carefully. Show me that this serves more than your own ambitions and we can talk.\"";
        }
        return "\"I haven't made up my mind yet. What are you willing to offer?\"";
    }

    private void applyDeal(CouncilDealOffer offer) {
        City.main.resources.ResourcePool res = gameState.getResources();
        City.main.nobles.ClaimManager cm = gameState.getNobleHouseManager().getClaimManager();
        switch (offer.getType()) {
            case GOLD      -> res.spendMoney(offer.getCost());
            case INFLUENCE -> res.spendInfluence(offer.getCost());
            case MANPOWER  -> res.spendManpower(offer.getCost());
            case GRANT_CLAIM -> {
                City.main.nobles.NobleHouse house = offer.getVoter().getHouse();
                if (house != null && offer.getTargetZoneId() != null)
                    cm.addClaim(house.getId(), offer.getTargetZoneId());
            }
            case REVOKE_RIVAL_CLAIM -> {
                if (offer.getTargetZoneId() != null && offer.getTargetHouseId() != null)
                    cm.removeClaim(offer.getTargetHouseId(), offer.getTargetZoneId());
            }
            case DECLARE_PROTECTION -> {
                City.main.nobles.NobleHouse house = offer.getVoter().getHouse();
                if (house != null) {
                    gameState.getProtectionManager().declareProtection(house.getId());
                    house.adjustPlayerOpinion(ProtectionParams.PROTECTION_TARGET_OPINION_BONUS);
                }
            }
        }
    }

private void finalizeVote() {
        CouncilSession session = gameState.getActiveCouncilSession();
        if (session == null) return;

        boolean passed = session.isPassingCurrently();

        String zoneIdForOutcome = selectedZoneId != null
                ? selectedZoneId
                : gameState.getPendingUnlawfulAcquisitionZoneId();

        if (passed) {
            java.util.List<String> effectLog = gameState.getCouncilSessionManager().applyOutcome(
                    session, session.getAction(), zoneIdForOutcome,
                    gameState.getNobleHouseManager(),
                    gameState.getZoneManager(),
                    gameState.getArmyManager(),
                    gameState.getResources(),
                    gameState.getPlayerPrestige(),
                    gameState.getProtectionManager());
            handleOutcomeLog(session.getAction(), effectLog);
        } else {
            City.ui.ThemedDialogs.showInfo(this, "Realm Council Result",
                    "✗ REALM COUNCIL DECREE REJECTED");
        }

        gameState.clearActiveCouncilSession();
        gameState.clearPendingUnlawfulAcquisitionZoneId();
        selectedZoneId = null;
        onBack.run();
    }

private void handleOutcomeLog(City.main.nobles.council.CouncilAction action,
                                   java.util.List<String> effectLog) {
        if (action == City.main.nobles.council.CouncilAction.UNLAWFUL_ACQUISITION
                && !effectLog.isEmpty()
                && "REFUSED".equals(effectLog.get(0))) {
            // Owner refused — show dedicated dialog
            String ownerName = effectLog.size() > 1 ? effectLog.get(1) : "The owner";
            String zoneDisplay = effectLog.size() > 2
                    ? effectLog.get(2).replace("_", " ") : "the zone";
            showRefusalDialog(ownerName, zoneDisplay);
        } else {
            City.ui.ThemedDialogs.showInfo(this, "Realm Council Result",
                    "✓ REALM COUNCIL DECREE PASSED\n\n" + String.join("\n", effectLog));
        }
    }

private void showRefusalDialog(String ownerName, String zoneDisplay) {
        JDialog d = new JDialog(
                SwingUtilities.getWindowAncestor(this) instanceof java.awt.Frame
                        ? (java.awt.Frame) SwingUtilities.getWindowAncestor(this) : null,
                "Unlawful Acquisition — Refused", true);
        d.setUndecorated(true);
        d.setSize(500, 320);
        d.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(City.ui.UITheme.BG_PANEL);
        root.setBorder(javax.swing.BorderFactory.createLineBorder(
                City.ui.UITheme.BORDER_COLOR, 2));

        JLabel title = new JLabel("  ⚑ Decree Passed — But Defied");
        title.setFont(City.ui.UITheme.FONT_TITLE);
        title.setForeground(new Color(220, 150, 40));
        title.setBackground(City.ui.UITheme.BG_PANEL_LIGHT);
        title.setOpaque(true);
        title.setBorder(new EmptyBorder(12, 12, 10, 12));

        JLabel body = new JLabel("<html><body style='width:380px; padding:8px'>"
                + "<b>" + ownerName + "</b> refuses to cede <b>" + zoneDisplay
                + "</b> — their forces are strong enough to resist the council's ruling.<br><br>"
                + "The zone has been marked as <b>unlawfully acquired</b>. "
                + "Each turn they hold it, you will lose prestige. "
                + "After " + City.main.parameters.NobleCouncilParams.UNLAWFUL_RETURN_TURNS
                + " turns the zone will be forcibly transferred.<br><br>"
                + "While the zone is marked unlawful:<br>"
                + "• Attacking " + ownerName + " in retaliation is <b>justified</b><br>"
                + "• Defending " + zoneDisplay + " against them is <b>justified</b>"
                + "</body></html>");
        body.setFont(City.ui.UITheme.FONT_BODY);
        body.setForeground(City.ui.UITheme.TEXT_PRIMARY);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btns.setBackground(City.ui.UITheme.BG_PANEL);
        JButton ok = new JButton("UNDERSTOOD");
        ok.setFont(City.ui.UITheme.FONT_BUTTON);
        ok.setForeground(City.ui.UITheme.TEXT_GOLD);
        ok.setBackground(City.ui.UITheme.BUTTON_BG);
        ok.setBorderPainted(false);
        ok.setFocusPainted(false);
        ok.addActionListener(e -> d.dispose());
        btns.add(ok);

        root.add(title, BorderLayout.NORTH);
        root.add(body,  BorderLayout.CENTER);
        root.add(btns,  BorderLayout.SOUTH);
        d.setContentPane(root);
        d.setVisible(true);
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

/**
     * Starts an Unlawful Acquisition session for a zone chosen via the map picker.
     * Stores the zone on GameState so it survives this CouncilPanel instance being
     * discarded and recreated by MainWindow.showCouncilView() after the picker closes.
     */

private void startSessionWithZone(CouncilAction action, String zoneId) {
        System.out.println("[UNLAWFUL-DEBUG] startSessionWithZone called with zoneId=" + zoneId
                + " on CouncilPanel instance=" + System.identityHashCode(this));
        gameState.setPendingUnlawfulAcquisitionZoneId(zoneId);
        System.out.println("[UNLAWFUL-DEBUG] gameState.pendingUnlawfulAcquisitionZoneId now="
                + gameState.getPendingUnlawfulAcquisitionZoneId()
                + " on gameState instance=" + System.identityHashCode(gameState));
        doStartSession(action);
    }

}