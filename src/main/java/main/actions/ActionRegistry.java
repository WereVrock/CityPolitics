package main.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds all available player actions for the current game.
 * Add new actions here as they are implemented.
 */
public class ActionRegistry {

    private final List<PlayerAction> actions = new ArrayList<>();

    public ActionRegistry() {
        actions.add(new ImportFoodAction());
        actions.add(new AcceptBribesAction());
        actions.add(new BribeAction());
        actions.add(new DistributeResourcesAction());
        actions.add(new FightCorruptionAction());
    }

    public List<PlayerAction> getActions() {
        return Collections.unmodifiableList(actions);
    }

    /**
     * Must be called at the start of every new turn.
     */
    public void resetAllActions() {
        for (PlayerAction action : actions) {
            action.resetUses();
        }
    }
}