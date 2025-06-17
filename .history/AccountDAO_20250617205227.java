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
 */
public class AccountDAO {
    
    // Complex SQL query to retrieve all account data using LEFT JOINs for Class Table Inheritance
    private static final String SELECT_ALL_ACCOUNTS = 
        "SELECT " +
        // Base account fields
        "a.account_id, a.account_number, a.account_type, a.date_opened, " +
        "a.bank_id, a.balance, a.manager_name, " +
        // Regular checking account fields
        "rca.credit_limit as rca_credit_limit, rca.profit as rca_profit, " +
        // Business checking account fields  
        "bca.credit_limit as bca_credit_limit, bca.business_revenue, " +
        "bca.profit as bca_profit, bca.management_fee as bca_management_fee, " +
        // Mortgage account fields
        "ma.original_mortgage_amount, ma.monthly_payment, ma.years as ma_years, " +
        "ma.profit as ma_profit, ma.management_fee as ma_management_fee, " +
        // Savings account fields
        "sa.deposit_amount, sa.years as sa_years " +
        "FROM accounts a " +
        "LEFT JOIN regular_checking_accounts rca ON a.account_id = rca.account_id " +
        "LEFT JOIN business_checking_accounts bca ON a.account_id = bca.account_id " +
        "LEFT JOIN mortgage_accounts ma ON a.account_id = ma.account_id " +
        "LEFT JOIN savings_accounts sa ON a.account_id = sa.account_id " +
        "ORDER BY a.account_number";
    
    // Same query as above but with WHERE clause for filtering by account type
    private static final String SELECT_ACCOUNTS_BY_TYPE = 
        "SELECT " +
        // Base account fields
        "a.account_id, a.account_number, a.account_type, a.date_opened, " +
        "a.bank_id, a.balance, a.manager_name, " +
        // Regular checking account fields
        "rca.credit_limit as rca_credit_limit, rca.profit as rca_profit, " +
        // Business checking account fields  
        "bca.credit_limit as bca_credit_limit, bca.business_revenue, " +
        "bca.profit as bca_profit, bca.management_fee as bca_management_fee, " +
        // Mortgage account fields
        "ma.original_mortgage_amount, ma.monthly_payment, ma.years as ma_years, " +
        "ma.profit as ma_profit, ma.management_fee as ma_management_fee, " +
        // Savings account fields
        "sa.deposit_amount, sa.years as sa_years " +
        "FROM accounts a " +
        "LEFT JOIN regular_checking_accounts rca ON a.account_id = rca.account_id " +
        "LEFT JOIN business_checking_accounts bca ON a.account_id = bca.account_id " +
        "LEFT JOIN mortgage_accounts ma ON a.account_id = ma.account_id " +
        "LEFT JOIN savings_accounts sa ON a.account_id = sa.account_id " +
        "WHERE a.account_type = ? " +
        "ORDER BY a.account_number";
    
    // SQL query to insert a new account-client association
    private static final String INSERT_ACCOUNT_CLIENT = 
        "INSERT INTO account_clients (account_id, client_id) VALUES (?, ?)";
    
    // SQL query to check if an account-client association already exists
    private static final String CHECK_ACCOUNT_CLIENT_EXISTS = 
        "SELECT 1 FROM account_clients WHERE account_id = ? AND client_id = ?";
    
    // SQL query to insert into the base accounts table
    private static final String INSERT_ACCOUNT = 
        "INSERT INTO accounts (account_number, account_type, date_opened, bank_id, balance, manager_name) " +
        "VALUES (?, ?, CURRENT_DATE, ?, 20.00, ?) RETURNING account_id";
    
    // SQL queries to insert into subclass tables
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
    
