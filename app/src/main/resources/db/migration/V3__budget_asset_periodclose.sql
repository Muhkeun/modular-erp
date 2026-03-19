-- =============================================
-- V3: Budget, Asset, Period Close modules
-- =============================================

-- Budget Period
CREATE TABLE budget_periods (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(50)  NOT NULL,
    fiscal_year     INT          NOT NULL,
    period_type     VARCHAR(20)  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    start_date      DATE         NOT NULL,
    end_date        DATE         NOT NULL,
    description     VARCHAR(500),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);
CREATE INDEX idx_budget_periods_tenant ON budget_periods(tenant_id);

-- Budget Item
CREATE TABLE budget_items (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         VARCHAR(50)    NOT NULL,
    budget_period_id  BIGINT         NOT NULL REFERENCES budget_periods(id),
    account_code      VARCHAR(20)    NOT NULL,
    account_name      VARCHAR(200)   NOT NULL,
    department_code   VARCHAR(20),
    plant_code        VARCHAR(20),
    budget_amount     NUMERIC(19,4)  NOT NULL,
    revised_amount    NUMERIC(19,4)  NOT NULL,
    actual_amount     NUMERIC(19,4)  NOT NULL DEFAULT 0,
    currency          VARCHAR(3)     NOT NULL DEFAULT 'KRW',
    notes             VARCHAR(500),
    active            BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP      NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100)
);
CREATE INDEX idx_budget_items_tenant ON budget_items(tenant_id);
CREATE INDEX idx_budget_items_period ON budget_items(budget_period_id);

-- Budget Transfer
CREATE TABLE budget_transfers (
    id                   BIGSERIAL PRIMARY KEY,
    tenant_id            VARCHAR(50)    NOT NULL,
    document_no          VARCHAR(30)    NOT NULL,
    transfer_date        DATE           NOT NULL,
    from_budget_item_id  BIGINT         NOT NULL REFERENCES budget_items(id),
    to_budget_item_id    BIGINT         NOT NULL REFERENCES budget_items(id),
    amount               NUMERIC(19,4)  NOT NULL,
    reason               VARCHAR(500)   NOT NULL,
    status               VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    approved_by          VARCHAR(100),
    approved_at          TIMESTAMP,
    active               BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP      NOT NULL DEFAULT NOW(),
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100)
);
CREATE INDEX idx_budget_transfers_tenant ON budget_transfers(tenant_id);

-- Asset
CREATE TABLE assets (
    id                       BIGSERIAL PRIMARY KEY,
    tenant_id                VARCHAR(50)    NOT NULL,
    asset_no                 VARCHAR(30)    NOT NULL,
    name                     VARCHAR(200)   NOT NULL,
    description              VARCHAR(500),
    category                 VARCHAR(20)    NOT NULL,
    status                   VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    acquisition_date         DATE           NOT NULL,
    acquisition_cost         NUMERIC(19,4)  NOT NULL,
    useful_life_months       INT            NOT NULL,
    depreciation_method      VARCHAR(30)    NOT NULL DEFAULT 'STRAIGHT_LINE',
    salvage_value            NUMERIC(19,4)  NOT NULL DEFAULT 0,
    accumulated_depreciation NUMERIC(19,4)  NOT NULL DEFAULT 0,
    location                 VARCHAR(100),
    department               VARCHAR(20),
    responsible_person       VARCHAR(100),
    serial_number            VARCHAR(100),
    currency                 VARCHAR(3)     NOT NULL DEFAULT 'KRW',
    active                   BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP      NOT NULL DEFAULT NOW(),
    created_by               VARCHAR(100),
    updated_by               VARCHAR(100)
);
CREATE INDEX idx_assets_tenant ON assets(tenant_id);
CREATE UNIQUE INDEX idx_assets_tenant_no ON assets(tenant_id, asset_no);

