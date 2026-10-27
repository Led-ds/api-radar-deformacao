# Radar++ — Especificação de Requisitos Não-Funcionais (RNF)

**Documento:** `06-requisitos-nao-funcionais.md`  
**Ciclo:** Ciclo 1 — Visão do Produto e Definição do MVP  
**Data:** Outubro de 2026  
**Status:** Proposto para Aprovação  
**Autores:** Equipe de Arquitetura de Software e Engenharia de Produto  
**Referências:** [01-visao-produto.md](file:///home/asoares/Documentos/repositories/repo_radar/docs/01-product/01-visao-produto.md), [03-escopo-mvp.md](file:///home/asoares/Documentos/repositories/repo_radar/docs/01-product/03-escopo-mvp.md), Documentos de auditoria ([07-integracoes.md](file:///home/asoares/Documentos/repositories/repo_radar/docs/audit/07-integracoes.md), [08-problemas-e-riscos.md](file:///home/asoares/Documentos/repositories/repo_radar/docs/audit/08-problemas-e-riscos.md)).

---

## 1. Princípios de Engenharia e Rigor Métrico

> [!IMPORTANT]
> Em estrita conformidade com as diretrizes do Ciclo 1, **nenhuma meta quantitativa é inventada sem fundamentação operacional**. Onde métricas exatas dependerem de testes de carga ou definição formal de SLA com o cliente final, o requisito registra a premissa de engenharia e declara formalmente a pendência de calibração métrica.

---

## 2. Especificação dos Requisitos Não-Funcionais

### 2.1. Segurança e Proteção de Dados

#### RNF-001 — Gestão Estrita de Segredos e Variáveis de Ambiente
* **Categoria:** Segurança
* **Descrição:** Nenhuma credencial de acesso (chaves da AWS, senhas de banco de dados, tokens de APIs da ESA ou tokens do GitHub) pode estar hardcoded no código-fonte, gravada em arquivos versionados (como `application.properties` ou `package.json`) ou embutida em URLs de repositórios Git.
* **Critério de Verificação:**
  1. Varredura automatizada com ferramentas de detecção de segredos (ex.: Gitleaks / TruffleHog) no pipeline de CI com aprovação bloqueante (0 segredos permitidos).
  2. Todas as credenciais locais devem ser injetadas via variáveis de ambiente do sistema operacional ou arquivo `.env` estritamente ignorado pelo `.gitignore`.

#### RNF-002 — Armazenamento em Nuvem Privado com Acesso Pré-Assinado
* **Categoria:** Segurança
* **Descrição:** Todos os buckets Amazon S3 utilizados para armazenamento de arquivos científicos devem ter a política *Block Public Access* ativada em nível de bucket e conta. O acesso aos arquivos pelo navegador deve ser concedido unicamente através de URLs pré-assinadas (*Presigned URLs*) de curta duração.
* **Critério de Verificação:** Tentativas de acesso direto às URIs S3 sem assinatura devem retornar invariavelmente `HTTP 403 Forbidden`. O tempo de expiração das presigned URLs não deve ultrapassar 60 minutos.

#### RNF-003 — Proteção de Endpoints e Controle de Acesso
* **Categoria:** Segurança
* **Descrição:** A API REST deve implementar proteção contra ataques comuns da web (CORS configurado para origens legítimas, cabeçalhos de segurança via Spring Security, proteção contra injeção SQL via Hibernate/JPA e sanitização de entradas GeoJSON).
* **Meta Quantitativa / Pendência:**
  * *Premissa:* Para o primeiro MVP de desenvolvimento local, a autenticação básica via token de sessão ou JWT simples é suficiente.
  * *Pendência P-01:* A definição da política de segurança corporativa (integração com SSO corporativo, Keycloak ou OAuth2/OIDC) está registrada como pendência no documento [10-decisoes-pendentes.md](file:///home/asoares/Documentos/repositories/repo_radar/docs/01-product/10-decisoes-pendentes.md).

---

### 2.2. Confiabilidade e Resiliência

#### RNF-004 — Desacoplamento e Resiliência de Processamento Assíncrono
* **Categoria:** Confiabilidade
* **Descrição:** Falhas temporárias de rede em APIs externas (como quedas momentâneas da ESA Copernicus ou instabilidades na AWS) não podem derrubar o servidor backend nem travar threads do container web.
* **Critério de Verificação:**
  1. Toda chamada para serviços externos pesados deve possuir timeout explícito configurado (conexão e leitura).
  2. O worker de processamento deve implementar política de retentativa exponencial (*exponential backoff*) limitada a 3 tentativas antes de marcar o job como `FAILED`.

#### RNF-005 — Idempotência e Integridade de Jobs
* **Categoria:** Confiabilidade
* **Descrição:** A submissão duplicada da mesma solicitação de processamento InSAR (mesma AOI e mesmo par de cenas) não deve gerar execuções concorrentes duplicadas que gerem custos redundantes na nuvem.
* **Critério de Verificação:** O backend deve calcular um hash determinístico da requisição (`hash(aoiId, masterSceneId, slaveSceneId, algorithmVersion)`) e rejeitar submissões concorrentes idênticas enquanto uma estiver em andamento.

---

### 2.3. Desempenho e Eficiência

#### RNF-006 — Latência das Operações Transacionais da API REST
* **Categoria:** Desempenho
* **Descrição:** Todas as rotas síncronas de consulta e cadastro (cadastro de AOI, consulta de cenas catalogadas, listagem de jobs e consulta de status) devem responder com alta agilidade, delegando computação pesada ao worker assíncrono.
* **Meta Fundamentada:** 95% das requisições síncronas (`GET /api/aois`, `POST /api/aois`, `GET /api/jobs/{id}/status`) devem responder em menos de **800 ms** sob carga de desenvolvimento local.
* *Pendência P-02:* A meta formal de tempo total de processamento do pipeline InSAR (tempo de execução do job pelo worker) depende da escolha do motor (HyP3 vs SNAP local) e será calibrada nos testes do Ciclo 4.

#### RNF-007 — Performance de Renderização Geoespacial no Frontend
* **Categoria:** Desempenho
* **Descrição:** A renderização de camadas de deformação sobre o mapa Leaflet no navegador não pode congelar a interface do usuário durante a navegação (pan/zoom).
* **Critério de Verificação:** O uso do formato Cloud Optimized GeoTIFF (COG) deve viabilizar requisições HTTP Range parciais, baixando apenas a resolução e a área necessárias para a visão atual da tela, garantindo taxa de quadros fluida durante a navegação.

---

### 2.4. Armazenamento e Ciclo de Vida de Dados

#### RNF-008 — Gestão de Volume e Eliminação de Imagens Brutas SLC
* **Categoria:** Armazenamento e Custos
* **Descrição:** Como arquivos de radar SLC possuem de 4 GB a 8 GB por cena, o sistema não deve reter arquivos brutos no storage permanente após o término do processamento.
* **Critério de Verificação:**
  1. O banco de dados relacional (PostgreSQL) deve armazenar apenas metadados textuais, parâmetros orbitais e geometrias PostGIS; é expressamente proibido armazenar bytes de imagem via colunas `@Lob oid` ou base64.
  2. O worker deve expurgar os arquivos brutos temporários do disco local ou bucket de staging imediatamente após a geração e upload dos produtos finais COG.
  3. Os produtos finais salvos no S3 (COG de deslocamento e COG de coerência) devem ocupar em média menos de **25 MB** por análise.

---

### 2.5. Observabilidade e Rastreabilidade

#### RNF-009 — Estruturação de Logs e Telemetria de Jobs
* **Categoria:** Observabilidade
* **Descrição:** Todas as transições de estado do processamento científico e erros operacionais devem gerar logs estruturados em formato JSON com correlação de `jobId` e `traceId`.
* **Critério de Verificação:** Em caso de falha de um job, deve ser possível inspecionar o log exato da etapa científica que falhou (ex.: erro no cálculo de órbitas pelo SNAP ou falta de memória no SNAPHU) diretamente pelo histórico de auditoria.

#### RNF-010 — Reprodutibilidade e Imutabilidade Científica
* **Categoria:** Rastreabilidade
* **Descrição:** Qualquer análise concluída deve manter registro imutável que permita a qualquer tempo reproduzir os mesmos resultados numéricos.
* **Critério de Verificação:** Os metadados persistidos devem registrar a versão exata do software/pipeline processador, os IDs completos das cenas orbitais e os parâmetros de filtragem adotados. Nenhuma atualização de registro (`UPDATE`) é permitida sobre análises finalizadas.

---

### 2.6. Manutenibilidade e Arquitetura de Software

#### RNF-011 — Arquitetura Modular e Isolamento de Domínio
* **Categoria:** Manutenibilidade
* **Descrição:** A base de código do backend deve ser organizada segundo a estratégia de **Monólito Modular** com princípios de Clean Architecture. Os módulos de negócio (`acquisition`, `processing`, `monitoring`, `presentation`) devem possuir fronteiras explícitas, sem vazamento de detalhes de persistência ou de clientes HTTP para o núcleo do domínio.
* **Critério de Verificação:** A injeção de dependências deve respeitar a inversão de controle: o domínio define interfaces (portas) e a infraestrutura implementa os adaptadores (clientes Feign, S3, repositories JPA).

#### RNF-012 — Ambiente de Desenvolvimento Reprodutível (Conteinerização)
* **Categoria:** Manutenibilidade
* **Descrição:** Todo o ambiente necessário para execução e teste do Radar++ localmente (PostgreSQL com extensão PostGIS, MinIO simulando AWS S3 e serviços auxiliares) deve ser provisionável via comando único de orquestração.
* **Critério de Verificação:** Um desenvolvedor deve ser capaz de subir a infraestrutura completa local através de `docker compose up -d` sem necessidade de configurações manuais adicionais no sistema operacional.
