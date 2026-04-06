package test;

import main.BankAccount;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MinimumBalanceWarningTest {

    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream capturedOut;

    @BeforeEach
    public void redirectOutput() {
        capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut));
    }

    @AfterEach
    public void restoreOutput() {
        System.setOut(originalOut);
    }

    @Test
    public void testDefaultThresholdIsTwentyFive() {
        BankAccount account = new BankAccount();
        assertEquals(25.00, account.getMinimumBalanceThreshold(), 0.01);
    }

    @Test
    public void testNoWarningWhenBalanceAboveThreshold() {
        BankAccount account = new BankAccount();
        account.deposit(100);
        account.withdraw(50);
        assertFalse(capturedOut.toString().contains("Warning"));
    }

    @Test
    public void testNoWarningWhenBalanceEqualsThreshold() {
        BankAccount account = new BankAccount();
        account.deposit(100);
        account.withdraw(75);
        assertFalse(capturedOut.toString().contains("Warning"));
    }

    @Test
    public void testWarningWhenBalanceDropsBelowThreshold() {
        BankAccount account = new BankAccount();
        account.deposit(100);
        account.withdraw(80);
        assertTrue(capturedOut.toString().contains("Warning"));
    }

    @Test
    public void testWarningContainsNewBalance() {
        BankAccount account = new BankAccount();
        account.deposit(100);
        account.withdraw(82);
        assertTrue(capturedOut.toString().contains("$18.00"));
    }

    @Test
    public void testWarningContainsThresholdAmount() {
        BankAccount account = new BankAccount();
        account.deposit(100);
        account.withdraw(82);
        assertTrue(capturedOut.toString().contains("$25.00"));
    }

    @Test
    public void testCustomThresholdTriggersWarning() {
        BankAccount account = new BankAccount();
        account.setMinimumBalanceThreshold(50.00);
        account.deposit(100);
        account.withdraw(60);
        assertTrue(capturedOut.toString().contains("Warning"));
        assertTrue(capturedOut.toString().contains("$40.00"));
        assertTrue(capturedOut.toString().contains("$50.00"));
    }

    @Test
    public void testCustomThresholdNoWarningWhenAbove() {
        BankAccount account = new BankAccount();
        account.setMinimumBalanceThreshold(10.00);
        account.deposit(100);
        account.withdraw(50);
        assertFalse(capturedOut.toString().contains("Warning"));
    }

    @Test
    public void testZeroThresholdNeverWarns() {
        BankAccount account = new BankAccount();
        account.setMinimumBalanceThreshold(0);
        account.deposit(100);
        account.withdraw(99);
        assertFalse(capturedOut.toString().contains("Warning"));
    }

    @Test
    public void testNegativeThresholdRejected() {
        BankAccount account = new BankAccount();
        try {
            account.setMinimumBalanceThreshold(-10);
            fail("Expected IllegalArgumentException for negative threshold");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testSetAndGetThreshold() {
        BankAccount account = new BankAccount();
        account.setMinimumBalanceThreshold(100.00);
        assertEquals(100.00, account.getMinimumBalanceThreshold(), 0.01);
    }

}
