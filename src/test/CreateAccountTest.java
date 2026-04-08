package test;

import bank.Account;
import bank.Bank;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
}
