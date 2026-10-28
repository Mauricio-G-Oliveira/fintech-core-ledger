import { ArrowLeftRight, Terminal } from 'lucide-react';
import { SystemMetrics } from '../types/ledger';

interface HeaderProps {
  metrics: SystemMetrics;
  onOpenArchModal: () => void;
}

export const Header: React.FC<HeaderProps> = ({ metrics, onOpenArchModal }) => {
  return (
    <header className="border-b border-slate-800 bg-slate-900/80 backdrop-blur sticky top-0 z-40">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3.5 flex items-center justify-between">
        {/* Brand */}
        <div className="flex items-center space-x-3.5">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-emerald-600 via-teal-500 to-cyan-400 p-0.5 shadow-lg shadow-emerald-500/20">
            <div className="w-full h-full bg-slate-950 rounded-[10px] flex items-center justify-center">
              <ArrowLeftRight className="w-5 h-5 text-emerald-400" />
            </div>
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-base sm:text-lg font-bold tracking-tight text-white flex items-center gap-2">
                Fintech Core Ledger
                <span className="text-xs px-2 py-0.5 rounded-full font-mono font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/30">
                  ENGINE v1.0
                </span>
              </h1>
            </div>
            <p className="text-xs text-slate-400 hidden sm:block">
              Double-Entry Bookkeeping • Instant Settlement • Idempotent &amp; Concurrency-Safe
            </p>
          </div>
        </div>

        {/* Live System Status & Architecture Doc Button */}
        <div className="flex items-center space-x-3">
          {/* Double-Entry Invariant Status Badge */}
          <div className={`flex items-center space-x-2 px-3 py-1.5 rounded-lg border text-xs font-mono ${
            metrics.isBalanced 
              ? 'bg-emerald-950/40 border-emerald-500/40 text-emerald-300' 
              : 'bg-red-950/40 border-red-500/40 text-red-300 animate-pulse'
          }`}>
            <span className={`w-2 h-2 rounded-full ${metrics.isBalanced ? 'bg-emerald-400 animate-ping' : 'bg-red-500'}`} />
            <span className="font-semibold">{metrics.isBalanced ? 'LEDGER INVARIANT: BALANCED' : 'INVARIANT CORRUPTED'}</span>
          </div>

          {/* Architecture Details Button */}
          <button
            onClick={onOpenArchModal}
            className="flex items-center space-x-2 px-3.5 py-1.5 text-xs font-medium text-slate-200 bg-slate-800 hover:bg-slate-700 active:bg-slate-850 border border-slate-700 rounded-lg transition-all shadow-sm"
          >
            <Terminal className="w-3.5 h-3.5 text-cyan-400" />
            <span className="hidden md:inline">Arch Specs</span>
          </button>
        </div>
      </div>
    </header>
  );
};
