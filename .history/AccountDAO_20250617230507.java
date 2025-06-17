import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * AccountDAO (Data Access Object) class for handling all database operations
 * related to the accounts tables. This class follows the DAO design pattern
 * and handles the Class Table Inheritance structure of accounts.
 * 
 * Refactored to implement Unit of Work pattern with connection management
 * and optimized to use the v_all_account_details VIEW for simplified queries.
 * 
 * PUBLIC METHODS: Create connections and delegate to private methods
 * PRIVATE METHODS: Accept Connection parameter and perform actual database operations
 */
public class AccountDAO {
    
    // Simplified SQL queries using the new database VIEW
    private static final String SELECT_ALL_ACCOUNTS = 
        "SELECT * FROM v_all_account_details ORDER BY account_number";
    
    private static final String SELECT_ACCOUNTS_BY_TYPE = 
        "SELECT * FROM v_all_account_details WHERE account_type = ? ORDER BY account_number";
    
    private static final String SELECT_ACCOUNT_BY_NUMBER = 
        "SELECT * FROM v_all_account_details WHERE account_number = ? ORDER BY account_number";
    
    private static final String SELECT_PROFIT_ACCOUNTS = 
        "SELECT * FROM v_all_account_details WHERE account_type != 'Savings Account' " +
        "ORDER BY COALESCE(rca_profit, bca_profit, ma_profit) DESC";
    
    private static final String SELECT_FEE_ACCOUNTS = 
        "SELECT * FROM v_all_account_details " +
        "WHERE account_type = 'Business Checking Account' OR account_type = 'Mortgage Account' " +
        "ORDER BY account_number";
    
    private static final String SELECT_TOP_CHECKING_BY_PROFIT = 
        "SELECT * FROM v_all_account_details " +
        "WHERE account_type LIKE '%Checking Account' " +
        "ORDER BY COALESCE(rca_profit, bca_profit) DESC LIMIT 1";
    
    // SQL queries for operations not using the VIEW
    private static final String INSERT_ACCOUNT_CLIENT = 
        "INSERT INTO account_clients (account_id, client_id) VALUES (?, ?)";
    
    private static final String CHECK_ACCOUNT_CLIENT_EXISTS = 
        "SELECT 1 FROM account_clients WHERE account_id = ? AND client_id = ?";
    
    private static final String INSERT_ACCOUNT = 
        "INSERT INTO accounts (account_number, account_type, date_opened, bank_id, balance, manager_name) " +
        "VALUES (?, ?, CURRENT_DATE, ?, 20.00, ?) RETURNING account_id";
    
    private static final String INSERT_REGULAR_CHECKING = 
        "INSERT INTO regular_checking_accounts (account_id, credit_limit, profit) VALUES (?, ?, ?)";
    
    private static final String INSERT_BUSINESS_CHECKING = 
        "INSERT INTO business_checking_accounts (account_id, credit_limit, business_revenue, profit, management_fee) " +
        "VALUES (?, ?, ?, ?, ?)";
    
    private static final String INSERT_MORTGAGE = 
        "INSERT INTO mortgage_accounts (account_id, original_mortgage_amount, monthly_payment, years, profit, management_fee) " +
        "VALUES (?, ?, ?, ?, ?, ?)";
    
    private static final String INSERT_SAVINGS = 
        "INSERT INTO savings_accounts (account_id, deposit_amount, years) VALUES (?, ?, ?)";

    private static final String DELETE_ACCOUNT_CLIENT = 
        "DELETE FROM account_clients WHERE account_id = ? AND client_id = ?";
    
    private static final String CHECK_ACCOUNT_EXISTS = 
        "SELECT 1 FROM accounts WHERE account_number = ?";

    // ========== PUBLIC WRAPPER METHODS (Unit of Work Pattern) ==========

    /**
     * Public wrapper for retrieving all accounts from the database.
     * Creates connection using Unit of Work pattern.
     */
    public static List<Account> getAllAccounts() {
        try (Connection conn = DatabaseManager.getConnection()) {
            return getAllAccounts(conn);
        } catch (SQLException e) {
            System.err.println("Connection error in getAllAccounts: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve accounts from database", e);
        }
    }

