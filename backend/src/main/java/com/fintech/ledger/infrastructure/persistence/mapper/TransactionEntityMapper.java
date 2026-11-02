package com.fintech.ledger.infrastructure.persistence.mapper;

import com.fintech.ledger.domain.model.LedgerEntry;
import com.fintech.ledger.domain.model.LedgerTransaction;
import com.fintech.ledger.domain.model.Money;
import com.fintech.ledger.infrastructure.persistence.entity.AccountEntity;
import com.fintech.ledger.infrastructure.persistence.entity.LedgerEntryEntity;
import com.fintech.ledger.infrastructure.persistence.entity.TransactionEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Mapper between LedgerTransaction aggregate and TransactionEntity / LedgerEntryEntity.
 */
@Component
public class TransactionEntityMapper {

    public LedgerTransaction toDomain(TransactionEntity entity) {
        if (entity == null) {
            return null;
        }

        List<LedgerEntry> entries = new ArrayList<>();
        if (entity.getEntries() != null) {
            for (LedgerEntryEntity entryEntity : entity.getEntries()) {
                AccountEntity account = entryEntity.getAccount();
                entries.add(new LedgerEntry(
                    entryEntity.getId(),
                    entity.getId(),
                    account != null ? account.getId() : null,
                    account != null ? account.getCode() : "",
                    account != null ? account.getName() : "",
                    entryEntity.getDirection(),
                    Money.of(entryEntity.getAmount(), entryEntity.getCurrency()),
                    Money.of(entryEntity.getRunningBalance(), entryEntity.getCurrency()),
                    entryEntity.getSequence(),
                    entryEntity.getCreatedAt()
                ));
            }
        }

        return LedgerTransaction.reconstitute(
            entity.getId(),
            entity.getIdempotencyKey(),
            entity.getType(),
            entity.getStatus(),
            Money.of(entity.getAmount(), entity.getCurrency()),
            entity.getDescription(),
            entity.getSettlementLatencyMs(),
            entity.getHash(),
            entity.getCreatedAt(),
            entries
        );
    }

    public TransactionEntity toEntity(LedgerTransaction domain, Function<UUID, AccountEntity> accountEntityResolver) {
        if (domain == null) {
            return null;
        }

        TransactionEntity txEntity = new TransactionEntity(
            domain.getId(),
            domain.getIdempotencyKey(),
            domain.getType(),
            domain.getStatus(),
            domain.getAmount().amount(),
            domain.getAmount().currency().getCurrencyCode(),
            domain.getDescription(),
            domain.getSettlementLatencyMs(),
            domain.getHash(),
            domain.getCreatedAt()
        );

        if (domain.getEntries() != null) {
            for (LedgerEntry entry : domain.getEntries()) {
                AccountEntity accountEntity = accountEntityResolver != null 
                    ? accountEntityResolver.apply(entry.accountId()) 
                    : null;

                LedgerEntryEntity entryEntity = new LedgerEntryEntity(
                    entry.id(),
                    txEntity,
                    accountEntity,
                    entry.direction(),
                    entry.amount().amount(),
                    entry.amount().currency().getCurrencyCode(),
                    entry.runningBalance().amount(),
                    entry.sequence(),
                    entry.createdAt()
                );
                txEntity.addEntry(entryEntity);
            }
        }

        return txEntity;
    }
}
