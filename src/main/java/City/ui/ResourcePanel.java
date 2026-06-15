package City.ui;

import City.main.core.GameState;
import City.main.parameters.ActionParams;
import City.main.pops.PopManager;
import City.main.resources.ResourcePool;
import City.main.resources.StatBlock;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
 

/**
 * Displays current resources and stats.
 */
public class ResourcePanel extends JPanel {

    private final GameState gameState;

    private JLabel foodLabel;
    private JLabel moneyLabel;
    private JLabel manpowerLabel;
    private JLabel influenceLabel;
    private JLabel trustLabel;
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
        trustLabel     = makeResourceLabel("Trust");

        foodDeltaLabel      = makeDeltaLabel();
        moneyDeltaLabel     = makeDeltaLabel();
        influenceDeltaLabel = makeDeltaLabel();

        add(makeResourceRow(foodLabel, foodDeltaLabel));
        add(makeResourceRow(moneyLabel, moneyDeltaLabel));
        add(makeResourceRow(manpowerLabel, new JLabel()));
        foodLabel.setToolTipText("Food consumed each turn by your population.");
        moneyLabel.setToolTipText("Money generated each turn. All costs scale with corruption.");
        manpowerLabel.setToolTipText("Military strength contributed by your population.");
        influenceLabel.setToolTipText("Political capital generated each turn.");
        add(makeResourceRow(influenceLabel, influenceDeltaLabel));
        add(makeResourceRow(trustLabel, new JLabel()));
        trustLabel.setToolTipText("<html>Trust: 0–10. Affects realm council impression bonus: (Trust−5)×100.<br>"
                + "Lost by joining unjustified battles. Gained over time.</html>");

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

        int corruption     = stats.getCorruption();
        int baseHappiness  = stats.getHappiness();
        int effectiveHappy = (int) Math.max(0,
            baseHappiness - corruption * City.main.parameters.ActionParams.CORRUPTION_HAPPINESS_MALUS);

        foodLabel.setText("Food:      " + res.getFood());
        moneyLabel.setText("Money:     " + res.getMoney());
        manpowerLabel.setText("Manpower:  " + res.getManpower());
        influenceLabel.setText("Influence: " + res.getInfluence());

        int trust = gameState.getPlayerPrestige().getTrust();
        Color trustColor = trust >= 7 ? UITheme.TEXT_GREEN
                         : trust <= 3 ? UITheme.TEXT_RED
                         : UITheme.TEXT_PRIMARY;
        trustLabel.setText("Trust:     " + trust + " / 10");
        trustLabel.setForeground(trustColor);

        City.main.ledger.Ledger ledger = gameState.getLedger();
        int deltaFood      = ledger.getDelta(City.main.resources.ResourceType.FOOD);
        int deltaGold      = ledger.getDelta(City.main.resources.ResourceType.GOLD);
        int deltaInfluence = ledger.getDelta(City.main.resources.ResourceType.INFLUENCE);

        setDeltaLabel(foodDeltaLabel,      deltaFood);
        setDeltaLabel(moneyDeltaLabel,     deltaGold);
        setDeltaLabel(influenceDeltaLabel, deltaInfluence);

        foodDeltaLabel.setToolTipText(buildTooltip(ledger, City.main.resources.ResourceType.FOOD));
        moneyDeltaLabel.setToolTipText(buildTooltip(ledger, City.main.resources.ResourceType.GOLD));
        influenceDeltaLabel.setToolTipText(buildTooltip(ledger, City.main.resources.ResourceType.INFLUENCE));

        corruptionLabel.setText("Corruption: " + corruption + " / 100");
        happinessLabel.setText("Happiness:  " + effectiveHappy
            + " / 100  (base " + baseHappiness + ")");
    }

private void setDeltaLabel(JLabel label, int delta) {
        label.setText((delta >= 0 ? "+" : "") + delta + "/turn");
        label.setForeground(delta >= 0 ? UITheme.TEXT_GREEN : UITheme.TEXT_RED);
    }

    private String buildTooltip(City.main.ledger.Ledger ledger, City.main.resources.ResourceType resource) {
        java.util.List<City.main.ledger.Ledger.Entry> entries = ledger.getRecurringEntries(resource);
        if (entries.isEmpty()) return "No contributions.";
        StringBuilder sb = new StringBuilder("<html>");
        for (City.main.ledger.Ledger.Entry e : entries) {
            sb.append(e.category).append(" / ").append(e.name)
              .append(": ").append(e.amount >= 0 ? "+" : "").append(e.amount).append("<br>");
        }
        sb.append("</html>");
        return sb.toString();
    }

}