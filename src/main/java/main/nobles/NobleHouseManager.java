// NobleHouseManager.java
package main.nobles;

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

        // Step 1 — economy (manpower accrues to noble pool, NO garrison tick yet)
        for (NobleHouse house : houses) {
            if (!house.isEliminated()) {
                processEconomy(house, playerResources, log);
            }
        }

        processConquestCosts();

        // Step 2 — resolve orders that were issued LAST turn (already ticked)
        log.addAll(armyManager.resolveOrders(new ArrayList<>(houses), claimManager));

        // Step 3 — upkeep for existing raised armies (after resolution, before new orders)
        for (NobleHouse house : houses) {
            if (!house.isEliminated()) {
                armyManager.payUpkeep(house);
            }
        }

        // Step 4 — AI decides actions (recruit, issue new orders, diplomacy)
        NobleAI.tickThreatenedDecay(houses);
        List<NobleHouse> snapshot = new ArrayList<>(houses);
        for (NobleHouse house : snapshot) {
            if (!house.isEliminated()) {
                List<String> aiLog = NobleAI.tick(
                    house, snapshot, relationships, claimManager, zoneManager, armyManager);
                log.addAll(aiLog);
            }
        }

        log.addAll(coalitionManager.checkCoalitions(new ArrayList<>(houses)));

        // Step 5 — tick orders issued this turn so they resolve NEXT turn
        armyManager.tickOrders();

        // Step 6 — garrison tick AFTER AI recruits (leftover manpower fills garrisons)
        for (NobleHouse house : houses) {
            if (!house.isEliminated()) {
                house.tickGarrisons();
            }
        }

        // Step 7 — recalculate capitals
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

    // ─── Conquest malus costs ─────────────────────────────────────────────────

    private void processConquestCosts() {
        for (NobleHouse house : houses) {
            if (house.isEliminated()) continue;
            for (String zoneId : house.getZoneIds()) {
                ZoneState state = zoneManager.getState(zoneId);
                if (state == null || !state.hasConquestMalus()) continue;
                int malus         = state.getConquestMalus();
                int goldCost      = (int)(malus * GameParameters.CONQUEST_MALUS_GOLD_COST_PER_PERCENT);
                int influenceCost = (int)(malus * GameParameters.CONQUEST_MALUS_INFLUENCE_COST_PER_PERCENT);
                if (goldCost > 0) house.addGold(-Math.min(goldCost, house.getGold()));
                if (influenceCost > 0) house.addInfluence(-Math.min(influenceCost, house.getInfluence()));
            }
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
            ZoneState state = zoneManager.getState(zoneId);
            double mult = state != null ? state.getProductionMultiplier() : 1.0;
            total += (int)(GameParameters.NOBLE_ZONE_GOLD_PER_TURN * mult);
        }
        return total;
    }

    // ─── Accessors ───────────────────────────────────────────────────────────

    public List<NobleHouse>    getHouses()        { return Collections.unmodifiableList(houses); }
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

        houses.add(new NobleHouse("house_emberveil", "House Emberveil",
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
            ), 65, 45));
    }
}

