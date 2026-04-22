package test;

import bank.Account;
import bank.AccountType;
import bank.Bank;
import bank.SpendingSummary;
import bank.Transaction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for {@link Account} behavior (balance, history, savings rules, spending aggregation, etc.). */
class AccountTest {

    private static final long T0 = 1_700_000_000_000L;
    private static final long ONE_DAY = 24 * 60 * 60 * 1000L;

    @Test
    void depositIncreasesBalanceAndRecordsTransaction() {
        Bank bank = new Bank();
        bank.addAccount(new Account("X1", 10.00));
        Account a = bank.getAccount("X1");
        a.deposit(25.50);
        assertEquals(35.50, a.getBalance(), 0.01);
        assertTrue(
                a.getTransactionHistory().stream()
                        .anyMatch(
                                t ->
                                        t.getType() == Transaction.Type.DEPOSIT
                                                && Math.abs(t.getAmount() - 25.50) < 0.01));
    }

    @Test
    void withdrawDecreasesBalanceAndRecordsTransaction() {
        Bank bank = new Bank();
        bank.addAccount(new Account("W1", 100.00));
        Account a = bank.getAccount("W1");
        a.withdraw(40.25);
        assertEquals(59.75, a.getBalance(), 0.01);
        assertTrue(
                a.getTransactionHistory().stream()
                        .anyMatch(
                                t ->
                                        t.getType() == Transaction.Type.WITHDRAW
                                                && Math.abs(t.getAmount() - 40.25) < 0.01));
    }

    @Test
    void withdrawMoreThanBalanceFails() {
        Account a = new Account("W2", 10.00);
        assertThrows(IllegalStateException.class, () -> a.withdraw(11));
    }

    @Test
    void balanceTracksDepositsAndWithdrawals() {
        Bank bank = new Bank();
        bank.addAccount(new Account("B1", 0));
        Account a = bank.getAccount("B1");
        assertEquals(0, a.getBalance(), 0.001);
        a.deposit(15.10);
        assertEquals(15.10, a.getBalance(), 0.01);
        a.withdraw(0.10);
        assertEquals(15.00, a.getBalance(), 0.01);
    }

    @Test
    void historyListsOpenDepositWithdrawInOrder() {
        Account a = new Account("H1", 0);
        a.deposit(5);
        a.withdraw(2);
        assertTrue(a.getTransactionHistory().get(0).getType() == Transaction.Type.OPEN);
        assertEquals(Transaction.Type.DEPOSIT, a.getTransactionHistory().get(1).getType());
        assertEquals(Transaction.Type.WITHDRAW, a.getTransactionHistory().get(2).getType());
        assertEquals(3, a.getBalance(), 0.001);
    }

    @Test
    void filterByTypeReturnsOnlyMatchingEntries() {
        Account a = new Account("F1", 0);
        a.deposit(10);
        a.withdraw(3);
        assertEquals(1, a.getTransactionHistoryByType(Transaction.Type.OPEN).size());
        assertEquals(1, a.getTransactionHistoryByType(Transaction.Type.DEPOSIT).size());
        assertEquals(1, a.getTransactionHistoryByType(Transaction.Type.WITHDRAW).size());
        assertEquals(0, a.getTransactionHistoryByType(Transaction.Type.TRANSFER_IN).size());
    }

    @Test
    void filterByTransferTypes() {
        Account a = new Account("T1", 100);
        a.transferOut(10);
        a.transferIn(5);
        assertEquals(1, a.getTransactionHistoryByType(Transaction.Type.TRANSFER_OUT).size());
        assertEquals(1, a.getTransactionHistoryByType(Transaction.Type.TRANSFER_IN).size());
    }

