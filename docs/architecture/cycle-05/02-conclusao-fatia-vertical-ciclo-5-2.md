# Radar++ — Ciclo 5.2: Conclusão da Primeira Fatia Vertical
**Relatório Técnico e Evidências de Homologação Ponta a Ponta**  
**Data:** 25/10/2026  
**Status:** Concluído com Sucesso — Primeira Fatia Vertical Totalmente Homologada  

---

## 1. Funcionalidades Efetivamente Implementadas

1. **Edição Geográfica Interativa no Mapa (Leaflet + React):**
   - Criação de polígono interativo via clique direto no mapa (`MapClickHandler`).
   - Linha guia dinâmica durante a adição de vértices.
   - Fechamento automatizado de anel poligonal.
   - Marcadores de vértices arrastáveis em tempo real (`Marker` com `L.divIcon` estilizado).
   - Exclusão pontual de vértices (garantindo invariante de no mínimo 3 pontos).
   - Inserção de ponto médio entre arestas para refinamento de detalhes cartográficos.
   - Limpeza total e restauração rápida para o polígono piloto de Maceió.
2. **Cálculo Geodésico e Validação Espacial em Tempo Real no Frontend:**
   - Cálculo geodésico esferoidal WGS 84 de área em km² à medida que vértices são desenhados ou movidos.
   - Detecção imediata de auto-interseções com alerta visual em vermelho.
   - Verificação em tempo real contra o teto contratual configurável (com alerta amarelo caso excedido).
3. **Persistência Espacial e Gestão de AOIs (PostgreSQL 16 + PostGIS 3.4 + Flyway):**
   - Suporte bidirecional a WKT e GeoJSON nativo (`Polygon` e `Feature`) no cadastro e na recuperação.
   - Validação topológica OGC rigorosa no backend (SRID 4326, limites geográficos lat [-90, 90], lon [-180, 180], anel fechado, sem auto-interseção).
   - Indexação espacial GiST e cálculo exato de área geodésica no PostgreSQL.
4. **Seleção de AOI e Centralização Cartográfica Suave:**
   - Dropdown reativo listando todas as AOIs cadastradas na organização.
   - Ao selecionar uma AOI, os limites geográficos (`BoundingBox`) são calculados e o mapa centraliza e aproxima suavemente (`map.fitBounds`).
5. **Descoberta e Consulta ao Catálogo SAR Sentinel-1 (NASA ASF DAAC):**
   - Busca de produtos Sentinel-1 IW SLC para a BBOX da AOI selecionada.
   - Suporte a filtros de período (`startDate`, `endDate`) e direção orbital (`ALL`, `DESCENDING`, `ASCENDING`).
   - Apresentação em tabela tipada com metadados científicos reais, identificador externo e atribuição explícita de fonte ("Alaska Satellite Facility DAAC").
   - Esclarecimento científico sobre a distinção entre catálogo SAR e seleção de pares interferométricos (Etapa 5.3).

---

## 2. Alterações no Frontend

