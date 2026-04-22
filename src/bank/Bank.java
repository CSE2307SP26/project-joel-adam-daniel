package bank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Bank {

    // doubles don't compare cleanly to 0; use this for "balance is basically zero" checks
    private static final double EPS = 1e-9;

    private final Map<String, Account> accounts = new LinkedHashMap<>();
    private final List<RecurringTransfer> recurringTransfers = new ArrayList<>();
    private int nextRecurringTransferId = 1;

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
        return tryCreateAccount(accountId, initialBalance, accountType, Account.DEFAULT_TEST_PIN);
    }

    public CreateAccountResult tryCreateAccount(
            String accountId, double initialBalance, AccountType accountType, int pin) {
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
        if (!PinLogin.isValidPin(pin)) {
            return CreateAccountResult.failure("PIN must be exactly 4 digits (1000-9999).");
        }

        try {
            addAccount(new Account(accountId, initialBalance, accountType, pin));
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

    public OperatorResult setAccountFrozen(String accountNumber, boolean freeze) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return OperatorResult.failure("Account id cannot be empty.");
        }
        Account acc = accounts.get(accountNumber);
        if (acc == null) {
            return OperatorResult.failure("Account not found: " + accountNumber);
        }
        acc.setFrozen(freeze);
        return OperatorResult.success(
                freeze
                        ? "Account " + accountNumber + " is now frozen."
                        : "Account " + accountNumber + " is now unfrozen.");
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

        if (from.isFrozen()) {
            return TransferResult.failure("Source account is frozen.");
        }
        if (to.isFrozen()) {
            return TransferResult.failure("Destination account is frozen.");
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

    public AddRecurringTransferResult addRecurringTransfer(
            String fromId, String toId, double amount, int intervalDays) {
        if (fromId == null || fromId.isBlank() || toId == null || toId.isBlank()) {
            return AddRecurringTransferResult.failure("From and to account numbers are required.");
        }
        if (fromId.equals(toId)) {
            return AddRecurringTransferResult.failure("Cannot transfer to the same account.");
        }
        if (amount <= 0 || Double.isNaN(amount)) {
            return AddRecurringTransferResult.failure("Transfer amount must be positive.");
        }
        if (intervalDays <= 0) {
            return AddRecurringTransferResult.failure("Interval must be at least 1 day.");
        }
        if (accounts.get(fromId) == null) {
            return AddRecurringTransferResult.failure("Source account not found: " + fromId);
        }
        if (accounts.get(toId) == null) {
            return AddRecurringTransferResult.failure("Destination account not found: " + toId);
        }

        String id = "RT-" + nextRecurringTransferId++;
        long nextRunMs = System.currentTimeMillis() + (long) intervalDays * 24 * 60 * 60 * 1000L;
        recurringTransfers.add(new RecurringTransfer(id, fromId, toId, amount, intervalDays, nextRunMs, true));
        return AddRecurringTransferResult.success(id);
    }

    public OperatorResult cancelRecurringTransfer(String id) {
        if (id == null || id.isBlank()) {
            return OperatorResult.failure("Recurring transfer id cannot be empty.");
        }
        for (RecurringTransfer rt : recurringTransfers) {
            if (rt.getId().equals(id)) {
                if (!rt.isActive()) {
                    return OperatorResult.failure("Recurring transfer " + id + " is already cancelled.");
                }
                rt.cancel();
                return OperatorResult.success("Recurring transfer " + id + " cancelled.");
            }
        }
        return OperatorResult.failure("Recurring transfer not found: " + id);
    }

    public Collection<RecurringTransfer> getAllRecurringTransfers() {
        return Collections.unmodifiableList(recurringTransfers);
    }

    public List<ProcessedRecurringTransferResult> processDueRecurringTransfers() {
        long now = System.currentTimeMillis();
        List<ProcessedRecurringTransferResult> results = new ArrayList<>();
        for (RecurringTransfer rt : recurringTransfers) {
            if (!rt.isActive() || rt.getNextRunMs() > now) {
                continue;
            }
            TransferResult txResult = transfer(rt.getFromAccountId(), rt.getToAccountId(), rt.getAmount());
            long intervalMs = (long) rt.getIntervalDays() * 24 * 60 * 60 * 1000L;
            rt.setNextRunMs(rt.getNextRunMs() + intervalMs);
            results.add(new ProcessedRecurringTransferResult(rt.getId(), txResult.isSuccess(), txResult.getMessage()));
        }
        return results;
    }

    public void addRecurringTransferForPersistence(RecurringTransfer rt) {
        recurringTransfers.add(rt);
        int num = parseRtIdNumber(rt.getId());
        if (num >= nextRecurringTransferId) {
            nextRecurringTransferId = num + 1;
        }
    }

    private static int parseRtIdNumber(String id) {
        if (id != null && id.startsWith("RT-")) {
            try {
                return Integer.parseInt(id.substring(3));
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
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

    public static final class OperatorResult {
        private final boolean success;
        private final String message;

        private OperatorResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static OperatorResult success(String message) {
            return new OperatorResult(true, message);
        }

        public static OperatorResult failure(String message) {
            return new OperatorResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    public static final class AddRecurringTransferResult {
        private final boolean success;
        private final String message;
        private final String recurringTransferId;

        private AddRecurringTransferResult(boolean success, String message, String recurringTransferId) {
            this.success = success;
            this.message = message;
            this.recurringTransferId = recurringTransferId;
        }

        public static AddRecurringTransferResult success(String id) {
            return new AddRecurringTransferResult(true, "Recurring transfer " + id + " created.", id);
        }

        public static AddRecurringTransferResult failure(String message) {
            return new AddRecurringTransferResult(false, message, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getRecurringTransferId() {
            return recurringTransferId;
        }
    }

    public static final class ProcessedRecurringTransferResult {
        private final String recurringTransferId;
        private final boolean success;
        private final String message;

        public ProcessedRecurringTransferResult(String recurringTransferId, boolean success, String message) {
            this.recurringTransferId = recurringTransferId;
            this.success = success;
            this.message = message;
        }

        public String getRecurringTransferId() {
            return recurringTransferId;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
