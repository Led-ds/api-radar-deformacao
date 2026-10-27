# Radar++ Discovery — D1-03: Matriz Competitiva Detalhada e Benchmarking de Alternativas
## Comparação Exaustiva de Fornecedores, Ferramentas Científicas e Soluções Comerciais

**Documento:** `D1-03-matriz-competitiva.md`  
**Ciclo:** Investigação D1 — Mercado e Viabilidade  
**Data:** 24 de Outubro de 2026  
**Status:** Oficial — Entregue para Governança  
**Responsável:** Equipe de Descoberta de Produto, Inteligência de Mercado e Sensoriamento Remoto  
**Repositório Oficial:** `api-radar-deformacao` (caminho: `docs/discovery/D1-03-matriz-competitiva.md`)  

---

## 1. Visão Geral do Cenário Competitivo

O mercado de detecção e monitoramento de deformações terrestres no Brasil é estruturado em três camadas distintas:
1. **Multinacionais de Alto Tíquete e Serviços Integrados:** Empresas globais com filial no Brasil focadas em grandes corporações de mineração e infraestrutura pesada (CLS Brasil / TRE Altamira, Fugro Brasil, Sixense, Telespazio).
2. **Consultorias Regionais de Geotecnologia:** Empresas nacionais de serviços que processam dados de radar ou aerofotogrametria sob demanda (Globalgeo, INSAR Corp, Imagem Geosistemas).
3. **Iniciativas Públicas e Ferramentas Científicas Abertas:** Serviços governamentais abertos (Copernicus EGMS, LiCSAR, ASF HyP3, MintPy, SNAP) que oferecem dados ou motores de processamento sem interface comercial voltada a negócios brasileiros.

---

## 2. Matriz Competitiva Comparativa Exaustiva

