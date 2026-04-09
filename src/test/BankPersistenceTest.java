package test;

import bank.Account;
import bank.AccountType;
import bank.Bank;
import bank.BankPersistence;
import bank.Transaction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void saveAndLoadPreservesFrozenFlag(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("frz.txt");
        Bank bank = new Bank();
        bank.addAccount(new Account("Z1", 10));
        bank.setAccountFrozen("Z1", true);
        BankPersistence.save(bank, "Z1", file);
        Bank loaded = BankPersistence.load(file).getBank();
        assertTrue(loaded.getAccount("Z1").isFrozen());
        loaded.setAccountFrozen("Z1", false);
        BankPersistence.save(loaded, "Z1", file);
        assertFalse(BankPersistence.load(file).getBank().getAccount("Z1").isFrozen());
    }

    @Test
    void tryLoadMissingFileReturnsEmpty(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("nope.txt");
        assertTrue(BankPersistence.tryLoad(missing).isEmpty());
    }

    @Test
    void saveAndLoadPreservesSavingsWithdrawalCount(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("bank-sav.txt");

        Bank bank = new Bank();
        bank.addAccount(new Account("CHK", 0, AccountType.CHECKING));
        bank.addAccount(new Account("SAV", 200, AccountType.SAVINGS));
        bank.getAccount("SAV").withdraw(5);

        BankPersistence.save(bank, "SAV", file);

        BankPersistence.BankSnapshot loaded = BankPersistence.load(file);
        Bank b = loaded.getBank();
        Account sav = b.getAccount("SAV");

        assertEquals(AccountType.SAVINGS, sav.getAccountType());
        assertEquals(195, sav.getBalance(), 0.01);

        for (int i = 0; i < Account.SAVINGS_MAX_WITHDRAWALS_PER_MONTH - 1; i++) {
            assertTrue(b.transfer("SAV", "CHK", 1).isSuccess());
        }
        assertFalse(b.transfer("SAV", "CHK", 1).isSuccess());
    }
}
