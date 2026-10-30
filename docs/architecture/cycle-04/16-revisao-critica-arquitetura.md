# Radar++ — Ciclo 4: Arquitetura Técnica, UML e Contratos
## Documento 16 — Revisão Crítica da Arquitetura e Decisões Fundamentais

**Documento:** `16-revisao-critica-arquitetura.md`  
**Ciclo:** Ciclo 4 — Arquitetura Técnica, UML e Contratos  
**Data:** 24 de Outubro de 2026  
**Status:** Oficial — Entregue para Homologação de Prontidão  
**Responsáveis:** Equipe Multidisciplinar de Arquitetura e Engenharia do Radar++  
**Repositório Oficial:** `api-radar-deformacao` (caminho: `docs/architecture/cycle-04/16-revisao-critica-arquitetura.md`)  
**Referências:** Documentos `01` a `15` em `docs/architecture/cycle-04/` e `docs/domain/cycle-03/`  

---

### 1. Propósito da Revisão Crítica

Esta revisão consolida a análise crítica e o estresse técnico das decisões registradas nos 15 documentos de arquitetura do Ciclo 4. O objetivo é assegurar que a transição para a implementação física (Ciclo 5) ocorra sobre bases técnicas irrefutáveis, fiscalmente controladas e cientificamente blindadas, sem premissas ocultas ou complexidade desnecessária.

---

### 2. Avaliação dos Pilares Arquiteturais Críticos

---

#### 2.1. Qualidade Científica Parametrizável e Rigor Geodésico
* **Fundamentação:** Em sistemas legados de sensoriamento remoto, limiares de coerência e janelas temporais eram tratados como constantes mágicas no código (*hardcoded*).
* **Solução Homologada:**
  - Os parâmetros geodésicos foram desacoplados em **Perfis Metodológicos Parametrizáveis** (`ConservativeUrban`, `StandardInSAR`, `HighCoherenceInfrastructure`);
  - Limiares de aceitação de produto (coerência média $\bar{\gamma} \ge 0{,}40$ e resíduos topológicos de desdobramento de fase SNAPHU $\le 15\%$) são injetados na política `QualityGatingPolicy`;
  - A velocidade na linha de visada ($d_{\text{LOS}}$) é preservada como a grandeza primária soberana em milímetros ($\text{mm}_{\text{LOS}}$). A projeção vertical ($d_{\text{vert}} = d_{\text{LOS}} / \cos\theta$) é estritamente condicional à hipótese física de deformação puramente vertical ($d_{\text{East}} \approx 0, d_{\text{North}} \approx 0$), sendo vedada em encostas, taludes e bordas periféricas tracionadas;
  - Desacoplamento mandatório: o término operacional de um job (`PROCESSING_COMPLETED`) **não torna a análise cientificamente válida**. A validação exige a execução formal de `evaluateQuality()`.

---

#### 2.2. Integração HyP3 Desacoplada e Gestão Orçamentária
* **Fundamentação:** O acoplamento síncrono a serviços de computação em nuvem externos gerava falhas por timeout e cobranças imprevistas de créditos.
* **Solução Homologada:**
  - O backend Java não invoca a API do HyP3 de forma bloqueante; ele delega a execução ao **Worker Python desacoplado**, que consome tarefas de forma assíncrona;
  - A orçamentação é rigorosamente calculada antes do disparo, decompondo custos conforme a tabela tarifária oficial da NASA/ASF: tipo de job `INSAR_ISCE_BURST` tarifado por par-burst a 1 crédito, número de interferogramas ($N_{\text{interf}}$), número de bursts interceptados na AOI ($N_{\text{bursts}}$) e resolução de multilooking ($20\,\text{m}$);
  - Idempotência criptográfica com hash MD5 dos pares para evitar reprocessamento acidental de dados idênticos;
  - Isolamento de falhas: se a API externa do ASF oscilar, o backend permanece íntegro e a tarefa é retida na fila.

---

