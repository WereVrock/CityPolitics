package City.ui;

import City.main.army.commander.Commander;
import City.main.army.commander.CommanderRecruitPool;
import City.main.army.commander.CommanderRoster;
import City.debug.Debug;
import City.main.army.Army;
import City.main.army.ArmyManager;
import City.main.parameters.GameParameters;
import City.main.politics.PartyManager;
import City.main.resources.ResourcePool;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Top-level military panel.
 *
 * Left  — army list: create / disband / merge / assign commander / recruit soldiers.
 * Right — commander roster: shows party affiliation by name, dismiss button.
 * Bottom — open commander recruitment screen.
 */
public class MilitaryMenuUI extends JPanel {

    private final ArmyManager                   armyManager;
    private final CommanderRoster               roster;
    private final CommanderRecruitPool          pool;
    private final ResourcePool                  resources;
    private final PartyManager                  partyManager;
    private final City.main.mercenaries.MercenaryManager mercenaryManager;
    private final Runnable             onBack;
    private final Runnable             onResourcesChanged;

    // ─── Constructor ─────────────────────────────────────────────────────────

public MilitaryMenuUI(ArmyManager armyManager,
                          CommanderRoster roster,
                          CommanderRecruitPool pool,
                          ResourcePool resources,
                          PartyManager partyManager,
                          Runnable onBack) {
        this(armyManager, roster, pool, resources, partyManager, onBack, () -> {});
    }

public MilitaryMenuUI(ArmyManager armyManager,
                      CommanderRoster roster,
                      CommanderRecruitPool pool,
                      ResourcePool resources,
                      PartyManager partyManager,
                      Runnable onBack,
                      Runnable onResourcesChanged) {
    this(armyManager, roster, pool, resources, partyManager, null, onBack, onResourcesChanged);
}

public MilitaryMenuUI(ArmyManager armyManager,
                      CommanderRoster roster,
                      CommanderRecruitPool pool,
                      ResourcePool resources,
                      PartyManager partyManager,
                      City.main.mercenaries.MercenaryManager mercenaryManager,
                      Runnable onBack,
                      Runnable onResourcesChanged) {
    this.armyManager         = armyManager;
    this.roster              = roster;
    this.pool                = pool;
    this.resources           = resources;
    this.partyManager        = partyManager;
    this.mercenaryManager    = mercenaryManager;
    this.onBack              = onBack;
    this.onResourcesChanged  = onResourcesChanged;
    setLayout(new BorderLayout(8, 8));
    setBorder(new EmptyBorder(10, 10, 10, 10));
    setBackground(UITheme.BG_DARK);
    build();
}

// ─── Build ───────────────────────────────────────────────────────────────

    private void build() {
        removeAll();
        add(buildTopBar(),    BorderLayout.NORTH);
        add(buildCentre(),    BorderLayout.CENTER);
        add(buildBottomBar(), BorderLayout.SOUTH);
        revalidate();
        repaint();
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UITheme.BG_PANEL);
        bar.setBorder(new EmptyBorder(6, 10, 6, 10));

