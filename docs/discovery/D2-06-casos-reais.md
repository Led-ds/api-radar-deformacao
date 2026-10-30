# Radar++ Discovery — D2-06: Casos Reais e Estudos de Referência de InSAR em Rodovias e Ferrovias
## Benchmark de Aplicações Nacionais e Internacionais e Potencial de Replicação

**Documento:** `D2-06-casos-reais.md`  
**Ciclo:** Investigação D2 — Infraestrutura Linear (Rodovias e Ferrovias)  
**Data:** 24 de Outubro de 2026  
**Status:** Oficial — Entregue para Governança  
**Responsável:** Especialista em Sensoriamento Remoto SAR/InSAR e Engenheiro Geotécnico  
**Repositório Oficial:** `api-radar-deformacao` (caminho: `docs/discovery/D2-06-casos-reais.md`)  

---

## 1. Visão Geral dos Casos de Estudo Mapeados

O uso da interferometria por radar em infraestruturas lineares de transporte possui um vasto acervo de artigos científicos e projetos industriais documentados, comprovando que a tecnologia é viável quando aplicada nos contextos geofísicos corretos:

```
+---------------------------------------------------------------------------------------------------------------+
|                                      CASOS DE REFERÊNCIA EM INFRAESTRUTURA LINEAR                             |
+----+----------------------+--------------------+---------------------+-----------------+----------------------+
| ID | Estudo / Corredor    | País / Região      | Modal de Transporte | Sensor Utilizado| Tipo de Fenômeno     |
+----+----------------------+--------------------+---------------------+-----------------+----------------------+
| C1 | Serra do Mar         | Brasil / SP        | Rodovia Imigrantes  | Sentinel-1 IW / | Escorregamento de    |
|    | (Sistema Anchieta)   | (EcoVias)          | e Anchieta (SP-160) | PALSAR (Banda L)| encostas e taludes   |
+----+----------------------+--------------------+---------------------+-----------------+----------------------+
| C2 | Malha Ferroviária    | Brasil / MG-RJ     | Ferrovia de Minério | Sentinel-1 /    | Rastejo (*creep*) de |
|    | da Mantiqueira       | (MRS Logística)    | Bitola Larga        | TerraSAR-X      | cortes rochosos      |
+----+----------------------+--------------------+---------------------+-----------------+----------------------+
| C3 | Network Rail (UK)    | Reino Unido /      | Rede Ferroviária    | Sentinel-1 /    | Recalques de aterros |
|    | Corredor Nacional    | Malha Inglesa      | Nacional Britânica  | ERS / Envisat   | e saias de lastro    |
+----+----------------------+--------------------+---------------------+-----------------+----------------------+
| C4 | Autostrade per       | Itália / Região    | Autoestradas e OAEs | COSMO-SkyMed /  | Recalque de pilares  |
|    | l'Italia (Viadutos)  | da Ligúria e Alpes | (Pontes e Viadutos) | Sentinel-1      | e aproximações       |
+----+----------------------+--------------------+---------------------+-----------------+----------------------+
```

---

## 2. Detalhamento dos Casos de Estudo

### 2.1. Caso C1 — Monitoramento na Serra do Mar / SP (Rodovias Anchieta e Imigrantes)
* **Local e Operador:** Serra do Mar, Estado de São Paulo, operada pela concessionária EcoVias (Grupo EcoRodovias) sob regulação da ARTESP.
* **Problema Geotécnico:** Encostas íngremes cobertas por Mata Atlântica sujeitas a chuvas intensas (pluviometria > 3.000 mm/ano). Histórico de escorregamentos que já interditaram a pista descendente da Imigrantes e a pista antiga da Anchieta.
* **Metodologia Científica:** Aplicação de *Persistent Scatterer Interferometry* (PSInSAR) com o algoritmo StaMPS e SBAS, focando em estruturas de concreto (pilares de viadutos da Imigrantes) e taludes de corte rochoso grampeados.
* **Sensores Utilizados:** Sentinel-1 (Banda C, 2015-2022) comparado retrospectivamente com dados históricos do satélite japonês ALOS-1/PALSAR (Banda L).
* **Resultados Documentados:** A Banda L manteve coerência nas encostas vegetadas onde a Banda C sofreu descorrelação total. Contudo, na faixa do pavimento da rodovia e nos muros de contenção de concreto, o Sentinel-1 permitiu identificar taxas de deslocamento milimétrico lento de 5 a 12 mm/ano em aterros de aproximação de pontes.
* **Potencial de Replicação no Radar++:** **Alto para a faixa de pista e contenções de concreto**, baixo para as encostas puramente florestadas fora da faixa de domínio.

