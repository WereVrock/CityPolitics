package City.ui.map;

import City.main.army.Army;
import City.main.army.ArmyManager;
import City.main.barbarians.BarbArmy;
import City.main.map.Zone;
import City.main.map.ZoneManager;
import City.main.nobles.NobleArmy;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.*;
import java.lang.reflect.Field;
import java.util.function.Consumer;

/**
 * Renders the zone map.
 *   Left-click zone       → select zone
 *   Left-click army       → select army (shows details)
 *   Left-click barb army  → select barbarian army
 *   Right-click deployed army (non-heartland) → recall to city
 *   Drag from ArmyListPanel → deploy to dropped zone
 */
public class MapPanel extends JPanel
        implements DropTargetListener, DragGestureListener, DragSourceListener {

    private final ZoneManager    zoneManager;
    private final ArmyManager    armyManager;
    private final Consumer<Zone> onZoneSelected;
    private final Consumer<Army> onArmySelected;
    private final Consumer<NobleArmy>  onNobleArmySelected;
    private final Consumer<BarbArmy>   onBarbArmySelected;
    private final ArmyListPanel        armyListPanel;

    private final MapCamera    camera;
    private final MapRenderer  renderer;
    private final ArmyRenderer armyRenderer;

    private Zone      selectedZone      = null;
    private Zone      hoveredZone       = null;
    private Army      selectedArmy      = null;
    private NobleArmy selectedNobleArmy = null;
    private BarbArmy  selectedBarbArmy  = null;

    private Point dragStart  = null;
    private int   panXAtDrag = 0;
    private int   panYAtDrag = 0;

    private final City.main.core.GameState gameState;
    private City.main.mercenaries.MercenaryArmy selectedMercArmy = null;
    private final java.util.function.Consumer<City.main.mercenaries.MercenaryArmy> onMercArmySelected;
    // merc drop target wired in constructor

    public MapPanel(City.main.core.GameState gameState,
                    Consumer<Zone>     onZoneSelected,
                    Consumer<Army>     onArmySelected,
                    Consumer<NobleArmy> onNobleArmySelected,
                    Consumer<BarbArmy>  onBarbArmySelected,
                    java.util.function.Consumer<City.main.mercenaries.MercenaryArmy> onMercArmySelected,
                    ArmyListPanel      armyListPanel,
                    City.main.nobles.NobleHouseManager nobleHouseManager) {

        this.gameState           = gameState;
        this.zoneManager         = gameState.getZoneManager();
        this.armyManager         = gameState.getArmyManager();
        this.onZoneSelected      = onZoneSelected;
        this.onArmySelected      = onArmySelected;
        this.onNobleArmySelected = onNobleArmySelected;
        this.onBarbArmySelected  = onBarbArmySelected;
        this.onMercArmySelected  = onMercArmySelected;
        this.armyListPanel       = armyListPanel;

        this.camera       = new MapCamera();
        this.renderer     = new MapRenderer(zoneManager,
                gameState.getDecorationRegistry(),
                gameState.getWorldGeography(),
                nobleHouseManager);
        this.armyRenderer = new ArmyRenderer(armyManager, zoneManager);
        this.renderer.setArmyRenderer(armyRenderer);

        NobleArmyRenderer nobleArmyRenderer = new NobleArmyRenderer(
                gameState.getNobleArmyManager(), zoneManager,
                gameState.getNobleHouseManager());
        this.renderer.setNobleArmyRenderer(nobleArmyRenderer);

        BarbArmyRenderer barbArmyRenderer = new BarbArmyRenderer(
                gameState.getBarbArmyManager(), zoneManager);
        this.renderer.setBarbArmyRenderer(barbArmyRenderer);
        this.renderer.setBarbArmyManager(gameState.getBarbArmyManager());
        this.renderer.setRavagedZoneManager(gameState.getRavagedZoneManager());

        City.ui.map.MercenaryArmyRenderer mercArmyRenderer =
                new City.ui.map.MercenaryArmyRenderer(gameState.getMercenaryManager(), zoneManager);
        this.renderer.setMercenaryArmyRenderer(mercArmyRenderer);

        setBackground(MapRenderer.COLOR_BG);
        setPreferredSize(new Dimension(
                City.main.parameters.MapZoneParams.MAP_CANVAS_WIDTH,
                City.main.parameters.MapZoneParams.MAP_CANVAS_HEIGHT));
        new DropTarget(this, DnDConstants.ACTION_MOVE, this, true);
        DragSource.getDefaultDragSource()
                .createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE, this);
        setupMouseHandlers();
    }

    // ─── Selection ───────────────────────────────────────────────────────────

