package com.fintech.ledger.domain;

import com.fintech.ledger.domain.exception.InvalidCurrencyException;
import com.fintech.ledger.domain.model.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit Tests: Money Value Object")
class MoneyTest {

    @Test
    @DisplayName("Should scale amount to 4 accounting decimal places using Banker's Rounding")
    void shouldScaleToAccountingScale() {
        Money money = Money.brl(150.55555);
        assertEquals(new BigDecimal("150.5556"), money.amount());
        assertEquals(4, money.amount().scale());
    }

    @Test
    @DisplayName("Should correctly add and subtract monetary amounts of same currency")
    void shouldAddAndSubtract() {
        Money m1 = Money.brl(100.00);
        Money m2 = Money.brl(50.25);

        Money sum = m1.add(m2);
        assertEquals(Money.brl(150.25), sum);

        Money diff = m1.subtract(m2);
        assertEquals(Money.brl(49.75), diff);
    }

    @Test
    @DisplayName("Should throw InvalidCurrencyException when operating across different currencies")
    void shouldThrowOnCurrencyMismatch() {
        Money brl = Money.brl(100.00);
        Money usd = Money.of(100.00, Currency.getInstance("USD"));

        assertThrows(InvalidCurrencyException.class, () -> brl.add(usd));
        assertThrows(InvalidCurrencyException.class, () -> brl.subtract(usd));
        assertThrows(InvalidCurrencyException.class, () -> brl.isGreaterThan(usd));
    }

    @Test
    @DisplayName("Should correctly evaluate relational predicates")
    void shouldEvaluateRelationalPredicates() {
        Money hundred = Money.brl(100.00);
        Money fifty = Money.brl(50.00);
        Money zero = Money.zeroBrl();

        assertTrue(hundred.isGreaterThan(fifty));
        assertTrue(fifty.isLessThan(hundred));
        assertTrue(hundred.isGreaterThanOrEqual(hundred));
        assertTrue(zero.isZero());
        assertTrue(hundred.isPositive());
    }
}
