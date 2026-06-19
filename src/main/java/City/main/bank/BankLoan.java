package City.main.bank;

public class BankLoan {

    private final String borrowerHouseId;
    private final double interestRate;
    private final String collateralZoneId; // nullable
    private int     amountOwed;            // principal + interest, decremented as paid
    private int     installmentAmount;
    private int     installmentsRemaining;
    private boolean defaulted;

    public BankLoan(String borrowerHouseId, int principal, double interestRate,
                     int installments, String collateralZoneId) {
        this.borrowerHouseId       = borrowerHouseId;
        this.interestRate          = interestRate;
        this.collateralZoneId      = collateralZoneId;
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

    public String  getBorrowerHouseId()       { return borrowerHouseId; }
    public double  getInterestRate()          { return interestRate; }
    public String  getCollateralZoneId()      { return collateralZoneId; }
    public int     getPrincipalRemaining()    { return amountOwed; }
    public int     getInstallmentAmount()     { return installmentAmount; }
    public int     getInstallmentsRemaining() { return installmentsRemaining; }
    public boolean isDefaulted()              { return defaulted; }
    public void    markDefaulted()            { defaulted = true; }

    public void setInstallmentAmount(int v)     { installmentAmount = Math.max(0, v); }
    public void setInstallmentsRemaining(int v) { installmentsRemaining = Math.max(0, v); }
    public void setAmountOwed(int v)            { amountOwed = Math.max(0, v); }
}