package main.core;

import main.actions.ActionRegistry;
import main.army.ArmyManager;
import main.barbarians.*;
import main.calendar.GameCalendar;
import main.map.ZoneManager;
import main.map.ZoneDecorationRegistry;
import main.map.WorldGeography;
import main.nobles.NobleHouseManager;
import main.pops.PopManager;
import main.politics.PartyManager;
import main.politics.VoteSessionManager;
import main.politics.VotingSession;
import main.resources.ResourcePool;
import main.resources.StatBlock;
import main.effects.EffectManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Central hub — owns all subsystems.
 * No game logic lives here; logic lives in processors and managers.
 */
public class GameState {

    private GameCalendar          calendar;
    private ResourcePool          resources;
    private StatBlock             stats;
    private PopManager            popManager;
    private PartyManager          partyManager;
    private ActionRegistry        actionRegistry;
    private EffectManager         effectManager;
    private VoteSessionManager    voteSessionManager;
    private ZoneManager           zoneManager;
    private ZoneDecorationRegistry decorationRegistry;
    private WorldGeography        worldGeography;
    private NobleHouseManager     nobleHouseManager;
    private ArmyManager           armyManager;
    private TurnProcessor         turnProcessor;
    private main.ledger.Ledger    ledger;

    // ── Barbarian invasion subsystem ─────────────────────────────────────────
    private BarbInvasionState     barbInvasionState;
    private BarbArmyManager       barbArmyManager;
    private RavagedZoneManager    ravagedZoneManager;
    private BarbInvasionProcessor barbInvasionProcessor;
    private main.army.PlayerCombatProcessor playerCombatProcessor;
    private main.army.commander.CommanderRoster       commanderRoster;
    private main.army.commander.CommanderRecruitPool  commanderRecruitPool;

    private final List<VotingSession> activeSessions = new ArrayList<>();

    public GameState() {
        initState();
    }

    private void initState() {
        main.nobles.ai.OpportunismEvaluator.reset();
        calendar           = new GameCalendar();
        resources          = new ResourcePool();
        stats              = new StatBlock();
        popManager         = new PopManager();
        partyManager       = new PartyManager(popManager);
        actionRegistry     = new ActionRegistry(this);
        effectManager      = new EffectManager();
        voteSessionManager = new VoteSessionManager();
        zoneManager        = new ZoneManager();
        decorationRegistry = new ZoneDecorationRegistry();
        worldGeography     = new WorldGeography();
        nobleHouseManager  = new NobleHouseManager(zoneManager);
        armyManager        = new ArmyManager();
        turnProcessor      = new TurnProcessor();
        ledger             = new main.ledger.Ledger();

        barbInvasionState     = new BarbInvasionState();
        barbArmyManager       = new BarbArmyManager(zoneManager);
        ravagedZoneManager    = new RavagedZoneManager();
        barbInvasionProcessor = new BarbInvasionProcessor(
                barbInvasionState,
                barbArmyManager,
                ravagedZoneManager,
                zoneManager,
                nobleHouseManager,
                armyManager);
        nobleHouseManager.setRavagedZoneManager(ravagedZoneManager);
        playerCombatProcessor = new main.army.PlayerCombatProcessor();
        playerCombatProcessor.setPartyManager(partyManager);
        commanderRoster      = new main.army.commander.CommanderRoster(resources, partyManager);
        commanderRecruitPool = new main.army.commander.CommanderRecruitPool(resources, partyManager);
        // Note: zoneAwardCallback is re-wired by MainWindow after reset via
        // rewireCallbacks(). Do not set it here.
        bootstrapLedger();
    }

