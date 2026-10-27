package com.ass.br.apiradar.territory.infrastructure.adapters.input.web;

import com.ass.br.apiradar.shared.domain.model.AoiId;
import com.ass.br.apiradar.shared.domain.model.BoundingBox;
import com.ass.br.apiradar.shared.domain.model.OrganizationId;
import com.ass.br.apiradar.shared.infrastructure.web.GlobalExceptionHandler;
import com.ass.br.apiradar.territory.application.dto.AoiResponseDto;
import com.ass.br.apiradar.territory.application.dto.CreateAoiCommand;
import com.ass.br.apiradar.territory.domain.ports.input.AreaOfInterestUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AreaOfInterestRestControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AreaOfInterestUseCase useCase;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        var orgResolver = new com.ass.br.apiradar.shared.infrastructure.security.OrganizationContextResolver(
                true, "d3b07384-d113-40a2-9721-a53b2169b821");
        AreaOfInterestRestController controller = new AreaOfInterestRestController(useCase, orgResolver);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/areas-of-interest retorna 201 Created com cabeçalho Location")
    void shouldCreateAoiAndReturn201() throws Exception {
        UUID aoiUuid = UUID.randomUUID();
        UUID orgUuid = UUID.fromString("d3b07384-d113-40a2-9721-a53b2169b821");

        AoiResponseDto responseDto = new AoiResponseDto(
                aoiUuid,
                orgUuid,
                "Piloto Maceio - Mutange",
                35.42,
                "ACTIVE",
                new BoundingBox(-35.795, -9.680, -35.710, -9.600),
                "POLYGON((-35.795 -9.680, -35.710 -9.680, -35.710 -9.600, -35.795 -9.600, -35.795 -9.680))",
                Instant.now()
        );

        when(useCase.registerAreaOfInterest(any(CreateAoiCommand.class))).thenReturn(responseDto);

        CreateAoiRequest request = new CreateAoiRequest(
                "Piloto Maceio - Mutange",
                "POLYGON((-35.795 -9.680, -35.710 -9.680, -35.710 -9.600, -35.795 -9.600, -35.795 -9.680))",
                "Monitoramento geodésico da área urbana de Maceió"
        );

        mockMvc.perform(post("/api/v1/areas-of-interest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Idempotency-Key", "7b83c21a-5544-411a-881b-9e2c4f10a8c2"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/areas-of-interest/" + aoiUuid))
                .andExpect(jsonPath("$.id").value(aoiUuid.toString()))
                .andExpect(jsonPath("$.name").value("Piloto Maceio - Mutange"))
                .andExpect(jsonPath("$.metricAreaKm2").value(35.42))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.boundingBox.minLon").value(-35.795));
    }

    @Test
    @DisplayName("POST /api/v1/areas-of-interest com GeoJSON nativo retorna 201 Created")
    void shouldCreateAoiWithGeoJson() throws Exception {
        UUID aoiUuid = UUID.randomUUID();
        UUID orgUuid = UUID.fromString("d3b07384-d113-40a2-9721-a53b2169b821");

        AoiResponseDto responseDto = new AoiResponseDto(
                aoiUuid,
                orgUuid,
                "Maceio GeoJSON AOI",
                10.98,
                "ACTIVE",
                new BoundingBox(-35.75, -9.67, -35.72, -9.64),
                "POLYGON((-35.75 -9.67, -35.72 -9.67, -35.72 -9.64, -35.75 -9.64, -35.75 -9.67))",
                Instant.now()
        );

        when(useCase.registerAreaOfInterest(any(CreateAoiCommand.class))).thenReturn(responseDto);

        Map<String, Object> geoJson = Map.of(
                "type", "Polygon",
                "coordinates", List.of(List.of(
                        List.of(-35.75, -9.67),
                        List.of(-35.72, -9.67),
                        List.of(-35.72, -9.64),
                        List.of(-35.75, -9.64),
                        List.of(-35.75, -9.67)
                ))
        );

        CreateAoiRequest request = new CreateAoiRequest(
                "Maceio GeoJSON AOI",
                null,
                geoJson,
                "Cadastro via GeoJSON nativo"
        );

        mockMvc.perform(post("/api/v1/areas-of-interest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(aoiUuid.toString()))
                .andExpect(jsonPath("$.name").value("Maceio GeoJSON AOI"));
    }

    @Test
    @DisplayName("GET /api/v1/areas-of-interest/policy retorna a política operacional vigente")
    void shouldGetAoiPolicy() throws Exception {
        mockMvc.perform(get("/api/v1/areas-of-interest/policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxAllowedAreaKm2").value(250.0))
                .andExpect(jsonPath("$.unit").value("km2"))
                .andExpect(jsonPath("$.srid").value(4326));
    }

    @Test
    @DisplayName("POST /api/v1/areas-of-interest rejeita com 400 Bad Request divergência entre WKT e GeoJSON")
    void shouldRejectDivergentWktAndGeoJson() throws Exception {
        Map<String, Object> geoJson = Map.of(
                "type", "Polygon",
                "coordinates", List.of(List.of(
                        List.of(-35.75, -9.67),
                        List.of(-35.72, -9.67),
                        List.of(-35.72, -9.64),
                        List.of(-35.75, -9.64),
                        List.of(-35.75, -9.67)
                ))
        );

        // WKT divergente (ex: Brasília)
        String divergentWkt = "POLYGON((-47.92 -15.80, -47.90 -15.80, -47.90 -15.78, -47.92 -15.78, -47.92 -15.80))";

        CreateAoiRequest request = new CreateAoiRequest(
                "AOI Divergente",
                divergentWkt,
                geoJson,
                "Teste de divergência de representações"
        );

        mockMvc.perform(post("/api/v1/areas-of-interest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Divergência detectada entre as representações WKT e GeoJSON")));
    }

    @Test
    @DisplayName("POST /api/v1/areas-of-interest aceita quando WKT e GeoJSON forem equivalentes")
    void shouldAcceptEquivalentWktAndGeoJson() throws Exception {
        UUID aoiUuid = UUID.randomUUID();
        UUID orgUuid = UUID.fromString("d3b07384-d113-40a2-9721-a53b2169b821");

        AoiResponseDto responseDto = new AoiResponseDto(
                aoiUuid,
                orgUuid,
                "AOI Consistente",
                10.98,
                "ACTIVE",
                new BoundingBox(-35.75, -9.67, -35.72, -9.64),
                "POLYGON((-35.75 -9.67, -35.72 -9.67, -35.72 -9.64, -35.75 -9.64, -35.75 -9.67))",
                Instant.now()
        );

        when(useCase.registerAreaOfInterest(any(CreateAoiCommand.class))).thenReturn(responseDto);

        Map<String, Object> geoJson = Map.of(
                "type", "Polygon",
                "coordinates", List.of(List.of(
                        List.of(-35.75, -9.67),
                        List.of(-35.72, -9.67),
                        List.of(-35.72, -9.64),
                        List.of(-35.75, -9.64),
                        List.of(-35.75, -9.67)
                ))
        );

        String identicalWkt = "POLYGON((-35.75 -9.67, -35.72 -9.67, -35.72 -9.64, -35.75 -9.64, -35.75 -9.67))";

        CreateAoiRequest request = new CreateAoiRequest(
                "AOI Consistente",
                identicalWkt,
                geoJson,
                "Teste consistente"
        );

        mockMvc.perform(post("/api/v1/areas-of-interest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(aoiUuid.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/areas-of-interest/{id} retorna 200 OK")
    void shouldGetAoiById() throws Exception {
        UUID aoiUuid = UUID.randomUUID();
        UUID orgUuid = UUID.fromString("d3b07384-d113-40a2-9721-a53b2169b821");

        AoiResponseDto responseDto = new AoiResponseDto(
                aoiUuid,
                orgUuid,
                "Piloto Maceio - Mutange",
                35.42,
                "ACTIVE",
                new BoundingBox(-35.795, -9.680, -35.710, -9.600),
                "POLYGON(...)",
                Instant.now()
        );

        when(useCase.getAreaOfInterestById(any(AoiId.class), any(OrganizationId.class))).thenReturn(responseDto);

        mockMvc.perform(get("/api/v1/areas-of-interest/" + aoiUuid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(aoiUuid.toString()))
                .andExpect(jsonPath("$.name").value("Piloto Maceio - Mutange"));
    }

    @Test
    @DisplayName("GET /api/v1/areas-of-interest/{id}/geojson retorna GeoJSON Feature")
    void shouldGetAoiGeoJson() throws Exception {
        UUID aoiUuid = UUID.randomUUID();
        UUID orgUuid = UUID.fromString("d3b07384-d113-40a2-9721-a53b2169b821");

        Map<String, Object> geoJsonGeom = Map.of(
                "type", "Polygon",
                "coordinates", List.of(List.of(
                        List.of(-35.75, -9.67),
                        List.of(-35.72, -9.67),
                        List.of(-35.72, -9.64),
                        List.of(-35.75, -9.64),
                        List.of(-35.75, -9.67)
                ))
        );

        AoiResponseDto responseDto = new AoiResponseDto(
                aoiUuid,
                orgUuid,
                "Maceio GeoJSON AOI",
                10.98,
                "ACTIVE",
                new BoundingBox(-35.75, -9.67, -35.72, -9.64),
                "POLYGON(...)",
                geoJsonGeom,
                Instant.now()
        );

        when(useCase.getAreaOfInterestById(any(AoiId.class), any(OrganizationId.class))).thenReturn(responseDto);

        mockMvc.perform(get("/api/v1/areas-of-interest/" + aoiUuid + "/geojson"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("Feature"))
                .andExpect(jsonPath("$.id").value(aoiUuid.toString()))
                .andExpect(jsonPath("$.geometry.type").value("Polygon"))
                .andExpect(jsonPath("$.properties.name").value("Maceio GeoJSON AOI"));
    }
}
