package com.ass.br.apiradar.interferometry.infrastructure;

import com.ass.br.apiradar.catalog.domain.model.SarScene;
import com.ass.br.apiradar.interferometry.application.dto.GeneratePairsRequest;
import com.ass.br.apiradar.interferometry.application.dto.GeneratePairsResponseDto;
import com.ass.br.apiradar.interferometry.application.dto.InterferometricPairDto;
import com.ass.br.apiradar.interferometry.application.dto.PairSelectionPolicyDto;
import com.ass.br.apiradar.interferometry.application.services.InterferometricPairApplicationService;
import com.ass.br.apiradar.interferometry.domain.model.PairSelectionPolicy;
import com.ass.br.apiradar.interferometry.infrastructure.adapters.input.web.InterferometricPairRestController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InterferometricPairRestController.class)
@DisplayName("Testes do Controlador REST: InterferometricPairRestController")
class InterferometricPairRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InterferometricPairApplicationService pairService;

    @MockBean
    private com.ass.br.apiradar.interferometry.application.services.InSarProcessingPreparationService processingPreparationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/interferometry/pairs/candidates deve retornar 200 OK com pares avaliados")
    void shouldReturn200WhenGeneratingPairs() throws Exception {
        PairSelectionPolicy policy = PairSelectionPolicy.tropicalUrbanMaceio();
        SarScene s1 = new SarScene("S1A_IW_SLC__1SDV_20230101T210000", Instant.now(), 96, "DESCENDING", "VV+VH", "SENTINEL-1A", "ID1", "NASA ASF");
        SarScene s2 = new SarScene("S1A_IW_SLC__1SDV_20230113T210000", Instant.now(), 96, "DESCENDING", "VV+VH", "SENTINEL-1A", "ID2", "NASA ASF");

        InterferometricPairDto pairDto = new InterferometricPairDto(
                "PAIR-1__2",
                s1, s2,
                12,
                45.0,
                "NASA ASF",
                true,
                1.0,
                "ELIGIBLE_FOR_SUBMISSION",
                "Elegível para Submissão",
                Collections.emptyList(),
                List.of("Modo IW", "Mesmo Track 96"),
                "Aprovado",
                policy.profileName(),
                Instant.now()
        );

        GeneratePairsResponseDto responseDto = new GeneratePairsResponseDto(
                "c0a80101-0000-0000-0000-000000000001",
                "Piloto Maceió",
                PairSelectionPolicyDto.fromDomain(policy),
                2,
                1,
                List.of(pairDto),
                Map.of("ELIGIBLE_FOR_SUBMISSION", 1L),
                GeneratePairsResponseDto.DEFAULT_SCIENTIFIC_DISCLAIMER
        );

        Mockito.when(pairService.generatePairs(any())).thenReturn(responseDto);

        GeneratePairsRequest request = new GeneratePairsRequest(
                "c0a80101-0000-0000-0000-000000000001",
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2023, 1, 31),
                "DESCENDING"
        );

        mockMvc.perform(post("/api/v1/interferometry/pairs/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aoiId").value("c0a80101-0000-0000-0000-000000000001"))
                .andExpect(jsonPath("$.totalPairsGenerated").value(1))
                .andExpect(jsonPath("$.pairs[0].status").value("ELIGIBLE_FOR_SUBMISSION"))
                .andExpect(jsonPath("$.pairs[0].temporalBaselineDays").value(12))
                .andExpect(jsonPath("$.scientificDisclaimer").exists());
    }

    @Test
    @DisplayName("GET /api/v1/interferometry/policies deve retornar 200 OK com a lista de políticas científicas")
    void shouldReturnAvailablePolicies() throws Exception {
        List<PairSelectionPolicyDto> policies = List.of(
                PairSelectionPolicyDto.fromDomain(PairSelectionPolicy.tropicalUrbanMaceio()),
                PairSelectionPolicyDto.fromDomain(PairSelectionPolicy.generalInSar()),
                PairSelectionPolicyDto.fromDomain(PairSelectionPolicy.strictScientific())
        );

        Mockito.when(pairService.getAvailablePolicies()).thenReturn(policies);

        mockMvc.perform(get("/api/v1/interferometry/policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].profileName").value("TROPICAL_URBAN_MACEIO"))
                .andExpect(jsonPath("$[1].profileName").value("GENERAL_INSAR_S1"))
                .andExpect(jsonPath("$[2].profileName").value("STRICT_SCIENTIFIC"));
    }

    @Test
    @DisplayName("GET /api/v1/interferometry/processing/modalities deve retornar 200 OK com modalidades suportadas")
    void shouldReturnSupportedModalities() throws Exception {
        mockMvc.perform(get("/api/v1/interferometry/processing/modalities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0]").value("FULL_SCENE"))
                .andExpect(jsonPath("$[1]").value("BURST_INSAR"))
                .andExpect(jsonPath("$[2]").value("MULTI_BURST"));
    }

    @Test
    @DisplayName("POST /api/v1/interferometry/pairs/{pairId}/processing/dry-run deve retornar 200 OK com payload auditável")
    void shouldReturnDryRunResponse() throws Exception {
        String pairId = "S1A_20230104__S1A_20230116";
        String canonRef = "S1_018319_IW1_20230104T080254_VV_604A-BURST";
        String canonSec = "S1_018319_IW1_20230116T080253_VV_BF75-BURST";

        var dryRunDto = new com.ass.br.apiradar.interferometry.application.dto.InSarProcessingDryRunResponseDto(
                java.util.UUID.randomUUID().toString(),
                "idemp_key_12345",
                "corr_id_67890",
                "c0a80101-0000-0000-0000-000000000001",
                pairId,
                "S1A_20230104",
                "S1A_20230116",
                List.of(canonRef, canonSec),
                com.ass.br.apiradar.interferometry.domain.model.processing.ProcessingModality.BURST_INSAR,
                "INSAR_ISCE_BURST",
                "hyp3-isce:v1.0",
                "TROPICAL_URBAN_MACEIO (v1.1)",
                1,
                "https://hyp3-docs.asf.alaska.edu/using/credits/",
                "READY_FOR_DRY_RUN (PARTIAL_COVERAGE_74%)",
                "Burst S1_018319_IW1 cobre 73.92% da AOI.",
                true,
                true,
                "{\"dry_run\": true}",
                Instant.now()
        );

        Mockito.when(processingPreparationService.generateDryRunRequest(Mockito.eq(pairId), any()))
                .thenReturn(dryRunDto);

        var requestBody = new com.ass.br.apiradar.interferometry.application.dto.InSarProcessingDryRunRequestDto(
                "c0a80101-0000-0000-0000-000000000001",
                com.ass.br.apiradar.interferometry.domain.model.processing.ProcessingModality.BURST_INSAR,
                List.of(canonRef, canonSec),
                "10x2",
                true,
                0.6,
                30,
                "corr_id_67890"
        );

        mockMvc.perform(post("/api/v1/interferometry/pairs/" + pairId + "/processing/dry-run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idempotencyKey").value("idemp_key_12345"))
                .andExpect(jsonPath("$.modality").value("BURST_INSAR"))
                .andExpect(jsonPath("$.estimatedCredits").value(1))
                .andExpect(jsonPath("$.dryRun").value(true))
                .andExpect(jsonPath("$.executionBlockedBySecurityPolicy").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/interferometry/pairs/{pairId}/processing/options deve retornar 200 OK com opções do par")
    void shouldReturn200WhenGettingProcessingOptions() throws Exception {
        String pairId = "S1A_20230104__S1A_20230116";
        String aoiId = "c0a80101-0000-0000-0000-000000000001";

        var optionsDto = new com.ass.br.apiradar.interferometry.application.dto.InSarProcessingOptionsResponseDto(
                pairId,
                aoiId,
                "Piloto Maceió",
                10.93,
                "S1A_20230104",
                "S1A_20230116",
                12,
                -79.0,
                "READY_FOR_SELECTION",
                List.of(new com.ass.br.apiradar.interferometry.application.dto.SupportedModalityOptionDto(
                        com.ass.br.apiradar.interferometry.domain.model.processing.ProcessingModality.BURST_INSAR,
                        "INSAR_ISCE_BURST",
                        "hyp3-isce:v1.0",
                        1,
                        "Processamento por burst",
                        100.0
                )),
                List.of(new com.ass.br.apiradar.interferometry.application.dto.CandidateBurstPairDto(
                        18319,
                        "IW1",
                        "VV",
                        "S1_018319_IW1_20230104T080254_VV_604A-BURST",
                        Instant.now(),
                        "S1_018319_IW1_20230116T080253_VV_BF75-BURST",
                        Instant.now(),
                        100.0,
                        100.0,
                        100.0,
                        10.93,
                        true
                )),
                new com.ass.br.apiradar.interferometry.application.dto.MultiBurstSummaryDto(
                        true,
                        List.of("S1_018319_IW1_20230104T080254_VV_604A-BURST"),
                        List.of("S1_018319_IW1_20230116T080253_VV_BF75-BURST"),
                        100.0,
                        10.93,
                        1
                ),
                List.of("Limitações de qualidade não avaliadas"),
                List.of()
        );

        Mockito.when(processingPreparationService.getProcessingOptions(eq(pairId), eq(aoiId)))
                .thenReturn(optionsDto);

        mockMvc.perform(get("/api/v1/interferometry/pairs/" + pairId + "/processing/options")
                        .param("aoiId", aoiId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pairId").value(pairId))
                .andExpect(jsonPath("$.aoiId").value(aoiId))
                .andExpect(jsonPath("$.status").value("READY_FOR_SELECTION"))
                .andExpect(jsonPath("$.candidateBurstPairs[0].relativeBurstId").value(18319))
                .andExpect(jsonPath("$.candidateBurstPairs[0].recommended").value(true))
                .andExpect(jsonPath("$.multiBurstSummary.available").value(true));
    }
}
