package City.main.bank;

import City.debug.Debug;
import City.main.nobles.NobleArmy;
import City.main.nobles.NobleArmyManager;
import City.main.nobles.NobleHouse;
import City.main.parameters.DragonBankParams;
import City.main.resources.ResourcePool;

import java.util.*;

/**
 * The Dragon Bank — an abstract, distant lending and deposit service with
 * no physical presence, no garrison, and no zone. It cannot be attacked or
 * robbed; "the dragon punishes those who interfere". In exchange for that
 * safety: no deposit interest, high loan interest, and a one-turn courier
 * delay on both loan disbursement and withdrawals.
 *
 * Noble houses may deposit (no minimum) and borrow. The player may only
 * borrow — deposits would protect against conquest gold-theft, a risk the
 * player's treasury doesn't have.
 */
public class DragonBankManager {

    public static final String PLAYER_ID = "player";

    private final NobleArmyManager armyManager;

    private final Map<String, Integer>          deposits           = new LinkedHashMap<>();
    private final Map<String, DragonBankLoan>    loans              = new LinkedHashMap<>();
    private final Map<String, PendingLoan>       pendingLoans       = new LinkedHashMap<>();
    private final Map<String, PendingWithdrawal> pendingWithdrawals = new LinkedHashMap<>();
    private final Map<String, Integer>           loanBanTurnsLeft   = new LinkedHashMap<>();

    public DragonBankManager(NobleArmyManager armyManager) {
        this.armyManager = armyManager;
    }

    // ─── Deposits (noble houses only) ──────────────────────────────────────

    public int getDeposit(String houseId) {
        return deposits.getOrDefault(houseId, 0);
    }

    /** No minimum deposit — unlike the Frostpeak Bank. */
    public boolean depositHouse(NobleHouse house, int amount, List<String> log) {
        if (amount <= 0 || amount > house.getGold()) return false;
        if (isHouseUnderAttack(house)) {
            Debug.log("dragonbank", "deposit-blocked", house.getName() + " is under attack, cannot send a caravan");
            return false;
        }
        house.addGold(-amount);
        deposits.merge(house.getId(), amount, Integer::sum);
        if (log != null) log.add(house.getName() + " entrusts " + amount + " gold to the dragon's agents.");
        Debug.log("dragonbank", "deposit", house.getName() + " deposited " + amount
                + " (balance=" + getDeposit(house.getId()) + ")");
        return true;
    }

    public boolean withdrawHouse(NobleHouse house, int amount, List<String> log) {
        if (amount <= 0) return false;
        int balance = getDeposit(house.getId());
        if (amount > balance) return false;
        if (pendingWithdrawals.containsKey(house.getId())) {
            if (log != null) log.add(house.getName() + " already has a withdrawal in transit with the dragon's agents.");
            return false;
        }
        deposits.put(house.getId(), balance - amount);
        pendingWithdrawals.put(house.getId(),
                new PendingWithdrawal(amount, DragonBankParams.WITHDRAWAL_DELAY_TURNS));
        if (log != null) log.add(house.getName() + " recalls " + amount
                + " gold from the dragon's agents — it will arrive in "
                + DragonBankParams.WITHDRAWAL_DELAY_TURNS + " turn(s).");
        Debug.log("dragonbank", "withdraw-requested", house.getName() + " requested " + amount);
        return true;
    }

    public boolean hasPendingWithdrawal(String id) { return pendingWithdrawals.containsKey(id); }

    // ─── Loans — noble houses ───────────────────────────────────────────────

    public int getMaxLoanAmount(int currentGold) {
        return Math.max(DragonBankParams.MAX_LOAN_FLAT_FLOOR,
                (int) (currentGold * DragonBankParams.MAX_LOAN_GOLD_MULTIPLIER));
    }

    public boolean hasActiveLoan(String id)        { return loans.containsKey(id); }
    public boolean hasPendingLoan(String id)       { return pendingLoans.containsKey(id); }
    public boolean isBanned(String id)             { return loanBanTurnsLeft.getOrDefault(id, 0) > 0; }
    public int     getBanTurnsRemaining(String id) { return loanBanTurnsLeft.getOrDefault(id, 0); }
    public DragonBankLoan getLoan(String id)       { return loans.get(id); }

