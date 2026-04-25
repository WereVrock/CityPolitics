package main.politics;

import main.parameters.GameParameters;
import main.resources.ResourcePool;
import main.resources.StatBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates the full negotiation monologue for a party leader.
 * Three sections, always in order:
 *   1. Greeting     — attitude toward player based on opinion, power, situation
 *   2. Vote stance  — why they vote yes/no/undecided, grounded in conditions
 *   3. Deal section — whether a deal is possible and exactly what they want
 */
public class NegotiationDialogueGenerator {

    private static final Random RNG = new Random();

    public static String generate(PoliticalParty party,
                                  VotingSession session,
                                  PoliticalParty oracles,
                                  ResourcePool resources,
                                  StatBlock stats) {
        boolean isOracles    = party == oracles;
        boolean alreadyDealt = session.hasDealt(party);
        double  score        = session.getScore(party);
        boolean canDeal      = session.canDeal(party);
        VotingSession.PartyVoteIntent intent = session.getIntent(party);

        String greeting  = buildGreeting(party, isOracles, alreadyDealt, resources, stats);
        String voteBlock = buildVoteBlock(party, session, isOracles, score, intent, resources, stats);
        String dealBlock = buildDealBlock(party, session, isOracles, alreadyDealt, canDeal, score, resources, stats);

        StringBuilder sb = new StringBuilder();
        sb.append(greeting);
        if (voteBlock != null && !voteBlock.isEmpty()) {
            sb.append("\n\n").append(voteBlock);
        }
        if (dealBlock != null && !dealBlock.isEmpty()) {
            sb.append("\n\n").append(dealBlock);
        }
        return sb.toString();
    }

    // =========================================================================
    // SECTION 1 — GREETING
    // =========================================================================

    private static String buildGreeting(PoliticalParty party, boolean isOracles,
                                        boolean alreadyDealt,
                                        ResourcePool resources, StatBlock stats) {
        if (alreadyDealt) return alreadyDealtGreeting(party);
        if (isOracles)    return oracleGreeting(party);

        int    opinion     = party.getPlayerOpinion();
        int    power       = party.getPower();
        String personality = party.getPersonality().toLowerCase();
        String leader      = lastName(party);

        if (opinion >= 70) return highOpinionGreeting(leader, personality, power);
        if (opinion <= 30) return lowOpinionGreeting(leader, personality, resources, stats);
        return neutralGreeting(leader, personality, resources, stats);
    }

    private static String alreadyDealtGreeting(PoliticalParty party) {
        String leader = lastName(party);
        return pick(List.of(
            "We have already struck our arrangement. " + leader + " does not revisit settled matters.",
            "Our agreement stands. Do not push further.",
            "You have our word and our seats. That is the end of it."
        ));
    }

    private static String oracleGreeting(PoliticalParty party) {
        int    opinion = party.getPlayerOpinion();
        String leader  = lastName(party);
        if (opinion >= 70) {
            return pick(List.of(
                "Ah... you again. Good. The stars have been quite insistent about you. We follow, as we have always followed.",
                "I dreamed of this moment three winters past. Or was it yesterday? No matter. You have our devotion, child.",
                "The Arch Oracle does not often leave his tower. But for you — yes. For you, he makes the effort."
            ));
        }
        if (opinion >= 40) {
            return pick(List.of(
                "The prophecy spoke of a great leader. We believed it was you. We still believe it. Mostly.",
                "You have given us reason to wonder, of late. But the stars do not lie. We remain cautiously devoted.",
                "My knees ache. My memory wanders. But I remember the prophecy. You are the one. Probably."
            ));
        }
        return pick(List.of(
            "The stars showed us a saviour. Perhaps they meant someone else. We shall reconsider.",
            "I am old. I may have misread the signs. You have disappointed " + leader + ", child. That is no small thing.",
            "We gave you our trust freely. Do not make an old man's faith look foolish."
        ));
    }

