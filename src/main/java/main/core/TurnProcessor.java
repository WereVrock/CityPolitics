package main.core;

import main.actions.ActionRegistry;
import main.barbarians.BarbArmy;
import main.barbarians.BarbInvasionProcessor;
import main.calendar.GameCalendar;
import main.core.GameState;
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
    private Runnable             onSnapshotRequested;

    public void setPayOffDialogSupplier(PayOffDialogSupplier supplier) {
        this.payOffDialogSupplier = supplier;
    }

    public void setOnSnapshotRequested(Runnable callback) {
        this.onSnapshotRequested = callback;
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
        if (onSnapshotRequested != null) onSnapshotRequested.run();
        ledger.clearOneTime();

        int foodBefore = resources.getFood();
        int goldBefore = resources.getMoney();
        int manBefore  = resources.getManpower();

        Debug.log("turn", "cycle", calendar.getDisplayString());

        applyPopEconomics(popManager, log, ledger);
        log.addAll(nobleHouseManager.processTurn(resources, ledger));
        applyStatDecay(stats, log, ledger);
        log.addAll(effectManager.processTurn(stats));

        // ── Commander pool turn reset ─────────────────────────────────────────
        gameState.getCommanderRecruitPool().newTurnRefresh();
        Debug.log("turn", "commander-pool", "Recruit pool refreshed.");

        // ── Commander upkeep & party power ────────────────────────────────────
        main.army.CommanderRoster roster = gameState.getCommanderRoster();
        log.addAll(roster.processTurnUpkeep());
        roster.applyPartyPowerContributions(gameState.getPartyManager());

        // ── Commander gold upkeep via ledger ──────────────────────────────────
        double cmdGoldUpkeep = roster.getTotalGoldUpkeep();
        if (cmdGoldUpkeep > 0) {
            int cmdGoldCeil = (int) Math.ceil(cmdGoldUpkeep);
            ledger.setRecurring(main.resources.ResourceType.GOLD,
                    "commanders", "Commander Upkeep", -cmdGoldCeil);
            log.add("Commander upkeep: -" + cmdGoldCeil + " gold.");
        }

        // ── Soldier upkeep — prompt each army ────────────────────────────────
        log.addAll(processSoldierUpkeep(gameState, resources));

        applyLedgerToResources(ledger, resources);

        // ── Player fight phase ────────────────────────────────────────────────
        main.army.PlayerCombatProcessor pcp = gameState.getPlayerCombatProcessor();
        log.addAll(pcp.processTurn(
                gameState.getArmyManager(),
                gameState.getBarbArmyManager(),
                nobleHouseManager,
                gameState.getZoneManager(),
                gameState.getRavagedZoneManager(),
                nobleHouseManager.getClaimManager()));

        // ── Barbarian phase ───────────────────────────────────────────────────
        BarbInvasionProcessor barbProcessor = gameState.getBarbInvasionProcessor();
        barbProcessor.setPayOffCallback((army, res, zoneId, owner, playerArmies, nobleArmies, nobleGarrison) -> {
            if (payOffDialogSupplier == null) return false;
            return payOffDialogSupplier.ask(army, res, zoneId, owner, playerArmies, nobleArmies, nobleGarrison);
        });
        barbProcessor.setGameOverCallback(reason -> triggerGameOver(gameState, reason));
        log.addAll(barbProcessor.processTurn(calendar, resources));

        calendar.advance();
        actionRegistry.resetAllActions();
        Debug.log("economy", "delta", "Food: " + foodBefore + " → " + resources.getFood()
                + " (Δ " + (resources.getFood() - foodBefore) + ")");
        Debug.log("economy", "delta", "Gold: " + goldBefore + " → " + resources.getMoney()
                + " (Δ " + (resources.getMoney() - goldBefore) + ")");
        Debug.log("economy", "delta", "Manpower: " + manBefore + " → " + resources.getManpower()
                + " (Δ " + (resources.getManpower() - manBefore) + ")");

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

/**
     * For each deployed army, computes soldier upkeep and auto-pays it.
     * If the army cannot afford upkeep, desertion fires.
     * Uses SoldierUpkeepProcessor so the logic stays in one place.
     * Upkeep is recorded as a one-time ledger expense per army.
     */

private List<String> processSoldierUpkeep(GameState gameState, ResourcePool resources) {
        List<String> log = new ArrayList<>();
        main.ledger.Ledger ledger = gameState.getLedger();
        for (main.army.Army army : gameState.getArmyManager().getArmies()) {
            if (army.getSize() <= 0) continue;
            main.army.SoldierUpkeepProcessor sup =
                    new main.army.SoldierUpkeepProcessor(resources, army);
            double cost = sup.computeUpkeepCost();
            if (cost <= 0) continue;
            boolean paid = sup.payUpkeep();
            if (paid) {
                int costCeil = (int) Math.ceil(cost);
                ledger.logOneTime(main.resources.ResourceType.GOLD,
                        "soldiers", "Soldier Upkeep (" + army.getDisplayName() + ")",
                        -costCeil);
                Debug.log("soldier-upkeep", "paid",
                        army.getDisplayName() + " cost=" + costCeil);
            } else {
                int lost = sup.processDesertion();
                log.add("⚠ " + army.getDisplayName()
                        + " could not pay upkeep — " + lost + " soldiers deserted.");
                Debug.log("soldier-upkeep", "desertion",
                        army.getDisplayName() + " lost=" + lost);
            }
        }
        return log;
    }

// ─── Private helpers ─────────────────────────────────────────────────────

private void applyPopEconomics(PopManager popManager,
                                    List<String> log, main.ledger.Ledger ledger) {
        int moneyGained     = popManager.getTotalMoneyGeneration();
        int influenceGained = popManager.getTotalInfluenceGeneration() + GameParameters.BASE_INFLUENCE_PER_TURN;
        int foodConsumed    = popManager.getTotalFoodConsumption();

        ledger.setRecurring(main.resources.ResourceType.GOLD,      "pops", "Pop Income",      moneyGained);
        ledger.setRecurring(main.resources.ResourceType.INFLUENCE,  "pops", "Pop Influence",   influenceGained);
        ledger.setRecurring(main.resources.ResourceType.FOOD,       "pops", "Pop Consumption", -foodConsumed);

        log.add("Pops generated " + moneyGained + " money, " + influenceGained + " influence.");
        log.add("Pops consumed " + foodConsumed + " food.");
    }

private void applyStatDecay(StatBlock stats, List<String> log,
                                 main.ledger.Ledger ledger) {
        stats.reduceHappiness(GameParameters.HAPPINESS_DECAY_PER_TURN);
        stats.reduceCorruption(GameParameters.CORRUPTION_DECAY_PER_TURN);
        log.add("Happiness -" + GameParameters.HAPPINESS_DECAY_PER_TURN
                + ", Corruption -" + GameParameters.CORRUPTION_DECAY_PER_TURN
                + " (natural decay).");
    }

private void applyLedgerToResources(main.ledger.Ledger ledger, ResourcePool resources) {
        int deltaGold      = ledger.getDelta(main.resources.ResourceType.GOLD);
        int deltaFood      = ledger.getDelta(main.resources.ResourceType.FOOD);
        int deltaManpower  = ledger.getDelta(main.resources.ResourceType.MANPOWER);
        int deltaInfluence = ledger.getDelta(main.resources.ResourceType.INFLUENCE);

        resources.addMoney(deltaGold);
        resources.addFood(deltaFood);
        resources.addManpower(deltaManpower);
        resources.addInfluence(deltaInfluence);

        Debug.log("ledger", "apply",
                "Applied ledger — gold:" + deltaGold + " food:" + deltaFood
                + " manpower:" + deltaManpower + " influence:" + deltaInfluence);
    }

}