package ui;

import main.actions.ActionResult;
import main.core.GameState;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Top-level JFrame. Assembles all panels and wires the End Turn button.
 */
public class MainWindow extends JFrame {

    private final GameState              gameState;
    private final CalendarPanel          calendarPanel;
    private final ResourcePanel          resourcePanel;
    private final PopPanel               popPanel;
    private final ActionsPanel           actionsPanel;
    private final EventLogPanel          eventLogPanel;
    private final SaveLoadDialog         saveLoadDialog;
    private final PartiesOverviewPanel   partiesOverviewPanel;
    private final VoteSessionPanel       voteSessionPanel;

    private final JPanel  centerPanel;
    private  JButton endTurnBtn;
    private  JButton partiesBtn;
    private  JButton openVoteBtn;

    public MainWindow(GameState gameState) {
        this.gameState = gameState;

        setTitle("FrostVeil");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(960, 680);
        setMinimumSize(new Dimension(800, 580));
        setLocationRelativeTo(null);

        getContentPane().setBackground(UITheme.BG_DARK);
        setLayout(new BorderLayout());

        calendarPanel        = new CalendarPanel(gameState);
        resourcePanel        = new ResourcePanel(gameState);
        popPanel             = new PopPanel(gameState);
        actionsPanel         = new ActionsPanel(gameState, this::handleActionResult);
        eventLogPanel        = new EventLogPanel();
        saveLoadDialog       = new SaveLoadDialog(this, gameState, eventLogPanel::appendLine);
        partiesOverviewPanel = new PartiesOverviewPanel(gameState, this::showMainView);
        voteSessionPanel     = new VoteSessionPanel(gameState, this::onVoteFinalized, this::swapCenter);

        // Left sidebar
        JPanel leftSidebar = new JPanel(new BorderLayout());
        leftSidebar.setBackground(UITheme.BG_PANEL);
        leftSidebar.setPreferredSize(new Dimension(230, 0));
        leftSidebar.add(resourcePanel, BorderLayout.CENTER);
        leftSidebar.add(popPanel,      BorderLayout.SOUTH);

        // Center panel (swappable)
        centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(UITheme.BG_DARK);

        JPanel actionsWrapper = new JPanel(new BorderLayout());
        actionsWrapper.setBackground(UITheme.BG_DARK);
        JPanel southStack = new JPanel(new BorderLayout());
        southStack.setBackground(UITheme.BG_DARK);
        southStack.add(buildSaveLoadBar(), BorderLayout.NORTH);
        southStack.add(buildBottomBar(),   BorderLayout.SOUTH);
        actionsWrapper.add(actionsPanel, BorderLayout.CENTER);
        actionsWrapper.add(southStack,   BorderLayout.SOUTH);

        centerPanel.add(actionsWrapper, BorderLayout.CENTER);

        add(calendarPanel, BorderLayout.NORTH);
        add(leftSidebar,   BorderLayout.WEST);
        add(centerPanel,   BorderLayout.CENTER);
        add(eventLogPanel, BorderLayout.SOUTH);

        refreshAll();
        eventLogPanel.appendLine("=== FrostVeil begins. " + gameState.getCalendar().getDisplayString() + " ===");
        eventLogPanel.appendLine("The realm awaits your guidance. The Frost Giants stir in the north.");
    }

private JPanel buildSaveLoadBar() {
        JButton newBtn  = buildBarButton("NEW");
        JButton saveBtn = buildBarButton("SAVE");
        JButton loadBtn = buildBarButton("LOAD");

        newBtn.addActionListener(e  -> saveLoadDialog.newGame(() -> { showMainView(); resetLogs(); }));
        saveBtn.addActionListener(e -> saveLoadDialog.save());
        loadBtn.addActionListener(e -> saveLoadDialog.load(this::refreshAll));

        JPanel bar = new JPanel(new GridLayout(1, 3, 6, 0));
        bar.setBackground(UITheme.BG_DARK);
        bar.setBorder(new EmptyBorder(0, 12, 4, 12));
        bar.add(newBtn);
        bar.add(saveBtn);
        bar.add(loadBtn);
        return bar;
    }

private JButton buildBarButton(String label) {
        JButton btn = new JButton(label);
        btn.setFont(UITheme.FONT_BUTTON);
        btn.setForeground(UITheme.TEXT_SECONDARY);
        btn.setBackground(UITheme.BG_PANEL);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(0, 28));
        return btn;
    }

    private JPanel buildBottomBar() {
        endTurnBtn = new JButton("END TURN  ▶");
        endTurnBtn.setFont(new Font("Serif", Font.BOLD, 15));
        endTurnBtn.setForeground(UITheme.ACCENT_FROST);
        endTurnBtn.setBackground(new Color(25, 45, 65));
        endTurnBtn.setBorderPainted(false);
        endTurnBtn.setFocusPainted(false);
        endTurnBtn.setPreferredSize(new Dimension(0, 48));
        endTurnBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        endTurnBtn.addActionListener(e -> endTurn());

        partiesBtn = new JButton("PARTIES");
        partiesBtn.setFont(UITheme.FONT_BUTTON);
        partiesBtn.setForeground(UITheme.TEXT_SECONDARY);
        partiesBtn.setBackground(UITheme.BUTTON_BG);
        partiesBtn.setBorderPainted(false);
        partiesBtn.setFocusPainted(false);
        partiesBtn.setPreferredSize(new Dimension(90, 48));
        partiesBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        partiesBtn.addActionListener(e -> showPartiesView());

        openVoteBtn = new JButton("⚑ OPEN VOTE");
        openVoteBtn.setFont(UITheme.FONT_BUTTON);
        openVoteBtn.setForeground(UITheme.TEXT_GOLD);
        openVoteBtn.setBackground(new Color(60, 40, 20));
        openVoteBtn.setBorderPainted(false);
        openVoteBtn.setFocusPainted(false);
        openVoteBtn.setPreferredSize(new Dimension(110, 48));
        openVoteBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        openVoteBtn.setVisible(false);
        openVoteBtn.addActionListener(e -> showVoteSession());

        JPanel wrapper = new JPanel(new BorderLayout(6, 0));
        wrapper.setBackground(UITheme.BG_DARK);
        wrapper.setBorder(new EmptyBorder(8, 12, 8, 12));

        JPanel leftBtns = new JPanel(new BorderLayout(4, 0));
        leftBtns.setBackground(UITheme.BG_DARK);
        leftBtns.add(partiesBtn,  BorderLayout.WEST);
        leftBtns.add(openVoteBtn, BorderLayout.EAST);

        wrapper.add(leftBtns,   BorderLayout.WEST);
        wrapper.add(endTurnBtn, BorderLayout.CENTER);
        return wrapper;
    }

    private void swapCenter(JPanel panel) {
        centerPanel.removeAll();
        centerPanel.add(panel, BorderLayout.CENTER);
        centerPanel.revalidate();
        centerPanel.repaint();
    }

    private void showMainView() {
        JPanel actionsWrapper = new JPanel(new BorderLayout());
        actionsWrapper.setBackground(UITheme.BG_DARK);
        JPanel southStack = new JPanel(new BorderLayout());
        southStack.setBackground(UITheme.BG_DARK);
        southStack.add(buildSaveLoadBar(), BorderLayout.NORTH);
        southStack.add(buildBottomBar(),   BorderLayout.SOUTH);
        actionsWrapper.add(actionsPanel, BorderLayout.CENTER);
        actionsWrapper.add(southStack,   BorderLayout.SOUTH);
        swapCenter(actionsWrapper);
        refreshAll();
        updateEndTurnState();
    }

    private void showPartiesView() {
        partiesOverviewPanel.refresh();
        swapCenter(partiesOverviewPanel);
    }

    private void showVoteSession() {
        voteSessionPanel.refresh();
        swapCenter(voteSessionPanel);
    }

    private void onVoteFinalized() {
        if (gameState.hasActiveSession()) {
            eventLogPanel.appendLine("↩ Returned to main view. Vote session still pending.");
        } else {
            eventLogPanel.appendLine("✓ Vote finalized.");
        }
        showMainView();
        refreshAll();
        updateEndTurnState();
    }

    private void updateEndTurnState() {
        boolean blocked = gameState.hasActiveSession();
        endTurnBtn.setEnabled(!blocked);
        endTurnBtn.setBackground(blocked ? UITheme.BUTTON_DISABLED : new Color(25, 45, 65));
        endTurnBtn.setText(blocked ? "VOTE PENDING  ⚠" : "END TURN  ▶");
        partiesBtn.setVisible(!blocked);
        openVoteBtn.setVisible(blocked);
    }

    private void endTurn() {
        List<String> log = gameState.getTurnProcessor().processTurn(
            gameState.getResources(),
            gameState.getStats(),
            gameState.getPopManager(),
            gameState.getCalendar(),
            gameState.getActionRegistry(),
            gameState.getEffectManager()
        );
        eventLogPanel.appendLines(log);
        refreshAll();

        if (gameState.getCalendar().isFrostGiantYear()) {
            eventLogPanel.appendLine("⚠  THE FROST GIANTS HAVE ARRIVED. THE REALM TREMBLES.");
        }
    }

    private void handleActionResult(ActionResult result) {
        if (result.isPending()) {
            eventLogPanel.appendLine("⚑ " + result.getMessage());
            showVoteSession();
            updateEndTurnState();
            return;
        }
        eventLogPanel.appendLine((result.isSuccess() ? "✓ " : "✗ ") + result.getMessage());
        refreshAll();
    }

    private void refreshEndTurn() {
        updateEndTurnState();
    }

    public void resetLogs() {
        eventLogPanel.clear();
        eventLogPanel.appendLine("=== New game started. " + gameState.getCalendar().getDisplayString() + " ===");
        eventLogPanel.appendLine("The realm awaits your guidance. The Frost Giants stir in the north.");
    }

    private void refreshAll() {
        calendarPanel.refresh();
        resourcePanel.refresh();
        popPanel.refresh();
        actionsPanel.refresh();
    }
}