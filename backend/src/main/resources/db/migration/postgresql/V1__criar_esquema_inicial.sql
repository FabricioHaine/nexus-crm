-- =============================================================================
-- Nexus CRM - esquema inicial (PostgreSQL)
-- =============================================================================

CREATE TABLE clientes (
    id               BIGSERIAL      PRIMARY KEY,
    nome             VARCHAR(150)  NOT NULL,
    email            VARCHAR(180)  NOT NULL,
    telefone         VARCHAR(30),
    empresa          VARCHAR(150),
    cargo            VARCHAR(100),
    observacoes      VARCHAR(1000),
    data_cadastro    TIMESTAMP     NOT NULL,
    data_atualizacao TIMESTAMP     NOT NULL,
    CONSTRAINT uk_clientes_email UNIQUE (email)
);

CREATE TABLE oportunidades (
    id                          BIGSERIAL       PRIMARY KEY,
    titulo                      VARCHAR(180)   NOT NULL,
    descricao                   VARCHAR(2000),
    cliente_id                  BIGINT          NOT NULL,
    valor_estimado              DECIMAL(15, 2) NOT NULL,
    etapa                       VARCHAR(30)     NOT NULL,
    probabilidade               INTEGER         NOT NULL DEFAULT 0,
    data_criacao                TIMESTAMP       NOT NULL,
    data_prevista_fechamento    DATE,
    data_atualizacao            TIMESTAMP       NOT NULL,
    data_fechamento             TIMESTAMP,
    motivo_perda                VARCHAR(500),
    CONSTRAINT fk_oportunidades_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT ck_oportunidades_valor CHECK (valor_estimado > 0),
    CONSTRAINT ck_oportunidades_probabilidade CHECK (probabilidade BETWEEN 0 AND 100)
);

CREATE INDEX idx_oportunidades_cliente ON oportunidades (cliente_id);
CREATE INDEX idx_oportunidades_etapa ON oportunidades (etapa);
CREATE INDEX idx_oportunidades_data_criacao ON oportunidades (data_criacao);
