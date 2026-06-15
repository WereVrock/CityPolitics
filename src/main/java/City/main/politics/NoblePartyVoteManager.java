package City.main.politics;

import City.main.nobles.NobleHouse;
import City.main.nobles.NobleHouseManager;

import java.util.*;

/**
 * Manages the "Noble Houses" political party — a fixed 3-seat party in the assembly.
 * The top 5 houses by prestige hold an internal vote based on their opinion of the player.
 * Results are visible to the player before finalization.
 * If all abstain, the outcome is randomized and shown as UNKNOWN until vote resolves.
 */
public class NoblePartyVoteManager {

    public static final String NOBLE_PARTY_NAME = "Noble Houses";
    public static final int    NOBLE_PARTY_SEATS = 3;

    public enum NobleVoteStance { YES, NO, ABSTAIN }

    public static class NobleVoterEntry {
        public final NobleHouse house;
        public final int        prestige;
        public final int        playerOpinion;
        public final NobleVoteStance stance;

        public NobleVoterEntry(NobleHouse house, int prestige, int playerOpinion, NobleVoteStance stance) {
            this.house         = house;
            this.prestige      = prestige;
            this.playerOpinion = playerOpinion;
            this.stance        = stance;
        }
    }

    public static class NoblePartyVoteResult {
        /** The five noble voters and their individual stances. */
        public final List<NobleVoterEntry> voters;
        /** The unified stance the party will vote with. ABSTAIN means unknown/random. */
        public final NobleVoteStance       unifiedStance;
        /** True if the unified result is unknown (all abstained → random). */
        public final boolean               isUnknown;
        /** Total prestige weight on YES side. */
        public final int                   yesWeight;
        /** Total prestige weight on NO side. */
        public final int                   noWeight;
        /** Total prestige weight abstaining. */
        public final int                   abstainWeight;

        public NoblePartyVoteResult(List<NobleVoterEntry> voters, NobleVoteStance unifiedStance,
                                     boolean isUnknown, int yesWeight, int noWeight, int abstainWeight) {
            this.voters        = Collections.unmodifiableList(voters);
            this.unifiedStance = unifiedStance;
            this.isUnknown     = isUnknown;
            this.yesWeight     = yesWeight;
            this.noWeight      = noWeight;
            this.abstainWeight = abstainWeight;
        }
    }

    private static final int    TOP_N_NOBLES        = 5;
    private static final int    ABSTAIN_LOWER        = 45;
    private static final int    ABSTAIN_UPPER        = 55;
    private static final Random RNG                  = new Random();

    private NoblePartyVoteManager() {}

    public static int NOBLE_PARTY_ABSTAIN_LOWER() { return ABSTAIN_LOWER; }
    public static int NOBLE_PARTY_ABSTAIN_UPPER() { return ABSTAIN_UPPER; }

    /**
     * Compute the noble party vote result for a given set of houses.
     * Called when building the vote session so the result can be shown in the UI.
     */
    public static NoblePartyVoteResult computeVote(NobleHouseManager nobleHouseManager) {
        List<NobleHouse> all = new ArrayList<>(nobleHouseManager.getHouses());
        all.removeIf(NobleHouse::isEliminated);
        // Sort by prestige descending, take top 5
        all.sort(Comparator.comparingInt(NobleHouse::getPrestige).reversed());
        List<NobleHouse> voters = all.subList(0, Math.min(TOP_N_NOBLES, all.size()));

        List<NobleVoterEntry> entries = new ArrayList<>();
        int yesWeight     = 0;
        int noWeight      = 0;
        int abstainWeight = 0;

        for (NobleHouse house : voters) {
            int opinion = house.getPlayerOpinion();
            NobleVoteStance stance;
            if (opinion >= ABSTAIN_LOWER && opinion <= ABSTAIN_UPPER) {
                stance = NobleVoteStance.ABSTAIN;
            } else if (opinion > ABSTAIN_UPPER) {
                stance = NobleVoteStance.YES;
            } else {
                stance = NobleVoteStance.NO;
            }
            entries.add(new NobleVoterEntry(house, house.getPrestige(), opinion, stance));
            switch (stance) {
                case YES     -> yesWeight     += house.getPrestige();
                case NO      -> noWeight      += house.getPrestige();
                case ABSTAIN -> abstainWeight += house.getPrestige();
            }
        }

        NobleVoteStance unified;
        boolean isUnknown = false;

        if (yesWeight > noWeight) {
            unified = NobleVoteStance.YES;
        } else if (noWeight > yesWeight) {
            unified = NobleVoteStance.NO;
        } else if (yesWeight == 0 && noWeight == 0) {
            // All abstained
            unified   = NobleVoteStance.ABSTAIN;
            isUnknown = true;
        } else {
            // Tie — random, unknown
            unified   = NobleVoteStance.ABSTAIN;
            isUnknown = true;
        }

        return new NoblePartyVoteResult(entries, unified, isUnknown, yesWeight, noWeight, abstainWeight);
    }

    /**
     * Resolve the actual vote when all abstained (random).
     * Called at finalization time.
     */
    public static NobleVoteStance resolveUnknown() {
        return RNG.nextBoolean() ? NobleVoteStance.YES : NobleVoteStance.NO;
    }

    /**
     * Convert unified stance to PartyVoteIntent for the VotingSession.
     */
    public static VotingSession.PartyVoteIntent toIntent(NobleVoteStance stance) {
        return switch (stance) {
            case YES     -> VotingSession.PartyVoteIntent.YES;
            case NO      -> VotingSession.PartyVoteIntent.NO;
            case ABSTAIN -> VotingSession.PartyVoteIntent.UNKNOWN;
        };
    }
}