package City.main.core;

import City.main.actions.ActionRegistry;
import City.main.army.ArmyManager;
import City.main.barbarians.BarbArmyManager;
import City.main.barbarians.BarbInvasionProcessor;
import City.main.barbarians.BarbInvasionState;
import City.main.barbarians.RavagedZoneManager;
import City.main.barbarians.WarStateChecker;
import City.main.calendar.GameCalendar;
import City.main.legislation.LegislationManager;
import City.main.map.ZoneManager;
import City.main.map.ZoneDecorationRegistry;
import City.main.map.WorldGeography;
import City.main.mercenaries.MercenaryManager;
import City.main.nobles.NobleHouseManager;
import City.main.pops.PopManager;
import City.main.politics.PartyManager;
import City.main.politics.VoteSessionManager;
import City.main.politics.VotingSession;
import City.main.resources.ResourcePool;
import City.main.resources.StatBlock;
import City.main.effects.EffectManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Central hub — owns all subsystems.
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
    private City.main.ledger.Ledger    ledger;

    private BarbInvasionState     barbInvasionState;
    private BarbArmyManager       barbArmyManager;
    private RavagedZoneManager    ravagedZoneManager;
    private BarbInvasionProcessor barbInvasionProcessor;
    private City.main.army.PlayerCombatProcessor playerCombatProcessor;
    private City.main.army.PlayerBattleInterventionProcessor battleInterventionProcessor;
    private City.main.army.commander.CommanderRoster       commanderRoster;
    private City.main.army.commander.CommanderRecruitPool  commanderRecruitPool;

    // ── New subsystems ───────────────────────────────────────────────────────
    private LegislationManager    legislationManager;
    private MercenaryManager      mercenaryManager;
    private WarStateChecker       warStateChecker;
    private City.main.politics.PropagandaManager      propagandaManager;
    private City.main.politics.ElectionManager        electionManager;
    private City.main.nobles.PlayerPrestige           playerPrestige;
    private City.main.nobles.ProtectionManager        protectionManager;
    private City.main.nobles.council.CouncilSessionManager councilSessionManager;
    private City.main.nobles.council.CouncilSession   activeCouncilSession;

    private final List<VotingSession> activeSessions = new ArrayList<>();

    public GameState() {
        initState();
    }

    private void initState() {
        City.main.nobles.ai.OpportunismEvaluator.reset();
        if (playerPrestige   != null) playerPrestige   = new City.main.nobles.PlayerPrestige();
        if (protectionManager!= null) protectionManager= new City.main.nobles.ProtectionManager();
        if (councilSessionManager != null) councilSessionManager.reset();
        activeCouncilSession = null;
        calendar           = new GameCalendar();
        resources          = new ResourcePool();
        stats              = new StatBlock();
        popManager         = new PopManager();
        partyManager       = new PartyManager(popManager);
        ledger             = new City.main.ledger.Ledger();
        legislationManager = new LegislationManager();
        mercenaryManager   = new MercenaryManager();
        effectManager      = new EffectManager();
        voteSessionManager = new VoteSessionManager();
        zoneManager        = new ZoneManager();
        decorationRegistry = new ZoneDecorationRegistry();
        worldGeography     = new WorldGeography();
        nobleHouseManager  = new NobleHouseManager(zoneManager);
        armyManager        = new ArmyManager();
        turnProcessor      = new TurnProcessor();

        barbInvasionState     = new BarbInvasionState();
        barbArmyManager       = new BarbArmyManager(zoneManager);
        ravagedZoneManager    = new RavagedZoneManager();
        warStateChecker       = new WarStateChecker(barbArmyManager);
        barbInvasionProcessor = new BarbInvasionProcessor(
                barbInvasionState,
                barbArmyManager,
                ravagedZoneManager,
                zoneManager,
                nobleHouseManager,
                armyManager);
        nobleHouseManager.setRavagedZoneManager(ravagedZoneManager);
        nobleHouseManager.setBarbArmyManager(barbArmyManager);
        playerCombatProcessor        = new City.main.army.PlayerCombatProcessor();
        playerCombatProcessor.setPartyManager(partyManager);
        playerCombatProcessor.setPlayerPrestige(playerPrestige);
        playerCombatProcessor.setProtectionManager(protectionManager);
        playerCombatProcessor.setNobleHouseManagerRef(nobleHouseManager);
        battleInterventionProcessor  = new City.main.army.PlayerBattleInterventionProcessor();
        // Wire intervention processor so noble army manager can prompt the player
        nobleHouseManager.getArmyManager().setInterventionProcessor(
                battleInterventionProcessor, armyManager);
        commanderRoster      = new City.main.army.commander.CommanderRoster(resources, partyManager);
        commanderRecruitPool = new City.main.army.commander.CommanderRecruitPool(resources, partyManager);

        propagandaManager    = new City.main.politics.PropagandaManager(partyManager.getParties());
        electionManager      = new City.main.politics.ElectionManager();
        playerPrestige       = new City.main.nobles.PlayerPrestige();
        protectionManager    = new City.main.nobles.ProtectionManager();
        councilSessionManager= new City.main.nobles.council.CouncilSessionManager();
        // ActionRegistry depends on legislation+mercenary managers
        actionRegistry = new ActionRegistry(this);

        bootstrapLedger();
    }

    public void reset() {
        activeSessions.clear();
        initState();
    }

    private void bootstrapLedger() {
        City.main.pops.PopManager pops = popManager;
        int moneyGained     = pops.getTotalMoneyGeneration();
        int influenceGained = pops.getTotalInfluenceGeneration()
                            + City.main.parameters.GameParameters.BASE_INFLUENCE_PER_TURN;
        int foodConsumed    = pops.getTotalFoodConsumption();
        ledger.setRecurring(City.main.resources.ResourceType.GOLD,      "pops", "Pop Income",      moneyGained);
        ledger.setRecurring(City.main.resources.ResourceType.INFLUENCE,  "pops", "Pop Influence",   influenceGained);
        ledger.setRecurring(City.main.resources.ResourceType.FOOD,       "pops", "Pop Consumption", -foodConsumed);

        for (City.main.nobles.NobleHouse house : nobleHouseManager.getHouses()) {
            if (house.isEliminated()) continue;
            double share    = house.getPlayerOpinion() <= City.main.parameters.GameParameters.NOBLE_HOSTILE_OPINION_THRESHOLD
                            ? 0.0
                            : house.getPlayerOpinion() > 50 ? 0.50 : 0.35;
            int sentManpower = house.computeManpowerSentToPlayer();
            int zoneCount = house.getZoneIds().size();
            int totalGold = 0;
            int totalFood = 0;
            for (String zoneId : house.getZoneIds()) {
                City.main.map.Zone zone = zoneManager.getZone(zoneId);
                totalGold += (zone != null ? zone.getGoldProduction() : 0)
                           + City.main.parameters.GameParameters.NOBLE_ZONE_GOLD_PER_TURN;
                totalFood += (zone != null ? zone.getFoodProduction() : 0);
            }
            int playerGold = (int)(totalGold * share);
            int playerFood = (int)(totalFood * share);
            ledger.setRecurring(City.main.resources.ResourceType.GOLD,     "nobles", house.getName(), playerGold);
            ledger.setRecurring(City.main.resources.ResourceType.FOOD,     "nobles", house.getName(), playerFood);
            ledger.setRecurring(City.main.resources.ResourceType.MANPOWER, "nobles", house.getName(), sentManpower);
        }
    }

    public boolean          hasActiveSession()  { return !activeSessions.isEmpty(); }
    public VotingSession    getActiveSession()  { return activeSessions.isEmpty() ? null : activeSessions.get(0); }
    public void             addSession(VotingSession s) { activeSessions.add(s); }
    public void             clearActiveSession(){ activeSessions.clear(); }

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
    public City.main.nobles.NobleArmyManager getNobleArmyManager() { return nobleHouseManager.getArmyManager(); }
    public ArmyManager           getArmyManager()           { return armyManager; }
    public TurnProcessor         getTurnProcessor()         { return turnProcessor; }

    public BarbInvasionState     getBarbInvasionState()     { return barbInvasionState; }
    public BarbArmyManager       getBarbArmyManager()       { return barbArmyManager; }
    public RavagedZoneManager    getRavagedZoneManager()    { return ravagedZoneManager; }
    public BarbInvasionProcessor       getBarbInvasionProcessor()  { return barbInvasionProcessor; }
    public City.main.ledger.Ledger          getLedger()                 { return ledger; }
    public City.main.army.PlayerCombatProcessor getPlayerCombatProcessor() { return playerCombatProcessor; }
    public City.main.army.PlayerBattleInterventionProcessor getBattleInterventionProcessor() {
        return battleInterventionProcessor;
    }
    public City.main.army.commander.CommanderRoster       getCommanderRoster()       { return commanderRoster; }
    public City.main.army.commander.CommanderRecruitPool  getCommanderRecruitPool()  { return commanderRecruitPool; }

    public LegislationManager            getLegislationManager()   { return legislationManager; }
    public MercenaryManager              getMercenaryManager()     { return mercenaryManager; }
    public WarStateChecker               getWarStateChecker()      { return warStateChecker; }
    public City.main.politics.PropagandaManager       getPropagandaManager()    { return propagandaManager; }
    public City.main.politics.ElectionManager         getElectionManager()      { return electionManager; }
    public City.main.nobles.PlayerPrestige            getPlayerPrestige()       { return playerPrestige; }
    public City.main.nobles.ProtectionManager         getProtectionManager()    { return protectionManager; }
    public City.main.nobles.council.CouncilSessionManager getCouncilSessionManager() { return councilSessionManager; }
    public City.main.nobles.council.CouncilSession    getActiveCouncilSession() { return activeCouncilSession; }
    public boolean hasActiveCouncilSession()      { return activeCouncilSession != null; }
    public void setActiveCouncilSession(City.main.nobles.council.CouncilSession s) { activeCouncilSession = s; }
    public void clearActiveCouncilSession()       { activeCouncilSession = null; }

    public void resetBarbarians() {
        barbArmyManager.reset();
        ravagedZoneManager.reset();
        barbInvasionState.resetCountdown();
    }
}