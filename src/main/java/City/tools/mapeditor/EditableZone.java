package City.tools.mapeditor;

import City.main.map.Zone;
import City.main.map.ZoneDecoration;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class EditableZone {

private final String id;
private final String displayName;
private final Zone.SettlementType settlementType;
private ZoneDecoration.TerrainSymbol terrainSymbol;
private final List<int[]> mountainEdges; // each int[]{indexA, indexB}
private final java.util.Set<String> adjacentZoneIds;

private final List<int[]> vertices; // each int[]{x, y}
private int labelX;
private int labelY;

public EditableZone(Zone zone, ZoneDecoration decoration, List<String> adjacentIds) {
this.id             = zone.getId();
this.displayName    = zone.getDisplayName();
this.settlementType = zone.getSettlement();
this.labelX         = zone.getLabelX();
this.labelY         = zone.getLabelY();
this.terrainSymbol  = decoration.getSymbol();
this.mountainEdges  = new ArrayList<>(decoration.getMountainEdges().size());
for (int[] edge : decoration.getMountainEdges()) {
mountainEdges.add(new int[]{edge[0], edge[1]});
}
this.adjacentZoneIds = new java.util.HashSet<>(adjacentIds);

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

public ZoneDecoration.TerrainSymbol getTerrainSymbol()   { return terrainSymbol; }
public void setTerrainSymbol(ZoneDecoration.TerrainSymbol sym) { this.terrainSymbol = sym; }

public List<int[]> getMountainEdges()       { return mountainEdges; }
public void addMountainEdge(int a, int b) {
if (!isMountainEdge(a)) {
mountainEdges.add(new int[]{a, b});
}
}
public void removeMountainEdge(int edgeIndex) {
int n = vertices.size();
if (edgeIndex < 0 || edgeIndex >= n) return;
mountainEdges.removeIf(e ->
(e[0] == edgeIndex && e[1] == (edgeIndex + 1) % n) ||
(e[0] == (edgeIndex + 1) % n && e[1] == edgeIndex));
}
public boolean isMountainEdge(int edgeIndex) {
int n = vertices.size();
if (edgeIndex < 0 || edgeIndex >= n) return false;
int next = (edgeIndex + 1) % n;
for (int[] e : mountainEdges) {
if ((e[0] == edgeIndex && e[1] == next) || (e[0] == next && e[1] == edgeIndex))
return true;
}
return false;
}
public void toggleMountainEdge(int edgeIndex) {
if (isMountainEdge(edgeIndex)) {
removeMountainEdge(edgeIndex);
} else {
int n = vertices.size();
if (edgeIndex < 0 || edgeIndex >= n) return;
addMountainEdge(edgeIndex, (edgeIndex + 1) % n);
}
}

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

public void insertVertex(int insertAfterIndex, int x, int y) {
vertices.add(insertAfterIndex + 1, new int[]{x, y});
// Adjust mountain edges that cross the split
int n = vertices.size() - 1; // count before insertion
if (n == 0) return;
int oldEdgeIdx = insertAfterIndex;
int oldNext = (oldEdgeIdx + 1) % n;
List<int[]> toAdd = new ArrayList<>();
mountainEdges.removeIf(e -> {
if (e[0] == oldEdgeIdx && e[1] == oldNext) { toAdd.add(new int[]{oldEdgeIdx, oldEdgeIdx+1}); toAdd.add(new int[]{oldEdgeIdx+1, oldEdgeIdx+2}); return true; }
if (e[0] == oldNext && e[1] == oldEdgeIdx) { toAdd.add(new int[]{oldEdgeIdx+1, oldEdgeIdx}); toAdd.add(new int[]{oldEdgeIdx+2, oldEdgeIdx+1}); return true; }
return false;
});
mountainEdges.addAll(toAdd);
}

public void removeVertex(int index) {
if (vertices.size() <= 3) return;
int n = vertices.size();
int prevIdx = (index - 1 + n) % n;
int nextIdx = (index + 1) % n;
// Remove mountain edges involving vertex index
mountainEdges.removeIf(e -> e[0] == index || e[1] == index);
// Merge mountain edges that would connect prev->index and index->next
boolean hasPrevIdx = mountainEdges.stream().anyMatch(e -> (e[0] == prevIdx && e[1] == index) || (e[0] == index && e[1] == prevIdx));
boolean hasNextIdx = mountainEdges.stream().anyMatch(e -> (e[0] == index && e[1] == nextIdx) || (e[0] == nextIdx && e[1] == index));
mountainEdges.removeIf(e -> (e[0] == prevIdx && e[1] == index) || (e[0] == index && e[1] == prevIdx) || (e[0] == index && e[1] == nextIdx) || (e[0] == nextIdx && e[1] == index));
if (hasPrevIdx && hasNextIdx && prevIdx != nextIdx) {
mountainEdges.add(new int[]{prevIdx, nextIdx});
}
vertices.remove(index);
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

public FeatureSnapshot createSnapshot() {
List<int[]> vertsCopy = new ArrayList<>();
for (int[] v : vertices) vertsCopy.add(new int[]{v[0], v[1]});
List<int[]> edgesCopy = new ArrayList<>();
for (int[] e : mountainEdges) edgesCopy.add(new int[]{e[0], e[1]});
java.util.Set<String> adjCopy = new java.util.HashSet<>(adjacentZoneIds);
return new ZoneSnapshot(id, vertsCopy, labelX, labelY, terrainSymbol, edgesCopy, adjCopy);
}

private static class ZoneSnapshot implements FeatureSnapshot {
private final String id;
private final List<int[]> vertices;
private final int labelX, labelY;
private final ZoneDecoration.TerrainSymbol terrainSymbol;
private final List<int[]> mountainEdges;
private final java.util.Set<String> adjacentIds;

ZoneSnapshot(String id, List<int[]> vertices, int labelX, int labelY,
ZoneDecoration.TerrainSymbol terrainSymbol, List<int[]> mountainEdges,
java.util.Set<String> adjacentIds) {
this.id = id;
this.vertices = vertices;
this.labelX = labelX;
this.labelY = labelY;
this.terrainSymbol = terrainSymbol;
this.mountainEdges = mountainEdges;
this.adjacentIds = adjacentIds;
}

@Override
public void restore(EditorState state) {
for (EditableZone z : state.getZones()) {
if (z.id.equals(id)) {
z.vertices.clear();
for (int[] v : vertices) z.vertices.add(new int[]{v[0], v[1]});
z.labelX = labelX;
z.labelY = labelY;
z.terrainSymbol = terrainSymbol;
z.mountainEdges.clear();
for (int[] e : mountainEdges) z.mountainEdges.add(new int[]{e[0], e[1]});
z.adjacentZoneIds.clear();
z.adjacentZoneIds.addAll(adjacentIds);
break;
}
}
}
}

public boolean isAdjacentTo(String id) { return adjacentZoneIds.contains(id); }
public void addAdjacent(String id) { adjacentZoneIds.add(id); }
public void removeAdjacent(String id) { adjacentZoneIds.remove(id); }
public java.util.Set<String> getAdjacentIds() { return adjacentZoneIds; }

}

