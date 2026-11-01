package com.fintech.ledger.application.dto;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

/**
 * Command DTO encapsulating the input required to execute an instant transfer settlement.
 */
public record TransferCommand(
    UUID sourceAccountId,
    UUID destinationAccountId,
    BigDecimal amount,
    String currencyCode,
    String idempotencyKey,
    String description
) {
    public TransferCommand {
        Objects.requireNonNull(sourceAccountId, "Source account ID must not be null");
        Objects.requireNonNull(destinationAccountId, "Destination account ID must not be null");
        Objects.requireNonNull(amount, "Amount must not be null");
        Objects.requireNonNull(currencyCode, "Currency code must not be null");
        Objects.requireNonNull(idempotencyKey, "Idempotency key must not be null");

        if (sourceAccountId.equals(destinationAccountId)) {
            throw new IllegalArgumentException("Source and destination accounts must be distinct");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be strictly greater than zero");
        }
        if (idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency key must not be blank");
        }
    }

    public Currency getCurrency() {
        return Currency.getInstance(currencyCode);
    }
}
