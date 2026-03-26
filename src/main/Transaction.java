package main;

import java.util.Date;

public class Transaction {

    private double amount;
    private String type;
    private Date date;

    public Transaction(double amount, String type, Date date) {
        this.amount = amount;
        this.type = type;
        this.date = date;
    }

    public double getAmount() {
        return this.amount;
    }

    public String getType() {
        return this.type;
    }

    public Date getDate() {
        return this.date;
    }
}
