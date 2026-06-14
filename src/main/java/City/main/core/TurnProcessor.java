package City.main.core;

import City.main.actions.ActionRegistry;
import City.main.barbarians.BarbArmy;
import City.main.barbarians.BarbInvasionProcessor;
import City.main.calendar.GameCalendar;
import City.main.effects.EffectManager;
import City.main.nobles.NobleHouseManager;
import City.main.parameters.GameParameters;
import City.main.pops.PopManager;
import City.main.resources.ResourcePool;
import City.debug.Debug;
import City.main.resources.StatBlock;

import java.util.ArrayList;
import java.util.List;

public class TurnProcessor {

    public interface PayOffDialogSupplier {
        boolean ask(BarbArmy army, ResourcePool resources,
                    String zoneId, City.main.nobles.NobleHouse owner,
                    java.util.List<City.main.army.Army> playerArmies,
                    java.util.List<City.main.nobles.NobleArmy> nobleArmies,
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

        City.main.ledger.Ledger ledger = gameState.getLedger();
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

        gameState.getCommanderRecruitPool().newTurnRefresh();

        City.main.army.commander.CommanderRoster roster = gameState.getCommanderRoster();
        log.addAll(roster.processTurnUpkeep());
        roster.applyPartyPowerContributions(gameState.getPartyManager());

        double cmdGoldUpkeep = roster.getTotalGoldUpkeep();
        if (cmdGoldUpkeep > 0) {
            int cmdGoldCeil = (int) Math.ceil(cmdGoldUpkeep);
            ledger.setRecurring(City.main.resources.ResourceType.GOLD,
                    "commanders", "Commander Upkeep", -cmdGoldCeil);
            log.add("Commander upkeep: -" + cmdGoldCeil + " gold.");
        }

        log.addAll(processSoldierUpkeep(gameState, resources));

        // ── Mercenary upkeep & raiding ────────────────────────────────────────
        log.addAll(gameState.getMercenaryManager().processUpkeep(resources));
        log.addAll(gameState.getMercenaryManager().processRaiding(
                gameState.getArmyManager(),
                gameState.getNobleHouseManager(),
                gameState.getZoneManager(),
                resources));

        // ── Propaganda ideology spread (before ledger apply) ─────────────────
        gameState.getPropagandaManager().processIdeologySpread(
                gameState.getPartyManager().getParties(),
                gameState.getPopManager());

        // ── Power drift ───────────────────────────────────────────────────────
        gameState.getElectionManager().applyPowerDrift(
                gameState.getPartyManager().getParties());

        // ── Council session manager turn ──────────────────────────────────────
        log.addAll(gameState.getCouncilSessionManager().processTurn(
                gameState.getNobleHouseManager(),
                resources,
                gameState.getPlayerPrestige()));

        // ── Player prestige: barb-owned zones ─────────────────────────────────
        int barbZones = 0;
        for (City.main.map.Zone z : gameState.getZoneManager().getZones()) {
            if (!z.isDesolate()
                    && !gameState.getBarbArmyManager().getGarrisonsInZone(z.getId()).isEmpty()) {
                barbZones++;
            }
        }
        if (barbZones > 0) {
            gameState.getPlayerPrestige().addPrestige(barbZones * City.main.parameters.GameParameters.PLAYER_PRESTIGE_PER_BARB_ZONE);
            if (barbZones > 0) {
                log.add("Prestige -" + (barbZones
                        * Math.abs(City.main.parameters.GameParameters.PLAYER_PRESTIGE_PER_BARB_ZONE))
                        + " (" + barbZones + " barbarian-occupied zones).");
            }
        }

        // ── Protection: check for protected house zone losses ─────────────────
        // (tracked via NobleHouseManager callback — prestige hit applied in PlayerCombatProcessor)

        // ── Election tick ─────────────────────────────────────────────────────
        gameState.getElectionManager().setCalendarContext(
                calendar.getYear(), calendar.getPeriod().getDisplayName());
        List<String> electionLog = gameState.getElectionManager().tick(
                gameState.getPartyManager().getParties(),
                gameState.getPopManager(),
                gameState.getPropagandaManager(),
                gameState.getStats().getCorruption());
        log.addAll(electionLog);

        // ── Legislation ticking ───────────────────────────────────────────────
        gameState.getLegislationManager().tickMercenaryWindow();
        gameState.getLegislationManager().tickWartimeTaxesCooldown();
        gameState.getLegislationManager().tickSendResourcesWindow();

        // ── Refresh mercenary hire pool ───────────────────────────────────────
        gameState.getMercenaryManager().getHirePool().refresh();

        applyLedgerToResources(ledger, resources);

        City.main.army.PlayerCombatProcessor pcp = gameState.getPlayerCombatProcessor();
        log.addAll(pcp.processTurn(
                gameState.getArmyManager(),
                gameState.getBarbArmyManager(),
                nobleHouseManager,
                gameState.getZoneManager(),
                gameState.getRavagedZoneManager(),
                nobleHouseManager.getClaimManager()));

        BarbInvasionProcessor barbProcessor = gameState.getBarbInvasionProcessor();
        barbProcessor.setPayOffCallback((army, res, zoneId, owner, playerArmies, nobleArmies, nobleGarrison) -> {
            if (payOffDialogSupplier == null) return false;
            return payOffDialogSupplier.ask(army, res, zoneId, owner, playerArmies, nobleArmies, nobleGarrison);
        });
        barbProcessor.setGameOverCallback(reason -> triggerGameOver(gameState, reason));
        log.addAll(barbProcessor.processTurn(calendar, resources));

        calendar.advance();
        actionRegistry.resetAllActions();
        Debug.log("economy", "delta", "Food: " + foodBefore + " → " + resources.getFood());
        Debug.log("economy", "delta", "Gold: " + goldBefore + " → " + resources.getMoney());
        Debug.log("economy", "delta", "Manpower: " + manBefore + " → " + resources.getManpower());

        log.add("--- " + calendar.getDisplayString() + " begins ---");
        return log;
    }

