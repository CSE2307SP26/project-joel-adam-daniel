package bank;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * In-memory bank: accounts, closing (zero balance), and transfers between accounts.
 */
public class Bank {
    private final Map<String, Account> accounts = new LinkedHashMap<>();

    public void addAccount(Account account) {
        accounts.put(account.getAccountNumber(), account);
    }

    public Account getAccount(String accountNumber) {
        return accounts.get(accountNumber);
    }

    public Collection<Account> getAllAccounts() {
        return accounts.values();
    }

    /**
     * Moves money from one account to another. Both must exist, be different, and the source must have enough funds.
     */
    public TransferResult transfer(String fromNumber, String toNumber, BigDecimal amount) {
        if (fromNumber == null || fromNumber.isBlank() || toNumber == null || toNumber.isBlank()) {
            return TransferResult.failure("From and to account numbers are required.");
        }
        if (fromNumber.equals(toNumber)) {
            return TransferResult.failure("Cannot transfer to the same account.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
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
        if (from.getBalance().compareTo(amount) < 0) {
            return TransferResult.failure(
                    "Insufficient funds. Balance: " + from.getBalance() + ", requested: " + amount);
        }
        from.withdraw(amount);
        to.deposit(amount);
        return TransferResult.success(
                "Transferred " + amount + " from " + fromNumber + " to " + toNumber + ".");
    }

    /**
     * Closes an existing account. The account must exist and have a zero balance.
     */
    public CloseAccountResult closeAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return CloseAccountResult.failure("No account selected.");
        }
        Account acc = accounts.get(accountNumber);
        if (acc == null) {
            return CloseAccountResult.failure("Account not found: " + accountNumber);
        }
        if (acc.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            return CloseAccountResult.failure(
                    "Balance must be zero to close this account. Current balance: " + acc.getBalance());
        }
        accounts.remove(accountNumber);
        return CloseAccountResult.success();
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
