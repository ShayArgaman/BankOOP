import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * AccountDAO מעודכן לשימוש ב-account_number כמפתח ראשי
 * השינויים העיקריים:
 * 1. הסרת כל השימושים ב-account_id
 * 2. שימוש ב-account_number בכל ה-foreign keys
 * 3. עדכון השאילתות בהתאם
 */
public class AccountDAO {
    
    // שאילתות מעודכנות - בלי account_id
    private static final String SELECT_ALL_ACCOUNTS = 
        "SELECT * FROM v_all_account_details ORDER BY account_number";
    
    private static final String SELECT_ACCOUNTS_BY_TYPE = 
        "SELECT * FROM v_all_account_details WHERE account_type = ? ORDER BY account_number";
    
    private static final String SELECT_ACCOUNT_BY_NUMBER = 
        "SELECT * FROM v_all_account_details WHERE account_number = ?";
    
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
    
    // שאילתות עדכון - עם account_number במקום account_id
    private static final String INSERT_ACCOUNT_CLIENT = 
        "INSERT INTO account_clients (account_number, client_id) VALUES (?, ?)";
    
    private static final String CHECK_ACCOUNT_CLIENT_EXISTS = 
        "SELECT 1 FROM account_clients WHERE account_number = ? AND client_id = ?";
    
    private static final String INSERT_ACCOUNT = 
        "INSERT INTO accounts (account_number, account_type, date_opened, bank_id, balance, manager_name) " +
        "VALUES (?, ?, CURRENT_DATE, ?, 20.00, ?)";
    
    private static final String INSERT_REGULAR_CHECKING = 
        "INSERT INTO regular_checking_accounts (account_number, credit_limit, profit) VALUES (?, ?, ?)";
    
    private static final String INSERT_BUSINESS_CHECKING = 
        "INSERT INTO business_checking_accounts (account_number, credit_limit, business_revenue, profit, management_fee) " +
        "VALUES (?, ?, ?, ?, ?)";
    
    private static final String INSERT_MORTGAGE = 
        "INSERT INTO mortgage_accounts (account_number, original_mortgage_amount, monthly_payment, years, profit, management_fee) " +
        "VALUES (?, ?, ?, ?, ?, ?)";
    
    private static final String INSERT_SAVINGS = 
        "INSERT INTO savings_accounts (account_number, deposit_amount, years) VALUES (?, ?, ?)";

    private static final String DELETE_ACCOUNT_CLIENT = 
        "DELETE FROM account_clients WHERE account_number = ? AND client_id = ?";
    
    private static final String CHECK_ACCOUNT_EXISTS = 
        "SELECT 1 FROM accounts WHERE account_number = ?";

    // ========== PUBLIC WRAPPER METHODS ==========

    public static List<Account> getAllAccounts() {
        try (Connection conn = DatabaseManager.getConnection()) {
            return getAllAccounts(conn);
        } catch (SQLException e) {
            System.err.println("Connection error in getAllAccounts: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve accounts from database", e);
        }
    }

    public static Account getAccountByNumber(int accountNumber) {
        try (Connection conn = DatabaseManager.getConnection()) {
            return getAccountByNumber(conn, accountNumber);
        } catch (SQLException e) {
            System.err.println("Connection error in getAccountByNumber: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve account from database", e);
        }
    }

    public static boolean accountExists(int accountNumber) {
        try (Connection conn = DatabaseManager.getConnection()) {
            return accountExists(conn, accountNumber);
        } catch (SQLException e) {
            System.err.println("Connection error in accountExists: " + e.getMessage());
            return false;
        }
    }

    public static Account addAccount(Account newAccount) {
        try (Connection conn = DatabaseManager.getConnection()) {
            return addAccount(conn, newAccount);
        } catch (SQLException e) {
            System.err.println("Connection error in addAccount: " + e.getMessage());
            throw new RuntimeException("Failed to add account to database", e);
        }
    }

