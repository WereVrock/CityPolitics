package ui;

import ui.politics.PartiesOverviewPanel;
import ui.map.MapView;
import ui.politics.VoteSessionPanel;
import main.actions.ActionResult;
import main.core.GameState;
import main.Main;

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
    private final ui.nobles.NobleHousesPanel nobleHousesPanel;
    private final VoteSessionPanel       voteSessionPanel;
    private final MapView                mapView;

    // version & build info
    private String buildNo = "?";
    private String buildTime = "?";
    private boolean buildInfoLoaded = false;
    private JLabel versionLabel;

    private final JPanel  centerPanel;
    private  JButton partiesBtn;
    private  JButton openVoteBtn;
    private ui.ledger.LedgerPanel ledgerPanel;
    private JDialog               ledgerDialog;

    public MainWindow(GameState gameState) {
        this.gameState = gameState;
        loadBuildInfo();
        rewireCallbacks();

        gameState.getTurnProcessor().setPayOffDialogSupplier((army, resources, zoneId, owner, playerArmies, nobleArmies, nobleGarrison) -> {
            java.awt.Window win = this;
            final boolean[] result = {false};
            if (javax.swing.SwingUtilities.isEventDispatchThread()) {
                result[0] = ui.barbarians.BarbPayOffDialog.show(army, resources, win,
                        zoneId, owner, playerArmies, nobleArmies, nobleGarrison);
            } else {
                try {
                    javax.swing.SwingUtilities.invokeAndWait(() ->
                        result[0] = ui.barbarians.BarbPayOffDialog.show(army, resources, win,
                                zoneId, owner, playerArmies, nobleArmies, nobleGarrison));
                } catch (Exception ignored) {}
            }
            return result[0];
        });

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
        nobleHousesPanel     = new ui.nobles.NobleHousesPanel(gameState, this::showMainView);
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

        // Build bottom area: event log + status bar (version info)
        JPanel bottomArea = new JPanel(new BorderLayout());
        bottomArea.setBackground(UITheme.BG_DARK);
        bottomArea.add(eventLogPanel, BorderLayout.CENTER);
        bottomArea.add(createStatusBar(), BorderLayout.SOUTH);
        add(bottomArea, BorderLayout.SOUTH);

        refreshAll();
        eventLogPanel.appendLine("=== FrostVeil begins. " + gameState.getCalendar().getDisplayString() + " ===");
        eventLogPanel.appendLine("The realm awaits your guidance. The Frost Giants stir in the north.");
    }

    private JPanel buildSaveLoadBar() {
        JButton newBtn  = buildBarButton("NEW");
        JButton saveBtn = buildBarButton("SAVE");
        JButton loadBtn = buildBarButton("LOAD");

        newBtn.addActionListener(e  -> saveLoadDialog.newGame(() -> {
            mapView.reinitialize(gameState);
            rewireCallbacks();
            showMainView();
            resetLogs();
        }));
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

        JButton noblesBtn = new JButton("NOBLES");
        noblesBtn.setFont(UITheme.FONT_BUTTON);
        noblesBtn.setForeground(UITheme.TEXT_SECONDARY);
        noblesBtn.setBackground(UITheme.BUTTON_BG);
        noblesBtn.setBorderPainted(false);
        noblesBtn.setFocusPainted(false);
        noblesBtn.setPreferredSize(new Dimension(100, 48));
        noblesBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        noblesBtn.addActionListener(e -> showNoblesView());

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

        JButton ledgerBtn = new JButton("LEDGER");
        ledgerBtn.setFont(UITheme.FONT_BUTTON);
        ledgerBtn.setForeground(UITheme.TEXT_SECONDARY);
        ledgerBtn.setBackground(UITheme.BUTTON_BG);
        ledgerBtn.setBorderPainted(false);
        ledgerBtn.setFocusPainted(false);
        ledgerBtn.setPreferredSize(new Dimension(90, 48));
        ledgerBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        ledgerBtn.addActionListener(e -> showLedger());
      

        JPanel wrapper = new JPanel(new BorderLayout(6, 0));
        wrapper.setBackground(UITheme.BG_DARK);
        wrapper.setBorder(new EmptyBorder(8, 12, 8, 12));

        JPanel leftBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        leftBtns.setBackground(UITheme.BG_DARK);
        leftBtns.add(partiesBtn);
        leftBtns.add(noblesBtn);
        leftBtns.add(mapBtn);
        leftBtns.add(openVoteBtn);
  leftBtns.add(ledgerBtn);
  
        wrapper.add(leftBtns, BorderLayout.WEST);
        return wrapper;
    }

