package main;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public class MainMenu {

    private static final int EXIT_SELECTION = 9;
    private static final int MAX_SELECTION = 9;

    private final Map<String, BankAccount> accounts;
    private String activeAccountId;
    private Scanner keyboardInput;

    public MainMenu() {
        this.accounts = new LinkedHashMap<>();
        this.activeAccountId = "A001";
        this.accounts.put(activeAccountId, new BankAccount());
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
        int selection = -1;
        while(selection < 1 || selection > max) {
            System.out.print("Please make a selection: ");
            selection = keyboardInput.nextInt();
        }
        return selection;
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
        getActiveAccount().printTransactionHistory();
    }
    
    public void performWithdrawal() {
        double withdrawalAmount = -1;

        while(withdrawalAmount < 0) { 
            System.out.print("How much would you like to withdraw: ");
            withdrawalAmount = keyboardInput.nextInt();
        }

        try {
            getActiveAccount().withdraw(withdrawalAmount);
        } catch (IllegalArgumentException e) {
            System.out.println("Withdrawal failed. Please check amount and balance.");
        }
    }
    public void performDeposit() {
        double depositAmount = -1;
        while(depositAmount < 0) {
            System.out.print("How much would you like to deposit: ");
            depositAmount = keyboardInput.nextInt();
        }
        getActiveAccount().deposit(depositAmount);
    }

    private BankAccount getActiveAccount() {
        return accounts.get(activeAccountId);
    }

    private void createAdditionalAccount() {
        keyboardInput.nextLine();
        System.out.print("New account id: ");
        String newId = keyboardInput.nextLine().trim();
        if (newId.isEmpty()) {
            System.out.println("Account id cannot be empty.");
            return;
        }
        if (accounts.containsKey(newId)) {
            System.out.println("An account with that id already exists.");
            return;
        }
        accounts.put(newId, new BankAccount());
        System.out.println("Created account " + newId + ".");
    }

    private void switchAccount() {
        keyboardInput.nextLine();
        System.out.print("Account id to switch to: ");
        String id = keyboardInput.nextLine().trim();
        if (!accounts.containsKey(id)) {
            System.out.println("Account not found.");
            return;
        }
        activeAccountId = id;
        System.out.println("Active account is now " + activeAccountId + ".");
    }

    private void closeAccount() {
        keyboardInput.nextLine();
        System.out.print("Account id to close: ");
        String id = keyboardInput.nextLine().trim();
        if (!accounts.containsKey(id)) {
            System.out.println("Account not found.");
            return;
        }
        if (accounts.size() == 1) {
            System.out.println("Cannot close the only remaining account.");
            return;
        }
        if (accounts.get(id).getBalance() != 0) {
            System.out.println("Account balance must be zero before closing.");
            return;
        }
        accounts.remove(id);
        if (id.equals(activeAccountId)) {
            activeAccountId = accounts.keySet().iterator().next();
        }
        System.out.println("Closed account " + id + ".");
    }

    private void transferMoney() {
        if (accounts.size() < 2) {
            System.out.println("Create another account before transferring.");
            return;
        }
        keyboardInput.nextLine();
        System.out.print("From account id: ");
        String fromId = keyboardInput.nextLine().trim();
        System.out.print("To account id: ");
        String toId = keyboardInput.nextLine().trim();
        if (fromId.equals(toId)) {
            System.out.println("Cannot transfer to the same account.");
            return;
        }
        BankAccount from = accounts.get(fromId);
        BankAccount to = accounts.get(toId);
        if (from == null || to == null) {
            System.out.println("One or both accounts do not exist.");
            return;
        }
        System.out.print("Transfer amount: ");
        if (!keyboardInput.hasNextDouble()) {
            System.out.println("Invalid amount.");
            return;
        }
        double amount = keyboardInput.nextDouble();
        if (amount <= 0) {
            System.out.println("Transfer amount must be positive.");
            return;
        }
        try {
            from.withdraw(amount);
            to.deposit(amount);
            System.out.println("Transfer complete.");
        } catch (IllegalArgumentException e) {
            System.out.println("Transfer failed due to insufficient funds or invalid amount.");
        }
    }

    public void run() {
        int selection = -1;
        while(selection != EXIT_SELECTION) {
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
