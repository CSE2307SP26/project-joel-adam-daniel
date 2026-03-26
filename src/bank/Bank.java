package bank;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * In-memory bank: holds accounts and supports closing an account when balance is zero.
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
     * Closes an existing account. The account must exist and have a zero balance.
     *
     * @param accountNumber the account to close
     * @return a result describing success or the reason for failure
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
