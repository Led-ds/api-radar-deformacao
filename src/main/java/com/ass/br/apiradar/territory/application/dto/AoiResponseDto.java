package com.ass.br.apiradar.territory.application.dto;

import com.ass.br.apiradar.shared.domain.model.BoundingBox;
import com.ass.br.apiradar.territory.domain.model.AreaOfInterest;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * DTO de resposta pública com os dados da Área de Interesse, BBOX, WKT e GeoJSON nativo.
 */
public record AoiResponseDto(
        UUID id,
        UUID organizationId,
        String name,
        double metricAreaKm2,
        String status,
        BoundingBox boundingBox,
        String geometryWkt,
        Map<String, Object> geometryGeoJson,
        Instant createdAt
) implements Serializable {

    /**
     * Construtor de compatibilidade para código legado e testes que utilizam 8 argumentos.
     */
    public AoiResponseDto(
            UUID id,
            UUID organizationId,
            String name,
            double metricAreaKm2,
            String status,
            BoundingBox boundingBox,
            String geometryWkt,
            Instant createdAt
    ) {
        this(id, organizationId, name, metricAreaKm2, status, boundingBox, geometryWkt, null, createdAt);
    }

    public static AoiResponseDto fromDomain(AreaOfInterest aoi) {
        return new AoiResponseDto(
                aoi.getId().value(),
                aoi.getOrganizationId().value(),
                aoi.getName(),
                aoi.getMetricArea().rounded(2),
                aoi.getStatus().name(),
                aoi.getBoundingBox(),
                aoi.getGeometry().wkt(),
                aoi.getGeometry().toGeoJson(),
                aoi.getCreatedAt()
        );
    }
}
