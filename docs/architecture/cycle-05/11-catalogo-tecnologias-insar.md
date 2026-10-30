# Radar++ — Catálogo Oficial de Tecnologias, Bibliotecas e Governança Técnica (v0.1)

**Documento:** `11-catalogo-tecnologias-insar.md`  
**Ciclo:** Auditoria Pré-Commit e Consolidação da Baseline Técnica v0.1  
**Data:** 26 de Outubro de 2026  
**Status:** **Oficial — Homologado**  
**Repositório Oficial:** `api-radar-deformacao`  

---

## 1. Visão Geral e Governança de Dependências

O presente catálogo consolida todas as tecnologias, frameworks e bibliotecas adotadas na construção da primeira baseline estável do **Radar++** (Ciclos 2, 3, 4 e 5.1 a 5.4A), bem como as tecnologias candidatas homologadas para os próximos ciclos de processamento científico (Ciclos 5.4B e 5.5).

A governança do Radar++ estabelece que:
- Nenhuma dependência é introduzida no repositório sem justificativa técnica explícita, avaliação de licenças comerciais/open source e análise de acoplamento arquitetural.
- É mantida distinção rigorosa entre **entidade mantenedora**, **proprietário/patrocinador** e **comunidade de desenvolvimento**.
- As dependências são categorizadas estritamente em: **Instalada e Utilizada**, **Instalada e em Validação (Testes)** ou **Candidata Planejada (Sem instalação prematura)**.

---

## 2. Tecnologias do Backend (`api-radar-deformacao` — Java 21 / Spring Boot)

### 2.1. Spring Boot Web & Actuator
1. **Nome e Versão:** Spring Boot Starter Web & Actuator `3.3.4`
2. **Entidade Mantenedora:** VMware Tanzu / Broadcom (proprietário comercial e patrocinador principal); Comunidade Spring / Open Source (colaboradores ativos).
3. **Licença:** Apache License 2.0 (permissiva comercial).
4. **Problema Resolvido no Radar++:** Fundação do monólito modular: injeção de dependências, ciclo de vida de componentes, servidor HTTP embutido (Apache Tomcat), endpoints REST e observabilidade técnica/saúde operacional.
5. **Justificativa da Escolha:** Padrão corporativo consolidado com suporte nativo a Java 21 (Virtual Threads), excelente ecossistema para validação declarativa (Jakarta Validation) e instrumentação com Actuator.
6. **Alternativas Consideradas:** Quarkus 3, Micronaut 4, Ktor. O Spring Boot foi escolhido por maturidade de ecossistema com Hibernate Spatial e equipe.
7. **Módulos que a Utilizam:** `shared`, `territory`, `catalog`, `interferometry`.
8. **Imports e Classes Representativos:**
   - `org.springframework.boot.SpringApplication`
   - `org.springframework.web.bind.annotation.RestController`
   - `org.springframework.boot.actuate.health.HealthIndicator`
9. **Status de Adoção:** **Instalada e Utilizada** (ativa em produção e testes).

---

### 2.2. Spring Boot WebFlux (`WebClient`)
1. **Nome e Versão:** Spring Boot Starter WebFlux `3.3.4`
2. **Entidade Mantenedora:** VMware Tanzu / Broadcom (patrocinador corporativo); Project Reactor Community.
3. **Licença:** Apache License 2.0.
4. **Problema Resolvido no Radar++:** Comunicação HTTP não-bloqueante e resiliente com APIs geoespaciais e de catálogo externas (NASA ASF DAAC e ASF Baseline API).
5. **Justificativa da Escolha:** Suporte nativo a timeouts finos (`Duration.ofSeconds`), buffers de memória configuráveis para payloads GeoJSON e desacoplamento de I/O em relação à thread pool da requisição.
6. **Alternativas Consideradas:** `RestTemplate` (legado e em manutenção), Apache HttpClient 5, Java native `HttpClient`. O `WebClient` foi escolhido por integração fluida com o Spring.
7. **Módulos que a Utilizam:** `catalog` (`AsfSearchClientAdapter`), `interferometry` (`AsfBaselineClientAdapter`, `Hyp3InSarProcessorAdapter`).
8. **Imports e Classes Representativos:**
   - `org.springframework.web.reactive.function.client.WebClient`
   - `reactor.core.publisher.Mono`
