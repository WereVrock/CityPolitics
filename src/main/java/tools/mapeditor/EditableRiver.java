package tools.mapeditor;

import java.util.ArrayList;
import java.util.List;

public class EditableRiver {

    private final String name;
    private final List<int[]> waypoints; // each int[]{x, y}
    private int labelX;
    private int labelY;

    public EditableRiver(String name, int[][] waypoints, int labelX, int labelY) {
        this.name = name;
        this.waypoints = new ArrayList<>();
        for (int[] pt : waypoints) {
            this.waypoints.add(new int[]{pt[0], pt[1]});
        }
        this.labelX = labelX;
        this.labelY = labelY;
    }

    public String getName() { return name; }
    public List<int[]> getWaypoints() { return waypoints; }
    public int getLabelX() { return labelX; }
    public int getLabelY() { return labelY; }
    public void setLabelX(int x) { this.labelX = x; }
    public void setLabelY(int y) { this.labelY = y; }

    public boolean hitTestWaypoint(int wx, int wy, int snapRadius) {
        for (int i = 0; i < waypoints.size(); i++) {
            int dx = waypoints.get(i)[0] - wx;
            int dy = waypoints.get(i)[1] - wy;
            if (dx * dx + dy * dy <= snapRadius * snapRadius) return true;
        }
        return false;
    }

    public int hitTestWaypointIndex(int wx, int wy, int snapRadius) {
        for (int i = 0; i < waypoints.size(); i++) {
            int dx = waypoints.get(i)[0] - wx;
            int dy = waypoints.get(i)[1] - wy;
            if (dx * dx + dy * dy <= snapRadius * snapRadius) return i;
        }
        return -1;
    }

    public int hitTestSegment(int wx, int wy, int snapRadius) {
        for (int i = 0; i < waypoints.size() - 1; i++) {
            int[] a = waypoints.get(i);
            int[] b = waypoints.get(i + 1);
            double dist = pointToSegmentDist(wx, wy, a[0], a[1], b[0], b[1]);
            if (dist <= snapRadius) return i;
        }
        return -1;
    }

    public boolean hitTestPolyline(int wx, int wy, int threshold) {
        return hitTestSegment(wx, wy, threshold) >= 0;
    }

    public void insertWaypoint(int afterIndex, int x, int y) {
        waypoints.add(afterIndex + 1, new int[]{x, y});
    }

    public void removeWaypoint(int index) {
        if (waypoints.size() > 2) waypoints.remove(index);
    }

    public int[] edgeMidpoint(int edgeIndex) {
        int[] a = waypoints.get(edgeIndex);
        int[] b = waypoints.get(edgeIndex + 1);
        return new int[]{(a[0] + b[0]) / 2, (a[1] + b[1]) / 2};
    }

    public boolean isLabelHit(int wx, int wy, int snapRadius) {
        int dx = wx - labelX;
        int dy = wy - labelY;
        return dx * dx + dy * dy <= snapRadius * snapRadius;
    }

    private double pointToSegmentDist(int px, int py, int ax, int ay, int bx, int by) {
        double dx = bx - ax, dy = by - ay;
        if (dx == 0 && dy == 0) return Math.hypot(px - ax, py - ay);
        double t = ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));
        return Math.hypot(px - (ax + t * dx), py - (ay + t * dy));
    }

public FeatureSnapshot createSnapshot() {
        List<int[]> ptsCopy = new ArrayList<>();
        for (int[] p : waypoints) ptsCopy.add(new int[]{p[0], p[1]});
        return new RiverSnapshot(name, ptsCopy, labelX, labelY);
    }

    private static class RiverSnapshot implements FeatureSnapshot {
        private final String name;
        private final List<int[]> waypoints;
        private final int labelX, labelY;

        RiverSnapshot(String name, List<int[]> waypoints, int labelX, int labelY) {
            this.name = name;
            this.waypoints = waypoints;
            this.labelX = labelX;
            this.labelY = labelY;
        }

        @Override
        public void restore(EditorState state) {
            for (EditableRiver r : state.getRivers()) {
                if (r.name.equals(name)) {
                    r.waypoints.clear();
                    for (int[] p : waypoints) r.waypoints.add(new int[]{p[0], p[1]});
                    r.labelX = labelX;
                    r.labelY = labelY;
                    break;
                }
            }
        }
    }

}