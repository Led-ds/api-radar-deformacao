# Radar++ — Critérios de Aceite e Sucesso do MVP

**Documento:** `09-criterios-sucesso-mvp.md`  
**Ciclo:** Ciclo 1 — Visão do Produto e Definição do MVP  
**Data:** Outubro de 2026  
**Status:** Proposto para Aprovação  
**Autores:** Equipe de Arquitetura de Software e Engenharia de Produto  
**Referências:** [01-visao-produto.md](file:///home/asoares/Documentos/repositories/repo_radar/docs/01-product/01-visao-produto.md), [03-escopo-mvp.md](file:///home/asoares/Documentos/repositories/repo_radar/docs/01-product/03-escopo-mvp.md), [05-requisitos-funcionais.md](file:///home/asoares/Documentos/repositories/repo_radar/docs/01-product/05-requisitos-funcionais.md), [06-requisitos-nao-funcionais.md](file:///home/asoares/Documentos/repositories/repo_radar/docs/01-product/06-requisitos-nao-funcionais.md).

---

## 1. Visão Geral dos Critérios de Sucesso

O MVP do Radar++ será considerado aprovado e aceito quando demonstrar o funcionamento estável, reprodutível e matematicamente correto do fluxo completo de monitoramento em uma área real de teste no território brasileiro.

Os critérios são organizados em três pilares inegociáveis:
* **Pilar 1 — Validade Científica e Geodésica:** Os dados apresentados são derivados de interferometria física real de micro-ondas.
* **Pilar 2 — Confiabilidade Técnica e Arquitetural:** O sistema é seguro, assíncrono, conteinerizado e testável.
* **Pilar 3 — Usabilidade e Operação Funcional:** A interface web atende com agilidade e clareza aos fluxos das personas.

---

## 2. Matriz de Critérios Verificáveis de Aceite

### 2.1. Critérios de Sucesso Científico e Metodológico (Geociências)

| ID | Critério Científico | Método de Verificação | Condição de Sucesso |
|---|---|---|---|
| **CS-CIEN-01** | **Processamento InSAR Real** | Execução de par interferométrico Sentinel-1 IW SLC sobre área conhecida (ex.: região metropolitana de Maceió/AL ou Quadrilátero Ferrífero/MG). | O interferograma diferencial é gerado com subtração de fase topográfica via DEM 30m e unwrapping SNAPHU convergido. |
| **CS-CIEN-02** | **Deslocamento LOS em Escala Métrica** | Inspeção dos valores nos rasters de saída gerados pelo worker. | Os valores de pixel representam deslocamento na Linha de Visada (LOS) expressos em milímetros ($\text{mm}$), derivados da fase ($\Delta\phi$), e **nunca** de distâncias euclidianas. |
| **CS-CIEN-03** | **Índice de Coerência Interferométrica ($\gamma$)** | Leitura do raster de coerência associado. | O raster de coerência contém valores numéricos estritamente no intervalo $[0.00, 1.00]$, permitindo discriminar feições de alta e baixa estabilidade temporal. |
| **CS-CIEN-04** | **Expurgo de Pseudo-Cálculos** | Análise estática do código-fonte e revisão de commits. | Nenhuma linha de código realiza cálculo de deformação por fórmula de Haversine ou predições por modelos de IA treinados sobre ruído sintético. |
| **CS-CIEN-05** | **Alinhamento Cartográfico Rigoroso** | Sobreposição do produto geocodificado (EPSG:4326) no mapa contra feições urbanas/rodoviárias do OpenStreetMap. | O desvio espacial entre as feições do raster InSAR e os vetores de referência do mapa de fundo é submétrico ou compatível com a resolução de pixel do Sentinel-1 (~14m x 14m). |

---

### 2.2. Critérios de Sucesso Técnico e de Arquitetura

| ID | Critério Técnico | Método de Verificação | Condição de Sucesso |
|---|---|---|---|
| **CS-TEC-01** | **Ausência de Credenciais Expostas** | Execução de ferramenta de SAST / secret detection (Gitleaks / TruffleHog) em todo o repositório. | 0 credenciais ativas, tokens PAT ou senhas gravadas em código ou histórico Git. |
| **CS-TEC-02** | **Armazenamento S3 Estritamente Privado** | Teste de requisição HTTP direta sem credenciais aos artefatos do bucket S3. | Retorno invariavelmente `HTTP 403 Forbidden`. O acesso pelo frontend ocorre exclusivamente por URLs pré-assinadas temporárias de 60 min. |
| **CS-TEC-03** | **Orquestração Assíncrona Não-Bloqueante** | Teste de estresse disparando submissão de job. | O endpoint `POST /api/jobs` responde com `HTTP 202 Accepted` em menos de **1.000 ms**, mantendo a thread HTTP liberada. |
| **CS-TEC-04** | **Ambiente de Desenvolvimento Reproduzível** | Clonagem do repositório em uma máquina limpa e execução de comando de subida. | O comando `docker compose up -d` inicializa o PostgreSQL com PostGIS, o MinIO (S3 local) e as dependências auxiliares sem intervenção manual. |
| **CS-TEC-05** | **Higiene de Banco de Dados Relacional** | Inspeção do schema do banco de dados relacional. | O schema do PostgreSQL não contém tabelas ou colunas armazenando arquivos binários de imagem (`@Lob`, `bytea` ou `oid`). Imagens residem exclusivamente no storage de objetos. |
| **CS-TEC-06** | **Testes Automatizados Mínimos** | Execução da suíte de testes com JUnit 5 / Testcontainers / Pytest. | Cobertura de testes unitários superior a **75%** nos componentes de cálculo geométrico, conversores de DTO e máquinas de estado do Job. |

---

### 2.3. Critérios de Sucesso Funcional e Usabilidade (Frontend)

| ID | Critério Funcional | Método de Verificação | Condição de Sucesso |
|---|---|---|---|
| **CS-FUNC-01** | **Delimitação Intuitiva de AOI** | Teste de usabilidade: usuário desenha um polígono livre sobre a área da estrutura. | O polígono é salvo e renderizado no mapa em menos de 2 segundos, com feedback visual imediato da área em $\text{km}^2$. |
| **CS-FUNC-02** | **Descoberta de Cenas e Validação de Par** | Consulta de catálogo com intervalo de datas. | O sistema lista as cenas reais Sentinel-1 agrupadas por Track e bloqueia pares com $|B_\perp| > 150 \text{ m}$ ou $B_t > 48 \text{ dias}$. |
| **CS-FUNC-03** | **Acompanhamento de Job sem Page Reload** | Submissão de análise e monitoramento visual. | A interface exibe a transição de status em tempo real via polling reativo sem disparar `window.location.reload()` ou piscar a tela. |
| **CS-FUNC-04** | **Inspeção Pontual e Rastreabilidade** | Clique sobre qualquer pixel da camada raster renderizada. | Abertura de popup exibindo o valor exato em $\text{mm}$, a coerência ($\gamma$), os IDs das duas cenas satelitais utilizadas e a data da análise. |
| **CS-FUNC-05** | **Download de Produtos Padronizados** | Clique no botão "Baixar Produtos" e abertura do arquivo ZIP baixado no software QGIS. | Os arquivos GeoTIFF abrem perfeitamente georreferenciados no QGIS sem necessidade de correção manual de projeção ou coordenadas. |

---

## 3. Protocolo de Homologação Final do MVP

Para declarar o MVP oficialmente concluído e apto para transição para o Ciclo 7 (Evolução Funcional do Produto), a seguinte bateria de homologação deverá ser executada:

1. **Teste Piloto Controlado:**
   * Escolher uma área piloto nacional com subsidência historicamente documentada (ex.: bairro do Pinheiro em Maceió/AL ou minas de ferro em Minas Gerais).
   * Processar um par interferométrico Sentinel-1 de 12 dias em época de estiagem.
   * Confrontar o mapa de deformação gerado pelo Radar++ com os relatórios públicos do Serviço Geológico do Brasil (SGB/CPRM) ou literatura científica.
2. **Auditoria de Código e Segurança:**
   * Aprovação do relatório de análise estática SonarQube e auditoria de segredos Gitleaks.
3. **Assinatura Técnica:**
   * Emissão de parecer de conformidade assinado pelo Arquiteto de Software e pelo Especialista em Sensoriamento Remoto.
