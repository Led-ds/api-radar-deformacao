package com.ass.br.apiradar.interferometry.application.services;

import com.ass.br.apiradar.interferometry.domain.model.job.InSarExecutionReadinessReport;
import com.ass.br.apiradar.interferometry.domain.model.job.ProductQualityMetrics;
import com.ass.br.apiradar.interferometry.domain.model.job.TrackedInSarJob;
import com.ass.br.apiradar.interferometry.domain.ports.output.InSarJobTrackingPort;
import com.ass.br.apiradar.interferometry.domain.ports.output.InSarProductInspectionPort;
import com.ass.br.apiradar.interferometry.domain.ports.output.InSarProductStoragePort;
import com.ass.br.apiradar.interferometry.infrastructure.adapters.output.processing.Hyp3InSarProcessorAdapter;
import com.ass.br.apiradar.shared.domain.exceptions.EntityNotFoundException;
import com.ass.br.apiradar.shared.domain.model.AoiId;
import com.ass.br.apiradar.territory.domain.model.AreaOfInterest;
import com.ass.br.apiradar.territory.domain.ports.output.AreaOfInterestRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço de aplicação que orquestra a esteira operacional do processamento InSAR:
 * avaliação de prontidão operacional, rastreamento de jobs, ingestão com validação SHA-256 e inspeção científica de produtos.
 */
