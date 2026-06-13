package main.nobles;

import main.combat.ArmyForce;
import main.combat.CombatResolver;
import main.combat.CombatResult;
import main.map.ZoneManager;
import main.map.ZoneState;
import main.parameters.GameParameters;

import java.util.*;
import main.map.Zone;

public class NobleArmyManager {

    private final List<NobleArmy>              armies   = new ArrayList<>();
    private final Map<String, List<NobleArmy>> byHouse  = new LinkedHashMap<>();
    private final Map<String, List<NobleArmy>> byZone   = new LinkedHashMap<>();
    private       int                          nextId   = 1;

    private final ZoneManager         zoneManager;
    private final RelationshipManager relationships;
    private       CoalitionManager    coalitionManager;
    private       main.army.PlayerBattleInterventionProcessor interventionProcessor;
    private       main.army.ArmyManager playerArmyManager;

    public NobleArmyManager(ZoneManager zoneManager, RelationshipManager relationships) {
        this.zoneManager   = zoneManager;
        this.relationships = relationships;
    }

    public void setCoalitionManager(CoalitionManager coalitionManager) {
        this.coalitionManager = coalitionManager;
    }

    public void setInterventionProcessor(
            main.army.PlayerBattleInterventionProcessor proc,
            main.army.ArmyManager armyMgr) {
        this.interventionProcessor = proc;
        this.playerArmyManager     = armyMgr;
    }

    // ─── Recruitment ─────────────────────────────────────────────────────────

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
        army.setSkipNextUpkeep(true);
        add(army);
        debug.Debug.log("noble", "recruit", house.getName() + " recruited " + size + " soldiers at " + zoneId + " (cost " + goldCost + " gold, " + manpowerCost + " manpower)");
        return army;
    }

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

    public void payUpkeep(NobleHouse house) {
        List<NobleArmy> houseArmies = getArmiesForHouse(house.getId());
        for (NobleArmy army : new ArrayList<>(houseArmies)) {
            if (army.getSkipNextUpkeep()) {
                army.setSkipNextUpkeep(false);
                continue;
            }
            boolean isDefending = isArmyDefending(army, house);
            int upkeepPerSoldier = GameParameters.NOBLE_UPKEEP_COST_PER_SOLDIER;
            if (isDefending) {
                upkeepPerSoldier = (int)(upkeepPerSoldier * (1.0 - GameParameters.NOBLE_UPKEEP_DEFENSE_DISCOUNT));
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
        if (!house.getZoneIds().contains(army.getZoneId())) return false;
        return army.getPendingOrder() == NobleArmy.OrderType.NONE;
    }

    // ─── Disband ────────────────────────────────────────────────────────────

    public void disbandPartial(NobleHouse house, NobleArmy army, int count) {
        int actual = army.disband(count);
        house.addNobleManpower(actual);
        if (!army.isAlive()) remove(army);
    }

    // ─── Order resolution ───────────────────────────────────────────────────

    public void tickOrders() {
        for (NobleArmy army : new ArrayList<>(armies)) {
            army.tickOrder();
        }
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
            if (army.getPendingOrder() != NobleArmy.OrderType.JOIN_BATTLE) {
                army.clearOrder();
            }
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
                debug.Debug.log("noble", "disband", house.getName() + " disbanded idle army of " + returned + " soldiers (returned to manpower).");
            }
        }
    }

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
     * Directly inserts a pre-built army (used by SaveManager on load).
     * Bypasses recruitment cost and merge logic.
     */
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
            log.add("=== Coalition attack on " + zoneId + " held by " + defender.getName()
                    + " === Coordinator: " + attacker.getName() + " ===");
        } else {
            log.add(attacker.getName() + " army attacks " + zoneId
                    + " held by " + defender.getName() + ".");
        }

        // (Player intervention check happens after forces are built — see below)

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

        // ── Player intervention ────────────────────────────────────────────────
        if (interventionProcessor != null && playerArmyManager != null) {
            int totalAtkSz = attackerForces.stream().mapToInt(ArmyForce::getRawSize).sum();
            main.army.PlayerBattleInterventionProcessor.PlayerChoice choice =
                    interventionProcessor.checkIntervention(
                            attacker, defender, zoneId, totalAtkSz, playerArmyManager);
            switch (choice) {
                case STOP_FIGHT -> {
                    log.add("⚔ Player intervenes and stops the battle at " + zoneId + ".");
                    attArmy.clearOrder();
                    return log;
                }
                case JOIN_ATTACKER -> {
                    log.add("⚔ Player army joins the attack on " + defender.getName() + " at " + zoneId + ".");
                    addPlayerForcesToAttack(attackerForces, zoneId, null, log);
                }
                case JOIN_DEFENDER -> {
                    log.add("⚔ Player army joins the defense of " + defender.getName() + " at " + zoneId + ".");
                    addPlayerForcesToDefense(defenderForces, zoneId, defFort, defMilitary(defender), log);
                }
                case IGNORE -> {}
            }
        }

        CombatResult result = CombatResolver.resolveMultiSideBattle(
                attackerForces, defenderForces,
                attacker.getId(), defender.getId(),
                defFort);
        log.addAll(result.getLog());

        for (int i = 0; i < attackerArmies.size(); i++) {
            attackerArmies.get(i).setSize(attackerForces.get(i).getRawSize());
        }
        int newGarrisonRaw = defenderForces.get(0).getRawSize();
        int garrisonDelta = garrisonSize - newGarrisonRaw;
        if (garrisonDelta > 0) {
            defender.damageGarrison(zoneId, garrisonDelta);
        }
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
                defender.removeZone(zoneId);
                attacker.conquerZone(zoneId, defFort);
                claimManager.addClaim(defender.getId(), zoneId);
                attacker.resetGarrison(zoneId);
                ZoneState st = zoneManager.getState(zoneId);
                if (st != null) st.setRebellionPower(st.getRebellionPower() / 2);
                log.add(attacker.getName() + " captures " + zoneId
                        + " from " + defender.getName() + ".");
                if (defender.isEliminated())
                    log.add(defender.getName() + " has been eliminated.");
            }
            relationships.set(attacker.getId(), defender.getId(), Relationship.RIVAL);
            for (NobleHouse p : attackerParticipants) p.clearThreats();

            if (coalitionManager != null) {
                coalitionManager.onHouseLostZone(defender.getId());
            }
        } else {
            log.add(defender.getName() + " repels the attack on " + zoneId + ".");
            relationships.set(attacker.getId(), defender.getId(), Relationship.RIVAL);

            if (isCoalition && coalitionManager != null) {
                coalitionManager.onCoalitionAttackFailed(attacker.getId(), defender.getId(), zoneId);
            }
        }

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

