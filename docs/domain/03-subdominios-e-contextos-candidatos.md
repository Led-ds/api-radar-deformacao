# Radar++ — Domínio: 03-subdominios-e-contextos-candidatos.md
## Análise Crítica dos Subdomínios, Avaliação de Sobreposições e Proposta de Simplificação

**Documento:** `03-subdominios-e-contextos-candidatos.md`  
**Ciclo:** Ciclo 2 — Linguagem Ubíqua e Modelo Conceitual do Domínio  
**Data:** 24 de Outubro de 2026  
**Status:** Oficial — Entregue para Homologação de Domínio  
**Responsável:** Especialista em DDD, Arquiteto de Software, Analista de Negócios Sênior  
**Repositório Oficial:** `api-radar-deformacao` (caminho: `docs/domain/03-subdominios-e-contextos-candidatos.md`)  
**Diretriz Arquitetural Fundamental:** Não assumir que cada capacidade de negócio corresponde obrigatoriamente a um *Bounded Context*, microsserviço ou módulo físico separado. Propor simplificações pragmáticas para eliminar sobreposições conceituais e evitar complexidade acidental no MVP.

---

## 1. Avaliação Crítica da Divisão Preliminar de Sete Contextos

No documento preliminar [D-ENC-05](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/docs/discovery/D-ENC-05-contextos-delimitados.md), foi proposta uma divisão conceitual contendo sete contextos delimitados:
1. Gestão de Áreas de Interesse (AOI);
2. Catálogo SAR;
3. Orquestração e Processamento;
4. Produtos e Séries Temporais;
5. Avaliação de Qualidade;
6. Subsidência Urbana;
7. Corredores de Infraestrutura Linear.

### Diagnóstico de Engenharia de Software e DDD Estratégico:
Embora essa divisão represente uma enumeração compreensiva de responsabilidades teóricas, **adotar sete Bounded Contexts em um produto em estágio de validação inicial constitui hiperfragmentação e excesso de engenharia (*over-engineering*)**:
* **Falso Acoplamento Microsserviçal:** Separar *Orquestração*, *Produtos* e *Qualidade* em contextos distintos impõe o uso de mensageria complexa, consistência eventual assíncrona desnecessária e problemas de transações distribuídas para operações que ocorrem de forma contínua no mesmo pipeline geodésico (HyP3 + MintPy);
* **Fronteira Tênue entre Produtos e Séries Temporais:** Uma *Série Temporal* não é um domínio independente de um *Produto InSAR*; ela é uma projeção analítica pontual $(x, y, t)$ do próprio produto raster matricial;
* **Prematuridade do Módulo de Infraestrutura Linear:** O módulo de corredores rodoviários e ferroviários ainda depende da validação de mercado das entrevistas qualitativas ([D1-04](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/docs/discovery/D1-04-roteiro-entrevistas.md)). Mantê-lo como um contexto ativo no MVP gera código ocioso e desvio de foco da engenharia.

---

## 2. Análise de Sobreposições de Responsabilidades

```
+----------------------------------------------------------------------------------------------------------------------------------------------------+
|                                    ANÁLISE DE SOBREPOSIÇÃO DE RESPONSABILIDADES NO MODELO PRELIMINAR                                               |
+----+-----------------------------+-----------------------------+------------------------------------+--------------------------------------+
| ID | Contextos com Sobreposição  | Ponto Crítico de Atrito     | Problema no Design de Domínio      | Solução de Simplificação Proposta    |
+----+-----------------------------+-----------------------------+------------------------------------+--------------------------------------+
| S1 | Orquestração + Processamento| O processamento só é consi- | Criação de 3 agregados separados   | **Unificar no Core de Interferometria|
|    | vs. Produtos InSAR          | derado concluído quando o   | para um pipeline geodésico que é   | e Séries Temporais (InSAR Core)**.   |
|    | vs. Avaliação de Qualidade  | raster COG e as métricas de | matematicamente indivisível na     | O cálculo da qualidade e geração do  |
|    |                             | qualidade são gerados juntos| inversão matricial do MintPy.      | COG tornam-se etapas do mesmo ciclo. |
+----+-----------------------------+-----------------------------+------------------------------------+--------------------------------------+
| S2 | Gestão de AOI               | A Área de Interesse é uma   | Tratar AOI como um contexto isolado| **Consolidar no Subdomínio de Gestão |
|    | vs. Organização/Multi-Tenant| casca vetorial simples que  | desconectado da governança do      | Territorial e Acesso (Territory)**.  |
|    |                             | pertence a uma Organização. | cliente gera acoplamento anêmico.  | A AOI passa a ser o agregado raiz.   |
+----+-----------------------------+-----------------------------+------------------------------------+--------------------------------------+
| S3 | Subsidência Urbana          | O módulo de subsidência con-| Criar um contexto de negócio que   | **Estruturar como Camada Especiali-  |
|    | vs. Core InSAR              | some os mesmos COGs e séries| apenas estiliza o mapa no frontend | zada de Aplicação de Laudos (Urban)**|
|    |                             | do Core, sem dados próprios.| gera cascas vazias de backend.     | focada em back-analysis e relatórios.|
+----+-----------------------------+-----------------------------+------------------------------------+--------------------------------------+
| S4 | Infraestrutura Linear       | O mercado não foi validado  | Modelar entidades viárias (km, esta| **Congelar Formalmente**: manter fora|
|    |                             | nas entrevistas de campo.   | queamento) antes da validação.     | do escopo do MVP até o fim da Fase 1.|
+----+-----------------------------+-----------------------------+------------------------------------+--------------------------------------+
```

