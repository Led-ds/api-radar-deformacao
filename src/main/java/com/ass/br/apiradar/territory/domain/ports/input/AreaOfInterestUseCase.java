package com.ass.br.apiradar.territory.domain.ports.input;

import com.ass.br.apiradar.shared.domain.model.AoiId;
import com.ass.br.apiradar.shared.domain.model.OrganizationId;
import com.ass.br.apiradar.territory.application.dto.AoiResponseDto;
import com.ass.br.apiradar.territory.application.dto.CreateAoiCommand;

import java.util.List;

/**
 * Porta de entrada (Use Case API) para gestão de Áreas de Interesse.
 */
public interface AreaOfInterestUseCase {

    AoiResponseDto registerAreaOfInterest(CreateAoiCommand command);

    AoiResponseDto getAreaOfInterestById(AoiId id, OrganizationId organizationId);

    List<AoiResponseDto> listAreasOfInterestByOrganization(OrganizationId organizationId);
}