        JLabel title = new JLabel("MILITARY");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_GOLD);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setBackground(UITheme.BG_PANEL);

        JButton createBtn = makeBarButton("+ New Army");
        createBtn.setForeground(UITheme.TEXT_GOLD);
        createBtn.addActionListener(e -> openCreateArmyDialog());
        right.add(createBtn);

        JButton backBtn = makeBarButton("◀ BACK");
        backBtn.addActionListener(e -> onBack.run());
        right.add(backBtn);

        bar.add(title, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

private JPanel buildCentre() {
    int cols = (mercenaryManager != null) ? 3 : 2;
    JPanel centre = new JPanel(new GridLayout(1, cols, 12, 0));
    centre.setBackground(UITheme.BG_DARK);
    centre.add(buildArmyScrollPane());
    centre.add(buildRosterScrollPane());
    if (mercenaryManager != null) {
        centre.add(buildMercenaryScrollPane());
    }
    return centre;
}

private JScrollPane buildArmyScrollPane() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UITheme.BG_DARK);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR),
                "Armies", 0, 0,
                UITheme.FONT_BUTTON, UITheme.TEXT_GOLD));

        List<Army> armies = armyManager.getArmies();
        if (armies.isEmpty()) {
            JLabel empty = new JLabel("  No armies.");
            empty.setFont(UITheme.FONT_SMALL);
            empty.setForeground(UITheme.TEXT_SECONDARY);
            panel.add(empty);
        }
        for (Army army : armies) {
            panel.add(buildArmyCard(army));
            panel.add(Box.createVerticalStrut(6));
        }
        JScrollPane sp = new JScrollPane(panel);
        sp.setBorder(null);
        sp.getViewport().setBackground(UITheme.BG_DARK);
        return sp;
    }

    private JScrollPane buildRosterScrollPane() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UITheme.BG_DARK);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR),
                "Commanders", 0, 0,
                UITheme.FONT_BUTTON, UITheme.TEXT_GOLD));

        List<Commander> all = roster.getAllCommanders();
        if (all.isEmpty()) {
            JLabel empty = new JLabel("  No commanders recruited.");
            empty.setFont(UITheme.FONT_SMALL);
            empty.setForeground(UITheme.TEXT_SECONDARY);
            panel.add(empty);
        }
        for (Commander c : all) {
            panel.add(buildCommanderCard(c));
            panel.add(Box.createVerticalStrut(6));
        }
        JScrollPane sp = new JScrollPane(panel);
        sp.setBorder(null);
        sp.getViewport().setBackground(UITheme.BG_DARK);
        return sp;
    }

private JScrollPane buildMercenaryScrollPane() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBackground(UITheme.BG_DARK);
    panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR),
            "Mercenaries", 0, 0,
            UITheme.FONT_BUTTON, new Color(200, 150, 60)));

    List<City.main.mercenaries.MercenaryArmy> mercs = mercenaryManager.getArmies();
    if (mercs.isEmpty()) {
        JLabel empty = new JLabel("  No mercenary companies hired.");
        empty.setFont(UITheme.FONT_SMALL);
        empty.setForeground(UITheme.TEXT_SECONDARY);
        panel.add(empty);
    } else {
        for (City.main.mercenaries.MercenaryArmy merc : mercs) {
            panel.add(buildMercenaryCard(merc));
            panel.add(Box.createVerticalStrut(6));
        }
    }

    double totalUpkeep = mercenaryManager.getTotalUpkeepPerTurn();
    if (totalUpkeep > 0) {
        JLabel upkeepLabel = new JLabel(String.format("  Total upkeep: %.1f gold/turn", totalUpkeep));
        upkeepLabel.setFont(UITheme.FONT_SMALL);
        upkeepLabel.setForeground(UITheme.TEXT_RED);
        panel.add(Box.createVerticalStrut(4));
        panel.add(upkeepLabel);
    }

    JScrollPane sp = new JScrollPane(panel);
    sp.setBorder(null);
    sp.getViewport().setBackground(UITheme.BG_DARK);
    return sp;
}

