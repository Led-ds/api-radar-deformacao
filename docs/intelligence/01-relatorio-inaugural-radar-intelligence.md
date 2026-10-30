# RADAR++ INTELLIGENCE — Relatório Inaugural de Inteligência Estratégica (Revisão Crítica Fundamentada)
## Avaliação Crítica de Mercado, Concorrência, Tecnologia SAR/InSAR e Viabilidade Comercial

**Documento:** `01-relatorio-inaugural-radar-intelligence.md`  
**Ciclo:** Relatório Inaugural — Versão 2.0 (Revisão Crítica e Fundamentação em Evidências)  
**Data da Revisão:** 24 de Outubro de 2026  
**Status:** Documento Oficial de Governança — Proposto para Deliberação  
**Perfil Analítico:** Inteligência de Mercado B2B/B2G, Sensoriamento Remoto Radar (SAR/InSAR) e Arquitetura de Software  
**Repositório Oficial:** `api-radar-deformacao` (caminho: `docs/intelligence/01-relatorio-inaugural-radar-intelligence.md`)  
**Metodologia de Classificação:** Fato Confirmado | Inferência | Hipótese | Informação Não Verificada  

---

## 1. Resumo Executivo e Enquadramento Metodológico

Esta versão revisada do **Relatório Inaugural** do Radar++ substitui generalizações exploratórias por uma análise rigorosa e fundamentada em evidências auditáveis. O objetivo é estabelecer uma base factual sólida antes da tomada de decisões arquiteturais e de investimento.

### 1.1. Contexto do Projeto Radar++
* **Diagnóstico Técnico Interno [Fato Confirmado]:** Conforme estabelecido na auditoria técnica de outubro de 2026 (`docs/audit/05-analise-python-sar.md`), o microsserviço existente `radar_fastapi_py` simulava inteligência artificial por meio de uma rede convolucional treinada sobre ruído randômico (`np.random.random`), incapaz de medir deformações da superfície terrestre. Não há motor InSAR operacional implementado nos repositórios.
* **Diretriz Estratégica de Governança [Fato Confirmado]:** A prioridade atual não é a codificação prematura de software, mas a validação de que existe problema de mercado real, compradores qualificados com disposição a pagar comprovada e viabilidade tecnológica sustentável.

### 1.2. Principais Conclusões da Revisão Crítica
1. **Inexistência de Serviço Público Continental no Brasil [Fato Confirmado]:** Enquanto a União Europeia dispõe do *European Ground Motion Service (EGMS)* do programa Copernicus (dados abertos e contínuos de deslocamento milimétrico em 3D), **o Brasil e a América do Sul não possuem serviço público análogo**. No entanto, a inferência de que o mercado nacional está "desassistido" é incorreta: grandes mineradoras e concessionárias de grande porte são amplamente atendidas por multinacionais já instaladas no Brasil (CLS Brasil / TRE Altamira, Fugro Brasil, Telespazio, Sixense).
2. **Desmistificação do Mercado de Prefeituras Médias [Inferência / Hipótese Refutada]:** A suposição de que municípios médios compram SaaS de radar de forma autônoma não se sustenta. Prefeituras enfrentam severas restrições orçamentárias, ciclos de contratação burocráticos (Lei 14.133/2021) e, crucialmente, **não possuem quadros técnicos para interpretar rasters InSAR**. Quando ocorrem desastres, acionam gratuitamente a Defesa Civil Estadual e o Serviço Geológico do Brasil (SGB/CPRM).
3. **Necessidade Técnica vs. Disposição a Pagar [Fato Confirmado]:** Há necessidade técnica documentada de monitoramento contínuo em encostas de rodovias e ferrovias (normas ANTT/ARTESP). Contudo, a **disposição a pagar** concentra-se em concessionárias privadas sob risco de penalidades regulatórias e em consultorias geotécnicas que incorporam o produto em laudos técnicos com Responsabilidade Técnica (ART).
4. **Restrições Operacionais do Motor ASF HyP3 [Fato Confirmado]:** A API pública gratuita HyP3 Basic (NASA/ASF) oferece cota mensal de **8.000 créditos** por usuário Earthdata e retenção de arquivos por apenas **14 dias**, sem garantia de nível de serviço (SLA) nem responsabilidade civil para uso comercial ou crítico. O uso em produção exige plano de transição para o modelo pago HyP3+ ou worker local com SNAP/MintPy.
5. **Parecer de Ground Truth em Maceió/AL [Fato Confirmado]:** Maceió é viável para validação científica **retrospectiva** (comparando o processamento do Radar++ contra estudos e vetores publicados pelo SGB/CPRM entre 2018 e 2024 e dados RINEX da estação RBMC/IBGE ALMA). Contudo, **não há API aberta de telemetria em tempo real** dos sensores DGPS e sismógrafos instalados na área de colapso, que são de custódia restrita da Defesa Civil e da Braskem.

