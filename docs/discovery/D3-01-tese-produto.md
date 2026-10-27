# Radar++ Discovery — D3-01: Tese de Produto e Posicionamento Estratégico
## Definição Fundamental da Proposta de Valor, Contexto Operacional e Diferenciação

**Documento:** `D3-01-tese-produto.md`  
**Ciclo:** Investigação D3 — Consolidação Estratégica e Engenharia de Produto  
**Data:** 24 de Outubro de 2026  
**Status:** Oficial — Entregue para Governança  
**Responsável:** Equipe Multidisciplinar (Product Manager B2B/B2G, Arquiteto de Software, Especialistas SAR e Negócios)  
**Repositório Oficial:** `api-radar-deformacao` (caminho: `docs/discovery/D3-01-tese-produto.md`)  

---

## 1. Problema Central e Contexto Operacional

### 1.1. A Dor Estrutural do Monitoramento Geotécnico e de Deformações
Engenheiros geotécnicos, gestores de infraestrutura de transporte e órgãos de defesa civil enfrentam uma assimetria fundamental: **as deformações do solo ocorrem em escala regional e contínua, mas os métodos tradicionais de inspeção são pontuais, reativos e visualmente limitados**.

```mermaid
graph TD
    subgraph "Limitação Atual dos Métodos de Campo"
        A["Inspeção Visual e Drones"] -->|Enxerga apenas superfície| B["Trinca ou Degrau já Manifestado"]
        C["Instrumentação In-Situ (Inclinômetros)"] -->|Extremamente pontual| D["Alto Custo por Ponto (R$ 30k-80k)"]
    end
    subgraph "Gargalo Operacional"
        B --> E["Intervenção Emergencial Tardia / Queda de Barreira / Colapso"]
        D --> E
    end
    subgraph "Oportunidade Radar++"
        F["Triagem Macroescala via Satélite (InSAR)"] -->|Mapeia deformação lenta contínua (creep)| G["Indicação Prévia de Áreas Anômalas"]
        G -->|Direciona vistorias e instrumentos| H["Intervenção Preventiva Planejada"]
    end
```

### 1.2. O Contexto Operacional no Brasil
1. **Subsidência Urbana e Bacias de Afundamento:** Em áreas de colapso geológico ou subsidência associada a mineração e superexplotação de aquíferos (ex.: Maceió/AL, bacias sedimentares de Recife/PE e capitais costeiras), o dano afeta milhares de edificações e quilômetros de rede de saneamento. A prefeitura e os órgãos locais não possuem histórico sistematizado nem ferramentas acessíveis para auditar a taxa milimétrica de afundamento.
2. **Infraestrutura Linear (Rodovias e Ferrovias):** Operadores privados (concessionárias) administram entre 3.000 e 14.000 km de malha cortando serras tropicais (Serra do Mar, Serra das Araras, Serra da Mantiqueira). É fisicamente inviável cobrir toda a malha com vistorias a pé ou voos de drone a cada 15 dias. O início do rastejo (*creep*) no topo dos taludes costuma ser invisível da pista até que o colapso repentino interdite o tráfego.

---

## 2. Público-Alvo, Usuários e Compradores

Para evitar o erro de desenhar funcionalidades para um "comprador abstrato", o Radar++ distingue rigorosamente os papéis:

```
+----------------------------------------------------------------------------------------------------------------------------------------------------+
|                                      MATRIZ DE ATORES: PAPÉIS, OBJETIVOS E RELAÇÃO COM O PRODUTO                                                   |
+--------------------------+--------------------------------+-------------------------------------+--------------------------------------------------+
| Categoria de Ator        | Perfil Típico                  | Papel na Jornada                    | Necessidade Central                              |
+--------------------------+--------------------------------+-------------------------------------+--------------------------------------------------+
| Usuário Operacional      | Engenheiro Geotécnico, Geólogo,| Utiliza a plataforma para cadastrar | Obter mapas de velocidade LOS, gráficos de série |
|                          | Analista de Geoprocessamento / | áreas, inspecionar deformações e    | temporal acumulada e exportar camadas SIG/GeoTIFF|
|                          | SIG de consultoria ou operadora| gerar laudos preliminares de risco. | sem lidar com terminais Linux ou scripts MintPy. |
+--------------------------+--------------------------------+-------------------------------------+--------------------------------------------------+
| Comprador Econômico      | Diretor de Engenharia/Operações| Aprova o orçamento e a assinatura   | Reduzir custos com vistorias desnecessárias,     |
| (B2B Privado)            | de Concessionária Viária ou    | corporativa ou contratação por cam- | evitar multas regulatórias (ANTT/ARTESP) e mini- |
|                          | Sócio de Consultoria de Projetos| panha da plataforma.                | mizar risco de interrupção catastrófica de vias. |
+--------------------------+--------------------------------+-------------------------------------+--------------------------------------------------+
| Comprador Econômico      | Secretário de Obras/Urbanismo, | Contrata serviços de engenharia via | Comprovar causalidade em áreas de subsidência,   |
| (B2G Público)            | Diretor de Defesa Civil        | licitação pública (Lei 14.133/2021) | embasar planos de contingência e orientar ações  |
|                          | Municipal / Estadual           | ou convênios de emergência.         | de evacuação com laudos periciais auditáveis.    |
+--------------------------+--------------------------------+-------------------------------------+--------------------------------------------------+
| Parceiro de Validação    | Pesquisador Universitário      | Executa testes metodológicos e      | Acesso a dados interferométricos pré-processados |
| Científica               | (UFRJ, UFPE, USP) ou Pesquisa- | compara séries temporais contra     | para publicação de artigos e validação de algorit|
|                          | dor do SGB/CPRM e IBGE         | marcos geodésicos in-situ.          | mos de desdobramento de fase (*phase unwrapping*)|
+--------------------------+--------------------------------+-------------------------------------+--------------------------------------------------+
```

