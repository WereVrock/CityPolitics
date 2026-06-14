package City.main.combat;

import City.main.parameters.GameParameters;

/**
 * One side in a combat engagement.
 * Stores raw troop count and military skill.
 * Effective power is computed on the fly and used only for battle math.
 */
public class ArmyForce {

    private final String houseId;
    private       int    rawSize;
    private final int    defense;       // 0–100, applies to defender only
    private final int    militarySkill;

    public ArmyForce(String houseId, int rawSize, int defense, int militarySkill) {
        this.houseId       = houseId;
        this.rawSize       = rawSize;
        this.defense       = defense;
        this.militarySkill = militarySkill;
    }

    public String getHouseId()       { return houseId; }
    public int    getRawSize()       { return rawSize; }
    public int    getDefense()       { return defense; }
    public int    getMilitarySkill() { return militarySkill; }

    /** Combat power used for casualty resolution. */
    public int getEffectivePower() {
        return (int)(rawSize * (1.0 + militarySkill * GameParameters.MILITARY_SKILL_BONUS_PER_POINT));
    }

    /** Legacy getter kept for combat log display. Returns effective power. */
    public int getArmySize() {
        return getEffectivePower();
    }

    /** Reduces raw troop count. */
    public void applyLosses(int losses) {
        rawSize = Math.max(0, rawSize - losses);
    }
}