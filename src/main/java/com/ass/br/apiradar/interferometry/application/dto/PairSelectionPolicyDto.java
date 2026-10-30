package com.ass.br.apiradar.interferometry.application.dto;

import com.ass.br.apiradar.interferometry.domain.model.PairSelectionPolicy;

/**
 * DTO para representação de perfis de políticas científicas de seleção de pares.
 */
public record PairSelectionPolicyDto(
        String profileName,
        String version,
        String description,
        int minTemporalBaselineDays,
        int maxTemporalBaselineDays,
        Double maxPerpendicularBaselineMeters,
        double minSpatialOverlapRatio,
        boolean requirePreciseOrbit
) {
    public static PairSelectionPolicyDto fromDomain(PairSelectionPolicy policy) {
        return new PairSelectionPolicyDto(
                policy.profileName(),
                policy.version(),
                policy.description(),
                policy.minTemporalBaselineDays(),
                policy.maxTemporalBaselineDays(),
                policy.maxPerpendicularBaselineMeters(),
                policy.minSpatialOverlapRatio(),
                policy.requirePreciseOrbit()
        );
    }
}
