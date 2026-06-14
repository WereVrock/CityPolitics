package City.main.mercenaries;

import City.debug.Debug;
import City.main.army.Army;
import City.main.army.ArmyManager;
import City.main.parameters.GameParameters;
import City.main.resources.ResourcePool;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Handles mercenary army creation from existing player armies.
 * The player picks an existing army (with commander) to serve as a mercenary unit.
 * Recruitment cost is randomised ±MERCENARY_RECRUIT_COST_VARIANCE.
 */
public class MercenaryRecruitmentHandler {

    private static final Random RNG = new Random();

    private MercenaryRecruitmentHandler() {}

    /**
     * Returns armies eligible to be "hired" as mercenaries:
     * must be in heartland, have a living commander, and have soldiers.
     */
    public static List<Army> getEligibleArmies(ArmyManager armyManager) {
        List<Army> result = new ArrayList<>();
        for (Army a : armyManager.getCityArmies()) {
            if (a.hasLivingCommander() && a.getSize() > 0) {
                result.add(a);
            }
        }
        return result;
    }

    /**
     * Computes the gold cost to hire a specific army as mercenaries.
     * Base = soldiers × recruit_cost × MERCENARY_COST_MULTIPLIER, ±15% random variance.
     */
    public static int computeHireCost(Army army) {
        double basePerSoldier = GameParameters.SOLDIER_RECRUIT_GOLD_COST
                * GameParameters.MERCENARY_COST_MULTIPLIER;
        double variance = 1.0 + (RNG.nextDouble() * 2 - 1)
                * GameParameters.MERCENARY_RECRUIT_COST_VARIANCE;
        return (int) Math.ceil(army.getSize() * basePerSoldier * variance);
    }

    /**
     * Recompute a deterministic preview cost (no variance) for display before commit.
     */
    public static int computeHireCostPreview(Army army) {
        return (int) Math.ceil(army.getSize()
                * GameParameters.SOLDIER_RECRUIT_GOLD_COST
                * GameParameters.MERCENARY_COST_MULTIPLIER);
    }

    /**
     * Hire the given army as a mercenary company.
     * Converts the player army into a MercenaryArmy entry. The Army object
     * remains in ArmyManager — mercenaries operate independently.
     * Returns null if player can't afford.
     */
    public static MercenaryArmy hire(Army army, ResourcePool resources,
                                      MercenaryManager mercenaryManager) {
        int cost = computeHireCost(army);
        if (resources.getMoney() < cost) {
            Debug.log("merc-recruitment", "hire-fail",
                    army.getDisplayName() + " — cannot afford cost=" + cost);
            return null;
        }
        resources.spendMoney(cost);
        // Create merc entry at army's current zone (heartland)
        MercenaryArmy merc = new MercenaryArmy(
                army.getDisplayName() + " (Merc)",
                army.getSize(),
                army.getZoneId());
        mercenaryManager.addFromArmy(merc);
        Debug.log("merc-recruitment", "hired",
                army.getDisplayName() + " cost=" + cost + " size=" + army.getSize());
        return merc;
    }
}