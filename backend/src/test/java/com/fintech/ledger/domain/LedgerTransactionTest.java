package com.fintech.ledger.domain;

import com.fintech.ledger.domain.exception.UnbalancedTransactionException;
import com.fintech.ledger.domain.model.EntryDirection;
import com.fintech.ledger.domain.model.LedgerEntry;
import com.fintech.ledger.domain.model.LedgerTransaction;
import com.fintech.ledger.domain.model.Money;
import com.fintech.ledger.domain.model.TransactionStatus;
import com.fintech.ledger.domain.model.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit Tests: LedgerTransaction & Double-Entry Invariant Enforcement")
class LedgerTransactionTest {

    @Test
    @DisplayName("Should successfully commit a perfectly balanced transaction and calculate SHA-256 hash")
    void shouldCommitBalancedTransaction() {
        UUID txId = UUID.randomUUID();
        UUID accSource = UUID.randomUUID();
        UUID accDest = UUID.randomUUID();
        Money amount = Money.brl(250.00);

        List<LedgerEntry> entries = List.of(
            LedgerEntry.of(UUID.randomUUID(), txId, accSource, "2.1.01.001", "Source Acc", EntryDirection.DEBIT, amount, Money.brl(750.00), 1),
            LedgerEntry.of(UUID.randomUUID(), txId, accDest, "2.1.02.001", "Dest Acc", EntryDirection.CREDIT, amount, Money.brl(1250.00), 2)
        );

        LedgerTransaction tx = LedgerTransaction.createCommitted(
            txId, "idem-test-001", TransactionType.INSTANT_PAYMENT, amount, "Test Settlement", entries, 15
        );

        assertNotNull(tx);
        assertEquals(TransactionStatus.COMMITTED, tx.getStatus());
        assertEquals(amount, tx.getAmount());
        assertNotNull(tx.getHash());
        assertTrue(tx.getHash().startsWith("0x"));
        assertEquals(66, tx.getHash().length()); // "0x" + 64 hex characters
    }

    @Test
    @DisplayName("Should throw UnbalancedTransactionException when Debits != Credits (Zero-leakage proof)")
    void shouldThrowWhenDebitsDoNotMatchCredits() {
        UUID txId = UUID.randomUUID();
        UUID accSource = UUID.randomUUID();
        UUID accDest = UUID.randomUUID();

        // Deliberate 1-cent discrepancy (unbalanced ledger)
        List<LedgerEntry> unbalancedEntries = List.of(
            LedgerEntry.of(UUID.randomUUID(), txId, accSource, "2.1.01.001", "Source", EntryDirection.DEBIT, Money.brl(100.00), Money.brl(900.00), 1),
            LedgerEntry.of(UUID.randomUUID(), txId, accDest, "2.1.02.001", "Dest", EntryDirection.CREDIT, Money.brl(99.99), Money.brl(1099.99), 2)
        );

        UnbalancedTransactionException ex = assertThrows(
            UnbalancedTransactionException.class,
            () -> LedgerTransaction.createCommitted(
                txId, "idem-test-002", TransactionType.INSTANT_PAYMENT, Money.brl(100.00), "Unbalanced TX", unbalancedEntries, 10
            )
        );

        assertTrue(ex.getMessage().contains("Double-entry bookkeeping invariant violated"));
    }

    @Test
    @DisplayName("Should throw UnbalancedTransactionException when entries list contains fewer than 2 legs")
    void shouldThrowWhenLessThanTwoLegs() {
        UUID txId = UUID.randomUUID();
        UUID accSource = UUID.randomUUID();

        List<LedgerEntry> singleLeg = List.of(
            LedgerEntry.of(UUID.randomUUID(), txId, accSource, "2.1.01.001", "Source", EntryDirection.DEBIT, Money.brl(100.00), Money.brl(900.00), 1)
        );

        assertThrows(
            UnbalancedTransactionException.class,
            () -> LedgerTransaction.createCommitted(
                txId, "idem-test-003", TransactionType.INSTANT_PAYMENT, Money.brl(100.00), "Single leg TX", singleLeg, 10
            )
        );
    }
}
