# Radar++ — Ciclo 4: Arquitetura Técnica, UML e Contratos
## Documento 07 — Arquitetura de Armazenamento de Objetos (AWS S3)

**Documento:** `07-armazenamento-aws-s3.md`  
**Ciclo:** Ciclo 4 — Arquitetura Técnica, UML e Contratos  
**Data:** 24 de Outubro de 2026  
**Status:** Oficial — Entregue para Homologação de Arquitetura  
**Responsáveis:** Arquiteto Cloud AWS, Engenheiro de Dados Geoespaciais, Arquiteto de Software Sênior  
**Repositório Oficial:** `api-radar-deformacao` (caminho: `docs/architecture/cycle-04/07-armazenamento-aws-s3.md`)  
**Regra de Governança Estrita:** Especificação técnica e política de retenção. **Não provisionar buckets AWS, não alterar permissões e não executar operações em nuvem nesta etapa.**

---

### 1. Diretrizes Estruturais e Segurança de Armazenamento

1. **Bloqueio Total de Acesso Público (*Block Public Access*):** O bucket de produtos e artefatos do Radar++ possui bloqueio público integral ativado na raiz (`BlockPublicAcls = true`, `IgnorePublicAcls = true`, `BlockPublicPolicy = true`, `RestrictPublicBuckets = true`). Nenhum arquivo é exposto diretamente na internet.
2. **Acesso Seguro via URLs Pré-Assinadas (*Presigned URLs*):** Tanto o upload de artefatos pelo worker quanto a leitura de rasters COG pela aplicação React ocorrem através de URLs temporárias assinadas criptograficamente com o algoritmo AWS SigV4, geradas pelo backend Spring Boot com validade máxima de 15 minutos (900 segundos).
3. **Criptografia em Repouso e em Trânsito:** Todos os objetos são criptografados em repouso usando SSE-S3 (*AES-256*) e trafegam estritamente por conexões TLS 1.3 (`aws:SecureTransport = true`).
4. **Integridade Criptográfica (SHA-256):** Todo arquivo gravado possui o metadado customizado `x-amz-meta-checksum-sha256` e o hash validado na ingestão para garantir a cadeia de custódia auditável da proveniência técnica.

---

### 2. Topologia de Buckets e Estrutura Hierárquica de Prefixos

Para garantir isolamento organizacional e sustentabilidade financeira, adota-se um **único bucket privado particionado por organização e ciclo de vida**:

* **Nome Canônico do Bucket:** `radar-insar-artifacts-[account-id]-[region]`

```
s3://radar-insar-artifacts-.../
│
├── organizations/{organizationId}/
│   └── aois/{aoiId}/
│       └── analyses/{analysisId}/
│           │
│           ├── v{versionNumber}/                       <-- CAMADA 1: PRODUTOS FINAIS PRESERVADOS
│           │   ├── velocity_annual_mean_los_cog.tif    (Cloud Optimized GeoTIFF)
│           │   ├── coherence_temporal_mean_cog.tif     (Cloud Optimized GeoTIFF)
│           │   ├── displacement_timeseries_sbas.nc     (NetCDF-4 / HDF5)
│           │   ├── deformation_points_coherent.parquet (Apache Parquet vetorial)
│           │   ├── metadata_provenance_audit.json      (Cadeia de custódia técnica)
│           │   └── quality_assessment_report.json      (Métricas e decisão do gating)
│           │
│           └── intermediate/                           <-- CAMADA 2: PRODUTOS INTERMEDIÁRIOS
│               ├── pairs/
│               │   ├── 20230105_20230117/
│               │   │   ├── unwrapped_phase.tif
│               │   │   └── spatial_coherence.tif
│               │   └── ...
│               └── correlation_matrix.h5
│
└── scratch/                                            <-- CAMADA 3: ARQUIVOS EFÊMEROS DESCARTÁVEIS
    └── jobs/{jobId}/
        ├── cropped_dem_glo30.tif
        ├── snap_logs.txt
        └── worker_temp_chunks/
```

---

### 3. As Três Camadas de Retenção e Regras de Ciclo de Vida (Lifecycle Rules)

Em estrito atendimento às restrições de custo do MVP, **Object Lock, replicação entre regiões e versionamento automático irrestrito NÃO são ativados por padrão**. O ciclo de vida é configurado via regras declarativas de S3 Lifecycle:

