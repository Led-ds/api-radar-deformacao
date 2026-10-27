package com.ass.br.apiradar.interferometry.application;

import com.ass.br.apiradar.catalog.domain.model.SarBurst;
import com.ass.br.apiradar.catalog.domain.model.SarScene;
import com.ass.br.apiradar.catalog.domain.ports.output.SarBurstCatalogPort;
import com.ass.br.apiradar.catalog.domain.ports.output.SarCatalogPort;
import com.ass.br.apiradar.interferometry.application.dto.InSarProcessingDryRunRequestDto;
import com.ass.br.apiradar.interferometry.application.dto.InSarProcessingDryRunResponseDto;
import com.ass.br.apiradar.interferometry.application.dto.InSarProcessingOptionsResponseDto;
import com.ass.br.apiradar.interferometry.application.services.InSarProcessingPreparationService;
import com.ass.br.apiradar.interferometry.domain.exceptions.IneligiblePairProcessingException;
import com.ass.br.apiradar.interferometry.domain.model.BaselineSource;
import com.ass.br.apiradar.interferometry.domain.model.BaselineValue;
import com.ass.br.apiradar.interferometry.domain.model.processing.InSarProcessingParameters;
import com.ass.br.apiradar.interferometry.domain.model.processing.InSarProcessingRequest;
import com.ass.br.apiradar.interferometry.domain.model.processing.ProcessingModality;
import com.ass.br.apiradar.interferometry.domain.ports.output.InSarProcessorPort;
import com.ass.br.apiradar.interferometry.domain.ports.output.PerpendicularBaselinePort;
import com.ass.br.apiradar.shared.domain.exceptions.EntityNotFoundException;
import com.ass.br.apiradar.shared.domain.model.AoiId;
import com.ass.br.apiradar.shared.domain.model.OrganizationId;
import com.ass.br.apiradar.shared.domain.model.PolygonGeometry;
import com.ass.br.apiradar.territory.domain.model.AreaOfInterest;
import com.ass.br.apiradar.territory.domain.ports.output.AreaOfInterestRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@DisplayName("Testes de Aplicação: InSarProcessingPreparationService")
class InSarProcessingPreparationServiceTest {

    private AreaOfInterestRepositoryPort aoiRepository;
    private SarCatalogPort sarCatalogPort;
    private PerpendicularBaselinePort baselinePort;
    private InSarProcessorPort processorPort;
    private SarBurstCatalogPort burstCatalogPort;

    private InSarProcessingPreparationService service;
    private AreaOfInterest aoi;

    private final String refSceneName = "S1A_IW_SLC__1SDV_20230104T080249_20230104T080316_046631_0596C5_604A";
    private final String secSceneName = "S1A_IW_SLC__1SDV_20230116T080248_20230116T080315_046806_059CA6_BF75";

    private final String fp19 = "POLYGON((-35.173587 -9.754047, -35.572546 -9.66247, -35.942022 -9.577039, -35.987944 -9.780139, -35.617222 -9.861317, -35.21699 -9.948644, -35.173587 -9.754047))";
    private final String fp18 = "POLYGON((-35.136491 -9.58748, -35.535205 -9.496005, -35.904471 -9.410672, -35.950324 -9.613783, -35.579818 -9.694862, -35.179836 -9.782087, -35.136491 -9.58748))";

    @BeforeEach
    void setUp() {
        aoiRepository = Mockito.mock(AreaOfInterestRepositoryPort.class);
        sarCatalogPort = Mockito.mock(SarCatalogPort.class);
        baselinePort = Mockito.mock(PerpendicularBaselinePort.class);
        processorPort = Mockito.mock(InSarProcessorPort.class);
        burstCatalogPort = Mockito.mock(SarBurstCatalogPort.class);

        service = new InSarProcessingPreparationService(aoiRepository, sarCatalogPort, baselinePort, processorPort, burstCatalogPort);

        aoi = AreaOfInterest.create(
                AoiId.of("c0a80101-0000-0000-0000-000000000001"),
                OrganizationId.random(),
                "Piloto Maceió",
                PolygonGeometry.fromWkt("POLYGON((-35.75 -9.67, -35.72 -9.67, -35.72 -9.64, -35.75 -9.64, -35.75 -9.67))"),
                120.0
        );

        Mockito.when(aoiRepository.findById(AoiId.of("c0a80101-0000-0000-0000-000000000001")))
                .thenReturn(Optional.of(aoi));

        SarScene s1 = new SarScene(refSceneName, Instant.parse("2023-01-04T08:02:49Z"), 9, "DESCENDING", "VV+VH", "SENTINEL-1A", "ID1", "NASA ASF");
        SarScene s2 = new SarScene(secSceneName, Instant.parse("2023-01-16T08:02:48Z"), 9, "DESCENDING", "VV+VH", "SENTINEL-1A", "ID2", "NASA ASF");

        Mockito.when(sarCatalogPort.searchScenes(any(), any(), any(), any()))
                .thenReturn(List.of(s1, s2));
    }