    /**
     * Public wrapper for retrieving all accounts of a specific type from the database.
     * Creates connection using Unit of Work pattern.
     */
    public static List<Account> getAccountsByType(String accountType) {
        try (Connection conn = DatabaseManager.getConnection()) {
            return getAccountsByType(conn, accountType);
        } catch (SQLException e) {
            System.err.println("Connection error in getAccountsByType: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve accounts from database", e);
        }
    }

    /**
     * Public wrapper for retrieving a specific account by its account number.
     * Creates connection using Unit of Work pattern.
     */
    public static Account getAccountByNumber(int accountNumber) {
        try (Connection conn = DatabaseManager.getConnection()) {
            return getAccountByNumber(conn, accountNumber);
        } catch (SQLException e) {
            System.err.println("Connection error in getAccountByNumber: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve account from database", e);
        }
    }

    /**
     * Public wrapper for checking if an account exists in the database.
     * Creates connection using Unit of Work pattern.
     */
    public static boolean accountExists(int accountNumber) {
        try (Connection conn = DatabaseManager.getConnection()) {
            return accountExists(conn, accountNumber);
        } catch (SQLException e) {
            System.err.println("Connection error in accountExists: " + e.getMessage());
            return false;
        }
    }

    /**
     * Public wrapper for adding a new account to the database.
     * Creates connection using Unit of Work pattern.
     */
    public static Account addAccount(Account newAccount) {
        try (Connection conn = DatabaseManager.getConnection()) {
            return addAccount(conn, newAccount);
        } catch (SQLException e) {
            System.err.println("Connection error in addAccount: " + e.getMessage());
            throw new RuntimeException("Failed to add account to database", e);
        }
    }

