package main;

import bank.AddRecurringTransferResult;
import bank.Bank;
import bank.OperatorResult;
import bank.ProcessedRecurringTransferResult;
import bank.RecurringTransfer;

import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.util.function.IntUnaryOperator;

/** CLI submenu for recurring transfers. */
final class RecurringTransferConsole {

    private final Scanner in;
    private final Bank bank;
    private final IntUnaryOperator readChoiceUpToMax;

    RecurringTransferConsole(Scanner in, Bank bank, IntUnaryOperator readChoiceUpToMax) {
        this.in = in;
        this.bank = bank;
        this.readChoiceUpToMax = readChoiceUpToMax;
    }

    void manageRecurringTransfers() {
        System.out.println("--- Recurring Transfers ---");
        System.out.println("1. Set up a new recurring transfer");
        System.out.println("2. List recurring transfers");
        System.out.println("3. Cancel a recurring transfer");
        System.out.println("4. Process due transfers now");
        System.out.println("5. Back");

        int choice = readChoiceUpToMax.applyAsInt(5);
        switch (choice) {
            case 1:
                createRecurringTransfer();
                break;
            case 2:
                listRecurringTransfers();
                break;
            case 3:
                cancelRecurringTransfer();
                break;
            case 4:
                processRecurringTransfersNow();
                break;
            default:
                break;
        }
    }

    private void createRecurringTransfer() {
        if (bank.getAccountCount() < 2) {
            System.out.println("Create another account before setting up a recurring transfer.");
            return;
        }

        String fromId = promptNonEmptyAccountId("From account id: ");
        if (fromId == null) {
            return;
        }

        String toId = promptNonEmptyAccountId("To account id: ");
        if (toId == null) {
            return;
        }

        double amount = readPositiveMoney("Transfer amount: ");

        System.out.println("Repeat every how many days? (e.g. 7 for weekly, 30 for monthly)");
        int intervalDays = readPositiveInt("Interval (days): ");

        AddRecurringTransferResult result = bank.addRecurringTransfer(fromId, toId, amount, intervalDays);
        System.out.println(result.getMessage());
        if (result.isSuccess()) {
            System.out.println("First transfer will run in " + intervalDays + " day(s).");
        }
    }

    private void listRecurringTransfers() {
        Collection<RecurringTransfer> all = bank.getAllRecurringTransfers();
        if (all.isEmpty()) {
            System.out.println("No recurring transfers set up.");
            return;
        }
        System.out.printf("%-8s %-10s %-10s %10s %10s %s%n",
                "ID", "From", "To", "Amount", "Every", "Status");
        for (RecurringTransfer rt : all) {
            System.out.printf("%-8s %-10s %-10s %10.2f %7d day(s) %s%n",
                    rt.getId(),
                    rt.getFromAccountId(),
                    rt.getToAccountId(),
                    rt.getAmount(),
                    rt.getIntervalDays(),
                    rt.isActive() ? "Active" : "Cancelled");
        }
    }

    private void cancelRecurringTransfer() {
        listRecurringTransfers();
        String id = promptNonEmptyAccountId("Recurring transfer id to cancel: ");
        if (id == null) {
            return;
        }
        OperatorResult r = bank.cancelRecurringTransfer(id);
        System.out.println(r.getMessage());
    }

    private void processRecurringTransfersNow() {
        List<ProcessedRecurringTransferResult> results = bank.processDueRecurringTransfers();
        if (results.isEmpty()) {
            System.out.println("No transfers are due right now.");
            return;
        }
        for (ProcessedRecurringTransferResult r : results) {
            System.out.println((r.isSuccess() ? "[OK] " : "[FAILED] ") + r.getMessage());
        }
    }

    private int readPositiveInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = in.nextLine().trim();
            try {
                int value = Integer.parseInt(line);
                if (value > 0) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.println("Enter a positive whole number.");
        }
    }

    private double readPositiveMoney(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = in.nextLine().trim();

            try {
                double amount = Double.parseDouble(line);
                if (amount > 0 && !Double.isNaN(amount)) {
                    return amount;
                }
            } catch (NumberFormatException ignored) {
            }

            System.out.println("Enter a positive dollar amount (e.g. 10 or 25.50).");
        }
    }

    private String promptNonEmptyAccountId(String prompt) {
        System.out.print(prompt);
        String id = in.nextLine().trim();

        if (id.isEmpty()) {
            System.out.println("Account id cannot be empty.");
            return null;
        }

        return id;
    }
}
