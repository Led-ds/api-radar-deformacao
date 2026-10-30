# Radar++ — Análise Técnica Detalhada do Backend Java/Spring Boot

**Documento:** `03-analise-backend.md`  
**Data da Auditoria:** Outubro de 2026  
**Status:** Concluído  
**Projeto Auditado:** `api-radar-deformacao`  
**Tecnologias:** Java 17, Spring Boot 3.3.4, Spring Data JPA, OpenFeign 4.1.1, GeoTools 26.0, JAI 1.1.3, AWS SDK v2, PostgreSQL.

---

## 1. Organização Estrutural e Arquitetura de Camadas

O projeto `api-radar-deformacao` adota nominalmente uma divisão inspirada em princípios de arquitetura em camadas e DDD (*Domain-Driven Design*), mas que na prática apresenta forte desvio conceitual e acoplamento reverso.

### Estrutura de Pacotes Observada:
* `com.ass.br.apiradar`:
  * `application`: Contém a classe `Main.java` (código de teste manual) e o pacote `usecases/ProcessarImagemSAR.java` (caso de uso com corpo comentado).
  * `domain`:
    * `model`: Entidades JPA misturadas com modelos de domínio anêmicos (`Deformacao`, `ImagemRadar`, `AnaliseResultado`, `AnaliseSolo`).
    * `model.dto`: DTOs de integração com a NASA (`NasaImageResponse`, `Resource`).
    * `repositories`: Interfaces Spring Data JPA (`DeformacaoRepository`, `ImagemRadarRepository`, `AnaliseResultadoRepository`).
    * `services`: Serviços de aplicação e infraestrutura (`AnaliseService`, `DeformacaoService`, `ImageService`, `ImagemRadarService`, `ProcessamentoService`, `StorageService`).
  * `infrastructure`:
    * `apis`: Interfaces OpenFeign para clientes HTTP externos (`AlosPalsarClient`, `FastAPIClientPy`, `NasaOpenDataClient`, `UsgsEarthquakeClient`).
    * `component`: Componentes com lógica computacional e geoespacial (`CalculoGeoespacial`, `GeoToolsProcessor`).
    * `config`: Classes de configuração do Spring (`CorsConfig`, `FeignConfig`, `S3Config`).
  * `presentation`:
    * `controllers`: Controladores REST (`DeformacaoController`, `ImagemRadarController`).
    * `dto`: Objetos de transferência de dados da API REST (`DeformacaoRequestDto`, `DeformacaoResponseDto`, `ImagemRadarResponseDto`, `RiscoResponseDto`).

### Problemas Estruturais de Arquitetura:
1. **Inversão de Dependências Violada:** A camada de domínio depende diretamente de clientes de infraestrutura (`AnaliseService` injeta `NasaOpenDataClient` e `ObjectMapper`; `ProcessamentoService` injeta `GeoToolsProcessor`, `FastAPIClientPy` e `ObjectMapper`). A regra do Clean Architecture determina que o domínio deve ser o núcleo isolado, sem conhecimento de frameworks ou clientes HTTP.
2. **Camada de Aplicação Esvaziada:** A pasta `application` possui apenas uma classe com método comentado (`ProcessarImagemSAR`), enquanto toda a orquestração de caso de uso está espalhada entre serviços do pacote `domain.services`.
3. **Modelos Anêmicos:** As entidades (`Deformacao`, `ImagemRadar`, `AnaliseResultado`) são meras estruturas de dados munidas de anotações Lombok (`@Data`, `@Builder`), sem encapsulamento, invariantes ou métodos de negócio.

---

## 2. Catálogo Completo de Endpoints REST

Mapeamento exaustivo de todas as rotas expostas pelo backend:

