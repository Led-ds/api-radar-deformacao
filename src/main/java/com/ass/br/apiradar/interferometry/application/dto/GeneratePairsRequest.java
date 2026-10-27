package com.ass.br.apiradar.interferometry.application.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/**
 * Requisição para descoberta de cenas e geração de pares interferométricos candidatos para uma AOI.
 */
public record GeneratePairsRequest(
        @NotBlank(message = "O identificador da Área de Interesse (aoiId) é obrigatório.")
        String aoiId,

        LocalDate startDate,
        LocalDate endDate,
        String flightDirection,
        String policyProfile,

        Integer customMinTemporalDays,
        Integer customMaxTemporalDays,
        Double customMaxPerpendicularMeters,

        String expectedGeometryWkt
) {
    public GeneratePairsRequest(String aoiId, LocalDate startDate, LocalDate endDate, String flightDirection) {
        this(aoiId, startDate, endDate, flightDirection, "TROPICAL_URBAN_MACEIO", null, null, null, null);
    }
}
