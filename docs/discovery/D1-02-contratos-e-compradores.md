# Radar++ Discovery — D1-02: Mapeamento de Contratos Públicos, Compradores e Modelos de Contratação
## Levantamento no PNCP, Compras.gov.br, Perfis de Contratantes e Entregáveis Exigidos

**Documento:** `D1-02-contratos-e-compradores.md`  
**Ciclo:** Investigação D1 — Mercado e Viabilidade  
**Data:** 24 de Outubro de 2026  
**Status:** Oficial — Entregue para Governança  
**Responsável:** Equipe de Descoberta de Produto, Inteligência de Mercado e Sensoriamento Remoto  
**Repositório Oficial:** `api-radar-deformacao` (caminho: `docs/discovery/D1-02-contratos-e-compradores.md`)  

---

## 1. Visão Geral do Cenário de Compras Públicas e Reguladas no Brasil

Para compreender a viabilidade comercial do Radar++ no setor público (B2G) e em concessionárias de serviços regulados, foi realizado um mapeamento de editais, termos de referência e contratos registrados no **Portal Nacional de Contratações Públicas (PNCP)**, no portal **Compras.gov.br** e em diários oficiais.

### 1.1. Principal Fato Confirmado sobre Contratações B2G
> **O poder público brasileiro não contrata software SaaS de monitoramento InSAR isolado na modalidade self-service.**
> Todas as contratações públicas mapeadas no PNCP e portais correlatos têm como objeto a **contratação de serviços de engenharia consultiva, laudos periciais ou implantação de redes integradas de monitoramento**, com exigência imperativa de emissão de **Anotação de Responsabilidade Técnica (ART)** junto ao CREA, liderada por engenheiro civil ou geólogo habilitado.

---

## 2. Tipologia dos Órgãos Compradores e Esferas Governamentais

```
+-----------------------------------------------------------------------------------------------------------------------+
|                                    MAPEAMENTO DE ÓRGÃOS COMPRADORES NO SETOR PÚBLICO                                  |
+-------------------+-----------------------+-----------------------------+---------------------------------------------+
| Esfera de Poder   | Órgãos Contratantes   | Modalidade de Compra Típica | Exemplo de Escopo / Objeto                  |
+-------------------+-----------------------+-----------------------------+---------------------------------------------+
| Federal           | DNIT                  | Concorrência Pública /      | Programa PROARTE (IN nº 2/2026): Gestão de  |
|                   |                       | Contratação Paramétrica     | OAEs (InSAR como tecnologia em avaliação)   |
+-------------------+-----------------------+-----------------------------+---------------------------------------------+
| Federal           | SGB / CPRM            | Pregão Eletrônico / RDC     | Estudos de cartografia geológica de risco e |
|                   |                       |                             | sondagens em municípios em calamidade       |
+-------------------+-----------------------+-----------------------------+---------------------------------------------+
| Federal           | ANTT                  | Regulação de Concessões     | Exigência de Sistemas de Gestão de Taludes  |
| (Regulatória)     |                       | (Auditoria de Contratos)    | em rodovias federais concedidas             |
+-------------------+-----------------------+-----------------------------+---------------------------------------------+
| Estadual          | DERs, Defesas Civis   | Pregão Eletrônico /         | Monitoramento de rodovias estaduais serranas|
|                   | Estaduais, Cias. Água | Contratos Plurianuais       | e adutoras de saneamento (ex.: Sabesp)      |
+-------------------+-----------------------+-----------------------------+---------------------------------------------+
| Municipal         | Secretarias de Obras, | Pregão Eletrônico /         | Laudos geotécnicos de estabilidade, cartas  |
| (Capitais / RMs)  | Habitação e Defesa C. | Dispensa Emergencial        | de aptidão à urbanização e vistorias        |
+-------------------+-----------------------+-----------------------------+---------------------------------------------+
```

### 2.1. Nível Federal: O Programa PROARTE do DNIT
* **Reformulação Normativa do PROARTE [Fato Confirmado]:** O **PROARTE (Programa de Manutenção e Reabilitação de Estruturas)** do DNIT foi reestruturado pela **Instrução Normativa nº 2/2026**, adotando diretrizes de orçamentação e contratação paramétrica para Obras de Arte Especiais (OAEs — pontes, viadutos e túneis) na malha rodoviária federal.
* **InSAR no PROARTE [Hipótese de Aplicação Complementar]:** Noticiários setoriais e debates técnicos apontam o monitoramento por satélite (InSAR) e IA como metodologias promissoras para triagem de recalques em OAEs. Contudo, **na ausência de dispositivo normativo específico da IN que cite nominalmente o InSAR como metodologia homologada ou obrigatória**, sua adoção deve ser tratada estritamente como **hipótese de aplicação complementar / tecnologia elegível em estudos e projetos paramétricos**, sem exclusividade ou garantia de contratação pública direta.
* **Valores e Escopo de Engenharia:** Contratos de lote rodoviário do PROARTE abrangem serviços contínuos e de escopo de engenharia orçados em **dezenas de milhões de reais** por lote regional plurianual, voltados a empresas de engenharia consultiva e execução física com ART.

### 2.2. Nível Estadual: Concessionárias e Departamentos de Estradas de Rodagem
* **São Paulo (ARTESP):** A regulação paulista exige das concessionárias rodoviárias planos preventivos de contenções e estabilidade de taludes com relatórios periódicos auditáveis.
* **Companhias de Saneamento (ex.: Sabesp, Sanepar, Copasa, Compesa):** Contratam empresas de engenharia diagnóstica para avaliar estabilidade de solos em perímetros de implantação de emissários, adutoras de grande porte e Estações de Tratamento de Esgoto (ETEs).

