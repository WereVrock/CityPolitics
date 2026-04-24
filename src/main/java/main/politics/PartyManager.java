package main.politics;

import main.pops.Pop;
import main.pops.PopManager;
import main.pops.PopType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Creates and holds all 6 political parties.
 * Wires pop references from PopManager on construction.
 */
public class PartyManager {

    private final List<PoliticalParty> parties = new ArrayList<>();

    private final PoliticalParty knightsOfRuan;
    private final PoliticalParty dwarvenFront;
    private final PoliticalParty unitedAxes;
    private final PoliticalParty archivists;
    private final PoliticalParty merchantUnion;
    private final PoliticalParty democrats;

    public PartyManager(PopManager popManager) {
        knightsOfRuan = buildKnightsOfRuan();
        dwarvenFront  = buildDwarvenFront();
        unitedAxes    = buildUnitedAxes();
        archivists    = buildArchivists();
        merchantUnion = buildMerchantUnion();
        democrats     = buildDemocrats();

        parties.add(knightsOfRuan);
        parties.add(dwarvenFront);
        parties.add(unitedAxes);
        parties.add(archivists);
        parties.add(merchantUnion);
        parties.add(democrats);

        wirePopReferences(popManager);
    }

    // ─── Party Builders ───────────────────────────────────────────────────────

    private PoliticalParty buildKnightsOfRuan() {
        PoliticalParty p = new PoliticalParty("Knights of Ruan", 12, 50, 55, 65);
        p.setView(PolitcalView.HUMAN_SUPREMACIST,  ViewStrength.STRONGLY_FOR);
        p.setView(PolitcalView.MILITARIST,         ViewStrength.STRONGLY_FOR);
        p.setView(PolitcalView.TRADITIONALIST,     ViewStrength.FOR);
        p.setView(PolitcalView.DEMOCRATIC,         ViewStrength.AGAINST);
        p.setView(PolitcalView.ENVIRONMENTALIST,   ViewStrength.AGAINST);
        p.setView(PolitcalView.MERCANTILE,         ViewStrength.NEUTRAL);
        return p;
    }

    private PoliticalParty buildDwarvenFront() {
        PoliticalParty p = new PoliticalParty("Dwarven Front", 8, 50, 50, 55);
        p.setView(PolitcalView.MERCANTILE,         ViewStrength.STRONGLY_FOR);
        p.setView(PolitcalView.ISOLATIONIST,       ViewStrength.STRONGLY_FOR);
        p.setView(PolitcalView.TRADITIONALIST,     ViewStrength.FOR);
        p.setView(PolitcalView.DEMOCRATIC,         ViewStrength.NEUTRAL);
        p.setView(PolitcalView.MILITARIST,         ViewStrength.FOR);
        p.setView(PolitcalView.ENVIRONMENTALIST,   ViewStrength.AGAINST);
        return p;
    }

    private PoliticalParty buildUnitedAxes() {
        PoliticalParty p = new PoliticalParty("United Axes", 10, 50, 50, 60);
        p.setView(PolitcalView.WARMONGERING,       ViewStrength.STRONGLY_FOR);
        p.setView(PolitcalView.MILITARIST,         ViewStrength.STRONGLY_FOR);
        p.setView(PolitcalView.DEMOCRATIC,         ViewStrength.AGAINST);
        p.setView(PolitcalView.MERCANTILE,         ViewStrength.NEUTRAL);
        p.setView(PolitcalView.ENVIRONMENTALIST,   ViewStrength.AGAINST);
        p.setView(PolitcalView.ISOLATIONIST,       ViewStrength.FOR);
        return p;
    }

    private PoliticalParty buildArchivists() {
        PoliticalParty p = new PoliticalParty("Archivists", 9, 50, 50, 50);
        p.setView(PolitcalView.ARCANE,             ViewStrength.STRONGLY_FOR);
        p.setView(PolitcalView.DEMOCRATIC,         ViewStrength.FOR);
        p.setView(PolitcalView.ENVIRONMENTALIST,   ViewStrength.FOR);
        p.setView(PolitcalView.TRADITIONALIST,     ViewStrength.FOR);
        p.setView(PolitcalView.MILITARIST,         ViewStrength.AGAINST);
        p.setView(PolitcalView.WARMONGERING,       ViewStrength.STRONGLY_AGAINST);
        return p;
    }

    private PoliticalParty buildMerchantUnion() {
        PoliticalParty p = new PoliticalParty("Merchant Union", 10, 50, 50, 55);
        p.setView(PolitcalView.MERCANTILE,         ViewStrength.STRONGLY_FOR);
        p.setView(PolitcalView.DEMOCRATIC,         ViewStrength.FOR);
        p.setView(PolitcalView.ISOLATIONIST,       ViewStrength.AGAINST);
        p.setView(PolitcalView.MILITARIST,         ViewStrength.AGAINST);
        p.setView(PolitcalView.WARMONGERING,       ViewStrength.STRONGLY_AGAINST);
        p.setView(PolitcalView.ENVIRONMENTALIST,   ViewStrength.NEUTRAL);
        return p;
    }

    private PoliticalParty buildDemocrats() {
        PoliticalParty p = new PoliticalParty("Democrats", 6, 50, 60, 40);
        p.setView(PolitcalView.DEMOCRATIC,         ViewStrength.STRONGLY_FOR);
        p.setView(PolitcalView.ENVIRONMENTALIST,   ViewStrength.STRONGLY_FOR);
        p.setView(PolitcalView.MERCANTILE,         ViewStrength.NEUTRAL);
        p.setView(PolitcalView.MILITARIST,         ViewStrength.AGAINST);
        p.setView(PolitcalView.WARMONGERING,       ViewStrength.STRONGLY_AGAINST);
        p.setView(PolitcalView.HUMAN_SUPREMACIST,  ViewStrength.STRONGLY_AGAINST);
        return p;
    }

    // ─── Pop Wiring ──────────────────────────────────────────────────────────

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

    // ─── Access ───────────────────────────────────────────────────────────────

    public List<PoliticalParty> getParties() {
        return Collections.unmodifiableList(parties);
    }

    public void reset() {
        for (PoliticalParty party : parties) {
            party.setPlayerOpinion(50);
            party.setPublicOpinion(50);
            party.setPower(50);
        }
    }
}