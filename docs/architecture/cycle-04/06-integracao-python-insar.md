# Radar++ — Ciclo 4: Arquitetura Técnica, UML e Contratos
## Documento 06 — Integração Python, Pipeline Científico e Mensageria Assíncrona

**Documento:** `06-integracao-python-insar.md`  
**Ciclo:** Ciclo 4 — Arquitetura Técnica, UML e Contratos  
**Data:** 24 de Outubro de 2026  
**Status:** Oficial — Entregue para Homologação de Arquitetura  
**Responsáveis:** Engenheiro Python InSAR, Arquiteto de Sistemas Distribuídos, Arquiteto de Software Sênior  
**Repositório Oficial:** `api-radar-deformacao` (caminho: `docs/architecture/cycle-04/06-integracao-python-insar.md`)  
**Regra de Governança Estrita:** Especificação conceitual de protocolos, payloads e contratos. **Não disparar chamadas a APIs externas da NASA/HyP3, não executar scripts de inversão e não consumir créditos nesta etapa.**

---

### 1. Visão Geral do Pipeline Científico InSAR

O processamento geodésico de interferometria por radar requer ferramentas científicas consolidadas na comunidade internacional (ASF HyP3, ISCE2, SNAP, MintPy, GDAL). Para garantir estabilidade e tipagem corporativa no backend Java sem abdicar do ecossistema científico, o Radar++ adota um **Worker Científico em Python 3.11 desacoplado**, cuja comunicação ocorre por meio de tarefas assíncronas serializadas em JSON.

```mermaid
flowchart TD
    subgraph "Backend Java / Spring Boot"
        REQ["Recebe Ordem de Processamento"] --> VAL["Valida Parâmetros e Cria ProcessingJob"]
        VAL --> ENQ["Grava Tarefa na Fila de Processamento"]
        RES["Consome Resultado e Executa Gating de Qualidade"] <-- NOTIF["Recebe Notificação de Término"]
    end

    subgraph "Worker Científico Python (Containerizado)"
        DEQ["1. Consome Tarefa da Fila (Polling com SKIP LOCKED)"] --> ASF_QUERY["2. Valida Bursts e Cenas no Catálogo ASF Search"]
        ASF_QUERY --> PAIR_SEL["3. Forma Rede Interferométrica SBAS (B_perp e delta_t)"]
        PAIR_SEL --> HYP3_SUB["4. Submete Jobs INSAR_ISCE_BURST via HyP3 SDK"]
        HYP3_SUB --> HYP3_POLL["5. Polling Assíncrono de Status no HyP3 Cluster"]
        HYP3_POLL --> S3_PULL["6. Download de Interferogramas e Coerência (GeoTIFF)"]
        S3_PULL --> MINTPY["7. Executa Inversão de Séries Temporais (MintPy SBAS)"]
        MINTPY --> QA_METRICS["8. Extrai Coerência Média e Resíduos de Desdobramento"]
        QA_METRICS --> COG_GEN["9. Converte Saídas em Cloud Optimized GeoTIFF (COG)"]
        COG_GEN --> S3_PUSH["10. Upload dos Produtos Finais para Bucket AWS S3 com SHA-256"]
        S3_PUSH --> NOTIF
    end
```

---

### 2. Avaliação Comparativa de Alternativas de Mensageria Assíncrona

Foram analisadas três alternativas para a fila de comunicação entre o Monólito Java e o Worker Python:

