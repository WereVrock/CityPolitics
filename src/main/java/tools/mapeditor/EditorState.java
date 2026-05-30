package tools.mapeditor;

import main.map.Zone;
import main.map.ZoneManager;

import java.util.*;

public class EditorState {

    private final List<EditableZone> zones = new ArrayList<>();
    private EditableZone selectedZone = null;
    private final Set<String> changedZoneIds = new HashSet<>();

    public EditorState(ZoneManager zoneManager) {
        for (Zone zone : zoneManager.getZones()) {
            zones.add(new EditableZone(zone));
        }
    }

    public List<EditableZone> getZones() { return zones; }

    public EditableZone getSelectedZone() { return selectedZone; }

    public void setSelectedZone(EditableZone zone) { this.selectedZone = zone; }

    public void markChanged(String zoneId) { changedZoneIds.add(zoneId); }

    public Set<String> getChangedZoneIds() { return changedZoneIds; }

    public boolean isChanged(String zoneId) { return changedZoneIds.contains(zoneId); }

    private boolean snappingEnabled = false;
    public boolean isSnappingEnabled()          { return snappingEnabled; }
    public void setSnappingEnabled(boolean val) { this.snappingEnabled = val; }

    /**
     * Returns the nearest vertex from any zone other than the excluded one,
     * within snapRadius world units, or null if none found.
     */
    public int[] findSnapTarget(int wx, int wy, EditableZone exclude, int snapRadius) {
        int bestDist = snapRadius * snapRadius;
        int[] best = null;
        for (EditableZone ez : zones) {
            if (ez == exclude) continue;
            for (int[] v : ez.getVertices()) {
                int dx = v[0] - wx;
                int dy = v[1] - wy;
                int dist = dx * dx + dy * dy;
                if (dist < bestDist) {
                    bestDist = dist;
                    best = v;
                }
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
}