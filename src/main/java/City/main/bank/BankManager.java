package City.main.bank;

import City.debug.Debug;
import City.main.barbarians.BarbArmy;
import City.main.barbarians.BarbArmyManager;
import City.main.map.Zone;
import City.main.map.ZoneManager;
import City.main.nobles.ClaimManager;
import City.main.nobles.NobleArmy;
import City.main.nobles.NobleArmyManager;
import City.main.nobles.NobleHouse;
import City.main.nobles.Relationship;
import City.main.nobles.RelationshipManager;
import City.main.nobles.ai.NobleAIPower;
import City.main.parameters.BankParams;
import City.main.parameters.NobleHouseParams;
import City.main.resources.ResourcePool;

import java.util.*;

/**
 * Owns all banking state: deposit accounts, loans, stakeholder/Bank-Robber
 * status, the Bank's secondary mercenary manpower pool, and the Bank's
 * survival response to threats — sizing its mercenary pool, recruiting
 * emergency muscle from forfeited deposits, and dissolving outright if its
 * capital falls.
 */
public class BankManager {

    /** Account ID used for the player (the king) in all bank operations. */
    public static final String PLAYER_HOUSE_ID = "player";

    private final RelationshipManager relationships;
    private final NobleArmyManager    armyManager;
    private       NobleHouse          bankHouse;

    private final Map<String, BankAccount> accounts         = new LinkedHashMap<>();
    private final Map<String, BankLoan>    loans            = new LinkedHashMap<>();
    private final Set<String>              bankRobbers      = new HashSet<>();
    private final Set<String>              protectorHouseIds = new HashSet<>();
    private final Set<String>              lastBattleDefenders = new HashSet<>();

    private int mercenaryManpower           = 0;
    private int withdrawDelayTurnsRemaining = 0;
    private int lastEndTurnGold             = 0;

    /** Ceiling set by BankAI each turn — growth never pushes the pool past this. */
    private int desiredMercenaries = Integer.MAX_VALUE;

    /** Total emergency mercenaries raised mid-battle during the current attack. */
    private int lastEmergencyMercenaryContribution = 0;

    public BankManager(RelationshipManager relationships, NobleArmyManager armyManager,
                        NobleHouse bankHouse) {
        this.relationships = relationships;
        this.armyManager   = armyManager;
        this.bankHouse     = bankHouse;
        if (bankHouse != null) this.lastEndTurnGold = bankHouse.getGold();
    }

    public void setBankHouse(NobleHouse house) {
        this.bankHouse = house;
        if (house != null) this.lastEndTurnGold = house.getGold();
    }

    public NobleHouse getBankHouse() { return bankHouse; }

    // ─── Account access ──────────────────────────────────────────────────

    public BankAccount getOrCreateAccount(String houseId) {
        return accounts.computeIfAbsent(houseId, BankAccount::new);
    }

    public BankAccount getAccount(String houseId) { return accounts.get(houseId); }

    public boolean isStakeholder(String houseId) {
        BankAccount a = accounts.get(houseId);
        return a != null && a.isStakeholder();
    }

    public List<String> getStakeholderIds() {
        List<String> result = new ArrayList<>();
        for (BankAccount a : accounts.values()) {
            if (a.isStakeholder()) result.add(a.getHouseId());
        }
        return result;
    }

    public boolean isBankRobber(String houseId)   { return bankRobbers.contains(houseId); }
    public int     getMercenaryManpower()         { return mercenaryManpower; }
    public Set<String> getLastBattleDefenders()   { return lastBattleDefenders; }

    // ─── Deposits / withdrawals ──────────────────────────────────────────

    public boolean isHouseUnderAttack(NobleHouse house) {
        for (NobleArmy a : armyManager.getAllArmies()) {
            if (a.getPendingOrder() != NobleArmy.OrderType.ATTACK) continue;
            String targetZone = a.getPendingTargetZoneId();
            if (targetZone != null && house.getZoneIds().contains(targetZone)) return true;
        }
        return false;
    }

    private int minDeposit(NobleHouse house) {
        return Math.max(BankParams.BANK_MIN_DEPOSIT_FLAT_GOLD,
                (int) (house.getGold() * BankParams.BANK_MIN_DEPOSIT_FRACTION_OF_GOLD));
    }

    public boolean deposit(NobleHouse house, int amount, List<String> log) {
        if (bankHouse == null || amount <= 0) return false;
        if (isHouseUnderAttack(house)) {
            Debug.log("bank", "deposit-blocked", house.getName() + " is under attack, cannot deposit");
            return false;
        }
        if (amount > house.getGold()) return false;

        BankAccount acc = getOrCreateAccount(house.getId());
        int resultingDeposit = acc.getDeposit() + amount;
        int min = minDeposit(house);
        if (resultingDeposit < min) {
            Debug.log("bank", "deposit-rejected", house.getName()
                    + " deposit would leave balance below minimum (" + min + ")");
            return false;
        }
        house.addGold(-amount);
        bankHouse.addGold(amount);
        acc.addDeposit(amount);
        if (log != null) log.add(house.getName() + " deposits " + amount + " gold with the Bank.");
        Debug.log("bank", "deposit", house.getName() + " deposited " + amount
                + " (balance=" + acc.getDeposit() + ")");
        return true;
    }

