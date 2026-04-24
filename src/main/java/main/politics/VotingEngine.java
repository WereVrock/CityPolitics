package main.politics;

import main.resources.ResourcePool;
import main.resources.StatBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import main.parameters.GameParameters;

/**
 * Calculates vote scores for all parties and resolves seat splits.
 */
public class VotingEngine {

    private static final double INDECISIVE_THRESHOLD     = GameParameters.VOTE_INDECISIVE_THRESHOLD;
    private static final double OPINION_NEUTRAL          = GameParameters.VOTE_OPINION_NEUTRAL;
    private static final double OPINION_MAX_CONTRIBUTION = GameParameters.VOTE_OPINION_MAX_CONTRIBUTION;
    private static final int    SEATS_NEEDED             = 27;
    private static final Random RANDOM                   = new Random();

    public VoteResult process(List<PoliticalParty> parties,
                              List<VoteCondition>  conditions,
                              ResourcePool         resources,
                              StatBlock            stats) {

        List<VoteScore> scores   = new ArrayList<>();
        int totalYes     = 0;
        int totalNo      = 0;
        int totalAbstain = 0;

        for (PoliticalParty party : parties) {
            double score    = calculateScore(party, conditions, resources, stats);
            VoteScore result = resolveSeatSplit(party, score);
            scores.add(result);
            totalYes     += result.getYesSeats();
            totalNo      += result.getNoSeats();
            totalAbstain += result.getAbstainSeats();
        }

        return new VoteResult(scores, totalYes, totalNo, totalAbstain, SEATS_NEEDED);
    }

private double calculateScore(PoliticalParty party,
                                  List<VoteCondition> conditions,
                                  ResourcePool resources,
                                  StatBlock stats) {
        double score = 0.0;
        for (VoteCondition condition : conditions) {
            if (conditionMet(condition, resources, stats)) {
                double viewMultiplier = (condition.getView() != null)
                    ? party.getViewStrength(condition.getView()).getMultiplier()
                    : 1.0;
                score += condition.getWeight() * viewMultiplier;
            }
        }
        double opinionDeviation = (party.getPlayerOpinion() - OPINION_NEUTRAL) / OPINION_NEUTRAL;
        score += opinionDeviation * OPINION_MAX_CONTRIBUTION;
        return score;
    }

private boolean conditionMet(VoteCondition condition,
                                 ResourcePool resources,
                                 StatBlock stats) {
        double value = resolveVariable(condition.getVariable(), resources, stats);
        return switch (condition.getRelation()) {
            case GREATER_THAN -> value > condition.getThreshold();
            case LESS_THAN    -> value < condition.getThreshold();
        };
    }

    private double resolveVariable(VoteCondition.Variable variable,
                                   ResourcePool resources,
                                   StatBlock stats) {
        return switch (variable) {
            case MONEY      -> resources.getMoney();
            case FOOD       -> resources.getFood();
            case INFLUENCE  -> resources.getInfluence();
            case MANPOWER   -> resources.getManpower();
            case CORRUPTION -> stats.getCorruption();
            case HAPPINESS  -> stats.getHappiness();
        };
    }

    private VoteScore resolveSeatSplit(PoliticalParty party, double score) {
        int seats = party.getSeats();

        if (score > INDECISIVE_THRESHOLD) {
            return new VoteScore(party, score, seats, 0, 0);
        }
        if (score < -INDECISIVE_THRESHOLD) {
            return new VoteScore(party, score, 0, seats, 0);
        }

        // Indecisive: random split across yes/no/abstain
        int yes     = 0;
        int no      = 0;
        int abstain = 0;
        for (int i = 0; i < seats; i++) {
            int roll = RANDOM.nextInt(3);
            if      (roll == 0) yes++;
            else if (roll == 1) no++;
            else                abstain++;
        }
        return new VoteScore(party, score, yes, no, abstain);
    }
}