import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import .history.AccountDAO;
import .history.ClientDAO;
import .history.DatabaseManager;

/**
 * Bank class implementing the Unit of Work pattern with enhanced database operations.
 * Each business operation manages its own database connection and transaction.
 * 
 * This refactored version ensures proper connection management and implements
 * safe-destructive delete operations for improved data integrity.
 */
public class Bank {

    private static final String[] accountTypes = {
            "Regular Checking Account",
            "Business Checking Account",
            "Mortgage Account",
            "Savings Account"
    };

    public Bank() { }

    // ========== BUSINESS OPERATIONS WITH UNIT OF WORK PATTERN ==========

    /**
     * Retrieves all accounts from the database.
     * Uses Unit of Work pattern for connection management.
     */
    public String getAllAccounts() {
        List<Account> accounts = AccountDAO.getAllAccounts();
        if (accounts.isEmpty()) {
            return "No accounts in the database.";
        }
        return formatAccountList(accounts, "--- All Accounts ---");
    }

    /**
     * Retrieves accounts by type from the database.
     * Uses Unit of Work pattern for connection management.
     */
    public String getAccountsByType(String accountType) {
        List<Account> accounts = AccountDAO.getAccountsByType(accountType);
        if (accounts.isEmpty()) {
            return "No accounts found for type: " + accountType;
        }
        return formatAccountList(accounts, "--- Accounts of Type: " + accountType + " ---");
    }

    /**
     * Retrieves profit accounts from the database.
     * Uses Unit of Work pattern for connection management.
     */
    public String getProfitAccounts() {
        List<Account> accounts = AccountDAO.getProfitAccounts();
        if (accounts.isEmpty()) {
            return "No profit-generating accounts found.";
        }
        return formatAccountList(accounts, "--- Accounts Ordered by Profit ---");
    }

    /**
     * Registers a new client to an existing account.
     * Uses Unit of Work pattern for connection management.
     * All operations within this method share the same connection/transaction.
     */
    public String registerClientToAccount(int accountNumber, String clientName, int clientRank) {
        try (Connection conn = DatabaseManager.getConnection()) {
            // Step 1: Get the account by number (use private method with connection)
            Account account = AccountDAO.getAccountByNumber(conn, accountNumber);
            if (account == null) {
                return "Error: Account #" + accountNumber + " not found.";
            }
            
            // Step 2: Create and save the new client
            Client newClient = new Client(clientName, clientRank);
            Client savedClient = ClientDAO.addClient(conn, newClient);
            
            // Step 3: Associate the client with the account
            AccountDAO.addClientToAccount(conn, account.getId(), savedClient.getId());
            
            return "Client '" + clientName + "' added to account #" + accountNumber;
            
        } catch (SQLException e) {
            System.err.println("Database error in registerClientToAccount: " + e.getMessage());
            return "Error: Could not add client. " + e.getMessage();
        } catch (Exception e) {
            return "Error: Could not add client. " + e.getMessage();
        }
    }

    /**
     * Gets the profit for a specific account.
     * Uses Unit of Work pattern for connection management.
     */
    public String getAccountProfit(int accountNumber) {
        Account account = AccountDAO.getAccountByNumber(accountNumber);
        if (account == null) {
            return "Error: Account #" + accountNumber + " does not exist.";
        }
        
        if (account instanceof ProfitAccount) {
            ProfitAccount profitAccount = (ProfitAccount) account;
            return String.format("Annual profit for account #%d is: %.2f ILS", accountNumber, profitAccount.getProfit());
        } else {
            return "Account #" + accountNumber + " does not generate profit.";
        }
    }

    /**
     * Calculates the total annual profit of all profit accounts.
     * Uses Unit of Work pattern for connection management.
     */
    public String getTotalAnnualProfit() {
        List<Account> profitAccounts = AccountDAO.getProfitAccounts();
        if (profitAccounts.isEmpty()) {
            return "No profit accounts to sum.";
        }
        
        double totalProfit = 0.0;
        for (Account account : profitAccounts) {
            totalProfit += ((ProfitAccount) account).getProfit();
        }
        return String.format("Total annual profit of the bank: %.2f ILS", totalProfit);
    }

    /**
     * Gets the top checking account by profit.
     * Uses Unit of Work pattern for connection management.
     */
    public String getTopCheckingAccountByProfit() {
        Account topAccount = AccountDAO.getTopCheckingAccountByProfit();
        if (topAccount == null) {
            return "No checking accounts with profit found.";
        }
        return "--- Top Checking Account by Profit ---\n" + topAccount.toString();
    }
    
