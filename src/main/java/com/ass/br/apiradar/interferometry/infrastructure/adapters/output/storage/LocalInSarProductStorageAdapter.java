package com.ass.br.apiradar.interferometry.infrastructure.adapters.output.storage;

import com.ass.br.apiradar.interferometry.domain.model.job.TrackedInSarJob;
import com.ass.br.apiradar.interferometry.domain.ports.output.InSarProductStoragePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Armazenamento controlado de produtos InSAR.
 * Extrai em diretório temporário e só publica o conjunto completo após validação.
 * O volume montado é responsabilidade da infraestrutura (Docker Compose).
 */
@Component
public class LocalInSarProductStorageAdapter implements InSarProductStoragePort {
    private static final Logger log = LoggerFactory.getLogger(LocalInSarProductStorageAdapter.class);
    private static final long MAX_ARCHIVE_BYTES = 512L * 1024 * 1024;
    private static final int DEFAULT_MAX_ENTRIES = 1000;
    private static final long DEFAULT_MAX_UNCOMPRESSED_BYTES = 2L * 1024 * 1024 * 1024;

    private final Path baseStoragePath;
    private final ObjectMapper objectMapper;
    private final int maxEntries;
    private final long maxUncompressedBytes;

    @Autowired
    public LocalInSarProductStorageAdapter(
            @Value("${radar.processing.storage.local-dir:target/insar-products}") String baseStorageDir,
            ObjectMapper objectMapper,
            @Value("${radar.processing.storage.max-entries:1000}") int maxEntries,
            @Value("${radar.processing.storage.max-uncompressed-bytes:2147483648}") long maxUncompressedBytes) {
        if (maxEntries < 1 || maxUncompressedBytes < 1) {
            throw new IllegalArgumentException("Limites de extração devem ser positivos.");
        }
        this.baseStoragePath = Paths.get(baseStorageDir).toAbsolutePath().normalize();
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.maxEntries = maxEntries;
        this.maxUncompressedBytes = maxUncompressedBytes;
    }

    /** Compatibilidade com testes e consumidores que instanciam o adaptador diretamente. */
    public LocalInSarProductStorageAdapter(String baseStorageDir, ObjectMapper objectMapper) {
        this(baseStorageDir, objectMapper, DEFAULT_MAX_ENTRIES, DEFAULT_MAX_UNCOMPRESSED_BYTES);
    }

    public Path getBaseStoragePath() { return baseStoragePath; }

