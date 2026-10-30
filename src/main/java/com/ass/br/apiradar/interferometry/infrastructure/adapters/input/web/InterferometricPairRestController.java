package com.ass.br.apiradar.interferometry.infrastructure.adapters.input.web;

import com.ass.br.apiradar.interferometry.application.dto.*;
import com.ass.br.apiradar.interferometry.application.services.InSarJobOrchestrationService;
import com.ass.br.apiradar.interferometry.application.services.InSarProcessingPreparationService;
import com.ass.br.apiradar.interferometry.application.services.InterferometricPairApplicationService;
import com.ass.br.apiradar.interferometry.domain.model.job.InSarExecutionReadinessReport;
import com.ass.br.apiradar.interferometry.domain.model.job.ProductQualityMetrics;
import com.ass.br.apiradar.interferometry.domain.model.job.TrackedInSarJob;
import com.ass.br.apiradar.interferometry.domain.model.processing.ProcessingModality;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST oficial para formação e avaliação preliminar de pares interferométricos InSAR,
 * prontidão operacional e acompanhamento de jobs.
 */
@RestController
@RequestMapping("/api/v1/interferometry")
public class InterferometricPairRestController {

    private final InterferometricPairApplicationService pairService;
    private final InSarProcessingPreparationService processingPreparationService;
    private final InSarJobOrchestrationService orchestrationService;

    public InterferometricPairRestController(
            InterferometricPairApplicationService pairService,
            InSarProcessingPreparationService processingPreparationService,
            InSarJobOrchestrationService orchestrationService
    ) {
        this.pairService = pairService;
        this.processingPreparationService = processingPreparationService;
        this.orchestrationService = orchestrationService;
    }

    /**
     * Gera e avalia pares interferométricos candidatos para a Área de Interesse informada.
     */
    @PostMapping("/pairs/candidates")
    public ResponseEntity<GeneratePairsResponseDto> generateCandidatePairs(
            @Valid @RequestBody GeneratePairsRequest request
    ) {
        GeneratePairsResponseDto response = pairService.generatePairs(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retorna a lista de perfis de políticas científicas configuráveis do sistema.
     */
    @GetMapping("/policies")
    public ResponseEntity<List<PairSelectionPolicyDto>> getAvailablePolicies() {
        List<PairSelectionPolicyDto> policies = pairService.getAvailablePolicies();
        return ResponseEntity.ok(policies);
    }

    /**
     * Gera uma requisição auditável de processamento InSAR em modo DRY-RUN para um par elegível,
     * sem disparar processamento na nuvem ou consumir créditos.
     */
    @PostMapping("/pairs/{pairId}/processing/dry-run")
    public ResponseEntity<InSarProcessingDryRunResponseDto> generateDryRun(
            @PathVariable("pairId") String pairId,
            @RequestBody InSarProcessingDryRunRequestDto request
    ) {
        var response = processingPreparationService.generateDryRunRequest(pairId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retorna as opções dinâmicas de preparação do par: bursts candidatos correspondentes
     * para as duas datas, coberturas nominais da AOI, créditos e pendências.
     */
    @GetMapping("/pairs/{pairId}/processing/options")
    public ResponseEntity<InSarProcessingOptionsResponseDto> getProcessingOptions(
            @PathVariable("pairId") String pairId,
            @RequestParam("aoiId") String aoiId
    ) {
        var options = processingPreparationService.getProcessingOptions(pairId, aoiId);
        return ResponseEntity.ok(options);
    }

    /**
     * Retorna as modalidades de processamento InSAR suportadas oficialmente pela esteira científica.
     */
    @GetMapping("/processing/modalities")
    public ResponseEntity<List<ProcessingModality>> getSupportedModalities() {
        return ResponseEntity.ok(List.of(ProcessingModality.values()));
    }

    /**
     * Avalia a prontidão operacional e científica antes da submissão do primeiro processamento real do par.
     */
    @GetMapping("/pairs/{pairId}/readiness")
    public ResponseEntity<InSarExecutionReadinessReport> getExecutionReadiness(
            @PathVariable("pairId") String pairId,
            @RequestParam("aoiId") String aoiId
    ) {
        InSarExecutionReadinessReport report = orchestrationService.checkOperationalReadiness(pairId, aoiId);
        return ResponseEntity.ok(report);
    }

    /**
     * Consulta o status atualizado do job InSAR no registro rastreado ou no cluster HyP3.
     */
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<TrackedInSarJob> getJobStatus(
            @PathVariable("jobId") String jobId
    ) {
        TrackedInSarJob job = orchestrationService.getJobStatus(jobId);
        return ResponseEntity.ok(job);
    }

    /**
     * Inspeciona os produtos locais gerados por um job já descompactado em disco.
     */
    @GetMapping("/jobs/{jobId}/inspection")
    public ResponseEntity<ProductQualityMetrics> inspectJobProduct(
            @PathVariable("jobId") String jobId,
            @RequestParam(value = "aoiId", required = false) String aoiId
    ) {
        ProductQualityMetrics metrics = orchestrationService.inspectStoredProduct(jobId, aoiId);
        return ResponseEntity.ok(metrics);
    }
}
