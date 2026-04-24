package main.core;

import main.actions.ActionRegistry;
import main.calendar.GameCalendar;
import main.effects.EffectManager;
import main.pops.PopManager;
import main.politics.PartyManager;
import main.resources.ResourcePool;
import main.resources.StatBlock;

public class GameState {

    private final GameCalendar   calendar;
    private final ResourcePool   resources;
    private final StatBlock      stats;
    private final PopManager     popManager;
    private final ActionRegistry actionRegistry;
    private final TurnProcessor  turnProcessor;
    private final EffectManager  effectManager;
    private final PartyManager   partyManager;

    public GameState() {
        this.calendar       = new GameCalendar();
        this.resources      = new ResourcePool();
        this.stats          = new StatBlock();
        this.popManager     = new PopManager();
        this.effectManager  = new EffectManager();
        this.partyManager   = new PartyManager(popManager);
        this.actionRegistry = new ActionRegistry(this);
        this.turnProcessor  = new TurnProcessor();
    }

    public void reset() {
        calendar.reset();
        resources.reset();
        stats.reset();
        popManager.reset();
        effectManager.reset();
        partyManager.reset();
        actionRegistry.resetAllActions();
    }

    public GameCalendar   getCalendar()       { return calendar; }
    public ResourcePool   getResources()      { return resources; }
    public StatBlock      getStats()          { return stats; }
    public PopManager     getPopManager()     { return popManager; }
    public ActionRegistry getActionRegistry() { return actionRegistry; }
    public TurnProcessor  getTurnProcessor()  { return turnProcessor; }
    public EffectManager  getEffectManager()  { return effectManager; }
    public PartyManager   getPartyManager()   { return partyManager; }
}