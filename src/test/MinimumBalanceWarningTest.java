package test;

import bank.Account;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinimumBalanceWarningTest {

    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream capturedOut;

    @BeforeEach
    void redirectOutput() {
        capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut));
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOut);
    }

    @Test
    void defaultThresholdIsTwentyFive() {
        Account account = new Account("A", 0);
        assertEquals(25.00, account.getMinimumBalanceThreshold(), 0.01);
    }

    @Test
    void noWarningWhenBalanceAboveThreshold() {
        Account account = new Account("B", 0);
        account.deposit(100);
        account.withdraw(50);
        assertFalse(capturedOut.toString().contains("Warning"));
    }

    @Test
    void noWarningWhenBalanceEqualsThreshold() {
        Account account = new Account("C", 0);
        account.deposit(100);
        account.withdraw(75);
        assertFalse(capturedOut.toString().contains("Warning"));
    }

    @Test
    void warningWhenBalanceDropsBelowThreshold() {
        Account account = new Account("D", 0);
        account.deposit(100);
        account.withdraw(80);
        assertTrue(capturedOut.toString().contains("Warning"));
    }

    @Test
    void warningContainsNewBalance() {
        Account account = new Account("E", 0);
        account.deposit(100);
        account.withdraw(82);
        assertTrue(capturedOut.toString().contains("$18.00"));
    }

    @Test
    void warningContainsThresholdAmount() {
        Account account = new Account("F", 0);
        account.deposit(100);
        account.withdraw(82);
        assertTrue(capturedOut.toString().contains("$25.00"));
    }

    @Test
    void customThresholdTriggersWarning() {
        Account account = new Account("G", 0);
        account.setMinimumBalanceThreshold(50.00);
        account.deposit(100);
        account.withdraw(60);
        assertTrue(capturedOut.toString().contains("Warning"));
        assertTrue(capturedOut.toString().contains("$40.00"));
        assertTrue(capturedOut.toString().contains("$50.00"));
    }

    @Test
    void customThresholdNoWarningWhenAbove() {
        Account account = new Account("H", 0);
        account.setMinimumBalanceThreshold(10.00);
        account.deposit(100);
        account.withdraw(50);
        assertFalse(capturedOut.toString().contains("Warning"));
    }

    @Test
    void zeroThresholdNeverWarns() {
        Account account = new Account("I", 0);
        account.setMinimumBalanceThreshold(0);
        account.deposit(100);
        account.withdraw(99);
        assertFalse(capturedOut.toString().contains("Warning"));
    }

    @Test
    void negativeThresholdRejected() {
        Account account = new Account("J", 0);
        assertThrows(IllegalArgumentException.class, () -> account.setMinimumBalanceThreshold(-10));
    }

    @Test
    void setAndGetThreshold() {
        Account account = new Account("K", 0);
        account.setMinimumBalanceThreshold(100.00);
        assertEquals(100.00, account.getMinimumBalanceThreshold(), 0.01);
    }
}
