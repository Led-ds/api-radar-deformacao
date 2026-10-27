package com.ass.br.apiradar.catalog.application.services;

import com.ass.br.apiradar.catalog.application.dto.SarCatalogResponseDto;
import com.ass.br.apiradar.catalog.application.dto.SarCatalogSearchRequest;
import com.ass.br.apiradar.catalog.domain.model.SarScene;
import com.ass.br.apiradar.catalog.domain.ports.output.SarCatalogPort;
import com.ass.br.apiradar.shared.domain.exceptions.EntityNotFoundException;
import com.ass.br.apiradar.shared.domain.model.AoiId;
import com.ass.br.apiradar.shared.domain.model.BoundingBox;
import com.ass.br.apiradar.territory.domain.model.AreaOfInterest;
import com.ass.br.apiradar.territory.domain.ports.output.AreaOfInterestRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ass.br.apiradar.shared.domain.exceptions.InvalidGeometryException;
import com.ass.br.apiradar.shared.domain.model.PolygonGeometry;

import java.time.LocalDate;
import java.util.List;

/**
 * Serviço de aplicação que orquestra a busca de cenas SAR Sentinel-1 compatíveis com a AOI.
 * Valida a consistência topológica da geometria caso informada na requisição.
 */
@Service
public class SarCatalogApplicationService {

    private static final Logger log = LoggerFactory.getLogger(SarCatalogApplicationService.class);

    private final AreaOfInterestRepositoryPort aoiRepository;
    private final SarCatalogPort sarCatalogPort;

    public SarCatalogApplicationService(AreaOfInterestRepositoryPort aoiRepository,
                                        SarCatalogPort sarCatalogPort) {
        this.aoiRepository = aoiRepository;
        this.sarCatalogPort = sarCatalogPort;
    }

    @Transactional(readOnly = true)
    public SarCatalogResponseDto searchAvailableScenes(SarCatalogSearchRequest request) {
        log.info("Executando busca de catálogo SAR para aoiId={}", request.aoiId());

        AreaOfInterest aoi = aoiRepository.findById(AoiId.of(request.aoiId()))
                .orElseThrow(() -> new EntityNotFoundException("Área de interesse não localizada para o id: " + request.aoiId()));

        // Verificação topológica de integridade espacial entre cliente e persistência
        if (request.expectedGeometryWkt() != null && !request.expectedGeometryWkt().isBlank()) {
            PolygonGeometry clientGeom = PolygonGeometry.fromWkt(request.expectedGeometryWkt());
            if (!clientGeom.jtsPolygon().equalsTopo(aoi.getGeometry().jtsPolygon()) &&
                !clientGeom.jtsPolygon().equalsExact(aoi.getGeometry().jtsPolygon())) {
                throw new InvalidGeometryException("Divergência espacial detectada: a geometria fornecida na consulta difere da geometria persistida para a AOI "
                        + aoi.getId().value() + " (" + aoi.getName() + "). Salve as alterações antes de consultar cenas.");
            }
        }

        BoundingBox bbox = aoi.getBoundingBox();
        LocalDate start = request.startDate() != null ? request.startDate() : LocalDate.now().minusDays(90);
        LocalDate end = request.endDate() != null ? request.endDate() : LocalDate.now();

        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Período de busca inválido: a data inicial (" + start + ") não pode ser posterior à data final (" + end + ").");
        }

        List<SarScene> scenes = sarCatalogPort.searchScenes(bbox, start, end, request.orbitDirection());
        log.info("Total de cenas localizadas no catálogo para a AOI {}: {}", aoi.getName(), scenes.size());

        return SarCatalogResponseDto.of(scenes);
    }
}
