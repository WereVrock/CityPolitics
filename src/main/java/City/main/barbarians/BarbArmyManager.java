package City.main.barbarians;

import City.main.map.Zone;
import City.main.map.ZoneManager;
import City.main.parameters.GameParameters;

import java.util.*;

/**
 * Owns all barbarian armies (mobile + garrisons).
 * Handles spawning, merging, and zone grouping.
 */
public class BarbArmyManager {

    private final List<BarbArmy>              armies  = new ArrayList<>();
    private final Map<String, List<BarbArmy>> byZone  = new LinkedHashMap<>();

    private final ZoneManager zoneManager;
    private final Random      rng = new Random();

    /** Globally visited zones across all barbarian armies this invasion. */
    private final Set<String> invasionVisited = new LinkedHashSet<>();

    public BarbArmyManager(ZoneManager zoneManager) {
        this.zoneManager = zoneManager;
    }

    // ─── Spawn ───────────────────────────────────────────────────────────────

public BarbArmy spawnWarboss(String zoneId, int turn) {
    int size = GameParameters.BARB_WARBOSS_BASE_SIZE
             + turn * GameParameters.BARB_WARBOSS_SIZE_PER_TURN;
    BarbArmy wb = new BarbArmy(BarbArmy.Type.WARBOSS, size, zoneId);
    wb.setDisplayName(BarbTribeNameGenerator.generateWarbossName());
    add(wb);
    invasionVisited.add(zoneId);
    return wb;
}

public BarbArmy spawnWarbossWithSize(String zoneId, int size) {
    BarbArmy wb = new BarbArmy(BarbArmy.Type.WARBOSS, size, zoneId);
    wb.setDisplayName(BarbTribeNameGenerator.generateWarbossName());
    add(wb);
    invasionVisited.add(zoneId);
    return wb;
}

public BarbArmy spawnRaider(String zoneId, int size) {
    return spawnRaider(zoneId, size, false);
}

public BarbArmy spawnRaider(String zoneId, int size, boolean earlyWave) {
    BarbArmy r = new BarbArmy(BarbArmy.Type.RAIDER, size, zoneId);
    r.setDisplayName(BarbTribeNameGenerator.generateRaiderName(earlyWave));
    add(r);
    invasionVisited.add(zoneId);
    return r;
}

public BarbArmy spawnRavager(String zoneId, int size) {
    return spawnRavager(zoneId, size, false);
}

public BarbArmy spawnRavager(String zoneId, int size, boolean earlyWave) {
    BarbArmy r = new BarbArmy(BarbArmy.Type.RAVAGER, size, zoneId);
    r.setDisplayName(BarbTribeNameGenerator.generateRavagerName(earlyWave));
    add(r);
    invasionVisited.add(zoneId);
    return r;
}

/**
     * Directly inserts a pre-built mobile army (used by SaveManager on load).
     * Does not trigger merge logic.
     */
    public void addRestoredArmy(BarbArmy army) {
        armies.add(army);
        byZone.computeIfAbsent(army.getZoneId(), k -> new ArrayList<>()).add(army);
        invasionVisited.addAll(army.getVisitedZones());
    }

// ─── Army access ─────────────────────────────────────────────────────────

    public List<BarbArmy> getAllArmies() {
        return Collections.unmodifiableList(armies);
    }

    /** Mobile armies only (not garrisons, not dismissed). */
    public List<BarbArmy> getMobileArmies() {
        List<BarbArmy> result = new ArrayList<>();
        for (BarbArmy a : armies) {
            if (a.isAlive() && !a.isGarrison()) result.add(a);
        }
        return result;
    }

    public List<BarbArmy> getArmiesInZone(String zoneId) {
        List<BarbArmy> result = new ArrayList<>();
        for (BarbArmy a : byZone.getOrDefault(zoneId, Collections.emptyList())) {
            if (a.isAlive()) result.add(a);
        }
        return result;
    }

    public List<BarbArmy> getGarrisonsInZone(String zoneId) {
        List<BarbArmy> result = new ArrayList<>();
        for (BarbArmy a : byZone.getOrDefault(zoneId, Collections.emptyList())) {
            if (a.isAlive() && a.isGarrison()) result.add(a);
        }
        return result;
    }

    public BarbArmy getWarboss() {
        for (BarbArmy a : armies) {
            if (a.isWarboss() && a.isAlive() && !a.isGarrison()) return a;
        }
        return null;
    }

    /** True if no mobile barbarian armies remain (garrisons don't count). */
    public boolean isInvasionDestroyed() {
        for (BarbArmy a : armies) {
            if (a.isAlive() && !a.isGarrison()) return false;
        }
        return true;
    }

