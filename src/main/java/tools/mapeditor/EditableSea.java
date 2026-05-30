package tools.mapeditor;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;

public class EditableSea {

    private final String name;
    private final List<int[]> vertices;
    private int labelX;
    private int labelY;

    public EditableSea(String name, int[] polyX, int[] polyY, int labelX, int labelY) {
        this.name = name;
        this.labelX = labelX;
        this.labelY = labelY;
        this.vertices = new ArrayList<>();
        for (int i = 0; i < polyX.length; i++) {
            vertices.add(new int[]{polyX[i], polyY[i]});
        }
    }

    public String getName() { return name; }
    public List<int[]> getVertices() { return vertices; }
    public int getLabelX() { return labelX; }
    public int getLabelY() { return labelY; }
    public void setLabelX(int x) { this.labelX = x; }
    public void setLabelY(int y) { this.labelY = y; }

    public boolean containsPoint(int wx, int wy) {
        return toPolygon().contains(wx, wy);
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

    public int hitTestVertex(int wx, int wy, int snapRadius) {
        for (int i = 0; i < vertices.size(); i++) {
            int dx = vertices.get(i)[0] - wx;
            int dy = vertices.get(i)[1] - wy;
            if (dx * dx + dy * dy <= snapRadius * snapRadius) return i;
        }
        return -1;
    }

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

    public int[] edgeMidpoint(int edgeIndex) {
        int[] a = vertices.get(edgeIndex);
        int[] b = vertices.get((edgeIndex + 1) % vertices.size());
        return new int[]{(a[0] + b[0]) / 2, (a[1] + b[1]) / 2};
    }

    public void insertVertex(int insertAfterIndex, int x, int y) {
        vertices.add(insertAfterIndex + 1, new int[]{x, y});
    }

    public void removeVertex(int index) {
        if (vertices.size() > 3) vertices.remove(index);
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
        List<int[]> vertsCopy = new ArrayList<>();
        for (int[] v : vertices) vertsCopy.add(new int[]{v[0], v[1]});
        return new SeaSnapshot(name, vertsCopy, labelX, labelY);
    }

    private static class SeaSnapshot implements FeatureSnapshot {
        private final String name;
        private final List<int[]> vertices;
        private final int labelX, labelY;

        SeaSnapshot(String name, List<int[]> vertices, int labelX, int labelY) {
            this.name = name;
            this.vertices = vertices;
            this.labelX = labelX;
            this.labelY = labelY;
        }

        @Override
        public void restore(EditorState state) {
            for (EditableSea s : state.getSeas()) {
                if (s.name.equals(name)) {
                    s.vertices.clear();
                    for (int[] v : vertices) s.vertices.add(new int[]{v[0], v[1]});
                    s.labelX = labelX;
                    s.labelY = labelY;
                    break;
                }
            }
        }
    }

public int[] getPolyX() {
        return vertices.stream().mapToInt(v -> v[0]).toArray();
    }

    public int[] getPolyY() {
        return vertices.stream().mapToInt(v -> v[1]).toArray();
    }

}