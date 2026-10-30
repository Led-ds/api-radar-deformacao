package com.ass.br.apiradar.interferometry.infrastructure;

import com.ass.br.apiradar.interferometry.domain.model.job.JobStatus;
import com.ass.br.apiradar.interferometry.domain.model.job.TrackedInSarJob;
import com.ass.br.apiradar.interferometry.infrastructure.adapters.output.storage.LocalInSarProductStorageAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Ciclo 6.0 — Testes do Armazenamento Seguro de Produtos InSAR, Validação SHA-256 e Zip-Slip")
class LocalInSarProductStorageAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private LocalInSarProductStorageAdapter storageAdapter;

    @TempDir
    Path tempStorageDir;

    @BeforeEach
    void setUp() {
        storageAdapter = new LocalInSarProductStorageAdapter(tempStorageDir.toString(), objectMapper);
    }

    @Test
    @DisplayName("Deve descompactar pacote InSAR válido, validar checksum SHA-256 e gerar radar_metadata.json")
    void shouldStoreValidZipWithSha256Checksum() throws Exception {
        String jobUuid = "job-valid-123";
        TrackedInSarJob job = new TrackedInSarJob(
                jobUuid,
                "idemp-key-123",
                "PAR-01__MACEIO",
                "aoi-maceio",
                "INSAR_ISCE_BURST",
                JobStatus.SUCCEEDED,
                Instant.now(),
                null,
                List.of(),
                null,
                null,
                false,
                null
        );

        byte[] zipData = createTestZip(List.of(
                new TestEntry("S1_018319_corr.tif", "dummy coherence data".getBytes(StandardCharsets.UTF_8)),
                new TestEntry("S1_018319_unw_phase.tif", "dummy phase data".getBytes(StandardCharsets.UTF_8))
        ));

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String sha256 = HexFormat.of().formatHex(digest.digest(zipData));

        Path storedDir = storageAdapter.storeJobProduct(job, zipData, sha256);

        assertNotNull(storedDir);
        assertTrue(Files.exists(storedDir));
        assertTrue(Files.exists(storedDir.resolve("S1_018319_corr.tif")));
        assertTrue(Files.exists(storedDir.resolve("S1_018319_unw_phase.tif")));
        assertTrue(Files.exists(storedDir.resolve("radar_metadata.json")));

        Optional<Path> found = storageAdapter.getJobProductPath(jobUuid);
        assertTrue(found.isPresent());
        assertEquals(storedDir, found.get());
    }

    @Test
    @DisplayName("Deve rejeitar compulsoriamente arquivo com checksum SHA-256 divergente")
    void shouldRejectWhenSha256Mismatches() {
        TrackedInSarJob job = new TrackedInSarJob(
                "job-corrupted-456",
                "idemp-key-456",
                "PAR-01__MACEIO",
                "aoi-maceio",
                "INSAR_ISCE_BURST",
                JobStatus.SUCCEEDED,
                Instant.now(),
                null,
                List.of(),
                null,
                null,
                false,
                null
        );

        byte[] fakeData = "tampered bytes".getBytes(StandardCharsets.UTF_8);
        String wrongSha256 = "0000000000000000000000000000000000000000000000000000000000000000";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                storageAdapter.storeJobProduct(job, fakeData, wrongSha256)
        );

        assertTrue(ex.getMessage().contains("divergência de checksum SHA-256"));
    }

    @Test
    @DisplayName("Deve bloquear e abortar com SecurityException tentativa de ataque Zip-Slip")
    void shouldPreventZipSlipVulnerability() throws Exception {
        TrackedInSarJob job = new TrackedInSarJob(
                "job-zipslip-789",
                "idemp-key-789",
                "PAR-01__MACEIO",
                "aoi-maceio",
                "INSAR_ISCE_BURST",
                JobStatus.SUCCEEDED,
                Instant.now(),
                null,
                List.of(),
                null,
                null,
                false,
                null
        );

        // Entrada maliciosa tentando escapar do diretório com ../../evil.txt
        byte[] maliciousZip = createTestZip(List.of(
                new TestEntry("../../evil.txt", "pwned".getBytes(StandardCharsets.UTF_8))
        ));

        SecurityException ex = assertThrows(SecurityException.class, () ->
                storageAdapter.storeJobProduct(job, maliciousZip, null)
        );

        assertTrue(ex.getMessage().contains("Zip Slip"));
    }

    private record TestEntry(String name, byte[] content) {}

    private byte[] createTestZip(List<TestEntry> entries) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (TestEntry e : entries) {
                ZipEntry entry = new ZipEntry(e.name());
                zos.putNextEntry(entry);
                zos.write(e.content());
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }
}