    public int getPendingLoanAmount(String id) {
        PendingLoan p = pendingLoans.get(id);
        return p != null ? p.amount : 0;
    }

    public int getPendingLoanTurnsRemaining(String id) {
        PendingLoan p = pendingLoans.get(id);
        return p != null ? p.turnsRemaining : 0;
    }

    public boolean requestLoan(NobleHouse house, int amount, List<String> log) {
        if (amount <= 0) return false;
        String id = house.getId();
        if (loans.containsKey(id) || pendingLoans.containsKey(id)) return false;
        if (isBanned(id)) return false;
        if (amount > getMaxLoanAmount(house.getGold())) return false;

        pendingLoans.put(id, new PendingLoan(amount, DragonBankParams.LOAN_DELAY_TURNS));
        if (log != null) log.add(house.getName() + " sends word to the dragon's agents for a loan of "
                + amount + " gold — it will arrive in " + DragonBankParams.LOAN_DELAY_TURNS + " turn(s).");
        Debug.log("dragonbank", "loan-requested", house.getName() + " requested loan of " + amount);
        return true;
    }

    // ─── Loans — player ──────────────────────────────────────────────────

    public boolean requestLoanPlayer(ResourcePool resources, int amount, List<String> log) {
        if (amount <= 0) return false;
        if (loans.containsKey(PLAYER_ID) || pendingLoans.containsKey(PLAYER_ID)) {
            if (log != null) log.add("You already have a loan with the dragon's agents — settle it before borrowing again.");
            return false;
        }
        if (isBanned(PLAYER_ID)) {
            if (log != null) log.add("The dragon's agents refuse to deal with you for " + getBanTurnsRemaining(PLAYER_ID) + " more turn(s).");
            return false;
        }
        int maxLoan = getMaxLoanAmount(resources.getMoney());
        if (amount > maxLoan) {
            if (log != null) log.add("The dragon's agents will lend at most " + maxLoan + " gold right now.");
            return false;
        }
        pendingLoans.put(PLAYER_ID, new PendingLoan(amount, DragonBankParams.LOAN_DELAY_TURNS));
        if (log != null) log.add("You send word to the dragon's agents for a loan of " + amount
                + " gold at " + String.format("%.0f%%", DragonBankParams.LOAN_INTEREST_RATE * 100)
                + " interest — it will arrive in " + DragonBankParams.LOAN_DELAY_TURNS + " turn(s).");
        Debug.log("dragonbank", "loan-requested", "Player requested loan of " + amount);
        return true;
    }

public boolean payInstallmentPlayer(ResourcePool resources, List<String> log) {
        DragonBankLoan loan = loans.get(PLAYER_ID);
        if (loan == null || loan.isPaidOff()) {
            Debug.log("dragonbank", "pay-installment-fail", "No active loan to pay");
            return false;
        }
        int due = loan.getNextInstallmentDue();
        if (resources.getMoney() < due) {
            if (log != null) log.add("Need " + due + " gold for this installment but you only have " + resources.getMoney() + ".");
            Debug.log("dragonbank", "pay-installment-fail", "Insufficient funds: need " + due + ", have " + resources.getMoney());
            return false;
        }
        resources.spendMoney(due);
        loan.applyPayment(due);
        if (loan.isPaidOff()) {
            loans.remove(PLAYER_ID);
            if (log != null) log.add("Your debt to the dragon's agents is settled in full.");
            Debug.log("dragonbank", "player-loan-repaid", "Player fully repaid loan (manual)");
        } else if (log != null) {
            log.add("Paid " + due + " gold. " + loan.getInstallmentsRemaining()
                    + " installments remain (" + loan.getFullRepaymentAmount() + " gold total).");
            Debug.log("dragonbank", "pay-installment-success", "Paid " + due + " gold, remaining: " + loan.getFullRepaymentAmount());
        }
        return true;
    }

public boolean repayLoanFullPlayer(ResourcePool resources, List<String> log) {
        DragonBankLoan loan = loans.get(PLAYER_ID);
        if (loan == null || loan.isPaidOff()) {
            Debug.log("dragonbank", "repay-full-fail", "No active loan to repay");
            return false;
        }
        int owed = loan.getFullRepaymentAmount();
        if (resources.getMoney() < owed) {
            if (log != null) log.add("Need " + owed + " gold to repay in full but you only have " + resources.getMoney() + ".");
            Debug.log("dragonbank", "repay-full-fail", "Insufficient funds: need " + owed + ", have " + resources.getMoney());
            return false;
        }
        resources.spendMoney(owed);
        loans.remove(PLAYER_ID);
        if (log != null) log.add("You repay " + owed + " gold and settle your debt to the dragon's agents.");
        Debug.log("dragonbank", "player-loan-repaid", "Player fully repaid loan (manual full) for " + owed + " gold");
        return true;
    }

// ─── Turn processing ─────────────────────────────────────────────────

