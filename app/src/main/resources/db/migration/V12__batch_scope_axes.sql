ALTER TABLE batch_jobs
    ADD COLUMN company_code VARCHAR(20),
    ADD COLUMN department_code VARCHAR(20),
    ADD COLUMN plant_code VARCHAR(20);

CREATE INDEX idx_batch_jobs_company ON batch_jobs (tenant_id, company_code);
CREATE INDEX idx_batch_jobs_department ON batch_jobs (tenant_id, department_code);
CREATE INDEX idx_batch_jobs_plant ON batch_jobs (tenant_id, plant_code);
