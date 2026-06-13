package main.politics;

import main.parameters.GameParameters;

/**
 * What a party demands in exchange for switching their vote to YES.
 * Cost scales with seat count and score magnitude.
 * If the party demands a favour, resource costs are zero (favour replaces them).
 */
public class DealOffer {

    private final int moneyCost;
    private final int influenceCost;
    private final int happinessMalus;
    private final int favourCost;
    private final boolean favourOnly;

    public DealOffer(PoliticalParty party, double score) {
        double magnitude = Math.abs(score);
        if (magnitude < 0.05) magnitude = 0.05;
        int seats = party.getSeats();
        double base = seats * magnitude;

        int rawMoney     = (int)(base * GameParameters.DEAL_MONEY_FACTOR);
        int rawInfluence = (int)(base * GameParameters.DEAL_INFLUENCE_FACTOR);
        int rawHappiness = (int)(base * GameParameters.DEAL_HAPPINESS_FACTOR);

        if (magnitude >= GameParameters.DEAL_FAVOUR_THRESHOLD_2) {
            this.favourCost     = 2;
        } else if (magnitude >= GameParameters.DEAL_FAVOUR_THRESHOLD_1) {
            this.favourCost     = 1;
        } else {
            this.favourCost     = 0;
        }

        // If favour is required, resource costs are zero — favour replaces money/influence
        if (this.favourCost > 0) {
            this.favourOnly      = true;
            this.moneyCost       = 0;
            this.influenceCost   = 0;
            this.happinessMalus  = rawHappiness;
        } else {
            this.favourOnly      = false;
            // Apply minimums only when no favour is involved
            this.moneyCost       = Math.max(GameParameters.DEAL_MIN_MONEY,     rawMoney);
            this.influenceCost   = Math.max(GameParameters.DEAL_MIN_INFLUENCE, rawInfluence);
            this.happinessMalus  = rawHappiness;
        }
    }

    public int     getMoneyCost()      { return moneyCost; }
    public int     getInfluenceCost()  { return influenceCost; }
    public int     getHappinessMalus() { return happinessMalus; }
    public int     getFavourCost()     { return favourCost; }
    public boolean isFavourOnly()      { return favourOnly; }

    public String getSummary() {
        if (favourOnly) {
            return "Demands: " + favourCost + " favour"
                    + (happinessMalus > 0 ? " + " + happinessMalus + " happiness" : "");
        }
        StringBuilder sb = new StringBuilder("Demands: ");
        if (moneyCost      > 0) sb.append(moneyCost).append(" gold  ");
        if (influenceCost  > 0) sb.append(influenceCost).append(" influence  ");
        if (happinessMalus > 0) sb.append(happinessMalus).append(" happiness  ");
        return sb.toString().trim();
    }

    public boolean canAfford(main.resources.ResourcePool res, main.resources.StatBlock stats) {
        if (favourOnly) {
            return res.getInfluence() >= 0; // favour is always "affordable" (it's owed, not spent)
        }
        return res.getMoney()     >= moneyCost
            && res.getInfluence() >= influenceCost;
    }

    public void apply(main.resources.ResourcePool res, main.resources.StatBlock stats) {
        if (!favourOnly) {
            res.spendMoney(moneyCost);
            res.spendInfluence(influenceCost);
        }
        if (happinessMalus > 0) stats.reduceHappiness(happinessMalus);
        // Favour is tracked via VotingSession.applyDeal which decrements party favour
    }
}