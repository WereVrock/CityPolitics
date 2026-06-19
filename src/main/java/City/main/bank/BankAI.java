package City.main.bank;

import City.debug.Debug;
import City.main.nobles.NobleArmy;
import City.main.nobles.NobleArmyManager;
import City.main.nobles.NobleHouse;
import City.main.nobles.Relationship;
import City.main.nobles.RelationshipManager;
import City.main.nobles.ai.NobleAIPower;

import java.util.List;

/**
 * The Bank's own per-turn survival behaviour.
 */
public final class BankAI {

    private BankAI() {}

    public static List<String> tick(NobleHouse bankHouse,
                                     List<NobleHouse> allHouses,
                                     RelationshipManager relationships,
                                     NobleArmyManager armyManager,
                                     BankManager bankManager,
                                     List<String> log) {
        if (!bankManager.isBankThreatened(allHouses)) return log;

        if (bankManager.getMercenaryManpower() > 0) {
            NobleArmy army = bankManager.recruitMercenaryArmy(
                    bankManager.getMercenaryManpower(), armyManager);
            if (army != null) {
                log.add("The Bank musters " + army.getSize() + " mercenaries to defend its vaults.");
                Debug.log("bank", "ai-merc-muster", "Bank raised mercenary army size=" + army.getSize());
            }
        }

        NobleHouse protector = findStrongestFriendlyProtector(bankHouse, allHouses, relationships, armyManager);
        if (protector != null && !bankManager.isProtector(protector.getId())) {
            bankManager.markProtector(protector.getId());
            log.add("The Bank quietly offers " + protector.getName()
                    + " favourable credit in exchange for a defensive guarantee.");
            Debug.log("bank", "ai-protector", "Bank offers protector terms to " + protector.getName());
        } else if (protector == null) {
            log.add("The Bank petitions the crown for a public guarantee of its safety.");
            Debug.log("bank", "ai-king-guarantee", "Bank asks for crown guarantee");
        }
        return log;
    }

    private static NobleHouse findStrongestFriendlyProtector(NobleHouse bankHouse,
                                                               List<NobleHouse> allHouses,
                                                               RelationshipManager relationships,
                                                               NobleArmyManager armyManager) {
        NobleHouse best  = null;
        int        power = 0;
        for (NobleHouse other : allHouses) {
            if (other == bankHouse || other.isEliminated()) continue;
            Relationship rel = relationships.get(bankHouse.getId(), other.getId());
            if (rel == Relationship.HOSTILE || rel == Relationship.RIVAL) continue;
            int p = NobleAIPower.estimateAttackPower(other, armyManager);
            if (p > power) { power = p; best = other; }
        }
        return best;
    }
}