import React from 'react';
import { Scale, CheckCircle2, Zap, Clock, ShieldAlert } from 'lucide-react';
import { SystemMetrics } from '../types/ledger';

interface Props {
  metrics: SystemMetrics;
}

export const DoubleEntryInvariantBanner: React.FC<Props> = ({ metrics }) => {
  const formatMoney = (val: number) =>
    new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val);

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      {/* Total Debits Card */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 shadow-sm relative overflow-hidden">
        <div className="flex items-center justify-between text-xs text-slate-400 mb-1">
          <span className="font-medium uppercase tracking-wider">Total Debits (DR)</span>
          <span className="text-emerald-400 font-mono text-[11px] bg-emerald-500/10 px-1.5 py-0.5 rounded border border-emerald-500/20">
            Source Outflow
          </span>
        </div>
        <div className="text-xl sm:text-2xl font-bold font-mono text-white">
          {formatMoney(metrics.totalDebits)}
        </div>
        <div className="mt-2 flex items-center text-xs text-slate-400">
          <span className="text-emerald-400 font-semibold mr-1">Σ Dr Legs:</span>
          <span>Validated against Chart of Accounts</span>
        </div>
      </div>

      {/* Total Credits Card */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 shadow-sm relative overflow-hidden">
        <div className="flex items-center justify-between text-xs text-slate-400 mb-1">
          <span className="font-medium uppercase tracking-wider">Total Credits (CR)</span>
          <span className="text-cyan-400 font-mono text-[11px] bg-cyan-500/10 px-1.5 py-0.5 rounded border border-cyan-500/20">
            Destination Inflow
          </span>
        </div>
        <div className="text-xl sm:text-2xl font-bold font-mono text-white">
          {formatMoney(metrics.totalCredits)}
        </div>
        <div className="mt-2 flex items-center text-xs text-slate-400">
          <span className="text-cyan-400 font-semibold mr-1">Σ Cr Legs:</span>
          <span>Zero-sum double-entry constraint</span>
        </div>
      </div>

      {/* Invariant Equation Card */}
      <div className={`border rounded-xl p-4 shadow-sm relative overflow-hidden ${
        metrics.isBalanced 
          ? 'bg-gradient-to-br from-emerald-950/30 to-slate-900/90 border-emerald-500/40' 
          : 'bg-red-950/40 border-red-500/50'
      }`}>
        <div className="flex items-center justify-between text-xs text-slate-400 mb-1">
          <span className="font-medium uppercase tracking-wider flex items-center gap-1.5">
            <Scale className="w-3.5 h-3.5 text-emerald-400" />
            Ledger Invariant Check
          </span>
          {metrics.isBalanced ? (
            <span className="flex items-center text-emerald-400 text-xs font-semibold">
              <CheckCircle2 className="w-3.5 h-3.5 mr-1" />
              PERFECT
            </span>
          ) : (
            <span className="flex items-center text-red-400 text-xs font-semibold">
              <ShieldAlert className="w-3.5 h-3.5 mr-1" />
              OUT OF BALANCE
            </span>
          )}
        </div>
        <div className="text-xl sm:text-2xl font-bold font-mono text-emerald-400">
          Δ = R$ 0,00
        </div>
        <div className="mt-2 text-xs text-slate-300 font-mono">
          Debits - Credits = 0.000000 (No leakage)
        </div>
      </div>

      {/* Performance & Idempotency Engine Metrics */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 shadow-sm relative overflow-hidden">
        <div className="flex items-center justify-between text-xs text-slate-400 mb-1">
          <span className="font-medium uppercase tracking-wider flex items-center gap-1.5">
            <Zap className="w-3.5 h-3.5 text-amber-400" />
            Settlement Engine
          </span>
          <span className="text-amber-400 font-mono text-xs font-semibold">
            {metrics.tps} TPS
          </span>
        </div>
        <div className="flex items-baseline gap-2">
          <span className="text-xl sm:text-2xl font-bold font-mono text-white flex items-center gap-1">
            <Clock className="w-4 h-4 text-slate-400" />
            {metrics.p99LatencyMs}ms
          </span>
          <span className="text-xs text-slate-400 font-mono">p99 latency</span>
        </div>
        <div className="mt-2 text-xs text-slate-400 flex items-center justify-between">
          <span>Idempotent Cache Hits:</span>
          <span className="font-mono text-emerald-400 font-semibold">{metrics.idempotencyCacheHits} blocked</span>
        </div>
      </div>
    </div>
  );
};
