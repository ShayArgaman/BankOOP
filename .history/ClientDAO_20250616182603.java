import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * ClientDAO (Data Access Object) for all database operations on the 'clients' table.
 * Refactored to use Unit of Work pattern with connection management.
 */
public class ClientDAO {

    private static final String SELECT_CLIENT_BY_ID = "SELECT client_id, name, rank_value FROM clients WHERE client_id = ?";
    private static final String INSERT_CLIENT = "INSERT INTO clients (name, rank_value) VALUES (?, ?) RETURNING client_id";
    private static final String SELECT_ALL_CLIENTS = "SELECT client_id, name, rank_value FROM clients ORDER BY client_id";
    private static final String CHECK_CLIENT_EXISTS = "SELECT 1 FROM clients WHERE client_id = ?";
    private static final String SELECT_CLIENTS_FOR_ACCOUNT = "SELECT c.client_id, c.name, c.rank_value FROM clients c JOIN account_clients ac ON c.client_id = ac.client_id WHERE ac.account_id = ?";
    private static final String UPDATE_CLIENT_RANK = "UPDATE clients SET rank_value = ? WHERE client_id = ?";
    private static final String CHECK_CLIENT_ASSOCIATED_WITH_ANY_ACCOUNT = "SELECT 1 FROM account_clients WHERE client_id = ? LIMIT 1";
    private static final String DELETE_CLIENT_BY_ID = "DELETE FROM clients WHERE client_id = ?";

    // ========== PUBLIC WRAPPER METHODS (for backward compatibility) ==========

