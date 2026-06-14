package main.politics;

import debug.Debug;
import main.parameters.GameParameters;
import main.pops.Pop;
import main.pops.PopElectoralData;
import main.pops.PopManager;

import java.util.*;

/**
 * Runs the full election simulation every ELECTION_PERIOD_TURNS turns.
 *
 * Election order:
 * 1. Pops vote based on view matching + propaganda
 * 2. Parties buy unaffiliated non-voting-for-them pops (power-based)
 * 3. Parties with power > 70 steal votes (corruption-scaled total cheat)
 * 4. Votes counted → seats redistributed (fixed-seat parties excluded)
 * 5. Affiliation streaks updated
 * 6. Power drift applied each turn (separately via applyPowerDrift)
 */
public class ElectionManager {

    private int turnsSinceLastElection = 0;

    private final Random rng = new Random();

    // ─── Turn tick ────────────────────────────────────────────────────────────

    /**
     * Called each turn. Returns non-empty log if election fires.
     */
    public List<String> tick(List<PoliticalParty> parties,
                              PopManager popManager,
                              PropagandaManager propagandaManager,
                              int corruption) {
        turnsSinceLastElection++;
        List<String> log = new ArrayList<>();
        if (turnsSinceLastElection >= GameParameters.ELECTION_PERIOD_TURNS) {
            turnsSinceLastElection = 0;
            log.addAll(runElection(parties, popManager, propagandaManager, corruption));
        }
        return log;
    }

    /** Returns true if an election will fire next turn. */
    public boolean isElectionImminent() {
        return turnsSinceLastElection >= GameParameters.ELECTION_PERIOD_TURNS - 1;
    }

    public int getTurnsUntilElection() {
        return Math.max(0, GameParameters.ELECTION_PERIOD_TURNS - turnsSinceLastElection);
    }

    // ─── Power drift (applied every turn) ────────────────────────────────────

    /**
     * If a party has power > seats × POWER_PER_SEAT_THRESHOLD, power drops by 1.
     */
    public void applyPowerDrift(List<PoliticalParty> parties) {
        for (PoliticalParty party : parties) {
            if (isFixedSeat(party)) continue;
            int threshold = party.getSeats() * GameParameters.POWER_DRIFT_SEAT_MULTIPLIER;
            if (party.getPower() > threshold) {
                party.setPower(party.getPower() - 1);
            }
        }
    }

    // ─── Election ─────────────────────────────────────────────────────────────

