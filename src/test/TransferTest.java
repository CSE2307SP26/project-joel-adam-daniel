package test;

import bank.Account;
import bank.Bank;
import bank.Transaction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransferTest {

    @Test
    void transferMovesMoneyAndRecordsTypes() {
        Bank bank = new Bank();
        bank.addAccount(new Account("T1", 100.00));
        bank.addAccount(new Account("T2", 0));
        Bank.TransferResult r = bank.transfer("T1", "T2", 30.00);
        assertTrue(r.isSuccess());
        assertEquals(70.00, bank.getAccount("T1").getBalance(), 0.01);
        assertEquals(30.00, bank.getAccount("T2").getBalance(), 0.01);
        assertTrue(
                bank.getAccount("T1").getTransactionHistory().stream()
                        .anyMatch(t -> t.getType() == Transaction.Type.TRANSFER_OUT));
        assertTrue(
                bank.getAccount("T2").getTransactionHistory().stream()
                        .anyMatch(t -> t.getType() == Transaction.Type.TRANSFER_IN));
    }

    @Test
    void sameAccountRejected() {
        Bank bank = new Bank();
        bank.addAccount(new Account("S1", 10));
        assertFalse(bank.transfer("S1", "S1", 5).isSuccess());
    }

    @Test
    void missingSourceRejected() {
        Bank bank = new Bank();
        bank.addAccount(new Account("ONLY", 10));
        assertFalse(bank.transfer("NONE", "ONLY", 1).isSuccess());
    }

    @Test
    void insufficientFundsRejected() {
        Bank bank = new Bank();
        bank.addAccount(new Account("P1", 5));
        bank.addAccount(new Account("P2", 0));
        assertFalse(bank.transfer("P1", "P2", 6).isSuccess());
    }
}
