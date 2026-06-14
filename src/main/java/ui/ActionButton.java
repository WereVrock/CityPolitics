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

    setLayout(new BorderLayout(8, 0));
    setBackground(UITheme.BG_PANEL_LIGHT);
    setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
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
    usesLabel.setAlignmentX(CENTER_ALIGNMENT);

    button = new JButton("USE");
    button.setFont(UITheme.FONT_BUTTON);
    button.setForeground(UITheme.TEXT_GOLD);
    button.setBackground(UITheme.BUTTON_BG);
    button.setBorderPainted(false);
    button.setFocusPainted(false);
    button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

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

    // Stack uses label above USE button; let both size naturally
    JPanel rightPanel = new JPanel();
    rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
    rightPanel.setBackground(UITheme.BG_PANEL_LIGHT);
    rightPanel.setOpaque(true);
    usesLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    button.setAlignmentX(Component.CENTER_ALIGNMENT);
    rightPanel.add(Box.createVerticalGlue());
    rightPanel.add(usesLabel);
    rightPanel.add(Box.createVerticalStrut(2));
    rightPanel.add(button);
    rightPanel.add(Box.createVerticalGlue());

    add(textPanel,  BorderLayout.CENTER);
    add(rightPanel, BorderLayout.EAST);

    refresh();
}

public void refresh() {
    boolean available = action.isAvailable();

    // For formal actions, also disable if the shared vote slot is used or session pending
    boolean isFormal = action instanceof main.actions.AbstractFormalAction;
    if (isFormal) {
        boolean voteUsed = gameState.getActionRegistry().isFormalUsedThisTurn();
        boolean hasPending = gameState.hasActiveSession();
        if (voteUsed || hasPending) {
            available = false;
        }
    }

    button.setEnabled(available);
    button.setBackground(available ? UITheme.BUTTON_BG : UITheme.BUTTON_DISABLED);

    boolean isLegGated = action instanceof main.actions.WartimeTaxesAction
            || action instanceof main.actions.HireMercenariesAction
            || action instanceof main.actions.SendResourcesToNoblesAction
            || action instanceof main.actions.GrantZoneClaimAction;
    if (isFormal || isLegGated) {
        usesLabel.setText("");
    } else {
        usesLabel.setText(action.getUsesThisTurn() + "/" + action.getMaxUsesPerTurn());
    }

    if (!available) {
        String reason = null;
        if (action instanceof main.actions.WartimeTaxesAction wta) {
            reason = wta.getUnavailableReason();
        } else if (action instanceof main.actions.HireMercenariesAction) {
            reason = "Mercenary hiring not currently authorised.";
        } else if (action instanceof main.actions.AbstractFormalAction) {
            if (gameState.hasActiveSession()) {
                reason = "A vote session is already pending.";
            } else if (gameState.getActionRegistry().isFormalUsedThisTurn()) {
                reason = "Formal/legislation vote already used this turn.";
            }
        }
        button.setToolTipText(reason != null ? reason : "Not available right now.");
    } else {
        button.setToolTipText(null);
    }
}

}