    private static String highOpinionGreeting(String leader, String personality, int power) {
        if (isMilitary(personality)) {
            return pick(List.of(
                "Good. " + leader + " was hoping you would come in person. We have few disagreements lately.",
                "You have conducted yourself well. " + leader + " respects that. Sit. Let us be brief.",
                "We have watched your recent decisions. Not displeased. What do you need?"
            ));
        }
        if (isMercantile(personality)) {
            return pick(List.of(
                "Always a pleasure when you visit under good terms, " + leader + " must say. Rare in this chamber.",
                "We have been allies more often than not lately. That has value. " + leader + " remembers it.",
                "You have treated us fairly. " + leader + " does not forget that. Speak freely."
            ));
        }
        if (isIdealistic(personality)) {
            return pick(List.of(
                "It is good to see you. The people speak well of your recent choices. That matters to " + leader + ".",
                "We have been pleased by your conduct. Genuinely so — not as a bargaining position.",
                "You have shown you care about more than your own position. That earns goodwill here."
            ));
        }
        return pick(List.of(
            leader + " has no complaints with you lately. That is more than most can say.",
            "You have earned some credit with us. Come in. Let us talk.",
            "We are well-disposed toward you at present. Use that wisely."
        ));
    }

    private static String lowOpinionGreeting(String leader, String personality,
                                             ResourcePool resources, StatBlock stats) {
        if (isMilitary(personality)) {
            return pick(List.of(
                leader + " will be direct: we have little patience for your proposals right now.",
                "You come to us with a request after your recent conduct. " + leader + " is not impressed.",
                "We respect strength. Lately you have shown us little of it. This conversation starts from a poor position."
            ));
        }
        if (isMercantile(personality)) {
            return pick(List.of(
                "Our ledger of goodwill toward you is, frankly, in the red. " + leader + " notes this.",
                "You have not been kind to our interests lately. That has a cost. A literal one.",
                "We do business with everyone — but with you, right now, the margin needs to be worth it."
            ));
        }
        return pick(List.of(
            leader + " will not pretend relations between us are warm. They are not.",
            "You have made things difficult. " + leader + " remembers that. This will not be easy.",
            "We are here. That is more than you deserve from us at the moment."
        ));
    }

    private static String neutralGreeting(String leader, String personality,
                                          ResourcePool resources, StatBlock stats) {
        if (isMercantile(personality)) {
            return pick(List.of(
                leader + " has no particular feelings toward you one way or another. That is not a bad place to start.",
                "We have no open grievances. No special warmth either. Consider this a clean table.",
                "Neutral ground, " + leader + " would call it. Make of that what you will."
            ));
        }
        if (isScholarly(personality)) {
            return pick(List.of(
                "The Archivists have reviewed your record. It is neither distinguished nor damning. " + leader + " has questions.",
                "You have our attention. Whether you have our support depends on what follows.",
                leader + " prefers to let the facts speak before forming opinions. We are listening."
            ));
        }
        return pick(List.of(
            leader + " will hear you out. Nothing more is promised yet.",
            "We have no strong feelings about you at present. That could go either way.",
            "You find us undecided — about you as much as about the vote. Speak."
        ));
    }

    // =========================================================================
    // SECTION 2 — VOTE STANCE
    // =========================================================================

    private static String buildVoteBlock(PoliticalParty party, VotingSession session,
                                         boolean isOracles, double score,
                                         VotingSession.PartyVoteIntent intent,
                                         ResourcePool resources, StatBlock stats) {
        if (isOracles)             return oracleVoteBlock();
        if (session.hasDealt(party)) return null;

        List<VoteCondition> conditions  = session.getAction().getVoteConditions();
        List<String>        forReasons  = new ArrayList<>();
        List<String>        againstReasons = new ArrayList<>();

        for (VoteCondition condition : conditions) {
            if (!conditionMet(condition, resources, stats)) continue;
            double viewMult = condition.getView() != null
                ? party.getViewStrength(condition.getView()).getMultiplier()
                : 1.0;
            double contribution = condition.getWeight() * viewMult;
            if (contribution > 0.05) {
                forReasons.add(conditionToPhrase(condition, party, true, resources, stats));
            } else if (contribution < -0.05) {
                againstReasons.add(conditionToPhrase(condition, party, false, resources, stats));
            }
        }

        if (forReasons.isEmpty() && againstReasons.isEmpty()) {
            return noConditionsBlock(score);
        }

        StringBuilder sb = new StringBuilder();
        if (!forReasons.isEmpty()) {
            sb.append(voteForIntro(intent));
            sb.append(" ");
            sb.append(joinReasons(forReasons));
            sb.append(".");
        }
        if (!againstReasons.isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(voteAgainstIntro(forReasons.isEmpty(), intent));
            sb.append(" ");
            sb.append(joinReasons(againstReasons));
            sb.append(".");
        }
        return sb.toString();
    }

