package com.fintech.ledger.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fintech.ledger.application.dto.TransferCommand;
import com.fintech.ledger.application.dto.TransferResult;
import com.fintech.ledger.application.exception.AccountNotFoundException;
import com.fintech.ledger.application.exception.IdempotencyConflictException;
import com.fintech.ledger.application.service.InstantTransferService;
import com.fintech.ledger.domain.exception.InsufficientFundsException;
import com.fintech.ledger.domain.exception.InvalidCurrencyException;
import com.fintech.ledger.domain.model.Account;
import com.fintech.ledger.domain.model.AccountType;
import com.fintech.ledger.domain.model.IdempotencyRecord;
import com.fintech.ledger.domain.model.Money;
import com.fintech.ledger.domain.model.TransactionStatus;
import com.fintech.ledger.domain.repository.AccountRepositoryPort;
import com.fintech.ledger.domain.repository.IdempotencyRepositoryPort;
import com.fintech.ledger.domain.repository.LedgerTransactionRepositoryPort;
import com.fintech.ledger.domain.service.DoubleEntryRuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Application Tests: InstantTransferService & Concurrency Control")
class InstantTransferServiceTest {

    @Mock
    private AccountRepositoryPort accountRepositoryPort;

    @Mock
    private LedgerTransactionRepositoryPort ledgerTransactionRepositoryPort;

    @Mock
    private IdempotencyRepositoryPort idempotencyRepositoryPort;

    @Captor
    private ArgumentCaptor<List<UUID>> lockIdsCaptor;

    @Captor
    private ArgumentCaptor<IdempotencyRecord> idempotencyCaptor;

    private InstantTransferService transferService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        DoubleEntryRuleEngine ruleEngine = new DoubleEntryRuleEngine();

