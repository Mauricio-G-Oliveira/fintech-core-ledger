package com.fintech.ledger.infrastructure.observability;

import com.fintech.ledger.application.port.LedgerMetricsPort;
import com.fintech.ledger.domain.model.TransactionStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Micrometer & Prometheus Secondary Adapter for LedgerMetricsPort.
 * Publishes operational metrics, settlement latencies, and percentiles to Prometheus.
 */
@Component
public class LedgerMetrics implements LedgerMetricsPort {

    private final MeterRegistry meterRegistry;
    private final Timer settlementTimer;

    public LedgerMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
        this.settlementTimer = Timer.builder("fintech.ledger.settlement.latency")
                .description("Settlement latency distribution of instant payments in milliseconds")
                .publishPercentiles(0.5, 0.95, 0.99)
                .minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofSeconds(10))
                .register(meterRegistry);
    }

    @Override
    public void recordTransfer(TransactionStatus status, boolean isReplay, long latencyMs) {
        Counter.builder("fintech.ledger.transfers.total")
                .description("Total number of processed instant transfer settlements")
                .tag("status", status.name())
                .tag("replay", String.valueOf(isReplay))
                .register(meterRegistry)
                .increment();

        settlementTimer.record(Math.max(1, latencyMs), TimeUnit.MILLISECONDS);
    }

    @Override
    public void recordFailure(String reason, long latencyMs) {
        Counter.builder("fintech.ledger.transfers.total")
                .description("Total number of processed instant transfer settlements")
                .tag("status", "REJECTED")
                .tag("reason", reason != null ? reason : "UNKNOWN")
                .tag("replay", "false")
                .register(meterRegistry)
                .increment();

        settlementTimer.record(Math.max(1, latencyMs), TimeUnit.MILLISECONDS);
    }

    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }
}