---

## 3. Proposta de Arquitetura de Domínio Simplificada (Quatro Contextos)

Para o lançamento do MVP com a vitrine de Maceió/AL, a arquitetura estratégica do Radar++ é racionalizada em **três contextos delimitados operacionais** e **um contexto de aplicação especializada**:

```mermaid
graph TD
    subgraph "Radar++ — Mapa de Contextos Simplificado e Pragmático (v1)"
        subgraph "1. Subdomínio de Suporte: Gestão Territorial (Territory & AOI)"
            BC_TERR["Contexto de Gestão Territorial<br/>• Agregado: AreaDeInteresse (AOI)<br/>• Objetos de Valor: BBOX, Geometria WKT, TenantId"]
        end

        subgraph "2. Subdomínio de Integração: Catálogo SAR (SAR Discovery & Catalog)"
            BC_CAT["Contexto de Catálogo SAR<br/>• Agregado: ConsultaCatalogo<br/>• Entidades: CenaSAR, MetadadosOrbita<br/>• Fronteira: Anti-Corruption Layer (ACL)"]
        end

        subgraph "3. Domínio Central Unificado: Motor InSAR e Séries Temporais (InSAR Core Engine)"
            BC_CORE["Contexto Unificado InSAR Core<br/>• Agregado 1: SolicitacaoProcessamento<br/>• Agregado 2: JobProcessamento (Orquestração/Estados)<br/>• Agregado 3: ProdutoInSAR (Rasters COG, STAC)<br/>• Objetos de Valor: ParInterferometrico, Coerencia, MetricasQualidade, SerieTemporal"]
        end

        subgraph "4. Contexto Especializado de Aplicação: Análise Urbana de Subsidência (Urban Analysis)"
            BC_URBAN["Contexto Especializado Urban (MVP)<br/>• Agregado: EstudoSubsidencia<br/>• Entidades: BaciaDeformacao, PerfilTransversal<br/>• Entrega: LaudoSinteticoApoio (PDF/Auditoria)"]
        end
    end

    BC_TERR -->|Fornece Geometria da AOI| BC_CAT
    BC_TERR -->|Vincula Estudo à AOI| BC_CORE
    BC_CAT -->|Fornece Cenas Candidatas Homogêneas| BC_CORE
    BC_CORE -->|Disponibiliza Produtos COG e Séries Temporais (OHS)| BC_URBAN
```

---

## 4. Caracterização dos Contextos Candidatos Consolidados

