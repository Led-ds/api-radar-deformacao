# Radar++ — Ciclo 6.0: Critérios de Aceite Científico e Protocolo Operacional do Primeiro Experimento InSAR Real

**Documento:** `01-criterios-aceite-primeiro-experimento-insar.md`  
**Ciclo:** Ciclo 6.0 — Preparação Operacional do Primeiro Experimento InSAR Real  
**Data:** 29 de Outubro de 2026  
**Status:** Oficial — Homologado para Execução Supervisionada  
**Alvo Geoespacial:** Bacia de Maceió / Região de Mutange e Bebedouro (Alagoas, Brasil)  
**Par Prioritário:** `PAR-01__MACEIO` (`2023-01-04` vs `2023-01-16`)  

---

## 1. Objetivo e Escopo

Estabelecer os critérios científicos de aceite, limites de tolerância física, protocolo seguro de autenticação e esteira de verificação pós-processamento para a realização de **uma única execução real autorizada** do par interferométrico prioritário **PAR-01** de Maceió na nuvem da NASA ASF HyP3, com custo estritamente limitado a **1 crédito**.

---

## 2. Caracterização Orbital e Geométrica do PAR-01

Conforme atestado nas auditorias científicas e consultas em tempo real à NASA ASF DAAC:

```
[Cena de Referência t1: 04/01/2023 08:02:49 UTC]
  Granule: S1A_IW_SLC__1SDV_20230104T080249_20230104T080316_046631_0596C5_604A
  Órbita Relativa: 9 | Direção: DESCENDING | Polarização: VV+VH | Sensor: Sentinel-1A
                     |
                     |  Δt = 12 dias (Janela temporal mínima Sentinel-1A)
                     |  B⊥ = -79.0 metros (|B⊥| = 79.0 m << 120.0 m)
                     v
[Cena Secundária t2: 16/01/2023 08:02:48 UTC]
  Granule: S1A_IW_SLC__1SDV_20230116T080248_20230116T080315_046806_059CA6_BF75
  Órbita Relativa: 9 | Direção: DESCENDING | Polarização: VV+VH | Sensor: Sentinel-1A
```

* **Burst Canônico Recomendado:** `018319` (Subswath IW1, Polarização VV)
  * Burst de Referência: `S1_018319_IW1_20230104T080254_VV_604A-BURST`
  * Burst Secundário: `S1_018319_IW1_20230116T080253_VV_BF75-BURST`
  * Cobertura Comum sobre a AOI Oficial de Maceió (`10.93 km²`): **100.00%**.

---

## 3. Matriz de Critérios Científicos de Aceite Pós-Processamento

Para que o primeiro experimento InSAR real seja considerado **Cientificamente Aprovado**, os produtos GeoTIFF gerados pelo cluster HyP3 ISCE2 devem satisfazer compulsoriamente os seguintes limiares avaliados pelo componente `GeoToolsInSarQualityInspectorAdapter`:

| Critério Científico | Métrica Avaliada | Faixa Nominal Aceitável | Faixa Crítica (Rejeição) | Justificativa Geodésica |
|---|---|---|---|---|
| **1. Coerência Média Urbana ($\bar{\gamma}$)** | $\frac{1}{N} \sum_{i=1}^N \gamma_i$ sobre solos e edificações | $\bar{\gamma} \ge \mathbf{0.35}$ | $\bar{\gamma} < 0.20$ | Em banda C ($\lambda = 5.6\text{ cm}$), coerências abaixo de 0.20 inviabilizam o desdobramento confiável da fase. |
| **2. Coerência Alta em Alvos Urbanos** | $\% \text{ pixels com } \gamma \ge 0.35$ | $\ge \mathbf{30\%}$ | $< 15\%$ | Garante presença de refletores estáveis (casas, ruas, estruturas) na área urbana de Mutange e Pinheiro. |
| **3. Integridade da Fase Desdobrada** | Valores de $\phi_{\text{unw}}$ em radianos | Numéricos finitos (não-NaN) | $> 50\%$ pixels NaN/Inf | Confirma que o algoritmo de desdobramento (SNAPHU) convergiu sem corrupção de matriz. |
| **4. Enquadramento e Referência Espacial** | CRS e Bounding Box vs AOI | EPSG:4326 / UTM intersecionando BBOX Maceió | BBOX desconectada da AOI | Assegura que o produto cobre efetivamente a mina 18 e o entorno da Lagoa Mundaú. |
| **5. Máscara de Água (Water Mask)** | Aplicação de máscara em corpos hídricos | Máscara ativa (`apply_water_mask: true`) | Ausência de máscara | Água possui coerência zero permanente; a máscara evita propagação de erros de fase para o continente. |

