package City.ui.politics;

import City.main.politics.PolitcalView;
import City.main.politics.PoliticalParty;
import City.main.politics.ViewStrength;
import City.main.core.GameState;
import City.main.politics.NoblePartyVoteManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.Map;
import City.ui.UITheme;

/**
 * Shows all parties, their seats, opinions, and political views.
 * Accessed via the PARTIES button in the main window.
 */
public class PartiesOverviewPanel extends JPanel {

    private final GameState  gameState;
    private final Runnable   onBack;
    private final JPanel     listPanel;
    // Survey panel shown during campaign
    private JPanel surveyPanel = null;

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
        scroll.getVerticalScrollBar().setUnitIncrement(32);
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

        // Election countdown banner
        City.main.politics.ElectionManager em = gameState.getElectionManager();
        int elTurns = em.getTurnsUntilElection();
        JLabel elBanner = new JLabel("⚑ Next election in " + elTurns + " turn(s)"
                + (elTurns <= 2 ? " — IMMINENT" : ""));
        elBanner.setFont(UITheme.FONT_HEADER);
        elBanner.setForeground(elTurns <= 2
                ? new Color(220, 190, 80) : new Color(160, 140, 200));
        elBanner.setBorder(new EmptyBorder(0, 0, 10, 0));
        listPanel.add(elBanner);

        // Election survey during campaign period
        if (em.isCampaignPeriod()) {
            listPanel.add(buildSurveyPanel());
            listPanel.add(Box.createVerticalStrut(8));
        }

        List<PoliticalParty> parties = gameState.getPartyManager().getParties();

        // Election support button (during campaign)
        if (em.canSupportParty()) {
            JPanel supportBanner = buildSupportBanner(parties, em);
            listPanel.add(supportBanner);
            listPanel.add(Box.createVerticalStrut(10));
        } else if (em.getSupportedPartyName() != null) {
            JLabel supportLabel = new JLabel(
                    "⚑ You are backing: " + em.getSupportedPartyName());
            supportLabel.setFont(UITheme.FONT_BODY);
            supportLabel.setForeground(new Color(240, 200, 80));
            supportLabel.setBorder(new javax.swing.border.EmptyBorder(0, 0, 10, 0));
            listPanel.add(supportLabel);
        }

        // Filter: only parties with seats OR fixed-seat parties
        for (PoliticalParty party : parties) {
            if (party.getSeats() == 0 && !party.isUnelected()) continue;
            listPanel.add(buildPartyCard(party));
            listPanel.add(Box.createVerticalStrut(8));
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

private JPanel buildPartyCard(PoliticalParty party) {
        City.main.politics.PropagandaManager pm = gameState.getPropagandaManager();
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBackground(UITheme.BG_PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
            new EmptyBorder(10, 12, 10, 12)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                showPartyDetailDialog(party);
            }
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                card.setBackground(UITheme.BG_PANEL_LIGHT);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                card.setBackground(UITheme.BG_PANEL);
            }
        });

        // Emblem + name block
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(UITheme.BG_PANEL);
        left.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                showPartyDetailDialog(party);
            }
        });

        String emblem = City.main.politics.PartyEmblemRegistry.getEmblem(party.getName());
        JLabel nameLabel = new JLabel(emblem + "  " + party.getName());
        nameLabel.setFont(UITheme.FONT_HEADER);
        nameLabel.setForeground(UITheme.TEXT_GOLD);

        JLabel seatsLabel = new JLabel(party.getSeats() + " seats  |  Power: " + party.getPower());
        seatsLabel.setFont(UITheme.FONT_SMALL);
        seatsLabel.setForeground(UITheme.TEXT_SECONDARY);

        int favour = party.getFavour();
        String favourText = favour == 0 ? ""
            : favour < 0 ? "  ⚠ You owe " + Math.abs(favour) + " favour(s)"
            : "  ✓ They owe " + favour + " favour(s)";
        JLabel favourLabel = new JLabel(favourText);
        favourLabel.setFont(UITheme.FONT_SMALL);
        favourLabel.setForeground(favour < 0 ? UITheme.TEXT_RED : UITheme.TEXT_GREEN);

        JLabel clickHint = new JLabel("Click for details");
        clickHint.setFont(UITheme.FONT_SMALL);
        clickHint.setForeground(new Color(100, 85, 130));

        left.add(nameLabel);
        left.add(seatsLabel);
        if (!favourText.isEmpty()) left.add(favourLabel);
        left.add(clickHint);

        // Right: opinion bars
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBackground(UITheme.BG_PANEL);
        right.setPreferredSize(new Dimension(130, 0));
        right.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                showPartyDetailDialog(party);
            }
        });
        right.add(makeStatLabelSmall("Opinion of you", party.getPlayerOpinion() + "/100",
                opinionColor(party.getPlayerOpinion())));
        right.add(Box.createVerticalStrut(4));
        right.add(makeStatLabelSmall("Public opinion", party.getPublicOpinion() + "/100",
                opinionColor(party.getPublicOpinion())));

        card.add(left,  BorderLayout.CENTER);
        card.add(right, BorderLayout.EAST);

        // Campaign resource row
        City.main.politics.ElectionManager em = gameState.getElectionManager();
        if (!party.isUnelected() && (em.isCampaignPeriod() || em.getTurnsUntilElection() <= 4)) {
            card.add(buildCampaignRow(party, pm), BorderLayout.SOUTH);
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        }
        return card;
    }

