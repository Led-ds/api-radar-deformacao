package com.ass.br.apiradar.interferometry.infrastructure;

import com.ass.br.apiradar.interferometry.domain.model.job.JobStatus;
import com.ass.br.apiradar.interferometry.domain.model.job.TrackedInSarJob;
import com.ass.br.apiradar.interferometry.infrastructure.adapters.output.tracking.Hyp3JobTrackingAdapter;
import com.ass.br.apiradar.interferometry.infrastructure.adapters.output.tracking.InSarJobJpaEntity;
import com.ass.br.apiradar.interferometry.infrastructure.adapters.output.tracking.InSarJobJpaRepository;
import com.ass.br.apiradar.interferometry.infrastructure.adapters.output.tracking.PersistentInSarJobTrackingAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PersistentInSarJobTrackingAdapterTest {

    private InSarJobJpaRepository repository;
    private Hyp3JobTrackingAdapter hyp3;
    private PersistentInSarJobTrackingAdapter adapter;

    @BeforeEach
    void setUp() {
        repository = mock(InSarJobJpaRepository.class);
        hyp3 = mock(Hyp3JobTrackingAdapter.class);
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        adapter = new PersistentInSarJobTrackingAdapter(repository, hyp3, mapper);
    }

    private TrackedInSarJob job(String id, String key) {
        return new TrackedInSarJob(id, key, "PAIR-1", "AOI-1",
                "INSAR_ISCE_BURST", JobStatus.PENDING,
                Instant.parse("2026-10-30T12:00:00Z"), null,
                List.of(), null, null, false, null);
    }

    @Test
    void storesJobMetadataWithoutRemoteSubmission() {
        TrackedInSarJob job = job("job-01", "key-01");
        when(repository.findById(job.jobId())).thenReturn(Optional.empty());
        when(repository.findByIdempotencyKey("key-01")).thenReturn(Optional.empty());

        assertEquals(job, adapter.registerOrUpdateJob(job));
        verify(repository).saveAndFlush(argThat(entity ->
                entity.getJobId().equals("job-01")
                        && entity.getIdempotencyKey().equals("key-01")
                        && entity.getJobPayload().contains("PAIR-1")));
        verifyNoInteractions(hyp3);
    }

    @Test
    void rejectsIdempotencyKeyOwnedByAnotherJob() {
        InSarJobJpaEntity existing = new InSarJobJpaEntity(
                "job-existing", "key-01", "PAIR-1", "AOI-1",
                "INSAR_ISCE_BURST", "PENDING", "{}", Instant.now(), Instant.now());
        when(repository.findByIdempotencyKey("key-01")).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class,
                () -> adapter.registerOrUpdateJob(job("job-new", "key-01")));
        verify(repository, never()).saveAndFlush(any());
        verifyNoInteractions(hyp3);
    }

    @Test
    void retrievesPersistedJobAfterNewAdapterInstance() throws Exception {
        TrackedInSarJob original = job("job-02", "key-02")
                .withStatus(JobStatus.SUCCEEDED, null, null)
                .withDownloaded("/durable/insar/job-02");
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        InSarJobJpaEntity persisted = new InSarJobJpaEntity(
                original.jobId(), original.idempotencyKey(), original.pairId(), original.aoiId(),
                original.jobType(), original.status().name(),
                mapper.writeValueAsString(original), Instant.now(), Instant.now());
        when(repository.findById(original.jobId())).thenReturn(Optional.of(persisted));
        when(repository.findByIdempotencyKey(original.idempotencyKey())).thenReturn(Optional.of(persisted));

        // A new adapter instance has no in-memory job registry.
        PersistentInSarJobTrackingAdapter restarted =
                new PersistentInSarJobTrackingAdapter(repository, hyp3, mapper);

        assertEquals(original.jobId(), restarted.getJobStatus(original.jobId()).jobId());
        assertEquals(JobStatus.SUCCEEDED, restarted.findByIdempotencyKey("key-02").orElseThrow().status());
        verifyNoInteractions(hyp3);
    }

    @Test
    void retainsLastPersistedStateIfRemotePollingFails() throws Exception {
        TrackedInSarJob original = job("job-03", "key-03");
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        InSarJobJpaEntity persisted = new InSarJobJpaEntity(
                original.jobId(), original.idempotencyKey(), original.pairId(), original.aoiId(),
                original.jobType(), original.status().name(),
                mapper.writeValueAsString(original), Instant.now(), Instant.now());
        when(repository.findById("job-03")).thenReturn(Optional.of(persisted));
        when(hyp3.getJobStatus("job-03")).thenThrow(new IllegalStateException("HyP3 offline"));

        TrackedInSarJob result = adapter.getJobStatus("job-03");

        assertEquals(JobStatus.PENDING, result.status());
        verify(hyp3).registerOrUpdateJob(original);
        verify(hyp3).getJobStatus("job-03");
    }
}
