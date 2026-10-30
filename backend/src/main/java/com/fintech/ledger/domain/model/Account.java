package com.fintech.ledger.domain.model;

import com.fintech.ledger.domain.exception.InactiveAccountException;
import com.fintech.ledger.domain.exception.InsufficientFundsException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain Entity representing a ledger account in the Chart of Accounts.
 * Encapsulates accounting balance mutation rules and active state verification.
 */
public class Account {

    private final UUID id;
    private final String code;
    private final String name;
    private final AccountType type;
    private Money balance;
    private AccountStatus status;
    private Long version;
    private final Instant createdAt;
    private Instant updatedAt;

    public Account(UUID id, String code, String name, AccountType type, Money balance, 
                   AccountStatus status, Long version, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "Account ID must not be null");
        this.code = Objects.requireNonNull(code, "Account code must not be null");
        this.name = Objects.requireNonNull(name, "Account name must not be null");
        this.type = Objects.requireNonNull(type, "Account type must not be null");
        this.balance = Objects.requireNonNull(balance, "Account balance must not be null");
        this.status = Objects.requireNonNull(status, "Account status must not be null");
        this.version = version != null ? version : 0L;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public static Account create(String code, String name, AccountType type, Money initialBalance) {
        return new Account(
            UUID.randomUUID(),
            code,
            name,
            type,
            initialBalance,
            AccountStatus.ACTIVE,
            0L,
            Instant.now(),
            Instant.now()
        );
    }

    /**
     * Applies a mutation according to double-entry accounting rules:
     * - Debit Normal (Assets/Expenses): DEBIT increases balance, CREDIT decreases.
     * - Credit Normal (Liabilities/Equity/Revenue): CREDIT increases balance, DEBIT decreases.
     */
    public Money applyEntry(EntryDirection direction, Money amount) {
        assertActive();
        Objects.requireNonNull(direction, "Direction must not be null");
        Objects.requireNonNull(amount, "Amount must not be null");

        if (type.isDebitNormal()) {
            if (direction.isDebit()) {
                this.balance = this.balance.add(amount);
            } else {
                this.balance = this.balance.subtract(amount);
            }
        } else {
            // Credit normal (e.g. Liability / Customer Wallets)
            if (direction.isCredit()) {
                this.balance = this.balance.add(amount);
            } else {
                if (type == AccountType.LIABILITY && this.balance.isLessThan(amount)) {
                    throw new InsufficientFundsException(
                        "Account " + code + " has insufficient funds. Available: " + 
                        this.balance + ", Required: " + amount
                    );
                }
                this.balance = this.balance.subtract(amount);
            }
        }

        this.updatedAt = Instant.now();
        this.version = (this.version != null ? this.version : 0L) + 1;
        return this.balance;
    }

    public void assertActive() {
        if (!status.isActive()) {
            throw new InactiveAccountException("Account " + code + " is currently " + status);
        }
    }

    public boolean canWithdraw(Money amount) {
        if (!status.isActive()) return false;
        if (type == AccountType.LIABILITY) {
            return this.balance.isGreaterThanOrEqual(amount);
        }
        return true;
    }

    public void freeze() {
        this.status = AccountStatus.FROZEN;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.status = AccountStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    // Getters
    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public AccountType getType() { return type; }
    public Money getBalance() { return balance; }
    public AccountStatus getStatus() { return status; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
