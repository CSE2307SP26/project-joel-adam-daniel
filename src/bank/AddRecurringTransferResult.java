package bank;

public final class AddRecurringTransferResult {
    private final boolean success;
    private final String message;
    private final String recurringTransferId;

    private AddRecurringTransferResult(boolean success, String message, String recurringTransferId) {
        this.success = success;
        this.message = message;
        this.recurringTransferId = recurringTransferId;
    }

    public static AddRecurringTransferResult success(String id) {
        return new AddRecurringTransferResult(true, "Recurring transfer " + id + " created.", id);
    }

    public static AddRecurringTransferResult failure(String message) {
        return new AddRecurringTransferResult(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getRecurringTransferId() {
        return recurringTransferId;
    }
}
