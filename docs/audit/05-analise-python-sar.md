# Radar++ — Análise Técnica da Implementação Python e Capacidades SAR/InSAR

**Documento:** `05-analise-python-sar.md`  
**Data da Auditoria:** Outubro de 2026  
**Status:** Concluído  
**Projeto Auditado:** `radar_fastapi_py`  
**Tecnologias Analisadas:** Python 3.10, FastAPI, Uvicorn, TensorFlow/Keras, Pillow (PIL), NumPy.

---

## 1. Visão Geral da Implementação Python

O projeto `radar_fastapi_py` foi concebido como um microsserviço de inteligência artificial e visão computacional para processar imagens e classificar o risco de deformação de terreno.

### 1.1. Arquitetura do Serviço:
* **Framework Web:** FastAPI com servidor ASGI Uvicorn executando na porta `8082` (`app/main.py:14`).
* **Dependências (`requirements.txt`):**
  ```text
  fastapi
  uvicorn
  tensorflow
  pillow
  ```
  *Nota:* O arquivo `requirements.txt` não fixa versões de nenhuma biblioteca, expondo o ambiente a quebras súbitas por atualizações de dependências.
* **Rotas Disponíveis (`app/routes/analyze.py`):**
  1. `POST /analyze/`: Recebe arquivo multipart (`file`), flags booleanas (`grayscale`) e tamanho alvo (`size`, padrão 64). Realiza pré-processamento via PIL e executa inferência no modelo Keras.
  2. `GET /metrics/`: Endpoint para consulta de métricas de acurácia e perda do modelo. **Contém bug fatal:** invoca `model.evaluate()` sem passar tensores `x` e `y`, gerando invariavelmente `TypeError` e resposta HTTP 500.

---

## 2. Investigação do Modelo de Inteligência Artificial

### 2.1. Arquitetura da Rede Neural (`models/train_model.py`)
A rede neural convolucional (CNN) compilada no arquivo `app/models/models/meu_modelo.h5` possui a seguinte topologia sequencial:
* Entrada: Matriz `(64, 64, 3)` ou `(64, 64, 1)` dependendo do modo de cor.
* Camada 1: `Conv2D(32, kernel_size=(3,3), activation='relu')`
* Camada 2: `MaxPooling2D(pool_size=(2,2))`
* Camada 3: `Conv2D(64, kernel_size=(3,3), activation='relu')`
* Camada 4: `MaxPooling2D(pool_size=(2,2))`
* Camada 5: `Flatten()`
* Camada 6: `Dense(64, activation='relu')`
* Camada 7 (Saída): `Dense(1, activation='sigmoid')`
* Otimizador: `Adam(learning_rate=0.001)`
* Função de perda: `binary_crossentropy`

### 2.2. Origem dos Dados de Treinamento e Validade Científica
Ao inspecionar o script `models/train_model.py:27-33`, identifica-se a gênese dos pesos do modelo:
```python
# Gerar dados fictícios de imagens com a configuração de canais correta
X_train = np.random.random((100, IMG_SIZE, IMG_SIZE, CHANNELS))  # 100 imagens de treino
y_train = np.random.randint(2, size=(100, 1))  # Labels binárias (Stable/Unstable)

# Treinar o modelo
model.fit(X_train, y_train, epochs=10, batch_size=32)
```

### Diagnóstico Técnico Inquestionável:
1. **Dados 100% Sintéticos / Ruído Branco:** O conjunto de treinamento é composto por 100 matrizes numéricas geradas por gerador de números pseudo-aleatórios uniformes (`np.random.random`).
2. **Rótulos Arbitrários:** As classes alvo foram geradas por sorteio binário aleatório de 0 ou 1 (`np.random.randint(2)`).
3. **Inexistência de Aprendizado de Características Físicas:** A rede foi treinada para minimizar o erro de previsão sobre ruído estatístico puro. O modelo não aprendeu nenhuma característica visual, geológica, textural ou interferométrica.
4. **Classificação em Produção:** Quando uma imagem chega à rota `POST /analyze/`, a inferência `prediction = model.predict(img_array)` devolve um valor flutuante aleatório. Se `prediction > 0.5`, rotula como `"Stable"`; caso contrário, `"Unstable"`. Trata-se de uma simulação estética sem nenhuma validade analítica ou científica.

---

## 3. Avaliação Rigorosa das Capacidades: Imagem Genérica vs Geoespacial vs SAR/InSAR

Para cumprir as diretrizes de auditoria do Ciclo 0, analisamos o que o código Python efetivamente executa em cada um dos três níveis de processamento:

```
+-------------------------------------------------------------------------+
|                  NÍVEIS DE CAPACIDADE DE PROCESSAMENTO                  |
+-------------------------------------------------------------------------+
| [X] A. Processamento Genérico de Imagens (PIL, normalização, resize)    |
| [ ] B. Processamento Geoespacial (GDAL, Rasterio, EPSG, Resampling)     |
| [ ] C. Processamento SAR/InSAR Científico (Interferometria, Fase, LOS)  |
+-------------------------------------------------------------------------+
```

