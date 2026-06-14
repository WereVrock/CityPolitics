package City.tools.mapeditor;

import City.main.map.Zone;
import City.main.map.ZoneDecorationRegistry;
import City.main.map.ZoneManager;
import City.main.map.WorldGeography;

import java.util.*;

public class EditorState {

    private final List<EditableZone> zones;
    private final List<EditableRiver> rivers;
    private final List<EditableSea> seas;

    private EditableZone selectedZone = null;
    private EditableRiver selectedRiver = null;
    private EditableSea selectedSea = null;

    private final Set<String> changedZoneIds = new HashSet<>();
    private final Set<String> changedRiverIds = new HashSet<>();
    private final Set<String> changedSeaIds = new HashSet<>();

    private boolean snappingEnabled = false;

    public EditorState(ZoneManager zoneManager, ZoneDecorationRegistry decorationRegistry,
                       WorldGeography geography) {
        this.zones = new ArrayList<>();
        for (Zone zone : zoneManager.getZones()) {
            zones.add(new EditableZone(zone, decorationRegistry.get(zone.getId()),
                         zone.getAdjacentIds()));
        }

        // Build rivers from geography
        this.rivers = new ArrayList<>();
        for (WorldGeography.River river : geography.getRivers()) {
            int[][] pts = river.getWaypoints();
            int lx = pts[0][0], ly = pts[0][1];
            rivers.add(new EditableRiver(river.getName(), pts, lx, ly));
        }

        // Build seas from geography
        this.seas = new ArrayList<>();
        for (WorldGeography.SeaRegion sea : geography.getSeaRegions()) {
            int[] px = sea.getPolyX();
            int[] py = sea.getPolyY();
            int cx = 0, cy = 0;
            for (int i = 0; i < px.length; i++) { cx += px[i]; cy += py[i]; }
            cx /= px.length; cy /= py.length;
            seas.add(new EditableSea(sea.getName(), px, py, cx, cy));
        }
    }

    public List<EditableZone> getZones() { return zones; }
    public List<EditableRiver> getRivers() { return rivers; }
    public List<EditableSea> getSeas() { return seas; }

    public EditableZone getSelectedZone() { return selectedZone; }
    public EditableRiver getSelectedRiver() { return selectedRiver; }
    public EditableSea getSelectedSea() { return selectedSea; }

    public void setSelectedZone(EditableZone zone) {
        this.selectedZone = zone;
        this.selectedRiver = null;
        this.selectedSea = null;
    }

    public void setSelectedRiver(EditableRiver river) {
        this.selectedRiver = river;
        this.selectedZone = null;
        this.selectedSea = null;
    }

    public void setSelectedSea(EditableSea sea) {
        this.selectedSea = sea;
        this.selectedZone = null;
        this.selectedRiver = null;
    }

    public void clearSelection() {
        selectedZone = null;
        selectedRiver = null;
        selectedSea = null;
    }

    public Object getSelectedFeature() {
        if (selectedZone != null) return selectedZone;
        if (selectedRiver != null) return selectedRiver;
        return selectedSea;
    }

    public void markChanged(String zoneId) { changedZoneIds.add(zoneId); }
    public void markRiverChanged(String riverName) { changedRiverIds.add(riverName); }
    public void markSeaChanged(String seaName) { changedSeaIds.add(seaName); }

private final java.util.Deque<UndoAction> undoStack = new java.util.ArrayDeque<>();

    public void pushUndo(FeatureSnapshot snapshot) {
        if (snapshot != null) {
            undoStack.push(new UndoAction(snapshot));
        }
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            UndoAction action = undoStack.pop();
            action.snapshot.restore(this);
        }
    }

    public static class UndoAction {
        public final FeatureSnapshot snapshot;
        public UndoAction(FeatureSnapshot snapshot) { this.snapshot = snapshot; }
    }

public Set<String> getChangedZoneIds() { return changedZoneIds; }
    public Set<String> getChangedRiverIds() { return changedRiverIds; }
    public Set<String> getChangedSeaIds() { return changedSeaIds; }

    public boolean isChanged(String zoneId) { return changedZoneIds.contains(zoneId); }
    public boolean isRiverChanged(String name) { return changedRiverIds.contains(name); }
    public boolean isSeaChanged(String name) { return changedSeaIds.contains(name); }

private boolean adjacencyMode = false;

    public boolean isAdjacencyModeEnabled() { return adjacencyMode; }
    public void setAdjacencyModeEnabled(boolean enabled) { this.adjacencyMode = enabled; }

    public void toggleAdjacency(EditableZone a, EditableZone b) {
        if (a.isAdjacentTo(b.getId())) {
            a.removeAdjacent(b.getId());
            b.removeAdjacent(a.getId());
        } else {
            a.addAdjacent(b.getId());
            b.addAdjacent(a.getId());
        }
        markChanged(a.getId());
        markChanged(b.getId());
    }

public boolean isSnappingEnabled() { return snappingEnabled; }
    public void setSnappingEnabled(boolean val) { this.snappingEnabled = val; }

    public int[] findSnapTarget(int wx, int wy, Object exclude, int snapRadius) {
        int bestDist = snapRadius * snapRadius;
        int[] best = null;
        // Check all zones' vertices
        for (EditableZone ez : zones) {
            if (ez == exclude) continue;
            for (int[] v : ez.getVertices()) {
                int dx = v[0] - wx, dy = v[1] - wy;
                int dist = dx * dx + dy * dy;
                if (dist < bestDist) { bestDist = dist; best = v; }
            }
        }
        // Rivers' waypoints
        for (EditableRiver r : rivers) {
            if (r == exclude) continue;
            for (int[] v : r.getWaypoints()) {
                int dx = v[0] - wx, dy = v[1] - wy;
                int dist = dx * dx + dy * dy;
                if (dist < bestDist) { bestDist = dist; best = v; }
            }
        }
        // Seas' vertices
        for (EditableSea s : seas) {
            if (s == exclude) continue;
            for (int[] v : s.getVertices()) {
                int dx = v[0] - wx, dy = v[1] - wy;
                int dist = dx * dx + dy * dy;
                if (dist < bestDist) { bestDist = dist; best = v; }
            }
        }
        return best;
    }

    public EditableZone getZoneAt(int worldX, int worldY) {
        for (EditableZone ez : zones) {
            if (ez.containsPoint(worldX, worldY)) return ez;
        }
        return null;
    }

    public EditableRiver getRiverAt(int worldX, int worldY, int snap) {
        for (EditableRiver r : rivers) {
            if (r.hitTestPolyline(worldX, worldY, snap)) return r;
        }
        return null;
    }

    public EditableSea getSeaAt(int worldX, int worldY) {
        for (EditableSea s : seas) {
            if (s.containsPoint(worldX, worldY)) return s;
        }
        return null;
    }
}