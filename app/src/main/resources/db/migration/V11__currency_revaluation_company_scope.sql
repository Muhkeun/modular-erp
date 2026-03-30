ALTER TABLE currency_revaluations
    ADD COLUMN IF NOT EXISTS company_code VARCHAR(20);

CREATE INDEX IF NOT EXISTS idx_currency_revaluations_company
    ON currency_revaluations(tenant_id, company_code);
