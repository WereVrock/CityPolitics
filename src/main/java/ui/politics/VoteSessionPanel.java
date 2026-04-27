package ui.politics;

import main.core.GameState;
import main.politics.PoliticalParty;
import main.politics.VoteResult;
import main.politics.VotingSession;
import main.politics.VotingSession.PartyVoteIntent;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import main.parameters.GameParameters;
import ui.UITheme;
// NegotiationDialogueGenerator is used only in PartyNegotiationPanel

/**
 * Main vote session screen. Shows parties, their expected votes,
 * player vote selector, deal buttons, and finalize.
 * Swaps to PartyNegotiationPanel when a party row is clicked.
 */
public class VoteSessionPanel extends JPanel {

    private final GameState                    gameState;
    private final Runnable                     onFinalized;
    private final java.util.function.Consumer<JPanel> onSwapPanel;

    private final JPanel   partyRows;
    private final JLabel   outcomeLabel;
    private final JButton  finalizeBtn;

    public VoteSessionPanel(GameState gameState,
                            Runnable onFinalized,
                            java.util.function.Consumer<JPanel> onSwapPanel) {
        this.gameState   = gameState;
        this.onFinalized = onFinalized;
        this.onSwapPanel = onSwapPanel;

        setBackground(UITheme.BG_DARK);
        setLayout(new BorderLayout(0, 0));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        add(buildHeader(),        BorderLayout.NORTH);

        partyRows = new JPanel();
        partyRows.setLayout(new BoxLayout(partyRows, BoxLayout.Y_AXIS));
        partyRows.setBackground(UITheme.BG_DARK);

        JScrollPane scroll = new JScrollPane(partyRows);
        scroll.setBorder(null);
        scroll.setBackground(UITheme.BG_DARK);
        scroll.getViewport().setBackground(UITheme.BG_DARK);
        add(scroll, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(0, 6));
        south.setBackground(UITheme.BG_DARK);
        south.setBorder(new EmptyBorder(8, 0, 0, 0));

        outcomeLabel = new JLabel();
        outcomeLabel.setFont(UITheme.FONT_BODY);
        outcomeLabel.setForeground(UITheme.TEXT_SECONDARY);

        finalizeBtn = new JButton("FINALIZE VOTE  ▶");
        finalizeBtn.setFont(new Font("Serif", Font.BOLD, 14));
        finalizeBtn.setForeground(UITheme.ACCENT_FROST);
        finalizeBtn.setBackground(new Color(25, 45, 65));
        finalizeBtn.setBorderPainted(false);
        finalizeBtn.setFocusPainted(false);
        finalizeBtn.setPreferredSize(new Dimension(0, 44));
        finalizeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        finalizeBtn.addActionListener(e -> finalizeVote());

        JButton backBtn = new JButton("← RETURN TO MAIN");
        backBtn.setFont(UITheme.FONT_BUTTON);
        backBtn.setForeground(UITheme.TEXT_SECONDARY);
        backBtn.setBackground(UITheme.BUTTON_BG);
        backBtn.setBorderPainted(false);
        backBtn.setFocusPainted(false);
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> onFinalized.run());

        JPanel btnRow = new JPanel(new BorderLayout(6, 0));
        btnRow.setBackground(UITheme.BG_DARK);
        btnRow.add(backBtn,     BorderLayout.WEST);
        btnRow.add(finalizeBtn, BorderLayout.CENTER);

        south.add(outcomeLabel, BorderLayout.CENTER);
        south.add(btnRow,       BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);