    /**
     * Checks VIP profit status for a business checking account.
     * Uses Unit of Work pattern for connection management.
     * Loads associated clients within the same connection/transaction.
     */
    public String checkBusinessVIPProfit(int accountNumber) {
        try (Connection conn = DatabaseManager.getConnection()) {
            Account account = AccountDAO.getAccountByNumber(conn, accountNumber);
            if (account == null) {
                return "Error: Account #" + accountNumber + " not found.";
            }
            if (!(account instanceof BusinessCheckingAccount)) {
                return "Error: Account #" + accountNumber + " is not a Business Checking Account.";
            }

            BusinessCheckingAccount bizAccount = (BusinessCheckingAccount) account;
            
            // Load clients for this account within the same connection
            List<Client> clients = ClientDAO.getClientsForAccount(conn, bizAccount.getId());
            for (Client c : clients) {
                bizAccount.addClient(c);
            }
            
            // Check VIP status
            boolean isVIP = bizAccount.getBusinessRevenue() >= BusinessCheckingAccount.VIP_REVENUE_THRESHOLD;
            for (Client client : bizAccount.getClients()) {
                if (client != null && client.getRank() != BusinessCheckingAccount.VIP_CLIENT_RANK) {
                    isVIP = false;
                    break;
                }
            }
            
            if (isVIP) {
                return "VIP Profit for Business Account #" + accountNumber + ": " + bizAccount.checkProfitVIP() + " ILS";
            } else {
                return "Account #" + accountNumber + " does not qualify as a VIP Business Account.";
            }
        } catch (SQLException e) {
            System.err.println("Database error in checkBusinessVIPProfit: " + e.getMessage());
            return "Error checking VIP profit: " + e.getMessage();
        }
    }
    
    /**
     * Updates a client's rank.
     * Uses Unit of Work pattern for connection management.
     * The rank change will be automatically logged via database trigger.
     */
    public String updateClientRank(int clientId, int newRank) {
        if (!ClientDAO.clientExists(clientId)) {
            return "Error: Client with ID " + clientId + " not found.";
        }
        
        boolean success = ClientDAO.updateClientRank(clientId, newRank);
        if (success) {
            return "Successfully updated rank for client ID " + clientId + ". Change has been logged to audit table.";
        } else {
            return "Failed to update rank.";
        }
    }

    /**
     * Enhanced method for removing a client from an account with safe-destructive functionality.
     * Uses Unit of Work pattern to ensure all operations occur within the same transaction.
     * 
     * Process:
     * 1. Remove client-account association
     * 2. Check if client has other associations
     * 3. If no other associations exist, delete the client entirely
     * 
     * All operations are performed within a single connection/transaction for data consistency.
     */
    public String removeClientFromAccount(int accountNumber, int clientId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false); // Start transaction for safe-destructive delete
            
