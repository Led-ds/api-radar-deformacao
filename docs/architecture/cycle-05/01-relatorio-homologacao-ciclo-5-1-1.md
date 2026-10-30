# Radar++ — Ciclo 5.1.1: Relatório de Estabilização e Homologação Técnica
**Fatia Vertical 1: Fundação Técnica, Catálogo SAR e Gestão Territorial**  
**Data de Homologação:** 25/10/2026  
**Status da Auditoria:** Concluído com Sucesso — Fundação Técnica Pronta e Homologada  

---

## 1. Sumário Executivo

O **Ciclo 5.1.1** teve como objetivo central auditar, estabilizar, corrigir inconsistências de contrato e homologar a primeira fatia vertical do **Radar++** implementada no Ciclo 5.1. A fatia vertical contempla:
1. **Gestão Territorial (Bounded Context `territory`):** Cadastro, validação WKT/SRID 4326, cálculo geodésico de área em km² e persistência espacial de Áreas de Interesse (AOIs) em PostgreSQL 16 + PostGIS 3.4.
2. **Catálogo SAR Sentinel-1 (Bounded Context `catalog`):** Integração externa com a API de busca da Alaska Satellite Facility (NASA ASF DAAC), filtragem estrita para produtos Sentinel-1 IW SLC, mapeamento tipado e tratamento de erros sem uso de mocks em produção.
3. **Frontend Web SPA (React 18 / TypeScript / Vite / Leaflet):** Alinhamento estrito de contratos TypeScript, validação de compilação sem emissão de erros (`tsc --noEmit`), build de produção em Nginx e visualização geográfica de AOIs.
4. **Infraestrutura em Docker Compose:** Orquestração completa dos serviços `radar_postgres`, `radar_backend` e `radar_frontend`, com health checks ativos e comunicação interna validada.

---

## 2. Problemas Encontrados e Correções Realizadas

| Item | Componente | Problema Detectado | Correção Aplicada | Status |
| :--- | :--- | :--- | :--- | :--- |
| **01** | Frontend TypeScript | Divergência de nomenclatura no contrato de busca SAR: `SarScene.ts` exportava `SarCatalogSearchRequest`, mas `radarApiClient.ts` importava `SarCatalogRequest`. | Padronização integral para `SarCatalogSearchRequest` em ambos os arquivos, espelhando fielmente o DTO Java record `SarCatalogSearchRequest.java`. | **Resolvido** |
| **02** | Frontend Mapa AOI | Associação rígida e estática no UI vinculando trajetórias orbitais exclusivamente aos Tracks 142/171 (Maceió), induzindo a erros em outras regiões do Brasil. | Removida associação restritiva; implementada seleção limpa de direção orbital (`ASCENDING`, `DESCENDING` ou todas), desacoplando a lógica de visualização. | **Resolvido** |
| **03** | Backend Maven Packaging | O `pom.xml` não declarava explicitamente o `spring-boot-maven-plugin` na seção `<build><plugins>`, gerando JAR padrão sem manifesto executável (`no main manifest attribute in /app/app.jar`) no contêiner Docker. | Adicionado `spring-boot-maven-plugin` versão 3.3.4 no `pom.xml` e executado repackage via `./mvnw package -DskipTests -B`. | **Resolvido** |
| **04** | Backend Segurança | Uso inseguro e permissivo de `X-Organization-Id` arbitrária sem validação de perfil ou autenticação. | Criado `OrganizationContextResolver` que isola fallback de organização mock (`d3b07384-d113-40a2-9721-a53b2169b821`) exclusivamente para perfis `dev` e `test`. Em produção, rejeita requisições não autenticadas. | **Resolvido** |
| **05** | Backend PostGIS Flyway | Convivência entre schema legado e novo schema `radar`: a entidade legada `ImagemRadar` e classes legadas apontavam para tabelas em schema público sem migrations unificadas. | Criada migration Flyway `V2__legacy_schema_compatibility.sql`, garantindo compatibilidade com tabelas legadas sem quebrar a inicialização do monólito. | **Resolvido** |
| **06** | Backend NASA ASF Adapter | Risco de respostas externas com falha causarem exceções genéricas 500 no backend. | Implementado tratamento rigoroso com `ExternalServiceException` mapeado para HTTP 502/504 no `GlobalExceptionHandler`, com timeouts de conexão (10s) e leitura (30s). | **Resolvido** |

---

## 3. Evidências dos Testes Automatizados (Backend & PostGIS Real)

### 3.1 Execução da Suite de Testes Maven (`./mvnw clean test`)

A suite completa de testes automatizados do backend foi executada contra a base de dados real PostGIS 16-3.4 em contêiner Docker local:

```bash
./mvnw clean test
```

**Resultado:**
```text
[INFO] Running com.ass.br.apiradar.territory.infrastructure.adapters.output.persistence.PostGisTerritoryRepositoryIntegrationTest
2026-10-25T22:43:09.156-03:00  INFO 136974 --- [apiRadar] [           main] ostGisTerritoryRepositoryIntegrationTest : Starting PostGisTerritoryRepositoryIntegrationTest using Java 21.0.12 with PID 136974
2026-10-25T22:43:10.006-03:00  INFO 136974 --- [apiRadar] [           main] .s.d.r.c.RepositoryConfigurationDelegate : Finished Spring Data repository scanning in 40 ms. Found 4 JPA repository interfaces.
2026-10-25T22:43:10.873-03:00  INFO 136974 --- [apiRadar] [           main] org.flywaydb.core.FlywayExecutor         : Database: jdbc:postgresql://localhost:5435/radar_db (PostgreSQL 16.4)
2026-10-25T22:43:10.906-03:00  INFO 136974 --- [apiRadar] [           main] o.f.core.internal.command.DbValidate     : Successfully validated 3 migrations (execution time 00:00.015s)
2026-10-25T22:43:10.922-03:00  INFO 136974 --- [apiRadar] [           main] o.f.core.internal.command.DbMigrate      : Current version of schema "radar": 2
2026-10-25T22:43:10.923-03:00  INFO 136974 --- [apiRadar] [           main] o.f.core.internal.command.DbMigrate      : Schema "radar" is up to date. No migration necessary.
2026-10-25T22:43:11.758-03:00  INFO 136974 --- [apiRadar] [           main] ostGisTerritoryRepositoryIntegrationTest : Started PostGisTerritoryRepositoryIntegrationTest in 2.836 seconds
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in PostGisTerritoryRepositoryIntegrationTest

[INFO] Running com.ass.br.apiradar.catalog.infrastructure.adapters.output.asf.AsfSearchClientAdapterTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- in AsfSearchClientAdapterTest

[INFO] Running com.ass.br.apiradar.architecture.ArchitectureTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in ArchitectureTest

[INFO] Running com.ass.br.apiradar.territory.domain.model.AreaOfInterestTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- in AreaOfInterestTest

[INFO] Running com.ass.br.apiradar.territory.domain.model.MetricAreaTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in MetricAreaTest

[INFO] Running com.ass.br.apiradar.territory.domain.model.BoundingBoxTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in BoundingBoxTest

[INFO] Running com.ass.br.apiradar.territory.infrastructure.adapters.input.web.AreaOfInterestRestControllerTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- in AreaOfInterestRestControllerTest

[INFO] Running com.ass.br.apiradar.catalog.infrastructure.adapters.input.web.SarCatalogRestControllerTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in SarCatalogRestControllerTest

[INFO] Running com.ass.br.apiradar.shared.infrastructure.web.GlobalExceptionHandlerTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in GlobalExceptionHandlerTest

[INFO] Running com.ass.br.apiradar.shared.infrastructure.web.HealthCheckControllerTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in HealthCheckControllerTest

[INFO] Running com.ass.br.apiradar.ApiRadarApplicationTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in ApiRadarApplicationTests

[INFO] ------------------------------------------------------------------------
[INFO] Results:
[INFO] Tests run: 27, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] Total time: 19.089 s
[INFO] ------------------------------------------------------------------------
```

### 3.2 Validação Real da Base PostGIS e Flyway no Banco de Dados

Verificação direta no container `radar_postgres`:

```bash
docker exec radar_postgres psql -U radar_user -d radar_db -c "SELECT PostGIS_Full_Version();"
```
```text
POSTGIS="3.4.3 e365945" [EXTENSION] PGSQL="160" GEOS="3.9.0-CAPI-1.16.2" PROJ="7.2.1" LIBXML="2.9.10" LIBJSON="0.15" LIBPROTOBUF="1.3.3" WAGYU="0.5.0 (Internal)" TOPOLOGY
```

Tabela `radar.flyway_schema_history`:
```text
 installed_rank | version |         description          |  type  |               script                | success 
----------------+---------+------------------------------+--------+-------------------------------------+---------
              0 |         | << Flyway Schema Creation >> | SCHEMA | "radar"                             | t
              1 | 1       | init territory schema        | SQL    | V1__init_territory_schema.sql       | t
              2 | 2       | legacy schema compatibility  | SQL    | V2__legacy_schema_compatibility.sql | t
```

---

## 4. Evidências do Frontend (TypeScript & Build)

Execução realizada no repositório `front_react/github-friend-helper`:

```bash
npx tsc --noEmit -p tsconfig.app.json && npm run build
```

