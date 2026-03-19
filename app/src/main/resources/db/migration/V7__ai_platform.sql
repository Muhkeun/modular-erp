-- AI Platform tables

CREATE TABLE ai_conversations (
    id              BIGSERIAL PRIMARY KEY,
    session_id      VARCHAR(64)  NOT NULL UNIQUE,
    user_id         VARCHAR(100) NOT NULL,
    title           VARCHAR(200),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    message_count   INT          NOT NULL DEFAULT 0,
    last_message_at TIMESTAMP,
    tenant_id       VARCHAR(50)  NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

CREATE INDEX idx_ai_conv_user    ON ai_conversations (user_id);
CREATE INDEX idx_ai_conv_tenant  ON ai_conversations (tenant_id);
CREATE INDEX idx_ai_conv_session ON ai_conversations (session_id);

CREATE TABLE ai_messages (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT       NOT NULL REFERENCES ai_conversations(id),
    role            VARCHAR(20)  NOT NULL,
    content         TEXT         NOT NULL,
    token_count     INT,
    metadata        TEXT,
    message_created_at TIMESTAMP  NOT NULL DEFAULT NOW(),
    tenant_id       VARCHAR(50)  NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

CREATE INDEX idx_ai_msg_conv ON ai_messages (conversation_id);

CREATE TABLE embedding_documents (
    id               BIGSERIAL PRIMARY KEY,
    source_type      VARCHAR(50)  NOT NULL,
    source_id        VARCHAR(100) NOT NULL,
    content          TEXT         NOT NULL,
    chunk_index      INT          NOT NULL DEFAULT 0,
    embedding_vector TEXT,
    metadata         TEXT,
    last_synced_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    tenant_id        VARCHAR(50)  NOT NULL,
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100)
);

CREATE INDEX idx_embedding_source ON embedding_documents (source_type, source_id);
CREATE INDEX idx_embedding_tenant ON embedding_documents (tenant_id);
CREATE INDEX idx_embedding_content ON embedding_documents USING gin (to_tsvector('simple', content));
