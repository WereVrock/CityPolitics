// ArmyManager.java
package main.army;

import debug.Debug;
import java.util.*;

/**
 * Owns all armies.
 */
public class ArmyManager {

    private final List<Army>        armies   = new ArrayList<>();
    private final Map<String, Army> armyById = new LinkedHashMap<>();

    public ArmyManager() {
        spawnStartingArmies();
    }

private void spawnStartingArmies() {
        addArmy(new Army("army_1", "Thunder Legion",
                new Commander("General Aldric Vane", "Human",
                        main.politics.PolitcalView.MILITARIST, 3)));
        addArmy(new Army("army_2", "Knights of Ruin",
                new Commander("Commander Syla Dorn", "Human",
                        main.politics.PolitcalView.HUMAN_SUPREMACIST, 2)));
        addArmy(new Army("army_3", "Iron Wolves",
                new Commander("Warchief Brunn Ashfist", "Orc",
                        main.politics.PolitcalView.WARMONGERING, 4)));
    }

private void addArmy(Army army) {
        armies.add(army);
        armyById.put(army.getId(), army);
    }

    public List<Army> getArmies()        { return Collections.unmodifiableList(armies); }
    public Army       getArmy(String id) { return armyById.get(id); }

    /** Armies currently in heartland (shown in city list). Excludes dragging ones. */

/**
     * Armies currently in heartland (shown in city list). Excludes dragging ones.
     * Armies without a living commander are always considered in-city even if
     * their zoneId somehow got set externally — they cannot deploy.
     */
    public List<Army> getCityArmies() {
        List<Army> result = new ArrayList<>();
        for (Army a : armies) {
            if (a.isInCity() && !a.isDragging()) result.add(a);
        }
        return result;
    }

/** Armies deployed outside heartland. Excludes dragging ones. */

/**
     * Armies deployed outside heartland that have a living commander.
     * Armies whose commander has died are recalled to heartland on access.
     * Excludes dragging ones.
     */
    public List<Army> getDeployedArmies() {
        List<Army> result = new ArrayList<>();
        for (Army a : armies) {
            if (!a.isInCity() && !a.isDragging()) {
                // Enforce commander requirement: recall if commander is gone
                if (!a.hasLivingCommander()) {
                    a.recallToCity();
                    Debug.log("army-manager", "forced-recall",
                            a.getId() + " recalled — no living commander");
                } else {
                    result.add(a);
                }
            }
        }
        return result;
    }

public void reset() {
        armies.clear();
        armyById.clear();
        spawnStartingArmies();
    }
}