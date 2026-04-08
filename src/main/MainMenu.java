package main;

import bank.Account;
import bank.Bank;
import bank.Transaction;

import java.util.List;
import java.util.Scanner;

public class MainMenu {

    private static final int EXIT_SELECTION = 9;
    private static final int MAX_SELECTION = 9;

    private final Bank bank;
    private String activeAccountId;
    private final Scanner keyboardInput;

    public MainMenu() {
        this.bank = new Bank();
        this.activeAccountId = "A001";
        this.bank.addAccount(new Account(activeAccountId, 0));
        this.keyboardInput = new Scanner(System.in);
    }

    public void displayOptions() {
        System.out.println("Welcome to the 237 Bank App!");
        System.out.println("Active account: " + activeAccountId);

        System.out.println("1. Make a deposit");
        System.out.println("2. Make a withdrawal");
        System.out.println("3. Check account balance");
        System.out.println("4. View transaction history");
        System.out.println("5. Create an additional account");
        System.out.println("6. Switch active account");
        System.out.println("7. Close an account");
        System.out.println("8. Transfer money between accounts");
        System.out.println("9. Exit the app");
    }

    public int getUserSelection(int max) {
        while (true) {
            System.out.print("Please make a selection: ");
            String line = keyboardInput.nextLine().trim();

            try {
                int selection = Integer.parseInt(line);
                if (selection >= 1 && selection <= max) {
                    return selection;
                }
            } catch (NumberFormatException ignored) {
            }

            System.out.println("Enter a number from 1 to " + max + ".");
        }
    }

    public void processInput(int selection) {
        switch (selection) {
            case 1:
                performDeposit();
                break;
            case 2:
                performWithdrawal();
                break;
            case 3:
                displayAccountBalance();
                break;
            case 4:
                viewTransactionHistory();
                break;

            case 5:
                createAdditionalAccount();
                break;
            case 6:
                switchAccount();
                break;
            case 7:
                closeAccount();
                break;

            case 8:
                transferMoney();
                break;

            default:
                break;
        }
    }

    public void displayAccountBalance() {
        System.out.printf("Your current balance is: $%.2f%n", getActiveAccount().getBalance());
    }

    public void viewTransactionHistory() {
        List<Transaction> history = getActiveAccount().getTransactionHistory();

        if (history.isEmpty()) {
            System.out.println("No transactions found for this account.");
            return;
        }

        for (Transaction t : history) {
            System.out.println(t);
        }
    }

    public void performWithdrawal() {
        double amount = readPositiveMoney("How much would you like to withdraw: ");

        try {
            getActiveAccount().withdraw(amount);
        } catch (IllegalStateException e) {
            System.out.println("Withdrawal failed. Insufficient funds.");
        } catch (IllegalArgumentException e) {
            System.out.println("Withdrawal failed. " + e.getMessage());
        }
    }

    public void performDeposit() {
        double amount = readPositiveMoney("How much would you like to deposit: ");

        try {
            getActiveAccount().deposit(amount);
        } catch (IllegalArgumentException e) {
            System.out.println("Deposit failed. " + e.getMessage());
        }
    }

    private Account getActiveAccount() {
        Account acc = bank.getAccount(activeAccountId);

        if (acc == null) {
            throw new IllegalStateException("No active account: " + activeAccountId);
        }

        return acc;
    }

    private void createAdditionalAccount() {
        String newId = promptNonEmptyAccountId("New account id: ");
        if (newId == null) {
            return;
        }

        Bank.CreateAccountResult result = bank.tryCreateAccount(newId, 0);
        System.out.println(result.getMessage());

        if (result.isSuccess()) {
            activeAccountId = newId;
            System.out.println("Active account is now " + activeAccountId + ".");
        }
    }

    private void switchAccount() {
        String id = promptNonEmptyAccountId("Account id to switch to: ");
        if (id == null) {
            return;
        }

        if (bank.getAccount(id) == null) {
            System.out.println("Account not found.");
            return;
        }

        activeAccountId = id;
        System.out.println("Active account is now " + activeAccountId + ".");
    }

    private void closeAccount() {
        String id = promptNonEmptyAccountId("Account id to close: ");
        if (id == null) {
            return;
        }

        Account acc = bank.getAccount(id);
        if (acc == null) {
            System.out.println("Account not found.");
            return;
        }

        System.out.println("Current balance: " + acc.getBalance());

        if (!confirmClose("Confirm close this account? (y/n): ")) {
            System.out.println("Cancelled.");
            return;
        }

        applyCloseResult(bank.closeCustomerAccount(id));
    }

    private void applyCloseResult(Bank.CloseAccountResult result) {
        System.out.println(result.getMessage());

        if (!result.isSuccess()) {
            return;
        }

        if (bank.getAccount(activeAccountId) == null && bank.getAccountCount() >= 1) {
            activeAccountId = bank.getAllAccounts().iterator().next().getAccountNumber();
            System.out.println("Active account is now " + activeAccountId + ".");
        }
    }

    private void transferMoney() {
        if (bank.getAccountCount() < 2) {
            System.out.println("Create another account before transferring.");
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

        if (!transferAccountsExistAndDistinct(fromId, toId)) {
            return;
        }

        double amount = readPositiveMoney("Transfer amount: ");
        Bank.TransferResult result = bank.transfer(fromId, toId, amount);
        System.out.println(result.getMessage());
    }

    private boolean transferAccountsExistAndDistinct(String fromId, String toId) {
        if (fromId.equals(toId)) {
            System.out.println("Cannot transfer to the same account.");
            return false;
        }

        if (bank.getAccount(fromId) == null || bank.getAccount(toId) == null) {
            System.out.println("One or both accounts do not exist.");
            return false;
        }

        return true;
    }

    private String promptNonEmptyAccountId(String prompt) {
        System.out.print(prompt);
        String id = keyboardInput.nextLine().trim();

        if (id.isEmpty()) {
            System.out.println("Account id cannot be empty.");
            return null;
        }

        return id;
    }

    private boolean confirmClose(String prompt) {
        System.out.print(prompt);
        String line = keyboardInput.nextLine().trim();

        return line.equalsIgnoreCase("y") || line.equalsIgnoreCase("yes");
    }

    private double readPositiveMoney(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = keyboardInput.nextLine().trim();

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

    public void run() {
        int selection = -1;

        while (selection != EXIT_SELECTION) {
            displayOptions();
            selection = getUserSelection(MAX_SELECTION);
            processInput(selection);
        }
    }

    public static void main(String[] args) {
        MainMenu bankApp = new MainMenu();
        bankApp.run();
    }
}
