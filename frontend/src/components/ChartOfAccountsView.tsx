import React from 'react';
import { Landmark, Layers, ArrowUpRight, ArrowDownLeft } from 'lucide-react';
import { Account, AccountType } from '../types/ledger';

interface Props {
  accounts: Account[];
}

export const ChartOfAccountsView: React.FC<Props> = ({ accounts }) => {
  const formatMoney = (val: number) =>
    new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val);

  const getTypeBadge = (type: AccountType) => {
    switch (type) {
      case 'ASSET':
        return <span className="px-2 py-0.5 rounded text-[10px] font-mono font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">ASSET (ATIVO)</span>;
      case 'LIABILITY':
        return <span className="px-2 py-0.5 rounded text-[10px] font-mono font-medium bg-cyan-500/10 text-cyan-400 border border-cyan-500/30">LIABILITY (PASSIVO)</span>;
      case 'REVENUE':
        return <span className="px-2 py-0.5 rounded text-[10px] font-mono font-medium bg-purple-500/10 text-purple-400 border border-purple-500/30">REVENUE (RECEITA)</span>;
      case 'EQUITY':
        return <span className="px-2 py-0.5 rounded text-[10px] font-mono font-medium bg-amber-500/10 text-amber-400 border border-amber-500/30">EQUITY (P. LÍQUIDO)</span>;
      default:
        return <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-slate-800 text-slate-300">{type}</span>;
    }
  };

  return (
    <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-lg">
      <div className="flex items-center justify-between pb-4 border-b border-slate-800">
        <div>
          <h2 className="text-base font-semibold text-white flex items-center gap-2">
            <Landmark className="w-4 h-4 text-cyan-400" />
            Chart of Accounts (Plano de Contas)
          </h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Normalized ledger balance tree with optimistic lock versioning.
          </p>
        </div>
        <span className="text-xs font-mono text-slate-400 flex items-center gap-1">
          <Layers className="w-3.5 h-3.5" />
          {accounts.length} Active Ledgers
        </span>
      </div>

      <div className="mt-4 divide-y divide-slate-800/80">
        {accounts.map((acc) => (
          <div key={acc.id} className="py-3 flex items-center justify-between hover:bg-slate-850/50 px-2 rounded-lg transition">
            <div className="flex items-start space-x-3">
              <div className="mt-0.5">
                {acc.type === 'ASSET' ? (
                  <div className="w-7 h-7 rounded-lg bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center">
                    <ArrowDownLeft className="w-4 h-4 text-emerald-400" />
                  </div>
                ) : (
                  <div className="w-7 h-7 rounded-lg bg-cyan-500/10 border border-cyan-500/20 flex items-center justify-center">
                    <ArrowUpRight className="w-4 h-4 text-cyan-400" />
                  </div>
                )}
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <span className="text-xs font-semibold text-white">{acc.name}</span>
                  {getTypeBadge(acc.type)}
                </div>
                <div className="flex items-center gap-2 mt-0.5 text-[11px] text-slate-400 font-mono">
                  <span>Code: {acc.code}</span>
                  <span>•</span>
                  <span className="text-slate-500">Lock Ver: #{acc.version}</span>
                </div>
              </div>
            </div>

            <div className="text-right">
              <div className="text-sm font-bold font-mono text-white">
                {formatMoney(acc.balance)}
              </div>
              <div className="text-[10px] text-emerald-400 font-mono">
                {acc.status}
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
