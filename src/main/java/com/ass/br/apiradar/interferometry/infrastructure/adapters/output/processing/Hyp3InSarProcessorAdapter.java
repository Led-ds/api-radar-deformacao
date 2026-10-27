package com.ass.br.apiradar.interferometry.infrastructure.adapters.output.processing;

import com.ass.br.apiradar.interferometry.domain.exceptions.InSarExecutionBlockedException;
import com.ass.br.apiradar.interferometry.domain.exceptions.IneligiblePairProcessingException;
import com.ass.br.apiradar.interferometry.domain.model.InterferometricPair;
import com.ass.br.apiradar.interferometry.domain.model.PairEligibilityStatus;
import com.ass.br.apiradar.interferometry.domain.model.processing.BurstIdentifier;
import com.ass.br.apiradar.interferometry.domain.model.processing.InSarProcessingParameters;
import com.ass.br.apiradar.interferometry.domain.model.processing.InSarProcessingRequest;
import com.ass.br.apiradar.interferometry.domain.model.processing.ProcessingModality;
import com.ass.br.apiradar.interferometry.domain.model.processing.ProcessorEligibilityResult;
import com.ass.br.apiradar.interferometry.domain.ports.output.InSarProcessorPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/**
 * Adaptador de infraestrutura homologado para integração com a API oficial da NASA ASF HyP3.
 * Traduz os conceitos de domínio para os contratos canônicos oficiais (OpenAPI schema e hyp3-sdk):
 * - INSAR_GAMMA (Full-Scene IW SLC com GAMMA)
 * - INSAR_ISCE_BURST (Single-Burst IW SLC com ISCE2)
 * - INSAR_ISCE_MULTI_BURST (Multi-Burst IW SLC com ISCE2)
 * <p>
 * Regras estritas de conformidade:
 * 1. Separação rigorosa entre parâmetros científicos internos do Radar++ e parâmetros transmitidos ao provedor.
 * 2. Rejeição compulsoria de campos não suportados (ex: filter_strength ou include_coherence em ISCE2).
 * 3. Validação canônica de identificadores de burst via regex oficial (rejeita placeholders como t009_018590_iw2).
 * 4. Barreira mandatória de segurança financeira (radar.processing.hyp3.execution-enabled=false).
 */
@Component
public class Hyp3InSarProcessorAdapter implements InSarProcessorPort {

    private static final Logger log = LoggerFactory.getLogger(Hyp3InSarProcessorAdapter.class);

    private final boolean executionEnabled;
    private final String hyp3ApiUrl;
    private final ObjectMapper objectMapper;

    public Hyp3InSarProcessorAdapter(
            @Value("${radar.processing.hyp3.execution-enabled:false}") boolean executionEnabled,
            @Value("${radar.processing.hyp3.api-url:https://hyp3-api.asf.alaska.edu}") String hyp3ApiUrl,
            ObjectMapper objectMapper
    ) {
        this.executionEnabled = executionEnabled;
        this.hyp3ApiUrl = hyp3ApiUrl;
        this.objectMapper = objectMapper;
    }

    public boolean isExecutionEnabled() {
        return executionEnabled;
    }

    public String getHyp3ApiUrl() {
        return hyp3ApiUrl;
    }

    @Override
    public List<ProcessingModality> getSupportedModalities() {
        return List.of(
                ProcessingModality.BURST_INSAR,
                ProcessingModality.MULTI_BURST,
                ProcessingModality.FULL_SCENE
        );
    }

    @Override
    public int estimateCredits(ProcessingModality modality, List<String> burstIds) {
        if (modality == null) {
            return 0;
        }
        int pairCount = 1;
        if (modality == ProcessingModality.MULTI_BURST && burstIds != null && !burstIds.isEmpty()) {
            pairCount = Math.max(1, burstIds.size() / 2);
        }
        return modality.calculateOfficialCredits(pairCount, "10x2");
    }

