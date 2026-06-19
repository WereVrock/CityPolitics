package City.main.nobles.ai;

import City.debug.Debug;
import City.main.bank.BankAccount;
import City.main.bank.BankManager;
import City.main.nobles.NobleHouse;
import City.main.parameters.BankParams;

import java.util.List;

/**
 * Decides whether a (non-Bank) noble house deposits excess gold or borrows
 * from the Bank this turn.
 */
public final class NobleBankingAI {

    private NobleBankingAI() {}

    public static void tick(NobleHouse house, int warChestTarget,
                             BankManager bankManager, List<String> log) {
        if (bankManager == null || house.isBank()) return;

        depositExcess(house, warChestTarget, bankManager, log);
        borrowIfNeeded(house, warChestTarget, bankManager, log);
    }

    private static void depositExcess(NobleHouse house, int warChestTarget,
                                       BankManager bankManager, List<String> log) {
        boolean smallHouse = isSmallRelativeToBank(house);
        int reserve = smallHouse ? 0 : warChestTarget;
        int excess  = house.getGold() - reserve;
        if (excess <= 0) return;

        int toDeposit = smallHouse ? excess
                : (int) (excess * BankParams.BANK_AI_DEPOSIT_EXCESS_FRACTION);
        if (toDeposit > 0 && bankManager.deposit(house, toDeposit, log)) {
            Debug.log("bank", "ai-deposit", house.getName() + " deposited " + toDeposit);
        }
    }

    private static boolean isSmallRelativeToBank(NobleHouse house) {
        return house.getGold() < BankParams.BANK_MIN_DEPOSIT_FLAT_GOLD * 4;
    }

    private static void borrowIfNeeded(NobleHouse house, int warChestTarget,
                                        BankManager bankManager, List<String> log) {
        if (bankManager.hasActiveLoan(house.getId())) return;
        int shortfall = warChestTarget - house.getGold();
        if (shortfall <= 0) return;

        BankAccount acc = bankManager.getOrCreateAccount(house.getId());
        if (acc.getCreditRating() < 25) return;

        int maxLoan = bankManager.getMaxLoanAmount(house);
        if (maxLoan <= 0) return;

        int amount = Math.min(shortfall, maxLoan);
        if (amount > 0) bankManager.requestLoan(house, amount, null, log);
    }
}