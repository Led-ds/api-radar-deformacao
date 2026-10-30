# Radar++ — Diagnóstico Consolidado de Problemas e Riscos

**Documento:** `08-problemas-e-riscos.md`  
**Data da Auditoria:** Outubro de 2026  
**Status:** Concluído  
**Escopo:** Mapeamento categorizado de todas as inconformidades técnicas, conceituais, de segurança e de negócio identificadas no Radar++.

---

## 1. Classificação de Severidade

* **CRÍTICO:** Inviabiliza a proposta de valor científica do produto, expõe credenciais de alto privilégio ou causa quebras estruturais imediatas em tempo de execução.
* **ALTO:** Compromete a integridade dos dados, gera acoplamento severo, introduz simulações artificiais no lugar de regras de negócio ou impede a manutenção normal.
* **MÉDIO:** Débito técnico relevante, falta de padronização, configurações frágeis ou ineficiência de recursos.
* **BAIXO:** Oportunidades de melhoria cosmética, organização de arquivos ou limpeza de código morto.

---

## 2. Matriz Diagnóstica de Problemas

### 2.1. Processamento Científico e Modelagem de Domínio

#### P01 — Distância Geodésica entre Coordenadas Interpretada como Deformação de Terreno
* **Severidade:** **CRÍTICA**
* **Evidências:**
  * `front_react/github-friend-helper/src/components/MonitoringMap.tsx` (linhas 44-59 e 81-87).
  * `api-radar-deformacao/src/main/java/com/ass/br/apiradar/infrastructure/component/CalculoGeoespacial.java` (linhas 47-59).
  * `monitorar-images-alos-palsar/src/app/monitoramento/monitoramento.component.ts` (linhas 47-67).
* **Descrição Técnica:** O sistema utiliza a fórmula esférica de Haversine para calcular a distância entre duas coordenadas geográficas consecutivas clicadas pelo usuário no mapa e multiplica esse valor por 100.000 para convertê-lo em centímetros, gravando o resultado no campo `deslocamento` como a medida de deformação física da superfície terrestre.
* **Impacto Funcional:** Invalidade científica total. O produto afirma monitorar deformações milimétricas ou centimétricas do terreno via radar, mas na realidade apresenta a distância espacial entre cliques do cursor do mouse.
* **Recomendação:** Descontinuar integralmente essa lógica. A deformação do terreno deve ser derivada exclusivamente da diferença de fase interferométrica ($\Delta\phi$) entre aquisições SAR em pares coerentes ao longo do tempo.
* **Esforço Estimado:** Médio (Requer reformulação do modelo de entrada e dos casos de uso de monitoramento).

---

#### P02 — Rede Neural Convolucional Treinada sobre Ruído Aleatório Puro
* **Severidade:** **CRÍTICA**
* **Evidências:** `radar_fastapi_py/models/train_model.py` (linhas 27-32) e `radar_fastapi_py/app/routes/analyze.py` (linhas 47-50).
* **Descrição Técnica:** O modelo `meu_modelo.h5` foi treinado com 100 amostras sintéticas de ruído branco uniforme geradas por `np.random.random((100, 64, 64, 3))` associadas a rótulos binários pseudo-aleatórios (`np.random.randint(2)`).
* **Impacto Funcional:** A predição de "Stable" ou "Unstable" exibida na interface é equivalente a um sorteio aleatório, sem correlação com estabilidade geotécnica.
* **Recomendação:** Substituir o modelo toy por um pipeline analítico comprovado de processamento InSAR (ex.: ISCE2 / MintPy / SNAP) ou, se mantida a abordagem de Machine Learning, treinar modelos sobre datasets reais e validados de interferogramas georreferenciados (como produtos do LiCSAR ou COMET).
* **Esforço Estimado:** Alto.

---

#### P03 — Ausência Completa de Dados e Algoritmos SAR / InSAR
* **Severidade:** **CRÍTICA**
* **Evidências:** Todo o ecossistema de código (`api-radar-deformacao`, `radar_fastapi_py`, `front_react`).
* **Descrição Técnica:** Não existe código para leitura de dados complexos SLC, estimativa de coerência espacial, corregistro de imagens, geração de interferograma, remoção de fase topográfica ou unwrapping de fase. A única fonte integrada é a API óptica da NASA.
* **Impacto Funcional:** O produto atual não é um sistema SAR, mas sim um visualizador de fotos de satélite com filtros convolucionais 2D.
* **Recomendação:** Implementar um motor de processamento geocientífico real baseado em padrões consolidados de sensoriamento remoto por micro-ondas.
* **Esforço Estimado:** Muito Alto.

---

### 2.2. Segurança e Gestão de Segredos

