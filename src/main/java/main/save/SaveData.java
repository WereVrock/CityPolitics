// SaveData.java
package main.save;

import java.util.List;
import java.util.Map;

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

    public List<PopEntry>          pops;
    public List<PartyEntry>        parties;
    public List<ActiveEffectEntry> activeEffects;
    public VoteSessionEntry        pendingVoteSession;

    // Noble system
    public List<NobleHouseEntry>   nobleHouses;
    public List<RelationshipEntry> relationships;
    public List<ClaimEntry>        claims;
    public List<NobleArmyEntry>    nobleArmies;

    // Player armies
    public List<PlayerArmyEntry>   playerArmies;

    // Barbarian system
    public BarbInvasionStateEntry  barbInvasionState;
    public List<BarbArmyEntry>     barbArmies;
    public List<RavagedZoneEntry>  ravagedZones;

    // Zone states
    public List<ZoneStateEntry>    zoneStates;

    // ─── Inner classes ───────────────────────────────────────────────────────

    public static class PopEntry {
        public String popType;
        public String affiliation;
        public int    count;
        public PopEntry() {}
        public PopEntry(String popType, String affiliation, int count) {
            this.popType = popType; this.affiliation = affiliation; this.count = count;
        }
    }

    public static class PartyEntry {
        public String name;
        public int    playerOpinion;
        public int    publicOpinion;
        public int    power;
        public int    favour;
        public PartyEntry() {}
        public PartyEntry(String name, int playerOpinion, int publicOpinion, int power, int favour) {
            this.name = name; this.playerOpinion = playerOpinion;
            this.publicOpinion = publicOpinion; this.power = power; this.favour = favour;
        }
    }

    public static class ActiveEffectEntry {
        public String type;
        public double remainingAmount;
        public int    turnsRemaining;
        public ActiveEffectEntry() {}
        public ActiveEffectEntry(String type, double remainingAmount, int turnsRemaining) {
            this.type = type; this.remainingAmount = remainingAmount; this.turnsRemaining = turnsRemaining;
        }
    }

    public static class VoteSessionEntry {
        public String              actionName;
        public String              playerIntent;
        public List<PartyVoteEntry> partyVotes;
        public VoteSessionEntry() {}

        public static class PartyVoteEntry {
            public String  partyName;
            public double  score;
            public String  intent;
            public boolean dealt;
            public PartyVoteEntry() {}
            public PartyVoteEntry(String partyName, double score, String intent, boolean dealt) {
                this.partyName = partyName; this.score = score;
                this.intent = intent; this.dealt = dealt;
            }
        }
    }

    // ─── Noble system ────────────────────────────────────────────────────────

    public static class NobleHouseEntry {
        public String              id;
        public int                 gold;
        public int                 food;
        public int                 nobleManpower;
        public int                 influence;
        public int                 playerOpinion;
        public int                 prestige;
        public List<String>        zoneIds;
        public int                 activeCharacterIndex;
        public Map<String, Integer> fortifications;
        public Map<String, Integer> garrisons;
        public Map<String, Integer> garrisonMaxBonus;
        public List<String>        threatenedBy;
        public NobleHouseEntry() {}
    }

    public static class RelationshipEntry {
        public String houseIdA;
        public String houseIdB;
        public String relationship;  // Relationship enum name
        public RelationshipEntry() {}
        public RelationshipEntry(String a, String b, String rel) {
            this.houseIdA = a; this.houseIdB = b; this.relationship = rel;
        }
    }

    public static class ClaimEntry {
        public String claimantId;
        public String zoneId;
        public ClaimEntry() {}
        public ClaimEntry(String claimantId, String zoneId) {
            this.claimantId = claimantId; this.zoneId = zoneId;
        }
    }

    public static class NobleArmyEntry {
        public String  id;
        public String  houseId;
        public int     size;
        public String  zoneId;
        public String  pendingOrder;         // OrderType enum name
        public String  pendingTargetZoneId;
        public boolean orderReadyToResolve;
        public boolean skipNextUpkeep;
        public boolean isCoalitionAttack;
        public List<String> coalitionMemberIds;
        public NobleArmyEntry() {}
    }

    // ─── Player armies ───────────────────────────────────────────────────────

    public static class PlayerArmyEntry {
        public String  id;
        public String  displayName;
        public String  zoneId;
        public int     size;
        public boolean dragging;
        // Commander fields (nullable — armies without commanders use defaults)
        public String  commanderName;
        public String  commanderRace;
        public String  commanderAffiliation;
        public int     commanderSkill;
        public PlayerArmyEntry() {}
    }

    // ─── Barbarian system ────────────────────────────────────────────────────

    public static class BarbInvasionStateEntry {
        public String  phase;              // Phase enum name
        public int     countdownTurns;
        public int     turnsSinceInvasionStart;
        public int     nextWaveTurn;
        public int     waveHalfPending;
        public BarbInvasionStateEntry() {}
    }

    public static class BarbArmyEntry {
        public String       id;
        public String       type;           // BarbArmy.Type enum name
        public int          size;
        public String       zoneId;
        public String       nextZoneId;
        public boolean      isGarrison;
        public boolean      paidOff;
        public boolean      dismissed;
        public List<String> visitedZones;
        public BarbArmyEntry() {}
    }

    public static class RavagedZoneEntry {
        public String zoneId;
        public String level;               // RavagedLevel enum name
        public int    turnsRavaged;
        public RavagedZoneEntry() {}
        public RavagedZoneEntry(String zoneId, String level, int turnsRavaged) {
            this.zoneId = zoneId; this.level = level; this.turnsRavaged = turnsRavaged;
        }
    }

    // ─── Zone states ─────────────────────────────────────────────────────────

    public static class ZoneStateEntry {
        public String  zoneId;
        public int     damage;
        public int     supplyLevel;
        public int     recentlyRaidedTurns;
        public int     conquestMalusPercent;
        public int     rebellionPower;
        public ZoneStateEntry() {}
    }
}