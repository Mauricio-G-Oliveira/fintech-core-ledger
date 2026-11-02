package com.fintech.ledger.infrastructure.persistence.entity;

import com.fintech.ledger.domain.model.TransactionStatus;
import com.fintech.ledger.domain.model.TransactionType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA Entity mapping to table `ledger.transactions`.
 * Stores transaction headers and cascades persisted journal entry legs.
 */
@Entity
@Table(name = "transactions", schema = "ledger")
public class TransactionEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 120, updatable = false)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40, updatable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30, updatable = false)
    private TransactionStatus status;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(name = "description", columnDefinition = "TEXT", updatable = false)
    private String description;

    @Column(name = "settlement_latency_ms", nullable = false, updatable = false)
    private Integer settlementLatencyMs;

    @Column(name = "hash", nullable = false, length = 66, updatable = false)
    private String hash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sequence ASC")
    private List<LedgerEntryEntity> entries = new ArrayList<>();

    public TransactionEntity() {
    }

    public TransactionEntity(UUID id, String idempotencyKey, TransactionType type, TransactionStatus status,
                             BigDecimal amount, String currency, String description,
                             Integer settlementLatencyMs, String hash, Instant createdAt) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.type = type;
        this.status = status;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.settlementLatencyMs = settlementLatencyMs != null ? settlementLatencyMs : 0;
        this.hash = hash;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public void addEntry(LedgerEntryEntity entry) {
        if (entry != null) {
            entries.add(entry);
            entry.setTransaction(this);
        }
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getSettlementLatencyMs() { return settlementLatencyMs; }
    public void setSettlementLatencyMs(Integer settlementLatencyMs) { this.settlementLatencyMs = settlementLatencyMs; }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public List<LedgerEntryEntity> getEntries() { return entries; }
    public void setEntries(List<LedgerEntryEntity> entries) { this.entries = entries; }
}
