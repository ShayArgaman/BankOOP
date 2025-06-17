import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DatabaseManager class using Singleton design pattern to manage PostgreSQL database connections.
 * This class provides a centralized way to establish database connections and clean up resources.
 * 
 * @author Your Name
 * @version 1.0
 */
public class DatabaseManager {
    
    // Singleton instance - only one DatabaseManager instance will exist
    private static DatabaseManager instance;
    
    // Database connection configuration - UPDATE THESE VALUES FOR YOUR ENVIRONMENT
    private static final String DB_URL = "jdbc:postgresql://localhost:5433/bank_project_db";
    private static final String DB_USERNAME = "postgres";
    private static final String DB_PASSWORD = "sa8294";
    private static final String DB_DRIVER = "org.postgresql.Driver";
    
    /**
     * Private constructor to prevent direct instantiation (Singleton pattern).
     * Loads the PostgreSQL JDBC driver when the class is first instantiated.
     * 
     * @throws RuntimeException if the PostgreSQL JDBC driver is not found
     */
    private DatabaseManager() {
        try {
            // Load the PostgreSQL JDBC driver
            Class.forName(DB_DRIVER);
            System.out.println("PostgreSQL JDBC Driver loaded successfully.");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL JDBC Driver not found. " +
                    "Please ensure postgresql-XX.X.jar is in your classpath.", e);
        }
    }
    
    /**
     * Returns the singleton instance of DatabaseManager.
     * Thread-safe implementation using synchronized keyword.
     * 
     * @return The single DatabaseManager instance
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }
    
    /**
     * Establishes and returns a new database connection.
     * Each call creates a fresh connection - remember to close it when done!
     * 
     * @return An active database connection
     * @throws SQLException if the connection cannot be established
     */
    public static Connection getConnection() throws SQLException {
        try {
            Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
            System.out.println("Database connection established successfully.");
            return connection;
        } catch (SQLException e) {
            System.err.println("Failed to establish database connection: " + e.getMessage());
            throw new SQLException("Unable to connect to database. Please check your connection settings.", e);
        }
    }
    
    /**
     * Safely closes database resources in the correct order to prevent resource leaks.
     * This method handles null values gracefully - you can pass null for any parameter.
     * 
     * Resource closing order: ResultSet -> PreparedStatement -> Connection
     * 
     * @param conn The database connection to close (can be null)
     * @param pstmt The prepared statement to close (can be null)
     * @param rs The result set to close (can be null)
     */
    public static void close(Connection conn, PreparedStatement pstmt, ResultSet rs) {
        // Close ResultSet first (innermost resource)
        if (rs != null) {
            try {
                rs.close();
                System.out.println("ResultSet closed successfully.");
            } catch (SQLException e) {
                System.err.println("Error closing ResultSet: " + e.getMessage());
            }
        }
        
        // Close PreparedStatement second
        if (pstmt != null) {
            try {
                pstmt.close();
                System.out.println("PreparedStatement closed successfully.");
            } catch (SQLException e) {
                System.err.println("Error closing PreparedStatement: " + e.getMessage());
            }
        }
        
        // Close Connection last (outermost resource)
        if (conn != null) {
            try {
                conn.close();
                System.out.println("Database connection closed successfully.");
            } catch (SQLException e) {
                System.err.println("Error closing Connection: " + e.getMessage());
            }
        }
    }
    
    /**
     * Overloaded close method for when you only need to close Connection and PreparedStatement.
     * 
     * @param conn The database connection to close (can be null)
     * @param pstmt The prepared statement to close (can be null)
     */
    public static void close(Connection conn, PreparedStatement pstmt) {
        close(conn, pstmt, null);
    }
    
    /**
     * Overloaded close method for when you only need to close a Connection.
     * 
     * @param conn The database connection to close (can be null)
     */
    public static void close(Connection conn) {
        close(conn, null, null);
    }
    
    /**
     * Tests the database connection to verify connectivity.
     * Useful for debugging connection issues.
     * 
     * @return true if connection test is successful, false otherwise
     */
    public static boolean testConnection() {
        Connection conn = null;
        try {
            conn = getConnection();
            // If we get here, connection was successful
            return true;
        } catch (SQLException e) {
            System.err.println("Connection test failed: " + e.getMessage());
            return false;
        } finally {
            close(conn);
        }
    }
    
    /**
     * Returns the current database URL (useful for debugging).
     * 
     * @return The database URL being used
     */
    public static String getDatabaseUrl() {
        return DB_URL;
    }
    
    /**
     * Returns the current database username (useful for debugging).
     * Password is not exposed for security reasons.
     * 
     * @return The database username being used
     */
    public static String getDatabaseUsername() {
        return DB_USERNAME;
    }
}