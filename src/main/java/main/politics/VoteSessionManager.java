package main.politics;

import main.actions.FormalAction;
import main.actions.PlayerAction;
import main.resources.ResourcePool;
import main.resources.StatBlock;
import main.save.SaveData;
import debug.Debug;

import java.util.*;
import main.parameters.GameParameters;

/**
 * Creates voting sessions and resolves them into VoteResults.
 */
public class VoteSessionManager {

    private final VotingEngine engine = new VotingEngine();

    public VotingSession restoreSession(FormalAction action,
                                        List<PoliticalParty> parties,
                                        main.save.SaveData.VoteSessionEntry entry) {
        Map<PoliticalParty, Double> scores  = new LinkedHashMap<>();
        Map<PoliticalParty, VotingSession.PartyVoteIntent> intents = new LinkedHashMap<>();
        Map<PoliticalParty, Boolean> dealt  = new LinkedHashMap<>();

        for (SaveData.VoteSessionEntry.PartyVoteEntry pve : entry.partyVotes) {
            for (PoliticalParty party : parties) {
                if (party.getName().equals(pve.partyName)) {
                    scores.put(party, pve.score);
                    intents.put(party, VotingSession.PartyVoteIntent.valueOf(pve.intent));
                    dealt.put(party, pve.dealt);
                    break;
                }
            }
        }

        VotingSession.PartyVoteIntent playerIntent =
            VotingSession.PartyVoteIntent.valueOf(entry.playerIntent);

        Debug.log("voting", "session-restored", action.getName());
        return new VotingSession(action, parties, scores, intents, dealt, playerIntent);
    }

    public VotingSession createSession(FormalAction action,
                                       List<PoliticalParty> parties,
                                       ResourcePool resources,
                                       StatBlock stats) {
        Map<PoliticalParty, Double> scores = new LinkedHashMap<>();
        for (PoliticalParty p : parties) {
            double score = engine.scoreForParty(p, action.getVoteConditions(), resources, stats);
            scores.put(p, score);
            Debug.log("voting", "score", p.getName() + " score=" + String.format("%.2f", score));
        }
        Debug.log("voting", "session-created", action.getName()
                + " parties=" + parties.size());
        return new VotingSession(action, parties, scores);
    }

    /**
     * Finalizes the vote. Returns both the VoteResult and a list of log lines
     * describing the outcome in detail.
     */
    public VoteResult finalize(VotingSession session, ResourcePool resources, StatBlock stats) {
        List<VoteScore> voteScores = new ArrayList<>();
        int totalYes = 0, totalNo = 0, totalAbstain = 0;

        // Player row: 1 seat
        switch (session.getPlayerIntent()) {
            case YES     -> totalYes++;
            case NO      -> totalNo++;
            default      -> totalAbstain++;
        }
        Debug.log("voting", "finalize-player", "intent=" + session.getPlayerIntent());

        for (PoliticalParty party : session.getParties()) {
            VotingSession.PartyVoteIntent intent = session.getIntent(party);
            int seats      = party.getSeats();
            int sideSeats  = session.getSideDealtSeats(party);
            int normalSeats = seats - sideSeats;
            int yes = 0, no = 0, abs = 0;

            switch (intent) {
                case YES     -> yes = normalSeats;
                case NO      -> no  = normalSeats;
                case ABSTAIN -> abs = normalSeats;
                case UNKNOWN -> {
                    Random rng = new Random();
                    for (int i = 0; i < normalSeats; i++) {
                        int r = rng.nextInt(3);
                        if      (r == 0) yes++;
                        else if (r == 1) no++;
                        else             abs++;
                    }
                }
            }
            yes += sideSeats; // side-dealt seats always vote YES

            totalYes     += yes;
            totalNo      += no;
            totalAbstain += abs;
            voteScores.add(new VoteScore(party, session.getScore(party), yes, no, abs));

            Debug.log("voting", "finalize-party", party.getName()
                    + " intent=" + intent
                    + " sideSeats=" + sideSeats
                    + " yes=" + yes + " no=" + no + " abs=" + abs);
        }

        VoteResult result = new VoteResult(voteScores, totalYes, totalNo, totalAbstain,
                                           GameParameters.SEATS_NEEDED);
        Debug.log("voting", "finalize-result",
                session.getAction().getName()
                + " YES=" + totalYes + " NO=" + totalNo
                + " ABSTAIN=" + totalAbstain
                + " needed=" + GameParameters.SEATS_NEEDED
                + " passed=" + result.isPassed());
        return result;
    }

    /**
     * Builds detailed log lines for the vote result.
     * Called by MainWindow after finalize() to append to the event log.
     */
    public List<String> buildResultLog(VotingSession session, VoteResult result) {
        List<String> log = new ArrayList<>();
        String action = session.getAction().getName();

        log.add("══════════════════════════════════════");
        log.add("ASSEMBLY VOTE: " + action.toUpperCase());
        log.add("──────────────────────────────────────");

        // Player row
        String playerVote = session.getPlayerIntent().name();
        log.add("  YOU (Supervisor) → " + playerVote);

        // Each party
        for (VoteScore vs : result.getPartyScores()) {
            PoliticalParty party = vs.getParty();
            int sideSeats = session.getSideDealtSeats(party);
            StringBuilder row = new StringBuilder();
            row.append("  ").append(party.getName())
               .append(" (").append(party.getSeats()).append(" seats)");
            if (session.hasDealt(party) && sideSeats == 0) {
                row.append(" [DEAL]");
            } else if (sideSeats > 0) {
                row.append(" [SIDE DEAL: ").append(sideSeats).append(" seats]");
            }
            row.append("  →  YES:").append(vs.getYesSeats())
               .append(" NO:").append(vs.getNoSeats())
               .append(" ABS:").append(vs.getAbstainSeats());
            log.add(row.toString());
        }

        log.add("──────────────────────────────────────");
        log.add("  TOTAL  YES:" + result.getTotalYes()
                + "  NO:" + result.getTotalNo()
                + "  ABSTAIN:" + result.getTotalAbstain()
                + "  (needed:" + result.getSeatsNeeded() + ")");
        String verdict = result.isPassed()
                ? "✓ PASSED — " + action + " takes effect."
                : "✗ REJECTED — " + action + " fails.";
        log.add(verdict);
        log.add("══════════════════════════════════════");
        return log;
    }
}