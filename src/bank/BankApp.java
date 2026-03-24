package bank;

import java.math.BigDecimal;
import java.util.Scanner;

/**
 * Terminal bank customer app. User story 6: close an existing account.
 */
public class BankApp {
    public static void main(String[] args) {
        Bank bank = demoBank();
        Scanner in = new Scanner(System.in);
        System.out.println("Bank — customer (terminal)");
        try {
            runMenu(bank, in);
        } finally {
            in.close();
        }
    }

    private static void runMenu(Bank bank, Scanner in) {
        while (true) {
            System.out.println();
            System.out.println("1) List accounts");
            System.out.println("2) Close an account");
            System.out.println("0) Quit");
            System.out.print("Choice: ");
            String line = in.nextLine().trim();
            switch (line) {
                case "1":
                    listAccounts(bank);
                    break;
                case "2":
                    closeAccountInteractive(bank, in);
                    break;
                case "0":
                    System.out.println("Goodbye.");
                    return;
                default:
                    System.out.println("Unknown option. Try 1, 2, or 0.");
            }
        }
    }

    private static void listAccounts(Bank bank) {
        if (bank.getAllAccounts().isEmpty()) {
            System.out.println("(no accounts)");
            return;
        }
        for (Account a : bank.getAllAccounts()) {
            System.out.println("  " + a.getAccountNumber() + " — balance: " + a.getBalance());
        }
    }

    private static void closeAccountInteractive(Bank bank, Scanner in) {
        if (bank.getAllAccounts().isEmpty()) {
            System.out.println("No accounts to close.");
            return;
        }
        System.out.print("Account number to close: ");
        String id = in.nextLine().trim();
        Account acc = bank.getAccount(id);
        if (acc == null) {
            System.out.println("Account not found.");
            return;
        }
        System.out.println("Current balance: " + acc.getBalance());
        System.out.print("Confirm close this account? (y/n): ");
        String confirm = in.nextLine().trim();
        if (!confirm.equalsIgnoreCase("y") && !confirm.equalsIgnoreCase("yes")) {
            System.out.println("Cancelled.");
            return;
        }
        Bank.CloseAccountResult result = bank.closeAccount(id);
        System.out.println(result.getMessage());
    }

    static Bank demoBank() {
        Bank b = new Bank();
        b.addAccount(new Account("1001", new BigDecimal("250.00")));
        b.addAccount(new Account("1002", BigDecimal.ZERO));
        return b;
    }
}
