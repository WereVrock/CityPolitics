package main.nobles.ai;

import main.nobles.*;
import debug.Debug;
import main.parameters.GameParameters;
import main.nobles.combat.ArmyForce;
import main.nobles.combat.CombatResolver;
import main.nobles.combat.CombatResult;
import main.map.ZoneState;
import main.parameters.GameParameters;
import main.rules.NobleRules;

import java.util.*;

/**
* Per-house AI brain.
* Handles motivation, action selection, and threatened state.
* Coalition logic lives in CoalitionManager.
*/
public class NobleAI {

private static final Random RNG = new Random();

// ─── Entry point ─────────────────────────────────────────────────────────

public static List<String> tick(NobleHouse actor,
List<NobleHouse> allHouses,
RelationshipManager relationships,
ClaimManager claimManager,
main.map.ZoneManager zoneManager,
NobleArmyManager armyManager) {
List<String> log = new ArrayList<>();
if (actor.isEliminated()) return log;

relationships.tickDecay(allHouseIds(allHouses));
considerBreakingAlliances(actor, allHouses, relationships, log);

List<NobleArmy> existingArmies = new ArrayList<>(armyManager.getArmiesForHouse(actor.getId()));
NobleArmy idleArmy = existingArmies.stream().filter(a -> !a.hasPendingOrder()).findFirst().orElse(null);

int manpower = actor.getNobleManpower();
int gold = actor.getGold();
int minSize = GameParameters.NOBLE_ARMY_MIN_RECRUIT_SIZE;
int maxSustainableSize = (int)(gold / (GameParameters.NOBLE_UPKEEP_COST_PER_SOLDIER * 2.0));

// Armies are now recruited on-demand in execute() and CoalitionManager.
// Proactive recruitment removed to prevent idle army gold drain.

// War-chest target for this house
int warChestTarget = getWarChestTarget(actor, allHouses, relationships, armyManager);
Debug.log("noble", "warchest", actor.getName() + " target=" + warChestTarget);

if (actor.getGold() < GameParameters.NOBLE_ARMY_DISBAND_GOLD_THRESHOLD) {
for (NobleArmy army : new ArrayList<>(armyManager.getArmiesForHouse(actor.getId()))) {
if (!army.hasPendingOrder()) {
armyManager.disbandPartial(actor, army, army.getSize());
log.add(actor.getName() + " disbands army to conserve gold.");
}
}
}

Motivation motivation = pickMotivation(actor.getActiveCharacter());
NobleAction action    = pickAction(actor, motivation, allHouses, relationships, claimManager);
Debug.log("noble", "tick", actor.getName() + " motivation=" + motivation + " action=" + action);
if (action == null) return log;

log.addAll(execute(actor, action, motivation, allHouses,
relationships, claimManager, zoneManager, armyManager));
return log;
}

// ─── Threatened ──────────────────────────────────────────────────────────

public static void updateThreatenedStatus(NobleHouse attacker,
List<NobleHouse> allHouses,
RelationshipManager relationships) {
updateThreatenedStatus(attacker, allHouses, relationships, 1.0);
}

/** Overload with a multiplier (e.g. 2.0 for claimless attacks). */
public static void updateThreatenedStatus(NobleHouse attacker,
List<NobleHouse> allHouses,
RelationshipManager relationships,
double multiplier) {
int attackerZones = attacker.getZoneIds().size();
int totalZones    = 0;
for (NobleHouse h : allHouses) totalZones += h.getZoneIds().size();

for (NobleHouse observer : allHouses) {
if (observer == attacker || observer.isEliminated()) continue;
Relationship rel = relationships.get(attacker.getId(), observer.getId());
if (rel != Relationship.NEUTRAL) continue;

int observerZones = observer.getZoneIds().size();
double chance = (double)(attackerZones - observerZones)
/ Math.max(1, totalZones)
* GameParameters.THREATENED_BASE_CHANCE_MULTIPLIER
* multiplier;
chance = Math.max(0, Math.min(1.0, chance));

if (RNG.nextDouble() < chance) {
observer.setThreatened(true);
}
}
}

public static void tickThreatenedDecay(List<NobleHouse> allHouses) {
for (NobleHouse house : allHouses) {
if (house.isThreatened()
&& RNG.nextDouble() < GameParameters.THREATENED_DECAY_CHANCE) {
house.setThreatened(false);
}
}
}

// ─── Alliance breaking ────────────────────────────────────────────────────

private static void considerBreakingAlliances(NobleHouse actor,
List<NobleHouse> allHouses,
RelationshipManager relationships,
List<String> log) {
List<String> allIds = allHouseIds(allHouses);
List<String> allies = new ArrayList<>(
relationships.getAll(actor.getId(), Relationship.ALLIED, allIds));

for (String allyId : allies) {
NobleHouse ally = findById(allyId, allHouses);
if (ally == null || ally.isEliminated()) {
relationships.set(actor.getId(), allyId, Relationship.NEUTRAL);
continue;
}
boolean tooWeak = ally.getTotalArmySize()
< actor.getTotalArmySize() * GameParameters.ALLIANCE_MIN_ARMY_FRACTION;
if (tooWeak) {
NobleCharacter c   = actor.getActiveCharacter();
int            dip = c != null ? c.getDiplomacy() : 0;
double cleanChance = GameParameters.ALLIANCE_BREAK_CLEAN_BASE
+ dip * GameParameters.ALLIANCE_BREAK_CLEAN_PER_DIPLOMACY;
Relationship result = RNG.nextDouble() < cleanChance
? Relationship.NEUTRAL : Relationship.HOSTILE;
relationships.set(actor.getId(), allyId, result);
log.add(actor.getName() + " breaks alliance with "
+ ally.getName() + ". New relation: " + result.name());
}
}
}

// ─── Motivation ──────────────────────────────────────────────────────────

private static Motivation pickMotivation(NobleCharacter character) {
if (character == null) return Motivation.SECURITY;
return RNG.nextDouble() < GameParameters.AI_DOMINANT_MOTIVATION_CHANCE
? character.getDominantMotivation()
: character.getSecondaryMotivation();
}

// ─── Action selection ────────────────────────────────────────────────────

private static NobleAction pickAction(NobleHouse actor, Motivation motivation,
List<NobleHouse> allHouses,
RelationshipManager relationships,
ClaimManager claimManager) {
List<String> allIds = allHouseIds(allHouses);

if (shouldGift(actor, motivation, allHouses, relationships)) {
Debug.log("noble", "action-pick", actor.getName() + " -> GIFT (shouldGift)");
return NobleAction.GIFT;
}

return switch (motivation) {
case EXPANSION -> {
boolean hasClaims = !claimManager.getClaimsFor(actor.getId()).isEmpty();
NobleCharacter ch = actor.getActiveCharacter();
boolean recklessEligible = ch != null && ch.getCunning() < 2 && ch.getMilitary() >= 2
&& ch.getDominantMotivation() == Motivation.EXPANSION;
Debug.log("noble", "action-pick", actor.getName() + " EXPANSION hasClaims=" + hasClaims + " recklessEligible=" + recklessEligible);
if (hasClaims || recklessEligible) {
Debug.log("noble", "action-pick", actor.getName() + " -> ATTACK");
yield NobleAction.ATTACK;
}
Debug.log("noble", "action-pick", actor.getName() + " -> FABRICATE_CLAIM");
yield NobleAction.FABRICATE_CLAIM;
}
case WEALTH -> {
NobleHouse raidTarget = findRaidTarget(actor, allHouses, relationships, null);
yield raidTarget != null ? NobleAction.RAID : NobleAction.DEMAND;
}
case SECURITY -> {
List<String> rivals = relationships.getAll(actor.getId(), Relationship.RIVAL, allIds);
List<String> hostiles = relationships.getAll(actor.getId(), Relationship.HOSTILE, allIds);
if (!rivals.isEmpty() && actor.getDefense() < GameParameters.AI_FORTIFY_THRESHOLD) {
yield NobleAction.FORTIFY;
}
int allyCount = relationships.getAll(actor.getId(), Relationship.ALLIED, allIds).size();
if (!rivals.isEmpty() || !hostiles.isEmpty()) {
NobleCharacter ch = actor.getActiveCharacter();
if (ch != null && ch.getCunning() >= 2 && RNG.nextDouble() < 0.4) {
yield NobleAction.SABOTAGE;
}
}
yield allyCount < GameParameters.ALLIANCE_MAX_PER_HOUSE
? NobleAction.ALLY : NobleAction.FORTIFY;
}
case PRESTIGE -> {
NobleHouse supTarget = findSuperiorityTarget(actor, allHouses, relationships);
if (supTarget != null) yield NobleAction.DEMAND;
List<String> rivals = relationships.getAll(actor.getId(), Relationship.RIVAL, allIds);
List<String> hostiles = relationships.getAll(actor.getId(), Relationship.HOSTILE, allIds);
if (!rivals.isEmpty() || !hostiles.isEmpty()) {
NobleCharacter ch = actor.getActiveCharacter();
if (ch != null && ch.getCunning() >= 1 && RNG.nextDouble() < 0.5) {
yield NobleAction.SABOTAGE;
}
}
yield !rivals.isEmpty() ? NobleAction.SCHEME : NobleAction.FORTIFY;
}
};
}

// ─── Execution ───────────────────────────────────────────────────────────

private static List<String> execute(NobleHouse actor, NobleAction action,
Motivation motivation,
List<NobleHouse> allHouses,
RelationshipManager relationships,
ClaimManager claimManager,
main.map.ZoneManager zoneManager,
NobleArmyManager armyManager) {
List<String> log    = new ArrayList<>();
List<String> allIds = allHouseIds(allHouses);
NobleCharacter character = actor.getActiveCharacter();
int cunning   = character != null ? character.getCunning()   : 0;
int military  = character != null ? character.getMilitary()  : 0;
int diplomacy = character != null ? character.getDiplomacy() : 0;

switch (action) {

case FABRICATE_CLAIM -> {
if (!canSpendInfluence(actor, GameParameters.AI_INFLUENCE_COST_FABRICATE, log)) break;
String targetZone = findClaimTarget(actor, allHouses, claimManager);
if (targetZone == null) break;
List<String> myZones = new ArrayList<>(actor.getZoneIds());
boolean success = claimManager.fabricate(actor.getId(), targetZone, cunning, RNG,
myZones, zoneManager.getZones());
if (success) {
log.add(actor.getName() + " fabricates a claim on " + targetZone + ".");
for (NobleHouse other : allHouses) {
if (other.getZoneIds().contains(targetZone)) {
relationships.set(actor.getId(), other.getId(), Relationship.RIVAL);
log.add(other.getName() + " becomes rival with "
+ actor.getName() + " over the claim.");
break;
}
}
} else {
log.add(actor.getName() + " fails to fabricate a claim. Cunning insufficient.");
}
}

case ATTACK -> {
int myPower = estimateAttackPower(actor, armyManager);
Debug.log("noble", "attack", actor.getName() + " myPower=" + myPower);
if (myPower <= 0) {
Debug.log("noble", "attack", actor.getName() + " no power -> fallback");
executeFallback(actor, motivation, allHouses, relationships, claimManager,
zoneManager, armyManager, log);
break;
}

// ---- 1. Find best claim-based target ----
NobleHouse bestClaimTarget = null;
String bestClaimZone = null;
double bestClaimValue = 0;
List<Claim> claims = claimManager.getClaimsFor(actor.getId());
Debug.log("noble", "attack-claim", actor.getName() + " evaluating " + claims.size() + " claims");
for (Claim c : claims) {
for (NobleHouse other : allHouses) {
if (other == actor || other.isEliminated()) continue;
if (!other.getZoneIds().contains(c.getZoneId())) continue;
Relationship rel = relationships.get(actor.getId(), other.getId());
if (rel == Relationship.ALLIED || rel == Relationship.FRIENDLY) {
Debug.log("noble", "attack-claim", actor.getName() + " claim on " + c.getZoneId() + " owned by " + other.getName() + " SKIPPED (allied/friendly)");
continue;
}
int defPower = estimateDefenderPower(other, c.getZoneId());
if (defPower <= 0) continue;
main.map.Zone z = zoneManager.getZone(c.getZoneId());
if (z == null) continue;
double value = (double) z.getGoldProduction() / defPower;
Debug.log("noble", "attack-claim", actor.getName() + " claim on " + c.getZoneId() + " owner=" + other.getName() + " defPower=" + defPower + " value=" + value);
if (value > bestClaimValue) {
bestClaimValue = value;
bestClaimTarget = other;
bestClaimZone = c.getZoneId();
}
break;
}
}
Debug.log("noble", "attack-claim", actor.getName() + " bestClaimTarget=" + (bestClaimTarget != null ? bestClaimTarget.getName() : "null") + " zone=" + bestClaimZone + " value=" + bestClaimValue);

// ---- 2. Find reckless target (if leader qualifies) ----
NobleHouse recklessTarget = null;
String recklessZone = null;
double recklessValue = 0;
boolean tryReckless = (character != null && cunning < 2 && military >= 2
&& motivation == Motivation.EXPANSION);
Debug.log("noble", "attack-reckless", actor.getName() + " tryReckless=" + tryReckless);
if (tryReckless) {
Object[] reckless = findRecklessClaimlessTarget(actor, allHouses, relationships,
claimManager, zoneManager, armyManager);
if (reckless != null) {
recklessTarget = (NobleHouse) reckless[0];
recklessZone = (String) reckless[1];
int defPower = estimateDefenderPower(recklessTarget, recklessZone);
main.map.Zone z = zoneManager.getZone(recklessZone);
if (z != null && defPower > 0) {
recklessValue = (double) z.getGoldProduction() / defPower;
}
Debug.log("noble", "attack-reckless", actor.getName() + " found target=" + recklessTarget.getName() + " zone=" + recklessZone + " value=" + recklessValue);
} else {
Debug.log("noble", "attack-reckless", actor.getName() + " no reckless target found");
}
}

// ---- 3. Pick best overall target ----
NobleHouse target = null;
String attackZone = null;
boolean isClaimless = false;

if (recklessTarget != null && recklessValue >= bestClaimValue * GameParameters.RECKLESS_VALUE_MULTIPLIER) {
target = recklessTarget;
attackZone = recklessZone;
isClaimless = true;
Debug.log("noble", "attack-choose", actor.getName() + " -> RECKLESS " + attackZone);
} else if (bestClaimTarget != null) {
target = bestClaimTarget;
attackZone = bestClaimZone;
isClaimless = false;
Debug.log("noble", "attack-choose", actor.getName() + " -> CLAIM " + attackZone);
} else {
Debug.log("noble", "attack-choose", actor.getName() + " -> no target yet");
}

// ---- 4. Feasibility check ----
if (target != null) {
int defPower = estimateDefenderPower(target, attackZone);
double threshold = defPower * GameParameters.NORMAL_ATTACK_STRENGTH_THRESHOLD;
boolean feasible = myPower >= threshold;
Debug.log("noble", "attack-feasibility", actor.getName() + " target=" + target.getName() + " zone=" + attackZone + " defPower=" + defPower + " needed=" + threshold + " myPower=" + myPower + " feasible=" + feasible);
if (!feasible) {
target = null;
Debug.log("noble", "attack-feasibility", actor.getName() + " NOT FEASIBLE -> fallback");
} else if (isRecklessAtWar(actor)) {
// Reckless leaders attack as soon as feasible, even if chest not full
Debug.log("noble", "attack-reckless-ready", actor.getName() + " reckless — attacking with current forces");
} else {
// Non‑reckless: check war chest readiness
int chestTarget = getWarChestTarget(actor, allHouses, relationships, armyManager);
if (actor.getGold() < chestTarget) {
Debug.log("noble", "attack-feasibility", actor.getName() + " war chest not ready (have=" + actor.getGold() + " need=" + chestTarget + ") -> fallback");
target = null;
}
}
}

if (target == null) {
executeFallback(actor, motivation, allHouses, relationships, claimManager,
zoneManager, armyManager, log);
break;
}

// ---- 5. Execute attack ----
Debug.log("noble", "attack-exec", actor.getName() + " attacking " + attackZone + " isClaimless=" + isClaimless);
if (!canSpendInfluence(actor, GameParameters.AI_INFLUENCE_COST_ATTACK, log)) break;

List<NobleArmy> actorArmies = armyManager.getArmiesForHouse(actor.getId());
NobleArmy army = null;
for (NobleArmy a : actorArmies) {
if (!a.hasPendingOrder()) { army = a; break; }
}
if (army == null
&& actor.getNobleManpower() >= GameParameters.NOBLE_ARMY_MIN_RECRUIT_SIZE
&& actor.getGold() >= GameParameters.NOBLE_ARMY_RECRUIT_GOLD_THRESHOLD) {
int recruitSize = Math.max(GameParameters.NOBLE_ARMY_MIN_RECRUIT_SIZE,
(int)(actor.getNobleManpower() * GameParameters.NOBLE_ARMY_RECRUIT_FRACTION));
army = armyManager.recruit(actor, recruitSize);
if (army != null) {
log.add(actor.getName() + " raises an army of " + army.getSize() + " for the attack.");
}
}
if (army != null && !army.hasPendingOrder() && army.getSize() > 0) {
armyManager.moveArmy(army, attackZone);
army.issueOrder(NobleArmy.OrderType.ATTACK, attackZone);
if (isClaimless) {
log.add(actor.getName() + " marches on " + attackZone + " without a claim.");
} else {
log.add(actor.getName() + " marches on " + attackZone + ".");
}
updateThreatenedStatus(actor, allHouses, relationships,
isClaimless ? GameParameters.THREATENED_CLAIMLESS_MULTIPLIER : 1.0);
triggerAllyDefense(target, actor, allHouses, relationships, log);
}
}

case RAID -> {
NobleHouse raidTarget = findRaidTarget(actor, allHouses, relationships, zoneManager);
if (raidTarget == null) break;
if (!canSpendInfluence(actor, GameParameters.AI_INFLUENCE_COST_RAID, log)) break;
String raidedZone = pickRaidableZone(raidTarget, zoneManager);
if (raidedZone == null) {
log.add(actor.getName() + " finds no raidable zone in " + raidTarget.getName() + ".");
break;
}
List<NobleArmy> actorArmies = armyManager.getArmiesForHouse(actor.getId());
NobleArmy raidArmy = null;
for (NobleArmy a : actorArmies) {
if (!a.hasPendingOrder()) { raidArmy = a; break; }
}
if (raidArmy == null
&& actor.getNobleManpower() >= GameParameters.NOBLE_ARMY_MIN_RECRUIT_SIZE
&& actor.getGold() >= GameParameters.NOBLE_ARMY_RECRUIT_GOLD_THRESHOLD) {
int recruitSize = Math.max(GameParameters.NOBLE_ARMY_MIN_RECRUIT_SIZE,
(int)(actor.getNobleManpower() * GameParameters.NOBLE_ARMY_RECRUIT_FRACTION));
raidArmy = armyManager.recruit(actor, recruitSize);
if (raidArmy != null) {
log.add(actor.getName() + " raises a raiding party of " + raidArmy.getSize() + ".");
}
}
if (raidArmy != null && !raidArmy.hasPendingOrder() && raidArmy.getSize() > 0) {
armyManager.moveArmy(raidArmy, raidedZone);
raidArmy.issueOrder(NobleArmy.OrderType.RAID, raidedZone);
log.add(actor.getName() + " sends raiders toward " + raidedZone + ".");
}
}

case DEMAND -> {
if (motivation == Motivation.PRESTIGE) {
NobleHouse supTarget = findSuperiorityTarget(actor, allHouses, relationships);
if (supTarget == null) break;
if (!canSpendInfluence(actor, GameParameters.AI_INFLUENCE_COST_DEMAND, log)) break;
boolean accepted = evaluateAcknowledgeSuperiority(actor, supTarget);
if (accepted) {
supTarget.addPrestige(-GameParameters.DEMAND_PRESTIGE_AMOUNT);
actor.addPrestige(GameParameters.DEMAND_PRESTIGE_AMOUNT);
log.add(supTarget.getName() + " acknowledges the superiority of "
+ actor.getName() + ". Prestige transferred.");
} else {
log.add(supTarget.getName() + " refuses to acknowledge "
+ actor.getName() + "'s superiority.");
relationships.worsen(actor.getId(), supTarget.getId());
}
break;
}
NobleHouse demTarget = findDemandTarget(actor, allHouses, relationships);
if (demTarget == null) break;
if (!canSpendInfluence(actor, GameParameters.AI_INFLUENCE_COST_DEMAND, log)) break;
DemandType type = demandTypeForMotivation(motivation);
boolean accepted = evaluateDemand(actor, demTarget, relationships, allIds, diplomacy);
if (accepted) {
applyDemand(actor, demTarget, type, log);
} else {
log.add(demTarget.getName() + " refuses " + actor.getName() + "'s demand.");
relationships.worsen(actor.getId(), demTarget.getId());
}
}

case SCHEME -> {
List<String> rivals = relationships.getAll(actor.getId(), Relationship.RIVAL, allIds);
if (rivals.isEmpty()) break;
if (!canSpendInfluence(actor, GameParameters.AI_INFLUENCE_COST_SCHEME, log)) break;
String targetId = rivals.get(RNG.nextInt(rivals.size()));
NobleHouse schemeTarget = findById(targetId, allHouses);
if (schemeTarget == null) break;
double successChance = GameParameters.SCHEME_BASE_SUCCESS_CHANCE
+ cunning * GameParameters.SCHEME_CUNNING_BONUS_PER_POINT;
if (RNG.nextDouble() < successChance) {
schemeTarget.addPrestige(-GameParameters.AI_SCHEME_PRESTIGE_LOSS);
actor.addPrestige(GameParameters.AI_SCHEME_PRESTIGE_GAIN);
log.add(actor.getName() + " schemes against " + schemeTarget.getName()
+ ". Their prestige suffers.");
} else {
log.add(actor.getName() + "'s scheme against " + schemeTarget.getName()
+ " is discovered. Relation worsens.");
relationships.worsen(actor.getId(), schemeTarget.getId());
}
}

case FORTIFY -> {
int cost = GameParameters.AI_FORTIFY_GOLD_COST;
if (actor.getGold() < cost) break;
if (actor.getGold() - cost < getWarChestTarget(actor, allHouses, relationships, armyManager)) break;
actor.addGold(-cost);
actor.addDefense(GameParameters.AI_FORTIFY_DEFENSE_GAIN);
String fortZone = actor.getCapitalZoneId();
if (fortZone != null) {
actor.addGarrison(fortZone, GameParameters.FORTIFY_GARRISON_GAIN);
}
log.add(actor.getName() + " fortifies. Defense +"
+ GameParameters.AI_FORTIFY_DEFENSE_GAIN
+ ", Garrison +" + GameParameters.FORTIFY_GARRISON_GAIN + ".");
}

case ALLY -> {
List<String> currentAllies = relationships.getAll(actor.getId(), Relationship.ALLIED, allIds);
if (currentAllies.size() >= GameParameters.ALLIANCE_MAX_PER_HOUSE) break;
if (!canSpendInfluence(actor, GameParameters.AI_INFLUENCE_COST_ALLY, log)) break;
NobleHouse allyTarget = findAllyTarget(actor, allHouses, relationships);
if (allyTarget == null) break;
Relationship currentRel = relationships.get(actor.getId(), allyTarget.getId());
if (currentRel == Relationship.FRIENDLY || currentRel == Relationship.ALLIED) break;
boolean tooWeak = allyTarget.getTotalArmySize()
< actor.getTotalArmySize() * GameParameters.ALLIANCE_MIN_ARMY_FRACTION;
if (tooWeak) {
log.add(actor.getName() + " considers alliance with "
+ allyTarget.getName() + " but deems them too weak.");
break;
}
double acceptChance = GameParameters.ALLY_BASE_ACCEPT_CHANCE
+ diplomacy * GameParameters.ALLY_DIPLOMACY_BONUS_PER_POINT;
if (RNG.nextDouble() < acceptChance) {
relationships.set(actor.getId(), allyTarget.getId(), Relationship.ALLIED);
log.add(actor.getName() + " and " + allyTarget.getName() + " form an alliance.");
} else {
log.add(allyTarget.getName() + " declines alliance with " + actor.getName() + ".");
}
}

case GIFT -> {
NobleHouse giftTarget = findGiftTarget(actor, allHouses, relationships);
if (giftTarget == null) break;
List<Claim> actorClaims = claimManager.getClaimsFor(actor.getId());
Claim claimOnTarget = actorClaims.stream()
.filter(c -> giftTarget.getZoneIds().contains(c.getZoneId()))
.findFirst().orElse(null);
if (claimOnTarget != null) {
claimManager.removeClaim(actor.getId(), claimOnTarget.getZoneId());
relationships.improve(actor.getId(), giftTarget.getId());
log.add(actor.getName() + " forfeits claim on " + claimOnTarget.getZoneId()
+ " as gift to " + giftTarget.getName() + ". Relations improve.");
} else if (actor.getGold() >= GameParameters.GIFT_MONEY_AMOUNT
&& actor.getGold() - GameParameters.GIFT_MONEY_AMOUNT >= getWarChestTarget(actor, allHouses, relationships, armyManager)) {
actor.addGold(-GameParameters.GIFT_MONEY_AMOUNT);
giftTarget.addGold(GameParameters.GIFT_MONEY_AMOUNT);
relationships.improve(actor.getId(), giftTarget.getId());
log.add(actor.getName() + " gifts " + GameParameters.GIFT_MONEY_AMOUNT
+ " gold to " + giftTarget.getName() + ". Relations improve.");
}
}

case SUPPORT_RIVAL -> {
List<String> allies = relationships.getAll(actor.getId(), Relationship.ALLIED, allIds);
if (allies.isEmpty()) break;
if (!canSpendInfluence(actor, GameParameters.AI_INFLUENCE_COST_SUPPORT, log)) break;
String allyId = allies.get(RNG.nextInt(allies.size()));
NobleHouse ally = findById(allyId, allHouses);
if (ally == null) break;
int support = (int)(actor.getGold() * GameParameters.AI_SUPPORT_GOLD_FRACTION);
actor.addGold(-support);
ally.addGold(support);
log.add(actor.getName() + " sends " + support
+ " gold to " + ally.getName() + " in support.");
}

case SABOTAGE -> {
NobleHouse sabTarget = findSabotageTarget(actor, allHouses, relationships);
if (sabTarget == null) break;
if (actor.getGold() < GameParameters.AI_SABOTAGE_GOLD_COST) break;
if (actor.getGold() - GameParameters.AI_SABOTAGE_GOLD_COST < getWarChestTarget(actor, allHouses, relationships, armyManager)) break;
if (!canSpendInfluence(actor, GameParameters.AI_INFLUENCE_COST_SABOTAGE, log)) break;
actor.addGold(-GameParameters.AI_SABOTAGE_GOLD_COST);

double successChance = GameParameters.SABOTAGE_BASE_SUCCESS_CHANCE
+ cunning * GameParameters.SABOTAGE_CUNNING_BONUS_PER_POINT;
if (RNG.nextDouble() < successChance) {
List<String> validZones = new ArrayList<>();
for (String zid : sabTarget.getZoneIds()) {
if (sabTarget.getFortificationFor(zid) > 0) validZones.add(zid);
}
if (!validZones.isEmpty()) {
String sabZone = validZones.get(RNG.nextInt(validZones.size()));
sabTarget.addFortification(sabZone, -1);
log.add(actor.getName() + " sabotages " + sabTarget.getName()
+ "'s fortifications at " + sabZone + ".");
}
} else {
log.add(actor.getName() + "'s sabotage attempt against "
+ sabTarget.getName() + " fails.");
}
}
}

return log;
}

// ─── Ally defense ────────────────────────────────────────────────────────

private static void triggerAllyDefense(NobleHouse attacked, NobleHouse attacker,
List<NobleHouse> allHouses,
RelationshipManager relationships,
List<String> log) {
List<String> allIds = allHouseIds(allHouses);
List<String> allies = new ArrayList<>(
relationships.getAll(attacked.getId(), Relationship.ALLIED, allIds));

for (String allyId : allies) {
NobleHouse ally = findById(allyId, allHouses);
if (ally == null || ally.isEliminated()) continue;
Relationship allyWithAttacker = relationships.get(ally.getId(), attacker.getId());
if (allyWithAttacker == Relationship.ALLIED
|| allyWithAttacker == Relationship.FRIENDLY) continue;
boolean strongEnough = ally.getTotalArmySize()
>= attacker.getTotalArmySize() * GameParameters.ALLY_DEFENSE_MIN_STRENGTH_FRACTION;
if (strongEnough) {
int allyMilitary = ally.getActiveCharacter() != null
? ally.getActiveCharacter().getMilitary() : 0;
ArmyForce allyForce = new ArmyForce(ally.getId(),
ally.getTotalArmySize(),
attacked.getDefense(),
allyMilitary);
ArmyForce atkForce = new ArmyForce(attacker.getId(),
attacker.getTotalArmySize(),
0,
0);
CombatResult defResult = CombatResolver.resolve(atkForce, allyForce);
log.add(ally.getName() + " joins defense of " + attacked.getName() + "!");
log.addAll(defResult.getLog());
attacker.setTotalArmySize(atkForce.getArmySize());
ally.setTotalArmySize(allyForce.getArmySize());
} else {
relationships.worsen(ally.getId(), attacked.getId());
log.add(ally.getName() + " fails to honor alliance with "
+ attacked.getName() + ". Relations cool.");
}
}
}

// ─── Demand evaluation ───────────────────────────────────────────────────

public static boolean evaluateDemand(NobleHouse requester, NobleHouse target,
RelationshipManager relationships,
List<String> allIds,
int requesterDiplomacy) {
double score = GameParameters.DEMAND_BASE_SCORE;
score += (requester.getPrestige() - target.getPrestige()) * GameParameters.DEMAND_PRESTIGE_WEIGHT;
score += (requester.getTotalArmySize() - target.getTotalArmySize()) * GameParameters.DEMAND_ARMY_WEIGHT;
score += switch (relationships.get(requester.getId(), target.getId())) {
case ALLIED, FRIENDLY -> GameParameters.DEMAND_ALLIED_BONUS;
case NEUTRAL          -> 0;
case HOSTILE          -> GameParameters.DEMAND_RIVAL_PENALTY / 2.0;
case RIVAL            -> GameParameters.DEMAND_RIVAL_PENALTY;
};
if (relationships.shareRival(requester.getId(), target.getId(), allIds)) {
score += GameParameters.DEMAND_SHARED_RIVAL_BONUS;
}
score += requesterDiplomacy * GameParameters.DEMAND_DIPLOMACY_BONUS_PER_POINT;
score += (RNG.nextDouble() - 0.5) * 2 * GameParameters.DEMAND_RANDOM_RANGE;
return score >= GameParameters.DEMAND_ACCEPT_THRESHOLD;
}

private static boolean evaluateAcknowledgeSuperiority(NobleHouse demander, NobleHouse target) {
int demanderMilitary = demander.getActiveCharacter() != null
? demander.getActiveCharacter().getMilitary() : 0;
int targetMilitary   = target.getActiveCharacter() != null
? target.getActiveCharacter().getMilitary() : 0;
if (demanderMilitary <= targetMilitary) return false;
double base  = GameParameters.SUPERIORITY_BASE_ACCEPT_CHANCE;
double noise = (RNG.nextDouble() - 0.5) * GameParameters.SUPERIORITY_RANDOM_RANGE;
return (base + noise) >= 0.5;
}

// ─── Target finders ──────────────────────────────────────────────────────

private static NobleHouse findAttackTarget(NobleHouse actor,
List<NobleHouse> allHouses,
RelationshipManager relationships,
ClaimManager claimManager) {
List<Claim> claims   = claimManager.getClaimsFor(actor.getId());
NobleHouse  best     = null;
int         bestArmy = Integer.MAX_VALUE;
for (Claim claim : claims) {
for (NobleHouse other : allHouses) {
if (other == actor || other.isEliminated()) continue;
if (!other.getZoneIds().contains(claim.getZoneId())) continue;
Relationship rel = relationships.get(actor.getId(), other.getId());
if (rel == Relationship.ALLIED || rel == Relationship.FRIENDLY) continue;
if (other.getTotalArmySize() < actor.getTotalArmySize()
&& other.getTotalArmySize() < bestArmy) {
best     = other;
bestArmy = other.getTotalArmySize();
}
}
}
return best;
}

private static NobleHouse findRaidTarget(NobleHouse actor,
List<NobleHouse> allHouses,
RelationshipManager relationships,
main.map.ZoneManager zoneManager) {
NobleHouse best     = null;
int        bestGold = 0;
for (NobleHouse other : allHouses) {
if (other == actor || other.isEliminated()) continue;
Relationship rel = relationships.get(actor.getId(), other.getId());
if (rel == Relationship.ALLIED || rel == Relationship.FRIENDLY) continue;
if (zoneManager != null && pickRaidableZone(other, zoneManager) == null) continue;
if (other.getGold() > bestGold) {
best     = other;
bestGold = other.getGold();
}
}
return best;
}

private static NobleHouse findDemandTarget(NobleHouse actor,
List<NobleHouse> allHouses,
RelationshipManager relationships) {
NobleHouse best      = null;
int        bestScore = Integer.MIN_VALUE;
for (NobleHouse other : allHouses) {
if (other == actor || other.isEliminated()) continue;
if (relationships.get(actor.getId(), other.getId()) == Relationship.RIVAL) continue;
int score = -other.getGold();
if (score > bestScore) { best = other; bestScore = score; }
}
return best;
}

private static NobleHouse findSuperiorityTarget(NobleHouse actor,
List<NobleHouse> allHouses,
RelationshipManager relationships) {
int actorMilitary = actor.getActiveCharacter() != null
? actor.getActiveCharacter().getMilitary() : 0;
for (NobleHouse other : allHouses) {
if (other == actor || other.isEliminated()) continue;
if (relationships.get(actor.getId(), other.getId()) == Relationship.RIVAL) continue;
int otherMilitary = other.getActiveCharacter() != null
? other.getActiveCharacter().getMilitary() : 0;
if (actorMilitary > otherMilitary) return other;
}
return null;
}

private static NobleHouse findAllyTarget(NobleHouse actor,
List<NobleHouse> allHouses,
RelationshipManager relationships) {
for (NobleHouse other : allHouses) {
if (other == actor || other.isEliminated()) continue;
if (relationships.get(actor.getId(), other.getId()) == Relationship.NEUTRAL) return other;
}
return null;
}

private static NobleHouse findGiftTarget(NobleHouse actor,
List<NobleHouse> allHouses,
RelationshipManager relationships) {
NobleHouse best     = null;
int        bestArmy = 0;
for (NobleHouse other : allHouses) {
if (other == actor || other.isEliminated()) continue;
Relationship rel = relationships.get(actor.getId(), other.getId());
if (rel == Relationship.RIVAL || rel == Relationship.ALLIED) continue;
if (other.getTotalArmySize() > bestArmy) {
best     = other;
bestArmy = other.getTotalArmySize();
}
}
return best;
}

private static String findClaimTarget(NobleHouse actor,
List<NobleHouse> allHouses,
ClaimManager claimManager) {
for (NobleHouse other : allHouses) {
if (other == actor || other.isEliminated()) continue;
for (String zoneId : other.getZoneIds()) {
if (!claimManager.hasClaim(actor.getId(), zoneId)) return zoneId;
}
}
return null;
}

/**
* Find a reckless claimless target: only for EXPANSION leaders with low cunning & high military.
* Target must be significantly weaker and more valuable than any claimed zone.
* Returns [targetHouse, zoneId] or null.
*/

private static Object[] findRecklessClaimlessTarget(NobleHouse actor,
List<NobleHouse> allHouses,
RelationshipManager relationships,
ClaimManager claimManager,
main.map.ZoneManager zoneManager,
NobleArmyManager armyManager) {
int myPower = estimateAttackPower(actor, armyManager);
Debug.log("noble", "reckless-scan", actor.getName() + " myPower=" + myPower);
if (myPower <= 0) {
Debug.log("noble", "reckless-scan", actor.getName() + " no power -> abort");
return null;
}

// compute best value among claimed zones (as benchmark)
double bestClaimedValue = 0;
for (Claim c : claimManager.getClaimsFor(actor.getId())) {
for (NobleHouse other : allHouses) {
if (other == actor || other.isEliminated()) continue;
if (!other.getZoneIds().contains(c.getZoneId())) continue;
int defPower = estimateDefenderPower(other, c.getZoneId());
if (defPower <= 0) continue;
main.map.Zone z = zoneManager.getZone(c.getZoneId());
if (z == null) continue;
double value = (double) z.getGoldProduction() / defPower;
if (value > bestClaimedValue) bestClaimedValue = value;
break;
}
}
Debug.log("noble", "reckless-scan", actor.getName() + " bestClaimedValue=" + bestClaimedValue);

// scan rivals/hostiles for high-value weak zones
NobleHouse bestTarget = null;
String bestZone = null;
double bestValue = 0;
int scanned = 0, rejectedTooStrong = 0;

for (NobleHouse other : allHouses) {
if (other == actor || other.isEliminated()) continue;
Relationship rel = relationships.get(actor.getId(), other.getId());
if (rel != Relationship.RIVAL && rel != Relationship.HOSTILE) {
continue;
}
for (String zid : other.getZoneIds()) {
scanned++;
int defPower = estimateDefenderPower(other, zid);
if (defPower <= 0) continue;
double neededPower = defPower * GameParameters.RECKLESS_MIN_STRENGTH;
boolean strongEnough = myPower >= neededPower;
main.map.Zone z = zoneManager.getZone(zid);
if (z == null) continue;
double value = (double) z.getGoldProduction() / defPower;
Debug.log("noble", "reckless-scan", actor.getName() + " zone=" + zid
+ " owner=" + other.getName()
+ " defPower=" + defPower
+ " neededPower=" + neededPower
+ " strongEnough=" + strongEnough
+ " value=" + value);
if (!strongEnough) {
rejectedTooStrong++;
continue;
}
if (value > bestValue) {
bestValue = value;
bestTarget = other;
bestZone = zid;
}
}
}

Debug.log("noble", "reckless-scan", actor.getName() + " scanned=" + scanned
+ " rejectedTooStrong=" + rejectedTooStrong
+ " bestTarget=" + (bestTarget != null ? bestTarget.getName() : "null")
+ " bestZone=" + bestZone + " bestValue=" + bestValue);

if (bestTarget == null) {
Debug.log("noble", "reckless-scan", actor.getName() + " no valid target -> abort");
return null;
}

double requiredValue = bestClaimedValue * GameParameters.RECKLESS_VALUE_MULTIPLIER;
boolean passes = bestValue >= requiredValue;
Debug.log("noble", "reckless-scan", actor.getName() + " bestValue=" + bestValue
+ " requiredValue=" + requiredValue + " passes=" + passes);
if (!passes) {
Debug.log("noble", "reckless-scan", actor.getName() + " not better enough -> abort");
return null;
}

Debug.log("noble", "reckless-scan", actor.getName() + " -> FOUND " + bestZone);
return new Object[] { bestTarget, bestZone };
}

private static NobleHouse findClaimlessAttackTarget(NobleHouse actor,
List<NobleHouse> allHouses,
RelationshipManager relationships) {
NobleHouse best     = null;
int        bestArmy = Integer.MAX_VALUE;
for (NobleHouse other : allHouses) {
if (other == actor || other.isEliminated()) continue;
Relationship rel = relationships.get(actor.getId(), other.getId());
if (rel != Relationship.RIVAL && rel != Relationship.HOSTILE) continue;
if (other.getTotalArmySize() < actor.getTotalArmySize()
&& other.getTotalArmySize() < bestArmy) {
best     = other;
bestArmy = other.getTotalArmySize();
}
}
return best;
}

private static NobleHouse findSabotageTarget(NobleHouse actor,
List<NobleHouse> allHouses,
RelationshipManager relationships) {
List<NobleHouse> candidates = new ArrayList<>();
for (NobleHouse other : allHouses) {
if (other == actor || other.isEliminated()) continue;
Relationship rel = relationships.get(actor.getId(), other.getId());
if (rel != Relationship.RIVAL && rel != Relationship.HOSTILE) continue;
// Must have at least one zone with fortification > 0
boolean hasFort = false;
for (String zid : other.getZoneIds()) {
if (other.getFortificationFor(zid) > 0) { hasFort = true; break; }
}
if (hasFort) candidates.add(other);
}
if (candidates.isEmpty()) return null;
return candidates.get(RNG.nextInt(candidates.size()));
}

private static String pickRaidableZone(NobleHouse target,
main.map.ZoneManager zoneManager) {
for (String zoneId : target.getZoneIds()) {
ZoneState state = zoneManager.getState(zoneId);
if (state != null && !state.isRecentlyRaided()) return zoneId;
}
return null;
}

// ─── Demand application ──────────────────────────────────────────────────

private static void applyDemand(NobleHouse requester, NobleHouse target,
DemandType type, List<String> log) {
switch (type) {
case WEALTH -> {
int amount = (int)(target.getGold() * GameParameters.DEMAND_WEALTH_FRACTION);
target.addGold(-amount);
requester.addGold(amount);
log.add(target.getName() + " yields " + amount + " gold to " + requester.getName() + ".");
}
case ARMY -> {
requester.addToRaisedArmy(GameParameters.DEMAND_ARMY_AMOUNT);
log.add(target.getName() + " sends " + GameParameters.DEMAND_ARMY_AMOUNT
+ " soldiers to " + requester.getName() + ".");
}
case ACKNOWLEDGE_SUPERIORITY -> {
target.addPrestige(-GameParameters.DEMAND_PRESTIGE_AMOUNT);
requester.addPrestige(GameParameters.DEMAND_PRESTIGE_AMOUNT);
log.add(target.getName() + " acknowledges the superiority of " + requester.getName() + ".");
}
}
}

private static DemandType demandTypeForMotivation(Motivation m) {
return switch (m) {
case WEALTH              -> DemandType.WEALTH;
case EXPANSION, SECURITY -> DemandType.ARMY;
case PRESTIGE            -> DemandType.ACKNOWLEDGE_SUPERIORITY;
};
}

// ─── Power estimation helpers ───────────────────────────────────────────

/** Max army size a house could recruit this turn (raw soldiers). */
private static int maxRecruitableSize(NobleHouse house) {
int manpower = house.getNobleManpower();
int gold     = house.getGold();
int minSize  = GameParameters.NOBLE_ARMY_MIN_RECRUIT_SIZE;
if (manpower < minSize || gold < GameParameters.NOBLE_ARMY_RECRUIT_GOLD_THRESHOLD) return 0;
int maxSustainableSize = (int)(gold / (GameParameters.NOBLE_UPKEEP_COST_PER_SOLDIER * 2.0));
int rawSize = Math.max(minSize, (int)(manpower * GameParameters.NOBLE_ARMY_RECRUIT_FRACTION));
return Math.max(minSize, Math.min(rawSize, maxSustainableSize));
}

/** Effective combat power a house can field RIGHT NOW or after recruiting. */
public static int estimateAttackPower(NobleHouse house, NobleArmyManager armyManager) {
int milSkill = house.getActiveCharacter() != null ? house.getActiveCharacter().getMilitary() : 0;
double mult  = 1.0 + milSkill * GameParameters.MILITARY_SKILL_BONUS_PER_POINT;
// existing idle army
for (NobleArmy a : armyManager.getArmiesForHouse(house.getId())) {
if (!a.hasPendingOrder() && a.getSize() > 0) {
return (int)(a.getSize() * mult);
}
}
// can recruit?
int size = maxRecruitableSize(house);
return (int)(size * mult);
}

/** Effective combat power a member could bring to a coalition (same logic). */
public static int estimateMemberPower(NobleHouse member, NobleArmyManager armyManager) {
return estimateAttackPower(member, armyManager);
}

/** Effective defender power at a specific zone, counting garrison + fortification. */
public static int estimateDefenderPower(NobleHouse defender, String zoneId) {
int garrison = defender.getGarrisonFor(zoneId);
int fort     = defender.getFortificationFor(zoneId);
int mil      = defender.getActiveCharacter() != null ? defender.getActiveCharacter().getMilitary() : 0;
double mult  = 1.0 + mil * GameParameters.MILITARY_SKILL_BONUS_PER_POINT;
double defReduction = 1.0 - (fort / 100.0) * GameParameters.COMBAT_DEFENSE_REDUCTION;
return (int)(garrison * mult * defReduction);
}

// ─── Helpers ─────────────────────────────────────────────────────────────

private static double militaryMultiplier(int military) {
return 1.0 + military * GameParameters.MILITARY_SKILL_BONUS_PER_POINT;
}

private static boolean canSpendInfluence(NobleHouse house, int cost, List<String> log) {
if (house.getInfluence() < cost) return false;
house.addInfluence(-cost);
return true;
}

private static List<String> allHouseIds(List<NobleHouse> houses) {
List<String> ids = new ArrayList<>();
for (NobleHouse h : houses) ids.add(h.getId());
return ids;
}

private static NobleHouse findById(String id, List<NobleHouse> houses) {
for (NobleHouse h : houses) if (h.getId().equals(id)) return h;
return null;
}

// ─── Attack fallback ────────────────────────────────────────────────────

/** Called when an ATTACK action is not feasible. Picks a non-attack action. */
private static void executeFallback(NobleHouse actor, Motivation motivation,
List<NobleHouse> allHouses,
RelationshipManager relationships,
ClaimManager claimManager,
main.map.ZoneManager zoneManager,
NobleArmyManager armyManager,
List<String> log) {
Debug.log("noble", "fallback", actor.getName() + " motivation=" + motivation + " trying alternatives");
// Try a few alternative actions in priority order
if (motivation == Motivation.EXPANSION) {
// Try to fabricate a claim if no claims exist
if (claimManager.getClaimsFor(actor.getId()).isEmpty()
&& actor.getInfluence() >= GameParameters.AI_INFLUENCE_COST_FABRICATE) {
String targetZone = findClaimTarget(actor, allHouses, claimManager);
if (targetZone != null) {
List<String> myZones = new ArrayList<>(actor.getZoneIds());
NobleCharacter ch = actor.getActiveCharacter();
int cunning = ch != null ? ch.getCunning() : 0;
boolean success = claimManager.fabricate(actor.getId(), targetZone, cunning, RNG,
myZones, zoneManager.getZones());
if (success) {
log.add(actor.getName() + " fabricates a claim on " + targetZone + ".");
for (NobleHouse other : allHouses) {
if (other.getZoneIds().contains(targetZone)) {
relationships.set(actor.getId(), other.getId(), Relationship.RIVAL);
log.add(other.getName() + " becomes rival with "
+ actor.getName() + " over the claim.");
break;
}
}
} else {
log.add(actor.getName() + " fails to fabricate a claim. Cunning insufficient.");
}
return;
}
}
// Try to raid
NobleHouse raidTarget = findRaidTarget(actor, allHouses, relationships, zoneManager);
if (raidTarget != null && actor.getInfluence() >= GameParameters.AI_INFLUENCE_COST_RAID) {
String raidedZone = pickRaidableZone(raidTarget, zoneManager);
if (raidedZone != null) {
NobleArmy raidArmy = null;
for (NobleArmy a : armyManager.getArmiesForHouse(actor.getId())) {
if (!a.hasPendingOrder()) { raidArmy = a; break; }
}
if (raidArmy == null
&& actor.getNobleManpower() >= GameParameters.NOBLE_ARMY_MIN_RECRUIT_SIZE
&& actor.getGold() >= GameParameters.NOBLE_ARMY_RECRUIT_GOLD_THRESHOLD) {
int recruitSize = Math.max(GameParameters.NOBLE_ARMY_MIN_RECRUIT_SIZE,
(int)(actor.getNobleManpower() * GameParameters.NOBLE_ARMY_RECRUIT_FRACTION));
raidArmy = armyManager.recruit(actor, recruitSize);
if (raidArmy != null) {
log.add(actor.getName() + " raises a raiding party of " + raidArmy.getSize() + ".");
}
}
if (raidArmy != null && !raidArmy.hasPendingOrder() && raidArmy.getSize() > 0) {
armyManager.moveArmy(raidArmy, raidedZone);
raidArmy.issueOrder(NobleArmy.OrderType.RAID, raidedZone);
log.add(actor.getName() + " sends raiders toward " + raidedZone + ".");
return;
}
}
}
// Fortify as last resort
if (actor.getGold() >= GameParameters.AI_FORTIFY_GOLD_COST) {
actor.addGold(-GameParameters.AI_FORTIFY_GOLD_COST);
actor.addDefense(GameParameters.AI_FORTIFY_DEFENSE_GAIN);
String fortZone = actor.getCapitalZoneId();
if (fortZone != null) {
actor.addGarrison(fortZone, GameParameters.FORTIFY_GARRISON_GAIN);
}
log.add(actor.getName() + " fortifies. Defense +"
+ GameParameters.AI_FORTIFY_DEFENSE_GAIN
+ ", Garrison +" + GameParameters.FORTIFY_GARRISON_GAIN + ".");
}
} else {
// For non-EXPANSION motivations, just do a simple action
if (actor.getGold() >= GameParameters.AI_FORTIFY_GOLD_COST) {
actor.addGold(-GameParameters.AI_FORTIFY_GOLD_COST);
actor.addDefense(GameParameters.AI_FORTIFY_DEFENSE_GAIN);
String fortZone = actor.getCapitalZoneId();
if (fortZone != null) {
actor.addGarrison(fortZone, GameParameters.FORTIFY_GARRISON_GAIN);
}
log.add(actor.getName() + " fortifies. Defense +"
+ GameParameters.AI_FORTIFY_DEFENSE_GAIN
+ ", Garrison +" + GameParameters.FORTIFY_GARRISON_GAIN + ".");
}
}
}

// ─── Claim decay ────────────────────────────────────────────────────────

/**
* Process claim decay for all houses. Called once per turn from NobleHouseManager.
* Each house has a chance to lose a random claim unless they pay influence.
*/
public static void tickClaimDecay(List<NobleHouse> allHouses,
RelationshipManager relationships,
ClaimManager claimManager,
List<String> log) {
for (NobleHouse house : allHouses) {
if (house.isEliminated()) continue;
Claim decayed = claimManager.rollClaimDecay(house.getId(), RNG);
if (decayed == null) continue;

// Decide whether to pay influence to keep the claim
boolean keep = false;
// Keep if the claimed zone is owned by a RIVAL or HOSTILE
for (NobleHouse other : allHouses) {
if (other.getZoneIds().contains(decayed.getZoneId())) {
Relationship rel = relationships.get(house.getId(), other.getId());
if (rel == Relationship.RIVAL || rel == Relationship.HOSTILE) {
keep = true;
break;
}
}
}
// Keep if EXPANSION-motivated and has an idle army
if (!keep && house.getActiveCharacter() != null
&& house.getActiveCharacter().getDominantMotivation() == Motivation.EXPANSION) {
keep = true;
}

if (keep && house.getInfluence() >= GameParameters.CLAIM_DECAY_INFLUENCE_COST) {
house.addInfluence(-GameParameters.CLAIM_DECAY_INFLUENCE_COST);
} else {
claimManager.removeClaim(house.getId(), decayed.getZoneId());
if (keep) {
// Wanted to keep but couldn't afford
}
}
}
}

// ─── War chest ──────────────────────────────────────────────────────────

/** Compute the gold target this house wants to keep in reserve. */
private static int getWarChestTarget(NobleHouse actor,
List<NobleHouse> allHouses,
RelationshipManager relationships,
NobleArmyManager armyManager) {
// 1. Estimate strongest enemy power among rivals/hostiles
int maxEnemyPower = 0;
for (NobleHouse other : allHouses) {
if (other == actor || other.isEliminated()) continue;
Relationship rel = relationships.get(actor.getId(), other.getId());
if (rel != Relationship.RIVAL && rel != Relationship.HOSTILE) continue;
for (String zid : other.getZoneIds()) {
int defPower = estimateDefenderPower(other, zid);
if (defPower > maxEnemyPower) maxEnemyPower = defPower;
}
}
// fallback if no enemies: at least a small garrison
if (maxEnemyPower < 5) maxEnemyPower = 5;

// 2. Soldiers needed to match that power
int myMil = actor.getActiveCharacter() != null ? actor.getActiveCharacter().getMilitary() : 0;
double myMult = 1.0 + myMil * GameParameters.MILITARY_SKILL_BONUS_PER_POINT;
int neededSoldiers = (int) Math.ceil(maxEnemyPower / myMult);

// 3. Gold needed = recruit cost + expected upkeep
int recruitCost = neededSoldiers * GameParameters.NOBLE_UPKEEP_COST_PER_SOLDIER;
int upkeepCost  = neededSoldiers * GameParameters.NOBLE_UPKEEP_COST_PER_SOLDIER * GameParameters.WAR_CHEST_UPKEEP_TURNS;
int baseGold = recruitCost + upkeepCost;

// 4. Personality multiplier (75% dominant + 25% secondary)
NobleCharacter ch = actor.getActiveCharacter();
Motivation dom = ch != null ? ch.getDominantMotivation() : Motivation.SECURITY;
Motivation sec = ch != null ? ch.getSecondaryMotivation() : Motivation.SECURITY;
double domPriority = motivationPriority(dom);
double secPriority = motivationPriority(sec);
double priority = 0.75 * domPriority + 0.25 * secPriority;

// 5. Fuzziness based on cunning
int cunning = ch != null ? ch.getCunning() : 0;
double fuzzRange = GameParameters.WAR_CHEST_FUZZ_BASE
+ (4 - cunning) * GameParameters.WAR_CHEST_FUZZ_PER_MISSING;
double fuzz = 1.0 + (RNG.nextDouble() * 2 - 1) * fuzzRange;

int target = (int) (baseGold * priority * fuzz);
return Math.max(0, target);
}

private static double motivationPriority(Motivation m) {
return switch (m) {
case EXPANSION -> GameParameters.WAR_CHEST_PRIORITY_EXPANSION;
case SECURITY  -> GameParameters.WAR_CHEST_PRIORITY_SECURITY;
case WEALTH    -> GameParameters.WAR_CHEST_PRIORITY_WEALTH;
case PRESTIGE  -> GameParameters.WAR_CHEST_PRIORITY_PRESTIGE;
};
}

/** Returns true if the house should skip the war‑chest check (reckless at war). */
private static boolean isRecklessAtWar(NobleHouse actor) {
NobleCharacter ch = actor.getActiveCharacter();
if (ch == null) return false;
return ch.getDominantMotivation() == Motivation.EXPANSION
&& ch.getMilitary() >= 2
&& ch.getCunning() < 2;
}

private static boolean shouldGift(NobleHouse actor, Motivation motivation,
List<NobleHouse> allHouses,
RelationshipManager relationships) {
double weight = switch (motivation) {
case SECURITY  -> GameParameters.GIFT_WEIGHT_SECURITY;
case WEALTH    -> actor.getGold() > GameParameters.GIFT_WEALTH_GOLD_THRESHOLD
? GameParameters.GIFT_WEIGHT_WEALTH : 0.0;
case PRESTIGE  -> GameParameters.GIFT_WEIGHT_PRESTIGE;
case EXPANSION -> GameParameters.GIFT_WEIGHT_EXPANSION;
};
if (RNG.nextDouble() > weight) return false;
for (NobleHouse other : allHouses) {
if (other == actor || other.isEliminated()) continue;
Relationship rel = relationships.get(actor.getId(), other.getId());
if ((rel == Relationship.HOSTILE || rel == Relationship.NEUTRAL)
&& other.getTotalArmySize() > actor.getTotalArmySize()) return true;
}
return false;
}
}














