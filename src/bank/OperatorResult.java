package bank;

public final class OperatorResult {
    private final boolean success;
    private final String message;

    private OperatorResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static OperatorResult success(String message) {
        return new OperatorResult(true, message);
    }

    public static OperatorResult failure(String message) {
        return new OperatorResult(false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
