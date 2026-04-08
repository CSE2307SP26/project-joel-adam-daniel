package bank;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class Bank {

    // doubles don't compare cleanly to 0; use this for "balance is basically zero" checks
    private static final double EPS = 1e-9;

    private final Map<String, Account> accounts = new LinkedHashMap<>();

    public void addAccount(Account account) {
        accounts.put(account.getAccountNumber(), account);
    }

    public Account getAccount(String accountNumber) {
        return accounts.get(accountNumber);
    }

    public int getAccountCount() {
        return accounts.size();
    }

    public Collection<Account> getAllAccounts() {
        return accounts.values();
    }

    public CreateAccountResult tryCreateAccount(String accountId, double initialBalance) {
        return tryCreateAccount(accountId, initialBalance, AccountType.CHECKING);
    }

    public CreateAccountResult tryCreateAccount(
            String accountId, double initialBalance, AccountType accountType) {
        if (accountId == null || accountId.isBlank()) {
            return CreateAccountResult.failure("Account id cannot be empty.");
        }
        if (accounts.containsKey(accountId)) {
            return CreateAccountResult.failure("An account with that id already exists.");
        }
        if (initialBalance < 0 || Double.isNaN(initialBalance)) {
            return CreateAccountResult.failure("Initial balance cannot be negative.");
        }
        if (accountType == AccountType.SAVINGS
                && initialBalance + EPS < Account.SAVINGS_MIN_OPENING_BALANCE) {
            return CreateAccountResult.failure(
                    "Savings accounts require a minimum opening deposit of $"
                            + Account.SAVINGS_MIN_OPENING_BALANCE
                            + ".");
        }

        try {
            addAccount(new Account(accountId, initialBalance, accountType));
        } catch (IllegalArgumentException e) {
            return CreateAccountResult.failure(e.getMessage());
        }

        return CreateAccountResult.success(
                "Created "
                        + accountType.getLabel().toLowerCase()
                        + " account "
                        + accountId
                        + ".");
    }

    public CloseAccountResult closeCustomerAccount(String accountNumber) {
        if (accounts.size() <= 1) {
            return CloseAccountResult.failure("Cannot close the only remaining account.");
        }

        return closeAccount(accountNumber);
    }

    public TransferResult transfer(String fromNumber, String toNumber, double amount) {
        if (fromNumber == null || fromNumber.isBlank() || toNumber == null || toNumber.isBlank()) {
            return TransferResult.failure("From and to account numbers are required.");
        }
        if (fromNumber.equals(toNumber)) {
            return TransferResult.failure("Cannot transfer to the same account.");
        }
        if (amount <= 0 || Double.isNaN(amount)) {
            return TransferResult.failure("Transfer amount must be positive.");
        }

        Account from = accounts.get(fromNumber);
        Account to = accounts.get(toNumber);

        if (from == null) {
            return TransferResult.failure("Source account not found: " + fromNumber);
        }
        if (to == null) {
            return TransferResult.failure("Destination account not found: " + toNumber);
        }

        if (from.getBalance() + EPS < amount) {
            return TransferResult.failure(
                    "Insufficient funds. Balance: " + from.getBalance() + ", requested: " + amount);
        }

        try {
            from.transferOut(amount);
            to.transferIn(amount);
        } catch (IllegalStateException e) {
            return TransferResult.failure(e.getMessage());
        }

        return TransferResult.success(
                "Transferred " + amount + " from " + fromNumber + " to " + toNumber + ".");
    }

    public CloseAccountResult closeAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return CloseAccountResult.failure("No account selected.");
        }

        Account acc = accounts.get(accountNumber);
        if (acc == null) {
            return CloseAccountResult.failure("Account not found: " + accountNumber);
        }

        if (Math.abs(acc.getBalance()) > EPS) {
            return CloseAccountResult.failure(
                    "Balance must be zero to close this account. Current balance: " + acc.getBalance());
        }

        accounts.remove(accountNumber);
        return CloseAccountResult.success();
    }

    public static final class CreateAccountResult {
        private final boolean success;
        private final String message;

        private CreateAccountResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static CreateAccountResult success(String message) {
            return new CreateAccountResult(true, message);
        }

        public static CreateAccountResult failure(String message) {
            return new CreateAccountResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    public static final class TransferResult {
        private final boolean success;
        private final String message;

        private TransferResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static TransferResult success(String message) {
            return new TransferResult(true, message);
        }

        public static TransferResult failure(String message) {
            return new TransferResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    public static final class CloseAccountResult {
        private final boolean success;
        private final String message;

        private CloseAccountResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static CloseAccountResult success() {
            return new CloseAccountResult(true, "Account closed.");
        }

        public static CloseAccountResult failure(String message) {
            return new CloseAccountResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
