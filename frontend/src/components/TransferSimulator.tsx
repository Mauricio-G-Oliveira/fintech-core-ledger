import React, { useState } from 'react';
import { Send, RefreshCw, ShieldCheck, Flame, CheckCircle, AlertTriangle } from 'lucide-react';
import { Account, InstantTransferPayload, LedgerTransaction } from '../types/ledger';

interface Props {
  accounts: Account[];
  onExecuteTransfer: (payload: InstantTransferPayload) => { success: boolean; message: string; tx?: LedgerTransaction };
  onSimulateRaceCondition: (payload: InstantTransferPayload) => void;
  isProcessing: boolean;
}

export const TransferSimulator: React.FC<Props> = ({
  accounts,
  onExecuteTransfer,
  onSimulateRaceCondition,
  isProcessing
}) => {
  const [sourceId, setSourceId] = useState<string>(accounts[1]?.id || accounts[0]?.id || '');
  const [destId, setDestId] = useState<string>(accounts[2]?.id || accounts[0]?.id || '');
  const [amount, setAmount] = useState<string>('2500.00');
  const [lockMode, setLockMode] = useState<'PESSIMISTIC_WRITE' | 'OPTIMISTIC'>('PESSIMISTIC_WRITE');
  const [idempotencyKey, setIdempotencyKey] = useState<string>(() => generateUUID());
  const [description, setDescription] = useState<string>('Instant Payment Settlement (PIX / FedNow Style)');
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error' | 'warning'; msg: string } | null>(null);

  function generateUUID() {
    return 'idem-' + Math.random().toString(36).substring(2, 9) + '-' + Date.now().toString(36);
  }

  const handleRegenerateKey = () => {
    setIdempotencyKey(generateUUID());
    setFeedback(null);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (sourceId === destId) {
      setFeedback({ type: 'error', msg: 'Source and destination accounts must be distinct.' });
      return;
    }
    const parsedAmount = parseFloat(amount);
    if (isNaN(parsedAmount) || parsedAmount <= 0) {
      setFeedback({ type: 'error', msg: 'Amount must be a positive number.' });
      return;
    }

    const payload: InstantTransferPayload = {
      sourceAccountId: sourceId,
      destinationAccountId: destId,
      amount: parsedAmount,
      currency: 'BRL',
      idempotencyKey,
      description,
      concurrencyLockMode: lockMode,
    };

    const res = onExecuteTransfer(payload);
    if (res.success) {
      setFeedback({ type: 'success', msg: res.message });
    } else {
      setFeedback({ type: 'error', msg: res.message });
    }
  };

  const handleRaceAttack = () => {
    if (sourceId === destId) {
      setFeedback({ type: 'error', msg: 'Source and destination accounts must be distinct.' });
      return;
    }
    const parsedAmount = parseFloat(amount);
    const payload: InstantTransferPayload = {
      sourceAccountId: sourceId,
      destinationAccountId: destId,
      amount: parsedAmount,
      currency: 'BRL',
      idempotencyKey, // Re-use the same key to prove idempotency under high concurrency!
      description: `[STRESS TEST] Parallel settlement duplicate check`,
      concurrencyLockMode: lockMode,
    };
    onSimulateRaceCondition(payload);
  };

  return (
    <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-lg">
      <div className="flex items-center justify-between pb-4 border-b border-slate-800">
        <div>
          <h2 className="text-base font-semibold text-white flex items-center gap-2">
            <Send className="w-4 h-4 text-emerald-400" />
            Instant Transfer Settlement Simulator
          </h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Atomically debits source, credits destination, and writes immutable journal entries.
          </p>
        </div>
        <span className="text-xs font-mono px-2 py-1 rounded bg-slate-800 text-slate-300 border border-slate-700">
          ACID • ISO-20022 Ready
        </span>
      </div>

      <form onSubmit={handleSubmit} className="mt-4 space-y-4">
        {/* Account Selectors */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-medium text-slate-300 mb-1">
              Source Account (Debit Leg)
            </label>
            <select
              value={sourceId}
              onChange={(e) => setSourceId(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-emerald-500 font-mono"
            >
              {accounts.map((acc) => (
                <option key={acc.id} value={acc.id}>
                  {acc.name} ({acc.code}) - R$ {acc.balance.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-300 mb-1">
              Destination Account (Credit Leg)
            </label>
            <select
              value={destId}
              onChange={(e) => setDestId(e.target.value)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-cyan-500 font-mono"
            >
              {accounts.map((acc) => (
                <option key={acc.id} value={acc.id}>
                  {acc.name} ({acc.code}) - R$ {acc.balance.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Amount & Locking Strategy */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-medium text-slate-300 mb-1">
              Transfer Amount (BRL)
            </label>
            <div className="relative">
              <span className="absolute left-3 top-2 text-xs text-slate-400 font-mono">R$</span>
              <input
                type="number"
                step="0.01"
                min="0.01"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                className="w-full bg-slate-950 border border-slate-700 rounded-lg pl-9 pr-3 py-2 text-xs text-slate-200 font-mono focus:outline-none focus:border-emerald-500"
                placeholder="100.00"
                required
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-300 mb-1 flex items-center justify-between">
              <span>Concurrency Lock Strategy</span>
              <span className="text-[10px] text-emerald-400">Deadlock-Free</span>
            </label>
            <select
              value={lockMode}
              onChange={(e) => setLockMode(e.target.value as any)}
              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-emerald-500 font-mono"
            >
              <option value="PESSIMISTIC_WRITE">
                PESSIMISTIC_WRITE (SELECT FOR UPDATE sorted by Account ID)
              </option>
              <option value="OPTIMISTIC">
                OPTIMISTIC (@Version Hibernate Concurrency Check)
              </option>
            </select>
          </div>
        </div>

        {/* Idempotency Key with Regenerate button */}
        <div>
          <label className="block text-xs font-medium text-slate-300 mb-1 flex items-center justify-between">
            <span className="flex items-center gap-1.5">
              <ShieldCheck className="w-3.5 h-3.5 text-cyan-400" />
              Header: <code className="text-cyan-300 font-mono">X-Idempotency-Key</code>
            </span>
            <span className="text-[11px] text-slate-400">Guarantees exactly-once execution</span>
          </label>
          <div className="flex gap-2">
            <input
              type="text"
              value={idempotencyKey}
              onChange={(e) => setIdempotencyKey(e.target.value)}
              className="flex-1 bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-slate-200 font-mono focus:outline-none focus:border-cyan-500"
              required
            />
            <button
              type="button"
              onClick={handleRegenerateKey}
              title="Generate new UUIDv7 Idempotency Key"
              className="px-3 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg border border-slate-700 transition flex items-center gap-1 text-xs"
            >
              <RefreshCw className="w-3.5 h-3.5" />
              <span>New</span>
            </button>
          </div>
        </div>

        {/* Description */}
        <div>
          <label className="block text-xs font-medium text-slate-300 mb-1">
            Transaction Narrative / Memo
          </label>
          <input
            type="text"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-emerald-500"
          />
        </div>

        {/* Feedback Alert */}
        {feedback && (
          <div className={`p-3 rounded-lg text-xs flex items-center gap-2 ${
            feedback.type === 'success'
              ? 'bg-emerald-950/40 border border-emerald-500/40 text-emerald-300'
              : 'bg-red-950/40 border border-red-500/40 text-red-300'
          }`}>
            {feedback.type === 'success' ? <CheckCircle className="w-4 h-4 shrink-0" /> : <AlertTriangle className="w-4 h-4 shrink-0" />}
            <span>{feedback.msg}</span>
          </div>
        )}

        {/* Actions */}
        <div className="pt-2 flex flex-col sm:flex-row gap-3">
          <button
            type="submit"
            disabled={isProcessing}
            className="flex-1 flex items-center justify-center space-x-2 py-2.5 px-4 bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white font-medium text-xs rounded-lg shadow-md shadow-emerald-900/30 transition disabled:opacity-50"
          >
            <Send className="w-3.5 h-3.5" />
            <span>{isProcessing ? 'Settling Transaction...' : 'Settle Instant Transfer (Single Call)'}</span>
          </button>

          <button
            type="button"
            onClick={handleRaceAttack}
            disabled={isProcessing}
            title="Simulate 5 concurrent requests with identical Idempotency Key"
            className="flex items-center justify-center space-x-1.5 py-2.5 px-4 bg-amber-600/20 hover:bg-amber-600/30 border border-amber-500/40 text-amber-300 font-medium text-xs rounded-lg transition disabled:opacity-50"
          >
            <Flame className="w-3.5 h-3.5 text-amber-400" />
            <span>Simulate Race Condition (5 Concurrent)</span>
          </button>
        </div>
      </form>
    </div>
  );
};
