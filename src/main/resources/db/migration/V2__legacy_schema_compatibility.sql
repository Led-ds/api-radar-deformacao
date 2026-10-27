-- ============================================================================
-- RADAR++ - MIGRATION V2: TABELAS LEGADAS DE COMPATIBILIDADE
-- Preservação do esquema legado (Deformação, Imagem Radar e Análise Resultado)
-- ============================================================================

CREATE TABLE IF NOT EXISTS radar.deformacao (
    id BIGSERIAL PRIMARY KEY,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    deslocamento DOUBLE PRECISION NOT NULL,
    risco VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS radar.imagem_radar (
    id BIGSERIAL PRIMARY KEY,
    origem VARCHAR(255),
    data_coleta DATE,
    dados_brutos OID,
    url_imagem VARCHAR(255),
    processada BOOLEAN NOT NULL DEFAULT FALSE,
    deformacao_id BIGINT REFERENCES radar.deformacao(id)
);

CREATE TABLE IF NOT EXISTS radar.analise_resultado (
    id BIGSERIAL PRIMARY KEY,
    classificacao VARCHAR(255),
    confianca DOUBLE PRECISION NOT NULL,
    imagem_radar_id BIGINT REFERENCES radar.imagem_radar(id)
);
