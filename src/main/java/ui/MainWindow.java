package ui;

import main.actions.ActionResult;
import main.core.GameState;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Top-level JFrame. Assembles all panels and wires the End Turn button.
 */
public class MainWindow extends JFrame {

    private final GameState     gameState;
    private final CalendarPanel calendarPanel;
    private final ResourcePanel resourcePanel;
    private final PopPanel      popPanel;
    private final ActionsPanel  actionsPanel;
    private final EventLogPanel eventLogPanel;

    public MainWindow(GameState gameState) {
        this.gameState = gameState;

        setTitle("FrostVeil");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(960, 680);
        setMinimumSize(new Dimension(800, 580));
        setLocationRelativeTo(null);

        getContentPane().setBackground(UITheme.BG_DARK);
        setLayout(new BorderLayout());

        calendarPanel  = new CalendarPanel(gameState);
        resourcePanel  = new ResourcePanel(gameState);
        popPanel       = new PopPanel(gameState);
        actionsPanel   = new ActionsPanel(gameState, this::handleActionResult);
        eventLogPanel  = new EventLogPanel();

        // Left sidebar: resources + pops
        JPanel leftSidebar = new JPanel(new BorderLayout());
        leftSidebar.setBackground(UITheme.BG_PANEL);
        leftSidebar.setPreferredSize(new Dimension(230, 0));
        leftSidebar.add(resourcePanel, BorderLayout.CENTER);
        leftSidebar.add(popPanel,      BorderLayout.SOUTH);

        // Center: actions + end turn
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(UITheme.BG_DARK);
        centerPanel.add(actionsPanel, BorderLayout.CENTER);
        centerPanel.add(buildEndTurnButton(), BorderLayout.SOUTH);

        add(calendarPanel, BorderLayout.NORTH);
        add(leftSidebar,   BorderLayout.WEST);
        add(centerPanel,   BorderLayout.CENTER);
        add(eventLogPanel, BorderLayout.SOUTH);

        refreshAll();
        eventLogPanel.appendLine("=== FrostVeil begins. " + gameState.getCalendar().getDisplayString() + " ===");
        eventLogPanel.appendLine("The realm awaits your guidance. The Frost Giants stir in the north.");
    }

    private JPanel buildEndTurnButton() {
        JButton endTurnBtn = new JButton("END TURN  ▶");
        endTurnBtn.setFont(new Font("Serif", Font.BOLD, 15));
        endTurnBtn.setForeground(UITheme.ACCENT_FROST);
        endTurnBtn.setBackground(new Color(25, 45, 65));
        endTurnBtn.setBorderPainted(false);
        endTurnBtn.setFocusPainted(false);
        endTurnBtn.setPreferredSize(new Dimension(0, 48));
        endTurnBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        endTurnBtn.addActionListener(e -> endTurn());

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(UITheme.BG_DARK);
        wrapper.setBorder(new EmptyBorder(8, 12, 8, 12));
        wrapper.add(endTurnBtn, BorderLayout.CENTER);
        return wrapper;
    }

    private void endTurn() {
        List<String> log = gameState.getTurnProcessor().processTurn(
            gameState.getResources(),
            gameState.getStats(),
            gameState.getPopManager(),
            gameState.getCalendar(),
            gameState.getActionRegistry()
        );
        eventLogPanel.appendLines(log);
        refreshAll();

        if (gameState.getCalendar().isFrostGiantYear()) {
            eventLogPanel.appendLine("⚠  THE FROST GIANTS HAVE ARRIVED. THE REALM TREMBLES.");
        }
    }

    private void handleActionResult(ActionResult result) {
        eventLogPanel.appendLine((result.isSuccess() ? "✓ " : "✗ ") + result.getMessage());
        refreshAll();
    }

    private void refreshAll() {
        calendarPanel.refresh();
        resourcePanel.refresh();
        popPanel.refresh();
        actionsPanel.refresh();
    }
}