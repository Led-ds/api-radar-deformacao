package com.ass.br.apiradar.shared.domain.model;

import com.ass.br.apiradar.shared.domain.exceptions.InvalidGeometryException;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.WKTReader;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Objeto de valor que encapsula um polígono geográfico WGS 84 (EPSG:4326),
 * garantindo invariantes topológicas OGC (anel fechado, sem auto-interseção, coordenadas válidas).
 */
public record PolygonGeometry(String wkt, Polygon jtsPolygon) implements Serializable {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    public static PolygonGeometry fromWkt(String wkt) {
        if (wkt == null || wkt.isBlank()) {
            throw new InvalidGeometryException("WKT geometry cannot be null or empty");
        }
        try {
            WKTReader reader = new WKTReader(GF);
            Geometry geom = reader.read(wkt.trim());

            if (!(geom instanceof Polygon polygon)) {
                throw new InvalidGeometryException("Geometry must be a 2D POLYGON, received: " + geom.getGeometryType());
            }

            validatePolygon(polygon);
            polygon.setSRID(4326);
            return new PolygonGeometry(polygon.toText(), polygon);
        } catch (InvalidGeometryException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidGeometryException("Failed to parse WKT polygon: " + e.getMessage(), e);
        }
    }

    public static PolygonGeometry fromJts(Polygon polygon) {
        if (polygon == null) {
            throw new InvalidGeometryException("JTS polygon cannot be null");
        }
        validatePolygon(polygon);
        polygon.setSRID(4326);
        return new PolygonGeometry(polygon.toText(), polygon);
    }

    /**
     * Constrói e valida uma geometria a partir de uma estrutura GeoJSON (Geometry ou Feature).
     */
    @SuppressWarnings("unchecked")
    public static PolygonGeometry fromGeoJson(Map<String, Object> geoJson) {
        if (geoJson == null || geoJson.isEmpty()) {
            throw new InvalidGeometryException("GeoJSON geometry cannot be null or empty");
        }

        try {
            Map<String, Object> geomMap = geoJson;
            if ("Feature".equalsIgnoreCase((String) geoJson.get("type"))) {
                Object geometryObj = geoJson.get("geometry");
                if (geometryObj instanceof Map) {
                    geomMap = (Map<String, Object>) geometryObj;
                } else {
                    throw new InvalidGeometryException("GeoJSON Feature does not contain a valid geometry object");
                }
            }

            String type = (String) geomMap.get("type");
            if (!"Polygon".equalsIgnoreCase(type)) {
                throw new InvalidGeometryException("GeoJSON geometry must be of type 'Polygon', received: " + type);
            }

            Object coordsObj = geomMap.get("coordinates");
            if (!(coordsObj instanceof List<?> rings) || rings.isEmpty()) {
                throw new InvalidGeometryException("GeoJSON coordinates must be a non-empty list of linear rings");
            }

            Object outerRingObj = rings.get(0);
            if (!(outerRingObj instanceof List<?> outerRingList) || outerRingList.size() < 4) {
                throw new InvalidGeometryException("GeoJSON outer ring must contain at least 4 coordinates (closed ring with >= 3 distinct points)");
            }

            Coordinate[] coordinates = new Coordinate[outerRingList.size()];
            for (int i = 0; i < outerRingList.size(); i++) {
                Object ptObj = outerRingList.get(i);
                if (!(ptObj instanceof List<?> ptList) || ptList.size() < 2) {
                    throw new InvalidGeometryException("Invalid point at index " + i + " in GeoJSON ring");
                }
                double lon = ((Number) ptList.get(0)).doubleValue();
                double lat = ((Number) ptList.get(1)).doubleValue();
                coordinates[i] = new Coordinate(lon, lat);
            }

            LinearRing shell = GF.createLinearRing(coordinates);
            LinearRing[] holes = null;
            if (rings.size() > 1) {
                holes = new LinearRing[rings.size() - 1];
                for (int r = 1; r < rings.size(); r++) {
                    Object holeObj = rings.get(r);
                    if (!(holeObj instanceof List<?> holeList) || holeList.size() < 4) {
                        throw new InvalidGeometryException("GeoJSON interior ring (hole) must contain at least 4 coordinates");
                    }
                    Coordinate[] holeCoords = new Coordinate[holeList.size()];
                    for (int i = 0; i < holeList.size(); i++) {
                        Object ptObj = holeList.get(i);
                        if (!(ptObj instanceof List<?> ptList) || ptList.size() < 2) {
                            throw new InvalidGeometryException("Invalid point at index " + i + " in hole ring");
                        }
                        double lon = ((Number) ptList.get(0)).doubleValue();
                        double lat = ((Number) ptList.get(1)).doubleValue();
                        holeCoords[i] = new Coordinate(lon, lat);
                    }
                    holes[r - 1] = GF.createLinearRing(holeCoords);
                }
            }
            Polygon polygon = GF.createPolygon(shell, holes);
            validatePolygon(polygon);
            polygon.setSRID(4326);

            return new PolygonGeometry(polygon.toText(), polygon);
        } catch (InvalidGeometryException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidGeometryException("Failed to parse GeoJSON polygon: " + e.getMessage(), e);
        }
    }

