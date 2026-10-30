# Radar++ — Ciclo 4: Arquitetura Técnica, UML e Contratos
## Documento 14 — Registros de Decisões Arquiteturais (ADRs) e Questões Pendentes

**Documento:** `14-adrs-e-decisoes-pendentes.md`  
**Ciclo:** Ciclo 4 — Arquitetura Técnica, UML e Contratos  
**Data:** 24 de Outubro de 2026  
**Status:** Oficial — Entregue para Homologação de Arquitetura  
**Responsáveis:** Arquiteto de Software Sênior, Arquiteto de Sistemas Distribuídos, Arquiteto Cloud  
**Repositório Oficial:** `api-radar-deformacao` (caminho: `docs/architecture/cycle-04/14-adrs-e-decisoes-pendentes.md`)  

---

### 1. ADR-002 — Monólito Modular vs. Microsserviços para o MVP

* **Status:** Aprovada
* **Contexto:** O protótipo legado apresentava fragmentação em 4 repositórios desarticulados, gerando problemas crônicos de sincronização de contratos, CORS, portas conflitantes e custos elevados de infraestrutura.
* **Decisão:** Adotar a arquitetura de **Monólito Modular** em Java 21 / Spring Boot 3.3+, organizando os quatro contextos delimitados (`territory`, `catalog`, `insarcore`, `urbananalysis`) como módulos de pacotes bem definidos com regras estritas de dependência unidirecional validadas via ArchUnit.
* **Consequências:**
  - *Positivas:* Menor custo operacional de infraestrutura (roda em um único contêiner ECS Fargate ou nó Docker local); facilidade de refatoração e tipagem estrita entre módulos; ausência de latência de rede interserviços; transações ACID locais simplificadas.
  - *Negativas:* Exige governança rigorosa de código para impedir violação de encapsulamento entre pacotes (mitigado pelo ArchUnit).

---

### 2. ADR-003 — Mecanismo de Fila e Comunicação Assíncrona no MVP

* **Status:** Aprovada
* **Contexto:** O processamento InSAR leva de 15 a 45 minutos. É mandatório desacoplar a API HTTP da execução do Worker Python. Analisou-se a introdução de RabbitMQ, AWS SQS ou o uso de fila em banco PostgreSQL.
* **Decisão:** Adotar **Fila baseada no PostgreSQL via `SELECT ... FOR UPDATE SKIP LOCKED`** operando sobre a tabela `radar.processing_jobs`.
* **Consequências:**
  - *Positivas:* Elimina a necessidade de subir e manter um broker RabbitMQ dedicado no Docker Compose local e em nuvem; garante consistência atômica perfeita entre a criação da solicitação e o enfileiramento do job (zero risco de *dual-write*); suporte nativo a transações e retentativas.
  - *Negativas:* Throughput limitado a ~1.500 mensagens/segundo (perfeitamente adequado ao volume de dezenas de jobs/dia do MVP). Quando o volume crescer na Fase 2, a migração para AWS SQS será transparente graças à camada de portas e adaptadores.

---

### 3. ADR-004 — Estratégia de Entrega de COGs e Acesso a Arquivos no S3 Privado

* **Status:** Aprovada
* **Contexto:** Os mapas de velocidade e séries temporais são arquivos raster pesados armazenados em um bucket AWS S3 estritamente privado. A interface web precisa renderizar esses dados e inspecionar valores de pixels em milímetros com alta performance.
* **Decisão:** Adotar **Cloud Optimized GeoTIFF (COG)** distribuído diretamente do S3 privado via **URLs pré-assinadas temporárias (Presigned GET)** geradas pelo backend Spring Boot, sendo consumidas nativamente pelo cliente (OpenLayers / geotiff.js) através de requisições HTTP Range Requests.
* **Consequências:**
  - *Positivas:* Custo computacional de nuvem quase nulo (dispensa a manutenção de contêineres dedicados de tile servers como TiTiler); o cliente lê apenas as fatias de bytes visíveis no zoom; preserva o valor numérico flutuante real do pixel em milímetros (ao contrário de tiles estáticos PNG que perdem precisão geodésica).
  - *Negativas:* O cliente web realiza o cômputo da rampa de cores via WebGL/Canvas (suportado com fluidez por navegadores modernos).

---

