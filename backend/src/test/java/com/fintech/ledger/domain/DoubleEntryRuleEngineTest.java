package com.fintech.ledger.domain;

import com.fintech.ledger.domain.model.Account;
import com.fintech.ledger.domain.model.AccountType;
import com.fintech.ledger.domain.model.LedgerTransaction;
import com.fintech.ledger.domain.model.Money;
import com.fintech.ledger.domain.model.TransactionStatus;
import com.fintech.ledger.domain.service.DoubleEntryRuleEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit Tests: DoubleEntryRuleEngine Domain Service")
class DoubleEntryRuleEngineTest {

    private final DoubleEntryRuleEngine engine = new DoubleEntryRuleEngine();

    @Test
    @DisplayName("Should execute bilateral transfer and mutate both accounts atomically according to double-entry rules")
    void shouldExecuteBilateralTransfer() {
        Account sender = Account.create("2.1.01.001", "Client A Wallet", AccountType.LIABILITY, Money.brl(1000.00));
        Account receiver = Account.create("2.1.01.002", "Client B Wallet", AccountType.LIABILITY, Money.brl(200.00));

        UUID txId = UUID.randomUUID();
        Money transferAmount = Money.brl(350.00);

        LedgerTransaction tx = engine.createTransfer(
            txId, "idem-pix-9988", sender, receiver, transferAmount, "Instant PIX Transfer", 18
        );

        assertNotNull(tx);
        assertEquals(TransactionStatus.COMMITTED, tx.getStatus());
        assertEquals(transferAmount, tx.getAmount());

        // Sender liability decreased by 350 -> 650.00
        assertEquals(Money.brl(650.00), sender.getBalance());

        // Receiver liability increased by 350 -> 550.00
        assertEquals(Money.brl(550.00), receiver.getBalance());

        // Exactly 2 legs: one debit, one credit
        assertEquals(2, tx.getEntries().size());
        assertEquals(Money.brl(350.00), tx.getEntries().get(0).amount());
        assertEquals(Money.brl(350.00), tx.getEntries().get(1).amount());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when source and destination accounts are identical")
    void shouldThrowWhenAccountsAreSame() {
        Account acc = Account.create("2.1.01.001", "Client Wallet", AccountType.LIABILITY, Money.brl(1000.00));

        assertThrows(
            IllegalArgumentException.class,
            () -> engine.createTransfer(UUID.randomUUID(), "idem-err", acc, acc, Money.brl(50.00), "Self transfer", 10)
        );
    }
}
