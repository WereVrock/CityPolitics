package City.ui.politics;

import City.main.core.GameState;
import City.main.politics.DealOffer;
import City.main.politics.PoliticalParty;
import City.main.politics.SideLeader;
import City.main.politics.VotingSession;
import City.main.politics.SideDealResult;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import City.main.parameters.GameParameters;
import City.main.politics.NegotiationDialogueGenerator;
import City.ui.UITheme;

/**
 * Shows the party leader portrait, in-character dialogue, deal offer,
 * and side-deal option with the secondary leader.
 */
public class PartyNegotiationPanel extends JPanel {

    private final GameState      gameState;
    private final PoliticalParty party;
    private final Runnable       onBack;
    private final Runnable       onSessionChanged;

    public PartyNegotiationPanel(GameState gameState,
                                 PoliticalParty party,
                                 Runnable onBack,
                                 Runnable onSessionChanged) {
        this.gameState        = gameState;
        this.party            = party;
        this.onBack           = onBack;
        this.onSessionChanged = onSessionChanged;

        setBackground(UITheme.BG_DARK);
        setLayout(new BorderLayout(0, 12));
        setBorder(new EmptyBorder(16, 24, 16, 24));

        add(buildBackButton(),   BorderLayout.NORTH);
        add(buildLeaderPanel(),  BorderLayout.CENTER);
        add(buildActionPanel(),  BorderLayout.SOUTH);
    }

    private JPanel buildBackButton() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.setBackground(UITheme.BG_DARK);
        JButton back = new JButton("← BACK TO VOTE");
        back.setFont(UITheme.FONT_BUTTON);
        back.setForeground(UITheme.TEXT_SECONDARY);
        back.setBackground(UITheme.BUTTON_BG);
        back.setBorderPainted(false);
        back.setFocusPainted(false);
        back.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        back.addActionListener(e -> onBack.run());
        p.add(back);
        return p;
    }

    private JPanel buildLeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 0));
        panel.setBackground(UITheme.BG_PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
            new EmptyBorder(20, 20, 20, 20)
        ));

        panel.add(buildLargePortrait(), BorderLayout.WEST);
        panel.add(buildDialoguePanel(), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildLargePortrait() {
        JPanel portrait = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.BG_PANEL_LIGHT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(UITheme.BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                int cx = getWidth() / 2;
                g2.setColor(UITheme.TEXT_SECONDARY);
                g2.fillOval(cx - 30, 20, 60, 60);
                g2.fillRoundRect(cx - 40, 88, 80, 70, 10, 10);
                g2.setColor(UITheme.BG_PANEL);
                g2.setFont(UITheme.FONT_TITLE);
                String init = party.getName().substring(0, 1);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(init, cx - fm.stringWidth(init)/2, 60);
            }
        };
        portrait.setPreferredSize(new Dimension(140, 180));
        portrait.setBackground(UITheme.BG_PANEL);
        return portrait;
    }

    private JPanel buildDialoguePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UITheme.BG_PANEL);

        JLabel leaderName = new JLabel(party.getLeaderName());
        leaderName.setFont(UITheme.FONT_TITLE);
        leaderName.setForeground(UITheme.TEXT_GOLD);

        JLabel partyName = new JLabel(party.getName() + "  ·  " + party.getSeats() + " seats");
        partyName.setFont(UITheme.FONT_SMALL);
        partyName.setForeground(UITheme.TEXT_SECONDARY);

        JTextArea dialogue = new JTextArea(buildDialogue());
        dialogue.setFont(new Font("Serif", Font.ITALIC, 13));
        dialogue.setForeground(UITheme.TEXT_PRIMARY);
        dialogue.setBackground(UITheme.BG_PANEL);
        dialogue.setEditable(false);
        dialogue.setLineWrap(true);
        dialogue.setWrapStyleWord(true);
        dialogue.setMinimumSize(new Dimension(200, 80));
        dialogue.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
            new EmptyBorder(8, 10, 8, 10)
        ));

        panel.add(leaderName);
        panel.add(Box.createVerticalStrut(2));
        panel.add(partyName);
        panel.add(Box.createVerticalStrut(12));
        panel.add(dialogue);
        return panel;
    }

    private String buildDialogue() {
        return NegotiationDialogueGenerator.generate(
            party,
            gameState.getActiveSession(),
            gameState.getPartyManager().getOracles(),
            gameState.getResources(),
            gameState.getStats()
        );
    }

