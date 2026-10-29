package com.ass.br.apiradar.interferometry.infrastructure;

import com.ass.br.apiradar.interferometry.domain.model.job.ProductQualityMetrics;
import com.ass.br.apiradar.interferometry.infrastructure.adapters.output.inspection.GeoToolsInSarQualityInspectorAdapter;
import com.ass.br.apiradar.shared.domain.model.AoiId;
import com.ass.br.apiradar.shared.domain.model.OrganizationId;
import com.ass.br.apiradar.shared.domain.model.PolygonGeometry;
import com.ass.br.apiradar.territory.domain.model.AreaOfInterest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Ciclo 6.0 — Testes da Inspeção de Qualidade Científica de Produtos InSAR (GeoTools)")
class GeoToolsInSarQualityInspectorAdapterTest {

    private GeoToolsInSarQualityInspectorAdapter inspector;
    private AreaOfInterest aoiMaceio;

    @TempDir
    Path tempProductDir;

    @BeforeEach
    void setUp() {
        inspector = new GeoToolsInSarQualityInspectorAdapter();

        PolygonGeometry geom = PolygonGeometry.fromWkt("POLYGON ((-35.75 -9.67, -35.72 -9.67, -35.72 -9.64, -35.75 -9.64, -35.75 -9.67))");
        aoiMaceio = AreaOfInterest.create(

                AoiId.of("1e7a1465-c3d7-44e6-8398-5ced7dac17ec"),
                OrganizationId.of("d3b07384-d113-40a2-9721-a53b2169b821"),
                "Maceio Homologacao 5.1.1",
                geom,
                250.0
        );
    }

    @Test
    @DisplayName("Deve emitir veredito REJECTED e alertas quando arquivos essenciais de coerência e fase não existem")
    void shouldReportRejectedWhenFilesAreMissing() {
        ProductQualityMetrics metrics = inspector.inspectProduct(
                "job-empty-dir",
                "PAR-01__MACEIO",
                tempProductDir,
                aoiMaceio
        );

        assertNotNull(metrics);
        assertEquals("REJECTED", metrics.qualityVerdict());
        assertFalse(metrics.validPhaseRange());
        assertEquals(0.0, metrics.meanCoherence());
        assertTrue(metrics.qualityWarnings().stream().anyMatch(w -> w.contains("corr.tif")));
        assertTrue(metrics.qualityWarnings().stream().anyMatch(w -> w.contains("unw_phase.tif")));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException para diretório inexistente")
    void shouldThrowWhenDirectoryDoesNotExist() {
        Path nonExistent = tempProductDir.resolve("non_existent_subdir");
        assertThrows(IllegalArgumentException.class, () ->
                inspector.inspectProduct("job-1", "PAR-1", nonExistent, aoiMaceio)
        );
    }
}