    @Test
    void getTransactionsBetweenIsInclusiveOnTimestamps() {
        Account a = new Account("ST", 0);
        injectTransaction(a, Transaction.Type.DEPOSIT, 5, T0);
        injectTransaction(a, Transaction.Type.DEPOSIT, 7, T0 + ONE_DAY);
        injectTransaction(a, Transaction.Type.DEPOSIT, 9, T0 + 3 * ONE_DAY);

        assertEquals(1, a.getTransactionsBetween(T0, T0).size());
        assertEquals(2, a.getTransactionsBetween(T0, T0 + ONE_DAY).size());
        assertEquals(0, a.getTransactionsBetween(T0 + 2 * ONE_DAY, T0 + 2 * ONE_DAY).size());
    }

    @Test
    void administratorFeeRecordsFeeTransaction() {
        Account a = new Account("AF", 50);
        a.setMinimumBalanceThreshold(0);
        a.applyAdministratorFee(12.5, "service charge");
        assertEquals(37.5, a.getBalance(), 0.001);
        assertEquals(1, a.getTransactionHistoryByType(Transaction.Type.FEE).size());
        assertEquals(12.5, a.getTransactionHistoryByType(Transaction.Type.FEE).get(0).getAmount(), 0.001);
    }

    @Test
    void administratorInterestRecordsInterestTransaction() {
        Account a = new Account("AI", 40);
        a.applyAdministratorInterest(2, "promotional");
        assertEquals(42, a.getBalance(), 0.001);
        assertEquals(1, a.getTransactionHistoryByType(Transaction.Type.INTEREST).size());
    }

    @Test
    void administratorFeeRejectsNonPositiveAmount() {
        Account a = new Account("AF2", 10);
        assertThrows(IllegalArgumentException.class, () -> a.applyAdministratorFee(0, ""));
        assertThrows(IllegalArgumentException.class, () -> a.applyAdministratorFee(-1, ""));
    }

    @Test
    void checkingAccountRulesSummaryMentionsNoMonthlyLimit() {
        Account c = new Account("R1", 0, AccountType.CHECKING);
        String s = c.getAccountRulesSummary();
        assertTrue(s.contains("Checking"));
        assertTrue(s.toLowerCase().contains("no monthly withdrawal limit"));
    }

    @Test
    void savingsAccountRulesSummaryMentionsMonthlyWithdrawalCap() {
        Account s = new Account("R2", 100, AccountType.SAVINGS);
        String summary = s.getAccountRulesSummary();
        assertTrue(summary.contains("Savings"));
        assertTrue(summary.contains(String.valueOf(Account.SAVINGS_MAX_WITHDRAWALS_PER_MONTH)));
        assertTrue(summary.contains("withdrawals per month"));
    }

    @Test
    void checkingWithdrawalsRemainingIsNotApplicable() {
        Account c = new Account("Q1", 10, AccountType.CHECKING);
        assertEquals(-1, c.getSavingsWithdrawalsRemainingThisMonth());
    }

    @Test
    void savingsWithdrawalsRemainingDecrementsAfterWithdrawals() {
        Account s = new Account("Q2", 1_000, AccountType.SAVINGS);
        s.setMinimumBalanceThreshold(0);
        assertEquals(Account.SAVINGS_MAX_WITHDRAWALS_PER_MONTH, s.getSavingsWithdrawalsRemainingThisMonth());
        s.withdraw(1);
        s.withdraw(1);
        assertEquals(Account.SAVINGS_MAX_WITHDRAWALS_PER_MONTH - 2, s.getSavingsWithdrawalsRemainingThisMonth());
    }

    @Test
    void savingsManyTransferInsDoNotConsumeWithdrawalQuota() {
        Account s = new Account("Q3", 10_000, AccountType.SAVINGS);
        s.setMinimumBalanceThreshold(0);
        for (int i = 0; i < 20; i++) {
            s.transferIn(1);
        }
        assertEquals(Account.SAVINGS_MAX_WITHDRAWALS_PER_MONTH, s.getSavingsWithdrawalsRemainingThisMonth());
    }

