package com.ass.br.apiradar.catalog.application.dto;

import com.ass.br.apiradar.catalog.domain.model.SarScene;

import java.io.Serializable;
import java.util.List;

/**
 * Resposta paginada/consolidada de cenas SAR localizadas para a AOI.
 */
public record SarCatalogResponseDto(
        int totalScenesFound,
        List<SarScene> scenes
) implements Serializable {

    public static SarCatalogResponseDto of(List<SarScene> scenes) {
        return new SarCatalogResponseDto(scenes.size(), scenes);
    }
}