```
+----------------------------------------------------------------------------------------------------------------------------------------------------+
|                                    POLÍTICA DE RETENÇÃO E CLASSES DE ARMAZENAMENTO AWS S3                                                          |
+---+-----------------------------+---------------------------------------+-----------------------------+--------------------------------------------+
| N | Categoria de Artefato       | Localização (Prefixo S3)              | Classe S3 e Transição       | Regra de Expurgo e Governança              |
+---+-----------------------------+---------------------------------------+-----------------------------+--------------------------------------------+
| 1 | **Produtos Finais**         | `.../analyses/.../v*/`                | **S3 Standard** nos         | **Retenção Permanente:** Preservação dos   |
|   | **Preservados**             |                                       | primeiros 90 dias; transição| COGs e metadados. Não expurga. Versionado  |
|   |                             |                                       | para **Glacier Instant**    | logicamente por pasta (`/v1/`, `/v2/`)     |
|   |                             |                                       | Retrieval após 90 dias.     | sem custo de versões ocultas do S3.        |
+---+-----------------------------+---------------------------------------+-----------------------------+--------------------------------------------+
| 2 | **Produtos**                | `.../analyses/.../intermediate/`      | **S3 Standard-IA**          | **Retenção Configurável (60 dias):**       |
|   | **Intermediários**          |                                       | (Infrequent Access) a partir| Expurgo automático após 60 dias da geração.|
|   |                             |                                       | do dia 30.                  | Permite re-inversão no MintPy em caso de   |
|   |                             |                                       |                             | ajuste metodológico sem reprocessar HyP3.  |
+---+-----------------------------+---------------------------------------+-----------------------------+--------------------------------------------+
| 3 | **Arquivos Efêmeros**       | `scratch/jobs/*`                      | **S3 Standard**             | **Expurgo Imediato / Curto (7 dias):**     |
|   | **e Descartáveis**          |                                       |                             | Lifecycle rule com `Expiration = 7 dias`.  |
|   |                             |                                       |                             | Deleta logs brutos e arquivos temporários. |
+---+-----------------------------+---------------------------------------+-----------------------------+--------------------------------------------+
```

---

### 4. Política IAM de Privilégio Mínimo (*Least Privilege*)

Acesso restrito atribuído exclusivamente às credenciais de execução da aplicação (IAM Roles para ECS Fargate):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowBackendGeneratePresignedUrlsAndAudit",
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:GetObjectMetadata",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::radar-insar-artifacts-*",
        "arn:aws:s3:::radar-insar-artifacts-*/*"
      ]
    },
    {
      "Sid": "EnforceSecureTransportOnly",
      "Effect": "Deny",
      "Action": "s3:*",
      "Resource": [
        "arn:aws:s3:::radar-insar-artifacts-*",
        "arn:aws:s3:::radar-insar-artifacts-*/*"
      ],
      "Condition": {
        "Bool": {
          "aws:SecureTransport": "false"
        }
      }
    }
  ]
}
```

---

### 5. Mecanismo de Leitura de Produtos COG via HTTP Range Request

Para permitir a exploração interativa de rasters geodésicos sem que o cliente precise baixar arquivos de centenas de megabytes:

1. O Cloud Optimized GeoTIFF (COG) é gerado com pirâmides internas (*overviews*) e blocos (*tiles*) de $256 \times 256$ pixels;
2. Quando o usuário navega pelo mapa ou clica em uma coordenada $(lat, lon)$, o visualizador cliente (OpenLayers / geotiff.js) lê apenas os cabeçalhos TIFF (*IFD — Image File Directory*) enviando o cabeçalho HTTP:
   `Range: bytes=0-16384`
3. O Amazon S3 suporta nativamente HTTP 206 (*Partial Content*) em URLs pré-assinadas com método GET;
4. O navegador recebe apenas alguns kilobytes correspondentes à área visível ou ao ponto de série temporal inspecionado, permitindo que curvas cinemáticas sejam carregadas em menos de **500 milissegundos**.

---

### 6. Estimativa Preliminar de Custos de Armazenamento (TCO MVP)

Considerando o monitoramento contínuo de 10 Áreas de Interesse (tamanho médio de 35 km² cada) ao longo de 1 ano:
- **Produtos Finais:** ~500 MB por análise anual $\times$ 10 AOIs = **5 GB / ano** ($\approx \text{US\$ } 0{,}12/\text{mês}$ no S3 Standard).
- **Intermediários (durante a execução ativa de 60 dias):** ~15 GB por rede interferométrica $\times$ 10 AOIs = **150 GB efêmeros** ($\approx \text{US\$ } 3{,}45/\text{mês}$).
- **Operações PUT/GET e Tráfego de Dados de Saída:** Estimado em $\approx \text{US\$ } 1{,}50/\text{mês}$.
- **Custo Total Estimado de Storage para o MVP:** **Menor que US$ 6,00 / mês**, comprovando a sustentabilidade financeira da arquitetura proposta.
