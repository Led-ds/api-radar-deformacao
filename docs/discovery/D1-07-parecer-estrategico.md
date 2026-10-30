# Radar++ Discovery — D1-07: Parecer Estratégico e Recomendações de Posicionamento do Radar++
## Síntese Executiva, Decisões de Governança e Roteiro para a Investigação D2

**Documento:** `D1-07-parecer-estrategico.md`  
**Ciclo:** Investigação D1 — Mercado e Viabilidade  
**Data:** 24 de Outubro de 2026  
**Status:** Oficial — Proposto para Deliberação da Governança  
**Responsável:** Equipe de Descoberta de Produto, Inteligência de Mercado e Sensoriamento Remoto  
**Repositório Oficial:** `api-radar-deformacao` (caminho: `docs/discovery/D1-07-parecer-estrategico.md`)  

---

## 1. Síntese Executiva da Investigação D1

A Investigação **D1 (Mercado e Viabilidade)** atingiu o objetivo de transformar o projeto Radar++ de uma ideia exploratória tecnológica em uma proposta de negócio fundamentada em dados empíricos e evidências verificáveis.

### 1.1. As Três Lições Estratégicas Centrais
1. **O Setor Público Municipal Não é Canal de Venda Direta:** Acreditava-se inicialmente que prefeituras médias seriam o cliente ideal para um SaaS de monitoramento de subsidência urbana. A investigação documental no PNCP e nas rotinas da Defesa Civil demonstrou que essa hipótese é **inviável**. Prefeituras não compram software de satélite avulso; compram **serviços integrados de engenharia com ART e responsabilidade civil**.
2. **O Cliente Ideal Inicial é a Consultoria Geotécnica e a Concessionária Privada (B2B / B2B2G):** A disposição a pagar comprovada concentra-se em consultorias de engenharia civil/ambiental (que precisam embasar laudos periciais e projetos de contenção) e em concessionárias privadas (rodovias, ferrovias, saneamento privado e gás) que têm obrigações regulatórias contratuais e risco financeiro direto por paralisações operacionais.
3. **A Estrutura Tecnológica de Baixo Custo é Factível:** A utilização da API **NASA/ASF HyP3** (com cota gratuita de 8.000 créditos mensais) acoplada à biblioteca de código aberto **MintPy** (SBAS) em um worker assíncrono em Python viabiliza a entrega do produto sem necessidade de investimentos milionários em infraestrutura de HPC ou compra de imagens comerciais no estágio de validação.

---

## 2. Posicionamento Estratégico Recomendado para o Radar++

```
+---------------------------------------------------------------------------------------------------------------+
|                                      POSICIONAMENTO ESTRATÉGICO DO RADAR++                                    |
+--------------------------+------------------------------------------------------------------------------------+
| Eixo Estratégico         | Definição Oficial Proposta                                                         |
+--------------------------+------------------------------------------------------------------------------------+
| O que o Radar++ É        | Uma plataforma web moderna de inteligência e auditoria geoespacial que simplifica |
|                          | o acesso, o processamento histórico e o monitoramento contínuo de deformações     |
|                          | da superfície terrestre utilizando dados abertos de radar (Sentinel-1).           |
+--------------------------+------------------------------------------------------------------------------------+
| O que o Radar++ NÃO É    | Não é ferramenta emissora de ART, não é substituto de laudo geotécnico de campo,   |
|                          | não é caixa-preta de IA generativa e não é visualizador ótico genérico.           |
+--------------------------+------------------------------------------------------------------------------------+
| Proposta de Valor Única  | "Zero-Install InSAR": transformar a complexidade matemática da geodésia radar em   |
| (UVP)                    | mapas interativos de deslocamento milimétrico gerados em minutos, 100% nativo nos  |
|                          | padrões geodésicos brasileiros (SIRGAS 2000) e integrado ao fluxo das consultorias.|
+--------------------------+------------------------------------------------------------------------------------+
| Modelo Comercial         | B2B Transacional e Assinatura: cobrança por área processada (R$ / km²) ou por      |
| Recomendado              | pacote mensal de análises para empresas de engenharia consultiva e concessionárias.|
+--------------------------+------------------------------------------------------------------------------------+
```

---

## 3. Deliberação sobre os Três Mercados Prioritários

```
+--------------------------------------------------------------------------------------------------------------------+
|                                      DIRECIONAMENTO DOS MERCADOS PRIORITÁRIOS                                      |
+--------------------------+-----------------------+-----------------------------------------------------------------+
| Segmento                 | Prioridade Atual      | Decisão Estratégica Fundamentada                                |
+--------------------------+-----------------------+-----------------------------------------------------------------+
| 1. Subsidência Urbana    | Manter como Vitrine   | Focar em casos de alta repercussão (Maceió) para validação ci-  |
|                          | Técnica e Laudos      | entífica retrospectiva e atendimento a consultorias periciais.  |
|                          | Periciais B2B         | Abandonar prospecção direta de prefeituras em autosserviço.     |
+--------------------------+-----------------------+-----------------------------------------------------------------+
| 2. Infraestrutura Linear | **PRIORIDADE MÁXIMA** | Segmento com maior densidade de capital privado, OPEX contínuo  |
| (Rodovias e Ferrovias)   | **COMERCIAL (B2B)**   | de conservação e regulação ativa (ANTT/ARTESP). Objeto da D2.   |
+--------------------------+-----------------------+-----------------------------------------------------------------+
| 3. Consultorias e P&D    | Canal de Parcerias e  | Atuar como parceiro tecnológico de PMEs de engenharia e grupos |
| Geotécnico               | Validação Metodológica| acadêmicos (UFPE, UNESP, UERJ) para obter validação científica. |
+--------------------------+-----------------------+-----------------------------------------------------------------+
```

---

## 4. Roteiro e Preparação para a Investigação D2 (Infraestrutura Linear)

A próxima etapa do plano de descoberta deve aprofundar o segmento com maior potencial de receita recorrente: **Infraestrutura Linear (Rodovias e Ferrovias)**.

### Objetivos da Investigação D2:
1. **Mapeamento das Resoluções e Normas da ANTT e ARTESP:** Identificar as exigências formais de sistemas de gestão de taludes e contenções impostas às concessionárias rodoviárias federais e paulistas.
2. **Levantamento das Principais Concessionárias Privadas:** Mapear os grupos CCR, EcoRodovias, Arteris, Rumo Logística, VLI e MRS Logística, identificando decisores e tecnologias de monitoramento de taludes em uso.
3. **Avaliação da Competição Satélite vs. Drones em Taludes:** Analisar por que concessionárias investem em sobrevoos com drones (fotogrametria/LiDAR) e qual o espaço de complementaridade para o InSAR orbital na detecção de *creep* (rastejo de encostas) regional.
4. **Viabilidade Técnica da Banda C em Corredores Serranos Tropicais:** Investigar a coerência de fase em trechos críticos como Serra das Araras (BR-116/RJ) e Serra do Mar (SP/PR).

---

## 5. Recomendações Imediatas de Governança

1. **Aprovação Documental:** Submeter os 7 documentos da pasta `docs/discovery/` para revisão humana oficial.
2. **Não Implementar Código sem Validação Qualitativa:** Manter o código-fonte inalterado até a realização de pelo menos 3 entrevistas de descoberta com engenheiros geotécnicos baseadas no roteiro `D1-04`.
3. **Autorização para Simulação do Piloto Maceió:** Aguardar decisão de governança para posterior execução controlada do protocolo científico `D1-05` em ambiente de experimentação isolado (Jupyter Notebook).
