package City.ui;

import City.ui.politics.PartiesOverviewPanel;
import City.ui.map.MapView;
import City.ui.politics.VoteSessionPanel;
import City.main.actions.ActionResult;
import City.main.core.GameState;
import City.main.Main;
import City.main.parameters.PoliticalParams;
import City.main.parameters.PrestigeXPParams;
import City.main.parameters.ProtectionParams;
import City.ui.GrantZoneClaimDialog;

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
    private final City.ui.nobles.NobleHousesPanel nobleHousesPanel;
    private  VoteSessionPanel       voteSessionPanel;
    private final MapView                mapView;
    private       City.ui.council.CouncilPanel councilPanel;

    // Election notification badge (shown until dismissed)
    private       JButton electionBadge;
    private       boolean electionPending = false;

    // version & build info
    private String buildNo = "?";
    private String buildTime = "?";
    private boolean buildInfoLoaded = false;
    private JLabel versionLabel;

    private final JPanel  centerPanel;
    private  JButton partiesBtn;
    private  JButton openVoteBtn;
    private City.ui.ledger.LedgerPanel ledgerPanel;
    private JDialog               ledgerDialog;

    public MainWindow(GameState gameState) {
        this.gameState = gameState;
        loadBuildInfo();
        rewireCallbacks();

        gameState.getTurnProcessor().setPayOffDialogSupplier((army, resources, zoneId, owner, playerArmies, nobleArmies, nobleGarrison) -> {
            java.awt.Window win = this;
            final boolean[] result = {false};
            if (javax.swing.SwingUtilities.isEventDispatchThread()) {
                result[0] = City.ui.barbarians.BarbPayOffDialog.show(army, resources, win,
                        zoneId, owner, playerArmies, nobleArmies, nobleGarrison);
            } else {
                try {
                    javax.swing.SwingUtilities.invokeAndWait(() ->
                        result[0] = City.ui.barbarians.BarbPayOffDialog.show(army, resources, win,
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
        calendarPanel.setElectionManager(gameState.getElectionManager());
        resourcePanel        = new ResourcePanel(gameState);
        popPanel             = new PopPanel(gameState);
        actionsPanel         = new ActionsPanel(gameState, this::handleActionResult);
        eventLogPanel        = new EventLogPanel();
        saveLoadDialog       = new SaveLoadDialog(this, gameState, eventLogPanel::appendLine);
        partiesOverviewPanel = new PartiesOverviewPanel(gameState, this::showMainView);
        nobleHousesPanel     = new City.ui.nobles.NobleHousesPanel(gameState, this::showMainView);
        voteSessionPanel     = new VoteSessionPanel(gameState, this::onVoteResult, this::swapCenter);
        mapView              = new MapView(gameState, this::showMainView);
        wireMapViewCallbacks();

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

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(UITheme.BG_DARK);

        JButton menuBtn = new JButton("☰");
        menuBtn.setFont(UITheme.FONT_TITLE);
        menuBtn.setForeground(UITheme.TEXT_GOLD);
        menuBtn.setBackground(UITheme.BG_PANEL);
        menuBtn.setBorderPainted(false);
        menuBtn.setFocusPainted(false);
        menuBtn.setPreferredSize(new Dimension(48, 48));
        menuBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        menuBtn.setToolTipText("Game Menu");
        menuBtn.addActionListener(e -> showGameMenu());

        topBar.add(menuBtn,     BorderLayout.WEST);
        topBar.add(calendarPanel, BorderLayout.CENTER);

        add(topBar,      BorderLayout.NORTH);
        add(leftSidebar, BorderLayout.WEST);
        add(centerPanel, BorderLayout.CENTER);

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
        // Save/load/new are now in the ☰ MENU dialog — this bar is removed
        JPanel bar = new JPanel();
        bar.setBackground(UITheme.BG_DARK);
        bar.setPreferredSize(new Dimension(0, 0));
        bar.setMaximumSize(new Dimension(0, 0));
        bar.setVisible(false);
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
    partiesBtn = makeBottomBarButton("PARTIES");
    partiesBtn.addActionListener(e -> showPartiesView());

    JButton noblesBtn = makeBottomBarButton("NOBLES");
    noblesBtn.addActionListener(e -> showNoblesView());

    JButton mapBtn = makeBottomBarButton("MAP");
    mapBtn.addActionListener(e -> showMapView());

    openVoteBtn = makeBottomBarButton("⚑ OPEN VOTE");
    openVoteBtn.setForeground(UITheme.TEXT_GOLD);
    openVoteBtn.setBackground(new Color(60, 40, 20));
    openVoteBtn.setVisible(false);
    openVoteBtn.addActionListener(e -> showVoteSession());

    JButton ledgerBtn = makeBottomBarButton("LEDGER");
    ledgerBtn.addActionListener(e -> showLedger());

        JButton militaryBtn = makeBottomBarButton("MILITARY");
        militaryBtn.addActionListener(e -> showMilitaryView());

        JButton councilBtn = makeBottomBarButton("⚑ REALM COUNCIL");
        councilBtn.setForeground(new Color(200, 170, 80));
        councilBtn.addActionListener(e -> showCouncilView());

        JButton cityCouncilBtn = makeBottomBarButton("🏛 CITY COUNCIL");
        cityCouncilBtn.setForeground(new Color(160, 190, 255));
        cityCouncilBtn.addActionListener(e -> showCityCouncilView());

        electionBadge = makeBottomBarButton("⚑ ELECTION RESULTS");
        electionBadge.setForeground(new Color(240, 200, 80));
        electionBadge.setBackground(new Color(70, 50, 10));
        electionBadge.setVisible(false);
        electionBadge.addActionListener(e -> showElectionResults());

        JPanel leftBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        leftBtns.setBackground(UITheme.BG_DARK);
        leftBtns.add(partiesBtn);
        leftBtns.add(noblesBtn);
        leftBtns.add(mapBtn);
        leftBtns.add(openVoteBtn);
        leftBtns.add(ledgerBtn);
        leftBtns.add(militaryBtn);
        leftBtns.add(councilBtn);
        leftBtns.add(cityCouncilBtn);
        leftBtns.add(electionBadge);

        JPanel wrapper = new JPanel(new BorderLayout(6, 0));
        wrapper.setBackground(UITheme.BG_DARK);
        wrapper.setBorder(new EmptyBorder(8, 12, 8, 12));
        wrapper.add(leftBtns, BorderLayout.WEST);
        return wrapper;
}

private JButton makeBottomBarButton(String label) {
    JButton btn = new JButton(label);
    btn.setFont(UITheme.FONT_BUTTON);
    btn.setForeground(UITheme.TEXT_SECONDARY);
    btn.setBackground(UITheme.BUTTON_BG);
    btn.setBorderPainted(false);
    btn.setFocusPainted(false);
    btn.setMargin(new Insets(6, 10, 6, 10));
    btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    return btn;
}

private void showLedger() {
        if (ledgerDialog == null) {
            ledgerPanel  = new City.ui.ledger.LedgerPanel(
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

private void showGameMenu() {
        JDialog menu = new JDialog(this, "Game Menu", true);
        menu.setUndecorated(true);
        menu.setSize(320, 380);
        menu.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_PANEL);
        root.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 2));

        JLabel title = new JLabel("  GAME MENU");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_GOLD);
        title.setBorder(new javax.swing.border.EmptyBorder(12, 12, 12, 12));
        title.setBackground(UITheme.BG_PANEL_LIGHT);
        title.setOpaque(true);

        JPanel btns = new JPanel(new GridLayout(0, 1, 0, 8));
        btns.setBackground(UITheme.BG_PANEL);
        btns.setBorder(new javax.swing.border.EmptyBorder(16, 24, 16, 24));

        JButton newBtn = makeMenuButton("🗺  New Game");
        newBtn.addActionListener(e -> {
            menu.dispose();
            saveLoadDialog.newGame(() -> {
                mapView.reinitialize(gameState);
                rewireCallbacks();
                showMainView();
                resetLogs();
            });
        });

        JButton saveBtn = makeMenuButton("💾  Save Game");
        saveBtn.addActionListener(e -> {
            menu.dispose();
            saveLoadDialog.save();
        });

        JButton loadBtn = makeMenuButton("📂  Load Game");
        loadBtn.addActionListener(e -> {
            menu.dispose();
            saveLoadDialog.load(() -> {
                showMainView();
                if (gameState.hasActiveSession()) showVoteSession();
                updateEndTurnState();
                resetLogs();
                eventLogPanel.appendLine("Game loaded.");
            });
        });

        JButton settingsBtn = makeMenuButton("⚙  Settings");
        settingsBtn.addActionListener(e -> {
            menu.dispose();
            showSettings();
        });

        JButton closeBtn = makeMenuButton("✕  Close");
        closeBtn.setForeground(UITheme.TEXT_SECONDARY);
        closeBtn.addActionListener(e -> menu.dispose());

        btns.add(newBtn);
        btns.add(saveBtn);
        btns.add(loadBtn);
        btns.add(settingsBtn);
        btns.add(Box.createVerticalStrut(8));
        btns.add(closeBtn);

        root.add(title, BorderLayout.NORTH);
        root.add(btns,  BorderLayout.CENTER);

        menu.setContentPane(root);
        menu.setVisible(true);
    }

private JButton makeMenuButton(String label) {
        JButton btn = new JButton(label);
        btn.setFont(UITheme.FONT_HEADER);
        btn.setForeground(UITheme.TEXT_PRIMARY);
        btn.setBackground(UITheme.BUTTON_BG);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(0, 42));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(UITheme.BUTTON_HOVER);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(UITheme.BUTTON_BG);
            }
        });
        return btn;
    }

private void showSettings() {
        JDialog settings = new JDialog(this, "Settings", true);
        settings.setUndecorated(true);
        settings.setSize(400, 300);
        settings.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_PANEL);
        root.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 2));

        JLabel title = new JLabel("  SETTINGS");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_GOLD);
        title.setBorder(new javax.swing.border.EmptyBorder(12, 12, 10, 12));
        title.setBackground(UITheme.BG_PANEL_LIGHT);
        title.setOpaque(true);

        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(UITheme.BG_PANEL);
        body.setBorder(new javax.swing.border.EmptyBorder(16, 24, 8, 24));

        GridBagConstraints gc = new GridBagConstraints();
        gc.fill    = GridBagConstraints.HORIZONTAL;
        gc.insets  = new java.awt.Insets(4, 0, 4, 8);
        gc.weightx = 0;

        // ── Global font size ──
        JLabel fontLabel = new JLabel("Font Size:");
        fontLabel.setFont(UITheme.FONT_BODY);
        fontLabel.setForeground(UITheme.TEXT_PRIMARY);
        gc.gridx = 0; gc.gridy = 0;
        body.add(fontLabel, gc);

        int currentSize = UITheme.BASE_SIZE;
        JSlider fontSlider = new JSlider(9, 18, currentSize);
        fontSlider.setBackground(UITheme.BG_PANEL);
        fontSlider.setMajorTickSpacing(3);
        fontSlider.setMinorTickSpacing(1);
        fontSlider.setPaintTicks(true);
        fontSlider.setPaintLabels(true);
        fontSlider.setForeground(UITheme.TEXT_SECONDARY);
        gc.gridx = 1; gc.weightx = 1.0;
        body.add(fontSlider, gc);

        JLabel sizeDisplay = new JLabel(currentSize + "px");
        sizeDisplay.setFont(UITheme.FONT_SMALL);
        sizeDisplay.setForeground(UITheme.TEXT_SECONDARY);
        gc.gridx = 2; gc.weightx = 0;
        body.add(sizeDisplay, gc);

        fontSlider.addChangeListener(e -> sizeDisplay.setText(fontSlider.getValue() + "px"));

        // ── Map panel font size ──
        JLabel mapFontLabel = new JLabel("Map Panel Size:");
        mapFontLabel.setFont(UITheme.FONT_BODY);
        mapFontLabel.setForeground(UITheme.TEXT_PRIMARY);
        gc.gridx = 0; gc.gridy = 1; gc.weightx = 0;
        body.add(mapFontLabel, gc);

        int currentMapSize = UITheme.MAP_PANEL_SIZE;
        JSlider mapFontSlider = new JSlider(8, 18, currentMapSize);
        mapFontSlider.setBackground(UITheme.BG_PANEL);
        mapFontSlider.setMajorTickSpacing(3);
        mapFontSlider.setMinorTickSpacing(1);
        mapFontSlider.setPaintTicks(true);
        mapFontSlider.setPaintLabels(true);
        mapFontSlider.setForeground(UITheme.TEXT_SECONDARY);
        gc.gridx = 1; gc.weightx = 1.0;
        body.add(mapFontSlider, gc);

        JLabel mapSizeDisplay = new JLabel(currentMapSize + "px");
        mapSizeDisplay.setFont(UITheme.FONT_SMALL);
        mapSizeDisplay.setForeground(UITheme.TEXT_SECONDARY);
        gc.gridx = 2; gc.weightx = 0;
        body.add(mapSizeDisplay, gc);

        mapFontSlider.addChangeListener(e -> mapSizeDisplay.setText(mapFontSlider.getValue() + "px"));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btnRow.setBackground(UITheme.BG_PANEL);

        JButton applyBtn = new JButton("Apply & Restart View");
        applyBtn.setFont(UITheme.FONT_BUTTON);
        applyBtn.setForeground(UITheme.TEXT_GOLD);
        applyBtn.setBackground(UITheme.BUTTON_BG);
        applyBtn.setBorderPainted(false);
        applyBtn.setFocusPainted(false);
        applyBtn.addActionListener(e -> {
            UITheme.applyFontScale(fontSlider.getValue());
            UITheme.applyMapPanelFontScale(mapFontSlider.getValue());
            FontPropagator.applyToWindow(this);
            mapView.applyMapPanelFonts();
            settings.dispose();
        });

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(UITheme.FONT_BUTTON);
        cancelBtn.setForeground(UITheme.TEXT_SECONDARY);
        cancelBtn.setBackground(UITheme.BUTTON_BG);
        cancelBtn.setBorderPainted(false);
        cancelBtn.setFocusPainted(false);
        cancelBtn.addActionListener(e -> settings.dispose());

        btnRow.add(cancelBtn);
        btnRow.add(applyBtn);

        root.add(title,  BorderLayout.NORTH);
        root.add(body,   BorderLayout.CENTER);
        root.add(btnRow, BorderLayout.SOUTH);

        settings.setContentPane(root);
        settings.setVisible(true);
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

    private void showElectionResults() {
        City.main.politics.ElectionRecord record =
                gameState.getElectionManager().getLastRecord();
        if (record == null) return;
        electionBadge.setVisible(false);
        electionPending = false;
        City.ui.politics.ElectionResultsPanel panel =
                new City.ui.politics.ElectionResultsPanel(
                        record,
                        gameState.getCalendar(),
                        this::showMainView,
                        () -> { showMainView(); showPartiesView(); }); // "See council" → parties overview
        swapCenter(panel);
    }

    private void showCouncilView() {
        councilPanel = new City.ui.council.CouncilPanel(gameState, () -> {
            showMainView();
            updateEndTurnState();
        });
        // Wire the unlawful zone picker to open the map with zone selection
        councilPanel.setUnlawfulZonePickerCallback(callback -> {
            showUnlawfulZonePicker(callback);
        });
        if (gameState.hasActiveCouncilSession()) {
            councilPanel.refresh();
        }
        swapCenter(councilPanel);
        updateEndTurnState();
    }