    public boolean withdraw(NobleHouse house, int amount, List<String> log) {
        if (bankHouse == null || amount <= 0) return false;

        BankAccount acc = accounts.get(house.getId());
        if (acc == null || acc.getDeposit() < amount) return false;

        int resultingDeposit = acc.getDeposit() - amount;
        int min = minDeposit(house);
        if (resultingDeposit > 0 && resultingDeposit < min) {
            amount = acc.getDeposit();
        }

        if (withdrawDelayTurnsRemaining > 0) {
            if (log != null) log.add("The Bank delays " + house.getName()
                    + "'s withdrawal — reserves are thin.");
            return false;
        }
        if (bankHouse.getGold() < amount) {
            if (log != null) log.add("The Bank cannot honor " + house.getName()
                    + "'s withdrawal right now — insufficient reserves.");
            return false;
        }
        int projectedGold = bankHouse.getGold() - amount;
        if (projectedGold < lastEndTurnGold * BankParams.BANK_WITHDRAW_RESERVE_FRACTION) {
            withdrawDelayTurnsRemaining = BankParams.BANK_WITHDRAW_DELAY_TURNS;
            if (log != null) log.add("The Bank asks " + house.getName()
                    + " for one turn to settle reserves before releasing the withdrawal.");
            return false;
        }

        acc.removeDeposit(amount);
        bankHouse.addGold(-amount);
        house.addGold(amount);
        if (log != null) log.add(house.getName() + " withdraws " + amount + " gold from the Bank.");
        Debug.log("bank", "withdraw", house.getName() + " withdrew " + amount
                + " (balance=" + acc.getDeposit() + ")");
        return true;
    }

    // ─── Loans ───────────────────────────────────────────────────────────

    public boolean  hasActiveLoan(String houseId) { return loans.containsKey(houseId); }
    public BankLoan getLoan(String houseId)       { return loans.get(houseId); }
    public Map<String, BankLoan> getLoansRaw()    { return loans; }

    public double getInterestRateFor(NobleHouse house, boolean hasCollateral) {
        BankAccount acc = getOrCreateAccount(house.getId());
        double creditAdjustment = (BankParams.BANK_CREDIT_RATING_BASE - acc.getCreditRating()) * 0.002;
        double rate = BankParams.BANK_BASE_LOAN_INTEREST_RATE + creditAdjustment;
        if (hasCollateral) rate -= BankParams.BANK_LOAN_COLLATERAL_RATE_DISCOUNT;
        if (protectorHouseIds.contains(house.getId())) rate -= BankParams.BANK_PROTECTOR_LOAN_RATE_DISCOUNT;
        return Math.max(0.01, rate);
    }

    public int getMaxLoanAmount(NobleHouse house) {
        if (bankRobbers.contains(house.getId())) return 0;
        BankAccount acc = getOrCreateAccount(house.getId());
        return (int) (acc.getCreditRating() * BankParams.BANK_MAX_LOAN_PER_CREDIT_POINT);
    }

    public boolean requestLoan(NobleHouse house, int amount, String collateralZoneId, List<String> log) {
        if (bankHouse == null || amount <= 0) return false;
        if (loans.containsKey(house.getId())) return false;
        if (bankRobbers.contains(house.getId())) return false;
        if (amount > getMaxLoanAmount(house)) return false;
        if (bankHouse.getGold() < amount) return false;

        double rate = getInterestRateFor(house, collateralZoneId != null);
        BankLoan loan = new BankLoan(house.getId(), amount, rate,
                BankParams.BANK_LOAN_INSTALLMENTS_DEFAULT, collateralZoneId);
        loans.put(house.getId(), loan);
        bankHouse.addGold(-amount);
        house.addGold(amount);
        if (log != null) log.add(house.getName() + " borrows " + amount
                + " gold from the Bank at " + String.format("%.1f%%", rate * 100) + " interest.");
        Debug.log("bank", "loan", house.getName() + " borrowed " + amount + " @ " + rate);
        return true;
    }

    private void collectInstallments(List<NobleHouse> allHouses, List<String> log) {
        for (NobleHouse house : new ArrayList<>(allHouses)) {
            BankLoan loan = loans.get(house.getId());
            if (loan == null) continue;
            if (loan.isPaidOff()) { loans.remove(house.getId()); continue; }
            int due = loan.getNextInstallmentDue();
            if (due <= 0) { loans.remove(house.getId()); continue; }

            // First try to draw from deposit (gold already held by Bank)
            int fromDeposit = 0;
            BankAccount acc = accounts.get(house.getId());
            if (acc != null && acc.getDeposit() > 0) {
                fromDeposit = Math.min(due, acc.getDeposit());
                acc.removeDeposit(fromDeposit);
                // No gold transfer needed: deposit is a liability of the Bank;
                // reducing it effectively repays the loan with funds already in the Bank.
                loan.applyPayment(fromDeposit);
                Debug.log("bank", "installment-from-deposit", house.getName() + " paid " + fromDeposit + " gold from deposit (owing " + loan.getPrincipalRemaining() + ")");
            }

            int remaining = due - fromDeposit;
            if (remaining <= 0) {
                if (loan.isPaidOff()) {
                    loans.remove(house.getId());
                    getOrCreateAccount(house.getId()).adjustCredit(BankParams.BANK_CREDIT_BONUS_PER_REPAYMENT);
                    if (log != null) log.add(house.getName() + " repays its loan to the Bank in full.");
                    Debug.log("bank", "loan-repaid", house.getName() + " fully repaid loan");
                }
                continue;
            }

            // Fallback: use house gold
            if (house.getGold() >= remaining) {
                house.addGold(-remaining);
                if (bankHouse != null) bankHouse.addGold(remaining);
                loan.applyPayment(remaining);
                Debug.log("bank", "installment-paid", house.getName() + " paid " + remaining + " gold installment (owing " + loan.getPrincipalRemaining() + ")");
                if (loan.isPaidOff()) {
                    loans.remove(house.getId());
                    getOrCreateAccount(house.getId()).adjustCredit(BankParams.BANK_CREDIT_BONUS_PER_REPAYMENT);
                    if (log != null) log.add(house.getName() + " repays its loan to the Bank in full.");
                    Debug.log("bank", "loan-repaid", house.getName() + " fully repaid loan");
                }
            } else {
                defaultLoan(house, loan, log);
            }
        }
    }

