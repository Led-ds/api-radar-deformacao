# Radar++ — Ciclo 4: Arquitetura Técnica, UML e Contratos
## Documento 13 — Suíte Completa de Diagramas UML Técnicos e Comportamentais

**Documento:** `13-diagramas-uml.md`  
**Ciclo:** Ciclo 4 — Arquitetura Técnica, UML e Contratos  
**Data:** 24 de Outubro de 2026  
**Status:** Oficial — Entregue para Homologação de Arquitetura  
**Responsáveis:** Arquiteto de Software Sênior, Especialista em DDD, Modelador UML  
**Repositório Oficial:** `api-radar-deformacao` (caminho: `docs/architecture/cycle-04/13-diagramas-uml.md`)  
**Aviso Conceitual:** Todos os diagramas representam **desenhos técnicos de arquitetura e contratos**, alinhados ao Monólito Modular Java/Spring Boot e aos Workers Python InSAR.

---

### 1. Diagrama de Classes Técnicas de Domínio e Persistência

```mermaid
classDiagram
    direction TB

    class AreaOfInterest {
        +AoiId id
        +OrganizationId organizationId
        +String name
        +PolygonGeometry geometry
        +BoundingBox boundingBox
        +MetricArea areaKm2
        +AoiStatus status
        +validateTopology()
        +updateGeometry(PolygonGeometry newGeom)
        +archive()
    }

    class ProcessingRequest {
        +RequestId id
        +OrganizationId organizationId
        +AoiId aoiId
        +UserId requestedBy
        +AnalysisParameters parameters
        +CreditEstimate creditsEstimated
        +RequestStatus status
        +validateParameters()
        +authorize()
        +reject(String reason)
    }

    class ProcessingJob {
        +JobId id
        +RequestId requestId
        +String externalJobReference
        +JobState state
        +int retryCount
        +startExecution()
        +recordStepProgress(String stepName, int percent)
        +handleTransientFailure(String cause)
        +completeOperationalExecution()
        +cancel()
    }

    class InSarAnalysis {
        +AnalysisId id
        +JobId jobId
        +AoiId aoiId
        +int versionNumber
        +AnalysisStatus status
        +ProvenanceMetadata provenance
        +QualityAssessment qualityAssessment
        +List~ScientificProductRef~ products
        +evaluateQuality(QualityMetrics metrics)
        +approveForVisualization()
        +qualifyWithRestrictions(String notes)
        +rejectQuality(String reason)
    }

    class ScientificProduct {
        +ProductId id
        +AnalysisId analysisId
        +ProductType type
        +ProductFormat format
        +StorageArtifactUri storageLocation
        +ChecksumSha256 checksum
        +SpatialExtent footprint
    }

    class UrbanInvestigation {
        +InvestigationId id
        +AnalysisId analysisId
        +AoiId aoiId
        +String title
        +PolygonGeometry subsidenceBowlBoundary
        +List~SectionProfile~ profiles
        +List~TechnicalObservationRecord~ observations
        +generateSectionProfile(LineString profileLine)
        +delineateSubsidenceBowl(PolygonGeometry boundary)
        +recordTechnicalObservation(String notes, String artNumber)
    }

    AreaOfInterest "1" ..> "*" ProcessingRequest : referencia
    ProcessingRequest "1" --> "1" ProcessingJob : dispara
    ProcessingJob "1" --> "1" InSarAnalysis : produz
    InSarAnalysis "1" *-- "*" ScientificProduct : gerencia referências
    InSarAnalysis "1" <.. "*" UrbanInvestigation : analisa
```

---

### 2. Diagrama de Sequência: Criação e Validação de Área de Interesse (AOI)

```mermaid
sequenceDiagram
    autonumber
    actor User as Analista Geotécnico
    participant Controller as AreaOfInterestRestController
    participant Service as TerritoryApplicationService
    participant Domain as AreaOfInterest (Aggregate)
    participant Repo as PostGisAoiRepository
    participant DB as PostgreSQL / PostGIS

    User->>Controller: POST /api/v1/areas-of-interest (JSON com WKT)
    Controller->>Service: registerAreaOfInterest(RegisterAoiCommand)
    Service->>Domain: AreaOfInterest.create(name, wktString, orgId)
    Domain->>Domain: Valida fechamento e auto-interseção (isSimple)
    Domain->>Domain: Calcula envelope BBOX e área métrica em km²
    Domain->>Domain: Valida teto métrico contra o plano da organização
    Service->>Repo: save(aoi)
    Repo->>DB: INSERT INTO radar.areas_of_interest (geom WGS84, bbox)
    Repo-->>Service: Confirmação de persistência
    Service-->>Controller: Retorna AoiDto com ID gerado
    Controller-->>User: HTTP 201 Created (Location: /api/v1/areas-of-interest/{id})
```