private void showLedger() {
        if (ledgerDialog == null) {
            ledgerPanel  = new ui.ledger.LedgerPanel(
                    gameState.getLedger(), gameState.getResources());
            ledgerDialog = new JDialog(this, "Ledger", false);
            ledgerDialog.setSize(560, 600);
            ledgerDialog.setMinimumSize(new Dimension(420, 440));
            ledgerDialog.setLocationRelativeTo(this);
            ledgerDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
            ledgerDialog.getContentPane().setBackground(UITheme.BG_DARK);
            ledgerDialog.getContentPane().setLayout(new BorderLayout());

            JLabel title = new JLabel("  LEDGER");
            title.setFont(UITheme.FONT_TITLE);
            title.setForeground(UITheme.TEXT_GOLD);
            title.setBorder(new javax.swing.border.EmptyBorder(10, 12, 8, 12));
            title.setOpaque(true);
            title.setBackground(UITheme.BG_PANEL);
            ledgerDialog.getContentPane().add(title,       BorderLayout.NORTH);
            ledgerDialog.getContentPane().add(ledgerPanel, BorderLayout.CENTER);

            gameState.getTurnProcessor().setOnSnapshotRequested(
                    () -> ledgerPanel.captureSnapshot());
        }
        ledgerPanel.refresh();
        ledgerDialog.setVisible(true);
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

    private void showNoblesView() {
        nobleHousesPanel.refresh();
        swapCenter(nobleHousesPanel);
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
        List<String> log = gameState.getTurnProcessor().processTurn(
            gameState,
            gameState.getResources(),
            gameState.getStats(),
            gameState.getPopManager(),
            gameState.getCalendar(),
            gameState.getActionRegistry(),
            gameState.getEffectManager(),
            gameState.getNobleHouseManager()
        );
        eventLogPanel.appendLines(log);
        calendarPanel.refresh();
        resourcePanel.refresh();
        popPanel.refresh();
        actionsPanel.refresh();
        if (ledgerPanel != null && ledgerDialog != null && ledgerDialog.isVisible()) {
            ledgerPanel.refresh();
        }

        if (centerPanel.getComponentCount() > 0 && centerPanel.getComponent(0) == mapView) {
            mapView.refreshSelectedZone();
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

private void loadBuildInfo() {
        if (buildInfoLoaded) return;
        java.util.Properties p = new java.util.Properties();
        boolean loaded = false;

        // 1. Try classpath (for JAR or IDE when resources are copied)
        try (java.io.InputStream is = MainWindow.class.getResourceAsStream("/buildinfo.properties")) {
            if (is != null) {
                p.load(is);
                loaded = true;
            }
        } catch (java.io.IOException e) {
            // fall through
        }

        // 2. Try local file in working directory
        if (!loaded) {
            try (java.io.FileReader fr = new java.io.FileReader("buildinfo.properties")) {
                p.load(fr);
                loaded = true;
            } catch (java.io.IOException e) {
                // fall through
            }
        }

        // 3. Try src/main/java relative to working directory (NetBeans typical)
        if (!loaded) {
            try (java.io.FileReader fr = new java.io.FileReader("src/main/java/buildinfo.properties")) {
                p.load(fr);
                loaded = true;
            } catch (java.io.IOException e) {
                // fall through
            }
        }

        if (loaded) {
            buildNo = p.getProperty("BUILD_NO", "?");
            buildTime = p.getProperty("LAST_UPDATED", "?");
            buildInfoLoaded = true;
            // Optionally log to event log if available
            if (eventLogPanel != null) {
                eventLogPanel.appendLine("[Build] Loaded: " + buildNo + " (" + buildTime + ")");
            }
        } else {
            // keep default "?" values
            if (eventLogPanel != null) {
                eventLogPanel.appendLine("[Build] No buildinfo.properties found");
            }
        }
    }

private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 2));
        statusBar.setBackground(UITheme.BG_DARK);
        statusBar.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 8, 2, 8));

        // Configure global tooltip behaviour for this application
        javax.swing.ToolTipManager ttm = javax.swing.ToolTipManager.sharedInstance();
        ttm.setInitialDelay(0);
        ttm.setDismissDelay(Integer.MAX_VALUE); // effectively infinite

        versionLabel = new JLabel("v" + Main.VERSION);
        versionLabel.setFont(UITheme.FONT_SMALL);
        versionLabel.setForeground(UITheme.TEXT_SECONDARY);
        String tooltipText;
        if (buildInfoLoaded && !"?".equals(buildNo) && !"?".equals(buildTime)) {
            tooltipText = String.format("Build: %s (%s)", buildNo, buildTime);
        } else if (buildInfoLoaded && !"?".equals(buildNo)) {
            tooltipText = String.format("Build: %s (timestamp unknown)", buildNo);
        } else {
            tooltipText = "No build info available";
        }
        versionLabel.setToolTipText(tooltipText);
        statusBar.add(versionLabel);

        return statusBar;
    }

