# Radar++ — Panorama Funcional: O que Já Construímos?

**Documento:** `02-panorama-funcional.md`  
**Data da Auditoria:** Outubro de 2026  
**Status:** Concluído  
**Escopo:** Reconstrução histórica, análise funcional de ponta a ponta e matriz de rastreabilidade do Radar++.

---

## 1. Reconstrução da História Técnica e Funcional

A evolução do Radar++ pode ser dividida em 3 fases distintas identificadas a partir dos históricos Git e arquivos do workspace:

### Fase 1: Exploração Conceitual ALOS-PALSAR & Frontend Angular (Dezembro/2024 - Janeiro/2025)
* O objetivo inicial concentrou-se na utilização de dados de radar de banda L da JAXA (ALOS-2 PALSAR-2), provavelmente motivado pela documentação técnica do sistema **JJ-FAST** (`JJ-FAST_API_UsersGuide_v1_1.pdf`) arquivada no repositório.
* Foi criado o projeto `monitorar-images-alos-palsar` em Angular 18 com o intuito de exibir mapas, integrar com Google Maps (via iframe) e Leaflet, e disparar requisições para envio de coordenadas.
* Nesta fase, o cálculo de deformação introduziu a primeira aproximação conceitualmente incorreta: o cálculo da fórmula de Haversine entre dois pontos geográficos multiplicado por 100 com o comentário de que se tratava de centímetros de deslocamento.

### Fase 2: Construção da PoC Backend Java & Modelo de IA em Python (Fevereiro/2025)
* O backend Java (`api-radar-deformacao`) foi desenvolvido para conectar o banco relacional PostgreSQL, o armazenamento S3 e uma API em Python (`radar_fastapi_py`).
* Diante da complexidade de processar dados brutos do ALOS-2 ou Sentinel-1 diretamente, o fluxo migrou para uma fonte simplificada de imagens: a API aberta da **NASA** (`api.nasa.gov/planetary/earth/assets`). Essa API, no entanto, fornece imagens ópticas multiespectrais (Landsat/MODIS), e não dados interferométricos de radar SAR.
* Foi criado o microsserviço Python `radar_fastapi_py` com um modelo convolucional Keras (`meu_modelo.h5`). No entanto, a falta de um dataset rotulado de interferogramas levou à geração de um script de treino sintético (`train_model.py`) baseado em 100 imagens de ruído aleatório.
* O componente `GeoToolsProcessor.java` foi incorporado para aplicar filtros de imagem e gerar GeoTIFFs, usando operadores JAI e envelopes geográficos artificiais.

### Fase 3: Migração para React & Ajustes de Integração (Fevereiro - Março/2025)
* O frontend Angular foi abandonado em favor de uma aplicação React 18 moderna gerada na plataforma Lovable (`front_react/github-friend-helper`), utilizando Vite, Tailwind CSS, shadcn/ui e React-Leaflet.
* Os desenvolvedores realizaram sucessivos commits de depuração para resolver problemas de portas, CORS (`http://localhost:8081` vs Vite), envio de dados multipart/form-data e visualização de imagens.
* A lógica de cálculo de deslocamento foi portada do Angular para o React: a distância entre o ponto clicado e o ponto anterior foi multiplicada por 100.000 para gerar o campo `deslocamento` em centímetros.
* As rotas de busca de imagens foram fixadas em nomes estáticos (`nasa_original.png` e `fastapi_processada.png`) e apontadas para um ID de deformação fixo no backend (`deformacao-68`), viabilizando a demonstração visual na interface.

---

## 2. Diagnóstico Funcional: O que Existe vs O que Funciona

### 2.1. Funcionalidades com Fluxo Completo (Aparentes na UI)
1. **Interface de Mapa e Captura de Coordenadas:** O usuário clica no mapa Leaflet, as coordenadas (latitude e longitude) são capturadas e preenchem automaticamente os campos do formulário.
2. **Envio e Persistência do Ponto:** O formulário envia `POST /api/analise/deformacao`, o backend persiste a entidade `Deformacao` no PostgreSQL e retorna um DTO com ID.
3. **Listagem e Renderização de Marcadores:** Ao carregar a tela, `GET /api/analise/deformacoes` é disparado e marcadores Leaflet são plotados no mapa.
4. **Exibição de Modal com Imagem:** Ao clicar em um marcador, o popup renderiza imagens que podem ser ampliadas em um modal interativo (`ImageViewer.tsx`).