    /**
     * Public wrapper for retrieving a client by their ID.
     */
    public static Client getClientById(int clientId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            return getClientById(conn, clientId);
        } catch (SQLException e) {
            System.err.println("Connection error in getClientById: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve client.", e);
        }
    }

    /**
     * Public wrapper for adding a new client to the database.
     */
    public static Client addClient(Client newClient) {
        try (Connection conn = DatabaseManager.getConnection()) {
            return addClient(conn, newClient);
        } catch (SQLException e) {
            System.err.println("Connection error in addClient: " + e.getMessage());
            throw new RuntimeException("Failed to add client.", e);
        }
    }

    /**
     * Public wrapper for retrieving all clients from the database.
     */
    public static List<Client> getAllClients() {
        try (Connection conn = DatabaseManager.getConnection()) {
            return getAllClients(conn);
        } catch (SQLException e) {
            System.err.println("Connection error in getAllClients: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve all clients.", e);
        }
    }

    /**
     * Public wrapper for checking if a client exists.
     */
    public static boolean clientExists(int clientId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            return clientExists(conn, clientId);
        } catch (SQLException e) {
            System.err.println("Connection error in clientExists: " + e.getMessage());
            return false;
        }
    }

    /**
     * Public wrapper for retrieving all clients associated with a specific account.
     */
    public static List<Client> getClientsForAccount(int accountId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            return getClientsForAccount(conn, accountId);
        } catch (SQLException e) {
            System.err.println("Connection error in getClientsForAccount: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve clients for account", e);
        }
    }

    /**
     * Public wrapper for updating a client's rank in the database.
     */
    public static boolean updateClientRank(int clientId, int newRank) {
        try (Connection conn = DatabaseManager.getConnection()) {
            return updateClientRank(conn, clientId, newRank);
        } catch (SQLException e) {
            System.err.println("Connection error in updateClientRank: " + e.getMessage());
            throw new RuntimeException("Failed to update client rank", e);
        }
    }

    /**
     * Public wrapper for checking if a client is associated with any account.
     */
    public static boolean isClientAssociatedWithAnyAccount(int clientId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            return isClientAssociatedWithAnyAccount(conn, clientId);
        } catch (SQLException e) {
            System.err.println("Connection error in isClientAssociatedWithAnyAccount: " + e.getMessage());
            return false;
        }
    }

    /**
     * Public wrapper for deleting a client by their ID.
     */
    public static boolean deleteClientById(int clientId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            return deleteClientById(conn, clientId);
        } catch (SQLException e) {
            System.err.println("Connection error in deleteClientById: " + e.getMessage());
            throw new RuntimeException("Failed to delete client", e);
        }
    }

    // ========== PACKAGE-PRIVATE METHODS (accept Connection parameter) ==========

    /**
     * Retrieves a client by their ID using provided connection.
     */
    static Client getClientById(Connection conn, int clientId) throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Client client = null;

        try {
            pstmt = conn.prepareStatement(SELECT_CLIENT_BY_ID);
            pstmt.setInt(1, clientId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                int dbClientId = rs.getInt("client_id");
                String name = rs.getString("name");
                int rankValue = rs.getInt("rank_value");
                client = new Client(dbClientId, name, rankValue);
            }
        } finally {
            DatabaseManager.close(null, pstmt, rs);
        }
        return client;
    }

    /**
     * Adds a new client to the database using provided connection.
     */
    static Client addClient(Connection conn, Client newClient) throws SQLException {
        if (newClient.getId() != -1) {
            throw new IllegalArgumentException("Client already has a database ID: " + newClient.getId());
        }

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Client clientWithId = null;

        try {
            pstmt = conn.prepareStatement(INSERT_CLIENT);
            pstmt.setString(1, newClient.getName());
            pstmt.setInt(2, newClient.getRank());
            rs = pstmt.executeQuery();

            if (rs.next()) {
                int generatedId = rs.getInt("client_id");
                clientWithId = new Client(generatedId, newClient.getName(), newClient.getRank());
            } else {
                throw new SQLException("Failed to retrieve generated client ID.");
            }
        } finally {
            DatabaseManager.close(null, pstmt, rs);
        }
        return clientWithId;
    }

    /**
     * Retrieves all clients from the database using provided connection.
     */
    static List<Client> getAllClients(Connection conn) throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Client> clients = new ArrayList<>();

        try {
            pstmt = conn.prepareStatement(SELECT_ALL_CLIENTS);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                int dbClientId = rs.getInt("client_id");
                String name = rs.getString("name");
                int rankValue = rs.getInt("rank_value");
                clients.add(new Client(dbClientId, name, rankValue));
            }
        } finally {
            DatabaseManager.close(null, pstmt, rs);
        }
        return clients;
    }

    /**
     * Checks if a client exists using provided connection.
     */
    static boolean clientExists(Connection conn, int clientId) throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(CHECK_CLIENT_EXISTS);
            pstmt.setInt(1, clientId);
            rs = pstmt.executeQuery();
            return rs.next();
        } finally {
            DatabaseManager.close(null, pstmt, rs);
        }
    }

    /**
     * Retrieves all clients associated with a specific account using provided connection.
     */
    static List<Client> getClientsForAccount(Connection conn, int accountId) throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Client> clients = new ArrayList<>();

        try {
            pstmt = conn.prepareStatement(SELECT_CLIENTS_FOR_ACCOUNT);
            pstmt.setInt(1, accountId);
            
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                int clientId = rs.getInt("client_id");
                String name = rs.getString("name");
                int rankValue = rs.getInt("rank_value");
                
                Client client = new Client(clientId, name, rankValue);
                clients.add(client);
                
                System.out.println("Loaded client for account " + accountId + ": " + client.toString());
            }
            
            System.out.println("Successfully loaded " + clients.size() + " clients for account ID " + accountId);
            
        } finally {
            DatabaseManager.close(null, pstmt, rs);
        }
        
        return clients;
    }

    /**
     * Updates a client's rank in the database using provided connection.
     */
    static boolean updateClientRank(Connection conn, int clientId, int newRank) throws SQLException {
        if (newRank < 0 || newRank > 10) {
            System.err.println("Invalid rank value: " + newRank + ". Rank must be between 0 and 10.");
            return false;
        }
        
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            pstmt = conn.prepareStatement(UPDATE_CLIENT_RANK);
            pstmt.setInt(1, newRank);
            pstmt.setInt(2, clientId);
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected == 1) {
                success = true;
                System.out.println("Successfully updated rank for Client ID " + clientId + " to " + newRank);
            } else if (rowsAffected == 0) {
                System.err.println("No client found with ID " + clientId + " to update.");
            } else {
                System.err.println("Unexpected: " + rowsAffected + " rows affected when updating Client ID " + clientId);
            }
            
        } finally {
            DatabaseManager.close(null, pstmt, null);
        }
        
        return success;
    }

    /**
     * Checks if a client is associated with any account using provided connection.
     */
    static boolean isClientAssociatedWithAnyAccount(Connection conn, int clientId) throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            pstmt = conn.prepareStatement(CHECK_CLIENT_ASSOCIATED_WITH_ANY_ACCOUNT);
            pstmt.setInt(1, clientId);
            rs = pstmt.executeQuery();
            
            return rs.next(); // Returns true if client is associated with any account
            
        } finally {
            DatabaseManager.close(null, pstmt, rs);
        }
    }

    /**
     * Deletes a client by their ID using provided connection.
     */
    static boolean deleteClientById(Connection conn, int clientId) throws SQLException {
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            pstmt = conn.prepareStatement(DELETE_CLIENT_BY_ID);
            pstmt.setInt(1, clientId);
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected == 1) {
                success = true;
                System.out.println("Successfully deleted Client ID " + clientId + " from database");
            } else if (rowsAffected == 0) {
                System.err.println("No client found with ID " + clientId + " to delete.");
            } else {
                System.err.println("Unexpected: " + rowsAffected + " rows affected when deleting Client ID " + clientId);
            }
            
        } finally {
            DatabaseManager.close(null, pstmt, null);
        }
        
        return success;
    }
}