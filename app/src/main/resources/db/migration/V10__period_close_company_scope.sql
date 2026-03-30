ALTER TABLE fiscal_periods
    ADD COLUMN IF NOT EXISTS company_code VARCHAR(20);

CREATE INDEX IF NOT EXISTS idx_fiscal_periods_company
    ON fiscal_periods(tenant_id, company_code);

DROP INDEX IF EXISTS idx_fiscal_periods_tenant_year_period;

CREATE UNIQUE INDEX IF NOT EXISTS idx_fiscal_periods_tenant_company_year_period
    ON fiscal_periods(tenant_id, company_code, fiscal_year, period);
