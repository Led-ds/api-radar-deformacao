package com.ass.br.apiradar.interferometry.application;

import com.ass.br.apiradar.catalog.domain.model.SarScene;
import com.ass.br.apiradar.catalog.domain.ports.output.SarCatalogPort;
import com.ass.br.apiradar.interferometry.application.dto.GeneratePairsRequest;
import com.ass.br.apiradar.interferometry.application.dto.GeneratePairsResponseDto;
import com.ass.br.apiradar.interferometry.application.services.InterferometricPairApplicationService;
import com.ass.br.apiradar.interferometry.domain.model.BaselineSource;
import com.ass.br.apiradar.interferometry.domain.model.BaselineValue;
import com.ass.br.apiradar.interferometry.domain.ports.output.PerpendicularBaselinePort;
import com.ass.br.apiradar.shared.domain.exceptions.EntityNotFoundException;
import com.ass.br.apiradar.shared.domain.exceptions.InvalidGeometryException;
import com.ass.br.apiradar.shared.domain.model.AoiId;
import com.ass.br.apiradar.shared.domain.model.BoundingBox;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("Testes de Aplicação: InterferometricPairApplicationService")
class InterferometricPairApplicationServiceTest {

    private AreaOfInterestRepositoryPort aoiRepository;
    private SarCatalogPort sarCatalogPort;
    private PerpendicularBaselinePort baselinePort;
    private InterferometricPairApplicationService applicationService;

    private final String VALID_WKT = "POLYGON((-35.795 -9.680, -35.710 -9.680, -35.710 -9.600, -35.795 -9.600, -35.795 -9.680))";

    @BeforeEach
    void setUp() {
        aoiRepository = Mockito.mock(AreaOfInterestRepositoryPort.class);
        sarCatalogPort = Mockito.mock(SarCatalogPort.class);
        baselinePort = Mockito.mock(PerpendicularBaselinePort.class);

        applicationService = new InterferometricPairApplicationService(
                aoiRepository,
                sarCatalogPort,
                baselinePort
        );
    }

    private AreaOfInterest createMockAoi(String id) {
        return AreaOfInterest.create(
                AoiId.of(id),
                com.ass.br.apiradar.shared.domain.model.OrganizationId.random(),
                "Piloto Maceió",
                PolygonGeometry.fromWkt(VALID_WKT),
                250.0
        );
    }

    @Test
    @DisplayName("Deve lançar EntityNotFoundException se a AOI não for localizada")
    void shouldThrowWhenAoiNotFound() {
        when(aoiRepository.findById(any())).thenReturn(Optional.empty());

        GeneratePairsRequest req = new GeneratePairsRequest(
                "c0a80101-0000-0000-0000-000000000001",
                LocalDate.now().minusDays(30),
                LocalDate.now(),
                "DESCENDING"
        );

        assertThrows(EntityNotFoundException.class, () -> applicationService.generatePairs(req));
    }

    @Test
    @DisplayName("Deve lançar InvalidGeometryException caso a geometria enviada divirja da AOI salva")
    void shouldThrowWhenGeometryDiverges() {
        AreaOfInterest aoi = createMockAoi("c0a80101-0000-0000-0000-000000000001");
        when(aoiRepository.findById(any())).thenReturn(Optional.of(aoi));

        String alteredWkt = "POLYGON((-35.800 -9.680, -35.710 -9.680, -35.710 -9.600, -35.800 -9.600, -35.800 -9.680))";

        GeneratePairsRequest req = new GeneratePairsRequest(
                aoi.getId().value().toString(),
                LocalDate.now().minusDays(30),
                LocalDate.now(),
                "DESCENDING",
                "TROPICAL_URBAN_MACEIO",
                null, null, null,
                alteredWkt
        );

        assertThrows(InvalidGeometryException.class, () -> applicationService.generatePairs(req));
    }

    @Test
    @DisplayName("Deve orquestrar com sucesso a descoberta de cenas e geração de pares")
    void shouldGenerateCandidatePairsSuccessfully() {
        AreaOfInterest aoi = createMockAoi("c0a80101-0000-0000-0000-000000000001");
        when(aoiRepository.findById(any())).thenReturn(Optional.of(aoi));

        SarScene s1 = new SarScene("S1A_IW_SLC__1SDV_20230101T210000", Instant.parse("2023-01-01T21:00:00Z"), 96, "DESCENDING", "VV+VH", "SENTINEL-1A", "ID1", "NASA ASF");
        SarScene s2 = new SarScene("S1A_IW_SLC__1SDV_20230113T210000", Instant.parse("2023-01-13T21:00:00Z"), 96, "DESCENDING", "VV+VH", "SENTINEL-1A", "ID2", "NASA ASF");
        when(sarCatalogPort.searchScenes(any(BoundingBox.class), any(), any(), any())).thenReturn(List.of(s1, s2));

        when(baselinePort.getPerpendicularBaseline(any(), any()))
                .thenReturn(BaselineValue.calculated(45.0, BaselineSource.ASF_BASELINE_SERVICE));

        GeneratePairsRequest req = new GeneratePairsRequest(
                aoi.getId().value().toString(),
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2023, 1, 31),
                "DESCENDING",
                "TROPICAL_URBAN_MACEIO",
                null, null, null,
                VALID_WKT
        );

        GeneratePairsResponseDto response = applicationService.generatePairs(req);

        assertNotNull(response);
        assertEquals(aoi.getId().value().toString(), response.aoiId());
        assertEquals(2, response.totalScenesDiscovered());
        assertEquals(1, response.totalPairsGenerated());
        assertEquals(1, response.pairs().size());
        assertEquals(12, response.pairs().get(0).temporalBaselineDays());
        assertEquals(45.0, response.pairs().get(0).perpendicularBaselineMeters());
        assertNotNull(response.scientificDisclaimer());
    }
}
