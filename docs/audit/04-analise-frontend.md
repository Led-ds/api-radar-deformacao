# Radar++ — Análise Técnica Detalhada do Frontend Web

**Documento:** `04-analise-frontend.md`  
**Data da Auditoria:** Outubro de 2026  
**Status:** Concluído  
**Projetos Auditados:** `front_react/github-friend-helper` (Principal) e `monitorar-images-alos-palsar` (Legado)  
**Tecnologias Analisadas:** React 18.3, TypeScript 5.5, Vite 5.4, Tailwind CSS 3.4, React-Leaflet 4.2, Radix UI, TanStack React Query 5.56, Angular 18.2.

---

## 1. Arquitetura e Estrutura de Componentes (React)

O frontend ativo do Radar++ está localizado na pasta `front_react/github-friend-helper`. A estrutura foi gerada inicialmente pela ferramenta de IA generativa Lovable.dev (conforme evidenciado em `README.md`, dependência `lovable-tagger` e histórico Git).

### 1.1. Organização do Código-Fonte:
* `src/main.tsx`: Ponto de entrada da aplicação React.
* `src/App.tsx`: Configuração de roteamento (`react-router-dom`), tema e provedores globais (`QueryClientProvider`, `TooltipProvider`, `Toaster`, `Sonner`).
* `src/pages/`:
  * `Index.tsx`: Única tela funcional do sistema ("Monitoramento SAR"). Renderiza o cabeçalho e o componente central `MonitoringMap`.
  * `NotFound.tsx`: Página genérica 404 para rotas inexistentes.
* `src/components/`:
  * `MonitoringMap.tsx`: Componente monolítico central de visualização e controle do mapa (268 linhas). Agrupa a inicialização do Leaflet, eventos de clique, cálculo de deslocamento, gerenciamento de estado do formulário e submissão HTTP.
  * `ImageViewer.tsx`: Componente modal baseado no `Dialog` do Radix UI para visualização ampliada das imagens satelitais em alta resolução.
  * `ui/`: Coleção extensa com mais de 50 componentes de design system do **shadcn/ui** (botões, diálogos, tabelas, popovers, sliders, etc.), a maioria não utilizada pela aplicação.
* `src/services/mapService.ts`: Módulo de comunicação HTTP assíncrona com o backend Java (`fetchDeformationPoints`, `fetchImageAsBlob`, `submitDeformacao`).
* `src/models/Deformacao.ts`: Interface TypeScript com a tipagem do domínio.

---

## 2. Investigação Aprofundada dos Recursos Geoespaciais

### 2.1. Como Funciona a Seleção de Coordenadas
* **Implementação:** O componente `MapEvents` (`MonitoringMap.tsx:21-28`) escuta eventos globais de clique do Leaflet (`useMapEvents({ click: ... })`).
* **Comportamento:** Ao clicar em qualquer local do mapa, o método `handleLocationClick` (`MonitoringMap.tsx:81-87`) extrai `lat` e `lng` e atualiza o estado local `formData`.
* **Limitação Técnica:** A seleção é restrita a um único ponto geográfico (latitude/longitude pontual). Não há suporte a seleção de polígonos, retângulos delimitadores (Bounding Box) ou importação de arquivos geoespaciais (GeoJSON, KML ou Shapefile).

### 2.2. Como os Deslocamentos são Calculados e Apresentados
* **Evidência do Código:** Em `MonitoringMap.tsx:44-59` e `:81-87`:
  ```typescript
  const calculateDisplacement = (lat1: number, lon1: number, lat2: number, lon2: number): number => {
    const R = 6371; // Raio da Terra em km
    const dLat = degreesToRadians(lat2 - lat1);
    const dLon = degreesToRadians(lon2 - lon1);
    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
              Math.cos(degreesToRadians(lat1)) * Math.cos(degreesToRadians(lat2)) *
              Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    const distance = R * c; // Distância em km
    return distance * 100_000; // Converte para centímetros
  };
  ```
