package bank;

public final class TransferResult {
    private final boolean success;
    private final String message;

    private TransferResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static TransferResult success(String message) {
        return new TransferResult(true, message);
    }

    public static TransferResult failure(String message) {
        return new TransferResult(false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
