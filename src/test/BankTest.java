package test;

import bank.Account;
import bank.AccountType;
import bank.AddRecurringTransferResult;
import bank.Bank;
import bank.CloseAccountResult;
import bank.CreateAccountResult;
import bank.OperatorResult;
import bank.ProcessedRecurringTransferResult;
import bank.RecurringTransfer;
import bank.SpendingSummaryResult;
import bank.TransferResult;
import bank.Transaction;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for {@link bank.Bank} orchestration, grouped by feature area. */
class BankTest {

    private static final long T0 = 1_700_000_000_000L;
    private static final long ONE_DAY = 24 * 60 * 60 * 1000L;

    @Nested
    class CloseAccountTests {

        @Test
        void zeroBalanceRemovedWhenNotLastAccount() {
            Bank bank = new Bank();
            bank.addAccount(new Account("C1", 0));
            bank.addAccount(new Account("C2", 0));
            CloseAccountResult r = bank.closeCustomerAccount("C1");
            assertTrue(r.isSuccess());
            assertNull(bank.getAccount("C1"));
            assertTrue(bank.getAccountCount() >= 1);
        }

        @Test
        void nonzeroBalanceNotClosed() {
            Bank bank = new Bank();
            bank.addAccount(new Account("D1", 5));
            bank.addAccount(new Account("D2", 0));
            CloseAccountResult r = bank.closeCustomerAccount("D1");
            assertFalse(r.isSuccess());
            assertNotNull(bank.getAccount("D1"));
        }

        @Test
        void lastAccountCannotCloseViaCustomerFlow() {
            Bank bank = new Bank();
            bank.addAccount(new Account("E1", 0));
            CloseAccountResult r = bank.closeCustomerAccount("E1");
            assertFalse(r.isSuccess());
        }

        /** {@link Bank#closeAccount} does not enforce "must keep one account" (unlike {@link Bank#closeCustomerAccount}). */
        @Test
        void closeAccountCanRemoveLastAccountWhenBalanceZero() {
            Bank bank = new Bank();
            bank.addAccount(new Account("ONLY", 0));
            CloseAccountResult r = bank.closeAccount("ONLY");
            assertTrue(r.isSuccess());
            assertEquals(0, bank.getAccountCount());
        }

        @Test
        void closeAccountBlankIdFails() {
            Bank bank = new Bank();
            bank.addAccount(new Account("X", 0));
            assertFalse(bank.closeAccount("").isSuccess());
            assertFalse(bank.closeAccount("  ").isSuccess());
        }
    }

    @Nested
    class CreateAccountTests {

        @Test
        void createAddsSecondAccount() {
            Bank bank = new Bank();
            bank.addAccount(new Account("A001", 0));
            CreateAccountResult r = bank.tryCreateAccount("A002", 0);
            assertTrue(r.isSuccess());
            Account second = bank.getAccount("A002");
            assertNotNull(second);
            assertEquals(0, second.getBalance(), 0.001);
            assertEquals(2, bank.getAccountCount());
        }

        @Test
        void duplicateIdRejected() {
            Bank bank = new Bank();
            bank.tryCreateAccount("Z1", 0);
            CreateAccountResult r = bank.tryCreateAccount("Z1", 0);
            assertFalse(r.isSuccess());
            assertEquals(1, bank.getAccountCount());
        }

        @Test
        void blankIdRejected() {
            Bank bank = new Bank();
            assertFalse(bank.tryCreateAccount("  ", 0).isSuccess());
            assertFalse(bank.tryCreateAccount("", 0).isSuccess());
        }

        @Test
        void nullAccountIdRejected() {
            Bank bank = new Bank();
            assertFalse(bank.tryCreateAccount(null, 0).isSuccess());
        }

        @Test
        void negativeInitialBalanceRejected() {
            Bank bank = new Bank();
            assertFalse(bank.tryCreateAccount("N1", -0.01, AccountType.CHECKING).isSuccess());
        }

        @Test
        void nanInitialBalanceRejected() {
            Bank bank = new Bank();
            assertFalse(bank.tryCreateAccount("Na1", Double.NaN, AccountType.CHECKING).isSuccess());
        }

