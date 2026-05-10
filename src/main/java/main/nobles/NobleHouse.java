// NobleHouse.java
package main.nobles;

import main.parameters.GameParameters;

import java.util.Collections;
import java.util.List;

/**
 * A noble house with territory, resources, opinion toward the player,
 * and a standing army.
 */
public class NobleHouse {

    public enum Race { HUMAN, ELF, DWARF, ORC }

    private final String       id;
    private final String       name;
    private final String       leaderName;
    private final String       leaderPersonality;
    private final Race         race;
    private final List<String> zoneIds;          // zone IDs this house controls

    // Resources
    private int gold;
    private int manpower;
    private int influence;

    // Opinion toward player: 0–100
    private int playerOpinion;

    // Standing army (free, in capital, no upkeep)
    private final int standingArmySize;

    // Raised army (costs gold to recruit and maintain)
    private int raisedArmySize;

    public NobleHouse(String id, String name, String leaderName, String leaderPersonality,
                      Race race, List<String> zoneIds, int startingGold) {
        this.id                = id;
        this.name              = name;
        this.leaderName        = leaderName;
        this.leaderPersonality = leaderPersonality;
        this.race              = race;
        this.zoneIds           = zoneIds;
        this.gold              = startingGold;
        this.manpower          = 0;
        this.influence         = GameParameters.NOBLE_HOUSE_STARTING_INFLUENCE;
        this.playerOpinion     = GameParameters.NOBLE_HOUSE_STARTING_OPINION;
        this.raisedArmySize    = 0;
        this.standingArmySize  = computeStandingArmySize();
    }

    // ─── Manpower ────────────────────────────────────────────────────────────

    /** Total raw manpower generated per turn across all controlled zones. */
    public int getManpowerPerTurn() {
        return zoneIds.size() * GameParameters.NOBLE_ZONE_MANPOWER_PER_TURN;
    }

    /**
     * Fraction of manpower sent to player this turn.
     * Scales 0–50% based on opinion (0 opinion = 0%, 100 opinion = 50%).
     * If opinion is below hostile threshold, nothing is sent.
     */
    public double getManpowerSendFraction() {
        if (playerOpinion <= GameParameters.NOBLE_HOSTILE_OPINION_THRESHOLD) return 0.0;
        return (playerOpinion / 100.0) * GameParameters.NOBLE_MAX_MANPOWER_SEND_FRACTION;
    }

    /** Manpower sent to player this turn (half the raw total, scaled by opinion). */
    public int computeManpowerSentToPlayer() {
        return (int) Math.floor(getManpowerPerTurn() * getManpowerSendFraction());
    }

    /** Manpower retained by this house this turn. */
    public int computeManpowerRetained() {
        return getManpowerPerTurn() - computeManpowerSentToPlayer();
    }

    // ─── Gold ────────────────────────────────────────────────────────────────

    /** Whether this house sends gold/food to the player (false if hostile). */
    public boolean sendsResourcesToPlayer() {
        return playerOpinion > GameParameters.NOBLE_HOSTILE_OPINION_THRESHOLD;
    }

    // ─── Influence ───────────────────────────────────────────────────────────

    public int getInfluencePerTurn() {
        return (int) Math.floor(
            GameParameters.NOBLE_INFLUENCE_BASE_PER_TURN
            + GameParameters.NOBLE_INFLUENCE_PER_ZONE * zoneIds.size()
        );
    }

    // ─── Standing army ───────────────────────────────────────────────────────

    private int computeStandingArmySize() {
        return getManpowerPerTurn() * GameParameters.NOBLE_STANDING_ARMY_MANPOWER_MULTIPLIER;
    }

    public int getStandingArmySize() { return standingArmySize; }
    public int getRaisedArmySize()   { return raisedArmySize; }
    public int getTotalArmySize()    { return standingArmySize + raisedArmySize; }

    /**
     * Attempt to raise additional soldiers. Returns soldiers actually raised.
     * Costs NOBLE_RECRUIT_COST_PER_SOLDIER gold per soldier.
     */
    public int raiseArmy(int soldiers) {
        int affordable = gold / GameParameters.NOBLE_RECRUIT_COST_PER_SOLDIER;
        int toRaise    = Math.min(soldiers, affordable);
        gold          -= toRaise * GameParameters.NOBLE_RECRUIT_COST_PER_SOLDIER;
        raisedArmySize += toRaise;
        return toRaise;
    }

    /**
     * Pay upkeep for raised army. If gold runs out soldiers disband.
     */
    public void payUpkeep() {
        int cost = raisedArmySize * GameParameters.NOBLE_UPKEEP_COST_PER_SOLDIER;
        if (gold >= cost) {
            gold -= cost;
        } else {
            // Disband as many as needed
            int canAfford  = gold / GameParameters.NOBLE_UPKEEP_COST_PER_SOLDIER;
            raisedArmySize = canAfford;
            gold           = gold - canAfford * GameParameters.NOBLE_UPKEEP_COST_PER_SOLDIER;
        }
    }

    // ─── Accessors / mutators ─────────────────────────────────────────────────

    public String       getId()                { return id; }
    public String       getName()              { return name; }
    public String       getLeaderName()        { return leaderName; }
    public String       getLeaderPersonality() { return leaderPersonality; }
    public Race         getRace()              { return race; }
    public List<String> getZoneIds()           { return Collections.unmodifiableList(zoneIds); }

    public int  getGold()            { return gold; }
    public int  getManpower()        { return manpower; }
    public int  getInfluence()       { return influence; }
    public int  getPlayerOpinion()   { return playerOpinion; }

    public void addGold(int v)       { gold      = Math.max(0, gold      + v); }
    public void addManpower(int v)   { manpower  = Math.max(0, manpower  + v); }
    public void addInfluence(int v)  { influence = Math.max(0, influence + v); }

    public void setPlayerOpinion(int v) {
        playerOpinion = Math.max(GameParameters.NOBLE_OPINION_MIN,
                        Math.min(GameParameters.NOBLE_OPINION_MAX, v));
    }
    public void adjustPlayerOpinion(int delta) { setPlayerOpinion(playerOpinion + delta); }
}