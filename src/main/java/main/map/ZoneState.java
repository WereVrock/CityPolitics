// ZoneState.java
package main.map;

/**
 * Mutable runtime state for a zone (damage, supply level).
 * Kept separate from the immutable Zone definition.
 */
public class ZoneState {

    private int damage;       // 0–100
    private int supplyLevel;  // 0–100

    public ZoneState() {
        this.damage      = 0;
        this.supplyLevel = 100;
    }

    public void reset() {
        this.damage      = 0;
        this.supplyLevel = 100;
    }

    public int  getDamage()               { return damage; }
    public void setDamage(int v)          { this.damage = Math.max(0, Math.min(100, v)); }
    public void addDamage(int amount)     { setDamage(damage + amount); }

    public int  getSupplyLevel()          { return supplyLevel; }
    public void setSupplyLevel(int v)     { this.supplyLevel = Math.max(0, Math.min(100, v)); }
}