package test;

import bank.Account;
import bank.Bank;
import bank.Transaction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepositTest {

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
}