        transferService = new InstantTransferService(
            accountRepositoryPort,
            ledgerTransactionRepositoryPort,
            idempotencyRepositoryPort,
            ruleEngine,
            objectMapper
        );
    }

    @Test
    @DisplayName("Should successfully settle instant transfer and enforce Double-Entry balance mutations")
    void shouldSuccessfullyExecuteTransfer() {
        UUID sourceId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID destId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        Account sourceAcc = Account.create("2.1.01.001", "Client A", AccountType.LIABILITY, Money.brl(1000.00));
        Account destAcc = Account.create("2.1.02.001", "Client B", AccountType.LIABILITY, Money.brl(500.00));

        // Inject IDs
        Account mockSource = new Account(sourceId, sourceAcc.getCode(), sourceAcc.getName(), sourceAcc.getType(),
            sourceAcc.getBalance(), sourceAcc.getStatus(), 0L, Instant.now(), Instant.now());
        Account mockDest = new Account(destId, destAcc.getCode(), destAcc.getName(), destAcc.getType(),
            destAcc.getBalance(), destAcc.getStatus(), 0L, Instant.now(), Instant.now());

        when(idempotencyRepositoryPort.findByKey("idem-tx-001")).thenReturn(Optional.empty());
        when(accountRepositoryPort.findByIdsForUpdate(any())).thenReturn(List.of(mockSource, mockDest));

        TransferCommand command = new TransferCommand(
            sourceId,
            destId,
            new BigDecimal("250.00"),
            "BRL",
            "idem-tx-001",
            "P2P Instant Payment"
        );

        TransferResult result = transferService.transfer(command);

        assertNotNull(result);
        assertEquals(TransactionStatus.COMMITTED, result.status());
        assertEquals(new BigDecimal("250.0000"), result.amount());
        assertEquals("BRL", result.currency());
        assertFalse(result.isIdempotentReplay());
        assertEquals(2, result.entries().size());

        // Verify balance mutations on accounts
        assertEquals(Money.brl(750.00), mockSource.getBalance());
        assertEquals(Money.brl(750.00), mockDest.getBalance());

        // Verify persistence calls
        verify(accountRepositoryPort).save(mockSource);
        verify(accountRepositoryPort).save(mockDest);
        verify(ledgerTransactionRepositoryPort).save(any());
        verify(idempotencyRepositoryPort).save(idempotencyCaptor.capture());

        IdempotencyRecord savedRecord = idempotencyCaptor.getValue();
        assertEquals("idem-tx-001", savedRecord.idempotencyKey());
        assertEquals(200, savedRecord.statusCode());
    }

    @Test
    @DisplayName("Deadlock Prevention: Must ALWAYS lock accounts in ascending UUID order regardless of transfer direction")
    void shouldEnforceDeterministicLockingOrder() {
        // Source has higher UUID than Destination (B -> A)
        UUID highUuid = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        UUID lowUuid = UUID.fromString("00000000-0000-0000-0000-000000000001");

        Account accHigh = new Account(highUuid, "2.1.01.001", "Acc High", AccountType.LIABILITY, Money.brl(1000.00),
            Account.create("1", "1", AccountType.ASSET, Money.zeroBrl()).getStatus(), 0L, Instant.now(), Instant.now());
        Account accLow = new Account(lowUuid, "2.1.02.001", "Acc Low", AccountType.LIABILITY, Money.brl(500.00),
            Account.create("1", "1", AccountType.ASSET, Money.zeroBrl()).getStatus(), 0L, Instant.now(), Instant.now());

        when(idempotencyRepositoryPort.findByKey("idem-deadlock-001")).thenReturn(Optional.empty());
        when(accountRepositoryPort.findByIdsForUpdate(lockIdsCaptor.capture())).thenReturn(List.of(accHigh, accLow));

        TransferCommand command = new TransferCommand(
            highUuid, // source
            lowUuid,  // destination
            new BigDecimal("100.00"),
            "BRL",
            "idem-deadlock-001",
            "Deadlock test"
        );

        transferService.transfer(command);

        List<UUID> capturedLockIds = lockIdsCaptor.getValue();
        assertEquals(2, capturedLockIds.size());
        // Verify lowUuid is first, highUuid is second (deterministic global lock order)
        assertEquals(lowUuid, capturedLockIds.get(0));
        assertEquals(highUuid, capturedLockIds.get(1));
    }

    @Test
    @DisplayName("Idempotency: Replaying with identical key must return cached result without re-mutating accounts")
    void shouldReturnCachedResultOnIdempotentReplay() throws Exception {
        UUID sourceId = UUID.randomUUID();
        UUID destId = UUID.randomUUID();
        String key = "idem-replay-001";

        TransferCommand command = new TransferCommand(
            sourceId,
            destId,
            new BigDecimal("150.00"),
            "BRL",
            key,
            "Replay Test"
        );

        // Compute request hash using service's expected format
        String raw = sourceId + "|" + destId + "|150|" + "BRL|" + key;
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        String expectedHash = java.util.HexFormat.of().formatHex(digest.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        TransferResult cachedResult = new TransferResult(
            UUID.randomUUID(),
            key,
            com.fintech.ledger.domain.model.TransactionType.INSTANT_PAYMENT,
            TransactionStatus.COMMITTED,
            new BigDecimal("150.00"),
            "BRL",
            "Replay Test",
            12,
            "0xabcdef123456",
            Instant.now(),
            false,
            List.of()
        );

        IdempotencyRecord record = IdempotencyRecord.of(
            key,
            cachedResult.transactionId(),
            expectedHash,
            objectMapper.writeValueAsString(cachedResult),
            200,
            Instant.now().plusSeconds(3600)
        );

        when(idempotencyRepositoryPort.findByKey(key)).thenReturn(Optional.of(record));

        TransferResult replayResult = transferService.transfer(command);

        assertNotNull(replayResult);
        assertTrue(replayResult.isIdempotentReplay());
        assertEquals(cachedResult.transactionId(), replayResult.transactionId());

        // Zero database lock calls or repository mutations!
        verifyNoInteractions(accountRepositoryPort);
        verifyNoInteractions(ledgerTransactionRepositoryPort);
    }

    @Test
    @DisplayName("Idempotency Conflict: Reusing same key with different amount must throw IdempotencyConflictException")
    void shouldThrowIdempotencyConflictOnMismatchedPayload() {
        UUID sourceId = UUID.randomUUID();
        UUID destId = UUID.randomUUID();
        String key = "idem-conflict-001";

        TransferCommand command = new TransferCommand(
            sourceId,
            destId,
            new BigDecimal("500.00"), // Different amount
            "BRL",
            key,
            "Conflict Test"
        );

        IdempotencyRecord previousRecord = IdempotencyRecord.of(
            key,
            UUID.randomUUID(),
            "different-sha256-hash-value",
            "{}",
            200,
            Instant.now().plusSeconds(3600)
        );

        when(idempotencyRepositoryPort.findByKey(key)).thenReturn(Optional.of(previousRecord));

        assertThrows(IdempotencyConflictException.class, () -> transferService.transfer(command));
        verifyNoInteractions(accountRepositoryPort);
    }

    @Test
    @DisplayName("Should throw AccountNotFoundException when account is not found in ledger")
    void shouldThrowWhenAccountNotFound() {
        UUID sourceId = UUID.randomUUID();
        UUID destId = UUID.randomUUID();

        when(idempotencyRepositoryPort.findByKey("idem-notfound")).thenReturn(Optional.empty());
        when(accountRepositoryPort.findByIdsForUpdate(any())).thenReturn(List.of()); // Neither found

        TransferCommand command = new TransferCommand(
            sourceId,
            destId,
            new BigDecimal("100.00"),
            "BRL",
            "idem-notfound",
            "Not found test"
        );

        assertThrows(AccountNotFoundException.class, () -> transferService.transfer(command));
    }

    @Test
    @DisplayName("Should throw InsufficientFundsException when source account has inadequate balance")
    void shouldThrowWhenInsufficientFunds() {
        UUID sourceId = UUID.randomUUID();
        UUID destId = UUID.randomUUID();

        Account brokeSource = new Account(sourceId, "2.1.01.001", "Broke Acc", AccountType.LIABILITY, Money.brl(10.00),
            Account.create("1", "1", AccountType.ASSET, Money.zeroBrl()).getStatus(), 0L, Instant.now(), Instant.now());
        Account dest = new Account(destId, "2.1.02.001", "Dest Acc", AccountType.LIABILITY, Money.brl(500.00),
            Account.create("1", "1", AccountType.ASSET, Money.zeroBrl()).getStatus(), 0L, Instant.now(), Instant.now());

        when(idempotencyRepositoryPort.findByKey("idem-broke")).thenReturn(Optional.empty());
        when(accountRepositoryPort.findByIdsForUpdate(any())).thenReturn(List.of(brokeSource, dest));

        TransferCommand command = new TransferCommand(
            sourceId,
            destId,
            new BigDecimal("500.00"), // Requires 500, but only has 10
            "BRL",
            "idem-broke",
            "Broke test"
        );

        assertThrows(InsufficientFundsException.class, () -> transferService.transfer(command));
        verify(accountRepositoryPort, never()).save(any());
        verify(ledgerTransactionRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Should throw InvalidCurrencyException when account currency does not match transfer currency")
    void shouldThrowOnCurrencyMismatch() {
        UUID sourceId = UUID.randomUUID();
        UUID destId = UUID.randomUUID();

        // Account with USD instead of BRL
        Account usdAccount = new Account(sourceId, "1.1.01.001", "USD Reserve", AccountType.ASSET,
            Money.of(1000.00, java.util.Currency.getInstance("USD")),
            Account.create("1", "1", AccountType.ASSET, Money.zeroBrl()).getStatus(), 0L, Instant.now(), Instant.now());
        Account brlAccount = new Account(destId, "2.1.01.001", "BRL Wallet", AccountType.LIABILITY, Money.brl(500.00),
            Account.create("1", "1", AccountType.ASSET, Money.zeroBrl()).getStatus(), 0L, Instant.now(), Instant.now());

        when(idempotencyRepositoryPort.findByKey("idem-curr")).thenReturn(Optional.empty());
        when(accountRepositoryPort.findByIdsForUpdate(any())).thenReturn(List.of(usdAccount, brlAccount));

        TransferCommand command = new TransferCommand(
            sourceId,
            destId,
            new BigDecimal("100.00"),
            "BRL",
            "idem-curr",
            "Currency mismatch test"
        );

        assertThrows(InvalidCurrencyException.class, () -> transferService.transfer(command));
    }
}
