# Radar++ — Relatório de Ajustes Finais de Integridade da Fatia Vertical 5.2

## 1. Resumo Executivo e Contexto

No encerramento da revisão técnica do Ciclo 5.2, foram identificados três pontos críticos de integridade operacional, cartográfica e científica:
1. **Política de limite de área autônoma pelo cliente:** Anteriormente, o cliente enviava `maxAllowedAreaKm2` no payload da requisição, violando o princípio de que cotas e autorizações pertencem à política de governança do backend.
2. **Dessincronização da AOI ativa e consulta SAR:** Ao modificar vértices no mapa de uma AOI previamente persistida, a interface mantinha o identificador antigo como ativo, permitindo consultar cenas SAR de uma região com os parâmetros de outra.
3. **Validação espacial e divergência de representações:** Ausência de validações estritas para polígonos colineares degenerados, vértices consecutivos duplicados e possíveis divergências topológicas quando WKT e GeoJSON fossem simultaneamente submetidos.
4. **Resultados e estado da busca de catálogo:** Necessidade de estados mutuamente independentes (`idle`, `loading`, `success`, `empty`, `error`) e reforço do disclaimer científico de que cenas individuais catalogadas não equivalem a pares interferométricos processáveis.

Todas as correções foram implementadas, testadas e homologadas preservando 100% das funcionalidades pré-existentes e respeitando estritamente os limites de governança (sem commits Git, sem deploy externo e sem processamento InSAR antecipado).

---

## 2. Ajustes Arquiteturais e de Domínio

### 2.1 Política de Limite de Área (Backend Autoritativo)

- **Remoção de controle pelo cliente:** O campo `Double maxAllowedAreaKm2` foi removido de [`CreateAoiRequest.java`](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/main/java/com/ass/br/apiradar/territory/infrastructure/adapters/input/web/CreateAoiRequest.java). O payload JSON agora aceita apenas metadados e geometria (`name`, `geometryWkt`, `geometryGeoJson`, `description`).
- **Configuração no Backend:** A propriedade institucional `radar.territory.max-allowed-area-km2: ${RADAR_TERRITORY_MAX_ALLOWED_AREA_KM2:250.0}` foi inserida em `application.yml` e `application.properties`.
- **Endpoint Informativo REST:** Implementado `GET /api/v1/areas-of-interest/policy` em [`AreaOfInterestRestController.java`](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/main/java/com/ass/br/apiradar/territory/infrastructure/adapters/input/web/AreaOfInterestRestController.java), expondo os limites vigentes (`maxAllowedAreaKm2: 250.0`, `unit: "km2"`, `srid: 4326`).
- **Aplicação pelo Domínio:** O comando [`CreateAoiCommand.java`](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/main/java/com/ass/br/apiradar/territory/application/dto/CreateAoiCommand.java) recebe o limite configurado do backend e o injeta na entidade `AreaOfInterest`, que valida `metricArea.validateMaxAllowed(maxAllowedAreaKm2)`. Áreas excedentes disparam `MaxAreaExceededException` (HTTP 422).
- **Interface Frontend Orientativa:** No frontend React, o campo editável foi substituído por um card informativo read-only com selo "Configurado no Backend", exibindo o limite consultado via `radarApiClient.getAoiPolicy()`.

### 2.2 Integridade da AOI Selecionada e Sincronização Cartográfica

- **Detecção de Geometria Modificada:** Criada a função `areCoordsEqual(c1, c2)` com tolerância geodésica de $10^{-5}$ graus (~1 metro). Se os vértices no mapa divergirem das coordenadas da AOI selecionada (`createdAoi`), a flag reativa `isGeometryModified` é ativada.
- **Sinalização Visual no Mapa:**
  - O polígono adota contorno e preenchimento âmbar/laranja (`#d97706`).
  - O mapa exibe uma tag explícita "Não Salvo".
  - A BBOX da AOI persistida antiga é ocultada para evitar confusão espacial.
  - Surge um card de alerta: *"Geometria Modificada (Não Salva) — Os vértices no mapa foram alterados e diferem da AOI persistida..."*.
- **Bloqueio de Consulta SAR para Geometrias Não Persistidas:**
  - O botão de busca no catálogo SAR é desabilitado enquanto a geometria ativa não estiver salva no PostGIS.
  - O painel exibe aviso claro de bloqueio, impedindo que o identificador da AOI antiga seja utilizado para consultar o novo polígono.