    public List<String> processTurn(List<NobleHouse> allHouses, ResourcePool playerResources) {
        List<String> log = new ArrayList<>();

        tickBans();
        resolvePendingLoans(allHouses, playerResources, log);
        resolvePendingWithdrawals(allHouses, log);
        collectInstallments(allHouses, log);
        collectPlayerInstallment(playerResources, log);

        return log;
    }

    private void tickBans() {
        for (Iterator<Map.Entry<String, Integer>> it = loanBanTurnsLeft.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Integer> e = it.next();
            int left = e.getValue() - 1;
            if (left <= 0) it.remove();
            else e.setValue(left);
        }
    }

private void resolvePendingLoans(List<NobleHouse> allHouses, ResourcePool playerResources, List<String> log) {
        for (Iterator<Map.Entry<String, PendingLoan>> it = pendingLoans.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, PendingLoan> e = it.next();
            PendingLoan pending = e.getValue();
            pending.turnsRemaining--;
            if (pending.turnsRemaining > 0) continue;

            String id = e.getKey();
            DragonBankLoan loan = new DragonBankLoan(id, pending.amount,
                    DragonBankParams.LOAN_INTEREST_RATE, DragonBankParams.LOAN_INSTALLMENTS_DEFAULT);
            loans.put(id, loan);

            if (id.equals(PLAYER_ID)) {
                playerResources.setMoney(playerResources.getMoney() + pending.amount);
                log.add("The dragon's agents arrive with " + pending.amount + " gold for you. Total owed: "
                        + loan.getFullRepaymentAmount() + " gold.");
                Debug.log("dragonbank", "player-loan-arrived", "Player received " + pending.amount
                        + " gold, total owed " + loan.getFullRepaymentAmount());
            } else {
                NobleHouse house = findHouse(id, allHouses);
                if (house != null) {
                    house.addGold(pending.amount);
                    log.add("The dragon's agents arrive with " + pending.amount + " gold for " + house.getName() + ".");
                    Debug.log("dragonbank", "house-loan-arrived", house.getName() + " received " + pending.amount
                            + " gold, total owed " + loan.getFullRepaymentAmount());
                }
            }
            it.remove();
        }
    }

private void resolvePendingWithdrawals(List<NobleHouse> allHouses, List<String> log) {
        for (Iterator<Map.Entry<String, PendingWithdrawal>> it = pendingWithdrawals.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, PendingWithdrawal> e = it.next();
            PendingWithdrawal pending = e.getValue();
            pending.turnsRemaining--;
            if (pending.turnsRemaining > 0) continue;

            NobleHouse house = findHouse(e.getKey(), allHouses);
            if (house != null) {
                house.addGold(pending.amount);
                log.add(house.getName() + "'s recalled gold arrives from the dragon's agents: " + pending.amount + " gold.");
                Debug.log("dragonbank", "withdrawal-arrived", house.getName() + " received " + pending.amount + " gold from withdrawal");
            }
            it.remove();
        }
    }

private void collectInstallments(List<NobleHouse> allHouses, List<String> log) {
        for (NobleHouse house : new ArrayList<>(allHouses)) {
            DragonBankLoan loan = loans.get(house.getId());
            if (loan == null) continue;
            if (loan.isPaidOff()) { loans.remove(house.getId()); continue; }

            int due = loan.getNextInstallmentDue();

            int fromDeposit = Math.min(due, getDeposit(house.getId()));
            if (fromDeposit > 0) {
                deposits.put(house.getId(), getDeposit(house.getId()) - fromDeposit);
                loan.applyPayment(fromDeposit);
            }

            int remaining = due - fromDeposit;
            if (remaining <= 0) {
                if (loan.isPaidOff()) {
                    loans.remove(house.getId());
                    log.add(house.getName() + " settles its debt to the dragon's agents in full.");
                    Debug.log("dragonbank", "loan-repaid", house.getName() + " fully repaid loan (from deposit)");
                }
                continue;
            }

            if (house.getGold() >= remaining) {
                house.addGold(-remaining);
                loan.applyPayment(remaining);
                if (loan.isPaidOff()) {
                    loans.remove(house.getId());
                    log.add(house.getName() + " settles its debt to the dragon's agents in full.");
                    Debug.log("dragonbank", "loan-repaid", house.getName() + " fully repaid loan (from gold)");
                }
            } else {
                defaultLoan(house, loan, log);
            }
        }
    }

private void collectPlayerInstallment(ResourcePool resources, List<String> log) {
        if (resources == null) return;
        DragonBankLoan loan = loans.get(PLAYER_ID);
        if (loan == null) return;
        if (loan.isPaidOff()) { loans.remove(PLAYER_ID); return; }

        int due = loan.getNextInstallmentDue();
        if (resources.getMoney() >= due) {
            resources.spendMoney(due);
            loan.applyPayment(due);
            if (loan.isPaidOff()) {
                loans.remove(PLAYER_ID);
                log.add("Your debt to the dragon's agents is settled in full.");
                Debug.log("dragonbank", "player-loan-repaid", "Player fully repaid loan (auto-collect)");
            } else {
                log.add("The dragon's agents collect " + due + " gold from your treasury. "
                        + loan.getInstallmentsRemaining() + " installments remain.");
            }
        } else {
            loans.remove(PLAYER_ID);
            loanBanTurnsLeft.put(PLAYER_ID, DragonBankParams.DEFAULT_BAN_TURNS);
            log.add("⚠ You default on your debt to the dragon's agents! They refuse to deal with you for "
                    + DragonBankParams.DEFAULT_BAN_TURNS + " turns.");
            Debug.log("dragonbank", "player-default", "Player defaulted on Dragon Bank loan (auto-collect)");
        }
    }

private void defaultLoan(NobleHouse house, DragonBankLoan loan, List<String> log) {
        loan.markDefaulted();
        loans.remove(house.getId());
        loanBanTurnsLeft.put(house.getId(), DragonBankParams.DEFAULT_BAN_TURNS);
        house.addPrestige(-DragonBankParams.DEFAULT_PRESTIGE_PENALTY);
        log.add(house.getName() + " defaults on its debt to the dragon's agents and loses standing.");
        Debug.log("dragonbank", "default", house.getName() + " defaulted on Dragon Bank loan");
    }