#### P04 — Exposição de Credenciais Ativas da AWS e Chaves de API em Texto Claro
* **Severidade:** **CRÍTICA**
* **Evidências:** `api-radar-deformacao/src/main/resources/application.properties` (linhas 17-23).
* **Descrição Técnica:** O arquivo `application.properties` contém credenciais ativas reais da AWS (`aws.s3.access-key` e `aws.s3.secret-key`) e chaves da API da NASA e Weather API registradas em texto não criptografado.
* **Impacto:** Risco iminente de acesso não autorizado à infraestrutura em nuvem, exclusão de dados e cobranças financeiras indevidas.
* **Recomendação:** Revogar imediatamente as chaves no console AWS IAM, adotar variáveis de ambiente locais e configurar `application.properties` para ler via `${AWS_ACCESS_KEY_ID}` e `${AWS_SECRET_ACCESS_KEY}`.
* **Esforço Estimado:** Baixo (Ação imediata).

---

#### P05 — GitHub Personal Access Token Exposto na URL Remota do Git
* **Severidade:** **CRÍTICA**
* **Evidências:** `front_react/github-friend-helper/.git/config` (URL remota `origin`).
* **Descrição Técnica:** A URL remota do repositório Git contém um token de acesso pessoal (`github_pat_...`) exposto diretamente na string de conexão HTTP.
* **Impacto:** Qualquer pessoa ou ferramenta com acesso ao repositório local obtém privilégios de escrita e leitura na conta GitHub do desenvolvedor.
* **Recomendação:** Revogar o token imediatamente nas configurações de segurança do GitHub e reconfigurar a origem remota para usar chaves SSH.
* **Esforço Estimado:** Muito Baixo (Ação imediata).

---

#### P06 — Ausência de Mecanismos de Autenticação e Autorização
* **Severidade:** **ALTO**
* **Evidências:** `api-radar-deformacao/src/main/java/com/ass/br/apiradar/presentation/controllers/`.
* **Descrição Técnica:** Não há Spring Security configurado para proteção de rotas, geração de tokens JWT ou controle de sessões. Todas as rotas de criação, listagem e download de arquivos estão públicas.
* **Impacto:** Qualquer cliente na rede pode inserir registros espúrios, disparar processamento intensivo e baixar imagens de clientes sem controle.
* **Recomendação:** Implementar módulo de segurança baseado em Spring Security com autenticação JWT ou OAuth2.
* **Esforço Estimado:** Médio.

---

### 2.3. Arquitetura e Engenharia de Software

#### P07 — Recuperação de Imagens com Caminho Fixo no S3 (Mock em Produção)
* **Severidade:** **ALTO**
* **Evidências:** `StorageService.java` (linhas 62 e 66) e `mapService.ts` (linhas 47-48).
* **Descrição Técnica:** O método `downloadFile` do backend fixa a chave no S3 para `imagens/deformacao-68/{nomeImagem}`, e o frontend busca sempre `nasa_original.png` e `fastapi_processada.png`.
* **Impacto:** O sistema é incapaz de exibir imagens reais associadas a qualquer deformação diferente do ID 68.
* **Recomendação:** Parametrizar a rota do backend para receber o ID da deformação e o ID da imagem, buscando dinamicamente a URI armazenada na entidade `ImagemRadar`.
* **Esforço Estimado:** Baixo.

---

#### P08 — Erro Fatal de Cast de Tipos em `FeignConfig.java`
* **Severidade:** **ALTO**
* **Evidências:** `api-radar-deformacao/src/main/java/com/ass/br/apiradar/infrastructure/config/FeignConfig.java` (linha 19).
* **Descrição Técnica:** O código tenta realizar um cast explícito de um `java.util.List<HttpMessageConverter<?>>` para `org.springframework.boot.autoconfigure.http.HttpMessageConverters`, lançando `ClassCastException` na inicialização do encoder Feign.
* **Impacto:** Quebra em runtime da injeção de dependências do encoder de formulários multipart.
* **Recomendação:** Substituir o cast por `new HttpMessageConverters(messageConverters)`.
* **Esforço Estimado:** Muito Baixo.

---

#### P09 — Duplicidade Ineficiente de Armazenamento de Imagens (Postgres OID + S3)
* **Severidade:** **MÉDIO**
* **Evidências:** `ImagemRadar.java` (linhas 25-27) e `StorageService.java` (linha 55).
* **Descrição Técnica:** Imagens binárias completas são gravadas simultaneamente no bucket S3 e como objetos grandes OID (`@Lob`) dentro das tabelas do PostgreSQL.
* **Impacto:** Degradação de performance do banco de dados relacional, inchaço de backups e desperdício de armazenamento.
* **Recomendação:** Armazenar no banco de dados relacional apenas os metadados e o ponteiro/URL do S3; manter os arquivos binários exclusivamente no object storage.
* **Esforço Estimado:** Baixo.

