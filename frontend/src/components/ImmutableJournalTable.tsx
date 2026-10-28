import React from 'react';
import { BookOpen, Hash, ShieldCheck, CheckCircle2, XCircle, ArrowUpRight, ArrowDownLeft } from 'lucide-react';
import { LedgerTransaction } from '../types/ledger';

interface Props {
  transactions: LedgerTransaction[];
}

export const ImmutableJournalTable: React.FC<Props> = ({ transactions }) => {
  const formatMoney = (val: number) =>
    new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val);

  const getStatusBadge = (status: LedgerTransaction['status']) => {
    switch (status) {
      case 'COMMITTED':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-mono font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
            <CheckCircle2 className="w-3 h-3" />
            COMMITTED (IMMUTABLE)
          </span>
        );
      case 'REJECTED_IDEMPOTENT':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-mono font-medium bg-amber-500/10 text-amber-400 border border-amber-500/30">
            <ShieldCheck className="w-3 h-3" />
            IDEMPOTENT CACHE HIT (409/CACHED)
          </span>
        );
      case 'REJECTED_INSUFFICIENT_FUNDS':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-mono font-medium bg-red-500/10 text-red-400 border border-red-500/30">
            <XCircle className="w-3 h-3" />
            INSUFFICIENT FUNDS
          </span>
        );
      case 'REJECTED_LOCK_TIMEOUT':
        return (
          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-mono font-medium bg-purple-500/10 text-purple-400 border border-purple-500/30">
            <XCircle className="w-3 h-3" />
            LOCK TIMEOUT (DEADLOCK PREVENTED)
          </span>
        );
    }
  };

  return (
    <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-lg">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 border-b border-slate-800 gap-2">
        <div>
          <h2 className="text-base font-semibold text-white flex items-center gap-2">
            <BookOpen className="w-4 h-4 text-emerald-400" />
            Immutable General Ledger Journal (Partidas Dobradas)
          </h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Every entry is strictly append-only; balance mutations are prohibited without counterbalancing legs.
          </p>
        </div>
        <div className="flex items-center gap-2 text-xs font-mono text-slate-400">
          <Hash className="w-3.5 h-3.5 text-cyan-400" />
          <span>{transactions.length} Total Journal Entries</span>
        </div>
      </div>

      <div className="mt-4 overflow-x-auto">
        <table className="w-full text-left text-xs">
          <thead>
            <tr className="border-b border-slate-800 text-slate-400 font-mono text-[11px] uppercase tracking-wider">
              <th className="py-2.5 px-3">Transaction / Hash</th>
              <th className="py-2.5 px-3">Type &amp; Description</th>
              <th className="py-2.5 px-3">Double-Entry Legs (DR / CR)</th>
              <th className="py-2.5 px-3 text-right">Amount</th>
              <th className="py-2.5 px-3 text-center">Status</th>
              <th className="py-2.5 px-3 text-right">Latency</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60 font-sans">
            {transactions.map((tx) => (
              <tr key={tx.id} className="hover:bg-slate-850/40 transition">
                {/* ID & Hash */}
                <td className="py-3 px-3 align-top font-mono">
                  <div className="text-white font-semibold flex items-center gap-1">
                    <span>{tx.id}</span>
                  </div>
                  <div className="text-[10px] text-slate-500 truncate max-w-[140px]" title={tx.hash}>
                    {tx.hash.substring(0, 16)}...
                  </div>
                  <div className="text-[10px] text-slate-400 mt-0.5">
                    {new Date(tx.createdAt).toLocaleTimeString('pt-BR')}
                  </div>
                </td>

                {/* Description & Idempotency Key */}
                <td className="py-3 px-3 align-top max-w-xs">
                  <div className="text-slate-200 font-medium">{tx.description}</div>
                  <div className="text-[10px] text-cyan-400 font-mono mt-0.5 flex items-center gap-1">
                    <span className="text-slate-500">IdemKey:</span>
                    <span className="truncate max-w-[150px]">{tx.idempotencyKey}</span>
                  </div>
                </td>

                {/* Double-Entry Legs */}
                <td className="py-3 px-3 align-top">
                  {tx.entries.length === 0 ? (
                    <span className="text-slate-500 italic">No legs booked (Rejected)</span>
                  ) : (
                    <div className="space-y-1.5 font-mono text-[11px]">
                      {tx.entries.map((entry) => (
                        <div key={entry.id} className="flex items-center justify-between gap-3 bg-slate-950/60 px-2 py-1 rounded border border-slate-800">
                          <div className="flex items-center gap-1.5">
                            {entry.direction === 'DEBIT' ? (
                              <span className="px-1.5 py-0.2 rounded text-[9px] font-bold bg-rose-500/20 text-rose-300 border border-rose-500/30 flex items-center gap-0.5">
                                <ArrowDownLeft className="w-2.5 h-2.5" />
                                DR
                              </span>
                            ) : (
                              <span className="px-1.5 py-0.2 rounded text-[9px] font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 flex items-center gap-0.5">
                                <ArrowUpRight className="w-2.5 h-2.5" />
                                CR
                              </span>
                            )}
                            <span className="text-slate-300 truncate max-w-[130px]" title={entry.accountName}>
                              {entry.accountName}
                            </span>
                          </div>
                          <span className={entry.direction === 'DEBIT' ? 'text-rose-400' : 'text-emerald-400'}>
                            {formatMoney(entry.amount)}
                          </span>
                        </div>
                      ))}
                    </div>
                  )}
                </td>

                {/* Total Transaction Amount */}
                <td className="py-3 px-3 align-top text-right font-mono font-bold text-white">
                  {formatMoney(tx.amount)}
                </td>

                {/* Status */}
                <td className="py-3 px-3 align-top text-center">
                  {getStatusBadge(tx.status)}
                </td>

                {/* Latency */}
                <td className="py-3 px-3 align-top text-right font-mono text-slate-400">
                  <span className="text-slate-200">{tx.settlementLatencyMs}</span>ms
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};
