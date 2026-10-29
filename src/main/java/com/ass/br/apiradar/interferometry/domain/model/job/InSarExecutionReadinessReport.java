package com.ass.br.apiradar.interferometry.domain.model.job;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Parecer formal de prontidão operacional antes da submissão de um experimento real ao cluster HyP3.
 */
public record InSarExecutionReadinessReport(
        String pairId,
        String aoiId,
        boolean pairEligible,
        boolean credentialsConfigured,
        boolean executionEnabled,
        int estimatedCredits,
        String officialJobType,
        String targetBurst,
        String readinessVerdict, // READY_FOR_SUPERVISED_EXECUTION, BLOCKED_MISSING_CREDENTIALS, BLOCKED_BY_POLICY, INELIGIBLE_PAIR
        List<String> blockerReasons,
        List<String> operationalChecklist
) implements Serializable {

    public InSarExecutionReadinessReport {
        blockerReasons = blockerReasons == null ? Collections.emptyList() : List.copyOf(blockerReasons);
        operationalChecklist = operationalChecklist == null ? Collections.emptyList() : List.copyOf(operationalChecklist);
    }
}
