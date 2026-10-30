# Radar++ Discovery — D1-06: Quadro Consolidado de Hipóteses, Evidências e Níveis de Incerteza
## Matriz Epistemológica de Negócio, Tecnologia SAR e Operação

**Documento:** `D1-06-hipoteses-e-evidencias.md`  
**Ciclo:** Investigação D1 — Mercado e Viabilidade  
**Data:** 24 de Outubro de 2026  
**Status:** Oficial — Entregue para Governança  
**Responsável:** Equipe de Descoberta de Produto, Inteligência de Mercado e Sensoriamento Remoto  
**Repositório Oficial:** `api-radar-deformacao` (caminho: `docs/discovery/D1-06-hipoteses-e-evidencias.md`)  

---

## 1. Metodologia de Classificação Epistemológica

Para garantir que o Radar++ não construa sua estratégia de produto e arquitetura sobre premissas falsas, todas as afirmações e crenças do projeto são classificadas em quatro categorias rigorosas:

1. **Fato Confirmado:** Informação comprovada por documentação oficial primária, dados auditados de portais públicos, publicações científicas revisadas por pares ou normas vigentes, com fonte e data explícitas.
2. **Inferência:** Conclusão lógica derivada de fatos confirmados, mas cuja aplicabilidade ao modelo de negócios específico do Radar++ ainda precisa de observação de campo.
3. **Hipótese:** Suposição plausível adotada para guiar o desenho de produto, mas que carece de evidências quantitativas ou validação com clientes.
4. **Informação Não Verificada:** Alegação de mercado, rumor comercial ou estimativa informal sem respaldo documental ou comprovação empírica.

---

## 2. Quadro Integrado de Hipóteses de Negócio e Mercado

```
+----------------------------------------------------------------------------------------------------------------------------------------------------+
|                                                MATRIZ DE HIPÓTESES DE NEGÓCIO E MERCADO (B2B / B2G)                                                |
+---------+----------------------------------------------+---------------+----------------------------------------+----------------------------------+
| ID      | Enunciado da Hipótese                        | Status Atual  | Evidência Primária / Justificativa     | Próximo Passo de Validação       |
+---------+----------------------------------------------+---------------+----------------------------------------+----------------------------------+
| H-N01   | Prefeituras municipais médias contratam      | REFUTADA      | Mapeamento no PNCP revelou que compras | Abandonar modelo de venda direta |
|         | software SaaS de satélite de forma autônoma. | (Inviável)    | públicas exigem ART e laudos humanos.  | B2G self-service de software puro|
+---------+----------------------------------------------+---------------+----------------------------------------+----------------------------------+
| H-N02   | Consultorias de geotecnia e perícia civil    | SOB VALIDAÇÃO | Consultorias cobram por laudo emitido; | Conduzir entrevistas do Bloco 4  |
|         | pagarão por área processada (modelo B2B2G).  | (Favorável)   | têm interesse em reduzir tempo técnico.| do roteiro D1-04 com 5 empresas. |
+---------+----------------------------------------------+---------------+----------------------------------------+----------------------------------+
| H-N03   | Concessionárias de rodovias adotarão InSAR   | ABERTA COM    | Drones são a tecnologia padrão adotada | Entrevistar gerentes de conserva-|
|         | como complemento oficial a vistorias e drone.| ALTO RISCO    | hoje por concessionárias em taludes.   | ção de rodovias ANTT/ARTESP.     |
+---------+----------------------------------------------+---------------+----------------------------------------+----------------------------------+
| H-N04   | Concessionárias de saneamento e gás compram  | ABERTA        | Rompimento de adutoras e vazamento de  | Identificar responsáveis por per-|
|         | monitoramento de solo para mitigar sinistros.| (Exploratória)| gás por subsidência geram perdas reais.| das e integridade de dutos.      |
+---------+----------------------------------------------+---------------+----------------------------------------+----------------------------------+
| H-N05   | O mercado aceita precificação na faixa de    | HIPÓTESE NÃO  | Estimativa baseada no custo marginal de| Testar aceitação de preço em     |
|         | R$ 1.500 a R$ 5.000 / mês por AOI monitorada.| VALIDADA      | nuvem, sem validação com compradores.  | entrevistas de descoberta.       |
+---------+----------------------------------------------+---------------+----------------------------------------+----------------------------------+
```

---

## 3. Quadro Integrado de Hipóteses Tecnológicas e Científicas