    /**
     * מתודה מעודכנת - מקבלת account_number במקום account_id
     */
    public static void addClientToAccount(int accountNumber, int clientId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            addClientToAccount(conn, accountNumber, clientId);
        } catch (SQLException e) {
            System.err.println("Connection error in addClientToAccount: " + e.getMessage());
            throw new RuntimeException("Failed to associate client with account", e);
        }
    }

    public static boolean removeClientFromAccount(int accountNumber, int clientId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            return removeClientFromAccount(conn, accountNumber, clientId);
        } catch (SQLException e) {
            System.err.println("Connection error in removeClientFromAccount: " + e.getMessage());
            throw new RuntimeException("Failed to remove client from account", e);
        }
    }

    // ========== PRIVATE METHODS ==========

    private static Account addAccount(Connection conn, Account newAccount) throws SQLException {
        PreparedStatement pstmt1 = null;
        PreparedStatement pstmt2 = null;
        
        try {
            conn.setAutoCommit(false);
            
            // הכנסה לטבלת accounts - בלי RETURNING (כי אנחנו משתמשים ב-natural key)
            pstmt1 = conn.prepareStatement(INSERT_ACCOUNT);
            pstmt1.setInt(1, newAccount.getAccountNumber());
            pstmt1.setString(2, newAccount.getAccountType());
            pstmt1.setInt(3, newAccount.getBankNumber());
            pstmt1.setString(4, newAccount.getManagerName());
            
            int rowsAffected = pstmt1.executeUpdate();
            if (rowsAffected != 1) {
                throw new SQLException("Failed to insert into accounts table");
            }
            
            // הכנסה לטבלת subclass
            pstmt2 = createSubclassInsertStatement(conn, newAccount);
            rowsAffected = pstmt2.executeUpdate();
            if (rowsAffected != 1) {
                throw new SQLException("Failed to insert into subclass table");
            }
            
            conn.commit();
            System.out.println("Account created successfully: " + newAccount.getAccountNumber());
            
            return newAccount; // מחזירים את אותו אובייקט (בלי שינוי ID)
            
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
            }
            DatabaseManager.close(null, pstmt2, null);
            DatabaseManager.close(null, pstmt1, null);
        }
    }

    /**
     * מתודה מעודכנת - משתמשת ב-account_number
     */
    private static void addClientToAccount(Connection conn, int accountNumber, int clientId) throws SQLException {
        PreparedStatement pstmt = null;
        
        try {
            pstmt = conn.prepareStatement(INSERT_ACCOUNT_CLIENT);
            pstmt.setInt(1, accountNumber);  // השינוי העיקרי: account_number במקום account_id
            pstmt.setInt(2, clientId);
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected == 1) {
                System.out.println("Successfully associated Client ID " + clientId + " with Account #" + accountNumber);
            } else {
                throw new SQLException("Failed to insert account-client association");
            }
            
        } finally {
            DatabaseManager.close(null, pstmt, null);
        }
    }

    private static Account createAccountFromResultSet(ResultSet rs) throws SQLException {
        int accountNumber = rs.getInt("account_number");
        String accountType = rs.getString("account_type");
        int bankNumber = rs.getInt("bank_id");
        String managerName = rs.getString("manager_name");
        
        Account account = null;
        
        try {
            switch (accountType) {
                case "Regular Checking Account":
                    double rcaCreditLimit = rs.getDouble("rca_credit_limit");
                    account = new RegularCheckingAccount(accountNumber, bankNumber, managerName, rcaCreditLimit);
                    break;
                    
                case "Business Checking Account":
                    double bcaCreditLimit = rs.getDouble("bca_credit_limit");
                    double businessRevenue = rs.getDouble("business_revenue");
                    account = new BusinessCheckingAccount(accountNumber, bankNumber, managerName, bcaCreditLimit, businessRevenue);
                    break;
                    
                case "Mortgage Account":
                    double originalMortgageAmount = rs.getDouble("original_mortgage_amount");
                    double monthlyPayment = rs.getDouble("monthly_payment");
                    int mortgageYears = rs.getInt("ma_years");
                    account = new MortgageAccount(accountNumber, bankNumber, managerName, originalMortgageAmount, monthlyPayment, mortgageYears);
                    break;
                    
                case "Savings Account":
                    double depositAmount = rs.getDouble("deposit_amount");
                    int savingsYears = rs.getInt("sa_years");
                    account = new SavingsAccount(accountNumber, bankNumber, managerName, depositAmount, savingsYears);
                    break;
                    
                default:
                    System.err.println("Unknown account type: " + accountType);
                    return null;
            }
            
        } catch (DuplicationException e) {
            System.err.println("Duplicate account number: " + e.getMessage());
            return null;
        }
        
        return account;
    }

    private static PreparedStatement createSubclassInsertStatement(Connection conn, Account newAccount) throws SQLException {
        PreparedStatement pstmt = null;
        
        switch (newAccount.getAccountType()) {
            case "Regular Checking Account":
                RegularCheckingAccount rca = (RegularCheckingAccount) newAccount;
                pstmt = conn.prepareStatement(INSERT_REGULAR_CHECKING);
                pstmt.setInt(1, newAccount.getAccountNumber());  // השינוי: account_number במקום account_id
                pstmt.setDouble(2, rca.getCreditLimit());
                pstmt.setDouble(3, rca.getProfit());
                break;
                
            case "Business Checking Account":
                BusinessCheckingAccount bca = (BusinessCheckingAccount) newAccount;
                pstmt = conn.prepareStatement(INSERT_BUSINESS_CHECKING);
                pstmt.setInt(1, newAccount.getAccountNumber());
                pstmt.setDouble(2, bca.getCreditLimit());
                pstmt.setDouble(3, bca.getBusinessRevenue());
                pstmt.setDouble(4, bca.getProfit());
                pstmt.setDouble(5, bca.getManagementFee());
                break;
                
            // המשך עבור Mortgage ו-Savings...
        }
        
        return pstmt;
    }

    // שאר המתודות עם שינויים דומים...
}