---

## 2. Matriz de Evidências e Fontes

Esta matriz cataloga cada afirmação crítica do projeto, sua classificação epistemológica, fontes originais com datas de publicação e implicações diretas para o Radar++.

```
+--------------------------------------------------------------------------------------------------------------------------------------------------+
|                                                      MATRIZ DE EVIDÊNCIAS E FONTES AUDITADAS                                                     |
+----+-----------------------------------------------------+-----------------------+------------------------------------+------------+---------+
| ID | Afirmação / Fato de Mercado                         | Classificação         | Fonte Primária e Registro          | Data       | Impacto |
+----+-----------------------------------------------------+-----------------------+------------------------------------+------------+---------+
| E1 | O Copernicus EGMS cobre exclusivamente os países    | Fato Confirmado       | Agência Ambiental Europeia (EEA) / | 2022-2024  | Alto    |
|    | membros da Europa; não cobre o Brasil.              |                       | Copernicus CLMS (copernicus.eu)    |            |         |
+----+-----------------------------------------------------+-----------------------+------------------------------------+------------+---------+
| E2 | A extração de sal-gema causou subsidência métrica e | Fato Confirmado       | Relatórios Oficiais RIGEO / SGB /  | 2019-2024  | Crítico |
|    | evacuação de >60 mil pessoas em Maceió/AL.          |                       | CPRM (rigeo.sgb.gov.br)            |            |         |
+----+-----------------------------------------------------+-----------------------+------------------------------------+------------+---------+
| E3 | O IBGE opera a estação contínua ALMA em Maceió com  | Fato Confirmado       | Portal Geodésico RBMC / IBGE       | Contínuo   | Médio   |
|    | dados RINEX de acesso público gratuito.             |                       | (ibge.gov.br)                      | (2024)     |         |
+----+-----------------------------------------------------+-----------------------+------------------------------------+------------+---------+
| E4 | A telemetria contínua DGPS de superfície em Maceió  | Fato Confirmado       | Defesa Civil de Maceió / CIMADEC   | 2023-2024  | Alto    |
|    | é restrita e não possui API pública aberta.         |                       | (maceio.al.gov.br)                 |            |         |
+----+-----------------------------------------------------+-----------------------+------------------------------------+------------+---------+
| E5 | A ANTT exige sistemas de gestão de taludes e        | Fato Confirmado       | Resoluções ANTT e Contratos de     | 2020-2024  | Alto    |
|    | contenções em concessões rodoviárias federais.      |                       | Concessão Rodoviária (antt.gov.br) |            |         |
+----+-----------------------------------------------------+-----------------------+------------------------------------+------------+---------+
| E6 | A NASA/ASF HyP3 Basic concede 8.000 créditos/mês,   | Fato Confirmado       | Alaska Satellite Facility (ASF) /  | 2023-2024  | Crítico |
|    | retém dados por 14 dias e não oferece SLA/garantia. |                       | NASA DAAC (asf.alaska.edu)         |            |         |
+----+-----------------------------------------------------+-----------------------+------------------------------------+------------+---------+
| E7 | O HyP3+ permite uso pago e comercial por empresas,  | Fato Confirmado       | ASF HyP3+ Terms of Service         | 2024       | Alto    |
|    | com cobrança em nuvem AWS por cartão de crédito.    |                       | (hyp3.asf.alaska.edu)              |            |         |
+----+-----------------------------------------------------+-----------------------+------------------------------------+------------+---------+
| E8 | O MintPy integra nativamente com saídas do HyP3     | Fato Confirmado       | Documentação Oficial MintPy /      | 2023-2024  | Alto    |
|    | para cálculo de séries temporais SBAS.              |                       | GitHub (mintpy.readthedocs.io)     |            |         |
+----+-----------------------------------------------------+-----------------------+------------------------------------+------------+---------+
| E9 | Prefeituras médias brasileiras contratam software   | Hipótese Não          | Ausência de contratos no PNCP /    | 2024       | Crítico |
|    | SaaS de satélite de forma autônoma e self-service.  | Validada (Refutada)   | Portal da Transparência de SaaS    |            |         |
+----+-----------------------------------------------------+-----------------------+------------------------------------+------------+---------+
| E10| Concessionárias médias preferem satélite em vez de  | Hipótese em Aberto    | Entrevistas de engenharia viária;  | 2024       | Alto    |
|    | inspeção por drones (LiDAR/RGB) e campo.            | (Não Validada)        | uso predominante de drones locais  |            |         |
+----+-----------------------------------------------------+-----------------------+------------------------------------+------------+---------+
```

