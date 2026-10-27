# Radar++ — Auditoria Técnica das Integrações e Segurança de Dados

**Documento:** `07-integracoes.md`  
**Data da Auditoria:** Outubro de 2026  
**Status:** Concluído  
**Escopo:** Auditoria detalhada das interfaces internas e externas, protocolos de comunicação, tratamento de falhas e análise de segurança de credenciais.

---

## 1. Comunicação Frontend ↔ Backend (React ↔ Java)

### 1.1. Protocolo e Contrato de Dados
* **Transporte:** HTTP/1.1 sobre REST com payloads JSON e binários (`Blob`).
* **Endpoints Consumidos:**
  * `POST http://localhost:8080/api/analise/deformacao` (envia `{ latitude, longitude, deslocamento, risco }`).
  * `GET http://localhost:8080/api/analise/deformacoes` (retorna lista de deformações cadastradas).
  * `GET http://localhost:8080/api/imagens/download/nome/{nomeImagem}` (retorna stream binário de imagem).

### 1.2. Pontos de Falha e Fragilidades Identificadas:
1. **Incompatibilidade de Portas e CORS:**
   * O backend configura expressamente em `CorsConfig.java:17` a permissão exclusiva para a origem `http://localhost:8081`.
   * O servidor de desenvolvimento Vite (`front_react/github-friend-helper`) inicializa por padrão na porta `5173` ou `8080`. Se executado nessas portas, o navegador bloqueia qualquer requisição por política de CORS.
2. **Nomes Fixos de Arquivos (Hardcoded Contracts):**
   * Em `mapService.ts:47-48`, o frontend solicita sistematicamente os nomes literais `nasa_original.png` e `fastapi_processada.png`, independentemente do identificador da deformação.
3. **Ausência de Paginação e Filtros:**
   * A rota `GET /analise/deformacoes` descarrega a tabela inteira sem limitação de registros (`findAll()`), criando um gargalo progressivo de memória conforme novos pontos forem cadastrados.

---

## 2. Comunicação Java ↔ Python (Spring Boot ↔ FastAPI)

### 2.1. Protocolo e Contrato de Dados
* **Transporte:** HTTP síncrono gerenciado pelo Spring Cloud OpenFeign (`FastAPIClientPy.java`).
* **URL Alvo:** `http://localhost:8082` (configurada estaticamente na anotação `@FeignClient`).
* **Método:** `POST /analyze/` consumindo `multipart/form-data`.
* **Payload Enviado:**
  * `file`: arquivo multipart com o nome sintético `fastapi_processada.png` contendo os bytes GeoTIFF serializados pelo GeoTools.
  * `grayscale`: booleano (`true`).
  * `size`: inteiro (`64`).
* **Payload Recebido:**
  ```json
  {
    "result": "Stable" | "Unstable",
    "confidence": 0.8123,
    "processing_time": 0.0152
  }
  ```

### 2.2. Pontos de Falha e Fragilidades Identificadas:
1. **Erro de Injeção de Dependência no `FeignConfig.java`:**
   * A linha 19 faz um cast inválido de `java.util.List` para `HttpMessageConverters`, o que quebra o ciclo de inicialização do encoder Feign se acionado.
2. **Incompatibilidade de Formatos de Arquivo:**
   * O backend gera um arquivo no formato GeoTIFF compactado com LZW e o envia com content-type declarado como `image/png` (`ImageService.java:47`). No lado Python, o Pillow tenta decodificá-lo como imagem matricial; embora versões recentes do Pillow possuam suporte a TIFF, inconsistências de compressão podem gerar `UnidentifiedImageError`.
3. **Ausência de Circuit Breaker e Timeout Explícito:**
   * Não há Resilience4j, Hystrix ou configuração de timeout de leitura no OpenFeign. Se o microsserviço Python estiver fora do ar ou sob alta carga, a requisição Java trava indefinidamente até o timeout padrão do SO, propagando falha em cascata.

---