```
+-----------------------------------------------------------------------------------------------------------------------------------------------------+
|                                            MATRIZ COMPARATIVA DE COMPETIDORES, ALTERNATIVAS E O RADAR++                                             |
+-------------------+--------------------+--------------------+--------------------+--------------------+--------------------+--------------------+
| Critério          | CLS Brasil /       | Fugro Brasil       | SkyGeo             | Sixense Brasil     | Copernicus EGMS /  | Radar++            |
|                   | TRE Altamira       |                    | (Global)           | (Atlas InSAR)      | LiCSAR             | (Posicionamento)   |
+-------------------+--------------------+--------------------+--------------------+--------------------+--------------------+--------------------+
| 1. Funcionalidades| Deformações em 3D, | InSAR integrado a  | Séries temporais   | InSAR integrado a  | Mapas de velocidade| Definição de AOI,  |
|                   | séries temporais,  | sondagens, geofísi-| automatizadas,     | sensores de campo, | média anual (LOS e | catálogo Sentinel, |
|                   | alertas de talude  | ca e instrumentação| alertas de risco   | túneis e ferrovias | decomposição vert.)| DInSAR/SBAS web    |
+-------------------+--------------------+--------------------+--------------------+--------------------+--------------------+--------------------+
| 2. Público-Alvo   | Vale, Petrobras,   | Grandes operadoras | Mineradoras multi- | Metrôs, ferrovias, | Governos europeus, | Concessionárias    |
|                   | megamineradoras e  | de infraestrutura, | nacionais e opera- | grandes consórcios | cientistas, órgãos | PMEs, consultorias |
|                   | barragens de rejeit| portos e energia   | doras ferroviárias | de construção civil| de proteção civil  | geotécnicas e P&D  |
+-------------------+--------------------+--------------------+--------------------+--------------------+--------------------+--------------------+
| 3. Cobertura      | Global; presença   | Global; escritórios| Global; cobertura  | Global; escritório | EGMS: Apenas Europa| Brasil e América do|
| Territorial       | forte no RJ/Brasil | no RJ, Macaé e MG  | remota no Brasil   | em São Paulo       | LiCSAR: global des-| Sul (foco inicial  |
|                   |                    |                    |                    |                    | contínuo em cratons| em território BR)  |
+-------------------+--------------------+--------------------+--------------------+--------------------+--------------------+--------------------+
| 4. Processamento  | Proprietário       | Proprietário       | Proprietário       | Proprietário       | Algoritmos abertos | Aberto baseado em  |
| Científico        | (SqueeSAR / PSI)   | multissensor com   | (PSI / SBAS) na    | integrado à plat.  | calibrados com rede| NASA/ASF HyP3 e    |
|                   | de altíssima precis| calibração de solo | nuvem corporativa  | Geoscope em solo   | GNSS europeia      | MintPy (SBAS Python|
+-------------------+--------------------+--------------------+--------------------+--------------------+--------------------+--------------------+
| 5. Apresentação   | Portal web robusto | Relatórios técnicos| Dashboard web      | Painel de monito-  | Visualizador web   | Web moderna fluida |
| de Resultados     | corporativo        | densos em PDF e    | moderno interativo | ramento estrutural | aberto comunitário | (React / Leaflet / |
|                   | (TREmaps®)         | shapefiles         | e gráficos de curva| em tempo real      | em WebGL           | WebGL), gráficos   |
+-------------------+--------------------+--------------------+--------------------+--------------------+--------------------+--------------------+
| 6. Histórico e    | Arquivos desde     | Back-analysis      | Arquivos históricos| Histórico associado| Histórico desde    | Rastreabilidade to-|
| Rastreabilidade   | ERS-1 (1992) até   | sob demanda de     | desde o lançamento | a obras específicas| 2015 via acervo    | tal de cenas, órbi-|
|                   | satélites atuais   | projetos           | dos satélites      | de engenharia      | Sentinel-1         | tas e coerência    |
+-------------------+--------------------+--------------------+--------------------+--------------------+--------------------+--------------------+
| 7. Integrações    | Exportação SIG     | Integração com     | API REST para ERP  | Integração direta  | Download de dados  | Exportação SIG     |
|                   | avançada para      | sistemas de ativi- | e sistemas de ges- | com sensores de    | abertos e pacotes  | (SHP, GeoPackage,  |
|                   | ArcGIS / QGIS      | dades industriais  | tão de risco minero| convergência solo  | de dados vetoriais | COG) e APIs abertas|
+-------------------+--------------------+--------------------+--------------------+--------------------+--------------------+--------------------+
| 8. Modelo         | Contratos anuais   | Contratos de enge- | Assinatura anual   | Contratos de moni- | Gratuito e aberto  | SaaS B2B flexível  |
| Comercial         | corporativos de    | nharia consultiva  | corporativa na     | toramento global   | (Financiamento pú- | por área/km e por  |
|                   | grande porte       | com ART inclusa    | nuvem              | de empreendimentos | blico da UE/Copern.)| job de análise     |
+-------------------+--------------------+--------------------+--------------------+--------------------+--------------------+--------------------+
| 9. Preços         | Sob consulta;      | Sob consulta;      | Sob consulta;      | Sob consulta;      | Gratuito           | Faixa pretendida:  |
| Verificáveis      | Tíquetes anuais de | Pacotes integrados | Faturas estimadas  | Atrelado ao escopo | (Custo zero de     | planos de R$ 1,5k  |
|                   | R$ 300k a >R$ 1M   | de centenas de milh| em US$ 30k a >100k | da obra de infra   | licença de uso)    | a R$ 15k / mês     |
+-------------------+--------------------+--------------------+--------------------+--------------------+--------------------+--------------------+
| 10. Limitações    | Inacessível para   | Não vende software | Interface em inglês| Fortemente preso à | EGMS não atende o  | Depende de cotas   |
| Documentadas      | PMEs e municípios; | isolado; burocráti-| pouca aderência aos| instalação física  | Brasil; LiCSAR des-| HyP3 ou workers    |
|                   | foco exclusivo Tier| co e complexo      | padrões regulat. BR| de instrumentação  | contínuo em cidades| próprios dedicados |
+-------------------+--------------------+--------------------+--------------------+--------------------+--------------------+--------------------+
```

