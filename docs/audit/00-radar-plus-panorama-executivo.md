# Radar++ — Relatório Consolidado e Panorama Executivo (Ciclo 0)

**Documento:** `00-radar-plus-panorama-executivo.md`  
**Data da Auditoria:** Outubro de 2026  
**Auditor Responsável:** Equipe de Arquitetura de Software e Engenharia Geocientífica  
**Status:** Concluído — Pronto para Decisão de Governança

---

## 1. Sumário Executivo: As 12 Respostas Estratégicas

### 1. O que é o Radar++ atualmente?
O Radar++ encontra-se atualmente no estágio de **Prova de Conceito (PoC) exploratória**, com ênfase na prototipação visual de interface web. Ele ainda **não é** um sistema de monitoramento de radar SAR ou interferometria InSAR operacional.

### 2. O que conseguimos construir até agora?
Conseguiu-se construir uma interface de usuário agradável em React com visualização de mapa Leaflet, um backend Spring Boot capaz de persistir coordenadas e arquivos no PostgreSQL e na AWS S3, e um microsserviço Python com FastAPI que executa uma rede neural convolucional para retornar um rótulo textual de risco.

### 3. Quais tecnologias estão efetivamente sendo usadas?
* **Frontend:** React 18, TypeScript 5.5, Vite 5.4, Tailwind CSS 3.4, React-Leaflet 4.2 e componentes de UI Radix / shadcn/ui.
* **Backend:** Java 17, Spring Boot 3.3.4, Spring Data JPA, OpenFeign 4.1.1, GeoTools 26.0, JAI 1.1.3 e AWS SDK for Java v2 (S3).
* **Processamento / IA:** Python 3.10, FastAPI 0.x, Uvicorn, TensorFlow/Keras e Pillow (PIL).
* **Banco de Dados / Armazenamento:** PostgreSQL (com Large Objects OID) e Amazon S3.

### 4. O que realmente funciona?
* A navegação cartográfica com mapa interativo do OpenStreetMap.
* A captura de cliques pontuais na tela para extrair latitude e longitude.
* O envio do formulário HTTP para a API Java e a inserção de registros em tabela relacional.
* A plotagem de marcadores no mapa e a abertura de modais com zoom de imagens satelitais.
* O fluxo de submissão de imagem multipart para o FastAPI e retorno de JSON.

### 5. O que ainda não foi validado?
* O comportamento com mais de um usuário simultâneo e requisições concorrentes.
* A resiliência a falhas de rede de terceiros e tratamento de indisponibilidade da NASA ou S3.
* O funcionamento em contêineres Docker padronizados (inexistentes no momento).
* Qualquer validação por testes unitários ou automatizados (cobertura real: 0%).

### 6. Como os três projetos se relacionam?
O usuário clica no mapa no frontend React, que calcula uma distância de Haversine e envia um POST para o backend Java (`porta 8080`). O Java salva a entidade no PostgreSQL, tenta baixar uma foto óptica da NASA, processa essa foto no GeoTools aplicando um filtro de convolução, salva o resultado no S3 e no banco, e envia o arquivo via HTTP para o Python FastAPI (`porta 8082`). O Python redimensiona para 64x64, passa pela rede neural e devolve "Stable" ou "Unstable". O Java grava essa resposta como o risco da deformação e responde ao React, que força o recarregamento total da tela.

### 7. Qual é a maturidade técnica atual?
A maturidade técnica global é **Baixa (1,1 / 5,0)**. O projeto priorizou a ilusão de funcionamento ("demo superficial") em detrimento da consistência matemática, da integridade do domínio geocientífico, da testabilidade e da segurança de infraestrutura.

