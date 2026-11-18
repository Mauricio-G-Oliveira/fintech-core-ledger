package com.fintech.ledger.infrastructure.security;

import com.fintech.ledger.application.port.WebhookSignerPort;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Enterprise Webhook Signer computing cryptographic HMAC-SHA256 signatures.
 * Includes constant-time equality check to prevent timing attack vulnerabilities.
 */
@Component
public class HmacWebhookSigner implements WebhookSignerPort {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    @Override
    public String sign(String payload, String secret) {
        if (payload == null || secret == null) {
            throw new IllegalArgumentException("Payload and secret must not be null");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return SIGNATURE_PREFIX + HexFormat.of().formatHex(rawHmac);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA256 webhook signature", e);
        }
    }

    @Override
    public boolean verify(String payload, String signature, String secret) {
        if (payload == null || signature == null || secret == null) {
            return false;
        }
        String expectedSignature = sign(payload, secret);
        // Time-constant comparison to prevent side-channel timing attacks
        return MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        );
    }
}
