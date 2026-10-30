package com.ass.br.apiradar.interferometry.domain.model;

import java.io.Serializable;

/**
 * Proveniência do valor de linha de base perpendicular (B⊥).
 */
public enum BaselineSource {
    ASF_BASELINE_SERVICE("Metadados fornecidos pela API de baseline da NASA ASF"),
    ORBIT_STATE_VECTORS_CALCULATION("Cálculo geométrico analítico derivado de vetores de estado orbitais"),
    USER_PROVIDED("Valor experimental fornecido na solicitação"),
    UNAVAILABLE("Dados de baseline perpendicular ainda indisponíveis na fonte");

    private final String description;

    BaselineSource(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