    private void defaultLoan(NobleHouse house, BankLoan loan, List<String> log) {
        loan.markDefaulted();
        getOrCreateAccount(house.getId()).adjustCredit(-BankParams.BANK_CREDIT_PENALTY_PER_DEFAULT);
        loans.remove(house.getId());
        if (loan.getCollateralZoneId() != null && house.getZoneIds().contains(loan.getCollateralZoneId())) {
            if (log != null) log.add("The Bank seizes income from " + loan.getCollateralZoneId()
                    + " after " + house.getName() + " defaults on its loan.");
        } else if (log != null) {
            log.add(house.getName() + " defaults on its loan to the Bank. Credit suffers.");
        }
        Debug.log("bank", "default", house.getName() + " defaulted on loan");
    }

    public void callInLoan(NobleHouse house, List<String> log) {
        BankLoan loan = loans.get(house.getId());
        if (loan == null) return;
        int owed = loan.getFullRepaymentAmount();
        if (house.getGold() >= owed) {
            house.addGold(-owed);
            if (bankHouse != null) bankHouse.addGold(owed);
            loans.remove(house.getId());
            getOrCreateAccount(house.getId()).adjustCredit(BankParams.BANK_CREDIT_BONUS_PER_REPAYMENT);
            if (log != null) log.add("The Bank calls in its loan. " + house.getName()
                    + " repays " + owed + " gold in full.");
        } else {
            defaultLoan(house, loan, log);
        }
    }

    private void maybeCallLoansForCash(List<NobleHouse> allHouses, List<String> log) {
        if (bankHouse == null) return;
        int totalDeposits = 0;
        for (BankAccount a : accounts.values()) totalDeposits += a.getDeposit();
        if (totalDeposits <= 0) return;
        if (bankHouse.getGold() >= totalDeposits * BankParams.BANK_LOW_RESERVE_CALL_THRESHOLD) return;

        for (NobleHouse house : allHouses) {
            BankLoan loan = loans.get(house.getId());
            if (loan == null) continue;
            BankAccount acc = getOrCreateAccount(house.getId());
            if (acc.getCreditRating() < BankParams.BANK_CREDIT_RATING_BASE) {
                callInLoan(house, log);
                break;
            }
        }
    }

    // ─── Interest accrual ────────────────────────────────────────────────

    private void accrueInterest(List<NobleHouse> allHouses, List<String> log) {
        if (bankHouse == null) return;
        double rate = BankParams.BANK_BASE_INTEREST_RATE_PER_TURN;
        if (isBankThreatened(allHouses)) rate += BankParams.BANK_THREATENED_INTEREST_BONUS;

        int totalAccrued = 0;
        for (BankAccount acc : accounts.values()) {
            if (acc.getDeposit() <= 0) continue;
            int interest = (int) Math.ceil(acc.getDeposit() * rate);
            acc.addDeposit(interest);
            totalAccrued += interest;
        }
        if (totalAccrued > 0) {
            // Interest is a book entry only; gold does not leave the vault until withdrawal.
            Debug.log("bank", "interest", "Accrued " + totalAccrued + " gold in deposit interest @ " + rate);
        }
    }

    // ─── Threat / stakeholder defense ───────────────────────────────────

    public boolean isBankThreatened(List<NobleHouse> allHouses) {
        if (bankHouse == null) return false;
        int bankPower = bankHouse.getTotalGarrisonSize() + mercenaryManpower;
        int strongestHostile = 0;
        for (NobleHouse other : allHouses) {
            if (other == bankHouse || other.isEliminated()) continue;
            Relationship rel = relationships.get(bankHouse.getId(), other.getId());
            if (rel != Relationship.HOSTILE && rel != Relationship.RIVAL) continue;
            int power = NobleAIPower.estimateAttackPower(other, armyManager);
            if (power > strongestHostile) strongestHostile = power;
        }
        return strongestHostile > bankPower * (BankParams.BANK_THREAT_RATIO_TRIGGER - 0.2);
    }

    /**
     * Highest single threat the Bank faces right now — the worst of any
     * hostile/rival noble (plus their allies) or any barbarian force bearing
     * down on the Bank's zone. Not a sum across threats — the single largest.
     */
    public int getStrongestThreat(List<NobleHouse> allHouses,
                                   RelationshipManager relationships,
                                   NobleArmyManager armyMgr,
                                   ZoneManager zoneMgr,
                                   BarbArmyManager barbMgr) {
        if (bankHouse == null) return 0;
        int strongest = 0;

        for (NobleHouse house : allHouses) {
            if (house == bankHouse || house.isEliminated()) continue;
            Relationship rel = relationships.get(bankHouse.getId(), house.getId());
            if (rel != Relationship.HOSTILE && rel != Relationship.RIVAL) continue;
            int threat = NobleAIPower.estimateAttackPower(house, armyMgr);
            for (NobleHouse ally : allHouses) {
                if (ally == house || ally == bankHouse || ally.isEliminated()) continue;
                if (relationships.get(house.getId(), ally.getId()) == Relationship.ALLIED) {
                    threat += NobleAIPower.estimateAttackPower(ally, armyMgr);
                }
            }
            if (threat > strongest) strongest = threat;
        }

        if (barbMgr != null) {
            int barbThreat = 0;
            BarbArmy warboss = barbMgr.getWarboss();
            if (warboss != null && BankParams.BANK_ZONE_ID.equals(warboss.getNextZoneId())) {
                barbThreat += warboss.getSize();
            }
            Zone bankZone = zoneMgr.getZone(BankParams.BANK_ZONE_ID);
            if (bankZone != null) {
                for (String adjId : bankZone.getAdjacentIds()) {
                    for (BarbArmy a : barbMgr.getArmiesInZone(adjId)) {
                        if (a.isAlive() && !a.isGarrison() && (a.isRaider() || a.isRavager())) {
                            barbThreat += a.getSize();
                        }
                    }
                }
            }
            if (barbThreat > strongest) strongest = barbThreat;
        }

        return strongest;
    }