    private void triggerGameOver(GameState gameState, String reason) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            javax.swing.JOptionPane.showMessageDialog(
                    null, "GAME OVER\n\n" + reason,
                    "The Realm Falls", javax.swing.JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        });
    }

    private List<String> processSoldierUpkeep(GameState gameState, ResourcePool resources) {
        List<String> log = new ArrayList<>();
        City.main.ledger.Ledger ledger = gameState.getLedger();
        for (City.main.army.Army army : gameState.getArmyManager().getArmies()) {
            if (army.getSize() <= 0) continue;
            City.main.army.SoldierUpkeepProcessor sup =
                    new City.main.army.SoldierUpkeepProcessor(resources, army);
            double cost = sup.computeUpkeepCost();
            if (cost <= 0) continue;
            boolean paid = sup.payUpkeep();
            if (paid) {
                int costCeil = (int) Math.ceil(cost);
                ledger.logOneTime(City.main.resources.ResourceType.GOLD,
                        "soldiers", "Soldier Upkeep (" + army.getDisplayName() + ")",
                        -costCeil);
            } else {
                int lost = sup.processDesertion();
                log.add("⚠ " + army.getDisplayName()
                        + " could not pay upkeep — " + lost + " soldiers deserted.");
            }
        }
        return log;
    }

    private void applyPopEconomics(PopManager popManager,
                                    List<String> log, City.main.ledger.Ledger ledger) {
        int moneyGained     = popManager.getTotalMoneyGeneration();
        int influenceGained = popManager.getTotalInfluenceGeneration() + GameParameters.BASE_INFLUENCE_PER_TURN;
        int foodConsumed    = popManager.getTotalFoodConsumption();

        ledger.setRecurring(City.main.resources.ResourceType.GOLD,      "pops", "Pop Income",      moneyGained);
        ledger.setRecurring(City.main.resources.ResourceType.INFLUENCE,  "pops", "Pop Influence",   influenceGained);
        ledger.setRecurring(City.main.resources.ResourceType.FOOD,       "pops", "Pop Consumption", -foodConsumed);

        log.add("Pops generated " + moneyGained + " money, " + influenceGained + " influence.");
        log.add("Pops consumed " + foodConsumed + " food.");
    }

    private void applyStatDecay(StatBlock stats, List<String> log,
                                 City.main.ledger.Ledger ledger) {
        stats.reduceHappiness(GameParameters.HAPPINESS_DECAY_PER_TURN);
        stats.reduceCorruption(GameParameters.CORRUPTION_DECAY_PER_TURN);
        log.add("Happiness -" + GameParameters.HAPPINESS_DECAY_PER_TURN
                + ", Corruption -" + GameParameters.CORRUPTION_DECAY_PER_TURN
                + " (natural decay).");
    }

    private void applyLedgerToResources(City.main.ledger.Ledger ledger, ResourcePool resources) {
        int deltaGold      = ledger.getDelta(City.main.resources.ResourceType.GOLD);
        int deltaFood      = ledger.getDelta(City.main.resources.ResourceType.FOOD);
        int deltaManpower  = ledger.getDelta(City.main.resources.ResourceType.MANPOWER);
        int deltaInfluence = ledger.getDelta(City.main.resources.ResourceType.INFLUENCE);

        resources.addMoney(deltaGold);
        resources.addFood(deltaFood);
        resources.addManpower(deltaManpower);
        resources.addInfluence(deltaInfluence);
    }
}