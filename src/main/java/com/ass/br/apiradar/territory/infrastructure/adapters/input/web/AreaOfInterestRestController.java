package com.ass.br.apiradar.territory.infrastructure.adapters.input.web;

import com.ass.br.apiradar.shared.domain.exceptions.InvalidGeometryException;
import com.ass.br.apiradar.shared.domain.model.AoiId;
import com.ass.br.apiradar.shared.domain.model.OrganizationId;
import com.ass.br.apiradar.shared.domain.model.PolygonGeometry;
import com.ass.br.apiradar.shared.infrastructure.security.OrganizationContextResolver;
import com.ass.br.apiradar.territory.application.dto.AoiResponseDto;
import com.ass.br.apiradar.territory.application.dto.CreateAoiCommand;
import com.ass.br.apiradar.territory.domain.ports.input.AreaOfInterestUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * Controlador REST oficial da API v1 para Gestão Territorial (AOIs).
 * Integra resolução segura de contexto de organização via OrganizationContextResolver,
 * validação estrita de consistência WKT/GeoJSON e política de área máxima configurada no backend.
 */
@RestController
@RequestMapping("/api/v1/areas-of-interest")
public class AreaOfInterestRestController {

    private final AreaOfInterestUseCase useCase;
    private final OrganizationContextResolver orgResolver;
    private final double configuredMaxAreaKm2;

    @Autowired
    public AreaOfInterestRestController(AreaOfInterestUseCase useCase,
                                        OrganizationContextResolver orgResolver,
                                        @Value("${radar.territory.max-allowed-area-km2:250.0}") double configuredMaxAreaKm2) {
        this.useCase = useCase;
        this.orgResolver = orgResolver;
        this.configuredMaxAreaKm2 = configuredMaxAreaKm2;
    }

    public AreaOfInterestRestController(AreaOfInterestUseCase useCase,
                                        OrganizationContextResolver orgResolver) {
        this(useCase, orgResolver, 250.0);
    }

    @GetMapping("/policy")
    public ResponseEntity<Map<String, Object>> getAoiPolicy() {
        return ResponseEntity.ok(Map.of(
                "maxAllowedAreaKm2", configuredMaxAreaKm2,
                "unit", "km2",
                "srid", 4326,
                "policyDescription", "Limite máximo de área operacional autorizado pelo sistema para o ambiente corrente."
        ));
    }

    @PostMapping
    public ResponseEntity<AoiResponseDto> createAreaOfInterest(
            @Valid @RequestBody CreateAoiRequest request,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationIdHeader,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        OrganizationId organizationId = orgResolver.resolveOrganizationId(organizationIdHeader);

        PolygonGeometry wktGeom = null;
        PolygonGeometry geoJsonGeom = null;

        if (request.geometryWkt() != null && !request.geometryWkt().isBlank()) {
            wktGeom = PolygonGeometry.fromWkt(request.geometryWkt());
        }
        if (request.geometryGeoJson() != null && !request.geometryGeoJson().isEmpty()) {
            geoJsonGeom = PolygonGeometry.fromGeoJson(request.geometryGeoJson());
        }

        if (wktGeom == null && geoJsonGeom == null) {
            throw new InvalidGeometryException("A geometria (WKT ou GeoJSON) é obrigatória para registrar a AOI.");
        }

        if (wktGeom != null && geoJsonGeom != null) {
            boolean exactlyEqual = wktGeom.jtsPolygon().equalsExact(geoJsonGeom.jtsPolygon());
            boolean topoEqual = wktGeom.jtsPolygon().equalsTopo(geoJsonGeom.jtsPolygon());
            if (!exactlyEqual && !topoEqual) {
                throw new InvalidGeometryException("Divergência detectada entre as representações WKT e GeoJSON enviadas na mesma requisição.");
            }
        }

        PolygonGeometry canonicalGeom = (geoJsonGeom != null) ? geoJsonGeom : wktGeom;
        String wkt = canonicalGeom.wkt();

        CreateAoiCommand command = new CreateAoiCommand(
                organizationId,
                request.name(),
                wkt,
                request.description(),
                configuredMaxAreaKm2
        );

        AoiResponseDto response = useCase.registerAreaOfInterest(command);
        URI location = URI.create("/api/v1/areas-of-interest/" + response.id());

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AoiResponseDto> getAreaOfInterestById(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationIdHeader) {

        OrganizationId organizationId = orgResolver.resolveOrganizationId(organizationIdHeader);
        AoiResponseDto response = useCase.getAreaOfInterestById(AoiId.of(id), organizationId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/geojson")
    public ResponseEntity<Map<String, Object>> getAreaOfInterestGeoJson(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationIdHeader) {

        OrganizationId organizationId = orgResolver.resolveOrganizationId(organizationIdHeader);
        AoiResponseDto response = useCase.getAreaOfInterestById(AoiId.of(id), organizationId);

        Map<String, Object> feature = Map.of(
                "type", "Feature",
                "id", response.id().toString(),
                "geometry", response.geometryGeoJson() != null ? response.geometryGeoJson() : Map.of(),
                "properties", Map.of(
                        "id", response.id().toString(),
                        "name", response.name(),
                        "metricAreaKm2", response.metricAreaKm2(),
                        "status", response.status(),
                        "createdAt", response.createdAt().toString()
                )
        );

        return ResponseEntity.ok(feature);
    }

    @GetMapping
    public ResponseEntity<List<AoiResponseDto>> listAreasOfInterest(
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationIdHeader) {

        OrganizationId organizationId = orgResolver.resolveOrganizationId(organizationIdHeader);
        List<AoiResponseDto> response = useCase.listAreasOfInterestByOrganization(organizationId);

        return ResponseEntity.ok(response);
    }
}