- [`AoiDrawingMap.tsx`](file:///home/asoares/Documentos/repositories/repo_radar/front_react/github-friend-helper/src/components/AoiDrawingMap.tsx): Implementado componente cartográfico completo com modos `draw`, `edit` e `view`, marcadores arrastáveis, cálculo geodésico, detecção de auto-interseção, sincronização bidirecional WKT/GeoJSON, seletor de AOIs e tabela de metadados SAR.
- [`RadarAoi.ts`](file:///home/asoares/Documentos/repositories/repo_radar/front_react/github-friend-helper/src/models/RadarAoi.ts): Adicionadas interfaces `GeoJsonPolygon` e `GeoJsonFeature`, além de campos opcionais `geometryGeoJson` e `maxAllowedAreaKm2` em `CreateAoiRequest` e `AoiResponse`.
- [`SarScene.ts`](file:///home/asoares/Documentos/repositories/repo_radar/front_react/github-friend-helper/src/models/SarScene.ts): Adicionados campos `externalId` e `metadataSource` para rastreabilidade de dados de satélite.
- [`radarApiClient.ts`](file:///home/asoares/Documentos/repositories/repo_radar/front_react/github-friend-helper/src/services/radarApiClient.ts): Adicionado método `getAreaOfInterestGeoJson(id)` para recuperação direta de GeoJSON Feature.

---

## 3. Alterações no Backend

- [`PolygonGeometry.java`](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/main/java/com/ass/br/apiradar/shared/domain/model/PolygonGeometry.java): Validações adicionadas para limites globais de coordenadas (lon [-180, 180], lat [-90, 90]), anel fechado e mínimo de 3 vértices distintos. Implementados métodos bidirecionais `toGeoJson()` e `fromGeoJson(Map<String, Object>)`.
- [`CreateAoiRequest.java`](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/main/java/com/ass/br/apiradar/territory/infrastructure/adapters/input/web/CreateAoiRequest.java): Suporte a `geometryGeoJson` e `maxAllowedAreaKm2`.
- [`AoiResponseDto.java`](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/main/java/com/ass/br/apiradar/territory/application/dto/AoiResponseDto.java): Inclusão de `geometryGeoJson` preservando construtor legado com 8 argumentos.
- [`AreaOfInterestRestController.java`](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/main/java/com/ass/br/apiradar/territory/infrastructure/adapters/input/web/AreaOfInterestRestController.java): Conversão transparente de GeoJSON/WKT no cadastro, aceitação de teto configurável e novo endpoint `GET /api/v1/areas-of-interest/{id}/geojson`.
- [`SarScene.java`](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/main/java/com/ass/br/apiradar/catalog/domain/model/SarScene.java): Adicionados `externalId` e `metadataSource`.
- [`AsfSearchClientAdapter.java`](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/main/java/com/ass/br/apiradar/catalog/infrastructure/adapters/output/external/AsfSearchClientAdapter.java): Extração de `fileID` como identificador externo e atribuição de `metadataSource = "Alaska Satellite Facility DAAC (NASA ASF)"`.
- [`SarCatalogApplicationService.java`](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/main/java/com/ass/br/apiradar/catalog/application/services/SarCatalogApplicationService.java): Validação de período inválido (`start.isAfter(end)`).
- [`GlobalExceptionHandler.java`](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/main/java/com/ass/br/apiradar/shared/infrastructure/web/GlobalExceptionHandler.java): Mapeamento de `IllegalArgumentException` para RFC 7807 Bad Request.

---

## 4. Migrations do Banco de Dados

As migrations Flyway existentes (`V1__init_territory_schema.sql` e `V2__legacy_schema_compatibility.sql`) atenderam perfeitamente a todos os requisitos espaciais do Ciclo 5.2 (tabelas `radar.areas_of_interest` com tipo `geometry(Polygon, 4326)` e índice GiST `idx_aoi_geom`), não havendo necessidade de novas alterações DDL.

---

## 5. Contratos REST Utilizados

### 5.1 POST `/api/v1/areas-of-interest`
```json
{
  "name": "AOI Homologacao E2E Ciclo 5.2",
  "description": "Validacao ponta a ponta com PostGIS e NASA ASF",
  "geometryWkt": "POLYGON((-35.795 -9.680, -35.710 -9.680, -35.710 -9.600, -35.795 -9.600, -35.795 -9.680))",
  "geometryGeoJson": {
    "type": "Polygon",
    "coordinates": [[[-35.795, -9.68], [-35.71, -9.68], [-35.71, -9.6], [-35.795, -9.6], [-35.795, -9.68]]]
  },
  "maxAllowedAreaKm2": 120.0
}
```
**Resposta (HTTP 201 Created):**
```json
{
  "id": "13c6cd8b-48c0-4cc2-afd7-a51af294a338",
  "organizationId": "d3b07384-d113-40a2-9721-a53b2169b821",
  "name": "AOI Homologacao E2E Ciclo 5.2",
  "metricAreaKm2": 82.94,
  "status": "ACTIVE",
  "boundingBox": { "minLon": -35.795, "minLat": -9.68, "maxLon": -35.71, "maxLat": -9.6 },
  "geometryWkt": "POLYGON ((-35.795 -9.68, -35.71 -9.68, -35.71 -9.6, -35.795 -9.6, -35.795 -9.68))",
  "geometryGeoJson": { "type": "Polygon", "coordinates": [...] },
  "createdAt": "2026-10-26T02:45:20.123456Z"
}
```

### 5.2 GET `/api/v1/areas-of-interest/{id}/geojson`
**Resposta (HTTP 200 OK — GeoJSON Feature):**
```json
{
  "type": "Feature",
  "id": "13c6cd8b-48c0-4cc2-afd7-a51af294a338",
  "geometry": {
    "type": "Polygon",
    "coordinates": [[[-35.795, -9.68], [-35.71, -9.68], [-35.71, -9.6], [-35.795, -9.6], [-35.795, -9.68]]]
  },
  "properties": {
    "id": "13c6cd8b-48c0-4cc2-afd7-a51af294a338",
    "name": "AOI Homologacao E2E Ciclo 5.2",
    "metricAreaKm2": 82.94,
    "status": "ACTIVE"
  }
}
```

### 5.3 POST `/api/v1/sar-catalog/search`
```json
{
  "aoiId": "13c6cd8b-48c0-4cc2-afd7-a51af294a338",
  "startDate": "2023-01-01",
  "endDate": "2023-01-31",
  "orbitDirection": "DESCENDING"
}
```
**Resposta (HTTP 200 OK — Dados Reais da NASA ASF):**
```json
{
  "totalScenesFound": 3,
  "scenes": [
    {
      "sceneName": "S1A_IW_SLC__1SDV_20230128T080248_20230128T080315_046981_05A28E_C246",
      "acquisitionDate": "2023-01-28T08:02:48Z",
      "relativeOrbit": 9,
      "flightDirection": "DESCENDING",
      "polarization": "VV+VH",
      "sensor": "Sentinel-1A",
      "externalId": "S1A_IW_SLC__1SDV_20230128T080248_20230128T080315_046981_05A28E_C246-SLC",
      "metadataSource": "Alaska Satellite Facility DAAC (NASA ASF)"
    },
    {
      "sceneName": "S1A_IW_SLC__1SDV_20230116T080248_20230116T080315_046806_059CA6_BF75",
      "acquisitionDate": "2023-01-16T08:02:48Z",
      "relativeOrbit": 9,
      "flightDirection": "DESCENDING",
      "polarization": "VV+VH",
      "sensor": "Sentinel-1A",
      "externalId": "S1A_IW_SLC__1SDV_20230116T080248_20230116T080315_046806_059CA6_BF75-SLC",
      "metadataSource": "Alaska Satellite Facility DAAC (NASA ASF)"
    },
    {
      "sceneName": "S1A_IW_SLC__1SDV_20230104T080249_20230104T080316_046631_0596C5_604A",
      "acquisitionDate": "2023-01-04T08:02:49Z",
      "relativeOrbit": 9,
      "flightDirection": "DESCENDING",
      "polarization": "VV+VH",
      "sensor": "Sentinel-1A",
      "externalId": "S1A_IW_SLC__1SDV_20230104T080249_20230104T080316_046631_0596C5_604A-SLC",
      "metadataSource": "Alaska Satellite Facility DAAC (NASA ASF)"
    }
  ]
}
```

---

## 6. Resultados dos Testes Automatizados

- **Backend (Maven Test Suite):**
  - Total de testes: **34 testes**
  - Falhas: **0**
  - Erros: **0**
  - Ignorados: **0**
  - Status: **BUILD SUCCESS**
- **Frontend (TypeScript & Vite):**
  - `npx tsc --noEmit -p tsconfig.app.json`: **0 erros**
  - `npm run build`: **Sucesso (built in 2.89s)**

---

## 7. Evidências da Jornada no Navegador (End-to-End)

A jornada exigida no item 8 foi executada em ambiente real de navegador utilizando Chrome com automação via Chrome DevTools Protocol (CDP):

| Etapa | Ação Realizada | Evidência Capturada |
| :--- | :--- | :--- |
| **1** | Abertura do Radar++ em `http://localhost:3000` | `01_initial_page.png` |
| **2** | Configuração e desenho da AOI ("AOI Homologacao E2E Ciclo 5.2", teto 120 km²) | `02_configured_aoi.png` |
| **3** | Clique em "Salvar Área no Banco Espacial" e retorno 201 com alerta verde no PostGIS | `03_saved_aoi.png` |
| **4** | Recarregamento da página (F5 / Reload completo) | `04_reloaded_page.png` |
| **5 & 6** | Recuperação da lista de AOIs no dropdown e seleção da AOI com centralização do mapa | `06_selected_aoi_centered.png` |
| **7 & 8** | Consulta e renderização da tabela de cenas Sentinel-1 IW SLC da NASA ASF DAAC | `08_sar_scenes_table.png` |
| **Geral**| Visão panorâmica de toda a interface homologada | `09_full_page_homologation.png` |

---

## 8. Limitações e Decisões Pendentes

1. **Par Interferométrico (InSAR Pair Selection):**
   - As cenas catalogadas foram validadas por sobreposição espacial (BBOX). A seleção analítica do par Master/Slave com baseline perpendicular (< 150m) e baseline temporal (ex.: 6, 12, 24 dias) será implementada no Ciclo 5.3.
2. **Autenticação Multi-Tenant em Produção:**
   - O isolamento com `OrganizationContextResolver` opera com organização mock padrão no perfil `dev` e rejeição em `prod`. A integração de tokens JWT/Keycloak segue mapeada para o ciclo de segurança institucional.

---

## 9. Estado Git dos Repositórios (Governança Respeitada)

Nenhum comando de commit, push, rebase ou mesclagem foi executado. Todos os arquivos permanecem intactos nos ramos de trabalho:
- `api-radar-deformacao`: Ramo `dev`, working tree preservada.
- `front_react/github-friend-helper`: Ramo `main`, working tree preservada.
- `monitorar-images-alos-palsar`: Limpo, sem alterações.
- `radar_fastapi_py`: Preservado sem modificações.