    private static String oracleVoteBlock() {
        return pick(List.of(
            "The stars have been clear on this matter for some time. We follow where they point.",
            "The prophecy does not annotate every vote — but the pattern is legible to those trained to read it.",
            "We have watched many rulers and many votes. The signs favour cooperation with you. For now."
        ));
    }

    private static String noConditionsBlock(double score) {
        if (Math.abs(score) < GameParameters.VOTE_INDECISIVE_THRESHOLD) {
            return pick(List.of(
                "Current conditions give us no strong signal either way. We are genuinely undecided.",
                "The numbers pull in different directions. Nothing is settled.",
                "This is not a simple call. We are weighing it."
            ));
        }
        return "";
    }

    private static String conditionToPhrase(VoteCondition condition, PoliticalParty party,
                                            boolean positive,
                                            ResourcePool resources, StatBlock stats) {
        double val       = resolveValue(condition.getVariable(), resources, stats);
        int    ival      = (int) val;
        String what      = describeVariable(condition.getVariable(), ival);
        String situation = actualSituation(condition.getVariable(), condition.getRelation(), ival);

        if (condition.getView() != null) {
            String view   = condition.getView().getDisplayName();
            String stance = positive
                ? "is exactly the kind of condition our " + view + " convictions demand action on"
                : "is precisely what our " + view + " principles warn against";
            return what + " — " + situation + " — " + stance;
        }
        return what + " " + situation + ", which " + (positive ? "supports this course" : "makes this hard to justify");
    }

    private static String describeVariable(VoteCondition.Variable v, int val) {
        return switch (v) {
            case MONEY      -> "the treasury (" + val + " gold)";
            case FOOD       -> "food stores (" + val + ")";
            case INFLUENCE  -> "political capital (" + val + ")";
            case CORRUPTION -> "corruption (" + val + ")";
            case HAPPINESS  -> "the mood of the people (" + val + ")";
            case MANPOWER   -> "military strength (" + val + ")";
        };
    }

    private static String actualSituation(VoteCondition.Variable variable,
                                          VoteCondition.Relation relation,
                                          int val) {
        boolean isHigh = relation == VoteCondition.Relation.GREATER_THAN;
        return switch (variable) {
            case MONEY      -> isHigh ? "running healthy"       : "running thin";
            case FOOD       -> isHigh ? "well-stocked"          : "stretched thin";
            case INFLUENCE  -> isHigh ? "carrying real weight"  : "wearing thin";
            case CORRUPTION -> isHigh ? "having risen this far" : "still manageable";
            case HAPPINESS  -> isHigh ? "holding well"          : "already strained";
            case MANPOWER   -> isHigh ? "standing strong"       : "dangerously low";
        };
    }

    private static String voteForIntro(VotingSession.PartyVoteIntent intent) {
        if (intent == VotingSession.PartyVoteIntent.YES) {
            return pick(List.of(
                "We are inclined to support this, and here is why:",
                "Our position is yes — because",
                "The case for approval, as we see it:"
            ));
        }
        return pick(List.of(
            "There are things pulling us toward yes:",
            "In its favour:",
            "What speaks for this:"
        ));
    }

    private static String voteAgainstIntro(boolean noForReasons,
                                           VotingSession.PartyVoteIntent intent) {
        if (noForReasons && intent == VotingSession.PartyVoteIntent.NO) {
            return pick(List.of(
                "We oppose this. The reason is straightforward:",
                "Our vote is no —",
                "We are against this, and here is why:"
            ));
        }
        return pick(List.of(
            "That said, we have real reservations:",
            "Against it:",
            "What holds us back:"
        ));
    }

    // =========================================================================
    // SECTION 3 — DEAL
    // =========================================================================

