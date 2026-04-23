package test;

import bank.Account;
import bank.AccountType;
import bank.Bank;
import bank.CloseAccountResult;
import bank.CreateAccountResult;
import bank.SpendingSummaryResult;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Extra orchestration coverage: null or NaN inputs and unknown account ids. */
class BankApiEdgeCasesTest {

    @Test
    void transferRejectsNullFromAccount() {
        Bank bank = new Bank();
        bank.addAccount(new Account("A", 10));
        bank.addAccount(new Account("B", 0));
        assertFalse(bank.transfer(null, "B", 1).isSuccess());
    }

    @Test
    void transferRejectsNullToAccount() {
        Bank bank = new Bank();
        bank.addAccount(new Account("A", 10));
        bank.addAccount(new Account("B", 0));
        assertFalse(bank.transfer("A", null, 1).isSuccess());
    }

    @Test
    void closeAccountRejectsUnknownId() {
        Bank bank = new Bank();
        bank.addAccount(new Account("X", 0));
        CloseAccountResult r = bank.closeAccount("NOPE");
        assertFalse(r.isSuccess());
    }

    @Test
    void closeAccountRejectsNullId() {
        Bank bank = new Bank();
        bank.addAccount(new Account("X", 0));
        assertFalse(bank.closeAccount(null).isSuccess());
    }

    @Test
    void closeCustomerAccountRejectsUnknownWhenMultipleAccountsExist() {
        Bank bank = new Bank();
        bank.addAccount(new Account("A", 0));
        bank.addAccount(new Account("B", 0));
        assertFalse(bank.closeCustomerAccount("GHOST").isSuccess());
    }

    @Test
    void setAccountFrozenRejectsNullId() {
        Bank bank = new Bank();
        bank.addAccount(new Account("Z", 0));
        assertFalse(bank.setAccountFrozen(null, true).isSuccess());
    }

    @Test
    void collectFeeRejectsNullAccountId() {
        Bank bank = new Bank();
        bank.addAccount(new Account("F", 50));
        assertFalse(bank.collectFee(null, 5).isSuccess());
    }

    @Test
    void collectFeeRejectsNanAmount() {
        Bank bank = new Bank();
        bank.addAccount(new Account("F", 50));
        assertFalse(bank.collectFee("F", Double.NaN).isSuccess());
    }

    @Test
    void applyInterestRejectsBlankAccountId() {
        Bank bank = new Bank();
        bank.addAccount(new Account("I", 10));
        assertFalse(bank.applyInterest("  ", 1).isSuccess());
    }

    @Test
    void applyInterestRejectsNullAccountId() {
        Bank bank = new Bank();
        bank.addAccount(new Account("I", 10));
        assertFalse(bank.applyInterest(null, 1).isSuccess());
    }

    @Test
    void applyInterestRejectsNanAmount() {
        Bank bank = new Bank();
        bank.addAccount(new Account("I", 10));
        assertFalse(bank.applyInterest("I", Double.NaN).isSuccess());
    }

    @Test
    void getSpendingSummaryRejectsNullAccountId() {
        Bank bank = new Bank();
        bank.addAccount(new Account("A", 1));
        SpendingSummaryResult r = bank.getSpendingSummary(null, 0L, 1L);
        assertFalse(r.isSuccess());
    }

    @Test
    void addRecurringTransferRejectsNanAmount() {
        Bank bank = new Bank();
        bank.addAccount(new Account("A", 100));
        bank.addAccount(new Account("B", 0));
        assertFalse(bank.addRecurringTransfer("A", "B", Double.NaN, 7).isSuccess());
    }

    @Test
    void tryCreateAccountAcceptsPipeInIdWhenPersisted() {
        Bank bank = new Bank();
        CreateAccountResult r = bank.tryCreateAccount("A|B", 0, AccountType.CHECKING);
        assertTrue(r.isSuccess());
        assertTrue(bank.getAccount("A|B").getAccountNumber().contains("|"));
    }
}
