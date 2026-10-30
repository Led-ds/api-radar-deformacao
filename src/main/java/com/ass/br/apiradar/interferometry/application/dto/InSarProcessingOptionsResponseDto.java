package com.ass.br.apiradar.interferometry.application.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Contrato oficial retornado pelo endpoint de opções de processamento:
 * GET /api/v1/interferometry/pairs/{pairId}/processing/options?aoiId={aoiId}
 */
public record InSarProcessingOptionsResponseDto(
        String pairId,
        String aoiId,
        String aoiName,
        double aoiMetricAreaKm2,
        String referenceSceneName,
        String secondarySceneName,
        long temporalBaselineDays,
        Double perpendicularBaselineMeters,
        String status,
        List<SupportedModalityOptionDto> modalities,
        List<CandidateBurstPairDto> candidateBurstPairs,
        MultiBurstSummaryDto multiBurstSummary,
        List<String> qualityLimitations,
        List<String> validationWarnings
) implements Serializable {}
