package com.ass.br.apiradar.shared.domain.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Identificador tipado e imutável de uma Organização (Tenant).
 */
public record OrganizationId(UUID value) implements Serializable {

    public OrganizationId {
        Objects.requireNonNull(value, "OrganizationId cannot be null");
    }

    public static OrganizationId of(UUID value) {
        return new OrganizationId(value);
    }

    public static OrganizationId of(String value) {
        return new OrganizationId(UUID.fromString(value));
    }

    public static OrganizationId random() {
        return new OrganizationId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