    /**
     * Defence the Bank can count on right now — garrison, its own field
     * armies, plus the estimated strength of every stakeholder and protector.
     * Does NOT include the mercenary pool (BankAI factors that separately).
     */
    public int getTotalDefence(List<NobleHouse> allHouses, NobleArmyManager armyMgr) {
        if (bankHouse == null) return 0;
        int defence = bankHouse.getTotalGarrisonSize();

        for (NobleArmy a : armyMgr.getArmiesForHouse(bankHouse.getId())) {
            if (a.isAlive()) defence += a.getSize();
        }

        for (String stakeholderId : getStakeholderIds()) {
            NobleHouse h = findHouse(stakeholderId, allHouses);
            if (h != null && !h.isEliminated()) {
                defence += NobleAIPower.estimateAttackPower(h, armyMgr);
            }
        }

        for (String protectorId : protectorHouseIds) {
            NobleHouse h = findHouse(protectorId, allHouses);
            if (h != null && !h.isEliminated()) {
                defence += NobleAIPower.estimateAttackPower(h, armyMgr);
            }
        }

        return defence;
    }

    /** The Bank's own money, separate from what it owes depositors. */
    public int getOwnGold() {
        if (bankHouse == null) return 0;
        return bankHouse.getGold() - getTotalDeposits();
    }

    /** Own gold plus a slice of depositor funds — only tapped when threatened. */
    public int getEmergencyFund() {
        if (bankHouse == null) return 0;
        return getOwnGold() + (int) (BankParams.BANK_EMERGENCY_FUND_DEPOSIT_FRACTION * getTotalDeposits());
    }

    private int getTotalDeposits() {
        int total = 0;
        for (BankAccount acc : accounts.values()) total += acc.getDeposit();
        return total;
    }

    public void    markProtector(String houseId)  { protectorHouseIds.add(houseId); }
    public boolean isProtector(String houseId)    { return protectorHouseIds.contains(houseId); }

    /**
     * Offers protector terms to a house. Refuses if the house is already a
     * stakeholder (a depositor turning protector would be an odd conflict of
     * interest). Rewards the new protector with prestige.
     */
    public boolean markProtector(NobleHouse house) {
        if (house == null) return false;
        BankAccount acc = accounts.get(house.getId());
        if (acc != null && acc.getDeposit() > 0) return false;
        if (protectorHouseIds.add(house.getId())) {
            house.addPrestige(BankParams.BANK_PROTECTOR_PRESTIGE_BONUS);
        }
        return true;
    }

    /**
     * Called when a house attacks the Bank. Each current stakeholder chooses
     * to defend (tracked in {@link #getLastBattleDefenders()} for the caller
     * to fold combat power into the battle) or abandon the Bank. Abandoned
     * deposits fund emergency mercenary musters. If the aggressor was a
     * protector, that pact is voided, its loan called in, and the recovered
     * gold likewise funds emergency mercenaries. The aggressor is
     * permanently branded a Bank-Robber.
     */
    public List<String> onAttackAgainstBank(NobleHouse aggressor, NobleHouse bank,
                                             List<NobleHouse> allHouses, List<String> log) {
        if (log == null) log = new ArrayList<>();
        lastBattleDefenders.clear();
        lastEmergencyMercenaryContribution = 0;
        int aggressorPower = NobleAIPower.estimateAttackPower(aggressor, armyManager);

        for (String stakeholderId : new ArrayList<>(getStakeholderIds())) {
            if (stakeholderId.equals(aggressor.getId())) {
                getOrCreateAccount(stakeholderId).clearStakeholderStatus();
                log.add(aggressor.getName() + " forfeits its entire deposit by attacking the Bank.");
                Debug.log("bank", "stakeholder-forfeit", aggressor.getName() + " forfeits deposit by attacking Bank");
                continue;
            }
            NobleHouse stakeholder = findHouse(stakeholderId, allHouses);
            if (stakeholder == null || stakeholder.isEliminated()) continue;

            boolean willDefend = decideStakeholderDefense(stakeholder, bank, aggressorPower);
            if (willDefend) {
                lastBattleDefenders.add(stakeholderId);
                getOrCreateAccount(stakeholderId).adjustCredit(BankParams.BANK_DEFEND_GOODWILL_CREDIT_BONUS);
                log.add(stakeholder.getName() + " rushes to defend the Bank against " + aggressor.getName() + ".");
                Debug.log("bank", "stakeholder-defend", stakeholder.getName() + " defends Bank vs " + aggressor.getName());
            } else {
                BankAccount acc = getOrCreateAccount(stakeholderId);
                int lost = (int) (acc.getDeposit() * BankParams.BANK_ABANDON_DEPOSIT_LOSS_FRACTION);
                acc.removeDeposit(lost);
                stakeholder.addPrestige(BankParams.BANK_ABANDON_PRESTIGE_PENALTY);
                log.add(stakeholder.getName() + " abandons the Bank, losing " + lost + " gold in deposits.");
                Debug.log("bank", "stakeholder-abandon", stakeholder.getName() + " abandons Bank, lost " + lost + " gold");
                emergencyRecruitMercenaries(lost, armyManager, log);
            }
        }

        if (protectorHouseIds.remove(aggressor.getId())) {
            aggressor.addPrestige(BankParams.BANK_ABANDON_PRESTIGE_PENALTY);
            log.add(aggressor.getName() + " betrays its protector pact with the Bank.");
            Debug.log("bank", "protector-betrayal", aggressor.getName() + " betrayed protector pact");
            BankLoan aggressorLoan = loans.get(aggressor.getId());
            if (aggressorLoan != null) {
                int owed = aggressorLoan.getFullRepaymentAmount();
                callInLoan(aggressor, log);
                if (!loans.containsKey(aggressor.getId())) {
                    emergencyRecruitMercenaries(owed, armyManager, log);
                }
            }
        }

        markBankRobber(aggressor, log);
        return log;
    }

