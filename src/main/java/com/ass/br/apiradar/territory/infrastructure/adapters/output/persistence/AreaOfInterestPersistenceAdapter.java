package com.ass.br.apiradar.territory.infrastructure.adapters.output.persistence;

import com.ass.br.apiradar.shared.domain.model.AoiId;
import com.ass.br.apiradar.shared.domain.model.BoundingBox;
import com.ass.br.apiradar.shared.domain.model.MetricArea;
import com.ass.br.apiradar.shared.domain.model.OrganizationId;
import com.ass.br.apiradar.shared.domain.model.PolygonGeometry;
import com.ass.br.apiradar.territory.domain.model.AoiStatus;
import com.ass.br.apiradar.territory.domain.model.AreaOfInterest;
import com.ass.br.apiradar.territory.domain.ports.output.AreaOfInterestRepositoryPort;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador de persistência que converte entre o Agregado de Domínio e a Entidade JPA PostGIS.
 */
@Component
public class AreaOfInterestPersistenceAdapter implements AreaOfInterestRepositoryPort {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    private final AreaOfInterestJpaRepository jpaRepository;

    public AreaOfInterestPersistenceAdapter(AreaOfInterestJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AreaOfInterest save(AreaOfInterest aoi) {
        AreaOfInterestJpaEntity entity = toEntity(aoi);
        try {
            Double postGisArea = jpaRepository.calculateGeodesicAreaKm2FromWkt(aoi.getGeometry().wkt());
            if (postGisArea != null && postGisArea > 0) {
                entity.setMetricAreaKm2(BigDecimal.valueOf(postGisArea));
            }
        } catch (Exception e) {
            // Se o PostGIS não estiver acessível de forma direta, preserva a aproximação calculada pelo domínio
        }
        AreaOfInterestJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<AreaOfInterest> findById(AoiId id, OrganizationId organizationId) {
        return jpaRepository.findByIdAndOrganizationId(id.value(), organizationId.value())
                .map(this::toDomain);
    }

    @Override
    public Optional<AreaOfInterest> findById(AoiId id) {
        return jpaRepository.findById(id.value())
                .map(this::toDomain);
    }

    @Override
    public List<AreaOfInterest> findByOrganizationId(OrganizationId organizationId) {
        return jpaRepository.findByOrganizationId(organizationId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    private AreaOfInterestJpaEntity toEntity(AreaOfInterest aoi) {
        AreaOfInterestJpaEntity entity = new AreaOfInterestJpaEntity();
        entity.setId(aoi.getId().value());
        entity.setOrganizationId(aoi.getOrganizationId().value());
        entity.setName(aoi.getName());
        entity.setGeometryWgs84(aoi.getGeometry().jtsPolygon());
        entity.setBboxWgs84(toBboxPolygon(aoi.getBoundingBox()));
        entity.setMetricAreaKm2(BigDecimal.valueOf(aoi.getMetricArea().valueKm2()));
        entity.setStatus(aoi.getStatus().name());
        entity.setCreatedAt(aoi.getCreatedAt());
        entity.setUpdatedAt(aoi.getUpdatedAt());
        return entity;
    }

    private AreaOfInterest toDomain(AreaOfInterestJpaEntity entity) {
        PolygonGeometry geom = PolygonGeometry.fromJts(entity.getGeometryWgs84());
        BoundingBox bbox = geom.calculateBoundingBox();
        MetricArea metricArea = new MetricArea(entity.getMetricAreaKm2().doubleValue());
        AoiStatus status = AoiStatus.valueOf(entity.getStatus());

        return AreaOfInterest.restore(
                AoiId.of(entity.getId()),
                OrganizationId.of(entity.getOrganizationId()),
                entity.getName(),
                geom,
                bbox,
                metricArea,
                status,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private Polygon toBboxPolygon(BoundingBox bbox) {
        Coordinate[] coords = new Coordinate[]{
                new Coordinate(bbox.minLon(), bbox.minLat()),
                new Coordinate(bbox.maxLon(), bbox.minLat()),
                new Coordinate(bbox.maxLon(), bbox.maxLat()),
                new Coordinate(bbox.minLon(), bbox.maxLat()),
                new Coordinate(bbox.minLon(), bbox.minLat())
        };
        LinearRing ring = GF.createLinearRing(coords);
        Polygon polygon = GF.createPolygon(ring);
        polygon.setSRID(4326);
        return polygon;
    }
}
