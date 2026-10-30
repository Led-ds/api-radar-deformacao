# Radar++ Discovery — D1-05: Protocolo Científico de Validação Retrospectiva — Piloto Maceió/AL
## Plano de Experimentação Metodológica DInSAR/SBAS contra Dados Publicados do SGB/CPRM

**Documento:** `D1-05-protocolo-piloto-maceio.md`  
**Ciclo:** Investigação D1 — Mercado e Viabilidade  
**Data:** 24 de Outubro de 2026  
**Status:** Protocolo Aprovado para Futura Execução — Não Executado Neste Ciclo  
**Responsável:** Especialista em Sensoriamento Remoto SAR/InSAR e Arquiteto de Software  
**Repositório Oficial:** `api-radar-deformacao` (caminho: `docs/discovery/D1-05-protocolo-piloto-maceio.md`)  
**Regra Estrita de Governança:** Este documento é um planejamento experimental científico. **Nenhum job de processamento, chamada de API paga ou consumo de recursos computacionais pesados deve ser executado neste ciclo.**  

---

## 1. Objetivo Científico do Piloto Retrospectivo

O objetivo deste protocolo é definir os parâmetros exatos para que, em ciclo futuro aprovado pela governança, a equipe técnica do Radar++ execute a **validação científica retrospectiva (*Back-Analysis*)** da metodologia interferométrica, confrontando os resultados gerados pelo pipeline proposto (NASA/ASF HyP3 + MintPy) contra o acervo consolidado e auditado do **Serviço Geológico do Brasil (SGB/CPRM)** no desastre de subsidência de Maceió/AL.

---

## 2. Área de Interesse (AOI) e Enquadramento Geográfico

A Área de Interesse engloba os bairros diretamente afetados pelo colapso das cavidades de sal-gema e a margem lagunar:

```
+---------------------------------------------------------------------------------------------------------------+
|                                    DELIMITAÇÃO DA ÁREA DE INTERESSE (AOI MACEIÓ)                              |
+--------------------------+------------------------------------------------------------------------------------+
| Parâmetro                | Especificação Técnica                                                              |
+--------------------------+------------------------------------------------------------------------------------+
| Localidade               | Município de Maceió, Estado de Alagoas, Brasil                                     |
| Bairros Englobados       | Pinheiro, Mutange, Bebedouro, Bom Parto, Farol, Levada e margem da Lagoa Mundaú   |
| Coordenadas Bounding Box | Latitudes:  -9.6600° S  a  -9.5800° S                                              |
| (WGS84 / SIRGAS 2000)    | Longitudes: -35.7900° W a  -35.7100° W                                             |
| Área Total Delimitada    | Aproximadamente 25 km²                                                             |
| Relevo e Altitude        | Terreno costeiro variando de 0 m (nível da lagoa) a ~60 m (tabuleiro sedimentar)   |
+--------------------------+------------------------------------------------------------------------------------+
```

```
Vértices do Polígono Oficial da AOI (Formato GeoJSON / WKT):
POLYGON((-35.7900 -9.5800, -35.7100 -9.5800, -35.7100 -9.6600, -35.7900 -9.6600, -35.7900 -9.5800))
```

---

## 3. Período Histórico Selecionado

* **Intervalo Temporal:** **1º de Janeiro de 2018 a 31 de Dezembro de 2021** (48 meses contínuos).
* **Justificativa Científica:**
  1. *Fevereiro/Março de 2018:* Ocorrência do abalo sísmico precursor (magnitude 2.4 mR) e abertura das primeiras fissuras em massa no bairro Pinheiro.
  2. *Maio de 2019:* Publicação do laudo técnico conclusivo do SGB/CPRM imputando a responsabilidade à extração de sal-gema.
  3. *2020-2021:* Período de subsidência acelerada contínua, desocupação dos bairros e ampla cobertura de dados pelo satélite Sentinel-1 em constelação dupla (Sentinel-1A e Sentinel-1B operando a cada 6 ou 12 dias).

---

## 4. Seleção de Cenas SAR e Compatibilidade Orbital