9. **Status de Adoção:** **Instalada e Utilizada** (ativa nos adaptadores de saída de infraestrutura).

---

### 2.3. LocationTech JTS Topology Suite
1. **Nome e Versão:** JTS Topology Suite `1.19.0` (`org.locationtech.jts:jts-core`)
2. **Entidade Mantenedora:** Eclipse Foundation (governança de propriedade intelectual e curadoria); LocationTech Working Group; Comunidade Open Source.
3. **Licença:** Eclipse Public License 2.0 (EPL-2.0) e Eclipse Distribution License 1.0 (EDL-1.0 / BSD 3-Clause).
4. **Problema Resolvido no Radar++:** Representação em memória e validação formal de invariantes geométricas no domínio (`isSimple()`, `isValid()`, fechamento de anéis de polígonos, interseções planas e envelopes BBOX).
5. **Justificativa da Escolha:** É o padrão soberano para geometria computacional na JVM (base de GeoTools, PostGIS JDBC e Hibernate Spatial), garantindo cálculo topológico livre de acoplamento a banco de dados.
6. **Alternativas Consideradas:** ESRI Geometry API for Java, GeoTools puro. JTS é mais leve, puro e desprovido de dependências transitivas pesadas.
7. **Módulos que a Utilizam:** `shared` (`BoundingBox`), `territory` (`RadarAoi`, `AoiSpatialService`), `catalog` (`SarScene`), `interferometry` (`BurstIdentifier`).
8. **Imports e Classes Representativos:**
   - `org.locationtech.jts.geom.Polygon`
   - `org.locationtech.jts.geom.GeometryFactory`
   - `org.locationtech.jts.io.WKTReader`
9. **Status de Adoção:** **Instalada e Utilizada** (núcleo do domínio espacial).

---

### 2.4. Hibernate Spatial & Spring Data JPA
1. **Nome e Versão:** Hibernate Spatial `6.5.3.Final` & Spring Data JPA `3.3.4`
2. **Entidade Mantenedora:** Red Hat / IBM (patrocinador comercial); Hibernate Community (open source).
3. **Licença:** GNU Lesser General Public License 2.1 (LGPL-2.1) — permissiva paraリンク dinâmico de bibliotecas sem contaminação do código proprietário.
4. **Problema Resolvido no Radar++:** Persistência transparente e consulta geoespacial de polígonos de AOI no PostgreSQL/PostGIS (`geometry(Polygon, 4326)`).
5. **Justificativa da Escolha:** Suporte nativo a predicados espaciais OGC (`ST_Intersects`, `ST_Area`, `ST_Transform`) mapeados diretamente para entidades Java sem necessidade de conversão manual de binários WKB/WKT.
6. **Alternativas Consideradas:** MyBatis com extensões espaciais, JDBC puro com `PGgeometry`, JOOQ. O Hibernate Spatial foi selecionado pela maturidade e compatibilidade com Spring Data.
7. **Módulos que a Utilizam:** `territory` (`RadarAoiJpaEntity`, `RadarAoiJpaRepository`).
8. **Imports e Classes Representativos:**
   - `org.hibernate.spatial.SpatialDialect`
   - `org.locationtech.jts.geom.Geometry`
   - `jakarta.persistence.Entity`
9. **Status de Adoção:** **Instalada e Utilizada**.

---

### 2.5. PostgreSQL JDBC Driver
1. **Nome e Versão:** PostgreSQL JDBC Driver `42.7.4`
2. **Entidade Mantenedora:** PostgreSQL Global Development Group (comunidade mundial aberta de software livre).
3. **Licença:** BSD-2-Clause.
4. **Problema Resolvido no Radar++:** Conector de comunicação binária JDBC entre a JVM e o banco relacional PostGIS.
5. **Justificativa da Escolha:** Driver oficial de alta estabilidade, mantido diretamente pelo core do PostgreSQL, com suporte total a streaming binário, SSL e pooling via HikariCP.
6. **Alternativas Consideradas:** R2DBC PostgreSQL (reativo). Não adotado para persistência relacional principal devido à maturidade de transações ACID e Hibernate Spatial no ecossistema JDBC tradicional.
7. **Módulos que a Utilizam:** `shared`, `territory` (camada de infraestrutura de dados).
8. **Imports e Classes Representativos:**
   - `org.postgresql.Driver`
