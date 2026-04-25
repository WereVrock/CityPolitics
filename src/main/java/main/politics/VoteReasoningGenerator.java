package main.politics;

import main.parameters.GameParameters;
import main.resources.ResourcePool;
import main.resources.StatBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates in-character prose explaining WHY a party votes as they do
 * and what they want from a deal. Separate from opening dialogue.
 */
public class VoteReasoningGenerator {

    private static final Random RNG = new Random();

    // ─── Vote reasoning ───────────────────────────────────────────────────────

    public static String explain(PoliticalParty party,
                                 VotingSession session,
                                 ResourcePool resources,
                                 StatBlock stats) {
        boolean isOracles = isOracleParty(party, session);
        if (isOracles) return oracleReasoning(party);

        double score = session.getScore(party);
        List<VoteCondition> conditions = session.getAction().getVoteConditions();

        List<String> forReasons     = new ArrayList<>();
        List<String> againstReasons = new ArrayList<>();

        for (VoteCondition condition : conditions) {
            if (!conditionMet(condition, resources, stats)) continue;

            double viewMult = condition.getView() != null
                ? party.getViewStrength(condition.getView()).getMultiplier()
                : 1.0;
            double contribution = condition.getWeight() * viewMult;

            if (contribution > 0.05) {
                forReasons.add(reasonPhrase(condition, party, true, resources, stats));
            } else if (contribution < -0.05) {
                againstReasons.add(reasonPhrase(condition, party, false, resources, stats));
            }
        }

        if (forReasons.isEmpty() && againstReasons.isEmpty()) {
            return neutralReasoning(party, score);
        }

        StringBuilder sb = new StringBuilder();
        if (!forReasons.isEmpty()) {
            sb.append(pickForIntro(party)).append(" ");
            sb.append(joinReasons(forReasons)).append(".");
        }
        if (!againstReasons.isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(pickAgainstIntro(party)).append(" ");
            sb.append(joinReasons(againstReasons)).append(".");
        }
        return sb.toString();
    }

    // ─── Deal wants ──────────────────────────────────────────────────────────