| Método | Rota HTTP | Parâmetros / Entrada | Resposta / Saída | Serviço Responsável | Dependências Externas | Observações Técnicas |
|---|---|---|---|---|---|---|
| `POST` | `/api/analise/deformacao` | Body JSON: `DeformacaoRequestDto` (`latitude`, `longitude`, `deslocamento`, `risco`) | `200 OK` com `DeformacaoResponseDto` | `DeformacaoService` → `AnaliseService` | NASA Open Data API, AWS S3, FastAPI (porta 8082), PostgreSQL | Executa todo o pipeline síncrono: busca na NASA, salva S3, processa GeoTools, chama FastAPI, persiste e retorna. Sujeito a timeout. |
| `GET` | `/api/analise/deformacoes` | Nenhuma | `200 OK` com `List<DeformacaoResponseDto>` | `DeformacaoService` → `AnaliseService` | PostgreSQL | Retorna todas as deformações cadastradas no banco sem paginação. |
| `POST` | `/api/analise/processar-imagem/{imagemRadarId}` | Path: `imagemRadarId` (String) | `200 OK` (Void) | `ProcessarImagemSAR` (Use Case) | Nenhuma | **No-op.** O método invocado está 100% comentado no código. |
| `GET` | `/api/imagens/deformacao/{deformacaoId}` | Path: `deformacaoId` (Long) | `200 OK` com `List<ImagemRadarResponseDto>` | `ImagemRadarService` | PostgreSQL | Retorna metadados das imagens associadas à deformação (ID, origem, data, processada). Não retorna bytes nem URLs. |
| `GET` | `/api/imagens/download/id/{imagemId}` | Path: `imagemId` (Long) | `200 OK` (`application/octet-stream`) com byte[] ou `404 Not Found` | `ImagemRadarService` | PostgreSQL (tabela `imagem_radar`, coluna OID) | Baixa a imagem diretamente do banco de dados relacional através de streaming do campo `@Lob`. |
| `GET` | `/api/imagens/s3/deformacao/{deformacaoId}` | Path: `deformacaoId` (Long) | `200 OK` com `List<String>` | `ImagemRadarService` | PostgreSQL | Retorna lista de URLs do S3 gravadas na entidade `ImagemRadar`. |
| `GET` | `/api/imagens/download/nome/{nomeImagem}` | Path: `nomeImagem` (String) | `200 OK` (`MediaType` detectado) com byte[] ou `500 Internal Server Error` | `StorageService` | AWS S3 (`radar-imagesbucket`) | **Grave bug:** ignora a deformação solicitada e faz download fixo da pasta `imagens/deformacao-68/{nomeImagem}`. |

---

## 3. Análise Detalhada dos Serviços e Regras de Negócio

### 3.1. `AnaliseService.java`
* **Fluxo de `salvarDeformacao`:**
  1. Executa um primeiro `deformacaoRepository.save(deformacao)` para gerar ID.
  2. Chama o método privado `avaliarRisco(deformacao)`.
  3. Atualiza o atributo `risco` com o resultado e executa novo `deformacaoRepository.save(deformacao)`.
* **Vulnerabilidades e Inconsistências:**
  * O método `avaliarRisco` consome a API da NASA usando a chave de API injetada via `@Value("${api.key.open.data.nasa}")`.
  * Se `nasaImage.getUrl()` for nulo, retorna a string `"Dados insuficientes para avaliação de risco."` e grava essa string no campo `risco`.
  * Não há tratamento para cenários de queda da NASA, rate-limiting ou retorno com erro HTTP. A transação inteira falha com rollback ou `RuntimeException`.

### 3.2. `ProcessamentoService.java`
* Orquestra a transformação de imagem:
  1. Chama `geoToolsProcessor.processar(imagemRadar.getDadosBrutos(), deformacao)`.
  2. Salva o resultado como nova entidade `ImagemRadar` com origem `"FASTAPI"`.
  3. Converte os bytes em um `MultipartFile` sintético via `ImageService.imageToMultipartFile`.
  4. Invoca o endpoint Feign `clientPy.analyze(multipartFile, true, 64)`.
  5. Converte o JSON de resposta (`result` e `confidence`) e persiste um `AnaliseResultado`.
* **Vulnerabilidade:**
  * Invocação síncrona encadeada: qualquer lentidão na rede neural do Python trava a thread do Tomcat no backend Java.
  * O `clientPy.analyze` envia a imagem gerada pelo GeoTools (que é um GeoTIFF com compressão LZW) com o nome e cabeçalho `image/png`. No Python, o PIL tenta abrir o arquivo e pode falhar se codecs TIFF específicos não estiverem disponíveis.

