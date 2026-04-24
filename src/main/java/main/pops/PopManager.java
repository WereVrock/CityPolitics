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