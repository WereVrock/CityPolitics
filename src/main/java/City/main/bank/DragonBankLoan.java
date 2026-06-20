package City.main.bank;

/**
 * An active loan from the Dragon Bank. Unsecured — no collateral, no
 * credit-rating-based pricing. Interest is fixed and high.
 */
public class DragonBankLoan {

    private final String borrowerId;
    private final double interestRate;
    private int     amountOwed;
    private int     installmentAmount;
    private int     installmentsRemaining;
    private boolean defaulted;

    public DragonBankLoan(String borrowerId, int principal, double interestRate, int installments) {
        this.borrowerId            = borrowerId;
        this.interestRate          = interestRate;
        this.amountOwed            = (int) Math.ceil(principal * (1.0 + interestRate));
        this.installmentsRemaining = Math.max(1, installments);
        this.installmentAmount     = (int) Math.ceil((double) amountOwed / this.installmentsRemaining);
        this.defaulted             = false;
    }

    public int getNextInstallmentDue() {
        return Math.min(installmentAmount, amountOwed);
    }

    public void applyPayment(int amount) {
        amountOwed = Math.max(0, amountOwed - amount);
        if (installmentsRemaining > 0) installmentsRemaining--;
    }

    public boolean isPaidOff() { return amountOwed <= 0; }

    public int getFullRepaymentAmount() { return amountOwed; }

    public String  getBorrowerId()            { return borrowerId; }
    public double  getInterestRate()          { return interestRate; }
    public int     getInstallmentAmount()     { return installmentAmount; }
    public int     getInstallmentsRemaining() { return installmentsRemaining; }
    public boolean isDefaulted()              { return defaulted; }
    public void    markDefaulted()            { defaulted = true; }

    public void setInstallmentAmount(int v)     { installmentAmount = Math.max(0, v); }
    public void setInstallmentsRemaining(int v) { installmentsRemaining = Math.max(0, v); }
    public void setAmountOwed(int v)            { amountOwed = Math.max(0, v); }
}