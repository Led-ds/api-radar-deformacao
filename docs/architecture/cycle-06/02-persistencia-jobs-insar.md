# Radar++ — Ciclo 6.2: Persistência durável de jobs InSAR

## Objetivo
O registro de jobs HyP3 antes armazenado exclusivamente em `ConcurrentHashMap` passa a ter fonte de verdade PostgreSQL, preservando o contrato `InSarJobTrackingPort` do domínio.

## Componentes
- `V3__init_insar_job_registry.sql`: tabela `radar.insar_jobs` com chave de job, índice único de idempotência, vínculo de AOI/par, estado e representação serializada completa.
- `InSarJobJpaEntity` / `InSarJobJpaRepository`: persistência técnica Spring Data JPA.
- `PersistentInSarJobTrackingAdapter`: implementação `@Primary` da porta, recupera a última situação persistida antes de consultar o HyP3. Em indisponibilidade remota, preserva o último estado conhecido.
- `Hyp3JobTrackingAdapter`: permanece como cliente remoto existente, sem alterações no bloqueio de execução real.

## Decisões
1. Não persistir credenciais Earthdata no registro.
2. Preservar idempotência por índice único no banco. A constraint também protege contra concorrência entre instâncias.
3. Não inventar associação de AOI/par para jobs consultados somente no HyP3 sem contexto local.
4. Produto em `target/insar-products` **ainda não é persistência durável**: configurar volume persistente e política de quotas é trabalho pendente.
5. A prontidão `checkOperationalReadiness` ainda contém valores do piloto Maceió, que devem ser generalizados antes do uso global.

## Pendências antes de Go Live
- Rodar `./mvnw test` e teste de migração em PostgreSQL/PostGIS real ou Testcontainers.
- Validar reinício de contêiner com volume persistente e integridade do registro.
- Confirmar semântica real dos erros HyP3 (401/403, 404, timeout, 5xx); o adaptador remoto ainda utiliza fallback.
- Implementar segurança de autorização por organização para consultas de jobs (o registro persistido por AOI não é controle de acesso).
- Persistir os produtos fora do filesystem efêmero e introduzir limites contra ZIP bombs.
- Normalizar documentação e distinguir rigorosamente dados de teste de produtos científicos reais.

## Governança
`radar.processing.hyp3.execution-enabled=false` permanece como política obrigatória; nenhuma submissão foi implementada neste incremento.
