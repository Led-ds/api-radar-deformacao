package com.ass.br.apiradar.interferometry.application.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Resumo da composição Multi-Burst para o par interferométrico e AOI,
 * contendo as listas distintas de referência e secundária e cobertura conjunta.
 */
public record MultiBurstSummaryDto(
        boolean available,
        List<String> referenceBurstIds,
        List<String> secondaryBurstIds,
        double combinedCoveragePercentage,
        double combinedCoverageKm2,
        int estimatedCredits
) implements Serializable {}
