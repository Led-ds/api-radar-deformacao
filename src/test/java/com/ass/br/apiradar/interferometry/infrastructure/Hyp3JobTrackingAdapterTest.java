package com.ass.br.apiradar.interferometry.infrastructure;

import com.ass.br.apiradar.interferometry.domain.model.job.JobStatus;
import com.ass.br.apiradar.interferometry.domain.model.job.TrackedInSarJob;
import com.ass.br.apiradar.interferometry.infrastructure.adapters.output.tracking.Hyp3JobTrackingAdapter;
import com.ass.br.apiradar.shared.domain.exceptions.EntityNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Ciclo 6.0 — Testes do Adaptador de Rastreamento de Jobs HyP3 e Idempotência")
class Hyp3JobTrackingAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Hyp3JobTrackingAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new Hyp3JobTrackingAdapter(
                "https://hyp3-api.asf.alaska.edu",
                "test-user",
                "test-pass",
                "",
                objectMapper
        );
    }

    @Test
    @DisplayName("Deve registrar job e permitir recuperação por chave determinística de idempotência")
    void shouldRegisterAndRetrieveJobByIdempotencyKey() {
        String jobId = "job-uuid-12345";
        String idempotencyKey = "sha256-idemp-123456789";

        TrackedInSarJob job = new TrackedInSarJob(
                jobId,
                idempotencyKey,
                "PAR-01__MACEIO",
                "aoi-maceio-mutange",
                "INSAR_ISCE_BURST",
                JobStatus.RUNNING,
                Instant.now(),
                null,
                List.of(),
                null,
                null,
                false,
                null
        );

        adapter.registerOrUpdateJob(job);

        Optional<TrackedInSarJob> found = adapter.findByIdempotencyKey(idempotencyKey);
        assertTrue(found.isPresent());
        assertEquals(jobId, found.get().jobId());
        assertEquals(JobStatus.RUNNING, found.get().status());
    }

    @Test
    @DisplayName("Deve deserializar resposta oficial de job HyP3 com status SUCCEEDED e arquivos")
    void shouldParseHyp3SucceededResponse() {
        String json = """
        {
          "job_id": "c1a2b3c4-5678-90ab-cdef-1234567890ab",
          "job_type": "INSAR_ISCE_BURST",
          "status_code": "SUCCEEDED",
          "request_time": "2023-01-16T12:00:00Z",
          "expiration_time": "2023-01-30T12:00:00Z",
          "files": [
            {
              "filename": "S1_018319_IW1_20230104_20230116_unw_phase.tif",
              "url": "https://datapool.asf.alaska.edu/unw.tif",
              "size": 5242880,
              "sha256": "abcdef1234567890"
            },
            {
              "filename": "S1_018319_IW1_20230104_20230116_corr.tif",
              "url": "https://datapool.asf.alaska.edu/corr.tif",
              "size": 2097152,
              "sha256": "1234567890abcdef"
            }
          ]
        }
        """;

        TrackedInSarJob job = adapter.parseHyp3JobResponse(json, null);

        assertNotNull(job);
        assertEquals("c1a2b3c4-5678-90ab-cdef-1234567890ab", job.jobId());
        assertEquals("INSAR_ISCE_BURST", job.jobType());
        assertEquals(JobStatus.SUCCEEDED, job.status());
        assertEquals(2, job.files().size());
        assertEquals("UNWRAPPED_PHASE", job.files().get(0).fileType());
        assertEquals("COHERENCE", job.files().get(1).fileType());
    }

    @Test
    @DisplayName("Deve deserializar resposta oficial de job HyP3 com status FAILED e registrar motivo de falha")
    void shouldParseHyp3FailedResponse() {
        String json = """
        {
          "job_id": "f1f2f3f4-5678-90ab-cdef-1234567890ab",
          "job_type": "INSAR_ISCE_BURST",
          "status_code": "FAILED",
          "request_time": "2023-01-16T12:00:00Z",
          "failure_reason": "ISCE2 coregistration failed due to severe temporal decorrelation.",
          "logs": "Traceback: RuntimeError in isce.core.coherence"
        }
        """;

        TrackedInSarJob job = adapter.parseHyp3JobResponse(json, null);

        assertNotNull(job);
        assertEquals(JobStatus.FAILED, job.status());
        assertTrue(job.failureReason().contains("ISCE2 coregistration failed"));
        assertTrue(job.logs().contains("RuntimeError"));
    }

    @Test
    @DisplayName("Deve lançar EntityNotFoundException quando job não existe no registro local nem na rede")
    void shouldThrowEntityNotFoundExceptionForUnknownJob() {
        assertThrows(EntityNotFoundException.class, () -> adapter.getJobStatus("non-existent-job-uuid"));
    }

    @Test
    @DisplayName("Deve identificar presença de credenciais configuradas")
    void shouldIdentifyCredentialsConfigured() {
        assertTrue(adapter.hasCredentialsConfigured());

        Hyp3JobTrackingAdapter unconfigured = new Hyp3JobTrackingAdapter(
                "https://hyp3-api.asf.alaska.edu",
                "",
                "",
                "",
                objectMapper
        );
        assertFalse(unconfigured.hasCredentialsConfigured());
    }
}
