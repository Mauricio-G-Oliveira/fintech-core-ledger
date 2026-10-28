import { X, Database, Shield, Lock, Cpu, Server, FileCode } from 'lucide-react';

interface Props {
  isOpen: boolean;
  onClose: () => void;
}

export const ArchitectureModal: React.FC<Props> = ({ isOpen, onClose }) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in">
      <div className="bg-slate-900 border border-slate-700 rounded-2xl max-w-3xl w-full max-h-[85vh] flex flex-col shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between p-5 border-b border-slate-800 bg-slate-950/50">
          <div className="flex items-center space-x-3">
            <div className="w-8 h-8 rounded-lg bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
              <Cpu className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white">
                Fintech Core Ledger Architecture Specifications
              </h3>
              <p className="text-xs text-slate-400">
                Senior Production Design: Clean Architecture • Hexagonal / DDD • Java 21 &amp; Spring Boot 3
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 overflow-y-auto space-y-6 text-xs text-slate-300">
          {/* Key Principles Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="bg-slate-950/60 p-4 rounded-xl border border-slate-800">
              <div className="flex items-center gap-2 text-emerald-400 font-semibold mb-2 text-sm">
                <Database className="w-4 h-4" />
                1. Ledger Imutável (Append-Only)
              </div>
              <p className="leading-relaxed text-slate-400">
                Nenhum saldo é alterado diretamente por <code className="text-slate-300 font-mono">UPDATE accounts SET balance = ...</code> sem registrar as entradas balanceadas (<code className="text-emerald-400 font-mono">LedgerEntry</code>). Cada transferência cria obrigatoriamente 2 pernas: <strong>DEBIT</strong> na origem e <strong>CREDIT</strong> no destino.
              </p>
            </div>

            <div className="bg-slate-950/60 p-4 rounded-xl border border-slate-800">
              <div className="flex items-center gap-2 text-cyan-400 font-semibold mb-2 text-sm">
                <Lock className="w-4 h-4" />
                2. Deadlock-Free Pessimistic Locking
              </div>
              <p className="leading-relaxed text-slate-400">
                Para transferências concorrentes (A-&gt;B e B-&gt;A ao mesmo tempo), o motor ordena o bloqueio dos registros no banco pelo <code className="text-cyan-300 font-mono">account_id</code> ascendente antes de invocar o <code className="text-slate-300 font-mono">SELECT ... FOR UPDATE</code>, eliminando 100% dos deadlocks em banco de dados.
              </p>
            </div>

            <div className="bg-slate-950/60 p-4 rounded-xl border border-slate-800">
              <div className="flex items-center gap-2 text-amber-400 font-semibold mb-2 text-sm">
                <Shield className="w-4 h-4" />
                3. Idempotência Rigorosa (X-Idempotency-Key)
              </div>
              <p className="leading-relaxed text-slate-400">
                Toda requisição de liquidação exige uma chave UUIDv7 enviada no cabeçalho HTTP. Um filtro interceptor verifica na tabela <code className="text-amber-300 font-mono">idempotency_keys</code> com trava única. Retentativas acidentais retornam o payload idêntico sem executar nova liquidação.
              </p>
            </div>

            <div className="bg-slate-950/60 p-4 rounded-xl border border-slate-800">
              <div className="flex items-center gap-2 text-purple-400 font-semibold mb-2 text-sm">
                <Server className="w-4 h-4" />
                4. Testes com Testcontainers &amp; ArchUnit
              </div>
              <p className="leading-relaxed text-slate-400">
                A suíte de testes de integração sobe contêineres reais do PostgreSQL via <code className="text-purple-300 font-mono">Testcontainers</code> para validar concorrência sob carga, e testes do <code className="text-purple-300 font-mono">ArchUnit</code> garantem que o Domínio permaneça puro sem vazar para a Infraestrutura.
              </p>
            </div>
          </div>

          {/* Clean Architecture Package Structure */}
          <div className="bg-slate-950/80 p-4 rounded-xl border border-slate-800">
            <h4 className="text-white font-semibold mb-2 flex items-center gap-2">
              <FileCode className="w-4 h-4 text-emerald-400" />
              Estrutura de Pastas de Alto Nível (Clean Architecture / DDD)
            </h4>
            <pre className="font-mono text-[11px] text-slate-300 overflow-x-auto p-3 bg-slate-900 rounded-lg border border-slate-800/80">
{`com.fintech.ledger
├── domain
│   ├── model/ (Account, LedgerTransaction, LedgerEntry, Money)
│   ├── repository/ (AccountRepositoryPort, LedgerRepositoryPort)
│   └── service/ (DoubleEntryRuleEngine, InvariantValidator)
├── application
│   ├── usecase/ (InstantTransferUseCase, AccountReconciliationUseCase)
│   └── dto/ (TransferRequest, TransferResponse)
├── infrastructure
│   ├── persistence/ (PostgreSqlAccountRepository, FlywayMigrations)
│   ├── idempotency/ (IdempotencyFilter, RedisLockAdapter)
│   └── concurrency/ (PessimisticLockManager)
└── interfaces
    └── rest/ (TransferController, AccountController, GlobalExceptionHandler)`}
            </pre>
          </div>
        </div>

        {/* Footer */}
        <div className="p-4 bg-slate-950/80 border-t border-slate-800 flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white font-medium text-xs rounded-lg transition"
          >
            Entendido, fechar especificação
          </button>
        </div>
      </div>
    </div>
  );
};
