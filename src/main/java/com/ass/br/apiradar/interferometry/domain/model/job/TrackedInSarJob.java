package com.ass.br.apiradar.interferometry.domain.model.job;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Entidade de domínio para rastreamento e auditoria do ciclo de vida de um job InSAR.
 */
public record TrackedInSarJob(
        String jobId,
        String idempotencyKey,
        String pairId,
        String aoiId,
        String jobType,
        JobStatus status,
        Instant requestTime,
        Instant expirationTime,
        List<InSarProductFile> files,
        String failureReason,
        String logs,
        boolean downloaded,
        String localProductPath
) implements Serializable {

    public TrackedInSarJob {
        files = files == null ? Collections.emptyList() : List.copyOf(files);
    }

    public TrackedInSarJob withStatus(JobStatus newStatus, String failureReason, String logs) {
        return new TrackedInSarJob(
                jobId, idempotencyKey, pairId, aoiId, jobType, newStatus,
                requestTime, expirationTime, files, failureReason, logs,
                downloaded, localProductPath
        );
    }

    public TrackedInSarJob withFiles(List<InSarProductFile> newFiles) {
        return new TrackedInSarJob(
                jobId, idempotencyKey, pairId, aoiId, jobType, status,
                requestTime, expirationTime, newFiles, failureReason, logs,
                downloaded, localProductPath
        );
    }

    public TrackedInSarJob withDownloaded(String localPath) {
        return new TrackedInSarJob(
                jobId, idempotencyKey, pairId, aoiId, jobType, status,
                requestTime, expirationTime, files, failureReason, logs,
                true, localPath
        );
    }
}
