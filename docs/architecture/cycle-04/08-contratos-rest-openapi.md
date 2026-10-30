# Radar++ — Ciclo 4: Arquitetura Técnica, UML e Contratos
## Documento 08 — Contratos de API REST e Especificação OpenAPI 3.0

**Documento:** `08-contratos-rest-openapi.md`  
**Ciclo:** Ciclo 4 — Arquitetura Técnica, UML e Contratos  
**Data:** 24 de Outubro de 2026  
**Status:** Oficial — Entregue para Homologação de Arquitetura  
**Responsáveis:** Arquiteto de Software Sênior, Engenheiro Frontend React/TypeScript  
**Repositório Oficial:** `api-radar-deformacao` (caminho: `docs/architecture/cycle-04/08-contratos-rest-openapi.md`)  
**Regra de Governança Estrita:** Especificação conceitual de contratos e payloads JSON. **Não criar controladores Spring MVC `@RestController` nem classes Java nesta etapa.**

---

### 1. Padrões Globais da API REST

1. **Protocolo e Formato:** Comunicação estritamente via HTTPS, utilizando JSON (`application/json;charset=UTF-8`) como padrão de intercâmbio de dados.
2. **Autenticação e Autorização:** Autenticação stateless via Bearer JWT (`Authorization: Bearer <token>`). O token contém os *claims* de identidade do usuário (`sub`), organização (`org_id`) e papéis de acesso (`roles`: `TechnicalAnalyst`, `DomainSpecialist`, `OrganizationAdmin`).
3. **Idempotência de Operações de Escrita:** Endpoints de mutação crítica (criação de AOI, submissão de processamento, emissão de nota técnica) suportam o cabeçalho `Idempotency-Key: <UUID>`. O backend armazena as chaves de idempotência em cache por 24 horas para evitar dupla submissão acidental.
4. **Padrão de Resposta de Erro:** Aderência estrita à especificação **RFC 7807 (*Problem Details for HTTP APIs*)** para todos os códigos de status de erro ($4xx$ e $5xx$).
5. **Paginação Padronizada:** Endpoints de listagem utilizam parâmetros de consulta `page` (0-indexed, default 0), `size` (default 20, max 100) e `sort` (ex: `createdAt,desc`).

---

### 2. Contrato de Erro Padronizado (RFC 7807 Problem Detail)

```json
{
  "type": "https://api.radarplusplus.com/errors/invalid-parameters",
  "title": "Unprocessable Entity",
  "status": 422,
  "detail": "A área métrica da geometria informada (68.40 km²) excede o teto contratual de 50.00 km² para a organização.",
  "instance": "/api/v1/areas-of-interest",
  "code": "ERR_AOI_AREA_EXCEEDED",
  "timestamp": "2026-10-24T12:00:00Z",
  "invalidParams": [
    {
      "name": "geometryWkt",
      "reason": "Área calculada 68.40 km2 excede limite maxAllowedAreaKm2 50.00 km2"
    }
  ]
}
```

---

### 3. Catálogo dos Endpoints Oficiais da API v1

