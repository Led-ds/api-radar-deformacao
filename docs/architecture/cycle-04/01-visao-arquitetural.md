# Radar++ — Ciclo 4: Arquitetura Técnica, UML e Contratos
## Documento 01 — Visão Geral da Arquitetura e Drivers Técnicos

**Documento:** `01-visao-arquitetural.md`  
**Ciclo:** Ciclo 4 — Arquitetura Técnica, UML e Contratos  
**Data:** 24 de Outubro de 2026  
**Status:** Oficial — Entregue para Homologação de Arquitetura  
**Responsáveis:** Arquiteto de Software Sênior, Especialista em DDD, Arquiteto de Sistemas Distribuídos, Arquiteto Cloud  
**Repositório Oficial:** `api-radar-deformacao` (caminho: `docs/architecture/cycle-04/01-visao-arquitetural.md`)  
**Referências Cruzadas:**
- Homologação Técnica do Domínio: [11-relatorio-homologacao-tecnica.md](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/docs/domain/cycle-03/11-relatorio-homologacao-tecnica.md)
- Decisões de Plataforma e Persistência: [ADR-001](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/docs/05-architecture/adr/ADR-001-plataforma-persistencia.md)

---

### 1. Propósito e Missão da Arquitetura

O Radar++ tem como missão fornecer uma plataforma analítica, reprodutível e financeiramente sustentável para detecção e monitoramento de deformações de superfície e subsidência do terreno, utilizando sensoriamento remoto por Radar de Abertura Sintética Interferométrico (InSAR).

O objetivo deste documento e do **Ciclo 4** é traduzir o modelo de domínio conceitual consolidado no Ciclo 3 em uma **arquitetura de software concreta, pragmática, implementável e orientada a testes**, eliminando o acoplamento excessivo do protótipo legado e estabelecendo bases sólidas para a engenharia de implementação (Ciclo 5).

---

### 2. Princípios Arquiteturais Norteadores

A arquitetura do Radar++ fundamenta-se em dez princípios estritos:

1. **Monólito Modular em Java / Spring Boot 3.3+:** Todo o gerenciamento de regras de negócio, transações, governança territorial, catálogo e controle de acesso é unificado em um único executável backend estruturado em módulos independentes (`territory`, `catalog`, `insarcore`, `urbananalysis`). Evita-se a complexidade operacional, a latência de rede e os custos de infraestrutura de microsserviços distribuídos no estágio de MVP.
2. **Isolamento da Computação Científica em Python:** Os algoritmos geodésicos e bibliotecas de processamento interferométrico (GDAL, SNAP/ISCE2, MintPy, HyP3 SDK) operam em contêineres desacoplados, consumindo tarefas assíncronas e gravando produtos diretamente no storage de objetos.
3. **Persistência Híbrida e Especializada:** Metadados, geometrias vetoriais e logs auditáveis residem em banco relacional **PostgreSQL 16 com extensão PostGIS**, enquanto matrizes matriciais volumosas residem exclusivamente em **Amazon S3** em formatos otimizados para nuvem (Cloud Optimized GeoTIFF - COG, NetCDF/HDF5).
4. **Comunicação Assíncrona Não-Bloqueante:** Processamentos interferométricos duram de minutos a horas. A API HTTP é estritamente não-bloqueante, retornando `202 Accepted` com `Location` header e despachando tarefas assíncronas via fila orientada a banco (`SKIP LOCKED`) ou mensageria leve.
5. **Aplicação Pragmática de DDD e Arquitetura Hexagonal:** O núcleo de domínio de cada módulo é mantido livre de anotações de infraestrutura (Spring/JPA). A comunicação com banco de dados, provedores de satélite e storage ocorre através de portas (*Ports*) e adaptadores (*Adapters*).
6. **Entrega Confiável com Transactional Outbox:** Eventos de integração intercontextuais e gatilhos de jobs são persistidos na mesma transação atômica relacional dos agregados, prevenindo inconsistências causadas por *dual-write*.
7. **Grandeza Primária Soberana ($d_{\text{LOS}}$):** O sistema armazena e expõe primordialmente a velocidade e deslocamento na Linha de Visada do satélite em milímetros ($\text{mm}_{\text{LOS}}$). A projeção vertical ($d_{\text{vert}} = d_{\text{LOS}} / \cos\theta$) é tratada como transformação derivada condicional, explicitando suas hipóteses e incertezas.
8. **Desacoplamento entre Sucesso Técnico e Validade Científica:** A conclusão operacional de um contêiner de processamento (`PROCESSING_COMPLETED`) não implica aprovação científica. O produto exige homologação formal no portão de qualidade (`evaluateQuality()`).
9. **Segurança e Isolamento Lógico desde o Princípio:** Multi-tenancy é modelado de forma simples e segura via isolamento lógico ancorado no identificador organizacional (`OrganizationId`), com controle de acesso baseado em papéis (RBAC) e autenticação stateless (JWT).
10. **Sustentabilidade Financeira e Otimização de Custos:** A infraestrutura é dimensionada para rodar localmente com Docker Compose e em nuvem sob demanda com provisionamento efêmero de recursos computacionais, eliminando custos fixos desnecessários de clusters Kubernetes gerenciados.

