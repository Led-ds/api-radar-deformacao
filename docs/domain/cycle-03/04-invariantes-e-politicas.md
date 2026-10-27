# Radar++ — Domínio: 04-invariantes-e-politicas.md
## Invariantes de Domínio, Políticas e Ajustes Metodológicos Obrigatórios

**Documento:** `04-invariantes-e-politicas.md`  
**Ciclo:** Ciclo 3 — Modelagem Estratégica e Tática com DDD  
**Data:** 24 de Outubro de 2026  
**Status:** Oficial — Entregue para Homologação de Arquitetura de Domínio  
**Responsável:** Arquiteto de Software, Especialista em DDD, Especialista em SAR/InSAR  
**Repositório Oficial:** `api-radar-deformacao` (caminho: `docs/domain/cycle-03/04-invariantes-e-politicas.md`)  

---

## 1. Ajustes e Correções Estruturais de Domínio

Em estrito atendimento às diretrizes do Ciclo 3, as seguintes premissas conceituais foram formalmente corrigidas em relação a rascunhos anteriores:

```
+----------------------------------------------------------------------------------------------------------------------------------------------------+
|                                    QUADRO DE SANEAMENTO CONCEITUAL DAS REGRAS DE DOMÍNIO                                                           |
+----+-----------------------+---------------------------------------+---------------------------------------+---------------------------------------+
| ID | Tópico Crítico        | Formulação Anterior Superada          | Formulação Oficial Corrigida (Ciclo 3)| Racional de Engenharia e Negócio      |
+----+-----------------------+---------------------------------------+---------------------------------------+---------------------------------------+
| C1 | Escopo de             | Multi-tenancy descrito como complexo  | **Multi-tenancy é modelado apenas como**| No MVP, trata-se de posse lógica de   |
|    | Multi-Tenancy         | isolamento de dados distribuído SaaS. | **pertencimento organizacional**      | dados (`OrganizationId`), sem impor   |
|    |                       |                                       | (`OrganizationId` na raiz de AOI).    | infraestrutura complexa desnecessária.|
+----+-----------------------+---------------------------------------+---------------------------------------+---------------------------------------+
| C2 | Responsabilidade      | Descrita como mera cláusula de        | **Fronteira Formal de Competência**   | O software mede deformação do terreno |
|    | Técnica e ART         | isenção de responsabilidade jurídica. | **Técnica e Legal**: o Radar++ calcula| mas não substitui a responsabilidade  |
|    |                       |                                       | dados geodésicos; a ART é do perito.  | privativa do engenheiro civil CREA.   |
+----+-----------------------+---------------------------------------+---------------------------------------+---------------------------------------+
| C3 | Parâmetros            | Limiares de 0,40 de coerência, 150m   | **Parâmetros Parametrizáveis e Não**  | Limiares dependem da frequência do    |
|    | Científicos           | de baseline descritos como universais.| **Universais**: configuráveis conforme| radar, clima e tipo de solo (ex.: solo|
|    |                       |                                       | a metodologia e a região em análise.  | urbano vs. encostas tropicais de serra|
+----+-----------------------+---------------------------------------+---------------------------------------+---------------------------------------+
| C4 | Papel do Formato      | COG descrito como se fosse a própria  | **COG é uma representação/formato de**| O Produto InSAR é a matriz geodésica; |
|    | COG                   | definição universal de Produto InSAR. | **entrega de produto**, não a sua     | COG é o formato otimizado de storage  |
|    |                       |                                       | essência de domínio.                  | para viabilizar HTTP Range Requests.  |
+----+-----------------------+---------------------------------------+---------------------------------------+---------------------------------------+
| C5 | Rastreabilidade       | Descrito como "prova pericial certi-  | **Registro Técnico Auditável de Pro-**| O sistema assegura reprodutibilidade  |
|    | do Histórico          | ficada" (termo legal impróprio).      | **veniência**: cadeia de custódia     | e auditoria interna, sem prometer cer-|
|    |                       |                                       | técnica dos parâmetros e versões.     | tificação forense automática ICP-BR.  |
+----+-----------------------+---------------------------------------+---------------------------------------+---------------------------------------+
| C6 | Distinção Geodésica   | Risco de confusão entre velocidade    | **Distinção Semântica Estrita entre** | d_LOS é a grandeza primária soberana. |
|    | LOS vs. Vertical      | na visada e recalque vertical puro.   | **d_LOS e d_vert** em todo o domínio. | Projeção vertical não é universal.    |
+----+-----------------------+---------------------------------------+---------------------------------------+---------------------------------------+
```

