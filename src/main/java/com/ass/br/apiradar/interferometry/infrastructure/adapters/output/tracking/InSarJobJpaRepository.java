package com.ass.br.apiradar.interferometry.infrastructure.adapters.output.tracking;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/** Consulta persistente por ID do job e chave de idempotência. */
public interface InSarJobJpaRepository extends JpaRepository<InSarJobJpaEntity, String> {
    Optional<InSarJobJpaEntity> findByIdempotencyKey(String idempotencyKey);
}
