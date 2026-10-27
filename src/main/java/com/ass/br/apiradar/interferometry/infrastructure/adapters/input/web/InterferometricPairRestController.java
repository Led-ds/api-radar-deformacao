package com.ass.br.apiradar.interferometry.infrastructure.adapters.input.web;

import com.ass.br.apiradar.interferometry.application.dto.GeneratePairsRequest;
import com.ass.br.apiradar.interferometry.application.dto.GeneratePairsResponseDto;
import com.ass.br.apiradar.interferometry.application.dto.PairSelectionPolicyDto;
import com.ass.br.apiradar.interferometry.application.services.InterferometricPairApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST oficial para formação e avaliação preliminar de pares interferométricos InSAR.
 */
@RestController
@RequestMapping("/api/v1/interferometry")
public class InterferometricPairRestController {

    private final InterferometricPairApplicationService pairService;
    private final com.ass.br.apiradar.interferometry.application.services.InSarProcessingPreparationService processingPreparationService;

    public InterferometricPairRestController(
            InterferometricPairApplicationService pairService,
            com.ass.br.apiradar.interferometry.application.services.InSarProcessingPreparationService processingPreparationService
    ) {
        this.pairService = pairService;
        this.processingPreparationService = processingPreparationService;
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
    public ResponseEntity<com.ass.br.apiradar.interferometry.application.dto.InSarProcessingDryRunResponseDto> generateDryRun(
            @PathVariable("pairId") String pairId,
            @RequestBody com.ass.br.apiradar.interferometry.application.dto.InSarProcessingDryRunRequestDto request
    ) {
        var response = processingPreparationService.generateDryRunRequest(pairId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retorna as opções dinâmicas de preparação do par: bursts candidatos correspondentes
     * para as duas datas, coberturas nominais da AOI, créditos e pendências.
     */
    @GetMapping("/pairs/{pairId}/processing/options")
    public ResponseEntity<com.ass.br.apiradar.interferometry.application.dto.InSarProcessingOptionsResponseDto> getProcessingOptions(
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
    public ResponseEntity<List<com.ass.br.apiradar.interferometry.domain.model.processing.ProcessingModality>> getSupportedModalities() {
        return ResponseEntity.ok(List.of(com.ass.br.apiradar.interferometry.domain.model.processing.ProcessingModality.values()));
    }
}
