# Radar++ Discovery — D-ENC-05: Proposta de Contextos Delimitados e Mapa de Contextos
## Arquitetura Estratégica em Domain-Driven Design (DDD) e Fronteiras de Domínio

**Documento:** `D-ENC-05-contextos-delimitados.md`  
**Ciclo:** Encerramento do Discovery e Preparação da Engenharia de Domínio  
**Data:** 24 de Outubro de 2026  
**Status:** Oficial — Proposta Estratégica para Arquitetura de Domínio  
**Responsável:** Arquiteto de Software (DDD / Clean Architecture), Product Manager B2B/B2G  
**Repositório Oficial:** `api-radar-deformacao` (caminho: `docs/discovery/D-ENC-05-contextos-delimitados.md`)  
**Regra Estrita de Governança:** Esta divisão é uma proposta conceitual de design estratégico. **Não criar microsserviços, novos repositórios Git, módulos Maven/Gradle ou bancos de dados nesta fase.**

---

## 1. Visão Geral dos Contextos Delimitados (*Bounded Contexts*)

No Domain-Driven Design (DDD), um **Contexto Delimitado (*Bounded Context*)** define uma fronteira explícita dentro da qual um modelo de domínio específico se aplica com significado único e coeso.

O Radar++ divide seu espaço de problemas em sete contextos delimitados, organizados entre o domínio principal (*Core Domain*), subdomínios de suporte (*Supporting Subdomains*) e subdomínios genéricos (*Generic Subdomains*):

```mermaid
graph TD
    subgraph "Radar++ — Mapa Estratégico de Contextos (Context Map)"
        subgraph "Subdomínios de Suporte e Integração"
            BC_AOI["1. Contexto de Gestão de Áreas de Interesse (AOI)<br/>[Supporting Subdomain]"]
            BC_CAT["2. Contexto de Catálogo SAR<br/>[Supporting Subdomain / ACL Externa]"]
        end

        subgraph "Domínio Central (Core Domain - Radar++ Core)"
            BC_ORQ["3. Contexto de Orquestração e Processamento<br/>[Core Domain - Assíncrono]"]
            BC_PROD["4. Contexto de Produtos e Séries Temporais<br/>[Core Domain - Geodésia / COG]"]
            BC_QUAL["5. Contexto de Avaliação de Qualidade<br/>[Core Domain - Confiabilidade]"]
        end

        subgraph "Subdomínios Especializados de Negócio"
            BC_URBAN["6. Contexto Especializado de Subsidência Urbana<br/>[Specialized Core - Radar++ Urban]"]
            BC_INFRA["7. Contexto de Corredores de Infraestrutura<br/>[Specialized Core - Radar++ Infra (Futuro)]"]
        end
    end

    BC_AOI -->|Disponibiliza Polígono/BBOX| BC_CAT
    BC_AOI -->|Referencia AOI| BC_ORQ
    BC_CAT -->|Fornece Cenas Compatíveis| BC_ORQ
    BC_ORQ -->|Gera Artefatos| BC_PROD
    BC_PROD -->|Submete Matrizes| BC_QUAL
    BC_QUAL -->|Atesta Confiabilidade| BC_PROD

    BC_PROD -->|Alimenta Visualização| BC_URBAN
    BC_PROD -.->|Alimentará Triagem (Fase 3)| BC_INFRA
```

---

## 2. Detalhamento Estruturado dos Contextos Delimitados

### 2.1. Contexto de Gestão de Áreas de Interesse (AOI Management Context)
* **Classificação DDD:** Subdomínio de Suporte (*Supporting Subdomain*).
* **Responsabilidade Única:** Cadastro, importação, validação topológica e persistência das geometrias vetoriais (polígonos e envelopes) que definem as regiões monitoradas por cada organização (*Tenant*).
* **Agregado Raiz (Root Entity):** `AreaDeInteresse` (AOI).
* **Objetos de Valor (Value Objects):** `GeometriaPoligonal`, `BoundingBox`, `AreaMetrica`, `TenantId`.
* **Eventos de Domínio Emitidos:**
  * `AreaDeInteresseCadastradaEvent`
  * `AreaDeInteresseAtualizadaEvent`
  * `AreaDeInteresseExcluidaEvent`
