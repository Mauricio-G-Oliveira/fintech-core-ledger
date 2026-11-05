package com.fintech.ledger.infrastructure.web.controller;

import com.fintech.ledger.application.dto.TransferCommand;
import com.fintech.ledger.application.dto.TransferResult;
import com.fintech.ledger.application.usecase.InstantTransferUseCase;
import com.fintech.ledger.infrastructure.web.dto.request.InitiateTransferRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;

/**
 * REST Controller for executing real-time instant payment transfers.
 * Enforces Double-Entry Bookkeeping invariants, dead-lock free concurrency, and idempotency replay.
 */
@RestController
@RequestMapping("/api/v1/transfers")
@Tag(name = "Instant Transfers", description = "High-throughput, deadlock-free double-entry payment processing")
public class TransferController {

    private final InstantTransferUseCase instantTransferUseCase;

    public TransferController(InstantTransferUseCase instantTransferUseCase) {
        this.instantTransferUseCase = Objects.requireNonNull(instantTransferUseCase, "instantTransferUseCase must not be null");
    }

    @PostMapping
    @Operation(
        summary = "Execute an Instant Payment Transfer",
        description = "Processes a real-time instant settlement between two accounts. " +
                      "Guarantees Debit = Credit double-entry balance, pessimistic ascending-lock concurrency, " +
                      "and safe idempotency retries via 'Idempotency-Key' header."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Transfer executed and committed to immutable ledger",
            headers = {
                @Header(name = "Idempotency-Key", description = "Unique idempotency key used for the transfer", schema = @Schema(type = "string")),
                @Header(name = "Idempotency-Replay", description = "Indicates whether response was replayed from idempotency cache", schema = @Schema(type = "boolean")),
                @Header(name = "X-Cryptographic-Hash", description = "SHA-256 Merkle root hash of transaction header and entries", schema = @Schema(type = "string"))
            },
            content = @Content(schema = @Schema(implementation = TransferResult.class))
        ),
        @ApiResponse(
            responseCode = "200",
            description = "Idempotent replay of previously committed transfer (safe retry)",
            headers = {
                @Header(name = "Idempotency-Key", description = "Unique idempotency key of original request", schema = @Schema(type = "string")),
                @Header(name = "Idempotency-Replay", description = "Always 'true' for cached responses", schema = @Schema(type = "boolean"))
            },
            content = @Content(schema = @Schema(implementation = TransferResult.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request payload or currency mismatch",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Source or destination account was not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Idempotency key reused with mismatched payload hash",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Business rule violation: Insufficient funds or inactive/frozen account",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    public ResponseEntity<TransferResult> executeTransfer(
            @Parameter(description = "Unique client-generated idempotency key (UUID or alphanumeric). If omitted, a server key is auto-generated.")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody InitiateTransferRequest request) {

        String effectiveKey = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey.trim()
                : UUID.randomUUID().toString();

        TransferCommand command = new TransferCommand(
                request.sourceAccountId(),
                request.destinationAccountId(),
                request.amount(),
                request.currency(),
                effectiveKey,
                request.description()
        );

        TransferResult result = instantTransferUseCase.execute(command);

        HttpStatus status = result.isIdempotentReplay() ? HttpStatus.OK : HttpStatus.CREATED;

        return ResponseEntity
                .status(status)
                .location(URI.create("/api/v1/transactions/" + result.transactionId()))
                .header("Idempotency-Key", effectiveKey)
                .header("Idempotency-Replay", String.valueOf(result.isIdempotentReplay()))
                .header("X-Cryptographic-Hash", result.hash())
                .header("X-Settlement-Latency-Ms", String.valueOf(result.latencyMs()))
                .body(result);
    }
}
