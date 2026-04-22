package test;

import bank.Transaction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for {@link Transaction} and {@link Transaction.Type}. */
class TransactionTest {

    @Test
    void transactionTypesHaveAuditLabels() {
        assertEquals("Deposits", Transaction.Type.DEPOSIT.getAuditLabel());
        assertEquals("Withdrawals", Transaction.Type.WITHDRAW.getAuditLabel());
        assertEquals("Transfers in", Transaction.Type.TRANSFER_IN.getAuditLabel());
        assertEquals("Transfers out", Transaction.Type.TRANSFER_OUT.getAuditLabel());
        assertEquals("Account opened", Transaction.Type.OPEN.getAuditLabel());
    }

    @Test
    void gettersReflectConstructor() {
        Transaction t = new Transaction(Transaction.Type.DEPOSIT, 42.5, 1_700_000_000_000L, "memo");
        assertEquals(Transaction.Type.DEPOSIT, t.getType());
        assertEquals(42.5, t.getAmount(), 0.001);
        assertEquals(1_700_000_000_000L, t.getWhenMs());
        assertEquals("memo", t.getDetail());
    }

    @Test
    void nullDetailBecomesEmpty() {
        Transaction t = new Transaction(Transaction.Type.WITHDRAW, 1, 0L, null);
        assertEquals("", t.getDetail());
    }

    @Test
    void toStringIncludesTypeAmountAndDetailTail() {
        Transaction t = new Transaction(Transaction.Type.DEPOSIT, 10, 0L, "x");
        String s = t.toString();
        assertTrue(s.contains("DEPOSIT"));
        assertTrue(s.contains("10.0"));
        assertTrue(s.contains(" - x"));
    }
}
