package com.fintech.ledger.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.ledger.application.dto.TransferCommand;
import com.fintech.ledger.application.dto.TransferResult;
import com.fintech.ledger.application.exception.AccountNotFoundException;
import com.fintech.ledger.application.exception.IdempotencyConflictException;
import com.fintech.ledger.application.usecase.InstantTransferUseCase;
import com.fintech.ledger.domain.exception.InvalidCurrencyException;
import com.fintech.ledger.domain.model.Account;
import com.fintech.ledger.domain.model.IdempotencyRecord;
import com.fintech.ledger.domain.model.LedgerTransaction;
import com.fintech.ledger.domain.model.Money;
import com.fintech.ledger.domain.repository.AccountRepositoryPort;
import com.fintech.ledger.domain.repository.IdempotencyRepositoryPort;
import com.fintech.ledger.domain.repository.LedgerTransactionRepositoryPort;
import com.fintech.ledger.domain.service.DoubleEntryRuleEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Core Application Service implementing the Instant Transfer Use Case.
 * 
 * Key Responsibilities:
 * 1. Idempotency Verification: Checks for previous execution and returns cached result if duplicate.
 * 2. Deadlock-Free Concurrency: Locks accounts in deterministic ascending UUID order via SELECT FOR UPDATE.
 * 3. Double-Entry Invariant Enforcement: Delegates to DoubleEntryRuleEngine for balanced mutations.
 * 4. ACID Persistence: Updates accounts, immutable transaction journal, and idempotency cache in a single atomic transaction.
 */
@Service
public class InstantTransferService implements InstantTransferUseCase {

    private static final Logger log = LoggerFactory.getLogger(InstantTransferService.class);

    private final AccountRepositoryPort accountRepositoryPort;
    private final LedgerTransactionRepositoryPort ledgerTransactionRepositoryPort;
    private final IdempotencyRepositoryPort idempotencyRepositoryPort;
    private final DoubleEntryRuleEngine doubleEntryRuleEngine;
    private final ObjectMapper objectMapper;

    public InstantTransferService(AccountRepositoryPort accountRepositoryPort,
                                  LedgerTransactionRepositoryPort ledgerTransactionRepositoryPort,
                                  IdempotencyRepositoryPort idempotencyRepositoryPort,
                                  DoubleEntryRuleEngine doubleEntryRuleEngine,
                                  ObjectMapper objectMapper) {
        this.accountRepositoryPort = accountRepositoryPort;
        this.ledgerTransactionRepositoryPort = ledgerTransactionRepositoryPort;
        this.idempotencyRepositoryPort = idempotencyRepositoryPort;
        this.doubleEntryRuleEngine = doubleEntryRuleEngine;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransferResult transfer(TransferCommand command) {
        long startTime = System.currentTimeMillis();
        String requestHash = computeRequestHash(command);

        // 1. Idempotency Guard
        Optional<IdempotencyRecord> existingRecordOpt = idempotencyRepositoryPort.findByKey(command.idempotencyKey());
        if (existingRecordOpt.isPresent()) {
            IdempotencyRecord existingRecord = existingRecordOpt.get();
            if (!existingRecord.requestHash().equals(requestHash)) {
                throw new IdempotencyConflictException(
                    "Idempotency key '" + command.idempotencyKey() + "' was previously used with a different request payload"
                );
            }
            log.info("Idempotent replay detected for key: {}", command.idempotencyKey());
            return deserializeCachedResult(existingRecord.responseBody());
        }

        // 2. Deadlock-Free Deterministic Lock Acquisition (Ordered by UUID ascending)
        List<UUID> sortedAccountIds = Stream.of(command.sourceAccountId(), command.destinationAccountId())
            .sorted()
            .toList();

        log.debug("Acquiring pessimistic write locks in deterministic order: {}", sortedAccountIds);
        List<Account> lockedAccounts = accountRepositoryPort.findByIdsForUpdate(sortedAccountIds);

        Map<UUID, Account> accountMap = lockedAccounts.stream()
            .collect(Collectors.toMap(Account::getId, Function.identity()));

        Account sourceAccount = accountMap.get(command.sourceAccountId());
        Account destinationAccount = accountMap.get(command.destinationAccountId());

        if (sourceAccount == null) {
            throw new AccountNotFoundException(command.sourceAccountId());
        }
        if (destinationAccount == null) {
            throw new AccountNotFoundException(command.destinationAccountId());
        }

        // 3. Currency Validation
        Money transferAmount = Money.of(command.amount(), command.getCurrency());
        if (!sourceAccount.getBalance().currency().equals(command.getCurrency())) {
            throw new InvalidCurrencyException(
                "Source account currency " + sourceAccount.getBalance().currency() + " does not match transfer currency " + command.currencyCode()
            );
        }
        if (!destinationAccount.getBalance().currency().equals(command.getCurrency())) {
            throw new InvalidCurrencyException(
                "Destination account currency " + destinationAccount.getBalance().currency() + " does not match transfer currency " + command.currencyCode()
            );
        }

        // 4. Double-Entry Bookkeeping Mutation
        UUID transactionId = UUID.randomUUID();
        int latencyMs = (int) (System.currentTimeMillis() - startTime);

        LedgerTransaction transaction = doubleEntryRuleEngine.createTransfer(
            transactionId,
            command.idempotencyKey(),
            sourceAccount,
            destinationAccount,
            transferAmount,
            command.description() != null ? command.description() : "Instant Account Transfer",
            Math.max(1, latencyMs)
        );

        // 5. Atomic Persistence
        accountRepositoryPort.save(sourceAccount);
        accountRepositoryPort.save(destinationAccount);
        ledgerTransactionRepositoryPort.save(transaction);

        TransferResult result = TransferResult.fromDomain(transaction, false);

        // 6. Persist Idempotency Record
        String responseJson = serializeResult(result);
        IdempotencyRecord idempotencyRecord = IdempotencyRecord.of(
            command.idempotencyKey(),
            transactionId,
            requestHash,
            responseJson,
            200,
            Instant.now().plusSeconds(86400) // 24h retention
        );
        idempotencyRepositoryPort.save(idempotencyRecord);

        log.info("Successfully settled transaction {} with idempotency key {}", transactionId, command.idempotencyKey());
        return result;
    }

    private String computeRequestHash(TransferCommand command) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String raw = command.sourceAccountId() + "|" +
                         command.destinationAccountId() + "|" +
                         command.amount().stripTrailingZeros().toPlainString() + "|" +
                         command.currencyCode() + "|" +
                         command.idempotencyKey();
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private String serializeResult(TransferResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize TransferResult to JSON", e);
            throw new IllegalStateException("Serialization error", e);
        }
    }

    private TransferResult deserializeCachedResult(String json) {
        try {
            TransferResult original = objectMapper.readValue(json, TransferResult.class);
            // Return marked as idempotent replay
            return new TransferResult(
                original.transactionId(),
                original.idempotencyKey(),
                original.type(),
                original.status(),
                original.amount(),
                original.currency(),
                original.description(),
                original.latencyMs(),
                original.hash(),
                original.createdAt(),
                true, // isIdempotentReplay = true
                original.entries()
            );
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize cached TransferResult JSON", e);
            throw new IllegalStateException("Deserialization error", e);
        }
    }
}