```
+----------------------------------------------------------------------------------------------------------------------------------------------------+
|                                              MATRIZ DE HIPÓTESES TECNOLÓGICAS E DE ENGENHARIA GEOESPACIAL                                          |
+---------+----------------------------------------------+---------------+----------------------------------------+----------------------------------+
| ID      | Enunciado da Hipótese                        | Status Atual  | Evidência Primária / Justificativa     | Próximo Passo de Validação       |
+---------+----------------------------------------------+---------------+----------------------------------------+----------------------------------+
| H-T01   | Redes neurais convolucionais (CNN) genéricas | REFUTADA      | Auditoria do repositório confirmou que | Implementar exclusivamente fluxos|
|         | medem subsidência sem fase interferométrica. | (Conclusiva)  | ruído sintético gerava saídas aleatóri.| baseados em DInSAR e SBAS.       |
+---------+----------------------------------------------+---------------+----------------------------------------+----------------------------------+
| H-T02   | A API NASA/ASF HyP3 Basic sustenta o MVP     | SOB VALIDAÇÃO | Cota mensal de 8.000 créditos atende o | Executar simulação de download   |
|         | sem custos computacionais pesados iniciais.  | TÉCNICA       | piloto de Maceió (consome <200 cred.)  | de COGs e arquivamento em S3.    |
+---------+----------------------------------------------+---------------+----------------------------------------+----------------------------------+
| H-T03   | A retenção de 14 dias do HyP3 é suficiente   | VALIDADA COM  | Documentação oficial da ASF confirma   | Projetar worker assíncrono para  |
|         | para download e arquivamento próprio em S3.  | RESSALVAS     | prazo de exclusão automática dos COGs. | ingestão imediata pós-geração.   |
+---------+----------------------------------------------+---------------+----------------------------------------+----------------------------------+
| H-T04   | O satélite Sentinel-1 (Banda C, 5.6 cm) man- | ABERTA COM    | Literatura relata forte decorrelação   | Testar índice de coerência média |
|         | tém coerência em encostas com vegetação BR.  | ALTO RISCO    | temporal em biomassa vegetal úmida.    | espacial gama em encostas de SP. |
+---------+----------------------------------------------+---------------+----------------------------------------+----------------------------------+
| H-T05   | A integração HyP3 + MintPy gera taxas de     | SOB VALIDAÇÃO | Artigos Euillades (2020) e Hartwig     | Executar o protocolo D1-05 no    |
|         | subsidência coincidentes com o laudo SGB/CPRM| (Protocolada) | (2023) comprovam viabilidade da série. | ciclo futuro de validação.       |
+---------+----------------------------------------------+---------------+----------------------------------------+----------------------------------+
```

---

## 4. Registro de Informações Não Verificadas que Devem Ser Desconsideradas

As seguintes afirmações circulavam em fases preliminares do projeto e **não possuem fundamentação verificável**, devendo ser rigorosamente descartadas:

1. *"Existe monopólio internacional no Brasil e nenhuma empresa nacional oferece serviços de satélite."*  
   * **Realidade Verificada:** Incorreto. Empresas brasileiras como Globalgeo, Telespazio Brasil, CLS Brasil e INSAR Corp operam no mercado nacional com serviços de interferometria.
2. *"Prefeituras brasileiras estão buscando desesperadamente ferramentas web para ver mapas de calor de satélite."*  
   * **Realidade Verificada:** Incorreto. Prefeituras médias não têm conhecimento técnico de radar e dependem da Defesa Civil Estadual e CPRM.
3. *"O processamento InSAR completo pode rodar no navegador do cliente via WebAssembly."*  
   * **Realidade Verificada:** Incorreto. Desembrulhamento de fase (SNAPHU) e inversões matriciais multitemporais (MintPy) demandam bibliotecas C++/Python com alta carga de memória RAM (8-16 GB) e processamento em servidor.

---

## 5. Matriz de Decisão: O que Precisa Ser Respondido Antes do Código

Antes de escrever qualquer linha de código de funcionalidades comerciais no backend Spring Boot ou frontend React:

1. **Validação de Disposição a Pagar:** Conduzir as 15 entrevistas do roteiro `D1-04` para confirmar se consultorias geotécnicas têm interesse em pagar pelo processamento automatizado.
2. **Execução do Piloto Maceió:** Rodar o protocolo `D1-05` em ambiente experimental (Notebook) para mensurar tempo, custo real de créditos e convergência científica contra os laudos do SGB.
