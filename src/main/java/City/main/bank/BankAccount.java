package City.main.bank;

import City.main.parameters.BankParams;

public class BankAccount {

    private final String houseId;
    private int     deposit;
    private int     creditRating;
    private boolean stakeholder;

    public BankAccount(String houseId) {
        this.houseId      = houseId;
        this.deposit       = 0;
        this.creditRating  = BankParams.BANK_CREDIT_RATING_BASE;
        this.stakeholder   = false;
    }

    public String  getHouseId()      { return houseId; }
    public int     getDeposit()      { return deposit; }
    public boolean isStakeholder()   { return stakeholder; }
    public int     getCreditRating() { return creditRating; }

    public void addDeposit(int amount) {
        deposit = Math.max(0, deposit + amount);
        if (deposit > 0) stakeholder = true;
    }

    public void removeDeposit(int amount) {
        deposit = Math.max(0, deposit - amount);
        if (deposit <= 0) stakeholder = false;
    }

    public void setDeposit(int amount) {
        deposit     = Math.max(0, amount);
        stakeholder = deposit > 0;
    }

    public void clearStakeholderStatus() {
        stakeholder = false;
        deposit     = 0;
    }

    public void adjustCredit(int delta) {
        creditRating = Math.max(BankParams.BANK_CREDIT_RATING_MIN,
                Math.min(BankParams.BANK_CREDIT_RATING_MAX, creditRating + delta));
    }

    public void capCreditAt(int max) {
        if (creditRating > max) creditRating = max;
    }
}