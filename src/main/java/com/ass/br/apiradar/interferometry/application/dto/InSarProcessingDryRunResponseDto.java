package com.ass.br.apiradar.interferometry.application.dto;

import com.ass.br.apiradar.interferometry.domain.model.processing.InSarProcessingRequest;
import com.ass.br.apiradar.interferometry.domain.model.processing.ProcessingModality;

import java.time.Instant;
import java.util.List;

/**
 * DTO de saída que expõe o pedido auditável gerado em modo dry-run,
 * os parâmetros canônicos do provedor, custo comprovado em créditos e confirmação de bloqueio financeiro.
 */
public record InSarProcessingDryRunResponseDto(
        String requestId,
        String idempotencyKey,
        String correlationId,
        String aoiId,
        String pairId,
        String referenceScene,
        String secondaryScene,
        List<String> selectedBursts,
        ProcessingModality modality,
        String officialJobType,
        String pipelineVersion,
        String evaluationProfile,
        int estimatedCredits,
        String creditCostSource,
        String validationStatus,
        String coverageEvaluation,
        boolean dryRun,
        boolean executionBlockedBySecurityPolicy,
        String auditPayloadJson,
        Instant createdAt
) {
    public static InSarProcessingDryRunResponseDto fromDomain(
            InSarProcessingRequest request,
            boolean executionBlocked,
            String validationStatus,
            String coverageEvaluation
    ) {
        return new InSarProcessingDryRunResponseDto(
                request.requestId().toString(),
                request.idempotencyKey(),
                request.correlationId(),
                request.aoiId(),
                request.pairId(),
                request.referenceScene(),
                request.secondaryScene(),
                request.selectedBursts(),
                request.modality(),
                request.modality().getOfficialJobType(),
                request.pipelineVersion(),
                request.evaluationProfile(),
                request.estimatedCredits(),
                "https://hyp3-docs.asf.alaska.edu/using/credits/",
                validationStatus != null ? validationStatus : "READY_FOR_DRY_RUN",
                coverageEvaluation != null ? coverageEvaluation : "Cobertura sob validação científica.",
                request.dryRun(),
                executionBlocked,
                request.auditPayloadJson(),
                request.createdAt()
        );
    }
}
