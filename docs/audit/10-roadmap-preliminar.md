# Radar++ — Roadmap Preliminar de Evolução Técnica

**Documento:** `10-roadmap-preliminar.md`  
**Data da Auditoria:** Outubro de 2026  
**Status:** Concluído  
**Escopo:** Planejamento estruturado em 7 ciclos de engenharia para transição do protótipo atual para um produto de monitoramento InSAR operacional e cientificamente validado.

> [!NOTE]
> Este roadmap é preliminar e técnico. Ele fornece a sequência lógica de engenharia para viabilizar as capacidades de radar de abertura sintética (SAR) e deverá ser revisado e ajustado após o alinhamento de negócio com os responsáveis pelo Radar++.

---

## 1. Visão Geral dos Ciclos

```mermaid
gantt
    title Radar++ — Cronograma Estratégico de Evolução
    dateFormat  YYYY-MM-DD
    section Engenharia
    Ciclo 1: Domínio e Arquitetura           :active, c1, 2026-11-01, 14d
    Ciclo 2: Infraestrutura e Fundações       :c2, after c1, 14d
    Ciclo 3: Aquisição e Catálogo SAR        :c3, after c2, 21d
    Ciclo 4: Processamento SAR/InSAR          :c4, after c3, 28d
    Ciclo 5: Visualização Geoespacial        :c5, after c4, 14d
    Ciclo 6: Validação Científica             :c6, after c5, 14d
    Ciclo 7: Evolução Funcional do Produto   :c7, after c6, 21d
```

---

## 2. Detalhamento por Ciclo

### Ciclo 1: Domínio e Arquitetura
* **Objetivo:** Estabelecer a modelagem de domínio ubíqua de sensoriamento remoto por radar e definir os contratos arquiteturais do Monólito Modular.
* **Principais Entregas:**
  1. Glossário de Domínio e Linguagem Ubíqua (Área de Interesse — AOI, Cena SAR, Par Interferométrico, Coerência, Fase Topográfica, Deslocamento LOS).
  2. Desenho formal dos limites dos módulos (*Bounded Contexts*): `acquisition`, `processing`, `monitoring`, `presentation`.
  3. Especificação dos contratos OpenAPI (REST) e esquemas de eventos assíncronos.
  4. Higienização de segurança: revogação de chaves expostas e configuração de `.gitignore` rigoroso.
* **Dependências:** Conclusão e aprovação da auditoria do Ciclo 0.
* **Riscos:** Definição excessivamente abstrata de domínio sem validação com os requisitos dos cientistas de dados.
* **Critérios de Aceite:**
  * Documento de arquitetura aprovado pela liderança técnica.
  * Todas as credenciais ativas nos repositórios antigos devidamente revogadas e rotacionadas.

---

### Ciclo 2: Infraestrutura e Fundações
* **Objetivo:** Criar o ambiente de desenvolvimento reproduzível, esteira de CI/CD e infraestrutura de persistência isolada.
* **Principais Entregas:**
  1. Repositório unificado ou sincronizado com suporte a contêineres Docker e Docker Compose (PostgreSQL com extensão PostGIS, MinIO/LocalStack para S3, RabbitMQ/Redis para filas assíncronas).
  2. Pipeline de CI no GitHub Actions executando build, linters e suíte de testes automatizados a cada pull request.
  3. Esqueleto do Monólito Modular em Spring Boot 3.3+ com Java 21/17 e suporte a Clean Architecture.
  4. Configuração segura de variáveis de ambiente e gerenciador de segredos.
* **Dependências:** Ciclo 1.
* **Riscos:** Dificuldade na padronização de imagens Docker com bibliotecas C/C++ geoespaciais (GDAL, PROJ).
* **Critérios de Aceite:**
  * Comando único `docker compose up` sobe todo o ecossistema local funcional.
  * CI executando verde com testes unitários iniciais.

---

### Ciclo 3: Aquisição e Catalogação de Cenas SAR
* **Objetivo:** Conectar o sistema a fontes abertas reais de dados SAR (Copernicus e NASA/ASF) para descoberta e download automatizado de imagens de radar.
* **Principais Entregas:**
  1. Conector com a API do **Copernicus Data Space Ecosystem (CDSE)** para busca de produtos Sentinel-1 (SLC e GRD).
  2. Conector com a API do **NASA/ASF Vertex** (via biblioteca `asf_search`) para busca de metadados e pares interferométricos compatíveis.
  3. Módulo de catálogo no banco de dados para indexação de cenas: satélite, órbita (passagem ascendente/descendente), ângulo de incidência, polarização (VV/VH), data de aquisição e Bounding Box georreferenciada com PostGIS.
  4. Worker assíncrono para agendamento de download de cenas em segundo plano diretamente para o storage S3.
* **Dependências:** Ciclo 2.
* **Riscos:** Mudanças de cotas ou políticas de download nas APIs da ESA/NASA; necessidade de grandes volumes de armazenamento (cenas SLC possuem ~4 a 8 GB cada).
* **Critérios de Aceite:**
  * Consulta por polígono no Brasil retorna metadados de cenas reais Sentinel-1 dos últimos 90 dias.
  * Metadados indexados e consultáveis com rastreabilidade completa.

