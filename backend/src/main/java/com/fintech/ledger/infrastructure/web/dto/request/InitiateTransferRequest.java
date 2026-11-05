package com.fintech.ledger.infrastructure.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * HTTP Request payload for initiating an instant transfer settlement.
 */
@Schema(description = "Request body to initiate a real-time instant payment transfer")
public record InitiateTransferRequest(

    @NotNull(message = "sourceAccountId is mandatory")
    @Schema(description = "UUID of the debiting account", example = "a0000000-0000-0000-0000-000000000003")
    UUID sourceAccountId,

    @NotNull(message = "destinationAccountId is mandatory")
    @Schema(description = "UUID of the crediting account", example = "a0000000-0000-0000-0000-000000000004")
    UUID destinationAccountId,

    @NotNull(message = "amount is mandatory")
    @DecimalMin(value = "0.01", message = "Amount must be strictly greater than 0.00")
    @Schema(description = "Transfer amount in major currency units", example = "150.00")
    BigDecimal amount,

    @NotBlank(message = "currency is mandatory")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter ISO-4217 code (e.g. BRL, USD)")
    @Schema(description = "ISO-4217 currency code", example = "BRL")
    String currency,

    @Size(max = 255, message = "Description must not exceed 255 characters")
    @Schema(description = "Optional transaction memo or description", example = "Instant Settlement P2P")
    String description
) {
}
