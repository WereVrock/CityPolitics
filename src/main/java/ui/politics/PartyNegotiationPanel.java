package ui.politics;

import main.core.GameState;
import main.politics.DealOffer;
import main.politics.PoliticalParty;
import main.politics.SideLeader;
import main.politics.VotingSession;
import main.politics.VotingSession.SideDealResult;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import main.politics.NegotiationDialogueGenerator;
import ui.UITheme;

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
        boolean isOracles     = party == gameState.getPartyManager().getOracles();
        boolean canDeal       = session.canDeal(party);
        boolean alreadyDealt  = session.hasDealt(party);

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

        // Main deal section
        DealOffer offer     = new DealOffer(party, session.getScore(party));
        boolean   canAfford = offer.canAfford(gameState.getResources(), gameState.getStats());

        JPanel mainDealRow = new JPanel(new BorderLayout(8, 0));
        mainDealRow.setBackground(UITheme.BG_DARK);

        JLabel costLabel = new JLabel("Main deal — " + offer.getSummary());
        costLabel.setFont(UITheme.FONT_SMALL);
        costLabel.setForeground(canAfford ? UITheme.TEXT_SECONDARY : UITheme.TEXT_RED);

        JButton acceptBtn = new JButton("STRIKE MAIN DEAL");
        acceptBtn.setFont(UITheme.FONT_BUTTON);
        acceptBtn.setForeground(UITheme.TEXT_GOLD);
        acceptBtn.setBackground(UITheme.BUTTON_BG);
        acceptBtn.setBorderPainted(false);
        acceptBtn.setFocusPainted(false);
        acceptBtn.setEnabled(canAfford);
        acceptBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        acceptBtn.addActionListener(e -> {
            offer.apply(gameState.getResources(), gameState.getStats());
            session.applyDeal(party);
            onSessionChanged.run();
            onBack.run();
        });

        mainDealRow.add(costLabel, BorderLayout.CENTER);
        mainDealRow.add(acceptBtn, BorderLayout.EAST);

        // Side deal section (not for oracles, only if party has side leaders)
        JPanel sideDealRow = buildSideDealRow(session, offer);

        panel.add(mainDealRow, BorderLayout.CENTER);
        if (sideDealRow != null) panel.add(sideDealRow, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildSideDealRow(VotingSession session, DealOffer mainOffer) {
        if (party.getSideLeaders().isEmpty()) return null;
        if (session.hasSideDealt(party)) {
            int seats = session.getSideDealtSeats(party);
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
            row.setBackground(UITheme.BG_DARK);
            JLabel lbl = new JLabel("Secondary leader convinced " + seats + " seat(s) to vote with you.");
            lbl.setFont(UITheme.FONT_SMALL);
            lbl.setForeground(UITheme.TEXT_GREEN);
            row.add(lbl);
            return row;
        }

        SideLeader sideLeader = party.getSideLeaders().get(0);
        int halfGold      = mainOffer.getMoneyCost()      / 2;
        int halfInfluence = mainOffer.getInfluenceCost()  / 2;

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
        JLabel costLbl = new JLabel("Cost: ~" + halfGold + "g / " + halfInfluence
                + " inf  |  Convinces 1–" + maxPossible + " seats (random)");
        costLbl.setFont(UITheme.FONT_SMALL);
        costLbl.setForeground(UITheme.TEXT_SECONDARY);

        textCol.add(sideName);
        textCol.add(sidePersonality);
        textCol.add(costLbl);

        boolean canAffordSide = gameState.getResources().getMoney()     >= halfGold
                             && gameState.getResources().getInfluence() >= halfInfluence;

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
                    gameState.getResources(), gameState.getStats());
            JOptionPane.showMessageDialog(this,
                    sideLeader.getName() + ":\n\n\"" + result.message + "\"",
                    "Side Deal Result", JOptionPane.INFORMATION_MESSAGE);
            onSessionChanged.run();
            onBack.run();
        });

        row.add(textCol, BorderLayout.CENTER);
        row.add(sideBtn, BorderLayout.EAST);
        return row;
    }
}