```
+----------------------------------------------------------------------------------------------------------------------------------------------------+
|                                    MATRIZ COMPARATIVA DE MECANISMOS DE FILA ASSÍNCRONA                                                             |
+--------------------+---------------------------------------+---------------------------------------+-----------------------------------------------+
| Critério Técnico   | Alternativa A: Fila em PostgreSQL     | Alternativa B: Broker RabbitMQ        | Alternativa C: Serviço AWS SQS/SNS            |
|                    | (`SELECT ... FOR UPDATE SKIP LOCKED`) | (Contêiner Dedicado AMQP)             | (Fila Gerenciada em Nuvem)                    |
+--------------------+---------------------------------------+---------------------------------------+-----------------------------------------------+
| **Complexidade**   | **Mínima:** Reutiliza o PostgreSQL já | **Média:** Exige novo contêiner,      | **Média-Alta:** Depende de credenciais AWS,   |
| **Operacional**    | existente; zero infraestrutura nova.  | monitoramento, clustering e gestão.   | ambiente de desenvolvimento mockado (LocalStack)|
+--------------------+---------------------------------------+---------------------------------------+-----------------------------------------------+
| **Custo de Infra** | **Zero adicional:** Não consome novas | Custo de memória RAM adicional (~300MB| Custo por milhão de requisições e latência de |
|                    | instâncias de nuvem.                  | contínuos) no host Docker.            | rede externa em desenvolvimento local.        |
+--------------------+---------------------------------------+---------------------------------------+-----------------------------------------------+
| **Atomicidade**    | **Perfeita:** A criação do job e o    | Risco de *dual-write*: exige padrão   | Exige Transactional Outbox para evitar perda  |
| **Transacional**   | enfileiramento são no mesmo commit.   | Outbox com polling no banco.          | de mensagens se a AWS oscilar.                |
+--------------------+---------------------------------------+---------------------------------------+-----------------------------------------------+
| **Throughput**     | Até 1.500 tarefas/segundo (mais que   | Mais de 30.000 mensagens/segundo      | Escalabilidade praticamente ilimitada.        |
|                    | suficiente para dezenas de jobs/dia). | (desproporcional ao volume do MVP).   |                                               |
+--------------------+---------------------------------------+---------------------------------------+-----------------------------------------------+
| **Confiabilidade** | **Excelente:** ACID garantido; se o   | ACK/NACK nativo; recuperação rápida   | Excelente; alta durabilidade distribuída.     |
| **e Retentativas** | worker cair, a transação aborta/volta.| com Dead Letter Queues (DLQ).         |                                               |
+--------------------+---------------------------------------+---------------------------------------+-----------------------------------------------+
| **Parecer Técnico**| **RECOMENDADA PARA O MVP (ADR-003):** | **Rejeitada para o MVP:**             | **Opção Futura (Ciclo 5+ Nuvem):**            |
|                    | Proporcional, simples e robusta.      | Complexidade desnecessária nesta fase.| Elegível quando o volume justificar.          |
+--------------------+---------------------------------------+---------------------------------------+-----------------------------------------------+
```

**Decisão Recomendada para o MVP (ADR-003):**  
Adotar a **Alternativa A (Fila em PostgreSQL via `FOR UPDATE SKIP LOCKED`)** operando sobre a tabela `radar.processing_jobs`. O worker Python faz polling a cada 5 segundos buscando jobs com `state = 'JOB_QUEUED'`. O bloqueio evita que múltiplos workers concorrentes peguem o mesmo job.

---

### 3. Contratos Conceituais de Integração (Schemas JSON)

---

#### 3.1. Contrato de Despacho de Tarefa (Backend Java $\rightarrow$ Worker Python)
Quando o job é despachado, os parâmetros geodésicos estruturados são lidos pelo worker:

```json
{
  "taskType": "INSAR_TIME_SERIES_PROCESSING",
  "jobId": "a1b2c3d4-e5f6-4a5b-8c9d-0123456789ab",
  "requestId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "organizationId": "d3b07384-d113-40a2-9721-a53b2169b821",
  "aoi": {
    "aoiId": "b8f3e21a-6411-451e-b81b-9e2c4f10a8c2",
    "wktGeometry": "POLYGON((-35.795 -9.680, -35.710 -9.680, -35.710 -9.600, -35.795 -9.600, -35.795 -9.680))",
    "bbox": { "minLon": -35.795, "minLat": -9.680, "maxLon": -35.710, "maxLat": -9.600 }
  },
  "parameters": {
    "dataset": "SENTINEL-1",
    "orbitPath": 142,
    "orbitDirection": "DESCENDING",
    "polarization": "VV",
    "dateRange": { "start": "2023-01-01", "end": "2023-12-31" },
    "networkThresholds": {
      "maxPerpendicularBaselineMeters": 150.0,
      "maxTemporalBaselineDays": 36
    },
    "processingProfile": "CONSERVATIVE_URBAN",
    "multilookResolutionMeters": 20.0
  },
  "storageDestination": {
    "s3Bucket": "radar-insar-artifacts",
    "basePrefix": "organizations/d3b07384/aois/b8f3e21a/analyses/job_a1b2c3d4/v1/"
  }
}
```

