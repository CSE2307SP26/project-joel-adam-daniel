package main;

import bank.Account;
import bank.PinLogin;

import java.util.Scanner;

/** Console prompts and verification loop for 4-digit PINs (uses {@link PinLogin} rules only). */
final class PinConsole {

    private final Scanner in;

    PinConsole(Scanner in) {
        this.in = in;
    }

    int readFourDigitPin(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = in.nextLine().trim();
            try {
                int pin = Integer.parseInt(line);
                if (PinLogin.isValidPin(pin)) {
                    return pin;
                }
            } catch (NumberFormatException ignored) {
            }

            System.out.println("PIN must be exactly 4 digits in the range 1000-9999.");
        }
    }

    boolean verifyPinForAccount(Account acc) {
        if (acc.isPinLocked()) {
            System.out.println("That account is locked due to too many incorrect PIN attempts.");
            return false;
        }

        while (true) {
            System.out.print("Enter PIN for account " + acc.getAccountNumber() + ": ");
            String line = in.nextLine().trim();
            int entered;
            try {
                entered = Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("PIN must be numeric.");
                continue;
            }

            if (acc.authenticatePin(entered)) {
                return true;
            }

            if (acc.isPinLocked()) {
                System.out.println("Too many failed attempts. This account is now locked.");
                return false;
            }

            System.out.println("Incorrect PIN. Attempts remaining: " + acc.getPinRemainingAttempts());
        }
    }
}
