package com.fintech.ledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Fintech Core Ledger & Instant Payment Engine.
 * 
 * Core Architectural Highlights:
 * - Double-Entry Bookkeeping (Debit Leg == Credit Leg invariant)
 * - Deadlock-free Pessimistic Locking with deterministic Account ID ordering
 * - Strict Idempotency Filtering (X-Idempotency-Key)
 * - Transactional Outbox Pattern with HMAC-SHA256 Webhook Signatures
 * - Real-Time Observability and Prometheus Metrics
 */
@SpringBootApplication
@EnableTransactionManagement
@EnableScheduling
public class FintechCoreLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FintechCoreLedgerApplication.class, args);
    }
}
