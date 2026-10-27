# Radar++ — Relatório de Homologação Técnica do Ciclo 5.3

## 1. Sumário Executivo

O **Ciclo 5.3: Seleção e Validação de Pares Interferométricos** concluiu com sucesso a transformação de cenas SAR isoladas em pares interferométricos candidatos rastreáveis, avaliados de forma automatizada por critérios técnicos e científicos explícitos.

Em estrita consonância com as diretrizes do Radar++:
- **Nenhum processamento interferométrico pesado foi executado** (sem HyP3, sem MintPy, sem geração de interferogramas, sem desdobramento de fase e sem cálculo de deformação milimétrica).
- **Nenhum download de imagens SLC de grande volume foi realizado**.
- **A integridade cartográfica da AOI e as políticas operacionais do Ciclo 5.2 foram integralmente preservadas**.
- **A governança Git foi rigorosamente respeitada** (zero `git add`, zero `git commit`, zero `git push`).

---

## 2. Modelo de Domínio e Arquitetura Hexagonal Implementados

O módulo de domínio `com.ass.br.apiradar.interferometry` foi concebido seguindo Domain-Driven Design (DDD) puro e Arquitetura Hexagonal:

```
src/main/java/com/ass/br/apiradar/interferometry/
├── domain/
│   ├── model/
│   │   ├── PairIdentifier.java              (Value Object: identidade canônica do par)
│   │   ├── PairEligibilityStatus.java       (Enum: 5 estados formais do ciclo de vida)
│   │   ├── PairRejectionReason.java         (Enum: 11 razões explícitas de rejeição)
│   │   ├── BaselineSource.java              (Enum: proveniência dos metadados de B⊥)
│   │   ├── BaselineValue.java               (Value Object: B⊥ distinguindo valor, fonte e disponibilidade)
│   │   ├── PairSelectionPolicy.java         (Value Object: políticas científicas configuráveis e versionáveis)
│   │   ├── PairCompatibilityAssessment.java (Value Object: parecer técnico rastreável e auditável)
│   │   └── InterferometricPair.java         (Entidade / Raiz do Agregado de Par InSAR)
│   ├── ports/
│   │   └── output/
│   │       └── PerpendicularBaselinePort.java (Porta de domínio para obtenção/cálculo de B⊥)
│   └── services/
│       └── PairFormationService.java        (Serviço de domínio para combinação temporal e avaliação)
├── application/
│   ├── dto/
│   │   ├── GeneratePairsRequest.java
│   │   ├── GeneratePairsResponseDto.java
│   │   ├── InterferometricPairDto.java
│   │   └── PairSelectionPolicyDto.java
│   └── services/
│       └── InterferometricPairApplicationService.java (Orquestrador de aplicação com validação topológica)
└── infrastructure/
    └── adapters/
        ├── output/
        │   └── external/
        │       └── AsfBaselineClientAdapter.java (Adaptador HTTP WebClient para API de baseline da ASF)
        └── input/
            └── web/
                └── InterferometricPairRestController.java (Controlador REST oficial da API)
```

---

## 3. Regras de Compatibilidade e Políticas Científicas

### 3.1. Máquina de Estados e Critérios de Transição
1. **`CANDIDATE`:** Formado por combinação temporal cronológica ($t_{\text{ref}} < t_{\text{sec}}$) no mesmo track orbital.
2. **`PRELIMINARILY_COMPATIBLE`:** Atende a modo de aquisição (`IW SLC`), identidade estrita de track, mesma direção de passagem (`ASCENDING` ou `DESCENDING`), compatibilidade de polarização co-polarizada e $\Delta t$ dentro da política.
3. **`PENDING_VALIDATION`:** Par preliminarmente compatível, porém com metadados complementares pendentes (ex: baseline perpendicular não consolidada na fonte ou necessidade de efemérides precisas `POEORB`).
   > **Invariante Mandatório:** Um par cuja linha de base perpendicular não esteja disponível na fonte **NUNCA** é classificado como elegível; ele permanece obrigatoriamente como `PENDING_VALIDATION`.
4. **`REJECTED`:** Rejeitado formalmente, registrando os critérios violados (ex: tracks divergentes, polarizações cruzadas, $\Delta t$ fora da faixa, cobertura $< 80\%$, $B_\perp$ excedido).
5. **`ELIGIBLE_FOR_SUBMISSION`:** Somente quando **todas** as verificações do perfil metodológico estiverem comprovadamente satisfeitas, com $B_\perp$ disponível e dentro do limiar.

### 3.2. Perfis Científicos Versionados e Configuráveis
- **`TROPICAL_URBAN_MACEIO` (v1.0):** $\Delta t \in [12, 24\text{ dias}]$, $|B_\perp| \le 150.0\text{ m}$, sobreposição mínima de $80\%$. Desenvolvido para áreas tropicais e úmidas sujeitas a rápida descorrelação temporal na banda C.
- **`GENERAL_INSAR_S1` (v1.0):** $\Delta t \in [12, 48\text{ dias}]$, $|B_\perp| \le 200.0\text{ m}$, sobreposição mínima de $70\%$.
- **`STRICT_SCIENTIFIC` (v1.0):** $\Delta t \in [6, 36\text{ dias}]$, $|B_\perp| \le 100.0\text{ m}$, sobreposição mínima de $85\%$, com exigência mandatória de órbitas precisas `POEORB`.
- **`CUSTOM`:** Parametrização dinâmica via payload de requisição.