/**
     * Shows a modal dialog asking the player to assign a liberated zone to a noble house.
     * Returns the chosen house, or the first claimant if the player closes without choosing.
     */
    private main.nobles.NobleHouse showZoneAwardDialog(
            String zoneId, java.util.List<main.nobles.NobleHouse> claimants) {

        if (claimants.isEmpty()) return null;

        String zoneName = zoneId.replace("_", " ");
        String[] options = claimants.stream()
                .map(h -> h.getName() + " (opinion: " + h.getPlayerOpinion() + ")")
                .toArray(String[]::new);

        int choice = JOptionPane.showOptionDialog(
                this,
                "The zone " + zoneName + " has been liberated from the barbarians!\n"
                + "Which noble house should receive it?",
                "Assign Liberated Zone",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        return (choice >= 0 && choice < claimants.size())
                ? claimants.get(choice)
                : claimants.get(0);
    }

/**
     * Re-wires all processor callbacks that reference UI components.
     * Must be called after construction and after gameState.reset() (new game).
     */

private void rewireCallbacks() {
        gameState.getPlayerCombatProcessor().setZoneAwardCallback(
                (zoneId, claimants) -> showZoneAwardDialog(zoneId, claimants));

        gameState.getTurnProcessor().setPayOffDialogSupplier(
                (army, resources, zoneId, owner, playerArmies, nobleArmies, nobleGarrison) -> {
            java.awt.Window win = this;
            final boolean[] result = {false};
            if (javax.swing.SwingUtilities.isEventDispatchThread()) {
                result[0] = ui.barbarians.BarbPayOffDialog.show(army, resources, win,
                        zoneId, owner, playerArmies, nobleArmies, nobleGarrison);
            } else {
                try {
                    javax.swing.SwingUtilities.invokeAndWait(() ->
                        result[0] = ui.barbarians.BarbPayOffDialog.show(army, resources, win,
                                zoneId, owner, playerArmies, nobleArmies, nobleGarrison));
                } catch (Exception ignored) {}
            }
            return result[0];
        });

        if (ledgerPanel != null) {
            gameState.getTurnProcessor().setOnSnapshotRequested(
                    () -> ledgerPanel.captureSnapshot());
        }
    }

}