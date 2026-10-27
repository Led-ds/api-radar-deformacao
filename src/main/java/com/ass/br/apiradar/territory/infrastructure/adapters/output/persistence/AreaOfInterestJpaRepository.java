package com.ass.br.apiradar.territory.infrastructure.adapters.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository para a entidade AreaOfInterestJpaEntity.
 */
@Repository
public interface AreaOfInterestJpaRepository extends JpaRepository<AreaOfInterestJpaEntity, UUID> {

    Optional<AreaOfInterestJpaEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<AreaOfInterestJpaEntity> findByOrganizationId(UUID organizationId);

    /**
     * Calcula a área métrica geodésica autoritativa no elipsoide WGS 84 via PostGIS.
     */
    @Query(value = "SELECT (ST_Area(ST_GeomFromText(:wkt, 4326)::geography) / 1000000.0)", nativeQuery = true)
    Double calculateGeodesicAreaKm2FromWkt(@Param("wkt") String wkt);
}
