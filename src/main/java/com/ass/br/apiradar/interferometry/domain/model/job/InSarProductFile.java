package com.ass.br.apiradar.interferometry.domain.model.job;

import java.io.Serializable;

/**
 * Representa um arquivo raster ou artefato documental produzido por um processamento InSAR.
 */
public record InSarProductFile(
        String filename,
        String url,
        Long sizeBytes,
        String sha256Checksum,
        String fileType
) implements Serializable {

    public static String inferFileType(String filename) {
        if (filename == null) return "UNKNOWN";
        String lower = filename.toLowerCase();
        if (lower.endsWith("_unw_phase.tif") || lower.contains("unw_phase")) return "UNWRAPPED_PHASE";
        if (lower.endsWith("_corr.tif") || lower.contains("corr") || lower.contains("coherence")) return "COHERENCE";
        if (lower.endsWith("_water_mask.tif") || lower.contains("water_mask")) return "WATER_MASK";
        if (lower.endsWith("_dem.tif") || lower.contains("dem")) return "DEM";
        if (lower.endsWith(".png") || lower.contains("browse")) return "BROWSE_IMAGE";
        if (lower.endsWith(".zip")) return "ARCHIVE";
        if (lower.endsWith(".kmz")) return "KMZ";
        if (lower.endsWith(".txt") || lower.endsWith(".md")) return "METADATA_DOC";
        return "OTHER";
    }
}
