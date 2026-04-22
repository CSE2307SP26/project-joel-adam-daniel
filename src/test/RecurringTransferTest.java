package test;

import bank.Account;
import bank.AccountType;
import bank.Bank;
import bank.BankPersistence;
import bank.RecurringTransfer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecurringTransferTest {

    // --- Creation validation ---

    @Test
    void createRecurringTransferSucceeds() {
        Bank bank = new Bank();
        bank.addAccount(new Account("A", 100));
        bank.addAccount(new Account("B", 0));

        Bank.AddRecurringTransferResult result = bank.addRecurringTransfer("A", "B", 25.0, 30);

        assertTrue(result.isSuccess());
        assertNotNull(result.getRecurringTransferId());
        assertTrue(result.getRecurringTransferId().startsWith("RT-"));
    }

    @Test
    void createRecurringTransferRejectsSameAccount() {
        Bank bank = new Bank();
        bank.addAccount(new Account("A", 100));

        assertFalse(bank.addRecurringTransfer("A", "A", 10.0, 7).isSuccess());
    }

    @Test
    void createRecurringTransferRejectsMissingAccount() {
        Bank bank = new Bank();
        bank.addAccount(new Account("A", 100));

        assertFalse(bank.addRecurringTransfer("A", "NOPE", 10.0, 7).isSuccess());
        assertFalse(bank.addRecurringTransfer("NOPE", "A", 10.0, 7).isSuccess());
    }

    @Test
    void createRecurringTransferRejectsNonPositiveAmount() {
        Bank bank = new Bank();
        bank.addAccount(new Account("A", 100));
        bank.addAccount(new Account("B", 0));

        assertFalse(bank.addRecurringTransfer("A", "B", 0, 7).isSuccess());
        assertFalse(bank.addRecurringTransfer("A", "B", -5, 7).isSuccess());
    }

    @Test
    void createRecurringTransferRejectsNonPositiveInterval() {
        Bank bank = new Bank();
        bank.addAccount(new Account("A", 100));
        bank.addAccount(new Account("B", 0));

        assertFalse(bank.addRecurringTransfer("A", "B", 10.0, 0).isSuccess());
        assertFalse(bank.addRecurringTransfer("A", "B", 10.0, -1).isSuccess());
    }

    // --- Listing ---

    @Test
    void newlyCreatedTransferAppearsInList() {
        Bank bank = new Bank();
        bank.addAccount(new Account("A", 100));
        bank.addAccount(new Account("B", 0));

        bank.addRecurringTransfer("A", "B", 50.0, 7);

        Collection<RecurringTransfer> all = bank.getAllRecurringTransfers();
        assertEquals(1, all.size());
        RecurringTransfer rt = all.iterator().next();
        assertEquals("A", rt.getFromAccountId());
        assertEquals("B", rt.getToAccountId());
        assertEquals(50.0, rt.getAmount(), 0.001);
        assertEquals(7, rt.getIntervalDays());
        assertTrue(rt.isActive());
    }

    @Test
    void idsAreUnique() {
        Bank bank = new Bank();
        bank.addAccount(new Account("A", 200));
        bank.addAccount(new Account("B", 0));

        String id1 = bank.addRecurringTransfer("A", "B", 10.0, 7).getRecurringTransferId();
        String id2 = bank.addRecurringTransfer("A", "B", 20.0, 14).getRecurringTransferId();

        assertFalse(id1.equals(id2));
    }

    // --- Cancellation ---

    @Test
    void cancelRecurringTransferSucceeds() {
        Bank bank = new Bank();
        bank.addAccount(new Account("A", 100));
        bank.addAccount(new Account("B", 0));

        String id = bank.addRecurringTransfer("A", "B", 10.0, 7).getRecurringTransferId();
        Bank.OperatorResult cancel = bank.cancelRecurringTransfer(id);

        assertTrue(cancel.isSuccess());
        RecurringTransfer rt = bank.getAllRecurringTransfers().iterator().next();
        assertFalse(rt.isActive());
    }

    @Test
    void cancelNonExistentIdFails() {
        Bank bank = new Bank();
        assertFalse(bank.cancelRecurringTransfer("RT-99").isSuccess());
    }

    @Test
    void cancelAlreadyCancelledFails() {
        Bank bank = new Bank();
        bank.addAccount(new Account("A", 100));
        bank.addAccount(new Account("B", 0));

        String id = bank.addRecurringTransfer("A", "B", 10.0, 7).getRecurringTransferId();
        bank.cancelRecurringTransfer(id);

        assertFalse(bank.cancelRecurringTransfer(id).isSuccess());
    }

    // --- Processing due transfers ---

    @Test
    void dueTransferExecutesAndAdvancesSchedule() {
        Bank bank = new Bank();
        bank.addAccount(new Account("A", 100));
        bank.addAccount(new Account("B", 0));

        // Manually add an RT whose nextRunMs is in the past
        bank.addRecurringTransferForPersistence(
                new RecurringTransfer("RT-1", "A", "B", 30.0, 7,
                        System.currentTimeMillis() - 1000, true));

        List<Bank.ProcessedRecurringTransferResult> results = bank.processDueRecurringTransfers();

        assertEquals(1, results.size());
        assertTrue(results.get(0).isSuccess());
        assertEquals(70.0, bank.getAccount("A").getBalance(), 0.001);
        assertEquals(30.0, bank.getAccount("B").getBalance(), 0.001);

        // Schedule should have advanced by one interval
        RecurringTransfer rt = bank.getAllRecurringTransfers().iterator().next();
        assertTrue(rt.getNextRunMs() > System.currentTimeMillis());
    }

    @Test
    void notYetDueTransferIsSkipped() {
        Bank bank = new Bank();
        bank.addAccount(new Account("A", 100));
        bank.addAccount(new Account("B", 0));

        bank.addRecurringTransferForPersistence(
                new RecurringTransfer("RT-1", "A", "B", 30.0, 30,
                        System.currentTimeMillis() + 999_999_000L, true));

        List<Bank.ProcessedRecurringTransferResult> results = bank.processDueRecurringTransfers();

        assertTrue(results.isEmpty());
        assertEquals(100.0, bank.getAccount("A").getBalance(), 0.001);
    }

    @Test
    void cancelledTransferIsNotProcessed() {
        Bank bank = new Bank();
        bank.addAccount(new Account("A", 100));
        bank.addAccount(new Account("B", 0));

        bank.addRecurringTransferForPersistence(
                new RecurringTransfer("RT-1", "A", "B", 30.0, 7,
                        System.currentTimeMillis() - 1000, false));

        List<Bank.ProcessedRecurringTransferResult> results = bank.processDueRecurringTransfers();

        assertTrue(results.isEmpty());
        assertEquals(100.0, bank.getAccount("A").getBalance(), 0.001);
    }

    @Test
    void insufficientFundsTransferReportsFailureButAdvancesSchedule() {
        Bank bank = new Bank();
        bank.addAccount(new Account("A", 5));
        bank.addAccount(new Account("B", 0));

        bank.addRecurringTransferForPersistence(
                new RecurringTransfer("RT-1", "A", "B", 50.0, 7,
                        System.currentTimeMillis() - 1000, true));

        List<Bank.ProcessedRecurringTransferResult> results = bank.processDueRecurringTransfers();

        assertEquals(1, results.size());
        assertFalse(results.get(0).isSuccess());
        // Balance unchanged
        assertEquals(5.0, bank.getAccount("A").getBalance(), 0.001);
        // Schedule still advanced
        RecurringTransfer rt = bank.getAllRecurringTransfers().iterator().next();
        assertTrue(rt.getNextRunMs() > System.currentTimeMillis());
    }

    @Test
    void frozenSourceAccountTransferReportsFailure() {
        Bank bank = new Bank();
        bank.addAccount(new Account("A", 100));
        bank.addAccount(new Account("B", 0));
        bank.setAccountFrozen("A", true);

        bank.addRecurringTransferForPersistence(
                new RecurringTransfer("RT-1", "A", "B", 20.0, 7,
                        System.currentTimeMillis() - 1000, true));

        List<Bank.ProcessedRecurringTransferResult> results = bank.processDueRecurringTransfers();

        assertEquals(1, results.size());
        assertFalse(results.get(0).isSuccess());
    }

    // --- Persistence ---

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

        // Add a second RT after loading — its ID must not clash with the persisted one
        String existingId = loaded.getAllRecurringTransfers().iterator().next().getId();
        String newId = loaded.addRecurringTransfer("A", "B", 20.0, 14).getRecurringTransferId();
        assertFalse(existingId.equals(newId));
    }

    @Test
    void v4FileLoadsWithEmptyRecurringTransfers(@TempDir Path tempDir) throws Exception {
        // Write a hand-crafted V4 file and confirm it loads without error
        Path file = tempDir.resolve("v4.txt");
        String v4Content = "BANK_PERSIST_V4\n"
                + "ACTIVE|A001\n"
                + "ACC|A001|100.0|CHECKING|0|1234|0|0|25.0\n"
                + "TXN|OPEN|100.0|1000000000000|Checking — Opened with balance 100.0\n"
                + "ENDACC\n";
        java.nio.file.Files.writeString(file, v4Content);

        Bank loaded = BankPersistence.load(file).getBank();
        assertTrue(loaded.getAllRecurringTransfers().isEmpty());
    }
}
