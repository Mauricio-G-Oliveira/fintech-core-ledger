package com.fintech.ledger.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable domain model representing an individual leg of a Double-Entry Bookkeeping transaction.
 * Once written to the ledger, an entry is strictly append-only and cannot be mutated or deleted.
 */
public record LedgerEntry(
    UUID id,
    UUID transactionId,
    UUID accountId,
    String accountCode,
    String accountName,
    EntryDirection direction,
    Money amount,
    Money runningBalance,
    int sequence,
    Instant createdAt
) {
    public LedgerEntry {
        Objects.requireNonNull(id, "Entry ID must not be null");
        Objects.requireNonNull(transactionId, "Transaction ID must not be null");
        Objects.requireNonNull(accountId, "Account ID must not be null");
        Objects.requireNonNull(accountCode, "Account Code must not be null");
        Objects.requireNonNull(direction, "Direction must not be null");
        Objects.requireNonNull(amount, "Amount must not be null");
        if (amount.isZero() || amount.isNegative()) {
            throw new IllegalArgumentException("Entry amount must be strictly positive");
        }
        if (sequence < 1) {
            throw new IllegalArgumentException("Sequence must be a positive integer (>= 1)");
        }
        createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static LedgerEntry of(UUID id, UUID transactionId, UUID accountId, String accountCode,
                                 String accountName, EntryDirection direction, Money amount,
                                 Money runningBalance, int sequence) {
        return new LedgerEntry(
            id,
            transactionId,
            accountId,
            accountCode,
            accountName,
            direction,
            amount,
            runningBalance,
            sequence,
            Instant.now()
        );
    }
}
