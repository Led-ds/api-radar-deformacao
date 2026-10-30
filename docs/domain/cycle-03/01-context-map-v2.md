# Radar++ — Domínio: 01-context-map-v2.md
## Mapa de Contextos v2: Fronteiras Estratégicas, Relações Intercontextuais e Alinhamento Arquitetural

**Documento:** `01-context-map-v2.md`  
**Ciclo:** Ciclo 3 — Modelagem Estratégica e Tática com DDD  
**Data:** 24 de Outubro de 2026  
**Status:** Oficial — Entregue para Homologação de Arquitetura de Domínio  
**Responsável:** Especialista em DDD, Arquiteto de Software, Especialista em SAR/InSAR  
**Repositório Oficial:** `api-radar-deformacao` (caminho: `docs/domain/cycle-03/01-context-map-v2.md`)  
**Diretriz de Design:** Não criar um microsserviço por contexto delimitado. O design estratégico estabelece fronteiras conceituais estritas que podem coexistir em uma aplicação modular (*Modular Monolith*), preservando o baixo acoplamento e a consistência semântica.

---

## 1. Visão Geral dos Quatro Contextos Delimitados Oficiais

Com base na simplificação aprovada no Ciclo 2 ([docs/domain/03-subdominios-e-contextos-candidatos.md](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/docs/domain/03-subdominios-e-contextos-candidatos.md)), o domínio do Radar++ é delimitado em **quatro contextos conceituais**, eliminando a sobrecarga de hiperfragmentação preliminar:

```mermaid
graph TD
    subgraph "Radar++ — Mapa de Contextos Estratégico v2"
        subgraph "1. Subdomínio de Suporte: Gestão Territorial (Territory Context)"
            BC_TERR["Contexto de Gestão Territorial<br/>• Responsabilidade: Áreas de Interesse (AOIs)<br/>• Agregado Raiz: AreaOfInterest<br/>• Papel: Provedor Territorial Upstream"]
        end

        subgraph "2. Subdomínio de Integração: Catálogo SAR (SAR Catalog Context)"
            BC_CAT["Contexto de Catálogo SAR<br/>• Responsabilidade: Descoberta de Cenas/Bursts Orbitais<br/>• Papel: Integração Externa via Anti-Corruption Layer (ACL)"]
        end

        subgraph "3. Domínio Principal (Core): Processamento e Produtos InSAR (InSAR Processing & Products)"
            BC_PROC["Contexto de Processamento e Produtos InSAR<br/>• Responsabilidade: Orquestração, Inversão Geodésica, Produtos e Avaliação Científica<br/>• Agregados: ProcessingRequest, ProcessingJob, InSarAnalysis, ScientificProduct"]
        end

        subgraph "4. Domínio Especializado: Análise de Subsidência Urbana (Urban Subsidence Analysis)"
            BC_URBAN["Contexto de Análise de Subsidência Urbana<br/>• Responsabilidade: Back-Analysis, Delimitação de Bacias e Laudos de Apoio<br/>• Agregado: UrbanInvestigation"]
        end
    end

    BC_TERR -->|Published Language (Geometria WKT/BBOX)| BC_CAT
    BC_TERR -->|Customer-Supplier (AOI ID e Vértices)| BC_PROC
    BC_CAT -->|Published Language (Metadados Cenas/Bursts)| BC_PROC
    BC_PROC -->|Open Host Service (Produtos Geodésicos / Séries)| BC_URBAN
```

---

## 2. Fronteiras, Responsabilidades e Dependências

```
+----------------------------------------------------------------------------------------------------------------------------------------------------+
|                                    MATRIZ DE RESPONSABILIDADES E FRONTEIRAS DOS QUATRO CONTEXTOS                                                   |
+----+-----------------------+-----------------------+------------------------------------------+----------------------------------------------------+
| ID | Contexto Delimitado   | Tipo de Subdomínio    | Responsabilidade Exclusiva de Negócio    | Dependências e Interações                          |
+----+-----------------------+-----------------------+------------------------------------------+----------------------------------------------------+
| 01 | Gestão Territorial    | Suporte               | Cadastro, validação topológica, cálculo  | Upstream para Catálogo SAR e Processamento.        |
|    | (Territory)           | (*Supporting*)        | de área e gestão do ciclo de vida da AOI.| Não depende de nenhum outro contexto interno.      |
+----+-----------------------+-----------------------+------------------------------------------+----------------------------------------------------+
| 02 | Catálogo SAR          | Suporte e Integração  | Descoberta de aquisições Sentinel-1,     | Depende da geometria da AOI fornecida por Gestão   |
|    | (SAR Catalog)         | (*Integration*)       | indexação de metadados de cenas e bursts.| Territorial. Isola APIs da NASA/ESA via ACL.       |
+----+-----------------------+-----------------------+------------------------------------------+----------------------------------------------------+
| 03 | Processamento e       | **Domínio Central**   | Orquestração de jobs assíncronos, cálculo| Depende das cenas de Catálogo SAR e da AOI de      |
|    | Produtos InSAR        | *(Core Domain)*       | de pares, inversão SBAS, avaliação de    | Gestão Territorial. Fornece produtos geodésicos    |
|    | (InSAR Processing)    |                       | qualidade científica e gestão de produtos| auditáveis para o contexto especializado Urban.    |
+----+-----------------------+-----------------------+------------------------------------------+----------------------------------------------------+
| 04 | Análise de Subsidên-  | Aplicação Especializ. | Comparação temporal de cenários urbanos, | Downstream estrito do Motor InSAR Core.            |
|    | cia Urbana (Urban)    | *(Specialized Core)*  | perfis topográficos e emissão de laudos. | Consome produtos matriciais e séries temporais.    |
+----+-----------------------+-----------------------+------------------------------------------+----------------------------------------------------+
```

