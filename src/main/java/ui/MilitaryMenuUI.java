package ui;

import debug.Debug;
import main.army.*;
import main.parameters.GameParameters;
import main.resources.ResourcePool;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Top-level military panel, accessed from the Actions menu.
 *
 * Left column  — army list with per-army controls (assign commander, recruit soldiers).
 * Right column — commander roster with dismiss button.
 * Bottom       — open recruitment screen button.
 * Top-right    — BACK / close button.
 */
public class MilitaryMenuUI extends JPanel {

    private final ArmyManager          armyManager;
    private final CommanderRoster      roster;
    private final CommanderRecruitPool pool;
    private final ResourcePool         resources;
    private final Runnable             onBack;

    private JPanel armyListPanel;
    private JPanel rosterPanel;

    // ─── Constructor ─────────────────────────────────────────────────────────

    public MilitaryMenuUI(ArmyManager armyManager,
                          CommanderRoster roster,
                          CommanderRecruitPool pool,
                          ResourcePool resources,
                          Runnable onBack) {
        this.armyManager = armyManager;
        this.roster      = roster;
        this.pool        = pool;
        this.resources   = resources;
        this.onBack      = onBack;
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(UITheme.BG_DARK);
        build();
    }

    // ─── Build ───────────────────────────────────────────────────────────────

    private void build() {
        removeAll();

        // ── Top bar ──────────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(UITheme.BG_PANEL);
        topBar.setBorder(new EmptyBorder(6, 10, 6, 10));

        JLabel title = new JLabel("MILITARY");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_GOLD);

