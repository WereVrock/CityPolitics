// NobleHouseManager.java
package main.nobles;

import debug.Debug;
import main.map.Zone;
import main.map.ZoneManager;
import main.map.ZoneState;
import main.nobles.ai.NobleAI;
import main.nobles.NobleArmyManager;
import main.parameters.GameParameters;
import main.resources.ResourcePool;

import java.util.*;

/**
 * Owns all noble houses, claims, relationships, and noble armies.
 * Drives per-turn economy, AI ticks, garrison ticks,
 * conquest malus costs, and coalition checks.
 */
public class NobleHouseManager {

    private final List<NobleHouse>    houses        = new ArrayList<>();
    private final RelationshipManager relationships = new RelationshipManager();
    private final ClaimManager        claimManager  = new ClaimManager();
    private final ZoneManager         zoneManager;
    private final NobleArmyManager    armyManager;
    private final CoalitionManager    coalitionManager;
    private       main.barbarians.RavagedZoneManager ravagedZoneManager;

    // Prebuilt zone gold/food maps for capital tiebreaker
    private Map<String, Integer> zoneGoldMap = new HashMap<>();
    private Map<String, Integer> zoneFoodMap = new HashMap<>();

    public NobleHouseManager(ZoneManager zoneManager) {
        this.zoneManager      = zoneManager;
        this.armyManager      = new NobleArmyManager(zoneManager, relationships);
        this.coalitionManager = new CoalitionManager(zoneManager, relationships, claimManager, armyManager);
        this.armyManager.setCoalitionManager(coalitionManager);
        buildZoneMaps();
        buildHouses();
    }

    public void setRavagedZoneManager(main.barbarians.RavagedZoneManager rzm) {
        this.ravagedZoneManager = rzm;
    }

    private void buildZoneMaps() {
        zoneGoldMap.clear();
        zoneFoodMap.clear();
        for (Zone z : zoneManager.getZones()) {
            zoneGoldMap.put(z.getId(), z.getGoldProduction());
            zoneFoodMap.put(z.getId(), z.getFoodProduction());
        }
    }

    // ─── Turn processing ──────────────────────────────────────────────────────

public List<String> processTurn(ResourcePool playerResources) {
        List<String> log = new ArrayList<>();

        tickZoneStates();

        for (NobleHouse house : houses) {
            if (house.isEliminated()) continue;

            // 1. Resolve this house's pending orders
            log.addAll(armyManager.resolveOrdersForHouse(house.getId(),
                new ArrayList<>(houses), claimManager));

            // 2. Economy
            processEconomy(house, playerResources, log);
        }

        processConquestCosts();

        // 3. Per-house: disband idle → AI tick → upkeep
        for (NobleHouse house : houses) {
            if (house.isEliminated()) continue;

            // Disband idle armies — manpower returns, gold drain stops
            armyManager.disbandIdleArmies(house);

            // AI tick — may recruit and issue new orders (ATTACK, RAID, JOIN_BATTLE)
            List<String> aiLog = NobleAI.tick(
                house, new ArrayList<>(houses), relationships,
                claimManager, zoneManager, armyManager);
            log.addAll(aiLog);

            // Upkeep — newly recruited armies have skipNextUpkeep set
            armyManager.payUpkeep(house);
        }

        // 4. Tick orders so this turn's orders resolve next turn
        armyManager.tickOrders();

        NobleAI.tickThreatenedDecay(houses);
        NobleAI.tickClaimDecay(new ArrayList<>(houses), relationships, claimManager, log);

        processRebellions(new ArrayList<>(houses), log);

        log.addAll(coalitionManager.checkCoalitions(new ArrayList<>(houses)));

        for (NobleHouse house : houses) {
            if (!house.isEliminated()) {
                house.tickGarrisons();
            } else {
                // Clean up armies of eliminated houses
                for (NobleArmy a : new ArrayList<>(armyManager.getArmiesForHouse(house.getId()))) {
                    armyManager.remove(a);
                }
            }
        }

        for (NobleHouse house : houses) {
            house.recalculateCapital(zoneGoldMap, zoneFoodMap);
        }

        return log;
    }

// ─── Zone state tick ─────────────────────────────────────────────────────

