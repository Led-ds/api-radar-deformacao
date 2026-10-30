# Radar++ — Domínio: 05-fluxos-conceituais.md
## Fluxos de Ciclo de Vida, Sequenciamento de Domínio e Máquinas de Estados

**Documento:** `05-fluxos-conceituais.md`  
**Ciclo:** Ciclo 2 — Linguagem Ubíqua e Modelo Conceitual do Domínio  
**Data:** 24 de Outubro de 2026  
**Status:** Oficial — Entregue para Homologação de Domínio  
**Responsável:** Arquiteto de Software, Especialista em DDD, Modelador de Domínios Geoespaciais  
**Repositório Oficial:** `api-radar-deformacao` (caminho: `docs/domain/05-fluxos-conceituais.md`)  
**Regra de Governança:** Representar as transições e relacionamentos estritamente no nível de domínio e negócios. **Não definir contratos de API REST, controladores Spring Boot nem mapeamentos ORM nesta etapa.**

---

## 1. Visão Macro do Encadeamento Conceitual Ponta a Ponta

O fluxo de valor do Radar++ organiza-se na seguinte sequência fundamental de transformações conceituais:

$$\text{Área de Interesse} \longrightarrow \text{Descoberta de Cenas} \longrightarrow \text{Seleção de Pares} \longrightarrow \text{Solicitação} \longrightarrow \text{Job} \longrightarrow \text{Produtos InSAR} \longrightarrow \text{Avaliação de Qualidade} \longrightarrow \text{Observações / Séries}$$

```mermaid
flowchart TD
    subgraph "1. Espacialização"
        A["Área de Interesse (AOI)<br/>(Polígono/BBOX validado)"]
    end

    subgraph "2. Aquisição de Dados"
        B["Descoberta no Catálogo SAR<br/>(Busca ASF Vertex / Copernicus)"]
        C["Filtragem Homogênea<br/>(Mesmo Path, IW, VV, Direção)"]
    end

    subgraph "3. Orquestração e Cálculo"
        D["Seleção de Pares Interferométricos<br/>(Grafo SBAS com B_perp e Delta_t)"]
        E["Solicitação de Processamento<br/>(Autorização de negócio e parâmetros)"]
        F["Job de Processamento Assíncrono<br/>(HyP3 / Inversão MintPy em nuvem)"]
    end

    subgraph "4. Ingestão e Validação"
        G["Produtos InSAR Matriciais<br/>(Interferogramas desdobrados e Velocidade)"]
        H["Avaliação de Qualidade Geodésica<br/>(Coerência média e resíduos unwrapping)"]
        I["Formatação em Cloud Optimized GeoTIFF (COG)<br/>(Gravação persistente no storage S3)"]
    end

    subgraph "5. Entrega de Valor"
        J["Observações Pontuais e Séries Temporais<br/>(Extração d_LOS em mm ao longo do tempo)"]
        K["Laudo Sintético de Apoio Pericial<br/>(Relatório técnico auditável para engenharia)"]
    end

    A --> B --> C --> D --> E --> F --> G --> H --> I --> J --> K
```

---

## 2. Diagrama de Estados do Job de Processamento (*Processing Job Lifecycle*)

O ciclo de vida operacional do `JobProcessamento` é modelado como uma **Máquina de Estados Finitos Unidirecional**, garantindo previsibilidade assíncrona e rastreabilidade total:

```mermaid
stateDiagram-v2
    [*] --> SUBMETIDO: Usuário confirma Solicitação

    SUBMETIDO --> VALIDANDO_PARAMETROS: Início da orquestração interna
    
    VALIDANDO_PARAMETROS --> REJEITADO: Falha na validação topológica ou parâmetros inválidos
    REJEITADO --> [*]

    VALIDANDO_PARAMETROS --> AGUARDANDO_INSUMOS: Cenas validadas e grafo SBAS conexo

    AGUARDANDO_INSUMOS --> EM_PROCESSAMENTO_ORBITAL: Despacho assíncrono para motor (HyP3/Nuvem)
    
    EM_PROCESSAMENTO_ORBITAL --> RETENTANDO: Timeout ou erro transitório de infraestrutura
    RETENTANDO --> EM_PROCESSAMENTO_ORBITAL: Tentativa < 3
    RETENTANDO --> FALHO: Esgotadas as tentativas

    EM_PROCESSAMENTO_ORBITAL --> EM_INVERSAO_TEMPORAL: Interferogramas concluídos, iniciando MintPy
    
    EM_INVERSAO_TEMPORAL --> AVALIANDO_QUALIDADE: Inversão concluída, calculando resíduos e coerência

    AVALIANDO_QUALIDADE --> CONCLUIDO_CONFORME: Resíduos <= limiar e coerência aceitável
    AVALIANDO_QUALIDADE --> CONCLUIDO_COM_ADVERTENCIA: Resíduos > limiar ou descorrelação parcial

    CONCLUIDO_CONFORME --> GERANDO_PRODUTOS_COG: Ingestão de rasters
    CONCLUIDO_COM_ADVERTENCIA --> GERANDO_PRODUTOS_COG: Ingestão de rasters com flag de alerta

    GERANDO_PRODUTOS_COG --> DISPONIVEL: COGs gravados no S3 e metadados no histórico
    DISPONIVEL --> [*]

    FALHO --> [*]: Notificação de erro emitida
```

