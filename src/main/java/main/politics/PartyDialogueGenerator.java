package main.politics;

import java.util.List;
import java.util.Random;
import main.parameters.GameParameters;

/**
 * Generates in-character opening lines for party leaders during negotiations.
 * Lines vary by leader personality, player opinion, party power, and vote stance.
 */
public class PartyDialogueGenerator {

    private static final Random RNG = new Random();

    public static String generate(PoliticalParty party,
                                  VotingSession session,
                                  PoliticalParty oracles) {
        boolean isOracles   = party == oracles;
        double  score       = session.getScore(party);
        boolean alreadyDealt= session.hasDealt(party);
        int     opinion     = party.getPlayerOpinion();
        int     power       = party.getPower();
        boolean canDeal     = session.canDeal(party);
        String  action      = session.getAction().getName();

        if (alreadyDealt) return alreadyDealtLine(party);
        if (isOracles)    return oracleLine(party, opinion);

        boolean stronglyFor     = score >  GameParameters.VOTE_DEAL_LOCK_THRESHOLD;
        boolean stronglyAgainst = score < -GameParameters.VOTE_DEAL_LOCK_THRESHOLD;

        if (stronglyFor)     return stronglyForLine(party, opinion, power, action);
        if (stronglyAgainst) return stronglyAgainstLine(party, opinion, power, action);
        return negotiableLine(party, opinion, power, action);
    }

    // ─── Oracle ──────────────────────────────────────────────────────────────

    private static String oracleLine(PoliticalParty party, int opinion) {
        String leader = party.getLeaderName();
        List<String> lines;
        if (opinion >= 70) {
            lines = List.of(
                "\"Ah... you again. Good. The stars have been... yes, quite insistent about you. We follow, as we have always followed.\"",
                "\"I dreamed of this moment three winters past. Or was it yesterday? No matter. You have our seats, child.\"",
                "\"The Arch Oracle does not often leave his tower. But for you... yes. For you, he makes the effort.\""
            );
        } else if (opinion >= 40) {
            lines = List.of(
                "\"The prophecy spoke of a great leader. We believed it was you. We... still believe it. Mostly.\"",
                "\"You have given us reason to wonder, of late. But the stars do not lie. We shall remain... cautiously devoted.\"",
                "\"My knees ache. My memory wanders. But I remember the prophecy. You are the one. Probably.\""
            );
        } else {
            lines = List.of(
                "\"The stars showed us a saviour. Perhaps they meant someone else. We shall... reconsider our position.\"",
                "\"I am old. I may have misread the signs. You have disappointed the Arch Oracle, child. That is no small thing.\"",
                "\"We gave you our trust freely. Do not make an old man's faith look foolish.\""
            );
        }
        return pick(lines);
    }

    // ─── Already dealt ────────────────────────────────────────────────────────

    private static String alreadyDealtLine(PoliticalParty party) {
        String first = firstName(party);
        List<String> lines = List.of(
            "\"We have an agreement. " + first + " does not revisit settled matters.\"",
            "\"Our arrangement stands. Do not push your luck further.\"",
            "\"You have our word and our seats. That is all you are getting.\""
        );
        return pick(lines);
    }

    // ─── Strongly for ─────────────────────────────────────────────────────────

    private static String stronglyForLine(PoliticalParty party, int opinion,
                                           int power, String action) {
        String first = firstName(party);
        String p     = party.getPersonality();

        if (isMilitary(p)) {
            return pick(List.of(
                "\"" + action + "? Of course. " + first + " has been pushing for this for years. You need not ask twice.\"",
                "\"Finally. Someone with the spine to do what needs doing. You have our full support.\"",
                "\"No negotiation needed. This is exactly the kind of decisive action this assembly requires.\""
            ));
        }
        if (isMercantile(p)) {
            return pick(List.of(
                "\"The numbers on this one are clear. " + first + " supports " + action + " without reservation.\"",
                "\"Good for business, good for the realm. We are already voting yes. Was there something else?\"",
                "\"You caught us on a good day. This proposal aligns with our interests perfectly.\""
            ));
        }
        if (isScholarly(p)) {
            return pick(List.of(
                "\"Historical precedent, current resource levels, projected outcomes — all point the same direction. We support this.\"",
                "\"The Archivists have reviewed the proposal. Our position is affirmative. There is little more to say.\"",
                "\"" + first + " rarely agrees with anything quickly. Consider this a rare occasion.\""
            ));
        }
        if (isIdealistic(p)) {
            return pick(List.of(
                "\"This is exactly what the people need. We stand behind " + action + " completely.\"",
                "\"For once, the assembly may do something right. We are with you on this.\"",
                "\"The people will remember who supported this. " + first + " intends to be on the right side.\""
            ));
        }
        // default
        return pick(List.of(
            "\"We support " + action + ". No conditions. Rare, I know.\"",
            "\"You have our votes. " + first + " sees no reason to complicate this.\"",
            "\"This one you get for free. Do not expect it every time.\""
        ));
    }