public void clearSelection() {
        selectedZone      = null;
        selectedArmy      = null;
        selectedNobleArmy = null;
        selectedBarbArmy  = null;
        selectedMercArmy  = null;
        renderer.setSelectedNobleArmy(null);
        BarbArmyRenderer br = renderer.getBarbArmyRenderer();
        if (br != null) br.setSelectedArmy(null);
        City.ui.map.MercenaryArmyRenderer mr = renderer.getMercenaryArmyRenderer();
        if (mr != null) mr.setSelectedArmy(null);
        repaint();
    }

public Zone     getSelectedZone()     { return selectedZone; }
    public BarbArmy getSelectedBarbArmy() { return selectedBarbArmy; }

    public void cycleViewMode() {
        renderer.setViewMode(renderer.getViewMode().next());
        repaint();
    }

public void setPickerValidZoneIds(java.util.Set<String> validIds) {
        renderer.setPickerValidZoneIds(validIds);
        repaint();
    }

    public void clearPickerValidZoneIds() {
        renderer.clearPickerValidZoneIds();
        repaint();
    }

public MapViewMode getViewMode() { return renderer.getViewMode(); }

    // ─── Mouse handlers ──────────────────────────────────────────────────────

    private void setupMouseHandlers() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStart  = e.getPoint();
                panXAtDrag = camera.getPanX();
                panYAtDrag = camera.getPanY();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (dragStart == null) return;
                int dx = e.getX() - dragStart.x;
                int dy = e.getY() - dragStart.y;
                if (Math.abs(dx) < 4 && Math.abs(dy) < 4) {
                    if (e.getButton() == MouseEvent.BUTTON3)
                        handleRightClick(e.getPoint());
                    else if (e.getButton() == MouseEvent.BUTTON1)
                        handleLeftClick(e.getPoint());
                }
                dragStart = null;
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStart != null) {
                    camera.setPan(
                            panXAtDrag + (e.getX() - dragStart.x),
                            panYAtDrag + (e.getY() - dragStart.y));
                    repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                Zone hit = zoneAt(e.getPoint());
                if (hit != hoveredZone) { hoveredZone = hit; repaint(); }
            }
        });

        addMouseWheelListener(e -> {
            camera.zoomAt(e.getX(), e.getY(), (float) e.getPreciseWheelRotation());
            repaint();
        });
    }