## 3. Integrações Externas Satelitais e Geocientíficas

### 3.1. NASA Open Data API (`api.nasa.gov`)
* **Endpoint Utilizado:** `https://api.nasa.gov/planetary/earth/assets` via `NasaOpenDataClient.java`.
* **Parâmetros:** `lon`, `lat`, `api_key`.
* **Diagnóstico Crítico:**
  * O endpoint `/planetary/earth/assets` pertence à API de observação terrestre óptica da NASA (primariamente baseada em imagens Landsat 8 multiespectrais).
  * **Essa integração NÃO fornece dados de radar SAR nem fase interferométrica.**
  * Além disso, a NASA alterou a política e estabilidade desse endpoint em anos recentes, exigindo data explícita e retornando erro 404 para diversas regiões e datas, o que causa frequentes interrupções no fluxo do backend.

### 3.2. JAXA ALOS-PALSAR
* **Evidência no Código:** Interface `AlosPalsarClient.java`:
  ```java
  @FeignClient(name = "alosPalsarClient", url = "https://api.alospalsar.jaxa.jp")
  public interface AlosPalsarClient {
      @GetMapping("/images")
      byte[] baixarImagem(@RequestParam("id") String idImagem);
  }
  ```
* **Diagnóstico:**
  * O domínio `https://api.alospalsar.jaxa.jp` **não existe publicamente** e não corresponde aos servidores da JAXA.
  * O manual oficial arquivado no repositório (`JJ-FAST_API_UsersGuide_v1_1.pdf`) define que a API oficial da JAXA para alertas de radar ALOS-2 opera sob o endpoint:
    `https://www.eorc.jaxa.jp/cgi-bin/jjfast/api/getlist.cgi?lat=XX.X&lon=xxx.x&date=YYYYMMDD`
  * Essa interface Feign jamais foi injetada ou invocada em nenhum serviço do sistema (código morto).

### 3.3. USGS Earthquake Hazards Program
* **Evidência no Código:** Interface `UsgsEarthquakeClient.java`:
  * Mapeia `https://earthquake.usgs.gov/fdsnws/event/1/query`.
* **Diagnóstico:** Interface sintaticamente válida para consulta de catálogo sísmico, porém nunca utilizada em nenhuma regra de negócio.

### 3.4. ESA Copernicus e NASA ASF (Alaska Satellite Facility)
* **Diagnóstico:** **Completamente ausentes.** Não há nenhum cliente, biblioteca (como `sentinelsat`, `asf_search` ou `cdse-api`), chave de API ou script de autenticação configurado para as duas principais fontes abertas de dados SAR do mundo.

---

## 4. Integração com Nuvem (Amazon Web Services — AWS S3)

### 4.1. Configuração e Autenticação
* O backend utiliza a biblioteca oficial `software.amazon.awssdk:s3` versão 2.20.43.
* O arquivo `S3Config.java` configura um cliente assíncrono Netty (`S3AsyncClient`) apontando para `Region.US_EAST_1`.
* **Risco Operacional:** Configurado com `ProfileCredentialsProvider.create("terraform")`. O cliente busca explicitamente um perfil nomeado `terraform` no arquivo de credenciais da máquina local (`~/.aws/credentials`). Ambientes sem essa configuração quebram em runtime.

### 4.2. Fluxo de Armazenamento e Recuperação de Imagens
1. **Upload (`StorageService.uploadFile`):**
   * Cria uma nova instância síncrona de `S3Client` a cada chamada.
   * Constrói a chave: `imagens/deformacao-{id}/nasa_original.png` ou `imagens/deformacao-{id}/fastapi_processada.png`.
   * Envia os bytes com content-type fixo `image/png`.
2. **Download (`StorageService.downloadFile`):**
   * Hardcodeia o bucket name (`String bucketName = "radar-imagesbucket"`).
   * Hardcodeia a chave de busca para: `imagens/deformacao-68/{nomeImagem}`.
   * Executa `future.get()` bloqueando a thread de execução até que o Netty finalize a transferência.