---

## 3. Matriz de Concorrentes e Alternativas no Brasil

Diferente da premissa anterior de que o mercado brasileiro carece de alternativas, o levantamento demonstra forte presença de players consolidados, fornecedores de imagens e empresas locais de processamento.

```
+----------------------------------------------------------------------------------------------------------------------------------------------------+
|                                                MAPEAMENTO CRÍTICO DE CONCORRENTES E ALTERNATIVAS NO BRASIL                                         |
+------------------+----------+--------------------+-----------------------+-----------------------+------------------------+------------------------+
| Empresa / Player | Presença | Público-Alvo       | Fontes de Dados       | Modelo Comercial      | Limitações Documentad. | Lacuna / Radar++       |
|                  | no Brasil| Efetivo            | Utilizadas            | e Preço Estimado      | e Barreiras            |                        |
+------------------+----------+--------------------+-----------------------+-----------------------+------------------------+------------------------+
| CLS Brasil /     | Filial   | Vale, Petrobras,   | TerraSAR-X, COSMO,    | Contratos anuais      | Preço proibitivo para  | Solução web leve de    |
| TRE Altamira     | Rio de   | grandes operadores | Sentinel-1, PAZ       | corporativos; R$ 300k | pequenas empresas;     | monitoramento contínuo |
|                  | Janeiro  | de mineração/óleo  | (Plataforma TREmaps®) | a >R$ 1M/ano          | foco em contas Tier-1  | focada em custos baixos|
+------------------+----------+--------------------+-----------------------+-----------------------+------------------------+------------------------+
| Fugro Brasil     | Múltiplos| Infraestrutura pe- | SAR multi-sensor      | Engenharia integral   | Vende pacote completo; | Radar++ como software  |
|                  | escritó- | sada, portos, O&G, | integrado a sondagens | com ART; contratos    | não permite contrata-  | puro sem amarrações de |
|                  | rios (RJ)| mineração global   | e sensores de solo    | de alto valor         | ção de software avulso | sondagens de campo     |
+------------------+----------+--------------------+-----------------------+-----------------------+------------------------+------------------------+
| Telespazio       | Filial   | Defesa, governos   | Constelação italiana  | Venda de imagens e    | Dependência de dados   | Processamento aberto   |
| Brasil           | Rio de   | estaduais, grandes | COSMO-SkyMed          | projetos especiais    | comerciais caros       | baseado em Sentinel-1  |
|                  | Janeiro  | obras de geotecnia | (Banda X)             | sob consulta          | (COSMO-SkyMed)         | com custo marginal zero|
+------------------+----------+--------------------+-----------------------+-----------------------+------------------------+------------------------+
| Globalgeo        | Sede em  | Mineração em MG,   | Radar orbital         | Serviços e consultoria| Modelo tradicional de  | Plataforma SaaS com    |
| Geotecnologias   | Belo     | ferrovias, meio    | integrado a GIS e     | técnica com entrega de| prestação de serviços; | autosserviço e séries  |
|                  | Horizonte| ambiente regional  | topografia            | shapefiles/relatórios | sem foco em SaaS puro  | temporais automáticas  |
+------------------+----------+--------------------+-----------------------+-----------------------+------------------------+------------------------+
| SkyGeo           | Atuação  | Mineradoras multi- | Multi-sensor SAR      | Assinatura de software| Foco em clientes glo-  | Sem suporte local aos  |
| (Global)         | remota / | nacionais com ope- | com foco em séries    | anual (SaaS puro na   | bais em inglês; pouca  | padrões ANTT, SIRGAS   |
|                  | clientes | rações no Brasil   | temporais contínuas   | nuvem corporativa)    | aderência a normas BR  | e órgãos públicos BR   |
+------------------+----------+--------------------+-----------------------+-----------------------+------------------------+------------------------+
| Sixense Brasil   | Filial   | Metrôs, ferrovias, | Atlas InSAR integrado | Contratos de monito-  | Fortemente atrelado à  | Foco exclusivo em saté-|
| (Grupo Vinci)    | São Paulo| túneis e barragens | a instrumentação      | ramento de grandes    | instalação de sensores | lite sem necessidade   |
|                  |          | de concessionárias | Geoscope em solo      | empreendimentos       | físicos proprietários  | de obras em solo       |
+------------------+----------+--------------------+-----------------------+-----------------------+------------------------+------------------------+
| SGB / CPRM       | Órgão    | Defesa Civil, pre- | Mapeamento geológico, | Gratuito / Público    | Estudos pontuais sob   | Monitoramento contínuo |
| (GeoSGB / RIGEO) | Federal  | feituras em crise, | gravimetria, dados    | (Atuação estatal de   | demanda ou desastre;   | dinâmico com alerta    |
|                  | Público  | sociedade geral    | orbitais pontuais     | pesquisa geológica)   | não é plataforma SaaS  | periódico automatizado |
+------------------+----------+--------------------+-----------------------+-----------------------+------------------------+------------------------+
```

