package com.ass.br.apiradar.shared.domain.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Identificador tipado e imutável de uma Área de Interesse (AOI).
 */
public record AoiId(UUID value) implements Serializable {

    public AoiId {
        Objects.requireNonNull(value, "AoiId cannot be null");
    }

    public static AoiId of(UUID value) {
        return new AoiId(value);
    }

    public static AoiId of(String value) {
        return new AoiId(UUID.fromString(value));
    }

    public static AoiId random() {
        return new AoiId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
