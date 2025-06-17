import java.util.List;

public class Bank {

    private static final String[] accountTypes = {
            "Regular Checking Account",
            "Business Checking Account",
            "Mortgage Account",
            "Savings Account"
    };

    public Bank() { }

    // --- Refactored Methods for Menu Options ---

    public String getAllAccounts() { // FIXED: שונה ל-public
        List<Account> accounts = AccountDAO.getAllAccounts();
        if (accounts.isEmpty()) return "No accounts in the database.";
        return formatAccountList(accounts, "--- All Accounts ---");
    }

    public String getAccountsByType(String accountType) { // FIXED: שונה ל-public
        List<Account> accounts = AccountDAO.getAccountsByType(accountType);
        if (accounts.isEmpty()) return "No accounts found for type: " + accountType;
        return formatAccountList(accounts, "--- Accounts of Type: " + accountType + " ---");
    }

    public String getProfitAccounts() { // FIXED: שונה ל-public
        List<Account> accounts = AccountDAO.getProfitAccounts();
        if (accounts.isEmpty()) return "No profit-generating accounts found.";
        return formatAccountList(accounts, "--- Accounts Ordered by Profit ---");
    }

    public String registerClientToAccount(int accountNumber, String clientName, int clientRank) { // FIXED: שונה ל-public
        Account account = AccountDAO.getAccountByNumber(accountNumber);
        if (account == null) return "Error: Account #" + accountNumber + " not found.";
        
        Client newClient = new Client(clientName, clientRank);
        try {
            Client savedClient = ClientDAO.addClient(newClient);
            AccountDAO.addClientToAccount(account.getId(), savedClient.getId());
            return "Client '" + clientName + "' added to account #" + accountNumber;
        } catch (Exception e) {
            return "Error: Could not add client. " + e.getMessage();
        }
    }

    public String getAccountProfit(int accountNumber) { // FIXED: שונה ל-public
        Account account = AccountDAO.getAccountByNumber(accountNumber);
        if (account == null) return "Error: Account #" + accountNumber + " does not exist.";
        
        if (account instanceof ProfitAccount) {
            ProfitAccount profitAccount = (ProfitAccount) account;
            return String.format("Annual profit for account #%d is: %.2f ILS", accountNumber, profitAccount.getProfit());
        } else {
            return "Account #" + accountNumber + " does not generate profit.";
        }
    }

    public String getTotalAnnualProfit() { // FIXED: שונה ל-public
        List<Account> profitAccounts = AccountDAO.getProfitAccounts();
        if (profitAccounts.isEmpty()) return "No profit accounts to sum.";
        
        double totalProfit = 0.0;
        for (Account account : profitAccounts) {
            totalProfit += ((ProfitAccount) account).getProfit();
        }
        return String.format("Total annual profit of the bank: %.2f ILS", totalProfit);
    }

    public String getTopCheckingAccountByProfit() { // FIXED: שונה ל-public
        Account topAccount = AccountDAO.getTopCheckingAccountByProfit();
        if (topAccount == null) return "No checking accounts with profit found.";
        return "--- Top Checking Account by Profit ---\n" + topAccount.toString();
    }
    
    public String checkBusinessVIPProfit(int accountNumber) { // FIXED: שונה ל-public
        Account account = AccountDAO.getAccountByNumber(accountNumber);
        if (account == null) return "Error: Account #" + accountNumber + " not found.";
        if (!(account instanceof BusinessCheckingAccount)) return "Error: Account #" + accountNumber + " is not a Business Checking Account.";

        BusinessCheckingAccount bizAccount = (BusinessCheckingAccount) account;
        List<Client> clients = ClientDAO.getClientsForAccount(bizAccount.getId());
        for (Client c : clients) {
            bizAccount.addClient(c);
        }
        
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
    }
    
    // --- Methods for UPDATE and DELETE ---

    public String updateClientRank(int clientId, int newRank) { // FIXED: שונה ל-public
        if (!ClientDAO.clientExists(clientId)) return "Error: Client with ID " + clientId + " not found.";
        boolean success = ClientDAO.updateClientRank(clientId, newRank);
        return success ? "Successfully updated rank for client ID " + clientId : "Failed to update rank.";
    }

    public String removeClientFromAccount(int accountNumber, int clientId) { // FIXED: שונה ל-public
        Account account = AccountDAO.getAccountByNumber(accountNumber);
        if (account == null) return "Error: Account #" + accountNumber + " not found.";
        if (!ClientDAO.clientExists(clientId)) return "Error: Client with ID " + clientId + " not found.";
        
        boolean success = AccountDAO.removeClientFromAccount(account.getId(), clientId);
        return success ? "Successfully removed client " + clientId + " from account #" + accountNumber : "Failed to remove client. Association may not exist.";
    }

    // --- Static helper methods ---

    public static String[] getAccountTypes() { // FIXED: שונה ל-public
        return accountTypes;
    }

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

    /**
     * show all clients in the database
     */
    public String getAllClients() {
        List<Client> clients = ClientDAO.getAllClients();
        if (clients.isEmpty()) return "No clients in the database.";
        
        StringBuilder sb = new StringBuilder("--- All Clients ---\n\n");
        for (Client client : clients) {
            sb.append(client.toString()).append("\n");
        }
        return sb.toString();
    }
    
    /**
     * show all accounts with their associated clients
     */
    public String getAllAccountClientAssociations() {
        List<Account> accounts = AccountDAO.getAllAccounts();
        if (accounts.isEmpty()) return "No accounts in the database.";
        
        StringBuilder sb = new StringBuilder("--- Account-Client Associations ---\n\n");
        boolean hasAssociations = false;
        
        for (Account account : accounts) {
            List<Client> clients = ClientDAO.getClientsForAccount(account.getId());
            if (!clients.isEmpty()) {
                hasAssociations = true;
                sb.append(String.format("Account #%d (%s):\n", 
                    account.getAccountNumber(), account.getAccountType()));
                for (Client client : clients) {
                    sb.append(String.format("  -> Client ID: %d, Name: %s, Rank: %d\n", 
                        client.getId(), client.getName(), client.getRank()));
                }
                sb.append("\n");
            }
        }
        
        if (!hasAssociations) {
            sb.append("No client-account associations found.\n");
        }
        
        return sb.toString();
    }
    
    /**
     * // show all accounts with their IDs and managers
     */
    public String getAccountsSummary() {
        List<Account> accounts = AccountDAO.getAllAccounts();
        if (accounts.isEmpty()) return "No accounts in the database.";
        
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
     * // show all clients with their IDs and ranks
     */
    public String getClientsSummary() {
        List<Client> clients = ClientDAO.getAllClients();
        if (clients.isEmpty()) return "No clients in the database.";
        
        StringBuilder sb = new StringBuilder("--- Clients Summary ---\n\n");
        for (Client client : clients) {
            sb.append(String.format("Client ID: %d - Name: %s - Rank: %d\n", 
                client.getId(), client.getName(), client.getRank()));
        }
        return sb.toString();
    }
}