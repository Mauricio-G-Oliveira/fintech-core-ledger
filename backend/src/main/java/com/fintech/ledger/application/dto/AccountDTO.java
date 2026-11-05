package com.fintech.ledger.application.dto;

import com.fintech.ledger.domain.model.Account;
import com.fintech.ledger.domain.model.AccountStatus;
import com.fintech.ledger.domain.model.AccountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object representing an Account in the Chart of Accounts (COA).
 */
public record AccountDTO(
    UUID id,
    String code,
    String name,
    AccountType type,
    BigDecimal balance,
    String currency,
    AccountStatus status,
    Long version,
    Instant createdAt,
    Instant updatedAt
) {
    public static AccountDTO fromDomain(Account account) {
        if (account == null) {
            return null;
        }
        return new AccountDTO(
            account.getId(),
            account.getCode(),
            account.getName(),
            account.getType(),
            account.getBalance().amount(),
            account.getBalance().currency().getCurrencyCode(),
            account.getStatus(),
            account.getVersion(),
            account.getCreatedAt(),
            account.getUpdatedAt()
        );
    }
}
