package City.main.politics;

import City.debug.Debug;
import City.main.parameters.GameParameters;
import City.main.pops.Pop;
import City.main.pops.PopElectoralData;
import City.main.pops.PopManager;

import java.util.*;

/**
 * Runs the full election simulation every ELECTION_PERIOD_TURNS turns.
 * Produces a structured ElectionRecord alongside log lines.
 */
public class ElectionManager {

    private int turnsSinceLastElection = 0;

    /** Last election result — shown in ElectionResultsPanel. */
    private ElectionRecord lastRecord = null;

    private final Random rng = new Random();

    // ─── Turn tick ────────────────────────────────────────────────────────────

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

    public boolean isElectionImminent() {
        return turnsSinceLastElection >= GameParameters.ELECTION_PERIOD_TURNS - 1;
    }

    public int getTurnsUntilElection() {
        return Math.max(0, GameParameters.ELECTION_PERIOD_TURNS - turnsSinceLastElection);
    }

    public ElectionRecord getLastRecord() { return lastRecord; }

    // ─── Power drift ─────────────────────────────────────────────────────────

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

    private int  lastElectionYear   = 0;
    private String lastElectionPeriod = "";

    public void setCalendarContext(int year, String period) {
        this.lastElectionYear   = year;
        this.lastElectionPeriod = period;
    }

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

        // Snapshot seats and power before changes
        Map<PoliticalParty, Integer> seatsBefore = new LinkedHashMap<>();
        Map<PoliticalParty, Integer> powerBefore = new LinkedHashMap<>();
        for (PoliticalParty p : parties) {
            seatsBefore.put(p, p.getSeats());
            powerBefore.put(p, p.getPower());
        }

        // Snapshot propaganda before consumption
        Map<String, Double> propagandaSnapshot = new LinkedHashMap<>();
        for (PoliticalParty p : electable) {
            propagandaSnapshot.put(p.getName(), propagandaManager.getElectionPropaganda(p));
        }

        // Step 1: Natural votes
        Map<PoliticalParty, Integer> votes = new LinkedHashMap<>();
        for (PoliticalParty p : parties) votes.put(p, 0);

        Map<Pop, PoliticalParty> popNaturalVote = new LinkedHashMap<>();
        Map<Pop, PoliticalParty> popFinalVote   = new LinkedHashMap<>();

        // Track natural votes per party
        Map<PoliticalParty, Integer> naturalVotesMap = new LinkedHashMap<>();
        for (PoliticalParty p : parties) naturalVotesMap.put(p, 0);

        for (Pop pop : pops) {
            PoliticalParty naturalChoice = pickNaturalVote(pop, electable, propagandaManager);
            popNaturalVote.put(pop, naturalChoice);
            popFinalVote.put(pop, naturalChoice);
            if (naturalChoice != null) {
                votes.merge(naturalChoice, pop.getCount(), Integer::sum);
                naturalVotesMap.merge(naturalChoice, pop.getCount(), Integer::sum);
            }
        }

        // Step 2: Affiliation overrides
        List<ElectionRecord.AffiliationChange> affiliationChanges = new ArrayList<>();

        for (Pop pop : pops) {
            PolitcalView affiliation = pop.getAffiliation();
            if (affiliation == PolitcalView.NONE) continue;

            PoliticalParty affiliatedParty = findPartyByAffiliation(affiliation, electable, parties);
            if (affiliatedParty == null) continue;

            PoliticalParty natural = popNaturalVote.get(pop);
            boolean overridden = !affiliatedParty.equals(natural);

            if (overridden) {
                if (natural != null) votes.merge(natural, -pop.getCount(), Integer::sum);
                votes.merge(affiliatedParty, pop.getCount(), Integer::sum);
                popFinalVote.put(pop, affiliatedParty);
            }

            PopElectoralData data = pop.getElectoralData();
            data.recordVote(affiliatedParty.getName(), overridden);

            if (data.getConsecutiveOverrides() >= GameParameters.ELECTION_AFFILIATION_GAIN_THRESHOLD) {
                String oldAff = pop.getAffiliation().getDisplayName();
                pop.setAffiliation(PolitcalView.NONE);
                data.setConsecutiveOverrides(0);
                affiliationChanges.add(new ElectionRecord.AffiliationChange(
                        pop.getType().getDisplayName(), oldAff, null, false));
                log.add("  " + pop.getType().getDisplayName() + " pops lose party affiliation.");
                Debug.log("election", "affiliation-lost", pop.getType().name());
            }
        }

