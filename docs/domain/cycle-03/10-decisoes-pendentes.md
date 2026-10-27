# Radar++ — Ciclo 3: Modelagem Estratégica e Tática com DDD
## Documento 10 — Decisões Pendentes, Divergências Científicas e Procedimento Exploratório de Metadados

**Status:** Aguardando Homologação e Decisão da Liderança do Radar++  
**Data:** 24/10/2026  
**Contextos Abrangidos:** Todos os Contextos Delimitados (Gestão Territorial, Catálogo SAR, Processamento InSAR, Análise Urbana)  

---

### 1. Resumo Executivo das Decisões Consolidadas

No Ciclo 3, o modelo de domínio do Radar++ foi formalizado sob as diretrizes de Domain-Driven Design (DDD), consolidando as seguintes decisões arquiteturais e semânticas:

1. **Adoção do Monólito Modular:** Os quatro contextos delimitados (Gestão Territorial, Catálogo SAR, Processamento & Produtos InSAR, Análise de Subsidência Urbana) serão organizados como módulos de fronteira bem definida no mesmo processo de deploy no Ciclo 4, evitando a complexidade e os custos de infraestrutura de microsserviços prematuros.
2. **Coabitação Modular de Produtos e Qualidade:** O subdomínio de Avaliação de Qualidade permanece conceitualmente desacoplado de Produtos InSAR, mas compartilha o mesmo pacote/módulo de processamento, garantindo que nenhum produto seja promovido a `APPROVED_FOR_VISUALIZATION` sem passar pelo portão de qualidade (`evaluateProductQuality`).
3. **Escopo Organizacional Simples (`OrganizationId`):** Multi-tenancy é modelado apenas como segregação lógica de propriedade de dados no nível de entidade/agregado, descartando esquemas dinâmicos de isolamento por banco de dados.
4. **Desacoplamento Técnico vs. Científico:** Um job computacional atingir `PROCESSING_COMPLETED` significa apenas integridade de execução operacional. A análise científica correspondente transita para `SCIENTIFIC_EVALUATION_PENDING`, exigindo homologação orientada a métricas (coerência $\bar{\gamma}$, fechamento de fase).
5. **COG como Representação Física:** O Produto InSAR é uma observação geodésica matricial; o Cloud Optimized GeoTIFF (COG) é a sua representação de entrega em armazenamento em nuvem para consumo via HTTP Range Requests.
6. **Diferenciação Estrita LOS vs. Vertical:** A grandeza primária do radar é a deformação na linha de visada ($d_{\text{LOS}}$). A projeção vertical ($d_{\text{vert}} = d_{\text{LOS}} / \cos\theta$) só é calculada com o registro explícito do ângulo de incidência $\theta$ e sob a hipótese de deformação predominantemente vertical, evitando equiparação errônea a nivelamento geométrico.
7. **Responsabilidade Técnica (ART) e Histórico Auditável:** A ART é uma fronteira de competência legal do engenheiro/geólogo e não uma isenção civil automática; os metadados e hashes SHA-256 constituem rastreabilidade técnica auditável de proveniência, sem alegação infundada de fé pública pericial imediata.

---

### 2. Divergências Científicas e Técnicas Pendentes (Piloto Maceió)

Durante a consolidação do Discovery e a modelagem do Ciclo 3, foram identificadas inconsistências quantitativas nos documentos anteriores que precisam de saneamento antes da execução no ambiente real:

| Item Divergente | Registro no Discovery / Piloto | Impacto Técnico no Domínio | Hipótese Proposta para Homologação |
| :--- | :--- | :--- | :--- |
| **Órbitas Sentinel-1 para Maceió** | D1-05 cita Track 142; D-ENC-03 cita Track 171. | Geometria de visada, ângulo de incidência e direção orbital (Ascendente vs. Descendente) condicionam a cobertura e a sensibilidade do radar. | **Manter Tracks 142 e 171 como informações estritamente pendentes de confirmação por metadados reais de catálogo.** Não atribuir direção orbital nem preferência metodológica prévia sem consulta empírica à API do ASF. |
| **BBOX de Busca vs. Geometria Efetiva da AOI** | BBOX de Maceió delimitada entre 60 e 78 km²; a mancha urbana afetada compreende 25 a 35 km². | BBOX de busca ampla pode retornar bursts adjacentes desnecessários, elevando a estimativa de custos. | **Separar a BBOX de busca (retângulo envolvente para queries de catálogo) da geometria efetiva da AOI (polígono restrito da mancha urbana).** O cômputo e as estatísticas aplicam-se exclusivamente ao polígono da AOI. |
| **Orçamentação de Créditos HyP3** | Estimativas simplificadas anteriores não decompunham variáveis de tarifação. | O custo de processamento na nuvem depende do tipo de algoritmo, resolução e granularidade espacial. | **Decompor a estimativa conforme a tabela tarifária oficial do ASF HyP3**, separando tipo de job (`INSAR_ISCE_BURST`), quantidade de pares, quantidade de bursts interceptados e resolução de multilooking. |
| **Equiparação a Marcos Topográficos** | Relatórios sugeriam comparação direta de deslocamento InSAR com dados de nivelamento óptico da Braskem/CPRM. | O radar mede a projeção em LOS ($d_{\text{LOS}}$). Marcos topográficos medem recalque vertical ($\Delta z$). | **Impedir no domínio a validação cruzada direta** sem que o modelo aplique a projeção angular $d_{\text{vert}} = d_{\text{LOS}} / \cos\theta$, mantendo $d_{\text{LOS}}$ como grandeza primária soberana e documentando as incertezas. |

