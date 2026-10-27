package com.ass.br.apiradar.interferometry.domain.model;

/**
 * Razões explícitas de rejeição de um par interferométrico.
 */
public enum PairRejectionReason {
    INCOMPATIBLE_TRACK("Órbitas relativas (tracks) divergentes. Pares InSAR exigem aquisições no mesmo corredor orbital."),
    INCOMPATIBLE_DIRECTION("Direções de voo divergentes (ex: ASCENDING com DESCENDING geram geometrias de visada opostas)."),
    INCOMPATIBLE_POLARIZATION("Polarizações incompatíveis ou sem canal correspondente co-polarizado (ex: VV com HH)."),
    INCOMPATIBLE_BEAM_MODE("Modos de aquisição divergentes (ambas as cenas devem estar em modo IW SLC)."),
    INCOMPATIBLE_SENSOR("Plataformas ou sensores de constelações incompatíveis sem calibração cruzada."),
    DUPLICATE_SCENES("As cenas primária (referência) e secundária informadas são idênticas."),
    INVALID_TEMPORAL_ORDER("A data da cena de referência é posterior ou igual à data da cena secundária."),
    TEMPORAL_BASELINE_EXCEEDED("O intervalo temporal (Δt) ultrapassou o limite máximo estabelecido pela política científica vigente."),
    PERPENDICULAR_BASELINE_EXCEEDED("A linha de base perpendicular (B⊥) ultrapassou o limiar crítico de descorrelação espacial da política."),
    INSUFFICIENT_SPATIAL_OVERLAP("A sobreposição espacial calculada entre os footprints das cenas e a AOI é inferior ao mínimo tolerado pela política."),
    MISSING_ORBITAL_METADATA("Metadados essenciais ausentes na fonte para viabilizar a análise interferométrica.");

    private final String description;

    PairRejectionReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
