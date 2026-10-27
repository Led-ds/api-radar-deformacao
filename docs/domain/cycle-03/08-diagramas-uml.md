# Radar++ — Domínio: 08-diagramas-uml.md
## Suíte de Diagramas Conceituais de Domínio: Arquitetura, Agregados, Estados e Sequências

**Documento:** `08-diagramas-uml.md`  
**Ciclo:** Ciclo 3 — Modelagem Estratégica e Tática com DDD  
**Data:** 24 de Outubro de 2026  
**Status:** Oficial — Entregue para Homologação de Arquitetura de Domínio  
**Responsável:** Arquiteto de Software, Especialista em DDD, Modelador de Domínios Geoespaciais  
**Repositório Oficial:** `api-radar-deformacao` (caminho: `docs/domain/cycle-03/08-diagramas-uml.md`)  
**Aviso Conceitual Obrigatório:** Todos os diagramas contidos neste documento representam **modelos conceituais de domínio e negócios**. Eles não representam tabelas relacionais de banco de dados, entidades JPA com chaves estrangeiras físicas nem controladores REST de implementação.

---

## 1. Diagrama 1: Context Map v2 (Fronteiras e Padrões de Integração)

```mermaid
graph TD
    subgraph "Radar++ — Mapa de Contextos Estratégico (Context Map v2)"
        subgraph "Subdomínio de Suporte"
            TERR["Contexto Gestão Territorial<br/>(Territory & AOI Context)<br/>• Agregado: AreaOfInterest"]
        end

        subgraph "Subdomínio de Integração"
            CAT["Contexto Catálogo SAR<br/>(SAR Catalog Context)<br/>• Agregado: ConsultaCatalogo"]
        end

        subgraph "Domínio Principal Unificado (Core Domain)"
            CORE["Contexto Motor InSAR Core<br/>(InSAR Processing & Products)<br/>• Agregados: ProcessingRequest,<br/>ProcessingJob, InSarAnalysis"]
        end

        subgraph "Aplicação Especializada"
            URBAN["Contexto Análise Urbana<br/>(Urban Subsidence Analysis)<br/>• Agregado: UrbanInvestigation"]
        end
    end

    EXT["APIs Externas<br/>(NASA ASF / ESA CDSE)"] -->|Anti-Corruption Layer| CAT
    TERR -->|Published Language (Geometria WKT)| CAT
    TERR -->|Customer-Supplier (AoiId)| CORE
    CAT -->|Published Language (Lista de Cenas)| CORE
    CORE -->|Open Host Service (Rasters COG / Séries)| URBAN
```

---

## 2. Diagrama 2: Modelo Conceitual Estrutural do Domínio

```mermaid
classDiagram
    direction TB
    
    class AreaOfInterest {
        AoiId id
        OrganizationId organizationId
        String name
        PolygonGeometry geometry
        BoundingBox boundingBox
        MetricArea areaKm2
        AoiStatus status
    }

    class ProcessingRequest {
        RequestId id
        AoiId aoiId
        UserId requestedBy
        DateRange temporalWindow
        OrbitPath targetOrbitPath
        BaselineThresholds baselineLimits
        RequestStatus status
    }

    class ProcessingJob {
        JobId id
        RequestId requestId
        ExternalJobReference externalJobRef
        JobExecutionState state
        RetryCounter retries
        ExecutionTimeline timeline
    }

    class InSarAnalysis {
        AnalysisId id
        JobId jobId
        AoiId aoiId
        OrbitPath orbitPath
        AnalysisStatus status
        AlgorithmProvenance provenance
        QualityAssessment qualityAssessment
        List~ScientificProductRef~ productReferences
        evaluateQuality(QualityMetrics metrics)
        approveForVisualization()
        qualifyWithRestrictions(String reason)
        rejectQuality(String reason)
    }

    class ScientificProduct {
        ProductId id
        ProductType type
        ProductFormat format
        StorageArtifactUri storageLocation
        ChecksumSha256 checksum
        SpatialExtent footprint
    }

    class QualityAssessment {
        CoherenceThreshold meanCoherence
        Percentage unwrappingResidues
        QualityFlag flag
        DateTime evaluatedAt
    }

    class UrbanInvestigation {
        InvestigationId id
        AnalysisId analysisId
        InvestigationTitle title
        SubsidenceBowlGeometry bowlBoundary
        List~SectionProfile~ profiles
        List~TechnicalObservationRecord~ observations
        generateSectionProfile()
        delineateSubsidenceBowl()
        recordTechnicalObservation()
    }

    AreaOfInterest "1" ..> "*" ProcessingRequest : âncora espacial
    ProcessingRequest "1" --> "1" ProcessingJob : dispara
    ProcessingJob "1" --> "1" InSarAnalysis : produz
    InSarAnalysis "1" *-- "*" ScientificProduct : referencia (ponteiros leves S3)
    InSarAnalysis "1" *-- "1" QualityAssessment : atesta
    InSarAnalysis "1" <.. "*" UrbanInvestigation : analisa (d_LOS primário)
```

