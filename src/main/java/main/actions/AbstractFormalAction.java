package main.actions;

import main.core.CostCalculator;
import main.core.GameState;
import main.politics.VotingSession;
import main.resources.ResourcePool;
import main.resources.StatBlock;

import main.politics.VotingEngine;

/**
* Base for all formal actions. Handles the voting flow.
* Subclasses implement costs, effects, and vote conditions.
*/
public abstract class AbstractFormalAction extends AbstractAction implements FormalAction {

private final GameState    gameState;
private final VotingEngine votingEngine;

protected AbstractFormalAction(GameState gameState) {
super(1); // formal actions: max 1 use per turn
this.gameState   = gameState;
this.votingEngine = new VotingEngine();
}



@Override
public final ActionResult execute(ResourcePool resources, StatBlock stats) {
if (!isAvailable()) {
return ActionResult.fail(getName() + " already used this turn.");
}

int influenceCost = CostCalculator.apply(getInfluenceCost(), stats.getCorruption());
if (!resources.spendInfluence(influenceCost)) {
return ActionResult.fail("Not enough influence. Need " + influenceCost + ".");
}

VotingSession session = gameState.getVoteSessionManager().createSession(
this,
gameState.getPartyManager().getParties(),
resources,
stats
);
gameState.addSession(session);
recordUse();

return ActionResult.votePending(getName() + " sent to assembly. " + influenceCost + " influence spent.");
}

/**
* Called only when the vote passes. Apply the action's actual effect here.
*/
public abstract ActionResult applyEffect(ResourcePool resources, StatBlock stats);

protected GameState getGameState() { return gameState; }
}

