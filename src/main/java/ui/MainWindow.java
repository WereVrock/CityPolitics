package ui;

import ui.politics.PartiesOverviewPanel;
import ui.map.MapView;
import ui.politics.VoteSessionPanel;
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
    private final MapView                mapView;

    private final JPanel  centerPanel;
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
        calendarPanel.setEndTurnCallback(this::endTurn);
        calendarPanel.setBlockedSupplier(() -> gameState.hasActiveSession());
        resourcePanel        = new ResourcePanel(gameState);
        popPanel             = new PopPanel(gameState);
        actionsPanel         = new ActionsPanel(gameState, this::handleActionResult);
        eventLogPanel        = new EventLogPanel();
        saveLoadDialog       = new SaveLoadDialog(this, gameState, eventLogPanel::appendLine);
        partiesOverviewPanel = new PartiesOverviewPanel(gameState, this::showMainView);
        voteSessionPanel     = new VoteSessionPanel(gameState, this::onVoteFinalized, this::swapCenter);
        mapView              = new MapView(gameState, this::showMainView);

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
        loadBtn.addActionListener(e -> saveLoadDialog.load(() -> {
            showMainView();
            if (gameState.hasActiveSession()) showVoteSession();
            updateEndTurnState();
            resetLogs();
            eventLogPanel.appendLine("Game loaded.");
        }));

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
        partiesBtn = new JButton("PARTIES");
        partiesBtn.setFont(UITheme.FONT_BUTTON);
        partiesBtn.setForeground(UITheme.TEXT_SECONDARY);
        partiesBtn.setBackground(UITheme.BUTTON_BG);
        partiesBtn.setBorderPainted(false);
        partiesBtn.setFocusPainted(false);
        partiesBtn.setPreferredSize(new Dimension(90, 48));
        partiesBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        partiesBtn.addActionListener(e -> showPartiesView());

        JButton mapBtn = new JButton("MAP");
        mapBtn.setFont(UITheme.FONT_BUTTON);
        mapBtn.setForeground(UITheme.TEXT_SECONDARY);
        mapBtn.setBackground(UITheme.BUTTON_BG);
        mapBtn.setBorderPainted(false);
        mapBtn.setFocusPainted(false);
        mapBtn.setPreferredSize(new Dimension(60, 48));
        mapBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        mapBtn.addActionListener(e -> showMapView());

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

        JPanel leftBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        leftBtns.setBackground(UITheme.BG_DARK);
        leftBtns.add(partiesBtn);
        leftBtns.add(mapBtn);
        leftBtns.add(openVoteBtn);

        wrapper.add(leftBtns, BorderLayout.WEST);
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

    private void showMapView() {
        mapView.refresh();
        swapCenter(mapView);
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
        boolean blocked       = gameState.hasActiveSession();
        calendarPanel.updateEndTurnState(blocked, blocked);
        partiesBtn.setVisible(!blocked);
        openVoteBtn.setVisible(blocked);
    }

private void endTurn() {
        long t0 = System.currentTimeMillis();
        List<String> log = gameState.getTurnProcessor().processTurn(
            gameState,
            gameState.getResources(),
            gameState.getStats(),
            gameState.getPopManager(),
            gameState.getCalendar(),
            gameState.getActionRegistry(),
            gameState.getEffectManager()
        );
        long t1 = System.currentTimeMillis();
        eventLogPanel.appendLines(log);
        long t2 = System.currentTimeMillis();
        calendarPanel.refresh();
        long t3 = System.currentTimeMillis();
        resourcePanel.refresh();
        long t4 = System.currentTimeMillis();
        popPanel.refresh();
        long t5 = System.currentTimeMillis();
        actionsPanel.refresh();
        long t6 = System.currentTimeMillis();

        System.out.println("=== END TURN TIMING ===");
        System.out.println("processTurn:      " + (t1-t0) + "ms");
        System.out.println("appendLines:      " + (t2-t1) + "ms");
        System.out.println("calendarPanel:    " + (t3-t2) + "ms");
        System.out.println("resourcePanel:    " + (t4-t3) + "ms");
        System.out.println("popPanel:         " + (t5-t4) + "ms");
        System.out.println("actionsPanel:     " + (t6-t5) + "ms");
        System.out.println("TOTAL:            " + (t6-t0) + "ms");

        // Force repaint of map if it is currently visible
        if (centerPanel.getComponentCount() > 0 && centerPanel.getComponent(0) == mapView) {
            mapView.repaint();
        }

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