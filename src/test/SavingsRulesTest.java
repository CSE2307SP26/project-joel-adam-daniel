package test;

import bank.Account;
import bank.AccountType;
import bank.Bank;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SavingsRulesTest {

    @Test
    void checkingAllowsManyWithdrawals() {
        Account c = new Account("C", 1_000, AccountType.CHECKING);
        for (int i = 0; i < 10; i++) {
            c.withdraw(1);
        }
        assertEquals(990, c.getBalance(), 0.01);
    }

    @Test
    void savingsBlocksSeventhWithdrawalInSameMonth() {
        Account s = new Account("S", 1_000, AccountType.SAVINGS);
        for (int i = 0; i < Account.SAVINGS_MAX_WITHDRAWALS_PER_MONTH; i++) {
            s.withdraw(1);
        }
        assertThrows(IllegalStateException.class, () -> s.withdraw(1));
    }

    @Test
    void savingsTransferOutCountsTowardMonthlyLimit() {
        Account s = new Account("S", 500, AccountType.SAVINGS);
        Bank bank = new Bank();
        bank.addAccount(s);
        bank.addAccount(new Account("T", 0, AccountType.CHECKING));

        for (int i = 0; i < Account.SAVINGS_MAX_WITHDRAWALS_PER_MONTH; i++) {
            assertTrue(bank.transfer("S", "T", 1).isSuccess());
        }
        assertFalse(bank.transfer("S", "T", 1).isSuccess());
    }
}
