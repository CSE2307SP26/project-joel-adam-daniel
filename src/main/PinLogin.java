package main;

public class PinLogin {

    private static final int MAX_ATTEMPTS = 3;

    private int pin;
    private int failedAttempts;
    private boolean locked;

    public PinLogin(int pin) {
        if (!isValidPin(pin)) {
            throw new IllegalArgumentException("PIN must be exactly 4 digits (1000-9999).");
        }
        this.pin = pin;
        this.failedAttempts = 0;
        this.locked = false;
    }

    public boolean authenticate(int enteredPin) {
        if (locked) {
            return false;
        }
        if (enteredPin == pin) {
            failedAttempts = 0;
            return true;
        }
        failedAttempts++;
        if (failedAttempts >= MAX_ATTEMPTS) {
            locked = true;
        }
        return false;
    }

    public void changePin(int currentPin, int newPin) {
        if (!authenticate(currentPin)) {
            throw new IllegalArgumentException("Current PIN is incorrect or account is locked.");
        }
        if (!isValidPin(newPin)) {
            throw new IllegalArgumentException("New PIN must be exactly 4 digits (1000-9999).");
        }
        this.pin = newPin;
        this.failedAttempts = 0;
        this.locked = false;
    }

    public boolean isLocked() {
        return locked;
    }

    public int getRemainingAttempts() {
        return MAX_ATTEMPTS - failedAttempts;
    }

    public static boolean isValidPin(int pin) {
        return pin >= 1000 && pin <= 9999;
    }

}
