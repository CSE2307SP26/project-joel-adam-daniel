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
    void transactionTypesHaveAuditLabels() {
        assertEquals("Deposits", Transaction.Type.DEPOSIT.getAuditLabel());
        assertEquals("Withdrawals", Transaction.Type.WITHDRAW.getAuditLabel());
    }
}