### 2.3. Nível Municipal: Grandes Capitais vs. Municípios Médios
* **Capitais Estruturadas (Rio de Janeiro/GeoRio, Salvador/CODESAL, Belo Horizonte/URBEL, São Paulo):**
  * Possuem fundações ou autarquias técnicas especializadas com quadro próprio de geólogos e engenheiros geotécnicos.
  * Contratam empresas privadas por meio de editais de engenharia para fornecimento de instrumentação física de encostas (pluviômetros, piezômetros, prismas e radares terrestres).
  * *Valores:* Contratos na faixa de **R$ 1.500.000 a R$ 8.000.000** por lote de encostas para operação e manutenção contínua.
* **Municípios Médios (100k a 500k habitantes):**
  * Não possuem contratos ativos de monitoramento contínuo por satélite.
  * Quando há eventos de subsidência ou trincas em massa, a prefeitura decreta situação de emergência e solicita auxílio técnico gratuito à **Defesa Civil Nacional (S2iD)** ou ao **SGB/CPRM**.
  * A contratação via Lei 14.133/2021 ocorre por dispensa de licitação (art. 75, inciso VIII — emergência/calamidade) para contratação emergencial de obras de contenção ou laudos periciais específicos.

---

## 3. Entregáveis Exigidos nos Contratos Públicos e Requisitos Técnicos

Para que uma solução seja aceita e faturada em contratos públicos e regulados no Brasil, os entregáveis devem obedecer a critérios padronizados:

```
+-----------------------------------------------------------------------------------------------------------------------+
|                                    EXIGÊNCIAS FORMAIS E ENTREGÁVEIS EM CONTRATOS PÚBLICOS                             |
+--------------------------+-----------------------+-----------------------------+--------------------------------------+
| Categoria de Requisito   | Especificação Padrão  | Justificativa Legal         | Impacto na Arquitetura do Radar++    |
+--------------------------+-----------------------+-----------------------------+--------------------------------------+
| Sistema Geodésico de     | SIRGAS 2000           | Decreto Federal nº          | Todo produto vetorial ou raster      |
| Referência Oficial       | (EPSG: 4674 / UTM)    | 5.334/2005 (IBGE)           | gerado deve ser projetado em SIRGAS. |
+--------------------------+-----------------------+-----------------------------+--------------------------------------+
| Responsabilidade Técnica | Emissão de ART / RRT  | Lei Federal nº 6.496/1977   | Software isolado não substitui o     |
| Profissional Habilitada  | no CREA / CAU         | (Responsabilidade Civil)    | engenheiro responsável pelo laudo.   |
+--------------------------+-----------------------+-----------------------------+--------------------------------------+
| Formato de Dados SIG     | Shapefile, GeoPackage,| Interoperabilidade com      | Exportação obrigatória de dados em   |
| e Georreferenciamento    | Cloud Opt. GeoTIFF    | INDE (Infraest. Nac. Dados) | formatos abertos OGC e COG.          |
+--------------------------+-----------------------+-----------------------------+--------------------------------------+
| Rastreabilidade e        | Metadados ISO 19115;  | Normas de Auditoria de      | Registro inalterável das cenas SAR de|
| Auditoria Metodológica   | Log de Processamento  | Tribunais de Contas (TCU)   | origem, órbitas e data de geração.   |
+--------------------------+-----------------------+-----------------------------+--------------------------------------+
```

---

## 4. Onde Está o Dinheiro: Rota Comercial B2B2G

A análise contratual demonstra que a tentativa de vender o Radar++ diretamente para prefeituras brasileiras via SaaS self-service enfrentaria as seguintes barreiras instransponíveis:
1. Prefeituras não compram software sem prévia licitação ou rubrica orçamentária específica.
2. Servidores municipais não sabem analisar deslocamentos milimétricos em mapas de radar.
3. Se um deslizamento ocorrer após a prefeitura contratar um software sem consultor responsável, o gestor público responde civil e penalmente.

### A Janela Comercial Efetiva: Parcerias B2B2G
* As grandes consultorias de engenharia (ex.: Arcadis, Pimenta de Ávila, Walm, Engeplus, Suporte Solos) já possuem os atestados de capacidade técnica, certidões negativas de débito e equipes para disputar as licitações de milhões de reais no DNIT, DERs e capitais.
* **Proposta de Valor do Radar++ no B2B2G:** Vender a plataforma como uma **ferramenta de produtividade analítica para essas consultorias**. A consultoria utiliza o Radar++ para gerar a análise histórica e a velocidade de deformação em horas (em vez de semanas), reduzindo seus custos operacionais internos e aumentando sua margem no contrato público já conquistado.

---

## 5. Resumo das Evidências Quantitativas

* **DNIT / PROARTE:** Reformulação paramétrica pela Instrução Normativa nº 2/2026; InSAR figura como hipótese de aplicação complementar sob avaliação técnica.
* **Faixa de Tíquetes em Licitações de Geotecnia/Mapeamento:**
  * Diagnóstico Geotécnico Municipal Pontual: R$ 80.000 a R$ 300.000.
  * Levantamentos Aerofotogramétricos e Cadastrais: R$ 500.000 a R$ 3.000.000.
  * Monitoramento de Encostas em Capitais (GeoRio/CODESAL): R$ 1,5M a R$ 8M por contrato plurianual.
* **Modelo Comercial Recomendado para o Radar++:**
  * Modelo transacional ou assinatura por volume de área monitorada (R$ / $km^2$ ou R$ / job) direcionado a consultorias e empresas de engenharia privada.