private JPanel buildMercenaryCard(City.main.mercenaries.MercenaryArmy merc) {
    JPanel card = new JPanel(new BorderLayout(8, 4));
    card.setBackground(new Color(45, 32, 15));
    card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(140, 100, 40), 1),
            new EmptyBorder(8, 10, 8, 10)));
    card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

    JPanel info = new JPanel();
    info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
    info.setBackground(new Color(45, 32, 15));

    addInfoLabel(info, "⚔ " + merc.getDisplayName(),
            UITheme.FONT_BUTTON, new Color(200, 165, 80));
    addInfoLabel(info, "Size: " + merc.getSize(),
            UITheme.FONT_SMALL, UITheme.TEXT_SECONDARY);
    double upkeep = merc.getSize()
            * City.main.parameters.GameParameters.SOLDIER_UPKEEP_GOLD
            * City.main.parameters.GameParameters.MERCENARY_COST_MULTIPLIER;
    addInfoLabel(info, String.format("Upkeep: %.1f gold/turn", upkeep),
            UITheme.FONT_SMALL, UITheme.TEXT_SECONDARY);
    addInfoLabel(info, "Location: " + merc.getZoneId(),
            UITheme.FONT_SMALL, UITheme.TEXT_SECONDARY);

    card.add(info, BorderLayout.CENTER);

    JButton disbandBtn = makeCardButton("Disband");
    disbandBtn.setForeground(new Color(200, 80, 80));
    disbandBtn.addActionListener(e -> {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Disband " + merc.getDisplayName() + "?",
                "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            mercenaryManager.remove(merc);
            build();
        }
    });

    JPanel btns = new JPanel();
    btns.setLayout(new BoxLayout(btns, BoxLayout.Y_AXIS));
    btns.setBackground(new Color(45, 32, 15));
    btns.add(disbandBtn);
    card.add(btns, BorderLayout.EAST);
    return card;
}

private JPanel buildBottomBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 6));
        bar.setBackground(UITheme.BG_PANEL);
        JButton btn = new JButton("Open Commander Recruitment");
        btn.setFont(UITheme.FONT_BUTTON);
        btn.setForeground(UITheme.TEXT_GOLD);
        btn.setBackground(new Color(60, 40, 20));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> openRecruitUI());
        bar.add(btn);
        return bar;
    }

    // ─── Army card ───────────────────────────────────────────────────────────