9. **Status de Adoção:** **Instalada e Utilizada**.

---

### 2.6. Flyway Database Migrations
1. **Nome e Versão:** Flyway Core & Flyway Database PostgreSQL `10.15.0`
2. **Entidade Mantenedora:** Redgate Software (proprietário comercial); Comunidade Open Source Flyway.
3. **Licença:** Apache License 2.0 (Community Edition).
4. **Problema Resolvido no Radar++:** Versionamento auditável e aplicação automatizada do esquema relacional e extensões PostGIS (`CREATE EXTENSION IF NOT EXISTS postgis`).
5. **Justificativa da Escolha:** Garantia de idempotência e reprodutibilidade perfeita do banco em qualquer ambiente (local Docker, CI/CD, staging e produção), prevenindo divergências manuais de DDL.
6. **Alternativas Consideradas:** Liquibase, Hibernate `hbm2ddl.auto=update` (proibido em produção por risco de perda de dados). Flyway foi escolhido pela clareza do SQL puro.
7. **Módulos que a Utilizam:** `api-radar-deformacao` (raiz do ciclo de vida de persistência).
8. **Imports e Classes Representativos:**
   - `org.flywaydb.core.Flyway`
   - Arquivos `src/main/resources/db/migration/V1__initial_schema.sql`
9. **Status de Adoção:** **Instalada e Utilizada**.

---

### 2.7. Jackson Core & Databind
1. **Nome e Versão:** FasterXML Jackson `2.17.2`
2. **Entidade Mantenedora:** FasterXML LLC (mantenedor principal Tatu Saloranta); Comunidade Open Source.
3. **Licença:** Apache License 2.0.
4. **Problema Resolvido no Radar++:** Serialização e desserialização de representações JSON/GeoJSON, construção da árvore sintática dos payloads externos HyP3 e parsing de respostas REST da NASA ASF.
5. **Justificativa da Escolha:** Performance de topo na JVM, facilidade de manipulação de nós dinâmicos (`JsonNode`, `ObjectNode`) e suporte integral a Java Records.
6. **Alternativas Consideradas:** Google Gson, Org.Json, Moshi. Jackson é a biblioteca padrão do ecossistema Spring.
7. **Módulos que a Utilizam:** `shared`, `catalog`, `interferometry`.
8. **Imports e Classes Representativos:**
   - `com.fasterxml.jackson.databind.ObjectMapper`
   - `com.fasterxml.jackson.databind.JsonNode`
9. **Status de Adoção:** **Instalada e Utilizada**.

---

### 2.8. NetworkNT JSON Schema Validator
1. **Nome e Versão:** JSON Schema Validator `1.4.0` (`com.networknt:json-schema-validator`)
2. **Entidade Mantenedora:** Network New Technologies Inc. (organização open source de microserviços de alta performance).
3. **Licença:** Apache License 2.0.
4. **Problema Resolvido no Radar++:** Validação programática estrita dos payloads de processamento InSAR contra os schemas OpenAPI v3.0 oficiais da NASA ASF HyP3 em tempo de teste unitário e de integração.
5. **Justificativa da Escolha:** Suporte estrito a JSON Schema Draft 7 / Draft 2020-12, detecção de violações de `additionalProperties: false` e `required`, sem necessidade de invocar a rede externa.
6. **Alternativas Consideradas:** Everit JSON Schema, Justify. O NetworkNT possui menor pegada de memória e ampla adoção em validação OpenAPI.
7. **Módulos que a Utilizam:** `interferometry` (testes automatizados de conformidade contratual).
8. **Imports e Classes Representativos:**
   - `com.networknt.schema.JsonSchema`
   - `com.networknt.schema.JsonSchemaFactory`
   - `com.networknt.schema.ValidationMessage`
