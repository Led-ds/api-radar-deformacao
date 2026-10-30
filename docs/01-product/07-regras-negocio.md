# Radar++ — Regras de Negócio e Restrições Científicas (RN)

**Documento:** `07-regras-negocio.md`  
**Ciclo:** Ciclo 1 — Visão do Produto e Definição do MVP  
**Data:** Outubro de 2026  
**Status:** Proposto para Aprovação  
**Autores:** Equipe de Arquitetura de Software e Engenharia de Produto  
**Referências:** [01-visao-produto.md](file:///home/asoares/Documentos/repositories/repo_radar/docs/01-product/01-visao-produto.md), [03-escopo-mvp.md](file:///home/asoares/Documentos/repositories/repo_radar/docs/01-product/03-escopo-mvp.md), [05-requisitos-funcionais.md](file:///home/asoares/Documentos/repositories/repo_radar/docs/01-product/05-requisitos-funcionais.md).

---

## 1. Visão Geral das Regras de Negócio

As Regras de Negócio do Radar++ dividem-se em dois eixos fundamentais:
1. **Regras Geocientíficas e Metodológicas:** Garantem a validade física e matemática do processamento de micro-ondas e da interferometria SAR (InSAR).
2. **Regras Operacionais e de Governança:** Garantem a estabilidade da plataforma, a integridade dos dados, a segurança de segredos e o controle de custos em nuvem.

---

## 2. Catálogo de Regras de Negócio

### RN-001 — Validação Topológica e Limite Espacial da Área de Interesse (AOI)
* **Tipo:** Regra Operacional e Geométrica
* **Descrição:** Toda Área de Interesse cadastrada no sistema deve ser um polígono geometricamente fechado, expresso no sistema de coordenadas geodésicas WGS84 (EPSG:4326), com área total não superior a **$2.500 \text{ km}^2$** (equivalente aproximado a um quadrado de 50 km x 50 km).
* **Fundamentação:** Limitar a área máxima previne o esgotamento de memória RAM no worker científico durante a etapa de desembrulhamento de fase (*phase unwrapping*) e mantém o tempo de processamento viável no MVP.
* **Critério de Bloqueio:** O sistema rejeita o cadastro via validação PostGIS (`ST_IsValid` e `ST_Area(geography)`) se a geometria possuir auto-interseções ou área $> 2.500 \text{ km}^2$.

---

### RN-002 — Restrição de Sensor e Modo Satelital no MVP
* **Tipo:** Regra Geocientífica
* **Descrição:** No primeiro MVP, o sistema processará exclusivamente produtos de radar de abertura sintética provenientes da constelação **Sentinel-1** (satélites Sentinel-1A e Sentinel-1C da ESA), no modo de aquisição **Interferometric Wide Swath (IW)** e formato **Single Look Complex (SLC)**.
* **Fundamentação:** O modo IW SLC em banda C (comprimento de onda $\lambda \approx 5.546 \text{ cm}$) possui revisita sistemática global, distribuição gratuita aberta e parâmetros de calibração orbital amplamente documentados.
* **Critério de Bloqueio:** Imagens ópticas (como NASA Landsat/MODIS), produtos já detectados em amplitude (GRD) ou imagens de outros satélites devem ser bloqueadas no catálogo do MVP.

---

### RN-003 — Compatibilidade Geométrica Mandatória do Par Interferométrico
* **Tipo:** Regra Geocientífica Crítica
* **Descrição:** Duas cenas SAR só podem compor um par interferométrico válido se satisfizerem simultaneamente a três condições de simetria orbital:
  1. **Mesma Órbita Relativa (Relative Orbit / Track):** Ambas as imagens devem ter sido adquiridas exatamente sob o mesmo trajeto orbital.
  2. **Mesma Direção de Passagem:** Ambas devem ser *Ascendentes* (satélite viajando de sul para norte) ou ambas *Descendentes* (norte para sul).
  3. **Mesma Polarização Primária:** O canal co-polarizado principal deve ser idêntico (preferencialmente **VV** para detecção de deformação de solo).
* **Fundamentação:** Cenas adquiridas de órbitas relativas diferentes ou direções opostas possuem ângulos de visada e azimute completamente distintos, resultando em descorrelação geométrica total ($\gamma \approx 0$), tornando matematicamente impossível extrair a fase de deformação.
* **Critério de Bloqueio:** A interface e o backend devem bloquear o botão de disparo de processamento se o usuário selecionar cenas de tracks ou direções distintas.

---

### RN-004 — Limite Máximo da Linha de Base Perpendicular ($B_\perp$)
* **Tipo:** Regra Geocientífica
* **Descrição:** A distância espacial perpendicular entre os dois trajetos orbitais no momento das passagens (linha de base perpendicular $B_\perp$) deve ter módulo estritamente menor que **$150 \text{ metros}$** ($|B_\perp| \le 150 \text{ m}$), sendo a faixa ideal $|B_\perp| < 80 \text{ m}$.
* **Fundamentação:** Para o radar em banda C do Sentinel-1, linhas de base perpendiculares elevadas provocam descorrelação espacial geométrica severa e tornam o interferograma excessivamente sensível a pequenos erros no modelo digital de elevação (DEM).
* **Critério de Bloqueio:** Pares com $|B_\perp| > 150 \text{ m}$ são marcados como inadequados e bloqueados por padrão na UI.

---

### RN-005 — Janela Temporal Admissível da Linha de Base Temporal ($B_t$)
* **Tipo:** Regra Geocientífica
* **Descrição:** A diferença temporal entre a passagem mestre e a passagem escravo ($B_t = |t_{\text{slave}} - t_{\text{master}}|$) deve situar-se obrigatoriamente entre **6 e 48 dias** ($B_t \in [6, 48] \text{ dias}$), priorizando múltiplos do ciclo orbital do Sentinel-1 (6, 12, 24 ou 36 dias).
* **Fundamentação:** Em regiões tropicais e subtropicais (como grande parte do Brasil), a rápida alteração na umidade do solo e o crescimento de vegetação causam rápida descorrelação temporal em banda C. Intervalos superiores a 48 dias em pares individuais DInSAR tipicamente inviabilizam o unwrapping de fase.
* **Critério de Bloqueio:** O catálogo não deve sugerir pares com defasagem superior a 48 dias no MVP.

---

### RN-006 — Requisito Mandatório de Modelo Digital de Elevação (DEM)
* **Tipo:** Regra Geocientífica
* **Descrição:** Toda formação de interferograma diferencial (DInSAR) deve obrigatoriamente incorporar a subtração da fase topográfica utilizando um Modelo Digital de Elevação (DEM) com resolução espacial igual ou superior a 30 metros (preferencialmente **Copernicus DEM GLO-30** ou **SRTM v3 30m**).
* **Fundamentação:** A fase interferométrica bruta ($\phi_{\text{interf}}$) contém tanto a topografia do terreno quanto a deformação superficial. A remoção precisa da fase topográfica sintética calculada a partir do DEM é o que permite isolar a parcela de deslocamento real:
  $$\phi_{\text{deformação}} = \phi_{\text{interf}} - \phi_{\text{topografia}}$$
* **Critério de Bloqueio:** Jobs InSAR não podem ser executados sem a validação do download e alinhamento da grade DEM sobre a AOI.

---

### RN-007 — Critério de Aceitabilidade da Coerência Interferométrica
* **Tipo:** Regra de Qualidade Científica
* **Descrição:** O mapa de coerência interferométrica ($\gamma$), cujos valores variam no intervalo contínuo de $0.00$ a $1.00$, deve ser calculado para toda a área. Para fins de interpretação técnica:
  * $\gamma \ge 0.60$: **Alta Confiabilidade** (superfícies rochosas, solo exposto, áreas urbanas, estruturas civis).
  * $0.30 \le \gamma < 0.60$: **Média Confiabilidade** (vegetação rasteira esparsa, áreas agrícolas em repouso).
  * $\gamma < 0.30$: **Baixa Confiabilidade / Descorrelação** (vegetação densa, florestas, corpos d'água ou áreas com chuva no momento da passagem).
* **Critério de Alerta:**
  1. Se a coerência média de toda a AOI for $\gamma_{\text{médio}} < 0.20$, o job deve ser considerado falho por descorrelação excessiva.
  2. Na inspeção pontual, qualquer pixel com $\gamma < 0.30$ deve exibir aviso mandatória de não-confiabilidade.

---

### RN-008 — Proibição Estrita de Diagnóstico Geotécnico Automático e Pseudo-Cálculos
* **Tipo:** Regra Ética e de Integridade Científica
* **Descrição:**
  1. É **expressamente proibido** no código do Radar++ calcular deformação através de distâncias euclidianas ou fórmulas geodésicas de tela (como a fórmula de Haversine entre cliques de usuário).
  2. É **expressamente proibido** classificar uma área como "Estável" ou "Instável" de forma automatizada sem a emissão de laudo técnico por profissional legalmente habilitado (Engenheiro ou Geólogo).
* **Fundamentação:** A auditoria do Ciclo 0 identificou que tais aproximações artificiais induzem a falsas sensações de segurança e desmoralizam o produto sob escrutínio de engenharia.
* **Critério de Bloqueio:** O sistema deve reportar estritamente o valor físico medido de deslocamento na Linha de Visada (LOS) em mm e o índice de qualidade ($\gamma$), sem emitir juízo categórico de risco estrutural.

---

### RN-009 — Imutabilidade de Análises e Artefatos Científicos
* **Tipo:** Regra de Integridade de Dados e Rastreabilidade
* **Descrição:** Uma vez concluído um job InSAR, os registros da análise, seus parâmetros de execução e os rasters finais gerados tornam-se estritamente **imutáveis**.
* **Fundamentação:** Garante a idoneidade jurídica e científica da plataforma. Laudos periciais e relatórios de auditoria exigem que o histórico nunca seja alterado ou sobrescrito.
* **Critério de Bloqueio:** Nenhuma operação de `UPDATE` sobre os parâmetros orbitais ou URIs dos rasters de uma análise concluída é permitida na camada de persistência.

---

### RN-010 — Política de Descarte de Cenas Brutas SLC e Retenção de COGs
* **Tipo:** Regra Operacional e de Gestão de Custos
* **Descrição:**
  1. Cenas brutas Sentinel-1 SLC (arquivos compactados `.zip` de 4 GB a 8 GB) baixadas para o processamento devem ser mantidas em armazenamento temporário (*scratch/staging*) e **excluídas imediatamente após a geração e validação dos produtos finais**.
  2. Apenas os produtos finais (Cloud Optimized GeoTIFF — COG de deslocamento e COG de coerência, com tamanho médio $< 25 \text{ MB}$) devem ser transferidos para o armazenamento permanente no S3.
* **Fundamentação:** Evita a explosão de custos de armazenamento em nuvem (armazenar permanentemente 200 cenas SLC custaria centenas de dólares por mês sem necessidade).

---

### RN-011 — Acesso e Expiração de Artefatos no Storage S3
* **Tipo:** Regra de Segurança
* **Descrição:** O frontend e os usuários nunca devem ter acesso público direto a objetos no Amazon S3. A visualização e o download de rasters devem ocorrer exclusivamente por meio de **URLs pré-assinadas (Presigned URLs)** com tempo de expiração fixado em no máximo **60 minutos**.
* **Fundamentação:** Previne vazamento de dados de clientes, links compartilhados indevidamente e ataques de negação de serviço de download.

---

### RN-012 — Unicidade e Não-Concorrência de Jobs Ativos por Par e AOI
* **Tipo:** Regra de Concorrência
* **Descrição:** O sistema não deve permitir a criação de um novo job de processamento para a mesma AOI utilizando exatamente o mesmo par de cenas SAR caso já exista um job no estado `PENDING` ou `PROCESSING`.
* **Fundamentação:** Evita custos redundantes de processamento duplo disparados por cliques repetidos na interface.
