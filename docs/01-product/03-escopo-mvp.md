# Radar++ — Declaração e Limites de Escopo do MVP

**Documento:** `03-escopo-mvp.md`  
**Ciclo:** Ciclo 1 — Visão do Produto e Definição do MVP  
**Data:** Outubro de 2026  
**Status:** Proposto para Aprovação  
**Autores:** Equipe de Arquitetura de Software e Engenharia de Produto  
**Referências:** [01-visao-produto.md](file:///home/asoares/Documentos/repositories/repo_radar/docs/01-product/01-visao-produto.md), [02-atores-e-personas.md](file:///home/asoares/Documentos/repositories/repo_radar/docs/01-product/02-atores-e-personas.md).

---

## 1. Visão Geral do Escopo do MVP

O primeiro Produto Mínimo Viável (MVP) do Radar++ tem como objetivo estabelecer o **pipeline geocientífico básico de ponta a ponta**: da seleção interativa de uma área no território brasileiro até a visualização e inspeção de um mapa de deslocamento milimétrico InSAR cientificamente válido.

```
[Cadastro AOI] ──► [Busca Cenas S1] ──► [Validação Par] ──► [Job InSAR] ──► [Visualização LOS & γ] ──► [Rastreabilidade]
```

O MVP substitui deliberadamente todos os componentes simulados, distâncias euclidianas e modelos de IA aleatórios auditados no Ciclo 0 por uma arquitetura assíncrona, rastreável e matematicamente fundamentada.

---

## 2. As 8 Capacidades Centrais Aprovadas para o MVP

A tabela abaixo correlaciona as 8 capacidades mandatórias do MVP com seus objetivos de implementação e limites técnicos:

| # | Capacidade Aprovada | Implementação no MVP | Limites e Restrições no MVP |
|---|---|---|---|
| **1** | **Cadastrar uma Área de Interesse (AOI)** | Interface web com desenho de polígonos ou retângulos delimitadores (Bounding Box) sobre mapa interativo Leaflet; persistência da geometria em PostgreSQL/PostGIS com validação topológica. | Área máxima permitida limitada a $2.500 \text{ km}^2$ (equivalente a aprox. 50 km x 50 km) para controle de consumo de memória e tempo de processamento. |
| **2** | **Consultar cenas SAR Sentinel-1** | Consulta federada via API com o catálogo de dados abertos da ESA (Copernicus CDSE) ou NASA ASF, retornando metadados de cenas Sentinel-1 IW SLC sobre a AOI em período especificado. | Suporte exclusivo ao satélite Sentinel-1 (A/C), modo Interferometric Wide (IW), produto Single Look Complex (SLC). |
| **3** | **Selecionar aquisições compatíveis** | Algoritmo que filtra e sugere pares de cenas aptos para interferometria: mesma órbita relativa (track), mesma direção de passagem (ascendente ou descendente), linha de base perpendicular ($B_\perp < 150 \text{ m}$) e linha de base temporal ($B_t \in [6, 48] \text{ dias}$). | Rejeição automática de pares com geometria incompatível que causariam descorrelação geométrica total. |
| **4** | **Solicitar processamento InSAR fundamentado** | Criação de um Job assíncrono de DInSAR diferencial de dois passos (par mestre/escravo + Modelo Digital de Elevação Copernicus DEM 30m para remoção de fase topográfica + filtro Goldstein + unwrapping SNAPHU). | Processamento desacoplado do ciclo de vida da requisição HTTP; executado por worker assíncrono. |
| **5** | **Acompanhar a execução de um job** | Painel visual de acompanhamento de status do processamento (`PENDING`, `DOWNLOADING_SCENES`, `COREGISTERING`, `INTERFEROGRAM`, `UNWRAPPING`, `GEOCODING`, `COMPLETED`, `FAILED`) com estimativa de tempo e logs de erro. | Polling inteligente no frontend via React Query ou WebSocket/SSE simples. |
| **6** | **Visualizar produtos georreferenciados** | Sobreposição no mapa interativo de camadas raster geocodificadas (formato Cloud Optimized GeoTIFF — COG ou tiles piramidais): camada de coerência ($\gamma$) e camada de deslocamento relativo na linha de visada (LOS). | Rampa de cores padronizada e controle de opacidade pelo usuário. |
| **7** | **Examinar deslocamentos LOS e qualidade** | Ferramenta de clique/cursor sobre o mapa para inspecionar em tempo real o valor numérico de deslocamento (em mm ou cm) e o índice de coerência interferométrica ($\gamma \in [0.0, 1.0]$) naquele ponto. | Exibição clara de avisos para pixels com baixa coerência ($\gamma < 0.3$), indicando medição não confiável. |
| **8** | **Consultar histórico e rastreabilidade** | Aba de auditoria de cada análise cadastrada na AOI, contendo Scene IDs das imagens da ESA, datas das passagens, parâmetros orbitais ($B_\perp, B_t$) e link para download dos produtos GeoTIFF/GeoJSON. | Histórico persistido e imutável para assegurar reprodutibilidade científica. |

---

## 3. Funcionalidades Incluídas vs Excluídas do MVP

A delimitação explícita de fronteiras é indispensável para evitar o inchaço de escopo (*scope creep*):

```
+-----------------------------------------------------------------------------------+
|                               MATRIZ DE ESCOPO DO MVP                             |
+--------------------------------------------------+--------------------------------+
|              INCLUÍDO NO MVP (IN-SCOPE)          |   EXCLUÍDO DO MVP (OUT-OF-SCOPE|
+--------------------------------------------------+--------------------------------+
| - Desenho de AOI (retângulo/polígono GeoJSON)   | - Diagnóstico automático de risco|
| - Catálogo de cenas Sentinel-1 IW SLC            | - Análise de séries temporais SBAS|
| - Validação automática de pares interferométricos| - Suporte a satélites ALOS/TerraSAR|
| - Orquestração assíncrona de jobs InSAR          | - Decomposição vetorial 3D (Up/East|
| - Geração de raster de deslocamento LOS e coerência| - Integração com sensores de solo |
| - Visualização em mapa com controle de opacidade | - Módulo de alertas push/SMS/email |
| - Inspeção pontual de valores de deslocamento e γ | - Gerador de relatórios PDF        |
| - Download de produtos gerados (GeoTIFF/GeoJSON) | - Multi-tenancy complexo (RBAC)    |
| - Histórico de análises e metadados orbitais     | - Modelos de Machine Learning      |
+--------------------------------------------------+--------------------------------+
```

### Justificativas Técnicas para as Exclusões:
1. **Sem Diagnóstico Automático de Risco:** Declarar se uma encosta ou barragem está "estável" ou "instável" exige conhecimento de geologia estrutural local, piezometria e histórico de chuvas. No MVP, o Radar++ atua como instrumento de medição física de alta precisão, deixando a interpretação diagnóstica para o engenheiro responsável.
2. **Sem Séries Temporais (PS-InSAR / SBAS):** Processar séries temporais exige o empilhamento de dezenas de cenas SLC (centenas de gigabytes) e resolução de matrizes de inversão de fase pesadas. O DInSAR de par temporal (duas cenas) valida o pipeline completo com uma fração do custo computacional.
3. **Sem Múltiplos Sensores:** Focar no Sentinel-1 garante custo zero de aquisição de cenas e estabilidade de formato (SAFE / IW SLC padronizado).

---

## 4. Priorização de Escopo (Método MoSCoW)

### 4.1. Must Have (Obrigatório para o MVP ser Considerado Válido)
* **MUST-01:** Cadastro e persistência de Áreas de Interesse (AOI) com geometria válida no PostGIS.
* **MUST-02:** Consulta e listagem de cenas Sentinel-1 IW SLC cobrindo a AOI selecionada via API da ESA/ASF.
* **MUST-03:** Verificação de compatibilidade de pares interferométricos (mesmo relative orbit, $B_\perp < 150 \text{ m}$, $B_t \le 48 \text{ dias}$).
* **MUST-04:** Processamento InSAR de 2 cenas gerando mapa de coerência ($\gamma$) e mapa de deslocamento LOS.
* **MUST-05:** Fila assíncrona de execução de jobs com máquina de estados observável (`PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`).
* **MUST-06:** Visualização das camadas raster geocodificadas sobre o mapa Leaflet no frontend React.
* **MUST-07:** Ferramenta de inspeção com exibição do valor de deslocamento e coerência por ponto clicado.
* **MUST-08:** Rastreabilidade completa dos metadados das imagens de entrada persistidos no banco relacional.

### 4.2. Should Have (Importante, mas com Alternativa Manual em Caso de Contingência)
* **SHOULD-01:** Download direto dos produtos GeoTIFF e GeoJSON gerados a partir da interface web.
* **SHOULD-02:** Histórico paginado de todas as análises já executadas na AOI.
* **SHOULD-03:** Filtro de nuvens / máscara de água baseada no Modelo Digital de Elevação para descartar corpos hídricos.

### 4.3. Could Have (Desejável, se Sobrar Tempo Hábil)
* **COULD-01:** Comparação lado a lado (modo split-screen) entre o mapa de coerência e o mapa de deslocamento.
* **COULD-02:** Exportação dos dados pontuais da inspeção para arquivo CSV simples.

### 4.4. Won't Have (Explicitamente Fora do Primeiro MVP)
* **WONT-01:** Laudo automático de estabilidade ("Área Segura / Risco Crítico").
* **WONT-02:** Séries temporais multitemporais (PS-InSAR / SBAS com mais de 2 cenas).
* **WONT-03:** Suporte a dados comerciais (TerraSAR-X, COSMO-SkyMed) ou satélites ópticos.
* **WONT-04:** Envio de alertas automáticos por SMS, WhatsApp ou e-mail.

---

## 5. Dependências Externas Críticas do MVP

O sucesso operacional do MVP depende da estabilidade e disponibilidade de quatro pilares externos:

1. **Catálogo e Distribuição de Cenas (ESA Copernicus CDSE e/ou NASA ASF):**
   * *Risco:* Indisponibilidade de API ou restrição de taxa de transferência de downloads de cenas SLC de 5 GB.
   * *Mitigação:* Utilizar a API do ASF Vertex (Alaska Satellite Facility) como fonte prioritária ou redundante, dada a sua reconhecida estabilidade e espelhamento das cenas Sentinel-1.
2. **Modelo Digital de Elevação (DEM 30m):**
   * *Risco:* Indisponibilidade de DEM preciso sobre a AOI para remoção da fase topográfica.
   * *Mitigação:* Utilizar o Copernicus DEM 30m global (GLO-30) ou SRTM v3 30m, amplamente disponíveis em nuvem (AWS Registry of Open Data).
3. **Motor de Processamento InSAR (ASF HyP3 API vs SNAP GPT Local):**
   * *Risco:* Alto custo de infraestrutura para executar SNAP localmente em contêineres com 16 GB+ de RAM.
   * *Mitigação:* Avaliar o uso da API aberta do HyP3 (ASF) para processamento em nuvem no MVP, mantendo a arquitetura modular para plugar worker local caso necessário.
4. **Infraestrutura AWS (S3):**
   * *Risco:* Custos de armazenamento descontrolados e transferência de dados (egress).
   * *Mitigação:* Armazenar apenas produtos finais (COGs compactados de ~10 MB) no S3 em longo prazo; expurgar cenas brutas SLC (~5 GB) após a conclusão do job.
