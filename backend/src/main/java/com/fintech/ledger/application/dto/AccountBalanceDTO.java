package com.fintech.ledger.application.dto;

import com.fintech.ledger.domain.model.Account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object representing the current verified balance of an account.
 */
public record AccountBalanceDTO(
    UUID accountId,
    String accountCode,
    String accountName,
    BigDecimal availableBalance,
    String currency,
    Instant timestamp
) {
    public static AccountBalanceDTO fromDomain(Account account) {
        if (account == null) {
            return null;
        }
        return new AccountBalanceDTO(
            account.getId(),
            account.getCode(),
            account.getName(),
            account.getBalance().amount(),
            account.getBalance().currency().getCurrencyCode(),
            Instant.now()
        );
    }
}
