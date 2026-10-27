# Radar++ Discovery — D-ENC-02: Matriz Consolidada de Decisões, Hipóteses e Parâmetros
## Mapeamento Integrado do Baseline Investigativo para Transição à Engenharia de Domínio

**Documento:** `D-ENC-02-matriz-decisoes-hipoteses.md`  
**Ciclo:** Encerramento do Discovery e Preparação da Engenharia de Domínio  
**Data:** 24 de Outubro de 2026  
**Status:** Oficial — Entregue para Homologação da Governança  
**Responsável:** Equipe Multidisciplinar (Product Manager, Arquiteto de Software, Analista de Negócios)  
**Repositório Oficial:** `api-radar-deformacao` (caminho: `docs/discovery/D-ENC-02-matriz-decisoes-hipoteses.md`)  

---

## 1. Matriz de Decisões Aprovadas (Fatos de Governança e Produto)

```
+----------------------------------------------------------------------------------------------------------------------------------------------------+
|                                      MATRIZ DE DECISÕES APROVADAS (BASELINE OFICIAL)                                                               |
+----+----------------------------------+-----------------------+------------------------------------+-----------------------------------------------+
| ID | Decisão Aprovada                 | Classificação         | Documento de Origem                | Justificativa Estratégica Consolidada         |
+----+----------------------------------+-----------------------+------------------------------------+-----------------------------------------------+
| DA1| Hierarquia Oficial de Prioridades| **Decisão Aprovada**  | [D2-08](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/docs/discovery/D2-08-comparativo-d1-d2.md#L75-L87), [D3-10](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/docs/discovery/D3-10-parecer-consolidado.md#L20-L32) | 1. Subsidência Urbana; 2. Infraestrutura Line-|
|    | (1. Urbana; 2. Infra; 3. Canal)  |                       |                                    | ar; 3. Consultorias e Pesquisa Científica.    |
+----+----------------------------------+-----------------------+------------------------------------+-----------------------------------------------+
| DA2| Maceió/AL como Piloto Candidato  | **Decisão Aprovada**  | [D1-05](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/docs/discovery/D1-05-protocolo-piloto-maceio.md), [D3-08](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/docs/discovery/D3-08-roadmap-estrategico.md#L30-L45) | Única área brasileira com dados geodésicos e |
|    | da Validação Científica Retrospec|                       |                                    | laudos públicos consolidados no SGB/CPRM.     |
+----+----------------------------------+-----------------------+------------------------------------+-----------------------------------------------+
| DA3| Posicionamento como Ferramenta   | **Decisão Aprovada**  | [D3-01](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/docs/discovery/D3-01-tese-produto.md#L45-L55), [D3-05](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/docs/discovery/D3-05-catalogo-regras.md#L39-L54) | Produto atua na triagem macro e análise retro-|
|    | Analítica e de Apoio à Decisão   |                       |                                    | spectiva; não emite alertas de emergência 24h.|
+----+----------------------------------+-----------------------+------------------------------------+-----------------------------------------------+
| DA4| Isenção Formal de Emissão de ART | **Decisão Aprovada**  | [D3-05](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/docs/discovery/D3-05-catalogo-regras.md#L22-L37) (RN-01)          | Responsabilidade técnica sobre obras e laudos |
|    | pelo Software Radar++            |                       |                                    | periciais cabe ao engenheiro com registro CREA|
+----+----------------------------------+-----------------------+------------------------------------+-----------------------------------------------+
| DA5| Padrão Operacional em Órbita     | **Decisão Aprovada**  | [D2-05](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/docs/discovery/D2-05-viabilidade-insar.md#L75-L87), [D3-05](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/docs/discovery/D3-05-catalogo-regras.md#L62-L78) | Órbita única reduz à metade o custo na nuvem; |
|    | Única (LOS) com Bi-Orbital Opc.  |                       |                                    | combinação bi-orbital é funcionalidade sob dem|
+----+----------------------------------+-----------------------+------------------------------------+-----------------------------------------------+
| DA6| Arquitetura Modular Conceitual   | **Decisão Aprovada**  | [D3-04](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/docs/discovery/D3-04-capacidades-produto.md#L18-L45)              | Divisão de produto em Core, Urban e Infra sem |
|    | (Core, Urban, Infra)             |                       |                                    | criação prematura de microsserviços ou bancos.|
+----+----------------------------------+-----------------------+------------------------------------+-----------------------------------------------+
```

---

## 2. Matriz de Hipóteses Comerciais sob Validação

