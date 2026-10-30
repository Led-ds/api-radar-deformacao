# Radar++ — Ciclo 3: Modelagem Estratégica e Tática com DDD
## Documento 11 — Relatório de Homologação Técnica do Domínio e Preparação para o Ciclo 4

**Documento:** `11-relatorio-homologacao-tecnica.md`  
**Ciclo:** Homologação Técnica do Ciclo 3 e Transição para o Ciclo 4  
**Data:** 24 de Outubro de 2026  
**Status:** Concluído — Aguardando Aprovação da Liderança do Radar++  
**Responsáveis:** Especialista em DDD, Modelador de Domínios Geoespaciais, Arquiteto de Software, Especialista em SAR/InSAR  
**Repositório Oficial:** `api-radar-deformacao` (caminho: `docs/domain/cycle-03/11-relatorio-homologacao-tecnica.md`)  

---

### 1. Resumo Executivo da Homologação

O presente relatório consolida a revisão final de consistência e alinhamento do **Modelo de Domínio DDD (Ciclo 3)** do produto **Radar++**. Esta revisão refinou as especificações conceituais e os diagramas, sanando inconsistências remanescentes e assegurando conformidade estrita com o escopo aprovado do MVP, os limites físicos do sensoriamento remoto SAR/InSAR e as boas práticas de engenharia de software antes da transição para o **Ciclo 4 (Arquitetura e Desenho Técnico)**.

---

### 2. Correções Realizadas e Inconsistências Resolvidas

Em atendimento integral às diretrizes de homologação técnica, os documentos do Ciclo 3 foram atualizados pontualmente:

1. **Modelagem Geodésica de LOS e Projeção Vertical (`04-invariantes-e-politicas.md`, `09-matriz-rastreabilidade.md`):**
   - **Correção:** A fórmula escalar de projeção $d_{\text{vert}} = d_{\text{LOS}} / \cos\theta$ deixou de ser apresentada como regra universal.
   - **Fundamentação Física:** Foram explicitadas suas hipóteses obrigatórias (deslocamento estritamente unidimensional vertical, $d_{\text{East}} \approx 0, d_{\text{North}} \approx 0$) e a convenção de sinais ($d_{\text{LOS}} < 0$ para afastamento/subsidência; $d_{\text{vert}} < 0$ para rebaixamento).
   - **Limitações e Proibições:** Documentou-se a proibição da projeção vertical em encostas com movimento gravitacional de massa, bordas periféricas de bacias com tração extensional horizontal e zonas de falha transcorrente.
   - **Grandeza Primária Soberana:** O deslocamento na linha de visada ($d_{\text{LOS}}$) foi consagrado como a grandeza primária mandatória do MVP.

2. **Orçamentação e Cálculo de Créditos HyP3 (`04-invariantes-e-politicas.md`, `10-decisoes-pendentes.md`):**
   - **Correção:** Eliminou-se qualquer menção a uma fórmula simplificada universal de créditos.
   - **Decomposição Oficial:** O cálculo foi estruturado com base nas variáveis da tabela tarifária do ASF HyP3:
     - Tipo de Job: `INSAR_ISCE_BURST` (processamento nativo por burst Sentinel-1 IW);
     - Quantidade de interferogramas que compõem a rede SBAS ($N_{\text{interf}}$);
     - Quantidade de bursts que interceptam a geometria efetiva da AOI ($N_{\text{bursts}}$);
     - Resolução espacial de multilooking (20 m com fator de look $5 \times 1$);
     - Tarifa unitária oficial (1 crédito por job de par-burst).

3. **Neutralidade e Geometria do Piloto Maceió (`04-invariantes-e-politicas.md`, `10-decisoes-pendentes.md`):**
   - **Correção:** Eliminou-se qualquer atribuição de preferência prévia ou direção orbital para as faixas Sentinel-1. Os **Tracks 142 e 171** foram classificados estritamente como hipóteses pendentes de confirmação empírica por metadados reais de catálogo.
   - **Separação Espacial:** Estabeleceu-se a distinção conceitual e operacional entre a **BBOX de busca de catálogo** (envelope retangular para queries em APIs) e a **geometria efetiva da AOI** (polígono vetorial fechado de 25 a 35 km² que delimita a mancha urbana ativa para cômputo e estatísticas).

