import java.text.SimpleDateFormat;

public class BusinessCheckingAccount extends CheckingAccount implements ManagementFeeAccount, Cloneable {
    public static final double VIP_REVENUE_THRESHOLD = 10000000; // Threshold for VIP business revenue
    public static final int VIP_CLIENT_RANK = 10;
    private static final double FIXED_COMMISSION = 3000; // Fixed annual commission for non-VIP accounts
    private static final double MANAGEMENT_FEE = 1000; // Fixed annual management fee for business accounts
    private final double businessRevenue;
    private double profit;

    /**
     * Constructor for creating BusinessCheckingAccount objects from database data.
     * This constructor should be used when retrieving accounts from the database.
     * 
     * @param id The unique database ID of the account
     * @param accountNumber The account number
     * @param bankNumber The bank number
     * @param managerName The manager's name
     * @param creditLimit The credit limit for this checking account
     * @param businessRevenue The business revenue amount
     * @throws DuplicationException if the account number is already in use
     */
    public BusinessCheckingAccount(int id, int accountNumber, int bankNumber, String managerName, double creditLimit, double businessRevenue) throws DuplicationException {
        super(id, accountNumber, bankNumber, managerName, creditLimit);
        this.businessRevenue = businessRevenue;
        calculateProfit(); // Calculate profit when the account is created
    }
    
    /**
     * Constructor for creating new BusinessCheckingAccount objects (without database ID).
     * This constructor should be used when creating new accounts that haven't been 
     * saved to the database yet (the database will auto-generate the ID).
     * 
     * @param accountNumber The account number
     * @param bankNumber The bank number
     * @param managerName The manager's name
     * @param creditLimit The credit limit for this checking account
     * @param businessRevenue The business revenue amount
     * @throws DuplicationException if the account number is already in use
     */
    public BusinessCheckingAccount(int accountNumber, int bankNumber, String managerName, double creditLimit, double businessRevenue) throws DuplicationException {
        super(accountNumber, bankNumber, managerName, creditLimit);
        this.businessRevenue = businessRevenue;
        calculateProfit(); // Calculate profit when the account is created
    }

    // Method to get the management fee for business checking accounts
    public double getManagementFee() {
        return MANAGEMENT_FEE; // Fixed management fee for business checking accounts
    }
    
    public double getBusinessRevenue() {
        return businessRevenue;
    }

    @Override
    public void calculateProfit() {
        boolean allClientsAreVIP = true;

        // Check if all clients have a rank of 10
        for (Client client : this.getClients()) {
            if (client != null && client.getRank() != VIP_CLIENT_RANK) {
                allClientsAreVIP = false;
                break; // No need to check further if one client doesn't meet the rank requirement
            }
        }

        // Determine profit based on the VIP status criteria
        if (this.businessRevenue >= VIP_REVENUE_THRESHOLD && allClientsAreVIP) {
            profit = 0; // VIP account, no profit
        } else {
            // Calculate profit using credit limit and add a fixed commission
            profit = this.getCreditLimit() * RATE_DIFFERENCE + FIXED_COMMISSION;
        }
    }

    // Method to check the profit with all client ranks set to 0 (as per the instructions)
    public double checkProfitVIP() {
        try {
            // Clone the current instance to get a deep copy
            BusinessCheckingAccount copy = this.clone();

            // Reset all client ranks to 0 in the cloned account for VIP profit check
            for (Client client : copy.getClients()) {
                if (client != null) {
                    client.setRank(0); // Set client rank to 0 for all clients
                }
            }

            // Recalculate profit based on the modified ranks and business revenue
            copy.calculateProfit();

            // Return the recalculated profit based on the modified account copy
            return copy.getProfit();

        } catch (Exception e) {
            System.out.println("Error calculating VIP profit: " + e.getMessage());
            return 0;
        }
    }
    
    @Override
    protected BusinessCheckingAccount clone() throws CloneNotSupportedException {
        BusinessCheckingAccount copy = (BusinessCheckingAccount) super.clone();

        // Deep clone the clients array
        if (this.getClients() != null) {
            Client[] clonedClients = new Client[this.getClients().length];
            for (int i = 0; i < this.getClients().length; i++) {
                if (this.getClients()[i] != null) {
                    clonedClients[i] = this.getClients()[i].clone(); // Assuming Client class implements Cloneable
                }
            }
            copy.setClients(clonedClients); // Use the setter to update clients in the cloned instance
        }

        return copy;
    }

    protected void setClients(Client[] clients) {
        this.clients = clients;
    }
    
    @Override
    public double getProfit() {
        calculateProfit(); // Recalculate profit before returning
        return profit;
    }

    @Override
    public String getAccountType() {
        return "Business Checking Account";
    }

    @Override
    public String toString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String baseFormat = "%-25s {accountNumber=%d, dateOpened=%s, bankNumber=%d, managerName='%s', balance=%.2f NIS, businessRevenue=%.2f NIS}";
        
        if (hasId()) {
            return String.format("(ID:" + getId() + ") " + baseFormat,
                    getAccountType(),
                    getAccountNumber(),
                    dateFormat.format(getDateOpened()),
                    getBankNumber(),
                    getManagerName(),
                    getBalance(),
                    businessRevenue);
        } else {
            return String.format(baseFormat,
                    getAccountType(),
                    getAccountNumber(),
                    dateFormat.format(getDateOpened()),
                    getBankNumber(),
                    getManagerName(),
                    getBalance(),
                    businessRevenue);
        }
    }
}