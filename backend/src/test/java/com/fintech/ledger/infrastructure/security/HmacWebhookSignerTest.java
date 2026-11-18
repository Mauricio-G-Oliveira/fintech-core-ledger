package com.fintech.ledger.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HmacWebhookSignerTest {

    private HmacWebhookSigner signer;
    private final String secret = "whsec_test_secret_key_123456789";

    @BeforeEach
    void setUp() {
        signer = new HmacWebhookSigner();
    }

    @Test
    @DisplayName("Should sign payload and output format prefixed with sha256=")
    void shouldSignPayload() {
        String payload = "{\"event\":\"transfer.settled\",\"amount\":150.00}";

        String signature = signer.sign(payload, secret);

        assertThat(signature).isNotNull();
        assertThat(signature).startsWith("sha256=");
        assertThat(signature.length()).isEqualTo(7 + 64); // "sha256=" + 64 hex characters
    }

    @Test
    @DisplayName("Should verify valid HMAC-SHA256 signature successfully")
    void shouldVerifyValidSignature() {
        String payload = "{\"event\":\"transfer.settled\",\"amount\":500.00}";
        String signature = signer.sign(payload, secret);

        boolean isValid = signer.verify(payload, signature, secret);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject verification if payload was tampered with")
    void shouldRejectTamperedPayload() {
        String originalPayload = "{\"event\":\"transfer.settled\",\"amount\":100.00}";
        String tamperedPayload = "{\"event\":\"transfer.settled\",\"amount\":999.00}";

        String signature = signer.sign(originalPayload, secret);

        boolean isValid = signer.verify(tamperedPayload, signature, secret);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject verification if secret differs")
    void shouldRejectDifferentSecret() {
        String payload = "{\"event\":\"transfer.settled\"}";
        String signature = signer.sign(payload, secret);

        boolean isValid = signer.verify(payload, signature, "different_secret_key");

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when payload or secret is null")
    void shouldThrowOnNullInput() {
        assertThatThrownBy(() -> signer.sign(null, secret))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> signer.sign("data", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