    @Test
    @DisplayName("Gera requisição dry-run com sucesso para o PAR-01 elegível com cobertura calculada")
    void shouldGenerateDryRunForEligiblePair() {
        String pairId = refSceneName + "__" + secSceneName;

        Mockito.when(baselinePort.getPerpendicularBaseline(any(), any()))
                .thenReturn(BaselineValue.calculated(-79.0, BaselineSource.ASF_BASELINE_SERVICE));

        String burstRef = "S1_018319_IW1_20230104T080254_VV_604A-BURST";
        String burstSec = "S1_018319_IW1_20230116T080253_VV_BF75-BURST";

        InSarProcessingRequest mockRequest = new InSarProcessingRequest(
                UUID.randomUUID(),
                "idemp-12345",
                "corr-67890",
                "c0a80101-0000-0000-0000-000000000001",
                pairId,
                refSceneName,
                secSceneName,
                List.of(burstRef, burstSec),
                ProcessingModality.BURST_INSAR,
                InSarProcessingParameters.defaultForUrbanMaceio(),
                "hyp3-isce:v1.0",
                "TROPICAL_URBAN_MACEIO (v1.1)",
                1,
                true,
                "{\"dry_run\": true}",
                Instant.now()
        );

        Mockito.when(processorPort.buildDryRunRequest(any(), any(), any(), any(), any(), any()))
                .thenReturn(mockRequest);

        InSarProcessingDryRunRequestDto requestDto = new InSarProcessingDryRunRequestDto(
                "c0a80101-0000-0000-0000-000000000001",
                ProcessingModality.BURST_INSAR,
                List.of(burstRef, burstSec),
                "10x2",
                true,
                0.6,
                30,
                "corr-67890"
        );

        InSarProcessingDryRunResponseDto response = service.generateDryRunRequest(pairId, requestDto);

        assertNotNull(response);
        assertTrue(response.dryRun());
        assertTrue(response.executionBlockedBySecurityPolicy());
        assertEquals("idemp-12345", response.idempotencyKey());
        assertEquals(ProcessingModality.BURST_INSAR, response.modality());
        assertEquals("INSAR_ISCE_BURST", response.officialJobType());
        assertEquals(1, response.estimatedCredits());
        assertEquals("https://hyp3-docs.asf.alaska.edu/using/credits/", response.creditCostSource());
        assertTrue(response.validationStatus().contains("READY_FOR_DRY_RUN"));
        assertTrue(response.coverageEvaluation().contains("cobertura nominal"));
    }

    @Test
    @DisplayName("Retorna opções completas de preparação com bursts dinâmicos e coberturas nominais")
    void shouldReturnProcessingOptionsForEligiblePair() {
        String pairId = refSceneName + "__" + secSceneName;

        Mockito.when(baselinePort.getPerpendicularBaseline(any(), any()))
                .thenReturn(BaselineValue.calculated(-79.0, BaselineSource.ASF_BASELINE_SERVICE));

        SarBurst b19Ref = new SarBurst("S1_018319_IW1_20230104T080254_VV_604A-BURST", 18319, "IW1",
                Instant.parse("2023-01-04T08:02:54Z"), 9, "DESCENDING", "VV", PolygonGeometry.fromWkt(fp19), "NASA ASF");
        SarBurst b18Ref = new SarBurst("S1_018318_IW1_20230104T080251_VV_604A-BURST", 18318, "IW1",
                Instant.parse("2023-01-04T08:02:51Z"), 9, "DESCENDING", "VV", PolygonGeometry.fromWkt(fp18), "NASA ASF");

        SarBurst b19Sec = new SarBurst("S1_018319_IW1_20230116T080253_VV_BF75-BURST", 18319, "IW1",
                Instant.parse("2023-01-16T08:02:53Z"), 9, "DESCENDING", "VV", PolygonGeometry.fromWkt(fp19), "NASA ASF");
        SarBurst b18Sec = new SarBurst("S1_018318_IW1_20230116T080250_VV_BF75-BURST", 18318, "IW1",
                Instant.parse("2023-01-16T08:02:50Z"), 9, "DESCENDING", "VV", PolygonGeometry.fromWkt(fp18), "NASA ASF");

        Mockito.when(burstCatalogPort.searchBursts(any(), any(), eq(LocalDate.of(2023, 1, 4)), any(int.class), any(), any()))
                .thenReturn(List.of(b19Ref, b18Ref));
        Mockito.when(burstCatalogPort.searchBursts(any(), any(), eq(LocalDate.of(2023, 1, 16)), any(int.class), any(), any()))
                .thenReturn(List.of(b19Sec, b18Sec));

        InSarProcessingOptionsResponseDto options = service.getProcessingOptions(pairId, "c0a80101-0000-0000-0000-000000000001");

        assertNotNull(options);
        assertEquals("READY_FOR_SELECTION", options.status());
        assertEquals(3, options.modalities().size());
        assertEquals(2, options.candidateBurstPairs().size());

        // Burst 18319 deve ser o recomendado (maior cobertura na AOI de Maceió)
        assertTrue(options.candidateBurstPairs().get(0).recommended());
        assertEquals(18319, options.candidateBurstPairs().get(0).relativeBurstId());
        assertEquals(100.0, options.candidateBurstPairs().get(0).commonTemporalCoveragePercentage(), 0.1);

        // Burst 18318 cobre ~69.68%
        assertEquals(18318, options.candidateBurstPairs().get(1).relativeBurstId());
        assertEquals(69.68, options.candidateBurstPairs().get(1).commonTemporalCoveragePercentage(), 0.5);

        // Multi-Burst cobre 100%
        assertTrue(options.multiBurstSummary().available());
        assertEquals(2, options.multiBurstSummary().referenceBurstIds().size());
        assertEquals(2, options.multiBurstSummary().secondaryBurstIds().size());
        assertEquals(100.0, options.multiBurstSummary().combinedCoveragePercentage(), 0.1);

        // Limitações de qualidade apresentadas separadamente
        assertFalse(options.qualityLimitations().isEmpty());
    }

