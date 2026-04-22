package bank;

public final class ProcessedRecurringTransferResult {
    private final String recurringTransferId;
    private final boolean success;
    private final String message;

    public ProcessedRecurringTransferResult(String recurringTransferId, boolean success, String message) {
        this.recurringTransferId = recurringTransferId;
        this.success = success;
        this.message = message;
    }

    public String getRecurringTransferId() {
        return recurringTransferId;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
