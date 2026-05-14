// MapPanel.java
package ui.map;

import main.army.Army;
import main.army.ArmyManager;
import main.map.Zone;
import main.map.ZoneManager;
import main.nobles.NobleArmy;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.*;
import java.util.function.Consumer;

/**
* Renders the zone map.
*   Left-click zone  → select zone
*   Left-click army  → select army (shows details)
*   Right-click deployed army (non-heartland) → recall to city
*   Drag from ArmyListPanel → deploy to dropped zone
*   Drop on heartland or cancel → army returns to city
*/
public class MapPanel extends JPanel implements DropTargetListener, DragGestureListener, DragSourceListener {

private final ZoneManager    zoneManager;
private final ArmyManager    armyManager;
private final Consumer<Zone> onZoneSelected;
private final Consumer<Army> onArmySelected;
private final ArmyListPanel  armyListPanel;

private final MapCamera    camera;
private final MapRenderer  renderer;
private final ArmyRenderer armyRenderer;

private Zone      selectedZone      = null;
private Zone      hoveredZone       = null;
private Army      selectedArmy      = null;
private NobleArmy selectedNobleArmy = null;

private Point dragStart  = null;
private int   panXAtDrag = 0;
private int   panYAtDrag = 0;

private final main.core.GameState gameState;

public MapPanel(main.core.GameState gameState,
Consumer<Zone> onZoneSelected, Consumer<Army> onArmySelected,
ArmyListPanel armyListPanel,
main.nobles.NobleHouseManager nobleHouseManager) {
this.gameState      = gameState;
this.zoneManager    = gameState.getZoneManager();
this.armyManager    = gameState.getArmyManager();
this.onZoneSelected = onZoneSelected;
this.onArmySelected = onArmySelected;
this.armyListPanel  = armyListPanel;

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

setBackground(MapRenderer.COLOR_BG);
setPreferredSize(new Dimension(
main.parameters.GameParameters.MAP_CANVAS_WIDTH,
main.parameters.GameParameters.MAP_CANVAS_HEIGHT));
new DropTarget(this, DnDConstants.ACTION_MOVE, this, true);
DragSource.getDefaultDragSource()
.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_MOVE, this);
setupMouseHandlers();
}

public void clearSelection() {
selectedZone      = null;
selectedArmy      = null;
selectedNobleArmy = null;
renderer.setSelectedNobleArmy(null);
repaint();
}

public void cycleViewMode() {
renderer.setViewMode(renderer.getViewMode().next());
repaint();
}

public MapViewMode getViewMode() {
return renderer.getViewMode();
}

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
if (e.getButton() == MouseEvent.BUTTON3)      handleRightClick(e.getPoint());
else if (e.getButton() == MouseEvent.BUTTON1) handleLeftClick(e.getPoint());
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
panYAtDrag + (e.getY() - dragStart.y)
);
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
Point world  = camera.screenToWorld(screenPt);
Army armyHit = armyRenderer.hitTest(world, zoneManager);

if (armyHit != null) {
selectedArmy      = (selectedArmy == armyHit) ? null : armyHit;
selectedNobleArmy = null;
selectedZone      = null;
renderer.setSelectedNobleArmy(null);
repaint();
onArmySelected.accept(selectedArmy);
return;
}

// Check noble armies
NobleArmyRenderer nobleRenderer = renderer.getNobleArmyRenderer();
if (nobleRenderer != null) {
NobleArmy nobleHit = nobleRenderer.hitTest(world);
if (nobleHit != null) {
selectedNobleArmy = (selectedNobleArmy == nobleHit) ? null : nobleHit;
selectedArmy      = null;
selectedZone      = null;
renderer.setSelectedNobleArmy(selectedNobleArmy);
repaint();
onArmySelected.accept(null);
onNobleArmySelected(selectedNobleArmy);
return;
}
}

Zone hit = zoneAt(screenPt);
selectedZone      = hit;
selectedArmy      = null;
selectedNobleArmy = null;
renderer.setSelectedNobleArmy(null);
repaint();
onZoneSelected.accept(hit);
}



private void onNobleArmySelected(NobleArmy army) {
if (army == null) return;
main.nobles.NobleHouse house =
gameState.getNobleHouseManager().getHouseById(army.getHouseId());
String houseName = house != null ? house.getName() : army.getHouseId();
String order = army.hasPendingOrder()
? army.getPendingOrder().name() + " → " + army.getPendingTargetZoneId()
: "None";
javax.swing.JOptionPane.showMessageDialog(this,
houseName + " Army\n"
+ "Size: " + army.getSize() + "\n"
+ "Zone: " + army.getZoneId() + "\n"
+ "Pending order: " + order,
houseName + " Army",
javax.swing.JOptionPane.PLAIN_MESSAGE);
}

private void handleRightClick(Point screenPt) {
Point world  = camera.screenToWorld(screenPt);
Army armyHit = armyRenderer.hitTest(world, zoneManager);
if (armyHit == null) return;
if (armyHit.isInCity()) return; // right-click on heartland army does nothing

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

// Drop on heartland = return to city
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

// ─── DragGestureListener — drag army directly from map ───────────────────

private Army draggedFromMap = null;

@Override
public void dragGestureRecognized(DragGestureEvent dge) {
Point world   = camera.screenToWorld(dge.getDragOrigin());
Army armyHit  = armyRenderer.hitTest(world, zoneManager);
if (armyHit == null) return;

draggedFromMap = armyHit;
armyHit.startDrag();
armyListPanel.refresh();
repaint();

Transferable t = new java.awt.datatransfer.Transferable() {
@Override public java.awt.datatransfer.DataFlavor[] getTransferDataFlavors() { return new java.awt.datatransfer.DataFlavor[]{ArmyListPanel.ARMY_FLAVOR}; }
@Override public boolean isDataFlavorSupported(java.awt.datatransfer.DataFlavor f) { return f.equals(ArmyListPanel.ARMY_FLAVOR); }
@Override public Object getTransferData(java.awt.datatransfer.DataFlavor f) { return armyHit; }
};
dge.startDrag(DragSource.DefaultMoveDrop, t, this);
}

// ─── DragSourceListener ──────────────────────────────────────────────────

@Override
public void dragDropEnd(DragSourceDropEvent dsde) {
if (draggedFromMap == null) return;
if (!dsde.getDropSuccess()) {
draggedFromMap.cancelDrag();
armyListPanel.refresh();
repaint();
}
draggedFromMap = null;
}

@Override public void dragEnter(DragSourceDragEvent e)         {}
@Override public void dragOver(DragSourceDragEvent e)          {}
@Override public void dropActionChanged(DragSourceDragEvent e) {}
@Override public void dragExit(DragSourceEvent e)              {}

@Override
protected void paintComponent(Graphics g) {
super.paintComponent(g);
Graphics2D g2 = (Graphics2D) g.create();
g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
camera.applyTransform(g2);
renderer.render(g2, selectedZone, hoveredZone, selectedArmy);
g2.dispose();
}
}