    // ─── Movement ────────────────────────────────────────────────────────────

    public void moveArmy(BarbArmy army, String newZoneId) {
        if (newZoneId == null || newZoneId.equals(army.getZoneId())) return;
        List<BarbArmy> oldList = byZone.get(army.getZoneId());
        if (oldList != null) oldList.remove(army);
        army.setZoneId(newZoneId);
        invasionVisited.add(newZoneId);
        byZone.computeIfAbsent(newZoneId, k -> new ArrayList<>()).add(army);
        tryMergeAt(newZoneId);
    }

    /** Merge raiders/ravagers of same type in same zone. Warboss absorbs all. */
    private void tryMergeAt(String zoneId) {
        List<BarbArmy> zone = byZone.getOrDefault(zoneId, Collections.emptyList());
        BarbArmy warboss = null;
        for (BarbArmy a : new ArrayList<>(zone)) {
            if (a.isWarboss() && a.isAlive() && !a.isGarrison()) { warboss = a; break; }
        }
        if (warboss != null) {
            for (BarbArmy a : new ArrayList<>(zone)) {
                if (a == warboss || !a.isAlive() || a.isGarrison()) continue;
                warboss.setSize(warboss.getSize() + a.getSize());
                remove(a);
            }
            return;
        }
        // Merge raiders with ravagers — ravager keeps identity
        BarbArmy ravager = null;
        for (BarbArmy a : new ArrayList<>(zone)) {
            if (a.isRavager() && a.isAlive() && !a.isGarrison()) { ravager = a; break; }
        }
        if (ravager != null) {
            for (BarbArmy a : new ArrayList<>(zone)) {
                if (a == ravager || !a.isAlive() || a.isGarrison() || !a.isRaider()) continue;
                ravager.setSize(ravager.getSize() + a.getSize());
                remove(a);
            }
        }
    }

    // ─── Removal ─────────────────────────────────────────────────────────────

    public void remove(BarbArmy army) {
        armies.remove(army);
        List<BarbArmy> z = byZone.get(army.getZoneId());
        if (z != null) z.remove(army);
    }

/** Add a pre-built garrison army (called from BarbInvasionProcessor). */
public void addGarrison(BarbArmy garrison) {
    armies.add(garrison);
    byZone.computeIfAbsent(garrison.getZoneId(), k -> new ArrayList<>()).add(garrison);
}

public void removeDeadArmies() {
        for (BarbArmy a : new ArrayList<>(armies)) {
            if (!a.isAlive()) remove(a);
        }
    }

    // ─── Zone picking helpers ─────────────────────────────────────────────────

    /**
     * Pick a zone to move to from candidates, preferring never-invaded zones,
     * then zones this army hasn't personally visited.
     */
    public String pickPreferredZone(BarbArmy army, List<String> candidates) {
        if (candidates.isEmpty()) return null;
        List<String> neverInvaded = new ArrayList<>();
        List<String> notPersonal  = new ArrayList<>();
        for (String z : candidates) {
            if (!invasionVisited.contains(z))    neverInvaded.add(z);
            else if (!army.hasVisited(z))         notPersonal.add(z);
        }
        if (!neverInvaded.isEmpty()) return neverInvaded.get(rng.nextInt(neverInvaded.size()));
        if (!notPersonal.isEmpty())  return notPersonal.get(rng.nextInt(notPersonal.size()));
        return candidates.get(rng.nextInt(candidates.size()));
    }

    /** Adjacent non-desolate zones, falling back to desolate if none found. */
    public List<String> getAdjacentMoveable(String zoneId) {
        Zone zone = zoneManager.getZone(zoneId);
        if (zone == null) return Collections.emptyList();
        List<String> nonDesolate = new ArrayList<>();
        List<String> desolate    = new ArrayList<>();
        for (String adjId : zone.getAdjacentIds()) {
            Zone adj = zoneManager.getZone(adjId);
            if (adj == null) continue;
            if (adj.isDesolate()) desolate.add(adjId);
            else                  nonDesolate.add(adjId);
        }
        return nonDesolate.isEmpty() ? desolate : nonDesolate;
    }

    public Set<String> getInvasionVisited() {
        return Collections.unmodifiableSet(invasionVisited);
    }

public void reset() {
    armies.clear();
    byZone.clear();
    invasionVisited.clear();
    BarbArmy.resetIdCounter();
}

// ─── Internal ────────────────────────────────────────────────────────────

    private void add(BarbArmy army) {
        armies.add(army);
        byZone.computeIfAbsent(army.getZoneId(), k -> new ArrayList<>()).add(army);
    }
}