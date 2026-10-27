package com.ass.br.apiradar.catalog.infrastructure.adapters.input.web;

import com.ass.br.apiradar.catalog.application.dto.SarCatalogResponseDto;
import com.ass.br.apiradar.catalog.application.dto.SarCatalogSearchRequest;
import com.ass.br.apiradar.catalog.application.services.SarCatalogApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST oficial para consultas ao Catálogo SAR Sentinel-1.
 */
@RestController
@RequestMapping("/api/v1/sar-catalog")
public class SarCatalogRestController {

    private final SarCatalogApplicationService catalogService;

    public SarCatalogRestController(SarCatalogApplicationService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping("/search")
    public ResponseEntity<SarCatalogResponseDto> searchScenes(@Valid @RequestBody SarCatalogSearchRequest request) {
        SarCatalogResponseDto response = catalogService.searchAvailableScenes(request);
        return ResponseEntity.ok(response);
    }
}