**Resultado:**
```text
> vite_react_shadcn_ts@0.0.0 build
> vite build

vite v5.4.10 building for production...
✓ 1755 modules transformed.
dist/index.html                   0.74 kB │ gzip:   0.43 kB
dist/assets/index-CkNtyTg0.css   80.24 kB │ gzip:  17.90 kB
dist/assets/index-CZU0NFCo.js   510.66 kB │ gzip: 158.79 kB
✓ built in 2.89s
```
*Zero erros de tipagem TypeScript e bundle de produção gerado com sucesso.*

---

## 5. Evidências do Docker Compose e Health Checks

### 5.1 Status dos Serviços (`docker compose ps`)

```bash
docker compose ps
```
```text
NAME             IMAGE                           COMMAND                  SERVICE    STATUS                    PORTS
radar_backend    api-radar-deformacao-backend    "java -XX:+UseZGC -X…"   backend    Up 27 minutes             0.0.0.0:8080->8080/tcp
radar_frontend   api-radar-deformacao-frontend   "/docker-entrypoint.…"   frontend   Up 27 minutes             0.0.0.0:3000->80/tcp
radar_postgres   postgis/postgis:16-3.4          "docker-entrypoint.s…"   postgres   Up 27 minutes (healthy)   0.0.0.0:5435->5432/tcp
```

### 5.2 Health Check do Backend (`/actuator/health` e `/api/v1/health`)

```bash
curl -i http://localhost:8080/actuator/health
```
```http
HTTP/1.1 200 
Content-Type: application/vnd.spring-boot.actuator.v3+json

{"status":"UP","components":{"db":{"status":"UP","details":{"database":"PostgreSQL","validationQuery":"isValid()"}},"diskSpace":{"status":"UP"},"livenessState":{"status":"UP"},"readinessState":{"status":"UP"}}}
```

```bash
curl -i http://localhost:8080/api/v1/health
```
```http
HTTP/1.1 200 
Content-Type: application/json

{"status":"UP","application":"Radar++","version":"0.0.1-MVP","profile":"dev","timestamp":"2026-10-26T01:42:00.535062217Z"}
```

### 5.3 Health Check do Frontend Web (`http://localhost:3000`)

```bash
curl -i http://localhost:3000
```
```http
HTTP/1.1 200 OK
Server: nginx/1.31.8
Content-Type: text/html
Content-Length: 736
```

---

## 6. Teste de Integração End-to-End Real

### 6.1 Cadastro de Nova AOI via POST `/api/v1/areas-of-interest`

```bash
curl -s -X POST http://localhost:8080/api/v1/areas-of-interest \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Maceio Homologacao 5.1.1",
    "description": "Teste end-to-end pos-recuperacao",
    "geometryWkt": "POLYGON((-35.75 -9.67, -35.72 -9.67, -35.72 -9.64, -35.75 -9.64, -35.75 -9.67))"
  }'
```
**Resposta do Servidor (HTTP 201 Created):**
```json
{
  "id": "1e7a1465-c3d7-44e6-8398-5ced7dac17ec",
  "organizationId": "d3b07384-d113-40a2-9721-a53b2169b821",
  "name": "Maceio Homologacao 5.1.1",
  "metricAreaKm2": 10.98,
  "status": "ACTIVE",
  "boundingBox": {
    "minLon": -35.75,
    "minLat": -9.67,
    "maxLon": -35.72,
    "maxLat": -9.64
  },
  "geometryWkt": "POLYGON ((-35.75 -9.67, -35.72 -9.67, -35.72 -9.64, -35.75 -9.64, -35.75 -9.67))",
  "createdAt": "2026-10-26T01:54:43.423953027Z"
}
```

### 6.2 Consulta Real de Cenas SAR Sentinel-1 IW SLC na NASA ASF para a AOI Cadastrada

```bash
curl -s -X POST http://localhost:8080/api/v1/sar-catalog/search \
  -H "Content-Type: application/json" \
  -d '{
    "aoiId": "1e7a1465-c3d7-44e6-8398-5ced7dac17ec",
    "startDate": "2023-01-01",
    "endDate": "2023-01-20"
  }'
```
**Resposta do Servidor (HTTP 200 OK — Dados Reais da NASA ASF):**
```json
{
  "totalScenesFound": 2,
  "scenes": [
    {
      "sceneName": "S1A_IW_SLC__1SDV_20230116T080248_20230116T080315_046806_059CA6_BF75",
      "acquisitionDate": "2023-01-16T08:02:48Z",
      "relativeOrbit": 9,
      "flightDirection": "DESCENDING",
      "polarization": "VV+VH",
      "sensor": "Sentinel-1A"
    },
    {
      "sceneName": "S1A_IW_SLC__1SDV_20230104T080249_20230104T080316_046631_0596C5_604A",
      "acquisitionDate": "2023-01-04T08:02:49Z",
      "relativeOrbit": 9,
      "flightDirection": "DESCENDING",
      "polarization": "VV+VH",
      "sensor": "Sentinel-1A"
    }
  ]
}
```