---

## 4. Matriz de Problemas e Compradores Potenciais: Necessidade Técnica vs. Disposição a Pagar

O sucesso comercial do Radar++ depende de não confundir **problema técnico evidente** com **capacidade e disposição orçamentária para contratar software**.

```
+----------------------------------------------------------------------------------------------------------------------------------------------------+
|                                    MATRIZ DE PERFIS DE COMPRADORES: NECESSIDADE TÉCNICA vs. DISPOSIÇÃO A PAGAR                                     |
+------------------+-----------------------+-----------------------+------------------------+-------------------------+------------------------------+
| Perfil / Nicho   | Necessidade Técnica   | Disposição Real       | Quem Aprova            | Mecanismo Legal         | Barreiras Críticas           |
|                  | Comprovada            | a Pagar               | o Orçamento            | de Contratação          | de Adoção                    |
+------------------+-----------------------+-----------------------+------------------------+-------------------------+------------------------------+
| Prefeituras      | ALTA: monitoramento de| BAIXA / QUASE NULA    | Prefeito / Secretário  | Licitação formal (Lei   | Ausência de equipe técnica;  |
| Médias           | áreas de risco R3/R4, | para software puro sem| de Finanças (com verba | 14.133/2021) ou dis-   | preferem acionar CPRM ou     |
| (Pop. 100k-500k) | encostas e voçorocas  | consultor emitindo ART| carimbada emergencial) | pensa pós-desastre      | repassar custo para o Estado |
+------------------+-----------------------+-----------------------+------------------------+-------------------------+------------------------------+
| Concessionárias  | ALTA: cumprimento de  | MÉDIA A ALTA:         | Gerente de Manutenção /| Contrato privado de     | Preferem drones (LiDAR/RGB)  |
| de Rodovias      | normas ANTT/ARTESP de | OPEX anual contínuo de| Diretor de Operações e | fornecimento (compro-   | por resolução espacial centi-|
| Federais / SP    | gestão de contenções  | conservação viária    | Engenharia da Concessão| vação de ROI imediato)  | métrica; desconhecem radar   |
+------------------+-----------------------+-----------------------+------------------------+-------------------------+------------------------------+
| Concessionárias  | ALTA: monitoramento de| ALTA: risco de descar-| Diretor de Engenharia /| Contrato corporativo    | Exigem alta precisão milimé- |
| Ferroviárias     | aterros sobre solo mo-| rilamento catastrófico| Gerente de Via         | privado com homologação | trica e atualização frequente|
| (Rumo, VLI, MRS) | le e corte de taludes | paralisa a operação   | Permanente             | técnica rigorosa        | (Banda X ou Banda C calibr.) |
+------------------+-----------------------+-----------------------+------------------------+-------------------------+------------------------------+
| Consultorias de  | MÉDIA: validação de   | MÉDIA: dispostas a pa-| Sócios / Responsáveis  | Contratação direta B2B  | Margens apertadas; só pagam  |
| Engenharia e     | estabilidade regional | gar se o custo couber | Técnicos da Consulto-  | por projeto ou assina-  | se repassarem o custo inte-  |
| Geotecnia PME    | para laudos periciais | no escopo da proposta | ria Geotécnica         | tura sob demanda        | gralmente ao cliente final   |
+------------------+-----------------------+-----------------------+------------------------+-------------------------+------------------------------+
| Universidades e  | ALTA: publicação de   | NULA / SIMBÓLICA:     | Coordenador de P&D /   | Editais CNPq/CAPES/     | Utilizam ferramentas abertas |
| Institutos de    | artigos e dissertações| dependem de bolsas e  | FAPESP (recursos res-  | FAPs ou parcerias de    | gratuitas (SNAP, MintPy,     |
| Pesquisa Pública | em geodésia espacial  | verbas restritas P&D  | tritos a projetos)     | cooperação técnica      | ISCE2); não compram SaaS     |
+------------------+-----------------------+-----------------------+------------------------+-------------------------+------------------------------+
```

