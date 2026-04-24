package main.actions;

import main.core.CostCalculator;
import main.core.GameState;
import main.effects.ActiveEffect;
import main.parameters.GameParameters;
import main.politics.PolitcalView;
import main.politics.VoteCondition;
import main.resources.ResourcePool;
import main.resources.StatBlock;

import java.util.List;

/**
 * Formal Action: Organize Festival
 * Large money cost. Happiness boost that decays over 5 turns.
 * Requires assembly approval.
 */
public class OrganizeFestivalAction extends AbstractFormalAction {

    private static final List<VoteCondition> CONDITIONS = List.of(// Mercantile parties like festivals when money reserves are healthy
        new VoteCondition(VoteCondition.Variable.MONEY, VoteCondition.Relation.GREATER_THAN,
            150, 0.6, PolitcalView.MERCANTILE),
        // Mercantile parties resist when money is very low
        new VoteCondition(VoteCondition.Variable.MONEY, VoteCondition.Relation.LESS_THAN,
            60, -0.8, PolitcalView.MERCANTILE),
        // Democratic parties always lean toward festivals (people love them)
        new VoteCondition(VoteCondition.Variable.HAPPINESS, VoteCondition.Relation.LESS_THAN,
            70, 0.5, PolitcalView.DEMOCRATIC),
        // Militarists dislike spending on festivals when corruption is high
        new VoteCondition(VoteCondition.Variable.CORRUPTION, VoteCondition.Relation.GREATER_THAN,
            40, -0.4, PolitcalView.MILITARIST),
        // Traditionalists enjoy festivals as cultural events
        new VoteCondition(VoteCondition.Variable.HAPPINESS, VoteCondition.Relation.LESS_THAN,
            80, 0.3, PolitcalView.TRADITIONALIST)
    );

    public OrganizeFestivalAction(GameState gameState) {
        super(gameState);
    }

    @Override public String getName() { return "Organize Festival"; }

    @Override
    public String getDescription() {
        return "Spend " + GameParameters.FESTIVAL_MONEY_COST
            + " money and " + GameParameters.FESTIVAL_INFLUENCE_COST
            + " influence. Grants +" + GameParameters.FESTIVAL_HAPPINESS_BOOST
            + " happiness decaying over " + GameParameters.FESTIVAL_DURATION_TURNS + " turns. Requires vote.";
    }

    @Override public int getInfluenceCost() { return GameParameters.FESTIVAL_INFLUENCE_COST; }

    @Override public List<VoteCondition> getVoteConditions() { return CONDITIONS; }

    @Override
    protected ActionResult applyEffect(ResourcePool resources, StatBlock stats) {
        int moneyCost = CostCalculator.apply(GameParameters.FESTIVAL_MONEY_COST, stats.getCorruption());
        if (!resources.spendMoney(moneyCost)) {
            return ActionResult.fail("Not enough money after vote. Need " + moneyCost + ".");
        }
        getGameState().getEffectManager().addEffect(new ActiveEffect(
            ActiveEffect.Type.HAPPINESS_BOOST,
            GameParameters.FESTIVAL_HAPPINESS_BOOST,
            GameParameters.FESTIVAL_DURATION_TURNS
        ));
        return ActionResult.ok("Festival declared! +" + GameParameters.FESTIVAL_HAPPINESS_BOOST
            + " happiness over " + GameParameters.FESTIVAL_DURATION_TURNS + " turns.");
    }
}