---

### 3. Diagrama de Sequência: Descoberta de Cenas e Avaliação de Pares

```mermaid
sequenceDiagram
    autonumber
    actor User as Analista Geotécnico
    participant Controller as SarCatalogRestController
    participant Service as CatalogApplicationService
    participant AsfAdapter as AsfSearchClientAdapter
    participant AsfApi as NASA ASF Search API

    User->>Controller: POST /api/v1/sar-catalog/search (BBOX, Data Inicial, Data Final)
    Controller->>Service: searchAvailableScenes(SearchScenesQuery)
    Service->>AsfAdapter: queryScenes(bbox, dateRange, VV)
    AsfAdapter->>AsfApi: GET /services/search/param (bbox, dataset=SENTINEL-1, mode=IW)
    AsfApi-->>AsfAdapter: Retorna GeoJSON com granulações SLC
    AsfAdapter->>AsfAdapter: Filtra apenas passagens com mesmo Relative Orbit (Path)
    AsfAdapter-->>Service: Lista estruturada de SarSceneMetadata
    Service->>Service: Avalia formação de pares da rede SBAS (Bperp <= 150m, dt <= 36d)
    Service-->>Controller: Retorna BaselineCompatibilityMatrixDto
    Controller-->>User: HTTP 200 OK (Lista de cenas compatíveis e rede temporal)
```

---

### 4. Diagrama de Sequência: Submissão e Despacho de Job Assíncrono

```mermaid
sequenceDiagram
    autonumber
    actor User as Analista Geotécnico
    participant Controller as ProcessingRequestRestController
    participant Service as ProcessingApplicationService
    participant ReqDomain as ProcessingRequest (Aggregate)
    participant JobDomain as ProcessingJob (Aggregate)
    participant Repo as ProcessingJobRepository
    participant DB as PostgreSQL (Jobs & Outbox)

    User->>Controller: POST /api/v1/processing-requests/{id}/authorize (Idempotency-Key)
    Controller->>Service: authorizeAndDispatch(AuthorizeProcessingCommand)
    Service->>ReqDomain: authorize()
    ReqDomain->>JobDomain: createJob(requestId, orgId, parameters)
    JobDomain->>JobDomain: Transiciona para JOB_QUEUED
    Service->>Repo: save(job)
    Repo->>DB: INSERT INTO radar.processing_jobs (state = 'JOB_QUEUED')
    Repo->>DB: INSERT INTO radar.outbox_events (event = 'JobQueued')
    Service-->>Controller: Retorna JobId gerado
    Controller-->>User: HTTP 202 Accepted (Location: /api/v1/processing-jobs/{jobId})
```

---

### 5. Diagrama de Sequência: Execução Científica pelo Worker Python

```mermaid
sequenceDiagram
    autonumber
    participant Worker as Worker Python InSAR
    participant DB as PostgreSQL
    participant HyP3 as NASA ASF HyP3 Cluster
    participant S3 as AWS S3 Privado

    Worker->>DB: SELECT * FROM processing_jobs WHERE state = 'JOB_QUEUED' FOR UPDATE SKIP LOCKED
    Worker->>DB: UPDATE processing_jobs SET state = 'PROCESSING_RUNNING', dispatched_at = NOW()
    
    Worker->>HyP3: Submete 72 jobs INSAR_ISCE_BURST (com credencial NASA)
    HyP3-->>Worker: Retorna batch_id do lote de interferogramas

    loop Polling Assíncrono Remoto (a cada 60s)
        Worker->>HyP3: Consulta status do batch
        Worker->>DB: Atualiza coluna execution_progress (ex: 60%)
    end

    HyP3-->>Worker: Conclusão de todos os interferogramas desdobrados
    Worker->>Worker: Executa inversão temporal SBAS via MintPy
    Worker->>Worker: Extrai métricas (coerência média = 0.54, resíduos = 8.7%)
    Worker->>Worker: Converte GeoTIFFs brutos em COG (tiles 256x256 e overviews)
    Worker->>S3: Upload dos COGs para prefixo organizations/.../v1/ com SHA-256
    Worker->>DB: UPDATE processing_jobs SET state = 'PROCESSING_COMPLETED', completed_at = NOW()
```

