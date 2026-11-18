package com.fintech.ledger.integration;

import com.fintech.ledger.application.dto.TransferCommand;
import com.fintech.ledger.application.dto.TransferResult;
import com.fintech.ledger.application.port.WebhookSignerPort;
import com.fintech.ledger.application.usecase.InstantTransferUseCase;
import com.fintech.ledger.domain.model.Account;
import com.fintech.ledger.domain.model.AccountType;
import com.fintech.ledger.domain.model.Money;
import com.fintech.ledger.domain.model.OutboxEvent;
import com.fintech.ledger.domain.model.OutboxStatus;
import com.fintech.ledger.domain.repository.AccountRepositoryPort;
import com.fintech.ledger.domain.repository.OutboxRepositoryPort;
import com.fintech.ledger.infrastructure.outbox.OutboxDispatcher;
import com.fintech.ledger.infrastructure.persistence.repository.SpringDataOutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Integration Tests: Transactional Outbox Pattern & HMAC Webhook Delivery")
class TransactionalOutboxIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private InstantTransferUseCase instantTransferUseCase;

    @Autowired
    private AccountRepositoryPort accountRepositoryPort;

    @Autowired
    private OutboxRepositoryPort outboxRepositoryPort;

    @Autowired
    private SpringDataOutboxRepository springDataOutboxRepository;

    @Autowired
    private OutboxDispatcher outboxDispatcher;

    @Autowired
    private WebhookSignerPort webhookSignerPort;

    @Value("${ledger.outbox.webhook-secret:whsec_fintech_core_ledger_secret_2026}")
    private String webhookSecret;

    @Test
    @DisplayName("Should atomically persist OutboxEvent on transfer and dispatch with valid HMAC-SHA256 signature")
    void shouldAtomicallyPersistOutboxAndDispatchWebhook() {
        // 1. Arrange: Create initial accounts in PostgreSQL
        Account source = Account.create("OUTBOX-SRC", "Outbox Source Account", AccountType.LIABILITY, Money.brl("1000.0000"));
        Account dest = Account.create("OUTBOX-DST", "Outbox Dest Account", AccountType.LIABILITY, Money.brl("200.0000"));

        source = accountRepositoryPort.save(source);
        dest = accountRepositoryPort.save(dest);

        String idempotencyKey = "outbox-tx-" + UUID.randomUUID();
        TransferCommand command = new TransferCommand(
                source.getId(),
                dest.getId(),
                new BigDecimal("150.00"),
                "BRL",
                idempotencyKey,
                "Outbox pattern integration test"
        );

        // 2. Act: Execute transfer
        TransferResult result = instantTransferUseCase.transfer(command);
        assertThat(result).isNotNull();
        UUID transactionId = result.transactionId();

        // 3. Assert: Outbox event was atomically persisted in the same transaction
        List<OutboxEvent> pendingEvents = outboxRepositoryPort.findPendingEvents(50);
        OutboxEvent outboxEvent = pendingEvents.stream()
                .filter(e -> transactionId.equals(e.getAggregateId()))
                .findFirst()
                .orElse(null);

        assertThat(outboxEvent).isNotNull();
        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outboxEvent.getAggregateType()).isEqualTo("TRANSACTION");
        assertThat(outboxEvent.getEventType()).isEqualTo("TRANSFER_SETTLED");
        assertThat(outboxEvent.getPayload()).contains(idempotencyKey);
        assertThat(outboxEvent.getPayload()).contains("150.00");

        // 4. Act: Dispatch outbox events using OutboxDispatcher
        AtomicReference<OutboxEvent> deliveredEventRef = new AtomicReference<>();
        outboxDispatcher.setTestDeliveryConsumer(deliveredEventRef::set);

        int dispatchedCount = outboxDispatcher.dispatchPendingEvents(50);
        assertThat(dispatchedCount).isGreaterThanOrEqualTo(1);
        assertThat(deliveredEventRef.get()).isNotNull();

        // 5. Assert: Event in database transitioned to PUBLISHED
        OutboxEvent publishedEvent = outboxRepositoryPort.findById(outboxEvent.getId()).orElseThrow();
        assertThat(publishedEvent.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(publishedEvent.getPublishedAt()).isNotNull();
        assertThat(publishedEvent.getLastError()).isNull();

        // 6. Assert: Cryptographic HMAC-SHA256 signature verification
        String payload = publishedEvent.getPayload();
        String signature = webhookSignerPort.sign(payload, webhookSecret);

        // Verify valid signature
        boolean isValid = webhookSignerPort.verify(payload, signature, webhookSecret);
        assertThat(isValid).isTrue();

        // Verify tampered payload fails signature validation
        String tamperedPayload = payload.replace("150.00", "999999.00");
        boolean isTamperedValid = webhookSignerPort.verify(tamperedPayload, signature, webhookSecret);
        assertThat(isTamperedValid).isFalse();

        // Verify wrong secret fails signature validation
        boolean isWrongSecretValid = webhookSignerPort.verify(payload, signature, "wrong_secret");
        assertThat(isWrongSecretValid).isFalse();
    }
}
