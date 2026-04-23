package main.core;

import main.actions.ActionRegistry;
import main.calendar.GameCalendar;
import main.parameters.GameParameters;
import main.pops.PopManager;
import main.resources.ResourcePool;
import main.resources.StatBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies all end-of-turn passive effects in order:
 *   1. Pop income and consumption
 *   2. Stat decay
 *   3. Calendar advance
 *   4. Action reset
 *
 * Returns a log of what happened for the UI to display.
 */
public class TurnProcessor {

    public List<String> processTurn(
            ResourcePool resources,
            StatBlock stats,
            PopManager popManager,
            GameCalendar calendar,
            ActionRegistry actionRegistry) {

        List<String> log = new ArrayList<>();

        applyPopEconomics(resources, popManager, log);
        applyStatDecay(stats, log);
        calendar.advance();
        actionRegistry.resetAllActions();
        log.add("--- " + calendar.getDisplayString() + " begins ---");

        return log;
    }

    private void applyPopEconomics(ResourcePool resources, PopManager popManager, List<String> log) {
        int moneyGained     = popManager.getTotalMoneyGeneration();
        int influenceGained = popManager.getTotalInfluenceGeneration();
        int foodConsumed    = popManager.getTotalFoodConsumption();

        resources.addMoney(moneyGained);
        resources.addInfluence(influenceGained);
        resources.addFood(-foodConsumed);

        log.add("Pops generated " + moneyGained + " money, " + influenceGained + " influence.");
        log.add("Pops consumed " + foodConsumed + " food.");
    }

    private void applyStatDecay(StatBlock stats, List<String> log) {
        stats.reduceHappiness(GameParameters.HAPPINESS_DECAY_PER_TURN);
        stats.reduceCorruption(GameParameters.CORRUPTION_DECAY_PER_TURN);
        log.add("Happiness -" + GameParameters.HAPPINESS_DECAY_PER_TURN
            + ", Corruption -" + GameParameters.CORRUPTION_DECAY_PER_TURN + " (natural decay).");
    }
}