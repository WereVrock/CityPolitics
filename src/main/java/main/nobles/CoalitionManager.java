package main.nobles;

import main.nobles.ai.NobleAI;
import main.map.ZoneManager;
import main.parameters.GameParameters;

import java.util.*;

/**
* Handles coalition formation, zone targeting, and post-conquest zone award.
* A coalition requires at least 2 member houses (excluding the threat).
* Coalition battles route through NobleArmyManager.resolveAttack() like normal attacks.
*/
public class CoalitionManager {

private static final Random RNG = new Random();

private final ZoneManager         zoneManager;
private final RelationshipManager relationships;
private final ClaimManager        claimManager;
private final NobleArmyManager    armyManager;

// key = "threatId:coordinatorId", value = set of zoneIds routed for that coalition
private final Map<String, Set<String>> routedZones = new HashMap<>();

public CoalitionManager(ZoneManager zoneManager,
RelationshipManager relationships,
ClaimManager claimManager,
NobleArmyManager armyManager) {
this.zoneManager   = zoneManager;
this.relationships = relationships;
this.claimManager  = claimManager;
this.armyManager   = armyManager;
}

// ─── Main entry point ────────────────────────────────────────────────────

/**
* Called once per turn. Checks all houses for coalition trigger.
* Each eligible threat may spawn at most one coalition per turn.
* The coordinator spends their action (their AI tick already ran — coalition
* is issued as an army order, consuming the coordinator's idle army).
*/
public List<String> checkCoalitions(List<NobleHouse> allHouses) {
List<String> log = new ArrayList<>();
// Snapshot to avoid modification during iteration
for (NobleHouse threat : new ArrayList<>(allHouses)) {
if (threat.isEliminated()) continue;
if (threat.getZoneIds().size() < GameParameters.COALITION_ZONE_THRESHOLD) continue;
log.addAll(tryFormCoalition(threat, allHouses));
}
return log;
}

// ─── Coalition formation ─────────────────────────────────────────────────

private List<String> tryFormCoalition(NobleHouse threat, List<NobleHouse> allHouses) {
List<String> log = new ArrayList<>();

List<NobleHouse> members = gatherMembers(threat, allHouses);
if (members.size() < 2) return log; // need at least 2

// Coordinator = highest prestige who can field an army
List<NobleHouse> sorted = new ArrayList<>(members);
sorted.sort(Comparator.comparingInt(NobleHouse::getPrestige).reversed());
NobleHouse coordinator = null;
NobleArmy  coordArmy   = null;
for (NobleHouse m : sorted) {
coordArmy = getOrRecruitIdleArmy(m);
if (coordArmy != null) {
coordinator = m;
break;
}
}
if (coordinator == null || coordArmy == null) return log;

// Determine routed zones for this coalition (threat + coordinator)
String routeKey = threat.getId() + ":" + coordinator.getId();
Set<String> routed = routedZones.getOrDefault(routeKey, Collections.emptySet());

// Pick target zone, skipping routed ones
String targetZone = pickTargetZone(coordinator, members, threat, allHouses, routed);
if (targetZone == null) {
log.add("=== Coalition against " + threat.getName()
+ " disbands — no viable targets remain. ===");
return log;
}

// ---------- Strength check ----------
int totalPower = NobleAI.estimateAttackPower(coordinator, armyManager);
for (NobleHouse m : members) {
if (m == coordinator) continue;
totalPower += NobleAI.estimateMemberPower(m, armyManager);
}
int defenderPower = NobleAI.estimateDefenderCombatPower(coordinator, threat, targetZone, allHouses, armyManager, relationships);
if (totalPower < defenderPower * GameParameters.COALITION_STRENGTH_THRESHOLD) {
// not strong enough yet — silently skip this turn
return log;
}

// Recruit for other members who can afford it
for (NobleHouse m : members) {
if (m == coordinator) continue;
getOrRecruitIdleArmy(m);
}

// Log formation
StringBuilder memberNames = new StringBuilder();
for (int i = 0; i < members.size(); i++) {
if (i > 0) memberNames.append(", ");
memberNames.append(members.get(i).getName());
}
log.add("=== Coalition forms against " + threat.getName()
+ " === Coordinator: " + coordinator.getName()
+ "  Members: " + memberNames);
log.add("Target zone: " + targetZone);

Set<String> memberIds = new HashSet<>();
for (NobleHouse m : members) {
if (m != coordinator) memberIds.add(m.getId());
}
armyManager.moveArmy(coordArmy, targetZone);
coordArmy.issueCoalitionOrder(targetZone, memberIds);
log.add(coordinator.getName() + " leads coalition march on " + targetZone + ".");

return log;
}

// ─── Member gathering ────────────────────────────────────────────────────

private List<NobleHouse> gatherMembers(NobleHouse threat, List<NobleHouse> allHouses) {
List<NobleHouse> members = new ArrayList<>();
for (NobleHouse h : allHouses) {
if (h == threat || h.isEliminated()) continue;
if (isEligible(h, threat)) members.add(h);
}
return members;
}

private boolean isEligible(NobleHouse house, NobleHouse threat) {
// Landless houses are always eligible
if (house.getZoneIds().isEmpty()) return true;
Relationship rel = relationships.get(house.getId(), threat.getId());
return rel == Relationship.RIVAL
|| rel == Relationship.HOSTILE
|| (rel == Relationship.NEUTRAL && house.isThreatened());
}

// ─── Zone selection ──────────────────────────────────────────────────────

/**
* Priority:
* 1. Landless member has a claim on a threat zone → coordinator picks among those
* 2. Coordinator's own claims on threat zones
* 3. Other members' claims on threat zones, preferred by relationship (allied→friendly→neutral→…)
* 4. Any threat zone
*/

private String pickTargetZone(NobleHouse coordinator,
List<NobleHouse> members,
NobleHouse threat,
List<NobleHouse> allHouses,
Set<String> routedZones) {
List<String> threatZones = new ArrayList<>(threat.getZoneIds());
if (threatZones.isEmpty()) return null;

// 1. Landless member claims (excluding routed)
List<String> landlessClaimed = new ArrayList<>();
for (NobleHouse m : members) {
if (!m.getZoneIds().isEmpty()) continue;
for (Claim c : claimManager.getClaimsFor(m.getId())) {
if (threatZones.contains(c.getZoneId()) && !routedZones.contains(c.getZoneId())) {
landlessClaimed.add(c.getZoneId());
}
}
}
if (!landlessClaimed.isEmpty()) {
return landlessClaimed.get(RNG.nextInt(landlessClaimed.size()));
}

// 2. Coordinator's own claims (excluding routed)
for (Claim c : claimManager.getClaimsFor(coordinator.getId())) {
if (threatZones.contains(c.getZoneId()) && !routedZones.contains(c.getZoneId())) {
return c.getZoneId();
}
}

// 3. Other members' claims, ordered by coordinator's relationship to claimant (excluding routed)
List<NobleHouse> claimantsByRelation = new ArrayList<>(members);
claimantsByRelation.remove(coordinator);
claimantsByRelation.sort(Comparator.comparingInt(
m -> relationshipOrder(relationships.get(coordinator.getId(), m.getId()))));
for (NobleHouse m : claimantsByRelation) {
for (Claim c : claimManager.getClaimsFor(m.getId())) {
if (threatZones.contains(c.getZoneId()) && !routedZones.contains(c.getZoneId())) {
return c.getZoneId();
}
}
}

// 4. Fallback: any threat zone not routed
for (String z : threatZones) {
if (!routedZones.contains(z)) return z;
}

return null;
}

private int relationshipOrder(Relationship r) {
return switch (r) {
case ALLIED   -> 0;
case FRIENDLY -> 1;
case NEUTRAL  -> 2;
case HOSTILE  -> 3;
case RIVAL    -> 4;
};
}

// ─── Post-conquest zone award ─────────────────────────────────────────────

/**
* Called by NobleArmyManager after a successful coalition attack.
* Determines who receives the conquered zone.
*
* Priority:
* 1. Landless member with a claim on the zone (guaranteed; random among ties by diplomacy+cunning)
* 2. Participating claimants weighted by coordinator bonus, cunning, diplomacy, prestige, army %
*/

public void awardConqueredZone(String zoneId,
NobleHouse conqueror,
List<NobleHouse> allParticipants,
List<NobleHouse> allHouses,
int previousFortification,
List<String> log) {
// Step 1 — landless claimants
List<NobleHouse> landlessClaimants = new ArrayList<>();
for (NobleHouse h : allParticipants) {
if (h.getZoneIds().isEmpty() && claimManager.hasClaim(h.getId(), zoneId)) {
landlessClaimants.add(h);
}
}
if (!landlessClaimants.isEmpty()) {
NobleHouse winner = pickByDiplomacyCunning(landlessClaimants);
transferZone(zoneId, winner, findCurrentOwner(zoneId, allHouses), previousFortification, log);
return;
}

// Step 2 — weighted random among participating claimants
List<NobleHouse> claimants = new ArrayList<>();
for (NobleHouse h : allParticipants) {
if (claimManager.hasClaim(h.getId(), zoneId)) claimants.add(h);
}
if (claimants.isEmpty()) {
transferZone(zoneId, conqueror, findCurrentOwner(zoneId, allHouses), previousFortification, log);
return;
}

int totalArmy = allParticipants.stream().mapToInt(NobleHouse::getTotalArmySize).sum();
NobleHouse winner = weightedPick(claimants, conqueror, totalArmy);
transferZone(zoneId, winner, findCurrentOwner(zoneId, allHouses), previousFortification, log);
}

private NobleHouse weightedPick(List<NobleHouse> claimants,
NobleHouse coordinator,
int totalArmy) {
double[] weights = new double[claimants.size()];
double total = 0;
for (int i = 0; i < claimants.size(); i++) {
NobleHouse h = claimants.get(i);
NobleCharacter c = h.getActiveCharacter();
int cunning   = c != null ? c.getCunning()   : 0;
int diplomacy = c != null ? c.getDiplomacy() : 0;
double w = 1.0
+ cunning   * GameParameters.COALITION_CUNNING_WEIGHT
+ diplomacy * GameParameters.COALITION_DIPLOMACY_WEIGHT
+ h.getPrestige() * GameParameters.COALITION_PRESTIGE_WEIGHT
+ (totalArmy > 0
? ((double) h.getTotalArmySize() / totalArmy)
* GameParameters.COALITION_ARMY_PARTICIPATION_WEIGHT
: 0);
if (h == coordinator) w += GameParameters.COALITION_COORDINATOR_BONUS;
weights[i] = w;
total += w;
}
double roll = RNG.nextDouble() * total;
double cumulative = 0;
for (int i = 0; i < claimants.size(); i++) {
cumulative += weights[i];
if (roll <= cumulative) return claimants.get(i);
}
return claimants.get(claimants.size() - 1);
}

private NobleHouse pickByDiplomacyCunning(List<NobleHouse> houses) {
return houses.stream().max(Comparator.comparingInt(h -> {
NobleCharacter c = h.getActiveCharacter();
return (c != null ? c.getDiplomacy() + c.getCunning() : 0);
})).orElse(houses.get(0));
}

private void transferZone(String zoneId, NobleHouse winner, NobleHouse loser,
int previousFortification, List<String> log) {
if (loser != null) {
loser.removeZone(zoneId);
// Loser automatically gains a claim on their former zone
claimManager.addClaim(loser.getId(), zoneId);
}
winner.conquerZone(zoneId, previousFortification);
// Other houses' claims on this zone are preserved
log.add(winner.getName() + " receives " + zoneId + " as coalition spoils.");
if (loser != null && loser.isEliminated()) {
log.add(loser.getName() + " has been eliminated.");
}
}

private NobleHouse findCurrentOwner(String zoneId, List<NobleHouse> allHouses) {
for (NobleHouse h : allHouses) {
if (h.getZoneIds().contains(zoneId)) return h;
}
return null;
}

// ─── Routed zone management ─────────────────────────────────────────────

/** Called when a coalition attack fails. 50% chance to mark the zone as routed for that coalition. */
public void onCoalitionAttackFailed(String coordinatorId, String threatId, String zoneId) {
if (RNG.nextDouble() < 0.5) {
String key = threatId + ":" + coordinatorId;
routedZones.computeIfAbsent(key, k -> new HashSet<>()).add(zoneId);
}
}

/** Called whenever a house loses any zone. Clears all routed sets for coalitions against that house. */
public void onHouseLostZone(String houseId) {
routedZones.entrySet().removeIf(entry -> {
String threatId = entry.getKey().split(":")[0];
return threatId.equals(houseId);
});
}

// ─── Army helpers ────────────────────────────────────────────────────────

private NobleArmy getOrRecruitIdleArmy(NobleHouse house) {
// Return existing idle army if available
for (NobleArmy a : armyManager.getArmiesForHouse(house.getId())) {
if (!a.hasPendingOrder() && a.getSize() > 0) return a;
}
// Try to recruit
int manpower = house.getNobleManpower();
int gold     = house.getGold();
int minSize  = GameParameters.NOBLE_ARMY_MIN_RECRUIT_SIZE;
if (manpower < minSize || gold < GameParameters.NOBLE_ARMY_RECRUIT_GOLD_THRESHOLD) return null;
int size = Math.max(minSize, (int)(manpower * GameParameters.NOBLE_ARMY_RECRUIT_FRACTION));
return armyManager.recruit(house, size);
}
}
