# Radar++ — Inventário Geral dos Projetos

**Documento:** `01-inventario-geral.md`  
**Data da Auditoria:** Outubro de 2026  
**Status:** Concluído  
**Escopo:** Levantamento exaustivo dos repositórios e artefatos presentes no workspace `repo_radar`.

---

## 1. Visão Geral do Ecossistema

O repositório raiz `/home/asoares/Documentos/repositories/repo_radar` abriga 4 projetos de software e 1 artefato documental de referência externa:

1. **`api-radar-deformacao`**: Backend principal desenvolvido em Java 17 com Spring Boot 3.3.4.
2. **`radar_fastapi_py`**: Microsserviço de processamento de imagens e inferência em Python 3.10 com FastAPI e TensorFlow/Keras.
3. **`front_react/github-friend-helper`**: Frontend web atual desenvolvido em React 18, TypeScript, Vite, Tailwind CSS e Leaflet.
4. **`monitorar-images-alos-palsar`**: Frontend legado desenvolvido em Angular 18 com TypeScript, Leaflet e Google Maps (descontinuado em favor do React).
5. **`JJ-FAST_API_UsersGuide_v1_1.pdf`**: Guia técnico oficial da JAXA/JICA sobre a API do sistema JJ-FAST (*JICA-JAXA Forest Early Warning System in the Tropics* via ALOS-2 PALSAR-2).

---

## 2. Inventário Detalhado por Projeto

### 2.1. Backend Java — `api-radar-deformacao`

