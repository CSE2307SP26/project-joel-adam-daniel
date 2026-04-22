package bank;

import java.util.Date;

public final class Transaction {

    public enum Type {
        OPEN("Account opened"),
        DEPOSIT("Deposits"),
        WITHDRAW("Withdrawals"),
        TRANSFER_IN("Transfers in"),
        TRANSFER_OUT("Transfers out"),
        FEE("Administrator fees"),
        INTEREST("Interest credits");

        private final String auditLabel;

        Type(String auditLabel) {
            this.auditLabel = auditLabel;
        }

        /** Short label for customer-facing filters and reports. */
        public String getAuditLabel() {
            return auditLabel;
        }
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