```
+----------------------------------------------------------------------------------------------------------------------------------------------------+
|                                    MATRIZ CONSOLIDADA DE ENDPOINTS REST (RADAR++ API V1)                                                           |
+----+--------+------------------------------------+-----------------------------+------------------------------------+------------------------------+
| Nº | Método | URI / Rota                         | Contexto Delimitado         | Descrição Sumária do Caso de Uso   | Status de Sucesso Esperado   |
+----+--------+------------------------------------+-----------------------------+------------------------------------+------------------------------+
| 01 | POST   | `/api/v1/areas-of-interest`        | `territory`                 | Cadastra nova Área de Interesse    | `201 Created` (`Location`)   |
| 02 | GET    | `/api/v1/areas-of-interest`        | `territory`                 | Lista AOIs da organização com filtro| `200 OK` (Paginado)          |
| 03 | GET    | `/api/v1/areas-of-interest/{id}`   | `territory`                 | Recupera detalhes e geometria AOI  | `200 OK`                     |
+----+--------+------------------------------------+-----------------------------+------------------------------------+------------------------------+
| 04 | POST   | `/api/v1/sar-catalog/search`       | `catalog`                   | Consulta cenas Sentinel-1 no ASF   | `200 OK`                     |
| 05 | POST   | `/api/v1/sar-catalog/bursts`       | `catalog`                   | Descobre bursts interceptados      | `200 OK`                     |
| 06 | POST   | `/api/v1/sar-catalog/baseline-pair`| `catalog`                   | Avalia viabilidade de rede SBAS    | `200 OK`                     |
+----+--------+------------------------------------+-----------------------------+------------------------------------+------------------------------+
| 07 | POST   | `/api/v1/processing-requests`      | `insarcore`                 | Submete solicitação e orça créditos| `201 Created`                |
| 08 | POST   | `/api/v1/processing-requests/{id}/authorize` | `insarcore`       | Autoriza e enfileira job operacional| `202 Accepted` (`Location`) |
| 09 | GET    | `/api/v1/processing-jobs/{id}`     | `insarcore`                 | Consulta telemetria e status do job| `200 OK`                     |
| 10 | POST   | `/api/v1/processing-jobs/{id}/cancel` | `insarcore`              | Cancela job em execução na nuvem   | `200 OK`                     |
+----+--------+------------------------------------+-----------------------------+------------------------------------+------------------------------+
| 11 | GET    | `/api/v1/analyses/{id}`            | `insarcore`                 | Consulta resumo da análise e gating| `200 OK`                     |
| 12 | GET    | `/api/v1/analyses/{id}/products`   | `insarcore`                 | Lista produtos aprovados/restritos | `200 OK`                     |
| 13 | GET    | `/api/v1/products/{id}/download-url`| `insarcore`                | Gera URL pré-assinada temporária S3| `200 OK`                     |
| 14 | GET    | `/api/v1/analyses/{id}/timeseries` | `insarcore`                 | Extrai série temporal pontual LOS  | `200 OK`                     |
+----+--------+------------------------------------+-----------------------------+------------------------------------+------------------------------+
| 15 | POST   | `/api/v1/urban-investigations`     | `urbananalysis`             | Inicia dossiê de análise urbana    | `201 Created`                |
| 16 | POST   | `/api/v1/urban-investigations/{id}/profiles` | `urbananalysis`   | Traça perfil transversal de bacia  | `200 OK`                     |
| 17 | POST   | `/api/v1/urban-investigations/{id}/observations` | `urbananalysis`| Registra nota técnica com ART      | `201 Created`                |
| 18 | GET    | `/api/v1/technical-notes/{id}/pdf` | `urbananalysis`             | Download do PDF da nota técnica    | `200 OK` (`application/pdf`) |
+----+--------+------------------------------------+-----------------------------+------------------------------------+------------------------------+
```

---

### 4. Especificação de Payloads e DTOs dos Principais Endpoints

---

#### 4.1. Cadastro de Área de Interesse (`POST /api/v1/areas-of-interest`)
* **Headers:** `Authorization: Bearer <token>`, `Idempotency-Key: <UUID>`
* **Request DTO:**
  ```json
  {
    "name": "Maceio - Mutange e Bebedouro",
    "geometryWkt": "POLYGON((-35.795 -9.680, -35.710 -9.680, -35.710 -9.600, -35.795 -9.600, -35.795 -9.680))",
    "description": "Monitoramento geodésico da área urbana afetada por subsidência de minas de sal-gema."
  }
  ```
* **Response DTO (`201 Created`):**
  ```json
  {
    "id": "b8f3e21a-6411-451e-b81b-9e2c4f10a8c2",
    "organizationId": "d3b07384-d113-40a2-9721-a53b2169b821",
    "name": "Maceio - Mutange e Bebedouro",
    "metricAreaKm2": 35.42,
    "boundingBox": {
      "minLon": -35.795,
      "minLat": -9.680,
      "maxLon": -35.710,
      "maxLat": -9.600
    },
    "status": "ACTIVE",
    "createdAt": "2026-10-24T12:00:00Z"
  }
  ```

---

