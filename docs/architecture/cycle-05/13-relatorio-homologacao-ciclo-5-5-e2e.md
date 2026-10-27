# Radar++ — Relatório de Homologação Funcional Local e Aceite Visual (Ciclos 5.5, 5.5.1 e 5.5.2)

## 1. Sumário Executivo

O **Ciclo 5.5, 5.5.1 e 5.5.2: Integridade do Dry-Run e Estabilização E2E** concluiu com sucesso a homologação integrada e ponta a ponta de todo o ecossistema local do **Radar++**, contemplando:
1. Camada de persistência e cálculo espacial no **PostgreSQL 16 com PostGIS 3.4**.
2. Monólito modular no backend (**Spring Boot 3.3.4 / Java 21**).
3. Interface de usuário no frontend (**React 18 / TypeScript / Vite / Leaflet** sob Nginx reverse proxy).
4. Integração resiliente com o catálogo da **NASA ASF DAAC** (Alaska Satellite Facility), serviço de baseline perpendicular e catálogo público de bursts Sentinel-1.
5. Operação de preparação de processamento InSAR (**Modo Dry-Run**) disponível diretamente na UI para o operador, com eliminação completa de bursts hardcoded e cálculo dinâmico de cobertura real via footprints.
6. Suporte estrito às modalidades **Single-Burst (`INSAR_ISCE_BURST`)**, **Multi-Burst (`INSAR_ISCE_MULTI_BURST`)** com separação estrita de conjuntos de referência e secundário, e **Full-Scene (`INSAR_GAMMA`)**.
7. Convergência matemática e autoritativa do cálculo de área geodésica entre PostGIS, Backend e Frontend, com testes de geometrias complexas e polígonos com múltiplos anéis (buracos).
8. Inspeção visual responsiva completa em três resoluções reais de tela (**Desktop 1440x1100**, **Tablet 768x1024**, **Celular 375x812**).
9. Saneamento automatizado e seguro da suíte E2E em navegador Chrome real (CDP), operando com **registro estrito de UUIDs** (sem exclusão destrutiva por prefixo `LIKE`) e proteção inviolável da AOI oficial de Maceió (`1e7a1465-c3d7-44e6-8398-5ced7dac17ec`).
10. Modularização limpa do frontend, extraindo o painel de preparação para `InSarProcessingPanel.tsx` e assegurando invalidação imediata de estado ao alterar AOI, par, datas, perfil ou ao modificar a geometria no mapa.

Todos os testes foram executados com a trava de segurança financeira e de nuvem ativa (`radar.processing.hyp3.execution-enabled=false`), assegurando custo zero e sem submissão de chamadas a processadores reais.

---

## 2. Inventário de Serviços, Redes e Portas

| Serviço | Imagem / Runtime | Container | Porta Externa | Porta Interna | Estado Operacional |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Banco Espacial (PostGIS)** | `postgis/postgis:16-3.4` | `radar_postgres` | `5435` | `5432` | **UP (Healthy)** |
| **Backend Monólito Modular** | `eclipse-temurin:21-jre-jammy` | `radar_backend` | `8080` | `8080` | **UP (Healthy)** |
| **Frontend Web SPA + Proxy** | `nginx:alpine` | `radar_frontend` | `3000` | `80` | **UP (Healthy)** |

- **CORS Configurado:** `http://localhost:5173,http://localhost:3000,http://localhost:80`
- **Reverse Proxy Nginx:** `/api/v1/*` &rarr; `http://radar_backend:8080/api/v1/*`
- **Migrações Flyway:** V1 (`V1__init_territory_schema.sql`) e V2 (`V2__legacy_schema_compatibility.sql`) aplicadas e consolidadas.

---

## 3. Matriz Formal de Homologação e Fechamento Funcional