### 2.2. Funcionalidades com Ruptura ou Dependência de Dados Simulados/Fixos
1. **Medição de Deformação do Terreno (Inexistente):** Não há medição de deformação. O sistema apenas mede a distância geodésica entre cliques consecutivos e a rotula falsamente como "deslocamento do solo em cm".
2. **Aquisição de Imagens SAR (Inexistente):** O sistema não se comunica com a ESA Copernicus (Sentinel-1) nem com a NASA/ASF (Alaska Satellite Facility). A chamada para a NASA consome imagens ópticas de satélites de observação da Terra (Landsat) ou falha por falta de cobertura temporal.
3. **Processamento Interferométrico (Inexistente):** Não existe geração de interferograma, remoção de fase topográfica, nem unwrapping. O processamento limita-se a um filtro de convolução JAI 3x3 no Java e redimensionamento para 64x64 pixels no Python.
4. **Avaliação de Risco por Inteligência Artificial (Simulada/Ruído):** A classificação "Stable"/"Unstable" é gerada por uma rede convolucional que foi treinada sobre matrizes de números aleatórios (`np.random.random`), tornando o resultado estatisticamente equivalente a um sorteio de cara ou coroa.
5. **Rastreabilidade de Imagens por Ponto (Fixa/Mock):** A recuperação de imagens no S3 ignora o ID da deformação selecionada e busca sempre `imagens/deformacao-68/nasa_original.png` no bucket.

### 2.3. Funcionalidades Abandonadas ou Descontinuadas
1. **Frontend Angular (`monitorar-images-alos-palsar`):** Completamente abandonado após o commit inicial em Dezembro de 2024.
2. **Caso de Uso `ProcessarImagemSAR.java`:** Criado na camada de aplicação do backend Java com uma rota exposta (`POST /api/analise/processar-imagem/{imagemRadarId}`), mas com seu corpo 100% comentado.
3. **Consulta de Terremotos USGS (`UsgsEarthquakeClient.java`):** Interface Feign declarada, mas jamais invocada no sistema.
4. **Consulta Direta ALOS-PALSAR (`AlosPalsarClient.java`):** Interface Feign apontando para URL inexistente, sem nenhuma classe consumidora.

---

## 3. Matriz de Panorama Funcional

A tabela a seguir apresenta o status minucioso de cada capacidade identificada no Radar++:

| Funcionalidade | Projeto Responsável | Situação | Evidência no Código | Dependências | Observações |
|---|---|---|---|---|---|
| **Seleção de região geográfica no mapa** | `front_react` | Implementada, mas não validada em execução | `MonitoringMap.tsx:21-28`, `handleLocationClick:81-87` | Leaflet, OpenStreetMap | Captura clique pontual; não suporta seleção de polígono delimitador (Bounding Box / ROI). |
| **Cálculo de deslocamento / deformação** | `front_react` e `api-radar-deformacao` | Simulada/mock | `MonitoringMap.tsx:44-59`, `CalculoGeoespacial.java:47-59` | Nenhuma | **Erro conceitual grave:** calcula distância Haversine em km entre cliques do mouse e multiplica por 100.000 chamando de "deslocamento em cm". |
| **Cadastro e persistência de deformações** | `api-radar-deformacao` | Implementada, mas não validada em execução | `DeformacaoController.java:22-26`, `DeformacaoService.java:18-28` | PostgreSQL, Spring Data JPA | Salva latitude, longitude, deslocamento e risco no banco de dados relacional. |
| **Listagem de histórico de monitoramento** | `front_react` e `api-radar-deformacao` | Implementada, mas não validada em execução | `MonitoringMap.tsx:61-71`, `DeformacaoController.java:28-32` | API REST `/analise/deformacoes` | Retorna todos os registros cadastrados sem paginação, filtros ou controle de permissão. |
| **Consulta e obtenção de imagens ESA Copernicus** | Nenhum | Não encontrada | Nenhuma referência no código ou POM | Copernicus Data Space Ecosystem / SciHub | Inexistente. Não há credenciais, endpoints ou scripts de consulta a dados Sentinel-1. |
| **Consulta e obtenção de imagens NASA / ASF (SAR)** | Nenhum | Não encontrada | Nenhuma referência no código ou POM | NASA Earthdata / ASF Vertex | Inexistente. A única integração existente é com o serviço óptico da NASA. |
| **Consulta de ativos ópticos NASA Earth** | `api-radar-deformacao` | Parcialmente implementada | `NasaOpenDataClient.java:11-16`, `AnaliseService.java:45-61` | API externa NASA (`api.nasa.gov`), API Key | Busca imagens óticas pelo endpoint `/planetary/earth/assets`. Não retorna dados SAR. |
| **Consulta de dados JAXA ALOS-PALSAR** | `api-radar-deformacao` | Apenas planejada ou documentada | `AlosPalsarClient.java:7-12`, `JJ-FAST_API_UsersGuide_v1_1.pdf` | Nenhuma | URL configurada é fictícia (`https://api.alospalsar.jaxa.jp`). Cliente não é injetado em nenhum serviço. |
| **Consulta de eventos sísmicos USGS** | `api-radar-deformacao` | Apenas planejada ou documentada | `UsgsEarthquakeClient.java:9-19` | USGS FDSN Web Service | Interface Feign declarada, mas não consumida por nenhuma regra de negócio. |
| **Reprojeção e georreferenciamento de raster** | `api-radar-deformacao` | Parcialmente implementada | `GeoToolsProcessor.java:45-47`, `corrigirProjecao:75-88` | GeoTools 26.0, EPSG:4326 | Cria envelope fictício usando o deslocamento como raio e tenta resample para WGS84. |
| **Filtragem e suavização convolucional de imagem** | `api-radar-deformacao` | Parcialmente implementada | `GeoToolsProcessor.java:90-139` | JAI 1.1.3 (`KernelJAI`, `JAI.create`) | Implementa cadeia de fallback com try/catch aninhados tentando operações JAI diversas. |
| **Armazenamento de imagens brutas e processadas no S3** | `api-radar-deformacao` | Parcialmente implementada | `StorageService.java:45-59`, `ImagemRadarService.java:65-70` | AWS S3, Bucket `radar-imagesbucket` | Realiza upload com chave estruturada (`imagens/deformacao-{id}/...`), mas requer credenciais ativas. |
| **Persistência de imagens brutas em banco de dados** | `api-radar-deformacao` | Implementada, mas não validada em execução | `ImagemRadar.java:25-27` (`@Lob oid`) | PostgreSQL Large Objects | Salva os bytes completos da imagem dentro do PostgreSQL (duplicação ineficiente com o S3). |
| **Recuperação de imagens do S3 para exibição** | `api-radar-deformacao` e `front_react` | Simulada/mock | `StorageService.java:61-79`, `mapService.ts:47-48` | AWS S3 | **Falha de projeto:** backend fixa a busca na pasta `imagens/deformacao-68/` e frontend usa nomes fixos `nasa_original.png` e `fastapi_processada.png`. |
| **Processamento e análise via microsserviço Python** | `radar_fastapi_py` | Parcialmente implementada | `analyze.py:36-56`, `FastAPIClientPy.java:15-19` | FastAPI, Uvicorn (porta 8082) | Recebe imagem via multipart, redimensiona para 64x64 e envia para inferência Keras. |
| **Classificação de risco de terreno por IA** | `radar_fastapi_py` | Simulada/mock | `train_model.py:28-40`, `analyze.py:48-50` | TensorFlow / Keras (`meu_modelo.h5`) | Rede convolucional treinada com 100 imagens de ruído sintético geradas com `np.random.random`. |
| **Métricas de desempenho do modelo de IA** | `radar_fastapi_py` | Parcialmente implementada (com erro em runtime) | `analyze.py:63-71` (`/metrics/`) | Keras | Dispara `TypeError` por chamar `model.evaluate()` sem passar dados e rótulos de teste. |
| **Visualização de marcadores e imagens no mapa** | `front_react` | Implementada, mas não validada em execução | `MonitoringMap.tsx:123-174`, `ImageViewer.tsx:13-28` | React-Leaflet, Radix UI Dialog | Renderiza marcadores com popups contendo miniaturas de imagens e modal de zoom. |
| **Execução assíncrona de processamento SAR** | `api-radar-deformacao` | Apenas planejada ou documentada | `ProcessarImagemSAR.java:13-15`, `DeformacaoController.java:34-38` | Nenhuma | Caso de uso vazio com método executável 100% comentado. |
| **Autenticação, autorização e controle de acesso** | Nenhum | Não encontrada | Nenhuma referência a Spring Security configs ou tokens JWT | Nenhuma | Todas as rotas estão completamente abertas; dependência de teste do Spring Security presente sem uso. |
| **Relatórios automatizados e alertas de deformação** | Nenhum | Apenas planejada ou documentada | Menções no escopo do produto | Nenhuma | Não há agendadores (Quartz/Spring Scheduled), envio de e-mails ou webhooks. |

---

## 4. Discrepâncias Documentação vs Código

1. **Documentação / Visão:** Afirma monitorar deformações de terreno com tecnologia radar (SAR/InSAR).  
   **Realidade do Código:** Não há uma única linha de código que leia, decodifique ou processe arquivos SAR (formato SLC, RAW, SAFE, COG ou HDF5). O código opera sobre imagens PNG/JPG ópticas da NASA.
2. **Documentação / Visão:** Afirma consultar imagens de fontes como ESA Copernicus e NASA/ASF.  
   **Realidade do Código:** Nenhuma integração com Copernicus SciHub/CDSE ou ASF Vertex existe no código. Apenas a API aberta de fotos da Terra da NASA (`api.nasa.gov`) foi mapeada.
3. **Documentação / Visão:** Sugere uso de modelos avançados de Inteligência Artificial para análise preditiva.  
   **Realidade do Código:** O modelo existente é uma CNN de brinquedo de 2 camadas convolucionais treinada sobre matrizes de números pseudo-aleatórios.
4. **Funcionalidades no Código não Documentadas:**
   * Utilização indevida da biblioteca GeoTools para forçar coordenadas de envelope a partir de distâncias geodésicas.
   * Hardcode explícito da pasta `deformacao-68` no serviço de armazenamento S3.
   * Script de treinamento com geração randômica de dados no repositório Python.
