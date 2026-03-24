package test;

import main.BankAccount;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.jupiter.api.Test;

public class BankAccountTest {

    @Test
    public void testDeposit() {
        BankAccount testAccount = new BankAccount();
        testAccount.deposit(50);
        assertEquals(50, testAccount.getBalance(), 0.01);
    }

    @Test
    public void testInvalidDeposit() {
        BankAccount testAccount = new BankAccount();
        try {
            testAccount.deposit(-50);
            fail();
        } catch (IllegalArgumentException e) {
            //do nothing, test passes
        }
    }

    @Test
    public void testWithdraw() {
        BankAccount testAccount = new BankAccount();
        try {
        testAccount.deposit(50);
        testAccount.withdraw(25);
        assertEquals(25, testAccount.getBalance(), 0.01);
        } catch (IllegalArgumentException e) {
            fail();
        }
    }

    @Test
    public void testInvalidWithdraw() {
        BankAccount testAccount = new BankAccount();
        try {
            testAccount.withdraw(-50);
            fail();
        } catch (IllegalArgumentException e) {
            //this is what we want
        }
    }

    @Test 
    public void testWithdrawMoreThanBalance() {
        BankAccount testAccount = new BankAccount();
        try {
            testAccount.deposit(50);
            testAccount.withdraw(60);
            fail();
        } catch (IllegalArgumentException e) {
            //this is what we want
        }
    }
}