---

## 2. Modelagem Geodésica: LOS e Projeção Vertical

O Radar++ adota a **deformação na Linha de Visada (*Line of Sight* — $d_{\text{LOS}}$) como a grandeza primária e mandatória** em todo o domínio e no MVP.

### 2.1. Formulação da Projeção e Hipóteses Físicas
A aproximação escalar de deslocamento vertical $d_{\text{vert}} = d_{\text{LOS}} / \cos\theta$ (onde $\theta$ é o ângulo de incidência local do radar) **NÃO constitui uma regra universal de domínio**, aplicando-se unicamente sob as seguintes condições estritas:
1. **Hipótese de Deformação Unidimensional Estritamente Vertical:** O vetor de deslocamento do terreno é modelado assumindo deslocamento horizontal negligenciável ($d_{\text{East}} \approx 0$ e $d_{\text{North}} \approx 0$);
2. **Registro Explícito do Ângulo de Incidência Local:** $\theta$ varia ao longo do alcance do feixe radar ($\approx 31^\circ$ a $46^\circ$ no Sentinel-1 IW), devendo ser computado pontualmente a partir do modelo geométrico do satélite e da topografia (DEM).

### 2.2. Convenção de Sinais
- **$d_{\text{LOS}}$ (Grandeza Primária):**
  - Valor **Negativo** ($d_{\text{LOS}} < 0$): Afastamento em relação ao sensor satelital (típico de abatimento/subsidência do terreno em visada descendente ou ascendente);
  - Valor **Positivo** ($d_{\text{LOS}} > 0$): Aproximação em relação ao sensor satelital (típico de soerguimento ou expansão de solo).
- **$d_{\text{vert}}$ (Grandeza Derivada Condicional):**
  - Valor **Negativo** ($d_{\text{vert}} < 0$): Subsidência ou rebaixamento vertical;
  - Valor **Positivo** ($d_{\text{vert}} > 0$): Soerguimento ou elevação vertical.

### 2.3. Limitações e Situações Onde a Projeção Vertical é Proibida
A projeção por $1/\cos\theta$ **não deve ser aplicada e é semanticamente inválida** nas seguintes situações:
- **Taludes, Encostas e Zonas de Instabilidade de Vertente:** Onde o vetor de escorregamento possui componente horizontal gravitacional dominante ao longo da linha de maior declive;
- **Bordas Periféricas de Bacias de Subsidência:** Onde a tração e a flexão extensional geram deformações horizontais significativas em direção ao centro da bacia;
- **Falhas Geológicas com Movimento Direcional (*Strike-slip*):** Onde o deslocamento é dominantemente horizontal;
- **Ambientes sem Decomposição Bi-Orbital:** A verdadeira separação vetorial 2D/3D (vertical vs. leste-oeste) exige a combinação de órbitas independentes (Ascendente e Descendente). Onde apenas uma órbita estiver disponível, o domínio opera estritamente com $d_{\text{LOS}}$.

---

## 3. Invariantes de Domínio por Agregado

As invariantes garantem que um agregado nunca transicione para um estado corrompido ou matematicamente inconsistente:

### 3.1. Invariantes do Agregado `AreaOfInterest`
* **INV-AOI-01 (Fechamento e Não-Interseção):** A geometria vetorial (`PolygonGeometry`) deve possuir o anel exterior estritamente fechado (primeiro vértice idêntico ao último) e ser topologicamente simples, sem laços de auto-interseção (*isSimple = true*).
* **INV-AOI-02 (Posse Organizacional):** Toda instância de `AreaOfInterest` deve ser instanciada obrigatoriamente com um `OrganizationId` válido e não nulo.
* **INV-AOI-03 (Consistência BBOX e Geometria):** A `BoundingBox` de busca de catálogo e a geometria efetiva da AOI são mantidas separadas: a BBOX é o envelope mínimo retangular para queries em APIs SAR, enquanto a geometria efetiva restringe o cômputo e as estatísticas.

