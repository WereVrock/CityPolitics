package City.ui.politics;

import City.main.core.GameState;
import City.main.politics.PoliticalParty;
import City.main.politics.VoteResult;
import City.main.politics.VotingSession;
import City.main.politics.VotingSession.PartyVoteIntent;
import City.main.politics.VoteSessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.List;
import City.main.parameters.GameParameters;
import City.ui.UITheme;

/**
 * Main vote session screen.
 * Shows parties, side-deal rows, player vote selector, and finalize button.
 * After finalize, displays the result clearly before returning.
 */
public class VoteSessionPanel extends JPanel {

    private final GameState                              gameState;
    private final java.util.function.BiConsumer<VoteResult, List<String>> onFinalized;
    private final java.util.function.Consumer<JPanel>   onSwapPanel;

    private final JPanel   partyRows;
    private final JLabel   outcomeLabel;
    private final JButton  finalizeBtn;

    // Result display panel (shown after vote)
    private JPanel resultPanel = null;

    public VoteSessionPanel(GameState gameState,
                            java.util.function.BiConsumer<VoteResult, List<String>> onFinalized,
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
        backBtn.addActionListener(e -> onFinalized.accept(null, null));

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

        // Player row
        partyRows.add(buildPlayerRow(session));
        partyRows.add(Box.createVerticalStrut(4));

        // Oracles second
        PoliticalParty oracles = gameState.getPartyManager().getOracles();
        session.syncOraclesWithPlayer(oracles);
        partyRows.add(buildPartyRow(session, oracles));
        partyRows.add(Box.createVerticalStrut(4));

        // Rest sorted
        List<PoliticalParty> parties = session.getParties().stream()
            .filter(p -> p != oracles)
            .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
            .toList();

        for (PoliticalParty party : parties) {
            partyRows.add(buildPartyRow(session, party));
            // Side deal row if struck
            if (session.hasSideDealt(party)) {
                partyRows.add(buildSideDealResultRow(session, party));
            }
            partyRows.add(Box.createVerticalStrut(4));
        }

        partyRows.revalidate();
        partyRows.repaint();
        updateOutcomeLabel(session);
    }

    // ─── Side deal result row shown below main party row ─────────────────────

    private JPanel buildSideDealResultRow(VotingSession session, PoliticalParty party) {
        int seats      = session.getSideDealtSeats(party);
        String leader  = party.getSideLeaders().isEmpty()
                ? "Secondary Leader" : party.getSideLeaders().get(0).getName();

        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(new Color(20, 30, 50));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, new Color(80, 120, 200)),
                new EmptyBorder(5, 14, 5, 12)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        row.setAlignmentX(LEFT_ALIGNMENT);

        JLabel nameLabel = new JLabel("↳ " + leader + "  (side deal — " + seats + " seats)");
        nameLabel.setFont(UITheme.FONT_SMALL);
        nameLabel.setForeground(new Color(160, 190, 255));

        JLabel intentLabel = new JLabel("YES");
        intentLabel.setFont(UITheme.FONT_BUTTON);
        intentLabel.setForeground(UITheme.TEXT_GREEN);

