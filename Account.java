import java.util.Date;

public abstract class Account {
    private final int id;
    private final int accountNumber;
    private final Date dateOpened;
    private final int bankNumber;
    private double balance; // Changed to be non-final
    private final String managerName;
    protected Client[] clients;
    private int clientCount;

    public Account(int id, int accountNumber, int bankNumber, String managerName) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.dateOpened = new Date();
        this.bankNumber = bankNumber;
        this.balance = 20.0;
        this.managerName = managerName;
        this.clients = new Client[2];
        this.clientCount = 0;
    }
    
    public Account(int accountNumber, int bankNumber, String managerName) {
        this(-1, accountNumber, bankNumber, managerName);
    }

    // Getters
    public int getId() { return id; }
    public boolean hasId() { return id != -1; }
    public int getAccountNumber() { return accountNumber; }
    public Date getDateOpened() { return dateOpened; }
    public int getBankNumber() { return bankNumber; }
    public double getBalance() { return balance; }
    public String getManagerName() { return managerName; }
    public Client[] getClients() { return clients; }
    public int getClientCount() {return clientCount;}

    // Setter for balance if needed later
    public void setBalance(double balance) {
        this.balance = balance;
    }

    public void addClient(Client client) {
        if (clientCount == clients.length) expandClientArray();
        clients[clientCount++] = client;
    }

    private void expandClientArray() {
        Client[] newClients = new Client[clients.length + 2];
        System.arraycopy(clients, 0, newClients, 0, clients.length);
        clients = newClients;
    }

    public abstract String getAccountType();
}