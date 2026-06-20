// NobleHouseManager.java
package City.main.nobles;

import City.debug.Debug;
import City.main.map.Zone;
import City.main.map.ZoneManager;
import City.main.map.ZoneState;
import City.main.nobles.ai.NobleAI;
import City.main.nobles.ai.NobleBankingAI;
import City.main.nobles.NobleArmyManager;
import City.main.bank.BankAI;
import City.main.bank.BankManager;
import City.main.bank.DragonBankManager;
import City.main.parameters.BankParams;
import City.main.parameters.DiplomacyParams;
 
import City.main.parameters.NobleAIParams;
import City.main.parameters.NobleHouseParams;
import City.main.resources.ResourcePool;

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
    private       City.main.barbarians.RavagedZoneManager ravagedZoneManager;
    private       City.main.barbarians.BarbArmyManager    barbArmyManager;
    private       int lastPlayerGoldSent = 0;
    private       int lastPlayerFoodSent = 0;
    private       BankManager bankManager;
    private final DragonBankManager dragonBankManager;

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
        buildLandlessHouses();
        this.bankManager = new BankManager(relationships, armyManager, getHouseById(BankParams.BANK_HOUSE_ID));
        this.armyManager.setBankManager(bankManager);
        City.main.nobles.ai.NobleAIPower.setBankManager(bankManager);
        this.dragonBankManager = new DragonBankManager(armyManager);
    }