        row.add(nameLabel,  BorderLayout.WEST);
        row.add(intentLabel, BorderLayout.EAST);
        return row;
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
        boolean isOracles    = party == gameState.getPartyManager().getOracles();
        boolean canDeal      = session.canDeal(party);
        boolean alreadyDealt = session.hasDealt(party);
        boolean hasSideDeal  = session.hasSideDealt(party);
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
        left.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { openNegotiation(party); }
        });

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
        right.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { openNegotiation(party); }
        });

        if (alreadyDealt && !hasSideDeal) {
            JLabel dealt = new JLabel("DEAL STRUCK");
            dealt.setFont(UITheme.FONT_SMALL);
            dealt.setForeground(UITheme.TEXT_GREEN);
            right.add(dealt);
        } else if (!isOracles && canDeal && !alreadyDealt) {
            JLabel negotiate = new JLabel("click to negotiate");
            negotiate.setFont(UITheme.FONT_SMALL);
            negotiate.setForeground(UITheme.TEXT_SECONDARY);
            right.add(negotiate);
        }

        // Show seat count breakdown if side deal active
        int sideSeats = session.getSideDealtSeats(party);
        if (sideSeats > 0) {
            JLabel sideLabel = new JLabel("+" + sideSeats + " side");
            sideLabel.setFont(UITheme.FONT_SMALL);
            sideLabel.setForeground(new Color(140, 170, 255));
            right.add(sideLabel);
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
            () -> onSwapPanel.accept(this),
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

        switch (session.getPlayerIntent()) {
            case YES     -> yes++;
            case NO      -> no++;
            case ABSTAIN -> {}
            case UNKNOWN -> unknown++;
        }

        for (PoliticalParty p : session.getParties()) {
            PartyVoteIntent intent = session.getIntent(p);
            int sideSeats = session.getSideDealtSeats(p);
            int mainSeats = p.getSeats() - sideSeats;
            yes += sideSeats; // side seats always YES
            switch (intent) {
                case YES     -> yes     += mainSeats;
                case NO      -> no      += mainSeats;
                case UNKNOWN -> unknown += mainSeats;
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
        List<String> logLines = gameState.getVoteSessionManager().buildResultLog(session, result);

        if (result.isPassed()) {
            session.getAction().applyEffect(
                gameState.getResources(), gameState.getStats()
            );
        }

        gameState.clearActiveSession();
        // Show result panel before returning
        showResultPanel(result, session.getAction().getName(), logLines);
    }

    private void showResultPanel(VoteResult result, String actionName, List<String> logLines) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(UITheme.BG_DARK);
        panel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Result header
        boolean passed = result.isPassed();
        JLabel outcomeHeader = new JLabel(
                passed ? "✓  VOTE PASSED" : "✗  VOTE REJECTED");
        outcomeHeader.setFont(new Font("Serif", Font.BOLD, 22));
        outcomeHeader.setForeground(passed ? UITheme.TEXT_GREEN : UITheme.TEXT_RED);
        outcomeHeader.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel actionLabel = new JLabel(actionName, SwingConstants.CENTER);
        actionLabel.setFont(UITheme.FONT_HEADER);
        actionLabel.setForeground(UITheme.TEXT_GOLD);

        JLabel totals = new JLabel(
                "YES: " + result.getTotalYes()
                + "   NO: " + result.getTotalNo()
                + "   ABSTAIN: " + result.getTotalAbstain()
                + "   (needed: " + result.getSeatsNeeded() + ")",
                SwingConstants.CENTER);
        totals.setFont(UITheme.FONT_BODY);
        totals.setForeground(UITheme.TEXT_SECONDARY);

        JPanel headerBlock = new JPanel();
        headerBlock.setLayout(new BoxLayout(headerBlock, BoxLayout.Y_AXIS));
        headerBlock.setBackground(UITheme.BG_PANEL);
        headerBlock.setBorder(new EmptyBorder(12, 12, 12, 12));
        outcomeHeader.setAlignmentX(CENTER_ALIGNMENT);
        actionLabel.setAlignmentX(CENTER_ALIGNMENT);
        totals.setAlignmentX(CENTER_ALIGNMENT);
        headerBlock.add(outcomeHeader);
        headerBlock.add(Box.createVerticalStrut(4));
        headerBlock.add(actionLabel);
        headerBlock.add(Box.createVerticalStrut(6));
        headerBlock.add(totals);

        // Party breakdown
        JPanel breakdown = new JPanel();
        breakdown.setLayout(new BoxLayout(breakdown, BoxLayout.Y_AXIS));
        breakdown.setBackground(UITheme.BG_DARK);

        for (City.main.politics.VoteScore vs : result.getPartyScores()) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setBackground(UITheme.BG_PANEL);
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_COLOR),
                    new EmptyBorder(4, 10, 4, 10)));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            row.setAlignmentX(LEFT_ALIGNMENT);

            JLabel pName = new JLabel(vs.getParty().getName()
                    + " (" + vs.getParty().getSeats() + ")");
            pName.setFont(UITheme.FONT_SMALL);
            pName.setForeground(UITheme.TEXT_PRIMARY);

            JLabel pVotes = new JLabel("YES:" + vs.getYesSeats()
                    + "  NO:" + vs.getNoSeats()
                    + "  ABS:" + vs.getAbstainSeats());
            pVotes.setFont(UITheme.FONT_SMALL);
            Color rowColor = vs.getYesSeats() > vs.getNoSeats()
                    ? UITheme.TEXT_GREEN : vs.getNoSeats() > vs.getYesSeats()
                    ? UITheme.TEXT_RED : UITheme.TEXT_SECONDARY;
            pVotes.setForeground(rowColor);

            row.add(pName,  BorderLayout.WEST);
            row.add(pVotes, BorderLayout.EAST);
            breakdown.add(row);
        }

        JScrollPane scrollBreakdown = new JScrollPane(breakdown);
        scrollBreakdown.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1));
        scrollBreakdown.setBackground(UITheme.BG_DARK);
        scrollBreakdown.getViewport().setBackground(UITheme.BG_DARK);

        JButton continueBtn = new JButton("CONTINUE  ▶");
        continueBtn.setFont(new Font("Serif", Font.BOLD, 14));
        continueBtn.setForeground(UITheme.ACCENT_FROST);
        continueBtn.setBackground(new Color(25, 45, 65));
        continueBtn.setBorderPainted(false);
        continueBtn.setFocusPainted(false);
        continueBtn.setPreferredSize(new Dimension(0, 44));
        continueBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        continueBtn.addActionListener(e -> onFinalized.accept(result, logLines));

        panel.add(headerBlock,     BorderLayout.NORTH);
        panel.add(scrollBreakdown, BorderLayout.CENTER);
        panel.add(continueBtn,     BorderLayout.SOUTH);

        onSwapPanel.accept(panel);
    }

    private String intentText(PartyVoteIntent intent) {
        return switch (intent) {
            case YES     -> "YES";
            case NO      -> "NO";
            case ABSTAIN -> "ABSTAIN";
            case UNKNOWN -> "?";
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