package main.save;

import main.calendar.GameCalendar;
import main.pops.PopType;
import main.politics.PolitcalView;

import java.util.List;

/**
 * Plain data transfer object for Jackson serialization.
 * No game logic — only primitives and simple value types.
 */
public class SaveData {

    public int year;
    public String period;
    public int totalTurnsElapsed;

    public int food;
    public int money;
    public int manpower;
    public int influence;

    public int corruption;
    public int happiness;

    public List<PopEntry> pops;
    public List<PartyEntry> parties;
    public List<ActiveEffectEntry> activeEffects;
    public VoteSessionEntry pendingVoteSession;

    public static class PopEntry {
        public String popType;
        public String affiliation;
        public int count;

        public PopEntry() {}

        public PopEntry(String popType, String affiliation, int count) {
            this.popType     = popType;
            this.affiliation = affiliation;
            this.count       = count;
        }
    }

    public static class PartyEntry {
        public String name;
        public int playerOpinion;
        public int publicOpinion;
        public int power;
        public int favour;

        public PartyEntry() {}

        public PartyEntry(String name, int playerOpinion, int publicOpinion, int power, int favour) {
            this.name          = name;
            this.playerOpinion = playerOpinion;
            this.publicOpinion = publicOpinion;
            this.power         = power;
            this.favour        = favour;
        }
    }

    public static class ActiveEffectEntry {
        public String type;
        public double remainingAmount;
        public int    turnsRemaining;

        public ActiveEffectEntry() {}

        public ActiveEffectEntry(String type, double remainingAmount, int turnsRemaining) {
            this.type            = type;
            this.remainingAmount = remainingAmount;
            this.turnsRemaining  = turnsRemaining;
        }
    }

    public static class VoteSessionEntry {
        public String actionName;
        public String playerIntent;
        public List<PartyVoteEntry> partyVotes;

        public VoteSessionEntry() {}

        public static class PartyVoteEntry {
            public String partyName;
            public double score;
            public String intent;
            public boolean dealt;

            public PartyVoteEntry() {}

            public PartyVoteEntry(String partyName, double score, String intent, boolean dealt) {
                this.partyName = partyName;
                this.score     = score;
                this.intent    = intent;
                this.dealt     = dealt;
            }
        }
    }
}