# ModularERP

Modern, modular Enterprise Resource Planning framework built with clean architecture principles.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.4 + Kotlin + JPA/Hibernate |
| Frontend | React 19 + TypeScript + Vite + Tailwind CSS |
| Database | PostgreSQL (H2 for dev) |
| Auth | Spring Security + JWT |
| Build | Gradle Kotlin DSL (monorepo) |
| API Docs | OpenAPI 3.0 (Springdoc) |
| Grid | AG Grid Community |

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                        Frontend                          │
│              React + TypeScript + AG Grid                │
└──────────────────────────┬──────────────────────────────┘
                           │ REST API (JSON)
┌──────────────────────────┴──────────────────────────────┐
│                      Spring Boot App                     │
│                                                          │
│  ┌─────────┐ ┌─────────┐ ┌──────────┐ ┌─────────┐     │
│  │ Purchase │ │  Sales  │ │Production│ │Planning │ ... │
│  └────┬─────┘ └────┬────┘ └────┬─────┘ └────┬────┘     │
│       │             │           │             │          │
│  ┌────┴─────────────┴───────────┴─────────────┴────┐    │
│  │              Platform (Shared Kernel)            │    │
│  │   core │ security │ web │ i18n │ messaging      │    │
│  └─────────────────────────────────────────────────┘    │
└──────────────────────────┬──────────────────────────────┘
                           │
                    ┌──────┴──────┐
                    │ PostgreSQL  │
                    └─────────────┘
```

### Module Dependency Rules

- Business modules **never depend on each other directly**
- Cross-module communication via **Port/Adapter** interfaces in `platform:core`
- Monolith: direct method calls → MSA: swap adapter to REST/event client
- Each module is an independent Gradle subproject with its own domain, repository, service, controller, DTO layers

## Modules

### Platform (Shared Kernel)

| Module | Purpose |
|--------|---------|
| `platform:core` | BaseEntity, TenantEntity, DomainEvent, Port interfaces, Value Objects |
| `platform:security` | JWT auth, tenant isolation (Hibernate @Filter), RBAC, User entity |
| `platform:web` | ApiResponse wrapper, GlobalExceptionHandler, OpenAPI config |
| `platform:i18n` | Multi-language translation infrastructure |
| `platform:messaging` | In-process event publisher (→ Kafka for MSA) |

### Business Modules

| Module | Domain Entities | Key Features |
|--------|----------------|--------------|
| `master-data` | Item, Company, Plant, BOM | Multi-level BOM explosion, phantom items, multi-language |
| `approval` | ApprovalRequest, ApprovalStep | Generic workflow engine for any document type |
| `document` | DocumentSequence | Atomic document numbering (SELECT FOR UPDATE) |
| `purchase` | PurchaseRequest, RFQ, PurchaseOrder | PR→RFQ→Bidding→PO flow, PR-to-PO conversion |
| `sales` | SalesOrder | Order→Ship→Complete lifecycle |
| `logistics` | GoodsReceipt, GoodsIssue, StockSummary | GR/GI confirm→stock update, average cost |
| `production` | WorkCenter, Routing, WorkOrder | WO with auto BOM/routing population, shop floor tracking |
| `planning` | MrpRun, ProductionSchedule, CapacityPlan | MRP engine, capacity planning, production scheduling |
| `account` | JournalEntry, AccountMaster | Balanced debit/credit validation, post/reverse |
| `hr` | Employee, Department | Employee master, department hierarchy |
| `quality` | QualityInspection | Incoming/in-process/final inspection with pass/fail |
| `supply-chain` | SupplierEvaluation | Weighted scoring (quality/delivery/price/service) |
| `contract` | Contract | Multi-type contracts (Purchase/Sales/NDA/Framework) |

## Manufacturing Flow (End-to-End)

```
Sales Order (SO)
    │
    ▼
Production Planning
    │
    ▼
MRP Run ──────────────────────────────────┐
    │                                      │
    ├─ Buy items ──► PR ──► PO ──► GR     │
    │                              │       │
    └─ Make items ──► Work Order   │       │
                        │          │       │
                        ▼          ▼       │
                   Material Issue (GI)     │
                        │                  │
                        ▼                  │
                   Shop Floor Operations   │
                   (per routing step)      │
                        │                  │
                        ▼                  │
                   Production Report       │
                   (good qty + scrap)      │
                        │                  │
                        ▼                  │
                   Finished Goods GR ──► Stock
                        │
                        ▼
                   Sales Delivery (GI) ──► Customer
```

## Quick Start

### Prerequisites

- Java 17+
- Node.js 18+
- (Optional) PostgreSQL 15+

### Backend

```bash
# Run with H2 in-memory database (zero config)
./gradlew :app:bootRun

# Run with PostgreSQL
DB_USERNAME=modularerp DB_PASSWORD=modularerp ./gradlew :app:bootRun --args='--spring.profiles.active=postgres'
```

API docs: http://localhost:8080/swagger-ui.html

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:3000

### First Login

Register a user via API:

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"tenantId":"DEFAULT","loginId":"admin","password":"admin123","name":"Admin"}'
```

Then login on the frontend with tenant `DEFAULT`, ID `admin`, password `admin123`.

## Project Structure

```
modular-erp/
├── platform/
│   ├── core/           # BaseEntity, ports, VOs, exceptions
│   ├── security/       # JWT, tenant filter, RBAC
│   ├── web/            # API response, error handler
│   ├── i18n/           # Translation infrastructure
│   └── messaging/      # Event publisher
├── modules/
│   ├── master-data/    # Item, BOM, Company, Plant
│   ├── approval/       # Workflow engine
│   ├── document/       # Doc number generator
│   ├── purchase/       # PR, RFQ, PO
│   ├── sales/          # Sales orders
│   ├── logistics/      # GR, GI, Stock
│   ├── production/     # Work center, routing, work order
│   ├── planning/       # MRP, capacity, scheduling
│   ├── account/        # Journal entries, chart of accounts
│   ├── hr/             # Employees, departments
│   ├── quality/        # Inspections
│   ├── supply-chain/   # Supplier evaluation
│   └── contract/       # Contracts
├── app/                # Spring Boot main application
├── frontend/           # React SPA
├── build.gradle.kts
└── settings.gradle.kts
```

## Design Principles

1. **Multi-tenant** — Shared DB with `tenant_id` column + Hibernate `@Filter`
2. **Multi-language** — Separate `_translations` tables per entity
3. **Port/Adapter** — Cross-module interfaces for MSA readiness
4. **Event-driven** — Domain events for loose coupling (in-process → Kafka)
5. **Soft delete** — `active` flag instead of hard deletes
6. **Audit trail** — `createdAt`, `updatedAt`, `createdBy`, `updatedBy` on all entities
7. **Document numbering** — Atomic sequence generation per type/period

## License

MIT
