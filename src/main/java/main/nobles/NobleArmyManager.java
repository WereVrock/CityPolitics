// NobleArmyManager.java
package main.nobles;

import main.nobles.combat.ArmyForce;
import main.nobles.combat.CombatResolver;
import main.nobles.combat.CombatResult;
import main.map.ZoneManager;
import main.map.ZoneState;
import main.parameters.GameParameters;

import java.util.*;
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
if (zoneId == null) {
debug.Debug.log("noble", "recruit", house.getName() + " cannot recruit (no capital zone)");
return null;
}

int manpowerCost = size;
int goldCost     = size * GameParameters.NOBLE_UPKEEP_COST_PER_SOLDIER;
if (house.getNobleManpower() < manpowerCost) {
debug.Debug.log("noble", "recruit", house.getName() + " insufficient manpower: have " + house.getNobleManpower() + ", need " + manpowerCost);
return null;
}
if (house.getGold() < goldCost) {
    
debug.Debug.log("noble", "recruit", house.getName() + " insufficient gold: have " + house.getGold() + ", need " + goldCost);
return null;
}

house.spendNobleManpower(manpowerCost);
house.addGold(-goldCost);

String    id   = "noble_army_" + (nextId++);
NobleArmy army = new NobleArmy(id, house.getId(), size, zoneId);
army.setSkipNextUpkeep(true);  // skip first-turn upkeep
add(army);
debug.Debug.log("noble", "recruit", house.getName() + " recruited " + size + " soldiers at " + zoneId + " (cost " + goldCost + " gold, " + manpowerCost + " manpower)");
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
if (house.getNobleManpower() < manpowerCost) {
debug.Debug.log("noble", "reinforce", house.getName() + " cannot reinforce: need " + manpowerCost + " manpower, have " + house.getNobleManpower());
return false;
}
if (house.getGold() < goldCost) {
debug.Debug.log("noble", "reinforce", house.getName() + " cannot reinforce: need " + goldCost + " gold, have " + house.getGold());
return false;
}

house.spendNobleManpower(manpowerCost);
house.addGold(-goldCost);
army.setSize(army.getSize() + additionalSize);
debug.Debug.log("noble", "reinforce", house.getName() + " reinforced army " + army.getId() + " by " + additionalSize + " soldiers (new size " + army.getSize() + ")");
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
if (army.getSkipNextUpkeep()) {
army.setSkipNextUpkeep(false);
continue;  // skip first-turn upkeep for newly recruited armies
}
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
* Resolve all ready orders for a specific house only.
* Called once per house in processTurn, in house-list order.
*/
public List<String> resolveOrdersForHouse(String houseId,
List<NobleHouse> allHouses,
ClaimManager claimManager) {
List<String> log = new ArrayList<>();
for (NobleArmy army : new ArrayList<>(armies)) {
if (!army.getHouseId().equals(houseId)) continue;
if (!army.isOrderReadyToResolve()) continue;
switch (army.getPendingOrder()) {
case ATTACK      -> log.addAll(resolveAttack(army, allHouses, claimManager,
army.getCoalitionMemberIds()));
case RAID        -> log.addAll(resolveRaid(army, allHouses));
case JOIN_BATTLE -> {} // pulled into resolveAttack of the attacking house
case NONE        -> {}
}
if (army.getPendingOrder() != NobleArmy.OrderType.JOIN_BATTLE) {
army.clearOrder();
}
}
removeDeadArmies(allHouses);
return log;
}

/**
* Disband every idle army belonging to this house.
* Manpower returns to the house noble pool.
* Called after resolveOrdersForHouse and before the AI tick.
*/
public void disbandIdleArmies(NobleHouse house) {
for (NobleArmy army : new ArrayList<>(getArmiesForHouse(house.getId()))) {
if (army.getPendingOrder() == NobleArmy.OrderType.NONE && army.isAlive()) {
int returned = army.disband(army.getSize());
house.addNobleManpower(returned);
remove(army);
debug.Debug.log("noble", "disband", house.getName() + " disbanded idle army of " + returned + " soldiers (returned to manpower).");
}
}
}