9. **Status de Adoção:** **Instalada e Utilizada** (escopo de teste e garantia de qualidade).

---

### 2.9. ArchUnit
1. **Nome e Versão:** ArchUnit JUnit 5 `1.3.0` (`com.tngtech.archunit:archunit-junit5`)
2. **Entidade Mantenedora:** TNG Technology Consulting GmbH (Alemanha); Comunidade Open Source.
3. **Licença:** Apache License 2.0.
4. **Problema Resolvido no Radar++:** Garantia automatizada da Arquitetura Hexagonal e independência dos módulos: impede ciclos entre pacotes, proíbe acesso direto a camadas internas e restringe frameworks ao exterior do domínio.
5. **Justificativa da Escolha:** Transforma regras arquiteturais em testes executáveis via `./mvnw test`, blindando o monólito contra regressões de acoplamento estrutural.
6. **Alternativas Consideradas:** JDepend, SonarQube rules, Checkstyle. ArchUnit permite asserções ricas em Java idiomático direto nos testes de unidade.
7. **Módulos que a Utilizam:** `api-radar-deformacao` (`src/test/java/.../architecture`).
8. **Imports e Classes Representativos:**
   - `com.tngtech.archunit.junit.AnalyzeClasses`
   - `com.tngtech.archunit.junit.ArchTest`
   - `com.tngtech.archunit.lang.ArchRule`
9. **Status de Adoção:** **Instalada e Utilizada**.

---

### 2.10. Testcontainers PostgreSQL
1. **Nome e Versão:** Testcontainers `1.20.4` (`org.testcontainers:postgresql`)
2. **Entidade Mantenedora:** AtomicJar / Docker Inc. (proprietário comercial); Comunidade Open Source Testcontainers.
3. **Licença:** MIT License.
4. **Problema Resolvido no Radar++:** Execução de testes de integração realistas com contêiner descartável PostgreSQL/PostGIS via Docker, garantindo que queries espaciais reais sejam validadas em ambiente idêntico à produção.
5. **Justificativa da Escolha:** Elimina a necessidade de bancos H2 em memória com dialetos espaciais incompletos ou emuladores sintéticos que mascaram bugs de PostGIS.
6. **Alternativas Consideradas:** H2 com modo PostgreSQL (não suporta funções geodésicas avançadas do PostGIS como `ST_MakeValid` ou projeções SIRGAS/UTM reais).
7. **Módulos que a Utilizam:** `territory`, `interferometry` (testes de integração).
8. **Imports e Classes Representativos:**
   - `org.testcontainers.containers.PostgreSQLContainer`
   - `org.testcontainers.junit.jupiter.Container`
9. **Status de Adoção:** **Instalada e Utilizada**.

---

## 3. Tecnologias do Frontend (`front_react/github-friend-helper` — React 18 / TypeScript)

### 3.1. React & React DOM
1. **Nome e Versão:** React `18.3.1` & React DOM `18.3.1`
2. **Entidade Mantenedora:** Meta Platforms Inc. (patrocinador corporativo); Comunidade Open Source React.
3. **Licença:** MIT License.
4. **Problema Resolvido no Radar++:** Renderização reativa declarativa dos componentes da interface de monitoramento geodésico (desenho de AOI, listagem de cenas, visualização de compatibilidade de pares e status de dry-run).
5. **Justificativa da Escolha:** Ecossistema maduro para GIS web, hooks modernos para controle de estado cartográfico e compatibilidade com o ecossistema Radix/Tailwind.
6. **Alternativas Consideradas:** Angular 17+ (utilizado no protótipo legado e descontinuado devido à rigidez e baixa flexibilidade com Leaflet reativo), Vue 3, Svelte.
7. **Módulos que a Utilizam:** Raiz do frontend (`src/App.tsx`, `src/pages/`, `src/components/`).
8. **Imports e Classes Representativos:**
   - `import React, { useState, useEffect, useCallback } from 'react'`
   - `import ReactDOM from 'react-dom/client'`
9. **Status de Adoção:** **Instalada e Utilizada**.

---

