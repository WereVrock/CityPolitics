package main.actions;

import main.core.GameState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ActionRegistry {

    private final List<PlayerAction> actions = new ArrayList<>();

    public ActionRegistry(GameState gameState) {
        actions.add(new ImportFoodAction());
        actions.add(new AcceptBribesAction());
        actions.add(new BribeAction());
        actions.add(new DistributeResourcesAction());
        actions.add(new FightCorruptionAction());
        actions.add(new OrganizeFestivalAction(gameState));
        actions.add(new CrackdownCorruptionAction(gameState));
        actions.add(new RoyalLevyAction(gameState));
    }

    public List<PlayerAction> getActions() {
        return Collections.unmodifiableList(actions);
    }

    public void resetAllActions() {
        for (PlayerAction action : actions) {
            action.resetUses();
        }
    }
}