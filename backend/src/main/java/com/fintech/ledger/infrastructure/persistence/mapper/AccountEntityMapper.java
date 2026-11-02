package com.fintech.ledger.infrastructure.persistence.mapper;

import com.fintech.ledger.domain.model.Account;
import com.fintech.ledger.domain.model.Money;
import com.fintech.ledger.infrastructure.persistence.entity.AccountEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper between Account domain aggregate and AccountEntity JPA entity.
 * Keeps domain models free of persistence annotations and vice-versa.
 */
@Component
public class AccountEntityMapper {

    public Account toDomain(AccountEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Account(
            entity.getId(),
            entity.getCode(),
            entity.getName(),
            entity.getType(),
            Money.of(entity.getBalance(), entity.getCurrency()),
            entity.getStatus(),
            entity.getVersion(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public AccountEntity toEntity(Account domain) {
        if (domain == null) {
            return null;
        }
        return new AccountEntity(
            domain.getId(),
            domain.getCode(),
            domain.getName(),
            domain.getType(),
            domain.getBalance().amount(),
            domain.getBalance().currency().getCurrencyCode(),
            domain.getStatus(),
            domain.getVersion(),
            domain.getCreatedAt(),
            domain.getUpdatedAt()
        );
    }

    public void updateEntityFromDomain(Account domain, AccountEntity entity) {
        if (domain == null || entity == null) {
            return;
        }
        entity.setBalance(domain.getBalance().amount());
        entity.setStatus(domain.getStatus());
        entity.setUpdatedAt(domain.getUpdatedAt());
    }
}
