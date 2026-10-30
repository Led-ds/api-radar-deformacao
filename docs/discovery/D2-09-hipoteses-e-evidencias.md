# Radar++ Discovery — D2-09: Quadro Consolidado de Hipóteses e Evidências em Infraestrutura Linear
## Matriz Epistemológica Aplicada a Rodovias e Ferrovias

**Documento:** `D2-09-hipoteses-e-evidencias.md`  
**Ciclo:** Investigação D2 — Infraestrutura Linear (Rodovias e Ferrovias)  
**Data:** 24 de Outubro de 2026  
**Status:** Oficial — Entregue para Governança  
**Responsável:** Equipe Multidisciplinar de Inteligência Estratégica, Geotecnia e Regulação  
**Repositório Oficial:** `api-radar-deformacao` (caminho: `docs/discovery/D2-09-hipoteses-e-evidencias.md`)  

---

## 1. Quadro Consolidado de Hipóteses e Evidências em Infraestrutura Linear

```
+----------------------------------------------------------------------------------------------------------------------------------------------------+
|                                      MATRIZ DE EVIDÊNCIAS E HIPÓTESES: INFRAESTRUTURA LINEAR (D2)                                                   |
+----+-----------------------------------------------------+-----------------------+------------------------------------+------------+---------------+
| ID | Afirmação / Hipótese Investigada                    | Classificação         | Fonte Primária e Registro          | Data       | Status / Risco|
+----+-----------------------------------------------------+-----------------------+------------------------------------+------------+---------------+
| D1 | A ANTT exige a implantação do SGTEC em contratos de | Fato Confirmado       | Contratos de Concessão Rodoviária  | 2020-2024  | Diretriz      |
|    | concessão rodoviária federal (NBR 11682 e DER/SP).  |                       | e PER da ANTT (antt.gov.br)        |            | Regulatória   |
+----+-----------------------------------------------------+-----------------------+------------------------------------+------------+---------------+
| D2 | A ANTT não exige nominalmente o uso de InSAR; a     | Fato Confirmado       | Manuais de Fiscalização e Resolu-  | 2024       | Crítico para  |
|    | metodologia de monitoramento de taludes é livre.    |                       | ções da ANTT (antt.gov.br)         |            | o Produto     |
+----+-----------------------------------------------------+-----------------------+------------------------------------+------------+---------------+
| D3 | A IN DNIT nº 2/2026 (PROARTE) não cita nominalmente | Fato (sem mandado) /  | Instrução Normativa DNIT nº 2/2026 | 2026       | Hipótese sob  |
|    | o InSAR; tecnologia é hipótese complementar.        | Hipótese Complementar | e portal oficial dnit.gov.br       |            | Avaliação     |
+----+-----------------------------------------------------+-----------------------+------------------------------------+------------+---------------+
| D4 | O Sentinel-1 rastreia tendências lentas (creep), mas| Fato Confirmado       | Literatura Geodésica e Geotécnica  | 2018-2024  | Limitação     |
|    | não oferece detecção antecipada garantida de ruptura| (Limitação Física)    | (Wasowski & Bovenga; Mansour)      |            | Operacional   |
+----+-----------------------------------------------------+-----------------------+------------------------------------+------------+---------------+
| D5 | Órbita única (LOS) é suficiente para triagem inicial| Fato Confirmado       | Manuais ASF/MintPy e Práticas      | 2020-2024  | Arquitetura   |
|    | bi-orbital é funcionalidade avançada, não obrigatória| (Diretriz Técnica)   | Operacionais InSAR                 |            | de Custos     |
+----+-----------------------------------------------------+-----------------------+------------------------------------+------------+---------------+
| D6 | Drones com câmeras ópticas e LiDAR são a tecnologia | Fato Confirmado       | Práticas operacionais documentadas | 2022-2024  | Concorrência  |
|    | preferida de concessionárias para inspeção visual.  |                       | nas concessionárias (ABCR)         |            | Substituta    |
+----+-----------------------------------------------------+-----------------------+------------------------------------+------------+---------------+
| D7 | A Banda C do Sentinel-1 sofre severa decorrelação   | Fato Confirmado       | Literatura Geodésica e Sensoria-   | 2018-2024  | Limitação     |
|    | temporal em encostas com vegetação de Mata Atlântica|                       | mento Remoto (Zebker; Hanssen)     |            | Geofísica     |
+----+-----------------------------------------------------+-----------------------+------------------------------------+------------+---------------+
| D8 | Superioridade comercial, ciclos de venda e maior dis| Hipótese Aberta       | Premissa de negócio a ser testada  | 2026       | Alto Risco    |
|    | posição a pagar de concessionárias frente a cidades.| (Comercial a Validar) | nas entrevistas D1-04 (B2B).       |            | Comercial     |
+----+-----------------------------------------------------+-----------------------+------------------------------------+------------+---------------+
| D9 | Consultorias geotécnicas viárias são o melhor canal | Inferência            | Consultorias emitem ART e têm con- | 2024       | Canal Estraté-|
|    | de distribuição do Radar++ para concessionárias.    | Fundamentada          | tratos vigentes com concessionárias|            | gico Principal|
+----+-----------------------------------------------------+-----------------------+------------------------------------+------------+---------------+
| D10| Maceió é piloto científico de validação retrospecti-| Diretriz Estratégica  | Parecer Estratégico D2-08/D2-10    | 2026       | Alinhamento   |
|    | va; infraestrutura linear é mercado comercial a test| Aprovada              | e Governança do Radar++            |            | de Escopo     |
+----+-----------------------------------------------------+-----------------------+------------------------------------+------------+---------------+
```

---

## 2. Condições para Validação ou Refutação das Hipóteses Abertas

1. **Validação da Hipótese D8 (Disposição a Pagar e Superioridade Comercial em Concessionárias):**
   * *Critério de Aceite:* Pelo menos duas das concessionárias ou consultorias entrevistadas no roteiro `D1-04` afirmarem que contratariam serviço InSAR como triagem preventiva em macroescala com orçamento próprio de conservação viária, considerando ciclo de vendas compatível com sustentabilidade da startup.
   * *Critério de Refutação:* Os entrevistados afirmarem que as inspeções visuais com drones e pluviômetros próprios atendem plenamente às exigências regulatórias e de seguradoras, ou que os ciclos de homologação corporativa e exigências de ART inviabilizam a contratação do software.
2. **Validação da Hipótese D3 (Aplicação Complementar no DNIT / PROARTE):**
   * *Critério de Aceite:* Confirmação por engenheiros viários ou editais públicos de que metodologias de sensoriamento remoto orbital são aceitas como documentação complementar de apoio à orçamentação paramétrica de OAEs.
   * *Critério de Refutação:* Órgãos federais e estaduais exigirem exclusivamente vistorias presenciais padronizadas em ficha cadastral física, rejeitando relatórios orbitais sem calibração in-situ prévia.
3. **Preservação do Alinhamento Metodológico (Hipótese D10):**
   * O pipeline computacional e a acurácia dos algoritmos serão comprovados primeiramente no **Piloto Científico de Maceió/AL** (subsidência urbana), cujos dados do SGB/CPRM servem de verdade de campo auditada, antes de qualquer mobilização comercial em escala para infraestrutura linear.