---

## 4. Contratos REST Oficiais

### 4.1. Geração e Avaliação de Pares Candidatos
- **Rota:** `POST /api/v1/interferometry/pairs/candidates`
- **Request Body:**
  ```json
  {
    "aoiId": "76dbb77d-f150-4ca0-a1be-246d8c1b7f2f",
    "startDate": "2023-01-01",
    "endDate": "2023-01-31",
    "flightDirection": "DESCENDING",
    "policyProfile": "TROPICAL_URBAN_MACEIO",
    "expectedGeometryWkt": "POLYGON((-35.795 -9.680, ...))"
  }
  ```
- **Response Body (200 OK):**
  - Contém metadados da AOI, resumo por status, aviso científico oficial e a lista de pares com:
    - Identificador do Par
    - Cenas Referência e Secundária (Nome, Data, Sensor, Track, Polarização)
    - $\Delta t$ (dias)
    - $B_\perp$ (metros, fonte e indicador de disponibilidade)
    - Status de Elegibilidade (`PENDING_VALIDATION`, etc.)
    - Parecer técnico com lista de critérios avaliados e justificativa.

### 4.2. Consulta de Políticas Científicas Disponíveis
- **Rota:** `GET /api/v1/interferometry/policies`
- **Response Body (200 OK):** Lista de perfis científicos parametrizados.

### 4.3. Rota de Catálogo SAR
- **Alinhamento Documental:** Confirmada a rota vigente de catálogo SAR: **`POST /api/v1/sar-catalog/search`**. Qualquer divergência documental em rascunhos preliminares foi devidamente retificada.

---

## 5. Funcionalidades Implementadas no Frontend

1. **Seletor de Perfil Metodológico:** Permite ao operador selecionar o perfil de avaliação (Maceió, Geral, Científico Estrito) com descrição clara dos parâmetros em tempo real.
2. **Disparo de Formação de Pares:** Botão `"Formar e Avaliar Pares InSAR"` acionado após a delimitação da AOI persistida.
3. **Painel de Resumo Analítico:** 4 cards com contadores dinâmicos (*Total Formados*, *Elegíveis*, *Pendentes de Validação*, *Rejeitados*).
4. **Banner de Aviso Científico InSAR:** Alerta explícito informando que os pares não constituem produtos processados e que nenhum cálculo artificial de deslocamento foi efetuado.
5. **Tabela Rica de Pares InSAR:**
   - Exibição paralela das duas cenas (Referência vs Secundária).
   - Metadados de $\Delta t$ e $B_\perp$.
   - Badges coloridos por status semântico.
   - Botão interativo `"Justificativa"` para expandir o parecer técnico rastreável e auditável de cada par.

---

## 6. Resultados Reais dos Testes Automatizados

### 6.1. Suíte Backend Java / Spring Boot / ArchUnit
Comando executado: `./mvnw test`
- **Total de testes:** **61 testes executados**.
- **Falhas:** **0**.
- **Erros:** **0**.
- **Pulos:** **0**.
- **Status:** **BUILD SUCCESS**.

#### Detalhamento dos 10 Cenários de Domínio Obrigatórios:
1. `shouldRejectPairWithIncompatibleTracks`: Rejeição por tracks orbitais distintos (`INCOMPATIBLE_TRACK`).
2. `shouldRejectPairWithIncompatibleFlightDirections`: Rejeição por direções opostas (`INCOMPATIBLE_DIRECTION`).
3. `shouldRejectPairWithIncompatibleBeamModes`: Rejeição por modos incompatíveis (`INCOMPATIBLE_BEAM_MODE`).
4. `shouldRejectPairWithIncompatiblePolarizations`: Rejeição por polarizações não sobrepostas (`INCOMPATIBLE_POLARIZATION`).
5. `shouldRejectDuplicateScenes`: Rejeição de cenas duplicadas (`DUPLICATE_SCENES`).
6. `shouldRejectInvalidTemporalBaseline`: Rejeição por inversão cronológica e $\Delta t$ excedido.
7. `shouldRejectWhenOrbitalMetadataMissing`: Rejeição por ausência de metadados orbitais vitais.
8. `shouldKeepPendingValidationWhenBaselineUnavailable`: Invariante de baseline desconhecida resultando em `PENDING_VALIDATION` (nunca elegível).
9. `shouldRejectPairWithInsufficientSpatialCoverage`: Rejeição por sobreposição $< 80\%$.
10. `shouldApplyDifferentMethodologicalPoliciesCorrectly`: Avaliação divergente entre perfis (rejeitado em Maceió com $\Delta t=36\text{d}$ vs aprovado no Geral).
11. `shouldProvideFullTraceabilityOfCriteriaAndJustifications`: Rastreabilidade de critérios e proveniência.
12. `shouldFormCandidatePairsFromSceneCollection`: Agrupamento e combinação temporal C(n, 2) por track.

