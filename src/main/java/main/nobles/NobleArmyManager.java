// NobleArmyManager.java
package main.nobles;

import main.nobles.combat.ArmyForce;
import main.nobles.combat.CombatResolver;
import main.nobles.combat.CombatResult;
import main.map.ZoneManager;
import main.map.ZoneState;
import main.parameters.GameParameters;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Collectors;
import main.map.Zone;

/**
* Owns all noble armies.
* Handles recruitment, upkeep, disbanding, and order resolution.
*/
public class NobleArmyManager {

private final List<NobleArmy>              armies   = new ArrayList<>();
private final Map<String, List<NobleArmy>> byHouse  = new LinkedHashMap<>();
private final Map<String, List<NobleArmy>> byZone   = new LinkedHashMap<>();
private       int                          nextId   = 1;

private final ZoneManager         zoneManager;
private final RelationshipManager relationships;
private       CoalitionManager    coalitionManager; // set after construction to avoid circular dep

public NobleArmyManager(ZoneManager zoneManager, RelationshipManager relationships) {
    this.zoneManager   = zoneManager;
    this.relationships = relationships;
}

public void setCoalitionManager(CoalitionManager coalitionManager) {
    this.coalitionManager = coalitionManager;
}

// ─── Recruitment ─────────────────────────────────────────────────────────

/**
* Recruit a new army for a house at its capital zone.
* Cost: size * NOBLE_RECRUIT_COST_PER_SOLDIER manpower from house pool
*       + 1 turn of upkeep pre-paid.
* Returns the new army or null if house can't afford it.
*/

public NobleArmy recruit(NobleHouse house, int size) {
if (size <= 0) return null;
String zoneId = house.getCapitalZoneId();
if (zoneId == null) return null; // no capital, can't place army

int manpowerCost = size;
int goldCost     = size * GameParameters.NOBLE_UPKEEP_COST_PER_SOLDIER;
if (house.getNobleManpower() < manpowerCost) return null;
if (house.getGold() < goldCost) return null;

house.spendNobleManpower(manpowerCost);
house.addGold(-goldCost);

String    id   = "noble_army_" + (nextId++);
NobleArmy army = new NobleArmy(id, house.getId(), size, zoneId);
add(army);
return army;
}

/**
* Reinforce an existing army by adding soldiers.
* Deducts manpower and gold from the house.
* Returns true if successful.
*/
public boolean reinforceArmy(NobleHouse house, NobleArmy army, int additionalSize) {
if (additionalSize <= 0) return false;
int manpowerCost = additionalSize;
int goldCost     = additionalSize * GameParameters.NOBLE_UPKEEP_COST_PER_SOLDIER;
if (house.getNobleManpower() < manpowerCost) return false;
if (house.getGold() < goldCost) return false;

house.spendNobleManpower(manpowerCost);
house.addGold(-goldCost);
army.setSize(army.getSize() + additionalSize);
return true;
}

// ─── Upkeep ──────────────────────────────────────────────────────────────

/**
* Pay upkeep for all armies of a house. If gold insufficient,
* disband soldiers until affordable, returning them to noble manpower.
*/

/**
     * Pay upkeep for all armies of a house. If gold insufficient,
     * disband soldiers until affordable, returning them to noble manpower.
     */
    public void payUpkeep(NobleHouse house) {
        List<NobleArmy> houseArmies = getArmiesForHouse(house.getId());
        for (NobleArmy army : new ArrayList<>(houseArmies)) {
            boolean isDefending = isArmyDefending(army, house);
            int upkeepPerSoldier = GameParameters.NOBLE_UPKEEP_COST_PER_SOLDIER;
            if (isDefending) {
                upkeepPerSoldier = (int)(upkeepPerSoldier * (1.0 - GameParameters.NOBLE_UPKEEP_DEFENSE_DISCOUNT));
                if (upkeepPerSoldier < 1) upkeepPerSoldier = 1; // minimum 1
            }
            int cost = army.getSize() * upkeepPerSoldier;
            if (house.getGold() >= cost) {
                house.addGold(-cost);
            } else {
                // Disband all — return to manpower
                int disbanded = army.disband(army.getSize());
                house.addNobleManpower(disbanded);
                remove(army);
            }
        }
    }