    /**
     * Retrieves all accounts from the database, including data from all account subtypes.
     * This method handles the Class Table Inheritance pattern by joining all account tables.
     * Note: This method retrieves accounts without their associated clients.
     * 
     * @return A List of Account objects representing all accounts in the database
     * @throws RuntimeException if a database error occurs during the operation
     */
    public static List<Account> getAllAccounts() {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Account> accounts = new ArrayList<>();
        
        try {
            // Step 1: Get database connection
            conn = DatabaseManager.getConnection();
            
            // Step 2: Prepare the complex SQL statement
            pstmt = conn.prepareStatement(SELECT_ALL_ACCOUNTS);
            
            // Step 3: Execute the query
            rs = pstmt.executeQuery();
            
            // Step 4: Process each row in the ResultSet
            while (rs.next()) {
                // Extract common account data including database ID
                int accountId = rs.getInt("account_id");        // NEW: Extract database ID
                int accountNumber = rs.getInt("account_number");
                String accountType = rs.getString("account_type");
                int bankNumber = rs.getInt("bank_id");
                String managerName = rs.getString("manager_name");
                
                Account account = null;
                
                // Step 5: Use switch statement to instantiate the correct account subclass
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
                            continue; // Skip this account and continue with the next one
                    }
                    
                    // If account was successfully created, add it to the list
                    if (account != null) {
                        accounts.add(account);
                        System.out.println("Loaded account: " + accountType + " #" + accountNumber);
                    }
                    
                } catch (DuplicationException e) {
                    // This shouldn't happen when reading from database, but handle it gracefully
                    System.err.println("Duplicate account number detected while loading from database: " + e.getMessage());
                    // Continue processing other accounts
                }
            }
            
