package City.main.nobles.ai;

import City.debug.Debug;
import City.main.bank.BankAccount;
import City.main.bank.BankManager;
import City.main.bank.DragonBankManager;
import City.main.nobles.NobleHouse;
import City.main.nobles.Relationship;
import City.main.nobles.RelationshipManager;
import City.main.parameters.BankParams;

import java.util.List;

/**
 * Decides whether a (non-Bank) noble house deposits excess gold or borrows
 * from the Bank this turn.  When the Frostpeak Bank cannot help, the house
 * turns to the Dragon Bank.
 */
public final class NobleBankingAI {

    private NobleBankingAI() {}

    public static void tick(NobleHouse house, int warChestTarget,
                             BankManager bankManager, DragonBankManager dragonBankManager,
                             List<NobleHouse> allHouses, RelationshipManager relationships,
                             List<String> log) {
        if (house.isBank()) return;

        // ── 1. Regular banking: deposit excess, then withdraw / borrow ──
        boolean regularBankAvailable = bankManager != null && bankManager.getBankHouse() != null
                && !bankManager.getBankHouse().isEliminated();
        if (regularBankAvailable) {
            regularBankDeposit(house, warChestTarget, bankManager, log);
            boolean gotMoney = regularBankWithdrawThenBorrow(house, warChestTarget, bankManager, log);
            if (gotMoney) return;          // need satisfied by regular bank
        }

        // ── 2. Dragon Bank fallback – loan only ────────────────────────
        if (dragonBankManager == null) return;
        if (dragonBankManager.hasActiveLoan(house.getId())
                || dragonBankManager.hasPendingLoan(house.getId())
                || dragonBankManager.isBanned(house.getId())) return;

        int shortfall = Math.max(0, warChestTarget - house.getGold());
        if (shortfall <= 0) return;

        // Only borrow from dragon if regular bank loan was impossible, or
        // the house is hostile / rival to the Frostpeak Bank.
        boolean hostileToBank = false;
        if (regularBankAvailable && relationships != null) {
            NobleHouse bankHouse = bankManager.getBankHouse();
            Relationship rel = relationships.get(house.getId(), bankHouse.getId());
            hostileToBank = (rel == Relationship.HOSTILE || rel == Relationship.RIVAL);
        }

        boolean regularLoanFailed = true;           // we already exhausted regular options
        if (regularLoanFailed || hostileToBank) {
            int maxDragon = dragonBankManager.getMaxLoanAmount(house.getGold());
            int amount = Math.min(shortfall, maxDragon);
            if (amount > 0) {
                dragonBankManager.requestLoan(house, amount, log);
                Debug.log("dragonbank", "ai-borrow", house.getName() + " requested "
                        + amount + " gold from dragon's agents (shortfall " + shortfall + ")");
            }
        }
    }

    // ─── Regular-bank helpers ──────────────────────────────────────────

    private static void regularBankDeposit(NobleHouse house, int warChestTarget,
                                            BankManager bankManager, List<String> log) {
        BankAccount acc = bankManager.getOrCreateAccount(house.getId());
        boolean smallHouse = isSmallRelativeToBank(house, bankManager);
        int totalLiquidity = house.getGold() + acc.getDeposit();
        int effectiveReserve = smallHouse ? 0 : warChestTarget;
        int excess = Math.max(0, totalLiquidity - effectiveReserve);
        if (excess <= 0) return;

        int goldAvailable = house.getGold();
        int toDeposit;
        if (smallHouse) {
            toDeposit = goldAvailable;
        } else {
            toDeposit = (int) (excess * BankParams.BANK_AI_DEPOSIT_EXCESS_FRACTION);
            toDeposit = Math.min(toDeposit, goldAvailable);
        }
        if (toDeposit > 0) {
            bankManager.deposit(house, toDeposit, log);
            Debug.log("bank", "ai-deposit", house.getName() + " deposited " + toDeposit);
        }
    }

    /** Tries to withdraw then borrow from regular bank. Returns true if shortfall was covered. */
    private static boolean regularBankWithdrawThenBorrow(NobleHouse house, int warChestTarget,
                                                          BankManager bankManager, List<String> log) {
        BankAccount acc = bankManager.getOrCreateAccount(house.getId());
        int totalLiquidity = house.getGold() + acc.getDeposit();
        int shortfall = Math.max(0, warChestTarget - totalLiquidity);
        if (shortfall <= 0) return true;            // already enough

        // Withdraw from deposit first
        int withdrawable = Math.min(shortfall, acc.getDeposit());
        if (withdrawable > 0 && bankManager.withdraw(house, withdrawable, log)) {
            totalLiquidity = house.getGold() + acc.getDeposit();
            shortfall = Math.max(0, warChestTarget - totalLiquidity);
        }
        if (shortfall <= 0) return true;

        // Borrow if eligible
        if (bankManager.hasActiveLoan(house.getId())) return false;
        if (acc.getCreditRating() < 25) return false;
        int maxLoan = bankManager.getMaxLoanAmount(house);
        if (maxLoan <= 0) return false;

        int amount = Math.min(shortfall, maxLoan);
        if (amount > 0 && bankManager.requestLoan(house, amount, null, log)) {
            return true;
        }
        return false;
    }

    private static boolean isSmallRelativeToBank(NobleHouse house, BankManager bankManager) {
        // if the regular bank is gone, treat every house as "small" so they deposit all
        if (bankManager == null || bankManager.getBankHouse() == null) return true;
        return house.getGold() < BankParams.BANK_MIN_DEPOSIT_FLAT_GOLD * 4;
    }
}