---

## 4. Tabela de Custos e Governança Financeira

| Modalidade Oficial | Job Type HyP3 | Pipeline Científico | Custo Oficial ASF | Enquadramento Ciclo 6.0 |
|---|---|---|:---:|---|
| **Single-Burst InSAR** | `INSAR_ISCE_BURST` | ISCE2 (v1.0) | **1 crédito** | **MODALIDADE EXCLUSIVA AUTORIZADA** |
| **Multi-Burst InSAR** | `INSAR_ISCE_MULTI_BURST` | ISCE2 (v1.0) | 1 crédito | Bloqueada para o 1º teste (reserva) |
| **Full-Scene InSAR** | `INSAR_GAMMA` | GAMMA (v2.0) | 15 créditos | **BLOQUEADA COMPULSORIAMENTE** |

> [!WARNING]
> **Teto Financeiro:** Apenas a modalidade `INSAR_ISCE_BURST` sobre o burst canônico `018319` está autorizada a ser executada no primeiro disparo, garantindo que o consumo máximo seja estritamente de **1 crédito**.

---

## 5. Pré-Requisitos e Protocolo Seguro de Autenticação NASA Earthdata

1. **Conta Ativa no NASA Earthdata Login (EDL):**
   * Cadastro em: `https://urs.earthdata.nasa.gov/`.
2. **Autorização da Aplicação HyP3:**
   * No painel Earthdata: `Applications` -> `Authorized Apps` -> autorizar `ASF HyP3`.
3. **Forma Segura de Injeção de Credenciais:**
   * As credenciais **NÃO** devem ser escritas em arquivos de propriedades commitados, código Java ou enviadas em prompts de IA.
   * A injeção deve ser feita exclusivamente via variáveis de ambiente no container ou shell local:
     ```bash
     export EARTHDATA_USERNAME="seu_usuario_earthdata"
     export EARTHDATA_PASSWORD="sua_senha_earthdata"
     # ou alternativamente:
     export EARTHDATA_TOKEN="seu_token_bearer_earthdata"
     ```
   * O backend Radar++ captura automaticamente através de `@Value("${radar.processing.hyp3.username:}")` e `Hyp3JobTrackingAdapter` mascara todas as saídas de log.

---

## 6. Procedimento Operacional Passo a Passo para o Disparo Supervisionado

```
[Etapa 1: Pré-Voo]
  GET /api/v1/interferometry/pairs/PAR-01__MACEIO/readiness?aoiId=1e7a1465-c3d7-44e6-8398-5ced7dac17ec
  Confirmar readinessVerdict: "READY_FOR_SUPERVISED_AUTHORIZATION"
           |
           v
[Etapa 2: Autorização Humana Formal]
  Operador humano insere credenciais Earthdata no ambiente e autoriza o disparo de 1 crédito
           |
           v
[Etapa 3: Submissão Única Controlada]
  Disparo via POST /api/v1/interferometry/pairs/PAR-01__MACEIO/processing/submit
  Captura do job_id retornado pelo cluster HyP3
           |
           v
[Etapa 4: Polling e Rastreamento Assíncrono]
  GET /api/v1/interferometry/jobs/{job_id}
  Estados monitorados: PENDING -> RUNNING -> SUCCEEDED (ou FAILED)
           |
           v
[Etapa 5: Download Seguro com Validação SHA-256]
  Download do zip de produtos para o storage local
  Validação de checksum SHA-256 e extração protegida contra Zip Slip
           |
           v
[Etapa 6: Inspeção de Qualidade GeoTools]
  GET /api/v1/interferometry/jobs/{job_id}/inspection?aoiId=1e7a1465-c3d7-44e6-8398-5ced7dac17ec
  Emissão do laudo de qualidade (ACCEPTED / REJECTED)
```

---

## 7. Critérios de Rejeição e Tratamento de Contingências

* **Falha de Autenticação (401/403):** O adaptador emite log descritivo `[HYP3_AUTH_FAIL]` sem expor senhas e preserva o estado local.
* **Checksum SHA-256 Inválido:** O download é abortado antes de qualquer processamento e o arquivo corrompido é sumariamente descartado.
* **Tentativa de Zip Slip:** Bloqueada com `SecurityException` caso alguma entrada do arquivo zip tente escapar do diretório estruturado do job.
* **Coerência Crítica (< 0.20):** Se a coerência em Mutange for insuficiente, o par é marcado como rejeitado para medição de deslocamento milimétrico, sugerindo a avaliação do par multi-burst adjacente ou janela temporal diferenciada.
