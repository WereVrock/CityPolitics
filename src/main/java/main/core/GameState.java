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

    // ── Barbarian invasion subsystem ─────────────────────────────────────────
    private BarbInvasionState     barbInvasionState;
    private BarbArmyManager       barbArmyManager;
    private RavagedZoneManager    ravagedZoneManager;
    private BarbInvasionProcessor barbInvasionProcessor;

    private final List<VotingSession> activeSessions = new ArrayList<>();

    public GameState() {
        initState();
    }

    private void initState() {
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
    }

    /**
     * Recreates all subsystems for a new game, keeping the same GameState instance.
     */
    public void reset() {
        activeSessions.clear();
        initState();
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
    public BarbInvasionProcessor getBarbInvasionProcessor() { return barbInvasionProcessor; }

    /** Resets barbarian subsystem for new game. */
    public void resetBarbarians() {
        barbArmyManager.reset();
        ravagedZoneManager.reset();
        barbInvasionState.resetCountdown();
    }
}