public void setRavagedZoneManager(City.main.barbarians.RavagedZoneManager rzm) {
        this.ravagedZoneManager = rzm;
    }

    public void setBarbArmyManager(City.main.barbarians.BarbArmyManager bam) {
        this.barbArmyManager = bam;
    }

    public City.main.barbarians.BarbArmyManager getBarbArmyManagerRef() {
        return barbArmyManager;
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

public List<String> processTurn(ResourcePool playerResources, City.main.ledger.Ledger ledger) {
        List<String> log = new ArrayList<>();

        tickZoneStates();

        lastPlayerGoldSent = 0;
        lastPlayerFoodSent = 0;

        for (NobleHouse house : houses) {
            if (house.isEliminated()) continue;

            // 1. Resolve this house's pending orders
            log.addAll(armyManager.resolveOrdersForHouse(house.getId(),
                new ArrayList<>(houses), claimManager));

            // 2. Economy
            processEconomy(house, playerResources, log, ledger);
        }

        processConquestCosts();

        // 3. Per-house: disband idle → AI tick → upkeep
        for (NobleHouse house : houses) {
            if (house.isEliminated()) continue;

            // Disband idle armies — manpower returns, gold drain stops
            armyManager.disbandIdleArmies(house);

            if (house.isBank()) {
                // The Bank never attacks and runs its own survival/economy logic.
                List<String> bankAiLog = BankAI.tick(house, new ArrayList<>(houses),
                        relationships, armyManager, bankManager, zoneManager, barbArmyManager, new ArrayList<>());
                log.addAll(bankAiLog);
                log.addAll(bankManager.processTurn(new ArrayList<>(houses), playerResources.getManpower(), playerResources));
            } else {
                // AI tick — may recruit and issue new orders (ATTACK, RAID, JOIN_BATTLE)
                List<String> aiLog = NobleAI.tick(
                    house, new ArrayList<>(houses), relationships,
                    claimManager, zoneManager, armyManager);
                log.addAll(aiLog);

                int warChestTarget = NobleAI.getWarChestTarget(
                        house, new ArrayList<>(houses), relationships, armyManager);
                NobleBankingAI.tick(house, warChestTarget, bankManager, dragonBankManager,
                        new ArrayList<>(houses), relationships, log);
            }

            // Upkeep — newly recruited armies have skipNextUpkeep set
            armyManager.payUpkeep(house);
        }

        // 4. Tick orders so this turn's orders resolve next turn
        armyManager.tickOrders();

        // Dragon Bank — abstract, distant lender; processed once per turn,
        // independent of any single house's loop iteration.
        log.addAll(dragonBankManager.processTurn(new ArrayList<>(houses), playerResources));

        NobleAI.tickThreatenedDecay(houses);
        NobleAI.tickClaimDecay(new ArrayList<>(houses), relationships, claimManager, log);

        processRebellions(new ArrayList<>(houses), log);

        if (barbArmyManager != null) {
            log.addAll(NobleBarbHunter.processTurn(
                    new ArrayList<>(houses),
                    armyManager,
                    nobleHouseManager -> nobleHouseManager,
                    this,
                    zoneManager));
        }
        log.addAll(coalitionManager.checkCoalitions(new ArrayList<>(houses)));

        for (NobleHouse house : houses) {
            if (!house.isEliminated()) {
                house.tickGarrisons();
            } else {
                for (NobleArmy a : new ArrayList<>(armyManager.getArmiesForHouse(house.getId()))) {
                    armyManager.remove(a);
                }
                if (ledger != null) {
                    ledger.removeRecurring("nobles", house.getName());
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
            int capacity = cunning + NobleAIParams.ADMIN_CAPACITY_BASE;
            int extraZones = Math.max(0, house.getZoneIds().size() - capacity);
            double increaseChance = NobleAIParams.REBELLION_BASE_CHANCE + extraZones * NobleAIParams.REBELLION_OVEREXTENSION_PER_ZONE;
            double decayChance = NobleAIParams.REBELLION_DECAY_BASE_CHANCE + cunning * NobleAIParams.REBELLION_DECAY_CUNNING_PER_POINT;

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
                            zoneManager.getState(zoneId).addRebellionPower(-NobleAIParams.REBELLION_POWER_DECREASE);
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
                        state.addRebellionPower(NobleAIParams.REBELLION_POWER_INCREASE);
                        Debug.log("noble", "rebellion", house.getName() + " zone=" + zoneId
                                + " REBELLION +" + NobleAIParams.REBELLION_POWER_INCREASE
                                + " → " + state.getRebellionPower()
                                + " (extraZones=" + extraZones + ", chance=" + increaseChance + ")");
                    }
                } else {
                    // Not overextended, decay
                    if (state.getRebellionPower() > 0 && rng.nextDouble() < decayChance) {
                        int oldPower = state.getRebellionPower();
                        state.addRebellionPower(-NobleAIParams.REBELLION_POWER_DECREASE);
                        Debug.log("noble", "rebellion", house.getName() + " zone=" + zoneId
                                + " REBELLION -" + NobleAIParams.REBELLION_POWER_DECREASE
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
                double threshold = (garrison + idleArmies) * NobleAIParams.REBELLION_FLIP_MULTIPLIER;
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
                    City.main.map.Zone zone = zoneManager.getZone(zoneId);
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
            int goldCost      = (int)(malus * DiplomacyParams.CONQUEST_MALUS_GOLD_COST_PER_PERCENT);
            int influenceCost = (int)(malus * DiplomacyParams.CONQUEST_MALUS_INFLUENCE_COST_PER_PERCENT);
            if (goldCost > 0)      house.addGold(-Math.min(goldCost, house.getGold()));
            if (influenceCost > 0) house.addInfluence(-Math.min(influenceCost, house.getInfluence()));
        }
    }

    // ─── Economy ─────────────────────────────────────────────────────────────

private void processEconomy(NobleHouse house, ResourcePool playerResources,
                                List<String> log, City.main.ledger.Ledger ledger) {
        int sentManpower = house.computeManpowerSentToPlayer();
        int keptManpower = house.computeManpowerRetained();
        house.addNobleManpower(keptManpower);

        double share     = getPlayerShareFraction(house.getPlayerOpinion());
        int totalGold    = computeHouseGold(house);
        int totalFood    = computeHouseFood(house);
        int playerGold   = (int)(totalGold * share);
        int playerFood   = (int)(totalFood * share);
        int houseGold    = totalGold - playerGold;
        int houseFood    = totalFood - playerFood;

        // House keeps its share internally
        house.addGold(houseGold);
        house.addFood(houseFood);
        lastPlayerGoldSent += playerGold;
        lastPlayerFoodSent += playerFood;

        // Player share registered to ledger — TurnProcessor applies in one shot
        ledger.setRecurring(City.main.resources.ResourceType.GOLD,     "nobles", house.getName(), playerGold);
        ledger.setRecurring(City.main.resources.ResourceType.FOOD,     "nobles", house.getName(), playerFood);
        ledger.setRecurring(City.main.resources.ResourceType.MANPOWER, "nobles", house.getName(), sentManpower);

        house.addInfluence(house.getInfluencePerTurn());

        City.debug.Debug.log("economy", "income",
                house.getName() + " — playerGold=" + playerGold
                + " playerFood=" + playerFood + " sentManpower=" + sentManpower);

        if (share > 0.0) {
            log.add(house.getName() + " sent " + playerGold + " gold, " + playerFood + " food.");
        } else {
            log.add(house.getName() + " is hostile — sent nothing to player.");
        }

        if (sentManpower > 0) {
            log.add(house.getName() + " sent " + sentManpower + " manpower to player.");
        }
    }

private int computeHouseGold(NobleHouse house) {
        int total = 0;
        for (String zoneId : house.getZoneIds()) {
            ZoneState state  = zoneManager.getState(zoneId);
            double ravagedMult = ravagedZoneManager != null
                    ? ravagedZoneManager.getProductionMultiplier(zoneId) : 1.0;
            double mult = state != null
                    ? state.getProductionMultiplier(ravagedMult) : ravagedMult;
            Zone zone = zoneManager.getZone(zoneId);
            int base = zone != null ? zone.getGoldProduction() : 0;
            total += (int)((base + NobleHouseParams.NOBLE_ZONE_GOLD_PER_TURN) * mult);
        }
        return total;
    }

private int computeHouseFood(NobleHouse house) {
        int total = 0;
        for (String zoneId : house.getZoneIds()) {
            ZoneState state  = zoneManager.getState(zoneId);
            double ravagedMult = ravagedZoneManager != null
                    ? ravagedZoneManager.getProductionMultiplier(zoneId) : 1.0;
            double mult = state != null
                    ? state.getProductionMultiplier(ravagedMult) : ravagedMult;
            Zone zone = zoneManager.getZone(zoneId);
            int base = zone != null ? zone.getFoodProduction() : 0;
            total += (int)(base * mult);
        }
        return total;
    }

// ─── Accessors ───────────────────────────────────────────────────────────

    public java.util.List<NobleHouse> getHouses() { return Collections.unmodifiableList(houses); }
    public RelationshipManager getRelationships() { return relationships; }
    public ClaimManager        getClaimManager()  { return claimManager; }

public ZoneManager getZoneManager() { return zoneManager; }

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
     * Transfer a zone from one owner to another, clearing the unlawful mark (rule A).
     */

public void transferZoneOwnership(String zoneId, NobleHouse from, NobleHouse to) {
        if (from != null) from.removeZone(zoneId);
        if (to   != null) to.addZone(zoneId);
        // Rule A: both marks clear when ownership changes
        City.main.map.ZoneState state = zoneManager.getState(zoneId);
        if (state != null) {
            state.clearUnlawfullyAcquired();
            state.clearLawfullyAcquired();
        }
        City.debug.Debug.log("noble", "zone-transfer",
                zoneId + " → " + (to != null ? to.getName() : "ungoverned")
                + " (unlawful/lawful marks cleared)");
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
        buildLandlessHouses();
        if (bankManager != null) {
            bankManager.reset();
            bankManager.setBankHouse(getHouseById(BankParams.BANK_HOUSE_ID));
        }
        dragonBankManager.reset();
    }

public CoalitionManager getCoalitionManager() { return coalitionManager; }

    /** Convenience accessor used by NobleBarbHunter. */
    public NobleHouseManager getSelf() { return this; }

    public int getLastPlayerGoldSent() { return lastPlayerGoldSent; }
    public int getLastPlayerFoodSent() { return lastPlayerFoodSent; }

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
            List.of("iceveil_tundra", "far_north", "snowmarch"),
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

        houses.add(buildBankHouse());
    }

/**
     * Landless noble houses — no zones, but each holds claims on territory
     * owned by the seated houses. They register as MINOR_NOBLE voters in
     * the Realm Council (see CouncilSessionManager) and are eligible to
     * reclaim land via rebellion flips or coalition victories.
     */
    private void buildLandlessHouses() {
        addLandlessHouse("house_varlow", "House Varlow", NobleHouse.Race.HUMAN, 8, 25,
            List.of(
                new NobleCharacter("Factor Yannick Varlow",
                    "Smooth-talking ledger-keeper who never forgot a debt owed to his family.",
                    Motivation.WEALTH, Motivation.PRESTIGE, 0.7, 0.3, 2, 0, 2),
                new NobleCharacter("Heiress Sable Varlow",
                    "Raised on stories of the counting-houses her family lost.",
                    Motivation.WEALTH, Motivation.EXPANSION, 0.65, 0.35, 1, 1, 2),
                new NobleCharacter("Old Tobus Varlow",
                    "The last to remember the family's wharf before it was seized.",
                    Motivation.SECURITY, Motivation.WEALTH, 0.75, 0.25, 1, 0, 1)
            ),
            "trade_coast", "port_reach");

        addLandlessHouse("house_korrath", "House Korrath", NobleHouse.Race.ORC, 5, 20,
            List.of(
                new NobleCharacter("Warbroken Ghazna Korrath",
                    "Once led a thousand spears; now leads a memory.",
                    Motivation.EXPANSION, Motivation.PRESTIGE, 0.7, 0.3, 1, 2, 1),
                new NobleCharacter("Shaman Vrok Korrath",
                    "Reads omens in the bones of old battles.",
                    Motivation.SECURITY, Motivation.EXPANSION, 0.7, 0.3, 1, 1, 2),
                new NobleCharacter("Pup-Chief Dazh Korrath",
                    "Young, hungry, and tired of being landless.",
                    Motivation.EXPANSION, Motivation.SECURITY, 0.75, 0.25, 0, 3, 0)
            ),
            "eastern_plains", "frostpeak_pass");

        addLandlessHouse("house_wrenfeld", "House Wrenfeld", NobleHouse.Race.HUMAN, 10, 15,
            List.of(
                new NobleCharacter("Banker Idris Wrenfeld",
                    "Meticulous and obsessed with restoring the family vault.",
                    Motivation.WEALTH, Motivation.PRESTIGE, 0.75, 0.25, 2, 0, 2),
                new NobleCharacter("Clerk Pell Wrenfeld",
                    "A nervous accountant who dreams of solvency.",
                    Motivation.WEALTH, Motivation.SECURITY, 0.7, 0.3, 1, 0, 1),
                new NobleCharacter("Heir Tamsin Wrenfeld",
                    "A reckless gambler hoping one bold move fixes everything.",
                    Motivation.WEALTH, Motivation.EXPANSION, 0.6, 0.4, 1, 1, 1)
            ),
            "westgate");

        addLandlessHouse("house_stillwater", "House Stillwater", NobleHouse.Race.ELF, 6, 20,
            List.of(
                new NobleCharacter("Matron Ilyenne Stillwater",
                    "Speaks of the river as a living ancestor owed its due.",
                    Motivation.SECURITY, Motivation.PRESTIGE, 0.7, 0.3, 2, 0, 2),
                new NobleCharacter("Wavekeeper Toren Stillwater",
                    "Believes the marshes will rise again for his line.",
                    Motivation.SECURITY, Motivation.EXPANSION, 0.65, 0.35, 1, 1, 2),
                new NobleCharacter("Drifter Sael Stillwater",
                    "Wanders the banks collecting old grievances.",
                    Motivation.EXPANSION, Motivation.WEALTH, 0.7, 0.3, 0, 1, 2)
            ),
            "river_bend", "wetmarsh");

        addLandlessHouse("house_ashgrave", "House Ashgrave", NobleHouse.Race.DWARF, 4, 18,
            List.of(
                new NobleCharacter("Ashlord Borin Ashgrave",
                    "Believes the family's fortune rose from fire and will rise again.",
                    Motivation.SECURITY, Motivation.PRESTIGE, 0.7, 0.3, 1, 1, 2),
                new NobleCharacter("Cinder-Priestess Maela Ashgrave",
                    "Tends sacred coals in a shrine that isn't legally hers.",
                    Motivation.PRESTIGE, Motivation.SECURITY, 0.75, 0.25, 2, 0, 2),
                new NobleCharacter("Stoker Hagen Ashgrave",
                    "Blunt and impatient — wants results, not rituals.",
                    Motivation.EXPANSION, Motivation.SECURITY, 0.7, 0.3, 0, 2, 1)
            ),
            "ashfield", "ashenveil");

        addLandlessHouse("house_mournhollow", "House Mournhollow", NobleHouse.Race.HUMAN, 7, 22,
            List.of(
                new NobleCharacter("Lady Selwyn Mournhollow",
                    "Widowed twice over by feuds that were never hers to begin with.",
                    Motivation.PRESTIGE, Motivation.SECURITY, 0.7, 0.3, 2, 0, 1),
                new NobleCharacter("Mourner Aldous Mournhollow",
                    "Keeps a ledger of every wrong done to the family.",
                    Motivation.SECURITY, Motivation.WEALTH, 0.75, 0.25, 1, 0, 2),
                new NobleCharacter("Young Wren Mournhollow",
                    "Doesn't remember a time when the family had land.",
                    Motivation.EXPANSION, Motivation.PRESTIGE, 0.65, 0.35, 0, 2, 1)
            ),
            "duskfall");

        addLandlessHouse("house_brackenwood", "House Brackenwood", NobleHouse.Race.ELF, 5, 20,
            List.of(
                new NobleCharacter("Warden Lyriel Brackenwood",
                    "Speaks for trees that no longer answer to her family.",
                    Motivation.SECURITY, Motivation.EXPANSION, 0.7, 0.3, 1, 1, 1),
                new NobleCharacter("Pathfinder Orin Brackenwood",
                    "Knows every deer-trail in woods he can't legally enter.",
                    Motivation.EXPANSION, Motivation.SECURITY, 0.7, 0.3, 0, 2, 1),
                new NobleCharacter("Elder Fennir Brackenwood",
                    "Patient as old growth, certain the woods remember.",
                    Motivation.PRESTIGE, Motivation.SECURITY, 0.75, 0.25, 1, 0, 2)
            ),
            "bramblewood", "thornwood");

        addLandlessHouse("house_tallowmere", "House Tallowmere", NobleHouse.Race.HUMAN, 9, 12,
            List.of(
                new NobleCharacter("Keeper Joran Tallowmere",
                    "Has kept a lamp lit for a coast he no longer owns.",
                    Motivation.SECURITY, Motivation.WEALTH, 0.7, 0.3, 1, 0, 1),
                new NobleCharacter("Net-Mother Yelena Tallowmere",
                    "Organizes fisherfolk still loyal to the old name.",
                    Motivation.WEALTH, Motivation.SECURITY, 0.7, 0.3, 2, 0, 1),
                new NobleCharacter("Drowned-Eyed Cass Tallowmere",
                    "Claims to hear the tide arguing the family's case.",
                    Motivation.PRESTIGE, Motivation.WEALTH, 0.65, 0.35, 1, 0, 2)
            ),
            "saltmere");

        addLandlessHouse("house_greyfen", "House Greyfen", NobleHouse.Race.ORC, 4, 15,
            List.of(
                new NobleCharacter("Mudreaver Skarn Greyfen",
                    "Led raids through bogs others called impassable.",
                    Motivation.EXPANSION, Motivation.WEALTH, 0.7, 0.3, 0, 2, 1),
                new NobleCharacter("Bog-Witch Yara Greyfen",
                    "Trades secrets for old debts owed to her line.",
                    Motivation.SECURITY, Motivation.EXPANSION, 0.7, 0.3, 1, 0, 2),
                new NobleCharacter("Iron-Tooth Rask Greyfen",
                    "Wants Ironhaven's forges back by any means necessary.",
                    Motivation.EXPANSION, Motivation.PRESTIGE, 0.75, 0.25, 0, 3, 0)
            ),
            "wetmarsh", "ironhaven");

        addLandlessHouse("house_quickstone", "House Quickstone", NobleHouse.Race.DWARF, 6, 18,
            List.of(
                new NobleCharacter("Prospector Dunna Quickstone",
                    "Swears the best veins were stolen from her grandsire.",
                    Motivation.WEALTH, Motivation.EXPANSION, 0.7, 0.3, 1, 0, 2),
                new NobleCharacter("Foreman Bram Quickstone",
                    "Organizes idle miners who remember better days.",
                    Motivation.WEALTH, Motivation.SECURITY, 0.7, 0.3, 1, 1, 1),
                new NobleCharacter("Assayer Tilda Quickstone",
                    "Keeps samples proving claims others dismiss.",
                    Motivation.WEALTH, Motivation.PRESTIGE, 0.75, 0.25, 1, 0, 2)
            ),
            "stonepass", "far_east");

        addLandlessHouse("house_larkspur", "House Larkspur", NobleHouse.Race.HUMAN, 8, 16,
            List.of(
                new NobleCharacter("Botanist Rosalind Larkspur",
                    "Cultivates rare blooms in borrowed soil, dreaming of her own.",
                    Motivation.PRESTIGE, Motivation.WEALTH, 0.7, 0.3, 2, 0, 1),
                new NobleCharacter("Gardener Wystan Larkspur",
                    "Patient and methodical — certain seeds outlast injustice.",
                    Motivation.SECURITY, Motivation.WEALTH, 0.7, 0.3, 1, 0, 1),
                new NobleCharacter("Wildling Faye Larkspur",
                    "Young and reckless, wants to seize back the family plots.",
                    Motivation.EXPANSION, Motivation.PRESTIGE, 0.7, 0.3, 0, 2, 1)
            ),
            "greenvale", "southern_march");

        addLandlessHouse("house_emberlight", "House Emberlight", NobleHouse.Race.ELF, 6, 28,
            List.of(
                new NobleCharacter("Cousin Ilara Emberlight",
                    "Insists the Emberveil name was split unfairly two generations back.",
                    Motivation.PRESTIGE, Motivation.SECURITY, 0.75, 0.25, 2, 0, 2),
                new NobleCharacter("Flamekeeper Soren Emberlight",
                    "Tends a single ember as proof of an unbroken lineage.",
                    Motivation.PRESTIGE, Motivation.EXPANSION, 0.7, 0.3, 1, 0, 2),
                new NobleCharacter("Quiet Vex Emberlight",
                    "Says little, plots much.",
                    Motivation.SECURITY, Motivation.EXPANSION, 0.7, 0.3, 0, 1, 3)
            ),
            "ashenveil");

        addLandlessHouse("house_marrow", "House Marrow", NobleHouse.Race.HUMAN, 5, 14,
            List.of(
                new NobleCharacter("Captain Hewett Marrow",
                    "Sold his sword so often he forgot which side he started on.",
                    Motivation.EXPANSION, Motivation.WEALTH, 0.7, 0.3, 0, 2, 1),
                new NobleCharacter("Quartermaster Brynn Marrow",
                    "Counts every coin owed for old contracts never paid.",
                    Motivation.WEALTH, Motivation.SECURITY, 0.7, 0.3, 1, 0, 1),
                new NobleCharacter("Recruit-Sergeant Owyn Marrow",
                    "Drills a company that exists mostly on paper.",
                    Motivation.EXPANSION, Motivation.PRESTIGE, 0.7, 0.3, 0, 2, 0)
            ),
            "northern_vale", "far_north");

        addLandlessHouse("house_hollowmere", "House Hollowmere", NobleHouse.Race.DWARF, 5, 17,
            List.of(
                new NobleCharacter("Delver Otric Hollowmere",
                    "Mapped tunnels under lands his family no longer holds.",
                    Motivation.SECURITY, Motivation.WEALTH, 0.7, 0.3, 1, 0, 2),
                new NobleCharacter("Hearth-Mother Greta Hollowmere",
                    "Keeps the old house traditions alive in exile.",
                    Motivation.SECURITY, Motivation.PRESTIGE, 0.75, 0.25, 1, 0, 1),
                new NobleCharacter("Tunnel-Runner Pim Hollowmere",
                    "Young and restless, knows every smuggler's route home.",
                    Motivation.WEALTH, Motivation.EXPANSION, 0.65, 0.35, 0, 1, 2)
            ),
            "highland_gap", "duskfall");

        addLandlessHouse("house_corvane", "House Corvane", NobleHouse.Race.ORC, 4, 19,
            List.of(
                new NobleCharacter("Skull-Counter Mag Corvane",
                    "Tallies enemies the way others tally coin.",
                    Motivation.EXPANSION, Motivation.PRESTIGE, 0.75, 0.25, 0, 3, 0),
                new NobleCharacter("Frost-Singer Yelka Corvane",
                    "Sings war-songs about a hall the family no longer holds.",
                    Motivation.PRESTIGE, Motivation.SECURITY, 0.7, 0.3, 1, 0, 1),
                new NobleCharacter("Cub-Lord Drez Corvane",
                    "The youngest ever to claim the title, eager to prove it means something.",
                    Motivation.EXPANSION, Motivation.SECURITY, 0.7, 0.3, 0, 2, 1)
            ),
            "snowmarch");
    }

    /**
     * Helper for landless houses: adds the house with no zones, then registers
     * its starting claims directly (bypassing the fabrication roll — these are
     * claims the house already held before it lost its land).
     */
    private void addLandlessHouse(String id, String name, NobleHouse.Race race,
                                   int startingGold, int startingPrestige,
                                   List<NobleCharacter> characters,
                                   String... claimedZoneIds) {
        NobleHouse house = new NobleHouse(id, name, race,
                List.of(), characters, startingGold, startingPrestige);
        houses.add(house);
        for (String zoneId : claimedZoneIds) {
            claimManager.addClaim(id, zoneId);
        }
    }

private double getPlayerShareFraction(int opinion) {
        if (opinion <= NobleHouseParams.NOBLE_HOSTILE_OPINION_THRESHOLD) return 0.0;
        if (opinion > 50) return 0.50;
        return 0.35;
    }

private NobleHouse buildBankHouse() {
        NobleHouse bank = new NobleHouse(BankParams.BANK_HOUSE_ID, "The Frostpeak Bank",
            NobleHouse.Race.DWARF,
            List.of(BankParams.BANK_ZONE_ID),
            List.of(
                new NobleCharacter("Master Ledger-Keeper Voril GoldStone",
                    "Counts every coin twice and trusts no one's word over a contract.",
                    Motivation.WEALTH, Motivation.SECURITY, 0.7, 0.3, 2, 0, 2),
                new NobleCharacter("Comptroller Minli Blackhammer",
                    "Calm under pressure — has talked down three bank runs already.",
                    Motivation.WEALTH, Motivation.SECURITY, 0.75, 0.25, 2, 0, 1),
                new NobleCharacter("Vault-Warden Branduin Mondrill",
                    "Former soldier turned banker. Believes a strong vault needs no apology.",
                    Motivation.SECURITY, Motivation.WEALTH, 0.65, 0.35, 1, 1, 1)
            ),
            BankParams.BANK_STARTING_GOLD, BankParams.BANK_STARTING_PRESTIGE);
        bank.setBank(true);
        bank.setManpowerGainMultiplier(BankParams.BANK_MANPOWER_GAIN_MULTIPLIER);
        bank.addFortification(BankParams.BANK_ZONE_ID, BankParams.BANK_STARTING_FORTIFICATION);
        return bank;
    }

    public BankManager getBankManager() { return bankManager; }

    public DragonBankManager getDragonBankManager() { return dragonBankManager; }

}

