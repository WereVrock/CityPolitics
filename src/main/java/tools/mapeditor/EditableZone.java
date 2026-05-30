package tools.mapeditor;

import main.map.Zone;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class EditableZone {

    private final String id;
    private final String displayName;
    private final Zone.SettlementType settlementType;

    private final List<int[]> vertices; // each int[]{x, y}
    private int labelX;
    private int labelY;

    public EditableZone(Zone zone) {
        this.id             = zone.getId();
        this.displayName    = zone.getDisplayName();
        this.settlementType = zone.getSettlement();
        this.labelX         = zone.getLabelX();
        this.labelY         = zone.getLabelY();

        this.vertices = new ArrayList<>();
        int[] px = zone.getPolyX();
        int[] py = zone.getPolyY();
        for (int i = 0; i < px.length; i++) {
            vertices.add(new int[]{px[i], py[i]});
        }
    }

    public String getId()                        { return id; }
    public String getDisplayName()               { return displayName; }
    public Zone.SettlementType getSettlementType() { return settlementType; }
    public List<int[]> getVertices()             { return vertices; }
    public int getLabelX()                       { return labelX; }
    public int getLabelY()                       { return labelY; }
    public void setLabelX(int x)                { this.labelX = x; }
    public void setLabelY(int y)                { this.labelY = y; }

    public boolean isDesolate() {
        return settlementType == Zone.SettlementType.DESOLATE;
    }

    public boolean containsPoint(int wx, int wy) {
        Polygon poly = toPolygon();
        return poly.contains(wx, wy);
    }

    public Polygon toPolygon() {
        int n = vertices.size();
        int[] px = new int[n];
        int[] py = new int[n];
        for (int i = 0; i < n; i++) {
            px[i] = vertices.get(i)[0];
            py[i] = vertices.get(i)[1];
        }
        return new Polygon(px, py, n);
    }

    public int[] getPolyX() {
        return vertices.stream().mapToInt(v -> v[0]).toArray();
    }

    public int[] getPolyY() {
        return vertices.stream().mapToInt(v -> v[1]).toArray();
    }

    /** Inserts a new vertex after the vertex at insertAfterIndex. */
    public void insertVertex(int insertAfterIndex, int x, int y) {
        vertices.add(insertAfterIndex + 1, new int[]{x, y});
    }

    /** Removes vertex at index, only if zone has more than 3 vertices. */
    public void removeVertex(int index) {
        if (vertices.size() > 3) {
            vertices.remove(index);
        }
    }

    /**
     * Returns the index of a vertex within SNAP_RADIUS pixels of (wx, wy),
     * or -1 if none.
     */
    public int hitTestVertex(int wx, int wy, int snapRadius) {
        for (int i = 0; i < vertices.size(); i++) {
            int dx = vertices.get(i)[0] - wx;
            int dy = vertices.get(i)[1] - wy;
            if (dx * dx + dy * dy <= snapRadius * snapRadius) return i;
        }
        return -1;
    }

    /**
     * Returns the index of the first vertex of the edge closest to (wx, wy)
     * within snapRadius, or -1 if none.
     */
    public int hitTestEdge(int wx, int wy, int snapRadius) {
        int n = vertices.size();
        for (int i = 0; i < n; i++) {
            int[] a = vertices.get(i);
            int[] b = vertices.get((i + 1) % n);
            double dist = pointToSegmentDist(wx, wy, a[0], a[1], b[0], b[1]);
            if (dist <= snapRadius) return i;
        }
        return -1;
    }

    /** Midpoint of edge starting at edgeIndex. */
    public int[] edgeMidpoint(int edgeIndex) {
        int[] a = vertices.get(edgeIndex);
        int[] b = vertices.get((edgeIndex + 1) % vertices.size());
        return new int[]{(a[0] + b[0]) / 2, (a[1] + b[1]) / 2};
    }

    private double pointToSegmentDist(int px, int py,
                                       int ax, int ay, int bx, int by) {
        double dx = bx - ax, dy = by - ay;
        if (dx == 0 && dy == 0) return Math.hypot(px - ax, py - ay);
        double t = ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));
        return Math.hypot(px - (ax + t * dx), py - (ay + t * dy));
    }

    public boolean isLabelHit(int wx, int wy, int snapRadius) {
        int dx = wx - labelX;
        int dy = wy - labelY;
        return dx * dx + dy * dy <= snapRadius * snapRadius;
    }
}