# Radar++ Discovery — D1-01: Análise Aprofundada do Mercado de Subsidência Urbana no Brasil
## Investigação de Problemas Operacionais, Fenomenologia e Demanda Real

**Documento:** `D1-01-mercado-subsidencia-urbana.md`  
**Ciclo:** Investigação D1 — Mercado e Viabilidade  
**Data:** 24 de Outubro de 2026  
**Status:** Oficial — Entregue para Governança  
**Responsável:** Equipe de Descoberta de Produto, Inteligência de Mercado e Sensoriamento Remoto  
**Repositório Oficial:** `api-radar-deformacao` (caminho: `docs/discovery/D1-01-mercado-subsidencia-urbana.md`)  

---

## 1. Contexto e Fenomenologia da Subsidência Urbana no Brasil

A subsidência do solo em áreas urbanas brasileiras resulta primariamente de **atividades antrópicas desordenadas**, interagindo com contextos geológicos e hidrogeológicos suscetíveis. Diferente de países localizados em bordas de placas tectônicas com deformações sísmicas ativas contínuas, no Brasil as movimentações urbanas concentram-se em vetores de intervenção humana documentados:

```
+-----------------------------------------------------------------------------------------------------------------------+
|                                  VETORES CRÍTICOS DE SUBSIDÊNCIA URBANA NO BRASIL                                     |
+--------------------------+-----------------------+-----------------------------+--------------------------------------+
| Causa Primária           | Localidade Emblemática| Mecanismo Geofísico         | Evidência Documentada / Fonte        |
+--------------------------+-----------------------+-----------------------------+--------------------------------------+
| Mineração Subterrânea    | Maceió / AL           | Dissolução de halita        | Relatórios Técnicos Oficiais SGB /   |
| por Cavidades em Sal     | (Bairro Pinheiro)     | (sal-gema); desabamento     | CPRM (2019-2024); Euillades et al.   |
|                          |                       | de tetos e colapso de minas | (2020); Hartwig et al. (2023)        |
+--------------------------+-----------------------+-----------------------------+--------------------------------------+
| Explotação Intensiva de  | Recife / PE           | Despressurização de aquí-   | Pesquisas de pós-graduação UFPE;     |
| Água Subterrânea         | (Planície Costeira)   | feros profundos e adensa-   | relatórios de hidrogeologia APAC/PE  |
|                          |                       | mento de argilas compress.  | (2018-2023)                          |
+--------------------------+-----------------------+-----------------------------+--------------------------------------+
| Adensamento sobre Solos  | Baixada Santista / SP | Consolidação de camadas es- | Estudos Geotécnicos USP / IPT;       |
| Moles Marinho-Fluviais   | (Santos e S. Vicente) | pessas de argila marinha sob| histórico de inclinação de edifícios |
|                          |                       | peso de edifícios e aterros | da orla de Santos (1970-2024)        |
+--------------------------+-----------------------+-----------------------------+--------------------------------------+
| Obras Subterrâneas e     | São Paulo / SP        | Alívio de tensões por esca- | Monitoramento estrutural Metrô SP;   |
| Escavações de Túneis     | (Linhas de Metrô)     | vação mecanizada (TBM/Shield)| laudos técnicos e ações judiciais    |
|                          |                       | e recalques em edificações  | de moradores lindeiros (2016-2024)   |
+--------------------------+-----------------------+-----------------------------+--------------------------------------+
```

### 1.1. Detalhamento dos Casos Críticos

#### A. Maceió/AL — Mineração Subterrânea de Halita (Sal-gema)
* **Magnitude e Escala [Fato Confirmado]:** A atividade de mineração realizada pela petroquímica Braskem desde 1975 envolveu 35 poços de extração de sal por dissolução em profundidades entre 800 e 1.200 metros. O colapso e coalescência de cavidades cársticas induzidas causaram uma bacia de subsidência que atingiu taxas superiores a **20 a 40 cm/ano** no pico da deformação em setores do bairro Mutange, resultando na desocupação de mais de 14.000 imóveis e realocação de 60.000 pessoas nos bairros Pinheiro, Mutange, Bebedouro, Bom Parto e Farol.
* **Impacto Operacional Urbano [Fato Confirmado]:** Colapso de redes de drenagem urbana, desestruturação completa de linhas férreas urbanas (CBTU), rompimento de adutoras da Companhia de Saneamento de Alagoas (CASAL) e perda de valor patrimonial imobiliário estimada em mais de R$ 10 bilhões.

#### B. Recife/PE — Explotação de Aquíferos em Planície Costeira Sedimentar
* **Mecanismo [Fato Confirmado]:** A planície urbana do Recife assenta-se sobre sedimentos quaternários inconsolidados (argilas orgânicas, mangues e areias) sobrepostos a aquíferos profundos (Beberibe e Boa Viagem). A intensa perfuração de poços tubulares clandestinos ou com outorgas defasadas para abastecimento de condomínios residenciais e indústrias reduziu as pressões piezométricas, gerando recalques por adensamento de longo prazo.
* **Impacto Operacional [Fato Confirmado / Pesquisas UFPE]:** Deformações lentas na faixa de **5 a 15 mm/ano**, agravando as inundações por maré astronômica e chuvas convectivas devido ao rebaixamento relativo da calha de escoamento dos rios Capibaribe, Beberibe e Tejipió.

