package com.fintech.ledger.application.port;

import com.fintech.ledger.domain.model.TransactionStatus;

/**
 * Output Port for recording operational metrics and settlement latencies.
 * Follows the Dependency Inversion Principle (Clean Architecture).
 */
public interface LedgerMetricsPort {

    void recordTransfer(TransactionStatus status, boolean isReplay, long latencyMs);

    void recordFailure(String reason, long latencyMs);
}