### 3.2. TypeScript
1. **Nome e Versão:** TypeScript `5.5.3`
2. **Entidade Mantenedora:** Microsoft Corporation (mantenedor principal); Comunidade Open Source TypeScript.
3. **Licença:** Apache License 2.0.
4. **Problema Resolvido no Radar++:** Tipagem estrita dos contratos de dados frontend/backend: modelos de AOI, geometrias GeoJSON, metadados orbitais de cenas SAR e DTOs de pares interferométricos.
5. **Justificativa da Escolha:** Detecção estática de erros em tempo de compilação, prevenindo falhas de formato no consumo das APIs REST do Spring Boot.
6. **Alternativas Consideradas:** JavaScript puro com JSDoc. Inviável para aplicações com precisão geodésica e contratos complexos.
7. **Módulos que a Utilizam:** 100% da base de código do frontend (`.ts`, `.tsx`).
8. **Imports e Classes Representativos:**
   - `export interface RadarAoi { ... }`
   - `export interface InterferometricPair { ... }`
9. **Status de Adoção:** **Instalada e Utilizada**.

---

### 3.3. Vite Build Tool
1. **Nome e Versão:** Vite `5.4.1`
2. **Entidade Mantenedora:** Evan You e Equipe do Vite; Comunidade Open Source.
3. **Licença:** MIT License.
4. **Problema Resolvido no Radar++:** Servidor de desenvolvimento ultrarrápido com Hot Module Replacement (HMR) e empacotamento de produção otimizado com Rollup/ESBuild.
5. **Justificativa da Escolha:** Tempos de inicialização quase instantâneos (< 300ms) e geração de bundles de alta performance sem complexidade de Webpack legada.
6. **Alternativas Consideradas:** Create-React-App / Webpack (descontinuado e lento), Turbopack / Next.js (desnecessário para SPA estática servida em contêiner Nginx).
7. **Módulos que a Utilizam:** Infraestrutura de build do frontend (`vite.config.ts`).
8. **Imports e Classes Representativos:**
   - `import { defineConfig } from "vite"`
9. **Status de Adoção:** **Instalada e Utilizada**.

---

### 3.4. Leaflet & React-Leaflet
1. **Nome e Versão:** Leaflet `1.9.4` & React-Leaflet `4.2.1`
2. **Entidade Mantenedora:** Vladimir Agafonkin (autor original); Leaflet Organization / Comunidade Open Source; Paul Le Cam (React-Leaflet).
3. **Licença:** BSD-2-Clause.
4. **Problema Resolvido no Radar++:** Visualização cartográfica interativa em 2D, renderização de camadas de tiles OSM/Satélite, desenho interativo de polígonos de AOI, manipulação de vértices e projeção de footprints de satélite.
5. **Justificativa da Escolha:** Biblioteca cartográfica web mais estável e leve do mundo (~40 kB), com vasta documentação, sem exigir tokens pagos de terceiros (como Mapbox GL).
6. **Alternativas Consideradas:** OpenLayers (mais pesada, mantida como candidata futura para renderização nativa de GeoTIFF/COG via WebGL), Mapbox GL JS (restrições de licença e custo por requisição).
7. **Módulos que a Utilizam:** `AoiDrawingMap.tsx`, `VerticalSlicePage.tsx`.
8. **Imports e Classes Representativos:**
   - `import L from 'leaflet'`
   - `import { MapContainer, TileLayer, Polygon, FeatureGroup } from 'react-leaflet'`
9. **Status de Adoção:** **Instalada e Utilizada**.

---

### 3.5. Tailwind CSS & shadcn/ui (Radix UI)
1. **Nome e Versão:** Tailwind CSS `3.4.11` & Radix UI Primitives (vários pacotes `@radix-ui/react-*` ~ `1.x`)
2. **Entidade Mantenedora:** Tailwind Labs Inc. (proprietário comercial); WorkOS (mantenedora do Radix UI); Comunidade shadcn.
3. **Licença:** MIT License.
4. **Problema Resolvido no Radar++:** Sistema de design moderno, consistente, altamente responsivo e acessível para o painel de monitoramento geodésico e painéis de controle InSAR.
5. **Justificativa da Escolha:** Elimina CSS desorganizado, fornece componentes primitivos sem estilo (Radix UI) que garantem acessibilidade WAI-ARIA completa e flexibilidade total de estilização.
6. **Alternativas Consideradas:** Material UI / MUI (muito opinativo e pesado), Ant Design (acoplamento estético rígido), Bootstrap.
7. **Módulos que a Utilizam:** `src/components/ui/`, `src/pages/`.
8. **Imports e Classes Representativos:**
   - `import { Button } from "@/components/ui/button"`
   - `import { Badge } from "@/components/ui/badge"`
