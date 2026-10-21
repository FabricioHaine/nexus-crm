-- =============================================================================
-- Nexus CRM - tokens de renovacao de sessao (PostgreSQL)
-- =============================================================================

CREATE TABLE tokens_refresh (
    id           BIGSERIAL    PRIMARY KEY,
    usuario_id   BIGINT       NOT NULL,
    token_hash   VARCHAR(64)  NOT NULL,
    expira_em    TIMESTAMP    NOT NULL,
    revogado    BOOLEAN      NOT NULL DEFAULT FALSE,
    data_criacao TIMESTAMP    NOT NULL,
    CONSTRAINT uk_tokens_refresh_hash UNIQUE (token_hash),
    CONSTRAINT fk_tokens_refresh_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE
);

CREATE INDEX idx_tokens_refresh_usuario ON tokens_refresh (usuario_id);
CREATE INDEX idx_tokens_refresh_expira ON tokens_refresh (expira_em);
