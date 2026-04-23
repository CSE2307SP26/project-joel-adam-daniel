package test;

import bank.RecurringTransfer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for {@link RecurringTransfer} (immutable identity and schedule fields). */
class RecurringTransferTest {

    @Test
    void accessorsMatchConstruction() {
        RecurringTransfer rt = new RecurringTransfer("RT-7", "A", "B", 12.34, 14, 99_000L, true);
        assertEquals("RT-7", rt.getId());
        assertEquals("A", rt.getFromAccountId());
        assertEquals("B", rt.getToAccountId());
        assertEquals(12.34, rt.getAmount(), 0.001);
        assertEquals(14, rt.getIntervalDays());
        assertEquals(99_000L, rt.getNextRunMs());
        assertTrue(rt.isActive());
    }

    @Test
    void inactiveConstruction() {
        RecurringTransfer rt = new RecurringTransfer("RT-1", "X", "Y", 1, 1, 0L, false);
        assertFalse(rt.isActive());
    }
}