### 3.2. Invariantes do Agregado `ProcessingRequest`
* **INV-REQ-01 (Ordenação Temporal):** A data de término da janela temporal solicitada deve ser estritamente posterior à data de início.
* **INV-REQ-02 (Imutabilidade Pós-Autorização):** Uma vez transicionada para o estado `AUTHORIZED`, os parâmetros geodésicos solicitados tornam-se congelados, não permitindo alterações posteriores.

### 3.3. Invariantes do Agregado `ProcessingJob`
* **INV-JOB-01 (Transição Canônica de Estados):** O job só pode atingir o estado `SUCCEEDED` se tiver transitado previamente por `PROCESSING`.
* **INV-JOB-02 (Finalidade de Estados Terminais):** Os estados `SUCCEEDED`, `FAILED` e `CANCELLED` são estados finais irreversíveis. Nenhuma nova transição de progresso é aceita após atingir um estado terminal.
* **INV-JOB-03 (Limite de Retentativas):** Em caso de falhas transitórias de infraestrutura, o número de retentativas registradas no `RetryCounter` não pode exceder o teto máximo configurado na política operacional.

### 3.4. Invariantes do Agregado `InSarAnalysis`
* **INV-ANA-01 (Gating Obrigatório de Qualidade):** A análise nunca pode transicionar para o estado `APPROVED_FOR_VISUALIZATION` sem que o método `evaluateQuality()` tenha sido executado e preenchido a `QualityAssessment`.
* **INV-ANA-02 (Homogeneidade Orbital dos Produtos):** Todos os produtos atrelados à análise devem obrigatoriamente compartilhar o mesmo `OrbitPath` e direção geométrica.
* **INV-ANA-03 (Grandeza Primária Soberana):** Todas as observações de velocidade e séries temporais de deslocamento mantêm como dado primário o valor em $\text{mm}_{\text{LOS}}$ no objeto `DisplacementValue`.

### 3.5. Invariantes do Agregado `UrbanInvestigation`
* **INV-URB-01 (Vínculo com Análise Aprovada ou Restrita):** Uma investigação urbana só pode ser vinculada a uma `InSarAnalysis` que esteja aprovada para visualização ou qualificada com restrições.
* **INV-URB-02 (Isenção de Diagnóstico Geotécnico Automático):** O agregado é estritamente um ambiente de observação e exploração. O sistema proíbe a geração de relatórios com alegação de laudo pericial oficial, atestado de segurança estrutural ou previsão garantida de colapso. Toda nota técnica emitida exige ressalva de limites metodológicos e necessidade de avaliação geotécnica presencial com ART.

---

## 4. Políticas de Domínio (*Domain Policies*)

### 4.1. Política de Compatibilidade de Cenas (*SceneCompatibilityPolicy*)
* **Regra:** Cenas SAR só podem ser agrupadas para formar pares interferométricos se atenderem simultaneamente a:
  1. Mesmo satélite e sensor (Sentinel-1A ou Sentinel-1B);
  2. Mesmo modo de aquisição operacional (IW — *Interferometric Wide Swath*);
  3. Mesma polarização principal (VV);
  4. Mesmo número de órbita relativa (*Relative Orbit / Path*);
  5. Mesma direção geométrica de passagem orbital.

### 4.2. Política de Conexão de Pares SBAS (*NetworkConnectivityPolicy*)
* **Regra:** A seleção de pares interferométricos em uma rede SBAS obedece aos limiares:
  * Linha de base perpendicular máxima: $B_\perp \le B_{\perp,\text{max}}$ (configurável, default: 150 m);
  * Linha de base temporal máxima: $\Delta t \le \Delta t_{\text{max}}$ (configurável, default: 48 dias);
  * Conectividade mínima de rede para inversão SVD.

