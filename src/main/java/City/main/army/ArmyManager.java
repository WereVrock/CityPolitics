// ArmyManager.java
package City.main.army;

import City.debug.Debug;
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
        // Starting armies have no party-affiliated commanders —
        // player assigns commanders via the Military panel.
        addArmy(new Army("army_1", "Thunder Legion"));
        addArmy(new Army("army_2", "Knights of Ruin"));
        addArmy(new Army("army_3", "Iron Wolves"));
    }

private void addArmy(Army army) {
        armies.add(army);
        armyById.put(army.getId(), army);
        Debug.log("army-manager", "add", army.getId() + " — " + army.getDisplayName());
    }

    /**
     * Creates a new army in heartland with the given display name and no commander.
     * ID is generated automatically and guaranteed unique.
     */
    public Army createArmy(String displayName) {
        String id   = generateId();
        Army   army = new Army(id, displayName);
        addArmy(army);
        Debug.log("army-manager", "create", id + " — " + displayName);
        return army;
    }

    /**
     * Removes an army from the roster entirely.
     * Caller is responsible for returning soldiers to manpower before calling this.
     */
    public void removeArmy(Army army) {
        armies.remove(army);
        armyById.remove(army.getId());
        Debug.log("army-manager", "remove", army.getId() + " — " + army.getDisplayName());
    }

    /**
     * Merges source into target: transfers all soldiers, then removes source.
     * Source must be in heartland. Target receives soldiers.
     * Returns number of soldiers transferred.
     */
    public int mergeArmies(Army source, Army target) {
        int transferred = source.getSoldierCount();
        target.addSoldiers(transferred);
        source.removeSoldiers(transferred);
        removeArmy(source);
        Debug.log("army-manager", "merge",
                source.getDisplayName() + " → " + target.getDisplayName()
                + " transferred=" + transferred);
        return transferred;
    }

    private String generateId() {
        int n = armies.size() + 1;
        String candidate = "army_" + n;
        while (armyById.containsKey(candidate)) {
            n++;
            candidate = "army_" + n;
        }
        return candidate;
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
        for (Army a : new ArrayList<>(armies)) {
            if (!a.isInCity() && !a.isDragging()) {
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

/**
     * Returns armies in heartland that can potentially be deployed.
     * Armies without a living commander are recalled here automatically.
     */
    public List<Army> getHeartlandArmies() {
        List<Army> result = new ArrayList<>();
        for (Army a : armies) {
            if (a.isInCity()) result.add(a);
        }
        return result;
    }

    public void reset() {
        armies.clear();
        armyById.clear();
        spawnStartingArmies();
    }
}