    private static String buildDealBlock(PoliticalParty party, VotingSession session,
                                         boolean isOracles, boolean alreadyDealt,
                                         boolean canDeal, double score,
                                         ResourcePool resources, StatBlock stats) {
        if (isOracles || alreadyDealt) return null;
        if (!canDeal) return noDealBlock(party, score);
        return dealAvailableBlock(party, score, resources, stats);
    }

    private static String noDealBlock(PoliticalParty party, double score) {
        String leader = lastName(party);
        if (score > GameParameters.VOTE_DEAL_LOCK_THRESHOLD) {
            return pick(List.of(
                "There is nothing to negotiate here. " + leader + " supports this outright. No deal needed.",
                "We are already voting yes. Keep your coin.",
                "Save the bargaining for someone who needs persuading."
            ));
        }
        return pick(List.of(
            "Our position on this is fixed. " + leader + " will not be moved by any arrangement.",
            "This is not a matter of price. We are opposed and that will not change.",
            "Do not waste resources trying to buy what we will not sell."
        ));
    }

    private static String dealAvailableBlock(PoliticalParty party, double score,
                                             ResourcePool resources, StatBlock stats) {
        String  personality = party.getPersonality().toLowerCase();
        String  leader      = lastName(party);
        boolean reluctant   = score < -0.8;

        String preamble = reluctant
            ? pick(List.of(
                "We will not pretend this is comfortable. " + leader + " can be moved, but it will cost you.",
                "Changing our position on this is a significant ask. We expect significant compensation.",
                "You are asking us to vote against our inclination. That has a price."
              ))
            : pick(List.of(
                leader + " is open to an arrangement.",
                "Our position is not locked. There is room to talk.",
                "We can be persuaded. Here is what it takes."
              ));

        String ask = buildConcreteAsk(party, personality, resources, stats, reluctant);
        return preamble + " " + ask;
    }

    private static String buildConcreteAsk(PoliticalParty party, String personality,
                                           ResourcePool resources, StatBlock stats,
                                           boolean reluctant) {
        String leader = lastName(party);

        if (isMercantile(personality)) {
            if (resources.getMoney() < 100) {
                return pick(List.of(
                    "Gold is thin across the board right now, which means ours is thinner too. We need coin — real coin. That is the price.",
                    "The guild's reserves are stretched. " + leader + " needs a direct injection of gold to make this work.",
                    "Treasury is low and so are we. You want our seats, you cover our shortfall. Simple arithmetic."
                ));
            }
            return pick(List.of(
                "We want influence — the kind that opens doors, not the ceremonial sort. Spend some of your political capital on us.",
                leader + " wants gold and a favour. A real commitment, logged, to support our next trade proposal.",
                "The guild expects a return. Redirect some influence our way or fund our next venture. Either works."
            ));
        }

        if (isMilitary(personality)) {
            if (resources.getManpower() < 70) {
                return pick(List.of(
                    "The garrison is thin. " + leader + " wants a commitment to rebuild it — manpower funded, not promised.",
                    "Our troops are understrength. Fix that and you have our vote. It is not a complicated ask.",
                    "Military strength in this realm is dangerously low. " + leader + " will back you when you back the army. That means now."
                ));
            }
            if (resources.getInfluence() < 40) {
                return pick(List.of(
                    leader + " wants political backing for our next appointment. A name we choose, placed where we choose.",
                    "We want your influence spent on our behalf — on the matter of the eastern command. Back us there.",
                    "Influence is the currency here. Spend yours on our priorities and we spend our seats on yours."
                ));
            }
            return pick(List.of(
                "We want gold for equipment procurement. " + leader + " will not ask twice.",
                "Fund the next military appropriation and the vote is yours.",
                leader + " wants a public commitment to expanding the standing force."
            ));
        }

        if (isScholarly(personality)) {
            if (resources.getInfluence() < 50) {
                return pick(List.of(
                    "We want your remaining influence directed toward our petition for archive access. That is the condition.",
                    leader + " needs political backing for the vault expansion. Influence spent on that earns you our seats.",
                    "Our request for expanded archival authority has been stalled. Back it with your influence and we back you."
                ));
            }
            return pick(List.of(
                "We want a formal seat on the next advisory council. Not honorary — functional. " + leader + " will hold you to that.",
                "The Archivists require influence and a written commitment that our counsel is sought before the next major decree.",
                leader + " wants gold for preservation work and your word — recorded — that this agreement will be honoured."
            ));
        }

        if (isIdealistic(personality)) {
            if (stats.getHappiness() < 50) {
                return pick(List.of(
                    "The people are suffering. " + leader + " will not back anything without a direct commitment — gold for relief, distributed now.",
                    "Happiness is low and our voters feel it. Give us something real to show them. Resources directed to the people.",
                    "Our condition is a happiness measure funded from the treasury. The people need to see action, not hear about it."
                ));
            }
            if (stats.getCorruption() > 40) {
                return pick(List.of(
                    "Corruption is undermining everything. " + leader + " wants influence and gold committed to fighting it before we agree to anything else.",
                    "We cannot keep supporting a government that lets corruption fester. Back an anti-corruption measure with real resources.",
                    "The people notice corruption even when the assembly pretends not to. " + leader + " wants it addressed — now."
                ));
            }
            return pick(List.of(
                "All we ask is that the next distribution measure goes through our ward representatives. Keep it fair, keep it public.",
                leader + " wants a happiness measure funded and enacted. Gold committed, not merely promised.",
                "Spend some of your political capital on something that visibly helps ordinary people and we will spend our seats on you."
            ));
        }

        // Default — isolationist, traditionalist, or mixed
        if (resources.getMoney() < 80) {
            return pick(List.of(
                leader + " wants gold. The realm is short and so are we. That is the honest answer.",
                "Our needs are simple right now: coin. The treasury is low, our patience is lower.",
                "Gold talks when everything else is tight. " + leader + " is listening for it."
            ));
        }
        return pick(List.of(
            "We want influence and a favour — not called in today, but acknowledged and owed.",
            leader + " wants your ear on matters that concern us. A real commitment, not a courtesy nod.",
            "Gold and goodwill. Both. " + leader + " is not interested in half-measures."
        ));
    }

