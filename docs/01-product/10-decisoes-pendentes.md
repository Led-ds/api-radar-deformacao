# Radar++ — Registro de Decisões Pendentes e Hipóteses em Aberto

**Documento:** `10-decisoes-pendentes.md`  
**Ciclo:** Ciclo 1 — Visão do Produto e Definição do MVP  
**Data:** Outubro de 2026  
**Status:** Proposto para Aprovação  
**Autores:** Equipe de Arquitetura de Software e Engenharia de Produto  
**Referências:** [01-visao-produto.md](file:///home/asoares/Documentos/repositories/repo_radar/docs/01-product/01-visao-produto.md), [03-escopo-mvp.md](file:///home/asoares/Documentos/repositories/repo_radar/docs/01-product/03-escopo-mvp.md), [06-requisitos-nao-funcionais.md](file:///home/asoares/Documentos/repositories/repo_radar/docs/01-product/06-requisitos-nao-funcionais.md).

---

## 1. Visão Geral das Decisões Pendentes

O planejamento do Radar++ adota o princípio de **arquitetura evolucionária**: decisões fundamentais são tomadas no momento adequado (*Last Responsible Moment*), com base em evidências experimentais e alinhamento de governança de produto.

Este documento cataloga as decisões técnicas e de negócio que permanecem como **hipóteses abertas** a serem deliberadas e validadas antes do início da implementação dos ciclos subsequentes:

```
+-----------------------------------------------------------------------------------+
|                        PAINEL DE DECISÕES PENDENTES (DP)                          |
+---------+----------------------------------------------+------------+-------------+
| ID      | Título da Decisão Pendente                   | Ciclo Alvo | Impacto     |
+---------+----------------------------------------------+------------+-------------+
| DP-01   | Motor de Processamento InSAR para o MVP      | Ciclo 2    | Crítico     |
| DP-02   | Estratégia de Renderização de Raster na Web  | Ciclo 2    | Alto        |
| DP-03   | Arquitetura de Autenticação e Gestão de Usuário| Ciclo 2  | Médio       |
| DP-04   | Protocolo de Telemetria de Jobs no Frontend  | Ciclo 2    | Médio       |
| DP-05   | Estratégia de Staging de Cenas Brutas SLC    | Ciclo 3    | Alto        |
| DP-06   | Seleção da Área Piloto de Validação em Solo  | Ciclo 4    | Alto        |
+---------+----------------------------------------------+------------+-------------+
```

---

## 2. Detalhamento das Decisões Pendentes

### DP-01 — Escolha do Motor de Processamento InSAR para o MVP
* **Contexto:** O Radar++ necessita de um mecanismo confiável para executar o pipeline DInSAR (corregistro, subtração de fase topográfica com DEM e unwrapping via SNAPHU).
* **Alternativas em Avaliação:**
  1. **Alternativa A — API HyP3 da NASA/ASF (Nuvem Gerenciada):** Utilizar a API pública gratuita do Alaska Satellite Facility (*Hybrid Pluggable Processing Pipeline*), que recebe os IDs das duas cenas Sentinel-1 e devolve o produto interferométrico DInSAR completo e geocodificado em COG na nuvem.
     * *Prós:* Custo zero de servidores no MVP, zero complexidade de compilar bibliotecas C++ geoespaciais pesadas, altíssima confiabilidade metodológica (mantido pela NASA).
     * *Contras:* Dependência externa dos servidores da NASA/ASF e limites de cotas de submissão por usuário.
  2. **Alternativa B — Worker Local com SNAP GPT em Docker:** Montar um worker Python executando o SNAP Graph Processing Tool (GPT) da ESA em contêiner Docker.
     * *Prós:* Independência de serviços externos; controle total dos parâmetros de filtragem e janelas de coerência.
     * *Contras:* Exige máquinas virtuais com no mínimo 16 GB a 32 GB de RAM e alto custo de CPU; tempo de desenvolvimento inicial mais lento.
  3. **Alternativa C — Pipeline com ISCE2 / MintPy em Python:** Compilar o motor ISCE2 da NASA/Caltech em contêiner.
     * *Prós:* Padrão ouro da geodésia para séries temporais (prepara o terreno para o pós-MVP).
     * *Contras:* Instalação e compilação extremamente complexas (múltiplas dependências legadas Fortran/C).
* **Recomendação Preliminar da Arquitetura:** Adotar a **Alternativa A (HyP3 API)** para o MVP, desenhando a camada de serviço com a interface `InSarProcessorPort` (Clean Architecture). Isso permite entregar o MVP em tempo recorde com custo de infraestrutura quase nulo, reservando a Alternativa B ou C para o momento de ganho de escala pós-MVP.
* **Ciclo de Resolução:** Ciclo 2 (Arquitetura e Fundações).

---

### DP-02 — Mecanismo de Renderização de Camadas Raster no Frontend
* **Contexto:** Os mapas de deslocamento e coerência gerados são rasters com milhares de pixels que precisam ser exibidos com fluidez sobre o Leaflet.
* **Alternativas em Avaliação:**
  1. **Alternativa A — COG Client-Side com WebGL (`geotiff.js` / Leaflet Canvas):** O navegador lê o cabeçalho do Cloud Optimized GeoTIFF via HTTP Range Request e faz a rasterização direta via canvas no cliente.
     * *Prós:* Não exige nenhum servidor adicional de mapas; arquitetura extremamente barata e simples.
     * *Contras:* Pode sofrer lentidão em computadores ou dispositivos com processador mais fraco se a AOI for muito extensa.
  2. **Alternativa B — Servidor de Tiles Dinâmico (Titiler / Rio-Tiler):** Subir um microsserviço leve em Python (Titiler) que converte o COG em tiles XYZ/TMS sob demanda e os entrega como imagens PNG leves para o Leaflet.
     * *Prós:* Altíssima performance no cliente; compatibilidade universal com qualquer navegador e dispositivo móvel.
     * *Contras:* Mais um serviço para orquestrar e manter rodando.
* **Recomendação Preliminar:** Avaliar a **Alternativa A (geotiff.js)** para AOIs de até $2.500 \text{ km}^2$. Se os testes de renderização indicarem queda de FPS abaixo de 30 no pan/zoom, migrar para a Alternativa B.
* **Ciclo de Resolução:** Ciclo 2 / Ciclo 5.

---

### DP-03 — Arquitetura de Autenticação e Gestão de Usuário
* **Contexto:** A auditoria identificou ausência total de segurança nas rotas. Precisamos definir o escopo de segurança para o MVP.
* **Alternativas em Avaliação:**
  1. **Alternativa A — Spring Security Embutido com JWT Simples:** Autenticação por usuário e senha gerenciada pelo próprio backend, emitindo tokens JWT assinados.
     * *Prós:* Simples, sem componentes externos, rápido de implementar no Monólito Modular.
     * *Contras:* Não suporta SSO corporativo nativo sem desenvolvimento extra.
  2. **Alternativa B — Servidor IAM Dedicado (Keycloak ou Auth0):** Delegar a autenticação para um provedor OpenID Connect (OIDC).
     * *Prós:* Padrão corporativo, gestão completa de usuários, MFA e recuperação de senha prontos.
     * *Contras:* Complexidade desnecessária para a fase inicial de Prova de Conceito/MVP.
* **Recomendação Preliminar:** Adotar a **Alternativa A (Spring Security + JWT)** para o MVP, isolando os contratos de segurança para facilitar eventual transição futura para OIDC/Keycloak.
* **Ciclo de Resolução:** Ciclo 2.

---

### DP-04 — Protocolo de Telemetria de Jobs no Frontend
* **Contexto:** Como processamentos InSAR levam minutos para concluir, o frontend precisa manter o usuário informado sobre as transições de estado (`DOWNLOADING`, `COREGISTERING`, etc.).
* **Alternativas em Avaliação:**
  1. **Alternativa A — Polling Inteligente via React Query:** O frontend dispara `GET /api/jobs/{id}/status` a cada 5 segundos enquanto o status for diferente de `COMPLETED` ou `FAILED`.
     * *Prós:* Nenhuma complexidade de conexões persistentes; extremamente resiliente a quedas de rede e compatível com proxies/firewalls corporativos restritivos.
     * *Contras:* Pequeno atraso (de até 5 segundos) na percepção de mudança de estado.
  2. **Alternativa B — Server-Sent Events (SSE) ou WebSocket:** Conexão contínua unidirecional (SSE) ou bidirecional (WebSocket).
     * *Prós:* Atualização instantânea milissegundo a milissegundo.
     * *Contras:* Exige controle de reconexão de sockets, gerenciamento de estado de sessões no backend e maior consumo de conexões abertas no Tomcat.
* **Recomendação Preliminar:** Adotar a **Alternativa A (React Query Polling)** para o MVP. O atraso de 5 segundos é totalmente imperceptível em um processamento que dura mais de 10 minutos.
* **Ciclo de Resolução:** Ciclo 2.

---

### DP-05 — Estratégia de Staging de Cenas Brutas SLC
* **Contexto:** Cenas Sentinel-1 SLC possuem ~5 GB cada. Ao processar um par, 10 GB de dados brutos são manipulados.
* **Alternativas em Avaliação:**
  1. **Alternativa A — Volume Temporário Efêmero Local:** Baixar diretamente para o disco montado no container do worker (scratch) e expurgar com comando `rm -rf` pós-processamento.
  2. **Alternativa B — Bucket S3 de Staging com Lifecycle Rule:** Gravar no bucket S3 e configurar regra de ciclo de vida da AWS para deletar objetos com prefixo `staging/` após 24 horas.
* **Recomendação Preliminar:** Utilizar a **Alternativa A** em desenvolvimento local e na nuvem se o worker for autocontido; adotar a **Alternativa B** caso o worker seja distribuído em múltiplos nós de computação efêmeros (ex.: AWS Fargate/Batch).
* **Ciclo de Resolução:** Ciclo 3 (Aquisição de Cenas).

---

### DP-06 — Seleção da Área Piloto de Validação em Solo
* **Contexto:** O MVP exige a demonstração de validação sobre uma área real com subsidência conhecida para cumprimento dos critérios de sucesso geocientíficos.
* **Candidatas em Avaliação:**
  1. **Bairro do Pinheiro e adjacências (Maceió/AL):** Fenômeno de subsidência urbana e colapso de cavidades de sal-gema amplamente documentado pelo Serviço Geológico do Brasil (SGB/CPRM) com dezenas de estudos InSAR publicados na literatura.
  2. **Quadrilátero Ferrífero (Minas Gerais):** Região com forte atividade minerária e movimentações de encostas e pilhas documentadas.
* **Recomendação Preliminar:** **Maceió/AL (Bairro do Pinheiro)**, pois apresenta altas taxas de subsidência em meio urbano desprovido de vegetação densa, garantindo alta coerência interferométrica ($\gamma > 0.60$) em banda C e facilidade de comparação com relatórios geológicos públicos já consolidados.
* **Ciclo de Resolução:** Ciclo 4 (Processamento SAR/InSAR).
