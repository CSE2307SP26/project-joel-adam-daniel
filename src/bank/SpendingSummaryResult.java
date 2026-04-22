package bank;

public final class SpendingSummaryResult {
    private final boolean success;
    private final String message;
    private final SpendingSummary summary;

    private SpendingSummaryResult(boolean success, String message, SpendingSummary summary) {
        this.success = success;
        this.message = message;
        this.summary = summary;
    }

    public static SpendingSummaryResult success(SpendingSummary summary) {
        return new SpendingSummaryResult(true, null, summary);
    }

    public static SpendingSummaryResult failure(String message) {
        return new SpendingSummaryResult(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public SpendingSummary getSummary() {
        return summary;
    }
}
