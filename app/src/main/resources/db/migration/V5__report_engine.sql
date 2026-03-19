-- Report Template & Execution tables for the Report/Export engine

CREATE TABLE IF NOT EXISTS report_templates (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(50)  NOT NULL,
    template_code   VARCHAR(50)  NOT NULL,
    template_name   VARCHAR(200) NOT NULL,
    report_type     VARCHAR(20)  NOT NULL DEFAULT 'TABLE',
    output_format   VARCHAR(10)  NOT NULL DEFAULT 'EXCEL',
    module_code     VARCHAR(30)  NOT NULL,
    query_definition TEXT         NOT NULL DEFAULT '{}',
    layout_definition TEXT,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    description     VARCHAR(500),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    UNIQUE (tenant_id, template_code)
);

CREATE INDEX IF NOT EXISTS idx_report_templates_tenant ON report_templates(tenant_id);
CREATE INDEX IF NOT EXISTS idx_report_templates_module ON report_templates(module_code);

CREATE TABLE IF NOT EXISTS report_executions (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(50)  NOT NULL,
    template_id     BIGINT       REFERENCES report_templates(id),
    execution_no    VARCHAR(30)  NOT NULL,
    parameters      TEXT,
    output_format   VARCHAR(10)  NOT NULL DEFAULT 'EXCEL',
    status          VARCHAR(20)  NOT NULL DEFAULT 'QUEUED',
    file_path       VARCHAR(500),
    file_size       BIGINT,
    file_data       BYTEA,
    generated_at    TIMESTAMP,
    error_message   VARCHAR(2000),
    requested_by    VARCHAR(100) NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_report_executions_tenant ON report_executions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_report_executions_requested_by ON report_executions(requested_by);
CREATE INDEX IF NOT EXISTS idx_report_executions_status ON report_executions(status);