private JPanel buildActionPanel() {
    JPanel panel = new JPanel(new BorderLayout(0, 6));
    panel.setBackground(UITheme.BG_DARK);

    VotingSession session = gameState.getActiveSession();
    boolean isOracles    = party == gameState.getPartyManager().getOracles();
    boolean canDeal      = session.canDeal(party);
    boolean alreadyDealt = session.hasDealt(party);

    if (isOracles || alreadyDealt) {
        if (alreadyDealt) {
            JLabel done = new JLabel("Deal already struck with this party.");
            done.setFont(UITheme.FONT_SMALL);
            done.setForeground(UITheme.TEXT_GREEN);
            panel.add(done, BorderLayout.NORTH);
        }
        return panel;
    }

    if (!canDeal) {
        JLabel locked = new JLabel("This party's position is too firm to negotiate.");
        locked.setFont(UITheme.FONT_SMALL);
        locked.setForeground(UITheme.TEXT_SECONDARY);
        panel.add(locked, BorderLayout.NORTH);
        return panel;
    }

    DealOffer offer     = new DealOffer(party, session.getScore(party));
    boolean   canAfford = offer.canAfford(gameState.getResources(), gameState.getStats());

    JPanel mainDealRow = new JPanel(new BorderLayout(8, 0));
    mainDealRow.setBackground(UITheme.BG_DARK);

    // Build cost label — show favour requirement prominently
    String costText;
    Color  costColor;
    if (offer.isFavourOnly()) {
        costText  = "Main deal — Requires " + offer.getFavourCost() + " favour"
                  + (offer.getHappinessMalus() > 0
                      ? " + " + offer.getHappinessMalus() + " happiness" : "");
        costColor = new Color(220, 170, 80);
    } else {
        costText  = "Main deal — " + offer.getSummary();
        costColor = canAfford ? UITheme.TEXT_SECONDARY : UITheme.TEXT_RED;
    }

    JLabel costLabel = new JLabel(costText);
    costLabel.setFont(UITheme.FONT_SMALL);
    costLabel.setForeground(costColor);

    JButton acceptBtn = new JButton(offer.isFavourOnly() ? "OWE A FAVOUR" : "STRIKE MAIN DEAL");
    acceptBtn.setFont(UITheme.FONT_BUTTON);
    acceptBtn.setForeground(offer.isFavourOnly() ? new Color(220, 170, 80) : UITheme.TEXT_GOLD);
    acceptBtn.setBackground(UITheme.BUTTON_BG);
    acceptBtn.setBorderPainted(false);
    acceptBtn.setFocusPainted(false);
    acceptBtn.setEnabled(canAfford);
    acceptBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    if (offer.isFavourOnly()) {
        acceptBtn.setToolTipText("You will owe this party " + offer.getFavourCost() + " favour(s).");
    }
    acceptBtn.addActionListener(e -> {
        if (offer.isFavourOnly()) {
            // Apply favour debt
            party.setFavour(party.getFavour() - offer.getFavourCost());
            if (offer.getHappinessMalus() > 0) {
                gameState.getStats().reduceHappiness(offer.getHappinessMalus());
            }
        } else {
            offer.apply(gameState.getResources(), gameState.getStats(),
                    party, gameState.getPropagandaManager());
        }
        session.applyDeal(party);
        onSessionChanged.run();
        onBack.run();
    });

    mainDealRow.add(costLabel, BorderLayout.CENTER);
    mainDealRow.add(acceptBtn, BorderLayout.EAST);

    JPanel sideDealRow = buildSideDealRow(session, offer);

    panel.add(mainDealRow, BorderLayout.CENTER);
    if (sideDealRow != null) panel.add(sideDealRow, BorderLayout.SOUTH);
    return panel;
}