```
+---------------------------------------------------------------------------------------------------------------+
|                                   PARÂMETROS ORBITAIS E DE AQUISIÇÃO SENTINEL-1                               |
+--------------------------+------------------------------------------------------------------------------------+
| Critério                 | Configuração Obrigatória                                                           |
+--------------------------+------------------------------------------------------------------------------------+
| Missão e Sensor          | Copernicus Sentinel-1A e Sentinel-1B (Radar em Banda C: lambda = 5.546 cm)         |
| Modo de Operação         | IW (Interferometric Wide Swath) — Nível 1 SLC (Single Look Complex)                |
| Polarização              | Co-polarizada VV (Vertical-Vertical) — maior coerência em meio urbano              |
| Órbita / Direção         | Órbita Descendente (Descending Track)                                              |
| Relative Orbit (Track)   | **Track 142** (Consagrado na literatura como o de melhor ângulo e coerência)       |
| Ângulo de Incidência (0) | Aproximadamente 39° a 41° no centro da cena                                        |
| Linha de Base Perpendic. | B_perp < 100 metros (para minimizar erro decorrente de topografia residual)        |
| Linha de Base Temporal   | B_temp <= 24 a 36 dias (para garantir coerência interferométrica gama >= 0.40)    |
+--------------------------+------------------------------------------------------------------------------------+
```

---

## 5. Ferramental de Processamento e Pipeline Proposto

```
+--------------------+      +--------------------+      +--------------------+      +--------------------+
| ASF Vertex /       | ---> | NASA/ASF HyP3 API  | ---> | Storage Local / S3 | ---> | MintPy (Python)    |
| Catálogo Aberto    |      | (Burst InSAR, DEM  |      | Download dos COGs  |      | Inversão SBAS,     |
| Sentinel-1 IW SLC  |      | COP30, SNAPHU unw) |      | antes de 14 dias   |      | Séries Temporais   |
+--------------------+      +--------------------+      +--------------------+      +--------------------+
```

1. **Geração de Pares Interferométricos via NASA/ASF HyP3:**
   * Utilização da funcionalidade *Burst InSAR* (submissão apenas do burst específico de Maceió do Track 142, reduzindo o volume de transferência em mais de 80%).
   * Modelo Digital de Elevação (DEM): Copernicus GLO-30 (resolução espacial de 30 metros).
   * Desembrulhamento de fase (*phase unwrapping*): Algoritmo estatístico SNAPHU em grade retangular.
   * Entrega: Imagens geocodificadas e ortorretificadas em formato Cloud Optimized GeoTIFF (COG).
2. **Inversão Multitemporal e Deformação Acumulada via MintPy:**
   * Ingestão nativa dos arquivos unwrapped e mapas de coerência gerados pelo HyP3.
   * Correção atmosférica de atraso de fase troposférico utilizando o modelo meteorológico global **ERA5** (ECMWF).
   * Algoritmo de inversão: *Small Baseline Subset* (SBAS) com filtro de descarte de pixels incoerentes ($\gamma < 0{,}40$).

---

## 6. Artigos e Dados de Referência (*Benchmark Ground Truth*)

Para validação cruzada independente, o resultado do processamento será confrontado com:

