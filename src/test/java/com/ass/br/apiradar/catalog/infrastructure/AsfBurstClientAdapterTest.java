package com.ass.br.apiradar.catalog.infrastructure;

import com.ass.br.apiradar.catalog.domain.model.SarBurst;
import com.ass.br.apiradar.catalog.infrastructure.adapters.output.external.AsfBurstClientAdapter;
import com.ass.br.apiradar.shared.domain.exceptions.ExternalServiceException;
import com.ass.br.apiradar.shared.domain.model.BoundingBox;
import com.ass.br.apiradar.shared.domain.model.PolygonGeometry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Infraestrutura: AsfBurstClientAdapter")
class AsfBurstClientAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String MACEIO_WKT =
            "POLYGON((-35.75 -9.67, -35.72 -9.67, -35.72 -9.64, -35.75 -9.64, -35.75 -9.67))";

    @Test
    @DisplayName("Retorna bursts de teste da ASF para Maceió quando mockEnabled=true")
    void shouldReturnMockBurstsWhenMockEnabled() {
        AsfBurstClientAdapter adapter = new AsfBurstClientAdapter(
                "https://fake-url.invalid",
                true,
                objectMapper
        );

        PolygonGeometry geom = PolygonGeometry.fromWkt(MACEIO_WKT);
        BoundingBox bbox = geom.calculateBoundingBox();

        List<SarBurst> bursts04 = adapter.searchBursts(geom, bbox, LocalDate.of(2023, 1, 4), 9, "DESCENDING", "VV");
        assertEquals(2, bursts04.size());
        assertTrue(bursts04.stream().anyMatch(b -> b.canonicalId().contains("018319")));
        assertTrue(bursts04.stream().anyMatch(b -> b.canonicalId().contains("018318")));
        assertEquals("IW1", bursts04.get(0).subswath());
        assertNotNull(bursts04.get(0).footprint());

        List<SarBurst> bursts16 = adapter.searchBursts(geom, bbox, LocalDate.of(2023, 1, 16), 9, "DESCENDING", "VV");
        assertEquals(2, bursts16.size());
        assertTrue(bursts16.stream().anyMatch(b -> b.canonicalId().contains("018319")));
        assertTrue(bursts16.stream().anyMatch(b -> b.canonicalId().contains("018318")));
    }

    @Test
    @DisplayName("Retorna lista vazia para datas sem bursts no modo mock")
    void shouldReturnEmptyForDatesWithoutBursts() {
        AsfBurstClientAdapter adapter = new AsfBurstClientAdapter(
                "https://fake-url.invalid",
                true,
                objectMapper
        );

        PolygonGeometry geom = PolygonGeometry.fromWkt(MACEIO_WKT);
        List<SarBurst> bursts = adapter.searchBursts(geom, geom.calculateBoundingBox(), LocalDate.of(2023, 5, 20), 9, "DESCENDING", "VV");
        assertTrue(bursts.isEmpty());
    }

    @Test
    @DisplayName("Lança ExternalServiceException quando mockEnabled=false e endpoint inalcançável")
    void shouldThrowExternalServiceExceptionWhenOfflineAndMockDisabled() {
        AsfBurstClientAdapter adapter = new AsfBurstClientAdapter(
                "http://localhost:59998",
                false,
                objectMapper
        );

        PolygonGeometry geom = PolygonGeometry.fromWkt(MACEIO_WKT);
        assertThrows(ExternalServiceException.class, () ->
                adapter.searchBursts(geom, geom.calculateBoundingBox(), LocalDate.of(2023, 1, 4), 9, "DESCENDING", "VV")
        );
    }
}
