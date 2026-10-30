package com.ass.br.apiradar.territory.infrastructure.adapters.input.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.Map;

/**
 * Payload JSON para cadastro de uma nova Área de Interesse.
 * Suporta geometria enviada via WKT ou GeoJSON nativo.
 */
public record CreateAoiRequest(
        @NotBlank(message = "O nome da AOI é obrigatório.")
        @Size(min = 3, max = 150, message = "O nome da AOI deve conter entre 3 e 150 caracteres.")
        String name,

        String geometryWkt,

        Map<String, Object> geometryGeoJson,

        @Size(max = 500, message = "A descrição não pode ultrapassar 500 caracteres.")
        String description
) implements Serializable {

    public CreateAoiRequest(String name, String geometryWkt, String description) {
        this(name, geometryWkt, null, description);
    }
}
