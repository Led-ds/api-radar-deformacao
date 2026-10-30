package com.ass.br.apiradar.catalog.domain.model;

import java.io.Serializable;
import java.time.Instant;

/**
 * Representa uma cena/granulação SAR (ex.: Sentinel-1 IW SLC) catalogada,
 * contendo metadados científicos reais, identificador externo e fonte de dados.
 */
public record SarScene(
        String sceneName,
        Instant acquisitionDate,
        Integer relativeOrbit,
        String flightDirection,
        String polarization,
        String sensor,
        String externalId,
        String metadataSource,
        String footprintWkt
) implements Serializable {

    public SarScene(String sceneName,
                    Instant acquisitionDate,
                    Integer relativeOrbit,
                    String flightDirection,
                    String polarization,
                    String sensor) {
        this(sceneName, acquisitionDate, relativeOrbit, flightDirection, polarization, sensor, sceneName, "NASA ASF DAAC", null);
    }

    public SarScene(String sceneName,
                    Instant acquisitionDate,
                    Integer relativeOrbit,
                    String flightDirection,
                    String polarization,
                    String sensor,
                    String externalId,
                    String metadataSource) {
        this(sceneName, acquisitionDate, relativeOrbit, flightDirection, polarization, sensor, externalId, metadataSource, null);
    }
}