4. **Desacoplamento e Escopo de Agregados (`03-agregados-entidades-value-objects.md`, `08-diagramas-uml.md`):**
   - **`InSarAnalysis`:** Blindado contra o carregamento de matrizes geodésicas volumosas, arquivos binários ou séries temporais brutas em memória. O agregado gerencia exclusivamente metadados de auditoria, referências leves (`ScientificProductRef`), ponteiros S3 (`StorageArtifactUri`), hashes SHA-256 e o resultado do gating de qualidade (`QualityAssessment`).
   - **`ScientificProduct`:** Definida como entidade interna subordinada ao agregado `InSarAnalysis`, com formato de arquivo explícito (`COG`, `GeoTIFF`, `NetCDF`, `HDF5`, `Parquet`), checksum de integridade e localização no storage, eliminando o acoplamento conceitual exclusivo ao formato COG.
   - **`UrbanInvestigation`:** Removida qualquer responsabilidade automática de emissão de diagnósticos geotécnicos de segurança estrutural, laudos periciais oficiais com fé pública, declarações conclusivas de estabilidade ou previsões de colapso. O agregado atua estritamente como espaço investigativo de exploração visual, comparação histórica e registro de notas técnicas observacionais (`TechnicalObservationRecorded`) com ressalva formal de competência de engenharia (ART).

5. **Taxonomia e Confiabilidade de Eventos (`05-eventos-de-dominio.md`, `09-matriz-rastreabilidade.md`):**
   - Os eventos foram segregados em quatro categorias formais: **Eventos de Domínio**, **Eventos de Integração**, **Eventos Técnicos de Processamento** e **Registros de Auditoria**.
   - Definiu-se o padrão **Transactional Outbox** para garantir entrega confiável (*At-Least-Once*), tabelas de deduplicação para garantir **idempotência nos consumidores** e retenção para **capacidade de repetição (*replay*)**.

6. **Segregação de Estados e Falhas Parciais (`06-estados-e-transicoes.md`):**
   - Foram consolidados os **quatro ciclos de vida finitos independentes**: Solicitação (`ProcessingRequest`), Job Operacional (`ProcessingJob`), Análise Científica (`InSarAnalysis`) e Produto Geodésico (`ScientificProduct`).
   - Reafirmou-se a invariante de que um Job `PROCESSING_COMPLETED` **não torna a análise cientificamente válida**.
   - Modelou-se a tolerância a falhas parciais em redes SBAS, permitindo descartar interferogramas decorrelacionados pontuais desde que a conectividade da rede temporal seja preservada.

7. **Política de Armazenamento e Retenção em Três Níveis (`04-invariantes-e-politicas.md`):**
   - Estruturou-se a política de governança de dados no AWS S3 em:
     1. *Produtos Finais Preservados:* S3 Standard / Glacier Instant Retrieval, permanente, versionado, imutável.
     2. *Produtos Intermediários:* S3 Standard com ciclo de vida de 30 a 90 dias para permitir re-inversão no MintPy, seguido de transição/expurgo.
     3. *Arquivos Temporários Descartáveis:* Lifecycle rule de 7 a 14 dias para expurgo automático de cenas SLC brutas de 4-8 GB e arquivos de scratch de workers.

---

### 3. Decisões Preservadas

Permanecem integralmente validadas e mantidas:
- **Monólito Modular** como modelo arquitetural físico para o Ciclo 4, evitando a complexidade de microsserviços distribuídos;
- **Subdomínio de Qualidade** conceitualmente separado, mas compartilhando o mesmo módulo físico de Processamento InSAR;
- **Multi-tenancy Lógico Simples** ancorado no Value Object `OrganizationId`, sem separação física ou dinâmica de bancos de dados;
- **Histórico Técnico Auditável** com cadeia de custódia e proveniência criptográfica (SHA-256), servindo como evidência técnica reprodutível;
- **Congelamento do Módulo de Infraestrutura Linear** para o Ciclo 5+.

---

### 4. Lista Consolidada de Agregados e Fronteiras Transacionais

