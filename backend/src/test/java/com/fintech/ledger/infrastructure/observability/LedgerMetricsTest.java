package com.fintech.ledger.infrastructure.observability;

import com.fintech.ledger.domain.model.TransactionStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class LedgerMetricsTest {

    private MeterRegistry meterRegistry;
    private LedgerMetrics ledgerMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        ledgerMetrics = new LedgerMetrics(meterRegistry);
    }

    @Test
    @DisplayName("Should increment counter and record latency timer for committed transfers")
    void shouldRecordCommittedTransfer() {
        ledgerMetrics.recordTransfer(TransactionStatus.COMMITTED, false, 8);

        Counter counter = meterRegistry.find("fintech.ledger.transfers.total")
                .tag("status", "COMMITTED")
                .tag("replay", "false")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);

        Timer timer = meterRegistry.find("fintech.ledger.settlement.latency").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(8.0);
    }

    @Test
    @DisplayName("Should tag idempotent replays accurately in Prometheus counter")
    void shouldRecordIdempotentReplay() {
        ledgerMetrics.recordTransfer(TransactionStatus.COMMITTED, true, 2);

        Counter replayCounter = meterRegistry.find("fintech.ledger.transfers.total")
                .tag("status", "COMMITTED")
                .tag("replay", "true")
                .counter();

        assertThat(replayCounter).isNotNull();
        assertThat(replayCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should record rejected transfers with failure reason")
    void shouldRecordFailure() {
        ledgerMetrics.recordFailure("INSUFFICIENT_FUNDS", 4);

        Counter failureCounter = meterRegistry.find("fintech.ledger.transfers.total")
                .tag("status", "REJECTED")
                .tag("reason", "INSUFFICIENT_FUNDS")
                .counter();

        assertThat(failureCounter).isNotNull();
        assertThat(failureCounter.count()).isEqualTo(1.0);
    }
}
