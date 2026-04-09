package test;

import bank.Account;
import bank.AccountType;
import bank.Bank;
import bank.BankPersistence;
import bank.Transaction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
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
    void saveAndLoadPreservesPinAndMinimumBalanceThreshold(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("pin-min.txt");

        Bank bank = new Bank();
        bank.addAccount(new Account("P", 100, AccountType.CHECKING, 2468));
        bank.getAccount("P").setMinimumBalanceThreshold(30.5);

        BankPersistence.save(bank, "P", file);

        assertEquals("BANK_PERSIST_V4", Files.readString(file).trim().split("\\R", 2)[0]);

        Account loaded = BankPersistence.load(file).getBank().getAccount("P");
        assertTrue(loaded.authenticatePin(2468));
        assertEquals(30.5, loaded.getMinimumBalanceThreshold(), 0.01);
    }

    @Test
    void saveAndLoadPreservesPinFailedAttemptsAndLock(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("pin-lock.txt");

        Bank bank = new Bank();
        bank.addAccount(new Account("L", 10, AccountType.CHECKING, 5555));
        Account a = bank.getAccount("L");
        assertFalse(a.authenticatePin(1111));
        assertFalse(a.authenticatePin(2222));
        assertEquals(1, a.getPinRemainingAttempts());
        assertFalse(a.isPinLocked());

        BankPersistence.save(bank, "L", file);
        Account loaded = BankPersistence.load(file).getBank().getAccount("L");
        assertEquals(1, loaded.getPinRemainingAttempts());
        assertFalse(loaded.isPinLocked());
        assertTrue(loaded.authenticatePin(5555));

        bank = new Bank();
        bank.addAccount(new Account("M", 10, AccountType.CHECKING, 7777));
        Account locked = bank.getAccount("M");
        locked.authenticatePin(0);
        locked.authenticatePin(0);
        locked.authenticatePin(0);
        assertTrue(locked.isPinLocked());

        BankPersistence.save(bank, "M", file);
        Account loadedLocked = BankPersistence.load(file).getBank().getAccount("M");
        assertTrue(loadedLocked.isPinLocked());
        assertFalse(loadedLocked.authenticatePin(7777));
    }

    @Test
    void saveAndLoadPreservesSavingsTypeAndFrozenTogether(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("combo.txt");

        Bank bank = new Bank();
        bank.addAccount(new Account("SAV", 200, AccountType.SAVINGS));
        bank.setAccountFrozen("SAV", true);

        BankPersistence.save(bank, "SAV", file);

        Bank loaded = BankPersistence.load(file).getBank();
        Account sav = loaded.getAccount("SAV");

        assertEquals(AccountType.SAVINGS, sav.getAccountType());
        assertTrue(sav.isFrozen());
        assertEquals(200, sav.getBalance(), 0.01);
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
