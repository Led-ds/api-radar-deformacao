package com.ass.br.apiradar.interferometry.domain.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Value Object que consolida o parecer técnico e científico de avaliação de um par interferométrico.
 * Fornece rastreabilidade completa dos critérios executados e suas respectivas justificativas.
 */
public record PairCompatibilityAssessment(
        PairEligibilityStatus status,
        List<PairRejectionReason> rejectionReasons,
        List<String> criteriaEvaluated,
        String details,
        String policyProfileUsed,
        Instant evaluatedAt
) implements Serializable {

    public PairCompatibilityAssessment {
        rejectionReasons = rejectionReasons != null ? List.copyOf(rejectionReasons) : List.of();
        criteriaEvaluated = criteriaEvaluated != null ? List.copyOf(criteriaEvaluated) : List.of();
        evaluatedAt = evaluatedAt != null ? evaluatedAt : Instant.now();
    }

    public static PairCompatibilityAssessment approved(
            List<String> criteria,
            String details,
            String policyProfile
    ) {
        return new PairCompatibilityAssessment(
                PairEligibilityStatus.ELIGIBLE_FOR_SUBMISSION,
                Collections.emptyList(),
                criteria,
                details,
                policyProfile,
                Instant.now()
        );
    }

    public static PairCompatibilityAssessment preliminaryCompatible(
            List<String> criteria,
            String details,
            String policyProfile
    ) {
        return new PairCompatibilityAssessment(
                PairEligibilityStatus.PRELIMINARILY_COMPATIBLE,
                Collections.emptyList(),
                criteria,
                details,
                policyProfile,
                Instant.now()
        );
    }

    public static PairCompatibilityAssessment pending(
            List<String> criteria,
            String pendingReasonDetails,
            String policyProfile
    ) {
        return new PairCompatibilityAssessment(
                PairEligibilityStatus.PENDING_VALIDATION,
                Collections.emptyList(),
                criteria,
                pendingReasonDetails,
                policyProfile,
                Instant.now()
        );
    }

    public static PairCompatibilityAssessment rejected(
            List<PairRejectionReason> reasons,
            List<String> criteria,
            String details,
            String policyProfile
    ) {
        return new PairCompatibilityAssessment(
                PairEligibilityStatus.REJECTED,
                reasons,
                criteria,
                details,
                policyProfile,
                Instant.now()
        );
    }

    public static PairCompatibilityAssessment candidate(
            List<String> criteria,
            String details,
            String policyProfile
    ) {
        return new PairCompatibilityAssessment(
                PairEligibilityStatus.CANDIDATE,
                Collections.emptyList(),
                criteria,
                details,
                policyProfile,
                Instant.now()
        );
    }
}