    public static String dealWants(PoliticalParty party, VotingSession session) {
        double score = session.getScore(party);
        String personality = party.getPersonality().toLowerCase();
        String leader = lastName(party);

        if (isMercantile(personality)) {
            return pick(List.of(
                "\"Name your price and " + leader + " will name mine. Everything has a value — let us find it.\"",
                "\"We are not unreasonable. The right arrangement and you have our seats. Cold coin, warm alliance.\"",
                "\"Gold speaks louder than principle in this chamber. " + leader + " is listening.\""
            ));
        }
        if (isMilitary(personality)) {
            return pick(List.of(
                "\"Show us the realm benefits from this and we will consider our position.\"",
                "\"" + leader + " does not deal cheaply. But we deal. Make your offer worthy of our name.\"",
                "\"Strength respects strength. If your offer reflects that, we can talk.\""
            ));
        }
        if (isScholarly(personality)) {
            return pick(List.of(
                "\"The Archivists do not often enter arrangements. When we do, we expect commitments to be honoured.\"",
                "\"Precedent suggests agreements made here carry weight. " + leader + " will hold you to yours.\"",
                "\"We are open to persuasion. Be precise. Vague promises carry no weight in the archives.\""
            ));
        }
        if (isIdealistic(personality)) {
            return pick(List.of(
                "\"We want to know the people benefit. Show us something real, not a politician's promise.\"",
                "\"" + leader + " does not sell votes for gold. But for the right commitment to the common good — perhaps.\"",
                "\"The base is watching. Whatever we agree must be something we can defend in the open.\""
            ));
        }
        // default
        return pick(List.of(
            "\"" + leader + " is open to arrangements. Make your case.\"",
            "\"Bring us something worth our seats and we will talk.\"",
            "\"We are not here to give votes away. What are you offering?\""
        ));
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────

    private static String reasonPhrase(VoteCondition condition,
                                        PoliticalParty party,
                                        boolean positive,
                                        ResourcePool resources,
                                        StatBlock stats) {
        double val = resolveValue(condition.getVariable(), resources, stats);
        String varName = switch (condition.getVariable()) {
            case MONEY      -> "the treasury";
            case FOOD       -> "food stores";
            case INFLUENCE  -> "our influence";
            case CORRUPTION -> "corruption in the realm";
            case HAPPINESS  -> "the people's mood";
            case MANPOWER   -> "military strength";
        };
        String direction = condition.getRelation() == VoteCondition.Relation.GREATER_THAN
            ? "high" : "low";

        if (condition.getView() != null) {
            String viewName = condition.getView().getDisplayName().toLowerCase();
            return varName + " being " + direction + " aligns with our " + viewName + " principles";
        }
        return varName + " stands at " + (int) val + ", which " + (positive ? "supports" : "undermines") + " this proposal";
    }

    private static String neutralReasoning(PoliticalParty party, double score) {
        if (Math.abs(score) < GameParameters.VOTE_INDECISIVE_THRESHOLD) {
            return pick(List.of(
                "The matter is not clear-cut. " + lastName(party) + " sees arguments on both sides.",
                "Current conditions offer no strong reason to decide either way. We are weighing it.",
                "Our position remains undetermined. The numbers pull in different directions."
            ));
        }
        return "";
    }

    private static String oracleReasoning(PoliticalParty party) {
        return pick(List.of(
            "The stars have spoken on this matter. We follow where they point, as we always have.",
            "The prophecy does not elaborate on every vote. But the pattern is clear enough to those who know how to read it.",
            "We have watched rulers come and go. This one has our favour — and our seats follow our faith."
        ));
    }

    private static String pickForIntro(PoliticalParty party) {
        return pick(List.of(
            lastName(party) + " sees reason to support this:",
            "We lean toward yes because",
            "The case for this is clear:",
            "Several factors push us toward approval —"
        ));
    }

    private static String pickAgainstIntro(PoliticalParty party) {
        return pick(List.of(
            "However, there are concerns:",
            "That said, " + lastName(party) + " has reservations:",
            "Against this: ",
            "We are held back by the following:"
        ));
    }

    private static String joinReasons(List<String> reasons) {
        if (reasons.size() == 1) return reasons.get(0);
        if (reasons.size() == 2) return reasons.get(0) + ", and " + reasons.get(1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < reasons.size(); i++) {
            if (i > 0) sb.append(i == reasons.size() - 1 ? ", and " : ", ");
            sb.append(reasons.get(i));
        }
        return sb.toString();
    }

    private static boolean conditionMet(VoteCondition condition,
                                         ResourcePool resources,
                                         StatBlock stats) {
        double value = resolveValue(condition.getVariable(), resources, stats);
        return switch (condition.getRelation()) {
            case GREATER_THAN -> value > condition.getThreshold();
            case LESS_THAN    -> value < condition.getThreshold();
        };
    }

    private static double resolveValue(VoteCondition.Variable variable,
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

    private static boolean isOracleParty(PoliticalParty party, VotingSession session) {
        return party.getSeats() == 4 && party.getLeaderName().contains("Thessivane");
    }

    private static String lastName(PoliticalParty party) {
        String[] parts = party.getLeaderName().split(" ");
        return parts[parts.length - 1];
    }

    private static boolean isMilitary(String p) {
        return p.contains("military") || p.contains("battle") || p.contains("strength")
            || p.contains("conquest") || p.contains("clipped");
    }

    private static boolean isMercantile(String p) {
        return p.contains("coin") || p.contains("trade") || p.contains("merchant")
            || p.contains("transactional") || p.contains("weighs");
    }

    private static boolean isScholarly(String p) {
        return p.contains("precise") || p.contains("histor") || p.contains("scribe")
            || p.contains("archivist");
    }

    private static boolean isIdealistic(String p) {
        return p.contains("idealist") || p.contains("earnest") || p.contains("people")
            || p.contains("justice") || p.contains("common");
    }

    private static String pick(List<String> lines) {
        return lines.get(RNG.nextInt(lines.size()));
    }
}