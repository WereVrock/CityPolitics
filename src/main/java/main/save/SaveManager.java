package main.save;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import main.actions.FormalAction;
import main.actions.PlayerAction;
import main.calendar.GameCalendar;
import main.core.GameState;
import main.effects.ActiveEffect;
import main.pops.Pop;
import main.pops.PopType;
import main.politics.PolitcalView;
import main.politics.PoliticalParty;
import main.politics.VotingSession;
import main.resources.ResourcePool;
import main.resources.StatBlock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts GameState to/from SaveData and handles file I/O.
 * Saves are stored in the OS-appropriate user data directory.
 * Stateless — all methods are static.
 */
public class SaveManager {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static final String APP_NAME  = "FrostVeil";
    private static final String SAVE_FILE = "save.fv";

    private SaveManager() {}

    // ─── Save Folder Resolution ───────────────────────────────────────────────

    public static Path getSaveDirectory() {
        String os = System.getProperty("os.name").toLowerCase();
        Path base;

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            base = (appData != null)
                ? Paths.get(appData)
                : Paths.get(System.getProperty("user.home"), "AppData", "Roaming");
        } else if (os.contains("mac")) {
            base = Paths.get(System.getProperty("user.home"),
                    "Library", "Application Support");
        } else {
            // Linux / other Unix
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

    // ─── Save ────────────────────────────────────────────────────────────────

    public static void save(GameState gameState) throws IOException {
        ensureSaveDirectoryExists();
        SaveData data = toSaveData(gameState);
        MAPPER.writeValue(getSaveFile(), data);
    }

    // ─── Load ────────────────────────────────────────────────────────────────

    public static void load(GameState gameState) throws IOException {
        File file = getSaveFile();
        if (!file.exists()) {
            throw new IOException("No save file found at: " + file.getAbsolutePath());
        }
        SaveData data = MAPPER.readValue(file, SaveData.class);
        applyToGameState(data, gameState);
    }

    public static boolean saveExists() {
        return getSaveFile().exists();
    }

    // ─── GameState → SaveData ─────────────────────────────────────────────────

    private static SaveData toSaveData(GameState gameState) {
        SaveData data = new SaveData();

        GameCalendar cal       = gameState.getCalendar();
        data.year              = cal.getYear();
        data.period            = cal.getPeriod().name();
        data.totalTurnsElapsed = cal.getTotalTurnsElapsed();

        ResourcePool res = gameState.getResources();
        data.food      = res.getFood();
        data.money     = res.getMoney();
        data.manpower  = res.getManpower();
        data.influence = res.getInfluence();

        StatBlock stats = gameState.getStats();
        data.corruption = stats.getCorruption();
        data.happiness  = stats.getHappiness();

        List<SaveData.PopEntry> popEntries = new ArrayList<>();
        for (Pop pop : gameState.getPopManager().getPops()) {
            popEntries.add(new SaveData.PopEntry(
                pop.getType().name(),
                pop.getAffiliation().name(),
                pop.getCount()
            ));
        }
        data.pops = popEntries;

        List<SaveData.PartyEntry> partyEntries = new ArrayList<>();
        for (PoliticalParty party : gameState.getPartyManager().getParties()) {
            partyEntries.add(new SaveData.PartyEntry(
                party.getName(),
                party.getPlayerOpinion(),
                party.getPublicOpinion(),
                party.getPower(),
                party.getFavour()
            ));
        }
        data.parties = partyEntries;

        List<SaveData.ActiveEffectEntry> effectEntries = new ArrayList<>();
        for (ActiveEffect effect : gameState.getEffectManager().getActiveEffects()) {
            effectEntries.add(new SaveData.ActiveEffectEntry(
                effect.getType().name(),
                effect.getRemainingAmount(),
                effect.getTurnsRemaining()
            ));
        }
        data.activeEffects = effectEntries;

        if (gameState.hasActiveSession()) {
            data.pendingVoteSession = serializeSession(gameState.getActiveSession());
        }

        return data;
    }

    private static SaveData.VoteSessionEntry serializeSession(VotingSession session) {
        SaveData.VoteSessionEntry entry = new SaveData.VoteSessionEntry();
        entry.actionName   = session.getAction().getName();
        entry.playerIntent = session.getPlayerIntent().name();

        List<SaveData.VoteSessionEntry.PartyVoteEntry> partyVotes = new ArrayList<>();
        for (PoliticalParty party : session.getParties()) {
            partyVotes.add(new SaveData.VoteSessionEntry.PartyVoteEntry(
                party.getName(),
                session.getScore(party),
                session.getIntent(party).name(),
                session.hasDealt(party)
            ));
        }
        entry.partyVotes = partyVotes;
        return entry;
    }

    // ─── SaveData → GameState ─────────────────────────────────────────────────

    private static void applyToGameState(SaveData data, GameState gameState) {
        applyCalendar(data, gameState.getCalendar());
        applyResources(data, gameState.getResources());
        applyStats(data, gameState.getStats());
        applyPops(data, gameState);
        applyParties(data, gameState);
        applyActiveEffects(data, gameState);
        applyVoteSession(data, gameState);
    }

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

    private static void applyPops(SaveData data, GameState gameState) {
        for (SaveData.PopEntry entry : data.pops) {
            PopType      type        = PopType.valueOf(entry.popType);
            PolitcalView affiliation = PolitcalView.valueOf(entry.affiliation);
            Pop pop = gameState.getPopManager().getPopByType(type);
            if (pop != null) {
                pop.setCount(entry.count);
                pop.setAffiliation(affiliation);
            }
        }
    }

    private static void applyParties(SaveData data, GameState gameState) {
        if (data.parties == null) return;
        for (SaveData.PartyEntry entry : data.parties) {
            for (PoliticalParty party : gameState.getPartyManager().getParties()) {
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

    private static void applyActiveEffects(SaveData data, GameState gameState) {
        gameState.getEffectManager().reset();
        if (data.activeEffects == null) return;
        for (SaveData.ActiveEffectEntry entry : data.activeEffects) {
            ActiveEffect.Type type = ActiveEffect.Type.valueOf(entry.type);
            gameState.getEffectManager().addEffect(
                new ActiveEffect(type, entry.remainingAmount, entry.turnsRemaining, true)
            );
        }
    }

    private static void applyVoteSession(SaveData data, GameState gameState) {
        gameState.clearActiveSession();
        if (data.pendingVoteSession == null) return;

        SaveData.VoteSessionEntry entry = data.pendingVoteSession;
        FormalAction action = findActionByName(entry.actionName, gameState);
        if (action == null) return;

        List<PoliticalParty> parties = gameState.getPartyManager().getParties();
        VotingSession session = gameState.getVoteSessionManager()
            .restoreSession(action, parties, entry);
        gameState.addSession(session);
    }

    private static FormalAction findActionByName(String name, GameState gameState) {
        for (PlayerAction action : gameState.getActionRegistry().getActions()) {
            if (action.getName().equals(name) && action instanceof FormalAction fa) {
                return fa;
            }
        }
        return null;
    }
}