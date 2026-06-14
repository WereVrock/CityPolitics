package City.main.barbarians;

import City.main.parameters.BarbarianParams;
import City.main.parameters.CalendarParams;
 

import java.util.Random;

/**
 * Tracks the invasion countdown and wave scheduling.
 * An invasion cycle: countdown → active → destroyed → new countdown.
 */
public class BarbInvasionState {

    public enum Phase { COUNTDOWN, ACTIVE, DESTROYED }

    private Phase  phase          = Phase.COUNTDOWN;
    private int    countdownTurns;          // turns until invasion starts
    private int    turnsSinceInvasionStart; // used for warboss size scaling
    private int    nextWaveTurn;            // absolute turn number of next wave
    private int    waveHalfPending;         // 0 = none, 1 = second half pending next turn
    private int    wavesSpawned;            // incremented each time a full wave completes
    private int    invasionCount  = 0;      // total invasions that have started (0-indexed)

    private final Random rng = new Random();

    public BarbInvasionState() {
        resetCountdown();
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────

public void resetCountdown() {
        int years = CalendarParams.BARB_COUNTDOWN_MIN_YEARS
                  + rng.nextInt(CalendarParams.BARB_COUNTDOWN_MAX_YEARS
                              - CalendarParams.BARB_COUNTDOWN_MIN_YEARS + 1);
        countdownTurns         = years * CalendarParams.PERIODS_PER_YEAR;
        phase                  = Phase.COUNTDOWN;
        turnsSinceInvasionStart = 0;
        waveHalfPending         = 0;
        wavesSpawned            = 0;
    }

public void startInvasion(int absoluteTurn) {
        phase                   = Phase.ACTIVE;
        turnsSinceInvasionStart = 0;
        wavesSpawned            = 0;
        scheduleNextWave(absoluteTurn);
    }

public void markDestroyed() {
        phase = Phase.DESTROYED;
        invasionCount++;
    }

// ─── Per-turn tick ────────────────────────────────────────────────────────

    /** Called each turn. Returns true if countdown just expired. */
    public boolean tickCountdown() {
        if (phase != Phase.COUNTDOWN) return false;
        countdownTurns--;
        return countdownTurns <= 0;
    }

    public void tickInvasion() {
        if (phase == Phase.ACTIVE) turnsSinceInvasionStart++;
    }

    // ─── Wave scheduling ─────────────────────────────────────────────────────

    public void scheduleNextWave(int absoluteTurn) {
        int interval = BarbarianParams.BARB_WAVE_MIN_TURNS
                     + rng.nextInt(BarbarianParams.BARB_WAVE_MAX_TURNS
                                 - BarbarianParams.BARB_WAVE_MIN_TURNS + 1);
        nextWaveTurn    = absoluteTurn + interval;
        waveHalfPending = 0;
    }

    /** True if a wave's first half should spawn this turn. */
    public boolean isWaveDue(int absoluteTurn) {
        return phase == Phase.ACTIVE && absoluteTurn >= nextWaveTurn && waveHalfPending == 0;
    }

    /** True if the second half of a wave is pending this turn. */
    public boolean isWaveSecondHalfDue() {
        return phase == Phase.ACTIVE && waveHalfPending == 1;
    }

    public void markFirstHalfSpawned()  { waveHalfPending = 1; }
    public void markSecondHalfSpawned() {
        waveHalfPending = 0;
        wavesSpawned++;
    }

    // ─── Accessors ───────────────────────────────────────────────────────────

    public Phase getPhase()                    { return phase; }
    public boolean isActive()                  { return phase == Phase.ACTIVE; }
    public boolean isCountdown()               { return phase == Phase.COUNTDOWN; }
    public int     getCountdownTurns()         { return countdownTurns; }
    public int     getTurnsSinceInvasionStart(){ return turnsSinceInvasionStart; }
    public int     getNextWaveTurn()           { return nextWaveTurn; }

/**
 * Returns true for the first N invasions (0-indexed).
 * During an early invasion, ALL armies including the warboss spawn at reduced size.
 */
public boolean isEarlyInvasion() {
    return invasionCount < BarbarianParams.BARB_EARLY_INVASION_COUNT;
}

/** Returns true for the first N waves within the current invasion. */
public boolean isEarlyWave() {
    return wavesSpawned < BarbarianParams.BARB_EARLY_WAVE_COUNT;
}

public int getInvasionCount() { return invasionCount; }
public void setInvasionCount(int v) { invasionCount = v; }

public int getWavesSpawnedCount() { return wavesSpawned; }

public int getWavesSpawned() { return wavesSpawned; }

}