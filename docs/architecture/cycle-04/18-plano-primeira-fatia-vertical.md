# Radar++ — Ciclo 4: Arquitetura Técnica, UML e Contratos
## Documento 18 — Plano da Primeira Fatia Vertical: React → Spring Boot → PostGIS → Catálogo SAR

**Documento:** `18-plano-primeira-fatia-vertical.md`  
**Ciclo:** Ciclo 4 — Arquitetura Técnica, UML e Contratos  
**Data:** 24 de Outubro de 2026  
**Status:** Oficial — Entregue para Homologação de Engenharia  
**Responsáveis:** Arquiteto de Software Sênior, Engenheiro Frontend React/TypeScript, Especialista PostGIS  
**Repositório Oficial:** `api-radar-deformacao` (caminho: `docs/architecture/cycle-04/18-plano-primeira-fatia-vertical.md`)  
**Regra de Governança Estrita:** Especificação do plano de execução da fatia vertical. **Não iniciar implementação física neste ciclo.**

---

### 1. Objetivo da Primeira Fatia Vertical

Implementar um fluxo funcional **completo de ponta a ponta (End-to-End)** conectando a interface gráfica web, o backend modular em Spring Boot, o banco de dados espacial PostgreSQL/PostGIS e a API externa de catálogo satelital da NASA/ASF.

O propósito desta fatia é **eliminar riscos de integração precoce**, validando na prática:
1. A renderização cartográfica e desenho vetorial de polígono na SPA React;
2. O tráfego de dados via REST/JSON com validação sintática e semântica;
3. A persistência espacial e indexação GiST no PostGIS em WGS 84 (`EPSG:4326`) e cálculo métrico em UTM (`EPSG:31985`);
4. A integração HTTP real com a API pública de busca da NASA/ASF (ASF Search API), retornando granulações SLC do Sentinel-1 disponíveis na BBOX delimitada.

```mermaid
flowchart LR
    subgraph "1. Frontend (React 18 / Leaflet)"
        UI["Tela de Desenho e Cadastro da AOI"]
    end

    subgraph "2. Backend (Spring Boot 3.3)"
        CTRL["AreaOfInterestRestController<br/>SarCatalogRestController"]
        MOD_TERR["Módulo: territory<br/>(Invariantes & Topologia)"]
        MOD_CAT["Módulo: catalog<br/>(Cliente HTTP ASF)"]
    end

    subgraph "3. Banco Espacial"
        PG[("PostgreSQL 16 + PostGIS<br/>radar.areas_of_interest<br/>Indexação GiST")]
    end

    subgraph "4. Provedor Externo"
        ASF["NASA ASF Search API<br/>(Metadados Sentinel-1 IW SLC)"]
    end

    UI -->|1. POST /api/v1/areas-of-interest| CTRL
    CTRL --> MOD_TERR
    MOD_TERR -->|2. Valida Área e Persiste Geom| PG
    
    UI -->|3. POST /api/v1/sar-catalog/search| CTRL
    CTRL --> MOD_CAT
    MOD_CAT -->|4. Query HTTP na BBOX da AOI| ASF
    ASF -->> MOD_CAT: 5. Retorna Cenas SLC
    MOD_CAT -->> UI: 6. Exibe Cenas Disponíveis no Mapa
```

---

### 2. Escopo da Fatia Vertical 1

```
+----------------------------------------------------------------------------------------------------------------------------------------------------+
|                                    ESCOPO DA FATIA VERTICAL 1 (REACT → SPRING BOOT → POSTGIS → CATÁLOGO)                                           |
+--------------------------------------------------------------------+-------------------------------------------------------------------------------+
| O QUE ESTÁ INCLUSO NA FATIA (IN-SCOPE)                             | O QUE FICA PARA AS FATIAS SUBSEQUENTES (OUT-OF-SCOPE)                         |
+--------------------------------------------------------------------+-------------------------------------------------------------------------------+
| 1. Interface Web com mapa interativo para desenho de polígono WKT. | 1. Execução de processamento InSAR pesado (MintPy / SNAP / Inversão).         |
| 2. Endpoint `POST /api/v1/areas-of-interest` (com Idempotency-Key).| 2. Submissão de jobs no cluster HyP3 ou consumo de créditos.                  |
| 3. Validação OGC de polígono fechado e sem auto-interseção.        | 3. Geração ou renderização de Cloud Optimized GeoTIFF (COG).                  |
| 4. Cálculo métrico exato de área em km² via projeção UTM PostGIS.  | 4. Emissão de laudos ou notas técnicas de subsidência urbana.                 |
| 5. Persistência na tabela `radar.areas_of_interest` com índice GiST| 5. Autenticação externa avançada via Keycloak ou AWS Cognito.                 |
| 6. Endpoint `POST /api/v1/sar-catalog/search` com consulta ao ASF. | 6. Pipeline completo de mensageria com workers em nós separados.              |
| 7. Exibição da lista de cenas Sentinel-1 IW SLC encontradas na UI. |                                                                               |
+--------------------------------------------------------------------+-------------------------------------------------------------------------------+
```

---

### 3. Critérios de Aceitação da Fatia Vertical 1

