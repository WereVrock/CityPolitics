package City.main.nobles;

import City.main.army.PlayerBattleInterventionProcessor;
import City.main.combat.ArmyForce;
import City.main.combat.CombatResolver;
import City.main.combat.CombatResult;
import City.main.map.ZoneManager;
import City.main.map.ZoneState;

import java.util.*;
import City.main.map.Zone;
import City.main.parameters.DiplomacyParams;
import City.main.parameters.MapZoneParams;
import City.main.parameters.NobleAIParams;
import City.main.parameters.NobleHouseParams;

public class NobleArmyManager {

    private final List<NobleArmy>              armies   = new ArrayList<>();
    private final Map<String, List<NobleArmy>> byHouse  = new LinkedHashMap<>();
    private final Map<String, List<NobleArmy>> byZone   = new LinkedHashMap<>();
    private       int                          nextId   = 1;

    private final ZoneManager          zoneManager;
    private final RelationshipManager  relationships;
    private       CoalitionManager     coalitionManager;
    private       PlayerBattleInterventionProcessor interventionProcessor;
    private       City.main.army.ArmyManager        playerArmyManager;
    private       City.main.nobles.PlayerPrestige   playerPrestige;
    private       City.main.nobles.ProtectionManager protectionManager;


    public NobleArmyManager(ZoneManager zoneManager, RelationshipManager relationships) {
        this.zoneManager   = zoneManager;
        this.relationships = relationships;
    }

    public void setCoalitionManager(CoalitionManager coalitionManager) {
        this.coalitionManager = coalitionManager;
    }

    public void setInterventionProcessor(
            PlayerBattleInterventionProcessor proc,
            City.main.army.ArmyManager armyMgr) {
        this.interventionProcessor = proc;
        this.playerArmyManager     = armyMgr;
    }

    public void setPlayerPrestige(City.main.nobles.PlayerPrestige pp) {
        this.playerPrestige = pp;
    }

    public void setProtectionManager(City.main.nobles.ProtectionManager pm) {
        this.protectionManager = pm;
    }

    // ─── Recruitment ─────────────────────────────────────────────────────────

    public NobleArmy recruit(NobleHouse house, int size) {
        if (size <= 0) return null;
        String zoneId = house.getCapitalZoneId();
        if (zoneId == null) {
            City.debug.Debug.log("noble", "recruit", house.getName() + " cannot recruit (no capital zone)");
            return null;
        }

        int manpowerCost = size;
        int goldCost     = size * NobleHouseParams.NOBLE_UPKEEP_COST_PER_SOLDIER;
        if (house.getNobleManpower() < manpowerCost) {
            City.debug.Debug.log("noble", "recruit", house.getName() + " insufficient manpower");
            return null;
        }
        if (house.getGold() < goldCost) {
            City.debug.Debug.log("noble", "recruit", house.getName() + " insufficient gold");
            return null;
        }

        house.spendNobleManpower(manpowerCost);
        house.addGold(-goldCost);

        String    id   = "noble_army_" + (nextId++);
        NobleArmy army = new NobleArmy(id, house.getId(), size, zoneId);
        army.setSkipNextUpkeep(true);
        add(army);
        City.debug.Debug.log("noble", "recruit", house.getName() + " recruited " + size + " at " + zoneId);
        return army;
    }

    public boolean reinforceArmy(NobleHouse house, NobleArmy army, int additionalSize) {
        if (additionalSize <= 0) return false;
        int manpowerCost = additionalSize;
        int goldCost     = additionalSize * NobleHouseParams.NOBLE_UPKEEP_COST_PER_SOLDIER;
        if (house.getNobleManpower() < manpowerCost || house.getGold() < goldCost) return false;
        house.spendNobleManpower(manpowerCost);
        house.addGold(-goldCost);
        army.setSize(army.getSize() + additionalSize);
        return true;
    }

    // ─── Upkeep ──────────────────────────────────────────────────────────────

    public void payUpkeep(NobleHouse house) {
        for (NobleArmy army : new ArrayList<>(getArmiesForHouse(house.getId()))) {
            if (army.getSkipNextUpkeep()) { army.setSkipNextUpkeep(false); continue; }
            boolean isDefending = isArmyDefending(army, house);
            int upkeepPerSoldier = NobleHouseParams.NOBLE_UPKEEP_COST_PER_SOLDIER;
            if (isDefending) {
                upkeepPerSoldier = (int)(upkeepPerSoldier * (1.0 - NobleHouseParams.NOBLE_UPKEEP_DEFENSE_DISCOUNT));
                if (upkeepPerSoldier < 1) upkeepPerSoldier = 1;
            }
            int cost = army.getSize() * upkeepPerSoldier;
            if (house.getGold() >= cost) {
                house.addGold(-cost);
            } else {
                int disbanded = army.disband(army.getSize());
                house.addNobleManpower(disbanded);
                remove(army);
            }
        }
    }

