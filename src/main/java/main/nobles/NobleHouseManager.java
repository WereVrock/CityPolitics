// NobleHouseManager.java
package main.nobles;

import main.map.Zone;
import main.map.ZoneManager;
import main.parameters.GameParameters;
import main.resources.ResourcePool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owns all noble houses and drives their per-turn economy.
 */
public class NobleHouseManager {

    private final List<NobleHouse> houses = new ArrayList<>();

    public NobleHouseManager(ZoneManager zoneManager) {
        buildHouses();
    }

    // ─── Turn processing ──────────────────────────────────────────────────────

    /**
     * Called once per turn. Distributes manpower and gold to player,
     * accrues retained resources to each house, pays army upkeep,
     * and grows influence.
     *
     * @return log lines describing what happened
     */
    public List<String> processTurn(ResourcePool playerResources) {
        List<String> log = new ArrayList<>();

        for (NobleHouse house : houses) {
            // — Manpower —
            int sentManpower = house.computeManpowerSentToPlayer();
            int keptManpower = house.computeManpowerRetained();
            if (sentManpower > 0) playerResources.addManpower(sentManpower);
            house.addManpower(keptManpower);

            // — Gold — house zones produce gold; amount shown to player is what
            //   is actually sent. Under the hood each zone produces twice the
            //   displayed zone gold; half stays with the house.
            if (house.sendsResourcesToPlayer()) {
                int zoneGoldTotal = houseZoneGold(house);
                int sentGold      = zoneGoldTotal;          // player sees this
                int keptGold      = zoneGoldTotal;          // house keeps equal share
                playerResources.addMoney(sentGold);
                house.addGold(keptGold);
                log.add(house.getName() + " sent " + sentGold + " gold, kept " + keptGold + ".");
            } else {
                // Hostile — house keeps everything
                int keptGold = houseZoneGold(house) * 2;
                house.addGold(keptGold);
                log.add(house.getName() + " is hostile — sent nothing to player.");
            }

            // — Influence —
            house.addInfluence(house.getInfluencePerTurn());

            // — Army upkeep —
            house.payUpkeep();

            if (sentManpower > 0) {
                log.add(house.getName() + " sent " + sentManpower + " manpower to player.");
            }
        }

        return log;
    }

    /** Sum of base gold production for all zones a house controls. */
    private int houseZoneGold(NobleHouse house) {
        // We use the zone gold constants directly via zone IDs.
        // Village=3, Town=10, Capital=12 — looked up by settlement type would
        // require ZoneManager; for now we use a flat per-zone constant so
        // NobleHouseManager stays decoupled. Wire ZoneManager if richer lookup needed.
        return house.getZoneIds().size() * GameParameters.NOBLE_ZONE_GOLD_PER_TURN;
    }

    // ─── Accessors ───────────────────────────────────────────────────────────

    public List<NobleHouse> getHouses() { return Collections.unmodifiableList(houses); }

    public NobleHouse getHouseById(String id) {
        return houses.stream().filter(h -> h.getId().equals(id)).findFirst().orElse(null);
    }

    /** Returns the house that owns the given zone, or null if unowned. */
    public NobleHouse getOwnerOfZone(String zoneId) {
        for (NobleHouse house : houses) {
            if (house.getZoneIds().contains(zoneId)) return house;
        }
        return null;
    }

    public void reset() {
        houses.clear();
        buildHouses();
    }

    // ─── House definitions ───────────────────────────────────────────────────

    private void buildHouses() {

        // 4-zone houses
        houses.add(new NobleHouse(
            "house_valdris", "House Valdris", "Lord Edaran Valdris",
            "Silver-tongued and patient. Has waited twenty years for his moment and shows no impatience whatsoever.",
            NobleHouse.Race.HUMAN,
            List.of("northern_vale", "greenvale", "westgate"),
            120
        ));

        houses.add(new NobleHouse(
            "house_thornmere", "House Thornmere", "Lady Serafin Thornmere",
            "Cold pragmatist. Commands loyalty through fear dressed as respect. Never raises her voice.",
            NobleHouse.Race.ELF,
            List.of("iceveil_tundra", "far_north", "snowmarch", "frostpeak_pass"),
            100
        ));

        // 3-zone houses
        houses.add(new NobleHouse(
            "house_ashkar", "House Ashkar", "Warlord Duvrak Ashkar",
            "Blunt, honourable, and deeply suspicious of paperwork. Respects deeds over words.",
            NobleHouse.Race.ORC,
            List.of("eastern_plains", "ashfield", "highland_gap"),
            80
        ));

        houses.add(new NobleHouse(
            "house_deepvein", "House Deepvein", "Thane Hurga Deepvein",
            "Meticulous record-keeper. Every favour is logged. Every debt collected — eventually.",
            NobleHouse.Race.DWARF,
            List.of("stonepass", "trade_coast", "far_east"),
            90
        ));

        houses.add(new NobleHouse(
            "house_crestfall", "House Crestfall", "Lord Aldric Crestfall",
            "Jovial on the surface. Underneath: a careful chess player who has never lost a long game.",
            NobleHouse.Race.HUMAN,
            List.of("river_bend", "southern_march", "ironhaven"),
            70
        ));

        houses.add(new NobleHouse(
            "house_sylvaine", "House Sylvaine", "Archon Thessiel Sylvaine",
            "Ethereal and unreadable. Speaks in half-sentences that somehow communicate everything.",
            NobleHouse.Race.ELF,
            List.of("thornwood", "bramblewood", "redcliff"),
            85
        ));

        // 2-zone houses
        houses.add(new NobleHouse(
            "house_duskmantle", "House Duskmantle", "Baron Orryn Duskmantle",
            "Paranoid and brilliant. Has contingency plans for his contingency plans.",
            NobleHouse.Race.HUMAN,
            List.of("duskfall", "redcliff"),
            60
        ));

        houses.add(new NobleHouse(
            "house_saltborn", "House Saltborn", "Admiral Vessa Saltborn",
            "Weathered sailor turned noble. Respects directness. Despises ceremony.",
            NobleHouse.Race.HUMAN,
            List.of("saltmere", "port_reach"),
            75
        ));

        houses.add(new NobleHouse(
            "house_emberveil", "House Emberveil", "Matriarch Ysolde Emberveil",
            "Warm and welcoming until crossed. The smile never leaves her face — even then.",
            NobleHouse.Race.ELF,
            List.of("ashenveil", "wetmarsh"),
            65
        ));
    }
}