    private boolean isArmyDefending(NobleArmy army, NobleHouse house) {
        // Defending if: in a zone owned by its house, and has no pending attack/raid order
        if (!house.getZoneIds().contains(army.getZoneId())) return false;
        return army.getPendingOrder() == NobleArmy.OrderType.NONE;
    }

// ─── Voluntary disband ────────────────────────────────────────────────────

/**
* AI chooses to disband some soldiers to save on upkeep.
* Disbanded soldiers return to noble manpower.
*/
public void disbandPartial(NobleHouse house, NobleArmy army, int count) {
int actual = army.disband(count);
house.addNobleManpower(actual);
if (!army.isAlive()) remove(army);
}

// ─── Order resolution ────────────────────────────────────────────────────

/**
* Tick all orders (mark them ready). Called at turn start before resolution.
*/

public void tickOrders() {
for (NobleArmy army : new ArrayList<>(armies)) {
army.tickOrder();
}
}

/**
* Resolve all ready orders. Returns log lines.
* Must be called after tickOrders().
*/

public List<String> resolveOrders(List<NobleHouse> allHouses,
                                   ClaimManager claimManager) {
    List<String> log = new ArrayList<>();
    for (NobleArmy army : new ArrayList<>(armies)) {
        if (!army.isOrderReadyToResolve()) continue;
        switch (army.getPendingOrder()) {
            case ATTACK -> log.addAll(resolveAttack(army, allHouses, claimManager,
                    army.getCoalitionMemberIds()));
            case RAID   -> log.addAll(resolveRaid(army, allHouses));
            case NONE   -> {}
        }
        army.clearOrder();
    }
    removeDeadArmies(allHouses);
    return log;
}

// ─── Attack resolution ───────────────────────────────────────────────────

private List<String> resolveAttack(NobleArmy attArmy, List<NobleHouse> allHouses,
                                    ClaimManager claimManager,
                                    Set<String> coalitionMemberIds) {
    List<String> log = new ArrayList<>();
    String zoneId = attArmy.getPendingTargetZoneId();
    if (zoneId == null) return log;

    NobleHouse attacker = findHouse(attArmy.getHouseId(), allHouses);
    NobleHouse defender = findZoneOwner(zoneId, allHouses);
    if (attacker == null || defender == null || attacker == defender) return log;

    boolean isCoalition = coalitionMemberIds != null && !coalitionMemberIds.isEmpty();
    if (isCoalition) {
        log.add("=== Coalition attack on " + zoneId + " held by " + defender.getName()
                + " === Coordinator: " + attacker.getName() + " ===");
    } else {
        log.add(attacker.getName() + " army attacks " + zoneId
                + " held by " + defender.getName() + ".");
    }

        // ---- Gather supporter armies for attacker ----
        List<NobleArmy> attackerArmies = new ArrayList<>();
        attackerArmies.add(attArmy);
        // Track all participants for coalition zone award
        List<NobleHouse> attackerParticipants = new ArrayList<>();
        attackerParticipants.add(attacker);

        for (NobleHouse house : allHouses) {
            if (house == attacker || house == defender || house.isEliminated()) continue;
            Relationship relToDef = relationships.get(house.getId(), defender.getId());
            Relationship relToAtk = relationships.get(house.getId(), attacker.getId());

            boolean qualifies = false;
            // Coalition members always join if they have an army
            if (isCoalition && coalitionMemberIds.contains(house.getId())) {
                qualifies = true;
            } else {
                // Do NOT join if hostile/rival towards the attacker
                if (relToAtk == Relationship.HOSTILE || relToAtk == Relationship.RIVAL) continue;
                if (relToAtk == Relationship.ALLIED) {
                    qualifies = true;
                } else if (relToDef == Relationship.HOSTILE || relToDef == Relationship.RIVAL
                        || (house.isThreatened() && relToDef == Relationship.NEUTRAL)) {
                    qualifies = true;
                }
            }

            if (!qualifies) continue;

            List<NobleArmy> idle = getArmiesForHouse(house.getId()).stream()
                    .filter(a -> !a.hasPendingOrder() && a.isAlive())
                    .collect(Collectors.toList());
            for (NobleArmy a : idle) {
                a.setPreviousZoneId(a.getZoneId());
                moveArmy(a, zoneId);
                attackerArmies.add(a);
                attackerParticipants.add(house);
                log.add(house.getName() + " joins the attack on " + zoneId + ".");
            }
        }

        // ---- Gather supporter armies for defender ----
        List<NobleArmy> defenderArmies = new ArrayList<>();
        List<NobleArmy> existingDef = getArmiesInZone(zoneId, defender.getId());
        defenderArmies.addAll(existingDef);
        int garrisonSize = defender.getGarrisonFor(zoneId);

        for (NobleHouse house : allHouses) {
            if (house == attacker || house == defender || house.isEliminated()) continue;
            Relationship relToDef = relationships.get(house.getId(), defender.getId());
            if (relToDef == Relationship.ALLIED) {
                List<NobleArmy> idle = getArmiesForHouse(house.getId()).stream()
                        .filter(a -> !a.hasPendingOrder() && a.isAlive())
                        .collect(Collectors.toList());
                for (NobleArmy a : idle) {
                    a.setPreviousZoneId(a.getZoneId());
                    moveArmy(a, zoneId);
                    defenderArmies.add(a);
                    log.add(house.getName() + " joins the defense of " + zoneId + ".");
                }
            }
        }

        // ---- Build ArmyForce lists ----
        List<ArmyForce> attackerForces = new ArrayList<>();
        for (NobleArmy a : attackerArmies) {
            int milSkill = militarySkill(findHouse(a.getHouseId(), allHouses));
            int power = (int)(a.getSize() * (1.0 + milSkill * GameParameters.MILITARY_SKILL_BONUS_PER_POINT));
            attackerForces.add(new ArmyForce(a.getHouseId(), power, 0));
        }
        List<ArmyForce> defenderForces = new ArrayList<>();
        int defMil = militarySkill(defender);
        int garrisonPower = (int)(garrisonSize * (1.0 + defMil * GameParameters.MILITARY_SKILL_BONUS_PER_POINT));
        defenderForces.add(new ArmyForce(defender.getId(), garrisonPower, defender.getFortificationFor(zoneId)));
        for (NobleArmy a : defenderArmies) {
            int mil = militarySkill(findHouse(a.getHouseId(), allHouses));
            int power = (int)(a.getSize() * (1.0 + mil * GameParameters.MILITARY_SKILL_BONUS_PER_POINT));
            defenderForces.add(new ArmyForce(a.getHouseId(), power, defender.getFortificationFor(zoneId)));
        }

        // ---- Resolve battle ----
        CombatResult result = CombatResolver.resolveMultiSideBattle(
                attackerForces, defenderForces,
                attacker.getId(), defender.getId(),
                defender.getFortificationFor(zoneId));
        log.addAll(result.getLog());

        // ---- Apply losses ----
        for (int i = 0; i < attackerArmies.size(); i++) {
            attackerArmies.get(i).setSize(attackerForces.get(i).getArmySize());
        }
        int newGarrison = defenderForces.get(0).getArmySize();
        defender.damageGarrison(zoneId, garrisonSize - newGarrison);
        for (int i = 1; i < defenderArmies.size(); i++) {
            defenderArmies.get(i-1).setSize(defenderForces.get(i).getArmySize());
        }

        boolean attackersWin = result.getWinnerId().equals(attacker.getId());

        if (attackersWin) {
            ZoneState state = zoneManager.getState(zoneId);
            if (state != null) state.markConquered();
            defender.resetGarrison(zoneId);

            if (isCoalition && coalitionManager != null) {
                // Coalition award logic determines who gets the zone
                coalitionManager.awardConqueredZone(zoneId, attacker,
                        attackerParticipants, allHouses, log);
            } else {
                defender.removeZone(zoneId);
                attacker.addZone(zoneId);
                claimManager.removeAllClaimsOnZone(zoneId);
                attacker.resetGarrison(zoneId);
                log.add(attacker.getName() + " captures " + zoneId
                        + " from " + defender.getName() + ".");
                if (defender.isEliminated())
                    log.add(defender.getName() + " has been eliminated.");
            }
            relationships.set(attacker.getId(), defender.getId(), Relationship.RIVAL);
            // Clear threatened for all attackers
            for (NobleHouse p : attackerParticipants) p.setThreatened(false);
        } else {
            log.add(defender.getName() + " repels the attack on " + zoneId + ".");
            relationships.set(attacker.getId(), defender.getId(), Relationship.RIVAL);
        }

        // ---- Return supporter armies to previous zones (or capital) ----
        for (NobleArmy a : attackerArmies) {
            if (a == attArmy) continue;
            String prev = a.getPreviousZoneId();
            NobleHouse owner = findHouse(a.getHouseId(), allHouses);
            if (prev != null && findZoneOwner(prev, allHouses) == owner) {
                moveArmy(a, prev);
            } else if (owner != null && owner.getCapitalZoneId() != null) {
                moveArmy(a, owner.getCapitalZoneId());
            }
            a.clearOrder();
        }
        for (NobleArmy a : defenderArmies) {
            String prev = a.getPreviousZoneId();
            NobleHouse owner = findHouse(a.getHouseId(), allHouses);
            if (prev != null && findZoneOwner(prev, allHouses) == owner) {
                moveArmy(a, prev);
            } else if (owner != null && owner.getCapitalZoneId() != null) {
                moveArmy(a, owner.getCapitalZoneId());
            }
            a.clearOrder();
        }

        if (!attackersWin) {
            String capital = attacker.getCapitalZoneId();
            if (capital != null) moveArmy(attArmy, capital);
        }
        attArmy.clearOrder();

        removeDeadArmies(allHouses);
        return log;
    }

// ─── Raid resolution ─────────────────────────────────────────────────────

private List<String> resolveRaid(NobleArmy attArmy, List<NobleHouse> allHouses) {
List<String> log = new ArrayList<>();
String zoneId = attArmy.getPendingTargetZoneId();
if (zoneId == null) return log;

NobleHouse attacker = findHouse(attArmy.getHouseId(), allHouses);
NobleHouse defender = findZoneOwner(zoneId, allHouses);
if (attacker == null || defender == null || attacker == defender) return log;

ZoneState state = zoneManager.getState(zoneId);
if (state != null && state.isRecentlyRaided()) {
log.add(attacker.getName() + " finds " + zoneId
+ " already raided. Raid cancelled.");
// Return army to capital
String capital = attacker.getCapitalZoneId();
if (capital != null) moveArmy(attArmy, capital);
return log;
}

// Intercept check — defender army already in that zone
List<NobleArmy> defArmies = getArmiesInZone(zoneId, defender.getId());
if (!defArmies.isEmpty()) {
int defMilitary = militarySkill(defender);
double interceptChance = GameParameters.RAID_INTERCEPT_BASE_CHANCE
+ defMilitary * GameParameters.RAID_INTERCEPT_MILITARY_BONUS;
if (Math.random() < interceptChance) {
log.add(defender.getName() + "'s army intercepts the raid on "
+ zoneId + "!");
int attMilitary = militarySkill(attacker);
ArmyForce atk = new ArmyForce(attacker.getId(),
(int)(attArmy.getSize() * militaryMult(attMilitary)), 0);
NobleArmy defArmy = defArmies.get(0);
ArmyForce def = new ArmyForce(defender.getId(),
(int)(defArmy.getSize() * militaryMult(defMilitary)), 0);
CombatResult result = CombatResolver.resolve(atk, def);
log.addAll(result.getLog());
attArmy.setSize(atk.getArmySize());
defArmy.setSize(def.getArmySize());
if (!attacker.getId().equals(result.getWinnerId())) {
log.add("Raid on " + zoneId + " repelled.");
String capital = attacker.getCapitalZoneId();
if (capital != null) moveArmy(attArmy, capital);
if (!attArmy.isAlive()) remove(attArmy);
return log;
}
log.add(attacker.getName() + " fights through and raids " + zoneId + ".");
} else {
log.add(defender.getName() + "'s army fails to intercept the raid.");
}
}

// Gold stolen — capped by zone production and army size
Zone zone    = zoneManager.getZone(zoneId);
int zoneGold = zone != null ? zone.getGoldProduction()
: GameParameters.ZONE_VILLAGE_GOLD;
int maxByZone = (int)(zoneGold * GameParameters.RAID_GOLD_ZONE_MULTIPLIER);
int maxByArmy = (int)(attArmy.getSize() * GameParameters.RAID_GOLD_PER_SOLDIER);
int maxSteal  = Math.min(maxByZone, maxByArmy);
int stolen    = Math.min(maxSteal,
(int)(defender.getGold() * GameParameters.AI_RAID_GOLD_FRACTION));
stolen = Math.max(0, stolen);

defender.addGold(-stolen);
attacker.addGold(stolen);
if (state != null) state.markRaided();
log.add(attacker.getName() + " raids " + zoneId
+ " stealing " + stolen + " gold.");
relationships.recordRaid(attacker.getId(), defender.getId());

// Return raiding army to capital
String capital = attacker.getCapitalZoneId();
if (capital != null) moveArmy(attArmy, capital);

if (!attArmy.isAlive()) remove(attArmy);
return log;
}

// ─── Collection access ───────────────────────────────────────────────────

public List<NobleArmy> getAllArmies() {
return Collections.unmodifiableList(armies);
}

public List<NobleArmy> getArmiesForHouse(String houseId) {
return Collections.unmodifiableList(
byHouse.getOrDefault(houseId, Collections.emptyList()));
}

public List<NobleArmy> getArmiesInZone(String zoneId) {
return Collections.unmodifiableList(
byZone.getOrDefault(zoneId, Collections.emptyList()));
}

public List<NobleArmy> getArmiesInZone(String zoneId, String houseId) {
List<NobleArmy> result = new ArrayList<>();
for (NobleArmy a : byZone.getOrDefault(zoneId, Collections.emptyList())) {
if (a.getHouseId().equals(houseId)) result.add(a);
}
return result;
}

public void reset() {
armies.clear();
byHouse.clear();
byZone.clear();
nextId = 1;
}

// ─── Internal ────────────────────────────────────────────────────────────

private void add(NobleArmy army) {
// Only merge into existing same-house army in same zone if NEITHER has a pending order
List<NobleArmy> zoneList = new ArrayList<>(
byZone.getOrDefault(army.getZoneId(), Collections.emptyList()));
for (NobleArmy existing : zoneList) {
if (existing.getHouseId().equals(army.getHouseId())
&& !existing.hasPendingOrder()
&& !army.hasPendingOrder()) {
existing.setSize(existing.getSize() + army.getSize());
return;
}
}
armies.add(army);
byHouse.computeIfAbsent(army.getHouseId(), k -> new ArrayList<>()).add(army);
byZone.computeIfAbsent(army.getZoneId(),   k -> new ArrayList<>()).add(army);
}

public void remove(NobleArmy army) {
armies.remove(army);
List<NobleArmy> h = byHouse.get(army.getHouseId());
if (h != null) h.remove(army);
List<NobleArmy> z = byZone.get(army.getZoneId());
if (z != null) z.remove(army);
}

/** Move army between zones — updates byZone index. */

public void moveArmy(NobleArmy army, String newZoneId) {
if (newZoneId == null) return;
if (newZoneId.equals(army.getZoneId())) return;

// Remove from old zone index
List<NobleArmy> oldList = byZone.get(army.getZoneId());
if (oldList != null) oldList.remove(army);

army.setZoneId(newZoneId);

// Only merge if NEITHER army has a pending order
List<NobleArmy> destList = new ArrayList<>(
byZone.getOrDefault(newZoneId, Collections.emptyList()));
for (NobleArmy existing : destList) {
if (existing != army
&& existing.getHouseId().equals(army.getHouseId())
&& !existing.hasPendingOrder()
&& !army.hasPendingOrder()) {
existing.setSize(existing.getSize() + army.getSize());
armies.remove(army);
List<NobleArmy> h = byHouse.get(army.getHouseId());
if (h != null) h.remove(army);
return;
}
}

byZone.computeIfAbsent(newZoneId, k -> new ArrayList<>()).add(army);
}

private void removeDeadArmies(List<NobleHouse> allHouses) {
for (NobleArmy army : new ArrayList<>(armies)) {
if (!army.isAlive()) remove(army);
}
}

private NobleHouse findHouse(String id, List<NobleHouse> all) {
for (NobleHouse h : all) if (h.getId().equals(id)) return h;
return null;
}

private NobleHouse findZoneOwner(String zoneId, List<NobleHouse> all) {
for (NobleHouse h : all) if (h.getZoneIds().contains(zoneId)) return h;
return null;
}

private int militarySkill(NobleHouse house) {
NobleCharacter c = house.getActiveCharacter();
return c != null ? c.getMilitary() : 0;
}

private double militaryMult(int skill) {
return 1.0 + skill * GameParameters.MILITARY_SKILL_BONUS_PER_POINT;
}
}
