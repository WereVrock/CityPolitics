package main.politics;

import debug.Debug;
import main.parameters.GameParameters;
import main.pops.Pop;
import main.pops.PopElectoralData;
import main.pops.PopManager;

import java.util.*;

/**
 * Manages the propaganda system for parties.
 *
 * Each turn, gold/manpower/influence paid to a party is converted to propaganda.
 * Half is banked for the election, half is spent on ideology spread each turn.
 */
public class PropagandaManager {

    /** Total propaganda accumulated since last spend. */
    private final Map<PoliticalParty, Double> totalPropaganda     = new LinkedHashMap<>();
    /** Half banked for election. */
    private final Map<PoliticalParty, Double> electionPropaganda  = new LinkedHashMap<>();
    /** Half available for ideology spread this turn. */
    private final Map<PoliticalParty, Double> spreadPropaganda    = new LinkedHashMap<>();

    private final Random rng = new Random();

    public PropagandaManager(List<PoliticalParty> parties) {
        for (PoliticalParty p : parties) {
            totalPropaganda   .put(p, 0.0);
            electionPropaganda.put(p, 0.0);
            spreadPropaganda  .put(p, 0.0);
        }
    }

    // ─── Propaganda accumulation ─────────────────────────────────────────────

    /**
     * Convert a resource payment to the party into propaganda.
     * The resource amount is converted at a fixed rate to propaganda units.
     */
    public void addPropaganda(PoliticalParty party, double amount) {
        if (amount <= 0) return;
        double half = amount / 2.0;
        electionPropaganda.merge(party, half, Double::sum);
        spreadPropaganda.merge(party,   half, Double::sum);
        Debug.log("propaganda", "add", party.getName() + " +" + amount
                + " election=" + electionPropaganda.get(party)
                + " spread=" + spreadPropaganda.get(party));
    }

    /**
     * Convert deal payment (gold + influence + happiness) into propaganda.
     * Called when a deal is struck by a party.
     */
    public void convertDealToPropaganda(PoliticalParty party, int gold, int influence) {
        double total = gold * GameParameters.PROPAGANDA_PER_GOLD
                + influence * GameParameters.PROPAGANDA_PER_INFLUENCE;
        addPropaganda(party, total);
    }

    // ─── Per-turn ideology spread ─────────────────────────────────────────────

    /**
     * Each turn, parties spend their spread propaganda to influence pops.
     * Returns log lines.
     */
    public List<String> processIdeologySpread(List<PoliticalParty> parties,
                                               PopManager popManager) {
        List<String> log = new ArrayList<>();
        List<Pop> allPops = new ArrayList<>(popManager.getPops());
        if (allPops.isEmpty()) return log;

        for (PoliticalParty party : parties) {
            double budget = spreadPropaganda.getOrDefault(party, 0.0);
            if (budget < GameParameters.PROPAGANDA_SPREAD_THRESHOLD) continue;

            // Pick views to spread (party's FOR or STRONGLY_FOR views)
            List<PolitcalView> spreadViews = new ArrayList<>();
            for (Map.Entry<PolitcalView, ViewStrength> e : party.getViews().entrySet()) {
                if (e.getValue().getMultiplier() >= 0.5) spreadViews.add(e.getKey());
            }
            if (spreadViews.isEmpty()) { spreadPropaganda.put(party, 0.0); continue; }

            // Affected pop count scales with budget
            int maxAffected = (int)(budget * GameParameters.PROPAGANDA_POPS_PER_UNIT);
            maxAffected = Math.max(1, Math.min(allPops.size(), maxAffected));
            int affected = 1 + rng.nextInt(maxAffected);

            // Shuffle and pick pops — centre-of-circle model: first selected = most affected
            List<Pop> shuffled = new ArrayList<>(allPops);
            Collections.shuffle(shuffled, rng);
            List<Pop> targetPops = shuffled.subList(0, affected);

            PolitcalView chosenView = spreadViews.get(rng.nextInt(spreadViews.size()));
            int spreadCount = 0;
            for (int i = 0; i < targetPops.size(); i++) {
                Pop pop = targetPops.get(i);
                // Intensity decreases with distance from center (index 0)
                double distanceFactor = 1.0 - ((double) i / targetPops.size());
                int maxIntensityChange = (int)(budget * distanceFactor
                        * GameParameters.PROPAGANDA_INTENSITY_PER_UNIT);
                maxIntensityChange = Math.max(1, maxIntensityChange);
                int intensityChange = 1 + rng.nextInt(maxIntensityChange);

                pop.getElectoralData().adjustIntensity(chosenView, intensityChange);
                spreadCount++;
            }

            spreadPropaganda.put(party, 0.0); // consume spread budget
            if (spreadCount > 0) {
                Debug.log("propaganda", "spread", party.getName()
                        + " view=" + chosenView + " pops=" + spreadCount
                        + " budget=" + budget);
            }
        }
        return log;
    }

    // ─── Accessors ───────────────────────────────────────────────────────────

    public double getElectionPropaganda(PoliticalParty party) {
        return electionPropaganda.getOrDefault(party, 0.0);
    }

    public void consumeElectionPropaganda(PoliticalParty party) {
        electionPropaganda.put(party, 0.0);
    }

    public Map<PoliticalParty, Double> getAllElectionPropaganda() {
        return Collections.unmodifiableMap(electionPropaganda);
    }

    public void reset() {
        totalPropaganda.replaceAll((k, v) -> 0.0);
        electionPropaganda.replaceAll((k, v) -> 0.0);
        spreadPropaganda.replaceAll((k, v) -> 0.0);
    }

    // ─── Save/load ───────────────────────────────────────────────────────────

    public void setElectionPropaganda(PoliticalParty party, double v) {
        electionPropaganda.put(party, v);
    }

    public void setSpreadPropaganda(PoliticalParty party, double v) {
        spreadPropaganda.put(party, v);
    }
}