    private void tickZoneStates() {
        for (NobleHouse house : houses) {
            for (String zoneId : house.getZoneIds()) {
                ZoneState state = zoneManager.getState(zoneId);
                if (state != null) state.tick();
            }
        }
    }

    private void processRebellions(List<NobleHouse> allHouses, List<String> log) {
        Random rng = new Random();
        // 1. Accumulate / decay rebellion power
        for (NobleHouse house : allHouses) {
            if (house.isEliminated()) continue;
            int cunning = house.getActiveCharacter() != null ? house.getActiveCharacter().getCunning() : 0;
            int capacity = cunning + GameParameters.ADMIN_CAPACITY_BASE;
            int extraZones = Math.max(0, house.getZoneIds().size() - capacity);
            double increaseChance = GameParameters.REBELLION_BASE_CHANCE + extraZones * GameParameters.REBELLION_OVEREXTENSION_PER_ZONE;
            double decayChance = GameParameters.REBELLION_DECAY_BASE_CHANCE + cunning * GameParameters.REBELLION_DECAY_CUNNING_PER_POINT;

            for (String zoneId : house.getZoneIds()) {
                // Only zones with claims can rebel
                boolean hasClaim = false;
                for (NobleHouse other : allHouses) {
                    if (other == house) continue;
                    if (claimManager.hasClaim(other.getId(), zoneId)) {
                        hasClaim = true;
                        break;
                    }
                }
                if (!hasClaim) {
                    // If rebellion power exists but no claim, it decays faster? We'll just leave it.
                    // But we still allow decay even without overextension.
                    if (extraZones == 0 && zoneManager.getState(zoneId).getRebellionPower() > 0) {
                        if (rng.nextDouble() < decayChance) {
                            zoneManager.getState(zoneId).addRebellionPower(-GameParameters.REBELLION_POWER_DECREASE);
                        }
                    }
                    continue;
                }

                ZoneState state = zoneManager.getState(zoneId);
                Debug.log("noble", "rebellion-check", house.getName() + " zone=" + zoneId
                        + " cunning=" + cunning + " capacity=" + capacity
                        + " extraZones=" + extraZones + " chance=" + (extraZones > 0 ? increaseChance : decayChance)
                        + " rebellionPower=" + state.getRebellionPower());
                if (extraZones > 0) {
                    if (rng.nextDouble() < increaseChance) {
                        state.addRebellionPower(GameParameters.REBELLION_POWER_INCREASE);
                        Debug.log("noble", "rebellion", house.getName() + " zone=" + zoneId
                                + " REBELLION +" + GameParameters.REBELLION_POWER_INCREASE
                                + " → " + state.getRebellionPower()
                                + " (extraZones=" + extraZones + ", chance=" + increaseChance + ")");
                    }
                } else {
                    // Not overextended, decay
                    if (state.getRebellionPower() > 0 && rng.nextDouble() < decayChance) {
                        int oldPower = state.getRebellionPower();
                        state.addRebellionPower(-GameParameters.REBELLION_POWER_DECREASE);
                        Debug.log("noble", "rebellion", house.getName() + " zone=" + zoneId
                                + " REBELLION -" + GameParameters.REBELLION_POWER_DECREASE
                                + " → " + state.getRebellionPower()
                                + " (cunning=" + cunning + ", decayChance=" + decayChance + ")");
                    }
                }
            }
        }

        // 2. Check for auto-flips
        for (NobleHouse house : new ArrayList<>(allHouses)) {
            if (house.isEliminated()) continue;
            for (String zoneId : new ArrayList<>(house.getZoneIds())) {
                ZoneState state = zoneManager.getState(zoneId);
                int rebellion = state.getRebellionPower();
                if (rebellion <= 0) continue;

                int garrison = house.getGarrisonFor(zoneId);
                int idleArmies = armyManager.getTotalIdleArmySize(house.getId(), zoneId);
                double threshold = (garrison + idleArmies) * GameParameters.REBELLION_FLIP_MULTIPLIER;
                if (rebellion <= threshold) continue;

                // 3. Select claimant
                List<NobleHouse> claimants = new ArrayList<>();
                for (NobleHouse other : allHouses) {
                    if (other == house || other.isEliminated()) {
                        if (claimManager.hasClaim(other.getId(), zoneId)) {
                            claimants.add(other); // landless
                        }
                    } else {
                        if (claimManager.hasClaim(other.getId(), zoneId)) {
                            claimants.add(other);
                        }
                    }
                }
                if (claimants.isEmpty()) continue;

                // Priority: landless > adjacent > any, each group by cunning desc
                NobleHouse winner = null;
                int bestCunning = -1;

                // Landless
                for (NobleHouse c : claimants) {
                    if (c.isEliminated()) {
                        int cun = c.getActiveCharacter() != null ? c.getActiveCharacter().getCunning() : 0;
                        if (cun > bestCunning) {
                            winner = c;
                            bestCunning = cun;
                        }
                    }
                }
                if (winner == null) {
                    // Adjacent
                    Set<String> adjacentZones = new HashSet<>();
                    main.map.Zone zone = zoneManager.getZone(zoneId);
                    if (zone != null) adjacentZones.addAll(zone.getAdjacentIds());
                    for (NobleHouse c : claimants) {
                        if (c.isEliminated()) continue;
                        boolean adj = false;
                        for (String z : c.getZoneIds()) {
                            if (adjacentZones.contains(z)) { adj = true; break; }
                        }
                        if (adj) {
                            int cun = c.getActiveCharacter() != null ? c.getActiveCharacter().getCunning() : 0;
                            if (cun > bestCunning) {
                                winner = c;
                                bestCunning = cun;
                            }
                        }
                    }
                }
                if (winner == null) {
                    // Any
                    for (NobleHouse c : claimants) {
                        if (c.isEliminated()) continue;
                        int cun = c.getActiveCharacter() != null ? c.getActiveCharacter().getCunning() : 0;
                        if (cun > bestCunning) {
                            winner = c;
                            bestCunning = cun;
                        }
                    }
                }
                if (winner == null) continue;

                // 4. Transfer zone
                boolean wasLandless = winner.isEliminated();
                house.removeZone(zoneId);
                winner.addZone(zoneId);
                winner.resetGarrison(zoneId);
                state.setRebellionPower(0);
                // Move owner's idle armies out
                for (NobleArmy a : new ArrayList<>(armyManager.getArmiesInZone(zoneId, house.getId()))) {
                    if (!a.hasPendingOrder()) {
                        String capital = house.getCapitalZoneId();
                        if (capital != null) {
                            armyManager.moveArmy(a, capital);
                        } else {
                            armyManager.remove(a);
                        }
                    }
                }
                String revivalMsg = wasLandless ? (" " + winner.getName() + " is revived!") : "";
                log.add("Rebellion in " + zoneId + "! Zone transfers to " + winner.getName() + "." + revivalMsg);
                Debug.log("noble", "rebellion", "FLIP " + zoneId + " from " + house.getName()
                        + " to " + winner.getName() + " (power=" + rebellion
                        + ", threshold=" + threshold + ")" + (wasLandless ? " REVIVED" : ""));
            }
        }
    }