private JPanel buildSideDealRow(VotingSession session, DealOffer mainOffer) {
    if (party.getSideLeaders().isEmpty()) return null;
    if (session.hasSideDealt(party)) {
        int     seats      = session.getSideDealtSeats(party);
        SideLeader leader  = party.getSideLeaders().get(0);
        return buildSideDealResultPanel(leader, seats);
    }

    SideLeader sideLeader = party.getSideLeaders().get(0);

    // Side deals always use resources (no favour), with minimums applied
    int halfGold      = Math.max(GameParameters.DEAL_MIN_MONEY,
                                  mainOffer.getMoneyCost() / 2);
    int halfInfluence = Math.max(GameParameters.DEAL_MIN_INFLUENCE,
                                  mainOffer.getInfluenceCost() / 2);
    // If main offer was favour-only, side deal uses flat minimums
    if (mainOffer.isFavourOnly()) {
        halfGold      = GameParameters.DEAL_MIN_MONEY;
        halfInfluence = GameParameters.DEAL_MIN_INFLUENCE;
    }

    final int finalGold      = halfGold;
    final int finalInfluence = halfInfluence;

    JPanel row = new JPanel(new BorderLayout(8, 0));
    row.setBackground(UITheme.BG_DARK);
    row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
            new EmptyBorder(6, 8, 6, 8)));

    JPanel textCol = new JPanel();
    textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
    textCol.setBackground(UITheme.BG_DARK);

    JLabel sideName = new JLabel("Side deal — " + sideLeader.getName());
    sideName.setFont(UITheme.FONT_BUTTON);
    sideName.setForeground(new Color(180, 200, 255));

    JLabel sidePersonality = new JLabel("<html><i>" + sideLeader.getPersonality() + "</i></html>");
    sidePersonality.setFont(UITheme.FONT_SMALL);
    sidePersonality.setForeground(UITheme.TEXT_SECONDARY);

    int maxPossible = Math.max(1, (int)(party.getSeats() * 0.75));
    JLabel costLbl = new JLabel("Cost: " + finalGold + "g / " + finalInfluence
            + " inf  |  Convinces 1–" + maxPossible + " seats (random)");
    costLbl.setFont(UITheme.FONT_SMALL);
    costLbl.setForeground(UITheme.TEXT_SECONDARY);

    textCol.add(sideName);
    textCol.add(sidePersonality);
    textCol.add(costLbl);

    boolean canAffordSide = gameState.getResources().getMoney()     >= finalGold
                         && gameState.getResources().getInfluence() >= finalInfluence;

    JButton sideBtn = new JButton("NEGOTIATE SIDE DEAL");
    sideBtn.setFont(UITheme.FONT_SMALL);
    sideBtn.setForeground(new Color(180, 200, 255));
    sideBtn.setBackground(new Color(30, 35, 60));
    sideBtn.setBorderPainted(false);
    sideBtn.setFocusPainted(false);
    sideBtn.setEnabled(canAffordSide);
    sideBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    sideBtn.addActionListener(e -> {
        SideDealResult result = session.applySideDeal(party,
                gameState.getResources(), gameState.getStats(),
                gameState.getPropagandaManager());
        showSideDealResultDialog(sideLeader, result);
        onSessionChanged.run();
        onBack.run();
    });

    row.add(textCol, BorderLayout.CENTER);
    row.add(sideBtn, BorderLayout.EAST);
    return row;
}

    private void showSideDealResultDialog(SideLeader sideLeader, SideDealResult result) {
        javax.swing.JDialog dialog = new javax.swing.JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(this) instanceof java.awt.Frame
                        ? (java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(this) : null,
                "Side Deal — " + sideLeader.getName(), true);
        dialog.setMinimumSize(new Dimension(460, 320));
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(true);
        dialog.getContentPane().setBackground(UITheme.BG_PANEL);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(UITheme.BG_PANEL);
        root.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel portrait = new JPanel() {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.BG_PANEL_LIGHT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(UITheme.BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                int cx = getWidth() / 2;
                g2.setColor(new Color(180, 200, 255));
                g2.fillOval(cx - 20, 14, 40, 40);
                g2.setColor(new Color(160, 180, 220));
                g2.fillRoundRect(cx - 25, 58, 50, 40, 8, 8);
            }
        };
        portrait.setPreferredSize(new Dimension(90, 110));
        portrait.setBackground(UITheme.BG_PANEL);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBackground(UITheme.BG_PANEL);

        JLabel nameLabel = new JLabel(sideLeader.getName());
        nameLabel.setFont(UITheme.FONT_HEADER);
        nameLabel.setForeground(new Color(180, 200, 255));

        String message;
        if (result.seatsWon <= 0) {
            message = "\"I tried my best, but I couldn't convince anyone to cross the party line. "
                    + "I will vote with you myself, for whatever that is worth.\"";
        } else if (result.seatsWon == 1) {
            message = "\"It wasn't easy, but I managed to bring one seat over to your side. "
                    + "Don't expect me to make a habit of it.\"";
        } else {
            message = "\"I worked the room as best I could. "
                    + result.seatsWon + " of my colleagues will cast their vote for you. "
                    + "The rest wouldn't hear of it.\"";
        }

        JTextArea messageArea = new JTextArea(message);
        messageArea.setFont(new java.awt.Font("Serif", java.awt.Font.ITALIC, 13));
        messageArea.setForeground(UITheme.TEXT_PRIMARY);
        messageArea.setBackground(UITheme.BG_PANEL);
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setPreferredSize(new Dimension(300, 80));
        messageArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
                new EmptyBorder(8, 10, 8, 10)));

        String seatsText = result.seatsWon > 0
                ? "Seats convinced: " + result.seatsWon + " / " + party.getSeats()
                : "No additional seats convinced — side leader votes with you alone.";
        JLabel seatsLabel = new JLabel("<html><body style='width:280px'>" + seatsText + "</body></html>");
        seatsLabel.setFont(UITheme.FONT_SMALL);
        seatsLabel.setForeground(result.seatsWon > 0 ? UITheme.TEXT_GREEN : UITheme.TEXT_SECONDARY);

        textPanel.add(nameLabel);
        textPanel.add(Box.createVerticalStrut(8));
        textPanel.add(messageArea);
        textPanel.add(Box.createVerticalStrut(6));
        textPanel.add(seatsLabel);

        JButton closeBtn = new JButton("CONTINUE");
        closeBtn.setFont(UITheme.FONT_BUTTON);
        closeBtn.setForeground(UITheme.TEXT_GOLD);
        closeBtn.setBackground(UITheme.BUTTON_BG);
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.addActionListener(ev -> dialog.dispose());

        root.add(portrait,  BorderLayout.WEST);
        root.add(textPanel, BorderLayout.CENTER);
        root.add(closeBtn,  BorderLayout.SOUTH);

        dialog.add(root);
        dialog.pack();
        dialog.setVisible(true);
    }

private JPanel buildSideDealResultPanel(SideLeader sideLeader, int seatsWon) {
    JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
    row.setBackground(UITheme.BG_DARK);
    JLabel lbl = new JLabel(sideLeader.getName() + " convinced " + seatsWon + " seat(s) to vote with you.");
    lbl.setFont(UITheme.FONT_SMALL);
    lbl.setForeground(UITheme.TEXT_GREEN);
    row.add(lbl);
    return row;
}

}