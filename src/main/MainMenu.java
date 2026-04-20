package main;

import bank.Account;
import bank.AccountType;
import bank.Bank;
import bank.BankPersistence;
import bank.PinLogin;
import bank.SpendingSummary;
import bank.Transaction;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class MainMenu {

    private static final int EXIT_SELECTION = 13;
    private static final int MAX_SELECTION = 13;

    private final Bank bank;
    private String activeAccountId;
    private final Scanner keyboardInput;
    private final Path dataPath;

    public MainMenu() {
        this(BankPersistence.defaultPath());
    }

    // dataPath lets tests use a temp file; production uses bank-data.txt in the working directory
    public MainMenu(Path dataPath) {
        this.dataPath = dataPath;
        this.keyboardInput = new Scanner(System.in);

        Optional<BankPersistence.BankSnapshot> snapshot = BankPersistence.tryLoad(dataPath);

        if (snapshot.isPresent()) {
            this.bank = snapshot.get().getBank();
            this.activeAccountId = snapshot.get().getActiveAccountId();
        } else {
            // No save file yet — same seed account as before persistence existed
            this.bank = new Bank();
            this.activeAccountId = "A001";
            int seedPin = readFourDigitPin("Set a 4-digit PIN for account A001 (1000-9999): ");
            this.bank.addAccount(new Account(activeAccountId, 0, AccountType.CHECKING, seedPin));
        }
    }

    public void displayOptions() {
        System.out.println("Welcome to the 237 Bank App!");
        Account active = getActiveAccount();
        String frozenTag = active.isFrozen() ? " [FROZEN]" : "";
        System.out.println(
                "Active account: "
                        + activeAccountId
                        + " ("
                        + active.getAccountType().getLabel()
                        + ")"
                        + frozenTag);
        System.out.println(active.getAccountRulesSummary());

        System.out.println("1. Make a deposit");
        System.out.println("2. Make a withdrawal");
        System.out.println("3. Check account balance");
        System.out.println("4. View transaction history");
        System.out.println("5. Create an additional account");
        System.out.println("6. Switch active account");
        System.out.println("7. Close an account");
        System.out.println("8. Transfer money between accounts");
        System.out.println("9. View balances for all accounts");
        System.out.println("10. Operator — freeze account");
        System.out.println("11. Operator — unfreeze account");
        System.out.println("12. View spending summary");
        System.out.println("13. Exit the app");
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

            case 9:
                displayAllAccountBalances();
                break;

            case 10:
                operatorSetFrozen(true);
                break;
            case 11:
                operatorSetFrozen(false);
                break;

            case 12:
                viewSpendingSummary();
                break;

            case 13:
                persistBankState();
                System.out.println("Goodbye!");
                break;

            default:
                break;
        }
    }

    public void displayAccountBalance() {
        System.out.printf("Your current balance is: $%.2f%n", getActiveAccount().getBalance());
    }

    public void displayAllAccountBalances() {
        System.out.println("Balances for all accounts:");
        for (Account acc : bank.getAllAccounts()) {
            String marker = acc.getAccountNumber().equals(activeAccountId) ? " (active)" : "";
            String frozen = acc.isFrozen() ? " [FROZEN]" : "";
            System.out.printf(
                    "  %s — %s: $%.2f%s%s%n",
                    acc.getAccountNumber(),
                    acc.getAccountType().getLabel(),
                    acc.getBalance(),
                    frozen,
                    marker);
        }
    }

    public void viewTransactionHistory() {
        System.out.println("1. All transactions");
        System.out.println("2. Filter by type");

        int mode = getUserSelection(2);

        List<Transaction> history;

        if (mode == 2) {
            Transaction.Type type = promptTransactionType();
            if (type == null) {
                System.out.println("Cancelled.");
                return;
            }

            history = getActiveAccount().getTransactionHistoryByType(type);

            if (history.isEmpty()) {
                System.out.println("No transactions of type " + type + " for this account.");
                return;
            }
        } else {
            history = getActiveAccount().getTransactionHistory();

            if (history.isEmpty()) {
                System.out.println("No transactions found for this account.");
                return;
            }
        }

        for (Transaction t : history) {
            System.out.println(t);
        }
    }

    private Transaction.Type promptTransactionType() {
        Transaction.Type[] types = Transaction.Type.values();

        System.out.println("Select transaction type:");
        for (int i = 0; i < types.length; i++) {
            System.out.println((i + 1) + ". " + types[i]);
        }
        System.out.println((types.length + 1) + ". Cancel");

        int sel = getUserSelection(types.length + 1);

        if (sel == types.length + 1) {
            return null;
        }

        return types[sel - 1];
    }

    // Called on exit so the next launch can tryLoad the same balances and history
    private void persistBankState() {
        try {
            BankPersistence.save(bank, activeAccountId, dataPath);
        } catch (IOException e) {
            System.err.println("Could not save bank data: " + e.getMessage());
        }
    }

    public void performWithdrawal() {
        double amount = readPositiveMoney("How much would you like to withdraw: ");

        try {
            getActiveAccount().withdraw(amount);
        } catch (IllegalStateException e) {
            System.out.println("Withdrawal failed. " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Withdrawal failed. " + e.getMessage());
        }
    }

    public void performDeposit() {
        double amount = readPositiveMoney("How much would you like to deposit: ");

        try {
            getActiveAccount().deposit(amount);
        } catch (IllegalStateException e) {
            System.out.println("Deposit failed. " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Deposit failed. " + e.getMessage());
        }
    }

    private void operatorSetFrozen(boolean freeze) {
        String id = promptNonEmptyAccountId("Operator — account id: ");
        if (id == null) {
            return;
        }
        System.out.println(bank.setAccountFrozen(id, freeze).getMessage());
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

        System.out.println("Account type: 1. Checking  2. Savings");
        int typeChoice = getUserSelection(2);
        AccountType type = typeChoice == 1 ? AccountType.CHECKING : AccountType.SAVINGS;

        double initial;
        if (type == AccountType.SAVINGS) {
            System.out.println(
                    "Savings accounts require a minimum opening deposit of $"
                            + Account.SAVINGS_MIN_OPENING_BALANCE
                            + ".");
            initial = readSavingsOpeningDeposit();
        } else {
            initial = readNonNegativeMoney("Initial deposit (0 is ok): ");
        }

        int pin = readFourDigitPin("Choose a 4-digit PIN for this account (1000-9999): ");
        Bank.CreateAccountResult result = bank.tryCreateAccount(newId, initial, type, pin);
        System.out.println(result.getMessage());

        if (result.isSuccess()) {
            activeAccountId = newId;
            System.out.println("Active account is now " + activeAccountId + ".");
        }
    }

    private double readSavingsOpeningDeposit() {
        while (true) {
            double amount = readPositiveMoney("Opening deposit amount: ");
            if (amount + 1e-9 >= Account.SAVINGS_MIN_OPENING_BALANCE) {
                return amount;
            }
            System.out.println(
                    "That amount is below the savings minimum. You need at least $"
                            + Account.SAVINGS_MIN_OPENING_BALANCE
                            + ".");
        }
    }

    private double readNonNegativeMoney(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = keyboardInput.nextLine().trim();
            try {
                double amount = Double.parseDouble(line);
                if (amount >= 0 && !Double.isNaN(amount)) {
                    return amount;
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.println("Enter zero or a positive dollar amount.");
        }
    }

    private void switchAccount() {
        String id = promptNonEmptyAccountId("Account id to switch to: ");
        if (id == null) {
            return;
        }

        Account target = bank.getAccount(id);
        if (target == null) {
            System.out.println("Account not found.");
            return;
        }

        if (id.equals(activeAccountId)) {
            System.out.println("That account is already active.");
            return;
        }

        if (!verifyPinForAccount(target)) {
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

    private int readFourDigitPin(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = keyboardInput.nextLine().trim();
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

    private boolean verifyPinForAccount(Account acc) {
        if (acc.isPinLocked()) {
            System.out.println("That account is locked due to too many incorrect PIN attempts.");
            return false;
        }

        while (true) {
            System.out.print("Enter PIN for account " + acc.getAccountNumber() + ": ");
            String line = keyboardInput.nextLine().trim();
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

    private void viewSpendingSummary() {
        System.out.println("Spending summary for account: " + activeAccountId);
        LocalDate from = readDate("Start date (YYYY-MM-DD): ");
        LocalDate to = readDate("End date   (YYYY-MM-DD): ");

        ZoneId zone = ZoneId.systemDefault();
        long fromMs = from.atStartOfDay(zone).toInstant().toEpochMilli();
        long toMs = to.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1;

        Bank.SpendingSummaryResult result = bank.getSpendingSummary(activeAccountId, fromMs, toMs);
        if (!result.isSuccess()) {
            System.out.println(result.getMessage());
            return;
        }

        SpendingSummary s = result.getSummary();
        System.out.printf("Period:      %s to %s%n", from, to);
        System.out.printf("Inflows:    +$%.2f (%d transaction(s))%n", s.getTotalDeposited(), s.getDepositCount());
        System.out.printf("Outflows:   -$%.2f (%d transaction(s))%n", s.getTotalWithdrawn(), s.getWithdrawalCount());
        System.out.printf("Net change:  $%.2f%n", s.getNetChange());
    }

    private LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = keyboardInput.nextLine().trim();
            try {
                return LocalDate.parse(line);
            } catch (DateTimeParseException ignored) {
            }
            System.out.println("Enter a date in YYYY-MM-DD format (e.g. 2025-01-31).");
        }
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