    private List<String> runElection(List<PoliticalParty> parties,
                                      PopManager popManager,
                                      PropagandaManager propagandaManager,
                                      int corruption) {
        List<String> log = new ArrayList<>();
        log.add("══════════════════════════════════════");
        log.add("     ELECTION — VOTES ARE CAST");
        log.add("══════════════════════════════════════");

        List<PoliticalParty> electable = getElectableParties(parties);
        List<Pop> pops = new ArrayList<>(popManager.getPops());
        int totalPopCount = pops.stream().mapToInt(Pop::getCount).sum();

        // Step 1: Base votes from view matching + propaganda
        Map<PoliticalParty, Integer> votes = new LinkedHashMap<>();
        for (PoliticalParty p : parties) votes.put(p, 0);

        // Track how each pop chunk voted (for affiliation updates)
        Map<Pop, PoliticalParty> popNaturalVote  = new LinkedHashMap<>();
        Map<Pop, PoliticalParty> popFinalVote     = new LinkedHashMap<>();

        for (Pop pop : pops) {
            PoliticalParty naturalChoice = pickNaturalVote(pop, electable, propagandaManager);
            popNaturalVote.put(pop, naturalChoice);
            popFinalVote.put(pop, naturalChoice);
            if (naturalChoice != null) {
                votes.merge(naturalChoice, pop.getCount(), Integer::sum);
            }
        }
        Debug.log("election", "step1-natural", voteSummary(votes));

        // Step 2: Affiliation override — affiliated pops vote for their party
        for (Pop pop : pops) {
            PolitcalView affiliation = pop.getAffiliation();
            if (affiliation == PolitcalView.NONE) continue;

            PoliticalParty affiliatedParty = findPartyByAffiliation(affiliation, electable, parties);
            if (affiliatedParty == null) continue;

            PoliticalParty natural = popNaturalVote.get(pop);
            boolean overridden = !affiliatedParty.equals(natural);

            if (overridden) {
                // Remove from natural party, add to affiliated
                if (natural != null) votes.merge(natural, -pop.getCount(), Integer::sum);
                votes.merge(affiliatedParty, pop.getCount(), Integer::sum);
                popFinalVote.put(pop, affiliatedParty);
            }

            // Update streak
            PopElectoralData data = pop.getElectoralData();
            data.recordVote(affiliatedParty.getName(), overridden);

            // Check if affiliation should be lost (3 consecutive overrides)
            if (data.getConsecutiveOverrides() >= GameParameters.ELECTION_OVERRIDE_LOSS_THRESHOLD) {
                pop.setAffiliation(PolitcalView.NONE);
                data.setConsecutiveOverrides(0);
                log.add("  " + pop.getType().getDisplayName() + " pops lose party affiliation"
                        + " (overridden " + GameParameters.ELECTION_OVERRIDE_LOSS_THRESHOLD
                        + " times).");
                Debug.log("election", "affiliation-lost", pop.getType().name());
            }

            // Check if affiliation should be gained (3 votes for affiliated)
            // (This is for unaffiliated pops; skip if already affiliated)
        }

        // For unaffiliated pops: check if they should gain affiliation
        for (Pop pop : pops) {
            if (pop.getAffiliation() != PolitcalView.NONE) continue;
            PopElectoralData data = pop.getElectoralData();
            PoliticalParty voted = popFinalVote.get(pop);
            if (voted == null) continue;

            boolean consecutive = voted.getName().equals(data.getLastVotedPartyName());
            if (consecutive) {
                data.recordVote(voted.getName(), false);
            } else {
                data.setConsecutiveVotesForAffiliated(0);
                data.recordVote(voted.getName(), false);
            }

            if (data.getConsecutiveVotesForAffiliated()
                    >= GameParameters.ELECTION_AFFILIATION_GAIN_THRESHOLD) {
                // Gain affiliation — find the party's dominant view
                PolitcalView newAffiliation = getDominantView(voted);
                if (newAffiliation != PolitcalView.NONE) {
                    pop.setAffiliation(newAffiliation);
                    data.setConsecutiveVotesForAffiliated(0);
                    log.add("  " + pop.getType().getDisplayName() + " pops become affiliated with "
                            + voted.getName() + ".");
                    Debug.log("election", "affiliation-gained",
                            pop.getType().name() + " → " + voted.getName());
                }
            }
        }

        // Clamp votes to 0
        for (PoliticalParty p : parties) {
            votes.put(p, Math.max(0, votes.getOrDefault(p, 0)));
        }

        Debug.log("election", "step2-affiliations", voteSummary(votes));

        // Step 3: Vote buying (power-based, unaffiliated pops)
        buyVotes(electable, votes, pops, totalPopCount, log);
        Debug.log("election", "step3-buying", voteSummary(votes));

        // Step 4: Vote stealing (parties with power > 70, corruption-scaled)
        stealVotes(electable, votes, totalPopCount, corruption, log);
        Debug.log("election", "step4-stealing", voteSummary(votes));

        // Step 5: Count votes → redistribute seats among electable parties
        int totalVotes = votes.values().stream().mapToInt(v -> v).sum();
        if (totalVotes <= 0) totalVotes = 1;

        int totalSeatPool = electable.stream().mapToInt(PoliticalParty::getSeats).sum();
        // Also count fixed-seat parties for total
        // Only redistribute electable seats
        int electableSeatPool = totalSeatPool;

        Map<PoliticalParty, Integer> newSeats = distributeSeats(electable, votes, totalVotes,
                electableSeatPool);

        log.add("─────────────────────────────────────");
        log.add("ELECTION RESULTS:");
        for (PoliticalParty party : electable) {
            int oldSeats  = party.getSeats();
            int seats     = newSeats.getOrDefault(party, 0);
            int v         = votes.getOrDefault(party, 0);
            double pct    = totalVotes > 0 ? (double)v / totalVotes * 100 : 0;
            String change = seats > oldSeats ? "▲" : seats < oldSeats ? "▼" : "─";
            log.add(String.format("  %-22s %5.1f%%  %2d seats %s",
                    party.getName(), pct, seats, change));
        }
        // Fixed-seat parties
        for (PoliticalParty party : parties) {
            if (isFixedSeat(party)) {
                log.add(String.format("  %-22s fixed    %2d seats",
                        party.getName(), party.getSeats()));
            }
        }
        log.add("══════════════════════════════════════");

        // Apply new seats
        for (PoliticalParty p : electable) {
            p.setSeats(newSeats.getOrDefault(p, 0));
        }

        // Step 6: Consume election propaganda
        for (PoliticalParty p : electable) {
            propagandaManager.consumeElectionPropaganda(p);
        }

        return log;
    }

    // ─── Natural vote ─────────────────────────────────────────────────────────

