package main.nobles;

import main.nobles.ai.NobleAI;
import main.parameters.GameParameters;
import main.resources.ResourcePool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owns all noble houses, drives their per-turn economy and AI tick.
 */
public class NobleHouseManager {

    private final List<NobleHouse>    houses        = new ArrayList<>();
    private final RelationshipManager relationships = new RelationshipManager();

    public NobleHouseManager(main.map.ZoneManager zoneManager) {
        buildHouses();
    }

    // ─── Turn processing ──────────────────────────────────────────────────────

    public List<String> processTurn(ResourcePool playerResources) {
        List<String> log = new ArrayList<>();

        for (NobleHouse house : houses) {
            processEconomy(house, playerResources, log);
        }

        // AI ticks — copy list to avoid concurrent modification
        List<NobleHouse> snapshot = new ArrayList<>(houses);
        for (NobleHouse house : snapshot) {
            List<String> aiLog = NobleAI.tick(house, snapshot, relationships);
            log.addAll(aiLog);
        }

        return log;
    }

    private void processEconomy(NobleHouse house, ResourcePool playerResources,
                                 List<String> log) {
        int sentManpower = house.computeManpowerSentToPlayer();
        int keptManpower = house.computeManpowerRetained();
        if (sentManpower > 0) playerResources.addManpower(sentManpower);
        house.addManpower(keptManpower);

        if (house.sendsResourcesToPlayer()) {
            int zoneGold = houseZoneGold(house);
            playerResources.addMoney(zoneGold);
            house.addGold(zoneGold);
            log.add(house.getName() + " sent " + zoneGold + " gold.");
        } else {
            house.addGold(houseZoneGold(house) * 2);
            log.add(house.getName() + " is hostile — sent nothing to player.");
        }

        house.addInfluence(house.getInfluencePerTurn());
        house.payUpkeep();

        if (sentManpower > 0) {
            log.add(house.getName() + " sent " + sentManpower + " manpower.");
        }
    }

    private int houseZoneGold(NobleHouse house) {
        return house.getZoneIds().size() * GameParameters.NOBLE_ZONE_GOLD_PER_TURN;
    }

    // ─── Accessors ───────────────────────────────────────────────────────────

    public List<NobleHouse>    getHouses()             { return Collections.unmodifiableList(houses); }
    public RelationshipManager getRelationships()      { return relationships; }

    public NobleHouse getHouseById(String id) {
        return houses.stream().filter(h -> h.getId().equals(id))
                     .findFirst().orElse(null);
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
        buildHouses();
    }

    // ─── House definitions ───────────────────────────────────────────────────