---

## 3. Diagrama 3: Estados do Job de Processamento (*ProcessingJob*)

```mermaid
stateDiagram-v2
    [*] --> JOB_QUEUED: CreateJobCommand

    JOB_QUEUED --> PROCESSING_RUNNING: StartJobCommand
    JOB_QUEUED --> EXECUTION_CANCELLED: CancelJobCommand

    state PROCESSING_RUNNING {
        [*] --> CorregistrandoSLC
        CorregistrandoSLC --> GerandoInterferogramas
        GerandoInterferogramas --> DesdobrandoFaseSNAPHU
        DesdobrandoFaseSNAPHU --> InversaoTemporalMintPy
        InversaoTemporalMintPy --> FormatandoCOGs
        FormatandoCOGs --> [*]
    }

    PROCESSING_RUNNING --> PROCESSING_RUNNING: Falha Transitória (Retry < 3)
    PROCESSING_RUNNING --> EXECUTION_FAILED: Falha Terminal ou Retry >= 3
    PROCESSING_RUNNING --> EXECUTION_CANCELLED: CancelJobCommand
    PROCESSING_RUNNING --> PROCESSING_COMPLETED: Todos os passos concluídos com sucesso

    PROCESSING_COMPLETED --> [*]
    EXECUTION_FAILED --> [*]
    EXECUTION_CANCELLED --> [*]
```

---

## 4. Diagrama 4: Estados da Análise Científica (*InSarAnalysis*)

```mermaid
stateDiagram-v2
    [*] --> SCIENTIFIC_EVALUATION_PENDING: JobSucceededEvent recebido

    state SCIENTIFIC_EVALUATION_PENDING {
        [*] --> InspecionandoCoerenciaMedia
        InspecionandoCoerenciaMedia --> CalculandoResiduosUnwrapping
        CalculandoResiduosUnwrapping --> DecidindoGating
        DecidindoGating --> [*]
    }

    SCIENTIFIC_EVALUATION_PENDING --> APPROVED_FOR_VISUALIZATION: Resíduos <= 15% e Coerência >= Limiar
    SCIENTIFIC_EVALUATION_PENDING --> QUALIFIED_WITH_RESTRICTIONS: Resíduos > 15% ou Baixa Coerência Tolerável
    SCIENTIFIC_EVALUATION_PENDING --> REJECTED_QUALITY: Decorrelação Severa ou Ruptura de Fase

    APPROVED_FOR_VISUALIZATION --> [*]: Disponível para Séries e Mapas
    QUALIFIED_WITH_RESTRICTIONS --> [*]: Disponível com Tarja de Advertência Técnica
    REJECTED_QUALITY --> [*]: Bloqueado para Visualização (Arquivado)
```

---