    private boolean decideStakeholderDefense(NobleHouse stakeholder, NobleHouse bank, int aggressorPower) {
        int myPower   = NobleAIPower.exactPotentialFieldArmy(stakeholder, armyManager)
                + stakeholder.getTotalGarrisonSize();
        int bankPower = bank.getTotalGarrisonSize() + mercenaryManpower;
        return (myPower + bankPower) >= aggressorPower * 0.8;
    }

    public void markBankRobber(NobleHouse aggressor, List<String> log) {
        if (bankRobbers.add(aggressor.getId())) {
            getOrCreateAccount(aggressor.getId()).capCreditAt(BankParams.BANK_ROBBER_CREDIT_CAP);
            for (String stakeholderId : getStakeholderIds()) {
                relationships.set(aggressor.getId(), stakeholderId, Relationship.RIVAL);
            }
            if (log != null) log.add(aggressor.getName()
                    + " is branded a Bank-Robber — trade penalties, diplomatic damage, and near-total loss of future credit.");
            Debug.log("bank", "bank-robber", aggressor.getName() + " marked as Bank-Robber");
        }
    }

    public List<String> onBarbarianAttackOnBank(List<NobleHouse> allHouses, List<String> log) {
        if (log == null) log = new ArrayList<>();
        for (String stakeholderId : getStakeholderIds()) {
            NobleHouse stakeholder = findHouse(stakeholderId, allHouses);
            if (stakeholder == null || stakeholder.isEliminated()) continue;
            log.add(stakeholder.getName() + " rushes to help defend the Bank from barbarians.");
        }
        return log;
    }

    // ─── Money stealing on conquest ─────────────────────────────────────

    public void applyConquestGoldTheft(NobleHouse victim, NobleHouse conqueror,
                                        boolean wasCapital, List<String> log) {
        double fraction = wasCapital
                ? BankParams.CONQUEST_STEAL_FRACTION_CAPITAL
                : BankParams.CONQUEST_STEAL_FRACTION_NORMAL;
        int stolen = (int) (victim.getGold() * fraction);
        if (stolen <= 0) return;
        victim.addGold(-stolen);
        conqueror.addGold(stolen);
        if (log != null) log.add(conqueror.getName() + " plunders " + stolen
                + " undeposited gold from " + victim.getName() + ".");
        Debug.log("bank", "conquest-theft", conqueror.getName() + " stole " + stolen
                + " from " + victim.getName() + " (capital=" + wasCapital + ")");
    }

    // ─── Mercenary manpower ──────────────────────────────────────────────

    public void setDesiredMercenaries(int v) { desiredMercenaries = Math.max(0, v); }
    public int  getDesiredMercenaries()      { return desiredMercenaries; }

    /** Grows the pool toward {@link #desiredMercenaries}, never past it. No max pool size. */
    public void growMercenaryManpower(List<NobleHouse> allHouses, int playerManpower) {
        int totalManpower = playerManpower;
        for (NobleHouse h : allHouses) totalManpower += h.getNobleManpower();
        int maxGrowthThisTurn = (int) Math.ceil(totalManpower * BankParams.BANK_MERC_MANPOWER_GROWTH_FRACTION_OF_TOTAL);
        int room = desiredMercenaries - mercenaryManpower;
        if (room <= 0) return;
        mercenaryManpower += Math.min(maxGrowthThisTurn, room);
    }

    public void setMercenaryManpower(int v)      { mercenaryManpower = Math.max(0, v); }
    public void decreaseMercenaryManpower(int v) { mercenaryManpower = Math.max(0, mercenaryManpower - v); }

    public void payMercenaryUpkeep(List<String> log) {
        if (bankHouse == null || mercenaryManpower <= 0) return;
        int cost = (int) Math.ceil(mercenaryManpower * BankParams.BANK_MERC_UPKEEP_GOLD_PER_MANPOWER);
        if (bankHouse.getGold() >= cost) {
            bankHouse.addGold(-cost);
            Debug.log("bank", "mercenary-upkeep", "Mercenary upkeep paid: " + cost + " gold for " + mercenaryManpower + " manpower");
            if (getOwnGold() < 0) {
                Debug.log("bank", "mercenary-upkeep-depositor-funds",
                        "Mercenary upkeep dipped into depositor funds — ownGold=" + getOwnGold());
            }
            return;
        }
        int affordable = (int) (bankHouse.getGold() / BankParams.BANK_MERC_UPKEEP_GOLD_PER_MANPOWER);
        int oldCount = mercenaryManpower;
        mercenaryManpower = Math.max(0, affordable);
        int reducedCost = (int) Math.ceil(mercenaryManpower * BankParams.BANK_MERC_UPKEEP_GOLD_PER_MANPOWER);
        bankHouse.addGold(-Math.min(reducedCost, bankHouse.getGold()));
        if (log != null) log.add("The Bank can no longer fully fund its mercenaries and disbands some.");
        Debug.log("bank", "mercenary-disband", "Mercenary manpower reduced from " + oldCount + " to " + mercenaryManpower + " due to funding shortage");
        if (getOwnGold() < 0) {
            Debug.log("bank", "mercenary-upkeep-depositor-funds",
                    "Mercenary upkeep dipped into depositor funds — ownGold=" + getOwnGold());
        }
    }