    private boolean isArmyDefending(NobleArmy army, NobleHouse house) {
        return house.getZoneIds().contains(army.getZoneId())
                && army.getPendingOrder() == NobleArmy.OrderType.NONE;
    }

    // ─── Disband ─────────────────────────────────────────────────────────────

    public void disbandPartial(NobleHouse house, NobleArmy army, int count) {
        int actual = army.disband(count);
        house.addNobleManpower(actual);
        if (!army.isAlive()) remove(army);
    }

    // ─── Order resolution ────────────────────────────────────────────────────

    public void tickOrders() {
        for (NobleArmy army : new ArrayList<>(armies)) army.tickOrder();
    }

    public List<String> resolveOrdersForHouse(String houseId, List<NobleHouse> allHouses, ClaimManager claimManager) {
        List<String> log = new ArrayList<>();
        for (NobleArmy army : new ArrayList<>(armies)) {
            if (!army.getHouseId().equals(houseId)) continue;
            if (!army.isOrderReadyToResolve()) continue;
            switch (army.getPendingOrder()) {
                case ATTACK      -> log.addAll(resolveAttack(army, allHouses, claimManager, army.getCoalitionMemberIds()));
                case RAID        -> log.addAll(resolveRaid(army, allHouses));
                case JOIN_BATTLE -> {}
                case NONE        -> {}
            }
            if (army.getPendingOrder() != NobleArmy.OrderType.JOIN_BATTLE) army.clearOrder();
        }
        removeDeadArmies(allHouses);
        return log;
    }

    public void disbandIdleArmies(NobleHouse house) {
        for (NobleArmy army : new ArrayList<>(getArmiesForHouse(house.getId()))) {
            if (army.getPendingOrder() == NobleArmy.OrderType.NONE && army.isAlive()) {
                int returned = army.disband(army.getSize());
                house.addNobleManpower(returned);
                remove(army);
            }
        }
    }

    public NobleArmy splitArmy(NobleArmy army, int count) {
        if (count <= 0 || count >= army.getSize()) return null;
        army.setSize(army.getSize() - count);
        String id = "noble_army_" + (nextId++);
        NobleArmy split = new NobleArmy(id, army.getHouseId(), count, army.getZoneId());
        add(split);
        return split;
    }

    public void addRestoredArmy(NobleArmy army) {
        armies.add(army);
        byHouse.computeIfAbsent(army.getHouseId(), k -> new ArrayList<>()).add(army);
        byZone.computeIfAbsent(army.getZoneId(),   k -> new ArrayList<>()).add(army);
    }