    @Test
    void depositRejectsNonPositiveAmount() {
        Account a = new Account("V0", 10);
        assertThrows(IllegalArgumentException.class, () -> a.deposit(0));
        assertThrows(IllegalArgumentException.class, () -> a.deposit(-1));
    }

    @Test
    void withdrawRejectsNonPositiveAmount() {
        Account a = new Account("V1", 10);
        assertThrows(IllegalArgumentException.class, () -> a.withdraw(0));
        assertThrows(IllegalArgumentException.class, () -> a.withdraw(-0.01));
    }

    @Test
    void transferInRejectsNonPositiveAmount() {
        Account a = new Account("V2", 10);
        assertThrows(IllegalArgumentException.class, () -> a.transferIn(0));
        assertThrows(IllegalArgumentException.class, () -> a.transferIn(-5));
    }

    @Test
    void transferOutRejectsNonPositiveAmount() {
        Account a = new Account("V3", 10);
        assertThrows(IllegalArgumentException.class, () -> a.transferOut(0));
        assertThrows(IllegalArgumentException.class, () -> a.transferOut(-1));
    }

    @Test
    void authenticatePinSuccessResetsAttempts() {
        Account a = new Account("PIN1", 0, AccountType.CHECKING, 4242);
        assertTrue(a.authenticatePin(4242));
        assertEquals(3, a.getPinRemainingAttempts());
    }

    @Test
    void authenticatePinFailureDecrementsAttempts() {
        Account a = new Account("PIN2", 0, AccountType.CHECKING, 4242);
        assertFalse(a.authenticatePin(1111));
        assertEquals(2, a.getPinRemainingAttempts());
    }

    @Test
    void checkingAllowsManyWithdrawals() {
        Account c = new Account("C", 1_000, AccountType.CHECKING);
        c.setMinimumBalanceThreshold(0);
        for (int i = 0; i < 10; i++) {
            c.withdraw(1);
        }
        assertEquals(990, c.getBalance(), 0.01);
    }

    @Test
    void savingsBlocksSeventhWithdrawalInSameMonth() {
        Account s = new Account("S", 1_000, AccountType.SAVINGS);
        s.setMinimumBalanceThreshold(0);
        for (int i = 0; i < Account.SAVINGS_MAX_WITHDRAWALS_PER_MONTH; i++) {
            s.withdraw(1);
        }
        assertThrows(IllegalStateException.class, () -> s.withdraw(1));
    }

    @Test
    void savingsTransferOutCountsTowardMonthlyLimit() {
        Account s = new Account("S", 500, AccountType.SAVINGS);
        s.setMinimumBalanceThreshold(0);
        Bank bank = new Bank();
        bank.addAccount(s);
        Account t = new Account("T", 0, AccountType.CHECKING);
        t.setMinimumBalanceThreshold(0);
        bank.addAccount(t);

        for (int i = 0; i < Account.SAVINGS_MAX_WITHDRAWALS_PER_MONTH; i++) {
            assertTrue(bank.transfer("S", "T", 1).isSuccess());
        }
        assertFalse(bank.transfer("S", "T", 1).isSuccess());
    }

    @Test
    void frozenBlocksDepositWithdrawAndTransfer() {
        Bank bank = new Bank();
        bank.addAccount(new Account("F1", 100));
        bank.setAccountFrozen("F1", true);
        Account a = bank.getAccount("F1");
        assertThrows(IllegalStateException.class, () -> a.deposit(1));
        assertThrows(IllegalStateException.class, () -> a.withdraw(1));
        assertThrows(IllegalStateException.class, () -> a.transferIn(1));
        assertThrows(IllegalStateException.class, () -> a.transferOut(1));
    }

    /**
     * {@link Account#getSpendingSummary(long, long)} aggregation (uses reflection to inject
     * transactions at specific timestamps).
     */
    @Nested
    class SpendingSummaryAggregation {