        JButton backBtn = new JButton("◀ BACK");
        backBtn.setFont(UITheme.FONT_BUTTON);
        backBtn.setForeground(UITheme.TEXT_SECONDARY);
        backBtn.setBackground(UITheme.BUTTON_BG);
        backBtn.setBorderPainted(false);
        backBtn.setFocusPainted(false);
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> onBack.run());

        topBar.add(title,   BorderLayout.WEST);
        topBar.add(backBtn, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // ── Centre: two columns ───────────────────────────────────────────────
        JPanel centre = new JPanel(new GridLayout(1, 2, 12, 0));
        centre.setBackground(UITheme.BG_DARK);

        armyListPanel = buildArmyListPanel();
        rosterPanel   = buildRosterPanel();

        centre.add(new JScrollPane(armyListPanel));
        centre.add(new JScrollPane(rosterPanel));
        add(centre, BorderLayout.CENTER);

        // ── Bottom: open recruitment ──────────────────────────────────────────
        JButton recruitBtn = new JButton("Open Commander Recruitment");
        recruitBtn.setFont(UITheme.FONT_BUTTON);
        recruitBtn.setForeground(UITheme.TEXT_GOLD);
        recruitBtn.setBackground(new Color(60, 40, 20));
        recruitBtn.setBorderPainted(false);
        recruitBtn.setFocusPainted(false);
        recruitBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        recruitBtn.addActionListener(e -> openRecruitUI());
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomBar.setBackground(UITheme.BG_DARK);
        bottomBar.add(recruitBtn);
        add(bottomBar, BorderLayout.SOUTH);

        revalidate();
        repaint();
    }

    // ─── Army list panel ─────────────────────────────────────────────────────

    private JPanel buildArmyListPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UITheme.BG_DARK);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR),
                "Armies", 0, 0,
                UITheme.FONT_BUTTON, UITheme.TEXT_GOLD));

        for (Army army : armyManager.getArmies()) {
            panel.add(buildArmyCard(army));
            panel.add(Box.createVerticalStrut(6));
        }
        return panel;
    }

    private JPanel buildArmyCard(Army army) {
        JPanel card = new JPanel(new BorderLayout(6, 4));
        card.setBackground(UITheme.BG_PANEL_LIGHT);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR),
                new EmptyBorder(8, 8, 8, 8)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        // Info block
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBackground(UITheme.BG_PANEL_LIGHT);

        JLabel nameLabel = new JLabel(army.getDisplayName());
        nameLabel.setFont(UITheme.FONT_BUTTON);
        nameLabel.setForeground(UITheme.TEXT_PRIMARY);

        String locationStr = army.isInCity() ? "In Heartland" : "Deployed: " + army.getZoneId();
        JLabel locLabel = new JLabel(locationStr);
        locLabel.setFont(UITheme.FONT_SMALL);
        locLabel.setForeground(UITheme.TEXT_SECONDARY);

        JLabel sizeLabel = new JLabel("Soldiers: " + army.getSoldierCount());
        sizeLabel.setFont(UITheme.FONT_SMALL);
        sizeLabel.setForeground(UITheme.TEXT_SECONDARY);

        String cmdText = army.hasLivingCommander()
                ? "Cmd: " + army.getCommander().getName()
                  + " [Skill " + army.getCommandingSkill() + "]"
                : "No Commander (cannot deploy)";
        JLabel cmdLabel = new JLabel(cmdText);
        cmdLabel.setFont(UITheme.FONT_SMALL);
        cmdLabel.setForeground(army.hasLivingCommander()
                ? UITheme.TEXT_PRIMARY : Color.ORANGE);

        info.add(nameLabel);
        info.add(locLabel);
        info.add(sizeLabel);
        info.add(cmdLabel);
        card.add(info, BorderLayout.CENTER);

        // Button column
        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
        buttons.setBackground(UITheme.BG_PANEL_LIGHT);

        // Assign commander button
        JButton assignBtn = new JButton("Assign Cmd");
        styleSmallButton(assignBtn);
        assignBtn.setToolTipText("Assign a commander from your roster to this army");
        assignBtn.addActionListener(e -> openAssignCommanderDialog(army));
        buttons.add(assignBtn);
        buttons.add(Box.createVerticalStrut(4));

        // Recruit soldiers button
        JButton recruitSoldiersBtn = new JButton("Recruit");
        styleSmallButton(recruitSoldiersBtn);
        recruitSoldiersBtn.setToolTipText(
                "Recruit soldiers — " + GameParameters.SOLDIER_RECRUIT_GOLD_COST
                + " gold + " + GameParameters.SOLDIER_RECRUIT_MANPOWER_COST + " manpower each");
        recruitSoldiersBtn.addActionListener(e -> openRecruitSoldiersDialog(army));
        buttons.add(recruitSoldiersBtn);

        card.add(buttons, BorderLayout.EAST);
        return card;
    }

    // ─── Roster panel ────────────────────────────────────────────────────────

    private JPanel buildRosterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UITheme.BG_DARK);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR),
                "Commanders", 0, 0,
                UITheme.FONT_BUTTON, UITheme.TEXT_GOLD));

        List<main.army.Commander> commanders = roster.getAllCommanders();
        if (commanders.isEmpty()) {
            JLabel empty = new JLabel("No commanders recruited.");
            empty.setForeground(UITheme.TEXT_SECONDARY);
            empty.setFont(UITheme.FONT_SMALL);
            panel.add(empty);
        }

        for (main.army.Commander c : commanders) {
            panel.add(buildCommanderCard(c));
            panel.add(Box.createVerticalStrut(6));
        }
        return panel;
    }

    private JPanel buildCommanderCard(main.army.Commander c) {
        JPanel card = new JPanel(new BorderLayout(6, 4));
        card.setBackground(UITheme.BG_PANEL_LIGHT);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR),
                new EmptyBorder(8, 8, 8, 8)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBackground(UITheme.BG_PANEL_LIGHT);

        JLabel nameLabel = new JLabel(c.getName() + (c.isAlive() ? "" : "  ✝"));
        nameLabel.setFont(UITheme.FONT_BUTTON);
        nameLabel.setForeground(c.isAlive() ? UITheme.TEXT_PRIMARY : Color.GRAY);

        JLabel detailLabel = new JLabel(
                "Skill " + c.getCommandingSkill()
                + " | XP " + c.getXp()
                + (c.xpToNextLevel() > 0 ? " (+" + c.xpToNextLevel() + " to next)" : " [MAX]")
                + " | " + c.getPartyName());
        detailLabel.setFont(UITheme.FONT_SMALL);
        detailLabel.setForeground(UITheme.TEXT_SECONDARY);

        JLabel upkeepLabel = new JLabel(
                String.format("Upkeep: %.1f gold/turn", c.getUpkeepCost()));
        upkeepLabel.setFont(UITheme.FONT_SMALL);
        upkeepLabel.setForeground(UITheme.TEXT_SECONDARY);

        info.add(nameLabel);
        info.add(detailLabel);
        info.add(upkeepLabel);
        card.add(info, BorderLayout.CENTER);

        if (c.isAlive()) {
            JButton dismissBtn = new JButton("Dismiss");
            styleSmallButton(dismissBtn);
            dismissBtn.setForeground(new Color(200, 80, 80));
            dismissBtn.setToolTipText("Dismiss — costs " + GameParameters.COMMANDER_DISMISS_COST
                    + " influence, lowers party opinion");
            dismissBtn.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Dismiss " + c.getName() + "?\nCosts "
                        + GameParameters.COMMANDER_DISMISS_COST + " influence and lowers "
                        + c.getPartyName() + " party opinion by "
                        + GameParameters.COMMANDER_DISMISS_OPINION_LOSS + ".",
                        "Confirm Dismiss",
                        JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    boolean ok = roster.dismiss(c);
                    if (!ok) {
                        JOptionPane.showMessageDialog(this, "Not enough influence.");
                        Debug.log("military-ui", "dismiss-fail",
                                c.getName() + " — not enough influence");
                    } else {
                        Debug.log("military-ui", "dismiss-ok", c.getName() + " dismissed");
                        build();
                    }
                }
            });
            card.add(dismissBtn, BorderLayout.EAST);
        }

        return card;
    }

    // ─── Assign commander dialog ──────────────────────────────────────────────

    private void openAssignCommanderDialog(Army army) {
        List<main.army.Commander> available = roster.getAliveCommanders();

        if (available.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No living commanders available. Recruit one first.",
                    "Assign Commander", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Build display strings
        String[] options = new String[available.size() + 1];
        options[0] = "— None (remove commander) —";
        for (int i = 0; i < available.size(); i++) {
            main.army.Commander c = available.get(i);
            options[i + 1] = c.getName()
                    + " [Skill " + c.getCommandingSkill()
                    + " | " + c.getPartyName() + "]";
        }

        String current = army.hasLivingCommander()
                ? "Current: " + army.getCommander().getName()
                : "Current: None";

        int choice = JOptionPane.showOptionDialog(this,
                current + "\n\nSelect commander for " + army.getDisplayName() + ":",
                "Assign Commander",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        if (choice < 0) return; // cancelled

        if (choice == 0) {
            army.setCommander(null);
            Debug.log("military-ui", "assign-cmd",
                    army.getDisplayName() + " commander removed");
        } else {
            main.army.Commander chosen = available.get(choice - 1);
            army.setCommander(chosen);
            Debug.log("military-ui", "assign-cmd",
                    army.getDisplayName() + " assigned " + chosen.getName());
        }
        build();
    }

    // ─── Recruit soldiers dialog ──────────────────────────────────────────────

    private void openRecruitSoldiersDialog(Army army) {
        int maxByGold     = (int)(resources.getMoney()    / GameParameters.SOLDIER_RECRUIT_GOLD_COST);
        int maxByManpower = resources.getManpower()       / GameParameters.SOLDIER_RECRUIT_MANPOWER_COST;
        int maxRecruit    = Math.min(maxByGold, maxByManpower);

        if (maxRecruit <= 0) {
            JOptionPane.showMessageDialog(this,
                    "Cannot recruit — need at least "
                    + GameParameters.SOLDIER_RECRUIT_GOLD_COST + " gold and "
                    + GameParameters.SOLDIER_RECRUIT_MANPOWER_COST + " manpower per soldier.",
                    "Recruit Soldiers", JOptionPane.WARNING_MESSAGE);
            Debug.log("military-ui", "recruit-soldiers-fail",
                    army.getDisplayName() + " — no resources");
            return;
        }

        SpinnerNumberModel spinnerModel =
                new SpinnerNumberModel(1, 1, maxRecruit, 1);
        JSpinner spinner = new JSpinner(spinnerModel);

        JLabel costLabel = new JLabel(buildCostString(1));
        spinner.addChangeListener(e ->
                costLabel.setText(buildCostString((int) spinner.getValue())));

        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        panel.add(new JLabel("Soldiers to recruit (max " + maxRecruit + "):"));
        panel.add(spinner);
        panel.add(costLabel);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Recruit Soldiers for " + army.getDisplayName(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        int amount    = (int) spinner.getValue();
        int goldCost  = amount * GameParameters.SOLDIER_RECRUIT_GOLD_COST;
        int manCost   = amount * GameParameters.SOLDIER_RECRUIT_MANPOWER_COST;

        if (!resources.spendMoney(goldCost)) {
            JOptionPane.showMessageDialog(this, "Not enough gold.");
            return;
        }
        if (!resources.spendManpower(manCost)) {
            // Refund gold already spent
            resources.addMoney(goldCost);
            JOptionPane.showMessageDialog(this, "Not enough manpower.");
            return;
        }

        army.addSoldiers(amount);
        Debug.log("military-ui", "recruit-soldiers",
                army.getDisplayName() + " +" + amount
                + " soldiers. Gold cost=" + goldCost + " manpower cost=" + manCost);
        build();
    }

    private String buildCostString(int amount) {
        return String.format("Cost: %d gold + %d manpower | Upkeep: %.1f gold/turn",
                amount * GameParameters.SOLDIER_RECRUIT_GOLD_COST,
                amount * GameParameters.SOLDIER_RECRUIT_MANPOWER_COST,
                amount * GameParameters.SOLDIER_UPKEEP_GOLD);
    }

    // ─── Recruitment pool dialog ──────────────────────────────────────────────

    private void openRecruitUI() {
        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this),
                "Commander Recruitment",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setContentPane(new CommanderRecruitUI(roster, pool, resources,
                () -> { dialog.dispose(); build(); }));
        dialog.setSize(700, 480);
        dialog.setMinimumSize(new Dimension(560, 380));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void styleSmallButton(JButton btn) {
        btn.setFont(UITheme.FONT_SMALL);
        btn.setForeground(UITheme.TEXT_SECONDARY);
        btn.setBackground(UITheme.BUTTON_BG);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
    }
}