---

### 3. Drivers Arquiteturais e Requisitos Não-Funcionais (NFRs)

```
+----------------------------------------------------------------------------------------------------------------------------------------------------+
|                                    DRIVERS ARQUITETURAIS E REQUISITOS NÃO-FUNCIONAIS DO RADAR++                                                     |
+---+----------------------------+---------------------------------------+---------------------------------------------------------------------------+
| # | Atributo de Qualidade      | Meta Mensurável / Especificação       | Mecanismo de Garantia Arquitetural                                        |
+---+----------------------------+---------------------------------------+---------------------------------------------------------------------------+
| 1 | **Desempenho de Leitura**  | Renderização de séries temporais em   | Leitura parcial de COG via HTTP Range Requests (fatia de bytes no S3),    |
|   |                            | menos de 2,0 s na UI web.             | indexação espacial GiST no PostGIS e índices B-Tree compostos.            |
+---+----------------------------+---------------------------------------+---------------------------------------------------------------------------+
| 2 | **Eficiência de Storage**  | Redução de até 80% do custo em S3     | Política de retenção em 3 níveis: expurgo automático de SLCs brutas       |
|   |                            | para séries históricas longas.        | (7-14 dias) e retenção permanente apenas de COGs e metadados JSON.        |
+---+----------------------------+---------------------------------------+---------------------------------------------------------------------------+
| 3 | **Confiabilidade de Tarefa**| 0% de perda de jobs em caso de queda  | Fila transacional no PostgreSQL (`FOR UPDATE SKIP LOCKED`) e padrão       |
|   |                            | de nós ou reinicialização do backend. | Transactional Outbox; retentativas com exponential backoff para rede.     |
+---+----------------------------+---------------------------------------+---------------------------------------------------------------------------+
| 4 | **Reprodutibilidade**      | 100% dos produtos vinculados à cadeia | Registro imutável de metadados: versão do software, DEM empregado,        |
|   |                            | determinística de proveniência.       | parâmetros de filtro, checksum SHA-256 e lista de granulações SLC.        |
+---+----------------------------+---------------------------------------+---------------------------------------------------------------------------+
| 5 | **Interoperabilidade SIG** | Aderência total aos padrões OGC       | Armazenamento de geometrias em WGS 84 (EPSG:4326), transformação sob      |
|   |                            | (WKT, GeoJSON, COG, EPSG).            | demanda para SIRGAS 2000 (EPSG:4674) e exportação em GeoTIFF/GeoJSON.     |
+---+----------------------------+---------------------------------------+---------------------------------------------------------------------------+
| 6 | **Segurança de Dados**     | Isolamento absoluto de dados entre    | Filtro mandatório de `organization_id` em todos os repositórios,         |
|   |                            | organizações clientes.                | URLs pré-assinadas temporárias (TTL <= 15 min) e tokens JWT assinados.    |
+---+----------------------------+---------------------------------------+---------------------------------------------------------------------------+
| 7 | **Auditabilidade Pericial**| Proibição de diagnósticos automáticos | Emissão de Notas Técnicas Exploratórias com ressalva formal de limites    |
|   |                            | e comprovação de integridade técnica. | metodológicos e exigência de ART presencial emitida por perito.           |
+---+----------------------------+---------------------------------------+---------------------------------------------------------------------------+
```

---

### 4. Topologia Geral do Sistema

A solução organiza-se em quatro camadas lógicas de execução:

```mermaid
graph TB
    subgraph "Camada de Apresentação e Geovisualização"
        SPA["Frontend SPA (React 18 + TypeScript + Vite)<br/>• Leaflet / OpenLayers para mapas<br/>• Recharts para séries temporais InSAR<br/>• TailwindCSS & ShadcnUI"]
    end

    subgraph "Camada de Negócio e Governança (Monólito Modular Spring Boot 3.3)"
        API["Controladores REST & Endpoints OpenAPI 3.0"]
        MOD_TERR["Módulo: territory (AOI, Geometria, Cotas)"]
        MOD_CAT["Módulo: catalog (Metadados SAR, Bursts, Pares)"]
        MOD_CORE["Módulo: insarcore (Jobs, Inversão, Qualidade, Produtos)"]
        MOD_URB["Módulo: urbananalysis (Investigações, Perfis, Notas Técnicas)"]
        OUTBOX["Tabela Transacional: outbox_events"]
    end

    subgraph "Camada de Processamento Científico Especializado (Python 3.11)"
        WORKER["Worker InSAR (Python / MintPy / GDAL)<br/>• Consumo assíncrono de jobs<br/>• Integração com ASF Search & HyP3 SDK<br/>• Inversão SBAS & Avaliação de Coerência<br/>• Formatação de Cloud Optimized GeoTIFF (COG)"]
    end

    subgraph "Camada de Persistência e Storage"
        DB[(PostgreSQL 16 + PostGIS<br/>• Schemas segregados<br/>• Índices GiST e B-Tree<br/>• Tabela de Filas e Outbox)]
        S3[(AWS S3 Privado<br/>• Camada 1: COG/NetCDF Permanentes<br/>• Camada 2: Interferogramas Intermediários<br/>• Camada 3: Cenas Efêmeras / Scratch)]
    end

    subgraph "Provedores Satelitais Externos"
        ASF["Alaska Satellite Facility (ASF Search API & HyP3)"]
        CDSE["Copernicus Data Space Ecosystem (ESA CDSE)"]
    end

    SPA <-->|HTTPS REST (JSON / Bearer JWT)| API
    SPA -->|HTTP Range Requests (Presigned S3 GET)| S3
    
    API --> MOD_TERR
    API --> MOD_CAT
    API --> MOD_CORE
    API --> MOD_URB
    
    MOD_TERR --> DB
    MOD_CAT --> DB
    MOD_CORE --> DB
    MOD_URB --> DB
    MOD_CORE --> OUTBOX

    OUTBOX -.->|Despacho Assíncrono| WORKER
    WORKER -->|Query Metadados & Submissão| ASF
    WORKER -->|Catálogo Sentinel-1| CDSE
    WORKER -->|Upload de Produtos COG| S3
    WORKER -->|Gravação de Resultados & Métricas| DB
```

---

### 5. Resumo das Decisões Estratégicas e Técnicas (ADRs Chave)

- **ADR-001 (Plataforma e Persistência):** Java/Spring Boot + Python assíncrono + PostgreSQL/PostGIS + S3 privado.
- **ADR-002 (Monólito Modular vs. Microsserviços):** Adoção de Monólito Modular com fronteiras DDD estritas por pacote; microsserviços rejeitados por complexidade desproporcional.
- **ADR-003 (Mecanismo de Fila Assíncrona no MVP):** Fila orientada a banco relacional com `FOR UPDATE SKIP LOCKED` nativo do PostgreSQL, dispensando broker RabbitMQ dedicado no início para minimizar custos e pontos de falha.
- **ADR-004 (Entrega de COGs e Acesso a Arquivos):** Distribuição de produtos raster diretamente do S3 privado via URLs pré-assinadas temporárias geradas pelo backend, permitindo leitura HTTP Range Request direta na interface.
- **ADR-005 (Sistemas de Coordenadas):** Armazenamento canônico em WGS 84 (`EPSG:4326`) para interoperabilidade web, transformação para projeções conformes UTM locais para cálculos métricos e suporte a SIRGAS 2000 (`EPSG:4674`) para conformidade legal brasileira.
- **ADR-006 (Gating de Qualidade Científica Desacoplado):** O status `PROCESSING_COMPLETED` do job de computação não autoriza visualização; o produto depende da avaliação expressa de coerência e fechamento de fase.