    // ─── Conquest malus costs ─────────────────────────────────────────────────

    private void processConquestCosts() {
        for (NobleHouse house : houses) {
            processConquestCostsForHouse(house);
        }
    }

    private void processConquestCostsForHouse(NobleHouse house) {
        if (house.isEliminated()) return;
        for (String zoneId : house.getZoneIds()) {
            ZoneState state = zoneManager.getState(zoneId);
            if (state == null || !state.hasConquestMalus()) continue;
            int malus         = state.getConquestMalus();
            int goldCost      = (int)(malus * GameParameters.CONQUEST_MALUS_GOLD_COST_PER_PERCENT);
            int influenceCost = (int)(malus * GameParameters.CONQUEST_MALUS_INFLUENCE_COST_PER_PERCENT);
            if (goldCost > 0)      house.addGold(-Math.min(goldCost, house.getGold()));
            if (influenceCost > 0) house.addInfluence(-Math.min(influenceCost, house.getInfluence()));
        }
    }

    // ─── Economy ─────────────────────────────────────────────────────────────

private void processEconomy(NobleHouse house, ResourcePool playerResources,
                                 List<String> log) {
        // Manpower: split between player and house noble pool
        int sentManpower  = house.computeManpowerSentToPlayer();
        int keptManpower  = house.computeManpowerRetained();
        if (sentManpower > 0) playerResources.addManpower(sentManpower);
        house.addNobleManpower(keptManpower);

        // Gold
        if (house.sendsResourcesToPlayer()) {
            int zoneGold = computeHouseGold(house);
            playerResources.addMoney(zoneGold);
            house.addGold(zoneGold);
            log.add(house.getName() + " sent " + zoneGold + " gold.");
        } else {
            house.addGold(computeHouseGold(house) * 2);
            log.add(house.getName() + " is hostile — sent nothing to player.");
        }

        house.addInfluence(house.getInfluencePerTurn());

        if (sentManpower > 0) {
            log.add(house.getName() + " sent " + sentManpower + " manpower to player.");
        }
        // NOTE: garrison tick intentionally moved to AFTER AI recruits in processTurn
    }