```
+----------------------------------------------------------------------------------------------------------------------------------------------------+
|                                    MATRIZ DE HIPÓTESES COMERCIAIS (A TESTAR NO CICLO SEGUINTE)                                                     |
+----+----------------------------------+-----------------------+------------------------------------+-----------------------------------------------+
| ID | Hipótese Comercial Investigada   | Estado Atual          | Mecanismo de Validação             | Critério de Aceite / Refutação                |
+----+----------------------------------+-----------------------+------------------------------------+-----------------------------------------------+
| HC1| Preferência pelo Modelo sob      | Hipótese Prioritária  | Roteiro de Entrevistas D1-04       | Pelo menos 3 de 5 consultorias afirmarem que  |
|    | Demanda / Por Laudo (Modelo M3)  | (Não Validada)        | com consultorias de engenharia.    | contratariam diagnósticos pontuais sob demanda|
+----+----------------------------------+-----------------------+------------------------------------+-----------------------------------------------+
| HC2| Disposição a Pagar de Concessio- | Hipótese Aberta       | Entrevistas D1-04 com gestores de  | Ao menos 2 concessionárias manifestarem inte- |
|    | nárias por Km Monitorado (M1)    | (Alto Risco)          | conservação de concessionárias.    | resse em piloto pago de corredor linear.      |
+----+----------------------------------+-----------------------+------------------------------------+-----------------------------------------------+
| HC3| Aderência do Canal B2B2B via     | Hipótese Estratégica  | Mapeamento de parcerias com PMEs   | Consultorias aceitarem ofertar o Radar++ como |
|    | Consultorias de Engenharia (M4)  | (Em Avaliação)        | de geotecnia viária e urbana.      | camada de inovação com a sua própria marca.   |
+----+----------------------------------+-----------------------+------------------------------------+-----------------------------------------------+
| HC4| Ciclo de Vendas B2B Corporativo  | Hipótese Estimativa   | Acompanhamento de propostas de     | Ciclo de negociação com concessionária fechar |
|    | Inferior a 6 Meses               | (Incerteza Alta)      | homologação de fornecedores.       | entre 3 e 6 meses (se > 12m, refuta modelo).  |
+----+----------------------------------+-----------------------+------------------------------------+-----------------------------------------------+
```

---

## 3. Matriz de Parâmetros Científicos Provisórios

```
+----------------------------------------------------------------------------------------------------------------------------------------------------+
|                                    MATRIZ DE PARÂMETROS CIENTÍFICOS (SUJEITOS A CALIBRAÇÃO EXPERIMENTAL)                                           |
+----+----------------------------------+-----------------------+------------------------------------+-----------------------------------------------+
| ID | Parâmetro Científico Provisório  | Valor Teórico Inicial | Faixa de Ajuste Admissível         | Dependência de Validação                      |
+----+----------------------------------+-----------------------+------------------------------------+-----------------------------------------------+
| PC1| Limiar de Coerência Mínima (RT4) | $\bar{\gamma} = 0{,}40$| $0{,}25 \le \bar{\gamma} \le 0{,}60$| Teste de cobertura de pontos em Maceió/AL.    |
+----+----------------------------------+-----------------------+------------------------------------+-----------------------------------------------+
| PC2| Linha de Base Perpendicular (RT5)| $B_\perp \le 150\text{ m}$| $50\text{ m} \le B_\perp \le 250\text{ m}$| Verificação de ruído decorrente de topografia.|
+----+----------------------------------+-----------------------+------------------------------------+-----------------------------------------------+
| PC3| Linha de Base Temporal (RT6)     | $\Delta t \le 48\text{ dias}$| $12\text{ dias} \le \Delta t \le 96\text{ dias}$| Análise de descorrelação sazonal chuvosa.     |
+----+----------------------------------+-----------------------+------------------------------------+-----------------------------------------------+
| PC4| Tolerância a Resíduos Unwrapping | $\le 15\%$ da AOI     | $5\% \le \text{Resíduo} \le 25\%$  | Identificação de saltos de fase de $2\pi$.    |
+----+----------------------------------+-----------------------+------------------------------------+-----------------------------------------------+
| PC5| Área Máxima por AOI Urbana (RT1) | $100\text{ km}^2$     | $10\text{ km}^2 \le A \le 500\text{ km}^2$| Monitoramento de consumo de RAM no MintPy.    |
+----+----------------------------------+-----------------------+------------------------------------+-----------------------------------------------+
```

---

## 4. Matriz de Riscos Não Resolvidos e Evidências Pendentes

```
+----------------------------------------------------------------------------------------------------------------------------------------------------+
|                                    RISCOS CRÍTICOS E EVIDÊNCIAS PENDENTES                                                                          |
+----+----------------------------------+-----------------------+------------------------------------+-----------------------------------------------+
| ID | Risco Crítico / Evidência        | Severidade            | Ação de Controle na Transição      | Fase Responsável pela Solução                 |
+----+----------------------------------+-----------------------+------------------------------------+-----------------------------------------------+
| R1 | Descorrelação por Mata Atlântica | **Alta (Geofísica)**  | Máscara automática de incoerência; | Fase 3 (Avaliar dados futuros em Banda L /    |
|    | em encostas de serra rodoviárias |                       | delimitação restrita à pista.      | NISAR quando operacionais).                   |
+----+----------------------------------+-----------------------+------------------------------------+-----------------------------------------------+
| R2 | Dependência de Provedores SAR    | **Média (Arquitet.)** | Projeto de abstração de catálogo   | Fase 2 (Implementação do Bounded Context      |
|    | (APIs externas da ESA e ASF)     |                       | com suporte a múltiplos provedores.| de Catálogo SAR com isolamento por Anti-Corrup|
+----+----------------------------------+-----------------------+------------------------------------+-----------------------------------------------+
| E1 | Validação da Acurácia em Campo   | **Crítica (Científica)**| Não alegar validação antes do teste| **Fase 0 (Plano de Execução do Piloto Maceió**|
|    | ($R^2 \ge 0{,}85$ vs. SGB/CPRM)  |                       | formal e auditoria de resíduos.    | detalhado no documento `D-ENC-03`).           |
+----+----------------------------------+-----------------------+------------------------------------+-----------------------------------------------+
| E2 | Validação da Disposição a Pagar  | **Crítica (Comercial)**| Não assumir contratos antes das en-| **Fase 1 (Entrevistas Qualitativas D1-04**    |
|    | em Concessionárias e Consultorias|                       | trevistas de descoberta B2B.       | com agentes reais do setor de transportes).   |
+----+----------------------------------+-----------------------+------------------------------------+-----------------------------------------------+
```