-- Depreciation Schedule
CREATE TABLE depreciation_schedules (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           VARCHAR(50)    NOT NULL,
    asset_id            BIGINT         NOT NULL REFERENCES assets(id),
    period_year         INT            NOT NULL,
    period_month        INT            NOT NULL,
    depreciation_amount NUMERIC(19,4)  NOT NULL,
    accumulated_amount  NUMERIC(19,4)  NOT NULL,
    book_value_after    NUMERIC(19,4)  NOT NULL,
    posted              BOOLEAN        NOT NULL DEFAULT FALSE,
    journal_entry_id    BIGINT,
    active              BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP      NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100)
);
CREATE INDEX idx_depreciation_schedules_tenant ON depreciation_schedules(tenant_id);
CREATE INDEX idx_depreciation_schedules_asset ON depreciation_schedules(asset_id);

-- Asset Disposal
CREATE TABLE asset_disposals (
    id                    BIGSERIAL PRIMARY KEY,
    tenant_id             VARCHAR(50)    NOT NULL,
    asset_id              BIGINT         NOT NULL REFERENCES assets(id),
    disposal_date         DATE           NOT NULL,
    disposal_type         VARCHAR(20)    NOT NULL,
    disposal_amount       NUMERIC(19,4)  NOT NULL DEFAULT 0,
    book_value_at_disposal NUMERIC(19,4) NOT NULL,
    gain_loss             NUMERIC(19,4)  NOT NULL,
    reason                VARCHAR(500),
    approved_by           VARCHAR(100),
    active                BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP      NOT NULL DEFAULT NOW(),
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100)
);
CREATE INDEX idx_asset_disposals_tenant ON asset_disposals(tenant_id);

-- Fiscal Period
CREATE TABLE fiscal_periods (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   VARCHAR(50)  NOT NULL,
    fiscal_year INT          NOT NULL,
    period      INT          NOT NULL,
    period_name VARCHAR(20)  NOT NULL,
    start_date  DATE         NOT NULL,
    end_date    DATE         NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    closed_by   VARCHAR(100),
    closed_at   TIMESTAMP,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100)
);
CREATE INDEX idx_fiscal_periods_tenant ON fiscal_periods(tenant_id);
CREATE UNIQUE INDEX idx_fiscal_periods_tenant_year_period ON fiscal_periods(tenant_id, fiscal_year, period);

-- Period Close Task
CREATE TABLE period_close_tasks (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        VARCHAR(50)   NOT NULL,
    fiscal_period_id BIGINT        NOT NULL REFERENCES fiscal_periods(id),
    task_type        VARCHAR(40)   NOT NULL,
    task_name        VARCHAR(200)  NOT NULL,
    sequence         INT           NOT NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    executed_by      VARCHAR(100),
    executed_at      TIMESTAMP,
    error_message    VARCHAR(1000),
    notes            VARCHAR(500),
    active           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100)
);
CREATE INDEX idx_period_close_tasks_tenant ON period_close_tasks(tenant_id);
CREATE INDEX idx_period_close_tasks_period ON period_close_tasks(fiscal_period_id);

-- Closing Entry
CREATE TABLE closing_entries (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        VARCHAR(50)    NOT NULL,
    fiscal_period_id BIGINT         NOT NULL REFERENCES fiscal_periods(id),
    document_no      VARCHAR(30)    NOT NULL,
    entry_type       VARCHAR(30)    NOT NULL,
    description      VARCHAR(500)   NOT NULL,
    debit_account    VARCHAR(20)    NOT NULL,
    credit_account   VARCHAR(20)    NOT NULL,
    amount           NUMERIC(19,4)  NOT NULL,
    posted           BOOLEAN        NOT NULL DEFAULT FALSE,
    reversal_date    DATE,
    active           BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100)
);
CREATE INDEX idx_closing_entries_tenant ON closing_entries(tenant_id);
CREATE INDEX idx_closing_entries_period ON closing_entries(fiscal_period_id);
