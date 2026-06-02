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
import debug.Debug;
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

        main.ledger.Ledger ledger = gameState.getLedger();
        ledger.clearOneTime();

        int foodBefore = resources.getFood();
        int goldBefore = resources.getMoney();
        int manBefore  = resources.getManpower();

        debug.Debug.log("turn", "cycle", calendar.getDisplayString());

        applyPopEconomics(resources, popManager, log, ledger);
        log.addAll(nobleHouseManager.processTurn(resources, ledger));
        applyStatDecay(stats, log, ledger);
        log.addAll(effectManager.processTurn(stats));

        BarbInvasionProcessor barbProcessor = gameState.getBarbInvasionProcessor();
        barbProcessor.setPayOffCallback((army, res, zoneId, owner, playerArmies, nobleArmies, nobleGarrison) -> {
            if (payOffDialogSupplier == null) return false;
            return payOffDialogSupplier.ask(army, res, zoneId, owner, playerArmies, nobleArmies, nobleGarrison);
        });
        barbProcessor.setGameOverCallback(reason -> triggerGameOver(gameState, reason));
        log.addAll(barbProcessor.processTurn(calendar, resources));

        calendar.advance();
        actionRegistry.resetAllActions();
        Debug.log("economy", "delta", "Food: " + foodBefore + " → " + resources.getFood() + " (Δ " + (resources.getFood() - foodBefore) + ")");
        Debug.log("economy", "delta", "Gold: " + goldBefore + " → " + resources.getMoney() + " (Δ " + (resources.getMoney() - goldBefore) + ")");
        Debug.log("economy", "delta", "Manpower: " + manBefore + " → " + resources.getManpower() + " (Δ " + (resources.getManpower() - manBefore) + ")");

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
                                    List<String> log, main.ledger.Ledger ledger) {
        int moneyGained     = popManager.getTotalMoneyGeneration();
        int influenceGained = popManager.getTotalInfluenceGeneration() + GameParameters.BASE_INFLUENCE_PER_TURN;
        int foodConsumed    = popManager.getTotalFoodConsumption();

        ledger.setRecurring(main.resources.ResourceType.GOLD,      "pops", "Pop Income",      moneyGained);
        ledger.setRecurring(main.resources.ResourceType.INFLUENCE,  "pops", "Pop Influence",   influenceGained);
        ledger.setRecurring(main.resources.ResourceType.FOOD,       "pops", "Pop Consumption", -foodConsumed);

        resources.addMoney(moneyGained);
        resources.addInfluence(influenceGained);
        resources.addFood(-foodConsumed);

        log.add("Pops generated " + moneyGained + " money, " + influenceGained + " influence.");
        log.add("Pops consumed " + foodConsumed + " food.");
    }

private void applyStatDecay(StatBlock stats, List<String> log,
                                 main.ledger.Ledger ledger) {
        ledger.setRecurring(main.resources.ResourceType.GOLD,      "decay", "Happiness Decay", 0);
        ledger.setRecurring(main.resources.ResourceType.INFLUENCE,  "decay", "Base Influence",  GameParameters.BASE_INFLUENCE_PER_TURN);

        stats.reduceHappiness(GameParameters.HAPPINESS_DECAY_PER_TURN);
        stats.reduceCorruption(GameParameters.CORRUPTION_DECAY_PER_TURN);
        log.add("Happiness -" + GameParameters.HAPPINESS_DECAY_PER_TURN
                + ", Corruption -" + GameParameters.CORRUPTION_DECAY_PER_TURN
                + " (natural decay).");
    }

}