    private boolean isHouseUnderAttack(NobleHouse house) {
        for (NobleArmy a : armyManager.getAllArmies()) {
            if (a.getPendingOrder() != NobleArmy.OrderType.ATTACK) continue;
            String targetZone = a.getPendingTargetZoneId();
            if (targetZone != null && house.getZoneIds().contains(targetZone)) return true;
        }
        return false;
    }

    private NobleHouse findHouse(String id, List<NobleHouse> all) {
        for (NobleHouse h : all) if (h.getId().equals(id)) return h;
        return null;
    }

    public void reset() {
        deposits.clear();
        loans.clear();
        pendingLoans.clear();
        pendingWithdrawals.clear();
        loanBanTurnsLeft.clear();
    }

    // ─── Save/load raw access ────────────────────────────────────────────

    public Map<String, Integer>           getDepositsRaw()           { return deposits; }
    public Map<String, DragonBankLoan>    getLoansRaw()              { return loans; }
    public Map<String, PendingLoan>       getPendingLoansRaw()       { return pendingLoans; }
    public Map<String, PendingWithdrawal> getPendingWithdrawalsRaw() { return pendingWithdrawals; }
    public Map<String, Integer>           getLoanBansRaw()           { return loanBanTurnsLeft; }

    // ─── Pending request records ─────────────────────────────────────────

    public static class PendingLoan {
        public int amount;
        public int turnsRemaining;
        public PendingLoan(int amount, int turnsRemaining) {
            this.amount = amount;
            this.turnsRemaining = turnsRemaining;
        }
    }

    public static class PendingWithdrawal {
        public int amount;
        public int turnsRemaining;
        public PendingWithdrawal(int amount, int turnsRemaining) {
            this.amount = amount;
            this.turnsRemaining = turnsRemaining;
        }
    }
}