A Fatia Vertical 1 será considerada homologada quando:
1. **Critério 1 (Desenho e Validação de AOI):** O usuário desenha um polígono na região de Maceió/AL na interface React. Ao submeter, o backend calcula a área métrica em km² e a BBOX. Se a área for $\le 50\,\text{km}^2$, retorna `HTTP 201 Created` com o ID da AOI; se for $> 50\,\text{km}^2$, retorna `HTTP 422 Unprocessable Entity` com erro RFC 7807;
2. **Critério 2 (Persistência PostGIS):** A geometria é gravada na coluna `geometry_wgs84` com SRID 4326 e pode ser consultada instantaneamente via operador de índice espacial `ST_Intersects`;
3. **Critério 3 (Consulta de Cenas Reais):** O backend consome a BBOX da AOI recém-criada, executa uma requisição HTTP na API pública da NASA ASF e retorna na tela do frontend uma lista com as granulações Sentinel-1 SLC disponíveis para aquela coordenada geográfica (exibindo data de aquisição, órbita relativa e direção de voo).

---

### 4. Roteiro Sequencial de Implementação (Passo a Passo Verificável)

---

#### Passo 1: Infraestrutura de Desenvolvimento Local (Docker Compose)
* **Ação:** Estruturar o arquivo `docker-compose.yml` contendo:
  - Serviço `postgres`: imagem `postgis/postgis:16-3.4` com banco `radar_db` exposto na porta 5432;
  - Serviço `minio`: emulador S3 exposto nas portas 9000/9001.
* **Validação:** Subir contêineres e verificar se a extensão PostGIS está ativa via `SELECT PostGIS_Version();`.

---

#### Passo 2: Estrutura Modular Backend (Java / Spring Boot)
* **Ação:** Configurar os pacotes do Monólito Modular:
  - `com.ass.br.apiradar.shared`: Value Objects (`AoiId`, `OrganizationId`, `MetricArea`, `BoundingBox`);
  - `com.ass.br.apiradar.territory`: Agregado `AreaOfInterest`, porta de repositório e adaptador JPA;
  - `com.ass.br.apiradar.catalog`: Serviço de consulta de catálogo e adaptador HTTP para a API da NASA ASF.
* **Validação:** Teste de arquitetura ArchUnit validando ausência de ciclos e isolamento de pacotes.

---

#### Passo 3: Criação da Tabela PostGIS via DDL/Migration
* **Ação:** Criar script Flyway inicial `V1__init_territory_schema.sql` contendo a tabela `radar.organizations` e `radar.areas_of_interest`, com índice espacial `idx_aoi_geom_gist`.
* **Validação:** Teste de integração com `Testcontainers` inserindo e recuperando um polígono via Spring Data JPA.

---

#### Passo 4: Implementação do Adaptador HTTP para Catálogo SAR (NASA ASF)
* **Ação:** Implementar o componente `AsfSearchClientAdapter` utilizando Spring WebClient (ou RestTemplate), invocando `https://api.daac.asf.alaska.edu/services/search/param`.
* **Validação:** Teste unitário com WireMock simulando resposta GeoJSON da NASA e teste de integração real opcional contra o endpoint público.

---

#### Passo 5: Implementação dos Controladores REST
* **Ação:** Criar `AreaOfInterestRestController` (`POST /api/v1/areas-of-interest`) e `SarCatalogRestController` (`POST /api/v1/sar-catalog/search`).
* **Validação:** Testes de integração com `MockMvc` validando status 201 Created e tratamento de erros RFC 7807.

---

#### Passo 6: Interface Frontend (React / TypeScript / Leaflet)
* **Ação:** Construir a tela `AoiCreatePage.tsx` com componente Leaflet para desenho vetorial de polígono e botão "Buscar Cenas Disponíveis", conectada aos endpoints criados.
* **Validação:** Teste de ponta a ponta manual ou automatizado (Cypress/Playwright) desenhando uma área e visualizando as granulações retornadas.

---

### 5. Contratos de Dados Testáveis da Fatia

#### 5.1. Entrada: Cadastro de AOI
```http
POST /api/v1/areas-of-interest HTTP/1.1
Content-Type: application/json
Idempotency-Key: 7b83c21a-5544-411a-881b-9e2c4f10a8c2

{
  "name": "Piloto Maceio - Mutange",
  "geometryWkt": "POLYGON((-35.795 -9.680, -35.710 -9.680, -35.710 -9.600, -35.795 -9.600, -35.795 -9.680))",
  "description": "Área de monitoramento da bacia de subsidência urbana em Maceió."
}
```

#### 5.2. Saída: Confirmação e Métricas
```http
HTTP/1.1 201 Created
Location: /api/v1/areas-of-interest/b8f3e21a-6411-451e-b81b-9e2c4f10a8c2

{
  "id": "b8f3e21a-6411-451e-b81b-9e2c4f10a8c2",
  "name": "Piloto Maceio - Mutange",
  "metricAreaKm2": 35.42,
  "status": "ACTIVE",
  "boundingBox": {
    "minLon": -35.795,
    "minLat": -9.680,
    "maxLon": -35.710,
    "maxLat": -9.600
  },
  "createdAt": "2026-10-24T12:00:00Z"
}
```

#### 5.3. Entrada: Busca de Cenas SAR no Catálogo
```http
POST /api/v1/sar-catalog/search HTTP/1.1
Content-Type: application/json

{
  "aoiId": "b8f3e21a-6411-451e-b81b-9e2c4f10a8c2",
  "startDate": "2023-01-01",
  "endDate": "2023-03-31",
  "orbitDirection": "DESCENDING"
}
```

#### 5.4. Saída: Granulações Sentinel-1 Encontradas
```http
HTTP/1.1 200 OK

{
  "totalScenesFound": 7,
  "scenes": [
    {
      "sceneName": "S1A_IW_SLC__1SDV_20230105T211512_20230105T211539_046654_05973E_5A12",
      "acquisitionDate": "2023-01-05T21:15:12Z",
      "relativeOrbit": 142,
      "flightDirection": "DESCENDING",
      "polarization": "VV+VH",
      "sensor": "SENTINEL-1A"
    }
  ]
}
```