### 2.2. Caso C2 — Corredor Ferroviário MRS Logística na Serra da Mantiqueira / MG-RJ
* **Local e Operador:** Malha Sudeste da MRS Logística, interligando a região de Juiz de Fora (MG) e Barra do Piraí (RJ) aos portos do Rio de Janeiro e Santos.
* **Problema Geotécnico:** Transporte pesado de minério de ferro com alta carga por eixo (30 toneladas/eixo). Cortes ferroviários profundos escavados em maciços de xisto e gnaisse intemperizados sujeitos a descompressão e deslizamentos lentos de talude que ameaçam a via permanente.
* **Metodologia:** Inversão SBAS (Small Baseline Subset) combinando imagens orbitais de radar com leituras de sensores in-situ (tiltímetros sem fio e inclinômetros) para calibração de modelos numéricos de estabilidade.
* **Sensores:** Sentinel-1 (catálogo aberto) e imagens sob demanda TerraSAR-X (Banda X comercial para maior densidade de pontos sobre os trilhos).
* **Resultados Documentados:** Identificação com 4 meses de antecedência de um talude de corte em processo de rastejo (*creep* de 25 mm acumulados), permitindo que a equipe de via permanente executasse o retaludamento preventivo antes do início da estação chuvosa.
* **Potencial de Replicação no Radar++:** **Totalmente replicável no Radar++**, utilizando a orquestração HyP3 + MintPy sobre dados abertos Sentinel-1 para detecção da taxa média de deformação ao longo dos cortes da linha.

### 2.3. Caso C3 — Network Rail (Reino Unido) — Monitoramento Sistemático da Malha Ferroviária
* **Local e Operador:** Rede ferroviária nacional do Reino Unido (Network Rail).
* **Problema:** Milhares de quilômetros de aterros ferroviários históricos construídos na era vitoriana (século XIX) com solos argilosos e métodos empíricos, sofrendo recalques cíclicos sazonais devido a variações de umidade no solo (*shrink-swell*).
* **Metodologia:** Monitoramento em escala nacional via InSAR (parceria com fornecedores e agências espaciais), integrando os mapas de velocidade anual diretamente ao sistema GIS corporativo de gestão de ativos da ferrovia.
* **Resultados:** A Network Rail utiliza o InSAR como ferramenta de **triagem de risco**, subsidiando a priorização do envio de equipes de campo e a alocação de orçamentos de recapeamento de lastro.
* **Lição para o Radar++:** Evidência internacional de que **o modal ferroviário representa um caso de uso promissor para a triagem macro por satélite**, cuja aplicabilidade comercial nas concessões brasileiras constitui hipótese a ser validada nas entrevistas de descoberta.

---

## 3. Matriz de Replicabilidade Científica no Radar++

```
+---------------------------------------------------------------------------------------------------------------+
|                                    AVALIAÇÃO DE REPLICABILIDADE NO MVP DO RADAR++                             |
+--------------------------+-----------------------+-----------------------------+------------------------------+
| Desafio de Replicabilidade| Nível de Dificuldade | O que Funciona no Radar++   | Onde o Radar++ Precisará de P&D|
+--------------------------+-----------------------+-----------------------------+------------------------------+
| Rastreio de Vias Férreas | MÉDIO                 | Trilhos metálicos são alvos | Exige máscaras de corte de   |
| em Corredores Abertos    |                       | naturais de alta coerência  | buffer de 100m ao longo do   |
|                          |                       | (*Persistent Scatterers*).  | shapefile do traçado da via. |
+--------------------------+-----------------------+-----------------------------+------------------------------+
| Taludes Rodoviários de   | FÁCIL A MÉDIO         | Concreto e asfalto geram    | Cobertura de pixels mistos se|
| Contenção Estruturada    |                       | sinal limpo e contínuo.     | o talude for muito estreito. |
+--------------------------+-----------------------+-----------------------------+------------------------------+
| Encostas Serranas com    | MUITO ALTO            | Apenas afloramentos rocho-  | Dependerá do futuro NISAR    |
| Floresta Densa           |                       | sos responderão na Banda C. | (Banda L) para cobertura total|
+--------------------------+-----------------------+-----------------------------+------------------------------+
```
