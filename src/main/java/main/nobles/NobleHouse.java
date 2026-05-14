// NobleHouse.java
package main.nobles;

import main.parameters.GameParameters;

import java.util.*;

/**
 * A noble house with territory, resources, opinion, army, prestige,
 * capital tracking, per-zone fortification, garrison, and noble-side manpower pool.
 *
 * Noble manpower is SEPARATE from player manpower (ResourcePool).
 * It accumulates each turn from zones and is spent to recruit noble armies.
 */
public class NobleHouse {

    public enum Race { HUMAN, ELF, DWARF, ORC }

    private final String               id;
    private final String               name;
    private final Race                 race;
    private final List<String>         zoneIds;
    private final List<NobleCharacter> characters;
    private final int                  activeCharacterIndex;

    private int     gold;
    private int     nobleManpower;   // house-side pool, NOT player manpower
    private int     influence;
    private int     playerOpinion;
    private int     prestige;
    private boolean threatened;

    // Per-zone fortification level (0–100) and garrison size
    private final Map<String, Integer> fortifications = new LinkedHashMap<>();
    private final Map<String, Integer> garrisons      = new LinkedHashMap<>();

    // Capital zone — recalculated when zones change
    private String capitalZoneId;

    public NobleHouse(String id, String name, Race race,
                      List<String> zoneIds,
                      List<NobleCharacter> characters,
                      int startingGold, int startingPrestige) {
        this.id                   = id;
        this.name                 = name;
        this.race                 = race;
        this.zoneIds              = new ArrayList<>(zoneIds);
        this.characters           = new ArrayList<>(characters);
        this.activeCharacterIndex = 0;
        this.gold                 = startingGold;
        this.nobleManpower        = 0;
        this.influence            = GameParameters.NOBLE_HOUSE_STARTING_INFLUENCE;
        this.playerOpinion        = GameParameters.NOBLE_HOUSE_STARTING_OPINION;
        this.prestige             = startingPrestige;
        this.threatened           = false;

        for (String z : zoneIds) {
            fortifications.put(z, 0);
            garrisons.put(z, 0);
        }
        recalculateCapital();
    }

    // ─── Capital ─────────────────────────────────────────────────────────────

    /**
     * Capital = zone with highest fortification.
     * Tie → highest gold production proxy (we use garrison as proxy since zone data
     * is not held here; caller should use the overload that passes zone data).
     * Recalculated whenever zones change.
     */
    public void recalculateCapital() {
        if (zoneIds.isEmpty()) { capitalZoneId = null; return; }
        String best     = zoneIds.get(0);
        int    bestFort = fortifications.getOrDefault(best, 0);
        int    bestGarr = garrisons.getOrDefault(best, 0);

        for (String z : zoneIds) {
            int fort = fortifications.getOrDefault(z, 0);
            int garr = garrisons.getOrDefault(z, 0);
            if (fort > bestFort || (fort == bestFort && garr > bestGarr)) {
                best     = z;
                bestFort = fort;
                bestGarr = garr;
            }
        }
        capitalZoneId = best;
    }

    /**
     * Capital selection using external zone gold/food as tiebreakers.
     * Preferred overload — pass zone gold and food maps.
     */
    public void recalculateCapital(Map<String, Integer> zoneGold,
                                    Map<String, Integer> zoneFood) {
        if (zoneIds.isEmpty()) { capitalZoneId = null; return; }
        String best     = zoneIds.get(0);
        int    bestFort = fortifications.getOrDefault(best, 0);
        int    bestGold = zoneGold.getOrDefault(best, 0);
        int    bestFood = zoneFood.getOrDefault(best, 0);

        for (String z : zoneIds) {
            int fort = fortifications.getOrDefault(z, 0);
            int gold = zoneGold.getOrDefault(z, 0);
            int food = zoneFood.getOrDefault(z, 0);
            if (fort > bestFort
                || (fort == bestFort && gold > bestGold)
                || (fort == bestFort && gold == bestGold && food > bestFood)) {
                best     = z;
                bestFort = fort;
                bestGold = gold;
                bestFood = food;
            }
        }
        capitalZoneId = best;
    }

