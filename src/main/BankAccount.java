package main;

import java.util.*;

public class BankAccount {

    private double balance;
    private List<Transaction> transactionHistory;

    public BankAccount() {
        this.transactionHistory = new ArrayList<>();
        this.balance = 0;
        
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
        } else {
            throw new IllegalArgumentException();
        }
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
