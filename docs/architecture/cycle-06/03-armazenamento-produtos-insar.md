# Radar++ — Ciclo 6.2: armazenamento persistente de produtos InSAR

## Entrega
- Arquivos ZIP extraídos para staging isolado com publicação atômica do diretório final.
- SHA-256 calculado e comparado ao valor esperado, se fornecido.
- Limites de ingestão: arquivo ZIP em memória até 512 MiB, até 1000 entradas e 2 GiB descompactados; os últimos dois são configuráveis.
- Rejeição de Zip Slip, entradas repetidas e nomes de caminhos malformados.
- Manifesto de auditoria `radar_metadata.json` com job, AOI, par, hashes e tamanho dos dados.
- Volume nomeado `radar_insar_products` montado no backend em `/var/lib/radar/insar-products`, com permissões para usuário de serviço não-root.
- Testes adicionados para limites de entrada, falhas parciais, reinício e rejeição de sobrescrita.

## Configuração
`RADAR_PROCESSING_STORAGE_LOCAL_DIR=/var/lib/radar/insar-products`
`RADAR_PROCESSING_STORAGE_MAX_ENTRIES=1000`
`RADAR_PROCESSING_STORAGE_MAX_UNCOMPRESSED_BYTES=2147483648`
`RADAR_PROCESSING_HYP3_EXECUTION_ENABLED=false` (obrigatório até autorização).

## Limitações e próximos passos
A ingestão ainda recebe `byte[]`; um download seguro em streaming e recomeço de transferências grandes devem ser desenvolvidos antes da operacionalização. O limite máximo de 512 MiB não impede consumo de memória antes da chamada. O volume Docker não substitui backup externo, política de retenção ou autorização multi-tenant. Os resultados não foram verificados com um produto HyP3 real.

## Homologação
Executar `./mvnw test` e `docker compose config`. Recriar o container backend para confirmar que o volume continua montado e seus dados persistem. Não executar `docker compose down -v` em ambiente contendo dados.

Esta entrega não realiza jobs HyP3 nem altera a política financeira.