### 3.3. `StorageService.java`
* Gerencia o Amazon S3.
* **Erros de Projeto Críticos:**
  * **Duplicidade de clientes:** Cria um `S3Client` síncrono sob demanda em cada chamada de `uploadFile` (instanciando builder e credenciais estáticas a cada request), mas utiliza um `S3AsyncClient` injetado para o método `downloadFile`.
  * **Bloqueio Assíncrono:** No método `downloadFile`, chama `future.get()` bloqueando a thread de execução.
  * **Hardcoding de Recursos:** Na linha 62 e 66, hardcodeia `bucketName = "radar-imagesbucket"` e a chave `.key("imagens/deformacao-68/" + nomeImagem)`, tornando impossível recuperar imagens dinâmicas de outras deformações via este método.

### 3.4. `GeoToolsProcessor.java`
* Aplica manipulação matricial com GeoTools e JAI:
  1. `lerImagemGeoTIFF`: Lê os bytes com `ImageIO.read()`. Se nulo, lança exceção com mensagem inconsistente (*"Não foi possível ler a imagem PNG."*).
  2. Calcula um envelope geográfico (`geoBoundingCalculator`) usando o valor de `deformacao.getDeslocamento()` como delta angular para os limites norte/sul e leste/oeste.
  3. `aplicarFiltroSuavizacao`: Tenta aplicar convolução com um kernel gaussiano 3x3 normalizado (1/16). Se lançar `IllegalArgumentException`, entra em uma sequência bizarra de blocos `try/catch` aninhados tentando operações aleatórias: `Exp` → `Scale` → `Multiply` → `log` → `MultiplyConst` → `DivideByConst`.
  4. `salvarImagemGeoTIFF`: Serializa a imagem resultante com `GeoTiffWriter` e compressão LZW.
* **Diagnóstico Técnico:**
  * A utilização de operações matemáticas aleatórias como fallback indica tentativa e erro durante a depuração de incompatibilidades do JAI no Java 17.
  * Como a imagem original veio da API pública da NASA (uma foto óptica RGB em JPG/PNG), ela **não contém bandas de amplitude ou fase de radar**. Tratar essa imagem com operadores convolucionais genéricos e salvá-la como GeoTIFF não agrega nenhuma informação de deformação real.

### 3.5. `CalculoGeoespacial.java`
* Implementa a fórmula da distância geodésica de Haversine (`calcularDistanciaHaversine`) e a conversão de quilômetros para centímetros (`distanciaKm * 100000`).
* **Veredito:** Embora a matemática do Haversine esteja correta para calcular a separação métrica entre duas cidades ou coordenadas no globo terrestre, utilizá-la para definir a "deformação de terreno" em centímetros é um erro de modelagem física elementar.
* **Situação no Backend:** O componente está declarado como `@Component`, mas **não é injetado em nenhum outro serviço do backend**. Toda a lógica que dele dependia foi copiada diretamente para o frontend React em TypeScript.

---

## 4. Persistência e Modelagem de Dados

### 4.1. Entidades JPA
1. **`Deformacao`**:
   * Chave primária: `id` (Long, auto-incremento).
   * Atributos: `latitude`, `longitude`, `deslocamento` (double), `risco` (String).
   * Relacionamento: `@OneToMany` com `ImagemRadar` (cascade ALL, fetch LAZY).
2. **`ImagemRadar`**:
   * Atributos: `origem` ("NASA" ou "FASTAPI"), `dataColeta`, `urlImagem`, `processada` (boolean).
   * Atributo Crítico: `@Lob @Column(columnDefinition = "oid") private byte[] dadosBrutos;`.
   * **Anti-pattern de Banco de Dados:** Armazenar imagens inteiras em tabelas relacionais via PostgreSQL Large Objects (`oid`), concomitantemente com o upload para o S3, causa rápido inchaço do banco (*database bloat*), lentidão de backup e contenção de I/O de disco.
3. **`AnaliseResultado`**:
   * Atributos: `classificacao` ("Estável" ou "Instável"), `confianca` (double).
   * Relacionamento: `@OneToOne` com `ImagemRadar`.
4. **`AnaliseSolo`**:
   * Classe Java simples no pacote `model`, sem anotação `@Entity`, nunca referenciada por repositories ou services. Código morto.

---

## 5. Integrações HTTP e Configurações

### 5.1. OpenFeign e Erro Crítico em `FeignConfig.java`
* Em `FeignConfig.java:19`, encontramos o seguinte código:
  ```java
  @Bean
  public Encoder feignFormEncoder(List<HttpMessageConverter<?>> messageConverters) {
      return new SpringFormEncoder(new SpringEncoder(() -> 
          (org.springframework.boot.autoconfigure.http.HttpMessageConverters) messageConverters));
  }
  ```
