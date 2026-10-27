# Radar++ — Fluxos de Negócio e Decisões Operacionais

**Documento:** `08-fluxos-negocio.md`  
**Ciclo:** Ciclo 1 — Visão do Produto e Definição do MVP  
**Data:** Outubro de 2026  
**Status:** Proposto para Aprovação  
**Autores:** Equipe de Arquitetura de Software e Engenharia de Produto  
**Referências:** [04-jornadas-usuario.md](file:///home/asoares/Documentos/repositories/repo_radar/docs/01-product/04-jornadas-usuario.md), [05-requisitos-funcionais.md](file:///home/asoares/Documentos/repositories/repo_radar/docs/01-product/05-requisitos-funcionais.md), [07-regras-negocio.md](file:///home/asoares/Documentos/repositories/repo_radar/docs/01-product/07-regras-negocio.md).

---

## 1. Visão Geral dos Fluxos de Negócio

Os fluxos de negócio modelam como os dados e as decisões transitam entre os atores e subsistemas do Radar++, detalhando ramificações de validação, tratamento de erros e pontos de parada controlados:

```
[Fluxo 1: AOI] ──► [Fluxo 2: Cenas & Par] ──► [Fluxo 3: Orquestração InSAR] ──► [Fluxo 4: Visualização]
```

---

## 2. Fluxo 1: Seleção e Cadastro de Área de Interesse (AOI)

```mermaid
flowchart TD
    Start([Início: Usuário no Mapa]) --> Draw[Desenha Polígono no Mapa Leaflet]
    Draw --> Name[Informa Nome Identificador da AOI]
    Name --> Submit[Clica em 'Salvar Área de Interesse']
    
    Submit --> ValTop{Geometria Válida?<br/>ST_IsValid}
    ValTop -- Não --> ErrTop[Erro 400: Polígono inválido<br/>Possui auto-interseção]
    ErrTop --> Draw
    
    ValTop -- Sim --> ValArea{Área <= 2.500 km²?<br/>RN-001}
    ValArea -- Não --> ErrArea[Erro 400: Área excede limite máximo<br/>Diminua o polígono]
    ErrArea --> Draw
    
    ValArea -- Sim --> SaveDB[Persiste AOI no PostgreSQL com PostGIS]
    SaveDB --> RenderLayer[Renderiza Polígono com Borda de Destaque]
    RenderLayer --> End([Fim do Fluxo 1: AOI Ativa])
```

---

## 3. Fluxo 2: Descoberta de Cenas e Seleção do Par Interferométrico

```mermaid
flowchart TD
    Start([Início: AOI Selecionada]) --> InputDate[Define Intervalo de Datas: Início / Fim]
    InputDate --> CallCat[API: Consulta Catálogo Sentinel-1 IW SLC]
    
    CallCat --> CheckCat{Retornou Cenas?<br/>ESA / ASF}
    CheckCat -- Não --> ZeroScenes[Exibe: Nenhuma cena encontrada no período]
    ZeroScenes --> SuggestDate[Sugere expandir intervalo temporal]
    SuggestDate --> InputDate
    
    CheckCat -- Sim --> GroupTrack[Agrupa Cenas por Track Orbital Relativo]
    GroupTrack --> UserSelect[Usuário Seleciona Cena Mestre e Cena Escravo]
    
    UserSelect --> CheckTrack{Mesmo Track e Direção?<br/>RN-003}
    CheckTrack -- Não --> ErrTrack[Alerta: Geometria incompatível<br/>Tracks distintos causam descorrelação]
    ErrTrack --> UserSelect
    
    CheckTrack -- Sim --> CalcBase[Calcula Linhas de Base: B_perp e B_t]
    CalcBase --> CheckBperp{B_perp <= 150m?<br/>RN-004}
    CheckBperp -- Não --> ErrBperp[Alerta: Linha de base perpendicular excessiva]
    ErrBperp --> UserSelect
    
    CheckBperp -- Sim --> CheckBt{6 <= B_t <= 48 dias?<br/>RN-005}
    CheckBt -- Não --> ErrBt[Alerta: Linha de base temporal fora da faixa recomendada]
    ErrBt --> UserSelect
    
    CheckBt -- Sim --> PairValid[Par Validado: Habilita Botão 'Iniciar Análise']
    PairValid --> End([Fim do Fluxo 2: Par Apto])
```

---

## 4. Fluxo 3: Orquestração e Execução Assíncrona do Pipeline InSAR

```mermaid
flowchart TD
    Start([Início: Disparo de Análise]) --> CheckActive{Existe Job Ativo para o mesmo par?<br/>RN-012}
    CheckActive -- Sim --> BlockDupl[Bloqueio: Job idêntico já em andamento]
    BlockDupl --> EndFail([Fim: Sem duplicação])
    
    CheckActive -- Não --> CreateJob[Gera Job no banco: Status = PENDING]
    CreateJob --> Return202[Retorna HTTP 202 Accepted para a UI]
    CreateJob --> EnqueueQueue[Publica na Fila de Mensageria Assíncrona]
    
    EnqueueQueue --> WorkerTake[Worker assume: Status = DOWNLOADING_SCENES]
    WorkerTake --> DownloadScenes[Download das 2 cenas SLC + Copernicus DEM 30m]
    
    DownloadScenes --> CheckDown{Download OK?}
    CheckDown -- Não --> RetryDown{Tentativas < 3?}
    RetryDown -- Sim --> DownloadScenes
    RetryDown -- Não --> FailDown[Status = FAILED: Falha na API do satélite]
    FailDown --> EndFail
    
    CheckDown -- Sim --> Coreg[Status = COREGISTERING: Corregistro Submétrico]
    Coreg --> CheckCoreg{Corregistro Convergiu?}
    CheckCoreg -- Não --> FailCoreg[Status = FAILED: Falha de corregistro]
    FailCoreg --> EndFail
    
    CheckCoreg -- Sim --> Interf[Status = INTERFEROGRAM: Subtrai DEM e Filtra Goldstein]
    Interf --> Unwrapping[Status = PHASE_UNWRAPPING: Executa SNAPHU 2D]
    
    Unwrapping --> CheckUnwrap{SNAPHU convergiu?}
    CheckUnwrap -- Não --> FailUnwrap[Status = FAILED: Falha no desembrulhamento de fase]
    FailUnwrap --> EndFail
    
    CheckUnwrap -- Sim --> CheckCoh{Coerência Média >= 0.20?<br/>RN-007}
    CheckCoh -- Não --> FailCoh[Status = FAILED: Coerência muito baixa na AOI]
    FailCoh --> EndFail
    
    CheckCoh -- Sim --> Geocode[Status = GEOCODING: Converte Fase para LOS mm em EPSG:4326]
    Geocode --> ExportCOG[Status = EXPORTING_COG: Gera Cloud Optimized GeoTIFFs]
    ExportCOG --> UploadS3[Upload de rasters no Bucket S3 Privado]
    UploadS3 --> PurgeTemp[RN-010: Exclui cenas SLC temporárias do disco]
    PurgeTemp --> MarkDone[Status = COMPLETED: Grava URIs e estatísticas no banco]
    MarkDone --> EndSuccess([Fim do Fluxo 3: Sucesso])
```

---

## 5. Fluxo 4: Visualização Geoespacial e Inspeção de Qualidade

```mermaid
flowchart TD
    Start([Início: Job COMPLETED]) --> UserClick[Usuário Clica em 'Visualizar Resultados']
    UserClick --> RequestURLs[UI solicita presigned URLs ao backend]
    RequestURLs --> BackendS3[Backend gera URLs temporárias S3 de 60 min<br/>RN-011]
    BackendS3 --> StreamCOG[Frontend realiza HTTP Range Requests dos COGs]
    
    StreamCOG --> RenderMap[Renderiza Camada de Deslocamento LOS sobre o Mapa]
    RenderMap --> UserInspect{Usuário Clica em um Ponto do Terreno?}
    
    UserInspect -- Não --> UserLayerCtrl{Usuário ajusta opacidade ou troca camada?}
    UserLayerCtrl -- Sim --> AdjustView[Atualiza transparência ou exibe Camada de Coerência]
    AdjustView --> UserInspect
    
    UserInspect -- Sim --> ExtractPixel[Lê valores numéricos exatos da coordenada Lat/Lon]
    ExtractPixel --> ReadValues[Deslocamento d_LOS em mm e Coerência gamma]
    
    ReadValues --> CheckQuality{gamma >= 0.30?<br/>RN-007}
    CheckQuality -- Sim --> PopupHigh[Exibe Popup:<br/>Deslocamento: X mm<br/>Coerência: Y - Confiável]
    CheckQuality -- Não --> PopupLow[Exibe Popup com AVISO:<br/>Deslocamento: X mm<br/>Coerência: Y - Baixa Confiabilidade / Ruído]
    
    PopupHigh --> DownloadOpt{Usuário clica em 'Baixar Produtos'?}
    PopupLow --> DownloadOpt
    
    DownloadOpt -- Sim --> GenZip[Backend compacta GeoTIFFs + metadata.json]
    GenZip --> DownloadFile[Download do arquivo ZIP iniciado]
    DownloadOpt -- Não --> EndView([Fim do Fluxo 4: Inspeção Concluída])
    DownloadFile --> EndView
```