* **Padrões de Integração:** *Open Host Service (OHS)* para fornecer polígonos validados aos contextos de Catálogo e Orquestração.

---

### 2.2. Contexto de Catálogo SAR (SAR Catalog Context)
* **Classificação DDD:** Subdomínio de Suporte e Integração Externa.
* **Responsabilidade Única:** Conectar-se a APIs abertas externas (ASF Search, ESA Copernicus Data Space), descobrir cenas orbitais disponíveis para uma AOI e catalogar metadados padronizados.
* **Agregado Raiz:** `ConsultaCatalogo`.
* **Entidades / Objetos de Valor:** `CenaSAR`, `MetadadosOrbita`, `Footprint`, `JanelaTemporal`.
* **Padrão de Integração Crítico:** **Camada Anti-Corrupção (*Anti-Corruption Layer — ACL*)**. Isola os modelos de resposta proprietários da NASA/ESA em relação ao modelo de domínio do Radar++.
* **Eventos Emitidos:**
  * `CenasSARDescobertasEvent`
  * `MetadadosOrbitaisSincronizadosEvent`

---

### 2.3. Contexto de Orquestração e Processamento (Processing Orchestration Context)
* **Classificação DDD:** Domínio Central (*Core Domain*).
* **Responsabilidade Única:** Validar compatibilidade interferométrica (mesmo Path, IW, VV), montar grafos de conexões de pares (SBAS), despachar requisições para motores de processamento em nuvem (HyP3 / contêineres locais) e gerenciar a máquina de estados assíncrona dos jobs.
* **Agregado Raiz:** `JobDeProcessamento`.
* **Entidades / Objetos de Valor:** `ParInterferometrico`, `GrafoConexoes`, `ParametrosProcessamento`, `StatusExecucao`.
* **Eventos Emitidos:**
  * `ProcessamentoIniciadoEvent`
  * `EtapaProcessamentoConcluidaEvent`
  * `ProcessamentoFinalizadoComSucessoEvent`
  * `ProcessamentoFalhouEvent`

---

### 2.4. Contexto de Produtos e Séries Temporais (InSAR Product & Time Series Context)
* **Classificação DDD:** Domínio Central (*Core Domain*).
* **Responsabilidade Única:** Ingestão de artefatos geodésicos resultantes do processamento, conversão para Cloud Optimized GeoTIFF (COG), catalogação no padrão STAC, extração de séries temporais pontuais e disponibilização via HTTP Range Requests.
* **Agregado Raiz:** `ProdutoInSAR`.
* **Entidades / Objetos de Valor:** `SerieTemporal`, `ObservacaoDeslocamento`, `MapaVelocidade`, `MetadadosSTAC`, `CaminhoArmazenamentoS3`.
* **Eventos Emitidos:**
  * `ProdutoInSARDisponibilizadoEvent`
  * `SerieTemporalExtraidaEvent`

---

### 2.5. Contexto de Avaliação de Qualidade e Geodésia (Quality & Geodetic Evaluation Context)
* **Classificação DDD:** Domínio Central (*Core Domain*).
* **Responsabilidade Única:** Avaliar a confiabilidade científica dos produtos InSAR, calculando métricas de coerência média, aplicando máscaras booleanas de descorrelação temporal e inspecionando resíduos de desdobramento de fase (*phase unwrapping*).
* **Agregado Raiz:** `AvaliacaoQualidade`.
* **Objetos de Valor:** `IndiceCoerencia`, `PercentualResiduoUnwrapping`, `MascaraConfiabilidade`, `FlagAdvertenciaTecnica`.
* **Eventos Emitidos:**
  * `QualidadeAvaliadaEvent`
  * `IncertezaGeodesicaIdentificadaEvent`