### 4.3. Política de Orçamentação e Estimativa de Créditos HyP3 (*CreditEstimationPolicy*)
* **Regra:** O cálculo de créditos na plataforma externa HyP3/ASF **não adota fórmulas simplificadas universais**, decompondo os custos nas seguintes variáveis oficiais:
  1. **Tipo de Job Solicitado:** `INSAR_ISCE_BURST` (processamento por burst individual) vs. `INSAR_GAMMA` (cena completa SLC) vs. `AUTORIFT`;
  2. **Quantidade de Pares Interferométricos ($N_{\text{pares}}$):** Quantidade de interferogramas que compõem a rede temporal;
  3. **Quantidade de Bursts por Data ($N_{\text{bursts}}$):** Determinado pela sobreposição geométrica da AOI com as pegadas dos bursts da faixa orbital;
  4. **Resolução Espacial / Fator de Multilooking:** Resolução solicitada (ex.: 20 m com look $5 \times 1$ vs. 80 m com look $20 \times 4$);
  5. **Tabela de Tarifação Oficial ASF/HyP3:** Cada job do tipo Sentinel-1 Burst InSAR consome 1 crédito HyP3 por execução de burst-par;
  6. **Cálculo da Estimativa Orçamentária:**
     $$\text{Créditos Estimados} = \sum_{j \in \text{Jobs}} \text{CustoTabela}(\text{TipoJob}_j, \text{Resolução}_j, \text{Bursts}_j)$$

### 4.4. Política de Avaliação de Confiabilidade (*QualityGatingPolicy*)
* **Regra:** A classificação final de confiabilidade é determinada por:
  ```
  SE (Resíduos de Desdobramento de Fase <= 15% DA ÁREA)
     E (Coerência Média na AOI >= LimiarConfigurado)
  ENTÃO:
     Status = APPROVED_FOR_VISUALIZATION
  SENÃO:
     Status = QUALIFIED_WITH_RESTRICTIONS
     Flag = "Área sujeita a decorrelação ou saltos de fase. Exige cautela na interpretação."
  ```

### 4.5. Política de Armazenamento e Retenção em Três Níveis (*StorageRetentionPolicy*)
A infraestrutura de armazenamento no AWS S3 é governada por três categorias distintas de artefatos:

```
+----------------------------------------------------------------------------------------------------------------------------------------------------+
|                                    POLÍTICA DE RETENÇÃO E ARMAZENAMENTO EM TRÊS NÍVEIS (AWS S3)                                                    |
+---+-----------------------------+---------------------------------------+-----------------------------+--------------------------------------------+
| N | Categoria de Artefato       | Formatos / Tipos de Arquivo           | Classe S3 e Ciclo de Vida   | Regra de Governança e Integridade          |
+---+-----------------------------+---------------------------------------+-----------------------------+--------------------------------------------+
| 1 | **Produtos Científicos**    | COG (GeoTIFF otimizado para nuvem),   | S3 Standard / Glacier       | **Preservação Permanente:** Versionamento  |
|   | **Finais Preservados**      | NetCDF/HDF5 (séries temporais MintPy),| Instant Retrieval.          | ativado, Object Lock (imutabilidade),      |
|   |                             | GeoJSON/Parquet (pontos PS/SBAS),     | Sem expurgo automático.     | Checksum SHA-256 e cadeia de proveniência. |
|   |                             | JSON (metadados de auditoria).        |                             |                                            |
+---+-----------------------------+---------------------------------------+-----------------------------+--------------------------------------------+
| 2 | **Produtos Intermediários** | Interferogramas unwrapped/wrapped,    | S3 Standard com transição   | **Retenção Configurável:** Mantidos por 30 |
|   | **com Retenção**            | mapas de coerência por par, matrizes  | para Glacier Flexible após  | a 90 dias para permitir re-inversão no     |
|   | **Configurável**            | de correlação de fase.                | 30 dias; expurgo em 90 dias.| MintPy sem reprocessar HyP3.               |
+---+-----------------------------+---------------------------------------+-----------------------------+--------------------------------------------+
| 3 | **Arquivos Temporários**    | Cenas SLC brutas Nível 1 (4-8 GB),    | S3 One Zone-IA / Scratch    | **Expurgo Rápido:** Lifecycle rule com     |
|   | **Descartáveis**            | DEMs intermediários recortados,       | disk local efêmero de worker| expurgo automático em 7 a 14 dias ou       |
|   |                             | logs de processamento de baixo nível. | (EBS/NVMe).                 | exclusão imediata ao término do job.       |
+---+-----------------------------+---------------------------------------+-----------------------------+--------------------------------------------+
```
