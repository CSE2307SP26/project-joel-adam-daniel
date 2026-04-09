package test;

import bank.PinLogin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PinLoginTest {

    @Test
    void validPinCreation() {
        PinLogin login = new PinLogin(1234);
        assertFalse(login.isLocked());
    }

    @Test
    void invalidPinTooShort() {
        assertThrows(IllegalArgumentException.class, () -> new PinLogin(999));
    }

    @Test
    void invalidPinTooLong() {
        assertThrows(IllegalArgumentException.class, () -> new PinLogin(10000));
    }

    @Test
    void invalidPinNegative() {
        assertThrows(IllegalArgumentException.class, () -> new PinLogin(-1234));
    }

    @Test
    void successfulAuthentication() {
        PinLogin login = new PinLogin(5678);
        assertTrue(login.authenticate(5678));
    }

    @Test
    void failedAuthentication() {
        PinLogin login = new PinLogin(5678);
        assertFalse(login.authenticate(1111));
    }

    @Test
    void lockoutAfterThreeFailures() {
        PinLogin login = new PinLogin(5678);
        login.authenticate(0);
        login.authenticate(1111);
        login.authenticate(2222);
        assertTrue(login.isLocked());
    }

    @Test
    void authenticationReturnsFalseWhenLocked() {
        PinLogin login = new PinLogin(5678);
        login.authenticate(1111);
        login.authenticate(1111);
        login.authenticate(1111);
        assertFalse(login.authenticate(5678));
    }

    @Test
    void remainingAttemptsDecrement() {
        PinLogin login = new PinLogin(5678);
        assertEquals(3, login.getRemainingAttempts());
        login.authenticate(1111);
        assertEquals(2, login.getRemainingAttempts());
        login.authenticate(1111);
        assertEquals(1, login.getRemainingAttempts());
    }

    @Test
    void remainingAttemptsResetOnSuccess() {
        PinLogin login = new PinLogin(5678);
        login.authenticate(1111);
        login.authenticate(5678);
        assertEquals(3, login.getRemainingAttempts());
    }

    @Test
    void changePinSuccess() {
        PinLogin login = new PinLogin(1234);
        login.changePin(1234, 5678);
        assertTrue(login.authenticate(5678));
    }

    @Test
    void changePinFailsWithWrongCurrentPin() {
        PinLogin login = new PinLogin(1234);
        assertThrows(IllegalArgumentException.class, () -> login.changePin(9999, 5678));
    }

    @Test
    void changePinFailsWithInvalidNewPin() {
        PinLogin login = new PinLogin(1234);
        assertThrows(IllegalArgumentException.class, () -> login.changePin(1234, 99));
    }

    @Test
    void isValidPinBoundary() {
        assertTrue(PinLogin.isValidPin(1000));
        assertTrue(PinLogin.isValidPin(9999));
        assertFalse(PinLogin.isValidPin(999));
        assertFalse(PinLogin.isValidPin(10000));
    }
}
