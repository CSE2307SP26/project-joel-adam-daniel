package test;

import bank.Account;
import bank.AccountType;
import bank.Bank;
import bank.SpendingSummary;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpendingSummaryTest {

    // Use a fixed "now" so tests are deterministic
    private static final long T0 = 1_700_000_000_000L; // arbitrary fixed epoch ms
    private static final long ONE_DAY = 24 * 60 * 60 * 1000L;

    // --- Bank-level validation ---

    @Test
    void unknownAccountReturnsFailure() {
        Bank bank = new Bank();
        Bank.SpendingSummaryResult r = bank.getSpendingSummary("NOPE", T0, T0 + ONE_DAY);
        assertFalse(r.isSuccess());
    }

    @Test
    void blankAccountIdReturnsFailure() {
        Bank bank = new Bank();
        assertFalse(bank.getSpendingSummary("", T0, T0 + ONE_DAY).isSuccess());
    }

    @Test
    void startAfterEndReturnsFailure() {
        Bank bank = new Bank();
        bank.addAccount(new Account("A", 100));
        assertFalse(bank.getSpendingSummary("A", T0 + ONE_DAY, T0).isSuccess());
    }

    @Test
    void validRequestReturnsSuccess() {
        Bank bank = new Bank();
        bank.addAccount(new Account("A", 100));
        assertTrue(bank.getSpendingSummary("A", T0, T0 + ONE_DAY).isSuccess());
        assertNotNull(bank.getSpendingSummary("A", T0, T0 + ONE_DAY).getSummary());
    }

    // --- Deposit counting ---

    @Test
    void depositsInRangeAreCountedAsInflows() {
        Account acc = new Account("A", 0);
        long mid = T0 + ONE_DAY;
        injectTransaction(acc, bank.Transaction.Type.DEPOSIT, 50.0, mid);

        SpendingSummary s = acc.getSpendingSummary(T0, T0 + 2 * ONE_DAY);

        assertEquals(50.0, s.getTotalDeposited(), 0.001);
        assertEquals(0.0, s.getTotalWithdrawn(), 0.001);
        assertEquals(1, s.getDepositCount());
        assertEquals(0, s.getWithdrawalCount());
        assertEquals(50.0, s.getNetChange(), 0.001);
    }

    @Test
    void transferInCountsAsInflow() {
        Account acc = new Account("A", 0);
        long mid = T0 + ONE_DAY;
        injectTransaction(acc, bank.Transaction.Type.TRANSFER_IN, 30.0, mid);

        SpendingSummary s = acc.getSpendingSummary(T0, T0 + 2 * ONE_DAY);

        assertEquals(30.0, s.getTotalDeposited(), 0.001);
        assertEquals(1, s.getDepositCount());
    }

    // --- Withdrawal counting ---

    @Test
    void withdrawalsInRangeAreCountedAsOutflows() {
        Account acc = new Account("A", 200);
        long mid = T0 + ONE_DAY;
        injectTransaction(acc, bank.Transaction.Type.WITHDRAW, 40.0, mid);

        SpendingSummary s = acc.getSpendingSummary(T0, T0 + 2 * ONE_DAY);

        assertEquals(0.0, s.getTotalDeposited(), 0.001);
        assertEquals(40.0, s.getTotalWithdrawn(), 0.001);
        assertEquals(0, s.getDepositCount());
        assertEquals(1, s.getWithdrawalCount());
        assertEquals(-40.0, s.getNetChange(), 0.001);
    }

    @Test
    void transferOutCountsAsOutflow() {
        Account acc = new Account("A", 200);
        long mid = T0 + ONE_DAY;
        injectTransaction(acc, bank.Transaction.Type.TRANSFER_OUT, 25.0, mid);

        SpendingSummary s = acc.getSpendingSummary(T0, T0 + 2 * ONE_DAY);

        assertEquals(25.0, s.getTotalWithdrawn(), 0.001);
        assertEquals(1, s.getWithdrawalCount());
    }

    // --- OPEN transactions excluded ---

    @Test
    void openTransactionIsIgnored() {
        // The OPEN transaction is added by the Account constructor at creation time.
        // It should not appear in deposits or withdrawals in the summary.
        Account acc = new Account("A", 100);

        SpendingSummary s = acc.getSpendingSummary(0, Long.MAX_VALUE);

        assertEquals(0.0, s.getTotalDeposited(), 0.001);
        assertEquals(0.0, s.getTotalWithdrawn(), 0.001);
        assertEquals(0, s.getDepositCount());
        assertEquals(0, s.getWithdrawalCount());
    }

    // --- Date range filtering ---

    @Test
    void transactionsOutsideRangeAreExcluded() {
        Account acc = new Account("A", 200);
        // Before range
        injectTransaction(acc, bank.Transaction.Type.DEPOSIT, 100.0, T0 - ONE_DAY);
        // Inside range
        injectTransaction(acc, bank.Transaction.Type.DEPOSIT, 50.0, T0 + ONE_DAY);
        // After range
        injectTransaction(acc, bank.Transaction.Type.DEPOSIT, 200.0, T0 + 3 * ONE_DAY);

        SpendingSummary s = acc.getSpendingSummary(T0, T0 + 2 * ONE_DAY);

        assertEquals(50.0, s.getTotalDeposited(), 0.001);
        assertEquals(1, s.getDepositCount());
    }

    @Test
    void transactionExactlyOnFromBoundaryIsIncluded() {
        Account acc = new Account("A", 0);
        injectTransaction(acc, bank.Transaction.Type.DEPOSIT, 10.0, T0);

        SpendingSummary s = acc.getSpendingSummary(T0, T0 + ONE_DAY);

        assertEquals(10.0, s.getTotalDeposited(), 0.001);
    }

    @Test
    void transactionExactlyOnToBoundaryIsIncluded() {
        Account acc = new Account("A", 0);
        long end = T0 + ONE_DAY;
        injectTransaction(acc, bank.Transaction.Type.DEPOSIT, 15.0, end);

        SpendingSummary s = acc.getSpendingSummary(T0, end);

        assertEquals(15.0, s.getTotalDeposited(), 0.001);
    }

    @Test
    void emptyRangeWithNoTransactionsReturnsZeroes() {
        Account acc = new Account("A", 100);

        SpendingSummary s = acc.getSpendingSummary(T0, T0 + ONE_DAY);

        assertEquals(0.0, s.getTotalDeposited(), 0.001);
        assertEquals(0.0, s.getTotalWithdrawn(), 0.001);
        assertEquals(0.0, s.getNetChange(), 0.001);
        assertEquals(0, s.getDepositCount());
        assertEquals(0, s.getWithdrawalCount());
    }

    // --- Mixed transactions ---

    @Test
    void mixedTransactionsProduceCorrectTotals() {
        Account acc = new Account("A", 500);
        injectTransaction(acc, bank.Transaction.Type.DEPOSIT, 100.0, T0 + ONE_DAY);
        injectTransaction(acc, bank.Transaction.Type.DEPOSIT, 200.0, T0 + ONE_DAY);
        injectTransaction(acc, bank.Transaction.Type.WITHDRAW, 50.0, T0 + ONE_DAY);
        injectTransaction(acc, bank.Transaction.Type.TRANSFER_IN, 75.0, T0 + ONE_DAY);
        injectTransaction(acc, bank.Transaction.Type.TRANSFER_OUT, 25.0, T0 + ONE_DAY);

        SpendingSummary s = acc.getSpendingSummary(T0, T0 + 2 * ONE_DAY);

        assertEquals(375.0, s.getTotalDeposited(), 0.001);  // 100 + 200 + 75
        assertEquals(75.0, s.getTotalWithdrawn(), 0.001);   // 50 + 25
        assertEquals(3, s.getDepositCount());
        assertEquals(2, s.getWithdrawalCount());
        assertEquals(300.0, s.getNetChange(), 0.001);
    }

    // --- Summary range stored correctly ---

    @Test
    void summaryPreservesRequestedRange() {
        Account acc = new Account("A", 0);
        SpendingSummary s = acc.getSpendingSummary(T0, T0 + ONE_DAY);

        assertEquals(T0, s.getFromMs());
        assertEquals(T0 + ONE_DAY, s.getToMs());
    }

    // --- Via Bank API ---

    @Test
    void bankApiDelegatesCorrectlyToAccount() {
        Bank bank = new Bank();
        bank.addAccount(new Account("X", 300));
        bank.getAccount("X").deposit(80);

        Bank.SpendingSummaryResult r = bank.getSpendingSummary("X", 0, Long.MAX_VALUE);

        assertTrue(r.isSuccess());
        assertEquals(80.0, r.getSummary().getTotalDeposited(), 0.001);
    }

    // --- Helper: inject a transaction directly into an account's history via real operations ---
    // We can't set timestamps directly, so we use real operations and then rely on the
    // fixed-range tests above which use a known time window around System.currentTimeMillis().
    // For timestamp-specific tests we use a package-accessible trick: since Transaction is
    // constructed with an explicit timestamp, we drive it via Account.deposit/withdraw which
    // always uses System.currentTimeMillis(). Instead we use a reflective helper only for
    // injection tests.

    private static void injectTransaction(Account acc,
                                          bank.Transaction.Type type,
                                          double amount,
                                          long whenMs) {
        // Access the transaction history list via reflection to inject at a specific timestamp.
        // This is test-only; production code never does this.
        try {
            java.lang.reflect.Field f = Account.class.getDeclaredField("transactionHistory");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.List<bank.Transaction> history = (java.util.List<bank.Transaction>) f.get(acc);
            history.add(new bank.Transaction(type, amount, whenMs, ""));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
