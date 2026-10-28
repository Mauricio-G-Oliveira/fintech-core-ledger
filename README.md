# 🏦 Fintech Core Ledger & Instant Payment Engine

> **Motor de Liquidação Financeira de Alta Confiabilidade, Conciliação e Ledger Imutável por Partidas Dobradas (Double-Entry Bookkeeping)**

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot 3](https://img.shields.io/badge/Spring_Boot-3.3.4-brightgreen.svg?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL 16](https://img.shields.io/badge/PostgreSQL-16-blue.svg?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![React 18](https://img.shields.io/badge/React-18-61DAFB.svg?style=flat-square&logo=react)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-5-646CFF.svg?style=flat-square&logo=vite)](https://vitejs.dev/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind-3-38B2AC.svg?style=flat-square&logo=tailwind-css)](https://tailwindcss.com/)
[![CI/CD](https://img.shields.io/badge/GitHub_Actions-Pages_Deploy-2088FF.svg?style=flat-square&logo=github-actions)](.github/workflows/deploy.yml)

---

## 🎯 Visão Geral da Arquitetura

Este projeto foi desenhado no padrão de **Arquitetura Limpa (Clean Architecture / Hexagonal)** e **Domain-Driven Design (DDD)** para simular o núcleo transacional de uma instituição de pagamento autorizada pelo Banco Central (análogo aos subsistemas do **PIX / FedNow / SEPA Instant**).

### Pilares Fundamentais:
1. **Ledger Imutável por Partidas Dobradas (Double-Entry Bookkeeping):**
   - Nenhuma conta tem seu saldo mutado diretamente (`UPDATE accounts SET balance = ...`).
   - Toda transação gera obrigatoriamente um registro pai (`Transaction`) e no mínimo duas pernas atômicas balanceadas (`LedgerEntry` - Débito e Crédito).
   - Invariante Contábil Universal: $\sum \text{Débitos} = \sum \text{Créditos}$ ($\Delta = R\$ 0,00$).
2. **Concorrência Segura e Livre de Deadlocks:**
   - Bloqueio pessimista via `SELECT ... FOR UPDATE` ordenado deterministicamente pelo identificador da conta (`account_id ASC`), eliminando conflitos de deadlock cruzado (A &rarr; B simultâneo com B &rarr; A).
   - Suporte a verificação otimista com `@Version` para cenários de alta vazão de leitura.
3. **Garantia de Idempotência Rigorosa (`X-Idempotency-Key`):**
   - Prevenção ativa contra duplicidade de cobrança/liquidação oriunda de retentativas de rede de gateways ou clientes.
4. **Testes de Integração com Contêineres Reais (Testcontainers):**
   - Validação de isolamento transacional, concorrência real e migrações Flyway sobre instâncias reais de PostgreSQL em Docker.

---

## 📂 Estrutura de Pastas Recomendada

```text
Fintech Core Ledger & Instant Payment Engine/
├── .github/
│   └── workflows/
│       └── deploy.yml              # Pipeline CI/CD GitHub Actions para GitHub Pages
├── docker/
│   ├── postgres/
│   │   └── init.sql                # Extensões UUID, pgcrypto e schema UTC
│   └── pgadmin/
│       └── servers.json            # Auto-registro do banco no pgAdmin
├── docker-compose.yml              # Orquestrador local PostgreSQL 16 + pgAdmin 4
├── backend/
│   ├── pom.xml                     # Maven: Java 21, Spring Boot 3.3.4, Flyway, Testcontainers
│   └── src/
│       ├── main/
│       │   ├── java/com/fintech/ledger/
│       │   │   ├── FintechCoreLedgerApplication.java
│       │   │   ├── domain/         # Núcleo de regras de negócio puras (sem dependências externas)
│       │   │   │   ├── model/      # Account, LedgerTransaction, LedgerEntry, Money
│       │   │   │   ├── repository/ # Ports de repositórios (interfaces)
│       │   │   │   └── service/    # DoubleEntryEngine, InvariantValidator
│       │   │   ├── application/    # Casos de uso e orquestração de serviços
│       │   │   │   ├── usecase/    # InstantTransferUseCase, ReconciliationUseCase
│       │   │   │   └── dto/        # TransferRequest, TransferResponse
│       │   │   ├── infrastructure/ # Adaptadores externos (Postgres, Flyway, Locks)
│       │   │   │   ├── persistence/# Entidades JPA, Spring Data Repositories
│       │   │   │   └── idempotency/# Interceptor HTTP, IdempotencyRecordRepository
│       │   │   └── interfaces/     # Adaptadores de entrada (REST Controllers, GlobalHandler)
│       │   └── resources/
│       │       ├── application.yml
│       │       └── db/migration/
│       │           └── V1__init_schema.sql # DDL versionado do schema do Ledger
│       └── test/                   # Testes unitários, de arquitetura (ArchUnit) e integração (Testcontainers)
└── frontend/
    ├── package.json
    ├── vite.config.ts              # Configurado com base: './' para GitHub Pages
    ├── tsconfig.json
    ├── tailwind.config.js
    ├── index.html
    └── src/
        ├── types/                  # Interfaces TypeScript do Ledger
        ├── mock/                   # Motor in-memory com simulação de estresse
        ├── components/             # Componentes modulares (Simulator, Table, Chart)
        ├── App.tsx
        └── main.tsx
```

---

## 🚀 Como Executar o Projeto

### 1. Subir a Infraestrutura (PostgreSQL + pgAdmin)
```bash
docker-compose up -d
```
- **PostgreSQL 16:** `localhost:5432` (Database: `fintech_ledger`, User: `fintech_admin`, Password: `fintech_secret_change_in_prod`)
- **pgAdmin 4:** `http://localhost:5050` (Login: `admin@fintech.local`, Senha: `admin`)

### 2. Executar o Frontend (React + Vite + Tailwind)
```bash
cd frontend
npm install
npm run dev
```
Acesse: `http://localhost:3000`

### 3. Publicar no GitHub Pages
O projeto já conta com o workflow `.github/workflows/deploy.yml`. Para publicar:
1. Crie seu repositório no GitHub e adicione o remote:
   ```bash
   git remote add origin https://github.com/SEU_USUARIO/SEU_REPOSITORIO.git
   git push -u origin main
   ```
2. No seu repositório do GitHub, vá em **Settings** &rarr; **Pages** &rarr; selecione **Source: GitHub Actions**.
3. O build e deploy acontecerão de forma automatizada e o link da sua aplicação estará no ar!

---

## 📋 Próximos Passos (Roadmap de Implementação)
- [x] **Passo 1 (Concluído):** Arquitetura base, Docker Compose, `pom.xml` enterprise, Frontend interativo com suporte a GitHub Pages.
- [ ] **Passo 2:** Entidades de Domínio puras (`Account`, `Transaction`, `LedgerEntry`), Value Object `Money` e validação do Invariante Contábil.
- [ ] **Passo 3:** Implementação do Caso de Uso de Transferência Instantânea com controle de concorrência livre de deadlocks (`SELECT FOR UPDATE`).
- [ ] **Passo 4:** Interceptor e Persistência de Chaves de Idempotência (`X-Idempotency-Key`).
- [ ] **Passo 5:** Suíte de Testes de Integração com **Testcontainers** simulando 100 requisições simultâneas para prova de carga e concorrência.
