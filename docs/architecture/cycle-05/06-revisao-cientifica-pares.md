# Radar++ — Revisão Científica e Auditoria da Seleção de Pares Interferométricos

## 1. Contexto e Objetivo

Este documento consolida a revisão técnica e científica da modelagem de pares interferométricos implementada no **Ciclo 5.3**, estabelecendo os fundamentos metodológicos que regerão a validação física prévia antes de qualquer submissão computacional a processadores InSAR (como ISCE2, SNAP ou MintPy).

O objetivo central é garantir que as avaliações de compatibilidade sejam **fundamentadas na física de micro-ondas**, **matematicamente rastreáveis** e **desacopladas de implementações computacionais pesadas**, prevenindo desperdício de recursos em processamentos destinados à descorrelação de fase.

---

## 2. Princípios Físicos de Formação do Sinal Interferométrico

A interferometria SAR (InSAR) explora a diferença de fase interferométrica ($\Delta \phi$) entre duas aquisições complexas coerentes de radar ($S_1$ e $S_2$):
$$\Delta \phi = \phi(t_2) - \phi(t_1) = \phi_{\text{topografia}} + \phi_{\text{deformação}} + \phi_{\text{atmosfera}} + \phi_{\text{órbita}} + \phi_{\text{ruído}}$$

Para que o sinal de deformação milimétrica ($\phi_{\text{deformação}}$) possa ser extraído confiavelmente, a coerência complexa ($\gamma$) entre as duas cenas deve ser maximizada ($|\gamma| \ge 0.3\text{ a }0.4$ no limiar operacional):
$$\gamma = \gamma_{\text{espacial}} \cdot \gamma_{\text{temporal}} \cdot \gamma_{\text{térmica}}$$

### 2.1. Descorrelação Espacial e Linha de Base Perpendicular ($B_\perp$)
A coerência espacial ($\gamma_{\text{espacial}}$) decai linearmente com o aumento da linha de base perpendicular ($B_\perp$) em relação à linha de base crítica ($B_{\perp,\text{crítico}}$):
$$\gamma_{\text{espacial}} = 1 - \frac{|B_\perp|}{B_{\perp,\text{crítico}}}, \quad \text{onde } B_{\perp,\text{crítico}} = \frac{\lambda \cdot R \cdot \tan(\theta - \alpha)}{2 \cdot \rho_r}$$

Para o satélite Sentinel-1:
- Comprimento de onda na Banda C: $\lambda \approx 0.0554657\text{ m}$ ($5.55\text{ cm}$).
- Distância slant range ($R$): $\approx 850\text{ km}$.
- Ângulo de incidência médio ($\theta$): $\approx 35^\circ \text{ a } 43^\circ$.
- Resolução em range ($\rho_r$): $\approx 2.7\text{ a }3.5\text{ m}$.
- Graças ao tubo orbital estrito mantido ativamente pela ESA ($\pm 50\text{ a }100\text{ m}$), a imensa maioria dos pares Sentinel-1 apresenta $|B_\perp| < 150\text{ m}$, muito inferior ao limite crítico teórico ($\approx 5000\text{ m}$), o que preserva excelente coerência geométrica.

### 2.2. Descorrelação Temporal ($\Delta t$) em Clima Tropical Úmido
Diferente da coerência geométrica, a coerência temporal ($\gamma_{\text{temporal}}$) é o fator preponderante de perda de sinal em regiões como o **Piloto Maceió/AL**:
- A cobertura vegetal de restinga e mata atlântica circundante, combinada com variações de umidade do solo na bacia da Lagoa Mundaú, provoca descorrelação volumétrica e de espalhamento após 24 dias na banda C.
- Enquanto superfícies áridas mantêm coerência por meses, no bioma de Maceió o limiar máximo seguro para pares interferométricos convencionais situa-se na faixa de **$\Delta t \in [12, 24\text{ dias}]$**.
- Pares com $\Delta t > 36\text{ dias}$ sobre áreas não-urbanizadas degradam a fase em níveis que impossibilitam o desdobramento (*phase unwrapping*) confiável.

---

## 3. Auditoria do Modelo de Domínio e Invariantes

### 3.1. Não-Comutatividade da Relação de Par
O par interferométrico é formalmente modelado em [InterferometricPair.java](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/main/java/com/ass/br/apiradar/interferometry/domain/model/InterferometricPair.java) como uma relação direcionada:
$$\text{Par}(S_{\text{ref}}, S_{\text{sec}}) \neq \text{Par}(S_{\text{sec}}, S_{\text{ref}})$$
- A cena de referência ($S_{\text{ref}}$) define o sistema de coordenadas de *zero doppler slant range* para o qual a cena secundária ($S_{\text{sec}}$) será reamostrada durante o co-registro.
- Uma inversão na ordenação temporal inverte o sinal algébrico da fase de deformação ($\Delta \phi \propto -\frac{4\pi}{\lambda}\Delta r$), induzindo diagnósticos falsos de subsidência em vez de soerguimento se não rastreado explicitamente.