private void showCityCouncilView() {
        City.ui.politics.CityCouncilPanel panel = new City.ui.politics.CityCouncilPanel(
                gameState, this::showMainView);
        swapCenter(panel);
    }

private void showUnlawfulZonePicker(java.util.function.Consumer<String> onZonePicked) {
        // Build list of valid zones: noble-owned, with at least 1 other claimant
        java.util.List<City.main.map.Zone> validZones = new java.util.ArrayList<>();
        for (City.main.map.Zone z : gameState.getZoneManager().getZones()) {
            if (z.isDesolate()) continue;
            City.main.nobles.NobleHouse owner =
                    gameState.getNobleHouseManager().getOwnerOfZone(z.getId());
            if (owner == null) continue;
            boolean hasClaimant = false;
            for (City.main.nobles.NobleHouse h : gameState.getNobleHouseManager().getHouses()) {
                if (h != owner && !h.isEliminated()
                        && gameState.getNobleHouseManager().getClaimManager()
                                .hasClaim(h.getId(), z.getId())) {
                    hasClaimant = true;
                    break;
                }
            }
            if (hasClaimant) validZones.add(z);
        }

        if (validZones.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No zone qualifies. Each zone must be noble-owned with at least one other claimant.",
                    "Unlawful Acquisition", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Show a map-styled zone picker dialog
        JDialog picker = new JDialog(this, "Select Zone — Unlawful Acquisition", true);
        picker.setSize(440, 460);
        picker.setLocationRelativeTo(this);
        picker.setResizable(true);
        picker.getContentPane().setBackground(UITheme.BG_PANEL);
        picker.setLayout(new BorderLayout());

        JLabel title = new JLabel("  Select the zone to declare as unlawfully acquired");
        title.setFont(UITheme.FONT_HEADER);
        title.setForeground(UITheme.TEXT_GOLD);
        title.setBorder(new EmptyBorder(14, 14, 10, 14));
        title.setBackground(UITheme.BG_PANEL_LIGHT);
        title.setOpaque(true);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(UITheme.BG_DARK);
        listPanel.setBorder(new EmptyBorder(8, 10, 8, 10));

        ButtonGroup group = new ButtonGroup();
        final City.main.map.Zone[] selected = {validZones.get(0)};

        for (City.main.map.Zone z : validZones) {
            City.main.nobles.NobleHouse owner =
                    gameState.getNobleHouseManager().getOwnerOfZone(z.getId());
            java.util.List<String> claimants = new java.util.ArrayList<>();
            for (City.main.nobles.NobleHouse h : gameState.getNobleHouseManager().getHouses()) {
                if (h != owner && !h.isEliminated()
                        && gameState.getNobleHouseManager().getClaimManager()
                                .hasClaim(h.getId(), z.getId())) {
                    claimants.add(h.getName().replace("House ", ""));
                }
            }

            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setBackground(UITheme.BG_PANEL);
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
                    new EmptyBorder(8, 10, 8, 10)));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

            JRadioButton rb = new JRadioButton();
            rb.setBackground(UITheme.BG_PANEL);
            if (z == selected[0]) rb.setSelected(true);
            group.add(rb);

            JPanel info = new JPanel();
            info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
            info.setBackground(UITheme.BG_PANEL);

            JLabel nameLabel = new JLabel(z.getDisplayName());
            nameLabel.setFont(UITheme.FONT_BUTTON);
            nameLabel.setForeground(UITheme.TEXT_GOLD);

            String ownerText = owner != null ? owner.getName().replace("House ", "") : "?";
            JLabel detailLabel = new JLabel("Owner: " + ownerText
                    + "   Claimants: " + String.join(", ", claimants));
            detailLabel.setFont(UITheme.FONT_SMALL);
            detailLabel.setForeground(UITheme.TEXT_SECONDARY);

            info.add(nameLabel);
            info.add(detailLabel);
            row.add(rb,   BorderLayout.WEST);
            row.add(info, BorderLayout.CENTER);

            row.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    rb.setSelected(true);
                    selected[0] = z;
                }
            });
            listPanel.add(row);
            listPanel.add(Box.createVerticalStrut(6));
        }

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.setBackground(UITheme.BG_DARK);
        scroll.getViewport().setBackground(UITheme.BG_DARK);
        scroll.getVerticalScrollBar().setUnitIncrement(24);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btnRow.setBackground(UITheme.BG_PANEL);

        JButton confirmBtn = new JButton("SELECT THIS ZONE");
        confirmBtn.setFont(UITheme.FONT_BUTTON);
        confirmBtn.setForeground(UITheme.TEXT_GOLD);
        confirmBtn.setBackground(UITheme.BUTTON_BG);
        confirmBtn.setBorderPainted(false);
        confirmBtn.setFocusPainted(false);
        confirmBtn.addActionListener(e -> {
            picker.dispose();
            onZonePicked.accept(selected[0].getId());
        });

        JButton cancelBtn = new JButton("CANCEL");
        cancelBtn.setFont(UITheme.FONT_BUTTON);
        cancelBtn.setForeground(UITheme.TEXT_SECONDARY);
        cancelBtn.setBackground(UITheme.BUTTON_BG);
        cancelBtn.setBorderPainted(false);
        cancelBtn.setFocusPainted(false);
        cancelBtn.addActionListener(e -> {
            picker.dispose();
            onZonePicked.accept(null);
        });

        btnRow.add(cancelBtn);
        btnRow.add(confirmBtn);

        picker.add(title,  BorderLayout.NORTH);
        picker.add(scroll, BorderLayout.CENTER);
        picker.add(btnRow, BorderLayout.SOUTH);
        picker.setVisible(true);
    }