    private int computeHouseGold(NobleHouse house) {
        int total = 0;
        for (String zoneId : house.getZoneIds()) {
            ZoneState state  = zoneManager.getState(zoneId);
            double ravagedMult = ravagedZoneManager != null
                    ? ravagedZoneManager.getProductionMultiplier(zoneId) : 1.0;
            double mult = state != null
                    ? state.getProductionMultiplier(ravagedMult) : ravagedMult;
            total += (int)(GameParameters.NOBLE_ZONE_GOLD_PER_TURN * mult);
        }
        return total;
    }
    // ─── Accessors ───────────────────────────────────────────────────────────

    public java.util.List<NobleHouse> getHouses() { return Collections.unmodifiableList(houses); }
    public RelationshipManager getRelationships() { return relationships; }
    public ClaimManager        getClaimManager()  { return claimManager; }
    public NobleArmyManager    getArmyManager()   { return armyManager; }

    public NobleHouse getHouseById(String id) {
        for (NobleHouse h : houses) if (h.getId().equals(id)) return h;
        return null;
    }

    public int getRaisedArmyTotal(String houseId) {
        return armyManager.getArmiesForHouse(houseId)
            .stream().mapToInt(NobleArmy::getSize).sum();
    }

    public NobleHouse getOwnerOfZone(String zoneId) {
        for (NobleHouse house : houses) {
            if (house.getZoneIds().contains(zoneId)) return house;
        }
        return null;
    }

    /**
     * Called by BarbInvasionProcessor after a noble wins against a barbarian garrison.
     * Awards the zone to the noble and clears the barbarian garrison entry.
     */
    public void awardRecapturedZone(NobleHouse noble, String zoneId) {
        // Ensure no other noble owns it (shouldn't happen but guard anyway)
        for (NobleHouse h : houses) {
            if (h != noble && h.getZoneIds().contains(zoneId)) h.removeZone(zoneId);
        }
        if (!noble.getZoneIds().contains(zoneId)) {
            noble.addZone(zoneId);
        }
        noble.resetGarrison(zoneId);
        noble.recalculateCapital(zoneGoldMap, zoneFoodMap);
    }

    public void reset() {
        houses.clear();
        relationships.reset();
        claimManager.reset();
        armyManager.reset();
        buildHouses();
    }

    public CoalitionManager getCoalitionManager() { return coalitionManager; }

    // ─── House definitions ───────────────────────────────────────────────────