    @Override
    public ProcessorEligibilityResult validateProcessorEligibility(InterferometricPair pair, List<String> burstIds) {
        if (pair == null) {
            throw new IllegalArgumentException("Par interferométrico não pode ser nulo.");
        }

        List<String> details = new ArrayList<>();
        boolean eligible = true;

        if (pair.getStatus() != PairEligibilityStatus.ELIGIBLE_FOR_SUBMISSION) {
            eligible = false;
            details.add("O par não possui estado ELIGIBLE_FOR_SUBMISSION (estado atual: " + pair.getStatus() + ").");
        }

        // Validação de modo de aquisição Sentinel-1
        if (!pair.getReferenceScene().sceneName().contains("_IW_") ||
                !pair.getSecondaryScene().sceneName().contains("_IW_")) {
            eligible = false;
            details.add("Processador HyP3 exige imagens Sentinel-1 no modo IW (Interferometric Wide Swath).");
        }

        // Validação de tipo de produto complexo Level-1 SLC
        if (!pair.getReferenceScene().sceneName().contains("_SLC_") ||
                !pair.getSecondaryScene().sceneName().contains("_SLC_")) {
            eligible = false;
            details.add("Processador HyP3 exige produtos complexos Level-1 SLC.");
        }

        // Validação de identificadores de burst fornecidos
        if (burstIds != null && !burstIds.isEmpty()) {
            List<String> invalidBursts = new ArrayList<>();
            for (String b : burstIds) {
                if (!BurstIdentifier.isValidCanonical(b)) {
                    invalidBursts.add(b);
                }
            }
            if (!invalidBursts.isEmpty()) {
                eligible = false;
                details.add(String.format(
                        "Identificadores de burst inválidos detectados: %s. " +
                                "O contrato oficial ASF HyP3 exige formato canônico S1_RRRRRR_IWX_YYYYMMDDTHHMMSS_PP_CCCC-BURST. " +
                                "Identificadores sintéticos (ex.: 't009_018590_iw2') violam o schema OpenAPI da NASA e serão rejeitados.",
                        invalidBursts
                ));
            }
        }

        if (eligible) {
            details.add("Par homologado para submissão ao processador HyP3 (ASF).");
            details.add("Baseline perpendicular compatível: " +
                    (pair.getPerpendicularBaseline() != null ? pair.getPerpendicularBaseline().meters() + "m" : "N/D"));
            details.add("Intervalo temporal: " + pair.getTemporalBaselineDays() + " dias.");
        }

        Map<ProcessingModality, Integer> credits = Map.of(
                ProcessingModality.BURST_INSAR, estimateCredits(ProcessingModality.BURST_INSAR, burstIds),
                ProcessingModality.MULTI_BURST, estimateCredits(ProcessingModality.MULTI_BURST, burstIds),
                ProcessingModality.FULL_SCENE, estimateCredits(ProcessingModality.FULL_SCENE, burstIds)
        );

        return new ProcessorEligibilityResult(
                pair.getId().value(),
                "ASF HyP3 (GAMMA / ISCE2)",
                eligible,
                details,
                getSupportedModalities(),
                credits,
                Instant.now()
        );
    }

    @Override
    public InSarProcessingRequest buildDryRunRequest(
            String aoiId,
            InterferometricPair pair,
            ProcessingModality modality,
            List<String> burstIds,
            InSarProcessingParameters parameters,
            String correlationId
    ) {
        if (pair == null) {
            throw new IllegalArgumentException("Par não pode ser nulo.");
        }
        if (pair.getStatus() != PairEligibilityStatus.ELIGIBLE_FOR_SUBMISSION) {
            throw new IneligiblePairProcessingException(
                    "Não é permitido construir requisição de processamento para o par " + pair.getId().value() +
                            " pois seu estado é " + pair.getStatus() + ". Apenas pares ELIGIBLE_FOR_SUBMISSION são aceitos."
            );
        }

        if (modality == null) {
            modality = ProcessingModality.BURST_INSAR;
        }
        if (parameters == null) {
            parameters = InSarProcessingParameters.defaultForUrbanMaceio();
        }
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        List<String> bursts = burstIds != null ? List.copyOf(burstIds) : List.of();

        // 1. Validação estrita de insumos por modalidade e construção do job_parameters oficial
        Map<String, Object> jobParams = buildOfficialJobParameters(modality, pair, bursts, parameters);

        Map<String, Object> jobSpec = new LinkedHashMap<>();
        jobSpec.put("job_type", modality.getOfficialJobType());
        jobSpec.put("name", "radar_plus_pair_" + pair.getId().value());
        jobSpec.put("job_parameters", jobParams);

        // 2. Cálculo determinístico de créditos e idempotency key
        int credits = modality.calculateOfficialCredits(
                modality == ProcessingModality.MULTI_BURST ? Math.max(1, bursts.size() / 2) : 1,
                parameters.looks()
        );

        String idempotencyKey = InSarProcessingRequest.calculateIdempotencyKey(
                aoiId,
                pair.getId().value(),
                modality,
                bursts,
                parameters
        );

        // 3. Payload externo oficial estritamente aderente ao schema OpenAPI v3.0 da NASA ASF HyP3 (POST /jobs).
        // Conforme especificação oficial: additionalProperties=false; propriedades aceitas: 'validate_only' e 'jobs'.
        // Nenhuma propriedade interna do Radar++ (dryRun, correlationId, idempotencyKey, aoiId) é injetada no payload externo.
        Map<String, Object> hyp3OfficialPayload = new LinkedHashMap<>();
        hyp3OfficialPayload.put("validate_only", true);
        hyp3OfficialPayload.put("jobs", List.of(jobSpec));

        String officialExternalJson;
        try {
            officialExternalJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(hyp3OfficialPayload);
        } catch (Exception e) {
            officialExternalJson = "{\"error\": \"Falha ao serializar payload oficial HyP3: " + e.getMessage() + "\"}";
        }

        log.info("[DRY-RUN_HOMOLOGADO] Requisição InSAR gerada com sucesso para o par {}. Modalidade: {} ({}), Créditos: {}, IdempotencyKey: {}",
                pair.getId().value(), modality, modality.getOfficialJobType(), credits, idempotencyKey);

        return new InSarProcessingRequest(
                UUID.randomUUID(),
                idempotencyKey,
                correlationId,
                aoiId,
                pair.getId().value(),
                pair.getReferenceScene().sceneName(),
                pair.getSecondaryScene().sceneName(),
                bursts,
                modality,
                parameters, // Parâmetros científicos internos do Radar++ preservados
                modality.getDefaultPipeline(),
                pair.getAssessment().policyProfileUsed() != null ? pair.getAssessment().policyProfileUsed() : "TROPICAL_URBAN_MACEIO (v1.1)",
                credits,
                true, // Dry-Run rigorosamente garantido
                officialExternalJson,
                Instant.now()
        );
    }