        // Unaffiliated pops: check affiliation gain
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
                PolitcalView newAffiliation = getDominantView(voted);
                if (newAffiliation != PolitcalView.NONE) {
                    String oldAff = pop.getAffiliation().getDisplayName();
                    pop.setAffiliation(newAffiliation);
                    data.setConsecutiveVotesForAffiliated(0);
                    affiliationChanges.add(new ElectionRecord.AffiliationChange(
                            pop.getType().getDisplayName(), oldAff,
                            newAffiliation.getDisplayName(), true));
                    log.add("  " + pop.getType().getDisplayName() + " pops affiliate with "
                            + voted.getName() + ".");
                    Debug.log("election", "affiliation-gained",
                            pop.getType().name() + " → " + voted.getName());
                }
            }
        }

        for (PoliticalParty p : parties) {
            votes.put(p, Math.max(0, votes.getOrDefault(p, 0)));
        }

        // Step 3: Vote buying
        Map<PoliticalParty, Integer> boughtVotesMap = new LinkedHashMap<>();
        for (PoliticalParty p : parties) boughtVotesMap.put(p, 0);
        buyVotes(electable, votes, pops, totalPopCount, boughtVotesMap, log);

        // Step 4: Vote stealing
        Map<PoliticalParty, Integer> stolenFromMap = new LinkedHashMap<>();
        for (PoliticalParty p : parties) stolenFromMap.put(p, 0);
        int totalStolenVotes = stealVotes(electable, votes, totalPopCount,
                corruption, stolenFromMap, log);

        // Step 5: Seat distribution
        int totalVotes = Math.max(1, votes.values().stream().mapToInt(v -> v).sum());
        int electableSeatPool = electable.stream().mapToInt(PoliticalParty::getSeats).sum();
        Map<PoliticalParty, Integer> newSeats = distributeSeats(electable, votes,
                totalVotes, electableSeatPool);

        log.add("─────────────────────────────────────");
        log.add("ELECTION RESULTS:");

        // Build party results and apply seats
        List<ElectionRecord.PartyResult> partyResults = new ArrayList<>();

        for (PoliticalParty party : parties) {
            boolean fixed    = isFixedSeat(party);
            int     after    = fixed ? party.getSeats() : newSeats.getOrDefault(party, 0);
            int     before   = seatsBefore.getOrDefault(party, 0);
            int     pBefore  = powerBefore.getOrDefault(party, party.getPower());
            int     natural  = naturalVotesMap.getOrDefault(party, 0);
            int     bought   = boughtVotesMap.getOrDefault(party, 0);
            int     stolen   = stolenFromMap.getOrDefault(party, 0);
            int     total    = votes.getOrDefault(party, 0);
            double  pct      = fixed ? 0.0 : (double) total / totalVotes * 100.0;
            int     pAfter   = party.getPower();

            if (!fixed) {
                int change   = after - before;
                String arrow = change > 0 ? "▲" : change < 0 ? "▼" : "─";
                log.add(String.format("  %-22s %5.1f%%  %2d seats %s",
                        party.getName(), pct, after, arrow));
            } else {
                log.add(String.format("  %-22s fixed    %2d seats", party.getName(), party.getSeats()));
            }

            partyResults.add(new ElectionRecord.PartyResult(
                    party.getName(), after, before,
                    natural, bought, stolen, total, pct,
                    pBefore, pAfter, fixed));
        }

        log.add("══════════════════════════════════════");

        // Apply new seats
        for (PoliticalParty p : electable) {
            p.setSeats(newSeats.getOrDefault(p, 0));
        }

        // Update power after to reflect seat application
        List<ElectionRecord.PartyResult> correctedResults = new ArrayList<>();
        for (ElectionRecord.PartyResult r : partyResults) {
            PoliticalParty p = findPartyByName(r.partyName, parties);
            int powerNow = p != null ? p.getPower() : r.powerAfter;
            correctedResults.add(new ElectionRecord.PartyResult(
                    r.partyName, r.seatsAfter, r.seatsBefore,
                    r.naturalVotes, r.boughtVotes, r.stolenVotes,
                    r.totalVotes, r.votePct,
                    r.powerBefore, powerNow, r.isFixedSeat));
        }

        // Step 6: Consume propaganda
        for (PoliticalParty p : electable) {
            propagandaManager.consumeElectionPropaganda(p);
        }

        // Build and store record
        lastRecord = new ElectionRecord(
                lastElectionYear, lastElectionPeriod,
                totalVotes, corruption, totalStolenVotes,
                correctedResults, affiliationChanges, propagandaSnapshot);

        Debug.log("election", "record-built",
                "parties=" + partyResults.size()
                + " totalVotes=" + totalVotes
                + " stolen=" + totalStolenVotes);

        return log;
    }

    // ─── Natural vote ─────────────────────────────────────────────────────────

    private PoliticalParty pickNaturalVote(Pop pop, List<PoliticalParty> electable,
                                            PropagandaManager propagandaManager) {
        if (electable.isEmpty()) return null;

        Map<PoliticalParty, Double> scores = new LinkedHashMap<>();
        for (PoliticalParty party : electable) {
            double score = computeViewMatchScore(pop, party);
            double propaganda = propagandaManager.getElectionPropaganda(party);
            score += propaganda * GameParameters.PROPAGANDA_VOTE_BONUS_PER_UNIT;
            score += party.getPrestige() * GameParameters.ELECTION_PRESTIGE_WEIGHT;
            scores.put(party, Math.max(0, score));
        }

        double total = scores.values().stream().mapToDouble(v -> v).sum();
        if (total <= 0) return electable.get(rng.nextInt(electable.size()));

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
                          Map<PoliticalParty, Integer> boughtVotesMap,
                          List<String> log) {
        int unaffiliatedCount = 0;
        for (Pop pop : pops) {
            if (pop.getAffiliation() == PolitcalView.NONE) unaffiliatedCount += pop.getCount();
        }
        if (unaffiliatedCount <= 0) return;

        double maxValue = (double) unaffiliatedCount / 10.0;

        Map<PoliticalParty, Integer> toBuy = new LinkedHashMap<>();
        for (PoliticalParty party : electable) {
            int power = party.getPower();
            if (power <= 0) continue;
            double buyMax = (power / 100.0) * maxValue;
            double buyMin = buyMax / 10.0;
            int bought = (int)(buyMin + rng.nextDouble() * (buyMax - buyMin));
            if (bought > 0) toBuy.put(party, bought);
        }

        int remainingPool = unaffiliatedCount;
        for (Map.Entry<PoliticalParty, Integer> entry : toBuy.entrySet()) {
            PoliticalParty party  = entry.getKey();
            int            amount = Math.min(entry.getValue(), remainingPool);
            if (amount <= 0) continue;
            votes.merge(party, amount, Integer::sum);
            boughtVotesMap.merge(party, amount, Integer::sum);
            remainingPool -= amount;
            Debug.log("election", "vote-buying",
                    party.getName() + " bought " + amount + " votes");
        }
    }

    // ─── Vote stealing ────────────────────────────────────────────────────────

    private int stealVotes(List<PoliticalParty> electable,
                            Map<PoliticalParty, Integer> votes,
                            int totalPopCount,
                            int corruption,
                            Map<PoliticalParty, Integer> stolenFromMap,
                            List<String> log) {
        double corruptionFraction = corruption / 100.0;
        double totalCheatFraction = 0.05 + 0.15 * (1.0 - Math.exp(-3.0 * corruptionFraction));
        int totalCheatVotes       = (int)(totalPopCount * totalCheatFraction);
        if (totalCheatVotes <= 0) return 0;

        Map<PoliticalParty, Integer> cheatPowers = new LinkedHashMap<>();
        for (PoliticalParty party : electable) {
            if (party.getPower() > 70) {
                int cheatPower = 5 + rng.nextInt(party.getPower() - 5 + 1);
                cheatPowers.put(party, cheatPower);
            }
        }
        if (cheatPowers.isEmpty()) return 0;

        List<Map.Entry<PoliticalParty, Integer>> sorted = new ArrayList<>(cheatPowers.entrySet());
        sorted.sort(Map.Entry.comparingByValue());

        int totalCheatPower = sorted.stream().mapToInt(Map.Entry::getValue).sum();
        if (totalCheatPower <= 0) return 0;

        int totalActualStolen = 0;
        for (Map.Entry<PoliticalParty, Integer> entry : sorted) {
            PoliticalParty thief  = entry.getKey();
            int            cPower = entry.getValue();
            double         share  = (double) cPower / totalCheatPower;
            int            toSteal = (int)(totalCheatVotes * share);
            if (toSteal <= 0) continue;

            int stolen = 0;
            for (PoliticalParty victim : electable) {
                if (victim == thief) continue;
                int victimVotes = votes.getOrDefault(victim, 0);
                int fromVictim  = (int)(victimVotes * share);
                fromVictim      = Math.min(fromVictim, Math.min(toSteal - stolen, victimVotes));
                if (fromVictim <= 0) continue;
                votes.merge(victim, -fromVictim, Integer::sum);
                votes.merge(thief,   fromVictim, Integer::sum);
                stolenFromMap.merge(victim, fromVictim, Integer::sum);
                stolen += fromVictim;
                if (stolen >= toSteal) break;
            }
            totalActualStolen += stolen;
            Debug.log("election", "vote-stealing",
                    thief.getName() + " stole " + stolen + " votes");
        }
        return totalActualStolen;
    }

    // ─── Seat distribution ────────────────────────────────────────────────────

    private Map<PoliticalParty, Integer> distributeSeats(List<PoliticalParty> electable,
                                                          Map<PoliticalParty, Integer> votes,
                                                          int totalVotes,
                                                          int seatPool) {
        Map<PoliticalParty, Double>  quotas     = new LinkedHashMap<>();
        Map<PoliticalParty, Integer> seats      = new LinkedHashMap<>();
        Map<PoliticalParty, Double>  remainders = new LinkedHashMap<>();

        for (PoliticalParty p : electable) {
            double quota = (double) votes.getOrDefault(p, 0) / totalVotes * seatPool;
            seats.put(p, (int) quota);
            remainders.put(p, quota - (int) quota);
        }

        int assigned  = seats.values().stream().mapToInt(v -> v).sum();
        int remaining = seatPool - assigned;

        List<PoliticalParty> byRemainder = new ArrayList<>(electable);
        byRemainder.sort((a, b) -> Double.compare(
                remainders.getOrDefault(b, 0.0), remainders.getOrDefault(a, 0.0)));

        for (int i = 0; i < remaining && i < byRemainder.size(); i++) {
            seats.merge(byRemainder.get(i), 1, Integer::sum);
        }

        for (PoliticalParty p : electable) {
            if (votes.getOrDefault(p, 0) > 0 && seats.getOrDefault(p, 0) == 0) {
                seats.put(p, 1);
            }
        }
        return seats;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private boolean isFixedSeat(PoliticalParty party) {
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
        PoliticalParty best    = null;
        double         bestMul = 0;
        for (PoliticalParty p : all) {
            double m = p.getViewStrength(view).getMultiplier();
            if (m > bestMul) { bestMul = m; best = p; }
        }
        return best;
    }

    private PoliticalParty findPartyByName(String name, List<PoliticalParty> parties) {
        for (PoliticalParty p : parties) {
            if (p.getName().equals(name)) return p;
        }
        return null;
    }

    private PolitcalView getDominantView(PoliticalParty party) {
        PolitcalView best        = PolitcalView.NONE;
        double       bestStrength = 0;
        for (Map.Entry<PolitcalView, ViewStrength> e : party.getViews().entrySet()) {
            if (e.getValue().getMultiplier() > bestStrength) {
                bestStrength = e.getValue().getMultiplier();
                best = e.getKey();
            }
        }
        return best;
    }

    // ─── Save / load ─────────────────────────────────────────────────────────

    public int  getTurnsSinceLastElection()      { return turnsSinceLastElection; }
    public void setTurnsSinceLastElection(int v) { turnsSinceLastElection = v; }
}