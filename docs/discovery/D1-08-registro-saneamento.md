# Radar++ Discovery — D1-08: Registro de Saneamento Metodológico da Investigação D1
## Auditoria Crítica, Reclassificação Epistemológica e Correções Materiais

**Documento:** `D1-08-registro-saneamento.md`  
**Ciclo:** Consolidação D1 e Transição para D2  
**Data:** 24 de Outubro de 2026  
**Status:** Oficial — Entregue para Governança  
**Responsável:** Equipe Multidisciplinar de Inteligência Estratégica, Geotecnia e Regulação  
**Repositório Oficial:** `api-radar-deformacao` (caminho: `docs/discovery/D1-08-registro-saneamento.md`)  

---

## 1. Motivação e Escopo da Auditoria de Saneamento

Durante a produção dos relatórios iniciais da Investigação D1 (`D1-01` a `D1-07`), foram consolidadas diversas premissas preliminares sobre regulação, contratos públicos, tecnologias de radar e comportamento de mercado.

Com o objetivo de manter a integridade documental, evitar que inferências sejam tratadas como certezas regulatórias e preservar a governança do Radar++, este documento registra o **saneamento formal e rastreável de todas as afirmações materiais que exigiram reclassificação epistemológica ou calibração factual**.

---

## 2. Quadro Consolidado de Saneamento e Correções Materiais

```
+-----------------------------------------------------------------------------------------------------------------------------------------------------+
|                                              REGISTRO DE SANEAMENTO E RECLASSIFICAÇÃO METODOLÓGICA (D1)                                             |
+----+-----------------------+-----------------------------+-----------------------------+--------------------+---------------------------------------+
| ID | Tema Auditado         | Afirmação Prévia no D1      | Correção Factual e Fonte    | Classificação Nova | Justificativa do Ajuste               |
+----+-----------------------+-----------------------------+-----------------------------+--------------------+---------------------------------------+
| S1 | Regulação ANTT        | "ANTT exige sistemas de     | A ANTT exige o Sistema de   | FATO quanto ao     | InSAR não é obrigatório por norma.    |
|    | sobre Gestão de       | monitoramento de taludes    | Gestão de Terraplenos e     | SGTEC;             | Concessionárias cumprem a regulação   |
|    | Taludes em Rodovias   | onde o InSAR é obrigatório  | Contenções (SGTEC) baseado  | HIPÓTESE quanto ao | primariamente com inspeções visuais   |
|    |                       | para conformidade."         | na NBR 11682 e DER/SP. Não  | uso de InSAR.      | e topografia; InSAR é voluntário/P&D. |
|    |                       |                             | há exigência nominal de SAR.|                    |                                       |
+----+-----------------------+-----------------------------+-----------------------------+--------------------+---------------------------------------+
| S2 | Programa PROARTE      | "DNIT formalizou a elegibi- | A IN nº 2/2026 estabelece a | FATO quanto à re-  | Na ausência de dispositivo normativo  |
|    | e DNIT (IN nº 2/2026) | lidade específica do InSAR  | orçamentação paramétrica de | formulação;         | específico citando nominalmente InSAR,|
|    |                       | no programa PROARTE."       | OAEs. Sem dispositivo nomi- | HIPÓTESE DE APLICA-| deve ser classificado como hipótese de |
|    |                       |                             | nal, InSAR é complementar.  | ÇÃO COMPLEMENTAR.   | aplicação complementar/estudo técnico.|
+----+-----------------------+-----------------------------+-----------------------------+--------------------+---------------------------------------+
| S3 | Contratos Públicos    | "Existem contratos no PNCP  | Os contratos mapeados no    | FATO CONFIRMADO    | Editais públicos contratam engenharia |
|    | no PNCP de InSAR      | para aquisição de software  | PNCP contratam laudos de    | (com precisão de   | consultiva com ART, e não assinaturas |
|    |                       | SaaS de monitoramento."     | engenharia com ART, não     | escopo).           | de software self-service na nuvem.    |
|    |                       |                             | software puro isolado.      |                    |                                       |
+----+-----------------------+-----------------------------+-----------------------------+--------------------+---------------------------------------+
| S4 | Disposição a Pagar    | Prefeituras médias eram     | Prefeituras médias acionam  | HIPÓTESE           | Não há evidência empírica de municí-  |
|    | de Prefeituras Médias | apontadas como potenciais   | CPRM e IPT gratuitamente;   | REFUTADA.          | pios médios mantendo assinaturas ativas|
|    |                       | compradoras de SaaS.        | não têm verba nem técnicos. |                    | de monitoramento de radar na web.     |
+----+-----------------------+-----------------------------+-----------------------------+--------------------+---------------------------------------+
| S5 | Preços de             | Valores de R$ 300k a R$ 1M  | Contratos privados de gran- | INFORMAÇÃO NÃO     | Valores exatos são protegidos por     |
|    | Concorrentes Privados | e US$ 30k a US$ 100k apre-  | des players são confidencia-| VERIFICADA / ESTI- | sigilo comercial (NDAs); devem ser    |
|    | (TRE, SkyGeo, Fugro)  | sentados como definitivos.  | is; ordens de grandeza são  | MATIVA INFORMAL.   | tratados como ordens de grandeza.     |
|    |                       |                             | empíricas e aproximadas.    |                    |                                       |
+----+-----------------------+-----------------------------+-----------------------------+--------------------+---------------------------------------+
| S6 | Telemetria de Solo    | Sugeria-se integração com   | Os dados em tempo real da   | FATO CONFIRMADO    | Apenas relatórios consolidados e RINEX|
|    | em Maceió/AL          | rede de sensores DGPS locais| rede local pertencem à De-  | (com limites de    | do IBGE são públicos; validação deve  |
|    |                       | da Defesa Civil de Maceió.  | fesa Civil/Braskem e não têm| acesso explícitos).| ser estritamente retrospectiva.       |
|    |                       |                             | API pública aberta.         |                    |                                       |
+----+-----------------------+-----------------------------+-----------------------------+--------------------+---------------------------------------+
| S7 | Cota e SLA da         | Apresentava-se o HyP3 como  | HyP3 Basic oferece 8.000    | FATO CONFIRMADO    | A dependência de serviços externos da |
|    | API NASA/ASF HyP3     | infraestrutura definitiva   | créditos/mês, mas apaga ar- | (com limitações de | NASA não possui SLA e exige estratégia|
|    |                       | gratuita para o Radar++.    | quivos em 14 dias e não tem | arquitetura).      | de storage próprio e transição futura.|
|    |                       |                             | SLA nem responsabilidade.   |                    |                                       |
+----+-----------------------+-----------------------------+-----------------------------+--------------------+---------------------------------------+
| S8 | Coerência na Banda C  | Assumia-se aplicabilidade   | Banda C (5.6 cm) sofre de-  | HIPÓTESE CRÍTICA   | Em encostas vegetadas sem rocha nua   |
|    | em Encostas Tropicais | universal em taludes e en-  | correlação severa em vege-  | (ALTO RISCO        | ou alvos artificiais, o sinal perde   |
|    |                       | costas de rodovias do BR.   | tação tropical rápida.      | TÉCNICO).          | coerência de fase em poucos ciclos.   |
+----+-----------------------+-----------------------------+-----------------------------+--------------------+---------------------------------------+
```