---

#### P10 — Pipeline de Execução 100% Síncrono e Bloqueante
* **Severidade:** **MÉDIO**
* **Evidências:** `DeformacaoController.java:23` → `AnaliseService.java:37` → `ProcessamentoService.java:28`.
* **Descrição Técnica:** O cadastro de uma deformação bloqueia a thread HTTP enquanto baixa imagem da NASA, executa filtros GeoTools, faz upload no S3, aciona o FastAPI via HTTP e persiste no banco relacional.
* **Impacto:** Alta probabilidade de timeout no navegador do cliente (HTTP 504), baixa concorrência e desperdício de threads do Tomcat.
* **Recomendação:** Desacoplar a requisição da análise pesada. O endpoint deve registrar a intenção de análise, retornar `202 Accepted` com um Job ID e delegar o processamento pesado para um worker assíncrono (ex.: Spring `@Async`, RabbitMQ ou fila Celery).
* **Esforço Estimado:** Médio.

---

#### P11 — Dependência Rígida de Perfil AWS Local (`terraform`)
* **Severidade:** **MÉDIO**
* **Evidências:** `S3Config.java` (linha 27: `ProfileCredentialsProvider.create("terraform")`).
* **Descrição Técnica:** A inicialização do `S3AsyncClient` exige a presença de um perfil denominado `terraform` configurado no arquivo `~/.aws/credentials` da máquina host.
* **Impacto:** Falha imediata de inicialização do cliente S3 em qualquer ambiente novo ou contêiner Docker.
* **Recomendação:** Utilizar `DefaultCredentialsProvider.create()` para suporte automático a variáveis de ambiente, IAM Roles de EC2/ECS e perfis padrão.
* **Esforço Estimado:** Muito Baixo.

---

#### P12 — Ausência Total de Testes Automatizados (Cobertura 0%)
* **Severidade:** **MÉDIO**
* **Evidências:** Inexistência do diretório `src/test` no backend Java e ausência de testes em React e Python.
* **Descrição Técnica:** Não há testes unitários, testes de integração ou validações de ponta a ponta em nenhum dos quatro projetos do ecossistema.
* **Impacto:** Altíssimo risco de regressão a cada modificação, impossibilidade de refatoração segura e ausência de documentação executável das regras de negócio.
* **Recomendação:** Estabelecer suíte de testes com JUnit 5, Mockito, Testcontainers (PostgreSQL/LocalStack), Pytest e Vitest.
* **Esforço Estimado:** Médio.

---

#### P13 — Bug Fatal no Endpoint de Métricas do Python (`/metrics/`)
* **Severidade:** **MÉDIO**
* **Evidências:** `radar_fastapi_py/app/routes/analyze.py` (linha 67: `metrics = model.evaluate()`).
* **Descrição Técnica:** O método `model.evaluate()` do Keras exige tensores de entrada e rótulos alvo; chamá-lo sem argumentos dispara `TypeError`.
* **Impacto:** O endpoint `/metrics/` sempre retorna HTTP 500.
* **Recomendação:** Remover o endpoint ou fornecer um dataset de validação com métricas calculadas em offline.
* **Esforço Estimado:** Muito Baixo.

---

#### P14 — Frontend Recarregando a Página Inteira após Submissão (`window.location.reload`)
* **Severidade:** **BAIXO**
* **Evidências:** `front_react/github-friend-helper/src/components/MonitoringMap.tsx` (linha 98).
* **Descrição Técnica:** O callback de sucesso do formulário força `window.location.reload()`, destruindo o estado da Single Page Application (SPA).
* **Impacto:** Degradação da experiência do usuário (UX), perda de posição do mapa e piscar de tela.
* **Recomendação:** Atualizar o estado do React Query ou do array `points` via mutação local reativa.
* **Esforço Estimado:** Muito Baixo.

---

## 3. Resumo por Categoria e Esforço

| Categoria | Crítico | Alto | Médio | Baixo | Total |
|---|---|---|---|---|---|
| **Processamento Científico / Domínio** | 3 | 0 | 0 | 0 | **3** |
| **Segurança e Segredos** | 2 | 1 | 0 | 0 | **3** |
| **Arquitetura e Backend** | 0 | 2 | 3 | 0 | **5** |
| **Qualidade e Testes** | 0 | 0 | 2 | 0 | **2** |
| **Frontend / UX** | 0 | 0 | 0 | 1 | **1** |
| **TOTAL** | **5** | **3** | **5** | **1** | **14** |