9. **Status de Adoção:** **Instalada e Utilizada**.

---

### 3.6. TanStack Query (React Query)
1. **Nome e Versão:** TanStack Query `5.56.2` (`@tanstack/react-query`)
2. **Entidade Mantenedora:** Tanner Linsley / TanStack Organization; Comunidade Open Source.
3. **Licença:** MIT License.
4. **Problema Resolvido no Radar++:** Gerenciamento do estado assíncrono de servidor no cliente: caching inteligente, deduplicação de requisições, refetch em background e sincronização com a API Spring Boot.
5. **Justificativa da Escolha:** Evita código boilerplate com `useEffect`/`useState` para chamadas HTTP, fornecendo estados explícitos de `isLoading`, `isError`, `data` e `refetch`.
6. **Alternativas Consideradas:** Redux Toolkit / RTK Query (complexidade desnecessária para o escopo), SWR, fetch manual.
7. **Módulos que a Utilizam:** `src/App.tsx`, `src/pages/VerticalSlicePage.tsx`.
8. **Imports e Classes Representativos:**
   - `import { QueryClient, QueryClientProvider, useQuery } from '@tanstack/react-query'`
9. **Status de Adoção:** **Instalada e Utilizada**.

---

### 3.7. Lucide React
1. **Nome e Versão:** Lucide React `0.462.0`
2. **Entidade Mantenedora:** Lucide Project (continuação do Feather Icons pela comunidade aberta).
3. **Licença:** ISC License (permissiva).
4. **Problema Resolvido no Radar++:** Iconografia técnica padronizada para ferramentas cartográficas (polígono, camada, satélite, órbita, zoom, status de conformidade e erros).
5. **Justificativa da Escolha:** Ícones SVG limpos e estritamente tree-shakable (apenas os ícones importados entram no bundle final).
6. **Alternativas Consideradas:** FontAwesome, React Icons, Material Icons. Lucide possui melhor alinhamento estético com shadcn/ui e menor pegada.
7. **Módulos que a Utilizam:** Componentes da UI e `AoiDrawingMap.tsx`.
8. **Imports e Classes Representativos:**
   - `import { MapPin, Layers, Satellite, ShieldCheck, AlertCircle } from 'lucide-react'`
9. **Status de Adoção:** **Instalada e Utilizada**.

---

## 4. Tecnologias Científicas Candidatas Futuras (Ciclos 5.4B e 5.5)

*As tecnologias desta seção foram avaliadas e aprovadas conceitualmente na arquitetura do Radar++, porém **NÃO estão instaladas no código atual** para prevenir acoplamentos prematuros, complexidade de build e custos desnecessários antes de seus respectivos ciclos de execução real.*

---

### 4.1. `hyp3_sdk` (Python SDK para HyP3)
1. **Nome e Versão Alvo:** `hyp3_sdk >= 6.0.0`
2. **Entidade Mantenedora:** Alaska Satellite Facility (NASA ASF) / University of Alaska Fairbanks.
3. **Licença:** BSD-3-Clause.
4. **Problema Resolvido no Radar++:** Automação do despacho e monitoramento assíncrono de jobs InSAR reais na nuvem ASF quando o processamento for autorizado.
5. **Justificativa da Escolha:** SDK oficial do provedor da NASA, que abstrai autenticação Earthdata Login, paginação e polling de status de execução.
6. **Alternativas Consideradas:** Chamadas HTTP REST puras em Java. O SDK Python será utilizado em jobs assíncronos e scripts de automação.
7. **Módulos que a Utilizarão:** Worker assíncrono InSAR / CLI de automação (Ciclo 5.4B).
8. **Imports e Classes Previstos:**
   - `import hyp3_sdk as hyp3`