private JPanel buildSupportBanner(java.util.List<PoliticalParty> parties,
                                   City.main.politics.ElectionManager em) {
    JPanel banner = new JPanel(new BorderLayout(12, 0));
    banner.setBackground(new Color(55, 42, 10));
    banner.setBorder(javax.swing.BorderFactory.createCompoundBorder(
            javax.swing.BorderFactory.createLineBorder(new Color(180, 140, 40), 1),
            new javax.swing.border.EmptyBorder(10, 14, 10, 14)));
    banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
    banner.setAlignmentX(LEFT_ALIGNMENT);

    JLabel text = new JLabel("<html><b>Election Campaign</b> — Declare your support for one party.<br>"
            + "Your prestige grants them a bonus, but if they lose 2+ seats you suffer -20 prestige.</html>");
    text.setFont(UITheme.FONT_SMALL);
    text.setForeground(new Color(220, 190, 140));

    JComboBox<String> partyBox = new JComboBox<>(
            parties.stream().filter(p -> !p.getName().equals("Oracles"))
                    .map(PoliticalParty::getName).toArray(String[]::new));
    partyBox.setFont(UITheme.FONT_SMALL);
    partyBox.setBackground(UITheme.BG_PANEL);
    partyBox.setForeground(UITheme.TEXT_GOLD);

    JButton supportBtn = new JButton("DECLARE SUPPORT");
    supportBtn.setFont(UITheme.FONT_BUTTON);
    supportBtn.setForeground(new Color(240, 200, 80));
    supportBtn.setBackground(new Color(70, 50, 10));
    supportBtn.setBorderPainted(false);
    supportBtn.setFocusPainted(false);
    supportBtn.addActionListener(e -> {
        String chosen = (String) partyBox.getSelectedItem();
        if (chosen == null) return;
        for (PoliticalParty p : parties) {
            if (p.getName().equals(chosen)) {
                int prestige = gameState.getPlayerPrestige().getPrestige();
                boolean ok   = em.declareSupport(p, prestige);
                if (ok) {
                    JOptionPane.showMessageDialog(this,
                            "You declare support for " + p.getName() + ".\n"
                            + "They receive a prestige bonus based on your standing.",
                            "Support Declared", JOptionPane.INFORMATION_MESSAGE);
                    refresh();
                }
                break;
            }
        }
    });

    JPanel right = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 6, 0));
    right.setBackground(new Color(55, 42, 10));
    right.add(partyBox);
    right.add(supportBtn);

    banner.add(text,  BorderLayout.CENTER);
    banner.add(right, BorderLayout.EAST);
    return banner;
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

        JTextArea personality = new JTextArea(party.getPersonality());
        personality.setFont(new java.awt.Font("Serif", java.awt.Font.ITALIC, 12));
        personality.setForeground(UITheme.TEXT_SECONDARY);
        personality.setBackground(UITheme.BG_PANEL);
        personality.setEditable(false);
        personality.setLineWrap(true);
        personality.setWrapStyleWord(true);
        personality.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 40));

        panel.add(name);
        panel.add(Box.createVerticalStrut(2));
        panel.add(leader);
        panel.add(Box.createVerticalStrut(2));
        panel.add(seats);
        panel.add(Box.createVerticalStrut(2));
        panel.add(favourLabel);
        panel.add(Box.createVerticalStrut(2));

        if (!party.isUnelected()) {
            // Propaganda display — only for elected parties
            City.main.politics.PropagandaManager pm = gameState.getPropagandaManager();
            double electionProp = pm.getElectionPropaganda(party);
            JLabel propLabel = new JLabel(String.format("Propaganda (election): %.1f", electionProp));
            propLabel.setFont(UITheme.FONT_SMALL);
            propLabel.setForeground(new Color(180, 150, 220));
            propLabel.setToolTipText("Propaganda banked for the next election. Higher = more vote bonus.");
            panel.add(propLabel);
            panel.add(Box.createVerticalStrut(2));
        } else if (party.getName().equals(NoblePartyVoteManager.NOBLE_PARTY_NAME)) {
            JLabel fixedNote = new JLabel("Internal vote — based on noble opinion of you");
            fixedNote.setFont(UITheme.FONT_SMALL);
            fixedNote.setForeground(new Color(200, 170, 100));
            panel.add(fixedNote);
            panel.add(Box.createVerticalStrut(2));
        }

        panel.add(Box.createVerticalStrut(2));
        panel.add(personality);
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