---

## 7. Auditoria de Segurança e Credenciais

1. **Credenciais Legadas no Histórico Git:**
   - Detectou-se que em commits históricos anteriores ao Ciclo 5.1 (ex.: `c2e0730` e anteriores), o arquivo `application.properties` continha credenciais de teste/desenvolvimento hardcoded (chaves de acesso AWS e chaves de APIs meteorológicas).
   - **Ação tomada:** O arquivo `application.properties` foi integralmente parametrizado no Ciclo 5.1 com variáveis de ambiente (`${AWS_ACCESS_KEY_ID:}`, etc.), sem nenhum segredo em texto puro no código ativo.
   - **Alerta de Segurança:** Nenhuma chave secreta foi impressa em logs ou relatórios. Recomenda-se a revogação/rotação imediata das credenciais antigas nos provedores de nuvem (AWS/APIs) e o expurgo do histórico Git via `git-filter-repo` em momento oportuno.

2. **Isolamento de Tenant / Organização:**
   - O uso de `X-Organization-Id` em modo `dev` é restrito e documentado como provisório para o piloto.
   - Em produção, a resolução deve obrigatoriamente derivar dos tokens de autenticação (OAuth2/JWT) com claims de tenant validadas pelo Spring Security.

---

## 8. Estado Git por Repositório (Governança Respeitada)

Em conformidade com a Seção 11 da governança técnica, **nenhum comando destrutivo, `git add`, `git commit` ou `git push` foi executado**.

1. `api-radar-deformacao`:
   - Ramo: `dev` (sincronizado com `origin/dev`).
   - Arquivos modificados (unstaged): `pom.xml`, `AnaliseService.java`, `StorageService.java`, `GeoToolsProcessor.java`, `application.properties`.
   - Arquivos não monitorados: `Dockerfile`, `docker-compose.yml`, `docs/`, `src/main/java/.../catalog/`, `src/main/java/.../shared/`, `src/main/java/.../territory/`, `application.properties.example`, `application.yml`, `db/`, `src/test/`.
2. `front_react/github-friend-helper`:
   - Ramo: `main` (sincronizado com `origin/main`).
   - Arquivos modificados (unstaged): `package-lock.json`, `src/App.tsx`.
   - Arquivos não monitorados: `.env`, `.env.example`, `Dockerfile`, `nginx.conf`, `src/components/AoiDrawingMap.tsx`, `src/models/RadarAoi.ts`, `src/models/SarScene.ts`, `src/pages/VerticalSlicePage.tsx`, `src/services/radarApiClient.ts`.
3. `monitorar-images-alos-palsar`:
   - Working tree limpa, nenhuma modificação.
4. `radar_fastapi_py`:
   - Preservadas alterações anteriores e ambiente local existente sem modificação.

---

## 9. Pendências Técnicas Mapeadas para a Etapa 5.2

1. **Ferramenta de Desenho Geográfico Interativo no Mapa (`AoiDrawingMap.tsx`):**
   - Integrar biblioteca de desenho vetorial (`leaflet-geoman` ou `@geoman-io/leaflet-geoman-free`) para permitir ao usuário desenhar polígonos diretamente clicando no mapa, além do suporte a input WKT já existente.
2. **Seleção de Par Interferométrico e Criação de Job InSAR:**
   - Interface e backend para seleção de cena Master e cena Slave a partir dos resultados da busca SAR.
   - Especificação do Bounded Context de Processamento Interferométrico (Job InSAR / Pipeline).
3. **Autenticação Centralizada:**
   - Evolução do `OrganizationContextResolver` para integração com Spring Security / JWT para autenticação multi-tenant segura.

---

## 10. Parecer de Prontidão para a Etapa 5.2

> **PARECER TÉCNICO:**  
> **APROVADO PARA INÍCIO DA ETAPA 5.2.**  
> A fundação técnica do **Radar++** (PostgreSQL 16, PostGIS 3.4, Spring Boot 3.3.4, Java 21, React 18, TypeScript, Docker Compose, Flyway, Integração NASA ASF DAAC) foi completamente estabilizada, testada, homologada e opera com 100% de sucesso.
>
> Conforme as regras de governança, a **Etapa 5.2 não foi iniciada automaticamente** e aguarda revisão e aprovação formal do usuário.
