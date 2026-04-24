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
}