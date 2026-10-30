package com.ass.br.apiradar.interferometry.domain.model.job;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Métricas científicas e geométricas extraídas dos produtos interferométricos (coerência, fase, CRS e cobertura).
 */
public record ProductQualityMetrics(
        String jobId,
        String pairId,
        String aoiId,
        double meanCoherence,
        double minCoherence,
        double maxCoherence,
        double urbanHighCoherenceRatio, // Proporção de pixels com coerência >= 0.35 na área urbana
        boolean validPhaseRange,
        long totalPixels,
        long validPixels,
        String crs,
        boolean spatialOverlapSatisfied,
        String qualityVerdict, // ACCEPTED, ACCEPTABLE_WITH_RESERVATIONS, REJECTED
        List<String> qualityWarnings
) implements Serializable {

    public ProductQualityMetrics {
        qualityWarnings = qualityWarnings == null ? Collections.emptyList() : List.copyOf(qualityWarnings);
    }
}
