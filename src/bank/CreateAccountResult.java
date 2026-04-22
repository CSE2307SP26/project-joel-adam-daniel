package bank;

public final class CreateAccountResult {
    private final boolean success;
    private final String message;

    private CreateAccountResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static CreateAccountResult success(String message) {
        return new CreateAccountResult(true, message);
    }

    public static CreateAccountResult failure(String message) {
        return new CreateAccountResult(false, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
