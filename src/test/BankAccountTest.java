package test;

import main.BankAccount;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
    public void testInitialBalance() {
        BankAccount testAccount = new BankAccount();
        assertEquals(0, testAccount.getBalance(), 0.01);
    }

    @Test
    public void testBalanceAfterDepositAndWithdraw() {
        BankAccount testAccount = new BankAccount();
        testAccount.deposit(100);
        testAccount.withdraw(37.5);
        assertEquals(62.5, testAccount.getBalance(), 0.01);
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

    @Test
    public void testTransactionHistory() {
        BankAccount testAccount = new BankAccount();
        testAccount.deposit(50);
        testAccount.withdraw(25);
        assertEquals(2, testAccount.getTransactionHistory().size());
        assertEquals(50, testAccount.getTransactionHistory().get(0).getAmount(), 0.01);
        assertEquals(25, testAccount.getTransactionHistory().get(1).getAmount(), 0.01);
        assertEquals("Deposit", testAccount.getTransactionHistory().get(0).getType());
        assertEquals("Withdrawal", testAccount.getTransactionHistory().get(1).getType());

        assertNotNull(testAccount.getTransactionHistory().get(0).getDate());
        assertNotNull(testAccount.getTransactionHistory().get(1).getDate());
    }
}
