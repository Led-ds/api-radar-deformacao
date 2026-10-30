# Radar++ — Ciclo 3: Modelagem Estratégica e Tática com DDD
## Documento 09 — Matriz de Rastreabilidade de Domínio

**Status:** Homologado para o Ciclo 3  
**Data:** 24/10/2026  
**Contextos Abrangidos:** Gestão Territorial, Catálogo SAR, Processamento e Produtos InSAR, Análise de Subsidência Urbana  

---

### 1. Objetivo da Matriz de Rastreabilidade

Garantir que todas as decisões estratégicas e táticas concebidas no Ciclo 3 (Agregados, Entidades, Value Objects, Invariantes, Políticas, Eventos de Domínio e Casos de Uso) possuam rastreabilidade bidirecional com:
1. Os achados e requisitos derivados do Discovery (D1, D2, D3 e Encerramento);
2. A Linguagem Ubíqua v1 (Ciclo 2);
3. As correções obrigatórias de domínio estabelecidas pela governança técnica.

---

### 2. Matriz de Rastreabilidade Tática e Estratégica

| ID Req. / Origem | Necessidade do Domínio | Contexto Delimitado | Caso de Uso / Comando | Agregado / Entidade Raiz | Regra / Invariante Associada | Evento de Domínio Disparado | Portão de Validação / Qualidade |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **REQ-TER-01** (D-ENC-02) | Delimitação territorial de áreas de monitoramento com controle de escala | Gestão Territorial | `UC-TER-01: Delimitar Área de Interesse` (`RegisterAreaOfInterestCommand`) | `AreaOfInterest` | `REG-DOM-01` (Geometria 2D fechada, CRS EPSG:4326/SIRGAS2000, $0 < \text{Área} \le \text{LimiteConfigurável}$) | `AreaOfInterestRegistered` | Validação topológica OGC e cota máxima parametrizada por `OrganizationId`. |
| **REQ-TER-02** (D-ENC-02) | Controle de isolamento e governança de dados por entidade | Gestão Territorial | `UC-TER-01` / `UC-URB-01` | `AreaOfInterest`, `UrbanInvestigation` | `REG-DOM-02` (Escopo Organizacional - isolamento lógico simples sem multi-tenancy dinâmico) | `AreaOfInterestRegistered`, `UrbanInvestigationInitiated` | Pertencimento estrito a `OrganizationId` e checagem de perfil (`TechnicalAnalyst`). |
| **REQ-SAR-01** (D1-05 / D3-04) | Verificação de disponibilidade de cenas e bursts compatíveis | Catálogo SAR | `UC-SAR-01: Consultar Cenas e Bursts Compatíveis` (`SearchCompatibleScenesQuery`) | `SarCatalog` (Serviço de Catálogo + Value Objects) | `REG-DOM-03` (Identidade de Bursts, mesma faixa orbital e modo IW SLC) | N/A (Consulta Idempotente) | Filtro por geometria AOI, polarização (VV preferencial), sub-swath (IW1/IW2/IW3). |
| **REQ-SAR-02** (D1-05 / D-ENC-03) | Prevenção de perda de coerência temporal e espacial | Catálogo SAR / Processamento | `UC-SAR-02: Avaliar Compatibilidade de Pares` (`EvaluateBaselinePairQuery`) | `BaselinePair` (Value Object) | `REG-DOM-04` (Baselines Parametrizáveis: $B_\perp \le 150\,\text{m}$, $\Delta t \le 36\,\text{dias}$) | N/A (Validação prévia) | Checagem contra perfis metodológicos (`ConservativeUrban`, `StandardInSAR`). |
| **REQ-PROC-01** (D3-04 / D-ENC-02) | Solicitação formal de processamento com estimativa orçamentária | Processamento e Produtos InSAR | `UC-PROC-01: Solicitar Processamento InSAR` (`SubmitProcessingRequestCommand`) | `ProcessingRequest` | `REG-DOM-05` (Validação de parâmetros científicos, cálculo de créditos necessários) | `ProcessingRequestSubmitted`, `ProcessingRequestValidated` | Verificação do saldo de créditos da organização e validação de parâmetros. |
| **REQ-PROC-02** (D3-05 / D-ENC-03) | Execução assíncrona desacoplada de pipelines SAR externos | Processamento e Produtos InSAR | `UC-PROC-02: Executar e Monitorar Job InSAR` (`DispatchJobExecutionCommand`) | `ProcessingJob` | `REG-DOM-06` (Idempotência de execução, limite de 3 retentativas por falhas de infraestrutura transitórias) | `ProcessingJobDispatched`, `ProcessingJobCompleted`, `ProcessingJobFailed` | Rastreabilidade do motor externo (`ExternalJobId`), monitoramento passivo. |
| **REQ-PROC-03** (Correção Ciclo 3) | Desacoplamento entre conclusão de job computacional e validade científica | Processamento e Produtos InSAR | `UC-PROC-02` $\rightarrow$ `UC-QUAL-01` | `ProcessingJob` $\rightarrow$ `InSarAnalysis` | `REG-DOM-07` (Job técnico concluído $\neq$ Produto cientificamente validado) | `ProcessingJobCompleted` $\rightarrow$ `InSarAnalysisPendingEvaluation` | Job técnico atinge `PROCESSING_COMPLETED`; análise científica transita para `SCIENTIFIC_EVALUATION_PENDING`. |
| **REQ-QUAL-01** (D-ENC-03 / Piloto Maceió) | Homologação científica baseada em coerência interferométrica | Processamento e Produtos InSAR | `UC-QUAL-01: Avaliar Qualidade Científica do Produto` (`EvaluateProductQualityCommand`) | `InSarAnalysis` | `REG-DOM-08` (Critérios de Coerência $\bar{\gamma} \ge 0{,}30$ e desvio de fechamento de fase) | `ProductApprovedForVisualization`, `ProductQualifiedWithRestrictions`, `ProductRejectedQuality` | Decisão baseada no `QualityMetricSet` calculado pelo pipeline. |
| **REQ-PROD-01** (Correção Ciclo 3) | Diferenciação entre Produto InSAR (modelo geodésico) e COG (formato de entrega) | Processamento e Produtos InSAR | `UC-QUAL-01` | `ScientificProduct` (Entidade interna de `InSarAnalysis`) | `REG-DOM-09` (COG como representação física / representação raster, não essência de domínio) | `ProductApprovedForVisualization` | Armazenamento de metadados geodésicos estruturados + URI da representação física (Cloud Optimized GeoTIFF). |
| **REQ-URB-01** (D1-04 / D3-04) | Abertura de dossiê técnico de investigação urbana | Análise de Subsidência Urbana | `UC-URB-01: Iniciar Investigação de Subsidência Urbana` (`InitiateUrbanInvestigationCommand`) | `UrbanInvestigation` | `REG-DOM-10` (Vínculo obrigatório com AOI homologada e analista responsável) | `UrbanInvestigationInitiated` | Verificação de elegibilidade da AOI e permissões do analista. |
| **REQ-URB-02** (D-ENC-03 / Piloto Maceió) | Distinção semântica estrita entre deslocamento LOS e vertical | Análise de Subsidência Urbana | `UC-URB-02: Interpretar Deformação e Gradiente Crítico` (`InterpretSubsidenceCommand`) | `UrbanInvestigation` / `DeformationTimeSeries` | `REG-DOM-11` (d_LOS é primário; projeção vertical d_vert condicional à hipótese vertical estrita com ângulo θ) | `DeformationGradientAnomalyDetected` | O sistema proíbe comparar diretamente medição de nivelamento óptico com $d_{\text{LOS}}$ sem projeção e documentação de incertezas. |
| **REQ-GOV-01** (Correção Ciclo 3) | Delimitação de responsabilidade técnica de engenharia (ART) | Análise de Subsidência Urbana | `UC-URB-03: Registrar Observação Técnica` (`RecordTechnicalObservationCommand`) | `UrbanInvestigation` | `REG-DOM-12` (Nota técnica exploratória; ART como delimitação de competência e governança profissional, sem diagnóstico conclusivo automático) | `TechnicalObservationRecorded` | Exigência de declaração formal de limites metodológicos na nota emitida. |
| **REQ-GOV-02** (Correção Ciclo 3) | Rastreabilidade probatória e proveniência técnica auditável | Processamento e Produtos InSAR / Análise Urbana | `UC-PROC-02`, `UC-QUAL-01`, `UC-URB-03` | `ProcessingJob`, `InSarAnalysis`, `UrbanInvestigation` | `REG-DOM-13` (Registro auditável de parâmetros, versões de algoritmos e hashes SHA-256; ausência de claim de prova pericial judicial certificada) | `TechnicalObservationRecorded` | Cadeia de custódia técnica com imutabilidade de metadados de execução. |

