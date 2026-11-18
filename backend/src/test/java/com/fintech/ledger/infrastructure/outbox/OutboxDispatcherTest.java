package com.fintech.ledger.infrastructure.outbox;

import com.fintech.ledger.application.port.WebhookSignerPort;
import com.fintech.ledger.domain.model.OutboxEvent;
import com.fintech.ledger.domain.model.OutboxStatus;
import com.fintech.ledger.domain.repository.OutboxRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Outbox Dispatcher Tests: OutboxDispatcher & HMAC Webhook Delivery")
class OutboxDispatcherTest {

    @Mock
    private OutboxRepositoryPort outboxRepositoryPort;

    @Mock
    private WebhookSignerPort webhookSignerPort;

    @Captor
    private ArgumentCaptor<OutboxEvent> eventCaptor;

    private OutboxDispatcher dispatcher;

    private final String webhookUrl = "http://localhost:8080/api/v1/webhooks/listener";
    private final String webhookSecret = "whsec_test_secret_123456789";

    @BeforeEach
    void setUp() {
        dispatcher = new OutboxDispatcher(outboxRepositoryPort, webhookSignerPort, webhookUrl, webhookSecret);
    }

    @Test
    @DisplayName("Should successfully dispatch pending events, sign with HMAC and mark as PUBLISHED")
    void shouldDispatchPendingEventsAndMarkAsPublished() {
        UUID eventId = UUID.randomUUID();
        UUID aggId = UUID.randomUUID();
        String payload = "{\"amount\":250.00,\"currency\":\"BRL\"}";

        OutboxEvent event = OutboxEvent.createPending("TRANSACTION", aggId, "TRANSFER_SETTLED", payload);

        when(outboxRepositoryPort.findPendingEvents(25)).thenReturn(List.of(event));
        when(webhookSignerPort.sign(eq(payload), eq(webhookSecret))).thenReturn("t=1700000000,v1=mocked_hmac_signature");

        AtomicBoolean delivered = new AtomicBoolean(false);
        dispatcher.setTestDeliveryConsumer(ev -> delivered.set(true));

        int processed = dispatcher.dispatchPendingEvents(25);

        assertThat(processed).isEqualTo(1);
        assertThat(delivered.get()).isTrue();

        verify(webhookSignerPort).sign(payload, webhookSecret);
        verify(outboxRepositoryPort).save(eventCaptor.capture());

        OutboxEvent savedEvent = eventCaptor.getValue();
        assertThat(savedEvent.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(savedEvent.getPublishedAt()).isNotNull();
        assertThat(savedEvent.getLastError()).isNull();
    }

    @Test
    @DisplayName("Should handle delivery failure and schedule exponential backoff retry")
    void shouldHandleDeliveryFailureAndScheduleRetry() {
        UUID aggId = UUID.randomUUID();
        String payload = "{\"amount\":100.00}";

        OutboxEvent event = OutboxEvent.createPending("TRANSACTION", aggId, "TRANSFER_SETTLED", payload);

        when(outboxRepositoryPort.findPendingEvents(10)).thenReturn(List.of(event));
        when(webhookSignerPort.sign(anyString(), anyString())).thenReturn("v1=signature");

        dispatcher.setTestDeliveryConsumer(ev -> {
            throw new RuntimeException("Connection timed out to webhook endpoint");
        });

        int processed = dispatcher.dispatchPendingEvents(10);

        assertThat(processed).isEqualTo(0);

        verify(outboxRepositoryPort).save(eventCaptor.capture());
        OutboxEvent savedEvent = eventCaptor.getValue();

        assertThat(savedEvent.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(savedEvent.getRetryCount()).isEqualTo(1);
        assertThat(savedEvent.getLastError()).contains("Connection timed out");
        assertThat(savedEvent.getNextRetryAt()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("Should mark event as FAILED when max retries limit is reached")
    void shouldMarkEventAsFailedWhenMaxRetriesExceeded() {
        UUID aggId = UUID.randomUUID();
        String payload = "{\"amount\":500.00}";

        // Event already at 4 retries, max is 5
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(), "TRANSACTION", aggId, "TRANSFER_SETTLED", payload,
                OutboxStatus.PENDING, 4, 5, Instant.now(), null, "Previous error", Instant.now().minusSeconds(600)
        );

        when(outboxRepositoryPort.findPendingEvents(5)).thenReturn(List.of(event));
        when(webhookSignerPort.sign(anyString(), anyString())).thenReturn("v1=signature");

        dispatcher.setTestDeliveryConsumer(ev -> {
            throw new RuntimeException("HTTP 500 Internal Server Error");
        });

        int processed = dispatcher.dispatchPendingEvents(5);

        assertThat(processed).isEqualTo(0);

        verify(outboxRepositoryPort).save(eventCaptor.capture());
        OutboxEvent savedEvent = eventCaptor.getValue();

        assertThat(savedEvent.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(savedEvent.getRetryCount()).isEqualTo(5);
        assertThat(savedEvent.getNextRetryAt()).isNull();
        assertThat(savedEvent.getLastError()).contains("HTTP 500");
    }

    @Test
    @DisplayName("Should return 0 without error when there are no pending events")
    void shouldReturnZeroWhenNoPendingEvents() {
        when(outboxRepositoryPort.findPendingEvents(25)).thenReturn(List.of());

        int processed = dispatcher.dispatchPendingEvents(25);

        assertThat(processed).isEqualTo(0);
        verify(outboxRepositoryPort, never()).save(any());
        verify(webhookSignerPort, never()).sign(anyString(), anyString());
    }
}
