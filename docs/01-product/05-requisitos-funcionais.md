# Radar++ — Especificação de Requisitos Funcionais (RF)

**Documento:** `05-requisitos-funcionais.md`  
**Ciclo:** Ciclo 1 — Visão do Produto e Definição do MVP  
**Data:** Outubro de 2026  
**Status:** Proposto para Aprovação  
**Autores:** Equipe de Arquitetura de Software e Engenharia de Produto  
**Referências:** [03-escopo-mvp.md](file:///home/asoares/Documentos/repositories/repo_radar/docs/01-product/03-escopo-mvp.md), [04-jornadas-usuario.md](file:///home/asoares/Documentos/repositories/repo_radar/docs/01-product/04-jornadas-usuario.md), [07-regras-negocio.md](file:///home/asoares/Documentos/repositories/repo_radar/docs/01-product/07-regras-negocio.md).

---

## 1. Matriz Resumida de Requisitos Funcionais

| Identificador | Nome do Requisito | Módulo Responsável | Prioridade | Regras de Negócio |
|---|---|---|:---:|:---:|
| **RF-001** | Cadastro e Validação de Área de Interesse (AOI) | `monitoring` | MUST | RN-001 |
| **RF-002** | Gestão e Listagem de AOIs Cadastradas | `monitoring` | MUST | — |
| **RF-003** | Descoberta e Consulta de Cenas SAR Sentinel-1 | `acquisition` | MUST | RN-002 |
| **RF-004** | Filtragem de Cenas por Parâmetros Orbitais | `acquisition` | MUST | RN-002, RN-003 |
| **RF-005** | Recomendação e Validação de Pares Interferométricos | `processing` | MUST | RN-003, RN-004, RN-005 |
| **RF-006** | Submissão e Enfileiramento de Job InSAR | `processing` | MUST | RN-006, RN-008 |
| **RF-007** | Execução Assíncrona do Pipeline DInSAR | `processing` | MUST | RN-006, RN-007, RN-009 |
| **RF-008** | Acompanhamento de Status e Progresso do Job | `processing` | MUST | — |
| **RF-009** | Cancelamento e Retentativa de Jobs | `processing` | SHOULD | — |
| **RF-010** | Armazenamento de Artefatos COG no S3 com Acesso Privado | `infrastructure` | MUST | RN-009, RN-010, RN-011 |
| **RF-011** | Visualização de Camadas Raster InSAR no Mapa | `presentation` | MUST | RN-007, RN-011 |
| **RF-012** | Inspeção Pontual de Deslocamento LOS e Coerência | `presentation` | MUST | RN-007, RN-008 |
| **RF-013** | Controle de Camadas (Opacidade e Rampa de Cores) | `presentation` | SHOULD | — |
| **RF-014** | Consulta a Histórico e Rastreabilidade Científica | `monitoring` | MUST | RN-009 |
| **RF-015** | Download de Produtos Científicos (GeoTIFF e JSON) | `presentation` | SHOULD | RN-011 |

---

## 2. Especificação Detalhada dos Requisitos

### RF-001 — Cadastro e Validação de Área de Interesse (AOI)
* **Descrição:** O sistema deve permitir ao usuário demarcar uma Área de Interesse (AOI) através de desenho de polígono livre ou retângulo delimitador (Bounding Box) no mapa web interativo, informando um nome identificador.
* **Motivação:** A delimitação geográfica é a base para busca de cenas em catálogos espaciais e corte dos rasters de deformação.
* **Prioridade:** **MUST HAVE**
* **Entradas:** Nome da AOI (string), Geometria GeoJSON (tipo `Polygon` com coordenadas WGS84 / EPSG:4326).
* **Saídas:** Entidade AOI persistida com ID único, área calculada em $\text{km}^2$ e envelope geográfico.
* **Critérios de Aceite:**
  1. A geometria deve ser validada topologicamente via PostGIS (`ST_IsValid`); geometrias com auto-interseção devem ser rejeitadas com erro explicativo 400.
  2. A área delimitada não pode ultrapassar $2.500 \text{ km}^2$ (conforme RN-001). Caso exceda, o sistema deve recusar o cadastro informando o excesso de dimensão.
  3. O polígono cadastrado deve ser renderizado imediatamente no mapa com contorno destacado.

---

### RF-002 — Gestão e Listagem de AOIs Cadastradas
* **Descrição:** O sistema deve disponibilizar um painel lateral listando todas as AOIs cadastradas, com opção de pesquisa por nome e seleção rápida para centralizar o mapa.
* **Motivação:** Facilitar a alternância entre diferentes frentes de monitoramento de campo sem redigitação de coordenadas.
* **Prioridade:** **MUST HAVE**
* **Entradas:** Nenhuma (ou termo de busca textual).
* **Saídas:** Lista paginada de AOIs com nome, data de criação, área ($\text{km}^2$) e contagem de análises vinculadas.
* **Critérios de Aceite:**
  1. Ao clicar em uma AOI da lista, o mapa deve aplicar transição suave de zoom e enquadramento (*fitBounds*) sobre a geometria correspondente.
  2. Permitir exclusão de AOI desde que não possua jobs em processamento ativo.

---

### RF-003 — Descoberta e Consulta de Cenas SAR Sentinel-1
* **Descrição:** O sistema deve consultar acervos satelitais abertos (Copernicus CDSE / NASA ASF) para identificar todas as cenas Sentinel-1 IW SLC que interceptam a geometria da AOI em um período de tempo informado.
* **Motivação:** Automatizar a busca manual que os analistas realizam em portais externos dispersos.
* **Prioridade:** **MUST HAVE**
* **Entradas:** ID da AOI, Data de Início, Data de Fim.
* **Saídas:** Lista estruturada de cenas encontradas com metadados orbitais.
* **Critérios de Aceite:**
  1. A consulta deve retornar apenas cenas do sensor Sentinel-1 (A/C) no modo Interferometric Wide (IW) e nível de produto Single Look Complex (SLC).
  2. Cada cena deve exibir obrigatoriamente: Scene ID, Data/Hora da Passagem, Órbita Relativa (Track), Direção da Passagem (Ascending / Descending) e Polarização (ex.: VV+VH).

---

### RF-004 — Filtragem de Cenas por Parâmetros Orbitais
* **Descrição:** A interface deve permitir filtrar as cenas descobertas por direção de passagem (Ascendente/Descendente), órbita relativa (Track) e polarização.
* **Motivação:** A interferometria SAR só é fisicamente viável entre cenas que compartilham exatamente a mesma geometria orbital.
* **Prioridade:** **MUST HAVE**
* **Entradas:** Filtros selecionados pelo usuário na UI.
* **Saídas:** Cenas filtradas e agrupadas por Track orbital.
* **Critérios de Aceite:**
  1. Cenas de tracks diferentes não podem ser agrupadas no mesmo conjunto de seleção de pares.

---

### RF-005 — Recomendação e Validação de Pares Interferométricos
* **Descrição:** O sistema deve avaliar pares de cenas selecionadas e indicar se cumprem as restrições físicas de linha de base para gerar um interferograma coerente.
* **Motivação:** Evitar desperdício de processamento computacional em pares que sofrerão descorrelação geométrica total.
* **Prioridade:** **MUST HAVE**
* **Entradas:** Cena Mestre (Master) e Cena Escravo (Slave).
* **Saídas:** Indicador de aptidão (Válido / Inválido) com cálculo de Linha de Base Perpendicular ($B_\perp$) e Linha de Base Temporal ($B_t$).
* **Critérios de Aceite:**
  1. O sistema deve validar que a linha de base perpendicular seja $|B_\perp| < 150 \text{ m}$ (conforme RN-004).
  2. O sistema deve validar que a linha de base temporal seja $B_t \in [6, 48] \text{ dias}$ (conforme RN-005).
  3. Caso o par selecionado viole esses limites, a interface deve exibir alerta com o valor discrepante e bloquear o disparo do processamento.

---

### RF-006 — Submissão e Enfileiramento de Job InSAR
* **Descrição:** O usuário confirma o par validado e submete a solicitação de processamento; o backend registra um Job e responde imediatamente com código HTTP 202.
* **Motivação:** Processamentos científicos InSAR levam de 10 a 40 minutos; o sistema deve ser estritamente não-bloqueante.
* **Prioridade:** **MUST HAVE**
* **Entradas:** `aoiId`, `masterSceneId`, `slaveSceneId`.
* **Saídas:** `202 Accepted` com objeto Job (ID, status `PENDING`, timestamp de criação).
* **Critérios de Aceite:**
  1. A resposta da API REST não deve demorar mais que 1 segundo.
  2. O Job deve ser persistido no PostgreSQL e publicado na fila de processamento assíncrono.

---

### RF-007 — Execução Assíncrona do Pipeline DInSAR
* **Descrição:** O worker científico em segundo plano deve processar as cenas, gerar o interferograma diferencial, subtrair a fase topográfica usando DEM Copernicus 30m, aplicar filtro Goldstein, realizar o unwrapping via SNAPHU e geocodificar o deslocamento LOS em milímetros.
* **Motivação:** Núcleo científico real do Radar++.
* **Prioridade:** **MUST HAVE**
* **Entradas:** Cenas Sentinel-1 SLC, Modelo Digital de Elevação (DEM GLO-30).
* **Saídas:** Dois rasters georreferenciados (Cloud Optimized GeoTIFF — COG): raster de deslocamento LOS (mm) e raster de coerência interferométrica ($\gamma \in [0, 1]$).
* **Critérios de Aceite:**
  1. A execução deve transicionar pelos estados formais da máquina de estados do job.
  2. Caso o unwrapping falhe por descorrelação, o job deve transicionar para `FAILED` com mensagem descritiva (conforme RN-007).
  3. Não salvar bytes binários pesados no banco de dados relacional (conforme RN-010).

---

### RF-008 — Acompanhamento de Status e Progresso do Job
* **Descrição:** A interface deve exibir um card com o progresso do job ativo, atualizando automaticamente sem recarregar a tela.
* **Motivação:** Transparência operacional e feedback contínuo para o usuário.
* **Prioridade:** **MUST HAVE**
* **Entradas:** `jobId`.
* **Saídas:** Status atual (`PENDING`, `DOWNLOADING_SCENES`, `COREGISTERING`, `INTERFEROGRAM`, `PHASE_UNWRAPPING`, `GEOCODING`, `EXPORTING_COG`, `COMPLETED`, `FAILED`), percentual estimado e tempo decorrido.
* **Critérios de Aceite:**
  1. A UI deve atualizar via React Query polling (intervalo de 5 segundos) sem piscar a tela ou perder a posição do mapa.
  2. Ao atingir `COMPLETED`, um botão "Visualizar Resultados" deve ser exibido.

---

### RF-009 — Cancelamento e Retentativa de Jobs
* **Descrição:** Permitir ao usuário cancelar um job em espera/execução ou retentar a execução de um job que falhou.
* **Motivação:** Evitar desperdício de recursos computacionais e recuperar falhas temporárias de rede.
* **Prioridade:** **SHOULD HAVE**
* **Entradas:** `jobId`, ação (`cancel` ou `retry`).
* **Saídas:** Job atualizado para `CANCELLED` ou novo job enfileirado.
* **Critérios de Aceite:**
  1. Jobs cancelados devem liberar imediatamente os workers computacionais associados.

---

### RF-010 — Armazenamento de Artefatos COG no S3 com Acesso Privado
* **Descrição:** Os rasters gerados devem ser gravados em bucket S3 privado, organizados por hash/ID do job, com acesso concedido exclusivamente por URLs pré-assinadas temporárias.
* **Motivação:** Segurança de dados científicos, conformidade e redução de custos de egress.
* **Prioridade:** **MUST HAVE**
* **Entradas:** Arquivos COG de deslocamento e coerência gerados pelo worker.
* **Saídas:** S3 URI registrada no banco de dados relacional.
* **Critérios de Aceite:**
  1. O bucket S3 não deve possuir nenhuma política de acesso público liberada (*Block Public Access: ON*).
  2. As URLs geradas para o frontend devem expirar em no máximo 60 minutos (conforme RN-011).

---

### RF-011 — Visualização de Camadas Raster InSAR no Mapa
* **Descrição:** O frontend deve renderizar sobre o mapa Leaflet as camadas raster geocodificadas do job selecionado, permitindo alternar entre o mapa de deslocamento LOS e o mapa de coerência ($\gamma$).
* **Motivação:** Visualização geoespacial intuitiva para identificação visual de anomalias no terreno.
* **Prioridade:** **MUST HAVE**
* **Entradas:** Presigned URLs dos arquivos COG.
* **Saídas:** Camada raster sobreposta com posicionamento espacial exato e rampa de cores legendada.
* **Critérios de Aceite:**
  1. A sobreposição deve se alinhar perfeitamente às feições terrestres do OpenStreetMap (sem desvio cartográfico).
  2. A camada deve exibir uma legenda visual clara indicando as faixas métricas (ex.: tons de vermelho para subsidência / afundamento, tons de azul para soerguimento).

---

### RF-012 — Inspeção Pontual de Deslocamento LOS e Coerência
* **Descrição:** Ao clicar sobre qualquer ponto coberto pela camada InSAR no mapa, o sistema deve abrir um popup exibindo o valor exato de deslocamento e o índice de qualidade naquele pixel.
* **Motivação:** Permitir ao analista extrair valores numéricos rigorosos para laudos técnicos.
* **Prioridade:** **MUST HAVE**
* **Entradas:** Clique do usuário (coordenadas Lat/Lon).
* **Saídas:** Popup informativo contendo:
  * Deslocamento LOS (em mm ou cm, com 1 casa decimal).
  * Coerência Interferométrica ($\gamma \in [0.00, 1.00]$).
  * Classificação de Qualidade da Medição (Alta: $\gamma \ge 0.6$, Média: $0.3 \le \gamma < 0.6$, Baixa/Não Confiável: $\gamma < 0.3$).
* **Critérios de Aceite:**
  1. O valor de deslocamento exibido deve ser derivado diretamente do raster interferométrico geocodificado, e **nunca** de distâncias geodésicas de clique do mouse (conforme RN-008).
  2. Pixels com coerência $\gamma < 0.3$ devem exibir aviso explícito: *"Medição sujeita a ruído por perda de coerência temporal"*.

---

### RF-013 — Controle de Camadas (Opacidade e Rampa de Cores)
* **Descrição:** Fornecer controles deslizantes (sliders) para ajuste da transparência/opacidade das camadas e botão de ativação/desativação rápida da sobreposição.
* **Motivação:** Permitir ao analista enxergar as feições de satélite e a imagem cartográfica subjacente para correlacionar a deformação com elementos de engenharia (diques, cavas, rodovias).
* **Prioridade:** **SHOULD HAVE**
* **Entradas:** Valor de opacidade de 0% a 100%.
* **Saídas:** Camada no mapa re-renderizada com a transparência solicitada em tempo real.
* **Critérios de Aceite:**
  1. O ajuste de opacidade deve ocorrer de forma suave sem travamento do navegador (*60 fps*).

---

### RF-014 — Consulta a Histórico e Rastreabilidade Científica
* **Descrição:** O sistema deve manter registro imutável de todas as análises concluídas em cada AOI, permitindo inspecionar todos os metadados de aquisição e parâmetros científicos adotados.
* **Motivação:** Reprodutibilidade científica e auditoria legal em caso de investigações de segurança geotécnica.
* **Prioridade:** **MUST HAVE**
* **Entradas:** `aoiId`.
* **Saídas:** Lista de análises com data de execução, autor, IDs das cenas Sentinel-1, baseline perpendicular ($B_\perp$), baseline temporal ($B_t$), DEM utilizado e versão do pipeline.
* **Critérios de Aceite:**
  1. Uma análise concluída não pode ter seus parâmetros ou rasters editados (imutabilidade conforme RN-009).

---

### RF-015 — Download de Produtos Científicos (GeoTIFF e JSON)
* **Descrição:** A interface deve fornecer um botão de download para exportação dos artefatos finais em formato compactado contendo os GeoTIFFs e o arquivo de metadados JSON.
* **Motivação:** Interoperabilidade com softwares GIS de mercado (QGIS, ArcGIS).
* **Prioridade:** **SHOULD HAVE**
* **Entradas:** `analysisId`.
* **Saídas:** Arquivo ZIP contendo:
  * `displacement_los.tif` (GeoTIFF EPSG:4326 em mm).
  * `coherence.tif` (GeoTIFF EPSG:4326 de 0 a 1).
  * `metadata.json` (Parâmetros completos de rastreabilidade).
* **Critérios de Aceite:**
  1. Os GeoTIFFs baixados devem abrir nativamente no QGIS com coordenadas e projeção espacial perfeitamente alinhadas.
