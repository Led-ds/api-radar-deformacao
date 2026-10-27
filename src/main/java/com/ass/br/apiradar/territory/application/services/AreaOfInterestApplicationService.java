package com.ass.br.apiradar.territory.application.services;

import com.ass.br.apiradar.shared.domain.exceptions.EntityNotFoundException;
import com.ass.br.apiradar.shared.domain.model.AoiId;
import com.ass.br.apiradar.shared.domain.model.OrganizationId;
import com.ass.br.apiradar.shared.domain.model.PolygonGeometry;
import com.ass.br.apiradar.territory.application.dto.AoiResponseDto;
import com.ass.br.apiradar.territory.application.dto.CreateAoiCommand;
import com.ass.br.apiradar.territory.domain.model.AreaOfInterest;
import com.ass.br.apiradar.territory.domain.ports.input.AreaOfInterestUseCase;
import com.ass.br.apiradar.territory.domain.ports.output.AreaOfInterestRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Serviço de aplicação que orquestra o caso de uso de registro e consulta de Áreas de Interesse.
 */
@Service
public class AreaOfInterestApplicationService implements AreaOfInterestUseCase {

    private static final Logger log = LoggerFactory.getLogger(AreaOfInterestApplicationService.class);

    private final AreaOfInterestRepositoryPort repository;

    public AreaOfInterestApplicationService(AreaOfInterestRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public AoiResponseDto registerAreaOfInterest(CreateAoiCommand command) {
        log.info("Iniciando registro de AOI '{}' para a organizacao {}", command.name(), command.organizationId());

        PolygonGeometry polygonGeometry = PolygonGeometry.fromWkt(command.geometryWkt());

        AreaOfInterest aoi = AreaOfInterest.create(
                AoiId.random(),
                command.organizationId(),
                command.name(),
                polygonGeometry,
                command.effectiveMaxAllowedArea()
        );

        AreaOfInterest savedAoi = repository.save(aoi);
        log.info("AOI registrada com sucesso: id={}, area={} km2", savedAoi.getId(), savedAoi.getMetricArea().valueKm2());

        return AoiResponseDto.fromDomain(savedAoi);
    }

    @Override
    @Transactional(readOnly = true)
    public AoiResponseDto getAreaOfInterestById(AoiId id, OrganizationId organizationId) {
        return repository.findById(id, organizationId)
                .map(AoiResponseDto::fromDomain)
                .orElseThrow(() -> new EntityNotFoundException("Área de interesse não encontrada para o ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AoiResponseDto> listAreasOfInterestByOrganization(OrganizationId organizationId) {
        return repository.findByOrganizationId(organizationId).stream()
                .map(AoiResponseDto::fromDomain)
                .toList();
    }
}