| Item Avaliado | Escopo do Teste | Status | Parecer Técnico |
| :--- | :--- | :---: | :--- |
| **Ambiente Completo** | Subida dos containers via Docker Compose, healthchecks e conectividade | **APROVADO** | Containers ativos, portas corretas, banco saudável e persistente. |
| **Identidade Visual & UI** | Carregamento, tipografia, badges, ausência de Lovable/gptengineer | **APROVADO** | Interface 100% limpa, sem qualquer dependência ou menção a plataformas externas de geração de código. |
| **AOI Maceió (Mutange)** | Carregamento da AOI oficial (`1e7a1465-c3d7-44e6-8398-5ced7dac17ec`) | **APROVADO** | Geometria renderizada na BBOX de Mutange; área autoritativa de **10.93 km²** (10.9254 km²) idêntica ao PostGIS. |
| **Consistência de Áreas** | Resolução da divergência de área e validação de polígonos complexos | **APROVADO** | PostGIS WGS84 adotado como fonte autoritativa de verdade (`ST_Area(geom::geography)`); `PolygonGeometry` cobre anéis internos (buracos), orientações horária/anti-horária e polígonos irregulares. |
| **Edição e Integridade AOI** | Modo de edição, remoção/modificação de vértice, detecção de alteração não salva | **APROVADO** | Exibe badge "Geometria Modificada (Não Salva)", bloqueia consulta SAR e invalida painel dry-run aberto. |
| **Persistência de Nova AOI** | Criação de AOI temporária de teste com nome único com timestamp | **APROVADO** | Salvo no PostGIS com cálculo WGS84 e área persistida com sucesso; UUID registrado para auditoria. |
| **Durabilidade pós-Reload** | F5 / Recarregamento da página e consulta no dropdown | **APROVADO** | A nova área e suas coordenadas foram restauradas com precisão diretamente do PostGIS após reload da SPA. |
| **Saneamento Seguro E2E** | Limpeza estrita por UUID registrado sem `LIKE` | **APROVADO** | Exclusão estrita apenas do UUID gerado pela execução (`DELETE WHERE id = '<uuid>'`). A AOI oficial `1e7a1465-c3d7-44e6-8398-5ced7dac17ec` é rigorosamente preservada. |
| **Catálogo SAR Real (ASF)** | Descoberta de cenas Sentinel-1 IW SLC para janeiro de 2023 (Maceió) | **APROVADO** | 3 cenas reais identificadas na NASA ASF com órbita relativa 9, direção DESCENDING e polarização VV+VH. |
| **Aviso Científico InSAR** | Disclaimer sobre cenas isoladas não serem pares automáticos | **APROVADO** | Texto oficial destacado em azul informando os limites metodológicos da busca espacial. |
| **Formação de Pares InSAR** | Combinação temporal de cenas e avaliação sob política `TROPICAL_URBAN_MACEIO` | **APROVADO** | 3 pares formados (1 Elegível, 2 Rejeitados por baseline espacial excessiva); zero pendentes. |
| **Avaliação do PAR-01** | Elegibilidade de `20230104_20230116` com baseline perpendicular real da ASF | **APROVADO** | $\Delta t = 12\text{d}$, $B_\perp = -79.0\text{ m} \le 120\text{ m}$; classificado como `ELIGIBLE_FOR_SUBMISSION`. |
| **Contrato Dinâmico de Opções** | `GET /api/v1/interferometry/pairs/{pairId}/processing/options?aoiId={aoiId}` | **APROVADO** | Retorna modalidades disponíveis, bursts reais da ASF com interseção JTS calculada, créditos e limitações científicas. |
| **Eliminação de Bursts Hardcoded** | Remoção de listas fixas e percentuais fictícios (`73.92%`, `71.34%`) | **APROVADO** | Frontend e backend operam sem strings fixas de Maceió; cálculo dinâmico baseado nos footprints reais (`018319` cobre 100.00%, `018318` cobre 69.68%). |
| **Single-Burst Dry-Run** | Simulação `BURST_INSAR` gerando contrato ISCE2 | **APROVADO** | Validação com 1 crédito, payload auditável com `validate_only: true` e `INSAR_ISCE_BURST`. |
| **Multi-Burst Dry-Run** | Simulação `MULTI_BURST` com separação de conjuntos reference e secondary | **APROVADO** | Validação com 1 crédito, conjuntos estritos separados por data ($t_1$: 2023-01-04, $t_2$: 2023-01-16), cobertura combinada de 100.00% e payload auditável com `INSAR_ISCE_MULTI_BURST`. |
| **Limitações de Qualidade** | Apresentação separada de 4 advertências científicas | **APROVADO** | Coerência preliminar não quantificada, descorrelação temporal por vegetação/chuva, distorções de relevo e perda de sinal em corpos d'água explicitadas na UI. |
| **Integridade do Estado React** | Invalidação ao alterar AOI, par, datas, perfil ou geometria | **APROVADO** | `selectedDryRunPair` é anulado imediatamente ao alterar parâmetros ou vértices; painel bloqueado para geometria modificada não salva. |
| **Modularização do Frontend** | Extração de `InSarProcessingPanel.tsx` de `AoiDrawingMap.tsx` | **APROVADO** | Código desacoplado e manutenível; mapa restrito a cartografia e tabela; painel focado na esteira científica InSAR. |
| **Governança Dry-Run** | Ausência de botão de execução real e bloqueio por política | **APROVADO** | Bloqueio `executionBlockedBySecurityPolicy: true` ativo; nenhum job despachado à nuvem. |
| **Aceite Visual Responsivo** | Inspeção visual em Desktop, Tablet (768x1024) e Celular (375x812) | **APROVADO** | Layout fluido, ausência de overflow horizontal, cards e tabelas perfeitamente adaptados. |
| **Submissão Real HyP3/MintPy** | Disparo de jobs de interferometria na nuvem ASF/NASA | **NÃO TESTADO** | Bloqueado deliberadamente por governança financeira e científica (`execution-enabled=false`). |

