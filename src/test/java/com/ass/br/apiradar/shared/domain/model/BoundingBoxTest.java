package com.ass.br.apiradar.shared.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoundingBoxTest {

    @Test
    @DisplayName("Cria com sucesso BoundingBox válida e gera WKT de polígono")
    void shouldCreateValidBoundingBox() {
        BoundingBox bbox = new BoundingBox(-35.795, -9.680, -35.710, -9.600);
        assertThat(bbox.minLon()).isEqualTo(-35.795);
        assertThat(bbox.minLat()).isEqualTo(-9.680);
        assertThat(bbox.maxLon()).isEqualTo(-35.710);
        assertThat(bbox.maxLat()).isEqualTo(-9.600);

        String wkt = bbox.toPolygonWkt();
        assertThat(wkt).startsWith("POLYGON((");
        assertThat(wkt).endsWith("))");
    }

    @Test
    @DisplayName("Rejeita BoundingBox com minLon > maxLon ou minLat > maxLat")
    void shouldRejectInvertedCoordinates() {
        assertThatThrownBy(() -> new BoundingBox(-35.700, -9.680, -35.800, -9.600))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new BoundingBox(-35.795, -9.500, -35.710, -9.600))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
