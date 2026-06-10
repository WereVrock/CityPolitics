// SaveManager.java
package main.save;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import main.actions.FormalAction;
import main.actions.PlayerAction;
import main.army.Army;
import main.army.ArmyManager;
import main.barbarians.*;
import main.calendar.GameCalendar;
import main.core.GameState;
import main.effects.ActiveEffect;
import main.map.ZoneState;
import main.nobles.*;
import main.pops.Pop;
import main.pops.PopType;
import main.politics.PolitcalView;
import main.politics.PoliticalParty;
import main.politics.VotingSession;
import main.resources.ResourcePool;
import main.resources.StatBlock;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Converts GameState to/from SaveData and handles file I/O.
 * Stateless — all methods are static.
 */
public class SaveManager {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static final String APP_NAME  = "FrostVeil";
    private static final String SAVE_FILE = "save.fv";

    private SaveManager() {}

    // ─── Save folder resolution ───────────────────────────────────────────────

    public static Path getSaveDirectory() {
        String os = System.getProperty("os.name").toLowerCase();
        Path base;
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            base = (appData != null)
                    ? Paths.get(appData)
                    : Paths.get(System.getProperty("user.home"), "AppData", "Roaming");
        } else if (os.contains("mac")) {
            base = Paths.get(System.getProperty("user.home"), "Library", "Application Support");
        } else {
            String xdg = System.getenv("XDG_DATA_HOME");
            base = (xdg != null && !xdg.isEmpty())
                    ? Paths.get(xdg)
                    : Paths.get(System.getProperty("user.home"), ".local", "share");
        }
        return base.resolve(APP_NAME).resolve("saves");
    }

    public static File getSaveFile() {
        return getSaveDirectory().resolve(SAVE_FILE).toFile();
    }

    private static void ensureSaveDirectoryExists() throws IOException {
        Files.createDirectories(getSaveDirectory());
    }

    public static boolean saveExists() {
        return getSaveFile().exists();
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    public static void save(GameState gameState) throws IOException {
        ensureSaveDirectoryExists();
        MAPPER.writeValue(getSaveFile(), toSaveData(gameState));
    }

    public static void load(GameState gameState) throws IOException {
        File file = getSaveFile();
        if (!file.exists()) throw new IOException("No save file found at: " + file.getAbsolutePath());
        applyToGameState(MAPPER.readValue(file, SaveData.class), gameState);
    }

    // =========================================================================
    // GameState → SaveData
    // =========================================================================

    private static SaveData toSaveData(GameState gs) {
        SaveData data = new SaveData();

        // Calendar
        GameCalendar cal        = gs.getCalendar();
        data.year               = cal.getYear();
        data.period             = cal.getPeriod().name();
        data.totalTurnsElapsed  = cal.getTotalTurnsElapsed();

        // Resources
        ResourcePool res = gs.getResources();
        data.food       = res.getFood();
        data.money      = res.getMoney();
        data.manpower   = res.getManpower();
        data.influence  = res.getInfluence();

        // Stats
        data.corruption = gs.getStats().getCorruption();
        data.happiness  = gs.getStats().getHappiness();

        // Pops
        data.pops = serializePops(gs);

        // Parties
        data.parties = serializeParties(gs);

        // Active effects
        data.activeEffects = serializeEffects(gs);

        // Vote session
        if (gs.hasActiveSession()) data.pendingVoteSession = serializeSession(gs.getActiveSession());

        // Noble system
        data.nobleHouses    = serializeNobleHouses(gs);
        data.relationships  = serializeRelationships(gs);
        data.claims         = serializeClaims(gs);
        data.nobleArmies    = serializeNobleArmies(gs);

        // Player armies
        data.playerArmies      = serializePlayerArmies(gs);
        data.commanderRoster   = serializeCommanderRoster(gs);

        // Barbarian system
        data.barbInvasionState = serializeBarbState(gs);
        data.barbArmies        = serializeBarbArmies(gs);
        data.ravagedZones      = serializeRavagedZones(gs);

        // Zone states
        data.zoneStates = serializeZoneStates(gs);

        return data;
    }

    // ─── Serialize helpers ───────────────────────────────────────────────────

    private static List<SaveData.PopEntry> serializePops(GameState gs) {
        List<SaveData.PopEntry> list = new ArrayList<>();
        for (Pop pop : gs.getPopManager().getPops()) {
            list.add(new SaveData.PopEntry(pop.getType().name(), pop.getAffiliation().name(), pop.getCount()));
        }
        return list;
    }

    private static List<SaveData.PartyEntry> serializeParties(GameState gs) {
        List<SaveData.PartyEntry> list = new ArrayList<>();
        for (PoliticalParty p : gs.getPartyManager().getParties()) {
            list.add(new SaveData.PartyEntry(p.getName(), p.getPlayerOpinion(),
                    p.getPublicOpinion(), p.getPower(), p.getFavour()));
        }
        return list;
    }

    private static List<SaveData.ActiveEffectEntry> serializeEffects(GameState gs) {
        List<SaveData.ActiveEffectEntry> list = new ArrayList<>();
        for (ActiveEffect e : gs.getEffectManager().getActiveEffects()) {
            list.add(new SaveData.ActiveEffectEntry(e.getType().name(),
                    e.getRemainingAmount(), e.getTurnsRemaining()));
        }
        return list;
    }

    private static List<SaveData.NobleHouseEntry> serializeNobleHouses(GameState gs) {
        List<SaveData.NobleHouseEntry> list = new ArrayList<>();
        for (NobleHouse h : gs.getNobleHouseManager().getHouses()) {
            SaveData.NobleHouseEntry e = new SaveData.NobleHouseEntry();
            e.id                  = h.getId();
            e.gold                = h.getGold();
            e.food                = h.getFood();
            e.nobleManpower       = h.getNobleManpower();
            e.influence           = h.getInfluence();
            e.playerOpinion       = h.getPlayerOpinion();
            e.prestige            = h.getPrestige();
            e.zoneIds             = new ArrayList<>(h.getZoneIds());
            e.activeCharacterIndex = 0; // always 0 in current design
            e.fortifications      = serializePerZoneInt(h, "fortifications");
            e.garrisons           = serializePerZoneInt(h, "garrisons");
            e.garrisonMaxBonus    = serializePerZoneInt(h, "garrisonMaxBonus");
            e.threatenedBy        = new ArrayList<>(h.getThreatenedBy());
            list.add(e);
        }
        return list;
    }

    /** Reads a private Map<String,Integer> field from NobleHouse via reflection. */
    @SuppressWarnings("unchecked")
    private static Map<String, Integer> serializePerZoneInt(NobleHouse h, String fieldName) {
        try {
            Field f = NobleHouse.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            Map<String, Integer> map = (Map<String, Integer>) f.get(h);
            return new LinkedHashMap<>(map);
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
    }

    private static List<SaveData.RelationshipEntry> serializeRelationships(GameState gs) {
        List<SaveData.RelationshipEntry> list = new ArrayList<>();
        RelationshipManager rm = gs.getNobleHouseManager().getRelationships();
        List<NobleHouse> houses = new ArrayList<>(gs.getNobleHouseManager().getHouses());
        for (int i = 0; i < houses.size(); i++) {
            for (int j = i + 1; j < houses.size(); j++) {
                String a = houses.get(i).getId();
                String b = houses.get(j).getId();
                Relationship rel = rm.get(a, b);
                if (rel != Relationship.NEUTRAL) {
                    list.add(new SaveData.RelationshipEntry(a, b, rel.name()));
                }
            }
        }
        return list;
    }

    private static List<SaveData.ClaimEntry> serializeClaims(GameState gs) {
        List<SaveData.ClaimEntry> list = new ArrayList<>();
        ClaimManager cm = gs.getNobleHouseManager().getClaimManager();
        for (NobleHouse h : gs.getNobleHouseManager().getHouses()) {
            for (Claim c : cm.getClaimsFor(h.getId())) {
                list.add(new SaveData.ClaimEntry(c.getClaimantId(), c.getZoneId()));
            }
        }
        return list;
    }

    private static List<SaveData.NobleArmyEntry> serializeNobleArmies(GameState gs) {
        List<SaveData.NobleArmyEntry> list = new ArrayList<>();
        NobleArmyManager am = gs.getNobleHouseManager().getArmyManager();
        for (NobleArmy a : am.getAllArmies()) {
            SaveData.NobleArmyEntry e = new SaveData.NobleArmyEntry();
            e.id                  = a.getId();
            e.houseId             = a.getHouseId();
            e.size                = a.getSize();
            e.zoneId              = a.getZoneId();
            e.pendingOrder        = a.getPendingOrder().name();
            e.pendingTargetZoneId = a.getPendingTargetZoneId();
            e.orderReadyToResolve = a.isOrderReadyToResolve();
            e.skipNextUpkeep      = a.getSkipNextUpkeep();
            e.isCoalitionAttack   = a.isCoalitionAttack();
            e.coalitionMemberIds  = a.isCoalitionAttack()
                    ? new ArrayList<>(a.getCoalitionMemberIds()) : null;
            list.add(e);
        }
        return list;
    }

private static List<SaveData.PlayerArmyEntry> serializePlayerArmies(GameState gs) {
        List<SaveData.PlayerArmyEntry> list = new ArrayList<>();
        for (Army a : gs.getArmyManager().getArmies()) {
            SaveData.PlayerArmyEntry e = new SaveData.PlayerArmyEntry();
            e.id          = a.getId();
            e.displayName = a.getDisplayName();
            e.zoneId      = a.getZoneId();
            e.size        = a.getSize();
            e.dragging    = a.isDragging();
            main.army.commander.Commander cmd = a.getCommander();
            if (cmd != null) {
                e.commanderName      = cmd.getName();
                e.commanderRace      = cmd.getRace();
                e.commanderPartyName = cmd.getPartyName();
                e.commanderSkill     = cmd.getCommandingSkill();
                e.commanderXp        = cmd.getXp();
            }
            list.add(e);
        }
        return list;
    }

private static List<SaveData.CommanderRosterEntry> serializeCommanderRoster(GameState gs) {
    List<SaveData.CommanderRosterEntry> list = new ArrayList<>();
    main.army.commander.CommanderRoster roster = gs.getCommanderRoster();
    // Collect commanders NOT assigned to any army to avoid double-saving
    java.util.Set<main.army.commander.Commander> assignedCommanders = new java.util.HashSet<>();
    for (Army a : gs.getArmyManager().getArmies()) {
        if (a.getCommander() != null) assignedCommanders.add(a.getCommander());
    }
    for (main.army.commander.Commander c : roster.getAllCommanders()) {
        if (assignedCommanders.contains(c)) continue; // already saved with army
        SaveData.CommanderRosterEntry e = new SaveData.CommanderRosterEntry();
        e.name      = c.getName();
        e.race      = c.getRace();
        e.partyName = c.getPartyName();
        e.skill     = c.getCommandingSkill();
        e.xp        = c.getXp();
        e.alive     = c.isAlive();
        list.add(e);
    }
    return list;
}

private static SaveData.BarbInvasionStateEntry serializeBarbState(GameState gs) {
        BarbInvasionState s = gs.getBarbInvasionState();
        SaveData.BarbInvasionStateEntry e = new SaveData.BarbInvasionStateEntry();
        e.phase                   = s.getPhase().name();
        e.countdownTurns          = s.getCountdownTurns();
        e.turnsSinceInvasionStart = s.getTurnsSinceInvasionStart();
        e.nextWaveTurn            = s.getNextWaveTurn();
        e.waveHalfPending         = readBarbWaveHalfPending(s);
        return e;
    }

    private static int readBarbWaveHalfPending(BarbInvasionState s) {
        try {
            Field f = BarbInvasionState.class.getDeclaredField("waveHalfPending");
            f.setAccessible(true);
            return f.getInt(s);
        } catch (Exception ex) { return 0; }
    }

    private static List<SaveData.BarbArmyEntry> serializeBarbArmies(GameState gs) {
        List<SaveData.BarbArmyEntry> list = new ArrayList<>();
        for (BarbArmy a : gs.getBarbArmyManager().getAllArmies()) {
            if (!a.isAlive() && !a.isGarrison()) continue;
            SaveData.BarbArmyEntry e = new SaveData.BarbArmyEntry();
            e.id           = a.getId();
            e.type         = a.getType().name();
            e.size         = a.getSize();
            e.zoneId       = a.getZoneId();
            e.nextZoneId   = a.getNextZoneId();
            e.isGarrison   = a.isGarrison();
            e.paidOff      = a.isPaidOff();
            e.dismissed    = a.isDismissed();
            e.visitedZones = new ArrayList<>(a.getVisitedZones());
            list.add(e);
        }
        return list;
    }

    private static List<SaveData.RavagedZoneEntry> serializeRavagedZones(GameState gs) {
        List<SaveData.RavagedZoneEntry> list = new ArrayList<>();
        RavagedZoneManager rzm = gs.getRavagedZoneManager();
        for (String zoneId : rzm.getRavagedZoneIds()) {
            RavagedZoneManager.RavagedLevel level = rzm.getLevel(zoneId);
            int turnsRavaged = readRavagedTurns(rzm, zoneId);
            list.add(new SaveData.RavagedZoneEntry(zoneId, level.name(), turnsRavaged));
        }
        return list;
    }

    private static int readRavagedTurns(RavagedZoneManager rzm, String zoneId) {
        try {
            Field entriesField = RavagedZoneManager.class.getDeclaredField("entries");
            entriesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Object> entries = (Map<String, Object>) entriesField.get(rzm);
            Object entry = entries.get(zoneId);
            if (entry == null) return 0;
            Field turnsField = entry.getClass().getDeclaredField("turnsRavaged");
            turnsField.setAccessible(true);
            return turnsField.getInt(entry);
        } catch (Exception ex) { return 0; }
    }

    private static List<SaveData.ZoneStateEntry> serializeZoneStates(GameState gs) {
        List<SaveData.ZoneStateEntry> list = new ArrayList<>();
        for (main.map.Zone z : gs.getZoneManager().getZones()) {
            ZoneState state = gs.getZoneManager().getState(z.getId());
            if (state == null) continue;
            // Only save non-default states to keep file small
            if (state.getDamage() == 0 && state.getSupplyLevel() == 100
                    && state.getRaidedTurns() == 0 && state.getConquestMalus() == 0
                    && state.getRebellionPower() == 0) continue;
            SaveData.ZoneStateEntry e = new SaveData.ZoneStateEntry();
            e.zoneId              = z.getId();
            e.damage              = state.getDamage();
            e.supplyLevel         = state.getSupplyLevel();
            e.recentlyRaidedTurns = state.getRaidedTurns();
            e.conquestMalusPercent = state.getConquestMalus();
            e.rebellionPower      = state.getRebellionPower();
            list.add(e);
        }
        return list;
    }

    private static SaveData.VoteSessionEntry serializeSession(VotingSession session) {
        SaveData.VoteSessionEntry entry = new SaveData.VoteSessionEntry();
        entry.actionName   = session.getAction().getName();
        entry.playerIntent = session.getPlayerIntent().name();
        List<SaveData.VoteSessionEntry.PartyVoteEntry> partyVotes = new ArrayList<>();
        for (PoliticalParty party : session.getParties()) {
            partyVotes.add(new SaveData.VoteSessionEntry.PartyVoteEntry(
                    party.getName(), session.getScore(party),
                    session.getIntent(party).name(), session.hasDealt(party)));
        }
        entry.partyVotes = partyVotes;
        return entry;
    }

    // =========================================================================
    // SaveData → GameState
    // =========================================================================

    private static void applyToGameState(SaveData data, GameState gs) {
        applyCalendar(data, gs.getCalendar());
        applyResources(data, gs.getResources());
        applyStats(data, gs.getStats());
        applyPops(data, gs);
        applyParties(data, gs);
        applyActiveEffects(data, gs);
        applyVoteSession(data, gs);
        applyNobleHouses(data, gs);
        applyRelationships(data, gs);
        applyClaims(data, gs);
        applyNobleArmies(data, gs);
        applyPlayerArmies(data, gs);
        applyCommanderRoster(data, gs);
        applyBarbState(data, gs);
        applyBarbArmies(data, gs);
        applyRavagedZones(data, gs);
        applyZoneStates(data, gs);
    }

    // ─── Apply helpers ────────────────────────────────────────────────────────

    private static void applyCalendar(SaveData data, GameCalendar cal) {
        cal.setYear(data.year);
        cal.setPeriod(GameCalendar.Period.valueOf(data.period));
        cal.setTotalTurnsElapsed(data.totalTurnsElapsed);
    }

    private static void applyResources(SaveData data, ResourcePool res) {
        res.setFood(data.food);
        res.setMoney(data.money);
        res.setManpower(data.manpower);
        res.setInfluence(data.influence);
    }

    private static void applyStats(SaveData data, StatBlock stats) {
        stats.setCorruption(data.corruption);
        stats.setHappiness(data.happiness);
    }

    private static void applyPops(SaveData data, GameState gs) {
        if (data.pops == null) return;
        for (SaveData.PopEntry entry : data.pops) {
            Pop pop = gs.getPopManager().getPopByType(PopType.valueOf(entry.popType));
            if (pop != null) {
                pop.setCount(entry.count);
                pop.setAffiliation(PolitcalView.valueOf(entry.affiliation));
            }
        }
    }

    private static void applyParties(SaveData data, GameState gs) {
        if (data.parties == null) return;
        for (SaveData.PartyEntry entry : data.parties) {
            for (PoliticalParty party : gs.getPartyManager().getParties()) {
                if (party.getName().equals(entry.name)) {
                    party.setPlayerOpinion(entry.playerOpinion);
                    party.setPublicOpinion(entry.publicOpinion);
                    party.setPower(entry.power);
                    party.setFavour(entry.favour);
                    break;
                }
            }
        }
    }

    private static void applyActiveEffects(SaveData data, GameState gs) {
        gs.getEffectManager().reset();
        if (data.activeEffects == null) return;
        for (SaveData.ActiveEffectEntry entry : data.activeEffects) {
            gs.getEffectManager().addEffect(new ActiveEffect(
                    ActiveEffect.Type.valueOf(entry.type),
                    entry.remainingAmount, entry.turnsRemaining, true));
        }
    }

    private static void applyVoteSession(SaveData data, GameState gs) {
        gs.clearActiveSession();
        if (data.pendingVoteSession == null) return;
        FormalAction action = findActionByName(data.pendingVoteSession.actionName, gs);
        if (action == null) return;
        VotingSession session = gs.getVoteSessionManager()
                .restoreSession(action, gs.getPartyManager().getParties(), data.pendingVoteSession);
        gs.addSession(session);
    }

    private static void applyNobleHouses(SaveData data, GameState gs) {
        if (data.nobleHouses == null) return;
        for (SaveData.NobleHouseEntry entry : data.nobleHouses) {
            NobleHouse h = gs.getNobleHouseManager().getHouseById(entry.id);
            if (h == null) continue;

            h.addGold(entry.gold - h.getGold());
            h.addFood(entry.food - h.getFood());
            h.addNobleManpower(entry.nobleManpower - h.getNobleManpower());
            h.addInfluence(entry.influence - h.getInfluence());
            h.setPlayerOpinion(entry.playerOpinion);
            h.addPrestige(entry.prestige - h.getPrestige());

            // Sync zones: remove zones not in saved list, add zones that are
            List<String> currentZones = new ArrayList<>(h.getZoneIds());
            for (String z : currentZones) {
                if (!entry.zoneIds.contains(z)) h.removeZone(z);
            }
            for (String z : entry.zoneIds) {
                if (!h.getZoneIds().contains(z)) h.addZone(z);
            }

            // Restore per-zone maps via reflection
            writePerZoneInt(h, "fortifications",   entry.fortifications);
            writePerZoneInt(h, "garrisons",        entry.garrisons);
            writePerZoneInt(h, "garrisonMaxBonus", entry.garrisonMaxBonus);

            // Restore threats
            h.clearThreats();
            if (entry.threatenedBy != null) {
                for (String threatId : entry.threatenedBy) h.addThreat(threatId);
            }

            h.recalculateCapital();
        }
    }

    @SuppressWarnings("unchecked")
    private static void writePerZoneInt(NobleHouse h, String fieldName, Map<String, Integer> data) {
        if (data == null) return;
        try {
            Field f = NobleHouse.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            Map<String, Integer> map = (Map<String, Integer>) f.get(h);
            map.clear();
            map.putAll(data);
        } catch (Exception ex) {
            // silently skip — field may not exist in this build
        }
    }

    private static void applyRelationships(SaveData data, GameState gs) {
        if (data.relationships == null) return;
        RelationshipManager rm = gs.getNobleHouseManager().getRelationships();
        rm.reset();
        for (SaveData.RelationshipEntry entry : data.relationships) {
            rm.set(entry.houseIdA, entry.houseIdB, Relationship.valueOf(entry.relationship));
        }
    }

    private static void applyClaims(SaveData data, GameState gs) {
        if (data.claims == null) return;
        ClaimManager cm = gs.getNobleHouseManager().getClaimManager();
        cm.reset();
        for (SaveData.ClaimEntry entry : data.claims) {
            cm.addClaim(entry.claimantId, entry.zoneId);
        }
    }

    private static void applyNobleArmies(SaveData data, GameState gs) {
        if (data.nobleArmies == null) return;
        NobleArmyManager am = gs.getNobleHouseManager().getArmyManager();
        am.reset();
        for (SaveData.NobleArmyEntry entry : data.nobleArmies) {
            NobleArmy army = new NobleArmy(entry.id, entry.houseId, entry.size, entry.zoneId);
            army.setSkipNextUpkeep(entry.skipNextUpkeep);

            NobleArmy.OrderType order = NobleArmy.OrderType.valueOf(entry.pendingOrder);
            if (order != NobleArmy.OrderType.NONE) {
                if (entry.isCoalitionAttack && entry.coalitionMemberIds != null) {
                    army.issueCoalitionOrder(entry.pendingTargetZoneId,
                            new HashSet<>(entry.coalitionMemberIds));
                } else {
                    army.issueOrder(order, entry.pendingTargetZoneId);
                }
                if (entry.orderReadyToResolve) army.tickOrder();
            }
            am.addRestoredArmy(army);
        }
    }

private static void applyPlayerArmies(SaveData data, GameState gs) {
        if (data.playerArmies == null) return;
        ArmyManager am = gs.getArmyManager();
        am.reset();
        for (SaveData.PlayerArmyEntry entry : data.playerArmies) {
            for (Army army : am.getArmies()) {
                if (army.getDisplayName().equals(entry.displayName)) {
                    // Directly set both fields to avoid sync issues
                    army.setSize(entry.size);
                    army.setSoldierCount(entry.size);
                    // Restore commander BEFORE moveTo so the deployment guard passes
                    if (entry.commanderName != null) {
                        main.politics.PoliticalParty party = null;
                        if (entry.commanderPartyName != null) {
                            for (main.politics.PoliticalParty p
                                    : gs.getPartyManager().getParties()) {
                                if (p.getName().equals(entry.commanderPartyName)) {
                                    party = p;
                                    break;
                                }
                            }
                        }
                        main.army.commander.Commander cmd = new main.army.commander.Commander(
                                entry.commanderName,
                                entry.commanderRace,
                                party,
                                entry.commanderSkill);
                        if (entry.commanderXp > 0) cmd.addXp(entry.commanderXp);
                        army.setCommander(cmd);
                    }
                    // moveTo after commander set — guard will allow if commander present
                    if (Army.HEARTLAND_ID.equals(entry.zoneId)) {
                        army.recallToCity();
                    } else {
                        // Bypass the guard for save restoration by setting field directly
                        // via the internal-zone setter (no commander required on load)
                        army.restoreZone(entry.zoneId);
                    }
                    break;
                }
            }
        }
    }

private static void applyCommanderRoster(SaveData data, GameState gs) {
    main.army.commander.CommanderRoster roster = gs.getCommanderRoster();
    // Clear existing unassigned commanders (assigned ones are restored via applyPlayerArmies)
    // We rebuild the full roster: assigned commanders come from armies, rest from saved list.
    // Reset roster then re-add army commanders + saved roster entries.
    roster.reset();

    // Re-add commanders that were restored onto armies
    for (Army a : gs.getArmyManager().getArmies()) {
        main.army.commander.Commander cmd = a.getCommander();
        if (cmd != null) roster.addRestoredCommander(cmd);
    }

    if (data.commanderRoster == null) return;
    for (SaveData.CommanderRosterEntry entry : data.commanderRoster) {
        main.politics.PoliticalParty party = null;
        if (entry.partyName != null && !entry.partyName.equals("None")) {
            for (main.politics.PoliticalParty p : gs.getPartyManager().getParties()) {
                if (p.getName().equals(entry.partyName)) { party = p; break; }
            }
        }
        main.army.commander.Commander cmd = new main.army.commander.Commander(
                entry.name, entry.race, party, entry.skill);
        if (entry.xp > 0) cmd.addXp(entry.xp);
        if (!entry.alive) cmd.kill();
        roster.addRestoredCommander(cmd);
    }
}

private static void applyBarbState(SaveData data, GameState gs) {
        if (data.barbInvasionState == null) return;
        BarbInvasionState s = gs.getBarbInvasionState();
        SaveData.BarbInvasionStateEntry e = data.barbInvasionState;
        writeBarbStateFields(s, e);
    }

    private static void writeBarbStateFields(BarbInvasionState s, SaveData.BarbInvasionStateEntry e) {
        try {
            setField(s, BarbInvasionState.class, "phase",
                    BarbInvasionState.Phase.valueOf(e.phase));
            setField(s, BarbInvasionState.class, "countdownTurns",        e.countdownTurns);
            setField(s, BarbInvasionState.class, "turnsSinceInvasionStart", e.turnsSinceInvasionStart);
            setField(s, BarbInvasionState.class, "nextWaveTurn",          e.nextWaveTurn);
            setField(s, BarbInvasionState.class, "waveHalfPending",       e.waveHalfPending);
        } catch (Exception ex) {
            // log and continue — invasion state may be slightly off but not fatal
        }
    }

    private static void applyBarbArmies(SaveData data, GameState gs) {
        if (data.barbArmies == null) return;
        BarbArmyManager am = gs.getBarbArmyManager();
        am.reset();
        for (SaveData.BarbArmyEntry entry : data.barbArmies) {
            BarbArmy army = new BarbArmy(
                    BarbArmy.Type.valueOf(entry.type), entry.size, entry.zoneId);
            restoreBarbArmyFields(army, entry);
            if (entry.isGarrison) {
                am.addGarrison(army);
            } else {
                am.addRestoredArmy(army);
            }
        }
    }

    private static void restoreBarbArmyFields(BarbArmy army, SaveData.BarbArmyEntry entry) {
        try {
            // id is final — set via reflection
            setField(army, BarbArmy.class, "id", entry.id);
            army.setNextZoneId(entry.nextZoneId);
            if (entry.paidOff)   army.setPaidOff(true);
            if (entry.dismissed) army.dismiss();
            if (entry.visitedZones != null) {
                Field vf = BarbArmy.class.getDeclaredField("visitedZones");
                vf.setAccessible(true);
                @SuppressWarnings("unchecked")
                Set<String> vs = (Set<String>) vf.get(army);
                vs.clear();
                vs.addAll(entry.visitedZones);
            }
        } catch (Exception ex) {
            // best-effort
        }
    }

    private static void applyRavagedZones(SaveData data, GameState gs) {
        if (data.ravagedZones == null) return;
        RavagedZoneManager rzm = gs.getRavagedZoneManager();
        rzm.reset();
        for (SaveData.RavagedZoneEntry entry : data.ravagedZones) {
            RavagedZoneManager.RavagedLevel level =
                    RavagedZoneManager.RavagedLevel.valueOf(entry.level);
            if (level == RavagedZoneManager.RavagedLevel.HEAVILY_RAVAGED) {
                rzm.markHeavilyRavaged(entry.zoneId);
            } else {
                rzm.markRavaged(entry.zoneId);
            }
            // Restore turnsRavaged
            writeRavagedTurns(rzm, entry.zoneId, entry.turnsRavaged);
        }
    }

    private static void writeRavagedTurns(RavagedZoneManager rzm, String zoneId, int turns) {
        try {
            Field entriesField = RavagedZoneManager.class.getDeclaredField("entries");
            entriesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Object> entries = (Map<String, Object>) entriesField.get(rzm);
            Object entry = entries.get(zoneId);
            if (entry == null) return;
            Field turnsField = entry.getClass().getDeclaredField("turnsRavaged");
            turnsField.setAccessible(true);
            turnsField.setInt(entry, turns);
        } catch (Exception ex) { /* best-effort */ }
    }

    private static void applyZoneStates(SaveData data, GameState gs) {
        if (data.zoneStates == null) return;
        for (SaveData.ZoneStateEntry entry : data.zoneStates) {
            ZoneState state = gs.getZoneManager().getState(entry.zoneId);
            if (state == null) continue;
            state.setDamage(entry.damage);
            state.setSupplyLevel(entry.supplyLevel);
            writeZoneStateFields(state, entry);
        }
    }

private static void writeZoneStateFields(ZoneState state, SaveData.ZoneStateEntry entry) {
        state.setRecentlyRaidedTurns(entry.recentlyRaidedTurns);
        state.setConquestMalusPercent(entry.conquestMalusPercent);
        state.setRebellionPower(entry.rebellionPower);
    }

// ─── Shared utilities ─────────────────────────────────────────────────────

    private static void setField(Object obj, Class<?> clazz, String name, Object value)
            throws Exception {
        Field f = clazz.getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, value);
    }

    private static FormalAction findActionByName(String name, GameState gs) {
        for (PlayerAction action : gs.getActionRegistry().getActions()) {
            if (action.getName().equals(name) && action instanceof FormalAction fa) return fa;
        }
        return null;
    }
}