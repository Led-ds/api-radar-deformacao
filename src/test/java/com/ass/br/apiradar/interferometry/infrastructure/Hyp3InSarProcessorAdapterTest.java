package com.ass.br.apiradar.interferometry.infrastructure;

import com.ass.br.apiradar.catalog.domain.model.SarScene;
import com.ass.br.apiradar.interferometry.domain.exceptions.InSarExecutionBlockedException;
import com.ass.br.apiradar.interferometry.domain.exceptions.IneligiblePairProcessingException;
import com.ass.br.apiradar.interferometry.domain.model.*;
import com.ass.br.apiradar.interferometry.domain.model.processing.InSarProcessingParameters;
import com.ass.br.apiradar.interferometry.domain.model.processing.InSarProcessingRequest;
import com.ass.br.apiradar.interferometry.domain.model.processing.ProcessingModality;
import com.ass.br.apiradar.interferometry.domain.model.processing.ProcessorEligibilityResult;
import com.ass.br.apiradar.interferometry.infrastructure.adapters.output.processing.Hyp3InSarProcessorAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Ciclo 5.4A — Homologação do Contrato Real NASA ASF HyP3 e Separação Estrita de Contratos")
class Hyp3InSarProcessorAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Hyp3InSarProcessorAdapter adapter;

    private SarScene refScene;
    private SarScene secScene;

    // Bursts canônicos oficiais confirmados via ASF Search API sobre a área crítica de Maceió (Mina 18 / Pinheiro)
    private final String canonBurstRef19 = "S1_018319_IW1_20230104T080254_VV_604A-BURST";
    private final String canonBurstSec19 = "S1_018319_IW1_20230116T080253_VV_BF75-BURST";

    private final String canonBurstRef18 = "S1_018318_IW1_20230104T080251_VV_604A-BURST";
    private final String canonBurstSec18 = "S1_018318_IW1_20230116T080250_VV_BF75-BURST";

    @BeforeEach
    void setUp() {
        // Adaptador configurado com executionEnabled = false (padrão de segurança financeira)
        adapter = new Hyp3InSarProcessorAdapter(false, "https://hyp3-api.asf.alaska.edu", objectMapper);

        refScene = new SarScene(
                "S1A_IW_SLC__1SDV_20230104T080249_20230104T080316_046631_0596C5_604A",
                Instant.parse("2023-01-04T08:02:49Z"),
                9,
                "DESCENDING",
                "VV+VH",
                "SENTINEL-1A",
                "S1A_20230104",
                "NASA ASF DAAC"
        );

        secScene = new SarScene(
                "S1A_IW_SLC__1SDV_20230116T080248_20230116T080315_046806_059CA6_BF75",
                Instant.parse("2023-01-16T08:02:48Z"),
                9,
                "DESCENDING",
                "VV+VH",
                "SENTINEL-1A",
                "S1A_20230116",
                "NASA ASF DAAC"
        );
    }

    private InterferometricPair createEligiblePair() {
        PairCompatibilityAssessment assessment = PairCompatibilityAssessment.approved(
                List.of("Modo IW", "Mesmo Track 9", "Baseline -79m compatível"),
                "Par 04/01 a 16/01 elegível",
                "TROPICAL_URBAN_MACEIO (v1.1)"
        );

        return new InterferometricPair(
                PairIdentifier.of("PAR-01__MACEIO"),
                refScene,
                secScene,
                12,
                BaselineValue.calculated(-79.0, BaselineSource.ASF_BASELINE_SERVICE),
                1.0,
                assessment
        );
    }

    private InterferometricPair createRejectedPair() {
        PairCompatibilityAssessment assessment = PairCompatibilityAssessment.rejected(
                List.of(PairRejectionReason.PERPENDICULAR_BASELINE_EXCEEDED),
                List.of("Baseline 235m excede limite 120m"),
                "Rejeitado por baseline excessiva",
                "TROPICAL_URBAN_MACEIO (v1.1)"
        );

        return new InterferometricPair(
                PairIdentifier.of("PAR-02__MACEIO"),
                refScene,
                secScene,
                12,
                BaselineValue.calculated(235.0, BaselineSource.ASF_BASELINE_SERVICE),
                1.0,
                assessment
        );
    }

    @Test
    @DisplayName("1. Separação Estrita de Contratos: Contrato Interno Radar++ vs Payload Externo Oficial HyP3")
    void shouldStrictlySeparateInternalContractFromExternalHyP3Payload() throws Exception {
        InterferometricPair pair = createEligiblePair();
        InSarProcessingParameters params = InSarProcessingParameters.defaultForUrbanMaceio();
        List<String> bursts = List.of(canonBurstRef19, canonBurstSec19);

        InSarProcessingRequest request = adapter.buildDryRunRequest(
                "aoi-maceio-123",
                pair,
                ProcessingModality.BURST_INSAR,
                bursts,
                params,
                "corr-test-uuid"
        );

        // A. CONTRATO INTERNO RADAR++: Contém metadados de domínio, auditoria e rastreabilidade
        assertNotNull(request);
        assertTrue(request.dryRun());
        assertEquals("aoi-maceio-123", request.aoiId());
        assertEquals("PAR-01__MACEIO", request.pairId());
        assertEquals(refScene.sceneName(), request.referenceScene());
        assertEquals(secScene.sceneName(), request.secondaryScene());
        assertEquals(bursts, request.selectedBursts());
        assertEquals(ProcessingModality.BURST_INSAR, request.modality());
        assertEquals(1, request.estimatedCredits());
        assertEquals("corr-test-uuid", request.correlationId());
        assertNotNull(request.idempotencyKey());
        assertEquals(params, request.parameters()); // Parâmetros científicos preservados internamente

        // B. CONTRATO EXTERNO HYP3: Aderente estritamente ao schema OpenAPI oficial (POST /jobs)
        JsonNode externalRoot = objectMapper.readTree(request.auditPayloadJson());

        // A raiz aceita EXCLUSIVAMENTE 'validate_only' e 'jobs' (additionalProperties: false no OpenAPI)
        assertTrue(externalRoot.has("validate_only"));
        assertTrue(externalRoot.get("validate_only").asBoolean(), "Dry-run oficial HyP3 utiliza validate_only=true");
        assertTrue(externalRoot.has("jobs"));

        // Propriedades internas do Radar++ NUNCA devem vazar para o payload externo
        assertFalse(externalRoot.has("dry_run"), "dry_run é propriedade interna Radar++ e não existe no OpenAPI");
        assertFalse(externalRoot.has("correlation_id"), "correlation_id é interno Radar++");
        assertFalse(externalRoot.has("idempotency_key"), "idempotency_key é interno Radar++");
        assertFalse(externalRoot.has("target_provider"), "target_provider é interno Radar++");
        assertFalse(externalRoot.has("pipeline_version"), "pipeline_version é interno Radar++");
        assertFalse(externalRoot.has("aoi_id"), "aoi_id é interno Radar++");
        assertFalse(externalRoot.has("pair_id"), "pair_id é interno Radar++");
        assertFalse(externalRoot.has("estimated_credits"), "estimated_credits é interno Radar++");

        // Validação das chaves do objeto raiz contra o schema OpenAPI
        Iterator<String> fieldNames = externalRoot.fieldNames();
        while (fieldNames.hasNext()) {
            String field = fieldNames.next();
            assertTrue(Set.of("validate_only", "jobs").contains(field),
                    "Campo inesperado no payload externo HyP3: " + field);
        }
    }

    @Test
    @DisplayName("2. Validação Efetiva dos Payloads Gerados contra o Snapshot OpenAPI Oficial do HyP3 (POST /jobs)")
    void shouldValidateGeneratedPayloadAgainstOpenApiSnapshot() throws Exception {
        // Carrega o snapshot OpenAPI versionado
        InputStream is = getClass().getResourceAsStream("/hyp3_openapi_jobs_schema_snapshot.json");
        assertNotNull(is, "Snapshot OpenAPI não encontrado em src/test/resources/hyp3_openapi_jobs_schema_snapshot.json");
        JsonNode openApiSnapshot = objectMapper.readTree(is);
        assertEquals("3.0.4", openApiSnapshot.get("openapi_version").asText());

        JsonNode rootSchema = openApiSnapshot.at("/paths/~1jobs/post/requestBody/content/application~1json/schema");
        assertFalse(rootSchema.isMissingNode());
        assertFalse(rootSchema.get("additionalProperties").asBoolean());

        InterferometricPair pair = createEligiblePair();

        // 2.1. Validação da modalidade BURST_INSAR contra o OpenAPI
        InSarProcessingRequest burstReq = adapter.buildDryRunRequest(
                "aoi-1",
                pair,
                ProcessingModality.BURST_INSAR,
                List.of(canonBurstRef19, canonBurstSec19),
                InSarProcessingParameters.defaultForUrbanMaceio(),
                "c-burst"
        );
        JsonNode burstPayload = objectMapper.readTree(burstReq.auditPayloadJson());
        validateOpenApiRootContract(burstPayload, rootSchema);

        JsonNode burstJob = burstPayload.get("jobs").get(0);
        assertEquals("INSAR_ISCE_BURST", burstJob.get("job_type").asText());
        JsonNode burstParams = burstJob.get("job_parameters");
        assertTrue(burstParams.has("granules"), "Campo obrigatório 'granules' ausente");
        assertEquals(2, burstParams.get("granules").size());
        assertTrue(burstParams.get("granules").get(0).asText().matches("^S1_\\d{6}_IW.*-BURST$"));
        assertTrue(Set.of("20x4", "10x2", "5x1").contains(burstParams.get("looks").asText()));
        assertEquals(3, burstParams.size(), "INSAR_ISCE_BURST deve conter estritamente 3 campos (granules, looks, apply_water_mask)");

        // 2.2. Validação da modalidade MULTI_BURST contra o OpenAPI
        InSarProcessingRequest multiReq = adapter.buildDryRunRequest(
                "aoi-1",
                pair,
                ProcessingModality.MULTI_BURST,
                List.of(canonBurstRef18, canonBurstRef19, canonBurstSec18, canonBurstSec19),
                InSarProcessingParameters.defaultForUrbanMaceio(),
                "c-multi"
        );
        JsonNode multiPayload = objectMapper.readTree(multiReq.auditPayloadJson());
        validateOpenApiRootContract(multiPayload, rootSchema);

        JsonNode multiJob = multiPayload.get("jobs").get(0);
        assertEquals("INSAR_ISCE_MULTI_BURST", multiJob.get("job_type").asText());
        JsonNode multiParams = multiJob.get("job_parameters");
        assertTrue(multiParams.has("reference"), "Campo obrigatório 'reference' ausente");
        assertTrue(multiParams.has("secondary"), "Campo obrigatório 'secondary' ausente");
        assertFalse(multiParams.has("granules"), "MULTI_BURST não pode conter 'granules'");
        assertEquals(4, multiParams.size(), "INSAR_ISCE_MULTI_BURST deve conter estritamente 4 campos (reference, secondary, looks, apply_water_mask)");

        // 2.3. Validação da modalidade FULL_SCENE (GAMMA) contra o OpenAPI
        InSarProcessingRequest gammaReq = adapter.buildDryRunRequest(
                "aoi-1",
                pair,
                ProcessingModality.FULL_SCENE,
                List.of(),
                InSarProcessingParameters.defaultForUrbanMaceio(),
                "c-gamma"
        );
        JsonNode gammaPayload = objectMapper.readTree(gammaReq.auditPayloadJson());
        validateOpenApiRootContract(gammaPayload, rootSchema);

        JsonNode gammaJob = gammaPayload.get("jobs").get(0);
        assertEquals("INSAR_GAMMA", gammaJob.get("job_type").asText());
        JsonNode gammaParams = gammaJob.get("job_parameters");
        assertTrue(gammaParams.has("granules"));
        assertEquals(2, gammaParams.get("granules").size());
        assertTrue(gammaParams.get("granules").get(0).asText().startsWith("S1A_IW_SLC__"));
        assertTrue(Set.of("20x4", "10x2").contains(gammaParams.get("looks").asText()));
    }

    private void validateOpenApiRootContract(JsonNode payload, JsonNode rootSchema) {
        assertTrue(payload.has("validate_only"));
        assertTrue(payload.get("validate_only").asBoolean());
        assertTrue(payload.has("jobs"));
        assertTrue(payload.get("jobs").isArray());
        assertFalse(payload.get("jobs").isEmpty());

        for (Iterator<String> it = payload.fieldNames(); it.hasNext(); ) {
            String field = it.next();
            assertTrue(rootSchema.get("properties").has(field),
                    "Propriedade gerada viola OpenAPI additionalProperties=false: " + field);
        }
    }

    @Test
    @DisplayName("3. Serialização fiel ao contrato oficial INSAR_ISCE_BURST: apenas 'granules', 'looks' e 'apply_water_mask'")
    void shouldBuildFaithfulSingleBurstPayloadConformingToOpenApiSchema() throws Exception {
        InterferometricPair pair = createEligiblePair();
        InSarProcessingParameters params = InSarProcessingParameters.defaultForUrbanMaceio();
        List<String> bursts = List.of(canonBurstRef19, canonBurstSec19);

        InSarProcessingRequest request = adapter.buildDryRunRequest(
                "aoi-maceio-123",
                pair,
                ProcessingModality.BURST_INSAR,
                bursts,
                params,
                "corr-test-uuid"
        );

        JsonNode root = objectMapper.readTree(request.auditPayloadJson());
        JsonNode jobs = root.get("jobs");
        assertEquals(1, jobs.size());

        JsonNode job = jobs.get(0);
        assertEquals("INSAR_ISCE_BURST", job.get("job_type").asText());

        JsonNode jobParams = job.get("job_parameters");
        assertNotNull(jobParams);

        // Campos estritamente permitidos pelo OpenAPI
        assertTrue(jobParams.has("granules"));
        assertTrue(jobParams.has("looks"));
        assertTrue(jobParams.has("apply_water_mask"));

        JsonNode granules = jobParams.get("granules");
        assertEquals(2, granules.size());
        assertEquals(canonBurstRef19, granules.get(0).asText());
        assertEquals(canonBurstSec19, granules.get(1).asText());
        assertEquals("10x2", jobParams.get("looks").asText());
        assertTrue(jobParams.get("apply_water_mask").asBoolean());

        // Rejeição de campos indevidos: adicionais NÃO podem existir no contrato ISCE2 (additionalProperties: false)
        assertFalse(jobParams.has("filter_strength"), "filter_strength é indevido no HyP3 ISCE2");
        assertFalse(jobParams.has("dem_matching_resolution"), "dem_matching_resolution é indevido no HyP3 ISCE2");
        assertFalse(jobParams.has("include_coherence"), "include_coherence é indevido no HyP3 ISCE2 (gerado por padrão)");
        assertFalse(jobParams.has("include_displacement"), "include_displacement é indevido no HyP3 ISCE2");
        assertFalse(jobParams.has("include_unwrapped_phase"), "include_unwrapped_phase é indevido no HyP3 ISCE2");
        assertFalse(jobParams.has("bursts"), "chave 'bursts' não existe no OpenAPI");

        // Os nomes de cena SLC completa NÃO podem estar no array de granules do INSAR_ISCE_BURST
        assertFalse(granules.get(0).asText().contains("S1A_IW_SLC__"));
        assertFalse(granules.get(1).asText().contains("S1A_IW_SLC__"));
    }

    @Test
    @DisplayName("4. Serialização fiel ao contrato oficial INSAR_ISCE_MULTI_BURST: usa 'reference' e 'secondary', não 'granules'")
    void shouldBuildFaithfulMultiBurstPayloadConformingToOpenApiSchema() throws Exception {
        InterferometricPair pair = createEligiblePair();
        InSarProcessingParameters params = InSarProcessingParameters.defaultForUrbanMaceio();
        // 2 bursts de referência e 2 secundários
        List<String> bursts = List.of(
                canonBurstRef18, canonBurstRef19, // Referência
                canonBurstSec18, canonBurstSec19  // Secundária
        );

        InSarProcessingRequest request = adapter.buildDryRunRequest(
                "aoi-maceio-123",
                pair,
                ProcessingModality.MULTI_BURST,
                bursts,
                params,
                "corr-multi-test"
        );

        assertNotNull(request);
        assertEquals(ProcessingModality.MULTI_BURST, request.modality());

        JsonNode root = objectMapper.readTree(request.auditPayloadJson());
        JsonNode job = root.get("jobs").get(0);
        assertEquals("INSAR_ISCE_MULTI_BURST", job.get("job_type").asText());

        JsonNode jobParams = job.get("job_parameters");
        assertTrue(jobParams.has("reference"), "MULTI_BURST exige campo 'reference'");
        assertTrue(jobParams.has("secondary"), "MULTI_BURST exige campo 'secondary'");
        assertFalse(jobParams.has("granules"), "MULTI_BURST NÃO usa campo 'granules'");

        assertEquals(2, jobParams.get("reference").size());
        assertEquals(2, jobParams.get("secondary").size());
        assertEquals(canonBurstRef18, jobParams.get("reference").get(0).asText());
        assertEquals(canonBurstRef19, jobParams.get("reference").get(1).asText());
    }

    @Test
    @DisplayName("5. Rejeição de identificadores fictícios como 't009_018590_iw2'")
    void shouldRejectFictitiousBurstIdentifiers() {
        InterferometricPair pair = createEligiblePair();
        InSarProcessingParameters params = InSarProcessingParameters.defaultForUrbanMaceio();

        // Passando identificador simplificado/fictício não canônico
        List<String> invalidBursts = List.of("t009_018590_iw2", "t009_018591_iw2");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                adapter.buildDryRunRequest(
                        "aoi-maceio",
                        pair,
                        ProcessingModality.BURST_INSAR,
                        invalidBursts,
                        params,
                        "corr-invalid"
                )
        );

        assertTrue(ex.getMessage().contains("não segue o contrato canônico oficial da NASA ASF HyP3"));
        assertTrue(ex.getMessage().contains("t009_018590_iw2"));
    }

    @Test
    @DisplayName("6. Falta de identificadores burst para modalidade BURST_INSAR: rejeição mandatória")
    void shouldRejectWhenBurstIdentifiersAreMissingForBurstModality() {
        InterferometricPair pair = createEligiblePair();
        InSarProcessingParameters params = InSarProcessingParameters.defaultForUrbanMaceio();

        // Lista de bursts vazia para modalidade BURST_INSAR
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
                adapter.buildDryRunRequest("aoi-1", pair, ProcessingModality.BURST_INSAR, List.of(), params, "c1")
        );
        assertTrue(ex1.getMessage().contains("exige a especificação de identificadores canônicos de burst"));

        // Apenas 1 burst fornecido (requer par: ref e sec)
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
                adapter.buildDryRunRequest("aoi-1", pair, ProcessingModality.BURST_INSAR, List.of(canonBurstRef19), params, "c2")
        );
        assertTrue(ex2.getMessage().contains("exige exatamente 2 identificadores"));
    }

    @Test
    @DisplayName("7. Incompatibilidade geométrica de bursts (subswaths ou burst IDs diferentes)")
    void shouldRejectIncompatibleBurstPair() {
        InterferometricPair pair = createEligiblePair();
        InSarProcessingParameters params = InSarProcessingParameters.defaultForUrbanMaceio();

        // Bursts de relativeBurstId diferentes (018318 vs 018319) não podem formar um único par single-burst!
        List<String> mismatchedBursts = List.of(canonBurstRef18, canonBurstSec19);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                adapter.buildDryRunRequest("aoi-1", pair, ProcessingModality.BURST_INSAR, mismatchedBursts, params, "c3")
        );
        assertTrue(ex.getMessage().contains("Incompatibilidade geométrica entre os bursts"));
    }

    @Test
    @DisplayName("8. Elegibilidade insuficiente: rejeita construção de dry-run quando o par não for ELIGIBLE_FOR_SUBMISSION")
    void shouldRejectDryRunForIneligiblePair() {
        InterferometricPair pair = createRejectedPair();
        InSarProcessingParameters params = InSarProcessingParameters.defaultForUrbanMaceio();

        assertThrows(IneligiblePairProcessingException.class, () ->
                adapter.buildDryRunRequest("aoi-123", pair, ProcessingModality.BURST_INSAR, List.of(canonBurstRef19, canonBurstSec19), params, "corr-1")
        );
    }

    @Test
    @DisplayName("9. Barreira de segurança financeira: bloqueia submissão real quando executionEnabled=false e dry-run NUNCA chama endpoint externo")
    void shouldBlockActualSubmissionWhenFinancialBarrierActive() {
        InterferometricPair pair = createEligiblePair();
        InSarProcessingRequest request = adapter.buildDryRunRequest(
                "aoi-maceio-123",
                pair,
                ProcessingModality.BURST_INSAR,
                List.of(canonBurstRef19, canonBurstSec19),
                InSarProcessingParameters.defaultForUrbanMaceio(),
                "corr-1"
        );

        assertNotNull(request);
        assertTrue(request.dryRun());

        // A tentativa de submissão real DEVE disparar InSarExecutionBlockedException compulsoriamente
        InSarExecutionBlockedException ex = assertThrows(InSarExecutionBlockedException.class, () ->
                adapter.submitProcessingRequest(request)
        );

        assertTrue(ex.getMessage().contains("radar.processing.hyp3.execution-enabled=false"));
        assertFalse(adapter.isExecutionEnabled());
    }

    @Test
    @DisplayName("10. Estimativas de crédito conferidas e comprovadas na documentação oficial HyP3")
    void shouldVerifyCreditEstimatesAgainstOfficialDocumentation() {
        // Tabela Oficial: https://hyp3-docs.asf.alaska.edu/using/credits/
        // Single-Burst: 1 crédito
        assertEquals(1, ProcessingModality.BURST_INSAR.calculateOfficialCredits(1, "20x4"));
        assertEquals(1, ProcessingModality.BURST_INSAR.calculateOfficialCredits(1, "10x2"));
        assertEquals(1, ProcessingModality.BURST_INSAR.calculateOfficialCredits(1, "5x1"));

        // Multi-Burst (10x2): 1 a 3 pares = 1 crédito; 4 a 9 pares = 5 créditos
        assertEquals(1, ProcessingModality.MULTI_BURST.calculateOfficialCredits(2, "10x2"));
        assertEquals(5, ProcessingModality.MULTI_BURST.calculateOfficialCredits(5, "10x2"));

        // Multi-Burst (5x1): 1 par = 1; 2 pares = 5; 3 pares = 10
        assertEquals(1, ProcessingModality.MULTI_BURST.calculateOfficialCredits(1, "5x1"));
        assertEquals(5, ProcessingModality.MULTI_BURST.calculateOfficialCredits(2, "5x1"));
        assertEquals(10, ProcessingModality.MULTI_BURST.calculateOfficialCredits(3, "5x1"));

        // Full-Scene (GAMMA): 20x4 = 10 créditos; 10x2 = 15 créditos
        assertEquals(10, ProcessingModality.FULL_SCENE.calculateOfficialCredits(1, "20x4"));
        assertEquals(15, ProcessingModality.FULL_SCENE.calculateOfficialCredits(1, "10x2"));
    }

    @Test
    @DisplayName("11. Rejeição obrigatória de combinação de bursts da mesma data de aquisição")
    void shouldRejectSameDateBurstsInInterferometricPair() {
        InterferometricPair pair = createEligiblePair();
        InSarProcessingParameters params = InSarProcessingParameters.defaultForUrbanMaceio();

        // Single-Burst com dois bursts da mesma data (2023-01-04)
        List<String> sameDateSingle = List.of(canonBurstRef19, canonBurstRef19);
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
                adapter.buildDryRunRequest("aoi-1", pair, ProcessingModality.BURST_INSAR, sameDateSingle, params, "c1")
        );
        assertTrue(ex1.getMessage().contains("Incompatibilidade temporal"));

        // Multi-Burst com pares onde referência e secundária possuem mesma data
        List<String> sameDateMulti = List.of(
                canonBurstRef18, canonBurstRef19,
                canonBurstRef18, canonBurstRef19
        );
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
                adapter.buildDryRunRequest("aoi-1", pair, ProcessingModality.MULTI_BURST, sameDateMulti, params, "c2")
        );
        assertTrue(ex2.getMessage().contains("Incompatibilidade temporal"));
    }

    @Test
    @DisplayName("12. Rejeição de bursts com datas divergentes dentro do mesmo conjunto de referência ou secundário")
    void shouldRejectMixedDatesWithinReferenceOrSecondarySets() {
        InterferometricPair pair = createEligiblePair();
        InSarProcessingParameters params = InSarProcessingParameters.defaultForUrbanMaceio();

        // Conjunto de referência misturando bursts de 20230104 e 20230116
        List<String> mixedRef = List.of(
                canonBurstRef18, canonBurstSec19, // ref misturada (04/01 e 16/01)
                canonBurstSec18, canonBurstRef19  // sec correspondentes (16/01 e 04/01)
        );
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                adapter.buildDryRunRequest("aoi-1", pair, ProcessingModality.MULTI_BURST, mixedRef, params, "c3")
        );
        assertTrue(ex.getMessage().contains("datas divergentes"));
    }

    @Test
    @DisplayName("13. Rejeição de bursts duplicados dentro do mesmo conjunto de referência ou secundário")
    void shouldRejectDuplicateBurstsInMultiBurst() {
        InterferometricPair pair = createEligiblePair();
        InSarProcessingParameters params = InSarProcessingParameters.defaultForUrbanMaceio();

        // 2 bursts idênticos na referência
        List<String> dupRef = List.of(
                canonBurstRef19, canonBurstRef19,
                canonBurstSec19, canonBurstSec19
        );
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                adapter.buildDryRunRequest("aoi-1", pair, ProcessingModality.MULTI_BURST, dupRef, params, "c4")
        );
        assertTrue(ex.getMessage().contains("duplicado"));
    }
}
