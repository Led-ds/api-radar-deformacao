-- Radar++ / Ciclo 6.2: registro persistente dos jobs HyP3
-- Não altera AOIs nem os registros territoriais existentes.
CREATE TABLE IF NOT EXISTS radar.insar_jobs (
    job_id VARCHAR(160) PRIMARY KEY,
    idempotency_key VARCHAR(128),
    pair_id TEXT NOT NULL,
    aoi_id VARCHAR(128) NOT NULL,
    job_type VARCHAR(80) NOT NULL,
    status VARCHAR(32) NOT NULL,
    job_payload TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_insar_jobs_idempotency
    ON radar.insar_jobs (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_insar_jobs_aoi_updated
    ON radar.insar_jobs (aoi_id, updated_at DESC);