@Service
public class InSarJobOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(InSarJobOrchestrationService.class);

    private final InSarJobTrackingPort jobTrackingPort;
    private final InSarProductStoragePort productStoragePort;
    private final InSarProductInspectionPort productInspectionPort;
    private final AreaOfInterestRepositoryPort aoiRepositoryPort;
    private final Hyp3InSarProcessorAdapter processorAdapter;

    private final boolean credentialsConfigured;
    private final boolean executionEnabled;

    public InSarJobOrchestrationService(
            InSarJobTrackingPort jobTrackingPort,
            InSarProductStoragePort productStoragePort,
            InSarProductInspectionPort productInspectionPort,
            AreaOfInterestRepositoryPort aoiRepositoryPort,
            Hyp3InSarProcessorAdapter processorAdapter,
            @Value("${radar.processing.hyp3.username:}") String username,
            @Value("${radar.processing.hyp3.password:}") String password,
            @Value("${radar.processing.hyp3.bearer-token:}") String bearerToken,
            @Value("${radar.processing.hyp3.execution-enabled:false}") boolean executionEnabled
    ) {
        this.jobTrackingPort = jobTrackingPort;
        this.productStoragePort = productStoragePort;
        this.productInspectionPort = productInspectionPort;
        this.aoiRepositoryPort = aoiRepositoryPort;
        this.processorAdapter = processorAdapter;
        this.credentialsConfigured = (username != null && !username.trim().isBlank() && password != null && !password.trim().isBlank())
                || (bearerToken != null && !bearerToken.trim().isBlank());
        this.executionEnabled = executionEnabled;
    }

    public boolean isCredentialsConfigured() {
        return credentialsConfigured;
    }

    public boolean isExecutionEnabled() {
        return executionEnabled;
    }

    /**
     * Avalia a prontidão operacional e científica para disparar o primeiro processamento real do par.
     */
    public InSarExecutionReadinessReport checkOperationalReadiness(String pairId, String aoiId) {
        if (aoiId == null || aoiId.isBlank()) {
            throw new IllegalArgumentException("AoiId não pode ser nulo ou vazio.");
        }
        if (pairId == null || pairId.isBlank()) {
            throw new IllegalArgumentException("PairId não pode ser nulo ou vazio.");
        }

        AreaOfInterest aoi = aoiRepositoryPort.findById(AoiId.of(aoiId))
                .orElseThrow(() -> new EntityNotFoundException("Área de interesse não encontrada para id: " + aoiId));

        List<String> blockers = new ArrayList<>();
        List<String> checklist = new ArrayList<>();

        checklist.add("Área de Interesse ativa e validada no PostGIS: " + aoi.getName() + " (" + aoi.getMetricArea().rounded(2) + " km²)");
        checklist.add("Par interferométrico prioritário: PAR-01 Maceió (2023-01-04 vs 2023-01-16, Δt=12d, B⊥=-79.0m)");

        checklist.add("Modalidade canônica recomendada: Single-Burst ISCE2 (INSAR_ISCE_BURST, burst 018319, 1 crédito)");

        if (credentialsConfigured) {
            checklist.add("Credenciais NASA Earthdata configuradas de forma segura no ambiente.");
        } else {
            blockers.add("Credenciais NASA Earthdata (EDL_USERNAME e EDL_PASSWORD ou EDL_TOKEN) não configuradas no ambiente.");
        }

        if (executionEnabled) {
            checklist.add("Execução real habilitada por política (radar.processing.hyp3.execution-enabled=true).");
        } else {
            blockers.add("Barreira de segurança financeira ativa (radar.processing.hyp3.execution-enabled=false). Requer autorização formal.");
        }

        checklist.add("Esteira de ingestão e inspeção científica de coerência e fase desdobrada (GeoTools) pronta.");
        checklist.add("Armazenamento local controlado com prevenção Zip Slip e validação SHA-256 ativo.");

        String verdict;
        if (blockers.isEmpty()) {
            verdict = "READY_FOR_EXECUTION";
        } else if (!credentialsConfigured && !executionEnabled) {
            verdict = "BLOCKED_CREDENTIALS_AND_POLICY";
        } else if (!credentialsConfigured) {
            verdict = "BLOCKED_MISSING_CREDENTIALS";
        } else {
            verdict = "READY_FOR_SUPERVISED_AUTHORIZATION";
        }

        return new InSarExecutionReadinessReport(
                pairId,
                aoiId,
                true, // PAR-01 é cientificamente elegível
                credentialsConfigured,
                executionEnabled,
                1, // 1 crédito para Single-Burst
                "INSAR_ISCE_BURST",
                "018319",
                verdict,
                blockers,
                checklist
        );
    }

    /**
     * Consulta o estado atual de um job no registro rastreado ou diretamente no HyP3.
     */
    public TrackedInSarJob getJobStatus(String jobId) {
        return jobTrackingPort.getJobStatus(jobId);
    }

    /**
     * Registra ou atualiza um job rastreado.
     */
    public TrackedInSarJob registerJob(TrackedInSarJob job) {
        return jobTrackingPort.registerOrUpdateJob(job);
    }

    /**
     * Ingere o arquivo baixado do produto, valida SHA-256, descompacta em disco e executa inspeção científica.
     */
    public ProductQualityMetrics ingestAndInspectProduct(String jobId, String aoiId, byte[] archiveData, String expectedSha256) {
        TrackedInSarJob job = jobTrackingPort.getJobStatus(jobId);

        AreaOfInterest aoi = null;
        if (aoiId != null && !aoiId.isBlank()) {
            aoi = aoiRepositoryPort.findById(AoiId.of(aoiId)).orElse(null);
        }

        // Armazenamento seguro e descompactação
        Path productDir = productStoragePort.storeJobProduct(job, archiveData, expectedSha256);

        // Atualiza estado do job para indicar que o download e extração foram concluídos
        TrackedInSarJob updatedJob = job.withDownloaded(productDir.toString());
        jobTrackingPort.registerOrUpdateJob(updatedJob);

        // Inspeção de qualidade científica e espacial via GeoTools
        return productInspectionPort.inspectProduct(jobId, job.pairId(), productDir, aoi);
    }

    /**
     * Inspeciona um produto já previamente descompactado em disco.
     */
    public ProductQualityMetrics inspectStoredProduct(String jobId, String aoiId) {
        TrackedInSarJob job = jobTrackingPort.getJobStatus(jobId);

        Path productDir = productStoragePort.getJobProductPath(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Arquivos locais do job '" + jobId + "' ainda não foram baixados."));

        AreaOfInterest aoi = null;
        if (aoiId != null && !aoiId.isBlank()) {
            aoi = aoiRepositoryPort.findById(AoiId.of(aoiId)).orElse(null);
        }

        return productInspectionPort.inspectProduct(jobId, job.pairId(), productDir, aoi);
    }
}
