package com.fintech.ledger.infrastructure.web.controller;

import com.fintech.ledger.application.port.WebhookSignerPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * REST Controller serving as a Webhook Simulator / Listener sink.
 * Demonstrates and audits HMAC-SHA256 signature verification for outgoing outbox events.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Webhook Dispatcher", description = "Audit and simulate HMAC-SHA256 webhook deliveries")
public class WebhookSimulatorController {

    private static final Logger log = LoggerFactory.getLogger(WebhookSimulatorController.class);

    private final WebhookSignerPort webhookSignerPort;
    private final String webhookSecret;
    private final List<Map<String, Object>> receivedEvents = Collections.synchronizedList(new ArrayList<>());

    public WebhookSimulatorController(WebhookSignerPort webhookSignerPort,
                                      @Value("${ledger.outbox.webhook-secret:whsec_fintech_core_ledger_secret_2026}") String webhookSecret) {
        this.webhookSignerPort = Objects.requireNonNull(webhookSignerPort, "webhookSignerPort must not be null");
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/listener")
    @Operation(summary = "Webhook delivery sink", description = "Validates cryptographic HMAC-SHA256 signature before ingesting webhook")
    public ResponseEntity<?> receiveWebhook(
            @RequestHeader(value = "X-Ledger-Signature", required = false) String signature,
            @RequestHeader(value = "X-Event-ID", required = false) String eventId,
            @RequestHeader(value = "X-Event-Type", required = false) String eventType,
            @RequestBody String rawPayload) {

        if (signature == null || !webhookSignerPort.verify(rawPayload, signature, webhookSecret)) {
            log.warn("Webhook rejected: Invalid or missing HMAC-SHA256 signature [{}]", signature);
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.UNAUTHORIZED, "Invalid HMAC-SHA256 webhook signature"
            );
            problem.setTitle("Webhook Signature Verification Failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
        }

        log.info("Webhook verified successfully: Event ID [{}], Type [{}]", eventId, eventType);
        receivedEvents.add(Map.of(
                "eventId", eventId != null ? eventId : "unknown",
                "eventType", eventType != null ? eventType : "unknown",
                "signature", signature,
                "payload", rawPayload,
                "receivedAt", Instant.now().toString()
        ));

        return ResponseEntity.ok(Map.of("status", "RECEIVED", "verified", true));
    }

    @GetMapping("/received")
    @Operation(summary = "List received webhooks", description = "Returns all verified webhooks collected by the simulator")
    public ResponseEntity<List<Map<String, Object>>> getReceivedWebhooks() {
        return ResponseEntity.ok(new ArrayList<>(receivedEvents));
    }
}
