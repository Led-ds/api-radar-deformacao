package com.ass.br.apiradar.territory.domain.model;

import com.ass.br.apiradar.shared.domain.exceptions.InvalidGeometryException;
import com.ass.br.apiradar.shared.domain.exceptions.MaxAreaExceededException;
import com.ass.br.apiradar.shared.domain.model.AoiId;
import com.ass.br.apiradar.shared.domain.model.BoundingBox;
import com.ass.br.apiradar.shared.domain.model.OrganizationId;
import com.ass.br.apiradar.shared.domain.model.PolygonGeometry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AreaOfInterestTest {

    private static final String MACEIO_MUTANGE_WKT =
            "POLYGON((-35.795 -9.680, -35.710 -9.680, -35.710 -9.600, -35.795 -9.600, -35.795 -9.680))";

    @Test
    @DisplayName("Cria com sucesso uma AOI válida dentro dos limites contratuais de área")
    void shouldCreateValidAoi() {
        AoiId aoiId = AoiId.random();
        OrganizationId orgId = OrganizationId.random();
        PolygonGeometry geometry = PolygonGeometry.fromWkt(MACEIO_MUTANGE_WKT);

        AreaOfInterest aoi = AreaOfInterest.create(
                aoiId,
                orgId,
                "Piloto Maceió - Mutange",
                geometry,
                150.0 // Teto contratual suficiente
        );

        assertThat(aoi).isNotNull();
        assertThat(aoi.getId()).isEqualTo(aoiId);
        assertThat(aoi.getOrganizationId()).isEqualTo(orgId);
        assertThat(aoi.getName()).isEqualTo("Piloto Maceió - Mutange");
        assertThat(aoi.getStatus()).isEqualTo(AoiStatus.ACTIVE);
        assertThat(aoi.getMetricArea().valueKm2()).isGreaterThan(0.0);

        BoundingBox bbox = aoi.getBoundingBox();
        assertThat(bbox.minLon()).isEqualTo(-35.795);
        assertThat(bbox.minLat()).isEqualTo(-9.680);
        assertThat(bbox.maxLon()).isEqualTo(-35.710);
        assertThat(bbox.maxLat()).isEqualTo(-9.600);
    }

    @Test
    @DisplayName("Rejeita AOI com área métrica superior ao teto contratual estipulado")
    void shouldThrowExceptionWhenAreaExceedsContractualLimit() {
        AoiId aoiId = AoiId.random();
        OrganizationId orgId = OrganizationId.random();
        PolygonGeometry geometry = PolygonGeometry.fromWkt(MACEIO_MUTANGE_WKT);

        // Área do polígono de Maceió é ~80-90 km2, logo com limite de 10 km2 deve falhar
        assertThatThrownBy(() -> AreaOfInterest.create(
                aoiId,
                orgId,
                "Área Gigante",
                geometry,
                10.0 // Teto reduzido de 10 km2
        )).isInstanceOf(MaxAreaExceededException.class)
          .hasMessageContaining("excede o teto contratual");
    }

    @Test
    @DisplayName("Rejeita geometria WKT sintaticamente nula ou em branco")
    void shouldRejectBlankOrNullWkt() {
        assertThatThrownBy(() -> PolygonGeometry.fromWkt(null))
                .isInstanceOf(InvalidGeometryException.class);

        assertThatThrownBy(() -> PolygonGeometry.fromWkt("   "))
                .isInstanceOf(InvalidGeometryException.class);
    }

    @Test
    @DisplayName("Rejeita geometria que não seja um polígono fechado")
    void shouldRejectNonPolygonOrUnclosedWkt() {
        // Polígono não fechado (primeiro e último pontos divergentes)
        String unclosedWkt = "POLYGON((-35.795 -9.680, -35.710 -9.680, -35.710 -9.600, -35.795 -9.600))";

        assertThatThrownBy(() -> PolygonGeometry.fromWkt(unclosedWkt))
                .isInstanceOf(InvalidGeometryException.class);
    }

    @Test
    @DisplayName("Rejeita AOI com nome menor que 3 caracteres")
    void shouldRejectAoiWithInvalidName() {
        AoiId aoiId = AoiId.random();
        OrganizationId orgId = OrganizationId.random();
        PolygonGeometry geometry = PolygonGeometry.fromWkt(MACEIO_MUTANGE_WKT);

        assertThatThrownBy(() -> AreaOfInterest.create(aoiId, orgId, "AB", geometry, 100.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 3 characters");
    }
}