private void handleLeftClick(Point screenPt) {
        Point world = camera.screenToWorld(screenPt);

        BarbArmyRenderer barbRenderer = renderer.getBarbArmyRenderer();
        City.ui.map.MercenaryArmyRenderer mercRenderer = renderer.getMercenaryArmyRenderer();

        // 1. Barbarian armies (rendered on top)
        if (barbRenderer != null) {
            BarbArmy barbHit = barbRenderer.hitTest(world);
            if (barbHit != null) {
                selectedBarbArmy  = (selectedBarbArmy == barbHit) ? null : barbHit;
                selectedArmy      = null;
                selectedNobleArmy = null;
                selectedMercArmy  = null;
                selectedZone      = null;
                renderer.setSelectedNobleArmy(null);
                barbRenderer.setSelectedArmy(selectedBarbArmy);
                if (mercRenderer != null) mercRenderer.setSelectedArmy(null);
                repaint();
                if (onBarbArmySelected != null) onBarbArmySelected.accept(selectedBarbArmy);
                return;
            }
        }

        // 2. Mercenary armies
        if (mercRenderer != null) {
            City.main.mercenaries.MercenaryArmy mercHit = mercRenderer.hitTest(world);
            if (mercHit != null) {
                selectedMercArmy  = (selectedMercArmy == mercHit) ? null : mercHit;
                selectedArmy      = null;
                selectedNobleArmy = null;
                selectedBarbArmy  = null;
                selectedZone      = null;
                renderer.setSelectedNobleArmy(null);
                if (barbRenderer != null) barbRenderer.setSelectedArmy(null);
                mercRenderer.setSelectedArmy(selectedMercArmy);
                repaint();
                if (onMercArmySelected != null) onMercArmySelected.accept(selectedMercArmy);
                return;
            }
        }

        // 3. Player armies
        Army armyHit = armyRenderer.hitTest(world, zoneManager);
        if (armyHit != null) {
            selectedArmy      = (selectedArmy == armyHit) ? null : armyHit;
            selectedNobleArmy = null;
            selectedBarbArmy  = null;
            selectedMercArmy  = null;
            selectedZone      = null;
            renderer.setSelectedNobleArmy(null);
            if (barbRenderer != null) barbRenderer.setSelectedArmy(null);
            if (mercRenderer != null) mercRenderer.setSelectedArmy(null);
            repaint();
            onArmySelected.accept(selectedArmy);
            return;
        }

        // 4. Noble armies
        NobleArmyRenderer nobleRenderer = renderer.getNobleArmyRenderer();
        if (nobleRenderer != null) {
            NobleArmy nobleHit = nobleRenderer.hitTest(world);
            if (nobleHit != null) {
                selectedNobleArmy = (selectedNobleArmy == nobleHit) ? null : nobleHit;
                selectedArmy      = null;
                selectedBarbArmy  = null;
                selectedMercArmy  = null;
                selectedZone      = null;
                renderer.setSelectedNobleArmy(selectedNobleArmy);
                if (barbRenderer != null) barbRenderer.setSelectedArmy(null);
                if (mercRenderer != null) mercRenderer.setSelectedArmy(null);
                repaint();
                onArmySelected.accept(null);
                if (onNobleArmySelected != null) onNobleArmySelected.accept(selectedNobleArmy);
                return;
            }
        }

        // 5. Zone
        Zone hit      = zoneAt(screenPt);
        selectedZone  = hit;
        selectedArmy  = null;
        selectedNobleArmy = null;
        selectedBarbArmy  = null;
        selectedMercArmy  = null;
        renderer.setSelectedNobleArmy(null);
        if (barbRenderer != null) barbRenderer.setSelectedArmy(null);
        if (mercRenderer != null) mercRenderer.setSelectedArmy(null);
        repaint();
        onZoneSelected.accept(hit);
    }

private void handleRightClick(Point screenPt) {
        Point world = camera.screenToWorld(screenPt);

        // Mercenary armies recall to heartland on right-click, same as player armies.
        City.ui.map.MercenaryArmyRenderer mercRenderer = renderer.getMercenaryArmyRenderer();
        if (mercRenderer != null) {
            City.main.mercenaries.MercenaryArmy mercHit = mercRenderer.hitTest(world);
            if (mercHit != null) {
                if (!City.main.army.Army.HEARTLAND_ID.equals(mercHit.getZoneId())) {
                    mercHit.setZoneId(City.main.army.Army.HEARTLAND_ID);
                    if (selectedMercArmy == mercHit) {
                        selectedMercArmy = null;
                        mercRenderer.setSelectedArmy(null);
                        if (onMercArmySelected != null) onMercArmySelected.accept(null);
                    }
                    armyListPanel.refresh();
                    repaint();
                }
                return;
            }
        }

        Army armyHit = armyRenderer.hitTest(world, zoneManager);
        if (armyHit == null || armyHit.isInCity()) return;
        armyHit.recallToCity();
        if (selectedArmy == armyHit) {
            selectedArmy = null;
            onArmySelected.accept(null);
        }
        armyListPanel.refresh();
        repaint();
    }

