package com.ass.br.apiradar.interferometry.application.dto;

import com.ass.br.apiradar.interferometry.domain.model.processing.ProcessingModality;

import java.util.List;

/**
 * DTO de entrada para solicitação de geração de requisição dry-run de processamento InSAR.
 * Suporta especificação por burstIds combinados ou listas explícitas referenceBursts / secondaryBursts.
 */
public record InSarProcessingDryRunRequestDto(
        String aoiId,
        ProcessingModality modality,
        List<String> burstIds,
        List<String> referenceBursts,
        List<String> secondaryBursts,
        String looks,
        Boolean applyWaterMask,
        Double filterStrength,
        Integer demResolutionMeters,
        String correlationId
) {
    public InSarProcessingDryRunRequestDto(
            String aoiId,
            ProcessingModality modality,
            List<String> burstIds,
            String looks,
            Boolean applyWaterMask,
            Double filterStrength,
            Integer demResolutionMeters,
            String correlationId
    ) {
        this(aoiId, modality, burstIds, null, null, looks, applyWaterMask, filterStrength, demResolutionMeters, correlationId);
    }
}
