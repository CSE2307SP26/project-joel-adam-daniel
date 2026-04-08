package test;

import bank.Account;
import bank.Bank;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BalanceTest {

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
}