    /**
     * Recreates all subsystems for a new game, keeping the same GameState instance.
     */
    public void reset() {
        activeSessions.clear();
        initState();
    }

private void bootstrapLedger() {
        // Pops
        main.pops.PopManager pops = popManager;
        int moneyGained     = pops.getTotalMoneyGeneration();
        int influenceGained = pops.getTotalInfluenceGeneration()
                            + main.parameters.GameParameters.BASE_INFLUENCE_PER_TURN;
        int foodConsumed    = pops.getTotalFoodConsumption();
        ledger.setRecurring(main.resources.ResourceType.GOLD,      "pops", "Pop Income",      moneyGained);
        ledger.setRecurring(main.resources.ResourceType.INFLUENCE,  "pops", "Pop Influence",   influenceGained);
        ledger.setRecurring(main.resources.ResourceType.FOOD,       "pops", "Pop Consumption", -foodConsumed);

        // Nobles
        for (main.nobles.NobleHouse house : nobleHouseManager.getHouses()) {
            if (house.isEliminated()) continue;
            double share    = house.getPlayerOpinion() <= main.parameters.GameParameters.NOBLE_HOSTILE_OPINION_THRESHOLD
                            ? 0.0
                            : house.getPlayerOpinion() > 50 ? 0.50 : 0.35;
            int sentManpower = house.computeManpowerSentToPlayer();
            // compute gold/food using zone data via NobleHouseManager exposed helpers
            // we approximate here using the same formula as processEconomy
            int zoneCount = house.getZoneIds().size();
            int totalGold = 0;
            int totalFood = 0;
            for (String zoneId : house.getZoneIds()) {
                main.map.Zone zone = zoneManager.getZone(zoneId);
                totalGold += (zone != null ? zone.getGoldProduction() : 0)
                           + main.parameters.GameParameters.NOBLE_ZONE_GOLD_PER_TURN;
                totalFood += (zone != null ? zone.getFoodProduction() : 0);
            }
            int playerGold = (int)(totalGold * share);
            int playerFood = (int)(totalFood * share);
            ledger.setRecurring(main.resources.ResourceType.GOLD,     "nobles", house.getName(), playerGold);
            ledger.setRecurring(main.resources.ResourceType.FOOD,     "nobles", house.getName(), playerFood);
            ledger.setRecurring(main.resources.ResourceType.MANPOWER, "nobles", house.getName(), sentManpower);
        }
    }

// ─── Vote session ─────────────────────────────────────────────────────────

    public boolean          hasActiveSession()  { return !activeSessions.isEmpty(); }
    public VotingSession    getActiveSession()  { return activeSessions.isEmpty() ? null : activeSessions.get(0); }
    public void             addSession(VotingSession s) { activeSessions.add(s); }
    public void             clearActiveSession(){ activeSessions.clear(); }

    // ─── Accessors ───────────────────────────────────────────────────────────

    public GameCalendar          getCalendar()              { return calendar; }
    public ResourcePool          getResources()             { return resources; }
    public StatBlock             getStats()                 { return stats; }
    public PopManager            getPopManager()            { return popManager; }
    public PartyManager          getPartyManager()          { return partyManager; }
    public ActionRegistry        getActionRegistry()        { return actionRegistry; }
    public EffectManager         getEffectManager()         { return effectManager; }
    public VoteSessionManager    getVoteSessionManager()    { return voteSessionManager; }
    public ZoneManager           getZoneManager()           { return zoneManager; }
    public ZoneDecorationRegistry getDecorationRegistry()  { return decorationRegistry; }
    public WorldGeography        getWorldGeography()        { return worldGeography; }
    public NobleHouseManager     getNobleHouseManager()     { return nobleHouseManager; }
    public main.nobles.NobleArmyManager getNobleArmyManager() { return nobleHouseManager.getArmyManager(); }
    public ArmyManager           getArmyManager()           { return armyManager; }
    public TurnProcessor         getTurnProcessor()         { return turnProcessor; }

    public BarbInvasionState     getBarbInvasionState()     { return barbInvasionState; }
    public BarbArmyManager       getBarbArmyManager()       { return barbArmyManager; }
    public RavagedZoneManager    getRavagedZoneManager()    { return ravagedZoneManager; }
    public BarbInvasionProcessor       getBarbInvasionProcessor()  { return barbInvasionProcessor; }
    public main.ledger.Ledger          getLedger()                 { return ledger; }
    public main.army.PlayerCombatProcessor getPlayerCombatProcessor() { return playerCombatProcessor; }
    public main.army.commander.CommanderRoster       getCommanderRoster()       { return commanderRoster; }
    public main.army.commander.CommanderRecruitPool  getCommanderRecruitPool()  { return commanderRecruitPool; }

    /** Resets barbarian subsystem for new game. */
    public void resetBarbarians() {
        barbArmyManager.reset();
        ravagedZoneManager.reset();
        barbInvasionState.resetCountdown();
    }
}