private JPanel buildArmyCard(Army army) {
        // Highlight if no commander
        boolean missingCmd = !army.hasLivingCommander();
        Color cardBg = missingCmd ? new Color(50, 30, 20) : UITheme.BG_PANEL_LIGHT;
        Color borderCol = missingCmd ? new Color(180, 80, 30) : UITheme.BORDER_COLOR;

        JPanel card = new JPanel(new BorderLayout(8, 4));
        card.setBackground(cardBg);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderCol),
                new EmptyBorder(8, 10, 8, 10)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBackground(cardBg);

        addInfoLabel(info, army.getDisplayName(),
                UITheme.FONT_BUTTON, UITheme.TEXT_PRIMARY);
        addInfoLabel(info,
                army.isInCity() ? "Location: Heartland" : "Deployed: " + army.getZoneId(),
                UITheme.FONT_SMALL, UITheme.TEXT_SECONDARY);
        addInfoLabel(info, "Soldiers: " + army.getSoldierCount(),
                UITheme.FONT_SMALL, UITheme.TEXT_SECONDARY);

        boolean hasCmd  = army.hasLivingCommander();
        String  cmdText = hasCmd
                ? "Cmd: " + army.getCommander().getName()
                  + "  [Skill " + army.getCommandingSkill() + "]"
                : "⚠ No Commander — stays in Heartland";
        Color cmdColor  = hasCmd ? UITheme.TEXT_PRIMARY : new Color(220, 140, 40);
        addInfoLabel(info, cmdText, UITheme.FONT_SMALL, cmdColor);

        // Commander drop target label
        JLabel dropHint = new JLabel(hasCmd ? "" : "← Drop commander here");
        dropHint.setFont(UITheme.FONT_SMALL);
        dropHint.setForeground(new Color(160, 130, 60));
        info.add(dropHint);

        card.add(info, BorderLayout.CENTER);

        // Make card a drop target for commanders
        new java.awt.dnd.DropTarget(card, java.awt.dnd.DnDConstants.ACTION_MOVE,
                new java.awt.dnd.DropTargetAdapter() {
            @Override
            public void dragOver(java.awt.dnd.DropTargetDragEvent dtde) {
                if (dtde.isDataFlavorSupported(COMMANDER_FLAVOR)) {
                    dtde.acceptDrag(java.awt.dnd.DnDConstants.ACTION_MOVE);
                    card.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(UITheme.ACCENT_FROST, 2),
                            new EmptyBorder(8, 10, 8, 10)));
                } else {
                    dtde.rejectDrag();
                }
            }
            @Override
            public void dragExit(java.awt.dnd.DropTargetEvent dte) {
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(borderCol),
                        new EmptyBorder(8, 10, 8, 10)));
            }
            @Override
            public void drop(java.awt.dnd.DropTargetDropEvent dtde) {
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(borderCol),
                        new EmptyBorder(8, 10, 8, 10)));
                try {
                    if (!dtde.isDataFlavorSupported(COMMANDER_FLAVOR)) {
                        dtde.rejectDrop();
                        return;
                    }
                    dtde.acceptDrop(java.awt.dnd.DnDConstants.ACTION_MOVE);
                    Commander dropped = (Commander) dtde.getTransferable()
                            .getTransferData(COMMANDER_FLAVOR);
                    // Unassign from any current army
                    for (Army a : armyManager.getArmies()) {
                        if (a.getCommander() == dropped) a.setCommander(null);
                    }
                    army.setCommander(dropped);
                    dtde.dropComplete(true);
                    build();
                } catch (Exception ex) {
                    dtde.rejectDrop();
                }
            }
        }, true);

        JPanel btns = new JPanel();
        btns.setLayout(new BoxLayout(btns, BoxLayout.Y_AXIS));
        btns.setBackground(cardBg);

        JButton recruitBtn = makeCardButton("Recruit");
        recruitBtn.setToolTipText("Recruit soldiers into this army");
        recruitBtn.addActionListener(e -> openRecruitSoldiersDialog(army));
        btns.add(recruitBtn);
        btns.add(Box.createVerticalStrut(3));

        boolean canMerge = army.isInCity()
                && armyManager.getCityArmies().stream().anyMatch(a -> a != army);
        JButton mergeBtn = makeCardButton("Merge Into");
        mergeBtn.setToolTipText("Transfer all soldiers into another heartland army, then disband this one");
        mergeBtn.setEnabled(canMerge);
        if (!canMerge) mergeBtn.setForeground(Color.GRAY);
        mergeBtn.addActionListener(e -> openMergeDialog(army));
        btns.add(mergeBtn);
        btns.add(Box.createVerticalStrut(3));

        if (hasCmd) {
            JButton removeCmd = makeCardButton("Remove Cmd");
            removeCmd.setForeground(new Color(200, 140, 60));
            removeCmd.setToolTipText("Unassign commander from this army");
            removeCmd.addActionListener(e -> {
                army.setCommander(null);
                build();
            });
            btns.add(removeCmd);
            btns.add(Box.createVerticalStrut(3));
        }

        JButton disbandBtn = makeCardButton("Disband");
        disbandBtn.setForeground(new Color(200, 80, 80));
        disbandBtn.setToolTipText("Disband — soldiers return to manpower pool");
        disbandBtn.addActionListener(e -> confirmDisband(army));
        btns.add(disbandBtn);

        card.add(btns, BorderLayout.EAST);
        return card;
    }

// ─── Commander card ───────────────────────────────────────────────────────

