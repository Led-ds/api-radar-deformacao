package com.ass.br.apiradar.interferometry.infrastructure.adapters.output.tracking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Registro técnico persistido; o domínio permanece independente de JPA. */
@Entity
@Table(name = "insar_jobs", schema = "radar")
public class InSarJobJpaEntity {
    @Id
    @Column(name = "job_id", length = 160, nullable = false)
    private String jobId;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "pair_id", columnDefinition = "text", nullable = false)
    private String pairId;

    @Column(name = "aoi_id", length = 128, nullable = false)
    private String aoiId;

    @Column(name = "job_type", length = 80, nullable = false)
    private String jobType;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    @Column(name = "job_payload", columnDefinition = "text", nullable = false)
    private String jobPayload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InSarJobJpaEntity() {}

    public InSarJobJpaEntity(String jobId, String idempotencyKey, String pairId, String aoiId,
                             String jobType, String status, String jobPayload, Instant createdAt, Instant updatedAt) {
        this.jobId = jobId;
        this.idempotencyKey = idempotencyKey;
        this.pairId = pairId;
        this.aoiId = aoiId;
        this.jobType = jobType;
        this.status = status;
        this.jobPayload = jobPayload;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getJobId() { return jobId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getPairId() { return pairId; }
    public String getAoiId() { return aoiId; }
    public String getJobType() { return jobType; }
    public String getStatus() { return status; }
    public String getJobPayload() { return jobPayload; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
