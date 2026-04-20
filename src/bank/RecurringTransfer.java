package bank;

public final class RecurringTransfer {

    private final String id;
    private final String fromAccountId;
    private final String toAccountId;
    private final double amount;
    private final int intervalDays;
    private long nextRunMs;
    private boolean active;

    public RecurringTransfer(String id, String fromAccountId, String toAccountId,
                      double amount, int intervalDays, long nextRunMs, boolean active) {
        this.id = id;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.intervalDays = intervalDays;
        this.nextRunMs = nextRunMs;
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public String getFromAccountId() {
        return fromAccountId;
    }

    public String getToAccountId() {
        return toAccountId;
    }

    public double getAmount() {
        return amount;
    }

    public int getIntervalDays() {
        return intervalDays;
    }

    public long getNextRunMs() {
        return nextRunMs;
    }

    public boolean isActive() {
        return active;
    }

    void setNextRunMs(long nextRunMs) {
        this.nextRunMs = nextRunMs;
    }

    void cancel() {
        this.active = false;
    }
}