    private Map<String, Object> buildOfficialJobParameters(
            ProcessingModality modality,
            InterferometricPair pair,
            List<String> bursts,
            InSarProcessingParameters parameters
    ) {
        Map<String, Object> jobParams = new LinkedHashMap<>();

        switch (modality) {
            case BURST_INSAR -> {
                // INSAR_ISCE_BURST: Requer exatamente 2 identificadores canônicos de burst (referência e secundária)
                if (bursts.isEmpty()) {
                    throw new IllegalArgumentException(
                            "A modalidade Single-Burst (INSAR_ISCE_BURST) exige a especificação de identificadores canônicos de burst. " +
                                    "Nenhum burst fornecido para o par " + pair.getId().value() + "."
                    );
                }
                if (bursts.size() != 2) {
                    throw new IllegalArgumentException(
                            "A modalidade Single-Burst (INSAR_ISCE_BURST) exige exatamente 2 identificadores canônicos de burst " +
                                    "(um de referência e um secundário correspondente). Recebido: " + bursts.size() + " bursts."
                    );
                }

                BurstIdentifier bRef = BurstIdentifier.of(bursts.get(0));
                BurstIdentifier bSec = BurstIdentifier.of(bursts.get(1));

                if (!bRef.isGeometricallyCompatibleWith(bSec)) {
                    throw new IllegalArgumentException(String.format(
                            "Incompatibilidade geométrica entre os bursts do par: ref=[relId=%d, subswath=%s, pol=%s] vs sec=[relId=%d, subswath=%s, pol=%s]. " +
                                    "O par de bursts deve compartilhar mesmo burst ID relativo, mesmo subswath e mesma polarização.",
                            bRef.relativeBurstId(), bRef.subswath(), bRef.polarization(),
                            bSec.relativeBurstId(), bSec.subswath(), bSec.polarization()
                    ));
                }

                // Validação de datas: referência e secundária não podem possuir a mesma data de aquisição
                java.time.LocalDate refDate = java.time.LocalDate.ofInstant(bRef.acquisitionTime(), java.time.ZoneOffset.UTC);
                java.time.LocalDate secDate = java.time.LocalDate.ofInstant(bSec.acquisitionTime(), java.time.ZoneOffset.UTC);
                if (refDate.equals(secDate)) {
                    throw new IllegalArgumentException(String.format(
                            "Incompatibilidade temporal: os bursts de referência (%s) e secundário (%s) possuem a mesma data (%s). " +
                                    "Um par interferométrico exige aquisições em datas distintas.",
                            bRef.value(), bSec.value(), refDate
                    ));
                }

                // Contrato OpenAPI estrito: apenas 'granules', 'looks', 'apply_water_mask' (additionalProperties: false)
                jobParams.put("granules", List.of(bRef.value(), bSec.value()));
                jobParams.put("looks", parameters.looks());
                jobParams.put("apply_water_mask", parameters.applyWaterMask());
            }

            case MULTI_BURST -> {
                // INSAR_ISCE_MULTI_BURST: Requer listas separadas 'reference' e 'secondary' de bursts canônicos
                if (bursts.isEmpty() || bursts.size() < 4 || bursts.size() % 2 != 0) {
                    throw new IllegalArgumentException(
                            "A modalidade Multi-Burst (INSAR_ISCE_MULTI_BURST) exige no mínimo 2 pares de bursts " +
                                    "(pelo menos 4 identificadores canônicos: N de referência e N secundários correspondentes). Recebido: " + bursts.size()
                    );
                }

                int half = bursts.size() / 2;
                List<String> refBursts = new ArrayList<>();
                List<String> secBursts = new ArrayList<>();
                Set<Integer> seenRefRelIds = new HashSet<>();
                Set<Integer> seenSecRelIds = new HashSet<>();
                java.time.LocalDate commonRefDate = null;
                java.time.LocalDate commonSecDate = null;

                for (int i = 0; i < half; i++) {
                    BurstIdentifier br = BurstIdentifier.of(bursts.get(i));
                    BurstIdentifier bs = BurstIdentifier.of(bursts.get(i + half));

                    if (!br.isGeometricallyCompatibleWith(bs)) {
                        throw new IllegalArgumentException(String.format(
                                "Incompatibilidade no par multi-burst [%d]: ref=%s vs sec=%s", i, br.value(), bs.value()
                        ));
                    }

                    java.time.LocalDate rDate = java.time.LocalDate.ofInstant(br.acquisitionTime(), java.time.ZoneOffset.UTC);
                    java.time.LocalDate sDate = java.time.LocalDate.ofInstant(bs.acquisitionTime(), java.time.ZoneOffset.UTC);

                    if (rDate.equals(sDate)) {
                        throw new IllegalArgumentException(String.format(
                                "Incompatibilidade temporal no par multi-burst [%d]: referência e secundária possuem a mesma data (%s).",
                                i, rDate
                        ));
                    }

                    if (commonRefDate == null) {
                        commonRefDate = rDate;
                    } else if (!commonRefDate.equals(rDate)) {
                        throw new IllegalArgumentException(String.format(
                                "Bursts de referência com datas divergentes no conjunto multi-burst: %s vs %s.",
                                commonRefDate, rDate
                        ));
                    }

                    if (commonSecDate == null) {
                        commonSecDate = sDate;
                    } else if (!commonSecDate.equals(sDate)) {
                        throw new IllegalArgumentException(String.format(
                                "Bursts secundários com datas divergentes no conjunto multi-burst: %s vs %s.",
                                commonSecDate, sDate
                        ));
                    }

                    if (!seenRefRelIds.add(br.relativeBurstId())) {
                        throw new IllegalArgumentException(String.format(
                                "Burst de referência duplicado no conjunto multi-burst: relativeBurstId=%d", br.relativeBurstId()
                        ));
                    }
                    if (!seenSecRelIds.add(bs.relativeBurstId())) {
                        throw new IllegalArgumentException(String.format(
                                "Burst secundário duplicado no conjunto multi-burst: relativeBurstId=%d", bs.relativeBurstId()
                        ));
                    }

                    refBursts.add(br.value());
                    secBursts.add(bs.value());
                }

                // Contrato OpenAPI estrito: 'reference', 'secondary', 'looks', 'apply_water_mask' (NÃO usa 'granules'!)
                jobParams.put("reference", refBursts);
                jobParams.put("secondary", secBursts);
                jobParams.put("looks", parameters.looks());
                jobParams.put("apply_water_mask", parameters.applyWaterMask());
            }

            case FULL_SCENE -> {
                // INSAR_GAMMA: Requer 2 cenas SLC completas no array 'granules'
                jobParams.put("granules", List.of(
                        pair.getReferenceScene().sceneName(),
                        pair.getSecondaryScene().sceneName()
                ));
                jobParams.put("looks", "10x2".equalsIgnoreCase(parameters.looks()) ? "10x2" : "20x4");
                jobParams.put("apply_water_mask", parameters.applyWaterMask());
                jobParams.put("phase_filter_parameter", parameters.filterStrength());
                jobParams.put("include_look_vectors", false);
                jobParams.put("include_inc_map", false);
                jobParams.put("include_dem", false);
                jobParams.put("include_wrapped_phase", false);
                jobParams.put("include_displacement_maps", parameters.includeDisplacement());
            }
        }

        return jobParams;
    }

    @Override
    public InSarProcessingRequest submitProcessingRequest(InSarProcessingRequest request) {
        if (!executionEnabled) {
            log.warn("[SEGURANCA_FINANCEIRA_BLOQUEIO] Submissão real de processamento InSAR bloqueada. radar.processing.hyp3.execution-enabled=false. IdempotencyKey: {}",
                    request != null ? request.idempotencyKey() : "N/D");
            throw new InSarExecutionBlockedException(
                    "A submissão operacional de processamento InSAR no cluster HyP3 está desabilitada por política de segurança financeira " +
                            "(radar.processing.hyp3.execution-enabled=false). Apenas geração de requisição dry-run é permitida no Ciclo 5.4A."
            );
        }

        // Salvaguarda: mesmo que executionEnabled venha a ser true no futuro, no Ciclo 5.4A nunca disparará
        throw new InSarExecutionBlockedException("Submissão real não implementada nesta fase. Apenas dry-run permitido.");
    }
}
