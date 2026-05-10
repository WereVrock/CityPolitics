package main.nobles.combat;

/**
 * One side in a combat engagement.
 */
public class ArmyForce {

    private final String houseId;
    private       int    armySize;
    private final int    defense;   // 0–100, applies to defender only

    public ArmyForce(String houseId, int armySize, int defense) {
        this.houseId  = houseId;
        this.armySize = armySize;
        this.defense  = defense;
    }

    public String getHouseId() { return houseId; }
    public int    getArmySize(){ return armySize; }
    public int    getDefense() { return defense; }

    public void applyLosses(int losses) {
        armySize = Math.max(0, armySize - losses);
    }
}