package City.main.nobles;

import City.debug.Debug;
import City.main.barbarians.BarbArmy;
import City.main.barbarians.BarbArmyManager;
import City.main.barbarians.BarbCombatHandler;
import City.main.barbarians.RavagedZoneManager;
import City.main.map.Zone;
import City.main.map.ZoneManager;
 
import City.main.parameters.NobleHouseParams;
import City.main.parameters.PrestigeXPParams;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Handles noble AI interactions with barbarian armies each turn.
 *
 * Behaviours:
 * 1. Prestige-motivated nobles hunt weak barbarian armies in adjacent zones.
 * 2. If the warboss is headed to an owned zone next turn, raise a large
 *    defensive army and wait in that zone.
 * 3. Last-zone desperate defence: if a noble has only one zone left and
 *    cannot beat incoming barbarians, recruit as many soldiers as possible.
 */
public final class NobleBarbHunter {

    private NobleBarbHunter() {}

    public static List<String> processTurn(
            List<NobleHouse>   allHouses,
            NobleArmyManager   armyManager,
            Function<NobleHouseManager, NobleHouseManager> selfFn,
            NobleHouseManager  houseManager,
            ZoneManager        zoneManager) {

        List<String> log = new ArrayList<>();

        BarbArmyManager barbArmyManager =
                houseManager.getBarbArmyManagerRef();
        if (barbArmyManager == null) return log;

        for (NobleHouse house : allHouses) {
            if (house.isEliminated()) continue;

            log.addAll(tryWarbossDefense(house, barbArmyManager,
                    armyManager, zoneManager, allHouses));

            log.addAll(tryBarbHunt(house, barbArmyManager,
                    armyManager, zoneManager));
        }

        return log;
    }

    // ─── Warboss defense ─────────────────────────────────────────────────────

    private static List<String> tryWarbossDefense(
            NobleHouse       house,
            BarbArmyManager  barbArmyManager,
            NobleArmyManager armyManager,
            ZoneManager      zoneManager,
            List<NobleHouse> allHouses) {

        List<String> log = new ArrayList<>();
        BarbArmy warboss = barbArmyManager.getWarboss();
        if (warboss == null || warboss.getNextZoneId() == null) return log;

        String nextZone = warboss.getNextZoneId();
        if (!house.getZoneIds().contains(nextZone)) return log;

        // Warboss is coming to our zone — prepare
        boolean isLastZone = house.getZoneIds().size() == 1;
        int warbossSize    = warboss.getSize();

        // How many soldiers do we already have in that zone?
        int existingInZone = armyManager.getArmiesInZone(nextZone, house.getId())
                .stream().mapToInt(NobleArmy::getSize).sum()
                + house.getGarrisonFor(nextZone);

        if (existingInZone >= warbossSize) {
            // Already strong enough
            Debug.log("noble-barb", "warboss-defense",
                    house.getName() + " already has enough defenders at " + nextZone);
            return log;
        }

        int needed = warbossSize - existingInZone + 1;

        double fraction = isLastZone
                ? 1.0
                : PrestigeXPParams.NOBLE_WARBOSS_DEFENSE_ARMY_FRACTION;

        int maxByManpower = house.getNobleManpower();
        int maxByGold     = house.getGold()
                / Math.max(1, NobleHouseParams.NOBLE_UPKEEP_COST_PER_SOLDIER);
        int maxAffordable = (int)(Math.min(maxByManpower, maxByGold) * fraction);

        if (maxAffordable < NobleHouseParams.NOBLE_ARMY_MIN_RECRUIT_SIZE) {
            if (isLastZone && maxAffordable > 0) {
                // Desperate — recruit whatever we can
                maxAffordable = Math.min(maxByManpower, maxByGold);
            } else {
                Debug.log("noble-barb", "warboss-defense",
                        house.getName() + " cannot afford warboss defense");
                return log;
            }
        }

        int recruitSize = Math.min(maxAffordable, needed);
        if (recruitSize < 1) return log;

        NobleArmy army = armyManager.recruit(house, recruitSize);
        if (army == null) return log;

        // Move to the threatened zone if not already there
        if (!army.getZoneId().equals(nextZone)) {
            armyManager.moveArmy(army, nextZone);
        }
        // No pending order — army waits (idle = defend in place)

        String urgency = isLastZone ? " [LAST STAND]" : "";
        log.add(house.getName() + " raises " + recruitSize
                + " soldiers to defend " + nextZone
                + " against the incoming Warboss." + urgency);
        Debug.log("noble-barb", "warboss-defense",
                house.getName() + " raised=" + recruitSize
                + " for warboss at " + nextZone + urgency);
        return log;
    }

    // ─── Barbarian hunting ────────────────────────────────────────────────────

