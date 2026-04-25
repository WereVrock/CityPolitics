// DealOffer.java
package main.politics;

import main.parameters.GameParameters;

/**
 * What a party demands in exchange for switching their vote to YES.
 * Cost scales with seat count and score magnitude.
 */
public class DealOffer {

    private final int moneyCost;
    private final int influenceCost;
    private final int happinessMalus;
    private final int favourCost;

public DealOffer(PoliticalParty party, double score) {
        double magnitude = Math.abs(score);
        int    seats     = party.getSeats();

        double base = seats * magnitude;

        this.moneyCost      = (int) (base * GameParameters.DEAL_MONEY_FACTOR);
        this.influenceCost  = (int) (base * GameParameters.DEAL_INFLUENCE_FACTOR);
        this.happinessMalus = (int) (base * GameParameters.DEAL_HAPPINESS_FACTOR);

        if (magnitude >= GameParameters.DEAL_FAVOUR_THRESHOLD_2) {
            this.favourCost = 2;
        } else if (magnitude >= GameParameters.DEAL_FAVOUR_THRESHOLD_1) {
            this.favourCost = 1;
        } else {
            this.favourCost = 0;
        }
    }

public int getMoneyCost()      { return moneyCost; }
    public int getInfluenceCost()  { return influenceCost; }
    public int getHappinessMalus() { return happinessMalus; }
    public int getFavourCost()     { return favourCost; }

public String getSummary() {
        StringBuilder sb = new StringBuilder("Demands: ");
        if (moneyCost      > 0) sb.append(moneyCost).append(" gold  ");
        if (influenceCost  > 0) sb.append(influenceCost).append(" influence  ");
        if (happinessMalus > 0) sb.append(happinessMalus).append(" happiness  ");
        if (favourCost     > 0) sb.append(favourCost).append(" favour");
        return sb.toString().trim();
    }

public boolean canAfford(main.resources.ResourcePool res, main.resources.StatBlock stats) {
        return res.getMoney()     >= moneyCost
            && res.getInfluence() >= influenceCost;
    }

    public void apply(main.resources.ResourcePool res, main.resources.StatBlock stats) {
        res.spendMoney(moneyCost);
        res.spendInfluence(influenceCost);
        if (happinessMalus > 0) stats.reduceHappiness(happinessMalus);
    }
}