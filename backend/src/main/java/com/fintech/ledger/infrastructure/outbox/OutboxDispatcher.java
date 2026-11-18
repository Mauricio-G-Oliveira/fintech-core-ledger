package com.fintech.ledger.infrastructure.outbox;

import com.fintech.ledger.application.port.WebhookSignerPort;
import com.fintech.ledger.domain.model.OutboxEvent;
import com.fintech.ledger.domain.repository.OutboxRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Transactional Outbox Dispatcher & Webhook Publisher.
 * Asynchronously polls pending events from `ledger.outbox_events`,
 * calculates cryptographic HMAC-SHA256 signatures, and dispatches them with retry backoff.
 */
@Component
public class OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);

    private final OutboxRepositoryPort outboxRepositoryPort;
    private final WebhookSignerPort webhookSignerPort;
    private final String webhookUrl;
    private final String webhookSecret;
    private final HttpClient httpClient;
    private Consumer<OutboxEvent> testDeliveryConsumer;

    public OutboxDispatcher(OutboxRepositoryPort outboxRepositoryPort,
                            WebhookSignerPort webhookSignerPort,
                            @Value("${ledger.outbox.webhook-url:http://localhost:8080/api/v1/webhooks/listener}") String webhookUrl,
                            @Value("${ledger.outbox.webhook-secret:whsec_fintech_core_ledger_secret_2026}") String webhookSecret) {
        this.outboxRepositoryPort = Objects.requireNonNull(outboxRepositoryPort, "outboxRepositoryPort must not be null");
        this.webhookSignerPort = Objects.requireNonNull(webhookSignerPort, "webhookSignerPort must not be null");
        this.webhookUrl = webhookUrl;
        this.webhookSecret = webhookSecret;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    /**
     * Periodic background poller.
     */
    @Scheduled(fixedDelayString = "${ledger.outbox.poll-interval-ms:2000}")
    public void scheduledDispatch() {
        try {
            int dispatched = dispatchPendingEvents(25);
            if (dispatched > 0) {
                log.debug("Outbox poller dispatched {} events", dispatched);
            }
        } catch (Exception e) {
            log.error("Error during scheduled outbox dispatching", e);
        }
    }

    /**
     * Polls pending events and dispatches them with HMAC-SHA256 signatures.
     * Transactional per batch with separate transaction propagation.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int dispatchPendingEvents(int batchSize) {
        List<OutboxEvent> pendingEvents = outboxRepositoryPort.findPendingEvents(batchSize);
        if (pendingEvents.isEmpty()) {
            return 0;
        }

        int processed = 0;
        for (OutboxEvent event : pendingEvents) {
            try {
                String signature = webhookSignerPort.sign(event.getPayload(), webhookSecret);

                if (testDeliveryConsumer != null) {
                    testDeliveryConsumer.accept(event);
                } else if (webhookUrl != null && !webhookUrl.isBlank()) {
                    deliverHttp(event, signature);
                }

                event.markPublished();
                outboxRepositoryPort.save(event);
                processed++;
                log.info("Successfully published outbox event {} [type: {}] with HMAC signature", 
                        event.getId(), event.getEventType());
            } catch (Exception e) {
                log.warn("Failed to dispatch outbox event {}. Scheduling retry. Reason: {}", 
                        event.getId(), e.getMessage());
                event.recordFailure(e.getMessage());
                outboxRepositoryPort.save(event);
            }
        }
        return processed;
    }

    private void deliverHttp(OutboxEvent event, String signature) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .header("X-Ledger-Signature", signature)
                .header("X-Event-ID", event.getId().toString())
                .header("X-Event-Type", event.getEventType())
                .header("X-Aggregate-ID", event.getAggregateId().toString())
                .POST(HttpRequest.BodyPublishers.ofString(event.getPayload()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Webhook endpoint responded with HTTP " + response.statusCode());
        }
    }

    public void setTestDeliveryConsumer(Consumer<OutboxEvent> testDeliveryConsumer) {
        this.testDeliveryConsumer = testDeliveryConsumer;
    }
}