| Agregado (Raiz) | Contexto Delimitado | Limite Transacional / Componentes Internos | Invariante Central de Negócio |
| :--- | :--- | :--- | :--- |
| **`AreaOfInterest`** | Gestão Territorial | Entidade Raiz + `PolygonGeometry` + `BoundingBox` + `OrganizationId` | Polígono 2D fechado sem auto-interseção; área dentro do teto contratual; posse organizacional imutável. |
| **`ProcessingRequest`** | Processamento InSAR | Entidade Raiz + `AnalysisParameters` + `CreditQuota` | Janela temporal válida ($\text{data}_{\text{fim}} > \text{data}_{\text{início}}$); parâmetros imutáveis após autorização. |
| **`ProcessingJob`** | Processamento InSAR | Entidade Raiz + `ExecutionTimeline` + `RetryCounter` | Transição de estados determinística; retentativas limitadas a 3 falhas transitórias; estados terminais imutáveis. |
| **`InSarAnalysis`** | Processamento InSAR | Entidade Raiz + `QualityAssessment` + `List<ScientificProductRef>` | Não carrega matrizes binárias; aprovação condicionada a portão de qualidade ($\bar{\gamma} \ge \text{limiar}$ e resíduos $\le 15\%$). |
| **`UrbanInvestigation`** | Análise Urbana | Entidade Raiz + `List<SectionProfile>` + `List<TechnicalObservationRecord>` | Vínculo exclusivo com análise aprovada/restrita; vedação expressa de laudos periciais automáticos; notas com ressalva ART. |

---

### 5. Eventos e Estados Revisados

#### 5.1. Eventos Oficiais do Sistema
- **Eventos de Domínio / Integração:** `AreaOfInterestRegistered`, `AreaOfInterestUpdated`, `ProcessingRequestSubmitted`, `ProcessingRequestAuthorized`, `ProcessingJobDispatched`, `ProcessingJobCompleted`, `ProcessingJobFailed`, `InSarAnalysisCreated`, `ProductQualityEvaluated`, `ProductApprovedForVisualization`, `ProductQualifiedWithRestrictions`, `ProductRejectedQuality`, `UrbanInvestigationInitiated`, `TechnicalObservationRecorded`.
- **Garantias Técnicas:** Eventos de integração despachados via **Transactional Outbox**, consumidos com verificação de **Idempotência** (`eventId`) e auditáveis para **Replay**.

#### 5.2. Estados Canônicos
- **`ProcessingRequest`:** `REQUEST_RECEIVED`, `REQUEST_VALIDATED`, `REQUEST_AUTHORIZED`, `REQUEST_REJECTED`, `REQUEST_CANCELLED`.
- **`ProcessingJob`:** `JOB_QUEUED`, `PROCESSING_RUNNING`, `PROCESSING_COMPLETED`, `EXECUTION_FAILED`, `EXECUTION_CANCELLED`.
- **`InSarAnalysis`:** `ANALYSIS_CREATED`, `SCIENTIFIC_EVALUATION_PENDING`, `APPROVED_FOR_VISUALIZATION`, `QUALIFIED_WITH_RESTRICTIONS`, `REJECTED_QUALITY`.
- **`ScientificProduct`:** `STAGED_FOR_EVALUATION`, `AVAILABLE_FOR_VISUALIZATION`, `RESTRICTED_ACCESS`, `ARCHIVED`.

---

### 6. Questões em Aberto para Validação no Ciclo 4

1. **Confirmação Empírica das Órbitas de Maceió:** Execução da consulta exploratória pública à API do ASF Search/Burst para verificar se a área urbana crítica (bairros Pinheiro/Mutange) encaixa-se em 1 ou 2 bursts nas faixas 142 e 171;
2. **Definição do Provedor de Mensageria Interno:** Escolha entre RabbitMQ vs. Amazon SQS/SNS para o despacho de mensagens do Transactional Outbox dentro da infraestrutura operacional;
3. **Mecanismo de Renderização de COG:** Validação do uso de visualizador cliente (TiTiler em contêiner Python desacoplado vs. leitura direta em React via geotiff.js/OpenLayers).

---

### 7. Parecer de Prontidão para o Ciclo 4