#### 2.3. Fila e Recuperação de Jobs em PostgreSQL (`SKIP LOCKED`)
* **Fundamentação:** Avaliou-se a introdução de corretores de mensagens dedicados (RabbitMQ ou AWS SQS). Concluiu-se que adicionar um broker no MVP introduziria pontos de falha operacionais e custos desproporcionais para o volume inicial de dezenas de jobs/dia.
* **Solução Homologada (ADR-003):**
  - Fila relacional nativa no PostgreSQL 16 utilizando `SELECT ... FROM radar.processing_jobs WHERE state = 'JOB_QUEUED' ORDER BY created_at FOR UPDATE SKIP LOCKED LIMIT 1`;
  - **Atomicidade Transacional Absoluta:** O enfileiramento ocorre no mesmo commit JDBC da criação da solicitação, eliminando o problema do *dual-write*;
  - **Recuperação de Falhas:** Se o worker Python travar ou sofrer reinicialização abrupta (SIGKILL), a transação JDBC/Psycopg3 é revertida (*rollback*) pelo banco e o job retorna automaticamente à fila sem intervenção manual;
  - **Retentativas e Circuit Breaker:** Erros transitórios de rede contam com até 3 retentativas com recuo exponencial (*exponential backoff*). Erros determinísticos de dados movem o job imediatamente para `EXECUTION_FAILED`, gravando a causa raiz técnica para auditoria;
  - **Falhas Parciais SBAS:** Redes com perda pontual de 1 ou 2 interferogramas mantêm o processamento se a conectividade da matriz de tempo for preservada.

---

#### 2.4. Acesso a COGs no S3 Privado e Renderização no Cliente
* **Fundamentação:** Servir arquivos raster geodésicos pesados via backend Spring Boot causava estouro de memória e consumo de banda; por outro lado, renderizar via tile server dinâmico (TiTiler) adicionava custos de contêiner contínuo em nuvem.
* **Solução Homologada (ADR-004):**
  - Produtos raster são formatados em **Cloud Optimized GeoTIFF (COG)** com blocos internos de $256 \times 256$ pixels e pirâmides overviews;
  - O bucket S3 é 100% privado. O backend Spring Boot emite **URLs pré-assinadas (Presigned GET)** temporárias com validade estrita de 15 minutos;
  - O navegador web do usuário (OpenLayers / geotiff.js) executa **HTTP Range Requests** diretamente contra o S3, baixando apenas os bytes do cabeçalho TIFF e as fatias de visualização visíveis na tela;
  - **Preservação do Dado Real:** O pixel retém seu valor numérico flutuante original em milímetros ($\text{mm}_{\text{LOS}}$), permitindo que a rampa de cores seja renderizada via WebGL no navegador e que curvas cinemáticas temporais sejam inspecionadas ao clicar na tela em menos de 500 ms.

---

#### 2.5. Sustentabilidade Financeira e Otimização de Custos (TCO AWS)
* **Fundamentação:** Startups e municípios médios no Brasil não comportam faturas de milhares de dólares em nuvem para pilotos de monitoramento.
* **Solução Homologada:**
  - Adoção de arquitetura serverless/conteinerizada sob demanda no **AWS ECS Fargate** e banco **Amazon RDS PostgreSQL micro/medium**;
  - **Política de Retenção S3 em 3 Níveis:**
    1. *Produtos Finais:* Preservação permanente em S3 Standard / Glacier Instant Retrieval (~5 GB/ano para 10 AOIs = US$ 0,12/mês);
    2. *Produtos Intermediários:* Retenção temporária de 60 dias para permitir re-inversão no MintPy, com expurgo automático via Lifecycle rule;
    3. *Arquivos Efêmeros:* Expurgo automático em 7 dias para cenas SLC brutas (4-8 GB cada) e pastas de scratch.
  - **Custo Operacional Total do MVP:** Projetado em **~ US$ 48,00 / mês**, viabilizando modelos comerciais acessíveis no mercado brasileiro.

---

### 3. Conclusão da Revisão Crítica

A revisão confirma que o desenho técnico do Ciclo 4 resolveu integralmente os gargalos identificados na auditoria do sistema legado, alinhando de ponta a ponta a teoria de Domain-Driven Design com a realidade de infraestrutura em nuvem e geociências. A arquitetura é financeiramente sustentável, tecnicamente elegante e pronta para código.
