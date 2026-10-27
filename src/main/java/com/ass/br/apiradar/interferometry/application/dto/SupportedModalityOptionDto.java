package com.ass.br.apiradar.interferometry.application.dto;

import com.ass.br.apiradar.interferometry.domain.model.processing.ProcessingModality;

import java.io.Serializable;

/**
 * Opção de modalidade de processamento InSAR elegível com créditos e cobertura calculados.
 */
public record SupportedModalityOptionDto(
        ProcessingModality modality,
        String officialJobType,
        String pipelineVersion,
        int estimatedCredits,
        String description,
        Double nominalCoveragePercentage
) implements Serializable {}