private JPanel buildCampaignRow(PoliticalParty party,
                                     City.main.politics.PropagandaManager pm) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row.setBackground(new Color(35, 26, 48));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_COLOR),
                new EmptyBorder(4, 6, 4, 6)));

        JLabel label = new JLabel("Donate:");
        label.setFont(UITheme.FONT_SMALL);
        label.setForeground(UITheme.TEXT_SECONDARY);
        row.add(label);

        // Gold donation
        JButton goldBtn = makeDonateButton("50g");
        goldBtn.setToolTipText("Donate 50 gold → propaganda for " + party.getName());
        goldBtn.setEnabled(gameState.getResources().getMoney() >= 50);
        goldBtn.addActionListener(e -> {
            if (gameState.getResources().getMoney() < 50) return;
            gameState.getResources().spendMoney(50);
            pm.addPropaganda(party, 50 * City.main.parameters.PoliticalParams.PROPAGANDA_PER_GOLD);
            refresh();
        });
        row.add(goldBtn);

        JButton gold200Btn = makeDonateButton("200g");
        gold200Btn.setToolTipText("Donate 200 gold → propaganda for " + party.getName());
        gold200Btn.setEnabled(gameState.getResources().getMoney() >= 200);
        gold200Btn.addActionListener(e -> {
            if (gameState.getResources().getMoney() < 200) return;
            gameState.getResources().spendMoney(200);
            pm.addPropaganda(party, 200 * City.main.parameters.PoliticalParams.PROPAGANDA_PER_GOLD);
            refresh();
        });
        row.add(gold200Btn);

        // Influence donation
        JButton infBtn = makeDonateButton("20 inf");
        infBtn.setToolTipText("Spend 20 influence → propaganda for " + party.getName());
        infBtn.setEnabled(gameState.getResources().getInfluence() >= 20);
        infBtn.addActionListener(e -> {
            if (gameState.getResources().getInfluence() < 20) return;
            gameState.getResources().spendInfluence(20);
            pm.addPropaganda(party, 20 * City.main.parameters.PoliticalParams.PROPAGANDA_PER_INFLUENCE);
            refresh();
        });
        row.add(infBtn);

        double electionProp = pm.getElectionPropaganda(party);
        JLabel propLbl = new JLabel(String.format("Banked: %.0f propaganda", electionProp));
        propLbl.setFont(UITheme.FONT_SMALL);
        propLbl.setForeground(new Color(160, 130, 210));
        row.add(propLbl);

        return row;
    }

