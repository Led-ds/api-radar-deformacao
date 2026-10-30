package com.ass.br.apiradar.interferometry.application.dto;

import java.io.Serializable;
import java.time.Instant;

/**
 * Representa um par de bursts correspondentes (mesmo relativeBurstId, subswath e polarização)
 * para as datas de referência e secundária, acompanhado das métricas de cobertura nominal calculadas.
 */
public record CandidateBurstPairDto(
        int relativeBurstId,
        String subswath,
        String polarization,
        String referenceBurstId,
        Instant referenceAcquisitionTime,
        String secondaryBurstId,
        Instant secondaryAcquisitionTime,
        double referenceCoveragePercentage,
        double secondaryCoveragePercentage,
        double commonTemporalCoveragePercentage,
        double commonTemporalCoverageKm2,
        boolean recommended
) implements Serializable {}
