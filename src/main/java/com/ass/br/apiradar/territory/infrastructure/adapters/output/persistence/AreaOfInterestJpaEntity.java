package com.ass.br.apiradar.territory.infrastructure.adapters.output.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.Polygon;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidade JPA mapeada para a tabela PostGIS radar.areas_of_interest.
 */
@Entity
@Table(name = "areas_of_interest", schema = "radar")
public class AreaOfInterestJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "geometry_wgs84", columnDefinition = "geometry(Polygon,4326)", nullable = false)
    private Polygon geometryWgs84;

    @Column(name = "bbox_wgs84", columnDefinition = "geometry(Polygon,4326)", nullable = false)
    private Polygon bboxWgs84;

    @Column(name = "metric_area_km2", nullable = false, precision = 10, scale = 4)
    private BigDecimal metricAreaKm2;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AreaOfInterestJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Polygon getGeometryWgs84() {
        return geometryWgs84;
    }

    public void setGeometryWgs84(Polygon geometryWgs84) {
        this.geometryWgs84 = geometryWgs84;
    }

    public Polygon getBboxWgs84() {
        return bboxWgs84;
    }

    public void setBboxWgs84(Polygon bboxWgs84) {
        this.bboxWgs84 = bboxWgs84;
    }

    public BigDecimal getMetricAreaKm2() {
        return metricAreaKm2;
    }

    public void setMetricAreaKm2(BigDecimal metricAreaKm2) {
        this.metricAreaKm2 = metricAreaKm2;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
