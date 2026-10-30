package com.ass.br.apiradar.interferometry.domain.model.processing;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Entidade imutável que representa uma requisição versionada de processamento InSAR.
 * Contém rastreabilidade completa (idempotency key, correlation ID, cenas, bursts,
 * parâmetros, versão do pipeline e payload auditável formatado para o provedor).
 */
public record InSarProcessingRequest(
        UUID requestId,
        String idempotencyKey,
        String correlationId,
        String aoiId,
        String pairId,
        String referenceScene,
        String secondaryScene,
        List<String> selectedBursts,
        ProcessingModality modality,
        InSarProcessingParameters parameters,
        String pipelineVersion,
        String evaluationProfile,
        int estimatedCredits,
        boolean dryRun,
        String auditPayloadJson,
        Instant createdAt
) implements Serializable {

    public InSarProcessingRequest {
        if (requestId == null) {
            throw new IllegalArgumentException("RequestId não pode ser nulo.");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("IdempotencyKey não pode ser nula ou vazia.");
        }
        if (correlationId == null || correlationId.isBlank()) {
            throw new IllegalArgumentException("CorrelationId não pode ser nulo ou vazio.");
        }
        if (aoiId == null || aoiId.isBlank()) {
            throw new IllegalArgumentException("AoiId não pode ser nulo ou vazio.");
        }
        if (pairId == null || pairId.isBlank()) {
            throw new IllegalArgumentException("PairId não pode ser nulo ou vazio.");
        }
        if (referenceScene == null || referenceScene.isBlank()) {
            throw new IllegalArgumentException("Cena de referência não pode ser nula.");
        }
        if (secondaryScene == null || secondaryScene.isBlank()) {
            throw new IllegalArgumentException("Cena secundária não pode ser nula.");
        }
        if (modality == null) {
            throw new IllegalArgumentException("Modalidade de processamento não pode ser nula.");
        }
        if (parameters == null) {
            throw new IllegalArgumentException("Parâmetros de processamento não podem ser nulos.");
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        selectedBursts = selectedBursts == null ? Collections.emptyList() : List.copyOf(selectedBursts);
    }

    /**
     * Calcula uma Idempotency Key determinística (SHA-256) a partir dos parâmetros invariantes da requisição.
     */
    public static String calculateIdempotencyKey(
            String aoiId,
            String pairId,
            ProcessingModality modality,
            List<String> bursts,
            InSarProcessingParameters params
    ) {
        try {
            String burstString = bursts != null ? String.join(",", bursts) : "";
            String raw = String.format("%s|%s|%s|%s|%s|%b|%.2f|%d",
                    aoiId,
                    pairId,
                    modality.name(),
                    burstString,
                    params.looks(),
                    params.applyWaterMask(),
                    params.filterStrength(),
                    params.demMatchingResolutionMeters()
            );
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 não disponível no ambiente Java.", e);
        }
    }
}
