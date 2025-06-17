import java.text.SimpleDateFormat;

public class SavingsAccount extends Account {
    private final double depositAmount;
    private final int years;

    /**
     * Constructor for creating SavingsAccount objects from database data.
     * This constructor should be used when retrieving accounts from the database.
     * 
     * @param id The unique database ID of the account
     * @param accountNumber The account number
     * @param bankNumber The bank number
     * @param managerName The manager's name
     * @param depositAmount The deposit amount for this savings account
     * @param years The number of years for the savings account
     * @throws DuplicationException if the account number is already in use
     */
    public SavingsAccount(int id, int accountNumber, int bankNumber, String managerName, double depositAmount, int years) throws DuplicationException {
        super(id, accountNumber, bankNumber, managerName);
        this.depositAmount = depositAmount;
        this.years = years;
    }
    
    /**
     * Constructor for creating new SavingsAccount objects (without database ID).
     * This constructor should be used when creating new accounts that haven't been 
     * saved to the database yet (the database will auto-generate the ID).
     * 
     * @param accountNumber The account number
     * @param bankNumber The bank number
     * @param managerName The manager's name
     * @param depositAmount The deposit amount for this savings account
     * @param years The number of years for the savings account
     * @throws DuplicationException if the account number is already in use
     */
    public SavingsAccount(int accountNumber, int bankNumber, String managerName, double depositAmount, int years) throws DuplicationException {
        super(accountNumber, bankNumber, managerName);
        this.depositAmount = depositAmount;
        this.years = years;
    }

    @Override
    public String getAccountType() {
        return "Savings Account";
    }

    @Override
    public String toString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String baseFormat = "%-25s {accountNumber=%d, dateOpened=%s, bankNumber=%d, managerName='%s', balance=%.2f NIS, depositAmount=%.2f NIS, years=%d}";
        
        if (hasId()) {
            return String.format("(ID:" + getId() + ") " + baseFormat,
                    getAccountType(),
                    getAccountNumber(),
                    dateFormat.format(getDateOpened()),
                    getBankNumber(),
                    getManagerName(),
                    getBalance(),
                    depositAmount, 
                    years);
        } else {
            return String.format(baseFormat,
                    getAccountType(),
                    getAccountNumber(),
                    dateFormat.format(getDateOpened()),
                    getBankNumber(),
                    getManagerName(),
                    getBalance(),
                    depositAmount, 
                    years);
        }
    }

    public double getDepositAmount() {
        return depositAmount;
    }
    
    public int getYears() {
        return years;
    }
}