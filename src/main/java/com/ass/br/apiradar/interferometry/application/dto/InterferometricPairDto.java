package com.ass.br.apiradar.interferometry.application.dto;

import com.ass.br.apiradar.catalog.domain.model.SarScene;
import com.ass.br.apiradar.interferometry.domain.model.InterferometricPair;
import com.ass.br.apiradar.interferometry.domain.model.PairRejectionReason;

import java.time.Instant;
import java.util.List;

/**
 * DTO para exposição REST de um par interferométrico avaliado.
 */
public record InterferometricPairDto(
        String pairId,
        SarScene referenceScene,
        SarScene secondaryScene,
        int temporalBaselineDays,
        Double perpendicularBaselineMeters,
        String baselineSource,
        boolean baselineAvailable,
        Double spatialOverlapRatio,
        String status,
        String statusLabel,
        List<String> rejectionReasons,
        List<String> criteriaEvaluated,
        String assessmentDetails,
        String policyProfileUsed,
        Instant evaluatedAt
) {
    public static InterferometricPairDto fromDomain(InterferometricPair pair) {
        List<String> reasons = pair.getAssessment().rejectionReasons().stream()
                .map(PairRejectionReason::getDescription)
                .toList();

        return new InterferometricPairDto(
                pair.getId().value(),
                pair.getReferenceScene(),
                pair.getSecondaryScene(),
                pair.getTemporalBaselineDays(),
                pair.getPerpendicularBaseline().meters(),
                pair.getPerpendicularBaseline().source().getDescription(),
                pair.getPerpendicularBaseline().available(),
                pair.getSpatialOverlapRatio(),
                pair.getStatus().name(),
                pair.getStatus().getLabel(),
                reasons,
                pair.getAssessment().criteriaEvaluated(),
                pair.getAssessment().details(),
                pair.getAssessment().policyProfileUsed(),
                pair.getAssessment().evaluatedAt()
        );
    }
}
