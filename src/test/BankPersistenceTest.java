package test;

import bank.Account;
import bank.AccountType;
import bank.Bank;
import bank.BankPersistence;
import bank.RecurringTransfer;
import bank.Transaction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

        assertEquals("BANK_PERSIST_V5", Files.readString(file).trim().split("\\R", 2)[0]);

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

    @Test
    void recurringTransfersSurviveRoundTrip(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("bank.txt");

        Bank bank = new Bank();
        bank.addAccount(new Account("A", 500));
        bank.addAccount(new Account("B", 0));
        bank.addRecurringTransfer("A", "B", 100.0, 14);
        String id = bank.getAllRecurringTransfers().iterator().next().getId();

        BankPersistence.save(bank, "A", file);
        Bank loaded = BankPersistence.load(file).getBank();

        Collection<RecurringTransfer> rts = loaded.getAllRecurringTransfers();
        assertEquals(1, rts.size());
        RecurringTransfer rt = rts.iterator().next();
        assertEquals(id, rt.getId());
        assertEquals("A", rt.getFromAccountId());
        assertEquals("B", rt.getToAccountId());
        assertEquals(100.0, rt.getAmount(), 0.001);
        assertEquals(14, rt.getIntervalDays());
        assertTrue(rt.isActive());
    }

    @Test
    void cancelledRecurringTransferPersistedAsCancelled(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("bank.txt");

        Bank bank = new Bank();
        bank.addAccount(new Account("A", 500));
        bank.addAccount(new Account("B", 0));
        String id = bank.addRecurringTransfer("A", "B", 50.0, 7).getRecurringTransferId();
        bank.cancelRecurringTransfer(id);

        BankPersistence.save(bank, "A", file);
        Bank loaded = BankPersistence.load(file).getBank();

        RecurringTransfer rt = loaded.getAllRecurringTransfers().iterator().next();
        assertFalse(rt.isActive());
        assertEquals(id, rt.getId());
    }

    @Test
    void idsDoNotClashAfterLoadAndCreate(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("bank.txt");

        Bank bank = new Bank();
        bank.addAccount(new Account("A", 500));
        bank.addAccount(new Account("B", 0));
        bank.addRecurringTransfer("A", "B", 10.0, 7);

        BankPersistence.save(bank, "A", file);
        Bank loaded = BankPersistence.load(file).getBank();

        String existingId = loaded.getAllRecurringTransfers().iterator().next().getId();
        String newId = loaded.addRecurringTransfer("A", "B", 20.0, 14).getRecurringTransferId();
        assertFalse(existingId.equals(newId));
    }

    @Test
    void v4FileLoadsWithEmptyRecurringTransfers(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("v4.txt");
        String v4Content = "BANK_PERSIST_V4\n"
                + "ACTIVE|A001\n"
                + "ACC|A001|100.0|CHECKING|0|1234|0|0|25.0\n"
                + "TXN|OPEN|100.0|1000000000000|Checking — Opened with balance 100.0\n"
                + "ENDACC\n";
        Files.writeString(file, v4Content);

        Bank loaded = BankPersistence.load(file).getBank();
        assertTrue(loaded.getAllRecurringTransfers().isEmpty());
        assertEquals(100.0, loaded.getAccount("A001").getBalance(), 0.001);
    }

    @Test
    void v2CheckingFileLoads(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("v2.txt");
        String content = "BANK_PERSIST_V2\n"
                + "ACTIVE|A001\n"
                + "ACC|A001|100.0|CHECKING\n"
                + "TXN|OPEN|100.0|1700000000000|Checking\n"
                + "ENDACC\n";
        Files.writeString(file, content);

        BankPersistence.BankSnapshot snap = BankPersistence.load(file);
        assertEquals("A001", snap.getActiveAccountId());
        assertEquals(AccountType.CHECKING, snap.getBank().getAccount("A001").getAccountType());
        assertEquals(100.0, snap.getBank().getAccount("A001").getBalance(), 0.001);
    }

    @Test
    void v3CheckingFrozenFileLoads(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("v3.txt");
        String content = "BANK_PERSIST_V3\n"
                + "ACTIVE|A001\n"
                + "ACC|A001|100.0|CHECKING|1\n"
                + "TXN|OPEN|100.0|1700000000000|Checking\n"
                + "ENDACC\n";
        Files.writeString(file, content);

        Bank loaded = BankPersistence.load(file).getBank();
        assertTrue(loaded.getAccount("A001").isFrozen());
    }

    @Test
    void tryLoadInvalidHeaderReturnsEmpty(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("bad.txt");
        Files.writeString(file, "NOT_A_BANK_FILE\nACTIVE|X\n");
        Optional<BankPersistence.BankSnapshot> snap = BankPersistence.tryLoad(file);
        assertTrue(snap.isEmpty());
    }

    @Test
    void tryLoadEmptyFileReturnsEmpty(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("empty.txt");
        Files.writeString(file, "");
        assertTrue(BankPersistence.tryLoad(file).isEmpty());
    }

    @Test
    void loadThrowsOnMalformedFile(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("malformed.txt");
        Files.writeString(file, "BANK_PERSIST_V5\nACTIVE|A001\n");
        assertThrows(Exception.class, () -> BankPersistence.load(file));
    }

    @Test
    void loadRejectsUnknownHeader(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("hdr.txt");
        Files.writeString(file, "BANK_PERSIST_V99\nACTIVE|A001\n");
        assertThrows(IllegalArgumentException.class, () -> BankPersistence.load(file));
    }
}
