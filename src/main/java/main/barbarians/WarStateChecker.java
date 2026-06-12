package main.barbarians;

/**
 * Checks whether the realm is currently in a state of war.
 * War is true if at least 1 barbarian ravager or warboss is alive.
 * More conditions can be added later.
 */
public class WarStateChecker {

    private final BarbArmyManager barbArmyManager;

    public WarStateChecker(BarbArmyManager barbArmyManager) {
        this.barbArmyManager = barbArmyManager;
    }

    public boolean isAtWar() {
        for (BarbArmy army : barbArmyManager.getMobileArmies()) {
            if (army.isAlive() && (army.isRavager() || army.isWarboss())) return true;
        }
        return false;
    }
}