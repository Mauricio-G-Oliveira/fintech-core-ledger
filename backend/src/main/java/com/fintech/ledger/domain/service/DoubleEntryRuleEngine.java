package com.fintech.ledger.domain.service;

import com.fintech.ledger.domain.model.Account;
import com.fintech.ledger.domain.model.EntryDirection;
import com.fintech.ledger.domain.model.LedgerEntry;
import com.fintech.ledger.domain.model.LedgerTransaction;
import com.fintech.ledger.domain.model.Money;
import com.fintech.ledger.domain.model.TransactionType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Domain Service orchestrating double-entry bookkeeping rules.
 * Generates balanced debit and credit journal entries and verifies mathematical equilibrium.
 */
public class DoubleEntryRuleEngine {

    /**
     * Creates a bilateral transfer between two accounts and updates their balances.
     */
    public LedgerTransaction createTransfer(UUID transactionId, String idempotencyKey,
                                           Account sourceAccount, Account destinationAccount,
                                           Money amount, String description, Integer latencyMs) {
        if (sourceAccount.getId().equals(destinationAccount.getId())) {
            throw new IllegalArgumentException("Source and destination accounts must be distinct");
        }

        List<LedgerEntry> entries = new ArrayList<>(2);

        // 1. Debit leg (Source account)
        Money sourceNewBalance = sourceAccount.applyEntry(EntryDirection.DEBIT, amount);
        entries.add(LedgerEntry.of(
            UUID.randomUUID(),
            transactionId,
            sourceAccount.getId(),
            sourceAccount.getCode(),
            sourceAccount.getName(),
            EntryDirection.DEBIT,
            amount,
            sourceNewBalance,
            1
        ));

        // 2. Credit leg (Destination account)
        Money destNewBalance = destinationAccount.applyEntry(EntryDirection.CREDIT, amount);
        entries.add(LedgerEntry.of(
            UUID.randomUUID(),
            transactionId,
            destinationAccount.getId(),
            destinationAccount.getCode(),
            destinationAccount.getName(),
            EntryDirection.CREDIT,
            amount,
            destNewBalance,
            2
        ));

        return LedgerTransaction.createCommitted(
            transactionId,
            idempotencyKey,
            TransactionType.INSTANT_PAYMENT,
            amount,
            description,
            entries,
            latencyMs
        );
    }
}
