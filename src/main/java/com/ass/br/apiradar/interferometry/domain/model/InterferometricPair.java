package com.ass.br.apiradar.interferometry.domain.model;

import com.ass.br.apiradar.catalog.domain.model.SarScene;

import java.io.Serializable;
import java.util.Objects;

/**
 * Entidade que representa um Par Interferométrico (relação direcionada entre Cena de Referência e Cena Secundária).
 * Encapsula os metadados orbitais combinados, baseline temporal e perpendicular, e o parecer formal de elegibilidade.
 */
public class InterferometricPair implements Serializable {

    private final PairIdentifier id;
    private final SarScene referenceScene;
    private final SarScene secondaryScene;
    private final int temporalBaselineDays;
    private final BaselineValue perpendicularBaseline;
    private final Double spatialOverlapRatio;
    private final PairCompatibilityAssessment assessment;

    public InterferometricPair(
            PairIdentifier id,
            SarScene referenceScene,
            SarScene secondaryScene,
            int temporalBaselineDays,
            BaselineValue perpendicularBaseline,
            Double spatialOverlapRatio,
            PairCompatibilityAssessment assessment
    ) {
        this.id = Objects.requireNonNull(id, "Identificador do par é obrigatório");
        this.referenceScene = Objects.requireNonNull(referenceScene, "Cena de referência é obrigatória");
        this.secondaryScene = Objects.requireNonNull(secondaryScene, "Cena secundária é obrigatória");
        this.temporalBaselineDays = temporalBaselineDays;
        this.perpendicularBaseline = perpendicularBaseline != null ? perpendicularBaseline : BaselineValue.unavailable();
        this.spatialOverlapRatio = spatialOverlapRatio;
        this.assessment = Objects.requireNonNull(assessment, "Parecer de avaliação do par é obrigatório");
    }

    public PairIdentifier getId() {
        return id;
    }

    public SarScene getReferenceScene() {
        return referenceScene;
    }

    public SarScene getSecondaryScene() {
        return secondaryScene;
    }

    public int getTemporalBaselineDays() {
        return temporalBaselineDays;
    }

    public BaselineValue getPerpendicularBaseline() {
        return perpendicularBaseline;
    }

    public Double getSpatialOverlapRatio() {
        return spatialOverlapRatio;
    }

    public PairCompatibilityAssessment getAssessment() {
        return assessment;
    }

    public PairEligibilityStatus getStatus() {
        return assessment.status();
    }

    public boolean isEligibleForSubmission() {
        return assessment.status() == PairEligibilityStatus.ELIGIBLE_FOR_SUBMISSION;
    }

    public boolean isRejected() {
        return assessment.status() == PairEligibilityStatus.REJECTED;
    }

    public boolean isPendingValidation() {
        return assessment.status() == PairEligibilityStatus.PENDING_VALIDATION;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InterferometricPair that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "InterferometricPair{" +
                "id=" + id.value() +
                ", dt=" + temporalBaselineDays + "d" +
                ", bPerp=" + (perpendicularBaseline.available() ? perpendicularBaseline.meters() + "m" : "N/D") +
                ", status=" + getStatus() +
                '}';
    }
}
