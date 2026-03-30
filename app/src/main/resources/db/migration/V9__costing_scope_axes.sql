ALTER TABLE standard_costs
    ADD COLUMN IF NOT EXISTS cost_center_code VARCHAR(30);

ALTER TABLE product_costs
    ADD COLUMN IF NOT EXISTS cost_center_code VARCHAR(30);

CREATE INDEX IF NOT EXISTS idx_standard_costs_cost_center
    ON standard_costs (tenant_id, cost_center_code);

CREATE INDEX IF NOT EXISTS idx_product_costs_cost_center
    ON product_costs (tenant_id, cost_center_code);
