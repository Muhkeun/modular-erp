# Architecture Overview

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin (Backend) + TypeScript (Frontend) |
| Framework | Spring Boot 3 + React 19 |
| ORM | Spring Data JPA |
| View | React SPA (Vite) |
| DB | PostgreSQL + H2 (test) |
| Build | Gradle multi-module |
| Auth | JWT + SSO (WIP) |
| i18n | i18next (ko/en) + platform/i18n (WIP) |
| Messaging | InProcessEventPublisher |
| Report | CSV/Excel/PDF export service |
| Test | JUnit5 + Vitest + Playwright |
| UI | TailwindCSS + AG Grid + Lucide Icons |
| State | Zustand (client) + TanStack Query (server) |

## Module Structure

```
modular-erp/
├── app/                    # Spring Boot 진입점, Auth, Dashboard
├── platform/               # 공통 플랫폼 (9 모듈)
│   ├── core/              # BaseEntity, DomainEvent, ValueObject
│   ├── security/          # JWT, SSO, RateLimit, TenantContext
│   ├── i18n/              # Translation (WIP)
│   ├── messaging/         # Event publishing
│   ├── web/               # ApiResponse, ExceptionHandler
│   ├── preference/        # User/Grid preferences
│   ├── admin/             # Roles, Permissions, Tenants, SystemCodes, MenuProfile, DataScope
│   ├── audit/             # AuditLog
│   ├── report/            # CSV/Excel/PDF export
│   └── ai/                # AI chat, embeddings
├── modules/                # 비즈니스 모듈 (20 모듈)
│   ├── master-data/       # Item, BOM, Company
│   ├── purchase/          # PR, PO, RFQ
│   ├── sales/             # SalesOrder
│   ├── logistics/         # GR, GI, Stock
│   ├── production/        # WorkOrder, WorkCenter, Routing
│   ├── planning/          # MRP, Capacity, Schedule
│   ├── account/           # JournalEntry, AccountMaster
│   ├── hr/                # Employee
│   ├── approval/          # ApprovalRequest, WorkflowDefinition
│   ├── notification/      # Notification, Template, Preference
│   ├── document/          # DocumentSequence, NumberGenerator
│   ├── budget/            # BudgetPeriod, BudgetItem
│   ├── asset/             # Asset, Depreciation
│   ├── period-close/      # FiscalPeriod, ClosingEntry
│   ├── batch/             # BatchJob, BatchExecution
│   ├── costing/           # CostCenter, StandardCost
│   ├── crm/               # Customer, Lead, Opportunity
│   ├── currency/          # ExchangeRate, Revaluation
│   ├── contract/          # Contract (WIP)
│   ├── quality/           # Inspection (WIP)
│   └── supply-chain/      # VendorEvaluation (WIP)
└── frontend/               # React 19 + Vite + TailwindCSS
    ├── src/app/           # App.tsx (routes), MainLayout.tsx (nav)
    ├── src/modules/       # 도메인별 pages
    └── src/shared/        # components, hooks, api, i18n
```

## Key Design Decisions
- Multi-tenant by default (TenantContext in platform/security)
- Event-driven via DomainEvent + InProcessEventPublisher
- Feature module = Gradle subproject + frontend src/modules/ directory
- API versioning: `/api/v1/...`
- Frontend state: Zustand (auth, preferences) + TanStack Query (server state)
- Grid: AG Grid with server-side preference persistence

## Changelog
- 2026-03-20: Initial documentation created from codebase analysis
