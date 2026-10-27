# Radar++ — Reconstrução da Arquitetura Atual e Fluxos do Sistema

**Documento:** `06-arquitetura-atual.md`  
**Data da Auditoria:** Outubro de 2026  
**Status:** Concluído  
**Escopo:** Reconstrução visual e descritiva da arquitetura real do Radar++, baseando-se estritamente nas implementações existentes.

---

## 1. Diagrama de Contexto do Sistema (C4 — Nível 1)

O diagrama abaixo ilustra o contexto operacional do Radar++, seus usuários, os subsistemas e as fronteiras com provedores externos:

```mermaid
C4Context
    title Diagrama de Contexto do Sistema - Radar++ (Estado Atual)

    Person(user, "Usuário / Analista", "Interage com o mapa web, clica em pontos e submete dados de deformação.")

    System(radar_frontend, "Radar++ Frontend (React)", "SPA em React 18 / Leaflet para visualização de mapa, marcadores e submissão.")
    System(radar_backend, "Radar++ Backend (Spring Boot)", "API REST Java 17 que orquestra chamadas, persistência e integração externa.")
    System(radar_python, "Radar++ Microsserviço Python", "API FastAPI para redimensionamento de imagem e inferência Keras.")

    System_Ext(nasa_api, "NASA Open Data API", "Endpoint /planetary/earth/assets (fotos ópticas Landsat).")
    System_Ext(aws_s3, "Amazon Web Services (S3)", "Bucket radar-imagesbucket para armazenamento de objetos PNG/GeoTIFF.")
    System_Ext(postgres_db, "Banco de Dados PostgreSQL", "Persistência relacional de entidades e imagens via OID (@Lob).")
    System_Ext(osm_tiles, "OpenStreetMap", "Provedor público de tiles para o mapa Leaflet.")

    Rel(user, radar_frontend, "Acessa via navegador", "HTTPS / HTTP:8080/8081")
    Rel(radar_frontend, osm_tiles, "Carrega tiles cartográficos", "HTTP GET")
    Rel(radar_frontend, radar_backend, "Envia coordenadas e consulta pontos", "HTTP REST /api")
    Rel(radar_backend, postgres_db, "Grava/Lê deformações e dados binários OID", "JDBC / 5432")
    Rel(radar_backend, aws_s3, "Upload e Download de imagens", "AWS SDK v2 S3")
    Rel(radar_backend, nasa_api, "Busca ativos de imagens ópticas da Terra", "HTTPS GET")
    Rel(radar_backend, radar_python, "Envia imagem processada para análise", "HTTP POST /analyze/")
```

---

## 2. Diagrama de Contêineres e Componentes (C4 — Nível 2 / 3)

Detalhamento dos componentes internos e suas conexões técnicas:

```mermaid
graph TB
    subgraph Frontend_React ["Frontend Web (Vite + React)"]
        UI_Map["MonitoringMap.tsx<br/>(Leaflet Map + Click Handler)"]
        UI_Form["Formulário de Coordenadas<br/>(Cálculo Haversine embutido)"]
        UI_Viewer["ImageViewer.tsx<br/>(Dialog modal de zoom)"]
        Service_Map["mapService.ts<br/>(HTTP fetcher)"]
        UI_Map --> UI_Form
        UI_Map --> UI_Viewer
        UI_Map --> Service_Map
    end

    subgraph Backend_Java ["Backend Java (Spring Boot 3.3.4)"]
        Controller_Def["DeformacaoController<br/>(/api/analise)"]
        Controller_Img["ImagemRadarController<br/>(/api/imagens)"]
        
        Service_Def["DeformacaoService"]
        Service_Ana["AnaliseService"]
        Service_Proc["ProcessamentoService"]
        Service_Radar["ImagemRadarService"]
        Service_Storage["StorageService"]
        Service_Image["ImageService"]
        
        Comp_Geo["GeoToolsProcessor<br/>(Envelope + JAI Convolve)"]
        
        Client_NASA["NasaOpenDataClient<br/>(OpenFeign)"]
        Client_Py["FastAPIClientPy<br/>(OpenFeign)"]
        Client_S3["S3AsyncClient & S3Client<br/>(AWS SDK)"]
        
        Repo_Def["DeformacaoRepository"]
        Repo_Radar["ImagemRadarRepository"]
        Repo_Ana["AnaliseResultadoRepository"]

        Controller_Def --> Service_Def
        Controller_Img --> Service_Radar
        Controller_Img --> Service_Storage
        
        Service_Def --> Service_Ana
        Service_Ana --> Client_NASA
        Service_Ana --> Service_Radar
        Service_Ana --> Service_Proc
        Service_Ana --> Repo_Def
        
        Service_Proc --> Comp_Geo
        Service_Proc --> Service_Radar
        Service_Proc --> Service_Image
        Service_Proc --> Client_Py
        Service_Proc --> Repo_Ana
        
        Service_Radar --> Service_Storage
        Service_Radar --> Repo_Radar
        Service_Storage --> Client_S3
    end

    subgraph Service_Python ["Microsserviço Python (FastAPI 0.x)"]
        Route_Ana["analyze.py<br/>(/analyze/)"]
        Model_Keras["meu_modelo.h5<br/>(CNN Keras)"]
        Preproc_PIL["preprocess_image<br/>(PIL Resize 64x64)"]
        
        Route_Ana --> Preproc_PIL
        Route_Ana --> Model_Keras
    end

    Service_Map -->|POST /analise/deformacao| Controller_Def
    Service_Map -->|GET /analise/deformacoes| Controller_Def
    Service_Map -->|GET /imagens/download/nome/*| Controller_Img
    
    Client_Py -->|POST multipart /analyze/| Route_Ana
```

