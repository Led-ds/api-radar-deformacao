package com.ass.br.apiradar.territory.application.dto;

import com.ass.br.apiradar.shared.domain.model.OrganizationId;

import java.io.Serializable;

/**
 * Comando imutável para cadastro de uma nova Área de Interesse.
 */
public record CreateAoiCommand(
        OrganizationId organizationId,
        String name,
        String geometryWkt,
        String description,
        double maxAllowedAreaKm2
) implements Serializable {

    public double effectiveMaxAllowedArea() {
        return maxAllowedAreaKm2 > 0 ? maxAllowedAreaKm2 : 250.0;
    }
}
