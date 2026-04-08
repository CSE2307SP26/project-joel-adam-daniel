package test;

import bank.Account;
import bank.Transaction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionHistoryTest {

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
}
