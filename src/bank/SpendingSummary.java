package bank;

public final class SpendingSummary {

    private final double totalDeposited;
    private final double totalWithdrawn;
    private final int depositCount;
    private final int withdrawalCount;
    private final long fromMs;
    private final long toMs;

    SpendingSummary(double totalDeposited, double totalWithdrawn,
                    int depositCount, int withdrawalCount,
                    long fromMs, long toMs) {
        this.totalDeposited = totalDeposited;
        this.totalWithdrawn = totalWithdrawn;
        this.depositCount = depositCount;
        this.withdrawalCount = withdrawalCount;
        this.fromMs = fromMs;
        this.toMs = toMs;
    }

    public double getTotalDeposited() {
        return totalDeposited;
    }

    public double getTotalWithdrawn() {
        return totalWithdrawn;
    }

    public int getDepositCount() {
        return depositCount;
    }

    public int getWithdrawalCount() {
        return withdrawalCount;
    }

    public double getNetChange() {
        return totalDeposited - totalWithdrawn;
    }

    public long getFromMs() {
        return fromMs;
    }

    public long getToMs() {
        return toMs;
    }
}
