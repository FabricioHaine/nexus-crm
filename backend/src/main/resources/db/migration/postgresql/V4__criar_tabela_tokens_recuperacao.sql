-- =============================================================================
-- Nexus CRM - tokens de recuperacao de senha (PostgreSQL)
-- =============================================================================

CREATE TABLE tokens_recuperacao (
    id           BIGSERIAL    PRIMARY KEY,
    usuario_id   BIGINT       NOT NULL,
    token_hash   VARCHAR(64)  NOT NULL,
    expira_em    TIMESTAMP    NOT NULL,
    usado        BOOLEAN      NOT NULL DEFAULT FALSE,
    data_criacao TIMESTAMP    NOT NULL,
    CONSTRAINT uk_tokens_recuperacao_hash UNIQUE (token_hash),
    CONSTRAINT fk_tokens_recuperacao_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE
);

CREATE INDEX idx_tokens_recuperacao_usuario ON tokens_recuperacao (usuario_id);
