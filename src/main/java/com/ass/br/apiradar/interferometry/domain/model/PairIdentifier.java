package com.ass.br.apiradar.interferometry.domain.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Value Object que representa o identificador único e imutável de um par interferométrico.
 * Formado convencionalmente a partir das cenas de referência e secundária.
 */
public record PairIdentifier(String value) implements Serializable {

    public PairIdentifier {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Identificador do par não pode ser nulo ou vazio.");
        }
    }

    public static PairIdentifier of(String value) {
        return new PairIdentifier(value);
    }

    public static PairIdentifier fromScenes(String referenceSceneId, String secondarySceneId) {
        Objects.requireNonNull(referenceSceneId, "Cena de referência obrigatória para gerar ID do par");
        Objects.requireNonNull(secondarySceneId, "Cena secundária obrigatória para gerar ID do par");
        return new PairIdentifier("PAIR-" + referenceSceneId + "__" + secondarySceneId);
    }
}