---

### 3. Decisões Estratégicas e de Domínio que Exigem Aprovação da Liderança

Antes de avançar para o **Ciclo 4 (Arquitetura e Desenho Técnico)**, as seguintes decisões necessitam de parecer formal da liderança de produto e de engenharia:

#### Decisão 1: Política de Descarte vs. Armazenamento de Interferogramas Intermediários
- **Opção A (Recomendada):** Armazenar permanentemente apenas os produtos finais aprovados (velocidade média anual, série temporal invertida e mapa de coerência temporal consolidado) em COG no S3/MinIO. Os interferogramas individuais brutos (wrap/unwrap) são expurgados após a convergência do SBAS/MintPy para economizar armazenamento, preservando apenas a cadeia de metadados para eventual reprocessamento.
- **Opção B:** Reter 100% dos interferogramas brutos gerados, aumentando o custo de armazenamento de nuvem em até 10x para grandes séries temporais.

#### Decisão 2: Modelo de Execução do Pipeline InSAR Core
- **Opção A (Híbrido Inicial):** Orquestrar chamadas de processamento interferométrico inicial via API HyP3 (ASF) para geração de interferogramas de bursts (modo econômico de nuvem), e executar a inversão de séries temporais (MintPy) em container especializado gerenciado pelo Radar++.
- **Opção B (Self-Hosted Completo):** Executar toda a cadeia InSAR (ISCE2/InSAR Scientific Computing Environment + MintPy) em workers dedicados na infraestrutura do Radar++, dispensando créditos HyP3, porém exigindo maior capacidade de GPU/RAM e manutenção de nós de processamento.

#### Decisão 3: Mecanismo de Autorização e Perfil de Acesso
- **Opção A (Role-Based Simples):** Definir 3 papéis conceituais no domínio (`TechnicalAnalyst`, `DomainSpecialist`, `OrganizationAdmin`) associados ao `OrganizationId`, sem implementação de RBAC dinâmico configurável por interface na v1.
- **Opção B:** Implementar controle de acesso granular por atributo (ABAC) no nível de feature geométrica da AOI na primeira versão.

---

### 4. Procedimento Exploratório de Metadados SAR (Não Destrutivo)

Em estrito cumprimento ao **Item 10 da especificação**, apresenta-se a seguir o procedimento metodológico e as especificações de consulta de metadados de catálogo.

> [!IMPORTANT]
> **Aviso de Governança e Execução Não Destrutiva:**  
> Este procedimento é **estritamente informativo e documental**. Nenhuma chamada de rede, requisição HTTP, consumo de créditos ou download de arquivos volumosos foi realizada ou deve ser executada sem autorização expressa da liderança técnica.

#### 4.1. Entradas Paramétricas da Consulta
- **Geometria de Interesse (AOI Maceió - BBOX Restrita):**
  - Formato WKT: `POLYGON((-35.795 -9.680, -35.710 -9.680, -35.710 -9.600, -35.795 -9.600, -35.795 -9.680))`
  - Área Aproximada: $\approx 72\,\text{km}^2$ (envolve bairros Pinheiro, Bebedouro, Mutange, Bom Parto e lagoa Mundaú).
- **Janela Temporal de Prova de Conceito:**
  - Início: `2023-01-01` | Fim: `2023-12-31` (12 meses de monitoramento histórico).
- **Plataforma e Sensor:**
  - Satélite: `Sentinel-1A` / `Sentinel-1B`
  - Modo de Feixe: `IW` (Interferometric Wide Swath)
  - Tipo de Produto de Entrada: `SLC` (Single Look Complex)
  - Polarização: `VV` ou `VV+VH` (seleção estrita de co-polarização `VV`).
