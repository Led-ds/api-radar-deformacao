# Radar++ — Estratégia de Evolução e Potencial de Reaproveitamento

**Documento:** `09-estrategia-evolucao.md`  
**Data da Auditoria:** Outubro de 2026  
**Status:** Concluído  
**Escopo:** Matriz de decisão de componentes (Preservar, Refatorar, Substituir, Descontinuar) e análise comparativa entre Estratégia A (Evolução Incremental) e Estratégia B (Reconstrução Controlada).

---

## 1. Classificação e Destino dos Componentes Existentes

Para maximizar a eficiência técnica e evitar desperdício de esforço, cada componente do ecossistema Radar++ foi avaliado segundo quatro categorias formais:

```
+-------------------------------------------------------------------------------+
|                       MATRIZ DE DISPOSIÇÃO DE COMPONENTES                     |
+-------------------+-------------------+-------------------+-------------------+
|    PRESERVAR      |     REFATORAR     |    SUBSTITUIR     |   DESCONTINUAR    |
| (Manter / Adotar) | (Melhorar código) | (Nova solução)    | (Eliminar do repo)|
+-------------------+-------------------+-------------------+-------------------+
```

### 1.1. Componentes para PRESERVAR (Aproveitamento Direto)
* **Design System e Componentes UI em React (`front_react/.../src/components/ui/`):**
  * *Justificativa:* O conjunto de componentes shadcn/ui e Radix UI configurado no frontend é moderno, responsivo, acessível e profissional. Deve ser mantido como biblioteca base de componentes.
  * *Impacto:* Economiza semanas de trabalho de estilização visual e criação de primitivas de interface (diálogos, popovers, botões, formulários).
* **Estrutura de Visualização Modal de Imagem (`ImageViewer.tsx`):**
  * *Justificativa:* O componente cumpre perfeitamente o papel de apresentar imagens ampliadas com controle de acessibilidade e fecho via backdrop/esc.
  * *Impacto:* Reutilização imediata para exibir mapas de coerência e interferogramas gerados no futuro.

---

### 1.2. Componentes para REFATORAR (Reaproveitáveis com Correções)
* **Frontend de Mapa (`MonitoringMap.tsx`):**
  * *Justificativa:* A integração do React-Leaflet com OpenStreetMap está funcional e bem renderizada.
  * *O que refatorar:* Remover completamente a função `calculateDisplacement` (Haversine); substituir o clique pontual por suporte a seleção de polígono/Área de Interesse (AOI); dissociar o formulário em componente próprio; e consumir dados reais de séries temporais via React Query sem forçar reload de página.
* **Cliente de Armazenamento S3 (`StorageService.java` & `S3Config.java`):**
  * *Justificativa:* A biblioteca AWS SDK v2 já está instalada e configurada com cliente assíncrono Netty.
  * *O que refatorar:* Eliminar o hardcoding do bucket e da pasta fixa `deformacao-68`; remover a duplicidade de clientes síncronos e assíncronos; e adotar `DefaultCredentialsProvider` com credenciais lidas de variáveis de ambiente.
* **Entidades de Domínio e Banco Relacional (`Deformacao`, `ImagemRadar`, `AnaliseResultado`):**
  * *Justificativa:* A modelagem relacional de relacionar uma análise a imagens e resultados é um ponto de partida lógico.
  * *O que refatorar:* Remover o atributo `@Lob oid dadosBrutos` (não salvar bytes binários no Postgres); adicionar campos essenciais de rastreabilidade SAR (Scene ID, Satélite, Modo de Aquisição, Polarização, Data do Par Mestre/Escravo, Linha de Base Perpendicular $B_{\perp}$ e Perpendicular Temporal $B_t$); e aplicar conceitos de Aggregate Root com métodos de negócio ricos.

---

### 1.3. Componentes para SUBSTITUIR (Implementação Inadequada aos Objetivos)
* **Microsserviço de Análise e Modelo de IA (`radar_fastapi_py`):**
  * *Justificativa:* O microserviço atual com Keras treinado sobre ruído aleatório uniforme e redimensionamento 64x64 por PIL é cientificamente nulo.
  * *Substituto:* Implementar um módulo de processamento InSAR em Python utilizando ferramentas consolidadas da geociência (ex.: scripts automatizados baseados em **ISCE2**, **SNAP via GPT/snappy**, **MintPy** para séries temporais ou integração com a API **HyP3** da NASA/ASF).
* **Integração Satelital Óptica da NASA (`NasaOpenDataClient.java`):**
  * *Justificativa:* Fornece fotografias ópticas terrestres, não radar de abertura sintética.
  * *Substituto:* Implementar clientes de catálogo e download para a **Copernicus Data Space Ecosystem (CDSE)** da ESA (dados Sentinel-1 SLC) e **NASA/ASF Vertex** (dados ALOS-2 e Sentinel-1).
* **Módulo de Processamento com GeoTools/JAI (`GeoToolsProcessor.java`):**
  * *Justificativa:* A cadeia de fallbacks com try/catch aleatórios de operações JAI e envelopes artificiais baseados em Haversine é tecnicamente insustentável no Java 17.
  * *Substituto:* Delegar o processamento raster pesado e georreferenciamento ao ambiente científico Python com **GDAL / Rasterio / Xarray**, mantendo o backend Java focado na orquestração de negócios.

---

