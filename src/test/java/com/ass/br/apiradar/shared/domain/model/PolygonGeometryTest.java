package com.ass.br.apiradar.shared.domain.model;

import com.ass.br.apiradar.shared.domain.exceptions.InvalidGeometryException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes Unitários - PolygonGeometry (WKT e GeoJSON)")
class PolygonGeometryTest {

    private static final String VALID_MACEIO_WKT =
            "POLYGON((-35.75 -9.67, -35.72 -9.67, -35.72 -9.64, -35.75 -9.64, -35.75 -9.67))";

    @Test
    @DisplayName("Deve construir com sucesso a partir de WKT válido")
    void shouldCreateFromValidWkt() {
        PolygonGeometry geom = PolygonGeometry.fromWkt(VALID_MACEIO_WKT);
        assertNotNull(geom);
        assertNotNull(geom.jtsPolygon());
        assertEquals(4326, geom.jtsPolygon().getSRID());

        BoundingBox bbox = geom.calculateBoundingBox();
        assertEquals(-35.75, bbox.minLon(), 0.001);
        assertEquals(-9.67, bbox.minLat(), 0.001);
    }

    @Test
    @DisplayName("Deve rejeitar polígono com coordenadas fora dos limites globais")
    void shouldRejectCoordinatesOutOfBounds() {
        String invalidLonWkt = "POLYGON((-195.0 -9.67, -35.72 -9.67, -35.72 -9.64, -195.0 -9.64, -195.0 -9.67))";
        assertThrows(InvalidGeometryException.class, () -> PolygonGeometry.fromWkt(invalidLonWkt));

        String invalidLatWkt = "POLYGON((-35.75 -95.0, -35.72 -95.0, -35.72 -9.64, -35.75 -9.64, -35.75 -95.0))";
        assertThrows(InvalidGeometryException.class, () -> PolygonGeometry.fromWkt(invalidLatWkt));
    }

    @Test
    @DisplayName("Deve rejeitar polígono com auto-interseção (formato gravata/8)")
    void shouldRejectSelfIntersectingPolygon() {
        String bowTieWkt = "POLYGON((0 0, 2 2, 2 0, 0 2, 0 0))";
        assertThrows(InvalidGeometryException.class, () -> PolygonGeometry.fromWkt(bowTieWkt));
    }

    @Test
    @DisplayName("Deve serializar e desserializar GeoJSON com integridade")
    void shouldConvertBidirectionallyWithGeoJson() {
        PolygonGeometry geom = PolygonGeometry.fromWkt(VALID_MACEIO_WKT);
        Map<String, Object> geoJson = geom.toGeoJson();

        assertNotNull(geoJson);
        assertEquals("Polygon", geoJson.get("type"));
        assertTrue(geoJson.containsKey("coordinates"));

        // Desserializa de volta para PolygonGeometry
        PolygonGeometry fromGeoJson = PolygonGeometry.fromGeoJson(geoJson);
        assertNotNull(fromGeoJson);
        assertEquals(4326, fromGeoJson.jtsPolygon().getSRID());

        // Áreas devem coincidir
        assertEquals(geom.calculateApproximateMetricArea().rounded(2),
                fromGeoJson.calculateApproximateMetricArea().rounded(2));
    }

    @Test
    @DisplayName("Deve permitir desserialização a partir de GeoJSON Feature")
    void shouldParseFromGeoJsonFeature() {
        Map<String, Object> geometry = Map.of(
                "type", "Polygon",
                "coordinates", List.of(List.of(
                        List.of(-35.75, -9.67),
                        List.of(-35.72, -9.67),
                        List.of(-35.72, -9.64),
                        List.of(-35.75, -9.64),
                        List.of(-35.75, -9.67)
                ))
        );
        Map<String, Object> feature = Map.of(
                "type", "Feature",
                "geometry", geometry,
                "properties", Map.of("name", "Maceio Feature Test")
        );

        PolygonGeometry geom = PolygonGeometry.fromGeoJson(feature);
        assertNotNull(geom);
        assertEquals(4326, geom.jtsPolygon().getSRID());
    }

    @Test
    @DisplayName("Deve rejeitar polígono com vértices consecutivos repetidos")
    void shouldRejectRepeatedVertices() {
        String repeatedWkt = "POLYGON((-35.75 -9.67, -35.72 -9.67, -35.72 -9.67, -35.72 -9.64, -35.75 -9.64, -35.75 -9.67))";
        InvalidGeometryException ex = assertThrows(InvalidGeometryException.class,
                () -> PolygonGeometry.fromWkt(repeatedWkt));
        assertTrue(ex.getMessage().contains("duplicados"));
    }

    @Test
    @DisplayName("Deve rejeitar polígono com segmentos colineares ou geometria colapsada")
    void shouldRejectCollinearSegments() {
        // Vértice intermediário redundante em linha reta (segmentos colineares)
        String collinearEdgeWkt = "POLYGON((0 0, 1 0, 2 0, 2 2, 0 2, 0 0))";
        InvalidGeometryException ex1 = assertThrows(InvalidGeometryException.class,
                () -> PolygonGeometry.fromWkt(collinearEdgeWkt));
        assertTrue(ex1.getMessage().contains("colineares"));

        // Polígono colapsado em uma linha (área nula)
        String collapsedLineWkt = "POLYGON((0 0, 1 1, 2 2, 0 0))";
        assertThrows(InvalidGeometryException.class,
                () -> PolygonGeometry.fromWkt(collapsedLineWkt));
    }

    @Test
    @DisplayName("Deve rejeitar geometrias vazias ou nulas em WKT e GeoJSON")
    void shouldRejectEmptyGeometries() {
        assertThrows(InvalidGeometryException.class, () -> PolygonGeometry.fromWkt(""));
        assertThrows(InvalidGeometryException.class, () -> PolygonGeometry.fromWkt("   "));
        assertThrows(InvalidGeometryException.class, () -> PolygonGeometry.fromWkt(null));
        assertThrows(InvalidGeometryException.class, () -> PolygonGeometry.fromWkt("POLYGON EMPTY"));
        assertThrows(InvalidGeometryException.class, () -> PolygonGeometry.fromGeoJson(Map.of()));
        assertThrows(InvalidGeometryException.class, () -> PolygonGeometry.fromGeoJson(null));
    }
}