    // ─── Strongly against ─────────────────────────────────────────────────────

    private static String stronglyAgainstLine(PoliticalParty party, int opinion,
                                               int power, String action) {
        String first = firstName(party);
        String p     = party.getPersonality();

        if (opinion <= 30) {
            return pick(List.of(
                "\"You come to us with " + action + " after everything? The answer is no. It will always be no.\"",
                "\"" + first + " has little patience for your proposals at the best of times. This is not the best of times.\"",
                "\"No amount of gold or smooth talk will move us on this. Leave.\""
            ));
        }
        if (isMilitary(p)) {
            return pick(List.of(
                "\"" + action + " weakens us. " + first + " will not stand for it, regardless of who asks.\"",
                "\"We respect strength. This proposal shows none. Our vote is no.\"",
                "\"There are things " + first + " will not bend on. This is one of them.\""
            ));
        }
        if (isMercantile(p)) {
            return pick(List.of(
                "\"The cost to commerce is too great. No deal makes this worthwhile for us.\"",
                "\"" + first + " has run the numbers. They do not favour " + action + ". Not at any price.\"",
                "\"Some votes cannot be bought. This is one of them. Regrettable, but there it is.\""
            ));
        }
        return pick(List.of(
            "\"Our position on " + action + " is firm. " + first + " will not be moved.\"",
            "\"We are against this. Strongly. Do not waste your time or ours.\"",
            "\"" + first + " has heard enough. The answer is no.\""
        ));
    }

    // ─── Negotiable ───────────────────────────────────────────────────────────

    private static String negotiableLine(PoliticalParty party, int opinion,
                                          int power, String action) {
        String first = firstName(party);
        String p     = party.getPersonality();

        if (opinion >= 70) {
            return pick(List.of(
                "\"We have been sympathetic to your cause before, " + first + " admits. But sympathy has its price.\"",
                "\"You have earned some goodwill with us. Let us see if it is enough for " + action + ".\"",
                "\"" + first + " likes you. That counts for something. Not everything — but something.\""
            ));
        }
        if (opinion <= 30) {
            return pick(List.of(
                "\"You want our support? After your recent conduct? This will cost you dearly.\"",
                "\"" + first + " is not feeling generous toward you lately. Make it worth our while.\"",
                "\"We are unconvinced. Of you, of this proposal, of most things you bring to this chamber.\""
            ));
        }
        // neutral opinion
        if (isMercantile(p)) {
            return pick(List.of(
                "\"" + action + " is neither good nor terrible for us. Name your offer and we shall see.\"",
                "\"" + first + " is open to persuasion. We are always open to persuasion. What are you offering?\"",
                "\"Our seats are not cheap. But they are available. What did you have in mind?\""
            ));
        }
        if (isMilitary(p)) {
            return pick(List.of(
                "\"" + first + " has not decided on " + action + ". Show us it is worth backing.\"",
                "\"We are on the fence. Convince us this serves the realm's strength.\"",
                "\"Our support is not guaranteed. What makes " + action + " worth our seats?\""
            ));
        }
        if (isIdealistic(p)) {
            return pick(List.of(
                "\"The people are watching how we vote on this. " + first + " wants to be sure we are on the right side.\"",
                "\"We are undecided. Make the case that " + action + " truly helps those who need it.\"",
                "\"" + first + " does not sell votes lightly. But we are listening.\""
            ));
        }
        return pick(List.of(
            "\"" + first + " has not made up their mind on " + action + ". Persuade us.\"",
            "\"We are open to discussion. What are you prepared to offer?\"",
            "\"Our vote is not decided. You have a chance here — do not waste it.\""
        ));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static String firstName(PoliticalParty party) {
        String[] parts = party.getLeaderName().split(" ");
        return parts[parts.length - 1]; // last name feels more formal
    }

    private static String pick(List<String> lines) {
        return lines.get(RNG.nextInt(lines.size()));
    }

    private static boolean isMilitary(String personality) {
        String p = personality.toLowerCase();
        return p.contains("military") || p.contains("battle") || p.contains("soldier")
            || p.contains("strength") || p.contains("conquest") || p.contains("clipped");
    }

    private static boolean isMercantile(String personality) {
        String p = personality.toLowerCase();
        return p.contains("coin") || p.contains("trade") || p.contains("merchant")
            || p.contains("transactional") || p.contains("weighs");
    }

    private static boolean isScholarly(String personality) {
        String p = personality.toLowerCase();
        return p.contains("precise") || p.contains("histor") || p.contains("scribe")
            || p.contains("archivist") || p.contains("record");
    }

    private static boolean isIdealistic(String personality) {
        String p = personality.toLowerCase();
        return p.contains("idealist") || p.contains("earnest") || p.contains("people")
            || p.contains("justice") || p.contains("common");
    }
}