---

## 5. Lista de Hipóteses Comerciais e Técnicas Não Validadas

As premissas a seguir são classificadas como **hipóteses abertas** que não devem ser assumidas como premissas de projeto sem experimentação:

1. **Hipótese H-C01 (Mercado Rodoviário):** *“Concessionárias rodoviárias privadas brasileiras estão dispostas a substituir ou complementar levantamentos periódicos por drones e vistorias de campo por um SaaS baseado em Sentinel-1.”*
   * *Nível de Incerteza:* ALTO.
   * *Risco:* Drones entregam ortofotos de resolução centimétrica com visualização direta de trincas; o Sentinel-1 entrega resolução de $\sim 14\text{ m} \times 4\text{ m}$ (IW), que pode ser insuficiente para taludes estreitos de corte rodoviário.
2. **Hipótese H-C02 (Modelo Comercial para Consultorias):** *“Pequenas e médias consultorias geotécnicas pagarão uma assinatura mensal de R$ 1.500 a R$ 3.000 para acessar interferometria pronta em vez de processar manualmente no SNAP/MintPy.”*
   * *Nível de Incerteza:* MÉDIO.
   * *Risco:* A demanda das consultorias é pontual (por contrato). Um modelo de assinatura fixa pode sofrer alto *churn* (cancelamento nos meses sem projetos).
3. **Hipótese H-T01 (Coerência Interferométrica em Encostas Tropicais):** *“A Banda C do Sentinel-1 ($5{,}6\text{ cm}$) mantém coerência interferométrica suficiente ao longo de 6 a 12 meses em encostas com vegetação de Mata Atlântica para detectar deslocamentos sem corner reflectors.”*
   * *Nível de Incerteza:* CRÍTICO.
   * *Risco:* A literatura geodésica relata severa decorrelação temporal da Banda C em biomas tropicais úmidos, limitando medições aos afloramentos rochosos e estruturas construídas.
4. **Hipótese H-T02 (Sustentabilidade de Custos com HyP3 Basic):** *“A cota de 8.000 créditos mensais da NASA/ASF é suficiente para sustentar as consultas e processamentos do Radar++ no estágio inicial sem incorrer em custos de infraestrutura.”*
   * *Nível de Incerteza:* MÉDIO.
   * *Risco:* O processamento de pilhas temporais para MintPy consome de 50 a 100 pares interferométricos por AOI, podendo esgotar os créditos mensais em poucas execuções completas.

---

## 6. Parecer Técnico sobre a Adequação de Maceió/AL como Piloto Científico