### 3.2. Deduplicação e Unicidade
O [PairFormationService.java](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/src/main/java/com/ass/br/apiradar/interferometry/domain/services/PairFormationService.java) assegura que:
- $S_{\text{ref}} \neq S_{\text{sec}}$ (rejeição mandatória `DUPLICATE_SCENES`).
- Cenas combinadas pertencem rigorosamente ao mesmo track orbital e direção de voo.
- Pares inversos cronologicamente ($t_{\text{ref}} \ge t_{\text{sec}}$) são invalidados por `INVALID_TEMPORAL_ORDER`.

---

## 4. Matriz Científica de Elegibilidade e Estados

A máquina de estados dos pares InSAR discrimina estritamente o nível de maturidade e confiabilidade da avaliação:

| Estado | Critérios Necessários | Evidências Mínimas | Pode Submeter ao Processador? |
| :--- | :--- | :--- | :--- |
| **`CANDIDATE`** | Cenas no mesmo track orbital e direção. | Identificadores válidos e datas conhecidas. | **NÃO** |
| **`PRELIMINARILY_COMPATIBLE`** | Modo `IW SLC`, co-polarização, $\Delta t$ dentro da política. | Metadados de produto e intervalo temporal calculados. | **NÃO** |
| **`PENDING_VALIDATION`** | Preliminarmente compatível, mas $B_\perp$ desconhecido ou órbitas precisas POEORB pendentes. | Falta de efemérides definitivas na fonte (ESA/ASF). | **NÃO** (Bloqueio estrito) |
| **`REJECTED`** | Violação de órbita, modo, polarização, $\Delta t$, $|B_\perp|$ ou sobreposição $< 80\%$. | Registro do motivo formal e limiar violado. | **NÃO** |
| **`ELIGIBLE_FOR_SUBMISSION`** | Todos os critérios atendidos, $|B_\perp|$ conhecido e dentro do limite, órbita validada. | Parecer técnico completo com baseline e geometria consolidadas. | **SIM** (Apenas após autorização) |

> [!IMPORTANT]
> **Invariante Mandatório de Baseline Desconhecida:** A indisponibilidade de dados de baseline perpendicular na fonte externa nunca autoriza a transição para `ELIGIBLE_FOR_SUBMISSION`. O par deve permanecer retido como `PENDING_VALIDATION`, com a limitação explicitamente registrada no parecer técnico.

---

## 5. Escalabilidade e Proteção Operacional do Servidor

Para prevenir exaustão de memória ou estouro de timeout HTTP sob combinações polinomiais $O(N^2)$:
1. **Filtragem Prévia:** As cenas são particionadas em grupos orbitais disjuntos antes de qualquer combinação. Cenas de tracks diferentes nunca geram iterações combinatórias.
2. **Teto de Cenas por Grupo (`MAX_SCENES_PER_ORBITAL_GROUP = 50`):** Se uma busca retornar mais de 50 cenas para um mesmo track, o algoritmo retém as 50 aquisições mais recentes.
3. **Limite Operacional de Pares Retornados (`MAX_EVALUATED_PAIRS_RETURNED = 100`):** Máximo de 100 pares retornados na resposta HTTP, priorizando deterministicamente o menor intervalo temporal ($\Delta t$).
4. **Ordenação Determinística Estrita:**
   $$\text{Ordem} = \Delta t \ \uparrow \ \longrightarrow \ t_{\text{ref}} \ \downarrow \ \longrightarrow \ \text{ID do Par}$$
5. **Salvaguarda de Políticas Customizadas:**
   O método `PairSelectionPolicy.createCustom()` impõe tetos invioláveis do servidor:
   - $\Delta t \in [6, 90\text{ dias}]$ (mesmo se o cliente solicitar 300 dias).
   - $|B_\perp| \le 300.0\text{ m}$ (mesmo se o cliente solicitar 1000 m).
   - Sobreposição espacial $\ge 50\%$.

---

## 6. Conclusão da Revisão

O modelo implementado no Ciclo 5.3 e refinado no 5.3.1 estabelece uma fundação científica robusta, prevenindo combinações inválidas antes de qualquer consumo de processamento pesado, e garantindo total rastreabilidade das decisões de elegibilidade.