    public String getCapitalZoneId() { return capitalZoneId; }
    public boolean isCapital(String zoneId) {
        return zoneId != null && zoneId.equals(capitalZoneId);
    }

    // ─── Fortification ───────────────────────────────────────────────────────

    public int getFortificationFor(String zoneId) {
        return fortifications.getOrDefault(zoneId, 0);
    }

    public void addFortification(String zoneId, int amount) {
        if (!zoneIds.contains(zoneId)) return;
        int current = fortifications.getOrDefault(zoneId, 0);
        fortifications.put(zoneId, Math.max(0, Math.min(100, current + amount)));
        recalculateCapital();
    }

    // ─── Garrison ────────────────────────────────────────────────────────────

    /**
     * Maximum garrison for a zone = capital gets GARRISON_CAPITAL_MULTIPLIER × manpower/turn,
     * others get GARRISON_OTHER_MULTIPLIER × manpower/turn.
     */
    public int getMaxGarrisonFor(String zoneId) {
        int manpowerPerTurn = GameParameters.NOBLE_ZONE_MANPOWER_PER_TURN;
        if (zoneId.equals(capitalZoneId)) {
            return manpowerPerTurn * GameParameters.GARRISON_CAPITAL_MULTIPLIER;
        }
        return manpowerPerTurn * GameParameters.GARRISON_OTHER_MULTIPLIER;
    }

    public int getGarrisonFor(String zoneId) {
        return garrisons.getOrDefault(zoneId, 0);
    }

    /**
     * Tick garrison — fill toward max using retained noble manpower.
     * Called every turn after manpower accrues.
     */
    public void tickGarrisons() {
        for (String z : new ArrayList<>(zoneIds)) {
            int current = garrisons.getOrDefault(z, 0);
            int max     = getMaxGarrisonFor(z);
            if (current < max) {
                int refill = Math.min(max - current, nobleManpower);
                garrisons.put(z, current + refill);
                nobleManpower = Math.max(0, nobleManpower - refill);
            }
        }
    }

    public void damageGarrison(String zoneId, int losses) {
        int current = garrisons.getOrDefault(zoneId, 0);
        garrisons.put(zoneId, Math.max(0, current - losses));
    }

    public void resetGarrison(String zoneId) {
        garrisons.put(zoneId, 0);
    }

    // ─── Noble manpower (house-side, NOT player resource) ────────────────────

    public int  getNobleManpower()          { return nobleManpower; }
    public void addNobleManpower(int v)     { nobleManpower = Math.max(0, nobleManpower + v); }
    public void spendNobleManpower(int v)   { nobleManpower = Math.max(0, nobleManpower - v); }

    /**
     * Manpower generated per turn from all zones (goes to noble pool).
     * Player receives a fraction based on opinion — handled in NobleHouseManager.
     */
    public int getManpowerPerTurn() {
        return zoneIds.size() * GameParameters.NOBLE_ZONE_MANPOWER_PER_TURN;
    }

    public double getManpowerSendFraction() {
        if (playerOpinion <= GameParameters.NOBLE_HOSTILE_OPINION_THRESHOLD) return 0.0;
        return (playerOpinion / 100.0) * GameParameters.NOBLE_MAX_MANPOWER_SEND_FRACTION;
    }

    public int computeManpowerSentToPlayer() {
        return (int) Math.floor(getManpowerPerTurn() * getManpowerSendFraction());
    }

    public int computeManpowerRetained() {
        return getManpowerPerTurn() - computeManpowerSentToPlayer();
    }

    public boolean sendsResourcesToPlayer() {
        return playerOpinion > GameParameters.NOBLE_HOSTILE_OPINION_THRESHOLD;
    }

    // ─── Elimination ─────────────────────────────────────────────────────────

    public boolean isEliminated() { return zoneIds.isEmpty(); }

    // ─── Threatened ──────────────────────────────────────────────────────────

    public boolean isThreatened()           { return threatened; }
    public void    setThreatened(boolean v) { this.threatened = v; }

    // ─── Military score ──────────────────────────────────────────────────────

    public int getMilitaryScore() {
        NobleCharacter c = getActiveCharacter();
        int skill = c != null ? c.getMilitary() : 0;
        return (int)(getTotalGarrisonSize() * (1.0 + skill
            * GameParameters.MILITARY_SKILL_BONUS_PER_POINT));
    }

