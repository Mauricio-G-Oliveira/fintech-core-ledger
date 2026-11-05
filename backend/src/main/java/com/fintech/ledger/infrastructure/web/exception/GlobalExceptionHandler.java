package com.fintech.ledger.infrastructure.web.exception;

import com.fintech.ledger.application.exception.AccountNotFoundException;
import com.fintech.ledger.application.exception.IdempotencyConflictException;
import com.fintech.ledger.application.exception.TransactionNotFoundException;
import com.fintech.ledger.domain.exception.InactiveAccountException;
import com.fintech.ledger.domain.exception.InsufficientFundsException;
import com.fintech.ledger.domain.exception.InvalidCurrencyException;
import com.fintech.ledger.domain.exception.UnbalancedTransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized REST API Exception Handler producing RFC 7807 ProblemDetail responses.
 * Maps domain, application, and validation exceptions to standardized HTTP error payloads.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InsufficientFundsException.class)
    public ProblemDetail handleInsufficientFunds(InsufficientFundsException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Insufficient Funds");
        problem.setType(URI.create("https://ledger.fintech.internal/errors/insufficient-funds"));
        problem.setProperty("errorCode", "INSUFFICIENT_FUNDS");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InactiveAccountException.class)
    public ProblemDetail handleInactiveAccount(InactiveAccountException ex) {
        log.warn("Inactive account accessed: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Account Inactive or Frozen");
        problem.setType(URI.create("https://ledger.fintech.internal/errors/inactive-account"));
        problem.setProperty("errorCode", "INACTIVE_ACCOUNT");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ProblemDetail handleIdempotencyConflict(IdempotencyConflictException ex) {
        log.warn("Idempotency conflict: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Idempotency Key Conflict");
        problem.setType(URI.create("https://ledger.fintech.internal/errors/idempotency-conflict"));
        problem.setProperty("errorCode", "IDEMPOTENCY_CONFLICT");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler({AccountNotFoundException.class, TransactionNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException ex) {
        log.info("Resource not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource Not Found");
        problem.setType(URI.create("https://ledger.fintech.internal/errors/not-found"));
        problem.setProperty("errorCode", "RESOURCE_NOT_FOUND");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler({UnbalancedTransactionException.class, InvalidCurrencyException.class, IllegalArgumentException.class})
    public ProblemDetail handleBadRequest(RuntimeException ex) {
        log.warn("Invalid ledger command argument: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Ledger Request");
        problem.setType(URI.create("https://ledger.fintech.internal/errors/bad-request"));
        problem.setProperty("errorCode", "BAD_REQUEST");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("Validation failed for incoming HTTP payload: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Payload validation failed");
        problem.setTitle("Validation Error");
        problem.setType(URI.create("https://ledger.fintech.internal/errors/validation-error"));
        problem.setProperty("errorCode", "VALIDATION_FAILED");
        problem.setProperty("timestamp", Instant.now());

        Map<String, String> invalidParams = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            invalidParams.put(error.getField(), error.getDefaultMessage());
        }
        problem.setProperty("invalidParams", invalidParams);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unhandled internal server exception occurred", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal error occurred while processing the financial ledger operation."
        );
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://ledger.fintech.internal/errors/internal-error"));
        problem.setProperty("errorCode", "INTERNAL_SERVER_ERROR");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