/**
* Split count soldiers out of army into a new separate army in the same zone.
* Returns the new army, or null if count <= 0 or army too small.
*/
public NobleArmy splitArmy(NobleArmy army, int count) {
if (count <= 0 || count >= army.getSize()) return null;
army.setSize(army.getSize() - count);
String id = "noble_army_" + (nextId++);
NobleArmy split = new NobleArmy(id, army.getHouseId(), count, army.getZoneId());
add(split);
debug.Debug.log("noble", "split", "Split " + count + " from " + army.getId() + " into new army " + split.getId());
return split;
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

// ---- Gather attacker armies: main army + JOIN_BATTLE orders targeting this zone ----
List<NobleArmy> attackerArmies = new ArrayList<>();
attackerArmies.add(attArmy);
List<NobleHouse> attackerParticipants = new ArrayList<>();
attackerParticipants.add(attacker);

for (NobleHouse house : allHouses) {
if (house == attacker || house == defender || house.isEliminated()) continue;
Relationship relToAtk = relationships.get(house.getId(), attacker.getId());
if (relToAtk == Relationship.HOSTILE || relToAtk == Relationship.RIVAL) continue;

boolean isCoalitionMember = isCoalition && coalitionMemberIds.contains(house.getId());

for (NobleArmy a : new ArrayList<>(getArmiesForHouse(house.getId()))) {
if (a.getPendingOrder() != NobleArmy.OrderType.JOIN_BATTLE) continue;
if (!zoneId.equals(a.getPendingTargetZoneId())) continue;
if (!a.isOrderReadyToResolve()) continue;

Relationship relToDef = relationships.get(house.getId(), defender.getId());
boolean onAttackingSide = isCoalitionMember
|| relToAtk == Relationship.ALLIED
|| relToDef == Relationship.HOSTILE
|| relToDef == Relationship.RIVAL
|| house.isThreatenedBy(defender.getId());
if (!onAttackingSide) continue;

a.clearOrder();
attackerArmies.add(a);
if (!attackerParticipants.contains(house)) attackerParticipants.add(house);
String reason = isCoalitionMember ? " (coalition member)" : " (join battle)";
log.add(house.getName() + " joins the attack on " + zoneId + reason + ".");
}
}

// ---- Gather defender armies: existing zone armies + JOIN_BATTLE orders targeting this zone ----
List<NobleArmy> defenderArmies = new ArrayList<>();
defenderArmies.addAll(getArmiesInZone(zoneId, defender.getId()));
int garrisonSize = defender.getGarrisonFor(zoneId);
int defFort = defender.getFortificationFor(zoneId);

for (NobleHouse house : allHouses) {
if (house == attacker || house == defender || house.isEliminated()) continue;
for (NobleArmy a : new ArrayList<>(getArmiesForHouse(house.getId()))) {
if (a.getPendingOrder() != NobleArmy.OrderType.JOIN_BATTLE) continue;
if (!zoneId.equals(a.getPendingTargetZoneId())) continue;
if (!a.isOrderReadyToResolve()) continue;

Relationship relToDef = relationships.get(house.getId(), defender.getId());
Relationship relToAtk = relationships.get(house.getId(), attacker.getId());
boolean onDefendingSide = relToDef == Relationship.ALLIED
|| relToAtk == Relationship.HOSTILE
|| relToAtk == Relationship.RIVAL;
if (!onDefendingSide) continue;

a.clearOrder();
defenderArmies.add(a);
log.add(house.getName() + " joins the defense of " + zoneId + " (join battle).");
}
}

// ---- Build ArmyForce lists with raw size and military skill ----
List<ArmyForce> attackerForces = new ArrayList<>();
for (NobleArmy a : attackerArmies) {
NobleHouse h = findHouse(a.getHouseId(), allHouses);
int milSkill = militarySkill(h);
attackerForces.add(new ArmyForce(a.getHouseId(), a.getSize(), 0, milSkill));
}
List<ArmyForce> defenderForces = new ArrayList<>();
int defMil = militarySkill(defender);
defenderForces.add(new ArmyForce(defender.getId(), garrisonSize, defFort, defMil));
for (NobleArmy a : defenderArmies) {
NobleHouse h = findHouse(a.getHouseId(), allHouses);
int mil = militarySkill(h);
defenderForces.add(new ArmyForce(a.getHouseId(), a.getSize(), defFort, mil));
}

// ---- Resolve battle ----
CombatResult result = CombatResolver.resolveMultiSideBattle(
attackerForces, defenderForces,
attacker.getId(), defender.getId(),
defFort);
log.addAll(result.getLog());

// ---- Apply losses ----
for (int i = 0; i < attackerArmies.size(); i++) {
attackerArmies.get(i).setSize(attackerForces.get(i).getRawSize());
}
int newGarrisonRaw = defenderForces.get(0).getRawSize();
int garrisonDelta = garrisonSize - newGarrisonRaw;
if (garrisonDelta > 0) {
defender.damageGarrison(zoneId, garrisonDelta);
}
// defender armies
for (int i = 1; i < defenderArmies.size(); i++) {
defenderArmies.get(i - 1).setSize(defenderForces.get(i).getRawSize());
}

boolean attackersWin = result.getWinnerId().equals(attacker.getId());

if (attackersWin) {
ZoneState state = zoneManager.getState(zoneId);
if (state != null) state.markConquered();
defender.resetGarrison(zoneId);

if (isCoalition && coalitionManager != null) {
coalitionManager.awardConqueredZone(zoneId, attacker,
attackerParticipants, allHouses, defFort, log);
} else {
// Non‑coalition conquest: halve fortification, zero garrison
defender.removeZone(zoneId);
attacker.conquerZone(zoneId, defFort);
// Loser automatically gains a claim on their former zone
claimManager.addClaim(defender.getId(), zoneId);
attacker.resetGarrison(zoneId);
// Halve rebellion power on conquest
ZoneState st = zoneManager.getState(zoneId);
if (st != null) st.setRebellionPower(st.getRebellionPower() / 2);
log.add(attacker.getName() + " captures " + zoneId
+ " from " + defender.getName() + ".");
if (defender.isEliminated())
log.add(defender.getName() + " has been eliminated.");
}
relationships.set(attacker.getId(), defender.getId(), Relationship.RIVAL);
for (NobleHouse p : attackerParticipants) p.clearThreats();

// Any zone loss by a house resets routed lists against that house
if (coalitionManager != null) {
coalitionManager.onHouseLostZone(defender.getId());
}
} else {
log.add(defender.getName() + " repels the attack on " + zoneId + ".");
relationships.set(attacker.getId(), defender.getId(), Relationship.RIVAL);

// Coalition failure — may route this target
if (isCoalition && coalitionManager != null) {
coalitionManager.onCoalitionAttackFailed(attacker.getId(), defender.getId(), zoneId);
}
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
String capital = attacker.getCapitalZoneId();
if (capital != null) moveArmy(attArmy, capital);
return log;
}

// Intercept check
List<NobleArmy> defArmies = getArmiesInZone(zoneId, defender.getId());
if (!defArmies.isEmpty()) {
int defMilitary = militarySkill(defender);
double interceptChance = GameParameters.RAID_INTERCEPT_BASE_CHANCE
+ defMilitary * GameParameters.RAID_INTERCEPT_MILITARY_BONUS;
if (Math.random() < interceptChance) {
log.add(defender.getName() + "'s army intercepts the raid on " + zoneId + "!");
int attMilitary = militarySkill(attacker);
ArmyForce atk = new ArmyForce(attacker.getId(), attArmy.getSize(), 0, attMilitary);
NobleArmy defArmy = defArmies.get(0);
ArmyForce def = new ArmyForce(defender.getId(), defArmy.getSize(), 0, defMilitary);
CombatResult result = CombatResolver.resolve(atk, def);
log.addAll(result.getLog());
attArmy.setSize(atk.getRawSize());
defArmy.setSize(def.getRawSize());
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

Zone zone    = zoneManager.getZone(zoneId);
int zoneGold = zone != null ? zone.getGoldProduction() : GameParameters.ZONE_VILLAGE_GOLD;
int maxByZone = (int)(zoneGold * GameParameters.RAID_GOLD_ZONE_MULTIPLIER);
int maxByArmy = (int)(attArmy.getSize() * GameParameters.RAID_GOLD_PER_SOLDIER);
int maxSteal  = Math.min(maxByZone, maxByArmy);
int stolen    = Math.min(maxSteal,
(int)(defender.getGold() * GameParameters.AI_RAID_GOLD_FRACTION));
stolen = Math.max(0, stolen);

defender.addGold(-stolen);
attacker.addGold(stolen);
if (state != null) state.markRaided();
log.add(attacker.getName() + " raids " + zoneId + " stealing " + stolen + " gold.");
relationships.recordRaid(attacker.getId(), defender.getId());

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

/**
* Returns true if the house already has a pending ATTACK order targeting the given zone.
*/
public boolean hasPendingAttackOrder(String houseId, String zoneId) {
for (NobleArmy a : getArmiesForHouse(houseId)) {
if (a.getPendingOrder() == NobleArmy.OrderType.ATTACK
&& zoneId.equals(a.getPendingTargetZoneId())) {
return true;
}
}
return false;
}

/** Total size of idle (no pending order) armies a house has in a specific zone. */
public int getTotalIdleArmySize(String houseId, String zoneId) {
int total = 0;
for (NobleArmy a : getArmiesInZone(zoneId, houseId)) {
if (!a.hasPendingOrder()) total += a.getSize();
}
return total;
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














