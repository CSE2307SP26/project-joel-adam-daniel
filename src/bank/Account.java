package bank;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Account {

    public static final double SAVINGS_MIN_OPENING_BALANCE = 50.0;
    public static final int SAVINGS_MAX_WITHDRAWALS_PER_MONTH = 6;

    /** Used when tests or legacy constructors omit an explicit PIN. */
    public static final int DEFAULT_TEST_PIN = 1234;

    public static final double DEFAULT_MINIMUM_BALANCE_THRESHOLD = 25.0;

    private static final double EPS = 1e-9;

    private final String accountNumber;
    private double balance;
    private final List<Transaction> transactionHistory;
    private final AccountType accountType;

    private String savingsPeriodKey;
    private int savingsWithdrawalsThisPeriod;

    private boolean frozen;

    private final PinLogin pinLogin;

    private double minimumBalanceThreshold;

    public Account(String accountNumber, double initialBalance) {
        this(accountNumber, initialBalance, AccountType.CHECKING, DEFAULT_TEST_PIN);
    }

    public Account(String accountNumber, double initialBalance, AccountType accountType) {
        this(accountNumber, initialBalance, accountType, DEFAULT_TEST_PIN);
    }

    public Account(String accountNumber, double initialBalance, AccountType accountType, int pin) {
        if (accountType == AccountType.SAVINGS && initialBalance + EPS < SAVINGS_MIN_OPENING_BALANCE) {
            throw new IllegalArgumentException(
                    "Savings accounts require a minimum opening deposit of $"
                            + SAVINGS_MIN_OPENING_BALANCE);
        }

        this.accountNumber = accountNumber;
        this.balance = initialBalance;
        this.transactionHistory = new ArrayList<>();
        this.accountType = accountType;
        this.savingsPeriodKey = currentYearMonthKey();
        this.savingsWithdrawalsThisPeriod = 0;
        this.frozen = false;
        this.pinLogin = new PinLogin(pin);
        this.minimumBalanceThreshold = DEFAULT_MINIMUM_BALANCE_THRESHOLD;

        long now = System.currentTimeMillis();
        String openDetail = buildOpenDetail(initialBalance, accountType);
        transactionHistory.add(new Transaction(Transaction.Type.OPEN, initialBalance, now, openDetail));
    }

    public static Account fromPersisted(
            String accountNumber,
            double balance,
            List<Transaction> fullHistory,
            AccountType accountType,
            String savingsPeriodKey,
            int savingsWithdrawalsThisPeriod,
            boolean frozen,
            int persistedPin,
            int pinFailedAttempts,
            boolean pinLocked,
            double minimumBalanceThreshold) {
        return new Account(
                accountNumber,
                balance,
                new ArrayList<>(fullHistory),
                accountType,
                savingsPeriodKey,
                savingsWithdrawalsThisPeriod,
                frozen,
                PinLogin.restoredForPersistence(persistedPin, pinFailedAttempts, pinLocked),
                minimumBalanceThreshold);
    }

    private Account(
            String accountNumber,
            double balance,
            List<Transaction> persistedHistory,
            AccountType accountType,
            String savingsPeriodKey,
            int savingsWithdrawalsThisPeriod,
            boolean frozen,
            PinLogin pinLogin,
            double minimumBalanceThreshold) {
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.transactionHistory = persistedHistory;
        this.accountType = accountType;
        this.savingsPeriodKey =
                savingsPeriodKey != null && !savingsPeriodKey.isEmpty()
                        ? savingsPeriodKey
                        : currentYearMonthKey();
        this.savingsWithdrawalsThisPeriod = savingsWithdrawalsThisPeriod;
        this.frozen = frozen;
        this.pinLogin = pinLogin;
        if (minimumBalanceThreshold < 0) {
            throw new IllegalArgumentException("Minimum balance threshold cannot be negative.");
        }
        this.minimumBalanceThreshold = minimumBalanceThreshold;
    }

    private static String buildOpenDetail(double initialBalance, AccountType accountType) {
        String typeWord = accountType == AccountType.CHECKING ? "Checking" : "Savings";
        String bal =
                initialBalance == 0 ? "Account opened" : "Opened with balance " + initialBalance;
        return typeWord + " — " + bal;
    }

    private static String currentYearMonthKey() {
        return YearMonth.now(ZoneId.systemDefault()).toString();
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public double getBalance() {
        return balance;
    }

    public List<Transaction> getTransactionHistory() {
        return Collections.unmodifiableList(transactionHistory);
    }

    public int getSavingsWithdrawalsRemainingThisMonth() {
        if (accountType != AccountType.SAVINGS) {
            return -1;
        }
        syncSavingsPeriod();
        return Math.max(0, SAVINGS_MAX_WITHDRAWALS_PER_MONTH - savingsWithdrawalsThisPeriod);
    }

    public String getAccountRulesSummary() {
        if (accountType == AccountType.CHECKING) {
            return "Checking — no monthly withdrawal limit.";
        }
        syncSavingsPeriod();
        int left = Math.max(0, SAVINGS_MAX_WITHDRAWALS_PER_MONTH - savingsWithdrawalsThisPeriod);
        return "Savings — min. opening deposit was $"
                + SAVINGS_MIN_OPENING_BALANCE
                + "; up to "
                + SAVINGS_MAX_WITHDRAWALS_PER_MONTH
                + " withdrawals per month ("
                + left
                + " remaining this month).";
    }

    public List<Transaction> getTransactionHistoryByType(Transaction.Type type) {
        List<Transaction> out = new ArrayList<>();

        for (Transaction t : transactionHistory) {
            if (t.getType() == type) {
                out.add(t);
            }
        }

        return Collections.unmodifiableList(out);
    }

    public boolean authenticatePin(int enteredPin) {
        return pinLogin.authenticate(enteredPin);
    }

    public boolean isPinLocked() {
        return pinLogin.isLocked();
    }

    public int getPinRemainingAttempts() {
        return pinLogin.getRemainingAttempts();
    }

    public void setMinimumBalanceThreshold(double threshold) {
        if (threshold < 0) {
            throw new IllegalArgumentException("Minimum balance threshold cannot be negative.");
        }
        this.minimumBalanceThreshold = threshold;
    }

    public double getMinimumBalanceThreshold() {
        return minimumBalanceThreshold;
    }

    public boolean isFrozen() {
        return frozen;
    }

    void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    private void ensureNotFrozen() {
        if (frozen) {
            throw new IllegalStateException("This account is frozen.");
        }
    }

    private void maybeWarnMinimumBalance() {
        if (minimumBalanceThreshold <= 0) {
            return;
        }
        if (balance < minimumBalanceThreshold) {
            System.out.printf(
                    "Warning: Your balance ($%.2f) is below the minimum balance of $%.2f.%n",
                    balance, minimumBalanceThreshold);
        }
    }

    PinLogin pinLoginForPersistence() {
        return pinLogin;
    }

    public void deposit(double amount) {
        ensureNotFrozen();
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }

        balance += amount;
        transactionHistory.add(
                new Transaction(Transaction.Type.DEPOSIT, amount, System.currentTimeMillis(), ""));
    }

    public void withdraw(double amount) {
        ensureNotFrozen();
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        assertSavingsWithdrawalAllowed();
        if (balance < amount) {
            throw new IllegalStateException("Insufficient funds");
        }

        balance -= amount;
        transactionHistory.add(
                new Transaction(Transaction.Type.WITHDRAW, amount, System.currentTimeMillis(), ""));
        recordSavingsOutgoingWithdrawal();
        maybeWarnMinimumBalance();
    }

    public void transferIn(double amount) {
        ensureNotFrozen();
        if (amount <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        balance += amount;
        transactionHistory.add(
                new Transaction(
                        Transaction.Type.TRANSFER_IN, amount, System.currentTimeMillis(), "Transfer in"));
    }

    public void transferOut(double amount) {
        ensureNotFrozen();
        if (amount <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        assertSavingsWithdrawalAllowed();
        if (balance < amount) {
            throw new IllegalStateException("Insufficient funds");
        }

        balance -= amount;
        transactionHistory.add(
                new Transaction(
                        Transaction.Type.TRANSFER_OUT, amount, System.currentTimeMillis(), "Transfer out"));
        recordSavingsOutgoingWithdrawal();
        maybeWarnMinimumBalance();
    }

    private void syncSavingsPeriod() {
        if (accountType != AccountType.SAVINGS) {
            return;
        }
        String nowKey = currentYearMonthKey();
        if (!nowKey.equals(savingsPeriodKey)) {
            savingsPeriodKey = nowKey;
            savingsWithdrawalsThisPeriod = 0;
        }
    }

    private void assertSavingsWithdrawalAllowed() {
        if (accountType != AccountType.SAVINGS) {
            return;
        }
        syncSavingsPeriod();
        if (savingsWithdrawalsThisPeriod >= SAVINGS_MAX_WITHDRAWALS_PER_MONTH) {
            throw new IllegalStateException(
                    "Savings accounts allow at most "
                            + SAVINGS_MAX_WITHDRAWALS_PER_MONTH
                            + " withdrawals per calendar month.");
        }
    }

    private void recordSavingsOutgoingWithdrawal() {
        if (accountType != AccountType.SAVINGS) {
            return;
        }
        syncSavingsPeriod();
        savingsWithdrawalsThisPeriod++;
    }

    String getSavingsPeriodKeyForPersistence() {
        return savingsPeriodKey;
    }

    int getSavingsWithdrawalsThisPeriodForPersistence() {
        return savingsWithdrawalsThisPeriod;
    }
}