private void addPlayerForcesToAttack(List<ArmyForce> attackerForces,
                                      String zoneId,
                                      List<NobleHouse> allHouses,
                                      List<String> log) {
    if (playerArmyManager == null) return;
    for (main.army.Army a : playerArmyManager.getDeployedArmies()) {
        if (zoneId.equals(a.getZoneId()) && a.isAlive() && a.getSize() > 0) {
            attackerForces.add(new ArmyForce("player_" + a.getId(),
                    a.getSize(), 0, a.getCommandingSkill()));
        }
    }
}

private void addPlayerForcesToDefense(List<ArmyForce> defenderForces,
                                       String zoneId,
                                       int defFort, int defMilitary,
                                       List<String> log) {
    if (playerArmyManager == null) return;
    for (main.army.Army a : playerArmyManager.getDeployedArmies()) {
        if (zoneId.equals(a.getZoneId()) && a.isAlive() && a.getSize() > 0) {
            defenderForces.add(new ArmyForce("player_" + a.getId(),
                    a.getSize(), defFort, a.getCommandingSkill()));
        }
    }
}

private int defMilitary(NobleHouse house) {
    NobleCharacter c = house.getActiveCharacter();
    return c != null ? c.getMilitary() : 0;
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
        int stolen    = Math.min(maxSteal, (int)(defender.getGold() * GameParameters.AI_RAID_GOLD_FRACTION));
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
        return Collections.unmodifiableList(byHouse.getOrDefault(houseId, Collections.emptyList()));
    }

    public List<NobleArmy> getArmiesInZone(String zoneId) {
        return Collections.unmodifiableList(byZone.getOrDefault(zoneId, Collections.emptyList()));
    }

    public boolean hasPendingAttackOrder(String houseId, String zoneId) {
        for (NobleArmy a : getArmiesForHouse(houseId)) {
            if (a.getPendingOrder() == NobleArmy.OrderType.ATTACK && zoneId.equals(a.getPendingTargetZoneId())) {
                return true;
            }
        }
        return false;
    }

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

    public NobleArmy getFirstIdleArmyInZone(String houseId, String zoneId) {
        for (NobleArmy a : getArmiesInZone(zoneId, houseId)) {
            if (!a.hasPendingOrder() && a.isAlive()) return a;
        }
        return null;
    }

    public void reset() {
        armies.clear();
        byHouse.clear();
        byZone.clear();
        nextId = 1;
    }

    /**
     * Moves an army to a zone and immediately resolves any same-house merge.
     * Returns the surviving army at that zone (may be a different object after merge).
     */
    public NobleArmy moveArmyAndGetResult(NobleArmy army, String zoneId) {
        moveArmy(army, zoneId);
        return getFirstIdleArmyInZone(army.getHouseId(), zoneId);
    }

    // ─── Internal ───────────────────────────────────────────────────────────

    private void add(NobleArmy army) {
        List<NobleArmy> zoneList = new ArrayList<>(byZone.getOrDefault(army.getZoneId(), Collections.emptyList()));
        for (NobleArmy existing : zoneList) {
            if (!existing.getHouseId().equals(army.getHouseId())) continue;

            if (canMergeOrders(existing.getPendingOrder(), army.getPendingOrder())) {
                existing.setSize(existing.getSize() + army.getSize());
                debug.Debug.log("noble", "merge", "Merged " + army.getSize() + " soldiers from " + army.getId() + " into " + existing.getId() + " (new size " + existing.getSize() + ")");
                if (existing.getPendingOrder() == NobleArmy.OrderType.NONE &&
                    army.getPendingOrder() != NobleArmy.OrderType.NONE) {
                    existing.issueOrder(army.getPendingOrder(), army.getPendingTargetZoneId());
                    if (army.isCoalitionAttack() && army.getCoalitionMemberIds() != null) {
                        existing.issueCoalitionOrder(army.getPendingTargetZoneId(),
                                                     new HashSet<>(army.getCoalitionMemberIds()));
                    }
                    debug.Debug.log("noble", "order-issued", "During merge, transferred order " + army.getPendingOrder() + " to army " + existing.getId());
                }
                return;
            }
        }
        armies.add(army);
        byHouse.computeIfAbsent(army.getHouseId(), k -> new ArrayList<>()).add(army);
        byZone.computeIfAbsent(army.getZoneId(),   k -> new ArrayList<>()).add(army);
    }

    private boolean canMergeOrders(NobleArmy.OrderType existingOrder, NobleArmy.OrderType newOrder) {
        if (existingOrder == newOrder) return true;
        if (existingOrder == NobleArmy.OrderType.ATTACK && newOrder == NobleArmy.OrderType.JOIN_BATTLE) return true;
        if (existingOrder == NobleArmy.OrderType.JOIN_BATTLE && newOrder == NobleArmy.OrderType.ATTACK) return true;
        if (existingOrder == NobleArmy.OrderType.NONE && newOrder == NobleArmy.OrderType.NONE) return true;
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
        if (newZoneId == null) return;
        if (newZoneId.equals(army.getZoneId())) return;

        List<NobleArmy> oldList = byZone.get(army.getZoneId());
        if (oldList != null) oldList.remove(army);

        army.setZoneId(newZoneId);

        List<NobleArmy> destList = new ArrayList<>(byZone.getOrDefault(newZoneId, Collections.emptyList()));
        for (NobleArmy existing : destList) {
            if (existing != army && existing.getHouseId().equals(army.getHouseId()) &&
                canMergeOrders(existing.getPendingOrder(), army.getPendingOrder())) {
                existing.setSize(existing.getSize() + army.getSize());
                debug.Debug.log("noble", "merge", "Moving army " + army.getId() + " merged into " + existing.getId() + " at " + newZoneId + " (new size " + existing.getSize() + ")");
                if (existing.getPendingOrder() == NobleArmy.OrderType.NONE &&
                    army.getPendingOrder() != NobleArmy.OrderType.NONE) {
                    existing.issueOrder(army.getPendingOrder(), army.getPendingTargetZoneId());
                    if (army.isCoalitionAttack() && army.getCoalitionMemberIds() != null) {
                        existing.issueCoalitionOrder(army.getPendingTargetZoneId(),
                                                     new HashSet<>(army.getCoalitionMemberIds()));
                    }
                    debug.Debug.log("noble", "order-issued", "During move, transferred order " + army.getPendingOrder() + " from " + army.getId() + " to " + existing.getId());
                }
                armies.remove(army);
                List<NobleArmy> h = byHouse.get(army.getHouseId());
                if (h != null) h.remove(army);
                return;
            }
        }

        byZone.computeIfAbsent(newZoneId, k -> new ArrayList<>()).add(army);
        debug.Debug.log("noble", "move", "Army " + army.getId() + " moved to " + newZoneId + " without merging");
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