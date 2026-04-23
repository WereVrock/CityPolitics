package ui;

import main.actions.ActionResult;
import main.actions.PlayerAction;
import main.core.GameState;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Scrollable panel listing all available player actions as ActionButtons.
 */
public class ActionsPanel extends JPanel {

    private final GameState              gameState;
    private final Consumer<ActionResult> onResult;
    private final List<ActionButton>     actionButtons = new ArrayList<>();
    private final JPanel                 buttonContainer;

    public ActionsPanel(GameState gameState, Consumer<ActionResult> onResult) {
        this.gameState = gameState;
        this.onResult  = onResult;

        setBackground(UITheme.BG_DARK);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12, 12, 4, 12));

        JLabel header = new JLabel("ACTIONS");
        header.setFont(UITheme.FONT_HEADER);
        header.setForeground(UITheme.TEXT_GOLD);
        header.setBorder(new EmptyBorder(0, 0, 8, 0));

        buttonContainer = new JPanel();
        buttonContainer.setLayout(new BoxLayout(buttonContainer, BoxLayout.Y_AXIS));
        buttonContainer.setBackground(UITheme.BG_DARK);

        buildActionButtons();

        JScrollPane scrollPane = new JScrollPane(buttonContainer);
        scrollPane.setBorder(null);
        scrollPane.setBackground(UITheme.BG_DARK);
        scrollPane.getViewport().setBackground(UITheme.BG_DARK);
        scrollPane.getVerticalScrollBar().setBackground(UITheme.BG_PANEL);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        add(header,     BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void buildActionButtons() {
        for (PlayerAction action : gameState.getActionRegistry().getActions()) {
            ActionButton btn = new ActionButton(gameState, action, result -> {
                onResult.accept(result);
                refresh();
            });
            btn.setAlignmentX(LEFT_ALIGNMENT);
            actionButtons.add(btn);
            buttonContainer.add(btn);
            buttonContainer.add(Box.createVerticalStrut(6));
        }
    }

    public void refresh() {
        for (ActionButton btn : actionButtons) {
            btn.refresh();
        }
    }
}