    /** Converts pooled mercenary manpower into an actual defensive army at double recruit cost. */
    public NobleArmy recruitMercenaryArmy(int size, NobleArmyManager armyManager) {
        if (bankHouse == null || size <= 0 || size > mercenaryManpower) return null;
        String zoneId = bankHouse.getCapitalZoneId();
        if (zoneId == null) return null;
        int cost = (int) Math.ceil(size * NobleHouseParams.NOBLE_UPKEEP_COST_PER_SOLDIER
                * BankParams.BANK_MERC_RECRUIT_COST_MULTIPLIER);
        if (bankHouse.getGold() < cost) return null;

        bankHouse.addGold(-cost);
        mercenaryManpower -= size;

        NobleArmy army = new NobleArmy("mercenary_army_" + System.nanoTime(),
                bankHouse.getId(), size, zoneId);
        army.setSkipNextUpkeep(true);
        army.setMercenary(true);
        armyManager.addRestoredArmy(army);
        return army;
    }

    /**
     * Converts as many pooled mercenaries into a real defensive NobleArmy as
     * {@code availableGold} allows (at the same 2x recruit-cost rate as
     * normal mercenary recruitment), for use in an ongoing attack on the
     * Bank. Accumulates into {@link #getLastEmergencyMercenaryContribution()}
     * so the caller can fold the size into the current battle.
     */
    public NobleArmy emergencyRecruitMercenaries(int availableGold, NobleArmyManager armyManager, List<String> log) {
        if (bankHouse == null || availableGold <= 0 || mercenaryManpower <= 0) return null;
        String zoneId = bankHouse.getCapitalZoneId();
        if (zoneId == null) return null;

        double costPerMerc = NobleHouseParams.NOBLE_UPKEEP_COST_PER_SOLDIER * BankParams.BANK_MERC_RECRUIT_COST_MULTIPLIER;
        int maxByGold = (int) (availableGold / costPerMerc);
        int size = Math.min(mercenaryManpower, maxByGold);
        if (size <= 0) return null;

        int cost = Math.min((int) Math.ceil(size * costPerMerc), bankHouse.getGold());
        if (cost <= 0) return null;

        bankHouse.addGold(-cost);
        mercenaryManpower -= size;

        NobleArmy army = new NobleArmy("mercenary_army_" + System.nanoTime(),
                bankHouse.getId(), size, zoneId);
        army.setSkipNextUpkeep(true);
        army.setMercenary(true);
        armyManager.addRestoredArmy(army);
        lastEmergencyMercenaryContribution += size;

        if (log != null) log.add("The Bank rushes " + size + " emergency mercenaries to its defense.");
        Debug.log("bank", "emergency-merc", "Recruited " + size + " emergency mercenaries for " + cost + " gold");
        return army;
    }

    public int getLastEmergencyMercenaryContribution() { return lastEmergencyMercenaryContribution; }

    // ─── Dissolution ─────────────────────────────────────────────────────

    /**
     * Called when the Bank's capital falls. The conqueror does NOT receive
     * the capital — the Bank simply ceases to exist. Depositors lose
     * everything, borrowers owe nothing, and the conqueror only gains a cut
     * of the vault. Any other zones the Bank held are redistributed.
     */
    public void dissolve(NobleHouse conqueror, List<NobleHouse> allHouses,
                         ClaimManager claimManager, ZoneManager zoneManager, List<String> log) {
        if (bankHouse == null) return;

        int seized = (int) (bankHouse.getGold() * BankParams.BANK_DISSOLVE_CONQUEROR_GOLD_FRACTION);
        if (seized > 0) {
            bankHouse.addGold(-seized);
            conqueror.addGold(seized);
            log.add(conqueror.getName() + " seizes " + seized + " gold from the Bank's vault as it collapses.");
        }

        if (!accounts.isEmpty()) {
            log.add("Every depositor's gold with the Bank is lost as it collapses.");
            accounts.clear();
        }
        if (!loans.isEmpty()) {
            log.add("Every outstanding debt to the Bank is wiped clean.");
            loans.clear();
        }

        Random rng = new Random();
        String capitalZoneId = bankHouse.getCapitalZoneId();
        List<String> otherZones = new ArrayList<>(bankHouse.getZoneIds());
        otherZones.remove(capitalZoneId);

        for (String zoneId : otherZones) {
            bankHouse.removeZone(zoneId);

            List<NobleHouse> claimants = new ArrayList<>();
            for (NobleHouse h : allHouses) {
                if (h == bankHouse || h.isEliminated()) continue;
                if (claimManager.hasClaim(h.getId(), zoneId)) claimants.add(h);
            }
            if (!claimants.isEmpty()) {
                NobleHouse winner = claimants.get(rng.nextInt(claimants.size()));
                winner.addZone(zoneId);
                log.add(winner.getName() + " claims the former Bank zone " + zoneId + ".");
                continue;
            }

            List<NobleHouse> adjacentOwners = new ArrayList<>();
            Zone zone = zoneManager.getZone(zoneId);
            if (zone != null) {
                for (String adjId : zone.getAdjacentIds()) {
                    for (NobleHouse h : allHouses) {
                        if (h == bankHouse || h.isEliminated()) continue;
                        if (h.getZoneIds().contains(adjId) && !adjacentOwners.contains(h)) adjacentOwners.add(h);
                    }
                }
            }
            if (!adjacentOwners.isEmpty()) {
                NobleHouse winner = adjacentOwners.get(rng.nextInt(adjacentOwners.size()));
                winner.addZone(zoneId);
                log.add(winner.getName() + " annexes the unclaimed former Bank zone " + zoneId + ".");
            } else {
                log.add(zoneId + " is left ungoverned after the Bank's collapse.");
            }
        }

        if (capitalZoneId != null) bankHouse.removeZone(capitalZoneId);

        bankRobbers.clear();
        protectorHouseIds.clear();
        lastBattleDefenders.clear();
        mercenaryManpower = 0;

        log.add("The Frostpeak Bank collapses and is no more.");
        Debug.log("bank", "dissolve", "Bank dissolved; conqueror=" + conqueror.getName());
    }

