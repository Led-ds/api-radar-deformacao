package com.ass.br.apiradar.interferometry.application.dto;

import java.util.List;
import java.util.Map;

/**
 * Resposta oficial para a geração e avaliação de pares interferométricos candidatos.
 */
public record GeneratePairsResponseDto(
        String aoiId,
        String aoiName,
        PairSelectionPolicyDto policyUsed,
        int totalScenesDiscovered,
        int totalPairsGenerated,
        List<InterferometricPairDto> pairs,
        Map<String, Long> summaryByStatus,
        String scientificDisclaimer
) {
    public static final String DEFAULT_SCIENTIFIC_DISCLAIMER =
            "AVISO CIENTÍFICO: Os pares interferométricos listados foram avaliados exclusivamente em termos de " +
            "compatibilidade orbital, modo, polarização e linha de base temporal/perpendicular. " +
            "NENHUM processamento interferométrico (co-registro fino, geração de interferograma, desdobramento de fase ou inversão de deslocamento) " +
            "foi executado nesta etapa. Pares em estado PENDENTE DE VALIDAÇÃO aguardam publicação de órbitas precisas (POEORB) ou " +
            "confirmação da baseline perpendicular definitiva.";
}
