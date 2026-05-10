package main.core;

import main.actions.ActionRegistry;
import main.calendar.GameCalendar;
import main.effects.EffectManager;
import main.nobles.NobleHouseManager;
import main.parameters.GameParameters;
import main.pops.PopManager;
import main.resources.ResourcePool;
import main.resources.StatBlock;

import java.util.ArrayList;
import java.util.List;

public class TurnProcessor {

public List<String> processTurn(
            GameState          gameState,
            ResourcePool       resources,
            StatBlock          stats,
            PopManager         popManager,
            GameCalendar       calendar,
            ActionRegistry     actionRegistry,
            EffectManager      effectManager,
            NobleHouseManager  nobleHouseManager) {

        List<String> log = new ArrayList<>();

        applyPopEconomics(resources, popManager, log);
        log.addAll(nobleHouseManager.processTurn(resources));
        applyStatDecay(stats, log);
        log.addAll(effectManager.processTurn(stats));
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
        resources.addInfluence(influenceGained + GameParameters.BASE_INFLUENCE_PER_TURN);
        resources.addFood(-foodConsumed);

        log.add("Pops generated " + moneyGained + " money, " + (influenceGained + GameParameters.BASE_INFLUENCE_PER_TURN) + " influence.");
        log.add("Pops consumed " + foodConsumed + " food.");
    }

    private void applyStatDecay(StatBlock stats, List<String> log) {
        stats.reduceHappiness(GameParameters.HAPPINESS_DECAY_PER_TURN);
        stats.reduceCorruption(GameParameters.CORRUPTION_DECAY_PER_TURN);
        log.add("Happiness -" + GameParameters.HAPPINESS_DECAY_PER_TURN
            + ", Corruption -" + GameParameters.CORRUPTION_DECAY_PER_TURN + " (natural decay).");
    }
}