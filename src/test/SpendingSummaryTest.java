package test;

import bank.SpendingSummary;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link SpendingSummary} value object (package-private constructor — constructed here
 * via reflection; production code builds instances through {@link bank.Account#getSpendingSummary}).
 */
class SpendingSummaryTest {

    private static SpendingSummary newSummary(
            double totalDeposited,
            double totalWithdrawn,
            int depositCount,
            int withdrawalCount,
            long fromMs,
            long toMs) {
        try {
            Constructor<SpendingSummary> c =
                    SpendingSummary.class.getDeclaredConstructor(
                            double.class,
                            double.class,
                            int.class,
                            int.class,
                            long.class,
                            long.class);
            c.setAccessible(true);
            return c.newInstance(totalDeposited, totalWithdrawn, depositCount, withdrawalCount, fromMs, toMs);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void netChangeIsDepositsMinusWithdrawals() {
        SpendingSummary s = newSummary(100, 30, 2, 1, 0L, 1L);
        assertEquals(70.0, s.getNetChange(), 0.001);
    }

    @Test
    void gettersReturnConstructionValues() {
        SpendingSummary s = newSummary(10.5, 2.5, 3, 4, 100L, 200L);
        assertEquals(10.5, s.getTotalDeposited(), 0.001);
        assertEquals(2.5, s.getTotalWithdrawn(), 0.001);
        assertEquals(3, s.getDepositCount());
        assertEquals(4, s.getWithdrawalCount());
        assertEquals(100L, s.getFromMs());
        assertEquals(200L, s.getToMs());
    }

    @Test
    void zeroCountsAndAmounts() {
        SpendingSummary s = newSummary(0, 0, 0, 0, 5L, 6L);
        assertEquals(0.0, s.getNetChange(), 0.001);
    }
}