    @Override
    public Path storeJobProduct(TrackedInSarJob job, byte[] archiveData, String expectedSha256) {
        if (job == null || job.jobId() == null || job.jobId().isBlank()
                || job.pairId() == null || job.pairId().isBlank()) {
            throw new IllegalArgumentException("JobId e PairId são obrigatórios.");
        }
        if (archiveData == null || archiveData.length == 0) {
            throw new IllegalArgumentException("Arquivo ZIP não pode ser vazio.");
        }
        if (archiveData.length > MAX_ARCHIVE_BYTES) {
            throw new SecurityException("Arquivo ZIP excede limite de 512 MiB para ingestão em memória.");
        }

        String actualSha256 = calculateSha256(archiveData);
        if (expectedSha256 != null && !expectedSha256.isBlank()
                && !actualSha256.equalsIgnoreCase(expectedSha256.trim())) {
            throw new IllegalArgumentException("Download corrompido: divergência de checksum SHA-256.");
        }

        Path jobDir = baseStoragePath.resolve(sanitizePathSegment(job.pairId()))
                .resolve(sanitizePathSegment(job.jobId())).normalize();
        if (!jobDir.startsWith(baseStoragePath)) {
            throw new SecurityException("Caminho de destino inválido.");
        }
        Path staging = null;
        try {
            Files.createDirectories(baseStoragePath);
            if (Files.isSymbolicLink(baseStoragePath)) {
                throw new SecurityException("Diretório base não pode ser link simbólico.");
            }
            Path pairDir = jobDir.getParent();
            Files.createDirectories(pairDir);
            if (Files.isSymbolicLink(pairDir)) {
                throw new SecurityException("Diretório do par não pode ser link simbólico.");
            }

            // Evita sobrescrever evidências já publicadas para um mesmo job.
            if (Files.exists(jobDir, LinkOption.NOFOLLOW_LINKS)) {
                throw new FileAlreadyExistsException("Produto do job já existe: " + job.jobId());
            }
            staging = Files.createTempDirectory(pairDir, ".insar-staging-");
            int entries = 0;
            long totalBytes = 0;
            byte[] buffer = new byte[64 * 1024];
            Set<Path> destinations = new HashSet<>();

            try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(archiveData))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (++entries > maxEntries) {
                        throw new SecurityException("ZIP excede quantidade máxima de entradas: " + maxEntries);
                    }
                    String name = entry.getName();
                    if (name == null || name.isBlank() || name.indexOf('\\') >= 0
                            || name.startsWith("/") || name.indexOf('\0') >= 0) {
                        throw new SecurityException("Nome de entrada ZIP inválido.");
                    }
                    Path destination = staging.resolve(name).normalize();
                    if (!destination.startsWith(staging) || destination.equals(staging)) {
                        throw new SecurityException("Tentativa de Zip Slip detectada: " + name);
                    }
                    if (!destinations.add(destination)) {
                        throw new SecurityException("Entrada ZIP duplicada: " + name);
                    }
                    if (entry.isDirectory()) {
                        Files.createDirectories(destination);
                    } else {
                        Files.createDirectories(destination.getParent());
                        // CREATE_NEW evita substituir arquivos existentes em caso de entradas conflitantes.
                        try (OutputStream out = Files.newOutputStream(destination, StandardOpenOption.CREATE_NEW)) {
                            int n;
                            while ((n = zis.read(buffer)) != -1) {
                                totalBytes += n;
                                if (totalBytes > maxUncompressedBytes) {
                                    throw new SecurityException("ZIP excede limite de bytes descompactados.");
                                }
                                out.write(buffer, 0, n);
                            }
                        }
                    }
                    zis.closeEntry();
                }
            }
            if (entries == 0) {
                throw new IllegalArgumentException("ZIP sem entradas.");
            }

            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("jobId", job.jobId());
            metadata.put("pairId", job.pairId());
            metadata.put("aoiId", job.aoiId());
            metadata.put("jobType", job.jobType());
            metadata.put("status", job.status().name());
            metadata.put("downloadedAt", Instant.now().toString());
            metadata.put("archiveSha256", actualSha256);
            metadata.put("archiveBytes", archiveData.length);
            metadata.put("extractedBytes", totalBytes);
            metadata.put("entriesCount", entries);
            metadata.put("files", job.files());
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(staging.resolve("radar_metadata.json").toFile(), metadata);

            // Mesma partição: publicação por rename atômico, sem produtos parciais visíveis.
            Files.move(staging, jobDir, StandardCopyOption.ATOMIC_MOVE);
            staging = null;
            log.info("[STORAGE_SUCCESS] Produto do job {} publicado em {}", job.jobId(), jobDir);
            return jobDir;
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao armazenar produto InSAR: " + ex.getMessage(), ex);
        } finally {
            if (staging != null) {
                cleanupStaging(staging);
            }
        }
    }

    @Override
    public Optional<Path> getJobProductPath(String jobId) {
        if (jobId == null || jobId.isBlank() || !Files.isDirectory(baseStoragePath)) {
            return Optional.empty();
        }
        String safeId = sanitizePathSegment(jobId);
        try (var stream = Files.walk(baseStoragePath, 3)) {
            return stream.filter(p -> Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS))
                    .filter(p -> p.getFileName().toString().equals(safeId))
                    .filter(p -> Files.isRegularFile(p.resolve("radar_metadata.json"), LinkOption.NOFOLLOW_LINKS))
                    .findFirst();
        } catch (IOException e) {
            log.warn("[STORAGE_LOOKUP_ERROR] Falha ao buscar job {}", jobId, e);
            return Optional.empty();
        }
    }

    private void cleanupStaging(Path staging) {
        try (var paths = Files.walk(staging)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); }
                catch (IOException e) { log.warn("Não foi possível remover temporário {}", p, e); }
            });
        } catch (IOException e) {
            log.warn("Falha ao limpar staging {}", staging, e);
        }
    }

    private String calculateSha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível.", e);
        }
    }

    private String sanitizePathSegment(String segment) {
        if (segment == null || !segment.matches("[a-zA-Z0-9][a-zA-Z0-9._-]{0,199}")
                || segment.equals(".") || segment.equals("..")) {
            throw new IllegalArgumentException("Identificador de caminho inválido.");
        }
        return segment;
    }
}
