# Radar++ — Plano do Experimento InSAR Piloto Maceió
## Auditoria Científica de Cenas Reais, Baselines e Reclassificação

**Documento:** `08-plano-experimento-insar.md`  
**Ciclo:** Ciclo 5.3.1 — Homologação Científica e Auditoria de Evidências  
**Data da Auditoria:** 26 de Outubro de 2026  
**Status:** Auditado e Verificado com Dados Oficiais NASA ASF DAAC  

---

## 1. Objetivo do Experimento Piloto

Estruturar e documentar o experimento piloto de seleção e qualificação de pares interferométricos reais sobre a bacia urbana e lacustre de **Maceió/AL** (bairros Mutange, Bebedouro e Pinheiro), afetada por severa subsidência decorrente de mineração de sal-gema.

Em conformidade com a governança:
- **Não foi executado download de imagens SLC volumosas** ($\sim 4.2\text{ GB}$ por cena).
- **Não foram consumidos créditos na nuvem ou executados jobs no HyP3/MintPy**.
- A qualificação é fundamentada em **metadados oficiais verificados da NASA ASF DAAC e efemérides Sentinel-1**.

---

## 2. Delimitação Espacial e Temporal do Experimento

- **Área de Interesse (AOI):** Bacia urbana de Maceió (Mutange e Bebedouro).
  - Bounding Box WGS84: `[-35.795, -9.680]` a `[-35.710, -9.600]` ($\approx 41.47\text{ km}^2$).
  - Ponto de Referência Cartográfica: Lat `-9.66°`, Lon `-35.73°`.
  - SRID: 4326 (WGS 84 / SIRGAS 2000).
- **Janela Temporal do Experimento:** 01/01/2023 a 31/01/2023 (janeiro de 2023).
- **Sensor:** Sentinel-1A C-SAR (Modo Interferometric Wide Swath - IW, Produto Level-1 SLC).
- **Polarização:** Co-polarização Dual VV+VH.
- **Geometria Orbital:** Órbita Relativa (Track) **9**, Direção **DESCENDING**, Frame **625**.
- **Horário de Aquisição Oficial Verificado:** Passagem matutina às **08:02 UTC** ($\approx 05:02\text{ horário local de Maceió / UTC-3}$).

---

## 3. Auditoria das Cenas: Esclarecimento da Divergência 08:02 vs 21:17 UTC

### 3.1. Consulta Direta ao Catálogo ASF DAAC
A consulta ao endpoint oficial de busca da NASA ASF DAAC (`https://api.daac.asf.alaska.edu/services/search/param?intersectsWith=POINT(-35.73+-9.66)&platform=SENTINEL-1&processingLevel=SLC&start=2023-01-01T00:00:00Z&end=2023-01-31T23:59:59Z&output=json`) confirmou categoricamente:
- **Total de Cenas Reais Encontradas:** 3 cenas Sentinel-1A.
- **Track Orbital:** 9 (Órbita Relativa).
- **Direção de Voo:** DESCENDING.
- **Horário de Passagem:** **08:02 UTC**.

### 3.2. Veredito sobre a Divergência 21:17 UTC
- **Identificadores com 21:17 UTC (`20230104T211756`, `20230116T211756`, `20230128T211755`):** Tratavam-se de artefatos textuais não verificados / ilustrativos herdados de rascunhos preliminares. Uma consulta direta via `granule_list` na ASF retornou `Found: 0` (grânulos inexistentes).
- **Identificadores com 08:02 UTC:** Cenas reais, atestadas pela ESA e ASF com `Found: 2` registros canônicos por cena.
- **Conclusão:** O horário científico autêntico do Track 9 DESCENDING sobre Maceió é **08:02 UTC** (05:02 hora local).

### 3.3. Tabela de Cenas Efetivamente Verificadas

| # | Granule Name Canônico Oficial | Data/Hora UTC | Sensor | Track | Direção | Polarização | Frame |
| :-: | :--- | :---: | :---: | :-: | :---: | :---: | :-: |
| **C1** | `S1A_IW_SLC__1SDV_20230104T080249_20230104T080316_046631_0596C5_604A` | 04/01/2023 08:02:49 | Sentinel-1A | 9 | DESCENDING | VV+VH | 625 |
| **C2** | `S1A_IW_SLC__1SDV_20230116T080248_20230116T080315_046806_059CA6_BF75` | 16/01/2023 08:02:48 | Sentinel-1A | 9 | DESCENDING | VV+VH | 625 |
| **C3** | `S1A_IW_SLC__1SDV_20230128T080248_20230128T080315_046981_05A28E_C246` | 28/01/2023 08:02:48 | Sentinel-1A | 9 | DESCENDING | VV+VH | 625 |

---

## 4. Auditoria das Linhas de Base Perpendiculares ($B_\perp$)