### 4.1. Contexto 1: Gestão Territorial e Áreas de Interesse (*Territory & AOI Context*)
* **Tipo de Subdomínio:** Suporte (*Supporting Subdomain*).
* **Fronteira Semântica:** Responsável exclusivo pela delimitação, desenho, importação (GeoJSON/Shapefile/KML), validação topológica e cálculo geométrico das áreas espaciais monitoradas por cada cliente/inquilino.
* **Agregado Raiz:** `AreaDeInteresse` (AOI).
* **Invariantes Centrais:** Polígono fechado sem auto-interseção; limites de área parametrizados ([RT1](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/docs/discovery/D3-05-catalogo-regras.md#L175-L178)); isolamento rígido por organização (*TenantId*).

### 4.2. Contexto 2: Catálogo SAR e Descoberta Orbital (*SAR Discovery & Catalog Context*)
* **Tipo de Subdomínio:** Suporte e Integração Externa.
* **Fronteira Semântica:** Responsável pela busca de metadados de cenas e *bursts* Sentinel-1 em repositórios abertos externos (ASF Search API, Copernicus Data Space), isolando a arquitetura interna por meio de uma Camada Anti-Corrupção (ACL).
* **Agregado Raiz:** `ConsultaCatalogo`.
* **Entidades e Objetos de Valor:** `CenaSAR`, `MetadadosOrbita`, `JanelaTemporal`.
* **Padrão de Integração:** *Anti-Corruption Layer (ACL)* que traduz os formatos específicos de resposta das agências espaciais para o modelo canônico de cenas do Radar++.

### 4.3. Contexto 3: Motor InSAR e Séries Temporais (*InSAR Core Engine Context*)
* **Tipo de Subdomínio:** **Domínio Central Unificado (*Core Domain*)**.
* **Fronteira Semântica:** Unifica o que antes estava fragmentado entre orquestração, processamento, produtos e qualidade. Gerencia a criação da solicitação, montagem de pares interferométricos compatíveis, despacho assíncrono de jobs, ingestão de rasters, avaliação de coerência/resíduos de desdobramento de fase e disponibilização de mapas em Cloud Optimized GeoTIFF (COG) e séries temporais.
* **Agregados Centrais:**
  1. `SolicitacaoProcessamento` (Controle de entrada e governança do pedido);
  2. `JobProcessamento` (Máquina de estados assíncrona da execução);
  3. `ProdutoInSAR` (O raster matricial geodésico, suas métricas de qualidade e as séries temporais pontuais extraídas).
* **Vantagem da Consolidação:** Elimina transações distribuídas e sincronizações desnecessárias; a etapa de avaliação de qualidade torna-se um método de domínio que sela o produto como "Conforme" ou "Com Advertência Técnica" antes da persistência no storage S3.

### 4.4. Contexto 4: Análise Especializada de Subsidência Urbana (*Urban Analysis Context*)
* **Tipo de Subdomínio:** Domínio Especializado de Negócio (*Specialized Core Application*).
* **Fronteira Semântica:** Camada de negócio voltada ao engenheiro geotécnico e à defesa civil para investigação de afundamentos urbanos. Consome as séries temporais e mapas do Core para delimitar a geometria das bacias de abatimento, traçar perfis transversais de seção e gerar laudos técnicos padronizados de apoio pericial.
* **Agregado Raiz:** `EstudoSubsidencia`.
* **Relação com o Core:** Atua como cliente (*Customer*) via serviço de host aberto (*Open Host Service — OHS*) disponibilizado pelo Core.

---

## 5. Matriz de Relacionamento e Padrões de Integração (*Context Map*)

```
+----------------------------------------------------------------------------------------------------------------------------------------------------+
|                                    MATRIZ REFINADA DE PADRÕES DE INTEGRAÇÃO (CONTEXT MAP v1)                                                       |
+--------------------------+--------------------------+-----------------------+----------------------------------------------------------------------+
| Upstream (Fornecedor)    | Downstream (Consumidor)  | Padrão de Integração  | Justificativa Arquitetural                                           |
+--------------------------+--------------------------+-----------------------+----------------------------------------------------------------------+
| APIs Externas (ASF/ESA)  | Catálogo SAR             | Anti-Corruption Layer | Blindar o domínio contra mudanças estruturais nos JSONs da NASA/ESA. |
+--------------------------+--------------------------+-----------------------+----------------------------------------------------------------------+
| Gestão Territorial (AOI) | Catálogo SAR             | Published Language    | O Catálogo consome a geometria padronizada (WKT/GeoJSON) da AOI.     |
+--------------------------+--------------------------+-----------------------+----------------------------------------------------------------------+
| Gestão Territorial (AOI) | Motor InSAR Core         | Customer-Supplier     | O Core referencia o identificador imutável da AOI para recorte.      |
+--------------------------+--------------------------+-----------------------+----------------------------------------------------------------------+
| Catálogo SAR             | Motor InSAR Core         | Published Language    | O Core consome listas padronizadas de `CenaSAR` filtradas pela ACL.  |
+--------------------------+--------------------------+-----------------------+----------------------------------------------------------------------+
| Motor InSAR Core         | Análise Urbana           | Open Host Service     | O módulo Urban consome rasters COG e séries temporais via API REST.  |
+--------------------------+--------------------------+-----------------------+----------------------------------------------------------------------+
```

---

## 6. Parecer de Simplificação e Recomendações

> **PARECER DA ARQUITETURA DE SOFTWARE:**  
> A redução de sete para **quatro Bounded Contexts** (Gestão Territorial, Catálogo SAR, Motor InSAR Core e Análise Urbana) preserva integralmente a separação de responsabilidades do negócio, elimina sobrecargas operacionais de microsserviços prematuros e permite que a equipe foque 100% da energia no que é essencial para o MVP: **comprovar o processamento científico de Maceió e disponibilizar as séries temporais na web com confiabilidade auditável**.
