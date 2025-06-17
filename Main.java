import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Bank bank = new Bank();
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        System.out.println("--- Bank Management System Initialized ---");
        DatabaseManager.getInstance(); // Ensures driver is loaded

        while (running) {
            showMenu();
            String choice = getValidChoice(scanner);

            switch (choice) {
                case "0":
                    running = false;
                    System.out.println("👋 Exiting... Goodbye!");
                    break;
                case "1": // Display all accounts
                    System.out.println(bank.getAllAccounts());
                    break;
                case "2": // Display accounts by type
                    showAccountTypes();
                    int typeChoice = getValidIntInput(scanner, "Enter account type (1-4): ", 1, 4);
                    String accountType = Bank.getAccountTypes()[typeChoice - 1];
                    System.out.println(bank.getAccountsByType(accountType));
                    break;
                case "3": // Display accounts with profit
                    System.out.println(bank.getProfitAccounts());
                    break;
                case "4": // Display all clients
                    showAllClients(bank, scanner);
                    break;
                case "5": // Display associations
                    showAllAssociations(bank, scanner);
                    break;
                case "6": // Add new account
                    addNewAccountManually(bank, scanner);
                    break;
                case "7": // Add client to account
                    addClientToAccount(bank, scanner);
                    break;
                case "8": // Update client rank
                    updateClientRank(bank, scanner);
                    break;
                case "9": // Remove client from account
                    removeClientFromAccount(bank, scanner);
                    break;
                case "10": // VIP profit check
                    int vipAccNum = getValidIntInput(scanner, "Enter business account number for VIP check: ", 1, Integer.MAX_VALUE);
                    System.out.println(bank.checkBusinessVIPProfit(vipAccNum));
                    break;
                default:
                    System.out.println("❌ Invalid choice. Please try again.");
                    break;
            }
        }
        scanner.close();
    }

