package com.ass.br.apiradar.interferometry.domain.model.processing;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Resultado da validação de elegibilidade e conformidade prévia de um par
 * contra os requisitos e regras operacionais do processador científico InSAR.
 */
public record ProcessorEligibilityResult(
        String pairId,
        String processorName,
        boolean eligible,
        List<String> validationDetails,
        List<ProcessingModality> supportedModalities,
        Map<ProcessingModality, Integer> estimatedCreditsByModality,
        Instant evaluatedAt
) implements Serializable {

    public ProcessorEligibilityResult {
        validationDetails = validationDetails == null ? List.of() : List.copyOf(validationDetails);
        supportedModalities = supportedModalities == null ? List.of() : List.copyOf(supportedModalities);
        estimatedCreditsByModality = estimatedCreditsByModality == null ? Map.of() : Map.copyOf(estimatedCreditsByModality);
        if (evaluatedAt == null) {
            evaluatedAt = Instant.now();
        }
    }
}