---

### 3. Mapeamento de Correções Críticas Exigidas no Ciclo 3

A tabela abaixo sintetiza como cada correção imposta pela governança técnica no Ciclo 3 foi endereçada nos artefatos de domínio:

| Correção de Domínio Exigida | Problema Anterior (V1 / Discovery) | Solução Conceitual Adotada (Ciclo 3) | Artefatos e Seções de Referência |
| :--- | :--- | :--- | :--- |
| **1. Multi-tenancy desproporcional** | Premissa prematura de multi-tenancy complexo com esquemas dinâmicos de isolamento. | Adoção do conceito enxuto de **Organização (`OrganizationId`)** como fronteira de governança lógica em monólito modular. | [01-context-map-v2.md](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/docs/domain/cycle-03/01-context-map-v2.md#secao-5), [04-invariantes-e-politicas.md](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/docs/domain/cycle-03/04-invariantes-e-politicas.md#reg-dom-02) |
| **2. ART e Isenção Jurídica** | Tratamento da ART (Anotação de Responsabilidade Técnica) como isenção automática de responsabilidade civil/penal do software. | Reclassificação da ART como **fronteira de competência e governança profissional**, exigindo nota técnica emitida por profissional habilitado com declaração explícita de incertezas instrumentais e sem diagnóstico conclusivo automático. | [04-invariantes-e-politicas.md](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/docs/domain/cycle-03/04-invariantes-e-politicas.md#reg-dom-12), [07-casos-de-uso.md](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/docs/domain/cycle-03/07-casos-de-uso.md#uc-urb-03) |
| **3. Parâmetros Científicos Estáticos** | Fixação de limiares universais e imutáveis ($B_\perp$, $\bar{\gamma}$, janelas temporais, limites de área). | Transformação em **Parâmetros Parametrizáveis vinculados a Perfis Metodológicos** (`MethodologicalProfile`), com justificativa científica explícita. | [03-agregados-entidades-value-objects.md](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/docs/domain/cycle-03/03-agregados-entidades-value-objects.md#value-objects), [04-invariantes-e-politicas.md](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/docs/domain/cycle-03/04-invariantes-e-politicas.md#reg-dom-04) |
| **4. COG como Essência de Produto** | Tratar o Cloud Optimized GeoTIFF (COG) como se fosse a própria definição de Produto InSAR. | O Produto InSAR é conceituado como uma **observação geodésica espacializada** (matriz de fase/deformação e metadados de proveniência), enquanto o COG é apenas uma de suas **representações físicas em formato raster otimizado**, ao lado de NetCDF, HDF5 e GeoTIFF. | [02-modelo-conceitual.md](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/docs/domain/cycle-03/02-modelo-conceitual.md#camada-de-produtos-e-dados), [04-invariantes-e-politicas.md](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/docs/domain/cycle-03/04-invariantes-e-politicas.md#reg-dom-09) |
| **5. Prova Pericial Certificada** | Afirmação de que logs e metadados constituiriam prova pericial incontestável em juízo. | Reclassificação para **Histórico Técnico Auditável com Rastreabilidade de Proveniência**, servindo como subsídio probatório transparente, sem atribuição de fé pública pericial automática. | [04-invariantes-e-politicas.md](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/docs/domain/cycle-03/04-invariantes-e-politicas.md#reg-dom-13) |
| **6. Ambiguidade LOS vs. Vertical** | Tratar séries temporais de deformação na linha de visada (LOS) como se fossem diretamente rebaixamento vertical ($\Delta z$). | Separação semântica mandatória: `DeformationLOS` é a grandeza primária soberana; `VerticalDeformation` é grandeza derivada condicional que exige registro explícito do ângulo $\theta$ e hipótese unidimensional vertical estrita. Proibida em encostas/taludes. | [03-agregados-entidades-value-objects.md](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/docs/domain/cycle-03/03-agregados-entidades-value-objects.md#value-objects), [04-invariantes-e-politicas.md](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/docs/domain/cycle-03/04-invariantes-e-politicas.md#reg-dom-11) |
| **7. Conclusão Técnica vs. Validação Científica** | Supor que o fim do processamento em lote libera automaticamente o produto para o usuário final. | Criação de um portão de transição de estado estrito: o Job atinge `PROCESSING_COMPLETED`, mas a Análise Científica permanece em `SCIENTIFIC_EVALUATION_PENDING` até homologação formal pelo analista ou algoritmo de qualidade. | [06-estados-e-transicoes.md](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/docs/domain/cycle-03/06-estados-e-transicoes.md#invariante-fundamental-de-desacoplamento), [08-diagramas-uml.md](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/docs/domain/cycle-03/08-diagramas-uml.md#diagrama-5-estados-da-analise-cientifica) |

---

### 4. Matriz de Cobertura de Eventos de Domínio por Caso de Uso

| Caso de Uso Executor | Evento de Domínio Gerado | Assinantes / Efeitos Colaterais no Domínio |
| :--- | :--- | :--- |
| `UC-TER-01` | `AreaOfInterestRegistered` | Atualiza catálogo de áreas monitoradas; disponibiliza contexto para consultas SAR. |
| `UC-PROC-01` | `ProcessingRequestSubmitted` | Notifica orquestrador de cobrança e serviço de verificação de saldo de créditos. |
| `UC-PROC-01` | `ProcessingRequestValidated` | Autoriza a criação de `ProcessingJob` na fila de despacho operacional. |
| `UC-PROC-02` | `ProcessingJobDispatched` | Registra início do cômputo no motor remoto; inicia temporizador de timeout. |
| `UC-PROC-02` | `ProcessingJobCompleted` | Instancia entidade `ScientificProduct` e coloca `InSarAnalysis` no estado de avaliação pendente. |
| `UC-PROC-02` | `ProcessingJobFailed` | Notifica analista; avalia elegibilidade para retentativa operacional automática. |
| `UC-QUAL-01` | `ProductApprovedForVisualization` | Notifica camada de mapa; libera camadas raster COG para renderização e análises urbanas. |
| `UC-QUAL-01` | `ProductQualifiedWithRestrictions` | Libera produto com aviso de baixa coerência em regiões específicas (máscara de qualidade). |
| `UC-QUAL-01` | `ProductRejectedQuality` | Bloqueia visualização padrão; arquiva produto com parecer técnico de rejeição metodológica. |
| `UC-URB-01` | `UrbanInvestigationInitiated` | Cria workspace de análise geotécnica urbana associado à AOI e analista. |
| `UC-URB-02` | `DeformationGradientAnomalyDetected`| Dispara alerta de subsidência acelerada para o painel de triagem do analista. |
| `UC-URB-03` | `TechnicalObservationRecorded` | Gera registro auditável imutável da nota técnica observacional com hash criptográfico. |

---

### 5. Conclusão da Validação de Rastreabilidade

A matriz confirma 100% de cobertura entre os problemas identificados no Discovery, os requisitos corrigidos pela governança e as especificações formais de DDD do Ciclo 3. Não há elementos órfãos no modelo conceitual, garantindo que o Ciclo 4 (Arquitetura e Desenho Técnico) possa ser construído sobre bases sólidas, verificáveis e semanticamente blindadas.