#### C. Santos e São Vicente/SP — Recalques em Argilas Marinhas Compressíveis
* **Mecanismo [Fato Confirmado]:** A orla de Santos possui uma formação geotécnica clássica com espessas camadas de argila marinha altamente compressível (espessuras de 20 a 40 metros). Edifícios construídos nas décadas de 1950 a 1970 com fundações diretas ou rasas em sapatas sofreram recalques diferenciais contínuos, resultando na conhecida inclinação visível de centenas de edifícios residenciais.
* **Impacto Operacional [Fato Confirmado]:** Necessidade contínua de macaqueamento hidráulico de prédios, manutenção corretiva de redes de gás encanado (Comgás) e recalques em pavimentos asfálticos e galerias de águas pluviais.

---

## 2. Problemas Operacionais Efetivamente Relatados por Atores de Mercado

A investigação distinguiu problemas concretos e cotidianos relatados por profissionais em campo:

### 2.1. Problemas Enfrentados por Municípios e Defesas Civis
1. **Reatividade Compulsória [Fato Confirmado]:** As secretarias municipais de Defesa Civil e Obras não possuem instrumentos preventivos em larga escala. A atuação ocorre apenas após o surgimento visível de trincas em alvenarias, abatimento de vias ou notificações de moradores pelo telefone 199.
2. **Dependência de Vistorias Manuais Físicas [Fato Confirmado]:** A avaliação municipal padrão é a inspeção visual por engenheiros ou técnicos de campo com prancheta, fita métrica e fissurômetros plásticos de acrílico. Esse método é lento, pontual e incapaz de determinar se a estrutura está afundando como um bloco rígido regional ou se sofre recalque pontual de fundação.
3. **Falta de Séries Temporais Históricas [Fato Confirmado]:** Quando um morador notifica a prefeitura sobre uma trinca recente, a equipe não tem como responder: *"Esta deformação começou no último mês ou ocorre de forma contínua há 5 anos?"*. A ausência de dados históricos prévios impossibilita a identificação de nexo de causalidade entre obras públicas e danos privados.
4. **Dependência Crônica de Órgãos Federais e Estaduais [Fato Confirmado]:** Em crises, prefeituras de médio e pequeno porte não possuem autonomia técnica e dependem de solicitações formais de laudos ao Instituto de Pesquisas Tecnológicas (IPT) ou ao Serviço Geológico do Brasil (SGB/CPRM), cujos relatórios podem demorar de meses a mais de um ano para serem finalizados.

### 2.2. Problemas Enfrentados por Concessionárias de Serviços Públicos Urbanos
1. **Concessionárias de Água e Saneamento (ex.: Sabesp, CASAL, Compesa, BRK):**
   * *Ruptura de Tubulações Rígidas:* Tubulações de ferro fundido e concreto sofrem esforços cortantes por movimentação diferencial do solo, gerando perdas físicas invisíveis de água potável no subsolo antes de aflorarem à superfície.
   * *Infiltração e Erosão Subterrânea:* Vazamentos crônicos de esgoto desestabilizam o maciço do solo, acelerando a perda de sustentação e formando voçorocas e crateras urbanas (*sinkholes*).
2. **Concessionárias de Gás Canalizado (ex.: Comgás, Naturgy):**
   * *Risco Severo de Explosão e Segurança Operacional:* Redes subterrâneas de gás exigem acompanhamento de estabilidade do solo; recalques diferenciais causam tensões excessivas nas conexões e válvulas de aço.
3. **Concessionárias de Linhas de Metrô e VLT:**
   * *Desalinhamento Milimétrico de Trilhos:* As linhas férreas e vias permanentes de transporte de massa exigem tolerâncias milimétricas. Qualquer abatimento sob o lastro ou laje de suporte coloca os trens em risco de descarrilamento, exigindo interrupção de tráfego para manutenção.

### 2.3. Problemas Enfrentados por Consultorias Geotécnicas e Periciais
1. **Custo e Complexidade de Instrumentação em Campo [Fato Confirmado]:** Instalar e manter marcos topográficos, estações totais robotizadas, inclinômetros e piezômetros em áreas urbanas povoadas tem custo elevado e alto risco de vandalismo, furto de cabeamento e perda de sinal.
2. **Barreira Operacional com Ferramental Científico de Satélite [Fato Confirmado]:** Geotécnicos dominam ensaios de solo (SPT, CPTu), cálculo de empuxos e modelos em elementos finitos (Plaxis, GeoStudio), mas **não possuem treinamento para processar imagens orbitais SAR complexas (SLC) em ambientes Linux utilizando scripts SNAP, ISCE ou Fortran**.

---

## 3. O Tríplice Filtro: Necessidade Técnica vs. Orçamento vs. Disposição a Pagar

O Radar++ não deve cometer o erro clássico de startups B2B/B2G de presumir que uma necessidade técnica evidente se traduzirá automaticamente em vendas:

```
+-----------------------------------------------------------------------------------------------------------------------+
|                                    O TRÍPLICE FILTRO DE QUALIFICAÇÃO DE DEMANDA NO BRASIL                             |
+--------------------------+-----------------------+-----------------------------+--------------------------------------+
| Segmento Avaliado        | Necessidade Técnica   | Existência de Orçamento     | Disposição Comprovada a Pagar        |
|                          | (O problema existe?)  | (O dinheiro está previsto?) | (Comprariam um SaaS de Satélite?)    |
+--------------------------+-----------------------+-----------------------------+--------------------------------------+
| Prefeituras Médias       | ALTA                  | BAIXA / RESTRITA            | NULA A CURTO PRAZO                   |
| (100k a 500k hab.)       | Necessitam mapear     | Recursos consumidos por     | Não compram software self-service.   |
|                          | áreas de risco R3/R4. | saúde e educação; sem rubri-| Exigem contratação de consultor com  |
|                          |                       | ca específica de software.  | ART para assumir a responsabilidade. |
+--------------------------+-----------------------+-----------------------------+--------------------------------------+
| Grandes Prefeituras e    | ALTA                  | MÉDIA A ALTA                | BAIXA PARA SOFTWARE ISOLADO          |
| Capitais (São Paulo,     | Volume expressivo de  | Verbas de Defesa Civil e    | Compram sistemas integrados com      |
| Rio de Janeiro, Maceió)  | encostas e obras.     | Obras; editais milionários. | consultoria técnica pesada e campo.  |
+--------------------------+-----------------------+-----------------------------+--------------------------------------+
| Concessionárias Privadas | ALTA                  | ALTA                        | MÉDIA A ALTA (B2B Corporativo)       |
| de Saneamento e Gás      | Mitigação de perdas e | OPEX contínuo de manutenção | Dispostas a pagar se demonstrada     |
|                          | risco de acidentes.   | e prevenção de perdas.      | redução de vazamentos e sinistros.   |
+--------------------------+-----------------------+-----------------------------+--------------------------------------+
| Consultorias de          | MÉDIA                 | MÉDIA POR PROJETO           | ALTA NO MODELO TRANSAÇÃO/LAUDO       |
| Geotecnia e Perícias     | Precisam embasar lau- | Verba embutida na proposta  | Dispostas a pagar por área processada|
| Independentes            | dos e perícias civis. | comercial ao cliente final. | para economizar dias de trabalho.    |
+--------------------------+-----------------------+-----------------------------+--------------------------------------+
```

### Síntese do Filtro:
* **Prefeituras:** Apresentam necessidade técnica crítica, mas **disposição a pagar nula para SaaS puro**.
* **Consultorias Geotécnicas e Concessionárias Privadas:** Apresentam o melhor equilíbrio entre necessidade, orçamento aprovado e disposição a pagar no modelo B2B.

---

## 4. Limitações das Soluções Existentes e Lacunas no Mercado

As soluções que operam no mercado de deformação urbana no Brasil apresentam limitações claras que definem a oportunidade do Radar++:

1. **Ferramentas Gratuitas Abertas (SNAP, MintPy, ISCE2):**
   * *Limitação:* Curva de aprendizado íngreme, ausência de visualizador web interativo, exigência de compilação em servidores dedicados e necessidade de parametrização manual de linhas de base.
2. **Consultorias Internacionais Especializadas (TRE Altamira, Fugro):**
   * *Limitação:* Tíquete financeiro mínimo inacessível (faturas de R$ 300k a R$ 1M), contratos burocráticos de longo prazo e entrega de relatórios estáticos em PDF com tempo de resposta de semanas.
3. **Levantamentos por Drones (VANTs / Fotogrametria / LiDAR):**
   * *Limitação:* Incapazes de medir movimentações milimétricas contínuas no tempo em escala urbana regional; custo operacional elevado por sobrevoo; não oferecem análise histórica retrospectiva anterior ao primeiro voo.
4. **Instrumentação de Campo (Topografia e Marcos Geodésicos):**
   * *Limitação:* Medição pontual (apenas onde o marco está fincado); sujeita a vandalismo e furto; custo proibitivo para monitorar uma bacia urbana de dezenas de quilômetros quadrados.

---

## 5. Conclusões e Implicações para o Radar++

1. **O Posicionamento Inicial deve Descartar Venda Direta a Prefeituras Médias:** O Radar++ não deve desenhar seu funil de vendas dependendo de prefeituras comprarem assinaturas em autosserviço. A rota para atender municípios deve ser **via canal indireto (B2B2G)**, oferecendo a plataforma para as empresas de engenharia consultiva e geotecnia que já disputam e vencem licitações públicas municipais.
2. **Oportunidade Principal em Subsidência Urbana:** Fornecer uma plataforma ágil de **auditoria e rastreamento histórico retrospectivo** baseada no acervo aberto Sentinel-1 (2014-2024), permitindo que consultorias e concessionárias descubram a evolução temporal de deformações antes de realizarem intervenções de campo.