private JPanel buildCommanderCard(Commander c) {
        // Highlight unassigned commanders
        boolean isAssigned = armyManager.getArmies().stream()
                .anyMatch(a -> a.getCommander() == c);
        Color cardBg   = isAssigned ? UITheme.BG_PANEL_LIGHT : new Color(20, 35, 20);
        Color borderCol = isAssigned ? UITheme.BORDER_COLOR   : new Color(60, 140, 60);

        JPanel card = new JPanel(new BorderLayout(8, 4));
        card.setBackground(cardBg);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderCol),
                new EmptyBorder(8, 10, 8, 10)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBackground(cardBg);

        Color nameColor = c.isAlive() ? (isAssigned ? UITheme.TEXT_PRIMARY : new Color(160, 220, 160)) : Color.GRAY;
        addInfoLabel(info, c.getName() + (c.isAlive() ? "" : "  ✝") + (isAssigned ? "" : "  [AVAILABLE]"),
                UITheme.FONT_BUTTON, nameColor);

        addInfoLabel(info,
                "Skill " + c.getCommandingSkill()
                + "  |  XP " + c.getXp()
                + (c.xpToNextLevel() > 0
                        ? "  (need " + c.xpToNextLevel() + " more)"
                        : "  [MAX SKILL]"),
                UITheme.FONT_SMALL, UITheme.TEXT_SECONDARY);

        String partyName = resolvePartyName(c);
        addInfoLabel(info,
                "Party: " + partyName
                + "  |  Upkeep: " + String.format("%.1f", c.getUpkeepCost()) + " gold/turn",
                UITheme.FONT_SMALL, UITheme.TEXT_SECONDARY);

        String assigned = armyManager.getArmies().stream()
                .filter(a -> a.getCommander() == c)
                .map(Army::getDisplayName)
                .findFirst().orElse("— drag to assign —");
        addInfoLabel(info, "Army: " + assigned,
                UITheme.FONT_SMALL, isAssigned ? UITheme.TEXT_SECONDARY : new Color(130, 190, 130));

        card.add(info, BorderLayout.CENTER);

        JPanel btns = new JPanel();
        btns.setLayout(new BoxLayout(btns, BoxLayout.Y_AXIS));
        btns.setBackground(cardBg);

        if (c.isAlive()) {
            JButton dismissBtn = makeCardButton("Dismiss");
            dismissBtn.setForeground(new Color(200, 80, 80));
            dismissBtn.setToolTipText("Costs " + City.main.parameters.GameParameters.COMMANDER_DISMISS_COST
                    + " influence; lowers " + partyName + " opinion");
            dismissBtn.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Dismiss " + c.getName() + "?\n"
                        + "Cost: " + City.main.parameters.GameParameters.COMMANDER_DISMISS_COST + " influence\n"
                        + partyName + " opinion −" + City.main.parameters.GameParameters.COMMANDER_DISMISS_OPINION_LOSS,
                        "Confirm Dismiss", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    boolean ok = roster.dismiss(c);
                    if (!ok) {
                        JOptionPane.showMessageDialog(this, "Not enough influence.");
                        Debug.log("military-ui", "dismiss-fail", c.getName());
                    } else {
                        Debug.log("military-ui", "dismiss-ok", c.getName());
                        onResourcesChanged.run();
                        build();
                    }
                }
            });
            btns.add(dismissBtn);
        }

        card.add(btns, BorderLayout.EAST);

        // Make card draggable
        if (c.isAlive()) {
            java.awt.dnd.DragSource ds = java.awt.dnd.DragSource.getDefaultDragSource();
            ds.createDefaultDragGestureRecognizer(card,
                    java.awt.dnd.DnDConstants.ACTION_MOVE, dge -> {
                java.awt.datatransfer.Transferable t = new java.awt.datatransfer.Transferable() {
                    @Override public java.awt.datatransfer.DataFlavor[] getTransferDataFlavors() {
                        return new java.awt.datatransfer.DataFlavor[]{COMMANDER_FLAVOR};
                    }
                    @Override public boolean isDataFlavorSupported(java.awt.datatransfer.DataFlavor f) {
                        return f.equals(COMMANDER_FLAVOR);
                    }
                    @Override public Object getTransferData(java.awt.datatransfer.DataFlavor f) {
                        return c;
                    }
                };
                dge.startDrag(java.awt.dnd.DragSource.DefaultMoveDrop, t);
            });
        }

        return card;
    }

// ─── Create army ─────────────────────────────────────────────────────────

private void openCreateArmyDialog() {
        java.util.List<String> existing = armyManager.getArmies().stream()
                .map(City.main.army.Army::getDisplayName)
                .collect(java.util.stream.Collectors.toList());
        String suggested = City.main.army.ArmyNameGenerator.generateUnique(existing);

        String name = (String) JOptionPane.showInputDialog(
                this,
                "Enter name for new army:",
                "Create New Army",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                suggested);

        if (name == null || name.isBlank()) return;
        Army army = armyManager.createArmy(name.trim());
        Debug.log("military-ui", "create-army",
                army.getId() + " — " + army.getDisplayName());
        build();
    }

