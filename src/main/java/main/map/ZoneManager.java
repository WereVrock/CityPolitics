// ZoneManager.java
package main.map;

import main.parameters.GameParameters;

import java.util.*;

/**
* Owns all Zone definitions and their mutable ZoneState.
* Provides BFS distance between zones.
*/
public class ZoneManager {

private final List<Zone>             zones;
private final Map<String, Zone>      zoneById;
private final Map<String, ZoneState> stateById;

public ZoneManager() {
this.zones     = buildZones();
this.zoneById  = new LinkedHashMap<>();
this.stateById = new LinkedHashMap<>();
for (Zone z : zones) {
zoneById.put(z.getId(), z);
stateById.put(z.getId(), new ZoneState());
}
}

public void reset() {
for (ZoneState s : stateById.values()) s.reset();
}

public List<Zone>  getZones()         { return Collections.unmodifiableList(zones); }
public Zone        getZone(String id) { return zoneById.get(id); }
public ZoneState   getState(String id){ return stateById.get(id); }
public boolean     hasZone(String id) { return zoneById.containsKey(id); }

public int distance(String fromId, String toId) {
if (fromId.equals(toId)) return 0;
Set<String>          visited = new HashSet<>();
Queue<String>        queue   = new LinkedList<>();
Map<String, Integer> dist    = new HashMap<>();
queue.add(fromId);
visited.add(fromId);
dist.put(fromId, 0);
while (!queue.isEmpty()) {
String cur = queue.poll();
int d = dist.get(cur);
Zone z = zoneById.get(cur);
if (z == null) continue;
for (String nb : z.getAdjacentIds()) {
if (!visited.contains(nb)) {
visited.add(nb);
dist.put(nb, d + 1);
if (nb.equals(toId)) return d + 1;
queue.add(nb);
}
}
}
return Integer.MAX_VALUE;
}

// ─── Zone definitions — 1200×700 canvas ──────────────────────────────────

private static List<Zone> buildZones() {
List<Zone> list = new ArrayList<>();

list.add(new Zone("heartland", "Heartland", Zone.SettlementType.CAPITAL,
new int[]{203, 339, 370, 340, 240, 191},
new int[]{265, 240, 300, 450, 460, 381},
270, 340,
GameParameters.ZONE_CAPITAL_GOLD, GameParameters.ZONE_CAPITAL_FOOD, GameParameters.ZONE_CAPITAL_POPS,
List.of("northern_vale", "westgate", "greenvale", "river_bend", "eastern_plains")));

list.add(new Zone("snowmarch", "Snowmarch", Zone.SettlementType.VILLAGE,
new int[]{173, 182, 219, 97, 95, 96},
new int[]{108, 131, 261, 206, 204, 100},
130, 175,
GameParameters.ZONE_VILLAGE_GOLD, GameParameters.ZONE_VILLAGE_FOOD, GameParameters.ZONE_VILLAGE_POPS,
List.of("northern_vale")));

list.add(new Zone("northern_vale", "Northern Vale", Zone.SettlementType.TOWN,
new int[]{173, 320, 340, 340, 220, 201},
new int[]{108, 100, 190, 240, 260, 202},
255, 175,
GameParameters.ZONE_TOWN_GOLD, GameParameters.ZONE_TOWN_FOOD, GameParameters.ZONE_TOWN_POPS,
List.of("snowmarch", "far_north", "heartland")));

list.add(new Zone("far_north", "Far North", Zone.SettlementType.VILLAGE,
new int[]{320, 480, 500, 470, 340, 341},
new int[]{100, 90, 180, 220, 240, 194},
410, 165,
GameParameters.ZONE_VILLAGE_GOLD, GameParameters.ZONE_VILLAGE_FOOD, GameParameters.ZONE_VILLAGE_POPS,
List.of("northern_vale", "iceveil_tundra")));

list.add(new Zone("iceveil_tundra", "Iceveil Tundra", Zone.SettlementType.VILLAGE,
new int[]{480, 660, 680, 650, 500, 501},
new int[]{90, 90, 180, 220, 230, 184},
570, 160,
GameParameters.ZONE_VILLAGE_GOLD, GameParameters.ZONE_VILLAGE_FOOD, GameParameters.ZONE_VILLAGE_POPS,
List.of("far_north", "ashfield", "trade_coast", "waste_northeast")));

list.add(new Zone("westgate", "Westgate", Zone.SettlementType.TOWN,
new int[]{93, 168, 200, 190, 110, 60},
new int[]{206, 239, 254, 380, 420, 419},
120, 300,
GameParameters.ZONE_TOWN_GOLD, GameParameters.ZONE_TOWN_FOOD, GameParameters.ZONE_TOWN_POPS,
List.of("heartland", "greenvale")));

list.add(new Zone("frostpeak_pass", "Frostpeak Pass", Zone.SettlementType.VILLAGE,
new int[]{340, 470, 464, 402, 400, 370},
new int[]{240, 220, 369, 371, 370, 300},
415, 295,
GameParameters.ZONE_VILLAGE_GOLD, GameParameters.ZONE_VILLAGE_FOOD, GameParameters.ZONE_VILLAGE_POPS,
List.of("eastern_plains")));

list.add(new Zone("eastern_plains", "Eastern Plains", Zone.SettlementType.VILLAGE,
new int[]{370, 400, 460, 480, 430, 340},
new int[]{300, 370, 370, 440, 470, 449},
415, 410,
GameParameters.ZONE_VILLAGE_GOLD, GameParameters.ZONE_VILLAGE_FOOD, GameParameters.ZONE_VILLAGE_POPS,
List.of("heartland", "frostpeak_pass", "river_bend", "ashfield")));

list.add(new Zone("ashfield", "Ashfield", Zone.SettlementType.VILLAGE,
new int[]{464, 500, 630, 640, 620, 480},
new int[]{364, 232, 227, 383, 460, 440},
565, 385,
GameParameters.ZONE_VILLAGE_GOLD, GameParameters.ZONE_VILLAGE_FOOD, GameParameters.ZONE_VILLAGE_POPS,
List.of("iceveil_tundra", "eastern_plains", "trade_coast", "stonepass")));

list.add(new Zone("trade_coast", "Trade Coast", Zone.SettlementType.TOWN,
new int[]{650, 680, 770, 780, 789, 744, 670, 640},
new int[]{220, 180, 220, 310, 347, 370, 380, 310},
710, 295,
GameParameters.ZONE_TOWN_GOLD, GameParameters.ZONE_TOWN_FOOD, GameParameters.ZONE_TOWN_POPS,
List.of("iceveil_tundra", "ashfield", "far_east", "waste_northeast")));

list.add(new Zone("stonepass", "Stonepass", Zone.SettlementType.VILLAGE,
new int[]{640, 739, 767, 748, 660, 620},
new int[]{380, 373, 458, 494, 510, 460},
700, 440,
GameParameters.ZONE_VILLAGE_GOLD, GameParameters.ZONE_VILLAGE_FOOD, GameParameters.ZONE_VILLAGE_POPS,
List.of("ashfield", "highland_gap", "thornwood", "ashenveil")));

list.add(new Zone("highland_gap", "Highland Gap", Zone.SettlementType.VILLAGE,
new int[]{480, 620, 660, 630, 490, 430},
new int[]{440, 460, 510, 560, 570, 470},
555, 505,
GameParameters.ZONE_VILLAGE_GOLD, GameParameters.ZONE_VILLAGE_FOOD, GameParameters.ZONE_VILLAGE_POPS,
List.of("stonepass", "duskfall")));

list.add(new Zone("far_east", "Far East", Zone.SettlementType.TOWN,
new int[]{747, 789, 897, 880, 865, 841, 773, 767},
new int[]{371, 347, 375, 460, 487, 489, 494, 458},
840, 435,
GameParameters.ZONE_TOWN_GOLD, GameParameters.ZONE_TOWN_FOOD, GameParameters.ZONE_TOWN_POPS,
List.of("trade_coast", "bramblewood", "waste_east")));

list.add(new Zone("greenvale", "Greenvale", Zone.SettlementType.VILLAGE,
new int[]{110, 190, 240, 218, 117, 90},
new int[]{420, 380, 461, 528, 548, 480},
155, 475,
GameParameters.ZONE_VILLAGE_GOLD, GameParameters.ZONE_VILLAGE_FOOD, GameParameters.ZONE_VILLAGE_POPS,
List.of("westgate", "heartland", "river_bend", "southern_march", "waste_southwest")));

list.add(new Zone("river_bend", "River Bend", Zone.SettlementType.VILLAGE,
new int[]{240, 340, 399, 429, 400, 280, 220},
new int[]{460, 450, 464, 487, 570, 590, 530},
320, 520,
GameParameters.ZONE_VILLAGE_GOLD, GameParameters.ZONE_VILLAGE_FOOD, GameParameters.ZONE_VILLAGE_POPS,
List.of("heartland", "greenvale", "eastern_plains", "duskfall", "wetmarsh")));

list.add(new Zone("southern_march", "Southern March", Zone.SettlementType.TOWN,
new int[]{110, 220, 280, 202, 129, 90, 100},
new int[]{550, 530, 591, 640, 622, 600, 575},
160, 595,
GameParameters.ZONE_TOWN_GOLD, GameParameters.ZONE_TOWN_FOOD, GameParameters.ZONE_TOWN_POPS,
List.of("greenvale", "ironhaven")));

list.add(new Zone("duskfall", "Duskfall", Zone.SettlementType.VILLAGE,
new int[]{429, 490, 520, 640, 420, 403},
new int[]{487, 570, 570, 635, 650, 569},
450, 580,
GameParameters.ZONE_VILLAGE_GOLD, GameParameters.ZONE_VILLAGE_FOOD, GameParameters.ZONE_VILLAGE_POPS,
List.of("river_bend", "highland_gap", "wetmarsh", "redcliff", "saltmere")));

list.add(new Zone("redcliff", "Redcliff", Zone.SettlementType.VILLAGE,
new int[]{522, 656, 651, 639, 584, 541},
new int[]{570, 558, 580, 630, 604, 582},
575, 575,
GameParameters.ZONE_VILLAGE_GOLD, GameParameters.ZONE_VILLAGE_FOOD, GameParameters.ZONE_VILLAGE_POPS,
List.of("duskfall", "ashenveil")));

list.add(new Zone("thornwood", "Thornwood", Zone.SettlementType.VILLAGE,
new int[]{750, 883, 898, 870, 770, 700},
new int[]{496, 489, 561, 600, 610, 504},
820, 545,
GameParameters.ZONE_VILLAGE_GOLD, GameParameters.ZONE_VILLAGE_FOOD, GameParameters.ZONE_VILLAGE_POPS,
List.of("bramblewood", "port_reach", "ashenveil", "stonepass")));

list.add(new Zone("bramblewood", "Bramblewood", Zone.SettlementType.VILLAGE,
new int[]{880, 883, 917, 980, 1000, 970, 878, 898},
new int[]{460, 443, 451, 460, 540, 590, 597, 561},
935, 525,
GameParameters.ZONE_VILLAGE_GOLD, GameParameters.ZONE_VILLAGE_FOOD, GameParameters.ZONE_VILLAGE_POPS,
List.of("far_east", "thornwood", "port_reach", "waste_east", "waste_se_upper")));

list.add(new Zone("ironhaven", "Ironhaven", Zone.SettlementType.VILLAGE,
new int[]{90, 130, 200, 197, 110, 80, 85},
new int[]{600, 624, 641, 675, 690, 645, 622},
150, 645,
GameParameters.ZONE_VILLAGE_GOLD, GameParameters.ZONE_VILLAGE_FOOD, GameParameters.ZONE_VILLAGE_POPS,
List.of("southern_march", "wetmarsh", "waste_farSW")));

list.add(new Zone("wetmarsh", "Wetmarsh", Zone.SettlementType.VILLAGE,
new int[]{280, 400, 407, 413, 362, 230, 200},
new int[]{590, 571, 586, 650, 663, 680, 640},
360, 635,
GameParameters.ZONE_VILLAGE_GOLD, GameParameters.ZONE_VILLAGE_FOOD, GameParameters.ZONE_VILLAGE_POPS,
List.of("river_bend", "duskfall", "saltmere", "ironhaven")));

list.add(new Zone("ashenveil", "Ashenveil", Zone.SettlementType.VILLAGE,
new int[]{669, 699, 769, 756, 677, 639},
new int[]{508, 504, 612, 638, 639, 635},
707, 600,
GameParameters.ZONE_VILLAGE_GOLD, GameParameters.ZONE_VILLAGE_FOOD, GameParameters.ZONE_VILLAGE_POPS,
List.of("stonepass", "redcliff", "thornwood", "saltmere")));

list.add(new Zone("port_reach", "Port Reach", Zone.SettlementType.TOWN,
new int[]{770, 901, 971, 969, 775, 766},
new int[]{614, 600, 592, 647, 641, 621},
882, 624,
GameParameters.ZONE_TOWN_GOLD, GameParameters.ZONE_TOWN_FOOD, GameParameters.ZONE_TOWN_POPS,
List.of("thornwood", "bramblewood", "waste_se_upper", "waste_se_lower")));

list.add(new Zone("saltmere", "Saltmere", Zone.SettlementType.TOWN,
new int[]{279, 418, 633, 760, 707, 630, 414, 335},
new int[]{672, 652, 636, 640, 654, 659, 682, 681},
485, 656,
GameParameters.ZONE_TOWN_GOLD, GameParameters.ZONE_TOWN_FOOD, GameParameters.ZONE_TOWN_POPS,
List.of("wetmarsh", "duskfall", "ashenveil")));

// ── Desolate wilderness zones ─────────────────────────────────────────

list.add(new Zone("waste_northeast", "The Ashen Reaches", Zone.SettlementType.DESOLATE,
new int[]{708, 816, 953, 770, 680, 660},
new int[]{90, 80, 151, 220, 180, 90},
720, 155,
0, 0, 0,
List.of("iceveil_tundra", "trade_coast")));

list.add(new Zone("waste_east", "The Sundered Expanse", Zone.SettlementType.DESOLATE,
new int[]{900, 1089, 1100, 991, 980, 949, 903, 887},
new int[]{375, 425, 490, 493, 460, 454, 446, 422},
975, 440,
0, 0, 0,
List.of("far_east", "bramblewood")));

list.add(new Zone("waste_se_upper", "The Rotting Shore", Zone.SettlementType.DESOLATE,
new int[]{991, 1100, 1110, 1080, 980, 971, 1000},
new int[]{493, 490, 580, 620, 610, 592, 540},
1040, 565,
0, 0, 0,
List.of("bramblewood", "port_reach")));

list.add(new Zone("waste_se_lower", "The Drowned Cliffs", Zone.SettlementType.DESOLATE,
new int[]{970, 1100, 1110, 1090, 975, 966},
new int[]{610, 580, 640, 680, 670, 640},
1030, 640,
0, 0, 0,
List.of("port_reach")));

list.add(new Zone("waste_southwest", "The Forsaken Moor", Zone.SettlementType.DESOLATE,
new int[]{55, 110, 90, 111, 110, 90, 50, 30},
new int[]{420, 418, 480, 533, 550, 600, 590, 490},
72, 505,
0, 0, 0,
List.of("greenvale")));

list.add(new Zone("waste_farSW", "The Hollow Reaches", Zone.SettlementType.DESOLATE,
new int[]{30, 90, 80, 110, 65, 25, 10},
new int[]{590, 600, 645, 690, 692, 690, 635},
55, 645,
0, 0, 0,
List.of("ironhaven")));

return list;
}

}

















