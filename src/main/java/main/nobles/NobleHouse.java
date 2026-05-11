package main.nobles;

import main.parameters.GameParameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A noble house with territory, resources, opinion toward the player,
 * army, prestige, defense, and an active character driving AI behavior.
 */
public class NobleHouse {

    public enum Race { HUMAN, ELF, DWARF, ORC }

    private final String               id;
    private final String               name;
    private final Race                 race;
    private final List<String>         zoneIds;
    private final List<NobleCharacter> characters;
    private final int                  activeCharacterIndex;

    private int gold;
    private int manpower;
    private int influence;
    private int playerOpinion;
    private int prestige;
    private int defense;
    private int standingArmySize;
    private int raisedArmySize;

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
        this.manpower             = 0;
        this.influence            = GameParameters.NOBLE_HOUSE_STARTING_INFLUENCE;
        this.playerOpinion        = GameParameters.NOBLE_HOUSE_STARTING_OPINION;
        this.prestige             = startingPrestige;
        this.defense              = GameParameters.NOBLE_STARTING_DEFENSE;
        this.raisedArmySize       = 0;
        this.standingArmySize     = computeStandingArmySize();
    }

    // ─── Elimination ─────────────────────────────────────────────────────────

    public boolean isEliminated() { return zoneIds.isEmpty(); }

    // ─── Character ───────────────────────────────────────────────────────────

    public NobleCharacter      getActiveCharacter() {
        if (characters.isEmpty()) return null;
        return characters.get(activeCharacterIndex);
    }

    public List<NobleCharacter> getCharacters() {
        return Collections.unmodifiableList(characters);
    }

    // ─── Prestige ────────────────────────────────────────────────────────────

    public int     getPrestige()       { return prestige; }
    public Prestige getPrestigeLevel() { return Prestige.fromValue(prestige); }
    public void addPrestige(int delta) {
        prestige = Math.max(0, Math.min(100, prestige + delta));
    }

    // ─── Defense ─────────────────────────────────────────────────────────────

    public int  getDefense()           { return defense; }
    public void addDefense(int delta)  {
        defense = Math.max(0, Math.min(100, defense + delta));
    }

    // ─── Manpower ────────────────────────────────────────────────────────────

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

    // ─── Gold ────────────────────────────────────────────────────────────────

    public boolean sendsResourcesToPlayer() {
        return playerOpinion > GameParameters.NOBLE_HOSTILE_OPINION_THRESHOLD;
    }

    // ─── Influence ───────────────────────────────────────────────────────────

    public int getInfluencePerTurn() {
        int prestigeBonus = (int)(prestige
            * GameParameters.NOBLE_INFLUENCE_PRESTIGE_FACTOR);
        return (int) Math.floor(
            GameParameters.NOBLE_INFLUENCE_BASE_PER_TURN
            + GameParameters.NOBLE_INFLUENCE_PER_ZONE * zoneIds.size()
            + prestigeBonus
        );
    }

    // ─── Army ────────────────────────────────────────────────────────────────

    private int computeStandingArmySize() {
        return zoneIds.size()
            * GameParameters.NOBLE_ZONE_MANPOWER_PER_TURN
            * GameParameters.NOBLE_STANDING_ARMY_MANPOWER_MULTIPLIER;
    }

    public int getStandingArmySize()  { return standingArmySize; }
    public int getRaisedArmySize()    { return raisedArmySize; }
    public int getTotalArmySize()     { return standingArmySize + raisedArmySize; }

    public void setTotalArmySize(int size) {
        int total = Math.max(0, size);
        if (total >= standingArmySize) {
            raisedArmySize = total - standingArmySize;
        } else {
            standingArmySize = total;
            raisedArmySize   = 0;
        }
    }

    public void addToRaisedArmy(int soldiers) {
        raisedArmySize = Math.max(0, raisedArmySize + soldiers);
    }

    public int raiseArmy(int soldiers) {
        int affordable = gold / GameParameters.NOBLE_RECRUIT_COST_PER_SOLDIER;
        int toRaise    = Math.min(soldiers, affordable);
        gold          -= toRaise * GameParameters.NOBLE_RECRUIT_COST_PER_SOLDIER;
        raisedArmySize += toRaise;
        return toRaise;
    }

    public void payUpkeep() {
        int cost = raisedArmySize * GameParameters.NOBLE_UPKEEP_COST_PER_SOLDIER;
        if (gold >= cost) {
            gold -= cost;
        } else {
            int canAfford  = gold / GameParameters.NOBLE_UPKEEP_COST_PER_SOLDIER;
            raisedArmySize = canAfford;
            gold           = gold - canAfford
                * GameParameters.NOBLE_UPKEEP_COST_PER_SOLDIER;
        }
    }

    // ─── Zone management ─────────────────────────────────────────────────────

    public void addZone(String zoneId) {
        if (!zoneIds.contains(zoneId)) {
            zoneIds.add(zoneId);
            standingArmySize = computeStandingArmySize();
        }
    }

    public void removeZone(String zoneId) {
        zoneIds.remove(zoneId);
        standingArmySize = computeStandingArmySize();
    }

    // ─── Accessors ───────────────────────────────────────────────────────────

    public String       getId()   { return id; }
    public String       getName() { return name; }
    public Race         getRace() { return race; }

    public String getLeaderName() {
        NobleCharacter c = getActiveCharacter();
        return c != null ? c.getName() : name;
    }

    public String getLeaderPersonality() {
        NobleCharacter c = getActiveCharacter();
        return c != null ? c.getPersonality() : "";
    }

    public List<String> getZoneIds()         { return Collections.unmodifiableList(zoneIds); }
    public int          getGold()            { return gold; }
    public int          getManpower()        { return manpower; }
    public int          getInfluence()       { return influence; }
    public int          getPlayerOpinion()   { return playerOpinion; }

    public void addGold(int v)      { gold      = Math.max(0, gold      + v); }
    public void addManpower(int v)  { manpower  = Math.max(0, manpower  + v); }
    public void addInfluence(int v) { influence = Math.max(0, influence + v); }

    public void setPlayerOpinion(int v) {
        playerOpinion = Math.max(GameParameters.NOBLE_OPINION_MIN,
                        Math.min(GameParameters.NOBLE_OPINION_MAX, v));
    }

    public void adjustPlayerOpinion(int delta) {
        setPlayerOpinion(playerOpinion + delta);
    }
}