---

### 6. Diagrama de Sequência: Gating de Qualidade e Disponibilização de Produtos

```mermaid
sequenceDiagram
    autonumber
    participant Listener as JobCompletionEventListener (Spring)
    participant Service as InSarAnalysisApplicationService
    participant Domain as InSarAnalysis (Aggregate)
    participant Repo as InSarAnalysisRepository
    participant DB as PostgreSQL
    actor User as Analista Geotécnico
    participant S3 as AWS S3 Privado

    Listener->>Service: onProcessingJobCompleted(ProcessingJobCompletedEvent)
    Service->>Domain: InSarAnalysis.createFromJob(jobId, productRefs, metrics)
    Domain->>Domain: Executa evaluateQuality(meanCoherence, unwrapResidues)
    
    alt Coerência >= 0.40 e Resíduos <= 15%
        Domain->>Domain: Transiciona para APPROVED_FOR_VISUALIZATION
    else Resíduos > 15% ou Baixa Coerência Tolerável
        Domain->>Domain: Transiciona para QUALIFIED_WITH_RESTRICTIONS (com tarja)
    end

    Service->>Repo: save(analysis)
    Repo->>DB: INSERT INTO radar.insar_analyses & quality_assessments
    
    User->>Service: GET /api/v1/products/{id}/download-url
    Service->>Service: Gera Presigned URL temporária do S3 (TTL = 15 min)
    Service-->>User: Retorna URL segura com token SigV4
    User->>S3: HTTP Range Request GET (Range: bytes=0-16384)
    S3-->>User: HTTP 206 Partial Content (Renderiza pixels no mapa)
```

---

### 7. Diagramas de Estados Técnicos

#### 7.1. Máquina de Estados do Job (`ProcessingJob`)
```mermaid
stateDiagram-v2
    [*] --> JOB_QUEUED: Submissão e autorização
    JOB_QUEUED --> PROCESSING_RUNNING: Capturado pelo Worker (SKIP LOCKED)
    JOB_QUEUED --> EXECUTION_CANCELLED: Cancelado pelo usuário antes do início

    state PROCESSING_RUNNING {
        [*] --> SubmetendoHyP3
        SubmetendoHyP3 --> AguardandoInterferogramas
        AguardandoInterferogramas --> InversaoMintPy
        InversaoMintPy --> FormatandoCOGs
        FormatandoCOGs --> [*]
    }

    PROCESSING_RUNNING --> PROCESSING_RUNNING: Falha Transitória de Rede (Retry < 3)
    PROCESSING_RUNNING --> EXECUTION_FAILED: Falha Determinística ou Retry >= 3
    PROCESSING_RUNNING --> EXECUTION_CANCELLED: Cancelamento Gracioso
    PROCESSING_RUNNING --> PROCESSING_COMPLETED: Inversão concluída e upload S3 ok

    PROCESSING_COMPLETED --> [*]
    EXECUTION_FAILED --> [*]
    EXECUTION_CANCELLED --> [*]
```

#### 7.2. Máquina de Estados da Análise (`InSarAnalysis`)
```mermaid
stateDiagram-v2
    [*] --> SCIENTIFIC_EVALUATION_PENDING: Criada a partir do Job Concluído

    SCIENTIFIC_EVALUATION_PENDING --> APPROVED_FOR_VISUALIZATION: Coerência >= 0.40 e Resíduos <= 15%
    SCIENTIFIC_EVALUATION_PENDING --> QUALIFIED_WITH_RESTRICTIONS: Coerência < 0.40 ou Resíduos > 15% (Aceitável)
    SCIENTIFIC_EVALUATION_PENDING --> REJECTED_QUALITY: Decorrelação Severa / Dados Inconsistentes

    APPROVED_FOR_VISUALIZATION --> [*]: Disponível para Visualização e Análises
    QUALIFIED_WITH_RESTRICTIONS --> [*]: Disponível com Advertência Técnica
    REJECTED_QUALITY --> [*]: Bloqueado para Uso Padrão (Arquivado)
```
