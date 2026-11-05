package com.fintech.ledger.application.service;

import com.fintech.ledger.application.dto.AccountBalanceDTO;
import com.fintech.ledger.application.dto.AccountDTO;
import com.fintech.ledger.application.exception.AccountNotFoundException;
import com.fintech.ledger.application.usecase.AccountQueryUseCase;
import com.fintech.ledger.domain.repository.AccountRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Application Service implementing AccountQueryUseCase.
 * Handles read-only queries against the AccountRepositoryPort.
 */
@Service
@Transactional(readOnly = true)
public class AccountQueryService implements AccountQueryUseCase {

    private final AccountRepositoryPort accountRepositoryPort;

    public AccountQueryService(AccountRepositoryPort accountRepositoryPort) {
        this.accountRepositoryPort = Objects.requireNonNull(accountRepositoryPort, "accountRepositoryPort must not be null");
    }

    @Override
    public List<AccountDTO> findAllAccounts() {
        return accountRepositoryPort.findAll()
                .stream()
                .map(AccountDTO::fromDomain)
                .toList();
    }

    @Override
    public AccountDTO getAccountById(UUID id) {
        Objects.requireNonNull(id, "Account ID must not be null");
        return accountRepositoryPort.findById(id)
                .map(AccountDTO::fromDomain)
                .orElseThrow(() -> new AccountNotFoundException("Account with ID " + id + " was not found"));
    }

    @Override
    public AccountBalanceDTO getAccountBalance(UUID id) {
        Objects.requireNonNull(id, "Account ID must not be null");
        return accountRepositoryPort.findById(id)
                .map(AccountBalanceDTO::fromDomain)
                .orElseThrow(() -> new AccountNotFoundException("Account with ID " + id + " was not found"));
    }

    @Override
    public AccountDTO getAccountByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Account code must not be null or blank");
        }
        return accountRepositoryPort.findByCode(code)
                .map(AccountDTO::fromDomain)
                .orElseThrow(() -> new AccountNotFoundException("Account with code '" + code + "' was not found"));
    }
}