            System.out.println("Successfully loaded " + accounts.size() + " accounts from database.");
            
        } catch (SQLException e) {
            System.err.println("Database error while retrieving all accounts: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve accounts from database", e);
            
        } finally {
            // Step 6: CRITICAL - Always close resources in finally block
            DatabaseManager.close(conn, pstmt, rs);
        }
        
        return accounts;
    }
    
    /**
     * Retrieves all accounts of a specific type from the database.
     * This method handles the Class Table Inheritance pattern by joining all account tables
     * and filtering by the specified account type.
     * Note: This method retrieves accounts without their associated clients.
     * 
     * @param accountType The type of accounts to retrieve (e.g., "Regular Checking Account", "Business Checking Account", etc.)
     * @return A List of Account objects representing all accounts of the specified type
     * @throws RuntimeException if a database error occurs during the operation
     */
    public static List<Account> getAccountsByType(String accountType) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Account> accounts = new ArrayList<>();
        
        try {
            // Step 1: Get database connection
            conn = DatabaseManager.getConnection();
            
            // Step 2: Prepare the complex SQL statement with WHERE clause
            pstmt = conn.prepareStatement(SELECT_ACCOUNTS_BY_TYPE);
            pstmt.setString(1, accountType); // Set the account type parameter
            
            // Step 3: Execute the query
            rs = pstmt.executeQuery();
            
            // Step 4: Process each row in the ResultSet
            while (rs.next()) {
                // Extract common account data including database ID
                int accountId = rs.getInt("account_id");
                int accountNumber = rs.getInt("account_number");
                String dbAccountType = rs.getString("account_type");
                int bankNumber = rs.getInt("bank_id");
                String managerName = rs.getString("manager_name");
                
                Account account = null;
                
                // Step 5: Use switch statement to instantiate the correct account subclass
                try {
                    switch (dbAccountType) {
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
                            System.err.println("Unknown account type: " + dbAccountType + " for account number: " + accountNumber);
                            continue; // Skip this account and continue with the next one
                    }
                    
                    // If account was successfully created, add it to the list
                    if (account != null) {
                        accounts.add(account);
                        System.out.println("Loaded account: " + dbAccountType + " #" + accountNumber);
                    }
                    
                } catch (DuplicationException e) {
                    // This shouldn't happen when reading from database, but handle it gracefully
                    System.err.println("Duplicate account number detected while loading from database: " + e.getMessage());
                    // Continue processing other accounts
                }
            }
            
            System.out.println("Successfully loaded " + accounts.size() + " accounts of type '" + accountType + "' from database.");
            
        } catch (SQLException e) {
            System.err.println("Database error while retrieving accounts of type '" + accountType + "': " + e.getMessage());
            throw new RuntimeException("Failed to retrieve accounts from database", e);
            
        } finally {
            // Step 6: CRITICAL - Always close resources in finally block
            DatabaseManager.close(conn, pstmt, rs);
        }
        
        return accounts;
    }
    
    /**
     * Retrieves a specific account by its account number.
     * This method handles the Class Table Inheritance pattern by joining all account tables.
     * Note: This method retrieves the account without its associated clients.
     * 
     * @param accountNumber The unique account number to search for
     * @return The Account object if found, null if no account exists with the given number
     * @throws RuntimeException if a database error occurs during the operation
     */
    public static Account getAccountByNumber(int accountNumber) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Account account = null;
        
        try {
            conn = DatabaseManager.getConnection();
            
            // Use the same complex query but with a WHERE clause
            String query = SELECT_ALL_ACCOUNTS.replace("ORDER BY a.account_number", 
                                                      "WHERE a.account_number = ? ORDER BY a.account_number");
            pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, accountNumber);
            
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                // Extract account data including database ID
                int accountId = rs.getInt("account_id");        // NEW: Extract database ID
                String accountType = rs.getString("account_type");
                int bankNumber = rs.getInt("bank_id");
                String managerName = rs.getString("manager_name");
                
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
                            System.err.println("Unknown account type: " + accountType);
                            return null;
                    }
                    
                    System.out.println("Found account: " + accountType + " #" + accountNumber);
                    
                } catch (DuplicationException e) {
                    System.err.println("Duplicate account number detected: " + e.getMessage());
                    return null;
                }
            } else {
                System.out.println("No account found with number: " + accountNumber);
            }
            
        } catch (SQLException e) {
            System.err.println("Database error while retrieving account " + accountNumber + ": " + e.getMessage());
            throw new RuntimeException("Failed to retrieve account from database", e);
            
        } finally {
            DatabaseManager.close(conn, pstmt, rs);
        }
        
        return account;
    }
    
    /**
     * Checks if an account exists in the database with the given account number.
     * 
     * @param accountNumber The account number to check for existence
     * @return true if an account exists with the given number, false otherwise
     */
    public static boolean accountExists(int accountNumber) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            pstmt = conn.prepareStatement("SELECT 1 FROM accounts WHERE account_number = ?");
            pstmt.setInt(1, accountNumber);
            rs = pstmt.executeQuery();
            
            return rs.next();
            
        } catch (SQLException e) {
            System.err.println("Database error while checking account existence for number " + accountNumber + ": " + e.getMessage());
            return false;
            
        } finally {
            DatabaseManager.close(conn, pstmt, rs);
        }
    }
    
    /**
     * Adds a new account to the database using a transaction to ensure data integrity.
     * This method inserts data into both the base accounts table and the appropriate subclass table.
     * 
     * @param newAccount An Account object without a database ID (id should be -1)
     * @return A new Account object containing the original data plus the database-generated ID
     * @throws RuntimeException if a database error occurs during the operation
     * @throws IllegalArgumentException if the account already has a database ID
     */
    public static Account addAccount(Account newAccount) {
        // Validate that this is a new account without a database ID
        if (newAccount.hasId()) {
            throw new IllegalArgumentException("Cannot add account that already has a database ID: " + newAccount.getId());
        }
        
        Connection conn = null;
        PreparedStatement pstmt1 = null; // For accounts table insert
        PreparedStatement pstmt2 = null; // For subclass table insert
        ResultSet rs = null;
        Account accountWithId = null;
        
        try {
            // Step 1: Get database connection and start transaction
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false); // Start transaction
            
            // Step 2: Insert into the base accounts table
            pstmt1 = conn.prepareStatement(INSERT_ACCOUNT);
            pstmt1.setInt(1, newAccount.getAccountNumber());
            pstmt1.setString(2, newAccount.getAccountType());
            pstmt1.setInt(3, newAccount.getBankNumber());
            pstmt1.setString(4, newAccount.getManagerName());
            
            // Execute and get the generated account_id
            rs = pstmt1.executeQuery();
            
            int generatedAccountId;
            if (rs.next()) {
                generatedAccountId = rs.getInt("account_id");
                System.out.println("Inserted into accounts table with ID: " + generatedAccountId);
            } else {
                throw new SQLException("Failed to retrieve generated account ID after insert");
            }
            
            // Step 3: Insert into the appropriate subclass table based on account type
            switch (newAccount.getAccountType()) {
                case "Regular Checking Account":
                    RegularCheckingAccount rca = (RegularCheckingAccount) newAccount;
                    pstmt2 = conn.prepareStatement(INSERT_REGULAR_CHECKING);
                    pstmt2.setInt(1, generatedAccountId);
                    pstmt2.setDouble(2, rca.getCreditLimit());
                    pstmt2.setDouble(3, rca.getProfit());
                    break;
                    
                case "Business Checking Account":
                    BusinessCheckingAccount bca = (BusinessCheckingAccount) newAccount;
                    pstmt2 = conn.prepareStatement(INSERT_BUSINESS_CHECKING);
                    pstmt2.setInt(1, generatedAccountId);
                    pstmt2.setDouble(2, bca.getCreditLimit());
                    pstmt2.setDouble(3, bca.getBusinessRevenue());
                    pstmt2.setDouble(4, bca.getProfit());
                    pstmt2.setDouble(5, bca.getManagementFee());
                    break;
                    
                case "Mortgage Account":
                    MortgageAccount ma = (MortgageAccount) newAccount;
                    pstmt2 = conn.prepareStatement(INSERT_MORTGAGE);
                    pstmt2.setInt(1, generatedAccountId);
                    pstmt2.setDouble(2, ma.getOriginalMortgageAmount());
                    pstmt2.setDouble(3, ma.getMonthlyPayment());
                    pstmt2.setInt(4, ma.getYears());
                    pstmt2.setDouble(5, ma.getProfit());
                    pstmt2.setDouble(6, ma.getManagementFee());
                    break;
                    
                case "Savings Account":
                    SavingsAccount sa = (SavingsAccount) newAccount;
                    pstmt2 = conn.prepareStatement(INSERT_SAVINGS);
                    pstmt2.setInt(1, generatedAccountId);
                    pstmt2.setDouble(2, sa.getDepositAmount());
                    pstmt2.setInt(3, sa.getYears());
                    break;
                    
                default:
                    throw new IllegalArgumentException("Unknown account type: " + newAccount.getAccountType());
            }
            
            // Execute the subclass table insert
            int rowsAffected = pstmt2.executeUpdate();
            if (rowsAffected != 1) {
                throw new SQLException("Failed to insert into subclass table. Rows affected: " + rowsAffected);
            }
            
            System.out.println("Inserted into " + newAccount.getAccountType().toLowerCase().replace(" ", "_") + " table");
            
            // Step 4: Commit the transaction
            conn.commit();
            System.out.println("Transaction committed successfully for account #" + newAccount.getAccountNumber());
            
            // Step 5: Create a new Account object with the database ID
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
                // This shouldn't happen since we just created the account, but handle it
                throw new RuntimeException("Unexpected duplication error when creating account object", e);
            }
            
        } catch (SQLException e) {
            // Step 6: Rollback transaction on any error
            try {
                if (conn != null) {
                    conn.rollback();
                    System.err.println("Transaction rolled back due to error: " + e.getMessage());
                }
            } catch (SQLException rollbackEx) {
                System.err.println("Error during rollback: " + rollbackEx.getMessage());
            }
            
            System.err.println("Database error while adding account #" + newAccount.getAccountNumber() + ": " + e.getMessage());
            throw new RuntimeException("Failed to add account to database", e);
            
        } finally {
            // Step 7: CRITICAL - Reset auto-commit and close resources
            try {
                if (conn != null) {
                    conn.setAutoCommit(true); // Reset auto-commit to default
                }
            } catch (SQLException e) {
                System.err.println("Error resetting auto-commit: " + e.getMessage());
            }
            
            DatabaseManager.close(conn, pstmt2, null);
            DatabaseManager.close(null, pstmt1, rs);
        }
        
        return accountWithId;
    }
    
    /**
     * Associates a client with an account by adding a record to the account_clients table.
     * This method handles the Many-to-Many relationship between accounts and clients.
     * 
     * @param accountId The database ID of the account
     * @param clientId The database ID of the client
     * @throws RuntimeException if a database error occurs during the operation
     * @throws IllegalArgumentException if the association already exists
     */
    public static void addClientToAccount(int accountId, int clientId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            // Step 1: Get database connection
            conn = DatabaseManager.getConnection();
            
            // Step 2: Check if the association already exists
            pstmt = conn.prepareStatement(CHECK_ACCOUNT_CLIENT_EXISTS);
            pstmt.setInt(1, accountId);
            pstmt.setInt(2, clientId);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                throw new IllegalArgumentException("Client ID " + clientId + " is already associated with Account ID " + accountId);
            }
            
            // Step 3: Close the check statement and prepare the insert
            DatabaseManager.close(null, pstmt, rs);
            pstmt = null;
            rs = null;
            
            // Step 4: Insert the new account-client association
            pstmt = conn.prepareStatement(INSERT_ACCOUNT_CLIENT);
            pstmt.setInt(1, accountId);
            pstmt.setInt(2, clientId);
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected == 1) {
                System.out.println("Successfully associated Client ID " + clientId + " with Account ID " + accountId);
            } else {
                throw new SQLException("Failed to insert account-client association. No rows affected.");
            }
            
        } catch (SQLException e) {
            System.err.println("Database error while associating client " + clientId + " with account " + accountId + ": " + e.getMessage());
            throw new RuntimeException("Failed to associate client with account", e);
            
        } finally {
            // Step 5: CRITICAL - Always close resources in finally block
            DatabaseManager.close(conn, pstmt, rs);
        }
    }
    
    /**
     * Checks if a client is already associated with an account.
     * This is a utility method that can be useful for validation.
     * 
     * @param accountId The database ID of the account
     * @param clientId The database ID of the client
     * @return true if the association exists, false otherwise
     */
    public static boolean isClientAssociatedWithAccount(int accountId, int clientId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = DatabaseManager.getConnection();
            pstmt = conn.prepareStatement(CHECK_ACCOUNT_CLIENT_EXISTS);
            pstmt.setInt(1, accountId);
            pstmt.setInt(2, clientId);
            rs = pstmt.executeQuery();
            
            return rs.next(); // Returns true if association exists, false otherwise
            
        } catch (SQLException e) {
            System.err.println("Database error while checking client-account association: " + e.getMessage());
            return false; // Conservative approach - assume doesn't exist if error
            
        } finally {
            DatabaseManager.close(conn, pstmt, rs);
        }
    }

    /**
     * Retrieves all accounts that implement the ProfitAccount interface from the database,
     * ordered by profit in descending order. This excludes SavingsAccount objects.
     * This method handles the Class Table Inheritance pattern by joining all account tables.
     * Note: This method retrieves accounts without their associated clients.
     * 
     * @return A List of Account objects representing all profit accounts, ordered by profit (highest first)
     * @throws RuntimeException if a database error occurs during the operation
     */
    public static List<Account> getProfitAccounts() {
        // SQL query to retrieve profit accounts with LEFT JOINs for Class Table Inheritance
        final String SELECT_PROFIT_ACCOUNTS = 
            "SELECT " +
            // Base account fields
            "a.account_id, a.account_number, a.account_type, a.date_opened, " +
            "a.bank_id, a.balance, a.manager_name, " +
            // Regular checking account fields
            "rca.credit_limit as rca_credit_limit, rca.profit as rca_profit, " +
            // Business checking account fields  
            "bca.credit_limit as bca_credit_limit, bca.business_revenue, " +
            "bca.profit as bca_profit, bca.management_fee as bca_management_fee, " +
            // Mortgage account fields
            "ma.original_mortgage_amount, ma.monthly_payment, ma.years as ma_years, " +
            "ma.profit as ma_profit, ma.management_fee as ma_management_fee, " +
            // Savings account fields (included for consistency but will be filtered out)
            "sa.deposit_amount, sa.years as sa_years " +
            "FROM accounts a " +
            "LEFT JOIN regular_checking_accounts rca ON a.account_id = rca.account_id " +
            "LEFT JOIN business_checking_accounts bca ON a.account_id = bca.account_id " +
            "LEFT JOIN mortgage_accounts ma ON a.account_id = ma.account_id " +
            "LEFT JOIN savings_accounts sa ON a.account_id = sa.account_id " +
            "WHERE a.account_type != 'Savings Account' " +
            "ORDER BY COALESCE(rca.profit, bca.profit, ma.profit) DESC";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Account> accounts = new ArrayList<>();
        
        try {
            // Step 1: Get database connection
            conn = DatabaseManager.getConnection();
            
            // Step 2: Prepare the SQL statement
            pstmt = conn.prepareStatement(SELECT_PROFIT_ACCOUNTS);
            
            // Step 3: Execute the query
            rs = pstmt.executeQuery();
            
            // Step 4: Process each row in the ResultSet
            while (rs.next()) {
                // Extract common account data including database ID
                int accountId = rs.getInt("account_id");
                int accountNumber = rs.getInt("account_number");
                String accountType = rs.getString("account_type");
                int bankNumber = rs.getInt("bank_id");
                String managerName = rs.getString("manager_name");
                
                Account account = null;
                
                // Step 5: Use switch statement to instantiate the correct account subclass
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
                            
                        default:
                            System.err.println("Unknown account type: " + accountType + " for account number: " + accountNumber);
                            continue; // Skip this account and continue with the next one
                    }
                    
                    // If account was successfully created, add it to the list
                    if (account != null) {
                        accounts.add(account);
                        System.out.println("Loaded profit account: " + accountType + " #" + accountNumber + " (Profit: " + ((ProfitAccount) account).getProfit() + ")");
                    }
                    
                } catch (DuplicationException e) {
                    // This shouldn't happen when reading from database, but handle it gracefully
                    System.err.println("Duplicate account number detected while loading from database: " + e.getMessage());
                    // Continue processing other accounts
                }
            }
            
            System.out.println("Successfully loaded " + accounts.size() + " profit accounts from database, ordered by profit (highest first).");
            
        } catch (SQLException e) {
            System.err.println("Database error while retrieving profit accounts: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve profit accounts from database", e);
            
        } finally {
            // Step 6: CRITICAL - Always close resources in finally block
            DatabaseManager.close(conn, pstmt, rs);
        }
        
        return accounts;
    }

    /**
     * Retrieves all accounts that implement the ManagementFeeAccount interface from the database.
     * This includes Business Checking Accounts and Mortgage Accounts.
     * This method handles the Class Table Inheritance pattern by joining all account tables.
     * Note: This method retrieves accounts without their associated clients.
     * 
     * @return A List of Account objects representing all management fee accounts
     * @throws RuntimeException if a database error occurs during the operation
     */
    public static List<Account> getFeeAccounts() {
        // SQL query to retrieve fee accounts with LEFT JOINs for Class Table Inheritance
        final String SELECT_FEE_ACCOUNTS = 
            "SELECT " +
            // Base account fields
            "a.account_id, a.account_number, a.account_type, a.date_opened, " +
            "a.bank_id, a.balance, a.manager_name, " +
            // Regular checking account fields
            "rca.credit_limit as rca_credit_limit, rca.profit as rca_profit, " +
            // Business checking account fields  
            "bca.credit_limit as bca_credit_limit, bca.business_revenue, " +
            "bca.profit as bca_profit, bca.management_fee as bca_management_fee, " +
            // Mortgage account fields
            "ma.original_mortgage_amount, ma.monthly_payment, ma.years as ma_years, " +
            "ma.profit as ma_profit, ma.management_fee as ma_management_fee, " +
            // Savings account fields (included for consistency but will be filtered out)
            "sa.deposit_amount, sa.years as sa_years " +
            "FROM accounts a " +
            "LEFT JOIN regular_checking_accounts rca ON a.account_id = rca.account_id " +
            "LEFT JOIN business_checking_accounts bca ON a.account_id = bca.account_id " +
            "LEFT JOIN mortgage_accounts ma ON a.account_id = ma.account_id " +
            "LEFT JOIN savings_accounts sa ON a.account_id = sa.account_id " +
            "WHERE a.account_type = 'Business Checking Account' OR a.account_type = 'Mortgage Account' " +
            "ORDER BY a.account_number";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Account> accounts = new ArrayList<>();
        
        try {
            // Step 1: Get database connection
            conn = DatabaseManager.getConnection();
            
            // Step 2: Prepare the SQL statement
            pstmt = conn.prepareStatement(SELECT_FEE_ACCOUNTS);
            
            // Step 3: Execute the query
            rs = pstmt.executeQuery();
            
            // Step 4: Process each row in the ResultSet
            while (rs.next()) {
                // Extract common account data including database ID
                int accountId = rs.getInt("account_id");
                int accountNumber = rs.getInt("account_number");
                String accountType = rs.getString("account_type");
                int bankNumber = rs.getInt("bank_id");
                String managerName = rs.getString("manager_name");
                
                Account account = null;
                
                // Step 5: Use switch statement to instantiate the correct account subclass
                try {
                    switch (accountType) {
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
                            
                        default:
                            System.err.println("Unknown account type: " + accountType + " for account number: " + accountNumber);
                            continue; // Skip this account and continue with the next one
                    }
                    
                    // If account was successfully created, add it to the list
                    if (account != null) {
                        accounts.add(account);
                        System.out.println("Loaded fee account: " + accountType + " #" + accountNumber + " (Fee: " + ((ManagementFeeAccount) account).getManagementFee() + ")");
                    }
                    
                } catch (DuplicationException e) {
                    // This shouldn't happen when reading from database, but handle it gracefully
                    System.err.println("Duplicate account number detected while loading from database: " + e.getMessage());
                    // Continue processing other accounts
                }
            }
            
            System.out.println("Successfully loaded " + accounts.size() + " management fee accounts from database.");
            
        } catch (SQLException e) {
            System.err.println("Database error while retrieving management fee accounts: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve management fee accounts from database", e);
            
        } finally {
            // Step 6: CRITICAL - Always close resources in finally block
            DatabaseManager.close(conn, pstmt, rs);
        }
        
        return accounts;
    }

    /**
     * Retrieves the top checking account by profit from the database.
     * This includes both Regular Checking Accounts and Business Checking Accounts.
     * This method handles the Class Table Inheritance pattern by joining all account tables.
     * Note: This method retrieves the account without its associated clients.
     * 
     * @return The Account object with the highest profit among checking accounts, or null if none found
     * @throws RuntimeException if a database error occurs during the operation
     */
    public static Account getTopCheckingAccountByProfit() {
        // SQL query to retrieve the top checking account by profit
        final String SELECT_TOP_CHECKING = 
            "SELECT " +
            // Base account fields
            "a.account_id, a.account_number, a.account_type, a.date_opened, " +
            "a.bank_id, a.balance, a.manager_name, " +
            // Regular checking account fields
            "rca.credit_limit as rca_credit_limit, rca.profit as rca_profit, " +
            // Business checking account fields  
            "bca.credit_limit as bca_credit_limit, bca.business_revenue, " +
            "bca.profit as bca_profit, bca.management_fee as bca_management_fee, " +
            // Mortgage account fields
            "ma.original_mortgage_amount, ma.monthly_payment, ma.years as ma_years, " +
            "ma.profit as ma_profit, ma.management_fee as ma_management_fee, " +
            // Savings account fields (included for consistency but will be filtered out)
            "sa.deposit_amount, sa.years as sa_years " +
            "FROM accounts a " +
            "LEFT JOIN regular_checking_accounts rca ON a.account_id = rca.account_id " +
            "LEFT JOIN business_checking_accounts bca ON a.account_id = bca.account_id " +
            "LEFT JOIN mortgage_accounts ma ON a.account_id = ma.account_id " +
            "LEFT JOIN savings_accounts sa ON a.account_id = sa.account_id " +
            "WHERE a.account_type LIKE '%Checking Account' " +
            "ORDER BY COALESCE(rca.profit, bca.profit) DESC " +
            "LIMIT 1";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Account account = null;
        
        try {
            // Step 1: Get database connection
            conn = DatabaseManager.getConnection();
            
            // Step 2: Prepare the SQL statement
            pstmt = conn.prepareStatement(SELECT_TOP_CHECKING);
            
            // Step 3: Execute the query
            rs = pstmt.executeQuery();
            
            // Step 4: Process the result if found
            if (rs.next()) {
                // Extract common account data including database ID
                int accountId = rs.getInt("account_id");
                int accountNumber = rs.getInt("account_number");
                String accountType = rs.getString("account_type");
                int bankNumber = rs.getInt("bank_id");
                String managerName = rs.getString("manager_name");
                
                // Step 5: Use switch statement to instantiate the correct account subclass
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
                            
                        default:
                            System.err.println("Unknown checking account type: " + accountType + " for account number: " + accountNumber);
                            return null;
                    }
                    
                    if (account != null) {
                        System.out.println("Found top checking account: " + accountType + " #" + accountNumber + " (Profit: " + ((ProfitAccount) account).getProfit() + ")");
                    }
                    
                } catch (DuplicationException e) {
                    // This shouldn't happen when reading from database, but handle it gracefully
                    System.err.println("Duplicate account number detected while loading from database: " + e.getMessage());
                    return null;
                }
            } else {
                System.out.println("No checking accounts found in database.");
            }
            
        } catch (SQLException e) {
            System.err.println("Database error while retrieving top checking account: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve top checking account from database", e);
            
        } finally {
            // Step 6: CRITICAL - Always close resources in finally block
            DatabaseManager.close(conn, pstmt, rs);
        }
        
        return account;
    }

    /**
     * Removes a client from an account by deleting the association record from the account_clients table.
     * This method handles the Many-to-Many relationship between accounts and clients by performing a DELETE operation.
     * 
     * @param accountId The database ID of the account
     * @param clientId The database ID of the client to remove
     * @return true if exactly one association was deleted, false otherwise
     * @throws RuntimeException if a database error occurs during the operation
     */
    public static boolean removeClientFromAccount(int accountId, int clientId) {
        // SQL query to delete from the linking table
        final String DELETE_ACCOUNT_CLIENT = "DELETE FROM account_clients WHERE account_id = ? AND client_id = ?";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            // Step 1: Get database connection
            conn = DatabaseManager.getConnection();
            
            // Step 2: Prepare the DELETE statement
            pstmt = conn.prepareStatement(DELETE_ACCOUNT_CLIENT);
            pstmt.setInt(1, accountId);
            pstmt.setInt(2, clientId);
            
            // Step 3: Execute the delete
            int rowsAffected = pstmt.executeUpdate();
            
            // Step 4: Check if exactly one row was deleted
            if (rowsAffected == 1) {
                success = true;
                System.out.println("Successfully removed Client ID " + clientId + " from Account ID " + accountId);
            } else if (rowsAffected == 0) {
                System.err.println("No association found between Account ID " + accountId + " and Client ID " + clientId + " to remove.");
            } else {
                System.err.println("Unexpected: " + rowsAffected + " rows affected when removing client-account association.");
            }
            
        } catch (SQLException e) {
            System.err.println("Database error while removing client " + clientId + " from account " + accountId + ": " + e.getMessage());
            throw new RuntimeException("Failed to remove client from account", e);
            
        } finally {
            // Step 5: CRITICAL - Always close resources in finally block
            DatabaseManager.close(conn, pstmt, null);
        }
        
        return success;
    }
}