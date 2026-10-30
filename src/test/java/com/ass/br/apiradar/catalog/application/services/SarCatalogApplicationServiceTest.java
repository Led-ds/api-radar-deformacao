package com.ass.br.apiradar.catalog.application.services;

import com.ass.br.apiradar.catalog.application.dto.SarCatalogResponseDto;
import com.ass.br.apiradar.catalog.application.dto.SarCatalogSearchRequest;
import com.ass.br.apiradar.catalog.domain.model.SarScene;
import com.ass.br.apiradar.catalog.domain.ports.output.SarCatalogPort;
import com.ass.br.apiradar.shared.domain.exceptions.InvalidGeometryException;
import com.ass.br.apiradar.shared.domain.model.AoiId;
import com.ass.br.apiradar.shared.domain.model.BoundingBox;
import com.ass.br.apiradar.shared.domain.model.OrganizationId;
import com.ass.br.apiradar.shared.domain.model.PolygonGeometry;
import com.ass.br.apiradar.territory.domain.model.AreaOfInterest;
import com.ass.br.apiradar.territory.domain.ports.output.AreaOfInterestRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários - SarCatalogApplicationService (Integridade Espacial de AOI)")
class SarCatalogApplicationServiceTest {

    @Mock
    private AreaOfInterestRepositoryPort aoiRepository;

    @Mock
    private SarCatalogPort sarCatalogPort;

    private SarCatalogApplicationService service;

    private static final String MACEIO_WKT =
            "POLYGON((-35.795 -9.680, -35.710 -9.680, -35.710 -9.600, -35.795 -9.600, -35.795 -9.680))";

    @BeforeEach
    void setUp() {
        service = new SarCatalogApplicationService(aoiRepository, sarCatalogPort);
    }

    @Test
    @DisplayName("Executa busca SAR com sucesso quando a geometria informada é equivalente à AOI persistida")
    void shouldSearchScenesWhenGeometryMatches() {
        AoiId aoiId = AoiId.random();
        OrganizationId orgId = OrganizationId.random();
        PolygonGeometry geom = PolygonGeometry.fromWkt(MACEIO_WKT);
        AreaOfInterest aoi = AreaOfInterest.create(aoiId, orgId, "Piloto Maceió", geom, 250.0);

        when(aoiRepository.findById(eq(aoiId))).thenReturn(Optional.of(aoi));

        SarScene scene = new SarScene(
                "S1A_IW_SLC__1SDV_20230105T211512",
                Instant.now(),
                142,
                "DESCENDING",
                "VV+VH",
                "SENTINEL-1A"
        );
        when(sarCatalogPort.searchScenes(any(BoundingBox.class), any(), any(), any())).thenReturn(List.of(scene));

        SarCatalogSearchRequest request = new SarCatalogSearchRequest(
                aoiId.value(),
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2023, 1, 31),
                "DESCENDING",
                MACEIO_WKT
        );

        SarCatalogResponseDto response = service.searchAvailableScenes(request);

        assertThat(response).isNotNull();
        assertThat(response.totalScenesFound()).isEqualTo(1);
    }

    @Test
    @DisplayName("Rejeita busca SAR com InvalidGeometryException quando a geometria informada diverge da AOI salva")
    void shouldRejectSearchWhenGeometryDivergesFromPersistedAoi() {
        AoiId aoiId = AoiId.random();
        OrganizationId orgId = OrganizationId.random();
        PolygonGeometry geom = PolygonGeometry.fromWkt(MACEIO_WKT);
        AreaOfInterest aoi = AreaOfInterest.create(aoiId, orgId, "Piloto Maceió", geom, 250.0);

        when(aoiRepository.findById(eq(aoiId))).thenReturn(Optional.of(aoi));

        // Geometria alterada (outro polígono com coordenadas diferentes)
        String alteredWkt = "POLYGON((-47.92 -15.80, -47.90 -15.80, -47.90 -15.78, -47.92 -15.78, -47.92 -15.80))";

        SarCatalogSearchRequest request = new SarCatalogSearchRequest(
                aoiId.value(),
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2023, 1, 31),
                "DESCENDING",
                alteredWkt
        );

        assertThatThrownBy(() -> service.searchAvailableScenes(request))
                .isInstanceOf(InvalidGeometryException.class)
                .hasMessageContaining("Divergência espacial detectada");
    }
}
