package ui;

import main.core.GameState;
import main.politics.DealOffer;
import main.politics.PoliticalParty;
import main.politics.VotingSession;
import main.parameters.GameParameters;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Shows the party leader portrait, in-character dialogue, and deal offer.
 * Opened from VoteSessionPanel when player clicks a party row.
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
                // head
                g2.setColor(UITheme.TEXT_SECONDARY);
                g2.fillOval(cx - 30, 20, 60, 60);
                // body
                g2.fillRoundRect(cx - 40, 88, 80, 70, 10, 10);
                // party initial
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
        VotingSession session = gameState.getActiveSession();
        boolean canDeal       = session.canDeal(party);
        boolean isOracles     = party == gameState.getPartyManager().getOracles();
        double  score         = session.getScore(party);
        boolean alreadyDealt  = session.hasDealt(party);

        if (isOracles) {
            return "\"The stars spoke of your coming long before you drew breath, child. "
                 + "Where you lead, we shall follow. The Arch Oracle has seen it.\"";
        }
        if (alreadyDealt) {
            return "\"We have already reached our understanding. "
                 + "You have our support. Do not make us regret it.\"";
        }
        if (!canDeal) {
            if (score > 0) {
                return "\"" + party.getLeaderName().split(" ")[0]
                    + " needs no convincing on this matter. "
                    + "Our vote is already given. There is nothing to discuss.\"";
            } else {
                return "\"Do not waste my time. "
                    + "We are firmly against this and no sum of gold or honeyed words will move us. "
                    + "Take your proposal back to whatever shadow it came from.\"";
            }
        }

        DealOffer offer = new DealOffer(party, score);
        return "\"" + buildDemandLine(offer) + "\"";
    }

    private String buildDemandLine(DealOffer offer) {
        String name = party.getLeaderName().split(" ")[0];
        String actionName = gameState.getActiveSession().getAction().getName();
        StringBuilder sb = new StringBuilder();
        sb.append("You want our support for ").append(actionName).append(", do you. ");

        if (offer.getMoneyCost() > 0 && offer.getInfluenceCost() > 0) {
            sb.append(name).append(" is not cheap. ")
              .append(offer.getMoneyCost()).append(" gold and ")
              .append(offer.getInfluenceCost()).append(" influence — that is our price. ");
        } else if (offer.getMoneyCost() > 0) {
            sb.append("We want ").append(offer.getMoneyCost()).append(" gold. Simple as that. ");
        } else {
            sb.append("Show us ").append(offer.getInfluenceCost()).append(" influence worth of respect. ");
        }
        if (offer.getHappinessMalus() > 0) {
            sb.append("And you will owe the people a debt too — they will feel this. ");
        }
        sb.append("Agree, and you have our seats.");
        return sb.toString();
    }

    private JPanel buildActionPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(UITheme.BG_DARK);

        VotingSession session = gameState.getActiveSession();
        boolean isOracles     = party == gameState.getPartyManager().getOracles();
        boolean canDeal       = session.canDeal(party);
        boolean alreadyDealt  = session.hasDealt(party);

        if (isOracles || !canDeal || alreadyDealt) {
            return panel; // no deal controls needed
        }

        DealOffer offer      = new DealOffer(party, session.getScore(party));
        boolean   canAfford  = offer.canAfford(gameState.getResources(), gameState.getStats());

        JLabel costLabel = new JLabel(offer.getSummary());
        costLabel.setFont(UITheme.FONT_SMALL);
        costLabel.setForeground(canAfford ? UITheme.TEXT_SECONDARY : UITheme.TEXT_RED);

        JButton acceptBtn = new JButton("STRIKE DEAL");
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

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnRow.setBackground(UITheme.BG_DARK);
        btnRow.add(acceptBtn);

        panel.add(costLabel, BorderLayout.CENTER);
        panel.add(btnRow,    BorderLayout.EAST);
        return panel;
    }
}