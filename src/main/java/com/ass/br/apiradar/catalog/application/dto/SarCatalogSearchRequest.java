package com.ass.br.apiradar.catalog.application.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Parâmetros de requisição para busca de cenas SAR Sentinel-1 na BBOX de uma AOI.
 * Suporta verificação de integridade espacial via expectedGeometryWkt.
 */
public record SarCatalogSearchRequest(
        @NotNull(message = "O ID da Área de Interesse (aoiId) é obrigatório.")
        UUID aoiId,

        LocalDate startDate,

        LocalDate endDate,

        String orbitDirection,

        String expectedGeometryWkt
) implements Serializable {

    public SarCatalogSearchRequest(UUID aoiId, LocalDate startDate, LocalDate endDate, String orbitDirection) {
        this(aoiId, startDate, endDate, orbitDirection, null);
    }
}
