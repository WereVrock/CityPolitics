// ArmyManager.java
package main.army;

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
        addArmy(new Army("army_1"));
        addArmy(new Army("army_2"));
        addArmy(new Army("army_3"));
    }

    private void addArmy(Army army) {
        armies.add(army);
        armyById.put(army.getId(), army);
    }

    public List<Army> getArmies()        { return Collections.unmodifiableList(armies); }
    public Army       getArmy(String id) { return armyById.get(id); }

    /** Armies currently in heartland (shown in city list). Excludes dragging ones. */
    public List<Army> getCityArmies() {
        List<Army> result = new ArrayList<>();
        for (Army a : armies) {
            if (a.isInCity() && !a.isDragging()) result.add(a);
        }
        return result;
    }

    /** Armies deployed outside heartland. Excludes dragging ones. */
    public List<Army> getDeployedArmies() {
        List<Army> result = new ArrayList<>();
        for (Army a : armies) {
            if (a.isDeployed() && !a.isDragging()) result.add(a);
        }
        return result;
    }

    public void reset() {
        armies.clear();
        armyById.clear();
        spawnStartingArmies();
    }
}