### 8. Quais são os principais problemas?
1. **Erro conceitual elementar de domínio:** A distância em quilômetros entre dois cliques do usuário no mapa é multiplicada por 100.000 e gravada como se fosse deformação milimétrica/centimétrica do solo.
2. **Modelo de IA nulo cientificamente:** A rede neural Keras foi treinada com matrizes de ruído aleatório uniforme geradas com `np.random.random`, gerando predições cosméticas.
3. **Ausência total de processamento SAR:** Não há leitura de dados SAR complexos (SLC), corregistro, interferometria de radar ou unwrapping de fase. A única fonte integrada é a API de fotografias ópticas da NASA.
4. **Segurança crítica:** Chaves ativas de acesso da AWS e chaves de API expostas em texto claro no `application.properties`, além de um GitHub Personal Access Token exposto na URL remota do Git.
5. **Mock fixo de imagens:** O backend busca imagens fixadas na pasta `imagens/deformacao-68/` no S3, fazendo com que todos os pontos do mapa exibam sempre as mesmas fotos.

### 9. O que podemos aproveitar?
* A base de interface e design system do React (`front_react`), incluindo os componentes shadcn/ui, o mapa Leaflet e o modal `ImageViewer.tsx`.
* A integração com a AWS S3 utilizando o AWS SDK v2 assíncrono (após expurgar as chaves e hardcodings).
* Os esquemas relacionais básicos de cadastro de pontos e histórico de análises.

### 10. O que precisamos reconstruir?
* O núcleo de processamento geocientífico: substituir o modelo toy de Python e os filtros JAI do Java por um pipeline SAR/InSAR comprovado (ex.: ISCE2 / SNAP GPT / HyP3).
* A integração satelital: substituir o cliente óptico da NASA por conectores reais com a ESA Copernicus (CDSE) e NASA/ASF.
* A lógica de negócio: eliminar o cálculo de Haversine e a dependência de IDs fixos.
* A segurança e infraestrutura: centralizar credenciais em variáveis de ambiente, adotar autenticação e criar infraestrutura Docker Compose.

### 11. O que falta para realizar a primeira análise SAR/InSAR cientificamente válida?
1. Ingestão de duas cenas reais de radar de abertura sintética (ex.: Sentinel-1 SLC em banda C) cobrindo a mesma área em datas distintas.
2. Obtenção do Modelo Digital de Elevação (DEM) correspondente (SRTM ou Copernicus DEM de 30m).
3. Execução de um pipeline interferométrico real (corregistro orbital, geração de interferograma complexo, subtração de fase topográfica, filtragem de coerência e unwrapping de fase via SNAPHU).
4. Conversão da fase desembrulhada em mapa geocodificado de deslocamento na Linha de Visada (LOS) em centímetros ou milímetros.

### 12. Qual seria o caminho mais seguro para chegar ao MVP?
Adotar a **Estratégia B (Reconstrução Controlada)**: estabelecer um repositório limpo e unificado, migrar a interface React moderna aproveitando todo o visual já construído, criar um backend enxuto em Monólito Modular para gerenciar áreas de interesse e orquestrar o processamento SAR através da API do **NASA/ASF HyP3** (que executa interferometria InSAR pronta e validada na nuvem sem necessidade de gerenciar nós pesados de HPC localmente nos primeiros meses).

---

## 2. Matriz de Maturidade Técnica por Dimensão

A avaliação a seguir utiliza uma escala de **0 a 5**, fundamentada nas evidências auditadas no código:

* **0:** Inexistente ou completamente disfuncional.
* **1:** Prova de conceito rudimentar com dados falsos ou severas quebras técnicas.
* **2:** Parcialmente funcional, mas com fortes débitos técnicos e mocks críticos.
* **3:** Funcional e adequado para ambiente de testes, requerendo polimento para produção.
* **4:** Robusto, testado, com boa arquitetura e pronto para produção.
* **5:** Estado da arte, altamente escalável, resiliente e totalmente automatizado.

