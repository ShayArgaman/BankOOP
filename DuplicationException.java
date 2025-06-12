public class DuplicationException extends Exception {
    private final int duplicateAccountNumber; // Store the duplicate account number

    // Constructor that takes a message and the duplicate account number as parameters
    public DuplicationException(int duplicateAccountNumber) {
        super("Account number " + duplicateAccountNumber + " already exists.");
        this.duplicateAccountNumber = duplicateAccountNumber;
    }

}