private JPanel buildSurveyPanel() {
        City.main.politics.ElectionManager em = gameState.getElectionManager();
        List<City.main.politics.ElectionSurveyCalculator.SurveyResult> survey =
                em.getSurveyCalculator().compute(
                        gameState.getPartyManager().getParties(),
                        gameState.getPopManager(),
                        gameState.getPropagandaManager());

        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(new Color(22, 16, 36));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 70, 160), 1),
                new EmptyBorder(10, 12, 10, 12)));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        panel.setAlignmentX(LEFT_ALIGNMENT);

        JLabel title = new JLabel("📊 ELECTION SURVEY (rough estimate ±15%)");
        title.setFont(UITheme.FONT_BUTTON);
        title.setForeground(new Color(200, 170, 255));

        // Pie chart panel
        JPanel piePanel = new JPanel() {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = 60, cy = 60, r = 55;
                double start = -90;
                java.awt.Color[] pieColors = {
                    new java.awt.Color(180, 60, 60),
                    new java.awt.Color(60, 120, 200),
                    new java.awt.Color(60, 160, 80),
                    new java.awt.Color(180, 140, 40),
                    new java.awt.Color(120, 60, 180),
                    new java.awt.Color(40, 160, 160),
                    new java.awt.Color(200, 100, 40),
                    new java.awt.Color(140, 140, 60)
                };
                int colorIdx = 0;
                for (City.main.politics.ElectionSurveyCalculator.SurveyResult sr : survey) {
                    if (sr.votePct < 0.5) { colorIdx++; continue; }
                    double arc = sr.votePct / 100.0 * 360.0;
                    g2.setColor(pieColors[colorIdx % pieColors.length]);
                    g2.fillArc(cx - r, cy - r, r * 2, r * 2, (int) start, (int) arc);
                    g2.setColor(new java.awt.Color(10, 8, 16));
                    g2.setStroke(new java.awt.BasicStroke(1.5f));
                    g2.drawArc(cx - r, cy - r, r * 2, r * 2, (int) start, (int) arc);
                    start += arc;
                    colorIdx++;
                }
                g2.setStroke(new java.awt.BasicStroke(1f));
            }
        };
        piePanel.setPreferredSize(new Dimension(130, 130));
        piePanel.setBackground(new java.awt.Color(22, 16, 36));

        // Legend
        JPanel legend = new JPanel();
        legend.setLayout(new BoxLayout(legend, BoxLayout.Y_AXIS));
        legend.setBackground(new java.awt.Color(22, 16, 36));

        java.awt.Color[] pieColors = {
            new java.awt.Color(180, 60, 60),
            new java.awt.Color(60, 120, 200),
            new java.awt.Color(60, 160, 80),
            new java.awt.Color(180, 140, 40),
            new java.awt.Color(120, 60, 180),
            new java.awt.Color(40, 160, 160),
            new java.awt.Color(200, 100, 40),
            new java.awt.Color(140, 140, 60)
        };
        int cIdx = 0;
        for (City.main.politics.ElectionSurveyCalculator.SurveyResult sr : survey) {
            JPanel row = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 1));
            row.setBackground(new java.awt.Color(22, 16, 36));
            JLabel dot = new JLabel("■");
            dot.setForeground(pieColors[cIdx % pieColors.length]);
            dot.setFont(UITheme.FONT_SMALL);
            String emblem = City.main.politics.PartyEmblemRegistry.getEmblem(sr.partyName);
            JLabel lbl = new JLabel(emblem + " " + sr.partyName + ": " + String.format("%.1f%%", sr.votePct));
            lbl.setFont(UITheme.FONT_SMALL);
            lbl.setForeground(UITheme.TEXT_PRIMARY);
            row.add(dot);
            row.add(lbl);
            legend.add(row);
            cIdx++;
        }

        JPanel chartRow = new JPanel(new BorderLayout(8, 0));
        chartRow.setBackground(new java.awt.Color(22, 16, 36));
        chartRow.add(piePanel, BorderLayout.WEST);
        chartRow.add(legend,   BorderLayout.CENTER);

        panel.add(title,    BorderLayout.NORTH);
        panel.add(chartRow, BorderLayout.CENTER);
        return panel;
    }

