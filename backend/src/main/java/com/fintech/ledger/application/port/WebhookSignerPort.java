package com.fintech.ledger.application.port;

/**
 * Output Port for computing cryptographic HMAC-SHA256 signatures for outgoing webhooks.
 */
public interface WebhookSignerPort {

    String sign(String payload, String secret);

    boolean verify(String payload, String signature, String secret);
}
