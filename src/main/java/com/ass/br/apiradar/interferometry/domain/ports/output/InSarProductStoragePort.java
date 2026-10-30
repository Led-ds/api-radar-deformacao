package com.ass.br.apiradar.interferometry.domain.ports.output;

import com.ass.br.apiradar.interferometry.domain.model.job.TrackedInSarJob;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Porta de saída para armazenamento local controlado, validação de integridade SHA-256
 * e descompactação segura dos produtos interferométricos produzidos.
 */
public interface InSarProductStoragePort {

    /**
     * Armazena e descompacta de forma segura o arquivo zip de produto com verificação de checksum SHA-256.
     * Retorna o diretório base onde os arquivos foram descompactados.
     */
    Path storeJobProduct(TrackedInSarJob job, byte[] archiveData, String expectedSha256);

    /**
     * Retorna o caminho do diretório local de produtos do job, caso já tenha sido baixado.
     */
    Optional<Path> getJobProductPath(String jobId);
}
