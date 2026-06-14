package City.main.actions;

import City.main.core.CostCalculator;
import City.main.core.GameState;
import City.main.parameters.ActionParams;
 
import City.main.politics.PolitcalView;
import City.main.politics.VoteCondition;
import City.main.resources.ResourcePool;
import City.main.resources.StatBlock;

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
        return "Spend " + ActionParams.LEVY_INFLUENCE_COST
            + " influence. Collect " + ActionParams.LEVY_MONEY_GAINED
            + " money. Happiness -" + ActionParams.LEVY_HAPPINESS_COST + ". Requires vote.";
    }

    @Override public int getInfluenceCost() { return ActionParams.LEVY_INFLUENCE_COST; }

    @Override public List<VoteCondition> getVoteConditions() { return CONDITIONS; }

    @Override


    public ActionResult applyEffect(ResourcePool resources, StatBlock stats) {
        getLedger().applyOneTime(City.main.resources.ResourceType.GOLD, "action", getName(),
                ActionParams.LEVY_MONEY_GAINED, resources);
        stats.reduceHappiness(ActionParams.LEVY_HAPPINESS_COST);
        return ActionResult.ok("Royal Levy collected. +" + ActionParams.LEVY_MONEY_GAINED
                + " money. Happiness -" + ActionParams.LEVY_HAPPINESS_COST + ".");
    }

}