| Atributo | Detalhe |
|---|---|
| **Diretório** | `/home/asoares/Documentos/repositories/repo_radar/api-radar-deformacao` |
| **Responsabilidade Pretendida** | API REST principal para orquestrar o cadastro de deformações, buscar imagens de satélite (NASA/ALOS), aplicar correções geoespaciais preliminares, enviar para análise na API Python, persistir resultados em banco relacional e gerenciar armazenamento de imagens na AWS S3. |
| **Linguagem / Runtime** | Java 17 |
| **Framework Principal** | Spring Boot 3.3.4 (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`) |
| **Gerenciador de Build** | Apache Maven 3.x (`pom.xml`, Maven Wrapper incluído `.mvn/wrapper`) |
| **Banco de Dados** | PostgreSQL (`org.postgresql:postgresql`, runtime) |
| **Bibliotecas Relevantes** | - **Spring Cloud OpenFeign 4.1.1** (`spring-cloud-starter-openfeign`): chamadas HTTP declarativas.<br>- **GeoTools 26.0** (`gt-image`, `gt-main`, `gt-referencing`, `gt-geotiff`, `gt-shapefile`, `gt-coverage`, `gt-epsg-hsql`): manipulação de rasters, CRS EPSG:4326 e envelopes geoespaciais.<br>- **Java Advanced Imaging (JAI) 1.1.3** (`jai_core`, `jai_codec`, `jai-imageio-core 1.4.0`): processamento de imagens em baixo nível.<br>- **AWS SDK for Java v2 2.20.43** (`s3`, `netty-nio-client`, `auth`): integração com Amazon S3 síncrono e assíncrono.<br>- **Lombok**: geração de getters, setters, builders e construtores. |
| **Estrutura de Diretórios** | ```<br>api-radar-deformacao/<br>├── pom.xml<br>└── src/main/<br>    ├── java/com/ass/br/apiradar/<br>    │   ├── ApiRadarApplication.java<br>    │   ├── application/<br>    │   │   ├── Main.java (rascunho de teste JAI)<br>    │   │   └── usecases/ProcessarImagemSAR.java (corpo comentado)<br>    │   ├── domain/<br>    │   │   ├── model/ (Deformacao, ImagemRadar, AnaliseResultado, AnaliseSolo)<br>    │   │   ├── model/dto/ (NasaImageResponse, Resource)<br>    │   │   ├── repositories/ (DeformacaoRepository, ImagemRadarRepository, AnaliseResultadoRepository)<br>    │   │   └── services/ (AnaliseService, DeformacaoService, ImageService, ImagemRadarService, ProcessamentoService, StorageService)<br>    │   ├── infrastructure/<br>    │   │   ├── apis/ (AlosPalsarClient, FastAPIClientPy, NasaOpenDataClient, UsgsEarthquakeClient)<br>    │   │   ├── component/ (CalculoGeoespacial, GeoToolsProcessor)<br>    │   │   └── config/ (CorsConfig, FeignConfig, S3Config)<br>    │   └── presentation/<br>    │       ├── controllers/ (DeformacaoController, ImagemRadarController)<br>    │       └── dto/ (DeformacaoRequestDto, DeformacaoResponseDto, ImagemRadarResponseDto, RiscoResponseDto)<br>    └── resources/<br>        ├── application.properties<br>        └── META-INF/additional-spring-configuration-metadata.json<br>``` |
| **Arquivos de Configuração** | `pom.xml`, `src/main/resources/application.properties` |
| **Integrações Externas** | - **NASA Open Data API** (`https://api.nasa.gov/planetary/earth/assets`) via `NasaOpenDataClient`.<br>- **Python FastAPI** (`http://localhost:8082`) via `FastAPIClientPy`.<br>- **AWS S3** (`s3.us-east-1.amazonaws.com` / `radar-imagesbucket`) via `StorageService` e `S3Config`.<br>- **JAXA ALOS-PALSAR** (`https://api.alospalsar.jaxa.jp` - endpoint fictício, não funcional) via `AlosPalsarClient`.<br>- **USGS Earthquake** (`https://earthquake.usgs.gov` - cliente criado, porém não referenciado em nenhum serviço). |
| **Formas de Execução** | `mvn spring-boot:run` ou `./mvnw spring-boot:run` (Porta padrão: `8080`). Depende de PostgreSQL local na porta 5432 (banco `bdusuariosapi`) e credenciais AWS configuradas. |
| **Testes Automatizados** | **Inexistentes.** O diretório `src/test` não existe no repositório. Há dependências declaradas no `pom.xml` (`spring-boot-starter-test`, `spring-security-test`), mas nenhuma classe de teste foi escrita. |
| **Documentação** | Inexistente no projeto (não há `README.md`). |
| **Controle de Versão (Git)** | Ramo ativo: `dev`. Total de 8 commits registrados no histórico. Remoto: `https://github.com/Led-ds/api-radar-deformacao.git`. |
| **Estado de Implementação** | **Parcial / PoC instável.** O fluxo básico compila, mas possui falhas de execução (ClassCastException em `FeignConfig`, dependência de perfil AWS `terraform`, busca S3 com ID fixo e ausência de dados reais de SAR). |

---

### 2.2. Microsserviço Python — `radar_fastapi_py`

| Atributo | Detalhe |
|---|---|
| **Diretório** | `/home/asoares/Documentos/repositories/repo_radar/radar_fastapi_py` |
| **Responsabilidade Pretendida** | API de processamento e classificação de imagens para detecção de deformação/instabilidade através de rede neural convolucional (CNN). |
| **Linguagem / Runtime** | Python 3.10 |
| **Framework Principal** | FastAPI 0.x com servidor ASGI Uvicorn |
| **Gerenciador de Dependências** | `pip` com arquivo `requirements.txt` (sem versões fixadas) |
| **Bibliotecas Relevantes** | - **FastAPI**: definição de rotas assíncronas.<br>- **Uvicorn**: servidor ASGI HTTP.<br>- **TensorFlow / Keras**: definição, treinamento e carga do modelo CNN (`meu_modelo.h5`).<br>- **Pillow (PIL)**: manipulação, redimensionamento (64x64) e conversão de formato de imagem.<br>- **NumPy**: matrizes de imagem e geração de dados sintéticos de treino. |
| **Estrutura de Diretórios** | ```<br>radar_fastapi_py/<br>├── requirements.txt<br>├── README.md<br>├── app/<br>│   ├── main.py<br>│   ├── models/<br>│   │   ├── model_loader.py<br>│   │   └── models/<br>│   │       └── meu_modelo.h5 (modelo Keras compilado, 9.9 MB)<br>│   └── routes/<br>│       └── analyze.py<br>└── models/<br>    └── train_model.py (script de treinamento com ruído aleatório)<br>``` |
| **Arquivos de Configuração** | `requirements.txt` |
| **Integrações Externas** | Nenhuma chamada externa direta. Recebe requisições multipart do backend Java na porta `8082`. |
| **Formas de Execução** | `uvicorn app.main:app --host 0.0.0.0 --port 8082 --reload` |
| **Testes Automatizados** | **Inexistentes.** Não há arquivos de teste unitário ou de integração com pytest/unittest. |
| **Documentação** | `README.md` conciso com instruções básicas de criação de venv e inicialização do servidor. |
| **Controle de Versão (Git)** | Ramo ativo: `master`. Total de 2 commits registrados no histórico. Remoto: `https://github.com/Led-ds/radar_fastapi_py.git`. Nota: a pasta `venv` completa com pacotes de terceiros foi indevidamente incluída no controle de versão. |
| **Estado de Implementação** | **Protótipo cosmético / Toy CNN.** O serviço sobe e responde `POST /analyze/`, mas o modelo foi treinado sobre 100 imagens de ruído aleatório uniforme (`np.random.random`), tornando as predições de "Stable"/"Unstable" desprovidas de qualquer validade física ou científica. A rota `/metrics/` falha com `TypeError` ao invocar `model.evaluate()` sem parâmetros. |

---

### 2.3. Frontend React — `front_react/github-friend-helper`

| Atributo | Detalhe |
|---|---|
| **Diretório** | `/home/asoares/Documentos/repositories/repo_radar/front_react/github-friend-helper` |
| **Responsabilidade Pretendida** | Aplicação web SPA para exibição de mapa interativo de monitoramento, visualização de pontos de deformação georreferenciados, captura de coordenadas por clique no mapa, cálculo de deslocamento aparente, visualização de imagens satelitais brutas e processadas via popup/modal, e envio de novos registros ao backend. |
| **Linguagem / Runtime** | TypeScript 5.5, Node.js (Vite 5.4) |
| **Framework / Biblioteca Base** | React 18.3 |
| **Gerenciador de Pacotes** | npm (`package.json`, `package-lock.json`, com rastro de `bun.lockb`) |
| **Bibliotecas Relevantes** | - **Vite 5.4**: build tool e dev server.<br>- **Leaflet 1.9 & React-Leaflet 4.2**: renderização de mapas, marcadores e popups com tiles OpenStreetMap.<br>- **Tailwind CSS 3.4 & PostCSS**: estilização utilitária responsiva.<br>- **Radix UI & shadcn/ui**: coleção de componentes de interface acessíveis (dialog, toast, popover, button, form, etc.).<br>- **TanStack React Query 5.56**: gerenciamento de requisições assíncronas.<br>- **Lucide React**: biblioteca de ícones. |
| **Estrutura de Diretórios** | ```<br>front_react/github-friend-helper/<br>├── index.html<br>├── package.json<br>├── tailwind.config.ts<br>├── vite.config.ts<br>├── src/<br>│   ├── main.tsx<br>│   ├── App.tsx<br>│   ├── index.css<br>│   ├── components/<br>│   │   ├── MonitoringMap.tsx (mapa Leaflet, cálculo de distância e formulário)<br>│   │   ├── ImageViewer.tsx (modal para visualização de imagem expandida)<br>│   │   └── ui/ (50+ componentes shadcn/ui)<br>│   ├── models/<br>│   │   └── Deformacao.ts (interface TypeScript)<br>│   ├── pages/<br>│   │   ├── Index.tsx (página principal "Monitoramento SAR")<br>│   │   └── NotFound.tsx<br>│   └── services/<br>│       └── mapService.ts (integração HTTP com o backend Java)<br>``` |
| **Arquivos de Configuração** | `package.json`, `vite.config.ts`, `tailwind.config.ts`, `components.json`, `tsconfig.json` |
| **Integrações Externas** | - Backend Java em `http://localhost:8080/api`.<br>- Tiles de mapa público OpenStreetMap (`https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png`).<br>- Ícones padrão Leaflet via CDN (`https://unpkg.com/leaflet@1.7.1/...`). |
| **Formas de Execução** | `npm run dev` (executa dev server Vite, tipicamente na porta 8080 ou 5173). |
| **Testes Automatizados** | **Inexistentes.** Não há testes Jest, Vitest ou Playwright/Cypress. |
| **Documentação** | `README.md` padrão gerado pela plataforma Lovable.dev. |
| **Controle de Versão (Git)** | Ramo ativo: `main`. Total de 24 commits. Remoto: `Led-ds/github-friend-helper.git` (contendo token PAT exposto na URL). |
| **Estado de Implementação** | **Funcional em UI, porém com lógica de domínio conceitualmente incorreta.** Renderiza o mapa e os marcadores, mas calcula deformação com base na distância esférica de Haversine entre dois cliques do usuário, e busca imagens com nomes fixos (`nasa_original.png`, `fastapi_processada.png`) direcionados a uma pasta fixa do S3. |

---

### 2.4. Frontend Legado Angular — `monitorar-images-alos-palsar`

| Atributo | Detalhe |
|---|---|
| **Diretório** | `/home/asoares/Documentos/repositories/repo_radar/monitorar-images-alos-palsar` |
| **Responsabilidade Pretendida** | Primeira versão do frontend web de monitoramento, idealizada para exibir imagens ALOS PALSAR e formulário de submissão de coordenadas. |
| **Linguagem / Runtime** | TypeScript 5.5, Angular 18.2 |
| **Framework Principal** | Angular 18 (`@angular/core`, `@angular/common`, `@angular/forms`) com suporte a SSR (`@angular/ssr`) |
| **Gerenciador de Pacotes** | npm (`package.json`, `package-lock.json`) |
| **Bibliotecas Relevantes** | - **Leaflet 1.9**: componente de mapa inicial.<br>- **Google Maps** (`@angular/google-maps`): dependência instalada, mas utilizada apenas como iframe estático do Google Maps.<br>- **Karma & Jasmine**: infraestrutura padrão de testes do Angular CLI. |
| **Estrutura de Diretórios** | ```<br>monitorar-images-alos-palsar/<br>├── angular.json<br>├── package.json<br>└── src/<br>    ├── app/<br>    │   ├── app.component.ts<br>    │   ├── model/deformacao.ts<br>    │   ├── service/api-radar.service.ts<br>    │   ├── map/ (componente Leaflet básico)<br>    │   └── monitoramento/ (componente com formulário e iframe de mapa)<br>``` |
| **Formas de Execução** | `ng serve` (Porta padrão: `4200`). |
| **Testes Automatizados** | Apenas os testes boilerplate gerados pelo Angular CLI (`.spec.ts`), que falham ou não cobrem regras de negócio customizadas. |
| **Documentação** | `README.md` padrão gerado pelo Angular CLI. |
| **Controle de Versão (Git)** | Ramo ativo: `dev`. 1 único commit ("first commit"). Remoto: `https://github.com/Led-ds/monitorar-images-alos-palsar.git`. |
| **Estado de Implementação** | **Descontinuado / Abandonado.** O projeto foi substituído pela implementação em React (`github-friend-helper`). Apresentava inconsistências graves, como a multiplicação da distância de Haversine por 100 com o comentário incorreto de que convertia km para cm. |

---

### 2.5. Artefato Documental Externo — `JJ-FAST_API_UsersGuide_v1_1.pdf`

| Atributo | Detalhe |
|---|---|
| **Localização** | `/home/asoares/Documentos/repositories/repo_radar/JJ-FAST_API_UsersGuide_v1_1.pdf` |
| **Origem** | JICA (*Japan International Cooperation Agency*) e JAXA (*Japan Aerospace Exploration Agency*). |
| **Versão / Data** | Versão 1.1, Setembro de 2021 (738 KB, 14 páginas). |
| **Conteúdo Técnico** | Manual de integração com a API do sistema JJ-FAST, que detecta desmatamento em florestas tropicais usando radar em banda L (satélite ALOS-2 PALSAR-2). Fornece especificações de chamadas HTTP GET (`https://www.eorc.jaxa.jp/cgi-bin/jjfast/api/getlist.cgi`) para obtenção de polígonos GeoJSON/Shapefiles de alertas de desmatamento em grades de 1°x1°. |
| **Relação com o Código** | Serviu como inspiração conceitual para o nome do repositório `monitorar-images-alos-palsar` e a criação do cliente `AlosPalsarClient.java`. Contudo, nenhuma das rotas reais documentadas no PDF foi implementada no código Java. |

---

## 3. Resumo Quantitativo do Ecossistema

| Projeto | Tecnologia Base | Arquivos Fonte (aprox.) | Linhas de Código (efetivas) | Testes Unitários | Cobertura Real |
|---|---|---|---|---|---|
| **api-radar-deformacao** | Java 17 / Spring Boot 3.3 | 25 classes Java | ~1.100 LOC | 0 | 0% |
| **radar_fastapi_py** | Python 3.10 / FastAPI / Keras | 5 arquivos Python | ~180 LOC | 0 | 0% |
| **front_react** | React 18 / TypeScript / Vite | 6 componentes de app | ~550 LOC (excl. shadcn) | 0 | 0% |
| **monitorar-images (Angular)** | Angular 18 / TypeScript | 5 componentes/serviços | ~200 LOC | Boilerplate CLI | 0% |
| **TOTAL** | Polyglot (Java + Python + TS) | ~41 arquivos de código | ~2.030 LOC | 0 | 0% |

---

## 4. Diagnóstico Geral do Inventário

1. **Volume de código reduzido:** O Radar++ não é um sistema legado volumoso; trata-se de uma Prova de Conceito (PoC) enxuta com cerca de 2.000 linhas de código proprietário escritas em ciclos exploratórios rápidos.
2. **Fragmentação de repositórios:** Para um sistema dessa escala, a divisão em 4 repositórios Git independentes (com repositório React aninhado em subpasta com nome genérico `github-friend-helper` e histórico de scaffolding da plataforma Lovable) gerou forte overhead de sincronização de contratos e segredos.
3. **Ausência total de automação de testes e CI/CD:** Não há esteiras automatizadas, linters configurados em pré-commit, nem testes que comprovem o funcionamento de ponta a ponta.
4. **Desconexão entre os requisitos SAR e o código existente:** Apesar de o objetivo do produto ser o monitoramento de deformações de solo por radar SAR (InSAR), os componentes construídos lidam com imagens ópticas da NASA (Landsat/MODIS), filtros convolucionais 2D genéricos de imagens em escala de cinza e distâncias geográficas entre cliques de tela.