| Dimensão Avaliada | Nota (0 a 5) | Critérios e Justificativas Técnicas | Evidências Observadas no Código |
|---|:---:|---|---|
| **1. Domínio e Modelagem SAR/InSAR** | **0.0 / 5.0** | Inexistência de conceitos de micro-ondas, fase, coerência ou pares interferométricos. O sistema calcula distância entre cliques de mouse como deformação. | `CalculoGeoespacial.java:47-59`, `MonitoringMap.tsx:44-59`. |
| **2. Processamento Científico e IA** | **0.5 / 5.0** | Rede neural Keras treinada com ruído branco (`np.random.random`); filtros convolucionais genéricos com try/catch aleatórios do JAI. Rota de métricas disfuncional. | `train_model.py:28-32`, `analyze.py:67`, `GeoToolsProcessor.java:105-132`. |
| **3. Backend e Arquitetura de Serviços** | **1.5 / 5.0** | Código Spring Boot compila e orquestra chamadas básicas, mas viola inversão de dependência, acopla operações síncronas bloqueantes e possui bug de cast no Feign. | `FeignConfig.java:19`, `AnaliseService.java:45-61`, `ProcessarImagemSAR.java:14`. |
| **4. Frontend e Visualização Web** | **2.5 / 5.0** | Interface moderna com Tailwind e shadcn/ui, mapa responsivo com Leaflet e modal de zoom. Porém, possui lógica de negócio incorreta e força recarregamento total de página. | `front_react/package.json`, `MonitoringMap.tsx:98`, `ImageViewer.tsx:13-28`. |
| **5. Persistência e Armazenamento** | **1.5 / 5.0** | Integração funcional com S3 e PostgreSQL, mas duplica imagens grandes via OID no banco e hardcodeia a chave `deformacao-68` no S3. | `StorageService.java:66`, `ImagemRadar.java:25-27`. |
| **6. Integrações Externas e Dados** | **1.0 / 5.0** | Não integra com Copernicus ou ASF. Integração satelital consome fotos ópticas da NASA; cliente ALOS aponta para URL inexistente; cliente USGS não é usado. | `AlosPalsarClient.java:7`, `NasaOpenDataClient.java:11-16`. |
| **7. Segurança e Gestão de Segredos** | **0.5 / 5.0** | Credenciais reais ativas da AWS expostas em texto claro; GitHub PAT na URL remota do Git; ausência completa de autenticação nas rotas. | `application.properties:22-23`, `front_react/.git/config`. |
| **8. Testabilidade e Qualidade** | **0.0 / 5.0** | Nenhuma suíte de testes automatizados presente em nenhum dos projetos. Sem CI/CD. Pasta `src/test` não existe no backend. | `find src/test` inexistente, ausência de linters e workflows YAML. |
| **MÉDIA GERAL PONDERADA** | **0.93 / 5.0** | **Maturidade Técnica Geral: PROTÓTIPO EXPLORATÓRIO INICIAL** | — |

---

## 3. Síntese Comparativa: O que Preservar vs O que Descartar

```
+--------------------------------------------------------------------------------+
|                              MAPA DE DECISÃO TÉCNICA                           |
+--------------------------------------------------------------------------------+
|  [PRESERVAR]   ---> Interface React, Componentes shadcn/ui, ImageViewer        |
|  [REFATORAR]   ---> Mapa Leaflet (para polígonos), AWS SDK v2, Esquema JPA     |
|  [SUBSTITUIR]  ---> FastAPI/Keras por SNAP/HyP3, NASA Óptica por Copernicus    |
|  [DESCARTAR]   ---> Frontend Angular, Cálculo Haversine, Clientes Fake ALOS/USGS|
+--------------------------------------------------------------------------------+
```

---

## 4. Conclusão e Próximo Passo Recomendado

O Radar++ possui um conceito de negócio de alto valor estratégico e uma casca visual moderna já prototipada no frontend React. No entanto, o motor de cálculo e a modelagem científica atualmente construídos não sustentam a promessa do produto.

A equipe de auditoria recomenda expressamente a **aprovação da Estratégia B (Reconstrução Controlada)** e o avanço para o **Ciclo 1 do Roadmap (Domínio e Arquitetura)**, formalizando o saneamento imediato de credenciais e a especificação de uma arquitetura baseada em dados reais de radar SAR.