### 1.4. Componentes para DESCONTINUAR (Eliminar Imediatamente)
* **Frontend Legado Angular (`monitorar-images-alos-palsar`):** Repositório abandonado que gera confusão e duplicidade de manutenção. Deve ser arquivado ou removido do workspace ativo.
* **Lógica de Cálculo Geodésico de Haversine (`CalculoGeoespacial.java`):** Código que induz ao erro de confundir distância entre coordenadas com subsidência/deformação.
* **Cliente Fictício JAXA (`AlosPalsarClient.java`):** Código morto apontando para domínio inexistente.
* **Cliente Inutilizado USGS (`UsgsEarthquakeClient.java`):** Código morto sem aderência ao fluxo atual.
* **Classes de Teste Scratch e Rascunhos (`Main.java`, `ProcessarImagemSAR.java`, `AnaliseSolo.java`):** Artefatos incompletos que poluem a base.

---

## 2. Comparativo Estratégico: Evolução Incremental vs Reconstrução Controlada

Para definir o rumo do Radar++, comparamos duas rotas de engenharia viáveis:

* **Estratégia A — Evoluir os Projetos Existentes:** Manter os repositórios atuais e tentar refatorar incrementalmente cada classe Java, rota Python e componente React, corrigindo os bugs e injetando as bibliotecas SAR no código existente.
* **Estratégia B — Reconstrução Controlada (Clean Foundation):** Estabelecer uma nova fundação arquitetural padronizada (Monólito Modular no backend ou estrutura unificada de repositório), migrando e aproveitando seletivamente os componentes úteis (UI React, schemas, configs válidas) e descartando o débito técnico acumulado.

| Critério de Comparação | Estratégia A: Evolução Incremental | Estratégia B: Reconstrução Controlada | Vencedor |
|---|---|---|---|
| **Esforço Total** | Alto (requer desfazer acoplamentos, lidar com dependências obsoletas do JAI/GeoTools 26 no Java 17 e depurar código de terceiros). | Médio (o volume total de código útil a migrar é de apenas ~1.000 LOC; começar limpo é muito mais rápido). | **Estratégia B** |
| **Risco Técnico** | Altíssimo (risco de manter premissas erradas no modelo de dados e sofrer com quebras de runtime do Spring Cloud / JAI). | Baixo (arquitetura desenhada desde o dia 1 com contratos explícitos e tipagem estrita). | **Estratégia B** |
| **Manutenibilidade Futura** | Baixa a Média (manterá cicatrizes de 4 repositórios fragmentados e históricos Git confusos). | Altíssima (código limpo, modular, documentado e com padrões consistentes). | **Estratégia B** |
| **Qualidade Técnica e Arquitetural** | Comprometida (difícil transformar a estrutura atual de serviços anêmicos em DDD puro sem reescrever quase tudo). | Excelente (Clean Architecture, DDD e boundaries bem estabelecidos desde o início). | **Estratégia B** |
| **Complexidade Operacional** | Alta (4 repositórios, portas díspares 8080/8081/8082, problemas crônicos de CORS e tokens expostos). | Baixa (estrutura coesa com Docker Compose local e orquestração centralizada). | **Estratégia B** |
| **Tempo Relativo para o MVP** | Lento (tentar "consertar" o GeoTools/JAI para SAR consumirá semanas sem garantia de sucesso científico). | Mais Rápido (conecta diretamente uma UI React limpa a um backend que delega o processamento SAR a uma ferramenta real como SNAP/HyP3). | **Estratégia B** |
| **Preservação de Investimentos** | Aparente preservação de 100% dos repositórios, mas 70% do código atual não é aplicável a SAR. | Real: preserva 100% dos ativos de valor real (UI React, shadcn, modelos de dados depurados e pipeline S3). | **Estratégia B** |

---

## 3. Recomendação Técnica Fundamentada

### Recomendação Final: **ESTRATÉGIA B — RECONSTRUÇÃO CONTROLADA**

### Justificativa Arquitetural:
1. **O custo de refatorar supera o de reconstruir:** O projeto inteiro possui cerca de **2.000 linhas de código proprietário**. Não estamos diante de um sistema legado de 500.000 linhas cuja regra de negócio esteja oculta.
2. **O domínio principal não existe no código atual:** O Radar++ precisa fazer interferometria de radar (InSAR). O código existente faz tratamento de fotos ópticas e cálculo de distância de mouse. Tentar "refatorar" `GeoToolsProcessor.java` para fazer corregistro e unwrapping de fase é inviável, pois a biblioteca GeoTools não foi projetada para processamento interferométrico de sinais de radar complexos.
3. **Segurança e Higiene de Repositório:** O ecossistema atual possui credenciais ativas em histórico Git, tokens PAT em remotos e diretórios inteiros de `venv` e `node_modules` misturados ao versionamento. Uma fundação limpa elimina imediatamente essas vulnerabilidades.
4. **Como executar a Reconstrução Controlada com Máximo Reaproveitamento:**
   * **No Frontend:** Copiar integralmente o diretório `front_react/github-friend-helper` para a nova base, renomeando o projeto para `radar-ui`, expurgando a lógica de Haversine e conectando-o à nova API.
   * **No Backend:** Criar uma nova aplicação Spring Boot 3.3+ com estrutura de **Monólito Modular** (`acquisition`, `processing`, `monitoring`, `presentation`), aproveitando a configuração do AWS SDK v2 e os DTOs depurados.
   * **No Processamento Científico:** Criar um módulo ou serviço assíncrono em Python focado em sensoriamento remoto real (integrando com HyP3 da NASA/ASF ou motor local com Docker e SNAP/ISCE2).
