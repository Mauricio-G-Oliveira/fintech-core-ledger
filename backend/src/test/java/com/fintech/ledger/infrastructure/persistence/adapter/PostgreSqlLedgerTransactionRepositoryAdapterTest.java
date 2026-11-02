package com.fintech.ledger.infrastructure.persistence.adapter;

import com.fintech.ledger.domain.model.EntryDirection;
import com.fintech.ledger.domain.model.LedgerEntry;
import com.fintech.ledger.domain.model.LedgerTransaction;
import com.fintech.ledger.domain.model.Money;
import com.fintech.ledger.domain.model.TransactionType;
import com.fintech.ledger.infrastructure.persistence.entity.AccountEntity;
import com.fintech.ledger.infrastructure.persistence.entity.TransactionEntity;
import com.fintech.ledger.infrastructure.persistence.mapper.TransactionEntityMapper;
import com.fintech.ledger.infrastructure.persistence.repository.SpringDataAccountRepository;
import com.fintech.ledger.infrastructure.persistence.repository.SpringDataTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostgreSqlLedgerTransactionRepositoryAdapterTest {

    @Mock
    private SpringDataTransactionRepository transactionRepository;

    @Mock
    private SpringDataAccountRepository accountRepository;

    private TransactionEntityMapper mapper;
    private PostgreSqlLedgerTransactionRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        mapper = new TransactionEntityMapper();
        adapter = new PostgreSqlLedgerTransactionRepositoryAdapter(transactionRepository, accountRepository, mapper);
    }

    @Test
    @DisplayName("Should find transaction by ID with entries")
    void shouldFindById() {
        UUID txId = UUID.randomUUID();
        TransactionEntity entity = new TransactionEntity();
        entity.setId(txId);
        entity.setIdempotencyKey("idem-test");
        entity.setType(TransactionType.INSTANT_PAYMENT);
        entity.setAmount(new java.math.BigDecimal("100.0000"));
        entity.setCurrency("BRL");

        when(transactionRepository.findByIdWithEntries(txId)).thenReturn(Optional.of(entity));

        Optional<LedgerTransaction> result = adapter.findById(txId);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(txId);
        verify(transactionRepository).findByIdWithEntries(txId);
    }

    @Test
    @DisplayName("Should find transaction by idempotency key")
    void shouldFindByIdempotencyKey() {
        String key = "idem-unique-123";
        TransactionEntity entity = new TransactionEntity();
        entity.setId(UUID.randomUUID());
        entity.setIdempotencyKey(key);
        entity.setType(TransactionType.INSTANT_PAYMENT);
        entity.setAmount(new java.math.BigDecimal("250.0000"));
        entity.setCurrency("BRL");

        when(transactionRepository.findByIdempotencyKeyWithEntries(key)).thenReturn(Optional.of(entity));

        Optional<LedgerTransaction> result = adapter.findByIdempotencyKey(key);

        assertThat(result).isPresent();
        assertThat(result.get().getIdempotencyKey()).isEqualTo(key);
    }

    @Test
    @DisplayName("Should save transaction and resolve account references for journal legs")
    void shouldSaveTransaction() {
        UUID txId = UUID.randomUUID();
        UUID accId1 = UUID.randomUUID();
        UUID accId2 = UUID.randomUUID();

        LedgerEntry entry1 = LedgerEntry.of(UUID.randomUUID(), txId, accId1, "2.1.02.001", "Sender",
                EntryDirection.DEBIT, Money.brl("10.0000"), Money.brl("90.0000"), 1);
        LedgerEntry entry2 = LedgerEntry.of(UUID.randomUUID(), txId, accId2, "2.1.02.002", "Receiver",
                EntryDirection.CREDIT, Money.brl("10.0000"), Money.brl("110.0000"), 2);

        LedgerTransaction domain = LedgerTransaction.createCommitted(
                txId, "key-save-1", TransactionType.INSTANT_PAYMENT,
                Money.brl("10.0000"), "Transfer", List.of(entry1, entry2), 5
        );

        AccountEntity ref1 = new AccountEntity();
        ref1.setId(accId1);
        AccountEntity ref2 = new AccountEntity();
        ref2.setId(accId2);

        when(accountRepository.getReferenceById(accId1)).thenReturn(ref1);
        when(accountRepository.getReferenceById(accId2)).thenReturn(ref2);
        when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        LedgerTransaction saved = adapter.save(domain);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(txId);
        verify(transactionRepository).save(any(TransactionEntity.class));
    }

    @Test
    @DisplayName("Should find recent transactions with pagination limit")
    void shouldFindRecent() {
        when(transactionRepository.findRecentWithEntries(PageRequest.of(0, 10)))
                .thenReturn(List.of());

        List<LedgerTransaction> result = adapter.findRecent(10);

        assertThat(result).isEmpty();
        verify(transactionRepository).findRecentWithEntries(PageRequest.of(0, 10));
    }
}
