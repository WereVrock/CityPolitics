package City.main.bank;

import City.debug.Debug;
import City.main.barbarians.BarbArmyManager;
import City.main.map.ZoneManager;
import City.main.nobles.NobleArmy;
import City.main.nobles.NobleArmyManager;
import City.main.nobles.NobleHouse;
import City.main.nobles.Relationship;
import City.main.nobles.RelationshipManager;
import City.main.nobles.ai.NobleAIPower;
import City.main.parameters.BankParams;

import java.util.List;

/**
 * The Bank's own per-turn survival behaviour: sizes its mercenary pool to
 * the single biggest threat it faces, recruits from that pool when
 * threatened, and lines up a protector or crown guarantee when it can't
 * defend itself alone.
 */
public final class BankAI {

    private BankAI() {}

    public static List<String> tick(NobleHouse bankHouse,
                                     List<NobleHouse> allHouses,
                                     RelationshipManager relationships,
                                     NobleArmyManager armyManager,
                                     BankManager bankManager,
                                     ZoneManager zoneManager,
                                     BarbArmyManager barbArmyManager,
                                     List<String> log) {

        int threat  = bankManager.getStrongestThreat(allHouses, relationships, armyManager, zoneManager, barbArmyManager);
        int defence = bankManager.getTotalDefence(allHouses, armyManager);

        int targetMercs = Math.max(0, (int) Math.ceil(
                threat * BankParams.BANK_AI_THREAT_COVERAGE_RATIO - defence));

        int budget        = threat > 0 ? bankManager.getEmergencyFund() : bankManager.getOwnGold();
        int maxAffordable = (int) (budget / (1.0 / 3.0));

        int desiredMercs = threat > 0
                ? targetMercs
                : Math.min(targetMercs, Math.max(0, maxAffordable));

        bankManager.setDesiredMercenaries(desiredMercs);
        if (bankManager.getMercenaryManpower() > desiredMercs) {
            bankManager.setMercenaryManpower(desiredMercs);
            Debug.log("bank", "ai-merc-shrink", "Bank scales mercenary pool down to " + desiredMercs);
        }

        if (threat > 0 && bankManager.getMercenaryManpower() > 0) {
            NobleArmy army = bankManager.recruitMercenaryArmy(
                    bankManager.getMercenaryManpower(), armyManager);
            if (army != null) {
                log.add("The Bank musters " + army.getSize() + " mercenaries to defend its vaults.");
                Debug.log("bank", "ai-merc-muster", "Bank raised mercenary army size=" + army.getSize());
            }
        }

        if (threat == 0) return log;

        NobleHouse protector = findStrongestFriendlyProtector(bankHouse, allHouses, relationships, armyManager);
        if (protector != null && !bankManager.isProtector(protector.getId())) {
            if (bankManager.markProtector(protector)) {
                log.add("The Bank quietly offers " + protector.getName()
                        + " favourable credit in exchange for a defensive guarantee.");
                Debug.log("bank", "ai-protector", "Bank offers protector terms to " + protector.getName());
            }
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