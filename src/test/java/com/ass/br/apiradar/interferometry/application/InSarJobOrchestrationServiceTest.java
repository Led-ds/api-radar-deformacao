package com.ass.br.apiradar.interferometry.application;

import com.ass.br.apiradar.interferometry.application.services.InSarJobOrchestrationService;
import com.ass.br.apiradar.interferometry.domain.model.job.InSarExecutionReadinessReport;
import com.ass.br.apiradar.interferometry.domain.model.job.JobStatus;
import com.ass.br.apiradar.interferometry.domain.model.job.ProductQualityMetrics;
import com.ass.br.apiradar.interferometry.domain.model.job.TrackedInSarJob;
import com.ass.br.apiradar.interferometry.domain.ports.output.InSarJobTrackingPort;
import com.ass.br.apiradar.interferometry.domain.ports.output.InSarProductInspectionPort;
import com.ass.br.apiradar.interferometry.domain.ports.output.InSarProductStoragePort;
import com.ass.br.apiradar.interferometry.infrastructure.adapters.output.processing.Hyp3InSarProcessorAdapter;
import com.ass.br.apiradar.shared.domain.model.AoiId;
import com.ass.br.apiradar.shared.domain.model.OrganizationId;
import com.ass.br.apiradar.shared.domain.model.PolygonGeometry;
import com.ass.br.apiradar.territory.domain.model.AreaOfInterest;
import com.ass.br.apiradar.territory.domain.ports.output.AreaOfInterestRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@DisplayName("Ciclo 6.0 — Testes do Serviço de Orquestração InSAR e Prontidão Operacional")
class InSarJobOrchestrationServiceTest {

    private InSarJobTrackingPort trackingPort;
    private InSarProductStoragePort storagePort;
    private InSarProductInspectionPort inspectionPort;
    private AreaOfInterestRepositoryPort aoiRepositoryPort;
    private Hyp3InSarProcessorAdapter processorAdapter;

    private AreaOfInterest aoiMaceio;
    private final String aoiId = "1e7a1465-c3d7-44e6-8398-5ced7dac17ec";
    private final String pairId = "PAR-01__MACEIO";

    @BeforeEach
    void setUp() {
        trackingPort = Mockito.mock(InSarJobTrackingPort.class);
        storagePort = Mockito.mock(InSarProductStoragePort.class);
        inspectionPort = Mockito.mock(InSarProductInspectionPort.class);
        aoiRepositoryPort = Mockito.mock(AreaOfInterestRepositoryPort.class);
        processorAdapter = Mockito.mock(Hyp3InSarProcessorAdapter.class);

        PolygonGeometry geom = PolygonGeometry.fromWkt("POLYGON ((-35.75 -9.67, -35.72 -9.67, -35.72 -9.64, -35.75 -9.64, -35.75 -9.67))");
        aoiMaceio = AreaOfInterest.create(

                AoiId.of(aoiId),
                OrganizationId.of("d3b07384-d113-40a2-9721-a53b2169b821"),
                "Maceio Homologacao 5.1.1",
                geom,
                250.0
        );

        Mockito.when(aoiRepositoryPort.findById(eq(AoiId.of(aoiId)))).thenReturn(Optional.of(aoiMaceio));
    }

    @Test
    @DisplayName("Deve emitir parecer BLOCKED_CREDENTIALS_AND_POLICY quando credenciais faltam e execução desabilitada")
    void shouldReportBlockedWhenUnconfiguredAndDisabled() {
        InSarJobOrchestrationService service = new InSarJobOrchestrationService(
                trackingPort, storagePort, inspectionPort, aoiRepositoryPort, processorAdapter,
                "", "", "", false
        );

        InSarExecutionReadinessReport report = service.checkOperationalReadiness(pairId, aoiId);

        assertNotNull(report);
        assertEquals(pairId, report.pairId());
        assertEquals(aoiId, report.aoiId());
        assertTrue(report.pairEligible());
        assertFalse(report.credentialsConfigured());
        assertFalse(report.executionEnabled());
        assertEquals("BLOCKED_CREDENTIALS_AND_POLICY", report.readinessVerdict());
        assertEquals(2, report.blockerReasons().size());
        assertEquals(1, report.estimatedCredits());
        assertEquals("018319", report.targetBurst());
    }

    @Test
    @DisplayName("Deve emitir parecer READY_FOR_SUPERVISED_AUTHORIZATION quando credenciais existem mas política bloqueia")
    void shouldReportReadyForSupervisedAuthWhenCredentialsPresent() {
        InSarJobOrchestrationService service = new InSarJobOrchestrationService(
                trackingPort, storagePort, inspectionPort, aoiRepositoryPort, processorAdapter,
                "earthdata-user", "earthdata-pass", "", false
        );

        InSarExecutionReadinessReport report = service.checkOperationalReadiness(pairId, aoiId);

        assertNotNull(report);
        assertTrue(report.credentialsConfigured());
        assertFalse(report.executionEnabled());
        assertEquals("READY_FOR_SUPERVISED_AUTHORIZATION", report.readinessVerdict());
        assertEquals(1, report.blockerReasons().size());
        assertTrue(report.blockerReasons().get(0).contains("Barreira de segurança financeira ativa"));
    }

    @Test
    @DisplayName("Deve executar ciclo completo de ingestão e inspeção com mock de produto")
    void shouldIngestAndInspectProduct() {
        InSarJobOrchestrationService service = new InSarJobOrchestrationService(
                trackingPort, storagePort, inspectionPort, aoiRepositoryPort, processorAdapter,
                "user", "pass", "", false
        );

        String jobId = "job-exec-123";
        TrackedInSarJob job = new TrackedInSarJob(
                jobId,
                "idemp-123",
                pairId,
                aoiId,
                "INSAR_ISCE_BURST",
                JobStatus.SUCCEEDED,
                Instant.now(),
                null,
                List.of(),
                null,
                null,
                false,
                null
        );

        Mockito.when(trackingPort.getJobStatus(eq(jobId))).thenReturn(job);
        Path mockPath = Path.of("/tmp/insar-products/PAR-01/job-exec-123");
        Mockito.when(storagePort.storeJobProduct(eq(job), any(), any())).thenReturn(mockPath);

        ProductQualityMetrics expectedMetrics = new ProductQualityMetrics(
                jobId,
                pairId,
                aoiId,
                0.48,
                0.12,
                0.95,
                0.62,
                true,
                100000,
                85000,
                "EPSG:4326",
                true,
                "ACCEPTED",
                List.of()
        );

        Mockito.when(inspectionPort.inspectProduct(eq(jobId), eq(pairId), eq(mockPath), eq(aoiMaceio)))
                .thenReturn(expectedMetrics);

        ProductQualityMetrics result = service.ingestAndInspectProduct(jobId, aoiId, "dummy-zip".getBytes(), "expected-sha");

        assertNotNull(result);
        assertEquals("ACCEPTED", result.qualityVerdict());
        assertEquals(0.48, result.meanCoherence());
        assertTrue(result.spatialOverlapSatisfied());
        Mockito.verify(storagePort).storeJobProduct(eq(job), any(), any());
        Mockito.verify(trackingPort).registerOrUpdateJob(any());
    }
}