    private PoliticalParty pickNaturalVote(Pop pop, List<PoliticalParty> electable,
                                            PropagandaManager propagandaManager) {
        if (electable.isEmpty()) return null;

        Map<PoliticalParty, Double> scores = new LinkedHashMap<>();
        for (PoliticalParty party : electable) {
            double score = computeViewMatchScore(pop, party);
            // Propaganda bonus (election budget → extra score)
            double propaganda = propagandaManager.getElectionPropaganda(party);
            score += propaganda * GameParameters.PROPAGANDA_VOTE_BONUS_PER_UNIT;
            // Prestige bonus
            score += party.getPrestige() * GameParameters.ELECTION_PRESTIGE_WEIGHT;
            scores.put(party, Math.max(0, score));
        }

        double total = scores.values().stream().mapToDouble(v -> v).sum();
        if (total <= 0) return electable.get(rng.nextInt(electable.size()));

        // Weighted random pick
        double roll = rng.nextDouble() * total;
        double cum  = 0;
        for (Map.Entry<PoliticalParty, Double> e : scores.entrySet()) {
            cum += e.getValue();
            if (roll <= cum) return e.getKey();
        }
        return electable.get(electable.size() - 1);
    }

    private double computeViewMatchScore(Pop pop, PoliticalParty party) {
        double score = 0;
        for (Map.Entry<PolitcalView, Integer> viewEntry
                : pop.getElectoralData().getViewIntensities().entrySet()) {
            PolitcalView view      = viewEntry.getKey();
            int          intensity = viewEntry.getValue();
            ViewStrength strength  = party.getViewStrength(view);
            score += intensity * strength.getMultiplier();
        }
        return score;
    }

    // ─── Vote buying ──────────────────────────────────────────────────────────

    private void buyVotes(List<PoliticalParty> electable,
                          Map<PoliticalParty, Integer> votes,
                          List<Pop> pops,
                          int totalPopCount,
                          List<String> log) {
        // Identify unaffiliated pops (not already voting for this party via affiliation)
        int unaffiliatedCount = 0;
        for (Pop pop : pops) {
            if (pop.getAffiliation() == PolitcalView.NONE) {
                unaffiliatedCount += pop.getCount();
            }
        }
        if (unaffiliatedCount <= 0) return;

        double maxValue = (double) unaffiliatedCount / 10.0;

        // Track which pops have been bought (by count pools)
        Set<PoliticalParty> buyingParties = new LinkedHashSet<>();
        Map<PoliticalParty, Integer> toBuy = new LinkedHashMap<>();

        for (PoliticalParty party : electable) {
            int power = party.getPower();
            if (power <= 0) continue;
            double buyMax = (power / 100.0) * maxValue;
            double buyMin = buyMax / 10.0;
            int bought = (int)(buyMin + rng.nextDouble() * (buyMax - buyMin));
            bought = Math.max(0, bought);
            if (bought > 0) {
                toBuy.put(party, bought);
                buyingParties.add(party);
            }
        }

        // Distribute buys — no party can buy the same voter twice
        int remainingPool = unaffiliatedCount;
        for (PoliticalParty party : buyingParties) {
            int amount = Math.min(toBuy.getOrDefault(party, 0), remainingPool);
            if (amount <= 0) continue;
            votes.merge(party, amount, Integer::sum);
            remainingPool -= amount;
            Debug.log("election", "vote-buying",
                    party.getName() + " bought " + amount + " votes");
        }
    }

    // ─── Vote stealing ────────────────────────────────────────────────────────

    private void stealVotes(List<PoliticalParty> electable,
                             Map<PoliticalParty, Integer> votes,
                             int totalPopCount,
                             int corruption,
                             List<String> log) {
        // Total cheat amount: 5% at 0 corruption, asymptotically approaches 20%
        double corruptionFraction = corruption / 100.0;
        double totalCheatFraction = 0.05 + 0.15 * (1.0 - Math.exp(-3.0 * corruptionFraction));
        int totalCheatVotes       = (int)(totalPopCount * totalCheatFraction);
        if (totalCheatVotes <= 0) return;

        // Parties with power > 70 get a cheat power value
        Map<PoliticalParty, Integer> cheatPowers = new LinkedHashMap<>();
        for (PoliticalParty party : electable) {
            if (party.getPower() > 70) {
                int cheatPower = 5 + rng.nextInt(party.getPower() - 5 + 1);
                cheatPowers.put(party, cheatPower);
            }
        }
        if (cheatPowers.isEmpty()) return;

        // Sort by cheat power ascending (lower cheaters go first, can be stolen from)
        List<Map.Entry<PoliticalParty, Integer>> sorted = new ArrayList<>(cheatPowers.entrySet());
        sorted.sort(Map.Entry.comparingByValue());

        int totalCheatPower = sorted.stream().mapToInt(Map.Entry::getValue).sum();
        if (totalCheatPower <= 0) return;

        // Each party steals votes proportional to their cheat power share
        for (Map.Entry<PoliticalParty, Integer> entry : sorted) {
            PoliticalParty thief   = entry.getKey();
            int            cPower  = entry.getValue();
            double         share   = (double) cPower / totalCheatPower;
            int            toSteal = (int)(totalCheatVotes * share);
            if (toSteal <= 0) continue;

            // Steal from all other parties proportionally
            int stolen = 0;
            for (PoliticalParty victim : electable) {
                if (victim == thief) continue;
                int victimVotes    = votes.getOrDefault(victim, 0);
                int fromVictim     = (int)(victimVotes * share);
                fromVictim         = Math.min(fromVictim, Math.min(toSteal - stolen, victimVotes));
                if (fromVictim <= 0) continue;
                votes.merge(victim, -fromVictim, Integer::sum);
                votes.merge(thief,   fromVictim, Integer::sum);
                stolen += fromVictim;
                if (stolen >= toSteal) break;
            }
            Debug.log("election", "vote-stealing", thief.getName()
                    + " stole " + stolen + " votes (cheatPower=" + cPower + ")");
        }
    }

