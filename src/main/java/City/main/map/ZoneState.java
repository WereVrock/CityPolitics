package City.main.map;

import City.main.parameters.DiplomacyParams;
 

/**
 * Mutable runtime state for a zone.
 * Tracks damage, supply, raid cooldown, and conquest malus.
 */
public class ZoneState {

    private int    damage;
    private int    supplyLevel;
    private int    recentlyRaidedTurns;  // >0 = unraidable, production malus active
    private int    conquestMalusPercent; // 0-100, decays each turn
    private int    rebellionPower;

    public ZoneState() {
        reset();
    }

    public void reset() {
        this.damage               = 0;
        this.supplyLevel          = 100;
        this.recentlyRaidedTurns  = 0;
        this.conquestMalusPercent = 0;
        this.rebellionPower      = 0;
    }

    // ─── Turn tick ───────────────────────────────────────────────────────────

    public void tick() {
        if (recentlyRaidedTurns > 0) recentlyRaidedTurns--;
        if (conquestMalusPercent > 0) {
            conquestMalusPercent = Math.max(0,
                conquestMalusPercent - DiplomacyParams.CONQUEST_MALUS_DECAY_PER_TURN);
        }
    }

    // ─── Raid ────────────────────────────────────────────────────────────────

    public boolean isRecentlyRaided()  { return recentlyRaidedTurns > 0; }
    public void    markRaided()        { recentlyRaidedTurns = DiplomacyParams.RAID_COOLDOWN_TURNS; }
    public int     getRaidedTurns()    { return recentlyRaidedTurns; }

    // ─── Conquest ────────────────────────────────────────────────────────────

    public void markConquered()        { conquestMalusPercent = 100; }

public void setRecentlyRaidedTurns(int v)  { recentlyRaidedTurns  = Math.max(0, v); }
    public void setConquestMalusPercent(int v)  { conquestMalusPercent = Math.max(0, Math.min(100, v)); }

public int  getConquestMalus()     { return conquestMalusPercent; }
    public boolean hasConquestMalus()  { return conquestMalusPercent > 0; }

    public int  getRebellionPower()          { return rebellionPower; }
    public void setRebellionPower(int v)     { rebellionPower = Math.max(0, v); }
    public void addRebellionPower(int delta) { rebellionPower = Math.max(0, rebellionPower + delta); }

    /**
     * Effective production multiplier accounting for raid, conquest malus,
     * and ravaged status. All stack multiplicatively.
     * Ravaged multiplier is injected by caller via overload.
     */
    public double getProductionMultiplier() {
        return getProductionMultiplier(1.0);
    }

    public double getProductionMultiplier(double ravagedMultiplier) {
        double multiplier = ravagedMultiplier;
        if (isRecentlyRaided()) {
            multiplier *= (1.0 - DiplomacyParams.RAID_PRODUCTION_MALUS);
        }
        if (hasConquestMalus()) {
            multiplier *= (1.0 - conquestMalusPercent / 100.0);
        }
        return multiplier;
    }

    // ─── Damage / supply ─────────────────────────────────────────────────────

    public int  getDamage()               { return damage; }
    public void setDamage(int v)          { damage = Math.max(0, Math.min(100, v)); }
    public void addDamage(int amount)     { setDamage(damage + amount); }
    public int  getSupplyLevel()          { return supplyLevel; }
    public void setSupplyLevel(int v)     { supplyLevel = Math.max(0, Math.min(100, v)); }
}