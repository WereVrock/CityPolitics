package ui;

import main.core.GameState;
import main.pops.Pop;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays the current population groups.
 */
public class PopPanel extends JPanel {

    private final GameState gameState;
    private final JPanel    popListPanel;

    public PopPanel(GameState gameState) {
        this.gameState   = gameState;
        this.popListPanel = new JPanel();
        popListPanel.setLayout(new BoxLayout(popListPanel, BoxLayout.Y_AXIS));
        popListPanel.setBackground(UITheme.BG_PANEL);

        setBackground(UITheme.BG_PANEL);
        setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(1, 0, 0, 0, UITheme.BORDER_COLOR),
            new EmptyBorder(12, 12, 12, 12)
        ));
        setLayout(new BorderLayout());

        JLabel header = new JLabel("POPULATION");
        header.setFont(UITheme.FONT_HEADER);
        header.setForeground(UITheme.TEXT_GOLD);
        header.setBorder(new EmptyBorder(0, 0, 8, 0));

        add(header, BorderLayout.NORTH);
        add(popListPanel, BorderLayout.CENTER);
    }

public void refresh() {
        popListPanel.removeAll();
        List<Pop> pops = new ArrayList<>(gameState.getPopManager().getPops());
        for (Pop pop : pops) {
            JLabel label = new JLabel(pop.toString());
            label.setFont(UITheme.FONT_BODY);
            label.setForeground(UITheme.TEXT_PRIMARY);
            label.setBorder(new EmptyBorder(2, 0, 2, 0));

            String tooltip = "<html>"
                + "<b>" + pop.getType().getDisplayName() + "s</b><br>"
                + "Count: " + pop.getCount() + "<br>"
                + "View: " + pop.getAffiliation().getDisplayName() + "<br>"
                + "Food/turn: " + pop.getFoodConsumption() + "<br>"
                + "Money/turn: " + pop.getMoneyGeneration() + "<br>"
                + "Influence/turn: " + pop.getInfluenceGeneration() + "<br>"
                + "Manpower: " + pop.getManpowerContribution()
                + "</html>";
            label.setToolTipText(tooltip);

            popListPanel.add(label);
        }
        popListPanel.revalidate();
        popListPanel.repaint();
    }

}