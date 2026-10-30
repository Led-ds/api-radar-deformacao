package com.ass.br.apiradar.territory.domain.model;

import com.ass.br.apiradar.shared.domain.model.AoiId;
import com.ass.br.apiradar.shared.domain.model.BoundingBox;
import com.ass.br.apiradar.shared.domain.model.MetricArea;
import com.ass.br.apiradar.shared.domain.model.OrganizationId;
import com.ass.br.apiradar.shared.domain.model.PolygonGeometry;

import java.time.Instant;
import java.util.Objects;

/**
 * Agregado Raiz que representa uma Área de Interesse (AOI) monitorada pelo Radar++.
 */
public class AreaOfInterest {

    private final AoiId id;
    private final OrganizationId organizationId;
    private String name;
    private PolygonGeometry geometry;
    private BoundingBox boundingBox;
    private MetricArea metricArea;
    private AoiStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public AreaOfInterest(AoiId id,
                          OrganizationId organizationId,
                          String name,
                          PolygonGeometry geometry,
                          MetricArea metricArea,
                          double maxAllowedAreaKm2,
                          Instant createdAt) {
        this.id = Objects.requireNonNull(id, "AoiId cannot be null");
        this.organizationId = Objects.requireNonNull(organizationId, "OrganizationId cannot be null");
        setName(name);
        setGeometry(geometry, metricArea, maxAllowedAreaKm2);
        this.status = AoiStatus.ACTIVE;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = this.createdAt;
    }

    public static AreaOfInterest create(AoiId id,
                                        OrganizationId organizationId,
                                        String name,
                                        PolygonGeometry geometry,
                                        double maxAllowedAreaKm2) {
        MetricArea calculatedArea = geometry.calculateApproximateMetricArea();
        return new AreaOfInterest(id, organizationId, name, geometry, calculatedArea, maxAllowedAreaKm2, Instant.now());
    }

    public static AreaOfInterest restore(AoiId id,
                                         OrganizationId organizationId,
                                         String name,
                                         PolygonGeometry geometry,
                                         BoundingBox bbox,
                                         MetricArea metricArea,
                                         AoiStatus status,
                                         Instant createdAt,
                                         Instant updatedAt) {
        AreaOfInterest aoi = new AreaOfInterest(id, organizationId, name, geometry, metricArea, Double.MAX_VALUE, createdAt);
        aoi.boundingBox = bbox;
        aoi.status = status;
        aoi.updatedAt = updatedAt;
        return aoi;
    }

    public void setGeometry(PolygonGeometry geometry, MetricArea metricArea, double maxAllowedAreaKm2) {
        this.geometry = Objects.requireNonNull(geometry, "PolygonGeometry cannot be null");
        this.boundingBox = geometry.calculateBoundingBox();
        this.metricArea = Objects.requireNonNull(metricArea, "MetricArea cannot be null");
        this.metricArea.validateMaxAllowed(maxAllowedAreaKm2);
        this.updatedAt = Instant.now();
    }

    public void setName(String name) {
        if (name == null || name.trim().length() < 3) {
            throw new IllegalArgumentException("AOI name must have at least 3 characters");
        }
        this.name = name.trim();
        this.updatedAt = Instant.now();
    }

    public void archive() {
        this.status = AoiStatus.ARCHIVED;
        this.updatedAt = Instant.now();
    }

    public AoiId getId() {
        return id;
    }

    public OrganizationId getOrganizationId() {
        return organizationId;
    }

    public String getName() {
        return name;
    }

    public PolygonGeometry getGeometry() {
        return geometry;
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public MetricArea getMetricArea() {
        return metricArea;
    }

    public AoiStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
