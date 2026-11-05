package com.ass.br.apiradar.interferometry.infrastructure.adapters.output.tracking;

import com.ass.br.apiradar.interferometry.domain.model.job.TrackedInSarJob;
import com.ass.br.apiradar.interferometry.domain.ports.output.InSarJobTrackingPort;
import com.ass.br.apiradar.shared.domain.exceptions.EntityNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Registro durável de jobs. Mantém a porta do domínio e delega ao adaptador HyP3
 * apenas a consulta remota; o banco sobrevive a reinício do processo/container.
 *
 * A consulta remota ocorre fora de transação de banco de dados.
 */
@Component
@Primary
public class PersistentInSarJobTrackingAdapter implements InSarJobTrackingPort {

    private final InSarJobJpaRepository repository;
    private final Hyp3JobTrackingAdapter hyp3;
    private final ObjectMapper mapper;

    public PersistentInSarJobTrackingAdapter(InSarJobJpaRepository repository,
                                              Hyp3JobTrackingAdapter hyp3,
                                              ObjectMapper mapper) {
        this.repository = repository;
        this.hyp3 = hyp3;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TrackedInSarJob> findByIdempotencyKey(String key) {
        if (key == null || key.isBlank()) return Optional.empty();
        return repository.findByIdempotencyKey(key).map(this::decode);
    }

    @Override
    @Transactional
    public TrackedInSarJob registerOrUpdateJob(TrackedInSarJob job) {
        if (job == null || job.jobId() == null || job.jobId().isBlank()) {
            throw new IllegalArgumentException("JobId obrigatório.");
        }
        if (job.pairId() == null || job.pairId().isBlank()
                || job.aoiId() == null || job.aoiId().isBlank()
                || job.jobType() == null || job.status() == null) {
            throw new IllegalArgumentException("Job sem vínculo de AOI, par, tipo ou status.");
        }
        String key = job.idempotencyKey() == null || job.idempotencyKey().isBlank()
                ? null : job.idempotencyKey();
        if (key != null) {
            repository.findByIdempotencyKey(key).ifPresent(existing -> {
                if (!existing.getJobId().equals(job.jobId())) {
                    throw new IllegalStateException("Chave de idempotência já vinculada a outro job.");
                }
            });
        }

        Instant now = Instant.now();
        Instant createdAt = repository.findById(job.jobId())
                .map(InSarJobJpaEntity::getCreatedAt).orElse(now);

        InSarJobJpaEntity entity = new InSarJobJpaEntity(
                job.jobId(), key, job.pairId(), job.aoiId(), job.jobType(),
                job.status().name(), encode(job), createdAt, now
        );
        // saveAndFlush antecipa a verificação da constraint UNIQUE no banco.
        repository.saveAndFlush(entity);
        return job;
    }

    @Override
    public TrackedInSarJob getJobStatus(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            throw new IllegalArgumentException("JobId obrigatório.");
        }

        TrackedInSarJob cached = repository.findById(jobId).map(this::decode).orElse(null);
        if (cached != null) {
            // Produto final já recuperado: registro persistente é suficiente,
            // sem sincronização desnecessária com o adaptador remoto.
            if (cached.status().isSuccess() && cached.downloaded()) return cached;
            // Para jobs não finalizados, fornece AOI/par/idempotência ao parser remoto.
            hyp3.registerOrUpdateJob(cached);
        }

        // Sem job local e sem contexto: preservar o comportamento de consulta existente.
        // Nunca inventar vínculo com AOI/par para persistir resposta remota.
        try {
            TrackedInSarJob remote = hyp3.getJobStatus(jobId);
            if (cached == null && ("UNKNOWN_AOI".equals(remote.aoiId())
                    || "UNKNOWN_PAIR".equals(remote.pairId()))) {
                return remote;
            }
            return registerOrUpdateJob(remote);
        } catch (EntityNotFoundException ex) {
            if (cached != null) return cached;
            throw ex;
        } catch (RuntimeException ex) {
            // Indisponibilidade externa não apaga a última situação persistida.
            if (cached != null) return cached;
            throw ex;
        }
    }

    private String encode(TrackedInSarJob job) {
        try {
            return mapper.writeValueAsString(job);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao serializar registro InSAR.", ex);
        }
    }

    private TrackedInSarJob decode(InSarJobJpaEntity entity) {
        try {
            return mapper.readValue(entity.getJobPayload(), TrackedInSarJob.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Registro InSAR inconsistente para job " + entity.getJobId(), ex);
        }
    }
}