    /**
     * Public wrapper for associating a client with an account.
     * Creates connection using Unit of Work pattern.
     */
    public static void addClientToAccount(int accountId, int clientId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            addClientToAccount(conn, accountId, clientId);
        } catch (SQLException e) {
            System.err.println("Connection error in addClientToAccount: " + e.getMessage());
            throw new RuntimeException("Failed to associate client with account", e);
        }
    }

    /**
     * Public wrapper for checking if a client is already associated with an account.
     * Creates connection using Unit of Work pattern.
     */
    public static boolean isClientAssociatedWithAccount(int accountId, int clientId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            return isClientAssociatedWithAccount(conn, accountId, clientId);
        } catch (SQLException e) {
            System.err.println("Connection error in isClientAssociatedWithAccount: " + e.getMessage());
            return false;
        }
    }

    /**
     * Public wrapper for retrieving all accounts that implement the ProfitAccount interface.
     * Creates connection using Unit of Work pattern.
     */
    public static List<Account> getProfitAccounts() {
        try (Connection conn = DatabaseManager.getConnection()) {
            return getProfitAccounts(conn);
        } catch (SQLException e) {
            System.err.println("Connection error in getProfitAccounts: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve profit accounts from database", e);
        }
    }

    /**
     * Public wrapper for retrieving all accounts that implement the ManagementFeeAccount interface.
     * Creates connection using Unit of Work pattern.
     */
    public static List<Account> getFeeAccounts() {
        try (Connection conn = DatabaseManager.getConnection()) {
            return getFeeAccounts(conn);
        } catch (SQLException e) {
            System.err.println("Connection error in getFeeAccounts: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve management fee accounts from database", e);
        }
    }

    /**
     * Public wrapper for retrieving the top checking account by profit.
     * Creates connection using Unit of Work pattern.
     */
    public static Account getTopCheckingAccountByProfit() {
        try (Connection conn = DatabaseManager.getConnection()) {
            return getTopCheckingAccountByProfit(conn);
        } catch (SQLException e) {
            System.err.println("Connection error in getTopCheckingAccountByProfit: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve top checking account from database", e);
        }
    }

    /**
     * Public wrapper for removing a client from an account.
     * Creates connection using Unit of Work pattern.
     */
    public static boolean removeClientFromAccount(int accountId, int clientId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            return removeClientFromAccount(conn, accountId, clientId);
        } catch (SQLException e) {
            System.err.println("Connection error in removeClientFromAccount: " + e.getMessage());
            throw new RuntimeException("Failed to remove client from account", e);
        }
    }

    // ========== PRIVATE METHODS (Accept Connection parameter) ==========

    /**
     * Retrieves all accounts from the database using provided connection.
     * Uses the v_all_account_details VIEW for optimized performance.
     */
    private static List<Account> getAllAccounts(Connection conn) throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Account> accounts = new ArrayList<>();
        
        try {
            pstmt = conn.prepareStatement(SELECT_ALL_ACCOUNTS);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Account account = createAccountFromResultSet(rs);
                if (account != null) {
                    accounts.add(account);
                }
            }
            
            System.out.println("Successfully loaded " + accounts.size() + " accounts from database using VIEW.");
            
        } finally {
            DatabaseManager.close(null, pstmt, rs);
        }
        
        return accounts;
    }

    /**
     * Retrieves all accounts of a specific type from the database using provided connection.
     * Uses the v_all_account_details VIEW for optimized performance.
     */
    private static List<Account> getAccountsByType(Connection conn, String accountType) throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Account> accounts = new ArrayList<>();
        
        try {
            pstmt = conn.prepareStatement(SELECT_ACCOUNTS_BY_TYPE);
            pstmt.setString(1, accountType);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Account account = createAccountFromResultSet(rs);
                if (account != null) {
                    accounts.add(account);
                }
            }
            
            System.out.println("Successfully loaded " + accounts.size() + " accounts of type '" + accountType + "' from database using VIEW.");
            
        } finally {
            DatabaseManager.close(null, pstmt, rs);
        }
        
        return accounts;
    }

    /**
     * Retrieves a specific account by its account number using provided connection.
     * Uses the v_all_account_details VIEW for optimized performance.
     */
    private static Account getAccountByNumber(Connection conn, int accountNumber) throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Account account = null;
        
        try {
            pstmt = conn.prepareStatement(SELECT_ACCOUNT_BY_NUMBER);
            pstmt.setInt(1, accountNumber);
            
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                account = createAccountFromResultSet(rs);
                if (account != null) {
                    System.out.println("Found account using VIEW: " + account.getAccountType() + " #" + accountNumber);
                }
            } else {
                System.out.println("No account found with number: " + accountNumber);
            }
            
        } finally {
            DatabaseManager.close(null, pstmt, rs);
        }
        
        return account;
    }

    /**
     * Checks if an account exists in the database using provided connection.
     */
    private static boolean accountExists(Connection conn, int accountNumber) throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            pstmt = conn.prepareStatement(CHECK_ACCOUNT_EXISTS);
            pstmt.setInt(1, accountNumber);
            rs = pstmt.executeQuery();
            
            boolean exists = rs.next();
            System.out.println("Account #" + accountNumber + " exists: " + exists);
            return exists;
            
        } finally {
            DatabaseManager.close(null, pstmt, rs);
        }
    }

    /**
     * Adds a new account to the database using provided connection.
     * This method handles the transaction for inserting into both base and subclass tables.
     */
    private static Account addAccount(Connection conn, Account newAccount) throws SQLException {
        if (newAccount.hasId()) {
            throw new IllegalArgumentException("Cannot add account that already has a database ID: " + newAccount.getId());
        }
        
        PreparedStatement pstmt1 = null; // For accounts table insert
        PreparedStatement pstmt2 = null; // For subclass table insert
        ResultSet rs = null;
        Account accountWithId = null;
        
        try {
            conn.setAutoCommit(false); // Start transaction
            
            // Insert into the base accounts table
            pstmt1 = conn.prepareStatement(INSERT_ACCOUNT);
            pstmt1.setInt(1, newAccount.getAccountNumber());
            pstmt1.setString(2, newAccount.getAccountType());
            pstmt1.setInt(3, newAccount.getBankNumber());
            pstmt1.setString(4, newAccount.getManagerName());
            
            rs = pstmt1.executeQuery();
            
            int generatedAccountId;
            if (rs.next()) {
                generatedAccountId = rs.getInt("account_id");
                System.out.println("Inserted into accounts table with ID: " + generatedAccountId);
            } else {
                throw new SQLException("Failed to retrieve generated account ID after insert");
            }
            
            // Insert into the appropriate subclass table
            pstmt2 = createSubclassInsertStatement(conn, newAccount, generatedAccountId);
            int rowsAffected = pstmt2.executeUpdate();
            if (rowsAffected != 1) {
                throw new SQLException("Failed to insert into subclass table. Rows affected: " + rowsAffected);
            }
            
            System.out.println("Inserted into " + newAccount.getAccountType().toLowerCase().replace(" ", "_") + " table");
            
            conn.commit();
            System.out.println("Transaction committed successfully for account #" + newAccount.getAccountNumber());
            
            // Create a new Account object with the database ID
            accountWithId = createAccountWithId(newAccount, generatedAccountId);
            
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                    System.err.println("Transaction rolled back due to error: " + e.getMessage());
                }
            } catch (SQLException rollbackEx) {
                System.err.println("Error during rollback: " + rollbackEx.getMessage());
            }
            throw e;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                System.err.println("Error resetting auto-commit: " + e.getMessage());
            }
            
            DatabaseManager.close(null, pstmt2, null);
            DatabaseManager.close(null, pstmt1, rs);
        }
        
        return accountWithId;
    }

    /**
     * Associates a client with an account using provided connection.
     */
    private static void addClientToAccount(Connection conn, int accountId, int clientId) throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            // Check if the association already exists
            pstmt = conn.prepareStatement(CHECK_ACCOUNT_CLIENT_EXISTS);
            pstmt.setInt(1, accountId);
            pstmt.setInt(2, clientId);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                throw new IllegalArgumentException("Client ID " + clientId + " is already associated with Account ID " + accountId);
            }
            
            // Close the check statement and prepare the insert
            DatabaseManager.close(null, pstmt, rs);
            pstmt = null;
            rs = null;
            
            // Insert the new account-client association
            pstmt = conn.prepareStatement(INSERT_ACCOUNT_CLIENT);
            pstmt.setInt(1, accountId);
            pstmt.setInt(2, clientId);
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected == 1) {
                System.out.println("Successfully associated Client ID " + clientId + " with Account ID " + accountId);
            } else {
                throw new SQLException("Failed to insert account-client association. No rows affected.");
            }
            
        } finally {
            DatabaseManager.close(null, pstmt, rs);
        }
    }

    /**
     * Checks if a client is already associated with an account using provided connection.
     */
    private static boolean isClientAssociatedWithAccount(Connection conn, int accountId, int clientId) throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            pstmt = conn.prepareStatement(CHECK_ACCOUNT_CLIENT_EXISTS);
            pstmt.setInt(1, accountId);
            pstmt.setInt(2, clientId);
            rs = pstmt.executeQuery();
            
            boolean isAssociated = rs.next();
            System.out.println("Client ID " + clientId + " associated with Account ID " + accountId + ": " + isAssociated);
            return isAssociated;
            
        } finally {
            DatabaseManager.close(null, pstmt, rs);
        }
    }

    /**
     * Retrieves all accounts that implement the ProfitAccount interface using provided connection.
     * Uses the v_all_account_details VIEW for optimized performance.
     */
    private static List<Account> getProfitAccounts(Connection conn) throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Account> accounts = new ArrayList<>();
        
        try {
            pstmt = conn.prepareStatement(SELECT_PROFIT_ACCOUNTS);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Account account = createAccountFromResultSet(rs);
                if (account != null) {
                    accounts.add(account);
                    System.out.println("Loaded profit account using VIEW: " + account.getAccountType() + " #" + account.getAccountNumber() + " (Profit: " + ((ProfitAccount) account).getProfit() + ")");
                }
            }
            
            System.out.println("Successfully loaded " + accounts.size() + " profit accounts from database using VIEW, ordered by profit (highest first).");
            
        } finally {
            DatabaseManager.close(null, pstmt, rs);
        }
        
        return accounts;
    }

    /**
     * Retrieves all accounts that implement the ManagementFeeAccount interface using provided connection.
     * Uses the v_all_account_details VIEW for optimized performance.
     */
    private static List<Account> getFeeAccounts(Connection conn) throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Account> accounts = new ArrayList<>();
        
        try {
            pstmt = conn.prepareStatement(SELECT_FEE_ACCOUNTS);
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Account account = createAccountFromResultSet(rs);
                if (account != null) {
                    accounts.add(account);
                    System.out.println("Loaded fee account using VIEW: " + account.getAccountType() + " #" + account.getAccountNumber() + " (Fee: " + ((ManagementFeeAccount) account).getManagementFee() + ")");
                }
            }
            
            System.out.println("Successfully loaded " + accounts.size() + " management fee accounts from database using VIEW.");
            
        } finally {
            DatabaseManager.close(null, pstmt, rs);
        }
        
        return accounts;
    }

    /**
     * Retrieves the top checking account by profit using provided connection.
     * Uses the v_all_account_details VIEW for optimized performance.
     */
    private static Account getTopCheckingAccountByProfit(Connection conn) throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Account account = null;
        
        try {
            pstmt = conn.prepareStatement(SELECT_TOP_CHECKING_BY_PROFIT);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                account = createAccountFromResultSet(rs);
                if (account != null) {
                    System.out.println("Found top checking account using VIEW: " + account.getAccountType() + " #" + account.getAccountNumber() + " (Profit: " + ((ProfitAccount) account).getProfit() + ")");
                }
            } else {
                System.out.println("No checking accounts found in database.");
            }
            
        } finally {
            DatabaseManager.close(null, pstmt, rs);
        }
        
        return account;
    }

    /**
     * Removes a client from an account using provided connection.
     */
    private static boolean removeClientFromAccount(Connection conn, int accountId, int clientId) throws SQLException {
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            pstmt = conn.prepareStatement(DELETE_ACCOUNT_CLIENT);
            pstmt.setInt(1, accountId);
            pstmt.setInt(2, clientId);
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected == 1) {
                success = true;
                System.out.println("Successfully removed Client ID " + clientId + " from Account ID " + accountId);
            } else if (rowsAffected == 0) {
                System.err.println("No association found between Account ID " + accountId + " and Client ID " + clientId + " to remove.");
            } else {
                System.err.println("Unexpected: " + rowsAffected + " rows affected when removing client-account association.");
            }
            
        } finally {
            DatabaseManager.close(null, pstmt, null);
        }
        
        return success;
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Creates an Account object from a ResultSet row.
     * Works with the v_all_account_details VIEW structure.
     */
    private static Account createAccountFromResultSet(ResultSet rs) throws SQLException {
        int accountId = rs.getInt("account_id");
        int accountNumber = rs.getInt("account_number");
        String accountType = rs.getString("account_type");
        int bankNumber = rs.getInt("bank_id");
        String managerName = rs.getString("manager_name");
        
        Account account = null;
        
        try {
            switch (accountType) {
                case "Regular Checking Account":
                    double rcaCreditLimit = rs.getDouble("rca_credit_limit");
                    account = new RegularCheckingAccount(accountId, accountNumber, bankNumber, managerName, rcaCreditLimit);
                    break;
                    
                case "Business Checking Account":
                    double bcaCreditLimit = rs.getDouble("bca_credit_limit");
                    double businessRevenue = rs.getDouble("business_revenue");
                    account = new BusinessCheckingAccount(accountId, accountNumber, bankNumber, managerName, bcaCreditLimit, businessRevenue);
                    break;
                    
                case "Mortgage Account":
                    double originalMortgageAmount = rs.getDouble("original_mortgage_amount");
                    double monthlyPayment = rs.getDouble("monthly_payment");
                    int mortgageYears = rs.getInt("ma_years");
                    account = new MortgageAccount(accountId, accountNumber, bankNumber, managerName, originalMortgageAmount, monthlyPayment, mortgageYears);
                    break;
                    
                case "Savings Account":
                    double depositAmount = rs.getDouble("deposit_amount");
                    int savingsYears = rs.getInt("sa_years");
                    account = new SavingsAccount(accountId, accountNumber, bankNumber, managerName, depositAmount, savingsYears);
                    break;
                    
                default:
                    System.err.println("Unknown account type: " + accountType + " for account number: " + accountNumber);
                    return null;
            }
            
            if (account != null) {
                System.out.println("Loaded account from VIEW: " + accountType + " #" + accountNumber);
            }
            
        } catch (DuplicationException e) {
            System.err.println("Duplicate account number detected while loading from database: " + e.getMessage());
            return null;
        }
        
        return account;
    }

    /**
     * Creates the appropriate PreparedStatement for inserting into subclass tables.
     */
    private static PreparedStatement createSubclassInsertStatement(Connection conn, Account newAccount, int generatedAccountId) throws SQLException {
        PreparedStatement pstmt = null;
        
        switch (newAccount.getAccountType()) {
            case "Regular Checking Account":
                RegularCheckingAccount rca = (RegularCheckingAccount) newAccount;
                pstmt = conn.prepareStatement(INSERT_REGULAR_CHECKING);
                pstmt.setInt(1, generatedAccountId);
                pstmt.setDouble(2, rca.getCreditLimit());
                pstmt.setDouble(3, rca.getProfit());
                break;
                
            case "Business Checking Account":
                BusinessCheckingAccount bca = (BusinessCheckingAccount) newAccount;
                pstmt = conn.prepareStatement(INSERT_BUSINESS_CHECKING);
                pstmt.setInt(1, generatedAccountId);
                pstmt.setDouble(2, bca.getCreditLimit());
                pstmt.setDouble(3, bca.getBusinessRevenue());
                pstmt.setDouble(4, bca.getProfit());
                pstmt.setDouble(5, bca.getManagementFee());
                break;
                
            case "Mortgage Account":
                MortgageAccount ma = (MortgageAccount) newAccount;
                pstmt = conn.prepareStatement(INSERT_MORTGAGE);
                pstmt.setInt(1, generatedAccountId);
                pstmt.setDouble(2, ma.getOriginalMortgageAmount());
                pstmt.setDouble(3, ma.getMonthlyPayment());
                pstmt.setInt(4, ma.getYears());
                pstmt.setDouble(5, ma.getProfit());
                pstmt.setDouble(6, ma.getManagementFee());
                break;
                
            case "Savings Account":
                SavingsAccount sa = (SavingsAccount) newAccount;
                pstmt = conn.prepareStatement(INSERT_SAVINGS);
                pstmt.setInt(1, generatedAccountId);
                pstmt.setDouble(2, sa.getDepositAmount());
                pstmt.setInt(3, sa.getYears());
                break;
                
            default:
                throw new IllegalArgumentException("Unknown account type: " + newAccount.getAccountType());
        }
        
        return pstmt;
    }

    /**
     * Creates a new Account object with the database-generated ID.
     */
    private static Account createAccountWithId(Account newAccount, int generatedAccountId) {
        Account accountWithId = null;
        
        try {
            switch (newAccount.getAccountType()) {
                case "Regular Checking Account":
                    RegularCheckingAccount rca = (RegularCheckingAccount) newAccount;
                    accountWithId = new RegularCheckingAccount(generatedAccountId, newAccount.getAccountNumber(), 
                            newAccount.getBankNumber(), newAccount.getManagerName(), rca.getCreditLimit());
                    break;
                    
                case "Business Checking Account":
                    BusinessCheckingAccount bca = (BusinessCheckingAccount) newAccount;
                    accountWithId = new BusinessCheckingAccount(generatedAccountId, newAccount.getAccountNumber(), 
                            newAccount.getBankNumber(), newAccount.getManagerName(), bca.getCreditLimit(), bca.getBusinessRevenue());
                    break;
                    
                case "Mortgage Account":
                    MortgageAccount ma = (MortgageAccount) newAccount;
                    accountWithId = new MortgageAccount(generatedAccountId, newAccount.getAccountNumber(), 
                            newAccount.getBankNumber(), newAccount.getManagerName(), ma.getOriginalMortgageAmount(), 
                            ma.getMonthlyPayment(), ma.getYears());
                    break;
                    
                case "Savings Account":
                    SavingsAccount sa = (SavingsAccount) newAccount;
                    accountWithId = new SavingsAccount(generatedAccountId, newAccount.getAccountNumber(), 
                            newAccount.getBankNumber(), newAccount.getManagerName(), sa.getDepositAmount(), sa.getYears());
                    break;
            }
        } catch (DuplicationException e) {
            throw new RuntimeException("Unexpected duplication error when creating account object", e);
        }
        
        return accountWithId;
    }
}