private void showCampaignDialog() {
        int turns = gameState.getElectionManager().getTurnsUntilElection();
        JDialog d = new JDialog(this, "⚑ Election Campaign", true);
        d.setUndecorated(true);
        d.setSize(420, 280);
        d.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(UITheme.BG_PANEL);
        root.setBorder(javax.swing.BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 2));

        JLabel title = new JLabel("  ⚑ ELECTION CAMPAIGN BEGINS");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(new Color(240, 200, 80));
        title.setBackground(UITheme.BG_PANEL_LIGHT);
        title.setOpaque(true);
        title.setBorder(new javax.swing.border.EmptyBorder(12, 12, 10, 12));

        JLabel body = new JLabel("<html><body style='width:340px; padding: 8px'>"
                + "The election is <b>" + turns + " turn(s)</b> away.<br><br>"
                + "You may declare support for one party. Based on your prestige, "
                + "they will receive a bonus. However, if they lose more than 2 seats, "
                + "you will suffer a significant prestige penalty.<br><br>"
                + "Open <b>PARTIES</b> to declare support."
                + "</body></html>");
        body.setFont(UITheme.FONT_BODY);
        body.setForeground(UITheme.TEXT_PRIMARY);

        JPanel btns = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 8));
        btns.setBackground(UITheme.BG_PANEL);

        JButton openPartiesBtn = new JButton("OPEN PARTIES");
        openPartiesBtn.setFont(UITheme.FONT_BUTTON);
        openPartiesBtn.setForeground(new Color(240, 200, 80));
        openPartiesBtn.setBackground(new Color(60, 45, 15));
        openPartiesBtn.setBorderPainted(false);
        openPartiesBtn.setFocusPainted(false);
        openPartiesBtn.addActionListener(e -> { d.dispose(); showPartiesView(); });

        JButton closeBtn = new JButton("Dismiss");
        closeBtn.setFont(UITheme.FONT_BUTTON);
        closeBtn.setForeground(UITheme.TEXT_SECONDARY);
        closeBtn.setBackground(UITheme.BUTTON_BG);
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.addActionListener(e -> d.dispose());

        btns.add(closeBtn);
        btns.add(openPartiesBtn);

        root.add(title, BorderLayout.NORTH);
        root.add(body,  BorderLayout.CENTER);
        root.add(btns,  BorderLayout.SOUTH);
        d.setContentPane(root);
        d.setVisible(true);
    }

