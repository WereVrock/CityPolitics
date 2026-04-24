package main.calendar;

import main.parameters.GameParameters;

/**
 * Tracks in-game time.
 * Two Periods (Thaw, Frost) make one Year.
 * Year 184 A.S. (After Sundering) = campaign start.
 */
public class GameCalendar {

    public enum Period {
        THAW("The Thaw"),
        FROST("The Frost");

        private final String displayName;

        Period(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private int year;
    private Period period;
    private int totalTurnsElapsed;

    public GameCalendar() {
        this.year              = GameParameters.START_YEAR;
        this.period            = Period.THAW;
        this.totalTurnsElapsed = 0;
    }

    /**
     * Advances one period. Rolls the year when FROST ends.
     */
    public void advance() {
        totalTurnsElapsed++;
        if (period == Period.THAW) {
            period = Period.FROST;
        } else {
            period = Period.THAW;
            year++;
        }
    }

    public boolean isFrostGiantYear() {
        return year >= GameParameters.FROST_GIANT_ARRIVAL_YEAR;
    }

    public int getTurnsUntilFrostGiants() {
        int yearsLeft    = GameParameters.FROST_GIANT_ARRIVAL_YEAR - year;
        int periodsLeft  = yearsLeft * GameParameters.PERIODS_PER_YEAR;
        if (period == Period.FROST) periodsLeft--;
        return Math.max(0, periodsLeft);
    }

    public int getYear()                { return year; }
    public Period getPeriod()           { return period; }
    public int getTotalTurnsElapsed()   { return totalTurnsElapsed; }

    public void reset() {
        this.year              = GameParameters.START_YEAR;
        this.period            = Period.THAW;
        this.totalTurnsElapsed = 0;
    }

    

    public void setYear(int year)                        { this.year = year; }
    public void setPeriod(Period period)                 { this.period = period; }
    public void setTotalTurnsElapsed(int turns)          { this.totalTurnsElapsed = turns; }

    public String getDisplayString() {
        return period.getDisplayName() + ", Year " + year + " A.S.";
    }
}