    // ─── Turn processing ──────────────────────────────────────────────────

    public List<String> processTurn(List<NobleHouse> allHouses, int playerManpower,
                                     ResourcePool playerResources) {
        List<String> log = new ArrayList<>();
        if (bankHouse == null) return log;
        if (withdrawDelayTurnsRemaining > 0) withdrawDelayTurnsRemaining--;

        accrueInterest(allHouses, log);
        collectInstallments(allHouses, log);
        collectPlayerInstallment(playerResources, log);
        maybeCallLoansForCash(allHouses, log);
        growMercenaryManpower(allHouses, playerManpower);
        payMercenaryUpkeep(log);

        lastEndTurnGold = bankHouse.getGold();
        return log;
    }

    private NobleHouse findHouse(String id, List<NobleHouse> all) {
        for (NobleHouse h : all) if (h.getId().equals(id)) return h;
        return null;
    }

    public void reset() {
        accounts.clear();
        loans.clear();
        bankRobbers.clear();
        protectorHouseIds.clear();
        lastBattleDefenders.clear();
        mercenaryManpower = 0;
        withdrawDelayTurnsRemaining = 0;
        desiredMercenaries = Integer.MAX_VALUE;
        lastEmergencyMercenaryContribution = 0;
        lastEndTurnGold = bankHouse != null ? bankHouse.getGold() : 0;
    }

    // ─── Save/load raw access ───────────────────────────────────────────

    public Map<String, BankAccount> getAccountsRaw()  { return accounts; }
    public Set<String> getBankRobbersRaw()             { return bankRobbers; }
    public Set<String> getProtectorsRaw()              { return protectorHouseIds; }
    public int  getWithdrawDelayTurnsRemaining()       { return withdrawDelayTurnsRemaining; }
    public void setWithdrawDelayTurnsRemaining(int v)  { withdrawDelayTurnsRemaining = Math.max(0, v); }
    public int  getLastEndTurnGold()                   { return lastEndTurnGold; }
    public void setLastEndTurnGoldRaw(int v)           { lastEndTurnGold = v; }

    // ─── Player-specific banking operations ──────────────────────────────

    public boolean depositPlayer(ResourcePool resources, int amount, List<String> log) {
        if (bankHouse == null || amount <= 0 || amount > resources.getMoney()) return false;
        BankAccount acc = getOrCreateAccount(PLAYER_HOUSE_ID);
        int resulting = acc.getDeposit() + amount;
        int min = Math.max(BankParams.BANK_MIN_DEPOSIT_FLAT_GOLD,
                (int) (resources.getMoney() * BankParams.BANK_MIN_DEPOSIT_FRACTION_OF_GOLD));
        if (resulting < min) {
            if (log != null) log.add("Minimum deposit is " + min + " gold — deposit more or build up your treasury first.");
            return false;
        }
        resources.spendMoney(amount);
        bankHouse.addGold(amount);
        acc.addDeposit(amount);
        if (log != null) log.add("You deposit " + amount + " gold with the Bank.");
        Debug.log("bank", "player-deposit", "Player deposited " + amount);
        return true;
    }

    public boolean withdrawPlayer(ResourcePool resources, int amount, List<String> log) {
        if (bankHouse == null || amount <= 0) return false;
        BankAccount acc = accounts.get(PLAYER_HOUSE_ID);
        if (acc == null || acc.getDeposit() < amount) {
            if (log != null) log.add("Insufficient deposit balance.");
            return false;
        }
        // If partial withdrawal would leave balance between 0 and new minimum, withdraw all.
        int resulting = acc.getDeposit() - amount;
        int minAfter  = Math.max(BankParams.BANK_MIN_DEPOSIT_FLAT_GOLD,
                (int) ((resources.getMoney() + amount) * BankParams.BANK_MIN_DEPOSIT_FRACTION_OF_GOLD));
        if (resulting > 0 && resulting < minAfter) {
            amount = acc.getDeposit();
            if (log != null) log.add("Adjusted to withdraw full balance — partial amount would fall below the minimum.");
        }
        if (withdrawDelayTurnsRemaining > 0) {
            if (log != null) log.add("The Bank delays your withdrawal — reserves are thin. Try next turn.");
            return false;
        }
        if (bankHouse.getGold() < amount) {
            if (log != null) log.add("The Bank currently lacks reserves to honor this withdrawal.");
            return false;
        }
        if (bankHouse.getGold() - amount < lastEndTurnGold * BankParams.BANK_WITHDRAW_RESERVE_FRACTION) {
            withdrawDelayTurnsRemaining = BankParams.BANK_WITHDRAW_DELAY_TURNS;
            if (log != null) log.add("The Bank asks for one turn to settle reserves before releasing your withdrawal.");
            return false;
        }
        acc.removeDeposit(amount);
        bankHouse.addGold(-amount);
        resources.setMoney(resources.getMoney() + amount);
        if (log != null) log.add("You withdraw " + amount + " gold from the Bank.");
        Debug.log("bank", "player-withdraw", "Player withdrew " + amount);
        return true;
    }