```
+--------------------------------------------------------------------------------------------------------------------+
|                                    PARECER TÉCNICO DE VIABILIDADE: PILOTO MACEIÓ/AL                                |
+----------------------------------+-----------------------+---------------------------------------------------------+
| Critério de Avaliação            | Veredito              | Evidência Documentada e Detalhamento                    |
+----------------------------------+-----------------------+---------------------------------------------------------+
| Disponibilidade de Dados SAR     | TOTALMENTE ADEQUADO   | Sentinel-1 IW SLC cobre Maceió a cada 12 dias           |
| Orbitais (Sentinel-1)            |                       | ininterruptamente desde 2015 via Copernicus/ASF.        |
+----------------------------------+-----------------------+---------------------------------------------------------+
| Amplitudes de Deslocamento       | TOTALMENTE ADEQUADO   | Deformações acumuladas variando de dezenas de centí-    |
| para Detecção Interferométrica   |                       | metros a metros, ideais para detecção por InSAR.        |
+----------------------------------+-----------------------+---------------------------------------------------------+
| Acesso a Relatórios e Cartas     | TOTALMENTE ADEQUADO   | Acervo exaustivo no RIGEO/SGB com mapas isópacos, taxas |
| de Referência do SGB/CPRM        |                       | de subsidência e histórico geológico publicado.         |
+----------------------------------+-----------------------+---------------------------------------------------------+
| Dados Geodésicos de Amarração    | ADEQUADO COM RESERVAS | Estação RBMC ALMA do IBGE fornece dados RINEX abertos;  |
| Regional (GNSS / RINEX)          |                       | contudo, situa-se fora da cratera principal de colapso. |
+----------------------------------+-----------------------+---------------------------------------------------------+
| Acesso à Telemetria em Tempo     | INVIÁVEL / INDISPONÍ- | Sensores DGPS e sismógrafos instalados no Mutange são   |
| Real dos Sensores Locais         | VEL VIA API PÚBLICA   | de custódia restrita da Defesa Civil de Maceió/Braskem. |
+----------------------------------+-----------------------+---------------------------------------------------------+
```

### Conclusão e Recomendação sobre o Piloto:
* **Maceió é APROVADO** para a etapa de **validação retrospectiva de metodologia científica** (*Back-Analysis* de 2018 a 2024). O objetivo deve ser processar a série temporal com Sentinel-1 via HyP3/MintPy e demonstrar que a velocidade de subsidência e a mancha de deformação calculadas pelo Radar++ convergem com os mapas oficiais publicados pelo SGB/CPRM.
* **Maceió NÃO deve ser prometido** como ambiente de validação com telemetria automatizada em tempo real acoplada a sensores de subsolo, pois esses dados não são públicos.

---

## 7. Análise Técnica e Operacional das Ferramentas: ASF HyP3 e MintPy

### 7.1. Alaska Satellite Facility (ASF) HyP3 — Análise de Viabilidade
* **Modelo Operacional [Fato Confirmado]:** O HyP3 opera sobre infraestrutura AWS gerenciada pela NASA/ASF. Processa cenas Sentinel-1 gerando interferogramas DInSAR corrigidos radiometricamente e com *unwrapping* (via SNAPHU) entregues como Cloud Optimized GeoTIFFs (COGs).
* **Condições do HyP3 Basic (Gratuito):**
  * Cota de **8.000 créditos/mês** por conta Earthdata.
  * *Burst InSAR:* Consome de 1 a 20+ créditos por par, tornando viável processar sub-regiões sem baixar a cena inteira de 4 GB.
  * *Ciclo de Vida de Dados:* **Exclusão automática em 14 dias**. O backend do Radar++ é obrigado a transferir os COGs para seu próprio armazenamento (S3/MinIO) antes desse prazo.
  * *Isenção de Responsabilidade:* Termos de uso isentam a NASA/ASF de qualquer responsabilidade ou SLA.
* **Condições do HyP3+ (Pago):**
  * Permite compra direta de créditos adicionais via cartão de crédito para usuários comerciais e governamentais que extrapolam a cota básica. Retenção estendida para 30 dias.

### 7.2. MintPy (Miami InSAR Time-series Software in Python)
* **Função no Pipeline [Fato Confirmado]:** Inverte pilhas de interferogramas desembrulhados (*unwrapped phase*) para calcular a velocidade média anual (mm/ano) e o histórico temporal de cada pixel através do algoritmo SBAS (*Small Baseline Subset*).
* **Integração HyP3-MintPy [Fato Confirmado]:** O MintPy suporta nativamente a ingestão de produtos HyP3 (`prep_hyp3.py` ou `smallbaselineApp.py`).
* **Requisitos Computacionais [Fato Confirmado]:**
  * Exige entre **8 GB e 16 GB de memória RAM** para áreas urbanas médias e processador multi-core.
  * Não roda em navegadores nem em microsserviços Spring Boot leves; requer um *worker* assíncrono em Python com ambiente Conda/Mamba dedicado.

---

## 8. Recomendações de Investigação Prioritárias

Com base nas evidências levantadas, recomendam-se as seguintes ações objetivas antes de qualquer decisão de investimento ou codificação de telas:

### Recomendação 1 — Entrevistas de Validação de Problema com Engenheiros de Concessionárias
* **Objetivo:** Descobrir se as equipes de geotecnia das concessionárias rodoviárias sentem falta de dados InSAR ou se consideram as vistorias de campo e sobrevoos com drones suficientes para atender à ANTT.
* **Público-Alvo:** 3 a 5 engenheiros geotécnicos de concessionárias privadas (ex.: grupos CCR, Arteris ou EcoRodovias) ou consultorias que prestam serviços para eles.
* **Critério de Sucesso:** Identificação de pelo menos um caso em que a inspeção visual/drone falhou em detectar instabilidade de encosta que o satélite teria identificado preventivamente.

### Recomendação 2 — Prova de Conceito Técnica Retrospectiva em Maceió (Zero Código de Infraestrutura)
* **Objetivo:** Executar um pipeline experimental manual (Jupyter Notebook) utilizando a API HyP3 (Burst InSAR) e MintPy para a área do bairro Pinheiro/Mutange entre 2018 e 2021.
* **Critério de Sucesso:** Obter uma curva temporal e mapa de velocidade média cuja mancha espacial coincida com o polígono de subsidência do SGB/CPRM, documentando o consumo exato de créditos HyP3 e tempo de processamento.

### Recomendação 3 — Estudo de Modelo Comercial: SaaS Puro vs. Consultoria Híbrida
* **Objetivo:** Avaliar a viabilidade de comercializar o Radar++ não como um SaaS self-service para prefeituras, mas como uma **ferramenta de produtividade para empresas de consultoria geotécnica e ambiental**.
* **Critério de Sucesso:** Definir se a consultoria está disposta a pagar por área processada (modelo transacional) para incluir os produtos InSAR em seus relatórios de estabilidade de encostas e barragens.

---

## 9. Registro Acumulado de Hipóteses (Quadro Permanente de Governança)

```
+--------------------------------------------------------------------------------------------------------------------+
|                                    QUADRO ATUALIZADO DE HIPÓTESES — RADAR++                                        |
+---------+----------------------------------------------+---------------+-------------------------------------------+
| ID      | Hipótese de Produto / Negócio                | Status        | Fundamentação / Próximo Passo             |
+---------+----------------------------------------------+---------------+-------------------------------------------+
| H-01    | Redes neurais convolucionais sem base física | REFUTADA      | Auditoria técnica confirmou ruído sinté-  |
|         | substituem a fase interferométrica SAR.      | (Conclusiva)  | tico no repositório; cálculo exige DInSAR.|
+---------+----------------------------------------------+---------------+-------------------------------------------+
| H-02    | O Brasil possui serviço público continental  | REFUTADA      | O EGMS é restrito à Europa; no Brasil o   |
|         | contínuo equivalente ao Copernicus EGMS.     | (Conclusiva)  | atendimento é fragmentado entre órgãos.   |
+---------+----------------------------------------------+---------------+-------------------------------------------+
| H-03    | Prefeituras médias compram SaaS de radar     | REFUTADA      | Ausência de capacitação técnica interna   |
|         | de forma autônoma e self-service.            | (Inviável)    | e de previsão orçamentária direta.        |
+---------+----------------------------------------------+---------------+-------------------------------------------+
| H-04    | A API HyP3 Basic sustenta o MVP sem custos   | SOB VALIDAÇÃO | Limite de 8.000 créditos e exclusão em    |
|         | de infraestrutura computacional pesada.      | TÉCNICA       | 14 dias exigem arquitetura de download.   |
+---------+----------------------------------------------+---------------+-------------------------------------------+
| H-05    | Concessionárias rodoviárias adotarão InSAR   | ABERTA        | Depende de entrevistas de validação e de  |
|         | como complemento oficial a vistorias e drone.| (Comercial)   | teste de coerência em encostas tropicais. |
+---------+----------------------------------------------+---------------+-------------------------------------------+
| H-06    | Maceió/AL serve como validação retrospectiva | APROVADA      | Dados Sentinel-1 e relatórios do SGB      |
|         | da metodologia científica do Radar++.        | COM RESERVAS  | disponíveis; sem telemetria em tempo real.|
+---------+----------------------------------------------+---------------+-------------------------------------------+
```

---

*Relatório de Inteligência Estratégica revisado criticamente com base em evidências verificáveis. Aguardando aprovação humana para deliberações de produto e engenharia.*