private static void showMenu() {
        System.out.println("\n╔════════════════════════════════════════════════════╗");
        System.out.println("║             Bank Management System                ║");
        System.out.println("╠════════════════════════════════════════════════════╣");
        System.out.println("║                   VIEW & REPORTS                   ║");
        System.out.println("║ 1. Display all accounts                           ║");
        System.out.println("║ 2. Display accounts by type                       ║"); 
        System.out.println("║ 3. Display accounts with annual profit            ║");
        System.out.println("║ 4. Display all clients                            ║");
        System.out.println("║ 5. Display account-client associations            ║");
        System.out.println("╠════════════════════════════════════════════════════╣");
        System.out.println("║                   CREATE & ADD                     ║");
        System.out.println("║ 6. Add a new account manually                     ║");
        System.out.println("║ 7. Add a client to an existing account            ║");
        System.out.println("╠════════════════════════════════════════════════════╣");
        System.out.println("║                 UPDATE & DELETE                    ║");
        System.out.println("║ 8. Update a client's rank                         ║");
        System.out.println("║ 9. Remove a client from an account                ║");
        System.out.println("╠════════════════════════════════════════════════════╣");
        System.out.println("║               SPECIAL OPERATIONS                   ║");
        System.out.println("║10. Check VIP profit status (Business Account)     ║");
        System.out.println("╠════════════════════════════════════════════════════╣");
        System.out.println("║ 0. Exit                                            ║");
        System.out.println("╚════════════════════════════════════════════════════╝");
        System.out.print("Your choice: ");
    }
    
    // --- Methods for handling menu choices ---

    private static void updateClientRank(Bank bank, Scanner scanner) {
        System.out.println("--- Update Client Rank ---");
        
        // show current clients
        System.out.println("\nCurrent clients in the system:");
        System.out.println(bank.getClientsSummary());
        
        int clientId = getValidIntInput(scanner, "Enter client ID to update: ", 1, Integer.MAX_VALUE);
        int newRank = getValidIntInput(scanner, "Enter new rank (0-10): ", 0, 10);
        String result = bank.updateClientRank(clientId, newRank);
        System.out.println(result);
    }

    private static void removeClientFromAccount(Bank bank, Scanner scanner) {
        System.out.println("--- Remove Client from Account ---");
        
        // show current account-client associations
        System.out.println("\nCurrent account-client associations:");
        System.out.println(bank.getAllAccountClientAssociations());
        
        int accountNumber = getValidIntInput(scanner, "Enter account number: ", 1, Integer.MAX_VALUE);
        int clientId = getValidIntInput(scanner, "Enter client ID to remove: ", 1, Integer.MAX_VALUE);
        String result = bank.removeClientFromAccount(accountNumber, clientId);
        System.out.println(result);
    }


    // Refactored method to add a new account using DAOs
    private static void addNewAccountManually(Bank bank, Scanner scanner) {
        System.out.println("--- Adding a new account manually ---");
        showAccountTypes();
        int accountTypeChoice = getValidIntInput(scanner, "Enter account type (1-4): ", 1, 4);

        try {
            switch (accountTypeChoice) {
                case 1:
                    addRegularCheckingAccount(bank, scanner);
                    break;
                case 2:
                    addBusinessCheckingAccount(bank, scanner);
                    break;
                case 3:
                    addMortgageAccount(bank, scanner);
                    break;
                case 4:
                    addSavingsAccount(bank, scanner);
                    break;
            }
        } catch (DuplicationException e) {
             System.out.println("\nError creating account: " + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println("\nAn unexpected error occurred: " + e.getMessage());
        }
    }

    // Helper for adding a Regular Checking Account
    private static void addRegularCheckingAccount(Bank bank, Scanner scanner) throws DuplicationException {
        System.out.println("-- Creating Regular Checking Account --");
        int accountNumber = getNewAccountNumber(scanner);
        String managerName = getValidName(scanner, "manager");
        double creditLimit = getValidDoubleInput(scanner, "Enter credit limit: ");

        Account newAccount = new RegularCheckingAccount(accountNumber, 1, managerName, creditLimit);
        Account savedAccount = AccountDAO.addAccount(newAccount);

        if (savedAccount != null) {
            System.out.println("Account created successfully with ID: " + savedAccount.getId());
            addFirstClientToAccount(savedAccount, scanner);
        } else {
            System.out.println("Failed to create account.");
        }
    }

    // Helper for adding a Business Checking Account
    private static void addBusinessCheckingAccount(Bank bank, Scanner scanner) throws DuplicationException {
        System.out.println("-- Creating Business Checking Account --");
        int accountNumber = getNewAccountNumber(scanner);
        String managerName = getValidName(scanner, "manager");
        double creditLimit = getValidDoubleInput(scanner, "Enter credit limit: ");
        double businessRevenue = getValidDoubleInput(scanner, "Enter business revenue: ");

        Account newAccount = new BusinessCheckingAccount(accountNumber, 1, managerName, creditLimit, businessRevenue);
        Account savedAccount = AccountDAO.addAccount(newAccount);
        
        if (savedAccount != null) {
            System.out.println("Account created successfully with ID: " + savedAccount.getId());
            addFirstClientToAccount(savedAccount, scanner);
        } else {
            System.out.println("Failed to create account.");
        }
    }

    // Helper for adding a Mortgage Account
    private static void addMortgageAccount(Bank bank, Scanner scanner) throws DuplicationException {
         System.out.println("-- Creating Mortgage Account --");
        int accountNumber = getNewAccountNumber(scanner);
        String managerName = getValidName(scanner, "manager");
        double originalAmount = getValidDoubleInput(scanner, "Enter original mortgage amount: ");
        double monthlyPayment = getValidDoubleInput(scanner, "Enter monthly payment: ");
        int years = getValidIntInput(scanner, "Enter mortgage years: ", 1, 100);

        Account newAccount = new MortgageAccount(accountNumber, 1, managerName, originalAmount, monthlyPayment, years);
        Account savedAccount = AccountDAO.addAccount(newAccount);

        if (savedAccount != null) {
            System.out.println("Account created successfully with ID: " + savedAccount.getId());
            addFirstClientToAccount(savedAccount, scanner);
        } else {
            System.out.println("Failed to create account.");
        }
    }

    // Helper for adding a Savings Account
    private static void addSavingsAccount(Bank bank, Scanner scanner) throws DuplicationException {
        System.out.println("-- Creating Savings Account --");
        int accountNumber = getNewAccountNumber(scanner);
        String managerName = getValidName(scanner, "manager");
        double depositAmount = getValidDoubleInput(scanner, "Enter deposit amount: ");
        int years = getValidIntInput(scanner, "Enter savings years: ", 1, 100);

        Account newAccount = new SavingsAccount(accountNumber, 1, managerName, depositAmount, years);
        Account savedAccount = AccountDAO.addAccount(newAccount);

        if (savedAccount != null) {
            System.out.println("Account created successfully with ID: " + savedAccount.getId());
            addFirstClientToAccount(savedAccount, scanner);
        } else {
            System.out.println("Failed to create account.");
        }
    }
    
    // Gets a new account number and validates it doesn't exist in the DB
    private static int getNewAccountNumber(Scanner scanner) throws DuplicationException {
        while (true) {
            int accountNumber = getValidIntInput(scanner, "Enter a new account number: ", 1, Integer.MAX_VALUE);
            if (AccountDAO.accountExists(accountNumber)) {
                System.out.println("Error: Account number " + accountNumber + " already exists. Please choose a different number.");
            } else {
                return accountNumber;
            }
        }
    }

    // Adds the first client to a newly created account
    private static void addFirstClientToAccount(Account account, Scanner scanner) {
        System.out.println("-- Adding First Client to New Account --");
        String clientName = getValidName(scanner, "client");
        int clientRank = getValidIntInput(scanner, "Enter client rank (0-10): ", 0, 10);
        
        Client newClient = new Client(clientName, clientRank);
        Client savedClient = ClientDAO.addClient(newClient);

        if (savedClient != null) {
            AccountDAO.addClientToAccount(account.getId(), savedClient.getId());
        } else {
            System.out.println("Failed to save the new client to the database.");
        }
    }
    
    // Adds a client to an already existing account
    private static void addClientToAccount(Bank bank, Scanner scanner) {
        System.out.println("--- Registering a Client to an Existing Account ---");
        
        // show available accounts
        System.out.println("\nAvailable accounts:");
        System.out.println(bank.getAccountsSummary());
        
        int accountNumber = getValidIntInput(scanner, "Enter account number to add a client to: ", 1, Integer.MAX_VALUE);

        if (!AccountDAO.accountExists(accountNumber)) {
            System.out.println("Error: Account number " + accountNumber + " does not exist. Please try again.");
            return;
        }

        String clientName = getValidName(scanner, "client");
        int clientRank = getValidIntInput(scanner, "Enter client rank (0-10): ", 0, 10);
        String resultMessage = bank.registerClientToAccount(accountNumber, clientName, clientRank);
        System.out.println("\n" + resultMessage);
    }

    // added method to show all accounts
    private static void showAllAssociations(Bank bank, Scanner scanner) {
        System.out.println("--- All Account-Client Associations ---");
        System.out.println(bank.getAllAccountClientAssociations());
    }

    // added method to show all clients
    private static void showAllClients(Bank bank, Scanner scanner) {
        System.out.println("--- All Clients ---");
        System.out.println(bank.getAllClients());
    }

    // --- Input Validation and UI Helper Methods ---

    private static void showAccountTypes() {
        System.out.println("Choose account type:");
        String[] accountTypes = Bank.getAccountTypes();
        for (int i = 0; i < accountTypes.length; i++) {
            System.out.println((i + 1) + ". " + accountTypes[i]);
        }
    }

    private static String getValidName(Scanner scanner, String type) {
        while (true) {
            System.out.print("Enter " + type + " name: ");
            String name = scanner.nextLine().trim();
            if (name != null && !name.isEmpty() && name.matches("[a-zA-Z ]+")) {
                return name;
            }
            System.out.println("Invalid name. Please use letters and spaces only.");
        }
    }

    private static String getValidChoice(Scanner scanner) {
        while (true) {
            String input = scanner.nextLine().trim();
            
            // רשימת האפשרויות החוקיות
            String[] validChoices = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
            
            for (String validChoice : validChoices) {
                if (input.equals(validChoice)) {
                    return input;
                }
            }
            
            System.out.println("❌ Invalid choice. Please enter a number from 0-10.");
            System.out.print("Your choice: ");
        }
    }

    private static int getValidIntInput(Scanner scanner, String message, int min, int max) {
        while (true) {
            System.out.print(message);
            try {
                int value = Integer.parseInt(scanner.nextLine().trim());
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println("The value must be between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid integer.");
            }
        }
    }
    
    private static double getValidDoubleInput(Scanner scanner, String message) {
        while (true) {
            System.out.print(message);
            try {
                double value = Double.parseDouble(scanner.nextLine().trim());
                if (value > 0) {
                    return value;
                }
                System.out.println("The value must be a positive number.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid number.");
            }
        }
    }
}