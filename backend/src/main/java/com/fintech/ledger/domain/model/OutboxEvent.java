package com.fintech.ledger.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain Aggregate representing an outbox event in the Transactional Outbox Pattern.
 * Guarantees at-least-once asynchronous event delivery with retry tracking.
 */
public class OutboxEvent {

    private final UUID id;
    private final String aggregateType;
    private final UUID aggregateId;
    private final String eventType;
    private final String payload;
    private OutboxStatus status;
    private int retryCount;
    private final int maxRetries;
    private Instant nextRetryAt;
    private Instant publishedAt;
    private String lastError;
    private final Instant createdAt;

    public OutboxEvent(UUID id, String aggregateType, UUID aggregateId, String eventType, String payload,
                       OutboxStatus status, int retryCount, int maxRetries, Instant nextRetryAt,
                       Instant publishedAt, String lastError, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "ID must not be null");
        this.aggregateType = Objects.requireNonNull(aggregateType, "aggregateType must not be null");
        this.aggregateId = Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.payload = Objects.requireNonNull(payload, "payload must not be null");
        this.status = status != null ? status : OutboxStatus.PENDING;
        this.retryCount = retryCount;
        this.maxRetries = maxRetries > 0 ? maxRetries : 5;
        this.nextRetryAt = nextRetryAt;
        this.publishedAt = publishedAt;
        this.lastError = lastError;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static OutboxEvent createPending(String aggregateType, UUID aggregateId, String eventType, String payload) {
        return new OutboxEvent(
            UUID.randomUUID(),
            aggregateType,
            aggregateId,
            eventType,
            payload,
            OutboxStatus.PENDING,
            0,
            5,
            Instant.now(),
            null,
            null,
            Instant.now()
        );
    }

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = Instant.now();
        this.lastError = null;
    }

    public void recordFailure(String error) {
        this.retryCount++;
        this.lastError = error;
        if (this.retryCount >= this.maxRetries) {
            this.status = OutboxStatus.FAILED;
            this.nextRetryAt = null;
        } else {
            this.status = OutboxStatus.PENDING;
            // Exponential backoff: 2^retryCount * 5 seconds
            long delaySeconds = (long) Math.pow(2, this.retryCount) * 5L;
            this.nextRetryAt = Instant.now().plusSeconds(delaySeconds);
        }
    }

    // Getters
    public UUID getId() { return id; }
    public String getAggregateType() { return aggregateType; }
    public UUID getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public OutboxStatus getStatus() { return status; }
    public int getRetryCount() { return retryCount; }
    public int getMaxRetries() { return maxRetries; }
    public Instant getNextRetryAt() { return nextRetryAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public String getLastError() { return lastError; }
    public Instant getCreatedAt() { return createdAt; }
}