9. **Status de Adoção:** **Candidata Planejada (Não Instalada)**.

---

### 4.2. `MintPy` (Miami INsar Time-series software in PYthon)
1. **Nome e Versão Alvo:** `MintPy >= 1.5.3`
2. **Entidade Mantenedora:** Yunjun Zhang, Falk Amelung e comunidade acadêmica internacional (Universidade de Miami / NASA JPL / Caltech).
3. **Licença:** GNU General Public License v3.0 (GPL-3.0).
4. **Problema Resolvido no Radar++:** Inversão temporal multitemporal de redes de interferogramas (Small Baseline Subset — SBAS) para gerar taxas de deformação em $\text{mm/ano}$ sobre a área urbana de Maceió.
5. **Justificativa da Escolha:** Software de referência global em geodésia por radar para análise de deformações de terreno com correção atmosférica (ERA5/GACOS) e fechamento de loops de fase.
6. **Alternativas Consideradas:** StaMPS (para PS-InSAR em MATLAB/C++), GIAnT (descontinuado). MintPy é o padrão aberto moderno mantido ativamente.
7. **Módulos que a Utilizarão:** Worker de Processamento Científico em contêiner Docker isolado (Ciclo 5.5 — ADR-002: sem contaminação GPL no monólito Java).
8. **Imports e Classes Previstos:**
   - `from mintpy.cli import smallbaselineApp`
9. **Status de Adoção:** **Candidata Planejada (Não Instalada)**.

---

### 4.3. `asf_search` (Python)
1. **Nome e Versão Alvo:** `asf_search >= 7.1.0`
2. **Entidade Mantenedora:** Alaska Satellite Facility (NASA ASF).
3. **Licença:** BSD-3-Clause.
4. **Problema Resolvido no Radar++:** Consultas complexas em lote a catálogos de metadados SAR, identificação de baselines perpendiculares em lote e download de efemérides orbitais precisas.
5. **Justificativa da Escolha:** Biblioteca oficial recomendada pela NASA ASF com suporte nativo a bursts Sentinel-1 e filtros por track/subswath.
6. **Alternativas Consideradas:** Consultas manuais via curl / API REST.
7. **Módulos que a Utilizarão:** Scripts de extração científica e apoio ao pipeline InSAR (Ciclo 5.4B).
8. **Imports e Classes Previstos:**
   - `import asf_search as asf`
9. **Status de Adoção:** **Candidata Planejada (Não Instalada)**.

---

### 4.4. `GDAL` & `rasterio`
1. **Nome e Versão Alvo:** GDAL `>= 3.8.0` & rasterio `>= 1.3.10`
2. **Entidade Mantenedora:** Open Source Geospatial Foundation (OSGeo) / Mapbox.
3. **Licença:** MIT License / BSD-3-Clause.
4. **Problema Resolvido no Radar++:** Reprojeção cartográfica de interferogramas para UTM 25S (`EPSG:31985`) e geração de Cloud Optimized GeoTIFFs (COGs) com pirâmides internas para entrega web otimizada.
5. **Justificativa da Escolha:** Ferramenta padrão da indústria de sensoriamento remoto para manipulação matricial e reamostragem espacial.
6. **Alternativas Consideradas:** GeoTools puro em Java. GDAL em C++/Python é ordens de magnitude mais rápido e eficiente para grandes matrizes raster SAR.
7. **Módulos que a Utilizarão:** Worker de Pós-Processamento Raster (Ciclo 5.5).
8. **Imports e Classes Previstos:**
   - `from osgeo import gdal`
   - `import rasterio`
9. **Status de Adoção:** **Candidata Planejada (Não Instalada)**.

---

