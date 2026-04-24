package ui;

import main.politics.PolitcalView;
import main.politics.PoliticalParty;
import main.politics.ViewStrength;
import main.core.GameState;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Shows all parties, their seats, opinions, and political views.
 * Accessed via the PARTIES button in the main window.
 */
public class PartiesOverviewPanel extends JPanel {

    private final GameState  gameState;
    private final Runnable   onBack;
    private final JPanel     listPanel;

    public PartiesOverviewPanel(GameState gameState, Runnable onBack) {
        this.gameState = gameState;
        this.onBack    = onBack;

        setBackground(UITheme.BG_DARK);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12, 12, 12, 12));

        add(buildHeader(),    BorderLayout.NORTH);
        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(UITheme.BG_DARK);

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.setBackground(UITheme.BG_DARK);
        scroll.getViewport().setBackground(UITheme.BG_DARK);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UITheme.BG_DARK);
        panel.setBorder(new EmptyBorder(0, 0, 12, 0));

        JLabel title = new JLabel("ASSEMBLY PARTIES");
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

        panel.add(title, BorderLayout.WEST);
        panel.add(back,  BorderLayout.EAST);
        return panel;
    }

    public void refresh() {
        listPanel.removeAll();
        List<PoliticalParty> parties = gameState.getPartyManager().getParties();
        for (PoliticalParty party : parties) {
            listPanel.add(buildPartyCard(party));
            listPanel.add(Box.createVerticalStrut(8));
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    private JPanel buildPartyCard(PoliticalParty party) {
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBackground(UITheme.BG_PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
            new EmptyBorder(10, 12, 10, 12)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        card.setAlignmentX(LEFT_ALIGNMENT);

        card.add(buildPortraitPlaceholder(party), BorderLayout.WEST);
        card.add(buildPartyInfo(party),           BorderLayout.CENTER);
        card.add(buildOpinionPanel(party),        BorderLayout.EAST);
        return card;
    }

    private JPanel buildPortraitPlaceholder(PoliticalParty party) {
        JPanel portrait = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.BG_PANEL_LIGHT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(UITheme.BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                // head
                g2.setColor(UITheme.TEXT_SECONDARY);
                int cx = getWidth() / 2;
                g2.fillOval(cx - 14, 10, 28, 28);
                // body
                g2.fillRoundRect(cx - 18, 42, 36, 30, 6, 6);
                // initials
                g2.setColor(UITheme.BG_PANEL);
                g2.setFont(UITheme.FONT_SMALL);
                String init = party.getName().substring(0, 1);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(init, cx - fm.stringWidth(init)/2, 30);
            }
        };
        portrait.setPreferredSize(new Dimension(70, 90));
        portrait.setBackground(UITheme.BG_PANEL);
        return portrait;
    }

    private JPanel buildPartyInfo(PoliticalParty party) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UITheme.BG_PANEL);

        JLabel name = new JLabel(party.getName());
        name.setFont(UITheme.FONT_HEADER);
        name.setForeground(UITheme.TEXT_GOLD);

        JLabel leader = new JLabel(party.getLeaderName());
        leader.setFont(UITheme.FONT_SMALL);
        leader.setForeground(UITheme.TEXT_SECONDARY);

        JLabel seats = new JLabel(party.getSeats() + " seats");
        seats.setFont(UITheme.FONT_SMALL);
        seats.setForeground(UITheme.TEXT_PRIMARY);

        int favour = party.getFavour();
        String favourText = favour == 0 ? "No favours owed"
            : favour < 0 ? "You owe " + Math.abs(favour) + " favour(s)"
            : "They owe " + favour + " favour(s)";
        JLabel favourLabel = new JLabel(favourText);
        favourLabel.setFont(UITheme.FONT_SMALL);
        favourLabel.setForeground(favour < 0 ? UITheme.TEXT_RED : UITheme.TEXT_SECONDARY);

        panel.add(name);
        panel.add(Box.createVerticalStrut(2));
        panel.add(leader);
        panel.add(Box.createVerticalStrut(4));
        panel.add(seats);
        panel.add(Box.createVerticalStrut(2));
        panel.add(favourLabel);
        panel.add(Box.createVerticalStrut(6));

        for (Map.Entry<PolitcalView, ViewStrength> entry : party.getViews().entrySet()) {
            JLabel view = new JLabel("  " + entry.getKey().getDisplayName()
                + ": " + entry.getValue().name().replace("_", " "));
            view.setFont(UITheme.FONT_SMALL);
            view.setForeground(viewColor(entry.getValue()));
            panel.add(view);
        }
        return panel;
    }

    private JPanel buildOpinionPanel(PoliticalParty party) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UITheme.BG_PANEL);
        panel.setPreferredSize(new Dimension(110, 0));

        JLabel playerOp = new JLabel("Opinion of you");
        playerOp.setFont(UITheme.FONT_SMALL);
        playerOp.setForeground(UITheme.TEXT_SECONDARY);

        JLabel playerVal = new JLabel(party.getPlayerOpinion() + " / 100");
        playerVal.setFont(UITheme.FONT_BODY);
        playerVal.setForeground(opinionColor(party.getPlayerOpinion()));

        JLabel publicOp = new JLabel("Public opinion");
        publicOp.setFont(UITheme.FONT_SMALL);
        publicOp.setForeground(UITheme.TEXT_SECONDARY);

        JLabel publicVal = new JLabel(party.getPublicOpinion() + " / 100");
        publicVal.setFont(UITheme.FONT_BODY);
        publicVal.setForeground(opinionColor(party.getPublicOpinion()));

        panel.add(playerOp);
        panel.add(playerVal);
        panel.add(Box.createVerticalStrut(8));
        panel.add(publicOp);
        panel.add(publicVal);
        return panel;
    }

    private Color viewColor(ViewStrength s) {
        return switch (s) {
            case STRONGLY_FOR     -> UITheme.TEXT_GREEN;
            case FOR              -> new Color(120, 200, 140);
            case NEUTRAL          -> UITheme.TEXT_SECONDARY;
            case AGAINST          -> new Color(200, 120, 100);
            case STRONGLY_AGAINST -> UITheme.TEXT_RED;
        };
    }

    private Color opinionColor(int v) {
        if (v >= 70) return UITheme.TEXT_GREEN;
        if (v <= 30) return UITheme.TEXT_RED;
        return UITheme.TEXT_PRIMARY;
    }
}