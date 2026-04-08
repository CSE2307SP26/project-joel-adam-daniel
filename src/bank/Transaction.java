package bank;

import java.util.Date;

public final class Transaction {

    public enum Type {
        OPEN,
        DEPOSIT,
        WITHDRAW,
        TRANSFER_IN,
        TRANSFER_OUT
    }

    private final Type type;
    private final double amount;
    private final long whenMs;
    private final String detail;

    public Transaction(Type type, double amount, long whenMs, String detail) {
        this.type = type;
        this.amount = amount;
        this.whenMs = whenMs;
        this.detail = detail != null ? detail : "";
    }

    public Type getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public long getWhenMs() {
        return whenMs;
    }

    public String getDetail() {
        return detail;
    }

    @Override
    public String toString() {
        String tail = detail.isEmpty() ? "" : " - " + detail;

        return type + " " + amount + " @ " + new Date(whenMs) + tail;
    }
}
