package com.ass.br.apiradar.shared.domain.model;

import com.ass.br.apiradar.shared.domain.exceptions.MaxAreaExceededException;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Objeto de valor imutável que representa a área métrica em quilômetros quadrados (km²).
 */
public record MetricArea(double valueKm2) implements Serializable {

    public static final double DEFAULT_MAX_KM2 = 50.0;

    public MetricArea {
        if (valueKm2 <= 0.0) {
            throw new IllegalArgumentException("MetricArea must be strictly positive (> 0.0 km²)");
        }
    }

    public void validateMaxAllowed(double maxAllowedKm2) {
        if (this.valueKm2 > maxAllowedKm2) {
            throw new MaxAreaExceededException(this.valueKm2, maxAllowedKm2);
        }
    }

    public double rounded(int decimals) {
        return BigDecimal.valueOf(valueKm2)
                .setScale(decimals, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
