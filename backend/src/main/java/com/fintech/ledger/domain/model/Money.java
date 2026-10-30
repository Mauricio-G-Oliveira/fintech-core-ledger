package com.fintech.ledger.domain.model;

import com.fintech.ledger.domain.exception.InvalidCurrencyException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Immutable Value Object representing monetary amounts with Banker's Rounding (HALF_EVEN)
 * and strict ISO-4217 currency invariant enforcement.
 * 
 * Standard internal ledger scale is 4 decimal places (0.0001) to support fractional fee accruals
 * while maintaining sub-cent reconciliation precision.
 */
public record Money(BigDecimal amount, Currency currency) implements Comparable<Money> {

    public static final int ACCOUNTING_SCALE = 4;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;
    public static final Currency BRL = Currency.getInstance("BRL");

    public Money {
        Objects.requireNonNull(amount, "Amount must not be null");
        Objects.requireNonNull(currency, "Currency must not be null");
        amount = amount.setScale(ACCOUNTING_SCALE, ROUNDING_MODE);
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money of(double amount, Currency currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
    }

    public static Money of(String amount, Currency currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    public static Money brl(BigDecimal amount) {
        return new Money(amount, BRL);
    }

    public static Money brl(double amount) {
        return of(amount, BRL);
    }

    public static Money brl(String amount) {
        return of(amount, BRL);
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public static Money zeroBrl() {
        return zero(BRL);
    }

    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public Money multiply(BigDecimal multiplier) {
        Objects.requireNonNull(multiplier, "Multiplier must not be null");
        return new Money(this.amount.multiply(multiplier), this.currency);
    }

    public Money negate() {
        return new Money(this.amount.negate(), this.currency);
    }

    public Money abs() {
        return new Money(this.amount.abs(), this.currency);
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegative() {
        return this.amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isGreaterThan(Money other) {
        assertSameCurrency(other);
        return this.compareTo(other) > 0;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        assertSameCurrency(other);
        return this.compareTo(other) >= 0;
    }

    public boolean isLessThan(Money other) {
        assertSameCurrency(other);
        return this.compareTo(other) < 0;
    }

    public boolean isLessThanOrEqual(Money other) {
        assertSameCurrency(other);
        return this.compareTo(other) <= 0;
    }

    private void assertSameCurrency(Money other) {
        Objects.requireNonNull(other, "Money operand must not be null");
        if (!this.currency.equals(other.currency)) {
            throw new InvalidCurrencyException(
                "Currency mismatch: Cannot operate between " + this.currency.getCurrencyCode() + 
                " and " + other.currency.getCurrencyCode()
            );
        }
    }

    @Override
    public int compareTo(Money other) {
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount);
    }

    @Override
    public String toString() {
        return currency.getCurrencyCode() + " " + amount.setScale(2, ROUNDING_MODE);
    }
}
