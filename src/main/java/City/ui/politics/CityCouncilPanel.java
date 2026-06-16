package City.ui.politics;

import City.main.core.GameState;
import City.main.politics.NoblePartyVoteManager;
import City.main.politics.PoliticalParty;
import City.ui.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * City Council panel — shows the current seat distribution and party composition.
 * Separate from Realm Council (noble decrees).
 */
public class CityCouncilPanel extends JPanel {

    private final GameState gameState;
    private final Runnable  onBack;

    public CityCouncilPanel(GameState gameState, Runnable onBack) {
        this.gameState = gameState;
        this.onBack    = onBack;

        setBackground(UITheme.BG_DARK);
        setLayout(new BorderLayout(0, 10));
        setBorder(new EmptyBorder(12, 14, 12, 14));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(),   BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BG_DARK);
        panel.setBorder(new EmptyBorder(0, 0, 8, 0));

        JLabel title = new JLabel("🏛 CITY COUNCIL");
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

        JLabel subtitle = new JLabel("The assembly that votes on formal actions and legislation.");
        subtitle.setFont(UITheme.FONT_SMALL);
        subtitle.setForeground(UITheme.TEXT_SECONDARY);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(UITheme.BG_DARK);
        left.add(title);
        left.add(subtitle);

        panel.add(left, BorderLayout.CENTER);
        panel.add(back, BorderLayout.EAST);
        return panel;
    }

    private JScrollPane buildBody() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(UITheme.BG_DARK);
        body.setBorder(new EmptyBorder(4, 0, 4, 0));

        List<PoliticalParty> parties = gameState.getPartyManager().getParties();

        int totalSeats = parties.stream().mapToInt(PoliticalParty::getSeats).sum() + 1; // +1 player
        int threshold  = City.main.parameters.VotingParams.SEATS_NEEDED;

        // Summary bar
        JPanel summary = new JPanel(new BorderLayout(8, 0));
        summary.setBackground(new Color(22, 16, 32));
        summary.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
                new EmptyBorder(10, 14, 10, 14)));
        summary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        summary.setAlignmentX(LEFT_ALIGNMENT);

        JLabel summaryLabel = new JLabel("Total seats: " + totalSeats
                + "   |   Majority threshold: " + threshold + " seats");
        summaryLabel.setFont(UITheme.FONT_BODY);
        summaryLabel.setForeground(UITheme.TEXT_GOLD);
        summary.add(summaryLabel, BorderLayout.WEST);
        body.add(summary);
        body.add(Box.createVerticalStrut(10));

        // Seat distribution bar
        body.add(buildSeatBar(parties, totalSeats));
        body.add(Box.createVerticalStrut(12));

        // Party rows
        int maxSeats = parties.stream().mapToInt(PoliticalParty::getSeats).max().orElse(1);
        for (PoliticalParty party : parties) {
            if (party.getSeats() == 0 && !party.isUnelected()) continue;
            body.add(buildPartyRow(party, maxSeats));
            body.add(Box.createVerticalStrut(5));
        }

        // Player row
        body.add(Box.createVerticalStrut(8));
        JPanel playerRow = buildPlayerRow();
        body.add(playerRow);
        body.add(Box.createVerticalStrut(8));

        // Next election
        int turns = gameState.getElectionManager().getTurnsUntilElection();
        JLabel elLabel = new JLabel("⚑ Next election in " + turns + " turn(s)");
        elLabel.setFont(UITheme.FONT_BODY);
        elLabel.setForeground(turns <= 2 ? new Color(220, 180, 60) : UITheme.TEXT_SECONDARY);
        elLabel.setAlignmentX(LEFT_ALIGNMENT);
        body.add(elLabel);

        body.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.setBackground(UITheme.BG_DARK);
        scroll.getViewport().setBackground(UITheme.BG_DARK);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        return scroll;
    }

    private JPanel buildSeatBar(List<PoliticalParty> parties, int total) {
        JPanel container = new JPanel(new BorderLayout(0, 4));
        container.setBackground(UITheme.BG_DARK);
        container.setAlignmentX(LEFT_ALIGNMENT);

        JLabel label = new JLabel("Seat Distribution:");
        label.setFont(UITheme.FONT_BUTTON);
        label.setForeground(UITheme.TEXT_SECONDARY);

        JPanel bar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                java.awt.Color[] colors = {
                    new java.awt.Color(180, 60, 60), new java.awt.Color(60, 120, 200),
                    new java.awt.Color(60, 160, 80), new java.awt.Color(180, 140, 40),
                    new java.awt.Color(120, 60, 180), new java.awt.Color(40, 160, 160),
                    new java.awt.Color(200, 100, 40), new java.awt.Color(140, 140, 60)
                };
                int x = 0; int cIdx = 0;
                int threshold = City.main.parameters.VotingParams.SEATS_NEEDED;
                for (PoliticalParty p : parties) {
                    if (p.getSeats() == 0) { cIdx++; continue; }
                    int w = (int)((double) p.getSeats() / total * getWidth());
                    g2.setColor(colors[cIdx % colors.length]);
                    g2.fillRect(x, 0, w, getHeight());
                    cIdx++;
                    x += w;
                }
                // Threshold line
                int tx = (int)((double) threshold / total * getWidth());
                g2.setColor(new java.awt.Color(255, 240, 100, 200));
                g2.setStroke(new java.awt.BasicStroke(2f, java.awt.BasicStroke.CAP_ROUND,
                        java.awt.BasicStroke.JOIN_ROUND, 1f, new float[]{4, 4}, 0f));
                g2.drawLine(tx, 0, tx, getHeight());
                g2.setStroke(new java.awt.BasicStroke(1f));
                g2.setColor(UITheme.TEXT_GOLD);
                g2.setFont(UITheme.FONT_SMALL);
                g2.drawString("⚑" + threshold, tx + 2, getHeight() - 2);
            }
        };
        bar.setPreferredSize(new Dimension(0, 24));
        bar.setBackground(new Color(15, 12, 22));
        bar.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1));

        container.add(label, BorderLayout.NORTH);
        container.add(bar,   BorderLayout.CENTER);
        return container;
    }

    private JPanel buildPartyRow(PoliticalParty party, int maxSeats) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(party.isUnelected() ? new Color(24, 20, 38) : UITheme.BG_PANEL);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
                new EmptyBorder(8, 12, 8, 12)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        row.setAlignmentX(LEFT_ALIGNMENT);

        String emblem = City.main.politics.PartyEmblemRegistry.getEmblem(party.getName());
        JLabel nameLabel = new JLabel(emblem + "  " + party.getName()
                + (party.isUnelected() ? "  [fixed]" : ""));
        nameLabel.setFont(UITheme.FONT_BUTTON);
        nameLabel.setForeground(party.isUnelected() ? UITheme.TEXT_SECONDARY : UITheme.TEXT_PRIMARY);

        // Seat bar
        JPanel barContainer = new JPanel(new BorderLayout(4, 0));
        barContainer.setBackground(row.getBackground());

        final int seats = party.getSeats();
        final int mx    = maxSeats;
        JPanel miniBar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(30, 22, 50));
                g.fillRoundRect(0, 2, getWidth(), getHeight() - 4, 3, 3);
                if (seats > 0 && mx > 0) {
                    g.setColor(party.isUnelected() ? new Color(80, 80, 100) : new Color(100, 75, 30));
                    g.fillRoundRect(0, 2, (int)((double) seats / mx * getWidth()), getHeight() - 4, 3, 3);
                }
            }
        };
        miniBar.setPreferredSize(new Dimension(0, 14));
        miniBar.setBackground(row.getBackground());

        JLabel seatsLabel = new JLabel(party.getSeats() + " seats");
        seatsLabel.setFont(UITheme.FONT_BUTTON);
        seatsLabel.setForeground(UITheme.TEXT_GOLD);
        seatsLabel.setPreferredSize(new Dimension(60, 20));
        seatsLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        barContainer.add(miniBar,   BorderLayout.CENTER);
        barContainer.add(seatsLabel, BorderLayout.EAST);

        row.add(nameLabel,    BorderLayout.WEST);
        row.add(barContainer, BorderLayout.CENTER);
        return row;
    }

    private JPanel buildPlayerRow() {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(new Color(20, 28, 46));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.ACCENT_FROST, 1),
                new EmptyBorder(8, 12, 8, 12)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        row.setAlignmentX(LEFT_ALIGNMENT);

        JLabel nameLabel = new JLabel("◆  You (Supervisor)  —  1 seat + casting vote");
        nameLabel.setFont(UITheme.FONT_BUTTON);
        nameLabel.setForeground(UITheme.ACCENT_FROST);

        JLabel prestige = new JLabel("Prestige: " + gameState.getPlayerPrestige().getPrestige()
                + "   Trust: " + gameState.getPlayerPrestige().getTrust() + "/10");
        prestige.setFont(UITheme.FONT_SMALL);
        prestige.setForeground(UITheme.TEXT_SECONDARY);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(new Color(20, 28, 46));
        left.add(nameLabel);
        left.add(prestige);

        row.add(left, BorderLayout.WEST);
        return row;
    }
}