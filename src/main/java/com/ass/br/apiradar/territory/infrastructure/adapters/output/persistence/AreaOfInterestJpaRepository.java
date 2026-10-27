package com.ass.br.apiradar.territory.infrastructure.adapters.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
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
}