### 4. ADR-005 — Sistemas de Referência Espacial (WGS 84 vs. SIRGAS 2000 e UTM)

* **Status:** Aprovada
* **Contexto:** APIs de satélite e visualizadores web utilizam o elipsoide WGS 84 (`EPSG:4326`). A legislação cartográfica brasileira (IBGE) exige SIRGAS 2000 (`EPSG:4674`). Cálculos métricos de área exigem projeções conformes (UTM).
* **Decisão:**
  1. Armazenar as geometrias vetoriais nativamente no PostGIS em **WGS 84 (`EPSG:4326`)** para garantir interoperabilidade direta com GeoJSON, Leaflet e satélites da NASA/ESA;
  2. Executar cálculos métricos de área e distância através de projeção UTM local (`EPSG:31985` para Maceió / UTM Zona 25S) usando `ST_Area(ST_Transform(geom, 31985))`;
  3. Disponibilizar exportações oficiais e notas técnicas com coordenadas declaradas em **SIRGAS 2000 (`EPSG:4674`)**, utilizando a transformação nativa do PostGIS sob demanda.
* **Consequências:**
  - *Positivas:* Elimina distorções métricas em cálculos de área e garante conformidade legal com o IBGE sem quebrar a interoperabilidade dos mapas web.

---

### 5. ADR-006 — Gating de Qualidade Científica Desacoplado do Sucesso do Job

* **Status:** Aprovada
* **Contexto:** Em sistemas legados, o status "concluído com sucesso" de um pipeline computacional era erroneamente interpretado como validação científica dos dados.
* **Decisão:** Separar estritamente o ciclo operacional do job (`ProcessingJob`: `PROCESSING_COMPLETED`) do ciclo de validação científica da análise (`InSarAnalysis`: `SCIENTIFIC_EVALUATION_PENDING`). A liberação do produto para visualização depende obrigatoriamente da execução do método de domínio `evaluateQuality()`, que analisa a coerência média ($\bar{\gamma}$) e os resíduos de fechamento de fase do SNAPHU.
* **Consequências:**
  - *Positivas:* Blindagem científica contra entrega de dados decorrelacionados ou com ruídos inaceitáveis; aderência aos princípios fundamentais de geodésia por radar.
  - *Negativas:* Exige um passo de avaliação intermediário no fluxo de trabalho.

---

### 6. ADR-007 — Bounded Context `interferometry` Autônomo no Monólito Modular

* **Status:** Aprovada (Oficializada no Ciclo 5.3.1 / 5.4A — Ver `docs/architecture/cycle-05/10-decisao-arquitetural-modulo-interferometry.md`)
* **Contexto:** A seleção, formação combinatória, validação orbital e qualificação metodológica de pares interferométricos envolve regras de física SAR complexas, máquina de estados com 5 níveis de maturidade (`PairEligibilityStatus`), políticas científicas parametrizadas e adaptadores externos (ASF Baseline API, HyP3 API).
* **Decisão:** Formalizar o módulo `com.ass.br.apiradar.interferometry` como um Bounded Context autônomo de primeira classe no Monólito Modular, atuando como elo isolado e seguro entre a descoberta de cenas (`catalog`) e a execução assíncrona (`insarcore`).
* **Consequências:**
  - *Positivas:* Isola a física de deformação e interferometria da ingestão genérica de cenas; viabiliza testes puros ultrarrápidos sem subida de banco; centraliza salvaguardas de conformidade física e barreira financeira contra disparos indevidos.
  - *Negativas:* Adiciona um módulo específico com arquitetura hexagonal concêntrica no monólito.

---

### 7. Registro Consolidado de Questões Pendentes e Próximos Passos

1. **Confirmação Empírica das Órbitas de Maceió:** Concluída no Ciclo 5.3.1 / 5.4A: confirmada a órbita Track 9 (Descendente), IW1, bursts 018318 e 018319 (janeiro de 2023).
2. **Definição Final do Provedor de Autenticação Corporativa:** O modelo arquitetural prevê autenticação stateless via JWT. Deve-se homologar se a gestão de identidades será feita internamente via banco de dados ou integrada a provedor corporativo externo (ex: Keycloak / AWS Cognito).
3. **Validação do Piloto em Campo:** Programar testes comparativos com os marcos topográficos da CPRM/Braskem assim que os primeiros COGs forem gerados no Ciclo 5.4B / 5.5.