private Zone zoneAt(Point screenPt) {
        return renderer.hitTest(camera.screenToWorld(screenPt));
    }

    // ─── Drop target ─────────────────────────────────────────────────────────

    @Override


public void drop(DropTargetDropEvent dtde) {
    try {
        dtde.acceptDrop(DnDConstants.ACTION_MOVE);
        Transferable t = dtde.getTransferable();

        // Merc army drop
        if (t.isDataFlavorSupported(ArmyListPanel.MERC_FLAVOR)) {
            City.main.mercenaries.MercenaryArmy merc =
                (City.main.mercenaries.MercenaryArmy) t.getTransferData(ArmyListPanel.MERC_FLAVOR);
            Zone target = zoneAt(dtde.getLocation());
            if (target == null) {
                dtde.dropComplete(false);
                return;
            }
            merc.setZoneId(target.getId());
            ArmyListPanel.DragDropCallback cb = armyListPanel.getCallback();
            if (cb != null) cb.onDrop(null, target.getId());
            dtde.dropComplete(true);
            repaint();
            return;
        }

        // Normal army drop
        if (!t.isDataFlavorSupported(ArmyListPanel.ARMY_FLAVOR)) {
            dtde.dropComplete(false);
            return;
        }
        Army army   = (Army) t.getTransferData(ArmyListPanel.ARMY_FLAVOR);
        Zone target = zoneAt(dtde.getLocation());
        if (target == null) {
            army.cancelDrag();
            armyListPanel.refresh();
            dtde.dropComplete(false);
            return;
        }
        army.moveTo(target.getId());
        ArmyListPanel.DragDropCallback cb = armyListPanel.getCallback();
        if (cb != null) cb.onDrop(army, target.getId());
        dtde.dropComplete(true);
        repaint();
    } catch (Exception ex) {
        dtde.dropComplete(false);
    }
}

@Override public void dragEnter(DropTargetDragEvent e)         { e.acceptDrag(DnDConstants.ACTION_MOVE); }
    @Override public void dragOver(DropTargetDragEvent e)          { e.acceptDrag(DnDConstants.ACTION_MOVE); }
    @Override public void dropActionChanged(DropTargetDragEvent e) {}
    @Override public void dragExit(DropTargetEvent e)              {}

    // ─── DragGestureListener ─────────────────────────────────────────────────

    private Army draggedFromMap = null;
    private City.main.mercenaries.MercenaryArmy draggedMercFromMap = null;

    @Override
    public void dragGestureRecognized(DragGestureEvent dge) {
        Point world = camera.screenToWorld(dge.getDragOrigin());

        Army armyHit = armyRenderer.hitTest(world, zoneManager);
        if (armyHit != null) {
            draggedFromMap = armyHit;
            armyHit.startDrag();
            armyListPanel.refresh();
            repaint();
            Transferable t = new Transferable() {
                @Override public java.awt.datatransfer.DataFlavor[] getTransferDataFlavors() {
                    return new java.awt.datatransfer.DataFlavor[]{ArmyListPanel.ARMY_FLAVOR};
                }
                @Override public boolean isDataFlavorSupported(java.awt.datatransfer.DataFlavor f) {
                    return f.equals(ArmyListPanel.ARMY_FLAVOR);
                }
                @Override public Object getTransferData(java.awt.datatransfer.DataFlavor f) {
                    return armyHit;
                }
            };
            dge.startDrag(DragSource.DefaultMoveDrop, t, this);
            return;
        }

        City.ui.map.MercenaryArmyRenderer mercRenderer = renderer.getMercenaryArmyRenderer();
        if (mercRenderer != null) {
            City.main.mercenaries.MercenaryArmy mercHit = mercRenderer.hitTest(world);
            if (mercHit != null) {
                draggedMercFromMap = mercHit;
                Transferable t = new Transferable() {
                    @Override public java.awt.datatransfer.DataFlavor[] getTransferDataFlavors() {
                        return new java.awt.datatransfer.DataFlavor[]{ArmyListPanel.MERC_FLAVOR};
                    }
                    @Override public boolean isDataFlavorSupported(java.awt.datatransfer.DataFlavor f) {
                        return f.equals(ArmyListPanel.MERC_FLAVOR);
                    }
                    @Override public Object getTransferData(java.awt.datatransfer.DataFlavor f) {
                        return mercHit;
                    }
                };
                dge.startDrag(DragSource.DefaultMoveDrop, t, this);
            }
        }
    }

    // ─── DragSourceListener ──────────────────────────────────────────────────

    @Override


    public void dragDropEnd(DragSourceDropEvent dsde) {
        if (draggedFromMap != null) {
            if (!dsde.getDropSuccess()) {
                draggedFromMap.cancelDrag();
                armyListPanel.refresh();
                repaint();
            }
            draggedFromMap = null;
            return;
        }
        if (draggedMercFromMap != null) {
            armyListPanel.refresh();
            repaint();
            draggedMercFromMap = null;
        }
    }