---

## 3. Avaliação da Separação Conceitual entre Produtos InSAR e Avaliação de Qualidade

Uma questão arquitetural crítica examinada neste ciclo foi: **Produtos InSAR e Avaliação de Qualidade devem constituir contextos delimitados separados ou compartilhar o mesmo contexto?**

### Parecer Arquitetural:
1. **Separação Conceitual Clara no Domínio:**
   * A entidade `ScientificProduct` representa o artefato de dado matricial (o raster de velocidade anual, o raster de deslocamento acumulado, a matriz de coerência espacial e suas projeções COG);
   * A entidade `QualityAssessment` (Avaliação de Qualidade) representa o parecer e as métricas científicas que atestam a confiabilidade daquele produto (percentual de resíduos de desdobramento de fase, coerência espacial média, detecção de erros de ciclo de $2\pi$ e desvio-padrão).
2. **Coabitação no Mesmo Bounded Context (*InSAR Processing & Products*):**
   * Separar os dois em Bounded Contexts distintos forçaria a adoção de mensageria assíncrona entre o cálculo geodésico e a sua própria validação de qualidade, criando estados intermediários inválidos onde um produto existe mas não se sabe se é confiável;
   * No pipeline científico real (HyP3 + MintPy), a matriz de resíduos de desdobramento é calculada na mesma etapa de inversão temporal. Portanto, **Produtos InSAR e Avaliação de Qualidade residem no mesmo contexto delimitado**, porém modelados como conceitos e entidades semanticamente distintos.

---

## 4. Padrões de Integração Estratégica (Context Map Patterns)

```
+----------------------------------------------------------------------------------------------------------------------------------------------------+
|                                    PADRÕES DE RELAÇÃO ENTRE CONTEXTOS DELIMITADOS (DDD)                                                           |
+--------------------------+--------------------------+-----------------------+----------------------------------------------------------------------+
| Upstream (Fornecedor)    | Downstream (Consumidor)  | Padrão Adotado        | Racional Técnico-Arquitetural                                        |
+--------------------------+--------------------------+-----------------------+----------------------------------------------------------------------+
| Acervos Espaciais        | Catálogo SAR             | Anti-Corruption Layer | As respostas JSON brutas das APIs da NASA (ASF Vertex) e da ESA      |
| (NASA ASF / ESA CDSE)    | Radar++                  | (ACL)                 | (Copernicus) são traduzidas em Value Objects canônicos de domínio.   |
+--------------------------+--------------------------+-----------------------+----------------------------------------------------------------------+
| Gestão Territorial       | Catálogo SAR             | Published Language    | O Catálogo consome a representação WKT/GeoJSON padronizada da BBOX.  |
+--------------------------+--------------------------+-----------------------+----------------------------------------------------------------------+
| Gestão Territorial       | Processamento InSAR      | Customer-Supplier     | O contexto de processamento exige que a AOI esteja validada antes de |
|                          |                          |                       | aceitar a submissão de uma solicitação de cálculo.                  |
+--------------------------+--------------------------+-----------------------+----------------------------------------------------------------------+
| Catálogo SAR             | Processamento InSAR      | Published Language    | O motor InSAR consome coleções imutáveis de `SarSceneMetadata`.      |
+--------------------------+--------------------------+-----------------------+----------------------------------------------------------------------+
| Processamento InSAR      | Análise Urbana           | Open Host Service     | O Core disponibiliza endpoints e leituras otimizadas para consulta   |
|                          |                          | (OHS) / Published Lang| de séries temporais e carregamento sob demanda de COGs via HTTP.     |
+--------------------------+--------------------------+-----------------------+----------------------------------------------------------------------+
```

---

## 5. Diretriz de Implementação Física: Monólito Modular

> **DECISÃO DE ARQUITETURA:**  
> A separação em quatro Bounded Contexts **não implica a criação de quatro microsserviços ou quatro bancos de dados distribuídos nesta fase**.  
> O Radar++ adotará a estratégia de **Monólito Modular (*Modular Monolith*)**, onde cada contexto delimitado é implementado como um pacote/módulo autônomo com fronteiras explícitas, comunicação síncrona via interfaces de serviço bem definidas e comunicação assíncrona por eventos de domínio internos. Essa abordagem preserva a integridade conceitual do DDD, reduz drasticamente os custos de hospedagem e evita a complexidade de transações distribuídas e redes orquestradas de contêineres na fase de validação do MVP.
