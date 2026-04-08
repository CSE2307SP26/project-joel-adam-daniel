package test;

import bank.Account;
import bank.Bank;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloseAccountTest {

    @Test
    void zeroBalanceRemovedWhenNotLastAccount() {
        Bank bank = new Bank();
        bank.addAccount(new Account("C1", 0));
        bank.addAccount(new Account("C2", 0));
        Bank.CloseAccountResult r = bank.closeCustomerAccount("C1");
        assertTrue(r.isSuccess());
        assertNull(bank.getAccount("C1"));
        assertTrue(bank.getAccountCount() >= 1);
    }

    @Test
    void nonzeroBalanceNotClosed() {
        Bank bank = new Bank();
        bank.addAccount(new Account("D1", 5));
        bank.addAccount(new Account("D2", 0));
        Bank.CloseAccountResult r = bank.closeCustomerAccount("D1");
        assertFalse(r.isSuccess());
        assertNotNull(bank.getAccount("D1"));
    }

    @Test
    void lastAccountCannotClose() {
        Bank bank = new Bank();
        bank.addAccount(new Account("E1", 0));
        Bank.CloseAccountResult r = bank.closeCustomerAccount("E1");
        assertFalse(r.isSuccess());
    }
}
