package com.fintech.ledger.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable domain record representing an idempotency tracking record.
 * Maps to ledger.idempotency_records to prevent duplicate mutations from network retries.
 */
public record IdempotencyRecord(
    String idempotencyKey,
    UUID transactionId,
    String requestHash,
    String responseBody,
    int statusCode,
    Instant createdAt,
    Instant expiresAt
) {
    public IdempotencyRecord {
        Objects.requireNonNull(idempotencyKey, "Idempotency key must not be null");
        Objects.requireNonNull(requestHash, "Request hash must not be null");
        createdAt = createdAt != null ? createdAt : Instant.now();
        expiresAt = expiresAt != null ? expiresAt : createdAt.plusSeconds(86400); // 24 hours TTL default
    }

    public static IdempotencyRecord of(String key, UUID transactionId, String requestHash, 
                                       String responseBody, int statusCode, Instant expiresAt) {
        return new IdempotencyRecord(
            key,
            transactionId,
            requestHash,
            responseBody,
            statusCode,
            Instant.now(),
            expiresAt
        );
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