private void showProtectionDialog(City.main.actions.DeclareProtectionAction action) {
    java.util.List<City.main.nobles.NobleHouse> houses =
            gameState.getNobleHouseManager().getHouses();
    java.util.List<City.main.nobles.NobleHouse> eligible = new java.util.ArrayList<>();
    for (City.main.nobles.NobleHouse h : houses) {
        if (!h.isEliminated()
                && !gameState.getProtectionManager().isUnderProtection(h.getId())) {
            eligible.add(h);
        }
    }
    if (eligible.isEmpty()) {
        JOptionPane.showMessageDialog(this,
                "All noble houses are already under protection or none are available.",
                "Declare Protection", JOptionPane.INFORMATION_MESSAGE);
        return;
    }

    JDialog d = new JDialog(this, "Declare Protection", true);
    d.setUndecorated(true);
    d.setSize(440, 380);
    d.setLocationRelativeTo(this);

    JPanel root = new JPanel(new BorderLayout(0, 8));
    root.setBackground(UITheme.BG_PANEL);
    root.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 2));

    JLabel title = new JLabel("  🛡 Declare Protection");
    title.setFont(UITheme.FONT_TITLE);
    title.setForeground(UITheme.TEXT_GOLD);
    title.setBackground(UITheme.BG_PANEL_LIGHT);
    title.setOpaque(true);
    title.setBorder(new javax.swing.border.EmptyBorder(12, 12, 10, 12));

    JLabel info = new JLabel("<html><body style='width:360px'>"
            + "Declare a noble house under your protection.<br>"
            + "Costs <b>" + ProtectionParams.PROTECTION_INFLUENCE_COST
            + " influence</b>. Target gains <b>+"
            + ProtectionParams.PROTECTION_TARGET_OPINION_BONUS
            + " opinion</b>. Their rivals get <b>"
            + ProtectionParams.PROTECTION_RIVAL_OPINION_MALUS
            + " opinion</b>.<br>"
            + "If they lose a zone, you suffer <b>-"
            + Math.abs(PrestigeXPParams.PLAYER_PRESTIGE_PROTECTED_ZONE_LOST)
            + " prestige</b>."
            + "</body></html>");
    info.setFont(UITheme.FONT_SMALL);
    info.setForeground(UITheme.TEXT_SECONDARY);
    info.setBorder(new javax.swing.border.EmptyBorder(8, 12, 8, 12));

    JPanel listPanel = new JPanel();
    listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
    listPanel.setBackground(UITheme.BG_DARK);
    listPanel.setBorder(new javax.swing.border.EmptyBorder(4, 8, 4, 8));

    final City.main.nobles.NobleHouse[] chosen = {eligible.get(0)};
    ButtonGroup group = new ButtonGroup();

    for (City.main.nobles.NobleHouse house : eligible) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(UITheme.BG_PANEL);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
                new javax.swing.border.EmptyBorder(6, 10, 6, 10)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JRadioButton rb = new JRadioButton();
        rb.setBackground(UITheme.BG_PANEL);
        if (house == chosen[0]) rb.setSelected(true);
        group.add(rb);

        JLabel nameLabel = new JLabel(house.getName());
        nameLabel.setFont(UITheme.FONT_BUTTON);
        nameLabel.setForeground(UITheme.TEXT_GOLD);

        JLabel opinionLabel = new JLabel("Opinion: " + house.getPlayerOpinion()
                + "  |  Zones: " + house.getZoneIds().size());
        opinionLabel.setFont(UITheme.FONT_SMALL);
        opinionLabel.setForeground(UITheme.TEXT_SECONDARY);

        JPanel infoCol = new JPanel();
        infoCol.setLayout(new BoxLayout(infoCol, BoxLayout.Y_AXIS));
        infoCol.setBackground(UITheme.BG_PANEL);
        infoCol.add(nameLabel);
        infoCol.add(opinionLabel);

        row.add(rb, BorderLayout.WEST);
        row.add(infoCol, BorderLayout.CENTER);

        row.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                rb.setSelected(true);
                chosen[0] = house;
            }
        });
        listPanel.add(row);
        listPanel.add(Box.createVerticalStrut(4));
    }

    JScrollPane scroll = new JScrollPane(listPanel);
    scroll.setBorder(null);
    scroll.setBackground(UITheme.BG_DARK);
    scroll.getViewport().setBackground(UITheme.BG_DARK);

    JPanel btnRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 8));
    btnRow.setBackground(UITheme.BG_PANEL);

    JButton confirmBtn = new JButton("DECLARE PROTECTION");
    confirmBtn.setFont(UITheme.FONT_BUTTON);
    confirmBtn.setForeground(UITheme.TEXT_GOLD);
    confirmBtn.setBackground(UITheme.BUTTON_BG);
    confirmBtn.setBorderPainted(false);
    confirmBtn.setFocusPainted(false);
    confirmBtn.addActionListener(e -> {
        City.main.actions.ActionResult result =
                action.applyProtection(chosen[0], gameState.getResources(),
                        gameState.getLedger());
        d.dispose();
        if (result.isSuccess()) {
            eventLogPanel.appendLine("✓ " + result.getMessage());
        } else {
            JOptionPane.showMessageDialog(this, result.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
        actionsPanel.refresh();
        resourcePanel.refresh();
    });

    JButton cancelBtn = new JButton("CANCEL");
    cancelBtn.setFont(UITheme.FONT_BUTTON);
    cancelBtn.setForeground(UITheme.TEXT_SECONDARY);
    cancelBtn.setBackground(UITheme.BUTTON_BG);
    cancelBtn.setBorderPainted(false);
    cancelBtn.setFocusPainted(false);
    cancelBtn.addActionListener(e -> d.dispose());

    btnRow.add(cancelBtn);
    btnRow.add(confirmBtn);

    root.add(title, BorderLayout.NORTH);
    root.add(info,  BorderLayout.CENTER);
    // Note: info is small — add scroll below it
    JPanel center = new JPanel(new BorderLayout());
    center.setBackground(UITheme.BG_PANEL);
    center.add(info,   BorderLayout.NORTH);
    center.add(scroll, BorderLayout.CENTER);
    root.add(center, BorderLayout.CENTER);
    root.add(btnRow, BorderLayout.SOUTH);

    d.setContentPane(root);
    d.setVisible(true);
}

public void showMilitaryViewWithHighlight(String armyDisplayName) {
    showMilitaryView();
    // Find the MilitaryMenuUI in center and request highlight
    if (centerPanel.getComponentCount() > 0) {
        Component c = centerPanel.getComponent(0);
        if (c instanceof City.ui.MilitaryMenuUI mui) {
            mui.highlightArmy(armyDisplayName);
            mui.rebuild();
        }
    }
}

private void showMilitaryView() {
        City.main.army.commander.CommanderRoster      roster = gameState.getCommanderRoster();
        City.main.army.commander.CommanderRecruitPool pool   = gameState.getCommanderRecruitPool();
    City.ui.MilitaryMenuUI militaryUI = new City.ui.MilitaryMenuUI(
            gameState.getArmyManager(),
            roster,
            pool,
            gameState.getResources(),
            gameState.getPartyManager(),
            gameState.getMercenaryManager(),
            this::showMainView,
            () -> {
                resourcePanel.refresh();
                popPanel.refresh();
            });
    swapCenter(militaryUI);
}

private void showVoteSession() {
        // Rebuild panel in case gameState changed
        voteSessionPanel = new VoteSessionPanel(gameState, this::onVoteResult, this::swapCenter);
        voteSessionPanel.refresh();
        swapCenter(voteSessionPanel);
    }

    private void onVoteResult(City.main.politics.VoteResult result,
                               java.util.List<String> logLines) {
        if (result != null && logLines != null) {
            eventLogPanel.appendLines(logLines);
        } else if (gameState.hasActiveSession()) {
            eventLogPanel.appendLine("↩ Returned to main view. Vote session still pending.");
        }
        showMainView();
        refreshAll();
        updateEndTurnState();
    }

    // Keep old no-arg version for back button
    private void onVoteFinalized() {
        onVoteResult(null, null);
    }

    private void updateEndTurnState() {
        boolean votePending   = gameState.hasActiveSession();
        boolean councilVoting = gameState.hasActiveCouncilSession();
        boolean blocked       = votePending || councilVoting;
        calendarPanel.updateEndTurnState(blocked, blocked);
        partiesBtn.setVisible(!votePending);
        openVoteBtn.setVisible(votePending);
    }

private void endTurn() {
        City.main.politics.ElectionRecord recordBefore =
                gameState.getElectionManager().getLastRecord();

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

        // Refresh center panel if it's the council view or parties view
        if (centerPanel.getComponentCount() > 0) {
            Component center = centerPanel.getComponent(0);
            if (center instanceof City.ui.council.CouncilPanel cp) {
                cp.refresh();
            } else if (center instanceof City.ui.politics.PartiesOverviewPanel pop) {
                pop.refresh();
            } else if (center instanceof City.ui.map.MapView mv) {
                mv.refreshSelectedZone();
                mv.repaint();
            }
        }

        if (gameState.getCalendar().isFrostGiantYear()) {
            eventLogPanel.appendLine("⚠  THE FROST GIANTS HAVE ARRIVED. THE REALM TREMBLES.");
        }

        // Show election results if an election just fired
        City.main.politics.ElectionRecord recordAfter =
                gameState.getElectionManager().getLastRecord();
        if (recordAfter != null && recordAfter != recordBefore) {
            showElectionResults();
            electionPending  = true;
            electionBadge.setVisible(true);
        }

        // Show campaign popup if campaign just started
        City.main.politics.ElectionManager elMgr = gameState.getElectionManager();
        if (elMgr.isCampaignPeriod() && elMgr.getTurnsUntilElection()
                == City.main.parameters.PoliticalParams.ELECTION_CAMPAIGN_WARNING_TURNS) {
            showCampaignDialog();
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

private City.main.nobles.NobleHouse showZoneAwardDialog(
            String zoneId, java.util.List<City.main.nobles.NobleHouse> claimants) {

        if (claimants.isEmpty()) return null;

        String zoneName = zoneId.replace("_", " ");

        JDialog dialog = new JDialog(this, "Assign Liberated Zone", true);
        dialog.setUndecorated(false);
        dialog.setSize(420, 340);
        dialog.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBackground(UITheme.BG_PANEL);
        root.setBorder(BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 2));

        JLabel title = new JLabel("  Zone Liberated: " + zoneName);
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_GOLD);
        title.setBorder(new javax.swing.border.EmptyBorder(10, 12, 10, 12));
        title.setBackground(UITheme.BG_PANEL_LIGHT);
        title.setOpaque(true);

        JLabel subtitle = new JLabel("  Which noble house should receive this zone?");
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_SECONDARY);
        subtitle.setBorder(new javax.swing.border.EmptyBorder(6, 12, 6, 12));

        JPanel topBlock = new JPanel(new BorderLayout());
        topBlock.setBackground(UITheme.BG_PANEL);
        topBlock.add(title,    BorderLayout.NORTH);
        topBlock.add(subtitle, BorderLayout.SOUTH);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(UITheme.BG_DARK);
        listPanel.setBorder(new javax.swing.border.EmptyBorder(8, 8, 8, 8));

        final City.main.nobles.NobleHouse[] chosen = {null};
        ButtonGroup group = new ButtonGroup();

        for (City.main.nobles.NobleHouse house : claimants) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setBackground(UITheme.BG_PANEL);
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UITheme.BORDER_COLOR, 1),
                    new javax.swing.border.EmptyBorder(6, 10, 6, 10)));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

            JRadioButton rb = new JRadioButton();
            rb.setBackground(UITheme.BG_PANEL);
            group.add(rb);

            JLabel nameLabel = new JLabel(house.getName());
            nameLabel.setFont(UITheme.FONT_BUTTON);
            nameLabel.setForeground(UITheme.TEXT_GOLD);

            JLabel opinionLabel = new JLabel("Opinion: " + house.getPlayerOpinion());
            opinionLabel.setFont(UITheme.FONT_SMALL);
            opinionLabel.setForeground(UITheme.TEXT_SECONDARY);

            JPanel infoCol = new JPanel();
            infoCol.setLayout(new BoxLayout(infoCol, BoxLayout.Y_AXIS));
            infoCol.setBackground(UITheme.BG_PANEL);
            infoCol.add(nameLabel);
            infoCol.add(opinionLabel);

            row.add(rb,      BorderLayout.WEST);
            row.add(infoCol, BorderLayout.CENTER);

            row.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    rb.setSelected(true);
                    chosen[0] = house;
                }
            });

            listPanel.add(row);
            listPanel.add(Box.createVerticalStrut(4));
        }

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.setBackground(UITheme.BG_DARK);
        scroll.getViewport().setBackground(UITheme.BG_DARK);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        btnRow.setBackground(UITheme.BG_PANEL);

        JButton confirmBtn = new JButton("Assign");
        confirmBtn.setFont(UITheme.FONT_BUTTON);
        confirmBtn.setForeground(UITheme.TEXT_GOLD);
        confirmBtn.setBackground(UITheme.BUTTON_BG);
        confirmBtn.setBorderPainted(false);
        confirmBtn.setFocusPainted(false);
        confirmBtn.addActionListener(e -> dialog.dispose());

        JButton cancelBtn = new JButton("First Claimant");
        cancelBtn.setFont(UITheme.FONT_BUTTON);
        cancelBtn.setForeground(UITheme.TEXT_SECONDARY);
        cancelBtn.setBackground(UITheme.BUTTON_BG);
        cancelBtn.setBorderPainted(false);
        cancelBtn.setFocusPainted(false);
        cancelBtn.addActionListener(e -> {
            chosen[0] = null;
            dialog.dispose();
        });

        btnRow.add(cancelBtn);
        btnRow.add(confirmBtn);

        root.add(topBlock, BorderLayout.NORTH);
        root.add(scroll,   BorderLayout.CENTER);
        root.add(btnRow,   BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.setVisible(true);

        return (chosen[0] != null) ? chosen[0] : claimants.get(0);
    }

