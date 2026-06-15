package City.main.politics;

import City.main.pops.Pop;
import City.main.pops.PopManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PartyManager {

    private final List<PoliticalParty> parties = new ArrayList<>();

    private final PoliticalParty knightsOfRuan;
    private final PoliticalParty dwarvenFront;
    private final PoliticalParty unitedAxes;
    private final PoliticalParty archivists;
    private final PoliticalParty merchantUnion;
    private final PoliticalParty democrats;
    private final PoliticalParty oracles;
    private final PoliticalParty nobleHouses;

    public PartyManager(PopManager popManager) {
        knightsOfRuan = buildKnightsOfRuan();
        dwarvenFront  = buildDwarvenFront();
        unitedAxes    = buildUnitedAxes();
        archivists    = buildArchivists();
        merchantUnion = buildMerchantUnion();
        democrats     = buildDemocrats();
        oracles       = buildOracles();
        nobleHouses   = buildNobleHouses();

        parties.add(knightsOfRuan);
        parties.add(dwarvenFront);
        parties.add(unitedAxes);
        parties.add(archivists);
        parties.add(merchantUnion);
        parties.add(democrats);
        parties.add(oracles);
        parties.add(nobleHouses);

        wirePopReferences(popManager);
    }

    private PoliticalParty buildNobleHouses() {
        PoliticalParty p = new PoliticalParty(
                NoblePartyVoteManager.NOBLE_PARTY_NAME,
                NoblePartyVoteManager.NOBLE_PARTY_SEATS,
                50, 50, 50,
                "The Noble Council",
                "The great houses of the realm. Their vote is determined by internal deliberation among the five most prestigious houses. They vote unanimously based on collective opinion.",
                List.of());
        p.setUnelected(true);
        p.setNoNegotiation(true);
        return p;
    }

    private PoliticalParty buildKnightsOfRuan() {
        List<SideLeader> sides = List.of(
            new SideLeader("Captain Edric Hale", "Loyal but ambitious. Quietly questions Voss's methods."),
            new SideLeader("Dame Isolde Crane",  "Old blood nobility. Polite on the surface, ruthless underneath.")
        );
        PoliticalParty p = new PoliticalParty("Knights of Ruan", 12, 50, 55, 65,
            "Commander Aldric Voss",
            "Proud and unyielding. Speaks in clipped military cadence. Believes human supremacy is destiny, not bigotry.",
            sides);
        p.setView(PolitcalView.HUMAN_SUPREMACIST,  ViewStrength.STRONGLY_FOR);
        p.setView(PolitcalView.MILITARIST,         ViewStrength.STRONGLY_FOR);
        p.setView(PolitcalView.TRADITIONALIST,     ViewStrength.FOR);
        p.setView(PolitcalView.DEMOCRATIC,         ViewStrength.AGAINST);
        p.setView(PolitcalView.ENVIRONMENTALIST,   ViewStrength.AGAINST);
        p.setView(PolitcalView.MERCANTILE,         ViewStrength.NEUTRAL);
        return p;
    }

    private PoliticalParty buildDwarvenFront() {
        List<SideLeader> sides = List.of(
            new SideLeader("Elder Marta Coalvein",  "Practical and tired. Wants stability above ideology."),
            new SideLeader("Foreman Durg Blackpick","Represents the miners. Blunt, suspicious of politics.")
        );
        PoliticalParty p = new PoliticalParty("Dwarven Front", 8, 50, 50, 55,
            "Thane Brokk Stonehammer",
            "Gruff and transactional. Every conversation is a negotiation. Deeply suspicious of outsiders.",
            sides);
        p.setView(PolitcalView.MERCANTILE,         ViewStrength.STRONGLY_FOR);
        p.setView(PolitcalView.ISOLATIONIST,       ViewStrength.STRONGLY_FOR);
        p.setView(PolitcalView.TRADITIONALIST,     ViewStrength.FOR);
        p.setView(PolitcalView.DEMOCRATIC,         ViewStrength.NEUTRAL);
        p.setView(PolitcalView.MILITARIST,         ViewStrength.FOR);
        p.setView(PolitcalView.ENVIRONMENTALIST,   ViewStrength.AGAINST);
        return p;
    }

    private PoliticalParty buildUnitedAxes() {
        List<SideLeader> sides = List.of(
            new SideLeader("Champion Rulf Ashenmaw","Hothead who respects power."),
            new SideLeader("Sergeant Brynn Tusk",   "Pragmatic veteran. Wants peace but won't say so.")
        );
        PoliticalParty p = new PoliticalParty("United Axes", 10, 50, 50, 60,
            "Warchief Gorra Ironblood",
            "Passionate and loud. Sees every vote as a battle. Respects strength above all else.",
            sides);
        p.setView(PolitcalView.WARMONGERING,       ViewStrength.STRONGLY_FOR);
        p.setView(PolitcalView.MILITARIST,         ViewStrength.STRONGLY_FOR);
        p.setView(PolitcalView.DEMOCRATIC,         ViewStrength.AGAINST);
        p.setView(PolitcalView.MERCANTILE,         ViewStrength.NEUTRAL);
        p.setView(PolitcalView.ENVIRONMENTALIST,   ViewStrength.AGAINST);
        p.setView(PolitcalView.ISOLATIONIST,       ViewStrength.FOR);
        return p;
    }

    private PoliticalParty buildArchivists() {
        List<SideLeader> sides = List.of(
            new SideLeader("Keeper Aldous Vrime", "Young and idealistic for an Archivist."),
            new SideLeader("Scribe Fennel Dusk",  "Obsessed with procedure.")
        );
        PoliticalParty p = new PoliticalParty("Archivists", 9, 50, 50, 50,
            "Grand Scribe Elowen Ashveil",
            "Precise and aloof. Quotes historical precedent constantly. Dislikes passion in politics.",
            sides);
        p.setView(PolitcalView.ARCANE,             ViewStrength.STRONGLY_FOR);
        p.setView(PolitcalView.DEMOCRATIC,         ViewStrength.FOR);
        p.setView(PolitcalView.ENVIRONMENTALIST,   ViewStrength.FOR);
        p.setView(PolitcalView.TRADITIONALIST,     ViewStrength.FOR);
        p.setView(PolitcalView.MILITARIST,         ViewStrength.AGAINST);
        p.setView(PolitcalView.WARMONGERING,       ViewStrength.STRONGLY_AGAINST);
        return p;
    }

    private PoliticalParty buildMerchantUnion() {
        List<SideLeader> sides = List.of(
            new SideLeader("Factor Dorin Pale",   "Handles the books. Cares only about margins."),
            new SideLeader("Broker Liessa Wynn",  "Specialises in influence trading. Dangerous but useful.")
        );
        PoliticalParty p = new PoliticalParty("Merchant Union", 10, 50, 50, 55,
            "Guildmaster Sera Vantis",
            "Charming and calculating. Always smiling. Weighs everything in coin.",
            sides);
        p.setView(PolitcalView.MERCANTILE,         ViewStrength.STRONGLY_FOR);
        p.setView(PolitcalView.DEMOCRATIC,         ViewStrength.FOR);
        p.setView(PolitcalView.ISOLATIONIST,       ViewStrength.AGAINST);
        p.setView(PolitcalView.MILITARIST,         ViewStrength.AGAINST);
        p.setView(PolitcalView.WARMONGERING,       ViewStrength.STRONGLY_AGAINST);
        p.setView(PolitcalView.ENVIRONMENTALIST,   ViewStrength.NEUTRAL);
        return p;
    }

    private PoliticalParty buildDemocrats() {
        List<SideLeader> sides = List.of(
            new SideLeader("Councillor Fen Marsh",  "Grassroots organiser. Distrusts the elite but pragmatic."),
            new SideLeader("Advocate Sable Reyne",  "Fiery and principled.")
        );
        PoliticalParty p = new PoliticalParty("Democrats", 6, 50, 60, 40,
            "Speaker Mira Dawnhollow",
            "Idealistic and earnest. Speaks for the common people. Easily moved by appeals to justice.",
            sides);
        p.setView(PolitcalView.DEMOCRATIC,         ViewStrength.STRONGLY_FOR);
        p.setView(PolitcalView.ENVIRONMENTALIST,   ViewStrength.STRONGLY_FOR);
        p.setView(PolitcalView.MERCANTILE,         ViewStrength.NEUTRAL);
        p.setView(PolitcalView.MILITARIST,         ViewStrength.AGAINST);
        p.setView(PolitcalView.WARMONGERING,       ViewStrength.STRONGLY_AGAINST);
        p.setView(PolitcalView.HUMAN_SUPREMACIST,  ViewStrength.STRONGLY_AGAINST);
        return p;
    }

    private PoliticalParty buildOracles() {
        List<SideLeader> sides = List.of(
            new SideLeader("Seer Voss Pale",   "Young oracle, sharp minded."),
            new SideLeader("Oracle Maren Dusk","Ancient even by oracle standards.")
        );
        PoliticalParty p = new PoliticalParty("Oracles", 4, 100, 80, 20,
            "Arch Oracle Thessivane",
            "Ancient and half-senile. Speaks in slow, wandering sentences. Deeply fond of the player.",
            sides);
        p.setView(PolitcalView.ARCANE,         ViewStrength.STRONGLY_FOR);
        p.setView(PolitcalView.TRADITIONALIST, ViewStrength.FOR);
        p.setUnelected(true);
        return p;
    }

    private void wirePopReferences(PopManager popManager) {
        for (Pop pop : popManager.getPops()) {
            PoliticalParty party = getPartyForPop(pop);
            if (party != null) party.addMemberPop(pop);
        }
    }

    private PoliticalParty getPartyForPop(Pop pop) {
        return switch (pop.getAffiliation()) {
            case HUMAN_SUPREMACIST -> knightsOfRuan;
            case WARMONGERING      -> unitedAxes;
            case ENVIRONMENTALIST  -> democrats;
            case ARCANE            -> archivists;
            case MERCANTILE        -> merchantUnion;
            case ISOLATIONIST      -> dwarvenFront;
            default                -> null;
        };
    }

    public List<PoliticalParty> getParties()    { return Collections.unmodifiableList(parties); }
    public PoliticalParty       getOracles()    { return oracles; }
    public PoliticalParty       getNobleHouses(){ return nobleHouses; }

    public void adjustOpinion(PolitcalView affiliation, int delta) {
        for (PoliticalParty party : parties) {
            if (party.getViews().containsKey(affiliation)
                    && party.getViewStrength(affiliation).getMultiplier() > 0) {
                party.adjustPlayerOpinion(delta);
                return;
            }
        }
    }

    public void addPrestige(PolitcalView affiliation, int amount) {
        PoliticalParty best = null;
        double bestStrength = Double.NEGATIVE_INFINITY;
        for (PoliticalParty party : parties) {
            double m = party.getViewStrength(affiliation).getMultiplier();
            if (m > bestStrength) { bestStrength = m; best = party; }
        }
        if (best != null) best.addPrestige(amount);
    }

    public void reset() {
        for (PoliticalParty party : parties) {
            party.setPlayerOpinion(party == oracles ? 100 : 50);
            party.setPublicOpinion(50);
            party.setPower(50);
            party.setFavour(0);
        }
    }
}