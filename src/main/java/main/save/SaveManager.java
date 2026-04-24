package main.save;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import main.calendar.GameCalendar;
import main.core.GameState;
import main.pops.Pop;
import main.pops.PopType;
import main.politics.PoliticalAffiliation;
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

        return data;
    }

    // ─── SaveData → GameState ─────────────────────────────────────────────────

    private static void applyToGameState(SaveData data, GameState gameState) {
        applyCalendar(data, gameState.getCalendar());
        applyResources(data, gameState.getResources());
        applyStats(data, gameState.getStats());
        applyPops(data, gameState);
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
            PopType              type        = PopType.valueOf(entry.popType);
            PoliticalAffiliation affiliation = PoliticalAffiliation.valueOf(entry.affiliation);
            Pop pop = gameState.getPopManager().getPopByType(type);
            if (pop != null) {
                pop.setCount(entry.count);
                pop.setAffiliation(affiliation);
            }
        }
    }
}