---

### Ciclo 4: Processamento SAR / InSAR
* **Objetivo:** Implementar a capacidade científica de processamento interferométrico para extração real de mapas de deformação milimétrica.
* **Principais Entregas:**
  1. Mecanismo de processamento InSAR automatizado (utilizando SNAP GPT em contêiner ou integração com a API de processamento em nuvem HyP3 da NASA/ASF).
  2. Pipeline de geração de interferogramas:
     * Corregistro fino de par mestre/escravo.
     * Subtração de fase topográfica com modelo digital de elevação (Copernicus DEM 30m).
     * Filtragem de Goldstein e cálculo do mapa de coerência ($\gamma$).
     * Desembrulhamento de fase 2D (SNAPHU).
     * Conversão para deslocamento geocodificado na linha de visada (LOS em cm/ano).
  3. Exportação de produtos padronizados em Cloud Optimized GeoTIFF (COG) e GeoJSON vetorial com estatísticas de deformação por pixel.
* **Dependências:** Ciclos 2 e 3.
* **Riscos:** Alto consumo de CPU/RAM em processamento local de pares interferométricos; complexidade de calibração de parâmetros atmosféricos.
* **Critérios de Aceite:**
  * Geração comprovada de um interferograma diferencial com mapa de deslocamento validado sobre uma região conhecida de subsidência (ex.: área de mineração ou vale sedimentar).

---

### Ciclo 5: Visualização Geoespacial e Interface do Usuário
* **Objetivo:** Evoluir o frontend React para exibir camadas raster georreferenciadas, polígonos de monitoramento e gráficos de séries temporais.
* **Principais Entregas:**
  1. Migração e refatoração da aplicação React existente (`front_react`), eliminando a lógica de Haversine e acoplando-a à nova API REST.
  2. Ferramenta de desenho de Áreas de Interesse (AOIs) no mapa (retângulos e polígonos livres via Leaflet-Geoman ou Mapbox GL).
  3. Sobreposição de camadas raster InSAR geocodificadas (mapas de deformação e coerência) com rampa de cores interativa e controle de opacidade.
  4. Painel de detalhes por ponto monitorado com gráfico de evolução temporal da deformação ao longo dos meses (série temporal milimétrica).
* **Dependências:** Ciclo 4.
* **Riscos:** Lentidão de renderização de grandes imagens matriciais no navegador se não forem convertidas para tiles piramidais (TMS/XYZ ou COG via tile server).
* **Critérios de Aceite:**
  * Usuário desenha um polígono no mapa, visualiza a sobreposição do raster de deformação com legenda de cores (mm/ano) e clica em um pixel para ver o gráfico histórico.

---

### Ciclo 6: Validação Científica, Rastreabilidade e Auditoria
* **Objetivo:** Garantir a conformidade metodológica, reprodutibilidade dos resultados e auditoria completa das medições.
* **Principais Entregas:**
  1. Rastreabilidade ponta a ponta: qualquer ponto ou polígono no mapa permite inspecionar quais cenas SAR (Scene IDs, órbitas, datas) deram origem àquela estimativa.
  2. Comparação dos resultados do Radar++ com dados de estações GNSS/GPS públicas (ex.: Rede Brasileira de Monitoramento Contínuo — RBMC do IBGE) para calibração de precisão.
  3. Relatório técnico de validação metodológica assinado por especialista em sensoriamento remoto.
* **Dependências:** Ciclos 4 e 5.
* **Riscos:** Divergências entre dados de satélite e dados de solo causadas por efeitos atmosféricos ou vegetação densa (perda de coerência em banda C).
* **Critérios de Aceite:**
  * Margem de erro do deslocamento InSAR calibrada e documentada contra marcos geodésicos terrestres conhecidos.

---

### Ciclo 7: Evolução Funcional do Produto (Alertas e Relatórios)
* **Objetivo:** Entregar os recursos de negócio avançados para gestão operacional de risco e suporte à tomada de decisão.
* **Principais Entregas:**
  1. Motor de regras de alerta com limiares configuráveis (ex.: alertas automáticos para taxas de deslocamento superiores a 15 mm/ano em áreas críticas).
  2. Notificações automáticas por e-mail e webhooks para órgãos ou clientes integrados.
  3. Geração de relatórios executivos e laudos técnicos em PDF com mapas, gráficos e sumários de estabilidade do terreno.
  4. Gestão multi-tenant de usuários, organizações e controle de acessos (RBAC).
* **Dependências:** Ciclo 6.
* **Riscos:** Sobrecarga de falsos positivos se os alertas não considerarem o índice de coerência interferométrica como filtro de confiabilidade.
* **Critérios de Aceite:**
  * Disparo automático de notificação com geração de relatório em PDF para anomalias geotécnicas detectadas em área de monitoramento contínuo.