- **Ações de Recuperação:**
  - **Descartar Alterações:** Botão que restaura imediatamente as coordenadas originais da AOI salva no PostGIS.
  - **Salvar como Nova Área de Interesse:** O botão de persistência comuta para "Salvar como Nova Área de Interesse", gravando a geometria modificada com um novo registro e novo UUID no banco espacial.

### 2.3 Validação Espacial e Divergência WKT / GeoJSON

- **Validações Implementadas em [`PolygonGeometry.java`](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/main/java/com/ass/br/apiradar/shared/domain/model/PolygonGeometry.java):**
  1. *Geometrias Vazias ou Nulas:* `fromWkt("")`, `"   "`, `null`, `"POLYGON EMPTY"` e GeoJSON vazio são sumariamente rejeitados.
  2. *Limites de Coordenadas:* Validação de latitude $[-90, 90]$ e longitude $[-180, 180]$.
  3. *Vértices Consecutivos Repetidos:* Varredura de coordenadas adjacentes `coords[i].equals2D(coords[i+1])`.
  4. *Segmentos Colineares e Degeneração:* Verificação do produto vetorial para três vértices consecutivos adjacentes ($|(x_{i+1}-x_i)(y_{i+2}-y_i) - (y_{i+1}-y_i)(x_{i+2}-x_i)| < 10^{-11}$) e verificação de área nula (`polygon.getArea() <= 1e-11`).
  5. *Auto-interseção:* Validação topológica JTS `!polygon.isValid()` (rejeição de laços, cruzamentos em 8 / gravata borboleta).
- **Validação de Consistência WKT vs GeoJSON:**
  - No `AreaOfInterestRestController`, se tanto `geometryWkt` quanto `geometryGeoJson` forem fornecidos, o servidor valida a equivalência geométrica (`equalsExact` e `equalsTopo`). Se houver divergência, retorna HTTP 400 Bad Request com mensagem detalhada.
- **Estimativa no Frontend vs Autoridade no Backend:**
  - No frontend, o painel de métricas exibe explicitamente *"Área Geodésica Estimada (Prévia Leaflet - WGS84)"*. O backend PostGIS permanece a autoridade definitiva para medição e persistência métrica.

### 2.4 Estados do Catálogo SAR e Aviso Científico InSAR

- **Estados Independentes:** O catálogo SAR agora opera sob máquina de estados explícita:
  `catalogStatus: 'idle' | 'loading' | 'success' | 'empty' | 'error'`.
- **Invalidação Instantânea:** Qualquer modificação na geometria (arraste de vértice, limpeza, recarga do piloto, troca de AOI ativa) invalida os resultados anteriores (`catalogScenes = []`, `catalogStatus = 'idle'`), impedindo exibição de resultados defasados.
- **Aviso Científico InSAR Mandatório:** No rodapé da tabela de cenas da NASA ASF DAAC, foi fixado o alerta:
  > *"Aviso Científico de Elegibilidade InSAR: As cenas apresentadas acima são aquisições individuais retornadas pela busca espacial da NASA ASF DAAC. Elas NÃO constituem pares interferométricos cientificamente elegíveis de forma automática. A seleção de pares interferométricos válidos (master e slave), o cálculo de coerência espacial/temporal e a validação de baseline perpendicular serão processados nas etapas subsequentes do pipeline InSAR."*

---

## 3. Matriz de Testes e Evidências

### 3.1 Testes Unitários e de Integração Backend (Maven)

- **Comando:** `./mvnw test`
- **Resultado:** **40 testes executados, 0 falhas, 0 erros (BUILD SUCCESS)**.
- **Novos Testes Adicionados:**
  - `PolygonGeometryTest.shouldRejectRepeatedVertices`: Validação de rejeição de vértices duplicados.
  - `PolygonGeometryTest.shouldRejectCollinearSegments`: Validação de rejeição de vértices colineares redundantes e linhas colapsadas.
  - `PolygonGeometryTest.shouldRejectEmptyGeometries`: Rejeição de strings vazias, nulas e `POLYGON EMPTY`.
  - `AreaOfInterestRestControllerTest.shouldGetAoiPolicy`: Validação do endpoint `GET /policy` (HTTP 200).
  - `AreaOfInterestRestControllerTest.shouldRejectDivergentWktAndGeoJson`: Rejeição com HTTP 400 ao submeter WKT e GeoJSON divergentes.
  - `AreaOfInterestRestControllerTest.shouldAcceptEquivalentWktAndGeoJson`: Sucesso com HTTP 201 ao submeter representações equivalentes.