---

## 4. Evolução Arquitetural e Correções Aplicadas no Ciclo 5.5.2

### 4.1. Eliminação Completa de Bursts e Métricas Hardcoded
- **Diagnóstico:** As implementações do Ciclo 5.5 e 5.5.1 mantinham listas fixas de identificadores de bursts de Maceió (`018319` e `018318`) e percentuais estáticos mockados (`73.92%` e `71.34%`) tanto em `AoiDrawingMap.tsx` quanto no backend.
- **Correção Aplicada:**
  1. Criação do modelo de domínio [SarBurst.java](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/main/java/com/ass/br/apiradar/catalog/domain/model/SarBurst.java) e porta de saída [SarBurstCatalogPort.java](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/main/java/com/ass/br/apiradar/catalog/domain/ports/output/SarBurstCatalogPort.java).
  2. Implementação do adaptador [AsfBurstClientAdapter.java](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/main/java/com/ass/br/apiradar/catalog/infrastructure/adapters/output/external/AsfBurstClientAdapter.java), que consulta a Search API pública da NASA ASF com `processingLevel=BURST`, extrai os footprints GeoJSON e decodifica as propriedades canônicas (`relativeBurstId`, `subswath`, `polarization`, `sceneName`, `acquisitionDate`).
  3. Cálculo dinâmico de interseção espacial e cobertura temporal comum em [InSarProcessingPreparationService.java](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/main/java/com/ass/br/apiradar/interferometry/application/services/InSarProcessingPreparationService.java) utilizando JTS Topology Suite sobre o polígono real da AOI selecionada.
  4. Constatação científica comprovada: para a AOI oficial de Maceió (10.93 km²), o burst `018319` cobre **100.00%** da área de Mutange, e o burst `018318` cobre **69.68%**. Os valores anteriores eram estimativas empíricas desatualizadas.

### 4.2. Contrato de Opções e Preparação InSAR
- **Contrato Estabelecido:**
  `GET /api/v1/interferometry/pairs/{pairId}/processing/options?aoiId={aoiId}`
- **DTOs Implementados:**
  [InSarProcessingOptionsResponseDto.java](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/main/java/com/ass/br/apiradar/interferometry/application/dto/InSarProcessingOptionsResponseDto.java), [CandidateBurstPairDto.java](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/main/java/com/ass/br/apiradar/interferometry/application/dto/CandidateBurstPairDto.java), [MultiBurstSummaryDto.java](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/main/java/com/ass/br/apiradar/interferometry/application/dto/MultiBurstSummaryDto.java) e [SupportedModalityOptionDto.java](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/main/java/com/ass/br/apiradar/interferometry/application/dto/SupportedModalityOptionDto.java).
