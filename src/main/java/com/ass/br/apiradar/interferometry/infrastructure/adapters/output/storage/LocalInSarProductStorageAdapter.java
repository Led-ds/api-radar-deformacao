package com.ass.br.apiradar.interferometry.infrastructure.adapters.output.storage;

import com.ass.br.apiradar.interferometry.domain.model.job.TrackedInSarJob;
import com.ass.br.apiradar.interferometry.domain.ports.output.InSarProductStoragePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Adaptador de armazenamento local com validação estrita de integridade SHA-256,
 * prevenção contra vulnerabilidades de Zip-Slip e organização estruturada por par e job.
 */
@Component
public class LocalInSarProductStorageAdapter implements InSarProductStoragePort {

    private static final Logger log = LoggerFactory.getLogger(LocalInSarProductStorageAdapter.class);

    private final Path baseStoragePath;
    private final ObjectMapper objectMapper;

    public LocalInSarProductStorageAdapter(
            @Value("${radar.processing.storage.local-dir:target/insar-products}") String baseStorageDir,
            ObjectMapper objectMapper
    ) {
        this.baseStoragePath = Paths.get(baseStorageDir).toAbsolutePath().normalize();
        this.objectMapper = objectMapper;
    }

    public Path getBaseStoragePath() {
        return baseStoragePath;
    }

    @Override
    public Path storeJobProduct(TrackedInSarJob job, byte[] archiveData, String expectedSha256) {
        if (job == null || job.jobId() == null || job.jobId().isBlank()) {
            throw new IllegalArgumentException("Job e JobId não podem ser nulos.");
        }
        if (archiveData == null || archiveData.length == 0) {
            throw new IllegalArgumentException("Os dados do arquivo de produto não podem ser vazios.");
        }

        // Validação criptográfica de integridade SHA-256 antes da persistência em disco
        if (expectedSha256 != null && !expectedSha256.isBlank()) {
            String calculatedSha256 = calculateSha256(archiveData);
            if (!calculatedSha256.equalsIgnoreCase(expectedSha256.trim())) {
                log.error("[STORAGE_CHECKSUM_ERROR] Divergência de checksum SHA-256 para o job {}. Esperado: {}, Calculado: {}",
                        job.jobId(), expectedSha256, calculatedSha256);
                throw new IllegalArgumentException(String.format(
                        "Download corrompido ou adulterado para o job %s: divergência de checksum SHA-256. Esperado: %s, Calculado: %s",
                        job.jobId(), expectedSha256, calculatedSha256
                ));
            }
            log.info("[STORAGE_CHECKSUM_OK] Checksum SHA-256 validado com sucesso para o job {}.", job.jobId());
        }

        String safePairId = sanitizePathSegment(job.pairId());
        String safeJobId = sanitizePathSegment(job.jobId());
        Path jobDir = baseStoragePath.resolve(safePairId).resolve(safeJobId).normalize();

        try {
            Files.createDirectories(jobDir);

            // Descompactação segura com proteção contra Path Traversal (Zip Slip)
            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(archiveData))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    Path entryDestination = jobDir.resolve(entry.getName()).normalize();

                    // Bloqueio rigoroso de Zip Slip
                    if (!entryDestination.startsWith(jobDir)) {
                        throw new SecurityException("Tentativa de Zip Slip detectada no arquivo: " + entry.getName());
                    }

                    if (entry.isDirectory()) {
                        Files.createDirectories(entryDestination);
                    } else {
                        if (entryDestination.getParent() != null) {
                            Files.createDirectories(entryDestination.getParent());
                        }
                        Files.copy(zis, entryDestination, StandardCopyOption.REPLACE_EXISTING);
                    }
                    zis.closeEntry();
                }
            }

            // Grava documento de metadados e auditoria local
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("jobId", job.jobId());
            metadata.put("pairId", job.pairId());
            metadata.put("aoiId", job.aoiId());
            metadata.put("jobType", job.jobType());
            metadata.put("status", job.status().name());
            metadata.put("downloadedAt", Instant.now().toString());
            metadata.put("filesCount", job.files().size());
            metadata.put("files", job.files());

            Path metadataPath = jobDir.resolve("radar_metadata.json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(metadataPath.toFile(), metadata);

            log.info("[STORAGE_SUCCESS] Produto do job {} armazenado e descompactado com sucesso em: {}",
                    job.jobId(), jobDir);

            return jobDir;
        } catch (IOException e) {
            log.error("[STORAGE_IO_ERROR] Falha ao persistir produto do job {}: {}", job.jobId(), e.getMessage(), e);
            throw new IllegalStateException("Falha de I/O ao armazenar arquivos do produto InSAR: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Path> getJobProductPath(String jobId) {
        if (jobId == null || jobId.isBlank() || !Files.exists(baseStoragePath)) {
            return Optional.empty();
        }
        String safeJobId = sanitizePathSegment(jobId);
        try (var stream = Files.walk(baseStoragePath, 3)) {
            return stream
                    .filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().equals(safeJobId))
                    .findFirst();
        } catch (IOException e) {
            log.warn("[STORAGE_SEARCH_WARN] Falha ao buscar produto local para o job {}: {}", jobId, e.getMessage());
            return Optional.empty();
        }
    }

    private String calculateSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 não disponível no ambiente Java.", e);
        }
    }

    private String sanitizePathSegment(String segment) {
        if (segment == null || segment.isBlank()) return "default";
        return segment.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
