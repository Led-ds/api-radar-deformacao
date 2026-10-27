package com.ass.br.apiradar.shared.domain.model;

import java.io.Serializable;

/**
 * Objeto de valor que representa a Bounding Box (BBOX) geográfica em WGS 84 (EPSG:4326).
 */
public record BoundingBox(double minLon, double minLat, double maxLon, double maxLat) implements Serializable {

    public BoundingBox {
        if (minLon > maxLon) {
            throw new IllegalArgumentException("minLon (" + minLon + ") cannot be greater than maxLon (" + maxLon + ")");
        }
        if (minLat > maxLat) {
            throw new IllegalArgumentException("minLat (" + minLat + ") cannot be greater than maxLat (" + maxLat + ")");
        }
        if (minLon < -180.0 || maxLon > 180.0) {
            throw new IllegalArgumentException("Longitude values must be within [-180, 180]");
        }
        if (minLat < -90.0 || maxLat > 90.0) {
            throw new IllegalArgumentException("Latitude values must be within [-90, 90]");
        }
    }

    /**
     * Retorna a string no formato WKT de um polígono retangular correspondente à BBOX.
     */
    public String toPolygonWkt() {
        return String.format(java.util.Locale.US,
                "POLYGON((%f %f, %f %f, %f %f, %f %f, %f %f))",
                minLon, minLat,
                maxLon, minLat,
                maxLon, maxLat,
                minLon, maxLat,
                minLon, minLat);
    }
}