---

## 3. Alternativas Existentes e Matriz de Posicionamento

```
+----------------------------------------------------------------------------------------------------------------------------------------------------+
|                                    COMPARATIVO DE ALTERNATIVAS DISPONÍVEIS NO MERCADO BRASILEIRO                                                   |
+--------------------------+--------------------------+--------------------------+--------------------------+----------------------------------------+
| Critério                 | Plataformas Globais InSAR| Drones com LiDAR / RGB   | Instrumentação In-Situ   | Radar++ (Proposta de Valor)             |
|                          | (TRE Altamira, SkyGeo)   | (Empresas de Aerolevant.)| (Inclinômetros, Marcos)  |                                        |
+--------------------------+--------------------------+--------------------------+--------------------------+----------------------------------------+
| Tíquete Médio            | Altíssimo (> R$ 300k/ano)| Médio (R$ 5k - 25k / voo)| Alto (R$ 30k - 80k/ponto)| Acessível (SaaS PME / sob demanda)     |
+--------------------------+--------------------------+--------------------------+--------------------------+----------------------------------------+
| Foco de Mercado          | Megamineração Tier 1 e   | Faixas estreitas locais  | Pontos críticos de altís-| Triagem macro de corredores e bacias   |
|                          | grandes petroleiras      | (10 a 50 km por campanha)| simo risco (R4)          | urbanas (dezenas a centenas de km²)    |
+--------------------------+--------------------------+--------------------------+--------------------------+----------------------------------------+
| Análise Histórica        | Sim (sob encomenda com   | Não (apenas a partir da  | Não (apenas a partir da  | **Nativa e Imediata** (catálogo aberto |
| Prévia (Back-Analysis)   | alto custo de ativação)  | data do primeiro voo)    | instalação dos sensores) | Sentinel-1 disponível desde 2014)      |
+--------------------------+--------------------------+--------------------------+--------------------------+----------------------------------------+
| Autonomia do Usuário     | Nula (entrega fechada em | Baixa (depende de equipe | Baixa (depende de leituras| **Totalmente Web Autosserviço**        |
|                          | formato consultoria)     | e piloto em campo)       | e calibração periódica)  | (usuário seleciona AOI e gera análise) |
+--------------------------+--------------------------+--------------------------+--------------------------+----------------------------------------+
| Complexidade Técnica     | Terceirizada             | Operação física de campo | Engenharia civil pesada  | Complexidade científica encapsulada    |
+--------------------------+--------------------------+--------------------------+--------------------------+----------------------------------------+
```

---

## 4. Proposta de Valor e Pilares de Diferenciação

### A Proposta Central de Valor:
> **Democratizar a inteligência interferométrica por radar para engenheiros geotécnicos e gestores de infraestrutura, transformando dados brutos de satélite em diagnósticos históricos e triagens macroespaciais de deformação milimétrica, sem complexidade de infraestrutura Linux e com custo acessível.**

### Pilares de Diferenciação Estratégica:
1. **Histórico Instantâneo (Viagem no Tempo Geotécnica):** Enquanto drones e sensores só geram dados após contratados e instalados, o Radar++ processa retrospectivamente dados do Sentinel-1 desde outubro de 2014, permitindo entender o comportamento do terreno nos últimos 10 anos em minutos.
2. **Triagem em Macroescala com Custo Marginal Baixo:** Permite varrer centenas de quilômetros de ferrovias, rodovias ou bacias urbanas para apontar exatamente onde a deformação está acelerando, direcionando os drones e ensaios caros para os locais certos.
3. **Encapsulamento Científico em Nuvem:** Elimina a necessidade de as consultorias contratarem especialistas em geodésia ou montarem clusters locais com ISCE2, MintPy ou SNAP, fornecendo resultados auditáveis via navegador web.

---

## 5. Limitações Físicas e Fronteiras do Produto

O Radar++ mantém sua integridade ética e comercial estabelecendo com clareza o que **NÃO faz**:

