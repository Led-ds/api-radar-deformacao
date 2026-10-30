package com.ass.br.apiradar.interferometry.domain.model;

import java.io.Serializable;

/**
 * Value Object que encapsula a linha de base perpendicular (B⊥) em metros,
 * sua proveniência, estado de disponibilidade técnica, unidades e convenção de sinal.
 * Garante que valores ausentes sejam rigorosamente distinguidos de zero ou aproximações espúrias.
 */
public record BaselineValue(
        Double meters,
        BaselineSource source,
        boolean available,
        String unit,
        String signConvention,
        String referenceGeometry
) implements Serializable {

    public static final String DEFAULT_UNIT = "meters";
    public static final String DEFAULT_SIGN_CONVENTION = "Projeção normal à linha de visada (LOS) da cena de referência no centro da AOI: B_perp = B · cos(θ - α)";
    public static final String DEFAULT_REFERENCE_GEOMETRY = "ZERO_DOPPLER_SLR_REFERENCE";

    public BaselineValue(Double meters, BaselineSource source, boolean available) {
        this(meters, source, available, DEFAULT_UNIT, DEFAULT_SIGN_CONVENTION, DEFAULT_REFERENCE_GEOMETRY);
    }

    public static BaselineValue calculated(double meters, BaselineSource source) {
        return new BaselineValue(meters, source, true);
    }

    public static BaselineValue provided(double meters) {
        return new BaselineValue(meters, BaselineSource.USER_PROVIDED, true);
    }

    public static BaselineValue unavailable() {
        return new BaselineValue(null, BaselineSource.UNAVAILABLE, false, DEFAULT_UNIT, DEFAULT_SIGN_CONVENTION, DEFAULT_REFERENCE_GEOMETRY);
    }

    public static BaselineValue fromAsf(Double meters) {
        if (meters == null) {
            return unavailable();
        }
        return new BaselineValue(meters, BaselineSource.ASF_BASELINE_SERVICE, true);
    }

    /**
     * Retorna a magnitude absoluta da linha de base perpendicular (|B⊥|),
     * utilizada no cálculo de descorrelação espacial e franja de altura de ambiguidade.
     */
    public Double absoluteMeters() {
        return meters != null ? Math.abs(meters) : null;
    }
}
