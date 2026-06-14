package City.main.combat;

import City.main.parameters.CombatParams;
 

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
* Placeholder combat resolver.
* Supports both single attacker and combined coalition force.
*/
public class CombatResolver {

private static final Random RNG = new Random();

/**
* Single attacker vs defender.
*/

public static CombatResult resolve(ArmyForce attacker, ArmyForce defender) {
List<String> log = new ArrayList<>();

double defenseMultiplier = 1.0 - (defender.getDefense() / 100.0)
* CombatParams.COMBAT_DEFENSE_REDUCTION;

int attackerEffective  = attacker.getEffectivePower();
int defenderEffective  = (int)(defender.getEffectivePower() * defenseMultiplier);

int defenderLosses = resolveCasualties(attackerEffective, defender.getEffectivePower());
int attackerLosses = resolveCasualties(defenderEffective, attacker.getEffectivePower());

log.add(attacker.getHouseId() + " attacks " + defender.getHouseId()
+ " (" + attacker.getEffectivePower() + " vs " + defender.getEffectivePower() + ")");

attacker.applyLosses(attackerLosses);
defender.applyLosses(defenderLosses);

log.add("Attacker losses: " + attackerLosses
+ "  Defender losses: " + defenderLosses);

return resolveWinner(attacker.getHouseId(), defender.getHouseId(),
attacker, defender, attackerLosses, defenderLosses, log);
}

private static CombatResult resolveWinner(String attackerId, String defenderId,
ArmyForce attacker, ArmyForce defender,
int attackerLosses, int defenderLosses,
List<String> log) {
String winnerId;
String loserId;

if (attacker.getRawSize() > 0 && defender.getRawSize() == 0) {
winnerId = attackerId;
loserId  = defenderId;
log.add(winnerId + " wins the engagement.");
} else if (defender.getRawSize() > 0 && attacker.getRawSize() == 0) {
winnerId = defenderId;
loserId  = attackerId;
log.add(winnerId + " repels the attack.");
} else if (attacker.getRawSize() > defender.getRawSize()) {
winnerId = attackerId;
loserId  = defenderId;
log.add(winnerId + " wins the engagement.");
} else if (defender.getRawSize() > attacker.getRawSize()) {
winnerId = defenderId;
loserId  = attackerId;
log.add(winnerId + " repels the attack.");
} else {
winnerId = null;
loserId  = null;
log.add("The engagement ends in a draw.");
}

return new CombatResult(winnerId, loserId,
attackerLosses, defenderLosses, log);
}

/**
* Coalition attack: multiple attackers combined vs one defender.
* Returns a single CombatResult. Winner is "coalition" or defender id.
* Losses are distributed proportionally across coalition members.
*/

public static CombatResult resolveCoalition(List<ArmyForce> attackers,
ArmyForce defender,
String coordinatorId) {
List<String> log = new ArrayList<>();

int totalAttackerEffective = 0;
int totalAttackerRaw       = 0;
for (ArmyForce a : attackers) {
totalAttackerEffective += a.getEffectivePower();
totalAttackerRaw       += a.getRawSize();
}

double defenseMultiplier = 1.0 - (defender.getDefense() / 100.0)
* CombatParams.COMBAT_DEFENSE_REDUCTION;
int defenderEffective = (int)(defender.getEffectivePower() * defenseMultiplier);

int defenderLosses  = resolveCasualties(totalAttackerEffective, defender.getEffectivePower());
int totalAtkLosses  = resolveCasualties(defenderEffective, totalAttackerEffective);

StringBuilder members = new StringBuilder();
for (int i = 0; i < attackers.size(); i++) {
if (i > 0) members.append(", ");
members.append(attackers.get(i).getHouseId());
}
log.add("Coalition [" + members + "] attacks " + defender.getHouseId()
+ " (" + totalAttackerEffective + " vs " + defender.getEffectivePower() + ")");

// Distribute losses proportionally by raw size
for (ArmyForce a : attackers) {
double fraction = (double) a.getRawSize() / Math.max(1, totalAttackerRaw);
int losses = (int) Math.ceil(totalAtkLosses * fraction);
a.applyLosses(losses);
}
defender.applyLosses(defenderLosses);

log.add("Coalition losses: " + totalAtkLosses
+ "  Defender losses: " + defenderLosses);

boolean coalitionWins = (totalAttackerRaw - totalAtkLosses) > (defender.getRawSize() - defenderLosses);

if (coalitionWins) {
log.add("Coalition wins the engagement.");
return new CombatResult(coordinatorId, defender.getHouseId(),
totalAtkLosses, defenderLosses, log);
} else {
log.add(defender.getHouseId() + " repels the coalition.");
return new CombatResult(defender.getHouseId(), coordinatorId,
totalAtkLosses, defenderLosses, log);
}
}



private static int resolveCasualties(int power, int targetSize) {
if (power <= 0 || targetSize <= 0) return 0;
double ratio    = Math.min(2.0, (double) power / Math.max(1, targetSize));
double baseRate = CombatParams.COMBAT_BASE_CASUALTY_RATE;
double variance = (RNG.nextDouble() - 0.5) * CombatParams.COMBAT_CASUALTY_VARIANCE;
double rate     = Math.max(0.05, Math.min(0.6, baseRate * ratio + variance));
return (int) Math.ceil(targetSize * rate);
}

/**
* Resolve a battle with multiple attacker armies and multiple defender armies.
* Returns a CombatResult where winnerId is the side that won.
*/

public static CombatResult resolveMultiSideBattle(
List<ArmyForce> attackers,
List<ArmyForce> defenders,
String attackerCoordinatorId,
String defenderHouseId,
int defenderFortification
) {
List<String> log = new ArrayList<>();

int totalAttackerEffective = 0;
int totalAttackerRaw       = 0;
for (ArmyForce a : attackers) {
totalAttackerEffective += a.getEffectivePower();
totalAttackerRaw       += a.getRawSize();
}

double defenseMultiplier = 1.0 - (defenderFortification / 100.0)
* CombatParams.COMBAT_DEFENSE_REDUCTION;
int totalDefenderEffective = 0;
int totalDefenderRaw       = 0;
for (ArmyForce d : defenders) {
totalDefenderEffective += (int)(d.getEffectivePower() * defenseMultiplier);
totalDefenderRaw       += d.getRawSize();
}

int defenderLossesTotal = resolveCasualties(totalAttackerEffective, totalDefenderEffective);
int attackerLossesTotal = resolveCasualties(totalDefenderEffective, totalAttackerEffective);

log.add("Multi‑side battle: Attackers power " + totalAttackerEffective
+ " vs Defenders power " + totalDefenderEffective);

// Distribute losses by raw size
for (ArmyForce a : attackers) {
double fraction = (double) a.getRawSize() / Math.max(1, totalAttackerRaw);
int losses = (int) Math.ceil(attackerLossesTotal * fraction);
a.applyLosses(losses);
}
for (ArmyForce d : defenders) {
double fraction = (double) d.getRawSize() / Math.max(1, totalDefenderRaw);
int losses = (int) Math.ceil(defenderLossesTotal * fraction);
d.applyLosses(losses);
}

boolean attackersWin = (totalAttackerRaw - attackerLossesTotal)
> (totalDefenderRaw - defenderLossesTotal);
String winnerId = attackersWin ? attackerCoordinatorId : defenderHouseId;
String loserId  = attackersWin ? defenderHouseId : attackerCoordinatorId;
log.add(attackersWin ? "Attacker coalition wins." : "Defender coalition wins.");

return new CombatResult(winnerId, loserId,
attackerLossesTotal, defenderLossesTotal, log);
}

}