1. **NÃO prevê desastres instantâneos nem quedas de barreira torrenciais:** O Sentinel-1 opera em ciclos de revisita orbital de 6 a 12 dias. O produto detecta rastejo acumulado lento (*creep*), nunca deslizamentos repentinos ocorridos em minutos durante temporais.
2. **NÃO penetra floresta densa contínua na Banda C:** Em encostas cobertas por dossel espesso de Mata Atlântica sem rocha exposta ou intervenção civil, a Banda C sofre descorrelação temporal. O produto alerta essa limitação ao usuário.
3. **NÃO confunde velocidade na linha de visada (LOS) com afundamento vertical puro:** O satélite mede a projeção escalar 1D no vetor radar. A conversão para deslocamento vertical exige pares bi-orbitais (Ascendente + Descendente) ou hipótese cinemática fundamentada.
4. **NÃO emite Anotação de Responsabilidade Técnica (ART):** O software fornece subsídios e evidências analíticas; a responsabilidade técnica e o projeto executivo de contenção cabem exclusivamente ao engenheiro legalmente habilitado.

---

## 6. Por que Escolher o Radar++? (Razões de Uso)

```
+----------------------------------------------------------------------------------------------------------------------------------------------------+
|                                              POR QUE UTILIZAR O RADAR++ EM VEZ DE OUTRAS ABORDAGENS?                                              |
+---------------------------------------+------------------------------------------------------------------------------------------------------------+
| Cenário Alternativo                   | Por que o Radar++ é Mais Vantajoso?                                                                        |
+---------------------------------------+------------------------------------------------------------------------------------------------------------+
| Scripts Acadêmicos Manuais            | O usuário não gasta semanas configurando dependências Linux, GDAL, HDF5 e ISCE2/MintPy. O Radar++ orquestra|
| (MintPy, StaMPS, SNAP em terminal)    | a busca de bursts, processamento e geração de COGs de forma automatizada e reprodutível.                   |
+---------------------------------------+------------------------------------------------------------------------------------------------------------+
| Grandes Fornecedores Globais          | Não exige compromissos contratuais de R$ 300.000/ano nem negociações de meses para uma análise exploratória|
| (TRE Altamira, Sixense, Fugro)        | de um trecho de serra ou bairro urbano; viabiliza diagnósticos pontuais e contratos flexíveis.             |
+---------------------------------------+------------------------------------------------------------------------------------------------------------+
| Levantamentos Topográficos e Drones   | O drone precisa se deslocar a campo e só vê o estado presente. O Radar++ fornece a linha de base histórica |
| Convencionais de Pista                | de 2014 a 2026 antes mesmo de a equipe pisar no terreno, a uma fração ínfima do custo por quilômetro.    |
+---------------------------------------+------------------------------------------------------------------------------------------------------------+
```

---

## 7. Matriz Epistemológica: Hipóteses vs. Benefícios Comprovados

```
+----------------------------------------------------------------------------------------------------------------------------------------------------+
|                                    CLASSIFICAÇÃO DE MATURIDADE DAS PROPOSIÇÕES DO PRODUTO                                                          |
+----+---------------------------------------------------------------+-----------------------+-------------------------------------------------------+
| ID | Proposição do Radar++                                         | Classificação         | Evidência ou Validação Necessária                     |
+----+---------------------------------------------------------------+-----------------------+-------------------------------------------------------+
| P1 | InSAR na Banda C mede deformação milimétrica em áreas urbanas | **Fato Comprovado**   | Artigos científicos consolidados e relatórios oficiais|
|    | e alvos rochosos/antrópicos com alta coerência (gamma > 0.6). | (Científico)          | do SGB/CPRM em Maceió e Recife (2019-2024).           |
+----+---------------------------------------------------------------+-----------------------+-------------------------------------------------------+
| P2 | Pipeline aberto HyP3 + MintPy reconstrói séries temporais     | **Fato Comprovado**   | Manuais e validações empíricas do Alaska Satellite    |
|    | com acurácia compatível com laudos geotécnicos periciais.     | (Engenharia)          | Facility (ASF) e literatura InSAR de referência.      |
+----+---------------------------------------------------------------+-----------------------+-------------------------------------------------------+
| P3 | Consultorias geotécnicas têm interesse em contratar diagnós-  | **Hipótese Comercial**| A ser validada nas entrevistas de descoberta (D1-04)  |
|    | ticos históricos pontuais sob demanda para laudos periciais.  | (Alta Probabilidade)  | com sócios de PMEs de geotecnia viária e urbana.      |
+----+---------------------------------------------------------------+-----------------------+-------------------------------------------------------+
| P4 | Concessionárias de rodovias pagarão assinatura anual contínua | **Hipótese Comercial**| Depende de validação em campo de aprovação em comitês |
|    | por km de malha para apoiar o SGTEC da ANTT/ARTESP.           | (Risco Alto)          | de compras e superação do ciclo de vendas corporativo.|
+----+---------------------------------------------------------------+-----------------------+-------------------------------------------------------+
| P5 | É possível processar buffers lineares de 500m no Sentinel-1   | **Hipótese Técnica**  | Exige prova de conceito de recorte de bursts ao longo |
|    | com consumo aceitável de créditos e armazenamento em nuvem.   | (Arquitetura)         | de shapefiles sinuosos sem sobrecarga de COGs.        |
+----+---------------------------------------------------------------+-----------------------+-------------------------------------------------------+
```