    // =========================================================================
    // SHARED UTILITIES
    // =========================================================================

    private static boolean conditionMet(VoteCondition condition,
                                        ResourcePool resources, StatBlock stats) {
        double value = resolveValue(condition.getVariable(), resources, stats);
        return switch (condition.getRelation()) {
            case GREATER_THAN -> value > condition.getThreshold();
            case LESS_THAN    -> value < condition.getThreshold();
        };
    }

    private static double resolveValue(VoteCondition.Variable variable,
                                       ResourcePool resources, StatBlock stats) {
        return switch (variable) {
            case MONEY      -> resources.getMoney();
            case FOOD       -> resources.getFood();
            case INFLUENCE  -> resources.getInfluence();
            case MANPOWER   -> resources.getManpower();
            case CORRUPTION -> stats.getCorruption();
            case HAPPINESS  -> stats.getHappiness();
        };
    }

    private static String joinReasons(List<String> reasons) {
        if (reasons.size() == 1) return reasons.get(0);
        if (reasons.size() == 2) return reasons.get(0) + ", and " + reasons.get(1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < reasons.size(); i++) {
            if (i > 0) sb.append(i == reasons.size() - 1 ? ", and " : "; ");
            sb.append(reasons.get(i));
        }
        return sb.toString();
    }

    private static String lastName(PoliticalParty party) {
        String[] parts = party.getLeaderName().split(" ");
        return parts[parts.length - 1];
    }

    private static boolean isMilitary(String p) {
        return p.contains("military") || p.contains("battle") || p.contains("strength")
            || p.contains("conquest") || p.contains("clipped") || p.contains("soldier");
    }

    private static boolean isMercantile(String p) {
        return p.contains("coin") || p.contains("trade") || p.contains("merchant")
            || p.contains("transactional") || p.contains("weighs");
    }

    private static boolean isScholarly(String p) {
        return p.contains("precise") || p.contains("histor") || p.contains("scribe")
            || p.contains("archivist") || p.contains("record");
    }

    private static boolean isIdealistic(String p) {
        return p.contains("idealist") || p.contains("earnest") || p.contains("people")
            || p.contains("justice") || p.contains("common");
    }

    private static String pick(List<String> lines) {
        return lines.get(RNG.nextInt(lines.size()));
    }
}