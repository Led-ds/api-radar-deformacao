package com.ass.br.apiradar.interferometry.domain.ports.output;

import com.ass.br.apiradar.interferometry.domain.model.job.TrackedInSarJob;

import java.util.Optional;

/**
 * Porta de saída para acompanhamento assíncrono de status de processamento e auditoria de jobs InSAR.
 */
public interface InSarJobTrackingPort {

    /**
     * Consulta o status atualizado do job InSAR no cluster HyP3 ou no registro rastreado.
     */
    TrackedInSarJob getJobStatus(String jobId);

    /**
     * Localiza um job previamente registrado por sua chave de idempotência determinística.
     */
    Optional<TrackedInSarJob> findByIdempotencyKey(String idempotencyKey);

    /**
     * Registra ou atualiza o estado local de um job rastreado.
     */
    TrackedInSarJob registerOrUpdateJob(TrackedInSarJob job);
}