> [!IMPORTANT]
> **PARECER TÉCNICO FORMAL:**  
> O Modelo de Domínio DDD do Radar++ encontra-se **HOMOLOGADO, CONSISTENTE e TOTALMENTE PRONTO** para a transição para o **Ciclo 4 (Arquitetura e Desenho Técnico)**.  
> As fronteiras estão claramente delimitadas, os agregados protegidos contra acoplamento excessivo de dados pesados, os limites legais e físicos formalizados, e os contratos de eventos estabelecidos.

---

### 8. Proposta de Escopo Detalhada para o Ciclo 4 (Arquitetura e Desenho Técnico)

Para o Ciclo 4, propõe-se o desenvolvimento estritamente arquitetural e técnico dos seguintes componentes (sem implementação de código):

1. **Arquitetura C4 (System Context, Container, Component, Code):**
   - Elaboração dos quatro níveis do modelo C4 detalhando as interações entre a SPA React, o Backend Spring Boot (Monólito Modular), os Workers Python InSAR, o PostgreSQL/PostGIS e os serviços de nuvem (AWS S3, ASF HyP3).
2. **UML Detalhado de Classes e Diagramas de Sequência Técnicos:**
   - Classes de domínio mapeadas para DTOs, entidades de domínio ricas e Value Objects imutáveis (Records Java).
   - Sequência técnica da ingestão assíncrona, conversão COG e consulta de séries temporais via HTTP Range Request.
3. **Organização de Módulos Java (Monólito Modular):**
   - Estrutura de pacotes por contexto: `com.ass.br.apiradar.territory`, `com.ass.br.apiradar.catalog`, `com.ass.br.apiradar.insarcore`, `com.ass.br.apiradar.urbananalysis`, `com.ass.br.apiradar.shared`.
   - Isolamento de visibilidade de pacotes (classes internas com package-private, expondo apenas interfaces e DTOs de integração).
4. **Arquitetura Hexagonal (Portas e Adaptadores):**
   - Definição formal de *Inbound Ports* (Casos de Uso), *Outbound Ports* (Repositórios e Clientes de Nuvem) e seus adaptadores (Controladores REST, Adaptador HyP3, Adaptador S3, Repositórios Spring Data JPA).
5. **Integração Python / HyP3 / MintPy:**
   - Especificação do protocolo de comunicação entre o backend Java e os scripts/workers de processamento geodésico (mensageria de tarefas assíncronas, schemas JSON de parâmetros e volumes compartilhados/S3).
6. **Modelagem de Dados PostgreSQL / PostGIS:**
   - DDL conceitual de esquemas e tabelas;
   - Definição de colunas geométricas (`GEOMETRY(POLYGON, 4674)` com SRID SIRGAS 2000 / WGS 84);
   - Índices espaciais GIST e tabela dedicada para `outbox_events`.
7. **Estratégia Detalhada AWS S3:**
   - Estruturação de prefixos/buckets por categoria de retenção (preservada, intermediária, temporária);
   - Regras de Lifecycle (transição para Glacier Instant Retrieval e expurgo);
   - Políticas de CORS e cabeçalhos para suporte nativo a HTTP Range Requests em arquivos COG.
8. **Contratos REST / OpenAPI 3.0:**
   - Especificação técnica dos endpoints de entrada, autenticação Bearer JWT, formatos de payload de comandos CQRS e respostas assíncronas (HTTP 202 com cabeçalho `Location`).
9. **Resiliência e Tratamento de Falhas:**
   - Configuração conceitual de Circuit Breaker (Resilience4j) para integrações externas (ASF/NASA);
   - Políticas de Dead Letter Queue (DLQ) para mensagens corrompidas.
10. **Segurança e Observabilidade:**
    - Autenticação JWT stateless com escopo de `OrganizationId`;
    - Instrumentação de métricas via Micrometer/Prometheus e rastreamento distribuído (OpenTelemetry/W3C Trace Context).
11. **Estratégia Abrangente de Testes:**
    - Matriz de testes: Unitários com arquitetura limpa (ArchUnit), Integração com Testcontainers (PostgreSQL + PostGIS), e Testes de Contrato para endpoints e eventos.