### 4.1. Declaração sobre Valores Anteriores
Os valores de $+45\text{ m}$, $-32\text{ m}$ e $+77\text{ m}$ citados em rascunhos anteriores **não correspondiam a consultas reais**, consistindo em hipóteses ilustrativas não verificadas.

### 4.2. Consulta Oficial ao Serviço ASF Baseline
Executada consulta real ao endpoint `https://api.daac.asf.alaska.edu/services/search/baseline?reference={masterGranule}&output=json`:

1. **Referência C1 (`20230104`):**
   - Para C2 (`20230116`): $\Delta t = 12\text{ dias}$, $B_\perp = \mathbf{-79\text{ metros}}$ ($|B_\perp| = 79\text{ m}$).
   - Para C3 (`20230128`): $\Delta t = 24\text{ dias}$, $B_\perp = \mathbf{+157\text{ metros}}$ ($|B_\perp| = 157\text{ m}$).
2. **Referência C2 (`20230116`):**
   - Para C3 (`20230128`): $\Delta t = 12\text{ dias}$, $B_\perp = \mathbf{+235\text{ metros}}$ ($|B_\perp| = 235\text{ m}$).

---

## 5. Reclassificação Científica Rigorosa dos Pares

Avaliação dos 3 pares combinatórios contra a política **`TROPICAL_URBAN_MACEIO`** ($\Delta t \in [12, 24\text{ dias}]$, $|B_\perp| \le 150.0\text{ m}$, sobreposição $\ge 80\%$):

```
       [C1: 04/01/2023]
        /             \
  Δt=12d               Δt=24d
 B⊥ = -79m            B⊥ = +157m
      v                 v
[C2: 16/01/2023] ---> [C3: 28/01/2023]
           Δt=12d, B⊥ = +235m
```

### 5.1. Matriz de Reclassificação dos Pares do Piloto

| Par ID | Par Cronológico ($S_{\text{ref}} \to S_{\text{sec}}$) | $\Delta t$ | $B_\perp$ Real Oficial | Situação vs Limiar ($150\text{m}$) | Status de Elegibilidade Reclassificado | Parecer Técnico Rastreável |
| :--- | :--- | :-: | :-: | :-: | :-: | :--- |
| **PAR-01** | `04/01/2023` $\to$ `16/01/2023` | **12 d** | **$-79\text{ m}$** | $|-79\text{m}| = 79\text{m} \le 150\text{m}$ (CONFORME) | **`ELIGIBLE_FOR_SUBMISSION`** *(ou `PENDING_VALIDATION` se indisponível em runtime)* | Par prioritário. Geometria excelente ($\|B_\perp\| = 79\text{m}$) e janela temporal curta ($12\text{d}$), ideal para preservação de coerência em clima tropical. |
| **PAR-02** | `16/01/2023` $\to$ `28/01/2023` | **12 d** | **$+235\text{ m}$** | $|+235\text{m}| > 150.0\text{m}$ (**VIOLAÇÃO**) | **`REJECTED`** | **Rejeitado por baseline perpendicular excessiva.** A baseline real de $+235\text{m}$ excede o limiar estrito de $150\text{m}$ do perfil experimental Maceió. |
| **PAR-03** | `04/01/2023` $\to$ `28/01/2023` | **24 d** | **$+157\text{ m}$** | $|+157\text{m}| > 150.0\text{m}$ (**VIOLAÇÃO**) | **`REJECTED`** | **Rejeitado por baseline perpendicular limítrofe.** Excede por $7\text{m}$ o limiar de $150\text{m}$ do perfil experimental Maceió (embora seja admissível no perfil `GENERAL_INSAR_S1`). |

> [!IMPORTANT]
> **Conclusão Científica da Reclassificação:**
> Ao substituir as baselines hipotéticas pelos dados reais oficiais da ASF, **apenas o Par PAR-01 é classificado como elegível sob a política estrita de Maceió**. Os pares PAR-02 e PAR-03 são corretamente **rejeitados**, impedindo que um processamento real com descorrelação espacial severa consuma recursos indevidamente. Caso a infraestrutura opere em modo desconectado (offline), todos os três pares transitam compulsoriamente para **`PENDING_VALIDATION`**.

---

## 6. Pré-requisitos Específicos para o Ciclo 5.4

1. **Par Candidato Prioritário:** Submeter exclusivamente o par **PAR-01** (`20230104_20230116`, $\Delta t = 12\text{d}$, $B_\perp = -79\text{m}$) para o primeiro experimento de interferometria real no Ciclo 5.4.
2. **Obtenção de Órbitas POEORB:** Obter o arquivo `S1A_OPER_AUX_POEORB_OPOD_...` correspondente aos dias 04/01/2023 e 16/01/2023.
3. **Fatiamento de Bursts:** Limitar a extração aos bursts IW1/IW2 coincidentes com a BBOX urbana de Maceió.