@Override public void dragEnter(DragSourceDragEvent e)         {}
    @Override public void dragOver(DragSourceDragEvent e)          {}
    @Override public void dropActionChanged(DragSourceDragEvent e) {}
    @Override public void dragExit(DragSourceEvent e)              {}

    // ─── Paint ───────────────────────────────────────────────────────────────

    @Override

protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        // Clip to this panel's bounds so the map never bleeds into adjacent panels
        g2.setClip(0, 0, getWidth(), getHeight());



        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        camera.applyTransform(g2);
        renderer.render(g2, selectedZone, hoveredZone, selectedArmy);
        g2.dispose();
    }

/**
     * Re-wires all internal references to the new GameState after a reset.
     * Called on new game from MapView.reinitialize().
     */
    public void reinitialize(City.main.core.GameState gs) {
        // Rebuild renderer with fresh subsystem references
        City.main.nobles.NobleHouseManager nhm = gs.getNobleHouseManager();

        renderer.setNobleHouseManager(nhm);
        renderer.setBarbArmyManager(gs.getBarbArmyManager());
        renderer.setRavagedZoneManager(gs.getRavagedZoneManager());

        ArmyRenderer newArmyRenderer = new ArmyRenderer(gs.getArmyManager(), gs.getZoneManager());
        renderer.setArmyRenderer(newArmyRenderer);

        NobleArmyRenderer newNobleRenderer = new NobleArmyRenderer(
                gs.getNobleArmyManager(), gs.getZoneManager(), nhm);
        renderer.setNobleArmyRenderer(newNobleRenderer);

        BarbArmyRenderer newBarbRenderer = new BarbArmyRenderer(
                gs.getBarbArmyManager(), gs.getZoneManager());
        renderer.setBarbArmyRenderer(newBarbRenderer);

        City.ui.map.MercenaryArmyRenderer newMercRenderer =
                new City.ui.map.MercenaryArmyRenderer(gs.getMercenaryManager(), gs.getZoneManager());
        renderer.setMercenaryArmyRenderer(newMercRenderer);

        // Update the armyRenderer reference used for hit-testing in this class
        Field armyRendererField;
        try {
            armyRendererField = MapPanel.class.getDeclaredField("armyRenderer");
            armyRendererField.setAccessible(true);
            armyRendererField.set(this, newArmyRenderer);
        } catch (Exception ignored) {}

        clearSelection();
        repaint();
    }

}