* **Diagnóstico de Falha:** O parâmetro injetado é um `java.util.List`. O código faz um cast explícito para `HttpMessageConverters`. Isso lança invariavelmente `java.lang.ClassCastException: java.util.ArrayList cannot be cast to org.springframework.boot.autoconfigure.http.HttpMessageConverters` durante a inicialização do contexto do Spring se o conversor for acionado, impedindo a subida estável da aplicação. A correção requer instanciar `new HttpMessageConverters(messageConverters)`.

### 5.2. Configuração de S3 (`S3Config.java`)
* Configura `S3AsyncClient` com cliente HTTP Netty NIO, apontando para `Region.US_EAST_1`.
* **Risco Operacional:** Configurado com `.credentialsProvider(ProfileCredentialsProvider.create("terraform"))`. Se a máquina de execução (desenvolvimento local, servidor de CI ou contêiner Docker) não tiver um arquivo `~/.aws/credentials` com a seção `[terraform]`, qualquer operação de download falhará com erro de autenticação AWS.

### 5.3. Configuração de CORS (`CorsConfig.java`)
* Mapeia rotas `/api/**` permitindo apenas a origem `http://localhost:8081`.
* **Problema com Frontend:** O frontend Vite (`front_react`) executa por padrão na porta `5173` ou `8080`. Se o frontend não for iniciado expressamente na porta 8081, o navegador bloqueará todas as chamadas por violação de CORS (*Cross-Origin Resource Sharing*).

---

## 6. Tratamento de Exceções e Resiliência

* **Ausência de ControllerAdvice:** Não existe nenhuma classe `@RestControllerAdvice` ou `@ExceptionHandler` global.
* Exceções como `IOException`, `RuntimeException`, falhas de conexão HTTP do Feign ou erros do S3 sobem diretamente para o container servlet, resultando em respostas HTTP 500 genéricas contendo stacktraces completos e detalhes internos do servidor.

---

## 7. Testabilidade e Qualidade do Código

* **Cobertura de Testes Automatizados: 0%.**
* Não existem testes unitários para cálculo de coordenadas, conversores de DTO, validação de requisições ou lógicas de serviço.
* Não existem testes de integração com MockMvc ou WireMock para validar os clientes OpenFeign e repositories.
* Código morto mantido no repositório:
  * `application/Main.java`: rascunho de código para inspecionar descritores do JAI.
  * `application/usecases/ProcessarImagemSAR.java`: classe vazia com código comentado.
  * `infrastructure/apis/AlosPalsarClient.java`: interface sem uso.
  * `infrastructure/apis/UsgsEarthquakeClient.java`: interface sem uso.
  * `presentation/dto/RiscoResponseDto.java`: DTO sem uso.
  * `domain/model/AnaliseSolo.java`: modelo sem uso.

---

## 8. Avaliação Arquitetural: Monólito Modular com DDD

O backend atual é um candidato natural a uma reestruturação profunda baseada em **Monólito Modular** com **Clean Architecture / DDD**:

1. **Por que NÃO adotar microsserviços no estado atual?**
   * A divisão prematura entre backend Java e microsserviço Python adicionou problemas de serialização multipart, latência de rede, conversão de formatos (GeoTIFF/PNG) e pontos únicos de falha, sem que houvesse volume ou necessidade computacional que justificasse um cluster distribuído.
2. **Como estruturar o Monólito Modular:**
   * **Módulo de Aquisição de Cenas SAR (`acquisition`):** Responsável por autenticar e baixar metadados e produtos de satélites reais (Sentinel-1 via Copernicus CDSE e ALOS-2 via ASF).
   * **Módulo de Processamento InSAR (`processing`):** Isolado em ambiente científico (preferencialmente Python/C++ com bibliotecas consagradas como ISCE2/MintPy/GDAL), invocado de forma assíncrona orientada a jobs/mensageria.
   * **Módulo de Monitoramento e Alertas (`monitoring`):** Gerencia as Áreas de Interesse (AOIs), séries temporais de deslocamento e gatilhos de notificação.
   * **Módulo de Geo-Visualização e Relatórios (`presentation`):** Fornece dados georreferenciados (GeoJSON, COG/WMS) para o frontend.
