package main.nobles.ai;

import main.nobles.NobleArmy;
import main.nobles.NobleArmyManager;
import main.nobles.NobleCharacter;
import main.nobles.NobleHouse;
import main.nobles.RelationshipManager;
import main.nobles.Relationship;
import main.parameters.GameParameters;

import java.util.List;

/** Estimates combat and economic power for noble houses. */
public final class NobleAIPower {

    private NobleAIPower() {}

    public static int exactPotentialFieldArmy(NobleHouse house, NobleArmyManager armyManager) {
        int manpower = house.getNobleManpower();
        int gold = house.getGold();
        int recruitable = Math.min(manpower, gold / GameParameters.NOBLE_RECRUIT_COST_PER_SOLDIER);
        int existingArmies = armyManager.getArmiesForHouse(house.getId()).stream()
                .mapToInt(NobleArmy::getSize).sum();
        return recruitable + existingArmies;
    }

    public static int estimatedPower(NobleHouse observer, NobleHouse target,
                                     NobleArmyManager armyManager) {
        int exact = exactPotentialFieldArmy(target, armyManager);
        int cunning = observer.getActiveCharacter() != null
                ? observer.getActiveCharacter().getCunning() : 0;
        return roughEstimate(exact, cunning);
    }

    static int roughEstimate(int exactValue, int cunning) {
        double fuzzRange = (4 - cunning) * 0.13 + 0.07;
        double multiplier = 1.0 + (NobleAIUtils.RNG.nextDouble() * 2 - 1) * fuzzRange;
        return Math.max(0, (int) (exactValue * multiplier));
    }

    public static int estimateAttackPower(NobleHouse house, NobleArmyManager armyManager) {
        int milSkill = house.getActiveCharacter() != null
                ? house.getActiveCharacter().getMilitary() : 0;
        double mult = 1.0 + milSkill * GameParameters.MILITARY_SKILL_BONUS_PER_POINT;
        for (NobleArmy a : armyManager.getArmiesForHouse(house.getId())) {
            if (!a.hasPendingOrder() && a.getSize() > 0) {
                return (int) (a.getSize() * mult);
            }
        }
        int size = maxRecruitableSize(house);
        return (int) (size * mult);
    }

    public static int estimateMemberPower(NobleHouse member, NobleArmyManager armyManager) {
        return estimateAttackPower(member, armyManager);
    }

    public static int estimateDefenderCombatPower(NobleHouse attacker, NobleHouse defender,
                                                   String zoneId,
                                                   List<NobleHouse> allHouses,
                                                   NobleArmyManager armyManager,
                                                   RelationshipManager relationships) {
        int garrison = defender.getGarrisonFor(zoneId);
        int fort     = defender.getFortificationFor(zoneId);
        int mil      = defender.getActiveCharacter() != null
                ? defender.getActiveCharacter().getMilitary() : 0;
        double mult        = 1.0 + mil * GameParameters.MILITARY_SKILL_BONUS_PER_POINT;
        double defReduction = 1.0 - (fort / 100.0) * GameParameters.COMBAT_DEFENSE_REDUCTION;
        int baseDefPower   = (int) (garrison * mult * defReduction);

        int fieldArmyEstimate = estimatedPower(attacker, defender, armyManager);

        int allyHelp = 0;
        List<String> allies = relationships.getAll(
                defender.getId(), Relationship.ALLIED, NobleAIUtils.allHouseIds(allHouses));
        for (String allyId : allies) {
            NobleHouse ally = NobleAIUtils.findById(allyId, allHouses);
            if (ally != null && !ally.isEliminated() && ally != attacker) {
                Relationship allyWithAttacker = relationships.get(ally.getId(), attacker.getId());
                if (allyWithAttacker != Relationship.ALLIED
                        && allyWithAttacker != Relationship.FRIENDLY) {
                    allyHelp += estimatedPower(attacker, ally, armyManager);
                }
            }
        }
        return baseDefPower + fieldArmyEstimate + allyHelp;
    }

    /** Maximum soldiers this house can recruit right now. */
    static int maxRecruitableSize(NobleHouse house) {
        int manpower = house.getNobleManpower();
        int gold     = house.getGold();
        int minSize  = GameParameters.NOBLE_ARMY_MIN_RECRUIT_SIZE;
        if (manpower < minSize || gold < GameParameters.NOBLE_ARMY_RECRUIT_GOLD_THRESHOLD) return 0;
        int maxByManpower = manpower;
        int maxByGold     = gold / GameParameters.NOBLE_UPKEEP_COST_PER_SOLDIER;
        return Math.max(minSize, Math.min(maxByManpower, maxByGold));
    }
}