* **Diagnóstico Crítico:**
  1. O estado inicial do formulário define Brasília como ponto de partida (`lat: -15.7942`, `lon: -47.8822`).
  2. Quando o usuário clica no primeiro ponto do mapa (por exemplo, em Mariana/MG), o sistema calcula a distância de Haversine entre Brasília e Mariana (~630 km) e multiplica por 100.000, resultando em um suposto "deslocamento" de **63.000.000 cm**!
  3. Se o usuário der um segundo clique próximo ao primeiro (por exemplo, a 50 metros de distância), o sistema calcula 0,05 km * 100.000 = **5.000 cm** de deslocamento.
  4. Esse valor arbitrário de distância euclidiana entre dois pontos é inserido no campo `deslocamento` do formulário (com atributo `readOnly`) e gravado no banco como a deformação física do solo.
  5. **Conclusão:** Não existe qualquer medição física ou interferométrica de deslocamento da superfície. O que a interface exibe é a distância métrica entre posições de clique do mouse na tela.

### 2.3. Como os Níveis de Risco são Definidos
* No formulário, o campo `risco` é um `<input type="text">` aberto para edição manual do usuário.
* O valor digitado pelo usuário é enviado ao backend, mas no backend é imediatamente sobrescrito pelo retorno do método `avaliarRisco(deformacao)`.
* Quando os marcadores são renderizados, o popup do Leaflet simplesmente exibe a string salva: `<p className="text-sm">Nível de Risco: {point.risco}</p>`.
* Não existem limiares automáticos configurados no frontend (por exemplo, cores verde/amarelo/vermelho por faixa de mm/ano) nem alertas visuais.

### 2.4. Como as Imagens são Recuperadas e Visualizadas
* **Evidência do Código:** Em `src/services/mapService.ts:40-54`:
  ```typescript
  const deformacoesComImagens = await Promise.all(
    deformacoes.map(async (deformacao) => {
      // Ajustando o formato do nome do arquivo para corresponder ao esperado pelo S3
      const imagemBrutaUrl = await fetchImageAsBlob(`nasa_original.png`);
      const imagemProcessadaUrl = await fetchImageAsBlob(`fastapi_processada.png`);
      return {
        ...deformacao,
        imagemBruta: imagemBrutaUrl,
        imagemProcessada: imagemProcessadaUrl
      };
    })
  );
  ```
* **Diagnóstico de Valores Fixos (Hardcoding):**
  1. Para **toda e qualquer deformação** retornada pelo banco, o frontend dispara duas requisições fixas:
     * `GET http://localhost:8080/api/imagens/download/nome/nasa_original.png`
     * `GET http://localhost:8080/api/imagens/download/nome/fastapi_processada.png`
  2. A função `fetchImageAsBlob` recebe o payload binário (`blob`), gera uma URL temporária em memória via `URL.createObjectURL(blob)` e a vincula aos atributos `imagemBruta` e `imagemProcessada`.
  3. Como o backend busca esses nomes fixos dentro da pasta fixa `imagens/deformacao-68/` no S3, **todos os marcadores no mapa exibem exatamente as mesmas duas imagens** da deformação 68.
  4. O popup do Leaflet exibe as duas imagens em tamanho miniatura (200px). Ao clicar sobre uma imagem, abre-se o modal `ImageViewer.tsx` com visualização ampliada.

---

## 3. Experiência do Usuário (UX) e Fluxo de Navegação

### 3.1. Telas e Interações
* A aplicação consiste em uma tela única (`Index.tsx`), sem barra de navegação, abas ou painéis laterais.
* O mapa ocupa 600px de altura com zoom inicial fixado em 5, centralizado no Brasil (`[-15.7942, -47.8822]`).
* Logo abaixo do mapa encontra-se um formulário em grid com 4 campos: Latitude (readOnly), Longitude (readOnly), Deslocamento (readOnly) e Risco (editável), finalizado pelo botão "Enviar".

