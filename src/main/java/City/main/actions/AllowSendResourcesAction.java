package City.main.actions;

import City.main.core.GameState;
import City.main.legislation.LegislationManager;
import City.main.legislation.LegislationType;
import City.main.politics.PolitcalView;
import City.main.politics.VoteCondition;
import City.main.resources.ResourcePool;
import City.main.resources.StatBlock;

import java.util.List;

/**
 * Formal action — Allow Sending Resources to Nobles.
 * If passed, grants a 3-turn window for the "Send Resources to Nobles" realm action.
 */
public class AllowSendResourcesAction extends AbstractFormalAction {

    private final LegislationManager legislationManager;

    private static final List<VoteCondition> CONDITIONS = List.of(
        // Imperialists / those who see the whole realm as home → support
        new VoteCondition(VoteCondition.Variable.HAPPINESS,
                VoteCondition.Relation.GREATER_THAN, 50, 0.5, PolitcalView.TRADITIONALIST),
        new VoteCondition(VoteCondition.Variable.MONEY,
                VoteCondition.Relation.GREATER_THAN, 120, 0.4, PolitcalView.MERCANTILE),
        // Populists / city-first thinkers → resist
        new VoteCondition(VoteCondition.Variable.HAPPINESS,
                VoteCondition.Relation.LESS_THAN, 50, -0.5, PolitcalView.DEMOCRATIC),
        new VoteCondition(VoteCondition.Variable.MONEY,
                VoteCondition.Relation.LESS_THAN, 80, -0.4, null),
        new VoteCondition(VoteCondition.Variable.CORRUPTION,
                VoteCondition.Relation.GREATER_THAN, 40, -0.3, PolitcalView.DEMOCRATIC)
    );

    public AllowSendResourcesAction(GameState gameState, LegislationManager legislationManager) {
        super(gameState);
        this.legislationManager = legislationManager;
    }

    @Override public String getName() { return "Allow Sending Resources"; }

    @Override
    public String getDescription() {
        return "Vote to allow sending resources to noble houses for "
                + City.main.parameters.GameParameters.SEND_RESOURCES_WINDOW_TURNS + " turns. "
                + "Costs " + City.main.parameters.GameParameters.ALLOW_SEND_RESOURCES_INFLUENCE_COST
                + " influence.";
    }

    @Override
    public int getInfluenceCost() {
        return City.main.parameters.GameParameters.ALLOW_SEND_RESOURCES_INFLUENCE_COST;
    }

    @Override
    public List<VoteCondition> getVoteConditions() { return CONDITIONS; }

    @Override
    public ActionResult applyEffect(ResourcePool resources, StatBlock stats) {
        legislationManager.grantSendResourcesWindow();
        return ActionResult.ok("Sending resources to nobles authorised for "
                + City.main.parameters.GameParameters.SEND_RESOURCES_WINDOW_TURNS + " turns.");
    }
}