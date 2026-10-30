# Radar++ — Visão do Produto e Posicionamento Estratégico

**Documento:** `01-visao-produto.md`  
**Ciclo:** Ciclo 1 — Visão do Produto e Definição do MVP  
**Data:** Outubro de 2026  
**Status:** Proposto para Aprovação  
**Autores:** Equipe de Arquitetura de Software e Engenharia de Produto  
**Referências:** Documentos de auditoria em `docs/audit/` ([00-radar-plus-panorama-executivo.md](file:///home/asoares/Documentos/repositories/repo_radar/docs/audit/00-radar-plus-panorama-executivo.md), [08-problemas-e-riscos.md](file:///home/asoares/Documentos/repositories/repo_radar/docs/audit/08-problemas-e-riscos.md), [09-estrategia-evolucao.md](file:///home/asoares/Documentos/repositories/repo_radar/docs/audit/09-estrategia-evolucao.md)).

---

## 1. Missão do Produto

Democratizar e simplificar o acesso à **análise e ao monitoramento científico de deformações da superfície terrestre**, transformando dados complexos de **Radar de Abertura Sintética (SAR)** em informações geoespaciais rastreáveis, compreensíveis e operacionais para engenheiros, geólogos e analistas de infraestrutura.

---

## 2. O Problema Real

### 2.1. O Desafio Geotécnico e de Infraestrutura
Movimentações de massa, subsidências de solo urbano, instabilidades em taludes de mineração, recalques em barragens e deslizamentos em encostas causam anualmente perdas humanas catastróficas e prejuízos econômicos bilionários. O monitoramento tradicional baseado em instrumentação geotécnica terrestre (marcos topográficos, piezômetros, inclinômetros e estações GNSS):
* Possui cobertura espacial pontual e limitada.
* Apresenta custo de implantação e manutenção extremamente elevado.
* Exige acesso físico constante a áreas muitas vezes perigosas ou inacessíveis.

### 2.2. A Barreira de Adoção do Sensoriamento Remoto por Radar (SAR/InSAR)
A Interferometria por Radar de Abertura Sintética (InSAR) é a tecnologia padrão da geociência para detectar deslocamentos milimétricos da crosta terrestre a partir do espaço, com capacidade de observação dia/noite e penetração através de nuvens. Contudo, seu uso prático é severamente limitado por:
* **Complexidade Algorítmica Extrema:** Exige conhecimentos avançados de propagação de ondas eletromagnéticas em micro-ondas, corregistro submétrico, remoção de fase topográfica e algoritmos complexos de desembrulhamento de fase (*phase unwrapping*).
* **Sobrecarga de Dados:** Cenas SAR complexas de nível 1 (*Single Look Complex* — SLC) possuem arquivos de 4 GB a 8 GB cada, exigindo infraestrutura de rede, disco e processamento de alta performance.
* **Ferramentas Especializadas e Inacessíveis:** As ferramentas científicas existentes (SNAP, ISCE2, GMTSAR, StaMPS) possuem interfaces desktop arcaicas, pipelines em linha de comando de difícil automação e curvas de aprendizado íngremes.
* **Falta de Plataformas Modernas e Integradas:** O mercado carece de soluções web que unam a facilidade de uso de aplicações geoespaciais modernas ao rigor metodológico da geodésia por satélite.

---

## 3. Proposta de Valor do Radar++

O Radar++ atua como a **ponte tecnológica** entre os acervos abertos de dados orbitais de radar e os profissionais responsáveis pela segurança e manutenção de territórios e infraestruturas:

```
+------------------------+      +---------------------------+      +--------------------------+
| Constelações Orbitais  | ---> |          RADAR++          | ---> | Profissionais e Decisores|
| (Copernicus Sentinel-1)|      |  Orquestração Científica, |      | Mapas de Deslocamento,   |
| (NASA / JAXA)          |      |  InSAR Automatizado & Web |      | Séries Temporais e Risco |
+------------------------+      +---------------------------+      +--------------------------+
```

### Pilares da Proposta de Valor:
1. **Rigor Geodésico e Científico:** Eliminação de aproximações empíricas inválidas (como distâncias euclidianas de tela ou modelos de IA treinados sobre ruído); todo cálculo é fundamentado em equações de fase de micro-ondas e geometria de visada orbital (LOS).
2. **Autonomia e Agilidade:** O analista delimita visualmente sua Área de Interesse (AOI), descobre automaticamente as imagens Sentinel-1 disponíveis, seleciona o par temporal ideal e dispara o processamento em poucos cliques.
3. **Transparência e Rastreabilidade Total:** Cada mapa e gráfico de deslocamento gerado possui procedência inequívoca, permitindo auditar as cenas satelitais de origem, as órbitas utilizadas, os modelos digitais de elevação e o índice de coerência interferométrica ($\gamma$).
4. **Interface Web Moderna e Fluida:** Navegação rápida, visualização em camadas de mapas (Leaflet / WebGL) e ferramentas de inspeção pontual acessíveis pelo navegador.

---

## 4. Objetivos Estratégicos

### 4.1. Objetivos do MVP (Ciclo 1 ao Ciclo 6)
* Permitir a definição de Áreas de Interesse (AOIs) no território brasileiro por meio de polígonos no mapa.
* Integrar com catálogo aberto de radar (Copernicus Data Space Ecosystem / ASF Vertex) para descoberta automatizada de cenas Sentinel-1 IW SLC.
* Oferecer mecanismo para montagem e validação de pares interferométricos adequados (critérios de linha de base temporal e perpendicular).
* Orquestrar o processamento interferométrico diferencial (DInSAR) de dois passos, gerando mapa de coerência e mapa de deslocamento relativo na linha de visada do satélite (LOS).
* Exibir os resultados geocodificados sobre o mapa interativo, permitindo inspeção de valores de deslocamento (em mm ou cm) e coerência por ponto.
* Fornecer histórico auditável de execuções com consulta de status de jobs.

### 4.2. Visão de Longo Prazo (Pós-MVP)
* Processamento InSAR de Séries Temporais Multitemporais (PS-InSAR / SBAS) para acompanhamento histórico de anos de deformação contínua.
* Decomposição vetorial das medições LOS em deslocamento vertical puro (afundamento/soerguimento) e horizontal leste-oeste (usando órbitas ascendentes e descendentes combinadas).
* Módulo de alertas inteligentes e notificações automáticas baseadas em limiares de velocidade de subsidência.
* Integração com sensores de solo (marcos geodésicos GNSS/RBMC do IBGE e piezômetros) para validação cruzada.
* Geração automatizada de laudos periciais e relatórios executivos de conformidade geotécnica em PDF.

---

## 5. Público-Alvo e Segmentos de Usuários

O Radar++ foi concebido para atender profissionais que demandam monitoramento de terreno de alta precisão:

| Segmento | Necessidade Primária | Como o Radar++ Agrega Valor |
|---|---|---|
| **Engenharia Geotécnica & Mineração** | Monitorar estabilidade de taludes, cavas de mineração, pilhas de estéril e bacias de rejeitos. | Visão ampla de toda a área de lavra a cada 6 ou 12 dias, identificando movimentações milimétricas antes de rupturas visíveis. |
| **Infraestrutura Linear & Transportes** | Inspecionar ferrovias, rodovias, dutos, pontes e túneis contra recalques e deslizamentos. | Rastreamento contínuo de centenas de quilômetros de malha sem necessidade de envio de equipes de campo a locais ermos. |
| **Defesa Civil & Gestão Pública** | Identificar áreas urbanas e periurbanas sujeitas a subsidência por extração de água/minérios ou risco geológico. | Mapeamento preventivo para planejamento urbano, zoneamento territorial e emissão de alertas precoces à população. |
| **Pesquisa e Consultoria Ambiental** | Mapear dinâmica de falhas geológicas, subsidência de aquíferos e impactos de grandes obras de engenharia. | Acesso rápido a produtos interferométricos padronizados sem a sobrecarga de configurar ambientes locais complexos de processamento. |

---

## 6. Posicionamento de Mercado

```
Complexidade de Uso
      ▲
      │   [ Ferramentas Científicas Desktop ]
      │   (SNAP, ISCE2, GMTSAR)
      │   - Altíssimo rigor científico
      │   - Interface arcaica, difícil automação, local
      │
      │                                   [ RADAR++ (Alvo) ]
      │                                   - Rigor científico InSAR
      │                                   - Interface web moderna e fluida
      │                                   - Catálogo e nuvem integrados
      │                                   - Foco em produtividade técnica
      │
      │   [ Visualizadores Genéricos GIS ]
      │   - Fáceis de usar
      │   - Sem capacidade InSAR real
      │   - Apenas imagens ópticas estáticas
      ▼
──────┴──────────────────────────────────────────────────────► Rigor Científico InSAR
```

O Radar++ posiciona-se no quadrante superior direito: **uma plataforma web intuitiva e moderna que preserva o rigor metodológico das ferramentas científicas especializadas**. Ele não busca competir com visualizadores ópticos genéricos, nem ser uma "caixa preta" mágica de inteligência artificial sem explicação física.

---

## 7. Oportunidades e Vantagens Técnicas

1. **Dados Gratuitos e Globais:** A constelação Sentinel-1 da ESA disponibiliza continuamente imagens de radar em banda C com revisita periódica sistemática e política de dados abertos (*Open Access*).
2. **Reaproveitamento de Ativos Existentes:** Conforme auditado no Ciclo 0, a interface do usuário desenvolvida em React com Tailwind CSS e shadcn/ui possui excelente apelo estético e estrutura de componentes madura, permitindo focar os esforços no domínio e na integração científica.
3. **Nuvem e Armazenamento Otimizado:** A utilização do formato Cloud Optimized GeoTIFF (COG) e buckets S3 privados com URLs pré-assinadas viabiliza alta performance de visualização com custos de armazenamento extremamente reduzidos.

---

## 8. Limitações Deliberadas do MVP

Para assegurar foco de entrega, controle de custos e consistência técnica, as seguintes restrições são adotadas deliberadamente para o primeiro MVP:

1. **Ausência de Diagnóstico Geotécnico Automático:** O sistema fornecerá mapas de deslocamento LOS e coerência, mas **não emitirá laudos automatizados de risco** ("Área Segura" ou "Risco Crítico"). A interpretação da gravidade física cabe ao profissional responsável.
2. **Escopo Monotemporal DInSAR (2 Cenas):** O MVP processará pares de duas imagens (DInSAR convencional). Análises temporais multitemporais complexas (PS-InSAR / SBAS com dezenas de cenas) ficam postergadas para ciclos futuros.
3. **Sensor Único Inicial:** Suporte exclusivo ao satélite **Sentinel-1** (modo Interferometric Wide Swath — IW, nível SLC). Sensores comerciais de alta resolução (TerraSAR-X, COSMO-SkyMed) ou banda L (ALOS-2) não serão abordados no MVP.
4. **Sem Integração com Sensores In Situ no MVP:** O cruzamento automatizado com dados terrestres de GNSS/estuturas físicas será tratado no Ciclo 6 (Validação Científica).