### 3.2. Ciclo de Vida do Envio de Dados
* Ao submeter o formulário (`onSubmit` em `MonitoringMap.tsx:93-104`):
  1. Exibe um diálogo de carregamento com spinner (`Dialog open={isSubmitting}`).
  2. Envia a requisição POST via `submitDeformacao(formData)`.
  3. **Comportamento Inadequado de SPA:** Após o retorno da API, executa `window.location.reload()`, forçando um recarregamento total da página HTML e reinicialização de todo o estado da aplicação no navegador, anulando os benefícios de uma SPA moderna.

### 3.3. Gerenciamento de Estado e Tratamento de Erros
* O gerenciamento de estado é feito estritamente via `useState` local no componente `MonitoringMap`.
* A biblioteca `TanStack React Query` foi instalada e instanciada em `App.tsx`, mas **não é utilizada** em nenhum dos componentes. As requisições HTTP utilizam `fetch` nativo com `async/await` dentro de `useEffect`.
* O tratamento de erros é básico: falhas na busca de dados apenas disparam `console.error()`, sem exibir avisos na interface (o sistema de toasts do Sonner e Radix UI está configurado, mas não é acionado pelo fluxo principal).

---

## 4. Comparativo Técnico: React (`front_react`) vs Angular Legado (`monitorar-images`)

| Aspecto | Implementação Legada (Angular 18) | Implementação Atual (React 18) | Diagnóstico da Evolução |
|---|---|---|---|
| **Tecnologia** | Angular 18 (Standalone Components) | React 18 com Vite e Tailwind | A migração para React modernizou o ecossistema de UI e simplificou o build. |
| **Renderização do Mapa** | Iframe embutido do Google Maps (`maps.google.co.uk`) + Leaflet básico | Leaflet nativo com React-Leaflet e OpenStreetMap | Evolução positiva: eliminou dependência de iframe estático do Google Maps. |
| **Cálculo de Haversine** | `distance * 100` (comentário dizia converter km para cm, mas multiplicava por 100 como se fossem metros) | `distance * 100_000` (converte corretamente km para cm) | A conversão numérica foi corrigida, mas o erro conceitual de considerar a distância geodésica como deformação foi mantido e amplificado. |
| **Visualização de Imagens** | Inexistente (apenas campos no modelo) | Popups com miniaturas e modal interativo (`ImageViewer`) | Grande evolução na apresentação visual para o usuário. |
| **Arquitetura de Componentes** | Separação em dois componentes (`map` e `monitoramento`) | Componente monolítico `MonitoringMap` | Regressão de modularização: no React, mapa, formulário e eventos foram concentrados em um único arquivo de 268 linhas. |
| **Nomes e Valores Fixos** | Não consumia imagens do S3 | Consome imagens fixas da pasta `deformacao-68` com nomes fixos | Criação de "mock visual" para demonstração. |

---

## 5. Resumo das Funcionalidades Efetivamente Disponíveis

1. **Disponível e Operacional:**
   * Navegação pelo mapa do Brasil com zoom e arrasto via Leaflet e OpenStreetMap.
   * Seleção de coordenada geográfica por clique do mouse.
   * Renderização de marcadores nos locais salvos no banco de dados.
   * Abertura de popup ao clicar nos marcadores com dados cadastrados.
   * Modal de zoom para visualização das imagens associadas.
2. **Disponível, mas Conceitualmente Inválido:**
   * Cálculo de deslocamento baseado na separação euclidiana entre coordenadas.
   * Associação de imagens idênticas (pasta `deformacao-68`) a todos os pontos de monitoramento.
3. **Ausente na Interface:**
   * Seleção de período temporal de monitoramento (datas início/fim).
   * Painel de séries temporais de deslocamento (gráficos de mm vs tempo).
   * Filtros por nível de risco ou região.
   * Suporte a camadas raster sobrepostas no mapa (WMS, COG ou tiles InSAR georreferenciados).
   * Autenticação e perfis de usuário.