### Nível A — Processamento Genérico de Imagens: [PRESENTE]
* **O que existe no código:**
  * Leitura de arquivo via buffer com Pillow (`Image.open(file.file)`).
  * Redimensionamento bilinear ou bicúbico padrão para 64x64 pixels (`img.resize(target_size)`).
  * Conversão de espaço de cor para RGB (`img.convert("RGB")`) ou escala de cinza (`img.convert("L")`).
  * Normalização elementar de valores de pixel para o intervalo `[0.0, 1.0]` dividindo por 255.0 (`img_array / 255.0`).
* **Veredito:** Operações computacionais genéricas de manipulação de imagens matriciais padrão da web, sem relação com sensoriamento remoto.

### Nível B — Processamento Geoespacial: [TOTALMENTE AUSENTE]
* **Critérios de verificação:** Uso de bibliotecas geoespaciais (como GDAL, Rasterio, Fiona, Shapely, PyProj), manipulação de metadados de georreferenciamento (GeoTransform, GCPs, CRS), reprojeção cartográfica, leitura de formatos científicos (GeoTIFF, COG, NetCDF, HDF5) e recorte espacial por Bounding Box / AOI.
* **O que existe no código:**
  * O microserviço Python **ignora solenemente todo e qualquer metadado geográfico**.
  * Se o backend Java enviar um GeoTIFF, a biblioteca PIL simplesmente decodifica a matriz de pixels bruta e descarta todas as tags TIFF e projeções georreferenciadas (tags EPSG, tiepoints, pixel scale).
  * Não há uma única linha de código com importações de `gdal`, `rasterio`, `geopandas` ou `pyproj`.
* **Veredito:** Inexistente no projeto Python.

### Nível C — Processamento SAR / InSAR Científico: [TOTALMENTE AUSENTE]
* **Critérios de verificação:**
  1. Suporte a dados SAR complexos de nível 1 (*Single Look Complex* — SLC), contendo canais em quadratura e fase ($I$ e $Q$).
  2. Corregistro submétrico de pares de imagens temporalmente defasadas (master e slave).
  3. Formação de interferograma complexo:
     $$\Delta\phi = \phi_{\text{interf}} = \phi_{\text{topo}} + \phi_{\text{deslocamento}} + \phi_{\text{atmosfera}} + \phi_{\text{órbita}} + \phi_{\text{ruído}}$$
  4. Remoção de fase topográfica utilizando Modelos Digitais de Elevação (DEM como SRTM, Copernicus DEM ou ALOS AW3D30).
  5. Estimativa e filtragem de coerência interferométrica (filtro de Goldstein-Werner).
  6. Desembrulhamento de fase 2D (*Phase Unwrapping*, ex.: algoritmos SNAPHU ou Branch-Cut).
  7. Conversão de fase desembrulhada em deslocamento métrico na Linha de Visada do satélite (*Line of Sight* — LOS):
     $$d_{\text{LOS}} = -\frac{\lambda}{4\pi} \Delta\phi_{\text{desembrulhada}}$$
  8. Decomposição vetorial para deslocamento vertical e leste-oeste (séries temporais PS-InSAR ou SBAS).
* **O que existe no código:**
  * **Absolutamente nada.** O repositório não possui referências a SNAP/snappy, ISCE2, ISCE3, MintPy, GMTSAR, StaMPS, GAMMA ou HyP3.
  * O termo "SAR" nos nomes de classes e pastas é estritamente aspiracional.

---

## 4. Comunicação com o Backend Java e Persistência

1. **Protocolo:** HTTP/REST síncrono.
2. **Payload:** Multipart/form-data contendo o arquivo de imagem e campos de texto (`grayscale=true`, `size=64`).
3. **Retorno:** JSON com o esquema:
   ```json
   {
     "result": "Stable",
     "confidence": 0.5432,
     "processing_time": 0.0125
   }
   ```
4. **Persistência de Resultados:** O microsserviço Python não possui banco de dados nem grava arquivos em disco. A gravação do resultado é delegada ao backend Java, que salva na tabela `analise_resultado`.

---

## 5. Resumo do Diagnóstico do Componente Python

| Dimensão | Estado Atual | Avaliação Técnica |
|---|---|---|
| **Ambiente e Dependências** | Python 3.10, 4 libs sem versão no `requirements.txt` | Frágil; pasta `venv` versionada indevidamente no git. |
| **Arquitetura da API** | FastAPI + Uvicorn funcional na porta 8082 | Simples e leve, mas com rota `/metrics/` quebrada. |
| **Processamento de Imagens** | Redimensionamento para 64x64 via PIL | Genérico, destrói informações de resolução e textura. |
| **Processamento Geoespacial** | Inexistente (0 bibliotecas geoespaciais) | Descarta qualquer informação de coordenadas e CRS. |
| **Processamento SAR/InSAR** | Inexistente (0 algoritmos interferométricos) | Ausência total do domínio científico principal do produto. |
| **Modelo de IA** | Treinado com 100 imagens de ruído pseudo-aleatório | Inútil cientificamente; gera saídas aleatórias para a interface. |