---

## 3. Avaliação de Competidores Nacionais e Alternativas Locais

Além dos grandes players multinacionais, investigou-se a atuação de prestadores de serviços brasileiros:

### 3.1. Globalgeo Geotecnologias (Belo Horizonte/MG)
* **Atuação Efetiva:** Empresa consolidada no ecossistema de mineração e meio ambiente de Minas Gerais. Presta serviços especializados de processamento SAR orbital, fotogrametria por drones e geoprocessamento.
* **Modelo Comercial:** Tradicional prestação de serviços de engenharia por projeto (elaboração de relatórios, shapefiles e mapas temáticos).
* **Vulnerabilidade Frente ao Radar++:** Não comercializa produto de software na nuvem com autosserviço para o cliente explorar e gerar análises sob demanda.

### 3.2. Telespazio Brasil (Rio de Janeiro/RJ)
* **Atuação Efetiva:** Braço do grupo Leonardo/Thales. Atua como revendedora oficial e fornecedora de soluções baseadas na constelação italiana COSMO-SkyMed (satélites de radar em Banda X de alta resolução submétrica).
* **Modelo Comercial:** Venda direta de cenas de radar comerciais e contratação de projetos customizados para defesa, segurança e grandes empreendimentos.
* **Vulnerabilidade Frente ao Radar++:** O custo de aquisição das imagens COSMO-SkyMed é cobrado em euros/dólares, inviabilizando projetos contínuos de baixo custo. O Radar++ foca em dados abertos gratuitos do Copernicus Sentinel-1.

---

## 4. Onde Reside a Real Diferenciação do Radar++

Para que o Radar++ tenha sucesso e não seja esmagado pelos competidores estabelecidos, sua diferenciação não pode ser baseada em "fazer o mesmo processamento dos gigantes por metade do preço". A diferenciação deve ser fundamentada em:

```
+---------------------------------------------------------------------------------------------------------------+
|                                     QUADRO DE DIFERENCIAÇÃO REAL DO RADAR++                                   |
+-------------------------------------+---------------------------------+---------------------------------------+
| Atributo de Valor                   | Soluções Existentes no Mercado  | Proposta de Valor do Radar++          |
+-------------------------------------+---------------------------------+---------------------------------------+
| Barreira de Entrada Financeira      | Proibitiva (Contratos >R$ 300k) | Acessível (Planos de R$ 1,5k a R$ 15k)|
+-------------------------------------+---------------------------------+---------------------------------------+
| Modelo de Adoção                    | Contratos corporativos lentos   | Onboarding web ágil em minutos        |
+-------------------------------------+---------------------------------+---------------------------------------+
| Localização Regulatória e Geodésica | Padrões internacionais em inglês| 100% nativo em SIRGAS 2000, ANTT e SGB|
+-------------------------------------+---------------------------------+---------------------------------------+
| Autonomia do Usuário                | Dependência de consultor sênior | Autosserviço visual para geotécnicos  |
+-------------------------------------+---------------------------------+---------------------------------------+
| Arquitetura Tecnológica             | Plataformas legadas e pesadas   | Web moderna (React, Leaflet, COGs, S3)|
+-------------------------------------+---------------------------------+---------------------------------------+
```

### O que o Radar++ NÃO DEVE tentar competir:
1. **Não competir com a Banda X de alta resolução:** Cenas comerciais da TerraSAR-X ou COSMO-SkyMed atingem resolução submétrica e identificam deformações em parafusos e vigas individuais; o Sentinel-1 (Banda C) foca em deformações regionais e maciços de encostas/subsidências.
2. **Não tentar substituir a ART de campo:** O Radar++ é ferramenta de inteligência analítica, não emissor de responsabilidade civil de engenharia.
