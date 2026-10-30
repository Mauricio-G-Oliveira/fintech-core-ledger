package com.fintech.ledger.domain;

import com.fintech.ledger.domain.exception.InactiveAccountException;
import com.fintech.ledger.domain.exception.InsufficientFundsException;
import com.fintech.ledger.domain.model.Account;
import com.fintech.ledger.domain.model.AccountType;
import com.fintech.ledger.domain.model.EntryDirection;
import com.fintech.ledger.domain.model.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit Tests: Account Entity & Accounting Normal Balances")
class AccountTest {

    @Test
    @DisplayName("Asset account (Debit Normal): Debit should increase balance, Credit should decrease")
    void shouldHandleAssetNormalBalance() {
        Account reserve = Account.create("1.1.01.001", "Central Bank Reserve", AccountType.ASSET, Money.brl(1000.00));

        // Debit increases Asset
        reserve.applyEntry(EntryDirection.DEBIT, Money.brl(500.00));
        assertEquals(Money.brl(1500.00), reserve.getBalance());

        // Credit decreases Asset
        reserve.applyEntry(EntryDirection.CREDIT, Money.brl(200.00));
        assertEquals(Money.brl(1300.00), reserve.getBalance());
        assertEquals(2L, reserve.getVersion());
    }

    @Test
    @DisplayName("Liability account (Credit Normal): Credit should increase balance, Debit should decrease")
    void shouldHandleLiabilityNormalBalance() {
        Account wallet = Account.create("2.1.01.001", "Customer Wallet", AccountType.LIABILITY, Money.brl(500.00));

        // Credit increases Liability (customer deposited)
        wallet.applyEntry(EntryDirection.CREDIT, Money.brl(300.00));
        assertEquals(Money.brl(800.00), wallet.getBalance());

        // Debit decreases Liability (customer spent)
        wallet.applyEntry(EntryDirection.DEBIT, Money.brl(200.00));
        assertEquals(Money.brl(600.00), wallet.getBalance());
    }

    @Test
    @DisplayName("Should throw InsufficientFundsException when debiting more than available on Liability account")
    void shouldThrowOnOverdraft() {
        Account wallet = Account.create("2.1.01.001", "Customer Wallet", AccountType.LIABILITY, Money.brl(100.00));

        assertThrows(InsufficientFundsException.class, () ->
            wallet.applyEntry(EntryDirection.DEBIT, Money.brl(150.00))
        );
        // Balance remains unchanged
        assertEquals(Money.brl(100.00), wallet.getBalance());
    }

    @Test
    @DisplayName("Should throw InactiveAccountException when transacting on a frozen account")
    void shouldThrowWhenAccountIsFrozen() {
        Account wallet = Account.create("2.1.01.001", "Customer Wallet", AccountType.LIABILITY, Money.brl(500.00));
        wallet.freeze();

        assertThrows(InactiveAccountException.class, () ->
            wallet.applyEntry(EntryDirection.DEBIT, Money.brl(50.00))
        );
    }
}
