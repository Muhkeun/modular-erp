-- Align generated business document numbers with multi-tenant uniqueness.
-- Existing schemas may come from the baseline constraint names or PostgreSQL's default *_key names.

ALTER TABLE purchase_requests DROP CONSTRAINT IF EXISTS uq_pr_document_no;
ALTER TABLE purchase_requests DROP CONSTRAINT IF EXISTS purchase_requests_document_no_key;

ALTER TABLE purchase_orders DROP CONSTRAINT IF EXISTS uq_po_document_no;
ALTER TABLE purchase_orders DROP CONSTRAINT IF EXISTS purchase_orders_document_no_key;

ALTER TABLE rfqs DROP CONSTRAINT IF EXISTS uq_rfq_document_no;
ALTER TABLE rfqs DROP CONSTRAINT IF EXISTS rfqs_document_no_key;

ALTER TABLE sales_orders DROP CONSTRAINT IF EXISTS uq_so_document_no;
ALTER TABLE sales_orders DROP CONSTRAINT IF EXISTS sales_orders_document_no_key;

ALTER TABLE goods_receipts DROP CONSTRAINT IF EXISTS uq_gr_document_no;
ALTER TABLE goods_receipts DROP CONSTRAINT IF EXISTS goods_receipts_document_no_key;

ALTER TABLE goods_issues DROP CONSTRAINT IF EXISTS uq_gi_document_no;
ALTER TABLE goods_issues DROP CONSTRAINT IF EXISTS goods_issues_document_no_key;

ALTER TABLE work_orders DROP CONSTRAINT IF EXISTS uq_wo_document_no;
ALTER TABLE work_orders DROP CONSTRAINT IF EXISTS work_orders_document_no_key;

ALTER TABLE journal_entries DROP CONSTRAINT IF EXISTS uq_je_document_no;
ALTER TABLE journal_entries DROP CONSTRAINT IF EXISTS journal_entries_document_no_key;

ALTER TABLE quality_inspections DROP CONSTRAINT IF EXISTS uq_qi_document_no;
ALTER TABLE quality_inspections DROP CONSTRAINT IF EXISTS quality_inspections_document_no_key;

ALTER TABLE contracts DROP CONSTRAINT IF EXISTS uq_contract_document_no;
ALTER TABLE contracts DROP CONSTRAINT IF EXISTS contracts_document_no_key;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_pr_tenant_document_no') THEN
        ALTER TABLE purchase_requests ADD CONSTRAINT uq_pr_tenant_document_no UNIQUE (tenant_id, document_no);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_po_tenant_document_no') THEN
        ALTER TABLE purchase_orders ADD CONSTRAINT uq_po_tenant_document_no UNIQUE (tenant_id, document_no);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_rfq_tenant_document_no') THEN
        ALTER TABLE rfqs ADD CONSTRAINT uq_rfq_tenant_document_no UNIQUE (tenant_id, document_no);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_so_tenant_document_no') THEN
        ALTER TABLE sales_orders ADD CONSTRAINT uq_so_tenant_document_no UNIQUE (tenant_id, document_no);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_gr_tenant_document_no') THEN
        ALTER TABLE goods_receipts ADD CONSTRAINT uq_gr_tenant_document_no UNIQUE (tenant_id, document_no);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_gi_tenant_document_no') THEN
        ALTER TABLE goods_issues ADD CONSTRAINT uq_gi_tenant_document_no UNIQUE (tenant_id, document_no);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_wo_tenant_document_no') THEN
        ALTER TABLE work_orders ADD CONSTRAINT uq_wo_tenant_document_no UNIQUE (tenant_id, document_no);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_je_tenant_document_no') THEN
        ALTER TABLE journal_entries ADD CONSTRAINT uq_je_tenant_document_no UNIQUE (tenant_id, document_no);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_qi_tenant_document_no') THEN
        ALTER TABLE quality_inspections ADD CONSTRAINT uq_qi_tenant_document_no UNIQUE (tenant_id, document_no);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_contract_tenant_document_no') THEN
        ALTER TABLE contracts ADD CONSTRAINT uq_contract_tenant_document_no UNIQUE (tenant_id, document_no);
    END IF;
END $$;
