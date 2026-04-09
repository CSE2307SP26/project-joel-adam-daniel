package test;

import bank.Account;
import bank.AccountType;
import bank.Bank;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateAccountTest {

    @Test
    void createAddsSecondAccount() {
        Bank bank = new Bank();
        bank.addAccount(new Account("A001", 0));
        Bank.CreateAccountResult r = bank.tryCreateAccount("A002", 0);
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
        Bank.CreateAccountResult r = bank.tryCreateAccount("Z1", 0);
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
    void savingsBelowMinimumOpeningRejected() {
        Bank bank = new Bank();
        Bank.CreateAccountResult r = bank.tryCreateAccount("S1", 49.99, AccountType.SAVINGS);
        assertFalse(r.isSuccess());
        assertEquals(0, bank.getAccountCount());
    }

    @Test
    void savingsAtMinimumOpeningAccepted() {
        Bank bank = new Bank();
        Bank.CreateAccountResult r =
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
