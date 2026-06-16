package City.main.politics;

import City.main.pops.Pop;
import City.main.pops.PopManager;

import java.util.*;

/**
 * Calculates a rough election survey estimate shown during campaign period.
 * Each turn generates a randomization seed per party. Within the turn, resource
 * donations use that same seed so the display is consistent but reflects changes.
 */
public class ElectionSurveyCalculator {

    /** Per-party randomization factor generated once per turn: 0.85 – 1.15 */
    private final Map<String, Double> randomFactors = new LinkedHashMap<>();
    private final Random rng = new Random();

    public static class SurveyResult {
        public final String partyName;
        public final int    estimatedVotes;
        public final double votePct;

        public SurveyResult(String partyName, int estimatedVotes, double votePct) {
            this.partyName      = partyName;
            this.estimatedVotes = estimatedVotes;
            this.votePct        = votePct;
        }
    }

    /** Call once at the start of each new turn to refresh randomization seeds. */
    public void regenerateFactors(List<PoliticalParty> parties) {
        randomFactors.clear();
        for (PoliticalParty p : parties) {
            if (p.isUnelected()) continue;
            double factor = 0.85 + rng.nextDouble() * 0.30; // ±15%
            randomFactors.put(p.getName(), factor);
        }
    }

    /**
     * Compute survey estimates based on current pop views + propaganda + power (vote buying).
     * Uses pre-generated random factors so changes within same turn scale consistently.
     */
    public List<SurveyResult> compute(List<PoliticalParty> parties,
                                       PopManager popManager,
                                       PropagandaManager propagandaManager) {
        List<PoliticalParty> electable = new ArrayList<>();
        for (PoliticalParty p : parties) {
            if (!p.isUnelected()) electable.add(p);
        }
        if (electable.isEmpty()) return List.of();

        // Natural votes: view-match scores
        Map<String, Double> rawScores = new LinkedHashMap<>();
        int totalPops = 0;
        for (Pop pop : popManager.getPops()) totalPops += pop.getCount();

        for (PoliticalParty party : electable) {
            double score = 0;
            for (Pop pop : popManager.getPops()) {
                for (Map.Entry<PolitcalView, Integer> viewEntry
                        : pop.getElectoralData().getViewIntensities().entrySet()) {
                    double mult = party.getViewStrength(viewEntry.getKey()).getMultiplier();
                    score += Math.max(0, viewEntry.getValue() * mult) * pop.getCount();
                }
            }
            // Add propaganda bonus
            double prop = propagandaManager.getElectionPropaganda(party);
            score += prop * City.main.parameters.PoliticalParams.PROPAGANDA_VOTE_BONUS_PER_UNIT * totalPops;
            // Add power-based vote buying estimate
            double buyEst = (party.getPower() / 100.0) * (totalPops * 0.05);
            score += buyEst;

            // Apply randomization factor
            double factor = randomFactors.getOrDefault(party.getName(), 1.0);
            rawScores.put(party.getName(), Math.max(1, score * factor));
        }

        double total = rawScores.values().stream().mapToDouble(v -> v).sum();
        if (total <= 0) total = 1;

        List<SurveyResult> results = new ArrayList<>();
        for (PoliticalParty party : electable) {
            double raw   = rawScores.getOrDefault(party.getName(), 0.0);
            double pct   = raw / total * 100.0;
            int    votes = (int) raw;
            results.add(new SurveyResult(party.getName(), votes, pct));
        }
        results.sort((a, b) -> Double.compare(b.votePct, a.votePct));
        return results;
    }
}