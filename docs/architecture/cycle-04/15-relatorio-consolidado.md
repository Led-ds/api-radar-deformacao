# Radar++ — Ciclo 4: Arquitetura Técnica, UML e Contratos
## Documento 15 — Relatório Consolidado de Arquitetura e Matriz de Rastreabilidade Técnica

**Documento:** `15-relatorio-consolidado.md`  
**Ciclo:** Ciclo 4 — Arquitetura Técnica, UML e Contratos  
**Data:** 24 de Outubro de 2026  
**Status:** Oficial — Entregue para Homologação de Arquitetura  
**Responsáveis:** Equipe Multidisciplinar de Arquitetura e Engenharia do Radar++  
**Repositório Oficial:** `api-radar-deformacao` (caminho: `docs/architecture/cycle-04/15-relatorio-consolidado.md`)  

---

### 1. Resumo Executivo da Arquitetura Homologada

O **Ciclo 4 (Arquitetura Técnica, UML e Contratos)** consolida a transição do Modelo Conceitual DDD do Radar++ para uma especificação técnica robusta, pronta para implementação no Ciclo 5. 

A arquitetura aprovada baseia-se em quatro pilares fundamentais:
1. **Monólito Modular em Java 21 / Spring Boot 3.3+ com Arquitetura Hexagonal:** Unificação das regras de negócio, persistência e governança em um único processo físico, garantindo baixo custo operacional, integridade transacional e isolamento estrito por pacotes (`territory`, `catalog`, `insarcore`, `urbananalysis`, `shared`), sem dependências circulares.
2. **Worker Científico Python 3.11 Especializado e Desacoplado:** Execução assíncrona dos pipelines interferométricos pesados (ASF Search, HyP3 SDK, MintPy SBAS, GDAL) em contêiner independente, acionado via fila orientada a banco relacional com `SELECT ... FOR UPDATE SKIP LOCKED`.
3. **Persistência Híbrida Inteligente e Geodésica:** Metadados estruturados, logs de auditoria e geometrias vetoriais indexadas (GiST) em **PostgreSQL 16 + PostGIS 3.4** (com armazenamento nativo em WGS 84 e projeções sob demanda para SIRGAS 2000 e UTM métrico); matrizes raster armazenadas exclusivamente no **Amazon S3 Privado**, distribuídas via URLs pré-assinadas temporárias e consumidas por **HTTP Range Requests**.
4. **Governança Científica e Legal Estrita:** A velocidade na linha de visada ($d_{\text{LOS}}$) é mantida como a grandeza primária soberana do MVP; o status de conclusão técnica de job computacional não se confunde com validação científica; e as notas técnicas emitidas trazem a ressalva formal de competência profissional de engenharia (ART), vedando alegações indevidas de laudos periciais automáticos com fé pública.

---

### 2. Matriz Completa de Rastreabilidade Técnica

A matriz abaixo estabelece a rastreabilidade bidirecional entre Regras de Negócio, Casos de Uso, Módulos Java, Endpoints REST, Tabelas do Banco PostGIS e Estratégia de Testes:

| Regra / Invariante | Caso de Uso | Módulo Java | Endpoint REST OpenAPI | Tabela PostGIS | Estratégia de Teste Associada |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **REG-DOM-01** (Geometria 2D fechada e cota métrica máxima) | `UC-01` (Delimitar Área de Interesse) | `territory` | `POST /api/v1/areas-of-interest` | `radar.areas_of_interest` | `AreaOfInterestTest` (Unitário) + `AoiRepositoryTest` (Testcontainers PostGIS) |
| **REG-DOM-02** (Isolamento por Organização) | `UC-01` a `UC-08` | Todos os módulos | Todos os endpoints (`X-Org-Id` / JWT) | Todas as tabelas (`organization_id`) | `MultiTenancyIsolationTest` (Integração Spring Security) |
| **REG-DOM-03** (Compatibilidade de Cenas e Bursts SAR) | `UC-02` (Consultar Cenas e Bursts) | `catalog` | `POST /api/v1/sar-catalog/search` | N/A (Consulta a APIs remotas com cache) | `SceneCompatibilityPolicyTest` (Unitário) + `AsfSearchAdapterTest` (Mocked HTTP) |
| **REG-DOM-04** (Limiares de Baseline $B_\perp$ e $\Delta t$) | `UC-03` (Avaliar Compatibilidade de Pares) | `catalog` | `POST /api/v1/sar-catalog/baseline-pair` | N/A (Cálculo matricial em memória) | `BaselineNetworkTest` (Unitário de algoritmo de rede) |
| **REG-DOM-05** (Orçamentação Decomposta de Créditos) | `UC-04` (Solicitar Processamento InSAR) | `insarcore` | `POST /api/v1/processing-requests` | `radar.processing_requests` | `CreditEstimationPolicyTest` (Tabela oficial HyP3) |
| **REG-DOM-06** (Idempotência e 3 Retentativas) | `UC-05` (Executar e Monitorar Job) | `insarcore` | `POST /api/v1/processing-requests/{id}/authorize` | `radar.processing_jobs` e `radar.job_retries` | `JobRetryAndBackoffTest` (Integração assíncrona) |
| **REG-DOM-07** (Job Concluído $\neq$ Validação Científica) | `UC-06` (Avaliar Qualidade Científica) | `insarcore` | `GET /api/v1/analyses/{id}` | `radar.insar_analyses` e `radar.quality_assessments` | `QualityGatingSeparationTest` (Teste de máquina de estados) |
| **REG-DOM-08** (Critérios de Coerência $\bar{\gamma} \ge 0{,}40$ e Resíduos) | `UC-06` (Avaliar Qualidade Científica) | `insarcore` | `GET /api/v1/analyses/{id}` | `radar.quality_assessments` | `PhaseClosureAndCoherenceTest` (Validação científica com dados reais) |
| **REG-DOM-09** (COG como Formato de Entrega) | `UC-07` (Consultar Séries Temporais) | `insarcore` | `GET /api/v1/products/{id}/download-url` | `radar.scientific_products` | `CogHttpRangeRequestTest` (LocalStack S3 parcial) |
| **REG-DOM-11** (Soberania de $d_{\text{LOS}}$ Primário) | `UC-07` (Consultar Séries Temporais) | `insarcore` | `GET /api/v1/analyses/{id}/timeseries` | N/A (Leitura de pixel flutuante no COG/NetCDF) | `DisplacementUnitConsistencyTest` (Unitário geodésico) |
| **REG-DOM-12** (Observações Técnicas e Ressalva de ART) | `UC-08` (Registrar Observação Técnica) | `urbananalysis` | `POST /api/v1/urban-investigations/{id}/observations` | `radar.technical_observations` | `TechnicalNoteDisclaimerTest` (Validação de cláusula legal no PDF) |
| **REG-DOM-13** (Proveniência Técnica e Hash SHA-256) | `UC-05` e `UC-08` | `insarcore` e `urbananalysis` | `GET /api/v1/technical-notes/{id}/pdf` | `radar.scientific_products` e `radar.technical_observations` | `Sha256ChecksumVerificationTest` (Integridade de arquivos) |

