package main.pops;

import main.politics.PolitcalView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages all Pop groups and aggregates their economic output.
 */
public class PopManager {

    private final List<Pop> pops = new ArrayList<>();

public PopManager() {
    pops.add(new Pop(PopType.HUMAN, PolitcalView.HUMAN_SUPREMACIST));
    pops.add(new Pop(PopType.DWARF, PolitcalView.WARMONGERING));
    pops.add(new Pop(PopType.ORC,   PolitcalView.WARMONGERING));
    pops.add(new Pop(PopType.ELF,   PolitcalView.ENVIRONMENTALIST));
    // Initialise starting view intensities based on affiliation
    for (Pop pop : pops) {
        if (pop.getAffiliation() != PolitcalView.NONE) {
            pop.getElectoralData().setViewIntensity(pop.getAffiliation(), 60);
        }
        // Secondary views (weaker)
        initSecondaryViews(pop);
    }
}

private void initSecondaryViews(Pop pop) {
    // Give each pop type some baseline secondary views at low intensity
    switch (pop.getType()) {
        case HUMAN -> {
            pop.getElectoralData().setViewIntensity(PolitcalView.TRADITIONALIST, 30);
            pop.getElectoralData().setViewIntensity(PolitcalView.MERCANTILE,     20);
        }
        case DWARF -> {
            pop.getElectoralData().setViewIntensity(PolitcalView.MERCANTILE,     40);
            pop.getElectoralData().setViewIntensity(PolitcalView.ISOLATIONIST,   30);
        }
        case ORC -> {
            pop.getElectoralData().setViewIntensity(PolitcalView.MILITARIST,     30);
        }
        case ELF -> {
            pop.getElectoralData().setViewIntensity(PolitcalView.ARCANE,         35);
            pop.getElectoralData().setViewIntensity(PolitcalView.DEMOCRATIC,     25);
        }
    }
}

// ─── Aggregate totals ────────────────────────────────────────────────────

    public int getTotalFoodConsumption() {
        int total = 0;
        for (Pop pop : pops) total += pop.getFoodConsumption();
        return total;
    }

    public int getTotalMoneyGeneration() {
        int total = 0;
        for (Pop pop : pops) total += pop.getMoneyGeneration();
        return total;
    }

    public int getTotalInfluenceGeneration() {
        int total = 0;
        for (Pop pop : pops) total += pop.getInfluenceGeneration();
        return total;
    }

    public int getTotalManpower() {
        int total = 0;
        for (Pop pop : pops) total += pop.getManpowerContribution();
        return total;
    }

    public int getTotalPopulation() {
        int total = 0;
        for (Pop pop : pops) total += pop.getCount();
        return total;
    }

    // ─── Access ───────────────────────────────────────────────────────────────

    public void reset() {
        for (Pop pop : pops) {
            pop.setCount(pop.getType().getStartingCount());
        }
    }

    

    public List<Pop> getPops() {
        return Collections.unmodifiableList(pops);
    }

    public Pop getPopByType(PopType type) {
        for (Pop pop : pops) {
            if (pop.getType() == type) return pop;
        }
        return null;
    }
}