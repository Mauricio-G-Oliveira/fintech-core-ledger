package com.fintech.ledger.integration;

import com.fintech.ledger.application.dto.TransferCommand;
import com.fintech.ledger.application.dto.TransferResult;
import com.fintech.ledger.application.usecase.InstantTransferUseCase;
import com.fintech.ledger.domain.model.Account;
import com.fintech.ledger.domain.model.AccountType;
import com.fintech.ledger.domain.model.Money;
import com.fintech.ledger.domain.model.TransactionStatus;
import com.fintech.ledger.domain.repository.AccountRepositoryPort;
import com.fintech.ledger.domain.repository.LedgerTransactionRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency Test for Idempotent Burst Requests.
 * 
 * Simulates a high-concurrency client network retry storm where multiple worker
 * threads submit the exact same transaction with identical Idempotency-Key simultaneously.
 * 
 * Guarantees:
 * - Exactly ONE transfer performs state mutation (201 Created).
 * - All other concurrent attempts receive the cached result with `isIdempotentReplay == true` (200 OK).
 * - Balances are debited and credited strictly ONCE.
 */
class IdempotencyBurstConcurrencyIntegrationTest extends AbstractIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyBurstConcurrencyIntegrationTest.class);

    @Autowired
    private InstantTransferUseCase instantTransferUseCase;

    @Autowired
    private AccountRepositoryPort accountRepositoryPort;

    @Autowired
    private LedgerTransactionRepositoryPort transactionRepositoryPort;

    @Test
    @DisplayName("Should process exactly once under concurrent burst storm with same Idempotency-Key")
    void shouldHandleConcurrentBurstWithExactSameIdempotencyKey() throws InterruptedException {
        // 1. Arrange: Create accounts
        Account accountA = Account.create("IDEM-BURST-A", "Sender", AccountType.LIABILITY, Money.brl("1000.0000"));
        Account accountB = Account.create("IDEM-BURST-B", "Receiver", AccountType.LIABILITY, Money.brl("500.0000"));

        accountA = accountRepositoryPort.save(accountA);
        accountB = accountRepositoryPort.save(accountB);

        UUID idA = accountA.getId();
        UUID idB = accountB.getId();
        String sharedKey = "burst-idem-key-" + UUID.randomUUID();
        BigDecimal transferAmount = new BigDecimal("250.0000");

        int burstConcurrency = 10;
        ExecutorService executor = Executors.newFixedThreadPool(burstConcurrency);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(burstConcurrency);

        List<TransferResult> results = Collections.synchronizedList(new ArrayList<>());
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger replayCount = new AtomicInteger(0);
        AtomicInteger freshCount = new AtomicInteger(0);

        // 2. Act: 10 threads submit the exact same command concurrently
        for (int i = 0; i < burstConcurrency; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();

                    TransferCommand command = new TransferCommand(
                            idA, idB, transferAmount, "BRL", sharedKey, "Burst retry storm"
                    );

                    TransferResult result = instantTransferUseCase.execute(command);
                    results.add(result);

                    if (result.isIdempotentReplay()) {
                        replayCount.incrementAndGet();
                    } else {
                        freshCount.incrementAndGet();
                    }
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    endGate.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        startGate.countDown();
        boolean completed = endGate.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // 3. Assert
        assertThat(completed).isTrue();
        assertThat(errors).isEmpty();
        assertThat(results).hasSize(burstConcurrency);

        // Exactly 1 execution mutated state, and 9 were idempotent replays
        assertThat(freshCount.get()).isEqualTo(1);
        assertThat(replayCount.get()).isEqualTo(burstConcurrency - 1);

        // All returned transaction IDs and Merkle hashes must match the single committed transaction
        UUID canonicalTxId = results.get(0).transactionId();
        String canonicalHash = results.get(0).hash();

        for (TransferResult res : results) {
            assertThat(res.transactionId()).isEqualTo(canonicalTxId);
            assertThat(res.hash()).isEqualTo(canonicalHash);
            assertThat(res.status()).isEqualTo(TransactionStatus.COMMITTED);
        }

        // Verify balances mutated exactly ONCE
        Account finalA = accountRepositoryPort.findById(idA).orElseThrow();
        Account finalB = accountRepositoryPort.findById(idB).orElseThrow();

        // Account A: 1000 - 250 = 750
        assertThat(finalA.getBalance().amount()).isEqualByComparingTo(new BigDecimal("750.0000"));
        // Account B: 500 + 250 = 750
        assertThat(finalB.getBalance().amount()).isEqualByComparingTo(new BigDecimal("750.0000"));

        log.info("Idempotency burst test PASSED: 1 fresh execution, {} replays, balances mutated strictly once", replayCount.get());
    }
}