1. **Relatório Técnico Oficial do SGB/CPRM (Maio de 2019):**
   * *"Estudos sobre a instabilidade do terreno no bairro Pinheiro, Maceió - AL"*. Repositório RIGEO ([rigeo.sgb.gov.br](https://rigeo.sgb.gov.br)).
   * Fornece mapas de isolinhas de subsidência acumulada e taxas anuais medidas por nivelamento geométrico de precisão.
2. **Euillades et al. (2020):**
   * *"Subsidence in Maceio, Brazil, characterized by DInSAR and inverse modeling"*, ISPRS Annals.
   * Fornece série temporal independente processada com Sentinel-1 entre 2014 e 2019.
3. **Hartwig et al. (2023):**
   * *"The significance of geological structures on the subsidence phenomenon at the Maceió salt dissolution field"*, Acta Geotechnica / INPE.
4. **Estação Geodésica Contínua RBMC ALMA (IBGE):**
   * Dados RINEX diários da estação GNSS fixa em Maceió para verificação de estabilidade geodésica do referencial regional.

---

## 7. Indicadores de Qualidade e Critérios de Aceite Científico

O processamento piloto será considerado **aprovado** se atender a todos os seguintes critérios:

```
+---------------------------------------------------------------------------------------------------------------+
|                                    CRITÉRIOS DE ACEITE CIENTÍFICO DO PILOTO MACEIÓ                            |
+----+------------------------------------+-----------------------+---------------------------------------------+
| ID | Indicador Científico               | Limiar de Aceite      | Método de Verificação                       |
+----+------------------------------------+-----------------------+---------------------------------------------+
| Q1 | Coerência Interferométrica Média   | gamma_medio >= 0.40   | Histograma de coerência nos alvos urbanos   |
|    | na mancha urbana                   |                       | dos bairros Pinheiro e Mutange              |
+----+------------------------------------+-----------------------+---------------------------------------------+
| Q2 | Correlação Espacial da Mancha      | r_Pearson >= 0.80     | Coeficiente de correlação espacial entre o  |
|    | com o polígono do SGB/CPRM         |                       | raster Radar++ e a mancha oficial CPRM      |
+----+------------------------------------+-----------------------+---------------------------------------------+
| Q3 | Divergência da Taxa Máxima de      | Erro relativo <= 15%  | Comparação do pico de velocidade (cm/ano)   |
|    | Subsidência contra o Benchmark SGB |                       | no Mutange contra as medições de campo      |
+----+------------------------------------+-----------------------+---------------------------------------------+
| Q4 | Erro Residual de Fechamento de     | RMS Residual <= 1.5   | Matriz de loop closure de fase no MintPy    |
|    | Fase (Phase Closure Residual)      | radianos              | eliminando pares com saltos de unwrapping   |
+----+------------------------------------+-----------------------+---------------------------------------------+
```

---

## 8. Possíveis Vieses, Ruídos e Limitações Documentadas

A equipe deve estar ciente dos seguintes desafios geofísicos em Maceió:
1. **Ruído Atmosférico Úmido Costeiro:** A proximidade imediata com o Oceano Atlântico e a Lagoa Mundaú provoca forte gradiente de vapor d'água na troposfera, gerando atrasos de fase que podem simular falsas deformações. A aplicação da correção ERA5 no MintPy é obrigatória.
2. **Perda de Coerência na Margem Lagunar:** Zonas de manguezal e vegetação aquática na orla do Mutange sofrem decorrelação quase total na Banda C. O processamento deve focar em estruturas edificadas (*Persistent Scatterers* urbanos).
3. **Altas Taxas de Deformação:** No epicentro do afundamento, taxas superiores a 30 cm/ano podem causar saltos de fase (*phase unwrapping errors*) de $2\pi$, exigindo o uso de pares com linhas de base temporais muito curtas (6 ou 12 dias).

---

## 9. Estimativa Quantitativa de Recursos e Custos (Simulação)

* **Número de Aquisições Sentinel-1 (2018-2021):** $\sim 80$ datas no Track 142.
* **Número de Pares Interferométricos (Small Baseline Stack):** $\sim 70$ a 90 pares unwrapped.
* **Consumo de Créditos NASA/ASF HyP3:**
  * Modo Burst InSAR consome $\sim 1$ a 2 créditos por par na resolução padrão.
  * *Consumo Total Estimado:* **70 a 180 créditos** (representa menos de **2,5% da cota mensal gratuita de 8.000 créditos** do HyP3 Basic).
* **Armazenamento em Disco Local:**
  * $\sim 200\text{ MB}$ por par COG (unwrapped phase, coherence, DEM corrigido).
  * *Volume Total de Disco Necessário:* **15 a 20 GB** de armazenamento local/S3.
* **Tempo de Processamento Estimado (Fase Futura):**
  * Submissão assíncrona ao HyP3: 2 a 4 horas (executado nos servidores da NASA/AWS).
  * Inversão SBAS no MintPy local (workstation 16 GB RAM): 1 a 2 horas.

---

*Protocolo científico elaborado e catalogado em `docs/discovery/`. Não foram executadas requisições ou processamentos computacionais.*
