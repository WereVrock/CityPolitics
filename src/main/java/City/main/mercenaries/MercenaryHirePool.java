package City.main.mercenaries;

import City.debug.Debug;
 
import City.main.parameters.MercenaryParams;
import City.main.parameters.PlayerArmyParams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Holds the pool of mercenary armies available for hire each turn.
 * Refreshed at the start of each turn with 1–4 random armies of size 100–500.
 */
public class MercenaryHirePool {

    public static class MercenaryOffer {
        private final String name;
        private final int    size;
        private final int    goldCost;    // one-time hire fee
        private       boolean hired;

        public MercenaryOffer(String name, int size, int goldCost) {
            this.name     = name;
            this.size     = size;
            this.goldCost = goldCost;
            this.hired    = false;
        }

        public String  getName()     { return name; }
        public int     getSize()     { return size; }
        public int     getGoldCost() { return goldCost; }
        public boolean isHired()     { return hired; }
        public void    markHired()   { this.hired = true; }

        public double getUpkeepPerTurn() {
            return size
                    * PlayerArmyParams.SOLDIER_UPKEEP_GOLD
                    * MercenaryParams.MERCENARY_COST_MULTIPLIER;
        }
    }

    private static final Random RNG = new Random();

    private final List<MercenaryOffer> offers = new ArrayList<>();

    public MercenaryHirePool() {
        refresh();
    }

    /** Called at the start of each turn to generate fresh offers. */
    public void refresh() {
        offers.clear();
        int count = MercenaryParams.MERCENARY_POOL_MIN_COUNT
                + RNG.nextInt(MercenaryParams.MERCENARY_POOL_MAX_COUNT
                              - MercenaryParams.MERCENARY_POOL_MIN_COUNT + 1);
        for (int i = 0; i < count; i++) {
            offers.add(generateOffer());
        }
        Debug.log("merc-pool", "refresh", "Generated " + count + " mercenary offers");
    }

    private MercenaryOffer generateOffer() {
        int    size     = MercenaryParams.MERCENARY_POOL_MIN_SIZE
                + RNG.nextInt(MercenaryParams.MERCENARY_POOL_MAX_SIZE
                              - MercenaryParams.MERCENARY_POOL_MIN_SIZE + 1);
        double baseGold = size
                * PlayerArmyParams.SOLDIER_RECRUIT_GOLD_COST
                * MercenaryParams.MERCENARY_COST_MULTIPLIER;
        double variance = 1.0 + (RNG.nextDouble() * 2 - 1)
                * MercenaryParams.MERCENARY_RECRUIT_COST_VARIANCE;
        int goldCost    = (int) Math.ceil(baseGold * variance);
        String name     = MercenaryNameGenerator.generate();
        return new MercenaryOffer(name, size, goldCost);
    }

    public List<MercenaryOffer> getOffers() {
        return Collections.unmodifiableList(offers);
    }

    /** Returns only offers that have not yet been hired this turn. */
    public List<MercenaryOffer> getAvailableOffers() {
        List<MercenaryOffer> result = new ArrayList<>();
        for (MercenaryOffer o : offers) {
            if (!o.isHired()) result.add(o);
        }
        return result;
    }
}