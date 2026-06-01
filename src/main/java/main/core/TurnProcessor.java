package main.core;

import main.actions.ActionRegistry;
import main.barbarians.BarbArmy;
import main.barbarians.BarbInvasionProcessor;
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

    /**
     * Swing-side pay-off dialog supplier.
     * Set by MainWindow so TurnProcessor stays free of UI imports.
     */
    public interface PayOffDialogSupplier {
        boolean ask(BarbArmy army, ResourcePool resources,
                    String zoneId, main.nobles.NobleHouse owner,
                    java.util.List<main.army.Army> playerArmies,
                    java.util.List<main.nobles.NobleArmy> nobleArmies,
                    int nobleGarrison);
    }

    private PayOffDialogSupplier payOffDialogSupplier;

    public void setPayOffDialogSupplier(PayOffDialogSupplier supplier) {
        this.payOffDialogSupplier = supplier;
    }

    public List<String> processTurn(
            GameState         gameState,
            ResourcePool      resources,
            StatBlock         stats,
            PopManager        popManager,
            GameCalendar      calendar,
            ActionRegistry    actionRegistry,
            EffectManager     effectManager,
            NobleHouseManager nobleHouseManager) {

        List<String> log = new ArrayList<>();

        debug.Debug.log("turn", "cycle", calendar.getDisplayString());

        applyPopEconomics(resources, popManager, log);
        log.addAll(nobleHouseManager.processTurn(resources));
        applyStatDecay(stats, log);
        log.addAll(effectManager.processTurn(stats));

        // Barbarian invasion — wire pay-off callback then process
        BarbInvasionProcessor barbProcessor = gameState.getBarbInvasionProcessor();
        barbProcessor.setPayOffCallback((army, res, zoneId, owner, playerArmies, nobleArmies, nobleGarrison) -> {
            if (payOffDialogSupplier == null) return false;
            return payOffDialogSupplier.ask(army, res, zoneId, owner, playerArmies, nobleArmies, nobleGarrison);
        });
        barbProcessor.setGameOverCallback(reason -> triggerGameOver(gameState, reason));
        log.addAll(barbProcessor.processTurn(calendar, resources));

        calendar.advance();
        actionRegistry.resetAllActions();
        log.add("--- " + calendar.getDisplayString() + " begins ---");

        return log;
    }

    // ─── Game over ───────────────────────────────────────────────────────────

    /**
     * Central game-over handler. Swap implementation later without refactor.
     */
    private void triggerGameOver(GameState gameState, String reason) {
        // Fired on the game-loop thread — delegate to EDT via invokeLater
        javax.swing.SwingUtilities.invokeLater(() -> {
            javax.swing.JOptionPane.showMessageDialog(
                    null,
                    "GAME OVER\n\n" + reason,
                    "The Realm Falls",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        });
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private void applyPopEconomics(ResourcePool resources, PopManager popManager,
                                    List<String> log) {
        int moneyGained     = popManager.getTotalMoneyGeneration();
        int influenceGained = popManager.getTotalInfluenceGeneration();
        int foodConsumed    = popManager.getTotalFoodConsumption();

        resources.addMoney(moneyGained);
        resources.addInfluence(influenceGained + GameParameters.BASE_INFLUENCE_PER_TURN);
        resources.addFood(-foodConsumed);

        log.add("Pops generated " + moneyGained + " money, "
                + (influenceGained + GameParameters.BASE_INFLUENCE_PER_TURN) + " influence.");
        log.add("Pops consumed " + foodConsumed + " food.");
    }

    private void applyStatDecay(StatBlock stats, List<String> log) {
        stats.reduceHappiness(GameParameters.HAPPINESS_DECAY_PER_TURN);
        stats.reduceCorruption(GameParameters.CORRUPTION_DECAY_PER_TURN);
        log.add("Happiness -" + GameParameters.HAPPINESS_DECAY_PER_TURN
                + ", Corruption -" + GameParameters.CORRUPTION_DECAY_PER_TURN
                + " (natural decay).");
    }
}