### 4.5. `geotiff.js` (TypeScript)
1. **Nome e Versão Alvo:** `geotiff.js >= 2.1.3`
2. **Entidade Mantenedora:** Fabian Schindler e comunidade open source de visualização geoespacial.
3. **Licença:** MIT License.
4. **Problema Resolvido no Radar++:** Leitura seletiva de bytes de COGs via HTTP Range Requests diretamente no navegador a partir do S3 privado, permitindo inspecionar valores exatos de deformação em milímetros nos pixels selecionados.
5. **Justificativa da Escolha:** Permite inspeção radiométrica e geodésica em tempo real no cliente web sem a necessidade de manter servidores dinâmicos de tiles (TiTiler) em execução contínua (ADR-004).
6. **Alternativas Consideradas:** Servidor dinâmico TiTiler / GeoServer (aumentaria drasticamente os custos fixos de nuvem).
7. **Módulos que a Utilizarão:** Frontend de Visualização InSAR (Ciclo 5.5).
8. **Imports e Classes Previstos:**
   - `import { fromUrl } from 'geotiff'`
9. **Status de Adoção:** **Candidata Planejada (Não Instalada)**.

---

## 5. Matriz Resumo de Licenciamento e Conformidade Legal

| Componente / Biblioteca | Escopo no Radar++ | Licença | Mantenedor Principal | Risco de Contaminação no Monólito |
| :--- | :--- | :--- | :--- | :---: |
| **Spring Boot / WebFlux** | Backend (Ativo) | Apache-2.0 | VMware Tanzu / Broadcom | **Nenhum** (Permissiva comercial) |
| **JTS Topology Suite** | Backend (Ativo) | EPL-2.0 / EDL-1.0 | Eclipse Foundation | **Nenhum** (Permissiva de biblioteca) |
| **Hibernate Spatial** | Backend (Ativo) | LGPL-2.1 | Red Hat / Hibernate Org | **Nenhum** (Link dinâmico de dependência) |
| **PostgreSQL JDBC** | Backend (Ativo) | BSD-2-Clause | PostgreSQL Global Dev Group | **Nenhum** (Permissiva) |
| **Flyway Core** | Backend (Ativo) | Apache-2.0 | Redgate Software | **Nenhum** (Permissiva) |
| **Jackson Databind** | Backend (Ativo) | Apache-2.0 | FasterXML LLC | **Nenhum** (Permissiva) |
| **NetworkNT Schema** | Backend (Testes) | Apache-2.0 | Network New Technologies | **Nenhum** (Permissiva) |
| **ArchUnit** | Backend (Testes) | Apache-2.0 | TNG Technology Consulting | **Nenhum** (Permissiva) |
| **Testcontainers** | Backend (Testes) | MIT | AtomicJar / Docker Inc. | **Nenhum** (Permissiva) |
| **React & React DOM** | Frontend (Ativo) | MIT | Meta Platforms Inc. | **Nenhum** (Permissiva) |
| **TypeScript** | Frontend (Ativo) | Apache-2.0 | Microsoft Corporation | **Nenhum** (Compilador estático) |
| **Vite** | Frontend (Ativo) | MIT | Evan You / Vite Core | **Nenhum** (Ferramenta de build) |
| **Leaflet & React-Leaflet** | Frontend (Ativo) | BSD-2-Clause | Vladimir Agafonkin / Comunidade | **Nenhum** (Permissiva) |
| **Tailwind CSS & Radix UI** | Frontend (Ativo) | MIT | Tailwind Labs / WorkOS | **Nenhum** (Permissiva) |
| **TanStack Query** | Frontend (Ativo) | MIT | TanStack Org | **Nenhum** (Permissiva) |
| **Lucide React** | Frontend (Ativo) | ISC | Lucide Project | **Nenhum** (Permissiva) |
| **hyp3_sdk** | Futuro (Ciclo 5.4B) | BSD-3-Clause | NASA ASF | **Nenhum** (Permissiva) |
| **MintPy** | Futuro (Ciclo 5.5) | GPL-3.0 | Univ. Miami / NASA JPL | **Isolado** em container worker assíncrono (ADR-002) |
| **GDAL / rasterio** | Futuro (Ciclo 5.5) | MIT / BSD | OSGeo / Mapbox | **Nenhum** (Permissiva) |
| **geotiff.js** | Futuro (Ciclo 5.5) | MIT | Fabian Schindler | **Nenhum** (Permissiva) |
