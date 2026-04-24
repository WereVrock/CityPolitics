package ui;

import main.core.GameState;
import main.pops.PopManager;
import main.resources.ResourcePool;
import main.resources.StatBlock;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import main.parameters.GameParameters;

/**
 * Displays current resources and stats.
 */
public class ResourcePanel extends JPanel {

    private final GameState gameState;

    private JLabel foodLabel;
    private JLabel moneyLabel;
    private JLabel manpowerLabel;
    private JLabel influenceLabel;
    private JLabel corruptionLabel;
    private JLabel happinessLabel;
    private JLabel foodDeltaLabel;
    private JLabel moneyDeltaLabel;
    private JLabel influenceDeltaLabel;

    public ResourcePanel(GameState gameState) {
        this.gameState = gameState;
        setBackground(UITheme.BG_PANEL);
        setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 0, 1, UITheme.BORDER_COLOR),
            new EmptyBorder(12, 12, 12, 12)
        ));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        buildUI();
    }

    private void buildUI() {
        add(makeHeader("REALM RESOURCES"));
        add(Box.createVerticalStrut(8));

        foodLabel      = makeResourceLabel("Food");
        moneyLabel     = makeResourceLabel("Money");
        manpowerLabel  = makeResourceLabel("Manpower");
        influenceLabel = makeResourceLabel("Influence");

        foodDeltaLabel      = makeDeltaLabel();
        moneyDeltaLabel     = makeDeltaLabel();
        influenceDeltaLabel = makeDeltaLabel();

        add(makeResourceRow(foodLabel, foodDeltaLabel));
        add(makeResourceRow(moneyLabel, moneyDeltaLabel));
        add(makeResourceRow(manpowerLabel, new JLabel()));
        foodLabel.setToolTipText("Food consumed each turn by your population. Runs out → starvation.");
        moneyLabel.setToolTipText("Money generated each turn. All costs scale with corruption.");
        manpowerLabel.setToolTipText("Military strength contributed by your population.");
        influenceLabel.setToolTipText("Political capital generated each turn. Used for formal actions.");

        add(makeResourceRow(influenceLabel, influenceDeltaLabel));

        add(Box.createVerticalStrut(16));
        add(makeHeader("REALM STATS"));
        add(Box.createVerticalStrut(8));

        corruptionLabel = makeStatLabel("Corruption", UITheme.TEXT_RED);
        happinessLabel  = makeStatLabel("Happiness",  UITheme.TEXT_GREEN);

        corruptionLabel.setToolTipText("Raises all action costs. Reduces effective happiness.");
        happinessLabel.setToolTipText("Effective happiness = base − (corruption × 0.3). Decays each turn.");

        add(corruptionLabel);
        add(Box.createVerticalStrut(4));
        add(happinessLabel);

        add(Box.createVerticalGlue());
    }

    private JLabel makeHeader(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UITheme.FONT_HEADER);
        label.setForeground(UITheme.TEXT_GOLD);
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private JLabel makeResourceLabel(String name) {
        JLabel label = new JLabel(name + ": --");
        label.setFont(UITheme.FONT_BODY);
        label.setForeground(UITheme.TEXT_PRIMARY);
        return label;
    }

    private JLabel makeDeltaLabel() {
        JLabel label = new JLabel("");
        label.setFont(UITheme.FONT_SMALL);
        label.setForeground(UITheme.TEXT_SECONDARY);
        return label;
    }

    private JLabel makeStatLabel(String name, Color color) {
        JLabel label = new JLabel(name + ": --");
        label.setFont(UITheme.FONT_BODY);
        label.setForeground(color);
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private JPanel makeResourceRow(JLabel valueLabel, JLabel deltaLabel) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(UITheme.BG_PANEL);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.add(valueLabel, BorderLayout.WEST);
        row.add(deltaLabel, BorderLayout.EAST);
        return row;
    }

public void refresh() {
        ResourcePool res   = gameState.getResources();
        StatBlock    stats = gameState.getStats();
        PopManager   pops  = gameState.getPopManager();

        int corruption     = stats.getCorruption();
        int baseHappiness  = stats.getHappiness();
        int effectiveHappy = (int) Math.max(0,
            baseHappiness - corruption * GameParameters.CORRUPTION_HAPPINESS_MALUS);

        foodLabel.setText("Food:      " + res.getFood());
        moneyLabel.setText("Money:     " + res.getMoney());
        manpowerLabel.setText("Manpower:  " + res.getManpower());
        influenceLabel.setText("Influence: " + res.getInfluence());

        foodDeltaLabel.setText("-" + pops.getTotalFoodConsumption() + "/turn");
        moneyDeltaLabel.setText("+" + pops.getTotalMoneyGeneration() + "/turn");
        influenceDeltaLabel.setText("+" + pops.getTotalInfluenceGeneration() + "/turn");

        corruptionLabel.setText("Corruption: " + corruption + " / 100");
        happinessLabel.setText("Happiness:  " + effectiveHappy
            + " / 100  (base " + baseHappiness + ")");
    }

}