import java.text.SimpleDateFormat;

public class RegularCheckingAccount extends CheckingAccount {
    private double profit = 0;

    /**
     * Constructor for creating RegularCheckingAccount objects from database data.
     * This constructor should be used when retrieving accounts from the database.
     * 
     * @param id The unique database ID of the account
     * @param accountNumber The account number
     * @param bankNumber The bank number
     * @param managerName The manager's name
     * @param creditLimit The credit limit for this checking account
     * @throws DuplicationException if the account number is already in use
     */
    public RegularCheckingAccount(int id, int accountNumber, int bankNumber, String managerName, double creditLimit) throws DuplicationException {
        super(id, accountNumber, bankNumber, managerName, creditLimit);
        calculateProfit(); // Calculate profit when the account is created
    }
    
    /**
     * Constructor for creating new RegularCheckingAccount objects (without database ID).
     * This constructor should be used when creating new accounts that haven't been 
     * saved to the database yet (the database will auto-generate the ID).
     * 
     * @param accountNumber The account number
     * @param bankNumber The bank number
     * @param managerName The manager's name
     * @param creditLimit The credit limit for this checking account
     * @throws DuplicationException if the account number is already in use
     */
    public RegularCheckingAccount(int accountNumber, int bankNumber, String managerName, double creditLimit) throws DuplicationException {
        super(accountNumber, bankNumber, managerName, creditLimit);
        calculateProfit(); // Calculate profit when the account is created
    }

    @Override
    public double getProfit() {
        return this.profit; // Return the profit that was already calculated
    }

    // Override getAccountType method to return the account type as a string
    @Override
    public String getAccountType() {
        return "Regular Checking Account";
    }

    // Override calculateProfit method to calculate the profit based on credit limit
    @Override
    public void calculateProfit() {
        this.profit = this.getCreditLimit() * RATE_DIFFERENCE;
    }

    // Override toString method to format the account details as a string
    @Override
    public String toString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String baseFormat = "%-25s {accountNumber=%d, dateOpened=%s, bankNumber=%d, managerName='%s', balance=%.2f NIS, creditLimit=%.2f NIS}";
        
        if (hasId()) {
            return String.format("(ID:" + getId() + ") " + baseFormat,
                    getAccountType(),
                    getAccountNumber(),
                    dateFormat.format(getDateOpened()),
                    getBankNumber(),
                    getManagerName(),
                    getBalance(),
                    getCreditLimit()
            );
        } else {
            return String.format(baseFormat,
                    getAccountType(),
                    getAccountNumber(),
                    dateFormat.format(getDateOpened()),
                    getBankNumber(),
                    getManagerName(),
                    getBalance(),
                    getCreditLimit()
            );
        }
    }
}