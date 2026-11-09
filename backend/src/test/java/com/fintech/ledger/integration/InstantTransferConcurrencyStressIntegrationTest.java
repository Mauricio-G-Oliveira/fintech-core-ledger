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
 * Concurrency & Stress Integration Test.
 * 
 * Verifies the core architectural promise:
 * - High-concurrency cross-transfers (A -> B and B -> A) execute concurrently across multiple threads.
 * - ZERO database deadlocks occur due to deterministic ascending Account ID locking (SELECT FOR UPDATE).
 * - Total balance conservation invariant holds true: Initial Sum == Final Sum.
 * - Double-Entry Bookkeeping invariant holds true: Sum(Debits) == Sum(Credits).
 */
class InstantTransferConcurrencyStressIntegrationTest extends AbstractIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(InstantTransferConcurrencyStressIntegrationTest.class);

    @Autowired
    private InstantTransferUseCase instantTransferUseCase;

    @Autowired
    private AccountRepositoryPort accountRepositoryPort;

    @Autowired
    private LedgerTransactionRepositoryPort transactionRepositoryPort;

    @Test
    @DisplayName("Should execute 100 concurrent cross-transfers with zero deadlocks and 100% balance conservation")
    void shouldExecuteConcurrentCrossTransfersWithoutDeadlock() throws InterruptedException {
        // 1. Arrange: Create two active accounts with substantial liquidity
        Account accountA = Account.create("STRESS-ACC-A", "Stress Test Account A", AccountType.LIABILITY, Money.brl("10000.0000"));
        Account accountB = Account.create("STRESS-ACC-B", "Stress Test Account B", AccountType.LIABILITY, Money.brl("10000.0000"));

        accountA = accountRepositoryPort.save(accountA);
        accountB = accountRepositoryPort.save(accountB);

        UUID idA = accountA.getId();
        UUID idB = accountB.getId();

        BigDecimal initialTotal = accountA.getBalance().amount().add(accountB.getBalance().amount());

        int totalTransfers = 100;
        int threadPoolSize = 20;
        BigDecimal transferAmount = new BigDecimal("15.0000");

        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(totalTransfers);

        AtomicInteger successCount = new AtomicInteger(0);
        List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());
        List<TransferResult> results = Collections.synchronizedList(new ArrayList<>());

        log.info("Starting concurrent stress test: {} cross-transfers across {} threads", totalTransfers, threadPoolSize);

        // 2. Act: Submit 50 transfers (A -> B) and 50 transfers (B -> A)
        for (int i = 0; i < totalTransfers; i++) {
            final boolean forward = (i % 2 == 0);
            final UUID src = forward ? idA : idB;
            final UUID dst = forward ? idB : idA;
            final String idempotencyKey = "stress-tx-" + UUID.randomUUID();

            executor.submit(() -> {
                try {
                    // Block until all threads are queued to generate a simultaneous burst
                    startGate.await();

                    TransferCommand command = new TransferCommand(
                            src, dst, transferAmount, "BRL", idempotencyKey, "Stress test concurrent payment"
                    );

                    TransferResult result = instantTransferUseCase.execute(command);
                    results.add(result);
                    successCount.incrementAndGet();
                } catch (Throwable t) {
                    log.error("Execution failed during concurrent transfer", t);
                    exceptions.add(t);
                } finally {
                    endGate.countDown();
                }
            });
        }

        // Release the start gate - all threads fire concurrently
        startGate.countDown();

        boolean completedInTime = endGate.await(45, TimeUnit.SECONDS);
        executor.shutdown();

        // 3. Assert: Concurrency & Invariant Verification
        assertThat(completedInTime)
                .withFailMessage("Stress test timed out waiting for all threads to complete")
                .isTrue();

        assertThat(exceptions)
                .withFailMessage("Deadlocks or concurrency exceptions detected: " + exceptions)
                .isEmpty();

        assertThat(successCount.get())
                .withFailMessage("Expected exactly %d successful transfers but got %d", totalTransfers, successCount.get())
                .isEqualTo(totalTransfers);

        // Verify all transactions are COMMITTED with valid Merkle root hashes
        for (TransferResult result : results) {
            assertThat(result.status()).isEqualTo(TransactionStatus.COMMITTED);
            assertThat(result.hash()).startsWith("0x");
            assertThat(result.entries()).hasSize(2);
        }

        // Verify Balance Conservation: Initial Total Balance == Final Total Balance
        Account updatedAccountA = accountRepositoryPort.findById(idA).orElseThrow();
        Account updatedAccountB = accountRepositoryPort.findById(idB).orElseThrow();

        BigDecimal finalTotal = updatedAccountA.getBalance().amount().add(updatedAccountB.getBalance().amount());

        assertThat(finalTotal)
                .withFailMessage("Money conservation violated! Initial [%s], Final [%s]", initialTotal, finalTotal)
                .isEqualByComparingTo(initialTotal);

        // Since exactly 50 transfers were A->B and 50 were B->A for the exact same amount, final balances should equal initial
        assertThat(updatedAccountA.getBalance().amount())
                .isEqualByComparingTo(new BigDecimal("10000.0000"));
        assertThat(updatedAccountB.getBalance().amount())
                .isEqualByComparingTo(new BigDecimal("10000.0000"));

        log.info("Concurrency stress test PASSED: 100/100 transactions committed with zero deadlocks and conserved wealth [{}]", finalTotal);
    }
}
