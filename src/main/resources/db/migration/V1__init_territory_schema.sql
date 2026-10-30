-- ============================================================================
-- RADAR++ - MIGRATION V1: FUNDAÇÃO DO MÓDULO TERRITORY E ORGANIZAÇÃO
-- PostGIS 3.4 / PostgreSQL 16
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "postgis";

CREATE SCHEMA IF NOT EXISTS radar;

-- 1. TABELA DE ORGANIZAÇÕES (ISOLAMENTO MULTI-TENANCY LÓGICO)
CREATE TABLE IF NOT EXISTS radar.organizations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    legal_name VARCHAR(255) NOT NULL,
    trade_name VARCHAR(255),
    tax_id VARCHAR(32) NOT NULL UNIQUE,
    subscription_tier VARCHAR(32) NOT NULL DEFAULT 'STANDARD',
    max_allowed_area_km2 NUMERIC(8,2) NOT NULL DEFAULT 50.00,
    max_concurrent_jobs INTEGER NOT NULL DEFAULT 2,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Organização Padrão para Desenvolvimento e Piloto Maceió
INSERT INTO radar.organizations (id, legal_name, trade_name, tax_id, subscription_tier, max_allowed_area_km2, max_concurrent_jobs)
VALUES ('d3b07384-d113-40a2-9721-a53b2169b821', 'Radar++ Defesa Civil Maceió', 'Defesa Civil Maceio', '00.000.000/0001-00', 'PILOT_MACEIO', 50.00, 5)
ON CONFLICT (tax_id) DO NOTHING;

-- 2. TABELA DE ÁREAS DE INTERESSE (GEOMETRIAS VETORIAIS POSTGIS)
CREATE TABLE IF NOT EXISTS radar.areas_of_interest (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES radar.organizations(id) ON DELETE RESTRICT,
    name VARCHAR(150) NOT NULL,
    geometry_wgs84 GEOMETRY(Polygon, 4326) NOT NULL,
    bbox_wgs84 GEOMETRY(Polygon, 4326) NOT NULL,
    metric_area_km2 NUMERIC(10,4) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_aoi_area_positive CHECK (metric_area_km2 > 0),
    CONSTRAINT chk_aoi_valid_geom CHECK (ST_IsValid(geometry_wgs84))
);

-- Índices Espaciais GiST e de Governança
CREATE INDEX IF NOT EXISTS idx_aoi_geom_gist ON radar.areas_of_interest USING GIST (geometry_wgs84);
CREATE INDEX IF NOT EXISTS idx_aoi_bbox_gist ON radar.areas_of_interest USING GIST (bbox_wgs84);
CREATE INDEX IF NOT EXISTS idx_aoi_org_id ON radar.areas_of_interest (organization_id);
