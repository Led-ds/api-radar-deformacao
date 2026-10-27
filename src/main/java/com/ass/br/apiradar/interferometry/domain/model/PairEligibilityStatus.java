package com.ass.br.apiradar.interferometry.domain.model;

/**
 * Estados semânticos formais de elegibilidade de um par interferométrico.
 * Representa rigorosamente o ciclo de vida analítico e científico do par.
 */
public enum PairEligibilityStatus {
    CANDIDATE(
            "Candidato",
            "Par combinado temporalmente na mesma órbita relativa (track) e direção de passagem."
    ),
    PRELIMINARILY_COMPATIBLE(
            "Preliminarmente Compatível",
            "Atende aos critérios preliminares de modo de feixe, track, direção, polarização e linha de base temporal."
    ),
    PENDING_VALIDATION(
            "Pendente de Validação",
            "Par tecnicamente compatível no plano temporal/geométrico preliminar, porém aguardando metadados complementares (ex: baseline perpendicular não consolidada na fonte ou órbitas precisas POEORB pendentes)."
    ),
    REJECTED(
            "Rejeitado",
            "Par rejeitado por violar um ou mais critérios técnicos, geométricos ou limites configurados na política científica."
    ),
    ELIGIBLE_FOR_SUBMISSION(
            "Elegível para Submissão",
            "Par que completou com sucesso todas as verificações exigidas pelo perfil metodológico vigente, com linha de base perpendicular conhecida dentro dos limites."
    );

    private final String label;
    private final String description;

    PairEligibilityStatus(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
