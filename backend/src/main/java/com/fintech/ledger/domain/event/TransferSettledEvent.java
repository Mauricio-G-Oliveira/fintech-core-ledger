package com.fintech.ledger.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain Event representing a finalized, committed double-entry transfer.
 * Dispatched to external subscribers via Transactional Outbox.
 */
public record TransferSettledEvent(
    UUID eventId,
    UUID transactionId,
    String idempotencyKey,
    UUID sourceAccountId,
    UUID destinationAccountId,
    BigDecimal amount,
    String currency,
    String description,
    String hash,
    Instant occurredAt
) {
}