        refresh();
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BG_DARK);
        panel.setBorder(new EmptyBorder(0, 0, 10, 0));

        VotingSession session = gameState.getActiveSession();
        String actionName = session != null ? session.getAction().getName() : "Vote";

        JLabel title = new JLabel("ASSEMBLY VOTE — " + actionName.toUpperCase());
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_GOLD);

        JLabel sub = new JLabel("Click a party to negotiate. Select your vote below.");
        sub.setFont(UITheme.FONT_SMALL);
        sub.setForeground(UITheme.TEXT_SECONDARY);

        JPanel textCol = new JPanel();
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        textCol.setBackground(UITheme.BG_DARK);
        textCol.add(title);
        textCol.add(Box.createVerticalStrut(2));
        textCol.add(sub);

        panel.add(textCol, BorderLayout.CENTER);
        return panel;
    }

    public void refresh() {
        partyRows.removeAll();
        VotingSession session = gameState.getActiveSession();
        if (session == null) return;

        // Player row first
        partyRows.add(buildPlayerRow(session));
        partyRows.add(Box.createVerticalStrut(4));

        // Oracles second
        PoliticalParty oracles = gameState.getPartyManager().getOracles();
        session.syncOraclesWithPlayer(oracles);
        partyRows.add(buildPartyRow(session, oracles));
        partyRows.add(Box.createVerticalStrut(4));

        // Rest alphabetical, skip oracles
        List<PoliticalParty> parties = session.getParties().stream()
            .filter(p -> p != oracles)
            .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
            .toList();

        for (PoliticalParty party : parties) {
            partyRows.add(buildPartyRow(session, party));
            partyRows.add(Box.createVerticalStrut(4));
        }

        partyRows.revalidate();
        partyRows.repaint();
        updateOutcomeLabel(session);
    }

    private JPanel buildPlayerRow(VotingSession session) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(UITheme.BG_PANEL_LIGHT);
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.ACCENT_FROST, 1),
            new EmptyBorder(8, 12, 8, 12)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        row.setAlignmentX(LEFT_ALIGNMENT);

        JLabel name = new JLabel("YOU  (Supervisor — 1 seat)");
        name.setFont(UITheme.FONT_BUTTON);
        name.setForeground(UITheme.ACCENT_FROST);

        JPanel voteSelector = buildVoteSelector(session);

        row.add(name,         BorderLayout.WEST);
        row.add(voteSelector, BorderLayout.EAST);
        return row;
    }

    private JPanel buildVoteSelector(VotingSession session) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        panel.setBackground(UITheme.BG_PANEL_LIGHT);

        String[] labels = {"YES", "ABSTAIN", "NO"};
        PartyVoteIntent[] intents = {
            PartyVoteIntent.YES, PartyVoteIntent.ABSTAIN, PartyVoteIntent.NO
        };

        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < labels.length; i++) {
            final PartyVoteIntent intent = intents[i];
            JToggleButton btn = new JToggleButton(labels[i]);
            btn.setFont(UITheme.FONT_SMALL);
            btn.setForeground(UITheme.TEXT_GOLD);
            btn.setBackground(UITheme.BUTTON_BG);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setSelected(session.getPlayerIntent() == intent);
            btn.addActionListener(e -> {
                session.setPlayerIntent(intent);
                session.syncOraclesWithPlayer(gameState.getPartyManager().getOracles());
                refresh();
            });
            group.add(btn);
            panel.add(btn);
        }
        return panel;
    }

    private JPanel buildPartyRow(VotingSession session, PoliticalParty party) {
        boolean isOracles   = party == gameState.getPartyManager().getOracles();
        boolean canDeal     = session.canDeal(party);
        boolean alreadyDealt= session.hasDealt(party);
        PartyVoteIntent intent = session.getIntent(party);

        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(UITheme.BG_PANEL);
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
            new EmptyBorder(8, 12, 8, 12)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                openNegotiation(party);
            }
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                row.setBackground(UITheme.BG_PANEL_LIGHT);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                row.setBackground(UITheme.BG_PANEL);
            }
        });

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(UITheme.BG_PANEL);

        JLabel nameLabel = new JLabel(party.getName() + "  (" + party.getSeats() + " seats)");
        nameLabel.setFont(UITheme.FONT_BUTTON);
        nameLabel.setForeground(isOracles ? UITheme.TEXT_GOLD : UITheme.TEXT_PRIMARY);

        JLabel leaderLabel = new JLabel(party.getLeaderName());
        leaderLabel.setFont(UITheme.FONT_SMALL);
        leaderLabel.setForeground(UITheme.TEXT_SECONDARY);

        left.add(nameLabel);
        left.add(leaderLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setBackground(UITheme.BG_PANEL);

        if (alreadyDealt) {
            JLabel dealt = new JLabel("DEAL STRUCK");
            dealt.setFont(UITheme.FONT_SMALL);
            dealt.setForeground(UITheme.TEXT_GREEN);
            right.add(dealt);
        } else if (!isOracles && canDeal) {
            JLabel negotiate = new JLabel("click to negotiate");
            negotiate.setFont(UITheme.FONT_SMALL);
            negotiate.setForeground(UITheme.TEXT_SECONDARY);
            right.add(negotiate);
        }

        JLabel intentLabel = new JLabel(intentText(intent));
        intentLabel.setFont(UITheme.FONT_BUTTON);
        intentLabel.setForeground(intentColor(intent));
        right.add(intentLabel);

        row.add(left,  BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    private void openNegotiation(PoliticalParty party) {
        PartyNegotiationPanel neg = new PartyNegotiationPanel(
            gameState, party,
            () -> onSwapPanel.accept(this),   // back → return to this panel
            () -> {
                onSwapPanel.accept(this);
                refresh();
            }
        );
        onSwapPanel.accept(neg);
    }

    private void updateOutcomeLabel(VotingSession session) {
        int yes     = 0;
        int no      = 0;
        int unknown = 0;

        // player seat
        switch (session.getPlayerIntent()) {
            case YES     -> yes++;
            case NO      -> no++;
            case ABSTAIN -> {}
            case UNKNOWN -> unknown++;
        }

        for (PoliticalParty p : session.getParties()) {
            PartyVoteIntent intent = session.getIntent(p);
            switch (intent) {
                case YES     -> yes     += p.getSeats();
                case NO      -> no      += p.getSeats();
                case UNKNOWN -> unknown += p.getSeats();
                case ABSTAIN -> {}
            }
        }

        int needed = GameParameters.SEATS_NEEDED;
        String status = yes >= needed ? "PASS ✓" : yes + unknown < needed ? "FAIL ✗" : "UNCERTAIN";
        Color  color  = yes >= needed ? UITheme.TEXT_GREEN
                      : yes + unknown < needed ? UITheme.TEXT_RED
                      : UITheme.TEXT_GOLD;
        outcomeLabel.setText("YES: " + yes + "   NO: " + no
            + "   UNKNOWN: " + unknown + "   needed: " + needed
            + "   →  " + status);
        outcomeLabel.setForeground(color);
    }

    private void finalizeVote() {
        VotingSession session = gameState.getActiveSession();
        VoteResult result = gameState.getVoteSessionManager().finalize(
            session, gameState.getResources(), gameState.getStats()
        );

        if (result.isPassed()) {
            session.getAction().applyEffect(
                gameState.getResources(), gameState.getStats()
            );
        }

        gameState.clearActiveSession();
        onFinalized.run();
    }

    private String intentText(PartyVoteIntent intent) {
        return switch (intent) {
            case YES     -> "YES";
            case NO      -> "NO";
            case ABSTAIN -> "ABSTAIN";
            case UNKNOWN -> "UNKNOWN";
        };
    }

    private Color intentColor(PartyVoteIntent intent) {
        return switch (intent) {
            case YES     -> UITheme.TEXT_GREEN;
            case NO      -> UITheme.TEXT_RED;
            case ABSTAIN -> UITheme.TEXT_SECONDARY;
            case UNKNOWN -> UITheme.TEXT_GOLD;
        };
    }
}