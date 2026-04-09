package test;

import bank.Account;
import bank.Bank;
import bank.Transaction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WithdrawTest {

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
}
