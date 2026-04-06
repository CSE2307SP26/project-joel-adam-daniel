package test;

import main.PinLogin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.jupiter.api.Test;

public class PinLoginTest {

    @Test
    public void testValidPinCreation() {
        PinLogin login = new PinLogin(1234);
        assertFalse(login.isLocked());
    }

    @Test
    public void testInvalidPinTooShort() {
        try {
            new PinLogin(999);
            fail("Expected IllegalArgumentException for 3-digit PIN");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testInvalidPinTooLong() {
        try {
            new PinLogin(10000);
            fail("Expected IllegalArgumentException for 5-digit PIN");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testInvalidPinNegative() {
        try {
            new PinLogin(-1234);
            fail("Expected IllegalArgumentException for negative PIN");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testSuccessfulAuthentication() {
        PinLogin login = new PinLogin(5678);
        assertTrue(login.authenticate(5678));
    }

    @Test
    public void testFailedAuthentication() {
        PinLogin login = new PinLogin(5678);
        assertFalse(login.authenticate(1111));
    }

    @Test
    public void testLockoutAfterThreeFailures() {
        PinLogin login = new PinLogin(5678);
        login.authenticate(0000);
        login.authenticate(1111);
        login.authenticate(2222);
        assertTrue(login.isLocked());
    }

    @Test
    public void testAuthenticationReturnsFalseWhenLocked() {
        PinLogin login = new PinLogin(5678);
        login.authenticate(1111);
        login.authenticate(1111);
        login.authenticate(1111);
        assertFalse(login.authenticate(5678));
    }

    @Test
    public void testRemainingAttemptsDecrement() {
        PinLogin login = new PinLogin(5678);
        assertTrue(login.getRemainingAttempts() == 3);
        login.authenticate(1111);
        assertTrue(login.getRemainingAttempts() == 2);
        login.authenticate(1111);
        assertTrue(login.getRemainingAttempts() == 1);
    }

    @Test
    public void testRemainingAttemptsResetOnSuccess() {
        PinLogin login = new PinLogin(5678);
        login.authenticate(1111);
        login.authenticate(5678);
        assertTrue(login.getRemainingAttempts() == 3);
    }

    @Test
    public void testChangePinSuccess() {
        PinLogin login = new PinLogin(1234);
        login.changePin(1234, 5678);
        assertTrue(login.authenticate(5678));
    }

    @Test
    public void testChangePinFailsWithWrongCurrentPin() {
        PinLogin login = new PinLogin(1234);
        try {
            login.changePin(9999, 5678);
            fail("Expected IllegalArgumentException for wrong current PIN");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testChangePinFailsWithInvalidNewPin() {
        PinLogin login = new PinLogin(1234);
        try {
            login.changePin(1234, 99);
            fail("Expected IllegalArgumentException for invalid new PIN");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testIsValidPinBoundary() {
        assertTrue(PinLogin.isValidPin(1000));
        assertTrue(PinLogin.isValidPin(9999));
        assertFalse(PinLogin.isValidPin(999));
        assertFalse(PinLogin.isValidPin(10000));
    }

}