---

## 3. Diagrama do Fluxo de Requisições de Ponta a Ponta

Rastreamento sequencial do ciclo de registro de uma deformação:

```mermaid
sequenceDiagram
    autonumber
    actor User as Usuário
    participant Front as Frontend (React)
    participant Back as Backend (Spring Boot)
    participant NASA as NASA Open Data API
    participant S3 as AWS S3 Bucket
    participant GeoTools as GeoToolsProcessor (Java)
    participant Py as FastAPI (Python)
    participant DB as PostgreSQL

    User->>Front: Clica no mapa em coordenada B
    Note over Front: Calcula Haversine entre B e ponto anterior A<br/>Multiplica km por 100.000 para gerar "cm"
    User->>Front: Clica no botão "Enviar"
    Front->>Back: POST /api/analise/deformacao (lat, lon, deslocamento)
    
    Back->>DB: INSERT into deformacao (status preliminar)
    Back->>NASA: GET /planetary/earth/assets?lat=...&lon=...
    alt Sucesso na API NASA
        NASA-->>Back: JSON com URL da imagem óptica
        Back->>NASA: Download do stream de bytes (HTTP GET)
        NASA-->>Back: Bytes da imagem PNG/JPG
    else Falha ou Sem Dados na NASA
        Note over Back: Interrupção: grava mensagem de "dados insuficientes"
    end

    Back->>S3: PutObject: imagens/deformacao-{id}/nasa_original.png
    Back->>DB: INSERT into imagem_radar (origem=NASA, bytes OID)
    
    Back->>GeoTools: processar(imagemBytes, deformacao)
    Note over GeoTools: Cria envelope em EPSG:4326 usando deslocamento<br/>Aplica filtro de convolução JAI<br/>Serializa como GeoTIFF
    GeoTools-->>Back: Bytes da imagem GeoTIFF
    
    Back->>S3: PutObject: imagens/deformacao-{id}/fastapi_processada.png
    Back->>DB: INSERT into imagem_radar (origem=FASTAPI, bytes OID)
    
    Back->>Py: POST /analyze/ (multipart: fastapi_processada.png)
    Note over Py: PIL abre e redimensiona para 64x64<br/>meu_modelo.h5 executa inferência (dados de ruído)<br/>Retorna "Stable" ou "Unstable"
    Py-->>Back: JSON: {"result": "Stable", "confidence": 0.81}
    
    Back->>DB: INSERT into analise_resultado (classificacao, confianca)
    Back->>DB: UPDATE deformacao SET risco = 'Stable'
    Back-->>Front: 200 OK (DeformacaoResponseDto)
    
    Note over Front: Executa window.location.reload()
    Front->>Back: GET /api/analise/deformacoes
    Back-->>Front: Lista de Deformações cadastradas
    
    loop Para cada deformação
        Front->>Back: GET /api/imagens/download/nome/nasa_original.png
        Note over Back: StorageService busca FIXO imagens/deformacao-68/nasa_original.png
        Back->>S3: GetObject (deformacao-68)
        S3-->>Back: Bytes da imagem
        Back-->>Front: Image Blob
        
        Front->>Back: GET /api/imagens/download/nome/fastapi_processada.png
        Note over Back: StorageService busca FIXO imagens/deformacao-68/fastapi_processada.png
        Back->>S3: GetObject (deformacao-68)
        S3-->>Back: Bytes da imagem
        Back-->>Front: Image Blob
    end
    
    Front-->>User: Renderiza marcadores com fotos da pasta 68 e popup
```

---

## 4. Diagrama do Processamento de Imagens e Pipeline de Transformação