    private void buildHouses() {
        houses.add(new NobleHouse("house_valdris", "House Valdris",
            NobleHouse.Race.HUMAN,
            List.of("northern_vale", "greenvale", "westgate"),
            List.of(
                new NobleCharacter("Lord Edaran Valdris",
                    "Silver-tongued and patient. Has waited twenty years for his moment.",
                    Motivation.PRESTIGE, Motivation.WEALTH, 0.7, 0.3, 3, 1, 2),
                new NobleCharacter("Heir Cael Valdris",
                    "Young and ambitious. Wants to prove himself in the field.",
                    Motivation.EXPANSION, Motivation.PRESTIGE, 0.65, 0.35, 1, 3, 1),
                new NobleCharacter("Steward Mira Valdris",
                    "Pragmatic administrator. Believes wars are won by accountants.",
                    Motivation.WEALTH, Motivation.SECURITY, 0.8, 0.2, 2, 0, 2)
            ), 120, 60));

        houses.add(new NobleHouse("house_thornmere", "House Thornmere",
            NobleHouse.Race.ELF,
            List.of("iceveil_tundra", "far_north", "snowmarch", "frostpeak_pass"),
            List.of(
                new NobleCharacter("Lady Serafin Thornmere",
                    "Cold pragmatist. Commands loyalty through fear dressed as respect.",
                    Motivation.SECURITY, Motivation.EXPANSION, 0.7, 0.3, 2, 1, 3),
                new NobleCharacter("Warden Aethos Thornmere",
                    "Isolationist. Wants no part of southern politics.",
                    Motivation.SECURITY, Motivation.WEALTH, 0.75, 0.25, 1, 1, 1),
                new NobleCharacter("Scout-Lord Vel Thornmere",
                    "Aggressive expansionist. Eyes the northern passes.",
                    Motivation.EXPANSION, Motivation.PRESTIGE, 0.8, 0.2, 0, 3, 1)
            ), 100, 55));

        houses.add(new NobleHouse("house_ashkar", "House Ashkar",
            NobleHouse.Race.ORC,
            List.of("eastern_plains", "ashfield"),
            List.of(
                new NobleCharacter("Warlord Duvrak Ashkar",
                    "Blunt, honourable, and deeply suspicious of paperwork.",
                    Motivation.EXPANSION, Motivation.SECURITY, 0.75, 0.25, 1, 3, 1),
                new NobleCharacter("Champion Brak Ashkar",
                    "Pure warrior. Respects only combat results.",
                    Motivation.EXPANSION, Motivation.PRESTIGE, 0.85, 0.15, 0, 3, 0),
                new NobleCharacter("Elder Gruum Ashkar",
                    "Old and tired. Wants peace before he dies.",
                    Motivation.SECURITY, Motivation.WEALTH, 0.7, 0.3, 2, 1, 1)
            ), 80, 45));

        houses.add(new NobleHouse("house_deepvein", "House Deepvein",
            NobleHouse.Race.DWARF,
            List.of("stonepass", "trade_coast", "far_east"),
            List.of(
                new NobleCharacter("Thane Hurga Deepvein",
                    "Meticulous record-keeper. Every favour is logged.",
                    Motivation.WEALTH, Motivation.SECURITY, 0.8, 0.2, 2, 1, 3),
                new NobleCharacter("Forgemaster Dolgrin Deepvein",
                    "Industrialist. Wants trade routes above all else.",
                    Motivation.WEALTH, Motivation.EXPANSION, 0.75, 0.25, 2, 1, 2),
                new NobleCharacter("Ironguard Bera Deepvein",
                    "Military pragmatist. Wealth through strength.",
                    Motivation.SECURITY, Motivation.WEALTH, 0.65, 0.35, 1, 2, 2)
            ), 90, 65));

        houses.add(new NobleHouse("house_crestfall", "House Crestfall",
            NobleHouse.Race.HUMAN,
            List.of("river_bend", "southern_march", "ironhaven"),
            List.of(
                new NobleCharacter("Lord Aldric Crestfall",
                    "Jovial on the surface. Underneath: a careful chess player.",
                    Motivation.PRESTIGE, Motivation.EXPANSION, 0.7, 0.3, 3, 1, 3),
                new NobleCharacter("Lady Vorn Crestfall",
                    "Diplomatic genius. Builds alliances like fortresses.",
                    Motivation.PRESTIGE, Motivation.SECURITY, 0.75, 0.25, 3, 0, 2),
                new NobleCharacter("Captain Renn Crestfall",
                    "Straightforward soldier. Distrusts politics entirely.",
                    Motivation.SECURITY, Motivation.EXPANSION, 0.7, 0.3, 1, 3, 0)
            ), 70, 50));

        houses.add(new NobleHouse("house_sylvaine", "House Sylvaine",
            NobleHouse.Race.ELF,
            List.of("thornwood", "bramblewood", "redcliff"),
            List.of(
                new NobleCharacter("Archon Thessiel Sylvaine",
                    "Ethereal and unreadable. Speaks in half-sentences.",
                    Motivation.PRESTIGE, Motivation.WEALTH, 0.75, 0.25, 2, 0, 3),
                new NobleCharacter("Keeper Aevi Sylvaine",
                    "Ancient archivist. Hoards knowledge and leverage equally.",
                    Motivation.PRESTIGE, Motivation.SECURITY, 0.8, 0.2, 1, 0, 3),
                new NobleCharacter("Blade Sorn Sylvaine",
                    "Unconventional warrior. Acts where words fail.",
                    Motivation.EXPANSION, Motivation.PRESTIGE, 0.7, 0.3, 1, 3, 2)
            ), 85, 70));

        houses.add(new NobleHouse("house_duskmantle", "House Duskmantle",
            NobleHouse.Race.HUMAN,
            List.of("duskfall", "wetmarsh", "highland_gap"),
            List.of(
                new NobleCharacter("Baron Orryn Duskmantle",
                    "Paranoid and brilliant. Has contingency plans for everything.",
                    Motivation.SECURITY, Motivation.WEALTH, 0.75, 0.25, 1, 1, 3),
                new NobleCharacter("Spy-Master Lira Duskmantle",
                    "Sees conspiracies everywhere. Half of them are real.",
                    Motivation.SECURITY, Motivation.PRESTIGE, 0.7, 0.3, 2, 0, 3),
                new NobleCharacter("Marshal Dorn Duskmantle",
                    "Aggressive defender. Attack is the best defense.",
                    Motivation.EXPANSION, Motivation.SECURITY, 0.65, 0.35, 1, 3, 1)
            ), 60, 40));

        houses.add(new NobleHouse("house_saltborn", "House Saltborn",
            NobleHouse.Race.HUMAN,
            List.of("saltmere", "port_reach"),
            List.of(
                new NobleCharacter("Admiral Vessa Saltborn",
                    "Weathered sailor turned noble. Respects directness.",
                    Motivation.WEALTH, Motivation.EXPANSION, 0.75, 0.25, 2, 2, 2),
                new NobleCharacter("Harbormaster Crul Saltborn",
                    "Trade above all. War is bad for shipping.",
                    Motivation.WEALTH, Motivation.SECURITY, 0.8, 0.2, 3, 0, 1),
                new NobleCharacter("Corsair Bren Saltborn",
                    "Pirate heritage never fully left. Raids first, asks later.",
                    Motivation.EXPANSION, Motivation.WEALTH, 0.7, 0.3, 0, 2, 3)
            ), 75, 50));

        NobleHouse emberveil = new NobleHouse("house_emberveil", "House Emberveil",
            NobleHouse.Race.ELF,
            List.of("ashenveil"),
            List.of(
                new NobleCharacter("Matriarch Ysolde Emberveil",
                    "Warm and welcoming until crossed. The smile never leaves.",
                    Motivation.PRESTIGE, Motivation.SECURITY, 0.7, 0.3, 3, 0, 3),
                new NobleCharacter("Oracle Fenn Emberveil",
                    "Mystical and withdrawn. Acts on visions others cannot see.",
                    Motivation.PRESTIGE, Motivation.WEALTH, 0.75, 0.25, 1, 0, 3),
                new NobleCharacter("Sentinel Dravan Emberveil",
                    "Silent guardian. Speaks through action alone.",
                    Motivation.SECURITY, Motivation.EXPANSION, 0.8, 0.2, 0, 3, 1)
            ), 130, 45);
        emberveil.addFortification("ashenveil", 1);
        houses.add(emberveil);
    }
}

