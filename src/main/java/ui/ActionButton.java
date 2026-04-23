package ui;

import main.actions.ActionResult;
import main.actions.PlayerAction;
import main.core.GameState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * A single themed button bound to one PlayerAction.
 * Calls back with the ActionResult so the parent can log it.
 */
public class ActionButton extends JPanel {

    private final GameState          gameState;
    private final PlayerAction       action;
    private final Consumer<ActionResult> onResult;

    private final JButton  button;
    private final JLabel   usesLabel;

    public ActionButton(GameState gameState, PlayerAction action, Consumer<ActionResult> onResult) {
        this.gameState = gameState;
        this.action    = action;
        this.onResult  = onResult;

        setLayout(new BorderLayout(6, 0));
        setBackground(UITheme.BG_PANEL_LIGHT);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBackground(UITheme.BG_PANEL_LIGHT);

        JLabel nameLabel = new JLabel(action.getName());
        nameLabel.setFont(UITheme.FONT_BUTTON);
        nameLabel.setForeground(UITheme.TEXT_PRIMARY);

        JLabel descLabel = new JLabel(action.getDescription());
        descLabel.setFont(UITheme.FONT_SMALL);
        descLabel.setForeground(UITheme.TEXT_SECONDARY);

        textPanel.add(nameLabel);
        textPanel.add(descLabel);

        usesLabel = new JLabel();
        usesLabel.setFont(UITheme.FONT_SMALL);
        usesLabel.setForeground(UITheme.TEXT_SECONDARY);

        button = new JButton("USE");
        button.setFont(UITheme.FONT_BUTTON);
        button.setForeground(UITheme.TEXT_GOLD);
        button.setBackground(UITheme.BUTTON_BG);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(60, 36));

        button.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) button.setBackground(UITheme.BUTTON_HOVER);
            }
            @Override public void mouseExited(MouseEvent e) {
                button.setBackground(button.isEnabled() ? UITheme.BUTTON_BG : UITheme.BUTTON_DISABLED);
            }
        });

        button.addActionListener(e -> {
            ActionResult result = action.execute(gameState.getResources(), gameState.getStats());
            onResult.accept(result);
            refresh();
        });

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(UITheme.BG_PANEL_LIGHT);
        rightPanel.add(usesLabel, BorderLayout.NORTH);
        rightPanel.add(button,    BorderLayout.SOUTH);

        add(textPanel,  BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        refresh();
    }

    public void refresh() {
        boolean available = action.isAvailable();
        button.setEnabled(available);
        button.setBackground(available ? UITheme.BUTTON_BG : UITheme.BUTTON_DISABLED);
        usesLabel.setText(action.getUsesThisTurn() + "/" + action.getMaxUsesPerTurn());
    }
}