    /**
     * Valida coordenadas geográficas (WGS84), auto-interseção, vértices repetidos,
     * segmentos colineares e anel fechado.
     */
    private static void validatePolygon(Polygon polygon) {
        if (polygon.isEmpty()) {
            throw new InvalidGeometryException("Geometry cannot be empty");
        }

        if (!polygon.getExteriorRing().isClosed()) {
            throw new InvalidGeometryException("Polygon ring must be closed (first and last coordinates must match)");
        }

        if (polygon.getExteriorRing().getNumPoints() < 4) {
            throw new InvalidGeometryException("Polygon must have at least 3 distinct vertices (plus closure point)");
        }

        Coordinate[] coords = polygon.getCoordinates();

        // 1. Coordenadas fora dos limites [-180, 180] e [-90, 90]
        for (Coordinate coord : coords) {
            if (coord.x < -180.0 || coord.x > 180.0) {
                throw new InvalidGeometryException("Longitude fora dos limites permitidos [-180, 180]: " + coord.x);
            }
            if (coord.y < -90.0 || coord.y > 90.0) {
                throw new InvalidGeometryException("Latitude fora dos limites permitidos [-90, 90]: " + coord.y);
            }
        }

        // 2. Vértices consecutivos repetidos
        for (int i = 0; i < coords.length - 1; i++) {
            if (coords[i].equals2D(coords[i + 1])) {
                throw new InvalidGeometryException("Vértices consecutivos duplicados detectados na geometria: " + coords[i]);
            }
        }

        // 3. Segmentos colineares consecutivos
        for (int i = 0; i < coords.length - 2; i++) {
            if (areCollinear(coords[i], coords[i + 1], coords[i + 2])) {
                throw new InvalidGeometryException("Segmentos colineares redundantes detectados na geometria entre os vértices: "
                        + coords[i] + ", " + coords[i + 1] + ", " + coords[i + 2]);
            }
        }

        // 4. Polígono com área nula / colapsado
        if (polygon.getArea() <= 1e-11) {
            throw new InvalidGeometryException("Geometria degenerada ou colinear detectada (área nula).");
        }

        // 5. Auto-interseção e validade topológica JTS (formato gravata/8, anel que se cruza)
        if (!polygon.isValid()) {
            throw new InvalidGeometryException("Geometry is topologically invalid: self-intersection or open ring detected");
        }
    }

    private static boolean areCollinear(Coordinate p1, Coordinate p2, Coordinate p3) {
        double crossProduct = (p2.x - p1.x) * (p3.y - p1.y) - (p2.y - p1.y) * (p3.x - p1.x);
        return Math.abs(crossProduct) < 1e-11;
    }

    /**
     * Converte o polígono JTS para uma estrutura GeoJSON nativa (type: Polygon).
     */
    public Map<String, Object> toGeoJson() {
        List<List<List<Double>>> rings = new ArrayList<>();
        List<List<Double>> shellCoords = new ArrayList<>();
        for (Coordinate c : jtsPolygon.getExteriorRing().getCoordinates()) {
            shellCoords.add(List.of(c.x, c.y));
        }
        rings.add(shellCoords);
        for (int i = 0; i < jtsPolygon.getNumInteriorRing(); i++) {
            List<List<Double>> holeCoords = new ArrayList<>();
            for (Coordinate c : jtsPolygon.getInteriorRingN(i).getCoordinates()) {
                holeCoords.add(List.of(c.x, c.y));
            }
            rings.add(holeCoords);
        }
        return Map.of(
                "type", "Polygon",
                "coordinates", rings
        );
    }

    /**
     * Calcula a Bounding Box (BBOX) WGS 84 a partir dos limites do polígono.
     */
    public BoundingBox calculateBoundingBox() {
        Envelope env = jtsPolygon.getEnvelopeInternal();
        return new BoundingBox(env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY());
    }

    /**
     * Calcula a área métrica geodésica em km² considerando a curvatura do elipsoide WGS 84,
     * compatibilizando a precisão com a função ST_Area(geom::geography) do PostGIS.
     */
    public MetricArea calculateApproximateMetricArea() {
        Envelope env = jtsPolygon.getEnvelopeInternal();
        double midLat = (env.getMinY() + env.getMaxY()) / 2.0;
        double latRad = Math.toRadians(midLat);
        double sinLat = Math.sin(latRad);
        double cosLat = Math.cos(latRad);

        // Parâmetros do Elipsoide WGS 84 (semi-eixo maior a e excentricidade e²)
        double a = 6378137.0;
        double e2 = 0.00669437999014;

        double denom = Math.sqrt(1.0 - e2 * sinLat * sinLat);
        double m = a * (1.0 - e2) / (denom * denom * denom); // raio de curvatura meridional
        double n = a / denom; // raio de curvatura normal

        // Comprimento em km de um grau geodésico
        double kmPerDegreeLat = m * Math.PI / (180.0 * 1000.0);
        double kmPerDegreeLon = n * cosLat * Math.PI / (180.0 * 1000.0);

        double areaDegrees2 = jtsPolygon.getArea();
        double areaKm2 = areaDegrees2 * kmPerDegreeLat * kmPerDegreeLon;

        // Limita a um valor mínimo positivo
        return new MetricArea(Math.max(areaKm2, 0.0001));
    }
}