// ─── Disband army ─────────────────────────────────────────────────────────

    private void confirmDisband(Army army) {
        int soldiers = army.getSoldierCount();
        int confirm  = JOptionPane.showConfirmDialog(this,
                "Disband " + army.getDisplayName() + "?\n"
                + soldiers + " soldiers return to manpower pool.",
                "Confirm Disband", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        resources.addManpower(soldiers);
        armyManager.removeArmy(army);
        Debug.log("military-ui", "disband",
                army.getDisplayName() + " disbanded. +" + soldiers + " manpower returned.");
        build();
    }

    // ─── Merge army ───────────────────────────────────────────────────────────

    private void openMergeDialog(Army source) {
        List<Army> targets = armyManager.getCityArmies().stream()
                .filter(a -> a != source)
                .toList();

        if (targets.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No other heartland armies to merge into.");
            return;
        }

        String[] options = targets.stream()
                .map(a -> a.getDisplayName() + "  (" + a.getSoldierCount() + " soldiers)")
                .toArray(String[]::new);

        int choice = JOptionPane.showOptionDialog(this,
                "Merge " + source.getDisplayName()
                + "  (" + source.getSoldierCount() + " soldiers)  into:",
                "Merge Army",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        if (choice < 0) return;

        Army target      = targets.get(choice);
        int transferred = armyManager.mergeArmies(source, target);
        Debug.log("military-ui", "merge",
                source.getDisplayName() + " → " + target.getDisplayName()
                + " transferred=" + transferred);
        build();
    }

    // ─── Assign commander ─────────────────────────────────────────────────────

    private void openAssignCommanderDialog(Army army) {
        List<Commander> available = roster.getAliveCommanders();
        if (available.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No living commanders available. Recruit one first.");
            return;
        }

        String[] options = new String[available.size() + 1];
        options[0] = "— None (unassign) —";
        for (int i = 0; i < available.size(); i++) {
            Commander c = available.get(i);
            options[i + 1] = c.getName()
                    + "  [Skill " + c.getCommandingSkill()
                    + " | " + c.getPartyName() + "]";
        }

        String current = army.hasLivingCommander()
                ? "Current: " + army.getCommander().getName()
                : "Current: None";

        int choice = JOptionPane.showOptionDialog(this,
                current + "\n\nSelect commander for " + army.getDisplayName() + ":",
                "Assign Commander",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        if (choice < 0) return;

        if (choice == 0) {
            // Unassigning — army must return to heartland immediately
            army.setCommander(null);
            army.recallToCity();
            Debug.log("military-ui", "assign-cmd",
                    army.getDisplayName() + " — commander removed, recalled to heartland");
        } else {
            Commander chosen = available.get(choice - 1);
            army.setCommander(chosen);
            Debug.log("military-ui", "assign-cmd",
                    army.getDisplayName() + " → " + chosen.getName());
        }
        build();
    }

    // ─── Recruit soldiers ─────────────────────────────────────────────────────

private void openRecruitSoldiersDialog(Army army) {
        int maxByGold     = (int)(resources.getMoney()  / City.main.parameters.GameParameters.SOLDIER_RECRUIT_GOLD_COST);
        int maxByManpower = resources.getManpower()     / City.main.parameters.GameParameters.SOLDIER_RECRUIT_MANPOWER_COST;
        int maxRecruit    = Math.max(0, Math.min(maxByGold, maxByManpower));

        if (maxRecruit <= 0) {
            JOptionPane.showMessageDialog(this,
                    "Cannot recruit — need at least "
                    + City.main.parameters.GameParameters.SOLDIER_RECRUIT_GOLD_COST + " gold and "
                    + City.main.parameters.GameParameters.SOLDIER_RECRUIT_MANPOWER_COST
                    + " manpower per soldier.",
                    "Recruit Soldiers", JOptionPane.WARNING_MESSAGE);
            return;
        }

        final int[] amount = {1};

        JPanel dialog = new JPanel();
        dialog.setLayout(new BoxLayout(dialog, BoxLayout.Y_AXIS));
        dialog.setBackground(UITheme.BG_DARK);
        dialog.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel titleLbl = new JLabel("Recruit Soldiers — " + army.getDisplayName());
        titleLbl.setFont(UITheme.FONT_TITLE);
        titleLbl.setForeground(UITheme.TEXT_GOLD);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        dialog.add(titleLbl);
        dialog.add(Box.createVerticalStrut(10));

        JLabel resLbl = new JLabel("Available: " + resources.getMoney()
                + " gold  |  " + resources.getManpower() + " manpower  |  Max: " + maxRecruit);
        resLbl.setFont(UITheme.FONT_SMALL);
        resLbl.setForeground(UITheme.TEXT_SECONDARY);
        resLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        dialog.add(resLbl);
        dialog.add(Box.createVerticalStrut(12));

        JLabel amountDisplay = new JLabel(String.valueOf(amount[0]));
        amountDisplay.setFont(amountDisplay.getFont().deriveFont(Font.BOLD, 28f));
        amountDisplay.setForeground(UITheme.TEXT_GOLD);
        amountDisplay.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel costLbl = new JLabel(buildCostString(amount[0]));
        costLbl.setFont(UITheme.FONT_SMALL);
        costLbl.setForeground(UITheme.TEXT_SECONDARY);
        costLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JSlider slider = new JSlider(1, Math.max(1, maxRecruit), 1);
        slider.setBackground(UITheme.BG_DARK);
        slider.setForeground(UITheme.TEXT_SECONDARY);
        slider.setAlignmentX(Component.LEFT_ALIGNMENT);
        slider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        if (maxRecruit <= 20) {
            slider.setMajorTickSpacing(1);
            slider.setPaintTicks(true);
            slider.setPaintLabels(true);
        } else if (maxRecruit <= 200) {
            slider.setMajorTickSpacing(50);
            slider.setMinorTickSpacing(10);
            slider.setPaintTicks(true);
        }

        Runnable updateAll = () -> {
            amountDisplay.setText(String.valueOf(amount[0]));
            costLbl.setText(buildCostString(amount[0]));
            if (slider.getValue() != amount[0]) slider.setValue(amount[0]);
        };

        slider.addChangeListener(e -> {
            if (amount[0] != slider.getValue()) {
                amount[0] = slider.getValue();
                amountDisplay.setText(String.valueOf(amount[0]));
                costLbl.setText(buildCostString(amount[0]));
            }
        });

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        btnRow.setBackground(UITheme.BG_DARK);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton minusBtn = makeStepButton("−");
        JButton plusBtn  = makeStepButton("+");

        minusBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                amount[0] = Math.max(1, amount[0] - resolveStep(e));
                updateAll.run();
            }
        });
        plusBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                amount[0] = Math.min(maxRecruit, amount[0] + resolveStep(e));
                updateAll.run();
            }
        });

        JButton maxBtn = makeCardButton("MAX");
        maxBtn.addActionListener(e -> { amount[0] = maxRecruit; updateAll.run(); });

        JLabel hintLbl = new JLabel("Ctrl=×10  Shift=×100  Ctrl+Shift=×1000");
        hintLbl.setFont(UITheme.FONT_SMALL);
        hintLbl.setForeground(new Color(120, 120, 120));
        hintLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnRow.add(minusBtn);
        btnRow.add(amountDisplay);
        btnRow.add(plusBtn);
        btnRow.add(Box.createHorizontalStrut(8));
        btnRow.add(maxBtn);

        dialog.add(btnRow);
        dialog.add(Box.createVerticalStrut(4));
        dialog.add(hintLbl);
        dialog.add(Box.createVerticalStrut(8));
        dialog.add(slider);
        dialog.add(Box.createVerticalStrut(10));
        dialog.add(costLbl);

        int result = JOptionPane.showConfirmDialog(this, dialog,
                "Recruit Soldiers",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        int goldCost = amount[0] * City.main.parameters.GameParameters.SOLDIER_RECRUIT_GOLD_COST;
        int manCost  = amount[0] * City.main.parameters.GameParameters.SOLDIER_RECRUIT_MANPOWER_COST;

        if (!resources.spendMoney(goldCost)) {
            JOptionPane.showMessageDialog(this, "Not enough gold."); return;
        }
        if (!resources.spendManpower(manCost)) {
            resources.addMoney(goldCost);
            JOptionPane.showMessageDialog(this, "Not enough manpower."); return;
        }

        army.addSoldiers(amount[0]);
        Debug.log("military-ui", "recruit-soldiers",
                army.getDisplayName() + " +" + amount[0]
                + "  gold=" + goldCost + "  manpower=" + manCost);
        onResourcesChanged.run();
        build();
    }

