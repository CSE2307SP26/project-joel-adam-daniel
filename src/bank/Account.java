package bank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Account {

    private final String accountNumber;
    private double balance;
    private final List<Transaction> transactionHistory;

    public Account(String accountNumber, double initialBalance) {
        this.accountNumber = accountNumber;
        this.balance = initialBalance;
        this.transactionHistory = new ArrayList<>();

        long now = System.currentTimeMillis();
        String openDetail =
                initialBalance == 0 ? "Account opened" : "Opened with balance " + initialBalance;
        transactionHistory.add(new Transaction(Transaction.Type.OPEN, initialBalance, now, openDetail));
    }

    // Rebuilds from disk; fullHistory must already include OPEN and agree with balance
    public static Account fromPersisted(
            String accountNumber, double balance, List<Transaction> fullHistory) {
        return new Account(accountNumber, balance, new ArrayList<>(fullHistory));
    }

    // Same in-memory shape as the public constructor, without inserting another OPEN row
    private Account(String accountNumber, double balance, List<Transaction> persistedHistory) {
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.transactionHistory = persistedHistory;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public double getBalance() {
        return balance;
    }

    public List<Transaction> getTransactionHistory() {
        return Collections.unmodifiableList(transactionHistory);
    }

    public void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }

        balance += amount;
        transactionHistory.add(
                new Transaction(Transaction.Type.DEPOSIT, amount, System.currentTimeMillis(), ""));
    }

    public void withdraw(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        if (balance < amount) {
            throw new IllegalStateException("Insufficient funds");
        }

        balance -= amount;
        transactionHistory.add(
                new Transaction(Transaction.Type.WITHDRAW, amount, System.currentTimeMillis(), ""));
    }

    public void transferIn(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        balance += amount;
        transactionHistory.add(
                new Transaction(
                        Transaction.Type.TRANSFER_IN, amount, System.currentTimeMillis(), "Transfer in"));
    }

    public void transferOut(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        if (balance < amount) {
            throw new IllegalStateException("Insufficient funds");
        }

        balance -= amount;
        transactionHistory.add(
                new Transaction(
                        Transaction.Type.TRANSFER_OUT, amount, System.currentTimeMillis(), "Transfer out"));
    }
}
