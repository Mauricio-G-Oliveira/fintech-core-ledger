package com.fintech.ledger.infrastructure.persistence.mapper;

import com.fintech.ledger.domain.model.AccountStatus;
import com.fintech.ledger.domain.model.AccountType;
import com.fintech.ledger.domain.model.EntryDirection;
import com.fintech.ledger.domain.model.LedgerEntry;
import com.fintech.ledger.domain.model.LedgerTransaction;
import com.fintech.ledger.domain.model.Money;
import com.fintech.ledger.domain.model.TransactionStatus;
import com.fintech.ledger.domain.model.TransactionType;
import com.fintech.ledger.infrastructure.persistence.entity.AccountEntity;
import com.fintech.ledger.infrastructure.persistence.entity.LedgerEntryEntity;
import com.fintech.ledger.infrastructure.persistence.entity.TransactionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionEntityMapperTest {

    private TransactionEntityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TransactionEntityMapper();
    }

    @Test
    @DisplayName("Should map TransactionEntity with entries to Domain LedgerTransaction")
    void shouldMapEntityToDomain() {
        UUID txId = UUID.randomUUID();
        UUID accountId1 = UUID.randomUUID();
        UUID accountId2 = UUID.randomUUID();
        Instant now = Instant.now();

        AccountEntity acc1 = new AccountEntity(accountId1, "2.1.02.001", "Sender", AccountType.LIABILITY,
                new BigDecimal("900.0000"), "BRL", AccountStatus.ACTIVE, 1L, now, now);
        AccountEntity acc2 = new AccountEntity(accountId2, "2.1.02.002", "Receiver", AccountType.LIABILITY,
                new BigDecimal("1100.0000"), "BRL", AccountStatus.ACTIVE, 1L, now, now);

        TransactionEntity txEntity = new TransactionEntity(
                txId,
                "tx-key-100",
                TransactionType.INSTANT_PAYMENT,
                TransactionStatus.COMMITTED,
                new BigDecimal("100.0000"),
                "BRL",
                "Instant P2P Transfer",
                12,
                "0xabc123",
                now
        );

        LedgerEntryEntity entry1 = new LedgerEntryEntity(
                UUID.randomUUID(), txEntity, acc1, EntryDirection.DEBIT,
                new BigDecimal("100.0000"), "BRL", new BigDecimal("900.0000"), 1, now
        );
        LedgerEntryEntity entry2 = new LedgerEntryEntity(
                UUID.randomUUID(), txEntity, acc2, EntryDirection.CREDIT,
                new BigDecimal("100.0000"), "BRL", new BigDecimal("1100.0000"), 2, now
        );

        txEntity.addEntry(entry1);
        txEntity.addEntry(entry2);

        LedgerTransaction domain = mapper.toDomain(txEntity);

        assertThat(domain).isNotNull();
        assertThat(domain.getId()).isEqualTo(txId);
        assertThat(domain.getIdempotencyKey()).isEqualTo("tx-key-100");
        assertThat(domain.getAmount()).isEqualTo(Money.brl("100.0000"));
        assertThat(domain.getEntries()).hasSize(2);
        assertThat(domain.getEntries().get(0).direction()).isEqualTo(EntryDirection.DEBIT);
        assertThat(domain.getEntries().get(0).accountCode()).isEqualTo("2.1.02.001");
        assertThat(domain.getEntries().get(1).direction()).isEqualTo(EntryDirection.CREDIT);
        assertThat(domain.getEntries().get(1).accountCode()).isEqualTo("2.1.02.002");
    }

    @Test
    @DisplayName("Should map Domain LedgerTransaction to TransactionEntity")
    void shouldMapDomainToEntity() {
        UUID txId = UUID.randomUUID();
        UUID accId1 = UUID.randomUUID();
        UUID accId2 = UUID.randomUUID();

        LedgerEntry entry1 = LedgerEntry.of(UUID.randomUUID(), txId, accId1, "2.1.02.001", "Sender",
                EntryDirection.DEBIT, Money.brl("50.0000"), Money.brl("950.0000"), 1);
        LedgerEntry entry2 = LedgerEntry.of(UUID.randomUUID(), txId, accId2, "2.1.02.002", "Receiver",
                EntryDirection.CREDIT, Money.brl("50.0000"), Money.brl("1050.0000"), 2);

        LedgerTransaction domain = LedgerTransaction.createCommitted(
                txId, "idem-key-200", TransactionType.INSTANT_PAYMENT,
                Money.brl("50.0000"), "Lunch payment", List.of(entry1, entry2), 8
        );

        AccountEntity accEntity1 = new AccountEntity();
        accEntity1.setId(accId1);
        AccountEntity accEntity2 = new AccountEntity();
        accEntity2.setId(accId2);

        TransactionEntity entity = mapper.toEntity(domain, id -> id.equals(accId1) ? accEntity1 : accEntity2);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(txId);
        assertThat(entity.getIdempotencyKey()).isEqualTo("idem-key-200");
        assertThat(entity.getAmount()).isEqualTo(new BigDecimal("50.0000"));
        assertThat(entity.getEntries()).hasSize(2);
        assertThat(entity.getEntries().get(0).getAccount().getId()).isEqualTo(accId1);
        assertThat(entity.getEntries().get(1).getAccount().getId()).isEqualTo(accId2);
    }
}
