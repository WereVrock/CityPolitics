package main.resources;

import main.parameters.GameParameters;

/**
 * Holds and mutates the four core resources.
 */
public class ResourcePool {

    private int food;
    private int money;
    private int manpower;
    private int influence;

    public ResourcePool() {
        this.food      = GameParameters.STARTING_FOOD;
        this.money     = GameParameters.STARTING_MONEY;
        this.manpower  = GameParameters.STARTING_MANPOWER;
        this.influence = GameParameters.STARTING_INFLUENCE;
    }

    // ─── Food ────────────────────────────────────────────────────────────────

    public int getFood()              { return food; }
    public void addFood(int amount)   { food = Math.max(0, food + amount); }
    public boolean spendFood(int amount) {
        if (food < amount) return false;
        food -= amount;
        return true;
    }

    // ─── Money ───────────────────────────────────────────────────────────────

    public int getMoney()             { return money; }
    public void addMoney(int amount)  { money = Math.max(0, money + amount); }
    public boolean spendMoney(int amount) {
        if (money < amount) return false;
        money -= amount;
        return true;
    }

    // ─── Manpower ────────────────────────────────────────────────────────────

    public int getManpower()              { return manpower; }
    public void addManpower(int amount)   { manpower = Math.max(0, manpower + amount); }
    public boolean spendManpower(int amount) {
        if (manpower < amount) return false;
        manpower -= amount;
        return true;
    }

    // ─── Influence ───────────────────────────────────────────────────────────

    public int getInfluence()             { return influence; }
    public void addInfluence(int amount)  { influence = Math.max(0, influence + amount); }
    public boolean spendInfluence(int amount) {
        if (influence < amount) return false;
        influence -= amount;
        return true;
    }
}