    @Test
    @DisplayName("Retorna estado pendente de forma resiliente quando catálogo de bursts falha ou está indisponível")
    void shouldReturnPendingWhenBurstMetadataFails() {
        String pairId = refSceneName + "__" + secSceneName;

        Mockito.when(baselinePort.getPerpendicularBaseline(any(), any()))
                .thenReturn(BaselineValue.calculated(-79.0, BaselineSource.ASF_BASELINE_SERVICE));

        Mockito.when(burstCatalogPort.searchBursts(any(), any(), any(), any(int.class), any(), any()))
                .thenThrow(new RuntimeException("NASA ASF Search API indisponível"));

        InSarProcessingOptionsResponseDto options = service.getProcessingOptions(pairId, "c0a80101-0000-0000-0000-000000000001");

        assertNotNull(options);
        assertEquals("PENDING_BURST_METADATA", options.status());
        assertTrue(options.candidateBurstPairs().isEmpty());
        assertFalse(options.multiBurstSummary().available());
        assertFalse(options.validationWarnings().isEmpty());
    }

    @Test
    @DisplayName("Retorna INELIGIBLE_PAIR para par que viola limiar de baseline perpendicular")
    void shouldReturnIneligiblePairWhenBaselineExceeded() {
        String pairId = refSceneName + "__" + secSceneName;

        Mockito.when(baselinePort.getPerpendicularBaseline(any(), any()))
                .thenReturn(BaselineValue.calculated(235.0, BaselineSource.ASF_BASELINE_SERVICE));

        InSarProcessingOptionsResponseDto options = service.getProcessingOptions(pairId, "c0a80101-0000-0000-0000-000000000001");

        assertNotNull(options);
        assertEquals("INELIGIBLE_PAIR", options.status());
        assertTrue(options.candidateBurstPairs().isEmpty());
        assertFalse(options.validationWarnings().isEmpty());
    }

    @Test
    @DisplayName("Bloqueia geração de dry-run para par inelegível (baseline excessiva)")
    void shouldBlockDryRunWhenPairIsRejected() {
        String pairId = refSceneName + "__" + secSceneName;

        Mockito.when(baselinePort.getPerpendicularBaseline(any(), any()))
                .thenReturn(BaselineValue.calculated(235.0, BaselineSource.ASF_BASELINE_SERVICE));

        InSarProcessingDryRunRequestDto requestDto = new InSarProcessingDryRunRequestDto(
                "c0a80101-0000-0000-0000-000000000001",
                ProcessingModality.BURST_INSAR,
                List.of(),
                "10x2",
                true,
                0.6,
                30,
                "corr-67890"
        );

        assertThrows(IneligiblePairProcessingException.class, () ->
                service.generateDryRunRequest(pairId, requestDto)
        );
    }

    @Test
    @DisplayName("Lança EntityNotFoundException quando a AOI não existir")
    void shouldThrowEntityNotFoundWhenAoiDoesNotExist() {
        Mockito.when(aoiRepository.findById(AoiId.of("c0a80101-9999-9999-9999-999999999999")))
                .thenReturn(Optional.empty());

        InSarProcessingDryRunRequestDto requestDto = new InSarProcessingDryRunRequestDto(
                "c0a80101-9999-9999-9999-999999999999",
                ProcessingModality.BURST_INSAR,
                List.of(),
                "10x2",
                true,
                0.6,
                30,
                "corr-1"
        );

        assertThrows(EntityNotFoundException.class, () ->
                service.generateDryRunRequest("S1A_1__S1A_2", requestDto)
        );
    }
}
