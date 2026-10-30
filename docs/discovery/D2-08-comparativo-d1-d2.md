# Radar++ Discovery — D2-08: Matriz Estratégica Comparativa: Subsidência Urbana (D1) vs. Infraestrutura Linear (D2)
## Avaliação Qualitativa Fundamentada e Deliberação sobre a Ordem de Prioridades do Produto

**Documento:** `D2-08-comparativo-d1-d2.md`  
**Ciclo:** Investigação D2 — Infraestrutura Linear (Rodovias e Ferrovias)  
**Data:** 24 de Outubro de 2026  
**Status:** Oficial — Entregue para Governança  
**Responsável:** Equipe Multidisciplinar de Inteligência Estratégica, Geotecnia e Regulação  
**Repositório Oficial:** `api-radar-deformacao` (caminho: `docs/discovery/D2-08-comparativo-d1-d2.md`)  
**Diretriz de Governança:** Este documento apresenta uma análise comparativa e recomendações fundamentadas. **A priorização oficial de requisitos do Radar++ não deve ser alterada automaticamente sem deliberação do responsável pelo produto.**  

---

## 1. Matriz Comparativa Estruturada (12 Critérios)

```
+----------------------------------------------------------------------------------------------------------------------------------------------------+
|                                    MATRIZ COMPARATIVA ESTRATÉGICA: SUBSIDÊNCIA URBANA (D1) vs. INFRAESTRUTURA LINEAR (D2)                          |
+----+-----------------------+-----------------------------------------------+-----------------------------------------------+-----------------------+
| Nº | Critério de Avaliação | Subsidência Urbana (D1)                       | Infraestrutura Linear (D2)                    | Vantagem Estratégica  |
+----+-----------------------+-----------------------------------------------+-----------------------------------------------+-----------------------+
| 01 | Clareza do Problema   | **Altíssima**: colapso de bairros, fissuras em| **Alta**: rastejo de taludes, quedas de bar-  | Empate técnico        |
|    | e Apelo Público       | residências e evacuação humana evidente.      | reira e interrupção de vias de transporte.    | (D1 tem maior apelo)  |
+----+-----------------------+-----------------------------------------------+-----------------------------------------------+-----------------------+
| 02 | Disponibilidade de    | **Excelente**: cidades contam com prédios e   | **Média a Boa**: pista e trilhos são alvos    | Subsidência Urbana    |
|    | Alvos Reflexivos (PS) | concreto que atuam como espalhadores puros.   | bons, mas encostas florestadas perdem coerênc.| (D1)                  |
+----+-----------------------+-----------------------------------------------+-----------------------------------------------+-----------------------+
| 03 | Viabilidade Cientí-   | **Muito Alta**: bacias de subsidência amplas  | **Média**: taludes estreitos (<20 m) geram    | Subsidência Urbana    |
|    | fica com Sentinel-1   | facilitam unwrapping e reduzem pixel misto.   | pixels mistos e sombras geométricas de serra. | (D1)                  |
+----+-----------------------+-----------------------------------------------+-----------------------------------------------+-----------------------+
| 04 | Acesso a Compradores  | **Difícil no B2G Direto**: prefeituras não têm | **Hipótese de Acesso Corporativo Mais Direto**| Hipótese a Validar    |
|    | e Disposição a Pagar   | orçamento livre de SaaS nem equipe técnica.    | (B2B): concessionárias têm OPEX de manutenção, | (Necessita entrevistas|
|    |                        |                                                | mas disposição a pagar por radar é a validar.  | qualitativas)         |
+----+-----------------------+-----------------------------------------------+-----------------------------------------------+-----------------------+
| 05 | Intensidade da        | **Média**: concorrentes focam em megaobras,   | **Alta**: presença massiva de drones (LiDAR/  | Subsidência Urbana    |
|    | Concorrência Direta   | deixando cidades médias desatendidas.         | RGB) e consultorias com instrumentação in-situ| (D1)                  |
+----+-----------------------+-----------------------------------------------+-----------------------------------------------+-----------------------+
| 06 | Diferenciação Real    | **Alta**: reconstrução do histórico prévio    | **Média**: complementa drones na escala macro,| Subsidência Urbana    |
|    | da Solução            | (desde 2014) que prefeituras não possuem.     | mas não substitui ortofotos centimétricas.    | (D1)                  |
+----+-----------------------+-----------------------------------------------+-----------------------------------------------+-----------------------+
| 07 | Ciclo de Vendas e     | **Burocrático (B2G)**: Lei 14.133/2021,       | **Hipótese de Ciclo B2B Privado**: processos  | Hipótese a Validar    |
|    | Complexidade de Compra| licitações formais, pregões e riscos legais.  | corporativos privados, sujeitos a homologação.| (Sem garantia prévia) |
+----+-----------------------+-----------------------------------------------+-----------------------------------------------+-----------------------+
| 08 | Custo de Processamen- | **Baixo**: AOIs compactas (20 a 50 km²) con-  | **Médio a Alto**: corredores de 500 a 1.000 km| Subsidência Urbana    |
|    | to e Armazenamento    | somem poucos bursts e poucos COGs.            | exigem múltiplos bursts e mosaicos pesados.   | (D1)                  |
+----+-----------------------+-----------------------------------------------+-----------------------------------------------+-----------------------+
| 09 | Complexidade de       | **Menor**: seleção visual por polígono retan- | **Maior**: exige geração de buffers lineares, | Subsidência Urbana    |
|    | Produto / Interface   | gular e mapas 2D contínuos em Leaflet.        | segmentação por km e projeções em estaqueamento| (D1)                  |
+----+-----------------------+-----------------------------------------------+-----------------------------------------------+-----------------------+
| 10 | Dependência de        | **Média**: produto pode entregar velocidade   | **Muito Alta**: concessionárias exigem engenhe| Subsidência Urbana    |
|    | Consultoria Humana    | de deformação analítica para laudos periciais.| iro geotécnico assinando laudo com ART oficial| (D1)                  |
+----+-----------------------+-----------------------------------------------+-----------------------------------------------+-----------------------+
| 11 | Recorrência Potencial | **Moderada**: bacias de subsidência mudam     | **Hipótese de Recorrência Periódica**: apoio  | Hipótese a Validar    |
|    | de Faturamento        | lentamente ao longo de anos (risco de churn). | ao SGTEC, sujeita à validação de interesse.   |                       |
+----+-----------------------+-----------------------------------------------+-----------------------------------------------+-----------------------+
| 12 | Validação com         | **Excepcional**: Maceió/AL e Recife possuem   | **Boa, mas Fragmentada**: dados de encostas   | Subsidência Urbana    |
|    | Casos Reais Públicos  | dados e laudos oficiais exaustivos no SGB.    | rodoviárias são de custódia privada das conc. | (D1)                  |
+----+-----------------------+-----------------------------------------------+-----------------------------------------------+-----------------------+
```

