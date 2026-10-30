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

    @Test
    @DisplayName("Deve calcular área geodésica elipsoidal WGS 84 compatível com ST_Area(geography) do PostGIS para AOI de Maceió")
    void shouldCalculateGeodesicAreaConsistentWithPostGis() {
        PolygonGeometry geom = PolygonGeometry.fromWkt(VALID_MACEIO_WKT);
        MetricArea area = geom.calculateApproximateMetricArea();

        // O cálculo geodésico no elipsoide WGS 84 deve convergir para 10.9254 km² (PostGIS ST_Area)
        assertEquals(10.93, area.rounded(2), "Área arredondada a 2 casas decimais deve ser 10.93 km²");
        assertEquals(10.9254, area.valueKm2(), 0.002, "Área geodésica contínua deve coincidir com o valor do PostGIS");
    }

    @Test
    @DisplayName("Deve calcular área para polígonos irregulares (triângulos e pentágonos)")
    void shouldCalculateAreaForIrregularPolygons() {
        // Triângulo irregular em Maceió
        String triangleWkt = "POLYGON((-35.75 -9.67, -35.70 -9.65, -35.73 -9.60, -35.75 -9.67))";
        PolygonGeometry triangle = PolygonGeometry.fromWkt(triangleWkt);
        assertNotNull(triangle);
        MetricArea triArea = triangle.calculateApproximateMetricArea();
        assertTrue(triArea.valueKm2() > 0);
        // PostGIS ST_Area(geom::geography) calcula 18.816 km²
        assertEquals(18.82, triArea.rounded(2), "Área do triângulo irregular deve ser consistente com PostGIS");

        // Pentágono irregular
        String pentagonWkt = "POLYGON((-35.75 -9.67, -35.71 -9.68, -35.69 -9.64, -35.72 -9.61, -35.76 -9.63, -35.75 -9.67))";
        PolygonGeometry pentagon = PolygonGeometry.fromWkt(pentagonWkt);
        assertNotNull(pentagon);
        MetricArea pentArea = pentagon.calculateApproximateMetricArea();
        assertTrue(pentArea.valueKm2() > 0);
    }

    @Test
    @DisplayName("Deve calcular área invariante em relação à orientação do anel (horário vs anti-horário)")
    void shouldBeOrientationInvariant() {
        String ccwWkt = "POLYGON((-35.75 -9.67, -35.70 -9.65, -35.73 -9.60, -35.75 -9.67))";
        String cwWkt = "POLYGON((-35.75 -9.67, -35.73 -9.60, -35.70 -9.65, -35.75 -9.67))";

        PolygonGeometry ccw = PolygonGeometry.fromWkt(ccwWkt);
        PolygonGeometry cw = PolygonGeometry.fromWkt(cwWkt);

        assertEquals(ccw.calculateApproximateMetricArea().rounded(4),
                cw.calculateApproximateMetricArea().rounded(4),
                "A área deve ser idêntica independentemente da orientação horária ou anti-horária dos vértices");
    }

    @Test
    @DisplayName("Deve rejeitar polígonos com anel aberto (primeiro e último ponto diferentes)")
    void shouldRejectOpenRings() {
        String openWkt = "POLYGON((-35.75 -9.67, -35.72 -9.67, -35.72 -9.64, -35.75 -9.64))";
        assertThrows(InvalidGeometryException.class, () -> PolygonGeometry.fromWkt(openWkt));
    }

    @Test
    @DisplayName("Deve suportar polígonos com orifícios (interior rings / holes) em WKT e GeoJSON")
    void shouldSupportPolygonsWithHoles() {
        // Polígono externo 0.05 x 0.07 graus com orifício central 0.03 x 0.05 graus
        String withHoleWkt = "POLYGON((-35.75 -9.67, -35.70 -9.67, -35.70 -9.60, -35.75 -9.60, -35.75 -9.67), " +
                "(-35.74 -9.66, -35.71 -9.66, -35.71 -9.61, -35.74 -9.61, -35.74 -9.66))";

        PolygonGeometry geom = PolygonGeometry.fromWkt(withHoleWkt);
        assertNotNull(geom);
        assertEquals(1, geom.jtsPolygon().getNumInteriorRing(), "Deve conter 1 anel interno");

        MetricArea areaWithHole = geom.calculateApproximateMetricArea();
        // A área com furo (PostGIS: 24.28 km²) deve ser menor que a área sem o furo (PostGIS: 41.52 km²)
        assertTrue(areaWithHole.valueKm2() > 20.0 && areaWithHole.valueKm2() < 30.0);
        assertEquals(24.28, areaWithHole.rounded(2));

        // Teste de ciclo de conversão GeoJSON com orifício
        Map<String, Object> geoJson = geom.toGeoJson();
        PolygonGeometry reconstructed = PolygonGeometry.fromGeoJson(geoJson);
        assertEquals(1, reconstructed.jtsPolygon().getNumInteriorRing());
        assertEquals(areaWithHole.rounded(2), reconstructed.calculateApproximateMetricArea().rounded(2));
    }
}
