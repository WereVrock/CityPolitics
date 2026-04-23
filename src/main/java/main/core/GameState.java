package main.core;

import main.actions.ActionRegistry;
import main.calendar.GameCalendar;
import main.pops.PopManager;
import main.resources.ResourcePool;
import main.resources.StatBlock;

/**
 * Assembles all game subsystems into one place.
 * Contains NO game logic — that belongs in TurnProcessor and action classes.
 */
public class GameState {

    private final GameCalendar    calendar;
    private final ResourcePool    resources;
    private final StatBlock       stats;
    private final PopManager      popManager;
    private final ActionRegistry  actionRegistry;
    private final TurnProcessor   turnProcessor;

    public GameState() {
        this.calendar       = new GameCalendar();
        this.resources      = new ResourcePool();
        this.stats          = new StatBlock();
        this.popManager     = new PopManager();
        this.actionRegistry = new ActionRegistry();
        this.turnProcessor  = new TurnProcessor();
    }

    public GameCalendar   getCalendar()       { return calendar; }
    public ResourcePool   getResources()      { return resources; }
    public StatBlock      getStats()          { return stats; }
    public PopManager     getPopManager()     { return popManager; }
    public ActionRegistry getActionRegistry() { return actionRegistry; }
    public TurnProcessor  getTurnProcessor()  { return turnProcessor; }
}