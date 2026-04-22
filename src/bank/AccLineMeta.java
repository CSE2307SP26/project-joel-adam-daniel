package bank;

/** Parsed metadata from one persisted {@code ACC|...} line (all format variants). */
final class AccLineMeta {
    final String accountId;
    final double balance;
    final AccountType accountType;
    final String savingsPeriodKey;
    final int savingsWithdrawalsThisPeriod;
    final boolean frozen;
    final int persistedPin;
    final int pinFailedAttempts;
    final boolean pinLocked;
    final double minimumBalanceThreshold;

    AccLineMeta(
            String accountId,
            double balance,
            AccountType accountType,
            String savingsPeriodKey,
            int savingsWithdrawalsThisPeriod,
            boolean frozen,
            int persistedPin,
            int pinFailedAttempts,
            boolean pinLocked,
            double minimumBalanceThreshold) {
        this.accountId = accountId;
        this.balance = balance;
        this.accountType = accountType;
        this.savingsPeriodKey = savingsPeriodKey;
        this.savingsWithdrawalsThisPeriod = savingsWithdrawalsThisPeriod;
        this.frozen = frozen;
        this.persistedPin = persistedPin;
        this.pinFailedAttempts = pinFailedAttempts;
        this.pinLocked = pinLocked;
        this.minimumBalanceThreshold = minimumBalanceThreshold;
    }
}
