package main;

import java.util.*;

public class BankAccount {

    private static final double DEFAULT_MINIMUM_BALANCE = 25.00;

    private double balance;
    private List<Transaction> transactionHistory;
    private double minimumBalanceThreshold;

    public BankAccount() {
        this.transactionHistory = new ArrayList<>();
        this.balance = 0;
        this.minimumBalanceThreshold = DEFAULT_MINIMUM_BALANCE;
    }

    public void deposit(double amount) {
        if(amount > 0) {
            this.balance += amount;
            this.transactionHistory.add(new Transaction(amount, "Deposit", new Date()));
        } else {
            throw new IllegalArgumentException();
        
        }
    }

    public void withdraw(double amount) {
        if(amount > 0 && amount <= this.balance) {
            this.balance -= amount;
            this.transactionHistory.add(new Transaction(amount, "Withdrawal", new Date()));
            if (this.balance < this.minimumBalanceThreshold) {
                System.out.printf("Warning: Your balance ($%.2f) is below the minimum balance of $%.2f.%n",
                        this.balance, this.minimumBalanceThreshold);
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void setMinimumBalanceThreshold(double threshold) {
        if (threshold < 0) {
            throw new IllegalArgumentException("Minimum balance threshold cannot be negative.");
        }
        this.minimumBalanceThreshold = threshold;
    }

    public double getMinimumBalanceThreshold() {
        return this.minimumBalanceThreshold;
    }

    public void printTransactionHistory() {
        if (this.transactionHistory.isEmpty()) {
            System.out.println("No transactions found for this account.");
            return;
        }

        for (Transaction transaction : this.transactionHistory) {
            System.out.println(transaction.getType() + " : " + transaction.getAmount() + " at " + transaction.getDate());
        }
    }

    public List<Transaction> getTransactionHistory() {
        return this.transactionHistory;
    }

    public double getBalance() {
        return this.balance;
    }
}