        @Test
        void invalidPinRejected() {
            Bank bank = new Bank();
            assertFalse(bank.tryCreateAccount("P1", 0, AccountType.CHECKING, 999).isSuccess());
            assertFalse(bank.tryCreateAccount("P2", 0, AccountType.CHECKING, 10000).isSuccess());
        }

        @Test
        void savingsBelowMinimumOpeningRejected() {
            Bank bank = new Bank();
            CreateAccountResult r = bank.tryCreateAccount("S1", 49.99, AccountType.SAVINGS);
            assertFalse(r.isSuccess());
            assertEquals(0, bank.getAccountCount());
        }

        @Test
        void savingsAtMinimumOpeningAccepted() {
            Bank bank = new Bank();
            CreateAccountResult r =
                    bank.tryCreateAccount("S2", Account.SAVINGS_MIN_OPENING_BALANCE, AccountType.SAVINGS);
            assertTrue(r.isSuccess());
            assertEquals(AccountType.SAVINGS, bank.getAccount("S2").getAccountType());
        }

        @Test
        void checkingCanOpenWithZero() {
            Bank bank = new Bank();
            assertTrue(bank.tryCreateAccount("C0", 0, AccountType.CHECKING).isSuccess());
        }

        @Test
        void directSavingsConstructorRejectsLowOpening() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new Account("X", 10, AccountType.SAVINGS));
        }
    }

    @Nested
    class TransferTests {

        @Test
        void transferMovesMoneyAndRecordsTypes() {
            Bank bank = new Bank();
            bank.addAccount(new Account("T1", 100.00));
            bank.addAccount(new Account("T2", 0));
            TransferResult r = bank.transfer("T1", "T2", 30.00);
            assertTrue(r.isSuccess());
            assertEquals(70.00, bank.getAccount("T1").getBalance(), 0.01);
            assertEquals(30.00, bank.getAccount("T2").getBalance(), 0.01);
            assertTrue(
                    bank.getAccount("T1").getTransactionHistory().stream()
                            .anyMatch(t -> t.getType() == Transaction.Type.TRANSFER_OUT));
            assertTrue(
                    bank.getAccount("T2").getTransactionHistory().stream()
                            .anyMatch(t -> t.getType() == Transaction.Type.TRANSFER_IN));
        }

        @Test
        void sameAccountRejected() {
            Bank bank = new Bank();
            bank.addAccount(new Account("S1", 10));
            assertFalse(bank.transfer("S1", "S1", 5).isSuccess());
        }

        @Test
        void missingSourceRejected() {
            Bank bank = new Bank();
            bank.addAccount(new Account("ONLY", 10));
            assertFalse(bank.transfer("NONE", "ONLY", 1).isSuccess());
        }

        @Test
        void missingDestinationRejected() {
            Bank bank = new Bank();
            bank.addAccount(new Account("ONLY", 10));
            assertFalse(bank.transfer("ONLY", "NONE", 1).isSuccess());
        }

        @Test
        void insufficientFundsRejected() {
            Bank bank = new Bank();
            bank.addAccount(new Account("P1", 5));
            bank.addAccount(new Account("P2", 0));
            assertFalse(bank.transfer("P1", "P2", 6).isSuccess());
        }

        @Test
        void blankFromAccountRejected() {
            Bank bank = new Bank();
            bank.addAccount(new Account("A", 10));
            assertFalse(bank.transfer("", "A", 1).isSuccess());
            assertFalse(bank.transfer("  ", "A", 1).isSuccess());
        }

        @Test
        void blankToAccountRejected() {
            Bank bank = new Bank();
            bank.addAccount(new Account("A", 10));
            assertFalse(bank.transfer("A", "", 1).isSuccess());
            assertFalse(bank.transfer("A", "  ", 1).isSuccess());
        }

        @Test
        void zeroAmountRejected() {
            Bank bank = new Bank();
            bank.addAccount(new Account("A", 10));
            bank.addAccount(new Account("B", 0));
            assertFalse(bank.transfer("A", "B", 0).isSuccess());
        }

        @Test
        void nanAmountRejected() {
            Bank bank = new Bank();
            bank.addAccount(new Account("A", 10));
            bank.addAccount(new Account("B", 0));
            assertFalse(bank.transfer("A", "B", Double.NaN).isSuccess());
        }
    }

    @Nested
    class OperatorFreezeTests {

        @Test
        void operatorFreezeUnfreeze() {
            Bank bank = new Bank();
            bank.addAccount(new Account("X", 0));
            assertTrue(bank.setAccountFrozen("X", true).isSuccess());
            assertTrue(bank.getAccount("X").isFrozen());
            assertTrue(bank.setAccountFrozen("X", false).isSuccess());
            assertFalse(bank.getAccount("X").isFrozen());
        }

        @Test
        void blankAccountIdRejectedForFreeze() {
            Bank bank = new Bank();
            bank.addAccount(new Account("X", 0));
            assertFalse(bank.setAccountFrozen("", true).isSuccess());
            assertFalse(bank.setAccountFrozen("  ", false).isSuccess());
        }

        @Test
        void unknownAccountRejectedForFreeze() {
            Bank bank = new Bank();
            bank.addAccount(new Account("X", 0));
            assertFalse(bank.setAccountFrozen("NOPE", true).isSuccess());
        }

        @Test
        void transferBlockedWhenEitherSideFrozen() {
            Bank bank = new Bank();
            bank.addAccount(new Account("A", 50));
            bank.addAccount(new Account("B", 0));
            bank.setAccountFrozen("A", true);
            assertFalse(bank.transfer("A", "B", 10).isSuccess());
            bank.setAccountFrozen("A", false);
            bank.setAccountFrozen("B", true);
            assertFalse(bank.transfer("A", "B", 10).isSuccess());
        }
    }

    @Nested
    class RecurringTransferTests {

        @Test
        void createRecurringTransferSucceeds() {
            Bank bank = new Bank();
            bank.addAccount(new Account("A", 100));
            bank.addAccount(new Account("B", 0));

            AddRecurringTransferResult result = bank.addRecurringTransfer("A", "B", 25.0, 30);

            assertTrue(result.isSuccess());
            assertNotNull(result.getRecurringTransferId());
            assertTrue(result.getRecurringTransferId().startsWith("RT-"));
        }

        @Test
        void createRecurringTransferRejectsSameAccount() {
            Bank bank = new Bank();
            bank.addAccount(new Account("A", 100));

            assertFalse(bank.addRecurringTransfer("A", "A", 10.0, 7).isSuccess());
        }

        @Test
        void createRecurringTransferRejectsMissingAccount() {
            Bank bank = new Bank();
            bank.addAccount(new Account("A", 100));

            assertFalse(bank.addRecurringTransfer("A", "NOPE", 10.0, 7).isSuccess());
            assertFalse(bank.addRecurringTransfer("NOPE", "A", 10.0, 7).isSuccess());
        }

        @Test
        void createRecurringTransferRejectsBlankFrom() {
            Bank bank = new Bank();
            bank.addAccount(new Account("A", 100));
            bank.addAccount(new Account("B", 0));
            assertFalse(bank.addRecurringTransfer("", "B", 10.0, 7).isSuccess());
            assertFalse(bank.addRecurringTransfer("  ", "B", 10.0, 7).isSuccess());
        }

        @Test
        void createRecurringTransferRejectsBlankTo() {
            Bank bank = new Bank();
            bank.addAccount(new Account("A", 100));
            bank.addAccount(new Account("B", 0));
            assertFalse(bank.addRecurringTransfer("A", "", 10.0, 7).isSuccess());
            assertFalse(bank.addRecurringTransfer("A", "  ", 10.0, 7).isSuccess());
        }

        @Test
        void createRecurringTransferRejectsNonPositiveAmount() {
            Bank bank = new Bank();
            bank.addAccount(new Account("A", 100));
            bank.addAccount(new Account("B", 0));

            assertFalse(bank.addRecurringTransfer("A", "B", 0, 7).isSuccess());
            assertFalse(bank.addRecurringTransfer("A", "B", -5, 7).isSuccess());
        }

        @Test
        void createRecurringTransferRejectsNonPositiveInterval() {
            Bank bank = new Bank();
            bank.addAccount(new Account("A", 100));
            bank.addAccount(new Account("B", 0));

            assertFalse(bank.addRecurringTransfer("A", "B", 10.0, 0).isSuccess());
            assertFalse(bank.addRecurringTransfer("A", "B", 10.0, -1).isSuccess());
        }

        @Test
        void newlyCreatedTransferAppearsInList() {
            Bank bank = new Bank();
            bank.addAccount(new Account("A", 100));
            bank.addAccount(new Account("B", 0));

            bank.addRecurringTransfer("A", "B", 50.0, 7);

            Collection<RecurringTransfer> all = bank.getAllRecurringTransfers();
            assertEquals(1, all.size());
            RecurringTransfer rt = all.iterator().next();
            assertEquals("A", rt.getFromAccountId());
            assertEquals("B", rt.getToAccountId());
            assertEquals(50.0, rt.getAmount(), 0.001);
            assertEquals(7, rt.getIntervalDays());
            assertTrue(rt.isActive());
        }

        @Test
        void idsAreUnique() {
            Bank bank = new Bank();
            bank.addAccount(new Account("A", 200));
            bank.addAccount(new Account("B", 0));

            String id1 = bank.addRecurringTransfer("A", "B", 10.0, 7).getRecurringTransferId();
            String id2 = bank.addRecurringTransfer("A", "B", 20.0, 14).getRecurringTransferId();

            assertFalse(id1.equals(id2));
        }

        @Test
        void cancelRecurringTransferSucceeds() {
            Bank bank = new Bank();
            bank.addAccount(new Account("A", 100));
            bank.addAccount(new Account("B", 0));

            String id = bank.addRecurringTransfer("A", "B", 10.0, 7).getRecurringTransferId();
            OperatorResult cancel = bank.cancelRecurringTransfer(id);

            assertTrue(cancel.isSuccess());
            RecurringTransfer rt = bank.getAllRecurringTransfers().iterator().next();
            assertFalse(rt.isActive());
        }

        @Test
        void cancelRecurringTransferBlankIdFails() {
            Bank bank = new Bank();
            assertFalse(bank.cancelRecurringTransfer("").isSuccess());
            assertFalse(bank.cancelRecurringTransfer("  ").isSuccess());
        }

        @Test
        void cancelNonExistentIdFails() {
            Bank bank = new Bank();
            assertFalse(bank.cancelRecurringTransfer("RT-99").isSuccess());
        }

        @Test
        void cancelAlreadyCancelledFails() {
            Bank bank = new Bank();
            bank.addAccount(new Account("A", 100));
            bank.addAccount(new Account("B", 0));

            String id = bank.addRecurringTransfer("A", "B", 10.0, 7).getRecurringTransferId();
            bank.cancelRecurringTransfer(id);

            assertFalse(bank.cancelRecurringTransfer(id).isSuccess());
        }

        @Test
        void dueTransferExecutesAndAdvancesSchedule() {
            Bank bank = new Bank();
            bank.addAccount(new Account("A", 100));
            bank.addAccount(new Account("B", 0));

            bank.addRecurringTransferForPersistence(
                    new RecurringTransfer("RT-1", "A", "B", 30.0, 7,
                            System.currentTimeMillis() - 1000, true));

            List<ProcessedRecurringTransferResult> results = bank.processDueRecurringTransfers();

            assertEquals(1, results.size());
            assertTrue(results.get(0).isSuccess());
            assertEquals(70.0, bank.getAccount("A").getBalance(), 0.001);
            assertEquals(30.0, bank.getAccount("B").getBalance(), 0.001);

            RecurringTransfer rt = bank.getAllRecurringTransfers().iterator().next();
            assertTrue(rt.getNextRunMs() > System.currentTimeMillis());
        }

        @Test
        void notYetDueTransferIsSkipped() {
            Bank bank = new Bank();
            bank.addAccount(new Account("A", 100));
            bank.addAccount(new Account("B", 0));

            bank.addRecurringTransferForPersistence(
                    new RecurringTransfer("RT-1", "A", "B", 30.0, 30,
                            System.currentTimeMillis() + 999_999_000L, true));

            List<ProcessedRecurringTransferResult> results = bank.processDueRecurringTransfers();

            assertTrue(results.isEmpty());
            assertEquals(100.0, bank.getAccount("A").getBalance(), 0.001);
        }

        @Test
        void cancelledTransferIsNotProcessed() {
            Bank bank = new Bank();
            bank.addAccount(new Account("A", 100));
            bank.addAccount(new Account("B", 0));

            bank.addRecurringTransferForPersistence(
                    new RecurringTransfer("RT-1", "A", "B", 30.0, 7,
                            System.currentTimeMillis() - 1000, false));

            List<ProcessedRecurringTransferResult> results = bank.processDueRecurringTransfers();

            assertTrue(results.isEmpty());
            assertEquals(100.0, bank.getAccount("A").getBalance(), 0.001);
        }

        @Test
        void insufficientFundsTransferReportsFailureButAdvancesSchedule() {
            Bank bank = new Bank();
            bank.addAccount(new Account("A", 5));
            bank.addAccount(new Account("B", 0));

            bank.addRecurringTransferForPersistence(
                    new RecurringTransfer("RT-1", "A", "B", 50.0, 7,
                            System.currentTimeMillis() - 1000, true));

            List<ProcessedRecurringTransferResult> results = bank.processDueRecurringTransfers();

            assertEquals(1, results.size());
            assertFalse(results.get(0).isSuccess());
            assertEquals(5.0, bank.getAccount("A").getBalance(), 0.001);
            RecurringTransfer rt = bank.getAllRecurringTransfers().iterator().next();
            assertTrue(rt.getNextRunMs() > System.currentTimeMillis());
        }

        @Test
        void frozenSourceAccountTransferReportsFailure() {
            Bank bank = new Bank();
            bank.addAccount(new Account("A", 100));
            bank.addAccount(new Account("B", 0));
            bank.setAccountFrozen("A", true);

            bank.addRecurringTransferForPersistence(
                    new RecurringTransfer("RT-1", "A", "B", 20.0, 7,
                            System.currentTimeMillis() - 1000, true));

            List<ProcessedRecurringTransferResult> results = bank.processDueRecurringTransfers();

            assertEquals(1, results.size());
            assertFalse(results.get(0).isSuccess());
        }
    }

    @Nested
    class SpendingSummaryApiTests {

        @Test
        void spendingSummaryUnknownAccountReturnsFailure() {
            Bank bank = new Bank();
            SpendingSummaryResult r = bank.getSpendingSummary("NOPE", T0, T0 + ONE_DAY);
            assertFalse(r.isSuccess());
        }

        @Test
        void spendingSummaryBlankAccountIdReturnsFailure() {
            Bank bank = new Bank();
            assertFalse(bank.getSpendingSummary("", T0, T0 + ONE_DAY).isSuccess());
        }

        @Test
        void spendingSummaryStartAfterEndReturnsFailure() {
            Bank bank = new Bank();
            bank.addAccount(new Account("A", 100));
            assertFalse(bank.getSpendingSummary("A", T0 + ONE_DAY, T0).isSuccess());
        }

        @Test
        void spendingSummaryValidRequestReturnsSuccess() {
            Bank bank = new Bank();
            bank.addAccount(new Account("A", 100));
            assertTrue(bank.getSpendingSummary("A", T0, T0 + ONE_DAY).isSuccess());
            assertNotNull(bank.getSpendingSummary("A", T0, T0 + ONE_DAY).getSummary());
        }

        @Test
        void spendingSummaryBankApiDelegatesToAccount() {
            Bank bank = new Bank();
            bank.addAccount(new Account("X", 300));
            bank.getAccount("X").deposit(80);

            SpendingSummaryResult r = bank.getSpendingSummary("X", 0, Long.MAX_VALUE);

            assertTrue(r.isSuccess());
            assertEquals(80.0, r.getSummary().getTotalDeposited(), 0.001);
        }

        @Test
        void spendingSummaryIncludesInterestAndFees() {
            Bank bank = new Bank();
            bank.addAccount(new Account("Y", 200));
            bank.getAccount("Y").applyAdministratorInterest(5, "promo");
            bank.getAccount("Y").applyAdministratorFee(2, "monthly");

            SpendingSummaryResult r = bank.getSpendingSummary("Y", 0, Long.MAX_VALUE);

            assertTrue(r.isSuccess());
            assertEquals(5.0, r.getSummary().getTotalDeposited(), 0.001);
            assertEquals(2.0, r.getSummary().getTotalWithdrawn(), 0.001);
        }
    }

    @Nested
    class AdministratorFeeAndInterestTests {

        @Test
        void collectFeeReducesBalance() {
            Bank bank = new Bank();
            bank.addAccount(new Account("F1", 100));
            assertTrue(bank.collectFee("F1", 15).isSuccess());
            assertEquals(85.0, bank.getAccount("F1").getBalance(), 0.001);
        }

        @Test
        void collectFeeInsufficientFunds() {
            Bank bank = new Bank();
            bank.addAccount(new Account("F2", 10));
            assertFalse(bank.collectFee("F2", 11).isSuccess());
        }

        @Test
        void collectFeeUnknownAccount() {
            Bank bank = new Bank();
            bank.addAccount(new Account("F3", 10));
            assertFalse(bank.collectFee("NONE", 1).isSuccess());
        }

        @Test
        void collectFeeBlankId() {
            Bank bank = new Bank();
            assertFalse(bank.collectFee("", 1).isSuccess());
        }

        @Test
        void collectFeeNonPositiveAmount() {
            Bank bank = new Bank();
            bank.addAccount(new Account("F4", 50));
            assertFalse(bank.collectFee("F4", 0).isSuccess());
            assertFalse(bank.collectFee("F4", -1).isSuccess());
        }

        @Test
        void collectFeeFailsWhenAccountFrozen() {
            Bank bank = new Bank();
            bank.addAccount(new Account("F5", 50));
            bank.setAccountFrozen("F5", true);
            assertFalse(bank.collectFee("F5", 5).isSuccess());
        }

        @Test
        void collectFeeDoesNotConsumeSavingsWithdrawalQuota() {
            Bank bank = new Bank();
            Account s = new Account("SAV", 500, AccountType.SAVINGS);
            s.setMinimumBalanceThreshold(0);
            bank.addAccount(s);
            for (int i = 0; i < Account.SAVINGS_MAX_WITHDRAWALS_PER_MONTH; i++) {
                s.withdraw(1);
            }
            assertTrue(bank.collectFee("SAV", 10).isSuccess());
            assertEquals(
                    500 - Account.SAVINGS_MAX_WITHDRAWALS_PER_MONTH - 10,
                    bank.getAccount("SAV").getBalance(),
                    0.01);
        }

        @Test
        void applyInterestIncreasesBalance() {
            Bank bank = new Bank();
            bank.addAccount(new Account("I1", 20));
            assertTrue(bank.applyInterest("I1", 3.5).isSuccess());
            assertEquals(23.5, bank.getAccount("I1").getBalance(), 0.001);
        }

        @Test
        void applyInterestUnknownAccount() {
            Bank bank = new Bank();
            assertFalse(bank.applyInterest("MISS", 1).isSuccess());
        }

        @Test
        void applyInterestNonPositiveAmount() {
            Bank bank = new Bank();
            bank.addAccount(new Account("I2", 10));
            assertFalse(bank.applyInterest("I2", 0).isSuccess());
        }

        @Test
        void applyInterestFailsWhenFrozen() {
            Bank bank = new Bank();
            bank.addAccount(new Account("I3", 100));
            bank.setAccountFrozen("I3", true);
            assertFalse(bank.applyInterest("I3", 5).isSuccess());
        }
    }
}
