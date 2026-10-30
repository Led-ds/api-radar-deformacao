# Radar++ — Validação de Metadados Orbitais e Baseline Perpendicular

## 1. Sumário e Propósito

Este documento analisa em profundidade a validação de metadados orbitais para o sensor **Sentinel-1 C-Band SAR**, detalhando o comportamento do adaptador [AsfBaselineClientAdapter.java](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/main/java/com/ass/br/apiradar/interferometry/infrastructure/adapters/output/external/AsfBaselineClientAdapter.java), a convenção de sinais de linha de base e os requisitos de sincronização de feixe (TOPSAR bursts).

---

## 2. Tipologia de Efemérides Orbitais Sentinel-1 (ESA)

A exatidão da determinação da linha de base perpendicular e a remoção das franjas de rampa orbital no interferograma dependem criticamente da qualidade das efemérides orbitais fornecidas pela Agência Espacial Europeia (ESA):

| Tipo de Arquivo Orbital | Sigla | Latência Pós-Aquisição | Precisão dos Vetores de Estado | Aplicação no Radar++ |
| :--- | :--- | :--- | :--- | :--- |
| **Órbitas Preditas** | `PREORB` | Antes da passagem orbital | $\sim 1\text{ a }5\text{ m}$ | Inadequado para InSAR (apenas planejamento de apontamento). |
| **Órbitas Restituídas** | `RESORB` | $\approx 3\text{ horas}$ após aquisição | $\sim 10\text{ cm}$ | Utilizado para pré-seleção e avaliação preliminar de pares no Ciclo 5.3. |
| **Órbitas Precisas Definitivas** | `POEORB` | $\mathbf{20\text{ a }25\text{ dias}}$ após aquisição | $\mathbf{< 5\text{ cm}}$ (determinação via GNSS pós-processado) | **Obrigatório** para processamento interferométrico e inversão multitemporal (SBAS/PS-InSAR). |

### 2.1. Implicação Arquitetural da Janela POEORB
- Pares com menos de 20 dias de aquisição possuem apenas órbitas `RESORB`.
- Mesmo que $B_\perp$ e $\Delta t$ estejam dentro dos limiares, o par **não pode transitar para `ELIGIBLE_FOR_SUBMISSION`** quando o perfil científico exigir órbita precisa (`requirePreciseOrbit = true`). Ele deve aguardar a sincronização de arquivos `AUX_POEORB` em estado `PENDING_VALIDATION`.

---

## 3. Linha de Base Perpendicular: Convenção de Sinal e Geometria

A linha de base física ($\vec{B}$) é o vetor que une a posição do centro de fase da antena no instante de aquisição da cena de referência ($S_{\text{ref}}$) à posição na cena secundária ($S_{\text{sec}}$).

```
                      Órbita Secundária (S_sec)
                             /|
                            / |
                   B_paral /  | B_perp
                          /   |
                         /    |
   Órbita Ref (S_ref) --*-----v
                         \  θ
                          \
                           \  Linha de Visada (LOS)
                            \
                             v  Alvo no Solo (AOI Maceió)
```

### 3.1. Convenção de Sinal Formal
No Radar++, a linha de base perpendicular é representada pelo Value Object [BaselineValue.java](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/main/java/com/ass/br/apiradar/interferometry/domain/model/BaselineValue.java):
- **Unidade:** Metros (`meters`).
- **Convenção de Sinal:**
  - $B_\perp > 0$: O satélite secundário está posicionado deslocado para a direita/acima da linha de visada em relação ao satélite de referência.
  - $B_\perp < 0$: O satélite secundário está posicionado para a esquerda/abaixo da linha de visada.
- **Magnitude:** A perda de coerência espacial é proporcional a $|B_\perp|$ (`absoluteMeters()`), de modo que sinais positivos ou negativos com magnitudes iguais possuem idêntico impacto de descorrelação geométrica.

---

## 4. Auditoria do Adaptador `AsfBaselineClientAdapter`

O componente [AsfBaselineClientAdapter.java](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/main/java/com/ass/br/apiradar/interferometry/infrastructure/adapters/output/external/AsfBaselineClientAdapter.java) implementa a porta [PerpendicularBaselinePort](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/main/java/com/ass/br/apiradar/interferometry/domain/ports/output/PerpendicularBaselinePort.java):

1. **Fonte Oficial:** Integração direta com o endpoint oficial de baseline da Alaska Satellite Facility (`https://api.daac.asf.alaska.edu/services/search/baseline?reference={masterGranule}`).
2. **Identificação da Origem:** Cada valor retornado carrega a proveniência `BaselineSource.ASF_BASELINE_SERVICE` ou `BaselineSource.UNAVAILABLE`.
3. **Resiliência contra Falhas:** Caso a API externa retorne erro HTTP (ex: 500/503), timeout de 6 segundos ou resposta nula, o adaptador:
   - **NÃO** inventa valores numéricos fictícios fora de testes.
   - **NÃO** lança exceções fatais que quebrem o carregamento da lista de pares.
   - **RETORNA** `BaselineValue.unavailable()`.
   - Como consequência, o par permanece categorizado como `PENDING_VALIDATION`, com a justificativa técnica explícita: *"A linha de base perpendicular (B⊥) ainda não está disponível na fonte; o par permanece PENDENTE DE VALIDAÇÃO e não pode ser classificado como elegível sem confirmação da baseline."*

---

## 5. Particularidades do Modo TOPSAR Sentinel-1: Subswaths e Bursts

Diferente de sensores SAR convencionais em modo Stripmap (como ALOS PALSAR ou ERS-1/2), o Sentinel-1 opera primariamente no modo **Terrain Observation with Progressive Scans SAR (TOPSAR)**:

1. **Estrutura por Bursts:** Uma cena Sentinel-1 IW SLC não é um raster contínuo único, mas um mosaico de 3 subswaths (IW1, IW2, IW3), cada um subdividido em 9 a 10 *bursts* individuais de $\sim 20\text{ km} \times 85\text{ km}$.
2. **Rampa Doppler Acentuada:** No modo TOPSAR, a antena do radar é rotacionada eletronicamente no azimute ao longo de cada burst, gerando uma taxa de variação do centroide Doppler extremamente alta.
3. **Requisito Crítico de Co-registro:** Para que dois bursts possam ser combinados em um par interferométrico:
   - A precisão de co-registro azimutal deve ser inferior a **$0.001\text{ pixel}$** (Enhanced Spectral Diversity - ESD).
   - Não se deve presumir que cenas do mesmo track possuam automaticamente bursts cobrindo a totalidade da AOI; as anotações XML dos bursts devem ser lidas para assegurar coincidência de footprints elementares.

No Ciclo 5.3 e 5.3.1, a verificação desse requisito foi formalmente incorporada à lista de critérios avaliados do par:
`"Sincronização de bursts e subswaths TOPSAR IW (anotações XML)"`, garantindo que na fase de processamento o fatiador de dados brutos trabalhe apenas com os bursts interceptados.