- **Faixas Orbitais Candidatas:**
  - Órbita Descendente: **Path/Track 142**
  - Órbita Ascendente: **Path/Track 171**

#### 4.2. Especificação da Consulta à API de Catálogo Público (ASF Search API / STAC)
A verificação não requer credenciais pagas nem processamento, utilizando a API pública de metadados do Alaska Satellite Facility:

```http
GET https://api.daac.asf.alaska.edu/services/search/param?
  bbox=-35.795,-9.680,-35.710,-9.600
  &dataset=SENTINEL-1
  &processingLevel=SLC
  &beamMode=IW
  &polarization=VV,VV+VH
  &relativeOrbit=142,171
  &start=2023-01-01T00:00:00Z
  &end=2023-12-31T23:59:59Z
  &output=geojson
```

#### 4.3. Saídas Esperadas e Verificação Estrutural
A resposta GeoJSON contém os metadados de cada cena sem transferir dados SAR binários:
1. `properties.sceneName`: Nome canônico da cena SLC (ex: `S1A_IW_SLC__1SDV_2023...`);
2. `properties.pathNumber`: Identificador da faixa orbital (confirma se é 142 ou 171);
3. `properties.flightDirection`: Direção de voo (`DESCENDING` ou `ASCENDING`);
4. `properties.bytes`: Tamanho em bytes da cena completa (normalmente ~4.2 GB, apenas informativo);
5. `properties.burst`: Em consultas específicas de bursts Sentinel-1 (ASF Burst API), retorna os identificadores de bursts:
   - Exemplo de Burst ID: `t142_304561_iw2` (Track 142, Swath IW2).

#### 4.4. Procedimento Verificável para Estimativa de Créditos HyP3
A orçamentação de créditos junto à API do ASF HyP3 é calculada a partir da decomposição técnica dos seguintes fatores operacionais:

1. **Classificação do Tipo de Job:** Para Sentinel-1 IW, adota-se o job oficial `INSAR_ISCE_BURST` (processamento interferométrico nativo por burst individual).
2. **Definição da Resolução de Multilooking:** Seleciona-se a resolução espacial padrão de 20 metros (fator de look azimutal/alcance $5 \times 1$).
3. **Determinação do Número de Bursts Interceptados ($N_{\text{bursts}}$):**
   - Sobrepor a geometria vetorial da AOI efetiva com as pegadas dos bursts da órbita selecionada (retornadas na query da Burst API);
   - Caso a mancha urbana esteja contida integralmente em um burst: $N_{\text{bursts}} = 1$;
   - Caso cruze a borda azimutal/alcance entre dois bursts adjacentes da mesma faixa: $N_{\text{bursts}} = 2$.
4. **Determinação da Rede de Interferogramas ($N_{\text{interf}}$):**
   - Para uma série de 12 meses (Sentinel-1A, revisita de 12 dias): $\approx 30$ aquisições SLC;
   - Formação da rede SBAS conectando pares temporais com $B_\perp \le 150\,\text{m}$ e $\Delta t \le 36\,\text{dias}$: $\approx 70$ a 75 interferogramas.
5. **Custo Unitário da Tabela Oficial ASF:** Cada job do tipo `INSAR_ISCE_BURST` tarifado a 1 crédito HyP3 por execução de par de burst.
6. **Estimativa Total de Créditos:**
   $$\text{Créditos Totais Estimados} = N_{\text{interf}} \times N_{\text{bursts}} \times \text{TarifaUnitária}$$
   - **Hipótese A (1 burst cobre a área efetiva):** $75 \times 1 \times 1 = \mathbf{75\text{ créditos HyP3}}$;
   - **Hipótese B (2 bursts necessários para cobrir a área):** $75 \times 2 \times 1 = \mathbf{150\text{ créditos HyP3}}$.

---

### 5. Recomendações para a Entrada no Ciclo 4

1. **Aprovação Formal dos Documentos de Domínio do Ciclo 3** pela liderança técnica.
2. **Definição sobre a Órbita para o Piloto Maceió:** Aguardar a realização da consulta não destrutiva de metadados reais de catálogo para verificar empiricamente a cobertura, integridade temporal e pegada dos bursts dos **Tracks 142 e 171**, selecionando a órbita com base estrita em dados verificáveis.
3. **Início do Ciclo 4 (Arquitetura e Desenho Técnico):** Estruturar a arquitetura C4, diagramas de componentes, organização de módulos Java/Spring Boot (Monólito Modular), portas e adaptadores, modelagem PostgreSQL/PostGIS e integração assíncrona com os pipelines Python/HyP3/MintPy.