    public boolean requestLoanPlayer(ResourcePool resources, int amount, List<String> log) {
        if (bankHouse == null || amount <= 0) return false;
        if (loans.containsKey(PLAYER_HOUSE_ID)) {
            if (log != null) log.add("You already have an active loan. Repay it before borrowing again.");
            return false;
        }
        if (bankRobbers.contains(PLAYER_HOUSE_ID)) {
            if (log != null) log.add("The Bank refuses to deal with you.");
            return false;
        }
        BankAccount acc    = getOrCreateAccount(PLAYER_HOUSE_ID);
        int         maxLoan = getMaxLoanAmountPlayer(acc);
        if (amount > maxLoan) {
            if (log != null) log.add("Maximum loan available: " + maxLoan + " gold (credit rating: " + acc.getCreditRating() + ").");
            return false;
        }
        if (bankHouse.getGold() < amount) {
            if (log != null) log.add("The Bank lacks reserves to extend this loan right now.");
            return false;
        }
        double   rate = getInterestRateForPlayer(acc);
        BankLoan loan = new BankLoan(PLAYER_HOUSE_ID, amount, rate,
                BankParams.BANK_LOAN_INSTALLMENTS_DEFAULT, null);
        loans.put(PLAYER_HOUSE_ID, loan);
        bankHouse.addGold(-amount);
        resources.setMoney(resources.getMoney() + amount);
        if (log != null) log.add("You borrow " + amount + " gold at "
                + String.format("%.1f%%", rate * 100) + " interest. Total owed: "
                + loan.getFullRepaymentAmount() + " gold over "
                + BankParams.BANK_LOAN_INSTALLMENTS_DEFAULT + " installments.");
        Debug.log("bank", "player-loan", "Player borrowed " + amount + " @ " + rate);
        return true;
    }

    public boolean payInstallmentPlayer(ResourcePool resources, List<String> log) {
        BankLoan loan = loans.get(PLAYER_HOUSE_ID);
        if (loan == null || loan.isPaidOff()) return false;
        int due = loan.getNextInstallmentDue();
        if (resources.getMoney() < due) {
            if (log != null) log.add("Need " + due + " gold for this installment but you only have " + resources.getMoney() + ".");
            return false;
        }
        resources.spendMoney(due);
        if (bankHouse != null) bankHouse.addGold(due);
        loan.applyPayment(due);
        if (loan.isPaidOff()) {
            loans.remove(PLAYER_HOUSE_ID);
            getOrCreateAccount(PLAYER_HOUSE_ID).adjustCredit(BankParams.BANK_CREDIT_BONUS_PER_REPAYMENT);
            if (log != null) log.add("Loan fully repaid! Credit rating +" + BankParams.BANK_CREDIT_BONUS_PER_REPAYMENT + ".");
        } else if (log != null) {
            log.add("Paid " + due + " gold. " + loan.getInstallmentsRemaining()
                    + " installments remain (" + loan.getFullRepaymentAmount() + " gold total).");
        }
        return true;
    }

    public boolean repayLoanFullPlayer(ResourcePool resources, List<String> log) {
        BankLoan loan = loans.get(PLAYER_HOUSE_ID);
        if (loan == null || loan.isPaidOff()) return false;
        int owed = loan.getFullRepaymentAmount();
        if (resources.getMoney() < owed) {
            if (log != null) log.add("Need " + owed + " gold to repay in full but you only have " + resources.getMoney() + ".");
            return false;
        }
        resources.spendMoney(owed);
        if (bankHouse != null) bankHouse.addGold(owed);
        loans.remove(PLAYER_HOUSE_ID);
        getOrCreateAccount(PLAYER_HOUSE_ID).adjustCredit(BankParams.BANK_CREDIT_BONUS_PER_REPAYMENT);
        if (log != null) log.add("Loan of " + owed + " gold fully repaid. Credit rating +"
                + BankParams.BANK_CREDIT_BONUS_PER_REPAYMENT + ".");
        return true;
    }

    public int getMaxLoanAmountPlayer(BankAccount acc) {
        if (bankRobbers.contains(PLAYER_HOUSE_ID)) return 0;
        return (int) (acc.getCreditRating() * BankParams.BANK_MAX_LOAN_PER_CREDIT_POINT);
    }

    public double getInterestRateForPlayer(BankAccount acc) {
        double creditAdj = (BankParams.BANK_CREDIT_RATING_BASE - acc.getCreditRating()) * 0.002;
        double rate = BankParams.BANK_BASE_LOAN_INTEREST_RATE + creditAdj;
        if (protectorHouseIds.contains(PLAYER_HOUSE_ID)) rate -= BankParams.BANK_PROTECTOR_LOAN_RATE_DISCOUNT;
        return Math.max(0.01, rate);
    }

    private void collectPlayerInstallment(ResourcePool resources, List<String> log) {
        if (resources == null) return;
        BankLoan loan = loans.get(PLAYER_HOUSE_ID);
        if (loan == null) return;
        if (loan.isPaidOff()) { loans.remove(PLAYER_HOUSE_ID); return; }
        int due = loan.getNextInstallmentDue();
        if (resources.getMoney() >= due) {
            resources.spendMoney(due);
            if (bankHouse != null) bankHouse.addGold(due);
            loan.applyPayment(due);
            if (loan.isPaidOff()) {
                loans.remove(PLAYER_HOUSE_ID);
                getOrCreateAccount(PLAYER_HOUSE_ID).adjustCredit(BankParams.BANK_CREDIT_BONUS_PER_REPAYMENT);
                if (log != null) log.add("Your Bank loan has been fully repaid.");
            } else if (log != null) {
                log.add("Bank collects " + due + " gold loan installment from your treasury. "
                        + loan.getInstallmentsRemaining() + " installments remain.");
            }
        } else {
            loans.remove(PLAYER_HOUSE_ID);
            getOrCreateAccount(PLAYER_HOUSE_ID).adjustCredit(-BankParams.BANK_CREDIT_PENALTY_PER_DEFAULT);
            if (log != null) log.add("⚠ You cannot meet your loan installment and default on your Bank loan! Credit rating -"
                    + BankParams.BANK_CREDIT_PENALTY_PER_DEFAULT + ".");
            Debug.log("bank", "player-default", "Player defaulted on loan");
        }
    }

}