---

## 3. Síntese das Correções Materiais por Documento

### 3.1. Ajustes nos Documentos `D1-01` e `D1-02`
* **Correção Regulatória:** A menção à obrigatoriedade regulatória de monitoramento de taludes pela ANTT permanece como fato confirmado (exigência do SGTEC em contratos de concessão rodoviária), mas **fica explicitamente saneado que a ANTT não obriga o uso de InSAR**. A escolha da metodologia é discricionária da concessionária, que tradicionalmente emprega vistorias visuais e instrumentação física.
* **Correção sobre o PROARTE do DNIT:** A Instrução Normativa nº 2/2026 reestruturou o programa PROARTE com orçamentação paramétrica de OAEs. Na ausência de dispositivo normativo que cite nominalmente o InSAR no texto legal, a tecnologia não pode ser apresentada como formalmente elegível ou obrigatória, devendo ser categorizada estritamente como hipótese de aplicação complementar sob avaliação técnica.

### 3.2. Ajustes no Documento `D1-03` (Matriz Competitiva)
* **Preços de Mercado:** As faixas de preços mencionadas para TRE Altamira, SkyGeo e Fugro foram reclassificadas de "preços verificáveis" para **estimativas informais de mercado baseadas em fontes não auditadas e relatos de profissionais**, mantendo a proibição estrita de inventar tabelas oficiais de preços.

### 3.3. Ajustes no Documento `D1-05` (Piloto Maceió)
* **Acesso a Dados:** Ficou definitivamente documentado que a validação de Maceió é estritamente **retrospectiva** (comparação do processamento Radar++ com os laudos técnicos públicos do SGB/CPRM e dados geodésicos RBMC do IBGE). Foi excluída qualquer suposição de integração com telemetria automatizada em tempo real dos sensores de subsolo ou superfície mantidos pela Braskem.

---

## 4. Conclusão da Auditoria

Este saneamento não invalida os achados conceituais da Investigação D1, mas eleva o rigor científico e a maturidade de negócios do projeto, garantindo que o Radar++ planeje seu modelo comercial e arquitetura com base em fatos verificáveis e limites técnicos explícitos.