### 4.3. Anti-Pattern de Duplicidade de Armazenamento
* O sistema salva a imagem bruta duas vezes:
  1. No bucket S3 como arquivo.
  2. No banco de dados relacional PostgreSQL como Large Object OID na coluna `imagem_radar.dados_brutos`.
* Essa duplicação sobrecarrega o banco de dados desnecessariamente, violando as melhores práticas de armazenamento de objetos em nuvem.

---

## 5. Rastreabilidade de Imagens e Dados Satelitais

| Pergunta de Auditoria | Situação Atual | Evidência Técnica |
|---|---|---|
| **O sistema sabe qual cena satelital gerou a análise?** | **NÃO** | A tabela `imagem_radar` grava apenas `origem` ("NASA" ou "FASTAPI") e a data de hoje (`LocalDate.now()`). Não armazena ID de órbita, passagem, sensor, satélite ou Scene ID. |
| **Os identificadores possuem origem rastreável?** | **NÃO** | O campo `id` da resposta da NASA é descartado; apenas a URL de download é utilizada temporariamente. |
| **É possível reproduzir ou reprocessar uma análise?** | **NÃO** | Como os metadados de aquisição e o par interferométrico original não são registrados, uma análise não pode ser auditada cientificamente. |

---

## 6. Auditoria de Segurança e Exposição de Credenciais

Em conformidade com a Regra Obrigatória nº 9, **valores de segredos e credenciais não são impressos neste relatório**. Registramos a sua existência, localização e gravidade do risco associado:

| Tipo de Segredo | Localização Identificada | Mecanismo de Exposição | Risco Associado | Severidade |
|---|---|---|---|---|
| **AWS Access Key & Secret Key** | `api-radar-deformacao/src/main/resources/application.properties` (linhas 22-23) | Credenciais ativas reais da AWS em texto claro em arquivo de configuração sob controle de versão local. | Comprometimento total da conta AWS, risco de custos elevados por provisionamento não autorizado e vazamento de dados do bucket S3. | **CRÍTICA** |
| **NASA API Key** | `api-radar-deformacao/src/main/resources/application.properties` (linha 17) | Chave pessoal da API NASA em texto claro. | Revogação de chave, rate limiting e bloqueio de acesso ao serviço. | **ALTA** |
| **Stormglass / Weather API Key** | `api-radar-deformacao/src/main/resources/application.properties` (linha 18) | Chave de serviço de meteorologia em texto claro (serviço nem sequer é utilizado pelo código). | Exposição desnecessária de credencial de serviço terceiro. | **MÉDIA** |
| **GitHub Personal Access Token (PAT)** | `front_react/github-friend-helper/.git/config` (remoto `origin`) | Token pessoal clássico do GitHub embutido diretamente na URL do repositório remoto. | Acesso não autorizado a repositórios privados da organização Led-ds no GitHub. | **CRÍTICA** |
| **Credenciais de Banco de Dados** | `api-radar-deformacao/src/main/resources/application.properties` (linhas 7-8) | Usuário e senha do PostgreSQL fixados em texto claro (`ass / ass`). | Risco padrão em ambientes locais; não deve ser promovido a ambientes de homologação/produção. | **MÉDIA** |

### Recomendação Imediata de Segurança:
1. Revogar imediatamente a Access Key da AWS e gerar novo par de chaves restrito a políticas de IAM de menor privilégio (*Least Privilege*).
2. Revogar imediatamente o GitHub Personal Access Token e atualizar a URL remota do repositório para autenticação via SSH (`git@github.com:...`).
3. Externalizar todas as credenciais sensíveis para variáveis de ambiente do sistema (`export AWS_ACCESS_KEY_ID=...`) ou gerenciador de segredos (AWS Secrets Manager / Vault), utilizando referências no Spring: `aws.s3.access-key=${AWS_ACCESS_KEY_ID}`.
4. Adicionar arquivos de configuração contendo senhas ao `.gitignore`.