    private static List<String> tryBarbHunt(
            NobleHouse       house,
            BarbArmyManager  barbArmyManager,
            NobleArmyManager armyManager,
            ZoneManager      zoneManager) {

        List<String> log = new ArrayList<>();

        // Only PRESTIGE-motivated houses actively hunt
        NobleCharacter ch = house.getActiveCharacter();
        if (ch == null) return log;
        if (ch.getDominantMotivation() != Motivation.PRESTIGE
                && ch.getSecondaryMotivation() != Motivation.PRESTIGE) return log;

        // Gather idle armies
        List<NobleArmy> idleArmies = new ArrayList<>();
        for (NobleArmy a : armyManager.getArmiesForHouse(house.getId())) {
            if (!a.hasPendingOrder() && a.isAlive()) idleArmies.add(a);
        }
        if (idleArmies.isEmpty()) return log;

        int totalForce = idleArmies.stream().mapToInt(NobleArmy::getSize).sum();

        // Find adjacent barbarian armies that are feasibly beatable
        BarbArmy target = findHuntTarget(house, barbArmyManager,
                zoneManager, totalForce);
        if (target == null) return log;

        // Merge all idle armies into one
        NobleArmy pool = idleArmies.get(0);
        for (int i = 1; i < idleArmies.size(); i++) {
            NobleArmy other = idleArmies.get(i);
            pool.setSize(pool.getSize() + other.getSize());
            armyManager.remove(other);
        }

        // Move to target zone and resolve combat immediately
        String targetZone = target.getZoneId();
        armyManager.moveArmy(pool, targetZone);

        log.add(house.getName() + " hunts barbarians at " + targetZone
                + " (" + pool.getSize() + " vs " + target.getSize() + ").");

        log.addAll(resolveBarbHunt(house, pool, target,
                barbArmyManager, armyManager, log));

        return log;
    }

    private static BarbArmy findHuntTarget(
            NobleHouse      house,
            BarbArmyManager barbArmyManager,
            ZoneManager     zoneManager,
            int             nobleForce) {

        BarbArmy best     = null;
        int      bestSize = Integer.MAX_VALUE;

        for (String ownedZone : house.getZoneIds()) {
            Zone zone = zoneManager.getZone(ownedZone);
            if (zone == null) continue;

            for (String adjId : zone.getAdjacentIds()) {
                for (BarbArmy barb : barbArmyManager.getMobileArmies()) {
                    if (!barb.getZoneId().equals(adjId)) continue;
                    if (barb.isWarboss()) continue; // warboss handled separately
                    if (barb.isPaidOff()) continue;

                    double ratio = (double) nobleForce / Math.max(1, barb.getSize());
                    if (ratio >= PrestigeXPParams.NOBLE_BARB_HUNT_STRENGTH_RATIO) {
                        if (barb.getSize() < bestSize) {
                            bestSize = barb.getSize();
                            best     = barb;
                        }
                    }
                }
            }
        }
        return best;
    }

    private static List<String> resolveBarbHunt(
            NobleHouse       house,
            NobleArmy        army,
            BarbArmy         target,
            BarbArmyManager  barbArmyManager,
            NobleArmyManager armyManager,
            List<String>     existingLog) {

        List<String> log = new ArrayList<>();
        int barbSizeBefore = target.getSize();

        BarbCombatHandler.BarbCombatResult result =
                BarbCombatHandler.nobleAttacksBarbGarrison(
                        // Reuse the garrison handler — same logic for field fight
                        // We need a direct fight, use the attacker-wins no-bonus path
                        house, target, target.getZoneId(),
                        armyManager, log);

        int killed = barbSizeBefore - target.getSize();

        if (result.attackerWon) {
            // Award prestige
            int prestige = Math.max(
                    PrestigeXPParams.NOBLE_BARB_PRESTIGE_MIN_WIN,
                    (int)(killed * PrestigeXPParams.NOBLE_BARB_PRESTIGE_PER_KILL));
            house.addPrestige(prestige);
            log.add(house.getName() + " defeats barbarians at " + target.getZoneId()
                    + "! Prestige +" + prestige + ".");
            Debug.log("noble-barb", "hunt-win",
                    house.getName() + " killed=" + killed + " prestige=+" + prestige);

            if (!target.isAlive()) {
                barbArmyManager.remove(target);
            }
        } else {
            log.add(house.getName() + " is repelled by barbarians at "
                    + target.getZoneId() + ".");
            Debug.log("noble-barb", "hunt-loss",
                    house.getName() + " repelled at " + target.getZoneId());
            // Recall surviving army to capital
            String capital = house.getCapitalZoneId();
            if (capital != null && army.isAlive()) {
                armyManager.moveArmy(army, capital);
            }
        }

        return log;
    }
}