---

### 2.6. Contexto Especializado de Subsidência Urbana (Urban Subsidence Analysis Context)
* **Classificação DDD:** Domínio Especializado de Negócio (*Radar++ Urban*).
* **Responsabilidade Única:** Análise retrospectiva (*back-analysis*), comparação temporal de cenários, delimitação de bacias de abatimento sobre malha urbana, perfis transversais de subsidência e geração de laudos técnicos de apoio pericial.
* **Agregado Raiz:** `EstudoSubsidenciaUrbana`.
* **Entidades / Objetos de Valor:** `BaciaAbatimento`, `PerfilTransversal`, `LaudoSinteticoApoio`, `JanelaRetrospectiva`.
* **Relação com o Core:** Consumidor (*Customer*) dos produtos InSAR disponibilizados pelo contexto de Produtos.

---

### 2.7. Contexto de Corredores de Infraestrutura (Linear Infrastructure Context — Provisório / Fase 3)
* **Classificação DDD:** Domínio Especializado de Negócio (*Radar++ Infra — Pós-MVP*).
* **Responsabilidade Única:** Gerenciamento de eixos viários, fatiamento por *buffers* estreitos, projeção em estaqueamento linear (km), associação de terraplenos e OAEs do SGTEC e triagem preventiva de taludes com rastejo lento (*creep*).
* **Agregado Raiz:** `CorredorLinearMonitorado`.
* **Entidades / Objetos de Valor:** `EixoViario`, `EstaqueamentoKm`, `AtivoGeotecnico`, `TaludeRisco`.
* **Status:** **Contexto Provisório / Congelado até Validação das Entrevistas D1-04**.

---

## 3. Matriz de Relacionamento entre Contextos (*Context Map*)

```
+----------------------------------------------------------------------------------------------------------------------------------------------------+
|                                    MATRIZ DE PADRÕES DE INTEGRAÇÃO ENTRE CONTEXTOS DELIMITADOS                                                    |
+--------------------------+--------------------------+-----------------------+----------------------------------------------------------------------+
| Contexto Origem (Upstr.) | Contexto Destino (Downs.)| Padrão de Integração  | Descrição da Fronteira Arquitetural                                  |
+--------------------------+--------------------------+-----------------------+----------------------------------------------------------------------+
| Catálogo SAR Externo     | Catálogo SAR Radar++     | Anti-Corruption Layer | A ACL traduz JSONs da NASA/ESA para o modelo `CenaSAR` do Radar++.   |
+--------------------------+--------------------------+-----------------------+----------------------------------------------------------------------+
| Gestão de AOI            | Catálogo SAR             | Customer / Supplier   | Catálogo consome a geometria da AOI para filtrar buscas orbitais.    |
+--------------------------+--------------------------+-----------------------+----------------------------------------------------------------------+
| Gestão de AOI            | Orquestração             | Shared Kernel (Leve)  | Compartilham identificador e BBOX da AOI de forma desacoplada.       |
+--------------------------+--------------------------+-----------------------+----------------------------------------------------------------------+
| Catálogo SAR             | Orquestração             | Published Language    | Orquestração consome lista padronizada de cenas candidatas.          |
+--------------------------+--------------------------+-----------------------+----------------------------------------------------------------------+
| Orquestração             | Produtos InSAR           | Event-Driven (Mensag.)| Fim do processamento dispara ingestão e conversão para COG no S3.    |
+--------------------------+--------------------------+-----------------------+----------------------------------------------------------------------+
| Produtos InSAR           | Avaliação de Qualidade   | Partner / Co-Evolução | Cálculo imediato de métricas de coerência e máscara de resíduos.     |
+--------------------------+--------------------------+-----------------------+----------------------------------------------------------------------+
| Produtos InSAR           | Subsidência Urbana       | Open Host Service     | Módulo Urban consome COGs e séries temporais via APIs padronizadas.  |
+--------------------------+--------------------------+-----------------------+----------------------------------------------------------------------+
```