#### 4.2. Submissão de Solicitação de Processamento (`POST /api/v1/processing-requests`)
* **Request DTO:**
  ```json
  {
    "aoiId": "b8f3e21a-6411-451e-b81b-9e2c4f10a8c2",
    "startDate": "2023-01-01",
    "endDate": "2023-12-31",
    "targetOrbitPath": 142,
    "orbitDirection": "DESCENDING",
    "polarization": "VV",
    "maxPerpendicularBaselineMeters": 150.0,
    "maxTemporalBaselineDays": 36,
    "processingProfile": "CONSERVATIVE_URBAN"
  }
  ```
* **Response DTO (`201 Created`):**
  ```json
  {
    "requestId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "aoiId": "b8f3e21a-6411-451e-b81b-9e2c4f10a8c2",
    "status": "REQUEST_VALIDATED",
    "costEstimation": {
      "jobType": "INSAR_ISCE_BURST",
      "totalPairs": 72,
      "burstsPerDate": 1,
      "multilookResolution": "20m",
      "estimatedHyp3Credits": 72,
      "availableAccountCredits": 500,
      "sufficientFunds": true
    },
    "createdAt": "2026-10-24T12:05:00Z"
  }
  ```

---

#### 4.3. Autorização de Job Assíncrono (`POST /api/v1/processing-requests/{id}/authorize`)
* **Response (`202 Accepted`):**
  - **Header:** `Location: /api/v1/processing-jobs/a1b2c3d4-e5f6-4a5b-8c9d-0123456789ab`
  - **Body DTO:**
    ```json
    {
      "jobId": "a1b2c3d4-e5f6-4a5b-8c9d-0123456789ab",
      "requestId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      "state": "JOB_QUEUED",
      "message": "Job de processamento geodésico autorizado e despachado para a fila assíncrona com sucesso.",
      "trackingUrl": "/api/v1/processing-jobs/a1b2c3d4-e5f6-4a5b-8c9d-0123456789ab"
    }
    ```

---

#### 4.4. Consulta de Série Temporal Pontual (`GET /api/v1/analyses/{id}/timeseries?lat=-9.635&lon=-35.742`)
* **Response DTO (`200 OK`):**
  ```json
  {
    "analysisId": "e9a0c123-5522-4411-8899-776655443322",
    "coordinate": { "latitude": -9.6350, "longitude": -35.7420 },
    "primaryMeasurementType": "LOS_DISPLACEMENT_MM",
    "pointMeanCoherence": 0.62,
    "annualVelocityMmPerYear": -185.4,
    "observations": [
      { "date": "2023-01-05", "displacementMm": 0.0, "uncertaintyMm": 3.2 },
      { "date": "2023-01-17", "displacementMm": -6.1, "uncertaintyMm": 3.1 },
      { "date": "2023-01-29", "displacementMm": -12.4, "uncertaintyMm": 3.0 },
      { "date": "2023-12-28", "displacementMm": -184.2, "uncertaintyMm": 3.8 }
    ],
    "disclaimer": "Valores expressos na Linha de Visada (LOS). Deformação negativa indica afastamento em relação ao satélite."
  }
  ```

---

#### 4.5. Registro de Observação Técnica (`POST /api/v1/urban-investigations/{id}/observations`)
* **Request DTO:**
  ```json
  {
    "documentCode": "NOT-TEC-2026-MCZ-001",
    "technicalNotes": "Observada bacia de subsidência contínua com gradiente acelerado na borda oeste da lagoa Mundaú.",
    "profileIdsIncluded": ["prof_881122"],
    "declaredArtNumber": "ART-AL-2026-998811"
  }
  ```
* **Response DTO (`201 Created`):**
  ```json
  {
    "observationId": "obs_44556677-8899-0011-2233-445566778899",
    "documentCode": "NOT-TEC-2026-MCZ-001",
    "checksumSha256": "4a5f6e7d8c9b0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b",
    "pdfDownloadUrl": "/api/v1/technical-notes/obs_44556677.../pdf",
    "issuedAt": "2026-10-24T12:10:00Z",
    "legalDisclaimer": "Nota Técnica Exploratória baseada em sensoriamento remoto espacial. Não substitui laudo pericial oficial nem ART presencial."
  }
  ```
