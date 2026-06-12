package main.mercenaries;

import debug.Debug;
import main.army.Army;
import main.parameters.GameParameters;
import main.resources.ResourcePool;

import java.util.*;

/**
 * Owns all mercenary armies.
 * Handles upkeep, raiding behaviour, and recruitment.
 */
public class MercenaryManager {

    private final List<MercenaryArmy> armies = new ArrayList<>();
    private final Random              rng    = new Random();

    public MercenaryManager() {}

    // ─── Recruitment ─────────────────────────────────────────────────────────

    /**
     * Hire a mercenary company. Returns null if resources insufficient.
     * Cost = SOLDIER_RECRUIT_GOLD_COST * MERCENARY_COST_MULTIPLIER per soldier.
     */
    public MercenaryArmy hire(String displayName, int size,
                              String zoneId, ResourcePool resources) {
        int cost = (int)(size
                * GameParameters.SOLDIER_RECRUIT_GOLD_COST
                * GameParameters.MERCENARY_COST_MULTIPLIER);
        int manpowerCost = size * GameParameters.SOLDIER_RECRUIT_MANPOWER_COST;
        if (resources.getMoney() < cost) {
            Debug.log("mercenaries", "hire-fail", "Not enough gold. Need " + cost);
            return null;
        }
        if (resources.getManpower() < manpowerCost) {
            Debug.log("mercenaries", "hire-fail", "Not enough manpower. Need " + manpowerCost);
            return null;
        }
        resources.spendMoney(cost);
        resources.spendManpower(manpowerCost);
        MercenaryArmy army = new MercenaryArmy(displayName, size, zoneId);
        armies.add(army);
        Debug.log("mercenaries", "hired", army.getId() + " size=" + size + " cost=" + cost);
        return army;
    }

    // ─── Per-turn upkeep ─────────────────────────────────────────────────────

    /**
     * Pay upkeep for all mercenary armies. If can't pay, that army disbands.
     * Returns log lines.
     */
    public List<String> processUpkeep(ResourcePool resources) {
        List<String> log = new ArrayList<>();
        for (MercenaryArmy army : new ArrayList<>(armies)) {
            if (!army.isAlive()) continue;
            int upkeep = (int)Math.ceil(army.getSize()
                    * GameParameters.SOLDIER_UPKEEP_GOLD
                    * GameParameters.MERCENARY_COST_MULTIPLIER);
            if (resources.getMoney() >= upkeep) {
                resources.spendMoney(upkeep);
                Debug.log("mercenaries", "upkeep", army.getId() + " paid=" + upkeep);
            } else {
                log.add("⚠ Cannot pay mercenary upkeep for " + army.getDisplayName()
                        + " — company disbands.");
                armies.remove(army);
                Debug.log("mercenaries", "disband", army.getId() + " — no upkeep");
            }
        }
        return log;
    }

    // ─── Raiding behaviour ────────────────────────────────────────────────────

    /**
     * Each mercenary army that is NOT well-outnumbered by allies has a 30% chance
     * to raid its current zone.
     *
     * Condition for raiding: (other noble + player armies in zone) NOT > 3/2 * merc size.
     * Money goes to no one (plundering with no benefit to player).
     */
    public List<String> processRaiding(
            main.army.ArmyManager playerArmyManager,
            main.nobles.NobleHouseManager nobleHouseManager,
            main.map.ZoneManager zoneManager,
            ResourcePool resources) {

        List<String> log = new ArrayList<>();

        for (MercenaryArmy merc : new ArrayList<>(armies)) {
            if (!merc.isAlive()) continue;

            String zoneId = merc.getZoneId();
            if (main.army.Army.HEARTLAND_ID.equals(zoneId)) continue;

            // Count allied forces in zone
            int alliedSize = 0;
            for (Army a : playerArmyManager.getDeployedArmies()) {
                if (zoneId.equals(a.getZoneId()) && a.isAlive()) alliedSize += a.getSize();
            }
            main.nobles.NobleHouse owner = nobleHouseManager.getOwnerOfZone(zoneId);
            if (owner != null) {
                alliedSize += owner.getGarrisonFor(zoneId);
                for (main.nobles.NobleArmy na :
                        nobleHouseManager.getArmyManager().getArmiesInZone(zoneId, owner.getId())) {
                    if (na.isAlive()) alliedSize += na.getSize();
                }
            }

            // Raiding condition: allied NOT greater than 3/2 of merc size
            double threshold = GameParameters.MERCENARY_RAID_ALLY_THRESHOLD * merc.getSize();
            boolean unsupervised = alliedSize <= threshold;

            if (!unsupervised) continue;

            if (rng.nextDouble() < GameParameters.MERCENARY_RAID_CHANCE) {
                // Raid zone — money goes to nobody
                main.map.Zone zone = zoneManager.getZone(zoneId);
                int zoneGold = zone != null ? zone.getGoldProduction() : 0;
                int stolen = (int)(zoneGold * GameParameters.RAID_GOLD_ZONE_MULTIPLIER);
                if (owner != null) {
                    int ownerLoss = Math.min(stolen, owner.getGold());
                    owner.addGold(-ownerLoss);
                    log.add("⚠ " + merc.getDisplayName()
                            + " mercenaries plunder " + zoneId
                            + " (" + ownerLoss + " gold lost — unsupervised).");
                } else {
                    log.add("⚠ " + merc.getDisplayName()
                            + " mercenaries plunder the unowned zone " + zoneId + ".");
                }
                // Mark zone raided
                main.map.ZoneState state = zoneManager.getState(zoneId);
                if (state != null) state.markRaided();
                Debug.log("mercenaries", "raid", merc.getId() + " raided " + zoneId);
            }
        }
        return log;
    }

    // ─── Accessors ────────────────────────────────────────────────────────────

    public List<MercenaryArmy> getArmies() {
        return Collections.unmodifiableList(armies);
    }

    public void remove(MercenaryArmy army) {
        armies.remove(army);
    }

    public void removeDeadArmies() {
        armies.removeIf(a -> !a.isAlive());
    }

    public double getTotalUpkeepPerTurn() {
        double total = 0;
        for (MercenaryArmy a : armies) {
            if (a.isAlive()) {
                total += a.getSize()
                        * GameParameters.SOLDIER_UPKEEP_GOLD
                        * GameParameters.MERCENARY_COST_MULTIPLIER;
            }
        }
        return total;
    }

    public void reset() {
        armies.clear();
        MercenaryArmy.resetIdCounter();
    }
}