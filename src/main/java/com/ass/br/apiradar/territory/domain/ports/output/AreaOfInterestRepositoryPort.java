package com.ass.br.apiradar.territory.domain.ports.output;

import com.ass.br.apiradar.shared.domain.model.AoiId;
import com.ass.br.apiradar.shared.domain.model.OrganizationId;
import com.ass.br.apiradar.territory.domain.model.AreaOfInterest;

import java.util.List;
import java.util.Optional;

/**
 * Porta de saída (SPI) para persistência e recuperação do Agregado AreaOfInterest.
 */
public interface AreaOfInterestRepositoryPort {

    AreaOfInterest save(AreaOfInterest aoi);

    Optional<AreaOfInterest> findById(AoiId id, OrganizationId organizationId);

    Optional<AreaOfInterest> findById(AoiId id);

    List<AreaOfInterest> findByOrganizationId(OrganizationId organizationId);
}
