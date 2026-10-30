package com.ass.br.apiradar.interferometry.application.services;

import com.ass.br.apiradar.catalog.domain.model.SarScene;
import com.ass.br.apiradar.catalog.domain.ports.output.SarCatalogPort;
import com.ass.br.apiradar.interferometry.application.dto.*;
import com.ass.br.apiradar.interferometry.domain.model.InterferometricPair;
import com.ass.br.apiradar.interferometry.domain.model.PairSelectionPolicy;
import com.ass.br.apiradar.interferometry.domain.ports.output.PerpendicularBaselinePort;
import com.ass.br.apiradar.interferometry.domain.services.PairFormationService;
import com.ass.br.apiradar.shared.domain.exceptions.EntityNotFoundException;
import com.ass.br.apiradar.shared.domain.exceptions.InvalidGeometryException;
import com.ass.br.apiradar.shared.domain.model.AoiId;
import com.ass.br.apiradar.shared.domain.model.BoundingBox;
import com.ass.br.apiradar.shared.domain.model.PolygonGeometry;
import com.ass.br.apiradar.territory.domain.model.AreaOfInterest;
import com.ass.br.apiradar.territory.domain.ports.output.AreaOfInterestRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Serviço de aplicação para formação, avaliação e consulta de pares interferométricos candidatos.
 * Orquestra catálogo SAR, repositório de território e portas de baseline com rigorosa validação espacial.
 */
@Service
public class InterferometricPairApplicationService {

    private static final Logger log = LoggerFactory.getLogger(InterferometricPairApplicationService.class);

    private final AreaOfInterestRepositoryPort aoiRepository;
    private final SarCatalogPort sarCatalogPort;
    private final PerpendicularBaselinePort baselinePort;
    private final PairFormationService pairFormationService;

    public InterferometricPairApplicationService(
            AreaOfInterestRepositoryPort aoiRepository,
            SarCatalogPort sarCatalogPort,
            PerpendicularBaselinePort baselinePort
    ) {
        this.aoiRepository = aoiRepository;
        this.sarCatalogPort = sarCatalogPort;
        this.baselinePort = baselinePort;
        this.pairFormationService = new PairFormationService();
    }

    @Transactional(readOnly = true)
    public GeneratePairsResponseDto generatePairs(GeneratePairsRequest request) {
        log.info("Iniciando geração de pares interferométricos para aoiId={}", request.aoiId());

        AreaOfInterest aoi = aoiRepository.findById(AoiId.of(request.aoiId()))
                .orElseThrow(() -> new EntityNotFoundException("Área de interesse não localizada para o id: " + request.aoiId()));

        // Validação de integridade espacial: se cliente informou geometria, confere com a geometria salva
        if (request.expectedGeometryWkt() != null && !request.expectedGeometryWkt().isBlank()) {
            PolygonGeometry clientGeom = PolygonGeometry.fromWkt(request.expectedGeometryWkt());
            if (!clientGeom.jtsPolygon().equalsTopo(aoi.getGeometry().jtsPolygon()) &&
                !clientGeom.jtsPolygon().equalsExact(aoi.getGeometry().jtsPolygon())) {
                throw new InvalidGeometryException(
                        "Divergência espacial detectada: a geometria fornecida na consulta difere da geometria persistida para a AOI "
                                + aoi.getId().value() + " (" + aoi.getName() + "). Salve as alterações antes de formar pares."
                );
            }
        }

        PairSelectionPolicy policy = resolvePolicy(request);
        log.info("Política científica selecionada para avaliação: {} (versão {})", policy.profileName(), policy.version());

        BoundingBox bbox = aoi.getBoundingBox();
        LocalDate start = request.startDate() != null ? request.startDate() : LocalDate.now().minusDays(90);
        LocalDate end = request.endDate() != null ? request.endDate() : LocalDate.now();

        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Período inválido: data inicial (" + start + ") não pode ser posterior à final (" + end + ").");
        }

        // 1. Descoberta de cenas SAR na AOI
        List<SarScene> scenes = sarCatalogPort.searchScenes(bbox, start, end, request.flightDirection());
        log.info("Total de cenas localizadas para formação de pares na AOI {}: {}", aoi.getName(), scenes.size());

        // 2. Formação e avaliação científica dos pares no domínio
        List<InterferometricPair> pairs = pairFormationService.formCandidatePairs(scenes, policy, baselinePort);
        log.info("Total de pares interferométricos avaliados: {}", pairs.size());

        // 3. Mapeamento de DTOs e estatísticas
        List<InterferometricPairDto> pairDtos = pairs.stream()
                .map(InterferometricPairDto::fromDomain)
                .toList();

        Map<String, Long> summary = pairs.stream()
                .collect(Collectors.groupingBy(p -> p.getStatus().name(), Collectors.counting()));

        return new GeneratePairsResponseDto(
                aoi.getId().value().toString(),
                aoi.getName(),
                PairSelectionPolicyDto.fromDomain(policy),
                scenes.size(),
                pairs.size(),
                pairDtos,
                summary,
                GeneratePairsResponseDto.DEFAULT_SCIENTIFIC_DISCLAIMER
        );
    }

    public List<PairSelectionPolicyDto> getAvailablePolicies() {
        return List.of(
                PairSelectionPolicyDto.fromDomain(PairSelectionPolicy.tropicalUrbanMaceio()),
                PairSelectionPolicyDto.fromDomain(PairSelectionPolicy.generalInSar()),
                PairSelectionPolicyDto.fromDomain(PairSelectionPolicy.strictScientific())
        );
    }

    private PairSelectionPolicy resolvePolicy(GeneratePairsRequest request) {
        String profile = request.policyProfile() != null ? request.policyProfile().toUpperCase() : "TROPICAL_URBAN_MACEIO";

        return switch (profile) {
            case "GENERAL_INSAR_S1" -> PairSelectionPolicy.generalInSar();
            case "STRICT_SCIENTIFIC" -> PairSelectionPolicy.strictScientific();
            case "CUSTOM" -> PairSelectionPolicy.createCustom(
                    request.customMinTemporalDays(),
                    request.customMaxTemporalDays(),
                    request.customMaxPerpendicularMeters(),
                    null,
                    false
            );
            default -> PairSelectionPolicy.tropicalUrbanMaceio();
        };
    }
}
