package com.ass.br.apiradar.catalog.infrastructure.adapters.input.web;

import com.ass.br.apiradar.catalog.application.dto.SarCatalogResponseDto;
import com.ass.br.apiradar.catalog.application.dto.SarCatalogSearchRequest;
import com.ass.br.apiradar.catalog.application.services.SarCatalogApplicationService;
import com.ass.br.apiradar.catalog.domain.model.SarScene;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SarCatalogRestControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SarCatalogApplicationService catalogService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        SarCatalogRestController controller = new SarCatalogRestController(catalogService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("POST /api/v1/sar-catalog/search retorna 200 OK com cenas Sentinel-1 SLC")
    void shouldSearchSarCatalogAndReturnScenes() throws Exception {
        UUID aoiId = UUID.randomUUID();

        SarScene scene1 = new SarScene(
                "S1A_IW_SLC__1SDV_20230105T211512_20230105T211539_046654_05973E_5A12",
                Instant.parse("2023-01-05T21:15:12Z"),
                142,
                "DESCENDING",
                "VV+VH",
                "SENTINEL-1A"
        );

        SarCatalogResponseDto responseDto = new SarCatalogResponseDto(1, List.of(scene1));
        when(catalogService.searchAvailableScenes(any(SarCatalogSearchRequest.class))).thenReturn(responseDto);

        String jsonPayload = String.format("""
                {
                    "aoiId": "%s",
                    "startDate": "2023-01-01",
                    "endDate": "2023-03-31",
                    "orbitDirection": "DESCENDING"
                }
                """, aoiId);

        mockMvc.perform(post("/api/v1/sar-catalog/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScenesFound").value(1))
                .andExpect(jsonPath("$.scenes[0].sceneName").value(scene1.sceneName()))
                .andExpect(jsonPath("$.scenes[0].relativeOrbit").value(142))
                .andExpect(jsonPath("$.scenes[0].flightDirection").value("DESCENDING"))
                .andExpect(jsonPath("$.scenes[0].sensor").value("SENTINEL-1A"));
    }
}