### 6.2. Testes Arquiteturais com ArchUnit (`ArchitectureTest`)
- Ausência absoluta de acoplamento circular entre `shared`, `territory`, `catalog` e `interferometry`.
- Isolamento total da camada de domínio de `interferometry` contra frameworks Spring/Web.

### 6.3. Frontend TypeScript & Bundling
Comando executado: `npm run build`
- **Resultado:** Compilação em 2.83s, zero erros de tipagem TypeScript ou de bundling.

### 6.4. Teste de Ponta a Ponta (E2E) via Chrome DevTools Protocol
Comando executado: `node run_e2e_cdp.mjs`
- **Resultado:** **100% de sucesso em 10 etapas automatizadas** (interação nativa com mapa Leaflet, salvamento no PostGIS, consulta ASF, geração de pares InSAR, verificação dos cards, expansão da justificativa técnica e verificação de disclaimers).

---

## 7. Evidências Fotográficas da Jornada E2E
 
- **01_initial_page.png** — Carregamento do sistema e verificação da política de área do backend.
- **02_policy_and_aoi_loaded.png** — Seleção da AOI persistida no PostGIS.
- **03_aoi_geometry_modified_unsaved.png** — Edição de vértice via UI pura e detecção de geometria não salva.
- **04_sar_search_blocked_warning.png** — Bloqueio estrito de operações SAR/InSAR para geometrias modificadas.
- **05_new_aoi_saved_postgis.png** — Persistência da nova geometria no PostGIS com feedback oficial.
- **06_sar_scenes_discovered.png** — Descoberta de cenas Sentinel-1 IW SLC reais na NASA ASF DAAC.
- **07_insar_scientific_disclaimer.png** — Aviso científico de que cenas isoladas não formam pares automaticamente.
- **10_insar_pairs_section.png** — Painel analítico de pares InSAR com cards de resumo e tabela.
- **11_insar_pair_expanded_justification.png** — Parecer técnico rastreável expandido com justificativas detalhadas.
- **12_full_page_cycle_5_3_homologation.png** — Homologação visual da página completa.

---

## 8. Governança Git e Estado dos Repositórios

Em conformidade estrita com as regras do Radar++, **nenhum comando `git add`, `git commit` ou `git push` foi executado**.

### 8.1. Repositório Backend (`api-radar-deformacao`)
- **Branch atual:** `dev` (atualizado com `origin/dev`).
- **Arquivos Preparados para Commit (`staged`):** **Nenhum**.
- **Arquivos Modificados (`unstaged`):**
  - `pom.xml`
  - `src/main/resources/application.properties`
  - `src/main/java/com/ass/br/apiradar/domain/services/AnaliseService.java` *(preexistente legada, fora do ciclo)*
  - `src/main/java/com/ass/br/apiradar/domain/services/StorageService.java` *(preexistente legada, fora do ciclo)*
  - `src/main/java/com/ass/br/apiradar/infrastructure/component/GeoToolsProcessor.java` *(preexistente legada, fora do ciclo)*
- **Arquivos Não Monitorados (`untracked` criados no Ciclo 5.3):**
  - `src/main/java/com/ass/br/apiradar/interferometry/` (Módulo completo de pares InSAR)
  - `src/test/java/com/ass/br/apiradar/interferometry/` (Suíte de testes de pares InSAR)
  - `docs/architecture/cycle-05/05-relatorio-homologacao-ciclo-5-3.md`

### 8.2. Repositório Frontend (`front_react/github-friend-helper`)
- **Branch atual:** `main` (atualizado com `origin/main`).
- **Arquivos Preparados para Commit (`staged`):** **Nenhum**.
- **Arquivos Modificados (`unstaged`):**
  - `package-lock.json`
  - `src/App.tsx`
- **Arquivos Não Monitorados (`untracked` atualizados no Ciclo 5.3):**
  - `src/models/InterferometricPair.ts`
  - `src/services/radarApiClient.ts`
  - `src/components/AoiDrawingMap.tsx`

---

## 9. Limitações Científicas e Pendências para o Processamento Futuro (Ciclo 5.4+)

1. **Dependência de Efemérides Orbitais Precisas (POEORB):** A ESA consolida as órbitas definitivas com janela de 20 a 25 dias após a aquisição. Pares recentes permanecem em `PENDING_VALIDATION` até a sincronização desses arquivos.
2. **Download e Extração de Bursts:** Para o processamento InSAR fino no MintPy/ISCE2, não é necessário baixar toda a cena de 4GB, mas sim os bursts individuais que interceptam a AOI.
3. **Ausência de Inversão e Interferogramas:** Esta etapa limita-se à pré-qualificação orbital e temporal. Não foram gerados mapas de coerência ou velocidades de deformação.

O Ciclo 5.3 está integralmente concluído e homologado, aguardando aprovação do responsável pelo Radar++.