            try {
                // Step 1: Get the account by number to find the account ID
                Account account = AccountDAO.getAccountByNumber(conn, accountNumber);
                if (account == null) {
                    conn.rollback();
                    return "Error: Account #" + accountNumber + " not found.";
                }
                
                // Step 2: Check if client exists
                if (!ClientDAO.clientExists(conn, clientId)) {
                    conn.rollback();
                    return "Error: Client with ID " + clientId + " not found.";
                }
                
                // Step 3: Remove the association from account_clients
                boolean associationRemoved = AccountDAO.removeClientFromAccount(conn, account.getId(), clientId);
                if (!associationRemoved) {
                    conn.rollback();
                    return "Failed to remove client " + clientId + " from account #" + accountNumber + ". Association may not exist.";
                }
                
                StringBuilder result = new StringBuilder();
                result.append("Successfully removed client ").append(clientId).append(" from account #").append(accountNumber).append(".");
                
                // Step 4: Check if client is associated with any other accounts
                boolean isAssociatedWithOtherAccounts = ClientDAO.isClientAssociatedWithAnyAccount(conn, clientId);
                
                // Step 5: If client is not associated with any other accounts, delete the client entirely
                if (!isAssociatedWithOtherAccounts) {
                    boolean clientDeleted = ClientDAO.deleteClientById(conn, clientId);
                    if (clientDeleted) {
                        result.append(" Client ").append(clientId)
                              .append(" was not associated with any other accounts and has been completely deleted from the system.");
                    } else {
                        // This shouldn't happen, but handle it gracefully
                        result.append(" However, failed to delete client ").append(clientId).append(" from the system.");
                    }
                } else {
                    result.append(" Client ").append(clientId)
                          .append(" remains in the system as they are associated with other accounts.");
                }
                
                conn.commit(); // Commit the transaction
                System.out.println("Safe-destructive delete transaction committed successfully.");
                return result.toString();
                
            } catch (SQLException e) {
                conn.rollback();
                throw e; // Re-throw to be handled by outer catch block
            }
            
        } catch (SQLException e) {
            System.err.println("Database error in removeClientFromAccount: " + e.getMessage());
            return "Error removing client from account: " + e.getMessage();
        }
    }

    /**
     * Shows all clients in the database.
     * Uses Unit of Work pattern for connection management.
     */
    public String getAllClients() {
        List<Client> clients = ClientDAO.getAllClients();
        if (clients.isEmpty()) {
            return "No clients in the database.";
        }
        
        StringBuilder sb = new StringBuilder("--- All Clients ---\n\n");
        for (Client client : clients) {
            sb.append(client.toString()).append("\n");
        }
        return sb.toString();
    }
    
    /**
     * Shows all accounts with their associated clients.
     * Uses Unit of Work pattern for connection management.
     * All queries within this method share the same connection.
     */
    public String getAllAccountClientAssociations() {
        try (Connection conn = DatabaseManager.getConnection()) {
            List<Account> accounts = AccountDAO.getAllAccounts(conn);
            if (accounts.isEmpty()) {
                return "No accounts in the database.";
            }
            
            StringBuilder sb = new StringBuilder("--- Account-Client Associations ---\n\n");
            int accountsWithClients = 0;
            int accountsWithoutClients = 0;
            
            for (Account account : accounts) {
                // Always display the account header
                sb.append(String.format("Account #%d (%s) - Manager: %s:\n", 
                    account.getAccountNumber(), 
                    account.getAccountType(),
                    account.getManagerName()));
                
                List<Client> clients = ClientDAO.getClientsForAccount(conn, account.getId());
                
                if (!clients.isEmpty()) {
                    // Account has clients - display them
                    accountsWithClients++;
                    for (Client client : clients) {
                        sb.append(String.format("  -> Client ID: %d, Name: %s, Rank: %d\n", 
                            client.getId(), client.getName(), client.getRank()));
                    }
                } else {
                    // Account has no clients - clearly indicate this
                    accountsWithoutClients++;
                    sb.append("  -> No clients associated with this account.\n");
                }
                sb.append("\n"); // Add spacing between accounts
            }
            
            // Add summary statistics
            sb.append("--- Summary ---\n");
            sb.append(String.format("Total accounts: %d\n", accounts.size()));
            sb.append(String.format("Accounts with clients: %d\n", accountsWithClients));
            sb.append(String.format("Accounts without clients: %d\n", accountsWithoutClients));
            
            return sb.toString();
            
        } catch (SQLException e) {
            System.err.println("Database error in getAllAccountClientAssociations: " + e.getMessage());
            return "Error retrieving account-client associations: " + e.getMessage();
        }
    }

    /**
     * Shows all accounts with their IDs and managers.
     * Uses Unit of Work pattern for connection management.
     */
    public String getAccountsSummary() {
        List<Account> accounts = AccountDAO.getAllAccounts();
        if (accounts.isEmpty()) {
            return "No accounts in the database.";
        }
        
        StringBuilder sb = new StringBuilder("--- Accounts Summary ---\n\n");
        for (Account account : accounts) {
            sb.append(String.format("Account #%d (ID: %d) - %s - Manager: %s\n", 
                account.getAccountNumber(), 
                account.getId(), 
                account.getAccountType(), 
                account.getManagerName()));
        }
        return sb.toString();
    }
    
    /**
     * Shows all clients with their IDs and ranks.
     * Uses Unit of Work pattern for connection management.
     */
    public String getClientsSummary() {
        List<Client> clients = ClientDAO.getAllClients();
        if (clients.isEmpty()) {
            return "No clients in the database.";
        }
        
        StringBuilder sb = new StringBuilder("--- Clients Summary ---\n\n");
        for (Client client : clients) {
            sb.append(String.format("Client ID: %d - Name: %s - Rank: %d\n", 
                client.getId(), client.getName(), client.getRank()));
        }
        return sb.toString();
    }

    // ========== STATIC HELPER METHODS ==========

    /**
     * Returns the available account types.
     */
    public static String[] getAccountTypes() {
        return accountTypes;
    }

    /**
     * Formats a list of accounts for display.
     * Includes profit information where applicable.
     */
    private String formatAccountList(List<Account> accounts, String header) {
        StringBuilder sb = new StringBuilder(header + "\n\n");
        for (Account account : accounts) {
            sb.append(account.toString());
            if (account instanceof ProfitAccount) {
                 sb.append(String.format("\n  -> Profit: %.2f ILS", ((ProfitAccount) account).getProfit()));
            }
            sb.append("\n\n");
        }
        return sb.toString();
    }
}