---

## 2. Análise Estratégica: Onde Cada Mercado se Destaca

### O Ponto Forte da Subsidência Urbana (D1): Viabilidade Técnica e Científica Superior
* **Por que é a escolha ideal para o piloto:** Na subsidência urbana, as áreas são bacias contínuas e largas (centenas de metros ou quilômetros de diâmetro), minimizando o problema do pixel misto. O ambiente urbano fornece milhares de telhados, lajes e postes que atuam como espalhadores persistentes naturais (*Persistent Scatterers*), garantindo alta coerência interferométrica ($\gamma > 0{,}60$).
* **O Ponto de Atenção:** O comprador público municipal (Prefeituras) não possui cultura de contratação tecnológica direta nem recursos livres para software isolado.

### O Potencial da Infraestrutura Linear (D2): Hipótese Comercial a Validar
* **A Hipótese Comercial:** Concessionárias rodoviárias e ferroviárias (Motiva, Arteris, Rumo, MRS) operam como empresas privadas com orçamentos de manutenção viária (OPEX). Supõe-se que o custo de paralisação de tráfego e exigências do SGTEC possam justificar a contratação de serviços de monitoramento orbital. **Contudo, a disposição a pagar efetiva, os ciclos de homologação de fornecedores e a aceitação do radar frente aos drones permanecem estritamente como hipóteses comerciais a validar em campo.**
* **O Desafio Técnico:** A detecção em encostas tropicais cobertas por Mata Atlântica sofre severa perda de sinal na Banda C do Sentinel-1, e a geometria estreita das rodovias exige algoritmos refinados para evitar contaminação por pixels mistos. Não há garantia técnica de detecção antecipada para todas as tipologias de deslizamento.

---

## 3. Recomendação Fundamentada sobre a Ordem de Prioridades

A ordem estratégica oficial atual do Radar++ é:
1. **Prioridade 1:** Subsidência e deformações urbanas.
2. **Prioridade 2:** Infraestrutura linear (rodovias e ferrovias).
3. **Prioridade 3:** Pesquisa científica e consultorias geoespaciais.

### Parecer da Equipe Multidisciplinar de Inteligência:
> **Recomenda-se MANTER a ordem de prioridades oficial sem alterações, preservando a distinção clara entre o piloto científico e o mercado comercial potencial:**
>
> 1. **Prioridade 1 — Subsidência Urbana como Piloto Científico (MVP):** Manter o foco da engenharia e validação metodológica em **Maceió/AL**. Trata-se de um **piloto científico de validação retrospectiva**, onde o acervo público e auditado do SGB/CPRM e do IBGE permite calibrar e comprovar a acurácia do pipeline HyP3 + MintPy a baixo custo e com alta coerência geodésica.
> 2. **Prioridade 2 — Infraestrutura Linear como Possível Mercado Comercial (Pós-MVP):** Tratar a aplicação em rodovias e ferrovias como uma **hipótese de mercado comercial a ser validada**. Os resultados consolidados do piloto científico de Maceió servirão como evidência técnica prévia para conduzir entrevistas de descoberta com concessionárias e consultorias, testando a disposição real a pagar sem promessas prematuras de superioridade comercial.