        @Test
        void depositsInRangeAreCountedAsInflows() {
            Account acc = new Account("A", 0);
            long mid = T0 + ONE_DAY;
            injectTransaction(acc, Transaction.Type.DEPOSIT, 50.0, mid);

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
            injectTransaction(acc, Transaction.Type.TRANSFER_IN, 30.0, mid);

            SpendingSummary s = acc.getSpendingSummary(T0, T0 + 2 * ONE_DAY);

            assertEquals(30.0, s.getTotalDeposited(), 0.001);
            assertEquals(1, s.getDepositCount());
        }

        @Test
        void withdrawalsInRangeAreCountedAsOutflows() {
            Account acc = new Account("A", 200);
            long mid = T0 + ONE_DAY;
            injectTransaction(acc, Transaction.Type.WITHDRAW, 40.0, mid);

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
            injectTransaction(acc, Transaction.Type.TRANSFER_OUT, 25.0, mid);

            SpendingSummary s = acc.getSpendingSummary(T0, T0 + 2 * ONE_DAY);

            assertEquals(25.0, s.getTotalWithdrawn(), 0.001);
            assertEquals(1, s.getWithdrawalCount());
        }

        @Test
        void openTransactionIsIgnored() {
            Account acc = new Account("A", 100);

            SpendingSummary s = acc.getSpendingSummary(0, Long.MAX_VALUE);

            assertEquals(0.0, s.getTotalDeposited(), 0.001);
            assertEquals(0.0, s.getTotalWithdrawn(), 0.001);
            assertEquals(0, s.getDepositCount());
            assertEquals(0, s.getWithdrawalCount());
        }

        @Test
        void transactionsOutsideRangeAreExcluded() {
            Account acc = new Account("A", 200);
            injectTransaction(acc, Transaction.Type.DEPOSIT, 100.0, T0 - ONE_DAY);
            injectTransaction(acc, Transaction.Type.DEPOSIT, 50.0, T0 + ONE_DAY);
            injectTransaction(acc, Transaction.Type.DEPOSIT, 200.0, T0 + 3 * ONE_DAY);

            SpendingSummary s = acc.getSpendingSummary(T0, T0 + 2 * ONE_DAY);

            assertEquals(50.0, s.getTotalDeposited(), 0.001);
            assertEquals(1, s.getDepositCount());
        }

        @Test
        void transactionExactlyOnFromBoundaryIsIncluded() {
            Account acc = new Account("A", 0);
            injectTransaction(acc, Transaction.Type.DEPOSIT, 10.0, T0);

            SpendingSummary s = acc.getSpendingSummary(T0, T0 + ONE_DAY);

            assertEquals(10.0, s.getTotalDeposited(), 0.001);
        }

        @Test
        void transactionExactlyOnToBoundaryIsIncluded() {
            Account acc = new Account("A", 0);
            long end = T0 + ONE_DAY;
            injectTransaction(acc, Transaction.Type.DEPOSIT, 15.0, end);

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

        @Test
        void mixedTransactionsProduceCorrectTotals() {
            Account acc = new Account("A", 500);
            injectTransaction(acc, Transaction.Type.DEPOSIT, 100.0, T0 + ONE_DAY);
            injectTransaction(acc, Transaction.Type.DEPOSIT, 200.0, T0 + ONE_DAY);
            injectTransaction(acc, Transaction.Type.WITHDRAW, 50.0, T0 + ONE_DAY);
            injectTransaction(acc, Transaction.Type.TRANSFER_IN, 75.0, T0 + ONE_DAY);
            injectTransaction(acc, Transaction.Type.TRANSFER_OUT, 25.0, T0 + ONE_DAY);

            SpendingSummary s = acc.getSpendingSummary(T0, T0 + 2 * ONE_DAY);

            assertEquals(375.0, s.getTotalDeposited(), 0.001);
            assertEquals(75.0, s.getTotalWithdrawn(), 0.001);
            assertEquals(3, s.getDepositCount());
            assertEquals(2, s.getWithdrawalCount());
            assertEquals(300.0, s.getNetChange(), 0.001);
        }

        @Test
        void summaryPreservesRequestedRange() {
            Account acc = new Account("A", 0);
            SpendingSummary s = acc.getSpendingSummary(T0, T0 + ONE_DAY);

            assertEquals(T0, s.getFromMs());
            assertEquals(T0 + ONE_DAY, s.getToMs());
        }
    }