private int resolveStep(MouseEvent e) {
        boolean ctrl  = (e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK)  != 0;
        boolean shift = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;
        if (ctrl && shift) return 1000;
        if (shift)         return 100;
        if (ctrl)          return 10;
        return 1;
    }

    private String buildCostString(int n) {
        return String.format("Cost: %d gold  +  %d manpower  |  Upkeep: %.1f gold/turn",
                n * GameParameters.SOLDIER_RECRUIT_GOLD_COST,
                n * GameParameters.SOLDIER_RECRUIT_MANPOWER_COST,
                n * GameParameters.SOLDIER_UPKEEP_GOLD);
    }

    // ─── Recruitment pool dialog ──────────────────────────────────────────────

private void openRecruitUI() {
        JDialog d = new JDialog(
                SwingUtilities.getWindowAncestor(this),
                "Commander Recruitment",
                Dialog.ModalityType.APPLICATION_MODAL);

        // Both the close button and the window X button trigger the same cleanup
        Runnable closeAction = () -> {
            if (d.isVisible()) {
                d.dispose();
                onResourcesChanged.run();
                build();
            }
        };

        d.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        d.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                closeAction.run();
            }
        });

        d.setContentPane(new CommanderRecruitUI(roster, pool, resources, closeAction));
        d.setSize(720, 500);
        d.setMinimumSize(new Dimension(580, 400));
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

