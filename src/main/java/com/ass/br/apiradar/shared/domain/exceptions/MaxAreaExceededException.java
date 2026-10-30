package com.ass.br.apiradar.shared.domain.exceptions;

import java.util.Locale;

/**
 * Lançada quando a área métrica calculada excede o limite contratual permitido para a organização.
 */
public class MaxAreaExceededException extends DomainException {

    public static final String ERROR_CODE = "ERR_AOI_AREA_EXCEEDED";

    private final double actualAreaKm2;
    private final double maxAllowedAreaKm2;

    public MaxAreaExceededException(double actualAreaKm2, double maxAllowedAreaKm2) {
        super(ERROR_CODE, String.format(Locale.US,
                "A área métrica da geometria informada (%.2f km²) excede o teto contratual de %.2f km² para a organização.",
                actualAreaKm2, maxAllowedAreaKm2));
        this.actualAreaKm2 = actualAreaKm2;
        this.maxAllowedAreaKm2 = maxAllowedAreaKm2;
    }

    public double getActualAreaKm2() {
        return actualAreaKm2;
    }

    public double getMaxAllowedAreaKm2() {
        return maxAllowedAreaKm2;
    }
}
