import java.text.SimpleDateFormat;

public class MortgageAccount extends Account implements ProfitAccount, ManagementFeeAccount {
    private final double originalMortgageAmount;
    private final double monthlyPayment;
    private final int years;
    private static final double MORTGAGE_PROFIT_FACTOR = 0.8;
    private static final double RATE_DIFFERENCE = 0.10;
    private static final double MANAGEMENT_FEE_PERCENTAGE = 0.10; // 10% of the original mortgage amount
    private double profit = 0;

    /**
     * Constructor for creating MortgageAccount objects from database data.
     * This constructor should be used when retrieving accounts from the database.
     * 
     * @param id The unique database ID of the account
     * @param accountNumber The account number
     * @param bankNumber The bank number
     * @param managerName The manager's name
     * @param originalMortgageAmount The original mortgage amount
     * @param monthlyPayment The monthly payment amount
     * @param years The number of years for the mortgage
     * @throws DuplicationException if the account number is already in use
     */
    public MortgageAccount(int id, int accountNumber, int bankNumber, String managerName, double originalMortgageAmount, double monthlyPayment, int years) throws DuplicationException {
        super(id, accountNumber, bankNumber, managerName);
        this.originalMortgageAmount = originalMortgageAmount;
        this.monthlyPayment = monthlyPayment;
        this.years = years;
        calculateProfit(); // Calculate profit when the account is created
    }
    
    /**
     * Constructor for creating new MortgageAccount objects (without database ID).
     * This constructor should be used when creating new accounts that haven't been 
     * saved to the database yet (the database will auto-generate the ID).
     * 
     * @param accountNumber The account number
     * @param bankNumber The bank number
     * @param managerName The manager's name
     * @param originalMortgageAmount The original mortgage amount
     * @param monthlyPayment The monthly payment amount
     * @param years The number of years for the mortgage
     * @throws DuplicationException if the account number is already in use
     */
    public MortgageAccount(int accountNumber, int bankNumber, String managerName, double originalMortgageAmount, double monthlyPayment, int years) throws DuplicationException {
        super(accountNumber, bankNumber, managerName);
        this.originalMortgageAmount = originalMortgageAmount;
        this.monthlyPayment = monthlyPayment;
        this.years = years;
        calculateProfit(); // Calculate profit when the account is created
    }

    @Override
    public double getManagementFee() {
        return originalMortgageAmount * MANAGEMENT_FEE_PERCENTAGE; // Calculate based on 10% of the original amount
    }
    
    @Override
    public String getAccountType() {
        return "Mortgage Account";
    }

    @Override
    public void calculateProfit() {
        profit = ((MORTGAGE_PROFIT_FACTOR * this.originalMortgageAmount) / this.years) * RATE_DIFFERENCE;
    }

    @Override
    public double getProfit() {
        return profit;
    }

    @Override
    public String toString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String baseFormat = "%-25s {accountNumber=%d, dateOpened=%s, bankNumber=%d, managerName='%s', balance=%.2f NIS, originalMortgageAmount=%.2f NIS, monthlyPayment=%.2f NIS, years=%d}";
        
        if (hasId()) {
            return String.format("(ID:" + getId() + ") " + baseFormat,
                    getAccountType(),
                    getAccountNumber(),
                    dateFormat.format(getDateOpened()),
                    getBankNumber(),
                    getManagerName(),
                    getBalance(),
                    originalMortgageAmount,
                    monthlyPayment,
                    years);
        } else {
            return String.format(baseFormat,
                    getAccountType(),
                    getAccountNumber(),
                    dateFormat.format(getDateOpened()),
                    getBankNumber(),
                    getManagerName(),
                    getBalance(),
                    originalMortgageAmount,
                    monthlyPayment,
                    years);
        }
    }
    
    public double getOriginalMortgageAmount() {
        return originalMortgageAmount;
    }
    
    public double getMonthlyPayment() {
        return monthlyPayment;
    }
    
    public int getYears() {
        return years;
    }
}