    // ─── Seat distribution ────────────────────────────────────────────────────

    private Map<PoliticalParty, Integer> distributeSeats(List<PoliticalParty> electable,
                                                          Map<PoliticalParty, Integer> votes,
                                                          int totalVotes,
                                                          int seatPool) {
        // Largest remainder method
        Map<PoliticalParty, Double> quotas  = new LinkedHashMap<>();
        Map<PoliticalParty, Integer> seats  = new LinkedHashMap<>();
        Map<PoliticalParty, Double> remainders = new LinkedHashMap<>();

        for (PoliticalParty p : electable) {
            double quota = (double) votes.getOrDefault(p, 0) / totalVotes * seatPool;
            seats.put(p, (int) quota);
            remainders.put(p, quota - (int) quota);
            quotas.put(p, quota);
        }

        int assigned = seats.values().stream().mapToInt(v -> v).sum();
        int remaining = seatPool - assigned;

        // Give remaining seats to parties with largest remainders
        List<PoliticalParty> byRemainder = new ArrayList<>(electable);
        byRemainder.sort((a, b) -> Double.compare(
                remainders.getOrDefault(b, 0.0), remainders.getOrDefault(a, 0.0)));

        for (int i = 0; i < remaining && i < byRemainder.size(); i++) {
            seats.merge(byRemainder.get(i), 1, Integer::sum);
        }

        // Ensure minimum 1 seat per party that got votes
        for (PoliticalParty p : electable) {
            if (votes.getOrDefault(p, 0) > 0 && seats.getOrDefault(p, 0) == 0) {
                seats.put(p, 1);
            }
        }

        return seats;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private boolean isFixedSeat(PoliticalParty party) {
        // Oracles are fixed-seat; in future, nobles panel party could also be fixed
        return party.getName().equals("Oracles");
    }

    private List<PoliticalParty> getElectableParties(List<PoliticalParty> parties) {
        List<PoliticalParty> result = new ArrayList<>();
        for (PoliticalParty p : parties) {
            if (!isFixedSeat(p)) result.add(p);
        }
        return result;
    }

    private PoliticalParty findPartyByAffiliation(PolitcalView view,
                                                    List<PoliticalParty> electable,
                                                    List<PoliticalParty> all) {
        // Find party that most strongly holds this view
        PoliticalParty best    = null;
        double         bestMul = 0;
        for (PoliticalParty p : all) {
            double m = p.getViewStrength(view).getMultiplier();
            if (m > bestMul) { bestMul = m; best = p; }
        }
        return best;
    }

    private PolitcalView getDominantView(PoliticalParty party) {
        PolitcalView best   = PolitcalView.NONE;
        double bestStrength = 0;
        for (Map.Entry<PolitcalView, ViewStrength> e : party.getViews().entrySet()) {
            if (e.getValue().getMultiplier() > bestStrength) {
                bestStrength = e.getValue().getMultiplier();
                best = e.getKey();
            }
        }
        return best;
    }

    private String voteSummary(Map<PoliticalParty, Integer> votes) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<PoliticalParty, Integer> e : votes.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(e.getKey().getName()).append(":").append(e.getValue());
        }
        return sb.toString();
    }

    // ─── Save/load ───────────────────────────────────────────────────────────

    public int  getTurnsSinceLastElection()             { return turnsSinceLastElection; }
    public void setTurnsSinceLastElection(int v)        { turnsSinceLastElection = v; }
}