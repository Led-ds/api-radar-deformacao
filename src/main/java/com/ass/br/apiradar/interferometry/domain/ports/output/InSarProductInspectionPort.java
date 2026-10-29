package com.ass.br.apiradar.interferometry.domain.ports.output;

import com.ass.br.apiradar.interferometry.domain.model.job.ProductQualityMetrics;
import com.ass.br.apiradar.territory.domain.model.AreaOfInterest;

import java.nio.file.Path;

/**
 * Porta de saída para inspeção inicial de integridade física, espacial e métricas científicas
 * de qualidade dos produtos interferométricos (coerência, fase e CRS).
 */
public interface InSarProductInspectionPort {

    /**
     * Inspeciona os arquivos GeoTIFF contidos no diretório do produto (coerência, fase desdobrada, DEM, CRS)
     * e avalia a conformidade com a Área de Interesse e critérios de aceite científico.
     */
    ProductQualityMetrics inspectProduct(String jobId, String pairId, Path productDirectory, AreaOfInterest aoi);
}