---

### 3. Estimativa Preliminar de Custos de Infraestrutura (AWS TCO - MVP)

Projeção de custos operacionais mensais para suportar o MVP com 10 organizações monitorando áreas de até 35 km² cada:

```
+----------------------------------------------------------------------------------------------------------------------------------------------------+
|                                    ESTIMATIVA DE CUSTO MENSAL DE INFRAESTRUTURA EM NUVEM (AWS)                                                     |
+---+------------------------------+---------------------------------------+-----------------------------+-------------------------------------------+
| # | Componente de Arquitetura    | Dimensionamento / Recurso AWS         | Modo de Operação            | Custo Mensal Estimado (USD)               |
+---+------------------------------+---------------------------------------+-----------------------------+-------------------------------------------+
| 1 | **Backend Spring Boot**      | AWS ECS Fargate (0.5 vCPU, 1 GB RAM)  | Contínuo (24/7)             | ~ US$ 15,00                               |
+---+------------------------------+---------------------------------------+-----------------------------+-------------------------------------------+
| 2 | **Worker InSAR Python**      | AWS ECS Fargate / EC2 Spot (1 vCPU)   | Sob Demanda (~2h/dia ativo) | ~ US$ 6,00                                |
+---+------------------------------+---------------------------------------+-----------------------------+-------------------------------------------+
| 3 | **Banco de Dados Relacional**| Amazon RDS PostgreSQL db.t4g.micro    | Contínuo (20 GB gp3)        | ~ US$ 18,00                               |
+---+------------------------------+---------------------------------------+-----------------------------+-------------------------------------------+
| 4 | **Armazenamento de Objetos** | Amazon S3 Standard + Glacier Instant  | 5 GB final + 150 GB efêmero | ~ US$ 5,00                                |
+---+------------------------------+---------------------------------------+-----------------------------+-------------------------------------------+
| 5 | **Rede, Tráfego e DNS**      | Route 53 + Data Transfer Out          | ~10 GB tráfego/mês          | ~ US$ 2,50                                |
+---+------------------------------+---------------------------------------+-----------------------------+-------------------------------------------+
| 6 | **Frontend SPA**             | Amazon CloudFront + S3 Estático       | Cache em borda global       | ~ US$ 1,50                                |
+---+------------------------------+---------------------------------------+-----------------------------+-------------------------------------------+
|   | **TOTAL MENSAL ESTIMADO**    |                                       |                             | **~ US$ 48,00 / mês**                     |
+---+------------------------------+---------------------------------------+-----------------------------+-------------------------------------------+
```
> O custo inferior a US$ 50/mês demonstra a sustentabilidade e a viabilidade comercial do produto Radar++ para clientes de médio porte no Brasil.

---

### 4. Parecer Final de Conclusão do Ciclo 4

A equipe técnica declara que o **Ciclo 4 (Arquitetura Técnica, UML e Contratos)** está **INTEGRALMENTE CONCLUÍDO E DOCUMENTADO**. Todos os critérios de aceite foram cumpridos:
- Os componentes e suas responsabilidades estão estritamente definidos;
- As fronteiras entre módulos Java eliminam dependências circulares;
- O modelo relacional e espacial no PostGIS é coerente e performático;
- A estratégia assíncrona, de mensageria e de storage S3 privado atende às restrições financeiras;
- Os contratos REST OpenAPI cobrem integralmente os casos de uso do MVP;
- O sistema está formalmente preparado para a entrada no **Ciclo 5 (Implementação e Engenharia de Software)**.