// ─── Party name resolution ────────────────────────────────────────────────

    /**
     * Resolves the player-facing party name from a PolitcalView affiliation.
     * Falls back to the view display name if no party claims that view strongly.
     */
    private String resolvePartyName(City.main.army.commander.Commander c) {
        return c.getPartyName();
    }

    // ─── Commander DataFlavor ─────────────────────────────────────────────────

    static final java.awt.datatransfer.DataFlavor COMMANDER_FLAVOR =
            new java.awt.datatransfer.DataFlavor(Commander.class, "Commander");

    // ─── Button factories ─────────────────────────────────────────────────────

    private JButton makeBarButton(String label) {
        JButton btn = new JButton(label);
        btn.setFont(UITheme.FONT_BUTTON);
        btn.setForeground(UITheme.TEXT_SECONDARY);
        btn.setBackground(UITheme.BUTTON_BG);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton makeCardButton(String label) {
        JButton btn = new JButton(label);
        btn.setFont(UITheme.FONT_SMALL);
        btn.setForeground(UITheme.TEXT_SECONDARY);
        btn.setBackground(UITheme.BUTTON_BG);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        return btn;
    }

private JButton makeStepButton(String label) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("Dialog", Font.BOLD, 20));
        btn.setForeground(UITheme.TEXT_GOLD);
        btn.setBackground(UITheme.BUTTON_BG);
        btn.setBorderPainted(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(48, 40));
        btn.setMinimumSize(new Dimension(48, 40));
        return btn;
    }

private void addInfoLabel(JPanel panel, String text, Font font, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(font);
        lbl.setForeground(color);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(lbl);
    }
}