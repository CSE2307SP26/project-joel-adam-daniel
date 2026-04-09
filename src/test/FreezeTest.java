package test;

import bank.Account;
import bank.Bank;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FreezeTest {

    @Test
    void frozenBlocksDepositWithdrawAndTransfer() {
        Bank bank = new Bank();
        bank.addAccount(new Account("F1", 100));
        bank.setAccountFrozen("F1", true);
        Account a = bank.getAccount("F1");
        assertThrows(IllegalStateException.class, () -> a.deposit(1));
        assertThrows(IllegalStateException.class, () -> a.withdraw(1));
        assertThrows(IllegalStateException.class, () -> a.transferIn(1));
        assertThrows(IllegalStateException.class, () -> a.transferOut(1));
    }

    @Test
    void operatorFreezeUnfreeze() {
        Bank bank = new Bank();
        bank.addAccount(new Account("X", 0));
        assertTrue(bank.setAccountFrozen("X", true).isSuccess());
        assertTrue(bank.getAccount("X").isFrozen());
        assertTrue(bank.setAccountFrozen("X", false).isSuccess());
        assertFalse(bank.getAccount("X").isFrozen());
    }

    @Test
    void transferBlockedWhenEitherSideFrozen() {
        Bank bank = new Bank();
        bank.addAccount(new Account("A", 50));
        bank.addAccount(new Account("B", 0));
        bank.setAccountFrozen("A", true);
        assertFalse(bank.transfer("A", "B", 10).isSuccess());
        bank.setAccountFrozen("A", false);
        bank.setAccountFrozen("B", true);
        assertFalse(bank.transfer("A", "B", 10).isSuccess());
    }
}