    private static void injectTransaction(Account acc, Transaction.Type type, double amount, long whenMs) {
        try {
            java.lang.reflect.Field f = Account.class.getDeclaredField("transactionHistory");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.List<Transaction> history = (java.util.List<Transaction>) f.get(acc);
            history.add(new Transaction(type, amount, whenMs, ""));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    class MinimumBalanceWarning {

        private final PrintStream originalOut = System.out;
        private ByteArrayOutputStream capturedOut;

        @BeforeEach
        void redirectOutput() {
            capturedOut = new ByteArrayOutputStream();
            System.setOut(new PrintStream(capturedOut));
        }

        @AfterEach
        void restoreOutput() {
            System.setOut(originalOut);
        }

        @Test
        void defaultThresholdIsTwentyFive() {
            Account account = new Account("A", 0);
            assertEquals(25.00, account.getMinimumBalanceThreshold(), 0.01);
        }

        @Test
        void noWarningWhenBalanceAboveThreshold() {
            Account account = new Account("B", 0);
            account.deposit(100);
            account.withdraw(50);
            assertFalse(capturedOut.toString().contains("Warning"));
        }

        @Test
        void noWarningWhenBalanceEqualsThreshold() {
            Account account = new Account("C", 0);
            account.deposit(100);
            account.withdraw(75);
            assertFalse(capturedOut.toString().contains("Warning"));
        }

        @Test
        void warningWhenBalanceDropsBelowThreshold() {
            Account account = new Account("D", 0);
            account.deposit(100);
            account.withdraw(80);
            assertTrue(capturedOut.toString().contains("Warning"));
        }

        @Test
        void warningContainsNewBalance() {
            Account account = new Account("E", 0);
            account.deposit(100);
            account.withdraw(82);
            assertTrue(capturedOut.toString().contains("$18.00"));
        }

        @Test
        void warningContainsThresholdAmount() {
            Account account = new Account("F", 0);
            account.deposit(100);
            account.withdraw(82);
            assertTrue(capturedOut.toString().contains("$25.00"));
        }

        @Test
        void customThresholdTriggersWarning() {
            Account account = new Account("G", 0);
            account.setMinimumBalanceThreshold(50.00);
            account.deposit(100);
            account.withdraw(60);
            assertTrue(capturedOut.toString().contains("Warning"));
            assertTrue(capturedOut.toString().contains("$40.00"));
            assertTrue(capturedOut.toString().contains("$50.00"));
        }

        @Test
        void customThresholdNoWarningWhenAbove() {
            Account account = new Account("H", 0);
            account.setMinimumBalanceThreshold(10.00);
            account.deposit(100);
            account.withdraw(50);
            assertFalse(capturedOut.toString().contains("Warning"));
        }

        @Test
        void zeroThresholdNeverWarns() {
            Account account = new Account("I", 0);
            account.setMinimumBalanceThreshold(0);
            account.deposit(100);
            account.withdraw(99);
            assertFalse(capturedOut.toString().contains("Warning"));
        }

        @Test
        void negativeThresholdRejected() {
            Account account = new Account("J", 0);
            assertThrows(IllegalArgumentException.class, () -> account.setMinimumBalanceThreshold(-10));
        }

        @Test
        void setAndGetThreshold() {
            Account account = new Account("K", 0);
            account.setMinimumBalanceThreshold(100.00);
            assertEquals(100.00, account.getMinimumBalanceThreshold(), 0.01);
        }
    }
}