- **Semântica de Resposta:**
  - Se os metadados de bursts não forem comprovados ou o serviço externo falhar, o backend retorna status `PENDING_BURST_METADATA` com aviso explícito de cobertura nominal pendente, impedindo percentuais fictícios.
  - Disponibiliza separadamente 4 advertências científicas em `qualityLimitations`.

### 4.3. Correção e Rigor Científico da Modalidade Multi-Burst
- **Regras Implementadas no Adaptador HyP3 ([Hyp3InSarProcessorAdapter.java](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/main/java/com/ass/br/apiradar/interferometry/infrastructure/adapters/output/processing/Hyp3InSarProcessorAdapter.java)):**
  1. O job `INSAR_ISCE_MULTI_BURST` exige conjuntos mutuamente exclusivos de bursts de referência e secundário (`reference` e `secondary`).
  2. Todos os bursts do array `reference` devem pertencer estritamente à data de aquisição da cena de referência ($t_1$).
  3. Todos os bursts do array `secondary` devem pertencer estritamente à data de aquisição da cena secundária ($t_2$).
  4. Rejeição com erro `400 Bad Request` caso ocorram datas misturadas no mesmo conjunto, bursts duplicados ou combinação de bursts com a mesma data de aquisição.
  5. Testes unitários cobrindo todos esses cenários adicionados em `Hyp3InSarProcessorAdapterTest.java`.