private JPanel makeStatLabelSmall(String key, String value, Color valueColor) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setBackground(UITheme.BG_PANEL);
        row.setAlignmentX(LEFT_ALIGNMENT);
        JLabel k = new JLabel(key);
        k.setFont(UITheme.FONT_SMALL);
        k.setForeground(UITheme.TEXT_SECONDARY);
        JLabel v = new JLabel(value);
        v.setFont(UITheme.FONT_BODY);
        v.setForeground(valueColor);
        row.add(k);
        row.add(v);
        return row;
    }

private void showPartyDetailDialog(PoliticalParty party) {
        JDialog dialog = new JDialog(
                (java.awt.Frame) SwingUtilities.getWindowAncestor(this),
                City.main.politics.PartyEmblemRegistry.getEmblem(party.getName()) + " " + party.getName(),
                true);
        dialog.setSize(520, 540);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(true);
        dialog.getContentPane().setBackground(UITheme.BG_PANEL);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(UITheme.BG_PANEL);
        content.setBorder(new EmptyBorder(16, 16, 16, 16));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.weightx = 1.0; gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new java.awt.Insets(3, 0, 3, 0);

        int row = 0;
        String emblem = City.main.politics.PartyEmblemRegistry.getEmblem(party.getName());

        gc.gridy = row++;
        JLabel nameLabel = new JLabel(emblem + "  " + party.getName());
        nameLabel.setFont(UITheme.FONT_TITLE);
        nameLabel.setForeground(UITheme.TEXT_GOLD);
        content.add(nameLabel, gc);

        gc.gridy = row++;
        JLabel leaderLabel = new JLabel("Leader: " + party.getLeaderName());
        leaderLabel.setFont(UITheme.FONT_BUTTON);
        leaderLabel.setForeground(UITheme.TEXT_SECONDARY);
        content.add(leaderLabel, gc);

        gc.gridy = row++;
        JTextArea personality = new JTextArea(party.getPersonality());
        personality.setFont(new java.awt.Font("Serif", java.awt.Font.ITALIC, 13));
        personality.setForeground(UITheme.TEXT_PRIMARY);
        personality.setBackground(UITheme.BG_PANEL);
        personality.setEditable(false);
        personality.setLineWrap(true);
        personality.setWrapStyleWord(true);
        personality.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
                new EmptyBorder(6, 8, 6, 8)));
        content.add(personality, gc);

        gc.gridy = row++;
        JSeparator sep = new JSeparator();
        sep.setForeground(UITheme.BORDER_COLOR);
        content.add(sep, gc);

        // Stats
        gc.gridy = row++;
        content.add(infoRow("Seats", String.valueOf(party.getSeats())), gc);
        gc.gridy = row++;
        content.add(infoRow("Power", party.getPower() + "/100"), gc);
        gc.gridy = row++;
        content.add(infoRow("Opinion of you", party.getPlayerOpinion() + "/100",
                opinionColor(party.getPlayerOpinion())), gc);
        gc.gridy = row++;
        content.add(infoRow("Public opinion", party.getPublicOpinion() + "/100",
                opinionColor(party.getPublicOpinion())), gc);

        int favour = party.getFavour();
        if (favour != 0) {
            gc.gridy = row++;
            content.add(infoRow("Favours",
                    favour < 0 ? "You owe " + Math.abs(favour) : "They owe " + favour,
                    favour < 0 ? UITheme.TEXT_RED : UITheme.TEXT_GREEN), gc);
        }

        gc.gridy = row++;
        JSeparator sep2 = new JSeparator();
        sep2.setForeground(UITheme.BORDER_COLOR);
        content.add(sep2, gc);

        // Views
        gc.gridy = row++;
        JLabel viewsHeader = new JLabel("Political Views:");
        viewsHeader.setFont(UITheme.FONT_BUTTON);
        viewsHeader.setForeground(UITheme.TEXT_GOLD);
        content.add(viewsHeader, gc);

        for (Map.Entry<City.main.politics.PolitcalView, City.main.politics.ViewStrength> entry
                : party.getViews().entrySet()) {
            if (entry.getValue() == City.main.politics.ViewStrength.NEUTRAL) continue;
            gc.gridy = row++;
            JLabel viewLabel = new JLabel("  " + entry.getKey().getDisplayName()
                    + ": " + entry.getValue().name().replace("_", " "));
            viewLabel.setFont(UITheme.FONT_SMALL);
            viewLabel.setForeground(viewColor(entry.getValue()));
            content.add(viewLabel, gc);
        }

        // Side leaders
        if (!party.getSideLeaders().isEmpty()) {
            gc.gridy = row++;
            JSeparator sep3 = new JSeparator();
            sep3.setForeground(UITheme.BORDER_COLOR);
            content.add(sep3, gc);

            gc.gridy = row++;
            JLabel slHeader = new JLabel("Other Prominent Members:");
            slHeader.setFont(UITheme.FONT_BUTTON);
            slHeader.setForeground(UITheme.TEXT_GOLD);
            content.add(slHeader, gc);

            for (City.main.politics.SideLeader sl : party.getSideLeaders()) {
                gc.gridy = row++;
                JLabel slLabel = new JLabel("  " + sl.getName() + " — " + sl.getPersonality());
                slLabel.setFont(UITheme.FONT_SMALL);
                slLabel.setForeground(UITheme.TEXT_SECONDARY);
                content.add(slLabel, gc);
            }
        }

        gc.gridy = row;
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        content.add(Box.createVerticalGlue(), gc);

        JButton closeBtn = new JButton("CLOSE");
        closeBtn.setFont(UITheme.FONT_BUTTON);
        closeBtn.setForeground(UITheme.TEXT_SECONDARY);
        closeBtn.setBackground(UITheme.BUTTON_BG);
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.addActionListener(e -> dialog.dispose());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setBackground(UITheme.BG_PANEL);
        scroll.getViewport().setBackground(UITheme.BG_PANEL);

        dialog.setLayout(new BorderLayout());
        dialog.add(scroll,    BorderLayout.CENTER);
        dialog.add(closeBtn,  BorderLayout.SOUTH);
        dialog.getContentPane().setBackground(UITheme.BG_PANEL);
        dialog.setVisible(true);
    }

private JPanel infoRow(String key, String value) {
        return infoRow(key, value, UITheme.TEXT_PRIMARY);
    }

    private JPanel infoRow(String key, String value, Color valueColor) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(UITheme.BG_PANEL);
        JLabel k = new JLabel(key + ":");
        k.setFont(UITheme.FONT_SMALL);
        k.setForeground(UITheme.TEXT_SECONDARY);
        JLabel v = new JLabel(value);
        v.setFont(UITheme.FONT_BODY);
        v.setForeground(valueColor);
        row.add(k, BorderLayout.WEST);
        row.add(v, BorderLayout.EAST);
        return row;
    }

private JButton makeDonateButton(String label) {
        JButton btn = new JButton(label);
        btn.setFont(UITheme.FONT_SMALL);
        btn.setForeground(new Color(210, 170, 80));
        btn.setBackground(new Color(55, 40, 10));
        btn.setBorderPainted(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(120, 90, 30), 1),
                new EmptyBorder(3, 8, 3, 8)));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
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