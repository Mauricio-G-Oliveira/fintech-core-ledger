import React, { useState } from 'react';
import { Header } from './components/Header';
import { DoubleEntryInvariantBanner } from './components/DoubleEntryInvariantBanner';
import { TransferSimulator } from './components/TransferSimulator';
import { ChartOfAccountsView } from './components/ChartOfAccountsView';
import { ImmutableJournalTable } from './components/ImmutableJournalTable';
import { ArchitectureModal } from './components/ArchitectureModal';
import {
  INITIAL_ACCOUNTS,
  INITIAL_TRANSACTIONS,
  computeMetrics,
  generateHash
} from './mock/ledgerEngine';
import {
  Account,
  LedgerTransaction,
  LedgerEntry,
  InstantTransferPayload,
  SystemMetrics
} from './types/ledger';

export const App: React.FC = () => {
  const [accounts, setAccounts] = useState<Account[]>(INITIAL_ACCOUNTS);
  const [transactions, setTransactions] = useState<LedgerTransaction[]>(INITIAL_TRANSACTIONS);
  const [metrics, setMetrics] = useState<SystemMetrics>(() => computeMetrics(INITIAL_TRANSACTIONS));
  const [processedIdempotencyKeys, setProcessedIdempotencyKeys] = useState<Set<string>>(
    new Set(INITIAL_TRANSACTIONS.map((t) => t.idempotencyKey))
  );
  const [isProcessing, setIsProcessing] = useState<boolean>(false);
  const [isArchModalOpen, setIsArchModalOpen] = useState<boolean>(false);

  // Core Settlement Engine Execution Function
  const executeSettlement = (
    payload: InstantTransferPayload,
    _isRaceSimulation: boolean = false
  ): { success: boolean; message: string; tx?: LedgerTransaction } => {
    const { sourceAccountId, destinationAccountId, amount, idempotencyKey, description } = payload;

    // 1. Rigorous Idempotency Guard
    if (processedIdempotencyKeys.has(idempotencyKey)) {
      const duplicateTx: LedgerTransaction = {
        id: `tx-rejected-${Date.now().toString(36)}`,
        idempotencyKey,
        type: 'INSTANT_PAYMENT',
        status: 'REJECTED_IDEMPOTENT',
        amount,
        currency: 'BRL',
        description: `[BLOCKED] Duplicate request detected for idempotency key "${idempotencyKey}"`,
        createdAt: new Date().toISOString(),
        settlementLatencyMs: 4,
        hash: '0x0000000000000000000000000000000000000000000000000000000000000000',
        entries: []
      };

      setTransactions((prev) => [duplicateTx, ...prev]);
      setMetrics((prev) => ({
        ...prev,
        idempotencyCacheHits: prev.idempotencyCacheHits + 1,
        totalTransactions: prev.totalTransactions + 1
      }));

      return {
        success: false,
        message: `IDEMPOTENCY CONFLICT: Key "${idempotencyKey}" was already processed. Duplicate settlement prevented.`,
        tx: duplicateTx
      };
    }

    // 2. Locate Accounts
    const sourceAccount = accounts.find((a) => a.id === sourceAccountId);
    const destAccount = accounts.find((a) => a.id === destinationAccountId);

    if (!sourceAccount || !destAccount) {
      return { success: false, message: 'Source or destination account not found in Chart of Accounts.' };
    }

    // 3. Balance Constraint Check (for Liability/Customer wallets)
    if (sourceAccount.type === 'LIABILITY' && sourceAccount.balance < amount) {
      const rejectedTx: LedgerTransaction = {
        id: `tx-failed-${Date.now().toString(36)}`,
        idempotencyKey,
        type: 'INSTANT_PAYMENT',
        status: 'REJECTED_INSUFFICIENT_FUNDS',
        amount,
        currency: 'BRL',
        description: `[INSUFFICIENT FUNDS] Account ${sourceAccount.code} has balance R$ ${sourceAccount.balance.toFixed(2)}, required R$ ${amount.toFixed(2)}`,
        createdAt: new Date().toISOString(),
        settlementLatencyMs: 14,
        hash: '0x0000000000000000000000000000000000000000000000000000000000000000',
        entries: []
      };
      setTransactions((prev) => [rejectedTx, ...prev]);
      return {
        success: false,
        message: `Settlement rejected: Insufficient available balance on ${sourceAccount.name}.`,
        tx: rejectedTx
      };
    }

    // 4. Calculate Double-Entry Bookkeeping Mutation
    const newTxId = `tx-fin-${Math.floor(1000 + Math.random() * 9000)}`;
    const txHash = generateHash(newTxId + idempotencyKey + amount);

    // DEBIT Leg
    const debitEntry: LedgerEntry = {
      id: `entry-${newTxId}-dr`,
      transactionId: newTxId,
      accountId: sourceAccount.id,
      accountCode: sourceAccount.code,
      accountName: sourceAccount.name,
      direction: 'DEBIT',
      amount,
      currency: 'BRL',
      runningBalance: sourceAccount.balance - amount,
      sequence: 1
    };

    // CREDIT Leg
    const creditEntry: LedgerEntry = {
      id: `entry-${newTxId}-cr`,
      transactionId: newTxId,
      accountId: destAccount.id,
      accountCode: destAccount.code,
      accountName: destAccount.name,
      direction: 'CREDIT',
      amount,
      currency: 'BRL',
      runningBalance: destAccount.balance + amount,
      sequence: 2
    };

    const committedTx: LedgerTransaction = {
      id: newTxId,
      idempotencyKey,
      type: 'INSTANT_PAYMENT',
      status: 'COMMITTED',
      amount,
      currency: 'BRL',
      description,
      createdAt: new Date().toISOString(),
      settlementLatencyMs: Math.floor(15 + Math.random() * 20),
      hash: txHash,
      entries: [debitEntry, creditEntry]
    };

    // 5. Atomic state update
    setAccounts((prevAccounts) =>
      prevAccounts.map((acc) => {
        if (acc.id === sourceAccountId) {
          return {
            ...acc,
            balance: acc.balance - amount,
            version: acc.version + 1,
            updatedAt: new Date().toISOString()
          };
        }
        if (acc.id === destinationAccountId) {
          return {
            ...acc,
            balance: acc.balance + amount,
            version: acc.version + 1,
            updatedAt: new Date().toISOString()
          };
        }
        return acc;
      })
    );

    setTransactions((prev) => [committedTx, ...prev]);
    setProcessedIdempotencyKeys((prev) => new Set(prev).add(idempotencyKey));

    // Recompute metrics
    setMetrics((prev) => {
      const updatedTotalDebits = prev.totalDebits + amount;
      const updatedTotalCredits = prev.totalCredits + amount;
      return {
        ...prev,
        totalDebits: updatedTotalDebits,
        totalCredits: updatedTotalCredits,
        isBalanced: Math.abs(updatedTotalDebits - updatedTotalCredits) < 0.0001,
        totalTransactions: prev.totalTransactions + 1,
        p99LatencyMs: committedTx.settlementLatencyMs
      };
    });

    return {
      success: true,
      message: `Settled R$ ${amount.toFixed(2)} instantly with ID ${newTxId}. Double-entry invariant verified.`,
      tx: committedTx
    };
  };

  // Stress-test simulation: 5 simultaneous calls with the exact same Idempotency Key
  const handleSimulateRaceCondition = (payload: InstantTransferPayload) => {
    setIsProcessing(true);
    // 1st request goes through
    executeSettlement(payload, true);

    // 4 immediate parallel duplicate attempts
    setTimeout(() => {
      for (let i = 2; i <= 5; i++) {
        executeSettlement(payload, true);
      }
      setIsProcessing(false);
    }, 200);
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans selection:bg-emerald-500/30 selection:text-emerald-300">
      <Header
        metrics={metrics}
        onOpenArchModal={() => setIsArchModalOpen(true)}
      />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6 space-y-6">
        {/* Top Invariant & Performance Metrics Banner */}
        <DoubleEntryInvariantBanner metrics={metrics} />

        {/* Dual Column: Transfer Simulator + Chart of Accounts */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          <div className="lg:col-span-7">
            <TransferSimulator
              accounts={accounts}
              onExecuteTransfer={(p) => executeSettlement(p, false)}
              onSimulateRaceCondition={handleSimulateRaceCondition}
              isProcessing={isProcessing}
            />
          </div>

          <div className="lg:col-span-5">
            <ChartOfAccountsView accounts={accounts} />
          </div>
        </div>

        {/* Immutable Journal Table */}
        <ImmutableJournalTable transactions={transactions} />
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-800/80 bg-slate-950 py-5 text-center text-xs text-slate-400">
        <div className="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-3">
          <p>
            Fintech Core Ledger &amp; Instant Payment Engine • Portfolio Architecture Reference
          </p>
          <div className="flex items-center space-x-4 font-mono text-[11px]">
            <span className="text-emerald-400">Java 21</span>
            <span>•</span>
            <span className="text-cyan-400">Spring Boot 3</span>
            <span>•</span>
            <span className="text-amber-400">PostgreSQL 16</span>
            <span>•</span>
            <span className="text-purple-400">Testcontainers</span>
          </div>
        </div>
      </footer>

      {/* Senior Architecture Specification Modal */}
      <ArchitectureModal
        isOpen={isArchModalOpen}
        onClose={() => setIsArchModalOpen(false)}
      />
    </div>
  );
};

export default App;
