package main.actions;

import main.core.CostCalculator;
import main.core.GameState;
import main.parameters.GameParameters;
import main.politics.PolitcalView;
import main.politics.VoteCondition;
import main.resources.ResourcePool;
import main.resources.StatBlock;

import java.util.List;

/**
 * Formal Action: Royal Levy
 * Extracts money from the realm. Costs influence and happiness.
 * Requires assembly approval.
 */
public class RoyalLevyAction extends AbstractFormalAction {

    private static final List<VoteCondition> CONDITIONS = List.of(// Traditionalists support — it is the ruler's right
        new VoteCondition(VoteCondition.Variable.HAPPINESS, VoteCondition.Relation.GREATER_THAN,
            30, 0.6, PolitcalView.TRADITIONALIST),
        // Democrats resist when happiness is already low — people are suffering
        new VoteCondition(VoteCondition.Variable.HAPPINESS, VoteCondition.Relation.LESS_THAN,
            50, -0.7, PolitcalView.DEMOCRATIC),
        // Mercantile parties resist — taxation hurts commerce
        new VoteCondition(VoteCondition.Variable.MONEY, VoteCondition.Relation.GREATER_THAN,
            200, -0.5, PolitcalView.MERCANTILE),
        // Everyone more willing if treasury is desperate
        new VoteCondition(VoteCondition.Variable.MONEY, VoteCondition.Relation.LESS_THAN,
            50, 0.6, null),
        // Militarists support if manpower is high — strong realm can afford it
        new VoteCondition(VoteCondition.Variable.MANPOWER, VoteCondition.Relation.GREATER_THAN,
            80, 0.3, PolitcalView.MILITARIST),
        // Isolationists support — keeps money inside the realm
        new VoteCondition(VoteCondition.Variable.MONEY, VoteCondition.Relation.LESS_THAN,
            150, 0.4, PolitcalView.ISOLATIONIST)
    );

    public RoyalLevyAction(GameState gameState) {
        super(gameState);
    }

    @Override public String getName() { return "Royal Levy"; }

    @Override
    public String getDescription() {
        return "Spend " + GameParameters.LEVY_INFLUENCE_COST
            + " influence. Collect " + GameParameters.LEVY_MONEY_GAINED
            + " money. Happiness -" + GameParameters.LEVY_HAPPINESS_COST + ". Requires vote.";
    }

    @Override public int getInfluenceCost() { return GameParameters.LEVY_INFLUENCE_COST; }

    @Override public List<VoteCondition> getVoteConditions() { return CONDITIONS; }

    @Override
    protected ActionResult applyEffect(ResourcePool resources, StatBlock stats) {
        resources.addMoney(GameParameters.LEVY_MONEY_GAINED);
        stats.reduceHappiness(GameParameters.LEVY_HAPPINESS_COST);
        return ActionResult.ok("Royal Levy collected. +" + GameParameters.LEVY_MONEY_GAINED
            + " money. Happiness -" + GameParameters.LEVY_HAPPINESS_COST + ".");
    }
}