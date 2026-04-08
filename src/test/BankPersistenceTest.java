package test;

import bank.Account;
import bank.Bank;
import bank.BankPersistence;
import bank.Transaction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankPersistenceTest {

    @Test
    void saveAndLoadRoundTripPreservesBalancesAndHistory(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("bank.txt");

        Bank bank = new Bank();
        bank.addAccount(new Account("A001", 0));
        bank.getAccount("A001").deposit(25);
        bank.getAccount("A001").withdraw(5);
        bank.tryCreateAccount("B002", 10);

        BankPersistence.save(bank, "B002", file);

        BankPersistence.BankSnapshot loaded = BankPersistence.load(file);

        assertEquals("B002", loaded.getActiveAccountId());

        Bank b = loaded.getBank();

        assertEquals(2, b.getAccountCount());
        assertEquals(20.0, b.getAccount("A001").getBalance(), 0.001);
        assertEquals(10.0, b.getAccount("B002").getBalance(), 0.001);

        assertEquals(3, b.getAccount("A001").getTransactionHistory().size());
        assertTrue(
                b.getAccount("A001").getTransactionHistory().stream()
                        .anyMatch(t -> t.getType() == Transaction.Type.DEPOSIT));
    }

    @Test
    void tryLoadMissingFileReturnsEmpty(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("nope.txt");
        assertTrue(BankPersistence.tryLoad(missing).isEmpty());
    }
}
