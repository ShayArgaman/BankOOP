// UPDATED CheckingAccount.java

public abstract class CheckingAccount extends Account implements ProfitAccount {
    private final double creditLimit;
    protected static final double RATE_DIFFERENCE = 0.10;

    /**
     * Constructor for creating objects from database data (with ID).
     * @param id The unique database ID
     * @param accountNumber The account number
     * @param bankNumber The bank number
     * @param managerName The manager's name
     * @param creditLimit The account's credit limit
     * @throws DuplicationException if account number is a duplicate
     */
    public CheckingAccount(int id, int accountNumber, int bankNumber, String managerName, double creditLimit) throws DuplicationException {
        super(id, accountNumber, bankNumber, managerName); // Passes the ID up to the Account class
        this.creditLimit = creditLimit;
    }
    
    /**
     * Constructor for creating new objects (without ID).
     * This calls the other constructor, passing -1 as a placeholder for the ID.
     * @param accountNumber The account number
     * @param bankNumber The bank number
     * @param managerName The manager's name
     * @param creditLimit The account's credit limit
     * @throws DuplicationException if account number is a duplicate
     */
    public CheckingAccount(int accountNumber, int bankNumber, String managerName, double creditLimit) throws DuplicationException {
        super(accountNumber, bankNumber, managerName); // Calls the old constructor in Account
        this.creditLimit = creditLimit;
    }

    public double getCreditLimit() {
        return creditLimit;
    }

    // Abstract method for profit calculation, remains unchanged
    @Override
    public abstract void calculateProfit();
}