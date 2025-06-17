import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * ClientDAO (Data Access Object) for all database operations on the 'clients' table.
 */
public class ClientDAO {

    private static final String SELECT_CLIENT_BY_ID = "SELECT client_id, name, rank_value FROM clients WHERE client_id = ?";
    private static final String INSERT_CLIENT = "INSERT INTO clients (name, rank_value) VALUES (?, ?) RETURNING client_id";
    private static final String SELECT_ALL_CLIENTS = "SELECT client_id, name, rank_value FROM clients ORDER BY client_id";
    private static final String CHECK_CLIENT_EXISTS = "SELECT 1 FROM clients WHERE client_id = ?";
    private static final String SELECT_CLIENTS_FOR_ACCOUNT = "SELECT c.client_id, c.name, c.rank_value FROM clients c JOIN account_clients ac ON c.client_id = ac.client_id WHERE ac.account_id = ?";
    private static final String UPDATE_CLIENT_RANK = "UPDATE clients SET rank_value = ? WHERE client_id = ?";

    /**
     * Retrieves a client by their ID.
     * @param clientId The ID of the client.
     * @return A Client object if found, otherwise null.
     */
    public static Client getClientById(int clientId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Client client = null;

        try {
            conn = DatabaseManager.getConnection();
            pstmt = conn.prepareStatement(SELECT_CLIENT_BY_ID);
            pstmt.setInt(1, clientId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                int dbClientId = rs.getInt("client_id");
                String name = rs.getString("name");
                int rankValue = rs.getInt("rank_value");
                client = new Client(dbClientId, name, rankValue);
            }
        } catch (SQLException e) {
            System.err.println("Database error retrieving client ID " + clientId + ": " + e.getMessage());
            throw new RuntimeException("Failed to retrieve client.", e);
        } finally {
            DatabaseManager.close(conn, pstmt, rs);
        }
        return client;
    }

    /**
     * Adds a new client to the database.
     * @param newClient A Client object (ID should be -1).
     * @return The new Client object with the database-generated ID.
     */
    public static Client addClient(Client newClient) {
        if (newClient.getId() != -1) {
            throw new IllegalArgumentException("Client already has a database ID: " + newClient.getId());
        }

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Client clientWithId = null;

        try {
            conn = DatabaseManager.getConnection();
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
        } catch (SQLException e) {
            System.err.println("Database error adding client '" + newClient.getName() + "': " + e.getMessage());
            throw new RuntimeException("Failed to add client.", e);
        } finally {
            DatabaseManager.close(conn, pstmt, rs);
        }
        return clientWithId;
    }
    
    /**
     * Retrieves all clients from the database.
     * @return A List of all Client objects.
     */
    public static List<Client> getAllClients() {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Client> clients = new ArrayList<>();

        try {
            conn = DatabaseManager.getConnection();
            pstmt = conn.prepareStatement(SELECT_ALL_CLIENTS);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                int dbClientId = rs.getInt("client_id");
                String name = rs.getString("name");
                int rankValue = rs.getInt("rank_value");
                clients.add(new Client(dbClientId, name, rankValue));
            }
        } catch (SQLException e) {
            System.err.println("Database error retrieving all clients: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve all clients.", e);
        } finally {
            DatabaseManager.close(conn, pstmt, rs);
        }
        return clients;
    }
    
    /**
     * Checks if a client exists.
     * @param clientId The ID of the client to check.
     * @return true if the client exists, false otherwise.
     */
    public static boolean clientExists(int clientId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseManager.getConnection();
            pstmt = conn.prepareStatement(CHECK_CLIENT_EXISTS);
            pstmt.setInt(1, clientId);
            rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Database error checking client existence for ID " + clientId + ": " + e.getMessage());
            return false;
        } finally {
            DatabaseManager.close(conn, pstmt, rs);
        }
    }

    /**
     * Retrieves all clients associated with a specific account.
     * This method uses a JOIN between clients and account_clients tables.
     * 
     * @param accountId The database ID of the account
     * @return A List of Client objects associated with the account (empty list if none found)
     * @throws RuntimeException if a database error occurs during the operation
     */
    public static List<Client> getClientsForAccount(int accountId) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<Client> clients = new ArrayList<>();

        try {
            // Step 1: Get database connection
            conn = DatabaseManager.getConnection();
            
            // Step 2: Prepare the JOIN statement
            pstmt = conn.prepareStatement(SELECT_CLIENTS_FOR_ACCOUNT);
            pstmt.setInt(1, accountId);
            
            // Step 3: Execute the query
            rs = pstmt.executeQuery();
            
            // Step 4: Process each row in the ResultSet
            while (rs.next()) {
                int clientId = rs.getInt("client_id");
                String name = rs.getString("name");
                int rankValue = rs.getInt("rank_value");
                
                Client client = new Client(clientId, name, rankValue);
                clients.add(client);
                
                System.out.println("Loaded client for account " + accountId + ": " + client.toString());
            }
            
            System.out.println("Successfully loaded " + clients.size() + " clients for account ID " + accountId);
            
        } catch (SQLException e) {
            System.err.println("Database error while retrieving clients for account " + accountId + ": " + e.getMessage());
            throw new RuntimeException("Failed to retrieve clients for account", e);
            
        } finally {
            // Step 5: CRITICAL - Always close resources in finally block
            DatabaseManager.close(conn, pstmt, rs);
        }
        
        return clients;
    }

    /**
     * Updates a client's rank in the database.
     * This method performs an UPDATE operation on the clients table.
     * 
     * @param clientId The database ID of the client to update
     * @param newRank The new rank value (should be between 0-10)
     * @return true if exactly one row was updated, false otherwise
     * @throws RuntimeException if a database error occurs during the operation
     */
    public static boolean updateClientRank(int clientId, int newRank) {
        // Validate input parameters
        if (newRank < 0 || newRank > 10) {
            System.err.println("Invalid rank value: " + newRank + ". Rank must be between 0 and 10.");
            return false;
        }
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        boolean success = false;

        try {
            // Step 1: Get database connection
            conn = DatabaseManager.getConnection();
            
            // Step 2: Prepare the UPDATE statement
            pstmt = conn.prepareStatement(UPDATE_CLIENT_RANK);
            pstmt.setInt(1, newRank);
            pstmt.setInt(2, clientId);
            
            // Step 3: Execute the update
            int rowsAffected = pstmt.executeUpdate();
            
            // Step 4: Check if exactly one row was affected
            if (rowsAffected == 1) {
                success = true;
                System.out.println("Successfully updated rank for Client ID " + clientId + " to " + newRank);
            } else if (rowsAffected == 0) {
                System.err.println("No client found with ID " + clientId + " to update.");
            } else {
                System.err.println("Unexpected: " + rowsAffected + " rows affected when updating Client ID " + clientId);
            }
            
        } catch (SQLException e) {
            System.err.println("Database error while updating client rank for ID " + clientId + ": " + e.getMessage());
            throw new RuntimeException("Failed to update client rank", e);
            
        } finally {
            // Step 5: CRITICAL - Always close resources in finally block
            DatabaseManager.close(conn, pstmt, null);
        }
        
        return success;
    }
}