---

## 3. Diagrama de Sequência Conceitual entre Contextos

O diagrama a seguir detalha a interação desacoplada entre os quatro contextos simplificados estabelecidos no documento [03-subdominios-e-contextos-candidatos.md](file:///home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao/docs/domain/03-subdominios-e-contextos-candidatos.md):

```mermaid
sequenceDiagram
    autonumber
    actor Eng as Engenheiro Geotécnico / Analista SIG
    participant Terr as Contexto Gestão Territorial (AOI)
    participant Cat as Contexto Catálogo SAR (com ACL)
    participant Core as Contexto Motor InSAR Core
    participant S3 as Storage S3 (Bucket de COGs)
    participant Urban as Contexto Análise Urbana

    Eng->>Terr: 1. Cadastra/Desenha Área de Interesse (AOI)
    Terr->>Terr: Valida topologia e calcula área métrica
    Terr-->>Eng: Confirma AOI com identificador exclusivo

    Eng->>Cat: 2. Solicita descoberta de cenas disponíveis para a AOI
    Cat->>Cat: Consulta acervo externo via Camada Anti-Corrupção (ACL)
    Cat-->>Eng: Exibe linha do tempo de cenas compatíveis (mesmo Path/IW)

    Eng->>Core: 3. Submete Solicitação de Processamento (intervalo e parâmetros)
    Core->>Core: Cria agregado JobProcessamento (Status: SUBMETIDO)
    Core-->>Eng: Retorna JobId (HTTP 202 Accepted)

    Note over Core: Processamento assíncrono não-bloqueante
    Core->>Core: Monta rede interferométrica SBAS (B_perp, Delta_t)
    Core->>Core: Orquestra desdobramento de fase e inversão MintPy
    Core->>Core: Executa Avaliação de Qualidade (coerência, resíduos)
    Core->>S3: Grava rasters geodésicos formatados em COG
    Core->>Core: Transiciona Job para DISPONÍVEL e registra Histórico

    Eng->>Urban: 4. Acessa Estudo de Subsidência Urbana da AOI
    Urban->>Core: Requisita mapa de velocidade e séries temporais
    Urban->>S3: Carrega fatias de visualização via HTTP Range Requests
    Urban-->>Eng: Renderiza mapa de calor de subsidência e curvas temporais
    Eng->>Urban: 5. Solicita geração de Laudo Sintético de Apoio
    Urban-->>Eng: Emite laudo em PDF com aviso legal de isenção de ART
```

---

## 4. Fluxo de Extração de Observações e Séries Temporais pelo Usuário

Quando o engenheiro ou analista clica em um ponto específico do mapa ou importa uma lista de coordenadas de edifícios:

```mermaid
flowchart LR
    A["Coordenada (lat, lon) informada pelo usuário"] --> B["Verificação de Coerência Local (gamma)"]
    B --> C{"gamma >= Limiar Configurável?"}
    C -- "Sim" --> D["Extração da Série Temporal de d_LOS (mm) ao longo das datas"]
    C -- "Não" --> E["Aplicação de Máscara de Incerteza (Ponto Incoerente / Ruído)"]
    D --> F["Ajuste de Regressão Linear para Determinação da Taxa (mm/ano)"]
    F --> G["Exibição do Gráfico de Série Temporal com Incerteza Estimada"]
```

### Regras do Fluxo de Extração:
1. **Validação de Coerência no Ponto:** Se o pixel selecionado possuir coerência inferior ao limiar padrão configurado (ex.: $\bar{\gamma} < 0{,}40$), a interface deve exibir aviso explícito de que o ponto sofreu decorrelação temporal (ex.: área gramada ou copa de árvore);
2. **Exibição Obrigatória da Unidade Geodésica:** Os valores são sempre apresentados com a anotação $\text{mm}_{\text{LOS}}$, com tooltip explicativo informando a geometria orbital (Ascendente ou Descendente).
