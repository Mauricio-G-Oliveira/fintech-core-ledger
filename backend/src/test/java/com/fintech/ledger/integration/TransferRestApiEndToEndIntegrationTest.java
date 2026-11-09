package com.fintech.ledger.integration;

import com.fintech.ledger.application.dto.AccountBalanceDTO;
import com.fintech.ledger.application.dto.TransferResult;
import com.fintech.ledger.domain.model.Account;
import com.fintech.ledger.domain.model.AccountType;
import com.fintech.ledger.domain.model.Money;
import com.fintech.ledger.domain.repository.AccountRepositoryPort;
import com.fintech.ledger.infrastructure.web.dto.request.InitiateTransferRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-End REST API Integration Test using a real PostgreSQL 16 Testcontainer.
 * Validates the full HTTP lifecycle, header injection, RFC 7807 error responses, and database updates.
 */
class TransferRestApiEndToEndIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AccountRepositoryPort accountRepositoryPort;

    @Test
    @DisplayName("Should execute end-to-end instant transfer, replay idempotently, and query audit trail")
    void shouldExecuteFullTransferLifecycle() {
        // 1. Arrange: Create initial accounts in PostgreSQL
        Account sender = Account.create("E2E-SENDER", "E2E Sender Account", AccountType.LIABILITY, Money.brl("2000.0000"));
        Account receiver = Account.create("E2E-RECEIVER", "E2E Receiver Account", AccountType.LIABILITY, Money.brl("500.0000"));

        sender = accountRepositoryPort.save(sender);
        receiver = accountRepositoryPort.save(receiver);

        UUID senderId = sender.getId();
        UUID receiverId = receiver.getId();
        String idempotencyKey = "e2e-tx-" + UUID.randomUUID();

        InitiateTransferRequest request = new InitiateTransferRequest(
                senderId, receiverId, new BigDecimal("350.00"), "BRL", "E2E payment verification"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", idempotencyKey);

        HttpEntity<InitiateTransferRequest> entity = new HttpEntity<>(request, headers);

        // 2. Act: Execute transfer (first call -> 201 Created)
        ResponseEntity<TransferResult> response1 = restTemplate.exchange(
                "/api/v1/transfers", HttpMethod.POST, entity, TransferResult.class
        );

        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response1.getHeaders().getFirst("Idempotency-Key")).isEqualTo(idempotencyKey);
        assertThat(response1.getHeaders().getFirst("Idempotency-Replay")).isEqualTo("false");
        assertThat(response1.getHeaders().getFirst("X-Cryptographic-Hash")).isNotBlank();
        assertThat(response1.getBody()).isNotNull();
        assertThat(response1.getBody().amount()).isEqualByComparingTo("350.0000");
        assertThat(response1.getBody().entries()).hasSize(2);

        UUID transactionId = response1.getBody().transactionId();

        // 3. Act: Replay transfer with exact same idempotency key -> 200 OK with cached response
        ResponseEntity<TransferResult> response2 = restTemplate.exchange(
                "/api/v1/transfers", HttpMethod.POST, entity, TransferResult.class
        );

        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response2.getHeaders().getFirst("Idempotency-Replay")).isEqualTo("true");
        assertThat(response2.getBody()).isNotNull();
        assertThat(response2.getBody().transactionId()).isEqualTo(transactionId);
        assertThat(response2.getBody().isIdempotentReplay()).isTrue();

        // 4. Verify balances via REST API
        ResponseEntity<AccountBalanceDTO> senderBalance = restTemplate.getForEntity(
                "/api/v1/accounts/" + senderId + "/balance", AccountBalanceDTO.class
        );
        assertThat(senderBalance.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(senderBalance.getBody()).isNotNull();
        // 2000 - 350 = 1650.0000
        assertThat(senderBalance.getBody().availableBalance()).isEqualByComparingTo("1650.0000");

        ResponseEntity<AccountBalanceDTO> receiverBalance = restTemplate.getForEntity(
                "/api/v1/accounts/" + receiverId + "/balance", AccountBalanceDTO.class
        );
        assertThat(receiverBalance.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(receiverBalance.getBody()).isNotNull();
        // 500 + 350 = 850.0000
        assertThat(receiverBalance.getBody().availableBalance()).isEqualByComparingTo("850.0000");

        // 5. Verify transaction journal via REST API
        ResponseEntity<TransferResult> txAudit = restTemplate.getForEntity(
                "/api/v1/transactions/" + transactionId, TransferResult.class
        );
        assertThat(txAudit.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(txAudit.getBody()).isNotNull();
        assertThat(txAudit.getBody().entries()).hasSize(2);

        // 6. Negative test: Attempt transfer exceeding balance -> 422 Unprocessable Entity
        InitiateTransferRequest overdraftRequest = new InitiateTransferRequest(
                senderId, receiverId, new BigDecimal("999999.00"), "BRL", "Attempted overdraft"
        );
        HttpEntity<InitiateTransferRequest> overdraftEntity = new HttpEntity<>(overdraftRequest, headers);

        ResponseEntity<ProblemDetail> errorResponse = restTemplate.exchange(
                "/api/v1/transfers", HttpMethod.POST, overdraftEntity, ProblemDetail.class
        );

        assertThat(errorResponse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(errorResponse.getBody()).isNotNull();
        assertThat(errorResponse.getBody().getTitle()).isEqualTo("Insufficient Funds");
    }
}
