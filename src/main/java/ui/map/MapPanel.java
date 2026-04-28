package ui.map;

import main.map.Zone;
import main.map.ZoneManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.Consumer;
import main.army.Army;
import main.army.ArmyManager;

/**
 * Custom JPanel that renders the zone map.
 * Handles zoom, pan (drag), and zone selection via click.
 */
public class MapPanel extends JPanel {

    private final ZoneManager    zoneManager;
    private final Consumer<Zone> onZoneSelected;

    private final MapCamera     camera;
    private final MapRenderer   renderer;
    private final ArmyRenderer  armyRenderer;
    private final ArmyManager   armyManager;
    private final Consumer<Army> onArmySelected;

    private Zone  selectedZone  = null;
    private Zone  hoveredZone   = null;
    private Army  selectedArmy  = null;

    private Point dragStart  = null;
    private int   panXAtDrag = 0;
    private int   panYAtDrag = 0;

    public MapPanel(ZoneManager zoneManager, ArmyManager armyManager,
                    Consumer<Zone> onZoneSelected, Consumer<Army> onArmySelected) {
        this.zoneManager    = zoneManager;
        this.armyManager    = armyManager;
        this.onZoneSelected = onZoneSelected;
        this.onArmySelected = onArmySelected;

        this.camera        = new MapCamera();
        this.renderer      = new MapRenderer(zoneManager);
        this.armyRenderer  = new ArmyRenderer(armyManager, zoneManager);
        this.renderer.setArmyRenderer(armyRenderer);

        setBackground(MapRenderer.COLOR_BG);
        setPreferredSize(new Dimension(800, 520));
        setupMouseHandlers();
    }

    // ─── Selection ────────────────────────────────────────────────────────────

    public Zone getSelectedZone() {
        return selectedZone;
    }

    // ─── Mouse ────────────────────────────────────────────────────────────────

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
                Point rel = e.getPoint();
                if (dragStart != null) {
                    int dx = rel.x - dragStart.x;
                    int dy = rel.y - dragStart.y;
                    if (Math.abs(dx) < 4 && Math.abs(dy) < 4) {
                        handleClick(rel);
                    }
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
                Zone hit = zoneAtScreenPoint(e.getPoint());
                if (hit != hoveredZone) {
                    hoveredZone = hit;
                    repaint();
                }
            }
        });

        addMouseWheelListener(e -> {
            camera.zoomAt(e.getX(), e.getY(), (float) e.getPreciseWheelRotation());
            repaint();
        });
    }

    private void handleClick(Point screenPt) {
        Point world   = camera.screenToWorld(screenPt);
        Army armyHit  = armyRenderer.hitTest(world, zoneManager);

        if (armyHit != null) {
            selectedArmy = (selectedArmy == armyHit) ? null : armyHit;
            selectedZone = null;
            repaint();
            onArmySelected.accept(selectedArmy);
            onZoneSelected.accept(null);
            return;
        }

        Zone hit = zoneAtScreenPoint(screenPt);

        // If an army is selected and user clicks a zone → issue order
        if (selectedArmy != null && hit != null) {
            onZoneSelected.accept(hit);    // still update info panel
            issueOrderToSelectedArmy(hit);
            return;
        }

        selectedZone = hit;
        selectedArmy = null;
        repaint();
        onZoneSelected.accept(hit);
        onArmySelected.accept(null);
    }

    private void issueOrderToSelectedArmy(Zone target) {
        if (selectedArmy == null) return;
        armyManager.issueMoveOrder(selectedArmy, target.getId());
        repaint();
    }

    public void clearSelection() {
        selectedZone = null;
        selectedArmy = null;
        repaint();
    }

    public Army getSelectedArmy() { return selectedArmy; }

    // ─── Hit testing ──────────────────────────────────────────────────────────

    private Zone zoneAtScreenPoint(Point screenPt) {
        Point world = camera.screenToWorld(screenPt);
        return renderer.hitTest(world);
    }

    // ─── Painting ─────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        camera.applyTransform(g2);
        renderer.render(g2, selectedZone, hoveredZone, selectedArmy);

        g2.dispose();
    }
}