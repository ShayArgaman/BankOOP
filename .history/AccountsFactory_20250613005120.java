public class AccountsFactory {
    private static final int HARD_CODED_COUNT = 4; // Number of accounts to create per type
    private static final int ACCOUNT_TYPES_COUNT = 4; // Number of different account types
    private static int lastAccountNumber = 1; // Initial account number for auto-increment


    // Method to create an array of 4 accounts, one of each required type
    public Account[] createAccounts() throws DuplicationException {
        Account[] accounts = new Account[HARD_CODED_COUNT * ACCOUNT_TYPES_COUNT];

        int index = 0;

        // Create Regular Checking Accounts
        for (int i = 0; i < HARD_CODED_COUNT; i++) {
            accounts[index++] = new RegularCheckingAccount(getNextAccountNumber(), getBankNumber(), "Manager A", getCreditLimit());
        }

        // Create Business Checking Accounts
        for (int i = 0; i < HARD_CODED_COUNT; i++) {
            accounts[index++] = new BusinessCheckingAccount(getNextAccountNumber(), getBankNumber(), "Manager B", getCreditLimit(), getBusinessRevenue());
        }

        // Create Mortgage Accounts
        for (int i = 0; i < HARD_CODED_COUNT; i++) {
            accounts[index++] = new MortgageAccount(getNextAccountNumber(), getBankNumber(), "Manager C", getOriginalMortgageAmount(), getMonthlyPayment(), getYears());
        }

        // Create Savings Accounts
        for (int i = 0; i < HARD_CODED_COUNT; i++) {
            accounts[index++] = new SavingsAccount(getNextAccountNumber(), getBankNumber(), "Manager D", getDepositAmount(), getYears());
        }


        // Add clients to each account
        for (Account account : accounts) {
            addClientsToAccount(account);
        }

        return accounts;
    }

    // Method to get the next available account number (auto-incremented)
    private int getNextAccountNumber() {
        while (AccountDAO.accountExists(lastAccountNumber)) {  // FIXED: Use DAO directly
            lastAccountNumber++; // Increment until we find an available number
        }
        return lastAccountNumber++;
    }

    // Placeholder methods to provide default values for account details
    private int getBankNumber() {
        return 1;
    }

    private double getCreditLimit() {
        return 5000.0;
    }

    private double getBusinessRevenue() {
        return 10000000.0;
    }

    private int getClientRank() {
        return 10;
    }

    private double getOriginalMortgageAmount() {
        return 250000.0;
    }

    private double getMonthlyPayment() {
        return 2000.0;
    }

    private int getYears() {
        return 15;
    }

    private double getDepositAmount() {
        return 10000.0;
    }

    // Method to add clients to an account with default names and ranks
    private void addClientsToAccount(Account account) {
        account.addClient(new Client("Client A", getClientRank()));
        account.addClient(new Client("Client B", getClientRank()));
    }
}