/**
     * Re-wires all processor callbacks that reference UI components.
     * Must be called after construction and after gameState.reset() (new game).
     */

    private void rewireCallbacks() {
        gameState.getPlayerCombatProcessor().setPlayerPrestige(
                gameState.getPlayerPrestige());
        gameState.getPlayerCombatProcessor().setProtectionManager(
                gameState.getProtectionManager());
        gameState.getPlayerCombatProcessor().setNobleHouseManagerRef(
                gameState.getNobleHouseManager());
    gameState.getPlayerCombatProcessor().setZoneAwardCallback(
            (zoneId, claimants) -> showZoneAwardDialog(zoneId, claimants));

    gameState.getTurnProcessor().setPayOffDialogSupplier((army, resources, zoneId, owner, playerArmies, nobleArmies, nobleGarrison) -> {
        java.awt.Window win = this;
        final boolean[] result = {false};
        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
            result[0] = City.ui.barbarians.BarbPayOffDialog.show(army, resources, win,
                    zoneId, owner, playerArmies, nobleArmies, nobleGarrison);
        } else {
            try {
                javax.swing.SwingUtilities.invokeAndWait(() ->
                    result[0] = City.ui.barbarians.BarbPayOffDialog.show(army, resources, win,
                            zoneId, owner, playerArmies, nobleArmies, nobleGarrison));
            } catch (Exception ignored) {}
        }
        return result[0];
    });

    if (ledgerPanel != null) {
        gameState.getTurnProcessor().setOnSnapshotRequested(
                () -> ledgerPanel.captureSnapshot());
    }

    // Mercenary hire dialog — pool-based
    gameState.getActionRegistry().getHireMercenariesAction()
            .setHireDialogCallback(() ->
                City.ui.MercenaryPoolHireDialog.show(
                        this,
                        gameState.getMercenaryManager(),
                        gameState.getResources(),
                        gameState.getLedger(),
                        () -> { resourcePanel.refresh(); popPanel.refresh(); }));

    // Send Resources to Nobles dialog
    gameState.getActionRegistry().getSendResourcesToNoblesAction()
            .setDialogCallback(() ->
                City.ui.SendResourcesToNoblesDialog.show(
                        this,
                        gameState.getActionRegistry().getSendResourcesToNoblesAction(),
                        gameState.getNobleHouseManager(),
                        gameState.getResources(),
                        gameState.getLedger(),
                        () -> { resourcePanel.refresh(); actionsPanel.refresh(); }));

    // Grant Zone Claim dialog
    gameState.getActionRegistry().getGrantZoneClaimAction()
            .setDialogCallback(() -> {
                City.ui.GrantZoneClaimDialog.show(
                        this,
                        gameState.getActionRegistry().getGrantZoneClaimAction(),
                        gameState.getNobleHouseManager(),
                        gameState.getZoneManager(),
                        gameState.getResources(),
                        gameState.getLedger(),
                        null,
                        () -> actionsPanel.refresh());
                return false;
            });

    // Declare Protection dialog
    for (City.main.actions.PlayerAction pa : gameState.getActionRegistry().getRealmActions()) {
        if (pa instanceof City.main.actions.DeclareProtectionAction dpa) {
            dpa.setDialogCallback(() -> showProtectionDialog(dpa));
            break;
        }
    }

    // Recreate vote session panel with new reference after reset
    voteSessionPanel = new VoteSessionPanel(gameState, this::onVoteResult, this::swapCenter);

    // Battle intervention dialog — detailed version
    gameState.getBattleInterventionProcessor().setDetailedCallback(
        (attackerName, defenderName, zoneId, playerSize, attackerSize,
         atkAllies, defAllies, defProtected) -> {
            final City.main.army.PlayerBattleInterventionProcessor.PlayerChoice[] result =
                { City.main.army.PlayerBattleInterventionProcessor.PlayerChoice.IGNORE };
            if (javax.swing.SwingUtilities.isEventDispatchThread()) {
                result[0] = City.ui.BattleInterventionDialog.showDetailed(
                        this, attackerName, defenderName, zoneId, playerSize, attackerSize,
                        atkAllies, defAllies, defProtected).choice();
            } else {
                try {
                    javax.swing.SwingUtilities.invokeAndWait(() ->
                        result[0] = City.ui.BattleInterventionDialog.showDetailed(
                                this, attackerName, defenderName, zoneId, playerSize, attackerSize,
                                atkAllies, defAllies, defProtected).choice());
                } catch (Exception ignored) {}
            }
            return result[0];
        });
    // Wire intervention processor, prestige and protection to army manager
    gameState.getNobleArmyManager().setInterventionProcessor(
            gameState.getBattleInterventionProcessor(),
            gameState.getArmyManager());
    gameState.getNobleArmyManager().setPlayerPrestige(gameState.getPlayerPrestige());
    gameState.getNobleArmyManager().setProtectionManager(gameState.getProtectionManager());
}

private void wireMapViewCallbacks() {
    mapView.getInfoPanel().setGrantClaimFromMapCallback((zoneId, nhm) -> {
        City.ui.GrantZoneClaimDialog.show(
                this,
                gameState.getActionRegistry().getGrantZoneClaimAction(),
                gameState.getNobleHouseManager(),
                gameState.getZoneManager(),
                gameState.getResources(),
                gameState.getLedger(),
                zoneId,
                () -> {
                    actionsPanel.refresh();
                    mapView.refresh();
                });
    });
    mapView.getInfoPanel().setOpenMilitaryCallback(armyName -> showMilitaryViewWithHighlight(armyName));
}

}