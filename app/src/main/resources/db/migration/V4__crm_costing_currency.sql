-- =============================================
-- V4: CRM, Costing, Multi-Currency modules
-- =============================================

-- ── CRM: Customers ──
CREATE TABLE IF NOT EXISTS crm_customers (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(50) NOT NULL,
    customer_code   VARCHAR(30) NOT NULL,
    customer_name   VARCHAR(200) NOT NULL,
    customer_type   VARCHAR(20) NOT NULL DEFAULT 'CORPORATE',
    industry        VARCHAR(100),
    phone           VARCHAR(30),
    email           VARCHAR(100),
    website         VARCHAR(200),
    address         VARCHAR(500),
    contact_person  VARCHAR(100),
    contact_phone   VARCHAR(30),
    contact_email   VARCHAR(100),
    credit_limit    NUMERIC(19,4) NOT NULL DEFAULT 0,
    payment_term_days INT NOT NULL DEFAULT 30,
    status          VARCHAR(20) NOT NULL DEFAULT 'PROSPECT',
    notes           VARCHAR(1000),
    assigned_to     VARCHAR(100),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_crm_customer_code ON crm_customers(tenant_id, customer_code);

-- ── CRM: Leads ──
CREATE TABLE IF NOT EXISTS crm_leads (
    id                    BIGSERIAL PRIMARY KEY,
    tenant_id             VARCHAR(50) NOT NULL,
    lead_no               VARCHAR(30) NOT NULL,
    company_name          VARCHAR(200),
    contact_name          VARCHAR(100) NOT NULL,
    contact_email         VARCHAR(100),
    contact_phone         VARCHAR(30),
    source                VARCHAR(20) NOT NULL DEFAULT 'OTHER',
    status                VARCHAR(20) NOT NULL DEFAULT 'NEW',
    estimated_value       NUMERIC(19,4),
    assigned_to           VARCHAR(100),
    notes                 VARCHAR(1000),
    converted_customer_id BIGINT,
    converted_at          TIMESTAMP,
    active                BOOLEAN NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_crm_lead_no ON crm_leads(tenant_id, lead_no);

-- ── CRM: Opportunities ──
CREATE TABLE IF NOT EXISTS crm_opportunities (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           VARCHAR(50) NOT NULL,
    opportunity_no      VARCHAR(30) NOT NULL,
    customer_id         BIGINT NOT NULL REFERENCES crm_customers(id),
    title               VARCHAR(200) NOT NULL,
    description         VARCHAR(1000),
    stage               VARCHAR(20) NOT NULL DEFAULT 'PROSPECTING',
    probability         INT NOT NULL DEFAULT 0,
    expected_amount     NUMERIC(19,4) NOT NULL DEFAULT 0,
    expected_close_date DATE,
    actual_amount       NUMERIC(19,4),
    closed_at           TIMESTAMP,
    lost_reason         VARCHAR(500),
    assigned_to         VARCHAR(100),
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_crm_opportunity_no ON crm_opportunities(tenant_id, opportunity_no);

-- ── CRM: Activities ──
CREATE TABLE IF NOT EXISTS crm_activities (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(50) NOT NULL,
    activity_type   VARCHAR(20) NOT NULL DEFAULT 'NOTE',
    subject         VARCHAR(200) NOT NULL,
    description     VARCHAR(1000),
    activity_date   TIMESTAMP NOT NULL DEFAULT NOW(),
    due_date        TIMESTAMP,
    completed       BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at    TIMESTAMP,
    reference_type  VARCHAR(30),
    reference_id    BIGINT,
    assigned_to     VARCHAR(100),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

-- ── Costing: Cost Centers ──
CREATE TABLE IF NOT EXISTS cost_centers (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         VARCHAR(50) NOT NULL,
    cost_center_code  VARCHAR(30) NOT NULL,
    cost_center_name  VARCHAR(200) NOT NULL,
    parent_code       VARCHAR(30),
    department_code   VARCHAR(30),
    manager_name      VARCHAR(100),
    status            VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_cost_center_code ON cost_centers(tenant_id, cost_center_code);

-- ── Costing: Standard Costs ──
CREATE TABLE IF NOT EXISTS standard_costs (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(50) NOT NULL,
    item_code       VARCHAR(50) NOT NULL,
    cost_type       VARCHAR(20) NOT NULL DEFAULT 'MATERIAL',
    standard_rate   NUMERIC(19,4) NOT NULL DEFAULT 0,
    effective_from  DATE NOT NULL,
    effective_to    DATE,
    currency        VARCHAR(3) NOT NULL DEFAULT 'KRW',
    notes           VARCHAR(500),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

-- ── Costing: Cost Allocations ──
CREATE TABLE IF NOT EXISTS cost_allocations (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         VARCHAR(50) NOT NULL,
    document_no       VARCHAR(30) NOT NULL,
    allocation_date   DATE NOT NULL,
    from_cost_center  VARCHAR(30) NOT NULL,
    to_cost_center    VARCHAR(30) NOT NULL,
    allocation_type   VARCHAR(20) NOT NULL DEFAULT 'DIRECT',
    amount            NUMERIC(19,4) NOT NULL DEFAULT 0,
    allocation_basis  VARCHAR(200),
    percentage        NUMERIC(5,2),
    description       VARCHAR(500),
    status            VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    fiscal_year       INT NOT NULL,
    period            INT NOT NULL,
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_cost_allocation_no ON cost_allocations(tenant_id, document_no);

-- ── Costing: Product Costs ──
CREATE TABLE IF NOT EXISTS product_costs (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(50) NOT NULL,
    item_code       VARCHAR(50) NOT NULL,
    fiscal_year     INT NOT NULL,
    period          INT NOT NULL,
    material_cost   NUMERIC(19,4) NOT NULL DEFAULT 0,
    labor_cost      NUMERIC(19,4) NOT NULL DEFAULT 0,
    overhead_cost   NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_cost      NUMERIC(19,4) NOT NULL DEFAULT 0,
    unit_cost       NUMERIC(19,4) NOT NULL DEFAULT 0,
    quantity        NUMERIC(15,4) NOT NULL DEFAULT 1,
    currency        VARCHAR(3) NOT NULL DEFAULT 'KRW',
    calculated      BOOLEAN NOT NULL DEFAULT FALSE,
    calculated_at   TIMESTAMP,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

-- ── Currency: Currencies ──
CREATE TABLE IF NOT EXISTS currencies (
    id               BIGSERIAL PRIMARY KEY,
    tenant_id        VARCHAR(50) NOT NULL,
    currency_code    VARCHAR(3) NOT NULL,
    currency_name    VARCHAR(100) NOT NULL,
    symbol           VARCHAR(5) NOT NULL,
    decimal_places   INT NOT NULL DEFAULT 2,
    is_base_currency BOOLEAN NOT NULL DEFAULT FALSE,
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_currency_code ON currencies(tenant_id, currency_code);

-- ── Currency: Exchange Rates ──
CREATE TABLE IF NOT EXISTS exchange_rates (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(50) NOT NULL,
    from_currency   VARCHAR(3) NOT NULL,
    to_currency     VARCHAR(3) NOT NULL,
    rate_date       DATE NOT NULL,
    exchange_rate   NUMERIC(15,6) NOT NULL DEFAULT 1,
    rate_type       VARCHAR(20) NOT NULL DEFAULT 'SPOT',
    source          VARCHAR(50),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

-- ── Currency: Revaluations ──
CREATE TABLE IF NOT EXISTS currency_revaluations (
    id                    BIGSERIAL PRIMARY KEY,
    tenant_id             VARCHAR(50) NOT NULL,
    document_no           VARCHAR(30) NOT NULL,
    revaluation_date      DATE NOT NULL,
    fiscal_year           INT NOT NULL,
    period                INT NOT NULL,
    from_currency         VARCHAR(3) NOT NULL,
    to_currency           VARCHAR(3) NOT NULL,
    original_rate         NUMERIC(15,6) NOT NULL DEFAULT 1,
    revaluation_rate      NUMERIC(15,6) NOT NULL DEFAULT 1,
    unrealized_gain_loss  NUMERIC(19,4) NOT NULL DEFAULT 0,
    status                VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    posted_by             VARCHAR(100),
    posted_at             TIMESTAMP,
    active                BOOLEAN NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_reval_no ON currency_revaluations(tenant_id, document_no);
