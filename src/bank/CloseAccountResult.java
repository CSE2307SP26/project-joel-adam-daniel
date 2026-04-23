package bank;

public final class CloseAccountResult {
    private final boolean success;
    private final String message;

    private CloseAccountResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static CloseAccountResult success() {
        return new CloseAccountResult(true, "Account closed.");
    }

    public static CloseAccountResult failure(String message) {
        return new CloseAccountResult(false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
