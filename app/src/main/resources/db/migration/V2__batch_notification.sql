-- =============================================
-- Batch Processing Tables
-- =============================================

CREATE TABLE IF NOT EXISTS batch_jobs (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(50)  NOT NULL,
    job_code        VARCHAR(50)  NOT NULL,
    job_name        VARCHAR(200) NOT NULL,
    job_type        VARCHAR(30)  NOT NULL,
    cron_expression VARCHAR(50),
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    last_run_at     TIMESTAMP,
    next_run_at     TIMESTAMP,
    description     VARCHAR(500),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_batch_jobs_tenant_code ON batch_jobs (tenant_id, job_code);

CREATE TABLE IF NOT EXISTS batch_executions (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         VARCHAR(50)  NOT NULL,
    batch_job_id      BIGINT       NOT NULL REFERENCES batch_jobs (id),
    execution_no      VARCHAR(30)  NOT NULL UNIQUE,
    status            VARCHAR(20)  NOT NULL DEFAULT 'QUEUED',
    started_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    completed_at      TIMESTAMP,
    total_records     INT          NOT NULL DEFAULT 0,
    processed_records INT          NOT NULL DEFAULT 0,
    failed_records    INT          NOT NULL DEFAULT 0,
    error_message     VARCHAR(2000),
    parameters        TEXT,
    result            TEXT,
    triggered_by      VARCHAR(20)  NOT NULL DEFAULT 'USER',
    executed_by       VARCHAR(100),
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_batch_executions_job ON batch_executions (batch_job_id);
CREATE INDEX IF NOT EXISTS idx_batch_executions_tenant ON batch_executions (tenant_id);

-- =============================================
-- Notification Tables
-- =============================================

CREATE TABLE IF NOT EXISTS notification_templates (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     VARCHAR(50)  NOT NULL,
    template_code VARCHAR(50)  NOT NULL,
    template_name VARCHAR(200) NOT NULL,
    channel       VARCHAR(20)  NOT NULL,
    event_type    VARCHAR(50)  NOT NULL,
    subject       VARCHAR(500) NOT NULL,
    body          TEXT         NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    language      VARCHAR(10)  NOT NULL DEFAULT 'ko',
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by    VARCHAR(100),
    updated_by    VARCHAR(100)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_notification_templates_tenant_code ON notification_templates (tenant_id, template_code);

CREATE TABLE IF NOT EXISTS notifications (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       VARCHAR(50)  NOT NULL,
    template_code   VARCHAR(50),
    channel         VARCHAR(20)  NOT NULL,
    recipient_id    VARCHAR(100) NOT NULL,
    recipient_email VARCHAR(200),
    subject         VARCHAR(500) NOT NULL,
    body            TEXT         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    sent_at         TIMESTAMP,
    read_at         TIMESTAMP,
    reference_type  VARCHAR(50),
    reference_id    BIGINT,
    error_message   VARCHAR(2000),
    priority        VARCHAR(10)  NOT NULL DEFAULT 'NORMAL',
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient ON notifications (tenant_id, recipient_id, status);
CREATE INDEX IF NOT EXISTS idx_notifications_reference ON notifications (reference_type, reference_id);

CREATE TABLE IF NOT EXISTS notification_preferences (
    id             BIGSERIAL PRIMARY KEY,
    tenant_id      VARCHAR(50)  NOT NULL,
    user_id        VARCHAR(100) NOT NULL,
    event_type     VARCHAR(50)  NOT NULL,
    channel_in_app BOOLEAN      NOT NULL DEFAULT TRUE,
    channel_email  BOOLEAN      NOT NULL DEFAULT FALSE,
    channel_sms    BOOLEAN      NOT NULL DEFAULT FALSE,
    channel_push   BOOLEAN      NOT NULL DEFAULT FALSE,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by     VARCHAR(100),
    updated_by     VARCHAR(100)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_notification_prefs_user_event ON notification_preferences (tenant_id, user_id, event_type);
