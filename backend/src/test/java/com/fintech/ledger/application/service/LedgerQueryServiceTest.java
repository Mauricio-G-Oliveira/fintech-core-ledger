package com.fintech.ledger.application.service;

import com.fintech.ledger.application.dto.TransferResult;
import com.fintech.ledger.application.exception.TransactionNotFoundException;
import com.fintech.ledger.domain.model.EntryDirection;
import com.fintech.ledger.domain.model.LedgerEntry;
import com.fintech.ledger.domain.model.LedgerTransaction;
import com.fintech.ledger.domain.model.Money;
import com.fintech.ledger.domain.model.TransactionType;
import com.fintech.ledger.domain.repository.LedgerTransactionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerQueryServiceTest {

    @Mock
    private LedgerTransactionRepositoryPort repositoryPort;

    private LedgerQueryService service;

    @BeforeEach
    void setUp() {
        service = new LedgerQueryService(repositoryPort);
    }

    @Test
    @DisplayName("Should return recent transactions")
    void shouldReturnRecentTransactions() {
        UUID txId = UUID.randomUUID();
        UUID acc1 = UUID.randomUUID();
        UUID acc2 = UUID.randomUUID();

        LedgerEntry entry1 = LedgerEntry.of(UUID.randomUUID(), txId, acc1, "2.1.02.001", "Sender",
                EntryDirection.DEBIT, Money.brl("10.0000"), Money.brl("90.0000"), 1);
        LedgerEntry entry2 = LedgerEntry.of(UUID.randomUUID(), txId, acc2, "2.1.02.002", "Receiver",
                EntryDirection.CREDIT, Money.brl("10.0000"), Money.brl("110.0000"), 2);

        LedgerTransaction tx = LedgerTransaction.createCommitted(
                txId, "key-1", TransactionType.INSTANT_PAYMENT,
                Money.brl("10.0000"), "Transfer", List.of(entry1, entry2), 4
        );

        when(repositoryPort.findRecent(10)).thenReturn(List.of(tx));

        List<TransferResult> results = service.getRecentTransactions(10);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).transactionId()).isEqualTo(txId);
        assertThat(results.get(0).entries()).hasSize(2);
    }

    @Test
    @DisplayName("Should throw TransactionNotFoundException when transaction missing")
    void shouldThrowWhenTransactionMissing() {
        UUID txId = UUID.randomUUID();
        when(repositoryPort.findById(txId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTransactionById(txId))
                .isInstanceOf(TransactionNotFoundException.class)
                .hasMessageContaining(txId.toString());
    }
}
