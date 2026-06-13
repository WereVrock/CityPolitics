// MapInfoPanel.java
package ui.map;

import main.map.Zone;
import main.map.ZoneManager;
import main.map.ZoneState;
import ui.GrantZoneClaimDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import ui.UITheme;

public class MapInfoPanel extends JPanel {

private static final String CARD_ZONE  = "zone";
private static final String CARD_ARMY  = "army";
private static final String CARD_EMPTY = "empty";

private final ZoneManager   zoneManager;
private final CardLayout    cardLayout;
private final JPanel        cards;
private final main.nobles.NobleHouseManager nobleHouseManager;
private main.barbarians.RavagedZoneManager  ravagedZoneManager;
private main.barbarians.BarbArmyManager     barbArmyManager;

public void setRavagedZoneManager(main.barbarians.RavagedZoneManager rzm) {
    this.ravagedZoneManager = rzm;
}

public void setBarbArmyManager(main.barbarians.BarbArmyManager bam) {
    this.barbArmyManager = bam;
}

// Zone
private final JLabel   zoneTitleLabel;
private final JLabel   zoneTypeLabel;
private final JButton  ownerButton;
private final JLabel   goldLabel;
private final JLabel   foodLabel;
private final JLabel   popsLabel;
private final JLabel   supplyLabel;
private final JLabel   damageLabel;
private final JTextArea adjacentArea;

// Zone claims
private final JPanel   claimsPanel;
private       java.util.function.BiConsumer<String, main.nobles.NobleHouseManager> grantClaimFromMapCallback;

// Army
private final JLabel   armyTitleLabel;
private final JLabel   armyZoneLabel;
private final JLabel   armyStatusLabel;
private final JLabel   armySizeLabel;
private final JLabel   armyUpkeepLabel;
private final JButton  commanderButton;
private final JLabel   commanderSkillLabel;
// Barbarian pay-off button
private final JButton barbPayOffBtn;
private final JButton barbDismissBtn;

public MapInfoPanel(ZoneManager zoneManager, main.nobles.NobleHouseManager nobleHouseManager) {
this.zoneManager       = zoneManager;
this.nobleHouseManager = nobleHouseManager;

cardLayout = new CardLayout();
cards      = new JPanel(cardLayout);
cards.setBackground(UITheme.BG_PANEL);

setBackground(UITheme.BG_PANEL);
setLayout(new BorderLayout());
setBorder(new EmptyBorder(8, 8, 8, 8));

// ── Empty card ──
JPanel emptyCard = new JPanel();
emptyCard.setBackground(UITheme.BG_PANEL);
emptyCard.add(makeLabel("Select a zone\nor an army.", UITheme.TEXT_SECONDARY, UITheme.FONT_SMALL));
cards.add(emptyCard, CARD_EMPTY);

// ── Zone card ──
zoneTitleLabel = makeLabel("", UITheme.TEXT_GOLD,        UITheme.FONT_HEADER);
zoneTypeLabel  = makeLabel("", UITheme.TEXT_SECONDARY,   UITheme.FONT_SMALL);
goldLabel      = makeLabel("", new Color(210, 170, 80),  UITheme.FONT_BODY);
foodLabel      = makeLabel("", new Color(120, 200, 100), UITheme.FONT_BODY);
popsLabel      = makeLabel("", UITheme.TEXT_PRIMARY,     UITheme.FONT_BODY);
supplyLabel    = makeLabel("", UITheme.ACCENT_FROST,     UITheme.FONT_BODY);
damageLabel    = makeLabel("", UITheme.TEXT_RED,         UITheme.FONT_BODY);

ownerButton = new JButton("");
ownerButton.setFont(UITheme.FONT_SMALL);
ownerButton.setForeground(new Color(220, 190, 130));
ownerButton.setBackground(UITheme.BG_PANEL);
ownerButton.setBorderPainted(false);
ownerButton.setFocusPainted(false);
ownerButton.setContentAreaFilled(false);
ownerButton.setHorizontalAlignment(SwingConstants.LEFT);
ownerButton.setHorizontalTextPosition(SwingConstants.LEFT);
ownerButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

adjacentArea = new JTextArea();
adjacentArea.setEditable(false);
adjacentArea.setBackground(UITheme.BG_PANEL);
adjacentArea.setForeground(UITheme.TEXT_PRIMARY);
adjacentArea.setFont(UITheme.FONT_SMALL);
adjacentArea.setLineWrap(true);
adjacentArea.setWrapStyleWord(true);

claimsPanel = new JPanel();
claimsPanel.setLayout(new BoxLayout(claimsPanel, BoxLayout.Y_AXIS));
claimsPanel.setBackground(UITheme.BG_PANEL);

JPanel zoneCard = new JPanel(new GridBagLayout());
zoneCard.setBackground(UITheme.BG_PANEL);

GridBagConstraints zc = new GridBagConstraints();
zc.gridx   = 0;
zc.weightx = 1.0;
zc.fill    = GridBagConstraints.HORIZONTAL;
zc.insets  = new Insets(1, 0, 1, 0);

zc.gridy = 0;  zoneCard.add(zoneTitleLabel, zc);
zc.gridy = 1;  zoneCard.add(zoneTypeLabel,  zc);
zc.gridy = 2;  zoneCard.add(ownerButton,    zc);
zc.gridy = 3;  zoneCard.add(sep(),          zc);
zc.gridy = 4;  zoneCard.add(goldLabel,      zc);
zc.gridy = 5;  zoneCard.add(foodLabel,      zc);
zc.gridy = 6;  zoneCard.add(popsLabel,      zc);
zc.gridy = 7;  zoneCard.add(sep(),          zc);
zc.gridy = 8;  zoneCard.add(supplyLabel,    zc);
zc.gridy = 9;  zoneCard.add(damageLabel,    zc);
zc.gridy = 10; zoneCard.add(sep(),          zc);
zc.gridy = 11; zoneCard.add(makeLabel("Claims:", UITheme.TEXT_SECONDARY, UITheme.FONT_SMALL), zc);
zc.gridy = 12; zoneCard.add(claimsPanel,    zc);
zc.gridy = 13; zoneCard.add(sep(),          zc);
zc.gridy = 14; zoneCard.add(makeLabel("Adjacent:", UITheme.TEXT_SECONDARY, UITheme.FONT_SMALL), zc);
zc.gridy   = 15;
zc.weighty = 1.0;
zc.fill    = GridBagConstraints.BOTH;
zoneCard.add(adjacentArea, zc);

cards.add(zoneCard, CARD_ZONE);

// ── Army card ──
armyTitleLabel  = makeLabel("", UITheme.TEXT_RED,       UITheme.FONT_HEADER);
armyZoneLabel   = makeLabel("", UITheme.TEXT_PRIMARY,   UITheme.FONT_BODY);
armyStatusLabel = makeLabel("", UITheme.TEXT_SECONDARY, UITheme.FONT_SMALL);
armySizeLabel   = makeLabel("", UITheme.TEXT_PRIMARY,   UITheme.FONT_BODY);
armyUpkeepLabel = makeLabel("", UITheme.TEXT_SECONDARY, UITheme.FONT_SMALL);

commanderButton = new JButton("");
commanderButton.setFont(UITheme.FONT_BUTTON);
commanderButton.setForeground(new Color(180, 210, 255));
commanderButton.setBackground(UITheme.BG_PANEL);
commanderButton.setBorderPainted(false);
commanderButton.setFocusPainted(false);
commanderButton.setContentAreaFilled(false);
commanderButton.setHorizontalAlignment(SwingConstants.LEFT);
commanderButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
commanderButton.setVisible(false);

commanderSkillLabel = makeLabel("", UITheme.TEXT_SECONDARY, UITheme.FONT_SMALL);
commanderSkillLabel.setVisible(false);

barbPayOffBtn = new JButton("PAY OFF");
barbPayOffBtn.setFont(UITheme.FONT_BUTTON);
barbPayOffBtn.setForeground(new Color(210, 170, 80));
barbPayOffBtn.setBackground(UITheme.BUTTON_BG);
barbPayOffBtn.setBorderPainted(false);
barbPayOffBtn.setFocusPainted(false);
barbPayOffBtn.setVisible(false);
barbPayOffBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

barbDismissBtn = new JButton("DISMISS (6x)");
barbDismissBtn.setFont(UITheme.FONT_BUTTON);
barbDismissBtn.setForeground(new Color(120, 200, 100));
barbDismissBtn.setBackground(UITheme.BUTTON_BG);
barbDismissBtn.setBorderPainted(false);
barbDismissBtn.setFocusPainted(false);
barbDismissBtn.setVisible(false);
barbDismissBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

JPanel armyCard = new JPanel(new GridBagLayout());
armyCard.setBackground(UITheme.BG_PANEL);
GridBagConstraints ac = new GridBagConstraints();
ac.gridx = 0; ac.weightx = 1.0; ac.fill = GridBagConstraints.HORIZONTAL;
ac.insets = new Insets(2, 0, 2, 0);
ac.gridy = 0; armyCard.add(armyTitleLabel,      ac);
ac.gridy = 1; armyCard.add(armyZoneLabel,        ac);
ac.gridy = 2; armyCard.add(armySizeLabel,        ac);
ac.gridy = 3; armyCard.add(armyUpkeepLabel,      ac);
ac.gridy = 4; armyCard.add(armyStatusLabel,      ac);
ac.gridy = 5; armyCard.add(sep(),                ac);
ac.gridy = 6; armyCard.add(commanderButton,      ac);
ac.gridy = 7; armyCard.add(commanderSkillLabel,  ac);
ac.gridy = 8; armyCard.add(barbPayOffBtn,        ac);
ac.gridy = 9; armyCard.add(barbDismissBtn,       ac);
ac.gridy = 10; ac.weighty = 1.0; ac.fill = GridBagConstraints.BOTH;
armyCard.add(Box.createVerticalGlue(), ac);

cards.add(armyCard, CARD_ARMY);

add(cards, BorderLayout.CENTER);
cardLayout.show(cards, CARD_EMPTY);
}

public void showZone(Zone zone) {
    if (zone == null) { clearZone(); return; }

    if (zone.isDesolate()) {
        showDesolateZone(zone);
        return;
    }

    ZoneState state = zoneManager.getState(zone.getId());

    zoneTitleLabel.setText(zone.getDisplayName());
    zoneTypeLabel.setText(capitalize(zone.getSettlement().name()));

    for (java.awt.event.ActionListener al : ownerButton.getActionListeners())
        ownerButton.removeActionListener(al);

    main.nobles.NobleHouse owner = nobleHouseManager.getOwnerOfZone(zone.getId());
    if (owner != null) {
        ownerButton.setText("<html><body>⚑ "
                + GrantZoneClaimDialog.stripHousePrefix(owner.getName())
                + "</body></html>");
        ownerButton.setForeground(new Color(220, 190, 130));
        ownerButton.addActionListener(e -> showHouseDialog(owner));
    } else {
        boolean isBarbarian = barbArmyManager != null
                && !barbArmyManager.getGarrisonsInZone(zone.getId()).isEmpty();
        if (isBarbarian) {
            ownerButton.setText("<html><body>☠ Barbaric</body></html>");
            ownerButton.setForeground(new Color(200, 60, 40));
        } else {
            ownerButton.setText("<html><body>Unowned</body></html>");
            ownerButton.setForeground(UITheme.TEXT_SECONDARY);
        }
    }

    goldLabel.setText("Gold/turn:  " + zone.getGoldProduction());
    foodLabel.setText("Food/turn:  " + zone.getFoodProduction());
    popsLabel.setText("Pops:       " + zone.getZonePops());
    supplyLabel.setText("Supply:     " + state.getSupplyLevel() + "%");
    damageLabel.setText("Damage:     " + state.getDamage() + "%");

    main.nobles.NobleHouse zoneOwner = nobleHouseManager.getOwnerOfZone(zone.getId());
    if (zoneOwner != null) {
        int fort     = zoneOwner.getFortificationFor(zone.getId());
        int garrison = zoneOwner.getGarrisonFor(zone.getId());
        int maxGarr  = zoneOwner.getMaxGarrisonFor(zone.getId());
        supplyLabel.setText("Supply:     " + state.getSupplyLevel() + "%"
            + "   Fort: " + fort);
        damageLabel.setText("Damage:     " + state.getDamage() + "%"
            + "   Garrison: " + garrison + "/" + maxGarr);
    }

    if (ravagedZoneManager != null) {
        main.barbarians.RavagedZoneManager.RavagedLevel lvl =
                ravagedZoneManager.getLevel(zone.getId());
        if (lvl == main.barbarians.RavagedZoneManager.RavagedLevel.HEAVILY_RAVAGED) {
            damageLabel.setText(damageLabel.getText() + "   ☠ HEAVILY RAVAGED");
            damageLabel.setForeground(new java.awt.Color(220, 40, 40));
        } else if (lvl == main.barbarians.RavagedZoneManager.RavagedLevel.RAVAGED) {
            damageLabel.setText(damageLabel.getText() + "   ☠ Ravaged");
            damageLabel.setForeground(new java.awt.Color(200, 110, 40));
        } else {
            damageLabel.setForeground(UITheme.TEXT_RED);
        }
    }

    // Claims panel
    updateClaimsPanel(zone.getId());

    StringBuilder sb = new StringBuilder();
    for (String adjId : zone.getAdjacentIds()) {
        Zone adj = zoneManager.getZone(adjId);
        if (adj != null) sb.append(adj.getDisplayName()).append("\n");
    }
    adjacentArea.setText(sb.toString().trim());
    cardLayout.show(cards, CARD_ZONE);
}

private void showDesolateZone(Zone zone) {
    zoneTitleLabel.setText(zone.getDisplayName());
    zoneTypeLabel.setText("Desolate Wastes");

    for (java.awt.event.ActionListener al : ownerButton.getActionListeners())
        ownerButton.removeActionListener(al);
    ownerButton.setText("<html><body>No ruler. No law.</body></html>");
    ownerButton.setForeground(new Color(100, 95, 90));

    goldLabel.setText("Gold/turn:  —");
    foodLabel.setText("Food/turn:  —");
    popsLabel.setText("Pops:       —");
    supplyLabel.setText("");
    damageLabel.setText("");

    // Build adjacent zones list
    StringBuilder sbAdj = new StringBuilder();
    for (String adjId : zone.getAdjacentIds()) {
        Zone adj = zoneManager.getZone(adjId);
        if (adj != null) sbAdj.append(adj.getDisplayName()).append("\n");
    }
    String adjacentText = sbAdj.toString().trim();
    
    // Flavour text
    String flavour = switch (zone.getId()) {
        case "waste_northeast" -> "Bitter winds scour these cracked plains. Nothing that enters ever returns the same.";
        case "waste_east"      -> "A sundered land of jagged rock and silence. Travellers speak of shapes moving at dusk.";
        case "waste_se_upper"  -> "The coast rots here. Black sand, dead tide, and the stench of old iron.";
        case "waste_se_lower"  -> "Cliffs that crumble into a churning void. The sea below swallows everything.";
        case "waste_southwest" -> "A moor without end. Fog clings to the ground even in high summer.";
        case "waste_farSW"     -> "Whatever once lived here left no trace. Only the wind remains, and it mourns.";
        default                -> "These lands lie beyond the reach of civilisation.";
    };
    
    // Combine: adjacent zones (if any) then blank line then flavour
    StringBuilder combined = new StringBuilder();
    if (!adjacentText.isEmpty()) {
        combined.append(adjacentText).append("\n\n");
    }
    combined.append(flavour);
    adjacentArea.setText(combined.toString());
    
    cardLayout.show(cards, CARD_ZONE);
}

public void showArmy(main.army.Army army, ZoneManager zm) {
    if (army == null) { clearArmy(); return; }
    armyTitleLabel.setForeground(UITheme.ACCENT_FROST);
    armyTitleLabel.setText("⚔ " + army.getDisplayName());
    if (army.isInCity()) {
        armyZoneLabel.setText("📍 Heartland (City)");
        armyStatusLabel.setText("Status: In city");
    } else {
        Zone zone = zm.getZone(army.getZoneId());
        armyZoneLabel.setText("📍 " + (zone != null ? zone.getDisplayName() : army.getZoneId()));
        armyStatusLabel.setText("Status: Deployed");
    }
    armySizeLabel.setText("Size: " + army.getSize());
    armyUpkeepLabel.setText("");
    barbPayOffBtn.setVisible(false);
    barbDismissBtn.setVisible(false);

    // Commander section
    for (java.awt.event.ActionListener al : commanderButton.getActionListeners())
        commanderButton.removeActionListener(al);

    main.army.commander.Commander cmd = army.getCommander();
    if (cmd != null) {
        commanderButton.setText("⚔ " + cmd.getName());
        commanderButton.setVisible(true);
        commanderSkillLabel.setText("Skill: " + skillLabel(cmd.getCommandingSkill()));
        commanderSkillLabel.setVisible(true);
        commanderButton.addActionListener(e -> showCommanderDialog(cmd));
    } else {
        commanderButton.setVisible(false);
        commanderSkillLabel.setVisible(false);
    }

    cardLayout.show(cards, CARD_ARMY);
}

public void showNobleArmy(main.nobles.NobleArmy army, ZoneManager zm,
                           main.nobles.NobleHouseManager houseManager) {
    if (army == null) { clearArmy(); return; }
    main.nobles.NobleHouse owner = houseManager.getHouseById(army.getHouseId());
    String houseName = owner != null ? owner.getName() : army.getHouseId();
    armyTitleLabel.setForeground(UITheme.ACCENT_FROST);
    armyTitleLabel.setText("🏰 " + houseName + " Army");
    Zone zone = zm.getZone(army.getZoneId());
    armyZoneLabel.setText("📍 " + (zone != null ? zone.getDisplayName() : army.getZoneId()));
    armySizeLabel.setText("Size: " + army.getSize() + " soldiers");
    int baseUpkeep = army.getSize() * main.parameters.GameParameters.NOBLE_UPKEEP_COST_PER_SOLDIER;
    if (owner != null && owner.getZoneIds().contains(army.getZoneId())
            && !army.hasPendingOrder()) {
        int discounted = (int)(baseUpkeep
                * (1.0 - main.parameters.GameParameters.NOBLE_UPKEEP_DEFENSE_DISCOUNT));
        if (discounted < 1) discounted = 1;
        armyUpkeepLabel.setText("Upkeep: " + discounted + " gold/turn (defending discount)");
    } else {
        armyUpkeepLabel.setText("Upkeep: " + baseUpkeep + " gold/turn");
    }
    String order = army.hasPendingOrder()
            ? army.getPendingOrder().name() + " → " + army.getPendingTargetZoneId()
            : "Idle";
    armyStatusLabel.setText("Order: " + order);
    barbPayOffBtn.setVisible(false);
    barbDismissBtn.setVisible(false);
    cardLayout.show(cards, CARD_ARMY);
}

public void clearZone() { cardLayout.show(cards, CARD_EMPTY); }

public void showBarbArmy(main.barbarians.BarbArmy army,
                          main.core.GameState gameState) {
    if (army == null) { clearArmy(); return; }

    String typeLabel = switch (army.getType()) {
        case WARBOSS -> "☠ " + army.getDisplayName();
        case RAIDER  -> "☠ " + army.getDisplayName();
        case RAVAGER -> "☠ " + army.getDisplayName();
    };
    armyTitleLabel.setForeground(new java.awt.Color(220, 60, 40));
    armyTitleLabel.setText(typeLabel);

    main.map.Zone zone = gameState.getZoneManager().getZone(army.getZoneId());
    armyZoneLabel.setText("📍 " + (zone != null ? zone.getDisplayName() : army.getZoneId()));
    armySizeLabel.setText("Warriors: " + army.getSize());

        String flavour = switch (army.getType()) {
            case WARBOSS -> "The great horde — driven from their homeland by the Frost Giants. "
                    + "Destroy the Warboss to end the invasion. They do not come to conquer. "
                    + "They come because something worse follows.";
            case RAIDER  -> "Fleeing survivors. Desperate, fast-moving, and dangerous. "
                    + "They raid to survive the march south.";
            case RAVAGER -> "Heavy warbands. The last to flee — and the most organized. "
                    + "They leave ruin wherever they pass.";
        };
    armyStatusLabel.setText("<html><body style='width:160px'>" + flavour + "</body></html>");
    armyUpkeepLabel.setText("");

    // Remove old listeners
    for (java.awt.event.ActionListener al : barbPayOffBtn.getActionListeners())
        barbPayOffBtn.removeActionListener(al);
    for (java.awt.event.ActionListener al : barbDismissBtn.getActionListeners())
        barbDismissBtn.removeActionListener(al);

    // Pay-off only for ravagers, dismiss for ravagers and warboss
    barbPayOffBtn.setVisible(false);
    barbDismissBtn.setVisible(false);

    main.resources.ResourcePool res = gameState.getResources();

    boolean canPayOff = army.isRavager();
    boolean canDismiss = army.isRavager() || army.isWarboss();

    main.ledger.Ledger ledger = gameState.getLedger();

    if (canPayOff) {
        int goldCost = army.cheapPayOffGoldCost();
        int foodCost = army.cheapPayOffFoodCost();
        barbPayOffBtn.setText("Pay off  (" + goldCost + "g + " + foodCost + "f)");
        barbPayOffBtn.setEnabled(res.getMoney() >= goldCost && res.getFood() >= foodCost);
        barbPayOffBtn.setVisible(true);
        barbPayOffBtn.addActionListener(e -> {
            ledger.applyOneTime(main.resources.ResourceType.GOLD, "barbarians", "Pay Off", -goldCost, res);
            ledger.applyOneTime(main.resources.ResourceType.FOOD, "barbarians", "Pay Off", -foodCost, res);
            army.setPaidOff(true);
            barbPayOffBtn.setEnabled(false);
        });
    }

    if (canDismiss) {
        int fullCost = army.fullDismissCost();
        barbDismissBtn.setText("Dismiss  (" + fullCost + "g)");
        barbDismissBtn.setEnabled(res.getMoney() >= fullCost);
        barbDismissBtn.setVisible(true);
        barbDismissBtn.addActionListener(e -> {
            ledger.applyOneTime(main.resources.ResourceType.GOLD, "barbarians", "Dismiss", -fullCost, res);
            army.dismiss();
            clearArmy();
        });
    }

    cardLayout.show(cards, CARD_ARMY);
}

public void clearArmy() {
    armyTitleLabel.setText("");
    armyZoneLabel.setText("");
    armySizeLabel.setText("");
    armyUpkeepLabel.setText("");
    armyStatusLabel.setText("");
    commanderButton.setVisible(false);
    commanderSkillLabel.setVisible(false);
    barbPayOffBtn.setVisible(false);
    barbDismissBtn.setVisible(false);
    cardLayout.show(cards, CARD_EMPTY);
}

private JLabel makeLabel(String text, Color color, Font font) {
JLabel l = new JLabel(text);
l.setForeground(color);
l.setFont(font);
return l;
}

private JSeparator sep() {
JSeparator s = new JSeparator();
s.setForeground(UITheme.BORDER_COLOR);
return s;
}

private String capitalize(String s) {
if (s == null || s.isEmpty()) return s;
return s.charAt(0) + s.substring(1).toLowerCase();
}

private void showHouseDialog(main.nobles.NobleHouse house) {
Window parent = SwingUtilities.getWindowAncestor(this);
JDialog dialog = new JDialog(parent instanceof Frame ? (Frame) parent : null,
house.getName(), true);
dialog.setSize(420, 380);
dialog.setLocationRelativeTo(this);

JPanel content = new JPanel(new GridBagLayout());
content.setBackground(UITheme.BG_PANEL);
content.setBorder(new EmptyBorder(16, 16, 16, 16));

GridBagConstraints dc = new GridBagConstraints();
dc.gridx = 0; dc.weightx = 1.0; dc.fill = GridBagConstraints.HORIZONTAL;
dc.insets = new Insets(2, 0, 2, 0);

int row = 0;
dc.gridy = row++; content.add(makeDialogLabel(house.getName(),       UITheme.TEXT_GOLD,      UITheme.FONT_HEADER), dc);
dc.gridy = row++; content.add(makeDialogLabel(house.getLeaderName(), UITheme.TEXT_SECONDARY, UITheme.FONT_SMALL),  dc);
dc.gridy = row++; content.add(makeDialogLabel("Race: " + house.getRace().name(), UITheme.TEXT_PRIMARY, UITheme.FONT_SMALL), dc);

JTextArea personality = new JTextArea(house.getLeaderPersonality());
personality.setFont(new Font("Serif", Font.ITALIC, 12));
personality.setForeground(UITheme.TEXT_SECONDARY);
personality.setBackground(UITheme.BG_PANEL);
personality.setEditable(false);
personality.setLineWrap(true);
personality.setWrapStyleWord(true);
dc.gridy = row++; content.add(personality, dc);

dc.gridy = row++; content.add(makeDialogLabel("Opinion: " + house.getPlayerOpinion() + " / 100", opinionColor(house.getPlayerOpinion()), UITheme.FONT_BODY), dc);
        dc.gridy = row++; content.add(makeDialogLabel("Gold: "      + house.getGold(),      new Color(210,170,80),  UITheme.FONT_BODY), dc);
        dc.gridy = row++; content.add(makeDialogLabel("Food: "      + house.getFood(),      new Color(120,200,100),  UITheme.FONT_BODY), dc);
        dc.gridy = row++; content.add(makeDialogLabel("Manpower: "  + house.getManpower(),  UITheme.TEXT_PRIMARY,   UITheme.FONT_BODY), dc);dc.gridy = row++; content.add(makeDialogLabel("Influence: " + house.getInfluence(), UITheme.ACCENT_FROST,   UITheme.FONT_BODY), dc);
String capitalName = house.getCapitalZoneId() != null
? house.getCapitalZoneId().replace("_", " ") : "None";
dc.gridy = row++; content.add(makeDialogLabel("Capital: " + capitalName,
new Color(255, 210, 80), UITheme.FONT_BODY), dc);
dc.gridy = row++; content.add(makeDialogLabel("Total garrison: " + house.getTotalGarrisonSize(),
UITheme.TEXT_PRIMARY, UITheme.FONT_BODY), dc);
dc.gridy = row++; content.add(makeDialogLabel("Capital garrison: "
+ (house.getCapitalZoneId() != null
? house.getGarrisonFor(house.getCapitalZoneId()) : 0)
+ " / " + (house.getCapitalZoneId() != null
? house.getMaxGarrisonFor(house.getCapitalZoneId()) : 0),
UITheme.TEXT_PRIMARY, UITheme.FONT_BODY), dc);
int raisedTotal = nobleHouseManager.getRaisedArmyTotal(house.getId());
dc.gridy = row++; content.add(makeDialogLabel("Raised armies: " + raisedTotal,
UITheme.TEXT_PRIMARY, UITheme.FONT_BODY), dc);

StringBuilder zones = new StringBuilder("Territories: ");
for (int i = 0; i < house.getZoneIds().size(); i++) {
if (i > 0) zones.append(", ");
zones.append(house.getZoneIds().get(i).replace("_", " "));
}
JTextArea zoneArea = new JTextArea(zones.toString());
zoneArea.setFont(UITheme.FONT_SMALL);
zoneArea.setForeground(new Color(180, 200, 160));
zoneArea.setBackground(UITheme.BG_PANEL);
zoneArea.setEditable(false);
zoneArea.setLineWrap(true);
zoneArea.setWrapStyleWord(true);
dc.gridy = row++; content.add(zoneArea, dc);

dc.gridy = row; dc.weighty = 1.0; dc.fill = GridBagConstraints.BOTH;
content.add(Box.createVerticalGlue(), dc);

JButton close = new JButton("CLOSE");
close.setFont(UITheme.FONT_BUTTON);
close.setForeground(UITheme.TEXT_SECONDARY);
close.setBackground(UITheme.BUTTON_BG);
close.setBorderPainted(false);
close.setFocusPainted(false);
close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
close.addActionListener(e -> dialog.dispose());

dialog.setLayout(new BorderLayout());
dialog.add(content,                         BorderLayout.CENTER);
dialog.add(close,                           BorderLayout.SOUTH);
dialog.getContentPane().setBackground(UITheme.BG_PANEL);
dialog.setVisible(true);
}

private void showCommanderDialog(main.army.commander.Commander cmd) {
    Window parent = SwingUtilities.getWindowAncestor(this);
    JDialog dialog = new JDialog(parent instanceof Frame ? (Frame) parent : null,
            cmd.getName(), true);
    dialog.setUndecorated(true);
    dialog.setSize(340, 280);
    dialog.setLocationRelativeTo(null);

    JPanel content = new JPanel(new GridBagLayout());
    content.setBackground(UITheme.BG_PANEL);
    content.setBorder(new EmptyBorder(16, 16, 16, 16));

    GridBagConstraints dc = new GridBagConstraints();
    dc.gridx = 0; dc.weightx = 1.0; dc.fill = GridBagConstraints.HORIZONTAL;
    dc.insets = new Insets(3, 0, 3, 0);

    int row = 0;
    dc.gridy = row++; content.add(makeDialogLabel("⚔ " + cmd.getName(),
            new Color(180, 210, 255), UITheme.FONT_HEADER), dc);
    dc.gridy = row++; content.add(makeDialogLabel("Race: " + cmd.getRace(),
            UITheme.TEXT_SECONDARY, UITheme.FONT_SMALL), dc);
    dc.gridy = row++; content.add(makeDialogLabel("Affiliation: " + cmd.getPartyName(),
            UITheme.TEXT_SECONDARY, UITheme.FONT_SMALL), dc);
    dc.gridy = row++; content.add(sep(), dc);
    dc.gridy = row++; content.add(makeDialogLabel(
            "Commanding Skill: " + cmd.getCommandingSkill() + " — " + skillLabel(cmd.getCommandingSkill()),
            skillColor(cmd.getCommandingSkill()), UITheme.FONT_BODY), dc);
    dc.gridy = row++; content.add(makeDialogLabel(
            skillDescription(cmd.getCommandingSkill()),
            UITheme.TEXT_SECONDARY, UITheme.FONT_SMALL), dc);

    dc.gridy = row; dc.weighty = 1.0; dc.fill = GridBagConstraints.BOTH;
    content.add(Box.createVerticalGlue(), dc);

    JButton close = new JButton("CLOSE");
    close.setFont(UITheme.FONT_BUTTON);
    close.setForeground(UITheme.TEXT_SECONDARY);
    close.setBackground(UITheme.BUTTON_BG);
    close.setBorderPainted(false);
    close.setFocusPainted(false);
    close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    close.addActionListener(e -> dialog.dispose());

    dialog.setLayout(new BorderLayout());
    dialog.add(content, BorderLayout.CENTER);
    dialog.add(close,   BorderLayout.SOUTH);
    dialog.getContentPane().setBackground(UITheme.BG_PANEL);
    dialog.setVisible(true);
}

private JLabel makeDialogLabel(String text, Color color, Font font) {
JLabel l = new JLabel(text);
l.setFont(font);
l.setForeground(color);
return l;
}

private Color opinionColor(int v) {
if (v >= 70) return UITheme.TEXT_GREEN;
if (v <= 30) return UITheme.TEXT_RED;
return UITheme.TEXT_PRIMARY;
}

private String skillLabel(int skill) {
    return switch (skill) {
        case 1 -> "Novice";
        case 2 -> "Seasoned";
        case 3 -> "Veteran";
        case 4 -> "Legendary";
        default -> "Unknown";
    };
}

private Color skillColor(int skill) {
    return switch (skill) {
        case 1 -> UITheme.TEXT_SECONDARY;
        case 2 -> UITheme.TEXT_PRIMARY;
        case 3 -> new Color(120, 200, 100);
        case 4 -> new Color(255, 210, 80);
        default -> UITheme.TEXT_SECONDARY;
    };
}

private String skillDescription(int skill) {
    return switch (skill) {
        case 1 -> "An inexperienced commander. Troops fight at base effectiveness.";
        case 2 -> "A capable officer with battlefield experience.";
        case 3 -> "A hardened veteran who inspires soldiers to fight harder.";
        case 4 -> "A legendary warlord. Enemies falter before this commander's name.";
        default -> "";
    };
}

/**
     * Re-wires manager references after gameState.reset() (new game).
     */
    public void reinitialize(main.core.GameState gs) {
        setRavagedZoneManager(gs.getRavagedZoneManager());
        setBarbArmyManager(gs.getBarbArmyManager());
        // nobleHouseManager is final — update via field
        try {
            java.lang.reflect.Field f = MapInfoPanel.class.getDeclaredField("nobleHouseManager");
            f.setAccessible(true);
            f.set(this, gs.getNobleHouseManager());
        } catch (Exception ignored) {}
        clearZone();
        clearArmy();
    }

/**
     * Re-applies UITheme map-panel fonts to every label/button in this panel.
     * Called from MapView after the user changes the map-panel font size in Settings.
     */
    public void applyMapPanelFonts() {
        zoneTitleLabel.setFont(UITheme.FONT_MAP_HEADER);
        zoneTypeLabel.setFont(UITheme.FONT_MAP_SMALL);
        ownerButton.setFont(UITheme.FONT_MAP_SMALL);
        goldLabel.setFont(UITheme.FONT_MAP_BODY);
        foodLabel.setFont(UITheme.FONT_MAP_BODY);
        popsLabel.setFont(UITheme.FONT_MAP_BODY);
        supplyLabel.setFont(UITheme.FONT_MAP_BODY);
        damageLabel.setFont(UITheme.FONT_MAP_BODY);
        adjacentArea.setFont(UITheme.FONT_MAP_SMALL);

        armyTitleLabel.setFont(UITheme.FONT_MAP_HEADER);
        armyZoneLabel.setFont(UITheme.FONT_MAP_BODY);
        armyStatusLabel.setFont(UITheme.FONT_MAP_SMALL);
        armySizeLabel.setFont(UITheme.FONT_MAP_BODY);
        armyUpkeepLabel.setFont(UITheme.FONT_MAP_SMALL);
        commanderButton.setFont(UITheme.FONT_MAP_BUTTON);
        commanderSkillLabel.setFont(UITheme.FONT_MAP_SMALL);
        barbPayOffBtn.setFont(UITheme.FONT_MAP_BUTTON);
        barbDismissBtn.setFont(UITheme.FONT_MAP_BUTTON);

        revalidate();
        repaint();
    }

private void updateClaimsPanel(String zoneId) {
    claimsPanel.removeAll();
    java.util.List<main.nobles.NobleHouse> claimants = new java.util.ArrayList<>();
    for (main.nobles.NobleHouse h : nobleHouseManager.getHouses()) {
        if (nobleHouseManager.getClaimManager().hasClaim(h.getId(), zoneId)) {
            claimants.add(h);
        }
    }
    claimants.sort(java.util.Comparator.comparing(
            h -> GrantZoneClaimDialog.stripHousePrefix(h.getName())));

    if (claimants.isEmpty()) {
        JLabel none = makeLabel("  No claims.", UITheme.TEXT_SECONDARY, UITheme.FONT_SMALL);
        claimsPanel.add(none);
    } else {
        for (main.nobles.NobleHouse h : claimants) {
            JLabel lbl = makeLabel("  ⚑ " + GrantZoneClaimDialog.stripHousePrefix(h.getName()),
                    new Color(200, 180, 100), UITheme.FONT_SMALL);
            claimsPanel.add(lbl);
        }
    }

    // Grant claim quick button
    JButton grantBtn = new JButton("+ Grant Claim Here");
    grantBtn.setFont(UITheme.FONT_SMALL);
    grantBtn.setForeground(UITheme.TEXT_GOLD);
    grantBtn.setBackground(UITheme.BG_PANEL_LIGHT);
    grantBtn.setBorderPainted(true);
    grantBtn.setFocusPainted(false);
    grantBtn.setAlignmentX(LEFT_ALIGNMENT);
    grantBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
    grantBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    grantBtn.setMargin(new Insets(2, 6, 2, 6));
    grantBtn.addActionListener(e -> {
        if (grantClaimFromMapCallback != null) {
            grantClaimFromMapCallback.accept(zoneId, nobleHouseManager);
        }
    });
    claimsPanel.add(Box.createVerticalStrut(3));
    claimsPanel.add(grantBtn);

    claimsPanel.revalidate();
    claimsPanel.repaint();
}

public void setGrantClaimFromMapCallback(
        java.util.function.BiConsumer<String, main.nobles.NobleHouseManager> cb) {
    this.grantClaimFromMapCallback = cb;
}

}