## 5. Diagrama 5: Sequência de Solicitação, Autorização e Execução

```mermaid
sequenceDiagram
    autonumber
    actor User as Engenheiro Geotécnico
    participant App as Interface Web / API
    participant Req as Agregado: ProcessingRequest
    participant Job as Agregado: ProcessingJob
    participant Ext as Motor Nuvem (HyP3 / MintPy)

    User->>App: 1. Seleciona AOI e submete janela temporal
    App->>Req: 2. SubmitProcessingRequestCommand
    Req->>Req: Valida janela temporal e compatibilidade
    Req->>Req: Transiciona para AUTHORIZED
    Req-->>App: Emite ProcessingRequestAuthorizedEvent

    App->>Job: 3. CreateJobCommand
    Job->>Job: Instancia Job no estado JOB_QUEUED
    Job->>Ext: 4. Despacha ordem de processamento assíncrono
    Ext-->>Job: Retorna ExternalJobReference
    Job->>Job: Transiciona para PROCESSING_RUNNING
    Job-->>App: Retorna JobId (HTTP 202 Accepted)
    App-->>User: Exibe painel de acompanhamento de progresso
```

---

## 6. Diagrama 6: Sequência de Avaliação e Disponibilização de Resultados

```mermaid
sequenceDiagram
    autonumber
    participant Ext as Motor Nuvem (HyP3 / MintPy)
    participant Job as Agregado: ProcessingJob
    participant Ana as Agregado: InSarAnalysis
    participant S3 as Storage S3 (Bucket de COGs)
    participant Urban as Contexto Análise Urbana
    actor User as Engenheiro Geotécnico

    Ext-->>Job: 1. Notifica conclusão da inversão e rasters
    Job->>Job: Transiciona para PROCESSING_COMPLETED
    Job-->>Ana: 2. Emite ProcessingJobSucceededEvent

    Ana->>Ana: Instancia Análise (SCIENTIFIC_EVALUATION_PENDING)
    Ana->>Ana: Executa EvaluateQuality()
    
    alt Resíduos <= 15% e Coerência OK
        Ana->>Ana: Transiciona para APPROVED_FOR_VISUALIZATION
        Ana-->>Urban: Emite InSarAnalysisApprovedEvent
    else Resíduos > 15% ou Baixa Coerência
        Ana->>Ana: Transiciona para QUALIFIED_WITH_RESTRICTIONS
        Ana-->>Urban: Emite InSarAnalysisRestrictedEvent (com alerta)
    end

    User->>Urban: 3. Acessa estudo e solicita série temporal de um ponto
    Urban->>S3: 4. Executa HTTP Range Request no raster COG
    S3-->>Urban: Devolve fatia de bytes de deslocamento
    Urban-->>User: 5. Renderiza curva histórica de subsidência (d_LOS em mm)
```

---

## 7. Diagrama 7: Fluxo de Tratamento de Falhas, Retentativas e Reprocessamento

```mermaid
flowchart TD
    A["Job em Execução (PROCESSING_RUNNING)"] --> B{"Ocorreu Erro na Execução?"}
    B -- "Não" --> C["Avança para PROCESSING_COMPLETED"]
    B -- "Sim" --> D{"Tipo de Erro Identificado"}
    
    D -- "Erro de Domínio (Ex.: DEM incompatível ou dados corrompidos)" --> E["Falha Imediata: Sem Retentativa"]
    E --> F["Transiciona para EXECUTION_FAILED"]
    F --> G["Emite ProcessingJobFailedEvent com Causa Raiz"]

    D -- "Erro Transitório de Infra (Ex.: Timeout de rede ou rate-limit)" --> H{"retryCount < 3?"}
    H -- "Sim" --> I["Incrementa retryCount"]
    I --> J["Aplica Exponential Backoff (30s, 60s, 120s)"]
    J --> A

    H -- "Não" --> K["Esgotadas as Retentativas"]
    K --> F
```
