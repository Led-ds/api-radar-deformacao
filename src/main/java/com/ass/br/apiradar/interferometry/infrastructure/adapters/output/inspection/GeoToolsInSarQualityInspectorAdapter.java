package com.ass.br.apiradar.interferometry.infrastructure.adapters.output.inspection;

import com.ass.br.apiradar.interferometry.domain.model.job.ProductQualityMetrics;
import com.ass.br.apiradar.interferometry.domain.ports.output.InSarProductInspectionPort;
import com.ass.br.apiradar.shared.domain.model.BoundingBox;
import com.ass.br.apiradar.territory.domain.model.AreaOfInterest;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adaptador de inspeção científica e espacial baseado na biblioteca GeoTools.
 * Avalia os rasters GeoTIFF gerados pelo processamento InSAR (coerência espacial, fase desdobrada e CRS).
 */
@Component
public class GeoToolsInSarQualityInspectorAdapter implements InSarProductInspectionPort {

    private static final Logger log = LoggerFactory.getLogger(GeoToolsInSarQualityInspectorAdapter.class);

    @Override
    public ProductQualityMetrics inspectProduct(String jobId, String pairId, Path productDirectory, AreaOfInterest aoi) {
        if (productDirectory == null || !Files.exists(productDirectory)) {
            throw new IllegalArgumentException("Diretório de produtos inválido ou inexistente: " + productDirectory);
        }

        List<String> warnings = new ArrayList<>();
        File corrFile = findMatchingFile(productDirectory, "corr");
        File unwPhaseFile = findMatchingFile(productDirectory, "unw_phase");

        double meanCoherence = 0.0;
        double minCoherence = 0.0;
        double maxCoherence = 0.0;
        double urbanHighCoherenceRatio = 0.0;
        long totalPixels = 0;
        long validPixels = 0;
        String crsName = "UNKNOWN";
        boolean spatialOverlap = false;

        // 1. Inspeção do Raster de Coerência Interferométrica
        if (corrFile != null && corrFile.exists()) {
            GeoTiffReader reader = null;
            try {
                reader = new GeoTiffReader(corrFile);
                GridCoverage2D coverage = reader.read(null);
                CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem2D();
                crsName = crs != null ? crs.getName().getCode() : "EPSG:4326";

                Raster raster = coverage.getRenderedImage().getData();
                int width = raster.getWidth();
                int height = raster.getHeight();
                totalPixels = (long) width * height;

                double sum = 0.0;
                long highCount = 0;
                double currentMin = 1.0;
                double currentMax = 0.0;

                // Amostragem regular estatística para desempenho previsível
                int stepX = Math.max(1, width / 200);
                int stepY = Math.max(1, height / 200);

                for (int y = 0; y < height; y += stepY) {
                    for (int x = 0; x < width; x += stepX) {
                        float val = raster.getSampleFloat(x, y, 0);
                        if (!Float.isNaN(val) && val >= 0.0f && val <= 1.0f && val > 0.0001f) {
                            validPixels++;
                            sum += val;
                            if (val < currentMin) currentMin = val;
                            if (val > currentMax) currentMax = val;
                            if (val >= 0.35f) {
                                highCount++;
                            }
                        }
                    }
                }

                if (validPixels > 0) {
                    meanCoherence = sum / validPixels;
                    minCoherence = currentMin;
                    maxCoherence = currentMax;
                    urbanHighCoherenceRatio = (double) highCount / validPixels;
                } else {
                    warnings.add("Nenhum pixel válido de coerência detectado na amostragem do raster.");
                }

                // 2. Verificação de Cobertura Espacial e Enquadramento da AOI
                if (aoi != null && aoi.getBoundingBox() != null) {
                    BoundingBox bbox = aoi.getBoundingBox();
                    org.geotools.geometry.Envelope2D env = coverage.getEnvelope2D();
                    spatialOverlap = env != null && (
                            env.getMinX() <= bbox.maxLon() && env.getMaxX() >= bbox.minLon() &&
                                    env.getMinY() <= bbox.maxLat() && env.getMaxY() >= bbox.minLat()
                    );
                    if (!spatialOverlap) {
                        warnings.add(String.format(Locale.ROOT,
                                "O footprint do produto raster [%.3f, %.3f, %.3f, %.3f] não possui interseção válida com a AOI '%s' [%.3f, %.3f, %.3f, %.3f].",
                                env != null ? env.getMinX() : 0, env != null ? env.getMinY() : 0,
                                env != null ? env.getMaxX() : 0, env != null ? env.getMaxY() : 0,
                                aoi.getName(), bbox.minLon(), bbox.minLat(), bbox.maxLon(), bbox.maxLat()));
                    }
                } else {
                    spatialOverlap = true;
                }


            } catch (Exception e) {
                log.warn("[INSPECTION_WARN] Falha ao inspecionar GeoTIFF de coerência {}: {}. Aplicando fallback de integridade.",
                        corrFile.getName(), e.getMessage());
                warnings.add("Não foi possível ler metadados geoespaciais completos de coerência via GeoTools: " + e.getMessage());
            } finally {
                if (reader != null) {
                    reader.dispose();
                }
            }
        } else {
            warnings.add("Arquivo de coerência (*_corr.tif) não localizado no pacote descompactado.");
        }

        // 3. Inspeção da Fase Desdobrada (Unwrapped Phase)
        boolean validPhase = false;
        if (unwPhaseFile != null && unwPhaseFile.exists()) {
            GeoTiffReader unwReader = null;
            try {
                unwReader = new GeoTiffReader(unwPhaseFile);
                GridCoverage2D unwCoverage = unwReader.read(null);
                Raster unwRaster = unwCoverage.getRenderedImage().getData();

                int w = unwRaster.getWidth();
                int h = unwRaster.getHeight();
                int step = Math.max(1, Math.min(w, h) / 50);
                long validPhaseSamples = 0;

                for (int y = 0; y < h; y += step) {
                    for (int x = 0; x < w; x += step) {
                        float val = unwRaster.getSampleFloat(x, y, 0);
                        if (!Float.isNaN(val) && !Float.isInfinite(val)) {
                            validPhaseSamples++;
                        }
                    }
                }
                validPhase = validPhaseSamples > 0;
            } catch (Exception e) {
                warnings.add("Falha ao analisar fase desdobrada (*_unw_phase.tif): " + e.getMessage());
            } finally {
                if (unwReader != null) {
                    unwReader.dispose();
                }
            }
        } else {
            warnings.add("Arquivo de fase desdobrada (*_unw_phase.tif) não localizado no pacote descompactado.");
        }

        // 4. Emissão do Veredito Científico Conclusivo
        String verdict;
        if (meanCoherence >= 0.35 && validPhase && spatialOverlap) {
            verdict = "ACCEPTED";
        } else if (meanCoherence >= 0.20 && validPhase) {
            verdict = "ACCEPTABLE_WITH_RESERVATIONS";
            warnings.add(String.format(Locale.ROOT,
                    "Coerência média moderada (%.2f). Aceitável com ressalvas para solos urbanos expostos, mas sujeita a descorrelação sobre vegetação ou lagoa.",
                    meanCoherence));
        } else {
            verdict = "REJECTED";
            if (meanCoherence < 0.20) {
                warnings.add(String.format(Locale.ROOT,
                        "Coerência média crítica insuficiente (%.2f < 0.20) para medição geodésica confiável de deformação de terreno.",
                        meanCoherence));
            }
        }

        return new ProductQualityMetrics(
                jobId,
                pairId,
                aoi != null ? aoi.getId().toString() : "UNKNOWN_AOI",
                meanCoherence,
                minCoherence,
                maxCoherence,
                urbanHighCoherenceRatio,
                validPhase,
                totalPixels,
                validPixels,
                crsName,
                spatialOverlap,
                verdict,
                warnings
        );
    }

    private File findMatchingFile(Path dir, String keyword) {
        try (var stream = Files.walk(dir, 2)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> f.getName().toLowerCase().endsWith(".tif") && f.getName().toLowerCase().contains(keyword))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }
}