```mermaid
flowchart TD
    Start([Início: Requisição de Cadastro]) --> CallNASA[Chamada HTTP GET para NASA Open Data]
    CallNASA --> CheckNASA{NASA retornou URL?}
    
    CheckNASA -- Não --> ReturnInsuff[Risco = 'Dados insuficientes'<br/>Finaliza sem processamento]
    CheckNASA -- Sim --> DownloadBytes[Download dos bytes ópticos da NASA]
    
    DownloadBytes --> SaveRawDB[Salva no Postgres: imagem_radar.dadosBrutos OID]
    DownloadBytes --> UploadRawS3[Upload no S3: deformacao-id/nasa_original.png]
    
    DownloadBytes --> GeoTools_In[Entrada em GeoToolsProcessor]
    GeoTools_In --> GeoEnvelope[Calcula Bounding Box usando 'deslocamento' como raio]
    GeoEnvelope --> GridCoverage[Cria GridCoverage2D com EPSG:4326]
    GridCoverage --> Resample[Reprojeção para CRS alvo]
    Resample --> JAI_Filter[Filtro JAI: Kernel Convolve 3x3]
    JAI_Filter --> GeoTiff_Out[GeoTiffWriter: exporta bytes GeoTIFF LZW]
    
    GeoTiff_Out --> SaveProcDB[Salva no Postgres: imagem_radar com origem FASTAPI]
    GeoTiff_Out --> UploadProcS3[Upload no S3: deformacao-id/fastapi_processada.png]
    
    GeoTiff_Out --> WrapMultipart[ImageService: encapsula GeoTIFF como MultipartFile com nome .png]
    WrapMultipart --> CallFastAPI[OpenFeign: POST http://localhost:8082/analyze/]
    
    CallFastAPI --> PIL_Open[PIL Image.open decodifica stream]
    PIL_Open --> PIL_Resize[Redimensiona para 64x64 pixels]
    PIL_Resize --> Norm_Array[Normaliza matriz / 255.0]
    Norm_Array --> Keras_Infer[Keras meu_modelo.h5 predict]
    Keras_Infer --> Pred_Check{Predição > 0.5?}
    
    Pred_Check -- Sim --> StableResult[Resultado: 'Stable']
    Pred_Check -- Não --> UnstableResult[Resultado: 'Unstable']
    
    StableResult --> RespJSON[Devolve JSON com result e confidence]
    UnstableResult --> RespJSON
    
    RespJSON --> SaveResultDB[Salva em analise_resultado]
    SaveResultDB --> UpdateDefRisco[Atualiza campo risco na tabela deformacao]
    UpdateDefRisco --> End([Fim do Pipeline])
```

---

## 5. Mapeamento de Interrupções e Gargalos nos Fluxos

A análise arquitetural identificou quatro pontos críticos de ruptura onde o fluxo pretendido é quebrado ou substituído por atalhos artificiais:

```
[Usuário / Mapa] 
      │ 
      ▼ (1. Ruptura: Deslocamento calculado por distância euclidiana de clique)
[Frontend React] 
      │ 
      ▼ (2. Ruptura: CORS restrito a porta 8081 / Invocação síncrona bloqueante)
[Backend Spring Boot] 
      │ 
      ├─► [NASA API] (3. Ruptura: Imagem óptica Landsat, não radar SAR; falhas frequentes)
      │ 
      ├─► [Python FastAPI] (4. Ruptura: Modelo treinado em ruído aleatório uniforme)
      │ 
      └─► [AWS S3 Download] (5. Ruptura: Chave fixada na pasta deformacao-68)
```

1. **Ruptura 1 (Entrada de Dados):** Não há ingestão de parâmetros de missão de radar (passagem ascendente/descendente, ângulo de incidência, linha de base temporal/perpendicular). A entrada é baseada exclusivamente na distância de clique do mouse na tela.
2. **Ruptura 2 (Orquestração Síncrona Monolítica):** Toda a cadeia (NASA → Download → JAI → S3 → FastAPI → Banco) roda em uma única thread HTTP síncrona. Falhas de rede ou timeout quebram toda a transação.
3. **Ruptura 3 (Fonte Satelital):** Não há ingestão de dados SAR reais da ESA Copernicus ou NASA/ASF. A fonte externa é a API de fotografias da Terra da NASA.
4. **Ruptura 4 (Processamento Científico Inexistente):** Não há geração de interferogramas nem extração de fase. A análise é simulada por uma CNN alimentada por ruído estático.
5. **Ruptura 5 (Recuperação de Imagens):** A visualização no mapa é incapaz de exibir o histórico dinâmico real porque o código faz download forçado dos arquivos da pasta `deformacao-68`.