    private void buildHouses() {

        houses.add(new NobleHouse("house_valdris", "House Valdris",
            NobleHouse.Race.HUMAN,
            List.of("northern_vale", "greenvale", "westgate"),
            List.of(
                new NobleCharacter("Lord Edaran Valdris",
                    "Silver-tongued and patient. Has waited twenty years for his moment.",
                    Motivation.PRESTIGE, Motivation.WEALTH, 0.7, 0.3),
                new NobleCharacter("Heir Cael Valdris",
                    "Young and ambitious. Wants to prove himself in the field.",
                    Motivation.EXPANSION, Motivation.PRESTIGE, 0.65, 0.35),
                new NobleCharacter("Steward Mira Valdris",
                    "Pragmatic administrator. Believes wars are won by accountants.",
                    Motivation.WEALTH, Motivation.SECURITY, 0.8, 0.2)
            ),
            120, 60
        ));

        houses.add(new NobleHouse("house_thornmere", "House Thornmere",
            NobleHouse.Race.ELF,
            List.of("iceveil_tundra", "far_north", "snowmarch", "frostpeak_pass"),
            List.of(
                new NobleCharacter("Lady Serafin Thornmere",
                    "Cold pragmatist. Commands loyalty through fear dressed as respect.",
                    Motivation.SECURITY, Motivation.EXPANSION, 0.7, 0.3),
                new NobleCharacter("Warden Aethos Thornmere",
                    "Isolationist. Wants no part of southern politics.",
                    Motivation.SECURITY, Motivation.WEALTH, 0.75, 0.25),
                new NobleCharacter("Scout-Lord Vel Thornmere",
                    "Aggressive expansionist. Eyes the northern passes.",
                    Motivation.EXPANSION, Motivation.PRESTIGE, 0.8, 0.2)
            ),
            100, 55
        ));

        houses.add(new NobleHouse("house_ashkar", "House Ashkar",
            NobleHouse.Race.ORC,
            List.of("eastern_plains", "ashfield", "highland_gap"),
            List.of(
                new NobleCharacter("Warlord Duvrak Ashkar",
                    "Blunt, honourable, and deeply suspicious of paperwork.",
                    Motivation.EXPANSION, Motivation.SECURITY, 0.75, 0.25),
                new NobleCharacter("Champion Brak Ashkar",
                    "Pure warrior. Respects only combat results.",
                    Motivation.EXPANSION, Motivation.PRESTIGE, 0.85, 0.15),
                new NobleCharacter("Elder Gruum Ashkar",
                    "Old and tired. Wants peace before he dies.",
                    Motivation.SECURITY, Motivation.WEALTH, 0.7, 0.3)
            ),
            80, 45
        ));

        houses.add(new NobleHouse("house_deepvein", "House Deepvein",
            NobleHouse.Race.DWARF,
            List.of("stonepass", "trade_coast", "far_east"),
            List.of(
                new NobleCharacter("Thane Hurga Deepvein",
                    "Meticulous record-keeper. Every favour is logged.",
                    Motivation.WEALTH, Motivation.SECURITY, 0.8, 0.2),
                new NobleCharacter("Forgemaster Dolgrin Deepvein",
                    "Industrialist. Wants trade routes above all else.",
                    Motivation.WEALTH, Motivation.EXPANSION, 0.75, 0.25),
                new NobleCharacter("Ironguard Bera Deepvein",
                    "Military pragmatist. Wealth through strength.",
                    Motivation.SECURITY, Motivation.WEALTH, 0.65, 0.35)
            ),
            90, 65
        ));

        houses.add(new NobleHouse("house_crestfall", "House Crestfall",
            NobleHouse.Race.HUMAN,
            List.of("river_bend", "southern_march", "ironhaven"),
            List.of(
                new NobleCharacter("Lord Aldric Crestfall",
                    "Jovial on the surface. Underneath: a careful chess player.",
                    Motivation.PRESTIGE, Motivation.EXPANSION, 0.7, 0.3),
                new NobleCharacter("Lady Vorn Crestfall",
                    "Diplomatic genius. Builds alliances like fortresses.",
                    Motivation.PRESTIGE, Motivation.SECURITY, 0.75, 0.25),
                new NobleCharacter("Captain Renn Crestfall",
                    "Straightforward soldier. Distrusts politics entirely.",
                    Motivation.SECURITY, Motivation.EXPANSION, 0.7, 0.3)
            ),
            70, 50
        ));

        houses.add(new NobleHouse("house_sylvaine", "House Sylvaine",
            NobleHouse.Race.ELF,
            List.of("thornwood", "bramblewood", "redcliff"),
            List.of(
                new NobleCharacter("Archon Thessiel Sylvaine",
                    "Ethereal and unreadable. Speaks in half-sentences.",
                    Motivation.PRESTIGE, Motivation.WEALTH, 0.75, 0.25),
                new NobleCharacter("Keeper Aevi Sylvaine",
                    "Ancient archivist. Hoards knowledge and leverage equally.",
                    Motivation.PRESTIGE, Motivation.SECURITY, 0.8, 0.2),
                new NobleCharacter("Blade Sorn Sylvaine",
                    "Unconventional warrior. Acts where words fail.",
                    Motivation.EXPANSION, Motivation.PRESTIGE, 0.7, 0.3)
            ),
            85, 70
        ));

        houses.add(new NobleHouse("house_duskmantle", "House Duskmantle",
            NobleHouse.Race.HUMAN,
            List.of("duskfall", "wetmarsh"),
            List.of(
                new NobleCharacter("Baron Orryn Duskmantle",
                    "Paranoid and brilliant. Has contingency plans for everything.",
                    Motivation.SECURITY, Motivation.WEALTH, 0.75, 0.25),
                new NobleCharacter("Spy-Master Lira Duskmantle",
                    "Sees conspiracies everywhere. Half of them are real.",
                    Motivation.SECURITY, Motivation.PRESTIGE, 0.7, 0.3),
                new NobleCharacter("Marshal Dorn Duskmantle",
                    "Aggressive defender. Attack is the best defense.",
                    Motivation.EXPANSION, Motivation.SECURITY, 0.65, 0.35)
            ),
            60, 40
        ));

        houses.add(new NobleHouse("house_saltborn", "House Saltborn",
            NobleHouse.Race.HUMAN,
            List.of("saltmere", "port_reach"),
            List.of(
                new NobleCharacter("Admiral Vessa Saltborn",
                    "Weathered sailor turned noble. Respects directness.",
                    Motivation.WEALTH, Motivation.EXPANSION, 0.75, 0.25),
                new NobleCharacter("Harbormaster Crul Saltborn",
                    "Trade above all. War is bad for shipping.",
                    Motivation.WEALTH, Motivation.SECURITY, 0.8, 0.2),
                new NobleCharacter("Corsair Bren Saltborn",
                    "Pirate heritage never fully left. Raids first, asks later.",
                    Motivation.EXPANSION, Motivation.WEALTH, 0.7, 0.3)
            ),
            75, 50
        ));

        houses.add(new NobleHouse("house_emberveil", "House Emberveil",
            NobleHouse.Race.ELF,
            List.of("ashenveil"),
            List.of(
                new NobleCharacter("Matriarch Ysolde Emberveil",
                    "Warm and welcoming until crossed. The smile never leaves.",
                    Motivation.PRESTIGE, Motivation.SECURITY, 0.7, 0.3),
                new NobleCharacter("Oracle Fenn Emberveil",
                    "Mystical and withdrawn. Acts on visions others cannot see.",
                    Motivation.PRESTIGE, Motivation.WEALTH, 0.75, 0.25),
                new NobleCharacter("Sentinel Dravan Emberveil",
                    "Silent guardian. Speaks through action alone.",
                    Motivation.SECURITY, Motivation.EXPANSION, 0.8, 0.2)
            ),
            65, 45
        ));
    }
}