    public int getTotalGarrisonSize() {
        int total = 0;
        for (int g : garrisons.values()) total += g;
        return total;
    }

    // ─── Character ───────────────────────────────────────────────────────────

    public NobleCharacter getActiveCharacter() {
        if (characters.isEmpty()) return null;
        return characters.get(activeCharacterIndex);
    }

    public List<NobleCharacter> getCharacters() {
        return Collections.unmodifiableList(characters);
    }

    // ─── Prestige ────────────────────────────────────────────────────────────

    public int      getPrestige()      { return prestige; }
    public Prestige getPrestigeLevel() { return Prestige.fromValue(prestige); }
    public void addPrestige(int delta) {
        prestige = Math.max(0, Math.min(100, prestige + delta));
    }

    // ─── Influence ───────────────────────────────────────────────────────────

    public int getInfluencePerTurn() {
        int prestigeBonus = (int)(prestige * GameParameters.NOBLE_INFLUENCE_PRESTIGE_FACTOR);
        return (int) Math.floor(
            GameParameters.NOBLE_INFLUENCE_BASE_PER_TURN
            + GameParameters.NOBLE_INFLUENCE_PER_ZONE * zoneIds.size()
            + prestigeBonus
        );
    }

    // ─── Zone management ─────────────────────────────────────────────────────

    public void addZone(String zoneId) {
        if (!zoneIds.contains(zoneId)) {
            zoneIds.add(zoneId);
            fortifications.put(zoneId, 0);
            garrisons.put(zoneId, 0);
            recalculateCapital();
        }
    }

    public void removeZone(String zoneId) {
        zoneIds.remove(zoneId);
        fortifications.remove(zoneId);
        garrisons.remove(zoneId);
        recalculateCapital();
    }

    // ─── Accessors ───────────────────────────────────────────────────────────

    public String getId()   { return id; }
    public String getName() { return name; }
    public Race   getRace() { return race; }

    public String getLeaderName() {
        NobleCharacter c = getActiveCharacter();
        return c != null ? c.getName() : name;
    }

    public String getLeaderPersonality() {
        NobleCharacter c = getActiveCharacter();
        return c != null ? c.getPersonality() : "";
    }

    public List<String> getZoneIds()       { return Collections.unmodifiableList(zoneIds); }
    public int          getGold()          { return gold; }
    public int          getInfluence()     { return influence; }
    public int          getPlayerOpinion() { return playerOpinion; }

    public void addGold(int v)      { gold      = Math.max(0, gold + v); }
    public void addInfluence(int v) { influence = Math.max(0, influence + v); }

    public void setPlayerOpinion(int v) {
        playerOpinion = Math.max(GameParameters.NOBLE_OPINION_MIN,
                        Math.min(GameParameters.NOBLE_OPINION_MAX, v));
    }

    public void adjustPlayerOpinion(int delta) {
        setPlayerOpinion(playerOpinion + delta);
    }

    // ─── Legacy compatibility (army size for AI combat scoring) ──────────────

    /** Used by coalition/demand AI that still references army size. Returns total garrison. */
    public int getTotalArmySize() { return getTotalGarrisonSize(); }

    /** No-op kept for save compatibility — noble armies are now separate objects. */
    public void setTotalArmySize(int size) { /* handled by NobleArmyManager */ }
    public void addToRaisedArmy(int soldiers) { addNobleManpower(soldiers); }

    // ─── Kept for NobleHousesPanel / MapInfoPanel display ────────────────────

    public int getStandingArmySize() { return getTotalGarrisonSize(); }
    /** @deprecated Returns manpower pool, not field armies. Use NobleHouseManager.getRaisedArmyTotal() instead. */
    @Deprecated
    public int getRaisedArmySize()   { return nobleManpower; }
    public int getManpower()         { return nobleManpower; }
    public int getDefense()          { return getFortificationFor(capitalZoneId != null ? capitalZoneId : ""); }
    public void addDefense(int delta){ if (capitalZoneId != null) addFortification(capitalZoneId, delta); }
}