    public List<String> resolveOrders(List<NobleHouse> allHouses, ClaimManager claimManager) {
        List<String> log = new ArrayList<>();
        for (NobleArmy army : new ArrayList<>(armies)) {
            if (!army.isOrderReadyToResolve()) continue;
            switch (army.getPendingOrder()) {
                case ATTACK -> log.addAll(resolveAttack(army, allHouses, claimManager, army.getCoalitionMemberIds()));
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
                                       ClaimManager claimManager, Set<String> coalitionMemberIds) {
        List<String> log = new ArrayList<>();
        String zoneId = attArmy.getPendingTargetZoneId();
        if (zoneId == null) return log;

        NobleHouse attacker = findHouse(attArmy.getHouseId(), allHouses);
        NobleHouse defender = findZoneOwner(zoneId, allHouses);
        if (attacker == null || defender == null || attacker == defender) return log;

        boolean isCoalition = coalitionMemberIds != null && !coalitionMemberIds.isEmpty();
        if (isCoalition) {
            log.add("=== Coalition attack on " + zoneId + " held by " + defender.getName() + " ===");
        } else {
            log.add(attacker.getName() + " army attacks " + zoneId + " held by " + defender.getName() + ".");
        }

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
                log.add(house.getName() + " joins the attack on " + zoneId + ".");
            }
        }

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
                log.add(house.getName() + " joins the defense of " + zoneId + ".");
            }
        }

        List<ArmyForce> attackerForces = new ArrayList<>();
        for (NobleArmy a : attackerArmies) {
            NobleHouse h = findHouse(a.getHouseId(), allHouses);
            attackerForces.add(new ArmyForce(a.getHouseId(), a.getSize(), 0, militarySkill(h)));
        }
        List<ArmyForce> defenderForces = new ArrayList<>();
        defenderForces.add(new ArmyForce(defender.getId(), garrisonSize, defFort, militarySkill(defender)));
        for (NobleArmy a : defenderArmies) {
            NobleHouse h = findHouse(a.getHouseId(), allHouses);
            defenderForces.add(new ArmyForce(a.getHouseId(), a.getSize(), defFort, militarySkill(h)));
        }

        // Player intervention
        if (interventionProcessor != null && playerArmyManager != null) {
            int totalAtkSz = attackerForces.stream().mapToInt(ArmyForce::getRawSize).sum();
            java.util.List<String> atkAllies = new java.util.ArrayList<>();
            java.util.List<String> defAllies = new java.util.ArrayList<>();
            for (ArmyForce f : attackerForces) {
                if (!f.getHouseId().equals(attacker.getId())) {
                    NobleHouse h = findHouse(f.getHouseId(), allHouses);
                    if (h != null) atkAllies.add(h.getName());
                }
            }
            for (ArmyForce f : defenderForces) {
                if (!f.getHouseId().equals(defender.getId())) {
                    NobleHouse h = findHouse(f.getHouseId(), allHouses);
                    if (h != null) defAllies.add(h.getName());
                }
            }
            boolean defProtected = protectionManager != null
                    && protectionManager.isUnderProtection(defender.getId());

            PlayerBattleInterventionProcessor.PlayerChoice choice =
                    interventionProcessor.checkInterventionDetailed(
                            attacker, defender, zoneId, totalAtkSz, playerArmyManager,
                            atkAllies, defAllies, defProtected);

            switch (choice) {
                case STOP_FIGHT -> {
                    log.add("Player intervenes and stops the battle at " + zoneId + ".");
                    applyInterventionOpinions(choice, attacker, defender, allHouses, log);
                    attArmy.clearOrder();
                    return log;
                }
                case JOIN_ATTACKER -> {
                    log.add("Player army joins the attack on " + defender.getName() + ".");
                    addPlayerForcesToAttack(attackerForces, zoneId, log);
                    applyInterventionOpinions(choice, attacker, defender, allHouses, log);
                boolean justified = isJoinAttackerJustified(attacker, this.zoneManager);
                if (!justified && playerPrestige != null) {
                    playerPrestige.addTrust(City.main.parameters.StartingParams.PLAYER_TRUST_JOIN_UNJUST);
                    applyBystanderOpinionPenalty(defender, attacker, allHouses, log);
                }
            }
            case JOIN_DEFENDER -> {
                log.add("Player army joins the defense of " + defender.getName() + ".");
                addPlayerForcesToDefense(defenderForces, zoneId, defFort, militarySkill(defender), log);
                applyInterventionOpinions(choice, attacker, defender, allHouses, log);
                boolean justified = defProtected
                        || isJoinDefenderSideJustifiedByUnlawful(zoneId, this.zoneManager);
                if (!justified && playerPrestige != null) {
                    playerPrestige.addTrust(City.main.parameters.StartingParams.PLAYER_TRUST_JOIN_UNJUST);
                    applyBystanderOpinionPenalty(attacker, defender, allHouses, log);
                }
                }
                case IGNORE -> {}
            }
        }

        CombatResult result = CombatResolver.resolveMultiSideBattle(
                attackerForces, defenderForces,
                attacker.getId(), defender.getId(), defFort);
        log.addAll(result.getLog());

        for (int i = 0; i < attackerArmies.size(); i++) {
            attackerArmies.get(i).setSize(attackerForces.get(i).getRawSize());
        }
        int newGarrisonRaw = defenderForces.get(0).getRawSize();
        int garrisonDelta = garrisonSize - newGarrisonRaw;
        if (garrisonDelta > 0) defender.damageGarrison(zoneId, garrisonDelta);
        for (int i = 1; i < defenderForces.size() && i - 1 < defenderArmies.size(); i++) {
            defenderArmies.get(i - 1).setSize(defenderForces.get(i).getRawSize());
        }

        boolean attackersWin = result.getWinnerId() != null
                && result.getWinnerId().equals(attacker.getId());

        if (attackersWin) {
            ZoneState state = zoneManager.getState(zoneId);
            if (state != null) {
                state.markConquered();
                state.clearUnlawfullyAcquired(); // ownership changed — mark clears (rule A)
            }
            defender.resetGarrison(zoneId);

            if (isCoalition && coalitionManager != null) {
                coalitionManager.awardConqueredZone(zoneId, attacker,
                        attackerParticipants, allHouses, defFort, log);
            } else {
                defender.removeZone(zoneId);
                attacker.conquerZone(zoneId, defFort);
                claimManager.addClaim(defender.getId(), zoneId);
                attacker.resetGarrison(zoneId);
                log.add(attacker.getName() + " captures " + zoneId + " from " + defender.getName() + ".");
                if (defender.isEliminated()) log.add(defender.getName() + " has been eliminated.");
            }
            relationships.set(attacker.getId(), defender.getId(), Relationship.RIVAL);
            for (NobleHouse p : attackerParticipants) p.clearThreats();
            if (coalitionManager != null) coalitionManager.onHouseLostZone(defender.getId());
        } else {
            log.add(defender.getName() + " repels the attack on " + zoneId + ".");
            relationships.set(attacker.getId(), defender.getId(), Relationship.RIVAL);
            if (isCoalition && coalitionManager != null)
                coalitionManager.onCoalitionAttackFailed(attacker.getId(), defender.getId(), zoneId);
        }

        for (NobleArmy a : attackerArmies) {
            if (a == attArmy) continue;
            String prev = a.getPreviousZoneId();
            NobleHouse owner = findHouse(a.getHouseId(), allHouses);
            if (prev != null && findZoneOwner(prev, allHouses) == owner) moveArmy(a, prev);
            else if (owner != null && owner.getCapitalZoneId() != null) moveArmy(a, owner.getCapitalZoneId());
            a.clearOrder();
        }
        for (NobleArmy a : defenderArmies) {
            String prev = a.getPreviousZoneId();
            NobleHouse owner = findHouse(a.getHouseId(), allHouses);
            if (prev != null && findZoneOwner(prev, allHouses) == owner) moveArmy(a, prev);
            else if (owner != null && owner.getCapitalZoneId() != null) moveArmy(a, owner.getCapitalZoneId());
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

    /**
     * Returns true if joining against the given attacker is justified.
     * Justified if attacker holds at least 1 unlawfully acquired zone (rule B).
     */
    /**
     * Rule B: joining against the attacker is justified if they hold ≥1 unlawfully acquired zone.
     */
    public boolean isJoinAttackerJustified(NobleHouse attacker, ZoneManager zm) {
        for (String zoneId : attacker.getZoneIds()) {
            ZoneState state = zm.getState(zoneId);
            if (state != null && state.isUnlawfullyAcquired()) return true;
        }
        return false;
    }

    /**
     * Rule C: joining the attacking side is justified if the contested zone is marked unlawful.
     */
    public boolean isJoinDefenderSideJustifiedByUnlawful(String zoneId, ZoneManager zm) {
        ZoneState state = zm.getState(zoneId);
        return state != null && state.isUnlawfullyAcquired();
    }

    private void addPlayerForcesToAttack(List<ArmyForce> attackerForces, String zoneId, List<String> log) {
        if (playerArmyManager == null) return;
        for (City.main.army.Army a : playerArmyManager.getDeployedArmies()) {
            if (zoneId.equals(a.getZoneId()) && a.isAlive())
                attackerForces.add(new ArmyForce("player_" + a.getId(), a.getSize(), 0, a.getCommandingSkill()));
        }
    }

    private void addPlayerForcesToDefense(List<ArmyForce> defenderForces, String zoneId,
                                           int defFort, int defMilitary, List<String> log) {
        if (playerArmyManager == null) return;
        for (City.main.army.Army a : playerArmyManager.getDeployedArmies()) {
            if (zoneId.equals(a.getZoneId()) && a.isAlive())
                defenderForces.add(new ArmyForce("player_" + a.getId(), a.getSize(), defFort, a.getCommandingSkill()));
        }
    }

    private void applyInterventionOpinions(PlayerBattleInterventionProcessor.PlayerChoice choice,
                                            NobleHouse attacker, NobleHouse defender,
                                            List<NobleHouse> allHouses, List<String> log) {
        int joinBonus   = City.main.parameters.NobleCouncilParams.INTERVENTION_JOIN_ATTACKER_SELF_OPINION;
        int joinPenalty = City.main.parameters.NobleCouncilParams.INTERVENTION_JOIN_ATTACKER_VICTIM_OPINION;
        int stopPenalty = City.main.parameters.NobleCouncilParams.INTERVENTION_STOP_ATTACKER_OPINION;
        int stopBonus   = City.main.parameters.NobleCouncilParams.INTERVENTION_STOP_DEFENDER_OPINION;
        switch (choice) {
            case JOIN_ATTACKER -> { attacker.adjustPlayerOpinion(joinBonus); defender.adjustPlayerOpinion(joinPenalty); }
            case JOIN_DEFENDER -> { defender.adjustPlayerOpinion(joinBonus); attacker.adjustPlayerOpinion(joinPenalty); }
            case STOP_FIGHT    -> { attacker.adjustPlayerOpinion(stopPenalty); defender.adjustPlayerOpinion(stopBonus / 2); }
            default -> {}
        }
    }

    private void applyBystanderOpinionPenalty(NobleHouse sideJoined, NobleHouse otherSide,
                                               List<NobleHouse> allHouses, List<String> log) {
        for (NobleHouse h : allHouses) {
            if (h == sideJoined || h == otherSide || h.isEliminated()) continue;
            Relationship relToOther = relationships.get(h.getId(), otherSide.getId());
            if (relToOther != Relationship.RIVAL && relToOther != Relationship.HOSTILE)
                h.adjustPlayerOpinion(City.main.parameters.StartingParams.PLAYER_TRUST_BYSTANDER_OPINION);
        }
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
            log.add(attacker.getName() + " finds " + zoneId + " already raided. Raid cancelled.");
            String capital = attacker.getCapitalZoneId();
            if (capital != null) moveArmy(attArmy, capital);
            return log;
        }

        List<NobleArmy> defArmies = getArmiesInZone(zoneId, defender.getId());
        if (!defArmies.isEmpty()) {
            double interceptChance = DiplomacyParams.RAID_INTERCEPT_BASE_CHANCE
                    + militarySkill(defender) * DiplomacyParams.RAID_INTERCEPT_MILITARY_BONUS;
            if (Math.random() < interceptChance) {
                log.add(defender.getName() + "'s army intercepts the raid on " + zoneId + "!");
                ArmyForce atk = new ArmyForce(attacker.getId(), attArmy.getSize(), 0, militarySkill(attacker));
                NobleArmy defArmy = defArmies.get(0);
                ArmyForce def = new ArmyForce(defender.getId(), defArmy.getSize(), 0, militarySkill(defender));
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
            }
        }

        Zone zone = zoneManager.getZone(zoneId);
        int zoneGold = zone != null ? zone.getGoldProduction() : MapZoneParams.ZONE_VILLAGE_GOLD;
        int maxByZone = (int)(zoneGold * DiplomacyParams.RAID_GOLD_ZONE_MULTIPLIER);
        int maxByArmy = (int)(attArmy.getSize() * DiplomacyParams.RAID_GOLD_PER_SOLDIER);
        int stolen = Math.min(Math.min(maxByZone, maxByArmy),
                (int)(defender.getGold() * NobleAIParams.AI_RAID_GOLD_FRACTION));
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

    public List<NobleArmy> getAllArmies() { return Collections.unmodifiableList(armies); }

    public List<NobleArmy> getArmiesForHouse(String houseId) {
        return Collections.unmodifiableList(byHouse.getOrDefault(houseId, Collections.emptyList()));
    }

    public List<NobleArmy> getArmiesInZone(String zoneId) {
        return Collections.unmodifiableList(byZone.getOrDefault(zoneId, Collections.emptyList()));
    }

    public List<NobleArmy> getArmiesInZone(String zoneId, String houseId) {
        List<NobleArmy> result = new ArrayList<>();
        for (NobleArmy a : byZone.getOrDefault(zoneId, Collections.emptyList()))
            if (a.getHouseId().equals(houseId)) result.add(a);
        return result;
    }

    public boolean hasPendingAttackOrder(String houseId, String zoneId) {
        for (NobleArmy a : getArmiesForHouse(houseId))
            if (a.getPendingOrder() == NobleArmy.OrderType.ATTACK && zoneId.equals(a.getPendingTargetZoneId())) return true;
        return false;
    }

    public int getTotalIdleArmySize(String houseId, String zoneId) {
        int total = 0;
        for (NobleArmy a : getArmiesInZone(zoneId, houseId))
            if (!a.hasPendingOrder()) total += a.getSize();
        return total;
    }

    public NobleArmy getFirstIdleArmyInZone(String houseId, String zoneId) {
        for (NobleArmy a : getArmiesInZone(zoneId, houseId))
            if (!a.hasPendingOrder() && a.isAlive()) return a;
        return null;
    }

    public void reset() {
        armies.clear();
        byHouse.clear();
        byZone.clear();
        nextId = 1;
    }

    public NobleArmy moveArmyAndGetResult(NobleArmy army, String zoneId) {
        moveArmy(army, zoneId);
        return getFirstIdleArmyInZone(army.getHouseId(), zoneId);
    }

    // ─── Internal ────────────────────────────────────────────────────────────

    private void add(NobleArmy army) {
        List<NobleArmy> zoneList = new ArrayList<>(byZone.getOrDefault(army.getZoneId(), Collections.emptyList()));
        for (NobleArmy existing : zoneList) {
            if (!existing.getHouseId().equals(army.getHouseId())) continue;
            if (canMergeOrders(existing.getPendingOrder(), army.getPendingOrder())) {
                existing.setSize(existing.getSize() + army.getSize());
                if (existing.getPendingOrder() == NobleArmy.OrderType.NONE
                        && army.getPendingOrder() != NobleArmy.OrderType.NONE) {
                    existing.issueOrder(army.getPendingOrder(), army.getPendingTargetZoneId());
                    if (army.isCoalitionAttack() && army.getCoalitionMemberIds() != null)
                        existing.issueCoalitionOrder(army.getPendingTargetZoneId(), new HashSet<>(army.getCoalitionMemberIds()));
                }
                return;
            }
        }
        armies.add(army);
        byHouse.computeIfAbsent(army.getHouseId(), k -> new ArrayList<>()).add(army);
        byZone.computeIfAbsent(army.getZoneId(),   k -> new ArrayList<>()).add(army);
    }

    private boolean canMergeOrders(NobleArmy.OrderType a, NobleArmy.OrderType b) {
        if (a == b) return true;
        if (a == NobleArmy.OrderType.ATTACK && b == NobleArmy.OrderType.JOIN_BATTLE) return true;
        if (a == NobleArmy.OrderType.JOIN_BATTLE && b == NobleArmy.OrderType.ATTACK) return true;
        if (a == NobleArmy.OrderType.NONE && b == NobleArmy.OrderType.NONE) return true;
        return false;
    }

    public void remove(NobleArmy army) {
        armies.remove(army);
        List<NobleArmy> h = byHouse.get(army.getHouseId());
        if (h != null) h.remove(army);
        List<NobleArmy> z = byZone.get(army.getZoneId());
        if (z != null) z.remove(army);
    }

    public void moveArmy(NobleArmy army, String newZoneId) {
        if (newZoneId == null || newZoneId.equals(army.getZoneId())) return;
        List<NobleArmy> oldList = byZone.get(army.getZoneId());
        if (oldList != null) oldList.remove(army);
        army.setZoneId(newZoneId);
        List<NobleArmy> destList = new ArrayList<>(byZone.getOrDefault(newZoneId, Collections.emptyList()));
        for (NobleArmy existing : destList) {
            if (existing != army && existing.getHouseId().equals(army.getHouseId())
                    && canMergeOrders(existing.getPendingOrder(), army.getPendingOrder())) {
                existing.setSize(existing.getSize() + army.getSize());
                if (existing.getPendingOrder() == NobleArmy.OrderType.NONE
                        && army.getPendingOrder() != NobleArmy.OrderType.NONE) {
                    existing.issueOrder(army.getPendingOrder(), army.getPendingTargetZoneId());
                    if (army.isCoalitionAttack() && army.getCoalitionMemberIds() != null)
                        existing.issueCoalitionOrder(army.getPendingTargetZoneId(), new HashSet<>(army.getCoalitionMemberIds()));
                }
                armies.remove(army);
                List<NobleArmy> hh = byHouse.get(army.getHouseId());
                if (hh != null) hh.remove(army);
                return;
            }
        }
        byZone.computeIfAbsent(newZoneId, k -> new ArrayList<>()).add(army);
    }

    private void removeDeadArmies(List<NobleHouse> allHouses) {
        for (NobleArmy army : new ArrayList<>(armies))
            if (!army.isAlive()) remove(army);
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
        if (house == null) return 0;
        NobleCharacter c = house.getActiveCharacter();
        return c != null ? c.getMilitary() : 0;
    }
}