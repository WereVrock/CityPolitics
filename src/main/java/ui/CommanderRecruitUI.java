package ui;

import debug.Debug;
import main.army.commander.Commander;
import main.army.commander.CommanderRecruitPool;
import main.army.commander.CommanderRoster;
import main.parameters.GameParameters;
import main.resources.ResourcePool;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Recruitment screen for commanders.
 * Shows up to 6 candidates (3 base + 3 after paid refresh, once per turn).
 * Each card shows name, race, skill, affiliation, upkeep, and recruit cost.
 */
public class CommanderRecruitUI extends JPanel {

    private final CommanderRoster      roster;
    private final CommanderRecruitPool pool;
    private final ResourcePool         resources;
    private final Runnable             onClose;

    private JPanel candidatePanel;
    private JLabel influenceLabel;
    private JButton refreshBtn;

    public CommanderRecruitUI(CommanderRoster roster,
                               CommanderRecruitPool pool,
                               ResourcePool resources,
                               Runnable onClose) {
        this.roster    = roster;
        this.pool      = pool;
        this.resources = resources;
        this.onClose   = onClose;
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(12, 12, 12, 12));
        setBackground(UITheme.BG_DARK);
        build();
    }

private void build() {
        removeAll();

        // ── Top bar ──────────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(UITheme.BG_PANEL);
        topBar.setBorder(new EmptyBorder(6, 10, 6, 10));

        JLabel title = new JLabel("Commander Recruitment");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_GOLD);

        influenceLabel = new JLabel("Influence: " + resources.getInfluence());
        influenceLabel.setFont(UITheme.FONT_BUTTON);
        influenceLabel.setForeground(UITheme.TEXT_SECONDARY);

        JButton closeBtn = new JButton("✕ CLOSE");
        closeBtn.setFont(UITheme.FONT_BUTTON);
        closeBtn.setForeground(UITheme.TEXT_SECONDARY);
        closeBtn.setBackground(UITheme.BUTTON_BG);
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> onClose.run());

        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        topRight.setBackground(UITheme.BG_PANEL);
        topRight.add(influenceLabel);
        topRight.add(closeBtn);

        topBar.add(title,    BorderLayout.WEST);
        topBar.add(topRight, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // ── Candidate cards ───────────────────────────────────────────────────
        candidatePanel = new JPanel(new GridLayout(0, 3, 8, 8));
        candidatePanel.setBackground(UITheme.BG_DARK);
        refreshCandidateCards();
        add(new JScrollPane(candidatePanel), BorderLayout.CENTER);

        // ── Bottom: refresh pool button ───────────────────────────────────────
        boolean alreadyRefreshed = pool.isRefreshUsedThisTurn();
        refreshBtn = new JButton(
                alreadyRefreshed
                ? "Extra Candidates Already Revealed (once per turn)"
                : "Reveal 3 More Candidates — " + GameParameters.COMMANDER_POOL_REFRESH_COST + " Influence");
        refreshBtn.setFont(UITheme.FONT_BUTTON);
        refreshBtn.setEnabled(!alreadyRefreshed);
        refreshBtn.setForeground(alreadyRefreshed ? UITheme.TEXT_SECONDARY : UITheme.TEXT_GOLD);
        refreshBtn.setBackground(alreadyRefreshed ? UITheme.BUTTON_DISABLED : new Color(60, 40, 20));
        refreshBtn.setBorderPainted(false);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> {
            boolean ok = pool.refreshPool();
            if (!ok) {
                String msg = pool.isRefreshUsedThisTurn()
                        ? "Already revealed extra candidates this turn."
                        : "Not enough influence.";
                JOptionPane.showMessageDialog(this, msg);
                Debug.log("recruit-ui", "refresh-fail", msg);
            } else {
                Debug.log("recruit-ui", "refresh-ok",
                        "Extra candidates revealed. Pool size=" + pool.getCandidates().size());
                influenceLabel.setText("Influence: " + resources.getInfluence());
                build();
            }
        });

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomBar.setBackground(UITheme.BG_DARK);
        bottomBar.add(refreshBtn);
        add(bottomBar, BorderLayout.SOUTH);

        revalidate();
        repaint();
    }

private void refreshCandidateCards() {
        candidatePanel.removeAll();
        for (Commander c : pool.getCandidates()) {
            candidatePanel.add(buildCandidateCard(c));
        }
        candidatePanel.revalidate();
        candidatePanel.repaint();
    }

private JPanel buildCandidateCard(Commander c) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(UITheme.BG_PANEL_LIGHT);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR),
                new EmptyBorder(8, 8, 8, 8)));

        JLabel nameLabel = new JLabel(c.getName());
        nameLabel.setFont(UITheme.FONT_BUTTON);
        nameLabel.setForeground(UITheme.TEXT_PRIMARY);

        JLabel raceLabel = new JLabel("Race: " + c.getRace());
        raceLabel.setFont(UITheme.FONT_SMALL);
        raceLabel.setForeground(UITheme.TEXT_SECONDARY);

        JLabel skillLabel = new JLabel("Skill: " + c.getCommandingSkill());
        skillLabel.setFont(UITheme.FONT_SMALL);
        skillLabel.setForeground(UITheme.TEXT_SECONDARY);

        JLabel partyLabel = new JLabel("Party: " + c.getPartyName());
        partyLabel.setFont(UITheme.FONT_SMALL);
        partyLabel.setForeground(UITheme.TEXT_SECONDARY);

        JLabel upkeepLabel = new JLabel(
                String.format("Upkeep: %.1f gold/turn", c.getUpkeepCost()));
        upkeepLabel.setFont(UITheme.FONT_SMALL);
        upkeepLabel.setForeground(UITheme.TEXT_SECONDARY);

        card.add(nameLabel);
        card.add(Box.createVerticalStrut(3));
        card.add(raceLabel);
        card.add(skillLabel);
        card.add(partyLabel);
        card.add(upkeepLabel);
        card.add(Box.createVerticalStrut(6));

        JButton recruitBtn = new JButton(
                "Recruit — " + GameParameters.COMMANDER_RECRUIT_BASE_COST + " Influence");
        recruitBtn.setFont(UITheme.FONT_SMALL);
        recruitBtn.setForeground(UITheme.TEXT_GOLD);
        recruitBtn.setBackground(new Color(40, 60, 20));
        recruitBtn.setBorderPainted(false);
        recruitBtn.setFocusPainted(false);
        recruitBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        recruitBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        recruitBtn.addActionListener(e -> {
            boolean ok = roster.recruit(c);
            if (!ok) {
                JOptionPane.showMessageDialog(this, "Not enough influence.");
                Debug.log("recruit-ui", "recruit-fail",
                        c.getName() + " — not enough influence");
            } else {
                pool.removeCandidate(c);
                Debug.log("recruit-ui", "recruited", c.getName() + " recruited successfully");
                influenceLabel.setText("Influence: " + resources.getInfluence());
                // Notify parent that resources changed, then rebuild
                onClose.run();
            }
        });
        card.add(recruitBtn);
        return card;
    }

}