- **Testes de Integração PostGIS:**
  - `PostGisTerritoryRepositoryIntegrationTest`: 3 testes executados e aprovados com sucesso contra banco PostGIS real.

### 3.2 Frontend TypeScript e Build

- **Comando:** `npm --prefix ../front_react/github-friend-helper run build`
- **Resultado:** **Sucesso absoluto (`✓ built in 3.12s`)**, zero erros de compilação ou tipagem.

### 3.3 Jornada E2E no Navegador Real (Chrome Headless CDP)

A homologação E2E foi executada via script automatizado `run_e2e_cdp.mjs` (Chrome DevTools Protocol), cobrindo todo o ciclo de alteração de AOI persistida:
1. **01_initial_page.png:** Inicialização da aplicação com política institucional de área (250 km²) carregada via API.
2. **02_policy_and_aoi_loaded.png:** Carregamento de uma AOI previamente persistida no PostGIS (`Piloto Maceió Mutange PostGIS Test`).
3. **03_aoi_geometry_modified_unsaved.png:** Modificação espacial de vértice no mapa. Interface ativa badge *"Não Salvo"*, estilo âmbar e alerta de geometria não persistida.
4. **04_sar_search_blocked_warning.png:** Bloqueio mandatório da busca SAR para a AOI antiga com aviso explicativo e apresentação do botão *"Salvar como Nova Área de Interesse"*.
5. **05_new_aoi_saved_postgis.png:** Persistência com sucesso da geometria modificada sob o nome `"AOI Maceio - Geometria Refinada Ciclo 5.2"` com novo UUID e 82.48 km² no PostGIS.
6. **06_sar_scenes_discovered.png:** Execução com sucesso da busca SAR na Alaska Satellite Facility (NASA ASF) para a nova AOI, retornando 3 cenas reais Sentinel-1 IW SLC com metadados científicos (Sensor, Órbita Relativa, Direção de Voo, Polarização).
7. **07_insar_scientific_disclaimer.png:** Exibição do aviso científico de elegibilidade InSAR.
8. **09_full_page_homologation.png:** Captura completa da página comprovando integração e integridade visual.

---

## 4. Estado Git e Governança

- **Regras Estritas Obedecidas:**
  - Nenhuma operação de `git add`, `git commit` ou `git push` foi executada.
  - Nenhum arquivo interno do Antigravity, `.env`, caches ou logs foram inseridos no versionamento.
  - Projetos legados (`monitorar-images-alos-palsar`, `radar_fastapi_py`) permaneceram inalterados.
  - Nenhuma infraestrutura em nuvem (AWS) ou componente InSAR (HyP3/MintPy) foi acionado.
- **Estado Git Atual (api-radar-deformacao):**
  - Ramo: `dev`
  - Arquivos modificados (unstaged):
    - `src/main/java/com/ass/br/apiradar/shared/domain/model/PolygonGeometry.java`
    - `src/main/java/com/ass/br/apiradar/territory/application/dto/CreateAoiCommand.java`
    - `src/main/java/com/ass/br/apiradar/territory/infrastructure/adapters/input/web/CreateAoiRequest.java`
    - `src/main/java/com/ass/br/apiradar/territory/infrastructure/adapters/input/web/AreaOfInterestRestController.java`
    - `src/main/resources/application.properties`
    - `src/main/resources/application.yml`
    - `src/test/java/com/ass/br/apiradar/shared/domain/model/PolygonGeometryTest.java`
    - `src/test/java/com/ass/br/apiradar/territory/infrastructure/adapters/input/web/AreaOfInterestRestControllerTest.java`
- **Estado Git Atual (front_react/github-friend-helper):**
  - Ramo: `main`
  - Arquivos modificados (unstaged):
    - `src/models/RadarAoi.ts`
    - `src/services/radarApiClient.ts`
    - `src/components/AoiDrawingMap.tsx`

---

## 5. Conclusão

Os três problemas identificados na revisão do Ciclo 5.2 foram integralmente solucionados:
1. A política de área foi completamente centralizada no backend, removendo autonomia indevida do cliente.
2. A integridade da AOI selecionada está matematicamente e visualmente blindada, impedindo consultas cruzadas a cenas SAR com geometrias não salvas e oferecendo fluxo direto para salvar novas versões no PostGIS.
3. A robustez espacial e a máquina de estados do catálogo SAR atendem plenamente aos padrões geodésicos e científicos do Radar++.

A primeira fatia vertical encontra-se formalmente ajustada, testada e pronta para aprovação pelo usuário. O Ciclo 5.3 não foi iniciado.