### 4.4. Modularização do Frontend e Integridade do Estado React
- **Componente Dedicado:** [InSarProcessingPanel.tsx](file:///home/asoares/Documentos/repositories/repo_radar/front_react/github-friend-helper/src/components/InSarProcessingPanel.tsx) criado para isolar a experiência de simulação e auditoria InSAR.
- **Prevenção de Dados Obsoletos em [AoiDrawingMap.tsx](file:///home/asoares/Documentos/repositories/repo_radar/front_react/github-friend-helper/src/components/AoiDrawingMap.tsx):**
  - Ao alterar AOI, par, intervalo de datas (`startDate`, `endDate`), direção orbital ou perfil metodológico, `selectedDryRunPair` é imediatamente redefinido para `null`.
  - Ao editar ou mover qualquer vértice da geometria no mapa, `clearCatalogSearch()` é invocado, fechando imediatamente o painel dry-run aberto e bloqueando nova busca SAR até que a geometria seja salva ou descartada.
  - Caso a geometria esteja modificada e não salva, o botão "Solicitar Validação Dry-Run" permanece desabilitado com aviso explicativo em amarelo.

### 4.5. Segurança Estrita da Suíte E2E no Banco de Dados
- **Diagnóstico Anterior:** O script E2E executava `DELETE FROM radar.areas_of_interest WHERE name LIKE 'AOI Maceio - Teste Homologacao E2E%'`.
- **Nova Estratégia de Isolamento em [run_e2e_homologation_5_5.mjs](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/test/resources/e2e-evidence/run_e2e_homologation_5_5.mjs):**
  1. Criação de AOIs temporárias com nome único acrescido de timestamp (`Date.now()`).
  2. Consulta do UUID exato persistido no PostGIS e armazenamento em um `Set<string>` em memória e em `.e2e_tracked_uuids.json` em disco.
  3. Saneamento estrito por ID: `DELETE FROM radar.areas_of_interest WHERE id = '<uuid>'`.
  4. Trava de segurança no teardown que impede a exclusão do UUID oficial de Maceió (`1e7a1465-c3d7-44e6-8398-5ced7dac17ec`), confirmando ao final a existência de 1 registro intacto no banco.
  5. Registro de tratadores `SIGINT` e `SIGTERM` no Node.js para garantir limpeza mesmo se a execução for cancelada no meio do fluxo.

### 4.6. Rigor Geodésico e Suporte a Polígonos Complexos
- **Fonte Autoritativa:** O PostGIS WGS84 (`ST_Area(geom::geography) / 1000000.0`) é invocado no adapter de persistência ([AreaOfInterestPersistenceAdapter.java](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/main/java/com/ass/br/apiradar/territory/infrastructure/adapters/output/persistence/AreaOfInterestPersistenceAdapter.java)) para garantir que o valor no banco seja sempre a verdade autoritativa.
- **Modelo de Domínio Atualizado ([PolygonGeometry.java](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/main/java/com/ass/br/apiradar/catalog/domain/model/PolygonGeometry.java)):**
  - Implementado suporte a polígonos GeoJSON com múltiplos anéis (anel externo menos a área de anéis internos / buracos).
  - Suporte a orientações horária e anti-horária com normalização matemática por valor absoluto.
  - Testes em [PolygonGeometryTest.java](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/test/java/com/ass/br/apiradar/catalog/domain/model/PolygonGeometryTest.java) cobrindo triângulos, pentágonos irregulares, buracos e rejeição de anéis abertos.

---

## 5. Rastreabilidade das Evidências Visuais e Execução E2E

Todas as capturas geradas durante a execução da suíte automatizada via Chrome CDP foram salvas em:
`src/test/resources/e2e-evidence/`

1. **`01_initial_dashboard.png`**: Tela inicial com cabeçalho Radar++, banner de política operacional de área (limite 250 km²) e mapa interativo.
2. **`02_official_maceio_aoi_selected.png`**: AOI oficial `Maceio Homologacao 5.1.1` selecionada e renderizada na BBOX de Mutange, exibindo métricas consistentes de **10.93 km²** com rótulo "Oficial PostGIS (WGS84)".
3. **`03_geometry_modified_unsaved_warning.png`**: Modo de edição ativado e vértice removido, disparando badge de "Geometria Modificada (Não Salva)" e aviso de bloqueio da consulta ao catálogo SAR.
4. **`04_new_aoi_persisted_postgis.png`**: Persistência de AOI de teste com nome único com timestamp, gravando área autoritativa no PostGIS e registrando UUID para rastreabilidade.
5. **`05_reloaded_and_verified_persistence.png`**: Recarregamento completo do SPA no navegador e restauração da área persistida a partir do banco de dados relacional espacial.
6. **`06_sar_scenes_table.png`**: Tabela de cenas SAR Sentinel-1 IW SLC descobertas na NASA ASF para janeiro de 2023.
7. **`07_insar_scientific_disclaimer.png`**: Aviso científico obrigatório demarcando que cenas isoladas não são interferogramas.
8. **`08_insar_pairs_summary_cards.png`**: Seção de avaliação InSAR com 4 cards de resumo: Total (3), Elegíveis (1), Pendentes (0), Rejeitados (2).
9. **`09_par_01_eligible_justification.png`**: Parecer técnico detalhado do PAR-01 (`20230104_20230116`, $\Delta t = 12\text{d}$, $B_\perp = -79.0\text{ m}$) exibindo conformidade total e status "Elegível para Submissão".
10. **`10_par_02_rejected_justification.png`**: Parecer técnico detalhado do PAR-02 (`20230116_20230128`, $\Delta t = 12\text{d}$, $B_\perp = 235.0\text{ m}$) exibindo rejeição formal por violação do limiar de descorrelação espacial ($> 120\text{ m}$).
11. **`12_insar_dry_run_panel_ready.png`**: Painel modular de preparação InSAR (`InSarProcessingPanel`) aberto, exibindo opções dinâmicas da ASF: bursts `018319` (100.00%) e `018318` (69.68%), sem valores hardcoded, e as 4 limitações científicas.
12. **`13_insar_dry_run_validated_audit.png`**: Simulação Dry-Run Single-Burst (`INSAR_ISCE_BURST`) auditada com sucesso (1 crédito) e contrato JSON aberto exibindo `validate_only: true`.
13. **`13b_insar_multi_burst_dry_run_audit.png`**: Simulação Dry-Run Multi-Burst (`INSAR_ISCE_MULTI_BURST`) auditada com sucesso (1 crédito), demonstrando conjuntos separados e correspondentes (`reference` e `secondary`) e cobertura combinada de 100.00%.
14. **`13c_insar_dry_run_blocked_unsaved_geometry.png`**: Demonstração de integridade de estado React: alteração na geometria cartográfica fecha e bloqueia imediatamente o dry-run, prevenindo pedidos baseados em geometria desatualizada.
15. **`14_tablet_viewport_768x1024.png`** e **`14_tablet_insar_table.png`**: Inspeção de layout no viewport Tablet (768x1024), comprovando ausência de sobreposição e navegação fluida.
16. **`15_mobile_viewport_375x812.png`** e **`15_mobile_insar_card.png`**: Inspeção de layout no viewport Celular (375x812), comprovando empilhamento harmônico de cards e botões acessíveis.
17. **`11_full_page_e2e_homologation.png`**: Captura integral da página demonstrando a integridade vertical e estética de ponta a ponta do Radar++.

---

## 6. Parecer Conclusivo de Homologação

O produto **Radar++ (v0.1 / Ciclo 5.5.2)** encontra-se **HOMOLOGADO COM SUCESSO** em ambiente local com integridade científica, espacial e contratual plena.

A esteira comprovou:
- Eliminação total de listas fixas e métricas fictícias de bursts.
- Contrato dinâmico e flexível entre backend e frontend via `/processing/options`.
- Modalidade Multi-Burst estrita conforme os requisitos oficiais HyP3 ISCE2.
- Prevenção total de submissão de dados obsoletos no frontend React.
- Suíte E2E automatizada não-destrutiva, segura e com tolerância a interrupções.
- Preservação estrita das diretrizes de governança financeira (`radar.processing.hyp3.execution-enabled=false`).

---

## 7. Encerramento Formal do Ciclo 5.5.2 e Verificações Finais

Em conformidade com a aprovação de encerramento de escopo do Ciclo 5.5.2, foram realizadas 5 verificações localizadas:

1. **Invalidação de Respostas Obsoletas de Dry-Run:**
   - Adicionada rotina unificada `clearDryRun` no componente [InSarProcessingPanel.tsx](file:///home/asoares/Documentos/repositories/repo_radar/front_react/github-friend-helper/src/components/InSarProcessingPanel.tsx).
   - A troca de modalidade (`handleModalityChange`), de burst (`handleBurstChange`), de AOI (`useEffect [pair.pairId, aoi.id]`) ou modificação de vértices (`useEffect [isGeometryModified]`) limpa instantaneamente qualquer resultado ou erro prévio de dry-run.

2. **Remoção de Fallback Fixo de Área `10.93` e Cobertura Full-Scene:**
   - Removido o fallback `|| "10.93"` no [InSarProcessingPanel.tsx](file:///home/asoares/Documentos/repositories/repo_radar/front_react/github-friend-helper/src/components/InSarProcessingPanel.tsx), apresentando a área real calculada ou omitindo quando nula.
   - No [InSarProcessingPreparationService.java](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/main/java/com/ass/br/apiradar/interferometry/application/services/InSarProcessingPreparationService.java), eliminada a asserção indiscriminada de 100% de cobertura Full-Scene, qualificando o enquadramento como regional e condicionado aos limites orbitais do frame.

3. **Bloqueio de Limpeza E2E por UUID em Produção:**
   - Incorporada a função `verifyTestEnvironment()` no script [run_e2e_homologation_5_5.mjs](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/test/resources/e2e-evidence/run_e2e_homologation_5_5.mjs).
   - A limpeza somente é autorizada caso o ambiente esteja na lista explícita (`test`, `development`, `homologation`, `local`, `dev`, `ci`), abortando imediatamente se executada sob perfis de produção (`production`, `prod`).

4. **Limites e Expiração nos Caches ASF:**
   - Os adaptadores [AsfBaselineClientAdapter.java](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/main/java/com/ass/br/apiradar/interferometry/infrastructure/adapters/output/external/AsfBaselineClientAdapter.java) e [AsfBurstClientAdapter.java](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/main/java/com/ass/br/apiradar/catalog/infrastructure/adapters/output/external/AsfBurstClientAdapter.java) foram dotados de cache delimitado (máximo de 500 entradas com política LRU via `LinkedHashMap` sincronizado) e expiração TTL de 2 horas (`Duration.ofHours(2)`).

5. **Validação de Testes e Regressão:**
   - Executados os testes direcionados e a suíte completa de regressão do backend (**98 testes executados com 100% de sucesso**) e build do frontend Vite (**zero erros de TypeScript**).