---

#### 3.2. Contrato de Notificação de Conclusão Técnica (Worker Python $\rightarrow$ Backend Java)
Ao finalizar a inversão temporal e a conversão para COG, o worker atualiza a linha do job e insere o payload de conclusão:

```json
{
  "jobId": "a1b2c3d4-e5f6-4a5b-8c9d-0123456789ab",
  "executionEngine": "HYP3_MINTPY_SBAS",
  "executionDurationSeconds": 2140,
  "externalReferences": {
    "hyp3JobBatchId": "batch_98124_maceio",
    "hyp3JobCount": 72
  },
  "qualityMetrics": {
    "meanSpatialCoherence": 0.528,
    "phaseUnwrappingResiduePercentage": 9.42,
    "coherenceThresholdApplied": 0.40,
    "totalPixelsEvaluated": 184520,
    "validCoherentPixelsPercentage": 78.6
  },
  "generatedProducts": [
    {
      "productType": "ANNUAL_VELOCITY_MAP",
      "productFormat": "COG",
      "primaryUnit": "MM_PER_YEAR_LOS",
      "s3StorageKey": "organizations/d3b07384/aois/b8f3e21a/analyses/job_a1b2c3d4/v1/velocity_los_cog.tif",
      "checksumSha256": "7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069",
      "fileSizeBytes": 4518204,
      "spatialResolutionMeters": 20.0
    },
    {
      "productType": "TEMPORAL_COHERENCE_MAP",
      "productFormat": "COG",
      "primaryUnit": "COHERENCE_INDEX",
      "s3StorageKey": "organizations/d3b07384/aois/b8f3e21a/analyses/job_a1b2c3d4/v1/coherence_spatial_cog.tif",
      "checksumSha256": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      "fileSizeBytes": 2210450,
      "spatialResolutionMeters": 20.0
    },
    {
      "productType": "DISPLACEMENT_TIME_SERIES",
      "productFormat": "NETCDF",
      "primaryUnit": "MM_LOS",
      "s3StorageKey": "organizations/d3b07384/aois/b8f3e21a/analyses/job_a1b2c3d4/v1/timeseries_mintpy.nc",
      "checksumSha256": "bc54a819b12d5e89a32c4b5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f",
      "fileSizeBytes": 48920150,
      "spatialResolutionMeters": 20.0
    }
  ],
  "provenanceMetadata": {
    "software": "MintPy v1.5.3 / GDAL 3.8.4",
    "demSource": "Copernicus 30m GLO-30",
    "multilookFactor": "5x1",
    "unwrapMethod": "SNAPHU",
    "referencePoint": { "lat": -9.6254, "lon": -35.7312 }
  }
}
```

---

### 4. Gestão de Falhas, Idempotência e Retentativas

1. **Idempotência no Worker Python:** Antes de submeter novos jobs ao cluster HyP3, o worker calcula o hash MD5 da tupla `(Granule1, Granule2, ProcessingParameters)`. Se uma submissão prévia idêntica já tiver sido registrada ou concluída, o worker reaproveita o artefato sem reprocessar nem cobrar novos créditos.
2. **Tratamento de Falhas Transitórias de Rede:** Chamadas HTTP para o ASF Search ou API HyP3 implementam retry com *Exponential Backoff* (tentativas aos 10s, 30s e 90s) para lidar com eventuais limites de taxa (*HTTP 429*) ou indisponibilidade temporária (*HTTP 503*).
3. **Falhas Fatais e Cancelamento:** Se ocorrer corrupção irrecuperável de arquivos SLC ou divergência matemática insolúvel no desdobramento de fase, o worker grava a causa raiz na coluna `failure_cause` da tabela `processing_jobs` e encerra a transação com `state = 'EXECUTION_FAILED'`, liberando travas e notificando a equipe de sustentação.
